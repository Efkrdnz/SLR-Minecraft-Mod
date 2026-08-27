
package net.solocraft.world.features.treedecorators;

import net.solocraft.SololevelingMod;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;

import net.minecraft.world.level.levelgen.feature.treedecorators.TrunkVineDecorator;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecoratorType;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecorator;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

import com.mojang.serialization.MapCodec;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD)
public class DungeonBiomeSnowTrunkDecorator extends TrunkVineDecorator {
	public static final MapCodec<DungeonBiomeSnowTrunkDecorator> CODEC = MapCodec.unit(DungeonBiomeSnowTrunkDecorator::new);
	public static final TreeDecoratorType<DungeonBiomeSnowTrunkDecorator> DECORATOR_TYPE = new TreeDecoratorType<>(CODEC);

	@SubscribeEvent
	public static void registerPointOfInterest(RegisterEvent event) {
		event.register(Registries.TREE_DECORATOR_TYPE, registerHelper -> registerHelper.register(
				ResourceLocation.fromNamespaceAndPath(SololevelingMod.MODID, "dungeon_biome_snow_tree_trunk_decorator"), DECORATOR_TYPE));
	}

	@Override
	protected TreeDecoratorType<?> type() {
		return DECORATOR_TYPE;
	}

	@Override
	public void place(TreeDecorator.Context context) {
		context.logs().forEach(blockpos -> {
			if (context.random().nextInt(3) > 0) {
				BlockPos pos = blockpos.west();
				if (context.isAir(pos)) {
					context.setBlock(pos, Blocks.SNOW_BLOCK.defaultBlockState());
				}
			}
			if (context.random().nextInt(3) > 0) {
				BlockPos pos = blockpos.east();
				if (context.isAir(pos)) {
					context.setBlock(pos, Blocks.SNOW_BLOCK.defaultBlockState());
				}
			}
			if (context.random().nextInt(3) > 0) {
				BlockPos pos = blockpos.north();
				if (context.isAir(pos)) {
					context.setBlock(pos, Blocks.SNOW_BLOCK.defaultBlockState());
				}
			}
			if (context.random().nextInt(3) > 0) {
				BlockPos pos = blockpos.south();
				if (context.isAir(pos)) {
					context.setBlock(pos, Blocks.SNOW_BLOCK.defaultBlockState());
				}
			}
		});
	}
}
