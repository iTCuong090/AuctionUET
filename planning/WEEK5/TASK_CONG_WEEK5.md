# 🅲️ TASK CÔNG — Tuần 5: Client GUI + Network (Bidding & Wallet)

> **Branch:** `feature/w5-cong-bidding-client`
> **Deadline:** Tối 09/05/2026

---

## Tổng quan

Công phụ trách toàn bộ **Client-side**: GUI (JavaFX + FXML), Client Network adapter, và đảm bảo mọi tính năng mới **hiển thị được trên giao diện và kết nối đúng với Server**. Tuần này cần tạo **3 view mới** và **2 network adapter mới**.

---

## 📋 Danh sách task

### BUỔI SÁNG (9:00 → 12:00)

#### Task G1: Đồng bộ Client ActionType ⏱️ 10 phút

**File:** `client/network/protocol/ActionType.java`

```java
public enum ActionType {
    // Cũ
    LOGIN, REGISTER, LOGOUT, PING,
    CREATE_ITEM, GET_MY_ITEMS,
    CREATE_AUCTION, START_AUCTION, GET_AUCTIONS, GET_AUCTION_DETAIL,

    // MỚI tuần 5 — Bidding
    PLACE_BID, GET_BID_HISTORY, SET_AUTO_BID, CANCEL_AUTO_BID,

    // MỚI tuần 5 — Wallet
    DEPOSIT, WITHDRAW, GET_WALLET,

    // MỚI tuần 5 — Payment
    PAY_AUCTION
}
```

#### Task G2: Tạo WalletView.fxml + WalletController ⏱️ 2 tiếng

**Files mới:**
- `client/src/main/resources/fxml/WalletView.fxml`
- `client/view/WalletController.java`

**Giao diện WalletView cần có:**

```
┌──────────────────────────────────────────┐
│            💰 VÍ TIỀN CỦA TÔI           │
├──────────────────────────────────────────┤
│                                          │
│   Số dư khả dụng:     1,500,000 VNĐ     │
│   Tiền đang cọc:        200,000 VNĐ     │
│   ─────────────────────────────────      │
│   Tổng tài sản:       1,700,000 VNĐ     │
│                                          │
├──────────────────────────────────────────┤
│  NẠP TIỀN                               │
│  ┌─────────────────────────────────┐     │
│  │  [Số tiền: ____________]        │     │
│  │                                 │     │
│  │  ┌─────────────────────────┐    │     │
│  │  │  🏦 QR Code Placeholder │    │     │
│  │  │  (Sẽ tích hợp VietQR   │    │     │
│  │  │   trong phiên bản sau)  │    │     │
│  │  └─────────────────────────┘    │     │
│  │                                 │     │
│  │  [✅ Xác nhận đã chuyển khoản]  │     │
│  └─────────────────────────────────┘     │
│                                          │
├──────────────────────────────────────────┤
│  RÚT TIỀN                               │
│  [Số tiền: ____________] [Rút tiền]     │
│                                          │
│  📋 Lịch sử giao dịch (tuỳ chọn)       │
└──────────────────────────────────────────┘
```

**WalletController logic:**

```java
public class WalletController {
    @FXML private Label balanceLabel, frozenLabel, totalLabel;
    @FXML private TextField depositAmountField, withdrawAmountField;
    @FXML private Label statusLabel;
    @FXML private VBox qrPlaceholder; // Placeholder cho QR

    @FXML
    public void initialize() {
        loadWalletInfo();   // Gọi GET_WALLET khi mở trang
    }

    private void loadWalletInfo() {
        // Gọi WalletClient.getWallet(token) trên thread phụ
        // Cập nhật labels trên Platform.runLater()
    }

    @FXML
    private void handleDeposit() {
        // 1. Parse số tiền từ depositAmountField
        // 2. Hiện QR placeholder (tạm thời chỉ là hình ảnh tĩnh)
        // 3. Khi user bấm "Xác nhận đã chuyển khoản":
        //    → Gọi WalletClient.deposit(token, amount)
        //    → Cập nhật balance labels
    }

    @FXML
    private void handleWithdraw() {
        // Gọi WalletClient.withdraw(token, amount)
        // Cập nhật labels
    }
}
```

#### Task G3: Tạo BiddingView — Màn hình đấu giá realtime ⏱️ 2.5 tiếng

**Files mới:**
- `client/src/main/resources/fxml/BiddingView.fxml`
- `client/view/BiddingController.java`

**Đây là view quan trọng nhất tuần này.** Hiển thị khi user bấm vào phiên RUNNING.

**Giao diện BiddingView cần có:**

```
┌──────────────────────────────────────────────┐
│  📦 [Tên sản phẩm]            Status: 🟢    │
│  👤 Seller: username           ⏰ Còn: 05:30 │
├──────────────────────────────────────────────┤
│                                              │
│  💰 GIÁ HIỆN TẠI:  1,500,000 VNĐ            │
│  👑 Người dẫn đầu: bidder123                 │
│  🔒 Tiền cọc của bạn: 100,000 VNĐ           │
│  💳 Số dư ví: 2,000,000 VNĐ                 │
│                                              │
├──────────────────────────────────────────────┤
│  ĐẶT GIÁ THỦ CÔNG                           │
│  [Số tiền: ____________] [🔨 Đặt giá]       │
│                                              │
│  CÀI ĐẶT AUTO-BID                           │
│  Giá tối đa: [____________]                  │
│  Bước giá:   [____________]                  │
│  [🤖 Bật Auto-Bid]  [❌ Hủy Auto-Bid]       │
│                                              │
├──────────────────────────────────────────────┤
│  📜 LỊCH SỬ ĐẶT GIÁ                        │
│  ┌──────────────────────────────────────┐    │
│  │ 14:30:25  bidder123   1,500,000 VNĐ │    │
│  │ 14:28:10  bidder456   1,400,000 VNĐ │    │
│  │ 14:25:00  bidder123   1,200,000 VNĐ │    │
│  │ 14:20:00  auto-bid    1,100,000 VNĐ │    │
│  └──────────────────────────────────────┘    │
└──────────────────────────────────────────────┘
```

**BiddingController logic (Server-Push, KHÔNG dùng Polling):**

> [!IMPORTANT]
> **TASK.md (mục 3.2.4) CẤM dùng polling liên tục.** Phải dùng Observer/Push thông qua Socket.
> Luồng: Server có bid mới → `ClientHandler` (là `AuctionObserver`) tự push JSON về Client → `ServerConnection` listener thread dispatch → `BiddingController` cập nhật UI.

```java
public class BiddingController {
    @FXML private Label titleLabel, priceLabel, leaderLabel, timeLeftLabel;
    @FXML private Label depositLabel, balanceLabel, statusLabel;
    @FXML private TextField bidAmountField, maxBidField, incrementField;
    @FXML private ListView<String> bidHistoryList;

    private String currentAuctionId;

    public void setAuctionId(String auctionId) {
        this.currentAuctionId = auctionId;

        // 1. Load dữ liệu ban đầu 1 lần duy nhất
        loadAuctionDetail();
        loadBidHistory();

        // 2. SUBSCRIBE phiên đấu giá → Server sẽ push khi có bid mới
        subscribeToAuction(auctionId);

        // 3. Đăng ký listener nhận push message từ ServerConnection
        ServerConnection.getInstance().setPushListener(this::onPushMessage);
    }

    /**
     * Callback được gọi khi ServerConnection nhận push message từ Server.
     * Chạy trên background thread → phải dùng Platform.runLater().
     */
    private void onPushMessage(PushMessage push) {
        Platform.runLater(() -> {
            switch (push.getType()) {
                case "BID_UPDATE":
                    // Server push: có bid mới → cập nhật giá, leader, thêm vào history
                    priceLabel.setText(String.format("💰 %,.0f VNĐ", push.getAmount()));
                    leaderLabel.setText("👑 " + push.getBidderUsername());
                    bidHistoryList.getItems().add(0, formatBidEntry(push));
                    break;

                case "AUCTION_EXTENDED":
                    // Server push: anti-sniping gia hạn thời gian
                    timeLeftLabel.setText("⏰ Gia hạn đến: " + push.getNewEndTime());
                    statusLabel.setText("⚡ Phiên được gia hạn!");
                    break;

                case "AUCTION_ENDED":
                    // Server push: phiên kết thúc
                    statusLabel.setText("🔴 Phiên đã kết thúc! Winner: " + push.getWinnerId());
                    // Disable input
                    bidAmountField.setDisable(true);
                    break;
            }
        });
    }

    private void subscribeToAuction(String auctionId) {
        // Gửi SUBSCRIBE action lên Server (trên thread phụ)
        // Server sẽ đăng ký ClientHandler làm AuctionObserver cho phiên này
        new Thread(() -> {
            try {
                BidClient client = new BidClient();
                client.subscribe(ClientSession.getInstance().getToken(), auctionId);
            } catch (Exception e) {
                Platform.runLater(() -> statusLabel.setText("❌ Lỗi subscribe: " + e.getMessage()));
            }
        }).start();
    }

    /**
     * Khi user rời trang BiddingView → UNSUBSCRIBE để Server ngừng push.
     */
    public void cleanup() {
        ServerConnection.getInstance().setPushListener(null);
        new Thread(() -> {
            try {
                BidClient client = new BidClient();
                client.unsubscribe(ClientSession.getInstance().getToken(), currentAuctionId);
            } catch (Exception ignored) {}
        }).start();
    }

    @FXML
    private void handlePlaceBid() {
        // Gọi BidClient.placeBid(token, auctionId, amount) trên thread phụ
        // KHÔNG cần refresh thủ công — Server sẽ push kết quả về qua onPushMessage()
    }

    @FXML
    private void handleSetAutoBid() {
        // Gọi BidClient.setAutoBid(token, auctionId, maxBid, increment)
    }

    @FXML
    private void handleCancelAutoBid() {
        // Gọi BidClient.cancelAutoBid(token, auctionId)
    }

    private void loadAuctionDetail() { /* Gọi 1 lần khi mở trang */ }
    private void loadBidHistory() { /* Gọi 1 lần khi mở trang */ }
}
```

---

### BUỔI CHIỀU (14:00 → 17:00)

#### Task G4: Tạo BidClient + WalletClient ⏱️ 1 tiếng

**File mới:** `client/network/BidClient.java`

```java
public class BidClient {
    private final Gson gson = new Gson();

    public BidDTO placeBid(String token, String auctionId, double amount) throws Exception {
        Map<String, Object> data = new HashMap<>();
        data.put("auctionId", auctionId);
        data.put("amount", amount);
        Request req = new Request(ActionType.PLACE_BID, data);
        req.setToken(token);
        Response res = ServerConnection.getInstance().sendRequest(req);
        if ("OK".equals(res.getStatus())) {
            return gson.fromJson(gson.toJson(res.getData()), BidDTO.class);
        }
        throw new Exception(res.getMessage());
    }

    public List<BidDTO> getBidHistory(String token, String auctionId) throws Exception { ... }
    public void setAutoBid(String token, String auctionId, double maxBid, double increment) throws Exception { ... }
    public void cancelAutoBid(String token, String auctionId) throws Exception { ... }

    // SUBSCRIBE/UNSUBSCRIBE — Đăng ký nhận push từ Server
    public void subscribe(String token, String auctionId) throws Exception {
        Map<String, Object> data = new HashMap<>();
        data.put("auctionId", auctionId);
        Request req = new Request(ActionType.SUBSCRIBE, data);
        req.setToken(token);
        Response res = ServerConnection.getInstance().sendRequest(req);
        if (!"OK".equals(res.getStatus())) throw new Exception(res.getMessage());
    }

    public void unsubscribe(String token, String auctionId) throws Exception {
        Map<String, Object> data = new HashMap<>();
        data.put("auctionId", auctionId);
        Request req = new Request(ActionType.UNSUBSCRIBE, data);
        req.setToken(token);
        Response res = ServerConnection.getInstance().sendRequest(req);
        if (!"OK".equals(res.getStatus())) throw new Exception(res.getMessage());
    }
}
```

**File mới:** `client/network/WalletClient.java`

```java
public class WalletClient {
    public Map<String, Double> deposit(String token, double amount) throws Exception { ... }
    public Map<String, Double> withdraw(String token, double amount) throws Exception { ... }
    public Map<String, Double> getWallet(String token) throws Exception { ... }
}
```

#### Task G5: Tạo/Cập nhật ProfileView ⏱️ 30 phút

**File:** `fxml/ProfileView.fxml` + `view/ProfileController.java`

Hiện thông tin user + link sang WalletView (hoặc embed wallet info trực tiếp).

#### Task G6: Fix navigation — AuctionList → BiddingView ⏱️ 45 phút

**File:** `client/view/AuctionListController.java`

Hiện tại nút "Xem chi tiết" chỉ `System.out.println`. Cần sửa:

```java
btnDetail.setOnAction(e -> {
    // Nếu auction status == RUNNING → mở BiddingView
    // Nếu OPEN → mở AuctionDetailView (cho Seller start)
    // Truyền auctionId sang controller mới
});
```

**Cập nhật DashboardController:** Thêm navigation cho WalletView.

#### Task G7: Cập nhật CreateAuctionView — Anti-Sniping config ⏱️ 30 phút

**File:** `fxml/CreateAuctionView.fxml`

Thêm 2 field cho Seller cấu hình anti-sniping:
- `antiSnipingWindowSeconds` (TextField, mặc định 60)
- `antiSnipingExtensionSeconds` (TextField, mặc định 120)

**Cập nhật CreateAuctionController:** Gửi thêm 2 field khi CREATE_AUCTION.

#### Task G8: Thêm model mới client-side ⏱️ 15 phút

**Cập nhật `client/model/UserDTO.java`** — thêm balance, frozenBalance.
**Cập nhật `client/model/AuctionDTO.java`** — thêm anti-sniping fields.

---

## ⚠️ Lưu ý quan trọng

1. **Thread Network:** LUÔN gọi network trên thread phụ (`new Thread(() -> {...}).start()`), cập nhật UI trên `Platform.runLater()`.
2. **QR Placeholder:** Tạm thời dùng Label/Rectangle với text "🏦 QR Code — Sẽ tích hợp VietQR". Khi bấm "Xác nhận" → gọi DEPOSIT API.
3. **⛔ KHÔNG DÙNG POLLING (TASK.md mục 3.2.4 cấm).** Dùng cơ chế Server-Push thông qua Observer Pattern:
   - Client gửi `SUBSCRIBE` → Server đăng ký `ClientHandler` làm `AuctionObserver`
   - Khi có bid mới → Server push `PushMessage` JSON qua socket
   - `ServerConnection` listener thread nhận và dispatch đến `BiddingController.onPushMessage()`
   - Khi user rời trang → gửi `UNSUBSCRIBE` và cleanup listener
4. **CSS styling:** Giữ consistent với style hiện tại (dark theme, `auction-card` class).
5. **DashboardController sidebar:** Thêm nút "💰 Ví tiền" vào sidebar, gọi `loadView("/fxml/WalletView.fxml")`.

---

## 🔌 Task G9: Cập nhật ServerConnection — Push Listener ⏱️ 1 tiếng ⭐ QUAN TRỌNG

**File:** `client/network/ServerConnection.java`

Hiện tại `ServerConnection.sendRequest()` gửi 1 dòng JSON → đọc 1 dòng response (synchronous). Cần nâng cấp để hỗ trợ **nhận push message bất ngờ** từ Server.

**Kiến trúc mới:**

```java
public class ServerConnection {
    // ... existing fields ...
    private PushListener pushListener; // Callback cho push message
    private Thread listenerThread;     // Thread lắng nghe push

    // Interface callback
    public interface PushListener {
        void onPushMessage(PushMessage message);
    }

    public void setPushListener(PushListener listener) {
        this.pushListener = listener;
    }

    /**
     * Cập nhật connect() → khởi động listener thread.
     * Thread này chạy vòng lặp đọc line từ Server.
     * Phân loại message:
     *   - Nếu có "type": "PUSH" → đây là push message → dispatch đến pushListener
     *   - Nếu có "type": "RESPONSE" → đây là response cho request → đưa vào responseQueue
     */
    public void connect(String host, int port) throws Exception {
        // ... existing connect logic ...

        // Khởi động listener thread
        listenerThread = new Thread(() -> {
            try {
                String line;
                while ((line = in.readLine()) != null) {
                    Map<String, Object> msg = gson.fromJson(line, Map.class);
                    String type = (String) msg.get("type");

                    if ("PUSH".equals(type)) {
                        // Push message từ Server (bid update, auction ended, ...)
                        PushMessage push = gson.fromJson(line, PushMessage.class);
                        if (pushListener != null) {
                            pushListener.onPushMessage(push);
                        }
                    } else {
                        // Response cho request đang chờ
                        responseQueue.put(line); // BlockingQueue<String>
                    }
                }
            } catch (Exception e) {
                // Connection closed
            }
        }, "server-listener");
        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    /**
     * sendRequest() cập nhật: gửi request rồi chờ response từ responseQueue
     * thay vì đọc trực tiếp từ in.readLine() (vì listener thread đang đọc)
     */
    private final BlockingQueue<String> responseQueue = new LinkedBlockingQueue<>();

    public synchronized Response sendRequest(Request req) throws Exception {
        if (!isConnected()) throw new Exception("Chưa kết nối tới server!");

        String jsonRequest = gson.toJson(req);
        out.println(jsonRequest);

        // Chờ response từ queue (listener thread sẽ đặt vào)
        String jsonResponse = responseQueue.poll(10, TimeUnit.SECONDS);
        if (jsonResponse == null) throw new Exception("Server không phản hồi (timeout)!");

        return gson.fromJson(jsonResponse, Response.class);
    }
}
```

**File mới:** `client/network/protocol/PushMessage.java`

```java
public class PushMessage {
    private String type;           // "PUSH"
    private String pushType;       // "BID_UPDATE", "AUCTION_EXTENDED", "AUCTION_ENDED"
    private String auctionId;
    private String bidderUsername;
    private double amount;
    private String winnerId;
    private String newEndTime;
    private double finalPrice;
    // getters...
}
```
