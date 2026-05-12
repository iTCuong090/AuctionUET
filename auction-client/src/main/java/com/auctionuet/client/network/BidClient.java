package com.auctionuet.client.network;

import com.auctionuet.protocol.ActionType;
import com.auctionuet.protocol.Request;
import com.auctionuet.protocol.Response;
import com.auctionuet.protocol.contract.Dto;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.util.List;
import java.util.Map;

public class BidClient {
    
    public void subscribe(String token, String auctionId) throws Exception {
        Dto<?> data = ActionType.SUBSCRIBE.createRequestDto()
                .set("auctionId", auctionId);
        
        Request req = new Request(ActionType.SUBSCRIBE, data);
        req.setToken(token);
        
        Response res = ServerConnection.getInstance().sendRequest(req);
        if (!"OK".equals(res.getStatus())) {
            throw new Exception(res.getMessage());
        }
    }

    public List<Map<String, Object>> getBidHistory(String token, String auctionId) throws Exception {
        Dto<?> data = ActionType.GET_BID_HISTORY.createRequestDto()
                .set("auctionId", auctionId);
        
        Request req = new Request(ActionType.GET_BID_HISTORY, data);
        req.setToken(token);
        
        Response res = ServerConnection.getInstance().sendRequest(req);
        if ("OK".equals(res.getStatus())) {
            Gson gson = new Gson();
            return gson.fromJson(gson.toJson(res.getData()), new TypeToken<List<Map<String, Object>>>(){}.getType());
        }
        throw new Exception(res.getMessage());
    }

    public void unsubscribe(String token, String auctionId) throws Exception {
        Dto<?> data = ActionType.UNSUBSCRIBE.createRequestDto()
                .set("auctionId", auctionId);
        
        Request req = new Request(ActionType.UNSUBSCRIBE, data);
        req.setToken(token);
        
        // Use a background call or ignore failure to not block UI thread
        try {
            ServerConnection.getInstance().sendRequest(req);
        } catch (Exception e) {
            // Ignore for unsubscribe
        }
    }

    public void placeBid(String token, String auctionId, double amount) throws Exception {
        Dto<?> data = ActionType.PLACE_BID.createRequestDto()
                .set("auctionId", auctionId)
                .set("amount", amount);
        
        Request req = new Request(ActionType.PLACE_BID, data);
        req.setToken(token);
        
        Response res = ServerConnection.getInstance().sendRequest(req);
        if (!"OK".equals(res.getStatus())) {
            throw new Exception(res.getMessage());
        }
    }

    public void setAutoBid(String token, String auctionId, double maxBid, double increment) throws Exception {
        Dto<?> data = ActionType.SET_AUTO_BID.createRequestDto()
                .set("auctionId", auctionId)
                .set("maxBid", maxBid)
                .set("increment", increment);
        
        Request req = new Request(ActionType.SET_AUTO_BID, data);
        req.setToken(token);
        
        Response res = ServerConnection.getInstance().sendRequest(req);
        if (!"OK".equals(res.getStatus())) {
            throw new Exception(res.getMessage());
        }
    }

    public void cancelAutoBid(String token, String auctionId) throws Exception {
        Dto<?> data = ActionType.CANCEL_AUTO_BID.createRequestDto()
                .set("auctionId", auctionId);
        
        Request req = new Request(ActionType.CANCEL_AUTO_BID, data);
        req.setToken(token);
        
        Response res = ServerConnection.getInstance().sendRequest(req);
        if (!"OK".equals(res.getStatus())) {
            throw new Exception(res.getMessage());
        }
    }

    public Map<String, Object> checkAutoBid(String token, String auctionId) throws Exception {
        Dto<?> data = ActionType.CHECK_AUTO_BID.createRequestDto()
                .set("auctionId", auctionId);
        
        Request req = new Request(ActionType.CHECK_AUTO_BID, data);
        req.setToken(token);
        
        Response res = ServerConnection.getInstance().sendRequest(req);
        if ("OK".equals(res.getStatus())) {
            if (res.getData() != null) {
                Gson gson = new Gson();
                return gson.fromJson(gson.toJson(res.getData()), new TypeToken<Map<String, Object>>(){}.getType());
            }
            return null;
        }
        throw new Exception(res.getMessage());
    }
}
