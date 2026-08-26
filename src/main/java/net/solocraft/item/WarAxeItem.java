
package net.solocraft.item;

import net.solocraft.init.SololevelingModMobEffects;

import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.api.distmarker.Dist;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.Entity;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.component.ItemAttributeModifiers;

import java.util.List;

public class WarAxeItem extends Item {
	public WarAxeItem() {
		super(new Item.Properties().durability(0).attributes(ItemAttributeModifiers.builder()
				.add(Attributes.ATTACK_DAMAGE,
						new AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID, 12.2D, AttributeModifier.Operation.ADD_VALUE),
						EquipmentSlotGroup.MAINHAND)
				.add(Attributes.ATTACK_SPEED,
						new AttributeModifier(Item.BASE_ATTACK_SPEED_ID, -3.2D, AttributeModifier.Operation.ADD_VALUE),
						EquipmentSlotGroup.MAINHAND)
				.build()));
	}

	@Override
	public float getDestroySpeed(ItemStack itemstack, BlockState blockstate) {
		return 1;
	}

	@Override
	public boolean mineBlock(ItemStack itemstack, Level world, BlockState blockstate, BlockPos pos, LivingEntity entity) {
		itemstack.hurtAndBreak(1, entity, EquipmentSlot.MAINHAND);
		return true;
	}

	@Override
	public boolean hurtEnemy(ItemStack itemstack, LivingEntity entity, LivingEntity sourceentity) {
		itemstack.hurtAndBreak(2, sourceentity, EquipmentSlot.MAINHAND);
		return true;
	}

	@Override
	public int getEnchantmentValue() {
		return 2;
	}

	@Override
	public void appendHoverText(ItemStack itemstack, Item.TooltipContext context, List<Component> list, TooltipFlag flag) {
		super.appendHoverText(itemstack, context, list, flag);
		list.add(Component.literal("\u00A76RARITY: S"));
		list.add(Component.literal("\u00A76ATTACK: +400"));
		list.add(Component.literal("\u00A76TYPE: AXE"));
		list.add(Component.literal("\u00A76THIS HAMMER WAS PREVIOUSLY USED BY GIANTS BUT THEN IT'S SIZE REDUCED BY THE HELP OF THE DRARWES TO A LEVEL WHERE HUMANS CAN USE IT."));
		list.add(Component.literal("\u00A76BUT STILL THERE ARE VERY LESS CHOOSEN PEOPLE WHO CAN LIFT IT"));
	}

	@Override
	@OnlyIn(Dist.CLIENT)
	public boolean isFoil(ItemStack itemstack) {
		return Minecraft.getInstance().player != null
				&& Minecraft.getInstance().player.hasEffect(SololevelingModMobEffects.SWORD_ENHANCE);
	}
}
