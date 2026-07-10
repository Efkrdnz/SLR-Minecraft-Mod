package net.solocraft.client.gui.system;

import net.solocraft.SololevelingMod;
import net.solocraft.network.TrainingGUIButtonMessage;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

/** System-styled Training screen — spawns a level-scaled training bot via the
 *  existing {@link TrainingGUIButtonMessage} (id 1 = spawn). */
public class SystemTrainScreen extends SystemScreen {

	private static final String[] INFO = {
			"Spawn a training bot to test",
			"your skills on and level up.",
			"",
			"The bot's strength scales with",
			"your current level."
	};

	public SystemTrainScreen() {
		super(Component.literal("TRAINING"));
		this.panelW = 200;
		this.panelH = 150;
	}

	@Override
	protected void init() {
		super.init();
		addRenderableWidget(new SystemButton(panelX + 3, panelY + 3, 40, 12, Component.literal("< Back"), b -> openChild(new SystemPanelScreen())));

		int bw = 130, bh = 22;
		addRenderableWidget(new SystemButton(panelX + (panelW - bw) / 2, panelY + panelH - 32, bw, bh, Component.literal("Spawn Training Bot"), b -> spawn()));
	}

	private void spawn() {
		Player player = Minecraft.getInstance().player;
		if (player == null)
			return;
		BlockPos bp = player.blockPosition();
		SololevelingMod.PACKET_HANDLER.sendToServer(new TrainingGUIButtonMessage(1, bp.getX(), bp.getY(), bp.getZ()));
	}

	@Override
	protected void renderContent(GuiGraphics g, int mouseX, int mouseY, float partialTicks) {
		int y = panelY + 30;
		for (String line : INFO) {
			if (!line.isEmpty())
				g.drawString(this.font, line, panelX + (panelW - this.font.width(line)) / 2, y, TEXT_SUB, false);
			y += 12;
		}
	}
}
