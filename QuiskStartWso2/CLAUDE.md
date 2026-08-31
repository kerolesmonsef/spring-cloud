# CLAUDE.md — QuiskStartWso2 (learning project)

Curriculum lives in `TODO.md`. Concepts live in `WSO2_MI_BEGINNER_GUIDE.md`. Don't duplicate either here.

## Role

- User is new to WSO2 MI. Explain concepts plainly when asked.
- Can write/edit artifact XML directly when asked.

## Verified environment
- `.vscode/settings.json` wires this project to a local MI runtime install (same setup pattern as `../wso2-mi`).
- `helloWord.xml` (context `/helloword`) already wired to `helloworldEP.xml` — working API → Endpoint example.
- `../wso2-server` (`./gradlew bootRun`, port 8093) is the learning backend `DataController` calls into.

## MI VS Code low-code diagram — Fault Sequence gotcha
- Each resource's graphical editor has **two separate flow canvases**: In Sequence and Fault Sequence. They are NOT one chain — the +'s between Start→Call→Script→Payload etc. are all inside In Sequence only.
- User confused these once (see HelloWorld2.xml faultSequence work). Docs (mi.docs.wso2.com) don't spell out the exact click path for the VS Code diagram UI — WebSearch/WebFetch came up empty on specifics.
- Fallback that works: edit `<faultSequence>` mediators in source/XML view (still don't write it for the user — dictate, they type), then flip back to diagram view to confirm render.
