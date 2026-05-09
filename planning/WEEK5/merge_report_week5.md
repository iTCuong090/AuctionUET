# 📋 BÁO CÁO MERGE TUẦN 5 — Bidding, Wallet & Auto-Bid

> **Ngày:** 09/05/2026 · **Nhánh target:** `develop/tuan5-complete-bidding-and-auction-feature`
> **Trạng thái:** ✅ **BUILD SUCCESS** — Cả server và client đều `mvn package` thành công

---

## 🔀 Thứ tự Merge

| # | Nhánh | Tác giả | Kết quả | Conflicts |
|---|-------|---------|---------|-----------|
| 1 | `origin/develop/tuan-5-cuong` | **Cường** | ✅ Fast-forward | 0 |
| 2 | `origin/feature/w5-anh-bidding-domain` | **Anh** | ✅ Merged + resolved | 2 |
| 3 | `origin/develop-tuan5` | **Khánh** | ✅ Merged + resolved | 5 |
| 4 | `origin/develop/tuan5-Client-Congcao` | **Công** | ✅ Merged + resolved | 6 |

**Tổng conflicts:** 13 · **Tổng commits merge:** 4

---

## 🔧 Chi tiết Conflicts & Cách giải quyết

### Merge #1: Cường (`develop/tuan-5-cuong`) → Fast-forward
- **0 conflicts** — Nhánh Cường branch từ đúng HEAD nên fast-forward.
- **12 files changed:** WalletService, BidService, AuctionService, AuctionManager, UserSchema, AuctionSchema, AutoBidConfig, LiveAuction, AuctionMapper, AuctionController, AuctionServer

### Merge #2: Anh (`feature/w5-anh-bidding-domain`) — 2 conflicts

| File | Quyết định | Lý do |
|------|-----------|-------|
| `AutoBidConfig.java` | **Chọn Anh** | Anh implement đầy đủ `Comparable<AutoBidConfig>`, có `bidderUsername`, `registeredAt`, logic `compareTo` theo đúng Contract W5.5. Cường chỉ có version đơn giản thiếu các trường quan trọng. |
| `LiveAuction.java` | **Merge cả hai** | Lấy Anh làm base (có PriorityQueue auto-bid, anti-sniping, observer pattern, resolveAutoBids, extendIfSniping, deposit tracking). Giữ lại Map-based `autoBids` và `getAutoBidConfig()` từ Cường cho quick lookup. |

### Merge #3: Khánh (`develop-tuan5`) — 5 conflicts

| File | Quyết định | Lý do |
|------|-----------|-------|
| `AuctionObserver.java` | **Merge** | Khánh có Javadoc đẹp hơn. Cả hai đều có `onAuctionExtended`, conflict chỉ ở comment. |
| `BidService.java` | **Chọn Cường** | Cường có full implementation (placeBid + wallet freeze/unfreeze + auto-bid). Khánh chỉ có constructor stub rỗng. |
| `WalletService.java` | **Chọn Cường** | Cường viết đầy đủ 6 methods (deposit, withdraw, freeze, unfreeze, forfeit, getWallet). Khánh chỉ có constructor stub. |
| `AuctionController.java` | **Chọn Cường** | Cường có defensive coding với `containsKey` + default values (60/120) cho anti-sniping. Khánh có bug: trailing space trong key name `"antiSnipingExtensionSeconds "`. |
| `AuctionServer.java` | **Merge** | Khánh có duplicate declarations (2x WalletService, 2x BidService). Lấy Cường's constructor cho BidService (có AuctionManager). Loại bỏ duplicates. |

### Merge #4: Công (`develop/tuan5-Client-Congcao`) — 6 conflicts

| File | Quyết định | Lý do |
|------|-----------|-------|
| `AuctionClient.java` | **Chọn Công** | Thêm `antiSnipingWindowSeconds`, `antiSnipingExtensionSeconds` vào method signature `createAuction`. |
| `DashboardController.java` | **Chọn Công** | Thêm `btnWallet` vào sidebar. |
| `ProfileController.java` | **Chọn Công** | Thêm `WalletClient` integration, `loadWalletBalance()` dynamic, `handleTopUp()` navigation. |
| `styles.css` | **Merge** | Thêm `.text-area` selector từ Công, giữ login/register titles. |
| `CreateItemView.fxml` | **Chọn Công** | Đổi TextArea từ `styleClass="text-field"` → `"text-area"` (chính xác hơn). |
| `DashboardView.fxml` | **Chọn Công** | Thêm nút `💰 Ví tiền` vào sidebar. |

---

## 🐛 Bugs phát hiện & Fix trong quá trình merge

| # | Bug | File | Tác giả gốc | Fix |
|---|-----|------|-------------|-----|
| 1 | `getBidHistory()` thiếu tham số `auctionId` | `BidController.java` | Khánh | Fix: `getBidHistory()` → `getBidHistory(auctionId)` |
| 2 | `BidMapper.schemaToDTO()` có `/* resolve username */` placeholder | `BidMapper.java` | Anh | Fix: thay bằng `schema.getBidderId()` |
| 3 | `AuctionMapper` class declaration bị mất — `toDomain()` nằm ngoài class | `AuctionMapper.java` | Merge artifact | Fix: thêm `public class AuctionMapper {` wrapper |
| 4 | Java source/target = 25 (JDK 24 không hỗ trợ) | `auction-server/pom.xml` | Anh | Fix: đổi về 21 (match parent pom) |
| 5 | `AutoBidConfig` constructor gọi 3 args, phải 4 args (thiếu `bidderUsername`) | `BidService.java` | Cường | Fix: thêm `bidder.getUsername()` |
| 6 | Missing `ThemeManager` class (referenced but never created) | Client | Anh/Công | Công đã push file ThemeManager trong nhánh riêng |
| 7 | Duplicate WalletService/BidService declarations trong AuctionServer | `AuctionServer.java` | Khánh | Fix: loại bỏ duplicates |
| 8 | Anti-sniping key có trailing space `"antiSnipingExtensionSeconds "` | `AuctionController.java` | Khánh | Fix: dùng Cường's defensive `containsKey` + defaults |

---

## 👥 Đánh giá từng thành viên

### 🅰️ Cường — ⭐⭐⭐⭐⭐ Xuất sắc
**Nhiệm vụ:** Persistence + Service Layer (WalletService, BidService, AuctionService, Schema updates)

| Tiêu chí | Đánh giá |
|----------|----------|
| Hoàn thành task | ✅ 100% — Tất cả service đều có implementation đầy đủ |
| Chất lượng code | ⭐⭐⭐⭐⭐ — synchronized methods, validation checks, error handling |
| Đúng Contract | ✅ Đúng Contract W5.2, W5.3, W5.4 |
| Merge dễ dàng | ✅ Fast-forward, không gây conflict nào |

**Nhận xét:** Cường là backbone của tuần này. WalletService, BidService, AuctionService payment flow đều chạy đúng. Code defensive, có null checks, synchronized cho thread safety. `AutoBidConfig` constructor hơi đơn giản nhưng logic đúng.

### 🅳️ Anh — ⭐⭐⭐⭐ Tốt
**Nhiệm vụ:** Domain Model + Test (AutoBidConfig, LiveAuction, AuctionObserver, BidMapper, BidDTO)

| Tiêu chí | Đánh giá |
|----------|----------|
| Hoàn thành task | ✅ 90% — Domain model hoàn chỉnh, thiếu unit tests |
| Chất lượng code | ⭐⭐⭐⭐ — PriorityQueue auto-bid, anti-sniping rất tốt |
| Đúng Contract | ✅ Đúng Contract W5.5, W5.6 |
| Vấn đề | ⚠️ pom.xml Java 25, BidMapper.schemaToDTO placeholder, client FXML redesign lớn |

**Nhận xét:** Anh viết LiveAuction rất chất lượng — PriorityQueue-based auto-bid, anti-sniping, Observer pattern, ReentrantLock thread safety. Tuy nhiên BidMapper có placeholder code chưa hoàn thiện, và thay đổi pom.xml Java 25 gây build fail. Client UI redesign mạnh tay nhưng thiếu ThemeManager class.

### 🅱️ Khánh — ⭐⭐⭐ Trung bình
**Nhiệm vụ:** Controller + Router + Push Notification (BidController, WalletController, RequestRouter, ClientHandler, ActionType, Permission)

| Tiêu chí | Đánh giá |
|----------|----------|
| Hoàn thành task | ✅ 80% — Controllers và Router hoạt động, có bugs |
| Chất lượng code | ⭐⭐⭐ — Cấu trúc đúng, nhưng nhiều lỗi nhỏ |
| Đúng Contract | ✅ ActionType, Permission, Router routes đúng |
| Vấn đề | ⚠️ Nhiều bugs: duplicate declarations, missing args, trailing space in key |

**Nhận xét:** Khánh hoàn thành phần routing và controller skeleton đúng theo spec. BidController có đầy đủ 6 handlers (PLACE_BID, GET_BID_HISTORY, SET_AUTO_BID, CANCEL_AUTO_BID, SUBSCRIBE, UNSUBSCRIBE). WalletController 3 handlers. ClientHandler implement AuctionObserver với push notifications — rất tốt theo Contract W5.9. Tuy nhiên BidService/WalletService chỉ là constructor stubs (phụ thuộc Cường), có bug `getBidHistory()` thiếu param, duplicate declarations trong AuctionServer, và trailing space trong key name.

### 🅲️ Công — ⭐⭐⭐⭐ Tốt
**Nhiệm vụ:** Client GUI + Network (BidClient, WalletClient, BiddingView, WalletView, ThemeManager, ServerConnection upgrade)

| Tiêu chí | Đánh giá |
|----------|----------|
| Hoàn thành task | ✅ 95% — Gần như hoàn chỉnh client-side |
| Chất lượng code | ⭐⭐⭐⭐ — GUI đẹp, network client đầy đủ |
| Đúng Contract | ✅ Client ActionType đồng bộ, push listener, WalletView, BiddingView |
| Vấn đề | ⚠️ Push muộn, nhánh khác tên kế hoạch |

**Nhận xét:** Công deliver toàn bộ client-side features: BiddingView (đặt giá, auto-bid config, bid history), WalletView (nạp/rút tiền QR), ThemeManager (light/dark mode), BidClient + WalletClient network classes, ServerConnection upgrade với listener thread + push message support. ProfileView cũng được upgrade với real wallet balance. Code chất lượng tốt, có Platform.runLater() cho UI updates. Điểm trừ: push code muộn và nhánh khác tên kế hoạch ban đầu.

---

## 📊 Tổng kết Feature Coverage

| # | Feature | Trạng thái | Contributors |
|---|---------|-----------|--------------|
| F1 | **Bidding Core** | ✅ Hoàn thành | Cường (Service) + Khánh (Controller) + Anh (Domain) + Công (Client) |
| F2 | **Wallet System** | ✅ Hoàn thành | Cường (Service) + Khánh (Controller) + Công (Client) |
| F3 | **Deposit/Freeze** | ✅ Hoàn thành | Cường (BidService + WalletService) + Anh (LiveAuction.depositedBidders) |
| F4 | **Auto-Bid** | ✅ Hoàn thành | Anh (PriorityQueue + resolveAutoBids) + Cường (BidService.setAutoBid) + Công (Client UI) |
| F5 | **Anti-Sniping** | ✅ Hoàn thành | Anh (LiveAuction.extendIfSniping) + Cường (AuctionSchema fields) |
| F6 | **Payment Flow** | ✅ Hoàn thành | Cường (AuctionService.payAuction + 24h timeout) + Khánh (PAY_AUCTION route) |
| F7 | **Realtime Push** | ✅ Hoàn thành | Khánh (ClientHandler Observer + SUBSCRIBE/UNSUBSCRIBE) + Công (Client listener) |

---

## ✅ Build Status

```
[INFO] Reactor Summary for auction-uet 1.0-SNAPSHOT:
[INFO] auction-uet ........................................ SUCCESS
[INFO] auction-server ..................................... SUCCESS
[INFO] auction-client ..................................... SUCCESS
[INFO] BUILD SUCCESS
```

> [!IMPORTANT]
> Tất cả merge đã hoàn thành. Project compile và package thành công.
> Nhánh `develop/tuan5-complete-bidding-and-auction-feature` sẵn sàng để push và test.
