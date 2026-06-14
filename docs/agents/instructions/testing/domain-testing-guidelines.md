# Testing Guidelines - Domain Module

**Scope**: Tests that validate business logic through Use Cases and Domain Services.

**Entry Point**: Tests drive the system through:
- **Use Case handlers** for write operations (commands that modify state)
- **Query Services** for read operations (retrieving data without modification)

See the [AGENTS.md Design Principles](../../../../AGENTS.md#design-principles--architectural-rules) for routing rules.

---

## Test Pattern - Use Case (Write Operation)

```java
class PlaceOrderUseCaseTest {
    private PlaceOrderUseCase sut;  // System Under Test
    private FakeOrderRepository orderRepository;
    private FakeEventPublisher eventPublisher;
    
    @BeforeEach
    void setUp() {
        orderRepository = new FakeOrderRepository();
        eventPublisher = new FakeEventPublisher();
        sut = new PlaceOrderUseCase(orderRepository, eventPublisher);
    }
    
    @Test
    void placeOrder_shouldPersistAndPublishEvent_whenBasketIsValid() {
        // GIVEN
        var basket = new Basket("cust-1", List.of(new Item("item-1", 2)));
        
        // WHEN
        sut.placeOrder(basket);
        
        // THEN
        assertThat(orderRepository.findAll()).hasSize(1);
        assertThat(eventPublisher.findAll()).hasSize(1);
    }
    
    @Test
    void placeOrder_shouldThrowException_whenBasketIsEmpty() {
        // GIVEN
        var emptyBasket = new Basket("cust-1", List.of());
        
        // WHEN & THEN
        assertThatThrownBy(() -> sut.placeOrder(emptyBasket))
            .isInstanceOf(DomainException.class);
    }
}
```

---

## Test Pattern - Query Service (Read Operation)

```java
class CatalogQueryServiceTest {
    private CatalogQueryService sut;  // System Under Test
    private FakeCatalogRepository catalogRepository;
    
    @BeforeEach
    void setUp() {
        catalogRepository = new FakeCatalogRepository();
        sut = new CatalogQueryService(catalogRepository);
    }
    
    @Test
    void getCatalog_shouldReturnAllProducts_whenCatalogIsPopulated() {
        // GIVEN
        var product = new Product("prod-1", "Coffee", BigDecimal.valueOf(5));
        catalogRepository.add(product);
        
        // WHEN
        var result = sut.getCatalog();
        
        // THEN
        assertThat(result).hasSize(1)
            .contains(product);
    }
}
```

---

## How to Test

Fakes are lightweight, in-memory implementations of secondary ports (repositories, event publishers, etc.):

```java
public class FakeOrderRepository implements OrderRepository, TestState<Order, String> {
    private final List<Order> store = new ArrayList<>();

    @Override
    public void add(Order item) {
        store.removeIf(o -> o.id().equals(item.id()));
        store.add(item);
    }

    @Override
    public Optional<Order> find(String id) {
        return store.stream()
            .filter(o -> o.id().equals(id))
            .findFirst();
    }

    @Override
    public List<Order> findAll() { 
        return List.copyOf(store); 
    }
}

public class FakeEventPublisher implements EventPublisherPort, TestState<DomainEvent, String> {
    private final List<DomainEvent> events = new ArrayList<>();

    @Override
    public void publish(DomainEvent event) {
        events.add(event);
    }

    @Override
    public void add(DomainEvent item) { events.add(item); }
    
    @Override
    public Optional<DomainEvent> find(String id) { /* adapt as needed */ }
    
    @Override
    public List<DomainEvent> findAll() { return List.copyOf(events); }
}
```

---

## TestState Contract

Fakes implement this interface to manage test state:

```java
public interface TestState<T, ID> {
    void add(T item);
    Optional<T> find(ID id);
    List<T> findAll();
}
```

---

## What to Test

- **Happy path**: Use Case/Service successfully processes valid input.
- **Validation errors**: Exceptions thrown for invalid input (empty, null, out-of-bounds).
- **State changes**: Entities are modified as expected (Use Cases only).
- **Event publishing**: Domain events are triggered correctly (Use Cases only).
- **Business constraints**: Rules are enforced (e.g., "insufficient balance").
- **Query results**: Services return correct data in expected format.

---

## Principles

✅ **DO:**
- Test through Use Case handlers (primary ports).
- Use lightweight fakes for secondary ports.
- Assert on observable behavior, not implementation.

❌ **DON'T:**
- Use Spring, Mockito, or JPA annotations in domain code.
- Call the real repository or event publisher implementations.
- Test framework code—only test your business logic.