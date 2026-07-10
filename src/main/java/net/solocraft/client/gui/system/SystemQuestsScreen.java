package net.solocraft.client.gui.system;

import net.solocraft.SololevelingMod;
import net.solocraft.network.QuestsButtonMessage;
import net.solocraft.util.DkcQuestManager;
import net.solocraft.util.JobChangeQuestManager;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

/** System-styled Quests hub — Daily Quests and the Demon King's Castle path,
 *  reusing the existing {@link QuestsButtonMessage} routing. */
public class SystemQuestsScreen extends SystemScreen {

	public SystemQuestsScreen() {
		super(Component.literal("QUESTS"));
		this.panelW = 190;
		this.panelH = 154;
	}

	@Override
	protected void init() {
		super.init();
		addRenderableWidget(new SystemButton(panelX + 3, panelY + 3, 40, 12, Component.literal("< Back"), b -> openChild(new SystemPanelScreen())));

		int bw = 150, bh = 24;
		int x = panelX + (panelW - bw) / 2;
		int y = panelY + 40;
		addRenderableWidget(new SystemButton(x, y, bw, bh, Component.literal("Daily Quests"), b -> sendQuest(0)));
		y += 32;
		Player player = Minecraft.getInstance().player;
		if (JobChangeQuestManager.isVisible(player)) {
			addRenderableWidget(new SystemButton(x, y, bw, bh, Component.literal("Job Change Quest"), b -> sendQuest(2)));
			y += 32;
		}
		if (DkcQuestManager.isVisible(player))
			addRenderableWidget(new SystemButton(x, y, bw, bh, Component.literal("Demon King's Castle"), b -> sendQuest(1)));
	}

	private void sendQuest(int id) {
		Player player = Minecraft.getInstance().player;
		if (player == null)
			return;
		SystemGuiSounds.exit();
		BlockPos bp = player.blockPosition();
		SololevelingMod.PACKET_HANDLER.sendToServer(new QuestsButtonMessage(id, bp.getX(), bp.getY(), bp.getZ()));
	}

	@Override
	protected void renderContent(GuiGraphics g, int mouseX, int mouseY, float partialTicks) {
		String sub = "Choose an objective";
		g.drawString(this.font, sub, panelX + (panelW - this.font.width(sub)) / 2, panelY + 26, TEXT_SUB, false);
	}
}
