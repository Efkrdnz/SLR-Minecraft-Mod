package net.solocraft.util;

import net.solocraft.entity.HealerVfxEntity;
import net.solocraft.network.SololevelingModVariables;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * Behaviour behind the curse effects.
 *
 * <p>Lives outside {@link net.solocraft.potion.CurseMobEffect} so the effect class
 * stays a thin registration shell and the combat maths sits with the rest of the
 * Curse Mage code. Every entry point resolves the owning player first: a curse
 * with no reachable owner does nothing, because its damage, mana and credit all
 * belong to that player.
 */
public final class CurseEffectHooks {
	private CurseEffectHooks() {
	}

	/** Per-tick work for the draining curses. Vanilla only calls this for them. */
	public static void tick(LivingEntity target, CurseType curse, int amplifier) {
		if (target == null || target.level().isClientSide()
				|| !(target.level() instanceof ServerLevel level))
			return;
		Entity owner = ownerOf(level, target, curse);
		if (owner == null)
			return;
		switch (curse) {
			case WITHERING -> {
				float damage = Math.max(1.0F, target.getMaxHealth() * 0.012F);
				MageCombatHelper.hurtWithCasterInterval(level, owner, target,
						"curse_withering", damage, 30);
			}
			case MANA_ROT -> {
				if (!MageCombatHelper.hurtWithCasterInterval(level, owner, target,
						"curse_mana_rot", Math.max(1.0F, target.getMaxHealth() * 0.008F), 40))
					return;
				// The rot feeds the weaver back a little of what it corrodes. Only a
				// player has a mana pool to feed; a mob weaver simply drains.
				if (!(owner instanceof ServerPlayer weaver))
					return;
				weaver.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
						.ifPresent(data -> {
							double maximum = ManaRules.maximumManaFor(
									TemporaryStatBonusManager.effectiveIntelligence(weaver));
							data.MP = Math.min(maximum, data.MP + maximum * 0.01D);
							data.syncPlayerVariables(weaver);
						});
			}
			default -> {
			}
		}
	}

	/**
	 * Doom pays out when its timer runs out rather than while it runs.
	 *
	 * <p>This fires from the expiry dispatch, not from removal, so a Culling that
	 * consumes the mark early deliberately forfeits the detonation.
	 */
	public static void onExpired(LivingEntity target, CurseType curse) {
		if (curse != CurseType.DOOM || target == null || target.level().isClientSide()
				|| !(target.level() instanceof ServerLevel level))
			return;
		Entity owner = ownerOf(level, target, curse);
		CurseState.clearOwner(target, curse);
		if (owner == null)
			return;
		float damage = (float) (10.0D + MageCombatHelper.intelligence(owner) * 0.09D
				+ target.getMaxHealth() * 0.05D);
		MageCombatHelper.hurt(level, owner, target, damage);
		Vec3 center = target.getBoundingBox().getCenter();
		HealerVfxEntity.wave(level, center, 3, 2.4F, CurseType.DOOM.accentColor(), 20);
	}

	/**
	 * Resolves whoever applied a curse, or null when they are unreachable.
	 *
	 * <p>Weavers are not always players. Generated Curse hunters cast the same
	 * curses at the party, and resolving only through the player list left every
	 * mob-cast curse inert -- it sat on the target doing nothing at all. The
	 * player list is still tried first because a player weaver may be in another
	 * dimension, which a level-local lookup would miss.
	 */
	private static Entity ownerOf(ServerLevel level, LivingEntity target, CurseType curse) {
		UUID ownerId = CurseState.ownerOf(target, curse);
		if (ownerId == null)
			return null;
		ServerPlayer player = level.getServer().getPlayerList().getPlayer(ownerId);
		return player != null ? player : level.getEntity(ownerId);
	}
}
