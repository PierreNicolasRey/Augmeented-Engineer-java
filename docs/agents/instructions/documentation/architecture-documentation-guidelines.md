# Architecture Documentation Guidelines

Use this template to document the system architecture in `docs/architecture/architecture-documentation.md`.

## Overview

Provide a high-level description of the system scope and non-functional goals:
- What is the system designed to do?
- Key non-functional goals (scalability, reliability, performance, security).
- The architectural style (e.g., Hexagonal, CQS-infused, event-driven).
- Context and rationale for major architectural choices.

## Modules

Describe each core module and its responsibilities:
- **Application Module:** REST API layer, request/response handling, input validation, DTO mapping.
- **Domain Module:** Business logic, use cases, domain models, services, ports definitions.
- **Infrastructure Module:** Persistence, messaging, external integrations, dependency injection configuration.

Include:
- Module boundaries and unidirectional dependencies.
- Communication flow between modules.
- Rationale for this separation.

## Data flow

Describe key request/response flows and event propagation patterns:
- A typical command flow from REST endpoint to Domain Use Case to persistence.
- A typical query flow from REST endpoint through Query Service to projection.
- Event publishing and consumption flows.
- Include sequence diagrams or narrative descriptions with references to actual code files.

## Integration points

Document external dependencies and contracts:
- Database (technology, schema, persistence strategy).
- Messaging systems (event bus, message broker, if any).
- External services and APIs (with links to contracts or OpenAPI docs).
- Authentication and authorization providers.

## Deployment

Describe the deployment topology:
- Target environment (cloud provider, on-premise, containerized, etc.).
- Service structure and scaling strategy.
- Configuration and environment variables.
- Data persistence strategy (database replication, backups, etc.).

## Security & Governance

Capture security and compliance considerations:
- Authentication mechanism and identity provider.
- Authorization rules (role-based, attribute-based, etc.).
- Data sensitivity and protection rules.
- Compliance or regulatory requirements (if any).
- Audit logging requirements.

## How to change the architecture

Outline the process for proposing and recording architectural changes:
1. Document the proposed change and its rationale.
2. Discuss trade-offs and impact on existing modules.
3. Notify relevant stakeholders and incorporate feedback.
4. Update this document with the new decision and reasoning.
5. Link to related feature documentation or design decision records (ADRs) if applicable.

## Diagrams & assets

Include or reference architectural diagrams:
- System context diagram (system boundary, external actors).
- Module/layering diagram (Application, Domain, Infrastructure).
- Data flow diagrams for key scenarios.
- Deployment topology diagram.

Store images in `docs/assets/` and reference them with clear captions explaining their purpose.