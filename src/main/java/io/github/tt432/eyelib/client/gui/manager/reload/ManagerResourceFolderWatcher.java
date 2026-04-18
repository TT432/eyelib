package io.github.tt432.eyelib.client.gui.manager.reload;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.monitor.FileAlterationListenerAdaptor;
import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.apache.commons.io.monitor.FileAlterationObserver;
import org.jspecify.annotations.Nullable;

import java.io.File;
import java.nio.file.Path;
import java.util.function.Consumer;

@Slf4j
public final class ManagerResourceFolderWatcher {
    @Nullable
    private FileAlterationMonitor fileMonitor;
    @Nullable
    private FileAlterationObserver fileObserver;

    public void start(Path basePath, Consumer<Path> onFileUpsert) {
        stop();

        fileObserver = new FileAlterationObserver(basePath.toFile());
        fileObserver.addListener(new FileAlterationListenerAdaptor() {
            @Override
            public void onFileCreate(File file) {
                onFileUpsert.accept(file.toPath());
            }

            @Override
            public void onFileChange(File file) {
                onFileUpsert.accept(file.toPath());
            }

            @Override
            public void onFileDelete(File file) {
                log.info("file deleted: {}", file.getAbsolutePath());
            }
        });

        fileMonitor = new FileAlterationMonitor(1000L, fileObserver);
        try {
            fileMonitor.start();
        } catch (Exception e) {
            log.error("Failed to start file monitor.", e);
        }
    }

    public void stop() {
        if (fileMonitor != null) {
            try {
                fileMonitor.stop();
            } catch (Exception e) {
                log.error("Failed to stop file monitor.", e);
            }
            fileMonitor = null;
        }

        fileObserver = null;
    }
}

