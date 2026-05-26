package com.auctionuet.server.network.controller;

import com.auctionuet.protocol.Request;
import com.auctionuet.protocol.Response;
import com.auctionuet.protocol.dto.request.admin.GetGlobalTransactionsRequestDTO;
import com.auctionuet.protocol.dto.request.admin.GetAllUsersRequestDTO;
import com.auctionuet.protocol.dto.request.admin.UpdateUserStatusRequestDTO;
import com.auctionuet.protocol.dto.request.admin.AdminCancelAuctionRequestDTO;
import com.auctionuet.protocol.dto.request.admin.UpdateItemApprovalSettingsRequestDTO;
import com.auctionuet.protocol.dto.request.admin.UpdateItemApprovalStatusRequestDTO;
import com.auctionuet.protocol.dto.response.admin.AdminUserDTO;
import com.auctionuet.protocol.dto.response.transaction.TransactionDTO;
import com.auctionuet.protocol.dto.response.item.ItemDTO;
import com.auctionuet.protocol.enums.Permission;
import com.auctionuet.server.domain.manager.SessionManager;
import com.auctionuet.server.domain.model.User;
import com.auctionuet.server.domain.service.AdminService;
import com.auctionuet.server.domain.service.AuctionService;
import com.auctionuet.server.domain.service.FinancialAuditService;
import com.auctionuet.server.domain.service.ItemService;

import java.util.List;

public class AdminController {
    private final AdminService adminService;
    private final AuctionService auctionService;
    private final ItemService itemService;
    private final FinancialAuditService financialAuditService;
    private final SessionManager sessionManager;

    public AdminController(
            AdminService adminService,
            AuctionService auctionService,
            ItemService itemService,
            FinancialAuditService financialAuditService) {
        this.adminService = adminService;
        this.auctionService = auctionService;
        this.itemService = itemService;
        this.financialAuditService = financialAuditService;
        this.sessionManager = SessionManager.getInstance();
    }

    public Response handleGetAllUsers(Request request) {
        requirePermission(request, Permission.GET_ALL_USERS);
        GetAllUsersRequestDTO filter = request.getData() == null
                ? new GetAllUsersRequestDTO()
                : request.getDataAs(GetAllUsersRequestDTO.class);
        List<AdminUserDTO> users = adminService.getUsers(
                filter.getUsernameQuery(),
                filter.getRole(),
                filter.getStatus());
        return Response.ok(users);
    }

    public Response handleUpdateUserStatus(Request request) {
        User admin = requirePermission(request, Permission.UPDATE_USER_STATUS);
        UpdateUserStatusRequestDTO update = request.getDataAs(UpdateUserStatusRequestDTO.class);
        return Response.ok(adminService.updateUserStatus(admin, update.getUserId(), update.getStatus()));
    }

    public Response handleCancelAuction(Request request) throws Exception {
        User admin = requirePermission(request, Permission.CANCEL_AUCTION_AS_ADMIN);
        AdminCancelAuctionRequestDTO cancel = request.getDataAs(AdminCancelAuctionRequestDTO.class);
        return Response.ok(auctionService.adminCancelAuction(admin, cancel.getAuctionId(), cancel.getReason()));
    }

    public Response handleGetItemApprovalSettings(Request request) {
        requirePermission(request, Permission.VIEW_ITEM_APPROVAL);
        return Response.ok(itemService.getApprovalSettings());
    }

    public Response handleUpdateItemApprovalSettings(Request request) {
        requirePermission(request, Permission.MANAGE_ITEM_APPROVAL);
        UpdateItemApprovalSettingsRequestDTO update =
                request.getDataAs(UpdateItemApprovalSettingsRequestDTO.class);
        return Response.ok(itemService.updateApprovalSettings(update.getEnabled()));
    }

    public Response handleGetItemsForApproval(Request request) {
        requirePermission(request, Permission.VIEW_ITEM_APPROVAL);
        List<ItemDTO> items = itemService.getItemsForApproval();
        return Response.ok(items);
    }

    public Response handleUpdateItemApprovalStatus(Request request) throws Exception {
        requirePermission(request, Permission.MANAGE_ITEM_APPROVAL);
        UpdateItemApprovalStatusRequestDTO update =
                request.getDataAs(UpdateItemApprovalStatusRequestDTO.class);
        return Response.ok(itemService.updateApprovalStatus(update.getItemId(), update.getStatus()));
    }

    public Response handleGetGlobalTransactions(Request request) {
        requirePermission(request, Permission.VIEW_FINANCIAL_AUDIT);
        GetGlobalTransactionsRequestDTO filter = request.getData() == null
                ? new GetGlobalTransactionsRequestDTO()
                : request.getDataAs(GetGlobalTransactionsRequestDTO.class);
        List<TransactionDTO> transactions = financialAuditService.getTransactions(
                filter.getUserId(),
                filter.getType());
        return Response.ok(transactions);
    }

    public Response handleGetFinancialSummary(Request request) {
        requirePermission(request, Permission.VIEW_FINANCIAL_AUDIT);
        return Response.ok(financialAuditService.getSummary());
    }

    private User requirePermission(Request request, Permission permission) {
        User user = sessionManager.validateToken(request.getToken());
        if (!user.hasPermission(permission)) {
            throw new IllegalArgumentException("Bạn không có quyền thực hiện thao tác quản trị");
        }
        return user;
    }
}

