package net.solocraft.util;

import net.solocraft.SololevelingMod;
import net.solocraft.init.SololevelingModEntities;
import net.solocraft.init.SololevelingModItems;
import net.solocraft.init.SololevelingModParticleTypes;
import net.solocraft.network.SololevelingModVariables;
import net.solocraft.entity.BeruShadowEntity;
import net.solocraft.entity.GoblinArcherShadowEntity;
import net.solocraft.entity.GoblinClubShadowEntity;
import net.solocraft.entity.GoblinMageShadowEntity;
import net.solocraft.entity.IgrisShadowEntity;
import net.solocraft.entity.KamishShadowEntity;
import net.solocraft.entity.ShadowKaiselinEntity;
import net.solocraft.entity.ShadowGreenOrcEntity;
import net.solocraft.entity.ShadowHighOrcEntity;
import net.solocraft.entity.ShadowPolarBearEntity;
import net.solocraft.entity.ShadowSold1Entity;
import net.solocraft.entity.SteelFangWolfShadowEntity;
import net.solocraft.entity.TuskShadowEntity;
import net.solocraft.procedures.SkillSlotHelper;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class ShadowMonarchManager {
	public static final String FORMATION_PREFIX = "Formation:";
	public static final int FORMATION_COLOR = 0xB965FF;
	public static final String COMMAND_DEFAULT = "default";
	public static final String COMMAND_PROTECT = "protect";
	public static final String COMMAND_BERSERK = "berserk";
	public static final String COMMAND_FOLLOW = "follow";
	public static final String COMMAND_CLEAR_DUNGEON = "clear_dungeon";
	private static final String ROOT = "sololeveling_shadow_monarch";
	private static final String SHADOWS = "shadows";
	private static final String FORMATIONS = "formations";
	private static final String SHADOW_ID = "sl_shadow_id";
	private static final String SHADOW_TYPE = "sl_shadow_type";
	private static final String SHADOW_OWNER = "sl_shadow_owner";
	private static final String SHADOW_COMMAND = "sl_shadow_command";
	private static final String PLAYER_COMMAND = "sl_shadow_command_mode";
	private static final String SHADOW_INVENTORY = "sl_shadow_inventory";
	private static final String BASE_HEALTH = "sl_shadow_base_health";
	private static final String BASE_ATTACK = "sl_shadow_base_attack";
	private static final String SAVED_HEALTH = "health";
	private static final String SAVED_HEALTH_AT = "health_saved_at";
	private static final TagKey<EntityType<?>> SHADOW_ENTITY_TAG = TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation("shadows"));

	private ShadowMonarchManager() {
	}

	public static boolean isFormationSkill(String skill) {
		return skill != null && skill.startsWith(FORMATION_PREFIX);
	}

	public static String displaySkillName(Entity entity, String skill) {
		if (isFormationSkill(skill)) {
			String embeddedName = formationNameFromSkill(skill);
			if (!embeddedName.isEmpty())
				return embeddedName;
			CompoundTag formation = getFormation(entity, formationIdFromSkill(skill));
			return formation == null ? "Formation" : formation.getString("name");
		}
		return "Critical Strike".equals(skill) ? "Cross Strike" : skill;
	}

	public static int skillColor(Entity entity, String skill) {
		if (isFormationSkill(skill))
			return FORMATION_COLOR;
		if (JobSkillManager.isJobSkill(skill))
			return JobSkillManager.skillColor(skill);
		return 0xFFFFFF;
	}

	public static boolean summonType(LevelAccessor world, double x, double y, double z, Entity caster, String type) {
		if (!(caster instanceof ServerPlayer player) || !(world instanceof ServerLevel level))
			return false;
		type = normalizeShadowType(type);
		if (type.isEmpty())
			return false;
		ensureRoster(player);
		absorbVisibleOwnedShadows(player);
		enforceSummonedLimit(player, type);
		CompoundTag shadow = firstSummonableShadow(player, type);
		if (shadow == null)
			return false;
		return summonShadow(level, player, shadow, new Vec3(x, y, z), true);
	}

	public static int summonAllOfType(LevelAccessor world, double x, double y, double z, Entity caster, String type) {
		if (!(caster instanceof ServerPlayer player) || !(world instanceof ServerLevel level))
			return 0;
		type = normalizeShadowType(type);
		if (type.isEmpty())
			return 0;
		ensureRoster(player);
		absorbVisibleOwnedShadows(player);
		enforceSummonedLimit(player, type);
		List<CompoundTag> matching = ownedRosterWithinLimit(player, type);
		int summoned = 0;
		Vec3 origin = new Vec3(x, y, z);
		for (CompoundTag shadow : matching) {
			Vec3 pos = spreadSummonPosition(player, origin, summoned);
			if (summonShadow(level, player, shadow, pos, true))
				summoned++;
		}
		return summoned;
	}

	public static boolean castFormation(LevelAccessor world, Entity caster, String skill) {
		if (!(caster instanceof ServerPlayer player) || !(world instanceof ServerLevel level) || !isFormationSkill(skill))
			return false;
		ensureRoster(player);
		CompoundTag formation = getFormation(player, formationIdFromSkill(skill));
		if (formation == null)
			return false;
		ListTag members = formation.getList("members", Tag.TAG_COMPOUND);
		Vec3 look = player.getLookAngle();
		Vec3 forward = new Vec3(look.x, 0, look.z);
		if (forward.lengthSqr() < 0.001)
			forward = new Vec3(0, 0, 1);
		forward = forward.normalize();
		Vec3 right = new Vec3(-forward.z, 0, forward.x);
		Vec3 origin = player.position().add(forward.scale(2.5));
		boolean summonedAny = false;
		for (int i = 0; i < members.size(); i++) {
			CompoundTag member = members.getCompound(i);
			CompoundTag shadow = getShadow(player, member.getString("id"));
			if (shadow == null)
				continue;
			Vec3 pos = origin.add(right.scale(member.getDouble("rx"))).add(0, member.getDouble("ry"), 0).add(forward.scale(member.getDouble("rz")));
			summonedAny |= summonShadow(level, player, shadow, pos, true);
		}
		return summonedAny;
	}

	public static String saveFormationFromSummoned(Player player, String requestedName) {
		if (!(player.level() instanceof ServerLevel))
			return "";
		ensureRoster(player);
		absorbVisibleOwnedShadows(player);
		List<CompoundTag> shadows = summonedOwnedShadows(player);
		if (shadows.isEmpty())
			return "";
		String id = UUID.randomUUID().toString();
		String name = cleanFormationName(requestedName, formationCount(player) + 1);
		Vec3 look = player.getLookAngle();
		Vec3 forward = new Vec3(look.x, 0, look.z);
		if (forward.lengthSqr() < 0.001)
			forward = new Vec3(0, 0, 1);
		forward = forward.normalize();
		Vec3 right = new Vec3(-forward.z, 0, forward.x);
		ListTag members = new ListTag();
		for (CompoundTag shadow : shadows) {
			UUID summonedId = shadow.getUUID("summoned");
			Entity summoned = ((ServerLevel) player.level()).getEntity(summonedId);
			if (summoned == null)
				continue;
			Vec3 delta = summoned.position().subtract(player.position());
			CompoundTag member = new CompoundTag();
			member.putString("id", shadow.getString("id"));
			member.putDouble("rx", delta.dot(right));
			member.putDouble("ry", Math.max(-1.0, Math.min(3.0, delta.y)));
			member.putDouble("rz", delta.dot(forward));
			members.add(member);
		}
		if (members.isEmpty())
			return "";
		CompoundTag formation = new CompoundTag();
		formation.putString("id", id);
		formation.putString("name", name);
		formation.put("members", members);
		formations(player).add(formation);
		appendFormationSkill(player, id, name);
		player.getPersistentData().put(ROOT, root(player));
		return name;
	}

	public static boolean removeFormationSkill(Player player, int skillIndex) {
		if (player == null || skillIndex < 1)
			return false;
		SololevelingModVariables.PlayerVariables vars = player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables());
		List<String> skills = parseSkillList(vars.Plist);
		if (skillIndex > skills.size())
			return false;
		String removed = skills.get(skillIndex - 1);
		if (!isFormationSkill(removed))
			return false;
		String formationId = formationIdFromSkill(removed);
		skills.remove(skillIndex - 1);
		removeFormationData(player, formationId);
		player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
			capability.Plist = writeSkillList(skills);
			for (int slot = 1; slot <= 16; slot++) {
				if (removed.equals(SkillSlotHelper.getSlot(capability, slot)))
					SkillSlotHelper.setSlot(capability, slot, "");
			}
			if (removed.equals(capability.PselectedPower))
				capability.PselectedPower = "";
			capability.syncPlayerVariables(player);
		});
		return true;
	}

	public static List<String> formationSkills(Player player) {
		ArrayList<String> result = new ArrayList<>();
		if (player == null)
			return result;
		ListTag list = formations(player);
		for (int i = 0; i < list.size(); i++) {
			CompoundTag formation = list.getCompound(i);
			String id = formation.getString("id");
			if (!id.isEmpty())
				result.add(FORMATION_PREFIX + id + "|" + cleanFormationName(formation.getString("name"), i + 1));
		}
		return result;
	}

	public static void grantKillXp(Player owner, Entity shadowEntity, Entity killed) {
		if (owner == null || shadowEntity == null || killed == null)
			return;
		ensureRoster(owner);
		String id = shadowEntity.getPersistentData().getString(SHADOW_ID);
		if (id.isEmpty())
			return;
		CompoundTag shadow = getShadow(owner, id);
		if (shadow == null)
			return;
		int level = Math.max(1, shadow.getInt("level"));
		int xp = shadow.getInt("xp") + Math.max(5, (int) Math.ceil(killed.getBbWidth() * killed.getBbHeight() * 4.0));
		int needed = xpNeeded(level);
		boolean leveled = false;
		while (xp >= needed) {
			xp -= needed;
			level++;
			needed = xpNeeded(level);
			leveled = true;
		}
		shadow.putInt("level", level);
		shadow.putInt("xp", xp);
		owner.getPersistentData().put(ROOT, root(owner));
		if (leveled)
			applyLevelStats(shadowEntity, shadow, false);
		if (leveled && owner instanceof ServerPlayer player)
			player.displayClientMessage(Component.literal(shadow.getString("name") + " reached Lv." + level), true);
	}

	public static void collectManaStoneDropsFromKill(Entity shadowEntity, Entity killed) {
		if (shadowEntity == null || killed == null || !(killed.level() instanceof ServerLevel level))
			return;
		Vec3 dropPos = killed.position();
		SololevelingMod.queueServerWork(1, () -> {
			if (shadowEntity.isRemoved() || !isTrackedShadowEntity(shadowEntity))
				return;
			AABB area = new AABB(dropPos, dropPos).inflate(3.0D);
			for (ItemEntity itemEntity : level.getEntitiesOfClass(ItemEntity.class, area, itemEntity -> isCollectibleManaStone(itemEntity.getItem()))) {
				ItemStack stack = itemEntity.getItem().copy();
				if (stack.isEmpty())
					continue;
				addStackToShadowInventory(shadowEntity, stack);
				itemEntity.discard();
			}
		});
	}

	public static void dropStoredShadowInventory(Entity shadowEntity) {
		if (shadowEntity == null || shadowEntity.level().isClientSide())
			return;
		saveBossHealthBeforeDespawn(null, shadowEntity);
		CompoundTag data = shadowEntity.getPersistentData();
		if (!data.contains(SHADOW_INVENTORY, Tag.TAG_LIST))
			return;
		ListTag inventory = data.getList(SHADOW_INVENTORY, Tag.TAG_COMPOUND);
		for (int i = 0; i < inventory.size(); i++) {
			ItemStack stack = ItemStack.of(inventory.getCompound(i));
			if (!stack.isEmpty())
				shadowEntity.spawnAtLocation(stack);
		}
		data.remove(SHADOW_INVENTORY);
	}

	public static void saveBossHealthBeforeDespawn(Entity ownerEntity, Entity shadowEntity) {
		Player owner = ownerEntity instanceof Player player ? player : null;
		saveBossHealthBeforeDespawn(owner, shadowEntity);
	}

	private static void saveBossHealthBeforeDespawn(Player owner, Entity shadowEntity) {
		if (!(shadowEntity instanceof LivingEntity living) || shadowEntity.level().isClientSide())
			return;
		if (!living.isAlive() || living.getHealth() <= 0.0F)
			return;
		CompoundTag data = shadowEntity.getPersistentData();
		String type = data.getString(SHADOW_TYPE);
		if (type.isEmpty())
			type = typeFromEntity(shadowEntity);
		if (!isBoss(type))
			return;
		if (owner == null && shadowEntity.level() instanceof ServerLevel level && data.hasUUID(SHADOW_OWNER))
			owner = level.getPlayerByUUID(data.getUUID(SHADOW_OWNER));
		if (owner == null)
			return;
		String id = data.getString(SHADOW_ID);
		if (id.isEmpty())
			return;
		CompoundTag shadow = getShadow(owner, id);
		if (shadow == null)
			return;
		shadow.putDouble(SAVED_HEALTH, Math.max(1.0D, Math.min(living.getHealth(), living.getMaxHealth())));
		shadow.putLong(SAVED_HEALTH_AT, shadowEntity.level().getGameTime());
		shadow.remove("summoned");
		owner.getPersistentData().put(ROOT, root(owner));
	}

	public static void tagExistingSummon(Player owner, Entity summoned, String type) {
		if (owner == null || summoned == null || type == null || owner.level().isClientSide())
			return;
		ensureRoster(owner);
		CompoundTag shadow = firstAvailableShadow(owner, type);
		if (shadow == null)
			shadow = createShadow(owner, type, countOwned(owner, type) + 1);
		tagSummonedEntity(owner, shadow, summoned);
	}

	public static boolean modifyShadowAmount(Player player, String requestedType, int amount) {
		if (player == null || requestedType == null || amount == 0)
			return false;
		String type = normalizeShadowType(requestedType);
		if (type.isEmpty())
			return false;
		ensureRoster(player);
		int current = legacyMax(player, type);
		int updated = Math.max(0, current + amount);
		setLegacyMax(player, type, updated);
		if (updated < current)
			trimOwnedShadows(player, type, updated);
		else
			ensureRoster(player);
		player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> capability.syncPlayerVariables(player));
		return true;
	}

	public static boolean dismissShadowType(Player player, String requestedType) {
		if (player == null || requestedType == null || player.level().isClientSide())
			return false;
		String type = normalizeShadowType(requestedType);
		if (!isDismissibleShadowType(type))
			return false;
		ensureRoster(player);
		if (legacyMax(player, type) <= 0)
			return false;
		if (!modifyShadowAmount(player, type, -1))
			return false;
		player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
			capability.shadowstorageusage = Math.max(0, capability.shadowstorageusage - 1);
			capability.syncPlayerVariables(player);
		});
		player.getPersistentData().put(ROOT, root(player));
		return true;
	}

	public static boolean isInDungeon(Player player) {
		if (player == null)
			return false;
		SololevelingModVariables.PlayerVariables vars = player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables());
		String dimension = player.level().dimension().location().getPath();
		return vars.dungeoning || vars.dkc_started || dimension.contains("dungeon") || dimension.contains("castle");
	}

	public static boolean isTrackedShadowEntity(Entity entity) {
		return entity != null && entity.getPersistentData().hasUUID(SHADOW_OWNER);
	}

	public static boolean isShadowEntity(Entity entity) {
		return entity != null && entity.getType().is(SHADOW_ENTITY_TAG);
	}

	public static UUID getShadowOwnerUUID(Entity entity) {
		if (entity == null)
			return null;
		if (entity instanceof TamableAnimal tame && tame.getOwnerUUID() != null)
			return tame.getOwnerUUID();
		CompoundTag data = entity.getPersistentData();
		return data.hasUUID(SHADOW_OWNER) ? data.getUUID(SHADOW_OWNER) : null;
	}

	public static boolean isOwnedShadow(Entity shadow, LivingEntity owner) {
		UUID ownerId = getShadowOwnerUUID(shadow);
		return isShadowEntity(shadow) && owner != null && ownerId != null && ownerId.equals(owner.getUUID());
	}

	public static boolean haveSameShadowOwner(Entity first, Entity second) {
		UUID firstOwner = getShadowOwnerUUID(first);
		UUID secondOwner = getShadowOwnerUUID(second);
		return isShadowEntity(first) && isShadowEntity(second) && firstOwner != null && firstOwner.equals(secondOwner);
	}

	public static boolean commandSummonedShadows(Player player, String requestedCommand) {
		if (!(player instanceof ServerPlayer serverPlayer))
			return false;
		String command = normalizeCommand(requestedCommand);
		if (command.isEmpty())
			return false;
		if (COMMAND_CLEAR_DUNGEON.equals(command) && !isInDungeon(player)) {
			serverPlayer.displayClientMessage(Component.literal("Clear Dungeon can only be used inside a dungeon."), true);
			return false;
		}
		ensureRoster(player);
		absorbVisibleOwnedShadows(player);
		player.getPersistentData().putString(PLAYER_COMMAND, command);
		player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
			capability.berserk = COMMAND_BERSERK.equals(command);
			capability.syncPlayerVariables(player);
		});
		List<CompoundTag> summoned = summonedOwnedShadows(player);
		for (CompoundTag shadow : summoned) {
			Entity entity = serverPlayer.serverLevel().getEntity(shadow.getUUID("summoned"));
			if (entity == null || !entity.isAlive())
				continue;
			entity.getPersistentData().putString(SHADOW_COMMAND, command);
			if (entity instanceof Mob mob)
				applyCommandTarget(mob, player, command);
		}
		serverPlayer.displayClientMessage(Component.literal("Shadow Command: " + commandDisplayName(command) + " (" + summoned.size() + " shadows)"), true);
		return true;
	}

	public static boolean hasShadowForDisplay(Player player, String type) {
		return ownedCountForDisplay(player, type) > 0;
	}

	public static String shadowCountText(Player player, String type) {
		if (player == null)
			return "0/0";
		int owned = ownedCountForDisplay(player, type);
		int summoned = Math.min(owned, summonedCountForDisplay(player, type));
		return summoned + "/" + owned;
	}

	private static int ownedCountForDisplay(Player player, String type) {
		if (player == null)
			return 0;
		String normalized = normalizeShadowType(type);
		if (normalized.isEmpty())
			return 0;
		return Math.max(legacyMax(player, normalized), countOwned(player, normalized));
	}

	private static int summonedCountForDisplay(Player player, String type) {
		if (player == null)
			return 0;
		String normalized = normalizeShadowType(type);
		if (normalized.isEmpty())
			return 0;
		int rosterCount = 0;
		ListTag shadows = shadows(player);
		for (int i = 0; i < shadows.size(); i++) {
			CompoundTag shadow = shadows.getCompound(i);
			if (normalized.equals(shadow.getString("type")) && shadow.hasUUID("summoned"))
				rosterCount++;
		}
		return Math.max(legacySpawned(player, normalized), rosterCount);
	}

	public static void tickCommandedShadow(Entity entity) {
		if (!(entity instanceof Mob mob) || !(entity.level() instanceof ServerLevel level))
			return;
		CompoundTag data = entity.getPersistentData();
		if (!data.hasUUID(SHADOW_OWNER))
			return;
		Player owner = findOnlineOwner(level, data.getUUID(SHADOW_OWNER));
		if (owner != null && !isCurrentSummonedInstance(owner, entity)) {
			entity.discard();
			return;
		}
		String command = commandOrDefault(data.getString(SHADOW_COMMAND));
		if (owner == null || !owner.isAlive()) {
			mob.setTarget(null);
			return;
		}
		applyCommandTarget(mob, owner, command);
	}

	private static boolean summonShadow(ServerLevel level, ServerPlayer owner, CompoundTag shadow, Vec3 pos, boolean allowRecall) {
		if (allowRecall && shadow.hasUUID("summoned")) {
			Entity existing = findSummonedEntity(owner, shadow.getUUID("summoned"));
			if (existing != null && existing.isAlive()) {
				if (existing.level() == level) {
					existing.teleportTo(pos.x, pos.y, pos.z);
					existing.setYRot(owner.getYRot());
					existing.setXRot(0);
					applyLevelStats(existing, shadow, false);
					playSummonEffects(level, pos);
					return true;
				}
				return recallShadowFromOtherDimension(level, owner, shadow, existing, pos);
			}
			shadow.remove("summoned");
			owner.getPersistentData().put(ROOT, root(owner));
		}
		EntityType<?> type = entityType(shadow.getString("type"));
		if (type == null)
			return false;
		Entity spawned = type.spawn(level, BlockPos.containing(pos), MobSpawnType.MOB_SUMMONED);
		if (spawned == null)
			return false;
		spawned.moveTo(pos.x, pos.y, pos.z, owner.getYRot(), 0);
		tagSummonedEntity(owner, shadow, spawned);
		updateLegacySpawnCounter(owner, shadow.getString("type"), 1);
		playSummonEffects(level, pos);
		return true;
	}

	private static boolean recallShadowFromOtherDimension(ServerLevel level, ServerPlayer owner, CompoundTag shadow, Entity existing, Vec3 pos) {
		EntityType<?> type = entityType(shadow.getString("type"));
		if (type == null)
			return false;
		ListTag carriedInventory = copyShadowInventory(existing);
		saveBossHealthBeforeDespawn(owner, existing);
		existing.discard();
		Entity spawned = type.spawn(level, BlockPos.containing(pos), MobSpawnType.MOB_SUMMONED);
		if (spawned == null)
			return false;
		spawned.moveTo(pos.x, pos.y, pos.z, owner.getYRot(), 0);
		tagSummonedEntity(owner, shadow, spawned);
		if (carriedInventory != null)
			spawned.getPersistentData().put(SHADOW_INVENTORY, carriedInventory);
		playSummonEffects(level, pos);
		return true;
	}

	private static ListTag copyShadowInventory(Entity entity) {
		if (entity == null || !entity.getPersistentData().contains(SHADOW_INVENTORY, Tag.TAG_LIST))
			return null;
		return entity.getPersistentData().getList(SHADOW_INVENTORY, Tag.TAG_COMPOUND).copy();
	}

	private static void playSummonEffects(ServerLevel level, Vec3 pos) {
		LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
		if (bolt != null) {
			bolt.moveTo(Vec3.atBottomCenterOf(BlockPos.containing(pos.x, pos.y - 1.0D, pos.z)));
			bolt.setVisualOnly(true);
			level.addFreshEntity(bolt);
		}
		level.sendParticles((SimpleParticleType) SololevelingModParticleTypes.SHADOW_REVIVE.get(), pos.x, pos.y + 1.6D, pos.z, 20, 0.35D, 0.65D, 0.35D, 0.04D);
		level.sendParticles(ParticleTypes.SQUID_INK, pos.x, pos.y + 1.0D, pos.z, 80, 1.3D, 1.2D, 1.3D, 0.18D);
		level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, pos.x, pos.y + 0.8D, pos.z, 24, 0.8D, 0.8D, 0.8D, 0.04D);
		level.sendParticles(ParticleTypes.ELECTRIC_SPARK, pos.x, pos.y + 1.2D, pos.z, 35, 0.6D, 0.9D, 0.6D, 0.15D);
	}

	private static void tagSummonedEntity(Player owner, CompoundTag shadow, Entity spawned) {
		spawned.getPersistentData().putString(SHADOW_ID, shadow.getString("id"));
		spawned.getPersistentData().putString(SHADOW_TYPE, shadow.getString("type"));
		spawned.getPersistentData().putUUID(SHADOW_OWNER, owner.getUUID());
		String command = commandOrDefault(owner.getPersistentData().getString(PLAYER_COMMAND));
		spawned.getPersistentData().putString(SHADOW_COMMAND, command);
		shadow.putUUID("summoned", spawned.getUUID());
		owner.getPersistentData().put(ROOT, root(owner));
		if (spawned instanceof TamableAnimal tame)
			tame.tame(owner);
		applyLevelStats(spawned, shadow, true);
		if (spawned instanceof Mob mob)
			applyCommandTarget(mob, owner, command);
	}

	private static Vec3 spreadSummonPosition(Player player, Vec3 origin, int index) {
		if (index <= 0)
			return origin;
		Vec3 look = player.getLookAngle();
		Vec3 forward = new Vec3(look.x, 0.0D, look.z);
		if (forward.lengthSqr() < 0.001D)
			forward = new Vec3(0.0D, 0.0D, 1.0D);
		forward = forward.normalize();
		Vec3 right = new Vec3(-forward.z, 0.0D, forward.x);
		int ringIndex = index - 1;
		int slot = ringIndex % 8;
		int ring = ringIndex / 8;
		double radius = 1.6D + ring * 1.15D;
		double angle = slot * (Math.PI * 2.0D / 8.0D) + ring * 0.35D;
		return origin.add(right.scale(Math.cos(angle) * radius)).add(forward.scale(Math.sin(angle) * radius));
	}

	private static void applyCommandTarget(Mob shadow, Player owner, String command) {
		if (COMMAND_DEFAULT.equals(command)) {
			shadow.setTarget(findDefaultCommandTarget(shadow, owner));
			return;
		}
		if (COMMAND_FOLLOW.equals(command)) {
			shadow.setTarget(null);
			shadow.getNavigation().stop();
			return;
		}
		if (COMMAND_CLEAR_DUNGEON.equals(command) && !isInDungeon(owner)) {
			shadow.setTarget(null);
			return;
		}
		if (COMMAND_PROTECT.equals(command)) {
			LivingEntity threat = findOwnerThreat(shadow, owner);
			shadow.setTarget(threat);
			return;
		}
		boolean requireReachablePath = COMMAND_CLEAR_DUNGEON.equals(command);
		LivingEntity current = shadow.getTarget();
		if (isValidShadowTarget(current, shadow, owner) && (!requireReachablePath || canReachShadowTarget(shadow, current)))
			return;
		double range = COMMAND_CLEAR_DUNGEON.equals(command) ? 96.0D : 48.0D;
		LivingEntity target = findNearestHostile(shadow, owner, range, requireReachablePath);
		shadow.setTarget(target);
		if (target == null && requireReachablePath)
			shadow.getNavigation().stop();
	}

	public static LivingEntity findDefaultCommandTarget(Mob shadow, Player owner) {
		if (shadow == null || owner == null)
			return null;
		LivingEntity current = shadow.getTarget();
		if (isDefaultLinkedTarget(current, shadow, owner))
			return current;
		LivingEntity ownerTarget = owner.getLastHurtMob();
		if (isValidOwnerDirectedTarget(ownerTarget, shadow, owner))
			return ownerTarget;
		LivingEntity ownerAttacker = owner.getLastHurtByMob();
		if (isValidShadowTarget(ownerAttacker, shadow, owner))
			return ownerAttacker;
		return shadow.level().getEntitiesOfClass(Mob.class, owner.getBoundingBox().inflate(32.0D), mob -> mob.getTarget() == owner && isValidShadowTarget(mob, shadow, owner)).stream()
				.min((a, b) -> Double.compare(a.distanceToSqr(shadow), b.distanceToSqr(shadow))).orElse(null);
	}

	private static boolean isDefaultLinkedTarget(LivingEntity target, Mob shadow, Player owner) {
		if (target == owner.getLastHurtMob())
			return isValidOwnerDirectedTarget(target, shadow, owner);
		if (!isValidShadowTarget(target, shadow, owner))
			return false;
		return target == owner.getLastHurtByMob() || target instanceof Mob mob && mob.getTarget() == owner;
	}

	private static boolean isValidOwnerDirectedTarget(LivingEntity target, Mob shadow, Player owner) {
		if (target == null || !target.isAlive() || target == shadow || target == owner || !target.isAttackable() || target.isInvulnerable())
			return false;
		if (target.getType().is(SHADOW_ENTITY_TAG) || owner.isAlliedTo(target) || shadow.isAlliedTo(target))
			return false;
		if (target instanceof TamableAnimal tame && owner.getUUID().equals(tame.getOwnerUUID()))
			return false;
		return !(target instanceof Player player) || owner.canHarmPlayer(player);
	}

	private static LivingEntity findOwnerThreat(Mob shadow, Player owner) {
		LivingEntity current = shadow.getTarget();
		if (current instanceof Mob mob && mob.getTarget() == owner && isValidShadowTarget(current, shadow, owner))
			return current;
		LivingEntity attacker = owner.getLastHurtByMob();
		if (isValidShadowTarget(attacker, shadow, owner))
			return attacker;
		return shadow.level().getEntitiesOfClass(Mob.class, owner.getBoundingBox().inflate(48.0D), mob -> mob.getTarget() == owner && isValidShadowTarget(mob, shadow, owner)).stream()
				.min((a, b) -> Double.compare(a.distanceToSqr(shadow), b.distanceToSqr(shadow))).orElse(null);
	}

	private static LivingEntity findNearestHostile(Mob shadow, Player owner, double range, boolean requireReachablePath) {
		return shadow.level().getEntitiesOfClass(LivingEntity.class, shadow.getBoundingBox().inflate(range), target -> isValidShadowTarget(target, shadow, owner) && (!requireReachablePath || canReachShadowTarget(shadow, target))).stream()
				.min((a, b) -> Double.compare(a.distanceToSqr(shadow), b.distanceToSqr(shadow))).orElse(null);
	}

	private static boolean isValidShadowTarget(LivingEntity target, Mob shadow, Player owner) {
		if (target == null || !target.isAlive() || target == shadow || target == owner)
			return false;
		if (target instanceof Player)
			return false;
		if (target.getType().is(SHADOW_ENTITY_TAG))
			return false;
		if (target instanceof TamableAnimal tame && owner.getUUID().equals(tame.getOwnerUUID()))
			return false;
		return target instanceof Monster || target instanceof Mob mob && mob.getTarget() == owner;
	}

	public static boolean canReachShadowTarget(Mob shadow, LivingEntity target) {
		if (shadow == null || target == null || !target.isAlive())
			return false;
		if (CombatRangeHelper.withinSurfaceRange(shadow, target, 6.0D) && shadow.hasLineOfSight(target))
			return true;
		Path path = shadow.getNavigation().createPath(target.blockPosition(), 0);
		return path != null && path.canReach();
	}

	private static String normalizeCommand(String command) {
		if (command == null)
			return "";
		String value = command.trim().toLowerCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
		return switch (value) {
			case COMMAND_DEFAULT, COMMAND_PROTECT, COMMAND_BERSERK, COMMAND_FOLLOW, COMMAND_CLEAR_DUNGEON -> value;
			default -> "";
		};
	}

	private static String commandOrDefault(String command) {
		String normalized = normalizeCommand(command);
		return normalized.isEmpty() ? COMMAND_DEFAULT : normalized;
	}

	private static String commandDisplayName(String command) {
		return switch (command) {
			case COMMAND_DEFAULT -> "Default";
			case COMMAND_PROTECT -> "Protect";
			case COMMAND_BERSERK -> "Berserk";
			case COMMAND_FOLLOW -> "Follow";
			case COMMAND_CLEAR_DUNGEON -> "Clear Dungeon";
			default -> "Unknown";
		};
	}

	private static void applyLevelStats(Entity entity, CompoundTag shadow, boolean restoreSavedBossHealth) {
		if (!(entity instanceof LivingEntity living))
			return;
		int level = Math.max(1, shadow.getInt("level"));
		String type = shadow.getString("type");
		if (living.getAttribute(Attributes.MAX_HEALTH) != null) {
			CompoundTag data = living.getPersistentData();
			if (!data.contains(BASE_HEALTH))
				data.putDouble(BASE_HEALTH, living.getAttribute(Attributes.MAX_HEALTH).getBaseValue());
			double base = data.getDouble(BASE_HEALTH);
			living.getAttribute(Attributes.MAX_HEALTH).setBaseValue(base + (level - 1) * healthGain(type));
			if (isBoss(type))
				applyBossHealth(living, shadow, restoreSavedBossHealth);
			else
				living.setHealth(living.getMaxHealth());
		}
		if (living.getAttribute(Attributes.ATTACK_DAMAGE) != null) {
			CompoundTag data = living.getPersistentData();
			if (!data.contains(BASE_ATTACK))
				data.putDouble(BASE_ATTACK, living.getAttribute(Attributes.ATTACK_DAMAGE).getBaseValue());
			double base = data.getDouble(BASE_ATTACK);
			living.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(base + (level - 1) * attackGain(type));
		}
		entity.setCustomName(Component.literal(shadow.getString("name") + " Lv." + level));
	}

	private static void applyBossHealth(LivingEntity living, CompoundTag shadow, boolean restoreSavedHealth) {
		float maxHealth = living.getMaxHealth();
		if (!restoreSavedHealth) {
			living.setHealth(Math.max(1.0F, Math.min(living.getHealth(), maxHealth)));
			return;
		}
		if (!shadow.contains(SAVED_HEALTH)) {
			living.setHealth(maxHealth);
			return;
		}
		double savedHealth = shadow.getDouble(SAVED_HEALTH);
		long savedAt = shadow.getLong(SAVED_HEALTH_AT);
		long now = living.level().getGameTime();
		double regenerated = savedHealth + Math.max(0L, now - savedAt) / 20.0D;
		living.setHealth((float) Math.max(1.0D, Math.min(maxHealth, regenerated)));
		shadow.putDouble(SAVED_HEALTH, living.getHealth());
		shadow.putLong(SAVED_HEALTH_AT, now);
	}

	private static void addStackToShadowInventory(Entity shadowEntity, ItemStack stack) {
		if (!isCollectibleManaStone(stack))
			return;
		CompoundTag data = shadowEntity.getPersistentData();
		ListTag inventory = data.getList(SHADOW_INVENTORY, Tag.TAG_COMPOUND);
		int remaining = stack.getCount();
		for (int i = 0; i < inventory.size() && remaining > 0; i++) {
			ItemStack stored = ItemStack.of(inventory.getCompound(i));
			if (!ItemStack.isSameItemSameTags(stored, stack) || stored.getCount() >= stored.getMaxStackSize())
				continue;
			int move = Math.min(remaining, stored.getMaxStackSize() - stored.getCount());
			stored.grow(move);
			remaining -= move;
			inventory.set(i, stored.save(new CompoundTag()));
		}
		while (remaining > 0) {
			ItemStack stored = stack.copy();
			stored.setCount(Math.min(remaining, stored.getMaxStackSize()));
			remaining -= stored.getCount();
			inventory.add(stored.save(new CompoundTag()));
		}
		data.put(SHADOW_INVENTORY, inventory);
	}

	private static boolean isCollectibleManaStone(ItemStack stack) {
		return stack != null && !stack.isEmpty() && isCollectibleManaStone(stack.getItem());
	}

	private static boolean isCollectibleManaStone(Item item) {
		return item == SololevelingModItems.MANA_CRYSTAL_E.get()
				|| item == SololevelingModItems.MANA_CRYSTAL_D.get()
				|| item == SololevelingModItems.MANA_CRYSTAL_C.get()
				|| item == SololevelingModItems.MANA_CRYSTAL_B.get()
				|| item == SololevelingModItems.MANA_CRYSTAL_A.get();
	}

	private static CompoundTag firstSummonableShadow(Player player, String type) {
		CompoundTag available = firstAvailableShadow(player, type);
		if (available != null)
			return available;
		CompoundTag bestActive = null;
		for (CompoundTag shadow : ownedRosterWithinLimit(player, type)) {
			if (!shadow.hasUUID("summoned"))
				continue;
			Entity existing = findSummonedEntity(player, shadow.getUUID("summoned"));
			if (existing != null && existing.isAlive() && isBetterShadow(shadow, bestActive))
				bestActive = shadow;
		}
		return bestActive;
	}

	private static CompoundTag firstAvailableShadow(Player player, String type) {
		CompoundTag best = null;
		for (CompoundTag shadow : ownedRosterWithinLimit(player, type)) {
			boolean available = !shadow.hasUUID("summoned");
			if (!available) {
				Entity existing = findSummonedEntity(player, shadow.getUUID("summoned"));
				available = existing == null || !existing.isAlive();
				if (available)
					shadow.remove("summoned");
			}
			if (available && isBetterShadow(shadow, best))
				best = shadow;
		}
		if (best != null)
			return best;
		int max = legacyMax(player, type);
		int count = countOwned(player, type);
		if (count < max)
			return createShadow(player, type, count + 1);
		return null;
	}

	private static List<CompoundTag> ownedRosterWithinLimit(Player player, String type) {
		int limit = Math.max(0, legacyMax(player, type));
		if (limit == 0)
			return List.of();
		ArrayList<CompoundTag> matching = new ArrayList<>();
		ListTag roster = shadows(player);
		for (int i = 0; i < roster.size(); i++) {
			CompoundTag shadow = roster.getCompound(i);
			if (type.equals(shadow.getString("type")))
				matching.add(shadow);
		}
		matching.sort(ShadowMonarchManager::compareStrongestFirst);
		if (matching.size() > limit)
			return new ArrayList<>(matching.subList(0, limit));
		return matching;
	}

	private static int compareStrongestFirst(CompoundTag first, CompoundTag second) {
		int byLevel = Integer.compare(Math.max(1, second.getInt("level")), Math.max(1, first.getInt("level")));
		if (byLevel != 0)
			return byLevel;
		int byXp = Integer.compare(second.getInt("xp"), first.getInt("xp"));
		if (byXp != 0)
			return byXp;
		return first.getString("id").compareTo(second.getString("id"));
	}

	private static void enforceSummonedLimit(ServerPlayer player, String type) {
		List<CompoundTag> allowed = ownedRosterWithinLimit(player, type);
		ArrayList<String> allowedIds = new ArrayList<>(allowed.size());
		for (CompoundTag shadow : allowed)
			allowedIds.add(shadow.getString("id"));
		int removed = 0;
		ListTag roster = shadows(player);
		for (int i = 0; i < roster.size(); i++) {
			CompoundTag shadow = roster.getCompound(i);
			if (!type.equals(shadow.getString("type")) || allowedIds.contains(shadow.getString("id")) || !shadow.hasUUID("summoned"))
				continue;
			Entity existing = findSummonedEntity(player, shadow.getUUID("summoned"));
			if (existing != null) {
				dropStoredShadowInventory(existing);
				existing.discard();
				removed++;
			}
			shadow.remove("summoned");
		}
		if (removed > 0)
			updateLegacySpawnCounter(player, type, -removed);
		player.getPersistentData().put(ROOT, root(player));
	}

	private static boolean isBetterShadow(CompoundTag candidate, CompoundTag current) {
		if (current == null)
			return true;
		int candidateLevel = Math.max(1, candidate.getInt("level"));
		int currentLevel = Math.max(1, current.getInt("level"));
		if (candidateLevel != currentLevel)
			return candidateLevel > currentLevel;
		int candidateXp = candidate.getInt("xp");
		int currentXp = current.getInt("xp");
		if (candidateXp != currentXp)
			return candidateXp > currentXp;
		return candidate.getString("id").compareTo(current.getString("id")) < 0;
	}

	private static CompoundTag createShadow(Player player, String type, int number) {
		CompoundTag shadow = new CompoundTag();
		shadow.putString("id", UUID.randomUUID().toString());
		shadow.putString("type", type);
		shadow.putString("name", defaultName(type, number));
		shadow.putInt("level", 1);
		shadow.putInt("xp", 0);
		shadow.putBoolean("boss", isBoss(type));
		shadows(player).add(shadow);
		return shadow;
	}

	private static void ensureRoster(Player player) {
		if (!player.getPersistentData().contains(ROOT, Tag.TAG_COMPOUND))
			player.getPersistentData().put(ROOT, new CompoundTag());
		for (String type : shadowTypes()) {
			int max = legacyMax(player, type);
			while (countOwned(player, type) < max)
				createShadow(player, type, countOwned(player, type) + 1);
		}
	}

	private static List<CompoundTag> summonedOwnedShadows(Player player) {
		ArrayList<CompoundTag> result = new ArrayList<>();
		if (!(player.level() instanceof ServerLevel level))
			return result;
		ListTag shadows = shadows(player);
		for (int i = 0; i < shadows.size(); i++) {
			CompoundTag shadow = shadows.getCompound(i);
			if (!shadow.hasUUID("summoned"))
				continue;
			Entity entity = level.getEntity(shadow.getUUID("summoned"));
			if (entity != null && entity.isAlive())
				result.add(shadow);
		}
		return result;
	}

	private static void absorbVisibleOwnedShadows(Player player) {
		if (!(player.level() instanceof ServerLevel level))
			return;
		AABB area = player.getBoundingBox().inflate(96);
		for (TamableAnimal tame : level.getEntitiesOfClass(TamableAnimal.class, area, e -> e.getOwnerUUID() != null && e.getOwnerUUID().equals(player.getUUID()))) {
			if (!tame.getPersistentData().getString(SHADOW_ID).isEmpty())
				continue;
			String type = typeFromEntity(tame);
			if (type.isEmpty())
				continue;
			CompoundTag shadow = firstAvailableShadow(player, type);
			if (shadow == null)
				shadow = createShadow(player, type, countOwned(player, type) + 1);
			tagSummonedEntity(player, shadow, tame);
		}
		for (ShadowKaiselinEntity kaisel : level.getEntitiesOfClass(ShadowKaiselinEntity.class, area, e -> e.getOwnerUUID() != null && e.getOwnerUUID().equals(player.getUUID()))) {
			if (!kaisel.getPersistentData().getString(SHADOW_ID).isEmpty())
				continue;
			CompoundTag shadow = firstAvailableShadow(player, "kaisel");
			if (shadow == null)
				shadow = createShadow(player, "kaisel", countOwned(player, "kaisel") + 1);
			tagSummonedEntity(player, shadow, kaisel);
		}
	}

	private static Entity findSummonedEntity(Player player, UUID entityId) {
		if (player == null || entityId == null || !(player.level() instanceof ServerLevel level))
			return null;
		for (ServerLevel serverLevel : level.getServer().getAllLevels()) {
			Entity entity = serverLevel.getEntity(entityId);
			if (entity != null)
				return entity;
		}
		return null;
	}

	private static Player findOnlineOwner(ServerLevel level, UUID ownerId) {
		if (level == null || ownerId == null)
			return null;
		return level.getServer().getPlayerList().getPlayer(ownerId);
	}

	private static boolean isCurrentSummonedInstance(Player owner, Entity entity) {
		if (owner == null || entity == null)
			return true;
		String id = entity.getPersistentData().getString(SHADOW_ID);
		if (id.isEmpty())
			return true;
		CompoundTag shadow = getShadow(owner, id);
		if (shadow == null)
			return true;
		return shadow.hasUUID("summoned") && shadow.getUUID("summoned").equals(entity.getUUID());
	}

	private static CompoundTag getShadow(Entity entity, String id) {
		if (entity == null || id == null)
			return null;
		ListTag shadows = shadows(entity);
		for (int i = 0; i < shadows.size(); i++) {
			CompoundTag shadow = shadows.getCompound(i);
			if (id.equals(shadow.getString("id")))
				return shadow;
		}
		return null;
	}

	private static CompoundTag getFormation(Entity entity, String id) {
		if (entity == null || id == null)
			return null;
		ListTag formations = formations(entity);
		for (int i = 0; i < formations.size(); i++) {
			CompoundTag formation = formations.getCompound(i);
			if (id.equals(formation.getString("id")))
				return formation;
		}
		return null;
	}

	private static void removeFormationData(Entity entity, String id) {
		ListTag formations = formations(entity);
		for (int i = formations.size() - 1; i >= 0; i--) {
			if (id.equals(formations.getCompound(i).getString("id")))
				formations.remove(i);
		}
	}

	private static CompoundTag root(Entity entity) {
		CompoundTag data = entity.getPersistentData();
		if (!data.contains(ROOT, Tag.TAG_COMPOUND))
			data.put(ROOT, new CompoundTag());
		CompoundTag root = data.getCompound(ROOT);
		if (!root.contains(SHADOWS, Tag.TAG_LIST))
			root.put(SHADOWS, new ListTag());
		if (!root.contains(FORMATIONS, Tag.TAG_LIST))
			root.put(FORMATIONS, new ListTag());
		return root;
	}

	private static ListTag shadows(Entity entity) {
		return root(entity).getList(SHADOWS, Tag.TAG_COMPOUND);
	}

	private static ListTag formations(Entity entity) {
		return root(entity).getList(FORMATIONS, Tag.TAG_COMPOUND);
	}

	private static int formationCount(Entity entity) {
		return formations(entity).size();
	}

	private static int countOwned(Player player, String type) {
		int count = 0;
		ListTag shadows = shadows(player);
		for (int i = 0; i < shadows.size(); i++) {
			if (type.equals(shadows.getCompound(i).getString("type")))
				count++;
		}
		return count;
	}

	private static void appendFormationSkill(Player player, String id, String name) {
		String skill = FORMATION_PREFIX + id + "|" + cleanFormationName(name, 1);
		player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
			String list = capability.Plist == null || capability.Plist.isEmpty() ? "." : capability.Plist;
			if (!list.contains(FORMATION_PREFIX + id)) {
				capability.Plist = list + skill + ",";
				capability.syncPlayerVariables(player);
			}
		});
	}

	private static String cleanFormationName(String value, int number) {
		String name = value == null ? "" : value.trim();
		if (name.isEmpty())
			return "Formation " + number;
		name = name.replace(',', ' ').replace('|', ' ');
		return name.length() > 24 ? name.substring(0, 24) : name;
	}

	private static List<String> parseSkillList(String plistOriginal) {
		ArrayList<String> result = new ArrayList<>();
		if (plistOriginal == null || plistOriginal.isEmpty())
			return result;
		for (String item : plistOriginal.split(",")) {
			String skill = item == null ? "" : item.trim();
			if (skill.startsWith("."))
				skill = skill.substring(1);
			if (!skill.isEmpty())
				result.add(skill);
		}
		return result;
	}

	private static String writeSkillList(List<String> skills) {
		if (skills == null || skills.isEmpty())
			return ".";
		StringBuilder builder = new StringBuilder(".");
		for (String skill : skills) {
			if (skill != null && !skill.isEmpty())
				builder.append(skill).append(",");
		}
		return builder.toString();
	}

	private static String formationIdFromSkill(String skill) {
		if (!isFormationSkill(skill))
			return "";
		String value = skill.substring(FORMATION_PREFIX.length());
		int split = value.indexOf('|');
		return split >= 0 ? value.substring(0, split) : value;
	}

	private static String formationNameFromSkill(String skill) {
		if (!isFormationSkill(skill))
			return "";
		int split = skill.indexOf('|');
		return split >= 0 && split + 1 < skill.length() ? skill.substring(split + 1) : "";
	}

	private static int xpNeeded(int level) {
		return 35 + level * 15;
	}

	private static double healthGain(String type) {
		return switch (type) {
			case "igris", "beru", "kamish", "tusk", "kaisel" -> 8.0;
			case "high_orc", "polar_bear" -> 5.0;
			case "knight", "orc", "wolf" -> 3.0;
			default -> 2.0;
		};
	}

	private static double attackGain(String type) {
		return switch (type) {
			case "igris", "beru", "kamish", "tusk", "kaisel" -> 1.25;
			case "high_orc", "polar_bear" -> 0.85;
			case "knight", "orc", "wolf" -> 0.55;
			default -> 0.4;
		};
	}

	private static String[] shadowTypes() {
		return new String[]{"goblin_club", "goblin_archer", "goblin_mage", "wolf", "knight", "polar_bear", "orc", "igris", "beru", "kamish", "high_orc", "tusk", "kaisel"};
	}

	private static EntityType<?> entityType(String type) {
		return switch (type) {
			case "goblin_club" -> SololevelingModEntities.GOBLIN_CLUB_SHADOW.get();
			case "goblin_archer" -> SololevelingModEntities.GOBLIN_ARCHER_SHADOW.get();
			case "goblin_mage" -> SololevelingModEntities.GOBLIN_MAGE_SHADOW.get();
			case "wolf" -> SololevelingModEntities.STEEL_FANG_WOLF_SHADOW.get();
			case "knight" -> SololevelingModEntities.SHADOW_SOLD_1.get();
			case "polar_bear" -> SololevelingModEntities.SHADOW_POLAR_BEAR.get();
			case "orc" -> SololevelingModEntities.SHADOW_GREEN_ORC.get();
			case "igris" -> SololevelingModEntities.IGRIS_SHADOW.get();
			case "beru" -> SololevelingModEntities.BERU_SHADOW.get();
			case "kamish" -> SololevelingModEntities.KAMISH_SHADOW.get();
			case "high_orc" -> SololevelingModEntities.SHADOW_HIGH_ORC.get();
			case "tusk" -> SololevelingModEntities.TUSK_SHADOW.get();
			case "kaisel" -> SololevelingModEntities.SHADOW_KAISELIN.get();
			default -> null;
		};
	}

	private static String typeFromEntity(Entity entity) {
		if (entity instanceof GoblinClubShadowEntity)
			return "goblin_club";
		if (entity instanceof GoblinArcherShadowEntity)
			return "goblin_archer";
		if (entity instanceof GoblinMageShadowEntity)
			return "goblin_mage";
		if (entity instanceof SteelFangWolfShadowEntity)
			return "wolf";
		if (entity instanceof ShadowSold1Entity)
			return "knight";
		if (entity instanceof ShadowPolarBearEntity)
			return "polar_bear";
		if (entity instanceof ShadowGreenOrcEntity)
			return "orc";
		if (entity instanceof IgrisShadowEntity)
			return "igris";
		if (entity instanceof BeruShadowEntity)
			return "beru";
		if (entity instanceof KamishShadowEntity)
			return "kamish";
		if (entity instanceof ShadowHighOrcEntity)
			return "high_orc";
		if (entity instanceof TuskShadowEntity)
			return "tusk";
		if (entity instanceof ShadowKaiselinEntity)
			return "kaisel";
		return "";
	}

	private static String normalizeShadowType(String type) {
		String value = type.trim().toLowerCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
		return switch (value) {
			case "goblin", "goblin_fighter", "goblin_club" -> "goblin_club";
			case "archer", "goblin_archer" -> "goblin_archer";
			case "mage", "goblin_mage" -> "goblin_mage";
			case "lycan", "wolf" -> "wolf";
			case "soldier", "knight" -> "knight";
			case "bear", "polar", "polar_bear" -> "polar_bear";
			case "orc", "green_orc" -> "orc";
			case "highorc", "high_orc" -> "high_orc";
			case "igris" -> "igris";
			case "beru" -> "beru";
			case "kamish" -> "kamish";
			case "tusk" -> "tusk";
			case "kaisel", "kaiselin", "shadow_kaisel", "shadow_kaiselin" -> "kaisel";
			default -> "";
		};
	}

	private static String defaultName(String type, int number) {
		return switch (type) {
			case "goblin_club" -> "Goblin Fighter " + number;
			case "goblin_archer" -> "Goblin Archer " + number;
			case "goblin_mage" -> "Goblin Mage " + number;
			case "wolf" -> "Lycan " + number;
			case "knight" -> "Knight " + number;
			case "polar_bear" -> "Polar Bear " + number;
			case "orc" -> "Orc " + number;
			case "igris" -> "Igris";
			case "beru" -> "Beru";
			case "kamish" -> "Kamish";
			case "high_orc" -> "High Orc " + number;
			case "tusk" -> "Tusk";
			case "kaisel" -> "Kaisel";
			default -> type.replace('_', ' ').toLowerCase(Locale.ROOT);
		};
	}

	private static boolean isBoss(String type) {
		return "igris".equals(type) || "beru".equals(type) || "kamish".equals(type) || "tusk".equals(type) || "kaisel".equals(type);
	}

	private static boolean isDismissibleShadowType(String type) {
		return "goblin_club".equals(type) || "goblin_archer".equals(type) || "goblin_mage".equals(type) || "wolf".equals(type) || "knight".equals(type) || "polar_bear".equals(type) || "orc".equals(type) || "high_orc".equals(type);
	}

	private static int legacyMax(Player player, String type) {
		SololevelingModVariables.PlayerVariables vars = player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables());
		return switch (type) {
			case "goblin_club" -> (int) vars.GobShadowMax;
			case "goblin_archer" -> (int) vars.ShadowGoblinArcherMax;
			case "goblin_mage" -> (int) vars.ShadowGoblinMageMax;
			case "wolf" -> (int) vars.WolfShadowMax;
			case "knight" -> (int) vars.ordshadowmax;
			case "polar_bear" -> (int) vars.polarbearmax;
			case "orc" -> (int) vars.orcmax;
			case "igris" -> (int) vars.igris;
			case "beru" -> (int) vars.berumax;
			case "kamish" -> (int) vars.shadowdragonmax;
			case "high_orc" -> (int) vars.highorcmax;
			case "tusk" -> (int) vars.tuskmax;
			case "kaisel" -> (int) vars.Kaisel;
			default -> 0;
		};
	}

	private static int legacySpawned(Player player, String type) {
		SololevelingModVariables.PlayerVariables vars = player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables());
		return switch (type) {
			case "goblin_club" -> (int) vars.GobShadow;
			case "goblin_archer" -> (int) vars.ShadowGoblinArcherAmount;
			case "goblin_mage" -> (int) vars.ShadowGoblinMageAmount;
			case "wolf" -> (int) vars.WolfShadow;
			case "knight" -> (int) vars.OrdShadow;
			case "polar_bear" -> (int) vars.polarbear;
			case "orc" -> (int) vars.orcspawned;
			case "igris" -> (int) vars.IgrisSpawned;
			case "beru" -> (int) vars.beru;
			case "kamish" -> (int) vars.shadowdragonnum;
			case "high_orc" -> (int) vars.highorcspawned;
			case "tusk" -> (int) vars.tuskspawned;
			case "kaisel" -> (int) vars.KaiselSpawned;
			default -> 0;
		};
	}

	private static void setLegacyMax(Player player, String type, int amount) {
		player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
			switch (type) {
				case "goblin_club" -> capability.GobShadowMax = amount;
				case "goblin_archer" -> capability.ShadowGoblinArcherMax = amount;
				case "goblin_mage" -> capability.ShadowGoblinMageMax = amount;
				case "wolf" -> capability.WolfShadowMax = amount;
				case "knight" -> capability.ordshadowmax = amount;
				case "polar_bear" -> capability.polarbearmax = amount;
				case "orc" -> capability.orcmax = amount;
				case "igris" -> capability.igris = amount;
				case "beru" -> capability.berumax = amount;
				case "kamish" -> capability.shadowdragonmax = amount;
				case "high_orc" -> capability.highorcmax = amount;
				case "tusk" -> capability.tuskmax = amount;
				case "kaisel" -> capability.Kaisel = amount;
				default -> {
				}
			}
		});
	}

	private static void trimOwnedShadows(Player player, String type, int amount) {
		ListTag shadows = shadows(player);
		while (countOwned(player, type) > amount) {
			int removeIndex = weakestShadowIndex(shadows, type);
			if (removeIndex < 0)
				return;
			CompoundTag shadow = shadows.getCompound(removeIndex);
			if (shadow.hasUUID("summoned") && player.level() instanceof ServerLevel level) {
				Entity summoned = findSummonedEntity(player, shadow.getUUID("summoned"));
				if (summoned != null) {
					dropStoredShadowInventory(summoned);
					summoned.discard();
					updateLegacySpawnCounter(player, type, -1);
				}
			}
			shadows.remove(removeIndex);
		}
	}

	private static int weakestShadowIndex(ListTag shadows, String type) {
		int weakestIndex = -1;
		CompoundTag weakest = null;
		for (int i = 0; i < shadows.size(); i++) {
			CompoundTag shadow = shadows.getCompound(i);
			if (!type.equals(shadow.getString("type")))
				continue;
			if (isWeakerShadow(shadow, weakest)) {
				weakest = shadow;
				weakestIndex = i;
			}
		}
		return weakestIndex;
	}

	private static boolean isWeakerShadow(CompoundTag candidate, CompoundTag current) {
		if (current == null)
			return true;
		int candidateLevel = Math.max(1, candidate.getInt("level"));
		int currentLevel = Math.max(1, current.getInt("level"));
		if (candidateLevel != currentLevel)
			return candidateLevel < currentLevel;
		int candidateXp = candidate.getInt("xp");
		int currentXp = current.getInt("xp");
		if (candidateXp != currentXp)
			return candidateXp < currentXp;
		return candidate.getString("id").compareTo(current.getString("id")) > 0;
	}

	private static void updateLegacySpawnCounter(Player player, String type, int amount) {
		player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
			switch (type) {
				case "goblin_club" -> capability.GobShadow = Math.max(0, capability.GobShadow + amount);
				case "goblin_archer" -> capability.ShadowGoblinArcherAmount = Math.max(0, capability.ShadowGoblinArcherAmount + amount);
				case "goblin_mage" -> capability.ShadowGoblinMageAmount = Math.max(0, capability.ShadowGoblinMageAmount + amount);
				case "wolf" -> capability.WolfShadow = Math.max(0, capability.WolfShadow + amount);
				case "knight" -> capability.OrdShadow = Math.max(0, capability.OrdShadow + amount);
				case "polar_bear" -> capability.polarbear = Math.max(0, capability.polarbear + amount);
				case "orc" -> capability.orcspawned = Math.max(0, capability.orcspawned + amount);
				case "igris" -> capability.IgrisSpawned = Math.max(0, capability.IgrisSpawned + amount);
				case "beru" -> capability.beru = Math.max(0, capability.beru + amount);
				case "kamish" -> capability.shadowdragonnum = Math.max(0, capability.shadowdragonnum + amount);
				case "high_orc" -> capability.highorcspawned = Math.max(0, capability.highorcspawned + amount);
				case "tusk" -> capability.tuskspawned = Math.max(0, capability.tuskspawned + amount);
				case "kaisel" -> capability.KaiselSpawned = Math.max(0, capability.KaiselSpawned + amount);
				default -> {
				}
			}
			capability.syncPlayerVariables(player);
		});
	}
}
