package net.solocraft.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Dependency-free reset-scope regressions. These checks protect the complete
 * snapshot reset and the item/story/social safety boundaries around it.
 */
public final class PlayerResetRegression {
	private static final Path MAIN_SOURCE = Path.of(
			"src", "main", "java", "net", "solocraft");

	private PlayerResetRegression() {
	}

	public static void main(String[] args) throws IOException {
		abilityDrivenHudBarsRetireOnReset();
		persistentKeyPolicyIsPlayerScoped();
		resetUsesTheCompleteCapabilitySchema();
		jobChangeReceiptsAreActuallyRemoved();
		itemEscrowsAreResolvedBeforeStateDeletion();
		transientQuestAndDungeonStateUseScopedManagers();
		specializedRuntimeStateIsCanceledBeforePersistence();
		advancementAndVanillaStateStayInScope();
		commandDelegatesToTheResetManager();
		PlayerEntryGenerationRegression.main(args);
	}

	/**
	 * Ability-driven HUD bars appear when the server first syncs them, so they
	 * must be explicitly retired. Without the sentinel a bar stayed on screen
	 * for the rest of the session after a reset or class change, because the
	 * client had no way to learn the resource was gone.
	 */
	private static void abilityDrivenHudBarsRetireOnReset() throws IOException {
		for (String manager : new String[] { "AssassinSkillManager.java",
				"FighterSkillManager.java", "JuggernautSkillManager.java" }) {
			String source = read("util", manager);
			expectTrue(source.contains("ClassPassiveClientState.UNAVAILABLE"),
					manager + " must retire its HUD bars with the unavailable sentinel");
		}

		// The client must honour the sentinel rather than latching on forever.
		String clientState = read("util", "ClassPassiveClientState.java");
		expectTrue(clientState.contains("boolean available = value >= 0.0D"),
				"ClassPassiveClientState must treat a negative value as unavailable");
		for (String channel : new String[] { "assassinVeilAvailable = available",
				"fighterTempoAvailable = available", "fighterFeralAvailable = available",
				"tankerPoiseAvailable = available" }) {
			expectTrue(clientState.contains(channel),
					"Availability must follow the sentinel: " + channel);
		}
	}

	private static void persistentKeyPolicyIsPlayerScoped() {
		expectTrue(PlayerResetKeyPolicy.shouldClear(
				"slr_job_change_selection_authorized"),
				"Job Change authorization must reset");
		expectTrue(PlayerResetKeyPolicy.shouldClear(
				"sololeveling_shadow_monarch"),
				"The shadow roster must reset");
		expectTrue(PlayerResetKeyPolicy.shouldClear("dkc_floor_20_complete"),
				"Per-player castle progression must reset");
		expectTrue(PlayerResetKeyPolicy.shouldClear("cd_shadow_exchange"),
				"Skill cooldowns must reset");
		for (String legacyTarget : new String[] {
				"CriticalAttackTarget",
				"Mutilation_Targetting",
				"MutilationTarget" }) {
			expectTrue(PlayerResetKeyPolicy.shouldClear(legacyTarget),
					"Legacy Assassin target state must reset: " + legacyTarget);
		}

		expectFalse(PlayerResetKeyPolicy.shouldClear("slr_story_intro_owner"),
				"World-owned story state must survive");
		expectFalse(PlayerResetKeyPolicy.shouldClear(
				"slr_cartenon_awakening_pending"),
				"Pending awakening must not be partially cleared");
		expectFalse(PlayerResetKeyPolicy.shouldClear(
				"slr_guild_gate_reserved_id"),
				"Guild reservations must remain consistent");
		expectFalse(PlayerResetKeyPolicy.shouldClear(
				"slr_forgiving_death_snapshot"),
				"Inventory/XP death recovery must survive");
		expectFalse(PlayerResetKeyPolicy.shouldClear(
				"slr_runestone_skill_shadow_exchange"),
				"Consumed runestone receipts must prevent duplicate claims");
		expectFalse(PlayerResetKeyPolicy.shouldClear(
				"slr_tanker_starter_redeemed_v1"),
				"Consumed starter packs must not become claimable again");
		expectFalse(PlayerResetKeyPolicy.shouldClear(
				"slr_instance_dungeon_key_claimed"),
				"Claimed physical dungeon keys must not duplicate");
		expectFalse(PlayerResetKeyPolicy.shouldClear(
				"sl_urgent_pvp_first_reward_claimed"),
				"One-time urgent rewards must not duplicate");
		expectFalse(PlayerResetKeyPolicy.shouldClear(
				"SLRKangTaeshikAmbushCompleted"),
				"Completed Kang reward receipts must remain consumed");
		expectFalse(PlayerResetKeyPolicy.shouldClear(
				"sl_shadow_reset_generation"),
				"Unloaded pre-reset shadows must stay invalidated");
		expectFalse(PlayerResetKeyPolicy.shouldClear(
				TemporaryArmorSessionManager.GENERATION_TAG),
				"Delayed temporary armor equips must stay invalidated");
		expectFalse(PlayerResetKeyPolicy.shouldClear(
				TemporaryArmorSessionManager.ACTIVE_ESCROW_TAG),
				"Active armor escrow must survive until safe restoration");
		expectFalse(PlayerResetKeyPolicy.shouldClear(
				TemporaryArmorSessionManager.EQUIPPED_ESCROW_TAG),
				"Armor escrow phase must survive until safe restoration");
		expectFalse(PlayerResetKeyPolicy.shouldClear(
				PlayerEntryGenerationGuard.GENERATION_TAG),
				"Delayed dungeon entry callbacks must stay invalidated");
		expectFalse(PlayerResetKeyPolicy.shouldClear(
				TemporaryStatBonusMigration.MIGRATION_RECEIPT),
				"Completed temporary-stat migration must not run again after reset");
		expectFalse(PlayerResetKeyPolicy.shouldClear("othermod_character_data"),
				"Unrelated mod data must never be cleared");
	}

	private static void resetUsesTheCompleteCapabilitySchema()
			throws IOException {
		String source = read("util", "PlayerProgressResetManager.java");
		expectTrue(source.contains("capability.readNBT(player.registryAccess(),")
						&& source.contains("new SololevelingModVariables.PlayerVariables()")
						&& source.contains(".writeNBT(player.registryAccess())"),
				"Reset must use the complete PlayerVariables serialization schema");
		for (String retained : new String[] {
				"variables.Player = systemPlayer",
				"variables.LoreAccurateRankStart = loreAccurateRankStart",
				"variables.CustomHUD = customHud",
				"variables.pvpUrgentQuests = pvpUrgentQuests",
				"variables.party = party",
				"variables.GuildCode = guildCode" }) {
			expectTrue(source.contains(retained),
					"Reset must retain " + retained);
		}
		expectFalse(source.contains("MapVariables.get"),
				"A player reset must not mutate global progression counters");
		expectFalse(source.contains("getInventory().clear"),
				"A progression reset must not clear inventory");
	}

	private static void jobChangeReceiptsAreActuallyRemoved()
			throws IOException {
		String source = read("util", "JobChangeQuestManager.java");
		expectTrue(source.contains("resetForPlayerReset(ServerPlayer player)"),
				"Job Change needs a dedicated reset entry point");
		expectTrue(source.contains(
				"capability.unlocked_quests = removeToken")
				&& source.contains(
						"capability.finished_quests = removeToken"),
				"Both blocking Job Change receipt lists must reset");
		expectTrue(source.contains("closeSelection(player)"),
				"A stale vessel-selection screen must close");
	}

	private static void itemEscrowsAreResolvedBeforeStateDeletion()
			throws IOException {
		String manager = read("util", "PlayerProgressResetManager.java");
		expectTrue(manager.indexOf("recoverEscrowForReset(player)")
				< manager.indexOf("clearModPersistentData"),
				"A thrown dagger must be returned before its escrow tag is cleared");
		expectTrue(manager.indexOf(
				"restoreEscrowedArmor(player, current, temporaryArmorEscrow")
				< manager.indexOf("capability.readNBT"),
				"Temporary-form armor must be restored before overrides reset");

		String shadows = read("util", "ShadowMonarchManager.java");
		expectTrue(shadows.contains("dropStoredShadowInventory(shadow)")
				&& shadows.contains("PLAYER_RESET_GENERATION"),
				"Loaded and unloaded shadows must preserve held items during reset");

		expectTrue(manager.indexOf(
				"TemporaryArmorSessionManager.invalidatePendingEquip(player)")
				< manager.indexOf("capability.readNBT"),
				"Reset must cancel delayed manifestation equips before state deletion");
		expectTrue(manager.contains(
				"TemporaryArmorSessionManager.hasActiveEscrow(player)")
				&& manager.contains(
						"TemporaryArmorSessionManager.hasEquippedEscrow(player)")
				&& manager.contains(
						"boolean legacyTemporaryArmor = hasEquippedTemporaryArmor(player)")
				&& manager.contains(
						"TemporaryArmorSessionManager.finishAfterRestore(player)"),
				"Tagged and legacy armor escrows must restore removed pieces without duplicating stale overrides");
		expectFalse(manager.contains(
				"ItemStack.matches(player.getItemBySlot(slot), escrowed)"),
				"An identical separately-equipped stack must not suppress escrow return");
		String shadowManifestation = read("procedures",
				"Ability4OnKeyPressedProcedure.java");
		String goliathManifestation = read("procedures",
				"GoliathManifestationProcedure.java");
		expectTrue(shadowManifestation.contains(
				"TemporaryArmorSessionManager.canEquipShadow")
				&& goliathManifestation.contains(
						"TemporaryArmorSessionManager.canEquipGoliath")
				&& shadowManifestation.contains(
						"TemporaryArmorSessionManager.markEquipped")
				&& goliathManifestation.contains(
						"TemporaryArmorSessionManager.markEquipped"),
				"Both delayed armor callbacks must revalidate their session");
		String shadowArmorTick = read("procedures",
				"ShadowARMORHelmetTickEventProcedure.java");
		String goliathArmorTick = read("procedures",
				"GoliathArmorTickProcedure.java");
		expectTrue(shadowArmorTick.contains(
				"TemporaryArmorSessionManager.finishAfterRestore")
				&& goliathArmorTick.contains(
						"TemporaryArmorSessionManager.finishAfterRestore"),
				"Automatic manifestation expiry must close the durable escrow");
		String armorSessions = read("util",
				"TemporaryArmorSessionManager.java");
		expectTrue(armorSessions.contains("GENERATION_TAG")
				&& armorSessions.contains("ACTIVE_ESCROW_TAG")
				&& armorSessions.contains("EQUIPPED_ESCROW_TAG")
				&& armorSessions.contains("variables.JOB")
				&& armorSessions.contains("ItemStack.matches"),
				"Delayed equip revalidation must cover token, job and saved armor");
	}

	private static void transientQuestAndDungeonStateUseScopedManagers()
			throws IOException {
		String source = read("util", "PlayerProgressResetManager.java");
		for (String call : new String[] {
				"AssassinSkillManager.resetPlayerState(player)",
				"FighterSkillManager.resetPlayerState(player)",
				"HealerSkillManager.resetPlayerState(player)",
				"JuggernautSkillManager.resetPlayerState(player)",
				"UrgentQuestManager.resetForPlayerReset(player)",
				"JobChangeQuestManager.resetForPlayerReset(player)",
				"DkcRadiruManager.resetPlayerState(player)",
				"DkcQuestProgressTracker.resetPlayerState(player)",
				"DkcRunSavedData.get(player.server).resetProgress(player.getUUID())",
				"DailyQuestLifecycleManager.resetQuestState(player, true)",
				"PartyHighlightManager.clearNow(player)",
				"PartyHighlightManager.syncNow(player)" }) {
			expectTrue(source.contains(call), "Missing scoped reset: " + call);
		}
		expectTrue(source.contains("StoryModeIntroManager.isStoryOwner(player)")
				&& source.contains("slr_cartenon_awakening_pending"),
				"Reset must be blocked during world-owned awakening scripts");
		String urgent = read("util", "UrgentQuestManager.java");
		expectTrue(urgent.contains("RECENT_CRITICALS.keySet().removeIf")
				&& urgent.contains("KangTaeshikAmbushManager.resetPlayerProgress"),
				"Urgent quest runtime and the player's Kang must reset together");
		String assassin = read("util", "AssassinSkillManager.java");
		expectTrue(assassin.contains("resetPlayerState(ServerPlayer player)")
				&& assassin.contains("STATES.remove(player.getUUID())")
				&& assassin.contains("endStealth(player, removed)")
				&& assassin.contains("syncTempo(player, removed)"),
				"Assassin state, decoy/stealth and tempo UI must reset together");
	}

	private static void specializedRuntimeStateIsCanceledBeforePersistence()
			throws IOException {
		String manager = read("util", "PlayerProgressResetManager.java");
		int persistentClear = manager.indexOf("clearModPersistentData");
		for (String call : new String[] {
				"PlayerEntryGenerationGuard.invalidate(player)",
				"ArcaneMageSpellManager.resetPlayerState(player)",
				"BarrierMageSpellManager.resetPlayerState(player)",
				"FireMageSpellManager.resetPlayerState(player)",
				"StormMageSpellManager.resetPlayerState(player)",
				"RangerCombatManager.resetPlayerState(player)",
				"ClassPassiveManager.resetPlayerState(player)",
				"TankerSkillManager.resetPlayerState(player)",
				"FrostArchitectureManager.resetPlayerState(player)",
				"FrostMonarchManager.resetPlayerState(player)",
				"RulersAuthorityManager.resetPlayerState(player)",
				"GoliathCombatManager.resetPlayerState(player)",
				"LiuZhigangCombatManager.resetPlayerState(player)",
				"BeastMonarchManager.resetPlayerState(player)",
				"WhiteFlameMonarchManager.resetPlayerState(player)",
				"DkcFloorBuilder.cancelPlayerBuilds(player.server, player.getUUID())" }) {
			int callIndex = manager.indexOf(call);
			expectTrue(callIndex >= 0 && callIndex < persistentClear,
					"Runtime cleanup must precede persistent reset: " + call);
		}
		expectTrue(manager.indexOf("PlayerEntryGenerationGuard.invalidate(player)")
				< manager.indexOf("detachFromActiveDungeon(player, current)"),
				"Queued dungeon entry must be invalidated before reset detaches the player");
		expectTrue(manager.indexOf(
				"DkcFloorBuilder.cancelPlayerBuilds(player.server, player.getUUID())")
				< manager.indexOf(
						"DkcRunSavedData.get(player.server).resetProgress(player.getUUID())"),
				"Queued castle construction must stop before its progress record resets");

		String tanker = read("util", "TankerSkillManager.java");
		String tankerReset = methodSlice(tanker,
				"public static void resetPlayerState(ServerPlayer player)",
				"private static void clearLegacyCancellationState");
		expectTrue(tankerReset.contains("STATES.remove(playerId)")
				&& tankerReset.contains("removeMark(playerId, MarkEnd.CANCEL)")
				&& tankerReset.contains("removeChallengesFor(playerId)")
				&& tankerReset.contains("removeOwnedSlows(playerId)")
				&& tankerReset.contains("clearPersistedWillpower(player)")
				&& tankerReset.contains("clearIronWall(player)")
				&& tankerReset.contains("state.willpower = null"),
				"Tanker reset must discard combat, control, Iron Wall and Willpower debt");
		expectFalse(tankerReset.contains("beginSettlement"),
				"Reset must not convert Willpower debt into damage pulses");

		String frost = read("util", "FrostArchitectureManager.java");
		expectTrue(frost.contains("resetPlayerState(ServerPlayer player)")
				&& frost.contains("BUILD_TASKS.remove(player.getUUID())"),
				"Frozen Architecture must cancel an unfinished blueprint");

		String rulers = read("util", "RulersAuthorityManager.java");
		String rulersReset = methodSlice(rulers,
				"public static void resetPlayerState(ServerPlayer player)",
				"@SubscribeEvent");
		expectTrue(rulersReset.contains("SESSIONS.remove(ownerId)")
				&& rulersReset.contains("discardAura(session)")
				&& rulersReset.contains("restoreGravity(controlled, session)")
				&& rulersReset.contains("THROWN.entrySet().iterator()"),
				"Ruler's Authority must release controlled and thrown targets safely");
		expectFalse(rulersReset.contains("remove(LAUNCH_PROTECTION)"),
				"Canceled throws must retain fall protection until their victim lands safely");

		String frostMonarch = read("util", "FrostMonarchManager.java");
		String frostMonarchReset = methodSlice(frostMonarch,
				"public static void resetPlayerState(ServerPlayer player)",
				"public static boolean isFrostMonarch");
		expectTrue(frostMonarchReset.contains("clearAll(player, true)")
				&& frostMonarchReset.contains(
						"GLACIAL_PURSUITS.containsKey(player.getUUID())")
				&& frostMonarchReset.contains(
						"PATH_FALL_PROTECTION_UNTIL"),
				"Frost reset must cancel its runtime while giving an interrupted pursuit time to land");

		String whiteFlame = read("util", "WhiteFlameMonarchManager.java");
		String whiteFlameReset = methodSlice(whiteFlame,
				"public static void resetPlayerState(ServerPlayer player)",
				"public static boolean isWhiteFlameVessel");
		expectTrue(whiteFlameReset.contains("player.server.getAllLevels()")
				&& whiteFlameReset.contains("SUMMON_OWNER")
				&& whiteFlameReset.contains("summon.discard()")
				&& whiteFlameReset.contains("PlayerAuraSystem.clearContinuous(player)"),
				"White Flame reset must dismiss loaded army summons and aura state");

		String floorBuilder = read("dkc", "DkcFloorBuilder.java");
		String floorCancel = methodSlice(floorBuilder,
				"public static void cancelPlayerBuilds(MinecraftServer server, UUID playerId)",
				"public static void prepareFloor");
		expectTrue(floorCancel.contains("cancelBuild(key, context)")
				&& floorCancel.contains("OWNED_SPAWN_GUARDS.keySet().removeIf"),
				"DKC reset must invalidate only the player's queued builds and spawn guards");
	}

	private static void advancementAndVanillaStateStayInScope()
			throws IOException {
		String source = read("util", "PlayerProgressResetManager.java");
		expectTrue(source.contains(
				"SololevelingMod.MODID.equals(advancement.id().getNamespace())"),
				"Only this mod's progression advancements may be revoked");
		expectTrue(source.contains(
				"\"awakened\".equals(advancement.id().getPath())"),
				"System awakening eligibility must survive reset");
		expectTrue(source.contains(
				"SololevelingMod.MODID.equals(effectId.getNamespace())"),
				"Only this mod's registered effects may be removed");
		expectTrue(source.contains("PlayerVitalSync.restoreAfterRespawn(player)"),
				"Fresh characters must receive valid starting Mana/MP and ordered vital sync");
		expectFalse(source.contains("removeAllEffects"),
				"Vanilla and other-mod potion effects must survive");
		expectFalse(source.contains("setAbsorptionAmount")
				|| source.contains("setRemainingFireTicks")
				|| source.contains("setDeltaMovement"),
				"Unrelated vanilla transient state must survive");
	}

	private static void commandDelegatesToTheResetManager()
			throws IOException {
		String source = read("procedures", "SLRResetProcedure.java");
		expectTrue(source.contains("PlayerProgressResetManager.reset(player)"),
				"/slr reset must use the complete centralized reset");
		expectFalse(source.contains("capability."),
				"The command wrapper must not grow another partial field list");
	}

	private static String read(String directory, String file)
			throws IOException {
		return Files.readString(MAIN_SOURCE.resolve(directory).resolve(file));
	}

	private static String methodSlice(String source, String startMarker,
			String endMarker) {
		int start = source.indexOf(startMarker);
		int end = source.indexOf(endMarker, Math.max(0, start));
		expectTrue(start >= 0 && end > start,
				"Missing source markers: " + startMarker + " / " + endMarker);
		return source.substring(start, end);
	}

	private static void expectTrue(boolean value, String message) {
		if (!value)
			throw new AssertionError(message);
	}

	private static void expectFalse(boolean value, String message) {
		expectTrue(!value, message);
	}
}
