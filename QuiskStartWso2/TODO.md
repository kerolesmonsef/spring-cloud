# WSO2 MI Learning TODO — QuiskStartWso2

## Phase 1: Vocabulary
- [ ] Understand API vs Endpoint vs Sequence vs Mediator
- [ ] Understand In/Out/Fault sequence model
- [ ] Read `WSO2_MI_BEGINNER_GUIDE.md`

## Phase 2: What's already here
- [x] `helloWord.xml` — API calling backend via `helloworldEP.xml` endpoint
- [ ] Trace `helloWord.xml` request flow end to end (client → API → endpoint → backend)
- [ ] Explain what `helloworldEP.xml` points to

## Phase 3: Static response, no backend
- [x] Generate `HelloWorld2.xml` skeleton via `MI: Create API`
- [x] Add `payloadFactory` returning static JSON in `inSequence`
- [x] Add `respond`
- [x] Deploy, hit `GET /hello2`, confirm static JSON response

## Phase 4: Real backend integration
- [x] Point an MI API at `../wso2-server` endpoints
- [x] Add `payloadFactory` to reshape a response
- [x] Add `filter`/`switch` conditional routing
- [x] Add a real `faultSequence`, force an error, confirm it's caught

## Phase 4.5: More everyday mediators
- [ ] `enrich` — preserve original payload before reshaping
- [x] `foreach` — loop array response, transform each item
- [ ] `validate` — reject malformed request body
- [ ] `dblookup` — read from a DB in-flow
- [ ] Reusable named `sequence` — extract shared fault-handling logic

## Phase 5: Packaging
- [ ] Package as `.car`
- [ ] Deploy into MI's `carbonapps/` folder, confirm hot-deploy in logs

## Phase 6: Enterprise integration patterns
- [ ] Message transformation (deeper: XSLT/data mapper, not just payloadFactory)
- [ ] Protocol bridging (HTTP/JMS/FTP/etc)
- [ ] Proxy services
- [ ] Enterprise connectors (SAP, Salesforce...)
- [ ] Transform/iterate payload array of objects `[{...},{...}]`
