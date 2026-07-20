package net.solocraft.world.dimension.rift;

import net.solocraft.SololevelingMod;
import net.solocraft.dungeon.runtime.DungeonLevelHelper;
import net.solocraft.dungeon.runtime.DungeonMobLevelAdapter;

import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;

/** Installs an outward radial level before legacy join-time scaling executes. */
@Mod.EventBusSubscriber(modid = SololevelingMod.MODID)
public final class DimensionalRiftMobScaling {
	public static final String SCALED_TAG = "slr_rift_radial_scaled";
	public static final String SPAWN_DISTANCE_TAG = "slr_rift_spawn_distance";
	public static final String PROGRESSION_TIER_TAG = "slr_rift_progression_tier";
	private static final TagKey<EntityType<?>> SCALABLE = TagKey.create(Registries.ENTITY_TYPE,
			new ResourceLocation(SololevelingMod.MODID, "rift_scalable"));

	private DimensionalRiftMobScaling() {
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void onEntityJoin(EntityJoinLevelEvent event) {
		if (event.getLevel().isClientSide() || event.loadedFromDisk()
				|| !DimensionalRiftDimension.LEVEL_KEY.equals(event.getLevel().dimension())
				|| !(event.getEntity() instanceof Mob mob) || !mob.getType().is(SCALABLE))
			return;

		CompoundTag data = mob.getPersistentData();
		if (data.getBoolean(SCALED_TAG)
				|| data.getBoolean(DungeonMobLevelAdapter.RUNTIME_SPAWN_TAG)
				|| DungeonLevelHelper.levelOf(mob) > 0.0D)
			return;

		double distance = RiftGeometry.distance(mob.getX(), mob.getZ());
		int level = RiftGeometry.levelForDistance(distance);
		level = Math.max(5, Math.min(100, level + mob.getRandom().nextInt(7) - 3));
		DungeonMobLevelAdapter.ScalingResult result = DungeonMobLevelAdapter.applyGenericScaling(
				mob, level, DungeonMobLevelAdapter.MobRole.NORMAL);
		if (!result.succeeded())
			return;

		data.putBoolean(SCALED_TAG, true);
		data.putDouble(SPAWN_DISTANCE_TAG, distance);
		data.putInt(PROGRESSION_TIER_TAG, Math.min(5, Math.max(1, 1 + (level - 1) / 20)));
	}
}
