package io.github.tt432.eyelib.client.instrument.collector;

import io.github.tt432.eyelibmolang.compiler.cache.MolangDiskCache;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

public final class MolangDiskCacheObserver implements CacheSizeObserver {
    private static final Logger LOG = Logger.getLogger(MolangDiskCacheObserver.class.getName());
    private final MolangDiskCache diskCache;

    public MolangDiskCacheObserver(MolangDiskCache diskCache) {
        this.diskCache = diskCache;
    }

    @Override
    public String source() { return "MolangDiskCache"; }

    @Override
    public String metricName() { return "disk_cache_files"; }

    @Override
    public int currentSize() {
        Path dir = diskCache.getCacheDirectory();
        if (!Files.exists(dir)) return 0;
        try (Stream<Path> files = Files.list(dir)) {
            return (int) files.filter(Files::isRegularFile).count();
        } catch (IOException e) {
            LOG.log(Level.FINE, "Failed to list disk cache directory: " + dir, e);
            return 0;
        }
    }

    @Override
    public String metricUnit() { return "files"; }
}
