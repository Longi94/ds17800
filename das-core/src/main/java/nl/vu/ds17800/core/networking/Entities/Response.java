package nl.vu.ds17800.core.networking.Entities;

import nl.vu.ds17800.core.networking.Communication;

import java.util.List;

public class Response {
    private String messageKey;
    private List<Message> responseBuffer;

    public Response(String key, List<Message> buffer){
        messageKey = key;
        responseBuffer = buffer;
    }

    public Message getResponse(int timeout, Server dest) throws InterruptedException {

            long curTime = System.currentTimeMillis();
            while(true){
                synchronized (responseBuffer){
                    for(Message message: responseBuffer){
                        if(((String)message.get(Communication.KEY_COMM_ID)).equals(messageKey)){
                            responseBuffer.remove(message);
                            return message;
                        }
                    }
                    long iterTime = System.currentTimeMillis();
                    if( Math.abs(iterTime - curTime ) > timeout) {
                        System.out.println("Timed out while waiting for answer from: " + dest);
                        throw new InterruptedException();
                    }
                }
                Thread.sleep(200);
        }

    }
}
