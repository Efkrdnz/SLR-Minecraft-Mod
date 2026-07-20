package net.solocraft.dungeon.runtime;

import net.solocraft.network.SololevelingModVariables;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;

import java.util.Collection;

/**
 * Shared level lookups for dungeon runtime code.
 *
 * <p>Players keep their level in the Solo Leveling player capability. Mobs use
 * persistent entity NBT. New dungeon entities receive a namespaced runtime key
 * as the source of truth and also mirror the legacy {@code Level} key used by
 * existing combat and reward procedures.</p>
 */
public final class DungeonLevelHelper {
	public static final String DUNGEON_LEVEL_TAG = "slr_dungeon_level";
	public static final String LEGACY_LEVEL_TAG = "Level";

	private DungeonLevelHelper() {
	}

	/** Returns the player's capability level, or zero when it is unavailable. */
	public static double playerLevel(@Nullable Player player) {
		if (player == null)
			return 0.0D;
		double level = player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
				.map(variables -> variables.Level)
				.orElse(0.0D);
		return finiteNonNegative(level);
	}

	/**
	 * Resolves a level consistently for either a player or a dungeon mob.
	 * Players use {@link SololevelingModVariables.PlayerVariables#Level}; living
	 * non-player entities use persistent NBT.
	 */
	public static double levelOf(@Nullable Entity entity) {
		if (entity instanceof Player player)
			return playerLevel(player);
		if (!(entity instanceof LivingEntity))
			return 0.0D;

		CompoundTag persistent = entity.getPersistentData();
		if (persistent.contains(DUNGEON_LEVEL_TAG, Tag.TAG_ANY_NUMERIC))
			return finiteNonNegative(persistent.getDouble(DUNGEON_LEVEL_TAG));
		if (persistent.contains(LEGACY_LEVEL_TAG, Tag.TAG_ANY_NUMERIC))
			return finiteNonNegative(persistent.getDouble(LEGACY_LEVEL_TAG));
		return 0.0D;
	}

	/** Stores both the canonical dungeon level and the legacy compatibility key. */
	public static void setEntityLevel(LivingEntity entity, int level) {
		int safeLevel = Math.max(0, level);
		entity.getPersistentData().putInt(DUNGEON_LEVEL_TAG, safeLevel);
		entity.getPersistentData().putDouble(LEGACY_LEVEL_TAG, safeLevel);
	}

	/** Rounds and clamps a level, tolerating reversed limits and non-finite input. */
	public static int clampLevel(double requestedLevel, int minimum, int maximum) {
		int low = Math.min(minimum, maximum);
		int high = Math.max(minimum, maximum);
		if (!Double.isFinite(requestedLevel))
			return low;
		long rounded = Math.round(requestedLevel);
		if (rounded <= low)
			return low;
		if (rounded >= high)
			return high;
		return (int) rounded;
	}

	/**
	 * Resolves and clamps an instance level from its owner or captured party.
	 * Party calculations ignore entities with no positive level and fall back to
	 * the owner, then the supplied fixed level.
	 */
	public static int resolveEffectiveLevel(EffectiveLevelSource source, @Nullable Entity owner,
			Collection<? extends Entity> participants, int fixedLevel, int minimum, int maximum) {
		EffectiveLevelSource safeSource = source == null ? EffectiveLevelSource.FIXED : source;
		double ownerLevel = levelOf(owner);
		double resolved = switch (safeSource) {
			case FIXED -> fixedLevel;
			case OWNER -> ownerLevel;
			case PARTY_AVERAGE -> averagePositiveLevel(participants);
			case PARTY_HIGHEST -> highestPositiveLevel(participants);
		};

		if (resolved <= 0.0D)
			resolved = ownerLevel;
		if (resolved <= 0.0D)
			resolved = fixedLevel;
		return clampLevel(resolved, minimum, maximum);
	}

	public static double averagePositiveLevel(Collection<? extends Entity> participants) {
		if (participants == null || participants.isEmpty())
			return 0.0D;
		double total = 0.0D;
		int count = 0;
		for (Entity participant : participants) {
			double level = levelOf(participant);
			if (level > 0.0D) {
				total += level;
				count++;
			}
		}
		return count == 0 ? 0.0D : total / count;
	}

	public static double highestPositiveLevel(Collection<? extends Entity> participants) {
		if (participants == null || participants.isEmpty())
			return 0.0D;
		double highest = 0.0D;
		for (Entity participant : participants)
			highest = Math.max(highest, levelOf(participant));
		return highest;
	}

	private static double finiteNonNegative(double value) {
		return Double.isFinite(value) ? Math.max(0.0D, value) : 0.0D;
	}

	public enum EffectiveLevelSource {
		FIXED,
		OWNER,
		PARTY_AVERAGE,
		PARTY_HIGHEST
	}
}
