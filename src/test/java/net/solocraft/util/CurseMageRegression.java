package net.solocraft.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Guards the Curse Mage's balance model.
 *
 * <p>{@link CurseType} carries the whole cooldown matrix and is dependency-free,
 * so the numbers are exercised directly rather than asserted as source text. The
 * only source reads here are the two facts that cannot be expressed in code: that
 * no curse name collides with the retired skills the login migration deletes, and
 * that the wheel skill is kept out of the QTE set.
 */
public final class CurseMageRegression {
	private static final Path MAIN = Path.of("src", "main", "java", "net", "solocraft");

	private CurseMageRegression() {
	}

	public static void main(String[] args) throws IOException {
		theRosterIsWellFormed();
		deliveryScalesTheLockout();
		strongerCursesCostMoreRotation();
		theWheelIsNotAQteSpell();
		noNameCollidesWithRetiredSkills();
		theWheelArrivesWithAnyCurseAbility();
		everyCurseHasAnEffectAndAMechanic();
		nothingSweepsEveryEntityEachTick();
		purificationRespectsTheIntelligenceGap();
		mobWeaversAreRealOwners();
		System.out.println("Curse Mage regression checks passed.");
	}

	/**
	 * A healer can break a curse woven at their own stage, or one stage above.
	 * Two stages above is out of reach entirely -- the weaver was working on a
	 * different level, and no amount of casting changes that.
	 */
	private static void purificationRespectsTheIntelligenceGap() throws IOException {
		for (int purifier = 1; purifier <= 5; purifier++) {
			for (int curse = 1; curse <= 5; curse++) {
				boolean expected = curse - purifier < 2;
				expect(CurseType.canPurify(purifier, curse) == expected,
						"Stage " + purifier + " purifier vs stage " + curse + " curse: expected "
								+ (expected ? "cleansable" : "resistant"));
			}
		}
		// The three cases the rule exists for.
		expect(CurseType.canPurify(3, 4), "One stage above must still come off");
		expect(!CurseType.canPurify(3, 5), "Two stages above must resist");
		expect(CurseType.canPurify(5, 1), "A stronger healer must always cleanse");

		// Only the dedicated purifiers may strip curses. An incidental cleanse from
		// a Sanctuary tick or a Blessing Mark reaction must not touch them.
		String healer = read("util", "HealerSkillManager.java");
		expect(healer.contains("purifyCurses(healer, ally, stage, removals)")
						|| healer.contains("purifyCurses(healer, healer, stage, removals)"),
				"Purification and Purifying Wave must break curses");
		int purifyCalls = countOccurrences(healer, "purifyCurses(");
		// One declaration plus four call sites: two abilities x ally and self.
		expect(purifyCalls >= 4 && purifyCalls <= 6,
				"Curse breaking must stay confined to the dedicated purifiers, found "
						+ purifyCalls + " references");
	}

	/**
	 * Generated Curse hunters cast the same curses at the party. Resolving the
	 * weaver only through the player list left every mob-cast curse sitting inert
	 * on its target, doing nothing at all.
	 */
	private static void mobWeaversAreRealOwners() throws IOException {
		String hooks = read("util", "CurseEffectHooks.java");
		expect(hooks.contains("level.getEntity(ownerId)"),
				"A non-player weaver must still be resolvable as a curse owner");
		expect(hooks.contains("private static Entity ownerOf("),
				"Curse ownership must not be typed to ServerPlayer");
		// The mana refund is the one part that genuinely requires a player.
		expect(hooks.contains("owner instanceof ServerPlayer"),
				"Only a player weaver may receive the Mana Rot refund");

		String state = read("util", "CurseState.java");
		expect(state.contains("data.putInt(stageKey(curse)"),
				"A curse must record the stage it was cast at, so it can resist a"
						+ " purifier even after its weaver is gone");
	}

	/**
	 * Every curse in the roster must be registered as an effect and must actually
	 * do something. Doom in particular shipped declared-but-inert once: it was in
	 * the roster, had a cooldown and a duration, and no code path ever read it.
	 */
	private static void everyCurseHasAnEffectAndAMechanic() throws IOException {
		String effects = read("init", "SololevelingModMobEffects.java");
		String state = read("util", "CurseState.java");
		String hooks = read("util", "CurseEffectHooks.java");
		String manager = read("util", "CurseMageSpellManager.java");
		String effectClass = read("potion", "CurseMobEffect.java");

		List<String> unregistered = new ArrayList<>();
		List<String> inert = new ArrayList<>();
		for (CurseType curse : CurseType.values()) {
			String constant = "CURSE_" + curse.name();
			if (!effects.contains(constant) || !state.contains(constant))
				unregistered.add(curse.name());
			// A curse earns its place one of three ways: a per-tick drain, an
			// attribute the effect carries, or a reaction to something happening.
			// Reactions live in either the manager (Blight's death hop) or the
			// hooks (Doom's expiry payout), so both count.
			boolean ticks = hooks.contains("case " + curse.name() + " ->");
			boolean attribute = effectClass.contains("case " + curse.name() + " ->");
			boolean reactive = manager.contains("CurseType." + curse.name())
					|| hooks.contains("CurseType." + curse.name());
			if (!ticks && !attribute && !reactive)
				inert.add(curse.name());
		}
		expect(unregistered.isEmpty(),
				"These curses have no registered effect: " + unregistered);
		expect(inert.isEmpty(),
				"These curses are declared but nothing acts on them: " + inert);
		expect(hooks.contains("onExpired") && hooks.contains("CurseType.DOOM"),
				"Doom must pay out through the expiry hook");
	}

	/**
	 * The reason curses became effects. A per-tick sweep over every living entity
	 * cost tick time whether or not anyone was playing the style; vanilla now ticks
	 * only the entities actually carrying a curse.
	 */
	private static void nothingSweepsEveryEntityEachTick() throws IOException {
		String manager = read("util", "CurseMageSpellManager.java");
		expect(!manager.contains("tickCurses"),
				"The full-level curse sweep must stay deleted");
		expect(!manager.contains("30000000"),
				"No curse code may scan the whole level with a world-sized AABB");
	}

	/**
	 * The wheel is infrastructure. A delivery casts whatever is armed, so owning
	 * one without the wheel strands the player on the starter curse forever. It
	 * must arrive automatically with any curse ability, and must never be
	 * obtainable on its own.
	 */
	private static void theWheelArrivesWithAnyCurseAbility() throws IOException {
		String progression = read("util", "MageSpellProgression.java");
		expect(progression.contains("grantCurseWheelCompanion(entity, skill);"),
				"unlockSkill must grant the wheel alongside any curse ability");
		// The companion has to inherit runestone permanence, or a non-Curse Mage
		// keeps the delivery and silently loses the wheel on next login.
		int runestoneGrant = progression.indexOf("public static boolean unlockFromRunestone");
		int end = progression.indexOf("\n\t}", runestoneGrant);
		String body = progression.substring(runestoneGrant, end);
		expect(body.contains("runestoneSkillKey(CurseMageSpellManager.CURSE_WEAVE)"),
				"A runestone-taught delivery must also mark the wheel as permanent");

		// No tier may hand out the wheel directly, and no runestone may exist for
		// it -- both would make it a reward instead of infrastructure.
		int tiersAt = progression.indexOf("CURSE_SPELL_TIERS");
		int tiersEnd = progression.indexOf(");", tiersAt);
		expect(tiersAt >= 0 && tiersEnd > tiersAt, "CURSE_SPELL_TIERS must exist");
		expect(!progression.substring(tiersAt, tiersEnd).contains("CURSE_WEAVE"),
				"The wheel must not be granted through a rank tier");

		String items = read("init", "SololevelingModItems.java");
		expect(!items.contains("runestone_curse_weave"),
				"There must be no runestone for the curse wheel");
		expect(items.contains("runestone_hex_bolt"),
				"The delivery runestones must still be registered");
	}

	/** Ids and keys are persisted, so duplicates or gaps would corrupt saves. */
	private static void theRosterIsWellFormed() {
		Set<Integer> ids = new HashSet<>();
		Set<String> keys = new HashSet<>();
		for (CurseType curse : CurseType.values()) {
			expect(ids.add(curse.id()), "Duplicate curse id: " + curse);
			expect(keys.add(curse.key()), "Duplicate curse key: " + curse);
			expect(curse.baseCooldownTicks() > 0, curse + " needs a positive cooldown");
			expect(curse.baseDurationTicks() > 0, curse + " needs a positive duration");
			expect(CurseType.byId(curse.id()) == curse, "byId must round-trip " + curse);
			expect(CurseType.byKey(curse.key()) == curse, "byKey must round-trip " + curse);
			expect(curse.cooldownKey().startsWith("curse_"),
					"Cooldown keys must be namespaced: " + curse.cooldownKey());
		}
		// Unlock tiers must cover 0..5 exactly, matching MageSpellProgression's
		// six rank tiers, or a rank would grant nothing or grant twice.
		for (int tier = 0; tier <= 5; tier++) {
			int atTier = 0;
			for (CurseType curse : CurseType.values()) {
				if (curse.unlockTier() == tier)
					atTier++;
			}
			expect(atTier == 1, "Exactly one curse must unlock at tier " + tier
					+ ", found " + atTier);
		}
		// An unknown key must fall back rather than throw, because saved data from
		// an older build can name a curse this version no longer has.
		expect(CurseType.byKey("not_a_curse") == CurseType.WITHERING,
				"An unknown curse key must fall back to the starter curse");
		expect(CurseType.byKey(null) == CurseType.WITHERING,
				"A null curse key must fall back to the starter curse");
	}

	/**
	 * The core balance claim: spreading a curse widely costs rotation, not
	 * potency. Every wider delivery must lock the curse out for strictly longer.
	 */
	private static void deliveryScalesTheLockout() {
		for (CurseType curse : CurseType.values()) {
			int direct = curse.cooldownTicks(CurseType.CurseDelivery.DIRECT);
			int area = curse.cooldownTicks(CurseType.CurseDelivery.AREA);
			int field = curse.cooldownTicks(CurseType.CurseDelivery.FIELD);
			int proxy = curse.cooldownTicks(CurseType.CurseDelivery.PROXY);
			expect(direct == curse.baseCooldownTicks(),
					"Direct delivery must cost the base cooldown for " + curse);
			expect(area > direct, curse + ": area must lock out longer than direct");
			expect(field > area, curse + ": a field must lock out longer than a burst");
			expect(proxy > field, curse + ": ally proxy must lock out longest");
			expect(proxy <= direct * 3,
					curse + ": proxy lockout must stay within 3x direct, got "
							+ proxy + " vs " + direct);
		}
		// A null delivery must not silently produce a free curse.
		expect(CurseType.DOOM.cooldownTicks(null) == CurseType.DOOM.baseCooldownTicks(),
				"An unknown delivery must fall back to the base cooldown");
	}

	/** Later curses are stronger, so they must always be rarer to reapply. */
	private static void strongerCursesCostMoreRotation() {
		CurseType[] roster = CurseType.values();
		for (int i = 1; i < roster.length; i++) {
			expect(roster[i].baseCooldownTicks() > roster[i - 1].baseCooldownTicks(),
					roster[i] + " must cost more rotation than " + roster[i - 1]);
		}
		// Worst case in the whole matrix, so a retune cannot quietly produce a
		// two-minute lockout on a single ability.
		int worst = roster[roster.length - 1]
				.cooldownTicks(CurseType.CurseDelivery.PROXY);
		expect(worst <= 1800, "The longest curse lockout must stay under 90s, got "
				+ worst / 20 + "s");
	}

	/**
	 * Curse Weave is intercepted client-side to open the wheel. If it ever joined
	 * the QTE set the aiming ring would fight the radial for the same key.
	 */
	private static void theWheelIsNotAQteSpell() throws IOException {
		String qte = read("util", "MageQTEHelper.java");
		expect(qte.contains("CurseMageSpellManager.MALEFIC_BURST"),
				"Curse QTE spells must be registered with the aiming ring");
		expect(!qte.contains("CurseMageSpellManager.CURSE_WEAVE"),
				"Curse Weave must never be a QTE spell; it opens the radial instead");

		String manager = read("util", "CurseMageSpellManager.java");
		expect(!manager.contains("QTE_SKILLS = Set.of(CURSE_WEAVE"),
				"Curse Weave must not appear in the QTE skill set");
	}

	/**
	 * MageSpellProgression strips retired skill names from every player on login.
	 * A collision would delete a working Curse Mage's kit silently.
	 */
	private static void noNameCollidesWithRetiredSkills() throws IOException {
		String progression = read("util", "MageSpellProgression.java");
		int start = progression.indexOf("RETIRED_UNBOUND_SKILLS");
		int end = progression.indexOf(");", start);
		expect(start >= 0 && end > start, "RETIRED_UNBOUND_SKILLS must still exist");
		String retired = progression.substring(start, end);

		List<String> collisions = new ArrayList<>();
		for (String skill : new String[] { "Curse Weave", "Hex Bolt", "Malefic Burst",
				"Creeping Miasma", "Vector of Ruin", "Culling" }) {
			if (retired.contains("\"" + skill + "\""))
				collisions.add(skill);
		}
		expect(collisions.isEmpty(),
				"These Curse Mage skills are on the retired list and will be deleted"
						+ " from every player on login: " + collisions);
	}

	private static int countOccurrences(String haystack, String needle) {
		int count = 0;
		for (int at = haystack.indexOf(needle); at >= 0;
				at = haystack.indexOf(needle, at + needle.length()))
			count++;
		return count;
	}

	private static String read(String... parts) throws IOException {
		Path path = MAIN;
		for (String part : parts)
			path = path.resolve(part);
		return Files.readString(path).replace("\r\n", "\n");
	}

	private static void expect(boolean condition, String message) {
		if (!condition)
			throw new AssertionError(message);
	}
}
