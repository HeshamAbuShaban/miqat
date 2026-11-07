# ZCode playbook — how this project uses the environment

> For any agent (and for Hesham): which environment capability to reach for at each
> stage. Keep it current as the ladder advances.

| Stage / need | ZCode capability | How we use it |
|---|---|---|
| Kick off any ladder step (S1, S2, …) | **Plan mode** | Discuss + get his approval before touching code — the ladder step IS the plan |
| Design work (S5) | **Visualize** | Interactive HTML mockups of home / alarm / widget in the visualization panel; Hesham reviews and iterates BEFORE any Compose is written |
| Donor deep-dives (qibla columns, AdhanService internals, ummalqura tables) | **Explore agents** | Fan out over `donor/extracted-code/` without flooding the main session context; agents return conclusions with file:line cites |
| Running the app | **Connected emulator** (`emulator-5554`) | Build → install → screenshot → review loop inside the session; donor is never executed, our build is |
| First Gradle build / big downloads | **Idle-time tasks** | Defer the multi-hundred-MB first build to off-peak with his OK (bandwidth discipline, same as the lab) |
| Repeated mechanical work (parity across 10+ methods, mass mockup variants) | **Workflows** | Only on his explicit request — never default to it |
| Long-running scheduled needs (nightly parity regression, reminders) | **Automations** | Only when he asks for scheduled work |
| Context across sessions | **Memory + `AGENTS.md` + `docs/journal.md`** | The contract carries state — a new session needs one sentence to start, never a re-briefing |
| Reviewing long docs (TEARDOWN, parity) | **Document viewers** | Open markdown/office artifacts in side panels for joint review |

Rules of thumb:

1. Default = direct work in-session. Agents/workflows/automations are opt-in tools,
   not the default posture.
2. Anything touching his devices (POCO/emulator) follows the lab's explicit-go pattern.
3. After every session: `docs/journal.md` entry + ROADMAP statuses (AGENTS.md §protocol).
