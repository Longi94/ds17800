package nl.vu.ds17800.core.model.units;

import java.util.Random;

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
     * Create a player, initialize both the hit and the attackpoints.
     *
     * @param unitId the id of the unit
     * @param x      x coordinate
     * @param y      y coordinate
     */
    public Player(String unitId, int x, int y) {
        this(unitId, x, y, new Random());
    }

    /**
     * Create a player, initialize both the hit and the attackpoints.
     *
     * @param unitId the id of the unit
     * @param x      x coordinate
     * @param y      y coordinate
     * @param random the random object, used to make simulation reproducible
     */
    public Player(String unitId, int x, int y, Random random) {
        /* Initialize the hitpoints and attackpoints */
        super(unitId,
                (int) (random.nextDouble() * (MAX_HITPOINTS - MIN_HITPOINTS) + MIN_HITPOINTS),
                (int) (random.nextDouble() * (MAX_ATTACKPOINTS - MIN_ATTACKPOINTS) + MIN_ATTACKPOINTS));

        this.setPosition(x, y);
    }

    @Override
    public UnitType getType() {
        return UnitType.PLAYER;
    }
}
