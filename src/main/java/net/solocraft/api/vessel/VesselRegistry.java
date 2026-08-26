package net.solocraft.api.vessel;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

import net.solocraft.network.SololevelingModVariables;
import net.solocraft.util.VesselManager;

/**
 * Every vessel the game knows about, built-in or contributed.
 *
 * <p>The built-ins are not copied here. They are derived from the list
 * {@link VesselManager} already owns, so this stays a view over that list rather
 * than a second table that could disagree with it. Addons register alongside them.
 *
 * <p>No new persisted field was introduced. Vessels are already stored as
 * {@code vesselType} + {@code vesselIdentity} + {@code JOB}, written together by
 * {@link VesselManager} and read by every combat manager. A contributed vessel
 * simply stores a namespaced identity where a built-in stores a bare one, which
 * keeps existing identity checks working: they compare against their own literal
 * and a contributed value matches none of them, which is the correct answer.
 */
public final class VesselRegistry {
	public static final String NAMESPACE = "sololeveling";

	/**
	 * Value written to the legacy {@code JOB} mirror for a vessel with no number.
	 *
	 * <p>Zero means "no vessel" to the existing checks, and any real number would
	 * impersonate a built-in. One reserved value outside the original range reads
	 * as "holds a vessel, but not one you know".
	 */
	public static final int CUSTOM_VESSEL_LEGACY_MIRROR = 1000;

	private static final Map<ResourceLocation, Vessel> BY_ID = new ConcurrentHashMap<>();
	private static final Map<Integer, Vessel> BY_LEGACY_JOB_ID = new ConcurrentHashMap<>();
	private static final Map<String, Vessel> BY_IDENTITY = new ConcurrentHashMap<>();

	static {
		for (VesselManager.VesselDefinition definition : VesselManager.definitions())
			registerInternal(new Vessel(id(definition.identity()), definition.jobId(),
					Vessel.Kind.fromLegacyType(definition.type()), definition.identity(),
					definition.name(), definition.powerName(), definition.description()));
	}

	private VesselRegistry() {
	}

	public static ResourceLocation id(String path) {
		return ResourceLocation.fromNamespaceAndPath(NAMESPACE, path);
	}

	/**
	 * Adds a vessel. Call during mod construction, before anything reads this.
	 *
	 * @throws IllegalArgumentException if the id, legacy job id, or identity is taken
	 */
	public static Vessel register(Vessel vessel) {
		if (vessel == null)
			throw new IllegalArgumentException("Vessel must not be null");
		return registerInternal(vessel);
	}

	private static Vessel registerInternal(Vessel vessel) {
		Vessel existingId = BY_ID.putIfAbsent(vessel.id(), vessel);
		if (existingId != null)
			throw new IllegalArgumentException("Duplicate vessel id: " + vessel.id());

		String identityKey = storedIdentity(vessel).toLowerCase(Locale.ROOT);
		Vessel existingIdentity = BY_IDENTITY.putIfAbsent(identityKey, vessel);
		if (existingIdentity != null) {
			BY_ID.remove(vessel.id(), vessel);
			throw new IllegalArgumentException("Vessel identity \"" + identityKey
					+ "\" is already held by " + existingIdentity.id());
		}

		if (vessel.hasLegacyJobId()) {
			Vessel existingJob = BY_LEGACY_JOB_ID.putIfAbsent(vessel.legacyJobId(), vessel);
			if (existingJob != null) {
				BY_ID.remove(vessel.id(), vessel);
				BY_IDENTITY.remove(identityKey, vessel);
				throw new IllegalArgumentException("Legacy job id " + vessel.legacyJobId()
						+ " is already held by " + existingJob.id());
			}
		}
		return vessel;
	}

	/**
	 * What goes in {@code vesselIdentity}. Built-ins keep their bare string so
	 * existing checks still match; contributed vessels use their full id, which
	 * cannot collide across mods.
	 */
	public static String storedIdentity(Vessel vessel) {
		if (vessel == null)
			return "";
		return vessel.hasLegacyJobId() ? vessel.identity() : vessel.id().toString();
	}

	/**
	 * Declared presentations, keyed by vessel id.
	 *
	 * <p>Replaced wholesale on every data-pack reload and on every sync, because
	 * a presentation left behind by a pack that is no longer loaded would theme a
	 * vessel nobody can see.
	 */
	private static final Map<ResourceLocation, VesselPresentation> PRESENTATION =
			new ConcurrentHashMap<>();

	public static synchronized void replacePresentations(
			Map<ResourceLocation, VesselPresentation> presentations) {
		PRESENTATION.clear();
		if (presentations != null)
			PRESENTATION.putAll(presentations);
	}

	public static Optional<VesselPresentation> presentation(ResourceLocation vesselId) {
		return vesselId == null ? Optional.empty()
				: Optional.ofNullable(PRESENTATION.get(vesselId));
	}

	public static synchronized List<VesselPresentation> presentations() {
		return List.copyOf(new ArrayList<>(PRESENTATION.values()));
	}

	/**
	 * The presentation for a vessel the selection screen knows only by its stored
	 * identity string.
	 *
	 * <p>Empty for every built-in: they are themed by the screen's own table, and
	 * a contributed presentation must never be able to restyle a shipped Monarch.
	 */
	public static Optional<VesselPresentation> presentationForStoredIdentity(String identity) {
		return byStoredIdentity(identity)
				.filter(vessel -> vessel.kind() == Vessel.Kind.MONARCH)
				.flatMap(vessel -> presentation(vessel.id()));
	}

	public static Optional<Vessel> byId(ResourceLocation id) {
		return id == null ? Optional.empty() : Optional.ofNullable(BY_ID.get(id));
	}

	public static Optional<Vessel> byLegacyJobId(int legacyJobId) {
		return Optional.ofNullable(BY_LEGACY_JOB_ID.get(legacyJobId));
	}

	public static Optional<Vessel> byStoredIdentity(String identity) {
		if (identity == null || identity.isBlank())
			return Optional.empty();
		return Optional.ofNullable(BY_IDENTITY.get(identity.trim().toLowerCase(Locale.ROOT)));
	}

	/** Built-ins first in their original order, then contributed vessels by id. */
	public static List<Vessel> all() {
		List<Vessel> vessels = new ArrayList<>(BY_ID.values());
		vessels.sort(Comparator.comparingInt((Vessel value) ->
						value.hasLegacyJobId() ? value.legacyJobId() : Integer.MAX_VALUE)
				.thenComparing(value -> value.id().toString()));
		return List.copyOf(vessels);
	}

	public static List<Vessel> ofKind(Vessel.Kind kind) {
		return all().stream().filter(vessel -> vessel.kind() == kind).toList();
	}

	/**
	 * Resolves the vessel a player currently holds.
	 *
	 * <p>The stored identity is only trusted when it agrees with {@code JOB}.
	 * Both are written together, but a save edited by hand or written by a build
	 * with a different vessel list could disagree, and {@code JOB} is what the
	 * rest of the mod actually acts on.
	 */
	public static Optional<Vessel> of(Entity entity) {
		SololevelingModVariables.PlayerVariables variables = variablesOf(entity);
		if (variables == null)
			return Optional.empty();

		int legacyJobId = (int) Math.floor(variables.JOB);
		Optional<Vessel> stored = byStoredIdentity(variables.vesselIdentity);
		if (stored.isPresent() && agreesWith(stored.get(), legacyJobId))
			return stored;
		return byLegacyJobId(legacyJobId);
	}

	private static boolean agreesWith(Vessel vessel, int legacyJobId) {
		return vessel.hasLegacyJobId()
				? vessel.legacyJobId() == legacyJobId
				: legacyJobId == CUSTOM_VESSEL_LEGACY_MIRROR;
	}

	/**
	 * Assigns a contributed vessel, writing the same three fields
	 * {@link VesselManager} writes so nothing downstream can tell the difference.
	 *
	 * <p>Built-in vessels keep going through {@link VesselManager}, which owns
	 * claim limits and the selection flow. This exists for vessels that scheme
	 * cannot represent.
	 *
	 * @return false when the vessel is unknown or the player has no variables
	 */
	public static boolean assign(Entity entity, Vessel vessel) {
		if (entity == null || vessel == null)
			return false;
		if (BY_ID.get(vessel.id()) != vessel)
			return false;
		if (variablesOf(entity) == null)
			return false;

		entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
				.ifPresent(capability -> {
					capability.vesselType = vessel.kind().legacyType();
					capability.vesselIdentity = storedIdentity(vessel);
					capability.JOB = vessel.hasLegacyJobId()
							? vessel.legacyJobId()
							: CUSTOM_VESSEL_LEGACY_MIRROR;
					capability.syncPlayerVariables(entity);
				});
		return true;
	}

	private static SololevelingModVariables.PlayerVariables variablesOf(Entity entity) {
		if (entity == null)
			return null;
		return entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
				.orElse(null);
	}
}
