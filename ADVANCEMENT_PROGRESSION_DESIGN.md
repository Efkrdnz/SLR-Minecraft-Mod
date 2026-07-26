# Advancement and Vessel Progression

## Visibility contract

SoloCraft uses two kinds of advancements:

- **Hunter history** is public. Entering ranked gates and defeating notable public bosses may announce in chat.
- **System history** is private. Becoming a Player, System levels, Job Change, vessel identity, vessel abilities, Demon King's Castle progress, and Radiru outcomes use local toasts only and never announce globally.

The existing `sololeveling:awakened` tab remains the public hunter-history root. The private System tab begins at `sololeveling:system/root`.

## System tree

```text
The System
├─ Level 10 ─ Level 30 ─ Level 50 ─ Level 100
├─ Job Change Available
│  └─ Become a Vessel
│     ├─ Shadow Monarch
│     ├─ Frost Monarch
│     ├─ Monarch of White Flames
│     ├─ Monarch of Fangs
│     └─ Ruler identities
└─ Demon King's Castle
   ├─ First Floor
   ├─ Floor 10
   ├─ House Radiru
   │  ├─ Pact
   │  └─ Bloodshed
   └─ Castle Conquered
```

All criteria are awarded by `VesselProgressionManager`; the JSON uses `minecraft:impossible` so client-side or vanilla triggers cannot forge progression.

## Skill progression

### Shadow Monarch

- Start: Arise, Shadow Summon, Dismiss Shadows, Shadow Command, Shadow Storage I.
- Levels 70/90/100/120: Shadow Storage II-V.
- 30 stored shadows: Shadow Exchange.
- Final: Spiritual Body Manifestation requires level 120, 60 stored shadows, Shadow Exchange, and DKC floor 10.

### Frost Monarch

- Start: Ice Spear.
- 55: Flash Freeze.
- 65: Frozen Path.
- 75: Frozen Architecture.
- 85: Frost Counter.
- 100: Absolute Zero.
- 120: Frost Monarch Spiritualization.

### Monarch of White Flames

- Start: Lightning Breath.
- 60: Radiru Blood Spear.
- 70: Doppelganger.
- 85: Hellstorm Dominion.
- 100: Hell's Army.
- 120: White Flame Spiritualization.

### Monarch of Fangs

- Start: Claw-Rift Passage.
- 60: Rubble Jaw.
- 75: King's Maul.
- 90: Feral Reconstitution.
- 120: White Fang Sovereign.

### Ruler vessels

Ruler's Authority and each implemented Ruler spiritual body are innate at vessel selection.

- Christopher Reed: Fire Charge at start, Meteor Rain at 65, Fireflies at 85.
- Thomas Andre: Goliath manifestation at start, Capture at 55, Power Smash at 70, Collapse at 90.
- Liu Zhigang: Dragon Sword manifestation at start, Heavenly Counter at 55, Golden Dragon Dance at 75, Sovereign Sword Domain at 95.
- Sung Il-Hwan and Go Gunhee retain innate Ruler's Authority until their unique combat kits are implemented.

Ashborn follows the Shadow Monarch progression rather than the ordinary Ruler rule: Shadow Spiritual Body Manifestation remains his final unlock.
