package net.solocraft.procedures;

import net.solocraft.entity.HunterEntity;

import net.minecraftforge.registries.ForgeRegistries;

import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.InteractionHand;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Mth;
import net.minecraft.tags.ItemTags;
import net.minecraft.resources.ResourceLocation;

public class HunterOnInitialEntitySpawnProcedure {
	public static void execute(Entity entity) {
		if (entity == null)
			return;
		double rnk = 0;
		if (entity instanceof HunterEntity _datEntSetI)
			_datEntSetI.getEntityData().set(HunterEntity.DATA_Eyes, Mth.nextInt(RandomSource.create(), 1, 8));
		if (entity instanceof HunterEntity _datEntSetI)
			_datEntSetI.getEntityData().set(HunterEntity.DATA_TopIn, Mth.nextInt(RandomSource.create(), 1, 4));
		if (entity instanceof HunterEntity _datEntSetI)
			_datEntSetI.getEntityData().set(HunterEntity.DATA_TopOut, Mth.nextInt(RandomSource.create(), 1, 15));
		if (entity instanceof HunterEntity _datEntSetI)
			_datEntSetI.getEntityData().set(HunterEntity.DATA_Foot, Mth.nextInt(RandomSource.create(), 1, 4));
		if (entity instanceof HunterEntity _datEntSetI)
			_datEntSetI.getEntityData().set(HunterEntity.DATA_EyeBs, Mth.nextInt(RandomSource.create(), 1, 2));
		if (entity instanceof HunterEntity _datEntSetI)
			_datEntSetI.getEntityData().set(HunterEntity.DATA_Hair, Mth.nextInt(RandomSource.create(), 1, 8));
		if (entity instanceof HunterEntity _datEntSetI)
			_datEntSetI.getEntityData().set(HunterEntity.DATA_Mouth, Mth.nextInt(RandomSource.create(), 1, 2));
		if (entity instanceof HunterEntity _datEntSetI)
			_datEntSetI.getEntityData().set(HunterEntity.DATA_Bottom, Mth.nextInt(RandomSource.create(), 1, 5));
		if (Math.random() < (1) / ((float) 4)) {
			if (entity instanceof HunterEntity _datEntSetS)
				_datEntSetS.getEntityData().set(HunterEntity.DATA_Rank, "C");
			rnk = 2;
		} else {
			if (Math.random() < (1) / ((float) 4)) {
				if (entity instanceof HunterEntity _datEntSetS)
					_datEntSetS.getEntityData().set(HunterEntity.DATA_Rank, "B");
				rnk = 3;
			} else {
				if (Math.random() < (1) / ((float) 4)) {
					if (entity instanceof HunterEntity _datEntSetS)
						_datEntSetS.getEntityData().set(HunterEntity.DATA_Rank, "A");
					rnk = 4;
				} else {
					if (Math.random() < (1) / ((float) 4)) {
						if (entity instanceof HunterEntity _datEntSetS)
							_datEntSetS.getEntityData().set(HunterEntity.DATA_Rank, "S");
						rnk = 5;
					} else {
						if (entity instanceof HunterEntity _datEntSetS)
							_datEntSetS.getEntityData().set(HunterEntity.DATA_Rank, "D");
						rnk = 1;
					}
				}
			}
		}
		if (Math.random() < (1) / ((float) 6)) {
			if (entity instanceof HunterEntity _datEntSetS)
				_datEntSetS.getEntityData().set(HunterEntity.DATA_HunterClass, "Assassin");
			if (entity instanceof LivingEntity _entity) {
				ItemStack _setstack = new ItemStack((ForgeRegistries.ITEMS.tags().getTag(ItemTags.create(new ResourceLocation("hunter_assassin"))).getRandomElement(RandomSource.create()).orElseGet(() -> Items.AIR)));
				_setstack.setCount(1);
				_entity.setItemInHand(InteractionHand.MAIN_HAND, _setstack);
				if (_entity instanceof Player _player)
					_player.getInventory().setChanged();
			}
			if (Math.random() < (1) / ((float) 3)) {
				if (entity instanceof LivingEntity _entity) {
					ItemStack _setstack = new ItemStack((ForgeRegistries.ITEMS.tags().getTag(ItemTags.create(new ResourceLocation("hunter_assassin"))).getRandomElement(RandomSource.create()).orElseGet(() -> Items.AIR)));
					_setstack.setCount(1);
					_entity.setItemInHand(InteractionHand.OFF_HAND, _setstack);
					if (_entity instanceof Player _player)
						_player.getInventory().setChanged();
				}
			}
			((LivingEntity) entity).getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE).setBaseValue((2 + rnk * 1.25));
			((LivingEntity) entity).getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue((20 + rnk * 5));
			((LivingEntity) entity).getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR).setBaseValue((rnk * 4));
			((LivingEntity) entity).getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED).setBaseValue((0.25 + rnk * 0.1));
			if (entity instanceof LivingEntity _entity)
				_entity.setHealth(entity instanceof LivingEntity _livEnt ? _livEnt.getMaxHealth() : -1);
		} else {
			if (Math.random() < (1) / ((float) 5)) {
				if (entity instanceof HunterEntity _datEntSetS)
					_datEntSetS.getEntityData().set(HunterEntity.DATA_HunterClass, "Mage");
				if (entity instanceof LivingEntity _entity) {
					ItemStack _setstack = new ItemStack((ForgeRegistries.ITEMS.tags().getTag(ItemTags.create(new ResourceLocation("hunter_mage_healer"))).getRandomElement(RandomSource.create()).orElseGet(() -> Items.AIR)));
					_setstack.setCount(1);
					_entity.setItemInHand(InteractionHand.OFF_HAND, _setstack);
					if (_entity instanceof Player _player)
						_player.getInventory().setChanged();
				}
				((LivingEntity) entity).getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue((20 + rnk * 10));
				((LivingEntity) entity).getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR).setBaseValue((rnk * 5));
				((LivingEntity) entity).getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED).setBaseValue((0.3 + rnk * 0.025));
				if (entity instanceof LivingEntity _entity)
					_entity.setHealth(entity instanceof LivingEntity _livEnt ? _livEnt.getMaxHealth() : -1);
			} else {
				if (Math.random() < (1) / ((float) 4)) {
					if (entity instanceof HunterEntity _datEntSetS)
						_datEntSetS.getEntityData().set(HunterEntity.DATA_HunterClass, "Fighter");
					if (entity instanceof LivingEntity _entity) {
						ItemStack _setstack = new ItemStack((ForgeRegistries.ITEMS.tags().getTag(ItemTags.create(new ResourceLocation("hunter_fighter"))).getRandomElement(RandomSource.create()).orElseGet(() -> Items.AIR)));
						_setstack.setCount(1);
						_entity.setItemInHand(InteractionHand.MAIN_HAND, _setstack);
						if (_entity instanceof Player _player)
							_player.getInventory().setChanged();
					}
					if (entity instanceof LivingEntity _entity) {
						ItemStack _setstack = new ItemStack((ForgeRegistries.ITEMS.tags().getTag(ItemTags.create(new ResourceLocation("hunter_fighter_tanker_off"))).getRandomElement(RandomSource.create()).orElseGet(() -> Items.AIR)));
						_setstack.setCount(1);
						_entity.setItemInHand(InteractionHand.OFF_HAND, _setstack);
						if (_entity instanceof Player _player)
							_player.getInventory().setChanged();
					}
					((LivingEntity) entity).getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE).setBaseValue((2 + rnk * 1.75));
					((LivingEntity) entity).getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue((20 + rnk * 10));
					((LivingEntity) entity).getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR).setBaseValue((rnk * 5));
					((LivingEntity) entity).getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED).setBaseValue((0.3 + rnk * 0.05));
					if (entity instanceof LivingEntity _entity)
						_entity.setHealth(entity instanceof LivingEntity _livEnt ? _livEnt.getMaxHealth() : -1);
				} else {
					if (Math.random() < (1) / ((float) 3)) {
						if (entity instanceof HunterEntity _datEntSetS)
							_datEntSetS.getEntityData().set(HunterEntity.DATA_HunterClass, "Tanker");
						if (entity instanceof LivingEntity _entity) {
							ItemStack _setstack = new ItemStack((ForgeRegistries.ITEMS.tags().getTag(ItemTags.create(new ResourceLocation("hunter_tanker"))).getRandomElement(RandomSource.create()).orElseGet(() -> Items.AIR)));
							_setstack.setCount(1);
							_entity.setItemInHand(InteractionHand.MAIN_HAND, _setstack);
							if (_entity instanceof Player _player)
								_player.getInventory().setChanged();
						}
						if (entity instanceof LivingEntity _entity) {
							ItemStack _setstack = new ItemStack((ForgeRegistries.ITEMS.tags().getTag(ItemTags.create(new ResourceLocation("hunter_fighter_tanker_off"))).getRandomElement(RandomSource.create()).orElseGet(() -> Items.AIR)));
							_setstack.setCount(1);
							_entity.setItemInHand(InteractionHand.OFF_HAND, _setstack);
							if (_entity instanceof Player _player)
								_player.getInventory().setChanged();
						}
						((LivingEntity) entity).getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE).setBaseValue((2 + rnk * 1));
						((LivingEntity) entity).getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue((20 + rnk * 15));
						((LivingEntity) entity).getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR).setBaseValue((rnk * 8));
						((LivingEntity) entity).getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED).setBaseValue((0.3 + rnk * 0.02));
						if (entity instanceof LivingEntity _entity)
							_entity.setHealth(entity instanceof LivingEntity _livEnt ? _livEnt.getMaxHealth() : -1);
					} else {
						if (Math.random() < (1) / ((float) 2)) {
							if (entity instanceof HunterEntity _datEntSetS)
								_datEntSetS.getEntityData().set(HunterEntity.DATA_HunterClass, "Ranger");
							if (entity instanceof LivingEntity _entity) {
								ItemStack _setstack = new ItemStack((ForgeRegistries.ITEMS.tags().getTag(ItemTags.create(new ResourceLocation("hunter_ranger"))).getRandomElement(RandomSource.create()).orElseGet(() -> Items.AIR)));
								_setstack.setCount(1);
								_entity.setItemInHand(InteractionHand.MAIN_HAND, _setstack);
								if (_entity instanceof Player _player)
									_player.getInventory().setChanged();
							}
							((LivingEntity) entity).getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue((20 + rnk * 12));
							((LivingEntity) entity).getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR).setBaseValue((rnk * 8));
							((LivingEntity) entity).getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED).setBaseValue((0.3 + rnk * 0.04));
							if (entity instanceof LivingEntity _entity)
								_entity.setHealth(entity instanceof LivingEntity _livEnt ? _livEnt.getMaxHealth() : -1);
						} else {
							if (entity instanceof HunterEntity _datEntSetS)
								_datEntSetS.getEntityData().set(HunterEntity.DATA_HunterClass, "Healer");
							if (entity instanceof LivingEntity _entity) {
								ItemStack _setstack = new ItemStack((ForgeRegistries.ITEMS.tags().getTag(ItemTags.create(new ResourceLocation("hunter_mage_healer"))).getRandomElement(RandomSource.create()).orElseGet(() -> Items.AIR)));
								_setstack.setCount(1);
								_entity.setItemInHand(InteractionHand.OFF_HAND, _setstack);
								if (_entity instanceof Player _player)
									_player.getInventory().setChanged();
							}
							((LivingEntity) entity).getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue((20 + rnk * 7));
							((LivingEntity) entity).getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR).setBaseValue((rnk * 5));
							((LivingEntity) entity).getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED).setBaseValue((0.3 + rnk * 0.01));
							if (entity instanceof LivingEntity _entity)
								_entity.setHealth(entity instanceof LivingEntity _livEnt ? _livEnt.getMaxHealth() : -1);
						}
					}
				}
			}
		}
		entity.getPersistentData().putDouble("int", (rnk * 12));
	}
}
