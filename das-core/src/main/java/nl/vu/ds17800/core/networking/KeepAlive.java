package nl.vu.ds17800.core.networking;

import java.io.IOException;
import java.net.Socket;

public class KeepAlive {
    private final IMessageSendable sendable;
    private volatile boolean running;

    public final static int INTERVAL = 1000;
    public KeepAlive(IMessageSendable sendable) {
        this.sendable = sendable;
    }

    public void start() {
        running = true;
        new Thread(new Runnable() {
            @Override
            public void run() {
                while(running) {
                    try {
                        Thread.sleep(INTERVAL);
                    } catch (InterruptedException e) {
                        running = false;
                    }
                    try {
                        sendable.sendMessage(Message.nop());
                    } catch (IOException e) {
                        running = false;
                    }
                }
                System.out.println("keep alive stop yay");
            }
        }).start();
    }

    public void stop() {
        running = false;
    }
}
