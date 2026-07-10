package net.solocraft.procedures;

import net.solocraft.dungeon.ProceduralDungeonRank;
import net.solocraft.entity.Portal1Entity;
import net.solocraft.entity.PortalAncientGolemEntity;
import net.solocraft.entity.PortalBeruEntity;
import net.solocraft.entity.PortalCemeteryEntity;
import net.solocraft.entity.PortalKargalgansThroneRoomEntity;
import net.solocraft.entity.PortalLabEntity;
import net.solocraft.entity.PortalLushEntity;
import net.solocraft.entity.PortalSewersEntity;
import net.solocraft.entity.RandomCaveLargeEntity;
import net.solocraft.entity.RedGateEntity;
import net.solocraft.network.SololevelingModVariables;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;

import java.util.LinkedHashMap;
import java.util.Map;

public class GateBreakChanceProcedure {
	private static final int MAX_MEMORY_STACKS = 4;
	private static final double MEMORY_PENALTY_PER_STACK = 0.15D;

	private GateBreakChanceProcedure() {
	}

	public static boolean shouldBreak(LevelAccessor world, Entity gate) {
		if (!(world instanceof ServerLevel serverLevel) || gate == null)
			return true;
		int gateRank = gateRank(gate);
		int maxPlayerRank = maxPlayerRank(serverLevel);
		if (gateRank <= maxPlayerRank)
			return true;
		String key = gateKey(gate);
		SololevelingModVariables.MapVariables mapVars = SololevelingModVariables.MapVariables.get(world);
		Map<String, Integer> memory = readMemory(mapVars.GateBreakMemory);
		int stacks = memory.getOrDefault(key, 0);
		double chance = Math.max(0.05D, baseBreakChance(gateRank - maxPlayerRank) - stacks * MEMORY_PENALTY_PER_STACK);
		boolean breaks = serverLevel.random.nextDouble() < chance;
		if (breaks) {
			memory.put(key, Math.min(MAX_MEMORY_STACKS, stacks + 1));
		} else if (stacks > 0) {
			memory.put(key, stacks - 1);
		}
		mapVars.GateBreakMemory = writeMemory(memory);
		mapVars.syncData(world);
		return breaks;
	}

	private static int maxPlayerRank(ServerLevel level) {
		int maxRank = 0;
		for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
			SololevelingModVariables.PlayerVariables vars = player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables());
			maxRank = Math.max(maxRank, (int) vars.HunterRank);
		}
		return maxRank;
	}

	private static double baseBreakChance(int rankGap) {
		if (rankGap <= 1)
			return 0.65D;
		if (rankGap == 2)
			return 0.45D;
		if (rankGap == 3)
			return 0.30D;
		return 0.18D;
	}

	private static int gateRank(Entity gate) {
		if (gate instanceof Portal1Entity)
			return proceduralRank(gate).numericRank;
		if (gate instanceof PortalSewersEntity)
			return ProceduralDungeonRank.D.numericRank;
		if (gate instanceof RandomCaveLargeEntity || gate instanceof PortalAncientGolemEntity)
			return ProceduralDungeonRank.C.numericRank;
		if (gate instanceof PortalLushEntity || gate instanceof PortalCemeteryEntity || gate instanceof RedGateEntity)
			return ProceduralDungeonRank.B.numericRank;
		if (gate instanceof PortalLabEntity || gate instanceof PortalKargalgansThroneRoomEntity)
			return ProceduralDungeonRank.A.numericRank;
		if (gate instanceof PortalBeruEntity)
			return ProceduralDungeonRank.S.numericRank;
		return ProceduralDungeonRank.E.numericRank;
	}

	private static String gateKey(Entity gate) {
		if (gate instanceof Portal1Entity)
			return "procedural_" + proceduralRank(gate).name().toLowerCase();
		if (gate instanceof PortalSewersEntity)
			return "sewers";
		if (gate instanceof RandomCaveLargeEntity)
			return "random_cave_large";
		if (gate instanceof PortalAncientGolemEntity)
			return "ancient_golem";
		if (gate instanceof PortalLushEntity)
			return "lush";
		if (gate instanceof PortalCemeteryEntity)
			return "cemetery";
		if (gate instanceof RedGateEntity)
			return "red_gate";
		if (gate instanceof PortalLabEntity)
			return "lab";
		if (gate instanceof PortalKargalgansThroneRoomEntity)
			return "kargalgan";
		if (gate instanceof PortalBeruEntity)
			return "beru";
		return gate.getType().toString().replace(';', '_').replace('=', '_');
	}

	private static ProceduralDungeonRank proceduralRank(Entity gate) {
		return ProceduralDungeonRank.fromString(gate.getPersistentData().getString("slr_procedural_rank"));
	}

	private static Map<String, Integer> readMemory(String saved) {
		Map<String, Integer> memory = new LinkedHashMap<>();
		if (saved == null || saved.isBlank())
			return memory;
		for (String entry : saved.split(";")) {
			int separator = entry.indexOf('=');
			if (separator <= 0 || separator >= entry.length() - 1)
				continue;
			try {
				int stacks = Integer.parseInt(entry.substring(separator + 1));
				if (stacks > 0)
					memory.put(entry.substring(0, separator), Math.min(MAX_MEMORY_STACKS, stacks));
			} catch (NumberFormatException ignored) {
			}
		}
		return memory;
	}

	private static String writeMemory(Map<String, Integer> memory) {
		StringBuilder saved = new StringBuilder();
		for (Map.Entry<String, Integer> entry : memory.entrySet()) {
			int stacks = Math.min(MAX_MEMORY_STACKS, Math.max(0, entry.getValue()));
			if (stacks <= 0)
				continue;
			if (saved.length() > 0)
				saved.append(';');
			saved.append(entry.getKey()).append('=').append(stacks);
		}
		return saved.toString();
	}
}
