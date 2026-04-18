package io.github.tt432.eyelib.client.gui.manager.io;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class FileDialogService {
    private static final ExecutorService FILE_DIALOG_EXECUTOR = Executors.newSingleThreadExecutor();
    @Nullable
    private static String lastPath;

    public static CompletableFuture<Optional<Path>> selectJsonFile(String title, @Nullable Path origin) {
        return openFile(title, origin, "读取 json", "*.json");
    }

    public static CompletableFuture<Optional<Path>> openFile(String title, @Nullable Path origin, @Nullable String filterLabel, String... filters) {
        CompletableFuture<Optional<Path>> future = new CompletableFuture<>();

        FILE_DIALOG_EXECUTOR.submit(() -> {
            PointerBuffer filterBuffer = MemoryUtil.memAllocPointer(filters.length);
            ByteBuffer[] utfBuffers = new ByteBuffer[filters.length];

            try {
                for (int index = 0; index < filters.length; index++) {
                    utfBuffers[index] = MemoryUtil.memUTF8(filters[index]);
                    filterBuffer.put(utfBuffers[index]);
                }
                filterBuffer.flip();

                String path = origin != null ? origin.toAbsolutePath().toString() : null;
                String result = TinyFileDialogs.tinyfd_openFileDialog(title, path, filterBuffer, filterLabel, false);
                future.complete(Optional.ofNullable(result).map(Paths::get));
            } finally {
                for (ByteBuffer buffer : utfBuffers) {
                    if (buffer != null) {
                        MemoryUtil.memFree(buffer);
                    }
                }
                MemoryUtil.memFree(filterBuffer);
            }
        });

        return future;
    }

    public static CompletableFuture<Optional<Path>> selectFolder(String title, @Nullable Path origin) {
        CompletableFuture<Optional<Path>> future = new CompletableFuture<>();

        FILE_DIALOG_EXECUTOR.submit(() -> {
            String path = lastPath != null ? lastPath : (origin != null ? origin.toAbsolutePath().toString() : "");
            String result = TinyFileDialogs.tinyfd_selectFolderDialog(title, path);
            lastPath = result;
            future.complete(Optional.ofNullable(result).map(Paths::get));
        });

        return future;
    }
}

