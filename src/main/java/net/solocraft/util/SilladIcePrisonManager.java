package net.solocraft.util;

import net.solocraft.SololevelingMod;
import net.solocraft.entity.BarrierVfxEntity;
import net.solocraft.entity.SilladBossEntity;
import net.solocraft.network.SololevelingModVariables;

import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Server-authoritative ice prisons used by Sillad's companion-control attack.
 *
 * <p>The visible shell reuses the Barrier Mage prism renderer, including its
 * shader-pack-safe fallback. The captured summon remains a normal entity: it
 * is tethered rather than saved with NoAI, automatically attacks the shell,
 * and is released as soon as the construct breaks.</p>
 */
@EventBusSubscriber(modid = SololevelingMod.MODID)
public final class SilladIcePrisonManager {
	private static final String PRISON_MARKER = "slr_sillad_ice_prison";
	private static final String PRISON_CONSTRUCT = PRISON_MARKER + "_construct";
	private static final String PRISON_BOSS = PRISON_MARKER + "_boss";
	private static final String PRISON_OWNER = PRISON_MARKER + "_owner";

	private static final Map<UUID, PrisonSession> BY_TARGET = new HashMap<>();
	private static final Map<UUID, Set<UUID>> BY_BOSS = new HashMap<>();

	private SilladIcePrisonManager() {
	}

	/** Returns whether the supplied owned companion is safe to capture. */
	public static boolean canCapture(SilladBossEntity sillad,
			LivingEntity target) {
		if (sillad == null || target == null || target == sillad
				|| !target.isAlive() || target instanceof Player
				|| target.level() != sillad.level() || target.isPassenger()
				|| target.isVehicle() || isImprisoned(target))
			return false;
		UUID ownerId = ShadowMonarchManager.getShadowOwnerUUID(target);
		if (ownerId == null || !(sillad.level() instanceof ServerLevel level))
			return false;
		ServerPlayer owner = level.getServer().getPlayerList().getPlayer(ownerId);
		return owner != null && owner.isAlive() && owner.level() == level
				&& !owner.isCreative() && !owner.isSpectator();
	}

	/** Creates one breakable prison for each validated target in the wave. */
	public static int captureWave(SilladBossEntity sillad,
			Collection<? extends LivingEntity> requestedTargets) {
		if (sillad == null || requestedTargets == null
				|| !(sillad.level() instanceof ServerLevel level))
			return 0;
		int captured = 0;
		for (LivingEntity target : requestedTargets) {
			if (!canCapture(sillad, target))
				continue;
			UUID ownerId = ShadowMonarchManager.getShadowOwnerUUID(target);
			if (ownerId == null)
				continue;
			float radius = Mth.clamp(target.getBbWidth() * 0.72F + 0.55F,
					0.90F, 3.50F);
			float height = Mth.clamp(target.getBbHeight() + 0.45F,
					1.80F, 6.50F);
			float integrity = SilladBossRules.prisonIntegrity(
					sillad.getEngagedPlayerCount(), target.getMaxHealth(),
					ShadowMonarchManager.appliedShadowRank(target));
			BarrierVfxEntity prison = BarrierVfxEntity.spawn(level,
					target.position().add(0.0D, 0.04D, 0.0D),
					BarrierVfxEntity.SILLAD_ICE_PRISON, 5, radius, height,
					SilladBossRules.PRISON_DURATION_TICKS + 12,
					target.getYRot(), 0.0F, sillad,
					target, false, integrity, true);
			prison.setCustomNameVisible(true);
			updatePrisonName(prison);
			long now = level.getGameTime();
			PrisonSession session = new PrisonSession(level.dimension(),
					sillad.getUUID(), prison.getUUID(), target.getUUID(), ownerId,
					target.position(), now + SilladBossRules.PRISON_DURATION_TICKS,
					integrity,
					target.getTicksFrozen(), now);
			register(session);
			mark(target, session);
			if (target instanceof Mob mob)
				mob.setTarget(null);
			level.sendParticles(ParticleTypes.SNOWFLAKE, target.getX(),
					target.getEyeY(), target.getZ(), 38, radius * 0.45D,
					height * 0.25D, radius * 0.45D, 0.055D);
			level.playSound(null, target.blockPosition(),
					SoundEvents.BEACON_ACTIVATE, SoundSource.HOSTILE,
					0.85F, 1.62F);
			captured++;
		}
		return captured;
	}

	public static boolean isImprisoned(Entity entity) {
		if (entity == null || !entity.getPersistentData()
				.getBoolean(PRISON_MARKER))
			return false;
		if (entity.level().isClientSide())
			return true;
		PrisonSession known = BY_TARGET.get(entity.getUUID());
		if (known != null)
			return true;
		if (!(entity.level() instanceof ServerLevel level)
				|| !entity.getPersistentData().hasUUID(PRISON_CONSTRUCT)
				|| !entity.getPersistentData().hasUUID(PRISON_BOSS)
				|| !entity.getPersistentData().hasUUID(PRISON_OWNER)) {
			clearMarker(entity);
			return false;
		}
		Entity raw = level.getEntity(entity.getPersistentData()
				.getUUID(PRISON_CONSTRUCT));
		if (!(raw instanceof BarrierVfxEntity prison) || !prison.isActive()
				|| prison.getStyle() != BarrierVfxEntity.SILLAD_ICE_PRISON) {
			clearMarker(entity);
			return false;
		}
		long now = level.getGameTime();
		PrisonSession recovered = new PrisonSession(level.dimension(),
				entity.getPersistentData().getUUID(PRISON_BOSS), prison.getUUID(),
				entity.getUUID(), entity.getPersistentData().getUUID(PRISON_OWNER),
				entity.position(), now + Math.max(1,
						prison.getLifetime() - prison.tickCount),
				prison.getIntegrity(), entity instanceof LivingEntity living
						? living.getTicksFrozen() : 0, now);
		register(recovered);
		return true;
	}

	/** Sends feedback and returns true when manual dismissal must be rejected. */
	public static boolean guardManualDismiss(Player player) {
		if (player == null || player.level().isClientSide())
			return false;
		boolean blocked = hasImprisonedOwnedCompanion(player);
		if (blocked)
			player.displayClientMessage(Component.translatable(
					"message.sololeveling.sillad_prison.dismiss_blocked"), true);
		return blocked;
	}

	public static boolean hasImprisonedOwnedCompanion(Player owner) {
		if (owner == null)
			return false;
		UUID ownerId = owner.getUUID();
		for (PrisonSession session : new ArrayList<>(BY_TARGET.values()))
			if (ownerId.equals(session.ownerId))
				return true;
		if (!(owner instanceof ServerPlayer serverPlayer)
				|| serverPlayer.server == null)
			return false;
		for (ServerLevel level : serverPlayer.server.getAllLevels()) {
			for (Entity entity : level.getAllEntities()) {
				if (ownerId.equals(ShadowMonarchManager
						.getShadowOwnerUUID(entity)) && isImprisoned(entity))
					return true;
			}
		}
		return false;
	}

	public static int activeCountForBoss(UUID bossId) {
		Set<UUID> targets = bossId == null ? null : BY_BOSS.get(bossId);
		return targets == null ? 0 : targets.size();
	}

	public static void releaseAllForBoss(SilladBossEntity sillad) {
		if (sillad != null)
			releaseAllForBoss(sillad.getUUID());
	}

	public static void releaseAllForBoss(UUID bossId) {
		if (bossId == null)
			return;
		Set<UUID> targets = BY_BOSS.get(bossId);
		if (targets == null)
			return;
		for (UUID targetId : new HashSet<>(targets)) {
			PrisonSession session = BY_TARGET.get(targetId);
			if (session != null)
				release(session, true, false);
		}
	}

	@SubscribeEvent
	public static void onServerTick(ServerTickEvent.Post event) {
		if (false || BY_TARGET.isEmpty())
			return;
		MinecraftServer server = event.getServer();
		Map<UUID, DrainBatch> manaDrains = new HashMap<>();
		for (PrisonSession session : new ArrayList<>(BY_TARGET.values())) {
			ServerLevel level = server.getLevel(session.dimension);
			if (level == null) {
				release(session, false, false);
				continue;
			}
			Entity bossRaw = level.getEntity(session.bossId);
			Entity targetRaw = level.getEntity(session.targetId);
			Entity prisonRaw = level.getEntity(session.constructId);
			if (!(bossRaw instanceof SilladBossEntity boss) || !boss.isAlive()
					|| !(targetRaw instanceof LivingEntity target)
					|| !target.isAlive()
					|| !(prisonRaw instanceof BarrierVfxEntity prison)
					|| !prison.isActive()
					|| prison.getStyle() != BarrierVfxEntity.SILLAD_ICE_PRISON) {
				release(session, false, false);
				continue;
			}
			long now = level.getGameTime();
			if (now >= session.expiresAt) {
				release(session, true, true);
				continue;
			}
			ServerPlayer owner = server.getPlayerList().getPlayer(session.ownerId);
			if (owner == null || !owner.isAlive() || owner.level() != level) {
				release(session, true, false);
				continue;
			}
			tether(target, session);
			if (prison.getIntegrity() + 0.01F < session.lastIntegrity
					&& now >= session.nextDamageFeedbackAt) {
				session.nextDamageFeedbackAt = now + 3L;
				level.sendParticles(ParticleTypes.SNOWFLAKE, prison.getX(),
						prison.getY() + prison.getLength() * 0.5D, prison.getZ(),
						5, prison.getScale() * 0.35D, prison.getLength() * 0.18D,
						prison.getScale() * 0.35D, 0.025D);
				level.playSound(null, prison.blockPosition(),
						SoundEvents.AMETHYST_BLOCK_HIT, SoundSource.HOSTILE,
						0.34F, 1.72F);
			}
			session.lastIntegrity = prison.getIntegrity();

			if (now >= session.nextAttackAt) {
				session.nextAttackAt = now + 10L;
				attackPrison(target, prison);
				if (!prison.isAlive() || !prison.isActive()) {
					release(session, false, false);
					continue;
				}
			}
			if (now >= session.nextPulseAt) {
				session.nextPulseAt = now + 20L;
				regenerate(level, prison);
				applyNonlethalDot(boss, target);
				manaDrains.compute(session.ownerId, (ignored, batch) ->
						batch == null ? new DrainBatch(level, owner, target.position())
								: batch.add(target.position()));
			}
			updatePrisonName(prison);
		}
		for (DrainBatch batch : manaDrains.values())
			drainMana(batch);
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void onLivingAttack(LivingIncomingDamageEvent event) {
		Entity attacker = event.getSource().getDirectEntity();
		if (attacker instanceof Projectile projectile
				&& projectile.getOwner() != null)
			attacker = projectile.getOwner();
		if (attacker == null)
			attacker = event.getSource().getEntity();
		if (isImprisoned(attacker))
			event.setCanceled(true);
	}

	@SubscribeEvent
	public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
		releaseOwnedBy(event.getEntity().getUUID());
	}

	@SubscribeEvent
	public static void onPlayerChangedDimension(
			PlayerEvent.PlayerChangedDimensionEvent event) {
		releaseOwnedBy(event.getEntity().getUUID());
	}

	@SubscribeEvent
	public static void onServerStopped(ServerStoppedEvent event) {
		BY_TARGET.clear();
		BY_BOSS.clear();
	}

	private static void tether(LivingEntity target, PrisonSession session) {
		if (target instanceof Mob mob) {
			mob.setTarget(null);
			mob.getNavigation().stop();
		}
		target.setDeltaMovement(Vec3.ZERO);
		if (target.position().distanceToSqr(session.anchor) > 0.015625D)
			target.teleportTo(session.anchor.x, session.anchor.y,
					session.anchor.z);
		target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,
				5, 9, false, false, false));
		target.setTicksFrozen(Math.max(target.getTicksFrozen(), 120));
		target.hurtMarked = true;
	}

	private static void attackPrison(LivingEntity target,
			BarrierVfxEntity prison) {
		if (!(target instanceof Mob mob))
			return;
		mob.swing(InteractionHand.MAIN_HAND);
		float damage = SilladBossRules.prisonerAttackDamage(
				mob.getAttributeValue(Attributes.ATTACK_DAMAGE));
		prison.hurt(mob.damageSources().mobAttack(mob), damage);
	}

	private static void regenerate(ServerLevel level,
			BarrierVfxEntity prison) {
		if (prison.getIntegrity() >= prison.getMaxIntegrity())
			return;
		float repaired = Math.min(prison.getMaxIntegrity(),
				prison.getIntegrity()
					+ SilladBossRules.prisonRegeneration(
							prison.getMaxIntegrity()));
		prison.setIntegrity(repaired);
		level.sendParticles(ParticleTypes.END_ROD, prison.getX(),
				prison.getY() + prison.getLength() * 0.50D, prison.getZ(),
				6, prison.getScale() * 0.22D, prison.getLength() * 0.18D,
				prison.getScale() * 0.22D, -0.018D);
		if (Math.floorMod(prison.getId(), 4) == Math.floorMod(
				(int) (level.getGameTime() / 20L), 4))
			level.playSound(null, prison.blockPosition(),
					SoundEvents.BEACON_AMBIENT, SoundSource.HOSTILE,
					0.22F, 1.78F);
	}

	private static void updatePrisonName(BarrierVfxEntity prison) {
		int health = Math.max(0, Mth.ceil(prison.getIntegrity()));
		int maximum = Math.max(1, Mth.ceil(prison.getMaxIntegrity()));
		Component name = Component.translatable(
				"entity.sololeveling.sillad_ice_prison")
				.append(Component.literal("  " + health + "/" + maximum));
		if (!name.equals(prison.getCustomName()))
			prison.setCustomName(name);
	}

	private static void applyNonlethalDot(SilladBossEntity boss,
			LivingEntity target) {
		float raw = SilladBossRules.prisonDot(target.getMaxHealth(),
				boss.getEngagedPlayerCount());
		float allowed = Math.min(raw, Math.max(0.0F, target.getHealth() - 1.0F));
		if (allowed <= 0.0F)
			return;
		target.invulnerableTime = 0;
		target.hurt(boss.damageSources().indirectMagic(boss, boss), allowed);
	}

	private static void drainMana(DrainBatch batch) {
		ServerPlayer owner = batch.owner;
		if (owner == null || owner.isCreative())
			return;
		final double[] drained = {0.0D};
		owner.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY,
				null).ifPresent(capability -> {
			double requested = SilladBossRules.prisonManaDrain(
					capability.Mana, batch.count);
			drained[0] = Math.min(Math.max(0.0D, capability.MP), requested);
			capability.MP = Math.max(0.0D, capability.MP - drained[0]);
			capability.syncPlayerVariables(owner);
		});
		if (drained[0] <= 0.0D)
			return;
		CooldownManager.set(owner, "mana_refresh", 30);
		owner.displayClientMessage(Component.translatable(
				"message.sololeveling.sillad_prison.mana_drain",
				(int) Math.ceil(drained[0])), true);
		Vec3 end = owner.getBoundingBox().getCenter();
		Vec3 delta = end.subtract(batch.center);
		for (int index = 1; index <= 10; index++) {
			Vec3 point = batch.center.add(delta.scale(index / 10.0D));
			batch.level.sendParticles(index % 2 == 0
					? ParticleTypes.END_ROD : ParticleTypes.SNOWFLAKE,
					point.x, point.y, point.z, 1, 0.02D, 0.02D,
					0.02D, 0.0D);
		}
	}

	private static void releaseOwnedBy(UUID ownerId) {
		for (PrisonSession session : new ArrayList<>(BY_TARGET.values()))
			if (ownerId.equals(session.ownerId))
				release(session, true, false);
	}

	private static void release(PrisonSession session, boolean dissolve,
			boolean timeout) {
		BY_TARGET.remove(session.targetId);
		Set<UUID> bossTargets = BY_BOSS.get(session.bossId);
		if (bossTargets != null) {
			bossTargets.remove(session.targetId);
			if (bossTargets.isEmpty())
				BY_BOSS.remove(session.bossId);
		}
		MinecraftServer server = net.neoforged.neoforge.server.ServerLifecycleHooks
				.getCurrentServer();
		ServerLevel level = server == null ? null
				: server.getLevel(session.dimension);
		if (level == null)
			return;
		Entity target = level.getEntity(session.targetId);
		if (target != null) {
			clearMarker(target);
			if (target instanceof LivingEntity living)
				living.setTicksFrozen(Math.min(living.getTicksFrozen(),
						session.previousFrozenTicks));
		}
		Entity raw = level.getEntity(session.constructId);
		if (dissolve && raw instanceof BarrierVfxEntity prison
				&& prison.isActive()) {
			prison.dissolve();
			level.playSound(null, prison.blockPosition(),
					timeout ? SoundEvents.POWDER_SNOW_BREAK
							: SoundEvents.AMETHYST_BLOCK_RESONATE,
					SoundSource.HOSTILE, timeout ? 0.65F : 0.45F,
					timeout ? 0.72F : 1.55F);
		}
	}

	private static void register(PrisonSession session) {
		BY_TARGET.put(session.targetId, session);
		BY_BOSS.computeIfAbsent(session.bossId, ignored -> new HashSet<>())
				.add(session.targetId);
	}

	private static void mark(Entity target, PrisonSession session) {
		target.getPersistentData().putBoolean(PRISON_MARKER, true);
		target.getPersistentData().putUUID(PRISON_CONSTRUCT,
				session.constructId);
		target.getPersistentData().putUUID(PRISON_BOSS, session.bossId);
		target.getPersistentData().putUUID(PRISON_OWNER, session.ownerId);
	}

	private static void clearMarker(Entity target) {
		target.getPersistentData().remove(PRISON_MARKER);
		target.getPersistentData().remove(PRISON_CONSTRUCT);
		target.getPersistentData().remove(PRISON_BOSS);
		target.getPersistentData().remove(PRISON_OWNER);
	}

	private static final class PrisonSession {
		private final ResourceKey<Level> dimension;
		private final UUID bossId;
		private final UUID constructId;
		private final UUID targetId;
		private final UUID ownerId;
		private final Vec3 anchor;
		private final long expiresAt;
		private final int previousFrozenTicks;
		private float lastIntegrity;
		private long nextAttackAt;
		private long nextPulseAt;
		private long nextDamageFeedbackAt;

		private PrisonSession(ResourceKey<Level> dimension, UUID bossId,
				UUID constructId, UUID targetId, UUID ownerId, Vec3 anchor,
				long expiresAt, float integrity, int previousFrozenTicks,
				long now) {
			this.dimension = dimension;
			this.bossId = bossId;
			this.constructId = constructId;
			this.targetId = targetId;
			this.ownerId = ownerId;
			this.anchor = anchor;
			this.expiresAt = expiresAt;
			this.previousFrozenTicks = previousFrozenTicks;
			this.lastIntegrity = integrity;
			this.nextAttackAt = now + 8L;
			this.nextPulseAt = now + 20L;
		}
	}

	private static final class DrainBatch {
		private final ServerLevel level;
		private final ServerPlayer owner;
		private Vec3 center;
		private int count = 1;

		private DrainBatch(ServerLevel level, ServerPlayer owner, Vec3 center) {
			this.level = level;
			this.owner = owner;
			this.center = center;
		}

		private DrainBatch add(Vec3 position) {
			center = center.scale(count).add(position).scale(1.0D / ++count);
			return this;
		}
	}
}
