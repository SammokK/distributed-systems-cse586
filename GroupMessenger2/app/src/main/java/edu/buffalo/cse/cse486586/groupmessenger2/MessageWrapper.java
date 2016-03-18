package edu.buffalo.cse.cse486586.groupmessenger2;

import java.io.Serializable;
import java.util.Comparator;

/**
 * Created by SammokKabasi on 3/15/16.
 */

class MessageWrapper implements Serializable {


    /**
     * if type == MessageWrapper.MESSAGE_TYPE, data holds the message
     * if type == MessageWrapper.REPLY_TYPE, reply type holds the initial sequence number
     * if type == MessageWrapper.ACK_TYPE, reply type holds the final sequence number
     */
    private String data;
    private Double priority;
    private int type;
    private boolean isReady;

    public static final int TYPE_MESSAGE = 1;
    public static final int TYPE_REPLY =  2;
    public static final int TYPE_ACK = 3;

    static Comparator<MessageWrapper> messageWrapperComparator = new Comparator<MessageWrapper>() {

        @Override
        public int compare(MessageWrapper o1, MessageWrapper o2) {
            if (o1.getPriority() > o2.getPriority()) {
                return 1;
            } else if (o1.getPriority() < o2.getPriority()) {
                return -1;
            } else {
                return 0;
            }
        }
    };

    public void setPriority(Double priority) {
        this.priority = priority;
    }

    public MessageWrapper(String data, Double priority, int type) {
        this.data = data;
        this.priority = priority;
        this.type = type;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public Double getPriority() {
        return priority;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public boolean isReady() {
        return isReady;
    }

    public void setIsReady(boolean isReady) {
        this.isReady = isReady;
    }

    @Override
    public String toString() {
        return "MessageWrapper{" +
                "type=" + type +
                ", priority=" + priority +
                ", data='" + data + '\'' +
                ", isReady=" + isReady +
                '}';
    }
}
