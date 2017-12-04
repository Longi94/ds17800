package nl.vu.ds17800.client;

import nl.vu.ds17800.core.model.units.Unit;

import static nl.vu.ds17800.core.model.MessageRequest.dealDamage;
import static nl.vu.ds17800.core.model.MessageRequest.healDamage;
import static nl.vu.ds17800.core.model.MessageRequest.moveUnit;

public class PlayerController implements Runnable {

    private enum Direction {up, down, left, right}

    public PlayerController() {
    }


    public void run() {
        Direction direction;
        Unit adjacentUnit;
        int targetX = 0, targetY = 0;

        while (true) {
            try {
                // Sleep while the player is considering its next move (HARDCODED 500)
                Thread.currentThread().sleep((int) (500));

                // Get current state of my unit
                Unit myUnit = DasClient.myUnit;

                // Stop if the player runs out of hitpoints
                if (myUnit.getHitPoints() <= 0) {
                    System.out.println("YOU DIED, GAME OVER");
                    break;
                }

                // Randomly choose one of the four wind directions to move to if there are no units present
                direction = Direction.values()[(int) (Direction.values().length * Math.random())];

                switch (direction) {
                    case up:
                        if (myUnit.getY() <= 0)
                            // The player was at the edge of the map, so he can't move north and there are no units there
                            continue;

                        targetX = myUnit.getX();
                        targetY = myUnit.getY() - 1;
                        break;
                    case down:
                        if (myUnit.getY() >= DasClient.battleField.MAP_HEIGHT - 1)
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
                        if (myUnit.getX() >= DasClient.battleField.MAP_WIDTH - 1)
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
                            actionWrapper = new ActionWrapper(healDamage, null, targetX, targetY, myUnit.getAttackPoints());
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
                System.out.println("IN THREAD ERROR: " + e.getMessage() + " [Stack]: " + e.getStackTrace());
            }
        }

    }

}
