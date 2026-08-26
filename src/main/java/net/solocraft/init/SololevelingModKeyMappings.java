package net.solocraft.init;

import org.lwjgl.glfw.GLFW;

import net.solocraft.network.UseSkillMessage;
import net.solocraft.network.TripleJumpMessage;
import net.solocraft.network.SkillCycleButtonMessage;
import net.solocraft.network.QuestInfoMessage;
import net.solocraft.network.DMessage;
import net.solocraft.network.Ability4Message;
import net.solocraft.network.Ability3Message;
import net.solocraft.network.Ability2Message;
import net.solocraft.network.Ability1Message;
import net.solocraft.network.Ab8Message;
import net.solocraft.network.Ab7Message;
import net.solocraft.network.Ab6Message;
import net.solocraft.network.Ab5Message;
import net.solocraft.network.Ab4Message;
import net.solocraft.network.Ab3Message;
import net.solocraft.network.Ab2Message;
import net.solocraft.network.Ab1Message;
import net.solocraft.network.SololevelingModVariables;
import net.solocraft.network.AbilitiesGUIButtonMessage;
import net.solocraft.util.SystemPlayerAccess;
import net.solocraft.util.DungeonBuilderMode;
import net.solocraft.client.gui.DkcQuestProgressClientState;
import net.solocraft.client.gui.CurseWheelClientState;
import net.solocraft.client.gui.FrostArchitectureClientState;
import net.solocraft.client.gui.dungeonbuilder.DungeonBuilderStudioClient;
import net.solocraft.SololevelingMod;

import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.api.distmarker.Dist;

import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import com.mojang.blaze3d.platform.InputConstants;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD, value = {Dist.CLIENT})
public class SololevelingModKeyMappings {
	public static final KeyMapping OPEN_PANEL = new KeyMapping("key.sololeveling.open_panel", GLFW.GLFW_KEY_N, "key.categories.sololeveling") {
		private boolean isDownOld = false;

		@Override
		public void setDown(boolean isDown) {
			super.setDown(isDown);
			if (isDownOld != isDown && isDown) {
				Minecraft mc = Minecraft.getInstance();
				if (mc.player != null && mc.screen == null) {
					if (DungeonBuilderMode.isActive(mc.level)) {
						DungeonBuilderStudioClient.requestOpen();
					} else if (SystemPlayerAccess.hasSystem(mc.player)) {
						mc.setScreen(new net.solocraft.client.gui.system.SystemPanelScreen());
					} else {
						var pos = mc.player.blockPosition();
						SololevelingMod.PACKET_HANDLER.sendToServer(
								new AbilitiesGUIButtonMessage(5, pos.getX(), pos.getY(), pos.getZ()));
					}
				}
			}
			isDownOld = isDown;
		}
	};
	public static final KeyMapping SKILL_CYCLE_BUTTON = new KeyMapping("key.sololeveling.skill_cycle_button", GLFW.GLFW_KEY_R, "key.categories.sololeveling") {
		private boolean isDownOld = false;

		@Override
		public void setDown(boolean isDown) {
			super.setDown(isDown);
			if (isDownOld != isDown && isDown) {
				SololevelingMod.PACKET_HANDLER.sendToServer(new SkillCycleButtonMessage(0, 0));
				SkillCycleButtonMessage.pressAction(Minecraft.getInstance().player, 0, 0);
			}
			isDownOld = isDown;
		}
	};
	public static final KeyMapping USE_SKILL = new KeyMapping("key.sololeveling.use_skill", GLFW.GLFW_KEY_Z, "key.categories.sololeveling") {
		private boolean isDownOld = false;

		@Override
		public void setDown(boolean isDown) {
			super.setDown(isDown);
			if (isDownOld != isDown && isDown) {
				SololevelingMod.PACKET_HANDLER.sendToServer(new UseSkillMessage(0, 0));
				UseSkillMessage.pressAction(Minecraft.getInstance().player, 0, 0);
				USE_SKILL_LASTPRESS = System.currentTimeMillis();
			} else if (isDownOld != isDown && !isDown) {
				int dt = (int) (System.currentTimeMillis() - USE_SKILL_LASTPRESS);
				SololevelingMod.PACKET_HANDLER.sendToServer(new UseSkillMessage(1, dt));
				UseSkillMessage.pressAction(Minecraft.getInstance().player, 1, dt);
			}
			isDownOld = isDown;
		}
	};
	public static final KeyMapping D = new KeyMapping("key.sololeveling.d", InputConstants.Type.MOUSE, GLFW.GLFW_MOUSE_BUTTON_RIGHT, "key.categories.sololeveling") {
		private boolean isDownOld = false;

		@Override
		public void setDown(boolean isDown) {
			super.setDown(isDown);
			if (isDownOld != isDown && isDown) {
				SololevelingMod.PACKET_HANDLER.sendToServer(new DMessage(0, 0));
				DMessage.pressAction(Minecraft.getInstance().player, 0, 0);
				D_LASTPRESS = System.currentTimeMillis();
			} else if (isDownOld != isDown && !isDown) {
				int dt = (int) (System.currentTimeMillis() - D_LASTPRESS);
				SololevelingMod.PACKET_HANDLER.sendToServer(new DMessage(1, dt));
				DMessage.pressAction(Minecraft.getInstance().player, 1, dt);
			}
			isDownOld = isDown;
		}
	};
	public static final KeyMapping ABILITY_1 = new KeyMapping("key.sololeveling.ability_1", GLFW.GLFW_KEY_X, "key.categories.sololeveling") {
		private boolean isDownOld = false;

		@Override
		public void setDown(boolean isDown) {
			super.setDown(isDown);
			if (isDownOld != isDown && isDown) {
				if (canUseAbilityKeys()) {
					SololevelingMod.PACKET_HANDLER.sendToServer(new Ability1Message(0, 0));
					Ability1Message.pressAction(Minecraft.getInstance().player, 0, 0);
					ABILITY_1_LASTPRESS = System.currentTimeMillis();
				}
			} else if (isDownOld != isDown && !isDown) {
				if (canUseAbilityKeys()) {
					int dt = (int) (System.currentTimeMillis() - ABILITY_1_LASTPRESS);
					SololevelingMod.PACKET_HANDLER.sendToServer(new Ability1Message(1, dt));
					Ability1Message.pressAction(Minecraft.getInstance().player, 1, dt);
				}
			}
			isDownOld = isDown;
		}
	};
	public static final KeyMapping ABILITY_2 = new KeyMapping("key.sololeveling.ability_2", GLFW.GLFW_KEY_C, "key.categories.sololeveling") {
		private boolean isDownOld = false;

		@Override
		public void setDown(boolean isDown) {
			super.setDown(isDown);
			if (isDownOld != isDown && isDown) {
				if (canUseAbilityKeys()) {
					SololevelingMod.PACKET_HANDLER.sendToServer(new Ability2Message(0, 0));
					ABILITY_2_LASTPRESS = System.currentTimeMillis();
				}
			} else if (isDownOld != isDown && !isDown) {
				if (canUseAbilityKeys()) {
					int dt = (int) Math.min(Integer.MAX_VALUE, System.currentTimeMillis() - ABILITY_2_LASTPRESS);
					SololevelingMod.PACKET_HANDLER.sendToServer(new Ability2Message(1, dt));
				}
			}
			isDownOld = isDown;
		}
	};
	public static final KeyMapping ABILITY_3 = new KeyMapping("key.sololeveling.ability_3", GLFW.GLFW_KEY_V, "key.categories.sololeveling") {
		private boolean isDownOld = false;

		@Override
		public void setDown(boolean isDown) {
			super.setDown(isDown);
			if (isDownOld != isDown && isDown) {
				if (canUseAbilityKeys()) {
					SololevelingMod.PACKET_HANDLER.sendToServer(new Ability3Message(0, 0));
					Ability3Message.pressAction(Minecraft.getInstance().player, 0, 0);
				}
			}
			isDownOld = isDown;
		}
	};
	public static final KeyMapping ABILITY_4 = new KeyMapping("key.sololeveling.ability_4", GLFW.GLFW_KEY_B, "key.categories.sololeveling") {
		private boolean isDownOld = false;

		@Override
		public void setDown(boolean isDown) {
			super.setDown(isDown);
			if (isDownOld != isDown && isDown) {
				if (canUseAbilityKeys()) {
					SololevelingMod.PACKET_HANDLER.sendToServer(new Ability4Message(0, 0));
					Ability4Message.pressAction(Minecraft.getInstance().player, 0, 0);
				}
			}
			isDownOld = isDown;
		}
	};
	public static final KeyMapping TRIPLE_JUMP = new KeyMapping("key.sololeveling.triple_jump", GLFW.GLFW_KEY_SPACE, "key.categories.sololeveling") {
		private boolean isDownOld = false;

		@Override
		public void setDown(boolean isDown) {
			super.setDown(isDown);
			if (isDownOld != isDown && isDown) {
				Player player = Minecraft.getInstance().player;
				Vec3 motion = player != null ? player.getDeltaMovement() : Vec3.ZERO;
				SololevelingMod.PACKET_HANDLER.sendToServer(new TripleJumpMessage(0, 0, motion.x, motion.z));
			}
			isDownOld = isDown;
		}
	};
	public static final KeyMapping QUEST_INFO = new KeyMapping("key.sololeveling.quest_info", GLFW.GLFW_KEY_TAB, "key.categories.sololeveling") {
		private boolean isDownOld = false;

		@Override
		public void setDown(boolean isDown) {
			super.setDown(isDown);
			if (isDownOld != isDown && isDown) {
				SololevelingMod.PACKET_HANDLER.sendToServer(new QuestInfoMessage(0, 0));
				QuestInfoMessage.pressAction(Minecraft.getInstance().player, 0, 0);
				QUEST_INFO_LASTPRESS = System.currentTimeMillis();
			} else if (isDownOld != isDown && !isDown) {
				int dt = (int) (System.currentTimeMillis() - QUEST_INFO_LASTPRESS);
				SololevelingMod.PACKET_HANDLER.sendToServer(new QuestInfoMessage(1, dt));
				QuestInfoMessage.pressAction(Minecraft.getInstance().player, 1, dt);
				DkcQuestProgressClientState.clear();
			}
			isDownOld = isDown;
		}
	};
	public static final KeyMapping DDD = new KeyMapping("key.sololeveling.ddd", InputConstants.Type.MOUSE, GLFW.GLFW_MOUSE_BUTTON_LEFT, "key.categories.misc");
	private static long USE_SKILL_LASTPRESS = 0;
	private static long D_LASTPRESS = 0;
	private static long ABILITY_1_LASTPRESS = 0;
	private static long ABILITY_2_LASTPRESS = 0;
	private static long QUEST_INFO_LASTPRESS = 0;
	private static final long[] HOTBAR_LASTPRESS = new long[8];

	public static void pressHotbarSkill(int slot) {
		int type = 9 + slot;
		HOTBAR_LASTPRESS[slot - 1] = System.currentTimeMillis();
		SololevelingMod.PACKET_HANDLER.sendToServer(new UseSkillMessage(type, 0));
		UseSkillMessage.pressAction(Minecraft.getInstance().player, type, 0);
	}

	public static void releaseHotbarSkill(int slot) {
		int type = 19 + slot;
		int dt = (int) (System.currentTimeMillis() - HOTBAR_LASTPRESS[slot - 1]);
		FrostArchitectureClientState.releaseAndSend();
		CurseWheelClientState.releaseAndSend();
		SololevelingMod.PACKET_HANDLER.sendToServer(new UseSkillMessage(type, dt));
		UseSkillMessage.pressAction(Minecraft.getInstance().player, type, dt);
	}

	private static boolean canUseAbilityKeys() {
		if (Minecraft.getInstance().player == null)
			return false;
		SololevelingModVariables.PlayerVariables variables = Minecraft.getInstance().player
				.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
				.orElse(new SololevelingModVariables.PlayerVariables());
		return variables.combatmode || (int) variables.JOB == 3;
	}

	@SubscribeEvent
	public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
		event.register(OPEN_PANEL);
		event.register(SKILL_CYCLE_BUTTON);
		event.register(USE_SKILL);
		event.register(D);
		event.register(ABILITY_1);
		event.register(ABILITY_2);
		event.register(ABILITY_3);
		event.register(ABILITY_4);
		event.register(TRIPLE_JUMP);
		event.register(QUEST_INFO);
		event.register(DDD);
	}

	@EventBusSubscriber({Dist.CLIENT})
	public static class KeyEventListener {
		@SubscribeEvent
		public static void onClientTick(ClientTickEvent.Post event) {
			if (Minecraft.getInstance().screen == null) {
				OPEN_PANEL.consumeClick();
				SKILL_CYCLE_BUTTON.consumeClick();
				USE_SKILL.consumeClick();
				D.consumeClick();
				ABILITY_1.consumeClick();
				ABILITY_2.consumeClick();
				ABILITY_3.consumeClick();
				ABILITY_4.consumeClick();
				TRIPLE_JUMP.consumeClick();
				QUEST_INFO.consumeClick();
			}
		}
	}
}
