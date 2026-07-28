package net.solocraft.entity;

import net.solocraft.dungeon.DatapackDungeonGateHandler;
import net.solocraft.init.SololevelingModEntities;

import net.minecraftforge.network.PlayMessages;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

import javax.annotation.Nullable;

/**
 * A visually shared portal entity whose runtime is exclusively backed by
 * datapack dungeon definitions.
 *
 * <p>This type deliberately overrides Portal1's interaction route. It can never
 * enter the built-in procedural generator or roll a Red Gate.</p>
 */
public class DatapackGateEntity extends Portal1Entity {
	public static final String TEXTURE_NAME = "gate_zero_purple";

	public DatapackGateEntity(PlayMessages.SpawnEntity packet, Level level) {
		this(SololevelingModEntities.DATAPACK_GATE.get(), level);
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	public DatapackGateEntity(EntityType<? extends DatapackGateEntity> type, Level level) {
		// Portal1Entity predates subtype-friendly entity type generics. The runtime
		// type remains the DatapackGate EntityType supplied by Forge.
		super((EntityType) type, level);
		setTexture(TEXTURE_NAME);
	}

	@Override
	public SpawnGroupData finalizeSpawn(ServerLevelAccessor level,
			DifficultyInstance difficulty, MobSpawnType reason,
			@Nullable SpawnGroupData spawnData, @Nullable CompoundTag entityTag) {
		SpawnGroupData result = super.finalizeSpawn(level, difficulty, reason,
				spawnData, entityTag);
		setTexture(TEXTURE_NAME);
		DatapackDungeonGateHandler.initializeSpawn(this, reason);
		return result;
	}

	@Override
	public InteractionResult mobInteract(Player player, InteractionHand hand) {
		if (level().isClientSide())
			return InteractionResult.SUCCESS;
		if (player instanceof ServerPlayer serverPlayer)
			DatapackDungeonGateHandler.interact(serverPlayer, this);
		return InteractionResult.CONSUME;
	}

	@Override
	public void baseTick() {
		super.baseTick();
		if (!level().isClientSide() && tickCount > 1)
			DatapackDungeonGateHandler.discardInvalidUnboundSpawn(this);
	}

	@Override
	public void addAdditionalSaveData(CompoundTag tag) {
		super.addAdditionalSaveData(tag);
		DatapackDungeonGateHandler.writeAdditionalSaveData(this, tag);
	}

	@Override
	public void readAdditionalSaveData(CompoundTag tag) {
		super.readAdditionalSaveData(tag);
		setTexture(TEXTURE_NAME);
		DatapackDungeonGateHandler.readAdditionalSaveData(this, tag);
	}
}
