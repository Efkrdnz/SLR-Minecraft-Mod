# Shader Rendering System API Plan

This document describes how the current SoloCraft Reawakening shader and quad visual system can become a standalone reusable Forge library/API mod. The goal is to stop copying shader loaders, render types, quad renderers, visual entities, and GLSL files between projects, while still letting each mod define its own effects, colors, timing, and ability behavior.

The proposed library name in this guide is `SoloVisualAPI`. The actual mod name can change later.

## Current SLR System Reference

The current SLR mod already has a useful pattern:

1. A server-side or synced visual entity stores effect data.
2. A client entity renderer builds one or more quads.
3. A small `RenderTypes` class registers and exposes a shader-backed `RenderType`.
4. Shader JSON, vertex shader, and fragment shader files live under `assets/sololeveling/shaders/core`.
5. The renderer passes effect variation through vertex position, UV, vertex color, alpha, scale, roll, yaw, seed, tick age, and synced entity data.

Current important files:

```text
src/main/java/net/solocraft/client/renderer/BasicAttackSlashRenderer.java
src/main/java/net/solocraft/client/renderer/CrossStrikeRenderer.java
src/main/java/net/solocraft/client/renderer/DualWieldFlurryRenderer.java
src/main/java/net/solocraft/client/renderer/QuickSlashesRenderer.java
src/main/java/net/solocraft/client/renderer/SlashEffectRenderer.java
src/main/java/net/solocraft/client/renderer/SwordBeamProjectileRenderer.java

src/main/java/net/solocraft/client/renderer/shader/BasicAttackSlashRenderTypes.java
src/main/java/net/solocraft/client/renderer/shader/CrossStrikeRenderTypes.java
src/main/java/net/solocraft/client/renderer/shader/DualWieldFlurryRenderTypes.java
src/main/java/net/solocraft/client/renderer/shader/QuickSlashesRenderTypes.java
src/main/java/net/solocraft/client/renderer/shader/SlashEffectRenderTypes.java
src/main/java/net/solocraft/client/renderer/shader/SwordBeamProjectileRenderTypes.java

src/main/resources/assets/sololeveling/shaders/core/rendertype_basic_slash_fist.*
src/main/resources/assets/sololeveling/shaders/core/rendertype_basic_slash_sword.*
src/main/resources/assets/sololeveling/shaders/core/rendertype_basic_slash_dagger.*
src/main/resources/assets/sololeveling/shaders/core/rendertype_basic_slash_dual_dagger.*
src/main/resources/assets/sololeveling/shaders/core/rendertype_cross_strike.*
src/main/resources/assets/sololeveling/shaders/core/rendertype_dual_wield_flurry.*
src/main/resources/assets/sololeveling/shaders/core/rendertype_quick_slashes.*
src/main/resources/assets/sololeveling/shaders/core/rendertype_slash_effect.*
src/main/resources/assets/sololeveling/shaders/core/rendertype_sword_beam_projectile.*
```

Current effect categories:

| SLR effect | Renderer behavior | API category |
| --- | --- | --- |
| Basic attack slash | One billboard quad, style-specific size, roll, shader | `billboard_slash` |
| Fist/sword/dagger/dual dagger slash | Same geometry, different shader/colors/profile | `slash_variant` |
| Cross Strike | Two billboard slashes revealed in sequence | `multi_slash_sequence` |
| Slash Fury / Slash Effect | Tinted slash with glow quad and core quad | `layered_slash` |
| Dual Wield Flurry | Many seeded quads in a volume, delayed reveal | `slash_volume` |
| Quick Slashes | Several quads on/around a target, randomized placement, staggered reveal | `target_slash_burst` |
| Sword Beam Projectile | Oriented projectile quad with pulse and roll | `projectile_quad` |

The current shaders are good references because they already solve the hard visual problems:

- sharp slash profiles from `sin(uv.x * pi)` blade masks
- noisy/chipped edges with `fbm`
- animated flow using `GameTime`
- local distortion using `localPos`
- tinting through vertex color
- additive-looking translucent blending
- depth-test disabled visuals for readable combat effects
- lightmap sampling through `Sampler2`

## Why Make This A Standalone API

The API is worth making if two or more mods will use these effects. The benefit is not only extra downloads. The real value is that visual effects become faster to build, easier to tune, and consistent across all your mods.

The standalone API should provide:

- reusable shader registration
- reusable `RenderType` creation
- reusable quad mesh builders
- reusable client visual entities
- server-to-client spawn helpers
- JSON visual presets
- a live in-game editor GUI
- export/import tools for presets
- default shader templates for slashes, beams, portals, rings, auras, trails, and bursts

SoloCraft should eventually stop owning the generic visual system. It should only own:

- ability logic
- damage logic
- SLR-specific colors and presets
- SLR-specific textures
- SLR-specific entity spawn timing

## Main Design Rule

The API should not require every mod to write GLSL for every new effect.

Instead, the API should provide a small set of powerful shader templates. Mods and the in-game editor should change uniforms and preset values:

```text
shape type
texture
color palette
width
height
length
curve
edge sharpness
noise scale
distortion
speed
alpha
glow strength
fade in
fade out
slash count
stagger delay
random spread
blend mode
depth mode
billboard mode
```

Only advanced users should need to add new GLSL.

## Mod Architecture

Recommended library package layout:

```text
net.solovisualapi
  SoloVisualAPI.java

net.solovisualapi.api
  SoloVisuals.java
  VisualPreset.java
  VisualSpawnContext.java
  VisualHandle.java
  VisualTargeting.java

net.solovisualapi.api.preset
  VisualPresetRegistry.java
  VisualPresetLoader.java
  VisualPresetCodec.java
  VisualPresetValidation.java

net.solovisualapi.api.shape
  VisualShapeType.java
  SlashShape.java
  BeamShape.java
  RingShape.java
  PortalShape.java
  AuraShape.java
  TrailShape.java
  BurstShape.java

net.solovisualapi.client
  SoloVisualAPIClient.java
  VisualRenderDispatcher.java
  VisualRenderTypes.java
  VisualShaderRegistry.java
  VisualQuadBuilder.java
  VisualMeshBuilder.java
  VisualEditorScreen.java

net.solovisualapi.client.shader
  ShaderKey.java
  ShaderTemplate.java
  ShaderUniformWriter.java
  VisualShaderUniforms.java

net.solovisualapi.entity
  VisualEffectEntity.java
  VisualProjectileEntity.java
  VisualAttachedEntity.java

net.solovisualapi.network
  SpawnVisualPacket.java
  AttachVisualPacket.java
  StopVisualPacket.java
  SyncEditorPreviewPacket.java

assets/solovisualapi/shaders/core
  rendertype_sva_slash.vsh
  rendertype_sva_slash.fsh
  rendertype_sva_slash.json
  rendertype_sva_beam.vsh
  rendertype_sva_beam.fsh
  rendertype_sva_beam.json
  rendertype_sva_portal.vsh
  rendertype_sva_portal.fsh
  rendertype_sva_portal.json

assets/solovisualapi/visual_presets
  examples/slash_basic.json
  examples/quick_slashes.json
  examples/cross_strike.json
  examples/portal_cloud.json
```

## Public API Goals

The API used by other mods should be simple:

```java
SoloVisuals.spawn(level, position, "sololeveling:quick_slashes");
SoloVisuals.spawnOnEntity(level, target, "sololeveling:quick_slashes");
SoloVisuals.spawnBeam(level, start, end, "sololeveling:sword_beam");
SoloVisuals.spawnAttached(level, owner, "sololeveling:shadow_aura");
```

Advanced usage should allow overrides:

```java
SoloVisuals.spawn(level, position, VisualSpawnContext.builder()
    .preset(new ResourceLocation("sololeveling", "quick_slashes"))
    .yaw(owner.getYRot())
    .scale(1.35F)
    .seed(owner.getRandom().nextInt())
    .color(0x7AD4FF)
    .build());
```

Projectile effects should be easy:

```java
SoloVisuals.spawnProjectileTrail(projectile, new ResourceLocation("sololeveling", "sword_beam"));
```

Ability code should not need to know:

- `ShaderInstance`
- `RenderType.CompositeState`
- `DefaultVertexFormat.NEW_ENTITY`
- raw vertex ordering
- Forge shader registration events
- buffer sizes
- fallback `RenderType`

## API Module Responsibilities

### `SoloVisuals`

This is the main public class for other mods.

Responsibilities:

- resolve preset IDs
- create spawn packets or visual entities
- provide convenience methods for common cases
- keep public methods stable between versions

Example methods:

```java
public static VisualHandle spawn(LevelAccessor world, Vec3 pos, ResourceLocation preset);
public static VisualHandle spawnOnEntity(LevelAccessor world, Entity target, ResourceLocation preset);
public static VisualHandle spawnAtEntityCenter(LevelAccessor world, Entity target, ResourceLocation preset);
public static VisualHandle spawnBeam(LevelAccessor world, Vec3 start, Vec3 end, ResourceLocation preset);
public static VisualHandle spawnSequence(LevelAccessor world, List<Vec3> positions, ResourceLocation preset, int tickDelay);
public static void stop(LevelAccessor world, UUID visualId);
```

### `VisualPreset`

This describes what the effect looks like. It should be JSON-loaded and also buildable in code.

Core fields:

```text
id
shape
shader
texture
lifetime
billboard
blend
depthTest
cull
light
scale
colorA
colorB
alpha
fadeIn
fadeOut
```

Shape-specific fields:

```text
slash:
  width
  height
  roll
  edgeSharpness
  curve
  slashCount
  staggerTicks

beam:
  width
  length
  pulse
  rollSpeed

portal:
  radius
  thickness
  swirlSpeed
  ringCount
  cloudAmount

burst:
  count
  spreadX
  spreadY
  spreadZ
  randomRoll
```

### `VisualEffectEntity`

This should be a generic lightweight entity for synced effects. It replaces many copied visual-only entity classes.

Synced fields:

```text
preset id
yaw
pitch
roll
scale
seed
target entity id, optional
owner entity id, optional
start position
end position, optional
variant
start tick
lifetime override
color override, optional
```

Not every effect needs a real entity. For pure client visuals, packets can spawn client-only handles. Use real entities when:

- the visual must be visible to nearby players
- the visual needs synced position
- the visual follows an entity
- the visual has a projectile hitbox or gameplay relation

Use client-only effects when:

- only the caster needs to see it
- it is UI-like
- it is purely decorative

### `VisualRenderDispatcher`

This chooses how to render a preset based on `shape`.

Example:

```text
billboard_slash -> SlashRenderer
multi_slash_sequence -> SlashSequenceRenderer
target_slash_burst -> SlashBurstRenderer
projectile_quad -> ProjectileQuadRenderer
beam -> BeamRenderer
ring -> RingRenderer
portal -> PortalRenderer
trail -> RibbonTrailRenderer
```

This removes the need for one custom Java renderer for every ability.

### `VisualRenderTypes`

This replaces many repeated `RenderTypes` classes.

Current SLR pattern:

```java
event.registerShader(new ShaderInstance(...), shader -> quickSlashesShader = shader);
RenderType.create("quick_slashes", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, ...);
```

API pattern:

```java
VisualRenderTypes.registerShader("solovisualapi:slash");
RenderType renderType = VisualRenderTypes.get(preset.shader(), preset.texture(), preset.renderState());
```

The API should cache `RenderType`s by:

```text
shader id
texture id
blend mode
depth mode
cull mode
write mask
vertex format
```

### `VisualQuadBuilder`

This removes duplicated vertex code.

Current SLR renderers repeat:

```java
vertexConsumer.vertex(...)
    .color(...)
    .uv(...)
    .overlayCoords(...)
    .uv2(240)
    .normal(...)
    .endVertex();
```

API helper:

```java
VisualQuadBuilder.billboard(poseStack, camera)
    .size(width, height)
    .roll(roll)
    .color(red, green, blue, alpha)
    .uv(0, 0, 1, 1)
    .emit(vertexConsumer);
```

Needed builders:

- billboard quad
- oriented quad
- crossed quad
- ring mesh
- ribbon strip
- beam strip
- portal disc/ring
- random slash volume

## Shader Contract

All built-in shader templates should use the same basic contract where possible.

Current SLR vertex shader pattern:

```glsl
in vec3 Position;
in vec4 Color;
in vec2 UV0;
in ivec2 UV1;
in ivec2 UV2;
in vec3 Normal;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform mat4 TextureMat;

out vec4 vertexColor;
out vec2 texCoord0;
out vec2 localPos;
out vec2 lightMap;
```

The API should keep this, then add optional uniforms:

```glsl
uniform float GameTime;
uniform float VisualAge;
uniform float Lifetime;
uniform float EdgeSharpness;
uniform float NoiseScale;
uniform float NoiseSpeed;
uniform float Distortion;
uniform float GlowStrength;
uniform float PulseStrength;
uniform vec4 ColorA;
uniform vec4 ColorB;
uniform vec4 ColorC;
```

Important note: Minecraft shader JSON uniforms are not automatically per-entity. Per-entity values are easiest through vertex color, UVs, local position, and the geometry itself. For true dynamic per-effect uniforms, the API needs a controlled render path that sets uniforms immediately before drawing that effect. That should be part of phase 2, not phase 1.

Phase 1 should use:

- vertex color for tint and alpha
- UV for profile and timing tricks
- local position for distortion
- GameTime for animation
- preset-specific shader variants only where needed

Phase 2 can add:

- per-effect uniform writing
- editor-driven live uniform preview
- shader template parameters

## Preset JSON Format

Example slash preset:

```json
{
  "shape": "billboard_slash",
  "shader": "solovisualapi:slash_energy",
  "texture": "solovisualapi:textures/effects/noise_slash.png",
  "lifetime": 12,
  "billboard": "camera",
  "render": {
    "blend": "translucent",
    "depthTest": false,
    "cull": false,
    "light": "fullbright"
  },
  "transform": {
    "width": 3.2,
    "height": 0.32,
    "scale": 1.0,
    "roll": 0.0
  },
  "animation": {
    "fadeIn": 1,
    "fadeOut": 8,
    "pulse": 0.05,
    "noiseSpeed": 1.4
  },
  "color": {
    "a": "#F8FFFF",
    "b": "#31B8FF",
    "c": "#7730FF",
    "alpha": 0.92
  },
  "shaderParams": {
    "edgeSharpness": 0.82,
    "noiseScale": 22.0,
    "distortion": 0.018,
    "glowStrength": 0.35
  }
}
```

Example Quick Slashes style preset:

```json
{
  "shape": "target_slash_burst",
  "shader": "solovisualapi:slash_energy",
  "texture": "solovisualapi:textures/effects/noise_slash.png",
  "lifetime": 12,
  "count": 8,
  "staggerTicks": 0.45,
  "spread": {
    "x": 1.15,
    "y": 1.45,
    "z": 0.70
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
  }
}
```

Example Cross Strike style preset:

```json
{
  "shape": "multi_slash_sequence",
  "shader": "solovisualapi:slash_molten",
  "texture": "solovisualapi:textures/effects/noise_slash.png",
  "lifetime": 14,
  "slashes": [
    { "roll": -43.0, "startTick": 0, "width": 4.35, "height": 0.24 },
    { "roll": 43.0, "startTick": 4, "width": 4.35, "height": 0.24 }
  ],
  "color": {
    "a": "#FFF8D8",
    "b": "#FF2408",
    "c": "#8130FF",
    "alpha": 0.92
  }
}
```

Example Sword Beam style preset:

```json
{
  "shape": "projectile_quad",
  "shader": "solovisualapi:slash_void",
  "texture": "solovisualapi:textures/effects/noise_slash.png",
  "lifetime": 30,
  "orientToVelocity": true,
  "width": 5.5,
  "height": 0.68,
  "pulse": 0.045,
  "rollWobble": 3.0,
  "color": {
    "a": "#D4F0FF",
    "b": "#8230FF",
    "c": "#1CB8FF",
    "alpha": 0.96
  }
}
```

## Built-In Shader Templates

The first API version should ship these templates:

### `slash_energy`

Reference: `rendertype_quick_slashes.fsh`

Best for:

- Quick Slashes
- blue shadow slashes
- assassin effects
- fast target bursts

Features:

- electric blue/violet palette
- sharp core
- noisy edge
- afterglow
- animated spark streaks

### `slash_molten`

Reference: `rendertype_cross_strike.fsh`

Best for:

- Cross Strike
- fighter skills
- red/orange impact slashes

Features:

- molten red/orange blade
- violet wake
- spark highlights
- strong white core

### `slash_fury`

Reference: `rendertype_slash_effect.fsh`

Best for:

- Slash Fury
- layered colored slashes
- multi-variant ability effects

Features:

- vertex color driven tint
- glow layer support
- curved/chipped blade
- stronger stylized edge

### `slash_void_projectile`

Reference: `rendertype_sword_beam_projectile.fsh`

Best for:

- sword beams
- flying slash projectiles
- dark magic arcs

Features:

- void smoke edge
- corrupted blue/purple edge
- pulsing body
- projectile-friendly profile

### `basic_weapon_slash`

References:

```text
rendertype_basic_slash_fist.fsh
rendertype_basic_slash_sword.fsh
rendertype_basic_slash_dagger.fsh
rendertype_basic_slash_dual_dagger.fsh
```

Best for:

- normal melee attacks
- weapon-specific visuals
- low-cost repeated combat effects

Features:

- fast lifetime
- compact shape
- style variants
- simple fallback path

## In-Game Visual Editor GUI

The editor should be one of the strongest features of the API. It should let you build effects in-game, see them live, then export JSON presets.

Important design goal: the editor edits presets and shader parameters, not raw GLSL code.

### Editor Screen Layout

Recommended tabs:

```text
Preview
Shape
Color
Shader
Timing
Motion
Spawn
Export
```

### Preview Tab

Controls:

- play/pause
- restart preview
- spawn at player
- spawn on target dummy
- loop preview
- preview distance
- camera lock toggle
- background contrast toggle

Preview modes:

- self billboard
- target entity
- projectile
- beam between two points
- portal/ring in world

### Shape Tab

Controls:

- shape type dropdown
- width slider
- height slider
- length slider
- scale slider
- roll slider
- count stepper
- spread X/Y/Z sliders
- billboard mode segmented control
- orientation mode dropdown

Shape types:

```text
billboard_slash
multi_slash_sequence
target_slash_burst
slash_volume
projectile_quad
beam
ring
portal
aura
trail
burst
```

### Color Tab

Controls:

- color A swatch
- color B swatch
- color C swatch
- alpha slider
- glow color swatch
- brightness slider
- hue shift slider
- gradient mode dropdown

### Shader Tab

Controls:

- shader template dropdown
- edge sharpness slider
- noise scale slider
- noise speed slider
- distortion slider
- glow strength slider
- pulse strength slider
- spark amount slider
- broken edge slider
- core width slider

### Timing Tab

Controls:

- lifetime slider
- fade in slider
- fade out slider
- reveal duration slider
- stagger delay slider
- loop toggle
- start delay slider

### Motion Tab

Controls:

- roll speed slider
- spin axis dropdown
- drift X/Y/Z sliders
- grow over lifetime slider
- shrink over lifetime slider
- pulse speed slider
- attach to entity toggle
- follow target toggle

### Spawn Tab

Controls:

- spawn mode dropdown
- offset X/Y/Z sliders
- random seed toggle
- random spread sliders
- target body anchor dropdown
- align to caster yaw toggle
- align to velocity toggle

Anchor options:

```text
feet
center
chest
eyes
weapon_hand
target_center
between_caster_and_target
```

### Export Tab

Controls:

- preset id field
- namespace field
- save to config button
- save to datapack button
- copy JSON button
- load preset button
- duplicate preset button
- reset to default button

Export locations:

```text
config/solovisualapi/presets/client_created/*.json
resourcepacks/<pack>/assets/<namespace>/visual_presets/*.json
src/main/resources/assets/<namespace>/visual_presets/*.json, during dev only
```

## How Other Mods Use The API

### During Development

For a normal Forge workspace:

```gradle
repositories {
    mavenLocal()
}

dependencies {
    implementation fg.deobf("com.yourname:solovisualapi:1.0.0")
}
```

Then in `mods.toml`:

```toml
[[dependencies.sololeveling]]
    modId="solovisualapi"
    mandatory=true
    versionRange="[1.0.0,)"
    ordering="AFTER"
    side="BOTH"
```

The API mod should be built and installed locally with:

```text
gradlew publishToMavenLocal
```

For quick testing, you can also copy the API jar into the consuming workspace's `run/mods` folder, but `mavenLocal` is cleaner for compilation.

### For CurseForge/Modrinth Releases

Release two jars:

```text
Solo Visual API
SoloCraft Reawakening
```

SoloCraft marks Solo Visual API as a required dependency. This gives the API its own downloads and lets other mods depend on it too.

### For External Mod Workspaces

Other Forge or NeoForge mod projects can depend on the API directly. The cleanest method is to keep the API in its own project, publish or copy the jar, and add it as a normal Gradle/runtime dependency.

Practical workflow:

1. Make the API in its own normal Forge MDK workspace.
2. Build/publish the API jar.
3. Add the API jar dependency to the mod workspace Gradle.
4. Add the API jar to the runtime mods folder for testing.
5. Keep API calls inside dedicated renderer/effect classes so future refactors do not overwrite or duplicate them.

Do not put the whole API inside every consuming workspace. That defeats the purpose.

## Migration Plan From Current SLR Code

### Phase 1: Extract The Common Shader Loader

Move repeated shader registration and `RenderType` creation into the API.

Current repeated class pattern:

```text
BasicAttackSlashRenderTypes
CrossStrikeRenderTypes
DualWieldFlurryRenderTypes
QuickSlashesRenderTypes
SlashEffectRenderTypes
SwordBeamProjectileRenderTypes
```

API replacement:

```java
VisualRenderTypes.get(new ResourceLocation("solovisualapi", "slash_energy"), texture, VisualRenderState.translucentNoDepth());
```

SLR change:

- keep current effect entities/renderers
- replace each custom `RenderTypes` class call with the API render type
- keep current shader resources until equivalent API shaders exist

### Phase 2: Extract Quad Builders

Replace duplicated vertex code with `VisualQuadBuilder`.

SLR renderers should stop manually doing:

```java
vertexConsumer.vertex(...).color(...).uv(...).overlayCoords(...).uv2(240).normal(...).endVertex();
```

Use:

```java
VisualQuadBuilder.oriented(poseStack.last())
    .halfSize(halfWidth, halfHeight)
    .color(red, green, blue, alpha)
    .emit(vertexConsumer);
```

### Phase 3: Add Generic Visual Entity

Create:

```text
VisualEffectEntity
VisualProjectileEntity
VisualAttachedEntity
```

Then migrate:

```text
QuickSlashesEntity -> VisualEffectEntity using target_slash_burst preset
CrossStrikeEntity -> VisualEffectEntity using multi_slash_sequence preset
SlashEffectEntity -> VisualEffectEntity using layered_slash preset
SwordBeamProjectileEntity -> VisualProjectileEntity using projectile_quad preset
```

Keep gameplay hit detection in SLR where needed. The API should render visuals; the ability code should still own damage unless the API explicitly adds optional hitbox helpers.

### Phase 4: Add Preset JSON Loader

Move hardcoded values into JSON:

```text
SLASH_COUNT
halfWidth
halfHeight
fade timing
colors
random spread
roll ranges
shader selection
texture selection
```

SLR should define presets like:

```text
assets/sololeveling/visual_presets/basic_fist_slash.json
assets/sololeveling/visual_presets/basic_sword_slash.json
assets/sololeveling/visual_presets/cross_strike.json
assets/sololeveling/visual_presets/quick_slashes.json
assets/sololeveling/visual_presets/dual_wield_flurry.json
assets/sololeveling/visual_presets/slash_fury.json
assets/sololeveling/visual_presets/sword_beam_projectile.json
```

### Phase 5: Add The Editor GUI

Start with the editor modifying only:

- shape
- count
- width/height
- color
- alpha
- lifetime
- fade
- spread
- roll
- shader template

Add advanced shader sliders later.

### Phase 6: Deprecate Old SLR Renderers

Once presets cover the existing effects, replace SLR-specific renderers with API generic renderers.

Keep old classes for one or two versions if needed, but stop adding new effects through the old copied pattern.

## Versioning Rules

The API must be stable. Other mods should not break every time you improve a shader.

Use semantic versions:

```text
1.0.0 initial release
1.1.0 new shapes or new optional fields
1.2.0 editor improvements
2.0.0 breaking preset schema changes
```

Rules:

- Adding a new preset field is minor.
- Removing or renaming a preset field is major.
- Changing shader visuals without changing JSON is patch/minor depending on how visible it is.
- Public API methods should stay compatible.
- Old presets should load with defaults.

## Performance Guidelines

The API should be built for combat spam.

Important rules:

- Cache `RenderType`s.
- Cache loaded presets.
- Avoid creating many temporary objects inside render loops.
- Prefer one visual entity rendering many quads over many entities rendering one quad each.
- Keep default quad counts reasonable.
- Use seeded randomness so effects are stable and do not sync huge data.
- Use client-only effects when server sync is unnecessary.
- Use `uv2(240)` only when fullbright is intended.
- Do not disable depth test for every effect. Use it only where readability needs it.

Recommended default limits:

```text
basic slash: 1 to 2 quads
cross strike: 2 quads
quick slashes: 6 to 10 quads
dual wield flurry: 12 to 24 quads
portal cloud: 2 to 5 layers
beam: 1 to 4 strips
trail: max 24 segments
```

## Safety And Compatibility

Shader systems can break resource reloads if not handled carefully.

The API should:

- fallback to vanilla translucent render type if a shader is missing
- log clear shader load errors
- validate preset JSON
- clamp bad editor values
- avoid crashing if a texture is missing
- ignore unknown JSON fields for forward compatibility
- allow server without client shader code
- keep client-only classes behind `Dist.CLIENT`

Dedicated server rule:

- The API can exist on both sides, but rendering classes and shader registration must only load on the client.

## What The First API Release Should Include

Minimum useful `1.0.0`:

- Forge 1.20.1 support
- shader registration helper
- cached render type helper
- common vertex shader
- slash shader templates based on current SLR effects
- quad builder helpers
- `VisualEffectEntity`
- spawn packets/helpers
- JSON preset loader
- generic render dispatcher for:
  - billboard slash
  - multi slash sequence
  - target slash burst
  - projectile quad
- example presets copied from current SLR visuals
- basic in-game preview GUI

Do not wait for the perfect portal editor before releasing the first version. Slashes and beams are already enough to justify the API.

## What Should Stay In SLR

SLR should keep:

- assassin ability logic
- melee damage timing
- DKC/dungeon logic
- shadow monarch logic
- entity target detection
- skill unlock logic
- SLR-specific preset files
- SLR-specific textures

SLR should stop owning:

- generic shader registration
- generic render type creation
- repeated quad vertex methods
- generic slash renderer patterns
- generic visual preset parsing
- visual editor GUI

## Example SLR Usage After Migration

Quick Slashes ability:

```java
for (LivingEntity target : targets) {
    SoloVisuals.spawnOnEntity(world, target, new ResourceLocation("sololeveling", "quick_slashes"));
}
```

Cross Strike:

```java
SoloVisuals.spawn(world, owner.position().add(owner.getLookAngle().scale(2.0D)),
        VisualSpawnContext.builder()
                .preset(new ResourceLocation("sololeveling", "cross_strike"))
                .yaw(owner.getYRot())
                .scale(1.0F)
                .build());
```

Sword Beam:

```java
SoloVisuals.spawnBeam(world, start, end, new ResourceLocation("sololeveling", "sword_beam_projectile"));
```

Slash Fury:

```java
SoloVisuals.spawnSequence(world, slashPositions, new ResourceLocation("sololeveling", "slash_fury"), 2);
```

## Development Roadmap

### Milestone 1: API Skeleton

- Create standalone Forge 1.20.1 workspace.
- Set mod id to `solovisualapi`.
- Add `mods.toml`.
- Add Gradle publishing to `mavenLocal`.
- Add public `SoloVisuals` class.
- Add empty client setup.

### Milestone 2: Shader Core

- Move current common vertex shader into API.
- Add `VisualShaderRegistry`.
- Add `VisualRenderTypes`.
- Add fallback render type behavior.
- Port `quick_slashes`, `cross_strike`, `slash_effect`, and `sword_beam_projectile` shaders as templates.

### Milestone 3: Quad Builders

- Add `VisualQuadBuilder`.
- Add billboard, oriented, crossed, beam, and ring helpers.
- Port one SLR renderer to use the builder as a test.

### Milestone 4: Presets

- Add JSON parser and registry.
- Add example presets.
- Add reload listener.
- Add validation errors in logs.

### Milestone 5: Generic Visual Entity

- Add `VisualEffectEntity`.
- Add network spawn helpers.
- Add generic render dispatcher.
- Implement `billboard_slash`, `multi_slash_sequence`, `target_slash_burst`, and `projectile_quad`.

### Milestone 6: SLR Integration

- Add API dependency to SLR.
- Add `mods.toml` dependency.
- Replace one effect at a time:
  1. Quick Slashes
  2. Cross Strike
  3. Basic Attack Slash
  4. Slash Fury
  5. Dual Wield Flurry
  6. Sword Beam Projectile

### Milestone 7: Editor GUI

- Add visual editor screen.
- Add preview spawning.
- Add sliders for common fields.
- Add export/import.
- Add preset reload button.

### Milestone 8: Release

- Build API jar.
- Build SLR jar depending on API.
- Test clean client install with only Forge, API, and SLR.
- Upload API separately.
- Mark API as required dependency for SLR.

## Suggested Names

Possible mod names:

```text
Solo Visual API
Solo FX API
Solo Render API
Arcane Visual API
Shadered Combat FX API
SVA - Solo Visual API
```

Recommended mod id:

```text
solovisualapi
```

Recommended Java group:

```text
net.solovisualapi
```

## Final Recommendation

Build the API around presets, not raw shader editing. The current SLR shaders should become the first built-in templates. The in-game GUI should live-edit preset values and preview them immediately, then export JSON.

This gives you:

- reusable shader visuals across all mods
- a stable API other mods can depend on
- a separate downloadable dependency
- less repeated renderer code
- faster ability visual creation
- a strong creator tool inside the game

The first target should be making Quick Slashes, Cross Strike, Slash Fury, Dual Wield Flurry, Basic Attack Slash, and Sword Beam Projectile work through the API with presets. Once those are migrated, the system is proven.
