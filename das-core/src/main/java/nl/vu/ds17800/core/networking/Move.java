package nl.vu.ds17800.core.networking;

import nl.vu.ds17800.core.networking.response.SerMessage;

/**
 * Created by hacku on 11/21/17.
 */
public class Move extends SerMessage {
    private short diffx;
    private short diffy;
    private boolean kick;

    public Move(short x, short y){
        diffx = x;
        diffy = y;
    }

    public Move(boolean kicked){
        kick = kicked;
    }

    public boolean isKicked() {
        return kick;
    }

    public short getDiffy() {
        return diffy;
    }

    public short getDiffx() {
        return diffx;
    }
}
