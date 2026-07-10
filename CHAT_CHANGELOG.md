# SoloCraft Changelog

## Unreleased

### Shader Visuals And Combat Effects
- Added shader-backed slash visuals using renderer classes, quads, `.vsh`, `.fsh`, and shader `.json` files.
- Added shader visuals for melee slash effects.
- Added shader visuals for Cross Strike.
- Added shader visuals for Slash Fury style effects.
- Added shader visuals for Dual Wielding flurry effects.
- Added Quickslashes visuals that spawn slash effects on each entity hit by the ability.
- Changed Quickslashes so slashes do not all appear at the same time.
- Improved Quickslashes with sharper slash visuals and small staggered delays.
- Tested shader portal ideas and reverted the gate shader experiment after visual/render-order issues.
- Restored gates back to the original model and texture setup after removing the experimental gate shader layer.
- Documented the shader rendering/API idea for a standalone library.
- Updated the shader API plan to include NeoForge 1.21.1 support.

### Dungeon Structures And Commands
- Added `/slr @p structure <dungeon name>` style support for spawning procedural room options next to each other.
- Added command auto-fill suggestions for command input segments.
- Replaced the Kasaka instance dungeon first room with the updated entrance/first-room structure.
- Reverted the overworld instance dungeon entrance after it was accidentally replaced.
- Updated Kasaka dungeon generation so only the first room uses the new structure while other rooms stay untouched.
- Removed chests from procedural dungeon chest rooms so those rooms can be decorated later with crystals.
- Changed procedural dungeon start rooms to be fully closed with no entrance opening toward the spawn room.
- Made the room after the spawn room empty.
- Changed procedural dungeon shell protection from obsidian to bedrock.
- Fixed ice dungeon ceiling gaps by making the ceiling full blocks and placing lanterns one block below.
- Added procedural dungeon creative-tab support with a spawn egg reusing the Goblin Sewers gate spawn egg texture.
- Fixed procedural dungeon spawning so it does not always generate the same C-rank moss dungeon.
- Reduced Red Gate chance for procedural dungeons so red gates are rare instead of constant.
- Matched procedural red gate behavior closer to the existing Red Gate behavior, including delayed teleport, texture change, party entry, locking, and boss-clear return rules.
- Added procedural dungeon integration into the normal dungeon/gate system.
- Added procedural dungeon integration with the Magic Reader read system.
- Fixed A-rank procedural dungeons so boss-grade Gem Golems/Futuristic Golems do not spawn as normal room mobs.
- Added gate expiry logic where gates that outrank every player can silently despawn instead of breaking.
- Added saved per-gate break memory so when a gate type breaks, that same gate type has a lower break chance next time.
- Changed the first natural gate in a world to always be Goblin Sewers.
- Reworked Job Change access so reaching the threshold unlocks it in System > Quests instead of requiring a key-spawned portal, and locked the Igris job dungeon until the quest fully completes.

### Dungeon Progression And Boss Rewards
- Added a central dungeon content roadmap plan for E-S, Red Gates, DKC, and Monarch/National tiers.
- Added procedural dungeon generation goals around start rooms, boss rooms, themed rooms, decoration, monster spacing, and boss spacing.
- Added the first Urgent Quest system pass with a chance to start contextual dungeon quests after entering gates.
- Added Urgent Quest objectives for timed dungeon clears, no-skill clears, and dungeon monster kill challenges.
- Added dungeon-themed Urgent Quest pools for Goblin Sewers, Cemetery, Lab, Ant/S-rank gates, Red Gates, and procedural/generic dungeons.
- Added Urgent Quest tracking for player and owned-shadow kills.
- Added Urgent Quest success/failure/progress popups through the System notification system.
- Made no-skill Urgent Quests fail on actual skill-key usage while allowing basic X-button slash combat.
- Fixed abandoned lab return behavior so only Red Gates should block returning before boss kill.
- Fixed dungeon completion so clearing the boss properly closes the dungeon/gate state.
- Fixed Ancient Golem kill incorrectly awarding the Giant Gem Golem achievement.
- Removed Demon King's Castle key drop from Igris.
- Fixed shadow kills not spawning shadow souls.
- Fixed shadow kills not counting as boss kills for dungeon completion/rewards.
- Fixed DKC spawning multiple Kaiselins at once.
- Fixed no-gravity state persisting after dungeon-loading crashes.
- Added a dungeon void safety guard that rescues players back onto nearby safe dungeon ground if they fall below normal dungeon or Demon King's Castle floors.
- Added Kaiselin spawn to Demon King's Castle floor 20 alongside Baran.
- Prevented Baran and Kaiselin from fighting each other.
- Made Kaiselin spawn a reviveable shadow soul like other reviveable bosses.
- Made Kaiselin shadow soul spawn at ground level if Kaiselin dies in the air.
- Removed extra Baruka dagger drops so Baruka rewards come from the system reward path only.
- Fixed Cerberus spawning in DKC with missing health.

### Daily Quest And Secret Quest
- Added a secret Daily Quest upgrade after the player reaches level 30.
- Changed secret Daily Quest behavior so requirements remain visually shown as the original value, such as `50/25`.
- Added secret Daily Quest reward path for DKC key, 20 Skill Points, and Full Recovery.
- Changed future quest failures so they do not send the player to the punishment zone.
- Changed quest failure display from center-screen text to popup-style notification.
- Added red/dark-red styled notification when Daily Quest turns into Secret Quest.
- Added Iris/Oculus shader-pack fallback for in-world System popups so notifications use non-shader backgrounds when shader packs are active.
- Added sneak + Arise cinematic System scan that reports whether Shadow Extraction is possible without reviving shadows.

### System GUI, Abilities GUI, And Popups
- Analyzed the new J-key System GUI and sub-GUIs.
- Fixed issues in the new System GUI before promoting it.
- Swapped the renewed System GUI to the main N key path and removed the J-key alternate path.
- Darkened the System GUI and linked System-styled GUI backgrounds for better contrast.
- Removed the main System GUI `progression/current XP` text next to SP because the tooltip already shows it.
- Shortened the base System GUI from the bottom and reduced spacing near the SP line and bottom button separator.
- Reworked Abilities GUI and Skill List GUI while preserving existing features.
- Added skill slot page support and returning to the slot screen after picking a skill.
- Added System Crafting access back to the main System GUI action grid.
- Moved System Settings to a compact title-bar button to keep the main System GUI layout balanced.
- Reworked the System Crafting GUI with System-themed visuals, circular sigil slot placement, triangle inputs, centered output, and corrected recipe input tags.
- Improved System Crafting GUI spacing with a larger panel, separated ritual/craft/inventory zones, and better-aligned slots.
- Fixed corrupted formatting characters appearing in System GUI and System Crafting tooltips.
- Added reduced-volume Solo Leveling style system opening and closing sounds to System GUI screens.
- Added system popup sound effects using the same Solo Leveling style sound.
- Added a separate negative System popup sound for failure/invalid-action notifications.
- Moved the old welcome overlay into the new popup system.
- Added popup support for level-up notifications.
- Added popup support for daily quest notifications.
- Added popup support for skill unlock notifications.
- Added popup support for job-change Shadow Monarch riddle messages.
- Removed job-change riddle text from the message log after moving it to popup notifications.
- Added DKC floor-entry popups with kill requirements and floor notifications.
- Moved `rewards to collect` style messages into the popup system.
- Fixed popup spam where `New Skill` showed with `.` as the bottom text every few seconds.
- Remade `Boss Slain Dungeon Cleared` into a popup notification.
- Added random class awakening popup when a player passes level 20 without choosing a class.
- Added small-text newline support for the world popup notification system.
- Adjusted the system popup position further left.
- Adjusted the popup panel angle and 3D presentation.
- Fixed notification scaling so larger settings do not spawn new panels inside old panels.

### World Notification Command
- Added and iterated on `/slnotify` world-space notification panels.
- Fixed `/slnotify` panel text being mirrored/right-to-left.
- Fixed `/slnotify` position being offset as if the player always faced south.
- Changed `/slnotify` to stay in a consistent relative screen position while existing in the world.
- Changed `/slnotify` so panels are visible only to the target player.
- Fixed yaw rotation direction and player-facing rotation.
- Adjusted `/slnotify` placement slightly left with a stronger 3D angle.

### Damage Numbers And Titles
- Added client-toggleable floating damage numbers.
- Added damage numbers for player-dealt damage.
- Added damage numbers for owned shadow/tamed entity damage.
- Added red incoming damage numbers when the player takes damage.
- Added damage number drift, fade, random side offset, and camera-facing rendering.
- Added `Damage Numbers` toggle to System Settings.
- Removed visible Combat Mode row from System Settings while keeping the keybind/variable behavior.
- Added a Titles GUI opened from the existing `Title:` text on the System panel.
- Added `None` and `Wolf Assassin` title entries.
- Added title unlock tracking and selected-title storage.
- Added Wolf Assassin unlock progress from wolf-family kills.
- Added Wolf Assassin unlock popup.
- Added Wolf Assassin tooltip text for locked/unlocked states.
- Added Wolf Assassin effects: bonus damage against wolves/lycans and movement speed while equipped.

### Shadow Monarch, Shadows, And Kaisel
- Added Kaiselin as a standalone Geckolib boss.
- Added Shadow Kaisel as the shadow version using the shadow texture and shared model/animation.
- Added Monarch's Domain runestone using shadow skill runestone styling.
- Added Monarch's Domain runestone to the creative tab next to other shadow ability runestones.
- Changed Monarch's Domain unlock condition to clearing level 10 in DKC.
- Added Shadow Command as a base Shadow Monarch ability.
- Added Shadow Command GUI for commanding summoned shadows.
- Restyled Shadow Command GUI to match the Shadow Summon shader, frame, button, animation, and sound style.
- Added Shadow Command icon texture.
- Added command modes like protect behavior and dungeon-focused commands.
- Added Clear Dungeon shadow command mode.
- Improved Clear Dungeon pathfinding so shadows do not blindly try to walk through walls.
- Added `/slr @p shadows add/remove <shadow>` command support.
- Added Kaisel summon button to the shadow summoning GUI.
- Matched Kaisel and Tusk summoning buttons to the same style as Igris and Beru.
- Added Shift-click summon-all support to Shadow Summon buttons for each soldier type.
- Restyled all three Shadow Exchange GUIs to match the Shadow shader/frame/button/animation style.
- Reworked Shadow Exchange anchors onto a structured backend with legacy save migration, safe teleport validation, cooldown, quiet sounds, and negative failure popups.
- Added a Shadow Dismiss GUI opened from Shadow Summon that lists only normal unit shadows and excludes boss shadows.
- Changed Shadow Dismiss so it removes the lowest-level shadows first, using lowest XP as the tie-breaker.
- Removed `Formation mode` text under the formation button.
- Added Kaisel into the shadow ownership variable set as `Kaisel`.
- Made Shadow Kaisel summonable/desummonable through the shadow system.
- Added summoning effects for Kaisel, including lightning/particles like other shadows.
- Prevented despawn-all from despawning Kaisel while the player is riding it.
- Removed Shadow Kaisel boss bar.
- Deleted the Shadow Kaisel spawn egg.
- Added Shadow Kaisel Monarch's Domain boosted texture variant.
- Fixed Beru attacking Kaisel by aligning Kaisel with shadow tags/properties.
- Fixed Monarch's Domain not boosting Kaisel.
- Made Shadow Kaisel rideable.
- Improved Shadow Kaisel flight controls.
- Fixed Kaisel A/D controls being reversed.
- Reduced Kaiselin boss health and shield.
- Increased Kaiselin visual size and hitbox size at the time.
- Later reduced Shadow Kaisel hitbox size for better gameplay feel.
- Added Shadow Kaisel health persistence between despawn/resummon.
- Added off-screen healing for boss shadows at 1 health per second while desummoned.
- Fixed duplicate same-shadow summons after entering gates.
- Fixed Shadow Kaisel taming/owner setup on spawn.
- Fixed Shadow Kaisel animation pausing.
- Changed Kaisel flying idle to reuse the moving flying animation.
- Continued fixes for Kaisel animation bugs.
- Added shadow inventories for summoned shadows.
- Made shadows collect only the five mana stone grades from enemies they kill.
- Made shadows drop inventory contents when despawned.
- Confirmed Kaisel can be added to formations.
- Improved Shadow Tusk behavior with more spell casting and occasional master buffs.

### Hunters, Classes, Skills, And Combat Balance
- Improved Hunter NPC behavior by class.
- Changed tankers to block more instead of dodging too much.
- Changed fighters to mix blocking and dodging more evenly.
- Changed assassins to dodge more often.
- Added class/rank-aware skill usage for Hunter NPCs.
- Reduced excessive movement behavior for ranger, healer, and mage hunters.
- Nerfed S-rank hunters because they were too oppressive.
- Fixed missing class crashes for hunter/ranger-related logic.
- Fixed Ranger three-charge Back Step not recharging.
- Fixed Triple Jump playing sound/particles without movement.
- Restored Triple Jump behavior so it does not wreck current speed.
- Boosted Mana Bow damage scaling to be closer to Mana Gun scaling.
- Made bosses other than Goblin King see through Stealth.
- Fixed boss stealth targeting so bosses can reacquire invisible survival/adventure players.

### Bosses, Monsters, Hitboxes, And Animation Fixes
- Fixed Steel Fanged Lycan hitbox.
- Replaced old Steel Fang Wolf saved entities inside structure NBTs with Steel Fanged Lycan.
- Fixed Steel Fanged Lycan texture fallback from legacy texture ids.
- Fixed dungeon-spawned Lycans having very low health.
- Fixed custom skeleton death behavior so they do not stand back up and attack during death animation.
- Fixed cemetery skeleton death/despawn behavior.
- Fixed Shadow Beru hitbox being too tall/inconsistent.
- Improved Kaiselin boss behavior so it is less lame and does not attack creative-mode players.
- Fixed Kaiselin rotation problems.
- Lowered Kaiselin boss power from its original overtuned state.
- Changed normal Kamish to use the same model and animation resources as Shadow Kamish.
- Fixed Statue Axe, Statue Hammer, and Statue Sword getting stuck in walking animation after being hit while not actually moving.
- Fixed Statue of God walking animation control so its scripted chase uses movement-based animation instead of a stuck looping procedure animation.
- Cleaned up Statue of God state syncing for throne/aggressive behavior.
- Reduced death despawn timers by 5 ticks for Skeleton Brute, Skeleton Warrior, Skeleton Summoner, Green Orc, and High Orc.
- Further reduced Skeleton Brute and Skeleton Warrior death despawn timers by another 10 ticks.
- Added a custom ominous Arise sound effect and delayed the Arise resurrection moment slightly after pressing the skill.

### Demon King's Castle
- Added DKC floor tracking and floor-entry notification flow.
- Added DKC floor kill requirements.
- Added DKC boss reward fixes.
- Added DKC debug/support commands.
- Added DKC travel/support items and supporting dungeon dimension pieces.
- Added DKC floor/boss structures and room pieces.
- Added Baran, Vulcan, Cerberus, Demon, and Demon Knight entity support.
- Added DKC boss and demon models/textures/animations.
- Fixed DKC key/reward routing issues.

### Gates And Red Gates
- Fixed Red Gate return restrictions so only Red Gates block exiting before boss kill.
- Fixed normal dungeon gates incorrectly locking players out from returning.
- Changed procedural red gate chance to be very low.
- Added gate locking behavior so players cannot enter after a gate turns red.
- Added group/party entrance handling for red gate behavior.
- Added silent despawn chance for expired gates that outrank every player.
- Added per-gate-type break chance reduction after successful dungeon breaks.

### Loot, Rewards, Titles, And Progression
- Removed unwanted extra Baruka dagger drops.
- Removed Igris dropping the Demon King's Castle key.
- Fixed shadow kill credit for bosses.
- Fixed shadow soul spawn when shadows kill valid reviveable entities.
- Added Wolf Assassin title unlock and combat effects.
- Added title persistence and legacy title-save support.
- Fixed shadow soldier level reset issues.
- Added boss-shadow health persistence and desummoned regeneration.

### Stability And Build
- Fixed multiple `NoClassDefFoundError` crash paths caused by missing runtime classes.
- Checked and fixed crash paths from tooltip rendering, Igris, random hunter ranger tick, and missing no-fall logic.
- Added compile checks after stability-sensitive changes.
- Set build output jar naming toward `SLR1.0.0`.
- Created a zip backup of the workspace excluding temporary/run folders.

### Assets, Textures, And Cleanup
- Removed many unused GUI textures.
- Added Kaiselin normal texture.
- Added Shadow Kaisel texture.
- Added Shadow Kaisel Monarch's Domain boosted texture.
- Added Shadow Command icon texture.
- Added Guild Computer block to the Solo Leveling blocks creative tab.
- Added new DKC/boss/entity textures and models.
- Added new shader files for slash and system visual effects.

### Documentation And Planning
- Added dungeon content roadmap planning for low-rank, mid-rank, endgame, DKC, and Monarch/National content.
- Added procedural dungeon generation planning.
- Added gameplay improvement suggestions and future-content recommendations.
- Updated shader rendering system documentation with SLR shader references and API design goals.
- Added this changelog.
