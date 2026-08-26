package net.solocraft.api.vessel;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.world.entity.Entity;

import net.solocraft.network.SololevelingModVariables;
import net.solocraft.util.AntaresCombatManager;
import net.solocraft.util.BeastMonarchManager;
import net.solocraft.util.FrostMonarchManager;
import net.solocraft.util.GoliathCombatManager;
import net.solocraft.util.LiuZhigangCombatManager;
import net.solocraft.util.WhiteFlameMonarchManager;

/**
 * Whether a vessel's heightened form is currently active, built-in or contributed.
 *
 * <p>Each built-in vessel grew its own predicate as it was written --
 * spiritualized, manifested, combat stance, fang stance -- so asking "is this
 * hunter transformed right now" meant knowing which vessel they hold and which
 * of six managers to ask. An ability wanting a different behaviour while
 * spiritualized should not have to know any of that, and an addon cannot.
 *
 * <p>Built-in forms are not replaced. Each manager remains the authority for its
 * own vessel and is asked directly, so the answer here can never disagree with
 * the vessel that owns it. Contributed forms are stored as ids on the player and
 * ride the normal sync, which means the client can render an aura for one
 * without the addon writing a packet.
 *
 * <p>Deliberately flat statics over strings rather than a predicate registry: a
 * lambda cannot be expressed as an MCreator procedure block, and MCreator authors
 * are a target audience for this API.
 */
public final class VesselState {
	/** Guards against an addon looping and filling the synced field. */
	private static final int MAX_ACTIVE_FORMS = 16;
	private static final int MAX_FORM_ID_LENGTH = 64;

	/** Forms declared to replace the normal left-click attack. */
	private static final Set<String> MELEE_CLAIMING_FORMS = ConcurrentHashMap.newKeySet();

	private VesselState() {
	}

	/**
	 * True while any heightened form is active -- Spiritualization, Manifestation,
	 * or a combat stance, whichever that vessel calls it.
	 *
	 * <p>Use this to branch an ability into its transformed variant.
	 */
	public static boolean isSpiritualized(Entity entity) {
		if (entity == null)
			return false;
		return isBuiltInFormActive(entity) || !activeForms(entity).isEmpty();
	}

	/**
	 * True while the hunter is in a melee stance that replaces the normal attack.
	 *
	 * <p>Narrower than {@link #isSpiritualized}: a stance means left click is
	 * already claimed, so an addon adding its own melee should stand down rather
	 * than fight over the same input.
	 */
	public static boolean isMeleeStanceActive(Entity entity) {
		if (entity == null)
			return false;
		if (GoliathCombatManager.isCombatStance(entity)
				|| LiuZhigangCombatManager.isCombatStance(entity)
				|| BeastMonarchManager.isFangStance(entity))
			return true;
		for (String form : activeForms(entity))
			if (MELEE_CLAIMING_FORMS.contains(form))
				return true;
		return false;
	}

	private static boolean isBuiltInFormActive(Entity entity) {
		return FrostMonarchManager.isSpiritualized(entity)
				|| WhiteFlameMonarchManager.isSpiritualized(entity)
				|| AntaresCombatManager.isManifested(entity)
				|| GoliathCombatManager.isManifested(entity)
				|| GoliathCombatManager.isCombatStance(entity)
				|| LiuZhigangCombatManager.isManifested(entity)
				|| LiuZhigangCombatManager.isCombatStance(entity)
				|| BeastMonarchManager.isFangStance(entity);
	}

	/**
	 * Declares that a form replaces the left-click attack while it is active.
	 *
	 * <p>Call once during mod construction. Optional: an undeclared form still
	 * counts as spiritualized, it just does not claim the attack button.
	 */
	public static void declareMeleeClaimingForm(String formId) {
		String normalised = normalise(formId);
		if (!normalised.isEmpty())
			MELEE_CLAIMING_FORMS.add(normalised);
	}

	/** True while this specific form is active on this hunter. */
	public static boolean isFormActive(Entity entity, String formId) {
		String normalised = normalise(formId);
		return !normalised.isEmpty() && activeForms(entity).contains(normalised);
	}

	/** Every contributed form currently active, ids only. Never null. */
	public static List<String> activeForms(Entity entity) {
		SololevelingModVariables.PlayerVariables variables = variablesOf(entity);
		if (variables == null || variables.activeForms == null || variables.activeForms.isBlank())
			return List.of();

		List<String> forms = new ArrayList<>();
		for (String part : variables.activeForms.split(",")) {
			String normalised = normalise(part);
			if (!normalised.isEmpty() && !forms.contains(normalised))
				forms.add(normalised);
		}
		return List.copyOf(forms);
	}

	/**
	 * Turns a contributed form on or off.
	 *
	 * <p>Server-side. The client is synchronised as a result, which is what lets
	 * a registered aura render for the form; deciding this on the client would
	 * make a transformation something a modified client could grant itself.
	 *
	 * @return false when nothing changed
	 */
	public static boolean setFormActive(Entity entity, String formId, boolean active) {
		String normalised = normalise(formId);
		if (entity == null || normalised.isEmpty() || variablesOf(entity) == null)
			return false;

		List<String> forms = new ArrayList<>(activeForms(entity));
		boolean changed = active ? addForm(forms, normalised) : forms.remove(normalised);
		if (!changed)
			return false;

		String joined = String.join(",", forms);
		entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
				.ifPresent(capability -> {
					capability.activeForms = joined;
					capability.syncPlayerVariables(entity);
				});
		return true;
	}

	private static boolean addForm(List<String> forms, String formId) {
		if (forms.contains(formId) || forms.size() >= MAX_ACTIVE_FORMS)
			return false;
		return forms.add(formId);
	}

	/** Clears every contributed form. Built-in forms are untouched. */
	public static boolean clearForms(Entity entity) {
		if (entity == null || variablesOf(entity) == null || activeForms(entity).isEmpty())
			return false;
		entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
				.ifPresent(capability -> {
					capability.activeForms = "";
					capability.syncPlayerVariables(entity);
				});
		return true;
	}

	private static String normalise(String formId) {
		if (formId == null)
			return "";
		String trimmed = formId.trim().toLowerCase(Locale.ROOT);
		// Commas separate entries in the stored field, so one inside an id would
		// silently split it into two forms that no lookup would ever match.
		trimmed = trimmed.replace(",", "");
		if (trimmed.length() > MAX_FORM_ID_LENGTH)
			trimmed = trimmed.substring(0, MAX_FORM_ID_LENGTH);
		return trimmed;
	}

	private static SololevelingModVariables.PlayerVariables variablesOf(Entity entity) {
		if (entity == null)
			return null;
		return entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
				.orElse(null);
	}
}
