package com.auctionuet.client.network;

import com.auctionuet.client.network.protocol.ActionType;
import com.auctionuet.client.network.protocol.Request;
import com.auctionuet.client.network.protocol.Response;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.HashMap;
import java.util.Map;

public class WalletClient {
    private final Gson gson = new Gson();

    public Map<String, Double> deposit(String token, double amount) throws Exception {
        Map<String, Object> data = new HashMap<>();
        data.put("amount", amount);
        
        Request req = new Request(ActionType.DEPOSIT, data);
        req.setToken(token);
        
        Response res = ServerConnection.getInstance().sendRequest(req);
        if ("OK".equals(res.getStatus())) {
            return gson.fromJson(gson.toJson(res.getData()), new TypeToken<Map<String, Double>>(){}.getType());
        }
        throw new Exception(res.getMessage());
    }

    public Map<String, Double> withdraw(String token, double amount) throws Exception {
        Map<String, Object> data = new HashMap<>();
        data.put("amount", amount);
        
        Request req = new Request(ActionType.WITHDRAW, data);
        req.setToken(token);
        
        Response res = ServerConnection.getInstance().sendRequest(req);
        if ("OK".equals(res.getStatus())) {
            return gson.fromJson(gson.toJson(res.getData()), new TypeToken<Map<String, Double>>(){}.getType());
        }
        throw new Exception(res.getMessage());
    }

    public Map<String, Double> getWallet(String token) throws Exception {
        Request req = new Request(ActionType.GET_WALLET, null);
        req.setToken(token);
        
        Response res = ServerConnection.getInstance().sendRequest(req);
        if ("OK".equals(res.getStatus())) {
            return gson.fromJson(gson.toJson(res.getData()), new TypeToken<Map<String, Double>>(){}.getType());
        }
        throw new Exception(res.getMessage());
    }
}
