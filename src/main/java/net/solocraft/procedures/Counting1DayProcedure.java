package net.solocraft.procedures;

import net.solocraft.network.SololevelingModVariables;

import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.Event;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;

@EventBusSubscriber
public class Counting1DayProcedure {
	private static final ResourceLocation SHOP_ITEMS = ResourceLocation.parse("sololeveling:shop_items");

	@SubscribeEvent
	public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
		if (event.getEntity() instanceof ServerPlayer player)
			initializeMissingStock(player);
	}

	@SubscribeEvent
	public static void onPlayerTick(PlayerTickEvent.Post event) {
		if (true) {
			execute(event, event.getEntity().level(), event.getEntity());
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
		return new ItemStack(net.solocraft.util.RegistryTagAccess.getTag(ItemTags.create(SHOP_ITEMS))
				.getRandomElement(random).orElseGet(() -> Items.AIR));
	}
}
