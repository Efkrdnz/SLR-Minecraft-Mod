package net.solocraft.procedures;

import net.solocraft.network.SololevelingModVariables;
import net.solocraft.init.SololevelingModItems;

import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.items.ItemHandlerHelper;

import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Mth;
import net.minecraft.tags.ItemTags;
import net.minecraft.resources.ResourceLocation;

public class RewardCollectProcedure {
	public static void execute(Entity entity, String reward_name) {
		if (entity == null || reward_name == null)
			return;
		double rand = 0;
		String reward = "";
		String reward_list = "";
		String item = "";
		ItemStack itemtogive = ItemStack.EMPTY;
		reward_list = "SP5, SP10, SP15, SP20, FR, ITEMBOX, GOLD";
		reward = reward_name;
		if ((reward).equals("FR")) {
			if (entity instanceof LivingEntity _entity)
				_entity.setHealth(entity instanceof LivingEntity _livEnt ? _livEnt.getMaxHealth() : -1);
			{
				double _setval = 0;
				entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
					capability.Fatigue = _setval;
					capability.syncPlayerVariables(entity);
				});
			}
			{
				double _setval = (entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).Mana;
				entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
					capability.MP = _setval;
					capability.syncPlayerVariables(entity);
				});
			}
		}
		if ((reward).equals("ITEMBOX")) {
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
					ItemStack _setstack = new ItemStack(SololevelingModItems.INSTANCE_DUNGEON_KEY.get());
					_setstack.setCount(1);
					ItemHandlerHelper.giveItemToPlayer(_player, _setstack);
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
						ItemStack _setstack = new ItemStack((ForgeRegistries.ITEMS.tags().getTag(ItemTags.create(new ResourceLocation("loot_items"))).getRandomElement(RandomSource.create()).orElseGet(() -> Items.AIR)));
						_setstack.setCount(1);
						ItemHandlerHelper.giveItemToPlayer(_player, _setstack);
					}
				}
			}
		}
		if (reward.startsWith("SP")) {
			try {
				int amount = Integer.parseInt(reward.substring(2));
				{
					double _setval = (entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).SkillPoints + amount;
					entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
						capability.SkillPoints = _setval;
						capability.syncPlayerVariables(entity);
					});
				}
			} catch (NumberFormatException e) {
				return;
			}
		}
		if (reward.startsWith("GOLD")) {
			try {
				int amount = Integer.parseInt(reward.substring(4));
				{
					double _setval = (entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).golds + amount;
					entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
						capability.golds = _setval;
						capability.syncPlayerVariables(entity);
					});
				}
			} catch (NumberFormatException e) {
				return;
			}
		}
		if (reward.startsWith("ITEM:")) {
			String itemResourceLocation = reward.substring(5); // Remove "ITEM:" prefix
			try {
				ResourceLocation itemLocation = new ResourceLocation(itemResourceLocation);
				Item itemm = ForgeRegistries.ITEMS.getValue(itemLocation);
				if (itemm != null && itemm != Items.AIR) {
					if (entity instanceof Player _player) {
						ItemStack _setstack = new ItemStack(itemm);
						_setstack.setCount(1);
						ItemHandlerHelper.giveItemToPlayer(_player, _setstack);
					}
				} else {
					System.err.println("[SoloLeveling] Invalid item reward: " + itemResourceLocation);
				}
			} catch (Exception e) {
				System.err.println("[SoloLeveling] Failed to parse item reward: " + reward);
				e.printStackTrace();
			}
		}
	}
}
