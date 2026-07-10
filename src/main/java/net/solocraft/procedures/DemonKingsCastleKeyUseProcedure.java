package net.solocraft.procedures;

import net.solocraft.network.SololevelingModVariables;
import net.solocraft.util.DkcQuestManager;

import net.minecraft.core.registries.Registries;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;

public class DemonKingsCastleKeyUseProcedure {
	private static final ResourceKey<Level> DKC_DIMENSION = ResourceKey.create(Registries.DIMENSION, new ResourceLocation("sololeveling", "dungeon_dimension_dkc"));

	public static void execute(LevelAccessor world, Entity entity, ItemStack stack) {
		if (!(entity instanceof ServerPlayer player) || player.server == null)
			return;
		DkcQuestManager.unlock(player);
		if (player.level().dimension().equals(DKC_DIMENSION)) {
			DKCPathTeleportProcedure.returnToSavedOverworld(player);
			return;
		}
		if (!player.level().dimension().equals(Level.OVERWORLD)) {
			player.displayClientMessage(Component.literal("\u00A74The Demon King's Castle Key only answers in the Overworld."), true);
			return;
		}
		if (!DKCCombatTrackerProcedure.canEnterCastle(player)) {
			DKCCombatTrackerProcedure.sendCombatBlockedMessage(player);
			return;
		}
		PointSetProcedure.execute(player);
		SololevelingModVariables.PlayerVariables vars = player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables());
		if (vars.dkc_cleared >= 20) {
			player.displayClientMessage(Component.literal("\u00A75The Demon King's Castle is already conquered. Its gates stay silent."), true);
			return;
		}
		if (vars.dkc_started || vars.dkc_cleared > 0) {
			if (!vars.dkc_started) {
				player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
					capability.dkc_started = true;
					capability.syncPlayerVariables(player);
				});
			}
			player.displayClientMessage(Component.literal("\u00A75The castle has already accepted your blood."), true);
			DKCPathTeleportProcedure.execute(player, 1);
			return;
		}
		ServerLevel dkcLevel = player.server.getLevel(DKC_DIMENSION);
		if (dkcLevel == null) {
			player.displayClientMessage(Component.literal("\u00A74The Demon King's Castle refuses to manifest."), true);
			return;
		}
		FloorCreateNewProcedure.execute(dkcLevel, player, "cerberus");
		player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
			capability.dkc_started = true;
			capability.dkc_cleared = Math.max(0, capability.dkc_cleared);
			capability.syncPlayerVariables(player);
		});
		player.getPersistentData().putDouble("dkc_current_floor", 0);
		player.getPersistentData().putBoolean("dkc_floor_just_changed", true);
		if (world instanceof Level level) {
			level.playSound(null, player.blockPosition(), SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 0.85F, 0.55F);
			level.playSound(null, player.blockPosition(), SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.PLAYERS, 0.9F, 0.65F);
		}
		player.displayClientMessage(Component.literal("\u00A74\u00A7lThe first seal cracks open. \u00A75Floor 1 awaits."), false);
		player.displayClientMessage(Component.literal("\u00A78The Demon King's Castle has marked a path for you."), true);
		player.serverLevel().sendParticles(ParticleTypes.SOUL_FIRE_FLAME, player.getX(), player.getY() + 1.0D, player.getZ(), 42, 0.75D, 0.75D, 0.75D, 0.035D);
		player.serverLevel().sendParticles(ParticleTypes.LARGE_SMOKE, player.getX(), player.getY() + 0.8D, player.getZ(), 24, 0.55D, 0.45D, 0.55D, 0.02D);
		DKCPathTeleportProcedure.execute(player, 1);
	}
}
