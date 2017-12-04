package nl.vu.ds17800.client;

import nl.vu.ds17800.core.model.units.Unit;

import java.util.List;

import static nl.vu.ds17800.core.model.MessageRequest.dealDamage;

public class DragonController implements Runnable {

    public DragonController() {}

    public void run() {

        while (true) {
            try {
                // Sleep while the dragon is considering its next move (HARDCODED 2000)
                Thread.currentThread().sleep((int) (2000));

                // Get current state of my unit
                Unit myUnit = DasClient.myUnit;

                /* Stop if the dragon runs out of hitpoints */
                if (myUnit.getHitPoints() <= 0)
                    break;

                // Pick a random near player to attack
                List<Unit> nearbyPlayers =  DasClient.battleField.getNearbyPlayers(myUnit.getX(), myUnit.getY(), Unit.UnitType.PLAYER, 2);
                if (nearbyPlayers.size() == 0)
                    continue; // There are no players to attack
                Unit playerToAttack = nearbyPlayers.get((int) (Math.random() * nearbyPlayers.size()));

                // Attack the player
                ActionWrapper actionWrapper = new ActionWrapper(dealDamage, null, playerToAttack.getX(), playerToAttack.getY(), myUnit.getAttackPoints());

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
