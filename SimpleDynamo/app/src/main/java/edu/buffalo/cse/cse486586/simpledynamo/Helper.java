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

        if(input == null) {
        }
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
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "Exception occurred while sending to port  " + port + ", message " + message );
            Log.e(TAG, Log.getStackTraceString(e));
            return false;
        }
        finally {
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

    public static String findNode(String key) {
        Log.d("FIND_NODE", "Input with key=" + key );
        String returnerValue = null;
        for (int i = 0; i<Constants.REMOTE_PORTS_IN_CONNECTED_ORDER.length; i++) {
            String port = Constants.REMOTE_PORTS_IN_CONNECTED_ORDER[i];
            if (isItThisNode(port, key)) {
                Log.d("FIND_NODE", "For key=" + key + " , node is " + port);
                return port;
            }
        }
        if (returnerValue == null) {
            new Exception("NO PORT FOUND").printStackTrace();
            Log.e(TAG + " NO PORT FOUND", "No port found for key= " + key);
            throw new RuntimeException("ERROR: no port found");
        }
        return null;
    }

//    public static String findNode(String key) {
//        Log.i("FIND_NODE", "Input with key=" + key );
//        String returnerValue = null;
//        String hashKey;
//        try {
//            hashKey = genHash(key);
//        } catch (NoSuchAlgorithmException e) {
//            e.printStackTrace();
//            throw new RuntimeException("genhash error");
//        }
//        for(int i = 0; i<4; i++) {
//            if(hashKey.compareTo(Constants.hashedPortsInAscendingOrder[i])<0 && hashKey.compareTo(Constants.hashedPortsInAscendingOrder[i+1]) > 0)  {
//                return Constants.REMOTE_PORTS_IN_CONNECTED_ORDER[i];
//            }
//        }
//        return  Constants.REMOTE_PORTS_IN_CONNECTED_ORDER[4];
//
//
//    }


    public static boolean isItThisNode(String port, String key) {
        Log.d("INSIDE_NODE","Checking if the key " + key + " belongs to node " + port);
        String hashedKey = null;
        String hashedThisNode = null;
        String localHashedPredecessor = null;
        String localHashedSuccessor = null;
        try {
            hashedKey = genHash(key);
            hashedThisNode = genHash(String.valueOf(Integer.parseInt(port) / 2));
            localHashedPredecessor = genHash(String.valueOf(Integer.parseInt(lookupPredecessor(port)) / 2));
//            localHashedSuccessor = genHash(String.valueOf(Integer.parseInt(lookupSuccessor(port)) / 2));
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            throw new RuntimeException("Some problem with hashing");
        }
        if (hashedThisNode.compareTo(localHashedPredecessor) < 0) {
            if (hashedThisNode.compareTo(hashedKey) >= 0 || hashedKey.compareTo(localHashedPredecessor) > 0) {
                Log.d("FIND_NODE","1 Port for " + key + " is " + port) ;
                return true;
            } else {
                return false;
            }
        }
        if (hashedThisNode.compareTo(hashedKey) >= 0 && hashedKey.compareTo(localHashedPredecessor) > 0) {
            Log.d("FIND_NODE","2 Port for " + key + " is " + port) ;
            return true;
        } else {
            return false;
        }
    }

    //the order is 11116 11120 11124 11112 11108
//    / {"11112", "11108", "11116", "11120", "11124"}

    public  static String lookupSuccessor(String port) {
        String successorPort = null;
        //Calculate predecessor and successor ports
        if (port.equalsIgnoreCase("11116")) {
            successorPort = "11120";
        } else if (port.equalsIgnoreCase("11120")) {
            successorPort = "11124";
        } else if (port.equalsIgnoreCase("11124")) {
            successorPort = "11112";
        } else if (port.equalsIgnoreCase("11112")) {
            successorPort = "11108";
        } else if (port.equalsIgnoreCase("11108")) {
            successorPort = "11116";
        }else {
            throw new RuntimeException("Port " + port + " not found!!");
        }
        Log.d("LOOKUP_SUCCESSOR", "Successor of port " + port  + " is  " + successorPort);
        return successorPort;
    }


    //the order is 11116 11120 11124 11112 11108
    public static String lookupPredecessor(String port) {
        String predecessorPort;
        //Calculate predecessor and successor ports
        if (port.equalsIgnoreCase("11116")) {
            predecessorPort = "11108";
        } else if (port.equalsIgnoreCase("11120")) {
            predecessorPort = "11116";
        } else if (port.equalsIgnoreCase("11124")) {
            predecessorPort = "11120";
        } else if (port.equalsIgnoreCase("11112")) {
            predecessorPort = "11124";
        } else if (port.equalsIgnoreCase("11108")) {
            predecessorPort = "11112";
        } else {
            throw new RuntimeException("Port " + port + " not found!!");
        }
        return predecessorPort;
    }


    public long insert(ContentValues values, SQLiteDatabase myDatabase) {
        Log.i("INSERT_HELPER", "Inserting... " + values);
        long returner = 0;
        synchronized (lock) {
            returner = myDatabase.insertWithOnConflict(Constants.SIMPLE_DYNAMO, null, values, SQLiteDatabase.CONFLICT_REPLACE);
        }
        return returner;
    }

    public Cursor query(SQLiteDatabase myDatabase) {
        Cursor cursor = null;
        synchronized (lock) {
            cursor = myDatabase.query(true, Constants.SIMPLE_DYNAMO, new String[]{Constants.KEY, Constants.VALUE}, null, null, null, null, null, null);
        }
        return cursor;
    }

    public Cursor query(String selection, SQLiteDatabase myDatabase) {
        Cursor cursor = null;
        synchronized (lock) {
            cursor = myDatabase.query(true, Constants.SIMPLE_DYNAMO, new String[]{Constants.KEY, Constants.VALUE}, Constants.KEY + " = ?", new String[]{selection}, null, null, null, null);
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