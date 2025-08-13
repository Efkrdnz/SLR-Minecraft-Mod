
/*
 *    MCreator note: This file will be REGENERATED on each build.
 */
package net.solocraft.init;

import net.solocraft.client.particle.WhiteFlamesParticle;
import net.solocraft.client.particle.SlashfuryParticle;
import net.solocraft.client.particle.Slashfury5Particle;
import net.solocraft.client.particle.Slashfury4Particle;
import net.solocraft.client.particle.Slashfury3Particle;
import net.solocraft.client.particle.Slashfury2Particle;
import net.solocraft.client.particle.ShardParticleParticle;
import net.solocraft.client.particle.ShamanMagicParticleParticle;
import net.solocraft.client.particle.ShadowReviveParticle;
import net.solocraft.client.particle.RedDustParticleParticle;
import net.solocraft.client.particle.PinkslashParticle;
import net.solocraft.client.particle.Pinkslash2Particle;
import net.solocraft.client.particle.PhysicalBuffParticleParticle;
import net.solocraft.client.particle.ManaRedParticle;
import net.solocraft.client.particle.ManaPurpleParticle;
import net.solocraft.client.particle.ManaGreenParticle;
import net.solocraft.client.particle.ManaBlueParticle;
import net.solocraft.client.particle.MagicMissilesParticle;
import net.solocraft.client.particle.LightningWhiteParticle;
import net.solocraft.client.particle.LightningBlueParticle;
import net.solocraft.client.particle.Impact22Particle;
import net.solocraft.client.particle.Impact21Particle;
import net.solocraft.client.particle.Impact1Particle;
import net.solocraft.client.particle.Impact13Particle;
import net.solocraft.client.particle.Impact12Particle;
import net.solocraft.client.particle.HealingParticleParticle;
import net.solocraft.client.particle.HasteBuffParticleParticle;
import net.solocraft.client.particle.GreySlash3Particle;
import net.solocraft.client.particle.GreySlash2Particle;
import net.solocraft.client.particle.GreySlash1Particle;
import net.solocraft.client.particle.Goodslash3Particle;
import net.solocraft.client.particle.Goodslash1Particle;
import net.solocraft.client.particle.GoodSlash2Particle;
import net.solocraft.client.particle.GlowYellowParticle;
import net.solocraft.client.particle.GlowAuraYellowParticle;
import net.solocraft.client.particle.GlowAuraRedParticle;
import net.solocraft.client.particle.GlowAuraPurpleParticle;
import net.solocraft.client.particle.FireParticleParticle;
import net.solocraft.client.particle.FireParticle2Particle;
import net.solocraft.client.particle.DismantleParticle;
import net.solocraft.client.particle.DetectEyeParticle;
import net.solocraft.client.particle.CurseSmokeParticle;
import net.solocraft.client.particle.CleaveParticle;
import net.solocraft.client.particle.BloodParticleParticle;
import net.solocraft.client.particle.BloodParticleLandParticle;
import net.solocraft.client.particle.AuraPurpleParticle;
import net.solocraft.client.particle.AuraGreenParticle;
import net.solocraft.client.particle.AuraBlueParticle;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.api.distmarker.Dist;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class SololevelingModParticles {
	@SubscribeEvent
	public static void registerParticles(RegisterParticleProvidersEvent event) {
		event.registerSpriteSet(SololevelingModParticleTypes.AURA_BLUE.get(), AuraBlueParticle::provider);
		event.registerSpriteSet(SololevelingModParticleTypes.AURA_PURPLE.get(), AuraPurpleParticle::provider);
		event.registerSpriteSet(SololevelingModParticleTypes.AURA_GREEN.get(), AuraGreenParticle::provider);
		event.registerSpriteSet(SololevelingModParticleTypes.MANA_BLUE.get(), ManaBlueParticle::provider);
		event.registerSpriteSet(SololevelingModParticleTypes.MANA_PURPLE.get(), ManaPurpleParticle::provider);
		event.registerSpriteSet(SololevelingModParticleTypes.MANA_GREEN.get(), ManaGreenParticle::provider);
		event.registerSpriteSet(SololevelingModParticleTypes.MANA_RED.get(), ManaRedParticle::provider);
		event.registerSpriteSet(SololevelingModParticleTypes.PINKSLASH.get(), PinkslashParticle::provider);
		event.registerSpriteSet(SololevelingModParticleTypes.PINKSLASH_2.get(), Pinkslash2Particle::provider);
		event.registerSpriteSet(SololevelingModParticleTypes.SLASHFURY.get(), SlashfuryParticle::provider);
		event.registerSpriteSet(SololevelingModParticleTypes.SLASHFURY_2.get(), Slashfury2Particle::provider);
		event.registerSpriteSet(SololevelingModParticleTypes.SLASHFURY_3.get(), Slashfury3Particle::provider);
		event.registerSpriteSet(SololevelingModParticleTypes.SLASHFURY_4.get(), Slashfury4Particle::provider);
		event.registerSpriteSet(SololevelingModParticleTypes.SLASHFURY_5.get(), Slashfury5Particle::provider);
		event.registerSpriteSet(SololevelingModParticleTypes.SHARD_PARTICLE.get(), ShardParticleParticle::provider);
		event.registerSpriteSet(SololevelingModParticleTypes.GREY_SLASH_1.get(), GreySlash1Particle::provider);
		event.registerSpriteSet(SololevelingModParticleTypes.GREY_SLASH_2.get(), GreySlash2Particle::provider);
		event.registerSpriteSet(SololevelingModParticleTypes.GREY_SLASH_3.get(), GreySlash3Particle::provider);
		event.registerSpriteSet(SololevelingModParticleTypes.GOODSLASH_1.get(), Goodslash1Particle::provider);
		event.registerSpriteSet(SololevelingModParticleTypes.GOOD_SLASH_2.get(), GoodSlash2Particle::provider);
		event.registerSpriteSet(SololevelingModParticleTypes.GLOW_YELLOW.get(), GlowYellowParticle::provider);
		event.registerSpriteSet(SololevelingModParticleTypes.DETECT_EYE.get(), DetectEyeParticle::provider);
		event.registerSpriteSet(SololevelingModParticleTypes.DISMANTLE.get(), DismantleParticle::provider);
		event.registerSpriteSet(SololevelingModParticleTypes.CLEAVE.get(), CleaveParticle::provider);
		event.registerSpriteSet(SololevelingModParticleTypes.IMPACT_1.get(), Impact1Particle::provider);
		event.registerSpriteSet(SololevelingModParticleTypes.IMPACT_12.get(), Impact12Particle::provider);
		event.registerSpriteSet(SololevelingModParticleTypes.IMPACT_13.get(), Impact13Particle::provider);
		event.registerSpriteSet(SololevelingModParticleTypes.GLOW_AURA_YELLOW.get(), GlowAuraYellowParticle::provider);
		event.registerSpriteSet(SololevelingModParticleTypes.HEALING_PARTICLE.get(), HealingParticleParticle::provider);
		event.registerSpriteSet(SololevelingModParticleTypes.PHYSICAL_BUFF_PARTICLE.get(), PhysicalBuffParticleParticle::provider);
		event.registerSpriteSet(SololevelingModParticleTypes.HASTE_BUFF_PARTICLE.get(), HasteBuffParticleParticle::provider);
		event.registerSpriteSet(SololevelingModParticleTypes.FIRE_PARTICLE.get(), FireParticleParticle::provider);
		event.registerSpriteSet(SololevelingModParticleTypes.GLOW_AURA_RED.get(), GlowAuraRedParticle::provider);
		event.registerSpriteSet(SololevelingModParticleTypes.IMPACT_21.get(), Impact21Particle::provider);
		event.registerSpriteSet(SololevelingModParticleTypes.IMPACT_22.get(), Impact22Particle::provider);
		event.registerSpriteSet(SololevelingModParticleTypes.SHADOW_REVIVE.get(), ShadowReviveParticle::provider);
		event.registerSpriteSet(SololevelingModParticleTypes.SHAMAN_MAGIC_PARTICLE.get(), ShamanMagicParticleParticle::provider);
		event.registerSpriteSet(SololevelingModParticleTypes.WHITE_FLAMES.get(), WhiteFlamesParticle::provider);
		event.registerSpriteSet(SololevelingModParticleTypes.FIRE_PARTICLE_2.get(), FireParticle2Particle::provider);
		event.registerSpriteSet(SololevelingModParticleTypes.CURSE_SMOKE.get(), CurseSmokeParticle::provider);
		event.registerSpriteSet(SololevelingModParticleTypes.RED_DUST_PARTICLE.get(), RedDustParticleParticle::provider);
		event.registerSpriteSet(SololevelingModParticleTypes.GOODSLASH_3.get(), Goodslash3Particle::provider);
		event.registerSpriteSet(SololevelingModParticleTypes.BLOOD_PARTICLE_LAND.get(), BloodParticleLandParticle::provider);
		event.registerSpriteSet(SololevelingModParticleTypes.BLOOD_PARTICLE.get(), BloodParticleParticle::provider);
		event.registerSpriteSet(SololevelingModParticleTypes.GLOW_AURA_PURPLE.get(), GlowAuraPurpleParticle::provider);
		event.registerSpriteSet(SololevelingModParticleTypes.MAGIC_MISSILES.get(), MagicMissilesParticle::provider);
		event.registerSpriteSet(SololevelingModParticleTypes.LIGHTNING_WHITE.get(), LightningWhiteParticle::provider);
		event.registerSpriteSet(SololevelingModParticleTypes.LIGHTNING_BLUE.get(), LightningBlueParticle::provider);
	}
}
