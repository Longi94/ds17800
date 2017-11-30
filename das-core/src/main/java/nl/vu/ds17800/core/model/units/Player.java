package nl.vu.ds17800.core.model.units;

import nl.vu.ds17800.core.model.Action;
import nl.vu.ds17800.core.model.BattleField;

/**
 * A Player is, as the name implies, a playing
 * character. It can move in the four wind directions,
 * has a hitpoint range between 10 and 20
 * and an attack range between 1 and 10.
 */
public class Player extends Unit {

    public static final int MIN_TIME_BETWEEN_TURNS = 2;
    public static final int MAX_TIME_BETWEEN_TURNS = 7;
    public static final int MIN_HITPOINTS = 20;
    public static final int MAX_HITPOINTS = 10;
    public static final int MIN_ATTACKPOINTS = 1;
    public static final int MAX_ATTACKPOINTS = 10;

    /**
     * Create a player, initialize both
     * the hit and the attackpoints.
     */
    public Player(BattleField battleField, int x, int y) {
        /* Initialize the hitpoints and attackpoints */
        super(battleField,
                (int) (Math.random() * (MAX_HITPOINTS - MIN_HITPOINTS) + MIN_HITPOINTS),
                (int) (Math.random() * (MAX_ATTACKPOINTS - MIN_ATTACKPOINTS) + MIN_ATTACKPOINTS));
    }

    @Override
    public UnitType getType() {
        return UnitType.PLAYER;
    }

    @Override
    public boolean apply(Action action) {
        Unit unit;
        switch (action.getType()) {
            case PLAYER_ATTACK:
                unit = battleField.getUnit(action.getX(), action.getY());
                if (unit != null && unit.getType() == UnitType.DRAGON) {
                    adjustHitPoints(action.getHitPoints());
                    return true;
                }
                return false;
            case PLAYER_HEAL:
                unit = battleField.getUnit(action.getX(), action.getY());
                if (unit != null && unit.getType() == UnitType.PLAYER) {
                    adjustHitPoints(action.getHitPoints());
                    return true;
                }
                return false;
            case PLAYER_MOVE:
                return battleField.moveUnit(this, action.getX(), action.getY());
            default:
                return false;
        }
    }

    @Override
    public boolean check(Action action) {
        Unit unit;
        switch (action.getType()) {
            case PLAYER_ATTACK:
                unit = battleField.getUnit(action.getX(), action.getY());
                return unit != null && unit.getType() == UnitType.DRAGON;
            case PLAYER_HEAL:
                unit = battleField.getUnit(action.getX(), action.getY());
                return unit != null && unit.getType() == UnitType.PLAYER;
            case PLAYER_MOVE:
                return battleField.getUnit(action.getX(), action.getY()) == null;
            default:
                return false;
        }
    }
}
