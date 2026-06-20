package io.github.tt432.eyelib.client.gui.manager.reload;

import com.mojang.blaze3d.systems.RenderSystem;
import io.github.tt432.eyelib.client.gui.manager.io.FileDialogService;
import lombok.Getter;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.nio.file.Path;

/**
 * @author TT432
 */
public final class ManagerFolderSession {
    private final ManagerResourceFolderWatcher folderWatcher = new ManagerResourceFolderWatcher();
    private boolean monitoredFolderUsesAddonBridge;

    @Getter
    @Nullable
    private Path monitoredFolderPath;

    @Getter
    @Nullable
    private String monitoredFolderPathText;

    public void chooseFolder(Logger logger) {
        FileDialogService.selectFolder("打开资源包文件夹", Path.of("/")).whenComplete((path, throwable) ->
                                                                                               runRenderCall(() -> path.ifPresent(selected -> bindFolder(selected.toAbsolutePath(), logger))));
    }

    private void bindFolder(Path absolutePath, Logger logger) {
        if (absolutePath.equals(monitoredFolderPath)) {
            return;
        }

        stop();

        monitoredFolderPath = absolutePath;
        monitoredFolderPathText = absolutePath.toString();
        monitoredFolderUsesAddonBridge = ManagerResourceImportPlanner.loadResourceFolder(absolutePath, logger);
        folderWatcher.start(absolutePath, changedPath -> runRenderCall(() -> {
            if (monitoredFolderPath != null) {
                if (monitoredFolderUsesAddonBridge) {
                    monitoredFolderUsesAddonBridge = ManagerResourceImportPlanner.loadResourceFolder(monitoredFolderPath, logger);
                } else {
                    ManagerResourceImportPlanner.loadSingleFile(monitoredFolderPath, changedPath, logger);
                }
            }
        }));
    }

    private void runRenderCall(Runnable runnable) {
        //? if <26.1 {
        RenderSystem.recordRenderCall(runnable::run);
        //?} else {
        throw new UnsupportedOperationException("26.1 migration");
        //?}
    }

    public void stop() {
        folderWatcher.stop();
        monitoredFolderUsesAddonBridge = false;
    }
}
