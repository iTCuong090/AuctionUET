package com.auctionuet.client.network;

import com.auctionuet.protocol.ActionType;
import com.auctionuet.protocol.Request;
import com.auctionuet.protocol.Response;
import com.auctionuet.protocol.dto.request.bid.AuctionIdRequestDTO;
import com.auctionuet.protocol.dto.request.bid.PlaceBidRequestDTO;
import com.auctionuet.protocol.dto.request.bid.SetAutoBidRequestDTO;
import com.auctionuet.protocol.dto.response.bid.AutoBidConfigDTO;
import com.auctionuet.protocol.dto.response.bid.BidDTO;

import java.util.List;

public class BidClient {

    public void subscribe(String token, String auctionId) throws Exception {
        AuctionIdRequestDTO data = new AuctionIdRequestDTO();
        data.setAuctionId(auctionId);

        Response response = ServerConnection.getInstance()
                .sendRequest(Request.fromDto(ActionType.SUBSCRIBE, data, token));

        if (!"OK".equals(response.getStatus())) {
            throw new Exception(response.getMessage());
        }
    }

    public List<BidDTO> getBidHistory(String token, String auctionId) throws Exception {
        AuctionIdRequestDTO data = new AuctionIdRequestDTO();
        data.setAuctionId(auctionId);

        Response response = ServerConnection.getInstance()
                .sendRequest(Request.fromDto(ActionType.GET_BID_HISTORY, data, token));

        if ("OK".equals(response.getStatus())) {
            return response.getDataListAs(BidDTO.class);
        }
        throw new Exception(response.getMessage());
    }

    public void unsubscribe(String token, String auctionId) {
        AuctionIdRequestDTO data = new AuctionIdRequestDTO();
        data.setAuctionId(auctionId);

        try {
            ServerConnection.getInstance()
                    .sendRequest(Request.fromDto(ActionType.UNSUBSCRIBE, data, token));
        } catch (Exception ignored) {
            // Ignore unsubscribe failures so navigation is not blocked.
        }
    }

    public BidDTO placeBid(String token, String auctionId, double amount) throws Exception {
        PlaceBidRequestDTO data = new PlaceBidRequestDTO();
        data.setAuctionId(auctionId);
        data.setAmount(amount);

        Response response = ServerConnection.getInstance()
                .sendRequest(Request.fromDto(ActionType.PLACE_BID, data, token));

        if ("OK".equals(response.getStatus())) {
            return response.getDataAs(BidDTO.class);
        }
        throw new Exception(response.getMessage());
    }

    public void setAutoBid(String token, String auctionId, double maxBid, double increment) throws Exception {
        SetAutoBidRequestDTO data = new SetAutoBidRequestDTO();
        data.setAuctionId(auctionId);
        data.setMaxBid(maxBid);
        data.setIncrement(increment);

        Response response = ServerConnection.getInstance()
                .sendRequest(Request.fromDto(ActionType.SET_AUTO_BID, data, token));

        if (!"OK".equals(response.getStatus())) {
            throw new Exception(response.getMessage());
        }
    }

    public void cancelAutoBid(String token, String auctionId) throws Exception {
        AuctionIdRequestDTO data = new AuctionIdRequestDTO();
        data.setAuctionId(auctionId);

        Response response = ServerConnection.getInstance()
                .sendRequest(Request.fromDto(ActionType.CANCEL_AUTO_BID, data, token));

        if (!"OK".equals(response.getStatus())) {
            throw new Exception(response.getMessage());
        }
    }

    public AutoBidConfigDTO checkAutoBid(String token, String auctionId) throws Exception {
        AuctionIdRequestDTO data = new AuctionIdRequestDTO();
        data.setAuctionId(auctionId);

        Response response = ServerConnection.getInstance()
                .sendRequest(Request.fromDto(ActionType.CHECK_AUTO_BID, data, token));

        if ("OK".equals(response.getStatus())) {
            return response.getDataAs(AutoBidConfigDTO.class);
        }
        throw new Exception(response.getMessage());
    }
}
