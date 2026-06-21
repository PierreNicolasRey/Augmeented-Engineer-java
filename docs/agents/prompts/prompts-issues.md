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

Take 4:
- Reinforce the rules to avoid anticipation like creating a prod interface when the Fake class is enough for the test to compile. Modified file : `TDD Red step.prompt.md`

Take 5:
- Force the agent to analyze its errors (Too many prod code produced instead of just skeletons). Make it rewrite the TDD Red step.prompt.md file to strictly avoid any anticipation. Modified file : `TDD Red step.prompt.md`
- Let it wrote a memory file of its bias analyze.

**TDD Refactor step prompt**
Take 1:
- Reinforce the rules so the agent follow micro-steps with verifications instead of moving/cleaning in one single batch and on single final verification. Modified file : `TDD Refactor step.prompt.md`