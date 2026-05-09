# 🅰️ TASK CƯỜNG — Tuần 5: Persistence + Service (Bidding & Wallet)

> **Branch:** `feature/w5-cuong-bidding-service`
> **Deadline:** Tối 09/05/2026

---

## Tổng quan

Cường phụ trách toàn bộ **tầng Persistence** (Schema, DAO) và **tầng Service** (business logic) cho 3 feature mới: **Wallet**, **Bidding**, và **Payment Flow**. Đây là tầng nền tảng — Khánh (Controller) sẽ gọi trực tiếp các Service của Cường.

---

## 📋 Danh sách task

### BUỔI SÁNG (9:00 → 12:00)

#### Task C1: Bổ sung Wallet vào UserSchema ⏱️ 30 phút

**File:** `persistence/schema/UserSchema.java`

```java
// THÊM 2 field:
private double balance = 0;
private double frozenBalance = 0;

// THÊM getter/setter:
public double getBalance() { return balance; }
public void setBalance(double balance) { this.balance = balance; }
public double getFrozenBalance() { return frozenBalance; }
public void setFrozenBalance(double frozenBalance) { this.frozenBalance = frozenBalance; }
```

**Lưu ý:** Constructor cũ giữ nguyên (default balance = 0). Gson sẽ tự parse từ JSON file nếu field tồn tại. Dữ liệu users.json cũ vẫn tương thích (field thiếu = 0).

#### Task C2: Bổ sung Anti-Sniping vào AuctionSchema ⏱️ 20 phút

**File:** `persistence/schema/AuctionSchema.java`

```java
// THÊM 2 field:
private int antiSnipingWindowSeconds = 60;     // Mặc định 60 giây
private int antiSnipingExtensionSeconds = 120; // Mặc định gia hạn 2 phút
// + getter/setter
```

**Cập nhật constructor** để nhận thêm 2 param khi Seller tạo auction.

#### Task C3: Tạo WalletService ⏱️ 1.5 tiếng

**File mới:** `domain/service/WalletService.java`

```java
public class WalletService {
    private final UserDAO userDAO;

    public WalletService(UserDAO userDAO) { this.userDAO = userDAO; }

    // Nạp tiền (mock auto-accept)
    public Map<String, Double> deposit(String userId, double amount) {
        // 1. Validate amount > 0
        // 2. Lấy UserSchema từ DB
        // 3. userSchema.setBalance(current + amount)
        // 4. userDAO.update(userSchema)
        // 5. Return {"balance": newBalance, "frozenBalance": currentFrozen}
    }

    // Rút tiền
    public Map<String, Double> withdraw(String userId, double amount) {
        // 1. Validate amount > 0
        // 2. Kiểm tra balance >= amount
        // 3. Trừ balance
        // 4. Return wallet info
    }

    // Đóng băng tiền cọc (khi bidder đặt giá)
    public void freezeDeposit(String userId, double amount) {
        // 1. Kiểm tra balance >= amount
        // 2. balance -= amount
        // 3. frozenBalance += amount
        // 4. Lưu DB
    }

    // Hoàn trả tiền cọc (khi bidder thua)
    public void unfreezeDeposit(String userId, double amount) {
        // 1. frozenBalance -= amount
        // 2. balance += amount
        // 3. Lưu DB
    }

    // Tịch thu tiền cọc (khi winner không thanh toán)
    public void forfeitDeposit(String userId, double amount) {
        // 1. frozenBalance -= amount
        // 2. Lưu DB (tiền biến mất)
    }

    // Lấy thông tin ví
    public Map<String, Double> getWallet(String userId) {
        // Return {"balance": ..., "frozenBalance": ...}
    }
}
```

#### Task C4: Tạo BidService ⏱️ 2 tiếng

**File:** `domain/service/BidService.java` (hiện đang rỗng)

```java
public class BidService {
    private final AuctionManager auctionManager;
    private final WalletService walletService;
    private final BidDAO bidDAO;
    private final ItemService itemService;
    private final AuctionDAO auctionDAO;

    // PLACE BID — Luồng chính:
    public BidRecord placeBid(User bidder, String auctionId, double amount) {
        // 1. Lấy LiveAuction từ AuctionManager
        // 2. Lấy ItemSchema để biết startingPrice
        // 3. Tính depositAmount = startingPrice * 0.10
        // 4. Kiểm tra bidder có đủ balance >= depositAmount không
        // 5. Nếu lần đầu bid auction này → freezeDeposit(bidderId, depositAmount)
        //    (Cần track ai đã deposit cho auction nào — dùng Map hoặc check bidHistory)
        // 6. Gọi LiveAuction.placeBid(bidder, amount)
        //    → Nếu thành công: cập nhật AuctionSchema trong DB (highestBid, winnerId)
        //    → Nếu có previous winner khác: unfreezeDeposit cho người cũ
        // 7. Lưu BidSchema vào bids.json
        // 8. Return BidRecord
    }

    // LẤY LỊCH SỬ BID
    public List<BidSchema> getBidHistory(String auctionId) {
        return bidDAO.findByAuctionId(auctionId);
    }

    // CÀI ĐẶT AUTO-BID
    public void setAutoBid(User bidder, String auctionId, double maxBid, double increment) {
        // 1. Lấy LiveAuction
        // 2. Validate: maxBid > currentHighestBid, increment > 0
        // 3. Kiểm tra ví đủ deposit 10%
        // 4. Freeze deposit nếu chưa freeze
        // 5. Tạo AutoBidConfig, gọi LiveAuction.addAutoBid(config)
        // 6. Trigger resolveAutoBids() ngay lập tức
    }

    // HỦY AUTO-BID
    public void cancelAutoBid(User bidder, String auctionId) {
        // 1. Lấy LiveAuction
        // 2. Gọi LiveAuction.removeAutoBid(bidderId)
    }
}
```

> [!IMPORTANT]
> **Điểm khó nhất:** Logic deposit tracking — cần biết ai đã cọc cho auction nào để tránh freeze nhiều lần. Gợi ý: dùng `Set<String>` trong `LiveAuction` hoặc check `bidHistory` xem bidder đã từng bid chưa.

---

### BUỔI CHIỀU (14:00 → 17:00)

#### Task C5: Cập nhật AuctionService — Payment Flow ⏱️ 1.5 tiếng

**File:** `domain/service/AuctionService.java`

Thêm các method mới:

```java
// Cập nhật createAuction() → nhận thêm antiSnipingWindowSeconds, antiSnipingExtensionSeconds
// từ Seller và lưu vào AuctionSchema

// THANH TOÁN
public void payAuction(User winner, String auctionId) {
    // 1. Lấy AuctionSchema, kiểm tra status == WAITING_PAYMENT
    // 2. Kiểm tra winner.getId() == schema.getWinnerId()
    // 3. Tính totalPrice = schema.getHighestBid()
    // 4. Tính depositAmount = startingPrice * 0.10
    // 5. Tính remaining = totalPrice - depositAmount
    // 6. Kiểm tra wallet balance >= remaining
    // 7. walletService.withdraw(winnerId, remaining)
    // 8. walletService.forfeitDeposit(winnerId, depositAmount) — chuyển cọc thành thanh toán
    // 9. Cộng tiền cho Seller: walletService.deposit(sellerId, totalPrice)
    // 10. Cập nhật status → PAID
}
```

Cập nhật `endAuction()`:

```java
public void endAuction(String auctionId) {
    // ... existing logic ...
    // THÊM: Nếu có winner → chuyển status = WAITING_PAYMENT
    //        Schedule 24h timeout → forfeit deposit
    // Nếu không có winner → status = CANCELED, unfreeze tất cả deposit
}
```

#### Task C6: Cập nhật AuctionServer bootstrap ⏱️ 30 phút

**File:** `network/server/AuctionServer.java`

```java
// Thêm khởi tạo:
BidDAO bidDAO = DataManager.getInstance().getBidDAO();
WalletService walletService = new WalletService(userDAO);
BidService bidService = new BidService(auctionManager, walletService, bidDAO, itemService, auctionDAO);
// ... Khánh sẽ tạo BidController và WalletController
// Truyền vào RequestRouter mới
```

#### Task C7: Test thủ công + hỗ trợ merge ⏱️ 30 phút

- Kiểm tra `users.json` có lưu đúng balance/frozenBalance
- Kiểm tra `auctions.json` có lưu đúng anti-sniping fields
- Kiểm tra `bids.json` ghi đúng sau placeBid
- Hỗ trợ Khánh kết nối Controller ↔ Service

---

## ⚠️ Lưu ý quan trọng

1. **Thread-safety:** WalletService cần `synchronized` hoặc lock khi thao tác balance để tránh race condition giữa nhiều bidder cùng freeze.
2. **Backward compatibility:** UserSchema cũ trong `users.json` sẽ không có field balance → Gson mặc định = 0. OK.
3. **24h timeout:** Dùng `ScheduledExecutorService` giống như auto-end auction. Schedule `forfeitDeposit()` sau 24h.
