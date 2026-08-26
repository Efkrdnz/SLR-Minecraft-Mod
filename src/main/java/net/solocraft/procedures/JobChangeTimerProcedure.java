package net.solocraft.procedures;

import net.solocraft.network.SololevelingModVariables;
import net.solocraft.util.JobChangeQuestManager;
import net.solocraft.util.SystemNotifications;

import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;
import net.minecraft.core.registries.BuiltInRegistries;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;

@EventBusSubscriber
public class JobChangeTimerProcedure {
	private static final int JOB_CHANGE_ACCENT = 0xFF7A5CFF;
	private static final int RIDDLE_DURATION = 56;
	private static final int RESULT_DURATION = 90;

	@SubscribeEvent
	public static void onPlayerTick(PlayerTickEvent.Post event) {
		if (true && event.getEntity() instanceof ServerPlayer player)
			execute(player.level(), player.getX(), player.getY(), player.getZ(), player);
	}

	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		if (!(entity instanceof ServerPlayer player) || !JobChangeQuestManager.isShadowPresentation(player))
			return;
		SololevelingModVariables.PlayerVariables data = player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
				.orElse(new SololevelingModVariables.PlayerVariables());
		if (data.JobChange_timer > 0 && data.JobChange_timer < 9 && world.getLevelData().getGameTime() % 60 == 0) {
			int next = (int) data.JobChange_timer + 1;
			player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
				capability.JobChange_timer = next;
				capability.syncPlayerVariables(player);
			});
			playPanelSound(player, "sololeveling:panelopen");
			switch (next) {
				case 2 -> show(player, systemTitle("JOB CHANGE"), systemUnder("Wherever the player goes, the angel of death follows..."), RIDDLE_DURATION);
				case 3 -> show(player, systemTitle("JOB CHANGE"), systemUnder("Every path the player takes is littered with corpses, and the stench of blood remains."), RIDDLE_DURATION);
				case 4 -> show(player, systemTitle("JOB CHANGE"), systemUnder("The player craves great power and blazed a trail without relying on others."), RIDDLE_DURATION);
				case 5 -> show(player, systemTitle("JOB CHANGE"), systemUnder("That thirst for strength calls forth the ghosts that roam the valley of death."), RIDDLE_DURATION);
				case 6 -> show(player, systemTitle("JOB CHANGE"), systemUnder("Those ghosts will become a shadow army, obeying the player and making way for no one else."), RIDDLE_DURATION);
				case 7 -> show(player, systemTitle("JOB ASSIGNED"), Component.literal("[Necromancer]").withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD), RESULT_DURATION);
				case 8 -> show(player, systemTitle("ADVANCEMENT"), systemUnder("The advancement points acquired have opened the path to an exceptional class."), RIDDLE_DURATION);
				default -> {
				}
			}
			return;
		}

		if (data.JobChange_timer == 9) {
			JobChangeCleanupProcedure.execute(world, x, y, z);
			Component evolution = Component.empty()
					.append(Component.literal("[Necromancer]").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD))
					.append(Component.literal("\nV\n").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD))
					.append(Component.literal("[Shadow Monarch]").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD));
			show(player,
					Component.literal("CLASS EVOLUTION").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD),
					evolution,
					RESULT_DURATION);
			JobChangeQuestManager.finish(player);
			playPanelSound(player, "sololeveling:panelclose");
		}
	}

	private static void show(ServerPlayer player, Component title, Component under, int durationTicks) {
		SystemNotifications.showTitleUnder(player, JOB_CHANGE_ACCENT, durationTicks, title, under);
	}

	private static Component systemTitle(String text) {
		return Component.literal(text).withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD);
	}

	private static Component systemUnder(String text) {
		return Component.literal(text).withStyle(ChatFormatting.GRAY);
	}

	private static void playPanelSound(ServerPlayer player, String soundId) {
		if (BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse(soundId)) != null)
			player.serverLevel().playSound(null, BlockPos.containing(player.position()),
					BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse(soundId)), SoundSource.NEUTRAL, 1.0F, 1.0F);
	}
}
