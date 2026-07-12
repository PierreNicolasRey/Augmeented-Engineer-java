# Custom agents workflow issues

## Implemeting new scenario when production code already exists

When production code already exists for a scenario close to the one currently implemented, the RED agent tends to :
- Add only the new test for the new scenario and consider its step done even though the new test in GREEN
    - Solution : reinforced the rules to ensure a failing test on RED phase.

- Use already existing production code instead of creating inner classes
    - Solution : reinforced the rules to ensure the creation of inner classes in RED phase.

In the same context, for the GREEN agent :
- Makes the test passing by using production code even though the inner classes creation were enforced
    - Solution : reinforced the rule so that the GREEN agent ONLY use inner class to make the test pass.

In the same context, for the REFACTOR agent :
- Tends to duplicate production code when refactoring a new implemented scenario
    - Solution 1 : reinforced the rules to check the pre-existence of production code that should be used for the new implemented scenario.
    - Solution 2 : reinforced the rules to check existing files not only by names but also by content. 
    Ex : pre-existing OrderStatusEnum with PENDING, RED and GREEN worked with a "non-following-naming-convention" OrderStatus with CANCELLED : should be merged (and following naming conventions).


## Fighting anticipation
For the RED agent, anticipation was an issue.
- When implementing a new scenario, this agent tends to anticipate by creating more inner classes than necessary like custom exceptions not covered by this scenario but by another or implementing inner class whose methods are used in the THEN and are unreachable
    - Solution : reinforced the anti-anticipation rules to ensure the production of was is needed ONLY.
    - Solution : reinforced the anti-anticipation rules to ensure production of GIVEN and WHEN code only without anticipating on the THEN.

Note : The GREEN and REFACTOR agents handle this anticipation problem better, as the GREEN agent only works with what the RED gave it and the REFACTOR, with what the GREEN gave it.


## Refactoring clean-up
RED and GREEN agents tends to comment their inner classes.
- When REFACTOR agent take its turn, it sometimes leave behind comments that should be cleaned up. 
Ex : Lone "// ------------- INNER CLASS ---------------" that should not be here when REFACTOR extracted all inner classes.
    - Solution : reinforced the clean-up rule to leave only relevant comments.


## Steps workflow
In the start of the implementon of the custom agents, there were no explicit "HARD STOPS", only critical rules, emphasised words (ALWAYS, NEVER, ...) and numeroted steps, causing a deviation in effectiveness between the former prompt file into the agent file.
        - Solution : convert the existing inner steps into effective HARD STOPS allowed the agents to better follow the workflow step-by-step and checking each of these steps internally.


## Minimal implementation confusion
When RED produces a true minimal implementation, sometimes GREEN covers only this minimal implementation and not the full scenario.
Ex : RED is implementing a useCase needing a passing inner check before pursuing. It has two options :
    - implementing minimal interface with a method to return true
    - not implementing anything and leave a comment on the test that no check = passing check
Both option are valid for RED minimal implementation.
With the latest, GREEN don't know what to do with this comment and thus don't implement any check (even minimal as its not the point of the current scenario and file tested).
        - Solution : pass the name of the feature and the scenario to GREEN to from the ouput of RED so it can have the full picture, not just the test it has to make pass. 


## Personal Note
Overall, these agents, after reinforcing the rules are working well with minimal deviation (ex : single unused import in a test file).
To go further, it would be best to concise the custom-agents files, as i think the are too long, to reduce the context space taken by these files in addition with the other guidelines files BUT still ensuring that it doesn't cause workflow regression.
It would also be an improvement that the agent pass in its handoff the json output file directly to the next agent.