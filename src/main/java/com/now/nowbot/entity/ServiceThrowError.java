package com.now.nowbot.entity;

import net.mamoe.mirai.event.events.MessageEvent;

public class ServiceThrowError extends RuntimeException{
    MessageEvent event;
    public  ServiceThrowError(MessageEvent event){
        this.event = event;
    }
    public MessageEvent getEvent(){return event;}
}
