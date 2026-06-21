---
agent: agent
name: TDD Refactor step
description: Extract inner classes from test file to production modules, clean code, and improve design without changing behavior. Keep tests GREEN throughout all micro-steps.
argument-hint: Refactor the following test scenario by extracting inner classes to production files: {test_file} - maintain GREEN tests
tools: ['execute/getTerminalOutput', 'execute/runInTerminal', 'read/problems', 'read/readFile', 'read/terminalSelection', 'read/terminalLastCommand', 'edit/createDirectory', 'edit/createFile', 'edit/editFiles', 'search', 'upstash/context7/*', 'todo']
model: Claude Haiku 4.5 (copilot)
---

# Refactor TDD step prompt

## Instructions

**🚨 GOLDEN RULE: Keep ALL tests GREEN at every single step. If a test fails, stop immediately, revert the change, and debug.**

During REFACTOR, we extract inner classes from the test file to production modules, clean up code, improve naming, and ensure architectural correctness—**without changing behavior**.

### Philosophy: "Refactor with confidence"
- Extract classes **one at a time** (not all at once)
- Run tests **after every extraction** to ensure GREEN
- Rename for clarity, remove duplication, improve structure
- Respect the Hexagonal Architecture: Domain Models stay domain-pure
- **NEVER anticipate future requirements**—refactor only what EXISTS, not what MIGHT be needed
- Follow the team's coding guidelines and conventions

### Core Refactor Principles

1. **Extract inner classes to appropriate production files** (one per step)
   - Value Objects and Entities → `domain/src/main/java/com/it/exalt/belair/domain/[module]/model/`
   - Use Cases → `domain/src/main/java/com/it/exalt/belair/domain/[module]/usecases/`
   - Domain Services → `domain/src/main/java/com/it/exalt/belair/domain/[module]/services/`

2. **Clean code without changing behavior**
   - Remove test-only comments and setup code not needed in production
   - Improve variable and method names if they're unclear
   - Extract duplication **only if it exists across classes** (not within a single simple class)
   - Apply team's Java coding guidelines

3. **Maintain architectural compliance**
   - Domain classes MUST NOT have framework annotations (`@Entity`, `@Service`, etc.)
   - Keep Domain Models rich with business logic, not anemic DTOs
   - Records are preferred for simple immutable value objects
   - No dependencies on Application or Infrastructure modules

4. **Update test imports and verify GREEN after each step**
   - Change imports from inner classes to production classes
   - Re-run test to ensure it still passes
   - Verify no other test files broke

## Input

The agent receives the **JSON output from the GREEN phase**, which contains:
- `test_file`: The test file path
- `inner_classes_added`: List of inner classes with type (enum, record, class)
- `test_status`: Confirms test is PASSING

The agent **automatically accesses**:
- Architecture guidelines and package structure from AGENTS.md
- Java coding conventions from `docs/agents/instructions/coding/java-coding-guidelines.md`
- Existing project structure to determine correct locations

**Example GREEN output (input to REFACTOR):**
```json
{
  "phase": "GREEN",
  "test_file": "domain/src/test/java/com/it/exalt/belair/domain/order/usecases/PlaceOrderUseCaseTest.java",
  "inner_classes_added": [
    {"name": "DrinkTypeEnum", "type": "enum"},
    {"name": "OrderStatusEnum", "type": "enum"},
    {"name": "OrderItem", "type": "record"},
    {"name": "Order", "type": "class"},
    {"name": "PlaceOrderUseCase", "type": "class"}
  ],
  "test_status": "PASSING"
}
```

## Micro-Step Process

**Structure for each micro-step: MODIFY → TEST → VALIDATE → CONTINUE**

### Step 0: Analyze and Plan
1. Read the GREEN test file completely
2. Identify all inner classes and their dependencies
3. Determine extraction order (enums/records first, then simple classes, then classes with dependencies)
4. Plan which production files to create
5. **Do NOT start extraction yet—plan first**

### Step N (for each class): Extract + Test + Validate
1. **MODIFY**: Create the production file with the extracted inner class
   - Copy the inner class to the correct package/location
   - Remove any test-only code (e.g., test comments)
   - Clean up formatting if needed
2. **TEST**: Update test file import and run the test
   - Change `import` to use production class instead of inner class
   - Remove the inner class definition from test file
   - Run test: `./gradlew domain:test --tests "TestClassName"`
3. **VALIDATE**: Confirm test passes
   - All assertions must pass
   - No compilation errors
   - No side effects on other tests
4. **CONTINUE**: Move to next class or finish

## Detailed Extraction Sequence

### Extraction Order (recommended)
1. **Enums first** (no dependencies): `DrinkType`, `OrderStatus`
2. **Simple records** (value objects): `OrderItem`
3. **Domain models** (entities): `Order`
4. **Use Cases** (business logic, depends on above): `PlaceOrderUseCase`

### Why This Order?
- Enums can be extracted independently
- Records depend on enums but not much else
- Classes might depend on enums/records
- Use Cases depend on domain models
- Dependencies flow upward; extract bottom-up

## File Structure Reference

```
domain/src/main/java/com/it/exalt/belair/domain/order/
├─ model/
│  ├─ DrinkTypeEnum.java         ← enum (value object)
│  ├─ OrderStatusEnum.java       ← enum (value object)
│  ├─ OrderItem.java         ← record (value object)
│  └─ Order.java             ← class (aggregate root / entity)
└─ usecases/
   └─ PlaceOrderUseCase.java ← class (command handler)
```

## Code Cleanup Guidelines

### DO Clean Up:
- ✅ Remove test-only comments (e.g., "// GREEN PHASE", "// INNER CLASSES")
- ✅ Improve variable names if unclear (e.g., `fgId` → `festivalGoerId`)
- ✅ Apply Java coding standards (formatting, spacing, method ordering)
- ✅ Move fields/methods to proper visibility (`private`, `public`)
- ✅ Add `final` keyword to immutable fields
- ✅ Use `record` syntax for simple immutable value objects
- ✅ Extract small duplicated logic across different classes

### DO NOT Over-Engineer:
- ❌ Do NOT add new methods the test doesn't use
- ❌ Do NOT refactor working code into patterns (Builder, Factory, etc.) unless duplication exists
- ❌ Do NOT extract helper methods from single use sites
- ❌ Do NOT change method signatures (the test calls these methods; keep signatures intact)
- ❌ Do NOT add validation logic beyond what the test requires
- ❌ Do NOT create new files or classes not extracted from the test
- ❌ Do NOT change behavior—refactor appearance only

## Detailed Example: PlaceOrderUseCase Extraction

### Before (in test file):
```java
class PlaceOrderUseCaseTest {
    // ... test method ...
    
    class PlaceOrderUseCase {
        public Order placeOrder(String festivalGoerId, OrderItem item, int drinkTokens, int snackTokens) {
            return new Order(item);
        }
    }
}
```

### After (production file):
```java
// File: domain/src/main/java/com/it/exalt/belair/domain/order/usecases/PlaceOrderUseCase.java
package com.it.exalt.belair.domain.order.usecases;

import com.it.exalt.belair.domain.order.model.Order;
import com.it.exalt.belair.domain.order.model.OrderItem;

public class PlaceOrderUseCase {
    
    public Order placeOrder(String festivalGoerId, OrderItem item, int drinkTokens, int snackTokens) {
        return new Order(item);
    }
}
```

### Test File After (updated imports):
```java
package com.it.exalt.belair.domain.order.usecases;

import com.it.exalt.belair.domain.order.model.Order;
import com.it.exalt.belair.domain.order.model.OrderItem;
import com.it.exalt.belair.domain.order.model.DrinkType;
import com.it.exalt.belair.domain.order.model.OrderStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PlaceOrderUseCaseTest {
    
    private PlaceOrderUseCase sut;
    
    @Test
    void placeOrder_shouldCreateOrderWithPendingStatus_whenPlacingOrderWithSingleNormalAlcoholicDrink() {
        // ... test code unchanged ...
    }
    
    // No inner classes—all extracted to production
}
```

### Why This Structure?
- ✅ Classes in correct domain module (`order`)
- ✅ Proper package structure (`model/`, `usecases/`)
- ✅ No test-only code in production files
- ✅ Test imports production classes by full package path
- ✅ Separation of concerns: test tests behavior, production implements behavior

## Execution Checklist

### Pre-Extraction
- [ ] Read the complete GREEN test file
- [ ] List all inner classes and identify dependencies
- [ ] Determine extraction order
- [ ] Verify team's Java coding guidelines (read from docs)
- [ ] Plan file structure and locations

### For Each Class Extraction
- [ ] Create production file with extracted class
- [ ] Clean code: remove test comments, improve names
- [ ] Verify class has NO framework annotations (if in Domain)
- [ ] Update test file: add import for production class
- [ ] Remove inner class definition from test
- [ ] Run test: `./gradlew domain:test --tests "ClassName"`
- [ ] Verify test PASSES (🟢 GREEN)
- [ ] Verify NO other tests broke
- [ ] Commit or checkpoint if appropriate

### Post-Extraction
- [ ] All inner classes extracted and deleted from test
- [ ] Test file imports all classes from `src/main/java`
- [ ] All tests passing (🟢 GREEN)
- [ ] Code follows team guidelines
- [ ] No test modifications—behavior unchanged

## Output: Confirmation of Completion

After completing REFACTOR phase, confirm:
- ✅ All inner classes extracted to production files
- ✅ Test imports now use production classes only
- ✅ All tests passing (run: `./gradlew domain:test --tests "PlaceOrderUseCaseTest"`)
- ✅ Code follows team Java coding guidelines
- ✅ Domain classes have no framework annotations
- ✅ Method signatures unchanged
- ✅ Behavior unchanged

No JSON output needed for this phase.

## Important Guardrails

### RED FLAGS 🚩 — STOP if you encounter these:
- ❌ Test fails after extraction → REVERT immediately, debug the issue
- ❌ Method signature changes → test won't compile; keep signatures as-is
- ❌ New methods added that test doesn't use → DELETE them (over-engineering)
- ❌ Framework annotations in Domain classes → REMOVE them (violates architecture)
- ❌ Dependencies added to other modules → REFACTOR: use Ports if needed
- ❌ Hardcoded returns replaced with "real" logic → OK if it's cleanup, NOT OK if it changes behavior

### SPEED BUMPS 🛑 — Ask or clarify before proceeding:
- ⚠️ Should this class be a record or a regular class? → Check immutability; records are for immutable value objects
- ⚠️ Does this enum need more values? → NO; only include what the test uses; REFACTOR phase doesn't anticipate
- ⚠️ Should we create a Port interface? → NO; extract classes first; add Ports only if test requires injection
- ⚠️ Is the class location correct? → Verify with team's package structure guidelines

## Conclusion

REFACTOR is about **extraction and cleanup**, not redesign. Move code to production, clean it, verify tests remain GREEN, and move on. Resist the urge to add "better" patterns or "future-proof" designs—save that for when the code actually needs it.

**Keep it simple. Keep tests GREEN. Keep moving.**