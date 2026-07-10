package net.solocraft.procedures;

import net.solocraft.network.SololevelingModVariables;
import net.solocraft.util.JobChangeQuestManager;
import net.solocraft.util.SystemNotifications;

import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.event.TickEvent;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
import net.minecraft.ChatFormatting;

import javax.annotation.Nullable;

@Mod.EventBusSubscriber
public class JobChangeTimerProcedure {
	private static final int JOB_CHANGE_ACCENT = 0xFF7A5CFF;
	private static final int RIDDLE_DURATION = 56;
	private static final int RESULT_DURATION = 90;

	@SubscribeEvent
	public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
		if (event.phase == TickEvent.Phase.END) {
			execute(event, event.player.level(), event.player.getX(), event.player.getY(), event.player.getZ(), event.player);
		}
	}

	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		execute(null, world, x, y, z, entity);
	}

	private static void execute(@Nullable Event event, LevelAccessor world, double x, double y, double z, Entity entity) {
		if (entity == null)
			return;
		if ((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).JobChange_timer > 0
				&& (entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).JobChange_timer < 9) {
			if (world.getLevelData().getGameTime() % 60 == 0) {
				{
					double _setval = (entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).JobChange_timer + 1;
					entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
						capability.JobChange_timer = _setval;
						capability.syncPlayerVariables(entity);
					});
				}
				if (world instanceof Level _level) {
					if (!_level.isClientSide()) {
						_level.playSound(null, BlockPos.containing(x, y, z), ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("sololeveling:panelopen")), SoundSource.NEUTRAL, 1, 1);
					} else {
						_level.playLocalSound(x, y, z, ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("sololeveling:panelopen")), SoundSource.NEUTRAL, 1, 1, false);
					}
				}
				if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide())
					_entity.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 100, 3, false, false));
				if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide())
					_entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 3, false, false));
				if ((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).JobChange_timer == 2) {
					showJobChangeMessage(entity,
							systemTitle("JOB CHANGE"),
							systemUnder("Wherever the player goes, angel of death follows..."),
							RIDDLE_DURATION);
				}
				if ((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).JobChange_timer == 3) {
					showJobChangeMessage(entity,
							systemTitle("JOB CHANGE"),
							systemUnder("Wherever path the player takes it is littered with corpses, and the stench of blood remains."),
							RIDDLE_DURATION);
				}
				if ((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).JobChange_timer == 4) {
					showJobChangeMessage(entity,
							systemTitle("JOB CHANGE"),
							systemUnder("In addition the player craves geat power and blazed his own trail without relying on others."),
							RIDDLE_DURATION);
				}
				if ((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).JobChange_timer == 5) {
					showJobChangeMessage(entity,
							systemTitle("JOB CHANGE"),
							systemUnder("Its thirst for strength calls forth the ghost that roam the valley of death."),
							RIDDLE_DURATION);
				}
				if ((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).JobChange_timer == 6) {
					showJobChangeMessage(entity,
							systemTitle("JOB CHANGE"),
							systemUnder("The ghosts summoned by the shadow army will follow the player's orders as the shadow army and make way only for the player."),
							RIDDLE_DURATION);
				}
				if ((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).JobChange_timer == 7) {
					showJobChangeMessage(entity,
							systemTitle("JOB ASSIGNED"),
							Component.literal("[Necromancer]").withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD),
							RESULT_DURATION);
				}
				if ((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).JobChange_timer == 8) {
					showJobChangeMessage(entity,
							systemTitle("ADVANCEMENT"),
							systemUnder("Depending on the advancement points acquired, you may progress to a higher class."),
							RIDDLE_DURATION);
				}
			}
		} else if ((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).JobChange_timer == 9) {
			if (entity instanceof LivingEntity living && !living.level().isClientSide())
				living.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
			JobChangeCleanupProcedure.execute(world, x, y, z);
			showJobChangeMessage(entity,
					Component.literal("CLASS EVOLUTION").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD),
					Component.literal("[Necromancer] -> [Shadow Monarch]").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD),
					RESULT_DURATION);
			{
				double _setval = 0;
				entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
					capability.JobChange_timer = _setval;
					capability.syncPlayerVariables(entity);
				});
			}
			{
				double _setval = 1;
				entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
					capability.JOB = _setval;
					capability.syncPlayerVariables(entity);
				});
			}
			JobChangeQuestManager.finish(entity);
			if (world instanceof Level _level) {
				if (!_level.isClientSide()) {
					_level.playSound(null, BlockPos.containing(x, y, z), ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("sololeveling:panelclose")), SoundSource.NEUTRAL, 1, 1);
				} else {
					_level.playLocalSound(x, y, z, ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("sololeveling:panelclose")), SoundSource.NEUTRAL, 1, 1, false);
				}
			}
		}
	}

	private static void showJobChangeMessage(Entity entity, Component popupTitle, Component popupUnder, int durationTicks) {
		if (entity instanceof ServerPlayer player)
			SystemNotifications.showTitleUnder(player, JOB_CHANGE_ACCENT, durationTicks, popupTitle, popupUnder);
	}

	private static Component systemTitle(String text) {
		return Component.literal(text).withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD);
	}

	private static Component systemUnder(String text) {
		return Component.literal(text).withStyle(ChatFormatting.GRAY);
	}
}
