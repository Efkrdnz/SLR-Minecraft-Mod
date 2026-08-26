package net.solocraft.procedures;

import net.solocraft.network.SololevelingModVariables;

import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.Event;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.entity.Entity;
import net.minecraft.tags.TagKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.BlockPos;

import javax.annotation.Nullable;

@EventBusSubscriber
public class DungeonRankingProcedure {
	private static final TagKey<Biome> DUNGEON_E = biomeTag("dune");
	private static final TagKey<Biome> DUNGEON_D = biomeTag("dund");
	private static final TagKey<Biome> DUNGEON_C = biomeTag("dunc");
	private static final TagKey<Biome> DUNGEON_B = biomeTag("dunb");
	private static final TagKey<Biome> DUNGEON_A = biomeTag("duna");
	private static final TagKey<Biome> DUNGEON_S = biomeTag("duns");

	@SubscribeEvent
	public static void onPlayerTick(PlayerTickEvent.Post event) {
		if (true) {
			execute(event, event.getEntity().level(), event.getEntity().getX(), event.getEntity().getY(), event.getEntity().getZ(), event.getEntity());
		}
	}

	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		execute(null, world, x, y, z, entity);
	}

	private static void execute(@Nullable Event event, LevelAccessor world, double x, double y, double z, Entity entity) {
		if (entity == null)
			return;
		if (world.getLevelData().getGameTime() % 50 != 0)
			return;

		int rank = dungeonRank(world.getBiome(BlockPos.containing(x, y, z)));
		entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
			if (capability.DunRank == rank)
				return;
			capability.DunRank = rank;
			capability.syncPlayerVariables(entity);
		});
	}

	private static int dungeonRank(Holder<Biome> biome) {
		if (biome.is(DUNGEON_E))
			return 1;
		if (biome.is(DUNGEON_D))
			return 2;
		if (biome.is(DUNGEON_C))
			return 3;
		if (biome.is(DUNGEON_B))
			return 4;
		if (biome.is(DUNGEON_A))
			return 5;
		return biome.is(DUNGEON_S) ? 6 : 0;
	}

	private static TagKey<Biome> biomeTag(String path) {
		return TagKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath("minecraft", path));
	}
}
