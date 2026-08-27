# Writing an addon for SoloCraft: Reawakening

SoloCraft exposes a small API under `net.solocraft.api` so other mods can add
hunter classes, Ruler and Monarch vessels, Spiritualizations, and abilities that
behave like the built-in ones.

There is a complete worked example at `addons/slr-example`. If you would rather
read code than prose, start there — this document explains *why* it does what it
does.

Two routes are covered:

- [Part 1 — a Java addon](#part-1--a-java-addon)
- [Part 2 — an MCreator addon](#part-2--an-mcreator-addon)

---

## What the API gives you, and what it does not

**It gives you** identity (classes and vessels the mod will recognise, persist,
and display), state (whether a hunter is spiritualized), and the shared systems
your ability needs to feel native — effective stats, the mana economy, cooldowns,
auras, and the left-click attack.

**It does not decide what your ability does.** You describe an ability in JSON and
write the code that runs it. The mod handles everything around that: teaching it,
listing it, equipping it, pricing it, cooling it down, drawing its tooltip, and
casting it from the skill key.

That split is the point. Description is data, behaviour is yours, and a player
cannot tell which mod an ability came from by how they use it.

---

# Part 1 — a Java addon

## 1. Project setup

Your addon is an ordinary NeoForge 1.21.1 mod that compiles against SoloCraft's
jar and never bundles it.

`gradle.properties`:

```properties
minecraft_version=1.21.1
neo_version=21.1.244
sololeveling_version=1.3.0
sololeveling_project_dir=../../slbc-neoforge-1.21.1
```

`build.gradle` — the important part is `compileOnly` plus `localRuntime`:

```groovy
def soloLevelingJar = file("${project.sololeveling_project_dir}/build/libs/" +
        "SLR${project.sololeveling_version}-neoforge-${project.minecraft_version}.jar")

dependencies {
    // compileOnly: never packaged into your jar.
    // localRuntime: your dev client boots with the real mod installed.
    compileOnly files(soloLevelingJar)
    localRuntime files(soloLevelingJar)

    // SoloCraft's own required dependency, so the dev run can start.
    localRuntime "software.bernie.geckolib:geckolib-neoforge-${minecraft_version}:4.9.2"
}
```

Name the jar exactly rather than globbing `SLR*`. `build/libs` keeps older
releases, and a stale jar on the compile classpath will shadow the API.

`neoforge.mods.toml` — depend on SoloCraft **required**, so your addon refuses to
load rather than half-working:

```toml
[[dependencies.yourmod]]
modId="sololeveling"
type="required"
versionRange="[1.3.0,)"
ordering="AFTER"
side="BOTH"
```

On CurseForge, add SoloCraft as a *Required Dependency* relation too. The
CurseForge app then installs it alongside your addon.

## 2. Registering a hunter class

```java
public static final HunterClass NECROMANCER = HunterClassRegistry.register(
        new HunterClass(
                ResourceLocation.fromNamespaceAndPath("yourmod", "necromancer"),
                HunterClass.NO_LEGACY_ID,
                "hunterclass.yourmod.necromancer",   // lang key
                "Necromancer"));                     // fallback if untranslated
```

Call this **during mod construction**. Anything registered later is missing for
whatever already read the registry.

Assign it:

```java
HunterClassRegistry.assign(player, NECROMANCER);   // server side
HunterClass current = HunterClassRegistry.of(player);
```

### Why `NO_LEGACY_ID` matters

Class identity in SoloCraft has always been the number in
`PlayerVariables.Classes`, compared against literals in dozens of places. Your
class has no number it could safely claim, so it uses `NO_LEGACY_ID` and the
registry writes a single reserved mirror value into `Classes` instead. That value
reads as *awakened, but not one you recognise* to the old comparisons, which is
exactly right: they were written before your class existed and should not match
it.

The consequence to plan for: **the built-in class systems will not grant your
class anything.** Stats, passives, and skill lists are all keyed on the numeric
values. Your class is recognised, displayed, and persisted; giving it behaviour
is your job.

### Letting the Evaluator roll it

Registering a class makes it exist. Shipping a **class presentation** makes the
Evaluator able to hand it out:

```json
// data/yourmod/sololeveling/classes/necromancer.json
{
  "class": "yourmod:necromancer",
  "description": "Authority borrowed from what is already buried.",
  "color": "#8FE3B0"
}
```

| Field | Meaning |
| --- | --- |
| `class` | The class you registered in code. A file naming an unregistered class is reported and skipped |
| `description` | The line shown under the class name during evaluation. Required |
| `color` | `#RRGGBB`, used wherever the class is themed. Optional, defaults to neutral |

There is no logo field. The Evaluator draws contributed classes with their
description and colour and no emblem, because a missing emblem reads worse than
none at all.

**Odds are equal.** A contributed class is drawn as often as any built-in, and
rerolling walks the pool without repeats — so with one addon installed you are
drawing from seven classes rather than six.

Shipping the file is the opt-in. A class with no presentation still exists and
can still be granted by your own items; it just is not something the System
offers. That is the difference between a class your addon awards and one a
player can awaken into.

### Styles

A class can offer styles, which is how the built-in classes split into
specialisations:

```java
HunterStyleRegistry.register(new HunterStyle(
        ResourceLocation.fromNamespaceAndPath("yourmod", "gravebound"),
        NECROMANCER.id(),
        "Gravebound",
        "Keeps what it raises."));
```

Styles are stored in the existing `classStyle` field, so they persist and sync
with no extra work.

**The evaluator draws them too.** Register two or more and evaluation offers
them the way it offers Fire Mage or Barrier Mage — same non-repeating bag, same
reroll behaviour. Register none and the class simply has no style, which is also
a valid shape.

What persists is the style's **key**, never its number, so registering another
style later cannot silently move a player onto a different one.

Styles are matched against built-ins first, so you cannot add a style to a
shipped class this way — a Mage's five stay exactly five.

## 3. Registering a vessel

```java
public static final Vessel GRAVE_SOVEREIGN = VesselRegistry.register(
        Vessel.of(
                ResourceLocation.fromNamespaceAndPath("yourmod", "grave_sovereign"),
                Vessel.Kind.MONARCH,               // or RULER
                "Kaelith",                         // the wielder
                "Sovereign of Graves",             // the power
                "Command the restless dead."));    // shown under the power
```

Your vessel appears in the normal selection screen and is selectable through the
normal Job Change flow, under the same gating a built-in answers to: trial
completion, the selection-ready gate, and the server claim limit.

`VesselRegistry.assign(player, vessel)` exists for testing and for granting a
vessel outside the selection flow. It bypasses the trial, so do not ship it as a
player-facing route unless you mean to.

Read the current vessel with `VesselRegistry.of(player)`, which returns
`Optional<Vessel>` and resolves built-ins too.

## 4. Declaring an ability

Abilities are declared in JSON and behave in code. Put the file in
`data/<yourmod>/sololeveling/abilities/`:

```json
{
  "name": "Cinder Slash",
  "summary": "A forward cut that leaves embers on the target.",
  "detail": "Burns for 3 seconds | Emberline only",
  "accent": "red",
  "cost": "LOW",
  "cooldown_ticks": 60,
  "owning_class": "yourmod:runeblade",
  "icon": "yourmod:textures/ability/cinder_slash.png",
  "executor": "net.yourmod.abilities.CinderSlash"
}
```

Every field is shown to players somewhere, which is why none of them are
optional decoration:

| Field | Where it appears |
| --- | --- |
| `name` | Skill list, equipped slots, top-left overlay, tooltip title |
| `summary` | Grey tooltip line |
| `detail` | Yellow tooltip line |
| `accent` | Colour of the name everywhere it is drawn |
| `cost` | `NOMINAL`, `LOW`, `MEDIUM`, `HIGH`, `APEX` |
| `cooldown_ticks` | Recovery after a successful cast |
| `owning_class` | Class id, or omit for any class |
| `icon` | 20x20 texture drawn in the HUD ability slot |
| `executor` | The class that runs it |

Contributed names are shown with `(ADN)` in front. The stored name stays clean,
so the marker never reaches save data.

### The slot icon

`icon` is the texture drawn in the ability slot on the HUD, the same place a
built-in ability's icon appears. Put a **20x20** PNG at the path you name — the
game blits it at 20x20 with no scaling, so a larger file is cropped rather than
shrunk.

```
src/main/resources/assets/yourmod/textures/ability/cinder_slash.png
```

The field is optional. An ability without one shows the empty slot template,
which is also what a built-in without an icon shows, so leaving it out looks
unfinished rather than broken.

Built-in icons are matched first, so an addon can never replace a shipped one.

The icon travels to the client inside the definition packet along with the name
and colour. That matters on a dedicated server: the HUD is client-side, and an
icon the client was never told about would draw as the empty template.

### The executor

```java
public final class CinderSlash implements AbilityExecutor {
    @Override
    public void execute(AbilityContext context) {
        ServerPlayer player = context.player();
        // ... the effect ...
    }
}
```

Needs a public no-argument constructor. Built once and reused, so hold no
per-player state — anything that varies per cast is on the context.

By the time it runs, the mod has confirmed the hunter holds the right class,
learned the ability, is off cooldown, and can afford it.

### Cost scales with what you actually did

Mana is settled **after** the effect, so an ability that hit four targets does
not cost the same as one that hit nothing:

```java
context.acceptedTargets(struck);      // count what it landed on, not what it aimed at
context.stage(3);                     // for staged abilities, 1-5
context.executionModifier(1.4D);      // anything the mod cannot see
context.noEffect();                   // nothing happened; charge nothing
```

Leave them alone and the ability costs its band's base, which is right for
something that always does the same thing.

**Never scale the modifier by Intelligence.** Intelligence already raises maximum
mana and cost is a fraction of that maximum, so a second Intelligence term scales
the stat twice and makes investment feel punishing.

### Toggle abilities

Add a mode and an upkeep, and the ability stays on until turned off or unpaid:

```json
{
  "name": "Tempest Decree",
  "mode": "toggle",
  "upkeep_per_second": 14,
  ...
}
```

```java
@Override public void execute(AbilityContext context)    { /* turn on  */ }
@Override public void deactivate(AbilityContext context) { /* turn off */ }
```

The mod drains the upkeep once a second and ends the ability the moment a second
cannot be paid for, on the same cadence its own spiritualizations use. It clears
the form state itself; the aura, the summon, the attribute you added are yours to
remove in `deactivate`.

A toggle is held as a `VesselState` form, so while it is on the hunter **counts
as spiritualized**, can carry an aura, and can claim the attack button — with
nothing extra to register.

### Teaching it

Runestones are how the mod teaches a class ability. Register one per ability:

```java
ITEMS.register("runestone_cinder_slash", () -> new HunterRunestoneItem("Cinder Slash"));
```

The stone names its ability rather than holding it, because items register during
mod construction while JSON arrives with the datapack. Supply a model and texture;
the enchanted look, the tooltip, teaching on right click, and being spent only
when something was learned all come with it.

### Reading the hunter

`HunterStats` gives the five combat stats as **effective** values — including
equipment, effects, and buffs:

```java
HunterStats.strength(player);      // agility, perception, vitality, intelligence
```

`HunterProgress` gives everything else, read-only:

```java
HunterProgress.level(player);              HunterProgress.skillPoints(player);
HunterProgress.rank(player);               HunterProgress.hasKilledBoss(player);
HunterProgress.isCastleUnlocked(player);   HunterProgress.hasActiveDailyQuest(player);
HunterProgress.isSecretDailyQuest(player);
```

Reading raw `PlayerVariables` fields instead would ignore every buff in the game.

## 5. Spiritualization

Two things must happen, and neither implies the other.

```java
// State: what abilities and the melee claim branch on.
VesselState.setFormActive(player, "yourmod:grave_spiritualization", true);

// Visual: who is actually wearing the aura.
PlayerAuraSystem.setContinuous(player, "yourmod_grave", 1.25F);
PlayerAuraSystem.burst(player, "yourmod_grave", 24, 1.9F);
```

Registering an aura recipe does **not** make it appear on anyone. Auras are
pushed to clients, not derived from player state. Register the recipe client-side
during `FMLClientSetupEvent`:

```java
PlayerAuraRegistry.register(new PlayerAuraDefinition(
        "yourmod_grave",
        0x8FE3B0,                 // primary colour
        0x0B2A16,                 // secondary colour
        yourGlowTexture,
        PlayerAuraDefinition.Facing.HORIZONTAL_CAMERA,
        0.88F,                    // radius
        1.52F,                    // height scale
        0.86F,                    // speed
        0, 0, 0,                  // shell layers, wisps, spikes
        new PlayerAuraDefinition.FluidProfile(0, 0, 0, 1.08F, 0.88F, 1.60F, 1.05F,
                PlayerAuraDefinition.FluidStyle.SHADOW_RIFT),
        false));                  // ground ring
```

Three fluid styles exist: `LIQUID_FLAME`, `SHADOW_RIFT`, and `WHITE_FLAME_HAIR`.

**Activation is server-side.** A transformation the client can grant itself is a
transformation every client grants itself.

Once your form is active, `VesselState.isSpiritualized(player)` returns true —
including to the built-in question, so an ability that branches on it transforms
for your vessel as well as the shipped ones.

## 6. Taking over left click

```java
// During mod construction:
VesselMelee.claimForForm("yourmod:grave_spiritualization", 150);
VesselState.declareMeleeClaimingForm("yourmod:grave_spiritualization");
```

```java
@SubscribeEvent
public static void onVesselMelee(VesselMeleeAttackEvent event) {
    // Other addons' claims reach this subscriber too.
    if (!"yourmod:grave_spiritualization".equals(event.getFormId()))
        return;
    ServerPlayer player = event.getPlayer();
    // ... your attack ...
}
```

Claims carry a priority; highest wins, and equal priorities tie-break on the
claim id so the outcome is identical on every install regardless of load order.

**Built-in stances always win.** Goliath and the Beast Monarch are tested before
any contributed claim, so you cannot take the attack button away from a shipped
vessel.

Reaching the event proves the player pressed a button and nothing more. Rate
limiting and resource costs belong in your handler, because a modified client can
press as fast as it likes.

## 6b. Theming a Monarch

A contributed Monarch can declare its own colour and pick which animated
backdrop plays behind the selection panel:

```json
// data/yourmod/sololeveling/vessels/kaelith.json
{
  "vessel": "yourmod:kaelith",
  "color": "#8FE3B0",
  "backdrop": "frost"
}
```

| Field | Meaning |
| --- | --- |
| `vessel` | The Monarch you registered in code |
| `color` | `#RRGGBB`. Drives the name, the accents, the cursor glow, and the tint over the backdrop |
| `backdrop` | `shadow`, `frost`, `white_flame`, `beast`, `destruction`, or `system`. Optional, defaults to `shadow` |

**Monarchs only.** Rulers all present alike by design — that sameness is what
separates the two columns — so a file naming a Ruler is reported and skipped
rather than quietly doing nothing.

You pick a backdrop rather than shipping one because the animations are GLSL
inside the mod's core shader, and a core shader cannot be extended from outside.
The one you choose is **tinted toward your colour**, so two Monarchs that picked
`frost` still read as different.

A Monarch with no presentation keeps the neutral Monarch theming, exactly as
before this existed.

## 6c. Armour and weapon spiritualization

Goliath equips an armour set while its stance holds; Liu Zhigang replaces what
is in your hands with a manifested sword. `SpiritualizationGear` is both:

```java
// in your toggle ability's execute
SpiritualizationGear.equipArmor(player, FORM,
        new ItemStack(MY_HELMET.get()), new ItemStack(MY_CHESTPLATE.get()),
        new ItemStack(MY_LEGGINGS.get()), new ItemStack(MY_BOOTS.get()));

// in deactivate
SpiritualizationGear.restore(player, FORM);
```

`equipWeapon(player, FORM, mainHand, offHand)` is the same shape for hands, and
one form may hold both. Pass an empty stack for a slot your form leaves alone —
the player's own piece stays on and is not recorded, so a partial set works.

**Equipping is the easy half.** The reason to use this rather than setting slots
yourself is everything after: death, logout, a dropped stack, an inventory
shuffle, and a server that stopped mid-form. Each of those either duplicates
your temporary item or eats the player's real gear, and all of them are already
handled — the gear is swept on death, logout, and login, and it cannot be
tossed, dropped, or picked up by anyone else.

`restore` is safe to call when the form equipped nothing, so your deactivate
path does not have to remember which of the two it used.

## 7. Gotchas

| Symptom | Cause |
| --- | --- |
| Registry entry missing | Registered after mod construction |
| Ability ignores buffs | Reading raw `PlayerVariables` fields instead of `HunterStats` |
| Investment feels punishing | Adding an Intelligence term on top of a cost band |
| Creative players charged mana | Subtracting mana directly instead of `HunterMana.spend` |
| HUD shows stale mana | Same |
| Your cooldown gets cancelled | Un-namespaced cooldown key |
| Form never activates | Comma inside a form id; they are stripped, splitting the id |
| Aura registered but invisible | Never called `PlayerAuraSystem.setContinuous` |
| Addon silently absent | `type="optional"` in mods.toml instead of `required` |

---

# Part 2 — an MCreator addon

MCreator can drive this API, with one real limitation you should know up front.

## What works, and what does not

**Works.** The API is deliberately flat static methods over primitives —
`HunterMana.spend(entity, 200)`, `HunterStats.intelligence(entity)`,
`VesselState.setFormActive(entity, "yourmod:form", true)`. Every one of these is
a single line that a *custom code* procedure block can emit.

**Abilities need no Java at all.** Build the effect as an ordinary MCreator
procedure, then name its generated class in your ability JSON:

```json
"executor": "net.mcreator.yourmod.procedures.CinderSlashProcedure"
```

The mod adapts a procedure's static `execute` directly. Three shapes are
recognised, which between them cover almost every player-triggered procedure:

| Procedure signature |
| --- |
| `execute(LevelAccessor, double, double, double, Entity)` |
| `execute(LevelAccessor, Entity)` |
| `execute(Entity)` |

The player is passed as the entity and their position as the coordinates.

**Does not work well.** Subscribing to `VesselMeleeAttackEvent` needs a NeoForge
event subscriber, which MCreator does not expose. If you want a custom left-click
attack, bind your own key instead — MCreator supports custom keybinds natively,
and it costs you nothing except that the attack is on a different button.

## Setting the workspace up

1. Create a NeoForge 1.21.1 workspace.
2. Put `SLR1.3.0-neoforge-1.21.1.jar` in the workspace's `libs/` folder.
3. In **Workspace → Settings → Dependencies**, add SoloCraft so the generated
   `mods.toml` declares it. If your MCreator version has no such field, edit the
   generated `src/main/resources/META-INF/neoforge.mods.toml` and add the
   `[[dependencies.yourmod]]` block from Part 1. Re-check it after a regenerate.
4. Confirm the jar is on the compile classpath by building once. If MCreator does
   not pick up `libs/` automatically, add to the workspace `build.gradle`:

   ```groovy
   dependencies {
       compileOnly fileTree(dir: 'libs', include: ['*.jar'])
       localRuntime fileTree(dir: 'libs', include: ['*.jar'])
   }
   ```

> MCreator's UI moves between versions. If a menu name here does not match what
> you see, the underlying requirement is the same: SoloCraft's jar on the compile
> classpath, and a required dependency in the generated mods.toml.

## Calling the API from a procedure

Use a **custom code** block inside any procedure. The entity variable MCreator
gives you is a `net.minecraft.world.entity.Entity`, which is what the API takes.

Scale an ability off Intelligence:

```java
double intelligence = net.solocraft.api.HunterStats.intelligence(entity);
double damage = 3.0D + intelligence * 0.08D;
```

Charge mana, and stop if the player cannot pay:

```java
int cost = net.solocraft.api.HunterMana.cost(entity,
        net.solocraft.api.AbilityCost.MEDIUM);
if (!net.solocraft.api.HunterMana.spend(entity, cost)) {
    return;
}
```

Branch on Spiritualization:

```java
if (net.solocraft.api.vessel.VesselState.isSpiritualized(entity)) {
    // transformed variant
}
```

Toggle your own Spiritualization, aura included:

```java
if (entity instanceof net.minecraft.server.level.ServerPlayer player) {
    net.solocraft.api.vessel.VesselState.setFormActive(player, "yourmod:form", true);
    net.solocraft.util.PlayerAuraSystem.setContinuous(player, "yourmod_aura", 1.25F);
}
```

Hold a cooldown that behaves like every other cooldown in the game:

```java
if (net.solocraft.api.AbilityCooldowns.isOnCooldown(entity, "yourmod", "burst")) {
    return;
}
net.solocraft.api.AbilityCooldowns.set(entity, "yourmod", "burst", 60);
```

Use fully qualified names in custom code blocks — MCreator manages the import
list of generated files, and hand-added imports tend not to survive a regenerate.

## Registering a class or vessel from MCreator

Registration must run during mod construction, which is earlier than any
procedure trigger. MCreator has no "on mod construction" trigger, so this one
step needs a small hand-written class in the workspace's `src/main/java`:

```java
package net.mcreator.yourmod;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

import net.solocraft.api.hunter.HunterClass;
import net.solocraft.api.hunter.HunterClassRegistry;

@Mod("yourmod_slr")
public final class YourModSoloCraftContent {
    public static final HunterClass NECROMANCER = HunterClassRegistry.register(
            new HunterClass(
                    ResourceLocation.fromNamespaceAndPath("yourmod", "necromancer"),
                    HunterClass.NO_LEGACY_ID,
                    "hunterclass.yourmod.necromancer",
                    "Necromancer"));

    public YourModSoloCraftContent(IEventBus modEventBus, ModContainer container) {
    }
}
```

Everything after that — awakening a player into it, abilities, effects — can be
ordinary MCreator procedures calling `HunterClassRegistry.assign(entity, ...)`.

Keep this file outside the packages MCreator regenerates, and it will survive
workspace rebuilds.

---

## Reference

`addons/slr-example` registers one of everything described here and can be run
directly. Its commands (`/slrexample status`, `class`, `vessel`, `spiritualize`,
`ability`) exercise the whole surface in a live game.
