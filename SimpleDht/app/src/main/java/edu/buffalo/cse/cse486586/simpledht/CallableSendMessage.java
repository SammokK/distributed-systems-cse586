package edu.buffalo.cse.cse486586.simpledht;

import android.os.AsyncTask;
import android.util.Log;

import java.net.ServerSocket;


/**
 * This is a workaround for android strict mode.
 */

public class CallableSendMessage extends AsyncTask<ServerSocket, String, Void> {
    private Message message;
    private String portNumber;

//    private static Object lock = new Object();

    @Override
    protected Void doInBackground(ServerSocket... params) {
        Log.i("CallableSendMessage", "Inside SendMessage doInBackground");
        new Helper().sendMessage(message, portNumber);
        return null;
    }

    public Message getMessage() {
        return message;
    }

    public void setMessage(Message message) {
        this.message = message;
    }

    public String getPortNumber() {
        return portNumber;
    }

    public void setPortNumber(String portNumber) {
        this.portNumber = portNumber;
    }
}