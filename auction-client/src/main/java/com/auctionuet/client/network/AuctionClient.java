package com.auctionuet.client.network;

import com.auctionuet.protocol.ActionType;
import com.auctionuet.protocol.Request;
import com.auctionuet.protocol.Response;
import com.auctionuet.protocol.dto.request.auction.AuctionIdRequestDTO;
import com.auctionuet.protocol.dto.request.auction.CreateAuctionRequestDTO;
import com.auctionuet.protocol.dto.response.auction.AuctionDTO;

import java.time.LocalDateTime;
import java.util.List;

public class AuctionClient {

    public AuctionDTO createAuction(
            String token,
            String itemId,
            LocalDateTime startTime,
            LocalDateTime endTime,
            String title,
            String description,
            int antiSnipingWindowSeconds,
            int antiSnipingExtensionSeconds) throws Exception {
        CreateAuctionRequestDTO data = new CreateAuctionRequestDTO();
        data.setItemId(itemId);
        data.setStartTime(startTime);
        data.setEndTime(endTime);
        data.setTitle(title);
        data.setDescription(description);
        data.setAntiSnipingWindowSeconds(antiSnipingWindowSeconds);
        data.setAntiSnipingExtensionSeconds(antiSnipingExtensionSeconds);

        Response response = ServerConnection.getInstance()
                .sendRequest(Request.fromDto(ActionType.CREATE_AUCTION, data, token));

        if ("OK".equals(response.getStatus())) {
            return response.getDataAs(AuctionDTO.class);
        }
        throw new Exception(response.getMessage());
    }

    public void startAuction(String token, String auctionId) throws Exception {
        AuctionIdRequestDTO data = new AuctionIdRequestDTO();
        data.setAuctionId(auctionId);

        Response response = ServerConnection.getInstance()
                .sendRequest(Request.fromDto(ActionType.START_AUCTION, data, token));

        if (!"OK".equals(response.getStatus())) {
            throw new Exception(response.getMessage());
        }
    }

    public List<AuctionDTO> getAuctions(String token) throws Exception {
        Response response = ServerConnection.getInstance()
                .sendRequest(new Request(ActionType.GET_AUCTIONS, null, token));

        if ("OK".equals(response.getStatus())) {
            return response.getDataListAs(AuctionDTO.class);
        }
        throw new Exception(response.getMessage());
    }

    public AuctionDTO getAuctionDetail(String token, String auctionId) throws Exception {
        AuctionIdRequestDTO data = new AuctionIdRequestDTO();
        data.setAuctionId(auctionId);

        Response response = ServerConnection.getInstance()
                .sendRequest(Request.fromDto(ActionType.GET_AUCTION_DETAIL, data, token));

        if ("OK".equals(response.getStatus())) {
            return response.getDataAs(AuctionDTO.class);
        }
        throw new Exception(response.getMessage());
    }

    public void payAuction(String token, String auctionId) throws Exception {
        AuctionIdRequestDTO data = new AuctionIdRequestDTO();
        data.setAuctionId(auctionId);

        Response response = ServerConnection.getInstance()
                .sendRequest(Request.fromDto(ActionType.PAY_AUCTION, data, token));

        if (!"OK".equals(response.getStatus())) {
            throw new Exception(response.getMessage());
        }
    }

    public List<AuctionDTO> getMyPendingPayments(String token) throws Exception {
        Response response = ServerConnection.getInstance()
                .sendRequest(new Request(ActionType.GET_MY_PENDING_PAYMENTS, null, token));

        if ("OK".equals(response.getStatus())) {
            return response.getDataListAs(AuctionDTO.class);
        }
        throw new Exception(response.getMessage());
    }
}
