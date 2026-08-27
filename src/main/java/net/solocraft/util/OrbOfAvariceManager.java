package net.solocraft.util;

import net.solocraft.SololevelingMod;
import net.solocraft.init.SololevelingModItems;

import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;

/** Shared held-item checks and server-side amplification for the Orb of Avarice. */
@EventBusSubscriber(modid = SololevelingMod.MODID)
public final class OrbOfAvariceManager {
	public static final int BLUE_FIRE_PRIMARY = 0x174DFF;
	public static final int BLUE_FIRE_SECONDARY = 0x69E7FF;
	public static final double MANA_COST_MULTIPLIER = 1.5D;

	private static final ResourceKey<DamageType> MAGE_DAMAGE = ResourceKey.create(
			Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(SololevelingMod.MODID, "mage"));

	private OrbOfAvariceManager() {
	}

	public static boolean isHeldBy(Entity entity) {
		Entity caster = MageCombatHelper.resolveCaster(entity);
		return caster instanceof Player player
				&& (player.getMainHandItem().is(SololevelingModItems.ORB_OF_AVARICE.get())
						|| player.getOffhandItem().is(SololevelingModItems.ORB_OF_AVARICE.get()));
	}

	public static double manaCostMultiplier(Entity entity) {
		return isHeldBy(entity) ? MANA_COST_MULTIPLIER : 1.0D;
	}

	public static int adjustManaCost(Entity entity, double baseCost) {
		return Math.max(0, (int) Math.ceil(Math.max(0.0D, baseCost) * manaCostMultiplier(entity)));
	}

	@SubscribeEvent(priority = EventPriority.HIGH)
	public static void amplifyMagicDamage(LivingIncomingDamageEvent event) {
		if (event.getEntity().level().isClientSide() || event.getAmount() <= 0.0F
				|| !isMagic(event.getSource()))
			return;
		if (ShadowEquipmentCombatHandler.isOrbAmplifiedTuskDamage(event.getSource()))
			return;
		Entity caster = resolveDamageCaster(event.getSource());
		if (isHeldBy(caster))
			event.setAmount(event.getAmount() * 2.0F);
	}

	private static boolean isMagic(DamageSource source) {
		return source.is(MAGE_DAMAGE) || source.is(DamageTypes.MAGIC)
				|| source.is(DamageTypes.INDIRECT_MAGIC);
	}

	private static Entity resolveDamageCaster(DamageSource source) {
		Entity caster = source.getEntity();
		if (caster instanceof Projectile projectile && projectile.getOwner() != null)
			caster = projectile.getOwner();
		if (caster == null && source.getDirectEntity() instanceof Projectile projectile
				&& projectile.getOwner() != null)
			caster = projectile.getOwner();
		return MageCombatHelper.resolveCaster(caster);
	}
}
