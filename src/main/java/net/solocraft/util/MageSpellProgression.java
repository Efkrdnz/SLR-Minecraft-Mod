package net.solocraft.util;

import net.solocraft.SololevelingMod;
import net.solocraft.network.SololevelingModVariables;
import net.solocraft.procedures.SkillSlotHelper;

import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Mod.EventBusSubscriber(modid = SololevelingMod.MODID)
public final class MageSpellProgression {
	public static final String FIRE = "fire";
	public static final String BARRIER = "barrier";
	public static final String ARCANE = "arcane";

	private static final Set<String> LEGACY_FIRE_SKILLS = Set.of(
			"Fireball", "Fire Rain", "Heavy Flame", "Flame Tornado", "Flame Vortex");

	private static final List<List<String>> FIRE_SPELL_TIERS = List.of(
			List.of(),
			List.of(FireMageSpellManager.FLAME_WEAVING, FireMageSpellManager.IGNITION_ORB),
			List.of(FireMageSpellManager.INFERNO_LANCE, FireMageSpellManager.FLASHFIRE),
			List.of(FireMageSpellManager.CREMATION),
			List.of(FireMageSpellManager.FURNACE_DOMINION),
			List.of(FireMageSpellManager.HEAVENFALL));

	private static final List<List<String>> FIRE_EVALUATION_UNLOCKS = List.of(
			List.of(),
			List.of(FireMageSpellManager.FLAME_WEAVING, FireMageSpellManager.IGNITION_ORB),
			List.of(FireMageSpellManager.INFERNO_LANCE),
			List.of(FireMageSpellManager.CREMATION),
			List.of(FireMageSpellManager.FURNACE_DOMINION),
			List.of(FireMageSpellManager.HEAVENFALL));

	private static final List<List<String>> BARRIER_SPELL_TIERS = List.of(
			List.of(BarrierMageSpellManager.FRACTURE_BOLT),
			List.of(BarrierMageSpellManager.PRISM_RAMPART),
			List.of(BarrierMageSpellManager.REPULSION_FRAME),
			List.of(BarrierMageSpellManager.SEALING_PRISM),
			List.of(BarrierMageSpellManager.MIRROR_WARD),
			List.of(BarrierMageSpellManager.RESONANT_COLLAPSE,
					BarrierMageSpellManager.ABSOLUTE_BASTION));

	private static final List<List<String>> ARCANE_SPELL_TIERS = List.of(
			List.of(ArcaneMageSpellManager.AETHER_BOLT),
			List.of(ArcaneMageSpellManager.VECTOR_STEP),
			List.of(ArcaneMageSpellManager.POLARITY_SPHERE),
			List.of(ArcaneMageSpellManager.RUNIC_RELAY),
			List.of(ArcaneMageSpellManager.ASTRAL_ARSENAL),
			List.of(ArcaneMageSpellManager.DIMENSIONAL_REND,
					ArcaneMageSpellManager.CONVERGENCE));

	private MageSpellProgression() {
	}

	@SubscribeEvent
	public static void migrateExistingMage(PlayerEvent.PlayerLoggedInEvent event) {
		Entity player = event.getEntity();
		removeLegacyFireSkills(player);
		if (variables(player).Classes != 2.0D)
			return;
		if (specialization(player).isBlank())
			setSpecialization(player, FIRE, false);
		grantEvaluationSpells(player);
	}

	private static void removeLegacyFireSkills(Entity entity) {
		entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
			boolean changed = false;
			String cleanedList = cleanSkillList(capability.Plist);
			if (!Objects.equals(cleanedList, capability.Plist)) {
				capability.Plist = cleanedList;
				changed = true;
			}
			for (int slot = 1; slot <= 16; slot++) {
				if (LEGACY_FIRE_SKILLS.contains(SkillSlotHelper.getSlot(capability, slot))) {
					SkillSlotHelper.setSlot(capability, slot, "");
					changed = true;
				}
			}
			if (LEGACY_FIRE_SKILLS.contains(capability.PselectedPower)) {
				capability.PselectedPower = "";
				changed = true;
			}
			if (changed)
				capability.syncPlayerVariables(entity);
		});
	}

	private static String cleanSkillList(String skillList) {
		if (skillList == null || skillList.isBlank())
			return ".";
		String cleaned = skillList;
		for (String skill : LEGACY_FIRE_SKILLS)
			cleaned = cleaned.replace(skill + ",", "");
		return cleaned.isBlank() ? "." : cleaned;
	}

	public static String specialization(Entity entity) {
		String value = variables(entity).mageSpecialization;
		return value == null ? "" : value.trim().toLowerCase();
	}

	public static boolean isBarrierMage(Entity entity) {
		return BARRIER.equals(specialization(entity));
	}

	public static boolean isArcaneMage(Entity entity) {
		return ARCANE.equals(specialization(entity));
	}

	public static String displayName(Entity entity) {
		return switch (specialization(entity)) {
			case BARRIER -> "Barrier Mage";
			case ARCANE -> "Arcane Mage";
			default -> "Fire Mage";
		};
	}

	public static String assignRandomSpecialization(Entity entity) {
		String current = specialization(entity);
		if (FIRE.equals(current) || BARRIER.equals(current) || ARCANE.equals(current))
			return current;
		String selected = switch (entity.level().getRandom().nextInt(3)) {
			case 1 -> BARRIER;
			case 2 -> ARCANE;
			default -> FIRE;
		};
		setSpecialization(entity, selected, true);
		return selected;
	}

	public static void setSpecialization(Entity entity, String specialization, boolean notify) {
		if (entity == null || (!FIRE.equals(specialization) && !BARRIER.equals(specialization)
				&& !ARCANE.equals(specialization)))
			return;
		entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
			capability.mageSpecialization = specialization;
			capability.syncPlayerVariables(entity);
		});
		if (notify && entity instanceof ServerPlayer player) {
			boolean barrier = BARRIER.equals(specialization);
			boolean arcane = ARCANE.equals(specialization);
			SystemNotifications.showTitleUnder(player,
					barrier ? 0xFF5CE8FF : arcane ? 0xFF8A5CFF : 0xFFFF5A2A, 120,
					Component.literal("MAGE SPECIALIZATION")
							.withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
					Component.literal(barrier ? "Barrier Mage" : arcane ? "Arcane Mage" : "Fire Mage")
							.withStyle(barrier ? ChatFormatting.AQUA
									: arcane ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.RED,
									ChatFormatting.BOLD));
		}
	}

	public static int tierForRank(double hunterRank) {
		int rank = (int) Math.round(hunterRank);
		return Math.max(0, Math.min(5, rank - 1));
	}

	public static void grantStarterSpells(Entity entity) {
		if (entity == null)
			return;
		String type = assignRandomSpecialization(entity);
		String starter = switch (type) {
			case BARRIER -> BarrierMageSpellManager.FRACTURE_BOLT;
			case ARCANE -> ArcaneMageSpellManager.AETHER_BOLT;
			default -> FireMageSpellManager.FLAME_WEAVING;
		};
		unlockSkill(entity, starter, false);
	}

	public static void grantEvaluationSpells(Entity entity) {
		if (entity == null)
			return;
		String type = assignRandomSpecialization(entity);
		int tier = tierForRank(variables(entity).HunterRank);
		entity.getPersistentData().putInt("sl_mage_spell_tier", tier);
		if (BARRIER.equals(type)) {
			unlockSkill(entity, BarrierMageSpellManager.FRACTURE_BOLT, false);
			for (int currentTier = 1; currentTier <= tier; currentTier++) {
				for (String skill : BARRIER_SPELL_TIERS.get(currentTier))
					unlockSkill(entity, skill, false);
			}
			return;
		}
		if (ARCANE.equals(type)) {
			for (int currentTier = 0; currentTier <= tier; currentTier++) {
				for (String skill : ARCANE_SPELL_TIERS.get(currentTier))
					unlockSkill(entity, skill, false);
			}
			return;
		}
		for (int currentTier = 1; currentTier <= tier; currentTier++) {
			for (String skill : FIRE_EVALUATION_UNLOCKS.get(currentTier))
				unlockSkill(entity, skill, false);
		}
	}

	public static void grantMasterySkill(Entity entity) {
		if (entity == null)
			return;
		String type = assignRandomSpecialization(entity);
		int tier = tierForRank(variables(entity).HunterRank);
		entity.getPersistentData().putInt("sl_mage_spell_tier", tier);

		List<String> missing = new ArrayList<>();
		if (BARRIER.equals(type)) {
			for (int currentTier = 0; currentTier <= tier; currentTier++) {
				for (String skill : BARRIER_SPELL_TIERS.get(currentTier)) {
					if (!hasSkill(entity, skill))
						missing.add(skill);
				}
			}
		} else if (ARCANE.equals(type)) {
			for (int currentTier = 0; currentTier <= tier; currentTier++) {
				for (String skill : ARCANE_SPELL_TIERS.get(currentTier)) {
					if (!hasSkill(entity, skill))
						missing.add(skill);
				}
			}
		} else {
			for (int currentTier = 1; currentTier <= tier; currentTier++) {
				for (String skill : FIRE_SPELL_TIERS.get(currentTier)) {
					if (!hasSkill(entity, skill))
						missing.add(skill);
				}
			}
		}
		if (!missing.isEmpty())
			unlockSkill(entity, missing.get(entity.level().getRandom().nextInt(missing.size())), true);
	}

	public static boolean hasSkill(Entity entity, String skill) {
		String list = variables(entity).Plist;
		if (list == null || list.isBlank())
			return false;
		for (String entry : list.split(",")) {
			String normalized = entry.trim();
			if (normalized.startsWith("."))
				normalized = normalized.substring(1);
			if (skill.equals(normalized))
				return true;
		}
		return false;
	}

	public static boolean unlockSkill(Entity entity, String skill, boolean notify) {
		if (skill == null || skill.isBlank() || hasSkill(entity, skill))
			return false;
		entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
			capability.Plist = capability.Plist + skill + ",";
			capability.syncPlayerVariables(entity);
		});
		if (notify && entity instanceof Player player && !player.level().isClientSide())
			player.displayClientMessage(Component.literal("Gained skill: " + skill), false);
		return true;
	}

	private static SololevelingModVariables.PlayerVariables variables(Entity entity) {
		return entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
				.orElse(new SololevelingModVariables.PlayerVariables());
	}
}
