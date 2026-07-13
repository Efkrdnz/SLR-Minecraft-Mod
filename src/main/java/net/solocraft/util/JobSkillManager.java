package net.solocraft.util;

import net.solocraft.SololevelingMod;
import net.solocraft.network.SololevelingModVariables;
import net.solocraft.procedures.Ability1OnKeyPressedProcedure;
import net.solocraft.procedures.Ability1OnKeyReleasedProcedure;
import net.solocraft.procedures.Ability2OnKeyPressedProcedure;
import net.solocraft.procedures.Ability3OnKeyPressedProcedure;
import net.solocraft.procedures.Ability3ResetProcedure;
import net.solocraft.procedures.Ability4OnKeyPressedProcedure;
import net.solocraft.procedures.AriseSkillProcedure;
import net.solocraft.procedures.DoesHaveExchangeProcedure;
import net.solocraft.procedures.DoesHaveShadowManifestationProcedure;
import net.solocraft.procedures.GoliathManifestationProcedure;
import net.solocraft.procedures.ShadowCommandOpenProcedure;
import net.solocraft.procedures.SkillSlotHelper;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LevelAccessor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;

@Mod.EventBusSubscriber
public class JobSkillManager {
	public static final String ARISE = "Arise";
	public static final String SHADOW_SUMMON = "Shadow Summon";
	public static final String DISMISS_SHADOWS = "Dismiss Shadows";
	public static final String SHADOW_COMMAND = "Shadow Command";
	public static final String SHADOW_EXCHANGE = "Shadow Exchange";
	public static final String SHADOW_MANIFESTATION = "Shadow Manifestation";
	public static final String FIRE_CHARGE = "Fire Charge";
	public static final String METEOR_RAIN = "Meteor Rain";
	public static final String FIREFLIES = "Fireflies";
	public static final String ICE_BALL = "Ice Ball";
	public static final String ICE_CHUNK = "Ice Chunk";
	public static final String ICE_SPEAR = "Ice Spear";
	public static final String SNOW_SCREEN = "Snow Screen";
	public static final String MONARCH_BEAM = "Monarch Beam";
	public static final String LIGHTNING_STORM = "Lightning Storm";
	public static final String STORM_BURST = "Storm Burst";
	public static final String THOMAS_MANIFESTATION = "Spiritual Body Manifestation";
	public static final String THOMAS_CAPTURE = GoliathCombatManager.CAPTURE;
	public static final String THOMAS_POWER_SMASH = GoliathCombatManager.POWER_SMASH;
	public static final String THOMAS_COLLAPSE = GoliathCombatManager.COLLAPSE;

	private static final String LAST_SYNCED_JOB = "sololeveling:last_synced_job_skills";

	private static final List<String> ALL_JOB_SKILLS = List.of(
			ARISE, SHADOW_SUMMON, DISMISS_SHADOWS, SHADOW_COMMAND, SHADOW_EXCHANGE, SHADOW_MANIFESTATION,
			FIRE_CHARGE, METEOR_RAIN, FIREFLIES,
			ICE_BALL, ICE_CHUNK, ICE_SPEAR, SNOW_SCREEN,
			THOMAS_CAPTURE, THOMAS_POWER_SMASH, THOMAS_COLLAPSE, THOMAS_MANIFESTATION,
			MONARCH_BEAM, LIGHTNING_STORM, STORM_BURST);

	private JobSkillManager() {
	}

	@SubscribeEvent
	public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
		if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide())
			return;
		if (event.player.tickCount % 40 != 0)
			return;
		syncJobSkills(event.player);
	}

	public static boolean isJobSkill(String skill) {
		return ALL_JOB_SKILLS.contains(skill);
	}

	public static int skillColor(String skill) {
		if (List.of(ARISE, SHADOW_SUMMON, DISMISS_SHADOWS, SHADOW_COMMAND, SHADOW_EXCHANGE, SHADOW_MANIFESTATION).contains(skill))
			return 0xB965FF;
		if (List.of(FIRE_CHARGE, METEOR_RAIN, FIREFLIES).contains(skill))
			return 0xFF5A32;
		if (List.of(ICE_BALL, ICE_CHUNK, ICE_SPEAR, SNOW_SCREEN).contains(skill))
			return 0x6FE8FF;
		if (List.of(THOMAS_CAPTURE, THOMAS_POWER_SMASH, THOMAS_COLLAPSE, THOMAS_MANIFESTATION).contains(skill))
			return 0xFFD35A;
		if (List.of(MONARCH_BEAM, LIGHTNING_STORM, STORM_BURST).contains(skill))
			return 0xFFE38A;
		return 0xFFFFFF;
	}

	public static List<Component> tooltip(Entity entity, String skill) {
		if (!List.of(THOMAS_CAPTURE, THOMAS_POWER_SMASH, THOMAS_COLLAPSE, THOMAS_MANIFESTATION).contains(skill))
			return List.of(Component.literal(ShadowMonarchManager.displaySkillName(entity, skill)), Component.literal(skill));
		boolean manifested = GoliathCombatManager.isManifested(entity);
		ArrayList<Component> lines = new ArrayList<>();
		lines.add(Component.literal(skill).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
		switch (skill) {
			case THOMAS_CAPTURE -> {
				lines.add(Component.literal("Pull and restrain enemies with crushing authority.").withStyle(ChatFormatting.GRAY));
				lines.add(Component.literal(manifested ? "Sovereign Capture: recast to hurl every captured target." : "Manifested: wider pull, suspension, and a throw recast.").withStyle(ChatFormatting.YELLOW));
			}
			case THOMAS_POWER_SMASH -> {
				lines.add(Component.literal("Drive a mana-loaded fist through a frontal formation.").withStyle(ChatFormatting.GRAY));
				lines.add(Component.literal(manifested ? "Goliath Breaker: extended wave and fractured guard." : "Manifested: greater reach, damage, and guard damage.").withStyle(ChatFormatting.YELLOW));
			}
			case THOMAS_COLLAPSE -> {
				lines.add(Component.literal("Shatter the battlefield with a radial ground impact.").withStyle(ChatFormatting.GRAY));
				lines.add(Component.literal(manifested ? "Continental Collapse: pressure lock followed by a second rupture." : "Manifested: larger impact and a delayed second shockwave.").withStyle(ChatFormatting.YELLOW));
			}
			case THOMAS_MANIFESTATION -> {
				lines.add(Component.literal("Manifest the golden Goliath armor.").withStyle(ChatFormatting.GRAY));
				lines.add(Component.literal("Transforms all Goliath skills and combat-stance attacks.").withStyle(ChatFormatting.YELLOW));
			}
			default -> {
			}
		}
		return lines;
	}

	public static void syncJobSkills(Entity entity) {
		if (entity == null)
			return;
		SololevelingModVariables.PlayerVariables vars = entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables());
		int job = (int) vars.JOB;
		List<String> granted = skillsForEntityJob(entity, job);
		boolean keepFormations = job == 1;
		entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
			String updatedList = writeSkillList(mergedJobSkillList(entity, capability.Plist, granted, keepFormations));
			boolean changed = !updatedList.equals(capability.Plist);
			changed |= clearStaleEquippedSkills(capability, granted, keepFormations);
			if (changed) {
				capability.Plist = updatedList;
				capability.syncPlayerVariables(entity);
			}
		});
		entity.getPersistentData().putInt(LAST_SYNCED_JOB, job);
	}

	public static boolean cast(LevelAccessor world, double x, double y, double z, Entity entity, String skill) {
		if (entity == null || skill == null || !isJobSkill(skill))
			return false;
		syncJobSkills(entity);
		SololevelingModVariables.PlayerVariables vars = entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables());
		if (!skillsForEntityJob(entity, (int) vars.JOB).contains(skill)) {
			if (entity instanceof Player player && !player.level().isClientSide())
				player.displayClientMessage(Component.literal("This job skill does not belong to your current job."), true);
			return true;
		}
		switch (skill) {
			case ARISE -> AriseSkillProcedure.execute(world, x, y, z, entity);
			case SHADOW_SUMMON -> runOldJobAbility(entity, () -> Ability1OnKeyPressedProcedure.execute(world, x, y, z, entity));
			case DISMISS_SHADOWS -> Ability3ResetProcedure.execute(world, x, y, z, entity);
			case SHADOW_COMMAND -> ShadowCommandOpenProcedure.execute(world, x, y, z, entity);
			case SHADOW_EXCHANGE -> runOldJobAbility(entity, () -> Ability3OnKeyPressedProcedure.execute(world, x, y, z, entity));
			case SHADOW_MANIFESTATION -> runOldJobAbility(entity, () -> Ability4OnKeyPressedProcedure.execute(world, x, y, z, entity));
			case THOMAS_CAPTURE -> GoliathCombatManager.castCapture(entity);
			case THOMAS_POWER_SMASH -> GoliathCombatManager.castPowerSmash(entity);
			case THOMAS_COLLAPSE -> GoliathCombatManager.castCollapse(entity);
			case THOMAS_MANIFESTATION -> GoliathManifestationProcedure.execute(world, x, y, z, entity);
			case FIRE_CHARGE -> castFireCharge(world, x, y, z, entity);
			case METEOR_RAIN -> runOldJobAbility(entity, () -> Ability2OnKeyPressedProcedure.execute(world, x, y, z, entity));
			case FIREFLIES -> runOldJobAbility(entity, () -> Ability3OnKeyPressedProcedure.execute(world, x, y, z, entity));
			case ICE_BALL -> runOldJobAbility(entity, () -> Ability1OnKeyPressedProcedure.execute(world, x, y, z, entity));
			case ICE_CHUNK -> runOldJobAbility(entity, () -> Ability2OnKeyPressedProcedure.execute(world, x, y, z, entity));
			case ICE_SPEAR -> runOldJobAbility(entity, () -> Ability3OnKeyPressedProcedure.execute(world, x, y, z, entity));
			case SNOW_SCREEN -> runOldJobAbility(entity, () -> Ability4OnKeyPressedProcedure.execute(world, x, y, z, entity));
			case MONARCH_BEAM -> castMonarchBeam(entity);
			case LIGHTNING_STORM -> runOldJobAbility(entity, () -> Ability2OnKeyPressedProcedure.execute(world, x, y, z, entity));
			case STORM_BURST -> runOldJobAbility(entity, () -> Ability3OnKeyPressedProcedure.execute(world, x, y, z, entity));
			default -> {
			}
		}
		return true;
	}

	public static String cooldownKey(String skill) {
		return switch (skill) {
			case FIRE_CHARGE, MONARCH_BEAM -> "job_1";
			case METEOR_RAIN, ICE_CHUNK, LIGHTNING_STORM -> "job_2";
			case FIREFLIES, ICE_SPEAR, STORM_BURST -> "job_3";
			case SHADOW_MANIFESTATION, SNOW_SCREEN, THOMAS_MANIFESTATION -> "job_4";
			case THOMAS_CAPTURE, THOMAS_POWER_SMASH, THOMAS_COLLAPSE -> skill;
			default -> skill;
		};
	}

	private static void castFireCharge(LevelAccessor world, double x, double y, double z, Entity entity) {
		if (CooldownManager.isOnCooldown(entity, "job_1")) {
			if (entity instanceof Player player && !player.level().isClientSide())
				player.displayClientMessage(Component.literal("Ability on cooldown!"), true);
			return;
		}
		if (entity.isShiftKeyDown()) {
			runOldJobAbility(entity, () -> Ability1OnKeyPressedProcedure.execute(world, x, y, z, entity));
			return;
		}
		runOldJobAbility(entity, () -> {
			Ability1OnKeyPressedProcedure.execute(world, x, y, z, entity);
			entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
				capability.firecharge = Math.max(45, capability.firecharge);
				capability.syncPlayerVariables(entity);
			});
			Ability1OnKeyReleasedProcedure.execute(world, x, y, z, entity);
		});
	}

	private static void castMonarchBeam(Entity entity) {
		if (CooldownManager.isOnCooldown(entity, "job_1")) {
			if (entity instanceof Player player && !player.level().isClientSide())
				player.displayClientMessage(Component.literal("Ability on cooldown!"), true);
			return;
		}
		entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
			capability.monarchbeam = true;
			capability.syncPlayerVariables(entity);
		});
		CooldownManager.set(entity, "job_1", 60);
		CooldownManager.set(entity, "mana_refresh", 60);
		SololevelingMod.queueServerWork(16, () -> entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
			capability.monarchbeam = false;
			capability.syncPlayerVariables(entity);
		}));
	}

	private static void runOldJobAbility(Entity entity, Runnable action) {
		entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
			boolean oldCombatMode = capability.combatmode;
			capability.combatmode = false;
			action.run();
			capability.combatmode = oldCombatMode;
			capability.syncPlayerVariables(entity);
		});
	}

	private static List<String> skillsForJob(int job) {
		return switch (job) {
			case 1 -> List.of(ARISE, SHADOW_SUMMON, DISMISS_SHADOWS, SHADOW_COMMAND, SHADOW_EXCHANGE, SHADOW_MANIFESTATION);
			case 2 -> List.of(FIRE_CHARGE, METEOR_RAIN, FIREFLIES);
			case 3 -> List.of(ICE_BALL, ICE_CHUNK, ICE_SPEAR, SNOW_SCREEN);
			case 4 -> List.of(MONARCH_BEAM, LIGHTNING_STORM, STORM_BURST);
			case 5 -> List.of(THOMAS_CAPTURE, THOMAS_POWER_SMASH, THOMAS_COLLAPSE, THOMAS_MANIFESTATION);
			default -> List.of();
		};
	}

	private static List<String> skillsForEntityJob(Entity entity, int job) {
		if (job != 1)
			return skillsForJob(job);
		List<String> skills = new ArrayList<>();
		skills.add(ARISE);
		skills.add(SHADOW_SUMMON);
		skills.add(DISMISS_SHADOWS);
		skills.add(SHADOW_COMMAND);
		if (DoesHaveExchangeProcedure.execute(entity))
			skills.add(SHADOW_EXCHANGE);
		if (DoesHaveShadowManifestationProcedure.execute(entity))
			skills.add(SHADOW_MANIFESTATION);
		return skills;
	}

	private static List<String> mergedJobSkillList(Entity entity, String plist, List<String> granted, boolean keepFormations) {
		LinkedHashSet<String> result = new LinkedHashSet<>();
		result.addAll(granted);
		for (String skill : parseSkillList(plist)) {
			if (!shouldRemoveJobOwnedSkill(skill, granted, keepFormations))
				result.add(skill);
		}
		if (keepFormations && entity instanceof Player player)
			result.addAll(ShadowMonarchManager.formationSkills(player));
		return new ArrayList<>(result);
	}

	private static boolean clearStaleEquippedSkills(SololevelingModVariables.PlayerVariables vars, List<String> granted, boolean keepFormations) {
		boolean changed = false;
		for (int slot = 1; slot <= 16; slot++) {
			String skill = SkillSlotHelper.getSlot(vars, slot);
			if (shouldRemoveJobOwnedSkill(skill, granted, keepFormations)) {
				SkillSlotHelper.setSlot(vars, slot, "");
				changed = true;
			}
		}
		if (shouldRemoveJobOwnedSkill(vars.PselectedPower, granted, keepFormations)) {
			vars.PselectedPower = "";
			changed = true;
		}
		return changed;
	}

	private static boolean shouldRemoveJobOwnedSkill(String skill, List<String> granted, boolean keepFormations) {
		String cleaned = cleanSkill(skill);
		if (cleaned.isEmpty())
			return false;
		if (ALL_JOB_SKILLS.contains(cleaned))
			return !granted.contains(cleaned);
		return ShadowMonarchManager.isFormationSkill(cleaned) && !keepFormations;
	}

	private static List<String> parseSkillList(String plist) {
		if (plist == null || plist.isBlank())
			return List.of();
		return Arrays.stream(plist.split(",")).map(JobSkillManager::cleanSkill).filter(skill -> !skill.isEmpty()).toList();
	}

	private static String cleanSkill(String item) {
		String skill = item == null ? "" : item.trim();
		if (skill.startsWith("."))
			skill = skill.substring(1);
		return skill;
	}

	private static String writeSkillList(List<String> skills) {
		StringBuilder builder = new StringBuilder();
		for (String skill : skills) {
			if (skill != null && !skill.isBlank())
				builder.append(skill).append(",");
		}
		return builder.toString();
	}
}
