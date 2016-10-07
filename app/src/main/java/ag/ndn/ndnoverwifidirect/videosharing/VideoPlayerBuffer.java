package ag.ndn.ndnoverwifidirect.videosharing;

import android.util.Log;

import java.util.LinkedList;

/**
 * Class that represents a buffer for buffering incoming packets from the
 * network - specifically those used by VideoPlayer.
 *
 * Created by allengong on 9/29/16.
 */

public class VideoPlayerBuffer {

    // finals
    public static final int MAX_ITEM_SIZE = 32000;     // in bytes
    private static final int MAX_CACHED_ITEMS = 10;

    private LinkedList<byte[]> buffer;
    private boolean eofReached = false;

    public VideoPlayerBuffer() {
        buffer = new LinkedList<>();
    }

    // returns true if bytes were added to buffer, false otherwise
    public boolean addToBuffer(byte[] bytes) {
        if (buffer.size() == MAX_CACHED_ITEMS) {
            return false;
        }

        buffer.add(bytes);
        return true;
    }

    // get's the next buffered packet content, null if none available,
    // or an empty byte[] of length 0 if EOF
    public byte[] getFromBuffer() {
        if (eofReached) {
            return new byte[0];
        }

        if (buffer.isEmpty()) {
            return null;
        } else {
            return buffer.removeFirst();
        }
    }

    public void clearBuffer() {
        buffer.clear();
    }

    public void notifyEofReached() {
        eofReached = true;
    }
}
