package de.tehmanu.skybad.tasks;

import de.tehmanu.skybad.SkyBadBot;

import java.io.IOException;
import java.nio.file.*;

/**
 * @author TehManu
 * @since 20.10.2024
 */
public class FileChangeTask implements Runnable {

    @Override
    public void run() {
        try (WatchService watchService = FileSystems.getDefault().newWatchService()) {
            Path path = Paths.get("src/main/resources");
            path.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);

            SkyBadBot.getLogger().debug("Watching files in directory: {}", path);

            while (true) {
                try {
                    WatchKey key = watchService.take();

                    for (WatchEvent<?> event : key.pollEvents()) {
                        WatchEvent.Kind<?> kind = event.kind();
                        if (kind == StandardWatchEventKinds.OVERFLOW) {
                            continue;
                        }
                        WatchEvent<Path> watchEvent = (WatchEvent<Path>) event;
                        Path file = watchEvent.context();
                        SkyBadBot.getLogger().debug("File change detect: " + file);
                        SkyBadBot.getInstance().getApplicationManager().loadAvailableJobs();
                    }
                    boolean valid = key.reset();
                    if (!valid) {
                        break;
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

        } catch (IOException exception) {
            SkyBadBot.getLogger().warn(exception.getMessage(), exception);
        }
    }
}
