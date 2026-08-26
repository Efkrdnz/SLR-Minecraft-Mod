package net.solocraft.util;

import net.solocraft.SololevelingMod;
import net.solocraft.network.SololevelingModVariables;
import net.solocraft.network.SungIlHwanAttackMessage;
import net.solocraft.network.SungIlHwanVfxEventMessage;

import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
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
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
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

/**
 * Server-authoritative combat kit for Sung Il-Hwan's Ruler vessel.
 *
 * <p>The manager deliberately keeps gameplay state separate from presentation:
 * this class decides stages, Fear, targets and damage, while
 * {@link SungIlHwanVfxEventMessage} recreates those decisions visually. Spatial
 * Execution uses collision-checked, server-authored combat teleports and always
 * attempts to restore the caster to the recorded release position.</p>
 */
@EventBusSubscriber(modid = SololevelingMod.MODID)
public final class SungIlHwanCombatManager {
	public static final String SKILL_PREDATORS_PRESENCE = "Predator's Presence";
	public static final String SKILL_ASSASSIN_STANCE = "Assassin Stance";
	public static final String SKILL_SPATIAL_EXECUTION = "Spatial Execution";
	public static final String SKILL_SPIRITUALIZATION = "Spiritualization";

	public static final int STAGE_NONE = 0;
	public static final int STAGE_ONE = 1;
	public static final int STAGE_TWO = 2;
	public static final int MAX_RISK = 100;

	private static final String IDENTITY = "sung_il_hwan";
	private static final String STATE_ROOT = "slr_sung_il_hwan";
	private static final String STATE_STAGE = "stage";
	private static final String STATE_STAGE_REMAINING = "stage_remaining";
	private static final String STATE_EXHAUSTION_REMAINING = "exhaustion_remaining";
	private static final String STATE_RISK = "rulers_fracture";
	private static final String STATE_RECOVERY_AT = "risk_recovery_at";
	private static final String STATE_PRESENCE = "presence";
	private static final String STATE_STANCE = "assassin_stance";
	private static final String STATE_LAST_DRAIN = "last_mana_drain";
	private static final String STATE_COMBO = "dagger_combo";
	private static final String STATE_COMBO_AT = "dagger_combo_at";
	private static final String SPIRITUALIZATION_AURA =
			"sung_il_hwan_spiritualization";
	private static final String PLAYER_AURA_KEY = "sololeveling_player_aura";

	private static final String FEAR_PREFIX = "slr_sih_fear_";
	private static final String FEAR_EXPIRY_PREFIX = "slr_sih_fear_expiry_";
	private static final String SEVER_COOLDOWN = "sih_spatial_sever";
	private static final int STAGE_TWO_DURATION = 20 * 18;
	private static final int EXHAUSTION_DURATION = 20 * 24;
	private static final int FEAR_LIFETIME = 20 * 8;
	private static final int MAX_AURA_TARGETS = 20;
	/**
	 * Spatial Execution is intended to include every valid entity in its sphere.
	 * This deliberately generous ceiling exists only as a pathological
	 * entity-density/network safety bound, not as ordinary target selection.
	 */
	private static final int MAX_EXECUTION_TARGETS = 96;
	private static final int MAX_PENDING_FRACTURES = MAX_EXECUTION_TARGETS;
	private static final int MAX_ASSASSIN_LINE_TARGETS = 24;
	private static final int MAX_CHARGE_TICKS = 20 * 5;
	private static final int EXECUTION_FRACTURE_DELAY = 20;
	private static final double EXECUTION_START_RADIUS = 3.0D;
	private static final double EXECUTION_MAX_RADIUS = 15.0D;
	private static final double EXECUTION_STAGE_TWO_MAX_RADIUS = 19.0D;
	private static final TagKey<net.minecraft.world.item.Item> DAGGERS =
			TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("minecraft", "dagger"));

	private static final Map<UUID, ChargeState> EXECUTION_CHARGES = new HashMap<>();
	private static final Map<UUID, ExecutionTraversal> EXECUTION_TRAVERSALS =
			new HashMap<>();
	private static final Map<UUID, List<FractureState>> PENDING_FRACTURES = new HashMap<>();
	private static final Map<UUID, Long> LAST_STANCE_ATTACK = new HashMap<>();
	private static final Set<UUID> INTERNAL_DAMAGE = new HashSet<>();

	private SungIlHwanCombatManager() {
	}

	public static boolean isSkill(String skillName) {
		return SKILL_PREDATORS_PRESENCE.equals(skillName)
				|| SKILL_ASSASSIN_STANCE.equals(skillName)
				|| SKILL_SPATIAL_EXECUTION.equals(skillName)
				|| SKILL_SPIRITUALIZATION.equals(skillName);
	}

	public static int skillColor(String skillName) {
		return switch (skillName) {
			case SKILL_PREDATORS_PRESENCE, SKILL_ASSASSIN_STANCE,
					SKILL_SPATIAL_EXECUTION, SKILL_SPIRITUALIZATION -> 0xFFD34E;
			default -> 0xFFFFFF;
		};
	}

	public static Component tooltip(Entity entity, String skillName) {
		return switch (skillName) {
			case SKILL_PREDATORS_PRESENCE -> Component.literal(
					"Toggle a silent pressure aura that builds Fear on visible enemies. Fear weakens prey and empowers Spatial Execution.");
			case SKILL_ASSASSIN_STANCE -> Component.literal(
					"Adopt a dagger stance for rapid spatial combinations. Use a dagger on prey to perform Spatial Sever.");
			case SKILL_SPATIAL_EXECUTION -> Component.literal(
					"Hold to form an execution field, then release to cut Fear-marked enemies before delayed spatial fractures erupt.");
			case SKILL_SPIRITUALIZATION -> Component.literal(
					"Toggle Stage I for sustained Ruler enhancement. While active, sneak and use again to commit to a fixed, non-cancellable Stage II followed by exhaustion; dying during Stage II or exhaustion causes recoverable Ruler's Fracture.");
			default -> Component.empty();
		};
	}

	public static boolean isSungIlHwanVessel(Entity entity) {
		if (entity == null)
			return false;
		SololevelingModVariables.PlayerVariables vars = variables(entity);
		return DeveloperModeManager.isEnabled(entity)
				&& (int) vars.JOB == 7
				&& (IDENTITY.equals(vars.vesselIdentity) || vars.vesselIdentity.isBlank());
	}

	/**
	 * Client and server predicate used by the attack mixin. The stance itself is
	 * server-owned; the client only receives its current boolean so it knows when
	 * vanilla melee must be replaced by the narrow server attack packet.
	 */
	public static boolean shouldReplaceBasicAttack(Entity entity) {
		if (!(entity instanceof Player player) || !isSungIlHwanVessel(player)
				|| !isDagger(player.getMainHandItem()))
			return false;
		if (player.level().isClientSide())
			return SungIlHwanAttackMessage.isClientStanceActive();
		return state(player).getBoolean(STATE_STANCE);
	}

	/**
	 * Handles one accepted dagger swing. All identity, equipment, stance and rate
	 * checks are repeated here; clients never supply targets, geometry or damage.
	 * The boolean reports whether this call authored a new line cut, allowing
	 * server attack-event fallbacks to cancel only attacks that were replaced.
	 */
	public static boolean performAssassinLineCut(ServerPlayer player) {
		if (player == null || !player.isAlive() || !shouldReplaceBasicAttack(player)
				|| !player.level().hasChunkAt(player.blockPosition()))
			return false;
		long now = player.level().getGameTime();
		Long lastAttack = LAST_STANCE_ATTACK.get(player.getUUID());
		int minimumInterval = stanceAttackIntervalTicks(player);
		if (lastAttack != null && now - lastAttack < minimumInterval)
			return false;
		LAST_STANCE_ATTACK.put(player.getUUID(), now);
		// Combat animation mods can reset the vanilla ticker immediately before
		// their attack-frame callback. The server interval above is authoritative,
		// so that integration detail must not turn an accepted swing into 20%
		// damage or reject it outright.
		float cooledStrength = Math.max(0.75F,
				player.getAttackStrengthScale(0.5F));

		CompoundTag stanceState = state(player);
		int currentStage = stage(player);
		boolean overloaded = currentStage == STAGE_TWO;
		int maxCombo = overloaded ? 4 : 3;
		int combo = now - stanceState.getLong(STATE_COMBO_AT) <= 16L
				? stanceState.getInt(STATE_COMBO) % maxCombo + 1 : 1;
		stanceState.putInt(STATE_COMBO, combo);
		stanceState.putLong(STATE_COMBO_AT, now);

		Vec3 direction = player.getLookAngle().normalize();
		if (direction.lengthSqr() < 0.001D)
			direction = new Vec3(0.0D, 0.0D, 1.0D);
		double range = overloaded ? 12.0D : 9.5D;
		double width = overloaded ? 2.8D : 2.1D;
		Vec3 origin = player.getEyePosition().add(direction.scale(0.3D));
		Vec3 endpoint = origin.add(direction.scale(range));

		double strength = TemporaryStatBonusManager.effectiveStrength(player);
		double agility = TemporaryStatBonusManager.effectiveAgility(player);
		double stageScale = overloaded ? 1.55D
				: currentStage == STAGE_ONE ? 1.22D : 1.0D;
		double riskScale = 1.0D
				- clampRisk(stanceState.getInt(STATE_RISK)) * 0.0025D;
		double swingScale = 0.20D + cooledStrength * cooledStrength * 0.80D;
		double weaponDamage = Math.max(1.0D,
				player.getAttributeValue(Attributes.ATTACK_DAMAGE));
		float damage = (float) ((weaponDamage + 1.5D + strength / 34.0D
				+ agility / 28.0D) * (1.0D + (combo - 1) * 0.12D)
				* stageScale * riskScale * swingScale);

		boolean confirmedHit = false;
		for (LivingEntity target : targetsAlongLine(player, origin, direction,
				range, width, overloaded ? MAX_ASSASSIN_LINE_TARGETS : 10)) {
			boolean hit = hurtInternally(player, target, damage);
			confirmedHit |= hit;
			if (hit)
				addFear(player, target, 5 + combo * 2 + (overloaded ? 4 : 0));
		}

		// A whiff still creates the full forward spatial cut. Stage II layers
		// crossed variants (>= 3) over the longer primary line.
		int seed = visualSeed(player) ^ combo * 0x45D9F3B;
		SungIlHwanVfxEventMessage.sendSpatialSlash(player, null, origin, endpoint,
				overloaded ? 3 + combo % 2 : combo - 1, confirmedHit, seed);
		if (overloaded) {
			Vec3 horizontalRight = new Vec3(-direction.z, 0.0D, direction.x);
			if (horizontalRight.lengthSqr() > 0.001D)
				horizontalRight = horizontalRight.normalize();
			SungIlHwanVfxEventMessage.sendSpatialSlash(player, null,
					origin.add(horizontalRight.scale(1.25D)).add(0.0D, -0.65D, 0.0D),
					endpoint.add(horizontalRight.scale(-1.25D)).add(0.0D, 0.8D, 0.0D),
					5, confirmedHit, seed ^ 0x51A7);
			SungIlHwanVfxEventMessage.sendSpatialSlash(player, null,
					origin.add(horizontalRight.scale(-1.0D)).add(0.0D, 0.7D, 0.0D),
					endpoint.add(horizontalRight.scale(1.0D)).add(0.0D, -0.55D, 0.0D),
					6, confirmedHit, seed ^ 0x2F19);
		}
		player.resetAttackStrengthTicker();
		player.level().playSound(null, player.blockPosition(),
				SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS,
				overloaded ? 1.15F : 0.9F, overloaded ? 1.42F : 1.62F);
		return true;
	}

	public static int fearTier(int fear) {
		int bounded = Math.max(0, fear);
		return bounded >= 75 ? 3 : bounded >= 50 ? 2 : bounded >= 25 ? 1 : 0;
	}

	public static int fearCap(LivingEntity target) {
		if (target instanceof Player)
			return 50;
		if (isBossLike(target))
			return 75;
		return 100;
	}

	public static double fearPowerScale(double casterPower, double targetPower) {
		double safeCaster = Math.max(1.0D, casterPower);
		double safeTarget = Math.max(1.0D, targetPower);
		return Mth.clamp(Math.sqrt(safeCaster / safeTarget), 0.35D, 1.65D);
	}

	public static double fearPowerScale(ServerPlayer caster, LivingEntity target) {
		return fearPowerScale(rulerPower(caster), targetPower(target));
	}

	public static int scaledFearGain(double baseGain, double powerScale) {
		return Mth.clamp((int) Math.round(Math.max(0.0D, baseGain)
				* Mth.clamp(powerScale, 0.25D, 2.0D)), 1, 24);
	}

	public static int scaledFearGain(ServerPlayer caster, LivingEntity target) {
		int base = stage(caster) == STAGE_TWO ? 10 : stage(caster) == STAGE_ONE ? 7 : 5;
		return scaledFearGain(base, fearPowerScale(caster, target));
	}

	public static boolean canCommitStageTwo(Entity entity) {
		if (!(entity instanceof ServerPlayer player) || !isSungIlHwanVessel(player))
			return false;
		CompoundTag state = state(player);
		return state.getInt(STATE_STAGE) == STAGE_ONE
				&& state.getInt(STATE_EXHAUSTION_REMAINING) <= 0
				&& player.isAlive() && player.isShiftKeyDown();
	}

	public static int clampRisk(int risk) {
		return Mth.clamp(risk, 0, MAX_RISK);
	}

	public static int executionChargeTier(int pressedMs) {
		int heldTicks = Math.max(0, pressedMs / 50);
		return heldTicks >= 50 ? 3 : heldTicks >= 28 ? 2 : heldTicks >= 12 ? 1 : 0;
	}

	public static void press(Entity entity, String skillName) {
		if (!(entity instanceof ServerPlayer player) || !isSungIlHwanVessel(player)
				|| !player.isAlive())
			return;
		switch (skillName) {
			case SKILL_PREDATORS_PRESENCE -> togglePresence(player);
			case SKILL_ASSASSIN_STANCE -> toggleAssassinStance(player);
			case SKILL_SPATIAL_EXECUTION -> beginSpatialExecution(player);
			case SKILL_SPIRITUALIZATION -> useSpiritualization(player);
			default -> {
			}
		}
	}

	public static void release(Entity entity, String skillName, int pressedMs) {
		if (!(entity instanceof ServerPlayer player)
				|| !SKILL_SPATIAL_EXECUTION.equals(skillName))
			return;
		releaseSpatialExecution(player, pressedMs);
	}

	/**
	 * Clears all Sung runtime and persistent vessel state. Intended for explicit
	 * vessel reset/admin recovery; ordinary death and logout deliberately do not
	 * call this method.
	 */
	public static void resetPlayerState(ServerPlayer player) {
		if (player == null)
			return;
		EXECUTION_CHARGES.remove(player.getUUID());
		abortExecutionTraversal(player, true);
		PENDING_FRACTURES.remove(player.getUUID());
		LAST_STANCE_ATTACK.remove(player.getUUID());
		CompoundTag outer = player.getPersistentData();
		if (outer.contains(Player.PERSISTED_NBT_TAG, Tag.TAG_COMPOUND)) {
			CompoundTag persisted = outer.getCompound(Player.PERSISTED_NBT_TAG);
			persisted.remove(STATE_ROOT);
			outer.put(Player.PERSISTED_NBT_TAG, persisted);
		}
		clearSpiritualizationAura(player);
		SungIlHwanAttackMessage.syncStance(player, false);
		SungIlHwanVfxEventMessage.sendExecutionCancel(player, visualSeed(player));
		SungIlHwanVfxEventMessage.sendStageEnd(player, visualSeed(player));
	}

	public static void tick(ServerPlayer player) {
		if (player == null)
			return;
		if (!isSungIlHwanVessel(player)) {
			teardownAfterIdentityLoss(player);
			return;
		}
		CompoundTag state = state(player);
		long now = player.level().getGameTime();
		int currentStage = Mth.clamp(state.getInt(STATE_STAGE), STAGE_NONE, STAGE_TWO);

		if (currentStage == STAGE_TWO) {
			int stageRemaining = Math.max(0, state.getInt(STATE_STAGE_REMAINING));
			if (stageRemaining <= 0) {
				finishStageTwo(player, state);
				currentStage = STAGE_NONE;
			} else {
				state.putInt(STATE_STAGE_REMAINING, stageRemaining - 1);
			}
		}

		if (currentStage == STAGE_ONE) {
			applyStageEffects(player, false);
			if (now % (20L * 45L) == 0L)
				SungIlHwanVfxEventMessage.sendStage(player, STAGE_ONE,
						20 * 60, visualSeed(player));
			if (now - state.getLong(STATE_LAST_DRAIN) >= 20L) {
				state.putLong(STATE_LAST_DRAIN, now);
				if (!consumeManaSilently(player, 16 + state.getInt(STATE_RISK) / 20))
					deactivateStageOne(player, state, true);
			}
		} else if (currentStage == STAGE_TWO) {
			applyStageEffects(player, true);
		}

		int exhaustionRemaining = Math.max(0,
				state.getInt(STATE_EXHAUSTION_REMAINING));
		if (exhaustionRemaining > 0) {
			state.putInt(STATE_EXHAUSTION_REMAINING,
					exhaustionRemaining - 1);
			int riskTier = Math.max(0, state.getInt(STATE_RISK) / 25);
			player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 12,
					Math.min(3, 1 + riskTier), false, false, true));
			player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 12,
					Math.min(2, riskTier), false, false, true));
			// ManaRegenProcedure honors this shared key. A bounded refresh cadence
			// suppresses passive regeneration without touching potions or permanent stats.
			if (now % 5L == 0L)
				CooldownManager.setFullDuration(player, "mana_refresh", 8);
		} else {
			recoverRisk(player, state, now);
		}

		if (state.getBoolean(STATE_PRESENCE))
			tickPresence(player, state, now);
		if (state.getBoolean(STATE_STANCE) && !hasDagger(player)) {
			state.putBoolean(STATE_STANCE, false);
			SungIlHwanAttackMessage.syncStance(player, false);
			player.displayClientMessage(Component.literal("Assassin Stance ended: equip a dagger")
					.withStyle(ChatFormatting.GRAY), true);
		}

		tickExecutionCharge(player, now);
		tickExecutionTraversal(player, now);
		tickFractures(player, now);
	}

	@SubscribeEvent
	public static void onPlayerTick(PlayerTickEvent.Post event) {
		if (true && event.getEntity() instanceof ServerPlayer player)
			tick(player);
	}

	/**
	 * Vanilla and Better Combat both eventually enter Player.attack when they hit
	 * something. Use that as an authoritative fallback, then suppress only the
	 * original hit that was actually replaced (or belongs to the same multi-hit
	 * attack frame). Empty swings are supplied by the narrow client request.
	 */
	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void onAttackEntity(AttackEntityEvent event) {
		if (!(event.getEntity() instanceof ServerPlayer player)
				|| INTERNAL_DAMAGE.contains(player.getUUID()))
			return;
		if (shouldReplaceBasicAttack(player)
				&& replaceOrAlreadyReplacedAssassinAttack(player))
			event.setCanceled(true);
	}

	/**
	 * Fallback for combat overhauls that apply player damage without calling
	 * Player.attack/AttackEntityEvent. INTERNAL_DAMAGE prevents the line cut's
	 * own damage from re-entering this handler.
	 */
	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void onAssassinLivingAttack(LivingIncomingDamageEvent event) {
		if (!(event.getSource().getEntity() instanceof ServerPlayer player)
				|| event.getSource().getDirectEntity() != player
				|| INTERNAL_DAMAGE.contains(player.getUUID())
				|| CartenonSuppression.blockVesselPassive(player)
				|| !shouldReplaceBasicAttack(player))
			return;
		if (replaceOrAlreadyReplacedAssassinAttack(player))
			event.setCanceled(true);
	}

	@SubscribeEvent
	public static void onDaggerDamage(LivingIncomingDamageEvent event) {
		if (event.getEntity() instanceof ServerPlayer victim
				&& isSungIlHwanVessel(victim)
				&& !CartenonSuppression.blockVesselPassive(victim)
				&& state(victim).getInt(STATE_EXHAUSTION_REMAINING) > 0) {
			int risk = state(victim).getInt(STATE_RISK);
			event.setAmount(event.getAmount()
					* (1.16F + Mth.clamp(risk, 0, MAX_RISK) * 0.0018F));
		}
		if (!(event.getSource().getEntity() instanceof ServerPlayer player)
				|| INTERNAL_DAMAGE.contains(player.getUUID())
				|| !isSungIlHwanVessel(player)
				|| !state(player).getBoolean(STATE_STANCE)
				|| !isDagger(player.getMainHandItem())
				|| !MageCombatHelper.isValidTarget(player, event.getEntity()))
			return;

		CompoundTag state = state(player);
		long now = player.level().getGameTime();
		int maxCombo = stage(player) == STAGE_TWO ? 4 : 3;
		int combo = now - state.getLong(STATE_COMBO_AT) <= 16L
				? state.getInt(STATE_COMBO) % maxCombo + 1 : 1;
		state.putInt(STATE_COMBO, combo);
		state.putLong(STATE_COMBO_AT, now);

		double strength = TemporaryStatBonusManager.effectiveStrength(player);
		double agility = TemporaryStatBonusManager.effectiveAgility(player);
		double stageScale = stage(player) == STAGE_TWO ? 1.45D
				: stage(player) == STAGE_ONE ? 1.20D : 1.0D;
		double riskScale = 1.0D - state.getInt(STATE_RISK) * 0.0025D;
		float addition = (float) ((2.0D + strength / 34.0D + agility / 28.0D)
				* (1.0D + (combo - 1) * 0.13D) * stageScale * riskScale);
		event.setAmount(event.getAmount() + addition);
		addFear(player, event.getEntity(), 5 + combo * 2);

		Vec3 from = player.getEyePosition().add(player.getLookAngle().scale(0.35D));
		Vec3 to = event.getEntity().getBoundingBox().getCenter();
		SungIlHwanVfxEventMessage.sendSpatialSlash(player, event.getEntity(), from, to,
				combo - 1, true, visualSeed(player) ^ event.getEntity().getId());
		if (stage(player) == STAGE_TWO) {
			Vec3 crossFrom = to.add(combo % 2 == 0 ? -1.0D : 1.0D,
					0.55D, combo % 2 == 0 ? 0.7D : -0.7D);
			SungIlHwanVfxEventMessage.sendSpatialSlash(player,
					event.getEntity(), crossFrom, to, combo + 2, true,
					visualSeed(player) ^ event.getEntity().getId() ^ 0x51A7);
		}
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void applyExhaustionAndFractureDamage(LivingIncomingDamageEvent event) {
		if (!(event.getSource().getEntity() instanceof ServerPlayer player)
				|| !isSungIlHwanVessel(player) || event.getEntity() == player)
			return;
		CompoundTag state = state(player);
		float multiplier = 1.0F
				- clampRisk(state.getInt(STATE_RISK)) * 0.0015F;
		if (state.getInt(STATE_EXHAUSTION_REMAINING) > 0)
			multiplier *= 0.72F;
		event.setAmount(Math.max(0.0F, event.getAmount() * multiplier));
	}

	@SubscribeEvent
	public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
		if (event.getHand() != InteractionHand.MAIN_HAND
				|| !(event.getEntity() instanceof ServerPlayer player)
				|| !(event.getTarget() instanceof LivingEntity target))
			return;
		if (trySpatialSever(player, target)) {
			event.setCanceled(true);
			player.swing(InteractionHand.MAIN_HAND, true);
		}
	}

	@SubscribeEvent
	public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
		if (event.getHand() != InteractionHand.MAIN_HAND
				|| !(event.getEntity() instanceof ServerPlayer player)
				|| !isSungIlHwanVessel(player)
				|| !state(player).getBoolean(STATE_STANCE)
				|| !isDagger(player.getMainHandItem()))
			return;
		LivingEntity target = findLookTarget(player,
				stage(player) == STAGE_TWO ? 12.0D : 8.0D);
		if (target != null && trySpatialSever(player, target)) {
			event.setCanceled(true);
			player.swing(InteractionHand.MAIN_HAND, true);
		}
	}

	@SubscribeEvent
	public static void onDeath(LivingDeathEvent event) {
		if (!(event.getEntity() instanceof ServerPlayer player) || !isSungIlHwanVessel(player))
			return;
		CompoundTag state = state(player);
		boolean stageTwo = state.getInt(STATE_STAGE) == STAGE_TWO;
		boolean exhausted = state.getInt(STATE_EXHAUSTION_REMAINING) > 0;
		if (stageTwo || exhausted) {
			int increase = stageTwo ? 22 : 12;
			state.putInt(STATE_RISK, clampRisk(state.getInt(STATE_RISK) + increase));
			state.putInt(STATE_STAGE, STAGE_NONE);
			state.putInt(STATE_STAGE_REMAINING, 0);
			state.putInt(STATE_EXHAUSTION_REMAINING,
					Math.max(state.getInt(STATE_EXHAUSTION_REMAINING),
							EXHAUSTION_DURATION));
			CooldownManager.setFullDuration(player, "mana_refresh", 12);
		}
		EXECUTION_CHARGES.remove(player.getUUID());
		state.putBoolean(STATE_STANCE, false);
		SungIlHwanAttackMessage.syncStance(player, false);
		clearSpiritualizationAura(player);
		abortExecutionTraversal(player, false);
		PENDING_FRACTURES.remove(player.getUUID());
		LAST_STANCE_ATTACK.remove(player.getUUID());
	}

	@SubscribeEvent
	public static void onClone(PlayerEvent.Clone event) {
		if (!(event.getOriginal() instanceof ServerPlayer original)
				|| !(event.getEntity() instanceof ServerPlayer replacement))
			return;
		CompoundTag oldOuter = original.getPersistentData();
		if (!oldOuter.contains(Player.PERSISTED_NBT_TAG, Tag.TAG_COMPOUND))
			return;
		CompoundTag oldPersisted = oldOuter.getCompound(Player.PERSISTED_NBT_TAG);
		if (!oldPersisted.contains(STATE_ROOT, Tag.TAG_COMPOUND))
			return;
		CompoundTag newOuter = replacement.getPersistentData();
		CompoundTag newPersisted = newOuter.contains(Player.PERSISTED_NBT_TAG, Tag.TAG_COMPOUND)
				? newOuter.getCompound(Player.PERSISTED_NBT_TAG) : new CompoundTag();
		newPersisted.put(STATE_ROOT, oldPersisted.getCompound(STATE_ROOT).copy());
		newOuter.put(Player.PERSISTED_NBT_TAG, newPersisted);
	}

	@SubscribeEvent
	public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
		if (!(event.getEntity() instanceof ServerPlayer player) || !isSungIlHwanVessel(player))
			return;
		CompoundTag state = state(player);
		state.putInt(STATE_RISK, clampRisk(state.getInt(STATE_RISK)));
		int currentStage = Mth.clamp(state.getInt(STATE_STAGE), STAGE_NONE, STAGE_TWO);
		state.putInt(STATE_STAGE, currentStage);
		syncPersistentPresentation(player, state);
		SungIlHwanAttackMessage.syncStance(player,
				state.getBoolean(STATE_STANCE) && hasDagger(player));
	}

	@SubscribeEvent
	public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
		if (!(event.getEntity() instanceof ServerPlayer player)
				|| !isSungIlHwanVessel(player))
			return;
		CompoundTag state = state(player);
		syncPersistentPresentation(player, state);
		SungIlHwanAttackMessage.syncStance(player,
				state.getBoolean(STATE_STANCE) && hasDagger(player));
		int risk = clampRisk(state.getInt(STATE_RISK));
		if (risk > 0)
			SungIlHwanVfxEventMessage.sendRiskFeedback(player, riskSeverity(risk),
					40, visualSeed(player));
	}

	@SubscribeEvent
	public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
		if (!(event.getEntity() instanceof ServerPlayer player))
			return;
		EXECUTION_CHARGES.remove(player.getUUID());
		abortExecutionTraversal(player, true);
		PENDING_FRACTURES.remove(player.getUUID());
		LAST_STANCE_ATTACK.remove(player.getUUID());
	}

	@SubscribeEvent
	public static void onDimensionChanged(PlayerEvent.PlayerChangedDimensionEvent event) {
		if (!(event.getEntity() instanceof ServerPlayer player))
			return;
		ChargeState charge = EXECUTION_CHARGES.remove(player.getUUID());
		if (charge != null)
			SungIlHwanVfxEventMessage.sendExecutionCancel(player, charge.seed);
		// A dimension transition invalidates both the recorded traversal origin
		// and the delayed target level. Cancel; never resolve queued damage early.
		abortExecutionTraversal(player, false);
		PENDING_FRACTURES.remove(player.getUUID());
		LAST_STANCE_ATTACK.remove(player.getUUID());
	}

	@SubscribeEvent
	public static void onServerStopping(ServerStoppingEvent event) {
		EXECUTION_CHARGES.clear();
		EXECUTION_TRAVERSALS.clear();
		PENDING_FRACTURES.clear();
		LAST_STANCE_ATTACK.clear();
		INTERNAL_DAMAGE.clear();
	}

	private static void togglePresence(ServerPlayer player) {
		CompoundTag state = state(player);
		boolean activating = !state.getBoolean(STATE_PRESENCE);
		if (activating && !consumeMana(player, 90))
			return;
		state.putBoolean(STATE_PRESENCE, activating);
		player.displayClientMessage(Component.literal(activating
				? "Predator's Presence spreads across the battlefield"
				: "Predator's Presence withdrawn")
				.withStyle(activating ? ChatFormatting.DARK_PURPLE : ChatFormatting.GRAY), true);
		if (activating)
			SungIlHwanVfxEventMessage.sendFearPulse(player, player.position(),
					stage(player) == STAGE_TWO ? 18.0D : 13.0D, visualSeed(player));
	}

	private static void toggleAssassinStance(ServerPlayer player) {
		CompoundTag state = state(player);
		boolean activating = !state.getBoolean(STATE_STANCE);
		if (activating && !hasDagger(player)) {
			player.displayClientMessage(Component.literal("Assassin Stance requires a dagger")
					.withStyle(ChatFormatting.RED), true);
			return;
		}
		if (activating && !consumeMana(player, 70))
			return;
		state.putBoolean(STATE_STANCE, activating);
		state.putInt(STATE_COMBO, 0);
		SungIlHwanAttackMessage.syncStance(player, activating);
		player.displayClientMessage(Component.literal(activating
				? "Assassin Stance ready" : "Assassin Stance released")
				.withStyle(activating ? ChatFormatting.AQUA : ChatFormatting.GRAY), true);
	}

	private static void useSpiritualization(ServerPlayer player) {
		CompoundTag state = state(player);
		int currentStage = state.getInt(STATE_STAGE);
		if (currentStage == STAGE_TWO) {
			player.displayClientMessage(Component.literal("Stage II cannot be cancelled")
					.withStyle(ChatFormatting.LIGHT_PURPLE), true);
			return;
		}
		if (currentStage == STAGE_ONE) {
			if (player.isShiftKeyDown()) {
				commitStageTwo(player, state);
			} else {
				deactivateStageOne(player, state, false);
			}
			return;
		}
		if (state.getInt(STATE_EXHAUSTION_REMAINING) > 0) {
			player.displayClientMessage(Component.literal("Ruler channels are exhausted")
					.withStyle(ChatFormatting.RED), true);
			return;
		}
		if (CooldownManager.isOnCooldown(player, SKILL_SPIRITUALIZATION)) {
			player.displayClientMessage(Component.literal("Spiritualization is recovering")
					.withStyle(ChatFormatting.RED), true);
			return;
		}
		if (!consumeMana(player, 180))
			return;
		state.putInt(STATE_STAGE, STAGE_ONE);
		state.putInt(STATE_STAGE_REMAINING, 0);
		long now = player.level().getGameTime();
		state.putLong(STATE_LAST_DRAIN, now);
		setSpiritualizationAura(player, STAGE_ONE);
		SungIlHwanVfxEventMessage.sendStage(player, STAGE_ONE, 20 * 60, visualSeed(player));
		player.displayClientMessage(Component.literal(
				"Stage I active. Sneak-use Spiritualization to commit to Stage II; death during Stage II or exhaustion deepens Ruler's Fracture.")
				.withStyle(ChatFormatting.LIGHT_PURPLE), false);
		player.level().playSound(null, player.blockPosition(), SoundEvents.BEACON_POWER_SELECT,
				SoundSource.PLAYERS, 0.75F, 1.45F);
	}

	private static void commitStageTwo(ServerPlayer player, CompoundTag state) {
		if (!canCommitStageTwo(player))
			return;
		int cost = 620 + state.getInt(STATE_RISK) * 3;
		if (!consumeMana(player, cost))
			return;
		state.putInt(STATE_STAGE, STAGE_TWO);
		state.putInt(STATE_STAGE_REMAINING, STAGE_TWO_DURATION);
		setSpiritualizationAura(player, STAGE_TWO);
		SungIlHwanVfxEventMessage.sendStage(player, STAGE_TWO, STAGE_TWO_DURATION,
				visualSeed(player));
		player.displayClientMessage(Component.literal("Spiritualization Stage II committed")
				.withStyle(ChatFormatting.LIGHT_PURPLE), true);
		player.level().playSound(null, player.blockPosition(), SoundEvents.RESPAWN_ANCHOR_CHARGE,
				SoundSource.PLAYERS, 1.0F, 1.25F);
	}

	private static void deactivateStageOne(ServerPlayer player, CompoundTag state,
			boolean depleted) {
		state.putInt(STATE_STAGE, STAGE_NONE);
		state.putInt(STATE_STAGE_REMAINING, 0);
		CooldownManager.set(player, SKILL_SPIRITUALIZATION, depleted ? 100 : 50);
		clearSpiritualizationAura(player);
		SungIlHwanVfxEventMessage.sendStageEnd(player, visualSeed(player));
		if (depleted)
			player.displayClientMessage(Component.literal("Stage I ended: insufficient MP")
					.withStyle(ChatFormatting.RED), true);
	}

	private static void finishStageTwo(ServerPlayer player, CompoundTag state) {
		state.putInt(STATE_STAGE, STAGE_NONE);
		state.putInt(STATE_STAGE_REMAINING, 0);
		state.putInt(STATE_EXHAUSTION_REMAINING, EXHAUSTION_DURATION);
		CooldownManager.setFullDuration(player, SKILL_SPIRITUALIZATION,
				EXHAUSTION_DURATION + 20 * 8);
		clearSpiritualizationAura(player);
		SungIlHwanVfxEventMessage.sendStageEnd(player, visualSeed(player));
		SungIlHwanVfxEventMessage.sendExhaustion(player,
				riskSeverity(state.getInt(STATE_RISK)), EXHAUSTION_DURATION,
				visualSeed(player));
	}

	private static void applyStageEffects(ServerPlayer player, boolean secondStage) {
		int riskPenalty = state(player).getInt(STATE_RISK) >= 75 ? 1 : 0;
		player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 12,
				Math.max(0, (secondStage ? 3 : 1) - riskPenalty), false, false, true));
		player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 12,
				Math.max(0, (secondStage ? 2 : 0) - riskPenalty), false, false, true));
		player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 12,
				Math.max(0, (secondStage ? 3 : 0) - riskPenalty), false, false, true));
		if (secondStage)
			player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 12,
					riskPenalty == 0 ? 1 : 0, false, false, true));
	}

	private static void recoverRisk(ServerPlayer player, CompoundTag state, long now) {
		int risk = clampRisk(state.getInt(STATE_RISK));
		if (risk <= 0)
			return;
		long recoverAt = state.getLong(STATE_RECOVERY_AT);
		if (recoverAt == 0L) {
			state.putLong(STATE_RECOVERY_AT, now + 20 * 45L);
			return;
		}
		if (now >= recoverAt && state.getInt(STATE_STAGE) == STAGE_NONE) {
			state.putInt(STATE_RISK, risk - 1);
			state.putLong(STATE_RECOVERY_AT, now + 20 * 45L);
			if ((risk - 1) % 10 == 0)
				SungIlHwanVfxEventMessage.sendRiskFeedback(player,
						riskSeverity(risk - 1), 24, visualSeed(player));
		}
	}

	/**
	 * Removing the vessel cancels its live combat modes, but deliberately retains
	 * Ruler's Fracture so swapping identity cannot cleanse the Stage II penalty.
	 */
	private static void teardownAfterIdentityLoss(ServerPlayer player) {
		clearSpiritualizationAura(player);
		SungIlHwanAttackMessage.syncStance(player, false);
		abortExecutionTraversal(player, true);
		ChargeState charge = EXECUTION_CHARGES.remove(player.getUUID());
		PENDING_FRACTURES.remove(player.getUUID());
		LAST_STANCE_ATTACK.remove(player.getUUID());
		if (charge != null)
			SungIlHwanVfxEventMessage.sendExecutionCancel(player, charge.seed);
		CompoundTag outer = player.getPersistentData();
		if (!outer.contains(Player.PERSISTED_NBT_TAG, Tag.TAG_COMPOUND))
			return;
		CompoundTag persisted = outer.getCompound(Player.PERSISTED_NBT_TAG);
		if (!persisted.contains(STATE_ROOT, Tag.TAG_COMPOUND))
			return;
		CompoundTag sungState = persisted.getCompound(STATE_ROOT);
		int previousStage = sungState.getInt(STATE_STAGE);
		boolean visuallyActive = previousStage != STAGE_NONE;
		if (previousStage == STAGE_TWO)
			sungState.putInt(STATE_EXHAUSTION_REMAINING,
					Math.max(sungState.getInt(STATE_EXHAUSTION_REMAINING),
							EXHAUSTION_DURATION));
		sungState.putInt(STATE_STAGE, STAGE_NONE);
		sungState.putInt(STATE_STAGE_REMAINING, 0);
		sungState.putBoolean(STATE_PRESENCE, false);
		sungState.putBoolean(STATE_STANCE, false);
		persisted.put(STATE_ROOT, sungState);
		outer.put(Player.PERSISTED_NBT_TAG, persisted);
		if (visuallyActive)
			SungIlHwanVfxEventMessage.sendStageEnd(player, visualSeed(player));
	}

	private static void tickPresence(ServerPlayer player, CompoundTag state, long now) {
		if (now % 10L != 0L)
			return;
		if (now % 40L == 0L && !consumeManaSilently(player,
				stage(player) == STAGE_TWO ? 10 : 7)) {
			state.putBoolean(STATE_PRESENCE, false);
			player.displayClientMessage(Component.literal("Predator's Presence faded: insufficient MP")
					.withStyle(ChatFormatting.RED), true);
			return;
		}
		double radius = stage(player) == STAGE_TWO ? 18.0D
				: stage(player) == STAGE_ONE ? 15.0D : 12.0D;
		List<LivingEntity> targets = nearbyTargets(player,
				player.getBoundingBox().inflate(radius, Math.min(8.0D, radius), radius),
				MAX_AURA_TARGETS);
		for (LivingEntity target : targets) {
			if (isFearExposed(player, target))
				addFear(player, target, scaledFearGain(player, target));
		}
		if (now % 20L == 0L)
			SungIlHwanVfxEventMessage.sendFearPulse(player, player.position(), radius,
					visualSeed(player));
	}

	private static boolean trySpatialSever(ServerPlayer player, LivingEntity target) {
		boolean overloaded = stage(player) == STAGE_TWO;
		double maximumRange = overloaded ? 12.0D : 10.0D;
		if (!isSungIlHwanVessel(player) || !state(player).getBoolean(STATE_STANCE)
				|| !isDagger(player.getMainHandItem())
				|| !MageCombatHelper.isValidTarget(player, target)
				|| player.distanceToSqr(target) > maximumRange * maximumRange
				|| !player.hasLineOfSight(target)
				|| CooldownManager.isOnCooldown(player, SEVER_COOLDOWN))
			return false;
		int stage = stage(player);
		int mana = stage == STAGE_TWO ? 80 : 110;
		if (!consumeMana(player, mana))
			return false;

		int fear = fear(player, target);
		int consumed = Math.min(fear, 35);
		setFear(player, target, fear - consumed);
		double strength = TemporaryStatBonusManager.effectiveStrength(player);
		double agility = TemporaryStatBonusManager.effectiveAgility(player);
		float damage = (float) ((10.0D + strength / 16.0D + agility / 13.0D)
				* (1.0D + consumed * 0.012D)
				* (stage == STAGE_TWO ? 1.35D : stage == STAGE_ONE ? 1.14D : 1.0D));
		boolean hit = hurtInternally(player, target,
				overloaded ? damage * 0.72F : damage);
		if (overloaded)
			hit |= hurtInternally(player, target, damage * 0.28F);
		if (hit) {
			target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 25, 2,
					false, false, true));
			addFear(player, target, 14);
		}
		CooldownManager.set(player, SEVER_COOLDOWN, stage == STAGE_TWO ? 28 : 42);
		if (stage == STAGE_ONE)
			CooldownManager.set(player, SEVER_COOLDOWN, 35);
		SungIlHwanVfxEventMessage.sendSpatialSlash(player, target,
				player.getEyePosition(), target.getBoundingBox().getCenter(), 5, hit,
				visualSeed(player) ^ target.getId());
		if (overloaded)
			SungIlHwanVfxEventMessage.sendSpatialSlash(player, target,
					target.getBoundingBox().getCenter().add(-1.2D, 0.8D, 0.8D),
					target.getBoundingBox().getCenter(), 6, hit,
					visualSeed(player) ^ target.getId() ^ 0x2F19);
		player.level().playSound(null, target.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP,
				SoundSource.PLAYERS, 0.9F, 1.55F);
		return true;
	}

	private static void beginSpatialExecution(ServerPlayer player) {
		if (EXECUTION_CHARGES.containsKey(player.getUUID())
				|| EXECUTION_TRAVERSALS.containsKey(player.getUUID())
				|| CooldownManager.isOnCooldown(player, SKILL_SPATIAL_EXECUTION))
			return;
		Vec3 focus = executionCenter(player);
		int seed = visualSeed(player);
		EXECUTION_CHARGES.put(player.getUUID(),
				new ChargeState(player.level().getGameTime(), seed));
		double maximumRadius = stage(player) == STAGE_TWO
				? EXECUTION_STAGE_TWO_MAX_RADIUS : EXECUTION_MAX_RADIUS;
		SungIlHwanVfxEventMessage.sendExecutionCharge(player, focus, maximumRadius,
				MAX_CHARGE_TICKS, seed);
	}

	private static void tickExecutionCharge(ServerPlayer player, long now) {
		ChargeState charge = EXECUTION_CHARGES.get(player.getUUID());
		if (charge == null)
			return;
		long heldTicks = Math.max(0L, now - charge.startedAt);
		if (!player.isAlive() || !isSungIlHwanVessel(player)) {
			EXECUTION_CHARGES.remove(player.getUUID());
			SungIlHwanVfxEventMessage.sendExecutionCancel(player, charge.seed);
			return;
		}
		if (heldTicks % 5L != 0L)
			return;
		Vec3 focus = executionCenter(player);
		double radius = executionRadius(player, heldTicks);
		// Once fully charged, keep a short rolling presentation lifetime so
		// holding slightly longer never makes the field vanish or cancels it.
		int remaining = (int) Math.max(8L, MAX_CHARGE_TICKS - heldTicks + 8L);
		// One null-target update refreshes the player-centred expanding sphere;
		// the bounded target updates that follow mark every entity currently inside.
		SungIlHwanVfxEventMessage.sendExecutionTarget(player, null, focus, radius,
				remaining, charge.seed);
		int markIndex = 0;
		for (LivingEntity target : executionTargets(player, focus, radius)) {
			SungIlHwanVfxEventMessage.sendExecutionTarget(player, target, focus, radius,
					remaining, charge.seed + 31 * ++markIndex);
		}
	}

	private static void releaseSpatialExecution(ServerPlayer player, int pressedMs) {
		ChargeState charge = EXECUTION_CHARGES.remove(player.getUUID());
		if (charge == null)
			return;
		if (!player.isAlive() || !isSungIlHwanVessel(player)) {
			SungIlHwanVfxEventMessage.sendExecutionCancel(player, charge.seed);
			return;
		}
		int elapsedMs = (int) Math.min(Integer.MAX_VALUE,
				Math.max(0L, player.level().getGameTime() - charge.startedAt) * 50L);
		// Key-release duration arrives from the client and is presentation-only.
		// Charge authority is exclusively the server's elapsed game time.
		int tier = executionChargeTier(elapsedMs);
		if (tier == 0) {
			SungIlHwanVfxEventMessage.sendExecutionCancel(player, charge.seed);
			return;
		}
		Vec3 originalPosition = player.position();
		Vec3 safeReturnPosition = findSafeExecutionReturnPosition(player,
				player.serverLevel(), originalPosition, 6, true);
		if (safeReturnPosition == null) {
			player.displayClientMessage(Component.literal(
					"Spatial Execution needs stable ground to return to")
					.withStyle(ChatFormatting.RED), true);
			SungIlHwanVfxEventMessage.sendExecutionCancel(player, charge.seed);
			return;
		}
		int mana = switch (tier) {
			case 3 -> 760;
			case 2 -> 510;
			default -> 310;
		};
		if (!consumeMana(player, mana)) {
			SungIlHwanVfxEventMessage.sendExecutionCancel(player, charge.seed);
			return;
		}

		int currentStage = stage(player);
		boolean overloaded = currentStage == STAGE_TWO;
		long heldTicks = Math.max(0L,
				player.level().getGameTime() - charge.startedAt);
		Vec3 focus = executionCenter(player);
		double radius = executionRadius(player, heldTicks);
		List<LivingEntity> targets = executionTargets(player, focus, radius);
		if (targets.isEmpty()) {
			CooldownManager.set(player, SKILL_SPATIAL_EXECUTION, 35);
			SungIlHwanVfxEventMessage.sendExecutionCancel(player, charge.seed);
			return;
		}

		double strength = TemporaryStatBonusManager.effectiveStrength(player);
		double agility = TemporaryStatBonusManager.effectiveAgility(player);
		double intelligence = TemporaryStatBonusManager.effectiveIntelligence(player);
		double base = (18.0D + strength / 13.0D + agility / 11.0D + intelligence / 24.0D)
				* (0.75D + tier * 0.28D)
				* (overloaded ? 1.35D : currentStage == STAGE_ONE ? 1.12D : 1.0D);
		ArrayList<ExecutionTarget> traversalTargets = new ArrayList<>();
		for (LivingEntity target : targets) {
			int storedFear = fear(player, target);
			int consumedFear = Math.min(storedFear, 75);
			setFear(player, target, storedFear - consumedFear);
			float totalDamage = (float) (base * (1.0D + consumedFear * 0.012D));
			traversalTargets.add(new ExecutionTarget(target.getUUID(),
					totalDamage, target.getBoundingBox().getCenter()));
		}

		ExecutionTraversal traversal = new ExecutionTraversal(
				player.level().dimension(), originalPosition, safeReturnPosition,
				player.getYRot(), player.getXRot(), focus, radius, tier, charge.seed,
				overloaded, traversalTargets, player.level().getGameTime());
		EXECUTION_TRAVERSALS.put(player.getUUID(), traversal);
		CooldownManager.set(player, SKILL_SPATIAL_EXECUTION,
				currentStage == STAGE_TWO ? 220 + tier * 25
						: currentStage == STAGE_ONE ? 260 + tier * 30
						: 300 + tier * 35);
		// Release is public, so never leak a marked target entity id.
		SungIlHwanVfxEventMessage.sendExecutionRelease(player, null, focus,
				radius, tier, charge.seed);
	}

	private static void tickExecutionTraversal(ServerPlayer player, long now) {
		ExecutionTraversal traversal = EXECUTION_TRAVERSALS.get(player.getUUID());
		if (traversal == null || now < traversal.nextStepAt)
			return;
		if (!player.isAlive() || !isSungIlHwanVessel(player)
				|| !player.level().dimension().equals(traversal.dimension)) {
			abortExecutionTraversal(player,
					player.isAlive() && player.level().dimension().equals(traversal.dimension));
			return;
		}
		if (traversal.nextTarget >= traversal.targets.size()) {
			completeExecutionTraversal(player, traversal, now);
			return;
		}

		ExecutionTarget marked = traversal.targets.get(traversal.nextTarget);
		ServerLevel level = player.server.getLevel(traversal.dimension);
		Entity entity = level == null ? null : level.getEntity(marked.targetId);
		Vec3 from = player.getEyePosition();
		if (entity instanceof LivingEntity target
				&& MageCombatHelper.isValidTarget(player, target)) {
			// This helper rejects unloaded, out-of-border, colliding, liquid,
			// hazardous and unsupported destinations. A failed move remains a
			// visual cut rather than risking suffocation or a void fall.
			IgrisCombatTeleportHelper.tryMoveBehindTarget(player, target);
			Vec3 point = target.getBoundingBox().getCenter();
			int index = traversal.nextTarget;
			SungIlHwanVfxEventMessage.sendSpatialSlash(player, target, from, point,
					index % 3, false, traversal.seed + index * 31);
			if (traversal.overloaded) {
				Vec3 crossStart = point.add(index % 2 == 0 ? -1.4D : 1.4D,
						0.8D, index % 2 == 0 ? 0.9D : -0.9D);
				SungIlHwanVfxEventMessage.sendSpatialSlash(player, target,
						crossStart, point, 3 + index % 4, false,
						traversal.seed + index * 31 + 13);
			}
		}
		traversal.nextTarget++;
		traversal.nextStepAt = now + 1L;
	}

	private static void completeExecutionTraversal(ServerPlayer player,
			ExecutionTraversal traversal, long now) {
		if (EXECUTION_TRAVERSALS.remove(player.getUUID()) != traversal)
			return;
		if (!returnToExecutionOrigin(player, traversal)) {
			// Never schedule the delayed strike if the caster could not be
			// restored safely. The traversal is canceled without early damage.
			SungIlHwanVfxEventMessage.sendExecutionCancel(player, traversal.seed);
			player.displayClientMessage(Component.literal(
					"Spatial Execution canceled: no safe return point")
					.withStyle(ChatFormatting.RED), true);
			return;
		}
		long sharedExecuteAt = now + EXECUTION_FRACTURE_DELAY;
		ArrayList<FractureState> pending = new ArrayList<>();
		for (ExecutionTarget target : traversal.targets) {
			if (pending.size() >= MAX_PENDING_FRACTURES)
				break;
			pending.add(new FractureState(target.targetId, sharedExecuteAt,
					target.damage, target.focus, traversal.dimension));
		}
		if (!pending.isEmpty())
			PENDING_FRACTURES.put(player.getUUID(), pending);
		// All Judgement-Cut-End-style lines erupt from the original sphere
		// exactly one second after the final behind-target traversal step.
		SungIlHwanVfxEventMessage.sendExecutionFracture(player,
				traversal.sphereCenter, traversal.radius,
				EXECUTION_FRACTURE_DELAY, traversal.seed);
	}

	private static void abortExecutionTraversal(ServerPlayer player,
			boolean restoreOrigin) {
		ExecutionTraversal traversal = EXECUTION_TRAVERSALS.remove(player.getUUID());
		if (traversal == null)
			return;
		if (restoreOrigin && player.isAlive())
			returnToExecutionOrigin(player, traversal);
		SungIlHwanVfxEventMessage.sendExecutionCancel(player, traversal.seed);
	}

	private static void tickFractures(ServerPlayer player, long now) {
		List<FractureState> pending = PENDING_FRACTURES.get(player.getUUID());
		if (pending == null || pending.isEmpty())
			return;
		for (FractureState fracture : new ArrayList<>(pending)) {
			if (fracture.executeAt > now)
				continue;
			pending.remove(fracture);
			ServerLevel level = player.server.getLevel(fracture.dimension);
			Entity entity = level == null ? null : level.getEntity(fracture.targetId);
			if (entity instanceof LivingEntity target
					&& MageCombatHelper.isValidTarget(player, target)) {
				hurtInternally(player, target, fracture.damage);
				target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,
						18, 1, false, false, true));
			}
		}
		if (pending.isEmpty())
			PENDING_FRACTURES.remove(player.getUUID());
	}

	private static List<LivingEntity> executionTargets(ServerPlayer player, Vec3 focus,
			double radius) {
		// Filter the complete bounded query before applying the pathological
		// density ceiling. An entity counts when its body intersects the sphere,
		// not only when its bounding-box center happens to be inside.
		List<LivingEntity> targets = player.serverLevel().getEntitiesOfClass(
				LivingEntity.class, new AABB(focus, focus).inflate(radius),
				target -> !(target instanceof ArmorStand)
						&& MageCombatHelper.isValidTarget(player, target));
		targets.removeIf(target -> distanceToAabbSqr(focus,
				target.getBoundingBox()) > radius * radius);
		targets.sort(Comparator.comparingDouble(target ->
				distanceToAabbSqr(focus, target.getBoundingBox())));
		if (targets.size() > MAX_EXECUTION_TARGETS)
			return new ArrayList<>(targets.subList(0, MAX_EXECUTION_TARGETS));
		return targets;
	}

	private static double executionRadius(ServerPlayer player, long heldTicks) {
		double maximum = stage(player) == STAGE_TWO
				? EXECUTION_STAGE_TWO_MAX_RADIUS : EXECUTION_MAX_RADIUS;
		double progress = Mth.clamp(heldTicks / (double) MAX_CHARGE_TICKS,
				0.0D, 1.0D);
		return Mth.lerp(progress, EXECUTION_START_RADIUS, maximum);
	}

	private static Vec3 executionCenter(ServerPlayer player) {
		return player.position().add(0.0D, player.getBbHeight() * 0.52D, 0.0D);
	}

	private static boolean returnToExecutionOrigin(ServerPlayer player,
			ExecutionTraversal traversal) {
		ServerLevel destinationLevel = player.server.getLevel(traversal.dimension);
		if (destinationLevel == null)
			return false;
		if (tryExecutionReturn(player, destinationLevel, traversal.originalPosition,
				traversal.originalYaw, traversal.originalPitch))
			return true;
		if (!traversal.safeReturnPosition.equals(traversal.originalPosition)
				&& tryExecutionReturn(player, destinationLevel,
						traversal.safeReturnPosition, traversal.originalYaw,
						traversal.originalPitch))
			return true;

		Vec3 nearby = findSafeExecutionReturnPosition(player, destinationLevel,
				traversal.originalPosition, 8, false);
		if (nearby != null && tryExecutionReturn(player, destinationLevel, nearby,
				traversal.originalYaw, traversal.originalPitch))
			return true;

		// Last checked fallback: search the loaded world-spawn neighborhood. This
		// is preferable to forcing the recorded coordinates or leaving the caster
		// beside the final target when the origin was changed mid-traversal.
		Vec3 sharedSpawn = Vec3.atBottomCenterOf(destinationLevel.getSharedSpawnPos());
		Vec3 emergency = findSafeExecutionReturnPosition(player, destinationLevel,
				sharedSpawn, 12, true);
		return emergency != null && tryExecutionReturn(player, destinationLevel,
				emergency, traversal.originalYaw, traversal.originalPitch);
	}

	private static boolean tryExecutionReturn(ServerPlayer player, ServerLevel level,
			Vec3 destination, float yaw, float pitch) {
		if (!isSafeExecutionReturn(level, player, destination))
			return false;
		player.teleportTo(level, destination.x, destination.y, destination.z,
				yaw, pitch);
		if (player.serverLevel() != level
				|| player.position().distanceToSqr(destination) > 0.25D)
			return false;
		player.setDeltaMovement(Vec3.ZERO);
		player.fallDistance = 0.0F;
		player.setOnGround(true);
		return true;
	}

	private static boolean isSafeExecutionReturn(ServerLevel level,
			ServerPlayer player, Vec3 destination) {
		return IgrisCombatTeleportHelper.isSafeDestination(level, player, destination);
	}

	private static Vec3 findSafeExecutionReturnPosition(ServerPlayer player,
			ServerLevel level, Vec3 center, int maximumRadius,
			boolean includeCenter) {
		if (includeCenter && isSafeExecutionReturn(level, player, center))
			return center;
		int[] verticalOffsets = { 0, 1, -1, 2, -2, 3, -3, 4, -4 };
		for (int radius = 1; radius <= maximumRadius; radius++) {
			for (int x = -radius; x <= radius; x++) {
				for (int z = -radius; z <= radius; z++) {
					if (Math.abs(x) != radius && Math.abs(z) != radius)
						continue;
					for (int y : verticalOffsets) {
						Vec3 candidate = center.add(x, y, z);
						if (isSafeExecutionReturn(level, player, candidate))
							return candidate;
					}
				}
			}
		}
		return null;
	}

	private static List<LivingEntity> nearbyTargets(ServerPlayer player, AABB bounds,
			int maximum) {
		List<LivingEntity> result = player.serverLevel().getEntitiesOfClass(
				LivingEntity.class, bounds,
				target -> !(target instanceof ArmorStand)
						&& MageCombatHelper.isValidTarget(player, target));
		result.sort(Comparator.comparingDouble(player::distanceToSqr));
		if (result.size() > maximum)
			return new ArrayList<>(result.subList(0, maximum));
		return result;
	}

	private static List<LivingEntity> targetsAlongLine(ServerPlayer player,
			Vec3 origin, Vec3 direction, double range, double width, int maximum) {
		Vec3 unitDirection = direction.lengthSqr() < 0.001D
				? new Vec3(0.0D, 0.0D, 1.0D) : direction.normalize();
		Vec3 endpoint = origin.add(unitDirection.scale(range));
		AABB query = new AABB(origin, endpoint).inflate(width);
		// Never pre-truncate the broad phase: filter by the actual segment
		// intersection first, sort by entry distance, and only then cap hits.
		List<LivingEntity> targets = player.serverLevel().getEntitiesOfClass(
				LivingEntity.class, query,
				target -> !(target instanceof ArmorStand)
						&& MageCombatHelper.isValidTarget(player, target));
		targets.removeIf(target -> !player.hasLineOfSight(target)
				|| !segmentIntersectsExpandedAabb(origin, endpoint,
						target.getBoundingBox(), width));
		targets.sort(Comparator.comparingDouble(target -> lineEntryDistanceSqr(
				origin, endpoint, target.getBoundingBox(), width)));
		if (targets.size() > maximum)
			return new ArrayList<>(targets.subList(0, maximum));
		return targets;
	}

	private static boolean segmentIntersectsExpandedAabb(Vec3 origin, Vec3 endpoint,
			AABB bounds, double width) {
		AABB expanded = bounds.inflate(width);
		return expanded.contains(origin) || expanded.clip(origin, endpoint).isPresent();
	}

	private static double lineEntryDistanceSqr(Vec3 origin, Vec3 endpoint,
			AABB bounds, double width) {
		AABB expanded = bounds.inflate(width);
		if (expanded.contains(origin))
			return 0.0D;
		return expanded.clip(origin, endpoint)
				.map(point -> point.distanceToSqr(origin))
				.orElse(Double.MAX_VALUE);
	}

	private static double distanceToAabbSqr(Vec3 point, AABB bounds) {
		double dx = Math.max(Math.max(bounds.minX - point.x, 0.0D),
				point.x - bounds.maxX);
		double dy = Math.max(Math.max(bounds.minY - point.y, 0.0D),
				point.y - bounds.maxY);
		double dz = Math.max(Math.max(bounds.minZ - point.z, 0.0D),
				point.z - bounds.maxZ);
		return dx * dx + dy * dy + dz * dz;
	}

	private static LivingEntity findLookTarget(ServerPlayer player, double range) {
		Vec3 eye = player.getEyePosition();
		Vec3 look = player.getLookAngle().normalize();
		Vec3 end = eye.add(look.scale(range));
		LivingEntity best = null;
		double bestDistance = Double.MAX_VALUE;
		for (LivingEntity target : nearbyTargets(player,
				player.getBoundingBox().expandTowards(look.scale(range)).inflate(1.5D),
				12)) {
			Vec3 center = target.getBoundingBox().getCenter();
			double along = Mth.clamp(center.subtract(eye).dot(look), 0.0D, range);
			double distance = center.distanceToSqr(eye.add(look.scale(along)));
			double allowance = Math.max(0.8D, target.getBbWidth() * 0.75D);
			if (distance <= allowance * allowance && along < bestDistance
					&& player.hasLineOfSight(target)) {
				best = target;
				bestDistance = along;
			}
		}
		return best;
	}

	private static int fear(ServerPlayer owner, LivingEntity target) {
		String suffix = owner.getUUID().toString().replace("-", "");
		CompoundTag data = target.getPersistentData();
		if (data.getLong(FEAR_EXPIRY_PREFIX + suffix) < target.level().getGameTime()) {
			data.remove(FEAR_PREFIX + suffix);
			data.remove(FEAR_EXPIRY_PREFIX + suffix);
			return 0;
		}
		return Mth.clamp(data.getInt(FEAR_PREFIX + suffix), 0, fearCap(target));
	}

	private static void setFear(ServerPlayer owner, LivingEntity target, int amount) {
		String suffix = owner.getUUID().toString().replace("-", "");
		CompoundTag data = target.getPersistentData();
		int bounded = Mth.clamp(amount, 0, fearCap(target));
		if (bounded <= 0) {
			data.remove(FEAR_PREFIX + suffix);
			data.remove(FEAR_EXPIRY_PREFIX + suffix);
			return;
		}
		data.putInt(FEAR_PREFIX + suffix, bounded);
		data.putLong(FEAR_EXPIRY_PREFIX + suffix,
				target.level().getGameTime() + FEAR_LIFETIME);
	}

	private static void addFear(ServerPlayer owner, LivingEntity target, int amount) {
		if (amount <= 0 || !MageCombatHelper.isValidTarget(owner, target))
			return;
		int oldFear = fear(owner, target);
		int updated = Mth.clamp(oldFear + amount, 0, fearCap(target));
		setFear(owner, target, updated);
		int oldTier = fearTier(oldFear);
		int newTier = fearTier(updated);
		applyFearDebuff(target, newTier);
		if (newTier > oldTier)
			SungIlHwanVfxEventMessage.sendFearMark(owner, target, FEAR_LIFETIME,
					visualSeed(owner) ^ target.getId() ^ newTier * 101);
	}

	private static void applyFearDebuff(LivingEntity target, int tier) {
		if (tier <= 0)
			return;
		target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30,
				Math.min(2, tier - 1), false, false, true));
		if (tier >= 2)
			target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 30,
					Math.min(1, tier - 2), false, false, true));
	}

	private static boolean isFearExposed(ServerPlayer caster, LivingEntity target) {
		if (!caster.hasLineOfSight(target))
			return false;
		if (!(target instanceof Player opponent))
			return true;
		if (!opponent.hasLineOfSight(caster))
			return false;
		Vec3 towardCaster = caster.getEyePosition().subtract(opponent.getEyePosition());
		if (towardCaster.lengthSqr() < 0.001D)
			return true;
		// PvP buildup pauses when the opponent breaks visual awareness by looking away.
		return opponent.getLookAngle().normalize().dot(towardCaster.normalize()) >= 0.08D;
	}

	private static void syncPersistentPresentation(ServerPlayer player, CompoundTag state) {
		int currentStage = Mth.clamp(state.getInt(STATE_STAGE), STAGE_NONE, STAGE_TWO);
		if (currentStage == STAGE_TWO
				&& state.getInt(STATE_STAGE_REMAINING) > 0) {
			setSpiritualizationAura(player, STAGE_TWO);
			SungIlHwanVfxEventMessage.sendStage(player, STAGE_TWO,
					state.getInt(STATE_STAGE_REMAINING), visualSeed(player));
		} else if (currentStage == STAGE_ONE) {
			setSpiritualizationAura(player, STAGE_ONE);
			SungIlHwanVfxEventMessage.sendStage(player, STAGE_ONE, 20 * 60,
					visualSeed(player));
		} else {
			clearSpiritualizationAura(player);
			SungIlHwanVfxEventMessage.sendStageEnd(player, visualSeed(player));
		}
		if (state.getInt(STATE_EXHAUSTION_REMAINING) > 0) {
			int remaining = state.getInt(STATE_EXHAUSTION_REMAINING);
			CooldownManager.setFullDuration(player, "mana_refresh", 12);
			SungIlHwanVfxEventMessage.sendExhaustion(player,
					riskSeverity(state.getInt(STATE_RISK)), remaining, visualSeed(player));
		}
	}

	private static void setSpiritualizationAura(ServerPlayer player, int stage) {
		PlayerAuraSystem.setContinuous(player, SPIRITUALIZATION_AURA,
				stage == STAGE_TWO ? 1.65F : 1.25F);
	}

	/**
	 * PlayerAuraSystem has one continuous slot. Never clear it merely because
	 * Sung's saved stage ended: another transformation may have replaced it.
	 */
	private static void clearSpiritualizationAura(ServerPlayer player) {
		if (player != null && SPIRITUALIZATION_AURA.equals(
				player.getPersistentData().getString(PLAYER_AURA_KEY)))
			PlayerAuraSystem.clearContinuous(player);
	}

	private static boolean hurtInternally(ServerPlayer player, LivingEntity target,
			float damage) {
		INTERNAL_DAMAGE.add(player.getUUID());
		try {
			return MageCombatHelper.hurt(player.serverLevel(), player, target,
					Math.max(0.0F, damage));
		} finally {
			INTERNAL_DAMAGE.remove(player.getUUID());
		}
	}

	private static int stage(ServerPlayer player) {
		return Mth.clamp(state(player).getInt(STATE_STAGE), STAGE_NONE, STAGE_TWO);
	}

	private static int stanceAttackIntervalTicks(ServerPlayer player) {
		double attackSpeed = Math.max(0.1D,
				player.getAttributeValue(Attributes.ATTACK_SPEED));
		double vanillaCooldownTicks = 20.0D / attackSpeed;
		// Accept normal clicks near the vanilla cooled-swing cadence while
		// retaining an absolute two-tick floor against packet spam.
		return Mth.clamp((int) Math.ceil(vanillaCooldownTicks * 0.8D), 2, 10);
	}

	private static boolean replaceOrAlreadyReplacedAssassinAttack(
			ServerPlayer player) {
		if (performAssassinLineCut(player))
			return true;
		Long lastAttack = LAST_STANCE_ATTACK.get(player.getUUID());
		// Better Combat may submit several targets for one attack frame, and the
		// target-free request may arrive immediately before/after those hits.
		// Treat only that narrow window as the same authored cut. A genuinely
		// separate attack that fails validation is left intact instead of becoming
		// a zero-damage click.
		return lastAttack != null
				&& player.level().getGameTime() - lastAttack <= 1L;
	}

	private static boolean hasDagger(ServerPlayer player) {
		return isDagger(player.getMainHandItem());
	}

	private static boolean isDagger(ItemStack stack) {
		return stack != null && !stack.isEmpty() && stack.is(DAGGERS);
	}

	private static double rulerPower(ServerPlayer player) {
		return 20.0D
				+ TemporaryStatBonusManager.effectiveStrength(player) * 1.25D
				+ TemporaryStatBonusManager.effectiveAgility(player) * 1.10D
				+ TemporaryStatBonusManager.effectiveIntelligence(player) * 0.35D;
	}

	private static double attributeValueOrZero(LivingEntity entity,
			Holder<Attribute> attribute) {
		AttributeInstance instance = entity.getAttribute(attribute);
		return instance == null ? 0.0D : instance.getValue();
	}

	private static double targetPower(LivingEntity target) {
		return 10.0D + target.getMaxHealth() * 1.25D
				+ Math.max(0.0D,
						attributeValueOrZero(target, Attributes.ARMOR)) * 3.0D
				+ Math.max(0.0D,
						attributeValueOrZero(target, Attributes.ATTACK_DAMAGE)) * 6.0D;
	}

	private static boolean isBossLike(LivingEntity target) {
		return target.getMaxHealth() >= 120.0F || target.getPersistentData().getBoolean("Boss")
				|| target.getType().toString().toLowerCase().contains("boss");
	}

	private static int riskSeverity(int risk) {
		return Mth.clamp((int) Math.round(clampRisk(risk) * 2.55D), 0, 255);
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
		player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
				.ifPresent(capability -> {
					capability.MP = Math.max(0.0D, capability.MP - Math.max(0, amount));
					capability.syncPlayerVariables(player);
				});
		CooldownManager.set(player, "mana_refresh", 35);
		return true;
	}

	private static CompoundTag state(Player player) {
		CompoundTag outer = player.getPersistentData();
		CompoundTag persisted = outer.contains(Player.PERSISTED_NBT_TAG, Tag.TAG_COMPOUND)
				? outer.getCompound(Player.PERSISTED_NBT_TAG) : new CompoundTag();
		CompoundTag state = persisted.contains(STATE_ROOT, Tag.TAG_COMPOUND)
				? persisted.getCompound(STATE_ROOT) : new CompoundTag();
		persisted.put(STATE_ROOT, state);
		outer.put(Player.PERSISTED_NBT_TAG, persisted);
		return state;
	}

	private static SololevelingModVariables.PlayerVariables variables(Entity entity) {
		return entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
				.orElse(new SololevelingModVariables.PlayerVariables());
	}

	private static int visualSeed(ServerPlayer player) {
		long mixed = player.getUUID().getMostSignificantBits()
				^ player.getUUID().getLeastSignificantBits()
				^ player.level().getGameTime() * 0x9E3779B97F4A7C15L;
		return (int) (mixed ^ mixed >>> 32);
	}

	private record ChargeState(long startedAt, int seed) {
	}

	private record ExecutionTarget(UUID targetId, float damage, Vec3 focus) {
	}

	private static final class ExecutionTraversal {
		private final ResourceKey<Level> dimension;
		private final Vec3 originalPosition;
		private final Vec3 safeReturnPosition;
		private final float originalYaw;
		private final float originalPitch;
		private final Vec3 sphereCenter;
		private final double radius;
		private final int tier;
		private final int seed;
		private final boolean overloaded;
		private final List<ExecutionTarget> targets;
		private int nextTarget;
		private long nextStepAt;

		private ExecutionTraversal(ResourceKey<Level> dimension,
				Vec3 originalPosition, Vec3 safeReturnPosition, float originalYaw,
				float originalPitch, Vec3 sphereCenter, double radius, int tier,
				int seed, boolean overloaded, List<ExecutionTarget> targets,
				long nextStepAt) {
			this.dimension = dimension;
			this.originalPosition = originalPosition;
			this.safeReturnPosition = safeReturnPosition;
			this.originalYaw = originalYaw;
			this.originalPitch = originalPitch;
			this.sphereCenter = sphereCenter;
			this.radius = radius;
			this.tier = tier;
			this.seed = seed;
			this.overloaded = overloaded;
			this.targets = List.copyOf(targets);
			this.nextStepAt = nextStepAt;
		}
	}

	private record FractureState(UUID targetId, long executeAt, float damage,
			Vec3 focus, ResourceKey<Level> dimension) {
	}
}
