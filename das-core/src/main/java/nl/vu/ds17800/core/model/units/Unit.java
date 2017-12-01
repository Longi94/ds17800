package nl.vu.ds17800.core.model.units;

import nl.vu.ds17800.core.model.BattleField;

import java.io.Serializable;

/**
 * Base class for all players whom can
 * participate in the DAS game. All properties
 * of the units (hitpoints, attackpoints) are
 * initialized in this class.
 */
public abstract class Unit implements Serializable {

    private static final long serialVersionUID = -4550572524008491160L;

    // Position of the unit
    private int x, y;

    // Health
    private int maxHitPoints;
    private int hitPoints;

    // Attack points
    private int attackPoints;

    // Identifier of the unit
    private String unitID;

    protected BattleField battleField;

    public enum UnitType {
        PLAYER, DRAGON, UNDEFINED
    }

    /**
     * Create a new unit and specify the
     * number of hitpoints. Units hitpoints
     * are initialized to the maxHitPoints.
     *
     * @param maxHealth is the maximum health of
     *                  this specific unit.
     */
    public Unit(BattleField battleField, int maxHealth, int attackPoints) {

        // Initialize the max health and health
        hitPoints = maxHitPoints = maxHealth;

        // Initialize the attack points
        this.attackPoints = attackPoints;

        this.battleField = battleField;

        // Get a new unit id
        this.unitID = battleField.getNewUnitID();
    }

    /**
     * Adjust the hitpoints to a certain level.
     * Useful for healing or dying purposes.
     *
     * @param modifier is to be added to the
     *                 hitpoint count.
     */
    public void adjustHitPoints(int modifier) {
        if (hitPoints <= 0) {
            return;
        }

        hitPoints = Math.min(hitPoints + modifier, maxHitPoints);

        if (hitPoints <= 0) {
            battleField.removeUnit(this);
        }
    }

    /**
     * @return the maximum number of hitpoints.
     */
    public int getMaxHitPoints() {
        return maxHitPoints;
    }

    /**
     * @return the unique unit identifier.
     */
    public String getUnitID() {
        return unitID;
    }

    /**
     * Set the position of the unit.
     *
     * @param x is the new x coordinate
     * @param y is the new y coordinate
     */
    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }

    /**
     * @return the x position
     */
    public int getX() {
        return x;
    }

    /**
     * @return the y position
     */
    public int getY() {
        return y;
    }

    /**
     * @return the current number of hitpoints.
     */
    public int getHitPoints() {
        return hitPoints;
    }

    /**
     * @return the attack points
     */
    public int getAttackPoints() {
        return attackPoints;
    }

    public UnitType getType() {
        return UnitType.UNDEFINED;
    }
}
