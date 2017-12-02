package nl.vu.ds17800.client;

import nl.vu.ds17800.core.model.BattleField;
import nl.vu.ds17800.core.model.units.Unit;


/**
 * @author lngtr
 * @since 2017-11-16
 */
public class DasClient {

    // Static stuff to be shared both with simulation thread and ClientController threads (is this approach correct?)
    public static BattleField battleField = new BattleField();
    public static ClientController clientController = new ClientController();
    public static Unit myUnit;

    public static void main(String[] args) {

        System.out.println("Running...");

        Boolean success = clientController.initialiseConnection();

        if(success) {
            System.out.println("Server connection set");
            System.out.println("Launching simulation..");
            Thread simulation = new Thread(new PlayerController());
            simulation.start();
            System.out.println("Simulation launched.");
        } else {
            System.out.println("Game launching failed");
        }

    }
}
