# Documentation Guidelines


## Global rules

- All documentation must be written in **English**.
- Use `{@link ...}` whenever you reference a Java class, interface, or member in Javadoc or technical comments.
- Prefer high-level intent over implementation details in documentation.
- Write comments only when they add clarity: document why a behavior exists, not what the code does line by line.
- Use Swagger/OpenAPI annotations for controller endpoint documentation whenever possible.

## 1. Java Code Documentation by Module

### Application Module (REST Controllers)

**Controllers / API endpoints:**
- Prefer Swagger/OpenAPI annotations for endpoint contract documentation instead of only relying on Javadoc.
- Document each public controller method with a clear HTTP operation description, expected request payload, response type, and status codes.
- Use `@Operation`, `@ApiResponse`, `@Schema`, and related annotations when available.
- Example:
  ```java
  @Operation(summary = "Create a new order", description = "Creates a group order and returns the created order details.")
  @ApiResponse(responseCode = "201", description = "Order created successfully", content = @Content(schema = @Schema(implementation = OrderResponse.class)))
  public ResponseEntity<OrderResponse> createOrder(@RequestBody CreateOrderRequest request) { ... }
  ```

### Domain Module (Use Cases, Services, Models, Ports)

**Use Cases:**
- Document the business operation implemented by the Use Case.
- Describe preconditions, execution behavior, and postconditions.
- List thrown Domain Exceptions and their meaning.
- Document any Domain Events published.
- Example:
  ```java
  /**
   * Acknowledges an order and publishes {@link OrderAcknowledgedEvent}.
   *
   * Preconditions: order must be in PENDING state.
   * Postcondition: order transitions to ACKNOWLEDGED.
   *
   * @throws OrderNotFoundException if the order does not exist
   * @throws InvalidOrderStateException if the order is not in PENDING state
   */
  public class AcknowledgeOrderUseCase { ... }
  ```

**Domain Services (Query Services):**
- Document that the service is read-only.
- Describe what data is retrieved and any filtering or aggregation logic.
- Document the returned Domain Model or Read Model.
- Example:
  ```java
  /**
   * Query service for retrieving festival information.
   * Does not modify state. Returns {@link FestivalReadModel} with aggregated data.
   */
  public class FestivalQueryService { ... }
  ```

**Ports (Interfaces):**
- Document the contract and expected implementation behavior.
- Clarify inbound vs outbound role.
- Example:
  ```java
  /**
   * Outbound port for dispatching domain events.
   * Implementations handle technical delivery, such as message brokers or persistence.
   */
  public interface EventPublisherPort { ... }
  ```

### Infrastructure Module (Repositories, Persistence, Configuration)

**Persistence Adapters (Repository Implementations):**
- Document the mapping between Domain Models and Persistence Entities.
- Clarify any database-specific queries or optimizations.
- Reference the port implemented.
- Example:
  ```java
  /**
   * JPA adapter implementing {@link OrderRepository}.
   * Maps {@link Order} to {@link OrderJpaEntity}.
   */
  public class OrderRepositoryAdapter implements OrderRepository { ... }
  ```

## 2. Data Model Documentation

### Domain Models, Entities and Value Objects

- Document the business concept represented by the model.
- Explain invariants, validation rules, and state transitions.
- For entities, describe identity and lifecycle boundaries.
- For value objects, document why equality is based on value and what makes the object immutable.
- Reference any related use cases, domain events, or ports when the model is a core business asset.
- Avoid repeating persistence details; those belong in the Infrastructure module.

### DTOs and Mapping Layers

- Document the role of each DTO as a boundary object between Application and Domain layers.
- Explain whether the DTO is used for input, output, or both.
- Clarify any non-obvious data transformations or normalization rules.
- When the DTO maps directly to a specific Domain command or response, mention that relationship.
- Document mapper classes to describe conversion direction and any exceptional cases.

### Persistence Entities

- Document the persistence structure and how it corresponds to the Domain Model.
- Explain any denormalization, technical columns, or audit fields.
- Keep the comment focused on the mapping contract, not the business logic.
- If a persistence entity diverges from the Domain Model shape for technical reasons, document the reason clearly.

## 3. Business Flow Documentation

### Use Cases and Command Flows

- Document the business intent of the use case and the problem it solves.
- Describe input expectations, side effects, and postconditions.
- Specify invariants and domain rules enforced by the use case.
- Note the domain entities loaded and any state transitions they undergo.
- Mention the primary outbound ports used and any domain events published.

### Query Services and Read Models

- Document that query services are read-only and do not alter domain state.
- Describe the shape of the returned data and the reason this projection exists.
- Explain filters, aggregations, or joins that matter for the business use case.
- Reference the Read Model or projection type returned by the query.

### Domain Events and Side Effects

- Document the meaning of each event in business terms, not technical transport details.
- Explain what triggered the event and what outcome it represents.
- Document expected consumers when known, and the side effects they should produce.
- Keep technical routing implementation details in Infrastructure documentation, not in Domain event definitions.

## 4. Feature Documentation

When documenting a feature, consult `feature-documentation-guidelines.md` for the template and detailed structure.

## 5. Architecture Documentation

When documenting architecture, consult `architecture-documentation-guidelines.md` for the template and detailed structure.
