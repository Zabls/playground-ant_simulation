package game;

import config.CanvasSetting;
import config.ColonySetting;
import util.StdDraw;
import util.Vector2;

import java.awt.*;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Canvas {
    //region Fields
    private static final int CANVAS_X = CanvasSetting.canvasSizeX;
    private static final int CANVAS_Y = CanvasSetting.canvasSizeY;
    public static final boolean[][] IS_FOOD = new boolean[CANVAS_X][CANVAS_Y];

    private static final int NUMBER_OF_FOOD_SPAWNS = CanvasSetting.foodSpawnsAtStart;

    private static final int NUMBER_OF_COLONIES_AT_START = CanvasSetting.numberOfColoniesAtStart;
    private final ConcurrentLinkedQueue<Colony> COLONIES = new ConcurrentLinkedQueue<>();
    //endregion

    //region Constructors
    public Canvas(ArrayList<ColonySetting> colonySettings) {
        createColonies(colonySettings);
        for (int i = 0; i < NUMBER_OF_FOOD_SPAWNS; i++) {
            createFood();
        }
    }
    //endregion

    //region Getters & Setters
    public int getNumberOfColonies() {
        return COLONIES.size();
    }
    //endregion


    //region Methods

    public void createColony(ColonySetting colonySetting) {
        COLONIES.add(new Colony(colonySetting));
    }

    public void deleteColony(String name) {
        COLONIES.removeIf(colony -> colony.getSETTING().name.equals(name));
    }

    /**
     Create Colonies at start of main.Simulation.
     */
    private void createColonies(ArrayList<ColonySetting> colonySettings) {
        for (int i = 0; i < NUMBER_OF_COLONIES_AT_START; i++) {
            createColony(colonySettings.get(i));
        }
    }

    /**
     Create Food with Radius depending on canvas size.
     */
    private void createFood() {
        final int RADIUS = (int) (CANVAS_X * CanvasSetting.radiusOfFoodComparedToCanvasSize * (Math.random() + 0.5)) / 2;

        final int randomX = (new Random().nextInt(CANVAS_X - 2 * RADIUS)) + RADIUS;
        final int randomY = (new Random().nextInt(CANVAS_Y - 2 * RADIUS)) + RADIUS;

        for (int x = -RADIUS; x <= RADIUS; x++) {
            for (int y = -RADIUS; y <= RADIUS; y++) {
                if (new Vector2(x, y).length() > RADIUS) { continue; }
                IS_FOOD[randomX + x][randomY + y] = true;

            }
        }
    }

    /**
     Spawn Food with percentage {@link CanvasSetting#likelihoodOfFoodSpawnPerIteration} and move all ants.
     */
    public void update() {
        if (Math.random() < CanvasSetting.likelihoodOfFoodSpawnPerIteration) {
            createFood();
        }


        COLONIES.parallelStream().forEach(colony -> {
            colony.move();
            colony.manageTrails();
            colony.killOldAnts();
            colony.spawnNewAnts();
        });

        COLONIES.removeIf(colony -> colony.getNumberOfAnts() == 0);
    }

    /**
     Clear whole canvas. Then loop through every pixel in canvas and draw it as food, ant or trail.
     */
    public void draw() {
        StdDraw.clear(StdDraw.BLACK);

        for (int x = 0; x < CANVAS_X; x++) {
            for (int y = 0; y < CANVAS_Y; y++) {
                if (IS_FOOD[x][y]) {
                    StdDraw.setPenColor(StdDraw.GREEN);
                    drawPixel(x, y);

                } else {
                    for (Colony colony : COLONIES) {
                        if (colony.isAnt[x][y]) {
                            StdDraw.setPenColor(colony.getSETTING().antColor);
                            drawPixel(x, y);
                        } else if (colony.TRAILS_TO_FOOD[x][y] != 0) {
                            Color colonyColor = colony.getSETTING().trailColorToFood;
                            Color newColor = new Color((float) colonyColor.getRed() / 255, (float) colonyColor.getGreen() / 255,
                                                       (float) colonyColor.getBlue() / 255,
                                                       (float) colony.TRAILS_TO_FOOD[x][y] / colony.getSETTING().maxTrailStrength);
                            StdDraw.setPenColor(newColor);
                            drawPixel(x, y);
                        } else if (colony.TRAILS_TO_NEST[x][y] != 0) {
                            Color colonyColor = colony.getSETTING().trailColorToNest;
                            Color newColor = new Color((float) colonyColor.getRed() / 255, (float) colonyColor.getGreen() / 255,
                                                       (float) colonyColor.getBlue() / 255,
                                                       (float) colony.TRAILS_TO_NEST[x][y] / colony.getSETTING().maxTrailStrength);
                            StdDraw.setPenColor(newColor);
                            drawPixel(x, y);
                        }
                    }
                }
            }
        }

        for (Colony colony : COLONIES) {
            StdDraw.setPenColor(colony.getSETTING().antColor);
            StdDraw.filledCircle(colony.NEST.x, colony.NEST.y, 5);
        }
        StdDraw.show();
    }

    private void drawPixel(int x, int y) {
        StdDraw.filledSquare(x, y, 0.5);
    }
    //endregion
}
