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

        if (args.length < 1){
            System.out.println("Input: \"dragon\" or \"player\" to run the game");
            return;
        }

        String unitType = args[0];
        int preferredServer = args.length >= 2 ? Integer.parseInt(args[1]) : -1;


        if(!(unitType.equals("dragon") || unitType.equals("player"))) {
            System.out.println("Wrong unit type!");
            return;
        }
        System.out.println("Unit Type: " + unitType);
        System.out.println("Running...");

        Boolean success = clientController.initialiseConnection(unitType, preferredServer);

        if(success) {
            System.out.println("Server connection set");
            System.out.println("Launching simulation..");
            Thread simulation;
            if(unitType.equals("player")) {
                simulation = new Thread(new PlayerController());
            } else {
                simulation = new Thread(new DragonController());
            }
            simulation.start();
            System.out.println("Simulation launched.");
        } else {
            System.out.println("Game launching failed");
        }

    }
}
