package net.solocraft.procedures;

import net.solocraft.network.SololevelingModVariables;

import net.minecraftforge.registries.ForgeRegistries;

import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;

public class RunestoneMonarchsDomainRightclickedProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity, ItemStack itemstack) {
		if (entity == null)
			return;
		if ((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).JOB == 1) {
			if (!hasMonarchsDomain(entity)) {
				grantMonarchsDomain(entity);
				if (entity instanceof Player _player) {
					ItemStack _stktoremove = itemstack;
					_player.getInventory().clearOrCountMatchingItems(p -> _stktoremove.getItem() == p.getItem(), 1, _player.inventoryMenu.getCraftSlots());
				}
				if (world instanceof Level _level) {
					if (!_level.isClientSide()) {
						_level.playSound(null, BlockPos.containing(x, y, z), ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("block.enchantment_table.use")), SoundSource.NEUTRAL, 1, 1);
					} else {
						_level.playLocalSound(x, y, z, ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("block.enchantment_table.use")), SoundSource.NEUTRAL, 1, 1, false);
					}
				}
			} else {
				if (entity instanceof Player _player && !_player.level().isClientSide())
					_player.displayClientMessage(Component.literal("You already have \"Monarch's Domain\""), true);
			}
		} else {
			if (entity instanceof Player _player && !_player.level().isClientSide())
				_player.displayClientMessage(Component.literal("You cannot learn this skill with current class"), true);
		}
	}

	private static boolean hasMonarchsDomain(Entity entity) {
		if (!(entity instanceof ServerPlayer player))
			return false;
		Advancement advancement = player.server.getAdvancements().getAdvancement(new ResourceLocation("sololeveling:monarchs_domain"));
		return advancement != null && player.getAdvancements().getOrStartProgress(advancement).isDone();
	}

	private static void grantMonarchsDomain(Entity entity) {
		if (!(entity instanceof ServerPlayer player))
			return;
		Advancement advancement = player.server.getAdvancements().getAdvancement(new ResourceLocation("sololeveling:monarchs_domain"));
		if (advancement == null)
			return;
		AdvancementProgress progress = player.getAdvancements().getOrStartProgress(advancement);
		if (!progress.isDone()) {
			for (String criteria : progress.getRemainingCriteria())
				player.getAdvancements().award(advancement, criteria);
		}
	}
}
