package nl.vu.ds17800.server;

import nl.vu.ds17800.core.model.BattleField;
import nl.vu.ds17800.core.networking.Communication;
import nl.vu.ds17800.core.networking.CommunicationImpl;
import nl.vu.ds17800.core.networking.Entities.Server;

import java.io.IOException;

/**
 * @author lngtr
 * @since 2017-11-16
 */
public class DasServer {

    DasServer(Server serverDescr) {
        BattleField bf = new BattleField();

        ServerController sc = new ServerController(bf);
        Communication c =  new CommunicationImpl(sc, serverDescr);
        sc.setCommuncation(c);

        System.out.println("Sleeping 5 seconds while other servers are coming online");
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        for (Server s : c.getServers()) {
            if(s.equals(serverDescr)) {
                continue;
            }

            System.out.println("Connecting to server " + s.toString() + "...");

            try {
                sc.connectServer(s);
            } catch (IOException e) {
                System.out.println("Unable to connect to server " + s.toString() + ": " + e.getMessage());
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        if(args.length < 1) {
            System.out.println("Usage: server.jar 10100|10101|10102|10103|10104");
            System.exit(1);
        }
        int port = Integer.parseInt(args[0]);
        Server serverDescr = new Server();
        serverDescr.serverPort = port;
        serverDescr.ipaddr = "localhost";

        new DasServer(serverDescr);
    }
}
