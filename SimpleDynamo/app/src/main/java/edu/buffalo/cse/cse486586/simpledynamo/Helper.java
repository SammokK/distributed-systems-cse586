package edu.buffalo.cse.cse486586.simpledynamo;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Formatter;

import static edu.buffalo.cse.cse486586.simpledynamo.ServerTask.hashMe;
import static edu.buffalo.cse.cse486586.simpledynamo.ServerTask.hashPredecessor;
import static edu.buffalo.cse.cse486586.simpledynamo.ServerTask.hashSuccessor;
import static edu.buffalo.cse.cse486586.simpledynamo.SimpleDynamoProvider.myPort;
import static edu.buffalo.cse.cse486586.simpledynamo.SimpleDynamoProvider.predecessorPort;
import static edu.buffalo.cse.cse486586.simpledynamo.SimpleDynamoProvider.successorPort;

/**
 * Created by smokey on 3/26/16.
 */
public class Helper {

    private static Object lock = new Object();
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

    public boolean sendMessage(Message message, String port) {
        Log.d(TAG, "sending message " + message + "on port " + port);
        Socket sendSocket = null;
        try {
            message.setHopCount(message.getHopCount() + 1);
            message.setSenderPort(myPort);
            sendSocket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                    Integer.parseInt(port));
            ObjectOutputStream oos = new ObjectOutputStream(sendSocket.getOutputStream());
            oos.writeObject(message);
            oos.flush();
            oos.reset();
            sendSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, Log.getStackTraceString(e));
            return false;
        } finally {
            if (sendSocket != null && !sendSocket.isClosed()) {
                try {
                    sendSocket.close();
                } catch (IOException e) {
                    Log.e(TAG, Log.getStackTraceString(e));
                }
            }
        }

        Log.i(TAG, "sent message " + message + "on port " + port);
        return true;
    }


    /**
     * extremely dirty way to redirect network operations off the main thread to get around android strict mode.
     * Nobody cares about performance in this assignment. In an ideal world this should be truly asynchronous.
     * For now, will make the main thread wait for this to get over.
     */
    public static boolean asyncSendMessage(Message message, String port) {
        Log.d(TAG, "Inside asyncSendMessage " + message + "on port " + port);
        CallableSendMessage callableSendMessage = new CallableSendMessage();
        callableSendMessage.setMessage(message);
        callableSendMessage.setPortNumber(port);
        callableSendMessage.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, null);
        return true;
    }

    public static boolean isItMyNode(String key) {
        String hashedKey = null;
        try {
            hashedKey = genHash(key);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        if (hashMe.compareTo(hashPredecessor) < 0) {
            if (hashMe.compareTo(hashedKey) > 0 || hashedKey.compareTo(hashPredecessor) > 0) {
                return true;
            } else {
                return false;
            }
        }
        if (hashMe.compareTo(hashedKey) > 0 && hashedKey.compareTo(hashPredecessor) > 0) {
            return true;
        } else {
            return false;
        }
    }

    //tells which node the insert,delete, query is supposed to happen. Use this logic with the lookup successor to figure out replication logic
    public static String whichNode(String key) {




        return null;
    }

    private String lookupSuccessor(String port) {
        String successorPort = null;
        //Calculate predecessor and successor ports
        if (myPort.equalsIgnoreCase("11116")) {
            successorPort = "11120";
        } else if (myPort.equalsIgnoreCase("11120")) {
            successorPort = "11124";
        } else if (myPort.equalsIgnoreCase("11124")) {
            successorPort = "11112";
        } else if (myPort.equalsIgnoreCase("11112")) {
            successorPort = "11108";
        } else if (myPort.equalsIgnoreCase("11108")) {
            successorPort = "11116";
        }
        return successorPort;
    }

    private String lookupPredecessor(String port) {
        String successorPort = null;
        //Calculate predecessor and successor ports
        if (myPort.equalsIgnoreCase("11116")) {
            successorPort = "11120";
        } else if (myPort.equalsIgnoreCase("11120")) {
            successorPort = "11124";
        } else if (myPort.equalsIgnoreCase("11124")) {
            successorPort = "11112";
        } else if (myPort.equalsIgnoreCase("11112")) {
            successorPort = "11108";
        } else if (myPort.equalsIgnoreCase("11108")) {
            successorPort = "11116";
        }
        return successorPort;
    }


    public long insert(ContentValues values, SQLiteDatabase myDatabase) {
        Log.i("INSERT helper", "Inserting... " + values);
        long returner = 0;
        synchronized (lock) {
            returner = myDatabase.insertWithOnConflict(Constants.SIMPLE_DHT, null, values, SQLiteDatabase.CONFLICT_REPLACE);
        }
        return returner;
    }

    public Cursor query(SQLiteDatabase myDatabase) {
        Cursor cursor = null;
        synchronized (lock) {
            cursor = myDatabase.query(true, Constants.SIMPLE_DHT, new String[]{Constants.KEY, Constants.VALUE}, null, null, null, null, null, null);
        }
        return cursor;
    }

    public Cursor query(String selection, SQLiteDatabase myDatabase) {
        Cursor cursor = null;
        synchronized (lock) {
            cursor = myDatabase.query(true, Constants.SIMPLE_DHT, new String[]{Constants.KEY, Constants.VALUE}, Constants.KEY + " = ?", new String[]{selection}, null, null, null, null);
        }
        return cursor;
    }

    public void recalculateHashValues() {
        synchronized (lock) {
            try {
                hashMe = genHash(String.valueOf(Integer.parseInt(myPort) / 2));
                hashSuccessor = null;
                if (successorPort != null) {
                    hashSuccessor = genHash(String.valueOf(Integer.parseInt(successorPort) / 2));
                }
                hashPredecessor = null;
                if (predecessorPort != null) {
                    hashPredecessor = genHash(String.valueOf(Integer.parseInt(predecessorPort) / 2));
                }
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
        }
    }
}