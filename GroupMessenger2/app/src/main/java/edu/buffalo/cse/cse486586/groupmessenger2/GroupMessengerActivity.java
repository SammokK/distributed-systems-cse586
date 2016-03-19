package edu.buffalo.cse.cse486586.groupmessenger2;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.PriorityBlockingQueue;

/**
 * GroupMessengerActivity is the main Activity for the assignment.
 *
 * @author stevko
 */
public class GroupMessengerActivity extends Activity {
    static final String TAG = GroupMessengerActivity.class.getSimpleName();
    private final ContentValues mContentValues = new ContentValues();

    static final String REMOTE_PORTS[] = {"11108", "11112", "11116", "11120", "11124"};

    Socket serverSockets[] = new Socket[5];

    String myPort = null;
    private static Integer dbCounter = 0;

    private final Uri mUri = buildUri("content", "edu.buffalo.cse.cse486586.groupmessenger2.provider");

    static final int SERVER_PORT = 10000;
    private ContentResolver mContentResolver;

    Double sequence = new Double(0);

    private PriorityBlockingQueue<MessageWrapper> queue = new PriorityBlockingQueue<MessageWrapper>(5, MessageWrapper.messageWrapperComparator);
    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {


        mContentResolver = getContentResolver();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_messenger);

        /*
         * DONE: Use the TextView to display your messages. Though there is no grading component
         * on how you display the messages, if you implement it, it'll make your debugging easier.
         */
        TextView tv = (TextView) findViewById(R.id.textView1);
        tv.setMovementMethod(new ScrollingMovementMethod());

        /*
         * Registers OnPTestClickListener for "button1" in the layout, which is the "PTest" button.
         * OnPTestClickListener demonstrates how to access a ContentProvider.
         */
        findViewById(R.id.button1).setOnClickListener(
                new OnPTestClickListener(tv, getContentResolver()));

        /*
         * TODO: You need to register and implement an OnClickListener for the "Send" button.
         * In your implementation you need to get the message from the input box (EditText)
         * and send it to other AVDs.
         */

        final EditText editText = (EditText) findViewById(R.id.editText1);
        Button sendButton = (Button) findViewById((R.id.button4));

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String message = editText.getText().toString();
                message.trim();
                TextView tv = (TextView) findViewById(R.id.textView1);

                tv.append(message + "\n");

                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, message, null);
                editText.setText("");
            }
        });



        /*
         * Calculate the *.port number that this AVD listens on.
         * It is just a hack that I came up with to get around the networking limitations of AVDs.
         * The explanation is provided in the PA1 spec.
         */
        TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        myPort = String.valueOf((Integer.parseInt(portStr) * 2));

        try {
            /*
             * Create a server socket as well as a thread (AsyncTask) that listens on the server
             * port.
             *
             * AsyncTask is a simplified thread construct that Android provides. Please make sure
             * you know how it works by reading
             * http://developer.android.com/reference/android/os/AsyncTask.html
             */
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
        } catch (IOException e) {
            Log.e(TAG, "Can't create a ServerSocket");
            return;
        }

        /*
         * Retrieve a pointer to the input box (EditText) defined in the layout
         * XML file (res/layout/main.xml).
         *
         * This is another example of R class variables. R.id.edit_text refers to the EditText UI
         * element declared in res/layout/main.xml. The id of "edit_text" is given in that file by
         * the use of "android:id="@+id/edit_text""
         */
        final EditText editText1 = (EditText) findViewById(R.id.editText1);

        /*
         * Register an OnKeyListener for the input box. OnKeyListener is an event handler that
         * processes each sequence event. The purpose of the following code is to detect an enter sequence
         * press event, and create a client thread so that the client thread can send the string
         * in the input box over the network.
         */
        editText1.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if ((event.getAction() == KeyEvent.ACTION_DOWN) &&
                        (keyCode == KeyEvent.KEYCODE_ENTER)) {
                    /*
                     * If the sequence is pressed (i.e., KeyEvent.ACTION_DOWN) and it is an enter sequence
                     * (i.e., KeyEvent.KEYCODE_ENTER), then we display the string. Then we create
                     * an AsyncTask that sends the string to the remote AVD.
                     */
                    String msg = editText1.getText().toString().trim();
                    editText1.setText(""); // This is one way to reset the input box.
                    TextView localTextView = (TextView) findViewById(R.id.textView1);
                    localTextView.append("\t" + msg); // This is one way to display a string.
//                    TextView remoteTextView = (TextView) findViewById(R.id.textView1);
//                    remoteTextView.append("\n");

                    /*
                     * Note that the following AsyncTask uses AsyncTask.SERIAL_EXECUTOR, not
                     * AsyncTask.THREAD_POOL_EXECUTOR as the above ServerTask does. To understand
                     * the difference, please take a look at
                     * http://developer.android.com/reference/android/os/AsyncTask.html
                     */
                    new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg, myPort);
                    editText1.setText("");
                    return true;
                }
                return false;
            }
        });
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_group_messenger, menu);
        return true;
    }

    @Override
    public void onStart() {
        super.onStart();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client.connect();
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "GroupMessenger Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app deep link URI is correct.
                Uri.parse("android-app://edu.buffalo.cse.cse486586.groupmessenger2/http/host/path")
        );
        AppIndex.AppIndexApi.start(client, viewAction);
    }

    @Override
    public void onStop() {
        super.onStop();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "GroupMessenger Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app deep link URI is correct.
                Uri.parse("android-app://edu.buffalo.cse.cse486586.groupmessenger2/http/host/path")
        );
        AppIndex.AppIndexApi.end(client, viewAction);
        client.disconnect();
    }


    /***
     * ServerTask is an AsyncTask that should handle incoming messages. It is created by
     * ServerTask.executeOnExecutor() call in SimpleMessengerActivity.
     * <p/>
     * Please make sure you understand how AsyncTask works by reading
     * http://developer.android.com/reference/android/os/AsyncTask.html
     *
     * @author stevko
     */

    private class ServerTask extends AsyncTask<ServerSocket, String, Void> {

        @Override
        public boolean equals(Object o) {
            return super.equals(o);
        }

        @Override
        protected Void doInBackground(ServerSocket... sockets) {
            ServerSocket serverSocket = sockets[0];

            /*
             * TODO: Fill in your server code that receives messages and passes them
             * to onProgressUpdate().
             */
            try {
                while (true) {
                    Log.v(TAG, "Waiting for next socket connection...");
                    final Socket socket;
                    socket = serverSocket.accept();
                    Log.v(TAG, "Accepted socket" +
                            socket);
                    //create a new thread that keeps listening on loop

                    Thread thread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            while (true) {
                                try {
                                    ObjectInputStream objectInputStream = new ObjectInputStream(socket.getInputStream());

                                    Log.v(TAG, "Waiting for a message on this socket " + socket);
                                    MessageWrapper wrapper = (MessageWrapper) objectInputStream.readObject();

                                    ObjectOutputStream objectOutputStream = new ObjectOutputStream(socket.getOutputStream());

                                    if (wrapper.getType() == MessageWrapper.TYPE_ACK) {
                                        //check if message is ready to be inserted to database, if it is - insert to database
                                        Log.v(TAG, "TYPE_ACK message received from socket " +socket + " Readying for insertion into database.");
                                        mContentValues.put(Constants.KEY, new StringBuffer().append(dbCounter++).toString());
                                        mContentValues.put(Constants.VALUE, wrapper.getData());
                                        mContentResolver.insert(mUri, mContentValues);
                                        sequence = wrapper.getNewPriority()+1;

                                        //remove the entry from the priority queue todo

                                    } else if (wrapper.getType() == MessageWrapper.TYPE_MESSAGE) {

                                        Log.v(TAG, "TYPE_MESSAGE wrapper received -> " + wrapper + " ... Should send a REPLY");

                                        //add this entry to priority queue
                                        queue.add(wrapper);
                                        //Send a reply proposing a sequence number
                                        double priority = sequence + Double.parseDouble(myPort) / 100000;
                                        wrapper.setNewPriority(priority);
                                        wrapper.setType(MessageWrapper.TYPE_REPLY);
                                        sequence++;
                                        objectOutputStream.writeObject(wrapper);
                                        objectOutputStream.flush();
                                        Log.v(TAG, "Sent a reply with  " + wrapper);
                                    } else if(wrapper.getType() == MessageWrapper.TYPE_REPLY) {

                                        Log.v(TAG, "TYPE_REPLY wrapper received -> " + wrapper + " ... Should send an ACK");

                                        //This is a reply. Send an ACK message with the highest priority
                                        //go through the priority queue searching for this message with this port number and original sequence number

                                        List<MessageWrapper> queueMessages = new ArrayList<MessageWrapper>();
                                        double maxPriority = 0;
                                        //check if all responses are collected
                                        for (MessageWrapper queueMessage : queue) {

                                            if (queueMessage.getPort() == wrapper.getPort() && queueMessage.getOriginalPriority() == wrapper.getOriginalPriority()) {
                                                queueMessages.add(queueMessage);
                                                //calculating max priority
                                                if(maxPriority<queueMessage.getNewPriority()) {
                                                    maxPriority=queueMessage.getNewPriority();
                                                }
                                            }
                                        }
                                        if (queueMessages.size() == REMOTE_PORTS.length) {
                                            Log.v(TAG, "All replies have come in. Max priority is " + maxPriority);
                                            //now set new priority, send final ack message to all nodes
                                            wrapper.setNewPriority(maxPriority);
                                            initializeSockets();

                                            for(int i = 0; i<serverSockets.length; i++) {

                                                MessageWrapper messageWrapper = new MessageWrapper();
                                                messageWrapper.setData(wrapper.getData());
                                                messageWrapper.setIsReady(true);
                                                messageWrapper.setNewPriority(maxPriority);
                                                messageWrapper.setType(MessageWrapper.TYPE_ACK);

                                                ObjectOutputStream objectOutputStream1 = new ObjectOutputStream(serverSockets[i].getOutputStream());
                                                objectOutputStream1.writeObject(messageWrapper);
                                                objectOutputStream1.flush();
                                                Log.v(TAG, "Sent ACK " + wrapper + " socket " + socket);
                                            }
                                        } else {
                                            Log.v(TAG, "Only " + queueMessages.size() + " replies have come in. Will process when all " + REMOTE_PORTS.length + " come in.");
                                        }
                                    }
                                    publishProgress(wrapper.toString());
                                } catch (IOException e) {
                                    Log.e(TAG, Log.getStackTraceString(e));
                                } catch (ClassNotFoundException e) {
                                    Log.e(TAG, Log.getStackTraceString(e));
                                }
                            }
                        }
                    });
                    thread.start();
                }
            } catch (Exception e) {
                Log.e(TAG, Log.getStackTraceString(e));
            }

            return null;
        }

        protected void onProgressUpdate(String... strings) {

            /*
             * The following code displays what is received in doInBackground().
             */
            String strReceived = strings[0].trim();
            TextView remoteTextView = (TextView) findViewById(R.id.textView1);
            remoteTextView.append(strReceived + "\t\n");
            TextView localTextView = (TextView) findViewById(R.id.textView1);
            localTextView.append("\n");



            /*
             * The following code creates a file in the AVD's internal storage and stores a file.
             *
             * For more information on file I/O on Android, please take a look at
             * http://developer.android.com/training/basics/data-storage/files.html
             */

            String string = strReceived + "\n";

            try {
            } catch (Exception e) {
                Log.e(TAG, "File write failed");
            }
            return;
        }
    }


    /***
     * ClientTask is an AsyncTask that should send a string over the network.
     * It is created by ClientTask.executeOnExecutor() call whenever OnKeyListener.onKey() detects
     * an enter sequence press event.
     *
     * @author stevko
     */
    private class ClientTask extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... msgs) {

            synchronized (sequence) {
                sequence++;
                //initializing each socket only once
                initializeSockets();
                MessageWrapper[] replies = new MessageWrapper[5];
                for (int j = 0; j < serverSockets.length; j++) {
                    try {

                        //write the message, set an original priority so everyone can propose a priority
                        String msgToSend = msgs[0];
                        MessageWrapper messageWrapper = new MessageWrapper();
                        messageWrapper.setData(msgToSend);
                        messageWrapper.setIsReady(false);
                        messageWrapper.setOriginalPriority(sequence);
                        messageWrapper.setNewPriority(0d);
                        messageWrapper.setType(MessageWrapper.TYPE_MESSAGE);
                        messageWrapper.setPort(myPort);

                        ObjectOutputStream objectOutputStream = new ObjectOutputStream(serverSockets[j].getOutputStream());
                        objectOutputStream.writeObject(messageWrapper);
                        objectOutputStream.flush();
                        Log.v(TAG, "Sending wrapper TYPE_MESSAGE " + messageWrapper + " to socket " + serverSockets[j]);
                    } catch (UnknownHostException e) {
                        Log.e(TAG, "ClientTask UnknownHostException");
                    } catch (IOException e) {
                        Log.e(TAG, "ClientTask socket IOException");
                        Log.e(TAG, Log.getStackTraceString(e));
                    }
                }


            }
            return null;
        }
    }

    private void initializeSockets() {
        try {
            for (int i = 0; i < REMOTE_PORTS.length; i++) {
                if (serverSockets[i] == null) {
                    serverSockets[i] = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            Integer.parseInt(REMOTE_PORTS[i]));
                }
                Log.v(TAG, "Creating socket " + serverSockets[i] + " for the first time...");
            }
        } catch (IOException e) {
            Log.e(TAG, "Some exception when trying to create sockets");
            Log.e(TAG, Log.getStackTraceString(e));
        }
    }

    private Uri buildUri(String scheme, String authority) {
        Uri.Builder uriBuilder = new Uri.Builder();
        uriBuilder.authority(authority);
        uriBuilder.scheme(scheme);
        return uriBuilder.build();
    }
}
