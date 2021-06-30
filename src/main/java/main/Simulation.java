package main;

import config.CanvasSetting;
import config.ColonySetting;
import config.ConfigLoader;
import config.FileWatcher;
import game.Canvas;
import org.jetbrains.annotations.NotNull;
import util.StdDraw;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Simulation {
    public static final String PATH_TO_CONFIGS = "./configs/";

    public static void main(String[] args) throws IOException {
        ConfigLoader.load(CanvasSetting.class, new File(PATH_TO_CONFIGS + "simulation.config"));

        HashMap<File, List<ColonySetting>> map = getColonySettings();

        ArrayList<ColonySetting> colonyList = new ArrayList<>();
        map.values().forEach(colonyList::addAll);
        Canvas canvas = new Canvas(colonyList);

        FileWatcher fileWatcher = startConfigWatcher(map, canvas);

        initStdDraw();

        while (canvas.getNumberOfColonies() > 0) {
            canvas.update();
            canvas.draw();
        }

        fileWatcher.stop();
    }

    @NotNull
    private static HashMap<File, List<ColonySetting>> getColonySettings() {
        List<File> fileList = getConfigsSortedByNumber();

        HashMap<File, List<ColonySetting>> map = new HashMap<>();
        for (int i = 0; i < CanvasSetting.numberOfColoniesAtStart; i++) {
            ColonySetting colonySetting = new ColonySetting();
            final File file = fileList.get(i < fileList.size() ? i : 0);

            ConfigLoader.load(colonySetting, file);

            if (!map.containsKey(file)) {
                map.put(file, new ArrayList<>());
            }
            map.get(file).add(colonySetting);
        }
        return map;
    }

    @NotNull
    private static FileWatcher startConfigWatcher(HashMap<File, List<ColonySetting>> map, Canvas canvas) throws IOException {
        FileWatcher fileWatcher = new FileWatcher(Paths.get(PATH_TO_CONFIGS), canvas, map);
        fileWatcher.start();
        return fileWatcher;
    }

    @NotNull
    private static List<File> getConfigsSortedByNumber() {
        final Predicate<File> isColonyConfig = file -> file.isFile() && FileWatcher.COLONY_FILENAME.matcher(file.getName()).matches();

        Stream<File> files = Arrays.stream(Objects.requireNonNull(new File(PATH_TO_CONFIGS).listFiles())).filter(isColonyConfig);

        final Comparator<File> sortByConfigNumber = Comparator.comparingInt(a -> Integer.parseInt(a.getName().replaceAll("[^0-9]", "")));
        return files.sorted(sortByConfigNumber).collect(Collectors.toList());
    }

    private static void initStdDraw() {
        StdDraw.setCanvasSize(950, 950);
        StdDraw.setXscale(0, CanvasSetting.canvasSizeX);
        StdDraw.setYscale(0, CanvasSetting.canvasSizeY);
        StdDraw.enableDoubleBuffering();
        StdDraw.clear(StdDraw.BLACK);
    }
}
