# Building a SoloCraft addon

A complete walkthrough for writing an addon in IntelliJ IDEA or Eclipse, starting
from nothing but the SoloCraft jar you already have.

You do **not** need SoloCraft's source code. You compile against the same jar you
would drop in your `mods` folder to play.

If you use MCreator instead of an IDE, the JSON half of this guide still applies
exactly; only [section 6](#6-writing-the-executor) changes.

**The goal to keep in mind:** a player should not be able to tell which mod a
thing came from by how they use it. No commands of your own, no keybinds of your
own. Your abilities are taught by runestones, appear in the skill list, equip to
slots, and cast from the skill key, exactly like the ones the mod ships.

---

## Contents

1. [What you need](#1-what-you-need)
2. [Starting the project](#2-starting-the-project)
3. [`build.gradle`](#3-buildgradle)
4. [The file layout](#4-the-file-layout)
5. [Your first ability, end to end](#5-your-first-ability-end-to-end)
6. [Writing the executor](#6-writing-the-executor)
7. [Reading game data — the full API](#7-reading-game-data--the-full-api)
8. [Paying for an ability](#8-paying-for-an-ability)
9. [Custom classes, styles and vessels](#9-custom-classes-styles-and-vessels)
10. [Toggles and Spiritualization](#10-toggles-and-spiritualization)
11. [Running and debugging](#11-running-and-debugging)
12. [Shipping it](#12-shipping-it)
13. [Troubleshooting](#13-troubleshooting)

---

## 1. What you need

| | |
| --- | --- |
| JDK | 21 (Temurin or equivalent) |
| IDE | IntelliJ IDEA (Community is fine) or Eclipse |
| NeoForge MDK | for 1.21.1 — from [neoforged.net](https://neoforged.net), Downloads → MDK |
| **SoloCraft jar** | `SLR1.3.0-neoforge-1.21.1.jar` or newer |

### Getting the SoloCraft jar

Whatever you downloaded to play the mod is the jar you compile against. Grab it
from CurseForge, or copy it straight out of your `.minecraft/mods` folder.

There is nothing to build and no source to clone. The jar contains the whole
addon API.

### Versions the jar expects

| | |
| --- | --- |
| Minecraft | 1.21.1 |
| NeoForge | 21.1.244+ |
| GeckoLib | `[4.9.2,5)` — required |

Sodium, Iris, and BetterCombat are optional for SoloCraft and irrelevant to you.

---

## 2. Starting the project

1. Download the **NeoForge MDK for 1.21.1** and unzip it. This gives you the
   Gradle wrapper (`gradlew` / `gradlew.bat`) and a working skeleton — you cannot
   easily build a NeoForge project without it.
2. Rename the folder to your addon's name.
3. Make a `libs/` folder at the top level and **drop the SoloCraft jar in it**.

```
myaddon/
├── gradlew
├── gradlew.bat
├── gradle/wrapper/
├── build.gradle
├── gradle.properties
├── settings.gradle
└── libs/
    └── SLR1.3.0-neoforge-1.21.1.jar   ← the jar you play with
```

Keeping the jar inside the project means the build works on any machine you copy
it to, and you can commit it or gitignore it as you prefer.

### `gradle.properties`

Replace the MDK's version block with these, and set your own ids:

```properties
minecraft_version=1.21.1
minecraft_version_range=[1.21.1]
neo_version=21.1.244
loader_version_range=[1,)
parchment_minecraft_version=1.21.1
parchment_mappings_version=2024.11.17

# The exact filename of the jar in libs/. Change this when you update SoloCraft.
sololeveling_jar=SLR1.3.0-neoforge-1.21.1.jar

mod_id=myaddon
mod_name=My Addon
mod_version=0.1.0
mod_group_id=com.example.myaddon
mod_license=All rights reserved
```

### Import it

**IntelliJ:** `File → Open`, select the folder, let Gradle sync. Nothing else.
Do *not* run `genIntellijRuns` — ModDevGradle 2 creates the run configurations
during sync. You should see `runClient` and `runServer` in the Gradle tool
window under `Tasks → neoforge`.

**Eclipse:** `./gradlew.bat eclipse`, then import as an existing project.

---

## 3. `build.gradle`

Start from the MDK's file and change the `dependencies`, `repositories`, and
`mods` blocks. The parts that matter:

```groovy
plugins {
    id 'java-library'
    id 'eclipse'
    id 'idea'
    id 'net.neoforged.moddev' version '2.0.143'
}

version = project.mod_version
group = project.mod_group_id
base { archivesName = project.mod_id }

java.toolchain.languageVersion = JavaLanguageVersion.of(21)

def soloLevelingJar = file("libs/${project.sololeveling_jar}")

neoForge {
    version = project.neo_version

    parchment {
        minecraftVersion = project.parchment_minecraft_version
        mappingsVersion = project.parchment_mappings_version
    }

    runs {
        client { client(); gameDirectory = project.file('run') }
        server { server(); gameDirectory = project.file('run-server') }
    }

    mods {
        myaddon { sourceSet sourceSets.main }
    }
}

// localRuntime puts a dependency in the dev game without shipping it.
configurations { runtimeClasspath.extendsFrom localRuntime }

repositories {
    mavenCentral()
    maven {
        name = 'GeckoLib'
        url = 'https://dl.cloudsmith.io/public/geckolib3/geckolib/maven/'
        content {
            includeGroup 'software.bernie.geckolib'
            includeGroup 'com.eliotlash.mclib'
        }
    }
}

dependencies {
    // compileOnly  -> you can call the API, but SoloCraft is never packaged
    //                 into your jar.
    // localRuntime -> your dev client actually boots with SoloCraft installed.
    compileOnly files(soloLevelingJar)
    localRuntime files(soloLevelingJar)

    // SoloCraft's own required dependency. Without it the dev run will not start.
    localRuntime "software.bernie.geckolib:geckolib-neoforge-${minecraft_version}:4.9.2"
}

// A missing jar is the most common setup failure. Say so plainly rather than
// letting it surface as a hundred unresolved symbols.
tasks.named('compileJava') {
    doFirst {
        if (!soloLevelingJar.isFile()) {
            throw new GradleException(
                    "SoloCraft jar not found at libs/${project.sololeveling_jar}\n" +
                    "Copy it there from your mods folder, and make sure " +
                    "sololeveling_jar in gradle.properties matches the filename exactly.")
        }
    }
}

tasks.withType(JavaCompile).configureEach { options.encoding = 'UTF-8' }
```

**Why `compileOnly` and never `implementation`:** `implementation` would copy
SoloCraft's 40 MB of classes inside your jar. Two copies of the same mod on one
classpath is a crash, not a warning.

---

## 4. The file layout

This is the part that is easy to get subtly wrong, because a misplaced JSON does
not error — it is simply never read.

```
myaddon/
├── libs/SLR1.3.0-neoforge-1.21.1.jar
└── src/main/
    ├── java/com/example/myaddon/
    │   ├── MyAddon.java              ← @Mod entry point
    │   └── EmberBolt.java            ← one executor per ability
    └── resources/
        ├── META-INF/
        │   └── neoforge.mods.toml    ← metadata + dependency on SoloCraft
        ├── pack.mcmeta
        ├── assets/myaddon/           ← CLIENT: things that get drawn
        │   ├── lang/en_us.json
        │   ├── models/item/*.json
        │   └── textures/
        │       ├── ability/*.png     ← 20x20 HUD slot icons
        │       └── item/*.png
        └── data/myaddon/             ← SERVER: things that get declared
            └── sololeveling/         ← SoloCraft's namespace, not yours
                ├── abilities/*.json
                ├── classes/*.json
                └── vessels/*.json
```

### The one path rule worth memorising

```
data/<your-mod-id>/sololeveling/<kind>/<name>.json
     ^^^^^^^^^^^^^ ^^^^^^^^^^^^
     YOUR namespace  SoloCraft's directory
```

The **first** segment is your mod id — it is what keeps your files from colliding
with another addon's. The **second** is always `sololeveling`, because that is
the directory SoloCraft's reload listeners scan.

Getting these backwards is the single most common mistake. `data/sololeveling/...`
looks right and is silently ignored.

### assets vs data

| | `assets/` | `data/` |
| --- | --- | --- |
| Loaded on | Client only | Server only |
| Holds | Textures, models, lang | Ability/class/vessel declarations |
| Reloaded by | F3+T | `/reload` |

Anything under `data/` that the client needs to **draw** is sent over the network
by SoloCraft — you do not have to do anything for that, but it explains why an
ability icon is a path declared in `data/` pointing at a file in `assets/`.

---

## 5. Your first ability, end to end

Six files and you have a working, castable ability. No custom class needed — this
one is available to any hunter.

### 5.1 `src/main/resources/pack.mcmeta`

```json
{
  "pack": {
    "description": "My Addon",
    "pack_format": 34
  }
}
```

### 5.2 `src/main/resources/META-INF/neoforge.mods.toml`

```toml
modLoader="javafml"
loaderVersion="[1,)"
license="All rights reserved"

[[mods]]
modId="myaddon"
version="0.1.0"
displayName="My Addon"
authors="you"
description='''Adds an Ember Bolt ability to SoloCraft: Reawakening.'''

[[dependencies.myaddon]]
modId="neoforge"
type="required"
versionRange="[21.1.244,)"
ordering="NONE"
side="BOTH"

[[dependencies.myaddon]]
modId="minecraft"
type="required"
versionRange="[1.21.1]"
ordering="NONE"
side="BOTH"

[[dependencies.myaddon]]
modId="sololeveling"
type="required"
versionRange="[1.3.0,)"
ordering="AFTER"     # you load after SoloCraft, so its registries exist
side="BOTH"
```

`ordering="AFTER"` is not optional. `type="required"` means the game refuses to
start without SoloCraft rather than crashing halfway through your mod class.

### 5.3 `MyAddon.java`

```java
package com.example.myaddon;

import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import net.solocraft.api.skill.HunterRunestoneItem;

@Mod(MyAddon.MOD_ID)
public final class MyAddon {
    public static final String MOD_ID = "myaddon";

    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(MOD_ID);

    // The runestone that teaches the ability. It takes the ability's NAME,
    // because items register now and the JSON arrives later with the data pack.
    public static final DeferredItem<Item> RUNESTONE_EMBER_BOLT =
            ITEMS.register("runestone_ember_bolt",
                    () -> new HunterRunestoneItem("Ember Bolt"));

    public MyAddon(IEventBus modEventBus, ModContainer container) {
        ITEMS.register(modEventBus);
    }
}
```

You need **no creative-tab code**. SoloCraft sweeps every registered
`HunterRunestoneItem` into its own Runestones tab.

### 5.4 `EmberBolt.java`

```java
package com.example.myaddon;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

import net.solocraft.api.HunterStats;
import net.solocraft.api.skill.AbilityExecutor;

public final class EmberBolt implements AbilityExecutor {
    private static final double REACH = 6.0D;
    private static final double DAMAGE_PER_INT = 0.18D;

    @Override
    public void execute(AbilityContext context) {
        ServerPlayer player = context.player();

        float damage = (float) (5.0D
                + HunterStats.intelligence(player) * DAMAGE_PER_INT);

        int struck = 0;
        AABB area = player.getBoundingBox().inflate(REACH);
        for (LivingEntity target :
                context.level().getEntitiesOfClass(LivingEntity.class, area)) {
            if (target == player) continue;
            target.hurt(player.damageSources().playerAttack(player), damage);
            struck++;
        }

        // Tell the mod what actually happened so it can price the cast.
        if (struck == 0) context.noEffect();
        else context.acceptedTargets(struck);
    }
}
```

### 5.5 `data/myaddon/sololeveling/abilities/ember_bolt.json`

```json
{
  "name": "Ember Bolt",
  "summary": "A lance of ember that scatters through anything close.",
  "detail": "Scales with Intelligence | costs more the more it reaches",
  "accent": "red",
  "cost": "LOW",
  "cooldown_ticks": 60,
  "icon": "myaddon:textures/ability/ember_bolt.png",
  "executor": "com.example.myaddon.EmberBolt"
}
```

| Field | Required | Where it shows up |
| --- | --- | --- |
| `name` | yes | Skill list, equipped slots, top-left overlay, tooltip title |
| `summary` | yes | Grey tooltip line |
| `detail` | no | Yellow tooltip line |
| `accent` | yes | Colour of the name everywhere it is drawn |
| `cost` | yes | `NOMINAL`, `LOW`, `MEDIUM`, `HIGH`, `APEX` |
| `cooldown_ticks` | yes | Recovery after a successful cast (20 ticks = 1s). On a toggle: how long until it can be turned back **on** |
| `owning_class` | no | Descriptive. Groups the ability; omit for any class |
| `icon` | no | 20x20 texture in the HUD ability slot |
| `mode` | no | `instant` (default) or `toggle` |
| `upkeep_per_second` | toggles only | Mana drained each second while held |
| `executor` | yes | Fully-qualified class name that runs it |

`owning_class` does **not** stop other classes from casting. It groups and
labels. If you want a real restriction, check it yourself in the executor.

Contributed names display with `(ADN)` in front. The stored name stays clean, so
the marker never reaches save data.

### 5.6 Assets

`assets/myaddon/lang/en_us.json`:

```json
{
  "item.myaddon.runestone_ember_bolt": "Runestone: Ember Bolt"
}
```

`assets/myaddon/models/item/runestone_ember_bolt.json` — borrow a vanilla texture
until you draw your own:

```json
{
  "parent": "minecraft:item/generated",
  "textures": {
    "layer0": "minecraft:item/blaze_powder"
  }
}
```

And a **20x20 PNG** at `assets/myaddon/textures/ability/ember_bolt.png` for the
HUD slot. Skip it and the slot shows the mod's empty template, which is what a
built-in without an icon shows too.

### 5.7 Run it

```bash
./gradlew.bat runClient
```

Open creative, find the **Runestones** tab, take your stone, right-click it.
The ability appears in your skill list. Equip it to a slot and cast it with the
skill key.

---

## 6. Writing the executor

Requirements:

- **A public no-argument constructor.** The executor is built once and reused.
- **`execute` runs on the server, on the game thread.** You already have a
  `ServerPlayer` and a `ServerLevel`; there is no side check to write.
- Implement `deactivate(AbilityContext)` too if your ability is a toggle.

### Reporting back

| Call | Meaning |
| --- | --- |
| `context.acceptedTargets(n)` | You reached `n` things. Scales the cost |
| `context.noEffect()` | Nothing happened. Do not bill a full cast |
| `context.stage(n)` | You reached charge stage `n` |
| `context.executionModifier(d)` | Arbitrary multiplier on the final cost |

These are the difference between an ability that costs a flat rate for a miss and
one that prices what it did.

### If you use MCreator

Point `executor` at the class MCreator generated for your procedure. SoloCraft
adapts the standard MCreator `execute` signatures by reflection, so you do not
have to implement `AbilityExecutor` by hand. Everything else in this guide is
unchanged.

---

## 7. Reading game data — the full API

Everything below is a static call taking an `Entity`. Import from
`net.solocraft.api`.

### Stats — `HunterStats`

```java
double strength     = HunterStats.strength(player);
double agility      = HunterStats.agility(player);
double perception   = HunterStats.perception(player);
double vitality     = HunterStats.vitality(player);
double intelligence = HunterStats.intelligence(player);
double level        = HunterStats.level(player);
```

**These are effective values.** They include equipment, active effects, and
temporary buffs. Reading the player attachment yourself would silently ignore
every one of them, and your ability would quietly stop responding to the systems
built to empower it. Always use these.

### Progress — `HunterProgress`

```java
int     skillPoints  = HunterProgress.skillPoints(player);
int     level        = HunterProgress.level(player);
String  rank         = HunterProgress.rank(player);          // "S", "A", ...
int     rankNumber   = HunterProgress.rankNumber(player);
int     hunterRank   = HunterProgress.hunterRank(player);

boolean killedBoss   = HunterProgress.hasKilledBoss(player);

// Demon Castle
boolean unlocked     = HunterProgress.isCastleUnlocked(player);
boolean started      = HunterProgress.isCastleStarted(player);
int     clears       = HunterProgress.castleClears(player);

// Daily quest
boolean hasDaily     = HunterProgress.hasActiveDailyQuest(player);
boolean isSecret     = HunterProgress.isSecretDailyQuest(player);
int     tasksDone    = HunterProgress.dailyQuestTasksDone(player);
boolean inTraining   = HunterProgress.isInDailyTraining(player);
```

### Mana — `HunterMana`

```java
double  current = HunterMana.current(player);
double  maximum = HunterMana.maximum(player);
boolean free    = HunterMana.isFree(player);          // Creative
```

### Identity — registries

```java
HunterClass           clazz  = HunterClassRegistry.of(player);
Optional<HunterStyle> style  = HunterStyleRegistry.of(player);
Optional<Vessel>      vessel = VesselRegistry.of(player);

HunterClassRegistry.assign(player, RUNEBLADE);   // server side
HunterStyleRegistry.assign(player, EMBERLINE);
VesselRegistry.assign(player, GRAVE_SOVEREIGN);
```

### Spiritualization state — `VesselState`

This is how you branch an ability on whether a Spiritualization is running:

```java
if (VesselState.isSpiritualized(player)) {
    // the empowered variant
} else {
    // the ordinary one
}

boolean mine  = VesselState.isFormActive(player, MY_FORM_ID);
List<String> forms = VesselState.activeForms(player);
boolean melee = VesselState.isMeleeStanceActive(player);
```

### Skills — `HunterSkills`

```java
List<String> known = HunterSkills.learned(player);
boolean has = HunterSkills.hasLearned(player, "Ember Bolt");
HunterSkills.learn(player, "Ember Bolt");
HunterSkills.forget(player, "Ember Bolt");
```

### Cooldowns — `AbilityCooldowns`

Keys are namespaced `owner:ability`, so yours can never collide with the mod's or
another addon's.

```java
AbilityCooldowns.set(player, MOD_ID, "ember_bolt", 60);
boolean cooling = AbilityCooldowns.isOnCooldown(player, MOD_ID, "ember_bolt");
int ticks   = AbilityCooldowns.remainingTicks(player, MOD_ID, "ember_bolt");
int seconds = AbilityCooldowns.remainingSeconds(player, MOD_ID, "ember_bolt");
AbilityCooldowns.clear(player, MOD_ID, "ember_bolt");
```

You do not need this for an ability's own `cooldown_ticks` — the mod applies
that. Use it for secondary rate limits, like a melee swing inside a form.

### A worked example

Scaling an ability off several readings at once:

```java
@Override
public void execute(AbilityContext context) {
    ServerPlayer player = context.player();

    double power = HunterStats.intelligence(player);

    // A hunter who has cleared the castle hits harder with this.
    if (HunterProgress.castleClears(player) > 0) power *= 1.25D;

    // And harder still while spiritualized.
    if (VesselState.isSpiritualized(player)) power *= 1.4D;

    // Rank is a string; rankNumber is the one to compare.
    if (HunterProgress.rankNumber(player) >= 5) power += 20.0D;

    ...
}
```

---

## 8. Paying for an ability

Declare a **weight band**, not a number:

```json
"cost": "LOW"
```

`NOMINAL` → `LOW` → `MEDIUM` → `HIGH` → `APEX`. The band tracks the mod's mana
economy, so a rebalance moves your ability with everything else.

**Do not add your own Intelligence term to the cost.** Intelligence already
raises maximum mana, and the cost is a fraction of that maximum, so an
Intelligence term scales the stat twice and your ability gets cheaper the
stronger the caster is — the opposite of the intent.

The mod settles the cost **after** your executor returns, using what you reported
through `acceptedTargets` / `noEffect`. If you want to charge manually anyway:

```java
int cost = HunterMana.cost(player, AbilityCost.LOW);
if (!HunterMana.canAfford(player, cost)) return;
HunterMana.spend(player, cost);
```

`HunterMana` handles the Creative exemption, the floor at zero, and the HUD sync.
Subtracting the field yourself desyncs the bar and charges Creative players.

---

## 9. Custom classes, styles and vessels

Identity lives in Java, not JSON, because a JSON file has to be able to *point*
at a class or vessel — which means the target must already exist when data packs
load. Register all of it in your mod constructor.

### A hunter class

```java
public static final HunterClass RUNEBLADE = HunterClassRegistry.register(
        new HunterClass(
                ResourceLocation.fromNamespaceAndPath(MOD_ID, "runeblade"),
                HunterClass.NO_LEGACY_ID,
                "hunterclass.myaddon.runeblade",   // lang key
                "Runeblade"));                     // fallback if untranslated
```

`NO_LEGACY_ID` matters. Class identity in SoloCraft has always been a number
compared against literals in dozens of places. Yours has no number it could
safely claim, so the registry writes a reserved mirror value that reads as
*awakened, but not one you recognise* to that old code — which is correct, since
it was written before your class existed.

**The consequence to plan for:** built-in class systems will not grant your class
anything. Stats, passives, and skill lists are keyed on the numeric values. Your
class is recognised, displayed, persisted, and rollable. Giving it *behaviour* is
your job.

### Making it rollable by the Evaluator

Ship a class presentation. This file is the opt-in:

```json
// data/myaddon/sololeveling/classes/runeblade.json
{
  "class": "myaddon:runeblade",
  "description": "Carves its own runes and spends itself reading them back.",
  "color": "#B48CFF"
}
```

| Field | Meaning |
| --- | --- |
| `class` | The class you registered in code. A file naming an unregistered class is reported and skipped |
| `description` | The line shown under the class name during evaluation. Required |
| `color` | `#RRGGBB`, used wherever the class is themed. Optional |

There is no logo field — contributed classes draw with their description and
colour and no emblem, because a missing emblem reads worse than none at all.

**Odds are equal with the built-ins.** With one addon installed, players draw
from seven classes rather than six.

No presentation file means the class exists and can be granted by your own items,
but the System never offers it.

### Styles

```java
public static final HunterStyle EMBERLINE = HunterStyleRegistry.register(
        HunterStyle.of(ResourceLocation.fromNamespaceAndPath(MOD_ID, "emberline"),
                RUNEBLADE.id(), "Emberline",
                "Cuts that keep burning after they land", 0xFFF0783C));
```

Register two or more and the Evaluator offers them the way it offers Fire Mage or
Barrier Mage. Register one and the bag has nothing to draw between, so every
reroll returns the same answer.

What persists is the style's **key**, never its number, so adding a style later
cannot move a player onto a different one.

Styles are matched against built-ins first, so you cannot add a style to a
shipped class — a Mage's five stay exactly five.

### A vessel

```java
public static final Vessel GRAVE_SOVEREIGN = VesselRegistry.register(
        Vessel.of(ResourceLocation.fromNamespaceAndPath(MOD_ID, "grave_sovereign"),
                Vessel.Kind.MONARCH, "Kaelith", "Sovereign of Graves",
                "Authority over what is already buried."));
```

`Kind.MONARCH` or `Kind.RULER`. The difference is not cosmetic: Monarchs can
declare their own colour and backdrop, Rulers deliberately cannot, because
presenting alike is what separates the two columns on the selection screen.

### Theming a Monarch

```json
// data/myaddon/sololeveling/vessels/kaelith.json
{
  "vessel": "myaddon:grave_sovereign",
  "color": "#8FE3B0",
  "backdrop": "frost"
}
```

`backdrop` is one of `shadow`, `frost`, `white_flame`, `beast`, `destruction`,
`system`. You pick one rather than shipping your own because the animations are
GLSL inside the mod's core shader, and a core shader cannot be extended from
outside. **The one you pick is tinted toward your colour**, so two Monarchs that
both chose `frost` still read as different.

Naming a Ruler here is reported in the log and skipped, not silently ignored.

---

## 10. Toggles and Spiritualization

Set `mode` to `toggle` and give it an upkeep:

```json
{
  "name": "Grave Spiritualization",
  "summary": "The graves answer, and keep answering.",
  "detail": "14 mana per second | left click becomes Grave Claw",
  "accent": "dark_green",
  "cost": "MEDIUM",
  "cooldown_ticks": 40,
  "mode": "toggle",
  "upkeep_per_second": 14,
  "executor": "com.example.myaddon.GraveSpiritualization"
}
```

A toggle is held as a **vessel form**, so while it runs the hunter counts as
spiritualized, can carry an aura, and can claim the attack button — with nothing
extra to register. The mod drains the upkeep and ends the form the moment a
second cannot be paid for.

**On a toggle, `cooldown_ticks` means "how long until it can be turned back
on".** Turning a form *off* is never gated — it spends no mana, runs no
executor, and produces no effect, so the cooldown that arming it set does not
apply to ending it. A player can always drop out of a form they are paying for.

### Manifesting armour and a weapon

```java
public final class GraveSpiritualization implements AbilityExecutor {
    @Override
    public void execute(AbilityContext context) {
        ServerPlayer player = context.player();
        String form = context.ability().formId();

        SpiritualizationGear.equipArmor(player, form,
                new ItemStack(MyAddon.CROWN.get()),
                new ItemStack(MyAddon.MANTLE.get()),
                ItemStack.EMPTY, ItemStack.EMPTY);
        SpiritualizationGear.equipWeapon(player, form,
                new ItemStack(MyAddon.BLADE.get()), ItemStack.EMPTY);
    }

    @Override
    public void deactivate(AbilityContext context) {
        SpiritualizationGear.restore(context.player(), context.ability().formId());
    }
}
```

Pass an empty stack for a slot your form leaves alone — the player's own piece
stays on and is not recorded, so a partial set works.

**Equipping is the easy half.** The reason to use this rather than setting slots
yourself is everything after: death, logout, a dropped stack, an inventory
shuffle, and a server that stopped mid-form. Each of those either duplicates your
temporary item or eats the player's real gear, and all of them are already
handled.

`restore` is safe to call when the form equipped nothing, so your deactivate path
does not have to remember which of the two it used.

---

## 11. Running and debugging

```bash
./gradlew.bat runClient
```

Your addon and SoloCraft both load, because of `localRuntime`.

### The loop for JSON changes

JSON under `data/` is a data pack. You do **not** need to restart:

```
/reload
```

Watch the log. SoloCraft reports every file it read and every one it refused:

```
[SoloCraft] Loaded 3 contributed ability definition(s)
[SoloCraft] Skipping ability myaddon:ember_bolt: executor
            com.example.myaddon.EmberBolt not found
```

A file that produces **no line at all** was never seen — that is a path problem,
not a content problem. Re-read [section 4](#4-the-file-layout).

### Breakpoints

Run `runClient` in debug mode from the IDE and breakpoints in your executor work
normally. Executors run on the server thread; in single-player that is the
integrated server, in the same JVM.

### Test on a dedicated server too

```bash
./gradlew.bat runServer
```

This is worth doing at least once. Data packs load server-side only, so anything
the client draws — icons, class descriptions, Monarch colours — has to cross the
network. SoloCraft syncs all of it for you, but **single-player hides mistakes**
in that area because the integrated server shares memory with the client. A bug
here looks like "works on my machine, blank for everyone else".

---

## 12. Shipping it

```bash
./gradlew.bat build
```

Your jar lands in `build/libs/`. Check it does **not** contain SoloCraft:

```bash
unzip -l build/libs/myaddon-0.1.0.jar | grep -c "net/solocraft/"
```

That should print `0`. Anything else means you used `implementation` instead of
`compileOnly`, and your addon is shipping a second copy of SoloCraft.

Players install your jar plus SoloCraft plus GeckoLib. Your
`neoforge.mods.toml` dependency block makes the loader enforce that.

---

## 13. Troubleshooting

**The SoloCraft API will not resolve in the IDE.**
The jar is missing from `libs/`, or `sololeveling_jar` in `gradle.properties`
does not match its filename character for character. Fix it and re-sync Gradle.

**The dev client crashes on startup with a missing GeckoLib.**
SoloCraft requires it. Check the `localRuntime` GeckoLib line is in your
`dependencies` block.

**My ability JSON does nothing and there is no log line.**
The path is wrong. It is `data/<yourmod>/sololeveling/abilities/`, not
`data/sololeveling/...`. Your mod id comes first.

**"executor ... not found".**
`executor` must be the fully-qualified class name, package included, and the
class needs a public no-argument constructor.

**The runestone teaches nothing.**
`HunterRunestoneItem` takes the ability **name** exactly as written in the JSON
`name` field — not the file name, not the id. It is case- and space-sensitive.

**My class never comes up in the Evaluator.**
It needs a presentation file under `classes/`. Registering it in code alone makes
it exist, not offerable.

**The ability slot shows an empty frame.**
Either no `icon` field, or the texture is missing at the declared path. The file
lives at `assets/<yourmod>/textures/ability/<name>.png` and the field spells it
`myaddon:textures/ability/<name>.png`.

**Everything works in single-player and breaks on a server.**
Something the client draws is not reaching it, or client-only code is running on
the server. Run `runServer` and read the log.

**My Monarch has no colour.**
Presentations are Monarch-only. Check the log — a file naming a Ruler is
explicitly reported and skipped.

**I updated SoloCraft and everything broke.**
Change `sololeveling_jar` in `gradle.properties` to the new filename, drop the
new jar in `libs/`, delete the old one, and re-sync. A stale jar left on the
compile classpath silently shadows the new API.
