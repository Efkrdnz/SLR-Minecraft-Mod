package net.solocraft.procedures;

import net.solocraft.init.SololevelingModItems;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;

public class ReturnIDPersonProcedure {
	public static String execute(Entity entity) {
		if (entity == null)
			return "";
		if ((entity instanceof LivingEntity _entity) ? _entity.isHolding(SololevelingModItems.HUNTER_ID.get()) : false) {
			return "Name: \u00A7l" + ((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getOrCreateTag().getString("Person"));
		}
		return "";
	}
}
