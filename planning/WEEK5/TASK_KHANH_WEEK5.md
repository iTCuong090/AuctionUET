# 🅱️ TASK KHÁNH — Tuần 5: Controller + Router (Bidding & Wallet API)

> **Branch:** `feature/w5-khanh-bidding-controller`
> **Deadline:** Tối 09/05/2026

---

## Tổng quan

Khánh phụ trách toàn bộ **tầng Controller**, **RequestRouter**, và **cơ chế Push Notification** — là cầu nối giữa Client request và Server service. Tuần này cần route **10 action mới** (bao gồm SUBSCRIBE/UNSUBSCRIBE), tạo **3 Controller mới**, và nâng cấp `ClientHandler` thành `AuctionObserver` để push realtime updates.

---

## 📋 Danh sách task

### BUỔI SÁNG (9:00 → 12:00)

#### Task K1: Bổ sung ActionType ⏱️ 15 phút

**File:** `network/protocol/ActionType.java`

```java
// Bidding (tuần 5)
PLACE_BID, GET_BID_HISTORY, SET_AUTO_BID, CANCEL_AUTO_BID,

// Realtime Subscribe (tuần 5) — đã khai báo sẵn từ tuần 4 nhưng chưa route
SUBSCRIBE, UNSUBSCRIBE,

// Wallet (tuần 5)
DEPOSIT, WITHDRAW, GET_WALLET,

// Payment (tuần 5)
PAY_AUCTION
```

#### Task K2: Bổ sung Permission ⏱️ 15 phút

**File:** `domain/enums/Permission.java`

```java
// THÊM:
DEPOSIT, WITHDRAW, GET_WALLET, PAY_AUCTION, SET_AUTO_BID, CANCEL_AUTO_BID
```

**Cập nhật Bidder.java:**
```java
case PLACE_BID, VIEW_AUCTION, VIEW_BID_HISTORY,
     GET_PROFILE, UPDATE_PROFILE, SUBSCRIBE, UNSUBSCRIBE,
     DEPOSIT, WITHDRAW, GET_WALLET, PAY_AUCTION,    // Wallet + Payment
     SET_AUTO_BID, CANCEL_AUTO_BID                   // Auto-bid
         -> true;
```

**Cập nhật Seller.java:**
```java
// Seller cũng cần DEPOSIT, WITHDRAW, GET_WALLET (để nhận tiền từ đấu giá)
case CREATE_ITEM, UPDATE_ITEM, DELETE_ITEM, GET_MY_ITEMS,
     CREATE_AUCTION, START_AUCTION,
     VIEW_AUCTION, VIEW_BID_HISTORY,
     GET_PROFILE, UPDATE_PROFILE,
     DEPOSIT, WITHDRAW, GET_WALLET       // Wallet
         -> true;
```

#### Task K3: Tạo WalletController ⏱️ 1 tiếng

**File mới:** `network/controller/WalletController.java`

```java
public class WalletController {
    private final WalletService walletService;
    private final SessionManager sessionManager;

    // DEPOSIT
    public Response handleDeposit(Request request) throws Exception {
        User user = sessionManager.validateToken(request.getToken());
        Map<String, Object> data = request.getData();
        double amount = ((Number) data.get("amount")).doubleValue();

        Map<String, Double> wallet = walletService.deposit(user.getId(), amount);
        return Response.ok(wallet);
    }

    // WITHDRAW
    public Response handleWithdraw(Request request) throws Exception {
        User user = sessionManager.validateToken(request.getToken());
        Map<String, Object> data = request.getData();
        double amount = ((Number) data.get("amount")).doubleValue();

        Map<String, Double> wallet = walletService.withdraw(user.getId(), amount);
        return Response.ok(wallet);
    }

    // GET_WALLET
    public Response handleGetWallet(Request request) throws Exception {
        User user = sessionManager.validateToken(request.getToken());
        Map<String, Double> wallet = walletService.getWallet(user.getId());
        return Response.ok(wallet);
    }
}
```

#### Task K4: Tạo BidController ⏱️ 2 tiếng

**File:** `network/controller/BidController.java` (hiện đang rỗng)

```java
public class BidController {
    private final BidService bidService;
    private final SessionManager sessionManager;

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
```

---

### BUỔI CHIỀU (14:00 → 17:00)

#### Task K5: Cập nhật AuctionController — PAY_AUCTION ⏱️ 30 phút

**File:** `network/controller/AuctionController.java`

```java
// THÊM method:
public Response handlePayAuction(Request request) throws Exception {
    User user = sessionManager.validateToken(request.getToken());
    Map<String, Object> data = request.getData();
    String auctionId = (String) data.get("auctionId");

    auctionService.payAuction(user, auctionId);
    return Response.ok("Thanh toán thành công! Sản phẩm đã thuộc về bạn.");
}
```

Cập nhật `handleCreateAuction()` để nhận thêm `antiSnipingWindowSeconds` và `antiSnipingExtensionSeconds` từ request data.

#### Task K6: Cập nhật RequestRouter ⏱️ 45 phút

**File:** `network/server/RequestRouter.java`

```java
// Constructor cần thêm:
private final BidController bidController;
private final WalletController walletController;

// Trong internalRoute() switch, THÊM:

// ── Bidding ──
case PLACE_BID:
    return bidController.handlePlaceBid(request);
case GET_BID_HISTORY:
    return bidController.handleGetBidHistory(request);
case SET_AUTO_BID:
    return bidController.handleSetAutoBid(request);
case CANCEL_AUTO_BID:
    return bidController.handleCancelAutoBid(request);

// ── Realtime Subscribe (cần truyền ClientHandler vào!) ──
// ⚠️ SUBSCRIBE/UNSUBSCRIBE cần ClientHandler ref → xem Task K9 bên dưới
case SUBSCRIBE:
    return bidController.handleSubscribe(request, clientHandler);
case UNSUBSCRIBE:
    return bidController.handleUnsubscribe(request, clientHandler);

// ── Wallet ──
case DEPOSIT:
    return walletController.handleDeposit(request);
case WITHDRAW:
    return walletController.handleWithdraw(request);
case GET_WALLET:
    return walletController.handleGetWallet(request);

// ── Payment ──
case PAY_AUCTION:
    return auctionController.handlePayAuction(request);
```

#### Task K7: Cập nhật AuctionServer constructor ⏱️ 30 phút

**File:** `network/server/AuctionServer.java`

Phối hợp với Cường để khởi tạo đúng dependency chain:

```java
// Sau khi Cường tạo WalletService và BidService:
WalletService walletService = new WalletService(userDAO);
BidService bidService = new BidService(...);

WalletController walletController = new WalletController(walletService);
BidController bidController = new BidController(bidService);

// Cập nhật RequestRouter constructor:
this.router = new RequestRouter(
    authController, itemController, auctionController,
    bidController, walletController  // MỚI
);
```

#### Task K8: Exception mới + Logging ⏱️ 30 phút

Tạo exception classes nếu cần:
- `InsufficientBalanceException` — khi rút/cọc mà không đủ tiền
- `PaymentException` — khi thanh toán thất bại

Đảm bảo tất cả controller handler có `AppLogger` logging giống pattern tuần 4.

---

#### Task K9: Nâng cấp ClientHandler thành AuctionObserver (Push Notification) ⏱️ 1.5 tiếng ⭐ QUAN TRỌNG

> [!IMPORTANT]
> **TASK.md (mục 3.2.4) CẤM polling.** ClientHandler phải implement `AuctionObserver` để chủ động push bid updates về Client qua Socket.

**File:** `network/server/ClientHandler.java`

```java
public class ClientHandler implements Runnable, AuctionObserver {
    // ... existing fields ...

    // ========== AuctionObserver PUSH METHODS ==========

    @Override
    public void onBidPlaced(BidRecord record) {
        // Server chủ động push JSON về client khi có bid mới
        Map<String, Object> push = new HashMap<>();
        push.put("type", "PUSH");
        push.put("pushType", "BID_UPDATE");
        push.put("bidderUsername", record.getBidderUsername());
        push.put("amount", record.getAmount());
        push.put("timestamp", record.getTimestamp().toString());

        String json = new Gson().toJson(push);
        sendPush(json);
    }

    @Override
    public void onAuctionEnded(String auctionId, String winnerId, double finalPrice) {
        Map<String, Object> push = new HashMap<>();
        push.put("type", "PUSH");
        push.put("pushType", "AUCTION_ENDED");
        push.put("auctionId", auctionId);
        push.put("winnerId", winnerId);
        push.put("finalPrice", finalPrice);

        String json = new Gson().toJson(push);
        sendPush(json);
    }

    @Override
    public void onAuctionExtended(String auctionId, LocalDateTime newEndTime) {
        Map<String, Object> push = new HashMap<>();
        push.put("type", "PUSH");
        push.put("pushType", "AUCTION_EXTENDED");
        push.put("auctionId", auctionId);
        push.put("newEndTime", newEndTime.toString());

        String json = new Gson().toJson(push);
        sendPush(json);
    }

    /**
     * Push JSON trực tiếp xuống Client qua PrintWriter.
     * Thread-safe nhờ synchronized.
     */
    public synchronized void sendPush(String json) {
        out.println(json);
    }

    // ========== CẬP NHẬT sendMessage() ==========
    // Thêm "type": "RESPONSE" vào mọi Response để Client phân biệt với Push
    public synchronized void sendMessage(Response response) {
        // Thêm trường type = "RESPONSE" để client listener thread phân loại
        Map<String, Object> wrapper = new HashMap<>();
        wrapper.put("type", "RESPONSE");
        wrapper.put("status", response.getStatus());
        wrapper.put("message", response.getMessage());
        wrapper.put("data", response.getData());
        String json = new Gson().toJson(wrapper);
        out.println(json);
    }
}
```

**Cập nhật RequestRouter:** `route()` method cần nhận thêm `ClientHandler` ref để truyền cho SUBSCRIBE/UNSUBSCRIBE:

```java
// RequestRouter:
public Response route(Request request, ClientHandler clientHandler) {
    // ... existing logic ...
    // Truyền clientHandler vào internalRoute()
}

private Response internalRoute(Request request, ClientHandler clientHandler) {
    // SUBSCRIBE/UNSUBSCRIBE cases dùng clientHandler
}
```

**Cập nhật ClientHandler.processLine():** Truyền `this` vào `router.route(request, this)`.

---

## ⚠️ Lưu ý quan trọng

1. **Cast số an toàn:** Gson parse number từ JSON thành `Double` by default. Dùng `((Number) data.get("amount")).doubleValue()` thay vì cast thẳng.
2. **Permission check:** EVERY handler phải check permission TRƯỚC khi xử lý logic.
3. **Phối hợp với Cường:** Khánh gọi Service method → Cường phải expose đúng signature. Nếu Cường chưa code xong, Khánh tạo skeleton.
4. **Push thread-safety:** `sendPush()` và `sendMessage()` phải `synchronized` vì Observer callbacks có thể chạy trên thread khác (bid thread, scheduler thread).
5. **Response type field:** Mọi Response giờ phải có `"type": "RESPONSE"` để Client listener thread phân biệt với Push message `"type": "PUSH"`.
