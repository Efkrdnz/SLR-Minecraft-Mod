package net.solocraft.util;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;

import net.solocraft.api.skill.HunterAbilityRegistry;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Skill-list descriptions for every ordinary Hunter ability.
 *
 * <p>Mage, Ranger and the vessel kits build their tooltips inside their own
 * managers because those lines change with output stage or manifestation. Every
 * other ability previously fell through to a placeholder that printed its name
 * twice; this registry fills that gap.</p>
 *
 * <p>Entries are keyed by exact token. Nothing here reads the player's class —
 * a rune-taught ability describes itself the same way for anyone.</p>
 */
public final class SkillDescriptionRegistry {
	/** One tooltip entry: what it does, and the rule worth knowing. */
	private record Entry(String summary, String detail, ChatFormatting accent) {
	}

	private static final Map<String, Entry> ENTRIES = new LinkedHashMap<>();

	static {
		// ── Assassin ──────────────────────────────────────────────────────────
		put("Ghost Step", "Blink a short distance, passing through enemies.",
				"Two charges | crossing a target Exposes it and builds Tempo",
				ChatFormatting.AQUA);
		put("Night Rend", "Appear behind a target and open with a dagger cut.",
				"Applies Exposed | Infiltrator holds it twice as long",
				ChatFormatting.AQUA);
		put("Stealth", "Leave a decoy and slip into concealment.",
				"Ordinary mobs chase the decoy | bosses only lose certainty",
				ChatFormatting.AQUA);
		put("Flash Cut", "A forward fan of cuts sharing one damage budget.",
				"Up to 5 targets | full Tempo empowers the whole fan",
				ChatFormatting.AQUA);
		put("Dualwield", "Enter a two-dagger stance with follow-up strikes.",
				"Every fourth hit crosses into nearby enemies",
				ChatFormatting.AQUA);
		put("Critical Attack", "Open a brief counter window.",
				"A successful counter evades, Exposes, and readies Riposte",
				ChatFormatting.AQUA);
		put("Mutilation", "Mark one target, then detonate the cuts you land on it.",
				"Detonation scales with the number of distinct cuts recorded",
				ChatFormatting.AQUA);
		put("Murderious Intent", "Project killing intent to unnerve nearby enemies.",
				"Pressure effect | does not scale with weapon damage",
				ChatFormatting.AQUA);
		put("Shadow Feint", "Leave an attackable afterimage in your place.",
				"Struck: relocate to a flank with a follow-up | recast: blind and slow",
				ChatFormatting.AQUA);
		put("Silent Domain", "Place a dim zone where enemies lose track of targets.",
				"Ordinary mobs drop their target | bosses are only delayed",
				ChatFormatting.AQUA);
		put("Zero Presence", "Enter a short apex state with three Vanish charges.",
				"A charge is spent only on a confirmed hit | grants no immunity",
				ChatFormatting.AQUA);
		put("Dagger Throw", "Hurl a dagger that pins its target's footing.",
				"Ruler's Authority foundation | returns at higher stages",
				ChatFormatting.AQUA);
		put("Dagger Rush", "Blitz forward behind a storm of thrown daggers.",
				"Evolution of Dagger Throw | requires Ruler's Authority",
				ChatFormatting.AQUA);
		put("Cold Blood", "Suppress pain and hesitation to keep swinging.",
				"Forbidden technique | stacks while you keep landing hits",
				ChatFormatting.DARK_RED);

		// ── Fighter: Swordsman ────────────────────────────────────────────────
		put("Slash Dash", "Dash forward with one swept cut along the path.",
				"Collision-safe | up to 4 targets share one budget",
				ChatFormatting.RED);
		put("Sword Beam", "Launch a slash-shaped wave of energy.",
				"Pierces up to 3 targets with falloff",
				ChatFormatting.RED);
		put("Slash Fury", "A directional sequence of ordered slashes.",
				"5 slashes sharing one budget | you are slowed, not frozen",
				ChatFormatting.RED);
		put("Radiant Execution", "Draw, then deliver one precision cut along a line.",
				"Up to 2 targets | bonus damage below 30% health | spends Sword Tempo",
				ChatFormatting.RED);
		put("Sword Dance", "Mark a target and flank-step on each confirmed hit.",
				"Up to 4 steps | ends early if no safe position exists",
				ChatFormatting.RED);
		put("Sword of Light", "A luminous stance that plants Light Seals on targets.",
				"Heavy input launches sealed enemies or spends seals on a beam",
				ChatFormatting.RED);
		put("Critical Strike", "A precise strike aimed at an opening.",
				"Universal runestone skill | shares the parry infrastructure",
				ChatFormatting.RED);

		// ── Fighter: Striker ──────────────────────────────────────────────────
		put("Ground Slam", "Drive a fist into the ground for a radial impact.",
				"5 block radius, 8 targets | damage falls off toward the rim",
				ChatFormatting.GOLD);
		put("Cross Strike", "Two merging arcs with a short forward lunge.",
				"The second hit carries the guard damage that builds Impact",
				ChatFormatting.GOLD);
		put("Iron Knuckle", "Brace and charge one armored punch, then release.",
				"Three charge steps | releasing later hits far harder",
				ChatFormatting.GOLD);
		put("Breaker Combo", "An input-timed three-part combo.",
				"Look down on the third input to spike instead of knocking away",
				ChatFormatting.GOLD);
		put("Meteor Fist", "Leap to a point and land a direct impact plus shockwave.",
				"Fails for free if no safe landing exists",
				ChatFormatting.GOLD);
		put("Titan's Barrage", "Lock one target in a sequence of heavy strikes.",
				"12 strikes on a rising curve, then a launching finisher",
				ChatFormatting.GOLD);

		// ── Fighter: Ravager ──────────────────────────────────────────────────
		put("Magical Eye", "Reveal wounded enemies and their weak points.",
				"Only targets below 70% health | owner-visible outlines",
				ChatFormatting.YELLOW);
		put("Claw Strikes", "A three-swipe claw chain that applies Rend.",
				"Look down on the last swipe to focus it on one target",
				ChatFormatting.YELLOW);
		put("Beast Sense", "Heighten speed, footing and awareness briefly.",
				"Improves Feral gain while active",
				ChatFormatting.YELLOW);
		put("Partial Transformation", "Enter a maintained hybrid beast form.",
				"Costs 40 Feral | Feral above 60 makes it cheaper to hold",
				ChatFormatting.YELLOW);
		put("Predator Rush", "Bound to a wounded target and maul it.",
				"Bonus damage at 3 Rend | fails free without a safe approach",
				ChatFormatting.YELLOW);
		put("Full Beast Transformation", "Commit Feral to a time-limited beast form.",
				"Costs 80 Feral | cannot overlap a vessel transformation",
				ChatFormatting.YELLOW);

		// ── Tanker: Sentinel ──────────────────────────────────────────────────
		put("Shield Bash", "Advance behind your shield and strike one target.",
				"Interacts with Iron Wall stacks and Willpower Strain",
				ChatFormatting.WHITE);
		put("Taunt", "Claim the attention of nearby enemies.",
				"Mobs focus you | players receive a Challenge instead",
				ChatFormatting.WHITE);
		put("Tank Leap", "Leap to a chosen point and gather enemies on landing",
				"Establishes position and slows what it gathers",
				ChatFormatting.WHITE);
		put("Reinforcement", "Brace against a telegraphed hit.",
				"A successful brace enters a short reinforced stance",
				ChatFormatting.WHITE);
		put("Willpower", "Delay incoming damage as Strain, then settle it.",
				"Strain cannot be erased by logging out or changing class",
				ChatFormatting.WHITE);
		put("Protection Mark", "Deploy a zone that spends integrity to protect allies.",
				"Capped beneficiaries | finite integrity",
				ChatFormatting.WHITE);

		// ── Tanker: Juggernaut ────────────────────────────────────────────────
		put("Heavy Blow", "A slow armored swing with heavy knockback.",
				"Modest damage, strong displacement and guard pressure",
				ChatFormatting.GOLD);
		put("Iron Body", "Brace into a short stagger-resistant stance.",
				"A hit taken in the first moment converts into extra Poise",
				ChatFormatting.GOLD);
		put("Seismic Grapple", "Pin a normal target, or drag on a boss.",
				"Bosses and players are slowed, never locked in place",
				ChatFormatting.GOLD);
		put("Gigantification", "Spend Poise to enter a heavy maintained form.",
				"Costs 35 Poise | Poise above 50 makes it cheaper to hold",
				ChatFormatting.GOLD);
		put("Colossus Charge", "An armored rush that gathers enemies along the front.",
				"Releases everything gathered when the charge ends",
				ChatFormatting.GOLD);
		put("Mountain Breaker", "A two-stage impact: outward lift, then inward collapse.",
				"Poise pays part of the cost, never all of it",
				ChatFormatting.GOLD);

		// ── Healer: Saint ─────────────────────────────────────────────────────
		put("Heal Beam", "Channel restoration into one ally.",
				"Sneak to self-cast at reduced efficiency",
				ChatFormatting.GREEN);
		put("Haste Buff", "Grant an ally movement and action speed.",
				"Sneak to self-cast | widens to two allies at higher Intelligence",
				ChatFormatting.GREEN);
		put("Purification", "Remove harmful effects from an ally.",
				"Sneak to self-cast | free if there is nothing to remove",
				ChatFormatting.GREEN);
		put("Physical Buff", "Raise an ally's physical output and durability.",
				"Sneak to self-cast | widens to two allies at higher Intelligence",
				ChatFormatting.GREEN);
		put("Overheal", "Convert banked excess healing into absorption.",
				"Sneak to self-cast | feeds on healing you actually wasted",
				ChatFormatting.GREEN);
		put("Blessing Mark", "Mark an ally; banked healing releases if they fall low.",
				"Sneak to mark yourself | auto-triggers below 30% health",
				ChatFormatting.GREEN);

		// ── Healer: Shepherd ──────────────────────────────────────────────────
		put("Healing Pulse", "A pulse healing you and the most wounded allies nearby.",
				"No target needed | pulses twice at the highest Intelligence stage",
				ChatFormatting.GREEN);
		put("Camouflage", "Drop threat and turn translucent to reposition.",
				"Nearby enemies lose you as a target",
				ChatFormatting.GREEN);
		put("Purifying Wave", "A wave that cleanses and lightly heals allies.",
				"No target needed | heals and cleanses you as well",
				ChatFormatting.GREEN);
		put("Guardian Step", "Dash to an ally and shield you both.",
				"Fails for free when no safe landing exists",
				ChatFormatting.GREEN);
		put("Sanctuary", "Place a bounded field that heals on its own timer.",
				"Adds stagger resistance at higher Intelligence",
				ChatFormatting.GREEN);
		put("Second Wind", "Emergency party recovery weighted by missing health.",
				"Cleanses one control effect and grants recovery protection",
				ChatFormatting.GREEN);

		// ── Healer: universal blessings ───────────────────────────────────────
		put("Guardian Ward", "Layer damage resistance onto your allies.",
				"Sneak to self-cast | adds fire resistance, then absorption",
				ChatFormatting.GREEN);
		put("Mana Font", "Restore an ally's mana pool.",
				"Sneak to self-cast | widens to two allies at higher Intelligence",
				ChatFormatting.GREEN);
		put("Vitality Surge", "Raise an ally's maximum health and heal into it.",
				"Sneak to self-cast | adds regeneration at higher Intelligence",
				ChatFormatting.GREEN);
		put("Divine Favor", "A capstone blessing granting several boons at once.",
				"Sneak to self-cast | ally count rises with Intelligence",
				ChatFormatting.GREEN);

		// ── Miscellaneous ─────────────────────────────────────────────────────
		put("Detection", "Sweep the area for hidden presences.",
				"Reveals nearby entities for a short time",
				ChatFormatting.GREEN);
		put("Proximity Trap", "Place a trap that triggers on approach.",
				"Legacy Ranger tool",
				ChatFormatting.GREEN);

		// Vessel and job abilities.
		//
		// These had no description text anywhere in the mod, so JobSkillManager
		// fell through to a placeholder that printed the skill name twice and
		// nothing else. Keyed by exact name like every other entry, so that
		// route picks them up with no new wiring. Grand Marshal and Sung
		// Il-Hwan entries stay behind their existing developer gates.
		put("Arise", "Extract the shadows of the fallen and raise them as your own.",
				"Raises every eligible corpse in range at once | costs mana per shadow and one storage slot each",
				ChatFormatting.DARK_PURPLE);
		put("Shadow Summon", "Call your stored shadows out to fight beside you.",
				"Summons from your roster up to your summon limit",
				ChatFormatting.DARK_PURPLE);
		put("Dismiss Shadows", "Return every summoned shadow to storage.",
				"Frees them for re-summoning | the shadows themselves are not spent",
				ChatFormatting.DARK_PURPLE);
		put("Shadow Command", "Open the shadow roster and issue standing orders.",
				"Rename, inspect, promote and dismiss individual shadows",
				ChatFormatting.DARK_PURPLE);
		put("Shadow Exchange", "Trade places instantly with one of your shadows.",
				"Repositions you to the shadow and the shadow to you",
				ChatFormatting.DARK_PURPLE);
		put("Shadow Manifestation", "Wear your own shadow as armour.",
				"Raises your combat stats for the duration",
				ChatFormatting.DARK_PURPLE);
		put("Grand Marshal Authority", "Command authority granted to a Grand Marshal.",
				"Developer preview | normal players see this as work in progress",
				ChatFormatting.GOLD);
		put("Ice Spear", "Hurl a spear of solid ice at a target.",
				"Frost Monarch basic attack",
				ChatFormatting.AQUA);
		put("Flash Freeze", "Freeze everything around you where it stands.",
				"Roots caught targets | frost builds toward a hard freeze",
				ChatFormatting.AQUA);
		put("Frozen Path", "Lay a road of ice and ride it at speed.",
				"Grants footing and momentum across gaps",
				ChatFormatting.AQUA);
		put("Frozen Architecture", "Hold to open the ice wheel; release to raise the chosen structure.",
				"Walls, pillars, domes and bridges built from the blueprint you pick",
				ChatFormatting.AQUA);
		put("Frost Counter", "Answer the next blow with a retaliating burst of frost.",
				"Reactive | consumes the window when it triggers",
				ChatFormatting.AQUA);
		put("Absolute Zero", "Drop the temperature until the battlefield itself stops.",
				"Frost Monarch capstone | once per encounter",
				ChatFormatting.AQUA);
		put("Frost Monarch Spiritualization", "Take the Monarch of Frost's true form.",
				"Raises every frost ability while it holds",
				ChatFormatting.AQUA);
		put("Stillness Decree", "Command the air to hold still, arresting everything in it.",
				"Frost control | movement and projectiles both suffer",
				ChatFormatting.AQUA);
		put("Pale Causeway", "Freeze a walkway of pale ice ahead of you.",
				"Crossing terrain and denying ground at once",
				ChatFormatting.AQUA);
		put("Winter Remembers", "Winter recalls where a target stood and drags it back.",
				"Rewinds the victim's position | a target cannot be rewound twice in quick succession",
				ChatFormatting.AQUA);
		put("Whiteout Procession", "Advance a wall of whiteout that smothers what it passes.",
				"Slows projectiles caught inside it",
				ChatFormatting.AQUA);
		put("Ice Ball", "Throw a compacted ball of ice.",
				"Legacy frost attack",
				ChatFormatting.AQUA);
		put("Ice Chunk", "Tear loose a chunk of ice and throw it.",
				"Legacy frost attack",
				ChatFormatting.AQUA);
		put("Snow Screen", "Raise a blinding screen of driven snow.",
				"Legacy frost cover",
				ChatFormatting.AQUA);
		put("Lightning Breath", "Breathe a channelled line of white lightning.",
				"Sustained frontal damage",
				ChatFormatting.YELLOW);
		put("Hellstorm Dominion", "Call a storm of hellfire over the battlefield.",
				"Wide area denial",
				ChatFormatting.YELLOW);
		put("Radiru Blood Spear", "Condense blood into a thrown spear.",
				"High single-target damage",
				ChatFormatting.YELLOW);
		put("Doppelganger", "Split off a duplicate of yourself to fight alongside you.",
				"The copy mirrors your attacks",
				ChatFormatting.YELLOW);
		put("Hell's Army", "Summon the ranks of hell to the field.",
				"Multiple summons for the duration",
				ChatFormatting.YELLOW);
		put("White Flame Spiritualization", "Take the White Flame Monarch's true form.",
				"Raises every White Flame ability while it holds",
				ChatFormatting.YELLOW);
		put("Claw-Rift Passage", "Tear a rift with your claws and step through it.",
				"Closes distance and opens with a strike",
				ChatFormatting.RED);
		put("Rubble Jaw", "Erupt the ground into jaws of stone.",
				"Terrain-based area damage in front of you",
				ChatFormatting.RED);
		put("King's Maul", "Bring down a sovereign's blow.",
				"Heavy single impact",
				ChatFormatting.RED);
		put("Feral Reconstitution", "Knit your wounds closed by force of instinct.",
				"Self-recovery | Beast Monarch sustain",
				ChatFormatting.RED);
		put("White Fang Sovereign", "Assume the White Fang's sovereign aspect.",
				"Beast Monarch capstone",
				ChatFormatting.RED);
		put("Destruction Claw", "Rake the air with a claw of pure destruction.",
				"Antares basic attack",
				ChatFormatting.DARK_RED);
		put("Breath of Destruction", "Exhale ruin in a wide cone.",
				"Heavy frontal area damage",
				ChatFormatting.DARK_RED);
		put("Monarch's Descent", "Fall on the battlefield from above.",
				"Gap closer ending in an impact",
				ChatFormatting.DARK_RED);
		put("Sovereign Roar", "Roar with a sovereign's authority.",
				"Staggers and scatters everything nearby",
				ChatFormatting.DARK_RED);
		put("Extinction", "Erase what stands in front of you.",
				"Antares capstone",
				ChatFormatting.DARK_RED);
		put("Monarch Manifestation", "Take the Monarch of Destruction's true form.",
				"Raises every Antares ability while it holds",
				ChatFormatting.DARK_RED);
		put("Heavenly Counter", "Turn an incoming attack back on its owner.",
				"Reactive | consumes the window when it triggers",
				ChatFormatting.GOLD);
		put("Golden Dragon Dance", "Move through a chained sword form.",
				"Multi-hit melee sequence",
				ChatFormatting.GOLD);
		put("Sovereign Sword Domain", "Claim the ground around you as sword domain.",
				"Persistent area that punishes anything inside it",
				ChatFormatting.GOLD);
		put("Dragon Sword Manifestation", "Manifest the dragon sword in full.",
				"Raises every Liu Zhigang ability while it holds",
				ChatFormatting.GOLD);
		put("Predator's Presence", "Let the predator in you show.",
				"Developer preview | pressures nearby enemies",
				ChatFormatting.LIGHT_PURPLE);
		put("Assassin Stance", "Drop into a killing stance.",
				"Developer preview | replaces your basic attack while held",
				ChatFormatting.LIGHT_PURPLE);
		put("Spatial Execution", "Cut across space to reach a target.",
				"Developer preview | teleporting strike",
				ChatFormatting.LIGHT_PURPLE);
		put("Spiritualization", "Take Sung Il-Hwan's true form.",
				"Developer preview | raises every ability while it holds",
				ChatFormatting.LIGHT_PURPLE);
		put("Capture", "Pull and restrain enemies with crushing authority.",
				"Manifested: wider pull, suspension, and a throw recast",
				ChatFormatting.WHITE);
		put("Power Smash", "Drive a mana-loaded fist through a frontal formation.",
				"Manifested: greater reach, damage and guard damage",
				ChatFormatting.WHITE);
		put("Collapse", "Shatter the battlefield with a radial ground impact.",
				"Manifested: wider radius and heavier impact",
				ChatFormatting.WHITE);
		put("Spiritual Body Manifestation", "Take Goliath's spiritual body.",
				"Raises every Goliath ability while it holds",
				ChatFormatting.WHITE);
		put("Fire Charge", "Charge forward wreathed in fire.",
				"Legacy job ability",
				ChatFormatting.RED);
		put("Meteor Rain", "Call meteors down on the area.",
				"Legacy job ability",
				ChatFormatting.RED);
		put("Fireflies", "Release drifting motes of flame that seek enemies.",
				"Legacy job ability",
				ChatFormatting.RED);
		put("Monarch Beam", "Fire a concentrated beam of monarch power.",
				"Legacy job ability",
				ChatFormatting.LIGHT_PURPLE);
		put("Lightning Storm", "Call a storm of lightning over the area.",
				"Legacy job ability",
				ChatFormatting.YELLOW);
		put("Storm Burst", "Detonate the air around you in a burst of storm.",
				"Legacy job ability",
				ChatFormatting.YELLOW);
	}

	private SkillDescriptionRegistry() {
	}

	private static void put(String skill, String summary, String detail,
			ChatFormatting accent) {
		ENTRIES.put(skill, new Entry(summary, detail, accent));
	}

	public static boolean has(String skill) {
		if (HunterAbilityRegistry.isContributed(skill))
			return true;
		return skill != null && ENTRIES.containsKey(skill.trim());
	}

	/**
	 * Tooltip lines for one ability, or an empty list when this registry does
	 * not own it. Healer abilities append their current Intelligence stage,
	 * because that stage changes what the ability does rather than only its
	 * numbers.
	 */
	public static List<Component> tooltip(Entity entity, String skill) {
		Entry entry = skill == null ? null : ENTRIES.get(skill.trim());
		if (entry == null)
			// Not one of ours. A contributed ability describes itself in this same shape.
			return HunterAbilityRegistry.tooltip(skill);
		List<Component> lines = new ArrayList<>();
		lines.add(Component.literal(skill.trim())
				.withStyle(entry.accent(), ChatFormatting.BOLD));
		lines.add(Component.literal(entry.summary()).withStyle(ChatFormatting.GRAY));
		if (!entry.detail().isBlank())
			lines.add(Component.literal(entry.detail()).withStyle(ChatFormatting.YELLOW));
		if (HealerSkillManager.isHealerSkill(skill)) {
			int stage = HealerSkillManager.outputStage(entity);
			lines.add(Component.literal("Output: " + HealerSkillManager.stageName(stage)
					+ "  (Intelligence stage " + stage + "/5)")
					.withStyle(ChatFormatting.AQUA));
		}
		return lines;
	}
}
