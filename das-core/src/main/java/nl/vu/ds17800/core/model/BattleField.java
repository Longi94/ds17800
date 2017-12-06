package nl.vu.ds17800.core.model;

import nl.vu.ds17800.core.model.units.Unit;

import java.io.Serializable;
import java.util.*;

/**
 * The actual battlefield where the fighting takes place.
 * It consists of an array of a certain width and height.
 */
public class BattleField implements Serializable {

    public final static int MAP_WIDTH = 25;
    public final static int MAP_HEIGHT = 25;

    /* The array of units */
    private Unit[][] map;

    private List<Unit> units = Collections.synchronizedList(new ArrayList<Unit>());

    /**
     * Initialize the battlefield to the default size
     */
    public BattleField() {
        this(MAP_WIDTH, MAP_HEIGHT);
    }

    /**
     * Initialize the battlefield to the specified size
     *
     * @param width  of the battlefield
     * @param height of the battlefield
     */
    public BattleField(int width, int height) {
        map = new Unit[width][height];
    }

    /**
     * Get random free x y position in the battle field
     *
     * @return [x, y] or otherwise null if no free position was found
     */
    public int[] getRandomFreePosition(Random random) {
        int[] pos;

        int x, y, attempt = 0;
        do {
            x = (int) (random.nextDouble() * BattleField.MAP_WIDTH);
            y = (int) (random.nextDouble() * BattleField.MAP_HEIGHT);
            attempt++;
        } while (getUnit(x, y) != null && attempt < 10);

        if (getUnit(x, y) != null) {
            return null;
        }

        pos = new int[]{x, y};

        return pos;
    }

    public Unit findUnitById(String id) {
        for (Unit u : units) {
            if (id.equals(u.getUnitID())) {
                return u;
            }
        }
        return null;
    }

    /**
     * Puts a new unit at the specified position. First, it
     * checks whether the position is empty, if not, it
     * does nothing.
     * In addition, the unit is also put in the list of known units.
     *
     * @param unit is the actual unit being spawned
     *             on the specified position.
     * @param x    is the x position.
     * @param y    is the y position.
     * @return true when the unit has been put on the
     * specified position.
     */
    public boolean spawnUnit(Unit unit, int x, int y) {
        synchronized (map) {

            if (map[x][y] != null) {
                return false;
            }

            map[x][y] = unit;
            unit.setPosition(x, y);
            units.add(unit);

        }
        return true;
    }

    /**
     * Put a unit at the specified position. First, it
     * checks whether the position is empty, if not, it
     * does nothing.
     *
     * @param unit is the actual unit being put
     *             on the specified position.
     * @param x    is the x position.
     * @param y    is the y position.
     * @return true when the unit has been put on the
     * specified position.
     */
    public boolean putUnit(Unit unit, int x, int y) {
        synchronized (map) {

            if (map[x][y] != null) {
                return false;
            }

            map[x][y] = unit;
            unit.setPosition(x, y);
        }

        return true;
    }

    /**
     * Get a unit from a position.
     *
     * @param x position.
     * @param y position.
     * @return the unit at the specified position, or return
     * null if there is no unit at that specific position.
     */
    public Unit getUnit(int x, int y) {
        assert x >= 0 && x < map.length;
        assert y >= 0 && x < map[0].length;

        return map[x][y];
    }

    /**
     * Move the specified unit a certain number of steps.
     *
     * @param unit is the unit being moved.
     * @param newX is the new x position.
     * @param newY is the new position.
     * @return true on success.
     */
    public boolean moveUnit(Unit unit, int newX, int newY) {
        synchronized (map) {

            int originalX = unit.getX();
            int originalY = unit.getY();

            if (unit.getHitPoints() <= 0) {
                return false;
            }

            if (newX < 0 || newX >= BattleField.MAP_WIDTH ||
                    newY < 0 || newY >= BattleField.MAP_HEIGHT ||
                    map[newX][newY] != null ||
                    !putUnit(unit, newX, newY)) {
                return false;
            }

            map[originalX][originalY] = null;
        }
        return true;
    }

    /**
     * Remove a unit from a specific position.
     *
     * @param x position.
     * @param y position.
     */
    public void removeUnit(int x, int y) {

        synchronized (map) {

            Unit unitToRemove = getUnit(x, y);
            if (unitToRemove == null) {
                return; // There was no unit here to remove
            }
            map[x][y] = null;
            unitToRemove.setHitPoints(0);
        }
    }

    /**
     * Returns a new unique unit ID.
     *
     * @return int: a new unique unit ID.
     */
    public String getNewUnitID() {
        return UUID.randomUUID().toString();
    }

    /**
     * Apply an action to the game.
     *
     * @param action action
     */
    public void apply(Map<String, Object> action) {
        MessageRequest request = (MessageRequest) action.get("request");
        Unit unit;
        switch (request) {
            case spawnUnit:
                spawnUnit((Unit) action.get("unit"), (Integer) action.get("x"), (Integer) action.get("y"));
                break;
            case putUnit:
                putUnit((Unit) action.get("unit"), (Integer) action.get("x"), (Integer) action.get("y"));
                break;
            case dealDamage: {
                int x = (Integer) action.get("x");
                int y = (Integer) action.get("y");
                unit = getUnit(x, y);
                if (unit != null) {
                    unit.adjustHitPoints(-(Integer) action.get("damage"));

                    if (unit.getHitPoints() <= 0) {
                        removeUnit(x, y);
                    }
                }
                break;
            }
            case healDamage: {
                int x = (Integer) action.get("x");
                int y = (Integer) action.get("y");
                unit = getUnit(x, y);
                if (unit != null) {
                    unit.adjustHitPoints((Integer) action.get("healed"));
                }
                break;
            }
            case moveUnit:
                unit = findUnitById(((Unit) action.get("unit")).getUnitID());
                if (unit != null) {
                    moveUnit(unit, (Integer) action.get("x"), (Integer) action.get("y"));
                }
                break;
            case removeUnit:
                removeUnit((Integer) action.get("x"), (Integer) action.get("y"));
                break;
        }
    }

    /**
     * Check if the action is valid or not.
     *
     * @param action action
     * @return valid
     */
    public boolean check(Map<String, Object> action) {
        MessageRequest request = (MessageRequest) action.get("request");
        Unit unit;
        switch (request) {
            case spawnUnit:
                return map[(Integer) action.get("x")][(Integer) action.get("y")] == null;
            case putUnit:
                return map[(Integer) action.get("x")][(Integer) action.get("y")] == null;
            case dealDamage: {
                int x = (Integer) action.get("x");
                int y = (Integer) action.get("y");
                return getUnit(x, y) != null;
            }
            case healDamage: {
                int x = (Integer) action.get("x");
                int y = (Integer) action.get("y");
                return getUnit(x, y) != null;
            }
            case moveUnit:
                int newX = (Integer) action.get("x");
                int newY = (Integer) action.get("y");
                unit = (Unit) action.get("unit");

                return unit.getHitPoints() > 0 && newX >= 0 && newX < BattleField.MAP_WIDTH && newY >= 0 &&
                        newY < BattleField.MAP_HEIGHT && map[newX][newY] == null;

            case removeUnit:
                return true;
            default:
                return false;
        }
    }

    /**
     * Find all the specified type of units that are nearby.
     *
     * @param x        x coordinate
     * @param y        y coordinate
     * @param type     the type of units to look for
     * @param distance the max steps to reach the unit
     * @return a list of nearby units
     */
    public List<Unit> getNearbyPlayers(int x, int y, Unit.UnitType type, int distance) {
        List<Unit> players = new ArrayList<>();

        for (Unit unit : units) {
            int d = Math.abs(x - unit.getX()) + Math.abs(y - unit.getY());
            if (unit.getHitPoints() > 0 && unit.getType() == type && d > 0 && d <= distance) {
                players.add(unit);
            }
        }

        return players;
    }

    /**
     * Find the nearest unit
     *
     * @param x    x coordinate
     * @param y    y coordinate
     * @param type the type of unit to find
     * @return the nearest unit, null if there are no such units
     */
    public Unit getNearestUnit(int x, int y, Unit.UnitType type) {
        Unit nearestUnit = null;
        int currentD = Integer.MAX_VALUE;

        for (Unit unit : units) {
            int d = Math.abs(x - unit.getX()) + Math.abs(y - unit.getY());
            if (unit.getHitPoints() > 0 && unit.getType() == type && d > 0 && d < currentD) {
                nearestUnit = unit;
                currentD = d;
            }
        }

        return nearestUnit;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < units.size(); i++) {
            builder.append(String.format("%3d | ", i)).append(units.get(i)).append('\n');
        }

        for (int i = map.length - 1; i >= 0; i--) {
            for (int j = 0; j < map[i].length; j++) {
                if (map[j][i] != null) {
                    builder.append(String.format("%3d", units.indexOf(map[j][i])));
                } else {
                    builder.append("  .");
                }
            }
            builder.append('\n');
        }

        return builder.toString();
    }
}
