# Reference — create-issue

## Output format

Each issue is a standalone Markdown file with the following sections:

| Section | Required | Description |
|---|---|---|
| `# Title` | ✅ | Concise title of the issue. Include the impacted module name when the feature spans multiple modules. |
| `**Context**` | ✅ | Functional context explaining *why* the issue exists and *who* it concerns. |
| `**Problem**` | ✅ | Short description of the bug or gap to fix. |
| `**Acceptance Criteria**` | ✅ | A Gherkin `Feature:` block with 1–N `Scenario:` entries covering the happy path and edge cases. |
| `**Implementation Plan**` | ✅ | Planned technical steps required to resolve the issue. |
| `**Gherkin Scenarios**` | ✅ | The issue template places the `Feature:` block here; include the full Gherkin scenario set. |
| `**Notes**` | ❌ | Optional references, constraints, or open questions. |

## File naming convention

Use `docs/issues/{feature_name}/{module_name}_{issue_title}.md`.

## Validation

Run `python ./scripts/validate_issue_format.py <issue_file>` to validate the issue structure and Gherkin syntax.