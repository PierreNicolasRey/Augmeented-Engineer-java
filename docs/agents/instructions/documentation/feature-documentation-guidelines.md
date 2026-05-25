# Feature Documentation Guidelines

Use this template to document implemented features in `docs/features/implemented-features-documentation.md`.

## Feature name

Short, descriptive name of the feature.

## Summary

1–3 sentence description of the feature, the business problem it solves, and why it exists.

## Status

- **Design:** Feature is being designed but not yet implemented.
- **Implementing:** Feature is actively being built.
- **Stable:** Feature is complete and tested, ready for production use.
- **Deprecated:** Feature is no longer recommended; document migration path if applicable.

## Public API / Contracts

List all public interfaces exposed by the feature:
- REST endpoints (HTTP method, path, description).
- DTOs (request/response shapes with links to code).
- Domain ports (interfaces the feature depends on).
- Domain events published by the feature.

Use markdown links to point to the actual code where contracts are defined.

## Quick usage

Provide minimal, concrete examples showing:
- A typical HTTP request and response.
- Any configuration or setup required.
- Common error scenarios and how to handle them.

Keep examples focused and directly relevant to using the feature.

## Design decisions

Document key choices made during implementation:
- **Decision:** What was chosen and why.
- **Trade-offs:** What was considered but rejected, and the rationale.
- **Rationale:** Why this approach fits the overall architecture (Hexagonal, CQS).

Examples:
- Why a specific Use Case pattern was chosen over a simpler service.
- Why this data was denormalized in the Persistence layer.
- Why events were chosen for cross-cutting concerns instead of direct calls.

## Tests & validation

Describe test coverage for the feature:
- Where integration tests live (module and file path).
- Key scenarios covered (happy path, error cases, edge cases, invariant violations).
- Any manual validation steps or acceptance criteria.

## Related files

Link to the primary implementation files organized by module:
- **Application:** REST controller, DTOs, mappers.
- **Domain:** Use Cases, Query Services, Domain Models, Events, Ports.
- **Infrastructure:** Persistence adapters, Repository implementations, Event handlers.

## Changelog

Track changes to the feature with dates and descriptions:
- `YYYY-MM-DD` — Summary of change (e.g., "Added X field to API", "Fixed Y edge case").

## Notes

Any additional context, known limitations, or future improvements worth mentioning.