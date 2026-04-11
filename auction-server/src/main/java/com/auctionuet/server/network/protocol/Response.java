package com.auctionuet.server.network.protocol;

public class Response {
    private String type,status,event,message;
    Object data;
    private Response(String type,String status,Object data,String event,String message){
        this.type=type;
        this.status=status;
        this.data=data;
        this.event=event;
        this.message=message;
    }
    public static Response ok(Object data){
        return new Response("RESPONSE","OK",data,null,null);
    }
    public static Response ok(String message){
        return new Response("RESPONSE","OK",null,null,message);
    }
    public static Response ok(String message, Object data){
        return new Response("RESPONSE","OK",data,null,message);
    }
    public static Response error(String message){
        return new Response("RESPONSE","ERROR",null,null,message);
    }
    public static Response push(String event,Object data){
        return new Response("RESPONSE","OK",data,event,null);
    }

    public Object getData() {
        return data;
    }

    public String getEvent() {
        return event;
    }

    public String getMessage() {
        return message;
    }

    public String getStatus() {
        return status;
    }

    public String getType() {
        return type;
    }
}