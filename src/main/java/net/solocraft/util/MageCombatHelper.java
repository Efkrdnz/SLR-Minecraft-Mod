package net.solocraft.util;

import net.solocraft.network.SololevelingModVariables;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LevelAccessor;

import java.util.UUID;

public final class MageCombatHelper {
	private static final ResourceKey<net.minecraft.world.damagesource.DamageType> MAGE_DAMAGE = ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation("sololeveling:mage"));
	private static final String HIT_PREFIX = "mage_hit_";

	private MageCombatHelper() {
	}

	public static Entity resolveCaster(Entity source) {
		if (source instanceof OwnableEntity ownable && ownable.getOwner() != null)
			return ownable.getOwner();
		return source;
	}

	public static boolean isValidTarget(Entity source, Entity target) {
		Entity caster = resolveCaster(source);
		if (caster == null || target == null || target == caster || !(target instanceof LivingEntity) || !target.isAlive())
			return false;
		if (target.isAlliedTo(caster) || caster.isAlliedTo(target))
			return false;
		if (target instanceof OwnableEntity ownableTarget && ownableTarget.getOwner() != null) {
			Entity targetOwner = ownableTarget.getOwner();
			if (targetOwner == caster || targetOwner.isAlliedTo(caster) || caster.isAlliedTo(targetOwner) || sameParty(caster, targetOwner))
				return false;
		}
		if (target instanceof Player player) {
			if (player.isCreative() || player.isSpectator())
				return false;
			if (caster instanceof Player casterPlayer && !casterPlayer.canHarmPlayer(player))
				return false;
		}
		if (sameParty(caster, target))
			return false;

		UUID casterOwner = ShadowMonarchManager.getShadowOwnerUUID(caster);
		UUID targetOwner = ShadowMonarchManager.getShadowOwnerUUID(target);
		UUID effectiveCaster = casterOwner != null ? casterOwner : caster.getUUID();
		if (targetOwner != null && targetOwner.equals(effectiveCaster))
			return false;
		return casterOwner == null || !target.getUUID().equals(casterOwner);
	}

	public static boolean areAllied(Entity first, Entity second) {
		if (first == null || second == null)
			return false;
		Entity resolvedFirst = resolveCaster(first);
		Entity resolvedSecond = resolveCaster(second);
		if (resolvedFirst == null || resolvedSecond == null)
			return false;
		UUID firstOwner = ShadowMonarchManager.getShadowOwnerUUID(resolvedFirst);
		UUID secondOwner = ShadowMonarchManager.getShadowOwnerUUID(resolvedSecond);
		UUID effectiveFirst = firstOwner == null ? resolvedFirst.getUUID() : firstOwner;
		UUID effectiveSecond = secondOwner == null ? resolvedSecond.getUUID() : secondOwner;
		return resolvedFirst == resolvedSecond || resolvedFirst.isAlliedTo(resolvedSecond)
				|| effectiveFirst.equals(effectiveSecond)
				|| resolvedSecond.isAlliedTo(resolvedFirst) || sameParty(resolvedFirst, resolvedSecond);
	}

	public static boolean hurt(LevelAccessor world, Entity caster, Entity target, float amount) {
		if (amount <= 0 || !isValidTarget(caster, target))
			return false;
		Entity resolvedCaster = resolveCaster(caster);
		DamageSource source = new DamageSource(world.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(MAGE_DAMAGE), resolvedCaster);
		return target.hurt(source, amount);
	}

	public static boolean hurtWithInterval(LevelAccessor world, Entity effect, Entity caster, Entity target, float amount, int intervalTicks) {
		if (effect == null || !isValidTarget(caster, target))
			return false;
		String key = HIT_PREFIX + target.getUUID().toString().replace("-", "");
		long now = effect.level().getGameTime();
		if (effect.getPersistentData().getLong(key) > now)
			return false;
		effect.getPersistentData().putLong(key, now + Math.max(1, intervalTicks));
		return hurt(world, caster, target, amount);
	}

	public static boolean hurtWithCasterInterval(LevelAccessor world, Entity caster, Entity target, String spellId, float amount, int intervalTicks) {
		Entity resolvedCaster = resolveCaster(caster);
		if (resolvedCaster == null || !isValidTarget(resolvedCaster, target))
			return false;
		String key = HIT_PREFIX + spellId + "_" + resolvedCaster.getUUID().toString().replace("-", "");
		long now = target.level().getGameTime();
		if (target.getPersistentData().getLong(key) > now)
			return false;
		target.getPersistentData().putLong(key, now + Math.max(1, intervalTicks));
		return hurt(world, resolvedCaster, target, amount);
	}

	public static double intelligence(Entity entity) {
		Entity caster = resolveCaster(entity);
		if (caster == null)
			return 0;
		if (caster instanceof Player)
			return caster.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
					.map(data -> data.Intelligence).orElse(0.0D);
		return caster.getPersistentData().getDouble("int");
	}

	private static boolean sameParty(Entity first, Entity second) {
		String firstParty = first.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
				.map(data -> data.party).orElse("");
		String secondParty = second.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
				.map(data -> data.party).orElse("");
		return !firstParty.isBlank() && firstParty.equals(secondParty);
	}
}
