package net.solocraft.world.dimension.rift;

import net.solocraft.SololevelingMod;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RegisterDimensionSpecialEffectsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

@Mod.EventBusSubscriber(modid = SololevelingMod.MODID)
public final class DimensionalRiftDimension {
	public static final ResourceLocation ID = new ResourceLocation(SololevelingMod.MODID, "dimensional_rift");
	public static final ResourceKey<Level> LEVEL_KEY = ResourceKey.create(Registries.DIMENSION, ID);

	private DimensionalRiftDimension() {
	}

	@SubscribeEvent
	public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
		if (!LEVEL_KEY.equals(event.getTo()) || !(event.getEntity() instanceof ServerPlayer player)
				|| !(player.level() instanceof ServerLevel level))
			return;
		DimensionalRiftEntry.ensurePlatform(level);
		if (RiftGeometry.distance(player.getX(), player.getZ()) > RiftGeometry.DEFAULT_PLAYABLE_RADIUS
				|| player.getY() < level.getMinBuildHeight())
			DimensionalRiftEntry.teleportToCenter(player, level);
	}

	@Mod.EventBusSubscriber(modid = SololevelingMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
	public static final class Effects {
		private Effects() {
		}

		@SubscribeEvent
		@OnlyIn(Dist.CLIENT)
		public static void registerDimensionSpecialEffects(RegisterDimensionSpecialEffectsEvent event) {
			DimensionSpecialEffects effects = new DimensionSpecialEffects(Float.NaN, true,
					DimensionSpecialEffects.SkyType.END, false, false) {
				@Override
				public Vec3 getBrightnessDependentFogColor(Vec3 color, float sunHeight) {
					return color.multiply(0.48D, 0.38D, 0.58D);
				}

				@Override
				public boolean isFoggyAt(int x, int y) {
					return false;
				}
			};
			event.register(ID, effects);
		}
	}
}
