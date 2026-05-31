# Code Review Guidelines

This document serves as your mandatory self-review checklist. During the Code Review Phase (🕵️), you MUST evaluate your generated code against these automated gates. Do not approve your own work if a single check fails.

**Contextual References:** This guide structuraly relies on `AGENTS.md` (Architecture & CQS), `java-coding-guidelines.md`, and the module-specific `*-testing-guidelines.md` files.

---

## 1. Hexagonal & Domain Integrity Gate

### Framework Leakage
- **Allowed in Domain:** Standard JDK utilities (`java.util`, `java.time`, `java.math.BigDecimal`).
- **Forbidden in Domain:** All third-party frameworks and annotations (Spring, JPA/Hibernate, Lombok, Jakarta Validation, Jackson, etc.).
- Validation and immutability for Value Objects MUST use pure Java features and compact constructors.

### Dependency Direction & Ports
- **Strict Boundaries:** `domain` must never import from `application` or `infrastructure`. `application` must never import from `infrastructure`.
- **Port Compliance:** Use Cases must only interact with Outbound Ports (interfaces), never directly with concrete Infrastructure adapters or repositories.

### Domain Test Isolation
- Domain tests MUST be 100% isolated. 
- **Allowed:** JUnit 5, AssertJ, and lightweight local fakes for secondary ports (repository fakes, event fakes).
- **Forbidden:** Mockito, Testcontainers, Spring test utilities, or any infrastructure setup.

## 2. Structural & CQS Architecture Gate

### Query Services & Read Models
- Query Services (`*QueryService`) in the Domain act strictly as **pass-through interfaces**.
- The Infrastructure module is solely responsible for executing optimized queries (native SQL, projections) and instantiating the immutable Domain `Read Model` records directly.

### Use Case Command Returns
- Use Cases (`*UseCase`) MUST return either `void` or a minimal identifier (`UUID`, `Long`, or a lightweight immutable ID record like `OrderId`).
- Returning a heavier object or execution report is a strict exception requiring explicit user validation.
- **Strict Rule:** A Use Case MUST NEVER return a Read Model.

### Rich Domain vs. Anemic Domain
- All business rules, calculations, and state validations must live inside Domain Entities or Value Objects. 
- Use Cases only orchestrate domain calls and trigger events; they must NOT contain operational business logic.

## 3. Code Quality & Java 21 Standards Gate

### Defensive Design
- `Objects.requireNonNull()` MUST be systematically applied to all reference parameters in public constructors and public methods.
- Postcondition checks are not required if your code structure already guarantees non-null returns via `Optional`.

### Data Carriers vs. Rich Objects
- Java `record` types MUST be used for immutable data carriers: DTOs (Application), Command execution inputs, and Read Models (Domain).
- Standard mutable classes MUST be used for Rich Domain Entities to allow state mutation via business methods.

### Resource & Complexity Management
- Connections, streams, or I/O operations MUST utilize try-with-resources blocks.
- **Cognitive Complexity (20-line soft limit):** Break down any method exceeding 20 logical code lines (excluding Javadoc) or containing nested loops/conditionals into single-responsibility private methods.

## 4. Test Adequacy & Documentation Gate

### Javadoc & Documentation
- **Public scope (mandatory):** Every public class, interface, and method MUST have a concise Javadoc with complete `@param` and `@throws` tags.
- **Private scope (optional):** Javadoc is optional and reserved for non-obvious code paths.

### Requirement-Driven Test Coverage
- Coverage is driven by requirement completeness, not numerical targets.
- Tests MUST cover all happy paths, error flows, and business invariants. Missing tests on edge cases (nulls, empty strings, negative numbers) or invariant violations are blocking issues.
- Do NOT test private constructors designed to hide instantiation logic.
- Integration tests (Infrastructure) must cleanly wipe/reset database states between runs to avoid state pollution.

## 5. Review Execution Protocol

When executing a self-review, your response MUST explicitly list:
1. 🟢 **Passed Gates:** Brief confirmation of sections that perfectly comply.
2. ⚠️ **Refactoring Opportunities:** Cleaner or more efficient alternatives (does NOT block merging).
3. ❌ **Blocking Issues:** Any violation of the rules above. Requires rolling back to the TDD Loop before requesting a merge.