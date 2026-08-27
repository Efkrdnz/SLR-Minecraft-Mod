package net.solocraft.procedures;

import net.solocraft.network.SololevelingModVariables;
import net.solocraft.dungeon.runtime.DungeonLevelHelper;
import net.solocraft.dungeon.runtime.DungeonMobLevelAdapter;

import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.ICancellableEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.tags.TagKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;

import javax.annotation.Nullable;

@EventBusSubscriber
public class BeforePlayerHurtProcedure {
	@SubscribeEvent
	public static void onEntityAttacked(LivingIncomingDamageEvent event) {
		Entity entity = event.getEntity();
		if (event != null && entity != null) {
			execute(event, entity.level(), event.getSource(), entity, event.getSource().getEntity(), event.getAmount());
		}
	}

	public static void execute(LevelAccessor world, DamageSource damagesource, Entity entity, Entity sourceentity, double amount) {
		execute(null, world, damagesource, entity, sourceentity, amount);
	}

	private static void execute(@Nullable ICancellableEvent event, LevelAccessor world, DamageSource damagesource, Entity entity, Entity sourceentity, double amount) {
		if (damagesource == null || entity == null || sourceentity == null)
			return;
		double dmg = 0;
		dmg = amount;
		if (!(sourceentity == null)) {
			if (!(damagesource).is(ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.parse("sololeveling:magic_beast")))) {
				if (entity instanceof Player) {
					if ((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).Player) {
						boolean runtimeDungeonMob = sourceentity.getPersistentData().getBoolean(DungeonMobLevelAdapter.RUNTIME_SPAWN_TAG);
						if (!sourceentity.getType().is(TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.parse("soloboss"))) || runtimeDungeonMob) {
							if (sourceentity.getType().is(TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.parse("dm"))) || runtimeDungeonMob) {// Ensure Level is treated as an integer
								int takerLevel = (int) entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables()).Level;
								int dealerLevel = (int) DungeonLevelHelper.levelOf(sourceentity);
								// Calculate level difference
								int levelDifference = takerLevel - dealerLevel;
								// Calculate damage reduction (clamped between 0 and 1)
								float reduction = (float) Math.max(0, Math.min(1, 0.01 * levelDifference));
								// Apply reduction to damage
								float finalDamage = (float) (dmg * (1 - reduction));
								if (event != null) {
									event.setCanceled(true);
								}
								if (finalDamage < 1) {
									entity.hurt(
											new DamageSource(world.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.parse("sololeveling:magic_beast"))), sourceentity),
											1);
								} else {
									entity.hurt(
											new DamageSource(world.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.parse("sololeveling:magic_beast"))), sourceentity),
											(float) finalDamage);
								}
							}
						}
					}
				}
			}
		}
	}
}
