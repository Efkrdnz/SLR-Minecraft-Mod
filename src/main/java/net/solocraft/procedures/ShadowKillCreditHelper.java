package net.solocraft.procedures;

import net.solocraft.util.ShadowMonarchManager;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.LevelAccessor;

import javax.annotation.Nullable;

import java.util.UUID;

public final class ShadowKillCreditHelper {
	private static final String LAST_PLAYER_DAMAGE_UUID =
			"SLRLastPlayerDamageUUID";
	private static final String LAST_PLAYER_DAMAGE_EXPIRES =
			"SLRLastPlayerDamageExpires";
	private static final long ENVIRONMENTAL_KILL_CREDIT_TICKS = 20L * 20L;

	private ShadowKillCreditHelper() {
	}

	public static Player creditedPlayer(LevelAccessor world,
			@Nullable Entity source) {
		if (source == null)
			return null;
		if (source instanceof Player player)
			return player;
		if (source instanceof Projectile projectile && projectile.getOwner() != null)
			return creditedPlayer(world, projectile.getOwner());
		if (source instanceof TamableAnimal tame && tame.isTame() && tame.getOwner() instanceof Player owner)
			return owner;
		UUID ownerId = ShadowMonarchManager.getShadowOwnerUUID(source);
		if (ownerId == null)
			return null;
		if (world instanceof ServerLevel level)
			return level.getPlayerByUUID(ownerId);
		for (Player player : world.players()) {
			if (player.getUUID().equals(ownerId))
				return player;
		}
		return null;
	}

	public static ServerPlayer creditedServerPlayer(LevelAccessor world,
			@Nullable Entity source) {
		Player player = creditedPlayer(world, source);
		return player instanceof ServerPlayer serverPlayer ? serverPlayer : null;
	}

	public static Entity creditedSource(LevelAccessor world,
			@Nullable Entity source) {
		Player player = creditedPlayer(world, source);
		return player != null ? player : source;
	}

	/**
	 * Remembers the owning player whenever a player, pet, projectile, or shadow
	 * damages a target. This lets later fire, fall, void, and collision damage
	 * preserve the actual combat owner.
	 */
	public static void rememberRecentPlayerDamage(LivingEntity victim,
			@Nullable Entity source, @Nullable Entity directSource) {
		if (victim == null || victim.level().isClientSide())
			return;
		Player player = creditedPlayer(victim.level(), source);
		if (player == null)
			player = creditedPlayer(victim.level(), directSource);
		if (player == null || player == victim)
			return;
		victim.getPersistentData().putUUID(LAST_PLAYER_DAMAGE_UUID,
				player.getUUID());
		victim.getPersistentData().putLong(LAST_PLAYER_DAMAGE_EXPIRES,
				victim.level().getGameTime()
						+ ENVIRONMENTAL_KILL_CREDIT_TICKS);
	}

	@Nullable
	public static Player creditedPlayerForDeath(LevelAccessor world,
			LivingEntity victim, @Nullable Entity source,
			@Nullable Entity directSource) {
		Player player = creditedPlayer(world, source);
		if (player == null)
			player = creditedPlayer(world, directSource);
		if (player == null && victim != null)
			player = creditedPlayer(world, victim.getKillCredit());
		if (player != null || victim == null)
			return player;
		if (!victim.getPersistentData().hasUUID(LAST_PLAYER_DAMAGE_UUID))
			return null;
		if (victim.getPersistentData().getLong(LAST_PLAYER_DAMAGE_EXPIRES)
				< victim.level().getGameTime()) {
			victim.getPersistentData().remove(LAST_PLAYER_DAMAGE_UUID);
			victim.getPersistentData().remove(LAST_PLAYER_DAMAGE_EXPIRES);
			return null;
		}
		UUID playerId = victim.getPersistentData().getUUID(
				LAST_PLAYER_DAMAGE_UUID);
		if (world instanceof ServerLevel level)
			return level.getServer().getPlayerList().getPlayer(playerId);
		for (Player candidate : world.players())
			if (candidate.getUUID().equals(playerId))
				return candidate;
		return null;
	}

	@Nullable
	public static Entity creditedSourceForDeath(LevelAccessor world,
			LivingEntity victim, @Nullable Entity source,
			@Nullable Entity directSource) {
		Player player = creditedPlayerForDeath(world, victim, source,
				directSource);
		if (player != null)
			return player;
		return source != null ? source : directSource;
	}
}
