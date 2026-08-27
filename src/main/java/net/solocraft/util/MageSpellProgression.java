package net.solocraft.util;

import net.solocraft.SololevelingMod;
import net.solocraft.network.SololevelingModVariables;
import net.solocraft.procedures.SkillSlotHelper;

import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@EventBusSubscriber(modid = SololevelingMod.MODID)
public final class MageSpellProgression {
	public static final String FIRE = "fire";
	public static final String BARRIER = "barrier";
	public static final String ARCANE = "arcane";
	public static final String STORM = "storm";
	public static final String CURSE = "curse";

	private static final Set<String> LEGACY_FIRE_SKILLS = Set.of(
			"Fireball", "Fire Rain", "Heavy Flame", "Flame Tornado", "Flame Vortex");
	private static final Set<String> RETIRED_UNBOUND_SKILLS = Set.of(
			"Magic Missiles", "Lightball", "Light Ball", "Water Slash", "Light Golem",
			"Curse Sphere", "Curse Smoke", "Curse Chains");
	private static final Set<String> RETIRED_RUNESTONE_IDS = Set.of(
			"runestone_magic_missiles", "runestone_lightball", "runestone_waterslash",
			"runestone_light_golem", "runestone_curse_sphere", "runestone_cursed_smoke",
			"runestone_curse_chains");
	private static final String RUNESTONE_SKILL_PREFIX = "slr_runestone_skill_";

	private static final List<List<String>> FIRE_SPELL_TIERS = List.of(
			List.of(FireMageSpellManager.FLAME_WEAVING),
			List.of(FireMageSpellManager.IGNITION_ORB),
			List.of(FireMageSpellManager.INFERNO_LANCE, FireMageSpellManager.FLASHFIRE),
			List.of(FireMageSpellManager.CREMATION),
			List.of(FireMageSpellManager.FURNACE_DOMINION),
			List.of(FireMageSpellManager.HEAVENFALL));

	private static final List<List<String>> FIRE_EVALUATION_UNLOCKS = List.of(
			List.of(FireMageSpellManager.FLAME_WEAVING),
			List.of(FireMageSpellManager.IGNITION_ORB),
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

	private static final List<List<String>> STORM_SPELL_TIERS = List.of(
			List.of(StormMageSpellManager.STATIC_NEEDLE),
			List.of(StormMageSpellManager.SLIPSTREAM, StormMageSpellManager.THUNDERCLAP),
			List.of(StormMageSpellManager.LIGHTNING_ROD),
			List.of(StormMageSpellManager.CHAIN_LIGHTNING),
			List.of(StormMageSpellManager.THUNDERHEAD),
			List.of(StormMageSpellManager.SKYBREAKER,
					StormMageSpellManager.TEMPEST_INCARNATE));

	private static final List<List<String>> STORM_EVALUATION_UNLOCKS = List.of(
			List.of(StormMageSpellManager.STATIC_NEEDLE),
			List.of(StormMageSpellManager.SLIPSTREAM),
			List.of(StormMageSpellManager.LIGHTNING_ROD),
			List.of(StormMageSpellManager.CHAIN_LIGHTNING),
			List.of(StormMageSpellManager.THUNDERHEAD),
			List.of(StormMageSpellManager.SKYBREAKER));

	// Curse Mage learns one delivery per tier. The wheel's curse roster unlocks
	// on the same rank ladder, in CurseType order.
	private static final List<List<String>> CURSE_SPELL_TIERS = List.of(
			List.of(CurseMageSpellManager.HEX_BOLT),
			List.of(),
			List.of(CurseMageSpellManager.MALEFIC_BURST),
			List.of(CurseMageSpellManager.CREEPING_MIASMA),
			List.of(CurseMageSpellManager.VECTOR_OF_RUIN),
			List.of(CurseMageSpellManager.CULLING));

	private MageSpellProgression() {
	}

	@SubscribeEvent
	public static void migrateExistingMage(PlayerEvent.PlayerLoggedInEvent event) {
		reconcilePlayerSkills(event.getEntity());
	}

	/**
	 * Reconciles native specialization ownership and Baran's inherited Storm
	 * techniques. This is safe to call repeatedly from vessel/login migration
	 * paths and never changes a player's Intelligence or permanent output stage.
	 */
	public static void reconcilePlayerSkills(Entity entity) {
		if (entity == null)
			return;
		removeInvalidMageSkills(entity);
		if (variables(entity).Classes == 2.0D) {
			String type = specialization(entity);
			if (type.isBlank())
				type = FIRE;
			SololevelingModVariables.PlayerVariables vars = variables(entity);
			if (!type.equals(normalizeStyle(vars.classStyle))
					|| !type.equals(normalizeStyle(vars.mageSpecialization)))
				setSpecialization(entity, type, false);
			grantEvaluationSpells(entity);
		}
		grantBaranStormInheritance(entity);
	}

	/**
	 * Lightweight vessel-path reconciliation. It removes borrowed Storm skills
	 * after a vessel reset, while preserving all eight for a native Storm Mage.
	 */
	public static void reconcileVesselInheritance(Entity entity) {
		if (entity == null)
			return;
		removeInvalidMageSkills(entity);
		grantBaranStormInheritance(entity);
	}

	private static void grantBaranStormInheritance(Entity entity) {
		if (WhiteFlameMonarchManager.isWhiteFlameVessel(entity)) {
			for (String skill : StormMageSpellManager.BARAN_CROSSOVER_SKILL_ORDER)
				unlockSkill(entity, skill, false);
		}
	}

	private static void removeInvalidMageSkills(Entity entity) {
		entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
			boolean changed = false;
			String cleanedList = cleanSkillList(entity, capability.Plist);
			if (!Objects.equals(cleanedList, capability.Plist)) {
				capability.Plist = cleanedList;
				changed = true;
			}
			for (int slot = 1; slot <= 16; slot++) {
				if (isInvalidMageSkill(entity, SkillSlotHelper.getSlot(capability, slot))) {
					SkillSlotHelper.setSlot(capability, slot, "");
					changed = true;
				}
			}
			if (isInvalidMageSkill(entity, capability.PselectedPower)) {
				capability.PselectedPower = "";
				changed = true;
			}
			if (changed)
				capability.syncPlayerVariables(entity);
		});
	}

	private static String cleanSkillList(Entity entity, String skillList) {
		if (skillList == null || skillList.isBlank())
			return ".";
		StringBuilder cleaned = new StringBuilder(".");
		for (String raw : skillList.split(",")) {
			String skill = raw == null ? "" : raw.trim();
			while (skill.startsWith("."))
				skill = skill.substring(1);
			if (!skill.isBlank() && !isInvalidMageSkill(entity, skill))
				cleaned.append(skill).append(',');
		}
		return cleaned.toString();
	}

	private static boolean isInvalidMageSkill(Entity entity, String skill) {
		if (skill == null || skill.isBlank())
			return false;
		if (LEGACY_FIRE_SKILLS.contains(skill) || RETIRED_UNBOUND_SKILLS.contains(skill))
			return true;
		String required = requiredSpecialization(skill);
		return required != null && !hasNativeAccess(entity, skill)
				&& !wasLearnedFromRunestone(entity, skill);
	}

	public static String specialization(Entity entity) {
		SololevelingModVariables.PlayerVariables vars = variables(entity);
		String generic = normalizeStyle(vars.classStyle);
		if (ClassStyleRules.styleId(ClassStyleRules.MAGE_CLASS_ID,
				generic) > 0)
			return generic;
		String legacy = normalizeStyle(vars.mageSpecialization);
		return ClassStyleRules.styleId(ClassStyleRules.MAGE_CLASS_ID,
				legacy) > 0 ? legacy : "";
	}

	public static boolean isBarrierMage(Entity entity) {
		return BARRIER.equals(specialization(entity));
	}

	public static boolean isArcaneMage(Entity entity) {
		return ARCANE.equals(specialization(entity));
	}

	public static boolean isStormMage(Entity entity) {
		return STORM.equals(specialization(entity));
	}

	public static boolean isCurseMage(Entity entity) {
		return CURSE.equals(specialization(entity));
	}

	public static String requiredSpecialization(String skill) {
		if (FireMageSpellManager.isFireSkill(skill))
			return FIRE;
		if (BarrierMageSpellManager.isBarrierSkill(skill))
			return BARRIER;
		if (ArcaneMageSpellManager.isArcaneSkill(skill))
			return ARCANE;
		if (StormMageSpellManager.isStormSkill(skill))
			return STORM;
		if (CurseMageSpellManager.isCurseSkill(skill))
			return CURSE;
		return null;
	}

	private static boolean hasNativeAccess(Entity entity, String skill) {
		String required = requiredSpecialization(skill);
		if (entity == null || required == null)
			return false;
		if (STORM.equals(required)
				&& StormMageSpellManager.BARAN_CROSSOVER_SKILLS.contains(skill)
				&& WhiteFlameMonarchManager.isWhiteFlameVessel(entity))
			return true;
		return variables(entity).Classes == 2.0D && required.equals(specialization(entity));
	}

	public static boolean canCastLearnedSkill(Entity entity, String skill) {
		return entity != null && requiredSpecialization(skill) != null && hasSkill(entity, skill);
	}

	public static boolean isRetiredRunestoneId(String registryId) {
		if (registryId == null)
			return false;
		String path = registryId.trim().toLowerCase();
		int separator = path.indexOf(':');
		if (separator >= 0)
			path = path.substring(separator + 1);
		return RETIRED_RUNESTONE_IDS.contains(path);
	}

	public static String displayName(Entity entity) {
		return switch (specialization(entity)) {
			case BARRIER -> "Barrier Mage";
			case ARCANE -> "Arcane Mage";
			case STORM -> "Storm Mage";
			case CURSE -> "Curse Mage";
			default -> "Fire Mage";
		};
	}

	public static String assignRandomSpecialization(Entity entity) {
		String current = specialization(entity);
		if (FIRE.equals(current) || BARRIER.equals(current) || ARCANE.equals(current)
				|| STORM.equals(current) || CURSE.equals(current))
			return current;
		String selected = switch (entity.level().getRandom().nextInt(5)) {
			case 1 -> BARRIER;
			case 2 -> ARCANE;
			case 3 -> STORM;
			case 4 -> CURSE;
			default -> FIRE;
		};
		setSpecialization(entity, selected, true);
		return selected;
	}

	public static void setSpecialization(Entity entity, String specialization, boolean notify) {
		String selected = normalizeStyle(specialization);
		if (entity == null || ClassStyleRules.styleId(
				ClassStyleRules.MAGE_CLASS_ID, selected) == 0)
			return;
		entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
			capability.classStyle = selected;
			capability.mageSpecialization = selected;
			capability.syncPlayerVariables(entity);
		});
		if (notify && entity instanceof ServerPlayer player) {
			boolean barrier = BARRIER.equals(selected);
			boolean arcane = ARCANE.equals(selected);
			boolean storm = STORM.equals(selected);
			SystemNotifications.showTitleUnder(player,
					barrier ? 0xFF5CE8FF : arcane ? 0xFF8A5CFF : storm ? 0xFFFFD45A : 0xFFFF5A2A, 120,
					Component.literal("MAGE SPECIALIZATION")
							.withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
					Component.literal(barrier ? "Barrier Mage" : arcane ? "Arcane Mage"
							: storm ? "Storm Mage" : "Fire Mage")
							.withStyle(barrier ? ChatFormatting.AQUA
									: arcane ? ChatFormatting.LIGHT_PURPLE
									: storm ? ChatFormatting.YELLOW : ChatFormatting.RED,
									ChatFormatting.BOLD));
		}
	}

	private static String normalizeStyle(String value) {
		return value == null ? "" : value.trim().toLowerCase();
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
			case STORM -> StormMageSpellManager.STATIC_NEEDLE;
			case CURSE -> CurseMageSpellManager.HEX_BOLT;
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
		if (STORM.equals(type)) {
			for (int currentTier = 0; currentTier <= tier; currentTier++) {
				for (String skill : STORM_EVALUATION_UNLOCKS.get(currentTier))
					unlockSkill(entity, skill, false);
			}
			return;
		}
		if (CURSE.equals(type)) {
			for (int currentTier = 0; currentTier <= tier; currentTier++) {
				for (String skill : CURSE_SPELL_TIERS.get(currentTier))
					unlockSkill(entity, skill, false);
			}
			return;
		}
		for (int currentTier = 0; currentTier <= tier; currentTier++) {
			for (String skill : FIRE_EVALUATION_UNLOCKS.get(currentTier))
				unlockSkill(entity, skill, false);
		}
	}

	public static void grantMasterySkill(Entity entity) {
		if (entity == null || variables(entity).Classes != 2.0D)
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
		} else if (STORM.equals(type)) {
			for (int currentTier = 0; currentTier <= tier; currentTier++) {
				for (String skill : STORM_SPELL_TIERS.get(currentTier)) {
					if (!hasSkill(entity, skill))
						missing.add(skill);
				}
			}
		} else if (CURSE.equals(type)) {
			for (int currentTier = 0; currentTier <= tier; currentTier++) {
				for (String skill : CURSE_SPELL_TIERS.get(currentTier)) {
					if (!hasSkill(entity, skill))
						missing.add(skill);
				}
			}
		} else {
			for (int currentTier = 0; currentTier <= tier; currentTier++) {
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
		if (entity == null || skill == null || skill.isBlank() || hasSkill(entity, skill))
			return false;
		entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
			capability.Plist = capability.Plist + skill + ",";
			capability.syncPlayerVariables(entity);
		});
		grantCurseWheelCompanion(entity, skill);
		if (notify && entity instanceof Player player && !player.level().isClientSide())
			player.displayClientMessage(Component.literal("Gained skill: " + skill), false);
		return true;
	}

	/**
	 * Curse Weave is infrastructure, not a reward.
	 *
	 * <p>Every Curse Mage delivery casts whatever the wheel has armed, so owning a
	 * delivery without the wheel would leave a player permanently stuck on the
	 * starter curse. It arrives automatically with the first curse ability from
	 * any source -- starter grant, evaluation, mastery roll or runestone -- and is
	 * therefore deliberately not obtainable on its own.
	 */
	private static void grantCurseWheelCompanion(Entity entity, String skill) {
		if (!CurseMageSpellManager.isCurseSkill(skill)
				|| CurseMageSpellManager.CURSE_WEAVE.equals(skill))
			return;
		unlockSkill(entity, CurseMageSpellManager.CURSE_WEAVE, false);
	}

	/**
	 * Runestones are universal unlock items. The marker preserves that permanent
	 * ownership when a player later changes Hunter class or Mage specialization.
	 */
	public static boolean unlockFromRunestone(Entity entity, String skill) {
		if (!unlockSkill(entity, skill, false))
			return false;
		runestoneData(entity).putBoolean(runestoneSkillKey(skill), true);
		// The companion wheel has to inherit the same permanence, or a non-Curse
		// Mage who learned a delivery from a runestone would keep the delivery and
		// lose the wheel on their next login.
		if (CurseMageSpellManager.isCurseSkill(skill)
				&& !CurseMageSpellManager.CURSE_WEAVE.equals(skill))
			runestoneData(entity).putBoolean(
					runestoneSkillKey(CurseMageSpellManager.CURSE_WEAVE), true);
		return true;
	}

	private static boolean wasLearnedFromRunestone(Entity entity, String skill) {
		if (entity == null)
			return false;
		String key = runestoneSkillKey(skill);
		return runestoneData(entity).getBoolean(key)
				|| entity.getPersistentData().getBoolean(key);
	}

	private static String runestoneSkillKey(String skill) {
		StringBuilder key = new StringBuilder(RUNESTONE_SKILL_PREFIX);
		for (int index = 0; index < skill.length(); index++) {
			char character = Character.toLowerCase(skill.charAt(index));
			key.append(Character.isLetterOrDigit(character) ? character : '_');
		}
		return key.toString();
	}

	private static CompoundTag runestoneData(Entity entity) {
		CompoundTag entityData = entity.getPersistentData();
		if (!(entity instanceof Player))
			return entityData;
		CompoundTag persisted = entityData.getCompound(Player.PERSISTED_NBT_TAG);
		if (!entityData.contains(Player.PERSISTED_NBT_TAG))
			entityData.put(Player.PERSISTED_NBT_TAG, persisted);
		return persisted;
	}

	private static SololevelingModVariables.PlayerVariables variables(Entity entity) {
		return entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
				.orElse(new SololevelingModVariables.PlayerVariables());
	}
}
