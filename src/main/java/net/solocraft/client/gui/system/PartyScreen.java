package net.solocraft.client.gui.system;

import net.solocraft.SololevelingMod;
import net.solocraft.client.gui.PartyClientState;
import net.solocraft.client.gui.PartyClientState.JoinRequest;
import net.solocraft.client.gui.PartyClientState.Member;
import net.solocraft.client.gui.PartyClientState.NearbyParty;
import net.solocraft.client.gui.PartyClientState.Snapshot;
import net.solocraft.network.AbilitiesGUIButtonMessage;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Multiplayer party console. The roster is presented as a linked signal
 * constellation instead of an inventory-style grid.
 */
public class PartyScreen extends SystemScreen {
	private static final int SECTION_FILL = 0x8A060D1F;
	private static final int SECTION_INNER = 0x4A102338;
	private static final int OFFLINE = 0xFF5B6D78;
	private static final int WARNING = 0xFFFFD966;
	private static final int DANGER = 0xFFFF5D67;

	private static final int CONSTELLATION_X = 10;
	private static final int CONSTELLATION_Y = 46;
	private static final int CONSTELLATION_W = 320;
	private static final int CONSTELLATION_H = 174;
	private static final int SIGNALS_X = 336;
	private static final int SIGNALS_Y = 46;
	private static final int SIGNALS_W = 214;
	private static final int SIGNALS_H = 174;
	private static final int CONTROL_Y = 226;
	private static final int CONTROL_H = 116;
	private static final int PERIODIC_REFRESH_TICKS = 100;
	private static final int GLOW_COMMIT_DELAY_TICKS = 4;
	private static final int GLOW_ACK_TIMEOUT_TICKS = 100;

	private final boolean returnToSkills;
	private Snapshot party = PartyClientState.snapshot();
	private UUID selectedMemberId;
	private UUID selectedPartyId;
	private UUID selectedRequestId;
	private EditBox partyName;
	private String draftPartyName = "";
	private int nearbyPage;
	private int requestPage;
	private int localGlowColor;
	private boolean localGlowEnabled;
	private boolean glowEditDirty;
	private boolean awaitingGlowAck;
	private boolean pendingGlowEnabled;
	private int pendingGlowColor;
	private int glowEditIdleTicks;
	private int glowAckTicks;
	private int refreshTicks;
	private boolean deferredRebuild;
	private String confirmationAction = "";
	private UUID confirmationTarget;

	public PartyScreen(boolean returnToSkills) {
		super(Component.literal("PARTY LINK"));
		this.returnToSkills = returnToSkills;
		this.panelW = 560;
		this.panelH = 350;
		this.localGlowColor = party.glowColor();
		this.localGlowEnabled = party.glowEnabled();
	}

	@Override
	protected void init() {
		boolean restoreNameFocus = partyName != null && partyName.isFocused();
		captureDraftName();
		super.init();
		this.party = PartyClientState.snapshot();
		if (!glowEditDirty && !awaitingGlowAck) {
			this.localGlowColor = party.glowColor();
			this.localGlowEnabled = party.glowEnabled();
		}
		validateSelections();
		buildWidgets();
		if (restoreNameFocus && partyName != null)
			setInitialFocus(partyName);
		PartyClientState.requestSnapshot();
	}

	/**
	 * Called by the packet-backed client state. Widgets are rebuilt in place so
	 * a refresh never restarts the System window animation.
	 */
	public void onPartyStateChanged(Snapshot updated) {
		boolean activeScreen = this.minecraft != null && this.minecraft.screen == this;
		if (activeScreen)
			captureDraftName();
		Snapshot next = updated == null ? PartyClientState.snapshot() : updated;
		if (awaitingGlowAck && next.glowEnabled() == pendingGlowEnabled
				&& next.glowColor() == pendingGlowColor) {
			awaitingGlowAck = false;
			glowAckTicks = 0;
		}
		boolean preserveGlowDraft = glowEditDirty || awaitingGlowAck;
		this.party = next;
		if (!preserveGlowDraft) {
			this.localGlowColor = party.glowColor();
			this.localGlowEnabled = party.glowEnabled();
		}
		validateSelections();
		if (activeScreen) {
			if (hasActiveDraftInteraction()) {
				deferredRebuild = true;
			} else {
				deferredRebuild = false;
				clearWidgets();
				buildWidgets();
			}
		}
	}

	@Override
	public void tick() {
		super.tick();
		if (glowEditDirty) {
			glowEditIdleTicks++;
			if (glowEditIdleTicks >= GLOW_COMMIT_DELAY_TICKS)
				commitGlowEdit();
		}
		if (awaitingGlowAck && ++glowAckTicks >= GLOW_ACK_TIMEOUT_TICKS) {
			awaitingGlowAck = false;
			glowAckTicks = 0;
			localGlowColor = party.glowColor();
			localGlowEnabled = party.glowEnabled();
			deferredRebuild = true;
		}
		if (deferredRebuild && !hasActiveDraftInteraction()) {
			deferredRebuild = false;
			rebuildPartyWidgets();
		}
		if (++refreshTicks >= PERIODIC_REFRESH_TICKS) {
			refreshTicks = 0;
			if (!hasActiveDraftInteraction() && !awaitingGlowAck)
				PartyClientState.requestSnapshot();
		}
	}

	@Override
	protected boolean allowsNonSystemAccess() {
		return true;
	}

	private void buildWidgets() {
		partyName = null;
		addRenderableWidget(new SystemButton(panelX + 3, panelY + 3, 46, 12,
				Component.literal("< Back"), button -> goBack()));
		addRenderableWidget(new SystemButton(panelX + panelW - 53, panelY + 3, 50, 12,
				Component.literal("Refresh"), button -> {
					clearConfirmation();
					PartyClientState.refresh();
				}));

		if (party.inParty())
			buildMemberNodes();
		else
			buildCreateControls();

		buildNearbyControls();
		if (isLeader())
			buildRequestControls();
		buildManagementControls();
		if (party.inParty())
			buildGlowControls();
	}

	private void buildCreateControls() {
		int x = panelX + CONSTELLATION_X + 24;
		int y = panelY + CONSTELLATION_Y + 82;
		partyName = new EditBox(this.font, x, y, 190, 20, Component.literal("Party name"));
		partyName.setMaxLength(24);
		partyName.setHint(Component.literal("Name your link").withStyle(style -> style.withColor(TEXT_SUB)));
		partyName.setTextColor(TEXT_MAIN);
		partyName.setBordered(true);
		partyName.setValue(draftPartyName);
		addRenderableWidget(partyName);
		addRenderableWidget(new SystemButton(x + 196, y, 76, 20,
				Component.literal("Create"), button -> createParty()));
	}

	private void buildMemberNodes() {
		for (NodePlacement placement : nodePlacements()) {
			Member member = placement.member();
			addRenderableWidget(new MemberNodeButton(
					panelX + placement.x(), panelY + placement.y(), 88, 27, member,
					Objects.equals(selectedMemberId, member.id()),
					button -> {
						clearConfirmation();
						selectedMemberId = member.id();
						rebuildPartyWidgets();
					}));
		}
	}

	private void buildNearbyControls() {
		List<NearbyParty> nearby = party.nearby();
		int pageCount = Math.max(1, (nearby.size() + 2) / 3);
		nearbyPage = Math.max(0, Math.min(nearbyPage, pageCount - 1));
		int start = nearbyPage * 3;
		int rows = Math.min(3, nearby.size() - start);
		for (int index = 0; index < rows; index++) {
			NearbyParty signal = nearby.get(start + index);
			int y = panelY + SIGNALS_Y + 20 + index * 22;
			addRenderableWidget(new NearbySignalButton(
					panelX + SIGNALS_X + 8, y, SIGNALS_W - 16, 20, signal,
					Objects.equals(selectedPartyId, signal.id()),
					button -> {
						clearConfirmation();
						selectedPartyId = signal.id();
						rebuildPartyWidgets();
					}));
		}
		if (pageCount > 1) {
			addRenderableWidget(new SystemButton(panelX + SIGNALS_X + SIGNALS_W - 46,
					panelY + SIGNALS_Y + 2, 17, 12, Component.literal("<"),
					button -> changeNearbyPage(-1)));
			addRenderableWidget(new SystemButton(panelX + SIGNALS_X + SIGNALS_W - 26,
					panelY + SIGNALS_Y + 2, 17, 12, Component.literal(">"),
					button -> changeNearbyPage(1)));
		}

		NearbyParty selected = selectedNearby();
		if (!party.inParty() && selected != null) {
			String label = selected.requested() ? "Cancel Request"
					: selected.available() ? "Request Link" : "Signal Unavailable";
			addRenderableWidget(new SignalActionButton(
					panelX + SIGNALS_X + 8, panelY + SIGNALS_Y + 89,
					SIGNALS_W - 16, 18, Component.literal(label),
					selected.requested() || selected.available(),
					button -> requestJoin(selected)));
		}
	}

	private void buildRequestControls() {
		List<JoinRequest> requests = party.requests();
		int pageCount = Math.max(1, requests.size());
		requestPage = Math.max(0, Math.min(requestPage, pageCount - 1));
		int start = requestPage;
		int rows = Math.min(1, requests.size() - start);
		for (int index = 0; index < rows; index++) {
			JoinRequest request = requests.get(start);
			int y = panelY + SIGNALS_Y + 134;
			addRenderableWidget(new RequestSignalButton(
					panelX + SIGNALS_X + 8, y, SIGNALS_W - 16, 17, request,
					Objects.equals(selectedRequestId, request.id()),
					button -> {
						clearConfirmation();
						selectedRequestId = request.id();
						rebuildPartyWidgets();
					}));
		}
		if (pageCount > 1) {
			addRenderableWidget(new SystemButton(panelX + SIGNALS_X + SIGNALS_W - 46,
					panelY + SIGNALS_Y + 118, 17, 12, Component.literal("<"),
					button -> changeRequestPage(-1)));
			addRenderableWidget(new SystemButton(panelX + SIGNALS_X + SIGNALS_W - 26,
					panelY + SIGNALS_Y + 118, 17, 12, Component.literal(">"),
					button -> changeRequestPage(1)));
		}

		JoinRequest selected = selectedRequest();
		if (selected != null) {
			int x = panelX + SIGNALS_X + 8;
			int y = panelY + SIGNALS_Y + 154;
			addRenderableWidget(new SystemButton(x, y, 94, 14,
					Component.literal("Accept"), button -> {
						clearConfirmation();
						playerAction("accept_request", selected.id());
					}));
			addRenderableWidget(new DestructiveButton(x + 100, y, 98, 14,
					Component.literal(confirmationLabel("deny_request", selected.id(),
							"Deny", "Confirm Deny")),
					button -> confirmAction("deny_request", selected.id())));
		}
	}

	private void buildManagementControls() {
		int x = panelX + 18;
		int y = panelY + CONTROL_Y + 42;
		if (!party.inParty())
			return;

		if (isLeader()) {
			addRenderableWidget(new SystemButton(x, y, 112, 18,
					Component.literal(party.discoverable() ? "Signal: Public" : "Signal: Hidden"),
					button -> {
						clearConfirmation();
						PartyClientState.sendAction("toggle_discoverable");
					}));
			addRenderableWidget(new DestructiveButton(x + 130, y, 112, 18,
					Component.literal(confirmationLabel("disband", null,
							"Disband", "Confirm Disband")),
					button -> confirmAction("disband", null)));
		} else {
			addRenderableWidget(new DestructiveButton(x + 130, y, 112, 18,
					Component.literal(confirmationLabel("leave", null,
							"Leave Party", "Confirm Leave")),
					button -> confirmAction("leave", null)));
		}

		Member selected = selectedMember();
		if (isLeader() && selected != null && !selected.leader() && !isLocalPlayer(selected.id())) {
			if (selected.online()) {
				addRenderableWidget(new DestructiveButton(x, y + 25, 112, 18,
						Component.literal(confirmationLabel("transfer_leader", selected.id(),
								"Transfer Lead", "Confirm Transfer")),
						button -> confirmAction("transfer_leader", selected.id())));
			}
			addRenderableWidget(new DestructiveButton(x + 130, y + 25, 112, 18,
					Component.literal(confirmationLabel("kick_member", selected.id(),
							"Remove Member", "Confirm Remove")),
					button -> confirmAction("kick_member", selected.id())));
		}
	}

	private void buildGlowControls() {
		int x = panelX + 284;
		int y = panelY + CONTROL_Y + 20;
		addRenderableWidget(new SystemButton(x, y, 90, 18,
				Component.literal(localGlowEnabled ? "Outline: ON" : "Outline: OFF"),
				button -> {
					clearConfirmation();
					sendGlow(!localGlowEnabled, localGlowColor);
				}));

		int[] presets = {0x3FC6FF, 0xFFD966, 0xA977FF, 0xFF5B8E};
		String[] labels = {"CYAN", "GOLD", "VIOLET", "ROSE"};
		for (int index = 0; index < presets.length; index++) {
			final int color = presets[index];
			addRenderableWidget(new ColorPresetButton(
					x + index * 62, y + 23, 57, 17, Component.literal(labels[index]), color,
					button -> {
						clearConfirmation();
						localGlowColor = color;
						sendGlow(localGlowEnabled, color);
					}));
		}

		addRenderableWidget(rgbSlider(x, y + 46, "R", 16));
		addRenderableWidget(rgbSlider(x, y + 63, "G", 8));
		addRenderableWidget(rgbSlider(x, y + 80, "B", 0));
	}

	private SystemSlider rgbSlider(int x, int y, String channel, int shift) {
		int value = localGlowColor >> shift & 0xFF;
		return new SystemSlider(x, y, 258, 14, value / 255.0D,
				position -> Component.literal(channel + " " + channelValue(position)),
				position -> {
					int channelValue = channelValue(position);
					localGlowColor = localGlowColor & ~(0xFF << shift) | channelValue << shift;
					glowEditDirty = true;
					glowEditIdleTicks = 0;
				},
				this::commitGlowEdit);
	}

	private void rebuildPartyWidgets() {
		captureDraftName();
		clearWidgets();
		buildWidgets();
	}

	private void captureDraftName() {
		if (partyName != null)
			draftPartyName = partyName.getValue();
	}

	private void changeNearbyPage(int direction) {
		clearConfirmation();
		int pageCount = Math.max(1, (party.nearby().size() + 2) / 3);
		nearbyPage = Math.floorMod(nearbyPage + direction, pageCount);
		int start = nearbyPage * 3;
		selectedPartyId = party.nearby().isEmpty() ? null
				: party.nearby().get(Math.min(start, party.nearby().size() - 1)).id();
		rebuildPartyWidgets();
	}

	private void changeRequestPage(int direction) {
		clearConfirmation();
		int pageCount = Math.max(1, party.requests().size());
		requestPage = Math.floorMod(requestPage + direction, pageCount);
		int start = requestPage;
		selectedRequestId = party.requests().isEmpty() ? null
				: party.requests().get(Math.min(start, party.requests().size() - 1)).id();
		rebuildPartyWidgets();
	}

	private void createParty() {
		if (partyName == null)
			return;
		String name = partyName.getValue().trim();
		if (name.isEmpty())
			return;
		CompoundTag payload = new CompoundTag();
		payload.putString("Name", name);
		PartyClientState.sendAction("create", payload);
	}

	private void requestJoin(NearbyParty selected) {
		if (selected.id() == null || !selected.requested() && !selected.available())
			return;
		CompoundTag payload = new CompoundTag();
		payload.putUUID("PartyId", selected.id());
		PartyClientState.sendAction(selected.requested() ? "cancel_request" : "request_join", payload);
	}

	private void playerAction(String action, UUID playerId) {
		if (playerId == null)
			return;
		CompoundTag payload = new CompoundTag();
		payload.putUUID("PlayerId", playerId);
		PartyClientState.sendAction(action, payload);
	}

	private void sendGlow(boolean enabled, int color) {
		localGlowEnabled = enabled;
		localGlowColor = color & 0xFFFFFF;
		pendingGlowEnabled = enabled;
		pendingGlowColor = localGlowColor;
		glowEditDirty = false;
		glowEditIdleTicks = 0;
		awaitingGlowAck = true;
		glowAckTicks = 0;
		CompoundTag payload = new CompoundTag();
		payload.putBoolean("Enabled", enabled);
		payload.putInt("Color", localGlowColor);
		PartyClientState.sendAction("set_glow", payload);
	}

	private void commitGlowEdit() {
		if (glowEditDirty)
			sendGlow(localGlowEnabled, localGlowColor);
	}

	private void confirmAction(String action, UUID target) {
		if (!Objects.equals(confirmationAction, action)
				|| !Objects.equals(confirmationTarget, target)) {
			confirmationAction = action;
			confirmationTarget = target;
			rebuildPartyWidgets();
			return;
		}
		clearConfirmation();
		if (target == null)
			PartyClientState.sendAction(action);
		else
			playerAction(action, target);
	}

	private String confirmationLabel(String action, UUID target, String normal,
			String confirmation) {
		return Objects.equals(confirmationAction, action)
				&& Objects.equals(confirmationTarget, target) ? confirmation : normal;
	}

	private void clearConfirmation() {
		confirmationAction = "";
		confirmationTarget = null;
	}

	private boolean hasActiveDraftInteraction() {
		return glowEditDirty || !party.inParty() && partyName != null
				&& partyName.isFocused();
	}

	private void goBack() {
		if (this.minecraft == null)
			return;
		if (!returnToSkills) {
			openChild(new SystemPanelScreen());
			return;
		}
		Player player = this.minecraft.player;
		if (player == null)
			return;
		BlockPos position = player.blockPosition();
		this.minecraft.setScreen(null);
		SololevelingMod.PACKET_HANDLER.sendToServer(new AbilitiesGUIButtonMessage(
				5, position.getX(), position.getY(), position.getZ()));
	}

	@Override
	protected void renderContent(GuiGraphics graphics, int mouseX, int mouseY,
			float partialTicks) {
		drawSection(graphics, panelX + CONSTELLATION_X, panelY + CONSTELLATION_Y,
				CONSTELLATION_W, CONSTELLATION_H, "LINKED CONSTELLATION");
		drawSection(graphics, panelX + SIGNALS_X, panelY + SIGNALS_Y,
				SIGNALS_W, SIGNALS_H, "NEARBY SIGNALS");
		drawSection(graphics, panelX + 10, panelY + CONTROL_Y, 260, CONTROL_H,
				party.inParty() ? "PARTY PROTOCOL" : "ESTABLISH LINK");
		drawSection(graphics, panelX + 276, panelY + CONTROL_Y, 274, CONTROL_H,
				"OUTLINE CHANNEL");

		drawTopStatus(graphics);
		if (party.inParty())
			drawConstellation(graphics);
		else
			drawCreateState(graphics);
		drawSignalsState(graphics);
		drawManagementState(graphics);
		drawGlowState(graphics);
	}

	private void drawTopStatus(GuiGraphics graphics) {
		String state = party.inParty()
				? party.partyName() + "  //  " + party.members().size() + "/" + party.maxMembers()
				: "NO ACTIVE LINK  //  SELECT A SIGNAL OR CREATE YOUR OWN";
		graphics.drawString(this.font, fit(state, panelW - 24), panelX + 12,
				panelY + 27, party.inParty() ? ACCENT : TEXT_SUB, false);

		String notice = party.notice();
		if (!notice.isEmpty())
			graphics.drawString(this.font, fit(notice, panelW - 24),
					panelX + panelW - 12
							- this.font.width(fit(notice, panelW - 24)),
					panelY + 37, WARNING, false);
	}

	private void drawCreateState(GuiGraphics graphics) {
		int x = panelX + CONSTELLATION_X;
		int y = panelY + CONSTELLATION_Y;
		drawCentered(graphics, "CREATE A NEW PARTY LINK", x, y + 40,
				CONSTELLATION_W, TEXT_MAIN);
		drawCentered(graphics, "You control visibility, membership and outline channel.",
				x, y + 56, CONSTELLATION_W, TEXT_SUB);
		drawCircuitBrackets(graphics, x + 22, y + 74, CONSTELLATION_W - 44, 40);
		drawCentered(graphics, "Nearby public signals remain visible on the right.",
				x, y + 132, CONSTELLATION_W, TEXT_SUB);
	}

	private void drawConstellation(GuiGraphics graphics) {
		List<NodePlacement> placements = nodePlacements();
		NodePlacement leader = placements.stream()
				.filter(placement -> placement.member().leader())
				.findFirst().orElse(placements.isEmpty() ? null : placements.get(0));
		if (leader != null) {
			int coreX = panelX + leader.x() + 44;
			int coreY = panelY + leader.y() + 13;
			for (NodePlacement placement : placements) {
				if (placement == leader)
					continue;
				int nodeX = panelX + placement.x() + 44;
				int nodeY = panelY + placement.y() + 13;
				drawSignalLine(graphics, coreX, coreY, nodeX, nodeY,
						placement.member().online() ? ACCENT_SOFT : 0x335B6D78);
			}
			graphics.fill(coreX - 2, coreY - 2, coreX + 3, coreY + 3, ACCENT_SOFT);
		}
		if (party.members().size() > placements.size()) {
			String overflow = "+" + (party.members().size() - placements.size()) + " REMOTE";
			graphics.drawString(this.font, overflow,
					panelX + CONSTELLATION_X + CONSTELLATION_W - 62,
					panelY + CONSTELLATION_Y + CONSTELLATION_H - 12, TEXT_SUB, false);
		}
	}

	private void drawSignalsState(GuiGraphics graphics) {
		int x = panelX + SIGNALS_X;
		int y = panelY + SIGNALS_Y;
		if (party.nearby().isEmpty())
			drawCentered(graphics, "NO PUBLIC SIGNALS IN RANGE", x, y + 40,
					SIGNALS_W, OFFLINE);
		else if (party.nearby().size() > 3) {
			int pages = (party.nearby().size() + 2) / 3;
			String page = (nearbyPage + 1) + "/" + pages;
			graphics.drawString(this.font, page,
					x + SIGNALS_W - 52 - this.font.width(page), y + 5,
					TEXT_SUB, false);
		}

		graphics.fill(x + 8, y + 115, x + SIGNALS_W - 8, y + 116, ACCENT_SOFT);
		graphics.drawString(this.font, "REQUEST INBOX", x + 8, y + 121,
				isLeader() ? ACCENT : OFFLINE, false);
		if (isLeader() && party.requests().size() > 1) {
			String page = (requestPage + 1) + "/" + party.requests().size();
			graphics.drawString(this.font, page,
					x + SIGNALS_W - 52 - this.font.width(page), y + 121,
					TEXT_SUB, false);
		}
		if (!party.inParty())
			graphics.drawString(this.font, "Join a party to receive requests.", x + 8,
					y + 140, OFFLINE, false);
		else if (!isLeader())
			graphics.drawString(this.font, "Leader channel only.", x + 8,
					y + 140, OFFLINE, false);
		else if (party.requests().isEmpty())
			graphics.drawString(this.font, "No pending requests.", x + 8,
					y + 140, TEXT_SUB, false);
	}

	private void drawManagementState(GuiGraphics graphics) {
		int x = panelX + 18;
		int y = panelY + CONTROL_Y;
		if (!party.inParty()) {
			graphics.drawString(this.font,
					fit("Create from the constellation console above.", 230),
					x, y + 19, TEXT_SUB, false);
			graphics.drawString(this.font, "Public visibility starts enabled.", x,
					y + 76, TEXT_SUB, false);
			graphics.drawString(this.font, "The leader can change it at any time.", x,
					y + 88, TEXT_SUB, false);
			return;
		}

		Member selected = selectedMember();
		String leader = party.leaderName().isEmpty() ? "Unknown" : party.leaderName();
		graphics.drawString(this.font, "LEADER  " + fit(leader, 115), x, y + 19,
				TEXT_SUB, false);
		if (selected != null) {
			String detail = selected.name() + "  //  LV." + selected.level()
					+ (selected.rank().isEmpty() ? "" : "  " + selected.rank());
			graphics.drawString(this.font, fit(detail, 230), x, y + 31,
					selected.online() ? TEXT_MAIN : OFFLINE, false);
		} else {
			graphics.drawString(this.font, "Select a constellation node to manage it.",
					x, y + 31, TEXT_SUB, false);
		}
		if (!confirmationAction.isEmpty())
			graphics.drawString(this.font, "Press the highlighted action again to confirm.",
					x, y + 94, WARNING, false);
	}

	private void drawGlowState(GuiGraphics graphics) {
		int x = panelX + 284;
		int y = panelY + CONTROL_Y;
		if (!party.inParty()) {
			graphics.drawString(this.font, "OUTLINE CHANNEL OFFLINE", x, y + 28,
					OFFLINE, false);
			graphics.drawString(this.font, "Party members can share a personal", x,
					y + 48, TEXT_SUB, false);
			graphics.drawString(this.font, "client-side outline once linked.", x,
					y + 60, TEXT_SUB, false);
			return;
		}

		int previewX = x + 102;
		int previewY = y + 20;
		int previewColor = 0xFF000000 | localGlowColor;
		graphics.fill(previewX, previewY, previewX + 156, previewY + 18, 0xAA030712);
		graphics.fill(previewX + 1, previewY + 1, previewX + 155, previewY + 2,
				previewColor);
		graphics.fill(previewX + 1, previewY + 16, previewX + 155, previewY + 17,
				previewColor);
		graphics.fill(previewX + 1, previewY + 1, previewX + 2, previewY + 17,
				previewColor);
		graphics.fill(previewX + 154, previewY + 1, previewX + 155, previewY + 17,
				previewColor);
		String hex = String.format("#%06X", localGlowColor & 0xFFFFFF);
		graphics.drawCenteredString(this.font, hex, previewX + 78, previewY + 5,
				TEXT_MAIN);
	}

	private void drawSection(GuiGraphics graphics, int x, int y, int width, int height,
			String label) {
		graphics.fill(x, y, x + width, y + height, SECTION_FILL);
		graphics.fill(x + 1, y + 17, x + width - 1, y + height - 1, SECTION_INNER);
		outline(graphics, x, y, width, height, ACCENT_DIM);
		graphics.fill(x, y + 16, x + width, y + 17, ACCENT_SOFT);
		graphics.drawString(this.font, label, x + 8, y + 5, ACCENT, false);
		graphics.fill(x + width - 20, y + 5, x + width - 8, y + 6, ACCENT_DIM);
		graphics.fill(x + width - 8, y + 5, x + width - 7, y + 12, ACCENT);
	}

	private void drawCircuitBrackets(GuiGraphics graphics, int x, int y, int width,
			int height) {
		graphics.fill(x, y, x + 22, y + 1, ACCENT_DIM);
		graphics.fill(x, y, x + 1, y + 10, ACCENT_DIM);
		graphics.fill(x + width - 22, y, x + width, y + 1, ACCENT_DIM);
		graphics.fill(x + width - 1, y, x + width, y + 10, ACCENT_DIM);
		graphics.fill(x, y + height - 1, x + 22, y + height, ACCENT_DIM);
		graphics.fill(x, y + height - 10, x + 1, y + height, ACCENT_DIM);
		graphics.fill(x + width - 22, y + height - 1, x + width, y + height,
				ACCENT_DIM);
		graphics.fill(x + width - 1, y + height - 10, x + width, y + height,
				ACCENT_DIM);
	}

	private void drawSignalLine(GuiGraphics graphics, int fromX, int fromY, int toX,
			int toY, int color) {
		int middleX = (fromX + toX) / 2;
		fillHorizontal(graphics, fromX, middleX, fromY, color);
		fillVertical(graphics, middleX, fromY, toY, color);
		fillHorizontal(graphics, middleX, toX, toY, color);
		graphics.fill(middleX - 1, toY - 1, middleX + 2, toY + 2, color);
	}

	private static void fillHorizontal(GuiGraphics graphics, int x0, int x1, int y,
			int color) {
		graphics.fill(Math.min(x0, x1), y, Math.max(x0, x1) + 1, y + 1, color);
	}

	private static void fillVertical(GuiGraphics graphics, int x, int y0, int y1,
			int color) {
		graphics.fill(x, Math.min(y0, y1), x + 1, Math.max(y0, y1) + 1, color);
	}

	private static void outline(GuiGraphics graphics, int x, int y, int width,
			int height, int color) {
		graphics.fill(x, y, x + width, y + 1, color);
		graphics.fill(x, y + height - 1, x + width, y + height, color);
		graphics.fill(x, y, x + 1, y + height, color);
		graphics.fill(x + width - 1, y, x + width, y + height, color);
	}

	private void drawCentered(GuiGraphics graphics, String text, int x, int y,
			int width, int color) {
		graphics.drawCenteredString(this.font, fit(text, width - 12),
				x + width / 2, y, color);
	}

	private String fit(String text, int maximumWidth) {
		if (this.font.width(text) <= maximumWidth)
			return text;
		String suffix = "...";
		return this.font.plainSubstrByWidth(text,
				Math.max(1, maximumWidth - this.font.width(suffix))) + suffix;
	}

	private List<NodePlacement> nodePlacements() {
		List<Member> ordered = new ArrayList<>(party.members());
		ordered.sort(Comparator.comparing(Member::leader).reversed()
				.thenComparing(Member::name, String.CASE_INSENSITIVE_ORDER));
		int[][] positions = {
				{CONSTELLATION_X + 116, CONSTELLATION_Y + 75},
				{CONSTELLATION_X + 18, CONSTELLATION_Y + 28},
				{CONSTELLATION_X + 214, CONSTELLATION_Y + 28},
				{CONSTELLATION_X + 8, CONSTELLATION_Y + 112},
				{CONSTELLATION_X + 224, CONSTELLATION_Y + 112},
				{CONSTELLATION_X + 62, CONSTELLATION_Y + 141},
				{CONSTELLATION_X + 170, CONSTELLATION_Y + 141},
				{CONSTELLATION_X + 116, CONSTELLATION_Y + 24}
		};
		List<NodePlacement> placements = new ArrayList<>();
		for (int index = 0; index < Math.min(positions.length, ordered.size()); index++)
			placements.add(new NodePlacement(ordered.get(index), positions[index][0],
					positions[index][1]));
		return placements;
	}

	private boolean isLeader() {
		Player player = Minecraft.getInstance().player;
		return party.inParty() && player != null
				&& Objects.equals(player.getUUID(), party.leaderId());
	}

	private boolean isLocalPlayer(UUID playerId) {
		Player player = Minecraft.getInstance().player;
		return player != null && Objects.equals(player.getUUID(), playerId);
	}

	private Member selectedMember() {
		return party.members().stream()
				.filter(member -> Objects.equals(member.id(), selectedMemberId))
				.findFirst().orElse(null);
	}

	private NearbyParty selectedNearby() {
		return party.nearby().stream()
				.filter(nearby -> Objects.equals(nearby.id(), selectedPartyId))
				.findFirst().orElse(null);
	}

	private JoinRequest selectedRequest() {
		return party.requests().stream()
				.filter(request -> Objects.equals(request.id(), selectedRequestId))
				.findFirst().orElse(null);
	}

	private void validateSelections() {
		if (party.inParty())
			draftPartyName = "";
		if (selectedMember() == null)
			selectedMemberId = party.members().stream()
					.filter(Member::leader).map(Member::id).findFirst()
					.orElse(party.members().isEmpty() ? null : party.members().get(0).id());
		if (selectedNearby() == null)
			selectedPartyId = party.nearby().isEmpty() ? null : party.nearby().get(0).id();
		int nearbyIndex = indexOfNearby(selectedPartyId);
		nearbyPage = nearbyIndex < 0 ? 0 : nearbyIndex / 3;
		if (selectedRequest() == null)
			selectedRequestId = party.requests().isEmpty() ? null : party.requests().get(0).id();
		int requestIndex = indexOfRequest(selectedRequestId);
		requestPage = requestIndex < 0 ? 0 : requestIndex;
		if (!confirmationAction.isEmpty()) {
			boolean leaderAction = confirmationAction.equals("deny_request")
					|| confirmationAction.equals("transfer_leader")
					|| confirmationAction.equals("kick_member")
					|| confirmationAction.equals("disband");
			boolean targetStillValid = confirmationTarget == null
					? party.inParty()
					: confirmationAction.equals("deny_request")
							? indexOfRequest(confirmationTarget) >= 0
							: party.members().stream()
									.anyMatch(member -> Objects.equals(
											member.id(), confirmationTarget));
			if (!targetStillValid || leaderAction && !isLeader())
				clearConfirmation();
		}
	}

	private int indexOfNearby(UUID partyId) {
		for (int index = 0; index < party.nearby().size(); index++) {
			if (Objects.equals(party.nearby().get(index).id(), partyId))
				return index;
		}
		return -1;
	}

	private int indexOfRequest(UUID playerId) {
		for (int index = 0; index < party.requests().size(); index++) {
			if (Objects.equals(party.requests().get(index).id(), playerId))
				return index;
		}
		return -1;
	}

	private static int channelValue(double position) {
		return Math.max(0, Math.min(255, (int) Math.round(position * 255.0D)));
	}

	private record NodePlacement(Member member, int x, int y) {
	}

	private static class MemberNodeButton extends Button {
		private final Member member;
		private final boolean selected;

		MemberNodeButton(int x, int y, int width, int height, Member member,
				boolean selected, OnPress onPress) {
			super(x, y, width, height, Component.literal(member.name()), onPress,
					DEFAULT_NARRATION);
			this.member = member;
			this.selected = selected;
		}

		@Override
		protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY,
				float partialTicks) {
			int border = selected || isHoveredOrFocused() ? ACCENT : member.online()
					? ACCENT_DIM : OFFLINE;
			int fill = selected ? 0x7A16334B : 0xB008101E;
			graphics.fill(getX(), getY(), getX() + width, getY() + height, fill);
			outline(graphics, getX(), getY(), width, height, border);

			int nodeColor = member.online() ? (member.leader() ? 0xFFFFD966 : ACCENT)
					: OFFLINE;
			graphics.fill(getX() + 5, getY() + 8, getX() + 16, getY() + 19,
					0xFF02060C);
			outline(graphics, getX() + 5, getY() + 8, 11, 11, nodeColor);
			if (member.leader())
				graphics.fill(getX() + 8, getY() + 11, getX() + 13,
						getY() + 16, nodeColor);

			Font font = Minecraft.getInstance().font;
			String name = trim(font, member.name().isEmpty() ? "Unknown" : member.name(), 64);
			graphics.drawString(font, name, getX() + 21, getY() + 4,
					member.online() ? TEXT_MAIN : OFFLINE, false);
			String detail = "LV." + member.level()
					+ (member.rank().isEmpty() ? "" : " " + member.rank());
			graphics.drawString(font, trim(font, detail, 64), getX() + 21,
					getY() + 15, member.leader() ? WARNING : TEXT_SUB, false);
		}
	}

	private static class NearbySignalButton extends Button {
		private final NearbyParty party;
		private final boolean selected;

		NearbySignalButton(int x, int y, int width, int height, NearbyParty party,
				boolean selected, OnPress onPress) {
			super(x, y, width, height, Component.literal(party.name()), onPress,
					DEFAULT_NARRATION);
			this.party = party;
			this.selected = selected;
		}

		@Override
		protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY,
				float partialTicks) {
			int border = selected || isHoveredOrFocused() ? ACCENT : ACCENT_DIM;
			if (!party.available() && !party.requested())
				border = OFFLINE;
			graphics.fill(getX(), getY(), getX() + width, getY() + height,
					selected ? 0x7A16334B : 0x9A08101E);
			outline(graphics, getX(), getY(), width, height, border);
			Font font = Minecraft.getInstance().font;
			graphics.drawString(font, trim(font,
					party.name().isEmpty() ? "Unnamed Party" : party.name(), 105),
					getX() + 5, getY() + 3, TEXT_MAIN, false);
			String state = party.available() ? party.distance() + "m"
					: party.requested() ? "PENDING" : "CLOSED";
			String telemetry = party.members() + "/" + party.maxMembers() + "  " + state;
			int color = party.requested() ? WARNING
					: party.available() ? TEXT_SUB : OFFLINE;
			graphics.drawString(font, telemetry, getX() + width - font.width(telemetry) - 5,
					getY() + 3, color, false);
			graphics.drawString(font, "Lead: " + trim(font, party.leaderName(), 70),
					getX() + 5, getY() + 12, TEXT_SUB, false);
		}
	}

	private static class RequestSignalButton extends Button {
		private final JoinRequest request;
		private final boolean selected;

		RequestSignalButton(int x, int y, int width, int height, JoinRequest request,
				boolean selected, OnPress onPress) {
			super(x, y, width, height, Component.literal(request.name()), onPress,
					DEFAULT_NARRATION);
			this.request = request;
			this.selected = selected;
		}

		@Override
		protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY,
				float partialTicks) {
			int border = selected || isHoveredOrFocused() ? ACCENT
					: request.online() ? ACCENT_DIM : OFFLINE;
			graphics.fill(getX(), getY(), getX() + width, getY() + height,
					selected ? 0x7A16334B : 0x8808101E);
			outline(graphics, getX(), getY(), width, height, border);
			Font font = Minecraft.getInstance().font;
			graphics.drawString(font, trim(font, request.name(), width - 40),
					getX() + 5, getY() + 5,
					request.online() ? TEXT_MAIN : OFFLINE, false);
			String state = request.online() ? "LIVE" : "AWAY";
			graphics.drawString(font, state, getX() + width - font.width(state) - 5,
					getY() + 5, request.online() ? ACCENT : OFFLINE, false);
		}
	}

	private static class SignalActionButton extends Button {
		private final boolean enabled;

		SignalActionButton(int x, int y, int width, int height, Component message,
				boolean enabled, OnPress onPress) {
			super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
			this.enabled = enabled;
		}

		@Override
		public void onPress() {
			if (enabled)
				super.onPress();
		}

		@Override
		protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY,
				float partialTicks) {
			boolean hovered = enabled && isHoveredOrFocused();
			int border = !enabled ? OFFLINE : hovered ? 0xFF7FE4FF : ACCENT_DIM;
			int fill = !enabled ? 0x44101824
					: hovered ? 0x804FB8E8 : 0x55102338;
			graphics.fill(getX(), getY(), getX() + width, getY() + height, fill);
			outline(graphics, getX(), getY(), width, height, border);
			graphics.drawCenteredString(Minecraft.getInstance().font, getMessage(),
					getX() + width / 2, getY() + (height - 8) / 2,
					enabled ? TEXT_MAIN : OFFLINE);
		}
	}

	private static class DestructiveButton extends Button {
		DestructiveButton(int x, int y, int width, int height, Component message,
				OnPress onPress) {
			super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
		}

		@Override
		protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY,
				float partialTicks) {
			boolean hovered = isHoveredOrFocused();
			int border = hovered ? 0xFFFF8A91 : 0xFF9C3540;
			graphics.fill(getX(), getY(), getX() + width, getY() + height,
					hovered ? 0x884D151D : 0x6630141B);
			outline(graphics, getX(), getY(), width, height, border);
			graphics.drawCenteredString(Minecraft.getInstance().font, getMessage(),
					getX() + width / 2, getY() + (height - 8) / 2,
					hovered ? 0xFFFFFFFF : DANGER);
		}
	}

	private static class ColorPresetButton extends Button {
		private final int color;

		ColorPresetButton(int x, int y, int width, int height, Component message,
				int color, OnPress onPress) {
			super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
			this.color = color;
		}

		@Override
		protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY,
				float partialTicks) {
			int argb = 0xFF000000 | color;
			graphics.fill(getX(), getY(), getX() + width, getY() + height,
					isHoveredOrFocused() ? 0xAA18283A : 0x9908101E);
			outline(graphics, getX(), getY(), width, height,
					isHoveredOrFocused() ? 0xFFFFFFFF : argb);
			graphics.fill(getX() + 2, getY() + height - 3, getX() + width - 2,
					getY() + height - 1, argb);
			graphics.drawCenteredString(Minecraft.getInstance().font, getMessage(),
					getX() + width / 2, getY() + 3, TEXT_MAIN);
		}
	}

	private static String trim(Font font, String text, int width) {
		if (text == null)
			return "";
		if (font.width(text) <= width)
			return text;
		String suffix = "...";
		return font.plainSubstrByWidth(text,
				Math.max(1, width - font.width(suffix))) + suffix;
	}
}
