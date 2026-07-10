package net.solocraft.procedures;

import net.solocraft.network.SololevelingModVariables;
import net.solocraft.entity.TuskShadowEntity;
import net.solocraft.entity.SteelFangWolfShadowEntity;
import net.solocraft.entity.ShadowSold1Entity;
import net.solocraft.entity.ShadowPolarBearEntity;
import net.solocraft.entity.ShadowHighOrcEntity;
import net.solocraft.entity.ShadowGreenOrcEntity;
import net.solocraft.entity.KamishShadowEntity;
import net.solocraft.entity.IgrisShadowEntity;
import net.solocraft.entity.GoblinMageShadowEntity;
import net.solocraft.entity.GoblinClubShadowEntity;
import net.solocraft.entity.GoblinArcherShadowEntity;
import net.solocraft.entity.BeruShadowEntity;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.event.entity.living.LivingDeathEvent;

import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;

import javax.annotation.Nullable;
import net.solocraft.util.CooldownManager;

@Mod.EventBusSubscriber
public class ShadowDeathReviveProcedure {
	@SubscribeEvent
	public static void onEntityDeath(LivingDeathEvent event) {
		if (event != null && event.getEntity() != null) {
			execute(event, event.getEntity());
		}
	}

	public static void execute(Entity entity) {
		execute(null, entity);
	}

	private static void execute(@Nullable Event event, Entity entity) {
		if (entity == null)
			return;
		double mpconsumption = 0;
		if (entity instanceof TamableAnimal _tamEnt ? _tamEnt.isTame() : false) {
			if (entity instanceof ShadowSold1Entity) {
				mpconsumption = 200;
				if (((entity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null).getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
						.orElse(new SololevelingModVariables.PlayerVariables())).MP >= mpconsumption) {
					if (event != null && event.isCancelable()) {
						event.setCanceled(true);
					}
					{
						double _setval = ((entity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null).getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
								.orElse(new SololevelingModVariables.PlayerVariables())).MP - mpconsumption;
						(entity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null).getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
							capability.MP = _setval;
							capability.syncPlayerVariables((entity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null));
						});
					}
					if (entity instanceof LivingEntity _entity)
						_entity.setHealth(entity instanceof LivingEntity _livEnt ? _livEnt.getMaxHealth() : -1);
					if ((entity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null) instanceof LivingEntity _entity && !_entity.level().isClientSide())
						CooldownManager.set(entity, "mana_refresh", 40);
				} else {
					{
						double _setval = ((entity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null).getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
								.orElse(new SololevelingModVariables.PlayerVariables())).OrdShadow - 1;
						(entity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null).getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
							capability.OrdShadow = _setval;
							capability.syncPlayerVariables((entity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null));
						});
					}
					{
						double _setval = ((entity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null).getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
								.orElse(new SololevelingModVariables.PlayerVariables())).ordshadowmax - 1;
						(entity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null).getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
							capability.ordshadowmax = _setval;
							capability.syncPlayerVariables((entity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null));
						});
					}
					{
						double _setval = ((entity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null).getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
								.orElse(new SololevelingModVariables.PlayerVariables())).shadowstorageusage - 1;
						(entity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null).getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
							capability.shadowstorageusage = _setval;
							capability.syncPlayerVariables((entity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null));
						});
					}
				}
			} else if (entity instanceof GoblinClubShadowEntity) {
				mpconsumption = 500;
				if (((entity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null).getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
						.orElse(new SololevelingModVariables.PlayerVariables())).MP >= mpconsumption) {
					if (event != null && event.isCancelable()) {
						event.setCanceled(true);
					}
					{
						double _setval = ((entity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null).getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
								.orElse(new SololevelingModVariables.PlayerVariables())).MP - mpconsumption;
						(entity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null).getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
							capability.MP = _setval;
							capability.syncPlayerVariables((entity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null));
						});
					}
					if (entity instanceof LivingEntity _entity)
						_entity.setHealth(entity instanceof LivingEntity _livEnt ? _livEnt.getMaxHealth() : -1);
					if ((entity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null) instanceof LivingEntity _entity && !_entity.level().isClientSide())
						CooldownManager.set(entity, "mana_refresh", 40);
				} else {
					{
						double _setval = ((entity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null).getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
								.orElse(new SololevelingModVariables.PlayerVariables())).GobShadow - 1;
						(entity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null).getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
							capability.GobShadow = _setval;
							capability.syncPlayerVariables((entity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null));
						});
					}
					{
						double _setval = ((entity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null).getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
								.orElse(new SololevelingModVariables.PlayerVariables())).GobShadowMax - 1;
						(entity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null).getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
							capability.GobShadowMax = _setval;
							capability.syncPlayerVariables((entity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null));
						});
					}
					{
						double _setval = ((entity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null).getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
								.orElse(new SololevelingModVariables.PlayerVariables())).shadowstorageusage - 1;
						(entity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null).getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
							capability.shadowstorageusage = _setval;
							capability.syncPlayerVariables((entity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null));
						});
					}
				}
			} else if (entity instanceof GoblinArcherShadowEntity) {
				mpconsumption = 500;
				if (((entity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null).getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
						.orElse(new SololevelingModVariables.PlayerVariables())).MP >= mpconsumption) {
					if (event != null && event.isCancelable()) {
						event.setCanceled(true);
					}
					{
						double _setval = ((entity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null).getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
								.orElse(new SololevelingModVariables.PlayerVariables())).MP - mpconsumption;
						(entity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null).getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
							capability.MP = _setval;
							capability.syncPlayerVariables((entity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null));
						});
					}
					if (entity instanceof LivingEntity _entity)
						_entity.setHealth(entity instanceof LivingEntity _livEnt ? _livEnt.getMaxHealth() : -1);
					if ((entity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null) instanceof LivingEntity _entity && !_entity.level().isClientSide())
						CooldownManager.set(entity, "mana_refresh", 40);
				} else {
					{
						double _setval = ((entity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null).getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
								.orElse(new SololevelingModVariables.PlayerVariables())).ShadowGoblinArcherAmount - 1;
						(entity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null).getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
							capability.ShadowGoblinArcherAmount = _setval;
							capability.syncPlayerVariables((entity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null));
						});
					}
					{
						double _setval = ((entity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null).getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
								.orElse(new SololevelingModVariables.PlayerVariables())).ShadowGoblinArcherMax - 1;
						(entity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null).getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
							capability.ShadowGoblinArcherMax = _setval;
							capability.syncPlayerVariables((entity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null));
						});
					}
					{
						double _setval = ((entity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null).getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
								.orElse(new SololevelingModVariables.PlayerVariables())).shadowstorageusage - 1;
						(entity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null).getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
							capability.shadowstorageusage = _setval;
							capability.syncPlayerVariables((entity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null));
						});
					}
				}
			} else if (entity instanceof GoblinMageShadowEntity) {
				mpconsumption = 500;
				if (((entity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null).getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
						.orElse(new SololevelingModVariables.PlayerVariables())).MP >= mpconsumption) {
					if (event != null && event.isCancelable()) {
						event.setCanceled(true);
					}
					{
						double _setval = ((entity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null).getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
								.orElse(new SololevelingModVariables.PlayerVariables())).MP - mpconsumption;
						(entity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null).getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
							capability.MP = _setval;
							capability.syncPlayerVariables((entity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null));
						});
					}
					if (entity instanceof LivingEntity _entity)
						_entity.setHealth(entity instanceof LivingEntity _livEnt ? _livEnt.getMaxHealth() : -1);
					if ((entity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null) instanceof LivingEntity _entity && !_entity.level().isClientSide())
						CooldownManager.set(entity, "mana_refresh", 40);
				} else {
					{
						double _setval = ((entity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null).getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
								.orElse(new SololevelingModVariables.PlayerVariables())).ShadowGoblinMageAmount - 1;
						(entity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null).getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
							capability.ShadowGoblinMageAmount = _setval;
							capability.syncPlayerVariables((entity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null));
						});
					}
					{
						double _setval = ((entity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null).getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
								.orElse(new SololevelingModVariables.PlayerVariables())).ShadowGoblinMageMax - 1;
						(entity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null).getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
							capability.ShadowGoblinMageMax = _setval;
							capability.syncPlayerVariables((entity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null));
						});
					}
					{
						double _setval = ((entity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null).getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
								.orElse(new SololevelingModVariables.PlayerVariables())).shadowstorageusage - 1;
						(entity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null).getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
							capability.shadowstorageusage = _setval;
							capability.syncPlayerVariables((entity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null));
						});
					}
				}
			} else if (entity instanceof SteelFangWolfShadowEntity) {
				mpconsumption = 100;
				if (((entity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null).getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
						.orElse(new SololevelingModVariables.PlayerVariables())).MP >= mpconsumption) {
					if (event != null && event.isCancelable()) {
						event.setCanceled(true);
					}
					{
						double _setval = ((entity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null).getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
								.orElse(new SololevelingModVariables.PlayerVariables())).MP - mpconsumption;
						(entity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null).getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
							capability.MP = _setval;
							capability.syncPlayerVariables((entity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null));
						});
					}
					if (entity instanceof LivingEntity _entity)
						_entity.setHealth(entity instanceof LivingEntity _livEnt ? _livEnt.getMaxHealth() : -1);
					if ((entity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null) instanceof LivingEntity _entity && !_entity.level().isClientSide())
						CooldownManager.set(entity, "mana_refresh", 40);
				} else {
					{
						double _setval = ((entity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null).getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
								.orElse(new SololevelingModVariables.PlayerVariables())).WolfShadow - 1;
						(entity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null).getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
							capability.WolfShadow = _setval;
							capability.syncPlayerVariables((entity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null));
						});
					}
					{
						double _setval = ((entity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null).getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
								.orElse(new SololevelingModVariables.PlayerVariables())).WolfShadowMax - 1;
						(entity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null).getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
							capability.WolfShadowMax = _setval;
							capability.syncPlayerVariables((entity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null));
						});
					}
					{
						double _setval = ((entity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null).getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
								.orElse(new SololevelingModVariables.PlayerVariables())).shadowstorageusage - 1;
						(entity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null).getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
							capability.shadowstorageusage = _setval;
							capability.syncPlayerVariables((entity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null));
						});
					}
				}
			} else if (entity instanceof ShadowGreenOrcEntity) {
				mpconsumption = 300;
				if (((entity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null).getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
						.orElse(new SololevelingModVariables.PlayerVariables())).MP >= mpconsumption) {
					if (event != null && event.isCancelable()) {
						event.setCanceled(true);
					}
					{
						double _setval = ((entity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null).getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
								.orElse(new SololevelingModVariables.PlayerVariables())).MP - mpconsumption;
						(entity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null).getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
							capability.MP = _setval;
							capability.syncPlayerVariables((entity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null));
						});
					}
					if (entity instanceof LivingEntity _entity)
						_entity.setHealth(entity instanceof LivingEntity _livEnt ? _livEnt.getMaxHealth() : -1);
					if ((entity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null) instanceof LivingEntity _entity && !_entity.level().isClientSide())
						CooldownManager.set(entity, "mana_refresh", 40);
				} else {
					{
						double _setval = ((entity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null).getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
								.orElse(new SololevelingModVariables.PlayerVariables())).orcspawned - 1;
						(entity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null).getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
							capability.orcspawned = _setval;
							capability.syncPlayerVariables((entity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null));
						});
					}
					{
						double _setval = ((entity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null).getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
								.orElse(new SololevelingModVariables.PlayerVariables())).orcmax - 1;
						(entity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null).getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
							capability.orcmax = _setval;
							capability.syncPlayerVariables((entity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null));
						});
					}
					{
						double _setval = ((entity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null).getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
								.orElse(new SololevelingModVariables.PlayerVariables())).shadowstorageusage - 1;
						(entity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null).getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
							capability.shadowstorageusage = _setval;
							capability.syncPlayerVariables((entity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null));
						});
					}
				}
			} else if (entity instanceof ShadowHighOrcEntity) {
				mpconsumption = 500;
				if (((entity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null).getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
						.orElse(new SololevelingModVariables.PlayerVariables())).MP >= mpconsumption) {
					if (event != null && event.isCancelable()) {
						event.setCanceled(true);
					}
					{
						double _setval = ((entity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null).getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
								.orElse(new SololevelingModVariables.PlayerVariables())).MP - mpconsumption;
						(entity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null).getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
							capability.MP = _setval;
							capability.syncPlayerVariables((entity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null));
						});
					}
					if (entity instanceof LivingEntity _entity)
						_entity.setHealth(entity instanceof LivingEntity _livEnt ? _livEnt.getMaxHealth() : -1);
					if ((entity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null) instanceof LivingEntity _entity && !_entity.level().isClientSide())
						CooldownManager.set(entity, "mana_refresh", 40);
				} else {
					{
						double _setval = ((entity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null).getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
								.orElse(new SololevelingModVariables.PlayerVariables())).highorcspawned - 1;
						(entity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null).getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
							capability.highorcspawned = _setval;
							capability.syncPlayerVariables((entity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null));
						});
					}
					{
						double _setval = ((entity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null).getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
								.orElse(new SololevelingModVariables.PlayerVariables())).highorcmax - 1;
						(entity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null).getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
							capability.highorcmax = _setval;
							capability.syncPlayerVariables((entity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null));
						});
					}
					{
						double _setval = ((entity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null).getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
								.orElse(new SololevelingModVariables.PlayerVariables())).shadowstorageusage - 1;
						(entity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null).getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
							capability.shadowstorageusage = _setval;
							capability.syncPlayerVariables((entity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null));
						});
					}
				}
			} else if (entity instanceof ShadowPolarBearEntity) {
				mpconsumption = 200;
				if (((entity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null).getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
						.orElse(new SololevelingModVariables.PlayerVariables())).MP >= mpconsumption) {
					if (event != null && event.isCancelable()) {
						event.setCanceled(true);
					}
					{
						double _setval = ((entity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null).getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
								.orElse(new SololevelingModVariables.PlayerVariables())).MP - mpconsumption;
						(entity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null).getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
							capability.MP = _setval;
							capability.syncPlayerVariables((entity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null));
						});
					}
					if (entity instanceof LivingEntity _entity)
						_entity.setHealth(entity instanceof LivingEntity _livEnt ? _livEnt.getMaxHealth() : -1);
					if ((entity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null) instanceof LivingEntity _entity && !_entity.level().isClientSide())
						CooldownManager.set(entity, "mana_refresh", 40);
				} else {
					{
						double _setval = ((entity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null).getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
								.orElse(new SololevelingModVariables.PlayerVariables())).polarbear - 1;
						(entity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null).getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
							capability.polarbear = _setval;
							capability.syncPlayerVariables((entity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null));
						});
					}
					{
						double _setval = ((entity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null).getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
								.orElse(new SololevelingModVariables.PlayerVariables())).polarbearmax - 1;
						(entity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null).getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
							capability.polarbearmax = _setval;
							capability.syncPlayerVariables((entity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null));
						});
					}
					{
						double _setval = ((entity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null).getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
								.orElse(new SololevelingModVariables.PlayerVariables())).shadowstorageusage - 1;
						(entity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null).getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
							capability.shadowstorageusage = _setval;
							capability.syncPlayerVariables((entity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null));
						});
					}
				}
			} else if (entity instanceof IgrisShadowEntity) {
				mpconsumption = 5000;
				if (((entity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null).getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
						.orElse(new SololevelingModVariables.PlayerVariables())).MP >= mpconsumption) {
					if (event != null && event.isCancelable()) {
						event.setCanceled(true);
					}
					{
						double _setval = ((entity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null).getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
								.orElse(new SololevelingModVariables.PlayerVariables())).MP - mpconsumption;
						(entity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null).getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
							capability.MP = _setval;
							capability.syncPlayerVariables((entity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null));
						});
					}
					if (entity instanceof LivingEntity _entity)
						_entity.setHealth(entity instanceof LivingEntity _livEnt ? _livEnt.getMaxHealth() : -1);
					if ((entity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null) instanceof LivingEntity _entity && !_entity.level().isClientSide())
						CooldownManager.set(entity, "mana_refresh", 200);
				} else {
					{
						double _setval = ((entity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null).getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
								.orElse(new SololevelingModVariables.PlayerVariables())).IgrisSpawned - 1;
						(entity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null).getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
							capability.IgrisSpawned = _setval;
							capability.syncPlayerVariables((entity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null));
						});
					}
				}
			} else if (entity instanceof BeruShadowEntity) {
				mpconsumption = 10000;
				if (((entity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null).getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
						.orElse(new SololevelingModVariables.PlayerVariables())).MP >= mpconsumption) {
					if (event != null && event.isCancelable()) {
						event.setCanceled(true);
					}
					{
						double _setval = ((entity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null).getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
								.orElse(new SololevelingModVariables.PlayerVariables())).MP - mpconsumption;
						(entity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null).getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
							capability.MP = _setval;
							capability.syncPlayerVariables((entity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null));
						});
					}
					if (entity instanceof LivingEntity _entity)
						_entity.setHealth(entity instanceof LivingEntity _livEnt ? _livEnt.getMaxHealth() : -1);
					if ((entity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null) instanceof LivingEntity _entity && !_entity.level().isClientSide())
						CooldownManager.set(entity, "mana_refresh", 40);
				} else {
					{
						double _setval = ((entity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null).getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
								.orElse(new SololevelingModVariables.PlayerVariables())).beru - 1;
						(entity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null).getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
							capability.beru = _setval;
							capability.syncPlayerVariables((entity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null));
						});
					}
				}
			} else if (entity instanceof KamishShadowEntity) {
				mpconsumption = 20000;
				if (((entity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null).getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
						.orElse(new SololevelingModVariables.PlayerVariables())).MP >= mpconsumption) {
					if (event != null && event.isCancelable()) {
						event.setCanceled(true);
					}
					{
						double _setval = ((entity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null).getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
								.orElse(new SololevelingModVariables.PlayerVariables())).MP - mpconsumption;
						(entity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null).getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
							capability.MP = _setval;
							capability.syncPlayerVariables((entity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null));
						});
					}
					if (entity instanceof LivingEntity _entity)
						_entity.setHealth(entity instanceof LivingEntity _livEnt ? _livEnt.getMaxHealth() : -1);
					if ((entity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null) instanceof LivingEntity _entity && !_entity.level().isClientSide())
						CooldownManager.set(entity, "mana_refresh", 40);
				} else {
					{
						double _setval = ((entity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null).getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
								.orElse(new SololevelingModVariables.PlayerVariables())).shadowdragonnum - 1;
						(entity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null).getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
							capability.shadowdragonnum = _setval;
							capability.syncPlayerVariables((entity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null));
						});
					}
				}
			} else if (entity instanceof TuskShadowEntity) {
				mpconsumption = 6000;
				if (((entity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null).getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
						.orElse(new SololevelingModVariables.PlayerVariables())).MP >= mpconsumption) {
					if (event != null && event.isCancelable()) {
						event.setCanceled(true);
					}
					{
						double _setval = ((entity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null).getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
								.orElse(new SololevelingModVariables.PlayerVariables())).MP - mpconsumption;
						(entity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null).getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
							capability.MP = _setval;
							capability.syncPlayerVariables((entity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null));
						});
					}
					if (entity instanceof LivingEntity _entity)
						_entity.setHealth(entity instanceof LivingEntity _livEnt ? _livEnt.getMaxHealth() : -1);
					if ((entity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null) instanceof LivingEntity _entity && !_entity.level().isClientSide())
						CooldownManager.set(entity, "mana_refresh", 40);
				} else {
					{
						double _setval = ((entity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null).getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
								.orElse(new SololevelingModVariables.PlayerVariables())).tuskspawned - 1;
						(entity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null).getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
							capability.tuskspawned = _setval;
							capability.syncPlayerVariables((entity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null));
						});
					}
				}
			}
		}
	}
}
