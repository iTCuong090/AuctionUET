package com.auctionuet.server.network.controller;

import com.auctionuet.protocol.enums.Permission;
import com.auctionuet.server.domain.manager.AuctionManager;
import com.auctionuet.server.domain.model.BidRecord;
import com.auctionuet.server.domain.model.LiveAuction;
import com.auctionuet.server.domain.model.User;
import com.auctionuet.server.domain.service.BidService;
import com.auctionuet.server.domain.manager.SessionManager;
import com.auctionuet.server.mapper.BidMapper;
import com.auctionuet.protocol.dto.request.bid.AuctionIdRequestDTO;
import com.auctionuet.protocol.dto.request.bid.PlaceBidRequestDTO;
import com.auctionuet.protocol.dto.request.bid.SetAutoBidRequestDTO;
import com.auctionuet.protocol.dto.response.bid.BidDTO;
import com.auctionuet.protocol.dto.response.bid.AutoBidConfigDTO;
import com.auctionuet.protocol.Request;
import com.auctionuet.protocol.Response;
import com.auctionuet.server.network.server.ClientHandler;
import com.auctionuet.server.persistence.schema.BidSchema;

import java.util.List;
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
        PlaceBidRequestDTO req = request.getDataAs(PlaceBidRequestDTO.class);

        BidRecord record = bidService.placeBid(user, req.getAuctionId(), req.getAmount());

        // Chuyển BidRecord → BidDTO để trả về
        BidDTO dto = BidMapper.toDTO(record, req.getAuctionId());
        return Response.ok(dto);
    }

    // GET_BID_HISTORY
    public Response handleGetBidHistory(Request request) throws Exception {
        User user = sessionManager.validateToken(request.getToken());
        AuctionIdRequestDTO req = request.getDataAs(AuctionIdRequestDTO.class);

        List<BidSchema> bids = bidService.getBidHistory(req.getAuctionId());
        List<BidDTO> dtos = bids.stream()
                .map(b -> BidMapper.schemaToDTO(b))
                .collect(Collectors.toList());
        return Response.ok(dtos);
    }

    // SET_AUTO_BID
    public Response handleSetAutoBid(Request request) throws Exception {
        User user = sessionManager.validateToken(request.getToken());
        SetAutoBidRequestDTO req = request.getDataAs(SetAutoBidRequestDTO.class);

        bidService.setAutoBid(user, req.getAuctionId(), req.getMaxBid(), req.getIncrement());
        return Response.ok("Đã cài đặt Auto-Bid thành công");
    }

    // CANCEL_AUTO_BID
    public Response handleCancelAutoBid(Request request) throws Exception {
        User user = sessionManager.validateToken(request.getToken());
        AuctionIdRequestDTO req = request.getDataAs(AuctionIdRequestDTO.class);

        bidService.cancelAutoBid(user, req.getAuctionId());
        return Response.ok("Đã hủy Auto-Bid");
    }

    // CHECK_AUTO_BID
    public Response handleCheckAutoBid(Request request) throws Exception {
        User user = sessionManager.validateToken(request.getToken());
        AuctionIdRequestDTO req = request.getDataAs(AuctionIdRequestDTO.class);

        LiveAuction liveAuction = AuctionManager.getInstance().getAuction(req.getAuctionId());
        if (liveAuction != null) {
            com.auctionuet.server.domain.model.AutoBidConfig config = liveAuction.getAutoBidConfig(user.getId());
            if (config != null) {
                return Response.ok(new AutoBidConfigDTO(config.getMaxBid(), config.getIncrement()));
            }
        }
        return Response.ok((Object)null);
    }

    // SUBSCRIBE — Đăng ký nhận push notification cho một phiên đấu giá
    public Response handleSubscribe(Request request, ClientHandler clientHandler) throws Exception {
        User user = sessionManager.validateToken(request.getToken());
        AuctionIdRequestDTO req = request.getDataAs(AuctionIdRequestDTO.class);

        // Lấy LiveAuction từ AuctionManager và đăng ký ClientHandler làm Observer
        LiveAuction auction = AuctionManager.getInstance().getAuction(req.getAuctionId());
        if (auction == null) return Response.error("Phiên đấu giá không tồn tại hoặc chưa RUNNING");

        auction.addObserver(clientHandler);
        return Response.ok("Đã subscribe phiên " + req.getAuctionId());
    }

    // UNSUBSCRIBE — Hủy đăng ký push
    public Response handleUnsubscribe(Request request, ClientHandler clientHandler) throws Exception {
        User user = sessionManager.validateToken(request.getToken());
        AuctionIdRequestDTO req = request.getDataAs(AuctionIdRequestDTO.class);

        LiveAuction auction = AuctionManager.getInstance().getAuction(req.getAuctionId());
        if (auction != null) {
            auction.removeObserver(clientHandler);
        }
        return Response.ok("Đã hủy Auto-Bid");
    }

}
