package net.solocraft.procedures;

import net.solocraft.network.SololevelingModVariables;
import net.solocraft.init.SololevelingModItems;
import net.solocraft.init.SololevelingModGameRules;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.commands.CommandFunction;

import java.util.Optional;

public class ShadowMonarchRightclickedProcedure {
	public static void execute(LevelAccessor world, Entity entity) {
		if (entity == null)
			return;
		if (SololevelingModVariables.MapVariables.get(world).shmlimit < (world.getLevelData().getGameRules().getInt(SololevelingModGameRules.SOLO_LEVELING_MONARCH_LIMIT))) {
			if ((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).JOB != 1) {
				{
					double _setval = 1;
					entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
						capability.JOB = _setval;
						capability.syncPlayerVariables(entity);
					});
				}
				SololevelingModVariables.MapVariables.get(world).shmlimit = SololevelingModVariables.MapVariables.get(world).shmlimit + 1;
				SololevelingModVariables.MapVariables.get(world).syncData(world);
				{
					Entity _ent = entity;
					if (!_ent.level().isClientSide() && _ent.getServer() != null) {
						Optional<CommandFunction> _fopt = _ent.getServer().getFunctions().get(new ResourceLocation("mod:purple_lightning_23"));
						if (_fopt.isPresent())
							_ent.getServer().getFunctions().execute(_fopt.get(), _ent.createCommandSourceStack());
					}
				}
			}
		}
		if ((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).JOB == 1) {
			if ((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == SololevelingModItems.GIVE_BERU.get()
					|| (entity instanceof LivingEntity _livEnt ? _livEnt.getOffhandItem() : ItemStack.EMPTY).getItem() == SololevelingModItems.GIVE_BERU.get()) {
				{
					double _setval = 1;
					entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
						capability.berumax = _setval;
						capability.syncPlayerVariables(entity);
					});
				}
			} else if ((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == SololevelingModItems.GIVE_IGRIS.get()
					|| (entity instanceof LivingEntity _livEnt ? _livEnt.getOffhandItem() : ItemStack.EMPTY).getItem() == SololevelingModItems.GIVE_IGRIS.get()) {
				{
					double _setval = 1;
					entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
						capability.igris = _setval;
						capability.syncPlayerVariables(entity);
					});
				}
			} else if ((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == SololevelingModItems.GIVE_KAMISH.get()
					|| (entity instanceof LivingEntity _livEnt ? _livEnt.getOffhandItem() : ItemStack.EMPTY).getItem() == SololevelingModItems.GIVE_KAMISH.get()) {
				{
					double _setval = 1;
					entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
						capability.shadowdragonmax = _setval;
						capability.syncPlayerVariables(entity);
					});
				}
			} else if ((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == SololevelingModItems.GIVE_TUSK.get()
					|| (entity instanceof LivingEntity _livEnt ? _livEnt.getOffhandItem() : ItemStack.EMPTY).getItem() == SololevelingModItems.GIVE_TUSK.get()) {
				{
					double _setval = 1;
					entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
						capability.tuskmax = _setval;
						capability.syncPlayerVariables(entity);
					});
				}
			}
		}
	}
}
