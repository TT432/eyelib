package io.github.tt432.eyelibmolang.compiler.cache;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.CRC32;

/**
 * Disk persistence for compiled Molang bytecode cache entries.
 *
 * <p>Byte layout (all integers are 4 bytes, big-endian):
 * <pre>
 * Offset  Size      Field
 * 0       4         Magic number: 0x4D4F4C43 ("MOLC")
 * 4       4         Compiler version (integer)
 * 8       4         Registry version ref length (integer)
 * 12      N         Registry version ref (UTF-8 bytes)
 * 12+N    4         Source expression length (integer)
 * 16+N    M         Source expression (UTF-8 bytes)
 * 16+N+M  4         Class bytes length (integer)
 * 20+N+M  C         Class bytes (raw JVM .class file)
 * 20+N+M+C 4        CRC32 checksum (over bytes 0..20+N+M+C-1)
 * </pre>
 */
public final class MolangDiskCache {
    private static final Logger LOG = Logger.getLogger(MolangDiskCache.class.getName());

    private static final int MAGIC = 0x4D4F4C43;
    private static final String FILE_EXTENSION = ".molcache";
    private static final String EMPTY_SOURCE_PREFIX = "_empty_";
    private static final int MAX_PREFIX_LENGTH = 64;

    private final Path cacheDirectory;

    public MolangDiskCache(Path cacheDirectory) {
        this.cacheDirectory = Objects.requireNonNull(cacheDirectory, "cacheDirectory");
    }

    public void write(byte[] classBytes, String sourceExpression, String registryVersionRef, int compilerVersion) throws IOException {
        Objects.requireNonNull(classBytes, "classBytes");
        Objects.requireNonNull(sourceExpression, "sourceExpression");
        Objects.requireNonNull(registryVersionRef, "registryVersionRef");

        Files.createDirectories(cacheDirectory);

        String fileName = computeFileName(sourceExpression) + FILE_EXTENSION;
        Path finalFile = cacheDirectory.resolve(fileName);
        Path tempFile = cacheDirectory.resolve(fileName + ".tmp." + UUID.randomUUID());

        boolean writeSucceeded = false;
        try {
            byte[] registryBytes = registryVersionRef.getBytes(StandardCharsets.UTF_8);
            byte[] sourceBytes = sourceExpression.getBytes(StandardCharsets.UTF_8);

            try (FileOutputStream fos = new FileOutputStream(tempFile.toFile());
                 DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(fos))) {
                dos.writeInt(MAGIC);
                dos.writeInt(compilerVersion);
                dos.writeInt(registryBytes.length);
                dos.write(registryBytes);
                dos.writeInt(sourceBytes.length);
                dos.write(sourceBytes);
                dos.writeInt(classBytes.length);
                dos.write(classBytes);
                dos.flush();
                fos.getFD().sync();
            }

            int checksum = computeCrc32ForPrefix(tempFile, Files.size(tempFile));

            try (FileOutputStream fos = new FileOutputStream(tempFile.toFile(), true);
                 DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(fos))) {
                dos.writeInt(checksum);
                dos.flush();
                fos.getFD().sync();
            }

            try (FileChannel channel = FileChannel.open(tempFile, StandardOpenOption.WRITE)) {
                channel.force(true);
            }

            try {
                Files.move(tempFile, finalFile, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                LOG.log(Level.WARNING, "Atomic move unsupported for cache entry: {0}; falling back to non-atomic move", fileName);
                Files.move(tempFile, finalFile, StandardCopyOption.REPLACE_EXISTING);
            }

            LOG.fine("Written cache entry: " + fileName);
            writeSucceeded = true;
        } finally {
            if (!writeSucceeded && Files.exists(tempFile)) {
                try {
                    Files.delete(tempFile);
                } catch (IOException cleanupError) {
                    LOG.log(Level.WARNING, "Failed to delete temp cache file: " + tempFile, cleanupError);
                }
            }
        }
    }

    public byte[] read(String sourceExpression, String registryVersionRef, int compilerVersion) throws IOException {
        Objects.requireNonNull(sourceExpression, "sourceExpression");
        Objects.requireNonNull(registryVersionRef, "registryVersionRef");

        String fileName = computeFileName(sourceExpression) + FILE_EXTENSION;
        Path finalFile = cacheDirectory.resolve(fileName);
        if (!Files.exists(finalFile)) {
            return null;
        }

        try {
            byte[] classBytes;

            try (DataInputStream dis = new DataInputStream(new BufferedInputStream(Files.newInputStream(finalFile)))) {
                int magic = dis.readInt();
                if (magic != MAGIC) {
                    return null;
                }

                int fileCompilerVersion = dis.readInt();
                if (fileCompilerVersion != compilerVersion) {
                    return null;
                }

                int registryLength = dis.readInt();
                if (registryLength < 0) {
                    return null;
                }
                byte[] registryBytes = dis.readNBytes(registryLength);
                if (registryBytes.length != registryLength) {
                    return null;
                }
                String fileRegistryVersionRef = new String(registryBytes, StandardCharsets.UTF_8);
                if (!registryVersionRef.equals(fileRegistryVersionRef)) {
                    return null;
                }

                int sourceLength = dis.readInt();
                if (sourceLength < 0) {
                    return null;
                }
                byte[] sourceBytes = dis.readNBytes(sourceLength);
                if (sourceBytes.length != sourceLength) {
                    return null;
                }
                String fileSourceExpression = new String(sourceBytes, StandardCharsets.UTF_8);
                if (!sourceExpression.equals(fileSourceExpression)) {
                    LOG.log(
                            Level.WARNING,
                            "Source expression mismatch for cache entry {0}; expected key source but file contains different source",
                            fileName
                    );
                }

                int classBytesLength = dis.readInt();
                if (classBytesLength < 0) {
                    return null;
                }

                classBytes = new byte[classBytesLength];
                dis.readFully(classBytes);

                dis.readInt();
            }

            if (!isChecksumValid(finalFile)) {
                LOG.log(Level.WARNING, "CRC32 mismatch for cache entry: {0}", fileName);
                return null;
            }

            return classBytes;
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Failed to read cache entry: " + finalFile, e);
            return null;
        }
    }

    public static String computeFileName(String sourceExpression) {
        Objects.requireNonNull(sourceExpression, "sourceExpression");

        String prefix;
        if (sourceExpression.isEmpty()) {
            prefix = EMPTY_SOURCE_PREFIX;
        } else {
            StringBuilder sb = new StringBuilder(MAX_PREFIX_LENGTH);
            int end = Math.min(sourceExpression.length(), MAX_PREFIX_LENGTH);
            for (int i = 0; i < end; i++) {
                char c = sourceExpression.charAt(i);
                sb.append(Character.isLetterOrDigit(c) ? c : '_');
            }
            prefix = sb.toString();
            if (prefix.isEmpty()) {
                prefix = EMPTY_SOURCE_PREFIX;
            }
        }

        String hashSuffix = String.format("%08x", sourceExpression.hashCode());
        return prefix + "-" + hashSuffix;
    }

    private static int computeCrc32ForPrefix(Path file, long length) throws IOException {
        CRC32 crc32 = new CRC32();
        try (FileChannel channel = FileChannel.open(file, StandardOpenOption.READ)) {
            ByteBuffer buffer = ByteBuffer.allocate(8192);
            long remaining = length;

            while (remaining > 0) {
                buffer.clear();
                int maxRead = (int) Math.min(buffer.capacity(), remaining);
                buffer.limit(maxRead);
                int read = channel.read(buffer);
                if (read <= 0) {
                    break;
                }
                buffer.flip();
                crc32.update(buffer);
                remaining -= read;
            }
        }
        return (int) crc32.getValue();
    }

    private static boolean isChecksumValid(Path file) throws IOException {
        byte[] allBytes = Files.readAllBytes(file);
        if (allBytes.length < Integer.BYTES * 2) {
            return false;
        }

        int checksumOffset = allBytes.length - Integer.BYTES;
        CRC32 crc32 = new CRC32();
        crc32.update(allBytes, 0, checksumOffset);
        int computedChecksum = (int) crc32.getValue();
        int expectedChecksum = ByteBuffer.wrap(allBytes, checksumOffset, Integer.BYTES).getInt();
        return computedChecksum == expectedChecksum;
    }
}
