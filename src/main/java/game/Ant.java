package game;

import config.CanvasSetting;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import util.Position;
import util.Vector2;

import java.util.Random;

public class Ant {
    //region Fields
    private final Colony COLONY;
    private final Position POSITION;
    private Vector2 direction;
    private float currentRandomAngle = 0;
    private boolean isSearchingForFood = true;
    private Position nestOrFood;

    private final int MAX_LIFETIME;
    public int lifetime = 0;
    private final int MAX_TIME_AWAY_FROM_NEST;
    public int timeAwayFromNest = 0;

    private final Random RANDOM = new Random();
    //endregion

    //region Constructors
    public Ant(Colony colony, int positionX, int positionY) {
        this.COLONY = colony;
        this.POSITION = new Position(positionX, positionY);

        this.MAX_LIFETIME = colony.getSETTING().maxLifetimeFixed + RANDOM.nextInt(colony.getSETTING().maxLifetimeRandom);
        this.MAX_TIME_AWAY_FROM_NEST = colony.getSETTING().maxTimeAwayFromNestFixed + RANDOM.nextInt(colony.getSETTING().maxTimeAwayFromNestRandom);

        this.direction = new Vector2((float) RANDOM.nextDouble() - 0.5f, (float) RANDOM.nextDouble() - 0.5f);
        this.direction.normalize();
    }
    //endregion

    //region Getters & Setters
    public boolean isSearchingForFood() {
        return isSearchingForFood;
    }

    public boolean isSearchingForNest() {
        return !isSearchingForFood;
    }

    private boolean hasNotFoundNestOrFood() {
        return nestOrFood == null;
    }

    private boolean foundNestOrFood() {
        return nestOrFood != null;
    }

    public int getMaxLifetime() {
        return MAX_LIFETIME;
    }

    public int getMaxTimeAwayFromNest() {
        return MAX_TIME_AWAY_FROM_NEST;
    }

    private double getHalfSenseConeAngle() {
        return COLONY.getSETTING().halfSenseConeAngle;
    }

    private int getSenseRadius() {
        return COLONY.getSETTING().senseRadius;
    }

    private int getTrailStrength() {
        return COLONY.getSETTING().trailStrength;
    }

    private int getMaxTrailStrength() {
        return COLONY.getSETTING().maxTrailStrength;
    }

    private double getMaxRotation() {
        return COLONY.getSETTING().maxRotation;
    }

    private double getMaxRandomRotation() {
        return COLONY.getSETTING().maxRandomRotation;
    }
    //endregion

    //region Methods

    /**
     Scans for Food. If ant is on food set {@link #isSearchingForFood} to true and search for nest.
     */
    public void calculateDirection() {
        if (foundNestOrFood()) {
            if (isSearchingForNest() || Canvas.IS_FOOD[nestOrFood.x][nestOrFood.y]) {
                setDirectionToNestOrFood();
                return;
            }
            nestOrFood = null;
        }

        Vector2 tendTowards = loopThroughPoints();

        if (tendTowards == null) { return; }
        if (hasNotFoundNestOrFood()) { calculateNewDirection(tendTowards); }
    }

    /**
     Move game.Ant to new utils.Position. Change direction if game.Ant would be out of bounds.
     */
    public void executeMove() {
        Position moveTo = angleToPosition(direction.angle());

        //Resolve out of Bounds
        int rotationDir = RANDOM.nextBoolean() ? 1 : -1;
        while (isOutOfBounds(POSITION.x + moveTo.x, POSITION.y + moveTo.y)) {
            direction.rotate(90 * rotationDir);
            moveTo = angleToPosition(direction.angle());
        }

        POSITION.move(moveTo);
        COLONY.isAnt[POSITION.x][POSITION.y] = true;

        if (isSearchingForFood && Canvas.IS_FOOD[POSITION.x][POSITION.y]) {
            //If is on Food
            isSearchingForFood = false;
            nestOrFood = null;
            if (RANDOM.nextDouble() < CanvasSetting.likelihoodOfFoodDisappearingAfterBeingCollected) {
                Canvas.IS_FOOD[POSITION.x][POSITION.y] = false;
            }
            turnAround();
        } else if (!isSearchingForFood && COLONY.NEST.equals(POSITION)) {
            //If is on Nest
            isSearchingForFood = true;
            nestOrFood = null;
            turnAround();
        }
    }

    @Nullable
    private Vector2 loopThroughPoints() {
        int[] boundingBox = calculateBoundingBox();

        Vector2 tendTowards = new Vector2(0, 0);
        float[] distanceToClosestFood = new float[]{Float.MAX_VALUE};

        for (int x = boundingBox[0]; x <= boundingBox[2]; x++) {
            for (int y = boundingBox[1]; y <= boundingBox[3]; y++) {
                if (doesNotSensePixel(x, y)) { continue; }
                if (isOutOfBounds(x, y)) { continue; }

                if (isSearchingForFood) {
                    lookForFood(tendTowards, x, y, distanceToClosestFood);
                } else {
                    lookForNest(tendTowards, x, y);
                    if (foundNestOrFood()) { return null; }
                }
            }
        }
        return foundNestOrFood() ? null : tendTowards;
    }

    private int[] calculateBoundingBox() {
        int[] boundingBox;
        if (getHalfSenseConeAngle() < 90) {
            //Calculate Points of triangle in which ant senses.
            Vector2 vectorToRightPoint = Vector2.times(Vector2.rotate(direction, -getHalfSenseConeAngle()), getSenseRadius());
            Vector2 vectorToLeftPoint = Vector2.times(Vector2.rotate(direction, getHalfSenseConeAngle()), getSenseRadius());
            Vector2 rightPoint = Vector2.plus(POSITION.toVector2(), vectorToRightPoint);
            Vector2 leftPoint = Vector2.plus(POSITION.toVector2(), vectorToLeftPoint);

            //Get bounding box
            int minXBoundingBox = (int) Math.min(Math.min(rightPoint.x, leftPoint.x), POSITION.x);
            int minYBoundingBox = (int) Math.min(Math.min(rightPoint.y, leftPoint.y), POSITION.y);
            int maxXBoundingBox = (int) Math.max(Math.max(rightPoint.x, leftPoint.x), POSITION.x);
            int maxYBoundingBox = (int) Math.max(Math.max(rightPoint.y, leftPoint.y), POSITION.y);

            if (getHalfSenseConeAngle() > 45) {
                Vector2 vectorToCenterPoint = Vector2.times(direction, getSenseRadius());
                Vector2 centerPoint = Vector2.plus(POSITION.toVector2(), vectorToCenterPoint);

                minXBoundingBox = (int) Math.min(minXBoundingBox, centerPoint.x);
                minYBoundingBox = (int) Math.min(minYBoundingBox, centerPoint.y);
                maxXBoundingBox = (int) Math.max(maxXBoundingBox, centerPoint.x);
                maxYBoundingBox = (int) Math.max(maxYBoundingBox, centerPoint.y);
            }

            boundingBox = new int[]{minXBoundingBox, minYBoundingBox, maxXBoundingBox, maxYBoundingBox};
        } else {
            boundingBox = new int[]{POSITION.x - getSenseRadius(), POSITION.y - getSenseRadius(), POSITION.x + getSenseRadius(),
                    POSITION.y + getSenseRadius()};
        }
        return boundingBox;
    }

    private void lookForFood(Vector2 tendTowards, int x, int y, float[] distanceToClosestFood) {
        boolean isPixelFood = Canvas.IS_FOOD[x][y];
        if (isPixelFood) {
            float distanceToFood = new Vector2(x - POSITION.x, y - POSITION.y).length();

            boolean foodIsCloserThanPrevious = distanceToFood < distanceToClosestFood[0] ||
                                               (Float.compare(distanceToFood, distanceToClosestFood[0]) == 0 && RANDOM.nextDouble() < 0.5);
            if (foodIsCloserThanPrevious) {
                nestOrFood = new Position(x, y);
                setDirectionToNestOrFood();
                distanceToClosestFood[0] = distanceToFood;
            }
        } else if (hasNotFoundNestOrFood()) {
            addToTendency(tendTowards, x, y, COLONY.TRAILS_TO_FOOD);
        }
    }

    private void lookForNest(Vector2 tendTowards, int x, int y) {
        boolean isPixelNest = COLONY.NEST.equals(x, y);
        if (isPixelNest) {
            nestOrFood = new Position(x, y);
            setDirectionToNestOrFood();
            return;
        }

        addToTendency(tendTowards, x, y, COLONY.TRAILS_TO_NEST);
    }

    private void addToTendency(Vector2 tendTowards, int x, int y, int[][] trails) {
        Vector2 directionToPixel = new Vector2(x - POSITION.x, y - POSITION.y);
        directionToPixel.normalize();

        float trailStrengthAtPixel = trails[x][y];
        directionToPixel.times(trailStrengthAtPixel);
        tendTowards.plus(directionToPixel);
    }

    private void setDirectionToNestOrFood() {
        direction = new Vector2(nestOrFood.x - POSITION.x, nestOrFood.y - POSITION.y);
        direction.normalize();
    }

    /**
     Convert an angle to the corresponding utils.Position z.B. 0°-45°->[1,0] or 180°-225°->[-1,0]

     @param angle angle of directional Vector
     @return utils.Position with values -1,0 or 1
     @throws NumberFormatException if angle is NaN or angle > 360°
     */
    @NotNull
    private Position angleToPosition(float angle) throws NumberFormatException {
        switch ((int) angle / 45) {
            case 0 -> { return new Position(1, 0); }
            case 1 -> { return new Position(1, 1); }
            case 2 -> { return new Position(0, 1); }
            case 3 -> { return new Position(-1, 1); }
            case 4 -> { return new Position(-1, 0); }
            case 5 -> { return new Position(-1, -1); }
            case 6 -> { return new Position(0, -1); }
            case 7 -> { return new Position(1, -1); }
            default -> throw new RuntimeException("Invalid Angle");
        }
    }

    /**
     Calculate the new direction. Uses the old direction and changes it according to tendTowards and a random value {@link
    #randomizeDirectionalAngle()}.

     @param tendTowards float map (CanvasX x CanvasY) with trails of all ants
     */
    private void calculateNewDirection(Vector2 tendTowards) {
        calculateDirectionalAngle(tendTowards);

        randomizeDirectionalAngle();
        direction.normalize();
    }

    /**
     Changes the angle towards the ants tendency

     @param tendTowards Vector the ant tends towards
     */
    private void calculateDirectionalAngle(Vector2 tendTowards) {
        if (tendTowards.length() != 0 && tendTowards.angle() != 0) { return; }

        float angleBetween = tendTowards.angle() - direction.angle();

        //Correct for errors around 0 in unit circle
        if (angleBetween < -180) { angleBetween += 360; } else if (angleBetween > 180) { angleBetween -= 360; }

        //Correct for normalization (length might be smaller than 1 because of float values)
        final float strength = Math.max(1, tendTowards.length());

        //Approx number of Squares within circle sector. Formular: r^2 * (angle / 2)
        final float numberOfSquaresInSense = (float) (Math.pow(getSenseRadius(), 2) * Math.toRadians(getHalfSenseConeAngle()));
        final float maxPossibleStrength = numberOfSquaresInSense * getMaxTrailStrength();

        //Rotate towards angleBetween. Higher strength means more rotation.
        //Logs are used to make smaller values more important
        final float rotateBy = (float) (angleBetween * 5 * Math.log(strength / maxPossibleStrength + 1) / Math.log(2));

        if (angleBetween > getHalfSenseConeAngle() / 6) {
            direction.rotate((float) Math.min(getMaxRotation(), rotateBy));
        } else if (angleBetween < -getHalfSenseConeAngle() / 6) {
            direction.rotate((float) Math.max(-getMaxRotation(), rotateBy));
        }
    }

    /**
     Change angle by {@link #currentRandomAngle}. Adds a new random value each iteration. {@link #currentRandomAngle} cannot be bigger than {@link Colony#getSETTING
    MAX_RANDOM_ROTATION}
     */
    private void randomizeDirectionalAngle() {
        currentRandomAngle /= 1.05;
        currentRandomAngle += (RANDOM.nextFloat() - 0.5f) * (getMaxRandomRotation() / 5);
        currentRandomAngle = (float) Math.min(getMaxRandomRotation(), Math.max(currentRandomAngle, -getMaxRandomRotation()));
        direction.rotate(currentRandomAngle);
    }

    /**
     @param x x value of pixel
     @param y y value of pixel
     @return 'true' if pixel is relevant to ants sense
     */
    private boolean doesNotSensePixel(int x, int y) {
        Vector2 pixel = new Vector2(x - POSITION.x, y - POSITION.y);
        if (pixel.length() > getSenseRadius()) { return true; }
        return !isInFrontOfAnt(pixel);
    }

    /**
     @param vec Vector
     @return 'true' if angle between Vectors is smaller than {@link Colony#getSETTING() HALF_SENSE_CONE_ANGLE}
     @see Vector2#angleBetween(Vector2)
     */
    private boolean isInFrontOfAnt(Vector2 vec) {
        return direction.angleBetween(vec) < getHalfSenseConeAngle();
    }

    private void turnAround() {
        direction.rotate(180 + 90 * (RANDOM.nextFloat() - 0.5f));
    }

    /**
     Set trail of game.Ant

     @param trailMap float map with dimensions CanvasX x CanvasY
     */
    public void leaveTrail(int[][] trailMap) {
        trailMap[POSITION.x][POSITION.y] = Math.min(getMaxTrailStrength(), trailMap[POSITION.x][POSITION.y] + getTrailStrength());
    }

    /**
     @param x check {@link #POSITION}.x + x
     @param y check {@link #POSITION}.y + y
     @return 'true' if out of Bounds
     */
    private boolean isOutOfBounds(int x, int y) {
        return x < 0 || x >= CanvasSetting.canvasSizeX || y < 0 || y >= CanvasSetting.canvasSizeY;
    }
    //endregion
}
