package com.auctionuet.protocol.contract;

import java.util.List;

public final class ActionContracts {
    private ActionContracts() {
    }

    public static final class EmptyData implements DtoContract {
        public static final DtoSchema SCHEMA = DtoSchema.strict(List.of());
    }

    public static final class GenericOpenData implements DtoContract {
        public static final DtoSchema SCHEMA = DtoSchema.open(List.of());
    }

    public static final class PingResponseData implements DtoContract {
        public static final DtoSchema SCHEMA = DtoSchema.strict(
                FieldDef.optional("message", String.class)
        );
    }

    public static final class LoginRequestData implements DtoContract {
        public static final DtoSchema SCHEMA = DtoSchema.strict(
                FieldDef.required("username", String.class, v -> ((String) v).length() < 3 ? "Username toi thieu 3 ky tu" : null),
                FieldDef.required("password", String.class, v -> ((String) v).length() < 6 ? "Password toi thieu 6 ky tu" : null)
        );
    }

    public static final class LoginResponseData implements DtoContract {
        public static final DtoSchema SCHEMA = DtoSchema.strict(
                FieldDef.required("token", String.class),
                FieldDef.required("user", Object.class)
        );
    }

    public static final class RegisterRequestData implements DtoContract {
        public static final DtoSchema SCHEMA = DtoSchema.strict(
                FieldDef.required("username", String.class, v -> ((String) v).length() < 3 ? "Username toi thieu 3 ky tu" : null),
                FieldDef.required("password", String.class, v -> ((String) v).length() < 6 ? "Password toi thieu 6 ky tu" : null),
                FieldDef.required("email", String.class),
                FieldDef.optional("role", String.class)
        );
    }

    public static final class AmountRequestData implements DtoContract {
        public static final DtoSchema SCHEMA = DtoSchema.strict(
                FieldDef.required("amount", Double.class, v -> (Double) v <= 0 ? "So tien phai lon hon 0" : null)
        );
    }

    public static final class WalletResponseData implements DtoContract {
        public static final DtoSchema SCHEMA = DtoSchema.strict(
                FieldDef.optional("balance", Double.class),
                FieldDef.optional("frozenBalance", Double.class)
        );
    }

    public static final class AuctionIdRequestData implements DtoContract {
        public static final DtoSchema SCHEMA = DtoSchema.strict(
                FieldDef.required("auctionId", String.class)
        );
    }

    public static final class CreateItemRequestData implements DtoContract {
        public static final DtoSchema SCHEMA = DtoSchema.open(
                FieldDef.required("name", String.class),
                FieldDef.required("description", String.class),
                FieldDef.required("startingPrice", Double.class, v -> (Double) v <= 0 ? "Gia khoi diem phai lon hon 0" : null),
                FieldDef.required("type", String.class),
                FieldDef.optional("imageUrl", String.class),
                FieldDef.optional("condition", String.class)
        );
    }

    public static final class ItemsResponseData implements DtoContract {
        public static final DtoSchema SCHEMA = DtoSchema.strict(
                FieldDef.optional("items", List.class)
        );
    }

    public static final class CreateAuctionRequestData implements DtoContract {
        public static final DtoSchema SCHEMA = DtoSchema.strict(
                FieldDef.required("itemId", String.class),
                FieldDef.required("title", String.class),
                FieldDef.optional("description", String.class),
                FieldDef.required("startTime", String.class),
                FieldDef.required("endTime", String.class),
                FieldDef.optional("antiSnipingWindowSeconds", Integer.class),
                FieldDef.optional("antiSnipingExtensionSeconds", Integer.class)
        );
    }

    public static final class AuctionsResponseData implements DtoContract {
        public static final DtoSchema SCHEMA = DtoSchema.strict(
                FieldDef.optional("auctions", List.class)
        );
    }

    public static final class PlaceBidRequestData implements DtoContract {
        public static final DtoSchema SCHEMA = DtoSchema.strict(
                FieldDef.required("auctionId", String.class),
                FieldDef.required("amount", Double.class, v -> (Double) v <= 0 ? "So tien phai lon hon 0" : null)
        );
    }

    public static final class BidsResponseData implements DtoContract {
        public static final DtoSchema SCHEMA = DtoSchema.strict(
                FieldDef.optional("bids", List.class)
        );
    }

    public static final class SetAutoBidRequestData implements DtoContract {
        public static final DtoSchema SCHEMA = DtoSchema.strict(
                FieldDef.required("auctionId", String.class),
                FieldDef.required("maxBid", Double.class, v -> (Double) v <= 0 ? "maxBid phai lon hon 0" : null),
                FieldDef.required("increment", Double.class, v -> (Double) v <= 0 ? "increment phai lon hon 0" : null)
        );
    }

    public static final class CheckAutoBidResponseData implements DtoContract {
        public static final DtoSchema SCHEMA = DtoSchema.strict(
                FieldDef.optional("maxBid", Double.class),
                FieldDef.optional("increment", Double.class)
        );
    }
}
