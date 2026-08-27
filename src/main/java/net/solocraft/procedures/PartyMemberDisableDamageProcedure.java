package net.solocraft.procedures;

import net.solocraft.network.SololevelingModVariables;

import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.ICancellableEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.Entity;
import net.minecraft.sounds.SoundSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;

import javax.annotation.Nullable;

@EventBusSubscriber
public class PartyMemberDisableDamageProcedure {
	@SubscribeEvent
	public static void onEntityAttacked(LivingIncomingDamageEvent event) {
		Entity entity = event.getEntity();
		if (event != null && entity != null) {
			execute(event, entity.level(), entity.getX(), entity.getY(), entity.getZ(), entity, event.getSource().getEntity());
		}
	}

	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity, Entity sourceentity) {
		execute(null, world, x, y, z, entity, sourceentity);
	}

	private static void execute(@Nullable ICancellableEvent event, LevelAccessor world, double x, double y, double z, Entity entity, Entity sourceentity) {
		if (entity == null || sourceentity == null)
			return;
		if (!((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).party).equals("")) {
			if (((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).party)
					.equals((sourceentity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).party)) {
				if (sourceentity instanceof Player _player && !_player.level().isClientSide())
					_player.displayClientMessage(Component.literal(("\u00A7e" + entity.getDisplayName().getString() + " \u00A7fIs your party member!")), true);
				if (world instanceof Level _level) {
					if (_level.isClientSide()) {
						_level.playLocalSound(x, y, z, BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("entity.experience_orb.pickup")), SoundSource.NEUTRAL, (float) 0.6, 1, false);
					}
				}
				if (event != null) {
					event.setCanceled(true);
				}
			}
			if (entity instanceof TamableAnimal _tamEnt ? _tamEnt.isTame() : false) {
				if (!((entity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null) == null)) {
					if ((((entity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null).getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).party)
							.equals((sourceentity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).party)) {
						if (sourceentity instanceof Player _player && !_player.level().isClientSide())
							_player.displayClientMessage(Component.literal(("\u00A7fThis pet belongs to your party member " + "\u00A7e" + ((entity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null).getDisplayName().getString()))),
									true);
						if (world instanceof Level _level) {
							if (_level.isClientSide()) {
								_level.playLocalSound(x, y, z, BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("entity.experience_orb.pickup")), SoundSource.NEUTRAL, (float) 0.6, 1, false);
							}
						}
						if (event != null) {
							event.setCanceled(true);
						}
					}
				}
			}
		}
	}
}
