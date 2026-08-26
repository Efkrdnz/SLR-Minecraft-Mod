package net.solocraft.procedures;

import net.solocraft.network.SololevelingModVariables;
import net.solocraft.init.SololevelingModItems;
import net.solocraft.util.InstanceDungeonKeyAccess;

import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.items.ItemHandlerHelper;

import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Entity;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Mth;
import net.minecraft.tags.ItemTags;
import net.minecraft.resources.ResourceLocation;

public class LootboxProcedure {
	public static void execute(Entity entity) {
		if (entity == null)
			return;
		double rand = 0;
		{
			boolean _setval = false;
			entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
				capability.giftstatus = _setval;
				capability.syncPlayerVariables(entity);
			});
		}
		if (entity instanceof Player _player)
			_player.closeContainer();
		if (((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).MainQuest).equals("Getting Stronger")
				&& (entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).QuestProgression == 0) {
			{
				boolean _setval = false;
				entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
					capability.giftstatus = _setval;
					capability.syncPlayerVariables(entity);
				});
			}
			if (entity instanceof Player _player) {
				InstanceDungeonKeyAccess.grantInitialKey(_player);
			}
			{
				double _setval = 1;
				entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
					capability.QuestProgression = _setval;
					capability.syncPlayerVariables(entity);
				});
			}
			if (entity instanceof Player _player)
				_player.closeContainer();
		} else {
			rand = Mth.nextInt(RandomSource.create(), 1, 130);
			{
				boolean _setval = false;
				entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
					capability.giftstatus = _setval;
					capability.syncPlayerVariables(entity);
				});
			}
			if (rand == 1) {
				if (entity instanceof Player _player) {
					ItemStack _setstack = new ItemStack(SololevelingModItems.KATANA_STIER.get());
					_setstack.setCount(1);
					ItemHandlerHelper.giveItemToPlayer(_player, _setstack);
				}
				if (entity instanceof Player _player)
					_player.closeContainer();
			} else if (rand == 2) {
				if (entity instanceof Player _player) {
					ItemStack _setstack = new ItemStack(SololevelingModItems.HAMMER.get());
					_setstack.setCount(1);
					ItemHandlerHelper.giveItemToPlayer(_player, _setstack);
				}
				if (entity instanceof Player _player)
					_player.closeContainer();
			} else if (rand == 3) {
				if (entity instanceof Player _player) {
					ItemStack _setstack = new ItemStack(SololevelingModItems.HAMMER.get());
					_setstack.setCount(1);
					ItemHandlerHelper.giveItemToPlayer(_player, _setstack);
				}
				if (entity instanceof Player _player)
					_player.closeContainer();
			} else if (rand == 4) {
				if (entity instanceof Player _player) {
					ItemStack _setstack = new ItemStack(SololevelingModItems.MYTHIC_DAGGER.get());
					_setstack.setCount(1);
					ItemHandlerHelper.giveItemToPlayer(_player, _setstack);
				}
				if (entity instanceof Player _player)
					_player.closeContainer();
			} else {
				if (entity instanceof Player _player) {
					ItemStack _setstack = new ItemStack((net.solocraft.util.RegistryTagAccess.getTag(ItemTags.create(ResourceLocation.parse("loot_items"))).getRandomElement(RandomSource.create()).orElseGet(() -> Items.AIR)));
					_setstack.setCount(1);
					ItemHandlerHelper.giveItemToPlayer(_player, _setstack);
				}
			}
		}
	}
}
