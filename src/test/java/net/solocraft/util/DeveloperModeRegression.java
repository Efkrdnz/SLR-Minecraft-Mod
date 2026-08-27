package net.solocraft.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

/**
 * Dependency-free source contracts for the hidden, persistent developer
 * preview and its server-authoritative WIP bypasses.
 */
public final class DeveloperModeRegression {
	private static final Path MAIN = Path.of(
			"src", "main", "java", "net", "solocraft");
	private static final String DIGEST =
			"a82fc3109f444ebbefe7290bb53f91d70ce4f9cdd33384a6ffa991ab5667c6b0";

	private DeveloperModeRegression() {
	}

	public static void main(String[] args) throws IOException {
		chatPhraseIsCancelledBeforeItsToggleIsScheduled();
		plainPhraseIsAbsentFromPackagedSources();
		flagPersistsAndSynchronizesPerPlayer();
		previewAccessUsesTheServerFlag();
	}

	private static void chatPhraseIsCancelledBeforeItsToggleIsScheduled()
			throws IOException {
		String manager = read("util", "DeveloperModeManager.java");
		expectTrue(manager.contains("ServerChatEvent")
						&& manager.contains(
								"priority = EventPriority.HIGHEST")
						&& manager.contains("event.getRawText()")
						&& manager.contains(DIGEST),
				"The exact raw chat phrase must be matched by digest at highest priority");
		int cancel = manager.indexOf("event.setCanceled(true)");
		int scheduledToggle = manager.indexOf(
				"player.server.execute(() -> toggle(player))");
		expectTrue(cancel >= 0 && scheduledToggle > cancel,
				"The secret chat must be cancelled synchronously before its toggle is scheduled");
	}

	private static void plainPhraseIsAbsentFromPackagedSources()
			throws IOException {
		String phrase = new String(new char[] {
				101, 102, 107, 114, 100, 110, 122,
				49, 49, 48, 50, 48, 53
		});
		try (Stream<Path> files = Files.walk(MAIN)) {
			boolean exposed = files.filter(path -> path.toString().endsWith(".java"))
					.anyMatch(path -> contains(path, phrase));
			expectFalse(exposed,
					"The plaintext developer phrase must not be embedded in packaged Java");
		}
	}

	private static void flagPersistsAndSynchronizesPerPlayer()
			throws IOException {
		String manager = read("util", "DeveloperModeManager.java");
		expectTrue(manager.contains("Player.PERSISTED_NBT_TAG")
						&& manager.contains("PlayerEvent.Clone")
						&& manager.contains("PlayerLoggedInEvent")
						&& manager.contains("PlayerRespawnEvent"),
				"Developer mode must survive saves, relogs, death, and respawn");
		expectTrue(manager.contains(
						"DeveloperModeStateMessage.sync(player")
						&& manager.contains("JobSkillManager.syncJobSkills(player)")
						&& manager.contains(
								"JobChangeQuestManager.requestSelectionScreen(player)"),
				"A toggle must immediately refresh client state, skills, and an open vessel screen");

		String packet = read("network", "DeveloperModeStateMessage.java");
		expectTrue(packet.contains("NetworkDirection.PLAY_TO_CLIENT")
						&& packet.contains(
								"private static volatile boolean clientEnabled"),
				"The preview flag may only be mirrored from server to client");
	}

	private static void previewAccessUsesTheServerFlag()
			throws IOException {
		String vessels = read("util", "VesselManager.java");
		String selection = read("util", "JobChangeQuestManager.java");
		String vesselPacket = read("network",
				"VesselSelectionStateMessage.java");
		String screen = read("client", "gui", "system",
				"VesselSelectionScreen.java");
		expectTrue(vessels.contains(
						"\"christopher_reed\", \"sung_il_hwan\", \"go_gunhee\"")
						&& !vessels.contains(
								"\"go_gunhee\",\n\t\t\tANTARES_IDENTITY")
						&& vessels.contains(
								"&& \"sung_il_hwan\".equals(definition.identity())")
						&& vessels.contains(
								"DeveloperModeManager.isEnabled(player)")
						&& selection.contains(
								"!VesselManager.isSelectableFor(player, definition)"),
				"Sung Il-Hwan must remain a server-enforced WIP preview while Antares is public");
		expectTrue(vesselPacket.contains("boolean developerMode")
						&& screen.contains(
								"developerMode")
						&& screen.contains(
								"VesselManager.isDeveloperPreview(definition)"),
				"The vessel screen must render the server-provided preview entitlement");

		String sung = read("util", "SungIlHwanCombatManager.java");
		String antares = read("util", "AntaresCombatManager.java");
		String shadows = read("util", "ShadowMonarchManager.java");
		String developer = read("util", "DeveloperModeManager.java");
		String jobs = read("util", "JobSkillManager.java");
		String grandMarshal = read("util",
				"GrandMarshalAbilityManager.java");
		expectTrue(sung.contains(
						"return DeveloperModeManager.isEnabled(entity)")
						&& !antares.contains(
								"DeveloperModeManager.isEnabled(entity)")
						&& jobs.contains(
								"if (job == 7")
						&& !developer.contains(
								"AntaresCombatManager.resetPlayerState(player)"),
				"Sung must remain gated without disabling public Antares combat or skills");
		expectTrue(shadows.contains(
						"public static boolean isShadowAvailableFor(Player player")
						&& shadows.contains(
								"\"iron\".equals(type)")
						&& shadows.contains(
								"DeveloperModeManager.isEnabled(player)")
						&& shadows.contains(
								"!isShadowAvailableFor(player, type)")
						&& shadows.contains(
								"dismissLockedPreviewShadows(ServerPlayer owner)")
						&& developer.contains(
								"ShadowMonarchManager.dismissLockedPreviewShadows(player)"),
				"Iron grants and summons must use the same persisted developer entitlement");
		expectTrue(grandMarshal.contains(
						"!DeveloperModeManager.isEnabled(player)")
						&& grandMarshal.contains("WIP (Work in progress)"),
				"Grand Marshal Authority must remain a nonfunctional WIP outside developer mode");
	}

	private static boolean contains(Path path, String value) {
		try {
			return Files.readString(path).replace("\r\n", "\n").contains(value);
		} catch (IOException exception) {
			throw new IllegalStateException(exception);
		}
	}

	private static String read(String... parts) throws IOException {
		Path path = MAIN;
		for (String part : parts)
			path = path.resolve(part);
		return Files.readString(path).replace("\r\n", "\n");
	}

	private static void expectTrue(boolean condition, String message) {
		if (!condition)
			throw new AssertionError(message);
	}

	private static void expectFalse(boolean condition, String message) {
		expectTrue(!condition, message);
	}
}
