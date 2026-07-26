package net.solocraft.procedures;

import net.solocraft.network.SololevelingModVariables;
import net.solocraft.entity.Portal12Entity;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.event.TickEvent;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.entity.Entity;
import net.minecraft.tags.TagKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.BlockPos;

import javax.annotation.Nullable;

@Mod.EventBusSubscriber
public class DungeonDetectionProcedure {
	private static final TagKey<Biome> DUNGEON_S = biomeTag("duns");
	private static final TagKey<Biome> DUNGEON_A = biomeTag("duna");
	private static final TagKey<Biome> DUNGEON_B = biomeTag("dunb");
	private static final TagKey<Biome> DUNGEON_C = biomeTag("dunc");
	private static final TagKey<Biome> DUNGEON_D = biomeTag("dund");

	@SubscribeEvent
	public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
		if (event.phase == TickEvent.Phase.END) {
			execute(event, event.player.level(), event.player.getX(), event.player.getY(), event.player.getZ(), event.player);
		}
	}

	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		execute(null, world, x, y, z, entity);
	}

	private static void execute(@Nullable Event event, LevelAccessor world, double x, double y, double z, Entity entity) {
		if (entity == null)
			return;
		Holder<Biome> biome = world.getBiome(BlockPos.containing(x, y, z));
		if (!isDungeonBiome(biome))
			return;

		entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
			if (capability.tpd)
				return;
			AABB nearby = AABB.ofSize(entity.position(), 6.0D, 6.0D, 6.0D);
			boolean reachedPortal = !world.getEntitiesOfClass(Portal12Entity.class, nearby,
					portal -> portal.distanceToSqr(entity) <= 9.0D).isEmpty();
			if (!reachedPortal)
				return;
			capability.tpd = true;
			capability.syncPlayerVariables(entity);
			entity.setNoGravity(false);
		});
	}

	private static boolean isDungeonBiome(Holder<Biome> biome) {
		return biome.is(DUNGEON_S) || biome.is(DUNGEON_A) || biome.is(DUNGEON_B)
				|| biome.is(DUNGEON_C) || biome.is(DUNGEON_D);
	}

	private static TagKey<Biome> biomeTag(String path) {
		return TagKey.create(Registries.BIOME, new ResourceLocation("minecraft", path));
	}
}
