package com.foonbot.aqi.dtos;

import com.fasterxml.jackson.annotation.JsonAlias;

public class Message {

    @JsonAlias("body")
    private String message;

    public Message(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
