"""Generate the shared Demon King's Castle realm and its floor biome profiles."""

from __future__ import annotations

import json
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
DATA = ROOT / "src/main/resources/data/sololeveling"

# Fog, sky and water palettes deliberately progress from ember dusk to Baran's
# violet storm. Values are RGB hex and are emitted as Minecraft decimal colors.
PALETTES = [
	(0x5A1515, 0xB62D20, 0x2A0909), (0x642019, 0xC74625, 0x2B0D0A),
	(0x4C201A, 0x9B4E2D, 0x21100C), (0x54251C, 0xB45A32, 0x28110C),
	(0x6A2110, 0xE05A19, 0x321007), (0x73260E, 0xF06A17, 0x3A1205),
	(0x591419, 0xD52E31, 0x2B080D), (0x4B1016, 0xB9202D, 0x22070B),
	(0x6E1808, 0xF2470D, 0x350B04), (0x7A2005, 0xFF6A08, 0x3B0D02),
	(0x291421, 0x71345D, 0x110910), (0x24142C, 0x673C78, 0x0E0914),
	(0x20152F, 0x594283, 0x0C0A17), (0x29143A, 0x6F3B94, 0x10091D),
	(0x321344, 0x8738A8, 0x140821), (0x171C3D, 0x465BB1, 0x090D21),
	(0x131E4A, 0x3E6BD2, 0x07102A), (0x22194F, 0x6755D8, 0x0D0B2D),
	(0x351653, 0x9B45D5, 0x16082F), (0x46105D, 0xCA35E8, 0x1F062F),
]

def floor_suffix(floor: int) -> str:
	return "" if floor == 1 else f"_f{floor:02d}"


def dimension_name(floor: int) -> str:
	return f"dungeon_dimension_dkc{floor_suffix(floor)}"


def biome_name(floor: int) -> str:
	return f"dungeon_biome_dkc{floor_suffix(floor)}"


def write_json(path: Path, value: object) -> None:
	path.parent.mkdir(parents=True, exist_ok=True)
	path.write_text(json.dumps(value, indent=2) + "\n", encoding="utf-8")


def dimension() -> dict:
	# A 32-block solid substrate is enough for authored foundations and costs a
	# fraction of cave/noise generation. Floor-specific authored structures sit
	# in distant cells inside this one realm; visuals come from the client floor
	# profile instead of extra LevelStems.
	return {
		"type": "sololeveling:dungeon_dimension_dkc",
		"generator": {
			"type": "minecraft:flat",
			"settings": {
				"biome": "sololeveling:dungeon_biome_dkc",
				"lakes": False,
				"features": False,
				"layers": [
					{"height": 1, "block": "minecraft:bedrock"},
					{"height": 23, "block": "minecraft:deepslate"},
					{"height": 5, "block": "minecraft:basalt"},
					{"height": 2, "block": "minecraft:blackstone"},
					{"height": 1, "block": "minecraft:cracked_polished_blackstone_bricks"},
				],
				"structure_overrides": [],
			},
		},
	}


def dimension_type() -> dict:
	return {
		"ultrawarm": False,
		"natural": False,
		"piglin_safe": True,
		"respawn_anchor_works": False,
		"bed_works": False,
		"has_raids": False,
		"has_skylight": True,
		"has_ceiling": False,
		"coordinate_scale": 1.0,
		"ambient_light": 0.11,
		"fixed_time": 18000,
		"logical_height": 384,
		"infiniburn": "#minecraft:infiniburn_nether",
		"min_y": 0,
		"height": 384,
		"monster_spawn_light_level": 0,
		"monster_spawn_block_light_limit": 0,
		"effects": "sololeveling:dungeon_dimension_dkc",
	}


def biome(floor: int) -> dict:
	fog, sky, water_fog = PALETTES[floor - 1]
	return {
		"has_precipitation": False,
		"temperature": 2.0,
		"downfall": 0.0,
		"effects": {
			"fog_color": fog,
			"sky_color": sky,
			"water_color": 0x45120D if floor <= 10 else 0x24133D,
			"water_fog_color": water_fog,
			"foliage_color": 0x382019,
			"grass_color": 0x30201B,
			"mood_sound": {
				"sound": "minecraft:ambient.basalt_deltas.mood",
				"tick_delay": 6000,
				"block_search_extent": 8,
				"offset": 2.0,
			},
		},
		"spawners": {
			"monster": [], "creature": [], "ambient": [], "axolotls": [],
			"underground_water_creature": [], "water_creature": [],
			"water_ambient": [], "misc": [],
		},
		"spawn_costs": {},
		"carvers": {},
		"features": [[] for _ in range(11)],
	}


def main() -> None:
	write_json(DATA / "dimension" / "dungeon_dimension_dkc.json", dimension())
	write_json(DATA / "dimension_type" / "dungeon_dimension_dkc.json", dimension_type())
	for floor in range(1, 21):
		write_json(DATA / "worldgen" / "biome" / f"{biome_name(floor)}.json", biome(floor))
		if floor > 1:
			(DATA / "dimension" / f"{dimension_name(floor)}.json").unlink(missing_ok=True)
			# Old level.dat WorldGenSettings retain references to these type and
			# biome IDs even after their LevelStems are retired. Keeping lightweight
			# codec stubs lets those saves decode without registering the old worlds.
			write_json(DATA / "dimension_type" / f"{dimension_name(floor)}.json", dimension_type())
	print("Generated one shared DKC realm, 19 legacy type stubs, and 20 legacy-safe biome profiles.")


if __name__ == "__main__":
	main()
