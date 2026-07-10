# EfkShaderRenderer Project Starter Prompt

```text
I want to start a new standalone Minecraft Forge 1.20.1 library/API mod called EfkShaderRenderer.

This API will be a reusable shader + quad rendering system that I can use across multiple Minecraft mods, especially my SoloCraft/Solo Leveling mod. The goal is to stop copying custom shader loaders, RenderType classes, quad renderers, visual-only entities, shader JSON files, VSH/FSH files, and effect logic into every project.

Please help me build this as a proper standalone Forge library mod.

Main goals:
1. Create a standalone shader rendering API mod named EfkShaderRenderer.
2. Primary first target: Forge 1.20.1, because my current SoloCraft/Solo Leveling workspace is Forge 1.20.1.
3. Also design the project so it can support NeoForge 1.21.1 as a second target/version.
4. The codebase should avoid locking the whole API to one loader if possible. Use a shared/common module for loader-independent code and separate loader modules for Forge and NeoForge integration.
5. Mod ID should be something like `efkshaderrenderer`.
6. Java package can be `net.efkshaderrenderer`.
7. It should act as a reusable API/library for shader-based combat visuals.
8. Other mods should be able to depend on it and call simple public methods to spawn visuals.
9. It should support shader-backed quads, slashes, beams, rings, portals, auras, trails, bursts, and projectile effects.
10. It should support JSON preset-based effects so other mods can define visuals without writing new renderer classes every time.
11. It should eventually include an in-game GUI/editor with sliders and options for live visual creation and preset export.

Version/loader support:
- Forge 1.20.1 should be implemented first.
- NeoForge 1.21.1 support should be planned from the beginning and added as its own module/build target.
- The API should keep preset parsing, math, shape definitions, spawn context data, validation, and public data models in shared/common code.
- Loader-specific code should be isolated:
  - mod bootstrap
  - event bus registration
  - shader registration event hooks
  - networking registration
  - entity registration
  - client setup
  - config registration
- Avoid scattering Forge-only or NeoForge-only imports through shared API classes.
- If a multi-loader Gradle setup is too much at first, start with Forge 1.20.1 but keep package boundaries clean so NeoForge 1.21.1 can be added without rewriting the API.

Current reference system:
In my SoloCraft/Solo Leveling mod, I already have a shader + quad rendering system with files like:

- BasicAttackSlashRenderer
- CrossStrikeRenderer
- DualWieldFlurryRenderer
- QuickSlashesRenderer
- SlashEffectRenderer
- SwordBeamProjectileRenderer

And shader render type loaders like:

- BasicAttackSlashRenderTypes
- CrossStrikeRenderTypes
- DualWieldFlurryRenderTypes
- QuickSlashesRenderTypes
- SlashEffectRenderTypes
- SwordBeamProjectileRenderTypes

And shader files like:

- rendertype_basic_slash_fist.vsh/fsh/json
- rendertype_basic_slash_sword.vsh/fsh/json
- rendertype_basic_slash_dagger.vsh/fsh/json
- rendertype_basic_slash_dual_dagger.vsh/fsh/json
- rendertype_cross_strike.vsh/fsh/json
- rendertype_dual_wield_flurry.vsh/fsh/json
- rendertype_quick_slashes.vsh/fsh/json
- rendertype_slash_effect.vsh/fsh/json
- rendertype_sword_beam_projectile.vsh/fsh/json

These should be treated as design references. The new API should generalize this system.

Important design principle:
Do not make every effect require custom GLSL. The API should provide powerful shader templates and configurable JSON presets. Most effects should be made by changing preset values such as shape, color, width, height, lifetime, fade, count, spread, noise, glow, distortion, pulse, roll, and blend/depth settings.

Possible public API usage:

```java
EfkShaderRenderer.spawn(level, position, new ResourceLocation("mymod", "quick_slashes"));

EfkShaderRenderer.spawnOnEntity(level, target, new ResourceLocation("mymod", "quick_slashes"));

EfkShaderRenderer.spawnBeam(level, start, end, new ResourceLocation("mymod", "sword_beam"));

EfkShaderRenderer.spawnAttached(level, player, new ResourceLocation("mymod", "shadow_aura"));
```

Advanced usage:

```java
EfkVisualSpawnContext ctx = EfkVisualSpawnContext.builder()
    .preset(new ResourceLocation("mymod", "quick_slashes"))
    .yaw(owner.getYRot())
    .scale(1.4F)
    .seed(owner.getRandom().nextInt())
    .color(0x7AD4FF)
    .build();

EfkShaderRenderer.spawn(level, target.position(), ctx);
```

Core API modules/classes I want:
- `EfkShaderRenderer`
- `EfkVisuals`
- `EfkVisualPreset`
- `EfkVisualPresetRegistry`
- `EfkVisualPresetLoader`
- `EfkVisualSpawnContext`
- `EfkVisualHandle`
- `EfkVisualShapeType`
- `EfkVisualEffectEntity`
- `EfkVisualProjectileEntity`
- `EfkVisualAttachedEntity`
- `EfkVisualRenderDispatcher`
- `EfkVisualRenderTypes`
- `EfkVisualShaderRegistry`
- `EfkVisualQuadBuilder`
- `EfkVisualMeshBuilder`
- `EfkVisualEditorScreen`
- network packets for spawning/syncing/stopping visuals

Shapes the API should support:
- billboard slash
- slash variant
- layered slash
- multi-slash sequence
- target slash burst
- slash volume/flurry
- projectile quad
- beam
- ring
- portal
- aura
- trail/ribbon
- burst

Preset JSON example:

```json
{
  "shape": "target_slash_burst",
  "shader": "efkshaderrenderer:slash_energy",
  "texture": "efkshaderrenderer:textures/effects/noise_slash.png",
  "lifetime": 12,
  "count": 8,
  "staggerTicks": 0.45,
  "spread": {
    "x": 1.15,
    "y": 1.45,
    "z": 0.7
  },
  "slash": {
    "minWidth": 1.15,
    "maxWidth": 1.87,
    "minHeight": 0.08,
    "maxHeight": 0.135,
    "randomRoll": 82.0
  },
  "color": {
    "a": "#F8FFFF",
    "b": "#37B8FF",
    "c": "#7929FF",
    "alpha": 0.92
  },
  "render": {
    "blend": "translucent",
    "depthTest": false,
    "cull": false,
    "light": "fullbright"
  },
  "shaderParams": {
    "edgeSharpness": 0.82,
    "noiseScale": 22.0,
    "distortion": 0.018,
    "glowStrength": 0.35
  }
}
```

Built-in shader templates:
1. `slash_energy`
   - blue/violet/electric slash
   - based on my Quick Slashes shader
   - good for assassin and shadow effects

2. `slash_molten`
   - red/orange molten slash
   - based on Cross Strike shader
   - good for fighter/heavy attacks

3. `slash_fury`
   - tinted layered slash
   - based on Slash Effect / Slash Fury shader

4. `slash_void_projectile`
   - purple/blue void slash projectile
   - based on Sword Beam Projectile shader

5. `basic_weapon_slash`
   - lightweight repeated melee slash
   - based on fist/sword/dagger/dual dagger shaders

The API should include:
- shader registration helpers
- RenderType caching
- fallback rendering if shader fails
- quad builders
- generic visual entity
- client-only visual packets
- synced visual packets for multiplayer
- preset JSON loader
- reload listener
- validation for bad preset JSON
- in-game editor later

Multiplayer requirement:
The visuals must be visible to other players in multiplayer. Avoid the mistake where effects only spawn client-side for the caster. The API should support server-triggered visual spawning. When an ability calls the API on the server, it should either:
- spawn a synced `EfkVisualEffectEntity`, or
- send a packet to all tracking clients / nearby players.

I want a clean distinction:
- server-synced visual: visible to everyone nearby
- client-local visual: visible only to one client, useful for UI or personal feedback
- attached visual: follows an entity and is synced if needed
- projectile visual: follows/projectile-like orientation

Networking should include:
- spawn visual packet
- spawn visual on entity packet
- spawn beam packet
- stop visual packet
- editor preview packet if needed

Important multiplayer API examples:

```java
// visible to all nearby players
EfkVisuals.spawnSynced(serverLevel, pos, preset);

// visible only to one player
EfkVisuals.spawnLocal(serverPlayer, pos, preset);

// visible to all tracking a target
EfkVisuals.spawnOnEntitySynced(serverLevel, target, preset);
```

Shader compatibility / Iris / Oculus / shader pack problems:
The API needs to be careful with compatibility. I know custom Forge core shaders can sometimes behave weirdly with Iris/Oculus/shader packs or other mods. Please design the system to reduce conflicts.

Required compatibility goals:
1. If custom shader loading fails, fall back to a vanilla translucent RenderType.
2. Do not crash the client if a shader JSON, VSH, FSH, or texture is missing.
3. Log clear warnings instead of hard crashing where possible.
4. Do not globally modify render state in unsafe ways.
5. Restore/clear render state properly after custom rendering.
6. Cache RenderTypes and avoid creating unstable render states every frame.
7. Provide config options:
   - enable/disable custom shaders
   - force vanilla fallback rendering
   - enable/disable no-depth rendering
   - lower effect intensity
   - max visual particles/quads
   - compatibility mode for Iris/Oculus/shader packs
8. Detect known shader mod environments if possible, or at least expose a config/manual compatibility mode.
9. Avoid depending on OptiFine-specific behavior.
10. Use standard Forge/Minecraft shader registration through `RegisterShadersEvent`.
11. Keep all shader/client classes under client-only dist guards.
12. Dedicated servers must not load client shader classes.

Rendering fallback behavior:
If the shader is unavailable, effects should still render with:
- `RenderType.entityTranslucent(texture)`
- or another safe vanilla RenderType
- with similar quads, colors, and alpha
This means the visuals still work in multiplayer even when the fancy shader is disabled.

Depth / transparency problems:
Some effects need to ignore depth so slashes are readable, but this can look broken through water, ice, glass, or shader packs. The API should support render modes:
- `normal_depth`
- `no_depth`
- `soft_depth`
- `additive`
- `translucent`
- `vanilla_fallback`

Each preset should choose a render mode. The editor should let me change this live.

In-game editor GUI:
Eventually I want a GUI that lets me create effects live in-game with options and sliders.

Editor tabs:
- Preview
- Shape
- Color
- Shader
- Timing
- Motion
- Spawn
- Export

Editor controls:
- shape type dropdown
- shader template dropdown
- texture picker
- color swatches
- alpha slider
- width/height/length sliders
- scale slider
- roll slider
- count slider
- spread X/Y/Z sliders
- lifetime slider
- fade in/out sliders
- stagger delay slider
- noise scale slider
- noise speed slider
- edge sharpness slider
- distortion slider
- glow strength slider
- pulse strength slider
- render mode dropdown
- depth test toggle
- fullbright toggle
- billboard mode toggle
- preview on self / target dummy / projectile / beam / portal

Export should save JSON presets to something like:
- `config/efkshaderrenderer/presets/client_created/*.json`
- or during development, copyable JSON for `src/main/resources/assets/<namespace>/visual_presets/*.json`

Usage in other mods:
Other mods should depend on EfkShaderRenderer through Gradle/mavenLocal during development and as a required dependency in release. There should eventually be separate artifacts for Forge 1.20.1 and NeoForge 1.21.1.

Example consuming mod `mods.toml`:

```toml
[[dependencies.sololeveling]]
    modId="efkshaderrenderer"
    mandatory=true
    versionRange="[1.0.0,)"
    ordering="AFTER"
    side="BOTH"
```

Example consuming mod Gradle:

```gradle
repositories {
    mavenLocal()
}

dependencies {
    implementation fg.deobf("net.efkshaderrenderer:efkshaderrenderer:1.0.0")
}
```

For NeoForge 1.21.1 consuming mods, provide a separate artifact and dependency example when that module exists, for example:

```gradle
repositories {
    mavenLocal()
}

dependencies {
    implementation "net.efkshaderrenderer:efkshaderrenderer-neoforge-1.21.1:1.0.0"
}
```

Development roadmap:
Milestone 1:
- create Forge 1.20.1 standalone mod skeleton
- mod id `efkshaderrenderer`
- package `net.efkshaderrenderer`
- basic public API classes
- build/publish to mavenLocal

Milestone 1.5:
- choose the long-term project structure for multi-version support
- either use a multi-module setup like:
  - `common`
  - `forge-1.20.1`
  - `neoforge-1.21.1`
- or start Forge-only with strict package boundaries and add the NeoForge module later
- document which classes are shared and which are loader-specific

Milestone 2:
- shader registry
- RenderType cache
- fallback render type
- common vertex shader
- first slash shader templates

Milestone 3:
- quad builder helpers
- billboard quad
- oriented quad
- crossed quad
- beam strip
- ring mesh

Milestone 4:
- JSON preset loader
- preset registry
- reload listener
- preset validation

Milestone 5:
- generic visual entity
- spawn packets
- synced multiplayer visuals
- local-only visuals

Milestone 6:
- generic render dispatcher
- implement billboard slash
- multi slash sequence
- target slash burst
- projectile quad

Milestone 7:
- port current SoloCraft effects as presets:
  - Basic Attack Slash
  - Cross Strike
  - Slash Fury
  - Dual Wield Flurry
  - Quick Slashes
  - Sword Beam Projectile

Milestone 8:
- in-game visual editor GUI
- live preview
- export JSON

Milestone 9:
- compatibility testing:
  - dedicated server
  - multiplayer visibility
  - missing shader fallback
  - resource reload
  - shader packs/Iris/Oculus compatibility mode
  - low-end config mode

Milestone 10:
- add NeoForge 1.21.1 support
- port loader-specific registration to NeoForge events/APIs
- verify shader registration/resource reload behavior on 1.21.1
- verify networking and synced visuals on NeoForge multiplayer
- keep preset JSON format compatible between Forge 1.20.1 and NeoForge 1.21.1 wherever possible

Important implementation rules:
- Do not crash dedicated servers with client imports.
- Do not make visuals caster-client-only unless explicitly requested.
- Effects spawned from server gameplay should be visible to nearby players.
- Keep shared API code free of unnecessary Forge-only or NeoForge-only imports.
- Prefer adapter/wrapper classes for loader-specific behavior.
- Always provide shader fallback.
- Keep public API stable.
- Keep old presets compatible when adding new fields.
- Use seeded randomness instead of syncing every random value.
- Prefer one visual entity with many quads over many tiny entities.
- Avoid making new RenderTypes every frame if possible.
- Add clear logs for missing presets/shaders/textures.

Please help me design and implement this project step by step. Start by creating the standalone Forge mod structure and the core API skeleton, then gradually add shader registration, RenderType caching, presets, visual entities, packets, and finally editor GUI support.
```
