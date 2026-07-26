# SoloCraft Reawakening Repository Context

Before making changes in this repository, read these sources in order:

1. `CHAT_CHANGELOG.md` — primary record of work performed in earlier GPT sessions.
2. `.codex/context/PROJECT_CONTEXT.md` — concise project orientation and verified status.
3. `.codex/context/project_snapshot.json` — machine-readable build and repository facts.
4. `.codex/context/subsystem_status.csv` — subsystem-level implementation/status index.
5. The relevant focused design document for the task, especially `PRODUCT.md`, `DESIGN.md`, or the Dungeon Builder documents.

Treat the source code and a fresh build as authoritative when a context file becomes stale. Do not interpret a changelog entry as proof that a feature was gameplay-tested. Preserve unrelated user changes and append newly completed work to `CHAT_CHANGELOG.md` under the appropriate `Unreleased` section.

Use Java 17 and the repository Gradle wrapper. The standard verification command is:

```powershell
.\gradlew.bat build --no-daemon
```

The intended installable artifact is `build/libs/SLR1.0.0.jar`.

After every successful build that produces or updates `build/libs/SLR1.0.0.jar`, also copy that jar to the CurseForge testing instance mods folder:

```powershell
Copy-Item -LiteralPath "build\libs\SLR1.0.0.jar" -Destination "C:\Users\CEO of SEX\curseforge\minecraft\Instances\Solo Leveling Testing\mods\SLR1.0.0.jar" -Force
```
