# Development Workflow Guidelines

Mandatory step-by-step lifecycle for implementing any new feature using Test-Driven Development and Hexagonal Architecture.

---

## Phase 1: Feature Initialization

1. Select the feature from [FEATURES.md](../../../FEATURES.md).
2. Create a feature branch: `feature/[short-description]` from `develop` ([git-guidelines.md](./git-guidelines.md)).
3. Initialize feature documentation in [docs/features/implemented-features-documentation.md](../features/implemented-features-documentation.md) using [feature-documentation-guidelines.md](./documentation/feature-documentation-guidelines.md). Set **Status** to "Implementing".
4. Break feature into small, testable sub-tasks (< 1 hour each, independently completable).

---

## Phase 2: Incremental Sub-Task Loop

**For each sub-task, apply the TDD cycle layer-by-layer in strict order: Domain → Infrastructure → Application.**

Complete the full TDD cycle (RED → GREEN → ANALYZE → REFACTOR → CODE REVIEW) **for each layer before moving to the next layer**. Repeat mini-cycles within each layer as needed until the layer is fully covered.

### Domain Layer TDD Cycles

Repeat until Domain logic is complete:

1. **RED Phase 🔴:** Write Domain test(s) (business logic, entities, services, use cases). Reference [testing-guidelines.md](./testing/testing-guidelines.md). Commit: `test(<scope>): <short description> (RED)`.
2. **GREEN Phase 🟢:** Implement minimal Domain code to pass test(s). Follow CQS patterns ([AGENTS.md](../../../AGENTS.md#design-principles--architectural-rules)). Follow the project java guidelines ([java-coding-guidelines.md](./coding/java-coding-guidelines.md)). Run `./gradlew test`. Commit: `feat(<scope>): <short description> (GREEN)` or `fix(<scope>): <short description> (GREEN)`.
3. **ANALYZE Phase 🔎:** Review for Hexagonal alignment, CQS separation, no framework annotations in Domain.
4. **REFACTOR Phase ⚪:** Optimize if issues found. Run tests. Commit: `refactor(<scope>): <short description>`.
5. **CODE REVIEW Phase 🕵️:** Self-review correctness, Javadoc, test coverage, style ([code-review-guidelines.md](./coding/code-review-guidelines.md)). If issues found, fix and loop back to ANALYZE.
6. **Repeat steps 1-5** if more Domain logic needed.

### Infrastructure Layer TDD Cycles

Once Domain is stable, repeat the same cycle for Infrastructure (persistence adapters, event handlers, external integrations).

### Application Layer TDD Cycles

Once Infrastructure is stable, repeat the same cycle for Application (REST controllers, DTOs, mappers).

### DOCUMENTATION Phase 📚 (After All Layers Complete)

- Update [docs/features/implemented-features-documentation.md](../features/implemented-features-documentation.md):
  - **Public API / Contracts:** New endpoints, DTOs, ports, events.
  - **Design Decisions:** Why this approach fits Hexagonal Architecture.
  - **Tests & Validation:** Coverage and scenarios tested.
  - **Related Files:** Links to implementation files (controller, Use Case, Entity, Adapter, etc.).
  - **Changelog:** Dated entry for this sub-task.

### LOCAL VALIDATION & PUSH

- Run: `./gradlew clean check` (verify no regressions across all layers).
- Push all commits to remote feature branch.
- **Do NOT push until build is clean.**

---

## Phase 3: Feature Completion

1. **Global Review:** Verify code correctness, Javadoc coverage, complete documentation, test coverage, and Hexagonal consistency ([code-review-guidelines.md](./coding/code-review-guidelines.md)).
2. **Update Status:** Set feature **Status** to "Stable" in [docs/features/implemented-features-documentation.md](../features/implemented-features-documentation.md). Ensure all sections complete.
3. **Final Build:** Run `./gradlew clean build`. Verify all tests pass. Fix any issues.
4. **Merge to Develop:** Merge feature branch into `develop` ([git-guidelines.md](./coding/git-guidelines.md)).

