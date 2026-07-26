package net.solocraft.procedures;

import net.solocraft.network.SololevelingModVariables;

import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;

@Mod.EventBusSubscriber
public class Counting1DayProcedure {
	private static final ResourceLocation SHOP_ITEMS = new ResourceLocation("forge:shop_items");

	@SubscribeEvent
	public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
		if (event.getEntity() instanceof ServerPlayer player)
			initializeMissingStock(player);
	}

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
		if (!world.isClientSide() && world.dayTime() % 24000 == 12000)
			refreshDailyStock(entity);
	}

	private static void initializeMissingStock(ServerPlayer player) {
		player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
			if (capability.shopitem1.isEmpty() || capability.shopitem2.isEmpty()
					|| capability.shopitem3.isEmpty() || capability.shopitem4.isEmpty()
					|| capability.shopitem5.isEmpty() || capability.shopitem6.isEmpty())
				populateDailyStock(player, capability);
		});
	}

	private static void refreshDailyStock(Entity entity) {
		entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
				.ifPresent(capability -> populateDailyStock(entity, capability));
	}

	private static void populateDailyStock(Entity entity, SololevelingModVariables.PlayerVariables capability) {
		RandomSource random = RandomSource.create();
		capability.shopitem1 = randomShopItem(random);
		capability.shopitem2 = randomShopItem(random);
		capability.shopitem3 = randomShopItem(random);
		capability.shopitem4 = randomShopItem(random);
		capability.shopitem5 = randomShopItem(random);
		capability.shopitem6 = randomShopItem(random);
		capability.daily_refreshes = 1;
		capability.syncPlayerVariables(entity);
	}

	private static ItemStack randomShopItem(RandomSource random) {
		return new ItemStack(ForgeRegistries.ITEMS.tags().getTag(ItemTags.create(SHOP_ITEMS))
				.getRandomElement(random).orElseGet(() -> Items.AIR));
	}
}
