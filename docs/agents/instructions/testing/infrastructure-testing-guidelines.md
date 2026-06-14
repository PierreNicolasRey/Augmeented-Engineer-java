# Testing Guidelines - Infrastructure Module

**Scope**: Tests that validate persistence adapters and their mapping layers work correctly with the database.

**Approach**: Use Testcontainers for real database integration. Focus on the adapter layer, not JPA itself.

---

## What NOT to Test

❌ **Don't test JPA repositories directly** — Spring Data handles that.

✅ **Instead, test the adapter that implements the port:**
   - The adapter receives Domain Models
   - Converts them to Persistence Entities
   - Persists via JPA
   - Retrieves and maps back to Domain Models or Read Models
   - Validates the full round-trip works

---

## Test Pattern - Persistence Adapter

```java
@Testcontainers
class OrderRepositoryAdapterIntegrationTest {
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
        .withDatabaseName("buvette_test")
        .withUsername("test")
        .withPassword("test");
    
    @Autowired private OrderRepositoryAdapter sut;  // System Under Test (the adapter)
    
    @Test
    void save_shouldPersistDomainModelAndRetrieveItCorrectly() {
        // GIVEN
        var order = new Order("order-1", "cust@test.com", LocalDateTime.now());
        
        // WHEN
        sut.save(order);
        
        // THEN: verify round-trip (Domain → Entity → DB → Entity → Domain)
        var retrieved = sut.find("order-1");
        assertThat(retrieved).isPresent()
            .get()
            .satisfies(o -> {
                assertThat(o.id()).isEqualTo("order-1");
                assertThat(o.customerEmail()).isEqualTo("cust@test.com");
            });
    }
    
    @Test
    void find_shouldReturnEmpty_whenOrderNotFound() {
        // WHEN
        var result = sut.find("non-existent");
        
        // THEN
        assertThat(result).isEmpty();
    }
}
```

---

## Test Pattern - Mapper

Test mappers independently (no Testcontainers needed):

```java
class OrderMapperTest {
    private OrderMapper sut;  // System Under Test
    
    @BeforeEach
    void setUp() {
        sut = new OrderMapper();
    }
    
    @Test
    void toDomain_shouldMapAllFields_whenEntityIsValid() {
        // GIVEN
        var entity = new OrderJpaEntity();
        entity.setId("order-1");
        entity.setCustomerEmail("cust@test.com");
        entity.setCreatedAt(LocalDateTime.of(2026, 5, 25, 10, 0));
        
        // WHEN
        var domain = sut.toDomain(entity);
        
        // THEN
        assertThat(domain.id()).isEqualTo("order-1");
        assertThat(domain.customerEmail()).isEqualTo("cust@test.com");
    }
    
    @Test
    void toEntity_shouldMapAllFields_whenDomainIsValid() {
        // GIVEN
        var domain = new Order("order-2", "user@test.com", LocalDateTime.now());
        
        // WHEN
        var entity = sut.toEntity(domain);
        
        // THEN
        assertThat(entity.getId()).isEqualTo("order-2");
        assertThat(entity.getCustomerEmail()).isEqualTo("user@test.com");
    }
}
```

---

## What to Test

- **Persistence**: Save, retrieve, update, delete operations work correctly.
- **Mapping**: Domain → Entity → Database → back to Domain works bidirectionally.
- **Constraints**: Duplicate keys, foreign keys, and other database constraints work.
- **Migrations**: Flyway migrations execute successfully on a clean schema.
- **Event Publishing**: Events are delivered to the message broker.
- **Query Results**: Adapters return correct Domain Models or Read Models from database.
- **Constraint Handling**: Database constraints (unique keys, foreign keys) are properly validated.
- **Mapper Bidirectionality**: Entities map to Domain and back without data loss.

---

## Principles

✅ **DO:**
- Use real Testcontainers for database integration.
- Test the **full round-trip**: Domain → Entity → DB → Entity → Domain.
- Test mappers independently (Domain ↔ Entity, Entity ↔ ReadModel).
- Test adapter error cases (constraint violations, not found).

❌ **DON'T:**
- Test JPA repository methods—Spring Data is already tested.
- Mock the database—use Testcontainers for real integration.
- Test business logic (belongs in domain tests).
- Call Use Cases directly.