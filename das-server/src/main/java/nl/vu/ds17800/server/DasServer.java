package nl.vu.ds17800.server;
import nl.vu.ds17800.core.networking.Endpoint;
import nl.vu.ds17800.core.networking.KeepAlive;
import nl.vu.ds17800.core.networking.Message;

import java.io.IOException;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;

/**
 * @author lngtr
 * @since 2017-11-16
 */
public class DasServer {
    public static boolean DEBUG = false;

    public static Random RANDOM;

    public final static List<Endpoint> SERVERS = Arrays.asList(
            new Endpoint("localhost", 20100),
            new Endpoint("localhost", 20101),
            new Endpoint("localhost", 20102),
            new Endpoint("localhost", 20103),
            new Endpoint("localhost", 20104)
    );

    private final ServerController serverController;

    public DasServer(final Endpoint serverClientEndp, final Endpoint serverServerEndp) {
        RANDOM = new Random(serverClientEndp.getPort());
        serverController = new ServerController();

        final SocketListener clientListener = new SocketListener(new IConnectionHandler() {
            @Override
            public void handle(Socket s) {
                new Thread(new ClientWorker(s, serverController)).start();
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

        // For the sake of symmetry the server connections are two-ways, there
        // will be two sockets open per server peer, initiated from either sides.
        // On these sockets, we receive messages on behalf of foreign clients.
        for (Endpoint endp : SERVERS) {
            if (endp.equals(serverServerEndp)) {
                // don't connect to ourselves
                continue;
            }

            connectServer(endp);
        }

        final SocketListener serverListener = new SocketListener(new IConnectionHandler() {
            @Override
            public void handle(Socket s) {
                ServerOutWorker sow = new ServerOutWorker(s, serverController);
                KeepAlive keepAlive = new KeepAlive(sow);
                keepAlive.start();
                new Thread(sow).start();
            }
        });

        // Listener thread for incoming server-server connections. On this socket
        // we send our client actions.
        new Thread(new Runnable() {
            @Override
            public void run() {
                System.out.println("Accepting server connections on " + serverServerEndp.getPort());
                serverListener.listenSocket(serverServerEndp.getPort());
                System.err.println("serverListener died!! ");
            }
        }).start();

        while(true) {
            // flush any outgoing messages
            serverController.flushOutgoingMessages();

            // handle all incoming messages
            serverController.consumeIncomingMessages();

            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {}

        }
    }

    private void connectServer(final Endpoint endp) {

        new Thread(new Runnable() {
            @Override
            public void run() {
                while(true) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e1) {}
                    Socket s = null;
                    try {
                        s = new Socket(endp.getHost(), endp.getPort());
                    } catch (IOException e) {

                        //retry
                        continue;
                    }

                    final ServerInWorker siw = new ServerInWorker(s, serverController);

                    KeepAlive keepAlive = new KeepAlive(siw);

                    keepAlive.start();
                    siw.run();
                    keepAlive.stop();

                    System.err.println("Lost connection! Will reconnect");
                }
            }
        }).start();
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
