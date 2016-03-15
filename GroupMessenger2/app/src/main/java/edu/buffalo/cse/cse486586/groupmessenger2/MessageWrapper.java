package edu.buffalo.cse.cse486586.groupmessenger2;

import java.io.Serializable;
import java.util.Comparator;

/**
 * Created by SammokKabasi on 3/15/16.
 */

class MessageWrapper implements Serializable {
    private String message;
    private Double priority;

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

    public MessageWrapper(String message, Double priority) {
        super();
        this.message = message;
        this.priority = priority;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Double getPriority() {
        return priority;
    }

    public void setPriority(Double priority) {
        this.priority = priority;
    }

    @Override
    public String toString() {
        return "MessageObject [message=" + message + ", priority=" + priority + "]";
    }

}
