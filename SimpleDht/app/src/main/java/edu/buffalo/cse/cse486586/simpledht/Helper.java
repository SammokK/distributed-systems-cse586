package edu.buffalo.cse.cse486586.simpledht;

import android.util.Log;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Formatter;
import java.util.concurrent.ExecutorService;

/**
 * Created by smokey on 3/26/16.
 */
public class Helper {

    static final String TAG = Helper.class.getSimpleName();

/*    private Socket[] initializeSockets() {
        Socket[] serverSockets = new Socket[Constants.REMOTE_PORTS.length];
        try {
            for (int i = 0; i < Constants.REMOTE_PORTS.length; i++) {
                if (serverSockets[i] == null) {
                    serverSockets[i] = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            Integer.parseInt(Constants.REMOTE_PORTS[i]));
                }
                Log.v(TAG, "Creating socket " + serverSockets[i] + " for the first time...");
            }
        } catch (IOException e) {
            Log.e(TAG, "Some exception when trying to create sockets");
            Log.e(TAG, Log.getStackTraceString(e));
        }
        return serverSockets;
    }*/

    public static String genHash(String input) throws NoSuchAlgorithmException {
        MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
        byte[] sha1Hash = sha1.digest(input.getBytes());
        Formatter formatter = new Formatter();
        for (byte b : sha1Hash) {
            formatter.format("%02x", b);
        }
        return formatter.toString();
    }

    public static boolean sendMessage(Message message, String port) {
        try {
            message.setHopCount(message.getHopCount() + 1);
            Socket sendSocket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                    Integer.parseInt(port));
            ObjectOutputStream oos = new ObjectOutputStream(sendSocket.getOutputStream());
            oos.writeObject(message);
            oos.flush();
        } catch (IOException e) {
            Log.e(TAG, Log.getStackTraceString(e));
            return false;
        }
        return true;
    }


    /**
     *     extremely dirty way to redirect network operations off the main thread to get around android strict mode.
     *     Nobody cares about performance in this assignment. In an ideal world this should be truly asynchronous.
     *     For now, will make the main thread wait for this to get over.
     */
    public static boolean asyncSendMessage(Message message, String port) {
        ExecutorService pool;


        return true;
    }
}
