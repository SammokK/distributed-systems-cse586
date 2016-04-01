package edu.buffalo.cse.cse486586.simpledht;

import java.util.concurrent.Callable;

import static edu.buffalo.cse.cse486586.simpledht.Helper.*;

/**
 * This is a workaround for android strict mode.
 */

public class CallableSendMessage implements Callable<String> {
    private Message message;
    private String portNumber;

    @Override
    public String call() throws Exception {

        sendMessage(message, portNumber);
        return "";
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