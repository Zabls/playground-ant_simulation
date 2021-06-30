package config;

import game.Canvas;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;

import static java.nio.file.StandardWatchEventKinds.*;
import static main.Simulation.PATH_TO_CONFIGS;

public class FileWatcher {
    public static final Pattern COLONY_FILENAME = Pattern.compile("colony[0-9]+\\.config");
    public static final String SIMULATION_FILENAME = "simulation.config";
    private Thread thread;
    private final WatchService watchService;
    private final HashMap<File, List<ColonySetting>> PATH_SETTING_MAP;

    private final Canvas CANVAS;

    public FileWatcher(Path directory, Canvas canvas, HashMap<File, List<ColonySetting>> PATH_SETTING_MAP) throws
            IOException {
        this.PATH_SETTING_MAP = PATH_SETTING_MAP;
        this.CANVAS = canvas;

        this.watchService = FileSystems.getDefault().newWatchService();
        directory.register(watchService, ENTRY_MODIFY, ENTRY_CREATE, ENTRY_DELETE);
    }

    /**
     Start FileWatcher in new thread.
     */
    public void start() {
        thread = new Thread(() -> {
            while (true) {
                processEvents();
            }
        });
        thread.setPriority(Thread.MAX_PRIORITY);
        thread.start();
    }

    /**
     Check for file changes.
     */
    private void processEvents() {
        final WatchKey wk;
        try {
            wk = watchService.take();
            Thread.sleep(500); // give a chance for duplicate events to pile up
        } catch (InterruptedException e) {
            System.out.println("Interrupted");
            return;
        }

        for (WatchEvent<?> event : wk.pollEvents()) {
            WatchEvent.Kind<?> kind = event.kind();
            if (kind == OVERFLOW) { continue; }

            @SuppressWarnings("unchecked") WatchEvent<Path> ev = (WatchEvent<Path>) event;
            Path filename = ev.context();
            File file = new File(PATH_TO_CONFIGS + filename.toString());

            if (kind.equals(ENTRY_MODIFY)) {
                handleFileModification(file);
            } else if (kind.equals(ENTRY_CREATE)) {
                handleFileCreation(file);
            } else if (kind.equals(ENTRY_DELETE)) {
                handleFileDeletion(file);
            } else {
                System.out.println(kind.name() + ": " + file);
            }
        }
        wk.reset();
    }

    /**
     Delete all colonies associated with deleted file

     @param file Path of deleted file
     */
    private void handleFileDeletion(File file) {
        System.out.println("Deleted: " + file);

        if (isColonyConfig(file)) {
            PATH_SETTING_MAP.get(file).forEach(colonySetting -> CANVAS.deleteColony(colonySetting.name));
            PATH_SETTING_MAP.remove(file);
        }
    }

    /**
     Creates a new colony if the file matches {@link #COLONY_FILENAME}

     @param file Path of created file
     */
    private void handleFileCreation(File file) {
        System.out.println("Created: " + file);

        if (isColonyConfig(file)) {
            createNewColony(file);
        }
    }

    /**
     Update Canvas if simulation.config is changed. Updates Colonies if file is known and creates new colonies if
     colonies config is unknown.

     @param file Path of modified file
     */
    private void handleFileModification(File file) {
        System.out.println("Modified: " + file);

        //Updates Canvas
        if (SIMULATION_FILENAME.equals(file.getName())) {
            ConfigLoader.load(CanvasSetting.class, file);
        } else {
            //Updates Colony if it exists already
            if (PATH_SETTING_MAP.containsKey(file)) {
                PATH_SETTING_MAP.get(file).forEach(colonySetting -> ConfigLoader.load(colonySetting, file));
                //Creates new colony if filename matches pattern
            } else if (isColonyConfig(file)) {
                createNewColony(file);
            }
        }
    }

    private boolean isColonyConfig(File file) {
        return COLONY_FILENAME.matcher(file.getName()).matches();
    }

    private void createNewColony(File file) {
        try {
            ColonySetting colonySetting = new ColonySetting();
            ConfigLoader.load(colonySetting, file);
            CANVAS.createColony(colonySetting);

            if (!PATH_SETTING_MAP.containsKey(file)) {
                PATH_SETTING_MAP.put(file, new ArrayList<>());
            }
            PATH_SETTING_MAP.get(file).add(colonySetting);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Unable to create new colony!");
        }
    }

    /**
     Interrupts thread and closes {@link #watchService}
     */
    public void stop() {
        thread.interrupt();
        try {
            watchService.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
