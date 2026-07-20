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
	public static final String ICE_SPEAR = "Ice Spear";
	public static final String FLASH_FREEZE = FrostMonarchManager.FLASH_FREEZE;
	public static final String FROZEN_PATH = FrostMonarchManager.FROZEN_PATH;
	public static final String FROST_COUNTER = FrostMonarchManager.FROST_COUNTER;
	public static final String ABSOLUTE_ZERO = FrostMonarchManager.ABSOLUTE_ZERO;
	public static final String FROST_SPIRITUALIZATION = FrostMonarchManager.SPIRITUALIZATION;
	public static final String MONARCH_BEAM = "Monarch Beam";
	public static final String LIGHTNING_STORM = "Lightning Storm";
	public static final String STORM_BURST = "Storm Burst";
	public static final String LIGHTNING_BREATH = WhiteFlameMonarchManager.LIGHTNING_BREATH;
	public static final String HELLSTORM_DOMINION = WhiteFlameMonarchManager.HELLSTORM_DOMINION;
	public static final String RADIRU_BLOOD_SPEAR = WhiteFlameMonarchManager.RADIRU_BLOOD_SPEAR;
	public static final String DOPPELGANGER = WhiteFlameMonarchManager.DOPPELGANGER;
	public static final String HELLS_ARMY = WhiteFlameMonarchManager.HELLS_ARMY;
	public static final String WHITE_FLAME_SPIRITUALIZATION = WhiteFlameMonarchManager.SPIRITUALIZATION;
	public static final String THOMAS_MANIFESTATION = "Spiritual Body Manifestation";
	public static final String THOMAS_CAPTURE = GoliathCombatManager.CAPTURE;
	public static final String THOMAS_POWER_SMASH = GoliathCombatManager.POWER_SMASH;
	public static final String THOMAS_COLLAPSE = GoliathCombatManager.COLLAPSE;
	public static final String LIU_HEAVENLY_COUNTER = LiuZhigangCombatManager.HEAVENLY_COUNTER;
	public static final String LIU_GOLDEN_DRAGON_DANCE = LiuZhigangCombatManager.GOLDEN_DRAGON_DANCE;
	public static final String LIU_SOVEREIGN_SWORD_DOMAIN = LiuZhigangCombatManager.SOVEREIGN_SWORD_DOMAIN;
	public static final String LIU_MANIFESTATION = LiuZhigangCombatManager.DRAGON_SWORD_MANIFESTATION;
	public static final String BEAST_CLAW_RIFT = BeastMonarchManager.CLAW_RIFT;
	public static final String BEAST_RUBBLE_JAW = BeastMonarchManager.RUBBLE_JAW;
	public static final String BEAST_KINGS_MAUL = BeastMonarchManager.KINGS_MAUL;
	public static final String BEAST_RECONSTITUTION = BeastMonarchManager.FERAL_RECONSTITUTION;
	public static final String BEAST_WHITE_FANG = BeastMonarchManager.WHITE_FANG_SOVEREIGN;

	private static final String LAST_SYNCED_JOB = "sololeveling:last_synced_job_skills";
	private static final String RETIRED_KINGS_VERDICT = "King's Verdict";
	private static final List<String> WHITE_FLAME_SKILLS = List.of(
			LIGHTNING_BREATH, HELLSTORM_DOMINION, RADIRU_BLOOD_SPEAR,
			DOPPELGANGER, HELLS_ARMY, WHITE_FLAME_SPIRITUALIZATION);
	private static final List<String> LIU_SKILLS = List.of(
			LIU_HEAVENLY_COUNTER, LIU_GOLDEN_DRAGON_DANCE,
			LIU_SOVEREIGN_SWORD_DOMAIN, LIU_MANIFESTATION);
	private static final List<String> FROST_SKILLS = List.of(
			ICE_SPEAR, FLASH_FREEZE, FROZEN_PATH,
			FROST_COUNTER, ABSOLUTE_ZERO, FROST_SPIRITUALIZATION);
	private static final List<String> BEAST_SKILLS = List.of(
			BEAST_CLAW_RIFT, BEAST_RUBBLE_JAW, BEAST_KINGS_MAUL,
			BEAST_RECONSTITUTION, BEAST_WHITE_FANG);

	private static final List<String> ALL_JOB_SKILLS = List.of(
			ARISE, SHADOW_SUMMON, DISMISS_SHADOWS, SHADOW_COMMAND, SHADOW_EXCHANGE, SHADOW_MANIFESTATION,
			FIRE_CHARGE, METEOR_RAIN, FIREFLIES,
			"Ice Ball", "Ice Chunk", "Snow Screen", "Stillness Decree", "Pale Causeway",
			"Winter Remembers", "Whiteout Procession",
			ICE_SPEAR, FLASH_FREEZE, FROZEN_PATH, FROST_COUNTER,
			ABSOLUTE_ZERO, FROST_SPIRITUALIZATION,
			THOMAS_CAPTURE, THOMAS_POWER_SMASH, THOMAS_COLLAPSE, THOMAS_MANIFESTATION,
			LIU_HEAVENLY_COUNTER, LIU_GOLDEN_DRAGON_DANCE, LIU_SOVEREIGN_SWORD_DOMAIN, LIU_MANIFESTATION,
			MONARCH_BEAM, LIGHTNING_STORM, STORM_BURST,
			LIGHTNING_BREATH, HELLSTORM_DOMINION, RETIRED_KINGS_VERDICT, RADIRU_BLOOD_SPEAR,
			DOPPELGANGER, HELLS_ARMY, WHITE_FLAME_SPIRITUALIZATION,
			BEAST_CLAW_RIFT, BEAST_RUBBLE_JAW, BEAST_KINGS_MAUL,
			BEAST_RECONSTITUTION, BEAST_WHITE_FANG);

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

	public static boolean isWhiteFlameSkill(String skill) {
		return WHITE_FLAME_SKILLS.contains(skill);
	}

	public static boolean isLiuSkill(String skill) {
		return LIU_SKILLS.contains(skill);
	}

	public static boolean isFrostSkill(String skill) {
		return FROST_SKILLS.contains(skill);
	}

	public static boolean isBeastSkill(String skill) {
		return BEAST_SKILLS.contains(skill);
	}

	public static int skillColor(String skill) {
		if (List.of(ARISE, SHADOW_SUMMON, DISMISS_SHADOWS, SHADOW_COMMAND, SHADOW_EXCHANGE, SHADOW_MANIFESTATION).contains(skill))
			return 0xB965FF;
		if (List.of(FIRE_CHARGE, METEOR_RAIN, FIREFLIES).contains(skill))
			return 0xFF5A32;
		if (isFrostSkill(skill))
			return 0x6FE8FF;
		if (List.of(THOMAS_CAPTURE, THOMAS_POWER_SMASH, THOMAS_COLLAPSE, THOMAS_MANIFESTATION).contains(skill))
			return 0xFFD35A;
		if (isLiuSkill(skill))
			return 0xFFD34E;
		if (isWhiteFlameSkill(skill))
			return 0xFFFFFF;
		if (isBeastSkill(skill))
			return 0xFF8A24;
		if (List.of(MONARCH_BEAM, LIGHTNING_STORM, STORM_BURST).contains(skill))
			return 0xFFE38A;
		return 0xFFFFFF;
	}

	public static List<Component> tooltip(Entity entity, String skill) {
		if (FireMageSpellManager.isFireSkill(skill))
			return FireMageSpellManager.tooltip(entity, skill);
		if (BarrierMageSpellManager.isBarrierSkill(skill))
			return BarrierMageSpellManager.tooltip(entity, skill);
		if (ArcaneMageSpellManager.isArcaneSkill(skill))
			return ArcaneMageSpellManager.tooltip(entity, skill);
		if (isFrostSkill(skill))
			return frostTooltip(entity, skill);
		if (isWhiteFlameSkill(skill))
			return whiteFlameTooltip(entity, skill);
		if (isLiuSkill(skill))
			return liuTooltip(entity, skill);
		if (isBeastSkill(skill))
			return beastTooltip(entity, skill);
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

	private static List<Component> frostTooltip(Entity entity, String skill) {
		boolean manifested = FrostMonarchManager.isSpiritualized(entity);
		ArrayList<Component> lines = new ArrayList<>();
		lines.add(Component.literal(skill).withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));
		switch (skill) {
			case ICE_SPEAR -> {
				lines.add(Component.literal("Throw the Ice Spear, then cast again to recall it.").withStyle(ChatFormatting.GRAY));
				lines.add(Component.literal("Both passes build Frostbite and shatter frozen enemies.").withStyle(ChatFormatting.AQUA));
				lines.add(Component.literal("Sneak-cast to receive the existing Ice Spear item in your inventory.").withStyle(ChatFormatting.DARK_AQUA));
				lines.add(Component.literal((manifested ? "300" : "260") + " MP throw | 5.5s cooldown | Sneak: 100 MP, 3s").withStyle(ChatFormatting.YELLOW));
			}
			case FLASH_FREEZE -> {
				lines.add(Component.literal("Damage enemies in a forward cone and inflict heavy Frostbite.").withStyle(ChatFormatting.GRAY));
				lines.add(Component.literal("Already-frozen enemies shatter into damaging ice fragments.").withStyle(ChatFormatting.AQUA));
				lines.add(Component.literal((manifested ? "12 blocks | 320 MP" : "9 blocks | 280 MP") + " | 7s cooldown").withStyle(ChatFormatting.YELLOW));
			}
			case FROZEN_PATH -> {
				lines.add(Component.literal("Hold to steer a piercing ice current that forms a two-block-wide road.").withStyle(ChatFormatting.GRAY));
				lines.add(Component.literal("Sneak before casting to ride it; release to rupture the current.").withStyle(ChatFormatting.AQUA));
				lines.add(Component.literal((manifested ? "210" : "180") + " MP + upkeep | 7-8s cooldown after release").withStyle(ChatFormatting.YELLOW));
			}
			case FROST_COUNTER -> {
				lines.add(Component.literal("Parry the next hit and retaliate with a freezing rupture.").withStyle(ChatFormatting.GRAY));
				lines.add(Component.literal("The counter deals damage, inflicts Frostbite, and can shatter.").withStyle(ChatFormatting.AQUA));
				lines.add(Component.literal(manifested
						? "2s window | 75% reduction | 280 MP | 9s cooldown"
						: "1.6s window | 60% reduction | 240 MP | 9s cooldown").withStyle(ChatFormatting.YELLOW));
			}
			case ABSOLUTE_ZERO -> {
				lines.add(Component.literal("Create a damaging freezing field that follows you.").withStyle(ChatFormatting.GRAY));
				lines.add(Component.literal("Recast to detonate chilled and frozen enemies at once.").withStyle(ChatFormatting.AQUA));
				lines.add(Component.literal(manifested
						? "10 blocks for 10s | 700 MP | 20s cooldown"
						: "8 blocks for 8s | 600 MP | 20s cooldown").withStyle(ChatFormatting.YELLOW));
			}
			case FROST_SPIRITUALIZATION -> {
				lines.add(Component.literal("Toggle the Frost Monarch's enhanced spiritual aura.").withStyle(ChatFormatting.GRAY));
				lines.add(Component.literal("Enhances Frost skills until disabled or MP is depleted.").withStyle(ChatFormatting.AQUA));
				lines.add(Component.literal("0 MP activation | 14 MP per second | No cooldown").withStyle(ChatFormatting.YELLOW));
			}
			default -> {
			}
		}
		return lines;
	}

	private static List<Component> liuTooltip(Entity entity, String skill) {
		boolean manifested = LiuZhigangCombatManager.isManifested(entity);
		ArrayList<Component> lines = new ArrayList<>();
		lines.add(Component.literal(skill).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
		switch (skill) {
			case LIU_HEAVENLY_COUNTER -> {
				lines.add(Component.literal("Enter a razor-thin counter window and turn force back on its source.").withStyle(ChatFormatting.GRAY));
				lines.add(Component.literal(manifested
						? "Manifested: two counters; success advances the next beam charge tier."
						: "A successful counter empowers the next charged sword beam.").withStyle(ChatFormatting.YELLOW));
			}
			case LIU_GOLDEN_DRAGON_DANCE -> {
				lines.add(Component.literal("Cross the battlefield in a chained sequence of sovereign cuts.").withStyle(ChatFormatting.GRAY));
				lines.add(Component.literal(manifested
						? "Manifested: hunts up to ten targets with stronger Dragon Sword cuts."
						: "With no target, releases three advancing sword waves.").withStyle(ChatFormatting.YELLOW));
			}
			case LIU_SOVEREIGN_SWORD_DOMAIN -> {
				lines.add(Component.literal("Claim the area as a field of suspended, unseen sword paths.").withStyle(ChatFormatting.GRAY));
				lines.add(Component.literal("Attacks echo, projectiles are repelled, and every mark ruptures together.").withStyle(ChatFormatting.YELLOW));
			}
			case LIU_MANIFESTATION -> {
				lines.add(Component.literal("Manifest the twin Dragon Swords granted by a Ruler's power.").withStyle(ChatFormatting.GRAY));
				lines.add(Component.literal(manifested
						? "ACTIVE: original hand items are safely sealed until release."
						: "Transforms Liu's skills and preserves both held items exactly.").withStyle(ChatFormatting.YELLOW));
			}
			default -> {
			}
		}
		return lines;
	}

	private static List<Component> beastTooltip(Entity entity, String skill) {
		boolean sovereign = BeastMonarchManager.isWhiteFangSovereign(entity);
		ArrayList<Component> lines = new ArrayList<>();
		lines.add(Component.literal(skill).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
		switch (skill) {
			case BEAST_CLAW_RIFT -> {
				lines.add(Component.literal("Rip forward through space and claw every enemy along the route.")
						.withStyle(ChatFormatting.GRAY));
				lines.add(Component.literal(sovereign
						? "Sovereign: farther dash, wider path, and stronger cuts."
						: "The dash stops safely at terrain and damages every enemy crossed.")
						.withStyle(ChatFormatting.GOLD));
				lines.add(Component.literal("220 base MP | 6s cooldown").withStyle(ChatFormatting.YELLOW));
			}
			case BEAST_RUBBLE_JAW -> {
				lines.add(Component.literal("Erupt the targeted area in a violent jaw of shattered ground.")
						.withStyle(ChatFormatting.GRAY));
				lines.add(Component.literal(sovereign
						? "Sovereign: larger eruption, stronger launch, and more damage."
						: "Deals immediate area damage and launches enemies upward.")
						.withStyle(ChatFormatting.GOLD));
				lines.add(Component.literal("300 base MP | 8.5s cooldown").withStyle(ChatFormatting.YELLOW));
			}
			case BEAST_KINGS_MAUL -> {
				lines.add(Component.literal("Lunge at the enemy in front of you and deliver a crushing maul.")
						.withStyle(ChatFormatting.GRAY));
				lines.add(Component.literal(sovereign
						? "Sovereign: longer lunge, heavier hit, and stronger movement lock."
						: "Automatically catches a visible target in your forward cone.")
						.withStyle(ChatFormatting.GOLD));
				lines.add(Component.literal("380 base MP | 10s cooldown").withStyle(ChatFormatting.YELLOW));
			}
			case BEAST_RECONSTITUTION -> {
				lines.add(Component.literal("Instantly regrow missing health and form a temporary absorption hide.")
						.withStyle(ChatFormatting.GRAY));
				lines.add(Component.literal(sovereign
						? "Sovereign: stronger healing and a thicker, longer-lasting hide."
						: "No channel, wound requirement, Quarry, or Hunt cost.")
						.withStyle(ChatFormatting.GOLD));
				lines.add(Component.literal("260 base MP | 12s cooldown").withStyle(ChatFormatting.YELLOW));
			}
			case BEAST_WHITE_FANG -> {
				lines.add(Component.literal("Instantly enter the White Fang's close-combat manifestation.")
						.withStyle(ChatFormatting.GRAY));
				lines.add(Component.literal("Enhances every Beast skill, adds a fourth claw beat, and increases movement speed.")
						.withStyle(ChatFormatting.GOLD));
				lines.add(Component.literal("600 base MP | 20s duration | Press again to end early").withStyle(ChatFormatting.YELLOW));
			}
			default -> {
			}
		}
		return lines;
	}

	private static List<Component> whiteFlameTooltip(Entity entity, String skill) {
		boolean manifested = WhiteFlameMonarchManager.isSpiritualized(entity);
		ArrayList<Component> lines = new ArrayList<>();
		lines.add(Component.literal(skill).withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD));
		switch (skill) {
			case LIGHTNING_BREATH -> {
				lines.add(Component.literal("Exhale a steerable stream of white lightning-fire.").withStyle(ChatFormatting.GRAY));
				lines.add(Component.literal("Repeated hits brand enemies for Hellstorm Dominion.").withStyle(ChatFormatting.AQUA));
				lines.add(Component.literal(manifested ? "Manifested: wider, longer, faster, and more efficient." : "220 MP | 3.9s cooldown").withStyle(ChatFormatting.YELLOW));
			}
			case HELLSTORM_DOMINION -> {
				lines.add(Component.literal("Claim a moving domain that hunts nearby enemies with lightning.").withStyle(ChatFormatting.GRAY));
				lines.add(Component.literal("Branded enemies are prioritized and suffer amplified strikes.").withStyle(ChatFormatting.AQUA));
				lines.add(Component.literal(manifested ? "Manifested: larger domain, denser storm, longer reign." : "850 MP | 19.5s cooldown").withStyle(ChatFormatting.YELLOW));
			}
			case RADIRU_BLOOD_SPEAR -> {
				lines.add(Component.literal("Hurl Radiru's royal spear through an enemy formation.").withStyle(ChatFormatting.GRAY));
				lines.add(Component.literal("Pierces targets, ignites them, and carves in a royal brand.").withStyle(ChatFormatting.AQUA));
				lines.add(Component.literal(manifested ? "Manifested: seven-target pierce with greater velocity." : "300 MP | 4.8s cooldown").withStyle(ChatFormatting.YELLOW));
			}
			case DOPPELGANGER -> {
				lines.add(Component.literal("Create three false selves that intercept incoming attacks.").withStyle(ChatFormatting.GRAY));
				lines.add(Component.literal("Each broken echo dodges, repositions, and retaliates.").withStyle(ChatFormatting.AQUA));
				lines.add(Component.literal(manifested ? "Manifested: four echoes with a longer lifetime." : "590 MP | 14s cooldown").withStyle(ChatFormatting.YELLOW));
			}
			case HELLS_ARMY -> {
				lines.add(Component.literal("Open Hell's gate and call Radiru's temporary royal guard.").withStyle(ChatFormatting.GRAY));
				lines.add(Component.literal("The guards follow your aggression and cannot harm you.").withStyle(ChatFormatting.AQUA));
				lines.add(Component.literal(manifested ? "Manifested: seven stronger guards remain longer." : "1200 MP | 32.5s cooldown").withStyle(ChatFormatting.YELLOW));
			}
			case WHITE_FLAME_SPIRITUALIZATION -> {
				lines.add(Component.literal("Unseal Baran's spiritual body without mortal armor.").withStyle(ChatFormatting.GRAY));
				lines.add(Component.literal("Adds 25% dodge after Perception; a dodge chains for 0.5 seconds.").withStyle(ChatFormatting.AQUA));
				lines.add(Component.literal(manifested ? "ACTIVE: skills have entered their sovereign forms." : "800 MP to awaken | 14 MP per second").withStyle(ChatFormatting.YELLOW));
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
			case LIU_HEAVENLY_COUNTER -> LiuZhigangCombatManager.castHeavenlyCounter(entity);
			case LIU_GOLDEN_DRAGON_DANCE -> LiuZhigangCombatManager.castGoldenDragonDance(entity);
			case LIU_SOVEREIGN_SWORD_DOMAIN -> LiuZhigangCombatManager.castSovereignSwordDomain(entity);
			case LIU_MANIFESTATION -> LiuZhigangCombatManager.toggleDragonSwordManifestation(entity);
			case BEAST_CLAW_RIFT -> BeastMonarchManager.castClawRift(entity);
			case BEAST_RUBBLE_JAW -> BeastMonarchManager.castRubbleJaw(entity);
			case BEAST_KINGS_MAUL -> BeastMonarchManager.castKingsMaul(entity);
			case BEAST_RECONSTITUTION -> BeastMonarchManager.castFeralReconstitution(entity);
			case BEAST_WHITE_FANG -> BeastMonarchManager.castWhiteFangSovereign(entity);
			case FIRE_CHARGE -> castFireCharge(world, x, y, z, entity);
			case METEOR_RAIN -> runOldJobAbility(entity, () -> Ability2OnKeyPressedProcedure.execute(world, x, y, z, entity));
			case FIREFLIES -> runOldJobAbility(entity, () -> Ability3OnKeyPressedProcedure.execute(world, x, y, z, entity));
			case ICE_SPEAR -> FrostMonarchManager.castIceSpear(entity);
			case FLASH_FREEZE -> FrostMonarchManager.castFlashFreeze(entity);
			case FROZEN_PATH -> FrostMonarchManager.castFrozenPath(entity);
			case FROST_COUNTER -> FrostMonarchManager.castFrostCounter(entity);
			case ABSOLUTE_ZERO -> FrostMonarchManager.castAbsoluteZero(entity);
			case FROST_SPIRITUALIZATION -> FrostMonarchManager.toggleSpiritualization(entity);
			case LIGHTNING_BREATH -> WhiteFlameMonarchManager.castLightningBreath(entity);
			case HELLSTORM_DOMINION -> WhiteFlameMonarchManager.castHellstormDominion(entity);
			case RADIRU_BLOOD_SPEAR -> WhiteFlameMonarchManager.castRadiruBloodSpear(entity);
			case DOPPELGANGER -> WhiteFlameMonarchManager.castDoppelganger(entity);
			case HELLS_ARMY -> WhiteFlameMonarchManager.castHellsArmy(entity);
			case WHITE_FLAME_SPIRITUALIZATION -> WhiteFlameMonarchManager.toggleSpiritualization(entity);
			case MONARCH_BEAM -> castMonarchBeam(entity);
			case LIGHTNING_STORM -> runOldJobAbility(entity, () -> Ability2OnKeyPressedProcedure.execute(world, x, y, z, entity));
			case STORM_BURST -> runOldJobAbility(entity, () -> Ability3OnKeyPressedProcedure.execute(world, x, y, z, entity));
			default -> {
			}
		}
		return true;
	}

	public static boolean release(Entity entity, String skill, int pressedMs) {
		if (entity == null || skill == null || !isJobSkill(skill))
			return false;
		if (FROZEN_PATH.equals(skill)) {
			FrostMonarchManager.releaseFrozenPath(entity);
			return true;
		}
		return false;
	}

	public static String cooldownKey(String skill) {
		return switch (skill) {
			case FIRE_CHARGE, MONARCH_BEAM -> "job_1";
			case METEOR_RAIN, LIGHTNING_STORM -> "job_2";
			case FIREFLIES, STORM_BURST -> "job_3";
			case SHADOW_MANIFESTATION, THOMAS_MANIFESTATION -> "job_4";
			case THOMAS_CAPTURE, THOMAS_POWER_SMASH, THOMAS_COLLAPSE,
					LIU_HEAVENLY_COUNTER, LIU_GOLDEN_DRAGON_DANCE, LIU_SOVEREIGN_SWORD_DOMAIN, LIU_MANIFESTATION,
					ICE_SPEAR, FLASH_FREEZE, FROZEN_PATH, FROST_COUNTER,
					ABSOLUTE_ZERO, BEAST_CLAW_RIFT, BEAST_RUBBLE_JAW, BEAST_KINGS_MAUL,
					BEAST_RECONSTITUTION, BEAST_WHITE_FANG -> skill;
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
			case 3 -> FROST_SKILLS;
			case 4 -> List.of(LIGHTNING_BREATH, HELLSTORM_DOMINION,
					RADIRU_BLOOD_SPEAR, DOPPELGANGER, HELLS_ARMY, WHITE_FLAME_SPIRITUALIZATION);
			case 5 -> List.of(THOMAS_CAPTURE, THOMAS_POWER_SMASH, THOMAS_COLLAPSE, THOMAS_MANIFESTATION);
			case 6 -> LIU_SKILLS;
			case 9 -> BEAST_SKILLS;
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
