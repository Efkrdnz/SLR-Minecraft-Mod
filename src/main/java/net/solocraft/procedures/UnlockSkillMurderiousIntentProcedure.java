package net.solocraft.procedures;

import net.solocraft.dungeon.runtime.SnowRedGateArenaManager;
import net.solocraft.network.SololevelingModVariables;
import net.solocraft.entity.KangTaeshikEntity;
import net.solocraft.entity.ChoijongEntity;
import net.solocraft.entity.BarukaEntity;
import net.solocraft.entity.BaekYoonhoEntity;
import net.solocraft.util.JobChangeQuestManager;
import net.solocraft.util.VesselManager;
import net.solocraft.world.dimension.rift.RiftTerritory;

import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.Event;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.advancements.AdvancementHolder;

import javax.annotation.Nullable;

@EventBusSubscriber
public class UnlockSkillMurderiousIntentProcedure {
	@SubscribeEvent
	public static void onEntityDeath(LivingDeathEvent event) {
		if (event != null && event.getEntity() != null) {
			execute(event, event.getEntity(), event.getSource().getEntity());
		}
	}

	public static void execute(Entity entity, Entity sourceentity) {
		execute(null, entity, sourceentity);
	}

	private static void execute(@Nullable Event event, Entity entity, Entity sourceentity) {
		if (entity == null || sourceentity == null)
			return;
		if (sourceentity instanceof Player) {
			if (SnowRedGateArenaManager.arenaTerritory(entity).orElse(null) == RiftTerritory.FROST) {
				if (entity instanceof BarukaEntity || (sourceentity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).Level >= 60) {
					if ((sourceentity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).JOB == 0) {
						if (Math.random() < (15) / ((float) 100)) {
							if (sourceentity instanceof ServerPlayer vesselPlayer
									&& VesselManager.assignPlayer(vesselPlayer, VesselManager.MONARCH, "sillad", true) == VesselManager.AssignmentResult.SUCCESS) {
								JobChangeQuestManager.finish(vesselPlayer);
								AdvancementHolder _adv = vesselPlayer.server.getAdvancements().get(ResourceLocation.parse("sololeveling:coldest_monarch"));
								AdvancementProgress _ap = vesselPlayer.getAdvancements().getOrStartProgress(_adv);
								if (!_ap.isDone()) {
									for (String criteria : _ap.getRemainingCriteria())
										vesselPlayer.getAdvancements().award(_adv, criteria);
								}
								vesselPlayer.displayClientMessage(Component.literal("You've been chosen as vessel for the \"Frost Monarch\""), false);
							}
						}
					}
				}
			}
			if ((sourceentity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).Player) {
				if (entity instanceof Player || entity instanceof KangTaeshikEntity || entity instanceof BaekYoonhoEntity || entity instanceof ChoijongEntity) {
					if (!((sourceentity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).Plist).contains("Murderious Intent")) {
						{
							String _setval = (sourceentity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).Plist + "Murderious Intent" + ",";
							sourceentity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
								capability.Plist = _setval;
								capability.syncPlayerVariables(sourceentity);
							});
						}
						if (sourceentity instanceof ServerPlayer _player) {
							AdvancementHolder _adv = _player.server.getAdvancements().get(ResourceLocation.parse("sololeveling:advancement_murderious_intent"));
							AdvancementProgress _ap = _player.getAdvancements().getOrStartProgress(_adv);
							if (!_ap.isDone()) {
								for (String criteria : _ap.getRemainingCriteria())
									_player.getAdvancements().award(_adv, criteria);
							}
						}
					}
				}
			}
		}
	}
}
