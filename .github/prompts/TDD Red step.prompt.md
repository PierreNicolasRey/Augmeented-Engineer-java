---
agent: agent
name: TDD Red step
description: This prompt is used to implement one test scenario that fails in a TDD workflow for an AI agent
argument-hint: Implement the following test scenario in a TDD workflow for an AI agent: {scenario_description}
tools: ['execute/getTerminalOutput', 'execute/runInTerminal', 'read/problems', 'read/readFile', 'read/terminalSelection', 'read/terminalLastCommand', 'edit/createDirectory', 'edit/createFile', 'edit/editFiles', 'search', 'upstash/context7/*', 'todo']
model: Claude Haiku 4.5 (copilot)
---

# Red TDD step prompt

## Instructions
1. Analyze the provided scenario description carefully.
    - If the scenario description is provided as an issue reference, retrieve the issue content and extract the specified scenario.
    - If the scenario description is provided directly, use it as is.
2. Check if a test file already exists for the scope of this test scenario. 
   - If it exists, append the new test case to the existing file.
   - If it does not exist, create a new test file in the appropriate directory structure based on the module (domain, application, infrastructure). 
3. Write the test case so it accurately reflects the scenario and is expected to fail initially. You MUST Follow the testing guidelines for the module you are currently working on : 
    - global testing guidelines are in `docs/agents/instructions/testing/testing-guidelines.md`
    - for the domain module, follow the guidelines in `docs/agents/instructions/testing/domain-testing.instructions.md`
    - for the application module, follow the guidelines in `docs/agents/instructions/testing/application-testing.instructions.md`
    - for the infrastructure module, follow the guidelines in `docs/agents/instructions/testing/infrastructure-testing.instructions.md`
4. If the test triggers compilation errors because the target production classes, interfaces, or methods do not exist yet, you MUST create their empty skeletons (stubs) in the main production source tree (`src/main/java`). 
   - Skeletons must contain ONLY the class/interface declaration and empty method signatures (returning default values like `null`, `0`, or throwing `UnsupportedOperationException`).
   - Do NOT implement any business logic.
5. If the test needs a Fake implementation, create a new Fake class in the appropriate directory structure based on the module (domain, application, infrastructure) and implement the necessary methods to support the test case. Do **NOT** implement the Fake inside the test file itself.
6. Run the test to confirm it compiles and fails.

## Requirements
- You **MUST** follow the guidelines for the module you are currently working on.
- **CRITICAL** **NEVER** implement any production logic or business features in this step. Skeletons and structural method signatures created in `src/main/java` purely to resolve Java compilation errors are **NOT** considered production logic. Your ONLY goal is to achieve a compiling project with a failing test (RED execution).
- You **MUST** ensure the test fails when executed. 
- The name of the test method should be descriptive and follow the naming conventions outlined in the testing guidelines.

## Examples

### Domain test example : file does not yet exist

Input : 
Scenario Description: Scenario: Successfully export contacts
Given a user with 20 contacts
When executing a query to fetch contacts
Then the system retrieves all 20 contacts and generates an export DTO 

Expected Output : 

- a new file `domain/src/test/java/com/example/domain/contact/ContactExportUseCaseTest.java` is created with the following content : 

```java
package com.example.domain.contact;

import com.example.domain.test.fixture.ContactExportFixture;
import com.example.domain.test.state.TestState;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import static org.assertj.core.api.Assertions.assertThat;

class ContactExportUseCaseTest {

    private ContactExportFixture fixture;

    @BeforeEach
    void setUp() {
        fixture = new ContactExportFixture();
    }

    @Test
    void shouldProduceExportDtoWhenContactsExist() {
        // Given a user with 20 contacts
        TestState<User> userTestState = fixture.getUserTestState();
        TestState<Contact> contactTestState = fixture.getContactTestState();
        UseCaseHandler<ExportContactQuery, ContactExportDto> handler = fixture.getUseCaseHandler();
        
        User user = new User("user1");
        userTestState.add(user);
        List<Contact> contacts = IntStream.range(0, 20)
                .mapToObj(i -> new Contact("Contact " + i, "contact" + i + "@example.com"))
                .collect(Collectors.toList());
        contacts.forEach(contactTestState::add);

        // When executing a query to fetch contacts
        ExportContactQuery query = new ExportContactQuery(user.getId());
        ContactExportDto exportDto = handler.execute(query);

        // Then the system retrieves all 20 contacts and generates an export DTO
        assertThat(exportDto).isNotNull();
        assertThat(exportDto.getContacts()).hasSize(20);
    }
}
```