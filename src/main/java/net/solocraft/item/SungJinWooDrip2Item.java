
package net.solocraft.item;

import net.solocraft.client.model.Modeljinwoolegs2;
import net.solocraft.client.model.Modeljinwoochest2;
import net.solocraft.init.SololevelingModArmorMaterials;

import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.api.distmarker.Dist;

import net.minecraft.world.level.Level;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Entity;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.Minecraft;

import java.util.function.Consumer;
import java.util.Map;
import java.util.List;
import java.util.Collections;

public abstract class SungJinWooDrip2Item extends ArmorItem {
	public SungJinWooDrip2Item(ArmorItem.Type type, Item.Properties properties) {
		super(SololevelingModArmorMaterials.SUNG_JIN_WOO_DRIP_2, type, properties);
	}

	public static class Chestplate extends SungJinWooDrip2Item {
		public Chestplate() {
			super(ArmorItem.Type.CHESTPLATE, new Item.Properties());
		}

		@Override
		public void initializeClient(Consumer<IClientItemExtensions> consumer) {
			consumer.accept(new IClientItemExtensions() {
				@Override
				@OnlyIn(Dist.CLIENT)
				public HumanoidModel getHumanoidArmorModel(LivingEntity living, ItemStack stack, EquipmentSlot slot, HumanoidModel defaultModel) {
					HumanoidModel armorModel = new HumanoidModel(new ModelPart(Collections.emptyList(), Map.of("body", new Modeljinwoochest2(Minecraft.getInstance().getEntityModels().bakeLayer(Modeljinwoochest2.LAYER_LOCATION)).Body, "left_arm",
							new Modeljinwoochest2(Minecraft.getInstance().getEntityModels().bakeLayer(Modeljinwoochest2.LAYER_LOCATION)).LeftArm, "right_arm",
							new Modeljinwoochest2(Minecraft.getInstance().getEntityModels().bakeLayer(Modeljinwoochest2.LAYER_LOCATION)).RightArm, "head", new ModelPart(Collections.emptyList(), Collections.emptyMap()), "hat",
							new ModelPart(Collections.emptyList(), Collections.emptyMap()), "right_leg", new ModelPart(Collections.emptyList(), Collections.emptyMap()), "left_leg", new ModelPart(Collections.emptyList(), Collections.emptyMap()))));
					armorModel.crouching = living.isShiftKeyDown();
					armorModel.riding = defaultModel.riding;
					armorModel.young = living.isBaby();
					return armorModel;
				}
			});
		}

		@Override
		public void appendHoverText(ItemStack itemstack, Item.TooltipContext context, List<Component> list, TooltipFlag flag) {
			super.appendHoverText(itemstack, context, list, flag);
		}

		@Override
		public boolean makesPiglinsNeutral(ItemStack itemstack, LivingEntity entity) {
			return false;
		}
	}

	public static class Leggings extends SungJinWooDrip2Item {
		public Leggings() {
			super(ArmorItem.Type.LEGGINGS, new Item.Properties());
		}

		@Override
		public void initializeClient(Consumer<IClientItemExtensions> consumer) {
			consumer.accept(new IClientItemExtensions() {
				@Override
				@OnlyIn(Dist.CLIENT)
				public HumanoidModel getHumanoidArmorModel(LivingEntity living, ItemStack stack, EquipmentSlot slot, HumanoidModel defaultModel) {
					HumanoidModel armorModel = new HumanoidModel(new ModelPart(Collections.emptyList(),
							Map.of("left_leg", new Modeljinwoolegs2(Minecraft.getInstance().getEntityModels().bakeLayer(Modeljinwoolegs2.LAYER_LOCATION)).RightLeg, "right_leg",
									new Modeljinwoolegs2(Minecraft.getInstance().getEntityModels().bakeLayer(Modeljinwoolegs2.LAYER_LOCATION)).RightLeg, "head", new ModelPart(Collections.emptyList(), Collections.emptyMap()), "hat",
									new ModelPart(Collections.emptyList(), Collections.emptyMap()), "body", new ModelPart(Collections.emptyList(), Collections.emptyMap()), "right_arm", new ModelPart(Collections.emptyList(), Collections.emptyMap()),
									"left_arm", new ModelPart(Collections.emptyList(), Collections.emptyMap()))));
					armorModel.crouching = living.isShiftKeyDown();
					armorModel.riding = defaultModel.riding;
					armorModel.young = living.isBaby();
					return armorModel;
				}
			});
		}

		@Override
		public void appendHoverText(ItemStack itemstack, Item.TooltipContext context, List<Component> list, TooltipFlag flag) {
			super.appendHoverText(itemstack, context, list, flag);
		}

		@Override
		public boolean makesPiglinsNeutral(ItemStack itemstack, LivingEntity entity) {
			return false;
		}
	}
}
