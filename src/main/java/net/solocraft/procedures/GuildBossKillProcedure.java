package net.solocraft.procedures;

import net.solocraft.guild.GuildData;
import net.solocraft.guild.GuildSavedData;

import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.fml.common.Mod;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

import java.util.Map;

/**
 * Awards guild XP when any soloboss is killed by a guild member.
 * XP scales with the gate tier the boss was killed in.
 */
@Mod.EventBusSubscriber
public class GuildBossKillProcedure {

    private static final TagKey<EntityType<?>> SOLOBOSS =
            TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation("soloboss"));

    /** XP awarded per gate tier. Keyed by dimension path (the part after "sololeveling:"). */
    private static final Map<String, Integer> DIM_XP = Map.of(
            "dungeon_dimension_d",    300,
            "dungeon_dimension_snow", 400,
            "dungeon_dimension_c",    600,
            "dungeon_dimension_b",   1200,
            "dungeon_dimension_a",   2500,
            "dungeon_dimension_s",   5000
    );

    /** Dimensions that should never award guild XP (system dungeons). */
    private static final java.util.Set<String> EXCLUDED_DIMS = java.util.Set.of(
            "dungeon_dimension_igris",
            "dungeon_dimension_kasaka",
            "dungeon_dimension_dkc",
            "system_void_dimension",
            "survival_dimension"
    );

    /** Fallback for normal gates not in the map (e.g. overworld spawns). */
    private static final int DEFAULT_XP = 200;

    @SubscribeEvent
    public static void onBossDeath(LivingDeathEvent event) {
        if (!event.getEntity().getType().is(SOLOBOSS)) return;

        Entity source = event.getSource().getEntity();
        if (source == null) return;

        // Resolve the player — direct kill or pet kill
        ServerPlayer player = ShadowKillCreditHelper.creditedServerPlayer(event.getEntity().level(), source);
        if (player == null || player.level().isClientSide()) return;

        GuildSavedData data  = GuildSavedData.get(player.serverLevel());
        GuildData      guild = data.getGuildForPlayer(player.getUUID());
        if (guild == null) return;

        // Determine XP from the dimension the boss died in
        ResourceKey<Level> dim = event.getEntity().level().dimension();
        String dimPath = dim.location().getPath(); // e.g. "dungeon_dimension_s"

        // System dungeons (Igris, Kasaka, DKC, etc.) never award guild XP
        if (EXCLUDED_DIMS.contains(dimPath)) return;

        int xp = DIM_XP.getOrDefault(dimPath, DEFAULT_XP);

        guild.awardXp(xp);
        guild.totalClears++;
        data.markDirty();

        player.sendSystemMessage(Component.literal(
                "§6[Guild] §eBoss cleared! §7Your guild §e" + guild.name
                + " §7gained §a" + xp + " XP§7."));
    }
}
