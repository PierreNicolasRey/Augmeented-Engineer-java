## Git Usage Guidelines

### Branching Model

We follow a structured **Feature-Branch Workflow** using a protected production branch and an active development branch. 

- **`main` Branch (Production):** - Strictly **untouchable** and protected. It represents the production-ready state.
  - Only the User (Software Engineer) is authorized to merge into `main`. The AI Agent MUST NEVER attempt or suggest direct commits or automated merges to `main`.
- **`develop` Branch (Active Integration):** - The main integration branch for all current development. 
  - All features must target this branch for integration.
- **Feature Branches (`feature/[feature-name]`):**
  - Create a dedicated branch for each specific feature or issue from `develop`.
  - Once the feature is fully implemented, thoroughly tested (TDD), and documented according to project guidelines, it can be automatically merged into `develop`.

### Branch Naming Conventions

- **Feature branches:** `feature/[short-description]` (e.g., `feature/token-transfer`)
- **Bug fixes:** `fix/[short-description]` (e.g., `fix/order-parallel-prep-time`)
- **Documentation/Chore:** `docs/[short-description]` or `chore/[short-description]`

### Conventional Commits

Follow the Conventional Commits specification:

```
<type>(<scope>): <short description>

<optional body>

<optional footer>
```

Common `type` values: `feat`, `fix`, `docs`, `style`, `refactor`, `perf`, `test`, `chore`, `build`, `ci`.

Examples:

```
feat(payment): add support for 3DS authentication

fix(order): prevent negative totals when discounts apply

docs(readme): update contribution section
```

### TDD Commit Markers

When implementing features using Test-Driven Development (TDD), explicitly mark the phase in your commit message:

- **RED phase (failing tests):** 

```
test(<scope>): <short description> (RED)


<optional body>

<optional footer>
```
  - Example: `test(order): validate token balance before placement (RED)`

- **GREEN phase (passing implementation):** 

```
feat(<scope>): <short description> (GREEN)

<optional body>

<optional footer>
```
  - Example: `feat(order): implement token balance validation (GREEN)`

- **REFACTOR phase (optimization without breaking tests):** 

```
refactor(<scope>): <short description>

<optional body>

<optional footer>
```
  - Example: `refactor(order): extract token validation to separate method`

This convention ensures the commit history explicitly traces the TDD lifecycle and makes it easy to understand which commits represent test definitions versus implementations.

### Commit Message Guidelines

- Subject line: imperative, present tense, max 72 characters.
- Body: explain the motivation and contrast with previous behavior.
- Footer: reference issues or breaking changes (e.g., `Refs #123`, `BREAKING CHANGE: ...`).

### Reverting Commits

Never rewrite history or force push (git push --force) on `main` or `develop`. If a mistake is integrated, use a revert commit:

```
git revert <commit-sha>
git push origin develop
```