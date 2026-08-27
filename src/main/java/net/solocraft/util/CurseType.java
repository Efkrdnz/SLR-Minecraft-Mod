package net.solocraft.util;

import java.util.Arrays;

/**
 * The Curse Mage's payload roster.
 *
 * <p>Curse Mage splits into delivery and payload: an ability decides <em>how</em> a
 * curse lands, this enum decides <em>what</em> lands. Because the two are
 * independent, the cooldown belongs to the curse rather than the ability -- see
 * {@link #baseCooldownTicks()} and {@link CurseDelivery}.
 *
 * <p>Ids are persisted in player data and sent over the wire, so they must never
 * be reordered or reused. Append only.
 */
public enum CurseType {
	WITHERING(0, "withering", "Withering", 0xFF8FBF4A, 100, 200,
			"Rots the target from within, bleeding life away over time."),
	ENFEEBLEMENT(1, "enfeeblement", "Enfeeblement", 0xFF9A6BD8, 140, 200,
			"Saps strength, weakening everything the target deals out."),
	LEADEN(2, "leaden", "Leaden", 0xFF6E7A99, 160, 180,
			"Weighs the target down and leaves it easy to throw around."),
	MANA_ROT(3, "mana_rot", "Mana Rot", 0xFF4FD6E8, 240, 160,
			"Corrodes the target's mana and feeds a share of it back to you."),
	BLIGHT(4, "blight", "Blight", 0xFF5FBF6A, 400, 240,
			"On death, leaps to the nearest uncursed enemy and takes root again."),
	DOOM(5, "doom", "Doom", 0xFFD8434F, 600, 200,
			"Marks an ending. If the mark survives its timer, it detonates.");

	private final int id;
	private final String key;
	private final String displayName;
	private final int accentColor;
	private final int baseCooldownTicks;
	private final int baseDurationTicks;
	private final String description;

	CurseType(int id, String key, String displayName, int accentColor,
			int baseCooldownTicks, int baseDurationTicks, String description) {
		this.id = id;
		this.key = key;
		this.displayName = displayName;
		this.accentColor = accentColor;
		this.baseCooldownTicks = baseCooldownTicks;
		this.baseDurationTicks = baseDurationTicks;
		this.description = description;
	}

	public int id() {
		return id;
	}

	/** Stable string written to player data and used for cooldown keys. */
	public String key() {
		return key;
	}

	public String displayName() {
		return displayName;
	}

	public int accentColor() {
		return accentColor;
	}

	/** Lockout before this curse can be applied again, before delivery scaling. */
	public int baseCooldownTicks() {
		return baseCooldownTicks;
	}

	public int baseDurationTicks() {
		return baseDurationTicks;
	}

	public String description() {
		return description;
	}

	/** The cooldown key this curse occupies, shared across every delivery. */
	public String cooldownKey() {
		return "curse_" + key;
	}

	/** Hunter-rank tier at which this curse joins the wheel, 0 through 5. */
	public int unlockTier() {
		return id;
	}

	/**
	 * How far a curse's Intelligence stage may exceed a purifier's before it
	 * cannot be cleansed at all. A gap of one is still workable; two means the
	 * weaver was simply operating on a different level.
	 */
	public static final int PURIFY_STAGE_GAP = 2;

	/**
	 * Whether a purifier of this stage can strip a curse of that stage.
	 *
	 * <p>Lives here rather than on CurseState because CurseState now speaks to the
	 * effect registry, and this rule has to stay exercisable without Minecraft on
	 * the classpath.
	 */
	public static boolean canPurify(int purifierStage, int curseStage) {
		return curseStage - purifierStage < PURIFY_STAGE_GAP;
	}

	public static CurseType byId(int id) {
		return Arrays.stream(values()).filter(value -> value.id == id)
				.findFirst().orElse(WITHERING);
	}

	public static CurseType byKey(String key) {
		if (key == null || key.isBlank())
			return WITHERING;
		String normalized = key.trim().toLowerCase();
		return Arrays.stream(values()).filter(value -> value.key.equals(normalized))
				.findFirst().orElse(WITHERING);
	}

	/**
	 * How a curse reached its target. The multiplier is the balance lever for the
	 * whole style: spreading one curse across a crowd locks it out far longer than
	 * placing it on a single target, so area delivery costs rotation rather than
	 * potency.
	 */
	public enum CurseDelivery {
		DIRECT(1.0D),
		AREA(1.6D),
		FIELD(2.0D),
		PROXY(2.5D);

		private final double cooldownMultiplier;

		CurseDelivery(double cooldownMultiplier) {
			this.cooldownMultiplier = cooldownMultiplier;
		}

		public double cooldownMultiplier() {
			return cooldownMultiplier;
		}
	}

	/** Lockout for this curse when applied through the given delivery. */
	public int cooldownTicks(CurseDelivery delivery) {
		double multiplier = delivery == null ? 1.0D : delivery.cooldownMultiplier();
		return (int) Math.round(baseCooldownTicks * multiplier);
	}
}
