package net.solocraft.procedures;

import net.solocraft.util.ShadowMonarchManager;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.LevelAccessor;

import java.util.UUID;

final class ShadowKillCreditHelper {
	private ShadowKillCreditHelper() {
	}

	static Player creditedPlayer(LevelAccessor world, Entity source) {
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

	static ServerPlayer creditedServerPlayer(LevelAccessor world, Entity source) {
		Player player = creditedPlayer(world, source);
		return player instanceof ServerPlayer serverPlayer ? serverPlayer : null;
	}

	static Entity creditedSource(LevelAccessor world, Entity source) {
		Player player = creditedPlayer(world, source);
		return player != null ? player : source;
	}
}
