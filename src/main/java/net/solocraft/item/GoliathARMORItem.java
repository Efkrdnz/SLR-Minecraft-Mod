package net.solocraft.item;

import net.solocraft.client.model.Modelgoliathchest;
import net.solocraft.client.model.Modelgoliathfeet;
import net.solocraft.client.model.Modelgoliathhelm;
import net.solocraft.client.model.Modelgoliathlegs;
import net.solocraft.procedures.GoliathArmorTickProcedure;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.registries.ForgeRegistries;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import com.google.common.collect.Iterables;

public abstract class GoliathARMORItem extends ArmorItem {
	public GoliathARMORItem(ArmorItem.Type type, Item.Properties properties) {
		super(new ArmorMaterial() {
			@Override
			public int getDurabilityForType(ArmorItem.Type type) {
				return new int[] { 13, 15, 16, 11 }[type.getSlot().getIndex()] * 80;
			}

			@Override
			public int getDefenseForType(ArmorItem.Type type) {
				return new int[] { 3, 14, 18, 7 }[type.getSlot().getIndex()];
			}

			@Override
			public int getEnchantmentValue() {
				return 0;
			}

			@Override
			public SoundEvent getEquipSound() {
				return ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("item.armor.equip_netherite"));
			}

			@Override
			public Ingredient getRepairIngredient() {
				return Ingredient.of();
			}

			@Override
			public String getName() {
				return "goliath_armor";
			}

			@Override
			public float getToughness() {
				return 5.0f;
			}

			@Override
			public float getKnockbackResistance() {
				return 1.0f;
			}
		}, type, properties);
	}

	public static class Helmet extends GoliathARMORItem {
		public Helmet() {
			super(ArmorItem.Type.HELMET, new Item.Properties());
		}

		@Override
		public void initializeClient(Consumer<IClientItemExtensions> consumer) {
			consumer.accept(new IClientItemExtensions() {
				@Override
				public HumanoidModel getHumanoidArmorModel(LivingEntity living, ItemStack stack, EquipmentSlot slot, HumanoidModel defaultModel) {
					Modelgoliathhelm model = new Modelgoliathhelm(Minecraft.getInstance().getEntityModels().bakeLayer(Modelgoliathhelm.LAYER_LOCATION));
					HumanoidModel armorModel = new HumanoidModel(new ModelPart(Collections.emptyList(),
							Map.of("head", model.Head, "hat", empty(), "body", empty(), "right_arm", empty(), "left_arm", empty(), "right_leg", empty(), "left_leg", empty())));
					copyState(living, defaultModel, armorModel);
					return armorModel;
				}
			});
		}

		@Override
		public void appendHoverText(ItemStack itemstack, Level world, List<Component> list, TooltipFlag flag) {
			super.appendHoverText(itemstack, world, list, flag);
		}

		@Override
		public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
			return "sololeveling:textures/models/armor/goliath_helm.png";
		}

		@Override
		public void inventoryTick(ItemStack itemstack, Level world, Entity entity, int slot, boolean selected) {
			super.inventoryTick(itemstack, world, entity, slot, selected);
			if (entity instanceof Player player && Iterables.contains(player.getArmorSlots(), itemstack))
				GoliathArmorTickProcedure.execute(world, entity, itemstack);
		}
	}

	public static class Chestplate extends GoliathARMORItem {
		public Chestplate() {
			super(ArmorItem.Type.CHESTPLATE, new Item.Properties());
		}

		@Override
		public void initializeClient(Consumer<IClientItemExtensions> consumer) {
			consumer.accept(new IClientItemExtensions() {
				@Override
				@OnlyIn(Dist.CLIENT)
				public HumanoidModel getHumanoidArmorModel(LivingEntity living, ItemStack stack, EquipmentSlot slot, HumanoidModel defaultModel) {
					Modelgoliathchest model = new Modelgoliathchest(Minecraft.getInstance().getEntityModels().bakeLayer(Modelgoliathchest.LAYER_LOCATION));
					HumanoidModel armorModel = new HumanoidModel(new ModelPart(Collections.emptyList(),
							Map.of("body", model.Body, "left_arm", model.LeftArm, "right_arm", model.RightArm, "head", empty(), "hat", empty(), "right_leg", empty(), "left_leg", empty())));
					copyState(living, defaultModel, armorModel);
					return armorModel;
				}
			});
		}

		@Override
		public void appendHoverText(ItemStack itemstack, Level world, List<Component> list, TooltipFlag flag) {
			super.appendHoverText(itemstack, world, list, flag);
		}

		@Override
		public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
			return "sololeveling:textures/models/armor/goliath_chest.png";
		}

		@Override
		public void inventoryTick(ItemStack itemstack, Level world, Entity entity, int slot, boolean selected) {
			super.inventoryTick(itemstack, world, entity, slot, selected);
			if (entity instanceof Player player && Iterables.contains(player.getArmorSlots(), itemstack))
				GoliathArmorTickProcedure.execute(world, entity, itemstack);
		}
	}

	public static class Leggings extends GoliathARMORItem {
		public Leggings() {
			super(ArmorItem.Type.LEGGINGS, new Item.Properties());
		}

		@Override
		public void initializeClient(Consumer<IClientItemExtensions> consumer) {
			consumer.accept(new IClientItemExtensions() {
				@Override
				@OnlyIn(Dist.CLIENT)
				public HumanoidModel getHumanoidArmorModel(LivingEntity living, ItemStack stack, EquipmentSlot slot, HumanoidModel defaultModel) {
					Modelgoliathlegs model = new Modelgoliathlegs(Minecraft.getInstance().getEntityModels().bakeLayer(Modelgoliathlegs.LAYER_LOCATION));
					HumanoidModel armorModel = new HumanoidModel(new ModelPart(Collections.emptyList(),
							Map.of("left_leg", model.LeftLeg, "right_leg", model.RightLeg, "head", empty(), "hat", empty(), "body", empty(), "right_arm", empty(), "left_arm", empty())));
					copyState(living, defaultModel, armorModel);
					return armorModel;
				}
			});
		}

		@Override
		public void appendHoverText(ItemStack itemstack, Level world, List<Component> list, TooltipFlag flag) {
			super.appendHoverText(itemstack, world, list, flag);
		}

		@Override
		public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
			return "sololeveling:textures/models/armor/goliath_legs.png";
		}

		@Override
		public void inventoryTick(ItemStack itemstack, Level world, Entity entity, int slot, boolean selected) {
			super.inventoryTick(itemstack, world, entity, slot, selected);
			if (entity instanceof Player player && Iterables.contains(player.getArmorSlots(), itemstack))
				GoliathArmorTickProcedure.execute(world, entity, itemstack);
		}
	}

	public static class Boots extends GoliathARMORItem {
		public Boots() {
			super(ArmorItem.Type.BOOTS, new Item.Properties());
		}

		@Override
		public void initializeClient(Consumer<IClientItemExtensions> consumer) {
			consumer.accept(new IClientItemExtensions() {
				@Override
				@OnlyIn(Dist.CLIENT)
				public HumanoidModel getHumanoidArmorModel(LivingEntity living, ItemStack stack, EquipmentSlot slot, HumanoidModel defaultModel) {
					Modelgoliathfeet model = new Modelgoliathfeet(Minecraft.getInstance().getEntityModels().bakeLayer(Modelgoliathfeet.LAYER_LOCATION));
					HumanoidModel armorModel = new HumanoidModel(new ModelPart(Collections.emptyList(),
							Map.of("left_leg", model.LeftLeg, "right_leg", model.RightLeg, "head", empty(), "hat", empty(), "body", empty(), "right_arm", empty(), "left_arm", empty())));
					copyState(living, defaultModel, armorModel);
					return armorModel;
				}
			});
		}

		@Override
		public void appendHoverText(ItemStack itemstack, Level world, List<Component> list, TooltipFlag flag) {
			super.appendHoverText(itemstack, world, list, flag);
		}

		@Override
		public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
			return "sololeveling:textures/models/armor/goliath_feet.png";
		}

		@Override
		public void inventoryTick(ItemStack itemstack, Level world, Entity entity, int slot, boolean selected) {
			super.inventoryTick(itemstack, world, entity, slot, selected);
			if (entity instanceof Player player && Iterables.contains(player.getArmorSlots(), itemstack))
				GoliathArmorTickProcedure.execute(world, entity, itemstack);
		}
	}

	private static ModelPart empty() {
		return new ModelPart(Collections.emptyList(), Collections.emptyMap());
	}

	private static void copyState(LivingEntity living, HumanoidModel defaultModel, HumanoidModel armorModel) {
		armorModel.crouching = living.isShiftKeyDown();
		armorModel.riding = defaultModel.riding;
		armorModel.young = living.isBaby();
	}
}
