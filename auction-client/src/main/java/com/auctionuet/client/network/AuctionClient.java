package com.auctionuet.client.network;

import com.auctionuet.client.model.AuctionDTO;
import com.auctionuet.protocol.ActionType;
import com.auctionuet.protocol.Request;
import com.auctionuet.protocol.Response;
import com.auctionuet.protocol.contract.Dto;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.List;

public class AuctionClient {
    private final Gson gson = new Gson();

    public AuctionDTO createAuction(
            String token,
            String itemId,
            String startTime,
            String endTime,
            String title,
            String description,
            int antiSnipingWindowSeconds,
            int antiSnipingExtensionSeconds
    ) throws Exception {
        Dto<?> data = ActionType.CREATE_AUCTION.createRequestDto()
                .set("itemId", itemId)
                .set("startTime", startTime)
                .set("endTime", endTime)
                .set("title", title)
                .set("description", description)
                .set("antiSnipingWindowSeconds", antiSnipingWindowSeconds)
                .set("antiSnipingExtensionSeconds", antiSnipingExtensionSeconds);

        Request request = new Request(ActionType.CREATE_AUCTION, data);
        request.setToken(token);

        Response response = ServerConnection.getInstance().sendRequest(request);
        if ("OK".equals(response.getStatus())) {
            return gson.fromJson(gson.toJson(response.getData()), AuctionDTO.class);
        }
        throw new Exception(response.getMessage());
    }

    public void startAuction(String token, String auctionId) throws Exception {
        Dto<?> data = ActionType.START_AUCTION.createRequestDto()
                .set("auctionId", auctionId);

        Request request = new Request(ActionType.START_AUCTION, data);
        request.setToken(token);

        Response response = ServerConnection.getInstance().sendRequest(request);
        if (!"OK".equals(response.getStatus())) {
            throw new Exception(response.getMessage());
        }
    }

    public List<AuctionDTO> getAuctions(String token) throws Exception {
        Request request = new Request(ActionType.GET_AUCTIONS, (Dto<?>) null);
        request.setToken(token);

        Response response = ServerConnection.getInstance().sendRequest(request);
        if ("OK".equals(response.getStatus())) {
            Type listType = new TypeToken<List<AuctionDTO>>() {}.getType();
            return gson.fromJson(gson.toJson(response.getData()), listType);
        }
        throw new Exception(response.getMessage());
    }

    public AuctionDTO getAuctionDetail(String token, String auctionId) throws Exception {
        Dto<?> data = ActionType.GET_AUCTION_DETAIL.createRequestDto()
                .set("auctionId", auctionId);

        Request request = new Request(ActionType.GET_AUCTION_DETAIL, data);
        request.setToken(token);

        Response response = ServerConnection.getInstance().sendRequest(request);
        if ("OK".equals(response.getStatus())) {
            return gson.fromJson(gson.toJson(response.getData()), AuctionDTO.class);
        }
        throw new Exception(response.getMessage());
    }
}
