package com.auctionuet.protocol;

import com.auctionuet.protocol.contract.ActionContracts;
import com.auctionuet.protocol.contract.Dto;
import com.auctionuet.protocol.contract.DtoContract;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * ActionType is the registry for all network actions.
 * Each action defines request/response contracts consumed by Dto.
 */
public final class ActionType {
    private final String name;
    private final boolean requiresAuth;
    private final boolean needsClientHandler;
    private final transient Class<? extends DtoContract> requestContract;
    private final transient Class<? extends DtoContract> responseContract;

    private static final Map<String, ActionType> REGISTRY = new LinkedHashMap<>();

    private ActionType(
            String name,
            boolean requiresAuth,
            boolean needsClientHandler,
            Class<? extends DtoContract> requestContract,
            Class<? extends DtoContract> responseContract
    ) {
        this.name = Objects.requireNonNull(name, "name");
        this.requiresAuth = requiresAuth;
        this.needsClientHandler = needsClientHandler;
        this.requestContract = requestContract;
        this.responseContract = responseContract;
        REGISTRY.put(name, this);
    }

    public String name() {
        return name;
    }

    public boolean isRequiresAuth() {
        return requiresAuth;
    }

    public boolean isNeedsClientHandler() {
        return needsClientHandler;
    }

    public Class<? extends DtoContract> getRequestContract() {
        return requestContract;
    }

    public Class<? extends DtoContract> getResponseContract() {
        return responseContract;
    }

    public Dto<?> createRequestDto() {
        return requestContract != null ? Dto.of(requestContract) : Dto.untyped();
    }

    public Dto<?> createResponseDto() {
        return responseContract != null ? Dto.of(responseContract) : Dto.untyped();
    }

    public Dto<?> parseRequest(Map<String, Object> raw) {
        return requestContract != null ? Dto.of(requestContract, raw) : Dto.untyped(raw);
    }

    public Dto<?> parseResponse(Map<String, Object> raw) {
        return responseContract != null ? Dto.of(responseContract, raw) : Dto.untyped(raw);
    }

    public void validateRequest(Dto<?> dto) {
        if (dto != null) {
            dto.validate();
        }
    }

    public void validateResponse(Dto<?> dto) {
        if (dto != null) {
            dto.validate();
        }
    }

    // Compatibility methods kept during the refactor.
    public Dto<?> createRequestData() {
        return createRequestDto();
    }

    public Dto<?> createResponseData() {
        return createResponseDto();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ActionType that)) return false;
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        return name;
    }

    public static ActionType valueOf(String name) {
        return REGISTRY.get(name);
    }

    public static Collection<ActionType> values() {
        return REGISTRY.values();
    }

    public static final ActionType PING = new ActionType(
            "PING", false, false, ActionContracts.EmptyData.class, ActionContracts.PingResponseData.class);
    public static final ActionType LOGIN = new ActionType(
            "LOGIN", false, false, ActionContracts.LoginRequestData.class, ActionContracts.LoginResponseData.class);
    public static final ActionType REGISTER = new ActionType(
            "REGISTER", false, false, ActionContracts.RegisterRequestData.class, ActionContracts.EmptyData.class);
    public static final ActionType LOGOUT = new ActionType(
            "LOGOUT", true, false, ActionContracts.EmptyData.class, ActionContracts.EmptyData.class);
    public static final ActionType GET_PROFILE = new ActionType(
            "GET_PROFILE", true, false, ActionContracts.EmptyData.class, ActionContracts.GenericOpenData.class);

    public static final ActionType CREATE_ITEM = new ActionType(
            "CREATE_ITEM", true, false, ActionContracts.CreateItemRequestData.class, ActionContracts.GenericOpenData.class);
    public static final ActionType GET_MY_ITEMS = new ActionType(
            "GET_MY_ITEMS", true, false, ActionContracts.EmptyData.class, ActionContracts.ItemsResponseData.class);

    public static final ActionType CREATE_AUCTION = new ActionType(
            "CREATE_AUCTION", true, false, ActionContracts.CreateAuctionRequestData.class, ActionContracts.GenericOpenData.class);
    public static final ActionType START_AUCTION = new ActionType(
            "START_AUCTION", true, false, ActionContracts.AuctionIdRequestData.class, ActionContracts.EmptyData.class);
    public static final ActionType GET_AUCTIONS = new ActionType(
            "GET_AUCTIONS", true, false, ActionContracts.EmptyData.class, ActionContracts.AuctionsResponseData.class);
    public static final ActionType GET_AUCTION_DETAIL = new ActionType(
            "GET_AUCTION_DETAIL", true, false, ActionContracts.AuctionIdRequestData.class, ActionContracts.GenericOpenData.class);

    public static final ActionType PLACE_BID = new ActionType(
            "PLACE_BID", true, false, ActionContracts.PlaceBidRequestData.class, ActionContracts.GenericOpenData.class);
    public static final ActionType GET_BID_HISTORY = new ActionType(
            "GET_BID_HISTORY", true, false, ActionContracts.AuctionIdRequestData.class, ActionContracts.BidsResponseData.class);
    public static final ActionType SET_AUTO_BID = new ActionType(
            "SET_AUTO_BID", true, false, ActionContracts.SetAutoBidRequestData.class, ActionContracts.EmptyData.class);
    public static final ActionType CANCEL_AUTO_BID = new ActionType(
            "CANCEL_AUTO_BID", true, false, ActionContracts.AuctionIdRequestData.class, ActionContracts.EmptyData.class);
    public static final ActionType CHECK_AUTO_BID = new ActionType(
            "CHECK_AUTO_BID", true, false, ActionContracts.AuctionIdRequestData.class, ActionContracts.CheckAutoBidResponseData.class);
    public static final ActionType SUBSCRIBE = new ActionType(
            "SUBSCRIBE", true, true, ActionContracts.AuctionIdRequestData.class, ActionContracts.EmptyData.class);
    public static final ActionType UNSUBSCRIBE = new ActionType(
            "UNSUBSCRIBE", true, true, ActionContracts.AuctionIdRequestData.class, ActionContracts.EmptyData.class);

    public static final ActionType DEPOSIT = new ActionType(
            "DEPOSIT", true, false, ActionContracts.AmountRequestData.class, ActionContracts.WalletResponseData.class);
    public static final ActionType WITHDRAW = new ActionType(
            "WITHDRAW", true, false, ActionContracts.AmountRequestData.class, ActionContracts.WalletResponseData.class);
    public static final ActionType GET_WALLET = new ActionType(
            "GET_WALLET", true, false, ActionContracts.EmptyData.class, ActionContracts.WalletResponseData.class);

    public static final ActionType PING_TYPE = PING;
    public static final ActionType LOGIN_TYPE = LOGIN;
    public static final ActionType REGISTER_TYPE = REGISTER;
    public static final ActionType LOGOUT_TYPE = LOGOUT;
    public static final ActionType CREATE_ITEM_TYPE = CREATE_ITEM;
    public static final ActionType PLACE_BID_TYPE = PLACE_BID;
}
