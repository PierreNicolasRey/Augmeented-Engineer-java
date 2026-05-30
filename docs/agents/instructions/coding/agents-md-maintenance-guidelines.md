# Agent Instructions Maintenance Guidelines

This document governs how the AI Agent must maintain, update, and refactor the repository's prompt instructions files (`AGENTS.md`, `*-guidelines.md`). Follow these rules strictly whenever you are asked to review or update an instructions file.

## Core Directives for Updates

- **Keep it Actionable:** Instructions must use the imperative mood ("Use", "Implement", "Avoid"). Do not use ambiguous words like "should", "might", or "possibly".
- **Strict Separation of Concerns:** Each guidelines file must stay within its boundary.
  - Do NOT put Java syntax details in `git-guidelines.md`.
  - Do NOT put Spring Framework restrictions in `agents-md-maintenance-guidelines.md`.
- **Enforce Context Minimization:** Keep all instructions highly dense and under 1,000 lines per file. Never add meta-commentary, introductory fluff, or external book/article references that bloat the context window.
- **No Self-Contradiction:** Before writing a new rule, scan existing instructions to ensure it does not create a conflict or ambiguity.
- **Language Rule:** Write all instructions in English only. Never mix languages.

## Markdown Format Specifications

Every custom instruction file MUST follow this structure:
1. **Title:** Single `#` heading reflecting the scope.
2. **Context/Overview:** Max 2-3 sentences explaining the "why".
3. **Explicit Rules:** Use bullet points or numbered lists with bold keywords to highlight constraints. Write all rules in imperative mood ("Use", "Avoid", "Never", "Always", "Implement"). Never use conditionals or ambiguous language.
4. **Concrete Snippets:** Provide real, minimalistic code or file architecture examples instead of abstract definitions.

## Validation & Change Process

**Mandatory Preview Rule:**
- Present a single, unified proposal plan before executing any modifications to instruction files.
- For each file affected, list WHAT changes and WHY the change is necessary.
- Never write final content or apply edits until the User validates the complete plan.

**The Proposal Plan Structure:**
- Organize by file—identify all impacted files, not just the primary target.
- For each file: state the section, the action, and the business or architectural justification.
- Use a structured format (table or list) for clarity.
- Include cross-file dependencies explicitly (see "Dependency Management" below).

---

## Scope & Consistency Rules

**Syntactic & Style Consistency:**
- Maintain uniform imperative style across all guidelines files (use "Use", "Avoid", "Never", "Always", "Implement"; never use conditionals or "when...then" patterns).
- Keep consistent code example density and complexity level across all files.
- Enforce the same documentation template structure across all guidelines: Title → Overview → Explicit Rules → Concrete Snippets.

**Empty File Treatment:**
- Ignore all currently empty guidelines files until the User explicitly requests they be filled.
- Never reference empty files in proposals or change plans.

**AGENTS.md as Source of Truth:**
- Treat `AGENTS.md` as the canonical reference for Architecture, Design Principles, and Core Guidelines.
- `AGENTS.md` is NOT immutable: if an inconsistency or improvement opportunity is detected, include a fix proposal in the change plan.
- All other guidelines files must align with and never contradict the Design Principles stated in `AGENTS.md`.

---

## Dependency Management

**Cross-File Dependencies:**
- When a rule change in one guidelines file affects content or meaning in another file, include all impacted files in the proposal plan.
- Document explicitly how the change propagates (e.g., "Updating Design Principles in AGENTS.md requires updating testing patterns in domain-testing-guidelines.md").
- Synchronize all related files in a single execution—do not apply partial changes across multiple turns.

**Example Cross-Dependency Scenario:**
If a new Design Principle is added to `AGENTS.md`, scan all testing, documentation, and coding guidelines to identify affected patterns and include updates for all impacted files in the proposal plan.

---

## Project Evolution (Impact Analysis Pattern)

**Major Project Changes (e.g., Package Renaming):**
- Before applying any global refactoring that affects instruction content (package names, module paths, architectural patterns), execute an Impact Analysis:
  1. List every file containing the obsolete value or pattern.
  2. Identify which sections in each file require updates.
  3. Propose a global synchronization plan covering ALL affected files.
  4. Present this plan to the User for validation before applying any changes.

**Verification After Impact Analysis:**
- Use search tools to confirm all occurrences are addressed.
- Ensure no stray references to the old pattern remain in documentation.

---

## Maintenance Workflow

When the User requests modifications to an instruction file (Entering the 🏗️ phase):

1. **Analyze Phase (🔎):** 
   - Evaluate the request against current architectural context.
   - Scan all guidelines files for dependent rules or examples that may be affected.
   - Check if changes to `AGENTS.md` trigger cascading updates in other files.

2. **Proposal Phase (🗒️):**
   - Prepare the unified proposal plan (see "Validation & Change Process").
   - Present it to the User for approval before writing any content.

3. **Implementation Phase (After User Approval) (✏️):**
   - Apply all approved changes simultaneously across all affected files.
   - Maintain syntactic consistency with existing guidelines (imperative mood, code example style, structure).
   - Verify all package names, directory paths, and architectural patterns match the exact project configuration.

4. **Verification (🔎):**
   - Retain the Critical Context Markers (Emojis stack) defined in the primary `AGENTS.md` unchanged.
   - Confirm no self-contradictions exist between files.
   - Ensure no references to empty or non-existent guidelines.
   - Validate that all package patterns and directory trees match the exact project configuration (`com.exalt.it.belair`).