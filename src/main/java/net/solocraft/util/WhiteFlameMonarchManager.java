package net.solocraft.util;

import net.solocraft.SololevelingMod;
import net.solocraft.entity.DemonKnightEntity;
import net.solocraft.entity.RadiruBloodSpearEntity;
import net.solocraft.entity.WhiteFlameVfxEntity;
import net.solocraft.init.SololevelingModEntities;
import net.solocraft.network.SololevelingModVariables;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = SololevelingMod.MODID)
public final class WhiteFlameMonarchManager {
	public static final String LIGHTNING_BREATH = "Lightning Breath";
	public static final String HELLSTORM_DOMINION = "Hellstorm Dominion";
	public static final String RADIRU_BLOOD_SPEAR = "Radiru Blood Spear";
	public static final String DOPPELGANGER = "Doppelganger";
	public static final String HELLS_ARMY = "Hell's Army";
	public static final String SPIRITUALIZATION = "White Flame Spiritualization";

	public static final String AURA_ID = "white_flame_spiritualization";
	public static final String DOPPELGANGER_AURA_ID = "white_flame_doppelganger";
	private static final String SPIRITUALIZED = "mowf_spiritualized";
	private static final String CHAIN_DODGE_UNTIL = "mowf_chain_dodge_until";
	private static final String LAST_DODGE_MOVE = "mowf_last_dodge_move";
	private static final String BREATH_UNTIL = "mowf_breath_until";
	private static final String DOMAIN_UNTIL = "mowf_domain_until";
	private static final String DOMAIN_NEXT = "mowf_domain_next";
	private static final String DOPPEL_CHARGES = "mowf_doppel_charges";
	private static final String DOPPEL_UNTIL = "mowf_doppel_until";
	private static final String BRAND_UNTIL = "mowf_brand_until";
	private static final String BRAND_STACKS = "mowf_brand_stacks";
	private static final String BRAND_OWNER = "mowf_brand_owner";
	public static final String SUMMON_OWNER = "mowf_summon_owner";
	public static final String SUMMON_UNTIL = "mowf_summon_until";
	private static final TagKey<EntityType<?>> SHADOWS = TagKey.create(Registries.ENTITY_TYPE,
			new net.minecraft.resources.ResourceLocation("shadows"));

	private WhiteFlameMonarchManager() {
	}

	public static boolean isWhiteFlameVessel(Entity entity) {
		if (entity == null)
			return false;
		SololevelingModVariables.PlayerVariables vars = variables(entity);
		return vars.JOB == 4 && (vars.vesselIdentity.isBlank() || "baran".equals(vars.vesselIdentity));
	}

	public static boolean isSpiritualized(Entity entity) {
		return isWhiteFlameVessel(entity) && entity.getPersistentData().getBoolean(SPIRITUALIZED);
	}

	public static void castLightningBreath(Entity entity) {
		if (!(entity instanceof ServerPlayer player) || !isWhiteFlameVessel(player) || !ready(player, LIGHTNING_BREATH))
			return;
		boolean spiritualized = isSpiritualized(player);
		if (!consumeMana(player, spiritualized ? 300 : 220))
			return;
		CooldownManager.set(player, LIGHTNING_BREATH, spiritualized ? 62 : 78);
		player.getPersistentData().putLong(BREATH_UNTIL, player.level().getGameTime() + (spiritualized ? 18 : 14));
		player.level().playSound(null, player.blockPosition(), SoundEvents.LIGHTNING_BOLT_THUNDER,
				SoundSource.PLAYERS, 0.55F, spiritualized ? 1.75F : 1.45F);
	}

	public static void castHellstormDominion(Entity entity) {
		if (!(entity instanceof ServerPlayer player) || !isWhiteFlameVessel(player) || !ready(player, HELLSTORM_DOMINION))
			return;
		boolean spiritualized = isSpiritualized(player);
		if (!consumeMana(player, spiritualized ? 1050 : 850))
			return;
		long now = player.level().getGameTime();
		CooldownManager.set(player, HELLSTORM_DOMINION, spiritualized ? 330 : 390);
		player.getPersistentData().putLong(DOMAIN_UNTIL, now + (spiritualized ? 280 : 220));
		player.getPersistentData().putLong(DOMAIN_NEXT, now + 4);
		for (int wave = 0; wave < 3; wave++) {
			int delay = wave * 3;
			SololevelingMod.queueServerWork(delay, () -> {
				if (player.isAlive() && player.getPersistentData().getLong(DOMAIN_UNTIL) >= player.level().getGameTime())
					spawnDomainVolley(player.serverLevel(), player.position(), spiritualized ? 4 : 3,
							spiritualized ? 13.0D : 10.0D);
			});
		}
		player.level().playSound(null, player.blockPosition(), SoundEvents.LIGHTNING_BOLT_THUNDER,
				SoundSource.PLAYERS, 0.55F, 1.15F);
	}

	public static void castRadiruBloodSpear(Entity entity) {
		if (!(entity instanceof ServerPlayer player) || !isWhiteFlameVessel(player) || !ready(player, RADIRU_BLOOD_SPEAR))
			return;
		boolean spiritualized = isSpiritualized(player);
		if (!consumeMana(player, spiritualized ? 390 : 300))
			return;
		CooldownManager.set(player, RADIRU_BLOOD_SPEAR, spiritualized ? 72 : 96);
		float damage = magicDamage(player, spiritualized ? 38.0D : 28.0D, spiritualized ? 5.6D : 7.0D);
		RadiruBloodSpearEntity.launch(player, damage, spiritualized);
		player.level().playSound(null, player.blockPosition(), SoundEvents.TRIDENT_THROW,
				SoundSource.PLAYERS, 1.1F, spiritualized ? 0.72F : 0.9F);
	}

	public static void castDoppelganger(Entity entity) {
		if (!(entity instanceof ServerPlayer player) || !isWhiteFlameVessel(player) || !ready(player, DOPPELGANGER))
			return;
		boolean spiritualized = isSpiritualized(player);
		if (!consumeMana(player, spiritualized ? 720 : 590))
			return;
		long now = player.level().getGameTime();
		CooldownManager.set(player, DOPPELGANGER, spiritualized ? 220 : 280);
		player.getPersistentData().putInt(DOPPEL_CHARGES, spiritualized ? 4 : 3);
		player.getPersistentData().putLong(DOPPEL_UNTIL, now + (spiritualized ? 220 : 160));
		PlayerAuraSystem.burst(player, DOPPELGANGER_AURA_ID, spiritualized ? 220 : 160, spiritualized ? 1.2F : 1.0F);
		WhiteFlameVfxEntity.spawn(player.serverLevel(), player.getX(), player.getY(), player.getZ(),
				WhiteFlameVfxEntity.DOPPELGANGER, 1.1F, 2.4F, 24, player.getYRot(), 0);
		player.level().playSound(null, player.blockPosition(), SoundEvents.ILLUSIONER_CAST_SPELL,
				SoundSource.PLAYERS, 1.0F, 1.35F);
	}

	public static void castHellsArmy(Entity entity) {
		if (!(entity instanceof ServerPlayer player) || !isWhiteFlameVessel(player) || !ready(player, HELLS_ARMY))
			return;
		boolean spiritualized = isSpiritualized(player);
		if (!consumeMana(player, spiritualized ? 1450 : 1200))
			return;
		CooldownManager.set(player, HELLS_ARMY, spiritualized ? 520 : 650);
		ServerLevel level = player.serverLevel();
		int count = spiritualized ? 7 : 5;
		long expiry = level.getGameTime() + (spiritualized ? 520 : 400);
		WhiteFlameVfxEntity.spawn(level, player.getX(), player.getY() + 0.05D, player.getZ(),
				WhiteFlameVfxEntity.HELL_GATE, spiritualized ? 5.5F : 4.2F, spiritualized ? 5.5F : 4.4F, 28, 0, 0);
		for (int i = 0; i < count; i++) {
			double angle = Math.PI * 2.0D * i / count;
			BlockPos pos = BlockPos.containing(player.getX() + Math.cos(angle) * 3.2D,
					player.getY(), player.getZ() + Math.sin(angle) * 3.2D);
			DemonKnightEntity knight = SololevelingModEntities.DEMON_KNIGHT.get().spawn(level, pos, MobSpawnType.MOB_SUMMONED);
			if (knight == null)
				continue;
			knight.randomizeVariant();
			knight.getPersistentData().putUUID(SUMMON_OWNER, player.getUUID());
			knight.getPersistentData().putLong(SUMMON_UNTIL, expiry);
			knight.getPersistentData().putBoolean("mowf_no_loot", true);
			knight.setCustomName(Component.literal("Radiru Royal Guard").withStyle(ChatFormatting.WHITE));
			knight.setCustomNameVisible(false);
			if (knight.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH) != null) {
				double max = spiritualized ? 100.0D : 72.0D;
				knight.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(max);
				knight.setHealth((float) max);
			}
			if (knight.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE) != null)
				knight.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE)
						.setBaseValue(Math.min(34.0D, (spiritualized ? 15.0D : 11.0D) + variables(player).Intelligence / 28.0D));
			knight.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, (int) (expiry - level.getGameTime()), 0, false, false));
		}
		player.level().playSound(null, player.blockPosition(), SoundEvents.WITHER_SPAWN,
				SoundSource.PLAYERS, 0.75F, 1.55F);
	}

	public static void toggleSpiritualization(Entity entity) {
		if (!(entity instanceof ServerPlayer player) || !isWhiteFlameVessel(player))
			return;
		if (isSpiritualized(player)) {
			disableSpiritualization(player, false);
			return;
		}
		if (!ready(player, SPIRITUALIZATION) || !consumeMana(player, 800))
			return;
		player.getPersistentData().putBoolean(SPIRITUALIZED, true);
		player.getPersistentData().remove(CHAIN_DODGE_UNTIL);
		PlayerAuraSystem.setContinuous(player, AURA_ID, 1.3F);
		WhiteFlameVfxEntity.spawn(player.serverLevel(), player.getX(), player.getY() + 0.05D, player.getZ(),
				WhiteFlameVfxEntity.HELL_GATE, 3.8F, 5.2F, 30, 0, 0);
		player.level().playSound(null, player.blockPosition(), SoundEvents.BEACON_ACTIVATE,
				SoundSource.PLAYERS, 1.0F, 1.65F);
	}

	@SubscribeEvent
	public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
		if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide() || !(event.player instanceof ServerPlayer player))
			return;
		long now = player.level().getGameTime();
		if (player.getPersistentData().getBoolean(SPIRITUALIZED)) {
			if (!isWhiteFlameVessel(player)) {
				disableSpiritualization(player, true);
			} else if (player.tickCount % 20 == 0 && !drainMana(player, 14)) {
				disableSpiritualization(player, true);
			}
		}
		if (player.getPersistentData().getLong(DOPPEL_UNTIL) < now)
			player.getPersistentData().remove(DOPPEL_CHARGES);
		if (player.getPersistentData().getLong(BREATH_UNTIL) >= now && player.tickCount % 2 == 0)
			breathPulse(player);
		if (player.getPersistentData().getLong(DOMAIN_UNTIL) >= now
				&& player.getPersistentData().getLong(DOMAIN_NEXT) <= now) {
			player.getPersistentData().putLong(DOMAIN_NEXT, now + (isSpiritualized(player) ? 7 : 10));
			domainStrike(player);
		}
	}

	@SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
	public static void onPlayerAttacked(LivingAttackEvent event) {
		if (!(event.getEntity() instanceof ServerPlayer player) || !isWhiteFlameVessel(player) || !canDodge(event.getSource()))
			return;
		long now = player.level().getGameTime();
		if (event.isCanceled()) {
			if (isSpiritualized(player)) {
				player.getPersistentData().putLong(CHAIN_DODGE_UNTIL, now + 10);
				performDodge(player, event.getSource().getEntity(), false);
			}
			return;
		}
		boolean chain = isSpiritualized(player) && player.getPersistentData().getLong(CHAIN_DODGE_UNTIL) >= now;
		int echoes = player.getPersistentData().getInt(DOPPEL_CHARGES);
		boolean doppel = echoes > 0 && player.getPersistentData().getLong(DOPPEL_UNTIL) >= now;
		boolean spiritualRoll = isSpiritualized(player) && player.getRandom().nextFloat() < 0.25F;
		if (!chain && !doppel && (!spiritualRoll || !drainMana(player, 55)))
			return;
		event.setCanceled(true);
		if (doppel) {
			player.getPersistentData().putInt(DOPPEL_CHARGES, echoes - 1);
			WhiteFlameVfxEntity.spawn(player.serverLevel(), player.getX(), player.getY(), player.getZ(),
					WhiteFlameVfxEntity.DOPPELGANGER, 1.15F, 2.5F, 12, player.getYRot(), 0);
			Entity attacker = event.getSource().getEntity();
			if (attacker instanceof LivingEntity living && validTarget(player, living)) {
				dealMagic(player, living, magicDamage(player, 13.0D, 15.0D));
				brand(living, player, 80, 1);
			}
		}
		if (isSpiritualized(player))
			player.getPersistentData().putLong(CHAIN_DODGE_UNTIL, now + 10);
		performDodge(player, event.getSource().getEntity(), doppel || !chain);
	}

	@SubscribeEvent
	public static void onSummonTick(LivingEvent.LivingTickEvent event) {
		if (!(event.getEntity() instanceof DemonKnightEntity knight) || knight.level().isClientSide()
				|| !knight.getPersistentData().hasUUID(SUMMON_OWNER) || !(knight.level() instanceof ServerLevel level))
			return;
		UUID ownerId = knight.getPersistentData().getUUID(SUMMON_OWNER);
		ServerPlayer owner = level.getServer().getPlayerList().getPlayer(ownerId);
		if (owner == null || !owner.isAlive() || owner.level() != level
				|| level.getGameTime() > knight.getPersistentData().getLong(SUMMON_UNTIL)) {
			knight.discard();
			return;
		}
		LivingEntity target = knight.getTarget();
		if (target == null || !validTarget(owner, target)) {
			target = owner.getLastHurtMob();
			if (target == null || !validTarget(owner, target))
				target = owner.getLastHurtByMob();
			if (target == null || !validTarget(owner, target))
				target = nearestHostileTarget(level, owner, knight.position(), 20.0D);
			knight.setTarget(target);
		}
		if (knight.distanceToSqr(owner) > 900.0D)
			knight.teleportTo(owner.getX() + 1.0D, owner.getY(), owner.getZ() + 1.0D);
		else if (target == null && knight.distanceToSqr(owner) > 64.0D)
			knight.getNavigation().moveTo(owner, 1.15D);
	}

	private static void breathPulse(ServerPlayer player) {
		ServerLevel level = player.serverLevel();
		boolean spiritualized = isSpiritualized(player);
		Vec3 eye = player.getEyePosition();
		Vec3 look = player.getLookAngle().normalize();
		double range = spiritualized ? 24.0D : 18.0D;
		WhiteFlameVfxEntity.spawn(level, eye.x, eye.y - 0.12D, eye.z,
				WhiteFlameVfxEntity.LIGHTNING_BREATH, spiritualized ? 2.6F : 1.9F, (float) range,
				5, player.getYRot(), player.getXRot());
		float damage = magicDamage(player, spiritualized ? 7.0D : 5.0D, spiritualized ? 24.0D : 30.0D);
		for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class,
				player.getBoundingBox().inflate(range), candidate -> validTarget(player, candidate))) {
			Vec3 toTarget = target.getBoundingBox().getCenter().subtract(eye);
			double distance = toTarget.length();
			if (distance > range || distance < 0.2D)
				continue;
			double projection = toTarget.dot(look);
			double sideDistance = toTarget.subtract(look.scale(projection)).length();
			if (projection > 0 && sideDistance <= (spiritualized ? 2.8D : 2.0D) + distance * 0.06D) {
				dealMagic(player, target, damage);
				brand(target, player, spiritualized ? 130 : 90, 1);
				target.setSecondsOnFire(spiritualized ? 4 : 2);
			}
		}
	}

	private static void domainStrike(ServerPlayer player) {
		ServerLevel level = player.serverLevel();
		boolean spiritualized = isSpiritualized(player);
		LivingEntity target = nearestTarget(level, player, player.position(), spiritualized ? 22.0D : 18.0D, true);
		Vec3 point;
		if (target != null) {
			point = findDomainStrikePoint(level, target.getX(), target.getY(), target.getZ());
			float damage = magicDamage(player, spiritualized ? 18.0D : 13.0D, spiritualized ? 13.0D : 17.0D);
			dealMagic(player, target, damage * (brandStacks(target, player) > 0 ? 1.22F : 1.0F));
			brand(target, player, spiritualized ? 150 : 110, 1);
		} else {
			double angle = player.getRandom().nextDouble() * Math.PI * 2.0D;
			double radius = 4.0D + player.getRandom().nextDouble() * 10.0D;
			point = findDomainStrikePoint(level, player.getX() + Math.cos(angle) * radius,
					player.getY(), player.getZ() + Math.sin(angle) * radius);
		}
		spawnVisualLightning(level, point);
		spawnDomainVolley(level, player.position(), spiritualized ? 2 : 1, spiritualized ? 15.0D : 11.0D);
		level.playSound(null, BlockPos.containing(point), SoundEvents.LIGHTNING_BOLT_IMPACT,
				SoundSource.PLAYERS, 0.65F, 1.05F + player.getRandom().nextFloat() * 0.18F);
	}

	private static void spawnDomainVolley(ServerLevel level, Vec3 center, int count, double radius) {
		for (int i = 0; i < count; i++) {
			double angle = level.random.nextDouble() * Math.PI * 2.0D;
			double distance = 2.0D + level.random.nextDouble() * Math.max(1.0D, radius - 2.0D);
			Vec3 point = findDomainStrikePoint(level, center.x + Math.cos(angle) * distance,
					center.y, center.z + Math.sin(angle) * distance);
			spawnVisualLightning(level, point);
		}
	}

	private static void spawnVisualLightning(ServerLevel level, Vec3 point) {
		net.minecraft.world.entity.LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
		if (bolt == null)
			return;
		bolt.moveTo(point.x, point.y, point.z);
		bolt.setVisualOnly(true);
		bolt.setSilent(true);
		level.addFreshEntity(bolt);
	}

	private static Vec3 findDomainStrikePoint(ServerLevel level, double x, double y, double z) {
		int bx = (int) Math.floor(x);
		int bz = (int) Math.floor(z);
		int top = Math.min(level.getMaxBuildHeight() - 2, (int) Math.floor(y) + 4);
		int bottom = Math.max(level.getMinBuildHeight() + 1, (int) Math.floor(y) - 14);
		BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
		for (int by = top; by >= bottom; by--) {
			cursor.set(bx, by, bz);
			BlockPos below = cursor.below();
			if (level.getBlockState(cursor).getCollisionShape(level, cursor).isEmpty()
					&& !level.getBlockState(below).getCollisionShape(level, below).isEmpty())
				return Vec3.atBottomCenterOf(cursor);
		}
		return new Vec3(x, y, z);
	}

	private static void performDodge(ServerPlayer player, Entity attacker, boolean move) {
		long now = player.level().getGameTime();
		if (move && player.getPersistentData().getLong(LAST_DODGE_MOVE) + 4 <= now) {
			Vec3 threat = attacker == null ? player.getLookAngle().scale(-1.0D)
					: player.position().subtract(attacker.position()).normalize();
			Vec3 side = new Vec3(-threat.z, 0.0D, threat.x).normalize()
					.scale(player.getRandom().nextBoolean() ? 1.65D : -1.65D);
			AABB moved = player.getBoundingBox().move(side.x, 0.15D, side.z);
			if (player.level().noCollision(player, moved)) {
				player.connection.teleport(player.getX() + side.x, player.getY() + 0.15D,
						player.getZ() + side.z, player.getYRot(), player.getXRot());
				player.getPersistentData().putLong(LAST_DODGE_MOVE, now);
			}
		}
		WhiteFlameVfxEntity.spawn(player.serverLevel(), player.getX(), player.getY() + player.getBbHeight() * 0.5D, player.getZ(),
				WhiteFlameVfxEntity.DODGE, 1.35F, 1.0F, 8, player.getYRot(), 0);
		player.level().playSound(null, player.blockPosition(), SoundEvents.FIRE_EXTINGUISH,
				SoundSource.PLAYERS, 0.45F, 1.8F);
	}

	private static boolean canDodge(DamageSource source) {
		return source.is(DamageTypes.PLAYER_ATTACK) || source.is(DamageTypes.MOB_ATTACK)
				|| source.is(DamageTypes.MOB_PROJECTILE) || source.is(DamageTypes.MAGIC)
				|| source.is(DamageTypes.INDIRECT_MAGIC) || source.is(DamageTypes.ARROW)
				|| source.is(DamageTypes.TRIDENT) || source.is(DamageTypes.EXPLOSION);
	}

	private static void disableSpiritualization(ServerPlayer player, boolean exhausted) {
		player.getPersistentData().remove(SPIRITUALIZED);
		player.getPersistentData().remove(CHAIN_DODGE_UNTIL);
		PlayerAuraSystem.clearContinuous(player);
		CooldownManager.set(player, SPIRITUALIZATION, exhausted ? 180 : 80);
		player.level().playSound(null, player.blockPosition(), SoundEvents.BEACON_DEACTIVATE,
				SoundSource.PLAYERS, 0.8F, 1.25F);
		if (exhausted)
			player.displayClientMessage(Component.literal("White Flame Spiritualization ended: insufficient MP")
					.withStyle(ChatFormatting.RED), true);
	}

	private static LivingEntity nearestTarget(ServerLevel level, ServerPlayer owner, Vec3 center,
			double radius, boolean preferBranded) {
		List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class,
				new AABB(center, center).inflate(radius), candidate -> validTarget(owner, candidate));
		return targets.stream().min(Comparator
				.<LivingEntity>comparingInt(target -> preferBranded && brandStacks(target, owner) > 0 ? 0 : 1)
				.thenComparingDouble(target -> target.distanceToSqr(center))).orElse(null);
	}

	private static LivingEntity nearestHostileTarget(ServerLevel level, ServerPlayer owner, Vec3 center, double radius) {
		return level.getEntitiesOfClass(Monster.class, new AABB(center, center).inflate(radius),
				candidate -> validTarget(owner, candidate)).stream()
				.min(Comparator.comparingDouble(target -> target.distanceToSqr(center))).orElse(null);
	}

	public static boolean validTarget(ServerPlayer owner, LivingEntity target) {
		if (target == null || !target.isAlive() || target == owner || target.isAlliedTo(owner) || owner.isAlliedTo(target))
			return false;
		if (target.getType().is(SHADOWS))
			return false;
		if (target instanceof TamableAnimal tame && owner.getUUID().equals(tame.getOwnerUUID()))
			return false;
		if (target.getPersistentData().hasUUID(SUMMON_OWNER)
				&& owner.getUUID().equals(target.getPersistentData().getUUID(SUMMON_OWNER)))
			return false;
		if (target instanceof Player other)
			return !other.isCreative() && !other.isSpectator() && owner.canHarmPlayer(other);
		return true;
	}

	public static boolean dealMagic(ServerPlayer owner, LivingEntity target, float amount) {
		if (!validTarget(owner, target))
			return false;
		DamageSource source = new DamageSource(owner.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
				.getHolderOrThrow(DamageTypes.MAGIC), owner);
		target.invulnerableTime = 0;
		boolean hurt = target.hurt(source, Math.max(0.5F, amount));
		if (hurt)
			target.setLastHurtByPlayer(owner);
		return hurt;
	}

	public static void brand(LivingEntity target, ServerPlayer owner, int duration, int stacks) {
		long now = target.level().getGameTime();
		if (!target.getPersistentData().hasUUID(BRAND_OWNER)
				|| !owner.getUUID().equals(target.getPersistentData().getUUID(BRAND_OWNER))
				|| target.getPersistentData().getLong(BRAND_UNTIL) < now)
			target.getPersistentData().putInt(BRAND_STACKS, 0);
		target.getPersistentData().putUUID(BRAND_OWNER, owner.getUUID());
		target.getPersistentData().putLong(BRAND_UNTIL, now + duration);
		target.getPersistentData().putInt(BRAND_STACKS,
				Math.min(3, target.getPersistentData().getInt(BRAND_STACKS) + Math.max(1, stacks)));
	}

	private static int brandStacks(LivingEntity target, ServerPlayer owner) {
		if (!target.getPersistentData().hasUUID(BRAND_OWNER)
				|| !owner.getUUID().equals(target.getPersistentData().getUUID(BRAND_OWNER))
				|| target.getPersistentData().getLong(BRAND_UNTIL) < target.level().getGameTime())
			return 0;
		return target.getPersistentData().getInt(BRAND_STACKS);
	}

	private static float magicDamage(ServerPlayer player, double base, double intelligenceDivisor) {
		return (float) (base + variables(player).Intelligence / intelligenceDivisor);
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

	private static boolean drainMana(ServerPlayer player, int amount) {
		if (player.isCreative())
			return true;
		if (variables(player).MP < amount)
			return false;
		player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
			capability.MP = Math.max(0.0D, capability.MP - amount);
			capability.syncPlayerVariables(player);
		});
		CooldownManager.set(player, "mana_refresh", 35);
		return true;
	}

	private static boolean ready(ServerPlayer player, String skill) {
		if (!CooldownManager.isOnCooldown(player, skill))
			return true;
		player.displayClientMessage(Component.literal(skill + " is on cooldown").withStyle(ChatFormatting.RED), true);
		return false;
	}

	private static SololevelingModVariables.PlayerVariables variables(Entity entity) {
		return entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
				.orElse(new SololevelingModVariables.PlayerVariables());
	}

}
