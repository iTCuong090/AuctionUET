package com.auctionuet.client.network;

import com.auctionuet.protocol.ActionType;
import com.auctionuet.protocol.Request;
import com.auctionuet.protocol.Response;
import com.auctionuet.protocol.dto.request.item.CreateItemRequestDTO;
import com.auctionuet.protocol.dto.request.item.ItemIdRequestDTO;
import com.auctionuet.protocol.dto.response.auction.AuctionDTO;
import com.auctionuet.protocol.dto.response.item.ItemDTO;

import java.util.List;

public class ItemClient {

    public ItemDTO createItem(String token, CreateItemRequestDTO itemData) throws Exception {
        Response response = ServerConnection.getInstance()
                .sendRequest(Request.fromDto(ActionType.CREATE_ITEM, itemData, token));

        if ("OK".equals(response.getStatus())) {
            return response.getDataAs(ItemDTO.class);
        }
        throw new Exception(response.getMessage());
    }

    public List<ItemDTO> getMyItems(String token) throws Exception {
        Response response = ServerConnection.getInstance()
                .sendRequest(new Request(ActionType.GET_MY_ITEMS, null, token));

        if ("OK".equals(response.getStatus())) {
            return response.getDataListAs(ItemDTO.class);
        }
        throw new Exception(response.getMessage());
    }

    public void deleteItem(String token, String itemId) throws Exception {
        ItemIdRequestDTO data = new ItemIdRequestDTO();
        data.setItemId(itemId);

        Response response = ServerConnection.getInstance()
                .sendRequest(Request.fromDto(ActionType.DELETE_ITEM, data, token));

        if (!"OK".equals(response.getStatus())) {
            throw new Exception(response.getMessage());
        }
    }

    public List<AuctionDTO> getItemAuctionHistory(String token, String itemId) throws Exception {
        ItemIdRequestDTO data = new ItemIdRequestDTO();
        data.setItemId(itemId);

        Response response = ServerConnection.getInstance()
                .sendRequest(Request.fromDto(ActionType.GET_ITEM_AUCTION_HISTORY, data, token));

        if ("OK".equals(response.getStatus())) {
            return response.getDataListAs(AuctionDTO.class);
        }
        throw new Exception(response.getMessage());
    }
}
