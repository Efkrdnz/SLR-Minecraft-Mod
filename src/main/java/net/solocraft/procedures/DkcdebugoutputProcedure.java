package net.solocraft.procedures;

import net.solocraft.network.SololevelingModVariables;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Entity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.registries.Registries;

public class DkcdebugoutputProcedure {
	public static void execute(Entity entity) {
		if (entity == null)
			return;
		Player player = (Player) entity;
		CompoundTag data = player.getPersistentData();
		if (player != null && !player.level().isClientSide()) {
			player.displayClientMessage(Component.literal("§6======================"), false);
			// Floor Cleared
			player.displayClientMessage(
					Component.literal(
							"§eFloor Cleared: §6" + new java.text.DecimalFormat("##").format((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).dkc_cleared)),
					false);
			// Floor Unlocked
			player.displayClientMessage(Component.literal("§eFloor Unlocked: §6" + ((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).dkc_started
					? new java.text.DecimalFormat("##").format(Math.min(20, Math.max((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).dkc_cleared + 1, 0)))
					: 0)), false);
			// Current Floor
			int currentFloor = 0;
			if ((entity.level().dimension()) == (ResourceKey.create(Registries.DIMENSION, new ResourceLocation("sololeveling:dungeon_dimension_dkc")))) {
				currentFloor = DKCFloorDetectorProcedure.getCurrentFloor(entity);
			}
			player.displayClientMessage(Component.literal("§eCurrent Floor: §6" + (currentFloor > 0 ? currentFloor : "None")), false);
			// Kill Progress (only show if on demon floor)
			if (currentFloor >= 2 && currentFloor <= 19 && currentFloor != 1 && currentFloor != 20) {
				double killed = data.getDouble("dkc_floor_" + currentFloor + "_killed");
				double required = data.getDouble("dkc_floor_" + currentFloor + "_required");
				double remaining = Math.max(0, required - killed);
				player.displayClientMessage(Component.literal("§7---"), false);
				player.displayClientMessage(Component.literal("§eKills: §a" + (int) killed + " §7/ §c" + (int) required), false);
				player.displayClientMessage(Component.literal("§eRemaining: §c" + (int) remaining), false);
				// Progress percentage
				double percentage = required > 0 ? (killed / required) * 100 : 0;
				player.displayClientMessage(Component.literal("§eProgress: §6" + new java.text.DecimalFormat("##.#").format(percentage) + "%"), false);
			} else if (currentFloor == 1 || currentFloor == 10 || currentFloor == 20) {
				player.displayClientMessage(Component.literal("§7---"), false);
				player.displayClientMessage(Component.literal("§4§lBoss Floor"), false);
			}
			player.displayClientMessage(Component.literal("§6======================"), false);
		}
	}
}
