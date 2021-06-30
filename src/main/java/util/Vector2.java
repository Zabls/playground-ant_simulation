package util;

import java.util.Random;

public class Vector2 {
    //region Fields
    public float x;
    public float y;
    //endregion

    //region Constructor
    public Vector2(float x, float y) {
        this.x = x;
        this.y = y;
    }
    //endregion

    //region Methods
    public void plus(Vector2 vec) {
        plus(vec.x, vec.y);
    }

    public static Vector2 plus(Vector2 vec, Vector2 vector2) {
        return new Vector2(vec.x + vector2.x, vec.y + vector2.y);
    }

    public void plus(float x, float y) {
        this.x += x;
        this.y += y;
    }

    public void minus(float x, float y) {
        this.x -= x;
        this.y -= y;
    }

    public void times(float i) {
        x *= i;
        y *= i;
    }

    public static Vector2 times(Vector2 vector2, float i) {
        return new Vector2(vector2.x * i, vector2.y * i);
    }

    /**
     @return Returns the length (norm) of this vector.
     */
    public float length() {
        return (float) Math.sqrt(x * x + y * y);
    }

    public float angle() {
        if (x == 0 && y != 0) {
            if (y == 0) {
                randomize();
                return angle();
            }
            return y > 0 ? 90 : 270;
        }
        float angle = (float) Math.toDegrees(Math.atan(y / x));

        return x > 0 ? (angle + 360) % 360 : (angle + 180) % 360;
    }

    public float angleBetween(Vector2 vec) {
        float division = scalar(vec) / (length() * vec.length());
        if (division > 1) { return 0; }
        if (division < -1) { return 180; }

        float angleInRand = (float) Math.acos(division);

        return (float) Math.toDegrees(angleInRand);
    }

    private float scalar(Vector2 vec) {
        return x * vec.x + y * vec.y;
    }

    public void rotate(float degrees) {
        float radians = (float) Math.toRadians(degrees);

        float rx = (float) (x * Math.cos(radians) - y * Math.sin(radians));
        float ry = (float) (x * Math.sin(radians) + y * Math.cos(radians));
        x = rx;
        y = ry;
    }

    public static Vector2 rotate(Vector2 vector2, double degrees) {
        double radians = Math.toRadians(degrees);

        float rx = (float) (vector2.x * Math.cos(radians) - vector2.y * Math.sin(radians));
        float ry = (float) (vector2.x * Math.sin(radians) + vector2.y * Math.cos(radians));
        return new Vector2(rx, ry);
    }

    public void randomize() {
        Random random = new Random();

        x = random.nextFloat() * 2 - 1;
        y = random.nextFloat() * 2 - 1;
        normalize();
    }

    /**
     Normalizes this vector: changes the length of this vector such that it becomes 1. The direction and orientation of the vector is not affected.
     */
    public void normalize() {
        float length = length();
        if (length == 0) {
            randomize();
            return;
        }
        x /= length;
        y /= length;
    }

    @Override
    public String toString() {
        return "[" + x + "," + y + "]";
    }
    //endregion
}
