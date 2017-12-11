package nl.vu.ds17800.client;

import nl.vu.ds17800.core.model.BattleField;
import nl.vu.ds17800.core.model.units.Unit;
import nl.vu.ds17800.core.networking.Message;

import java.util.Random;

public class PlayerController implements IUnitController {

    private final BattleField battleField;
    private final Unit unit;
    private boolean gameover = false;

    private enum Direction {up, down, left, right}

    private Random random = new Random(0L);

    public PlayerController(BattleField battleField, Unit unit) {
        this.battleField = battleField;
        this.unit = unit;
    }

    @Override
    public Message makeAction() {
        Direction direction;
        Unit adjacentUnit;
        int targetX = 0, targetY = 0;

        // Stop if the player runs out of hitpoints
        if (unit.getHitPoints() <= 0) {
            System.out.println("YOU DIED, GAME OVER");
            // we can also disconnect to improve server performance
            return Message.nop();
        }

        direction = getDirection(unit);

        switch (direction) {
            case up:
                if (unit.getY() <= 0) {
                    // The player was at the edge of the map, so he can't move north and there are no units there
                    return Message.nop();
                }
                targetX = unit.getX();
                targetY = unit.getY() - 1;
                break;
            case down:
                if (unit.getY() >= BattleField.MAP_HEIGHT - 1) {
                    // The player was at the edge of the map, so he can't move south and there are no units there
                    return Message.nop();
                }
                targetX = unit.getX();
                targetY = unit.getY() + 1;
                break;
            case left:
                if (unit.getX() <= 0) {
                    // The player was at the edge of the map, so he can't move west and there are no units there
                    return Message.nop();
                }


                targetX = unit.getX() - 1;
                targetY = unit.getY();
                break;
            case right:
                if (unit.getX() >= BattleField.MAP_WIDTH - 1) {
                    // The player was at the edge of the map, so he can't move east and there are no units there
                    return Message.nop();
                }

                targetX = unit.getX() + 1;
                targetY = unit.getY();
                break;
        }

        // Get what unit lies in the target square
        adjacentUnit = battleField.getUnit(targetX, targetY);

        if (adjacentUnit == null) {
            //there is no unit - go there
            return Message.moveUnit(unit, targetX, targetY);
        } else {
            switch (adjacentUnit.getType()) {
                case UNDEFINED:
                    // There is no unit in the square. Move the player to this square (just in case)
                    return Message.moveUnit(unit, targetX, targetY);
                case PLAYER:
                    // There is a player in the square, attempt a healing
                    if (adjacentUnit.getHitPoints() < adjacentUnit.getMaxHitPoints() / 2) {
                        return Message.healDamage(targetX, targetY, unit.getAttackPoints());
                    } else {
                        return Message.nop();
                    }
                case DRAGON:
                    // There is a dragon in the square, attempt a dragon slaying
                    return Message.dealDamage(targetX, targetY, unit.getAttackPoints());
            }
        }
        return Message.nop();
    }

    @Override
    public boolean isGameover() {
        return gameover;
    }

    private Direction getDirection(Unit myUnit) {
        Unit unit = battleField.getNearestUnit(myUnit.getX(), myUnit.getY(), Unit.UnitType.DRAGON);

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
