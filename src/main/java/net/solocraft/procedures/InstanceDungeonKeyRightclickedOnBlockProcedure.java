package net.solocraft.procedures;

import net.solocraft.network.SololevelingModVariables;
import net.solocraft.init.SololevelingModItems;
import net.solocraft.init.SololevelingModBlocks;
import net.solocraft.SololevelingMod;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.BlockPos;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.CommandSource;
import net.minecraft.world.phys.Vec3;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Mth;

public class InstanceDungeonKeyRightclickedOnBlockProcedure {
    public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
        if (!(entity instanceof ServerPlayer player)) return;

        // Ensure the block is correct
        if (world.getBlockState(BlockPos.containing(x, y, z)).getBlock() != SololevelingModBlocks.INSTANCE_DUNGEON_KEY_LOGGER.get()) return;

        // Ensure quest progress is correct
        entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
            if (capability.QuestProgression == 1 && "Getting Stronger".equals(capability.MainQuest)) {
                capability.QuestProgression = 2;
                capability.syncPlayerVariables(entity);
            }
        });

        // Remove dungeon key item
        if (player.getInventory().contains(new ItemStack(SololevelingModItems.INSTANCE_DUNGEON_KEY.get()))) {
            player.getInventory().clearOrCountMatchingItems(stack -> stack.getItem() == SololevelingModItems.INSTANCE_DUNGEON_KEY.get(), 1, player.inventoryMenu.getCraftSlots());
        }

        // Store previous location
        entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
            capability.DunX = entity.getX();
            capability.DunY = entity.getY();
            capability.DunZ = entity.getZ();
            capability.syncPlayerVariables(entity);
        });

        // Get the target dimension
        ResourceKey<Level> dungeonDimension = ResourceKey.create(Registries.DIMENSION, new ResourceLocation("sololeveling:dungeon_dimension_kasaka"));
        ServerLevel targetWorld = player.server.getLevel(dungeonDimension);
        if (targetWorld == null) return;

        // Generate random teleportation coordinates
        final int randX = Mth.nextInt(RandomSource.create(), -29999999, 29999999);
        int randY = Mth.nextInt(RandomSource.create(), 60, 120);
        final int randZ = Mth.nextInt(RandomSource.create(), -29999999, 29999999);

        // Ensure safe teleportation (avoid teleporting into solid blocks)
        while (!targetWorld.getBlockState(new BlockPos(randX, randY, randZ)).isAir() && randY < 200) {
            randY++;
        }
        final int safeRandY = randY;

        // Store new teleport coordinates
        entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
            capability.randplayerx = randX;
            capability.randplayery = safeRandY;
            capability.randplayerz = randZ;
            capability.syncPlayerVariables(entity);
        });

        // First, teleport the player to the dungeon dimension
        SololevelingMod.queueServerWork(10, () -> {
            if (player.level().dimension() != dungeonDimension) {
                player.teleportTo(targetWorld, randX, safeRandY, randZ, player.getYRot(), player.getXRot());
            }

            // Wait a bit, then move the player to the correct location
            SololevelingMod.queueServerWork(10, () -> {
                player.teleportTo(randX, safeRandY, randZ);

                // Spawn the dungeon structure in the correct dimension
                SololevelingMod.queueServerWork(10, () -> {
                    if (!targetWorld.isClientSide() && player.getServer() != null) {
                        player.getServer().getCommands().performPrefixedCommand(
                                new CommandSourceStack(
                                        CommandSource.NULL,
                                        Vec3.atCenterOf(new BlockPos(randX, safeRandY, randZ)),  // ✅ FIX: Convert BlockPos to Vec3
                                        player.getRotationVector(),
                                        targetWorld,
                                        4,
                                        player.getName().getString(),
                                        player.getDisplayName(),
                                        player.getServer(),
                                        player
                                ),
                                "execute in sololeveling:dungeon_dimension_kasaka run spawninstance"
                        );
                    }
                });
            });
        });
    }
}
