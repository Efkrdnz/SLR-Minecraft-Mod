package net.solocraft.procedures;

import net.solocraft.SololevelingMod;
import net.solocraft.init.SololevelingModItems;
import net.solocraft.network.SololevelingModVariables;

import net.minecraftforge.registries.ForgeRegistries;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;

public class GoliathManifestationProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		if (entity == null)
			return;
		SololevelingModVariables.PlayerVariables vars = entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables());
		if (vars.JOB != 5)
			return;
		if (isWearingFullGoliath(entity)) {
			restoreArmor(entity);
			return;
		}
		if (isWearingAnyGoliath(entity))
			return;
		if (vars.MP < 500) {
			if (entity instanceof Player player && !player.level().isClientSide())
				player.displayClientMessage(Component.literal("You dont have enough MP"), true);
			return;
		}
		saveArmor(entity);
		playStartSound(world, x, y, z);
		SololevelingMod.queueServerWork(5, () -> {
			equip(entity, EquipmentSlot.FEET, SololevelingModItems.GOLIATH_ARMOR_BOOTS.get());
			equip(entity, EquipmentSlot.LEGS, SololevelingModItems.GOLIATH_ARMOR_LEGGINGS.get());
			equip(entity, EquipmentSlot.CHEST, SololevelingModItems.GOLIATH_ARMOR_CHESTPLATE.get());
			equip(entity, EquipmentSlot.HEAD, SololevelingModItems.GOLIATH_ARMOR_HELMET.get());
		});
	}

	private static void saveArmor(Entity entity) {
		entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
			capability.overridehead = get(entity, EquipmentSlot.HEAD).copy();
			capability.overridetorso = get(entity, EquipmentSlot.CHEST).copy();
			capability.overridelegs = get(entity, EquipmentSlot.LEGS).copy();
			capability.overridefeet = get(entity, EquipmentSlot.FEET).copy();
			capability.syncPlayerVariables(entity);
		});
	}

	private static void restoreArmor(Entity entity) {
		SololevelingModVariables.PlayerVariables vars = entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables());
		set(entity, EquipmentSlot.FEET, vars.overridefeet);
		set(entity, EquipmentSlot.LEGS, vars.overridelegs);
		set(entity, EquipmentSlot.CHEST, vars.overridetorso);
		set(entity, EquipmentSlot.HEAD, vars.overridehead);
	}

	private static boolean isWearingFullGoliath(Entity entity) {
		return is(entity, EquipmentSlot.HEAD, SololevelingModItems.GOLIATH_ARMOR_HELMET.get())
				&& is(entity, EquipmentSlot.CHEST, SololevelingModItems.GOLIATH_ARMOR_CHESTPLATE.get())
				&& is(entity, EquipmentSlot.LEGS, SololevelingModItems.GOLIATH_ARMOR_LEGGINGS.get())
				&& is(entity, EquipmentSlot.FEET, SololevelingModItems.GOLIATH_ARMOR_BOOTS.get());
	}

	private static boolean isWearingAnyGoliath(Entity entity) {
		return is(entity, EquipmentSlot.HEAD, SololevelingModItems.GOLIATH_ARMOR_HELMET.get())
				|| is(entity, EquipmentSlot.CHEST, SololevelingModItems.GOLIATH_ARMOR_CHESTPLATE.get())
				|| is(entity, EquipmentSlot.LEGS, SololevelingModItems.GOLIATH_ARMOR_LEGGINGS.get())
				|| is(entity, EquipmentSlot.FEET, SololevelingModItems.GOLIATH_ARMOR_BOOTS.get());
	}

	private static boolean is(Entity entity, EquipmentSlot slot, Item item) {
		return get(entity, slot).getItem() == item;
	}

	private static ItemStack get(Entity entity, EquipmentSlot slot) {
		return entity instanceof LivingEntity living ? living.getItemBySlot(slot) : ItemStack.EMPTY;
	}

	private static void equip(Entity entity, EquipmentSlot slot, Item item) {
		set(entity, slot, new ItemStack(item));
	}

	private static void set(Entity entity, EquipmentSlot slot, ItemStack stack) {
		if (entity instanceof Player player) {
			player.getInventory().armor.set(slot.getIndex(), stack.copy());
			player.getInventory().setChanged();
		} else if (entity instanceof LivingEntity living) {
			living.setItemSlot(slot, stack.copy());
		}
	}

	private static void playStartSound(LevelAccessor world, double x, double y, double z) {
		if (world instanceof Level level) {
			if (!level.isClientSide())
				level.playSound(null, BlockPos.containing(x, y, z), ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("block.beacon.activate")), SoundSource.NEUTRAL, 1.0f, 0.8f);
			else
				level.playLocalSound(x, y, z, ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("block.beacon.activate")), SoundSource.NEUTRAL, 1.0f, 0.8f, false);
		}
	}

}
