package io.github.tt432.eyelib.client.gui.manager.reload;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.monitor.FileAlterationListenerAdaptor;
import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.apache.commons.io.monitor.FileAlterationObserver;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Path;
import java.util.function.Consumer;

/** @author TT432 */
@Slf4j
public final class ManagerResourceFolderWatcher {
    private static final Logger LOGGER = LoggerFactory.getLogger(ManagerResourceFolderWatcher.class);

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
                onFileUpsert.accept(file.toPath());
            }
        });

        fileMonitor = new FileAlterationMonitor(1000L, fileObserver);
        try {
            fileMonitor.start();
        } catch (Exception e) {
            LOGGER.error("Failed to start file monitor.", e);
        }
    }

    public void stop() {
        if (fileMonitor != null) {
            try {
                fileMonitor.stop();
            } catch (Exception e) {
                LOGGER.error("Failed to stop file monitor.", e);
            }
            fileMonitor = null;
        }

        fileObserver = null;
    }
}
