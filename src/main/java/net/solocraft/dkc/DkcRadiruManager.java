package net.solocraft.dkc;

import net.solocraft.SololevelingMod;
import net.solocraft.dkc.event.EsilPermitClaimEvent;
import net.solocraft.entity.DemonEntity;
import net.solocraft.entity.DemonKnightEntity;
import net.solocraft.entity.EsilRadiruEntity;
import net.solocraft.entity.EsilRadiruEntity.EncounterState;
import net.solocraft.init.SololevelingModEntities;
import net.solocraft.init.SololevelingModItems;
import net.solocraft.network.RadiruMercyChoiceStateMessage;
import net.solocraft.network.SololevelingModVariables;
import net.solocraft.procedures.XPGainProcedure;
import net.solocraft.procedures.DKCDemonSpawnerProcedure;
import net.solocraft.util.ShadowMonarchManager;
import net.solocraft.util.SystemNotifications;
import net.solocraft.util.VesselProgressionManager;

import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Persistent Floor-15 branch: surrender, Esil's permit, sanctuary protection,
 * the post-conquest Radiru route, and six no-AI damage-test targets.
 */
@Mod.EventBusSubscriber(modid = SololevelingMod.MODID)
public final class DkcRadiruManager {
	public static final int FLOOR = 15;
	public static final String RESIDENT_TAG = "radiru_resident";
	public static final String TRAINING_DUMMY_TAG = "radiru_training_dummy";
	public static final String PERMIT_FLOOR_TAG = "DkcPermitFloor";
	public static final String PERMIT_OWNER_TAG = "DkcPermitOwner";

	private static final String OWNER_TAG = "radiru_owner";
	private static final String SANCTUARY_TAG = "radiru_sanctuary";
	private static final String STATION_TAG = "radiru_training_station";
	private static final String SURRENDERED_TAG = "radiru_floor_15_surrendered";
	private static final String GATE_OPEN_TAG = "radiru_floor_15_gate_open";
	private static final String RESIDENTS_SPAWNED_TAG = "radiru_floor_15_residents_spawned";
	private static final String TOWER_SEALED_TAG = "radiru_floor_15_tower_sealed";
	private static final String STATE_SCHEMA_TAG = "radiru_state_schema";
	private static final String NOTICE_COOLDOWN = "radiru_pact_notice_cooldown";
	private static final String DAMAGE_WINDOW_START = "radiru_dummy_damage_window_start";
	private static final String DAMAGE_WINDOW_TOTAL = "radiru_dummy_damage_window_total";
	private static final String DAMAGE_WINDOW_HITS = "radiru_dummy_damage_window_hits";
	private static final String DAMAGE_DISPLAY_AFTER = "radiru_dummy_display_after";
	private static final String PERMIT_REISSUE_AFTER = "radiru_permit_reissue_after";
	private static final int STATE_SCHEMA = 1;
	private static final int FLOOR_XP = 1_500;
	private static final int EXECUTION_BONUS_XP = 2_500;
	private static final long ENSURE_INTERVAL = 100L;
	private static final int MERCY_CHOICE_TICKS = 600;
	private static final Map<UUID, PendingMercyChoice> PENDING_MERCY_CHOICES = new HashMap<>();

	private static final double[] DUMMY_ARMOR = {0.0D, 8.0D, 15.0D, 22.0D, 28.0D, 30.0D};
	private static final double[] DUMMY_TOUGHNESS = {0.0D, 0.0D, 4.0D, 8.0D, 12.0D, 20.0D};
	private static final String[] DUMMY_NAMES = {
			"Unarmored Demon", "Demon Soldier", "Armored Demon",
			"Demon Knight", "Elite Demon Knight", "Royal Guard"
	};

	private DkcRadiruManager() {
	}

	/** Cancels an unresolved Esil decision and closes its client prompt. */
	public static void resetPlayerState(ServerPlayer player) {
		if (player == null)
			return;
		PENDING_MERCY_CHOICES.remove(player.getUUID());
		sendMercyChoiceState(player, false);
	}

	/** Called instead of ordinary Floor-15 auto-reward when the field wave ends. */
	public static void onDefendersOverpowered(ServerLevel level, ServerPlayer player) {
		if (!validOwnerOnFloor(level, player) || variables(player).dkc_cleared >= FLOOR)
			return;
		CompoundTag data = player.getPersistentData();
		data.putBoolean(SURRENDERED_TAG, true);
		if (DkcFloorBuilder.openRadiruGate(level, player))
			data.putBoolean(GATE_OPEN_TAG, true);
		ensureEsil(level, player);
		spawnInitialResidents(level, player);
		notify(player, 0xFFD84CFF, "HOUSE RADIRU SURRENDERS",
				"The castle gate is open. Esil waits inside with an Entry Permit.", 150);
	}

	/** Low-frequency recovery only while the owning player occupies Floor 15. */
	public static void tick(ServerPlayer player) {
		if (player == null || !(player.level() instanceof ServerLevel level)
				|| !validOwnerOnFloor(level, player))
			return;
		SololevelingModVariables.PlayerVariables vars = migrateLegacyState(player);
		CompoundTag data = player.getPersistentData();
		if (!data.getBoolean(SURRENDERED_TAG) && !vars.radiru_pact && !vars.radiru_slaughtered)
			return;
		if (level.getGameTime() % ENSURE_INTERVAL != Math.floorMod(player.getId(), ENSURE_INTERVAL))
			return;
		reconcileLoadedCastle(level, player, vars, false);
	}

	/** Rehydrates physical state immediately after a player arrives on Floor 15. */
	public static void onFloorEntered(ServerPlayer player) {
		if (player == null || !(player.level() instanceof ServerLevel level)
				|| !validOwnerOnFloor(level, player) || player.server == null
				|| !DkcRunSavedData.get(player.server).isGenerated(player, FLOOR))
			return;
		SololevelingModVariables.PlayerVariables vars = migrateLegacyState(player);
		if (player.getPersistentData().getBoolean(SURRENDERED_TAG)
				|| vars.radiru_pact || vars.radiru_slaughtered)
			reconcileLoadedCastle(level, player, vars, true);
		else
			purgeLoadedRadiruActors(level, player);
	}

	@SubscribeEvent
	public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
		if (!(event.getEntity() instanceof ServerPlayer player) || player.server == null)
			return;
		SololevelingMod.queueServerWork(player.server, 1, () -> {
			if (!player.hasDisconnected())
				onFloorEntered(player);
		});
	}

	@SubscribeEvent
	public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
		if (event.getEntity() instanceof ServerPlayer player)
			PENDING_MERCY_CHOICES.remove(player.getUUID());
	}

	@SubscribeEvent
	public static void onServerStopped(ServerStoppedEvent event) {
		PENDING_MERCY_CHOICES.entrySet().removeIf(entry -> entry.getValue().server() == event.getServer());
	}

	private static SololevelingModVariables.PlayerVariables migrateLegacyState(ServerPlayer player) {
		SololevelingModVariables.PlayerVariables vars = variables(player);
		CompoundTag data = player.getPersistentData();
		boolean firstMigration = data.getInt(STATE_SCHEMA_TAG) < STATE_SCHEMA;
		boolean legacyPact = firstMigration && vars.dkc_cleared >= FLOOR
				&& !vars.radiru_pact && !vars.radiru_slaughtered;
		boolean unlockSideQuest = (vars.radiru_pact || legacyPact)
				&& vars.dkc_cleared >= DkcFloorRegistry.LAST_FLOOR && !vars.radiru_side_quest_unlocked;
		if (legacyPact || unlockSideQuest) {
			player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
				if (legacyPact)
					capability.radiru_pact = true;
				if (unlockSideQuest)
					capability.radiru_side_quest_unlocked = true;
				capability.syncPlayerVariables(player);
			});
			vars = variables(player);
		}
		if (vars.radiru_pact)
			data.putBoolean(SURRENDERED_TAG, true);
		if (firstMigration)
			data.putInt(STATE_SCHEMA_TAG, STATE_SCHEMA);
		return vars;
	}

	private static void reconcileLoadedCastle(ServerLevel level, ServerPlayer player,
			SololevelingModVariables.PlayerVariables vars, boolean forcePhysicalState) {
		CompoundTag data = player.getPersistentData();
		boolean gateShouldOpen = data.getBoolean(SURRENDERED_TAG) || vars.dkc_cleared >= FLOOR;
		if (gateShouldOpen && (forcePhysicalState || !data.getBoolean(GATE_OPEN_TAG))
				&& DkcFloorBuilder.openRadiruGate(level, player))
			data.putBoolean(GATE_OPEN_TAG, true);
		boolean towerShouldSeal = vars.dkc_cleared >= DkcFloorRegistry.LAST_FLOOR && !vars.radiru_slaughtered;
		if (towerShouldSeal && (forcePhysicalState || !data.getBoolean(TOWER_SEALED_TAG))
				&& DkcFloorBuilder.sealRadiruTower(level, player))
			data.putBoolean(TOWER_SEALED_TAG, true);
		if (!vars.radiru_slaughtered && (data.getBoolean(SURRENDERED_TAG) || vars.radiru_pact)) {
			ensureEsil(level, player);
			ensureResidents(level, player);
		}
		if (vars.radiru_pact)
			ensureTrainingDummies(level, player);
	}

	/** Converts the final DKC victory into the permanent Radiru side-quest route. */
	public static void onCastleConquered(ServerPlayer player) {
		if (player == null)
			return;
		SololevelingModVariables.PlayerVariables vars = variables(player);
		if (!vars.radiru_pact || vars.radiru_slaughtered || vars.radiru_side_quest_unlocked)
			return;
		player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
			capability.radiru_side_quest_unlocked = true;
			capability.syncPlayerVariables(player);
		});
		notify(player, 0xFFD84CFF, "SECRET QUEST UNLOCKED",
				"A House Beyond the Gate - Radiru Castle is now bound to your System.", 180);
	}

	/** Keeps the creative floor rewind tool from manufacturing impossible outcomes. */
	public static void normalizeDebugProgress(ServerPlayer player, int cleared) {
		if (player == null)
			return;
		closeMercyChoice(player);
		player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
			if (cleared < FLOOR) {
				capability.radiru_pact = false;
				capability.radiru_slaughtered = false;
				capability.radiru_side_quest_unlocked = false;
			} else {
				if (!capability.radiru_pact && !capability.radiru_slaughtered)
					capability.radiru_pact = true;
				capability.radiru_side_quest_unlocked = cleared >= DkcFloorRegistry.LAST_FLOOR
						&& capability.radiru_pact && !capability.radiru_slaughtered;
			}
			capability.syncPlayerVariables(player);
		});
		CompoundTag data = player.getPersistentData();
		data.putInt(STATE_SCHEMA_TAG, STATE_SCHEMA);
		if (cleared < FLOOR) {
			data.remove(SURRENDERED_TAG);
			data.remove(GATE_OPEN_TAG);
			data.remove(RESIDENTS_SPAWNED_TAG);
			data.remove(TOWER_SEALED_TAG);
			String prefix = "dkc_floor_15";
			data.remove(prefix + "_spawned");
			data.remove(prefix + "_initial_spawned");
			data.remove(prefix + "_spawning");
			data.remove(prefix + "_complete");
			data.remove(prefix + "_killed");
			data.remove(prefix + "_required");
			data.remove(prefix + "_demon_count");
			data.remove(prefix + "_knight_count");
			data.remove(prefix + "_miniboss_spawned");
			data.remove(prefix + "_spawn_retry_after");
			DKCDemonSpawnerProcedure.invalidateAttempt(player, FLOOR);
			if (player.server != null) {
				ServerLevel dkc = player.server.getLevel(DkcFloorRegistry.SHARED_DIMENSION);
				if (dkc != null)
					DKCDemonSpawnerProcedure.discardOwnedWave(dkc, player, FLOOR);
			}
		}
		if (cleared < DkcFloorRegistry.LAST_FLOOR)
			data.remove(TOWER_SEALED_TAG);
	}

	/** Applies creative rewinds immediately when the tester is standing on Floor 15. */
	public static void reconcileDebugProgress(ServerPlayer player, int cleared) {
		if (player == null || !(player.level() instanceof ServerLevel level)
				|| !validOwnerOnFloor(level, player))
			return;
		if (cleared < FLOOR)
			purgeLoadedRadiruActors(level, player);
		DkcFloorBuilder.prepareFloor(player, FLOOR);
		if (cleared >= FLOOR)
			onFloorEntered(player);
	}

	/** Restores a clean closed-gate encounter when the owner dies before choosing. */
	public static void resetFailedEncounter(ServerLevel level, ServerPlayer player) {
		if (!validOwnerOnFloor(level, player) || variables(player).dkc_cleared >= FLOOR)
			return;
		closeMercyChoice(player);
		CompoundTag data = player.getPersistentData();
		data.remove(SURRENDERED_TAG);
		data.remove(GATE_OPEN_TAG);
		data.remove(RESIDENTS_SPAWNED_TAG);
		DkcFloorBuilder.closeRadiruGate(level, player);
		purgeLoadedRadiruActors(level, player);
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void onPermitClaim(EsilPermitClaimEvent event) {
		EsilRadiruEntity esil = event.esil();
		ServerPlayer player = event.player();
		if (!(esil.level() instanceof ServerLevel level) || !validMercyInteraction(level, player, esil)) {
			event.deny();
			return;
		}
		SololevelingModVariables.PlayerVariables vars = variables(player);
		if (vars.radiru_pact && !vars.radiru_slaughtered && vars.dkc_cleared >= FLOOR) {
			// The post-conquest destination has a sealed tower and no longer needs
			// floor permits; every click is a conversation from this point onward.
			if (vars.radiru_side_quest_unlocked || vars.dkc_cleared >= DkcFloorRegistry.LAST_FLOOR)
				return;
			DkcRunSavedData runs = DkcRunSavedData.get(player.server);
			long now = level.getGameTime();
			if (runs.isTransitionArmed(player, FLOOR)) {
				event.deny();
				return;
			}
			// A still-accounted-for permit means this is an ordinary conversation,
			// so leave the event as PASS and let Esil's entity handle the dialogue.
			if (hasEntryPermit(player) || hasNearbyFloorPermit(level, player))
				return;
			if (now < player.getPersistentData().getLong(PERMIT_REISSUE_AFTER)) {
				player.displayClientMessage(Component.literal("\u00A75Your Floor 15 permit is still accounted for."), true);
				event.deny();
				return;
			}
			give(player, createFloorPermit(player));
			player.getPersistentData().putLong(PERMIT_REISSUE_AFTER, now + 200L);
			event.grantPermit();
			notify(player, 0xFFB94CFF, "RADIRU PERMIT RESTORED",
					"Esil replaced the lost Floor 15 permit. It cannot open any other floor.", 120);
			return;
		}
		if (vars.radiru_slaughtered || vars.radiru_pact || vars.dkc_cleared >= FLOOR) {
			event.deny();
			return;
		}
		if (!isExecutionReady(player)) {
			event.deny();
			return;
		}

		openMercyChoice(level, player, esil);
		event.deny();
		event.setCanceled(true);
	}

	/**
	 * Resolves only a recent prompt issued for this player and this exact Esil.
	 * Every encounter invariant is checked again because the client sends only
	 * its requested choice and is never trusted to identify or mutate the run.
	 */
	public static void resolveMercyChoice(ServerPlayer player, boolean spare) {
		if (player == null)
			return;
		PendingMercyChoice pending = PENDING_MERCY_CHOICES.remove(player.getUUID());
		sendMercyChoiceState(player, false);
		if (!spare || pending == null || player.server != pending.server()
				|| !(player.level() instanceof ServerLevel level)
				|| level.getGameTime() > pending.expiresAt())
			return;
		Entity entity = level.getEntity(pending.esilId());
		if (!(entity instanceof EsilRadiruEntity esil)
				|| !validMercyInteraction(level, player, esil))
			return;
		SololevelingModVariables.PlayerVariables vars = variables(player);
		if (vars.radiru_pact || vars.radiru_slaughtered || vars.dkc_cleared >= FLOOR
				|| !isExecutionReady(player))
			return;
		finalizePact(level, player, esil);
	}

	private static void openMercyChoice(ServerLevel level, ServerPlayer player, EsilRadiruEntity esil) {
		long expiresAt = level.getGameTime() + MERCY_CHOICE_TICKS;
		PENDING_MERCY_CHOICES.put(player.getUUID(),
				new PendingMercyChoice(player.server, esil.getUUID(), expiresAt));
		esil.getLookControl().setLookAt(player, 30.0F, 30.0F);
		player.displayClientMessage(Component.translatable("dialogue.sololeveling.esil.speech",
				esil.getDisplayName(), Component.translatable("dialogue.sololeveling.esil.mercy.plea")), false);
		sendMercyChoiceState(player, true);
		MinecraftServer server = player.server;
		UUID playerId = player.getUUID();
		SololevelingMod.queueServerWork(server, MERCY_CHOICE_TICKS,
				() -> expireMercyChoice(server, playerId, expiresAt));
	}

	private static void expireMercyChoice(MinecraftServer server, UUID playerId, long expiresAt) {
		PendingMercyChoice pending = PENDING_MERCY_CHOICES.get(playerId);
		if (pending == null || pending.server() != server || pending.expiresAt() != expiresAt)
			return;
		PENDING_MERCY_CHOICES.remove(playerId);
		ServerPlayer player = server.getPlayerList().getPlayer(playerId);
		if (player != null)
			sendMercyChoiceState(player, false);
	}

	private static void finalizePact(ServerLevel level, ServerPlayer player, EsilRadiruEntity esil) {
		// Persist the branch before delivering rewards. A duplicate packet in the
		// same server tick therefore cannot duplicate the permit or XP.
		player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
			capability.radiru_pact = true;
			capability.radiru_slaughtered = false;
			capability.dkc_cleared = Math.max(capability.dkc_cleared, FLOOR);
			capability.syncPlayerVariables(player);
		});
		VesselProgressionManager.reconcileEntitlements(player);
		player.getPersistentData().putInt(STATE_SCHEMA_TAG, STATE_SCHEMA);
		player.getPersistentData().putLong(PERMIT_REISSUE_AFTER, level.getGameTime() + 200L);
		esil.getPersistentData().putBoolean(SANCTUARY_TAG, true);
		esil.markPermitClaimed();
		give(player, createFloorPermit(player));
		XPGainProcedure.awardBaseXp(level, player, FLOOR_XP);
		ensureResidents(level, player);
		ensureTrainingDummies(level, player);
		notify(player, 0xFFB94CFF, "PACT OF HOUSE RADIRU",
				"Entry Permit received. Radiru blood may no longer be spilled here.", 150);
	}

	/** Sanctuary residents cannot be harmed; training targets remain damageable. */
	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void protectSanctuary(LivingAttackEvent event) {
		Entity target = event.getEntity();
		CompoundTag tag = target.getPersistentData();
		if (tag.getBoolean(TRAINING_DUMMY_TAG)) {
			if (creditedPlayer(event.getSource()) == null)
				event.setCanceled(true);
			return;
		}
		if (!tag.getBoolean(SANCTUARY_TAG) && !isPactProtected(target))
			return;
		event.setCanceled(true);
		ServerPlayer attacker = creditedPlayer(event.getSource());
		if (attacker == null)
			return;
		long now = attacker.level().getGameTime();
		if (now >= attacker.getPersistentData().getLong(NOTICE_COOLDOWN)) {
			attacker.getPersistentData().putLong(NOTICE_COOLDOWN, now + 40L);
			attacker.displayClientMessage(Component.literal("\u00A75The pact forbids bloodshed inside Radiru Castle."), true);
		}
	}

	/** Shows final post-mitigation output and resets each dummy before every hit. */
	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void meterTrainingDamage(LivingDamageEvent event) {
		if (!event.getEntity().getPersistentData().getBoolean(TRAINING_DUMMY_TAG))
			return;
		ServerPlayer player = creditedPlayer(event.getSource());
		if (player == null) {
			event.setCanceled(true);
			return;
		}
		float damage = Math.max(0.0F, event.getAmount());
		event.getEntity().setHealth(event.getEntity().getMaxHealth());
		CompoundTag data = player.getPersistentData();
		long now = player.level().getGameTime();
		long start = data.getLong(DAMAGE_WINDOW_START);
		if (start <= 0L || now - start > 100L) {
			start = now;
			data.putLong(DAMAGE_WINDOW_START, start);
			data.putDouble(DAMAGE_WINDOW_TOTAL, 0.0D);
			data.putInt(DAMAGE_WINDOW_HITS, 0);
		}
		double total = data.getDouble(DAMAGE_WINDOW_TOTAL) + damage;
		int hits = data.getInt(DAMAGE_WINDOW_HITS) + 1;
		data.putDouble(DAMAGE_WINDOW_TOTAL, total);
		data.putInt(DAMAGE_WINDOW_HITS, hits);
		if (now < data.getLong(DAMAGE_DISPLAY_AFTER))
			return;
		data.putLong(DAMAGE_DISPLAY_AFTER, now + 4L);
		double seconds = Math.max(0.05D, (now - start + 1L) / 20.0D);
		double dps = total / seconds;
		double armor = attributeValue(event.getEntity().getAttribute(Attributes.ARMOR));
		player.displayClientMessage(Component.literal(String.format(Locale.ROOT,
				"\u00A7dTraining Hit: \u00A7f%.1f  \u00A75Window DPS: \u00A7f%.1f  \u00A78Armor: %.0f",
				damage, dps, armor)), true);
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void preserveTrainingTargetsAndResolveEsil(LivingDeathEvent event) {
		Entity target = event.getEntity();
		if (target.getPersistentData().getBoolean(TRAINING_DUMMY_TAG)) {
			restoreFromDeath(event);
			return;
		}
		if (isPactProtected(target)
				|| target instanceof EsilRadiruEntity protectedEsil && protectedEsil.isPermitClaimed()) {
			restoreFromDeath(event);
			return;
		}

		if (target.getPersistentData().getBoolean(RESIDENT_TAG)) {
			ServerPlayer owner = ownerPlayer(target);
			ServerPlayer killer = creditedPlayer(event.getSource());
			if (owner == null || killer == null || !owner.getUUID().equals(killer.getUUID())
					|| !(target.level() instanceof ServerLevel level) || !validOwnerOnFloor(level, owner)) {
				restoreFromDeath(event);
				return;
			}
			SololevelingModVariables.PlayerVariables vars = variables(owner);
			if (vars.radiru_slaughtered)
				return;
			if (!isExecutionReady(owner)) {
				event.setCanceled(true);
				target.discard();
				return;
			}
			if (!completeExecutionRoute(level, owner))
				restoreFromDeath(event);
			return;
		}

		if (!(target instanceof EsilRadiruEntity esil))
			return;
		UUID owner = esil.getEncounterOwner().orElse(null);
		ServerPlayer killer = creditedPlayer(event.getSource());
		if (owner == null || killer == null || !owner.equals(killer.getUUID())
				|| !(esil.level() instanceof ServerLevel level) || !validOwnerOnFloor(level, killer)) {
			restoreFromDeath(event);
			return;
		}
		if (variables(killer).radiru_slaughtered)
			return;
		if (!isExecutionReady(killer)) {
			event.setCanceled(true);
			esil.discard();
			return;
		}
		if (!completeExecutionRoute(level, killer))
			restoreFromDeath(event);
	}

	private static boolean completeExecutionRoute(ServerLevel level, ServerPlayer player) {
		SololevelingModVariables.PlayerVariables vars = variables(player);
		if (vars.radiru_pact || vars.radiru_slaughtered || vars.dkc_cleared >= FLOOR)
			return false;
		closeMercyChoice(player);
		player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
			capability.radiru_slaughtered = true;
			capability.radiru_pact = false;
			capability.dkc_cleared = Math.max(capability.dkc_cleared, FLOOR);
			capability.syncPlayerVariables(player);
		});
		VesselProgressionManager.reconcileEntitlements(player);
		player.getPersistentData().putInt(STATE_SCHEMA_TAG, STATE_SCHEMA);
		give(player, createFloorPermit(player));
		give(player, new ItemStack(SololevelingModItems.RUNESTONE_COLD_BLOOD.get()));
		XPGainProcedure.awardBaseXp(level, player, FLOOR_XP + EXECUTION_BONUS_XP);
		notify(player, 0xFFFF3434, "HOUSE RADIRU HAS FALLEN",
				"Entry Permit, Cold Blood, and execution XP acquired.", 160);
		return true;
	}

	private static EsilRadiruEntity ensureEsil(ServerLevel level, ServerPlayer player) {
		AABB castle = DkcFloorBuilder.radiruCastleBounds(player);
		AABB search = castle.inflate(96.0D);
		List<EsilRadiruEntity> found = level.getEntitiesOfClass(EsilRadiruEntity.class, search,
				entity -> entity.isOwnedBy(player));
		found.sort(Comparator.comparingInt(Entity::getId));
		EsilRadiruEntity esil = found.isEmpty() ? null : found.get(0);
		for (int i = 1; i < found.size(); i++)
			found.get(i).discard();
		BlockPos pos = DkcFloorBuilder.radiruEsilPosition(player);
		if (esil == null) {
			if (!level.isPositionEntityTicking(pos))
				return null;
			esil = SololevelingModEntities.ESIL_RADIRU.get().create(level);
			if (esil == null)
				return null;
			esil.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, 180.0F, 0.0F);
			esil.setEncounterOwner(player.getUUID());
			esil.setCustomName(Component.literal("Esil Radiru").withStyle(ChatFormatting.LIGHT_PURPLE));
			esil.setCustomNameVisible(true);
			if (!level.addFreshEntity(esil)) {
				esil.discard();
				return null;
			}
		}
		if (level.isPositionEntityTicking(pos)
				&& (!castle.intersects(esil.getBoundingBox())
						|| esil.distanceToSqr(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D) > 36.0D
						|| !isOpenActorAnchor(level, esil.blockPosition()))) {
			esil.teleportTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
			esil.setDeltaMovement(0.0D, 0.0D, 0.0D);
		}
		boolean sanctuary = variables(player).radiru_pact;
		esil.setEncounterOwner(player.getUUID());
		esil.setPermitClaimed(sanctuary);
		esil.setEncounterState(sanctuary ? EncounterState.SANCTUARY : EncounterState.SURRENDERED);
		esil.getPersistentData().putBoolean(SANCTUARY_TAG, sanctuary);
		return esil;
	}

	private static void spawnInitialResidents(ServerLevel level, ServerPlayer player) {
		CompoundTag data = player.getPersistentData();
		data.putBoolean(RESIDENTS_SPAWNED_TAG, true);
		ensureResidents(level, player);
	}

	private static void ensureResidents(ServerLevel level, ServerPlayer player) {
		List<BlockPos> anchors = DkcFloorBuilder.radiruResidentPositions(player);
		AABB bounds = DkcFloorBuilder.radiruCastleBounds(player).inflate(64.0D);
		List<Mob> existing = level.getEntitiesOfClass(Mob.class, bounds,
				mob -> mob.getPersistentData().getBoolean(RESIDENT_TAG)
						&& player.getStringUUID().equals(mob.getPersistentData().getString(OWNER_TAG)));
		for (int station = 0; station < anchors.size(); station++) {
			final int index = station;
			List<Mob> stationResidents = existing.stream()
					.filter(mob -> mob.getPersistentData().getInt(STATION_TAG) == index)
					.sorted(Comparator.comparingInt(Entity::getId)).toList();
			Mob resident = stationResidents.isEmpty() ? null : stationResidents.get(0);
			for (int duplicate = 1; duplicate < stationResidents.size(); duplicate++)
				stationResidents.get(duplicate).discard();
			BlockPos anchor = anchors.get(station);
			if (resident == null) {
				if (!level.isPositionEntityTicking(anchor))
					continue;
				resident = station % 2 == 0 ? SololevelingModEntities.DEMON_KNIGHT.get().create(level)
						: SololevelingModEntities.DEMON.get().create(level);
				if (resident == null)
					continue;
				resident.moveTo(anchor.getX() + 0.5D, anchor.getY(), anchor.getZ() + 0.5D, 180.0F, 0.0F);
				if (resident instanceof DemonEntity demon)
					demon.randomizeAppearance();
				else if (resident instanceof DemonKnightEntity knight)
					knight.randomizeAppearance();
				resident.setNoAi(true);
				resident.setPersistenceRequired();
				resident.setCustomName(Component.literal(station < 2 ? "Radiru Royal Guard" : "Radiru Resident")
						.withStyle(ChatFormatting.DARK_PURPLE));
				resident.setCustomNameVisible(false);
				CompoundTag tag = resident.getPersistentData();
				tag.putBoolean(RESIDENT_TAG, true);
				tag.putBoolean(SANCTUARY_TAG, variables(player).radiru_pact);
				tag.putString(OWNER_TAG, player.getStringUUID());
				tag.putInt(STATION_TAG, station);
				if (!level.addFreshEntity(resident))
					resident.discard();
			} else {
				boolean sanctuary = variables(player).radiru_pact;
				resident.getPersistentData().putBoolean(SANCTUARY_TAG, sanctuary);
				if (sanctuary) {
					resident.setHealth(resident.getMaxHealth());
				}
				if (level.isPositionEntityTicking(anchor)
						&& resident.distanceToSqr(anchor.getX() + 0.5D, anchor.getY(), anchor.getZ() + 0.5D) > 2.25D)
					resident.teleportTo(anchor.getX() + 0.5D, anchor.getY(), anchor.getZ() + 0.5D);
			}
			resident.setDeltaMovement(0.0D, 0.0D, 0.0D);
		}
	}

	private static void ensureTrainingDummies(ServerLevel level, ServerPlayer player) {
		List<BlockPos> anchors = DkcFloorBuilder.radiruTrainingDummyPositions(player);
		AABB bounds = DkcFloorBuilder.radiruCastleBounds(player).inflate(64.0D);
		List<Mob> existing = level.getEntitiesOfClass(Mob.class, bounds,
				mob -> mob.getPersistentData().getBoolean(TRAINING_DUMMY_TAG)
						&& player.getStringUUID().equals(mob.getPersistentData().getString(OWNER_TAG)));
		for (int station = 0; station < anchors.size(); station++) {
			final int index = station;
			List<Mob> stationDummies = existing.stream()
					.filter(mob -> mob.getPersistentData().getInt(STATION_TAG) == index)
					.sorted(Comparator.comparingInt(Entity::getId)).toList();
			Mob dummy = stationDummies.isEmpty() ? null : stationDummies.get(0);
			for (int duplicate = 1; duplicate < stationDummies.size(); duplicate++)
				stationDummies.get(duplicate).discard();
			BlockPos anchor = anchors.get(station);
			if (dummy == null) {
				if (!level.isPositionEntityTicking(anchor))
					continue;
				dummy = station % 2 == 0 ? SololevelingModEntities.DEMON.get().create(level)
						: SololevelingModEntities.DEMON_KNIGHT.get().create(level);
				if (dummy == null)
					continue;
				dummy.moveTo(anchor.getX() + 0.5D, anchor.getY(), anchor.getZ() + 0.5D, 180.0F, 0.0F);
				try {
					dummy.finalizeSpawn(level, level.getCurrentDifficultyAt(anchor), MobSpawnType.EVENT, null, null);
				} catch (RuntimeException ignored) {
					// Appearance randomization is optional for a training target.
				}
				dummy.setNoAi(true);
				dummy.setPersistenceRequired();
				CompoundTag tag = dummy.getPersistentData();
				tag.putBoolean(TRAINING_DUMMY_TAG, true);
				tag.putString(OWNER_TAG, player.getStringUUID());
				tag.putInt(STATION_TAG, station);
				configureDummy(dummy, station);
				if (!level.addFreshEntity(dummy)) {
					dummy.discard();
					continue;
				}
			} else {
				configureDummy(dummy, station);
				if (level.isPositionEntityTicking(anchor)
						&& dummy.distanceToSqr(anchor.getX() + 0.5D, anchor.getY(), anchor.getZ() + 0.5D) > 2.25D)
					dummy.teleportTo(anchor.getX() + 0.5D, anchor.getY(), anchor.getZ() + 0.5D);
			}
			dummy.setDeltaMovement(0.0D, 0.0D, 0.0D);
		}
	}

	private static boolean isOpenActorAnchor(ServerLevel level, BlockPos feet) {
		return level.getBlockState(feet).getCollisionShape(level, feet).isEmpty()
				&& level.getBlockState(feet.above()).getCollisionShape(level, feet.above()).isEmpty();
	}

	private static void purgeLoadedRadiruActors(ServerLevel level, ServerPlayer player) {
		AABB bounds = DkcFloorBuilder.radiruCastleBounds(player).inflate(96.0D);
		level.getEntitiesOfClass(EsilRadiruEntity.class, bounds, entity -> entity.isOwnedBy(player))
				.forEach(Entity::discard);
		level.getEntitiesOfClass(Mob.class, bounds, mob -> player.getStringUUID().equals(
				mob.getPersistentData().getString(OWNER_TAG))
				&& (mob.getPersistentData().getBoolean(RESIDENT_TAG)
						|| mob.getPersistentData().getBoolean(TRAINING_DUMMY_TAG)))
				.forEach(Entity::discard);
	}

	private static void configureDummy(Mob dummy, int station) {
		int index = Math.max(0, Math.min(DUMMY_ARMOR.length - 1, station));
		setBase(dummy.getAttribute(Attributes.MAX_HEALTH), 1_024.0D);
		setBase(dummy.getAttribute(Attributes.ARMOR), DUMMY_ARMOR[index]);
		setBase(dummy.getAttribute(Attributes.ARMOR_TOUGHNESS), DUMMY_TOUGHNESS[index]);
		setBase(dummy.getAttribute(Attributes.KNOCKBACK_RESISTANCE), 1.0D);
		dummy.setHealth(dummy.getMaxHealth());
		dummy.setNoAi(true);
		dummy.setCustomName(Component.literal(DUMMY_NAMES[index] + "  [" + (int) DUMMY_ARMOR[index]
				+ " Armor / " + (int) DUMMY_TOUGHNESS[index] + " Toughness]")
				.withStyle(index >= 4 ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.GOLD));
		dummy.setCustomNameVisible(true);
	}

	private static void setBase(AttributeInstance attribute, double value) {
		if (attribute != null)
			attribute.setBaseValue(value);
	}

	private static double attributeValue(AttributeInstance attribute) {
		return attribute == null ? 0.0D : attribute.getValue();
	}

	/** A replacement-safe permit that is worthless at every pedestal except Floor 15. */
	public static ItemStack createFloorPermit(ServerPlayer player) {
		ItemStack permit = new ItemStack(SololevelingModItems.ENTRY_PERMIT.get());
		CompoundTag tag = permit.getOrCreateTag();
		tag.putInt(PERMIT_FLOOR_TAG, FLOOR);
		if (player != null)
			tag.putUUID(PERMIT_OWNER_TAG, player.getUUID());
		return permit;
	}

	/** Untagged legacy permits remain valid; new bound replacements cannot skip floors. */
	public static boolean isPermitValidForFloor(ItemStack stack, ServerPlayer player, int floor) {
		if (stack == null || !stack.is(SololevelingModItems.ENTRY_PERMIT.get()))
			return false;
		CompoundTag tag = stack.getTag();
		if (tag == null || !tag.contains(PERMIT_FLOOR_TAG))
			return true;
		if (tag.getInt(PERMIT_FLOOR_TAG) != floor)
			return false;
		return player == null || !tag.hasUUID(PERMIT_OWNER_TAG)
				|| player.getUUID().equals(tag.getUUID(PERMIT_OWNER_TAG));
	}

	/** Last-resort pedestal recovery for a resolved route whose physical permit was lost. */
	public static boolean canRecoverTransitionWithoutPermit(ServerPlayer player) {
		if (player == null)
			return false;
		SololevelingModVariables.PlayerVariables vars = variables(player);
		return vars.dkc_cleared >= FLOOR && (vars.radiru_pact || vars.radiru_slaughtered)
				&& !hasEntryPermit(player);
	}

	private static boolean hasEntryPermit(ServerPlayer player) {
		for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++)
			if (isPermitValidForFloor(player.getInventory().getItem(slot), player, FLOOR))
				return true;
		return false;
	}

	private static boolean hasNearbyFloorPermit(ServerLevel level, ServerPlayer player) {
		return !level.getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(12.0D), item ->
				isPermitValidForFloor(item.getItem(), player, FLOOR)).isEmpty();
	}

	private static boolean isExecutionReady(ServerPlayer player) {
		CompoundTag data = player.getPersistentData();
		return data.getBoolean(SURRENDERED_TAG) && data.getBoolean("dkc_floor_15_complete")
				&& variables(player).dkc_cleared < FLOOR;
	}

	private static boolean isPactProtected(Entity target) {
		if (target == null || target.getPersistentData().getBoolean(TRAINING_DUMMY_TAG))
			return false;
		if (target.getPersistentData().getBoolean(SANCTUARY_TAG))
			return true;
		if (!(target instanceof EsilRadiruEntity)
				&& !target.getPersistentData().getBoolean(RESIDENT_TAG))
			return false;
		ServerPlayer owner = ownerPlayer(target);
		if (owner == null)
			return false;
		SololevelingModVariables.PlayerVariables vars = variables(owner);
		return vars.radiru_pact && !vars.radiru_slaughtered;
	}

	private static ServerPlayer ownerPlayer(Entity target) {
		if (target == null || target.getServer() == null)
			return null;
		UUID owner = null;
		if (target instanceof EsilRadiruEntity esil)
			owner = esil.getEncounterOwner().orElse(null);
		else {
			String ownerText = target.getPersistentData().getString(OWNER_TAG);
			if (!ownerText.isBlank()) {
				try {
					owner = UUID.fromString(ownerText);
				} catch (IllegalArgumentException ignored) {
					return null;
				}
			}
		}
		return owner == null ? null : target.getServer().getPlayerList().getPlayer(owner);
	}

	private static void restoreFromDeath(LivingDeathEvent event) {
		event.setCanceled(true);
		event.getEntity().setHealth(event.getEntity().getMaxHealth());
		event.getEntity().clearFire();
	}

	private static void give(ServerPlayer player, ItemStack stack) {
		if (player.getInventory().add(stack))
			return;
		ItemEntity dropped = new ItemEntity(player.level(), player.getX(), player.getY() + 0.5D,
				player.getZ(), stack);
		dropped.setNoPickUpDelay();
		player.level().addFreshEntity(dropped);
	}

	private static boolean validOwnerOnFloor(ServerLevel level, ServerPlayer player) {
		return level != null && player != null && DkcFloorRegistry.isSharedDkc(level)
				&& DkcSpatialLayout.isPlayerInFloor(player, FLOOR);
	}

	private static boolean validMercyInteraction(ServerLevel level, ServerPlayer player,
			EsilRadiruEntity esil) {
		if (level == null || player == null || esil == null || esil.level() != level
				|| !validOwnerOnFloor(level, player) || !esil.isOwnedBy(player)
				|| !DkcSpatialLayout.isEntityInOwnedFloor(esil, player.getUUID(), FLOOR)
				|| player.distanceToSqr(esil) > 64.0D)
			return false;
		AABB castle = DkcFloorBuilder.radiruCastleBounds(player);
		return castle.intersects(player.getBoundingBox()) && castle.intersects(esil.getBoundingBox());
	}

	private static void closeMercyChoice(ServerPlayer player) {
		if (player != null && PENDING_MERCY_CHOICES.remove(player.getUUID()) != null)
			sendMercyChoiceState(player, false);
	}

	private static void sendMercyChoiceState(ServerPlayer player, boolean open) {
		SololevelingMod.PACKET_HANDLER.send(PacketDistributor.PLAYER.with(() -> player),
				new RadiruMercyChoiceStateMessage(open));
	}

	private static SololevelingModVariables.PlayerVariables variables(Entity entity) {
		return entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
				.orElse(new SololevelingModVariables.PlayerVariables());
	}

	private static ServerPlayer creditedPlayer(DamageSource source) {
		if (source == null)
			return null;
		return creditedPlayer(source.getEntity());
	}

	private static ServerPlayer creditedPlayer(Entity source) {
		if (source instanceof ServerPlayer player)
			return player;
		if (source instanceof Projectile projectile)
			return creditedPlayer(projectile.getOwner());
		if (source instanceof TamableAnimal tame)
			return creditedPlayer(tame.getOwner());
		if (source != null && source.getServer() != null) {
			UUID owner = ShadowMonarchManager.getShadowOwnerUUID(source);
			if (owner != null)
				return source.getServer().getPlayerList().getPlayer(owner);
		}
		return null;
	}

	private static void notify(ServerPlayer player, int accent, String title, String under, int duration) {
		SystemNotifications.showTitleUnder(player, accent, duration,
				Component.literal(title).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
				Component.literal(under).withStyle(ChatFormatting.GRAY));
	}

	private record PendingMercyChoice(MinecraftServer server, UUID esilId, long expiresAt) {
	}
}
