
package net.solocraft.client.screens;

import org.checkerframework.checker.units.qual.h;

import net.solocraft.procedures.WeaponAbCooldownSymbolProcedure;
import net.solocraft.procedures.TelekinesisAbilityCooldownProcedure;
import net.solocraft.procedures.SlectedCon8Procedure;
import net.solocraft.procedures.SlectedCon7Procedure;
import net.solocraft.procedures.SkillTextColorProcedure;
import net.solocraft.procedures.SkillTextProcedure;
import net.solocraft.procedures.SkillSlotHelper;
import net.solocraft.procedures.SelectedConProcedure;
import net.solocraft.procedures.ReturnCooldownAmountProcedure;
import net.solocraft.procedures.MeleeAbilityCooldownProcedure;
import net.solocraft.procedures.IsUsingDashProcedure;
import net.solocraft.procedures.IsInCombatModeProcedure;
import net.solocraft.procedures.DoesHaveTelekinesisProcedure;
import net.solocraft.procedures.AuraAbilityCooldownProcedure;
import net.solocraft.procedures.Ability4ReturnProcedure;
import net.solocraft.procedures.Ability3ReturnProcedure;
import net.solocraft.procedures.Ability2ReturnProcedure;
import net.solocraft.procedures.Ability1ReturnProcedure;
import net.solocraft.procedures.Ab9CooldownProcedure;
import net.solocraft.procedures.Ab2CooldownProcedure;
import net.solocraft.procedures.CooldownRemainingOnTickProcedure;
import net.solocraft.api.skill.HunterAbility;
import net.solocraft.api.skill.HunterAbilityRegistry;
import net.solocraft.network.SololevelingModVariables;
import net.solocraft.util.JobSkillManager;
import net.solocraft.util.FighterSkillManager;
import net.solocraft.util.HealerSkillManager;
import net.solocraft.util.BarrierMageSpellManager;
import net.solocraft.util.ArcaneMageSpellManager;
import net.solocraft.util.FireMageSpellManager;
import net.solocraft.util.OrbOfAvariceManager;
import net.solocraft.util.CurseMageSpellManager;
import net.solocraft.util.StormMageSpellManager;
import net.solocraft.util.ShadowMonarchManager;

import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.api.distmarker.Dist;

import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.Minecraft;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.platform.GlStateManager;

@EventBusSubscriber({Dist.CLIENT})
public class DisplayOverlay {
	private static final ResourceLocation SKILL_COOLDOWN_COVER = ResourceLocation.parse("sololeveling:textures/screens/newbasiccdcover.png");

	/**
	 * The slot texture an addon declared, or the empty template.
	 *
	 * <p>Falling back to the template rather than skipping the blit keeps a slot
	 * holding an icon-less ability looking like a slot, which is what every
	 * built-in without an icon already does.
	 */
	private static ResourceLocation contributedSkillTexture(String skillName) {
		return HunterAbilityRegistry.byName(skillName)
				.map(HunterAbility::icon)
				.filter(java.util.Objects::nonNull)
				.orElse(ResourceLocation.parse("sololeveling:textures/screens/icon_template.png"));
	}

	private static ResourceLocation getSkillTexture(String skillName, boolean avariceHeld) {
		if (ShadowMonarchManager.isFormationSkill(skillName))
			return ResourceLocation.parse("sololeveling:textures/screens/newshadowformation.png");
		return switch (skillName) {
			case FireMageSpellManager.FLAME_WEAVING -> fireMageTexture("firebullet", avariceHeld);
			case FireMageSpellManager.IGNITION_ORB -> fireMageTexture("fireball", avariceHeld);
			case FireMageSpellManager.INFERNO_LANCE -> fireMageTexture("firelance", avariceHeld);
			case FireMageSpellManager.FLASHFIRE -> fireMageTexture("firedash", avariceHeld);
			case FireMageSpellManager.CREMATION -> fireMageTexture("cremation", avariceHeld);
			case FireMageSpellManager.FURNACE_DOMINION -> fireMageTexture("furnace", avariceHeld);
			case FireMageSpellManager.HEAVENFALL -> fireMageTexture("meteor", avariceHeld);
			case BarrierMageSpellManager.FRACTURE_BOLT -> ResourceLocation.parse("sololeveling:textures/screens/icon_mage_barrier_fracturebolt.png");
			case BarrierMageSpellManager.PRISM_RAMPART -> ResourceLocation.parse("sololeveling:textures/screens/icon_mage_barrier_prismrampart.png");
			case BarrierMageSpellManager.REPULSION_FRAME -> ResourceLocation.parse("sololeveling:textures/screens/icon_mage_barrier_repulsionframe.png");
			case BarrierMageSpellManager.SEALING_PRISM -> ResourceLocation.parse("sololeveling:textures/screens/icon_mage_barrier_sealingprism.png");
			case BarrierMageSpellManager.MIRROR_WARD -> ResourceLocation.parse("sololeveling:textures/screens/icon_mage_barrier_mirrorward.png");
			case BarrierMageSpellManager.RESONANT_COLLAPSE -> ResourceLocation.parse("sololeveling:textures/screens/icon_mage_barrier_resonantcollapse.png");
			case BarrierMageSpellManager.ABSOLUTE_BASTION -> ResourceLocation.parse("sololeveling:textures/screens/icon_mage_barrier_absolutebastion.png");
			case ArcaneMageSpellManager.AETHER_BOLT -> ResourceLocation.parse("sololeveling:textures/screens/icon_magicmissiles.png");
			case ArcaneMageSpellManager.VECTOR_STEP -> ResourceLocation.parse("sololeveling:textures/screens/icon_shadowstep.png");
			case ArcaneMageSpellManager.POLARITY_SPHERE -> ResourceLocation.parse("sololeveling:textures/screens/icon_cursesphere.png");
			case ArcaneMageSpellManager.RUNIC_RELAY -> ResourceLocation.parse("sololeveling:textures/screens/icon_telekinesis.png");
			case ArcaneMageSpellManager.ASTRAL_ARSENAL -> ResourceLocation.parse("sololeveling:textures/screens/icon_swordbeam.png");
			case ArcaneMageSpellManager.DIMENSIONAL_REND -> ResourceLocation.parse("sololeveling:textures/screens/icon_slashfury.png");
			case ArcaneMageSpellManager.CONVERGENCE -> ResourceLocation.parse("sololeveling:textures/screens/icon_groundslam.png");
			case StormMageSpellManager.STATIC_NEEDLE -> ResourceLocation.parse("sololeveling:textures/screens/icon_magicmissiles.png");
			case StormMageSpellManager.SLIPSTREAM -> ResourceLocation.parse("sololeveling:textures/screens/icon_shadowstep.png");
			case StormMageSpellManager.THUNDERCLAP -> ResourceLocation.parse("sololeveling:textures/screens/newbaranstormburst.png");
			case StormMageSpellManager.LIGHTNING_ROD -> ResourceLocation.parse("sololeveling:textures/screens/icon_telekinesis.png");
			case StormMageSpellManager.CHAIN_LIGHTNING -> ResourceLocation.parse("sololeveling:textures/screens/icon_mowf_lightningbreath.png");
			case StormMageSpellManager.THUNDERHEAD -> ResourceLocation.parse("sololeveling:textures/screens/newbaranlightningstrike.png");
			case StormMageSpellManager.SKYBREAKER -> ResourceLocation.parse("sololeveling:textures/screens/newbaranlaser.png");
			case StormMageSpellManager.TEMPEST_INCARNATE -> ResourceLocation.parse("sololeveling:textures/screens/icon_mowf_hellstormdominion.png");
			case CurseMageSpellManager.CURSE_WEAVE -> ResourceLocation.parse("sololeveling:textures/screens/icon_cursesphere.png");
			case CurseMageSpellManager.HEX_BOLT -> ResourceLocation.parse("sololeveling:textures/screens/icon_magicmissiles.png");
			case CurseMageSpellManager.MALEFIC_BURST -> ResourceLocation.parse("sololeveling:textures/screens/icon_cursesphere.png");
			case CurseMageSpellManager.CREEPING_MIASMA -> ResourceLocation.parse("sololeveling:textures/screens/icon_telekinesis.png");
			case CurseMageSpellManager.VECTOR_OF_RUIN -> ResourceLocation.parse("sololeveling:textures/screens/icon_new_blessing_mark.png");
			case CurseMageSpellManager.CULLING -> ResourceLocation.parse("sololeveling:textures/screens/icon_slashfury.png");
			case "Backstab", "Night Rend" -> ResourceLocation.parse("sololeveling:textures/screens/icon_backstab.png");
			case "Dualwield" -> ResourceLocation.parse("sololeveling:textures/screens/icon_dualwielding.png");
			case "Quickslashes", "Flash Cut" -> ResourceLocation.parse("sololeveling:textures/screens/icon_quickslashes.png");
			case "Dagger Throw" -> ResourceLocation.parse("sololeveling:textures/screens/icon_dualwielding.png");
			case "Dagger Rush" -> ResourceLocation.parse("sololeveling:textures/screens/icon_quickslashes.png");
			case "Shadowstep", "Ghost Step" -> ResourceLocation.parse("sololeveling:textures/screens/icon_shadowstep.png");
			case "Stealth" -> ResourceLocation.parse("sololeveling:textures/screens/icon_stealth.png");
			case "Murderious Intent" -> ResourceLocation.parse("sololeveling:textures/screens/icon_murderiousintend.png");
			case "Detection" -> ResourceLocation.parse("sololeveling:textures/screens/icon_detection.png");
			case "Slash Dash" -> ResourceLocation.parse("sololeveling:textures/screens/icon_slashdash.png");
			case "Cross Strike", "Critical Strike" -> ResourceLocation.parse("sololeveling:textures/screens/icon_criticalstrike.png");
			case "Sword of Light" -> ResourceLocation.parse("sololeveling:textures/screens/icon_swordoflight.png");
			case "Ground Slam" -> ResourceLocation.parse("sololeveling:textures/screens/icon_groundslam.png");
			case "Sword Dance" -> ResourceLocation.parse("sololeveling:textures/screens/icon_sworddance.png");
			case "Heal Beam" -> ResourceLocation.parse("sololeveling:textures/screens/icon_new_healing_beam.png");
			case "Slash Fury" -> ResourceLocation.parse("sololeveling:textures/screens/icon_slashfury.png");
			case "Blessing Mark" -> ResourceLocation.parse("sololeveling:textures/screens/icon_new_blessing_mark.png");
			case "Purification" -> ResourceLocation.parse("sololeveling:textures/screens/icon_purification.png");
			case "Physical Buff" -> ResourceLocation.parse("sololeveling:textures/screens/icon_physicalbuff.png");
			case "Haste Buff" -> ResourceLocation.parse("sololeveling:textures/screens/icon_hastebuff.png");
			case "Overheal" -> ResourceLocation.parse("sololeveling:textures/screens/icon_new_overheal.png");
			case "Tank Leap" -> ResourceLocation.parse("sololeveling:textures/screens/icon_tankleap.png");
			case "Protection Mark" -> ResourceLocation.parse("sololeveling:textures/screens/icon_protectionmark.png");
			case "Reinforcement" -> ResourceLocation.parse("sololeveling:textures/screens/icon_reinforcement.png");
			case "Shield Bash" -> ResourceLocation.parse("sololeveling:textures/screens/icon_shieldbash.png");
			case "Willpower" -> ResourceLocation.parse("sololeveling:textures/screens/icon_willpower.png");
			case "Taunt" -> ResourceLocation.parse("sololeveling:textures/screens/icon_taunt.png");
			case "Sharpshooter" -> ResourceLocation.parse("sololeveling:textures/screens/icon_sharpshooter.png");
			case "Mana Quiver" -> ResourceLocation.parse("sololeveling:textures/screens/icon_sharpshooter.png");
			case "Rapid Fire" -> ResourceLocation.parse("sololeveling:textures/screens/icon_firearrows.png");
			case "Arrow Shower" -> ResourceLocation.parse("sololeveling:textures/screens/icon_proximitytrap.png");
			case "Proximity Trap" -> ResourceLocation.parse("sololeveling:textures/screens/icon_proximitytrap.png");
			case "Back Step" -> ResourceLocation.parse("sololeveling:textures/screens/icon_backstep.png");
			case "High Value Target" -> ResourceLocation.parse("sololeveling:textures/screens/icon_highvaluetarget.png");
			case "Hawkeye" -> ResourceLocation.parse("sololeveling:textures/screens/icon_hawkeye.png");
			case "Hyper Focus" -> ResourceLocation.parse("sololeveling:textures/screens/icon_hyperfocus.png");
			case "Cold Blood" -> ResourceLocation.parse("sololeveling:textures/screens/icon_murderiousintend.png");
			case "Critical Attack" -> ResourceLocation.parse("sololeveling:textures/screens/icon_critical_strike.png");
			case "Mutilation" -> ResourceLocation.parse("sololeveling:textures/screens/icon_mutilation.png");
			case "Sword Beam" -> ResourceLocation.parse("sololeveling:textures/screens/icon_swordbeam.png");
			case FighterSkillManager.IRON_KNUCKLE -> ResourceLocation.parse("sololeveling:textures/screens/icon_ironknuckle.png");
			case FighterSkillManager.BREAKER_COMBO -> ResourceLocation.parse("sololeveling:textures/screens/icon_breakercombo.png");
			case FighterSkillManager.METEOR_FIST -> ResourceLocation.parse("sololeveling:textures/screens/icon_meteorfist.png");
			case FighterSkillManager.TITANS_BARRAGE -> ResourceLocation.parse("sololeveling:textures/screens/icon_titansbarrage.png");
			case FighterSkillManager.RADIANT_EXECUTION -> ResourceLocation.parse("sololeveling:textures/screens/icon_radiantexecution.png");
			case FighterSkillManager.MAGICAL_EYE -> ResourceLocation.parse("sololeveling:textures/screens/icon_magicaleye.png");
			case FighterSkillManager.CLAW_STRIKES -> ResourceLocation.parse("sololeveling:textures/screens/icon_clawstrikes.png");
			case FighterSkillManager.BEAST_SENSE -> ResourceLocation.parse("sololeveling:textures/screens/icon_beastsense.png");
			case FighterSkillManager.PARTIAL_TRANSFORMATION -> ResourceLocation.parse("sololeveling:textures/screens/icon_partialtransformation.png");
			case FighterSkillManager.PREDATOR_RUSH -> ResourceLocation.parse("sololeveling:textures/screens/icon_predatorrush.png");
			case FighterSkillManager.FULL_BEAST_TRANSFORMATION -> ResourceLocation.parse("sololeveling:textures/screens/icon_fullbeasttransformation.png");
			case HealerSkillManager.HEALING_PULSE -> ResourceLocation.parse("sololeveling:textures/screens/icon_healingpulse.png");
			case HealerSkillManager.PURIFYING_WAVE -> ResourceLocation.parse("sololeveling:textures/screens/icon_purifyingwave.png");
			case HealerSkillManager.SANCTUARY -> ResourceLocation.parse("sololeveling:textures/screens/icon_sanctuary.png");
			case HealerSkillManager.SECOND_WIND -> ResourceLocation.parse("sololeveling:textures/screens/icon_secondwind.png");
			case HealerSkillManager.VITALITY_SURGE -> ResourceLocation.parse("sololeveling:textures/screens/icon_vitalitysurge.png");
			case HealerSkillManager.MANA_FONT -> ResourceLocation.parse("sololeveling:textures/screens/icon_manafont.png");
			case HealerSkillManager.GUARDIAN_WARD -> ResourceLocation.parse("sololeveling:textures/screens/icon_guardianward.png");
			case HealerSkillManager.GUARDIAN_STEP -> ResourceLocation.parse("sololeveling:textures/screens/icon_guardianstep.png");
			case HealerSkillManager.DIVINE_FAVOR -> ResourceLocation.parse("sololeveling:textures/screens/icon_divinefavor.png");
			case HealerSkillManager.CAMOUFLAGE -> ResourceLocation.parse("sololeveling:textures/screens/icon_camouflage.png");
			case JobSkillManager.ARISE -> ResourceLocation.parse("sololeveling:textures/screens/newshadowarise.png");
			case JobSkillManager.SHADOW_SUMMON -> ResourceLocation.parse("sololeveling:textures/screens/newshadowsummon.png");
			case JobSkillManager.DISMISS_SHADOWS -> ResourceLocation.parse("sololeveling:textures/screens/newshadowdismiss.png");
			case JobSkillManager.SHADOW_COMMAND -> ResourceLocation.parse("sololeveling:textures/screens/icon_shadowcommand.png");
			case JobSkillManager.SHADOW_EXCHANGE -> ResourceLocation.parse("sololeveling:textures/screens/newshadowexchange.png");
			case JobSkillManager.SHADOW_MANIFESTATION -> ResourceLocation.parse("sololeveling:textures/screens/newshadowarmor.png");
			case JobSkillManager.FIRE_CHARGE -> ResourceLocation.parse("sololeveling:textures/screens/griamorefire.png");
			case JobSkillManager.METEOR_RAIN -> ResourceLocation.parse("sololeveling:textures/screens/icon_firearrows.png");
			case JobSkillManager.FIREFLIES -> ResourceLocation.parse("sololeveling:textures/screens/newmagesummoning.png");
			case JobSkillManager.ICE_SPEAR -> ResourceLocation.parse("sololeveling:textures/screens/newfrostmonarchspear.png");
			case JobSkillManager.FLASH_FREEZE -> ResourceLocation.parse("sololeveling:textures/screens/icon_frost_stillness.png");
			case JobSkillManager.FROZEN_PATH -> ResourceLocation.parse("sololeveling:textures/screens/icon_frost_causeway.png");
			case JobSkillManager.FROZEN_ARCHITECTURE -> ResourceLocation.parse("sololeveling:textures/screens/newfrostmonarchchunk.png");
			case JobSkillManager.FROST_COUNTER -> ResourceLocation.parse("sololeveling:textures/screens/icon_frost_winter_remembers.png");
			case JobSkillManager.ABSOLUTE_ZERO -> ResourceLocation.parse("sololeveling:textures/screens/icon_frost_whiteout.png");
			case JobSkillManager.FROST_SPIRITUALIZATION -> ResourceLocation.parse("sololeveling:textures/screens/icon_frost_spiritualization.png");
			case JobSkillManager.MONARCH_BEAM -> ResourceLocation.parse("sololeveling:textures/screens/newbaranlaser.png");
			case JobSkillManager.LIGHTNING_STORM -> ResourceLocation.parse("sololeveling:textures/screens/newbaranlightningstrike.png");
			case JobSkillManager.STORM_BURST -> ResourceLocation.parse("sololeveling:textures/screens/newbaranstormburst.png");
			case JobSkillManager.LIGHTNING_BREATH -> ResourceLocation.parse("sololeveling:textures/screens/icon_mowf_lightningbreath.png");
			case JobSkillManager.HELLSTORM_DOMINION -> ResourceLocation.parse("sololeveling:textures/screens/icon_mowf_hellstormdominion.png");
			case JobSkillManager.RADIRU_BLOOD_SPEAR -> ResourceLocation.parse("sololeveling:textures/screens/icon_mowf_radirubloodspear.png");
			case JobSkillManager.DOPPELGANGER -> ResourceLocation.parse("sololeveling:textures/screens/icon_mowf_doppelganger.png");
			case JobSkillManager.HELLS_ARMY -> ResourceLocation.parse("sololeveling:textures/screens/icon_mowf_hellsarmy.png");
			case JobSkillManager.WHITE_FLAME_SPIRITUALIZATION -> ResourceLocation.parse("sololeveling:textures/screens/icon_spiritualize_mowf.png");
			case JobSkillManager.THOMAS_CAPTURE -> ResourceLocation.parse("sololeveling:textures/screens/icon_goliath_1.png");
			case JobSkillManager.THOMAS_POWER_SMASH -> ResourceLocation.parse("sololeveling:textures/screens/icon_goliath_2.png");
			case JobSkillManager.THOMAS_COLLAPSE -> ResourceLocation.parse("sololeveling:textures/screens/icon_goliath_3.png");
			case JobSkillManager.THOMAS_MANIFESTATION -> ResourceLocation.parse("sololeveling:textures/screens/icon_spiritualize_goliath.png");
			case JobSkillManager.LIU_HEAVENLY_COUNTER -> ResourceLocation.parse("sololeveling:textures/screens/icon_goliath_1.png");
			case JobSkillManager.LIU_GOLDEN_DRAGON_DANCE -> ResourceLocation.parse("sololeveling:textures/screens/icon_goliath_2.png");
			case JobSkillManager.LIU_SOVEREIGN_SWORD_DOMAIN -> ResourceLocation.parse("sololeveling:textures/screens/icon_goliath_3.png");
			case JobSkillManager.LIU_MANIFESTATION -> ResourceLocation.parse("sololeveling:textures/screens/icon_spiritualize_goliath.png");
			case JobSkillManager.SUNG_PREDATORS_PRESENCE -> ResourceLocation.parse("sololeveling:textures/screens/icon_goliath_1.png");
			case JobSkillManager.SUNG_ASSASSIN_STANCE -> ResourceLocation.parse("sololeveling:textures/screens/icon_goliath_2.png");
			case JobSkillManager.SUNG_SPATIAL_EXECUTION -> ResourceLocation.parse("sololeveling:textures/screens/icon_goliath_3.png");
			case JobSkillManager.SUNG_SPIRITUALIZATION -> ResourceLocation.parse("sololeveling:textures/screens/icon_spiritualize_goliath.png");
			case JobSkillManager.ANTARES_DESTRUCTION_CLAW -> ResourceLocation.parse("sololeveling:textures/screens/icon_antares_claw.png");
			case JobSkillManager.ANTARES_BREATH -> ResourceLocation.parse("sololeveling:textures/screens/icon_antares_breathofdestruction.png");
			case JobSkillManager.ANTARES_DESCENT -> ResourceLocation.parse("sololeveling:textures/screens/icon_antares_monarchdescend.png");
			case JobSkillManager.ANTARES_ROAR -> ResourceLocation.parse("sololeveling:textures/screens/icon_antares_monarchsroar.png");
			case JobSkillManager.ANTARES_EXTINCTION -> ResourceLocation.parse("sololeveling:textures/screens/icon_antares_extinction.png");
			case JobSkillManager.ANTARES_MANIFESTATION -> ResourceLocation.parse("sololeveling:textures/screens/icon_antares_spiritualize.png");
			// A contributed ability declares its own slot texture. Built-ins are
			// matched first above, so an addon can never shadow a shipped icon.
			default -> contributedSkillTexture(skillName);
		};
	}

	private static ResourceLocation fireMageTexture(String name, boolean avariceHeld) {
		return ResourceLocation.parse("sololeveling:textures/screens/icon_mage_fire_" + name
				+ (avariceHeld ? "_orb" : "") + ".png");
	}

	@SubscribeEvent(priority = EventPriority.NORMAL)
	public static void eventHandler(RenderGuiEvent.Pre event) {
		int w = event.getGuiGraphics().guiWidth();
		int h = event.getGuiGraphics().guiHeight();
		Level world = null;
		double x = 0;
		double y = 0;
		double z = 0;
		Player entity = Minecraft.getInstance().player;
		if (entity != null) {
			world = entity.level();
			x = entity.getX();
			y = entity.getY();
			z = entity.getZ();
		}
		boolean visible = IsInCombatModeProcedure.execute(entity);
		if (!visible)
			return;
		RenderSystem.disableDepthTest();
		RenderSystem.depthMask(false);
		RenderSystem.enableBlend();
		RenderSystem.setShader(GameRenderer::getPositionTexShader);
		RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
		RenderSystem.setShaderColor(1, 1, 1, 1);
		if (visible) {
			event.getGuiGraphics().blit(ResourceLocation.parse("sololeveling:textures/screens/icon_melee.png"), w - 24, h - 24, 0, 0, 20, 20, 20, 20);
			if (WeaponAbCooldownSymbolProcedure.execute(entity)) {
				event.getGuiGraphics().blit(ResourceLocation.parse("sololeveling:textures/screens/newbasiccdcover.png"), w - 24, h - 24, 0, 0, 20, 20, 20, 20);
			}
			event.getGuiGraphics().blit(ResourceLocation.parse("sololeveling:textures/screens/icon_telekinesis.png"), w - 24, h - 47, 0, 0, 20, 20, 20, 20);
			if (Ab2CooldownProcedure.execute(entity)) {
				event.getGuiGraphics().blit(ResourceLocation.parse("sololeveling:textures/screens/newbasiccdcover.png"), w - 24, h - 47, 0, 0, 20, 20, 20, 20);
			}
			if (DoesHaveTelekinesisProcedure.execute(entity)) {
				event.getGuiGraphics().blit(ResourceLocation.parse("sololeveling:textures/screens/newbasicabilitylocked.png"), w - 23, h - 47, 0, 0, 20, 20, 20, 20);
			}
			event.getGuiGraphics().blit(ResourceLocation.parse("sololeveling:textures/screens/icon_dash.png"), w - 24, h - 70, 0, 0, 20, 20, 20, 20);
			event.getGuiGraphics().blit(ResourceLocation.parse("sololeveling:textures/screens/icon_aura.png"), w - 24, h - 93, 0, 0, 20, 20, 20, 20);
			if (Ab9CooldownProcedure.execute(entity)) {
				event.getGuiGraphics().blit(ResourceLocation.parse("sololeveling:textures/screens/newbasiccdcover.png"), w - 24, h - 93, 0, 0, 20, 20, 20, 20);
			}
			event.getGuiGraphics().blit(ResourceLocation.parse("sololeveling:textures/screens/keyplaceholder.png"), w - 31, h - 89, 0, 0, 12, 12, 12, 12);
			event.getGuiGraphics().blit(ResourceLocation.parse("sololeveling:textures/screens/keyplaceholder.png"), w - 31, h - 66, 0, 0, 12, 12, 12, 12);
			event.getGuiGraphics().blit(ResourceLocation.parse("sololeveling:textures/screens/keyplaceholder.png"), w - 31, h - 43, 0, 0, 12, 12, 12, 12);
			event.getGuiGraphics().blit(ResourceLocation.parse("sololeveling:textures/screens/keyplaceholder.png"), w - 31, h - 20, 0, 0, 12, 12, 12, 12);
			if (IsUsingDashProcedure.execute(entity)) {
				event.getGuiGraphics().blit(ResourceLocation.parse("sololeveling:textures/screens/newbasetick.png"), w - 16, h - 78, 0, 0, 20, 20, 20, 20);
			}
			event.getGuiGraphics().blit(ResourceLocation.parse("sololeveling:textures/screens/icon_background.png"), w / 2 + -90, h - 22, 0, 0, 162, 22, 162, 22);
			int[] slotXOffsets = {-89, -69, -49, -29, -9, 11, 31, 51};
			SololevelingModVariables.PlayerVariables vars = entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables());
			boolean avariceHeld = OrbOfAvariceManager.isHeldBy(entity);
			for (int i = 0; i < 8; i++) {
				String skillName = SkillSlotHelper.getSlot(vars, vars.PskillPage >= 2 ? i + 9 : i + 1);
				/*
				if (skillName.isEmpty()) {
					Minecraft.getInstance().gui.getChat().addMessage(Component.literal("Slot " + (i + 1) + " is empty!"));
				}
				*/
				ResourceLocation icon = getSkillTexture(skillName, avariceHeld);
				int slotX = w / 2 + slotXOffsets[i];
				int slotY = h - 21;
				event.getGuiGraphics().blit(icon, slotX, slotY, 0, 0, 20, 20, 20, 20);
				String cooldownLabel = CooldownRemainingOnTickProcedure.executeForSkill(entity, skillName);
				if (!cooldownLabel.isEmpty()) {
					event.getGuiGraphics().blit(SKILL_COOLDOWN_COVER, slotX, slotY, 0, 0, 20, 20, 20, 20);
					int labelX = slotX + (20 - Minecraft.getInstance().font.width(cooldownLabel)) / 2;
					event.getGuiGraphics().drawString(Minecraft.getInstance().font, cooldownLabel, labelX, slotY + 6, 0xFFFFFF, true);
				}
			}
			if (SelectedConProcedure.execute(entity, 1)) {
				event.getGuiGraphics().blit(ResourceLocation.parse("sololeveling:textures/screens/icon_frame.png"), w / 2 + -90, h - 22, 0, 0, 22, 22, 22, 22);
			}
			if (SelectedConProcedure.execute(entity, 2)) {
				event.getGuiGraphics().blit(ResourceLocation.parse("sololeveling:textures/screens/icon_frame.png"), w / 2 + -70, h - 22, 0, 0, 22, 22, 22, 22);
			}
			if (SelectedConProcedure.execute(entity, 3)) {
				event.getGuiGraphics().blit(ResourceLocation.parse("sololeveling:textures/screens/icon_frame.png"), w / 2 + -50, h - 22, 0, 0, 22, 22, 22, 22);
			}
			if (SelectedConProcedure.execute(entity, 4)) {
				event.getGuiGraphics().blit(ResourceLocation.parse("sololeveling:textures/screens/icon_frame.png"), w / 2 + -30, h - 22, 0, 0, 22, 22, 22, 22);
			}
			if (SelectedConProcedure.execute(entity, 5)) {
				event.getGuiGraphics().blit(ResourceLocation.parse("sololeveling:textures/screens/icon_frame.png"), w / 2 + -10, h - 22, 0, 0, 22, 22, 22, 22);
			}
			if (SelectedConProcedure.execute(entity, 6)) {
				event.getGuiGraphics().blit(ResourceLocation.parse("sololeveling:textures/screens/icon_frame.png"), w / 2 + 10, h - 22, 0, 0, 22, 22, 22, 22);
			}
			if (SlectedCon7Procedure.execute(entity)) {
				event.getGuiGraphics().blit(ResourceLocation.parse("sololeveling:textures/screens/icon_frame.png"), w / 2 + 30, h - 22, 0, 0, 22, 22, 22, 22);
			}
			if (SlectedCon8Procedure.execute(entity)) {
				event.getGuiGraphics().blit(ResourceLocation.parse("sololeveling:textures/screens/icon_frame.png"), w / 2 + 50, h - 22, 0, 0, 22, 22, 22, 22);
			}
			event.getGuiGraphics().drawString(Minecraft.getInstance().font, ReturnCooldownAmountProcedure.execute(entity), w / 2 + 74, h - 12, -26266, false);
			event.getGuiGraphics().drawString(Minecraft.getInstance().font, SkillTextProcedure.execute(entity), w / 2 + 74, h - 22, SkillTextColorProcedure.execute(entity), false);
			event.getGuiGraphics().drawString(Minecraft.getInstance().font, Ability3ReturnProcedure.execute(), w - 28, h - 64, -1, false);
			event.getGuiGraphics().drawString(Minecraft.getInstance().font, Ability1ReturnProcedure.execute(), w - 28, h - 18, -1, false);
			event.getGuiGraphics().drawString(Minecraft.getInstance().font, Ability2ReturnProcedure.execute(), w - 28, h - 41, -1, false);
			event.getGuiGraphics().drawString(Minecraft.getInstance().font, Ability4ReturnProcedure.execute(), w - 27, h - 87, -1, false);
			event.getGuiGraphics().drawString(Minecraft.getInstance().font, MeleeAbilityCooldownProcedure.execute(entity), w - 18, h - 18, -1, false);
			event.getGuiGraphics().drawString(Minecraft.getInstance().font, TelekinesisAbilityCooldownProcedure.execute(entity), w - 18, h - 41, -1, false);
			event.getGuiGraphics().drawString(Minecraft.getInstance().font, AuraAbilityCooldownProcedure.execute(entity), w - 18, h - 87, -1, false);
		}
		RenderSystem.depthMask(true);
		RenderSystem.defaultBlendFunc();
		RenderSystem.enableDepthTest();
		RenderSystem.disableBlend();
		RenderSystem.setShaderColor(1, 1, 1, 1);
	}
}
