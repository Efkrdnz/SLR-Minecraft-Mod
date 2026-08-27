package net.solocraft.api.skill;

import java.util.Objects;

import net.minecraft.ChatFormatting;
import net.minecraft.resources.ResourceLocation;

import net.solocraft.api.AbilityCost;

/**
 * A complete ability declaration, filled in the way the mod fills its own.
 *
 * <p>Nothing here is optional decoration. The mod shows an ability in the skill
 * list, on the equipped slots, in the top-left overlay, and in a tooltip, and it
 * has a name, a colour, a summary, and a rule line for each of those places. An
 * addon ability that left any of them blank would read as unfinished next to a
 * built-in one, which is the opposite of the point.
 *
 * <p>Abilities are cast only through the mod's skill system: the hunter learns
 * one from a runestone, selects it, and presses the skill key. There is no
 * keybind of your own to add and no command to expose.
 *
 * @param id           stable identity, namespaced to your mod. Not shown.
 * @param name         the ability's name, stored in the hunter's skill list and
 *                     shown everywhere with the addon marker in front of it.
 * @param summary      one line saying what it does. The grey tooltip line.
 * @param detail       the rule worth knowing. The yellow tooltip line.
 * @param accent       colour for the name, in lists, slots, and the overlay.
 * @param cost         weight band the mana cost is derived from.
 * @param cooldownTicks recovery after a successful cast.
 * @param owningClass  the hunter class that may use it, or null for any class.
 *                     Use a built-in class id to extend a shipped class.
 * @param icon         a 20x20 texture drawn in the ability slot on the HUD, or
 *                     null to leave the slot showing only its frame. Give the
 *                     full texture path, the way a resource pack would.
 */
public record HunterAbility(ResourceLocation id, String name, String summary, String detail,
		ChatFormatting accent, AbilityCost cost, int cooldownTicks, ResourceLocation owningClass,
		Mode mode, int upkeepPerSecond, ResourceLocation icon) {

	/** How an ability is used, which is a different question from what it does. */
	public enum Mode {
		/** Fires once per press. Most abilities. */
		INSTANT,
		/**
		 * Turns on, stays on, and drains mana until it is turned off or cannot be
		 * paid for. Spiritualizations work this way.
		 *
		 * <p>A toggle is held as a {@code VesselState} form, so while it is on the
		 * hunter counts as spiritualized, can carry an aura, and can claim the
		 * attack button, with nothing extra to register.
		 */
		TOGGLE
	}

	/** Convenience for an ordinary single-press ability. */
	public static HunterAbility instant(ResourceLocation id, String name, String summary,
				String detail, ChatFormatting accent, AbilityCost cost, int cooldownTicks,
				ResourceLocation owningClass) {
		return new HunterAbility(id, name, summary, detail, accent, cost, cooldownTicks,
				owningClass, Mode.INSTANT, 0, null);
	}

	public HunterAbility {
		Objects.requireNonNull(id, "Ability id must not be null");
		Objects.requireNonNull(accent, "Ability accent must not be null");
		Objects.requireNonNull(cost, "Ability cost band must not be null");

		name = clean(name);
		summary = clean(summary);
		detail = detail == null ? "" : clean(detail);

		if (name.isEmpty())
			throw new IllegalArgumentException("Ability " + id + " must have a name");
		if (summary.isEmpty())
			throw new IllegalArgumentException("Ability " + id
					+ " must have a summary; it is the tooltip line players read first");
		if (mode == null)
			mode = Mode.INSTANT;
		upkeepPerSecond = Math.max(0, upkeepPerSecond);
		if (mode == Mode.TOGGLE && upkeepPerSecond <= 0)
			throw new IllegalArgumentException("Toggle ability " + id
				+ " needs an upkeep; without one it would never end on its own");
		if (cooldownTicks < 0)
			throw new IllegalArgumentException("Ability " + id + " cannot have a negative cooldown");
	}

	/** True for an ability that stays on until turned off or unpaid. */
	public boolean isToggle() {
		return mode == Mode.TOGGLE;
	}

	/**
	 * The {@code VesselState} form id this ability holds while active.
	 *
	 * <p>Derived from the ability id so it is namespaced without the author
	 * having to think about collisions.
	 */
	public String formId() {
		return id.toString();
	}

	/** True when the HUD slot has something to draw beyond its frame. */
	public boolean hasIcon() {
		return icon != null;
	}

	/** Any class may use it. */
	public boolean isClassless() {
		return owningClass == null;
	}

	/**
	 * Commas separate entries in the stored skill list, so one inside a name
	 * would split the ability into two the game could never find again.
	 */
	private static String clean(String value) {
		return value == null ? "" : value.replace(",", "").trim();
	}
}
