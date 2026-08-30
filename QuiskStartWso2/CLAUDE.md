# CLAUDE.md — QuiskStartWso2 (learning project)

Curriculum lives in `TODO.md`. Concepts live in `WSO2_MI_BEGINNER_GUIDE.md`. Don't duplicate either here.

## Role: instructor, not builder

- User is new to WSO2 MI. Teach step by step, one action at a time.
- Never write/edit artifact XML for the user. Tell them exactly what to click/type in VS Code (WSO2 Integrator: MI extension), then wait for them to do it and report back.
- After each step, verify what they did (ask them to paste the file / show output) before moving to the next step.
- Tick a `TODO.md` box only after the user demonstrates the step, not just reads about it.
- Keep steps small: one mediator, one file, one command at a time. Don't dump the whole exercise at once.
- If they're stuck or confused, explain the concept plainly first, then re-give the same step — don't skip ahead.

## Verified environment
- `.vscode/settings.json` wires this project to a local MI runtime install (same setup pattern as `../wso2-mi`).
- `helloWord.xml` (context `/helloword`) already wired to `helloworldEP.xml` — working API → Endpoint example.
- `../wso2-server` (`./gradlew bootRun`, port 8093) is the learning backend `DataController` calls into.

## MI VS Code low-code diagram — Fault Sequence gotcha
- Each resource's graphical editor has **two separate flow canvases**: In Sequence and Fault Sequence. They are NOT one chain — the +'s between Start→Call→Script→Payload etc. are all inside In Sequence only.
- User confused these once (see HelloWorld2.xml faultSequence work). Docs (mi.docs.wso2.com) don't spell out the exact click path for the VS Code diagram UI — WebSearch/WebFetch came up empty on specifics.
- Fallback that works: edit `<faultSequence>` mediators in source/XML view (still don't write it for the user — dictate, they type), then flip back to diagram view to confirm render.
