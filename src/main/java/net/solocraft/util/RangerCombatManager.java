package net.solocraft.util;

import net.solocraft.entity.ManaArrowEntity;
import net.solocraft.init.SololevelingModEntities;
import net.solocraft.network.RangerStateMessage;
import net.solocraft.network.SololevelingModVariables;
import net.solocraft.procedures.BackStepProcedure;
import net.solocraft.procedures.SkillSlotHelper;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.ArrowLooseEvent;
import net.minecraftforge.event.entity.player.ArrowNockEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.server.ServerLifecycleHooks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * Server-authoritative Ranger combat runtime.
 *
 * <p>The manager intentionally keeps scheduled attacks bounded: one Rapid Fire
 * sequence per player, one Fivefold state per player, five pulses per Arrow
 * Shower, and at most five ordinary Mana Arrows per player.</p>
 */
@Mod.EventBusSubscriber
public final class RangerCombatManager {
	public static final String MANA_QUIVER = "Mana Quiver";
	public static final String BACK_STEP = "Back Step";
	public static final String HAWKEYE = "Hawkeye";
	public static final String RAPID_FIRE = "Rapid Fire";
	public static final String HIGH_VALUE_TARGET = "High Value Target";
	public static final String SHARPSHOOTER = "Sharpshooter";
	public static final String ARROW_SHOWER = "Arrow Shower";
	public static final String LEGACY_PROXIMITY_TRAP = "Proximity Trap";
	public static final String HYPER_FOCUS = "Hyper Focus";

	private static final ResourceKey<net.minecraft.world.damagesource.DamageType> RANGER_DAMAGE =
			ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation("sololeveling", "ranger"));
	private static final List<String> CORE_SKILLS = List.of(
			MANA_QUIVER, BACK_STEP, HAWKEYE, RAPID_FIRE, HIGH_VALUE_TARGET,
			SHARPSHOOTER, ARROW_SHOWER, HYPER_FOCUS);

	private static final String QUIVER_ACTIVE = "slr_ranger_quiver_active";
	private static final String CORE_RECONCILED = "slr_ranger_core_reconciled_v4";
	private static final String CORE_RECONCILED_RANK = "slr_ranger_core_rank_v4";
	private static final String LOCK_TARGET = "slr_ranger_lock_target";
	private static final String HAWKEYE_UNTIL = "slr_ranger_hawkeye_until";
	private static final String HYPER_UNTIL = "slr_ranger_hyper_until";
	private static final String QUICK_DRAW_UNTIL = "slr_ranger_quick_draw_until";
	private static final String MARK_TARGET = "slr_ranger_mark_target";
	private static final String MARK_DIMENSION = "slr_ranger_mark_dimension";
	private static final String MARK_UNTIL = "slr_ranger_mark_until";
	private static final String SUNDER_OWNER = "slr_ranger_sunder_owner";
	private static final String SUNDER_UNTIL = "slr_ranger_sunder_until";
	private static final String FIVEFOLD_CHARGES = "slr_ranger_fivefold_charges";
	private static final String FIVEFOLD_UNTIL = "slr_ranger_fivefold_until";
	private static final String CLIENT_STATE_ACTIVE = "slr_ranger_client_state_active";

	private static final String HAWKEYE_SOURCE = "skill:ranger_hawkeye";
	private static final String MARK_SOURCE = "skill:ranger_mark";

	private static final int FULL_DRAW_TICKS = 20;
	private static final int STAGE_ONE_TICKS = 27;
	private static final int STAGE_TWO_TICKS = 39;
	private static final int STAGE_THREE_TICKS = 55;
	private static final int MAX_ORDINARY_MANA_ARROWS = 5;
	private static final double LOCK_RANGE = 96.0D;
	private static final double LOCK_CONE_DOT = 0.996D;

	private static final List<ArrowShowerState> ARROW_SHOWERS = new ArrayList<>();
	private static final java.util.Map<UUID, RapidFireState> RAPID_STATES = new java.util.HashMap<>();
	private static final java.util.Map<UUID, ArrayDeque<ManaArrowEntity>> ORDINARY_ARROWS =
			new java.util.HashMap<>();

	private RangerCombatManager() {
	}

	public static boolean isRangerSkill(String skill) {
		return CORE_SKILLS.contains(skill) || LEGACY_PROXIMITY_TRAP.equals(skill);
	}

	public static boolean isManaQuiverActive(Entity entity) {
		return hasSkill(entity, MANA_QUIVER) && entity.getPersistentData().getBoolean(QUIVER_ACTIVE);
	}

	public static boolean isHyperFocusActive(Entity entity) {
		return entity != null && entity.getPersistentData().getLong(HYPER_UNTIL) > entity.level().getGameTime();
	}

	public static boolean hasSkill(Entity entity, String skill) {
		if (entity == null || skill == null || skill.isBlank())
			return false;
		String list = variables(entity).Plist;
		if (list == null || list.isBlank())
			return false;
		for (String token : list.split(",")) {
			String clean = cleanSkillToken(token);
			if (skill.equals(clean))
				return true;
		}
		return false;
	}

	/**
	 * Grants and migrates Ranger skills without rank-gating Mana Quiver.
	 * This is safe to call after any class assignment path and on old saves.
	 */
	public static void reconcileRanger(ServerPlayer player) {
		if (!isRanger(player))
			return;
		CompoundTag data = player.getPersistentData();
		int currentRank = rangerRankTier(variables(player));
		if (data.getBoolean(CORE_RECONCILED) && hasSkill(player, MANA_QUIVER)
				&& data.getInt(CORE_RECONCILED_RANK) == currentRank)
			return;

		player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(vars -> {
			boolean changed = ensureSkill(vars, MANA_QUIVER);

			// The old trap slot is the safest migration path for Arrow Shower.
			if (hasExactToken(vars.Plist, LEGACY_PROXIMITY_TRAP)) {
				vars.Plist = replaceSkillToken(vars.Plist, LEGACY_PROXIMITY_TRAP, ARROW_SHOWER);
				for (int slot = 1; slot <= 16; slot++) {
					if (LEGACY_PROXIMITY_TRAP.equals(SkillSlotHelper.getSlot(vars, slot)))
						SkillSlotHelper.setSlot(vars, slot, ARROW_SHOWER);
				}
				if (LEGACY_PROXIMITY_TRAP.equals(vars.PselectedPower))
					vars.PselectedPower = ARROW_SHOWER;
				changed = true;
			}

			// Ordinary Ranger skill unlocks remain Hunter-rank based. System
			// Level affects only Mana Quiver's charging stages.
			int rank = rangerRankTier(vars);
			changed |= ensureSkill(vars, BACK_STEP);
			if (rank >= 2)
				changed |= ensureSkill(vars, HAWKEYE);
			if (rank >= 3)
				changed |= ensureSkill(vars, RAPID_FIRE);
			if (rank >= 4)
				changed |= ensureSkill(vars, HIGH_VALUE_TARGET);
			if (rank >= 5)
				changed |= ensureSkill(vars, SHARPSHOOTER);
			if (rank >= 6)
				changed |= ensureSkill(vars, ARROW_SHOWER);

			if (changed)
				vars.syncPlayerVariables(player);
		});
		data.putBoolean(CORE_RECONCILED, true);
		data.putInt(CORE_RECONCILED_RANK, currentRank);
		ClassPassiveManager.syncRangerFocus(player);
		syncClient(player);
	}

	public static boolean grantSkill(ServerPlayer player, String skill) {
		if (player == null || skill == null || skill.isBlank())
			return false;
		final boolean[] granted = {false};
		player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(vars -> {
			granted[0] = ensureSkill(vars, skill);
			if (granted[0])
				vars.syncPlayerVariables(player);
		});
		return granted[0];
	}

	/** Shared, token-safe server path for universal Ranger runestone items. */
	public static void learnFromRunestone(Entity entity, ItemStack stack, String skill) {
		if (!(entity instanceof ServerPlayer player) || stack == null || stack.isEmpty())
			return;
		if (hasSkill(player, skill)) {
			player.displayClientMessage(Component.translatable(
					"message.sololeveling.ranger.skill_known"), false);
			return;
		}
		if (!grantSkill(player, skill))
			return;
		if (!player.isCreative())
			stack.shrink(1);
		player.displayClientMessage(Component.translatable(
				"message.sololeveling.ranger.skill_gained", skill), false);
	}

	public static boolean activateSkill(ServerPlayer player, String skill) {
		if (player == null || !isRangerSkill(skill))
			return false;
		String canonicalSkill = LEGACY_PROXIMITY_TRAP.equals(skill) ? ARROW_SHOWER : skill;
		boolean unlocked = hasSkill(player, canonicalSkill)
				|| (LEGACY_PROXIMITY_TRAP.equals(skill) && hasSkill(player, LEGACY_PROXIMITY_TRAP));
		if (!unlocked) {
			player.displayClientMessage(Component.translatable("message.sololeveling.ranger.skill_locked"), true);
			return false;
		}
		return switch (canonicalSkill) {
			case MANA_QUIVER -> toggleManaQuiver(player);
			case BACK_STEP -> castEvasiveShot(player);
			case HAWKEYE -> castHawkeye(player);
			case RAPID_FIRE -> castRapidFire(player);
			case HIGH_VALUE_TARGET -> castHighValueTarget(player);
			case SHARPSHOOTER -> castSunderShot(player);
			case ARROW_SHOWER -> castArrowShower(player);
			case HYPER_FOCUS -> castHyperFocus(player);
			default -> false;
		};
	}

	@SubscribeEvent
	public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
		if (!(event.getEntity() instanceof ServerPlayer player))
			return;
		if (isRanger(player))
			reconcileRanger(player);
		else if (hasRangerAccess(player))
			syncClient(player);
		else
			syncEmptyClient(player);
	}

	@SubscribeEvent
	public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
		if (event.getEntity() instanceof ServerPlayer player) {
			clearScheduledAttacks(player.getUUID());
			player.getPersistentData().remove(CORE_RECONCILED);
			if (isRanger(player))
				reconcileRanger(player);
			else if (hasRangerAccess(player))
				syncClient(player);
			else
				syncEmptyClient(player);
		}
	}

	@SubscribeEvent
	public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
		clearScheduledAttacks(event.getEntity().getUUID());
	}

	@SubscribeEvent
	public static void onServerStopped(ServerStoppedEvent event) {
		RAPID_STATES.clear();
		ARROW_SHOWERS.clear();
		ORDINARY_ARROWS.clear();
	}

	@SubscribeEvent
	public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
		if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player))
			return;
		if (!hasRangerAccess(player)) {
			player.getPersistentData().remove(CORE_RECONCILED);
			player.getPersistentData().remove(CORE_RECONCILED_RANK);
			if (player.getPersistentData().getBoolean(CLIENT_STATE_ACTIVE)) {
				clearLock(player);
				clearMark(player);
				EntityHighlightSystem.clearSource(player, HAWKEYE_SOURCE);
				clearScheduledAttacks(player.getUUID());
				syncEmptyClient(player);
			}
			return;
		}
		player.getPersistentData().putBoolean(CLIENT_STATE_ACTIVE, true);

		if (isRanger(player) && (!player.getPersistentData().getBoolean(CORE_RECONCILED)
				|| !hasSkill(player, MANA_QUIVER)
				|| player.getPersistentData().getInt(CORE_RECONCILED_RANK)
						!= rangerRankTier(variables(player))))
			reconcileRanger(player);

		tickRapidFire(player);
		tickHawkeye(player);
		tickManaArrowLock(player);
		expireCombatStates(player);

		if (player.tickCount % 2 == 0 && ((isManaQuiverActive(player) && isUsingSupportedBow(player))
				|| player.tickCount % 20 == 0 || fivefoldCharges(player) > 0))
			syncClient(player);
	}

	@SubscribeEvent
	public static void onServerTick(TickEvent.ServerTickEvent event) {
		if (event.phase != TickEvent.Phase.END)
			return;
		tickArrowShowers();
	}

	@SubscribeEvent
	public static void onArrowNock(ArrowNockEvent event) {
		Player player = event.getEntity();
		if (!hasSkill(player, MANA_QUIVER) || !isSupportedBow(event.getBow())
				|| !shouldInterceptBow(player))
			return;
		player.startUsingItem(event.getHand());
		event.setAction(InteractionResultHolder.consume(event.getBow()));
	}

	@SubscribeEvent
	public static void onArrowLoose(ArrowLooseEvent event) {
		Player player = event.getEntity();
		ItemStack bow = event.getBow();
		if (!hasSkill(player, MANA_QUIVER) || !isSupportedBow(bow)
				|| !shouldInterceptBow(player))
			return;

		event.setCanceled(true);
		if (!(player instanceof ServerPlayer serverPlayer))
			return;

		if (isFivefoldActive(serverPlayer)) {
			fireFivefoldArrow(serverPlayer, bow, event.getCharge());
			return;
		}

		int previewStage = chargeStage(serverPlayer, event.getCharge());
		if (previewStage <= 0) {
			serverPlayer.displayClientMessage(Component.translatable("message.sololeveling.ranger.mana_arrow_forming"), true);
			clearLock(serverPlayer);
			syncClient(serverPlayer);
			return;
		}

		LivingEntity locked = lockedTarget(serverPlayer);
		int stage = previewStage >= 3 && locked != null ? 3 : Math.min(previewStage, 2);
		double damage = manaArrowDamage(serverPlayer, bow, stage);
		double cost = manaArrowCost(serverPlayer, damage, stage);
		if (!spendMana(serverPlayer, cost, 25 + stage * 10))
			return;

		spawnManaArrow(serverPlayer, bow, stage, damage, locked, true, false, false);
		damageBow(serverPlayer, bow);
		serverPlayer.awardStat(Stats.ITEM_USED.get(bow.getItem()));
		clearLock(serverPlayer);
		syncClient(serverPlayer);
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void onRangerProjectileDamage(LivingHurtEvent event) {
		if (event.isCanceled())
			return;
		if (!(event.getSource().getEntity() instanceof ServerPlayer ranger)
				|| !isRanger(ranger) || !(event.getSource().getDirectEntity() instanceof AbstractArrow arrow))
			return;
		LivingEntity target = event.getEntity();
		if (!MageCombatHelper.isValidTarget(ranger, target))
			return;

		double distance = ranger.distanceTo(target);
		boolean pvp = target instanceof Player;
		double rangeBonus = distance <= 10.0D ? 0.0D
				: Mth.clamp((distance - 10.0D) / 22.0D, 0.0D, 1.0D) * (pvp ? 0.08D : 0.18D);
		boolean marked = isMarkedTarget(ranger, target);
		double multiplier = 1.0D + rangeBonus + (marked ? (pvp ? 0.05D : 0.12D) : 0.0D);

		CompoundTag targetData = target.getPersistentData();
		if (targetData.hasUUID(SUNDER_OWNER)
				&& ranger.getUUID().equals(targetData.getUUID(SUNDER_OWNER))
				&& targetData.getLong(SUNDER_UNTIL) > target.level().getGameTime())
			multiplier += pvp ? 0.04D : 0.08D;

		boolean focusEligible = !arrow.getPersistentData().getBoolean("ranger_no_focus")
				&& !target.getPersistentData().getBoolean("radiru_training_dummy");
		if (focusEligible && ClassPassiveManager.consumeRangerFocus(ranger))
			multiplier += pvp ? 0.10D : 0.25D;

		event.setAmount((float) Math.max(0.0D, event.getAmount() * multiplier));

		if (arrow.getPersistentData().getBoolean("ranger_sunder")) {
			targetData.putUUID(SUNDER_OWNER, ranger.getUUID());
			targetData.putLong(SUNDER_UNTIL, target.level().getGameTime() + (pvp ? 60 : 100));
		}

		if (focusEligible) {
			double gain = 16.0D + (distance >= 18.0D ? 8.0D : 0.0D) + (marked ? 6.0D : 0.0D);
			if (isHyperFocusActive(ranger))
				gain *= 2.0D;
			ClassPassiveManager.addRangerFocus(ranger, gain);
		}

		if (arrow.getPersistentData().getBoolean("ranger_typhoon")
				&& !arrow.getPersistentData().getBoolean("ranger_typhoon_triggered")) {
			arrow.getPersistentData().putBoolean("ranger_typhoon_triggered", true);
			triggerTyphoonImpact(ranger, target, event.getAmount());
		}
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void onRangerDamaged(LivingHurtEvent event) {
		if (event.isCanceled())
			return;
		if (!(event.getEntity() instanceof ServerPlayer ranger) || !isRanger(ranger))
			return;
		Entity attacker = event.getSource().getEntity();
		if (attacker != null && attacker.distanceToSqr(ranger) <= 36.0D)
			ClassPassiveManager.addRangerFocus(ranger, -20.0D);
	}

	@SubscribeEvent
	public static void onMarkedTargetDeath(LivingDeathEvent event) {
		if (event.getEntity().level().isClientSide())
			return;
		MinecraftServer server = event.getEntity().getServer();
		if (server == null)
			return;
		for (ServerPlayer ranger : server.getPlayerList().getPlayers()) {
			if (!isMarkedTarget(ranger, event.getEntity()))
				continue;
			if (event.getEntity() instanceof Player) {
				clearMark(ranger);
				continue;
			}
			refundMana(ranger, 200.0D);
			ranger.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(vars -> {
				if (vars.rangerleapnum < 3) {
					double reducedTimer = vars.rangerleaptimer - 60.0D;
					if (reducedTimer <= 0.0D) {
						vars.rangerleapnum = Math.min(3.0D, vars.rangerleapnum + 1.0D);
						vars.rangerleaptimer = vars.rangerleapnum >= 3.0D ? 0.0D : 180.0D;
					} else {
						vars.rangerleaptimer = reducedTimer;
					}
				}
				vars.syncPlayerVariables(ranger);
			});
			clearMark(ranger);
		}
	}

	private static boolean toggleManaQuiver(ServerPlayer player) {
		boolean enabled = !player.getPersistentData().getBoolean(QUIVER_ACTIVE);
		player.getPersistentData().putBoolean(QUIVER_ACTIVE, enabled);
		clearLock(player);
		player.displayClientMessage(Component.translatable(enabled
				? "message.sololeveling.ranger.mana_quiver_on"
				: "message.sololeveling.ranger.mana_quiver_off"), true);
		player.level().playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME,
				SoundSource.PLAYERS, 0.55F, enabled ? 1.45F : 0.75F);
		syncClient(player);
		return true;
	}

	private static boolean castEvasiveShot(ServerPlayer player) {
		if (variables(player).rangerleapnum <= 0.0D) {
			player.displayClientMessage(Component.translatable(
					"message.sololeveling.ranger.no_evasive_charges"), true);
			return false;
		}
		if (!canAffordMana(player, 200.0D))
			return false;
		if (!BackStepProcedure.execute(player))
			return false;
		spendMana(player, 200.0D, 25);
		player.getPersistentData().putLong(QUICK_DRAW_UNTIL, player.level().getGameTime() + 40);
		ItemStack bow = heldBow(player);
		if (!bow.isEmpty())
			spawnManaArrow(player, bow, 1, manaArrowDamage(player, bow, 1) * 0.55D,
					null, false, true, false);
		return true;
	}

	private static boolean castHawkeye(ServerPlayer player) {
		if (CooldownManager.isOnCooldown(player, HAWKEYE))
			return cooldownMessage(player, HAWKEYE);
		if (!spendMana(player, 350.0D, 40))
			return false;
		player.getPersistentData().putLong(HAWKEYE_UNTIL, player.level().getGameTime() + 360);
		CooldownManager.set(player, HAWKEYE, 520);
		player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 380, 0, false, false));
		player.displayClientMessage(Component.translatable("message.sololeveling.ranger.hawkeye"), true);
		return true;
	}

	private static boolean castRapidFire(ServerPlayer player) {
		if (CooldownManager.isOnCooldown(player, RAPID_FIRE))
			return cooldownMessage(player, RAPID_FIRE);
		ItemStack bow = heldBow(player);
		if (bow.isEmpty()) {
			player.displayClientMessage(Component.translatable("message.sololeveling.ranger.bow_required"), true);
			return false;
		}
		if (!spendMana(player, 550.0D, 45))
			return false;
		CooldownManager.set(player, RAPID_FIRE, 180);
		RAPID_STATES.put(player.getUUID(), new RapidFireState(player.level().dimension(),
				bow.copy(), player.level().getGameTime(), 0));
		damageBow(player, bow);
		player.displayClientMessage(Component.translatable("message.sololeveling.ranger.rapid_fire"), true);
		return true;
	}

	private static boolean castHighValueTarget(ServerPlayer player) {
		if (CooldownManager.isOnCooldown(player, HIGH_VALUE_TARGET))
			return cooldownMessage(player, HIGH_VALUE_TARGET);
		LivingEntity target = raycastTarget(player, 64.0D);
		if (target == null) {
			player.displayClientMessage(Component.translatable("message.sololeveling.ranger.no_target"), true);
			return false;
		}
		if (!spendMana(player, 300.0D, 35))
			return false;
		long duration = target instanceof Player ? 120L : 300L;
		CompoundTag data = player.getPersistentData();
		if (data.hasUUID(MARK_TARGET))
			clearMark(player);
		data.putUUID(MARK_TARGET, target.getUUID());
		data.putString(MARK_DIMENSION, target.level().dimension().location().toString());
		data.putLong(MARK_UNTIL, player.level().getGameTime() + duration);
		CooldownManager.set(player, HIGH_VALUE_TARGET, 240);
		EntityHighlightSystem.show(player, target, MARK_SOURCE, 0xFFC83D,
				(int) duration, EntityHighlightSystem.PRIORITY_PERCEPTION + 60);
		player.displayClientMessage(Component.translatable("message.sololeveling.ranger.target_marked",
				target.getDisplayName()), true);
		return true;
	}

	private static boolean castSunderShot(ServerPlayer player) {
		if (CooldownManager.isOnCooldown(player, SHARPSHOOTER))
			return cooldownMessage(player, SHARPSHOOTER);
		ItemStack bow = heldBow(player);
		if (bow.isEmpty()) {
			player.displayClientMessage(Component.translatable("message.sololeveling.ranger.bow_required"), true);
			return false;
		}
		boolean evolved = hasAllCoreSkills(player);
		double manaCost = evolved ? 1050.0D : 800.0D;
		if (!spendMana(player, manaCost, evolved ? 70 : 55))
			return false;
		CooldownManager.set(player, SHARPSHOOTER, evolved ? 300 : 240);
		ManaArrowEntity arrow = spawnManaArrow(player, bow, 2,
				manaArrowDamage(player, bow, 2) * (evolved ? 2.65D : 2.20D),
				null, false, true, true);
		if (evolved && arrow != null)
			arrow.getPersistentData().putBoolean("ranger_typhoon", true);
		damageBow(player, bow);
		player.displayClientMessage(Component.translatable(evolved
				? "message.sololeveling.ranger.typhoon_shot"
				: "message.sololeveling.ranger.sunder_shot"), true);
		return true;
	}

	private static boolean castArrowShower(ServerPlayer player) {
		if (CooldownManager.isOnCooldown(player, ARROW_SHOWER))
			return cooldownMessage(player, ARROW_SHOWER);
		HitResult hit = player.pick(56.0D, 1.0F, false);
		Vec3 center = hit.getType() == HitResult.Type.MISS
				? player.getEyePosition().add(player.getLookAngle().scale(36.0D))
				: hit.getLocation().add(0.0D, 0.12D, 0.0D);
		if (!spendMana(player, 1100.0D, 65))
			return false;
		CooldownManager.set(player, ARROW_SHOWER, 400);
		CooldownManager.set(player, LEGACY_PROXIMITY_TRAP, 400);
		ARROW_SHOWERS.removeIf(state -> state.ownerId.equals(player.getUUID()));
		double pulseDamage = manaBaseDamage(player, heldBow(player)) * 0.45D;
		ARROW_SHOWERS.add(new ArrowShowerState(player.getUUID(), player.level().dimension(),
				center, pulseDamage, 0));
		if (player.level() instanceof ServerLevel level) {
			level.sendParticles(ParticleTypes.ENCHANT, center.x, center.y + 0.2D, center.z,
					32, 5.0D, 0.05D, 5.0D, 0.1D);
			level.playSound(null, BlockPos.containing(center), SoundEvents.EVOKER_CAST_SPELL,
					SoundSource.PLAYERS, 0.9F, 1.45F);
		}
		player.displayClientMessage(Component.translatable("message.sololeveling.ranger.arrow_shower"), true);
		return true;
	}

	private static boolean castHyperFocus(ServerPlayer player) {
		if (CooldownManager.isOnCooldown(player, HYPER_FOCUS))
			return cooldownMessage(player, HYPER_FOCUS);
		boolean evolved = hasAllCoreSkills(player);
		if (evolved) {
			if (ClassPassiveManager.getRangerFocus(player) < 100.0D) {
				player.displayClientMessage(Component.translatable("message.sololeveling.ranger.deadeye_required"), true);
				return false;
			}
			double cost = Math.max(900.0D, variables(player).Mana * 0.08D);
			if (!spendMana(player, cost, 80))
				return false;
			ClassPassiveManager.consumeRangerFocus(player);
			CompoundTag data = player.getPersistentData();
			data.putInt(FIVEFOLD_CHARGES, 5);
			data.putLong(FIVEFOLD_UNTIL, player.level().getGameTime() + 200);
			CooldownManager.set(player, HYPER_FOCUS, 960);
			player.displayClientMessage(Component.translatable("message.sololeveling.ranger.fivefold"), true);
			syncClient(player);
			return true;
		}

		if (!spendMana(player, 900.0D, 60))
			return false;
		player.getPersistentData().putLong(HYPER_UNTIL, player.level().getGameTime() + 160);
		CooldownManager.set(player, HYPER_FOCUS, 600);
		player.displayClientMessage(Component.translatable("message.sololeveling.ranger.hyper_focus"), true);
		return true;
	}

	private static void tickRapidFire(ServerPlayer player) {
		RapidFireState state = RAPID_STATES.get(player.getUUID());
		if (state == null)
			return;
		if (!player.isAlive() || !state.dimension.equals(player.level().dimension())) {
			RAPID_STATES.remove(player.getUUID());
			return;
		}
		long now = player.level().getGameTime();
		if (now < state.nextShotTick)
			return;
		ItemStack bow = state.bow;
		double damage = manaArrowDamage(player, bow, 1) * 0.48D;
		ManaArrowEntity arrow = spawnManaArrow(player, bow, 2, damage,
				null, false, true, false);
		if (arrow != null)
			arrow.getPersistentData().putBoolean("ranger_no_focus", state.shotsFired > 0);
		int fired = state.shotsFired + 1;
		if (fired >= 5) {
			RAPID_STATES.remove(player.getUUID());
		} else {
			RAPID_STATES.put(player.getUUID(), new RapidFireState(state.dimension,
					state.bow, now + 5L, fired));
		}
	}

	private static void tickHawkeye(ServerPlayer player) {
		long now = player.level().getGameTime();
		if (player.getPersistentData().getLong(HAWKEYE_UNTIL) <= now || player.tickCount % 10 != 0)
			return;
		Vec3 eye = player.getEyePosition();
		Vec3 look = player.getLookAngle();
		List<LivingEntity> candidates = player.level().getEntitiesOfClass(LivingEntity.class,
				player.getBoundingBox().inflate(48.0D),
				target -> MageCombatHelper.isValidTarget(player, target)
						&& target.getBoundingBox().getCenter().subtract(eye).normalize().dot(look) >= 0.93D)
				.stream()
				.sorted(Comparator.comparingDouble(player::distanceToSqr))
				.limit(16)
				.toList();
		int shown = 0;
		for (LivingEntity target : candidates) {
			if (!player.hasLineOfSight(target))
				continue;
			EntityHighlightSystem.show(player, target, HAWKEYE_SOURCE,
					EntityHighlightSystem.perceptionColor(target), 16,
					EntityHighlightSystem.PRIORITY_PERCEPTION + 20);
			if (++shown >= 5)
				break;
		}
	}

	private static void tickManaArrowLock(ServerPlayer player) {
		if (!isManaQuiverActive(player) || !isUsingSupportedBow(player)
				|| maxManaArrowStage(player) < 3) {
			clearLock(player);
			return;
		}

		CompoundTag data = player.getPersistentData();
		if (data.hasUUID(LOCK_TARGET)) {
			Entity locked = entityByUuid(player.serverLevel(), data.getUUID(LOCK_TARGET));
			if (locked instanceof LivingEntity living && validLockTarget(player, living))
				return;
			clearLock(player);
		}

		// Sneaking is required to acquire a lock, but once acquired the player
		// may release sneak and freely redirect the bow before firing.
		if (!player.isCrouching())
			return;

		LivingEntity candidate = findInstantLockTarget(player);
		if (candidate == null)
			return;

		data.putUUID(LOCK_TARGET, candidate.getUUID());
		player.level().playSound(null, player.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP,
				SoundSource.PLAYERS, 0.45F, 1.9F);
	}

	private static void tickArrowShowers() {
		MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
		if (server == null || ARROW_SHOWERS.isEmpty())
			return;
		Iterator<ArrowShowerState> iterator = ARROW_SHOWERS.iterator();
		while (iterator.hasNext()) {
			ArrowShowerState state = iterator.next();
			ServerLevel level = server.getLevel(state.dimension);
			ServerPlayer owner = server.getPlayerList().getPlayer(state.ownerId);
			if (level == null || owner == null || owner.level() != level || !owner.isAlive()) {
				iterator.remove();
				continue;
			}
			int age = state.age + 1;
			if (age >= 16 && age <= 56 && (age - 16) % 10 == 0)
				pulseArrowShower(level, owner, state.center, state.damage, (age - 16) / 10);
			if (age > 60) {
				iterator.remove();
			} else {
				state.age = age;
			}
		}
	}

	private static void pulseArrowShower(ServerLevel level, ServerPlayer owner, Vec3 center,
			double snapshottedDamage, int pulse) {
		level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, center.x, center.y + 6.0D, center.z,
				24, 4.2D, 2.0D, 4.2D, 0.03D);
		level.sendParticles(ParticleTypes.END_ROD, center.x, center.y + 2.5D, center.z,
				12, 4.5D, 2.5D, 4.5D, 0.04D);
		List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class,
				new AABB(center, center).inflate(7.0D, 4.0D, 7.0D),
				target -> MageCombatHelper.isValidTarget(owner, target))
				.stream().sorted(Comparator.comparingDouble(target -> target.distanceToSqr(center)))
				.limit(32).toList();
		float damage = (float) Math.max(0.25D, snapshottedDamage);
		for (LivingEntity target : targets) {
			boolean hurt = target.hurt(new net.minecraft.world.damagesource.DamageSource(
					level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
							.getHolderOrThrow(RANGER_DAMAGE), owner), damage);
			if (hurt)
				target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 16, 0, false, false));
		}
		if (pulse == 0 || pulse == 4)
			level.playSound(null, BlockPos.containing(center), SoundEvents.ARROW_HIT,
					SoundSource.PLAYERS, 0.85F, 1.2F + pulse * 0.08F);
	}

	private static void triggerTyphoonImpact(ServerPlayer ranger, LivingEntity primary,
			float primaryDamage) {
		if (!(ranger.level() instanceof ServerLevel level))
			return;
		Vec3 center = primary.getBoundingBox().getCenter();
		level.sendParticles(ParticleTypes.CLOUD, center.x, center.y, center.z,
				28, 2.2D, 1.2D, 2.2D, 0.16D);
		level.sendParticles(ParticleTypes.END_ROD, center.x, center.y, center.z,
				16, 1.8D, 1.0D, 1.8D, 0.08D);
		level.playSound(null, primary.blockPosition(), SoundEvents.GENERIC_EXPLODE,
				SoundSource.PLAYERS, 0.75F, 1.65F);
		float splashDamage = Math.max(1.0F, primaryDamage * 0.42F);
		List<LivingEntity> nearby = level.getEntitiesOfClass(LivingEntity.class,
				primary.getBoundingBox().inflate(4.5D),
				target -> target != primary && MageCombatHelper.isValidTarget(ranger, target))
				.stream()
				.sorted(Comparator.comparingDouble(primary::distanceToSqr))
				.limit(12)
				.toList();
		for (LivingEntity target : nearby) {
			boolean hurt = target.hurt(new net.minecraft.world.damagesource.DamageSource(
					level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
							.getHolderOrThrow(RANGER_DAMAGE), ranger), splashDamage);
			if (!hurt)
				continue;
			Vec3 push = target.position().subtract(center);
			if (push.lengthSqr() > 0.001D) {
				push = push.normalize().scale(target instanceof Player ? 0.35D : 0.7D);
				target.push(push.x, 0.16D, push.z);
				target.hurtMarked = true;
			}
		}
	}

	private static void fireFivefoldArrow(ServerPlayer player, ItemStack bow, int charge) {
		if (charge < 6) {
			player.displayClientMessage(Component.translatable("message.sololeveling.ranger.fivefold_forming"), true);
			return;
		}
		int charges = fivefoldCharges(player);
		if (charges <= 0)
			return;
		ManaArrowEntity arrow = spawnManaArrow(player, bow, 2,
				manaBaseDamage(player, bow) * 1.20D, null, false, true, false);
		if (arrow != null)
			arrow.getPersistentData().putBoolean("ranger_no_focus", true);
		player.getPersistentData().putInt(FIVEFOLD_CHARGES, charges - 1);
		damageBow(player, bow);
		if (charges - 1 <= 0)
			player.getPersistentData().remove(FIVEFOLD_UNTIL);
		syncClient(player);
	}

	private static ManaArrowEntity spawnManaArrow(ServerPlayer player, ItemStack bow, int stage,
			double damage, LivingEntity lockedTarget, boolean ordinary, boolean noFocus,
			boolean sunder) {
		ManaArrowEntity arrow = new ManaArrowEntity(SololevelingModEntities.MANA_ARROW.get(), player, player.level());
		double speed = switch (stage) {
			case 2 -> 3.65D;
			case 3 -> 3.55D;
			default -> 3.2D;
		};
		if (player.getPersistentData().getLong(HAWKEYE_UNTIL) > player.level().getGameTime())
			speed *= 1.10D;
		if (isHyperFocusActive(player))
			speed *= 1.15D;
		arrow.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F,
				(float) speed, stage >= 2 ? 0.0F : 0.35F);
		arrow.setBaseDamage(Math.max(0.25D, damage));
		int punch = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.PUNCH_ARROWS, bow);
		if (punch > 0)
			arrow.setKnockback(punch);
		if (EnchantmentHelper.getItemEnchantmentLevel(Enchantments.FLAMING_ARROWS, bow) > 0)
			arrow.setSecondsOnFire(100);
		arrow.setCritArrow(stage >= 2);
		if (sunder)
			arrow.setPierceLevel((byte) 3);
		arrow.configureRangerShot(stage, lockedTarget == null ? null : lockedTarget.getUUID(),
				lockedTarget == null ? 0.0D : player.distanceTo(lockedTarget), ordinary);
		if (noFocus)
			arrow.getPersistentData().putBoolean("ranger_no_focus", true);
		if (sunder)
			arrow.getPersistentData().putBoolean("ranger_sunder", true);
		player.level().addFreshEntity(arrow);
		if (ordinary)
			trackOrdinaryArrow(player, arrow);
		player.level().playSound(null, player.blockPosition(), SoundEvents.ARROW_SHOOT,
				SoundSource.PLAYERS, 0.9F, 1.15F + stage * 0.13F);
		return arrow;
	}

	private static double manaArrowDamage(ServerPlayer player, ItemStack bow, int stage) {
		double multiplier = switch (stage) {
			case 2 -> 1.23D;
			case 3 -> 1.38D;
			default -> 1.0D;
		};
		return manaBaseDamage(player, bow) * multiplier;
	}

	private static double manaBaseDamage(ServerPlayer player, ItemStack bow) {
		SololevelingModVariables.PlayerVariables vars = variables(player);
		double statBonus = Math.min(8.0D, vars.perception / 55.0D + vars.Intelligence / 120.0D);
		int power = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.POWER_ARROWS, bow);
		double enchantment = power > 0 ? 0.5D + power * 0.5D : 0.0D;
		return 3.0D + statBonus + enchantment;
	}

	private static double manaArrowCost(ServerPlayer player, double predictedDamage, int stage) {
		SololevelingModVariables.PlayerVariables vars = variables(player);
		double base = 90.0D + Math.max(1000.0D, vars.Mana) * 0.01D + predictedDamage * 4.0D;
		double multiplier = switch (stage) {
			case 2 -> 1.75D;
			case 3 -> 2.75D;
			default -> 1.0D;
		};
		if (isHyperFocusActive(player))
			multiplier *= 0.80D;
		return Math.ceil(base * multiplier);
	}

	private static int chargeStage(ServerPlayer player, int charge) {
		int max = maxManaArrowStage(player);
		if (charge >= adjustedThreshold(player, STAGE_THREE_TICKS) && max >= 3)
			return 3;
		if (charge >= adjustedThreshold(player, STAGE_TWO_TICKS) && max >= 2)
			return 2;
		if (charge >= adjustedThreshold(player, STAGE_ONE_TICKS))
			return 1;
		return 0;
	}

	private static int adjustedThreshold(ServerPlayer player, int threshold) {
		int extra = Math.max(0, threshold - FULL_DRAW_TICKS);
		if (isHyperFocusActive(player))
			extra = (int) Math.ceil(extra * 0.80D);
		int result = FULL_DRAW_TICKS + extra;
		if (player.getPersistentData().getLong(QUICK_DRAW_UNTIL) > player.level().getGameTime())
			result = Math.max(12, result - 5);
		return result;
	}

	private static int maxManaArrowStage(ServerPlayer player) {
		SololevelingModVariables.PlayerVariables vars = variables(player);
		int rank = rangerRankTier(vars);
		int systemLevel = Math.max(0, (int) Math.floor(vars.Level));
		if (rank >= 5 || systemLevel >= 80)
			return 3;
		if (rank >= 4 || systemLevel >= 60)
			return 2;
		return 1;
	}

	private static int rangerRankTier(SololevelingModVariables.PlayerVariables vars) {
		return Mth.clamp((int) Math.round(vars.HunterRank), 1, 6);
	}

	private static void expireCombatStates(ServerPlayer player) {
		long now = player.level().getGameTime();
		CompoundTag data = player.getPersistentData();
		if (data.getLong(MARK_UNTIL) <= now && data.hasUUID(MARK_TARGET))
			clearMark(player);
		if (data.getLong(FIVEFOLD_UNTIL) <= now && data.getInt(FIVEFOLD_CHARGES) > 0) {
			data.putInt(FIVEFOLD_CHARGES, 0);
			data.remove(FIVEFOLD_UNTIL);
		}
	}

	private static void trackOrdinaryArrow(ServerPlayer player, ManaArrowEntity arrow) {
		ArrayDeque<ManaArrowEntity> arrows = ORDINARY_ARROWS.computeIfAbsent(
				player.getUUID(), ignored -> new ArrayDeque<>());
		arrows.removeIf(existing -> existing == null || existing.isRemoved());
		while (arrows.size() >= MAX_ORDINARY_MANA_ARROWS) {
			ManaArrowEntity oldest = arrows.pollFirst();
			if (oldest != null && !oldest.isRemoved())
				oldest.discard();
		}
		arrows.addLast(arrow);
	}

	private static LivingEntity raycastTarget(ServerPlayer player, double range) {
		Vec3 eye = player.getEyePosition();
		Vec3 end = eye.add(player.getLookAngle().scale(range));
		BlockHitResult blockHit = player.level().clip(new ClipContext(eye, end,
				ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
		double maxDistanceSq = blockHit.getType() == HitResult.Type.MISS
				? range * range : eye.distanceToSqr(blockHit.getLocation());
		AABB search = player.getBoundingBox().expandTowards(player.getLookAngle().scale(range)).inflate(1.0D);
		EntityHitResult hit = ProjectileUtil.getEntityHitResult(player, eye, end, search,
				candidate -> candidate instanceof LivingEntity living
						&& MageCombatHelper.isValidTarget(player, living), maxDistanceSq);
		return hit != null && hit.getEntity() instanceof LivingEntity living ? living : null;
	}

	private static LivingEntity findInstantLockTarget(ServerPlayer player) {
		LivingEntity direct = raycastTarget(player, LOCK_RANGE);
		if (direct != null)
			return direct;

		Vec3 eye = player.getEyePosition();
		Vec3 look = player.getLookAngle();
		Vec3 end = eye.add(look.scale(LOCK_RANGE));
		AABB searchBox = new AABB(eye, end).inflate(9.0D);
		return player.level().getEntitiesOfClass(LivingEntity.class,
				searchBox,
				target -> MageCombatHelper.isValidTarget(player, target)
						&& player.distanceToSqr(target) <= LOCK_RANGE * LOCK_RANGE
						&& lockAlignment(eye, look, target) >= LOCK_CONE_DOT)
				.stream()
				.filter(target -> canSeeLockTarget(player, target))
				.sorted(Comparator
						.comparingDouble((LivingEntity target) ->
								-lockAlignment(eye, look, target))
						.thenComparingDouble(player::distanceToSqr))
				.findFirst()
				.orElse(null);
	}

	private static double lockAlignment(Vec3 eye, Vec3 look, LivingEntity target) {
		Vec3 towardTarget = target.getBoundingBox().getCenter().subtract(eye);
		if (towardTarget.lengthSqr() < 0.0001D)
			return 1.0D;
		return towardTarget.normalize().dot(look);
	}

	private static LivingEntity lockedTarget(ServerPlayer player) {
		CompoundTag data = player.getPersistentData();
		if (!data.hasUUID(LOCK_TARGET))
			return null;
		Entity entity = entityByUuid(player.serverLevel(), data.getUUID(LOCK_TARGET));
		return entity instanceof LivingEntity living && validLockTarget(player, living) ? living : null;
	}

	private static boolean validLockTarget(ServerPlayer player, LivingEntity target) {
		return MageCombatHelper.isValidTarget(player, target)
				&& target.level() == player.level()
				&& player.distanceToSqr(target) <= LOCK_RANGE * LOCK_RANGE
				&& canSeeLockTarget(player, target);
	}

	private static boolean canSeeLockTarget(ServerPlayer player, LivingEntity target) {
		Vec3 eye = player.getEyePosition();
		return hasClearLockPath(player, eye, target.getBoundingBox().getCenter())
				|| hasClearLockPath(player, eye, target.getEyePosition());
	}

	private static boolean hasClearLockPath(ServerPlayer player, Vec3 from, Vec3 to) {
		BlockHitResult obstruction = player.level().clip(new ClipContext(from, to,
				ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
		return obstruction.getType() != HitResult.Type.BLOCK
				|| obstruction.getLocation().distanceToSqr(from) + 0.25D >= to.distanceToSqr(from);
	}

	private static Entity entityByUuid(ServerLevel level, UUID uuid) {
		return level == null || uuid == null ? null : level.getEntity(uuid);
	}

	private static boolean isMarkedTarget(ServerPlayer ranger, Entity target) {
		CompoundTag data = ranger.getPersistentData();
		return target != null && data.hasUUID(MARK_TARGET)
				&& target.getUUID().equals(data.getUUID(MARK_TARGET))
				&& data.getLong(MARK_UNTIL) > ranger.level().getGameTime()
				&& target.level().dimension().location().toString().equals(data.getString(MARK_DIMENSION));
	}

	private static void clearMark(ServerPlayer player) {
		CompoundTag data = player.getPersistentData();
		if (data.hasUUID(MARK_TARGET)) {
			ResourceLocation storedDimension = ResourceLocation.tryParse(data.getString(MARK_DIMENSION));
			ResourceKey<Level> dimension = storedDimension == null
					? player.level().dimension()
					: ResourceKey.create(Registries.DIMENSION, storedDimension);
			EntityHighlightSystem.hide(player, data.getUUID(MARK_TARGET), dimension, MARK_SOURCE);
		}
		data.remove(MARK_TARGET);
		data.remove(MARK_DIMENSION);
		data.remove(MARK_UNTIL);
	}

	private static void clearLock(ServerPlayer player) {
		CompoundTag data = player.getPersistentData();
		data.remove(LOCK_TARGET);
	}

	private static boolean spendMana(ServerPlayer player, double amount, int regenLockTicks) {
		if (player.isCreative())
			return true;
		SololevelingModVariables.PlayerVariables vars = variables(player);
		if (vars.MP + 0.0001D < amount) {
			player.displayClientMessage(Component.translatable("message.sololeveling.ranger.not_enough_mana",
					(int) Math.ceil(amount)), true);
			return false;
		}
		player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(cap -> {
			cap.MP = Math.max(0.0D, cap.MP - amount);
			cap.syncPlayerVariables(player);
		});
		CooldownManager.set(player, "mana_refresh", regenLockTicks);
		return true;
	}

	private static boolean canAffordMana(ServerPlayer player, double amount) {
		if (player.isCreative())
			return true;
		if (variables(player).MP + 0.0001D >= amount)
			return true;
		player.displayClientMessage(Component.translatable(
				"message.sololeveling.ranger.not_enough_mana",
				(int) Math.ceil(amount)), true);
		return false;
	}

	private static void refundMana(ServerPlayer player, double amount) {
		player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(cap -> {
			cap.MP = Math.min(cap.Mana, cap.MP + Math.max(0.0D, amount));
			cap.syncPlayerVariables(player);
		});
	}

	private static boolean cooldownMessage(ServerPlayer player, String skill) {
		player.displayClientMessage(Component.translatable("message.sololeveling.ranger.cooldown",
				skill, CooldownManager.getRemainingSeconds(player, skill)), true);
		return false;
	}

	private static void damageBow(ServerPlayer player, ItemStack bow) {
		if (player.isCreative() || bow.isEmpty())
			return;
		InteractionHand hand = player.getOffhandItem() == bow
				? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
		bow.hurtAndBreak(1, player, owner -> owner.broadcastBreakEvent(hand));
	}

	private static ItemStack heldBow(Player player) {
		if (isSupportedBow(player.getMainHandItem()))
			return player.getMainHandItem();
		if (isSupportedBow(player.getOffhandItem()))
			return player.getOffhandItem();
		return ItemStack.EMPTY;
	}

	private static boolean isUsingSupportedBow(Player player) {
		return player != null && player.isUsingItem() && isSupportedBow(player.getUseItem());
	}

	private static boolean isSupportedBow(ItemStack stack) {
		return stack != null && !stack.isEmpty() && stack.getItem() instanceof BowItem;
	}

	private static boolean isRanger(Entity entity) {
		return entity != null && Math.round(variables(entity).Classes) == 6L;
	}

	private static boolean hasRangerAccess(Entity entity) {
		if (isRanger(entity))
			return true;
		for (String skill : CORE_SKILLS) {
			if (hasSkill(entity, skill))
				return true;
		}
		return hasSkill(entity, LEGACY_PROXIMITY_TRAP);
	}

	private static SololevelingModVariables.PlayerVariables variables(Entity entity) {
		return entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
				.orElse(new SololevelingModVariables.PlayerVariables());
	}

	private static boolean ensureSkill(SololevelingModVariables.PlayerVariables vars, String skill) {
		if (hasExactToken(vars.Plist, skill))
			return false;
		String list = vars.Plist == null ? "" : vars.Plist.trim();
		if (list.isEmpty())
			list = ".";
		if (!list.endsWith(","))
			list += ",";
		vars.Plist = list + skill + ",";
		return true;
	}

	private static boolean hasExactToken(String list, String skill) {
		if (list == null || list.isBlank())
			return false;
		for (String token : list.split(",")) {
			if (skill.equals(cleanSkillToken(token)))
				return true;
		}
		return false;
	}

	private static String replaceSkillToken(String list, String oldSkill, String newSkill) {
		if (list == null || list.isBlank())
			return "." + newSkill + ",";
		List<String> skills = new ArrayList<>();
		for (String token : list.split(",")) {
			String clean = cleanSkillToken(token);
			if (clean.isEmpty())
				continue;
			String mapped = oldSkill.equals(clean) ? newSkill : clean;
			if (!skills.contains(mapped))
				skills.add(mapped);
		}
		return "." + String.join(",", skills) + ",";
	}

	private static String cleanSkillToken(String token) {
		if (token == null)
			return "";
		String clean = token.trim();
		while (clean.startsWith("."))
			clean = clean.substring(1);
		return clean;
	}

	private static boolean hasAllCoreSkills(Entity player) {
		if (player == null)
			return false;
		for (String skill : CORE_SKILLS) {
			if (!hasSkill(player, skill))
				return false;
		}
		return true;
	}

	private static boolean isFivefoldActive(Player player) {
		return player != null && fivefoldCharges(player) > 0
				&& player.getPersistentData().getLong(FIVEFOLD_UNTIL) > player.level().getGameTime();
	}

	private static boolean shouldInterceptBow(Player player) {
		if (player == null)
			return false;
		if (player.level().isClientSide())
			return RangerClientState.quiverActive || RangerClientState.fivefoldCharges > 0;
		return isManaQuiverActive(player) || isFivefoldActive(player);
	}

	private static int fivefoldCharges(Player player) {
		return player == null ? 0 : Math.max(0, player.getPersistentData().getInt(FIVEFOLD_CHARGES));
	}

	private static void syncClient(ServerPlayer player) {
		if (player == null)
			return;
		int stage = isUsingSupportedBow(player) ? chargeStage(player, player.getTicksUsingItem()) : 0;
		CompoundTag data = player.getPersistentData();
		float lockProgress = data.hasUUID(LOCK_TARGET) ? 1.0F : 0.0F;
		RangerStateMessage message = new RangerStateMessage(
				isManaQuiverActive(player), stage, maxManaArrowStage(player),
				lockProgress, data.hasUUID(LOCK_TARGET), fivefoldCharges(player),
				data.getLong(HAWKEYE_UNTIL) > player.level().getGameTime(),
				isHyperFocusActive(player));
		net.solocraft.SololevelingMod.PACKET_HANDLER.send(
				PacketDistributor.PLAYER.with(() -> player), message);
	}

	private static void syncEmptyClient(ServerPlayer player) {
		if (player == null)
			return;
		player.getPersistentData().remove(CLIENT_STATE_ACTIVE);
		net.solocraft.SololevelingMod.PACKET_HANDLER.send(
				PacketDistributor.PLAYER.with(() -> player),
				new RangerStateMessage(false, 0, 1, 0.0F,
						false, 0, false, false));
	}

	private static void clearScheduledAttacks(UUID playerId) {
		if (playerId == null)
			return;
		RAPID_STATES.remove(playerId);
		ARROW_SHOWERS.removeIf(state -> state.ownerId.equals(playerId));
		ArrayDeque<ManaArrowEntity> arrows = ORDINARY_ARROWS.remove(playerId);
		if (arrows != null) {
			for (ManaArrowEntity arrow : arrows) {
				if (arrow != null && !arrow.isRemoved())
					arrow.discard();
			}
		}
	}

	public static List<Component> tooltip(Entity entity, String skill) {
		List<Component> lines = new ArrayList<>();
		lines.add(Component.literal(skill).withStyle(net.minecraft.ChatFormatting.GREEN,
				net.minecraft.ChatFormatting.BOLD));
		String description = switch (skill) {
			case MANA_QUIVER -> "Toggle mana ammunition. Linear unlocks at B rank or System Lv60; Seeking at A rank or System Lv80.";
			case BACK_STEP -> "Retreat safely, fire a covering shot, preserve Focus, and accelerate the next draw.";
			case HAWKEYE -> "Reveal visible hostiles in your aiming cone and sharpen long-range shots.";
			case RAPID_FIRE -> "Fire five controlled spectral arrows while freely adjusting your aim.";
			case HIGH_VALUE_TARGET -> "Mark one visible target to increase Ranger damage and Focus generation.";
			case SHARPSHOOTER -> hasAllCoreSkills(entity)
					? "Typhoon Shot: pierce the formation and detonate a controlled windburst on first impact."
					: "Fire a piercing Sunder Shot that exposes a target to subsequent arrows.";
			case ARROW_SHOWER, LEGACY_PROXIMITY_TRAP -> "Rain five bounded volleys onto a distant target area.";
			case HYPER_FOCUS -> hasAllCoreSkills(entity)
					? "Fivefold Execution: manifest five rapid precision arrows."
					: "Accelerate Mana Arrow charging and double Focus generation.";
			default -> "";
		};
		lines.add(Component.literal(description).withStyle(net.minecraft.ChatFormatting.GRAY));
		return lines;
	}

	private record RapidFireState(ResourceKey<Level> dimension, ItemStack bow,
			long nextShotTick, int shotsFired) {
	}

	private static final class ArrowShowerState {
		private final UUID ownerId;
		private final ResourceKey<Level> dimension;
		private final Vec3 center;
		private final double damage;
		private int age;

		private ArrowShowerState(UUID ownerId, ResourceKey<Level> dimension,
				Vec3 center, double damage, int age) {
			this.ownerId = ownerId;
			this.dimension = dimension;
			this.center = center;
			this.damage = damage;
			this.age = age;
		}
	}
}
