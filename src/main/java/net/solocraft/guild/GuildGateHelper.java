package net.solocraft.guild;

import net.solocraft.entity.Portal1Entity;
import net.solocraft.entity.PortalAncientGolemEntity;
import net.solocraft.entity.PortalBeruEntity;
import net.solocraft.entity.PortalCemeteryEntity;
import net.solocraft.entity.PortalKargalgansThroneRoomEntity;
import net.solocraft.entity.PortalLabEntity;
import net.solocraft.entity.PortalLushEntity;
import net.solocraft.entity.PortalSEntity;
import net.solocraft.entity.PortalSewersEntity;
import net.solocraft.entity.RandomCaveLargeEntity;
import net.solocraft.entity.RedGateEntity;
import net.solocraft.network.SololevelingModVariables;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LevelAccessor;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.UUID;

public final class GuildGateHelper {
    public static final TagKey<EntityType<?>> PORTAL_TAG =
            TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation("portals"));
    private static final String INTERACTED_KEY = "slr_guild_gate_interacted";
    private static final String RESERVED_KEY = "slr_guild_gate_reserved";
    private static final String RESERVED_GUILD_NAME_KEY = "slr_guild_gate_reserved_name";
    private static final String RESERVED_GUILD_ID_KEY = "slr_guild_gate_reserved_id";

    private GuildGateHelper() {
    }

    public static boolean isDeployableGate(Entity entity) {
        if (entity == null || entity.isRemoved() || !entity.getType().is(PORTAL_TAG)) return false;
        String path = entityPath(entity);
        return !path.equals("portal_12")
                && !path.equals("portal_job_change")
                && !path.equals("spawner_portal");
    }

    public static Entity findGate(ServerLevel level, UUID uuid) {
        Entity entity = level.getEntity(uuid);
        return isDeployableGate(entity) ? entity : null;
    }

    public static boolean isGateInteracted(Entity gate) {
        if (gate == null || gate.isRemoved()) return true;
        if (gate.getPersistentData().getBoolean(INTERACTED_KEY)) return true;
        if (gate instanceof Portal1Entity portal) return portal.getEntityData().get(Portal1Entity.DATA_usedbefore);
        if (gate instanceof PortalSewersEntity portal) return portal.getEntityData().get(PortalSewersEntity.DATA_usedbefore);
        if (gate instanceof PortalCemeteryEntity portal) return portal.getEntityData().get(PortalCemeteryEntity.DATA_usedbefore);
        if (gate instanceof PortalAncientGolemEntity portal) return portal.getEntityData().get(PortalAncientGolemEntity.DATA_usedbefore);
        if (gate instanceof PortalLabEntity portal) return portal.getEntityData().get(PortalLabEntity.DATA_usedbefore);
        if (gate instanceof PortalLushEntity portal) return portal.getEntityData().get(PortalLushEntity.DATA_usedbefore);
        if (gate instanceof PortalKargalgansThroneRoomEntity portal) return portal.getEntityData().get(PortalKargalgansThroneRoomEntity.DATA_usedbefore);
        if (gate instanceof PortalBeruEntity portal) return portal.getEntityData().get(PortalBeruEntity.DATA_usedbefore);
        if (gate instanceof PortalSEntity portal) return portal.getEntityData().get(PortalSEntity.DATA_usedbefore);
        if (gate instanceof RandomCaveLargeEntity portal) return portal.getEntityData().get(RandomCaveLargeEntity.DATA_usedbefore);
        if (gate instanceof RedGateEntity portal) return portal.getEntityData().get(RedGateEntity.DATA_usedbefore);
        return false;
    }

    public static void markGateInteracted(Entity gate) {
        if (gate != null && !gate.isRemoved()) {
            gate.getPersistentData().putBoolean(INTERACTED_KEY, true);
        }
    }

    public static boolean tryBlockReservedGateEntry(LevelAccessor world, Entity gate, Entity sourceentity) {
        if (gate == null || sourceentity == null || !isGateReserved(gate)) return false;
        if (!world.isClientSide() && sourceentity instanceof Player player) {
            player.displayClientMessage(Component.literal("§cThis gate has already been bought by §e"
                    + reservedGuildName(gate) + " §cguild and is currently being raided."), false);
        }
        return true;
    }

    public static boolean prepareGateEntry(LevelAccessor world, Entity gate, Entity sourceentity) {
        if (tryBlockReservedGateEntry(world, gate, sourceentity)) return true;
        markGateInteracted(gate);
        return false;
    }

    public static boolean isGateReserved(Entity gate) {
        return gate != null && !gate.isRemoved() && gate.getPersistentData().getBoolean(RESERVED_KEY);
    }

    public static String reservedGuildName(Entity gate) {
        if (gate == null) return "another";
        String name = gate.getPersistentData().getString(RESERVED_GUILD_NAME_KEY);
        return name.isEmpty() ? "another" : name;
    }

    public static void reserveGate(Entity gate, GuildData guild) {
        if (gate == null || guild == null || gate.isRemoved()) return;
        gate.getPersistentData().putBoolean(RESERVED_KEY, true);
        gate.getPersistentData().putString(RESERVED_GUILD_NAME_KEY, guild.name);
        gate.getPersistentData().putString(RESERVED_GUILD_ID_KEY, guild.id.toString());
    }

    public static void clearReservation(Entity gate) {
        if (gate == null || gate.isRemoved()) return;
        gate.getPersistentData().remove(RESERVED_KEY);
        gate.getPersistentData().remove(RESERVED_GUILD_NAME_KEY);
        gate.getPersistentData().remove(RESERVED_GUILD_ID_KEY);
    }

    @Nullable
    public static GuildData findGuildRaidingGate(GuildSavedData data, String gateUuid) {
        if (data == null || gateUuid == null || gateUuid.isEmpty()) return null;
        for (GuildData guild : data.allGuilds()) {
            for (GuildDeployment deployment : guild.deployments) {
                if (gateUuid.equals(deployment.gateEntityUUID)) return guild;
            }
        }
        return null;
    }

    public static int gateRank(Entity entity) {
        String proceduralRank = entity.getPersistentData().getString("slr_procedural_rank");
        if (!proceduralRank.isEmpty()) return rankNumber(proceduralRank);

        String path = entityPath(entity);
        return switch (path) {
            case "portal", "portal_sewers" -> 2;
            case "portal_1", "random_cave_large", "portal_ancient_golem" -> 3;
            case "portal_lush", "portal_cemetery", "red_gate" -> 4;
            case "portal_lab", "portal_kargalgans_throne_room" -> 5;
            case "portal_beru" -> 6;
            default -> 1;
        };
    }

    public static String gateLabel(Entity entity) {
        int rank = gateRank(entity);
        return GuildDeployment.rankLabel(rank) + "-Rank Gate @("
                + entity.blockPosition().getX() + ", " + entity.blockPosition().getZ() + ")";
    }

    public static void markGateCleared(ServerLevel level, String gateUuid) {
        if (gateUuid == null || gateUuid.isEmpty()) return;

        Entity gate = null;
        try {
            gate = level.getEntity(UUID.fromString(gateUuid));
        } catch (IllegalArgumentException ignored) {
        }

        SololevelingModVariables.MapVariables vars = SololevelingModVariables.MapVariables.get(level);
        String token = gateUuid + ",";
        if (!vars.GatesCleared.contains(gateUuid)) {
            vars.GatesCleared = vars.GatesCleared + token;
        }
        if (gate instanceof RedGateEntity || (gate != null && "red_gate".equals(entityPath(gate)))) {
            vars.RedGate = false;
        }
        vars.syncData(level);

        if (gate != null && !gate.isRemoved()) {
            gate.discard();
        }
    }

    private static int rankNumber(String rank) {
        return switch (rank.trim().toUpperCase()) {
            case "D" -> 2;
            case "C" -> 3;
            case "B" -> 4;
            case "A" -> 5;
            case "S" -> 6;
            default -> 1;
        };
    }

    private static String entityPath(Entity entity) {
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        return id == null ? "" : id.getPath();
    }
}
