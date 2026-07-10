package net.solocraft.procedures;

import net.solocraft.network.SololevelingModVariables;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.event.TickEvent;

import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.advancements.Advancement;

import javax.annotation.Nullable;

@Mod.EventBusSubscriber
public class ShadowExtractionShowProcedure {
	@SubscribeEvent
	public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
		if (event.phase == TickEvent.Phase.END) {
			execute(event, event.player);
		}
	}

	public static void execute(Entity entity) {
		execute(null, entity);
	}

	private static void execute(@Nullable Event event, Entity entity) {
		if (entity == null)
			return;
		if ((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).JOB == 1) {
			if (!(entity instanceof ServerPlayer _plr0 && _plr0.level() instanceof ServerLevel
					&& _plr0.getAdvancements().getOrStartProgress(_plr0.server.getAdvancements().getAdvancement(new ResourceLocation("sololeveling:shadow_extraction"))).isDone())) {
				if (entity instanceof ServerPlayer _player) {
					Advancement _adv = _player.server.getAdvancements().getAdvancement(new ResourceLocation("sololeveling:shadow_extraction"));
					AdvancementProgress _ap = _player.getAdvancements().getOrStartProgress(_adv);
					if (!_ap.isDone()) {
						for (String criteria : _ap.getRemainingCriteria())
							_player.getAdvancements().award(_adv, criteria);
					}
				}
			}
			if (!(entity instanceof ServerPlayer _plr2 && _plr2.level() instanceof ServerLevel
					&& _plr2.getAdvancements().getOrStartProgress(_plr2.server.getAdvancements().getAdvancement(new ResourceLocation("sololeveling:shadow_storage"))).isDone())) {
				if (entity instanceof ServerPlayer _player) {
					Advancement _adv = _player.server.getAdvancements().getAdvancement(new ResourceLocation("sololeveling:shadow_storage"));
					AdvancementProgress _ap = _player.getAdvancements().getOrStartProgress(_adv);
					if (!_ap.isDone()) {
						for (String criteria : _ap.getRemainingCriteria())
							_player.getAdvancements().award(_adv, criteria);
					}
				}
			}
			if ((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).Level >= 70) {
				if (!(entity instanceof ServerPlayer _plr4 && _plr4.level() instanceof ServerLevel
						&& _plr4.getAdvancements().getOrStartProgress(_plr4.server.getAdvancements().getAdvancement(new ResourceLocation("sololeveling:shadow_storage_level_2"))).isDone())) {
					if (entity instanceof ServerPlayer _player) {
						Advancement _adv = _player.server.getAdvancements().getAdvancement(new ResourceLocation("sololeveling:shadow_storage_level_2"));
						AdvancementProgress _ap = _player.getAdvancements().getOrStartProgress(_adv);
						if (!_ap.isDone()) {
							for (String criteria : _ap.getRemainingCriteria())
								_player.getAdvancements().award(_adv, criteria);
						}
					}
				}
			}
			if ((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).Level >= 90) {
				if (!(entity instanceof ServerPlayer _plr6 && _plr6.level() instanceof ServerLevel
						&& _plr6.getAdvancements().getOrStartProgress(_plr6.server.getAdvancements().getAdvancement(new ResourceLocation("sololeveling:shadow_storage_level_3"))).isDone())) {
					if (entity instanceof ServerPlayer _player) {
						Advancement _adv = _player.server.getAdvancements().getAdvancement(new ResourceLocation("sololeveling:shadow_storage_level_3"));
						AdvancementProgress _ap = _player.getAdvancements().getOrStartProgress(_adv);
						if (!_ap.isDone()) {
							for (String criteria : _ap.getRemainingCriteria())
								_player.getAdvancements().award(_adv, criteria);
						}
					}
				}
			}
			if ((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).Level >= 100) {
				if (!(entity instanceof ServerPlayer _plr8 && _plr8.level() instanceof ServerLevel
						&& _plr8.getAdvancements().getOrStartProgress(_plr8.server.getAdvancements().getAdvancement(new ResourceLocation("sololeveling:shadow_storage_level_4"))).isDone())) {
					if (entity instanceof ServerPlayer _player) {
						Advancement _adv = _player.server.getAdvancements().getAdvancement(new ResourceLocation("sololeveling:shadow_storage_level_4"));
						AdvancementProgress _ap = _player.getAdvancements().getOrStartProgress(_adv);
						if (!_ap.isDone()) {
							for (String criteria : _ap.getRemainingCriteria())
								_player.getAdvancements().award(_adv, criteria);
						}
					}
				}
			}
			if ((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).Level >= 120) {
				if (!(entity instanceof ServerPlayer _plr10 && _plr10.level() instanceof ServerLevel
						&& _plr10.getAdvancements().getOrStartProgress(_plr10.server.getAdvancements().getAdvancement(new ResourceLocation("sololeveling:shadow_storage_level_5"))).isDone())) {
					if (entity instanceof ServerPlayer _player) {
						Advancement _adv = _player.server.getAdvancements().getAdvancement(new ResourceLocation("sololeveling:shadow_storage_level_5"));
						AdvancementProgress _ap = _player.getAdvancements().getOrStartProgress(_adv);
						if (!_ap.isDone()) {
							for (String criteria : _ap.getRemainingCriteria())
								_player.getAdvancements().award(_adv, criteria);
						}
					}
				}
			}
			if ((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).shadowstorageusage >= 30) {
				if (!(entity instanceof ServerPlayer _plr14 && _plr14.level() instanceof ServerLevel
						&& _plr14.getAdvancements().getOrStartProgress(_plr14.server.getAdvancements().getAdvancement(new ResourceLocation("sololeveling:shadow_exchange"))).isDone())) {
					if (entity instanceof ServerPlayer _player) {
						Advancement _adv = _player.server.getAdvancements().getAdvancement(new ResourceLocation("sololeveling:shadow_exchange"));
						AdvancementProgress _ap = _player.getAdvancements().getOrStartProgress(_adv);
						if (!_ap.isDone()) {
							for (String criteria : _ap.getRemainingCriteria())
								_player.getAdvancements().award(_adv, criteria);
						}
					}
					{
						boolean _setval = true;
						entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
							capability.ShadowExchange = _setval;
							capability.syncPlayerVariables(entity);
						});
					}
				}
			}
			if ((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).shadowstorageusage >= 60) {
				if (!(entity instanceof ServerPlayer _plr16 && _plr16.level() instanceof ServerLevel
						&& _plr16.getAdvancements().getOrStartProgress(_plr16.server.getAdvancements().getAdvancement(new ResourceLocation("sololeveling:shadow_spiritual_body_manifestation"))).isDone())) {
					if (entity instanceof ServerPlayer _player) {
						Advancement _adv = _player.server.getAdvancements().getAdvancement(new ResourceLocation("sololeveling:shadow_spiritual_body_manifestation"));
						AdvancementProgress _ap = _player.getAdvancements().getOrStartProgress(_adv);
						if (!_ap.isDone()) {
							for (String criteria : _ap.getRemainingCriteria())
								_player.getAdvancements().award(_adv, criteria);
						}
					}
					{
						boolean _setval = true;
						entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
							capability.ShadowBody = _setval;
							capability.syncPlayerVariables(entity);
						});
					}
				}
			}
		}
	}
}
