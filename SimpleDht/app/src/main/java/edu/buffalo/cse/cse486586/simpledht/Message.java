package edu.buffalo.cse.cse486586.simpledht;

import java.util.HashMap;

public class Message {


    private HashMap<String, String> messageMap = new HashMap<String, String>();
    private String originPort;
    private String data;
    private String newSuccessor;
    private String newPredecessor;
    private String isProcessed;
    private Integer hopCount = new Integer(0);

    @Override
    public String toString() {
        return "Message{" +
                "messageMap=" + messageMap +
                ", originPort='" + originPort + '\'' +
                ", data='" + data + '\'' +
                ", newSuccessor='" + newSuccessor + '\'' +
                ", newPredecessor='" + newPredecessor + '\'' +
                ", isProcessed='" + isProcessed + '\'' +
                ", hopCount=" + hopCount +
                ", type=" + type +
                '}';
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public String getNewSuccessor() {
        return newSuccessor;
    }

    public void setNewSuccessor(String newSuccessor) {
        this.newSuccessor = newSuccessor;
    }

    public String getNewPredecessor() {
        return newPredecessor;
    }

    public void setNewPredecessor(String newPredecessor) {
        this.newPredecessor = newPredecessor;
    }

    public enum MessageType {
        godJoin, slaveJoin, chSuccessor, chPredecessor, chSuccAndPred , insert, query, update, delete
    };
    private MessageType type;

    public Message(MessageType type, String originPort) {
        this.type = type;
        this.originPort = originPort;
    }

    public HashMap<String, String> getMessageMap() {
        return messageMap;
    }

    public void setMessageMap(HashMap<String, String> messageMap) {
        this.messageMap = messageMap;
    }

    public String getOriginPort() {
        return originPort;
    }

    public void setOriginPort(String originPort) {
        this.originPort = originPort;
    }

    public String getIsProcessed() {
        return isProcessed;
    }

    public void setIsProcessed(String isProcessed) {
        this.isProcessed = isProcessed;
    }

    public Integer getHopCount() {
        return hopCount;
    }

    public void setHopCount(Integer hopCount) {
        this.hopCount = hopCount;
    }

    public MessageType getType() {
        return type;
    }

    public void setType(MessageType type) {
        this.type = type;
    }
}
