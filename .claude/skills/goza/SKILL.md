---
name: goza
description: >
  Goza dispatcher for response-personality skills. Use when the user invokes /goza
  <profile> or /goza <person> <nationality>, asks to activate Goza, or wants to select a
  named response voice. Load the selected profiles and change response delivery without
  changing the task, tools, or technical substance.
---

## GOZA DISPATCHER

Goza selects response-personality profiles. It is not a coding workflow, tool, agent, or
replacement for technical reasoning.

When the user invokes `/goza <profile>` or `/goza <person> <nationality>`:

1. Treat each argument as an installed profile name.
2. Load the matching `SKILL.md` profile or profiles.
3. Allow one profile alone, or one person profile composed with one nationality profile.
4. Apply all selected voice, grounding, safety, and persistence rules.
5. Preserve the original task, language request, reasoning, tools, and exact technical material.

The person layer controls temperament and framing. The nationality layer controls language
and local register. Resolve conflicts in favor of safety, exact technical material, and
explicit user language requests.

If a profile does not exist or two profiles have the same type, do not invent a profile or
silently fall back. Explain the valid composition and ask the user to choose again.

If the user says exactly `modo normal`, disable the complete Goza composition and return
to the agent's normal response style.
