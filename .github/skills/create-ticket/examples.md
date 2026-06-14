# Examples

Input: "The user wants to export their contacts list to CSV"

Output:
Three files, one per module : one for the domain, one for the application, one for the infrastructure.

file `docs/features/export-contacts/domain_export-contacts.md`
```markdown
# Export Contacts List : Domain Module impact
**Context**
The user wants to export their contacts list to CSV so they can share or back up their data.

**Acceptance Criteria**
- The domain layer supports a contacts export request and returns an export-ready model.
- The export model includes all required contact fields.
- The export flow handles empty contact lists gracefully.

**Implementation Plan**
1. Add a domain read model for contact exports.
2. Add a domain query service that builds export-ready contact data.
3. Add domain tests for populated and empty export results.

**Gherkin Scenarios**
Feature: Export contacts list

Scenario: Successfully export contacts
Given an authenticated user with 20 contacts
When executing a domain export request
Then the system retrieves all 20 contacts and produces an export-ready model

Scenario: No contacts to export
Given an authenticated user with no contacts
When executing a domain export request
Then the system returns an empty export result
```

file `docs/features/export-contacts/application_export-contacts.md`
```markdown
# Export Contacts List : Application Module impact
**Context**
The user wants to export their contacts list to CSV through the public API.

**Acceptance Criteria**
- The application exposes a CSV export endpoint for contacts.
- The endpoint delegates to the domain export flow and returns the CSV payload.
- The endpoint handles empty contact lists with a successful response.

**Implementation Plan**
1. Add a new controller endpoint for contact exports.
2. Map incoming requests to the domain export request.
3. Add application tests for successful and empty export responses.

**Gherkin Scenarios**
Feature: Export contacts list

Scenario: Successfully export contacts via API
Given an authenticated user with 20 contacts
When calling GET /contacts/export with `Accept: text/csv`
Then the application processes the request and returns a CSV payload

Scenario: No contacts to export via API
Given an authenticated user with no contacts
When calling GET /contacts/export with `Accept: text/csv`
Then the application returns a successful response with an empty CSV result
```

file `docs/features/export-contacts/infrastructure_export-contacts.md`
```markdown
# Export Contacts List : Infrastructure Module impact
**Context**
The user wants the contact export to be transformed and streamed as valid CSV content.

**Acceptance Criteria**
- The infrastructure layer transforms export read models into valid CSV content.
- The CSV output includes all contact details in the expected columns.
- The export stream is delivered without data loss or formatting errors.

**Implementation Plan**
1. Implement a CSV transformer for contact export read models.
2. Ensure the infrastructure layer produces a valid CSV stream.
3. Add integration tests verifying CSV structure and content.

**Gherkin Scenarios**
Feature: Export contacts list

Scenario: Transform contacts export model to CSV
Given an export model containing 20 contacts
When transforming the model to CSV
Then the infrastructure produces valid CSV content with all contact details
```

file `docs/issues/export-contacts/application_export-contacts-invalid-mime-type.md`
```markdown
# Issue: Application export endpoint returns wrong MIME type
**Context**
Users request contact exports in CSV format, but the application layer must return the correct headers so browsers and clients can consume the file correctly.

**Problem**
The current CSV export endpoint returns the wrong `Content-Type`, causing some clients to reject the download or save the file with an incorrect type.

**Acceptance Criteria**
- The export endpoint responds with `Content-Type: text/csv` for CSV exports.
- The response includes a `Content-Disposition` header with a `.csv` filename.
- Non-CSV export requests continue to use the correct MIME type.

**Implementation Plan**
1. Inspect the application controller and response mapper for CSV export.
2. Fix the response headers for CSV exports.
3. Add integration tests validating the CSV response headers.

**Gherkin Scenarios**
Feature: Correct CSV headers for contact export

Scenario: Return CSV headers for export
Given an authenticated user with contacts
When calling GET /contacts/export with `Accept: text/csv`
Then the application responds with `Content-Type: text/csv` and a `Content-Disposition` header for a CSV filename
```