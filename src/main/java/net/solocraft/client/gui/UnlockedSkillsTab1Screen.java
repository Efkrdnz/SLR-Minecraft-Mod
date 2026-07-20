package net.solocraft.client.gui;

import net.solocraft.SololevelingMod;
import net.solocraft.client.gui.system.SystemContainerScreen;
import net.solocraft.client.gui.system.SystemScreen;
import net.solocraft.client.gui.system.SystemTooltip;
import net.solocraft.network.SololevelingModVariables;
import net.solocraft.network.UnlockedSkillsTab1ButtonMessage;
import net.solocraft.procedures.SkillSlotHelper;
import net.solocraft.util.ShadowMonarchManager;
import net.solocraft.util.SkillCategoryRegistry;
import net.solocraft.util.SkillListHelper;
import net.solocraft.util.JobSkillManager;
import net.solocraft.world.inventory.UnlockedSkillsTab1Menu;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import com.mojang.blaze3d.systems.RenderSystem;

import java.util.List;

public class UnlockedSkillsTab1Screen extends SystemContainerScreen<UnlockedSkillsTab1Menu> {
	private static final int ROWS = 8;
	private static final int ROW_H = 23;

	private final Level world;
	private final int x, y, z;
	private final Player entity;
	private final SystemScreen.SystemButton[] equipButtons = new SystemScreen.SystemButton[ROWS];
	private final SystemScreen.SystemButton[] removeButtons = new SystemScreen.SystemButton[ROWS];
	private SystemScreen.SystemButton previousButton;
	private SystemScreen.SystemButton nextButton;
	private int page = 0;

	public UnlockedSkillsTab1Screen(UnlockedSkillsTab1Menu container, Inventory inventory, Component text) {
		super(container, inventory, text);
		this.world = container.world;
		this.x = container.x;
		this.y = container.y;
		this.z = container.z;
		this.entity = container.entity;
		this.imageWidth = 0;
		this.imageHeight = 0;
		this.pRelX = -124;
		this.pRelY = -126;
		this.pW = 248;
		this.pH = 252;
	}

	@Override
	protected void renderBg(GuiGraphics g, float partialTicks, int gx, int gy) {
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		ShopStyle.panel(g, leftPos + pRelX, topPos + pRelY, pW, pH);
		ShopStyle.titleBar(g, this.font, leftPos + pRelX, topPos + pRelY, pW, "SKILL LIST");

		int startY = topPos + pRelY + 42;
		for (int i = 0; i < ROWS; i++) {
			int y0 = startY + i * ROW_H;
			int fill = i % 2 == 0 ? 0x33102338 : 0x22102338;
			g.fill(leftPos + pRelX + 10, y0, leftPos + pRelX + pW - 10, y0 + 20, fill);
			g.fill(leftPos + pRelX + 10, y0 + 20, leftPos + pRelX + pW - 10, y0 + 21, ShopStyle.ACCENT_SOFT);
		}
		RenderSystem.disableBlend();
	}

	@Override
	protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
		clampPage();
		SololevelingModVariables.PlayerVariables vars = entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables());
		int selectedSlot = (int) vars.PslotSelecting;
		String current = selectedSlot > 0 ? SkillSlotHelper.getSlot(vars, selectedSlot) : "";
		String target = selectedSlot > 0 ? "TARGET SLOT " + selectedSlot : "TARGET SLOT";
		g.drawString(this.font, target, pRelX + 12, pRelY + 22, ShopStyle.ACCENT, false);
		String currentLabel = current == null || current.isBlank() ? "Empty" : ShadowMonarchManager.displaySkillName(entity, current);
		Component currentText = current == null || current.isBlank()
				? Component.literal(currentLabel)
				: SkillCategoryRegistry.decorate(current, currentLabel);
		if (JobSkillManager.isWhiteFlameSkill(current))
			currentText = currentText.copy().withStyle(ChatFormatting.BOLD);
		g.drawString(this.font, currentText, pRelX + 12, pRelY + 33,
				JobSkillManager.isWhiteFlameSkill(current) ? 0xFFFFFFFF : ShopStyle.TEXT_SUB, false);

		int pageCount = SkillListHelper.pageCount(entity, ROWS);
		for (int i = 0; i < ROWS; i++) {
			int index = page * ROWS + i + 1;
			String raw = SkillListHelper.rawSkillAt(entity, index);
			String label = SkillListHelper.displaySkillAt(entity, index);
			int color = SkillListHelper.colorAt(entity, index);
			int rowY = pRelY + 47 + i * ROW_H;
			g.drawString(this.font, index < 10 ? "0" + index : String.valueOf(index), pRelX + 16, rowY, 0xFF8FB8D8, false);
			int availableWidth = ShadowMonarchManager.isFormationSkill(raw) ? pW - 136 : pW - 104;
			Component skillText = "empty".equals(raw)
					? Component.literal("-")
					: fitSkillText(raw, label, availableWidth);
			if (JobSkillManager.isWhiteFlameSkill(raw))
				skillText = skillText.copy().withStyle(ChatFormatting.BOLD);
			g.drawString(this.font, skillText, pRelX + 44, rowY,
					"empty".equals(raw) ? 0xFF566A7A : JobSkillManager.isWhiteFlameSkill(raw) ? 0xFFFFFFFF : color, false);
		}
		String pageText = "Page " + (page + 1) + "/" + pageCount;
		g.drawString(this.font, pageText, pRelX + (pW - this.font.width(pageText)) / 2, pRelY + pH - 19, ShopStyle.TEXT_SUB, false);
	}

	@Override
	protected void renderExtras(GuiGraphics g, int mouseX, int mouseY) {
		for (int i = 0; i < ROWS; i++) {
			int index = page * ROWS + i + 1;
			String raw = SkillListHelper.rawSkillAt(entity, index);
			if ("empty".equals(raw))
				continue;
			int y0 = topPos + pRelY + 42 + i * ROW_H;
			if (mouseX >= leftPos + pRelX + 10 && mouseX < leftPos + pRelX + pW - 10 && mouseY >= y0 && mouseY < y0 + 20) {
				SystemTooltip.render(g, this.font, JobSkillManager.tooltip(entity, raw), mouseX, mouseY, this.width, this.height);
				return;
			}
		}
	}

	@Override
	public void containerTick() {
		super.containerTick();
		refreshButtons();
	}

	@Override
	public void init() {
		super.init();
		this.addRenderableWidget(new SystemScreen.SystemButton(leftPos + pRelX + 3, topPos + pRelY + 2, 40, 12, Component.literal("< Back"), b -> openSlotScreen()));

		int startY = topPos + pRelY + 43;
		for (int i = 0; i < ROWS; i++) {
			final int row = i;
			int by = startY + i * ROW_H;
			removeButtons[i] = new SystemScreen.SystemButton(leftPos + pRelX + pW - 88, by, 28, 18, Component.literal("X"), b -> removeFormation(row));
			equipButtons[i] = new SystemScreen.SystemButton(leftPos + pRelX + pW - 56, by, 44, 18, Component.literal("Equip"), b -> equip(row));
			this.addRenderableWidget(removeButtons[i]);
			this.addRenderableWidget(equipButtons[i]);
		}

		previousButton = new SystemScreen.SystemButton(leftPos + pRelX + 12, topPos + pRelY + pH - 27, 42, 18, Component.literal("<"), b -> {
			if (page > 0)
				page--;
			refreshButtons();
		});
		nextButton = new SystemScreen.SystemButton(leftPos + pRelX + pW - 54, topPos + pRelY + pH - 27, 42, 18, Component.literal(">"), b -> {
			int maxPage = SkillListHelper.pageCount(entity, ROWS) - 1;
			if (page < maxPage)
				page++;
			refreshButtons();
		});
		this.addRenderableWidget(previousButton);
		this.addRenderableWidget(nextButton);
		refreshButtons();
	}

	private Component fitSkillText(String rawSkill, String label, int maxWidth) {
		Component result = SkillCategoryRegistry.decorate(rawSkill, label);
		if (this.font.width(result) <= maxWidth)
			return result;
		String shortened = label;
		while (!shortened.isEmpty()) {
			shortened = shortened.substring(0, shortened.length() - 1);
			result = SkillCategoryRegistry.decorate(rawSkill, shortened + "...");
			if (this.font.width(result) <= maxWidth)
				return result;
		}
		return SkillCategoryRegistry.decorate(rawSkill, "...");
	}

	@Override
	protected boolean allowsNonSystemAccess() {
		return true;
	}

	private void openSlotScreen() {
		SololevelingMod.PACKET_HANDLER.sendToServer(new UnlockedSkillsTab1ButtonMessage(101, x, y, z, 0));
	}

	private void equip(int row) {
		int index = page * ROWS + row + 1;
		if (!"empty".equals(SkillListHelper.rawSkillAt(entity, index)))
			SololevelingMod.PACKET_HANDLER.sendToServer(new UnlockedSkillsTab1ButtonMessage(row, x, y, z, index));
	}

	private void removeFormation(int row) {
		int index = page * ROWS + row + 1;
		if (ShadowMonarchManager.isFormationSkill(SkillListHelper.rawSkillAt(entity, index)))
			SololevelingMod.PACKET_HANDLER.sendToServer(new UnlockedSkillsTab1ButtonMessage(100, x, y, z, index));
	}

	private void refreshButtons() {
		clampPage();
		int pageCount = SkillListHelper.pageCount(entity, ROWS);
		if (previousButton != null)
			previousButton.visible = isOpen() && page > 0;
		if (nextButton != null)
			nextButton.visible = isOpen() && page + 1 < pageCount;
		for (int i = 0; i < ROWS; i++) {
			int index = page * ROWS + i + 1;
			String raw = SkillListHelper.rawSkillAt(entity, index);
			boolean hasSkill = !"empty".equals(raw);
			boolean formation = ShadowMonarchManager.isFormationSkill(raw);
			if (equipButtons[i] != null) {
				equipButtons[i].visible = isOpen() && hasSkill;
				equipButtons[i].active = hasSkill;
			}
			if (removeButtons[i] != null) {
				removeButtons[i].visible = isOpen() && formation;
				removeButtons[i].active = formation;
			}
		}
	}

	private void clampPage() {
		int pageCount = SkillListHelper.pageCount(entity, ROWS);
		if (page >= pageCount)
			page = pageCount - 1;
		if (page < 0)
			page = 0;
	}
}
