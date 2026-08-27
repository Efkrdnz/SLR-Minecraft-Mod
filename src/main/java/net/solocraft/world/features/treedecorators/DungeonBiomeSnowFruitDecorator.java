
package net.solocraft.world.features.treedecorators;

import net.solocraft.SololevelingMod;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;

import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecoratorType;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecorator;
import net.minecraft.world.level.levelgen.feature.treedecorators.CocoaDecorator;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.util.RandomSource;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

import com.mojang.serialization.MapCodec;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD)
public class DungeonBiomeSnowFruitDecorator extends CocoaDecorator {
	public static final MapCodec<DungeonBiomeSnowFruitDecorator> CODEC = MapCodec.unit(DungeonBiomeSnowFruitDecorator::new);
	public static final TreeDecoratorType<DungeonBiomeSnowFruitDecorator> DECORATOR_TYPE = new TreeDecoratorType<>(CODEC);

	@SubscribeEvent
	public static void registerPointOfInterest(RegisterEvent event) {
		event.register(Registries.TREE_DECORATOR_TYPE, registerHelper -> registerHelper.register(
				ResourceLocation.fromNamespaceAndPath(SololevelingMod.MODID, "dungeon_biome_snow_tree_fruit_decorator"), DECORATOR_TYPE));
	}

	public DungeonBiomeSnowFruitDecorator() {
		super(0.2f);
	}

	@Override
	protected TreeDecoratorType<?> type() {
		return DECORATOR_TYPE;
	}

	@Override
	public void place(TreeDecorator.Context context) {
		RandomSource randomsource = context.random();
		if (!(randomsource.nextFloat() >= 0.2F)) {
			List<BlockPos> list = context.logs();
			int i = list.get(0).getY();
			list.stream().filter((p_69980_) -> {
				return p_69980_.getY() - i <= 2;
			}).forEach((p_226026_) -> {
				for (Direction direction : Direction.Plane.HORIZONTAL) {
					if (randomsource.nextFloat() <= 0.25F) {
						Direction direction1 = direction.getOpposite();
						BlockPos blockpos = p_226026_.offset(direction1.getStepX(), 0, direction1.getStepZ());
						if (context.isAir(blockpos)) {
							context.setBlock(blockpos, Blocks.SNOW_BLOCK.defaultBlockState());
						}
					}
				}
			});
		}
	}
}
