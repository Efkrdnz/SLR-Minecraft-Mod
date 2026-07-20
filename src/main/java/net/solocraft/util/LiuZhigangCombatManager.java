package net.solocraft.util;

import net.solocraft.SololevelingMod;
import net.solocraft.entity.LiuSwordBeamEntity;
import net.solocraft.entity.LiuSwordVfxEntity;
import net.solocraft.init.SololevelingModItems;
import net.solocraft.network.LiuExecutionImpactMessage;
import net.solocraft.network.SololevelingModVariables;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Mod.EventBusSubscriber
public final class LiuZhigangCombatManager {
	public static final String HEAVENLY_COUNTER = "Heavenly Counter";
	public static final String GOLDEN_DRAGON_DANCE = "Golden Dragon Dance";
	public static final String SOVEREIGN_SWORD_DOMAIN = "Sovereign Sword Domain";
	public static final String DRAGON_SWORD_MANIFESTATION = "Dragon Sword Manifestation";

	public static final int GOLD = 0xFFD34E;
	public static final int PALE_GOLD = 0xFFF3B0;
	public static final int DEMON_BLUE = 0xA8E9FF;
	public static final int KAMISH_RED = 0xFF3048;
	public static final long BEAM_TIER_ONE_TICKS = 17L;
	public static final long BEAM_TIER_TWO_TICKS = 34L;
	public static final long BEAM_TIER_THREE_TICKS = BEAM_TIER_TWO_TICKS + 200L;
	private static final long MAX_BEAM_CHARGE_TICKS = 600L;

	private static final String IDENTITY = "liu_zhigang";
	private static final String BEAM_COOLDOWN = "liu_charged_sword_beam";
	private static final String FLASH_COOLDOWN = "liu_dragon_flash";
	private static final String NEXT_STRIKE = "liu_next_enhanced_strike";
	private static final String FALL_SAFE_UNTIL = "liu_fall_safe_until";
	private static final String COUNTER_UNTIL = "liu_counter_until";
	private static final String COUNTER_CHARGES = "liu_counter_charges";
	private static final String COUNTER_SURGE_UNTIL = "liu_counter_surge_until";
	private static final String DOMAIN_MARK_OWNER = "liu_domain_mark_owner";
	private static final String DOMAIN_MARK_UNTIL = "liu_domain_mark_until";
	private static final String DOMAIN_MARK_STACKS = "liu_domain_mark_stacks";
	private static final String EXECUTION_SLOW_OWNER = "liu_execution_slow_owner";
	private static final String EXECUTION_SLOW_UNTIL = "liu_execution_slow_until";
	private static final int EXECUTION_DELAY_TICKS = 50;
	private static final int EXECUTION_IMPACT_TICKS = 16;
	private static final int EXECUTION_FIRE_RED = 0xE23818;
	private static final int EXECUTION_FIRE_YELLOW = 0xFFD84A;
	private static final int EXECUTION_PARTICLE_TARGET_LIMIT = 10;
	private static final double EXECUTION_PARTICLE_RANGE_SQR = 384.0D * 384.0D;
	private static final int EXECUTION_MAX_LINK_NODES = 48;
	private static final int EXECUTION_MAX_LINKS = 24;
	private static final double EXECUTION_LINK_RANGE_SQR = 20.0D * 20.0D;

	private static final TagKey<net.minecraft.world.item.Item> NORMAL_SWORDS = TagKey.create(Registries.ITEM, new ResourceLocation("minecraft", "nsword"));
	private static final TagKey<net.minecraft.world.item.Item> DAGGERS = TagKey.create(Registries.ITEM, new ResourceLocation("minecraft", "dagger"));

	private static final Map<UUID, BeamChargeState> BEAM_CHARGES = new HashMap<>();
	private static final Map<UUID, FlashChargeState> FLASH_CHARGES = new HashMap<>();
	private static final Map<UUID, FlashDashState> FLASH_DASHES = new HashMap<>();
	private static final Map<UUID, DanceState> DANCES = new HashMap<>();
	private static final Map<UUID, DomainState> DOMAINS = new HashMap<>();
	private static final Map<UUID, List<ExecutionState>> EXECUTIONS = new HashMap<>();

	private LiuZhigangCombatManager() {
	}

	public static boolean isLiuVessel(Entity entity) {
		if (entity == null)
			return false;
		SololevelingModVariables.PlayerVariables vars = variables(entity);
		return (int) vars.JOB == 6 && (IDENTITY.equals(vars.vesselIdentity) || vars.vesselIdentity.isBlank());
	}

	public static boolean isCombatStance(Entity entity) {
		return isLiuVessel(entity) && variables(entity).combatmode;
	}

	public static boolean isManifested(Entity entity) {
		return LiuManifestationManager.isActive(entity);
	}

	public static boolean isMeleeWeapon(ItemStack stack) {
		return !stack.isEmpty() && (stack.getItem() instanceof SwordItem || stack.getItem() instanceof AxeItem
				|| stack.getItem() instanceof TridentItem || stack.is(NORMAL_SWORDS) || stack.is(DAGGERS));
	}

	public static int colorFor(ItemStack stack) {
		if (stack.is(SololevelingModItems.DEMON_KINGS_LONG_SWORD.get()))
			return DEMON_BLUE;
		if (stack.is(SololevelingModItems.KAMISH_WRATH.get()) || stack.is(SololevelingModItems.KAMISH_WRATH_2.get()))
			return KAMISH_RED;
		return GOLD;
	}

	public static int beamChargeTier(long heldTicks) {
		return heldTicks >= BEAM_TIER_THREE_TICKS ? 3
				: heldTicks >= BEAM_TIER_TWO_TICKS ? 2
				: heldTicks >= BEAM_TIER_ONE_TICKS ? 1 : 0;
	}

	public static void beginBeamCharge(ServerPlayer player) {
		if (!isCombatStance(player) || !player.isAlive() || BEAM_CHARGES.containsKey(player.getUUID())
				)
			return;
		ItemStack main = player.getMainHandItem();
		ItemStack off = player.getOffhandItem();
		if (!isMeleeWeapon(main) && !isMeleeWeapon(off))
			return;
		boolean dual = isMeleeWeapon(main) && isMeleeWeapon(off);
		if (!isMeleeWeapon(main))
			main = off;
		int mainColor = colorFor(main);
		int offColor = dual ? colorFor(off) : mainColor;
		LiuSwordVfxEntity charge = LiuSwordVfxEntity.spawnAttached(player.serverLevel(), player,
				LiuSwordVfxEntity.CHARGE, mainColor, offColor, dual ? 1.5F : 1.15F,
				dual ? 2.8F : 2.2F, 0.0F, (int) MAX_BEAM_CHARGE_TICKS + 20, dual);
		BEAM_CHARGES.put(player.getUUID(), new BeamChargeState(player.level().getGameTime(),
				itemSignature(main), itemSignature(off), mainColor, offColor, dual,
				(float) ((weaponPower(main) + (dual ? weaponPower(off) : 0.0D)) / (dual ? 2.0D : 1.0D)),
				charge.getUUID()));
		player.level().playSound(null, player.blockPosition(), SoundEvents.BEACON_POWER_SELECT,
				SoundSource.PLAYERS, 0.52F, 1.72F);
	}

	public static void releaseBeamCharge(ServerPlayer player) {
		BeamChargeState state = BEAM_CHARGES.remove(player.getUUID());
		if (state == null)
			return;
		removeVfx(player.serverLevel(), state.effectId);
		if (!isCombatStance(player) || !player.isAlive())
			return;
		if (!sameHeldWeapons(player, state))
			return;

		long heldTicks = Math.max(1L, Math.min(MAX_BEAM_CHARGE_TICKS,
				player.level().getGameTime() - state.startedAt));
		if (heldTicks < BEAM_TIER_ONE_TICKS && CooldownManager.isOnCooldown(player, BEAM_COOLDOWN)) {
			player.displayClientMessage(Component.literal("Sword beam is still recovering")
					.withStyle(ChatFormatting.GOLD), true);
			return;
		}
		int tier = beamChargeTier(heldTicks);
		if (player.getPersistentData().getLong(COUNTER_SURGE_UNTIL) >= player.level().getGameTime()) {
			// Counter surge can accelerate the ordinary tiers, but the execution tier
			// always requires its full ten-second final charge interval.
			if (tier < 2)
				tier++;
			player.getPersistentData().remove(COUNTER_SURGE_UNTIL);
		}
		int mana = switch (tier) {
			case 1 -> 190;
			case 2 -> 450;
			case 3 -> 1100;
			default -> 55 + (int) Math.min(85L, heldTicks * 5L);
		};
		double manaScaling = switch (tier) {
			case 1 -> 0.75D;
			case 2 -> 1.0D;
			case 3 -> 1.35D;
			default -> 0.55D;
		};
		mana = VesselManaScaling.strengthScaledCost(player, mana, manaScaling);
		if (state.dual)
			mana = (int) Math.ceil(mana * 1.18D);
		if (!consumeMana(player, mana))
			return;

		float quality = Mth.clamp((state.weaponPower - 5.0F) / 27.0F, 0.0F, 0.85F);
		float partial = Mth.clamp(heldTicks / (float) BEAM_TIER_ONE_TICKS, 0.3F, 1.0F);
		float width = switch (tier) {
			case 1 -> 5.8F;
			case 2 -> 8.5F;
			case 3 -> 27.0F;
			default -> 2.6F + 2.1F * partial;
		};
		float range = switch (tier) {
			case 1 -> 29.0F;
			case 2 -> 43.0F;
			case 3 -> 180.0F;
			default -> 12.0F + 10.0F * partial;
		};
		width *= 1.0F + quality * 0.24F + (state.dual ? 0.18F : 0.0F);
		range *= 1.0F + quality * 0.18F;
		float multiplier = switch (tier) {
			case 1 -> 1.0F;
			case 2 -> 1.75F;
			case 3 -> 3.5F;
			default -> 0.42F + partial * 0.3F;
		};
		float damage = swordSkillDamage(player, state.weaponPower,
				multiplier * (state.dual ? 1.1D : 1.0D));
		Vec3 look = player.getLookAngle().normalize();
		Vec3 origin = player.getEyePosition().add(look.scale(1.9D)).add(0.0D, -0.22D, 0.0D);
		LiuSwordBeamEntity.spawn(player.serverLevel(), player, origin, look, tier, state.dual,
				state.primaryColor, state.secondaryColor, width, range, tier == 3 ? 5.2F : 3.35F, damage);
		CooldownManager.set(player, BEAM_COOLDOWN, switch (tier) {
			case 2 -> 30;
			case 3 -> 55;
			default -> 13;
		});
		player.level().playSound(null, player.blockPosition(), tier == 3 ? SoundEvents.WARDEN_SONIC_BOOM : SoundEvents.TRIDENT_THROW,
				SoundSource.PLAYERS, tier == 3 ? 1.2F : 0.82F, tier == 3 ? 0.72F : 1.15F + tier * 0.14F);
	}

	public static void cancelBeamCharge(ServerPlayer player) {
		BeamChargeState state = BEAM_CHARGES.remove(player.getUUID());
		if (state != null)
			removeVfx(player.serverLevel(), state.effectId);
	}

	public static void enhancedAttack(ServerPlayer player, boolean offhand, int comboIndex) {
		if (!isCombatStance(player) || !player.isAlive())
			return;
		long now = player.level().getGameTime();
		if (player.getPersistentData().getLong(NEXT_STRIKE) > now)
			return;
		ItemStack stack = offhand ? player.getOffhandItem() : player.getMainHandItem();
		if (!isMeleeWeapon(stack))
			return;
		boolean finisher = Math.floorMod(comboIndex, 4) == 3;
		int mana = VesselManaScaling.strengthScaledCost(player, finisher ? 42 : 26, 0.45D);
		if (!consumeManaSilently(player, mana))
			return;
		player.getPersistentData().putLong(NEXT_STRIKE, now + 2L);
		int color = colorFor(stack);
		int otherColor = colorFor(offhand ? player.getMainHandItem() : player.getOffhandItem());
		boolean dual = isMeleeWeapon(player.getMainHandItem()) && isMeleeWeapon(player.getOffhandItem());
		float roll = switch (Math.floorMod(comboIndex, 4)) {
			case 0 -> -26.0F;
			case 1 -> 24.0F;
			case 2 -> -8.0F;
			default -> 18.0F;
		};
		if (offhand)
			roll = -roll;
		final float strikeRoll = roll;
		Vec3 look = player.getLookAngle().normalize();
		Vec3 origin = player.getEyePosition().add(look.scale(3.0D)).add(0.0D, -0.28D, 0.0D);
		LiuSwordVfxEntity.spawnMoving(player.serverLevel(), origin, look, LiuSwordVfxEntity.ARC,
				color, dual ? otherColor : color, finisher ? 4.9F : 3.55F, finisher ? 8.2F : 6.2F,
				strikeRoll, finisher ? 8 : 6, finisher && dual, finisher ? 1.38F : 1.12F);

		float damage = swordSkillDamage(player, stack, finisher ? 0.78D : 0.5D);
		SololevelingMod.queueServerWork(1, () -> {
			if (!player.isAlive() || !isCombatStance(player))
				return;
			for (LivingEntity target : targetsInSlash(player, player.getEyePosition(), look,
						finisher ? 8.5D : 6.3D, finisher ? 3.2D : 2.0D, finisher ? 8 : 4)) {
				if (dealSwordDamage(player, target, damage)) {
					markForDomain(player, target, 1);
					push(target, look, finisher ? 0.75D : 0.35D, 0.08D);
					if (isDomainActive(player)) {
						SololevelingMod.queueServerWork(4, () -> {
							if (isValidTarget(player, target)) {
								LiuSwordVfxEntity.spawnAttached(player.serverLevel(), target, LiuSwordVfxEntity.ARC,
										color, color, 2.1F, 3.4F, -strikeRoll * 0.7F, 8, false);
								dealSwordDamage(player, target, damage * 0.42F);
							}
						});
					}
				}
			}
		});
	}

	public static void beginDragonFlash(Entity entity) {
		if (!(entity instanceof ServerPlayer player) || !isCombatStance(player)
				|| FLASH_CHARGES.containsKey(player.getUUID()) || FLASH_DASHES.containsKey(player.getUUID())
				|| CooldownManager.isOnCooldown(player, FLASH_COOLDOWN))
			return;
		LivingEntity target = crosshairTarget(player, 52.0D);
		UUID marker = null;
		if (target != null) {
			LiuSwordVfxEntity effect = LiuSwordVfxEntity.spawnAttached(player.serverLevel(), target,
					LiuSwordVfxEntity.TARGET, primaryHandColor(player), secondaryHandColor(player),
					Math.max(1.4F, target.getBbWidth() * 1.35F), Math.max(2.1F, target.getBbHeight()),
					0.0F, 80, isDualWielding(player));
			marker = effect.getUUID();
		}
		FLASH_CHARGES.put(player.getUUID(), new FlashChargeState(target == null ? null : target.getUUID(),
				player.level().getGameTime(), marker));
		player.level().playSound(null, player.blockPosition(), SoundEvents.TRIDENT_RETURN,
				SoundSource.PLAYERS, 0.55F, target == null ? 1.1F : 1.55F);
	}

	public static void releaseDragonFlash(Entity entity, int pressedMs) {
		if (!(entity instanceof ServerPlayer player) || !isCombatStance(player))
			return;
		FlashChargeState charge = FLASH_CHARGES.remove(player.getUUID());
		if (charge == null || CooldownManager.isOnCooldown(player, FLASH_COOLDOWN))
			return;
		if (charge.markerId != null)
			removeVfx(player.serverLevel(), charge.markerId);
		LivingEntity target = charge.targetId == null ? null : living(player.serverLevel(), charge.targetId);
		if (target != null && (!isValidTarget(player, target) || player.distanceToSqr(target) > 60.0D * 60.0D))
			target = null;
		boolean manifested = isManifested(player);
		int mana = target == null ? (manifested ? 210 : 150) : (manifested ? 360 : 265);
		mana = VesselManaScaling.strengthScaledCost(player, mana, 0.65D);
		if (!consumeMana(player, mana))
			return;
		Vec3 direction = target == null ? player.getLookAngle().normalize()
				: target.getBoundingBox().getCenter().subtract(player.getBoundingBox().getCenter()).normalize();
		double power = Mth.clamp(0.78D + pressedMs / 1500.0D, 0.8D, 1.32D);
		FLASH_DASHES.put(player.getUUID(), new FlashDashState(target == null ? null : target.getUUID(),
				direction, player.level().getGameTime(), target == null ? 18 : 24, power,
				primaryHandColor(player), secondaryHandColor(player), isDualWielding(player), new HashSet<>()));
		CooldownManager.set(player, FLASH_COOLDOWN, manifested ? 58 : 72);
		player.getPersistentData().putLong(FALL_SAFE_UNTIL, player.level().getGameTime() + 80L);
		player.setDeltaMovement(direction.scale((manifested ? 2.85D : 2.45D) * power));
		player.hurtMarked = true;
	}

	public static void castHeavenlyCounter(Entity entity) {
		if (!(entity instanceof ServerPlayer player) || !isLiuVessel(player) || !ready(player, HEAVENLY_COUNTER))
			return;
		boolean manifested = isManifested(player);
		int mana = VesselManaScaling.strengthScaledCost(player, manifested ? 360 : 250, 0.5D);
		if (!consumeMana(player, mana))
			return;
		player.getPersistentData().putLong(COUNTER_UNTIL, player.level().getGameTime() + (manifested ? 34L : 24L));
		player.getPersistentData().putInt(COUNTER_CHARGES, manifested ? 2 : 1);
		CooldownManager.set(player, HEAVENLY_COUNTER, manifested ? 105 : 125);
		LiuSwordVfxEntity.spawnAttached(player.serverLevel(), player, LiuSwordVfxEntity.COUNTER,
				primaryHandColor(player), secondaryHandColor(player), manifested ? 2.5F : 2.0F,
				manifested ? 4.1F : 3.3F, 0.0F, manifested ? 34 : 24, isDualWielding(player));
		player.level().playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME,
				SoundSource.PLAYERS, 0.9F, 1.42F);
	}

	public static void castGoldenDragonDance(Entity entity) {
		if (!(entity instanceof ServerPlayer player) || !isLiuVessel(player) || !ready(player, GOLDEN_DRAGON_DANCE)
				|| DANCES.containsKey(player.getUUID()))
			return;
		boolean manifested = isManifested(player);
		int mana = VesselManaScaling.strengthScaledCost(player, manifested ? 760 : 540, 0.8D);
		if (!consumeMana(player, mana))
			return;
		double radius = manifested ? 27.0D : 20.0D;
		List<LivingEntity> found = targets(player, player.getBoundingBox().inflate(radius, radius * 0.5D, radius));
		found.removeIf(target -> !player.hasLineOfSight(target));
		found.sort(Comparator.comparingDouble(player::distanceToSqr));
		int cap = manifested ? 10 : 6;
		List<UUID> ids = found.stream().limit(cap).map(Entity::getUUID).toList();
		CooldownManager.set(player, GOLDEN_DRAGON_DANCE, manifested ? 155 : 185);
		if (ids.isEmpty()) {
			spawnDanceWaves(player, manifested);
			return;
		}
		DANCES.put(player.getUUID(), new DanceState(new ArrayList<>(ids), 0, 0,
				player.level().getGameTime(), manifested, primaryHandColor(player), secondaryHandColor(player)));
		player.getPersistentData().putLong(FALL_SAFE_UNTIL, player.level().getGameTime() + 100L);
		player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP,
				SoundSource.PLAYERS, 1.0F, 1.55F);
	}

	public static void castSovereignSwordDomain(Entity entity) {
		if (!(entity instanceof ServerPlayer player) || !isLiuVessel(player) || !ready(player, SOVEREIGN_SWORD_DOMAIN))
			return;
		boolean manifested = isManifested(player);
		int mana = VesselManaScaling.strengthScaledCost(player, manifested ? 1420 : 1050, 1.0D);
		if (!consumeMana(player, mana))
			return;
		DomainState old = DOMAINS.remove(player.getUUID());
		if (old != null)
			finishDomain(player, old);
		long now = player.level().getGameTime();
		int duration = manifested ? 280 : 220;
		DOMAINS.put(player.getUUID(), new DomainState(now + duration, now, now, manifested));
		CooldownManager.set(player, SOVEREIGN_SWORD_DOMAIN, manifested ? 520 : 600);
		LiuSwordVfxEntity.spawnAttached(player.serverLevel(), player, LiuSwordVfxEntity.DOMAIN,
				GOLD, PALE_GOLD, manifested ? 29.0F : 22.0F, manifested ? 11.0F : 8.5F,
				0.0F, duration, true);
		player.level().playSound(null, player.blockPosition(), SoundEvents.BEACON_ACTIVATE,
				SoundSource.PLAYERS, 1.2F, 0.78F);
	}

	public static void toggleDragonSwordManifestation(Entity entity) {
		if (!(entity instanceof ServerPlayer player) || !isLiuVessel(player))
			return;
		if (isManifested(player)) {
			LiuManifestationManager.restore(player);
			LiuSwordVfxEntity.spawnAttached(player.serverLevel(), player, LiuSwordVfxEntity.DETONATION,
					GOLD, PALE_GOLD, 3.1F, 5.2F, 18.0F, 15, true);
			player.level().playSound(null, player.blockPosition(), SoundEvents.BEACON_DEACTIVATE,
					SoundSource.PLAYERS, 0.72F, 1.45F);
			return;
		}
		if (!ready(player, DRAGON_SWORD_MANIFESTATION) || !consumeMana(player, 780))
			return;
		LiuManifestationManager.toggle(player);
		CooldownManager.set(player, DRAGON_SWORD_MANIFESTATION, 45);
		LiuSwordVfxEntity.spawnAttached(player.serverLevel(), player, LiuSwordVfxEntity.CHARGE,
				GOLD, PALE_GOLD, 3.8F, 6.2F, 0.0F, 32, true);
		LiuSwordVfxEntity.spawnAttached(player.serverLevel(), player, LiuSwordVfxEntity.DETONATION,
				GOLD, PALE_GOLD, 4.5F, 7.0F, 18.0F, 20, true);
		player.level().playSound(null, player.blockPosition(), SoundEvents.TRIDENT_THUNDER,
				SoundSource.PLAYERS, 1.0F, 1.72F);
	}

	public static boolean hitBySwordBeam(ServerPlayer owner, LivingEntity target, float damage, int tier) {
		if (!dealSwordDamage(owner, target, damage))
			return false;
		markForDomain(owner, target, Math.max(1, tier));
		Vec3 direction = target.position().subtract(owner.position());
		if (direction.lengthSqr() > 0.001D)
			push(target, direction.normalize(), 0.25D + tier * 0.18D, 0.08D + tier * 0.03D);
		return true;
	}

	public static void detonateExecutionMarks(ServerPlayer owner, Set<UUID> ids, float damage,
			int primaryColor, int secondaryColor, boolean dual) {
		List<LivingEntity> victims = new ArrayList<>();
		for (UUID id : ids) {
			LivingEntity target = living(owner.serverLevel(), id);
			if (isValidTarget(owner, target))
				victims.add(target);
		}
		victims.sort(Comparator.comparingDouble(owner::distanceToSqr));
		spawnExecutionDetonationEffects(owner, victims);
		for (LivingEntity target : victims) {
			dealSwordDamage(owner, target, damage);
			markForDomain(owner, target, 3);
		}
		for (UUID id : ids) {
			LivingEntity target = living(owner.serverLevel(), id);
			if (target != null)
				releaseExecutionRestraint(owner, target);
		}
		if (!victims.isEmpty())
			owner.level().playSound(null, BlockPos.containing(victims.get(0).position()), SoundEvents.GENERIC_EXPLODE,
					SoundSource.PLAYERS, 1.5F, 0.68F);
	}

	private static void spawnExecutionDetonationEffects(ServerPlayer owner, List<LivingEntity> victims) {
		ServerLevel level = owner.serverLevel();
		List<Vec3> centers = new ArrayList<>(victims.size());
		for (int index = 0; index < victims.size(); index++) {
			LivingEntity target = victims.get(index);
			Vec3 center = target.getBoundingBox().getCenter();
			centers.add(center);
			float blastWidth = Mth.clamp(Math.max(4.5F, target.getBbWidth() * 2.15F), 4.5F, 10.5F);
			float blastHeight = Mth.clamp(Math.max(4.8F, target.getBbHeight() * 1.65F), 4.8F, 12.0F);
			LiuSwordVfxEntity.spawn(level, center.x, center.y, center.z,
					LiuSwordVfxEntity.EXECUTION_EXPLOSION, EXECUTION_FIRE_RED, EXECUTION_FIRE_YELLOW,
					blastWidth, blastHeight, owner.getRandom().nextFloat() * 360.0F, 22,
					0.0F, 0.0F, false);
			if (index < EXECUTION_PARTICLE_TARGET_LIMIT) {
				double spread = Math.min(1.2D, blastWidth * 0.14D);
				sendExecutionParticles(level, owner, center, ParticleTypes.EXPLOSION,
						2, spread * 0.45D, spread * 0.45D, spread * 0.45D, 0.02D);
				sendExecutionParticles(level, owner, center, ParticleTypes.FLAME,
						8, spread, spread * 0.8D, spread, 0.12D);
				sendExecutionParticles(level, owner, center, ParticleTypes.LAVA,
						3, spread * 0.55D, spread * 0.45D, spread * 0.55D, 0.05D);
			}
		}
		spawnExecutionLinks(level, centers);
	}

	private static void sendExecutionParticles(ServerLevel level, ServerPlayer owner, Vec3 center,
			ParticleOptions particle, int count, double xSpread, double ySpread, double zSpread,
			double speed) {
		for (ServerPlayer viewer : level.players()) {
			if (viewer != owner && viewer.position().distanceToSqr(center) > EXECUTION_PARTICLE_RANGE_SQR)
				continue;
			level.sendParticles(viewer, particle, true, center.x, center.y, center.z,
					count, xSpread, ySpread, zSpread, speed);
		}
	}

	private static void spawnExecutionLinks(ServerLevel level, List<Vec3> centers) {
		int nodeCount = Math.min(EXECUTION_MAX_LINK_NODES, centers.size());
		if (nodeCount < 2)
			return;
		List<ExecutionLink> candidates = new ArrayList<>();
		for (int first = 0; first < nodeCount; first++) {
			for (int second = first + 1; second < nodeCount; second++) {
				double distanceSqr = centers.get(first).distanceToSqr(centers.get(second));
				if (distanceSqr >= 0.25D && distanceSqr <= EXECUTION_LINK_RANGE_SQR)
					candidates.add(new ExecutionLink(first, second, distanceSqr));
			}
		}
		candidates.sort(Comparator.comparingDouble(ExecutionLink::distanceSqr));
		int[] parent = new int[nodeCount];
		for (int index = 0; index < nodeCount; index++)
			parent[index] = index;
		int spawned = 0;
		for (ExecutionLink candidate : candidates) {
			int firstRoot = executionRoot(parent, candidate.first());
			int secondRoot = executionRoot(parent, candidate.second());
			if (firstRoot == secondRoot)
				continue;
			parent[secondRoot] = firstRoot;
			Vec3 start = centers.get(candidate.first());
			Vec3 end = centers.get(candidate.second());
			float distance = (float) Math.sqrt(candidate.distanceSqr());
			float width = Mth.clamp(0.22F + distance * 0.018F, 0.28F, 0.62F);
			LiuSwordVfxEntity.spawnExecutionLink(level, start, end,
					EXECUTION_FIRE_RED, EXECUTION_FIRE_YELLOW, width, 16);
			if (++spawned >= EXECUTION_MAX_LINKS)
				break;
		}
	}

	private static int executionRoot(int[] parent, int node) {
		int root = node;
		while (parent[root] != root)
			root = parent[root];
		while (parent[node] != node) {
			int next = parent[node];
			parent[node] = root;
			node = next;
		}
		return root;
	}

	public static void restrainExecutionTarget(ServerPlayer owner, LivingEntity target, int durationTicks) {
		if (!isValidTarget(owner, target))
			return;
		long until = owner.level().getGameTime() + Math.max(2, durationTicks);
		CompoundTag data = target.getPersistentData();
		data.putUUID(EXECUTION_SLOW_OWNER, owner.getUUID());
		data.putLong(EXECUTION_SLOW_UNTIL, Math.max(data.getLong(EXECUTION_SLOW_UNTIL), until));
		target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,
				Math.max(2, durationTicks), 2, false, false, true));
	}

	public static void scheduleExecutionDetonation(ServerPlayer owner, Set<UUID> ids,
			Map<UUID, UUID> markerEffects, float damage, int primaryColor, int secondaryColor,
			boolean dual) {
		if (ids.isEmpty())
			return;
		long detonateAt = owner.level().getGameTime() + EXECUTION_DELAY_TICKS;
		Set<UUID> targets = new HashSet<>(ids);
		for (UUID id : targets) {
			LivingEntity target = living(owner.serverLevel(), id);
			if (target != null) {
				CompoundTag data = target.getPersistentData();
				data.putUUID(EXECUTION_SLOW_OWNER, owner.getUUID());
				data.putLong(EXECUTION_SLOW_UNTIL, detonateAt + 3L);
				target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,
						EXECUTION_DELAY_TICKS + 3, 2, false, false, true));
			}
		}
		EXECUTIONS.computeIfAbsent(owner.getUUID(), ignored -> new ArrayList<>())
				.add(new ExecutionState(detonateAt, targets, new HashMap<>(markerEffects), damage,
						primaryColor, secondaryColor, dual));
	}

	public static boolean isValidTarget(Player player, LivingEntity target) {
		if (player == null || target == null || target == player || !target.isAlive() || !target.isAttackable()
				|| target.isInvulnerable() || target instanceof ArmorStand)
			return false;
		if (player.isAlliedTo(target) || target.isAlliedTo(player) || ShadowMonarchManager.isOwnedShadow(target, player))
			return false;
		if (target instanceof TamableAnimal tame && player.getUUID().equals(tame.getOwnerUUID()))
			return false;
		if (target instanceof Player other)
			return !other.isCreative() && !other.isSpectator() && player.canHarmPlayer(other);
		return true;
	}

	@SubscribeEvent
	public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
		if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide()
				|| !(event.player instanceof ServerPlayer player))
			return;
		if (!player.isAlive() || !isLiuVessel(player)) {
			clearState(player);
			return;
		}
		tickCharge(player);
		tickFlash(player);
		tickDance(player);
		tickDomain(player);
		tickExecutions(player);
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void onAttacked(LivingAttackEvent event) {
		if (!(event.getEntity() instanceof ServerPlayer player) || !isLiuVessel(player)
				|| event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY))
			return;
		long now = player.level().getGameTime();
		int charges = player.getPersistentData().getInt(COUNTER_CHARGES);
		if (charges <= 0 || player.getPersistentData().getLong(COUNTER_UNTIL) < now)
			return;
		event.setCanceled(true);
		player.getPersistentData().putInt(COUNTER_CHARGES, charges - 1);
		player.getPersistentData().putLong(COUNTER_SURGE_UNTIL, now + 100L);
		Entity direct = event.getSource().getDirectEntity();
		Entity attackerEntity = event.getSource().getEntity();
		if (direct instanceof Projectile projectile) {
			Vec3 reflected = projectile.getDeltaMovement().scale(-1.35D).add(0.0D, 0.08D, 0.0D);
			projectile.setOwner(player);
			projectile.setDeltaMovement(reflected);
			projectile.hurtMarked = true;
		}
		if (attackerEntity instanceof LivingEntity attacker && isValidTarget(player, attacker)) {
			float damage = swordSkillDamage(player, player.getMainHandItem(),
					1.55D * (isManifested(player) ? 1.15D : 1.0D));
			dealSwordDamage(player, attacker, damage);
			Vec3 away = attacker.position().subtract(player.position());
			push(attacker, away.lengthSqr() < 0.001D ? player.getLookAngle() : away.normalize(), 1.15D, 0.24D);
			LiuSwordVfxEntity.spawnAttached(player.serverLevel(), attacker, LiuSwordVfxEntity.COUNTER,
					primaryHandColor(player), secondaryHandColor(player), 3.2F, 5.0F, 24.0F, 12, isDualWielding(player));
		}
		player.level().playSound(null, player.blockPosition(), SoundEvents.ANVIL_LAND,
				SoundSource.PLAYERS, 0.75F, 1.62F);
	}

	@SubscribeEvent
	public static void onFall(LivingFallEvent event) {
		if (event.getEntity() instanceof Player player
				&& player.getPersistentData().getLong(FALL_SAFE_UNTIL) >= player.level().getGameTime()) {
			event.setCanceled(true);
			player.fallDistance = 0.0F;
		}
	}

	@SubscribeEvent
	public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
		clearState(event.getEntity());
	}

	private static void tickCharge(ServerPlayer player) {
		BeamChargeState state = BEAM_CHARGES.get(player.getUUID());
		if (state == null)
			return;
		long held = player.level().getGameTime() - state.startedAt;
		if (held > 600L || !isCombatStance(player) || !sameHeldWeapons(player, state)) {
			cancelBeamCharge(player);
			return;
		}
		if (held == BEAM_TIER_ONE_TICKS || held == BEAM_TIER_TWO_TICKS
				|| held == BEAM_TIER_THREE_TICKS) {
			boolean executionReady = held == BEAM_TIER_THREE_TICKS;
			float pitch = held == BEAM_TIER_ONE_TICKS ? 1.25F
					: held == BEAM_TIER_TWO_TICKS ? 1.55F : 1.9F;
			player.level().playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE,
					SoundSource.PLAYERS, executionReady ? 1.0F : 0.62F, pitch);
			LiuSwordVfxEntity.spawnAttached(player.serverLevel(), player, LiuSwordVfxEntity.CHARGE,
					state.primaryColor, state.secondaryColor,
					executionReady ? 3.1F : 1.7F + held / 38.0F,
					executionReady ? 5.5F : 3.4F, executionReady ? 18.0F : 0.0F, 8, state.dual);
		}
	}

	private static void tickFlash(ServerPlayer player) {
		FlashChargeState charge = FLASH_CHARGES.get(player.getUUID());
		if (charge != null && player.level().getGameTime() - charge.startedAt > 85L) {
			FLASH_CHARGES.remove(player.getUUID());
			if (charge.markerId != null)
				removeVfx(player.serverLevel(), charge.markerId);
		}
		FlashDashState state = FLASH_DASHES.get(player.getUUID());
		if (state == null)
			return;
		long elapsed = player.level().getGameTime() - state.startedAt;
		LivingEntity target = state.targetId == null ? null : living(player.serverLevel(), state.targetId);
		if (state.targetId != null && !isValidTarget(player, target)) {
			finishFlash(player, state, null, false);
			return;
		}
		Vec3 motion;
		if (target != null) {
			if (CombatRangeHelper.withinSurfaceRange(player, target, 1.65D)) {
				finishFlash(player, state, target, true);
				return;
			}
			Vec3 toward = target.getBoundingBox().getCenter().subtract(player.getBoundingBox().getCenter());
			motion = toward.normalize().scale(2.65D * state.power);
			dampenTargetForFlash(target);
		} else {
			motion = state.direction.scale(2.35D * state.power).add(0.0D, -0.035D * Math.max(0L, elapsed - 5L), 0.0D);
		}
		if (!player.serverLevel().hasChunkAt(BlockPos.containing(player.position().add(motion)))) {
			finishFlash(player, state, target, false);
			return;
		}
		boolean blocked = !player.serverLevel().noCollision(player, player.getBoundingBox().move(motion));
		if (blocked || elapsed >= state.maxTicks) {
			finishFlash(player, state, target, true);
			return;
		}
		player.setDeltaMovement(motion);
		player.hurtMarked = true;
		player.fallDistance = 0.0F;
		if ((elapsed & 1L) == 0L)
			LiuSwordVfxEntity.spawnAttached(player.serverLevel(), player, LiuSwordVfxEntity.DASH,
					state.primaryColor, state.secondaryColor, 1.65F, 4.2F,
					state.dual ? 18.0F : 0.0F, 4, state.dual);
		for (LivingEntity crossed : player.serverLevel().getEntitiesOfClass(LivingEntity.class,
				player.getBoundingBox().inflate(1.4D), candidate -> isValidTarget(player, candidate))) {
			if (state.crossed.add(crossed.getUUID()))
				dealSwordDamage(player, crossed, swordSkillDamage(player, player.getMainHandItem(), 0.45D));
		}
	}

	private static void finishFlash(ServerPlayer player, FlashDashState state, LivingEntity target, boolean impact) {
		FLASH_DASHES.remove(player.getUUID());
		player.setDeltaMovement(Vec3.ZERO);
		player.hurtMarked = true;
		player.fallDistance = 0.0F;
		if (!impact)
			return;
		Vec3 center = target == null ? player.position().add(player.getLookAngle().scale(1.4D)) : target.getBoundingBox().getCenter();
		float damage = swordSkillDamage(player, player.getMainHandItem(),
				2.2D * (isManifested(player) ? 1.18D : 1.0D));
		for (LivingEntity victim : targets(player, new AABB(center, center).inflate(state.dual ? 6.8D : 5.2D, 3.4D, state.dual ? 6.8D : 5.2D))) {
			if (dealSwordDamage(player, victim, victim == target ? damage * 1.2F : damage))
				markForDomain(player, victim, 2);
		}
		LiuSwordVfxEntity.spawn(player.serverLevel(), center.x, center.y, center.z, LiuSwordVfxEntity.DETONATION,
				state.primaryColor, state.secondaryColor, state.dual ? 8.5F : 6.4F, state.dual ? 10.0F : 7.2F,
				18.0F, 17, player.getYRot(), 0.0F, state.dual);
		player.level().playSound(null, BlockPos.containing(center), SoundEvents.PLAYER_ATTACK_SWEEP,
				SoundSource.PLAYERS, 1.25F, 0.72F);
	}

	private static void tickDance(ServerPlayer player) {
		DanceState state = DANCES.get(player.getUUID());
		if (state == null)
			return;
		if (state.index >= state.targets.size() || player.level().getGameTime() - state.startedAt > 95L) {
			finishDance(player);
			return;
		}
		LivingEntity target = living(player.serverLevel(), state.targets.get(state.index));
		if (!isValidTarget(player, target)) {
			state.index++;
			state.approachTicks = 0;
			return;
		}
		Vec3 toward = target.getBoundingBox().getCenter().subtract(player.getBoundingBox().getCenter());
		boolean close = CombatRangeHelper.withinSurfaceRange(player, target, 1.7D);
		if (!close && state.approachTicks < 4) {
			Vec3 motion = toward.normalize().scale(state.manifested ? 2.45D : 2.05D);
			if (player.serverLevel().noCollision(player, player.getBoundingBox().move(motion))) {
				player.setDeltaMovement(motion);
				player.hurtMarked = true;
				state.approachTicks++;
				if ((state.approachTicks & 1) == 0)
					LiuSwordVfxEntity.spawnAttached(player.serverLevel(), player, LiuSwordVfxEntity.DASH,
							state.primaryColor, state.secondaryColor, 1.35F, 3.2F, 22.0F, 4, true);
				return;
			}
		}
		float damage = swordSkillDamage(player, player.getMainHandItem(),
				1.2D * (state.manifested ? 1.16D : 1.0D));
		dealSwordDamage(player, target, damage);
		markForDomain(player, target, 2);
		LiuSwordVfxEntity.spawnAttached(player.serverLevel(), target, LiuSwordVfxEntity.DANCE,
				state.index % 2 == 0 ? state.primaryColor : state.secondaryColor,
				state.index % 2 == 0 ? state.secondaryColor : state.primaryColor,
				3.0F + state.index * 0.18F, 5.2F, state.index % 2 == 0 ? -28.0F : 28.0F, 10, true);
		state.index++;
		state.approachTicks = 0;
		player.setDeltaMovement(Vec3.ZERO);
		player.hurtMarked = true;
	}

	private static void finishDance(ServerPlayer player) {
		DANCES.remove(player.getUUID());
		player.setDeltaMovement(Vec3.ZERO);
		player.hurtMarked = true;
		LiuSwordVfxEntity.spawnAttached(player.serverLevel(), player, LiuSwordVfxEntity.DETONATION,
				primaryHandColor(player), secondaryHandColor(player), 5.4F, 7.5F, 18.0F, 13, true);
	}

	private static void spawnDanceWaves(ServerPlayer player, boolean manifested) {
		Vec3 origin = player.getEyePosition().add(0.0D, -0.25D, 0.0D);
		for (int i = -1; i <= 1; i++) {
			int delay = (i + 1) * 3;
			int index = i;
			SololevelingMod.queueServerWork(delay, () -> {
				if (!player.isAlive())
					return;
				Vec3 direction = rotateYaw(player.getLookAngle().normalize(), index * 11.0D);
				LiuSwordBeamEntity.spawn(player.serverLevel(), player, origin, direction, 0, false,
						primaryHandColor(player), secondaryHandColor(player), manifested ? 4.8F : 3.6F,
						manifested ? 25.0F : 19.0F, 3.5F,
						swordSkillDamage(player, player.getMainHandItem(),
								0.9D * (manifested ? 1.15D : 1.0D)));
			});
		}
	}

	private static void tickDomain(ServerPlayer player) {
		DomainState state = DOMAINS.get(player.getUUID());
		if (state == null)
			return;
		long now = player.level().getGameTime();
		if (now >= state.endTick) {
			DOMAINS.remove(player.getUUID());
			finishDomain(player, state);
			return;
		}
		if (now >= state.nextScan) {
			state.nextScan = now + 10L;
			double radius = state.manifested ? 28.0D : 22.0D;
			List<LivingEntity> nearby = targets(player, player.getBoundingBox().inflate(radius, 10.0D, radius));
			nearby.sort(Comparator.comparingDouble(player::distanceToSqr));
			for (LivingEntity target : nearby.stream().limit(state.manifested ? 18 : 12).toList()) {
				markForDomain(player, target, 1);
				if (player.tickCount % 20 == 0)
					LiuSwordVfxEntity.spawnAttached(player.serverLevel(), target, LiuSwordVfxEntity.MARK,
							GOLD, PALE_GOLD, Math.max(1.1F, target.getBbWidth()),
							Math.max(1.8F, target.getBbHeight()), 0.0F, 14, false);
			}
		}
		if (now >= state.nextDeflect) {
			state.nextDeflect = now + 4L;
			int count = 0;
			for (Projectile projectile : player.serverLevel().getEntitiesOfClass(Projectile.class,
					player.getBoundingBox().inflate(state.manifested ? 10.0D : 7.5D), projectile -> projectile.getOwner() != player)) {
				if (count++ >= (state.manifested ? 10 : 6))
					break;
				Vec3 away = projectile.position().subtract(player.getEyePosition());
				if (away.lengthSqr() < 0.001D)
					away = player.getLookAngle();
				projectile.setOwner(player);
				projectile.setDeltaMovement(away.normalize().scale(Math.max(1.0D, projectile.getDeltaMovement().length() * 1.15D)));
				projectile.hurtMarked = true;
			}
		}
	}

	private static void finishDomain(ServerPlayer player, DomainState state) {
		double radius = state.manifested ? 32.0D : 25.0D;
		List<LivingEntity> marked = targets(player, player.getBoundingBox().inflate(radius, 14.0D, radius));
		marked.removeIf(target -> !hasDomainMark(player, target));
		for (LivingEntity target : marked) {
			int stacks = target.getPersistentData().getInt(DOMAIN_MARK_STACKS);
			LiuSwordVfxEntity.spawnAttached(player.serverLevel(), target, LiuSwordVfxEntity.DETONATION,
					GOLD, PALE_GOLD, Math.max(3.0F, target.getBbWidth() * 1.8F),
					Math.max(3.8F, target.getBbHeight() * 1.35F), stacks % 2 == 0 ? 20.0F : -20.0F, 15, true);
		}
		for (LivingEntity target : marked) {
			int stacks = Mth.clamp(target.getPersistentData().getInt(DOMAIN_MARK_STACKS), 1, 8);
			float damage = swordSkillDamage(player, player.getMainHandItem(),
					0.75D * (1.0D + stacks * 0.16D));
			dealSwordDamage(player, target, damage);
			clearDomainMark(target);
		}
		if (!marked.isEmpty())
			player.level().playSound(null, player.blockPosition(), SoundEvents.TRIDENT_THUNDER,
					SoundSource.PLAYERS, 0.92F, 1.62F);
	}

	private static boolean isDomainActive(ServerPlayer player) {
		DomainState state = DOMAINS.get(player.getUUID());
		return state != null && state.endTick > player.level().getGameTime();
	}

	private static void markForDomain(ServerPlayer player, LivingEntity target, int stacks) {
		if (!isDomainActive(player) || !isValidTarget(player, target))
			return;
		long now = target.level().getGameTime();
		if (!target.getPersistentData().hasUUID(DOMAIN_MARK_OWNER)
				|| !player.getUUID().equals(target.getPersistentData().getUUID(DOMAIN_MARK_OWNER))
				|| target.getPersistentData().getLong(DOMAIN_MARK_UNTIL) < now)
			target.getPersistentData().putInt(DOMAIN_MARK_STACKS, 0);
		target.getPersistentData().putUUID(DOMAIN_MARK_OWNER, player.getUUID());
		target.getPersistentData().putLong(DOMAIN_MARK_UNTIL, now + 340L);
		target.getPersistentData().putInt(DOMAIN_MARK_STACKS,
				Math.min(8, target.getPersistentData().getInt(DOMAIN_MARK_STACKS) + Math.max(1, stacks)));
	}

	private static boolean hasDomainMark(ServerPlayer player, LivingEntity target) {
		return target.getPersistentData().hasUUID(DOMAIN_MARK_OWNER)
				&& player.getUUID().equals(target.getPersistentData().getUUID(DOMAIN_MARK_OWNER))
				&& target.getPersistentData().getLong(DOMAIN_MARK_UNTIL) >= target.level().getGameTime();
	}

	private static void clearDomainMark(LivingEntity target) {
		target.getPersistentData().remove(DOMAIN_MARK_OWNER);
		target.getPersistentData().remove(DOMAIN_MARK_UNTIL);
		target.getPersistentData().remove(DOMAIN_MARK_STACKS);
	}

	private static void tickExecutions(ServerPlayer player) {
		List<ExecutionState> pending = EXECUTIONS.get(player.getUUID());
		if (pending == null)
			return;
		long now = player.level().getGameTime();
		for (int i = pending.size() - 1; i >= 0; i--) {
			ExecutionState state = pending.get(i);
			if (!state.impactSent && now >= state.detonateAt - EXECUTION_IMPACT_TICKS) {
				state.impactSent = true;
				sendExecutionImpactFrame(player, state);
			}
			if (now < state.detonateAt)
				continue;
			detonateExecutionMarks(player, state.targets, state.damage,
					state.primaryColor, state.secondaryColor, state.dual);
			for (UUID markerId : state.markerEffects.values())
				removeVfx(player.serverLevel(), markerId);
			pending.remove(i);
		}
		if (pending.isEmpty())
			EXECUTIONS.remove(player.getUUID());
	}

	private static void sendExecutionImpactFrame(ServerPlayer owner, ExecutionState state) {
		for (UUID targetId : state.targets) {
			LivingEntity target = living(owner.serverLevel(), targetId);
			if (isValidTarget(owner, target)) {
				LiuExecutionImpactMessage message = new LiuExecutionImpactMessage(EXECUTION_IMPACT_TICKS,
						state.primaryColor, state.secondaryColor);
				SololevelingMod.PACKET_HANDLER.send(PacketDistributor.PLAYER.with(() -> owner), message);
				return;
			}
		}
	}

	private static void releaseExecutionRestraint(ServerPlayer owner, LivingEntity target) {
		releaseExecutionRestraint(owner, target, false);
	}

	private static void releaseExecutionRestraint(ServerPlayer owner, LivingEntity target, boolean force) {
		CompoundTag data = target.getPersistentData();
		if (!data.hasUUID(EXECUTION_SLOW_OWNER)
				|| !owner.getUUID().equals(data.getUUID(EXECUTION_SLOW_OWNER)))
			return;
		if (!force && data.getLong(EXECUTION_SLOW_UNTIL) > target.level().getGameTime() + 5L)
			return;
		MobEffectInstance effect = target.getEffect(MobEffects.MOVEMENT_SLOWDOWN);
		if (effect != null && effect.getAmplifier() == 2)
			target.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
		data.remove(EXECUTION_SLOW_OWNER);
		data.remove(EXECUTION_SLOW_UNTIL);
	}

	private static void cancelPendingExecutions(ServerPlayer player) {
		List<ExecutionState> pending = EXECUTIONS.remove(player.getUUID());
		if (pending == null)
			return;
		for (ExecutionState state : pending) {
			for (UUID targetId : state.targets) {
				LivingEntity target = living(player.serverLevel(), targetId);
				if (target != null)
					releaseExecutionRestraint(player, target, true);
			}
			for (UUID markerId : state.markerEffects.values())
				removeVfx(player.serverLevel(), markerId);
		}
	}

	private static void clearState(Entity entity) {
		if (entity == null)
			return;
		UUID id = entity.getUUID();
		if (entity instanceof ServerPlayer player) {
			cancelBeamCharge(player);
			cancelPendingExecutions(player);
		}
		FlashChargeState charge = FLASH_CHARGES.remove(id);
		if (charge != null && charge.markerId != null && entity instanceof ServerPlayer player)
			removeVfx(player.serverLevel(), charge.markerId);
		FLASH_DASHES.remove(id);
		DANCES.remove(id);
		DOMAINS.remove(id);
	}

	private static boolean sameHeldWeapons(ServerPlayer player, BeamChargeState state) {
		ItemStack main = player.getMainHandItem();
		ItemStack off = player.getOffhandItem();
		if (!isMeleeWeapon(main))
			main = off;
		return state.mainSignature.equals(itemSignature(main)) && state.offSignature.equals(itemSignature(off));
	}

	private static String itemSignature(ItemStack stack) {
		if (stack.isEmpty())
			return "empty";
		ResourceLocation id = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(stack.getItem());
		return id == null ? stack.getItem().getDescriptionId() : id.toString();
	}

	private static double weaponPower(ItemStack stack) {
		if (stack.isEmpty())
			return 2.0D;
		double value = 1.0D;
		for (AttributeModifier modifier : stack.getAttributeModifiers(EquipmentSlot.MAINHAND).get(Attributes.ATTACK_DAMAGE))
			value += modifier.getAmount();
		return Mth.clamp(value, 2.0D, 42.0D);
	}

	private static float swordSkillDamage(ServerPlayer player, ItemStack weapon, double abilityScale) {
		return swordSkillDamage(player, weaponPower(weapon), abilityScale);
	}

	private static float swordSkillDamage(ServerPlayer player, double weaponPower, double abilityScale) {
		double strength = Math.max(0.0D, variables(player).Strength);
		// Strength supplies the damage; weapon quality only improves how efficiently Liu can express it.
		double mastery = 1.0D + strength * 0.12D + Math.pow(strength, 1.25D) * 0.025D;
		double weaponEfficiency = 0.82D + Mth.clamp(weaponPower, 2.0D, 30.0D) / 70.0D;
		return (float) Math.max(0.5D, mastery * weaponEfficiency * abilityScale);
	}

	private static int primaryHandColor(ServerPlayer player) {
		ItemStack main = isMeleeWeapon(player.getMainHandItem()) ? player.getMainHandItem() : player.getOffhandItem();
		return colorFor(main);
	}

	private static int secondaryHandColor(ServerPlayer player) {
		return isMeleeWeapon(player.getOffhandItem()) ? colorFor(player.getOffhandItem()) : primaryHandColor(player);
	}

	private static boolean isDualWielding(ServerPlayer player) {
		return isMeleeWeapon(player.getMainHandItem()) && isMeleeWeapon(player.getOffhandItem());
	}

	private static LivingEntity crosshairTarget(ServerPlayer player, double range) {
		Vec3 eye = player.getEyePosition();
		Vec3 look = player.getLookAngle().normalize();
		AABB search = player.getBoundingBox().expandTowards(look.scale(range)).inflate(2.8D);
		LivingEntity best = null;
		double bestAlong = Double.MAX_VALUE;
		for (LivingEntity target : targets(player, search)) {
			Vec3 to = target.getBoundingBox().getCenter().subtract(eye);
			double along = to.dot(look);
			if (along <= 0.0D || along > range)
				continue;
			double perpendicular = to.subtract(look.scale(along)).length();
			if (perpendicular <= 1.1D + target.getBbWidth() * 0.55D && along < bestAlong && player.hasLineOfSight(target)) {
				best = target;
				bestAlong = along;
			}
		}
		return best;
	}

	private static List<LivingEntity> targets(ServerPlayer player, AABB area) {
		return player.serverLevel().getEntitiesOfClass(LivingEntity.class, area, target -> isValidTarget(player, target));
	}

	private static List<LivingEntity> targetsInSlash(ServerPlayer player, Vec3 start, Vec3 direction,
			double range, double width, int cap) {
		Vec3 end = start.add(direction.scale(range));
		AABB area = new AABB(start, end).inflate(width, width * 0.75D, width);
		List<LivingEntity> found = targets(player, area);
		found.removeIf(target -> {
			Vec3 center = target.getBoundingBox().getCenter();
			Vec3 relative = center.subtract(start);
			double along = relative.dot(direction);
			if (along < -0.5D || along > range + target.getBbWidth())
				return true;
			return relative.subtract(direction.scale(Mth.clamp(along, 0.0D, range))).length()
					> width + target.getBbWidth() * 0.65D;
		});
		found.sort(Comparator.comparingDouble(player::distanceToSqr));
		return found.stream().limit(cap).toList();
	}

	private static boolean dealSwordDamage(ServerPlayer player, LivingEntity target, float damage) {
		if (!isValidTarget(player, target))
			return false;
		target.invulnerableTime = 0;
		boolean hurt = target.hurt(player.damageSources().playerAttack(player), Math.max(0.5F, damage));
		if (hurt)
			target.setLastHurtByPlayer(player);
		return hurt;
	}

	private static void dampenTargetForFlash(LivingEntity target) {
		double factor = controlFactor(target);
		Vec3 movement = target.getDeltaMovement();
		target.setDeltaMovement(movement.x * (1.0D - factor), Math.max(0.015D, movement.y * 0.18D), movement.z * (1.0D - factor));
		target.fallDistance = 0.0F;
		target.hurtMarked = true;
	}

	private static double controlFactor(LivingEntity target) {
		double resistance = Mth.clamp(target.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 0.0D, 1.0D);
		double sizePenalty = target.getBbWidth() >= 4.0F || target.getMaxHealth() >= 500.0F ? 0.25D
				: target.getBbWidth() >= 2.4F || target.getMaxHealth() >= 220.0F ? 0.5D : 1.0D;
		return Mth.clamp((1.0D - resistance * 0.8D) * sizePenalty, 0.1D, 1.0D);
	}

	private static void push(LivingEntity target, Vec3 direction, double horizontal, double vertical) {
		Vec3 flat = new Vec3(direction.x, 0.0D, direction.z);
		if (flat.lengthSqr() < 0.001D)
			flat = new Vec3(0.0D, 0.0D, 1.0D);
		double factor = controlFactor(target);
		flat = flat.normalize().scale(horizontal * factor);
		target.setDeltaMovement(flat.x, Math.max(target.getDeltaMovement().y, vertical * factor), flat.z);
		target.hurtMarked = true;
	}

	private static Vec3 rotateYaw(Vec3 vector, double degrees) {
		double radians = Math.toRadians(degrees);
		double sin = Math.sin(radians);
		double cos = Math.cos(radians);
		return new Vec3(vector.x * cos - vector.z * sin, vector.y, vector.x * sin + vector.z * cos).normalize();
	}

	private static LivingEntity living(ServerLevel level, UUID id) {
		Entity entity = level.getEntity(id);
		return entity instanceof LivingEntity living ? living : null;
	}

	private static void removeVfx(ServerLevel level, UUID id) {
		Entity effect = id == null ? null : level.getEntity(id);
		if (effect != null)
			effect.discard();
	}

	private static boolean consumeMana(ServerPlayer player, int amount) {
		if (player.isCreative())
			return true;
		if (variables(player).MP < amount) {
			player.displayClientMessage(Component.literal("Not enough MP (" + amount + " required)")
					.withStyle(ChatFormatting.RED), true);
			return false;
		}
		return drainMana(player, amount);
	}

	private static boolean consumeManaSilently(ServerPlayer player, int amount) {
		return player.isCreative() || variables(player).MP >= amount && drainMana(player, amount);
	}

	private static boolean drainMana(ServerPlayer player, int amount) {
		if (player.isCreative())
			return true;
		player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
			capability.MP = Math.max(0.0D, capability.MP - amount);
			capability.syncPlayerVariables(player);
		});
		CooldownManager.set(player, "mana_refresh", 35);
		return true;
	}

	private static boolean ready(ServerPlayer player, String key) {
		if (!CooldownManager.isOnCooldown(player, key))
			return true;
		player.displayClientMessage(Component.literal(key + " is on cooldown").withStyle(ChatFormatting.RED), true);
		return false;
	}

	private static SololevelingModVariables.PlayerVariables variables(Entity entity) {
		return entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
				.orElse(new SololevelingModVariables.PlayerVariables());
	}

	private record BeamChargeState(long startedAt, String mainSignature, String offSignature,
			int primaryColor, int secondaryColor, boolean dual, float weaponPower, UUID effectId) {
	}

	private record FlashChargeState(UUID targetId, long startedAt, UUID markerId) {
	}

	private record FlashDashState(UUID targetId, Vec3 direction, long startedAt, int maxTicks,
			double power, int primaryColor, int secondaryColor, boolean dual, Set<UUID> crossed) {
	}

	private record ExecutionLink(int first, int second, double distanceSqr) {
	}

	private static final class ExecutionState {
		private final long detonateAt;
		private final Set<UUID> targets;
		private final Map<UUID, UUID> markerEffects;
		private final float damage;
		private final int primaryColor;
		private final int secondaryColor;
		private final boolean dual;
		private boolean impactSent;

		private ExecutionState(long detonateAt, Set<UUID> targets, Map<UUID, UUID> markerEffects,
				float damage, int primaryColor, int secondaryColor, boolean dual) {
			this.detonateAt = detonateAt;
			this.targets = targets;
			this.markerEffects = markerEffects;
			this.damage = damage;
			this.primaryColor = primaryColor;
			this.secondaryColor = secondaryColor;
			this.dual = dual;
		}
	}

	private static final class DanceState {
		private final List<UUID> targets;
		private int index;
		private int approachTicks;
		private final long startedAt;
		private final boolean manifested;
		private final int primaryColor;
		private final int secondaryColor;

		private DanceState(List<UUID> targets, int index, int approachTicks, long startedAt,
				boolean manifested, int primaryColor, int secondaryColor) {
			this.targets = targets;
			this.index = index;
			this.approachTicks = approachTicks;
			this.startedAt = startedAt;
			this.manifested = manifested;
			this.primaryColor = primaryColor;
			this.secondaryColor = secondaryColor;
		}
	}

	private static final class DomainState {
		private final long endTick;
		private long nextScan;
		private long nextDeflect;
		private final boolean manifested;

		private DomainState(long endTick, long nextScan, long nextDeflect, boolean manifested) {
			this.endTick = endTick;
			this.nextScan = nextScan;
			this.nextDeflect = nextDeflect;
			this.manifested = manifested;
		}
	}
}
