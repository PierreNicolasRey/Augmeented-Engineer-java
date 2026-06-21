# Java Coding Guidelines

This document defines the Java coding conventions and best practices for the project. Follow these rules to keep the codebase consistent, readable, and maintainable.

## Formatting

- **Indentation:** 4 spaces per indent level. Do not use tabs.
- **Line length:** Prefer max 120 characters.
- **Braces:** Always use K&R style (opening brace on same line): `if (cond) {`.

Recommended tooling: use `google-java-format` or `spotless` configured in Gradle.

## Naming Conventions

- **Packages:** All lower-case, reverse-domain style: `com.exalt.it.belair`.
- **Classes / Interfaces:** PascalCase: `OrderService`, `MenuItem`.
- **Enums:** PascalCase with `Enum` suffix: `OrderStatusEnum`, `DrinkTypeEnum`.
- **Methods:** camelCase, verb-based: `calculateTotal()`.
- **Variables / Parameters / Fields:** camelCase: `orderItems`, `totalAmount`.
- **Constants:** UPPER_SNAKE_CASE and `static final`: `DEFAULT_TAX_RATE`.

## Java Records

Use `record` types for concise, immutable data carriers (DTOs, value objects):

```java
public record Money(BigDecimal amount, Currency currency) {
    public Money {
        Objects.requireNonNull(amount, "amount must not be null");
        Objects.requireNonNull(currency, "currency must not be null");
    }
}
```

## Switch Expressions & Pattern Matching

Prefer expression-based switches and pattern matching over complex if-else chains when dealing with sealed interfaces or type patterns.

## Dependency Injection

- **Constructor Injection:** Always use standard constructor injection for dependencies. Do not use field injection (`@Autowired` on fields).
- **Module-Based Rule:**
  - **Inside Domain Module:** Do NOT use framework annotations (like Spring's `@Service` or `@Component`). Pure Java constructors are sufficient. Wiring is handled externally in the Infrastructure configuration.
  - **Inside Application & Infrastructure Modules:** Framework annotations (e.g., `@RestController`, `@Service`, `@Repository`, `@Configuration`) are strictly required to let Spring manage the lifecycle and technical wiring.

```java
// Example in Infrastructure/Application layer (Spring managed)
@Service
public class OrderPersistenceAdapter implements OrderOutboundPort {
    private final JpaOrderRepository repository;

    public OrderPersistenceAdapter(JpaOrderRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
    }
}

// Example in Domain layer (Pure Java)
public class PlaceGroupOrderUseCase implements PlaceGroupOrderInboundPort {
    private final OrderOutboundPort orderPort;

    public PlaceGroupOrderUseCase(OrderOutboundPort orderPort) {
        this.orderPort = Objects.requireNonNull(orderPort, "orderPort must not be null");
    }
}
```

## Null & Error Handling

- **Null Avoidance:** Avoid returning null from any public method. Use Optional<T> for optional or nullable results.

- **Defensive Design:** Validate public method arguments using Objects.requireNonNull() or explicit guards in constructors.

- **Exceptions:** Use runtime (unchecked) exceptions for unrecoverable business rule violations or programming errors. Design custom, descriptive domain exceptions (e.g., TokenBalanceInsufficientException).

## Javadoc & Documentation

- **Public scope (mandatory):** Every public class, interface, and method MUST have complete Javadoc including `@param` for all parameters, `@return` for non-void methods, and `@throws` for checked/domain exceptions.
- **Private scope (optional):** Javadoc is reserved for non-obvious code paths.
- Use `{@link ClassName}` to reference other classes or methods within Javadoc.
- Prefer high-level intent over implementation details.

Example:
```java
/**
 * Places a group order and publishes {@link OrderPlacedEvent}.
 * 
 * @param basket the shopping basket containing items to order
 * @throws EmptyBasketException if basket contains no items
 * @throws InvalidCustomerException if customer is not found or inactive
 */
public void placeOrder(Basket basket) { ... }
```

## Logging

- Use `org.slf4j.Logger`:
```java 
private static final Logger LOGGER = LoggerFactory.getLogger(MyClass.class);
```
- Never log sensitive information.

## Build & Dependencies

- Use Gradle for builds. Keep dependency versions centralized in `gradle/libs.versions.toml`.
