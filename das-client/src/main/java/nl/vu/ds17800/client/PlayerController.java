package nl.vu.ds17800.client;

import nl.vu.ds17800.core.model.BattleField;
import nl.vu.ds17800.core.model.units.Unit;

import java.util.Random;

import static nl.vu.ds17800.core.model.MessageRequest.*;

public class PlayerController implements Runnable {

    private enum Direction {up, down, left, right}

    private Random random = new Random(0L);

    public PlayerController() {
    }


    public void run() {
        Direction direction;
        Unit adjacentUnit;
        int targetX = 0, targetY = 0;

        while (true) {
            try {
                // Sleep while the player is considering its next move (HARDCODED 500)
                Thread.sleep(500L);

                System.out.println(DasClient.battleField.toString());

                // Get current state of my unit
                Unit myUnit = DasClient.myUnit;

                // Stop if the player runs out of hitpoints
                if (myUnit.getHitPoints() <= 0) {
                    System.out.println("YOU DIED, GAME OVER");
                    break;
                }

                direction = getDirection(myUnit);

                switch (direction) {
                    case up:
                        if (myUnit.getY() <= 0)
                            // The player was at the edge of the map, so he can't move north and there are no units there
                            continue;

                        targetX = myUnit.getX();
                        targetY = myUnit.getY() - 1;
                        break;
                    case down:
                        if (myUnit.getY() >= BattleField.MAP_HEIGHT - 1)
                            // The player was at the edge of the map, so he can't move south and there are no units there
                            continue;

                        targetX = myUnit.getX();
                        targetY = myUnit.getY() + 1;
                        break;
                    case left:
                        if (myUnit.getX() <= 0)
                            // The player was at the edge of the map, so he can't move west and there are no units there
                            continue;

                        targetX = myUnit.getX() - 1;
                        targetY = myUnit.getY();
                        break;
                    case right:
                        if (myUnit.getX() >= BattleField.MAP_WIDTH - 1)
                            // The player was at the edge of the map, so he can't move east and there are no units there
                            continue;

                        targetX = myUnit.getX() + 1;
                        targetY = myUnit.getY();
                        break;
                }

                // Get what unit lies in the target square
                adjacentUnit = DasClient.battleField.getUnit(targetX, targetY);

                ActionWrapper actionWrapper = null;
                if(adjacentUnit == null) {
                    //there is no unit - go there
                    actionWrapper = new ActionWrapper(moveUnit, myUnit, targetX, targetY, 0);
                } else {
                    switch (adjacentUnit.getType()) {
                        case UNDEFINED:
                            // There is no unit in the square. Move the player to this square (just in case)
                            actionWrapper = new ActionWrapper(moveUnit, myUnit, targetX, targetY, 0);
                            break;
                        case PLAYER:
                            // There is a player in the square, attempt a healing
                            if(adjacentUnit.getHitPoints() < adjacentUnit.getMaxHitPoints()/2) {
                                actionWrapper = new ActionWrapper(healDamage, null, targetX, targetY, myUnit.getAttackPoints());
                            }
                            break;
                        case DRAGON:
                            // There is a dragon in the square, attempt a dragon slaying
                            actionWrapper = new ActionWrapper(dealDamage, null, targetX, targetY, myUnit.getAttackPoints());
                            break;
                    }

                }

                boolean success = DasClient.clientController.sendUnitAction(actionWrapper);

                if(!success) {
                    System.out.println("CONNECTION LOST, RECONNECTION FAILED");
                    break;
                }

            } catch (Exception e) {
                System.out.println("IN THREAD ERROR: " + e.getMessage() + " [Stack]:");
                e.printStackTrace();
                break;
            }
        }

    }

    private Direction getDirection(Unit myUnit) {
        Unit unit = DasClient.battleField.getNearestUnit(myUnit.getX(), myUnit.getY(), Unit.UnitType.DRAGON);

        if (unit != null) {
            // move towards the nearest dragon, random if no dragon
            int dX = Math.abs(unit.getX() - myUnit.getX());
            int dY = Math.abs(unit.getY() - myUnit.getY());

            if (dX > dY) {
                return unit.getX() > myUnit.getX() ? Direction.right : Direction.left;
            } else {
                return unit.getY() > myUnit.getY() ? Direction.down : Direction.up;
            }
        }

        return Direction.values()[(int) (Direction.values().length * random.nextDouble())];
    }

}
