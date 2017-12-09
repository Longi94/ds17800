package nl.vu.ds17800.client;

import nl.vu.ds17800.core.model.BattleField;
import nl.vu.ds17800.core.model.units.Unit;
import nl.vu.ds17800.core.networking.Message;

import java.util.List;
import java.util.Random;

public class DragonController implements IUnitController {

    private final BattleField battleField;
    private final Unit unit;

    private Random random = new Random(0L);

    public DragonController(BattleField battleField, Unit unit) {
        this.unit = unit;
        this.battleField = battleField;
    }

    @Override
    public Message makeAction() {

        /* Stop if the dragon runs out of hitpoints */
        if (unit.getHitPoints() <= 0) {
            // dragon can disconnect, he is not real and does not need to spectate.
            return Message.clientDisconnect(this.unit.getUnitID());
        }

        // Pick a random near player to attack
        List<Unit> nearbyPlayers =  battleField.getNearbyPlayers(unit.getX(), unit.getY(), Unit.UnitType.PLAYER, 2);
        if (nearbyPlayers.size() == 0) {
            // still send a dumy message because if we don't talk to server regularily, we will be disconnected
            return Message.nop();
        }

        Unit playerToAttack = nearbyPlayers.get((int) (random.nextDouble() * nearbyPlayers.size()));

        // Attack the player
        return Message.dealDamage(playerToAttack.getX(), playerToAttack.getY(), unit.getAttackPoints());
    }
}
