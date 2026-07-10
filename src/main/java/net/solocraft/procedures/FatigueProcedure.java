package net.solocraft.procedures;

import net.solocraft.network.SololevelingModVariables;
import net.solocraft.init.SololevelingModGameRules;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.event.entity.living.LivingAttackEvent;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.GameType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.client.Minecraft;

import javax.annotation.Nullable;
import net.solocraft.util.CooldownManager;

@Mod.EventBusSubscriber
public class FatigueProcedure {
	@SubscribeEvent
	public static void onEntityAttacked(LivingAttackEvent event) {
		Entity entity = event.getEntity();
		if (event != null && entity != null) {
			execute(event, entity.level(), event.getSource(), entity);
		}
	}

	public static void execute(LevelAccessor world, DamageSource damagesource, Entity entity) {
		execute(null, world, damagesource, entity);
	}

	private static void execute(@Nullable Event event, LevelAccessor world, DamageSource damagesource, Entity entity) {
		if (damagesource == null || entity == null)
			return;
		if (world.getLevelData().getGameRules().getBoolean(SololevelingModGameRules.SOLO_FATIGUE)) {
			if (!CooldownManager.isOnCooldown(entity, "fatigue")) {
				if (new Object() {
					public boolean checkGamemode(Entity _ent) {
						if (_ent instanceof ServerPlayer _serverPlayer) {
							return _serverPlayer.gameMode.getGameModeForPlayer() == GameType.SURVIVAL;
						} else if (_ent.level().isClientSide() && _ent instanceof Player _player) {
							return Minecraft.getInstance().getConnection().getPlayerInfo(_player.getGameProfile().getId()) != null
									&& Minecraft.getInstance().getConnection().getPlayerInfo(_player.getGameProfile().getId()).getGameMode() == GameType.SURVIVAL;
						}
						return false;
					}
				}.checkGamemode(entity)) {
					if ((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).Player == true && !((damagesource).is(DamageTypes.IN_FIRE)
							|| (damagesource).is(ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation("sololeveling:bleeding"))) || (damagesource).is(DamageTypes.LAVA) || (damagesource).is(DamageTypes.IN_WALL)
							|| (damagesource).is(DamageTypes.CACTUS) || (damagesource).is(DamageTypes.ON_FIRE) || (damagesource).is(DamageTypes.GENERIC) || (damagesource).is(DamageTypes.GENERIC_KILL))) {
						{
							double _setval = (entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).Fatigue + 2;
							entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
								capability.Fatigue = _setval;
								capability.syncPlayerVariables(entity);
							});
						}
						CooldownManager.set(entity, "fatigue", 20);
					}
				}
			}
		}
	}
}
