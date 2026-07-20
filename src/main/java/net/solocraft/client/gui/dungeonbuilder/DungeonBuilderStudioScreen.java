package net.solocraft.client.gui.dungeonbuilder;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.function.BooleanSupplier;

import org.lwjgl.glfw.GLFW;

import net.solocraft.client.gui.ResponsiveGuiScale;
import net.solocraft.client.gui.dungeonbuilder.DungeonBuilderStudioController.AssignAnchor;
import net.solocraft.client.gui.dungeonbuilder.DungeonBuilderStudioController.CaptureSnapshot;
import net.solocraft.client.gui.dungeonbuilder.DungeonBuilderStudioController.CreatePool;
import net.solocraft.client.gui.dungeonbuilder.DungeonBuilderStudioController.CreateProject;
import net.solocraft.client.gui.dungeonbuilder.DungeonBuilderStudioController.DeletePool;
import net.solocraft.client.gui.dungeonbuilder.DungeonBuilderStudioController.DeleteProject;
import net.solocraft.client.gui.dungeonbuilder.DungeonBuilderStudioController.DeleteDungeon;
import net.solocraft.client.gui.dungeonbuilder.DungeonBuilderStudioController.EditSocket;
import net.solocraft.client.gui.dungeonbuilder.DungeonBuilderStudioController.ExportDungeon;
import net.solocraft.client.gui.dungeonbuilder.DungeonBuilderStudioController.RequestSnapshot;
import net.solocraft.client.gui.dungeonbuilder.DungeonBuilderStudioController.NewDungeon;
import net.solocraft.client.gui.dungeonbuilder.DungeonBuilderStudioController.RunSimulation;
import net.solocraft.client.gui.dungeonbuilder.DungeonBuilderStudioController.SavePoolDraft;
import net.solocraft.client.gui.dungeonbuilder.DungeonBuilderStudioController.SelectProject;
import net.solocraft.client.gui.dungeonbuilder.DungeonBuilderStudioController.SelectDungeon;
import net.solocraft.client.gui.dungeonbuilder.DungeonBuilderStudioController.SetRoomRole;
import net.solocraft.client.gui.dungeonbuilder.DungeonBuilderStudioController.SetRoomWeight;
import net.solocraft.client.gui.dungeonbuilder.DungeonBuilderStudioController.SetProjectSettings;
import net.solocraft.client.gui.dungeonbuilder.DungeonBuilderStudioController.UpdateLayout;
import net.solocraft.client.gui.dungeonbuilder.DungeonBuilderStudioController.UpsertPoolEntry;
import net.solocraft.client.gui.dungeonbuilder.DungeonBuilderStudioController.ValidateDungeon;
import net.solocraft.client.gui.dungeonbuilder.DungeonBuilderStudioModel.Anchor;
import net.solocraft.client.gui.dungeonbuilder.DungeonBuilderStudioModel.AnchorKind;
import net.solocraft.client.gui.dungeonbuilder.DungeonBuilderStudioModel.Bounds;
import net.solocraft.client.gui.dungeonbuilder.DungeonBuilderStudioModel.DraftSummary;
import net.solocraft.client.gui.dungeonbuilder.DungeonBuilderStudioModel.Facing;
import net.solocraft.client.gui.dungeonbuilder.DungeonBuilderStudioModel.FootprintCell;
import net.solocraft.client.gui.dungeonbuilder.DungeonBuilderStudioModel.LayoutConnection;
import net.solocraft.client.gui.dungeonbuilder.DungeonBuilderStudioModel.LayoutDraft;
import net.solocraft.client.gui.dungeonbuilder.DungeonBuilderStudioModel.LayoutMode;
import net.solocraft.client.gui.dungeonbuilder.DungeonBuilderStudioModel.LevelRange;
import net.solocraft.client.gui.dungeonbuilder.DungeonBuilderStudioModel.LayoutNode;
import net.solocraft.client.gui.dungeonbuilder.DungeonBuilderStudioModel.MobPool;
import net.solocraft.client.gui.dungeonbuilder.DungeonBuilderStudioModel.Notice;
import net.solocraft.client.gui.dungeonbuilder.DungeonBuilderStudioModel.OptionalXp;
import net.solocraft.client.gui.dungeonbuilder.DungeonBuilderStudioModel.Point;
import net.solocraft.client.gui.dungeonbuilder.DungeonBuilderStudioModel.PoolEntry;
import net.solocraft.client.gui.dungeonbuilder.DungeonBuilderStudioModel.Project;
import net.solocraft.client.gui.dungeonbuilder.DungeonBuilderStudioModel.ProjectKind;
import net.solocraft.client.gui.dungeonbuilder.DungeonBuilderStudioModel.RoomRole;
import net.solocraft.client.gui.dungeonbuilder.DungeonBuilderStudioModel.RoomWeight;
import net.solocraft.client.gui.dungeonbuilder.DungeonBuilderStudioModel.Severity;
import net.solocraft.client.gui.dungeonbuilder.DungeonBuilderStudioModel.SelectorKind;
import net.solocraft.client.gui.dungeonbuilder.DungeonBuilderStudioModel.SimConnection;
import net.solocraft.client.gui.dungeonbuilder.DungeonBuilderStudioModel.SimRoom;
import net.solocraft.client.gui.dungeonbuilder.DungeonBuilderStudioModel.Simulation;
import net.solocraft.client.gui.dungeonbuilder.DungeonBuilderStudioModel.Socket;
import net.solocraft.client.gui.dungeonbuilder.DungeonBuilderStudioModel.SocketType;
import net.solocraft.client.gui.dungeonbuilder.DungeonBuilderStudioModel.SpawnRole;
import net.solocraft.client.gui.dungeonbuilder.DungeonBuilderStudioModel.Topology;
import net.solocraft.client.gui.dungeonbuilder.DungeonBuilderStudioModel.ValidationIssue;
import net.solocraft.client.gui.system.SystemScreen;
import net.solocraft.util.DungeonBuilderMode;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * Client-only authoring workspace. It renders immutable server snapshots and
 * emits typed intents through {@link DungeonBuilderStudioController}; it never
 * mutates world or SavedData state directly.
 */
public final class DungeonBuilderStudioScreen extends SystemScreen {
	private static final int WIDE_WIDTH = 500;
	private static final int WIDE_HEIGHT = 338;
	private static final int MIN_WIDTH = 312;
	private static final int MIN_HEIGHT = 236;
	private static final int TAB_HEIGHT = 18;
	private static final int ROW_HEIGHT = 27;

	private static final int RAISED = 0xE60C1423;
	private static final int CANVAS = 0xF0060D18;
	private static final int BORDER = 0xB82273A8;
	private static final int GRID = 0x202273A8;
	private static final int SELECTED = 0x70407FA0;
	private static final int SUCCESS = 0xFF5FE38C;
	private static final int WARNING = 0xFFFFD166;
	private static final int ERROR = 0xFFFF5D6C;
	private static final int INFO = 0xFF67D8FF;
	private static final int DISABLED = 0xFF667B8A;

	private enum Tab {
		ROOMS, ANCHORS, POOLS, LAYOUT, SIMULATE, EXPORT
	}

	private enum CompactPane {
		CANVAS, INSPECTOR
	}

	private enum Dialog {
		NONE, NEW_ROOM, NEW_POOL, POOL_ENTRY, ANCHOR_SETUP, PRESET_SETUP,
		DUNGEON_CATALOG, NEW_DUNGEON, DELETE_DUNGEON, LAYOUT_SETUP
	}

	private static final List<String> DUNGEON_RANKS = List.of("E", "D", "C", "B", "A", "S");

	private DungeonBuilderStudioModel model;
	private final DungeonBuilderStudioController controller;
	private Tab activeTab = Tab.ROOMS;
	private CompactPane compactPane = CompactPane.CANVAS;
	private boolean compact;
	private boolean requestSent;
	private boolean closeReported;
	private boolean canvasPanning;
	private boolean layoutNodeDragging;
	private boolean layoutDragBlocked;
	private GraphMap layoutDragMap;
	private double layoutDragRemainderX;
	private double layoutDragRemainderZ;
	private boolean layoutDirty;
	private Dialog activeDialog = Dialog.NONE;

	private String selectedProjectId;
	private String selectedPoolId;
	private String selectedAnchorId = "";
	private String selectedSocketId = "";
	private String selectedSimRoomId = "";
	private String selectedLayoutNodeId = "";
	private String selectedLayoutSocketId = "";
	private String selectedDungeonDraftId = "";
	private String pendingConnectionNodeId = "";
	private String pendingConnectionSocketId = "";
	private String pendingPoolDelete = "";
	private String pendingProjectDelete = "";
	private long simulationSeed;
	private int catalogScroll;
	private EditBox seedBox;

	private EditBox poolIdBox;
	private EditBox roomIdBox;
	private EditBox selectorIdBox;
	private EditBox entryWeightBox;
	private EditBox requiredModBox;
	private EditBox eligibleMinBox;
	private EditBox eligibleMaxBox;
	private EditBox spawnMinBox;
	private EditBox spawnMaxBox;
	private EditBox baseXpBox;
	private EditBox anchorEncounterBox;
	private EditBox anchorMinLevelBox;
	private EditBox anchorMaxLevelBox;
	private EditBox shellBlockBox;
	private EditBox shellThicknessBox;
	private EditBox maxDepthBox;
	private EditBox newDungeonIdBox;
	private SystemButton selectorKindButton;
	private SystemButton projectKindButton;
	private SystemButton eligibleRangeButton;
	private SystemButton spawnRangeButton;
	private SystemButton xpModeButton;
	private SystemButton anchorLevelModeButton;

	private String poolIdDraft = "";
	private String roomIdDraft = "";
	private String selectorIdDraft = "minecraft:zombie";
	private String entryWeightDraft = "1";
	private String requiredModDraft = "";
	private String eligibleMinDraft = "1";
	private String eligibleMaxDraft = "1000";
	private String spawnMinDraft = "1";
	private String spawnMaxDraft = "1";
	private String baseXpDraft = "0";
	private String anchorEncounterDraft = "default";
	private String anchorMinLevelDraft = "1";
	private String anchorMaxLevelDraft = "1";
	private String shellBlockDraft = "minecraft:bedrock";
	private String shellThicknessDraft = "1";
	private String maxDepthDraft = "8";
	private String newDungeonIdDraft = "";
	private String editingSelectorId = "";
	private SelectorKind editingSelectorKind = SelectorKind.ENTITY;
	private SelectorKind selectorKindDraft = SelectorKind.ENTITY;
	private ProjectKind projectKindDraft = ProjectKind.MODULE;
	private boolean eligibleRangePresent;
	private boolean spawnRangePresent = true;
	private boolean baseXpPresent;
	private boolean anchorLevelOverrideDraft;
	private final Set<String> rankDraft = new LinkedHashSet<>();
	private String dialogError = "";
	private int entitySuggestionCursor;

	private final int[] leftScroll = new int[Tab.values().length];
	private final int[] centerScroll = new int[Tab.values().length];
	private final int[] inspectorScroll = new int[Tab.values().length];
	private double canvasZoom = 1.0D;
	private double canvasPanX;
	private double canvasPanY;

	private List<Component> hoverTooltip;
	private String localFeedback = "";
	private Severity localFeedbackSeverity = Severity.INFO;

	public DungeonBuilderStudioScreen(DungeonBuilderStudioModel model,
			DungeonBuilderStudioController controller) {
		super(Component.literal("DUNGEON BUILDER STUDIO"));
		this.panelW = WIDE_WIDTH;
		this.panelH = WIDE_HEIGHT;
		this.model = model == null ? DungeonBuilderStudioModel.loadingState() : model;
		this.controller = controller == null ? DungeonBuilderStudioController.noop() : controller;
		this.selectedProjectId = preferredProjectId(this.model, this.model.selectedProjectId());
		this.selectedPoolId = preferredPoolId(this.model, this.model.selectedPoolId());
		this.selectedDungeonDraftId = preferredDraftId(this.model, this.model.dungeonId());
		this.simulationSeed = this.model.simulation().seed();
	}

	/** Replaces the immutable server view without resetting tab, scroll, zoom, or selection state. */
	public void updateModel(DungeonBuilderStudioModel next) {
		captureDialogValues();
		String previousDungeonId = this.model.dungeonId();
		DungeonBuilderStudioModel replacement = next == null ? DungeonBuilderStudioModel.empty() : next;
		if (layoutDirty) {
			LayoutDraft localLayout = this.model.layout();
			String localDungeonId = this.model.dungeonId();
			if (replacement.layout().equals(localLayout) && replacement.dungeonId().equals(localDungeonId)) {
				layoutDirty = false;
			} else {
				replacement = new DungeonBuilderStudioModel(replacement.revision(), replacement.selectedProjectId(),
						replacement.selectedPoolId(), localDungeonId, replacement.loading(), replacement.projects(),
						replacement.pools(), localLayout, replacement.simulation(), replacement.validation(),
						replacement.notice(), replacement.dungeonDrafts());
			}
		}
		this.model = replacement;
		this.selectedProjectId = preferredProjectId(replacement,
				replacement.project(selectedProjectId).isPresent() ? selectedProjectId : replacement.selectedProjectId());
		this.selectedPoolId = preferredPoolId(replacement,
				replacement.pool(selectedPoolId).isPresent() ? selectedPoolId : replacement.selectedPoolId());
		if (!previousDungeonId.equals(replacement.dungeonId())) {
			selectedDungeonDraftId = preferredDraftId(replacement, replacement.dungeonId());
			selectedLayoutNodeId = "";
			selectedLayoutSocketId = "";
			clearPendingConnection();
			catalogScroll = 0;
		} else if (replacement.draft(selectedDungeonDraftId).isEmpty()) {
			selectedDungeonDraftId = preferredDraftId(replacement, replacement.dungeonId());
		}
		if (replacement.layout().nodes().stream().noneMatch(node -> node.id().equals(selectedLayoutNodeId))) {
			selectedLayoutNodeId = "";
			selectedLayoutSocketId = "";
		}
		if (replacement.simulation().status() != DungeonBuilderStudioModel.SimulationStatus.IDLE)
			this.simulationSeed = replacement.simulation().seed();
		if (this.minecraft != null && this.minecraft.screen == this)
			rebuildWidgets();
	}

	@Override
	protected boolean allowsNonSystemAccess() {
		return true;
	}

	@Override
	protected boolean shouldPlaySystemSounds() {
		return true;
	}

	@Override
	protected void init() {
		if (this.minecraft == null || this.minecraft.level == null || !DungeonBuilderMode.isActive(this.minecraft.level)) {
			if (this.minecraft != null)
				this.minecraft.setScreen(null);
			return;
		}
		this.compact = this.width < 520;
		this.panelW = compact ? Math.max(MIN_WIDTH, this.width - 8) : WIDE_WIDTH;
		this.panelH = Math.min(WIDE_HEIGHT, Math.max(MIN_HEIGHT, this.height - 8));
		super.init();
		rebuildWidgets();
		if (!requestSent) {
			requestSent = true;
			controller.submit(new RequestSnapshot());
		}
	}

	@Override
	public void removed() {
		super.removed();
		if (!closeReported) {
			closeReported = true;
			controller.screenClosed();
		}
	}

	@Override
	protected void rebuildWidgets() {
		if (this.minecraft == null)
			return;
		captureDialogValues();
		String seedValue = seedBox == null ? Long.toString(simulationSeed) : seedBox.getValue();
		clearWidgets();
		seedBox = null;
		clearDialogWidgetReferences();

		if (activeDialog != Dialog.NONE) {
			buildDialogWidgets();
			return;
		}

		addRenderableWidget(new SystemButton(panelX + panelW - 15, panelY + 3, 12, 12,
				Component.literal("X"), button -> beginClose()));

		int tabX = panelX + 6;
		int tabY = panelY + 21;
		int columns = compact ? 3 : Tab.values().length;
		int rows = compact ? 2 : 1;
		int usable = panelW - 12;
		int tabW = usable / columns;
		for (int index = 0; index < Tab.values().length; index++) {
			Tab tab = Tab.values()[index];
			int col = index % columns;
			int row = index / columns;
			int width = col == columns - 1 ? usable - col * tabW : tabW;
			addRenderableWidget(new StudioTabButton(tabX + col * tabW, tabY + row * TAB_HEIGHT,
					width, TAB_HEIGHT, Component.literal(tab.name()), () -> activeTab == tab,
					() -> setTab(tab)));
		}

		WorkspaceLayout layout = workspaceLayout();
		if (activeTab == Tab.LAYOUT && canUseDungeonCatalog()) {
			int contextLeftWidth = Math.max(100, layout.context().w() * 62 / 100);
			addButton(layout.context().x() + contextLeftWidth - 70, layout.context().y() + 4,
					66, 19, "Dungeons", this::openDungeonCatalog);
		}
		if (compact) {
			int half = layout.mainToggle().w() / 2;
			addRenderableWidget(new StudioTabButton(layout.mainToggle().x(), layout.mainToggle().y(), half,
					16, Component.literal("CANVAS"), () -> compactPane == CompactPane.CANVAS,
					() -> setCompactPane(CompactPane.CANVAS)));
			addRenderableWidget(new StudioTabButton(layout.mainToggle().x() + half, layout.mainToggle().y(),
					layout.mainToggle().w() - half, 16, Component.literal("INSPECT"),
					() -> compactPane == CompactPane.INSPECTOR,
					() -> setCompactPane(CompactPane.INSPECTOR)));
		}
		buildFooterWidgets(layout.footer(), seedValue);
	}

	private void buildDialogWidgets() {
		Rect dialog = dialogRect();
		switch (activeDialog) {
			case NEW_ROOM -> buildNewRoomDialog(dialog);
			case NEW_POOL -> buildNewPoolDialog(dialog);
			case POOL_ENTRY -> buildPoolEntryDialog(dialog);
			case ANCHOR_SETUP -> buildAnchorSetupDialog(dialog);
			case PRESET_SETUP -> buildPresetSetupDialog(dialog);
			case DUNGEON_CATALOG -> buildDungeonCatalogDialog(dialog);
			case NEW_DUNGEON -> buildNewDungeonDialog(dialog);
			case DELETE_DUNGEON -> buildDeleteDungeonDialog(dialog);
			case LAYOUT_SETUP -> buildLayoutSetupDialog(dialog);
			default -> {
			}
		}
	}

	private void buildNewRoomDialog(Rect dialog) {
		roomIdBox = addDialogField(dialog.x() + 82, dialog.y() + 32, dialog.w() - 94, roomIdDraft, 81,
				DungeonBuilderStudioScreen::validRoomProjectDraft, "namespace:start_room");
		projectKindButton = new SystemButton(dialog.x() + 82, dialog.y() + 57, 92, 18,
				Component.literal("TYPE: " + projectKindDraft.name()), button -> toggleProjectKind());
		addRenderableWidget(projectKindButton);
		int buttonY = dialog.bottom() - 26;
		addButton(dialog.right() - 150, buttonY, 66, 18, "Create", this::confirmCreateRoom);
		addButton(dialog.right() - 78, buttonY, 66, 18, "Cancel", this::closeDialog);
	}

	private void buildNewPoolDialog(Rect dialog) {
		poolIdBox = addDialogField(dialog.x() + 82, dialog.y() + 34, dialog.w() - 94, poolIdDraft, 192,
				DungeonBuilderStudioScreen::validDungeonResourceDraft, "namespace:pool_name");
		int buttonY = dialog.bottom() - 26;
		addButton(dialog.right() - 150, buttonY, 66, 18, "Create", this::confirmCreatePool);
		addButton(dialog.right() - 78, buttonY, 66, 18, "Cancel", this::closeDialog);
	}

	private void buildPoolEntryDialog(Rect dialog) {
		int top = dialog.y() + 30;
		selectorKindButton = new SystemButton(dialog.x() + 10, top, 70, 18,
				Component.literal(selectorKindLabel()), button -> toggleSelectorKind());
		addRenderableWidget(selectorKindButton);
		selectorIdBox = addDialogField(dialog.x() + 85, top, dialog.w() - 153, selectorIdDraft, 192,
				DungeonBuilderStudioScreen::validDungeonResourceDraft, "namespace:entity_or_tag");
		addButton(dialog.right() - 63, top, 53, 18, "Suggest", this::suggestEntityId);

		entryWeightBox = addDialogField(dialog.x() + 66, top + 27, 54, entryWeightDraft, 7,
				DungeonBuilderStudioScreen::validUnsignedIntegerDraft, "1");
		requiredModBox = addDialogField(dialog.x() + 205, top + 27, Math.max(70, dialog.w() - 215),
				requiredModDraft, 64, DungeonBuilderStudioScreen::validModIdDraft, "leave_blank");

		eligibleRangeButton = new SystemButton(dialog.x() + 10, top + 54, 92, 18,
				Component.literal(eligibleRangeLabel()), button -> toggleEligibleRange());
		addRenderableWidget(eligibleRangeButton);
		eligibleMinBox = addDialogField(dialog.x() + 112, top + 54, 48, eligibleMinDraft, 4,
				DungeonBuilderStudioScreen::validUnsignedIntegerDraft, "min");
		eligibleMaxBox = addDialogField(dialog.x() + 170, top + 54, 48, eligibleMaxDraft, 4,
				DungeonBuilderStudioScreen::validUnsignedIntegerDraft, "max");

		spawnRangeButton = new SystemButton(dialog.x() + 10, top + 81, 92, 18,
				Component.literal(spawnRangeLabel()), button -> toggleSpawnRange());
		addRenderableWidget(spawnRangeButton);
		spawnMinBox = addDialogField(dialog.x() + 112, top + 81, 48, spawnMinDraft, 4,
				DungeonBuilderStudioScreen::validUnsignedIntegerDraft, "min");
		spawnMaxBox = addDialogField(dialog.x() + 170, top + 81, 48, spawnMaxDraft, 4,
				DungeonBuilderStudioScreen::validUnsignedIntegerDraft, "max");

		xpModeButton = new SystemButton(dialog.x() + 10, top + 108, 92, 18,
				Component.literal(xpModeLabel()), button -> toggleXpMode());
		addRenderableWidget(xpModeButton);
		baseXpBox = addDialogField(dialog.x() + 112, top + 108, 72, baseXpDraft, 8,
				DungeonBuilderStudioScreen::validUnsignedIntegerDraft, "base XP");

		int buttonY = dialog.bottom() - 26;
		addButton(dialog.right() - 150, buttonY, 66, 18, editingSelectorId.isBlank() ? "Add" : "Update",
				this::confirmPoolEntry);
		addButton(dialog.right() - 78, buttonY, 66, 18, "Cancel", this::closeDialog);
	}

	private void buildAnchorSetupDialog(Rect dialog) {
		int top = dialog.y() + 30;
		anchorEncounterBox = addDialogField(dialog.x() + 98, top, dialog.w() - 108,
				anchorEncounterDraft, 64, DungeonBuilderStudioScreen::validEncounterDraft, "room_mobs");
		if (!selectedAnchorIsTrigger()) {
			anchorLevelModeButton = new SystemButton(dialog.x() + 10, top + 29, 126, 18,
					Component.literal(anchorLevelModeLabel()), button -> toggleAnchorLevelOverride());
			addRenderableWidget(anchorLevelModeButton);
			anchorMinLevelBox = addDialogField(dialog.x() + 146, top + 29, 48, anchorMinLevelDraft, 4,
					DungeonBuilderStudioScreen::validUnsignedIntegerDraft, "min");
			anchorMaxLevelBox = addDialogField(dialog.x() + 204, top + 29, 48, anchorMaxLevelDraft, 4,
					DungeonBuilderStudioScreen::validUnsignedIntegerDraft, "max");
		}
		int buttonY = dialog.bottom() - 26;
		addButton(dialog.right() - 150, buttonY, 66, 18, "Apply", this::confirmAnchorSetup);
		addButton(dialog.right() - 78, buttonY, 66, 18, "Cancel", this::closeDialog);
	}

	private void buildLayoutSetupDialog(Rect dialog) {
		int top = dialog.y() + 30;
		int rankX = dialog.x() + 72;
		addRenderableWidget(new StudioTabButton(rankX, top + 27, 36, 18, Component.literal("ALL"),
				rankDraft::isEmpty, rankDraft::clear));
		rankX += 40;
		for (String rank : DUNGEON_RANKS) {
			String value = rank;
			addRenderableWidget(new StudioTabButton(rankX, top + 27, 27, 18, Component.literal(rank),
					() -> rankDraft.contains(value), () -> toggleRank(value)));
			rankX += 30;
		}

		shellBlockBox = addDialogField(dialog.x() + 92, top + 54, dialog.w() - 170, shellBlockDraft, 128,
				DungeonBuilderStudioScreen::validResourceDraft, "minecraft:bedrock");
		addButton(dialog.right() - 72, top + 54, 62, 18, "Bedrock", () -> shellBlockBox.setValue("minecraft:bedrock"));
		shellThicknessBox = addDialogField(dialog.x() + 92, top + 81, 48, shellThicknessDraft, 1,
				DungeonBuilderStudioScreen::validUnsignedIntegerDraft, "1");
		maxDepthBox = addDialogField(dialog.x() + 92, top + 108, 48, maxDepthDraft, 2,
				DungeonBuilderStudioScreen::validUnsignedIntegerDraft, "8");

		int buttonY = dialog.bottom() - 26;
		addButton(dialog.right() - 150, buttonY, 66, 18, "Apply", this::confirmLayoutSetup);
		addButton(dialog.right() - 78, buttonY, 66, 18, "Cancel", this::closeDialog);
	}

	private void buildPresetSetupDialog(Rect dialog) {
		int top = dialog.y() + 30;
		int rankX = dialog.x() + 72;
		addRenderableWidget(new StudioTabButton(rankX, top, 36, 18, Component.literal("ALL"),
				rankDraft::isEmpty, rankDraft::clear));
		rankX += 40;
		for (String rank : DUNGEON_RANKS) {
			String value = rank;
			addRenderableWidget(new StudioTabButton(rankX, top, 27, 18, Component.literal(rank),
					() -> rankDraft.contains(value), () -> toggleRank(value)));
			rankX += 30;
		}
		shellBlockBox = addDialogField(dialog.x() + 92, top + 27, dialog.w() - 170, shellBlockDraft, 128,
				DungeonBuilderStudioScreen::validResourceDraft, "minecraft:bedrock");
		addButton(dialog.right() - 72, top + 27, 62, 18, "Bedrock", () -> shellBlockBox.setValue("minecraft:bedrock"));
		shellThicknessBox = addDialogField(dialog.x() + 92, top + 54, 48, shellThicknessDraft, 1,
				DungeonBuilderStudioScreen::validUnsignedIntegerDraft, "1");
		int buttonY = dialog.bottom() - 26;
		addButton(dialog.right() - 150, buttonY, 66, 18, "Apply", this::confirmPresetSetup);
		addButton(dialog.right() - 78, buttonY, 66, 18, "Cancel", this::closeDialog);
	}

	private void buildDungeonCatalogDialog(Rect dialog) {
		int y = dialog.bottom() - 26;
		int x = dialog.x() + 10;
		x = addButton(x, y, 54, 18, "Open", this::openSelectedDungeon) + 4;
		x = addButton(x, y, 48, 18, "New", this::openNewDungeonDialog) + 4;
		x = addButton(x, y, 56, 18, "Delete", this::openDeleteDungeonDialog) + 4;
		addButton(x, y, 48, 18, "Close", this::closeDialog);
	}

	private void buildNewDungeonDialog(Rect dialog) {
		newDungeonIdBox = addDialogField(dialog.x() + 92, dialog.y() + 34, dialog.w() - 104,
				newDungeonIdDraft, 192, DungeonBuilderStudioScreen::validDungeonResourceDraft, "namespace:dungeon_name");
		int y = dialog.bottom() - 26;
		addButton(dialog.right() - 150, y, 66, 18, "Create", this::confirmNewDungeon);
		addButton(dialog.right() - 78, y, 66, 18, "Cancel", this::openDungeonCatalog);
	}

	private void buildDeleteDungeonDialog(Rect dialog) {
		int y = dialog.bottom() - 26;
		addButton(dialog.right() - 150, y, 66, 18, "Delete", this::confirmDeleteDungeon);
		addButton(dialog.right() - 78, y, 66, 18, "Cancel", this::openDungeonCatalog);
	}

	private EditBox addDialogField(int x, int y, int width, String value, int maxLength,
			java.util.function.Predicate<String> filter, String hint) {
		EditBox field = new EditBox(this.font, x, y, Math.max(38, width), 18, Component.literal(hint));
		field.setMaxLength(maxLength);
		field.setFilter(filter);
		field.setValue(value == null ? "" : value);
		field.setHint(Component.literal(hint));
		addRenderableWidget(field);
		return field;
	}

	private void captureDialogValues() {
		if (roomIdBox != null)
			roomIdDraft = roomIdBox.getValue();
		if (poolIdBox != null)
			poolIdDraft = poolIdBox.getValue();
		if (selectorIdBox != null)
			selectorIdDraft = selectorIdBox.getValue();
		if (entryWeightBox != null)
			entryWeightDraft = entryWeightBox.getValue();
		if (requiredModBox != null)
			requiredModDraft = requiredModBox.getValue();
		if (eligibleMinBox != null)
			eligibleMinDraft = eligibleMinBox.getValue();
		if (eligibleMaxBox != null)
			eligibleMaxDraft = eligibleMaxBox.getValue();
		if (spawnMinBox != null)
			spawnMinDraft = spawnMinBox.getValue();
		if (spawnMaxBox != null)
			spawnMaxDraft = spawnMaxBox.getValue();
		if (baseXpBox != null)
			baseXpDraft = baseXpBox.getValue();
		if (anchorEncounterBox != null)
			anchorEncounterDraft = anchorEncounterBox.getValue();
		if (anchorMinLevelBox != null)
			anchorMinLevelDraft = anchorMinLevelBox.getValue();
		if (anchorMaxLevelBox != null)
			anchorMaxLevelDraft = anchorMaxLevelBox.getValue();
		if (shellBlockBox != null)
			shellBlockDraft = shellBlockBox.getValue();
		if (shellThicknessBox != null)
			shellThicknessDraft = shellThicknessBox.getValue();
		if (maxDepthBox != null)
			maxDepthDraft = maxDepthBox.getValue();
		if (newDungeonIdBox != null)
			newDungeonIdDraft = newDungeonIdBox.getValue();
	}

	private void clearDialogWidgetReferences() {
		roomIdBox = poolIdBox = selectorIdBox = entryWeightBox = requiredModBox = null;
		eligibleMinBox = eligibleMaxBox = spawnMinBox = spawnMaxBox = baseXpBox = null;
		anchorEncounterBox = anchorMinLevelBox = anchorMaxLevelBox = null;
		shellBlockBox = shellThicknessBox = maxDepthBox = newDungeonIdBox = null;
		selectorKindButton = projectKindButton = eligibleRangeButton = spawnRangeButton = xpModeButton = anchorLevelModeButton = null;
	}

	private void buildFooterWidgets(Rect footer, String seedValue) {
		if (compact) {
			buildCompactFooterWidgets(footer, seedValue);
			return;
		}
		int x = footer.x() + 2;
		int y = footer.y() + 2;
		int h = Math.max(16, footer.h() - 4);
		boolean fixedLayout = activeTab == Tab.LAYOUT && model.layout().mode() == LayoutMode.FIXED;
		x = addButton(x, y, 52, h, fixedLayout ? "Mode" : "Refresh",
				fixedLayout ? this::cycleLayoutMode : () -> controller.submit(new RequestSnapshot())) + 4;

		switch (activeTab) {
			case ROOMS -> {
				Optional<Project> project = selectedProject();
				x = addButton(x, y, 58, h, "New Room", this::createRoomProject) + 4;
				String capture = project.map(value -> value.snapshotCaptured() ? "Update Snapshot" : "Capture Room")
						.orElse("Capture Room");
				x = addButton(x, y, 88, h, capture, this::captureSelectedProject) + 4;
				if (project.map(value -> value.kind() == ProjectKind.PRESET).orElse(false)) {
					x = addButton(x, y, 72, h, "Preset Setup", this::openPresetSetupDialog) + 4;
				} else {
					x = addButton(x, y, 42, h, "Role", this::cycleRoomRole) + 4;
					x = addButton(x, y, 48, h, "Default-", () -> adjustRoomWeight(-1)) + 4;
					x = addButton(x, y, 48, h, "Default+", () -> adjustRoomWeight(1)) + 4;
					if (selectedSocket().isPresent())
						x = addButton(x, y, 58, h, "Required", this::toggleSelectedSocketRequired) + 4;
				}
				String delete = pendingProjectDelete.equals(selectedProjectId) ? "Confirm" : "Delete";
				addButton(x, y, pendingProjectDelete.equals(selectedProjectId) ? 54 : 44, h, delete, this::deleteRoomProject);
			}
			case ANCHORS -> {
				if (!selectedAnchorIsTrigger()) {
					x = addButton(x, y, 58, h, "Next Role", this::cycleSelectedAnchorRole) + 4;
					x = addButton(x, y, 58, h, "Next Pool", this::cycleSelectedAnchorPool) + 4;
					x = addButton(x, y, 44, h, "Level -", () -> shiftSelectedAnchorLevel(-1)) + 4;
					x = addButton(x, y, 44, h, "Level +", () -> shiftSelectedAnchorLevel(1)) + 4;
					x = addButton(x, y, 56, h, "Delayed", this::toggleSelectedAnchorDelay) + 4;
				}
				addButton(x, y, 62, h, "Configure", this::openAnchorSetupDialog);
			}
			case POOLS -> {
				x = addButton(x, y, 60, h, "New Pool", this::createPoolDraft) + 4;
				x = addButton(x, y, 68, h, "Add Entity", this::beginPoolEntryDraft) + 4;
				x = addButton(x, y, 70, h, "Save Draft", this::savePoolDraft) + 4;
				String delete = pendingPoolDelete.equals(selectedPoolId) ? "Confirm Delete" : "Delete";
				addButton(x, y, pendingPoolDelete.equals(selectedPoolId) ? 82 : 52, h, delete, this::deletePool);
			}
			case LAYOUT -> {
				if (model.layout().mode() == LayoutMode.FIXED) {
					x = addButton(x, y, 42, h, "Add", this::addSelectedRoomNode) + 4;
					x = addButton(x, y, 24, h, "-X", () -> moveSelectedLayoutNode(-1, 0)) + 4;
					x = addButton(x, y, 24, h, "+X", () -> moveSelectedLayoutNode(1, 0)) + 4;
					x = addButton(x, y, 24, h, "-Z", () -> moveSelectedLayoutNode(0, -1)) + 4;
					x = addButton(x, y, 24, h, "+Z", () -> moveSelectedLayoutNode(0, 1)) + 4;
					x = addButton(x, y, 38, h, "Rotate", this::rotateSelectedLayoutNode) + 4;
					x = addButton(x, y, 42, h, "Socket", this::cycleLayoutSocket) + 4;
					x = addButton(x, y, 42, h, layoutLinkLabel(), this::connectSelectedLayoutSocket) + 4;
					x = addButton(x, y, 36, h, "Delete", this::deleteSelectedLayoutNode) + 4;
					x = addButton(x, y, 42, h, "Setup", this::openLayoutSetupDialog) + 4;
				addButton(x, y, 42, h, "Apply", this::submitLayout);
			} else {
				x = addButton(x, y, 42, h, "Mode", this::cycleLayoutMode) + 4;
				x = addButton(x, y, 58, h, "Topology", this::cycleTopology) + 4;
				x = addButton(x, y, 54, h, layoutIncludeLabel(), this::toggleSelectedProjectIncluded) + 4;
				x = addButton(x, y, 32, h, "Min-", () -> adjustRoomRange(-1, 0)) + 4;
				x = addButton(x, y, 32, h, "Min+", () -> adjustRoomRange(1, 0)) + 4;
				x = addButton(x, y, 32, h, "Max-", () -> adjustRoomRange(0, -1)) + 4;
				x = addButton(x, y, 32, h, "Max+", () -> adjustRoomRange(0, 1)) + 4;
				x = addButton(x, y, 44, h, "Setup", this::openLayoutSetupDialog) + 4;
				addButton(x, y, 44, h, "Apply", this::submitLayout);
			}
			}
			case SIMULATE -> {
				seedBox = new EditBox(this.font, x, y, 126, h, Component.literal("Simulation seed"));
				seedBox.setMaxLength(20);
				seedBox.setFilter(DungeonBuilderStudioScreen::validSeedText);
				seedBox.setValue(seedValue == null || seedValue.isBlank() ? Long.toString(simulationSeed) : seedValue);
				seedBox.setHint(Component.literal("Seed"));
				addRenderableWidget(seedBox);
				x += 130;
				x = addButton(x, y, 68, h, "New Seed", this::newSimulationSeed) + 4;
				addButton(x, y, 76, h, "Run Preview", this::runSimulation);
			}
			case EXPORT -> {
				x = addButton(x, y, 70, h, "Validate", this::validateDungeon) + 4;
				addButton(x, y, 78, h, "Export Pack", this::exportDungeon);
			}
		}
	}

	private void buildCompactFooterWidgets(Rect footer, String seedValue) {
		int x = footer.x() + 2;
		int firstY = footer.y() + 2;
		int secondY = footer.y() + 20;
		int h = 16;
		boolean fixedLayout = activeTab == Tab.LAYOUT && model.layout().mode() == LayoutMode.FIXED;
		x = addButton(x, firstY, 52, h, fixedLayout ? "Mode" : "Refresh",
				fixedLayout ? this::cycleLayoutMode : () -> controller.submit(new RequestSnapshot())) + 4;
		switch (activeTab) {
			case ROOMS -> {
				x = addButton(x, firstY, 58, h, "New Room", this::createRoomProject) + 4;
				String capture = selectedProject().map(value -> value.snapshotCaptured() ? "Update Snapshot" : "Capture Room")
						.orElse("Capture Room");
				addButton(x, firstY, 90, h, capture, this::captureSelectedProject);
				x = footer.x() + 2;
				if (selectedProject().map(value -> value.kind() == ProjectKind.PRESET).orElse(false)) {
					x = addButton(x, secondY, 76, h, "Preset Setup", this::openPresetSetupDialog) + 4;
				} else {
					x = addButton(x, secondY, 42, h, "Role", this::cycleRoomRole) + 4;
					x = addButton(x, secondY, 48, h, "Default-", () -> adjustRoomWeight(-1)) + 4;
					x = addButton(x, secondY, 48, h, "Default+", () -> adjustRoomWeight(1)) + 4;
					if (selectedSocket().isPresent())
						x = addButton(x, secondY, 58, h, "Required", this::toggleSelectedSocketRequired) + 4;
				}
				String delete = pendingProjectDelete.equals(selectedProjectId) ? "Confirm Delete" : "Delete";
				addButton(x, secondY, pendingProjectDelete.equals(selectedProjectId) ? 78 : 44, h, delete, this::deleteRoomProject);
			}
			case ANCHORS -> {
				if (!selectedAnchorIsTrigger()) {
					x = addButton(x, firstY, 58, h, "Next Role", this::cycleSelectedAnchorRole) + 4;
					x = addButton(x, firstY, 58, h, "Next Pool", this::cycleSelectedAnchorPool) + 4;
				}
				addButton(x, firstY, 62, h, "Configure", this::openAnchorSetupDialog);
				if (!selectedAnchorIsTrigger()) {
					x = footer.x() + 2;
					x = addButton(x, secondY, 44, h, "Level -", () -> shiftSelectedAnchorLevel(-1)) + 4;
					x = addButton(x, secondY, 44, h, "Level +", () -> shiftSelectedAnchorLevel(1)) + 4;
					addButton(x, secondY, 56, h, "Delayed", this::toggleSelectedAnchorDelay);
				}
			}
			case POOLS -> {
				x = addButton(x, firstY, 60, h, "New Pool", this::createPoolDraft) + 4;
				addButton(x, firstY, 68, h, "Add Entity", this::beginPoolEntryDraft);
				x = footer.x() + 2;
				x = addButton(x, secondY, 70, h, "Save Draft", this::savePoolDraft) + 4;
				String delete = pendingPoolDelete.equals(selectedPoolId) ? "Confirm Delete" : "Delete";
				addButton(x, secondY, pendingPoolDelete.equals(selectedPoolId) ? 82 : 52, h, delete, this::deletePool);
			}
			case LAYOUT -> {
				if (model.layout().mode() == LayoutMode.FIXED) {
					x = addButton(x, firstY, 40, h, "Add", this::addSelectedRoomNode) + 4;
					x = addButton(x, firstY, 46, h, "Rotate", this::rotateSelectedLayoutNode) + 4;
					x = addButton(x, firstY, 46, h, "Socket", this::cycleLayoutSocket) + 4;
					addButton(x, firstY, 40, h, layoutLinkLabel(), this::connectSelectedLayoutSocket);
					x = footer.x() + 2;
					x = addButton(x, secondY, 26, h, "-X", () -> moveSelectedLayoutNode(-1, 0)) + 4;
					x = addButton(x, secondY, 26, h, "+X", () -> moveSelectedLayoutNode(1, 0)) + 4;
					x = addButton(x, secondY, 26, h, "-Z", () -> moveSelectedLayoutNode(0, -1)) + 4;
					x = addButton(x, secondY, 26, h, "+Z", () -> moveSelectedLayoutNode(0, 1)) + 4;
					x = addButton(x, secondY, 40, h, "Delete", this::deleteSelectedLayoutNode) + 4;
					x = addButton(x, secondY, 42, h, "Setup", this::openLayoutSetupDialog) + 4;
				addButton(x, secondY, 42, h, "Apply", this::submitLayout);
				} else {
					x = addButton(x, firstY, 50, h, "Mode", this::cycleLayoutMode) + 4;
					x = addButton(x, firstY, 60, h, "Topology", this::cycleTopology) + 4;
					x = addButton(x, firstY, 54, h, layoutIncludeLabel(), this::toggleSelectedProjectIncluded) + 4;
					addButton(x, firstY, 44, h, "Apply", this::submitLayout);
					x = footer.x() + 2;
					x = addButton(x, secondY, 34, h, "Min-", () -> adjustRoomRange(-1, 0)) + 4;
					x = addButton(x, secondY, 34, h, "Min+", () -> adjustRoomRange(1, 0)) + 4;
					x = addButton(x, secondY, 34, h, "Max-", () -> adjustRoomRange(0, -1)) + 4;
					x = addButton(x, secondY, 34, h, "Max+", () -> adjustRoomRange(0, 1)) + 4;
					addButton(x, secondY, 48, h, "Setup", this::openLayoutSetupDialog);
				}
			}
			case SIMULATE -> {
				seedBox = new EditBox(this.font, x, firstY, Math.max(84, footer.w() - 136), h,
						Component.literal("Simulation seed"));
				seedBox.setMaxLength(20);
				seedBox.setFilter(DungeonBuilderStudioScreen::validSeedText);
				seedBox.setValue(seedValue == null || seedValue.isBlank() ? Long.toString(simulationSeed) : seedValue);
				seedBox.setHint(Component.literal("Seed"));
				addRenderableWidget(seedBox);
				addButton(seedBox.getX() + seedBox.getWidth() + 4, firstY, 72, h, "Run Preview", this::runSimulation);
				addButton(footer.x() + 2, secondY, 68, h, "New Seed", this::newSimulationSeed);
			}
			case EXPORT -> {
				x = addButton(x, firstY, 70, h, "Validate", this::validateDungeon) + 4;
				addButton(x, firstY, 78, h, "Export Pack", this::exportDungeon);
			}
		}
	}

	private int addButton(int x, int y, int width, int height, String label, Runnable action) {
		addRenderableWidget(new SystemButton(x, y, width, height, Component.literal(label), button -> action.run()));
		return x + width;
	}

	private void setTab(Tab tab) {
		if (activeTab == tab)
			return;
		activeTab = tab;
		layoutNodeDragging = false;
		layoutDragMap = null;
		canvasZoom = 1.0D;
		canvasPanX = canvasPanY = 0.0D;
		localFeedback = "";
		rebuildWidgets();
	}

	private void setCompactPane(CompactPane pane) {
		if (compactPane == pane)
			return;
		compactPane = pane;
		rebuildWidgets();
	}

	@Override
	protected void renderContent(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
		hoverTooltip = null;
		WorkspaceLayout layout = workspaceLayout();
		renderContext(graphics, layout.context(), mouseX, mouseY);
		renderLeftRail(graphics, layout.left(), mouseX, mouseY);

		if (!compact || compactPane == CompactPane.CANVAS)
			renderMain(graphics, layout.main(), mouseX, mouseY);
		if (!compact)
			renderInspector(graphics, layout.inspector(), mouseX, mouseY);
		else if (compactPane == CompactPane.INSPECTOR)
			renderInspector(graphics, layout.main(), mouseX, mouseY);

		drawPanel(graphics, layout.footer(), RAISED, BORDER);
		if (activeDialog != Dialog.NONE)
			renderDialogOverlay(graphics, mouseX, mouseY);
	}

	private void renderDialogOverlay(GuiGraphics graphics, int mouseX, int mouseY) {
		graphics.fill(panelX + 1, panelY + 17, panelX + panelW - 1, panelY + panelH - 1, 0xD9000209);
		Rect dialog = dialogRect();
		drawPanel(graphics, dialog, 0xFA08111F, ACCENT);
		String title = switch (activeDialog) {
			case NEW_ROOM -> "CREATE ROOM PROJECT";
			case NEW_POOL -> "CREATE MOB POOL";
			case POOL_ENTRY -> editingSelectorId.isBlank() ? "ADD POOL ENTRY" : "EDIT POOL ENTRY";
			case ANCHOR_SETUP -> "ANCHOR ENCOUNTER SETUP";
			case PRESET_SETUP -> "PRESET RANK & SHELL";
			case DUNGEON_CATALOG -> "SAVED DUNGEONS";
			case NEW_DUNGEON -> "CREATE DUNGEON DRAFT";
			case DELETE_DUNGEON -> "DELETE DUNGEON?";
			case LAYOUT_SETUP -> "DUNGEON SETUP";
			default -> "";
		};
		graphics.drawString(font, title, dialog.x() + 10, dialog.y() + 8, INFO, false);

		switch (activeDialog) {
			case NEW_ROOM -> {
				graphics.drawString(font, "ROOM ID", dialog.x() + 10, dialog.y() + 37, TEXT_SUB, false);
				graphics.drawString(font, "PROJECT", dialog.x() + 10, dialog.y() + 62, TEXT_SUB, false);
				drawClipped(graphics, projectKindDraft == ProjectKind.MODULE
						? "MODULE is one procedural room. PRESET is a complete prebuilt dungeon."
						: "PRESET captures one complete dungeon; use MODULE for procedural rooms.",
						dialog.x() + 10, dialog.y() + 81, dialog.w() - 20, TEXT_SUB, mouseX, mouseY);
			}
			case NEW_POOL -> {
				graphics.drawString(font, "POOL ID", dialog.x() + 10, dialog.y() + 39, TEXT_SUB, false);
				drawClipped(graphics, "Use namespace:name. This ID is referenced by room spawn anchors.",
						dialog.x() + 10, dialog.y() + 59, dialog.w() - 20, TEXT_SUB, mouseX, mouseY);
			}
			case POOL_ENTRY -> renderPoolEntryDialogLabels(graphics, dialog, mouseX, mouseY);
			case ANCHOR_SETUP -> renderAnchorSetupDialogLabels(graphics, dialog, mouseX, mouseY);
			case PRESET_SETUP -> renderPresetSetupDialogLabels(graphics, dialog, mouseX, mouseY);
			case DUNGEON_CATALOG -> renderDungeonCatalogDialog(graphics, dialog, mouseX, mouseY);
			case NEW_DUNGEON -> {
				graphics.drawString(font, "DUNGEON ID", dialog.x() + 10, dialog.y() + 39, TEXT_SUB, false);
				drawClipped(graphics, "Creates and opens a blank saved draft. Existing dungeons are never overwritten.",
						dialog.x() + 10, dialog.y() + 59, dialog.w() - 20, TEXT_SUB, mouseX, mouseY);
			}
			case DELETE_DUNGEON -> renderDeleteDungeonDialog(graphics, dialog, mouseX, mouseY);
			case LAYOUT_SETUP -> renderLayoutSetupDialogLabels(graphics, dialog, mouseX, mouseY);
			default -> {
			}
		}

		if (!dialogError.isBlank())
			drawClipped(graphics, "[ERROR] " + dialogError, dialog.x() + 10, dialog.bottom() - 38,
					dialog.w() - 20, ERROR, mouseX, mouseY);
	}

	private void renderDungeonCatalogDialog(GuiGraphics graphics, Rect dialog, int mouseX, int mouseY) {
		Rect list = dungeonCatalogListRect(dialog);
		drawPanel(graphics, list, CANVAS, BORDER);
		if (model.dungeonDrafts().isEmpty()) {
			renderEmpty(graphics, list.inset(6, 6), "No saved dungeons",
					"Press New to create the first namespaced dungeon draft.");
			return;
		}
		enableScissor(graphics, list);
		int y = list.y() - catalogScroll;
		for (DraftSummary draft : model.dungeonDrafts()) {
			if (y + 27 >= list.y() && y <= list.bottom()) {
				boolean selected = draft.id().equals(selectedDungeonDraftId);
				boolean active = draft.id().equals(model.dungeonId());
				if (selected)
					graphics.fill(list.x() + 1, y, list.right() - 1, y + 26, SELECTED);
				drawClipped(graphics, (active ? "[ACTIVE] " : "") + draft.id(), list.x() + 5, y + 3,
						list.w() - 10, active ? SUCCESS : selected ? TEXT_MAIN : TEXT_SUB, mouseX, mouseY);
				String count = draft.mode() == LayoutMode.FIXED
						? draft.placementCount() + " placements" : draft.roomCount() + " rooms";
				drawClipped(graphics, draft.mode().name() + " | " + draft.topology().name() + " | " + count,
						list.x() + 5, y + 14, list.w() - 10, TEXT_SUB, mouseX, mouseY);
			}
			y += 28;
		}
		graphics.disableScissor();
	}

	private void renderDeleteDungeonDialog(GuiGraphics graphics, Rect dialog, int mouseX, int mouseY) {
		String id = selectedDungeonDraftId.isBlank() ? "No dungeon selected" : selectedDungeonDraftId;
		drawClipped(graphics, id, dialog.x() + 10, dialog.y() + 34, dialog.w() - 20,
				id.equals(model.dungeonId()) ? WARNING : TEXT_MAIN, mouseX, mouseY);
		drawWrapped(graphics, "This permanently removes the saved dungeon draft. Room projects and mob pools are not deleted.",
				dialog.x() + 10, dialog.y() + 54, dialog.w() - 20, TEXT_SUB, 4);
	}

	private void renderPoolEntryDialogLabels(GuiGraphics graphics, Rect dialog, int mouseX, int mouseY) {
		int top = dialog.y() + 30;
		graphics.drawString(font, "WEIGHT", dialog.x() + 10, top + 32, TEXT_SUB, false);
		graphics.drawString(font, "OPTIONAL MOD", dialog.x() + 127, top + 32, TEXT_SUB, false);
		graphics.drawString(font, "MIN", dialog.x() + 224, top + 59, TEXT_SUB, false);
		graphics.drawString(font, "MAX", dialog.x() + 251, top + 59, TEXT_SUB, false);
		graphics.drawString(font, "MIN", dialog.x() + 224, top + 86, TEXT_SUB, false);
		graphics.drawString(font, "MAX", dialog.x() + 251, top + 86, TEXT_SUB, false);
		graphics.drawString(font, "VALUE", dialog.x() + 190, top + 113, TEXT_SUB, false);
		drawClipped(graphics, selectorResolutionText(), dialog.x() + 10, top + 132,
				dialog.w() - 20, selectorResolutionColor(), mouseX, mouseY);
	}

	private void renderLayoutSetupDialogLabels(GuiGraphics graphics, Rect dialog, int mouseX, int mouseY) {
		int top = dialog.y() + 30;
		graphics.drawString(font, "ACTIVE DUNGEON", dialog.x() + 10, top + 5, TEXT_SUB, false);
		graphics.fill(dialog.x() + 92, top, dialog.right() - 10, top + 18, 0xC0060D18);
		drawOutline(graphics, dialog.x() + 92, top, dialog.w() - 102, 18, BORDER);
		drawClipped(graphics, model.dungeonId(), dialog.x() + 97, top + 5,
				dialog.w() - 112, TEXT_MAIN, mouseX, mouseY);
		graphics.drawString(font, "RANKS", dialog.x() + 10, top + 32, TEXT_SUB, false);
		graphics.drawString(font, "SHELL", dialog.x() + 10, top + 59, TEXT_SUB, false);
		graphics.drawString(font, "THICKNESS", dialog.x() + 10, top + 86, TEXT_SUB, false);
		graphics.drawString(font, "MAX DEPTH", dialog.x() + 10, top + 113, TEXT_SUB, false);
		if (dialogError.isBlank())
			drawClipped(graphics, "ALL means every rank. Max depth limits graph distance from the start room.",
					dialog.x() + 10, top + 135, dialog.w() - 20, TEXT_SUB, mouseX, mouseY);
	}

	private void renderAnchorSetupDialogLabels(GuiGraphics graphics, Rect dialog, int mouseX, int mouseY) {
		int top = dialog.y() + 30;
		graphics.drawString(font, "ENCOUNTER ID", dialog.x() + 10, top + 5, TEXT_SUB, false);
		Anchor anchor = selectedAnchor().orElse(null);
		if (anchor != null && anchor.kind() == AnchorKind.TRIGGER) {
			drawClipped(graphics, "Trigger volume " + (anchor.triggerBounds() == null ? "is not set"
					: anchor.triggerBounds().width() + " x " + anchor.triggerBounds().height() + " x "
							+ anchor.triggerBounds().depth()), dialog.x() + 10, top + 34,
					dialog.w() - 20, anchor.triggerBounds() == null ? WARNING : INFO, mouseX, mouseY);
			drawClipped(graphics, "This gate activates its encounter group; spawn role, pool, level, XP, and delay are configured on spawn anchors.",
					dialog.x() + 10, top + 52, dialog.w() - 20, TEXT_SUB, mouseX, mouseY);
			return;
		}
		graphics.drawString(font, "MIN", dialog.x() + 158, top + 20, TEXT_SUB, false);
		graphics.drawString(font, "MAX", dialog.x() + 216, top + 20, TEXT_SUB, false);
		String assignment = anchor == null ? "No anchor selected"
				: "POOL " + (anchor.poolId().isBlank() ? "NOT SET" : anchor.poolId()) + "  |  ROLE " + anchor.spawnRole();
		drawClipped(graphics, assignment, dialog.x() + 10, top + 58, dialog.w() - 20,
				anchor == null || anchor.poolId().isBlank() ? WARNING : TEXT_SUB, mouseX, mouseY);
		drawClipped(graphics, "Encounter IDs group spawn points. Inherit uses the pool or dungeon rank level.",
				dialog.x() + 10, top + 72, dialog.w() - 20, TEXT_SUB, mouseX, mouseY);
	}

	private void renderPresetSetupDialogLabels(GuiGraphics graphics, Rect dialog, int mouseX, int mouseY) {
		int top = dialog.y() + 30;
		graphics.drawString(font, "RANKS", dialog.x() + 10, top + 5, TEXT_SUB, false);
		graphics.drawString(font, "SHELL", dialog.x() + 10, top + 32, TEXT_SUB, false);
		graphics.drawString(font, "THICKNESS", dialog.x() + 10, top + 59, TEXT_SUB, false);
		if (dialogError.isBlank())
			drawClipped(graphics, "These settings route this complete preset and wrap its captured bounds.",
					dialog.x() + 10, top + 78, dialog.w() - 20, TEXT_SUB, mouseX, mouseY);
	}

	private void renderContext(GuiGraphics graphics, Rect rect, int mouseX, int mouseY) {
		drawPanel(graphics, rect, 0xD20A1120, BORDER);
		Project project = selectedProject().orElse(null);
		String room = project == null ? "ROOM  No room selected" : "ROOM  " + project.id();
		int leftWidth = Math.max(100, rect.w() * 62 / 100);
		int reserved = activeTab == Tab.LAYOUT && canUseDungeonCatalog() ? 74 : 0;
		drawClipped(graphics, room, rect.x() + 6, rect.y() + 4,
				Math.max(24, leftWidth - 8 - reserved), TEXT_MAIN, mouseX, mouseY);
		String context = "DUNGEON  " + model.dungeonId() + "  |  REV " + model.revision();
		drawClipped(graphics, context, rect.x() + 6, rect.y() + 14,
				Math.max(24, leftWidth - 8 - reserved), TEXT_SUB, mouseX, mouseY);

		Severity severity = feedbackSeverity();
		String status = feedbackText();
		int statusX = rect.x() + leftWidth;
		drawClipped(graphics, statusLabel(severity) + "  " + status, statusX, rect.y() + 9,
				rect.x() + rect.w() - statusX - 5, severityColor(severity), mouseX, mouseY);
	}

	private void renderLeftRail(GuiGraphics graphics, Rect rect, int mouseX, int mouseY) {
		drawPanel(graphics, rect, RAISED, BORDER);
		switch (activeTab) {
			case POOLS -> renderPoolLibrary(graphics, rect, mouseX, mouseY);
			case SIMULATE -> renderSimulationRoomList(graphics, rect, mouseX, mouseY);
			case EXPORT -> renderIssueList(graphics, rect, mouseX, mouseY);
			default -> renderProjectLibrary(graphics, rect, mouseX, mouseY);
		}
	}

	private void renderProjectLibrary(GuiGraphics graphics, Rect rect, int mouseX, int mouseY) {
		drawSectionTitle(graphics, rect, "ROOM LIBRARY", model.projects().size());
		if (model.loading()) {
			renderEmpty(graphics, rect.inset(7, 24), "Loading rooms...", "The server is preparing your workspace.");
			return;
		}
		if (model.projects().isEmpty()) {
			renderEmpty(graphics, rect.inset(7, 24), "No rooms saved", "Build one room in-world, mark its bounds, then capture it here.");
			return;
		}

		Rect clip = new Rect(rect.x() + 2, rect.y() + 18, rect.w() - 4, rect.h() - 20);
		enableScissor(graphics, clip);
		int y = clip.y() - leftScroll[activeTab.ordinal()];
		for (Project project : model.projects()) {
			if (y + ROW_HEIGHT >= clip.y() && y <= clip.bottom()) {
				boolean selected = project.id().equals(selectedProjectId);
				if (selected)
					graphics.fill(clip.x(), y, clip.right(), y + ROW_HEIGHT - 1, SELECTED);
				boolean layoutRoom = activeTab == Tab.LAYOUT;
				boolean included = model.layout().enabledProjectIds().contains(project.id());
				long nodeCount = model.layout().nodes().stream().filter(node -> node.projectId().equals(project.id())).count();
				boolean weightControls = layoutRoom && model.layout().mode() == LayoutMode.PROCEDURAL
						&& included && project.kind() == ProjectKind.MODULE;
				String snapshot = layoutRoom
						? model.layout().mode() == LayoutMode.FIXED ? nodeCount + " NODE" + (nodeCount == 1 ? "" : "S")
								: included ? "DW " + dungeonRoomWeight(project) : "EXCLUDED"
						: project.snapshotOutdated() ? "TODO UPDATE" : project.snapshotCaptured() ? "CAPTURED" : "TODO CAPTURE";
				int stateColor = layoutRoom ? (included || nodeCount > 0 ? SUCCESS : DISABLED)
						: project.snapshotOutdated() || !project.snapshotCaptured() ? WARNING : SUCCESS;
				drawClipped(graphics, project.name(), clip.x() + 5, y + 4, clip.w() - 10,
						selected ? TEXT_MAIN : TEXT_SUB, mouseX, mouseY);
				int detailWidth = weightControls ? clip.w() - 49 : clip.w() - 10;
				drawClipped(graphics, project.role().name() + "  |  " + snapshot, clip.x() + 5, y + 15,
						detailWidth, project.errors() > 0 ? ERROR : stateColor, mouseX, mouseY);
				if (weightControls)
					graphics.drawString(font, "[-] [+]", clip.right() - 39, y + 15, INFO, false);
			}
			y += ROW_HEIGHT;
		}
		graphics.disableScissor();
	}

	private void renderPoolLibrary(GuiGraphics graphics, Rect rect, int mouseX, int mouseY) {
		drawSectionTitle(graphics, rect, "MOB POOLS", model.pools().size());
		if (model.pools().isEmpty()) {
			renderEmpty(graphics, rect.inset(7, 24), "No mob pools", "Press New Pool, then add entities or entity tags.");
			return;
		}
		Rect clip = new Rect(rect.x() + 2, rect.y() + 18, rect.w() - 4, rect.h() - 20);
		enableScissor(graphics, clip);
		int y = clip.y() - leftScroll[activeTab.ordinal()];
		for (MobPool pool : model.pools()) {
			if (y + ROW_HEIGHT >= clip.y() && y <= clip.bottom()) {
				boolean selected = pool.id().equals(selectedPoolId);
				if (selected)
					graphics.fill(clip.x(), y, clip.right(), y + ROW_HEIGHT - 1, SELECTED);
				drawClipped(graphics, pool.id(), clip.x() + 5, y + 4, clip.w() - 10,
						selected ? TEXT_MAIN : TEXT_SUB, mouseX, mouseY);
				String state = pool.draft() ? "TODO DRAFT" : "SAVED";
				graphics.drawString(font, state + "  |  " + pool.entries().size() + " entries", clip.x() + 5,
						y + 15, pool.draft() ? WARNING : SUCCESS, false);
			}
			y += ROW_HEIGHT;
		}
		graphics.disableScissor();
	}

	private void renderSimulationRoomList(GuiGraphics graphics, Rect rect, int mouseX, int mouseY) {
		Simulation simulation = model.simulation();
		drawSectionTitle(graphics, rect, "GENERATED ROOMS", simulation.rooms().size());
		if (simulation.rooms().isEmpty()) {
			renderEmpty(graphics, rect.inset(7, 24), "No preview yet", "Set a seed and press Run Preview.");
			return;
		}
		Rect clip = new Rect(rect.x() + 2, rect.y() + 18, rect.w() - 4, rect.h() - 20);
		enableScissor(graphics, clip);
		int y = clip.y() - leftScroll[activeTab.ordinal()];
		for (SimRoom room : simulation.rooms()) {
			if (y + 23 >= clip.y() && y <= clip.bottom()) {
				boolean selected = room.id().equals(selectedSimRoomId);
				if (selected)
					graphics.fill(clip.x(), y, clip.right(), y + 22, SELECTED);
				drawClipped(graphics, room.projectId(), clip.x() + 5, y + 3, clip.w() - 10,
						selected ? TEXT_MAIN : TEXT_SUB, mouseX, mouseY);
				graphics.drawString(font, room.role().name() + "  R" + room.rotation(), clip.x() + 5, y + 13,
						roleColor(room.role()), false);
			}
			y += 23;
		}
		graphics.disableScissor();
	}

	private void renderIssueList(GuiGraphics graphics, Rect rect, int mouseX, int mouseY) {
		List<ValidationIssue> issues = model.validation().issues();
		drawSectionTitle(graphics, rect, "VALIDATION", issues.size());
		if (!model.validation().hasRun()) {
			renderEmpty(graphics, rect.inset(7, 24), "Not validated", "Press Validate before exporting.");
			return;
		}
		if (issues.isEmpty()) {
			renderEmpty(graphics, rect.inset(7, 24), "PASS", "No blocking issues were reported.");
			return;
		}
		Rect clip = new Rect(rect.x() + 2, rect.y() + 18, rect.w() - 4, rect.h() - 20);
		enableScissor(graphics, clip);
		int y = clip.y() - leftScroll[activeTab.ordinal()];
		for (ValidationIssue issue : issues) {
			if (y + 34 >= clip.y() && y <= clip.bottom()) {
				graphics.drawString(font, statusLabel(issue.severity()), clip.x() + 5, y + 3,
						severityColor(issue.severity()), false);
				drawClipped(graphics, issue.projectId(), clip.x() + 5, y + 13, clip.w() - 10,
						TEXT_SUB, mouseX, mouseY);
				drawClipped(graphics, issue.message(), clip.x() + 5, y + 23, clip.w() - 10,
						TEXT_MAIN, mouseX, mouseY);
			}
			y += 34;
		}
		graphics.disableScissor();
	}

	private void renderMain(GuiGraphics graphics, Rect rect, int mouseX, int mouseY) {
		drawPanel(graphics, rect, CANVAS, BORDER);
		switch (activeTab) {
			case ROOMS, ANCHORS -> renderRoomCanvas(graphics, rect, mouseX, mouseY);
			case POOLS -> renderPoolEntries(graphics, rect, mouseX, mouseY);
			case LAYOUT -> renderLayoutCanvas(graphics, rect, mouseX, mouseY);
			case SIMULATE -> renderSimulationCanvas(graphics, rect, mouseX, mouseY);
			case EXPORT -> renderExportOverview(graphics, rect, mouseX, mouseY);
		}
	}

	private void renderRoomCanvas(GuiGraphics graphics, Rect rect, int mouseX, int mouseY) {
		Project project = selectedProject().orElse(null);
		if (project == null) {
			renderEmpty(graphics, rect.inset(8, 8), "Select a room", "Choose a room from the library to inspect its saved top view.");
			return;
		}
		if (project.bounds() == null) {
			renderEmpty(graphics, rect.inset(8, 8), "Bounds missing", "Use the Surveyor Wand to mark structure bounds, then refresh.");
			return;
		}

		drawBlueprintGrid(graphics, rect);
		Rect viewport = rect.inset(7, 18);
		enableScissor(graphics, viewport);
		RoomMap map = RoomMap.forProject(project, viewport, canvasZoom, canvasPanX, canvasPanY);
		if (!project.footprint().isEmpty()) {
			for (FootprintCell cell : project.footprint()) {
				int x0 = map.localX(cell.x());
				int z0 = map.localZ(cell.z());
				int x1 = Math.max(x0 + 1, map.localX(cell.x() + 1));
				int z1 = Math.max(z0 + 1, map.localZ(cell.z() + 1));
				int color = (cell.argb() & 0x00FFFFFF) | 0xB0000000;
				graphics.fill(x0, z0, x1, z1, color);
			}
		} else {
			graphics.fill(map.minScreenX(), map.minScreenZ(), map.maxScreenX(), map.maxScreenZ(), 0x7A183548);
		}
		drawOutline(graphics, map.minScreenX(), map.minScreenZ(),
				Math.max(1, map.maxScreenX() - map.minScreenX()),
				Math.max(1, map.maxScreenZ() - map.minScreenZ()), ACCENT);

		for (Socket socket : project.sockets())
			drawSocket(graphics, map, socket, socket.id().equals(selectedSocketId));
		for (Anchor anchor : project.anchors())
			drawAnchor(graphics, map, anchor, anchor.id().equals(selectedAnchorId));
		graphics.disableScissor();

		String heading = "TOP VIEW  |  " + Math.round(canvasZoom * 100.0D) + "%";
		graphics.drawString(font, heading, rect.x() + 6, rect.y() + 5, INFO, false);
		String help = activeTab == Tab.ANCHORS ? "Click point: inspect  |  Wheel: zoom  |  Middle drag: pan  |  F: fit"
				: "Click socket: inspect  |  Wheel: zoom  |  Middle drag: pan  |  F: fit";
		drawClipped(graphics, help, rect.x() + 6, rect.bottom() - 11, rect.w() - 12, TEXT_SUB, mouseX, mouseY);
	}

	private void renderPoolEntries(GuiGraphics graphics, Rect rect, int mouseX, int mouseY) {
		MobPool pool = selectedPool().orElse(null);
		if (pool == null) {
			renderEmpty(graphics, rect.inset(8, 8), "Select a mob pool", "Choose a pool on the left or create a new draft.");
			return;
		}
		graphics.drawString(font, pool.draft() ? "TODO  UNSAVED DRAFT" : "SAVED POOL", rect.x() + 6, rect.y() + 5,
				pool.draft() ? WARNING : SUCCESS, false);
		if (pool.entries().isEmpty()) {
			renderEmpty(graphics, rect.inset(8, 22), "Pool is empty", "Press Add Entity to choose a loaded entity or entity tag.");
			return;
		}

		Rect clip = new Rect(rect.x() + 3, rect.y() + 18, rect.w() - 6, rect.h() - 21);
		enableScissor(graphics, clip);
		int y = clip.y() - centerScroll[activeTab.ordinal()];
		int total = Math.max(1, pool.totalWeight());
		for (PoolEntry entry : pool.entries()) {
			if (y + 36 >= clip.y() && y <= clip.bottom()) {
				graphics.fill(clip.x(), y, clip.right(), y + 35, 0xA00C1423);
				drawOutline(graphics, clip.x(), y, clip.w(), 35, 0x682273A8);
				drawClipped(graphics, entry.selectorLabel(), clip.x() + 5, y + 4, clip.w() - 52,
						TEXT_MAIN, mouseX, mouseY);
				int percent = Math.max(1, Math.round(entry.weight() * 100.0F / total));
				String xp = entry.baseXp().present() ? Integer.toString(entry.baseXp().value()) : "AUTO";
				String spawn = entry.spawnLevel().present()
						? entry.spawnLevel().min() + "-" + entry.spawnLevel().max() : "DUNGEON";
				graphics.drawString(font, "WEIGHT " + entry.weight() + " (" + percent + "%)  |  XP " + xp,
						clip.x() + 5, y + 15, TEXT_SUB, false);
				drawClipped(graphics, "SPAWN LEVEL " + spawn
						+ (entry.requiredMod().isBlank() ? "" : "  |  MOD " + entry.requiredMod()),
						clip.x() + 5, y + 25, clip.w() - 55, TEXT_SUB, mouseX, mouseY);
				graphics.drawString(font, "EDIT  [-] [+] [X]", clip.right() - 92, y + 15, INFO, false);
			}
			y += 38;
		}
		graphics.disableScissor();
	}

	private void renderLayoutCanvas(GuiGraphics graphics, Rect rect, int mouseX, int mouseY) {
		LayoutDraft draft = model.layout();
		drawBlueprintGrid(graphics, rect);
		String title = draft.mode().name() + "  |  " + draft.topology().name() + "  |  "
				+ draft.minRooms() + "-" + draft.maxRooms() + " ROOMS";
		drawClipped(graphics, title, rect.x() + 6, rect.y() + 5, rect.w() - 12, INFO, mouseX, mouseY);

		if (draft.nodes().isEmpty()) {
			String line1 = draft.mode() == LayoutMode.PROCEDURAL ? "Procedural rules are ready" : "No fixed rooms placed";
			String line2 = draft.mode() == LayoutMode.PROCEDURAL
					? "Include room assets on the left; weights and sockets decide turns without overlap."
					: "Select a room in the library and press Add to place its first exact node.";
			renderEmpty(graphics, rect.inset(8, 22), line1, line2);
			return;
		}
		renderNodeGraph(graphics, rect.inset(7, 18), draft.nodes(), draft.connections(), mouseX, mouseY);
		if (draft.mode() == LayoutMode.FIXED)
			drawClipped(graphics, "Drag free node | click socket point + Link | arrows move 1 block",
					rect.x() + 6, rect.bottom() - 11, rect.w() - 12, TEXT_SUB, mouseX, mouseY);
	}

	private void renderSimulationCanvas(GuiGraphics graphics, Rect rect, int mouseX, int mouseY) {
		Simulation simulation = model.simulation();
		drawBlueprintGrid(graphics, rect);
		String title = switch (simulation.status()) {
			case IDLE -> "TODO  PREVIEW NOT RUN";
			case RUNNING -> "RUNNING  PLANNER WORKING";
			case SUCCESS -> "PASS  SEED " + simulation.seed();
			case FAILED -> "ERROR  SEED " + simulation.seed();
		};
		Severity severity = switch (simulation.status()) {
			case SUCCESS -> Severity.PASS;
			case FAILED -> Severity.ERROR;
			case RUNNING -> Severity.INFO;
			default -> Severity.TODO;
		};
		drawClipped(graphics, title, rect.x() + 6, rect.y() + 5, rect.w() - 12,
				severityColor(severity), mouseX, mouseY);
		if (simulation.rooms().isEmpty()) {
			renderEmpty(graphics, rect.inset(8, 22), "No generated layout", "Set a seed below and press Run Preview.");
			return;
		}
		renderSimulationGraph(graphics, rect.inset(7, 18), simulation, mouseX, mouseY);
	}

	private void renderExportOverview(GuiGraphics graphics, Rect rect, int mouseX, int mouseY) {
		Severity severity = model.validation().severity();
		String heading = statusLabel(severity) + "  " + model.validation().errors() + " ERRORS  |  "
				+ model.validation().warnings() + " WARNINGS";
		graphics.drawCenteredString(font, heading, rect.x() + rect.w() / 2, rect.y() + 18,
				severityColor(severity));
		int y = rect.y() + 42;
		y = exportLine(graphics, rect, y, model.projects().isEmpty() ? Severity.ERROR : Severity.PASS,
				"ROOM ASSETS", model.projects().isEmpty() ? "Capture at least one room." : model.projects().size() + " rooms available.");
		y = exportLine(graphics, rect, y, model.pools().stream().anyMatch(MobPool::draft) ? Severity.WARNING : Severity.PASS,
				"MOB POOLS", model.pools().stream().anyMatch(MobPool::draft) ? "Save remaining drafts." : model.pools().size() + " pools saved.");
		y = exportLine(graphics, rect, y, model.layout().enabledProjectIds().isEmpty() ? Severity.TODO : Severity.PASS,
				"LAYOUT", model.layout().enabledProjectIds().isEmpty() ? "Choose rooms for generation." : "Planner rules configured.");
		y = exportLine(graphics, rect, y,
				model.simulation().status() == DungeonBuilderStudioModel.SimulationStatus.SUCCESS ? Severity.PASS : Severity.TODO,
				"SIMULATION", model.simulation().status() == DungeonBuilderStudioModel.SimulationStatus.SUCCESS
						? "Latest seed generated successfully." : "Run at least one preview seed.");
		exportLine(graphics, rect, y, severity, "DATAPACK",
				severity == Severity.PASS ? "Ready for an explicit export." : "Validate and resolve blocking errors first.");
	}

	private int exportLine(GuiGraphics graphics, Rect rect, int y, Severity severity, String label, String detail) {
		graphics.fill(rect.x() + 12, y, rect.right() - 12, y + 28, 0xA00C1423);
		graphics.drawString(font, statusLabel(severity) + "  " + label, rect.x() + 18, y + 5,
				severityColor(severity), false);
		drawClipped(graphics, detail, rect.x() + 18, y + 16, rect.w() - 36, TEXT_SUB, -1, -1);
		return y + 33;
	}

	private void renderInspector(GuiGraphics graphics, Rect rect, int mouseX, int mouseY) {
		drawPanel(graphics, rect, RAISED, BORDER);
		Rect clip = new Rect(rect.x() + 2, rect.y() + 18, rect.w() - 4, rect.h() - 20);
		graphics.drawString(font, inspectorTitle(), rect.x() + 6, rect.y() + 5, INFO, false);
		enableScissor(graphics, clip);
		int y = clip.y() + 2 - inspectorScroll[activeTab.ordinal()];
		switch (activeTab) {
			case ROOMS -> renderRoomInspector(graphics, clip, y, mouseX, mouseY);
			case ANCHORS -> renderAnchorInspector(graphics, clip, y, mouseX, mouseY);
			case POOLS -> renderPoolInspector(graphics, clip, y, mouseX, mouseY);
			case LAYOUT -> renderLayoutInspector(graphics, clip, y, mouseX, mouseY);
			case SIMULATE -> renderSimulationInspector(graphics, clip, y, mouseX, mouseY);
			case EXPORT -> renderExportInspector(graphics, clip, y, mouseX, mouseY);
		}
		graphics.disableScissor();
	}

	private void renderRoomInspector(GuiGraphics graphics, Rect clip, int y, int mouseX, int mouseY) {
		Project project = selectedProject().orElse(null);
		if (project == null) {
			renderEmpty(graphics, clip, "No room selected", "Select a room from the library.");
			return;
		}
		y = inspectorValue(graphics, clip, y, "ROOM ID", project.id(), mouseX, mouseY);
		y = inspectorValue(graphics, clip, y, "ROLE", project.role().name(), mouseX, mouseY);
		y = inspectorValue(graphics, clip, y, "SNAPSHOT", project.snapshotLabel() + "  REV " + project.snapshotRevision(), mouseX, mouseY);
		String size = project.bounds() == null ? "NOT SET"
				: project.bounds().width() + " x " + project.bounds().height() + " x " + project.bounds().depth();
		y = inspectorValue(graphics, clip, y, "BOUNDS", size, mouseX, mouseY);
		y = inspectorValue(graphics, clip, y, "DEFAULT WEIGHT", Integer.toString(project.weight()), mouseX, mouseY);
		y = inspectorValue(graphics, clip, y, "RANKS", rankLabel(project.ranks()), mouseX, mouseY);
		if (project.kind() == ProjectKind.PRESET) {
			y = inspectorValue(graphics, clip, y, "SHELL", project.shellBlock(), mouseX, mouseY);
			y = inspectorValue(graphics, clip, y, "THICKNESS", Integer.toString(project.shellThickness()), mouseX, mouseY);
		}
		y = inspectorValue(graphics, clip, y, "SOCKETS", project.sockets().size() + " total", mouseX, mouseY);
		Socket socket = selectedSocket().orElse(null);
		if (socket != null) {
			y += 4;
			graphics.drawString(font, "SELECTED SOCKET", clip.x() + 4, y, INFO, false);
			y += 12;
			y = inspectorValue(graphics, clip, y, "ID", socket.id(), mouseX, mouseY);
			y = inspectorValue(graphics, clip, y, "FACING", socket.facing().name(), mouseX, mouseY);
			y = inspectorValue(graphics, clip, y, "TYPE", socket.type().name(), mouseX, mouseY);
			inspectorValue(graphics, clip, y, "POLICY", socket.required() ? "MUST CONNECT" : "OPTIONAL", mouseX, mouseY);
		}
	}

	private void renderAnchorInspector(GuiGraphics graphics, Rect clip, int y, int mouseX, int mouseY) {
		Anchor anchor = selectedAnchor().orElse(null);
		if (anchor == null) {
			renderEmpty(graphics, clip, "Select a point", "Click a labeled marker in the top view. Generic points show TODO until assigned.");
			return;
		}
		y = inspectorValue(graphics, clip, y, "ANCHOR ID", anchor.id(), mouseX, mouseY);
		y = inspectorValue(graphics, clip, y, "KIND", anchor.kind().name(), mouseX, mouseY);
		y = inspectorValue(graphics, clip, y, "POSITION", pointText(anchor.position()), mouseX, mouseY);
		if (anchor.triggerBounds() != null)
			y = inspectorValue(graphics, clip, y, "TRIGGER VOLUME", anchor.triggerBounds().width() + " x "
					+ anchor.triggerBounds().height() + " x " + anchor.triggerBounds().depth(), mouseX, mouseY);
		y = inspectorValue(graphics, clip, y, "ENCOUNTER", anchor.encounterId(), mouseX, mouseY);
		if (anchor.kind() == AnchorKind.TRIGGER) {
			drawWrapped(graphics, "Trigger anchors only activate this encounter group. Configure its mobs and levels on spawn anchors.",
					clip.x() + 4, y + 3, clip.w() - 8, TEXT_SUB, 5);
			return;
		}
		y = inspectorValue(graphics, clip, y, "SPAWN ROLE", anchor.spawnRole().name(), mouseX, mouseY);
		y = inspectorValue(graphics, clip, y, "MOB POOL", anchor.poolId().isBlank() ? "TODO  NOT ASSIGNED" : anchor.poolId(), mouseX, mouseY);
		y = inspectorValue(graphics, clip, y, "LEVEL", anchor.levelOverride()
				? anchor.minLevel() + "-" + anchor.maxLevel() + " (OVERRIDE)"
				: "POOL / DUNGEON DEFAULT", mouseX, mouseY);
		inspectorValue(graphics, clip, y, "ACTIVATION", anchor.delayed() ? "DELAYED BY TRIGGER" : "ON GENERATION", mouseX, mouseY);
	}

	private void renderPoolInspector(GuiGraphics graphics, Rect clip, int y, int mouseX, int mouseY) {
		MobPool pool = selectedPool().orElse(null);
		if (pool == null) {
			renderEmpty(graphics, clip, "No pool selected", "Create a reusable pool or select one from the library.");
			return;
		}
		y = inspectorValue(graphics, clip, y, "POOL ID", pool.id(), mouseX, mouseY);
		y = inspectorValue(graphics, clip, y, "STATUS", pool.draft() ? "TODO  UNSAVED" : "SAVED", mouseX, mouseY);
		y = inspectorValue(graphics, clip, y, "ENTRIES", Integer.toString(pool.entries().size()), mouseX, mouseY);
		y = inspectorValue(graphics, clip, y, "TOTAL WEIGHT", Integer.toString(pool.totalWeight()), mouseX, mouseY);
		y += 5;
		graphics.drawString(font, "WEIGHT MEANING", clip.x() + 4, y, INFO, false);
		y += 12;
		drawWrapped(graphics, "Chance is entry weight divided by total eligible weight. Entity tags resolve when the datapack reloads.",
				clip.x() + 4, y, clip.w() - 8, TEXT_SUB, 7);
	}

	private void renderLayoutInspector(GuiGraphics graphics, Rect clip, int y, int mouseX, int mouseY) {
		LayoutDraft draft = model.layout();
		y = inspectorValue(graphics, clip, y, "STATUS", layoutDirty ? "UNSAVED - PRESS APPLY" : "SERVER SNAPSHOT", mouseX, mouseY);
		y = inspectorValue(graphics, clip, y, "DUNGEON ID", model.dungeonId(), mouseX, mouseY);
		Project selectedRoom = selectedProject().orElse(null);
		if (selectedRoom != null && selectedRoom.kind() == ProjectKind.PRESET)
			y = inspectorValue(graphics, clip, y, "SAVED DUNGEONS", "PRESET EXPORTS DIRECTLY", mouseX, mouseY);
		y = inspectorValue(graphics, clip, y, "MODE", draft.mode().name(), mouseX, mouseY);
		y = inspectorValue(graphics, clip, y, "TOPOLOGY", draft.topology().name(), mouseX, mouseY);
		y = inspectorValue(graphics, clip, y, "ROOM RANGE", draft.minRooms() + "-" + draft.maxRooms(), mouseX, mouseY);
		y = inspectorValue(graphics, clip, y, "MAX DEPTH", Integer.toString(draft.maxDepth()), mouseX, mouseY);
		y = inspectorValue(graphics, clip, y, "RANKS", rankLabel(draft.ranks()), mouseX, mouseY);
		y = inspectorValue(graphics, clip, y, "SHELL", draft.shellBlock(), mouseX, mouseY);
		y = inspectorValue(graphics, clip, y, "THICKNESS", Integer.toString(draft.shellThickness()), mouseX, mouseY);
		y = inspectorValue(graphics, clip, y, "ROOM ASSETS", draft.enabledProjectIds().size() + " enabled", mouseX, mouseY);
		if (selectedRoom != null) {
			String dungeonWeight = selectedRoom.kind() == ProjectKind.PRESET ? "PRESET EXPORTS DIRECTLY"
					: draft.mode() == LayoutMode.FIXED ? "NOT USED IN FIXED MODE"
					: !draft.enabledProjectIds().contains(selectedRoom.id()) ? "EXCLUDED"
					: Integer.toString(dungeonRoomWeight(selectedRoom));
			y = inspectorValue(graphics, clip, y, "DUNGEON WEIGHT", dungeonWeight, mouseX, mouseY);
			y = inspectorValue(graphics, clip, y, "PROJECT DEFAULT", Integer.toString(selectedRoom.weight()), mouseX, mouseY);
		}
		y = inspectorValue(graphics, clip, y, "FIXED NODES", Integer.toString(draft.nodes().size()), mouseX, mouseY);
		LayoutNode node = selectedLayoutNode().orElse(null);
		if (node != null) {
			y += 4;
			graphics.drawString(font, "SELECTED FIXED NODE", clip.x() + 4, y, INFO, false);
			y += 12;
			y = inspectorValue(graphics, clip, y, "NODE", node.id(), mouseX, mouseY);
			y = inspectorValue(graphics, clip, y, "ROOM", node.projectId(), mouseX, mouseY);
			y = inspectorValue(graphics, clip, y, "POSITION", node.x() + ", " + node.y() + ", " + node.z(), mouseX, mouseY);
			y = inspectorValue(graphics, clip, y, "ROTATION", node.rotation() + " DEGREES", mouseX, mouseY);
			Socket socket = layoutSocket(node, selectedLayoutSocketId).orElse(null);
			y = inspectorValue(graphics, clip, y, "SOCKET", socket == null ? "PRESS SOCKET TO SELECT" : socket.id(), mouseX, mouseY);
			if (socket != null)
				y = inspectorValue(graphics, clip, y, "FACING", rotatedFacing(socket.facing(), node.rotation()).name(), mouseX, mouseY);
			inspectorValue(graphics, clip, y, "LINK", pendingConnectionNodeId.isBlank()
					? "NO FIRST ENDPOINT" : pendingConnectionNodeId + " / " + pendingConnectionSocketId, mouseX, mouseY);
		}
	}

	private void renderSimulationInspector(GuiGraphics graphics, Rect clip, int y, int mouseX, int mouseY) {
		Simulation simulation = model.simulation();
		y = inspectorValue(graphics, clip, y, "STATUS", simulation.status().name(), mouseX, mouseY);
		y = inspectorValue(graphics, clip, y, "SEED", Long.toString(simulation.seed()), mouseX, mouseY);
		y = inspectorValue(graphics, clip, y, "ROOMS", Integer.toString(simulation.rooms().size()), mouseX, mouseY);
		y = inspectorValue(graphics, clip, y, "CONNECTIONS", Integer.toString(simulation.connections().size()), mouseX, mouseY);
		y = inspectorValue(graphics, clip, y, "ATTEMPTS", Integer.toString(simulation.attempts()), mouseX, mouseY);
		SimRoom room = selectedSimulationRoom().orElse(null);
		if (room != null) {
			y += 5;
			graphics.drawString(font, "SELECTED GENERATED ROOM", clip.x() + 4, y, INFO, false);
			y += 12;
			y = inspectorValue(graphics, clip, y, "PROJECT", room.projectId(), mouseX, mouseY);
			y = inspectorValue(graphics, clip, y, "ROLE", room.role().name(), mouseX, mouseY);
			inspectorValue(graphics, clip, y, "ROTATION", room.rotation() + " DEGREES", mouseX, mouseY);
		}
	}

	private void renderExportInspector(GuiGraphics graphics, Rect clip, int y, int mouseX, int mouseY) {
		y = inspectorValue(graphics, clip, y, "DUNGEON ID", model.dungeonId(), mouseX, mouseY);
		y = inspectorValue(graphics, clip, y, "REVISION", Long.toString(model.revision()), mouseX, mouseY);
		y = inspectorValue(graphics, clip, y, "VALIDATION", statusLabel(model.validation().severity()), mouseX, mouseY);
		y = inspectorValue(graphics, clip, y, "ERRORS", Integer.toString(model.validation().errors()), mouseX, mouseY);
		y = inspectorValue(graphics, clip, y, "WARNINGS", Integer.toString(model.validation().warnings()), mouseX, mouseY);
		y += 5;
		graphics.drawString(font, "EXPORT IS EXPLICIT", clip.x() + 4, y, WARNING, false);
		y += 12;
		drawWrapped(graphics, "Metadata autosaves, but captured blocks only change when you press Capture Room or Update Snapshot.",
				clip.x() + 4, y, clip.w() - 8, TEXT_SUB, 7);
	}

	private int inspectorValue(GuiGraphics graphics, Rect clip, int y, String label, String value,
			int mouseX, int mouseY) {
		graphics.drawString(font, label, clip.x() + 4, y, TEXT_SUB, false);
		drawClipped(graphics, value, clip.x() + 4, y + 10, clip.w() - 8, TEXT_MAIN, mouseX, mouseY);
		graphics.fill(clip.x() + 4, y + 21, clip.right() - 4, y + 22, 0x382273A8);
		return y + 27;
	}

	@Override
	protected List<Component> getHoverTooltip(int mouseX, int mouseY) {
		return hoverTooltip;
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (super.mouseClicked(mouseX, mouseY, button))
			return true;
		if (activeDialog == Dialog.DUNGEON_CATALOG) {
			if (button == 0)
				handleDungeonCatalogClick((int) Math.round(logicalMouseX(mouseX)),
						(int) Math.round(logicalMouseY(mouseY)));
			return true;
		}
		if (activeDialog != Dialog.NONE)
			return true;
		int logicalX = (int) Math.round(logicalMouseX(mouseX));
		int logicalY = (int) Math.round(logicalMouseY(mouseY));
		WorkspaceLayout layout = workspaceLayout();

		if (button == 2 && layout.main().contains(logicalX, logicalY)
				&& (!compact || compactPane == CompactPane.CANVAS)) {
			canvasPanning = true;
			return true;
		}
		if (layout.left().contains(logicalX, logicalY)) {
			handleLeftClick(layout.left(), logicalX, logicalY, button);
			return true;
		}
		if ((!compact || compactPane == CompactPane.CANVAS) && layout.main().contains(logicalX, logicalY)) {
			if (button == 0 && !compact && activeTab == Tab.LAYOUT
					&& model.layout().mode() == LayoutMode.FIXED) {
				if (selectLayoutSocketAt(layout.main(), logicalX, logicalY))
					return true;
				LayoutNode selected = selectLayoutNodeAt(layout.main(), logicalX, logicalY);
				if (selected != null && !selected.locked() && !isLayoutNodeConnected(selected.id())) {
					layoutNodeDragging = true;
					layoutDragBlocked = false;
					layoutDragMap = layoutGraphMap(layout.main().inset(7, 18), model.layout().nodes());
					layoutDragRemainderX = 0.0D;
					layoutDragRemainderZ = 0.0D;
				}
				return true;
			}
			handleMainClick(layout.main(), logicalX, logicalY);
			return true;
		}
		return false;
	}

	private void handleLeftClick(Rect rect, int mouseX, int mouseY, int button) {
		int localY = mouseY - (rect.y() + 18) + leftScroll[activeTab.ordinal()];
		if (localY < 0)
			return;
		switch (activeTab) {
			case POOLS -> {
				int index = localY / ROW_HEIGHT;
				if (index >= 0 && index < model.pools().size()) {
					selectedPoolId = model.pools().get(index).id();
					pendingPoolDelete = "";
					centerScroll[activeTab.ordinal()] = 0;
					rebuildWidgets();
				}
			}
			case SIMULATE -> {
				int index = localY / 23;
				if (index >= 0 && index < model.simulation().rooms().size())
					selectedSimRoomId = model.simulation().rooms().get(index).id();
			}
			case EXPORT -> {
				int index = localY / 34;
				if (index >= 0 && index < model.validation().issues().size()) {
					String projectId = model.validation().issues().get(index).projectId();
					if (!projectId.isBlank() && model.project(projectId).isPresent()) {
						selectedProjectId = projectId;
						controller.submit(new SelectProject(projectId));
					}
				}
			}
			default -> {
				int index = localY / ROW_HEIGHT;
				if (index >= 0 && index < model.projects().size()) {
					Project project = model.projects().get(index);
					selectedProjectId = project.id();
					selectedAnchorId = "";
					selectedSocketId = "";
					if (activeTab == Tab.ROOMS || activeTab == Tab.ANCHORS) {
						canvasZoom = 1.0D;
						canvasPanX = canvasPanY = 0.0D;
					}
					controller.submit(new SelectProject(project.id()));
					boolean weightControl = button == 0 && activeTab == Tab.LAYOUT
							&& model.layout().mode() == LayoutMode.PROCEDURAL
							&& project.kind() == ProjectKind.MODULE
							&& model.layout().enabledProjectIds().contains(project.id())
							&& mouseX >= rect.right() - 42;
					if (weightControl) {
						adjustDungeonRoomWeight(project, mouseX < rect.right() - 22 ? -1 : 1);
						rebuildWidgets();
						return;
					}
					rebuildWidgets();
				}
			}
		}
	}

	private void handleDungeonCatalogClick(int mouseX, int mouseY) {
		Rect list = dungeonCatalogListRect(dialogRect());
		if (!list.contains(mouseX, mouseY))
			return;
		int localY = mouseY - list.y() + catalogScroll;
		int index = localY / 28;
		if (index < 0 || index >= model.dungeonDrafts().size())
			return;
		selectedDungeonDraftId = model.dungeonDrafts().get(index).id();
		dialogError = "";
	}

	private void handleMainClick(Rect rect, int mouseX, int mouseY) {
		switch (activeTab) {
			case ROOMS, ANCHORS -> selectRoomElementAt(rect, mouseX, mouseY);
			case POOLS -> handlePoolEntryClick(rect, mouseX, mouseY);
			case LAYOUT -> {
				if (!selectLayoutSocketAt(rect, mouseX, mouseY))
					selectLayoutNodeAt(rect, mouseX, mouseY);
			}
			case SIMULATE -> selectSimulationRoomAt(rect, mouseX, mouseY);
			default -> {
			}
		}
	}

	private void selectRoomElementAt(Rect rect, int mouseX, int mouseY) {
		Project project = selectedProject().orElse(null);
		if (project == null || project.bounds() == null)
			return;
		RoomMap map = RoomMap.forProject(project, rect.inset(7, 18), canvasZoom, canvasPanX, canvasPanY);
		if (activeTab == Tab.ANCHORS) {
			Anchor closest = null;
			double distance = Double.MAX_VALUE;
			for (Anchor anchor : project.anchors()) {
				double dx = mouseX - map.worldX(anchor.position().x());
				double dz = mouseY - map.worldZ(anchor.position().z());
				double candidate = dx * dx + dz * dz;
				if (candidate <= 64.0D && candidate < distance) {
					closest = anchor;
					distance = candidate;
				}
			}
			if (closest != null) {
				selectedAnchorId = closest.id();
				if (compact)
					compactPane = CompactPane.INSPECTOR;
				rebuildWidgets();
			}
		} else {
			Socket closest = null;
			double distance = Double.MAX_VALUE;
			for (Socket socket : project.sockets()) {
				double dx = mouseX - map.worldX(socket.position().x());
				double dz = mouseY - map.worldZ(socket.position().z());
				double candidate = dx * dx + dz * dz;
				if (candidate <= 64.0D && candidate < distance) {
					closest = socket;
					distance = candidate;
				}
			}
			if (closest != null) {
				selectedSocketId = closest.id();
				if (compact)
					compactPane = CompactPane.INSPECTOR;
				rebuildWidgets();
			}
		}
	}

	private void handlePoolEntryClick(Rect rect, int mouseX, int mouseY) {
		MobPool pool = selectedPool().orElse(null);
		if (pool == null)
			return;
		int localY = mouseY - (rect.y() + 18) + centerScroll[activeTab.ordinal()];
		int index = localY / 38;
		if (index < 0 || index >= pool.entries().size())
			return;
		PoolEntry entry = pool.entries().get(index);
		int right = rect.right() - 8;
		if (mouseX >= right - 54 && mouseX < right - 36) {
			controller.submit(new UpsertPoolEntry(pool.id(), copyEntryWeight(entry, entry.weight() - 1)));
		} else if (mouseX >= right - 36 && mouseX < right - 18) {
			controller.submit(new UpsertPoolEntry(pool.id(), copyEntryWeight(entry, entry.weight() + 1)));
		} else if (mouseX >= right - 18) {
			List<PoolEntry> replacement = new ArrayList<>(pool.entries());
			replacement.remove(index);
			controller.submit(new SavePoolDraft(pool.id(), replacement));
		} else {
			openPoolEntryDialog(entry);
		}
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
		if (activeDialog == Dialog.DUNGEON_CATALOG) {
			int logicalX = (int) Math.round(logicalMouseX(mouseX));
			int logicalY = (int) Math.round(logicalMouseY(mouseY));
			Rect dialog = dialogRect();
			if (dungeonCatalogListRect(dialog).contains(logicalX, logicalY))
				catalogScroll = clamp(catalogScroll - (int) Math.round(delta * 28.0D),
						0, maxCatalogScroll(dialog));
			return true;
		}
		if (activeDialog != Dialog.NONE)
			return true;
		int logicalX = (int) Math.round(logicalMouseX(mouseX));
		int logicalY = (int) Math.round(logicalMouseY(mouseY));
		WorkspaceLayout layout = workspaceLayout();
		if (layout.left().contains(logicalX, logicalY)) {
			leftScroll[activeTab.ordinal()] = clamp(leftScroll[activeTab.ordinal()] - (int) Math.round(delta * 18.0D),
					0, maxLeftScroll(layout.left()));
			return true;
		}
		Rect inspector = compact ? layout.main() : layout.inspector();
		if ((!compact || compactPane == CompactPane.INSPECTOR) && inspector.contains(logicalX, logicalY)) {
			inspectorScroll[activeTab.ordinal()] = clamp(inspectorScroll[activeTab.ordinal()]
					- (int) Math.round(delta * 14.0D), 0, 360);
			return true;
		}
		if ((!compact || compactPane == CompactPane.CANVAS) && layout.main().contains(logicalX, logicalY)) {
			if (activeTab == Tab.POOLS) {
				centerScroll[activeTab.ordinal()] = clamp(centerScroll[activeTab.ordinal()]
						- (int) Math.round(delta * 18.0D), 0, maxCenterScroll(layout.main()));
			} else {
				canvasZoom = clamp(canvasZoom + delta * 0.12D, 0.5D, 4.0D);
			}
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, delta);
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
		if (activeDialog != Dialog.NONE)
			return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
		if (canvasPanning && button == 2) {
			float responsiveScale = responsiveTransform().scale();
			canvasPanX += dragX / responsiveScale;
			canvasPanY += dragY / responsiveScale;
			return true;
		}
		if (layoutNodeDragging && button == 0 && activeTab == Tab.LAYOUT
				&& model.layout().mode() == LayoutMode.FIXED) {
			LayoutNode node = selectedLayoutNode().orElse(null);
			List<LayoutNode> nodes = model.layout().nodes();
			if (node == null || nodes.isEmpty() || isLayoutNodeConnected(node.id())) {
				layoutNodeDragging = false;
				layoutDragMap = null;
				return true;
			}
			GraphMap map = layoutDragMap == null
					? layoutGraphMap(workspaceLayout().main().inset(7, 18), nodes) : layoutDragMap;
			float responsiveScale = responsiveTransform().scale();
			layoutDragRemainderX += dragX / responsiveScale / map.scale();
			layoutDragRemainderZ += dragY / responsiveScale / map.scale();
			int moveX = (int) layoutDragRemainderX;
			int moveZ = (int) layoutDragRemainderZ;
			if (moveX == 0 && moveZ == 0)
				return true;
			layoutDragRemainderX -= moveX;
			layoutDragRemainderZ -= moveZ;
			LayoutNode moved = translateLayoutNodeWithoutCollision(node, moveX, moveZ);
			if (moved.x() != node.x() + moveX || moved.z() != node.z() + moveZ) {
				layoutDragBlocked = true;
				layoutDragRemainderX = 0.0D;
				layoutDragRemainderZ = 0.0D;
			}
			if (moved.x() != node.x() || moved.z() != node.z())
				replaceLayoutNode(moved);
			return true;
		}
		return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		if (button == 2)
			canvasPanning = false;
		if (button == 0 && layoutNodeDragging) {
			layoutNodeDragging = false;
			layoutDragMap = null;
			LayoutNode node = selectedLayoutNode().orElse(null);
			if (node != null)
				feedback(layoutDragBlocked ? Severity.WARNING : Severity.INFO,
						(layoutDragBlocked ? "Stopped at another room. " : "Moved room. ")
								+ "Position: X " + node.x() + ", Z " + node.z() + ". Press Apply to save.");
			layoutDragBlocked = false;
			return true;
		}
		return super.mouseReleased(mouseX, mouseY, button);
	}

	@Override
	public boolean keyPressed(int key, int scanCode, int modifiers) {
		if (activeDialog != Dialog.NONE) {
			if (key == GLFW.GLFW_KEY_ESCAPE) {
				closeDialog();
				return true;
			}
			if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
				confirmActiveDialog();
				return true;
			}
			return super.keyPressed(key, scanCode, modifiers);
		}
		if (key == GLFW.GLFW_KEY_TAB && hasControlDown()) {
			int step = hasShiftDown() ? -1 : 1;
			setTab(Tab.values()[Math.floorMod(activeTab.ordinal() + step, Tab.values().length)]);
			return true;
		}
		if (key == GLFW.GLFW_KEY_F && (seedBox == null || !seedBox.isFocused())) {
			canvasZoom = 1.0D;
			canvasPanX = canvasPanY = 0.0D;
			return true;
		}
		if (key == GLFW.GLFW_KEY_ENTER && activeTab == Tab.SIMULATE
				&& seedBox != null && seedBox.isFocused()) {
			runSimulation();
			return true;
		}
		return super.keyPressed(key, scanCode, modifiers);
	}

	private void createRoomProject() {
		roomIdDraft = suggestedRoomId();
		projectKindDraft = ProjectKind.MODULE;
		dialogError = "";
		activeDialog = Dialog.NEW_ROOM;
		rebuildWidgets();
	}

	private void cycleRoomRole() {
		Project project = selectedProject().orElse(null);
		if (project == null) {
			feedback(Severity.TODO, "Select a room first.");
			return;
		}
		RoomRole[] roles = RoomRole.values();
		RoomRole next = roles[(project.role().ordinal() + 1) % roles.length];
		controller.submit(new SetRoomRole(project.id(), next));
		feedback(Severity.INFO, "Setting room role to " + next.name().toLowerCase(Locale.ROOT) + "...");
	}

	private void adjustRoomWeight(int amount) {
		Project project = selectedProject().orElse(null);
		if (project == null) {
			feedback(Severity.TODO, "Select a room first.");
			return;
		}
		controller.submit(new SetRoomWeight(project.id(), clamp(project.weight() + amount, 1, 10_000)));
		feedback(Severity.INFO, "Setting the project's DEFAULT WEIGHT. Existing dungeon-specific weights are unchanged...");
	}

	private void deleteRoomProject() {
		Project project = selectedProject().orElse(null);
		if (project == null) {
			feedback(Severity.TODO, "Select a room first.");
			return;
		}
		if (!pendingProjectDelete.equals(project.id())) {
			pendingProjectDelete = project.id();
			feedback(Severity.WARNING, "Press Confirm Delete to remove " + project.id() + ".");
			rebuildWidgets();
			return;
		}
		controller.submit(new DeleteProject(project.id()));
		pendingProjectDelete = "";
	}

	private void openNewPoolDialog() {
		poolIdDraft = suggestedPoolId();
		dialogError = "";
		activeDialog = Dialog.NEW_POOL;
		rebuildWidgets();
	}

	private void openPoolEntryDialog(PoolEntry entry) {
		if (selectedPool().isEmpty()) {
			feedback(Severity.TODO, "Select or create a pool first.");
			return;
		}
		PoolEntry source = entry == null
				? new PoolEntry(SelectorKind.ENTITY, "minecraft:zombie", 1, "", LevelRange.unset(),
						LevelRange.of(1, 1), OptionalXp.automatic())
				: entry;
		editingSelectorId = entry == null ? "" : entry.selectorId();
		editingSelectorKind = entry == null ? SelectorKind.ENTITY : entry.selectorKind();
		selectorKindDraft = source.selectorKind();
		selectorIdDraft = source.selectorId();
		entryWeightDraft = Integer.toString(source.weight());
		requiredModDraft = source.requiredMod();
		eligibleRangePresent = source.eligibleLevel().present();
		eligibleMinDraft = Integer.toString(source.eligibleLevel().min());
		eligibleMaxDraft = Integer.toString(source.eligibleLevel().max());
		spawnRangePresent = source.spawnLevel().present();
		spawnMinDraft = Integer.toString(source.spawnLevel().min());
		spawnMaxDraft = Integer.toString(source.spawnLevel().max());
		baseXpPresent = source.baseXp().present();
		baseXpDraft = Integer.toString(source.baseXp().value());
		dialogError = "";
		entitySuggestionCursor = 0;
		activeDialog = Dialog.POOL_ENTRY;
		rebuildWidgets();
	}

	private void openLayoutSetupDialog() {
		if (model.draft(model.dungeonId()).isEmpty()) {
			feedback(Severity.TODO, "Create or open a saved dungeon before editing its setup.");
			return;
		}
		LayoutDraft draft = model.layout();
		shellBlockDraft = draft.shellBlock();
		shellThicknessDraft = Integer.toString(draft.shellThickness());
		maxDepthDraft = Integer.toString(draft.maxDepth());
		rankDraft.clear();
		rankDraft.addAll(draft.ranks());
		if (rankDraft.containsAll(DUNGEON_RANKS))
			rankDraft.clear();
		dialogError = "";
		activeDialog = Dialog.LAYOUT_SETUP;
		rebuildWidgets();
	}

	private void openDungeonCatalog() {
		if (!canUseDungeonCatalog()) {
			feedback(Severity.INFO, "Preset exports directly; saved dungeon drafts are for MODULE rooms.");
			return;
		}
		if (model.draft(selectedDungeonDraftId).isEmpty())
			selectedDungeonDraftId = preferredDraftId(model, model.dungeonId());
		dialogError = "";
		activeDialog = Dialog.DUNGEON_CATALOG;
		catalogScroll = clamp(catalogScroll, 0, maxCatalogScroll(dialogRect()));
		rebuildWidgets();
	}

	private void openNewDungeonDialog() {
		if (!canUseDungeonCatalog()) {
			dialogError = "Preset exports directly; select a MODULE room first.";
			return;
		}
		if (layoutDirty) {
			dialogError = "Apply or discard the current layout edits before creating another dungeon.";
			return;
		}
		newDungeonIdDraft = suggestedDungeonId();
		dialogError = "";
		activeDialog = Dialog.NEW_DUNGEON;
		rebuildWidgets();
	}

	private void openDeleteDungeonDialog() {
		DraftSummary selected = model.draft(selectedDungeonDraftId).orElse(null);
		if (selected == null) {
			dialogError = "Select a saved dungeon first.";
			return;
		}
		if (layoutDirty) {
			dialogError = "Apply or discard the current layout edits before deleting a dungeon.";
			return;
		}
		dialogError = "";
		activeDialog = Dialog.DELETE_DUNGEON;
		rebuildWidgets();
	}

	private void openSelectedDungeon() {
		DraftSummary selected = model.draft(selectedDungeonDraftId).orElse(null);
		if (selected == null) {
			dialogError = "Select a saved dungeon first.";
			return;
		}
		if (selected.id().equals(model.dungeonId())) {
			closeDialog();
			feedback(Severity.INFO, selected.id() + " is already the active dungeon.");
			return;
		}
		if (layoutDirty) {
			dialogError = "Apply or discard the current layout edits before opening another dungeon.";
			return;
		}
		controller.submit(new SelectDungeon(selected.id()));
		closeDialog();
		feedback(Severity.INFO, "Opening saved dungeon " + selected.id() + "...");
	}

	private void confirmNewDungeon() {
		captureDialogValues();
		String id = normalizedResourceId(newDungeonIdDraft);
		if (!validDungeonId(id)) {
			dialogError = "Use lowercase namespace:path (max 192); path segments cannot be empty or equal to '.' or '..'.";
			return;
		}
		if (model.draft(id).isPresent()) {
			dialogError = "A saved dungeon with this ID already exists. Use Open instead.";
			return;
		}
		controller.submit(new NewDungeon(id));
		selectedDungeonDraftId = id;
		closeDialog();
		feedback(Severity.INFO, "Creating and opening blank dungeon " + id + "...");
	}

	private void confirmDeleteDungeon() {
		DraftSummary selected = model.draft(selectedDungeonDraftId).orElse(null);
		if (selected == null) {
			dialogError = "The selected dungeon no longer exists.";
			return;
		}
		if (layoutDirty) {
			dialogError = "Apply or discard the current layout edits before deleting a dungeon.";
			return;
		}
		String id = selected.id();
		controller.submit(new DeleteDungeon(id));
		selectedDungeonDraftId = "";
		closeDialog();
		feedback(Severity.WARNING, "Deleting saved dungeon " + id + "...");
	}

	private void openAnchorSetupDialog() {
		Anchor anchor = selectedAnchor().orElse(null);
		if (anchor == null) {
			feedback(Severity.TODO, "Select an anchor point in the top view first.");
			return;
		}
		anchorEncounterDraft = anchor.encounterId();
		anchorMinLevelDraft = Integer.toString(anchor.minLevel());
		anchorMaxLevelDraft = Integer.toString(anchor.maxLevel());
		anchorLevelOverrideDraft = anchor.levelOverride();
		dialogError = "";
		activeDialog = Dialog.ANCHOR_SETUP;
		rebuildWidgets();
	}

	private void openPresetSetupDialog() {
		Project project = selectedProject().orElse(null);
		if (project == null || project.kind() != ProjectKind.PRESET) {
			feedback(Severity.TODO, "Select a PRESET project first.");
			return;
		}
		rankDraft.clear();
		rankDraft.addAll(project.ranks());
		if (rankDraft.containsAll(DUNGEON_RANKS))
			rankDraft.clear();
		shellBlockDraft = project.shellBlock();
		shellThicknessDraft = Integer.toString(project.shellThickness());
		dialogError = "";
		activeDialog = Dialog.PRESET_SETUP;
		rebuildWidgets();
	}

	private void confirmActiveDialog() {
		switch (activeDialog) {
			case NEW_ROOM -> confirmCreateRoom();
			case NEW_POOL -> confirmCreatePool();
			case POOL_ENTRY -> confirmPoolEntry();
			case ANCHOR_SETUP -> confirmAnchorSetup();
			case PRESET_SETUP -> confirmPresetSetup();
			case DUNGEON_CATALOG -> openSelectedDungeon();
			case NEW_DUNGEON -> confirmNewDungeon();
			case DELETE_DUNGEON -> confirmDeleteDungeon();
			case LAYOUT_SETUP -> confirmLayoutSetup();
			default -> {
			}
		}
	}

	private void confirmCreateRoom() {
		captureDialogValues();
		String requested = normalizedResourceId(roomIdDraft);
		if (!validRoomProjectId(requested)) {
			dialogError = "Room ID needs namespace:name; each part starts with a letter/number, uses _, - or ., and the name cannot contain /.";
			return;
		}
		if (model.project(requested).isPresent()) {
			dialogError = "A room project with this ID already exists.";
			return;
		}
		int separator = requested.indexOf(':');
		controller.submit(new CreateProject(requested.substring(0, separator), requested.substring(separator + 1),
				projectKindDraft));
		selectedProjectId = requested;
		closeDialog();
		feedback(Severity.INFO, "Creating " + requested + " as a " + projectKindDraft.name().toLowerCase(Locale.ROOT) + "...");
	}

	private void confirmCreatePool() {
		captureDialogValues();
		String requested = normalizedResourceId(poolIdDraft);
		if (!validDatapackResourceId(requested, 192)) {
			dialogError = "Pool ID must use a filesystem-safe lowercase namespace:path.";
			return;
		}
		if (model.pool(requested).isPresent()) {
			dialogError = "A pool with this ID already exists.";
			return;
		}
		controller.submit(new CreatePool(requested));
		selectedPoolId = requested;
		closeDialog();
		feedback(Severity.INFO, "Creating mob pool " + requested + "...");
	}

	private void confirmPoolEntry() {
		captureDialogValues();
		MobPool pool = selectedPool().orElse(null);
		if (pool == null) {
			dialogError = "The selected pool no longer exists; close and refresh.";
			return;
		}
		String selectorId = normalizedResourceId(selectorIdDraft);
		if (!validResourceId(selectorId)) {
			dialogError = "Entity/tag ID must use lowercase namespace:name syntax.";
			return;
		}
		String requiredMod = requiredModDraft.trim().toLowerCase(Locale.ROOT);
		if (!validModId(requiredMod)) {
			dialogError = "Required mod must be empty or 2-64 characters: start with a letter, then use letters, numbers, _ or -.";
			return;
		}
		try {
			int weight = parseIntDraft(entryWeightDraft, "Weight", 1, 1_000_000);
			LevelRange eligible = parseRange(eligibleRangePresent, eligibleMinDraft, eligibleMaxDraft, "Eligible level");
			LevelRange spawn = parseRange(spawnRangePresent, spawnMinDraft, spawnMaxDraft, "Spawn level");
			OptionalXp xp = baseXpPresent
					? new OptionalXp(true, parseIntDraft(baseXpDraft, "Base XP", 0, 1_000_000))
					: OptionalXp.automatic();
			PoolEntry entry = new PoolEntry(selectorKindDraft, selectorId, weight,
					requiredMod, eligible, spawn, xp);
			String previousId = editingSelectorId;
			List<PoolEntry> replacement = new ArrayList<>();
			for (PoolEntry existing : pool.entries()) {
				boolean original = !previousId.isBlank() && existing.selectorKind() == editingSelectorKind
						&& existing.selectorId().equals(previousId);
				boolean duplicate = previousId.isBlank() && existing.selectorKind() == entry.selectorKind()
						&& existing.selectorId().equals(entry.selectorId());
				if (!original && !duplicate)
					replacement.add(existing);
			}
			replacement.add(entry);
			controller.submit(new SavePoolDraft(pool.id(), replacement));
			closeDialog();
			feedback(Severity.INFO, (previousId.isBlank() ? "Adding " : "Updating ") + entry.selectorLabel() + "...");
		} catch (IllegalArgumentException exception) {
			dialogError = exception.getMessage();
		}
	}

	private void confirmLayoutSetup() {
		captureDialogValues();
		String dungeonId = model.dungeonId();
		String shellBlock = normalizedResourceId(shellBlockDraft);
		if (model.draft(dungeonId).isEmpty()) {
			dialogError = "Create or open a saved dungeon before editing its setup.";
			return;
		}
		if (!validResourceId(shellBlock)) {
			dialogError = "Shell block must use lowercase namespace:name syntax.";
			return;
		}
		try {
			int thickness = parseIntDraft(shellThicknessDraft, "Shell thickness", 0, 4);
			int maxDepth = parseIntDraft(maxDepthDraft, "Maximum depth", 1, 64);
			LayoutDraft source = model.layout();
			LayoutDraft updated = new LayoutDraft(source.mode(), source.topology(), source.minRooms(), source.maxRooms(),
					maxDepth, Set.copyOf(rankDraft), shellBlock, thickness, source.enabledProjectIds(),
					source.roomWeights(), source.nodes(), source.connections());
			setLocalLayout(updated);
			closeDialog();
			feedback(Severity.INFO, "Updated setup for " + dungeonId + ". Press Layout Apply to save.");
		} catch (IllegalArgumentException exception) {
			dialogError = exception.getMessage();
		}
	}

	private void confirmAnchorSetup() {
		captureDialogValues();
		Project project = selectedProject().orElse(null);
		Anchor anchor = selectedAnchor().orElse(null);
		if (project == null || anchor == null) {
			dialogError = "The selected anchor no longer exists; close and refresh.";
			return;
		}
		String encounterId = anchorEncounterDraft.trim().toLowerCase(Locale.ROOT);
		if (!validEncounterId(encounterId)) {
			dialogError = "Encounter ID is 1-64 characters, starts with a letter/number, uses _, - or ., and cannot end with a dot.";
			return;
		}
		if (anchor.kind() == AnchorKind.TRIGGER) {
			controller.submit(new AssignAnchor(project.id(), anchor.id(), AnchorKind.TRIGGER, SpawnRole.NONE,
					anchor.triggerBounds(), encounterId, "", false, anchor.minLevel(), anchor.maxLevel(), false));
			closeDialog();
			feedback(Severity.INFO, "Saving trigger encounter group...");
			return;
		}
		try {
			int min = anchor.minLevel();
			int max = anchor.maxLevel();
			if (anchorLevelOverrideDraft) {
				LevelRange range = parseRange(true, anchorMinLevelDraft, anchorMaxLevelDraft, "Anchor level");
				min = range.min();
				max = range.max();
			}
			controller.submit(new AssignAnchor(project.id(), anchor.id(), anchor.kind(), anchor.spawnRole(),
					anchor.triggerBounds(), encounterId, anchor.poolId(), anchorLevelOverrideDraft,
					min, max, anchor.delayed()));
			closeDialog();
			feedback(Severity.INFO, "Saving encounter group and level policy...");
		} catch (IllegalArgumentException exception) {
			dialogError = exception.getMessage();
		}
	}

	private void confirmPresetSetup() {
		captureDialogValues();
		Project project = selectedProject().orElse(null);
		if (project == null || project.kind() != ProjectKind.PRESET) {
			dialogError = "The selected preset no longer exists; close and refresh.";
			return;
		}
		String shellBlock = normalizedResourceId(shellBlockDraft);
		if (!validResourceId(shellBlock)) {
			dialogError = "Shell block must use lowercase namespace:name syntax.";
			return;
		}
		try {
			int thickness = parseIntDraft(shellThicknessDraft, "Shell thickness", 0, 4);
			controller.submit(new SetProjectSettings(project.id(), Set.copyOf(rankDraft), shellBlock, thickness));
			closeDialog();
			feedback(Severity.INFO, "Saving preset rank routing and shell rules...");
		} catch (IllegalArgumentException exception) {
			dialogError = exception.getMessage();
		}
	}

	private void closeDialog() {
		captureDialogValues();
		activeDialog = Dialog.NONE;
		dialogError = "";
		editingSelectorId = "";
		editingSelectorKind = SelectorKind.ENTITY;
		rebuildWidgets();
	}

	private void toggleSelectorKind() {
		selectorKindDraft = selectorKindDraft == SelectorKind.ENTITY ? SelectorKind.TAG : SelectorKind.ENTITY;
		if (selectorIdBox != null) {
			String value = selectorIdBox.getValue();
			selectorIdBox.setValue(value.startsWith("#") ? value.substring(1) : value);
		}
		dialogError = "";
		if (selectorKindButton != null)
			selectorKindButton.setMessage(Component.literal(selectorKindLabel()));
	}

	private void toggleProjectKind() {
		projectKindDraft = projectKindDraft == ProjectKind.MODULE ? ProjectKind.PRESET : ProjectKind.MODULE;
		if (projectKindButton != null)
			projectKindButton.setMessage(Component.literal("TYPE: " + projectKindDraft.name()));
	}

	private void toggleEligibleRange() {
		eligibleRangePresent = !eligibleRangePresent;
		if (eligibleRangeButton != null)
			eligibleRangeButton.setMessage(Component.literal(eligibleRangeLabel()));
	}

	private void toggleSpawnRange() {
		spawnRangePresent = !spawnRangePresent;
		if (spawnRangeButton != null)
			spawnRangeButton.setMessage(Component.literal(spawnRangeLabel()));
	}

	private void toggleXpMode() {
		baseXpPresent = !baseXpPresent;
		if (xpModeButton != null)
			xpModeButton.setMessage(Component.literal(xpModeLabel()));
	}

	private void toggleAnchorLevelOverride() {
		anchorLevelOverrideDraft = !anchorLevelOverrideDraft;
		if (anchorLevelModeButton != null)
			anchorLevelModeButton.setMessage(Component.literal(anchorLevelModeLabel()));
	}

	private void toggleRank(String rank) {
		if (!rankDraft.remove(rank))
			rankDraft.add(rank);
	}

	private void suggestEntityId() {
		if (selectorKindDraft == SelectorKind.TAG) {
			dialogError = "Entity tags come from datapacks; enter their namespaced ID without the # symbol.";
			return;
		}
		List<String> installed = BuiltInRegistries.ENTITY_TYPE.keySet().stream()
				.map(ResourceLocation::toString).sorted().toList();
		if (installed.isEmpty()) {
			dialogError = "No installed entity registry entries are available.";
			return;
		}
		String current = selectorIdBox == null ? selectorIdDraft : selectorIdBox.getValue();
		int exact = installed.indexOf(current);
		String suggestion;
		if (exact >= 0) {
			suggestion = installed.get((exact + 1) % installed.size());
		} else {
			List<String> matches = installed.stream().filter(id -> id.startsWith(current)).limit(256).toList();
			List<String> candidates = matches.isEmpty() ? installed : matches;
			suggestion = candidates.get(Math.floorMod(entitySuggestionCursor++, candidates.size()));
		}
		selectorIdDraft = suggestion;
		if (selectorIdBox != null)
			selectorIdBox.setValue(suggestion);
		dialogError = "";
	}

	private String selectorKindLabel() {
		return selectorKindDraft == SelectorKind.ENTITY ? "ENTITY" : "TAG (#)";
	}

	private String eligibleRangeLabel() {
		return eligibleRangePresent ? "ELIGIBLE: SET" : "ELIGIBLE: ALL";
	}

	private String spawnRangeLabel() {
		return spawnRangePresent ? "SPAWN: SET" : "SPAWN: RANK";
	}

	private String xpModeLabel() {
		return baseXpPresent ? "XP: EXPLICIT" : "XP: AUTO";
	}

	private String anchorLevelModeLabel() {
		return anchorLevelOverrideDraft ? "LEVEL: OVERRIDE" : "LEVEL: INHERIT";
	}

	private String selectorResolutionText() {
		if (selectorKindDraft == SelectorKind.TAG)
			return "[TAG] Resolved from loaded datapacks when the dungeon is loaded.";
		String value = selectorIdBox == null ? selectorIdDraft : selectorIdBox.getValue();
		String required = requiredModBox == null ? requiredModDraft : requiredModBox.getValue();
		ResourceLocation id = ResourceLocation.tryParse(value);
		if (id != null && BuiltInRegistries.ENTITY_TYPE.containsKey(id)
				&& "sololeveling".equals(required))
			return "[LOADED] Optional Mod is unnecessary for Solo Leveling entities.";
		if (id != null && BuiltInRegistries.ENTITY_TYPE.containsKey(id))
			return "[LOADED] Entity is present in the current mod registry.";
		return "[EXTERNAL] Allowed; set Optional Mod only when this addon may be absent.";
	}

	private int selectorResolutionColor() {
		if (selectorKindDraft == SelectorKind.TAG)
			return INFO;
		String value = selectorIdBox == null ? selectorIdDraft : selectorIdBox.getValue();
		ResourceLocation id = ResourceLocation.tryParse(value);
		return id != null && BuiltInRegistries.ENTITY_TYPE.containsKey(id) ? SUCCESS : WARNING;
	}

	private LevelRange parseRange(boolean present, String minText, String maxText, String label) {
		if (!present)
			return LevelRange.unset();
		int min = parseIntDraft(minText, label + " minimum", 1, 1_000);
		int max = parseIntDraft(maxText, label + " maximum", 1, 1_000);
		if (max < min)
			throw new IllegalArgumentException(label + " maximum cannot be lower than its minimum.");
		return LevelRange.of(min, max);
	}

	private static int parseIntDraft(String text, String label, int min, int max) {
		try {
			int value = Integer.parseInt(text == null || text.isBlank() ? "-1" : text);
			if (value < min || value > max)
				throw new IllegalArgumentException(label + " must be between " + min + " and " + max + ".");
			return value;
		} catch (NumberFormatException exception) {
			throw new IllegalArgumentException(label + " must be a whole number.");
		}
	}

	private String preferredNamespace() {
		String source = selectedProject().map(Project::id).orElse(model.dungeonId());
		int separator = source.indexOf(':');
		return separator > 0 ? source.substring(0, separator) : "builder";
	}

	private String suggestedPoolId() {
		String namespace = preferredNamespace();
		int suffix = Math.max(1, model.pools().size() + 1);
		String id;
		do {
			id = namespace + ":pool_" + suffix++;
		} while (model.pool(id).isPresent());
		return id;
	}

	private String suggestedRoomId() {
		String namespace = preferredNamespace();
		int suffix = 1;
		String id;
		do {
			id = namespace + ":room_" + suffix++;
		} while (model.project(id).isPresent());
		return id;
	}

	private String suggestedDungeonId() {
		String namespace = preferredNamespace();
		int suffix = 1;
		String id;
		do {
			id = namespace + ":dungeon_" + suffix++;
		} while (model.draft(id).isPresent());
		return id;
	}

	private void captureSelectedProject() {
		selectedProject().ifPresentOrElse(project -> {
			if (project.bounds() == null) {
				feedback(Severity.ERROR, "Set structure bounds before capturing.");
				return;
			}
			controller.submit(new CaptureSnapshot(project.id(), project.snapshotCaptured()));
			feedback(Severity.INFO, project.snapshotCaptured() ? "Updating explicit room snapshot..." : "Capturing room snapshot...");
		}, () -> feedback(Severity.TODO, "Select a room first."));
	}

	private void toggleSelectedSocketRequired() {
		Project project = selectedProject().orElse(null);
		Socket socket = selectedSocket().orElse(null);
		if (project == null || socket == null) {
			feedback(Severity.TODO, "Select a socket in the top view first.");
			return;
		}
		controller.submit(new EditSocket(project.id(), socket.id(), socket.type(), !socket.required()));
	}

	private void cycleSelectedSocketType() {
		Project project = selectedProject().orElse(null);
		Socket socket = selectedSocket().orElse(null);
		if (project == null || socket == null) {
			feedback(Severity.TODO, "Select a socket in the top view first.");
			return;
		}
		SocketType next = SocketType.values()[(socket.type().ordinal() + 1) % SocketType.values().length];
		controller.submit(new EditSocket(project.id(), socket.id(), next, socket.required()));
	}

	private void cycleSelectedAnchorRole() {
		Project project = selectedProject().orElse(null);
		Anchor anchor = selectedAnchor().orElse(null);
		if (project == null || anchor == null) {
			feedback(Severity.TODO, "Select an anchor in the top view first.");
			return;
		}
		if (anchor.kind() == AnchorKind.TRIGGER) {
			feedback(Severity.INFO, "Trigger anchors only edit their encounter group through Configure.");
			return;
		}
		SpawnRole next = switch (anchor.spawnRole()) {
			case NONE, BOSS -> SpawnRole.NORMAL;
			case NORMAL -> SpawnRole.ELITE;
			case ELITE -> SpawnRole.BOSS;
		};
		AnchorKind kind = switch (next) {
			case NORMAL -> AnchorKind.MOB_SPAWN;
			case ELITE -> AnchorKind.ELITE_SPAWN;
			case BOSS -> AnchorKind.BOSS_SPAWN;
			default -> AnchorKind.SPAWN_POINT;
		};
		String pool = anchor.poolId();
		if (pool.isBlank()) {
			if (model.pools().isEmpty()) {
				feedback(Severity.TODO, "Create a mob pool first; a spawn role requires both role and pool.");
				return;
			}
			pool = model.pools().get(0).id();
		}
		submitAnchor(project, anchor, kind, next, pool, anchor.levelOverride(),
				anchor.minLevel(), anchor.maxLevel(), anchor.delayed());
	}

	private void cycleSelectedAnchorPool() {
		Project project = selectedProject().orElse(null);
		Anchor anchor = selectedAnchor().orElse(null);
		if (project == null || anchor == null) {
			feedback(Severity.TODO, "Select an anchor in the top view first.");
			return;
		}
		if (anchor.kind() == AnchorKind.TRIGGER) {
			feedback(Severity.INFO, "Trigger anchors do not own mob pools; configure the group's spawn anchors.");
			return;
		}
		if (model.pools().isEmpty()) {
			feedback(Severity.TODO, "Create a mob pool before assigning this anchor.");
			return;
		}
		int current = -1;
		for (int index = 0; index < model.pools().size(); index++)
			if (model.pools().get(index).id().equals(anchor.poolId()))
				current = index;
		String pool = model.pools().get((current + 1) % model.pools().size()).id();
		SpawnRole role = anchor.spawnRole() == SpawnRole.NONE ? SpawnRole.NORMAL : anchor.spawnRole();
		AnchorKind kind = anchor.spawnRole() == SpawnRole.NONE ? AnchorKind.MOB_SPAWN : anchor.kind();
		submitAnchor(project, anchor, kind, role, pool, anchor.levelOverride(),
				anchor.minLevel(), anchor.maxLevel(), anchor.delayed());
	}

	private void shiftSelectedAnchorLevel(int amount) {
		Project project = selectedProject().orElse(null);
		Anchor anchor = selectedAnchor().orElse(null);
		if (project == null || anchor == null) {
			feedback(Severity.TODO, "Select an anchor in the top view first.");
			return;
		}
		if (anchor.kind() == AnchorKind.TRIGGER) {
			feedback(Severity.INFO, "Trigger anchors inherit encounter timing; levels belong to spawn anchors.");
			return;
		}
		int min = clamp(anchor.minLevel() + amount, 1, 1_000);
		int max = clamp(anchor.maxLevel() + amount, min, 1_000);
		submitAnchor(project, anchor, anchor.kind(), anchor.spawnRole(), anchor.poolId(), true,
				min, max, anchor.delayed());
	}

	private void toggleSelectedAnchorDelay() {
		Project project = selectedProject().orElse(null);
		Anchor anchor = selectedAnchor().orElse(null);
		if (project == null || anchor == null) {
			feedback(Severity.TODO, "Select an anchor in the top view first.");
			return;
		}
		if (anchor.kind() == AnchorKind.TRIGGER) {
			feedback(Severity.INFO, "Trigger anchors are the gate; delay is configured on spawn anchors.");
			return;
		}
		submitAnchor(project, anchor, anchor.kind(), anchor.spawnRole(), anchor.poolId(), anchor.levelOverride(),
				anchor.minLevel(), anchor.maxLevel(), !anchor.delayed());
	}

	private void submitAnchor(Project project, Anchor anchor, AnchorKind kind, SpawnRole role,
			String pool, boolean levelOverride, int minLevel, int maxLevel, boolean delayed) {
		controller.submit(new AssignAnchor(project.id(), anchor.id(), kind, role, anchor.triggerBounds(),
				anchor.encounterId(), pool, levelOverride, minLevel, maxLevel, delayed));
	}

	private void createPoolDraft() {
		openNewPoolDialog();
	}

	private void beginPoolEntryDraft() {
		openPoolEntryDialog(null);
	}

	private void savePoolDraft() {
		selectedPool().ifPresentOrElse(pool -> controller.submit(new SavePoolDraft(pool.id(), pool.entries())),
				() -> feedback(Severity.TODO, "Select or create a pool first."));
	}

	private void deletePool() {
		MobPool pool = selectedPool().orElse(null);
		if (pool == null) {
			feedback(Severity.TODO, "Select a pool first.");
			return;
		}
		if (!pendingPoolDelete.equals(pool.id())) {
			pendingPoolDelete = pool.id();
			feedback(Severity.WARNING, "Press Confirm Delete to remove " + pool.id() + ".");
			rebuildWidgets();
			return;
		}
		controller.submit(new DeletePool(pool.id()));
		pendingPoolDelete = "";
	}

	private String layoutIncludeLabel() {
		return selectedProject().filter(project -> model.layout().enabledProjectIds().contains(project.id())).isPresent()
				? "Exclude" : "Include";
	}

	private void toggleSelectedProjectIncluded() {
		if (!hasActiveDungeonDraft())
			return;
		Project project = selectedProject().orElse(null);
		if (project == null) {
			feedback(Severity.TODO, "Select a room in the library first.");
			return;
		}
		if (project.kind() != ProjectKind.MODULE) {
			feedback(Severity.WARNING, "PRESET is a complete dungeon. Include MODULE rooms in procedural layouts.");
			return;
		}
		LayoutDraft source = model.layout();
		List<String> enabled = new ArrayList<>(source.enabledProjectIds());
		boolean removing = enabled.remove(project.id());
		if (!removing)
			enabled.add(project.id());
		List<RoomWeight> weights = ensureRoomWeight(source.roomWeights(), project);
		setLocalLayout(copyLayoutParts(source, enabled, weights, source.nodes(), source.connections()));
		feedback(Severity.INFO, (removing ? "Excluded " : "Included ") + project.id() + ". Press Apply to save.");
		rebuildWidgets();
	}

	private void addSelectedRoomNode() {
		if (!hasActiveDungeonDraft())
			return;
		if (model.layout().mode() != LayoutMode.FIXED) {
			feedback(Severity.TODO, "Switch Layout Mode to FIXED before placing exact nodes.");
			return;
		}
		Project project = selectedProject().orElse(null);
		if (project == null || project.kind() != ProjectKind.MODULE || project.bounds() == null) {
			feedback(Severity.TODO, "Select a captured room with valid bounds first.");
			return;
		}
		LayoutDraft source = model.layout();
		if (source.nodes().size() >= DungeonBuilderStudioModel.MAX_LAYOUT_ROOMS) {
			feedback(Severity.ERROR, "Fixed layouts support at most " + DungeonBuilderStudioModel.MAX_LAYOUT_ROOMS + " placements.");
			return;
		}
		int suffix = 1;
		String nodeId;
		while (true) {
			String candidate = "node_" + suffix++;
			if (source.nodes().stream().noneMatch(node -> node.id().equals(candidate))) {
				nodeId = candidate;
				break;
			}
		}
		int x = source.nodes().stream().mapToInt(node -> node.x() + node.width()).max().orElse(-4) + 4;
		LayoutNode node = new LayoutNode(nodeId, project.id(), project.role(), x, 0, 0,
				project.bounds().width(), project.bounds().depth(), 0, false);
		List<LayoutNode> nodes = new ArrayList<>(source.nodes());
		nodes.add(node);
		List<String> enabled = new ArrayList<>(source.enabledProjectIds());
		if (!enabled.contains(project.id()))
			enabled.add(project.id());
		setLocalLayout(copyLayoutParts(source, enabled, ensureRoomWeight(source.roomWeights(), project),
				nodes, source.connections()));
		selectedLayoutNodeId = node.id();
		selectedLayoutSocketId = "";
		feedback(Severity.INFO, "Placed " + node.id() + " at X " + node.x() + ", Z " + node.z() + ".");
		rebuildWidgets();
	}

	private void moveSelectedLayoutNode(int dx, int dz) {
		LayoutNode node = selectedLayoutNode().orElse(null);
		if (node == null) {
			feedback(Severity.TODO, "Click a fixed-layout node first.");
			return;
		}
		if (isLayoutNodeConnected(node.id())) {
			feedback(Severity.WARNING, "Disconnect this node before moving it; connected socket geometry must stay aligned.");
			return;
		}
		int step = hasShiftDown() ? 4 : 1;
		LayoutNode moved = translateLayoutNodeWithoutCollision(node, dx * step, dz * step);
		if (moved.x() == node.x() && moved.z() == node.z()) {
			feedback(Severity.WARNING, "Another room blocks movement in that direction.");
			return;
		}
		replaceLayoutNode(moved);
		boolean blocked = moved.x() != node.x() + dx * step || moved.z() != node.z() + dz * step;
		feedback(blocked ? Severity.WARNING : Severity.INFO, (blocked ? "Stopped at another room. " : "")
				+ "Node position: X " + moved.x() + ", Z " + moved.z() + ".");
	}

	private void rotateSelectedLayoutNode() {
		LayoutNode node = selectedLayoutNode().orElse(null);
		if (node == null) {
			feedback(Severity.TODO, "Click a fixed-layout node first.");
			return;
		}
		if (isLayoutNodeConnected(node.id())) {
			feedback(Severity.WARNING, "Disconnect this node before rotating it; connected socket geometry must stay aligned.");
			return;
		}
		LayoutNode rotated = new LayoutNode(node.id(), node.projectId(), node.role(), node.x(), node.y(), node.z(),
				node.depth(), node.width(), node.rotation() + 90, node.locked());
		if (layoutNodeCollides(rotated)) {
			feedback(Severity.WARNING, "Rotation would overlap another room. Move this node first.");
			return;
		}
		replaceLayoutNode(rotated);
		feedback(Severity.INFO, "Rotated " + node.id() + " to " + Math.floorMod(node.rotation() + 90, 360) + " degrees.");
	}

	private void deleteSelectedLayoutNode() {
		LayoutNode node = selectedLayoutNode().orElse(null);
		if (node == null) {
			feedback(Severity.TODO, "Click a fixed-layout node first.");
			return;
		}
		LayoutDraft source = model.layout();
		List<LayoutNode> nodes = source.nodes().stream().filter(value -> !value.id().equals(node.id())).toList();
		List<LayoutConnection> connections = source.connections().stream()
				.filter(connection -> !connection.fromNodeId().equals(node.id()) && !connection.toNodeId().equals(node.id()))
				.toList();
		List<String> enabled = new ArrayList<>(source.enabledProjectIds());
		if (nodes.stream().noneMatch(value -> value.projectId().equals(node.projectId())))
			enabled.remove(node.projectId());
		setLocalLayout(copyLayoutParts(source, enabled, source.roomWeights(), nodes, connections));
		selectedLayoutNodeId = "";
		selectedLayoutSocketId = "";
		if (pendingConnectionNodeId.equals(node.id()))
			clearPendingConnection();
		feedback(Severity.INFO, "Removed " + node.id() + " and its connections.");
		rebuildWidgets();
	}

	private void cycleLayoutSocket() {
		LayoutNode node = selectedLayoutNode().orElse(null);
		Project project = node == null ? null : model.project(node.projectId()).orElse(null);
		if (node == null || project == null || project.sockets().isEmpty()) {
			feedback(Severity.TODO, "Select a node whose room has at least one socket.");
			return;
		}
		int current = -1;
		for (int index = 0; index < project.sockets().size(); index++)
			if (project.sockets().get(index).id().equals(selectedLayoutSocketId))
				current = index;
		Socket socket = project.sockets().get((current + 1) % project.sockets().size());
		selectedLayoutSocketId = socket.id();
		feedback(Severity.INFO, "Endpoint " + node.id() + " / " + socket.id() + " faces "
				+ rotatedFacing(socket.facing(), node.rotation()).name() + ".");
		rebuildWidgets();
	}

	private String layoutLinkLabel() {
		LayoutNode node = selectedLayoutNode().orElse(null);
		return node != null && !selectedLayoutSocketId.isBlank()
				&& isLayoutSocketUsed(node.id(), selectedLayoutSocketId) ? "Unlink" : "Link";
	}

	private void connectSelectedLayoutSocket() {
		LayoutNode node = selectedLayoutNode().orElse(null);
		Socket socket = node == null ? null : layoutSocket(node, selectedLayoutSocketId).orElse(null);
		if (node == null || socket == null) {
			feedback(Severity.TODO, "Select a node, press Socket to choose its endpoint, then press Link.");
			return;
		}
		if (isLayoutSocketUsed(node.id(), socket.id())) {
			List<LayoutConnection> remaining = model.layout().connections().stream()
					.filter(connection -> !((connection.fromNodeId().equals(node.id())
							&& connection.fromSocketId().equals(socket.id()))
							|| (connection.toNodeId().equals(node.id())
									&& connection.toSocketId().equals(socket.id()))))
					.toList();
			setLocalLayout(copyLayoutParts(model.layout(), model.layout().enabledProjectIds(),
					model.layout().roomWeights(), model.layout().nodes(), remaining));
			clearPendingConnection();
			feedback(Severity.INFO, "Disconnected " + node.id() + " / " + socket.id()
					+ ". The rooms keep their current positions until you move or relink them.");
			rebuildWidgets();
			return;
		}
		if (pendingConnectionNodeId.isBlank()) {
			pendingConnectionNodeId = node.id();
			pendingConnectionSocketId = socket.id();
			feedback(Severity.INFO, "First endpoint stored. Select another node/socket and press Link.");
			return;
		}
		if (model.layout().connections().size() >= DungeonBuilderStudioModel.MAX_CONNECTIONS) {
			clearPendingConnection();
			feedback(Severity.ERROR, "Fixed layouts support at most " + DungeonBuilderStudioModel.MAX_CONNECTIONS + " connections.");
			return;
		}
		LayoutNode fromNode = model.layout().nodes().stream()
				.filter(value -> value.id().equals(pendingConnectionNodeId)).findFirst().orElse(null);
		Socket fromSocket = fromNode == null ? null : layoutSocket(fromNode, pendingConnectionSocketId).orElse(null);
		if (fromNode == null || fromSocket == null) {
			clearPendingConnection();
			feedback(Severity.WARNING, "The first endpoint disappeared; select it again.");
			return;
		}
		if (fromNode.id().equals(node.id())) {
			feedback(Severity.WARNING, "Choose an endpoint on a different node.");
			return;
		}
		if (isLayoutSocketUsed(fromNode.id(), fromSocket.id())) {
			clearPendingConnection();
			feedback(Severity.WARNING, "The first endpoint was connected elsewhere; select another endpoint.");
			return;
		}
		if (!compatibleLayoutSockets(fromNode, fromSocket, node, socket)) {
			feedback(Severity.ERROR, "Sockets must face opposite directions and match type/opening size.");
			return;
		}
		boolean fromConnected = isLayoutNodeConnected(fromNode.id());
		boolean targetConnected = isLayoutNodeConnected(node.id());
		if (fromConnected && targetConnected) {
			feedback(Severity.ERROR, "Both rooms already belong to the layout graph. Link a new/unconnected node to avoid breaking existing geometry.");
			return;
		}
		LayoutNode movingNode = targetConnected ? fromNode : node;
		LayoutNode snapped = targetConnected
				? snapLayoutNode(node, socket, fromNode, fromSocket)
				: snapLayoutNode(fromNode, fromSocket, node, socket);
		if (snapped == null) {
			feedback(Severity.ERROR, "Socket coordinates could not be transformed from the captured room bounds.");
			return;
		}
		if (layoutNodeCollides(snapped)) {
			feedback(Severity.ERROR, "Auto-placement would overlap another room. Move or rotate that branch, then Link again.");
			return;
		}
		List<LayoutConnection> connections = new ArrayList<>(model.layout().connections());
		connections.add(new LayoutConnection(fromNode.id(), fromSocket.id(), node.id(), socket.id()));
		List<LayoutNode> snappedNodes = new ArrayList<>(model.layout().nodes().size());
		for (LayoutNode value : model.layout().nodes())
			snappedNodes.add(value.id().equals(snapped.id()) ? snapped : value);
		setLocalLayout(copyLayoutParts(model.layout(), model.layout().enabledProjectIds(), model.layout().roomWeights(),
				snappedNodes, connections));
		clearPendingConnection();
		feedback(Severity.PASS, "Connected and snapped " + movingNode.id() + " to X " + snapped.x() + ", Y "
				+ snapped.y() + ", Z " + snapped.z() + ". Press Apply to save.");
		rebuildWidgets();
	}

	private LayoutNode snapLayoutNode(LayoutNode sourceNode, Socket sourceSocket,
			LayoutNode targetNode, Socket targetSocket) {
		Point sourceOffset = transformedSocketOffset(sourceNode, sourceSocket);
		Point targetOffset = transformedSocketOffset(targetNode, targetSocket);
		Project sourceProject = model.project(sourceNode.projectId()).orElse(null);
		Project targetProject = model.project(targetNode.projectId()).orElse(null);
		if (sourceOffset == null || targetOffset == null || sourceProject == null || targetProject == null
				|| sourceProject.bounds() == null || targetProject.bounds() == null)
			return null;
		int sourceSocketX = sourceNode.x() + sourceOffset.x();
		int sourceSocketY = sourceNode.y() + sourceOffset.y();
		int sourceSocketZ = sourceNode.z() + sourceOffset.z();
		int x = targetNode.x();
		int y = sourceSocketY - targetOffset.y();
		int z = targetNode.z();
		switch (rotatedFacing(sourceSocket.facing(), sourceNode.rotation())) {
			case EAST -> {
				x = sourceNode.x() + sourceNode.width();
				z = sourceSocketZ - targetOffset.z();
			}
			case WEST -> {
				x = sourceNode.x() - targetNode.width();
				z = sourceSocketZ - targetOffset.z();
			}
			case SOUTH -> {
				x = sourceSocketX - targetOffset.x();
				z = sourceNode.z() + sourceNode.depth();
			}
			case NORTH -> {
				x = sourceSocketX - targetOffset.x();
				z = sourceNode.z() - targetNode.depth();
			}
			case UP -> {
				x = sourceSocketX - targetOffset.x();
				y = sourceNode.y() + sourceProject.bounds().height();
				z = sourceSocketZ - targetOffset.z();
			}
			case DOWN -> {
				x = sourceSocketX - targetOffset.x();
				y = sourceNode.y() - targetProject.bounds().height();
				z = sourceSocketZ - targetOffset.z();
			}
		}
		return new LayoutNode(targetNode.id(), targetNode.projectId(), targetNode.role(), x, y, z,
				targetNode.width(), targetNode.depth(), targetNode.rotation(), targetNode.locked());
	}

	private Point transformedSocketOffset(LayoutNode node, Socket socket) {
		Project project = model.project(node.projectId()).orElse(null);
		if (project == null || project.bounds() == null)
			return null;
		Bounds bounds = project.bounds();
		int localX = socket.position().x() - bounds.min().x();
		int localY = socket.position().y() - bounds.min().y();
		int localZ = socket.position().z() - bounds.min().z();
		return switch (Math.floorMod(node.rotation(), 360)) {
			case 90 -> new Point(bounds.depth() - 1 - localZ, localY, localX);
			case 180 -> new Point(bounds.width() - 1 - localX, localY, bounds.depth() - 1 - localZ);
			case 270 -> new Point(localZ, localY, bounds.width() - 1 - localX);
			default -> new Point(localX, localY, localZ);
		};
	}

	private boolean layoutNodeCollides(LayoutNode candidate) {
		Project candidateProject = model.project(candidate.projectId()).orElse(null);
		if (candidateProject == null || candidateProject.bounds() == null)
			return true;
		int candidateHeight = candidateProject.bounds().height();
		for (LayoutNode other : model.layout().nodes()) {
			if (other.id().equals(candidate.id()))
				continue;
			Project otherProject = model.project(other.projectId()).orElse(null);
			if (otherProject == null || otherProject.bounds() == null)
				continue;
			boolean overlapX = candidate.x() < other.x() + other.width() && candidate.x() + candidate.width() > other.x();
			boolean overlapY = candidate.y() < other.y() + otherProject.bounds().height()
					&& candidate.y() + candidateHeight > other.y();
			boolean overlapZ = candidate.z() < other.z() + other.depth() && candidate.z() + candidate.depth() > other.z();
			if (overlapX && overlapY && overlapZ)
				return true;
		}
		return false;
	}

	private LayoutNode translateLayoutNodeWithoutCollision(LayoutNode start, int deltaX, int deltaZ) {
		int steps = Math.max(Math.abs(deltaX), Math.abs(deltaZ));
		if (steps == 0)
			return start;
		LayoutNode current = start;
		for (int index = 1; index <= steps; index++) {
			int x = start.x() + (int) Math.round(deltaX * (index / (double) steps));
			int z = start.z() + (int) Math.round(deltaZ * (index / (double) steps));
			if (x == current.x() && z == current.z())
				continue;
			LayoutNode candidate = new LayoutNode(start.id(), start.projectId(), start.role(), x, start.y(), z,
					start.width(), start.depth(), start.rotation(), start.locked());
			if (layoutNodeCollides(candidate))
				return current;
			current = candidate;
		}
		return current;
	}

	private void replaceLayoutNode(LayoutNode replacement) {
		LayoutDraft source = model.layout();
		List<LayoutNode> nodes = new ArrayList<>(source.nodes().size());
		for (LayoutNode node : source.nodes())
			nodes.add(node.id().equals(replacement.id()) ? replacement : node);
		setLocalLayout(copyLayoutParts(source, source.enabledProjectIds(), source.roomWeights(), nodes, source.connections()));
	}

	private List<RoomWeight> ensureRoomWeight(List<RoomWeight> source, Project project) {
		if (source.stream().anyMatch(weight -> weight.projectId().equals(project.id())))
			return source;
		List<RoomWeight> result = new ArrayList<>(source);
		result.add(new RoomWeight(project.id(), project.weight()));
		return result;
	}

	private int dungeonRoomWeight(Project project) {
		return model.layout().roomWeights().stream()
				.filter(weight -> weight.projectId().equals(project.id()))
				.mapToInt(RoomWeight::weight).findFirst().orElse(project.weight());
	}

	private void adjustDungeonRoomWeight(Project project, int amount) {
		if (model.draft(model.dungeonId()).isEmpty()) {
			feedback(Severity.TODO, "Create or open a saved dungeon before changing its room weights.");
			return;
		}
		LayoutDraft source = model.layout();
		if (source.mode() != LayoutMode.PROCEDURAL || project.kind() != ProjectKind.MODULE
				|| !source.enabledProjectIds().contains(project.id()))
			return;
		int next = clamp(dungeonRoomWeight(project) + amount, 1, 1_000_000);
		List<RoomWeight> weights = new ArrayList<>(source.roomWeights().size() + 1);
		boolean replaced = false;
		for (RoomWeight weight : source.roomWeights()) {
			if (weight.projectId().equals(project.id())) {
				if (!replaced)
					weights.add(new RoomWeight(project.id(), next));
				replaced = true;
			} else {
				weights.add(weight);
			}
		}
		if (!replaced)
			weights.add(new RoomWeight(project.id(), next));
		setLocalLayout(copyLayoutParts(source, source.enabledProjectIds(), weights,
				source.nodes(), source.connections()));
		feedback(Severity.INFO, "Dungeon weight for " + project.id() + " is " + next + ". Press Apply to save.");
	}

	private LayoutDraft copyLayoutParts(LayoutDraft source, List<String> enabled, List<RoomWeight> weights,
			List<LayoutNode> nodes, List<LayoutConnection> connections) {
		return new LayoutDraft(source.mode(), source.topology(), source.minRooms(), source.maxRooms(), source.maxDepth(),
				source.ranks(), source.shellBlock(), source.shellThickness(), enabled, weights, nodes, connections);
	}

	private Optional<LayoutNode> selectedLayoutNode() {
		return model.layout().nodes().stream().filter(node -> node.id().equals(selectedLayoutNodeId)).findFirst();
	}

	private Optional<Socket> layoutSocket(LayoutNode node, String socketId) {
		return model.project(node.projectId()).flatMap(project -> project.sockets().stream()
				.filter(socket -> socket.id().equals(socketId)).findFirst());
	}

	private boolean isLayoutSocketUsed(String nodeId, String socketId) {
		return model.layout().connections().stream().anyMatch(connection ->
				(connection.fromNodeId().equals(nodeId) && connection.fromSocketId().equals(socketId))
						|| (connection.toNodeId().equals(nodeId) && connection.toSocketId().equals(socketId)));
	}

	private boolean isLayoutNodeConnected(String nodeId) {
		return model.layout().connections().stream().anyMatch(connection ->
				connection.fromNodeId().equals(nodeId) || connection.toNodeId().equals(nodeId));
	}

	private boolean compatibleLayoutSockets(LayoutNode fromNode, Socket from, LayoutNode toNode, Socket to) {
		Facing fromFacing = rotatedFacing(from.facing(), fromNode.rotation());
		Facing toFacing = rotatedFacing(to.facing(), toNode.rotation());
		if (opposite(fromFacing) != toFacing || from.type() != to.type())
			return false;
		if (fromFacing == Facing.UP || fromFacing == Facing.DOWN) {
			int fromX = swapsHorizontalAxes(fromNode.rotation()) ? from.openingHeight() : from.openingWidth();
			int fromZ = swapsHorizontalAxes(fromNode.rotation()) ? from.openingWidth() : from.openingHeight();
			int toX = swapsHorizontalAxes(toNode.rotation()) ? to.openingHeight() : to.openingWidth();
			int toZ = swapsHorizontalAxes(toNode.rotation()) ? to.openingWidth() : to.openingHeight();
			return fromX == toX && fromZ == toZ;
		}
		return from.openingWidth() == to.openingWidth() && from.openingHeight() == to.openingHeight();
	}

	private static boolean swapsHorizontalAxes(int rotation) {
		return Math.floorMod(rotation, 180) == 90;
	}

	private static Facing rotatedFacing(Facing facing, int rotation) {
		if (facing == Facing.UP || facing == Facing.DOWN)
			return facing;
		return Facing.values()[Math.floorMod(facing.ordinal() + Math.floorMod(rotation, 360) / 90, 4)];
	}

	private static Facing opposite(Facing facing) {
		return switch (facing) {
			case NORTH -> Facing.SOUTH;
			case EAST -> Facing.WEST;
			case SOUTH -> Facing.NORTH;
			case WEST -> Facing.EAST;
			case UP -> Facing.DOWN;
			case DOWN -> Facing.UP;
		};
	}

	private void clearPendingConnection() {
		pendingConnectionNodeId = "";
		pendingConnectionSocketId = "";
	}

	private void cycleLayoutMode() {
		if (!hasActiveDungeonDraft())
			return;
		LayoutDraft draft = model.layout();
		LayoutMode next = draft.mode() == LayoutMode.PROCEDURAL ? LayoutMode.FIXED : LayoutMode.PROCEDURAL;
		setLocalLayout(copyLayout(draft, next,
				draft.topology(), draft.minRooms(), draft.maxRooms()));
		clearPendingConnection();
		selectedLayoutSocketId = "";
		feedback(Severity.INFO, "Layout mode is now " + next.name() + ". Press Apply to save.");
		rebuildWidgets();
	}

	private void cycleTopology() {
		if (!hasActiveDungeonDraft())
			return;
		LayoutDraft draft = model.layout();
		Topology next = draft.topology() == Topology.LINEAR ? Topology.BRANCHING : Topology.LINEAR;
		int minimum = next == Topology.BRANCHING ? Math.max(4, draft.minRooms()) : draft.minRooms();
		int maximum = Math.max(minimum, next == Topology.BRANCHING ? Math.max(4, draft.maxRooms()) : draft.maxRooms());
		setLocalLayout(copyLayout(draft, draft.mode(),
				next, minimum, maximum));
		if (next == Topology.BRANCHING && draft.minRooms() < 4)
			feedback(Severity.INFO, "Branching needs at least 4 rooms; the minimum was raised automatically.");
	}

	private void adjustRoomRange(int minDelta, int maxDelta) {
		if (!hasActiveDungeonDraft())
			return;
		LayoutDraft draft = model.layout();
		int lowerBound = draft.topology() == Topology.BRANCHING ? 4 : 3;
		int min = clamp(draft.minRooms() + minDelta, lowerBound, 64);
		int max = clamp(draft.maxRooms() + maxDelta, min, 64);
		setLocalLayout(copyLayout(draft, draft.mode(), draft.topology(), min, max));
	}

	private void setLocalLayout(LayoutDraft layout) {
		model = new DungeonBuilderStudioModel(model.revision(), selectedProjectId, selectedPoolId,
				model.dungeonId(), model.loading(), model.projects(), model.pools(), layout,
				model.simulation(), model.validation(), model.notice(), model.dungeonDrafts());
		layoutDirty = true;
	}

	private void submitLayout() {
		if (!hasActiveDungeonDraft())
			return;
		controller.submit(new UpdateLayout(model.dungeonId(), model.layout()));
		feedback(Severity.INFO, "Saving layout rules...");
	}

	private void newSimulationSeed() {
		simulationSeed = System.nanoTime() ^ (long) selectedProjectId.hashCode() << 32;
		if (seedBox != null)
			seedBox.setValue(Long.toString(simulationSeed));
	}

	private void runSimulation() {
		if (layoutDirty) {
			feedback(Severity.TODO, "Press Apply on the Layout tab before running a server preview.");
			return;
		}
		try {
			simulationSeed = Long.parseLong(seedBox == null ? Long.toString(simulationSeed) : seedBox.getValue());
			controller.submit(new RunSimulation(model.dungeonId(), simulationSeed));
			feedback(Severity.INFO, "Running the canonical dungeon planner...");
		} catch (NumberFormatException exception) {
			feedback(Severity.ERROR, "Seed must be a whole number from -9223372036854775808 to 9223372036854775807.");
		}
	}

	private void validateDungeon() {
		if (layoutDirty) {
			feedback(Severity.TODO, "Press Apply on the Layout tab before validation.");
			return;
		}
		controller.submit(new ValidateDungeon(model.dungeonId()));
		feedback(Severity.INFO, "Validating the current revision...");
	}

	private void exportDungeon() {
		if (layoutDirty) {
			feedback(Severity.ERROR, "Unsaved layout changes exist. Press Apply before export.");
			return;
		}
		if (!model.validation().hasRun() || model.validation().errors() > 0) {
			feedback(Severity.ERROR, "Validate and resolve all blocking errors before export.");
			return;
		}
		controller.submit(new ExportDungeon(model.dungeonId()));
		feedback(Severity.INFO, "Exporting the validated datapack...");
	}

	private Optional<Project> selectedProject() {
		return model.project(selectedProjectId);
	}

	private boolean canUseDungeonCatalog() {
		return selectedProject().map(project -> project.kind() == ProjectKind.MODULE).orElse(false);
	}

	private boolean hasActiveDungeonDraft() {
		if (model.draft(model.dungeonId()).isPresent())
			return true;
		if (selectedProject().map(project -> project.kind() == ProjectKind.PRESET).orElse(false)) {
			feedback(Severity.INFO, "Preset exports directly. Configure it with Preset Setup on the Rooms tab.");
			return false;
		}
		feedback(Severity.TODO, "Open Dungeons and create or select a saved dungeon before editing layout rules.");
		return false;
	}

	private Optional<MobPool> selectedPool() {
		return model.pool(selectedPoolId);
	}

	private Optional<Socket> selectedSocket() {
		return selectedProject().flatMap(project -> project.sockets().stream()
				.filter(socket -> socket.id().equals(selectedSocketId)).findFirst());
	}

	private Optional<Anchor> selectedAnchor() {
		return selectedProject().flatMap(project -> project.anchors().stream()
				.filter(anchor -> anchor.id().equals(selectedAnchorId)).findFirst());
	}

	private boolean selectedAnchorIsTrigger() {
		return selectedAnchor().map(anchor -> anchor.kind() == AnchorKind.TRIGGER).orElse(false);
	}

	private Optional<SimRoom> selectedSimulationRoom() {
		return model.simulation().rooms().stream().filter(room -> room.id().equals(selectedSimRoomId)).findFirst();
	}

	private String inspectorTitle() {
		return switch (activeTab) {
			case ROOMS -> "ROOM INSPECTOR";
			case ANCHORS -> "ANCHOR INSPECTOR";
			case POOLS -> "POOL INSPECTOR";
			case LAYOUT -> "LAYOUT RULES";
			case SIMULATE -> "PREVIEW DETAILS";
			case EXPORT -> "EXPORT DETAILS";
		};
	}

	private Severity feedbackSeverity() {
		if (!localFeedback.isBlank())
			return localFeedbackSeverity;
		Notice notice = model.notice();
		if (!notice.message().isBlank())
			return notice.severity();
		return model.validation().severity();
	}

	private String feedbackText() {
		if (!localFeedback.isBlank())
			return localFeedback;
		if (!model.notice().message().isBlank())
			return model.notice().message();
		if (model.loading())
			return "Loading workspace";
		return switch (model.validation().severity()) {
			case PASS -> "Ready to export";
			case ERROR -> model.validation().errors() + " blocking errors";
			case WARNING -> model.validation().warnings() + " warnings";
			default -> "Validation not run";
		};
	}

	private void feedback(Severity severity, String message) {
		localFeedbackSeverity = severity;
		localFeedback = message == null ? "" : message;
	}

	private WorkspaceLayout workspaceLayout() {
		int tabRows = compact ? 2 : 1;
		int contextY = panelY + 21 + tabRows * TAB_HEIGHT + 3;
		Rect context = new Rect(panelX + 6, contextY, panelW - 12, 27);
		int footerH = compact ? 38 : 20;
		Rect footer = new Rect(panelX + 6, panelY + panelH - footerH - 5, panelW - 12, footerH);
		int bodyY = context.bottom() + 4;
		Rect body = new Rect(panelX + 6, bodyY, panelW - 12, Math.max(100, footer.y() - bodyY - 4));
		int leftW = compact ? Math.min(112, Math.max(94, body.w() / 3)) : 126;
		Rect left = new Rect(body.x(), body.y(), leftW, body.h());
		int mainX = left.right() + 4;
		if (compact) {
			Rect toggle = new Rect(mainX, body.y(), body.right() - mainX, 16);
			Rect main = new Rect(mainX, body.y() + 19, body.right() - mainX, body.h() - 19);
			return new WorkspaceLayout(context, body, left, main, Rect.empty(), toggle, footer);
		}
		int inspectorW = 140;
		Rect inspector = new Rect(body.right() - inspectorW, body.y(), inspectorW, body.h());
		Rect main = new Rect(mainX, body.y(), inspector.x() - mainX - 4, body.h());
		return new WorkspaceLayout(context, body, left, main, inspector, Rect.empty(), footer);
	}

	private Rect dialogRect() {
		int width = switch (activeDialog) {
			case NEW_ROOM -> Math.min(350, panelW - 20);
			case NEW_POOL -> Math.min(330, panelW - 20);
			case POOL_ENTRY -> Math.min(390, panelW - 20);
			case ANCHOR_SETUP -> Math.min(360, panelW - 20);
			case PRESET_SETUP -> Math.min(370, panelW - 20);
			case DUNGEON_CATALOG -> Math.min(390, panelW - 20);
			case NEW_DUNGEON, DELETE_DUNGEON -> Math.min(350, panelW - 20);
			case LAYOUT_SETUP -> Math.min(370, panelW - 20);
			default -> Math.min(300, panelW - 20);
		};
		int height = switch (activeDialog) {
			case NEW_ROOM -> 132;
			case NEW_POOL -> 112;
			case POOL_ENTRY -> Math.min(218, panelH - 18);
			case ANCHOR_SETUP -> 150;
			case PRESET_SETUP -> 154;
			case DUNGEON_CATALOG -> Math.min(256, panelH - 18);
			case NEW_DUNGEON -> 112;
			case DELETE_DUNGEON -> 126;
			case LAYOUT_SETUP -> Math.min(202, panelH - 18);
			default -> 100;
		};
		return new Rect(panelX + (panelW - width) / 2, panelY + (panelH - height) / 2, width, height);
	}

	private Rect dungeonCatalogListRect(Rect dialog) {
		return new Rect(dialog.x() + 10, dialog.y() + 27, dialog.w() - 20,
				Math.max(44, dialog.h() - 75));
	}

	private int maxCatalogScroll(Rect dialog) {
		return Math.max(0, model.dungeonDrafts().size() * 28 - dungeonCatalogListRect(dialog).h());
	}

	private int maxLeftScroll(Rect rect) {
		int rows;
		int rowHeight;
		switch (activeTab) {
			case POOLS -> {
				rows = model.pools().size();
				rowHeight = ROW_HEIGHT;
			}
			case SIMULATE -> {
				rows = model.simulation().rooms().size();
				rowHeight = 23;
			}
			case EXPORT -> {
				rows = model.validation().issues().size();
				rowHeight = 34;
			}
			default -> {
				rows = model.projects().size();
				rowHeight = ROW_HEIGHT;
			}
		}
		return Math.max(0, rows * rowHeight - (rect.h() - 20));
	}

	private int maxCenterScroll(Rect rect) {
		return selectedPool().map(pool -> Math.max(0, pool.entries().size() * 38 - (rect.h() - 21))).orElse(0);
	}

	private void drawBlueprintGrid(GuiGraphics graphics, Rect rect) {
		for (int x = rect.x() + 6; x < rect.right(); x += 12)
			graphics.fill(x, rect.y() + 1, x + 1, rect.bottom() - 1, GRID);
		for (int y = rect.y() + 6; y < rect.bottom(); y += 12)
			graphics.fill(rect.x() + 1, y, rect.right() - 1, y + 1, GRID);
	}

	private void drawSocket(GuiGraphics graphics, RoomMap map, Socket socket, boolean selected) {
		int x = map.worldX(socket.position().x());
		int z = map.worldZ(socket.position().z());
		int color = selected ? WARNING : socket.required() ? ACCENT : 0xFF78A7BC;
		if (socket.facing() == Facing.UP || socket.facing() == Facing.DOWN) {
			graphics.fill(x - 4, z - 4, x + 5, z + 5, 0xDD071420);
			drawOutline(graphics, x - 4, z - 4, 9, 9, color);
			graphics.drawString(font, socket.facing() == Facing.UP ? "U" : "D", x - 2, z - 4, color, false);
			return;
		}
		int dx = socket.facing().stepX();
		int dz = socket.facing().stepZ();
		graphics.fill(x - 2, z - 2, x + 3, z + 3, 0xDD071420);
		drawOutline(graphics, x - 2, z - 2, 5, 5, color);
		graphics.fill(Math.min(x, x + dx * 8), Math.min(z, z + dz * 8),
				Math.max(x, x + dx * 8) + 1, Math.max(z, z + dz * 8) + 1, color);
		int tipX = x + dx * 8;
		int tipZ = z + dz * 8;
		if (dx != 0)
			graphics.fill(tipX - dx * 2, tipZ - 2, tipX + dx + 1, tipZ + 3, color);
		else
			graphics.fill(tipX - 2, tipZ - dz * 2, tipX + 3, tipZ + dz + 1, color);
		if (socket.required())
			graphics.drawString(font, "!", x + 4, z - 5, color, false);
	}

	private void drawAnchor(GuiGraphics graphics, RoomMap map, Anchor anchor, boolean selected) {
		int x = map.worldX(anchor.position().x());
		int z = map.worldZ(anchor.position().z());
		int color = selected ? WARNING : anchorColor(anchor);
		switch (anchor.kind()) {
			case BOSS_SPAWN -> {
				graphics.fill(x, z - 4, x + 1, z + 5, color);
				graphics.fill(x - 3, z - 1, x + 4, z + 2, color);
				graphics.fill(x - 2, z - 2, x + 3, z + 3, color);
			}
			case PLAYER_START -> {
				graphics.fill(x - 4, z, x + 5, z + 1, color);
				graphics.fill(x, z - 4, x + 1, z + 5, color);
			}
			case TRIGGER -> drawOutline(graphics, x - 4, z - 4, 9, 9, color);
			case RETURN_PORTAL -> {
				drawOutline(graphics, x - 4, z - 4, 9, 9, color);
				drawOutline(graphics, x - 2, z - 2, 5, 5, color);
			}
			default -> graphics.fill(x - 3, z - 3, x + 4, z + 4, color);
		}
		graphics.drawString(font, anchor.kind().symbol(), x + 5, z - 4, color, false);
	}

	private int anchorColor(Anchor anchor) {
		return switch (anchor.kind()) {
			case UNASSIGNED, SPAWN_POINT -> WARNING;
			case MOB_SPAWN -> 0xFF6BE6A0;
			case ELITE_SPAWN -> 0xFFFFB45C;
			case BOSS_SPAWN -> ERROR;
			case PLAYER_START -> 0xFF72E5FF;
			case RETURN_PORTAL -> 0xFFB88BFF;
			case TRIGGER -> 0xFFFFD166;
			case LOOT -> 0xFFFFE37A;
			case CUSTOM -> TEXT_MAIN;
		};
	}

	private void renderNodeGraph(GuiGraphics graphics, Rect rect, List<LayoutNode> nodes,
			List<LayoutConnection> connections, int mouseX, int mouseY) {
		GraphMap map = layoutNodeDragging && layoutDragMap != null ? layoutDragMap : layoutGraphMap(rect, nodes);
		enableScissor(graphics, rect);
		for (LayoutConnection connection : connections) {
			LayoutNode from = nodes.stream().filter(node -> node.id().equals(connection.fromNodeId())).findFirst().orElse(null);
			LayoutNode to = nodes.stream().filter(node -> node.id().equals(connection.toNodeId())).findFirst().orElse(null);
			Socket fromSocket = from == null ? null : layoutSocket(from, connection.fromSocketId()).orElse(null);
			Socket toSocket = to == null ? null : layoutSocket(to, connection.toSocketId()).orElse(null);
			Point fromOffset = from == null || fromSocket == null ? null : transformedSocketOffset(from, fromSocket);
			Point toOffset = to == null || toSocket == null ? null : transformedSocketOffset(to, toSocket);
			if (from != null && to != null) {
				int fromX = fromOffset == null ? map.centerX(from.x(), from.width()) : map.x(from.x() + fromOffset.x());
				int fromZ = fromOffset == null ? map.centerZ(from.z(), from.depth()) : map.z(from.z() + fromOffset.z());
				int toX = toOffset == null ? map.centerX(to.x(), to.width()) : map.x(to.x() + toOffset.x());
				int toZ = toOffset == null ? map.centerZ(to.z(), to.depth()) : map.z(to.z() + toOffset.z());
				drawOrthogonalConnection(graphics, fromX, fromZ, toX, toZ, ACCENT_DIM);
			}
		}
		for (LayoutNode node : nodes)
			drawGraphRoom(graphics, map, node.id(), node.projectId(), node.role(), node.x(), node.z(),
					node.width(), node.depth(), node.id().equals(selectedLayoutNodeId), mouseX, mouseY);
		for (LayoutNode node : nodes) {
			Project project = model.project(node.projectId()).orElse(null);
			if (project == null)
				continue;
			for (Socket socket : project.sockets())
				drawLayoutSocket(graphics, map, node, socket, mouseX, mouseY);
		}
		graphics.disableScissor();
	}

	private void drawLayoutSocket(GuiGraphics graphics, GraphMap map, LayoutNode node, Socket socket,
			int mouseX, int mouseY) {
		Point offset = transformedSocketOffset(node, socket);
		if (offset == null)
			return;
		int x = map.x(node.x() + offset.x());
		int z = map.z(node.z() + offset.z());
		boolean selected = node.id().equals(selectedLayoutNodeId) && socket.id().equals(selectedLayoutSocketId);
		boolean pending = node.id().equals(pendingConnectionNodeId) && socket.id().equals(pendingConnectionSocketId);
		boolean used = isLayoutSocketUsed(node.id(), socket.id());
		int color = pending ? SUCCESS : selected ? WARNING : used ? DISABLED : socket.required() ? ACCENT : INFO;
		graphics.fill(x - 2, z - 2, x + 3, z + 3, color);
		Facing facing = rotatedFacing(socket.facing(), node.rotation());
		if (facing == Facing.UP || facing == Facing.DOWN) {
			graphics.drawString(font, facing == Facing.UP ? "U" : "D", x + 3, z - 4, color, false);
		} else {
			int endX = x + facing.stepX() * 5;
			int endZ = z + facing.stepZ() * 5;
			graphics.fill(Math.min(x, endX), Math.min(z, endZ), Math.max(x, endX) + 1,
					Math.max(z, endZ) + 1, color);
		}
		if (mouseX >= x - 4 && mouseX <= x + 4 && mouseY >= z - 4 && mouseY <= z + 4)
			hoverTooltip = List.of(Component.literal(node.id() + " / " + socket.id()),
					Component.literal((used ? "CONNECTED | " : "") + facing.name() + " | " + socket.type().name()));
	}

	private void renderSimulationGraph(GuiGraphics graphics, Rect rect, Simulation simulation,
			int mouseX, int mouseY) {
		List<SimRoom> rooms = simulation.rooms();
		int minX = rooms.stream().mapToInt(SimRoom::x).min().orElse(0);
		int minZ = rooms.stream().mapToInt(SimRoom::z).min().orElse(0);
		int maxX = rooms.stream().mapToInt(room -> room.x() + room.width()).max().orElse(1);
		int maxZ = rooms.stream().mapToInt(room -> room.z() + room.depth()).max().orElse(1);
		GraphMap map = GraphMap.create(rect, minX, minZ, maxX, maxZ,
				canvasZoom, canvasPanX, canvasPanY);
		enableScissor(graphics, rect);
		for (SimConnection connection : simulation.connections()) {
			SimRoom from = rooms.stream().filter(room -> room.id().equals(connection.fromRoomId())).findFirst().orElse(null);
			SimRoom to = rooms.stream().filter(room -> room.id().equals(connection.toRoomId())).findFirst().orElse(null);
			if (from != null && to != null)
				drawOrthogonalConnection(graphics, map.centerX(from.x(), from.width()), map.centerZ(from.z(), from.depth()),
						map.centerX(to.x(), to.width()), map.centerZ(to.z(), to.depth()), ACCENT);
		}
		for (SimRoom room : rooms)
			drawGraphRoom(graphics, map, room.id(), room.projectId(), room.role(), room.x(), room.z(),
					room.width(), room.depth(), room.id().equals(selectedSimRoomId), mouseX, mouseY);
		graphics.disableScissor();
	}

	private void drawGraphRoom(GuiGraphics graphics, GraphMap map, String id, String projectId,
			RoomRole role, int x, int z, int width, int depth, boolean selected, int mouseX, int mouseY) {
		Rect box = graphRoomRect(map, x, z, width, depth);
		graphics.fill(box.x(), box.y(), box.right(), box.bottom(), selected ? SELECTED : 0xD0183548);
		drawOutline(graphics, box.x(), box.y(), box.w(), box.h(), selected ? WARNING : roleColor(role));
		String label = fit(projectId, Math.max(1, box.w() - 4));
		graphics.drawString(font, label, box.x() + 2, box.y() + 2, TEXT_MAIN, false);
		if (!label.equals(projectId) && box.contains(mouseX, mouseY))
			hoverTooltip = List.of(Component.literal(projectId), Component.literal(id));
	}

	private static Rect graphRoomRect(GraphMap map, int x, int z, int width, int depth) {
		int x0 = map.x(x);
		int z0 = map.z(z);
		int x1 = Math.max(x0 + 8, map.x(x + width));
		int z1 = Math.max(z0 + 8, map.z(z + depth));
		return new Rect(x0, z0, x1 - x0, z1 - z0);
	}

	private void selectSimulationRoomAt(Rect rect, int mouseX, int mouseY) {
		List<SimRoom> rooms = model.simulation().rooms();
		if (rooms.isEmpty())
			return;
		Rect viewport = rect.inset(7, 18);
		if (!viewport.contains(mouseX, mouseY))
			return;
		int minX = rooms.stream().mapToInt(SimRoom::x).min().orElse(0);
		int minZ = rooms.stream().mapToInt(SimRoom::z).min().orElse(0);
		int maxX = rooms.stream().mapToInt(room -> room.x() + room.width()).max().orElse(1);
		int maxZ = rooms.stream().mapToInt(room -> room.z() + room.depth()).max().orElse(1);
		GraphMap map = GraphMap.create(viewport, minX, minZ, maxX, maxZ,
				canvasZoom, canvasPanX, canvasPanY);
		for (int index = rooms.size() - 1; index >= 0; index--) {
			SimRoom room = rooms.get(index);
			if (graphRoomRect(map, room.x(), room.z(), room.width(), room.depth()).contains(mouseX, mouseY)) {
				selectedSimRoomId = room.id();
				if (compact) {
					compactPane = CompactPane.INSPECTOR;
					rebuildWidgets();
				}
				return;
			}
		}
	}

	private boolean selectLayoutSocketAt(Rect rect, int mouseX, int mouseY) {
		List<LayoutNode> nodes = model.layout().nodes();
		if (nodes.isEmpty() || model.layout().mode() != LayoutMode.FIXED)
			return false;
		Rect viewport = rect.inset(7, 18);
		if (!viewport.contains(mouseX, mouseY))
			return false;
		GraphMap map = layoutGraphMap(viewport, nodes);
		LayoutNode closestNode = null;
		Socket closestSocket = null;
		double closestDistance = 36.0D;
		for (int nodeIndex = nodes.size() - 1; nodeIndex >= 0; nodeIndex--) {
			LayoutNode node = nodes.get(nodeIndex);
			Project project = model.project(node.projectId()).orElse(null);
			if (project == null)
				continue;
			for (Socket socket : project.sockets()) {
				Point offset = transformedSocketOffset(node, socket);
				if (offset == null)
					continue;
				double dx = mouseX - map.x(node.x() + offset.x());
				double dz = mouseY - map.z(node.z() + offset.z());
				double distance = dx * dx + dz * dz;
				if (distance <= closestDistance) {
					closestDistance = distance;
					closestNode = node;
					closestSocket = socket;
				}
			}
		}
		if (closestNode == null || closestSocket == null)
			return false;
		selectedLayoutNodeId = closestNode.id();
		selectedLayoutSocketId = closestSocket.id();
		selectedProjectId = closestNode.projectId();
		if (!closestNode.projectId().equals(model.selectedProjectId()))
			controller.submit(new SelectProject(closestNode.projectId()));
		boolean used = isLayoutSocketUsed(closestNode.id(), closestSocket.id());
		feedback(Severity.INFO, "Selected endpoint " + closestNode.id() + " / " + closestSocket.id()
				+ (used ? ". Press Unlink to detach it." : ". Press Link to store or connect it."));
		if (compact)
			compactPane = CompactPane.INSPECTOR;
		rebuildWidgets();
		return true;
	}

	private LayoutNode selectLayoutNodeAt(Rect rect, int mouseX, int mouseY) {
		List<LayoutNode> nodes = model.layout().nodes();
		if (nodes.isEmpty())
			return null;
		Rect viewport = rect.inset(7, 18);
		if (!viewport.contains(mouseX, mouseY))
			return null;
		GraphMap map = layoutGraphMap(viewport, nodes);
		for (int index = nodes.size() - 1; index >= 0; index--) {
			LayoutNode node = nodes.get(index);
			if (graphRoomRect(map, node.x(), node.z(), node.width(), node.depth()).contains(mouseX, mouseY)) {
				selectedLayoutNodeId = node.id();
				selectedLayoutSocketId = "";
				selectedProjectId = node.projectId();
				if (!node.projectId().equals(model.selectedProjectId()))
					controller.submit(new SelectProject(node.projectId()));
				if (compact)
					compactPane = CompactPane.INSPECTOR;
				rebuildWidgets();
				return node;
			}
		}
		return null;
	}

	private GraphMap layoutGraphMap(Rect viewport, List<LayoutNode> nodes) {
		int contentMinX = nodes.stream().mapToInt(LayoutNode::x).min().orElse(0);
		int contentMinZ = nodes.stream().mapToInt(LayoutNode::z).min().orElse(0);
		int contentMaxX = nodes.stream().mapToInt(node -> node.x() + node.width()).max().orElse(1);
		int contentMaxZ = nodes.stream().mapToInt(node -> node.z() + node.depth()).max().orElse(1);
		int minX = Math.min(-32, contentMinX - 6);
		int minZ = Math.min(-32, contentMinZ - 6);
		int maxX = Math.max(32, contentMaxX + 6);
		int maxZ = Math.max(32, contentMaxZ + 6);
		return GraphMap.create(viewport, minX, minZ, maxX, maxZ,
				canvasZoom, canvasPanX, canvasPanY);
	}

	private void drawOrthogonalConnection(GuiGraphics graphics, int x0, int y0, int x1, int y1, int color) {
		int middleX = (x0 + x1) / 2;
		graphics.fill(Math.min(x0, middleX), y0, Math.max(x0, middleX) + 1, y0 + 2, color);
		graphics.fill(middleX, Math.min(y0, y1), middleX + 2, Math.max(y0, y1) + 1, color);
		graphics.fill(Math.min(middleX, x1), y1, Math.max(middleX, x1) + 1, y1 + 2, color);
	}

	private void drawSectionTitle(GuiGraphics graphics, Rect rect, String label, int count) {
		graphics.drawString(font, label, rect.x() + 6, rect.y() + 5, INFO, false);
		String countLabel = Integer.toString(count);
		graphics.drawString(font, countLabel, rect.right() - 6 - font.width(countLabel), rect.y() + 5, TEXT_SUB, false);
	}

	private void renderEmpty(GuiGraphics graphics, Rect rect, String title, String instruction) {
		int titleY = Math.max(rect.y() + 3, rect.y() + rect.h() / 2 - 17);
		graphics.drawCenteredString(font, title, rect.x() + rect.w() / 2, titleY, TEXT_MAIN);
		drawWrappedCentered(graphics, instruction, rect, titleY + 13, TEXT_SUB, 4);
	}

	private void drawWrappedCentered(GuiGraphics graphics, String text, Rect rect, int y, int color, int maxLines) {
		List<String> lines = wrap(text, Math.max(20, rect.w() - 10));
		for (int index = 0; index < Math.min(maxLines, lines.size()); index++)
			graphics.drawCenteredString(font, lines.get(index), rect.x() + rect.w() / 2, y + index * 10, color);
	}

	private void drawWrapped(GuiGraphics graphics, String text, int x, int y, int width, int color, int maxLines) {
		List<String> lines = wrap(text, width);
		for (int index = 0; index < Math.min(maxLines, lines.size()); index++)
			graphics.drawString(font, lines.get(index), x, y + index * 10, color, false);
	}

	private List<String> wrap(String text, int width) {
		List<String> result = new ArrayList<>();
		String remaining = text == null ? "" : text.trim();
		while (!remaining.isEmpty()) {
			String line = font.plainSubstrByWidth(remaining, width);
			if (line.isEmpty())
				break;
			int split = line.length();
			if (split < remaining.length()) {
				int space = line.lastIndexOf(' ');
				if (space > 0)
					split = space;
			}
			result.add(remaining.substring(0, split).trim());
			remaining = remaining.substring(split).trim();
		}
		return result;
	}

	private void drawClipped(GuiGraphics graphics, String text, int x, int y, int width, int color,
			int mouseX, int mouseY) {
		String safe = text == null ? "" : text;
		String fitted = fit(safe, width);
		graphics.drawString(font, fitted, x, y, color, false);
		if (!fitted.equals(safe) && mouseX >= x && mouseX < x + width && mouseY >= y - 1 && mouseY < y + 10)
			hoverTooltip = List.of(Component.literal(safe));
	}

	private String fit(String text, int width) {
		if (font.width(text) <= width)
			return text;
		String suffix = "...";
		return font.plainSubstrByWidth(text, Math.max(1, width - font.width(suffix))) + suffix;
	}

	private void drawPanel(GuiGraphics graphics, Rect rect, int fill, int border) {
		graphics.fill(rect.x(), rect.y(), rect.right(), rect.bottom(), fill);
		drawOutline(graphics, rect.x(), rect.y(), rect.w(), rect.h(), border);
	}

	private static void drawOutline(GuiGraphics graphics, int x, int y, int width, int height, int color) {
		if (width <= 0 || height <= 0)
			return;
		graphics.fill(x, y, x + width, y + 1, color);
		graphics.fill(x, y + height - 1, x + width, y + height, color);
		graphics.fill(x, y, x + 1, y + height, color);
		graphics.fill(x + width - 1, y, x + width, y + height, color);
	}

	private void enableScissor(GuiGraphics graphics, Rect rect) {
		ResponsiveGuiScale.enableScissor(graphics, responsiveTransform(), rect.x(), rect.y(), rect.right(), rect.bottom());
	}

	private static int roleColor(RoomRole role) {
		return switch (role) {
			case START -> 0xFF61D9FF;
			case BOSS -> ERROR;
			case JUNCTION -> 0xFFFFD166;
			case TREASURE -> 0xFFFFE58A;
			case STAIR -> 0xFFB78BFF;
			default -> 0xFF5FE38C;
		};
	}

	private static int severityColor(Severity severity) {
		return switch (severity) {
			case PASS -> SUCCESS;
			case TODO, WARNING -> WARNING;
			case ERROR -> ERROR;
			case INFO -> INFO;
		};
	}

	private static String statusLabel(Severity severity) {
		return switch (severity) {
			case PASS -> "[PASS]";
			case TODO -> "[TODO]";
			case WARNING -> "[WARN]";
			case ERROR -> "[ERROR]";
			case INFO -> "[INFO]";
		};
	}

	private static String pointText(Point point) {
		return point.x() + ", " + point.y() + ", " + point.z();
	}

	private static String rankLabel(Set<String> ranks) {
		return ranks == null || ranks.isEmpty() || ranks.containsAll(DUNGEON_RANKS)
				? "ALL" : String.join(",", ranks);
	}

	private static String normalizedResourceId(String value) {
		return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
	}

	private static boolean validResourceId(String value) {
		return value != null && value.matches("[a-z0-9_.-]+:[a-z0-9/._-]+");
	}

	private static boolean validDungeonId(String value) {
		return validDatapackResourceId(value, 192);
	}

	private static boolean validDatapackResourceId(String value, int maxLength) {
		if (!validResourceId(value) || value.length() > maxLength)
			return false;
		int separator = value.indexOf(':');
		if (!safeFilesystemSegment(value.substring(0, separator)))
			return false;
		String path = value.substring(separator + 1);
		if (path.startsWith("/") || path.endsWith("/"))
			return false;
		for (String segment : path.split("/", -1))
			if (!safeFilesystemSegment(segment))
				return false;
		return true;
	}

	private static boolean safeFilesystemSegment(String segment) {
		if (segment == null || segment.isBlank() || segment.equals(".") || segment.equals("..")
				|| segment.endsWith("."))
			return false;
		return !segment.toLowerCase(Locale.ROOT)
				.matches("(con|prn|aux|nul|com[1-9]|lpt[1-9])(\\..*)?");
	}

	private static boolean validResourceDraft(String value) {
		return value == null || value.matches("[a-z0-9_./:-]{0,128}");
	}

	private static boolean validDungeonResourceDraft(String value) {
		return value == null || value.matches("[a-z0-9_./:-]{0,192}");
	}

	private static boolean validRoomProjectDraft(String value) {
		return value == null || value.length() <= 81
				&& value.matches("[a-z0-9_.:-]*") && value.chars().filter(character -> character == ':').count() <= 1;
	}

	private static boolean validRoomProjectId(String value) {
		if (value == null)
			return false;
		int separator = value.indexOf(':');
		if (separator <= 0 || separator != value.lastIndexOf(':'))
			return false;
		String namespace = value.substring(0, separator);
		String name = value.substring(separator + 1);
		return namespace.length() <= 32 && name.length() <= 48
				&& validProjectIdPart(namespace) && validProjectIdPart(name);
	}

	private static boolean validProjectIdPart(String value) {
		return value.matches("[a-z0-9][a-z0-9_.-]*") && !value.endsWith(".")
				&& !value.matches("(con|prn|aux|nul|com[1-9]|lpt[1-9])(\\..*)?");
	}

	private static boolean validModIdDraft(String value) {
		return value == null || value.isEmpty() || value.matches("[a-z][a-z0-9_-]{0,63}");
	}

	private static boolean validModId(String value) {
		return value != null && (value.isEmpty() || value.matches("[a-z][a-z0-9_-]{1,63}"));
	}

	private static boolean validEncounterDraft(String value) {
		return value == null || value.isEmpty() || value.matches("[a-z0-9][a-z0-9_.-]{0,63}");
	}

	private static boolean validEncounterId(String value) {
		return value != null && value.matches("[a-z0-9][a-z0-9_.-]{0,63}") && !value.endsWith(".");
	}

	private static boolean validUnsignedIntegerDraft(String value) {
		return value == null || value.matches("[0-9]{0,8}");
	}

	private static boolean validSeedText(String value) {
		return value == null || value.isEmpty() || value.equals("-") || value.matches("-?[0-9]{0,19}");
	}

	private static String preferredProjectId(DungeonBuilderStudioModel model, String requested) {
		if (requested != null && model.project(requested).isPresent())
			return requested;
		return model.projects().isEmpty() ? "" : model.projects().get(0).id();
	}

	private static String preferredPoolId(DungeonBuilderStudioModel model, String requested) {
		if (requested != null && model.pool(requested).isPresent())
			return requested;
		return model.pools().isEmpty() ? "" : model.pools().get(0).id();
	}

	private static String preferredDraftId(DungeonBuilderStudioModel model, String requested) {
		if (requested != null && model.draft(requested).isPresent())
			return requested;
		if (model.draft(model.dungeonId()).isPresent())
			return model.dungeonId();
		return model.dungeonDrafts().isEmpty() ? "" : model.dungeonDrafts().get(0).id();
	}

	private static PoolEntry copyEntryWeight(PoolEntry entry, int weight) {
		return new PoolEntry(entry.selectorKind(), entry.selectorId(), Math.max(1, weight), entry.requiredMod(),
				entry.eligibleLevel(), entry.spawnLevel(), entry.baseXp());
	}

	private static LayoutDraft copyLayout(LayoutDraft source, LayoutMode mode, Topology topology,
			int minRooms, int maxRooms) {
		return new LayoutDraft(mode, topology, minRooms, maxRooms, source.maxDepth(), source.ranks(),
				source.shellBlock(), source.shellThickness(), source.enabledProjectIds(), source.roomWeights(),
				source.nodes(), source.connections());
	}

	private static int clamp(int value, int min, int max) {
		return Math.max(min, Math.min(max, value));
	}

	private static double clamp(double value, double min, double max) {
		return Math.max(min, Math.min(max, value));
	}

	private record WorkspaceLayout(Rect context, Rect body, Rect left, Rect main,
			Rect inspector, Rect mainToggle, Rect footer) {
	}

	private record Rect(int x, int y, int w, int h) {
		private static Rect empty() {
			return new Rect(0, 0, 0, 0);
		}

		private int right() {
			return x + w;
		}

		private int bottom() {
			return y + h;
		}

		private boolean contains(double px, double py) {
			return px >= x && px < right() && py >= y && py < bottom();
		}

		private Rect inset(int horizontal, int vertical) {
			return new Rect(x + horizontal, y + vertical,
					Math.max(1, w - horizontal * 2), Math.max(1, h - vertical * 2));
		}
	}

	private record RoomMap(Bounds bounds, Rect rect, double scale, double centerX, double centerZ) {
		private static RoomMap forProject(Project project, Rect rect, double zoom, double panX, double panY) {
			Bounds bounds = project.bounds();
			double fit = Math.min((rect.w() - 12.0D) / Math.max(1, bounds.width()),
					(rect.h() - 12.0D) / Math.max(1, bounds.depth()));
			fit = Math.max(1.0D, fit) * zoom;
			return new RoomMap(bounds, rect, fit, rect.x() + rect.w() / 2.0D + panX,
					rect.y() + rect.h() / 2.0D + panY);
		}

		private int worldX(int worldX) {
			double local = worldX - bounds.min().x() + 0.5D - bounds.width() / 2.0D;
			return (int) Math.round(centerX + local * scale);
		}

		private int worldZ(int worldZ) {
			double local = worldZ - bounds.min().z() + 0.5D - bounds.depth() / 2.0D;
			return (int) Math.round(centerZ + local * scale);
		}

		private int localX(int localX) {
			double local = localX - bounds.width() / 2.0D;
			return (int) Math.round(centerX + local * scale);
		}

		private int localZ(int localZ) {
			double local = localZ - bounds.depth() / 2.0D;
			return (int) Math.round(centerZ + local * scale);
		}

		private int minScreenX() {
			return localX(0);
		}

		private int maxScreenX() {
			return localX(bounds.width());
		}

		private int minScreenZ() {
			return localZ(0);
		}

		private int maxScreenZ() {
			return localZ(bounds.depth());
		}
	}

	private record GraphMap(Rect rect, int minX, int minZ, int maxX, int maxZ, double scale,
			double offsetX, double offsetZ) {
		private static GraphMap create(Rect rect, int minX, int minZ, int maxX, int maxZ,
				double zoom, double panX, double panZ) {
			double fit = Math.max(0.2D, Math.min((rect.w() - 12.0D) / Math.max(1, maxX - minX),
					(rect.h() - 12.0D) / Math.max(1, maxZ - minZ)));
			double scale = fit * clamp(zoom, 0.5D, 4.0D);
			double offsetX = rect.x() + rect.w() / 2.0D + panX - (minX + maxX) / 2.0D * scale;
			double offsetZ = rect.y() + rect.h() / 2.0D + panZ - (minZ + maxZ) / 2.0D * scale;
			return new GraphMap(rect, minX, minZ, maxX, maxZ, scale, offsetX, offsetZ);
		}

		private int x(int worldX) {
			return (int) Math.round(offsetX + worldX * scale);
		}

		private int z(int worldZ) {
			return (int) Math.round(offsetZ + worldZ * scale);
		}

		private int centerX(int x, int width) {
			return this.x(x + width / 2);
		}

		private int centerZ(int z, int depth) {
			return this.z(z + depth / 2);
		}
	}

	private static final class StudioTabButton extends SystemButton {
		private final BooleanSupplier selected;

		private StudioTabButton(int x, int y, int width, int height, Component label,
				BooleanSupplier selected, Runnable onPress) {
			super(x, y, width, height, label, button -> onPress.run());
			this.selected = selected;
		}

		@Override
		protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
			super.renderWidget(graphics, mouseX, mouseY, partialTicks);
			if (selected.getAsBoolean()) {
				graphics.fill(getX() + 1, getY() + height - 2, getX() + width - 1, getY() + height, ACCENT);
				graphics.fill(getX() + 2, getY() + 1, getX() + width - 2, getY() + height - 2, 0x1F3FC6FF);
			}
		}
	}
}
