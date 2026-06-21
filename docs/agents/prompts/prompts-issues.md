**TDD Red step prompt**
Take 1:
- Had to add '**CRITICAL**' so the agent does not produce production code in Red step. Modified file : `TDD Red step.prompt.md`
- Had to reinforce the test naming convention rule and provide a good and bad examples. Modified file : `testing-guidelines.md`

Take 2:
- Had to precise the "Do NOT write production code" rule to allow the writing of skeleton classes without ANY business logic. Modified file : `TDD Red step.prompt.md`
- Had to add a section to precise where to write Fake implementations as the agent wrote it inside the test file and thus making it not re-usable. Modified file : `TDD Red step.prompt.md`

Take 3:
- Reinforce the rules to avoid ANY production code writing except skeletons to avoid compilation failure. Modified file : `TDD Red step.prompt.md`
- Add more examples of expected output : test file and src/main/java skeleton files. Modified file : `TDD Red step.prompt.md`