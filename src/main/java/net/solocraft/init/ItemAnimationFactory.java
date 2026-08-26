package net.solocraft.init;

import software.bernie.geckolib.animatable.GeoItem;

import net.solocraft.item.ManaGunItem;
import net.solocraft.util.ItemStackData;
import net.solocraft.item.KangsDaggerItem;
import net.solocraft.item.GriamoreItem;

import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import net.minecraft.world.item.ArmorItem;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.Minecraft;

@EventBusSubscriber
public class ItemAnimationFactory {
	public static void disableUseAnim() {
		try {
			ItemInHandRenderer renderer = Minecraft.getInstance().gameRenderer.itemInHandRenderer;
			if (renderer != null) {
				renderer.mainHandHeight = 1F;
				renderer.oMainHandHeight = 1F;
				renderer.offHandHeight = 1F;
				renderer.oOffHandHeight = 1F;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@SubscribeEvent
	public static void animatedItems(PlayerTickEvent.Pre event) {
		String animation = "";
		if (true && (event.getEntity().getMainHandItem().getItem() instanceof GeoItem || event.getEntity().getOffhandItem().getItem() instanceof GeoItem)) {
			if (!ItemStackData.getString(event.getEntity().getMainHandItem(), "geckoAnim").isEmpty() && !(event.getEntity().getMainHandItem().getItem() instanceof ArmorItem)) {
				animation = ItemStackData.getString(event.getEntity().getMainHandItem(), "geckoAnim");
				ItemStackData.putString(event.getEntity().getMainHandItem(), "geckoAnim", "");
				if (event.getEntity().getMainHandItem().getItem() instanceof ManaGunItem animatable)
					if (event.getEntity().level().isClientSide()) {
						animatable.animationprocedure = animation;
						disableUseAnim();
					}
				if (event.getEntity().getMainHandItem().getItem() instanceof GriamoreItem animatable)
					if (event.getEntity().level().isClientSide()) {
						animatable.animationprocedure = animation;
						disableUseAnim();
					}
				if (event.getEntity().getMainHandItem().getItem() instanceof KangsDaggerItem animatable)
					if (event.getEntity().level().isClientSide()) {
						animatable.animationprocedure = animation;
						disableUseAnim();
					}
			}
			if (!ItemStackData.getString(event.getEntity().getOffhandItem(), "geckoAnim").isEmpty() && !(event.getEntity().getOffhandItem().getItem() instanceof ArmorItem)) {
				animation = ItemStackData.getString(event.getEntity().getOffhandItem(), "geckoAnim");
				ItemStackData.putString(event.getEntity().getOffhandItem(), "geckoAnim", "");
				if (event.getEntity().getOffhandItem().getItem() instanceof ManaGunItem animatable)
					if (event.getEntity().level().isClientSide()) {
						animatable.animationprocedure = animation;
						disableUseAnim();
					}
				if (event.getEntity().getOffhandItem().getItem() instanceof GriamoreItem animatable)
					if (event.getEntity().level().isClientSide()) {
						animatable.animationprocedure = animation;
						disableUseAnim();
					}
				if (event.getEntity().getOffhandItem().getItem() instanceof KangsDaggerItem animatable)
					if (event.getEntity().level().isClientSide()) {
						animatable.animationprocedure = animation;
						disableUseAnim();
					}
			}
		}
	}
}
