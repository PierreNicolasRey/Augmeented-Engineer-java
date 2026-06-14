--- 
name: create-issue
description: Create an issue in the form of a markdown file with title, description, implementation plan, and Gherkin test scenarios from a functional request. Use when needing structured, testable issues.
---

# Instructions
1. Extract context and success criteria from the request  
2. Ask 2-3 questions to clarify the request if necessary
3. Identify impacted modules. If more than one module is impacted, you MUST generate one issue per module. For each module : 
    1. Summarize the context specific to the module
    2. Identify specific success criteria for the module
    3. Generate a concise title and structured description.
    4. Produce 1..N Gherkin scenarios covering happy path and edge cases.
    5. Create the issue in the `docs/issues/{feature_name}/{module_name}_{issue_title}.md` file using the `templates/issue.md` template.
    6. Validate the issue using `scripts/validate_issue_format.py`.
    
# Note
- This skill is intended to create manageable issue. Typically, it should not span more than one module. 
- If the request is too broad, propose the user to break it down per module