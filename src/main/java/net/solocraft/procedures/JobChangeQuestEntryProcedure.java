package net.solocraft.procedures;

import net.solocraft.SololevelingMod;
import net.solocraft.network.SololevelingModVariables;
import net.solocraft.util.JobChangeQuestManager;

import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.network.protocol.game.ClientboundLevelEventPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerAbilitiesPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;

public class JobChangeQuestEntryProcedure {
	private static final double PLAYER_PORTAL_ENTRY_X_OFFSET = 3.0D;
	private static final ResourceKey<Level> IGRIS_DIMENSION = ResourceKey.create(Registries.DIMENSION, new ResourceLocation("sololeveling:dungeon_dimension_igris"));

	public static void execute(LevelAccessor world, Entity entity) {
		if (!(entity instanceof ServerPlayer player) || world == null)
			return;
		if (!JobChangeQuestManager.isVisible(player)) {
			player.displayClientMessage(Component.literal("\u00A75No active Job Change Quest."), true);
			return;
		}
		if (player.level().dimension() == IGRIS_DIMENSION) {
			player.displayClientMessage(Component.literal("\u00A75Job Change Quest is already active."), true);
			return;
		}
		saveEntryState(player);
		player.setNoGravity(true);
		ResourceKey<Level> destinationType = IGRIS_DIMENSION;
		ServerLevel nextLevel = player.server.getLevel(destinationType);
		if (nextLevel == null)
			return;
		player.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.WIN_GAME, 0));
		player.teleportTo(nextLevel, player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());
		player.connection.send(new ClientboundPlayerAbilitiesPacket(player.getAbilities()));
		for (MobEffectInstance effect : player.getActiveEffects())
			player.connection.send(new ClientboundUpdateMobEffectPacket(player.getId(), effect));
		player.connection.send(new ClientboundLevelEventPacket(1032, BlockPos.ZERO, 0, false));
		SololevelingMod.queueServerWork(70, () -> {
			if (!player.isAlive() || player.level().dimension() != IGRIS_DIMENSION)
				return;
			SololevelingModVariables.PlayerVariables vars = player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables());
			player.connection.teleport(vars.randplayerx + PLAYER_PORTAL_ENTRY_X_OFFSET, vars.randplayery, vars.randplayerz, player.getYRot(), player.getXRot());
			SololevelingMod.queueServerWork(10, () -> {
				if (player.isAlive() && player.level().dimension() == IGRIS_DIMENSION)
					spawnIgrisDungeon(player);
			});
		});
	}

	private static void saveEntryState(ServerPlayer player) {
		player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
			capability.DunX = player.getX();
			capability.DunY = player.getY();
			capability.DunZ = player.getZ();
			capability.randplayerx = Mth.nextInt(RandomSource.create(), -29999999, 29999999);
			capability.randplayery = Mth.nextInt(RandomSource.create(), 60, 120);
			capability.randplayerz = Mth.nextInt(RandomSource.create(), -29999999, 29999999);
			capability.instancecomplete = false;
			capability.BossKilled = false;
			capability.tpd = false;
			capability.jobtimer = 0;
			capability.JobChange_timer = 0;
			capability.syncPlayerVariables(player);
		});
		player.getPersistentData().putBoolean("slr_job_change_dungeon", true);
	}

	private static void spawnIgrisDungeon(ServerPlayer player) {
		player.getServer().getCommands().performPrefixedCommand(
				new CommandSourceStack(CommandSource.NULL, player.position(), player.getRotationVector(), player.serverLevel(), 4, player.getName().getString(), player.getDisplayName(), player.getServer(), player),
				"execute in sololeveling:dungeon_dimension_igris as @s at @s unless entity @e[type=sololeveling:portal_12,distance=..100] run spawnigris");
	}
}
