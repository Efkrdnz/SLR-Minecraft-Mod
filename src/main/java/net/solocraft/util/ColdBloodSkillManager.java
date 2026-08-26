package net.solocraft.util;

import net.solocraft.SololevelingMod;
import net.solocraft.init.SololevelingModParticleTypes;
import net.solocraft.network.SololevelingModVariables;
import net.solocraft.procedures.StealthBossDetectionHelper;

import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;

import java.util.UUID;

@EventBusSubscriber(modid = SololevelingMod.MODID)
public final class ColdBloodSkillManager {
	public static final String SKILL = "Cold Blood";

	private static final String ACTIVE_UNTIL = "sl_cold_blood_until";
	private static final String STACKS = "sl_cold_blood_stacks";
	private static final String NEXT_FX = "sl_cold_blood_next_fx";
	private static final TagKey<EntityType<?>> SOLO_BOSS_TAG = TagKey.create(Registries.ENTITY_TYPE,
			ResourceLocation.parse("soloboss"));

	private static final int MP_COST = 800;
	private static final int COOLDOWN_TICKS = 35 * 20;
	private static final int BASE_DURATION_TICKS = 12 * 20;
	private static final int MAX_DURATION_TICKS = 22 * 20;
	private static final int KILL_EXTENSION_TICKS = 4 * 20;
	private static final int COOLDOWN_REFUND_TICKS = 4 * 20;
	private static final int MIN_REMAINING_COOLDOWN_TICKS = 8 * 20;
	private static final int MAX_STACKS = 5;

	private ColdBloodSkillManager() {
	}

	public static void cast(Entity entity) {
		if (!(entity instanceof ServerPlayer player))
			return;
		SololevelingModVariables.PlayerVariables vars = variables(player);
		if (vars.MP < MP_COST) {
			player.displayClientMessage(Component.literal("You dont have enough MP"), true);
			return;
		}
		if (CooldownManager.isOnCooldown(player, SKILL)) {
			player.displayClientMessage(Component.literal("Ability on cooldown! "
					+ CooldownManager.getRemainingSeconds(player, SKILL) + "s"), true);
			return;
		}

		player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
			capability.MP = Math.max(0.0D, capability.MP - MP_COST);
			capability.syncPlayerVariables(player);
		});
		long now = player.level().getGameTime();
		CompoundTag data = player.getPersistentData();
		data.putLong(ACTIVE_UNTIL, now + BASE_DURATION_TICKS);
		data.putInt(STACKS, 0);
		data.putLong(NEXT_FX, 0L);
		CooldownManager.set(player, SKILL, COOLDOWN_TICKS);
		CooldownManager.set(player, "mana_refresh", 60);
		applyBuffs(player);
		emitCasterParticles(player, 36);
		player.displayClientMessage(Component.literal("Using Cold Blood").withStyle(ChatFormatting.DARK_RED), true);
	}

	public static boolean isActive(Entity entity) {
		return entity != null && entity.getPersistentData().getLong(ACTIVE_UNTIL) > entity.level().getGameTime();
	}

	@SubscribeEvent
	public static void onPlayerTick(PlayerTickEvent.Post event) {
		if (false || !(event.getEntity() instanceof ServerPlayer player))
			return;
		CompoundTag data = player.getPersistentData();
		if (data.getLong(ACTIVE_UNTIL) <= 0L)
			return;
		long now = player.level().getGameTime();
		if (!player.isAlive() || data.getLong(ACTIVE_UNTIL) <= now) {
			clear(player);
			return;
		}
		if (player.tickCount % 10 == 0)
			applyBuffs(player);
		if (data.getLong(NEXT_FX) <= now) {
			data.putLong(NEXT_FX, now + 8L);
			emitCasterParticles(player, 8 + stacks(player) * 2);
		}
	}

	@SubscribeEvent(priority = EventPriority.LOW)
	public static void onLivingHurt(LivingIncomingDamageEvent event) {
		if (event.getAmount() <= 0.0F || event.getEntity().level().isClientSide())
			return;
		ServerPlayer attacker = owningPlayer(event.getSource().getEntity());
		if (attacker == null)
			attacker = owningPlayer(event.getSource().getDirectEntity());
		if (attacker == null || !isActive(attacker))
			return;
		LivingEntity target = event.getEntity();
		if (target == attacker || !MageCombatHelper.isValidTarget(attacker, target))
			return;

		int stacks = stacks(attacker);
		boolean playerTarget = target instanceof Player;
		boolean boss = isBoss(target);
		float multiplier = damageMultiplier(stacks, boss, playerTarget);
		float boosted = event.getAmount() * multiplier;
		if (boss && target.getHealth() <= target.getMaxHealth() * 0.15F) {
			float finisher = Math.min(32.0F, Math.max(4.0F, target.getMaxHealth() * 0.04F));
			boosted += finisher;
		} else if (!boss && !playerTarget && target.getHealth() - boosted <= target.getMaxHealth() * 0.25F) {
			target.invulnerableTime = 0;
			boosted = Math.max(boosted, target.getHealth() + Math.max(1.0F, target.getMaxHealth() * 0.05F));
		}
		event.setAmount(boosted);
		emitTargetParticles(attacker.serverLevel(), target, boss ? 10 : 14);
	}

	@SubscribeEvent
	public static void onLivingDeath(LivingDeathEvent event) {
		if (event.getEntity().level().isClientSide() || event.getEntity() instanceof Player)
			return;
		ServerPlayer player = owningPlayer(event.getSource().getEntity());
		if (player == null)
			player = owningPlayer(event.getSource().getDirectEntity());
		if (player == null || !isActive(player) || MageCombatHelper.areAllied(player, event.getEntity()))
			return;

		CompoundTag data = player.getPersistentData();
		long now = player.level().getGameTime();
		data.putInt(STACKS, Math.min(MAX_STACKS, data.getInt(STACKS) + 1));
		long currentUntil = Math.max(data.getLong(ACTIVE_UNTIL), now);
		data.putLong(ACTIVE_UNTIL, Math.min(now + MAX_DURATION_TICKS, currentUntil + KILL_EXTENSION_TICKS));
		restoreResources(player);
		reduceCooldown(player);
		emitCasterParticles(player, 24);
		player.displayClientMessage(Component.literal("Cold Blood x" + data.getInt(STACKS))
				.withStyle(ChatFormatting.RED, ChatFormatting.BOLD), true);
	}

	private static void applyBuffs(ServerPlayer player) {
		player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 30, 0, false, true));
		player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 30, 0, false, true));
	}

	private static void restoreResources(ServerPlayer player) {
		player.heal(Math.min(4.0F, Math.max(2.0F, player.getMaxHealth() * 0.06F)));
		player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
			capability.MP = Math.min(capability.Mana, capability.MP + Math.max(120.0D, capability.Mana * 0.04D));
			capability.syncPlayerVariables(player);
		});
	}

	private static void reduceCooldown(ServerPlayer player) {
		int remaining = CooldownManager.getRemainingTicks(player, SKILL);
		if (remaining <= MIN_REMAINING_COOLDOWN_TICKS)
			return;
		CooldownManager.set(player, SKILL,
				Math.max(MIN_REMAINING_COOLDOWN_TICKS, remaining - COOLDOWN_REFUND_TICKS));
	}

	private static float damageMultiplier(int stacks, boolean boss, boolean playerTarget) {
		if (playerTarget)
			return 1.08F + stacks * 0.02F;
		if (boss)
			return 1.15F + stacks * 0.03F;
		return 1.25F + stacks * 0.05F;
	}

	private static int stacks(ServerPlayer player) {
		return Mth.clamp(player.getPersistentData().getInt(STACKS), 0, MAX_STACKS);
	}

	private static boolean isBoss(LivingEntity target) {
		return target.getType().is(SOLO_BOSS_TAG) || StealthBossDetectionHelper.seesThroughStealth(target);
	}

	private static void clear(ServerPlayer player) {
		CompoundTag data = player.getPersistentData();
		data.remove(ACTIVE_UNTIL);
		data.remove(STACKS);
		data.remove(NEXT_FX);
	}

	private static void emitCasterParticles(ServerPlayer player, int count) {
		ServerLevel level = player.serverLevel();
		level.sendParticles((SimpleParticleType) SololevelingModParticleTypes.BLOOD_PARTICLE.get(), player.getX(),
				player.getY() + 1.0D, player.getZ(), count, 0.55D, 0.7D, 0.55D, 0.08D);
		level.sendParticles((SimpleParticleType) SololevelingModParticleTypes.RED_DUST_PARTICLE.get(), player.getX(),
				player.getY() + 1.0D, player.getZ(), Math.max(4, count / 3), 0.45D, 0.6D, 0.45D, 0.02D);
	}

	private static void emitTargetParticles(ServerLevel level, LivingEntity target, int count) {
		level.sendParticles((SimpleParticleType) SololevelingModParticleTypes.BLOOD_PARTICLE.get(), target.getX(),
				target.getY() + target.getBbHeight() * 0.6D, target.getZ(), count, target.getBbWidth() * 0.35D,
				target.getBbHeight() * 0.25D, target.getBbWidth() * 0.35D, 0.12D);
	}

	private static SololevelingModVariables.PlayerVariables variables(Entity entity) {
		return entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
				.orElse(new SololevelingModVariables.PlayerVariables());
	}

	private static ServerPlayer owningPlayer(Entity source) {
		if (source instanceof ServerPlayer player)
			return player;
		if (source instanceof Projectile projectile)
			return owningPlayer(projectile.getOwner());
		if (source instanceof TamableAnimal tame && tame.getOwner() instanceof ServerPlayer owner)
			return owner;
		if (source != null && source.getServer() != null) {
			UUID owner = ShadowMonarchManager.getShadowOwnerUUID(source);
			if (owner != null)
				return source.getServer().getPlayerList().getPlayer(owner);
		}
		return null;
	}
}
