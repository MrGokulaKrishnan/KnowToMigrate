# Naming

## Engine
| Name | Meaning | Where it is used |
|---|---|---|
| **KnowToMigrate** | The product | App names, package id `com.knowtomigrate.app`, website, store listing |
| **Transit Engine** | The transfer engine, replaces "Pluto Engine" | About screens, status bars, docs, the website's "Powered by" line |
| Transit Direct / Transit Wi-Fi Direct / Transit Auto | Transport modes shown to users | Transport labels in the Android and Windows apps |
| Transit Core | Transfer controller (planned subsystem name) | Docs; code still lives in `Ktm*` classes |
| Transit Link | Adaptive transport selection (planned) | Docs; code still lives in `KtmTransportManager` |
| Transit Shield | Security subsystem (planned) | Docs only until encryption ships |
| Transit Verify | Integrity checks (SHA-256 chunk digests today) | Docs; integrity text in the UI |
| Transit Recovery | Pause, resume, retry (checkpointing today) | Docs; checkpoint file `.ktm-checkpoint.json` |

## Rules
- Use "Transit Engine" only for the engine as a whole. Do not use it for the Android or Windows app.
- Do not show "Encrypted" in the UI until transport encryption is implemented. Until then the honest status is "Not encrypted yet".
- "Verified" means SHA-256 chunk digests were checked. It does not mean the connection was authenticated.
- Code identifiers keep their `Ktm` prefix until the shared core refactor. Renaming classes is a separate change that must not alter transfer behaviour.

## Packages and files
- `packages/transit-engine` (was `packages/core-engine`), package `@knowtomigrate/transit-engine`
- `apps/website/src/services/transitWebEngine.ts` (was `webTransferEngine.ts`), class `TransitWebEngine`
