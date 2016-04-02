package edu.buffalo.cse.cse486586.simpledht;

import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;

import static edu.buffalo.cse.cse486586.simpledht.Helper.asyncSendMessage;
import static edu.buffalo.cse.cse486586.simpledht.Helper.genHash;
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
        Socket socket = null;
        while (true) {
            try {
                socket = null;
                Log.i(TAG, "Waiting for new sockets on 10000...");
                Log.i(TAG, "Me: " + myPort + "; successor: " + successorPort + "; predecessor=" + predecessorPort);
                socket = serverSocket.accept();
                Log.i(TAG, "Accepted socket" +
                        socket);
                ObjectInputStream objectInputStream = new ObjectInputStream(socket.getInputStream());
                Object object = objectInputStream.readObject();
                Message message = (Message) object;
                Log.i(TAG, "Received message " + message + " on socket " + socket);
                if(!socket.isClosed()) {
                    socket.close();
                }
                socket = null;
                switch (message.getType()) {
                    case godJoin:
                        Log.i(TAG, "God received a join request " + message);
                        //check if i'm god
                        if (Constants.god.equalsIgnoreCase(SimpleDhtProvider.myPort)) {
                            Log.i(TAG, "Yes, I'm God. I can process this request.");
                            if (successorPort != null) {
                                //Yes, God has a successor, this is not the first join.
                                if (genHash(myPort).compareTo(message.getOriginPort()) < 0 && genHash(successorPort).compareTo(message.getOriginPort()) > 0) {
                                    Log.i(TAG, "Node should be added between me(god) and successor here");
                                    insertNode(message);
                                } else {
                                    Log.i(TAG, "I already have a successor. Send a slave join message to successor.");
                                    message.setType(Message.MessageType.slaveJoin);
                                    asyncSendMessage(message, successorPort);
                                }
                            } else {
                                //This is the first join ever. Just insert the node here.
                                Log.i(TAG, "God does not have a successor yet. Setting message's origin to successor and predecessor");
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
                        } else if (successorPort.equalsIgnoreCase(Constants.god)) {
                            Log.i(TAG, "My successor is God. Just insert it between me and God since it hasn't been inserted anywhere else");
                            insertNode(message);
                        } else {
                            //not meant for me. Forward untouched message to my successor
                            Log.i(TAG, "forward the message to successor");
                            asyncSendMessage(message, successorPort);
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

            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            } finally {
                try {
                    if (socket!=null && !socket.isClosed()) {
                        socket.close();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        }
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
            asyncSendMessage(message, message.getOriginPort());
        } else {
            //if i don't have a former successor, this is god at startup. Then tell new node to point both successor and predecessor to me.
            message.setType(Message.MessageType.chSuccAndPred);
            message.setNewSuccessor(myPort);
            message.setNewPredecessor(myPort);
            asyncSendMessage(message, message.getOriginPort());
        }
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        //3. tell my former successor(If i have one. At startup, god won't have one) to point it's predecessor to new node's port.
        //If i don't have a former successor, no need to tell anyone anything.
        if (formerSuccessor != null) {
            message.setType(Message.MessageType.chPredecessor);
            message.setNewPredecessor(message.getOriginPort());
            asyncSendMessage(message, formerSuccessor);
        }
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
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


