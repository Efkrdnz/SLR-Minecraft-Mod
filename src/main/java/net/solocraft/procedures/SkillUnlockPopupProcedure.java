package net.solocraft.procedures;

import net.solocraft.network.SololevelingModVariables;
import net.solocraft.util.SystemNotifications;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.LinkedHashSet;
import java.util.Set;

@Mod.EventBusSubscriber
public class SkillUnlockPopupProcedure {
	private static final String KEY_READY = "sl_skill_unlock_popup_ready";
	private static final String KEY_PLIST = "sl_skill_unlock_popup_plist";
	private static final int ACCENT = 0xFF8A4DFF;

	@SubscribeEvent
	public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
		if (event.phase != TickEvent.Phase.END)
			return;
		if (!(event.player instanceof ServerPlayer player))
			return;
		player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
				.ifPresent(capability -> checkForNewSkills(player, capability.Plist == null ? "" : capability.Plist));
	}

	private static void checkForNewSkills(ServerPlayer player, String currentPlist) {
		CompoundTag data = player.getPersistentData();
		Set<String> currentSkills = parseSkills(currentPlist);
		String normalizedCurrent = normalizeSkills(currentSkills);
		if (!data.getBoolean(KEY_READY)) {
			data.putBoolean(KEY_READY, true);
			data.putString(KEY_PLIST, normalizedCurrent);
			return;
		}

		String previousPlist = data.getString(KEY_PLIST);
		Set<String> previousSkills = parseSkills(previousPlist);
		String normalizedPrevious = normalizeSkills(previousSkills);
		if (normalizedCurrent.equals(normalizedPrevious)) {
			if (!previousPlist.equals(normalizedCurrent))
				data.putString(KEY_PLIST, normalizedCurrent);
			return;
		}

		for (String skill : currentSkills) {
			if (!previousSkills.contains(skill))
				showSkillPopup(player, skill);
		}
		data.putString(KEY_PLIST, normalizedCurrent);
	}

	private static Set<String> parseSkills(String plist) {
		Set<String> skills = new LinkedHashSet<>();
		for (String entry : plist.split(",")) {
			String skill = entry.trim();
			if (!isPlaceholderSkill(skill))
				skills.add(skill);
		}
		return skills;
	}

	private static String normalizeSkills(Set<String> skills) {
		return String.join(",", skills);
	}

	private static boolean isPlaceholderSkill(String skill) {
		return skill.isEmpty() || ".".equals(skill) || "\"\"".equals(skill) || "empty".equalsIgnoreCase(skill) || "null".equalsIgnoreCase(skill);
	}

	private static void showSkillPopup(ServerPlayer player, String skill) {
		SystemNotifications.showTitleUnder(player, ACCENT, 90,
				Component.literal("NEW SKILL").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD),
				Component.literal(skill).withStyle(ChatFormatting.AQUA));
	}
}
