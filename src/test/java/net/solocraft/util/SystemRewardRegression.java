package net.solocraft.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Source-level regression checks for server-authoritative System rewards. */
public final class SystemRewardRegression {
	private SystemRewardRegression() {
	}

	public static void main(String[] args) throws Exception {
		bossRewardsResolveIndirectKillCreditAndPreserveInbox();
		claimsStayServerAuthoritativeAndDoNotDiscardFailures();
		undeadKnightUsesAValidBowForArrows();
		instanceDungeonCompletionRetiresGettingStrongerQuest();
		System.out.println("System reward regression checks passed.");
	}

	private static void undeadKnightUsesAValidBowForArrows() throws IOException {
		String knight = read("entity", "DKnight1Entity.java");
		expect(knight.contains("new ItemStack(Items.ARROW), new ItemStack(Items.BOW)"),
				"Undead Knight arrows must use a valid bow stack in 1.21");
		expect(!knight.contains("new ItemStack(Items.ARROW), ItemStack.EMPTY"),
				"Undead Knight must not construct arrows with an empty weapon stack");
	}

	private static void bossRewardsResolveIndirectKillCreditAndPreserveInbox() throws IOException {
		String source = read("procedures", "RewardGainAdvProcedure.java");
		expect(source.contains("event.getSource().getDirectEntity()"),
				"Boss rewards must resolve direct projectile/ability sources");
		expect(source.contains("killCredit.getKillCredit()"),
				"Boss rewards must fall back to the victim's recorded kill credit");
		expect(source.contains("ShadowMonarchManager.getShadowOwnerUUID"),
				"Boss rewards must credit player-owned shadows");
		expect(source.contains("RewardManager.appendReward(player, entry)"),
				"Boss rewards must append instead of overwriting the inbox");
		expect(source.contains("PAID_TAG_PREFIX"),
				"Boss rewards must have a persistence-backed duplicate guard");
	}

	private static void claimsStayServerAuthoritativeAndDoNotDiscardFailures() throws IOException {
		String screen = readClient("gui", "RewardPanelScreen.java");
		String packet = read("network", "RewardPanelButtonMessage.java");
		String rewards = read("util", "RewardManager.java");
		expect(!screen.contains("RewardPanelButtonMessage.handleButtonAction"),
				"Reward screen must not execute claims on the client");
		expect(packet.contains("NetworkDirection.PLAY_TO_SERVER")
					&& packet.contains("entity instanceof ServerPlayer player")
					&& packet.contains("if (buttonID >= 100)")
					&& packet.contains("RewardCollectButtonProcedure.execute(player, buttonID - 99)")
					&& packet.contains("player.containerMenu instanceof net.solocraft.world.inventory.RewardPanelMenu"),
				"Modern System reward claims must reach the server without the legacy menu, while legacy buttons remain guarded");
		expect(rewards.contains("else if (!RewardCollectProcedure.execute(entity, reward))\n\t\t\treturn false;"),
				"A failed reward delivery must remain in the inbox");
	}

	private static void instanceDungeonCompletionRetiresGettingStrongerQuest()
			throws IOException {
		String access = read("util", "InstanceDungeonKeyAccess.java");
		expect(access.contains("clearCompletedGettingStrongerQuest(player);")
					&& access.contains("variables.MainQuest = \"\";")
					&& access.contains("variables.QuestProgression = 0.0D;"),
				"Completing the one-time instance dungeon must retire its stale Getting Stronger objective");
	}

	private static String read(String directory, String name) throws IOException {
		return Files.readString(Path.of("src", "main", "java", "net", "solocraft", directory, name)).replace("\r\n", "\n");
	}

	private static String readClient(String directory, String name) throws IOException {
		return Files.readString(Path.of("src", "main", "java", "net", "solocraft", "client", directory, name)).replace("\r\n", "\n");
	}

	private static void expect(boolean condition, String message) {
		if (!condition)
			throw new IllegalStateException(message);
	}
}
