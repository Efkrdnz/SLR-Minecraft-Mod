package net.solocraft.procedures;

import net.solocraft.network.SololevelingModVariables;

import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.event.TickEvent;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.Entity;
import net.minecraft.util.RandomSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;

@Mod.EventBusSubscriber
public class Counting1DayProcedure {
	@SubscribeEvent
	public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
		if (event.phase == TickEvent.Phase.END) {
			execute(event, event.player.level(), event.player);
		}
	}

	public static void execute(LevelAccessor world, Entity entity) {
		execute(null, world, entity);
	}

	private static void execute(@Nullable Event event, LevelAccessor world, Entity entity) {
		if (entity == null)
			return;
		if (world.dayTime() % 24000 == 12000) {
			{
				ItemStack _setval = new ItemStack((ForgeRegistries.ITEMS.tags().getTag(ItemTags.create(new ResourceLocation("forge:shop_items"))).getRandomElement(RandomSource.create()).orElseGet(() -> Items.AIR)));
				entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
					capability.shopitem1 = _setval;
					capability.syncPlayerVariables(entity);
				});
			}
			{
				ItemStack _setval = new ItemStack((ForgeRegistries.ITEMS.tags().getTag(ItemTags.create(new ResourceLocation("forge:shop_items"))).getRandomElement(RandomSource.create()).orElseGet(() -> Items.AIR)));
				entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
					capability.shopitem2 = _setval;
					capability.syncPlayerVariables(entity);
				});
			}
			{
				ItemStack _setval = new ItemStack((ForgeRegistries.ITEMS.tags().getTag(ItemTags.create(new ResourceLocation("forge:shop_items"))).getRandomElement(RandomSource.create()).orElseGet(() -> Items.AIR)));
				entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
					capability.shopitem3 = _setval;
					capability.syncPlayerVariables(entity);
				});
			}
			{
				ItemStack _setval = new ItemStack((ForgeRegistries.ITEMS.tags().getTag(ItemTags.create(new ResourceLocation("forge:shop_items"))).getRandomElement(RandomSource.create()).orElseGet(() -> Items.AIR)));
				entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
					capability.shopitem4 = _setval;
					capability.syncPlayerVariables(entity);
				});
			}
			{
				ItemStack _setval = new ItemStack((ForgeRegistries.ITEMS.tags().getTag(ItemTags.create(new ResourceLocation("forge:shop_items"))).getRandomElement(RandomSource.create()).orElseGet(() -> Items.AIR)));
				entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
					capability.shopitem5 = _setval;
					capability.syncPlayerVariables(entity);
				});
			}
			{
				ItemStack _setval = new ItemStack((ForgeRegistries.ITEMS.tags().getTag(ItemTags.create(new ResourceLocation("forge:shop_items"))).getRandomElement(RandomSource.create()).orElseGet(() -> Items.AIR)));
				entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
					capability.shopitem6 = _setval;
					capability.syncPlayerVariables(entity);
				});
			}
			{
				double _setval = 1;
				entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
					capability.daily_refreshes = _setval;
					capability.syncPlayerVariables(entity);
				});
			}
		}
	}
}
