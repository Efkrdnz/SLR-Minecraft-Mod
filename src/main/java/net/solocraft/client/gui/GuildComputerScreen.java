package net.solocraft.client.gui;

import net.solocraft.client.gui.system.SystemTooltip;
import net.solocraft.guild.GuildData;
import net.solocraft.guild.GuildBuffRegistry;
import net.solocraft.guild.GuildDeployment;
import net.solocraft.guild.GuildHunter;
import net.solocraft.guild.GuildMemberPermissions;
import net.solocraft.network.GuildActionMessage;
import net.solocraft.SololevelingMod;
import net.solocraft.world.inventory.GuildComputerMenu;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@OnlyIn(Dist.CLIENT)
public class GuildComputerScreen extends AbstractContainerScreen<GuildComputerMenu> {

    // ── Layout constants ──────────────────────────────────────────────────────
    private static final int GUI_W  = 340;
    private static final int GUI_H  = 220;
    private static final int TAB_H  = 20;
    private static final int TAB_Y_OFFSET = 4;

    private static final int TAB_OVERVIEW    = 0;
    private static final int TAB_ROSTER      = 1;
    private static final int TAB_TEAMS       = 2;
    private static final int TAB_DUNGEONS    = 3;
    private static final int TAB_STORAGE     = 4;
    private static final int TAB_BUFFS       = 5;
    private static final int TAB_LEADERBOARD = 6;
    private static final int TAB_MANAGEMENT  = 7;

    private static final String[] TAB_LABELS = {
        "Overview", "Roster", "Teams", "Dungeons", "Storage", "Buffs", "Leaderboard", "Manage"
    };

    private static final int DIVIDER_X = 166; // relative to guiLeft
    private static UUID pendingGuildId = null;
    private static int pendingActiveTab = -1;
    private static UUID pendingSelectedTeamId = null;
    private static int pendingDeployTeamIdx = 0;

    // ── State ─────────────────────────────────────────────────────────────────
    private int  activeTab = TAB_OVERVIEW;
    private int  guiLeft, guiTop;

    // Create-guild form
    private EditBox nameBox;
    private Button  createBtn;

    // Management tab
    private Button  addMemberBtn;
    private EditBox addMemberBox;
    private Button  deleteGuildBtn;
    private int     managementScrollOffset = 0;

    // Roster tab
    private int rosterScrollOffset = 0;

    // Teams tab
    private UUID selectedTeamId          = null;
    private int  availableHuntersScroll  = 0;

    // Dungeons tab
    private int  deployTeamIdx  = 0;  // index into menu.teams
    private int  deploymentsScroll = 0;
    private long localScreenTicks = 0;

    // Buffs tab
    private int buffsScroll = 0;

    // Tab layout
    private int[] tabXPositions;
    private int[] tabWidths;

    // ── Constructor ───────────────────────────────────────────────────────────

    public GuildComputerScreen(GuildComputerMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth  = GUI_W;
        this.imageHeight = GUI_H;
        this.inventoryLabelX = -9999;
        this.titleLabelX     = -9999;
    }

    // ── Init ──────────────────────────────────────────────────────────────────

    @Override
    protected void init() {
        super.init();
        guiLeft = (width  - GUI_W) / 2;
        guiTop  = (height - GUI_H) / 2;

        computeTabLayout();
        restorePendingViewState();

        nameBox = new EditBox(font, guiLeft + GUI_W / 2 - 80, guiTop + GUI_H / 2 - 10, 160, 20,
                Component.literal("Guild Name"));
        nameBox.setMaxLength(24);
        nameBox.setVisible(false);
        nameBox.setHint(Component.literal("Enter guild name..."));
        nameBox.setBordered(true);
        addWidget(nameBox);

        createBtn = Button.builder(Component.literal("Create Guild"),
                btn -> sendAction("create", nameBox.getValue(), ""))
                .bounds(guiLeft + GUI_W / 2 - 50, guiTop + GUI_H / 2 + 18, 100, 20)
                .build();
        createBtn.visible = false;
        addRenderableWidget(createBtn);

        addMemberBox = new EditBox(font, guiLeft + 10, guiTop + GUI_H - 38, 150, 16,
                Component.literal("Player name"));
        addMemberBox.setMaxLength(32);
        addMemberBox.setVisible(false);
        addMemberBox.setHint(Component.literal("Online player name..."));
        addWidget(addMemberBox);

        addMemberBtn = Button.builder(Component.literal("Send Invite"),
                btn -> {
                    String n = addMemberBox.getValue().trim();
                    if (!n.isEmpty()) {
                        sendAction("invite_member", n, "");
                        addMemberBox.setValue("");
                    }
                })
                .bounds(guiLeft + 165, guiTop + GUI_H - 39, 90, 18)
                .build();
        addMemberBtn.visible = false;
        addRenderableWidget(addMemberBtn);

        deleteGuildBtn = Button.builder(Component.literal("Delete Guild"),
                btn -> sendAction("delete_guild", "", ""))
                .bounds(guiLeft + GUI_W - 82, guiTop + GUI_H - 39, 74, 18)
                .build();
        deleteGuildBtn.visible = false;
        addRenderableWidget(deleteGuildBtn);

        updateWidgetVisibility();
        if (!menu.hasGuild) setInitialFocus(nameBox);
    }

    private void computeTabLayout() {
        tabWidths     = new int[TAB_LABELS.length];
        tabXPositions = new int[TAB_LABELS.length];
        int total = 0;
        for (int i = 0; i < TAB_LABELS.length; i++) { tabWidths[i] = font.width(TAB_LABELS[i]) + 10; total += tabWidths[i]; }
        int bonus = (GUI_W - total) / TAB_LABELS.length;
        int x = guiLeft;
        for (int i = 0; i < TAB_LABELS.length; i++) { tabWidths[i] += bonus; tabXPositions[i] = x; x += tabWidths[i]; }
    }

    private void restorePendingViewState() {
        if (!menu.hasGuild || pendingActiveTab < 0 || pendingGuildId == null || !pendingGuildId.equals(menu.guildId)) return;
        if (canAccessTab(pendingActiveTab)) activeTab = pendingActiveTab;
        if (pendingSelectedTeamId != null && teamExists(pendingSelectedTeamId)) selectedTeamId = pendingSelectedTeamId;
        if (!menu.teams.isEmpty()) deployTeamIdx = Math.max(0, Math.min(pendingDeployTeamIdx, menu.teams.size() - 1));
        pendingGuildId = null;
        pendingActiveTab = -1;
        pendingSelectedTeamId = null;
        pendingDeployTeamIdx = 0;
    }

    private void updateWidgetVisibility() {
        boolean noGuild    = !menu.hasGuild;
        boolean management = menu.hasGuild && activeTab == TAB_MANAGEMENT && menu.viewerIsOwner;

        nameBox.setVisible(noGuild);
        createBtn.visible    = noGuild;
        addMemberBox.setVisible(management);
        addMemberBtn.visible = management;
        deleteGuildBtn.visible = management;

        menu.storageTabActive = menu.hasGuild && activeTab == TAB_STORAGE;
    }

    // ── Key / char handling ───────────────────────────────────────────────────

    @Override
    public boolean keyPressed(int kc, int sc, int mod) {
        if (textBoxFocused()) {
            if (kc == 256) return super.keyPressed(kc, sc, mod);
            if (nameBox.isVisible() && nameBox.isFocused()) nameBox.keyPressed(kc, sc, mod);
            if (addMemberBox.isVisible() && addMemberBox.isFocused()) addMemberBox.keyPressed(kc, sc, mod);
            return true;
        }
        return super.keyPressed(kc, sc, mod);
    }

    @Override
    public boolean charTyped(char c, int mod) {
        if (nameBox.isVisible()     && nameBox.isFocused())     return nameBox.charTyped(c, mod);
        if (addMemberBox.isVisible() && addMemberBox.isFocused()) return addMemberBox.charTyped(c, mod);
        return super.charTyped(c, mod);
    }

    private boolean textBoxFocused() {
        return (nameBox.isVisible() && nameBox.isFocused()) || (addMemberBox.isVisible() && addMemberBox.isFocused());
    }

    // ── Mouse ─────────────────────────────────────────────────────────────────

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
		ResponsiveGuiScale.Transform transform = responsiveTransform();
		mx = transform.logicalX(mx);
		my = transform.logicalY(my);
        if (menu.hasGuild && canAccessAnyTab()) {
            int ty = guiTop + TAB_Y_OFFSET;
            if (my >= ty && my <= ty + TAB_H) {
                for (int i = 0; i < TAB_LABELS.length; i++) {
                    if (mx >= tabXPositions[i] && mx < tabXPositions[i] + tabWidths[i] && canAccessTab(i)) {
                        activeTab = i;
                        rosterScrollOffset = 0;
                        availableHuntersScroll = 0;
                        deploymentsScroll = 0;
                        buffsScroll = 0;
                        updateWidgetVisibility();
                        return true;
                    }
                }
            }
            if (activeTab == TAB_MANAGEMENT && menu.viewerIsOwner) handleManagementClick(mx, my);
            if (activeTab == TAB_ROSTER)                           handleRosterClick(mx, my);
            if (activeTab == TAB_TEAMS  && menu.viewerIsOwner)     handleTeamsClick(mx, my);
            if (activeTab == TAB_DUNGEONS && menu.viewerIsOwner)   handleDungeonsClick(mx, my);
            if (activeTab == TAB_BUFFS && menu.viewerIsOwner)       handleBuffsClick(mx, my);
        }
        return super.mouseClicked(mx, my, btn);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
		ResponsiveGuiScale.Transform transform = responsiveTransform();
		mx = transform.logicalX(mx);
		my = transform.logicalY(my);
        if (activeTab == TAB_MANAGEMENT)  { managementScrollOffset  = Math.max(0, managementScrollOffset  - (int)(delta*6)); return true; }
        if (activeTab == TAB_ROSTER)      { rosterScrollOffset       = Math.max(0, rosterScrollOffset       - (int)(delta*6)); return true; }
        if (activeTab == TAB_TEAMS) {
            int divX = guiLeft + DIVIDER_X;
            if (mx > divX) availableHuntersScroll = Math.max(0, availableHuntersScroll - (int)(delta*6));
            return true;
        }
        if (activeTab == TAB_DUNGEONS)    { deploymentsScroll        = Math.max(0, deploymentsScroll        - (int)(delta*6)); return true; }
        if (activeTab == TAB_BUFFS) {
            buffsScroll = Math.max(0, Math.min(maxBuffsScroll(), buffsScroll - (int)(delta * 8)));
            return true;
        }
        return super.mouseScrolled(mx, my, delta);
    }

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		ResponsiveGuiScale.Transform transform = responsiveTransform();
		return super.mouseReleased(transform.logicalX(mouseX), transform.logicalY(mouseY), button);
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
		ResponsiveGuiScale.Transform transform = responsiveTransform();
		return super.mouseDragged(transform.logicalX(mouseX), transform.logicalY(mouseY), button,
				dragX / transform.scale(), dragY / transform.scale());
	}

	@Override
	public void mouseMoved(double mouseX, double mouseY) {
		ResponsiveGuiScale.Transform transform = responsiveTransform();
		super.mouseMoved(transform.logicalX(mouseX), transform.logicalY(mouseY));
	}

    // ── Management click ──────────────────────────────────────────────────────

    private void handleManagementClick(double mx, double my) {
        int rowH   = 18;
        int startY = guiTop + TAB_Y_OFFSET + TAB_H + 42;
        int startX = guiLeft + 8;
        String[] permKeys = { "canOpen","tabOverview","tabRoster","tabTeams","tabDungeons","tabStorage","tabBuffs","tabLeaderboard" };
        int nameColW = 82, checkGap = 28, checkSize = 10;

        for (int row = 0; row < menu.members.size(); row++) {
            int ry = startY + row * rowH - managementScrollOffset;
            if (ry < startY - rowH || ry > guiTop + GUI_H - 50) continue;
            GuildMemberPermissions p = menu.members.get(row);
            for (int col = 0; col < permKeys.length; col++) {
                int cx = startX + nameColW + col * checkGap + 4, cy = ry + 4;
                if (mx >= cx && mx <= cx + checkSize && my >= cy && my <= cy + checkSize) {
                    sendAction("toggle_perm", p.playerUUID.toString(), permKeys[col]); return;
                }
            }
            int rx = guiLeft + GUI_W - 20;
            if (mx >= rx && mx <= rx + 12 && my >= ry + 3 && my <= ry + 13) {
                sendAction("remove_member", p.playerUUID.toString(), ""); return;
            }
        }
    }

    // ── Roster click ──────────────────────────────────────────────────────────

    private void handleRosterClick(double mx, double my) {
        if (!menu.viewerIsOwner) return;
        int contentY   = guiTop + TAB_Y_OFFSET + TAB_H + 4;
        int listStartY = contentY + 14;
        int divX       = guiLeft + DIVIDER_X;

        if (mx < divX) { // Dismiss buttons (hired hunters)
            int rowH = 16;
            for (int i = 0; i < menu.hunters.size(); i++) {
                int ry = listStartY + i * rowH - rosterScrollOffset;
                int bx = divX - 18;
                if (mx >= bx && mx <= bx + 14 && my >= ry + 1 && my <= ry + 13) {
                    sendAction("dismiss", menu.hunters.get(i).id.toString(), ""); return;
                }
            }
        }
        if (mx > divX) { // Hire / refresh pool
            int poolListEndY = guiTop + GUI_H - 28;
            int rowH = 20;
            int rPanelX = divX + 5;

            // Refresh button
            int rby = guiTop + GUI_H - 26, rbx = rPanelX, rbw = GUI_W - DIVIDER_X - 12;
            if (mx >= rbx && mx <= rbx + rbw && my >= rby && my <= rby + 16) {
                sendAction("refresh_pool", "", ""); return;
            }
            // Hire buttons
            for (int i = 0; i < menu.recruitPool.size(); i++) {
                int ry = listStartY + i * rowH - rosterScrollOffset;
                if (ry + rowH < listStartY || ry > poolListEndY) continue;
                int hx = guiLeft + GUI_W - 40;
                if (mx >= hx && mx <= hx + 34 && my >= ry + 2 && my <= ry + 16) {
                    sendAction("hire", menu.recruitPool.get(i).id.toString(), ""); return;
                }
            }
        }
    }

    // ── Teams click ───────────────────────────────────────────────────────────

    private void handleTeamsClick(double mx, double my) {
        int contentY   = guiTop + TAB_Y_OFFSET + TAB_H + 4;
        int listStartY = contentY + 14;
        int divX       = guiLeft + DIVIDER_X;

        // Left panel: select team
        if (mx < divX) {
            int rowH = 28;
            for (int i = 0; i < menu.teams.size(); i++) {
                int ry = listStartY + i * rowH;
                if (my >= ry && my < ry + rowH - 2) {
                    selectedTeamId = menu.teams.get(i).id();
                    availableHuntersScroll = 0;
                    return;
                }
            }
        }

        // Right panel
        GuildComputerMenu.TeamInfo team = getSelectedTeam();
        if (team != null && mx > divX) {
            int rPanelX = divX + 5;
            int ry = listStartY + 14; // after header
            int autoY = ry;
            int toggleX = guiLeft + GUI_W - 88;
            int rankX = guiLeft + GUI_W - 42;
            if (my >= autoY && my <= autoY + 14) {
                if (mx >= toggleX && mx <= toggleX + 42) {
                    sendAction("set_team_auto_raid", team.id().toString(),
                            (!team.autoRaidEnabled()) + ":" + team.autoRaidMaxRank());
                    return;
                }
                if (mx >= rankX && mx <= rankX + 34) {
                    int nextRank = team.autoRaidMaxRank() >= 6 ? 1 : team.autoRaidMaxRank() + 1;
                    sendAction("set_team_auto_raid", team.id().toString(),
                            team.autoRaidEnabled() + ":" + nextRank);
                    return;
                }
            }
            ry += 18;

            // Member rows — remove [×] button
            for (UUID memberId : team.memberIds()) {
                int bx = guiLeft + GUI_W - 20;
                if (mx >= bx && mx <= bx + 14 && my >= ry + 1 && my <= ry + 12) {
                    sendAction("remove_from_team", team.id().toString(), memberId.toString()); return;
                }
                ry += 14;
            }

            // Separator + "Available" header
            ry = teamAvailableListStartY(team, listStartY);

            // Available hunters — add [+] button
            List<GuildHunter> avail = getAvailableHunters();
            int availListStartY = ry;
            int availEndY = guiTop + GUI_H - 6;
            if (team.memberIds().size() >= 5) return;
            for (int i = 0; i < avail.size(); i++) {
                int rowY = availListStartY + i * 14 - availableHuntersScroll;
                if (rowY + 14 < availListStartY || rowY > availEndY) continue;
                int bx = guiLeft + GUI_W - 20;
                if (mx >= bx && mx <= bx + 14 && my >= rowY + 1 && my <= rowY + 12) {
                    sendAction("assign_hunter", team.id().toString(), avail.get(i).id.toString()); return;
                }
            }
        }
    }

    // ── Dungeons click ────────────────────────────────────────────────────────

    private void handleDungeonsClick(double mx, double my) {
        int contentY   = guiTop + TAB_Y_OFFSET + TAB_H + 4;
        int selectorY  = contentY + 2;
        int headerY    = selectorY + 22;
        int listStartY = headerY + 14;
        int divX       = guiLeft + DIVIDER_X;

        // Team selector arrows
        if (my >= selectorY && my <= selectorY + 18) {
            if (mx >= guiLeft + 6 && mx <= guiLeft + 18) { // left arrow
                if (!menu.teams.isEmpty()) deployTeamIdx = (deployTeamIdx - 1 + menu.teams.size()) % menu.teams.size();
                return;
            }
            int arrowRightX = divX - 18;
            if (mx >= arrowRightX && mx <= arrowRightX + 12) { // right arrow
                if (!menu.teams.isEmpty()) deployTeamIdx = (deployTeamIdx + 1) % menu.teams.size();
                return;
            }
        }

        // Left panel: nearby gates — [Deploy] button
        if (mx < divX) {
            int rowH = 20;
            for (int i = 0; i < menu.nearbyGates.size(); i++) {
                int ry = listStartY + i * rowH;
                int bx = divX - 46;
                if (mx >= bx && mx <= bx + 40 && my >= ry + 2 && my <= ry + 16) {
                    // Deploy selected team to this gate
                    if (!menu.teams.isEmpty()) {
                        GuildComputerMenu.TeamInfo team = menu.teams.get(Math.min(deployTeamIdx, menu.teams.size()-1));
                        if (!isTeamDeployed(team.id())) {
                            sendAction("deploy_team", team.id().toString(),
                                    menu.nearbyGates.get(i).entityId().toString());
                        }
                    }
                    return;
                }
            }

            // Simulated missions: rank buttons below real gates (match renderDungeons layout)
            int gateListH = Math.max(menu.nearbyGates.size(), 1) * 20;
            int simY = listStartY + gateListH + 8 + 12; // simLabelY + 12
            int simBtnW = 24;
            String[] simRanks = {"E","D","C","B","A","S"};
            for (int r = 0; r < simRanks.length; r++) {
                int bx = guiLeft + 6 + r * (simBtnW + 3);
                if (mx >= bx && mx <= bx + simBtnW && my >= simY && my <= simY + 16) {
                    if (!menu.teams.isEmpty()) {
                        GuildComputerMenu.TeamInfo team = menu.teams.get(Math.min(deployTeamIdx, menu.teams.size()-1));
                        if (!isTeamDeployed(team.id())) {
                            sendAction("deploy_team", team.id().toString(), "sim:" + (r + 1));
                        }
                    }
                    return;
                }
            }
        }

        // Right panel: recall buttons
        if (mx > divX) {
            int rowH = 26;
            for (int i = 0; i < menu.deployments.size(); i++) {
                int ry = listStartY + i * rowH - deploymentsScroll;
                int bx = guiLeft + GUI_W - 46;
                if (mx >= bx && mx <= bx + 40 && my >= ry + 8 && my <= ry + 22) {
                    sendAction("recall_team", menu.deployments.get(i).id().toString(), ""); return;
                }
            }
        }
    }

    // ── Render ────────────────────────────────────────────────────────────────

    private void handleBuffsClick(double mx, double my) {
        int contentY = guiTop + TAB_Y_OFFSET + TAB_H + 4;
        int slotY = contentY + 28;
        int slotW = 148;

        for (int slot = 1; slot <= 2; slot++) {
            int sx = guiLeft + 8 + (slot - 1) * (slotW + 18);
            boolean unlocked = slot == 1 || menu.guildLevel >= 10;
            if (unlocked && mx >= sx + slotW - 36 && mx <= sx + slotW - 6 && my >= slotY + 18 && my <= slotY + 32) {
                sendAction("set_buff", Integer.toString(slot), "0");
                return;
            }
        }

        int listY = contentY + 76;
        int rowH = 16;
        for (int i = 0; i < GuildBuffRegistry.all().size(); i++) {
            GuildBuffRegistry.GuildBuff buff = GuildBuffRegistry.all().get(i);
            int y = listY + i * rowH - buffsScroll;
            if (y + rowH < listY || y > buffListEndY()) continue;
            int bx = guiLeft + GUI_W - 54;
            boolean unlocked = menu.guildLevel >= buff.unlockLevel();
            boolean active = menu.activeBuffSlot1 == buff.id() || menu.activeBuffSlot2 == buff.id();
            if (unlocked && !active && mx >= bx && mx <= bx + 46 && my >= y + 1 && my <= y + 13) {
                int slot = menu.activeBuffSlot1 == 0 ? 1 : (menu.guildLevel >= 10 && menu.activeBuffSlot2 == 0 ? 2 : 1);
                sendAction("set_buff", Integer.toString(slot), Integer.toString(buff.id()));
                return;
            }
        }
    }

    @Override
    public void render(GuiGraphics gg, int mouseX, int mouseY, float pt) {
        renderBackground(gg);
		ResponsiveGuiScale.Transform transform = responsiveTransform();
		int logicalMouseX = transform.logicalMouseX(mouseX);
		int logicalMouseY = transform.logicalMouseY(mouseY);
		ResponsiveGuiScale.push(gg, transform);
		super.render(gg, logicalMouseX, logicalMouseY, pt);
		ResponsiveGuiScale.pop(gg);
        if (activeTab == TAB_STORAGE) renderTooltip(gg, mouseX, mouseY);
		if (activeTab == TAB_BUFFS) renderBuffTooltip(gg, logicalMouseX, logicalMouseY, mouseX, mouseY);
    }

	private ResponsiveGuiScale.Transform responsiveTransform() {
		return ResponsiveGuiScale.fit(this.width, this.height, GUI_W + 8, GUI_H + 8);
	}

    @Override protected void renderLabels(GuiGraphics gg, int mouseX, int mouseY) {}

    @Override
    protected void containerTick() {
        super.containerTick();
        localScreenTicks++;
    }

    @Override
    protected void renderBg(GuiGraphics gg, float pt, int mouseX, int mouseY) {
        fillRounded(gg, guiLeft, guiTop, GUI_W, GUI_H, 0xE8101018, 6);
        drawBorder(gg, guiLeft, guiTop, GUI_W, GUI_H, 0xFF2A2A3A, 1);

        if (!menu.hasGuild)       renderCreateGuildScreen(gg);
        else if (!canAccessAnyTab()) renderAccessDenied(gg);
        else { renderTabs(gg, mouseX, mouseY); renderTabContent(gg, mouseX, mouseY, pt); }
    }

    // ── Create / access denied ────────────────────────────────────────────────

    private void renderCreateGuildScreen(GuiGraphics gg) {
        String t = "§eNo Guild Registered", s = "§7Create a new guild for this computer:";
        gg.drawString(font, t, guiLeft + GUI_W/2 - font.width(t)/2, guiTop + GUI_H/2 - 46, 0xFFFFFFFF, false);
        gg.drawString(font, s, guiLeft + GUI_W/2 - font.width(s)/2, guiTop + GUI_H/2 - 30, 0xFFFFFFFF, false);
        nameBox.render(gg, 0, 0, 0);
    }

    private void renderAccessDenied(GuiGraphics gg) {
        String m1 = "§cAccess Denied", m2 = "§7You are not authorised to use this computer.";
        gg.drawString(font, m1, guiLeft + GUI_W/2 - font.width(m1)/2, guiTop + GUI_H/2 - 10, 0xFFFFFFFF, false);
        gg.drawString(font, m2, guiLeft + GUI_W/2 - font.width(m2)/2, guiTop + GUI_H/2 +  6, 0xFFFFFFFF, false);
    }

    // ── Tab bar ───────────────────────────────────────────────────────────────

    private void renderTabs(GuiGraphics gg, int mx, int my) {
        int ty = guiTop + TAB_Y_OFFSET;
        for (int i = 0; i < TAB_LABELS.length; i++) {
            boolean active = (i == activeTab), access = canAccessTab(i);
            boolean hov = mx >= tabXPositions[i] && mx < tabXPositions[i] + tabWidths[i] && my >= ty && my <= ty + TAB_H;
            int bg   = active ? 0xFF1E1E2E : hov && access ? 0xFF2A2A3A : 0xFF141420;
            int text = !access ? 0xFF555565 : active ? 0xFFE0D060 : 0xFFAAAAAA;
            fillRounded(gg, tabXPositions[i], ty, tabWidths[i], TAB_H, bg, 3);
            if (active) gg.fill(tabXPositions[i]+2, ty, tabXPositions[i]+tabWidths[i]-2, ty+2, 0xFFE0D060);
            int lw = font.width(TAB_LABELS[i]);
            gg.drawString(font, TAB_LABELS[i], tabXPositions[i]+(tabWidths[i]-lw)/2, ty+(TAB_H-8)/2, text, false);
        }
        gg.fill(guiLeft, guiTop+TAB_Y_OFFSET+TAB_H, guiLeft+GUI_W, guiTop+TAB_Y_OFFSET+TAB_H+1, 0xFF2A2A3A);
    }

    // ── Tab content router ────────────────────────────────────────────────────

    private void renderTabContent(GuiGraphics gg, int mx, int my, float pt) {
        int cy = guiTop + TAB_Y_OFFSET + TAB_H + 4;
        int ch = GUI_H - TAB_Y_OFFSET - TAB_H - 4;
        if (!canAccessTab(activeTab)) { renderComingSoon(gg, cy, "§cNo access to this tab."); return; }
        switch (activeTab) {
            case TAB_OVERVIEW    -> renderOverview(gg, cy);
            case TAB_ROSTER      -> renderRoster(gg, cy, ch, mx, my);
            case TAB_TEAMS       -> renderTeams(gg, cy, ch, mx, my);
            case TAB_DUNGEONS    -> renderDungeons(gg, cy, ch, mx, my);
            case TAB_STORAGE     -> renderStorage(gg, cy);
            case TAB_BUFFS       -> renderBuffs(gg, cy, mx, my);
            case TAB_MANAGEMENT  -> renderManagement(gg, cy, ch, mx, my);
            case TAB_LEADERBOARD -> renderLeaderboard(gg, cy);
            default              -> renderComingSoon(gg, cy, "§7Coming in a future update.");
        }
    }

    // ── Overview tab ─────────────────────────────────────────────────────────

    private void renderOverview(GuiGraphics gg, int startY) {
        int cx = guiLeft + GUI_W / 2, y = startY + 8;
        drawCentered(gg, "§e§l" + menu.guildName, cx, y); y += 14;
        drawCentered(gg, guildLevelBadge(menu.guildLevel) + " §7Level " + menu.guildLevel, cx, y); y += 14;
        drawCentered(gg, "§7Owner: §f" + menu.ownerName, cx, y); y += 14;
        // XP bar
        int barW = 200, barH = 8, barX = cx - barW/2;
        gg.fill(barX, y, barX+barW, y+barH, 0xFF222230);
        if (menu.xpToNext > 0) gg.fill(barX, y, barX + (int)(barW * Math.min((double)menu.guildXp/menu.xpToNext, 1.0)), y+barH, 0xFF60C060);
        drawBorder(gg, barX, y, barW, barH, 0xFF3A3A4A, 1);
        String xpStr = "§7XP: §a" + menu.guildXp + " §7/ §a" + menu.xpToNext;
        drawCentered(gg, xpStr, cx, y+barH+3); y += barH + 16;
        drawCentered(gg, "§7Total Gate Clears: §e" + menu.totalClears, cx, y); y += 14;
        drawCentered(gg, "§7Members: §f" + (menu.members.size() + 1), cx, y); y += 14;
        drawCentered(gg, "§7Hunters: §f" + menu.hunters.size() + " §8/ " + GuildData.MAX_HUNTERS, cx, y);
    }

    // ── Roster tab ────────────────────────────────────────────────────────────

    private void renderRoster(GuiGraphics gg, int contentY, int contentH, int mx, int my) {
        int divX = guiLeft + DIVIDER_X;
        int listStartY = contentY + 14;
        gg.fill(divX, contentY, divX+1, guiTop+GUI_H-4, 0xFF2A2A3A);

        // Left: hired hunters
        gg.drawString(font, "§eHired §7(" + menu.hunters.size() + "/" + GuildData.MAX_HUNTERS + ")", guiLeft+6, contentY+2, 0xFFFFFFFF, false);
        gg.fill(guiLeft+4, contentY+12, divX-1, contentY+13, 0xFF2A2A3A);

        int rowH = 16, leftEndY = guiTop+GUI_H-6;
        enableScissor(gg, guiLeft+4, listStartY, DIVIDER_X-6, leftEndY-listStartY);
        if (menu.hunters.isEmpty()) {
            gg.drawString(font, "§8No hunters hired yet.", guiLeft+10, listStartY+10, 0xFFFFFFFF, false);
        } else {
            for (int i = 0; i < menu.hunters.size(); i++) {
                int ry = listStartY + i*rowH - rosterScrollOffset;
                if (ry+rowH < listStartY || ry > leftEndY) continue;
                GuildHunter h = menu.hunters.get(i);
                if (i%2==0) gg.fill(guiLeft+4, ry, divX-1, ry+rowH-1, 0x20FFFFFF);
                gg.fill(guiLeft+6, ry+5, guiLeft+9, ry+8, "deployed".equals(h.status) ? 0xFF60D060 : 0xFF606060);
                gg.drawString(font, GuildHunter.rankColor(h.rank)+"["+h.rank+"] §f"
                        +(h.name.length()>10?h.name.substring(0,9)+"…":h.name)+" §7· "
                        +GuildHunter.classColor(h.hunterClass)+h.hunterClass, guiLeft+11, ry+4, 0xFFFFFFFF, false);
                if (menu.viewerIsOwner) {
                    int bx = divX-18;
                    boolean hov = mx>=bx&&mx<=bx+14&&my>=ry+1&&my<=ry+13;
                    gg.fill(bx, ry+1, bx+14, ry+13, hov ? 0xFFCC2222 : 0xFF882222);
                    gg.drawString(font, "×", bx+3, ry+2, 0xFFFFFFFF, false);
                }
            }
        }
        disableScissor(gg);

        // Right: recruit pool
        int rPanelX = divX+5, poolListEndY = guiTop+GUI_H-28;
        gg.drawString(font, "§eRecruit Pool", rPanelX, contentY+2, 0xFFFFFFFF, false);
        gg.fill(divX+2, contentY+12, guiLeft+GUI_W-4, contentY+13, 0xFF2A2A3A);
        int rowH2 = 20;
        enableScissor(gg, divX+2, listStartY, GUI_W-DIVIDER_X-8, poolListEndY-listStartY);
        if (menu.recruitPool.isEmpty()) {
            gg.drawString(font, "§8Pool is empty.", rPanelX, listStartY+10, 0xFFFFFFFF, false);
        } else {
            for (int i = 0; i < menu.recruitPool.size(); i++) {
                int ry = listStartY + i*rowH2 - rosterScrollOffset;
                if (ry+rowH2 < listStartY || ry > poolListEndY) continue;
                GuildHunter h = menu.recruitPool.get(i);
                if (i%2==0) gg.fill(divX+2, ry, guiLeft+GUI_W-4, ry+rowH2-1, 0x20FFFFFF);
                String nameTrim = h.name.length()>9 ? h.name.substring(0,8)+"…" : h.name;
                gg.drawString(font, GuildHunter.rankColor(h.rank)+"["+h.rank+"] §f"+nameTrim+" §7· "+GuildHunter.classColor(h.hunterClass)+h.hunterClass, rPanelX, ry+2, 0xFFFFFFFF, false);
                gg.drawString(font, "§7Cost: §e"+GuildHunter.hireCost(h.rank)+" "+GuildHunter.rankColor(h.rank)+GuildHunter.hireMaterialName(h.rank), rPanelX, ry+11, 0xFFFFFFFF, false);
                if (menu.viewerIsOwner) {
                    int hx = guiLeft+GUI_W-40;
                    boolean hov = mx>=hx&&mx<=hx+34&&my>=ry+2&&my<=ry+16;
                    gg.fill(hx, ry+2, hx+34, ry+16, hov ? 0xFF40C040 : 0xFF256025);
                    String lbl = "Hire";
                    gg.drawString(font, "§f"+lbl, hx+(34-font.width(lbl))/2, ry+5, 0xFFFFFFFF, false);
                }
            }
        }
        disableScissor(gg);
        if (menu.viewerIsOwner) {
            int rby = guiTop+GUI_H-26, rbx = rPanelX, rbw = GUI_W-DIVIDER_X-12;
            boolean hovRef = mx>=rbx&&mx<=rbx+rbw&&my>=rby&&my<=rby+16;
            gg.fill(rbx, rby, rbx+rbw, rby+16, hovRef ? 0xFF907010 : 0xFF604A08);
            drawBorder(gg, rbx, rby, rbw, 16, 0xFF807030, 1);
            String rl = "§eRefresh Pool §7(8x Gold Ingot)";
            gg.drawString(font, rl, rbx+(rbw-font.width(rl))/2, rby+4, 0xFFFFFFFF, false);
        }
    }

    // ── Teams tab ─────────────────────────────────────────────────────────────

    private void renderTeams(GuiGraphics gg, int contentY, int contentH, int mx, int my) {
        int divX       = guiLeft + DIVIDER_X;
        int listStartY = contentY + 14;

        gg.fill(divX, contentY, divX+1, guiTop+GUI_H-4, 0xFF2A2A3A);

        // Left panel header
        gg.drawString(font, "§eTeams", guiLeft+6, contentY+2, 0xFFFFFFFF, false);
        gg.fill(guiLeft+4, contentY+12, divX-1, contentY+13, 0xFF2A2A3A);

        int rowH = 28;
        for (int i = 0; i < menu.teams.size(); i++) {
            GuildComputerMenu.TeamInfo t = menu.teams.get(i);
            int ry = listStartY + i * rowH;
            boolean selected = t.id().equals(selectedTeamId);
            boolean deployed = isTeamDeployed(t.id());

            if (selected) gg.fill(guiLeft+4, ry, divX-1, ry+rowH-2, 0x40E0D060);
            else if (i%2==0) gg.fill(guiLeft+4, ry, divX-1, ry+rowH-2, 0x18FFFFFF);

            String statusDot = deployed ? "§a● " : "§8○ ";
            gg.drawString(font, statusDot + "§e" + t.name(), guiLeft+8, ry+6, 0xFFFFFFFF, false);
            String auto = t.autoRaidEnabled() ? " §bAuto<=" + GuildDeployment.rankLabel(t.autoRaidMaxRank()) : "";
            gg.drawString(font, "§7Hunters: §f" + t.memberIds().size() + "/5" + auto
                    + (deployed ? " §a[Deployed]" : ""), guiLeft+8, ry+17, 0xFFFFFFFF, false);
        }

        // Right panel
        GuildComputerMenu.TeamInfo team = getSelectedTeam();
        int rPanelX = divX + 5;

        if (team == null) {
            gg.drawString(font, "§8← Select a team to manage it", rPanelX, contentY+40, 0xFFFFFFFF, false);
            return;
        }

        boolean deployed = isTeamDeployed(team.id());

        // Header
        gg.drawString(font, "§e" + team.name() + " §7(" + team.memberIds().size() + "/5)", rPanelX, contentY+2, 0xFFFFFFFF, false);
        gg.fill(divX+2, contentY+12, guiLeft+GUI_W-4, contentY+13, 0xFF2A2A3A);

        int ry = listStartY + 2;

        int autoY = ry;
        gg.drawString(font, "§7Auto raid:", rPanelX, autoY+3, 0xFFFFFFFF, false);
        int toggleX = guiLeft + GUI_W - 88;
        int rankX = guiLeft + GUI_W - 42;
        if (menu.viewerIsOwner) {
            boolean hovToggle = mx>=toggleX&&mx<=toggleX+42&&my>=autoY&&my<=autoY+14;
            gg.fill(toggleX, autoY, toggleX+42, autoY+14, team.autoRaidEnabled() ? (hovToggle ? 0xFF2F8A88 : 0xFF1F6668) : (hovToggle ? 0xFF4A4A5A : 0xFF2A2A3A));
            drawBorder(gg, toggleX, autoY, 42, 14, 0xFF40DCE8, 1);
            String autoLabel = team.autoRaidEnabled() ? "ON" : "OFF";
            gg.drawString(font, "§f"+autoLabel, toggleX+(42-font.width(autoLabel))/2, autoY+3, 0xFFFFFFFF, false);

            boolean hovRank = mx>=rankX&&mx<=rankX+34&&my>=autoY&&my<=autoY+14;
            gg.fill(rankX, autoY, rankX+34, autoY+14, hovRank ? 0xFF404050 : 0xFF252535);
            drawBorder(gg, rankX, autoY, 34, 14, 0xFF40DCE8, 1);
            String rankText = "<=" + GuildDeployment.rankLabel(team.autoRaidMaxRank());
            gg.drawString(font, gateColor(team.autoRaidMaxRank())+rankText, rankX+(34-font.width(rankText))/2, autoY+3, 0xFFFFFFFF, false);
        } else {
            gg.drawString(font, (team.autoRaidEnabled() ? "§bON" : "§8OFF") + " §7<= "
                    + gateColor(team.autoRaidMaxRank()) + GuildDeployment.rankLabel(team.autoRaidMaxRank()),
                    rPanelX + 58, autoY+3, 0xFFFFFFFF, false);
        }
        ry += 18;

        // Current members
        gg.drawString(font, "§7Members:", rPanelX, ry, 0xFFFFFFFF, false); ry += 12;
        if (team.memberIds().isEmpty()) {
            gg.drawString(font, "§8No hunters assigned.", rPanelX+4, ry, 0xFFFFFFFF, false); ry += 12;
        } else {
            for (UUID mid : team.memberIds()) {
                GuildHunter h = findHunter(mid);
                if (h == null) { ry += 14; continue; }
                if (menu.viewerIsOwner && !deployed) {
                    int bx = guiLeft+GUI_W-20;
                    boolean hov = mx>=bx&&mx<=bx+14&&my>=ry+1&&my<=ry+12;
                    gg.fill(bx, ry+1, bx+14, ry+12, hov ? 0xFFCC2222 : 0xFF882222);
                    gg.drawString(font, "×", bx+3, ry+1, 0xFFFFFFFF, false);
                }
                gg.drawString(font, GuildHunter.rankColor(h.rank)+"["+h.rank+"] §f"
                        +(h.name.length()>12?h.name.substring(0,11)+"…":h.name)+" §7· "
                        +GuildHunter.classColor(h.hunterClass)+h.hunterClass, rPanelX+4, ry+2, 0xFFFFFFFF, false);
                ry += 14;
            }
        }

        if (deployed) {
            gg.drawString(font, "§a[Team is currently deployed]", rPanelX, ry+4, 0xFFFFFFFF, false);
            return;
        }

        // Divider
        gg.fill(divX+2, ry+2, guiLeft+GUI_W-4, ry+3, 0xFF2A2A3A); ry += 8;
        gg.drawString(font, "§7Available hunters:", rPanelX, ry, 0xFFFFFFFF, false); ry += 12;

        // Available hunters list (scrollable)
        List<GuildHunter> avail = getAvailableHunters();
        int availListStartY = teamAvailableListStartY(team, listStartY);
        int availEndY = guiTop + GUI_H - 6;
        enableScissor(gg, divX+2, availListStartY, GUI_W-DIVIDER_X-8, availEndY-availListStartY);
        if (avail.isEmpty()) {
            gg.drawString(font, "§8All hunters assigned.", rPanelX+4, availListStartY+2, 0xFFFFFFFF, false);
        } else if (team.memberIds().size() >= 5) {
            gg.drawString(font, "§8Team is full.", rPanelX+4, availListStartY+2, 0xFFFFFFFF, false);
        } else {
            for (int i = 0; i < avail.size(); i++) {
                GuildHunter h = avail.get(i);
                int rowY = availListStartY + i*14 - availableHuntersScroll;
                if (rowY + 14 < availListStartY || rowY > availEndY) continue;
                if (menu.viewerIsOwner) {
                    int bx = guiLeft+GUI_W-20;
                    boolean hov = mx>=bx&&mx<=bx+14&&my>=rowY+1&&my<=rowY+12;
                    gg.fill(bx, rowY+1, bx+14, rowY+12, hov ? 0xFF40C040 : 0xFF206020);
                    gg.drawString(font, "+", bx+3, rowY+1, 0xFFFFFFFF, false);
                }
                gg.drawString(font, GuildHunter.rankColor(h.rank)+"["+h.rank+"] §f"
                        +(h.name.length()>12?h.name.substring(0,11)+"…":h.name)+" §7· "
                        +GuildHunter.classColor(h.hunterClass)+h.hunterClass, rPanelX+4, rowY+2, 0xFFFFFFFF, false);
            }
        }
        disableScissor(gg);
    }

    // ── Dungeons tab ──────────────────────────────────────────────────────────

    private void renderDungeons(GuiGraphics gg, int contentY, int contentH, int mx, int my) {
        int divX      = guiLeft + DIVIDER_X;
        // Selector bar sits at the very top of content area
        int selectorY = contentY + 2;
        // Section headers sit below the selector bar with a clear gap
        int headerY   = selectorY + 22;   // "Nearby Gates" / "Active Missions" labels
        // Gate list and deployments list start below the header + separator
        int listY     = headerY + 14;

        gg.fill(divX, contentY, divX+1, guiTop+GUI_H-4, 0xFF2A2A3A);

        // ── Team selector bar ─────────────────────────────────────────────────
        gg.fill(guiLeft+4, selectorY, divX-1, selectorY+18, 0xFF1A1A28);
        drawBorder(gg, guiLeft+4, selectorY, DIVIDER_X-6, 18, 0xFF2A2A3A, 1);

        // Left arrow
        boolean hovL = mx>=guiLeft+6&&mx<=guiLeft+18&&my>=selectorY&&my<=selectorY+18;
        gg.fill(guiLeft+6, selectorY+3, guiLeft+18, selectorY+15, hovL ? 0xFF404050 : 0xFF252535);
        gg.drawString(font, "◄", guiLeft+7, selectorY+5, 0xFFAAAAAA, false);

        // Right arrow
        int arrowRx = divX-18;
        boolean hovR = mx>=arrowRx&&mx<=arrowRx+12&&my>=selectorY&&my<=selectorY+18;
        gg.fill(arrowRx, selectorY+3, arrowRx+12, selectorY+15, hovR ? 0xFF404050 : 0xFF252535);
        gg.drawString(font, "►", arrowRx+1, selectorY+5, 0xFFAAAAAA, false);

        // Team name in selector
        String teamLabel;
        if (menu.teams.isEmpty()) {
            teamLabel = "§8No teams";
        } else {
            int idx = Math.min(deployTeamIdx, menu.teams.size()-1);
            GuildComputerMenu.TeamInfo t = menu.teams.get(idx);
            teamLabel = isTeamDeployed(t.id()) ? "§7" + t.name() + " §8[deployed]" : "§e" + t.name();
        }
        int tlw = font.width(teamLabel);
        gg.drawString(font, teamLabel, guiLeft+4 + (DIVIDER_X-6-tlw)/2 + 14, selectorY+5, 0xFFFFFFFF, false);

        // ── Left panel: nearby real gates ─────────────────────────────────────
        gg.drawString(font, "§eNearby Gates", guiLeft+6, headerY, 0xFFFFFFFF, false);
        gg.fill(guiLeft+4, headerY+10, divX-1, headerY+11, 0xFF2A2A3A);

        if (menu.nearbyGates.isEmpty()) {
            gg.drawString(font, "§8No gates detected.", guiLeft+10, listY+2, 0xFFFFFFFF, false);
        } else {
            int rowH = 20;
            for (int i = 0; i < menu.nearbyGates.size(); i++) {
                GuildComputerMenu.NearbyGate gate = menu.nearbyGates.get(i);
                int ry = listY + i*rowH;
                if (i%2==0) gg.fill(guiLeft+4, ry, divX-1, ry+rowH-1, 0x18FFFFFF);
                int dist = (int) Math.sqrt(Math.pow(gate.pos().getX() - menu.computerPos.getX(), 2)
                        + Math.pow(gate.pos().getZ() - menu.computerPos.getZ(), 2));
                String label = trimToWidth(gateColor(gate.rank()) + gate.label(), divX - guiLeft - 56);
                gg.drawString(font, label, guiLeft+7, ry+3, 0xFFFFFFFF, false);
                gg.drawString(font, "§8~"+dist+"m", guiLeft+7, ry+11, 0xFFFFFFFF, false);
                if (menu.viewerIsOwner && !menu.teams.isEmpty()) {
                    int bx = divX-46;
                    boolean canDeploy = !isTeamDeployed(menu.teams.get(Math.min(deployTeamIdx, menu.teams.size()-1)).id());
                    boolean hov = canDeploy && mx>=bx&&mx<=bx+40&&my>=ry+2&&my<=ry+16;
                    gg.fill(bx, ry+2, bx+40, ry+16, !canDeploy ? 0xFF333343 : hov ? 0xFF40C040 : 0xFF256025);
                    String dl = "Deploy";
                    gg.drawString(font, !canDeploy?"§8"+dl:"§f"+dl, bx+(40-font.width(dl))/2, ry+5, 0xFFFFFFFF, false);
                }
            }
        }

        // Simulated missions section — always at least 24px below the gate list area
        int gateListHeight = Math.max(menu.nearbyGates.size(), 1) * 20;
        int simLabelY = listY + gateListHeight + 8;
        int simY      = simLabelY + 12;
        if (simY + 16 <= guiTop + GUI_H - 4) {
            gg.drawString(font, "§8Simulated Missions:", guiLeft+6, simLabelY, 0xFFFFFFFF, false);
            String[] simColors = {"§7","§f","§a","§b","§e","§6"};
            String[] simLabels = {"E","D","C","B","A","S"};
            int btnW = 24;
            for (int r = 0; r < 6; r++) {
                int bx = guiLeft+6 + r*(btnW+3);
                if (bx+btnW > divX-4) break;
                boolean hov = mx>=bx&&mx<=bx+btnW&&my>=simY&&my<=simY+16;
                boolean canDeploy = !menu.teams.isEmpty() && !isTeamDeployed(menu.teams.get(Math.min(deployTeamIdx, menu.teams.size()-1)).id());
                gg.fill(bx, simY, bx+btnW, simY+16, !canDeploy ? 0xFF252535 : hov ? 0xFF404050 : 0xFF1E1E2E);
                drawBorder(gg, bx, simY, btnW, 16, 0xFF2A2A3A, 1);
                gg.drawString(font, simColors[r]+simLabels[r], bx+(btnW-font.width(simLabels[r]))/2, simY+4, 0xFFFFFFFF, false);
            }
        }

        // ── Right panel: active deployments ───────────────────────────────────
        int rPanelX = divX+5;
        gg.drawString(font, "§eActive Missions", rPanelX, headerY, 0xFFFFFFFF, false);
        gg.fill(divX+2, headerY+10, guiLeft+GUI_W-4, headerY+11, 0xFF2A2A3A);

        if (menu.deployments.isEmpty()) {
            gg.drawString(font, "§8No active missions.", rPanelX, listY+2, 0xFFFFFFFF, false);
        } else {
            int rowH = 26;
            int depEndY = guiTop+GUI_H-6;
            enableScissor(gg, divX+2, listY, GUI_W-DIVIDER_X-8, depEndY-listY);
            for (int i = 0; i < menu.deployments.size(); i++) {
                GuildComputerMenu.DeploymentInfo dep = menu.deployments.get(i);
                int ry = listY + i*rowH - deploymentsScroll;
                if (ry+rowH < listY || ry > depEndY) continue;
                if (i%2==0) gg.fill(divX+2, ry, guiLeft+GUI_W-4, ry+rowH-1, 0x18FFFFFF);

                long ticksLeft = dep.completesAt() - currentDisplayedServerGameTime();
                String timeStr = ticksLeft <= 0 ? "§aComplete!" : "§e" + formatTicks(ticksLeft);
                gg.drawString(font, "§e" + dep.teamName() + " §8→ §f" + dep.gateLabel(), rPanelX, ry+2, 0xFFFFFFFF, false);
                gg.drawString(font, "§7Time: " + timeStr, rPanelX, ry+12, 0xFFFFFFFF, false);

                if (menu.viewerIsOwner) {
                    int bx = guiLeft+GUI_W-46;
                    boolean hov = mx>=bx&&mx<=bx+40&&my>=ry+8&&my<=ry+22;
                    gg.fill(bx, ry+8, bx+40, ry+22, hov ? 0xFFAA2222 : 0xFF772222);
                    String rl = "Recall";
                    gg.drawString(font, "§f"+rl, bx+(40-font.width(rl))/2, ry+11, 0xFFFFFFFF, false);
                }
            }
            disableScissor(gg);
        }
    }

    // ── Storage tab ───────────────────────────────────────────────────────────

    private void renderStorage(GuiGraphics gg, int contentY) {
        String title = "§e§lGuild Storage";
        gg.drawString(font, title, guiLeft+GUI_W/2-font.width(title)/2, contentY+2, 0xFFFFFFFF, false);
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++) {
            int sx = guiLeft + GuildComputerMenu.STORAGE_X + col*18, sy = guiTop + GuildComputerMenu.STORAGE_Y + row*18;
            gg.fill(sx-1, sy-1, sx+17, sy+17, 0xFF333343); gg.fill(sx, sy, sx+16, sy+16, 0xFF555565);
        }
        int invLabelY = guiTop+GuildComputerMenu.INV_Y-10;
        gg.drawString(font, "§7Inventory", guiLeft+GuildComputerMenu.INV_X, invLabelY, 0xFFAAAAAA, false);
        gg.fill(guiLeft+GuildComputerMenu.INV_X, invLabelY+6, guiLeft+GuildComputerMenu.INV_X+9*18, invLabelY+7, 0xFF2A2A3A);
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++) {
            int sx = guiLeft+GuildComputerMenu.INV_X+col*18, sy = guiTop+GuildComputerMenu.INV_Y+row*18;
            gg.fill(sx, sy, sx+16, sy+16, 0xFF555565);
        }
        int hsy = guiTop+GuildComputerMenu.HOTBAR_Y;
        gg.fill(guiLeft+GuildComputerMenu.INV_X, hsy-3, guiLeft+GuildComputerMenu.INV_X+9*18, hsy-2, 0xFF2A2A3A);
        for (int col = 0; col < 9; col++) {
            int sx = guiLeft+GuildComputerMenu.INV_X+col*18;
            gg.fill(sx, hsy, sx+16, hsy+16, 0xFF555565);
        }
    }

    // ── Management tab ────────────────────────────────────────────────────────

    private void renderBuffs(GuiGraphics gg, int contentY, int mx, int my) {
        drawCentered(gg, "§e§lGuild Buffs", guiLeft + GUI_W / 2, contentY + 3);
        gg.drawString(font, "§7Active Slots", guiLeft + 8, contentY + 18, 0xFFFFFFFF, false);

        int slotY = contentY + 28;
        int slotW = 148;
        drawBuffSlot(gg, 1, menu.activeBuffSlot1, guiLeft + 8, slotY, slotW, true, mx, my);
        drawBuffSlot(gg, 2, menu.activeBuffSlot2, guiLeft + 8 + slotW + 18, slotY, slotW, menu.guildLevel >= 10, mx, my);

        int listY = contentY + 76;
        gg.fill(guiLeft + 8, listY - 8, guiLeft + GUI_W - 8, listY - 7, 0xFF2A2A3A);
        int rowH = 16;
        int listEndY = buffListEndY();
        buffsScroll = Math.max(0, Math.min(maxBuffsScroll(), buffsScroll));
        enableScissor(gg, guiLeft + 6, listY, GUI_W - 12, listEndY - listY);
        for (int i = 0; i < GuildBuffRegistry.all().size(); i++) {
            GuildBuffRegistry.GuildBuff buff = GuildBuffRegistry.all().get(i);
            int y = listY + i * rowH - buffsScroll;
            if (y + rowH < listY || y > listEndY) continue;
            boolean unlocked = menu.guildLevel >= buff.unlockLevel();
            boolean active = menu.activeBuffSlot1 == buff.id() || menu.activeBuffSlot2 == buff.id();
            if (i % 2 == 0) gg.fill(guiLeft + 6, y - 1, guiLeft + GUI_W - 6, y + rowH - 2, 0x18FFFFFF);
            gg.drawString(font, trimToWidth((unlocked ? "§f" : "§8") + buff.name(), 116), guiLeft + 10, y + 3, 0xFFFFFFFF, false);
            gg.drawString(font, unlocked ? "§7Lv " + buff.unlockLevel() : "§8Lv " + buff.unlockLevel(), guiLeft + 132, y + 3, 0xFFFFFFFF, false);
            gg.drawString(font, trimToWidth((unlocked ? "§7" : "§8") + buff.description(), 112), guiLeft + 164, y + 3, 0xFFFFFFFF, false);
            if (menu.viewerIsOwner) {
                int bx = guiLeft + GUI_W - 54;
                boolean hov = unlocked && !active && mx >= bx && mx <= bx + 46 && my >= y + 1 && my <= y + 13;
                int color = !unlocked || active ? 0xFF252535 : hov ? 0xFF405078 : 0xFF263858;
                gg.fill(bx, y + 1, bx + 46, y + 13, color);
                String text = active ? "Active" : unlocked ? "Equip" : "Locked";
                gg.drawString(font, (unlocked && !active ? "§f" : "§8") + text, bx + (46 - font.width(text)) / 2, y + 3, 0xFFFFFFFF, false);
            }
        }
        disableScissor(gg);
        if (maxBuffsScroll() > 0) {
            int trackX = guiLeft + GUI_W - 6;
            int visibleH = listEndY - listY;
            int contentH = GuildBuffRegistry.all().size() * 16;
            int knobH = Math.max(12, visibleH * visibleH / contentH);
            int knobY = listY + (visibleH - knobH) * buffsScroll / maxBuffsScroll();
            gg.fill(trackX, listY, trackX + 1, listEndY, 0xFF263858);
            gg.fill(trackX - 1, knobY, trackX + 2, knobY + knobH, 0xFF40B8FF);
        }
    }

    private void drawBuffSlot(GuiGraphics gg, int slot, int buffId, int x, int y, int w, boolean unlocked, int mx, int my) {
        gg.fill(x, y, x + w, y + 38, unlocked ? 0xFF161826 : 0xFF111116);
        drawBorder(gg, x, y, w, 38, unlocked ? 0xFF3A4A70 : 0xFF252535, 1);
        gg.drawString(font, (unlocked ? "§b" : "§8") + "Slot " + slot, x + 6, y + 5, 0xFFFFFFFF, false);
        String name = unlocked ? GuildBuffRegistry.displayName(buffId) : "Unlocks at guild level 10";
        gg.drawString(font, (buffId == 0 || !unlocked ? "§8" : "§f") + trimToWidth(name, w - 46), x + 6, y + 19, 0xFFFFFFFF, false);
        if (menu.viewerIsOwner && unlocked) {
            boolean hov = mx >= x + w - 36 && mx <= x + w - 6 && my >= y + 18 && my <= y + 32;
            gg.fill(x + w - 36, y + 18, x + w - 6, y + 32, hov ? 0xFF884444 : 0xFF552D2D);
            gg.drawString(font, "§fClear", x + w - 33, y + 21, 0xFFFFFFFF, false);
        }
    }

    private void renderManagement(GuiGraphics gg, int startY, int contentH, int mx, int my) {
        if (!menu.viewerIsOwner) { renderComingSoon(gg, startY, "§cOnly the guild owner can access this tab."); return; }
        int startX = guiLeft+8, nameColW = 82, checkGap = 28, rowH = 18;
        String[] pLabels = {"Op","Ov","Ro","Tm","Du","St","Bf","Lb"};
        int founderY = startY+2;
        gg.drawString(font, "Â§7Founder", startX, founderY, 0xFFFFFFFF, false);
        gg.drawString(font, "Â§d" + menu.ownerName, startX+58, founderY, 0xFFFFFFFF, false);
        gg.fill(guiLeft+8, founderY+12, guiLeft+GUI_W-8, founderY+13, 0xFF3A244A);

        int hy = startY+26;
        gg.drawString(font, "§7Member", startX, hy, 0xFFFFFFFF, false);
        for (int c = 0; c < pLabels.length; c++) gg.drawString(font, "§8"+pLabels[c], startX+nameColW+c*checkGap, hy, 0xFFFFFFFF, false);
        gg.fill(guiLeft+8, hy+10, guiLeft+GUI_W-8, hy+11, 0xFF2A2A3A);
        int listY = startY+38, listEndY = guiTop+GUI_H-42;
        enableScissor(gg, guiLeft+4, listY, GUI_W-8, listEndY-listY);
        for (int row = 0; row < menu.members.size(); row++) {
            int ry = listY + row*rowH - managementScrollOffset;
            if (ry+rowH < listY || ry > listEndY) continue;
            GuildMemberPermissions p = menu.members.get(row);
            if (row%2==0) gg.fill(guiLeft+6, ry, guiLeft+GUI_W-6, ry+rowH-1, 0x20FFFFFF);
            String dn = p.playerName.length()>14 ? p.playerName.substring(0,13)+"…" : p.playerName;
            gg.drawString(font, "§f"+dn, startX, ry+5, 0xFFFFFFFF, false);
            boolean[] vals = {p.canOpen,p.tabOverview,p.tabRoster,p.tabTeams,p.tabDungeons,p.tabStorage,p.tabBuffs,p.tabLeaderboard};
            for (int c = 0; c < vals.length; c++) {
                int cx = startX+nameColW+c*checkGap+4, cy = ry+4;
                boolean chk = vals[c];
                gg.fill(cx, cy, cx+10, cy+10, chk ? 0xFF50C050 : 0xFF333343);
                drawBorder(gg, cx, cy, 10, 10, 0xFF4A4A5A, 1);
                if (chk) gg.drawString(font, "✓", cx+1, cy, 0xFFFFFFFF, false);
            }
            int rx = guiLeft+GUI_W-20;
            boolean hx = mx>=rx&&mx<=rx+12&&my>=ry+3&&my<=ry+13;
            gg.fill(rx, ry+3, rx+12, ry+13, hx ? 0xFFAA2222 : 0xFF772222);
            gg.drawString(font, "×", rx+2, ry+3, 0xFFFFFFFF, false);
        }
        disableScissor(gg);
        gg.fill(guiLeft+6, guiTop+GUI_H-42, guiLeft+GUI_W-6, guiTop+GUI_H-41, 0xFF2A2A3A);
        gg.drawString(font, "§7Invite player:", startX, guiTop+GUI_H-38, 0xFFFFFFFF, false);
        addMemberBox.render(gg, mx, my, 0);
    }

    // ── Leaderboard tab ───────────────────────────────────────────────────────

    private void renderLeaderboard(GuiGraphics gg, int startY) {
        int cx = guiLeft+GUI_W/2, y = startY+4;
        drawCentered(gg, "§e§lGuild Leaderboard", cx, y); y += 14;
        gg.drawString(font, "§8#",      guiLeft+12,          y, 0xFFFFFFFF, false);
        gg.drawString(font, "§8Name",   guiLeft+28,          y, 0xFFFFFFFF, false);
        gg.drawString(font, "§8Lvl",    guiLeft+GUI_W-80,    y, 0xFFFFFFFF, false);
        gg.drawString(font, "§8Clears", guiLeft+GUI_W-50,    y, 0xFFFFFFFF, false);
        gg.fill(guiLeft+8, y+10, guiLeft+GUI_W-8, y+11, 0xFF2A2A3A); y += 14;
        if (menu.leaderboard.isEmpty()) { drawCentered(gg, "§7No guilds yet.", cx, y+20); return; }
        for (int i = 0; i < menu.leaderboard.size(); i++) {
            var e = menu.leaderboard.get(i);
            int ry = y + i*16;
            if (e.isOwnGuild()) gg.fill(guiLeft+8, ry-1, guiLeft+GUI_W-8, ry+11, 0x25E0D060);
            String rank = switch(i){case 0->"§6#1";case 1->"§7#2";case 2->"§c#3";default->"§8#"+(i+1);};
            gg.drawString(font, rank, guiLeft+12, ry, 0xFFFFFFFF, false);
            gg.drawString(font, (e.isOwnGuild()?"§e":"§f")+e.name(), guiLeft+28, ry, 0xFFFFFFFF, false);
            gg.drawString(font, "§a"+e.level(), guiLeft+GUI_W-80, ry, 0xFFFFFFFF, false);
            gg.drawString(font, "§f"+e.clears(), guiLeft+GUI_W-50, ry, 0xFFFFFFFF, false);
        }
    }

    // ── Coming soon ───────────────────────────────────────────────────────────

    private void renderComingSoon(GuiGraphics gg, int y, String msg) {
        gg.drawString(font, msg, guiLeft+GUI_W/2-font.width(msg)/2, y+40, 0xFFFFFFFF, false);
    }

    // ── Permission helpers ────────────────────────────────────────────────────

    private boolean canAccessTab(int tab) {
        if (menu.viewerIsOwner) return true;
        GuildMemberPermissions p = getViewerPerms();
        if (p == null) return false;
        if (p.canOpen) return true;
        return switch (tab) {
            case TAB_OVERVIEW    -> p.tabOverview;
            case TAB_ROSTER      -> p.tabRoster;
            case TAB_TEAMS       -> p.tabTeams;
            case TAB_DUNGEONS    -> p.tabDungeons;
            case TAB_STORAGE     -> p.tabStorage;
            case TAB_BUFFS       -> p.tabBuffs;
            case TAB_LEADERBOARD -> p.tabLeaderboard;
            case TAB_MANAGEMENT  -> false;
            default              -> false;
        };
    }

    private boolean canAccessAnyTab() {
        if (menu.viewerIsOwner) return true;
        GuildMemberPermissions p = getViewerPerms();
        return p != null && (p.canOpen || p.tabOverview || p.tabRoster || p.tabTeams
                || p.tabDungeons || p.tabStorage || p.tabBuffs || p.tabLeaderboard);
    }

    private GuildMemberPermissions getViewerPerms() {
        for (GuildMemberPermissions p : menu.members) {
            if (p.playerUUID.equals(menu.viewerUUID)) return p;
        }
        return null;
    }

    // ── Data helpers ──────────────────────────────────────────────────────────

    private GuildComputerMenu.TeamInfo getSelectedTeam() {
        if (selectedTeamId == null) return null;
        for (GuildComputerMenu.TeamInfo t : menu.teams) {
            if (t.id().equals(selectedTeamId)) return t;
        }
        return null;
    }

    private int teamAvailableListStartY(GuildComputerMenu.TeamInfo team, int listStartY) {
        int ry = listStartY + 2;
        ry += 18; // Auto-raid controls
        ry += 12; // Members label
        ry += team.memberIds().isEmpty() ? 12 : team.memberIds().size() * 14;
        ry += 8; // Divider
        ry += 12; // Available hunters label
        return ry;
    }

    private List<GuildHunter> getAvailableHunters() {
        Set<UUID> inTeam = new HashSet<>();
        for (GuildComputerMenu.TeamInfo t : menu.teams) inTeam.addAll(t.memberIds());
        List<GuildHunter> avail = new ArrayList<>();
        for (GuildHunter h : menu.hunters) if (!inTeam.contains(h.id)) avail.add(h);
        return avail;
    }

    private GuildHunter findHunter(UUID id) {
        for (GuildHunter h : menu.hunters) if (h.id.equals(id)) return h;
        return null;
    }

    private int buffListEndY() {
        return guiTop + GUI_H - 8;
    }

    private int maxBuffsScroll() {
        int contentY = guiTop + TAB_Y_OFFSET + TAB_H + 4;
        int listY = contentY + 76;
        int visibleH = Math.max(1, buffListEndY() - listY);
        return Math.max(0, GuildBuffRegistry.all().size() * 16 - visibleH);
    }

    private void renderBuffTooltip(GuiGraphics gg, int mx, int my, int screenMouseX, int screenMouseY) {
        int contentY = guiTop + TAB_Y_OFFSET + TAB_H + 4;
        int listY = contentY + 76;
        int listEndY = buffListEndY();
        if (mx < guiLeft + 6 || mx > guiLeft + GUI_W - 6 || my < listY || my > listEndY) return;
        int rowH = 16;
        for (int i = 0; i < GuildBuffRegistry.all().size(); i++) {
            GuildBuffRegistry.GuildBuff buff = GuildBuffRegistry.all().get(i);
            int y = listY + i * rowH - buffsScroll;
            if (my < y || my > y + rowH) continue;
            boolean unlocked = menu.guildLevel >= buff.unlockLevel();
            boolean active = menu.activeBuffSlot1 == buff.id() || menu.activeBuffSlot2 == buff.id();
            List<Component> lines = new ArrayList<>();
            lines.add(Component.literal("§b" + buff.name()));
            lines.add(Component.literal((unlocked ? "§7Unlocked" : "§8Locked") + " §7at Guild Level " + buff.unlockLevel()));
            for (String line : wrapTooltipText(buff.description(), 190)) {
                lines.add(Component.literal((unlocked ? "§f" : "§8") + line));
            }
            if (active) lines.add(Component.literal("§aCurrently active."));
            else if (unlocked && menu.viewerIsOwner) lines.add(Component.literal("§7Click Equip to activate this passive."));
            SystemTooltip.render(gg, font, lines, screenMouseX, screenMouseY, width, height);
            return;
        }
    }

    private List<String> wrapTooltipText(String text, int maxWidth) {
        List<String> lines = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String word : text.split(" ")) {
            String next = current.length() == 0 ? word : current + " " + word;
            if (font.width(next) > maxWidth && current.length() > 0) {
                lines.add(current.toString());
                current = new StringBuilder(word);
            } else {
                current = new StringBuilder(next);
            }
        }
        if (current.length() > 0) lines.add(current.toString());
        return lines;
    }

    private boolean isTeamDeployed(UUID teamId) {
        for (GuildComputerMenu.DeploymentInfo d : menu.deployments) {
            if (d.teamId().equals(teamId)) return true;
        }
        return false;
    }

    private int nextIdleTeamIndexAfter(UUID justDeployedTeamId) {
        if (menu.teams.isEmpty()) return 0;
        int start = Math.max(0, Math.min(deployTeamIdx, menu.teams.size() - 1));
        for (int offset = 1; offset <= menu.teams.size(); offset++) {
            int idx = (start + offset) % menu.teams.size();
            UUID teamId = menu.teams.get(idx).id();
            if (!teamId.equals(justDeployedTeamId) && !isTeamDeployed(teamId)) return idx;
        }
        return start;
    }

    private boolean teamExists(UUID teamId) {
        for (GuildComputerMenu.TeamInfo t : menu.teams) {
            if (t.id().equals(teamId)) return true;
        }
        return false;
    }

    private String formatTicks(long ticks) {
        long secs = ticks / 20;
        return (secs / 60) + "m " + (secs % 60) + "s";
    }

    private long currentDisplayedServerGameTime() {
        return menu.serverGameTime + localScreenTicks;
    }

    private String gateColor(int rank) {
        return switch (rank) {
            case 2 -> "§f";
            case 3 -> "§a";
            case 4 -> "§b";
            case 5 -> "§e";
            case 6 -> "§6";
            default -> "§7";
        };
    }

    private String trimToWidth(String text, int maxWidth) {
        if (font.width(text) <= maxWidth) return text;
        String suffix = "...";
        String raw = text;
        while (!raw.isEmpty() && font.width(raw + suffix) > maxWidth) {
            raw = raw.substring(0, raw.length() - 1);
        }
        return raw + suffix;
    }

    // ── Networking ────────────────────────────────────────────────────────────

    private void sendAction(String action, String p1, String p2) {
        pendingGuildId = menu.hasGuild ? menu.guildId : null;
        pendingActiveTab = activeTab;
        pendingSelectedTeamId = selectedTeamId;
        pendingDeployTeamIdx = deployTeamIdx;
        if ("deploy_team".equals(action)) {
            try {
                pendingDeployTeamIdx = nextIdleTeamIndexAfter(UUID.fromString(p1));
            } catch (IllegalArgumentException ignored) {
            }
        }
        SololevelingMod.PACKET_HANDLER.send(PacketDistributor.SERVER.noArg(),
                new GuildActionMessage(action, menu.computerPos, p1, p2));
    }

    // ── Drawing utilities ─────────────────────────────────────────────────────

    private void drawCentered(GuiGraphics gg, String text, int cx, int y) {
        gg.drawString(font, text, cx - font.width(text)/2, y, 0xFFFFFFFF, false);
    }

    private void fillRounded(GuiGraphics gg, int x, int y, int w, int h, int color, int r) {
        gg.fill(x+r, y, x+w-r, y+h, color);
        gg.fill(x, y+r, x+r, y+h-r, color);
        gg.fill(x+w-r, y+r, x+w, y+h-r, color);
    }

    private void drawBorder(GuiGraphics gg, int x, int y, int w, int h, int color, int t) {
        gg.fill(x, y, x+w, y+t, color); gg.fill(x, y+h-t, x+w, y+h, color);
        gg.fill(x, y, x+t, y+h, color); gg.fill(x+w-t, y, x+w, y+h, color);
    }

    private void enableScissor(GuiGraphics gg, int x, int y, int w, int h) {
		ResponsiveGuiScale.enableScissor(gg, responsiveTransform(), x, y, x + w, y + h);
    }

    private void disableScissor(GuiGraphics gg) { gg.disableScissor(); }

    private String guildLevelBadge(int level) {
        return switch (level) {
            case 1 -> "§7[E]"; case 2 -> "§7[D]"; case 3 -> "§a[C]";
            case 4 -> "§b[B]"; case 5 -> "§e[A]"; default -> "§6[S]";
        };
    }
}
