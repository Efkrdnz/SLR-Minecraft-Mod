package net.solocraft.procedures;

import net.solocraft.network.SololevelingModVariables;
import net.solocraft.init.SololevelingModGameRules;
import net.solocraft.entity.Portal1Entity;
import net.solocraft.dungeon.DungeonTheme;
import net.solocraft.dungeon.ProceduralDungeonRank;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.Entity;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Mth;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.network.chat.Component;
import net.minecraft.core.registries.Registries;

public class PortalSpawnProcedure {
	public static void execute(LevelAccessor world, Entity entity) {
		if (entity == null)
			return;
		if ((entity.level().dimension()) == (ResourceKey.create(Registries.DIMENSION, new ResourceLocation("sololeveling:survival_dimension")))) {
			if (!entity.level().isClientSide())
				entity.discard();
		}
		if ((entity.level().dimension()) == Level.OVERWORLD) {
			if (world.getLevelData().getGameRules().getBoolean(SololevelingModGameRules.SOLO_GATE_NOTIFICATION)) {
				if (!world.isClientSide() && world.getServer() != null)
					world.getServer().getPlayerList().broadcastSystemMessage(Component.literal(("\u00A7c" + "A portal appeared at X: \"" + "\u00A7f" + Math.round(entity.getX()) + "\" Y:\" " + "\u00A7f" + Math.round(entity.getY()) + "\" Z:\" "
							+ "\u00A7f" + Math.round(entity.getZ()) + "\u00A7c" + " \"It will Disappear in 1 DAY!")), false);
			}
			SololevelingModVariables.MapVariables.get(world).gatetimer = 0;
			SololevelingModVariables.MapVariables.get(world).syncData(world);
			entity.getPersistentData().putDouble("PortalLife", 0);
			RandomSource random = RandomSource.create();
			entity.getPersistentData().putDouble("tpx", (Mth.nextInt(random, -299999, 299999)));
			entity.getPersistentData().putDouble("tpy", (Mth.nextInt(random, 60, 120)));
			entity.getPersistentData().putDouble("tpz", (Mth.nextInt(random, -299999, 299999)));
			if (entity instanceof Portal1Entity && !entity.getPersistentData().contains("slr_procedural_gate")) {
				ProceduralDungeonRank[] ranks = ProceduralDungeonRank.values();
				DungeonTheme[] themes = DungeonTheme.values();
				ProceduralDungeonRank rank = ranks[Mth.nextInt(random, 0, ranks.length - 1)];
				DungeonTheme theme = themes[Mth.nextInt(random, 0, themes.length - 1)];
				entity.getPersistentData().putBoolean("slr_procedural_gate", true);
				entity.getPersistentData().putBoolean("slr_procedural_red_gate", false);
				entity.getPersistentData().putString("slr_procedural_rank", rank.name());
				entity.getPersistentData().putString("slr_procedural_theme", theme.name());
				entity.getPersistentData().putInt("slr_procedural_complexity", randomComplexity(rank, random));
			}
		} else {
			if (!entity.level().isClientSide())
				entity.discard();
		}
	}

	private static int randomComplexity(ProceduralDungeonRank rank, RandomSource random) {
		return switch (rank) {
			case E -> Mth.nextInt(random, 1, 3);
			case D -> Mth.nextInt(random, 2, 4);
			case C -> Mth.nextInt(random, 3, 6);
			case B -> Mth.nextInt(random, 5, 7);
			case A -> Mth.nextInt(random, 7, 9);
			case S -> Mth.nextInt(random, 8, 10);
		};
	}
}
