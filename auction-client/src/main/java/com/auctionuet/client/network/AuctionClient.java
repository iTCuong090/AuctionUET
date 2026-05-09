package com.auctionuet.client.network;

import com.auctionuet.client.network.protocol.Request;
import com.auctionuet.client.network.protocol.Response;
import com.auctionuet.client.network.protocol.ActionType;
import com.auctionuet.client.model.AuctionDTO;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AuctionClient {
    private final Gson gson = new Gson();

    /**
     * Gửi yêu cầu tạo một phiên đấu giá mới
     */
    public AuctionDTO createAuction(String token, String itemId, String startTime,
                                    String endTime, String title, String description,
                                    int antiSnipingWindowSeconds, int antiSnipingExtensionSeconds) throws Exception {
        // 1. Đóng gói dữ liệu vào Map
        Map<String, Object> data = new HashMap<>();
        data.put("itemId", itemId);
        data.put("startTime", startTime);
        data.put("endTime", endTime);
        data.put("title", title);
        data.put("description", description);
        data.put("antiSnipingWindowSeconds", antiSnipingWindowSeconds);
        data.put("antiSnipingExtensionSeconds", antiSnipingExtensionSeconds);

        // 2. Tạo Request với ActionType enum (Chuẩn file Request của ông)
        Request request = new Request(ActionType.CREATE_AUCTION, data);
        request.setToken(token);

        // 3. Gửi qua ServerConnection
        Response response = ServerConnection.getInstance().sendRequest(request);

        // 4. Kiểm tra và Parse kết quả về DTO
        if ("OK".equals(response.getStatus())) {
            return gson.fromJson(gson.toJson(response.getData()), AuctionDTO.class);
        } else {
            throw new Exception(response.getMessage());
        }
    }

    /**
     * Lệnh bắt đầu phiên đấu giá (Chuyển trạng thái sang RUNNING)
     */
    public void startAuction(String token, String auctionId) throws Exception {
        Map<String, Object> data = new HashMap<>();
        data.put("auctionId", auctionId);

        Request request = new Request(ActionType.START_AUCTION, data);
        request.setToken(token);

        Response response = ServerConnection.getInstance().sendRequest(request);

        if (!"OK".equals(response.getStatus())) {
            throw new Exception(response.getMessage());
        }
    }

    /**
     * Lấy danh sách tất cả các phiên đấu giá đang có
     */
    public List<AuctionDTO> getAuctions(String token) throws Exception {
        // GET_AUCTIONS thường không cần truyền data (null)
        Request request = new Request(ActionType.GET_AUCTIONS, null);
        request.setToken(token);

        Response response = ServerConnection.getInstance().sendRequest(request);

        if ("OK".equals(response.getStatus())) {
            // Dùng TypeToken để bóc tách danh sách List<AuctionDTO>
            Type listType = new TypeToken<List<AuctionDTO>>(){}.getType();
            return gson.fromJson(gson.toJson(response.getData()), listType);
        } else {
            throw new Exception(response.getMessage());
        }
    }

    /**
     * Xem chi tiết một phiên đấu giá cụ thể
     */
    public AuctionDTO getAuctionDetail(String token, String auctionId) throws Exception {
        Map<String, Object> data = new HashMap<>();
        data.put("auctionId", auctionId);

        Request request = new Request(ActionType.GET_AUCTION_DETAIL, data);
        request.setToken(token);

        Response response = ServerConnection.getInstance().sendRequest(request);

        if ("OK".equals(response.getStatus())) {
            return gson.fromJson(gson.toJson(response.getData()), AuctionDTO.class);
        } else {
            throw new Exception(response.getMessage());
        }
    }
}