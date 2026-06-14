# Examples

Input: "The user wants to export their contacts list to CSV"

Output:
Three files, one per module : one for the domain, one for the application, one for the infrastructure.

file `docs/issues/export-contacts/domain_export-contacts-issue.md`
```markdown
# Export Contacts List : Domain Module impact
**Context**
The user wants to export their contacts list to CSV to facilitate sharing and backing up their data.

**Acceptance Criteria**
Feature: Export contacts list
    In order to share or backup contacts
    As a user
    I want to export my contacts to CSV

1. Scenario: Successfully export contacts
    Given an authenticated user with 20 contacts
    When executing a query to fetch contacts
    Then the system retrieves all 20 contacts and generates an export DTO

2. Scenario: No contacts to export
    Given an authenticated user with no contacts
    When executing a query to fetch contacts
    Then the system returns an empty export result
```

file `docs/issues/export-contacts/application_export-contacts-issue.md`
```markdown
# Export Contacts List : Application Module impact
**Context**
The user wants to export their contacts list to CSV.
**Acceptance Criteria**
Feature: Export contacts list
1. Scenario: Successfully export contacts
    Given an authenticated user with 20 contacts
    When calling the GET /contacts/export endpoint with a MIME type of text/csv
    Then the application layer processes the request and returns a CSV file with all contacts
2. Scenario: No contacts to export
    Given an authenticated user with no contacts
    When calling the GET /contacts/export endpoint with a MIME type of text/csv
    Then the application layer returns a 204 No Content response
```

file `docs/features/export-contacts/infrastructure_export-contacts-issue.md`
```markdown
# Export Contacts List : Infrastructure Module impact
**Context**
The user wants to export their contacts list to CSV.
**Acceptance Criteria**
Feature: Export contacts list
1. Scenario: Transform export DTO to CSV format 
    Given an export DTO containing 20 contacts
    When transforming the DTO to CSV format
    Then a valid CSV file is generated as stream of bytes with all contact details
```