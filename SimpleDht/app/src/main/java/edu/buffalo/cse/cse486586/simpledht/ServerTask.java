package edu.buffalo.cse.cse486586.simpledht;

import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;

import static edu.buffalo.cse.cse486586.simpledht.Helper.genHash;
import static edu.buffalo.cse.cse486586.simpledht.Helper.sendMessage;
import static edu.buffalo.cse.cse486586.simpledht.SimpleDhtProvider.myPort;
import static edu.buffalo.cse.cse486586.simpledht.SimpleDhtProvider.predecessorPort;
import static edu.buffalo.cse.cse486586.simpledht.SimpleDhtProvider.successorPort;

/**
 * Created by smokey on 3/28/16.
 */
public class ServerTask extends AsyncTask<ServerSocket, String, Void> {

    static final String TAG = ServerTask.class.getSimpleName();

    @Override
    protected Void doInBackground(ServerSocket... sockets) {
        ServerSocket serverSocket = null;
        try {
            serverSocket = new ServerSocket(10000);
        } catch (IOException e) {
            e.printStackTrace();
        }
        while (true) {
            try {
                Log.i(TAG, "Waiting for new sockets on 10000...");
                Socket socket = serverSocket.accept();
                Log.i(TAG, "Accepted socket" +
                        socket);
                ObjectInputStream objectInputStream = new ObjectInputStream(socket.getInputStream());
                Log.v(TAG, "Waiting for a message on this socket " + socket);
                Message message = (Message) objectInputStream.readObject();

                switch (message.getType()) {
                    case godJoin:
                        Log.i(TAG, "Received a join request!" + message);
                        //check if i'm god
                        if (Constants.god.equalsIgnoreCase(SimpleDhtProvider.myPort)) {
                            Log.i(TAG, "Yes, I'm God.");
                            //check the hashcode.
                            if (successorPort != null) {
                                if (genHash(myPort).compareTo(message.getOriginPort()) < 0 && genHash(successorPort).compareTo(message.getOriginPort()) > 0) {
                                    Log.i(TAG, "Node should be added between me(god) and successor here");
                                    insertNode(message);
                                } else {
                                    Log.i(TAG, "forward the message to successor");
                                    message.setType(Message.MessageType.slaveJoin);
                                    sendMessage(message, message.getOriginPort());
                                }
                            } else {
                                Log.i(TAG, "God does not have a successor. Setting message's origin to sucecssor and predecessor");
                                predecessorPort = message.getOriginPort();
                                insertNode(message);
                            }
                        } else {
                            Log.e(TAG, "I'm not God, why did you send me a join request?");
                            continue;
                        }
                        break;
                    case slaveJoin:
                        //this is a message handed down from God to join a node
                        if (genHash(myPort).compareTo(message.getOriginPort()) < 0 && genHash(successorPort).compareTo(message.getOriginPort()) > 0) {
                            //node should be added between successor and me
                            Log.i(TAG, "Node should be added between me and successor here");
                            insertNode(message);
                        } else {
                            //not meant for me. Forward untouched message to my successor
                            Log.i(TAG, "forward the message to successor");
                            sendMessage(message, successorPort);
                        }
                        break;

                    case chSuccAndPred:
                        successorPort = message.getNewSuccessor();
                        predecessorPort = message.getNewPredecessor();
                        break;

                    case chPredecessor:
                        predecessorPort = message.getNewPredecessor();
                        break;
                    case chSuccessor:
                        successorPort = message.getNewSuccessor();
                        break;
                    case insert:
                        Log.i(TAG, "Received an insert request" + message);
                        break;
                    default:
                        Log.w(TAG, "unimplemented type." + message);
                }
                publishProgress(message.toString());
                if(!socket.isClosed()) {
                    socket.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
        }
    }

    private void insertNode(Message message) {
        //1. add origin port as my successor. My former successor is null if I am god at startup.
        String formerSuccessor = successorPort;
        successorPort = message.getOriginPort();
        //2. tell successor to set it's successor to my former successor, and it's predecessor to me.
        if(formerSuccessor!=null) {
            message.setType(Message.MessageType.chSuccAndPred);
            message.setNewSuccessor(formerSuccessor);
            message.setNewPredecessor(myPort);
            sendMessage(message, message.getOriginPort());
        } else {
            //if i don't have a former successor, this is god at startup. Then tell new node to point both successor and predecessor to me.
            message.setType(Message.MessageType.chSuccAndPred);
            message.setNewSuccessor(myPort);
            message.setNewPredecessor(myPort);
            sendMessage(message, message.getOriginPort());
        }
        //3. tell my former successor(If i have one. At startup, god won't have one) to point it's predecessor to new node's port.
        //If i don't have a former successor, no need to tell anyone anything.
        if (formerSuccessor != null) {
            message.setType(Message.MessageType.chPredecessor);
            message.setNewPredecessor(message.getOriginPort());
            sendMessage(message, formerSuccessor);
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


