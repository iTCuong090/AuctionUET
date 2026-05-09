# 🏛️ KẾ HOẠCH TUẦN 5 — Bidding, Wallet & Auto-Bid

> **Phiên bản:** 1.0 · **Ngày tạo:** 09/05/2026
>
> **Mục tiêu:** Hoàn thiện toàn bộ luồng **Đặt giá (Bidding)**, **Ví tiền (Wallet)**, **Auto-Bid (PriorityQueue)**, **Anti-Sniping**, và **Thanh toán sau thắng**. Tối nay (9/5) merge code và chạy trơn tru.

> [!IMPORTANT]
> Deadline cứng: **Tối 09/05/2026** — cả nhóm họp, merge, demo live.
> Mỗi người có ~6 tiếng code. Feature-first: hoàn thành → chạy được → merge.

---

## 📊 5 Feature cần triển khai

| # | Feature | Mô tả ngắn |
|---|---------|-------------|
| F1 | **Bidding Core** | PLACE_BID, GET_BID_HISTORY, validate + lưu DB + realtime push notify |
| F2 | **Wallet System** | balance + frozenBalance trong UserSchema, DEPOSIT (QR placeholder → confirm), WITHDRAW |
| F3 | **Deposit/Freeze** | Muốn bid → cần 10% startingPrice → freeze. Thua → unfreeze. Thắng → trừ vào thanh toán |
| F4 | **Auto-Bid** | maxBid + increment + PriorityQueue. Nhiều auto-bid cạnh tranh, ưu tiên thời gian đăng ký |
| F5 | **Anti-Sniping** | Bid trong X giây cuối → gia hạn Y giây. Seller cấu hình X, Y khi tạo auction |
| F6 | **Payment Flow** | FINISHED → WAITING_PAYMENT. Winner bấm PAY. 24h timeout → mất cọc tự động |
| F7 | **Realtime Push** | ClientHandler implement AuctionObserver → push bid/end/extend events. **KHÔNG POLLING** (TASK.md 3.2.4) |

---

## 👥 Phân công

| Thành viên | Vai trò | Branch | Task file |
|---|---|---|---|
| 🅰️ **Cường** | Persistence + Service | `feature/w5-cuong-bidding-service` | [TASK_CUONG_WEEK5.md](./TASK_CUONG_WEEK5.md) |
| 🅱️ **Khánh** | Controller + Router | `feature/w5-khanh-bidding-controller` | [TASK_KHANH_WEEK5.md](./TASK_KHANH_WEEK5.md) |
| 🅲️ **Công** | Client GUI + Network | `feature/w5-cong-bidding-client` | [TASK_CONG_WEEK5.md](./TASK_CONG_WEEK5.md) |
| 🅳️ **Anh** | Domain Model + Test | `feature/w5-anh-bidding-domain` | [TASK_ANH_WEEK5.md](./TASK_ANH_WEEK5.md) |

---

## 🤝 Contracts mới — Tuần 5

### Contract W5.1: Wallet fields trong UserSchema (Cường)

```java
// UserSchema bổ sung:
private double balance = 0;        // Số dư khả dụng
private double frozenBalance = 0;  // Số dư đang bị đóng băng (cọc)
// + getter/setter
```

### Contract W5.2: WalletService (Cường)

```java
public class WalletService {
    void deposit(String userId, double amount);        // Cộng tiền
    void withdraw(String userId, double amount);       // Rút tiền (kiểm tra đủ balance)
    void freezeDeposit(String userId, double amount);  // balance -= amount, frozenBalance += amount
    void unfreezeDeposit(String userId, double amount);// frozenBalance -= amount, balance += amount
    void forfeitDeposit(String userId, double amount); // frozenBalance -= amount (mất cọc)
    double getBalance(String userId);
    double getFrozenBalance(String userId);
}
```

### Contract W5.3: BidService (Cường)

```java
public class BidService {
    BidRecord placeBid(User bidder, String auctionId, double amount);
    // → Kiểm tra ví (10% startingPrice), freeze deposit, gọi LiveAuction.placeBid()
    // → Lưu BidSchema vào DB, unfreeze người bị vượt qua

    List<BidSchema> getBidHistory(String auctionId);
    void setAutoBid(User bidder, String auctionId, double maxBid, double increment);
    void cancelAutoBid(User bidder, String auctionId);
}
```

### Contract W5.4: Anti-Sniping fields trong AuctionSchema (Cường)

```java
// AuctionSchema bổ sung:
private int antiSnipingWindowSeconds = 60;     // X giây cuối
private int antiSnipingExtensionSeconds = 120; // Gia hạn Y giây
// Seller cấu hình khi CREATE_AUCTION
```

### Contract W5.5: AutoBidConfig model (Anh)

```java
public class AutoBidConfig implements Comparable<AutoBidConfig> {
    String bidderId;
    String bidderUsername;
    double maxBid;
    double increment;
    LocalDateTime registeredAt; // Dùng để ưu tiên khi maxBid bằng nhau

    @Override
    public int compareTo(AutoBidConfig other) {
        // So sánh: maxBid cao hơn thắng. Nếu bằng → registeredAt sớm hơn thắng
    }
}
```

### Contract W5.6: LiveAuction cập nhật (Anh)

```java
public class LiveAuction {
    // BỎ final cho endTime → private LocalDateTime endTime;
    // THÊM:
    private int antiSnipingWindowSeconds;
    private int antiSnipingExtensionSeconds;
    private final PriorityQueue<AutoBidConfig> autoBidQueue;

    // CẬP NHẬT placeBid():
    //   → Sau khi bid thành công, check anti-sniping → extend endTime
    //   → Sau khi bid thành công, trigger resolveAutoBids()

    void resolveAutoBids();
    // → Lấy top AutoBidConfig từ PriorityQueue, auto-bid increment
    // → Lặp cho đến khi không ai auto-bid được nữa

    void addAutoBid(AutoBidConfig config);
    void removeAutoBid(String bidderId);
    boolean extendIfSniping(); // return true nếu đã gia hạn
}
```

### Contract W5.7: New ActionTypes

```java
// Server ActionType bổ sung:
PLACE_BID, GET_BID_HISTORY,        // Bidding
SET_AUTO_BID, CANCEL_AUTO_BID,     // Auto-bid
SUBSCRIBE, UNSUBSCRIBE,            // Realtime push (Observer)
DEPOSIT, WITHDRAW, GET_WALLET,     // Wallet
PAY_AUCTION                         // Payment

// Client ActionType phải ĐỒNG BỘ thêm tương tự
```

### Contract W5.9: Push Notification Architecture (Khánh + Công)

> [!IMPORTANT]
> **TASK.md mục 3.2.4 CẤM sử dụng polling liên tục.** Phải dùng Observer/Socket push.

```
Luồng Push:
1. Client gửi SUBSCRIBE {auctionId} → Server đăng ký ClientHandler làm AuctionObserver
2. Khi có bid mới → LiveAuction gọi onBidPlaced() trên tất cả observer
3. ClientHandler (là Observer) push JSON {"type": "PUSH", "pushType": "BID_UPDATE", ...} xuống Client
4. Client ServerConnection listener thread nhận → phân loại → dispatch đến BiddingController
5. BiddingController cập nhật UI trên Platform.runLater()
6. Client gửi UNSUBSCRIBE khi rời trang

Phân loại message tại Client:
- {"type": "RESPONSE", ...} → Response cho request đang chờ → đưa vào responseQueue
- {"type": "PUSH", ...}     → Server push → dispatch đến pushListener callback
```

### Contract W5.8: Client ↔ Server Protocol mới

```
PLACE_BID:
  → {"action":"PLACE_BID", "token":"...", "data":{"auctionId":"...", "amount": 500000}}
  ← {"status":"OK", "data": {BidDTO}}

GET_BID_HISTORY:
  → {"action":"GET_BID_HISTORY", "token":"...", "data":{"auctionId":"..."}}
  ← {"status":"OK", "data": [BidDTO, BidDTO, ...]}

SET_AUTO_BID:
  → {"action":"SET_AUTO_BID", "token":"...", "data":{"auctionId":"...", "maxBid": 1000000, "increment": 50000}}
  ← {"status":"OK", "message":"Đã cài Auto-Bid"}

CANCEL_AUTO_BID:
  → {"action":"CANCEL_AUTO_BID", "token":"...", "data":{"auctionId":"..."}}
  ← {"status":"OK"}

DEPOSIT:
  → {"action":"DEPOSIT", "token":"...", "data":{"amount": 500000}}
  ← {"status":"OK", "data":{"balance": 500000, "frozenBalance": 0}}

WITHDRAW:
  → {"action":"WITHDRAW", "token":"...", "data":{"amount": 100000}}
  ← {"status":"OK", "data":{"balance": 400000, "frozenBalance": 0}}

GET_WALLET:
  → {"action":"GET_WALLET", "token":"..."}
  ← {"status":"OK", "data":{"balance": 400000, "frozenBalance": 50000}}

PAY_AUCTION:
  → {"action":"PAY_AUCTION", "token":"...", "data":{"auctionId":"..."}}
  ← {"status":"OK", "message":"Thanh toán thành công"}
```

---

## 📅 Timeline — Ngày 09/05/2026

### Buổi sáng (9:00 → 12:00) — Song song, không phụ thuộc nhau

| Cường | Khánh | Công | Anh |
|---|---|---|---|
| UserSchema += balance, frozenBalance | ActionType bổ sung 10 action mới | Client ActionType đồng bộ | AutoBidConfig model |
| WalletService (deposit, withdraw, freeze, unfreeze) | WalletController (3 handler) | WalletView.fxml + Controller (QR placeholder, nạp/rút) | LiveAuction cập nhật (bỏ final endTime, anti-sniping, PriorityQueue) |
| AuctionSchema += anti-sniping fields | BidController (6 handler + SUBSCRIBE/UNSUBSCRIBE) | ProfileView.fxml (hiện balance) | WalletDTO, AutoBidDTO |
| BidService (placeBid + wallet integration) | Permission updates + ClientHandler implement AuctionObserver | BiddingView.fxml (đặt giá, auto-bid config, bid history, push listener) | AuctionMapper cập nhật anti-sniping |

### Buổi chiều (14:00 → 17:00) — Kết nối & Fix

| Cường | Khánh | Công | Anh |
|---|---|---|---|
| AuctionService cập nhật (payment flow, 24h timeout) | RequestRouter route 10 action mới | ServerConnection nâng cấp (listener thread + responseQueue) | Unit test: placeBid + wallet freeze |
| AuctionServer bootstrap (BidController, WalletController) | Kết nối Controller ↔ Service + Push test | Fix AuctionList "Xem chi tiết" → navigate → subscribe | Unit test: AutoBid PriorityQueue |
| Bug fix + hỗ trợ merge | PAY handler + cleanup observer on disconnect | Client ↔ Server push integration test | Integration test E2E: bid flow |

### Buổi tối (19:00 →) — MERGE & DEMO

1. **Push code** lên branch riêng
2. **Merge** tất cả vào `develop`
3. **Demo live:**
   - Seller tạo item → tạo auction (cấu hình anti-sniping) → start
   - Bidder nạp tiền → đặt giá → cọc bị freeze
   - Bidder 2 đặt auto-bid → hệ thống tự tăng giá
   - Bid sát giờ → auction gia hạn
   - Phiên kết thúc → Winner thanh toán
4. **Tag `v0.3.0-bidding-wallet`**

---

## 📊 Dependency Graph

```
BUỔI SÁNG: SONG SONG — không ai chờ ai
┌──────────────┐  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐
│ Cường        │  │ Khánh        │  │ Công         │  │ Anh          │
│ UserSchema   │  │ ActionType   │  │ Client Enum  │  │ AutoBidConfig│
│ WalletService│  │ Controllers  │  │ WalletView   │  │ LiveAuction  │
│ BidService   │  │ skeleton     │  │ BiddingView  │  │ DTO/Mapper   │
│ AuctionSchema│  │ Permission   │  │ ProfileView  │  │ updates      │
└──────────────┘  └──────────────┘  └──────────────┘  └──────────────┘
       │                 │                 │                 │
       ▼                 ▼                 ▼                 ▼
    MERGE GIỮA NGÀY (13:00) ─────────────────────────
       │                 │                 │                 │
       ▼                 ▼                 ▼                 ▼
BUỔI CHIỀU: Kết nối & Integration
┌──────────────┐  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐
│ Cường        │  │ Khánh        │  │ Công         │  │ Anh          │
│ Payment flow │  │ Router full  │  │ Client Net   │  │ Tests E2E    │
│ Server boot  │  │ Ctrl↔Service │  │ GUI↔Server   │  │ Bug fix      │
│ 24h timeout  │  │ PAY handler  │  │ Navigation   │  │ Demo prep    │
└──────────────┘  └──────────────┘  └──────────────┘  └──────────────┘
```

---

## ✅ Checklist MVP — Bidding + Wallet hoàn thành khi nào?

| # | Tiêu chí | Verify |
|---|----------|--------|
| 1 | Bidder nạp tiền (DEPOSIT) → balance tăng | Cường |
| 2 | Bidder rút tiền (WITHDRAW) → balance giảm, kiểm tra đủ tiền | Cường |
| 3 | Bidder đặt giá → kiểm tra 10% deposit → freeze → bid thành công | Khánh |
| 4 | Bidder thua → tiền cọc unfreeze về balance | Anh |
| 5 | Bidder thắng → PAY_AUCTION → trừ tiền (finalPrice - deposit) | Khánh |
| 6 | 24h không thanh toán → mất cọc tự động | Cường |
| 7 | Auto-Bid: đặt maxBid + increment → hệ thống tự bid khi bị vượt | Anh |
| 8 | Nhiều Auto-Bid cạnh tranh → PriorityQueue xử lý đúng thứ tự | Anh |
| 9 | Anti-Sniping: bid trong X giây cuối → gia hạn Y giây | Anh |
| 10 | Seller cấu hình X, Y khi tạo auction | Cường |
| 11 | Client hiện WalletView: nạp/rút tiền, QR placeholder | Công |
| 12 | Client hiện BiddingView: đặt giá, xem bid history, cài auto-bid | Công |
| 13 | Client AuctionList "Xem chi tiết" navigate đúng | Công |
| 14 | **Realtime push: bid mới → tất cả client đang xem được cập nhật ngay (KHÔNG polling)** | Khánh |
| 15 | `mvn test` ALL PASS | Anh |
