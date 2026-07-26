package net.solocraft.tools;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

/**
 * Offline, opt-in migration for worlds whose level.dat still embeds the
 * retired Demon King's Castle and Monarch Red Gate LevelStems.
 *
 * <p>This tool intentionally edits only Data.WorldGenSettings.dimensions in
 * level.dat. It never deletes or moves any dimension folder.</p>
 */
public final class LegacyLevelStemMigrator {
	private static final DateTimeFormatter BACKUP_TIME = DateTimeFormatter
			.ofPattern("uuuuMMdd-HHmmss", Locale.ROOT)
			.withZone(ZoneOffset.UTC);
	private static final Set<String> RETIRED_LEVEL_STEMS = retiredLevelStems();

	private LegacyLevelStemMigrator() {
	}

	public static void main(String[] args) {
		try {
			run(args);
		} catch (Exception exception) {
			System.err.println("Legacy LevelStem migration failed: " + exception.getMessage());
			System.exit(2);
		}
	}

	private static void run(String[] args) throws IOException {
		if (args.length != 2 || !("--dry-run".equals(args[1]) || "--apply".equals(args[1])))
			throw new IllegalArgumentException(
					"Usage: LegacyLevelStemMigrator <world-directory> <--dry-run|--apply>");

		boolean apply = "--apply".equals(args[1]);
		Path requestedWorld = Path.of(args[0]).toAbsolutePath().normalize();
		if (!Files.isDirectory(requestedWorld, LinkOption.NOFOLLOW_LINKS))
			throw new IOException("World directory does not exist or is not a directory: " + requestedWorld);
		Path world = requestedWorld.toRealPath();
		Path levelDat = world.resolve("level.dat");
		if (Files.isSymbolicLink(levelDat)
				|| !Files.isRegularFile(levelDat, LinkOption.NOFOLLOW_LINKS))
			throw new IOException("Refusing to use a missing, non-regular, or symbolic-link level.dat: " + levelDat);

		System.out.println("World: " + world);
		System.out.println("Mode: " + (apply ? "APPLY" : "DRY RUN (no files will be changed)"));
		try (HeldSessionLock ignored = acquireSessionLock(world)) {
			CompoundTag root = NbtIo.readCompressed(levelDat.toFile());
			CompoundTag dimensions = dimensionsTag(root);
			Set<String> before = new TreeSet<>(dimensions.getAllKeys());
			List<String> present = RETIRED_LEVEL_STEMS.stream().filter(before::contains).sorted().toList();

			System.out.println("Saved LevelStems before migration: " + before.size());
			System.out.println("Retired LevelStems present: " + present.size());
			for (String key : present)
				System.out.println("  - " + key);
			System.out.println("All non-target LevelStems will be preserved.");

			if (present.isEmpty()) {
				System.out.println("Nothing to migrate; level.dat was not changed.");
				return;
			}
			if (!apply) {
				System.out.println("Dry run complete. Re-run with -PapplyLegacyStemMigration=true to apply.");
				return;
			}

			for (String key : present)
				dimensions.remove(key);
			Set<String> expected = new TreeSet<>(before);
			expected.removeAll(RETIRED_LEVEL_STEMS);
			if (!new TreeSet<>(dimensions.getAllKeys()).equals(expected))
				throw new IOException("In-memory validation detected an unexpected LevelStem-key change.");

			Path temporary = Files.createTempFile(world, ".level.dat-legacy-stems-", ".tmp");
			Path backup = nextBackupPath(world);
			boolean replaced = false;
			try {
				NbtIo.writeCompressed(root, temporary.toFile());
				forceFile(temporary);
				verifyDimensionKeys(temporary, expected);

				Files.copy(levelDat, backup);
				forceFile(backup);
				if (Files.mismatch(levelDat, backup) != -1L)
					throw new IOException("The level.dat backup did not match the original byte-for-byte: " + backup);

				try {
					Files.move(temporary, levelDat, StandardCopyOption.ATOMIC_MOVE,
							StandardCopyOption.REPLACE_EXISTING);
					replaced = true;
				} catch (AtomicMoveNotSupportedException exception) {
					throw new IOException("The filesystem does not support an atomic level.dat replacement. "
							+ "The original remains in place and the backup is " + backup, exception);
				}

				verifyDimensionKeys(levelDat, expected);
				System.out.println("Migration complete. Saved LevelStems after migration: " + expected.size());
				System.out.println("Backup: " + backup);
				System.out.println("Dimension folders were preserved and were not opened or modified.");
			} finally {
				Files.deleteIfExists(temporary);
				if (!replaced && Files.exists(backup))
					System.err.println("level.dat was not replaced; the safety backup remains at " + backup);
			}
		}
	}

	private static CompoundTag dimensionsTag(CompoundTag root) throws IOException {
		if (!root.contains("Data", Tag.TAG_COMPOUND))
			throw new IOException("level.dat is missing the Data compound.");
		CompoundTag data = root.getCompound("Data");
		if (!data.contains("WorldGenSettings", Tag.TAG_COMPOUND))
			throw new IOException("level.dat is missing Data.WorldGenSettings.");
		CompoundTag worldGenSettings = data.getCompound("WorldGenSettings");
		if (!worldGenSettings.contains("dimensions", Tag.TAG_COMPOUND))
			throw new IOException("level.dat is missing Data.WorldGenSettings.dimensions.");
		return worldGenSettings.getCompound("dimensions");
	}

	private static void verifyDimensionKeys(Path levelDat, Set<String> expected) throws IOException {
		CompoundTag check = NbtIo.readCompressed(levelDat.toFile());
		Set<String> actual = new TreeSet<>(dimensionsTag(check).getAllKeys());
		if (!actual.equals(expected))
			throw new IOException("Verification failed for " + levelDat + "; expected "
					+ expected.size() + " LevelStem keys but found " + actual.size() + '.');
	}

	private static Path nextBackupPath(Path world) {
		String timestamp = BACKUP_TIME.format(Instant.now());
		Path candidate = world.resolve("level.dat.before-shared-realms-" + timestamp + ".bak");
		for (int suffix = 1; Files.exists(candidate); suffix++)
			candidate = world.resolve("level.dat.before-shared-realms-" + timestamp + '-' + suffix + ".bak");
		return candidate;
	}

	private static void forceFile(Path path) throws IOException {
		try (FileChannel channel = FileChannel.open(path, StandardOpenOption.WRITE)) {
			channel.force(true);
		}
	}

	private static HeldSessionLock acquireSessionLock(Path world) throws IOException {
		Path lockPath = world.resolve("session.lock");
		if (Files.isSymbolicLink(lockPath)
				|| !Files.isRegularFile(lockPath, LinkOption.NOFOLLOW_LINKS))
			throw new IOException("Missing, non-regular, or symbolic-link session.lock; refusing to touch the world: "
					+ lockPath);

		FileChannel channel;
		try {
			channel = FileChannel.open(lockPath, StandardOpenOption.READ, StandardOpenOption.WRITE);
		} catch (IOException exception) {
			throw new IOException("Could not open session.lock. The world may be running or inaccessible: "
					+ lockPath, exception);
		}
		try {
			FileLock lock = channel.tryLock();
			if (lock == null) {
				channel.close();
				throw new IOException("session.lock is active; stop Minecraft/server before migrating: " + lockPath);
			}
			System.out.println("Acquired offline world lock: " + lockPath);
			return new HeldSessionLock(channel, lock);
		} catch (OverlappingFileLockException exception) {
			channel.close();
			throw new IOException("session.lock is active in this JVM; stop the world before migrating: "
					+ lockPath, exception);
		} catch (IOException exception) {
			channel.close();
			throw exception;
		}
	}

	private static Set<String> retiredLevelStems() {
		Set<String> keys = new LinkedHashSet<>();
		for (int floor = 2; floor <= 20; floor++)
			keys.add(String.format(Locale.ROOT, "sololeveling:dungeon_dimension_dkc_f%02d", floor));
		for (String territory : List.of("beginning", "destruction", "fangs", "frost", "iron_body",
				"plagues", "transfiguration", "white_flames"))
			keys.add("sololeveling:monarch_territory_" + territory);
		return Collections.unmodifiableSet(keys);
	}

	private record HeldSessionLock(FileChannel channel, FileLock lock) implements AutoCloseable {
		@Override
		public void close() throws IOException {
			List<IOException> failures = new ArrayList<>();
			try {
				lock.release();
			} catch (IOException exception) {
				failures.add(exception);
			}
			try {
				channel.close();
			} catch (IOException exception) {
				failures.add(exception);
			}
			if (!failures.isEmpty()) {
				IOException combined = new IOException("Could not release the world session lock.");
				failures.forEach(combined::addSuppressed);
				throw combined;
			}
		}
	}
}
