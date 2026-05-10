package com.auctionuet.server.network.controller;

import com.auctionuet.server.domain.enums.Permission;
import com.auctionuet.server.domain.manager.AuctionManager;
import com.auctionuet.server.domain.model.BidRecord;
import com.auctionuet.server.domain.model.LiveAuction;
import com.auctionuet.server.domain.model.User;
import com.auctionuet.server.domain.service.BidService;
import com.auctionuet.server.domain.manager.SessionManager;
import com.auctionuet.server.mapper.BidMapper;
import com.auctionuet.server.network.dto.BidDTO;
import com.auctionuet.server.network.protocol.Request;
import com.auctionuet.server.network.protocol.Response;
import com.auctionuet.server.network.server.ClientHandler;
import com.auctionuet.server.persistence.schema.BidSchema;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class BidController {
    private final BidService bidService;
    private final SessionManager sessionManager;
    public BidController(BidService bidService){
        this.bidService=bidService;
        this.sessionManager=SessionManager.getInstance();
    }
    // PLACE_BID
    public Response handlePlaceBid(Request request) throws Exception {
        User user = sessionManager.validateToken(request.getToken());
        // Permission check
        if (!user.hasPermission(Permission.PLACE_BID)) {
            return Response.error("Bạn không có quyền đặt giá");
        }
        Map<String, Object> data = request.getData();
        String auctionId = (String) data.get("auctionId");
        double amount = ((Number) data.get("amount")).doubleValue();

        BidRecord record = bidService.placeBid(user, auctionId, amount);

        // Chuyển BidRecord → BidDTO để trả về
        BidDTO dto = BidMapper.toDTO(record, auctionId);
        return Response.ok(dto);
    }

    // GET_BID_HISTORY
    public Response handleGetBidHistory(Request request) throws Exception {
        User user = sessionManager.validateToken(request.getToken());
        Map<String, Object> data = request.getData();
        String auctionId = (String) data.get("auctionId");

        List<BidSchema> bids = bidService.getBidHistory(auctionId);
        List<BidDTO> dtos = bids.stream()
                .map(b -> BidMapper.schemaToDTO(b))
                .collect(Collectors.toList());
        return Response.ok(dtos);
    }

    // SET_AUTO_BID
    public Response handleSetAutoBid(Request request) throws Exception {
        User user = sessionManager.validateToken(request.getToken());
        Map<String, Object> data = request.getData();
        String auctionId = (String) data.get("auctionId");
        double maxBid = ((Number) data.get("maxBid")).doubleValue();
        double increment = ((Number) data.get("increment")).doubleValue();

        bidService.setAutoBid(user, auctionId, maxBid, increment);
        return Response.ok("Đã cài đặt Auto-Bid thành công");
    }

    // CANCEL_AUTO_BID
    public Response handleCancelAutoBid(Request request) throws Exception {
        User user = sessionManager.validateToken(request.getToken());
        Map<String, Object> data = request.getData();
        String auctionId = (String) data.get("auctionId");

        bidService.cancelAutoBid(user, auctionId);
        return Response.ok("Đã hủy Auto-Bid");
    }

    // CHECK_AUTO_BID
    public Response handleCheckAutoBid(Request request) throws Exception {
        User user = sessionManager.validateToken(request.getToken());
        Map<String, Object> data = request.getData();
        String auctionId = (String) data.get("auctionId");

        LiveAuction liveAuction = AuctionManager.getInstance().getAuction(auctionId);
        if (liveAuction != null) {
            com.auctionuet.server.domain.model.AutoBidConfig config = liveAuction.getAutoBidConfig(user.getId());
            if (config != null) {
                java.util.Map<String, Object> resp = new java.util.HashMap<>();
                resp.put("maxBid", config.getMaxBid());
                resp.put("increment", config.getIncrement());
                return Response.ok(resp);
            }
        }
        return Response.ok((Object)null);
    }

    // SUBSCRIBE — Đăng ký nhận push notification cho một phiên đấu giá
    public Response handleSubscribe(Request request, ClientHandler clientHandler) throws Exception {
        User user = sessionManager.validateToken(request.getToken());
        Map<String, Object> data = request.getData();
        String auctionId = (String) data.get("auctionId");

        // Lấy LiveAuction từ AuctionManager và đăng ký ClientHandler làm Observer
        LiveAuction auction = AuctionManager.getInstance().getAuction(auctionId);
        if (auction == null) return Response.error("Phiên đấu giá không tồn tại hoặc chưa RUNNING");

        auction.addObserver(clientHandler);
        return Response.ok("Đã subscribe phiên " + auctionId);
    }

    // UNSUBSCRIBE — Hủy đăng ký push
    public Response handleUnsubscribe(Request request, ClientHandler clientHandler) throws Exception {
        User user = sessionManager.validateToken(request.getToken());
        Map<String, Object> data = request.getData();
        String auctionId = (String) data.get("auctionId");

        LiveAuction auction = AuctionManager.getInstance().getAuction(auctionId);
        if (auction != null) {
            auction.removeObserver(clientHandler);
        }
        return Response.ok("Đã hủy Auto-Bid");
    }

}
