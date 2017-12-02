package nl.vu.ds17800.client;

import nl.vu.ds17800.core.model.MessageRequest;
import nl.vu.ds17800.core.model.units.Unit;

/**
 * Class for wrapping actions that are passed from PlayerController to ClientController
 */
public class ActionWrapper {
    Unit unit;
    MessageRequest actionType;
    private int targetX;
    private int targetY;
    private int actionPoints;

    public ActionWrapper(MessageRequest actionType, Unit unit, int targetX, int targetY, int actionPoints) {
        this.unit = unit;
        this.actionType = actionType;
        this.targetX = targetX;
        this.targetY = targetY;
        this.actionPoints = actionPoints;
    }

    public int getTargetX() {
        return targetX;
    }

    public int getTargetY() {
        return targetY;
    }

    public int getActionPoints() {
        return actionPoints;
    }

    public MessageRequest getActionType() {
        return actionType;
    }

    public Unit getUnit() {
        return unit;
    }

}
