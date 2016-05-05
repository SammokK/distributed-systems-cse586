package edu.buffalo.cse.cse486586.simpledynamo;

import android.content.ContentValues;
import android.database.Cursor;
import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;

import static edu.buffalo.cse.cse486586.simpledynamo.Helper.genHash;
import static edu.buffalo.cse.cse486586.simpledynamo.SimpleDynamoProvider.myDatabase;
import static edu.buffalo.cse.cse486586.simpledynamo.SimpleDynamoProvider.myPort;
import static edu.buffalo.cse.cse486586.simpledynamo.SimpleDynamoProvider.predecessorPort;
import static edu.buffalo.cse.cse486586.simpledynamo.SimpleDynamoProvider.successorPort;

/**
 * Created by smokey on 3/28/16.
 */
public class ServerTask extends AsyncTask<ServerSocket, String, Void> {

    public static String hashSuccessor = null;
    public static  String hashPredecessor = null;
    public static String hashNewNode = null;
    public static String hashMe = null;

    static String TAG = ServerTask.class.getSimpleName();

    @Override
    protected Void doInBackground(ServerSocket... sockets) {
        ServerSocket serverSocket = null;
        try {
            serverSocket = new ServerSocket(10000);
        } catch (IOException e) {
            e.printStackTrace();
        }
        Socket socket = null;
        while (true) {
            try {
                TAG = ServerTask.class.getSimpleName();
                socket = null;
                Log.i(TAG, "Waiting for new sockets on 10000...");
                hashMe = genHash(String.valueOf(Integer.parseInt(myPort) / 2));
                hashSuccessor = null;
                if (successorPort != null) {
                    hashSuccessor = genHash(String.valueOf(Integer.parseInt(successorPort) / 2));
                }
                hashPredecessor = null;
                if (predecessorPort != null) {
                    hashPredecessor = genHash(String.valueOf(Integer.parseInt(predecessorPort) / 2));
                }
                socket = serverSocket.accept();
                Log.i(TAG, "Accepted socket" +
                        socket);
                ObjectInputStream objectInputStream = new ObjectInputStream(socket.getInputStream());
                Object object = objectInputStream.readObject();
                Message message = (Message) object;
                Log.i(TAG, "Received message " + message + " on socket " + socket);

                TAG = ServerTask.class.getSimpleName() + " " + message.getDebugCode();
                if (!socket.isClosed()) {
                    socket.close();
                }
                socket = null;

                hashNewNode = null;
                if (message.getOriginPort() != null) {
                    hashNewNode = genHash(String.valueOf(Integer.parseInt(message.getOriginPort()) / 2));
                }
                switch (message.getType()) {


                    case delete:
                        while(SimpleDynamoProvider.recoveryMode) {
                            try {
                                Log.i("DELETE_SERVER_TASK", "Sleeping on recovery mode");
                                Thread.sleep(100);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                        Log.i("DELETE_SERVER_TASK", "Received an delete request" + message);
                    myDatabase.delete(Constants.SIMPLE_DYNAMO, Constants.KEY + "=?", new String[]{message.getData()});
                        break;

                    case insert:
                        while(SimpleDynamoProvider.recoveryMode) {
                            try {
                                Log.i("INSERT_PROVIDER", "Sleeping on recovery mode");
                                Thread.sleep(100);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                        Log.i("INSERT", "Received an insert request" + message);
                        HashMap<String, String> insertMap = message.getMessageMap();
                        //directly insert into this node.

                        ContentValues values = new ContentValues();
                        values.put(Constants.KEY, insertMap.get(Constants.KEY));
                        values.put(Constants.VALUE, insertMap.get(Constants.VALUE));
                        new Helper().insert(values, myDatabase);
                        Log.i("INSERT", "Inserted message" + message);
                        break;
                    case recoveryQueryStar:
                        Log.i("RECOVERY_QUERY_STAR", "Received recovery query star request object");
                    case queryStar:
                        while(SimpleDynamoProvider.recoveryMode) {
                            try {
                                Log.i("INSERT_PROVIDER", "Sleeping on recovery mode");
                                Thread.sleep(100);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                        Log.i("QUERY_STAR_REQUEST", "Received query star request object");
                        HashMap<String, String> queryStarMap = new HashMap<String, String>();
                        //query everything in this node

                        Cursor cursor = new Helper().query(myDatabase);
                        HashMap<String, String> map = new HashMap<String, String>();
                        while (cursor.moveToNext()) {
                            map.put(cursor.getString(cursor.getColumnIndex(Constants.KEY)), cursor.getString(cursor.getColumnIndex(Constants.VALUE)));
                        }

                        Message queryStarReplyMessage = new Message(Message.MessageType.queryStarResult, myPort);
                        if(message.getType()== Message.MessageType.recoveryQueryStar) {
                            queryStarReplyMessage.setType(Message.MessageType.recoveryQueryStarResult);
                        }
                        queryStarReplyMessage.setMessageMap(map);
                        new Helper().sendMessage(queryStarReplyMessage, message.getOriginPort());
                        Log.i("QUERY_RESULT", "query result " +map);
                        break;
                    case recoveryQueryStarResult :
                        --SimpleDynamoProvider.recoveryCounter;
                    case queryStarResult:
//                        Log.i("QUERY_STAR_RESULT", "Received query star result object with result map" + message.getMessageMap());
                        SimpleDynamoProvider.queryStarMessage.getMessageMap().putAll(message.getMessageMap());
                        SimpleDynamoProvider.queryStarMessage.setQueryStarCount(SimpleDynamoProvider.queryStarMessage.getQueryStarCount() + 1);
                        break;
                    case queryResult:
                        Log.i("QUERY_RESULT", "Received returned query result object with result map" + message.getMessageMap());
                        if(!(message.getMessageMap()==null || message.getMessageMap().size()==0)) {
                            SimpleDynamoProvider.queryMap.put(message.getMessageMap().get(Constants.KEY), message.getMessageMap().get(Constants.VALUE));
                        } else {
                            Log.w("QUERY_RESULT", "Received empty query result object!!!" + message.getMessageMap());
                        }
                        break;
                    case query:

                        while(SimpleDynamoProvider.recoveryMode) {
                            try {
                                Log.i("INSERT_PROVIDER", "Sleeping on recovery mode");
                                Thread.sleep(100);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                        Log.i("QUERY", "Received query request" + message.getData());
                        Cursor queryCursor = null;
                        int tries = 0;
                        do {
                            try {
                                Thread.sleep(100);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            queryCursor = new Helper().query(message.getData(), myDatabase);
                            Log.i("QUERY_LOOP", "inside brute force loop for key=" + message.getData() + ", iteration=" + tries);
                            tries++;
                            if(tries==30) {
                                Log.i("QUERY_TIME_OUT", "Timed out for query key=" + message.getData());
                                break;
                            }
                        }
                        while (queryCursor.getColumnIndex(Constants.VALUE) == -1 && tries<30);

                        HashMap<String, String> queryMap = new HashMap<String, String>();
                        queryCursor.moveToPosition(-1);
                        while (queryCursor.moveToNext()) {
                            // Extract data.
                            queryMap.put(Constants.KEY, queryCursor.getString(queryCursor.getColumnIndex(Constants.KEY)));
                            queryMap.put(Constants.VALUE, queryCursor.getString(queryCursor.getColumnIndex(Constants.VALUE)));
                        }
                        message.setType(Message.MessageType.queryResult);
                        message.setMessageMap(queryMap);
                        new Helper().sendMessage(message, message.getOriginPort());
                        Log.i("QUERY_TIME_OUT", "Sent the message " + message.getMessageMap() + " back to " + message.getOriginPort());
                        break;
                    default:
                        Log.e(TAG, "unimplemented type." + message);
                }
//                publishProgress(message.toString());

            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            } finally {
                try {
                    if (socket != null && !socket.isClosed()) {
                        socket.close();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        }
    }

}


