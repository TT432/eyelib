package io.github.tt432.eyelib.client.gui.manager.reload;

import com.mojang.blaze3d.systems.RenderSystem;
import io.github.tt432.eyelib.client.gui.manager.io.FileDialogService;
import lombok.Getter;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.nio.file.Path;

public final class ManagerFolderSession {
    private final ManagerResourceFolderWatcher folderWatcher = new ManagerResourceFolderWatcher();

    @Getter
    @Nullable
    private Path monitoredFolderPath;

    @Getter
    @Nullable
    private String monitoredFolderPathText;

    public void chooseFolder(Logger logger) {
        FileDialogService.selectFolder("打开资源包文件夹", Path.of("/")).whenComplete((path, throwable) ->
                RenderSystem.recordRenderCall(() -> path.ifPresent(selected -> bindFolder(selected.toAbsolutePath(), logger))));
    }

    private void bindFolder(Path absolutePath, Logger logger) {
        if (absolutePath.equals(monitoredFolderPath)) {
            return;
        }

        stop();

        monitoredFolderPath = absolutePath;
        monitoredFolderPathText = absolutePath.toString();
        ManagerResourceImportPlanner.loadResourceFolder(absolutePath, logger);
        folderWatcher.start(absolutePath, changedPath -> RenderSystem.recordRenderCall(() -> {
            if (monitoredFolderPath != null) {
                ManagerResourceImportPlanner.loadSingleFile(monitoredFolderPath, changedPath, logger);
            }
        }));
    }

    public void stop() {
        folderWatcher.stop();
    }
}

