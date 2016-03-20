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
    private Double newPriority;
    private int type;
    private boolean isReady;
    private Double originalPriority;
    private String port;
    private int noOfReplies;
    private long finalPriority;



    public static final int TYPE_MESSAGE = 1;
    public static final int TYPE_REPLY =  2;
    public static final int TYPE_ACK = 3;

    static Comparator<MessageWrapper> messageWrapperComparator = new Comparator<MessageWrapper>() {

        @Override
        public int compare(MessageWrapper o1, MessageWrapper o2) {
            if (o1.getNewPriority() > o2.getNewPriority()) {
                return 1;
            } else if (o1.getNewPriority() < o2.getNewPriority()) {
                return -1;
            } else {
                return 0;
            }
        }
    };


    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
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

    public Double getNewPriority() {
        return newPriority;
    }

    public void setNewPriority(Double newPriority) {
        this.newPriority = newPriority;
    }

    public Double getOriginalPriority() {
        return originalPriority;
    }

    public void setOriginalPriority(Double originalPriority) {
        this.originalPriority = originalPriority;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    @Override
    public String toString() {
        return "MessageWrapper{" +
                "data='" + data + '\'' +
                ", newPriority=" + newPriority +
                ", type=" + type +
                ", isReady=" + isReady +
                ", originalPriority=" + originalPriority +
                ", port='" + port + '\'' +
                '}';
    }
}
