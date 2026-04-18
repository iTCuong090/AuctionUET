package com.auctionuet.server.network.controller;

import com.auctionuet.server.domain.model.User;
import com.auctionuet.server.domain.service.AuctionService;
import com.auctionuet.server.domain.service.SessionManager;
import com.auctionuet.server.exception.AuctionException;
import com.auctionuet.server.exception.AuthenticationException;
import com.auctionuet.server.mapper.AuctionMapper;
import com.auctionuet.server.mapper.ItemMapper;
import com.auctionuet.server.network.dto.AuctionDTO;
import com.auctionuet.server.network.dto.ItemDTO;
import com.auctionuet.server.network.protocol.Request;
import com.auctionuet.server.network.protocol.Response;
import com.auctionuet.server.persistence.dao.ItemDAO;
import com.auctionuet.server.persistence.schema.AuctionSchema;
import com.auctionuet.server.persistence.schema.ItemSchema;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AuctionController {
    private AuctionService auctionService;
    private SessionManager sessionManager=SessionManager.getInstance();
    private final ItemDAO itemDAO=new ItemDAO();
    private final ItemMapper itemMapper=new ItemMapper();
    private final AuctionMapper auctionMapper=new AuctionMapper();

    public AuctionController(AuctionService auctionService) {
        this.auctionService=auctionService;
    }
    public Response handleCreateItem(Request request) {
        try {
            // Bước 1: Validate Token
            User user = sessionManager.validateToken(request.getToken());
            // Bước 2: Kiểm tra quyền
            if (!user.hasPermission("CREATE_ITEM")) {
                return Response.error("Chỉ Seller mới được đăng sản phẩm");
            }
            Map<String, Object> itemData = request.getData();
            ItemSchema schema = auctionService.createItem(user, itemData);
            ItemDTO dto = ItemMapper.toDTO(schema, schema.getSellerId());
            return Response.ok(dto);
        } catch (AuthenticationException e) {
            return Response.error("Token không hợp lệ");
        } catch (IllegalArgumentException e) {
            return Response.error("Dữ liệu sản phẩm không hợp lệ: " + e.getMessage());
        } catch (AuctionException e) {
            return Response.error(e.getMessage());
        }
    }
    public Response handleCreateAuction(Request request) {
        try {
            // Bước 1: Validate Token
            User user = sessionManager.validateToken(request.getToken());
            // Bước 2: Kiểm tra quyền
            if (!user.hasPermission("CREATE_AUCTION")) {
                return Response.error("Chỉ Seller mới được tạo cuộc đấu giá");
            }
            // Bước 3: Lấy dữ liệu từ request.getData()
            Map<String, Object> auctionData = request.getData();
            String itemId = (String) auctionData.get("itemId");
            String title = (String) auctionData.get("title");
            String description = (String) auctionData.get("description");
            // Bước 4: Parse startTime, endTime -> LocalDateTime
            LocalDateTime startTime = LocalDateTime.parse((String) auctionData.get("startTime"));
            LocalDateTime endTime = LocalDateTime.parse((String) auctionData.get("endTime"));

            // Bước 5: Gọi Service để tạo Auction
            AuctionSchema auctionSchema = auctionService.createAuction(user, itemId, startTime, endTime, title, description
            );

            // Bước 6: Lấy item từ AuctionSchema
            String targetItemId = auctionSchema.getItemId();
            ItemSchema itemSchema = itemDAO.findById(targetItemId);

            if (itemSchema == null) {
                return Response.error("Lỗi: Không tìm thấy vật phẩm của phiên đấu giá này!");
            }
            ItemDTO itemDTO = itemMapper.toDTO(itemSchema, user.getUsername());
            //Bước 7: Tạo auctionDTO
            AuctionDTO auctionDTO = auctionMapper.toDTO(auctionSchema, itemDTO,null,null);
            // Bước 8: Trả về thành công
            return Response.ok(auctionDTO);

        } catch (AuthenticationException e) {
            return Response.error("Token không hợp lệ");
        }
    }
    public Response handleStartAuction(Request request) {
        try {
            //Bước 1: Validate Token
            User user = sessionManager.validateToken(request.getToken());
            // Bước 2: Kiểm tra quyền
            if (!user.hasPermission("START_AUCTION")) {
                return Response.error("Chỉ Seller mới được bắt đầu cuộc đấu giá");
            }
            // Bước 3: Lấy dữ liệu từ request.getData()
            String auctionId = (String) request.getData().get("auctionId");
            auctionService.startAuction(user, auctionId);
            return Response.ok("Phiên đấu giá đã bắt đầu");
        } catch (AuthenticationException e) {
            return Response.error("Token không hợp lệ");
        } catch (AuctionException e) {
            return Response.error(e.getMessage());
        }
    }
    public Response handleGetAuctions(Request request) {
        try {
            User user = sessionManager.validateToken(request.getToken());
            if (!user.hasPermission("GET_AUCTIONS")) {
                return Response.error("Không có quyền lấy danh sách đấu giá");
            }
            List<AuctionSchema> auctions = auctionService.getAuctions();
            List<AuctionDTO>auctionDTOList=new ArrayList<>();
            for (AuctionSchema auction:auctions){
                String targetItemId = auction.getItemId();
                ItemSchema itemSchema = itemDAO.findById(targetItemId);

                if (itemSchema == null) {
                    // Bỏ qua phần tử này, tiếp tục vòng lặp với phần tử kế tiếp
                    System.err.println("Cảnh báo: Bỏ qua đấu giá " + auction.getId() + " do không tìm thấy vật phẩm!");
                    continue;
                }
                String sellerUsername = itemSchema.getSellerId(); // (Hoặc dùng userDAO để tra cứu Username từ SellerId)
                ItemDTO itemDTO = itemMapper.toDTO(itemSchema, sellerUsername);
                AuctionDTO auctionDTO = auctionMapper.toDTO(auction, itemDTO,null,null);
                auctionDTOList.add(auctionDTO);
            }
            return Response.ok(auctionDTOList);
        } catch (AuthenticationException e) {
            return Response.error("Token không hợp lệ");
        }
    }
    public Response handleGetAuctionDetail(Request request) {
        try {
            User user = sessionManager.validateToken(request.getToken());
            if (!user.hasPermission("GET_AUCTION_DETAIL")) {
                return Response.error("Không có quyền xem thông tin đấu giá");
            }
            String auctionId = (String) request.getData().get("auctionId");
            AuctionSchema schema = auctionService.getAuctionById(auctionId);
            if(schema==null) {
                return Response.error("Không tìm thấy phiên đấu giá");
            }
            String targetItemId = schema.getItemId();
            ItemSchema itemSchema = itemDAO.findById(targetItemId);

            if (itemSchema == null) {
                return Response.error("Lỗi: Không tìm thấy vật phẩm của phiên đấu giá này!");
            }
            ItemDTO itemDTO = itemMapper.toDTO(itemSchema, user.getUsername());

            AuctionDTO auctionDTO = auctionMapper.toDTO(schema, itemDTO,null,null);
            return Response.ok(auctionDTO);
        } catch (AuthenticationException e) {
            return Response.error("Token không hợp lệ");
        }
    }
//    public Response handleGetMyItems(Request request) {
//        try {
//            User user = sessionManager.validateToken(request.getToken());
//            if (!user.hasPermission("CREATE_ITEM")) {
//                return Response.error("Chỉ Seller mới có quyền xem vật phẩm của mình");
//            }
//            List<ItemSchema> items = auctionService.(user.getId());
//            return Response.error("Chưa implement");
//        } catch (AuthenticationException e) {
//            return Response.error("Token không hợp lệ");
//        }
//    }


}

