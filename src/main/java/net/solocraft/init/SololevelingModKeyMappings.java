package net.solocraft.init;

import org.lwjgl.glfw.GLFW;

import net.solocraft.network.UseSkillMessage;
import net.solocraft.network.TripleJumpMessage;
import net.solocraft.network.TrainingMessage;
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
import net.solocraft.client.gui.dungeonbuilder.DungeonBuilderStudioClient;
import net.solocraft.SololevelingMod;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.api.distmarker.Dist;

import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import com.mojang.blaze3d.platform.InputConstants;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD, value = {Dist.CLIENT})
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
	public static final KeyMapping AB_1 = new KeyMapping("key.sololeveling.ab_1", GLFW.GLFW_KEY_1, "key.categories.sololeveling") {
		private boolean isDownOld = false;

		@Override
		public void setDown(boolean isDown) {
			super.setDown(isDown);
			if (isDownOld != isDown && isDown) {
				pressHotbarSkill(1);
			} else if (isDownOld != isDown && !isDown) {
				releaseHotbarSkill(1);
			}
			isDownOld = isDown;
		}
	};
	public static final KeyMapping AB_2 = new KeyMapping("key.sololeveling.ab_2", GLFW.GLFW_KEY_2, "key.categories.sololeveling") {
		private boolean isDownOld = false;

		@Override
		public void setDown(boolean isDown) {
			super.setDown(isDown);
			if (isDownOld != isDown && isDown) {
				pressHotbarSkill(2);
			} else if (isDownOld != isDown && !isDown) {
				releaseHotbarSkill(2);
			}
			isDownOld = isDown;
		}
	};
	public static final KeyMapping AB_3 = new KeyMapping("key.sololeveling.ab_3", GLFW.GLFW_KEY_3, "key.categories.sololeveling") {
		private boolean isDownOld = false;

		@Override
		public void setDown(boolean isDown) {
			super.setDown(isDown);
			if (isDownOld != isDown && isDown) {
				pressHotbarSkill(3);
			} else if (isDownOld != isDown && !isDown) {
				releaseHotbarSkill(3);
			}
			isDownOld = isDown;
		}
	};
	public static final KeyMapping AB_4 = new KeyMapping("key.sololeveling.ab_4", GLFW.GLFW_KEY_4, "key.categories.sololeveling") {
		private boolean isDownOld = false;

		@Override
		public void setDown(boolean isDown) {
			super.setDown(isDown);
			if (isDownOld != isDown && isDown) {
				pressHotbarSkill(4);
			} else if (isDownOld != isDown && !isDown) {
				releaseHotbarSkill(4);
			}
			isDownOld = isDown;
		}
	};
	public static final KeyMapping AB_5 = new KeyMapping("key.sololeveling.ab_5", GLFW.GLFW_KEY_5, "key.categories.sololeveling") {
		private boolean isDownOld = false;

		@Override
		public void setDown(boolean isDown) {
			super.setDown(isDown);
			if (isDownOld != isDown && isDown) {
				pressHotbarSkill(5);
			} else if (isDownOld != isDown && !isDown) {
				releaseHotbarSkill(5);
			}
			isDownOld = isDown;
		}
	};
	public static final KeyMapping AB_6 = new KeyMapping("key.sololeveling.ab_6", GLFW.GLFW_KEY_6, "key.categories.sololeveling") {
		private boolean isDownOld = false;

		@Override
		public void setDown(boolean isDown) {
			super.setDown(isDown);
			if (isDownOld != isDown && isDown) {
				pressHotbarSkill(6);
			} else if (isDownOld != isDown && !isDown) {
				releaseHotbarSkill(6);
			}
			isDownOld = isDown;
		}
	};
	public static final KeyMapping TRAINING = new KeyMapping("key.sololeveling.training", GLFW.GLFW_KEY_K, "key.categories.sololeveling") {
		private boolean isDownOld = false;

		@Override
		public void setDown(boolean isDown) {
			super.setDown(isDown);
			if (isDownOld != isDown && isDown) {
				SololevelingMod.PACKET_HANDLER.sendToServer(new TrainingMessage(0, 0));
				TrainingMessage.pressAction(Minecraft.getInstance().player, 0, 0);
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
			}
			isDownOld = isDown;
		}
	};
	public static final KeyMapping AB_7 = new KeyMapping("key.sololeveling.ab_7", GLFW.GLFW_KEY_7, "key.categories.sololeveling") {
		private boolean isDownOld = false;

		@Override
		public void setDown(boolean isDown) {
			super.setDown(isDown);
			if (isDownOld != isDown && isDown) {
				pressHotbarSkill(7);
			} else if (isDownOld != isDown && !isDown) {
				releaseHotbarSkill(7);
			}
			isDownOld = isDown;
		}
	};
	public static final KeyMapping AB_8 = new KeyMapping("key.sololeveling.ab_8", GLFW.GLFW_KEY_8, "key.categories.sololeveling") {
		private boolean isDownOld = false;

		@Override
		public void setDown(boolean isDown) {
			super.setDown(isDown);
			if (isDownOld != isDown && isDown) {
				pressHotbarSkill(8);
			} else if (isDownOld != isDown && !isDown) {
				releaseHotbarSkill(8);
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

	private static void pressHotbarSkill(int slot) {
		int type = 9 + slot;
		HOTBAR_LASTPRESS[slot - 1] = System.currentTimeMillis();
		SololevelingMod.PACKET_HANDLER.sendToServer(new UseSkillMessage(type, 0));
		UseSkillMessage.pressAction(Minecraft.getInstance().player, type, 0);
	}

	private static void releaseHotbarSkill(int slot) {
		int type = 19 + slot;
		int dt = (int) (System.currentTimeMillis() - HOTBAR_LASTPRESS[slot - 1]);
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
		event.register(AB_1);
		event.register(AB_2);
		event.register(AB_3);
		event.register(AB_4);
		event.register(AB_5);
		event.register(AB_6);
		event.register(TRAINING);
		event.register(QUEST_INFO);
		event.register(AB_7);
		event.register(AB_8);
		event.register(DDD);
	}

	@Mod.EventBusSubscriber({Dist.CLIENT})
	public static class KeyEventListener {
		@SubscribeEvent
		public static void onClientTick(TickEvent.ClientTickEvent event) {
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
				AB_1.consumeClick();
				AB_2.consumeClick();
				AB_3.consumeClick();
				AB_4.consumeClick();
				AB_5.consumeClick();
				AB_6.consumeClick();
				TRAINING.consumeClick();
				QUEST_INFO.consumeClick();
				AB_7.consumeClick();
				AB_8.consumeClick();
			}
		}
	}
}
