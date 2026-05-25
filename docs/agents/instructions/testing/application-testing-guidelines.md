# Testing Guidelines - Application Module

**Scope**: Tests that validate REST API contracts, controllers, and HTTP responses.

**Approach**: Mock all Use Cases and Domain Services, test HTTP layer in isolation.

---

## Test Pattern - MockMvc

```java
@WebMvcTest(OrderController.class)
class OrderControllerTest {
    @Autowired private MockMvc mockMvc;
    @MockBean private PlaceOrderUseCase placeOrderUseCase;
    
    private static final String ORDERS_ENDPOINT = "/api/orders";
    
    @Test
    void post_shouldReturn201_whenOrderIsValid() throws Exception {
        // GIVEN
        var request = new OrderRequestDTO("cust-1", List.of(new ItemDTO("item-1", 2)));
        var response = new Order("order-1", "cust-1", LocalDateTime.now());
        given(placeOrderUseCase.execute(any())).willReturn(response);
        
        // WHEN & THEN
        mockMvc.perform(post(ORDERS_ENDPOINT)
            .contentType(MediaType.APPLICATION_JSON)
            .content(asJson(request)))
            .andExpect(status().isCreated());
    }
    
    @Test
    void post_shouldReturn400_whenPayloadIsInvalid() throws Exception {
        // GIVEN
        var invalidRequest = """
            { "customerId": "", "items": [] }
            """;
        
        // WHEN & THEN
        mockMvc.perform(post(ORDERS_ENDPOINT)
            .contentType(MediaType.APPLICATION_JSON)
            .content(invalidRequest))
            .andExpect(status().isBadRequest());
    }
}
```

---

## What to Test

- **Happy path**: Valid request returns correct status and response shape.
- **Error cases**: 400 (Bad Request), 401 (Unauthorized), 404 (Not Found), 500 (Server Error).
- **Request mapping**: DTOs are correctly converted to domain commands.
- **Response mapping**: Domain models are correctly converted to DTOs.
- **Headers**: Content-Type and other response headers are correct.

---

## Constants

```java
private static final String ORDERS_ENDPOINT = "/api/orders";
private static final String ORDERS_BY_ID_ENDPOINT = "/api/orders/{id}";
```

---

## Principles

✅ **DO:**
- Mock all Use Cases and Domain Services with `@MockBean`.
- Test API contracts (status codes, response shape, headers).
- Test mappers independently (`OrderRequestDTO` → `PlaceOrderCommand`).
- Test error responses (400, 401, 404, 500).

❌ **DON'T:**
- Call real Use Cases—mock them.
- Test business logic (belongs in domain tests).
- Query the database.
- Make real external API calls.
