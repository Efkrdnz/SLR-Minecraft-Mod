package net.solocraft.procedures;

import net.solocraft.network.SololevelingModVariables;
import net.solocraft.init.SololevelingModItems;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.event.TickEvent;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Entity;

import javax.annotation.Nullable;
import net.solocraft.util.CooldownManager;

@Mod.EventBusSubscriber
public class ManaRegenProcedure {
	@SubscribeEvent
	public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
		if (event.phase == TickEvent.Phase.END) {
			execute(event, event.player);
		}
	}

	public static void execute(Entity entity) {
		execute(null, entity);
	}

	private static void execute(@Nullable Event event, Entity entity) {
		if (entity == null)
			return;
		SololevelingModVariables.PlayerVariables capability = entity.getCapability(
				SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(null);
		if (capability == null)
			return;

		double previousMp = capability.MP;
		boolean suppressRegen = false;
		if (entity instanceof LivingEntity living) {
			ItemStack boots = living.getItemBySlot(EquipmentSlot.FEET);
			ItemStack leggings = living.getItemBySlot(EquipmentSlot.LEGS);
			ItemStack chestplate = living.getItemBySlot(EquipmentSlot.CHEST);
			ItemStack helmet = living.getItemBySlot(EquipmentSlot.HEAD);
			suppressRegen = boots.is(SololevelingModItems.SHADOW_ARMOR_BOOTS.get())
					|| leggings.is(SololevelingModItems.SHADOW_ARMOR_LEGGINGS.get())
					|| chestplate.is(SololevelingModItems.SHADOW_ARMOR_CHESTPLATE.get())
					|| helmet.is(SololevelingModItems.SHADOW_ARMOR_HELMET.get())
					|| boots.is(SololevelingModItems.GOLIATH_ARMOR_BOOTS.get())
					|| leggings.is(SololevelingModItems.GOLIATH_ARMOR_LEGGINGS.get())
					|| chestplate.is(SololevelingModItems.GOLIATH_ARMOR_CHESTPLATE.get())
					|| helmet.is(SololevelingModItems.GOLIATH_ARMOR_HELMET.get());
		}
		if (capability.MP < capability.Mana && !suppressRegen
				&& !CooldownManager.isOnCooldown(entity, "mana_refresh")) {
			capability.MP += capability.manaregen + (capability.Intelligence / 20) * 2;
		}
		if (capability.MP > capability.Mana)
			capability.MP = capability.Mana;
		if (capability.MP < 0)
			capability.MP = 0;
		if (Double.compare(capability.MP, previousMp) != 0)
			capability.syncPlayerVariables(entity);
	}
}
