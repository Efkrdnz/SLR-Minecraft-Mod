package net.solocraft.util;

import net.solocraft.SololevelingMod;
import net.solocraft.network.SololevelingModVariables;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.ArrayList;
import java.util.List;

/**
 * Server-authoritative bridge between System progression, vessel skill access,
 * and the advancement UI. Advancement JSON remains presentation-only; every
 * unlock condition is checked here so commands or stale client state cannot
 * equip a skill early.
 */
@Mod.EventBusSubscriber(modid = SololevelingMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class VesselProgressionManager {
	private static final int SYNC_INTERVAL = 40;

	private VesselProgressionManager() {
	}

	@SubscribeEvent
	public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
		if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)
				|| player.tickCount % SYNC_INTERVAL != Math.floorMod(player.getId(), SYNC_INTERVAL))
			return;
		sync(player);
	}

	@SubscribeEvent
	public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
		if (event.getEntity() instanceof ServerPlayer player)
			reconcileEntitlements(player);
	}

	@SubscribeEvent
	public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
		if (event.getEntity() instanceof ServerPlayer player)
			reconcileEntitlements(player);
	}

	/**
	 * Re-evaluates every durable progression fact before rebuilding the player's
	 * concrete job-skill list. Call this after a job or prerequisite milestone
	 * changes so either completion order produces the same result immediately.
	 */
	public static void reconcileEntitlements(ServerPlayer player) {
		sync(player);
		JobSkillManager.syncJobSkills(player);
	}

	public static void sync(ServerPlayer player) {
		if (player == null || player.server == null)
			return;

		SololevelingModVariables.PlayerVariables vars = variables(player);
		VesselManager.VesselDefinition definition = VesselManager.currentDefinition(player);
		int resolvedJob = definition == null ? (int) vars.JOB : definition.jobId();
		if (vars.Player) {
			award(player, "system/root");
			if (vars.Level >= 10)
				award(player, "system/level_10");
			if (vars.Level >= 30)
				award(player, "system/level_30");
			if (vars.Level >= 50)
				award(player, "system/level_50");
			if (vars.Level >= 100)
				award(player, "system/level_100");
			if (JobChangeQuestManager.isUnlocked(player))
				award(player, "system/job_change");
		}

		syncDemonKingsCastle(player, vars, resolvedJob);
		MageSpellProgression.reconcileVesselInheritance(player);
		if (resolvedJob <= 0)
			return;

		award(player, "system/vessel");
		if (definition != null) {
			syncVesselIdentity(player, definition);
			// Explicit vessel identity is authoritative over stale legacy JOB data.
			// Advancement grants are irreversible, so never dispatch them from a
			// mismatched numeric job if a canonical definition is available.
			resolvedJob = definition.jobId();
		}

		boolean changed = false;
		if (resolvedJob == 1) {
			if (!vars.ShadowExchange && VesselProgressionRules.canUnlockShadowExchange(
					resolvedJob, vars.ShadowExchange, vars.shadowstorageusage)) {
				vars.ShadowExchange = true;
				changed = true;
			}
			if (!vars.ShadowBody && VesselProgressionRules.canUnlockShadowManifestation(
					resolvedJob, vars.ShadowBody, vars.Level, vars.shadowstorageusage,
					vars.ShadowExchange, vars.dkc_cleared)) {
				vars.ShadowBody = true;
				changed = true;
			}
		}
		if (changed) {
			player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
				capability.ShadowExchange = vars.ShadowExchange;
				capability.ShadowBody = vars.ShadowBody;
				capability.syncPlayerVariables(player);
			});
			JobSkillManager.syncJobSkills(player);
		}

		syncSkillAdvancements(player, resolvedJob);
	}

	public static List<String> unlockedSkills(Entity entity, int job) {
		SololevelingModVariables.PlayerVariables vars = variables(entity);
		int level = Math.max(0, (int) Math.floor(vars.Level));
		ArrayList<String> skills = new ArrayList<>();

		switch (job) {
			case 1 -> {
				add(skills, JobSkillManager.ARISE, JobSkillManager.SHADOW_SUMMON,
						JobSkillManager.DISMISS_SHADOWS, JobSkillManager.SHADOW_COMMAND);
				if (vars.ShadowExchange)
					skills.add(JobSkillManager.SHADOW_EXCHANGE);
				if (vars.ShadowBody)
					skills.add(JobSkillManager.SHADOW_MANIFESTATION);
			}
			case 2 -> {
				skills.add(JobSkillManager.FIRE_CHARGE);
				if (level >= 65)
					skills.add(JobSkillManager.METEOR_RAIN);
				if (level >= 85)
					skills.add(JobSkillManager.FIREFLIES);
			}
			case 3 -> {
				skills.add(JobSkillManager.ICE_SPEAR);
				if (level >= 55)
					skills.add(JobSkillManager.FLASH_FREEZE);
				if (level >= 65)
					skills.add(JobSkillManager.FROZEN_PATH);
				if (level >= 75)
					skills.add(JobSkillManager.FROZEN_ARCHITECTURE);
				if (level >= 85)
					skills.add(JobSkillManager.FROST_COUNTER);
				if (level >= 100)
					skills.add(JobSkillManager.ABSOLUTE_ZERO);
				if (level >= 120)
					skills.add(JobSkillManager.FROST_SPIRITUALIZATION);
			}
			case 4 -> {
				skills.add(JobSkillManager.LIGHTNING_BREATH);
				if (level >= 60)
					skills.add(JobSkillManager.RADIRU_BLOOD_SPEAR);
				if (level >= 70)
					skills.add(JobSkillManager.DOPPELGANGER);
				if (level >= 85)
					skills.add(JobSkillManager.HELLSTORM_DOMINION);
				if (level >= 100)
					skills.add(JobSkillManager.HELLS_ARMY);
				if (level >= 120)
					skills.add(JobSkillManager.WHITE_FLAME_SPIRITUALIZATION);
			}
			case 5 -> {
				// Ruler vessels receive their spiritual body at selection.
				skills.add(JobSkillManager.THOMAS_MANIFESTATION);
				if (level >= 55)
					skills.add(JobSkillManager.THOMAS_CAPTURE);
				if (level >= 70)
					skills.add(JobSkillManager.THOMAS_POWER_SMASH);
				if (level >= 90)
					skills.add(JobSkillManager.THOMAS_COLLAPSE);
			}
			case 6 -> {
				// Ruler vessels receive their spiritual body at selection.
				skills.add(JobSkillManager.LIU_MANIFESTATION);
				if (level >= 55)
					skills.add(JobSkillManager.LIU_HEAVENLY_COUNTER);
				if (level >= 75)
					skills.add(JobSkillManager.LIU_GOLDEN_DRAGON_DANCE);
				if (level >= 95)
					skills.add(JobSkillManager.LIU_SOVEREIGN_SWORD_DOMAIN);
			}
			case 9 -> {
				skills.add(JobSkillManager.BEAST_CLAW_RIFT);
				if (level >= 60)
					skills.add(JobSkillManager.BEAST_RUBBLE_JAW);
				if (level >= 75)
					skills.add(JobSkillManager.BEAST_KINGS_MAUL);
				if (level >= 90)
					skills.add(JobSkillManager.BEAST_RECONSTITUTION);
				if (level >= 120)
					skills.add(JobSkillManager.BEAST_WHITE_FANG);
			}
			default -> {
			}
		}
		return List.copyOf(skills);
	}

	public static boolean canUnlockShadowExchange(Entity entity) {
		SololevelingModVariables.PlayerVariables vars = variables(entity);
		return VesselProgressionRules.canUnlockShadowExchange(
				resolvedJob(entity, vars), vars.ShadowExchange, vars.shadowstorageusage);
	}

	public static boolean canUnlockShadowManifestation(Entity entity) {
		SololevelingModVariables.PlayerVariables vars = variables(entity);
		return VesselProgressionRules.canUnlockShadowManifestation(
				resolvedJob(entity, vars), vars.ShadowBody, vars.Level,
				vars.shadowstorageusage, vars.ShadowExchange, vars.dkc_cleared);
	}

	private static void syncDemonKingsCastle(ServerPlayer player,
			SololevelingModVariables.PlayerVariables vars, int resolvedJob) {
		if (vars.dkc_started || vars.dkc_cleared > 0)
			award(player, "system/dkc_entered");
		if (vars.dkc_cleared >= 1)
			award(player, "system/dkc_first_floor");
		if (vars.dkc_cleared >= 10) {
			award(player, "system/dkc_midpoint");
			if (VesselProgressionRules.hasMonarchsDomain(resolvedJob, vars.dkc_cleared))
				award(player, "monarchs_domain");
		}
		if (vars.dkc_cleared >= 15) {
			award(player, "system/dkc_radiru");
			if (vars.radiru_pact && !vars.radiru_slaughtered)
				award(player, "system/dkc_radiru_pact");
			if (vars.radiru_slaughtered)
				award(player, "system/dkc_radiru_bloodshed");
		}
		if (vars.dkc_cleared >= 20)
			award(player, "system/dkc_conquered");
	}

	private static void syncVesselIdentity(ServerPlayer player, VesselManager.VesselDefinition definition) {
		String identity = definition.identity();
		switch (identity) {
			case "ashborn" -> award(player, "vessels/shadow_monarch");
			case "sillad" -> award(player, "coldest_monarch");
			case "baran" -> award(player, "monarch_of_blue_flames");
			case "rakan" -> award(player, "vessels/monarch_of_fangs");
			case "christopher_reed" -> {
				award(player, "scorching_mage");
				award(player, "vessels/rulers/flame_manifestation");
			}
			case "thomas_andre" -> award(player, "vessels/thomas_andre");
			case "liu_zhigang" -> award(player, "vessels/liu_zhigang");
			case "sung_il_hwan" -> {
				award(player, "vessels/sung_il_hwan");
				award(player, "vessels/rulers/silent_manifestation");
			}
			case "go_gunhee" -> {
				award(player, "vessels/go_gunhee");
				award(player, "vessels/rulers/brilliant_manifestation");
			}
			default -> {
			}
		}
		if (VesselManager.RULER.equals(definition.type()) && !"ashborn".equals(identity))
			award(player, "rulers_will");
	}

	private static void syncSkillAdvancements(ServerPlayer player, int job) {
		List<String> unlocked = unlockedSkills(player, job);
		switch (job) {
			case 1 -> {
				award(player, "shadow_extraction");
				award(player, "shadow_storage");
				double level = variables(player).Level;
				if (level >= 70)
					award(player, "shadow_storage_level_2");
				if (level >= 90)
					award(player, "shadow_storage_level_3");
				if (level >= 100)
					award(player, "shadow_storage_level_4");
				if (level >= 120)
					award(player, "shadow_storage_level_5");
				if (unlocked.contains(JobSkillManager.SHADOW_EXCHANGE))
					award(player, "shadow_exchange");
				if (unlocked.contains(JobSkillManager.SHADOW_MANIFESTATION))
					award(player, "shadow_spiritual_body_manifestation");
			}
			case 2 -> {
				award(player, "vessels/flame/fire_charge");
				if (unlocked.contains(JobSkillManager.METEOR_RAIN))
					award(player, "vessels/flame/meteor_rain");
				if (unlocked.contains(JobSkillManager.FIREFLIES))
					award(player, "vessels/flame/fireflies");
			}
			case 3 -> {
				award(player, "vessels/frost/ice_spear");
				awardIfUnlocked(player, unlocked, JobSkillManager.FLASH_FREEZE, "vessels/frost/flash_freeze");
				awardIfUnlocked(player, unlocked, JobSkillManager.FROZEN_PATH, "vessels/frost/frozen_path");
				awardIfUnlocked(player, unlocked, JobSkillManager.FROZEN_ARCHITECTURE, "vessels/frost/frozen_architecture");
				awardIfUnlocked(player, unlocked, JobSkillManager.FROST_COUNTER, "vessels/frost/frost_counter");
				awardIfUnlocked(player, unlocked, JobSkillManager.ABSOLUTE_ZERO, "vessels/frost/absolute_zero");
				awardIfUnlocked(player, unlocked, JobSkillManager.FROST_SPIRITUALIZATION, "vessels/frost/spiritual_body");
			}
			case 4 -> {
				award(player, "vessels/white_flames/lightning_breath");
				award(player, "vessels/white_flames/storm_inheritance");
				awardIfUnlocked(player, unlocked, JobSkillManager.RADIRU_BLOOD_SPEAR, "vessels/white_flames/blood_spear");
				awardIfUnlocked(player, unlocked, JobSkillManager.DOPPELGANGER, "vessels/white_flames/doppelganger");
				awardIfUnlocked(player, unlocked, JobSkillManager.HELLSTORM_DOMINION, "vessels/white_flames/hellstorm");
				awardIfUnlocked(player, unlocked, JobSkillManager.HELLS_ARMY, "vessels/white_flames/hells_army");
				awardIfUnlocked(player, unlocked, JobSkillManager.WHITE_FLAME_SPIRITUALIZATION, "vessels/white_flames/spiritual_body");
			}
			case 5 -> {
				award(player, "vessels/rulers/goliath_manifestation");
				awardIfUnlocked(player, unlocked, JobSkillManager.THOMAS_CAPTURE, "vessels/rulers/capture");
				awardIfUnlocked(player, unlocked, JobSkillManager.THOMAS_POWER_SMASH, "vessels/rulers/power_smash");
				awardIfUnlocked(player, unlocked, JobSkillManager.THOMAS_COLLAPSE, "vessels/rulers/collapse");
			}
			case 6 -> {
				award(player, "vessels/rulers/dragon_sword_manifestation");
				awardIfUnlocked(player, unlocked, JobSkillManager.LIU_HEAVENLY_COUNTER, "vessels/rulers/heavenly_counter");
				awardIfUnlocked(player, unlocked, JobSkillManager.LIU_GOLDEN_DRAGON_DANCE, "vessels/rulers/golden_dragon_dance");
				awardIfUnlocked(player, unlocked, JobSkillManager.LIU_SOVEREIGN_SWORD_DOMAIN, "vessels/rulers/sword_domain");
			}
			case 9 -> {
				award(player, "vessels/fangs/claw_rift");
				awardIfUnlocked(player, unlocked, JobSkillManager.BEAST_RUBBLE_JAW, "vessels/fangs/rubble_jaw");
				awardIfUnlocked(player, unlocked, JobSkillManager.BEAST_KINGS_MAUL, "vessels/fangs/kings_maul");
				awardIfUnlocked(player, unlocked, JobSkillManager.BEAST_RECONSTITUTION, "vessels/fangs/reconstitution");
				awardIfUnlocked(player, unlocked, JobSkillManager.BEAST_WHITE_FANG, "vessels/fangs/spiritual_body");
			}
			default -> {
			}
		}
	}

	private static void awardIfUnlocked(ServerPlayer player, List<String> unlocked, String skill, String advancement) {
		if (unlocked.contains(skill))
			award(player, advancement);
	}

	private static boolean award(ServerPlayer player, String path) {
		Advancement advancement = player.server.getAdvancements()
				.getAdvancement(new ResourceLocation(SololevelingMod.MODID, path));
		if (advancement == null)
			return false;
		AdvancementProgress progress = player.getAdvancements().getOrStartProgress(advancement);
		if (progress.isDone())
			return false;
		ArrayList<String> remaining = new ArrayList<>();
		progress.getRemainingCriteria().forEach(remaining::add);
		for (String criterion : remaining)
			player.getAdvancements().award(advancement, criterion);
		return true;
	}

	private static SololevelingModVariables.PlayerVariables variables(Entity entity) {
		return entity == null
				? new SololevelingModVariables.PlayerVariables()
				: entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
						.orElse(new SololevelingModVariables.PlayerVariables());
	}

	private static int resolvedJob(Entity entity, SololevelingModVariables.PlayerVariables vars) {
		VesselManager.VesselDefinition definition = VesselManager.currentDefinition(entity);
		return definition == null ? (int) vars.JOB : definition.jobId();
	}

	private static void add(List<String> target, String... skills) {
		target.addAll(List.of(skills));
	}
}
