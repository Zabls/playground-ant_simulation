package util;

public class Position {
    //region Fields
    public int x;
    public int y;
    //endregion

    //region Constructor
    public Position(int x, int y) {
        this.x = x;
        this.y = y;
    }
    //endregion

    //region Methods
    public void move(Position position) {
        x += position.x;
        y += position.y;
    }

    public Vector2 toVector2() {
        return new Vector2(x, y);
    }

    public boolean equals(Position position) {
        return (x == position.x && y == position.y);
    }

    public boolean equals(int x, int y) {
        return (this.x == x && this.y == y);
    }

    public float distanceTo(Position position) {
        return (float) Math.sqrt((x - position.x) * (x - position.x) + (y - position.y) * (y - position.y));
    }
    //endregion
}
