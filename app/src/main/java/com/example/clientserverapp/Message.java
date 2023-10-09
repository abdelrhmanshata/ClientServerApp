package com.example.clientserverapp;

public class Message {

    String Sender , msgText , msgTime;

    public Message() {
    }

    public Message(String sender, String msgText, String msgTime) {
        Sender = sender;
        this.msgText = msgText;
        this.msgTime = msgTime;
    }

    public String getSender() {
        return Sender;
    }

    public void setSender(String sender) {
        Sender = sender;
    }

    public String getMsgText() {
        return msgText;
    }

    public void setMsgText(String msgText) {
        this.msgText = msgText;
    }

    public String getMsgTime() {
        return msgTime;
    }

    public void setMsgTime(String msgTime) {
        this.msgTime = msgTime;
    }

    @Override
    public String toString() {
        return Sender + " : " + msgText + " {" + msgTime+"}";
    }
}
