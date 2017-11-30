package nl.vu.ds17800.core.model;

/**
 * @author lngtr
 * @since 2017-11-30
 */
public class Action {

    private ActionType type;

    private int x;

    private int y;

    private int hitPoints;

    public ActionType getType() {
        return type;
    }

    public void setType(ActionType type) {
        this.type = type;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public int getHitPoints() {
        return hitPoints;
    }

    public void setHitPoints(int hitPoints) {
        this.hitPoints = hitPoints;
    }
}
