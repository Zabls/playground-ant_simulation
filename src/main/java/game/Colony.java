package game;

import config.CanvasSetting;
import config.ColonySetting;
import util.Position;

import java.util.ArrayList;


public class Colony {
    //region Fields
    public final Position NEST = new Position((int) (Math.random() * CanvasSetting.canvasSizeX), (int) (Math.random() * CanvasSetting.canvasSizeY));
    public final int[][] TRAILS_TO_NEST = new int[CanvasSetting.canvasSizeX][CanvasSetting.canvasSizeY];
    public final int[][] TRAILS_TO_FOOD = new int[CanvasSetting.canvasSizeX][CanvasSetting.canvasSizeY];
    public boolean[][] isAnt = new boolean[CanvasSetting.canvasSizeX][CanvasSetting.canvasSizeY];
    private final ArrayList<Ant> ANTS = new ArrayList<>();

    private int returnedFood;

    private final ColonySetting SETTING;
    //endregion

    //region Constructors
    public Colony(ColonySetting SETTING) {
        this.SETTING = SETTING;

        createAnts();
    }
    //endregion

    //region Getters & Setters
    public ColonySetting getSETTING() {
        return SETTING;
    }

    public int getNumberOfAnts() {
        return ANTS.size();
    }
    //endregion

    /**
     Create Ants at start of main.Simulation.
     */
    private void createAnts() {
        for (int i = 0; i < SETTING.antsAtStart; i++) {
            ANTS.add(new Ant(this, NEST.x, NEST.y));
        }
        isAnt[NEST.x][NEST.y] = true;
    }

    /**
     Calculate direction and move all ants.
     */
    public void move() {
        isAnt = new boolean[CanvasSetting.canvasSizeX][CanvasSetting.canvasSizeY];
        returnedFood = 0;

        ANTS.parallelStream().forEach(ant -> {
            boolean wasSearchingNest = !ant.isSearchingForFood();

            ant.calculateDirection();
            ant.executeMove();

            ant.lifetime++;
            ant.timeAwayFromNest++;
            // Ant returned food
            if (wasSearchingNest && ant.isSearchingForFood()) {
                ant.timeAwayFromNest = 0;
                returnedFood++;
            }
        });
    }

    /**
     Reduce all existing Trail by {@link #SETTING COLONY_TRAIL_REDUCTION} and leave new Trails with strength {@link #SETTING COLONY_TRAIL_STRENGTH}
     */
    public void manageTrails() {
        reduceTrails();

        ANTS.parallelStream().forEach(ant -> {
            if (ant.isSearchingForFood()) {
                ant.leaveTrail(TRAILS_TO_NEST);
            } else {
                ant.leaveTrail(TRAILS_TO_FOOD);
            }
        });
    }

    /**
     Kill old ants or ants which where to long away from their nest.
     */
    public void killOldAnts() {
        ANTS.removeIf(ant -> ant.lifetime > ant.getMaxLifetime() || ant.timeAwayFromNest > ant.getMaxTimeAwayFromNest());
    }

    /**
     Add new ants depending on food returned to nest. Has a chance of {@link #SETTING NEW_ANTS_PER_RETURNED_FOOD} to spawn ant per food.
     */
    public void spawnNewAnts() {
        for (int i = 0; i < returnedFood; i++) {
            if (Math.random() < SETTING.newAntsPerReturnedFood) {
                ANTS.add(new Ant(this, NEST.x, NEST.y));
            }
        }
    }

    /**
     Reduce every Trail in canvas by {@link #SETTING TRAIL_REDUCTION}
     */
    private void reduceTrails() {
        for (int x = 0; x < CanvasSetting.canvasSizeX; x++) {
            for (int y = 0; y < CanvasSetting.canvasSizeY; y++) {
                TRAILS_TO_FOOD[x][y] = (int) Math.max(0, TRAILS_TO_FOOD[x][y] - SETTING.trailReduction);
                TRAILS_TO_NEST[x][y] = (int) Math.max(0, TRAILS_TO_NEST[x][y] - SETTING.trailReduction);
            }
        }
    }
}
