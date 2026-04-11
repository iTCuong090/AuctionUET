package com.auctionuet.client.network.protocol;

public class Response {
    private String type;
    private String status;
    private String event;
    private String message;
    private Object data;

    // Constructor rỗng (Bắt buộc phải có để thư viện Gson nó bóc hộp JSON)
    public Response() {}

    public String getType() { return type; }
    public String getStatus() { return status; }
    public String getEvent() { return event; }
    public String getMessage() { return message; }
    public Object getData() { return data; }
}