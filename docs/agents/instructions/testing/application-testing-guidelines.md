# Testing Guidelines - Application Module

**Scope**: Tests that validate REST API contracts, controllers, and HTTP responses.

**Approach**: Mock all Use Cases and Domain Services, test HTTP layer **ONLY via MockMvc** (never call controller directly).

**Critical Rule**: Controllers are **thin**: receive HTTP → delegate to Use Case → return response.

---

## Test Pattern - MockMvc with @WebMvcTest

**MANDATORY APPROACH:**

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
        
        // WHEN & THEN: Call endpoint via HTTP, verify 201 Created
        mockMvc.perform(post(ORDERS_ENDPOINT)
            .contentType(MediaType.APPLICATION_JSON)
            .content(asJson(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.orderId").exists());
    }
    
    @Test
    void post_shouldReturn400_whenOrderIsEmpty() throws Exception {
        // GIVEN: Use Case throws validation exception
        given(placeOrderUseCase.execute(any()))
            .willThrow(new EmptyOrderException("Cannot be empty"));
        
        var request = new OrderRequestDTO("cust-1", List.of());
        
        // WHEN & THEN: Verify 400 Bad Request (mapped by GlobalErrorHandler)
        mockMvc.perform(post(ORDERS_ENDPOINT)
            .contentType(MediaType.APPLICATION_JSON)
            .content(asJson(request)))
            .andExpect(status().isBadRequest());
    }
    
    @Test
    void post_shouldReturn404_whenResourceNotFound() throws Exception {
        // GIVEN: Use Case throws not found exception
        given(placeOrderUseCase.execute(any()))
            .willThrow(new ResourceNotFoundException("Goer not found"));
        
        var request = new OrderRequestDTO("unknown", List.of(new ItemDTO("item-1", 1)));
        
        // WHEN & THEN: Verify 404 Not Found (mapped by GlobalErrorHandler)
        mockMvc.perform(post(ORDERS_ENDPOINT)
            .contentType(MediaType.APPLICATION_JSON)
            .content(asJson(request)))
            .andExpect(status().isNotFound());
    }
    
    @Test
    void post_shouldReturn422_whenBusinessRuleViolated() throws Exception {
        // GIVEN: Use Case throws business exception
        given(placeOrderUseCase.execute(any()))
            .willThrow(new InsufficientTokensException("Not enough tokens"));
        
        var request = new OrderRequestDTO("cust-1", List.of(new ItemDTO("premium-drink", 5)));
        
        // WHEN & THEN: Verify 422 Unprocessable Entity (mapped by GlobalErrorHandler)
        mockMvc.perform(post(ORDERS_ENDPOINT)
            .contentType(MediaType.APPLICATION_JSON)
            .content(asJson(request)))
            .andExpect(status().isUnprocessableEntity());
    }
}
```

---

## HTTP Status Codes Reference

| Code | Assertion | When to use |
|------|-----------|-----------|
| **201** | `status().isCreated()` | Resource successfully created |
| **200** | `status().isOk()` | Request successful (GET, successful state change) |
| **400** | `status().isBadRequest()` | Input validation failed (empty fields, invalid format, etc.) |
| **401** | `status().isUnauthorized()` | Authentication required or failed |
| **403** | `status().isForbidden()` | Authorized but not permitted |
| **404** | `status().isNotFound()` | Resource doesn't exist |
| **409** | `status().isConflict()` | Resource already exists or state conflict |
| **422** | `status().isUnprocessableEntity()` | Business rule violated (insufficient tokens, constraint violation, etc.) |
| **500** | `status().isInternalServerError()` | Unexpected server error |

**Mapping Example**: When Domain Use Case throws `InsufficientTokensException`, `GlobalErrorHandler` maps it to **422 Unprocessable Entity**.

---

## Standalone MockMvc Setup (if @WebMvcTest unavailable)

If `@MockBean` is not available in your Spring version, use standalone setup:

```java
class OrderControllerTest {
    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    
    @Mock private PlaceOrderUseCase placeOrderUseCase;
    
    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        OrderController controller = new OrderController(placeOrderUseCase);
        // Register GlobalErrorHandler to catch exceptions and map to HTTP status
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalErrorHandler())
                .build();
        objectMapper = new ObjectMapper();
    }
    
    @Test
    void post_shouldReturn400_whenOrderIsEmpty() throws Exception {
        given(placeOrderUseCase.execute(any()))
            .willThrow(new EmptyOrderException("Cannot be empty"));
        
        var request = new OrderRequestDTO("cust-1", List.of());
        
        mockMvc.perform(post("/api/orders")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }
}
```

---

## What to Test

- **Happy path**: Valid request returns correct HTTP 200/201 status and response structure.
- **Error cases**: Wrong status codes (400, 404, 422, 500) with proper error messages.
- **Request mapping**: DTOs are correctly converted to domain commands.
- **Response mapping**: Domain models are correctly converted to response DTOs.
- **Headers**: Content-Type and location headers are correct.

---

## Constants

```java
private static final String ORDERS_ENDPOINT = "/api/v1/orders";
private static final String ORDER_BY_ID_ENDPOINT = "/api/v1/orders/{id}";
```

---

## CRITICAL Rules

✅ **MUST DO:**
- Use `@WebMvcTest(ControllerClass.class)` with `@MockBean` for Use Cases
- Mock all business operations—never call real Use Cases
- Call endpoints via `mockMvc.perform(post/get/put/delete(...))` with full HTTP context
- Assert **exact HTTP status codes** (201, 400, 404, 422, etc.)
- Assert response body structure with `jsonPath(...)`
- Use exception-based error handling: Use Case throws → GlobalErrorHandler maps to HTTP status

❌ **MUST NOT:**
- Test controller method directly: `var result = controller.placeOrder(request);` → **WRONG**, use MockMvc instead
- Leave Javadoc describing the phase: `"GREEN phase test for..."` → Remove it
- Add validation logic in controller → Belongs in Domain Use Case
- Add business calculations in controller → Belongs in Domain Use Case
- Test business logic → That's Domain layer tests job
- Make real database calls → Mock Use Cases completely
- Make real external API calls

