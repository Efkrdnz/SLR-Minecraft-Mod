package net.solocraft.procedures;

import net.solocraft.init.SololevelingModItems;
import net.solocraft.init.SololevelingModBlocks;

import net.minecraftforge.registries.ForgeRegistries;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.sounds.SoundSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;

public class DeepslateKeyblockRedRCProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		if (entity == null)
			return;
		if ((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == SololevelingModItems.REDKEY.get()) {
			if (entity instanceof Player _player) {
				ItemStack _stktoremove = new ItemStack(SololevelingModItems.REDKEY.get());
				_player.getInventory().clearOrCountMatchingItems(p -> _stktoremove.getItem() == p.getItem(), 1, _player.inventoryMenu.getCraftSlots());
			}
			if (world instanceof Level _level) {
				if (!_level.isClientSide()) {
					_level.playSound(null, BlockPos.containing(x, y, z), ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("entity.experience_orb.pickup")), SoundSource.NEUTRAL, 2, 1);
				} else {
					_level.playLocalSound(x, y, z, ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("entity.experience_orb.pickup")), SoundSource.NEUTRAL, 2, 1, false);
				}
			}
			world.destroyBlock(BlockPos.containing(x, y, z), false);
			if ((world.getBlockState(BlockPos.containing(x + 0, y + 1, z + 0))).getBlock() == SololevelingModBlocks.UNBREAKABLE_DEEPSLATE.get()) {
				world.destroyBlock(BlockPos.containing(x + 0, y + 1, z + 0), false);
			}
			if ((world.getBlockState(BlockPos.containing(x + 0, y - 1, z + 0))).getBlock() == SololevelingModBlocks.UNBREAKABLE_DEEPSLATE.get()) {
				world.destroyBlock(BlockPos.containing(x + 0, y - 1, z + 0), false);
			}
			if ((world.getBlockState(BlockPos.containing(x + 1, y + 0, z + 0))).getBlock() == SololevelingModBlocks.UNBREAKABLE_DEEPSLATE.get()) {
				world.destroyBlock(BlockPos.containing(x + 1, y + 0, z + 0), false);
			}
			if ((world.getBlockState(BlockPos.containing(x - 1, y + 0, z + 0))).getBlock() == SololevelingModBlocks.UNBREAKABLE_DEEPSLATE.get()) {
				world.destroyBlock(BlockPos.containing(x - 1, y + 0, z + 0), false);
			}
			if ((world.getBlockState(BlockPos.containing(x + 1, y + 1, z + 0))).getBlock() == SololevelingModBlocks.UNBREAKABLE_DEEPSLATE.get()) {
				world.destroyBlock(BlockPos.containing(x + 1, y + 1, z + 0), false);
			}
			if ((world.getBlockState(BlockPos.containing(x - 1, y + 1, z + 0))).getBlock() == SololevelingModBlocks.UNBREAKABLE_DEEPSLATE.get()) {
				world.destroyBlock(BlockPos.containing(x - 1, y + 1, z + 0), false);
			}
			if ((world.getBlockState(BlockPos.containing(x + 1, y - 1, z + 0))).getBlock() == SololevelingModBlocks.UNBREAKABLE_DEEPSLATE.get()) {
				world.destroyBlock(BlockPos.containing(x + 1, y - 1, z + 0), false);
			}
			if ((world.getBlockState(BlockPos.containing(x - 1, y - 1, z + 0))).getBlock() == SololevelingModBlocks.UNBREAKABLE_DEEPSLATE.get()) {
				world.destroyBlock(BlockPos.containing(x - 1, y - 1, z + 0), false);
			}
			if ((world.getBlockState(BlockPos.containing(x + 0, y + 0, z + 1))).getBlock() == SololevelingModBlocks.UNBREAKABLE_DEEPSLATE.get()) {
				world.destroyBlock(BlockPos.containing(x + 0, y + 0, z + 1), false);
			}
			if ((world.getBlockState(BlockPos.containing(x + 0, y - 1, z + 1))).getBlock() == SololevelingModBlocks.UNBREAKABLE_DEEPSLATE.get()) {
				world.destroyBlock(BlockPos.containing(x + 0, y - 1, z + 1), false);
			}
			if ((world.getBlockState(BlockPos.containing(x + 0, y + 1, z + 1))).getBlock() == SololevelingModBlocks.UNBREAKABLE_DEEPSLATE.get()) {
				world.destroyBlock(BlockPos.containing(x + 0, y + 1, z + 1), false);
			}
			if ((world.getBlockState(BlockPos.containing(x + 0, y + 0, z - 1))).getBlock() == SololevelingModBlocks.UNBREAKABLE_DEEPSLATE.get()) {
				world.destroyBlock(BlockPos.containing(x + 0, y + 0, z - 1), false);
			}
			if ((world.getBlockState(BlockPos.containing(x + 0, y + 1, z - 1))).getBlock() == SololevelingModBlocks.UNBREAKABLE_DEEPSLATE.get()) {
				world.destroyBlock(BlockPos.containing(x + 0, y + 1, z - 1), false);
			}
			if ((world.getBlockState(BlockPos.containing(x + 0, y - 1, z - 1))).getBlock() == SololevelingModBlocks.UNBREAKABLE_DEEPSLATE.get()) {
				world.destroyBlock(BlockPos.containing(x + 0, y - 1, z - 1), false);
			}
		} else {
			if (entity instanceof Player _player && !_player.level().isClientSide())
				_player.displayClientMessage(Component.literal(("You need to use:" + " " + "\u00A74" + "Amber Key")), true);
		}
	}
}
