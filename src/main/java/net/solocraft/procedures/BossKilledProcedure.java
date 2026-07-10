package net.solocraft.procedures;

import net.solocraft.network.SololevelingModVariables;
import net.solocraft.entity.FangedKasakaEntity;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.event.entity.living.LivingDeathEvent;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.Entity;
import net.minecraft.tags.TagKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;

import javax.annotation.Nullable;

import java.util.ArrayList;

@Mod.EventBusSubscriber
public class BossKilledProcedure {
	@SubscribeEvent
	public static void onEntityDeath(LivingDeathEvent event) {
		if (event != null && event.getEntity() != null) {
			execute(event, event.getEntity().level(), event.getEntity(), event.getSource().getEntity());
		}
	}

	public static void execute(LevelAccessor world, Entity entity, Entity sourceentity) {
		execute(null, world, entity, sourceentity);
	}

	private static void execute(@Nullable Event event, LevelAccessor world, Entity entity, Entity sourceentity) {
		if (entity == null || sourceentity == null)
			return;
		sourceentity = ShadowKillCreditHelper.creditedSource(world, sourceentity);
		final Entity creditedSourceentity = sourceentity;
		if ((sourceentity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).dungeoning == true || !((entity.level().dimension()) == Level.OVERWORLD)) {
			if (entity.getType().is(TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation("soloboss")))) {
				// Guild XP is handled by GuildBossKillProcedure (separate event subscriber)
				if (sourceentity instanceof Player) {
					if (((sourceentity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).party).equals("")) {
						{
							boolean _setval = true;
							sourceentity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
								capability.BossKilled = _setval;
								capability.syncPlayerVariables(creditedSourceentity);
							});
						}
						markGateCleared(world, resolveDungeonTag(world, entity, sourceentity));
					} else {
						for (Entity entityiterator : new ArrayList<>(world.players())) {
							if (((sourceentity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).party)
									.equals((entityiterator.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).party)) {
								if ((entity.level().dimension()) == (entityiterator.level().dimension())) {
									{
										boolean _setval = true;
										entityiterator.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
											capability.BossKilled = _setval;
											capability.syncPlayerVariables(entityiterator);
										});
									}
									markGateCleared(world, resolveDungeonTag(world, entity, entityiterator));
								}
							}
						}
					}
				} else if (sourceentity instanceof TamableAnimal _tamEnt ? _tamEnt.isTame() : false) {
					if (!((sourceentity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null) == null)) {
						if ((((sourceentity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null).getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
								.orElse(new SololevelingModVariables.PlayerVariables())).party).equals("")) {
							{
								boolean _setval = true;
								(sourceentity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null).getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
									capability.BossKilled = _setval;
									capability.syncPlayerVariables((creditedSourceentity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null));
								});
							}
							markGateCleared(world, resolveDungeonTag(world, entity, (sourceentity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null)));
						} else {
							for (Entity entityiterator : new ArrayList<>(world.players())) {
								if ((((sourceentity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null).getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
										.orElse(new SololevelingModVariables.PlayerVariables())).party)
										.equals((entityiterator.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).party)) {
									if ((entity.level().dimension()) == (entityiterator.level().dimension())) {
										{
											boolean _setval = true;
											entityiterator.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
												capability.BossKilled = _setval;
												capability.syncPlayerVariables(entityiterator);
											});
										}
										markGateCleared(world, resolveDungeonTag(world, entity, entityiterator));
									}
								}
							}
						}
					}
				} else {
					markGateCleared(world, resolveDungeonTag(world, entity, sourceentity));
				}
			} else if (entity instanceof FangedKasakaEntity) {
				if (((sourceentity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).party).equals("")) {
					{
						boolean _setval = true;
						sourceentity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
							capability.instancecomplete = _setval;
							capability.syncPlayerVariables(creditedSourceentity);
						});
					}
				} else {
					for (Entity entityiterator : new ArrayList<>(world.players())) {
						if (((sourceentity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).party)
								.equals((entityiterator.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).party)) {
							if ((entity.level().dimension()) == (entityiterator.level().dimension())) {
								{
									boolean _setval = true;
									entityiterator.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
										capability.instancecomplete = _setval;
										capability.syncPlayerVariables(entityiterator);
									});
								}
							}
						}
					}
				}
			}
		}
	}

	private static void markGateCleared(LevelAccessor world, String dungeonTag) {
		if (dungeonTag == null || dungeonTag.isEmpty())
			return;
		String token = dungeonTag + ",";
		if (!SololevelingModVariables.MapVariables.get(world).GatesCleared.contains(token)) {
			SololevelingModVariables.MapVariables.get(world).GatesCleared = SololevelingModVariables.MapVariables.get(world).GatesCleared + token;
			SololevelingModVariables.MapVariables.get(world).syncData(world);
		}
	}

	private static String resolveDungeonTag(LevelAccessor world, Entity boss, Entity sourceentity) {
		String tag = dungeonTag(sourceentity);
		if (!tag.isEmpty())
			return tag;
		if (sourceentity instanceof TamableAnimal tame && tame.getOwner() != null) {
			tag = dungeonTag(tame.getOwner());
			if (!tag.isEmpty())
				return tag;
		}
		tag = dungeonTag(boss);
		if (!tag.isEmpty())
			return tag;
		for (Entity player : new ArrayList<>(world.players())) {
			if ((boss.level().dimension()) == (player.level().dimension())) {
				tag = dungeonTag(player);
				if (!tag.isEmpty())
					return tag;
			}
		}
		return "";
	}

	private static String dungeonTag(Entity entity) {
		if (entity == null)
			return "";
		String tag = entity.getPersistentData().getString("dungeon_tag");
		return tag == null ? "" : tag;
	}
}
