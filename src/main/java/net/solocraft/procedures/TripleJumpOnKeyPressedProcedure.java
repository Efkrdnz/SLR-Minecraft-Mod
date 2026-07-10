package net.solocraft.procedures;

import net.solocraft.network.SololevelingModVariables;
import net.solocraft.init.SololevelingModParticleTypes;

import net.minecraftforge.registries.ForgeRegistries;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.GameType;
import net.minecraft.world.entity.Entity;
import net.minecraft.sounds.SoundSource;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.SimpleParticleType;

public class TripleJumpOnKeyPressedProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		execute(world, x, y, z, entity, 0, 0);
	}

	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity, double clientMotionX, double clientMotionZ) {
		if (entity == null)
			return;
		if ((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).tjonoff == true) {
			if ((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).Speed >= 31
					&& (entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).Level >= 25) {
				if (new Object() {
					public boolean checkGamemode(Entity _ent) {
						if (_ent instanceof ServerPlayer _serverPlayer) {
							return _serverPlayer.gameMode.getGameModeForPlayer() == GameType.SURVIVAL;
						}
						return false;
					}
				}.checkGamemode(entity)) {
					if ((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).tj < 2 && !entity.onGround()) {
						{
							double _setval = (entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).tj + 1;
							entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
								capability.tj = _setval;
								capability.syncPlayerVariables(entity);
							});
						}
						Vec3 serverMotion = entity.getDeltaMovement();
						Vec3 horizontal = new Vec3(serverMotion.x(), 0, serverMotion.z());
						Vec3 clientHorizontal = new Vec3(clientMotionX, 0, clientMotionZ);
						if (clientHorizontal.lengthSqr() > horizontal.lengthSqr() && clientHorizontal.lengthSqr() < 16) {
							horizontal = clientHorizontal;
						}
						double oldSpeedSqr = horizontal.lengthSqr();
						Vec3 look = new Vec3(entity.getLookAngle().x, 0, entity.getLookAngle().z);
						if (look.lengthSqr() > 0.0001) {
							Vec3 boosted = horizontal.add(look.normalize().scale(0.25));
							if (boosted.lengthSqr() >= oldSpeedSqr) {
								horizontal = boosted;
							}
						}
						Vec3 jumpMotion = new Vec3(horizontal.x(), 0.5, horizontal.z());
						entity.setDeltaMovement(jumpMotion);
						entity.hasImpulse = true;
						if (entity instanceof ServerPlayer _serverPlayer)
							_serverPlayer.connection.send(new ClientboundSetEntityMotionPacket(_serverPlayer));
						entity.fallDistance = 0;
						if (world instanceof Level _level) {
							if (!_level.isClientSide()) {
								_level.playSound(null, BlockPos.containing(x, y, z), ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("block.end_portal_frame.fill")), SoundSource.NEUTRAL, (float) 0.5, 1);
							}
						}
						if (world instanceof ServerLevel _level)
							_level.sendParticles((SimpleParticleType) (SololevelingModParticleTypes.MANA_BLUE.get()), x, y, z, 25, 0.5, 0, 0.5, 1);
					}
				}
			}
		}
	}
}
