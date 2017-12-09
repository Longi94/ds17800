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
            if (server == null) {
                System.out.println("No server available or rather: You're not offline");
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                continue;
            }

            clientController = new ClientController();
            serverConnection = new ServerConnection(server, clientController);
            try {
                serverConnection.listenSocket();
                serverConnection.initializeClientUnit(unitType, unitId);
            } catch (Exception e) {
                System.out.println("Connection error: " + e.getMessage() + ". Will reconnect... ");
                e.printStackTrace();
                // start over, make a new connection
                continue;
            }

            // save the unit id in case we need to reconnect
            unitId = clientController.getUnitID();

            serverConnection.startListenerThread();

            while (true) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    System.out.println("Sleep interrupted!");
                }

                // apply any incoming message we might have in the socket buffer
                clientController.applyIncomingMessages();

                // run next client action
                try {
                    serverConnection.sendMessage(clientController.getNextMessage());
                } catch (IOException e) {
                    System.out.println("Unable to send message");
                    break;
                }
            }

            serverConnection.close();
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
