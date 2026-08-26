
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
import net.minecraft.world.level.levelgen.feature.treedecorators.LeaveVineDecorator;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

import com.mojang.serialization.MapCodec;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD)
public class DungeonBiomeSnowLeaveDecorator extends LeaveVineDecorator {
	public static final MapCodec<DungeonBiomeSnowLeaveDecorator> CODEC = MapCodec.unit(DungeonBiomeSnowLeaveDecorator::new);
	public static final TreeDecoratorType<DungeonBiomeSnowLeaveDecorator> DECORATOR_TYPE = new TreeDecoratorType<>(CODEC);

	@SubscribeEvent
	public static void registerPointOfInterest(RegisterEvent event) {
		event.register(Registries.TREE_DECORATOR_TYPE, registerHelper -> registerHelper.register(
				ResourceLocation.fromNamespaceAndPath(SololevelingMod.MODID, "dungeon_biome_snow_tree_leave_decorator"), DECORATOR_TYPE));
	}

	public DungeonBiomeSnowLeaveDecorator() {
		super(0.25f);
	}

	@Override
	protected TreeDecoratorType<?> type() {
		return DECORATOR_TYPE;
	}

	@Override
	public void place(TreeDecorator.Context context) {
		context.leaves().forEach((blockpos) -> {
			if (context.random().nextFloat() < 0.25f) {
				BlockPos pos = blockpos.west();
				if (context.isAir(pos)) {
					addVine(pos, context);
				}
			}
			if (context.random().nextFloat() < 0.25f) {
				BlockPos pos = blockpos.east();
				if (context.isAir(pos)) {
					addVine(pos, context);
				}
			}
			if (context.random().nextFloat() < 0.25f) {
				BlockPos pos = blockpos.north();
				if (context.isAir(pos)) {
					addVine(pos, context);
				}
			}
			if (context.random().nextFloat() < 0.25f) {
				BlockPos pos = blockpos.south();
				if (context.isAir(pos)) {
					addVine(pos, context);
				}
			}
		});
	}

	private static void addVine(BlockPos pos, TreeDecorator.Context context) {
		context.setBlock(pos, Blocks.SNOW_BLOCK.defaultBlockState());
		int i = 4;
		for (BlockPos blockpos = pos.below(); context.isAir(blockpos) && i > 0; --i) {
			context.setBlock(blockpos, Blocks.SNOW_BLOCK.defaultBlockState());
			blockpos = blockpos.below();
		}
	}
}
