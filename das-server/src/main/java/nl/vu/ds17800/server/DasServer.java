package nl.vu.ds17800.server;
import nl.vu.ds17800.core.networking.Endpoint;

import java.net.Socket;

/**
 * @author lngtr
 * @since 2017-11-16
 */
public class DasServer {
    public static boolean DEBUG = false;

    public DasServer(final Endpoint serverClientEndp, final Endpoint serverServerEndp) {
        final ServerController sc = new ServerController();

        final SocketListener clientListener = new SocketListener(new IConnectionHandler() {
            @Override
            public void handle(Socket s) {
                new Thread(new ClientWorker(s, sc)).start();
            }
        });

        // listener thread for incoming client-server connections
        new Thread(new Runnable() {
            @Override
            public void run() {
                System.out.println("Accepting client connections on " + serverClientEndp.getPort());
                clientListener.listenSocket(serverClientEndp.getPort());
            }
        }).start();


        final SocketListener serverListener = new SocketListener(new IConnectionHandler() {
            @Override
            public void handle(Socket s) {
                new Thread(new ServerWorker(s, sc)).start();
            }
        });

        // listener thread for incoming server-server connections
        new Thread(new Runnable() {
            @Override
            public void run() {
                System.out.println("Accepting server connections on " + serverServerEndp.getPort());
                serverListener.listenSocket(serverServerEndp.getPort());
            }
        }).start();

        while(true) {
            // flush any outgoing messages
            sc.flushOutgoingMessages();

            // handle all incoming messages
            sc.handleNextMessage();

            System.out.println("Clients: " + sc.getConnectedClients().size());
            System.out.println("Servers: " + sc.getConnectedServers().size());
        }
    }

    public static void main(String[] args) {
        if(args.length < 2) {
            System.out.println("Usage: server.jar <clientNodeListenPort> <serverNodeListenPort>  [debug]");
            System.exit(1);
        }
        int clientServerPort = Integer.parseInt(args[0]);
        int serverServerPort = Integer.parseInt(args[1]);

        DasServer.DEBUG = args.length > 2;

        new DasServer(
                new Endpoint("localhost", clientServerPort),
                new Endpoint("localhost", serverServerPort)
        );
    }
}
