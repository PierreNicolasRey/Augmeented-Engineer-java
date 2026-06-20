--- 
name: create-ticket
description: Create a ticket in the form of a markdown file with title, description, implementation plan, and Gherkin test scenarios from a functional request. Use when needing structured, testable tickets.
---

# Instructions
1. Extract context and success criteria from the request  
2. Ask 2-3 questions to clarify the request if necessary
3. Identify impacted modules. If more than one module is impacted, you MUST generate one ticket per module. For each module : 
    1. Summarize the context specific to the module
    2. Identify specific success criteria for the module
    3. Generate a concise title, structured description, and implementation plan.
    4. Produce Gherkin scenarios covering the feature thoroughly (see **Gherkin Coverage Guidelines** below).
    5. Create the ticket in the `docs/{ticket_type}/{feature_name}/{module_name}_{ticket_title}.md` file using the `templates/ticket.md` template.
       - Use `ticket_type = issues` for bug tickets.
       - Use `ticket_type = features` for feature tickets.
    6. ALWAYS validate the ticket using `scripts/validate_ticket_format.py`.
    
# Gherkin Coverage Guidelines

Gherkin scenarios must be comprehensive and cover happy paths, error cases, edge cases, and verification aspects. The number of scenarios depends on feature complexity.

## Coverage Categories

For each ticket, ensure you cover:

### Functional Scenarios
- **Happy path**: Normal, expected behavior with valid inputs
- **Empty/minimal case**: No data, empty lists, zero values (if applicable)
- **Full/maximal case**: Multiple items, large lists, all fields populated (if applicable)

### Error Scenarios
- Handle error cases relevant to the feature (failures, invalid operations, etc.)

### Edge Cases
- Boundary conditions and special cases specific to the feature

### Persistence & Events
- Verify data integrity and side effects (if applicable)

### Concurrency & Idempotency
- Verify behavior under concurrent access and retry scenarios (if applicable)
    
# Note
- This skill is intended to create manageable tickets. Typically, it should not span more than one module. 
- If the request is too broad, propose the user to break it down per module