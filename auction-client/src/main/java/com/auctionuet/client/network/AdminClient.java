package com.auctionuet.client.network;

import com.auctionuet.protocol.ActionType;
import com.auctionuet.protocol.Request;
import com.auctionuet.protocol.Response;
import com.auctionuet.protocol.dto.request.admin.GetAllUsersRequestDTO;
import com.auctionuet.protocol.dto.request.admin.GetGlobalTransactionsRequestDTO;
import com.auctionuet.protocol.dto.request.admin.AdminCancelAuctionRequestDTO;
import com.auctionuet.protocol.dto.request.admin.UpdateUserStatusRequestDTO;
import com.auctionuet.protocol.dto.request.admin.UpdateItemApprovalSettingsRequestDTO;
import com.auctionuet.protocol.dto.request.admin.UpdateItemApprovalStatusRequestDTO;
import com.auctionuet.protocol.dto.response.admin.ItemApprovalSettingsDTO;
import com.auctionuet.protocol.dto.response.admin.AdminUserDTO;
import com.auctionuet.protocol.dto.response.admin.FinancialSummaryDTO;
import com.auctionuet.protocol.dto.response.auction.AuctionDTO;
import com.auctionuet.protocol.dto.response.item.ItemDTO;
import com.auctionuet.protocol.dto.response.transaction.TransactionDTO;
import com.auctionuet.protocol.enums.AccountStatus;
import com.auctionuet.protocol.enums.ItemApprovalStatus;
import com.auctionuet.protocol.enums.TransactionType;
import com.auctionuet.protocol.enums.UserRole;

import java.util.List;

public class AdminClient {

    public List<AdminUserDTO> getUsers(
            String token,
            String usernameQuery,
            UserRole role,
            AccountStatus status) throws Exception {
        GetAllUsersRequestDTO filter = new GetAllUsersRequestDTO();
        filter.setUsernameQuery(usernameQuery);
        filter.setRole(role);
        filter.setStatus(status);

        Response response = ServerConnection.getInstance()
                .sendRequest(Request.fromDto(ActionType.GET_ALL_USERS, filter, token));
        if ("OK".equals(response.getStatus())) {
            return response.getDataListAs(AdminUserDTO.class);
        }
        throw new Exception(response.getMessage());
    }

    public AdminUserDTO updateUserStatus(String token, String userId, AccountStatus status) throws Exception {
        UpdateUserStatusRequestDTO update = new UpdateUserStatusRequestDTO();
        update.setUserId(userId);
        update.setStatus(status);

        Response response = ServerConnection.getInstance()
                .sendRequest(Request.fromDto(ActionType.UPDATE_USER_STATUS, update, token));
        if ("OK".equals(response.getStatus())) {
            return response.getDataAs(AdminUserDTO.class);
        }
        throw new Exception(response.getMessage());
    }

    public AuctionDTO cancelAuction(String token, String auctionId, String reason) throws Exception {
        AdminCancelAuctionRequestDTO cancel = new AdminCancelAuctionRequestDTO();
        cancel.setAuctionId(auctionId);
        cancel.setReason(reason);

        Response response = ServerConnection.getInstance()
                .sendRequest(Request.fromDto(ActionType.ADMIN_CANCEL_AUCTION, cancel, token));
        if ("OK".equals(response.getStatus())) {
            return response.getDataAs(AuctionDTO.class);
        }
        throw new Exception(response.getMessage());
    }

    public ItemApprovalSettingsDTO getItemApprovalSettings(String token) throws Exception {
        Response response = ServerConnection.getInstance()
                .sendRequest(new Request(ActionType.GET_ITEM_APPROVAL_SETTINGS, null, token));
        if ("OK".equals(response.getStatus())) {
            return response.getDataAs(ItemApprovalSettingsDTO.class);
        }
        throw new Exception(response.getMessage());
    }

    public ItemApprovalSettingsDTO updateItemApprovalSettings(String token, boolean enabled) throws Exception {
        UpdateItemApprovalSettingsRequestDTO update = new UpdateItemApprovalSettingsRequestDTO();
        update.setEnabled(enabled);
        Response response = ServerConnection.getInstance()
                .sendRequest(Request.fromDto(ActionType.UPDATE_ITEM_APPROVAL_SETTINGS, update, token));
        if ("OK".equals(response.getStatus())) {
            return response.getDataAs(ItemApprovalSettingsDTO.class);
        }
        throw new Exception(response.getMessage());
    }

    public List<ItemDTO> getItemsForApproval(String token) throws Exception {
        Response response = ServerConnection.getInstance()
                .sendRequest(new Request(ActionType.GET_ITEMS_FOR_APPROVAL, null, token));
        if ("OK".equals(response.getStatus())) {
            return response.getDataListAs(ItemDTO.class);
        }
        throw new Exception(response.getMessage());
    }

    public ItemDTO updateItemApprovalStatus(String token, String itemId, ItemApprovalStatus status)
            throws Exception {
        UpdateItemApprovalStatusRequestDTO update = new UpdateItemApprovalStatusRequestDTO();
        update.setItemId(itemId);
        update.setStatus(status);
        Response response = ServerConnection.getInstance()
                .sendRequest(Request.fromDto(ActionType.UPDATE_ITEM_APPROVAL_STATUS, update, token));
        if ("OK".equals(response.getStatus())) {
            return response.getDataAs(ItemDTO.class);
        }
        throw new Exception(response.getMessage());
    }

    public FinancialSummaryDTO getFinancialSummary(String token) throws Exception {
        Response response = ServerConnection.getInstance()
                .sendRequest(new Request(ActionType.GET_FINANCIAL_SUMMARY, null, token));
        if ("OK".equals(response.getStatus())) {
            return response.getDataAs(FinancialSummaryDTO.class);
        }
        throw new Exception(response.getMessage());
    }

    public List<TransactionDTO> getGlobalTransactions(
            String token,
            String userId,
            TransactionType type) throws Exception {
        GetGlobalTransactionsRequestDTO filter = new GetGlobalTransactionsRequestDTO();
        filter.setUserId(userId);
        filter.setType(type);

        Response response = ServerConnection.getInstance()
                .sendRequest(Request.fromDto(ActionType.GET_GLOBAL_TRANSACTIONS, filter, token));
        if ("OK".equals(response.getStatus())) {
            return response.getDataListAs(TransactionDTO.class);
        }
        throw new Exception(response.getMessage());
    }
}
