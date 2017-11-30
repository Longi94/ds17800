package nl.vu.ds17800.core.model.units;

import nl.vu.ds17800.core.model.Action;
import nl.vu.ds17800.core.model.BattleField;

/**
 * A dragon is a non-playing character, which can't
 * move, has a hitpoint range between 50 and 100
 * and an attack range between 5 and 20.
 */
public class Dragon extends Unit {

    public static final int MIN_TIME_BETWEEN_TURNS = 2;
    public static final int MAX_TIME_BETWEEN_TURNS = 7;
    // The minimum and maximum amount of hitpoints that a particular dragon starts with
    public static final int MIN_HITPOINTS = 50;
    public static final int MAX_HITPOINTS = 100;
    // The minimum and maximum amount of hitpoints that a particular dragon has
    public static final int MIN_ATTACKPOINTS = 5;
    public static final int MAX_ATTACKPOINTS = 20;

    /**
     * Spawn a new dragon, initialize the
     * reaction speed
     */
    public Dragon(BattleField battleField, int x, int y) {
        /* Spawn the dragon with a random number of hitpoints between
         * 50..100 and 5..20 attackpoints. */
        super(battleField,
                (int) (Math.random() * (MAX_HITPOINTS - MIN_HITPOINTS) + MIN_HITPOINTS),
                (int) (Math.random() * (MAX_ATTACKPOINTS - MIN_ATTACKPOINTS) + MIN_ATTACKPOINTS));

        this.setPosition(x, y);
    }

    @Override
    public UnitType getType() {
        return UnitType.DRAGON;
    }

    @Override
    public boolean apply(Action action) {
        switch (action.getType()) {
            case DRAGON_ATTACK:
                Unit unit = battleField.getUnit(action.getX(), action.getY());
                if (unit != null && unit.getType() == UnitType.PLAYER) {
                    adjustHitPoints(action.getHitPoints());
                    return true;
                }
                return false;
            default:
                return false;
        }
    }

    @Override
    public boolean check(Action action) {
        switch (action.getType()) {
            case DRAGON_ATTACK:
                Unit unit = battleField.getUnit(action.getX(), action.getY());
                return unit != null && unit.getType() == UnitType.PLAYER;
            default:
                return false;
        }
    }
}
