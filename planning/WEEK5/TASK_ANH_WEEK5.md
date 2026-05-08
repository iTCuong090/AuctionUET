# 🅳️ TASK ANH — Tuần 5: Domain Model + Mapper + DTO + Test

> **Branch:** `feature/w5-anh-bidding-domain`
> **Deadline:** Tối 09/05/2026

---

## Tổng quan

Anh phụ trách **tầng Domain Model** (logic nghiệp vụ cốt lõi), **Mapper/DTO**, và **toàn bộ Unit/Integration Test**. Tuần này là tuần nặng nhất cho Anh vì phải triển khai **Auto-Bid PriorityQueue** và **Anti-Sniping** — hai thuật toán cốt lõi nằm hoàn toàn trong LiveAuction.

---

## 📋 Danh sách task

### BUỔI SÁNG (9:00 → 12:00)

#### Task A1: Tạo AutoBidConfig model ⏱️ 30 phút

**File mới:** `domain/model/AutoBidConfig.java`

```java
public class AutoBidConfig implements Comparable<AutoBidConfig> {
    private final String bidderId;
    private final String bidderUsername;
    private double maxBid;
    private final double increment;
    private final LocalDateTime registeredAt;

    public AutoBidConfig(String bidderId, String bidderUsername,
                         double maxBid, double increment) {
        this.bidderId = bidderId;
        this.bidderUsername = bidderUsername;
        this.maxBid = maxBid;
        this.increment = increment;
        this.registeredAt = LocalDateTime.now();
    }

    @Override
    public int compareTo(AutoBidConfig other) {
        // maxBid cao hơn → ưu tiên trước (nằm đầu queue)
        int cmp = Double.compare(other.maxBid, this.maxBid);
        if (cmp != 0) return cmp;
        // Nếu bằng → đăng ký sớm hơn ưu tiên
        return this.registeredAt.compareTo(other.registeredAt);
    }

    // getters...
}
```

#### Task A2: Cập nhật LiveAuction — Anti-Sniping + Auto-Bid ⏱️ 3 tiếng ⭐ TASK QUAN TRỌNG NHẤT

**File:** `domain/model/LiveAuction.java`

**Thay đổi lớn:**

```java
public class LiveAuction {
    // === CẬP NHẬT ===
    private LocalDateTime endTime;  // BỎ final để anti-sniping có thể extend

    // === THÊM MỚI ===
    private int antiSnipingWindowSeconds;      // X giây
    private int antiSnipingExtensionSeconds;   // Y giây
    private final PriorityQueue<AutoBidConfig> autoBidQueue = new PriorityQueue<>();
    private final Set<String> depositedBidders = new ConcurrentHashMap<>().newKeySet();
    // Track ai đã cọc rồi để BidService không freeze 2 lần

    // === CẬP NHẬT CONSTRUCTOR ===
    public LiveAuction(String id, String itemId, String sellerId,
                       LocalDateTime endTime, AuctionStatus status,
                       double currentHighestBid, String currentWinnerId,
                       int antiSnipingWindowSeconds, int antiSnipingExtensionSeconds) {
        // ... existing fields ...
        this.antiSnipingWindowSeconds = antiSnipingWindowSeconds;
        this.antiSnipingExtensionSeconds = antiSnipingExtensionSeconds;
    }

    // === CẬP NHẬT placeBid() ===
    public BidRecord placeBid(User bidder, double amount)
            throws InvalidBidException, AuctionClosedException {
        bidLock.lock();
        try {
            // ... existing validation ...

            // 2. Update state (giữ nguyên)
            BidRecord record = new BidRecord(...);
            this.currentHighestBid = amount;
            this.currentWinnerId = bidder.getId();
            this.bidHistory.add(record);

            // 3. Anti-Sniping check — THÊM MỚI
            boolean extended = extendIfSniping();

            // 4. Notify observers (giữ nguyên)
            notifyObservers(record);

            // 5. Trigger Auto-Bid resolution — THÊM MỚI
            resolveAutoBids();

            return record;
        } finally {
            bidLock.unlock();
        }
    }

    // === ANTI-SNIPING ===
    public boolean extendIfSniping() {
        LocalDateTime now = LocalDateTime.now();
        long secondsLeft = java.time.Duration.between(now, endTime).getSeconds();

        if (secondsLeft <= antiSnipingWindowSeconds && secondsLeft > 0) {
            // Bid trong X giây cuối → gia hạn thêm Y giây
            this.endTime = this.endTime.plusSeconds(antiSnipingExtensionSeconds);
            // Notify observers về việc gia hạn
            for (AuctionObserver obs : observers) {
                obs.onAuctionExtended(id, endTime);
            }
            return true;
        }
        return false;
    }

    // === AUTO-BID ===
    public void addAutoBid(AutoBidConfig config) {
        bidLock.lock();
        try {
            // Remove existing config for same bidder (update scenario)
            autoBidQueue.removeIf(c -> c.getBidderId().equals(config.getBidderId()));
            autoBidQueue.add(config);
        } finally {
            bidLock.unlock();
        }
    }

    public void removeAutoBid(String bidderId) {
        bidLock.lock();
        try {
            autoBidQueue.removeIf(c -> c.getBidderId().equals(bidderId));
        } finally {
            bidLock.unlock();
        }
    }

    /**
     * Xử lý Auto-Bid PriorityQueue.
     * Gọi SAU mỗi lần placeBid thành công.
     *
     * Logic:
     * 1. Lấy auto-bid config có maxBid cao nhất (đầu PriorityQueue)
     * 2. Nếu config này KHÔNG phải người đang dẫn đầu
     *    VÀ currentHighestBid + increment <= maxBid
     *    → Tự động đặt giá = currentHighestBid + increment
     * 3. Lặp lại cho đến khi:
     *    - Không còn auto-bid nào đủ điều kiện, HOẶC
     *    - Người dẫn đầu chính là top auto-bidder
     */
    public void resolveAutoBids() {
        // KHÔNG CẦN lock vì hàm này luôn được gọi trong placeBid() đã lock sẵn

        boolean keepResolving = true;
        int maxIterations = 100; // Safety: tránh infinite loop
        int iteration = 0;

        while (keepResolving && iteration < maxIterations) {
            iteration++;
            keepResolving = false;

            // Tìm auto-bid tốt nhất (không phải current winner)
            AutoBidConfig bestConfig = null;
            for (AutoBidConfig config : autoBidQueue) {
                if (!config.getBidderId().equals(currentWinnerId)) {
                    bestConfig = config;
                    break; // PriorityQueue đã sort, lấy cái đầu tiên khác winner
                }
            }

            if (bestConfig == null) break; // Không ai auto-bid

            double newBidAmount = currentHighestBid + bestConfig.getIncrement();

            if (newBidAmount <= bestConfig.getMaxBid()) {
                // Auto-bid!
                BidRecord autoRecord = new BidRecord(
                    bestConfig.getBidderId(),
                    bestConfig.getBidderUsername() + " [AUTO]",
                    newBidAmount,
                    LocalDateTime.now()
                );
                this.currentHighestBid = newBidAmount;
                this.currentWinnerId = bestConfig.getBidderId();
                this.bidHistory.add(autoRecord);

                // Anti-sniping cho auto-bid
                extendIfSniping();

                // Notify
                notifyObservers(autoRecord);

                keepResolving = true; // Có thể còn auto-bid khác phản hồi
            } else {
                // maxBid không đủ → remove config
                autoBidQueue.remove(bestConfig);
            }
        }
    }

    // === DEPOSIT TRACKING ===
    public boolean hasDeposited(String bidderId) {
        return depositedBidders.contains(bidderId);
    }

    public void markDeposited(String bidderId) {
        depositedBidders.add(bidderId);
    }

    // === GETTERS mới ===
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
    public int getAntiSnipingWindowSeconds() { return antiSnipingWindowSeconds; }
    public int getAntiSnipingExtensionSeconds() { return antiSnipingExtensionSeconds; }
}
```

#### Task A3: Cập nhật AuctionObserver interface ⏱️ 10 phút

**File:** `domain/model/AuctionObserver.java`

```java
// THÊM method cho anti-sniping notification:
void onAuctionExtended(String auctionId, LocalDateTime newEndTime);
```

---

### BUỔI CHIỀU (14:00 → 17:00)

#### Task A4: Cập nhật Mapper + DTO ⏱️ 45 phút

**AuctionMapper.java** — cập nhật `toDomain()` để truyền anti-sniping fields:
```java
public static LiveAuction toDomain(AuctionSchema schema) {
    return new LiveAuction(
        schema.getId(), schema.getItemId(), schema.getSellerId(),
        schema.getEndTime(), schema.getStatus(),
        schema.getHighestBid(), schema.getWinnerId(),
        schema.getAntiSnipingWindowSeconds(),      // MỚI
        schema.getAntiSnipingExtensionSeconds()     // MỚI
    );
}
```

**AuctionDTO.java** — thêm anti-sniping fields để Client hiển thị.

**Tạo WalletDTO hoặc dùng Map<String, Double>** — thảo luận với Cường.

**BidMapper.java** — thêm `schemaToDTO(BidSchema)` nếu chưa có:
```java
public static BidDTO schemaToDTO(BidSchema schema) {
    return new BidDTO(schema.getAuctionId(), /* resolve username */,
                      schema.getAmount(), schema.getTimestamp());
}
```

#### Task A5: Unit Test — Bidding + Wallet logic ⏱️ 1.5 tiếng

**File mới:** `test/.../UnitTest/BiddingTest.java`

```java
// Test cases cần viết:

// === PlaceBid ===
@Test void placeBid_success() // Bid cao hơn → thành công
@Test void placeBid_tooLow()  // Bid thấp hơn → InvalidBidException
@Test void placeBid_auctionClosed() // Phiên đã đóng → AuctionClosedException
@Test void placeBid_sellerCantBid() // Seller tự bid → InvalidBidException

// === Auto-Bid ===
@Test void autoBid_singleConfig()         // 1 auto-bid → tự tăng giá
@Test void autoBid_twoCompeting()          // 2 auto-bid cạnh tranh → giá tăng đến maxBid thấp hơn
@Test void autoBid_sameMaxBid_earlyWins()  // Cùng maxBid → người đăng ký sớm thắng
@Test void autoBid_exceedsMax_removed()    // maxBid không đủ → bị remove khỏi queue
@Test void autoBid_currentWinnerSkipped()  // Người đang dẫn đầu không auto-bid chính mình

// === Anti-Sniping ===
@Test void antiSniping_bidInWindow_extends()  // Bid trong 60s cuối → endTime gia hạn
@Test void antiSniping_bidOutsideWindow_noExtend() // Bid ngoài window → không gia hạn
@Test void antiSniping_multipleExtensions()   // Gia hạn nhiều lần liên tiếp

// === Concurrent ===
@Test void concurrent_twoBidders_noLostUpdate() // 2 thread bid cùng lúc → không mất data
```

#### Task A6: Integration Test — E2E Bidding Flow ⏱️ 1 tiếng

**File mới:** `test/.../IntegrationTest/BiddingFlowTest.java`

```java
// Test luồng đầy đủ:
// 1. Tạo 2 user (seller + bidder)
// 2. Seller tạo item, tạo auction, start auction
// 3. Bidder nạp tiền (deposit)
// 4. Bidder đặt giá → kiểm tra bid lưu đúng
// 5. Auction hết giờ → kiểm tra winner đúng
// 6. Winner thanh toán → kiểm tra balance thay đổi đúng
```

---

## ⚠️ Lưu ý quan trọng

1. **Auto-Bid loop safety:** `resolveAutoBids()` phải có `maxIterations` guard để tránh infinite loop khi 2 auto-bid cạnh tranh nhau.
2. **PriorityQueue thread-safety:** `PriorityQueue` bản thân không thread-safe, nhưng vì luôn truy cập trong `bidLock.lock()` nên OK.
3. **resolveAutoBids() gọi placeBid?** KHÔNG! `resolveAutoBids()` thao tác trực tiếp trên state (currentHighestBid, bidHistory) thay vì gọi lại `placeBid()` để tránh recursive lock. Nhưng vẫn notify observers.
4. **Phối hợp với Cường:** `BidService.placeBid()` sẽ gọi `LiveAuction.placeBid()`. AutoBid logic nằm TRONG LiveAuction. BidService chỉ handle wallet (freeze/unfreeze).
5. **AuctionMapper:** Phải cập nhật `toDomain()` TRƯỚC khi Cường test AuctionService, vì AuctionService gọi `AuctionManager.loadAuction()` → `AuctionMapper.toDomain()`.
