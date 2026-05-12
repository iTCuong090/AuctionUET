package com.auctionuet.server.network.controller;

import com.auctionuet.protocol.ActionType;
import com.auctionuet.protocol.Response;
import com.auctionuet.protocol.contract.Dto;
import com.auctionuet.protocol.enums.Permission;
import com.auctionuet.server.domain.manager.AuctionManager;
import com.auctionuet.server.domain.model.BidRecord;
import com.auctionuet.server.domain.model.LiveAuction;
import com.auctionuet.server.domain.model.User;
import com.auctionuet.server.domain.service.BidService;
import com.auctionuet.server.network.server.ClientHandler;
import com.auctionuet.server.persistence.schema.BidSchema;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BidController {
    private final BidService bidService;

    public BidController(BidService bidService) {
        this.bidService = bidService;
    }

    public Response handlePlaceBid(Dto<?> requestData, User user) throws Exception {
        if (!user.hasPermission(Permission.PLACE_BID)) {
            return Response.error("Ban khong co quyen dat gia");
        }

        String auctionId = requestData.getString("auctionId");
        Double amount = requestData.getDouble("amount");
        BidRecord record = bidService.placeBid(user, auctionId, amount);

        Map<String, Object> dto = bidRecordToMap(record, auctionId);
        return Response.ok(Dto.untyped(dto));
    }

    public Response handleGetBidHistory(Dto<?> requestData) throws Exception {
        String auctionId = requestData.getString("auctionId");
        List<BidSchema> bids = bidService.getBidHistory(auctionId);

        List<Map<String, Object>> dtos = new ArrayList<>();
        for (BidSchema bid : bids) {
            dtos.add(bidSchemaToMap(bid));
        }

        return Response.ok(ActionType.GET_BID_HISTORY.createResponseDto().set("bids", dtos));
    }

    public Response handleSetAutoBid(Dto<?> requestData, User user) throws Exception {
        bidService.setAutoBid(
                user,
                requestData.getString("auctionId"),
                requestData.getDouble("maxBid"),
                requestData.getDouble("increment")
        );
        return Response.ok("Da cai dat Auto-Bid thanh cong");
    }

    public Response handleCancelAutoBid(Dto<?> requestData, User user) throws Exception {
        bidService.cancelAutoBid(user, requestData.getString("auctionId"));
        return Response.ok("Da huy Auto-Bid");
    }

    public Response handleCheckAutoBid(Dto<?> requestData, User user) throws Exception {
        String auctionId = requestData.getString("auctionId");
        LiveAuction liveAuction = AuctionManager.getInstance().getAuction(auctionId);
        if (liveAuction != null) {
            com.auctionuet.server.domain.model.AutoBidConfig config = liveAuction.getAutoBidConfig(user.getId());
            if (config != null) {
                return Response.ok(ActionType.CHECK_AUTO_BID.createResponseDto()
                        .set("maxBid", config.getMaxBid())
                        .set("increment", config.getIncrement()));
            }
        }
        return Response.ok(ActionType.CHECK_AUTO_BID.createResponseDto());
    }

    public Response handleSubscribe(Dto<?> requestData, User user, ClientHandler clientHandler) throws Exception {
        String auctionId = requestData.getString("auctionId");
        LiveAuction auction = AuctionManager.getInstance().getAuction(auctionId);
        if (auction == null) return Response.error("Phien dau gia khong ton tai hoac chua RUNNING");

        auction.addObserver(clientHandler);
        return Response.ok("Da subscribe phien " + auctionId);
    }

    public Response handleUnsubscribe(Dto<?> requestData, User user, ClientHandler clientHandler) throws Exception {
        String auctionId = requestData.getString("auctionId");
        LiveAuction auction = AuctionManager.getInstance().getAuction(auctionId);
        if (auction != null) {
            auction.removeObserver(clientHandler);
        }
        return Response.ok("Da huy Subscribe");
    }

    public static Map<String, Object> bidRecordToMap(BidRecord record, String auctionId) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", record.getBidderId() + "-" + record.getTimestamp());
        map.put("auctionId", auctionId);
        map.put("bidderUsername", record.getBidderUsername());
        map.put("amount", record.getAmount());
        map.put("time", record.getTimestamp().toString());
        return map;
    }

    public static Map<String, Object> bidSchemaToMap(BidSchema schema) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", schema.getId());
        map.put("auctionId", schema.getAuctionId());
        map.put("bidderUsername", schema.getBidderId());
        map.put("amount", schema.getAmount());
        map.put("time", schema.getTimestamp().toString());
        return map;
    }
}
