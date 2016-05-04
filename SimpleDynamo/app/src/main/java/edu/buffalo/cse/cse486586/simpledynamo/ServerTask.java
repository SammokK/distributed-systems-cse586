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

                    case godJoin:
                        Log.i(TAG, "God: received a join request " + message);
                        //check if i'm god
                        if (Constants.god.equalsIgnoreCase(SimpleDynamoProvider.myPort)) {
                            Log.d(TAG, "God: Yes I'm God.");
                            if (successorPort != null) {
                                //check if my predecessor's value is higher than mine
                                if (hashMe.compareTo(hashPredecessor) < 0) {
                                    if (hashMe.compareTo(hashNewNode) > 0 || hashNewNode.compareTo(hashPredecessor) > 0) {
                                        Log.i(TAG, "God: inserting node between me and predecessor.");
                                        insertNodeBehindMe(message);
                                        break;
                                    } else {
                                        //not meant for me. Forward untouched message to my successor
                                        Log.i(TAG, "forward the message to successor");
                                        message.setType(Message.MessageType.slaveJoin);
                                        new Helper().sendMessage(message, successorPort);
                                        break;
                                    }
                                }
                                //Yes, God has a successor, this is not the first join.
//                                else
                                if (hashMe.compareTo(hashNewNode) > 0 && hashNewNode.compareTo(hashPredecessor) > 0) {
                                    Log.i(TAG, "God: Inserting node between me and successor.");
                                    insertNodeBehindMe(message);
                                } else {
                                    Log.i(TAG, "God: sending a slave join message to successor.");
                                    message.setType(Message.MessageType.slaveJoin);
                                    new Helper().sendMessage(message, successorPort);
                                }
                            } else {
                                //This is the first join ever. Just insert the node here.
                                Log.i(TAG, "God: does not have a successor yet. Setting message's origin port to my successor and predecessor fields");
                                predecessorPort = message.getOriginPort();
                                successorPort = message.getOriginPort();

                                message.setType(Message.MessageType.chSuccAndPred);
                                message.setNewSuccessor(myPort);
                                message.setNewPredecessor(myPort);
                                new Helper().sendMessage(message, message.getOriginPort());
                            }
                        } else {
                            Log.e(TAG, "I'm not God, why did you send me a join request?");
                            continue;
                        }
                        break;
                    case slaveJoin:
                        //this is a message handed down from God to join a node
                        if (hashMe.compareTo(hashPredecessor) < 0) {
                            if (hashMe.compareTo(hashNewNode) > 0 || hashNewNode.compareTo(hashPredecessor) > 0) {
                                Log.i(TAG, "Inserting node between me and predecessor.");
                                insertNodeBehindMe(message);
                                break;
                            }else {
                                //not meant for me. Forward untouched message to my successor
                                Log.i(TAG, "forward the message to successor");
                                new Helper().sendMessage(message, successorPort);
                                break;
                            }
                        }
                        if (hashMe.compareTo(hashNewNode) > 0 && hashNewNode.compareTo(hashPredecessor) > 0) {
                            //node should be added between successor and me
                            Log.i(TAG, "Node should be added between me and successor here");
                            insertNodeBehindMe(message);
                        } else {
                            //not meant for me. Forward untouched message to my successor
                            Log.i(TAG, "forward the message to successor");
                            new Helper().sendMessage(message, successorPort);
                        }
                        break;

                    case chSuccAndPred:
                        successorPort = message.getNewSuccessor();
                        predecessorPort = message.getNewPredecessor();
                        Log.i(TAG, "type chSuccAndPred, new successor, predecessor-> " + successorPort + " , " + predecessorPort);
                        break;

                    case chPredecessor:
                        predecessorPort = message.getNewPredecessor();
                        Log.i(TAG, "type chPredecessor, new successor, predecessor-> " + successorPort + " , " + predecessorPort);
                        break;
                    case chSuccessor:
                        successorPort = message.getNewSuccessor();
                        Log.i(TAG, "type chSuccessor, new successor, predecessor-> " + successorPort + " , " + predecessorPort);
                        break;
                    case insert:
                        Log.i("INSERT", "Received an insert request" + message);
                        HashMap<String, String> insertMap = message.getMessageMap();
//                        if (isItMyNode(insertMap.get(Constants.KEY))) {
//                            Log.i("INSERT", "Inserting at my node " + message);
//
//                            ContentValues values = new ContentValues();
//                            values.put(Constants.KEY, insertMap.get(Constants.KEY));
//                            values.put(Constants.VALUE, insertMap.get(Constants.VALUE));
//                            new Helper().insert(values, myDatabase);
//                        } else {
//                            Log.i("INSERT", "Sending insert to successor " + message);
//                            new Helper().sendMessage(message, successorPort);
//                        }



                        //directly insert into this node.

                        ContentValues values = new ContentValues();
                        values.put(Constants.KEY, insertMap.get(Constants.KEY));
                        values.put(Constants.VALUE, insertMap.get(Constants.VALUE));
                        new Helper().insert(values, myDatabase);
                        Log.i("INSERT", "Inserted message" + message);
                        break;
                    case queryStar:
                        Log.i("QUERY_STAR_REQUEST", "Received QUERY  query star result object with map map"  + message.getMessageMap());
                        HashMap<String, String> queryStarMap = new HashMap<String, String>();
                        //query everything in this node

                        Cursor cursor = new Helper().query(myDatabase);
                        HashMap<String, String> map = new HashMap<String, String>();
                        while (cursor.moveToNext()) {
                            map.put(cursor.getString(cursor.getColumnIndex(Constants.KEY)), cursor.getString(cursor.getColumnIndex(Constants.VALUE)));
                        }

                        Message queryStarReplyMessage = new Message(Message.MessageType.queryStarResult, myPort);
                        queryStarReplyMessage.setMessageMap(map);
                        new Helper().sendMessage(queryStarReplyMessage, message.getOriginPort());
                        Log.i("QUERY_RESULT", "query result " +map);
                        break;
                    case queryStarResult:
                        Log.i("QUERY_STAR_RESULT", "Received query star result object with result map" + message.getMessageMap());
                        SimpleDynamoProvider.queryStarMessage.getMessageMap().putAll(message.getMessageMap());
                        SimpleDynamoProvider.queryStarMessage.setQueryStarCount(SimpleDynamoProvider.queryStarMessage.getQueryStarCount() + 1);
                        break;
                    case queryResult:
                        Log.i("QUERY RESULT", "Received returned query result object with result map" + message.getMessageMap());
                        SimpleDynamoProvider.queryMap.put(message.getMessageMap().get(Constants.KEY), message.getMessageMap().get(Constants.VALUE));
                        break;
                    case query:
                        Log.i("QUERY", "Received query request" + message.getData());
                        Cursor queryCursor = new Helper().query(message.getData(), myDatabase);
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

    private void insertNodeBehindMe(Message message) {
        Log.i(TAG, "Inserting node " + message.getOriginPort() + " between me " + myPort + " and predecessor " + predecessorPort);
        //set new node as my predecessor.
        String formerPredecessor = predecessorPort;
        predecessorPort = message.getOriginPort();
        //tell new node to set it's predecessor to my former predecessor, and it's successor to me.
        message.setType(Message.MessageType.chSuccAndPred);
        message.setNewPredecessor(formerPredecessor);
        message.setNewSuccessor(myPort);
        new Helper().sendMessage(message, message.getOriginPort());
        //tell my former predecessor to point it's successor to new node's port.
        message.setType(Message.MessageType.chSuccessor);
        message.setNewSuccessor(message.getOriginPort());
        new Helper().sendMessage(message, formerPredecessor);
    }

    private void insertNode(Message message) {
        Log.i(TAG, "Inserting node " + message.getOriginPort() + " between me " + myPort + " and successor " + successorPort);
        //add origin port as my successor. My former successor is null if I am god at startup.
        String formerSuccessor = successorPort;
        successorPort = message.getOriginPort();
        if (formerSuccessor != null) {
            //if i'm not god, tell successor to set it's successor to my former successor, and it's predecessor to me.
            message.setType(Message.MessageType.chSuccAndPred);
            message.setNewSuccessor(formerSuccessor);
            message.setNewPredecessor(myPort);
            new Helper().sendMessage(message, message.getOriginPort());
        } else {
            //if i don't have a former successor, this is god at startup. Then tell new node to point both successor and predecessor to me.
            message.setType(Message.MessageType.chSuccAndPred);
            message.setNewSuccessor(myPort);
            message.setNewPredecessor(myPort);
            new Helper().sendMessage(message, message.getOriginPort());
        }
        //3. tell my former successor(If i have one. At startup, god won't have one) to point it's predecessor to new node's port.
        //If i don't have a former successor, no need to tell anyone anything.
        if (formerSuccessor != null) {
            message.setType(Message.MessageType.chPredecessor);
            message.setNewPredecessor(message.getOriginPort());
            new Helper().sendMessage(message, formerSuccessor);
        }
    }
/*
    protected void onProgressUpdate(String... strings) {

            *//*
             * The following code displays what is received in doInBackground().
             *//*
        String strReceived = strings[0].trim();
        TextView remoteTextView = (TextView) findViewById(R.id.textView1);
        remoteTextView.append(strReceived + "\t\n");
        TextView localTextView = (TextView) findViewById(R.id.textView1);
        localTextView.append("\n");



            *//*
             * The following code creates a file in the AVD's internal storage and stores a file.
             *
             * For more information on file I/O on Android, please take a look at
             * http://developer.android.com/training/basics/data-storage/files.html
             *//*

        String string = strReceived + "\n";

        try {
        } catch (Exception e) {
            Log.e(TAG, "File write failed");
        }
        return;
    }*/
}


