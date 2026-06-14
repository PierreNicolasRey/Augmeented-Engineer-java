# Reference — create-ticket

## Output format

Each ticket is a standalone Markdown file with the following sections:

| Section | Required | Description |
|---|---|---|
| `# Title` | ✅ | Concise title of the ticket. Include the impacted module name when the feature spans multiple modules. |
| `**Context**` | ✅ | Functional context explaining *why* the ticket exists and *who* it concerns. |
| `**Problem**` | ✅ / ❌ | Required for bug tickets; optional for feature tickets when the need can be described as a gap or opportunity. |
| `**Acceptance Criteria**` | ✅ | A Gherkin `Feature:` block with 1–N `Scenario:` entries covering the happy path and edge cases. |
| `**Implementation Plan**` | ✅ | Planned technical steps required to resolve the ticket. |
| `**Gherkin Scenarios**` | ✅ | The ticket template places the `Feature:` block here; include the full Gherkin scenario set. |
| `**Notes**` | ❌ | Optional references, constraints, or open questions. |

## File naming convention

Use `docs/issues/{feature_name}/{module_name}_{ticket_title}.md` for bug tickets.
Use `docs/features/{feature_name}/{module_name}_{ticket_title}.md` for feature tickets.

## Validation

Run `python ./scripts/validate_ticket_format.py <ticket_file>` to validate the ticket structure and Gherkin syntax.