package net.solocraft.util;

import net.solocraft.SololevelingMod;
import net.solocraft.entity.AfterImageEntity;
import net.solocraft.entity.SlashEffectEntity;
import net.solocraft.entity.ThrownDaggerEntity;
import net.solocraft.init.SololevelingModEntities;
import net.solocraft.init.SololevelingModMobEffects;
import net.solocraft.network.ClassPassiveMessage;
import net.solocraft.network.SololevelingModVariables;
import net.solocraft.procedures.BloodLustProcedure;
import net.solocraft.procedures.SkillSlotHelper;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.projectile.ProjectileUtil;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server-authoritative combat implementation for the reworked Assassin kit.
 * Dagger Throw and Dagger Rush remain owned by {@link DaggerThrowManager}.
 */
@Mod.EventBusSubscriber(modid = SololevelingMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class AssassinSkillManager {
	public static final String GHOST_STEP = "Ghost Step";
	public static final String NIGHT_REND = "Night Rend";
	public static final String STEALTH = "Stealth";
	public static final String FLASH_CUT = "Flash Cut";
	public static final String DUALWIELD = "Dualwield";
	public static final String CRITICAL_ATTACK = "Critical Attack";
	public static final String MUTILATION = "Mutilation";
	public static final String MURDERIOUS_INTENT = "Murderious Intent";

	private static final ResourceKey<DamageType> ASSASSIN_DAMAGE = ResourceKey.create(
			Registries.DAMAGE_TYPE, new ResourceLocation("sololeveling:assassin"));
	private static final Map<String, String> LEGACY_NAMES = Map.of(
			"Shadowstep", GHOST_STEP,
			"Backstab", NIGHT_REND,
			"Quickslashes", FLASH_CUT);
	private static final List<String> REWORKED_SKILLS = List.of(
			GHOST_STEP, NIGHT_REND, STEALTH, FLASH_CUT, DUALWIELD,
			CRITICAL_ATTACK, MUTILATION, MURDERIOUS_INTENT);
	private static final Map<UUID, CombatState> STATES = new ConcurrentHashMap<>();

	private static final int MAX_TEMPO = 5;
	private static final int GHOST_MAX_CHARGES = 2;
	private static final int GHOST_RECHARGE_TICKS = 90;
	private static final String GHOST_CHARGES_TAG = "slr_assassin_ghost_charges";
	private static final String GHOST_RECHARGE_TAG = "slr_assassin_ghost_recharge";

	private AssassinSkillManager() {
	}

	public static boolean isReworkedSkill(String skill) {
		return REWORKED_SKILLS.contains(canonicalName(skill));
	}

	public static String canonicalName(String skill) {
		if (skill == null)
			return "";
		String cleaned = skill.trim();
		for (Map.Entry<String, String> entry : LEGACY_NAMES.entrySet()) {
			if (entry.getKey().equalsIgnoreCase(cleaned))
				return entry.getValue();
		}
		for (String canonical : REWORKED_SKILLS) {
			if (canonical.equalsIgnoreCase(cleaned))
				return canonical;
		}
		return cleaned;
	}

	public static boolean activateSkill(ServerPlayer player, String requestedSkill) {
		if (player == null || !player.isAlive())
			return false;
		String skill = canonicalName(requestedSkill);
		CombatState state = state(player);
		boolean cast = switch (skill) {
			case GHOST_STEP -> castGhostStep(player, state);
			case NIGHT_REND -> castNightRend(player, state);
			case STEALTH -> castStealth(player, state);
			case FLASH_CUT -> castFlashCut(player, state);
			case DUALWIELD -> castDualwield(player, state);
			case CRITICAL_ATTACK -> castCriticalAttack(player, state);
			case MUTILATION -> castMutilation(player, state);
			case MURDERIOUS_INTENT -> castMurderiousIntent(player);
			default -> false;
		};
		return cast;
	}

	private static boolean castGhostStep(ServerPlayer player, CombatState state) {
		long now = player.level().getGameTime();
		if (player.isCreative()) {
			state.ghostCharges = GHOST_MAX_CHARGES;
			state.nextGhostRecharge = 0L;
		} else {
			rechargeGhostStep(player, state, now);
			if (state.ghostCharges <= 0) {
				showCooldown(player, GHOST_STEP, Math.max(1L, state.nextGhostRecharge - now));
				return false;
			}
		}
		if (!consumeMana(player, 80.0D))
			return false;

		Vec3 start = player.position();
		Vec3 horizontal = horizontalDirection(player.getLookAngle());
		Vec3 intended = start.add(horizontal.scale(6.0D));
		BlockHitResult hit = player.serverLevel().clip(new ClipContext(
				player.getEyePosition(), player.getEyePosition().add(horizontal.scale(6.0D)),
				ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
		if (hit.getType() != HitResult.Type.MISS)
			intended = hit.getLocation().subtract(horizontal.scale(0.65D)).add(0.0D, -player.getEyeHeight(), 0.0D);
		Vec3 destination = furthestSafePosition(player, start, intended);
		player.teleportTo(destination.x, destination.y, destination.z);
		player.setDeltaMovement(Vec3.ZERO);
		player.hurtMarked = true;

		if (!player.isCreative()) {
			state.ghostCharges--;
			if (state.nextGhostRecharge <= now)
				state.nextGhostRecharge = now + GHOST_RECHARGE_TICKS;
		}
		state.ghostEvadeUntil = now + 4L;
		updateGhostStepCooldown(player, state, now);
		CooldownManager.set(player, "mana_refresh", 20);

		boolean crossedTarget = false;
		AABB path = new AABB(start, destination).inflate(1.1D);
		for (LivingEntity target : player.serverLevel().getEntitiesOfClass(
				LivingEntity.class, path, candidate -> validTarget(player, candidate))) {
			if (target.getBoundingBox().inflate(0.7D)
					.clip(start.add(0.0D, 0.9D, 0.0D), destination.add(0.0D, 0.9D, 0.0D)).isPresent()) {
				expose(state, target, now + 60L);
				crossedTarget = true;
			}
		}
		if (crossedTarget)
			addTempo(player, state, "ghost-cross");
		spawnSlash(player, destination.add(0.0D, 0.9D, 0.0D), -18.0F, 0.8F, 100);
		play(player, SoundEvents.ENDER_PEARL_THROW, 0.65F, 1.65F);
		message(player, "Ghost Step  " + state.ghostCharges + "/2");
		return true;
	}

	private static boolean castNightRend(ServerPlayer player, CombatState state) {
		if (!hasHeldDagger(player))
			return requiresDagger(player, false);
		if (!ready(player, NIGHT_REND) || !consumeMana(player, 220.0D))
			return false;

		LivingEntity target = findLookTarget(player, 14.0D);
		boolean struck = false;
		if (target != null) {
			Vec3 targetForward = horizontalDirection(target.getLookAngle());
			Vec3 behind = target.position().subtract(targetForward.scale(1.25D));
			Vec3 side = new Vec3(-targetForward.z, 0.0D, targetForward.x).scale(0.45D);
			Vec3 destination = firstSafePosition(player, List.of(
					behind, behind.add(side), behind.subtract(side), target.position().add(0.0D, 0.2D, 0.0D)));
			if (destination != null) {
				player.teleportTo(destination.x, destination.y, destination.z);

				float multiplier = 1.65F;
				if (isExposed(state, target, player.level().getGameTime()))
					multiplier += 0.25F;
				multiplier *= consumePerfectCut(player, state);
				struck = dealAssassinDamage(player, state, target, assassinPower(player) * multiplier);
				if (struck) {
					expose(state, target, player.level().getGameTime() + 60L);
					recordMutilationCut(player, state, target, "night-rend");
					addTempo(player, state, "night-rend");
					spawnCross(player, target.getBoundingBox().getCenter(), 1.05F, 102);
				}
			}
		} else {
			Vec3 start = player.position();
			Vec3 end = furthestSafePosition(player, start,
					start.add(horizontalDirection(player.getLookAngle()).scale(3.2D)));
			player.teleportTo(end.x, end.y, end.z);
			LivingEntity close = findLookTarget(player, 3.8D);
			if (close != null) {
				float multiplier = 1.35F * consumePerfectCut(player, state);
				struck = dealAssassinDamage(player, state, close, assassinPower(player) * multiplier);
				if (struck) {
					expose(state, close, player.level().getGameTime() + 60L);
					recordMutilationCut(player, state, close, "night-rend");
					addTempo(player, state, "night-rend");
					spawnCross(player, close.getBoundingBox().getCenter(), 0.9F, 102);
				}
			}
		}

		setAssassinCooldown(player, NIGHT_REND, 160);
		CooldownManager.set(player, "mana_refresh", 40);
		play(player, struck ? SoundEvents.PLAYER_ATTACK_CRIT : SoundEvents.PLAYER_ATTACK_SWEEP,
				0.85F, struck ? 1.55F : 1.25F);
		message(player, NIGHT_REND);
		return true;
	}

	private static boolean castStealth(ServerPlayer player, CombatState state) {
		long now = player.level().getGameTime();
		if (state.stealthUntil >= now) {
			Entity decoy = state.decoyId == null ? null : player.serverLevel().getEntity(state.decoyId);
			if (decoy != null) {
				Vec3 destination = firstSafePosition(player, List.of(
						decoy.position(), decoy.position().add(0.5D, 0.0D, 0.0D),
						decoy.position().add(-0.5D, 0.0D, 0.0D)));
				if (destination != null)
					player.teleportTo(destination.x, destination.y, destination.z);
			}
			endStealth(player, state);
			spawnCross(player, player.getBoundingBox().getCenter(), 0.7F, 101);
			play(player, SoundEvents.ILLUSIONER_MIRROR_MOVE, 0.65F, 1.35F);
			message(player, "Stealth swap");
			return true;
		}
		if (!ready(player, STEALTH) || !consumeMana(player, 300.0D))
			return false;

		AfterImageEntity decoy = new AfterImageEntity(SololevelingModEntities.AFTER_IMAGE.get(), player.serverLevel());
		decoy.moveTo(player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());
		decoy.setYHeadRot(player.getYHeadRot());
		decoy.setTexture("ghost");
		decoy.setSilent(true);
		decoy.setInvulnerable(true);
		player.serverLevel().addFreshEntity(decoy);

		state.stealthUntil = now + 80L;
		state.decoyId = decoy.getUUID();
		player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 80, 0, false, false, false));
		for (Mob mob : player.serverLevel().getEntitiesOfClass(Mob.class,
				player.getBoundingBox().inflate(12.0D), candidate -> candidate.getTarget() == player))
			mob.setTarget(decoy);

		setAssassinCooldown(player, STEALTH, 300);
		CooldownManager.set(player, "mana_refresh", 40);
		spawnCross(player, player.getBoundingBox().getCenter(), 0.7F, 101);
		play(player, SoundEvents.ILLUSIONER_MIRROR_MOVE, 0.65F, 1.15F);
		message(player, "Stealth  4s");
		return true;
	}

	private static boolean castFlashCut(ServerPlayer player, CombatState state) {
		if (!hasHeldDagger(player))
			return requiresDagger(player, false);
		if (!ready(player, FLASH_CUT) || !consumeMana(player, 120.0D))
			return false;

		Vec3 forward = horizontalDirection(player.getLookAngle());
		Vec3 origin = player.position().add(0.0D, player.getBbHeight() * 0.5D, 0.0D);
		List<LivingEntity> targets = new ArrayList<>(player.serverLevel().getEntitiesOfClass(
				LivingEntity.class, player.getBoundingBox().inflate(5.0D),
				candidate -> validTarget(player, candidate)));
		targets.sort((a, b) -> Double.compare(a.distanceToSqr(player), b.distanceToSqr(player)));
		int hits = 0;
		for (LivingEntity target : targets) {
			if (hits >= 12)
				break;
			Vec3 toTarget = target.getBoundingBox().getCenter().subtract(origin);
			if (toTarget.lengthSqr() > 27.0D || horizontalDirection(toTarget).dot(forward) < 0.42D
					|| !player.hasLineOfSight(target))
				continue;
			if (dealAssassinDamage(player, state, target, assassinPower(player) * 0.9F)) {
				recordMutilationCut(player, state, target, "flash-cut");
				hits++;
			}
		}
		if (hits > 0)
			addTempo(player, state, "flash-cut");
		Vec3 visual = origin.add(forward.scale(2.4D));
		spawnCross(player, visual, 1.05F, 101);
		setAssassinCooldown(player, FLASH_CUT, 80);
		CooldownManager.set(player, "mana_refresh", 30);
		play(player, SoundEvents.PLAYER_ATTACK_SWEEP, 0.9F, 1.6F);
		message(player, FLASH_CUT);
		return true;
	}

	private static boolean castDualwield(ServerPlayer player, CombatState state) {
		if (!DaggerThrowManager.isDagger(player.getMainHandItem())
				|| !DaggerThrowManager.isDagger(player.getOffhandItem()))
			return requiresDagger(player, true);
		if (!ready(player, DUALWIELD) || !consumeMana(player, 350.0D))
			return false;

		state.dualwieldUntil = player.level().getGameTime() + 140L;
		state.dualwieldHits = 0;
		setAssassinCooldown(player, DUALWIELD, 360);
		CooldownManager.set(player, "mana_refresh", 50);
		play(player, SoundEvents.ARMOR_EQUIP_CHAIN, 0.7F, 1.65F);
		message(player, "Dualwield  7s");
		return true;
	}

	private static boolean castCriticalAttack(ServerPlayer player, CombatState state) {
		if (!hasHeldDagger(player))
			return requiresDagger(player, false);
		if (!ready(player, CRITICAL_ATTACK) || !consumeMana(player, 160.0D))
			return false;

		state.counterUntil = player.level().getGameTime() + 7L;
		setAssassinCooldown(player, CRITICAL_ATTACK, 180);
		CooldownManager.set(player, "mana_refresh", 30);
		play(player, SoundEvents.AMETHYST_BLOCK_CHIME, 0.7F, 1.8F);
		message(player, "Critical Attack  counter");
		return true;
	}

	private static boolean castMutilation(ServerPlayer player, CombatState state) {
		long now = player.level().getGameTime();
		if (state.mutilationTarget != null && state.mutilationUntil >= now) {
			detonateMutilation(player, state, false);
			return true;
		}
		if (!hasHeldDagger(player))
			return requiresDagger(player, false);
		if (!ready(player, MUTILATION))
			return false;
		LivingEntity target = findLookTarget(player, 18.0D);
		if (target == null) {
			message(player, "Mutilation needs a target");
			return false;
		}
		if (!consumeMana(player, 450.0D))
			return false;

		state.mutilationTarget = target.getUUID();
		state.mutilationUntil = now + 140L;
		state.mutilationCuts = 0;
		state.lastMutilationCut = Long.MIN_VALUE;
		state.lastMutilationAction = "";
		// Publish the complete time-to-ready immediately. Detonation replaces this
		// with the normal post-detonation cooldown while remaining recastable here.
		setAssassinCooldown(player, MUTILATION, 540);
		CooldownManager.set(player, "mana_refresh", 50);
		spawnSlash(player, target.getBoundingBox().getCenter(), -35.0F, 0.65F, 100);
		play(player, SoundEvents.AMETHYST_BLOCK_CHIME, 0.7F, 1.25F);
		message(player, "Mutilation marked");
		return true;
	}

	private static boolean castMurderiousIntent(ServerPlayer player) {
		if (!ready(player, MURDERIOUS_INTENT) || !consumeMana(player, 600.0D))
			return false;
		BloodLustProcedure.execute(player.serverLevel(), player.getX(), player.getY(), player.getZ(), player);
		CooldownManager.set(player, "mana_refresh", 60);
		play(player, SoundEvents.WITHER_AMBIENT, 0.45F, 1.7F);
		message(player, MURDERIOUS_INTENT);
		return true;
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void onLivingAttack(LivingAttackEvent event) {
		if (!(event.getEntity() instanceof ServerPlayer player))
			return;
		CombatState state = STATES.get(player.getUUID());
		if (state == null || state.internalDamage)
			return;
		long now = player.level().getGameTime();
		Entity attackerEntity = event.getSource().getEntity();

		if (attackerEntity instanceof LivingEntity attacker
				&& !event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
			if (state.counterUntil >= now && event.getSource().getDirectEntity() == attacker) {
				state.counterUntil = 0L;
				event.setCanceled(true);
				Vec3 behind = attacker.position().subtract(horizontalDirection(attacker.getLookAngle()).scale(1.2D));
				Vec3 destination = firstSafePosition(player, List.of(behind, player.position()));
				if (destination != null)
					player.teleportTo(destination.x, destination.y, destination.z);
				state.riposteUntil = now + 40L;
				expose(state, attacker, now + 60L);
				addTempo(player, state, "critical-counter");
				spawnCross(player, attacker.getBoundingBox().getCenter(), 0.85F, 103);
				play(player, SoundEvents.PLAYER_ATTACK_CRIT, 0.9F, 1.7F);
				message(player, "Riposte ready");
				return;
			}
			if (state.ghostEvadeUntil >= now) {
				state.ghostEvadeUntil = 0L;
				event.setCanceled(true);
				expose(state, attacker, now + 60L);
				addTempo(player, state, "ghost-evade");
				Vec3 right = new Vec3(-player.getLookAngle().z, 0.0D, player.getLookAngle().x).normalize();
				Vec3 destination = firstSafePosition(player, List.of(
						player.position().add(right.scale(1.4D)),
						player.position().subtract(right.scale(1.4D))));
				if (destination != null)
					player.teleportTo(destination.x, destination.y, destination.z);
				spawnSlash(player, player.getBoundingBox().getCenter(), 20.0F, 0.7F, 100);
				return;
			}
		}
		if (state.stealthUntil >= now)
			endStealth(player, state);
	}

	@SubscribeEvent(priority = EventPriority.HIGH)
	public static void onLivingHurt(LivingHurtEvent event) {
		if (!(event.getSource().getEntity() instanceof ServerPlayer attacker))
			return;
		LivingEntity target = event.getEntity();
		CombatState state = STATES.get(attacker.getUUID());
		if (state == null || state.internalDamage || !validTarget(attacker, target))
			return;
		long now = attacker.level().getGameTime();
		Entity direct = event.getSource().getDirectEntity();
		boolean daggerMelee = direct == attacker && DaggerThrowManager.isDagger(attacker.getMainHandItem());
		boolean thrownDagger = direct instanceof ThrownDaggerEntity;

		if (isExposed(state, target, now))
			event.setAmount(event.getAmount() * 1.15F);
		if (state.stealthUntil >= now) {
			event.setAmount(event.getAmount() * 1.25F);
			endStealth(attacker, state);
			spawnCross(attacker, target.getBoundingBox().getCenter(), 0.85F, 102);
		}
		if (daggerMelee && state.riposteUntil >= now) {
			state.riposteUntil = 0L;
			event.setAmount(event.getAmount() * 1.5F * consumePerfectCut(attacker, state));
			spawnCross(attacker, target.getBoundingBox().getCenter(), 0.95F, 103);
			play(attacker, SoundEvents.PLAYER_ATTACK_CRIT, 0.9F, 1.55F);
		}

		if (daggerMelee || thrownDagger) {
			addTempo(attacker, state, thrownDagger ? "thrown-dagger" : "dagger-melee");
			recordMutilationCut(attacker, state, target,
					thrownDagger ? "thrown-dagger" : "dagger-melee");
		}
		if (daggerMelee && state.dualwieldUntil >= now
				&& DaggerThrowManager.isDagger(attacker.getOffhandItem())
				&& attacker.getAttackStrengthScale(0.5F) >= 0.85F) {
			int hitIndex = ++state.dualwieldHits;
			UUID targetId = target.getUUID();
			SololevelingMod.queueServerWork(2, () -> dualwieldFollowup(attacker, targetId, hitIndex));
		}
	}

	@SubscribeEvent
	public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
		if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player))
			return;
		CombatState state = state(player);
		long now = player.level().getGameTime();
		rechargeGhostStep(player, state, now);
		state.exposed.entrySet().removeIf(entry -> entry.getValue() < now);
		if (state.tempo > 0 && state.nextTempoDecay <= now) {
			state.tempo--;
			state.nextTempoDecay = state.tempo > 0 ? now + 30L : Long.MAX_VALUE;
			syncTempo(player, state);
		}
		if (state.stealthUntil > 0L && state.stealthUntil < now)
			endStealth(player, state);
		if (state.mutilationTarget != null && state.mutilationUntil < now)
			detonateMutilation(player, state, false);
	}

	@SubscribeEvent
	public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
		if (event.getEntity() instanceof ServerPlayer player) {
			migrateLegacySkills(player);
			CombatState state = state(player);
			state.tempo = 0;
			syncTempo(player, state);
		}
	}

	@SubscribeEvent
	public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
		if (event.getEntity() instanceof ServerPlayer player) {
			migrateLegacySkills(player);
			clearState(player);
			syncTempo(player, state(player));
		}
	}

	@SubscribeEvent
	public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
		if (event.getEntity() instanceof ServerPlayer player)
			endStealth(player, state(player));
	}

	@SubscribeEvent
	public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
		if (event.getEntity() instanceof ServerPlayer player)
			clearState(player);
	}

	@SubscribeEvent
	public static void onDeath(LivingDeathEvent event) {
		if (event.getEntity() instanceof ServerPlayer player)
			clearState(player);
	}

	@SubscribeEvent
	public static void onServerStopped(ServerStoppedEvent event) {
		STATES.clear();
	}

	public static void migrateLegacySkills(ServerPlayer player) {
		player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(vars -> {
			boolean changed = false;
			String migratedList = migrateList(vars.Plist);
			if (!migratedList.equals(vars.Plist)) {
				vars.Plist = migratedList;
				changed = true;
			}
			for (int slot = 1; slot <= 16; slot++) {
				String old = SkillSlotHelper.getSlot(vars, slot);
				String migrated = canonicalName(old);
				if (!migrated.equals(old)) {
					SkillSlotHelper.setSlot(vars, slot, migrated);
					changed = true;
				}
			}
			String selected = canonicalName(vars.PselectedPower);
			if (!selected.equals(vars.PselectedPower)) {
				vars.PselectedPower = selected;
				changed = true;
			}
			if (changed)
				vars.syncPlayerVariables(player);
		});
		player.getPersistentData().remove("Critical_Attack_Targetting");
		player.getPersistentData().remove("CriticalAttackTarget");
		player.getPersistentData().remove("Mutilation_Targetting");
		player.getPersistentData().remove("MutilationTarget");
		player.removeEffect(SololevelingModMobEffects.DUAL_WIELDING.get());
		for (Map.Entry<String, String> entry : LEGACY_NAMES.entrySet()) {
			int remaining = CooldownManager.getRemainingTicks(player, entry.getKey());
			if (remaining > 0) {
				setAssassinCooldown(player, entry.getValue(), remaining);
				CooldownManager.clear(player, entry.getKey());
			}
		}
	}

	private static String migrateList(String encoded) {
		LinkedHashMap<String, String> unique = new LinkedHashMap<>();
		if (encoded != null) {
			for (String raw : encoded.split(",")) {
				String cleaned = raw.replaceFirst("^\\.", "").trim();
				if (cleaned.isEmpty())
					continue;
				String canonical = canonicalName(cleaned);
				unique.putIfAbsent(canonical.toLowerCase(Locale.ROOT), canonical);
			}
		}
		StringBuilder result = new StringBuilder(".");
		for (String skill : unique.values())
			result.append(skill).append(',');
		return result.toString();
	}

	private static void dualwieldFollowup(ServerPlayer player, UUID targetId, int hitIndex) {
		if (!player.isAlive())
			return;
		CombatState state = STATES.get(player.getUUID());
		if (state == null || state.dualwieldUntil < player.level().getGameTime()
				|| !DaggerThrowManager.isDagger(player.getMainHandItem())
				|| !DaggerThrowManager.isDagger(player.getOffhandItem()))
			return;
		Entity entity = player.serverLevel().getEntity(targetId);
		if (!(entity instanceof LivingEntity primary) || !validTarget(player, primary)
				|| primary.distanceToSqr(player) > 49.0D)
			return;

		boolean cross = hitIndex % 4 == 0;
		float damage = assassinPower(player) * (cross ? 0.65F : 0.45F);
		primary.invulnerableTime = 0;
		if (dealAssassinDamage(player, state, primary, damage))
			recordMutilationCut(player, state, primary, cross ? "dual-cross" : "dual-followup");
		spawnSlash(player, primary.getBoundingBox().getCenter(),
				cross ? 40.0F : (hitIndex % 2 == 0 ? 26.0F : -26.0F), cross ? 0.95F : 0.72F, 100);
		if (cross) {
			int extra = 0;
			for (LivingEntity nearby : player.serverLevel().getEntitiesOfClass(
					LivingEntity.class, primary.getBoundingBox().inflate(2.3D),
					candidate -> candidate != primary && validTarget(player, candidate))) {
				nearby.invulnerableTime = 0;
				dealAssassinDamage(player, state, nearby, assassinPower(player) * 0.35F);
				if (++extra >= 2)
					break;
			}
			spawnSlash(player, primary.getBoundingBox().getCenter(), -40.0F, 0.95F, 101);
		}
		play(player, SoundEvents.PLAYER_ATTACK_SWEEP, 0.65F, cross ? 1.75F : 1.45F);
	}

	private static void detonateMutilation(ServerPlayer player, CombatState state, boolean invalid) {
		UUID targetId = state.mutilationTarget;
		int cuts = state.mutilationCuts;
		clearMutilation(state);
		Entity entity = targetId == null ? null : player.serverLevel().getEntity(targetId);
		if (invalid || !(entity instanceof LivingEntity target) || !validTarget(player, target)) {
			setAssassinCooldown(player, MUTILATION, 200);
			message(player, "Mutilation target lost");
			return;
		}
		float damage = assassinPower(player) * (1.0F + 0.3F * cuts)
				* consumePerfectCut(player, state);
		dealAssassinDamage(player, state, target, damage);
		Vec3 center = target.getBoundingBox().getCenter();
		for (int i = 0; i < Math.max(2, cuts); i++) {
			int index = i;
			SololevelingMod.queueServerWork(Math.min(4, i), () ->
					spawnSlash(player, center, -55.0F + index * 29.0F,
							0.7F + Math.min(0.35F, index * 0.04F), 103));
		}
		setAssassinCooldown(player, MUTILATION, 400);
		play(player, SoundEvents.PLAYER_ATTACK_CRIT, 1.0F, 1.35F);
		message(player, "Mutilation  " + cuts + " cuts");
	}

	private static void recordMutilationCut(ServerPlayer player, CombatState state,
			LivingEntity target, String action) {
		long now = player.level().getGameTime();
		if (state.mutilationTarget == null || !state.mutilationTarget.equals(target.getUUID())
				|| state.mutilationUntil < now || state.mutilationCuts >= 6)
			return;
		if (now - state.lastMutilationCut < 3L
				&& action.equals(state.lastMutilationAction))
			return;
		state.mutilationCuts++;
		state.lastMutilationCut = now;
		state.lastMutilationAction = action;
		message(player, "Mutilation  " + state.mutilationCuts + "/6");
	}

	private static boolean dealAssassinDamage(ServerPlayer player, CombatState state,
			LivingEntity target, float amount) {
		if (!validTarget(player, target))
			return false;
		DamageSource source = new DamageSource(player.level().registryAccess()
				.registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(ASSASSIN_DAMAGE), player);
		state.internalDamage = true;
		try {
			boolean hurt = target.hurt(source, Math.max(0.5F, amount));
			if (hurt)
				target.setLastHurtByPlayer(player);
			return hurt;
		} finally {
			state.internalDamage = false;
		}
	}

	private static float assassinPower(ServerPlayer player) {
		SololevelingModVariables.PlayerVariables vars = player.getCapability(
				SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
				.orElse(new SololevelingModVariables.PlayerVariables());
		double attack = player.getAttributeValue(Attributes.ATTACK_DAMAGE);
		double offhand = DaggerThrowManager.isDagger(player.getOffhandItem()) ? attack * 0.25D : 0.0D;
		return (float) Math.max(4.0D,
				attack + offhand + vars.Speed * 0.08D + vars.perception * 0.06D);
	}

	private static void addTempo(ServerPlayer player, CombatState state, String action) {
		if (playerClass(player) != 1)
			return;
		long now = player.level().getGameTime();
		long last = state.lastTempoActions.getOrDefault(action, Long.MIN_VALUE);
		if (last != Long.MIN_VALUE && now - last < 6L)
			return;
		state.lastTempoActions.put(action, now);
		if (state.tempo < MAX_TEMPO) {
			state.tempo++;
			syncTempo(player, state);
		}
		state.nextTempoDecay = now + 100L;
	}

	private static float consumePerfectCut(ServerPlayer player, CombatState state) {
		if (state.tempo < MAX_TEMPO)
			return 1.0F;
		state.tempo = 0;
		state.nextTempoDecay = Long.MAX_VALUE;
		state.ghostCharges = Math.min(GHOST_MAX_CHARGES, state.ghostCharges + 1);
		if (state.ghostCharges >= GHOST_MAX_CHARGES)
			state.nextGhostRecharge = 0L;
		updateGhostStepCooldown(player, state, player.level().getGameTime());
		syncTempo(player, state);
		message(player, "Perfect Cut");
		return 1.3F;
	}

	private static void rechargeGhostStep(ServerPlayer player, CombatState state, long now) {
		if (state.ghostCharges >= GHOST_MAX_CHARGES || state.nextGhostRecharge > now)
			return;
		state.ghostCharges++;
		if (state.ghostCharges < GHOST_MAX_CHARGES)
			state.nextGhostRecharge = now + GHOST_RECHARGE_TICKS;
		else
			state.nextGhostRecharge = 0L;
		updateGhostStepCooldown(player, state, now);
	}

	private static void updateGhostStepCooldown(ServerPlayer player, CombatState state, long now) {
		if (player.isCreative()) {
			state.ghostCharges = GHOST_MAX_CHARGES;
			state.nextGhostRecharge = 0L;
		}
		persistGhostStepState(player, state);
		if (state.ghostCharges >= GHOST_MAX_CHARGES || state.nextGhostRecharge <= 0L) {
			CooldownManager.clear(player, GHOST_STEP);
			return;
		}
		setAssassinCooldown(player, GHOST_STEP,
				(int) Math.max(1L, state.nextGhostRecharge - now));
	}

	private static void persistGhostStepState(ServerPlayer player, CombatState state) {
		CompoundTag data = player.getPersistentData();
		if (state.ghostCharges >= GHOST_MAX_CHARGES && state.nextGhostRecharge <= 0L) {
			data.remove(GHOST_CHARGES_TAG);
			data.remove(GHOST_RECHARGE_TAG);
			return;
		}
		data.putInt(GHOST_CHARGES_TAG, state.ghostCharges);
		if (state.nextGhostRecharge > 0L)
			data.putLong(GHOST_RECHARGE_TAG, state.nextGhostRecharge);
		else
			data.remove(GHOST_RECHARGE_TAG);
	}

	private static LivingEntity findLookTarget(ServerPlayer player, double range) {
		Vec3 start = player.getEyePosition();
		Vec3 end = start.add(player.getLookAngle().normalize().scale(range));
		BlockHitResult blockHit = player.serverLevel().clip(new ClipContext(
				start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
		if (blockHit.getType() != HitResult.Type.MISS)
			end = blockHit.getLocation();
		AABB search = player.getBoundingBox().expandTowards(end.subtract(start)).inflate(1.3D);
		EntityHitResult hit = ProjectileUtil.getEntityHitResult(player, start, end, search,
				entity -> entity instanceof LivingEntity living && validTarget(player, living),
				start.distanceToSqr(end));
		return hit != null && hit.getEntity() instanceof LivingEntity living ? living : null;
	}

	private static boolean validTarget(Player player, LivingEntity target) {
		if (target == null || target == player || !target.isAlive() || !target.isAttackable()
				|| target.isInvulnerable() || target instanceof ArmorStand)
			return false;
		if (player.isAlliedTo(target) || target.isAlliedTo(player))
			return false;
		if (ShadowMonarchManager.isOwnedShadow(target, player))
			return false;
		if (target instanceof TamableAnimal tame && player.getUUID().equals(tame.getOwnerUUID()))
			return false;
		String party = player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
				.map(vars -> vars.party).orElse("");
		String targetParty = target.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
				.map(vars -> vars.party).orElse("");
		if (!party.isBlank() && party.equals(targetParty))
			return false;
		if (target instanceof Player other)
			return !other.isCreative() && !other.isSpectator() && player.canHarmPlayer(other);
		return true;
	}

	private static Vec3 furthestSafePosition(ServerPlayer player, Vec3 start, Vec3 intended) {
		Vec3 delta = intended.subtract(start);
		for (int step = 10; step >= 0; step--) {
			Vec3 candidate = start.add(delta.scale(step / 10.0D));
			if (isSafePosition(player, candidate))
				return candidate;
		}
		return start;
	}

	private static Vec3 firstSafePosition(ServerPlayer player, List<Vec3> candidates) {
		for (Vec3 candidate : candidates) {
			if (isSafePosition(player, candidate))
				return candidate;
		}
		return null;
	}

	private static boolean isSafePosition(ServerPlayer player, Vec3 position) {
		BlockPos blockPos = BlockPos.containing(position);
		if (!player.serverLevel().hasChunkAt(blockPos)
				|| !player.serverLevel().getWorldBorder().isWithinBounds(blockPos))
			return false;
		AABB moved = player.getBoundingBox().move(position.subtract(player.position()));
		return player.serverLevel().noCollision(player, moved);
	}

	private static Vec3 horizontalDirection(Vec3 direction) {
		Vec3 horizontal = new Vec3(direction.x, 0.0D, direction.z);
		return horizontal.lengthSqr() < 0.0001D ? new Vec3(0.0D, 0.0D, 1.0D) : horizontal.normalize();
	}

	private static void expose(CombatState state, LivingEntity target, long expiry) {
		state.exposed.put(target.getUUID(), expiry);
	}

	private static boolean isExposed(CombatState state, LivingEntity target, long now) {
		return state.exposed.getOrDefault(target.getUUID(), 0L) >= now;
	}

	private static boolean ready(ServerPlayer player, String key) {
		if (player.isCreative()) {
			CooldownManager.clear(player, key);
			return true;
		}
		if (!CooldownManager.isOnCooldown(player, key))
			return true;
		showCooldown(player, key, CooldownManager.getRemainingTicks(player, key));
		return false;
	}

	private static void setAssassinCooldown(ServerPlayer player, String key, int durationTicks) {
		if (player.isCreative())
			CooldownManager.clear(player, key);
		else
			CooldownManager.setFullDuration(player, key, durationTicks);
	}

	private static void showCooldown(ServerPlayer player, String skill, long ticks) {
		message(player, skill + "  " + Math.max(1L, (ticks + 19L) / 20L) + "s");
	}

	private static boolean consumeMana(ServerPlayer player, double cost) {
		if (player.isCreative())
			return true;
		SololevelingModVariables.PlayerVariables vars = player.getCapability(
				SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(null);
		if (vars == null || vars.MP < cost) {
			player.displayClientMessage(Component.literal("Not enough MP")
					.withStyle(ChatFormatting.RED), true);
			return false;
		}
		vars.MP -= cost;
		vars.syncPlayerVariables(player);
		return true;
	}

	private static boolean hasHeldDagger(ServerPlayer player) {
		return DaggerThrowManager.isDagger(player.getMainHandItem())
				|| DaggerThrowManager.isDagger(player.getOffhandItem());
	}

	private static boolean requiresDagger(ServerPlayer player, boolean bothHands) {
		message(player, bothHands ? "Equip a dagger in both hands" : "Equip a dagger");
		return false;
	}

	private static int playerClass(ServerPlayer player) {
		return (int) Math.round(player.getCapability(
				SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
				.map(vars -> vars.Classes).orElse(0.0D));
	}

	private static void spawnCross(ServerPlayer player, Vec3 center, float scale, int variant) {
		spawnSlash(player, center, -42.0F, scale, variant);
		spawnSlash(player, center, 42.0F, scale, variant);
	}

	private static void spawnSlash(ServerPlayer player, Vec3 center, float roll, float scale, int variant) {
		SlashEffectEntity.spawn(player.serverLevel(), player,
				center.x, center.y, center.z, player.getYRot(), player.getXRot(),
				roll, scale, 0.0F, variant);
	}

	private static void play(ServerPlayer player, net.minecraft.sounds.SoundEvent sound,
			float volume, float pitch) {
		player.level().playSound(null, player.blockPosition(), sound,
				SoundSource.PLAYERS, volume, pitch);
	}

	private static void message(ServerPlayer player, String text) {
		player.displayClientMessage(Component.literal(text)
				.withStyle(ChatFormatting.AQUA), true);
	}

	private static CombatState state(ServerPlayer player) {
		return STATES.computeIfAbsent(player.getUUID(), ignored -> loadState(player));
	}

	private static CombatState loadState(ServerPlayer player) {
		CombatState state = new CombatState();
		CompoundTag data = player.getPersistentData();
		long now = player.level().getGameTime();
		boolean persistedState = data.contains(GHOST_CHARGES_TAG, Tag.TAG_INT);
		boolean reconcileCooldown = persistedState;
		if (persistedState) {
			state.ghostCharges = Mth.clamp(data.getInt(GHOST_CHARGES_TAG), 0, GHOST_MAX_CHARGES);
			state.nextGhostRecharge = data.getLong(GHOST_RECHARGE_TAG);
			if (state.nextGhostRecharge > now + GHOST_RECHARGE_TICKS)
				state.nextGhostRecharge = now + GHOST_RECHARGE_TICKS;
			while (state.ghostCharges < GHOST_MAX_CHARGES
					&& state.nextGhostRecharge > 0L && state.nextGhostRecharge <= now) {
				state.ghostCharges++;
				state.nextGhostRecharge = state.ghostCharges < GHOST_MAX_CHARGES
						? state.nextGhostRecharge + GHOST_RECHARGE_TICKS : 0L;
			}
			if (state.ghostCharges < GHOST_MAX_CHARGES && state.nextGhostRecharge <= 0L)
				state.nextGhostRecharge = now + GHOST_RECHARGE_TICKS;
		} else {
			int remaining = CooldownManager.getRemainingTicks(player, GHOST_STEP);
			if (remaining > 0) {
				state.ghostCharges = 0;
				state.nextGhostRecharge = now + Math.min(remaining, GHOST_RECHARGE_TICKS);
				reconcileCooldown = true;
			}
		}
		if (reconcileCooldown)
			updateGhostStepCooldown(player, state, now);
		return state;
	}

	private static void syncTempo(ServerPlayer player, CombatState state) {
		SololevelingMod.PACKET_HANDLER.send(PacketDistributor.PLAYER.with(() -> player),
				new ClassPassiveMessage(0, state.tempo));
	}

	private static void endStealth(ServerPlayer player, CombatState state) {
		if (state.decoyId != null) {
			if (player.getServer() != null) {
				for (ServerLevel level : player.getServer().getAllLevels()) {
					Entity decoy = level.getEntity(state.decoyId);
					if (decoy != null) {
						decoy.discard();
						break;
					}
				}
			}
		}
		state.decoyId = null;
		state.stealthUntil = 0L;
		player.removeEffect(MobEffects.INVISIBILITY);
	}

	private static void clearMutilation(CombatState state) {
		state.mutilationTarget = null;
		state.mutilationUntil = 0L;
		state.mutilationCuts = 0;
		state.lastMutilationCut = Long.MIN_VALUE;
		state.lastMutilationAction = "";
	}

	private static void clearState(ServerPlayer player) {
		CombatState state = STATES.remove(player.getUUID());
		if (state != null)
			endStealth(player, state);
	}

	private static final class CombatState {
		private int tempo;
		private long nextTempoDecay = Long.MAX_VALUE;
		private final Map<String, Long> lastTempoActions = new LinkedHashMap<>();
		private final Map<UUID, Long> exposed = new ConcurrentHashMap<>();
		private int ghostCharges = GHOST_MAX_CHARGES;
		private long nextGhostRecharge;
		private long ghostEvadeUntil;
		private long stealthUntil;
		private UUID decoyId;
		private long counterUntil;
		private long riposteUntil;
		private long dualwieldUntil;
		private int dualwieldHits;
		private UUID mutilationTarget;
		private long mutilationUntil;
		private int mutilationCuts;
		private long lastMutilationCut = Long.MIN_VALUE;
		private String lastMutilationAction = "";
		private boolean internalDamage;
	}
}
