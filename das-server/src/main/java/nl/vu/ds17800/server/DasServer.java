package nl.vu.ds17800.server;
import nl.vu.ds17800.core.networking.Endpoint;

/**
 * @author lngtr
 * @since 2017-11-16
 */
public class DasServer {

    public DasServer(final Endpoint serverClientEndp, final Endpoint serverServerEndp) {
        final ServerController sc = new ServerController();
        final ClientListener cl = new ClientListener(sc);
        final ServerListener sl = new ServerListener(sc);

        // listener thread for incoming client-server connections
        new Thread(new Runnable() {
            @Override
            public void run() {
                cl.listenSocket(serverClientEndp.getPort());
            }
        }).start();

        // listener thread for incoming server-server connections
        new Thread(new Runnable() {
            @Override
            public void run() {
                sl.listenSocket(serverServerEndp.getPort());
            }
        }).start();

        while(true) {
            sc.handleNextMessage();
        }
    }

    public static void main(String[] args) {
        if(args.length < 1) {
            System.out.println("Usage: server.jar 10100|10101|10102|10103|10104 [debug]");
            System.exit(1);
        }
        int port = Integer.parseInt(args[0]);
        Endpoint serverDescr = new Endpoint("localhost", port);
        new DasServer(serverDescr);
    }
}
