
package net.solocraft.item;

import net.solocraft.procedures.KamishWrathHasItemGlowingEffectProcedure;

import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.api.distmarker.Dist;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.Entity;
import net.minecraft.network.chat.Component;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.component.ItemAttributeModifiers;

import java.util.List;

public class KamishWrathItem extends Item {
	public KamishWrathItem() {
		super(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).attributes(ItemAttributeModifiers.builder()
				.add(Attributes.ATTACK_DAMAGE,
						new AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID, 12.0D, AttributeModifier.Operation.ADD_VALUE),
						EquipmentSlotGroup.MAINHAND)
				.add(Attributes.ATTACK_SPEED,
						new AttributeModifier(Item.BASE_ATTACK_SPEED_ID, -2.4D, AttributeModifier.Operation.ADD_VALUE),
						EquipmentSlotGroup.MAINHAND)
				.build()));
	}

	@Override
	public float getDestroySpeed(ItemStack par1ItemStack, BlockState par2Block) {
		return 1.9f;
	}

	@Override
	@OnlyIn(Dist.CLIENT)
	public boolean isFoil(ItemStack itemstack) {
		Entity entity = Minecraft.getInstance().player;
		return KamishWrathHasItemGlowingEffectProcedure.execute(entity);
	}

	@Override
	public void appendHoverText(ItemStack itemstack, Item.TooltipContext context, List<Component> list, TooltipFlag flag) {
		super.appendHoverText(itemstack, context, list, flag);
		list.add(Component.literal("\u00A76LEVEL OF DIFFICULTY: ??"));
		list.add(Component.literal("\u00A76TYPE: DAGGER"));
		list.add(Component.literal("\u00A76ATTACK: UNSTABLE"));
		list.add(Component.literal("\u00A76THE MOST POWERFUL DAGGER FORGED BY A MASTER CRAFTSMAN USING THE SHARPEST FANG OF A DRAGON. ITS SHARPNESS IS SECOND TO NONE, AND ITS MANA SENSITIVITY IS EXCEPTIONAL."));
		list.add(Component.literal("\u00A76PASSIVE \"MANA SENSITIVITY\": EACH HELD FANG COMBINES THE WIELDER'S PERMANENT STRENGTH AND INTELLIGENCE INTO A SCALING STRENGTH BONUS."));
	}
}
