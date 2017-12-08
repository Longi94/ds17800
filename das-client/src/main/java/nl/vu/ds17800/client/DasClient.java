package nl.vu.ds17800.client;

import nl.vu.ds17800.core.networking.Endpoint;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;


/**
 * @author lngtr
 * @since 2017-11-16
 */
public class DasClient {
    public final static List<Endpoint> SERVERS = Arrays.asList(
            new Endpoint("localhost",10100),
            new Endpoint("localhost",10101),
            new Endpoint("localhost",10102),
            new Endpoint("localhost",10103),
            new Endpoint("localhost",10104)
    );

    private String unitId = null;

    public DasClient(String unitType) {
        Endpoint server;
        ClientController clientController;
        ServerConnection serverConnection;

        while(true) {
            // Set up (re-)connection
            server = ClientController.getServerToConnect(SERVERS);
            clientController = new ClientController();
            serverConnection = new ServerConnection(server, clientController);
            try {
                serverConnection.listenSocket();

                // blocking until we get initial data from server
                serverConnection.initializeClientUnit(unitType, unitId);
            } catch (Exception e) {
                System.out.println("Connection error: " + e.getMessage() + ". Will reconnect... ");
                // start over, make a new connection
                continue;
            }

            // save the unit id in case we need to reconnect
            unitId = clientController.getUnit().getUnitID();

            while (true) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    System.out.println("Sleep interrupted!");
                }

                // apply any incoming message we might have in the socket buffer
                try {
                    serverConnection.consumeIncomingMessages();
                } catch (Exception e) {
                    System.out.println("Connection error: " + e.getMessage() + ". Will reconnect... ");
                    // break out of the game loop, reconnect and hope everything will be ok again
                    break;
                }

                // run next client action
                serverConnection.sendMessage(clientController.getNextMessage());
            }
        }
    }


    public static void main(String[] args) {


        if (args.length < 1){
            System.out.println("Input: <dragon/player> [debug]");
            return;
        }

        String unitType = args[0];

        if(!(unitType.equals("dragon") || unitType.equals("player"))) {
            System.out.println("Wrong unit type!");
            return;
        }
        System.out.println("Unit Type: " + unitType);
        System.out.println("Running...");

        new DasClient(unitType);
    }
}
