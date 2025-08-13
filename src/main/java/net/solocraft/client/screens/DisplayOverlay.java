
package net.solocraft.client.screens;

import org.checkerframework.checker.units.qual.h;

import net.solocraft.procedures.WeaponAbCooldownSymbolProcedure;
import net.solocraft.procedures.TelekinesisAbilityCooldownProcedure;
import net.solocraft.procedures.SlectedCon8Procedure;
import net.solocraft.procedures.SlectedCon7Procedure;
import net.solocraft.procedures.SkillTextProcedure;
import net.solocraft.procedures.SelectedCon6Procedure;
import net.solocraft.procedures.SelectedCon5Procedure;
import net.solocraft.procedures.SelectedCon4Procedure;
import net.solocraft.procedures.SelectedCon3Procedure;
import net.solocraft.procedures.SelectedCon2Procedure;
import net.solocraft.procedures.SelectedCon1Procedure;
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
import net.solocraft.network.SololevelingModVariables;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.api.distmarker.Dist;

import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.Minecraft;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.platform.GlStateManager;

@Mod.EventBusSubscriber({Dist.CLIENT})
public class DisplayOverlay {
	private static ResourceLocation getSkillTexture(String skillName) {
		return switch (skillName) {
			// goofy ahh mapping
			case "Fireball" -> new ResourceLocation("sololeveling:textures/screens/icon_new_fireball.png");
			case "Water Slash" -> new ResourceLocation("sololeveling:textures/screens/icon_new_waterslash.png");
			case "Lightball" -> new ResourceLocation("sololeveling:textures/screens/icon_new_light_ball.png");
			case "Light Golem" -> new ResourceLocation("sololeveling:textures/screens/icon_lightgolem.png");
			case "Backstab" -> new ResourceLocation("sololeveling:textures/screens/icon_backstab.png");
			case "Dualwield" -> new ResourceLocation("sololeveling:textures/screens/icon_dualwielding.png");
			case "Quickslashes" -> new ResourceLocation("sololeveling:textures/screens/icon_quickslashes.png");
			case "Shadowstep" -> new ResourceLocation("sololeveling:textures/screens/icon_shadowstep.png");
			case "Stealth" -> new ResourceLocation("sololeveling:textures/screens/icon_stealth.png");
			case "Murderious Intent" -> new ResourceLocation("sololeveling:textures/screens/icon_murderiousintend.png");
			case "Detection" -> new ResourceLocation("sololeveling:textures/screens/icon_detection.png");
			case "Curse Sphere" -> new ResourceLocation("sololeveling:textures/screens/icon_cursesphere.png");
			case "Slash Dash" -> new ResourceLocation("sololeveling:textures/screens/icon_slashdash.png");
			case "Critical Strike" -> new ResourceLocation("sololeveling:textures/screens/icon_criticalstrike.png");
			case "Sword of Light" -> new ResourceLocation("sololeveling:textures/screens/icon_swordoflight.png");
			case "Ground Slam" -> new ResourceLocation("sololeveling:textures/screens/icon_groundslam.png");
			case "Sword Dance" -> new ResourceLocation("sololeveling:textures/screens/icon_sworddance.png");
			case "Heal Beam" -> new ResourceLocation("sololeveling:textures/screens/icon_new_healing_beam.png");
			case "Slash Fury" -> new ResourceLocation("sololeveling:textures/screens/icon_slashfury.png");
			case "Blessing Mark" -> new ResourceLocation("sololeveling:textures/screens/icon_new_blessing_mark.png");
			case "Purification" -> new ResourceLocation("sololeveling:textures/screens/icon_purification.png");
			case "Physical Buff" -> new ResourceLocation("sololeveling:textures/screens/icon_physicalbuff.png");
			case "Haste Buff" -> new ResourceLocation("sololeveling:textures/screens/icon_hastebuff.png");
			case "Overheal" -> new ResourceLocation("sololeveling:textures/screens/icon_new_overheal.png");
			case "Tank Leap" -> new ResourceLocation("sololeveling:textures/screens/icon_tankleap.png");
			case "Protection Mark" -> new ResourceLocation("sololeveling:textures/screens/icon_protectionmark.png");
			case "Reinforcement" -> new ResourceLocation("sololeveling:textures/screens/icon_reinforcement.png");
			case "Shield Bash" -> new ResourceLocation("sololeveling:textures/screens/icon_shieldbash.png");
			case "Willpower" -> new ResourceLocation("sololeveling:textures/screens/icon_willpower.png");
			case "Taunt" -> new ResourceLocation("sololeveling:textures/screens/icon_taunt.png");
			case "Sharpshooter" -> new ResourceLocation("sololeveling:textures/screens/icon_sharpshooter.png");
			case "Proximity Trap" -> new ResourceLocation("sololeveling:textures/screens/icon_proximitytrap.png");
			case "Back Step" -> new ResourceLocation("sololeveling:textures/screens/icon_backstep.png");
			case "High Value Target" -> new ResourceLocation("sololeveling:textures/screens/icon_highvaluetarget.png");
			case "Hawkeye" -> new ResourceLocation("sololeveling:textures/screens/icon_hawkeye.png");
			case "Hyper Focus" -> new ResourceLocation("sololeveling:textures/screens/icon_hyperfocus.png");
			case "Flame Tornado" -> new ResourceLocation("sololeveling:textures/screens/icon_firehurricane.png");
			case "Heavy Flame" -> new ResourceLocation("sololeveling:textures/screens/icon_firebeam.png");
			case "Flame Vortex" -> new ResourceLocation("sololeveling:textures/screens/icon_firevacuum.png");
			case "Curse Smoke" -> new ResourceLocation("sololeveling:textures/screens/icon_cursesmoke.png");
			case "Curse Chains" -> new ResourceLocation("sololeveling:textures/screens/icon_cursechains.png");
			case "Critical Attack" -> new ResourceLocation("sololeveling:textures/screens/icon_critical_strike.png");
			case "Mutilation" -> new ResourceLocation("sololeveling:textures/screens/icon_mutilation.png");
			case "Sword Beam" -> new ResourceLocation("sololeveling:textures/screens/icon_swordbeam.png");
			case "Magic Missiles" -> new ResourceLocation("sololeveling:textures/screens/icon_magicmissiles.png");
			case "Fire Rain" -> new ResourceLocation("sololeveling:textures/screens/icon_firearrows.png");
			default -> new ResourceLocation("sololeveling:textures/screens/icon_template.png");
		};
	}

	@SubscribeEvent(priority = EventPriority.NORMAL)
	public static void eventHandler(RenderGuiEvent.Pre event) {
		int w = event.getWindow().getGuiScaledWidth();
		int h = event.getWindow().getGuiScaledHeight();
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
		RenderSystem.disableDepthTest();
		RenderSystem.depthMask(false);
		RenderSystem.enableBlend();
		RenderSystem.setShader(GameRenderer::getPositionTexShader);
		RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
		RenderSystem.setShaderColor(1, 1, 1, 1);
		if (IsInCombatModeProcedure.execute(entity)) {
			event.getGuiGraphics().blit(new ResourceLocation("sololeveling:textures/screens/icon_melee.png"), w - 24, h - 24, 0, 0, 20, 20, 20, 20);
			if (WeaponAbCooldownSymbolProcedure.execute(entity)) {
				event.getGuiGraphics().blit(new ResourceLocation("sololeveling:textures/screens/newbasiccdcover.png"), w - 24, h - 24, 0, 0, 20, 20, 20, 20);
			}
			event.getGuiGraphics().blit(new ResourceLocation("sololeveling:textures/screens/icon_telekinesis.png"), w - 24, h - 47, 0, 0, 20, 20, 20, 20);
			if (Ab2CooldownProcedure.execute(entity)) {
				event.getGuiGraphics().blit(new ResourceLocation("sololeveling:textures/screens/newbasiccdcover.png"), w - 24, h - 47, 0, 0, 20, 20, 20, 20);
			}
			if (DoesHaveTelekinesisProcedure.execute(entity)) {
				event.getGuiGraphics().blit(new ResourceLocation("sololeveling:textures/screens/newbasicabilitylocked.png"), w - 23, h - 47, 0, 0, 20, 20, 20, 20);
			}
			event.getGuiGraphics().blit(new ResourceLocation("sololeveling:textures/screens/icon_dash.png"), w - 24, h - 70, 0, 0, 20, 20, 20, 20);
			event.getGuiGraphics().blit(new ResourceLocation("sololeveling:textures/screens/icon_aura.png"), w - 24, h - 93, 0, 0, 20, 20, 20, 20);
			if (Ab9CooldownProcedure.execute(entity)) {
				event.getGuiGraphics().blit(new ResourceLocation("sololeveling:textures/screens/newbasiccdcover.png"), w - 24, h - 93, 0, 0, 20, 20, 20, 20);
			}
			event.getGuiGraphics().blit(new ResourceLocation("sololeveling:textures/screens/keyplaceholder.png"), w - 31, h - 89, 0, 0, 12, 12, 12, 12);
			event.getGuiGraphics().blit(new ResourceLocation("sololeveling:textures/screens/keyplaceholder.png"), w - 31, h - 66, 0, 0, 12, 12, 12, 12);
			event.getGuiGraphics().blit(new ResourceLocation("sololeveling:textures/screens/keyplaceholder.png"), w - 31, h - 43, 0, 0, 12, 12, 12, 12);
			event.getGuiGraphics().blit(new ResourceLocation("sololeveling:textures/screens/keyplaceholder.png"), w - 31, h - 20, 0, 0, 12, 12, 12, 12);
			if (IsUsingDashProcedure.execute(entity)) {
				event.getGuiGraphics().blit(new ResourceLocation("sololeveling:textures/screens/newbasetick.png"), w - 16, h - 78, 0, 0, 20, 20, 20, 20);
			}
			event.getGuiGraphics().blit(new ResourceLocation("sololeveling:textures/screens/icon_background.png"), w / 2 + -90, h - 22, 0, 0, 162, 22, 162, 22);
			int[] slotXOffsets = {-89, -69, -49, -29, -9, 11, 31, 51};
			for (int i = 0; i < 8; i++) {
				String skillName = "";
				// Get capability safely
				SololevelingModVariables.PlayerVariables vars = entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables());
				// Use explicit if-else for better debugging
				if (i == 0) {
					skillName = vars.Pslot1;
				} else if (i == 1) {
					skillName = vars.Pslot2;
				} else if (i == 2) {
					skillName = vars.Pslot3;
				} else if (i == 3) {
					skillName = vars.Pslot4;
				} else if (i == 4) {
					skillName = vars.Pslot5;
				} else if (i == 5) {
					skillName = vars.Pslot6;
				} else if (i == 6) {
					skillName = vars.Pslot7;
				} else if (i == 7) {
					skillName = vars.Pslot8;
				}
				/*
				if (skillName.isEmpty()) {
					Minecraft.getInstance().gui.getChat().addMessage(Component.literal("Slot " + (i + 1) + " is empty!"));
				}
				*/
				ResourceLocation icon = getSkillTexture(skillName);
				event.getGuiGraphics().blit(icon, w / 2 + slotXOffsets[i], h - 21, 0, 0, 20, 20, 20, 20);
			}
			if (SelectedCon1Procedure.execute(entity)) {
				event.getGuiGraphics().blit(new ResourceLocation("sololeveling:textures/screens/icon_frame.png"), w / 2 + -90, h - 22, 0, 0, 22, 22, 22, 22);
			}
			if (SelectedCon2Procedure.execute(entity)) {
				event.getGuiGraphics().blit(new ResourceLocation("sololeveling:textures/screens/icon_frame.png"), w / 2 + -70, h - 22, 0, 0, 22, 22, 22, 22);
			}
			if (SelectedCon3Procedure.execute(entity)) {
				event.getGuiGraphics().blit(new ResourceLocation("sololeveling:textures/screens/icon_frame.png"), w / 2 + -50, h - 22, 0, 0, 22, 22, 22, 22);
			}
			if (SelectedCon4Procedure.execute(entity)) {
				event.getGuiGraphics().blit(new ResourceLocation("sololeveling:textures/screens/icon_frame.png"), w / 2 + -30, h - 22, 0, 0, 22, 22, 22, 22);
			}
			if (SelectedCon5Procedure.execute(entity)) {
				event.getGuiGraphics().blit(new ResourceLocation("sololeveling:textures/screens/icon_frame.png"), w / 2 + -10, h - 22, 0, 0, 22, 22, 22, 22);
			}
			if (SelectedCon6Procedure.execute(entity)) {
				event.getGuiGraphics().blit(new ResourceLocation("sololeveling:textures/screens/icon_frame.png"), w / 2 + 10, h - 22, 0, 0, 22, 22, 22, 22);
			}
			if (SlectedCon7Procedure.execute(entity)) {
				event.getGuiGraphics().blit(new ResourceLocation("sololeveling:textures/screens/icon_frame.png"), w / 2 + 30, h - 22, 0, 0, 22, 22, 22, 22);
			}
			if (SlectedCon8Procedure.execute(entity)) {
				event.getGuiGraphics().blit(new ResourceLocation("sololeveling:textures/screens/icon_frame.png"), w / 2 + 50, h - 22, 0, 0, 22, 22, 22, 22);
			}
			event.getGuiGraphics().drawString(Minecraft.getInstance().font, ReturnCooldownAmountProcedure.execute(entity), w / 2 + 74, h - 12, -26266, false);
			event.getGuiGraphics().drawString(Minecraft.getInstance().font, SkillTextProcedure.execute(entity), w / 2 + 74, h - 22, -26266, false);
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
