package edu.buffalo.cse.cse486586.simpledht;

import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import static edu.buffalo.cse.cse486586.simpledht.Helper.*;

/**
 * This is a workaround for android strict mode.
 */

public class AsyncSendMessageHelper implements Runnable {

    private Message message;
    private String portNumber;

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

    @Override
    public void run() {
        sendMessage(message, portNumber);
    }
}