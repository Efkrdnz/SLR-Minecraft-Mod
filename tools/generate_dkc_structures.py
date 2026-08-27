"""Generate the authored DKC rework structure modules.

The source orientation is always south (z=0) to north. Every playable surface
has a three-block foundation and every connector keeps a flat threshold.
"""

from __future__ import annotations

from dataclasses import dataclass
import gzip
import math
from pathlib import Path
import struct
import time


ROOT = Path(__file__).resolve().parents[1]
OUTPUT = ROOT / "src/main/resources/data/sololeveling/structure"
DATA_VERSION = 3465


@dataclass(frozen=True, order=True)
class State:
	name: str
	properties: tuple[tuple[str, str], ...] = ()


def state(name: str, **properties: str) -> State:
	return State(name, tuple(sorted(properties.items())))


AIR = state("minecraft:air")
BEDROCK = state("minecraft:bedrock")
DEEPSLATE = state("minecraft:deepslate")
DEEPSLATE_TILES = state("minecraft:deepslate_tiles")
CRACKED_TILES = state("minecraft:cracked_deepslate_tiles")
DEEPSLATE_BRICKS = state("minecraft:deepslate_bricks")
CRACKED_BRICKS = state("minecraft:cracked_deepslate_bricks")
POLISHED_BLACKSTONE = state("minecraft:polished_blackstone")
POLISHED_BLACKSTONE_BRICKS = state("minecraft:polished_blackstone_bricks")
CRACKED_BLACKSTONE_BRICKS = state("minecraft:cracked_polished_blackstone_bricks")
GILDED_BLACKSTONE = state("minecraft:gilded_blackstone")
BLACKSTONE = state("minecraft:blackstone")
NETHERRACK = state("minecraft:netherrack")
RED_NETHER_BRICKS = state("minecraft:red_nether_bricks")
NETHER_BRICKS = state("minecraft:nether_bricks")
BASALT = state("minecraft:basalt", axis="y")
POLISHED_BASALT = state("minecraft:polished_basalt", axis="y")
MAGMA = state("minecraft:magma_block")
SHROOMLIGHT = state("minecraft:shroomlight")
GLOWSTONE = state("minecraft:glowstone")
CRYING_OBSIDIAN = state("minecraft:crying_obsidian")
OBSIDIAN = state("minecraft:obsidian")
IRON_BARS = state("minecraft:iron_bars")
CHAIN = state("minecraft:chain", axis="y", waterlogged="false")
GOLD = state("minecraft:gold_block")
CUT_COPPER = state("minecraft:cut_copper")
EXPOSED_CUT_COPPER = state("minecraft:exposed_cut_copper")
BLACK_CONCRETE = state("minecraft:black_concrete")
GRAY_CONCRETE = state("minecraft:gray_concrete")
RED_CONCRETE = state("minecraft:red_concrete")
RED_GLASS = state("minecraft:red_stained_glass")
ORANGE_GLASS = state("minecraft:orange_stained_glass")
WHITE_GLASS = state("minecraft:white_stained_glass")
DARK_OAK = state("minecraft:dark_oak_planks")
STRIPPED_DARK_OAK = state("minecraft:stripped_dark_oak_log", axis="y")
FIRE = state("minecraft:fire", age="0", east="false", north="false", south="false", up="false", west="false")
SOUL_FIRE = state("minecraft:soul_fire")
STRUCTURE_DATA = state("minecraft:structure_block", mode="data")
CONNECTOR_DIMENSIONS = {
	"main": (11, 9),
	"side": (7, 7),
	"boss": (15, 14),
}


class Structure:
	def __init__(self, name: str, width: int, length: int, height: int):
		self.name = name
		self.width = width
		self.length = length
		self.height = height
		self.blocks: dict[tuple[int, int, int], State] = {}
		self.markers: dict[tuple[int, int, int], str] = {}

	def set(self, x: int, y: int, z: int, block: State) -> None:
		if 0 <= x < self.width and 0 <= y < self.height and 0 <= z < self.length:
			self.blocks[(x, y, z)] = block
			self.markers.pop((x, y, z), None)

	def fill(self, x1: int, y1: int, z1: int, x2: int, y2: int, z2: int, block: State) -> None:
		for y in range(max(0, y1), min(self.height - 1, y2) + 1):
			for z in range(max(0, z1), min(self.length - 1, z2) + 1):
				for x in range(max(0, x1), min(self.width - 1, x2) + 1):
					self.set(x, y, z, block)

	def marker(self, x: int, y: int, z: int, marker_name: str) -> None:
		if marker_name.startswith("connector_") and self.name != "dkc_tower_gate_closed":
			contract = marker_name.split("_", 2)[1]
			width, height = CONNECTOR_DIMENSIONS[contract]
			if z <= 2:
				self.carve("south", width, height, 3)
			elif z >= self.length - 3:
				self.carve("north", width, height, 3)
			elif x <= 2:
				self.carve("west", width, height, 3)
			elif x >= self.width - 3:
				self.carve("east", width, height, 3)
			else:
				raise ValueError(f"{self.name}: connector {marker_name} is not on an edge")
		self.set(x, y, z, STRUCTURE_DATA)
		self.markers[(x, y, z)] = marker_name

	def carve(self, side: str, width: int, height: int, depth: int = 2) -> None:
		if side in ("south", "north"):
			x1 = self.width // 2 - width // 2
			z1 = 0 if side == "south" else self.length - depth
			self.fill(x1, 3, z1, x1 + width - 1, 3 + height - 1, z1 + depth - 1, AIR)
		else:
			z1 = self.length // 2 - width // 2
			x1 = 0 if side == "west" else self.width - depth
			self.fill(x1, 3, z1, x1 + depth - 1, 3 + height - 1, z1 + width - 1, AIR)

	def slice(self, name: str, x0: int, z0: int, width: int, length: int) -> "Structure":
		result = Structure(name, width, length, self.height)
		for (x, y, z), block in self.blocks.items():
			if x0 <= x < x0 + width and z0 <= z < z0 + length:
				result.blocks[(x - x0, y, z - z0)] = block
		for (x, y, z), marker_name in self.markers.items():
			if x0 <= x < x0 + width and z0 <= z < z0 + length:
				result.markers[(x - x0, y, z - z0)] = marker_name
		return result


def _utf(value: str) -> bytes:
	encoded = value.encode("utf-8")
	return struct.pack(">H", len(encoded)) + encoded


def _named_header(tag_type: int, name: str) -> bytes:
	return bytes((tag_type,)) + _utf(name)


def _named_string(name: str, value: str) -> bytes:
	return _named_header(8, name) + _utf(value)


def _named_int(name: str, value: int) -> bytes:
	return _named_header(3, name) + struct.pack(">i", value)


def _named_long(name: str, value: int) -> bytes:
	return _named_header(4, name) + struct.pack(">q", value)


def _named_float(name: str, value: float) -> bytes:
	return _named_header(5, name) + struct.pack(">f", value)


def _named_byte(name: str, value: int) -> bytes:
	return _named_header(1, name) + struct.pack(">b", value)


def _marker_payload(marker_name: str) -> bytes:
	result = bytearray()
	result += _named_string("id", "minecraft:structure_block")
	result += _named_string("name", "")
	result += _named_string("author", "SoloCraft")
	result += _named_string("metadata", marker_name)
	result += _named_string("mode", "DATA")
	result += _named_string("mirror", "NONE")
	result += _named_string("rotation", "NONE")
	result += _named_int("posX", 0) + _named_int("posY", 1) + _named_int("posZ", 0)
	result += _named_int("sizeX", 0) + _named_int("sizeY", 0) + _named_int("sizeZ", 0)
	result += _named_float("integrity", 1.0) + _named_long("seed", 0)
	result += _named_byte("ignoreEntities", 1) + _named_byte("powered", 0)
	result += _named_byte("showair", 0) + _named_byte("showboundingbox", 1)
	result += b"\x00"
	return bytes(result)


def save_structure(structure: Structure) -> None:
	states = sorted({AIR, *structure.blocks.values()})
	state_ids = {block: index for index, block in enumerate(states)}
	buf = bytearray(b"\x0a\x00\x00")

	buf += _named_header(9, "size") + b"\x03" + struct.pack(">i", 3)
	buf += struct.pack(">iii", structure.width, structure.height, structure.length)
	buf += _named_header(9, "entities") + b"\x0a" + struct.pack(">i", 0)

	# Structure templates are sparse: positions that were never authored do not
	# need an AIR record.  The old writer expanded every module to its complete
	# bounding volume, which made the tower place roughly 983k mostly-empty
	# records.  Explicit AIR written by carve() remains in the template, so an
	# open gate can still replace the matching closed-gate blocks safely.
	entries = sorted(structure.blocks.items(), key=lambda item: (item[0][1], item[0][2], item[0][0]))
	buf += _named_header(9, "blocks") + b"\x0a" + struct.pack(">i", len(entries))
	for (x, y, z), block in entries:
		pos = (x, y, z)
		buf += _named_header(9, "pos") + b"\x03" + struct.pack(">i", 3)
		buf += struct.pack(">iii", x, y, z)
		buf += _named_int("state", state_ids[block])
		marker_name = structure.markers.get(pos)
		if marker_name is not None:
			buf += _named_header(10, "nbt") + _marker_payload(marker_name)
		buf += b"\x00"

	buf += _named_header(9, "palette") + b"\x0a" + struct.pack(">i", len(states))
	for block in states:
		buf += _named_string("Name", block.name)
		if block.properties:
			buf += _named_header(10, "Properties")
			for key, value in block.properties:
				buf += _named_string(key, value)
			buf += b"\x00"
		buf += b"\x00"
	buf += _named_int("DataVersion", DATA_VERSION)
	buf += b"\x00"

	OUTPUT.mkdir(parents=True, exist_ok=True)
	target = OUTPUT / f"{structure.name}.nbt"
	# Gradle's resource scanner and Windows antivirus can briefly hold an NBT
	# path while a full deterministic regeneration is replacing all templates.
	# Bounded retries keep that harmless sharing race from aborting the generator.
	for attempt in range(6):
		try:
			with gzip.GzipFile(target, "wb", compresslevel=9, mtime=0) as out:
				out.write(buf)
			return
		except OSError:
			if attempt == 5:
				raise
			time.sleep(0.05 * (attempt + 1))


def foundation(s: Structure, floor: State = DEEPSLATE_TILES) -> None:
	s.fill(0, 0, 0, s.width - 1, 0, s.length - 1, BEDROCK)
	s.fill(0, 1, 0, s.width - 1, 1, s.length - 1, DEEPSLATE)
	s.fill(0, 2, 0, s.width - 1, 2, s.length - 1, floor)


def patterned_floor(s: Structure, primary: State, secondary: State, spacing: int = 8) -> None:
	for z in range(s.length):
		for x in range(s.width):
			if x % spacing == 0 or z % spacing == 0:
				s.set(x, 2, z, secondary)
			else:
				s.set(x, 2, z, primary)


def perimeter(s: Structure, height: int, block: State, thickness: int = 2) -> None:
	s.fill(0, 3, 0, s.width - 1, height, thickness - 1, block)
	s.fill(0, 3, s.length - thickness, s.width - 1, height, s.length - 1, block)
	s.fill(0, 3, 0, thickness - 1, height, s.length - 1, block)
	s.fill(s.width - thickness, 3, 0, s.width - 1, height, s.length - 1, block)


def pillar(s: Structure, x: int, z: int, height: int, body: State = POLISHED_BLACKSTONE_BRICKS, accent: State = GILDED_BLACKSTONE) -> None:
	s.fill(x - 1, 3, z - 1, x + 1, 4, z + 1, accent)
	s.fill(x, 5, z, x, height - 1, z, body)
	s.fill(x - 1, height, z - 1, x + 1, height, z + 1, accent)


def brazier(s: Structure, x: int, z: int) -> None:
	s.set(x, 3, z, POLISHED_BLACKSTONE_BRICKS)
	s.set(x, 4, z, NETHERRACK)
	s.set(x, 5, z, state("minecraft:fire"))


def integrated_lights(s: Structure, positions: list[tuple[int, int, int]]) -> None:
	for x, y, z in positions:
		s.set(x, y, z, SHROOMLIGHT)
		if y + 1 < s.height:
			s.set(x, y + 1, z, IRON_BARS)


def make_arrival_plaza() -> Structure:
	s = Structure("dkc_f1_arrival_plaza", 48, 48, 24)
	foundation(s)
	patterned_floor(s, DEEPSLATE_TILES, CRACKED_TILES, 8)
	perimeter(s, 9, DEEPSLATE_BRICKS, 2)
	s.carve("north", 11, 9)
	# A centered ceremonial road and a broken facade frame the tower view.
	s.fill(18, 2, 7, 29, 2, 47, POLISHED_BLACKSTONE)
	for z in range(8, 48, 6):
		s.fill(23, 2, z, 24, 2, z + 1, GILDED_BLACKSTONE)
	for x, z, h in ((5, 5, 18), (42, 5, 15), (5, 40, 13), (42, 40, 17)):
		s.fill(x - 3, 3, z - 3, x + 3, h, z + 3, DEEPSLATE)
		s.fill(x - 2, 4, z - 2, x + 2, h - 2, z + 2, BLACK_CONCRETE)
		pillar(s, x, z, min(h, 13))
	# Return shrine behind the arrival point.
	s.fill(19, 3, 2, 28, 3, 6, POLISHED_BLACKSTONE_BRICKS)
	s.fill(20, 4, 3, 21, 11, 4, CRYING_OBSIDIAN)
	s.fill(26, 4, 3, 27, 11, 4, CRYING_OBSIDIAN)
	s.fill(20, 11, 3, 27, 12, 4, GILDED_BLACKSTONE)
	s.fill(22, 5, 4, 25, 10, 4, BLACK_CONCRETE)
	for x in (4, 43):
		for z in (14, 28):
			brazier(s, x, z)
	integrated_lights(s, [(1, 7, 12), (46, 7, 12), (1, 7, 34), (46, 7, 34)])
	s.marker(24, 3, 14, "player_start")
	s.marker(21, 3, 14, "safe_anchor")
	s.marker(24, 4, 5, "return_sigil")
	s.marker(24, 3, 47, "connector_main_out")
	return s


def make_approach(variant: str) -> Structure:
	s = Structure(f"dkc_f1_approach_{variant}", 32, 24, 18)
	foundation(s)
	patterned_floor(s, DEEPSLATE_TILES, POLISHED_BLACKSTONE, 6)
	perimeter(s, 9, DEEPSLATE_BRICKS, 2)
	s.carve("south", 11, 9)
	s.carve("north", 15, 14)
	s.fill(10, 2, 0, 21, 2, 23, POLISHED_BLACKSTONE)
	for z in (5, 12, 19):
		s.fill(2, 3, z - 2, 7, 9 + (z % 3), z + 2, DEEPSLATE)
		s.fill(24, 3, z - 2, 29, 8 + ((z + 1) % 4), z + 2, NETHER_BRICKS)
	if variant == "a":
		s.fill(4, 3, 8, 8, 5, 11, CRACKED_BRICKS)
		s.fill(24, 3, 15, 28, 6, 18, RED_NETHER_BRICKS)
	else:
		for z in (7, 16):
			s.fill(5, 3, z, 8, 4, z + 2, BASALT)
			s.fill(23, 3, z + 1, 26, 5, z + 3, CRACKED_TILES)
	for x in (4, 27):
		brazier(s, x, 6)
		brazier(s, x, 18)
	s.marker(16, 3, 0, "connector_main_in")
	s.marker(16, 3, 23, "connector_boss_out")
	return s


def make_cerberus_courtyard() -> Structure:
	s = Structure("dkc_f1_cerberus_courtyard", 80, 80, 24)
	foundation(s)
	patterned_floor(s, DEEPSLATE_TILES, CRACKED_TILES, 10)
	perimeter(s, 13, DEEPSLATE_BRICKS, 3)
	s.carve("south", 15, 14, 3)
	s.carve("north", 15, 14, 3)
	# Recessed heat channels stay outside the clean 56 x 56 charge arena.
	for x in range(7, 73):
		for z in (7, 8, 71, 72):
			s.set(x, 2, z, MAGMA if x % 3 else ORANGE_GLASS)
	for z in range(9, 71):
		for x in (7, 8, 71, 72):
			s.set(x, 2, z, MAGMA if z % 3 else ORANGE_GLASS)
	# Broken restraint pylons and hanging chains remain outside charge lanes.
	for x, z in ((10, 10), (69, 10), (10, 69), (69, 69)):
		s.fill(x - 2, 3, z - 2, x + 2, 8, z + 2, POLISHED_BLACKSTONE_BRICKS)
		s.fill(x - 1, 9, z - 1, x + 1, 12, z + 1, GILDED_BLACKSTONE)
		for y in range(13, 21):
			s.set(x, y, z, CHAIN)
	for offset in range(0, 80, 8):
		s.fill(offset, 3, 0, min(offset + 2, 79), 16, 2, DEEPSLATE)
		s.fill(offset, 3, 77, min(offset + 2, 79), 16, 79, DEEPSLATE)
	for x, z in ((5, 20), (74, 20), (5, 58), (74, 58)):
		brazier(s, x, z)
	# Scorch marks are flat, so they do not interfere with pathfinding.
	for z in range(22, 58):
		if z % 4 != 0:
			s.set(39, 2, z, BLACKSTONE)
			s.set(40, 2, z, CRACKED_BLACKSTONE_BRICKS)
	s.marker(40, 3, 0, "connector_boss_in")
	s.marker(40, 3, 12, "combat_threshold")
	s.marker(40, 3, 38, "boss_spawn_cerberus")
	s.marker(37, 3, 6, "safe_anchor")
	s.marker(40, 4, 79, "gate_controller")
	s.marker(40, 3, 77, "connector_boss_out")
	return s


def make_tower_stage(stage_name: str, height: int) -> Structure:
	s = Structure(f"dkc_tower_{stage_name}", 64, 64, height)
	center = 31.5
	outer_radius = 31.6
	inner_radius = 27.0
	if stage_name == "base":
		for z in range(64):
			for x in range(64):
				if math.hypot(x - center, z - center) <= outer_radius:
					s.set(x, 0, z, BEDROCK)
					s.set(x, 1, z, DEEPSLATE)
					s.set(x, 2, z, DEEPSLATE_TILES if (x + z) % 9 else POLISHED_BLACKSTONE)
		start_y = 3
	else:
		start_y = 0

	if stage_name == "crown":
		for y in range(height):
			progress = max(0.0, (y - 5) / max(1, height - 6))
			outer = outer_radius - 20.0 * progress
			inner = max(5.0, outer - 4.0)
			for z in range(64):
				for x in range(64):
					distance = math.hypot(x - center, z - center)
					if inner <= distance <= outer:
						block = GILDED_BLACKSTONE if y % 8 in (0, 1) else DEEPSLATE_TILES
						s.set(x, y, z, block)
		# Eight curved spines make the crown readable without squaring its silhouette.
		for spine in range(8):
			angle = spine * math.tau / 8.0
			for y in range(8, height):
				progress = max(0.0, (y - 5) / max(1, height - 6))
				radius = outer_radius - 20.0 * progress - 1.5
				x = round(center + math.cos(angle) * radius)
				z = round(center + math.sin(angle) * radius)
				s.fill(x - 1, y, z - 1, x + 1, y, z + 1,
						GILDED_BLACKSTONE if y % 6 == 0 else POLISHED_BLACKSTONE_BRICKS)
		return s

	# A four-block cylindrical shell with backed curved windows forms the tower body.
	for y in range(start_y, height):
		wall = DEEPSLATE if (y // 6) % 2 == 0 else DEEPSLATE_TILES
		window_band = (y - start_y) % 18 in range(7, 14)
		belt = y in (start_y, start_y + 1, height - 2, height - 1)
		for z in range(64):
			for x in range(64):
				dx, dz = x - center, z - center
				distance = math.hypot(dx, dz)
				if not inner_radius <= distance <= outer_radius:
					continue
				angle = math.atan2(dz, dx)
				window_axis = round(angle / (math.pi / 4.0)) * (math.pi / 4.0)
				angle_delta = abs(math.atan2(math.sin(angle - window_axis), math.cos(angle - window_axis)))
				if belt:
					block = GILDED_BLACKSTONE if y in (height - 2, height - 1) else POLISHED_BLACKSTONE_BRICKS
				elif window_band and distance >= 29.8 and angle_delta < 0.105:
					block = RED_GLASS
				else:
					block = wall
				s.set(x, y, z, block)

	# Sixteen integrated ribs preserve the circular silhouette at long distance.
	for rib in range(16):
		angle = rib * math.tau / 16.0
		x = round(center + math.cos(angle) * 29.2)
		z = round(center + math.sin(angle) * 29.2)
		for y in range(start_y, height):
			material = GILDED_BLACKSTONE if y % 12 in (0, 1) else POLISHED_BLACKSTONE_BRICKS
			for dz in range(-1, 2):
				for dx in range(-1, 2):
					if math.hypot(x + dx - center, z + dz - center) <= outer_radius:
						s.set(x + dx, y, z + dz, material)
	if stage_name == "base":
		s.carve("south", 15, 18, 7)
		# The only rectangular mass is the ceremonial gate facade attached to the curve.
		s.fill(18, 3, 0, 23, 32, 8, POLISHED_BLACKSTONE_BRICKS)
		s.fill(40, 3, 0, 45, 32, 8, POLISHED_BLACKSTONE_BRICKS)
		s.fill(18, 21, 0, 45, 26, 8, GILDED_BLACKSTONE)
	elif stage_name in ("mid_b", "mid_d"):
		y = max(8, height // 2)
		for band_y in range(y, y + 3):
			for z in range(64):
				for x in range(64):
					if 26.0 <= math.hypot(x - center, z - center) <= outer_radius:
						s.set(x, band_y, z, GILDED_BLACKSTONE)
	return s


def make_gate(opened: bool) -> Structure:
	name = "dkc_tower_gate_open" if opened else "dkc_tower_gate_closed"
	s = Structure(name, 21, 7, 21)
	s.fill(0, 0, 0, 20, 2, 6, DEEPSLATE_TILES)
	s.fill(0, 3, 0, 2, 20, 6, POLISHED_BLACKSTONE_BRICKS)
	s.fill(18, 3, 0, 20, 20, 6, POLISHED_BLACKSTONE_BRICKS)
	s.fill(0, 18, 0, 20, 20, 6, GILDED_BLACKSTONE)
	if opened:
		# The open template is overlaid on the already-placed closed gate.  Keep
		# the aperture as explicit AIR (rather than relying only on the connector
		# edge carves) so the middle z-slice of the old portcullis is removed too.
		s.fill(3, 3, 1, 17, 16, 5, AIR)
		s.fill(4, 17, 1, 16, 20, 5, IRON_BARS)
		s.fill(2, 16, 2, 4, 20, 4, CHAIN)
		s.fill(16, 16, 2, 18, 20, 4, CHAIN)
	else:
		s.fill(3, 3, 1, 17, 17, 5, IRON_BARS)
		s.fill(9, 3, 1, 11, 17, 5, CRYING_OBSIDIAN)
		s.fill(3, 9, 1, 17, 11, 5, GILDED_BLACKSTONE)
	s.marker(10, 3, 3, "gate_controller")
	s.marker(10, 3, 0, "connector_boss_in")
	s.marker(10, 3, 6, "connector_boss_out")
	return s


def make_lobby() -> Structure:
	s = Structure("dkc_tower_lobby", 36, 36, 32)
	foundation(s)
	patterned_floor(s, POLISHED_BLACKSTONE_BRICKS, GILDED_BLACKSTONE, 9)
	center = 17.5
	for y in range(3, 31):
		for z in range(36):
			for x in range(36):
				distance = math.hypot(x - center, z - center)
				if 15.0 <= distance <= 17.8:
					s.set(x, y, z, DEEPSLATE_BRICKS if y % 7 else GILDED_BLACKSTONE)
	for z in range(36):
		for x in range(36):
			if math.hypot(x - center, z - center) <= 17.8:
				s.set(x, 31, z, DEEPSLATE_TILES)
	s.carve("south", 15, 16, 3)
	s.carve("north", 11, 9, 3)
	for angle_index in range(8):
		angle = angle_index * math.tau / 8.0
		x = round(center + math.cos(angle) * 11.5)
		z = round(center + math.sin(angle) * 11.5)
		pillar(s, x, z, 23, POLISHED_BLACKSTONE_BRICKS, GILDED_BLACKSTONE)
	for z in range(4, 32):
		s.set(17, 2, z, GILDED_BLACKSTONE)
		s.set(18, 2, z, GILDED_BLACKSTONE)
	for x, z in ((6, 11), (29, 11), (6, 25), (29, 25)):
		brazier(s, x, z)
	s.marker(18, 3, 6, "safe_anchor")
	s.marker(15, 3, 6, "floor_1_checkpoint")
	s.marker(18, 3, 18, "aura_anchor")
	s.marker(18, 3, 0, "connector_boss_in")
	s.marker(18, 3, 35, "connector_main_out")
	return s


def make_ascension_chamber(name: str = "dkc_f1_ascension_chamber", noble: bool = False) -> Structure:
	s = Structure(name, 32, 32, 24)
	foundation(s, POLISHED_BLACKSTONE_BRICKS if noble else DEEPSLATE_TILES)
	patterned_floor(s, POLISHED_BLACKSTONE_BRICKS, GILDED_BLACKSTONE if noble else CRYING_OBSIDIAN, 8)
	# Floor 1's chamber overlaps the rear curve of the tower base.  These
	# explicit AIR records cut a clean interior through that pre-existing shell
	# when the sparse chamber template is overlaid.
	s.fill(3, 3, 0, 28, 21, 8, AIR)
	perimeter(s, 22, POLISHED_BLACKSTONE_BRICKS, 3)
	s.fill(0, 23, 0, 31, 23, 31, DEEPSLATE_TILES)
	s.carve("south", 11, 9, 3)
	center = 16
	for z in range(8, 25):
		for x in range(8, 25):
			d2 = (x - center) ** 2 + (z - center) ** 2
			if 42 <= d2 <= 72:
				s.set(x, 2, z, GILDED_BLACKSTONE)
			elif d2 < 42:
				s.set(x, 2, z, CRYING_OBSIDIAN if not noble else WHITE_GLASS)
	s.fill(13, 3, 13, 19, 3, 19, POLISHED_BLACKSTONE_BRICKS)
	for x, z in ((6, 6), (25, 6), (6, 25), (25, 25)):
		pillar(s, x, z, 15)
		brazier(s, x, z)
	s.marker(16, 4, 16, "ascension_seal")
	s.marker(16, 4, 18, "transition_out")
	s.marker(13, 3, 7, "safe_anchor")
	s.marker(16, 3, 0, "connector_main_in")
	return s


def make_return_shrine() -> Structure:
	s = Structure("dkc_return_shrine", 16, 16, 16)
	foundation(s, POLISHED_BLACKSTONE_BRICKS)
	perimeter(s, 14, DEEPSLATE_BRICKS, 2)
	s.fill(0, 15, 0, 15, 15, 15, DEEPSLATE_TILES)
	s.carve("south", 7, 7, 2)
	s.fill(4, 3, 9, 11, 3, 13, GILDED_BLACKSTONE)
	s.fill(4, 4, 12, 5, 11, 13, CRYING_OBSIDIAN)
	s.fill(10, 4, 12, 11, 11, 13, CRYING_OBSIDIAN)
	s.fill(4, 11, 12, 11, 12, 13, POLISHED_BLACKSTONE_BRICKS)
	s.marker(8, 4, 11, "return_sigil")
	s.marker(6, 3, 7, "safe_anchor")
	s.marker(8, 3, 0, "connector_side_1")
	return s


def lower_shell(name: str, width: int, length: int, height: int, openings: tuple[str, ...]) -> Structure:
	s = Structure(name, width, length, height)
	foundation(s, DEEPSLATE_BRICKS)
	patterned_floor(s, DEEPSLATE_BRICKS, RED_NETHER_BRICKS, 8)
	perimeter(s, min(height - 2, 11), DEEPSLATE, 2)
	for side in openings:
		s.carve(side, 11 if side in ("south", "north") else 7, 9 if side in ("south", "north") else 7, 2)
	return s


def make_lower_start() -> Structure:
	s = lower_shell("dkc_lower_start", 24, 24, 16, ("north",))
	s.fill(0, 15, 0, 23, 15, 23, DEEPSLATE_TILES)
	for x, z in ((4, 5), (19, 5), (4, 18), (19, 18)):
		pillar(s, x, z, 12, DEEPSLATE_BRICKS, RED_NETHER_BRICKS)
	s.marker(12, 3, 9, "player_start")
	s.marker(9, 3, 9, "safe_anchor")
	s.marker(12, 3, 23, "connector_main_out")
	return s


def make_lower_street(variant: str) -> Structure:
	s = lower_shell(f"dkc_lower_street_{variant}", 32, 24, 20, ("south", "north"))
	s.fill(10, 2, 0, 21, 2, 23, POLISHED_BLACKSTONE)
	# Dense but shallow facades keep the 9-11 block road clear.
	for z in (2, 9, 16):
		left_h = 12 + ((z + ord(variant)) % 6)
		right_h = 11 + ((z * 2 + ord(variant)) % 7)
		s.fill(2, 3, z, 8, left_h, min(23, z + 6), RED_NETHER_BRICKS if z % 2 else DEEPSLATE_BRICKS)
		s.fill(23, 3, z, 29, right_h, min(23, z + 6), NETHER_BRICKS if z % 2 else DEEPSLATE)
		for y in range(6, min(left_h, 15), 4):
			s.set(8, y, min(22, z + 2), ORANGE_GLASS)
	if variant == "b":
		s.fill(3, 3, 8, 8, 5, 12, DARK_OAK)
		s.fill(23, 3, 14, 28, 4, 18, BASALT)
	elif variant == "c":
		s.fill(4, 3, 5, 8, 7, 9, CRACKED_BRICKS)
		s.fill(23, 3, 17, 27, 8, 21, RED_CONCRETE)
	for x in (4, 27):
		brazier(s, x, 6)
		brazier(s, x, 18)
	s.marker(16, 3, 0, "connector_main_in")
	s.marker(16, 3, 23, "connector_main_out")
	s.marker(12, 3, 12, "pack_spawn_1")
	s.marker(20, 3, 16, "pack_spawn_2")
	return s


def make_lower_intersection(four_way: bool) -> Structure:
	name = "dkc_lower_intersection_four_way" if four_way else "dkc_lower_intersection_t"
	openings = ("south", "north", "east", "west") if four_way else ("south", "east", "west")
	s = lower_shell(name, 32, 32, 20, openings)
	s.fill(10, 2, 0, 21, 2, 31, POLISHED_BLACKSTONE)
	s.fill(0, 2, 12, 31, 2, 19, POLISHED_BLACKSTONE)
	for x, z in ((5, 5), (26, 5), (5, 26), (26, 26)):
		s.fill(x - 2, 3, z - 2, x + 2, 12 + ((x + z) % 4), z + 2, DEEPSLATE_BRICKS)
		brazier(s, x, z)
	s.marker(16, 3, 0, "connector_main_in")
	if four_way:
		s.marker(16, 3, 31, "connector_main_out")
	s.marker(0, 3, 16, "connector_side_1")
	s.marker(31, 3, 16, "connector_side_2")
	return s


def make_rune_plaza(through: bool) -> Structure:
	name = "dkc_lower_rune_plaza_through" if through else "dkc_lower_rune_plaza_branch"
	openings = ("south", "north") if through else ("south",)
	s = lower_shell(name, 32, 32, 20, openings)
	if not through:
		# Branch plazas attach to the 7x7 side-route contract, not the 11x9 main route.
		s.fill(0, 3, 0, 31, 11, 1, DEEPSLATE_BRICKS)
		s.carve("south", 7, 7, 2)
	for x, z in ((5, 5), (26, 5), (5, 26), (26, 26)):
		pillar(s, x, z, 13, DEEPSLATE_BRICKS, RED_NETHER_BRICKS)
	s.fill(13, 3, 13, 19, 3, 19, POLISHED_BLACKSTONE_BRICKS)
	s.fill(15, 4, 15, 17, 6, 17, CRYING_OBSIDIAN)
	s.set(16, 7, 16, GLOWSTONE)
	for x, z in ((6, 16), (26, 16), (16, 7), (16, 25)):
		brazier(s, x, z)
	s.marker(16, 7, 16, "objective_1")
	s.marker(9, 3, 16, "pack_spawn_1")
	s.marker(23, 3, 16, "pack_spawn_2")
	s.marker(6, 3, 6, "safe_anchor")
	s.marker(16, 3, 0, "connector_main_in" if through else "connector_side_1")
	if through:
		s.marker(16, 3, 31, "connector_main_out")
	return s


def make_patrol_market() -> Structure:
	s = lower_shell("dkc_lower_patrol_market", 48, 48, 24, ("south", "north", "east"))
	# Broad loop lanes surround a low market island.
	s.fill(8, 2, 8, 39, 2, 39, POLISHED_BLACKSTONE)
	s.fill(15, 2, 15, 32, 2, 32, DEEPSLATE_BRICKS)
	for x in (12, 22, 32):
		for z in (12, 22, 32):
			if 16 < x < 31 and 16 < z < 31:
				continue
			s.fill(x - 2, 3, z - 1, x + 2, 5, z + 1, DARK_OAK)
			s.fill(x - 2, 6, z - 1, x + 2, 6, z + 1, RED_CONCRETE)
	for x, z in ((5, 5), (42, 5), (5, 42), (42, 42)):
		pillar(s, x, z, 15)
	waypoints = {
		"patrol_1": [(10, 10), (37, 10), (37, 18), (10, 18)],
		"patrol_2": [(10, 29), (37, 29), (37, 37), (10, 37)],
		"patrol_3": [(18, 10), (29, 10), (29, 37), (18, 37)],
	}
	for prefix, points in waypoints.items():
		for index, (x, z) in enumerate(points, 1):
			s.marker(x, 3, z, f"{prefix}_{index}")
	s.marker(24, 3, 0, "connector_main_in")
	s.marker(24, 3, 47, "connector_main_out")
	s.marker(47, 3, 24, "connector_side_1")
	return s


def make_defense_courtyard() -> Structure:
	s = lower_shell("dkc_lower_purification_courtyard", 48, 48, 24, ("south", "north"))
	for x, z in ((6, 6), (41, 6), (6, 41), (41, 41)):
		pillar(s, x, z, 15, DEEPSLATE_BRICKS, RED_NETHER_BRICKS)
	s.fill(20, 3, 20, 27, 3, 27, POLISHED_BLACKSTONE_BRICKS)
	for z in range(18, 30):
		for x in range(18, 30):
			if 34 <= (x - 24) ** 2 + (z - 24) ** 2 <= 50:
				s.set(x, 2, z, GLOWSTONE)
	s.marker(24, 4, 24, "objective_1")
	for index, (x, z) in enumerate(((24, 7), (40, 24), (24, 40), (7, 24)), 1):
		s.marker(x, 3, z, f"pack_spawn_{index}")
	s.marker(12, 3, 16, "caster_spawn_1")
	s.marker(36, 3, 32, "caster_spawn_2")
	s.marker(8, 3, 8, "safe_anchor")
	s.marker(24, 3, 0, "connector_main_in")
	s.marker(24, 3, 47, "connector_main_out")
	return s


def make_magma_sluice() -> Structure:
	s = lower_shell("dkc_lower_magma_sluice", 48, 48, 24, ("south", "north", "east", "west"))
	# Contained channels and five-block bridges preserve safe traversal.
	for z in range(5, 43):
		for x in (8, 9, 10, 37, 38, 39):
			s.set(x, 2, z, MAGMA if z % 3 else ORANGE_GLASS)
	for x in range(11, 37):
		for z in (20, 21, 22, 25, 26, 27):
			s.set(x, 2, z, MAGMA if x % 3 else ORANGE_GLASS)
	s.fill(21, 2, 0, 26, 2, 47, POLISHED_BLACKSTONE)
	s.fill(0, 2, 21, 47, 2, 26, POLISHED_BLACKSTONE)
	for x, z in ((15, 15), (32, 15), (15, 32), (32, 32)):
		s.fill(x - 2, 3, z - 2, x + 2, 8, z + 2, BASALT)
		s.set(x, 9, z, CUT_COPPER)
	s.marker(24, 3, 24, "objective_1")
	s.marker(0, 3, 24, "connector_side_1")
	s.marker(47, 3, 24, "connector_side_2")
	s.marker(24, 3, 0, "connector_main_in")
	s.marker(24, 3, 47, "connector_main_out")
	return s


def make_valve_chamber(variant: str) -> Structure:
	s = lower_shell(f"dkc_lower_valve_chamber_{variant}", 24, 24, 18, ("south",))
	# Valve chambers are optional side rooms and use the 7x7 side-route threshold.
	s.fill(0, 3, 0, 23, 11, 1, DEEPSLATE_BRICKS)
	s.carve("south", 7, 7, 2)
	s.fill(8, 3, 12, 15, 6, 18, BASALT)
	s.fill(10, 7, 14, 13, 11, 17, CUT_COPPER if variant != "c" else EXPOSED_CUT_COPPER)
	s.fill(11, 8, 11, 12, 10, 14, IRON_BARS)
	for x, z in ((4, 5), (19, 5), (4, 19), (19, 19)):
		pillar(s, x, z, 12, DEEPSLATE_BRICKS, RED_NETHER_BRICKS)
	s.marker(12, 7, 13, "objective_1")
	s.marker(12, 3, 0, "connector_side_1")
	return s


def make_lower_transition(rest: bool) -> Structure:
	name = "dkc_lower_rest_transition" if rest else "dkc_lower_transition"
	s = make_ascension_chamber(name)
	if rest:
		s.marker(7, 3, 16, "return_sigil")
		s.marker(24, 3, 16, "objective_1")
	return s


def make_wall_cap() -> Structure:
	s = Structure("dkc_lower_wall_cap", 13, 4, 12)
	s.fill(0, 0, 0, 12, 2, 3, DEEPSLATE)
	s.fill(0, 3, 0, 12, 11, 3, DEEPSLATE_BRICKS)
	s.fill(1, 4, 0, 11, 9, 1, RED_NETHER_BRICKS)
	for x in (2, 6, 10):
		s.fill(x, 3, 0, x, 11, 3, POLISHED_BLACKSTONE_BRICKS)
	return s


def make_adapter() -> Structure:
	s = lower_shell("dkc_lower_connector_adapter", 16, 16, 16, ("south", "north"))
	s.fill(0, 15, 0, 15, 15, 15, DEEPSLATE_TILES)
	s.marker(8, 3, 0, "connector_main_in")
	s.marker(8, 3, 15, "connector_main_out")
	return s


def open_foundation(s: Structure, primary: State, accent: State) -> None:
	foundation(s, primary)
	for z in range(s.length):
		for x in range(s.width):
			if (x * 7 + z * 11) % 37 == 0:
				s.set(x, 2, z, accent)
	# Every outdoor district keeps one wide, readable north/south route.
	s.fill(s.width // 2 - 6, 2, 0, s.width // 2 + 5, 2, s.length - 1, POLISHED_BLACKSTONE)
	for z in range(5, s.length, 12):
		s.fill(s.width // 2 - 1, 2, z, s.width // 2, 2, min(s.length - 1, z + 2), GILDED_BLACKSTONE)


def burning_ruin(s: Structure, x: int, z: int, width: int, length: int, height: int, material: State) -> None:
	# An intentionally broken shell: missing corners and roof sections keep each
	# ruin cheap to render while still reading as a burned building.
	s.fill(x, 3, z, x + width - 1, 3, z + length - 1, CRACKED_BRICKS)
	for y in range(4, height):
		if y % 3 != 1:
			s.fill(x, y, z, x + width - 1, y, z, material)
			s.fill(x, y, z + length - 1, x + width - 1, y, z + length - 1, material)
		if y % 4 != 2:
			s.fill(x, y, z, x, y, z + length - 1, material)
			s.fill(x + width - 1, y, z, x + width - 1, y, z + length - 1, material)
	for beam_x in range(x + 2, x + width - 1, 4):
		s.fill(beam_x, 4, z + 2, beam_x, min(height + 3, s.height - 2), z + 2, STRIPPED_DARK_OAK)
	for fx, fz in ((x + 2, z + 2), (x + width - 3, z + length - 3)):
		s.set(fx, 3, fz, NETHERRACK)
		s.set(fx, 4, fz, FIRE)


def make_open_burnt_village() -> Structure:
	s = Structure("dkc_open_burnt_village", 80, 80, 32)
	open_foundation(s, BLACKSTONE, NETHERRACK)
	s.fill(0, 2, 34, 79, 2, 45, POLISHED_BLACKSTONE)
	for args in ((4, 6, 22, 18, 15, DEEPSLATE_BRICKS), (52, 5, 23, 21, 18, RED_NETHER_BRICKS),
			(5, 51, 24, 22, 17, NETHER_BRICKS), (53, 54, 20, 19, 14, DEEPSLATE)):
		burning_ruin(s, *args)
	for x, z in ((9, 35), (22, 43), (58, 36), (70, 44), (33, 12), (47, 66)):
		s.set(x, 2, z, NETHERRACK)
		s.set(x, 3, z, FIRE)
	s.marker(40, 3, 0, "connector_main_in")
	s.marker(40, 3, 79, "connector_main_out")
	s.marker(40, 3, 40, "objective_1")
	for index, (x, z) in enumerate(((26, 25), (54, 25), (25, 57), (55, 57)), 1):
		s.marker(x, 3, z, f"pack_spawn_{index}")
	return s


def make_open_ash_wastes() -> Structure:
	s = Structure("dkc_open_ash_wastes", 80, 80, 24)
	open_foundation(s, BASALT, MAGMA)
	# Dead, fire-scarred trees and shallow magma fissures create an outdoor
	# overworld silhouette without foliage ticks, caves, fluids, or features.
	for index, (x, z, height) in enumerate(((10, 12, 11), (21, 29, 14), (67, 17, 12), (59, 38, 15),
			(13, 61, 13), (67, 65, 16), (27, 70, 10), (53, 7, 12))):
		s.fill(x, 3, z, x, height, z, STRIPPED_DARK_OAK)
		branch_y = height - 3
		s.fill(x - 2, branch_y, z, x + 2, branch_y, z, STRIPPED_DARK_OAK)
		if index % 2 == 0:
			s.set(x, height + 1, z, FIRE)
	for z in range(9, 72):
		x = 14 + ((z * 13) % 51)
		if abs(x - 40) > 8:
			s.set(x, 2, z, MAGMA if z % 3 else NETHERRACK)
	s.marker(40, 3, 0, "connector_main_in")
	s.marker(40, 3, 79, "connector_main_out")
	s.marker(40, 3, 42, "objective_1")
	return s


def make_open_ruined_cathedral() -> Structure:
	s = Structure("dkc_open_ruined_cathedral", 80, 80, 40)
	open_foundation(s, DEEPSLATE_TILES, CRYING_OBSIDIAN)
	# Nave, broken transept and an incomplete rose-window facade.
	s.fill(13, 3, 8, 18, 25, 70, DEEPSLATE_BRICKS)
	s.fill(61, 3, 8, 66, 25, 70, DEEPSLATE_BRICKS)
	s.fill(13, 3, 8, 66, 23, 13, POLISHED_BLACKSTONE_BRICKS)
	for z in range(18, 68, 12):
		for x in (22, 57):
			pillar(s, x, z, 22, POLISHED_BLACKSTONE_BRICKS, GILDED_BLACKSTONE)
	for x in range(29, 52):
		for y in range(11, 32):
			if 70 <= (x - 40) ** 2 + (y - 21) ** 2 <= 108:
				s.set(x, y, 11, RED_GLASS if (x + y) % 3 else ORANGE_GLASS)
	# Carry the main entrance through the full six-block facade after decorating
	# it. The former depth of six stopped at z=5 and left the nave sealed at
	# z=8..13; carving earlier also let the rose window refill the doorway.
	s.carve("south", 15, 18, 14)
	for x, z in ((25, 25), (55, 25), (25, 55), (55, 55)):
		brazier(s, x, z)
	s.marker(40, 3, 0, "connector_main_in")
	s.marker(40, 3, 79, "connector_main_out")
	s.marker(40, 3, 58, "objective_1")
	return s


def make_open_forge_arena() -> Structure:
	s = Structure("dkc_open_forge_arena", 80, 80, 30)
	open_foundation(s, POLISHED_BLACKSTONE, MAGMA)
	perimeter(s, 17, POLISHED_BLACKSTONE_BRICKS, 3)
	s.carve("south", 15, 14, 3)
	s.carve("north", 15, 14, 3)
	# Contained heat channels leave a 48x48 unobstructed boss arena.
	for edge in range(8, 72):
		for x, z in ((edge, 8), (edge, 71), (8, edge), (71, edge)):
			s.set(x, 2, z, MAGMA if edge % 4 else ORANGE_GLASS)
	for x, z in ((12, 12), (67, 12), (12, 67), (67, 67)):
		s.fill(x - 3, 3, z - 3, x + 3, 13, z + 3, BASALT)
		s.fill(x - 1, 14, z - 1, x + 1, 19, z + 1, GILDED_BLACKSTONE)
		brazier(s, x, z)
	s.marker(40, 3, 0, "connector_main_in")
	s.marker(40, 3, 79, "connector_main_out")
	s.marker(40, 3, 40, "boss_spawn_vulcan")
	return s


def make_open_dragon_court() -> Structure:
	s = Structure("dkc_open_dragon_court", 80, 80, 36)
	open_foundation(s, DEEPSLATE_BRICKS, GILDED_BLACKSTONE)
	perimeter(s, 20, DEEPSLATE, 3)
	s.carve("south", 15, 16, 3)
	s.carve("north", 15, 16, 3)
	for x, z in ((8, 8), (71, 8), (8, 71), (71, 71), (8, 40), (71, 40)):
		s.fill(x - 2, 3, z - 2, x + 2, 24, z + 2, POLISHED_BLACKSTONE_BRICKS)
		for y in range(25, 34):
			s.set(x, y, z, CHAIN)
	# A broad circular landing sigil remains clear for Kaiselin.
	for z in range(18, 63):
		for x in range(18, 63):
			d2 = (x - 40) ** 2 + (z - 40) ** 2
			if 320 <= d2 <= 410:
				s.set(x, 2, z, CRYING_OBSIDIAN if (x + z) % 4 else GILDED_BLACKSTONE)
	s.marker(40, 3, 0, "connector_main_in")
	s.marker(40, 3, 79, "connector_main_out")
	s.marker(40, 4, 40, "dragon_spawn")
	return s


def make_open_throne_court() -> Structure:
	s = Structure("dkc_open_throne_court", 80, 80, 44)
	open_foundation(s, POLISHED_BLACKSTONE_BRICKS, GILDED_BLACKSTONE)
	perimeter(s, 24, DEEPSLATE_TILES, 4)
	s.carve("south", 17, 18, 4)
	# Baran's elevated throne and broken crown arch dominate the north end.
	s.fill(25, 3, 58, 55, 5, 74, POLISHED_BLACKSTONE_BRICKS)
	s.fill(31, 6, 65, 49, 11, 75, GILDED_BLACKSTONE)
	s.fill(35, 12, 69, 45, 22, 76, CRYING_OBSIDIAN)
	for x in (18, 62):
		pillar(s, x, 18, 30, DEEPSLATE_BRICKS, GILDED_BLACKSTONE)
		pillar(s, x, 52, 30, DEEPSLATE_BRICKS, GILDED_BLACKSTONE)
	for offset in range(0, 18):
		s.set(31 - offset // 2, 24 + offset, 69, GILDED_BLACKSTONE)
		s.set(49 + offset // 2, 24 + offset, 69, GILDED_BLACKSTONE)
	for x, z in ((12, 12), (68, 12), (12, 66), (68, 66)):
		s.set(x, 2, z, NETHERRACK)
		s.set(x, 3, z, SOUL_FIRE)
	s.marker(40, 3, 0, "connector_main_in")
	s.marker(40, 6, 54, "boss_spawn_baran")
	s.marker(40, 6, 34, "dragon_spawn")
	return s


def hollow_room(s: Structure, x1: int, z1: int, x2: int, z2: int,
		height: int, wall: State, roof: State | None = None, thickness: int = 2) -> None:
	"""Author a broad hollow room without filling its interior with disposable AIR."""
	s.fill(x1, 3, z1, x2, height, z1 + thickness - 1, wall)
	s.fill(x1, 3, z2 - thickness + 1, x2, height, z2, wall)
	s.fill(x1, 3, z1, x1 + thickness - 1, height, z2, wall)
	s.fill(x2 - thickness + 1, 3, z1, x2, height, z2, wall)
	if roof is not None:
		s.fill(x1, height + 1, z1, x2, height + 1, z2, roof)


def castle_tower(s: Structure, center_x: int, center_z: int, radius: int, height: int) -> None:
	"""A hollow octagonal noble tower with a narrow crown rather than a solid cube."""
	for y in range(3, height + 1):
		for z in range(center_z - radius, center_z + radius + 1):
			for x in range(center_x - radius, center_x + radius + 1):
				dx, dz = abs(x - center_x), abs(z - center_z)
				outer = max(dx, dz) <= radius and dx + dz <= radius + radius // 2
				inner = max(dx, dz) < radius - 2 and dx + dz < radius + radius // 2 - 3
				if outer and not inner:
					block = GILDED_BLACKSTONE if y % 9 in (0, 1) else RED_NETHER_BRICKS
					s.set(x, y, z, block)
	for z in range(center_z - radius, center_z + radius + 1):
		for x in range(center_x - radius, center_x + radius + 1):
			dx, dz = abs(x - center_x), abs(z - center_z)
			if max(dx, dz) <= radius and dx + dz <= radius + radius // 2:
				s.set(x, height + 1, z, POLISHED_BLACKSTONE_BRICKS)
	# Alternating merlons keep the skyline readable without a block-heavy roof.
	for step in range(-radius, radius + 1, 3):
		for x, z in ((center_x + step, center_z - radius), (center_x + step, center_z + radius),
				(center_x - radius, center_z + step), (center_x + radius, center_z + step)):
			s.fill(x, height + 2, z, x, height + 4, z, GILDED_BLACKSTONE)
	for y in range(height + 2, min(s.height - 1, height + 13)):
		radius_at_y = max(1, 5 - (y - height - 2) // 3)
		s.fill(center_x - radius_at_y, y, center_z - radius_at_y,
				center_x + radius_at_y, y, center_z + radius_at_y,
				CRYING_OBSIDIAN if y % 4 == 0 else POLISHED_BLACKSTONE_BRICKS)


def make_radiru_castle() -> Structure:
	"""House Radiru's open, traversable Floor-15 castle and training keep."""
	s = Structure("dkc_radiru_castle", 112, 96, 64)
	foundation(s, POLISHED_BLACKSTONE_BRICKS)
	patterned_floor(s, POLISHED_BLACKSTONE_BRICKS, RED_NETHER_BRICKS, 12)

	# A crenellated curtain with four high noble towers gives the castle a strong
	# silhouette while the 20-wide northern gate and all internal doors stay open.
	perimeter(s, 15, RED_NETHER_BRICKS, 3)
	for x in range(3, 109, 4):
		s.fill(x, 16, 0, min(x + 1, 108), 18, 2, GILDED_BLACKSTONE)
		s.fill(x, 16, 93, min(x + 1, 108), 18, 95, GILDED_BLACKSTONE)
	for z in range(3, 93, 4):
		s.fill(0, 16, z, 2, 18, min(z + 1, 92), GILDED_BLACKSTONE)
		s.fill(109, 16, z, 111, 18, min(z + 1, 92), GILDED_BLACKSTONE)
	for center_x, center_z in ((10, 10), (101, 10), (10, 85), (101, 85)):
		castle_tower(s, center_x, center_z, 9, 34)

	# Gatehouse facing the player's arrival field. Its aperture is overlaid by a
	# tiny closed/open portcullis template so surrender never rebuilds the castle.
	hollow_room(s, 35, 78, 76, 95, 25, POLISHED_BLACKSTONE_BRICKS, DEEPSLATE_TILES, 3)
	s.fill(39, 20, 81, 72, 25, 94, GILDED_BLACKSTONE)
	s.fill(46, 3, 78, 65, 18, 95, AIR)
	for x in (38, 73):
		pillar(s, x, 83, 31, RED_NETHER_BRICKS, GILDED_BLACKSTONE)
		pillar(s, x, 91, 31, RED_NETHER_BRICKS, GILDED_BLACKSTONE)
	for x in range(43, 69):
		for y in range(20, 31):
			if 45 <= (x - 56) ** 2 + (y - 25) ** 2 <= 78:
				s.set(x, y, 82, RED_GLASS if (x + y) % 3 else ORANGE_GLASS)

	# The central court is deliberately broad: resident demons can idle here and
	# the player always retains a direct 16-block-wide line to the keep.
	s.fill(15, 2, 58, 96, 2, 80, DEEPSLATE_TILES)
	s.fill(48, 2, 56, 63, 2, 95, GILDED_BLACKSTONE)
	for x, z in ((22, 65), (89, 65), (22, 75), (89, 75)):
		brazier(s, x, z)
	for x, z in ((31, 69), (80, 69)):
		pillar(s, x, z, 16, POLISHED_BLACKSTONE_BRICKS, GILDED_BLACKSTONE)

	# The keep is one tall, open great hall rather than a maze of small rooms.
	hollow_room(s, 16, 6, 95, 60, 28, POLISHED_BLACKSTONE_BRICKS, DEEPSLATE_TILES, 3)
	s.fill(47, 3, 56, 64, 17, 60, AIR)
	for z in range(15, 55, 10):
		for x in (27, 40, 71, 84):
			pillar(s, x, z, 23, RED_NETHER_BRICKS, GILDED_BLACKSTONE)
	# A glowing skylight keeps the great hall visually open and cheap to light.
	for z in range(17, 51):
		for x in range(43, 69):
			if x in (43, 68) or z in (17, 50) or (x + z) % 7 == 0:
				s.set(x, 29, z, RED_GLASS if (x + z) % 3 else ORANGE_GLASS)
	for x, z in ((22, 12), (89, 12), (22, 53), (89, 53), (34, 32), (77, 32)):
		integrated_lights(s, [(x, 24, z)])

	# Esil's dais is visible from the keep doors and remains large enough for
	# dialogue/cutscene positioning without collision around the anchor.
	s.fill(43, 3, 10, 68, 4, 31, RED_NETHER_BRICKS)
	s.fill(47, 5, 13, 64, 6, 27, GILDED_BLACKSTONE)
	s.fill(51, 7, 15, 60, 12, 19, POLISHED_BLACKSTONE_BRICKS)
	s.fill(53, 8, 20, 58, 10, 24, CRYING_OBSIDIAN)
	# Clear the actual Esil stand and the broad approach to it after decorating.
	s.fill(52, 3, 23, 59, 6, 30, AIR)
	s.fill(49, 3, 30, 62, 12, 56, AIR)
	s.fill(47, 2, 23, 64, 2, 60, GILDED_BLACKSTONE)

	# East wing: an open training hall with six durable, obstruction-free pads.
	s.fill(73, 2, 22, 94, 2, 55, CRACKED_BLACKSTONE_BRICKS)
	s.fill(73, 3, 22, 75, 14, 55, RED_NETHER_BRICKS)
	s.fill(92, 3, 22, 94, 14, 55, RED_NETHER_BRICKS)
	s.fill(73, 3, 22, 94, 14, 24, RED_NETHER_BRICKS)
	s.fill(73, 3, 53, 94, 14, 55, RED_NETHER_BRICKS)
	s.fill(73, 3, 35, 77, 11, 43, AIR)
	for x in (79, 86):
		for z in (29, 39, 49):
			for px in range(x - 2, x + 3):
				for pz in range(z - 2, z + 3):
					d2 = (px - x) ** 2 + (pz - z) ** 2
					if 3 <= d2 <= 6:
						s.set(px, 2, pz, GILDED_BLACKSTONE)
	# West wing mirrors the training hall as a sparse resident commons.
	s.fill(17, 2, 22, 38, 2, 55, CRACKED_BLACKSTONE_BRICKS)
	s.fill(17, 3, 22, 19, 12, 55, RED_NETHER_BRICKS)
	s.fill(36, 3, 22, 38, 12, 55, RED_NETHER_BRICKS)
	s.fill(17, 3, 22, 38, 12, 24, RED_NETHER_BRICKS)
	s.fill(17, 3, 53, 38, 12, 55, RED_NETHER_BRICKS)
	s.fill(34, 3, 35, 38, 10, 43, AIR)

	# Stable semantic markers are validated here and stripped after placement;
	# matching public BlockPos helpers in DkcFloorBuilder are the runtime API.
	s.marker(56, 3, 25, "radiru_esil")
	for index, (x, z) in enumerate(((35, 67), (76, 67), (27, 73), (84, 73)), 1):
		s.marker(x, 3, z, f"radiru_resident_{index}")
	for index, (x, z) in enumerate(((79, 29), (86, 29), (79, 39), (86, 39), (79, 49), (86, 49)), 1):
		s.marker(x, 3, z, f"radiru_training_{index}")
	s.marker(56, 3, 92, "radiru_gate_controller")
	s.marker(56, 3, 74, "radiru_courtyard")
	return s


def make_radiru_battlefield() -> Structure:
	"""A wide, readable surrender-wave field between arrival and castle."""
	s = Structure("dkc_radiru_battlefield", 112, 96, 18)
	foundation(s, BLACKSTONE)
	# Keep the central 84 blocks hazard-free; fire and magma live only along the
	# flanks, where they frame the fight without trapping wave navigation.
	for z in range(s.length):
		for x in range(s.width):
			if 15 <= x <= 96:
				s.set(x, 2, z, CRACKED_BRICKS if (x * 5 + z * 7) % 31 == 0 else BLACKSTONE)
			else:
				s.set(x, 2, z, MAGMA if (x * 7 + z * 11) % 17 == 0 else BASALT)
	s.fill(47, 2, 0, 64, 2, 95, POLISHED_BLACKSTONE)
	for z in range(4, 94, 12):
		s.fill(54, 2, z, 57, 2, min(95, z + 2), GILDED_BLACKSTONE)
	# Broken edge silhouettes sell the burned city without creating rooms or LOS traps.
	for x, z, width, length, height in ((1, 8, 12, 18, 10), (98, 12, 13, 20, 12),
			(2, 54, 11, 22, 13), (99, 59, 12, 20, 10)):
		burning_ruin(s, x, z, width, length, height, RED_NETHER_BRICKS)
	for x, z in ((8, 36), (103, 41), (7, 84), (104, 88), (18, 17), (93, 76)):
		s.set(x, 2, z, NETHERRACK)
		s.set(x, 3, z, FIRE)
	for index, z in enumerate((22, 43, 64), 1):
		s.marker(56, 3, z, f"radiru_wave_{index}")
	s.marker(56, 3, 76, "radiru_arrival")
	s.marker(56, 3, 4, "radiru_castle_approach")
	return s


def make_radiru_gate(opened: bool) -> Structure:
	name = "dkc_radiru_gate_open" if opened else "dkc_radiru_gate_closed"
	s = Structure(name, 20, 8, 19)
	if opened:
		# Explicit AIR clears every closed portcullis block in-place; the raised bars
		# remain overhead as a readable permanent surrender-state silhouette.
		s.fill(0, 3, 3, 19, 14, 6, AIR)
		s.fill(1, 15, 3, 18, 17, 6, IRON_BARS)
		s.fill(1, 15, 2, 3, 18, 7, CHAIN)
		s.fill(16, 15, 2, 18, 18, 7, CHAIN)
	else:
		s.fill(0, 3, 3, 19, 14, 6, IRON_BARS)
		for x in range(1, 20, 4):
			s.fill(x, 3, 3, min(x + 1, 19), 14, 6, GILDED_BLACKSTONE)
		s.fill(8, 3, 3, 11, 14, 6, CRYING_OBSIDIAN)
	s.marker(10, 3, 4, "radiru_gate_controller")
	return s


def tile(structure: Structure, prefix: str, tile_size: int) -> list[Structure]:
	result: list[Structure] = []
	for z0 in range(0, structure.length, tile_size):
		for x0 in range(0, structure.width, tile_size):
			xi, zi = x0 // tile_size, z0 // tile_size
			result.append(structure.slice(f"{prefix}_x{xi}_z{zi}", x0, z0,
					min(tile_size, structure.width - x0), min(tile_size, structure.length - z0)))
	return result


def build_all() -> list[Structure]:
	structures: list[Structure] = [
		make_arrival_plaza(),
		make_approach("a"),
		make_approach("b"),
		*tile(make_cerberus_courtyard(), "dkc_f1_cerberus_courtyard", 40),
	]
	for stage_name, height in (("base", 48), ("mid_a", 36), ("mid_b", 36), ("mid_c", 36), ("mid_d", 36), ("crown", 48)):
		structures.extend(tile(make_tower_stage(stage_name, height), f"dkc_tower_{stage_name}", 32))
	structures.extend([
		make_gate(False),
		make_gate(True),
		make_lobby(),
		make_ascension_chamber(),
		make_return_shrine(),
		make_lower_start(),
		make_lower_street("a"),
		make_lower_street("b"),
		make_lower_street("c"),
		make_lower_intersection(False),
		make_lower_intersection(True),
		make_rune_plaza(True),
		make_rune_plaza(False),
		make_patrol_market(),
		make_defense_courtyard(),
		make_magma_sluice(),
		make_valve_chamber("a"),
		make_valve_chamber("b"),
		make_valve_chamber("c"),
		make_lower_transition(False),
		make_lower_transition(True),
		make_wall_cap(),
		make_adapter(),
		*tile(make_open_burnt_village(), "dkc_open_burnt_village", 40),
		*tile(make_open_ash_wastes(), "dkc_open_ash_wastes", 40),
		*tile(make_open_ruined_cathedral(), "dkc_open_ruined_cathedral", 40),
		*tile(make_open_forge_arena(), "dkc_open_forge_arena", 40),
		*tile(make_open_dragon_court(), "dkc_open_dragon_court", 40),
		*tile(make_open_throne_court(), "dkc_open_throne_court", 40),
		*tile(make_radiru_castle(), "dkc_radiru_castle", 32),
		*tile(make_radiru_battlefield(), "dkc_radiru_battlefield", 32),
		make_radiru_gate(False),
		make_radiru_gate(True),
	])
	return structures


def validate_structures(structures: list[Structure]) -> None:
	names = [structure.name for structure in structures]
	if len(names) != len(set(names)):
		raise ValueError("Duplicate DKC structure names")

	for structure in structures:
		for position, marker_name in structure.markers.items():
			if structure.blocks.get(position) != STRUCTURE_DATA or not marker_name.strip():
				raise ValueError(f"{structure.name}: malformed marker at {position}")
			if not marker_name.startswith("connector_"):
				continue
			# Courtyard connectors cross the 40-block tile boundary and are checked
			# against the unsliced source below. The closed gate is intentionally solid.
			if structure.name.startswith("dkc_f1_cerberus_courtyard_") or structure.name == "dkc_tower_gate_closed":
				continue

			contract = marker_name.split("_", 2)[1]
			if contract not in CONNECTOR_DIMENSIONS:
				raise ValueError(f"{structure.name}: unknown connector contract {marker_name}")
			width, height = CONNECTOR_DIMENSIONS[contract]
			x, y, z = position
			if z <= 2:
				points = ((px, py, pz) for py in range(y, y + height)
						for pz in range(2) for px in range(x - width // 2, x + width // 2 + 1))
			elif z >= structure.length - 3:
				points = ((px, py, pz) for py in range(y, y + height)
						for pz in range(structure.length - 2, structure.length)
						for px in range(x - width // 2, x + width // 2 + 1))
			elif x <= 2:
				points = ((px, py, pz) for py in range(y, y + height)
						for px in range(2) for pz in range(z - width // 2, z + width // 2 + 1))
			elif x >= structure.width - 3:
				points = ((px, py, pz) for py in range(y, y + height)
						for px in range(structure.width - 2, structure.width)
						for pz in range(z - width // 2, z + width // 2 + 1))
			else:
				raise ValueError(f"{structure.name}: connector {marker_name} is not on an edge")

			for point in points:
				block = structure.blocks.get(point, AIR)
				if block not in (AIR, STRUCTURE_DATA):
					raise ValueError(f"{structure.name}: obstructed {marker_name} at {point}")

	# Verify the 80x80 source before it is split into four placement-safe tiles.
	courtyard = make_cerberus_courtyard()
	for z in (*range(0, 3), *range(77, 80)):
		for y in range(3, 17):
			for x in range(33, 48):
				if courtyard.blocks.get((x, y, z), AIR) not in (AIR, STRUCTURE_DATA):
					raise ValueError(f"{courtyard.name}: obstructed boss connector at {(x, y, z)}")

	cathedral = make_open_ruined_cathedral()
	for z in range(8, 14):
		for y in range(3, 21):
			for x in range(33, 48):
				if cathedral.blocks.get((x, y, z), AIR) != AIR:
					raise ValueError(f"{cathedral.name}: sealed central entrance at {(x, y, z)}")

	# Floor 15 must remain a single readable route: battlefield -> portcullis ->
	# courtyard -> keep -> Esil, with every future entity anchor on solid floor
	# and at least a 3x3x3 clear pad.
	castle = make_radiru_castle()
	for x, y, z in ((56, 3, 25), (35, 3, 67), (76, 3, 67), (27, 3, 73), (84, 3, 73),
			(79, 3, 29), (86, 3, 29), (79, 3, 39), (86, 3, 39), (79, 3, 49), (86, 3, 49)):
		if castle.blocks.get((x, 2, z), AIR) in (AIR, MAGMA):
			raise ValueError(f"{castle.name}: unsafe anchor floor at {(x, y, z)}")
		for px in range(x - 1, x + 2):
			for py in range(y, y + 3):
				for pz in range(z - 1, z + 2):
					if castle.blocks.get((px, py, pz), AIR) not in (AIR, STRUCTURE_DATA):
						raise ValueError(f"{castle.name}: obstructed anchor at {(px, py, pz)}")
	for z in range(78, 96):
		for y in range(3, 15):
			for x in range(46, 66):
				if castle.blocks.get((x, y, z), AIR) not in (AIR, STRUCTURE_DATA):
					raise ValueError(f"{castle.name}: sealed Radiru gate route at {(x, y, z)}")
	def radiru_walkable(x: int, z: int) -> bool:
		return (0 <= x < castle.width and 0 <= z < castle.length
				and castle.blocks.get((x, 2, z), AIR) not in (AIR, MAGMA)
				and all(castle.blocks.get((x, y, z), AIR) in (AIR, STRUCTURE_DATA)
						for y in range(3, 6)))
	frontier = [(56, 94)]
	reachable = set(frontier)
	while frontier:
		x, z = frontier.pop()
		for candidate in ((x - 1, z), (x + 1, z), (x, z - 1), (x, z + 1)):
			if candidate not in reachable and radiru_walkable(*candidate):
				reachable.add(candidate)
				frontier.append(candidate)
	for anchor in ((56, 25), (35, 67), (76, 67), (27, 73), (84, 73),
			(79, 29), (86, 29), (79, 39), (86, 39), (79, 49), (86, 49)):
		if anchor not in reachable:
			raise ValueError(f"{castle.name}: anchor is unreachable from the outer gate: {anchor}")
	battlefield = make_radiru_battlefield()
	for z in range(4, 92):
		for y in range(3, 7):
			for x in range(47, 65):
				if battlefield.blocks.get((x, y, z), AIR) not in (AIR, STRUCTURE_DATA):
					raise ValueError(f"{battlefield.name}: blocked central navigation lane at {(x, y, z)}")


def main() -> None:
	structures = build_all()
	validate_structures(structures)
	for index, structure in enumerate(structures, 1):
		save_structure(structure)
		print(f"[{index:02d}/{len(structures):02d}] {structure.name}: {structure.width}x{structure.length}x{structure.height}")
	print(f"Generated {len(structures)} DKC structures in {OUTPUT}")


if __name__ == "__main__":
	main()
