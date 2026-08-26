package net.solocraft.procedures;

import net.solocraft.network.SololevelingModVariables;
import net.solocraft.init.SololevelingModParticleTypes;
import net.solocraft.init.SololevelingModMobEffects;

import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.Event;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.BlockPos;

import javax.annotation.Nullable;

@EventBusSubscriber
public class SwordOfLightOnHitProcedure {
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

    private static void execute(@Nullable Event event, LevelAccessor world, double x, double y, double z, Entity entity, Entity sourceentity) {
        if (entity == null || sourceentity == null)
            return;
        
        if (sourceentity instanceof LivingEntity _livEnt0 && _livEnt0.hasEffect(SololevelingModMobEffects.SWORD_OF_LIGHT)) {
            if ((sourceentity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).MP >= 100) {
                
                // Set a consistent launch force for all entities
                double launchPower = 1.2; // You can adjust this for a higher/lower launch
                double launchPower2 = 1.0;

                // Play sound effect
                if (world instanceof Level _level) {
                    if (!_level.isClientSide()) {
                        _level.playSound(null, BlockPos.containing(x, y, z), 
                            BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("sololeveling:dash")), 
                            SoundSource.NEUTRAL, 0.5f, 1.0f);
                    } else {
                        _level.playLocalSound(x, y, z, 
                            BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("sololeveling:dash")), 
                            SoundSource.NEUTRAL, 0.5f, 1.0f, false);
                    }
                }

                // Apply launch to attacker
                sourceentity.setDeltaMovement(new Vec3(0, launchPower2, 0));
                syncVelocity(sourceentity); // Sync for multiplayer

                // Apply launch to target
                entity.setDeltaMovement(new Vec3(0, launchPower, 0));
                syncVelocity(entity); // Sync for multiplayer

                // Effects for attacker
                if (sourceentity instanceof LivingEntity _entity) {
                    if (!_entity.level().isClientSide()) {
                        _entity.addEffect(new MobEffectInstance(SololevelingModMobEffects.NO_FALL_DAMAGE, 999, 1, false, false));
                        _entity.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 10, 5, false, false));
                        _entity.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 10, 5, false, false));
                    }
                }

                // Effects for target
                if (entity instanceof LivingEntity _entity) {
                    if (!_entity.level().isClientSide()) {
                        _entity.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 10, 5, false, false));
                    }
                }

                // Particles
                if (world instanceof ServerLevel _level) {
                    _level.sendParticles((SimpleParticleType) (SololevelingModParticleTypes.GLOW_YELLOW.get()), 
                        sourceentity.getX(), sourceentity.getY() + 0.8, sourceentity.getZ(), 
                        5, 3, 3, 3, 1);
                    _level.sendParticles((SimpleParticleType) (SololevelingModParticleTypes.GLOW_AURA_YELLOW.get()), 
                        sourceentity.getX(), sourceentity.getY() + 0.8, sourceentity.getZ(), 
                        9, 0.25, 0.6, 0.25, 0);
                }
            }
        }
    }

    /**
     * Syncs entity velocity for multiplayer support.
     * This ensures other players see the launch effect.
     */
    private static void syncVelocity(Entity entity) {
        if (entity instanceof ServerPlayer serverPlayer) {
            ServerGamePacketListenerImpl connection = serverPlayer.connection;
            if (connection != null) {
                connection.send(new ClientboundSetEntityMotionPacket(serverPlayer));
            }
        }
    }
}