# Examples

## Example 1: Feature "Export Contacts to CSV"

Input: "The user wants to export their contacts list to CSV"

Output: Three files, one per module

### ✅ GOOD EXAMPLE - Domain Ticket (Comprehensive)

file `docs/features/export-contacts/domain_export-contacts.md`
```markdown
# Export Contacts List : Domain Module impact

**Context**
The user wants to export their contacts list to CSV so they can share or back up their data.

**Problem**
The Domain layer must support building an export-ready model that includes all required contact fields and handles edge cases gracefully.

**Acceptance Criteria**
- The domain layer provides a ContactExportQuery service
- The service returns an immutable ExportReadModel with all contact fields
- Empty contact lists are handled gracefully (returns empty list, not error)
- Archived/inactive contacts are excluded from export

**Implementation Plan**
1. Create `ContactExportReadModel` immutable record
2. Create `ContactExportQueryService` with export query logic
3. Add unit tests for populated, empty, and filtered contact lists

**Gherkin Scenarios**
Feature: Export contacts list

Scenario: Export contacts with populated list
  Given a user with 20 active contacts
  When the domain export query is executed
  Then the system returns ExportReadModel with all 20 contacts
  And each contact includes all required fields (name, email, phone)

Scenario: Export with empty contact list
  Given a user with no contacts
  When the domain export query is executed
  Then the system returns ExportReadModel with empty list
  And no error is thrown

Scenario: Export excludes archived contacts
  Given a user with 10 active and 5 archived contacts
  When the domain export query is executed
  Then only the 10 active contacts are included in the result
  And archived contacts are filtered out

**Notes**
- Export is read-only; no state changes
- ReadModel is immutable for thread-safety
```

### ✅ GOOD EXAMPLE - Application Ticket (Comprehensive)

file `docs/features/export-contacts/application_export-contacts.md`
```markdown
# Export Contacts List : Application Module impact

**Context**
The API must expose a CSV export endpoint that maps HTTP requests to domain export flow and returns properly formatted CSV content.

**Problem**
The Application layer must provide a REST endpoint for contact exports with appropriate error handling and response formatting.

**Acceptance Criteria**
- HTTP endpoint created: `GET /api/v1/contacts/export?format=csv`
- Request parameter: `format` (optional, defaults to csv)
- Response includes `Content-Type: text/csv` and `Content-Disposition` headers
- Success response (HTTP 200) with CSV payload
- Error handling for unauthenticated requests (401), invalid parameters (400)

**Implementation Plan**
1. Create controller endpoint in `ContactController`
2. Map request to domain `ContactExportQueryService`
3. Format response with proper CSV headers
4. Add integration tests for success and error cases

**Gherkin Scenarios**
Feature: Export contacts via API

Scenario: Successfully export contacts as CSV
  Given an authenticated user with 10 contacts
  When calling GET /api/v1/contacts/export?format=csv
  Then HTTP 200 OK response is returned
  And Content-Type header is text/csv
  And response body contains CSV formatted contacts

Scenario: Export with no contacts
  Given an authenticated user with no contacts
  When calling GET /api/v1/contacts/export?format=csv
  Then HTTP 200 OK response is returned
  And response body is CSV header only (empty data)

Scenario: Invalid format parameter
  Given a request with format=xml
  When calling GET /api/v1/contacts/export?format=xml
  Then HTTP 400 Bad Request is returned
  And error message indicates format must be csv

Scenario: Unauthenticated request
  Given an unauthenticated user
  When calling GET /api/v1/contacts/export?format=csv without auth token
  Then HTTP 401 Unauthorized is returned

**Notes**
- CSV format is the only supported export format for this version
- Empty contact list still returns valid CSV (header only)
```

### ✅ GOOD EXAMPLE - Infrastructure Ticket (Comprehensive)

file `docs/features/export-contacts/infrastructure_export-contacts.md`
```markdown
# Export Contacts List : Infrastructure Module impact

**Context**
The infrastructure layer must transform domain ExportReadModel into valid CSV content streamed over HTTP.

**Problem**
The Infrastructure layer needs CSV serialization logic and proper stream handling to deliver contact exports.

**Acceptance Criteria**
- CSV transformer converts ExportReadModel to valid RFC 4180 CSV format
- Header row includes all contact field names
- Data rows include all contact values with proper escaping
- Large contact lists (1000+) are streamed without loading into memory
- Integration tests verify CSV structure and content

**Implementation Plan**
1. Create `ContactExportCsvTransformer` class
2. Implement CSV serialization with proper escaping
3. Configure streaming response in Spring
4. Add integration tests with sample data sets

**Gherkin Scenarios**
Feature: Transform contacts to CSV

Scenario: Transform contacts to valid CSV
  Given ExportReadModel with 5 contacts
  When transformer converts to CSV
  Then valid RFC 4180 CSV is produced
  And header row contains field names
  And each data row has matching fields

Scenario: Handle special characters in CSV
  Given a contact with name "Smith, John" and email "john@example.com"
  When transformer converts to CSV
  Then the name is properly escaped with quotes
  And the CSV remains valid and parseable

Scenario: Large export streams without memory issues
  Given ExportReadModel with 10000 contacts
  When transformer converts to CSV
  Then streaming produces output without loading all rows in memory
  And response completes successfully

**Notes**
- CSV serialization follows RFC 4180 standard
- Special characters are escaped with double quotes
- Large exports use streaming to minimize memory usage
```

---

## Example 2: ❌ BAD EXAMPLE - Insufficient Coverage

### ❌ Domain Ticket (Incomplete - Missing Edge Cases)

file `docs/features/order-status/domain_order-status.md`
```markdown
# Update Order Status : Domain Module

**Context**
Orders need to transition between states.

**Problem**
Domain must support status transitions.

**Acceptance Criteria**
- Order status can be updated
- Status transitions are persisted

**Implementation Plan**
1. Add status field to Order
2. Add updateStatus method

**Gherkin Scenarios**
Feature: Update order status

Scenario: Update order status
  Given an order
  When status is updated to READY
  Then status is READY

**Notes**
None
```

**Problems with this example:**
- ❌ Only 1 scenario (happy path only)
- ❌ Missing error cases: What if status invalid? What if order not found?
- ❌ Missing edge cases: What if status transition is illegal?
- ❌ Missing state verification: Are other fields affected?
- ❌ No timestamp verification: When was it updated?
- ❌ Too vague: "an order" - no concrete ID or state
- ❌ Implementation plan is trivial - no specifics

**How to improve:**
- Add 3-4 error scenarios (not found, invalid transition, etc.)
- Add edge case (null status, empty string)
- Verify persistence and timestamps
- Verify state machine rules (e.g., PENDING → ACKNOWLEDGED → READY, but not READY → PENDING)
- Add concrete example data