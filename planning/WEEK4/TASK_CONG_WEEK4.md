# 🅲️ NHIỆM VỤ CHI TIẾT — CÔNG (Tuần 4: Client GUI Auction + Client Network)

> **Tuần:** 4 · **Branch:** `feature/tuan-4-cong-auction-gui`
>
> **Tài liệu tham chiếu bắt buộc đọc trước khi code:**
> - `docs/diagrams_flow_auction.md` — Luồng Create Item, Get Auctions (hiểu data flow)
> - `docs/javafx-guide.md` — JavaFX guide
> - `planning/WEEK4/WEEK4_OVERVIEW.md` — Contract 11 (Client ↔ Server Protocol)

---

## MỤC LỤC

1. [PHẦN A — Hiểu vai trò tuần 4 của Công](#phần-a--hiểu-vai-trò-tuần-4-của-công)
2. [PHẦN B — Nửa đầu tuần: GUI cho Auction Feature](#phần-b--nửa-đầu-tuần-gui-cho-auction-feature)
3. [PHẦN C — Nửa sau tuần: Client Network + Kết nối GUI ↔ Server](#phần-c--nửa-sau-tuần-client-network--kết-nối-gui--server)
4. [PHẦN D — Test bắt buộc](#phần-d--test-bắt-buộc)
5. [PHẦN E — Dependency & Nhắc nhở phối hợp](#phần-e--dependency--nhắc-nhở-phối-hợp)
6. [PHẦN F — Checklist hoàn thành](#phần-f--checklist-hoàn-thành)

---

## PHẦN A — Hiểu vai trò tuần 4 của Công

### A.1 Tuần 4 Công làm gì?

Tuần 2-3, Công đã xây GUI cho Auth (Login, Register, Dashboard shell) và Client Network layer (ServerConnection, AuthClient).

Tuần 4 Công mở rộng Client cho cả Auction Feature:

```
TUẦN 2-3 (đã có):                   TUẦN 4 (MỚI):
├── view/                            ├── view/
│   ├── LoginController ✅           │   ├── LoginController ✅ (không sửa)
│   ├── RegisterController ✅        │   ├── RegisterController ✅ (không sửa)
│   ├── DashboardController ✅       │   ├── DashboardController ← CẬP NHẬT (sidebar nav)
│   └── SceneManager ✅              │   ├── CreateItemController ← MỚI
│                                    │   ├── AuctionListController ← MỚI
│                                    │   ├── CreateAuctionController ← MỚI
│                                    │   └── AuctionDetailController ← MỚI
├── network/                         ├── network/
│   ├── ServerConnection ✅          │   ├── ServerConnection ✅ (không sửa)
│   ├── AuthClient ✅                │   ├── AuthClient ✅ (không sửa)
│                                    │   ├── ItemClient ← MỚI
│                                    │   └── AuctionClient ← MỚI
└── resources/                       └── resources/
    ├── fxml/                            ├── fxml/
    │   ├── LoginView ✅                 │   ├── CreateItemView.fxml ← MỚI
    │   ├── RegisterView ✅              │   ├── AuctionListView.fxml ← MỚI
    │   └── DashboardView ✅             │   ├── CreateAuctionView.fxml ← MỚI
    └── css/                             │   └── AuctionDetailView.fxml ← MỚI
        └── styles.css ✅                └── css/
                                             └── styles.css ← CẬP NHẬT
```

### A.2 UI Flow tuần 4

```
Login → Dashboard (Sidebar Navigation)
              │
              ├── [Seller] Sản phẩm của tôi (My Items)
              │      └── Nút "Đăng sản phẩm mới" → CreateItemView
              │
              ├── [Seller] Tạo phiên đấu giá → CreateAuctionView
              │      └── Chọn item → chọn thời gian → tạo auction
              │
              ├── [Tất cả] Danh sách đấu giá → AuctionListView
              │      └── Click 1 auction → AuctionDetailView
              │
              └── [Tất cả] Hồ sơ (Profile)
```

### A.3 Thiết kế UI theo role

**Sidebar khác nhau cho từng role:**

- **Bidder:** Danh sách đấu giá, Hồ sơ, Đăng xuất
- **Seller:** Sản phẩm của tôi, Tạo phiên đấu giá, Danh sách đấu giá, Hồ sơ, Đăng xuất
- **Admin:** Quản lý người dùng, Danh sách đấu giá, Hồ sơ, Đăng xuất

---

## PHẦN B — Nửa đầu tuần: GUI cho Auction Feature

### B.1 Cập nhật: `DashboardView.fxml` + `DashboardController.java`

**Mô tả:** Thêm **sidebar navigation** thay vì chỉ có header cũ.

**Layout:**

```
┌─────────────────────────────────────────────────────┐
│  🏛️ AuctionUET                    ◎ Cuong (Seller) │  ← Header
├──────────┬──────────────────────────────────────────┤
│ Sidebar  │                                          │
│          │          CONTENT AREA                     │
│ ☐ Home   │   (thay đổi nội dung khi click sidebar) │
│ ☐ Items  │                                          │
│ ☐ Create │                                          │
│ ☐ Auction│                                          │
│ ☐ Profile│                                          │
│          │                                          │
│ [Logout] │                                          │
├──────────┴──────────────────────────────────────────┤
│  Footer: © 2026 AuctionUET                         │
└─────────────────────────────────────────────────────┘
```

**DashboardController cần làm:**
1. Nhận thông tin user (role) sau khi login
2. Dựa vào role → hiện/ẩn menu items tương ứng
3. Khi click menu → load FXML tương ứng vào Content Area
4. Highlight menu item đang active

**CSS cho Sidebar (dark theme):**
```css
.sidebar {
    -fx-background-color: #1a1a2e;
    -fx-min-width: 200px;
}
.sidebar-item {
    -fx-padding: 12 20;
    -fx-text-fill: #a0a0b0;
    -fx-cursor: hand;
}
.sidebar-item:hover {
    -fx-background-color: #16213e;
    -fx-text-fill: #ffffff;
}
.sidebar-item.active {
    -fx-background-color: #0f3460;
    -fx-text-fill: #e94560;
    -fx-border-color: #e94560;
    -fx-border-width: 0 0 0 3;
}
```

---

### B.2 File MỚI: `CreateItemView.fxml` + `CreateItemController.java`

**Mô tả:** Form đăng sản phẩm mới lên hệ thống. Chỉ Seller mới thấy menu này.

**Giao diện form:**

```
┌──────────────────────────────────────┐
│  📦 Đăng sản phẩm mới               │
│                                      │
│  Loại sản phẩm: [ComboBox ▼]        │  ← ELECTRONICS / ART / VEHICLE
│  ────────────────────────────        │
│  Tên sản phẩm:  [________________]  │
│  Mô tả:         [________________]  │  ← TextArea nhiều dòng
│                  [________________]  │
│  Giá khởi điểm: [________________]  │  ← Chỉ nhận số
│  Tình trạng:    [ComboBox ▼      ]  │  ← NEW / LIKE_NEW / GOOD / FAIR / POOR
│  Ảnh sản phẩm:  [Chọn file...   ]  │  ← FileChooser hoặc TextField URL
│                  [Preview ảnh    ]  │
│  ────────────────────────────        │
│  ⚡ Thuộc tính riêng (theo loại):   │  ← Thay đổi theo ComboBox trên
│                                      │
│  [Nếu ELECTRONICS:]                 │
│  Thương hiệu:   [________________]  │
│  Bảo hành (th):  [________________]  │
│                                      │
│  [Nếu ART:]                         │
│  Nghệ sĩ:       [________________]  │
│  Năm sáng tác:   [________________]  │
│  Chất liệu:     [________________]  │
│                                      │
│  [Nếu VEHICLE:]                     │
│  Hãng xe:        [________________]  │
│  Dòng xe:        [________________]  │
│  Số km đã đi:    [________________]  │
│  Năm sản xuất:   [________________]  │
│                                      │
│         [   Đăng sản phẩm   ]       │  ← Button nổi bật, gradient
│                                      │
│  [Label lỗi/thành công]             │
└──────────────────────────────────────┘
```

**Logic trong Controller:**
1. Khi ComboBox "Loại sản phẩm" thay đổi → hiện/ẩn các trường riêng tương ứng (VBox visibility toggle)
2. Validation client-side: tên không rỗng, giá > 0, loại đã chọn
3. Khi bấm "Đăng sản phẩm":
   - **Nửa đầu tuần:** Print ra console (chưa kết nối server)
   - **Nửa sau tuần:** Gọi `ItemClient.createItem(...)` trên background thread

---

### B.3 File MỚI: `AuctionListView.fxml` + `AuctionListController.java`

**Mô tả:** Hiển thị danh sách phiên đấu giá. TẤT CẢ role đều xem được.

**Giao diện:**

```
┌──────────────────────────────────────────────────────┐
│  🏛️ Danh sách phiên đấu giá                         │
│                                                      │
│  Lọc: [Tất cả ▼]  [Tìm kiếm..._______________] 🔍  │
│                                                      │
│  ┌────────────────┐  ┌────────────────┐  ┌──────┐   │
│  │ 📱 iPhone 15   │  │ 🎨 Mona Lisa  │  │ ...  │   │
│  │                │  │                │  │      │   │
│  │ Giá: 1.000.000 │  │ Giá: 5.000.000│  │      │   │
│  │ 🟢 RUNNING     │  │ 🟡 OPEN       │  │      │   │
│  │ ⏰ 2:30:15     │  │ ⏰ Chưa bắt đầu│  │      │   │
│  │ Seller: cuong  │  │ Seller: anh   │  │      │   │
│  │ 5 bids         │  │ 0 bids        │  │      │   │
│  │ [Xem chi tiết] │  │ [Xem chi tiết]│  │      │   │
│  └────────────────┘  └────────────────┘  └──────┘   │
│                                                      │
│  ┌────────────────┐  ┌────────────────┐              │
│  │ 🚗 Toyota Cam  │  │ ...            │              │
│  │ ...            │  │ ...            │              │
│  └────────────────┘  └────────────────┘              │
│                                                      │
│  [< 1  2  3  ... >]                                 │  ← Pagination (optional)
└──────────────────────────────────────────────────────┘
```

**Thiết kế card:**
- Mỗi auction là 1 card có gradient border
- Icon theo loại sản phẩm (📱 Electronics, 🎨 Art, 🚗 Vehicle)
- Badge trạng thái: 🟢 RUNNING, 🟡 OPEN, 🔴 FINISHED, ⚫ CANCELED
- Countdown timer hiển thị thời gian còn lại (nếu RUNNING)
- Hiệu ứng hover: card nhô lên + shadow

**Controller logic:**
1. Khi load view → gọi `AuctionClient.getAuctions()` trên background thread
2. Nhận List<AuctionDTO> → `Platform.runLater()` → render thành list card
3. Click "Xem chi tiết" → load AuctionDetailView với auctionId
4. Filter ComboBox: Tất cả / OPEN / RUNNING / FINISHED

**CSS cho card:**
```css
.auction-card {
    -fx-background-color: #16213e;
    -fx-background-radius: 12;
    -fx-padding: 16;
    -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 8, 0.2, 0, 2);
    -fx-cursor: hand;
}
.auction-card:hover {
    -fx-translate-y: -4;
    -fx-effect: dropshadow(gaussian, rgba(233,69,96,0.4), 16, 0.3, 0, 4);
}
.status-badge-running {
    -fx-background-color: #2ecc71;
    -fx-background-radius: 20;
    -fx-padding: 4 12;
    -fx-text-fill: white;
}
.status-badge-open {
    -fx-background-color: #f39c12;
    -fx-background-radius: 20;
    -fx-padding: 4 12;
    -fx-text-fill: white;
}
```

---

### B.4 File MỚI: `CreateAuctionView.fxml` + `CreateAuctionController.java`

**Mô tả:** Form tạo phiên đấu giá cho sản phẩm đã đăng. Chỉ Seller.

**Giao diện:**

```
┌──────────────────────────────────────┐
│  ⚡ Tạo phiên đấu giá               │
│                                      │
│  Chọn sản phẩm: [ComboBox ▼       ] │  ← Load từ GET_MY_ITEMS
│                                      │
│  ┌─ Thông tin SP đã chọn ─────────┐ │
│  │ Tên: iPhone 15 Pro Max          │ │
│  │ Giá khởi điểm: 25.000.000đ     │ │
│  │ Loại: Electronics               │ │
│  └──────────────────────────────────┘│
│                                      │
│  Tiêu đề phiên: [________________] │
│  Mô tả phiên:   [________________] │
│  Thời gian bắt đầu: [📅] [🕐]      │  ← DatePicker + TimePicker
│  Thời gian kết thúc: [📅] [🕐]      │
│                                      │
│  ⏱️ Thời lượng dự kiến: 2 giờ 30 phút│  ← Auto-calculate
│                                      │
│         [   Tạo phiên đấu giá  ]    │
│                                      │
│  [Label lỗi/thành công]             │
└──────────────────────────────────────┘
```

**Controller logic:**
1. Khi load view → gọi `ItemClient.getMyItems()` → populate ComboBox
2. Khi chọn item → hiện thông tin chi tiết bên dưới
3. Validation: item đã chọn, startTime > now, endTime > startTime
4. Khi bấm tạo → `AuctionClient.createAuction(...)` trên background thread
5. Thành công → chuyển về AuctionListView + hiện thông báo

---

### B.5 File MỚI: `AuctionDetailView.fxml` + `AuctionDetailController.java`

**Mô tả:** Chi tiết phiên đấu giá. Hiển thị thông tin sản phẩm, trạng thái, giá hiện tại.

**Giao diện:**

```
┌──────────────────────────────────────────────┐
│  🏛️ Chi tiết phiên đấu giá                   │
│                                              │
│  ┌─────────────┬─────────────────────────┐   │
│  │  [Ảnh SP]   │ 📱 iPhone 15 Pro Max    │   │
│  │             │ Seller: seller_cuong     │   │
│  │             │ Loại: Electronics        │   │
│  │             │ Thương hiệu: Apple      │   │
│  │             │ Tình trạng: NEW          │   │
│  │             │                          │   │
│  │             │ Mô tả: Lorem ipsum...   │   │
│  └─────────────┴─────────────────────────┘   │
│                                              │
│  ┌──────────────────────────────────────┐    │
│  │  💰 Giá hiện tại: 25.500.000đ       │    │
│  │  👤 Người dẫn đầu: bidder_khanh     │    │
│  │  ⏰ Thời gian còn lại: 01:45:30     │    │
│  │  📊 Trạng thái: 🟢 RUNNING          │    │
│  └──────────────────────────────────────┘    │
│                                              │
│  [Nếu Seller + auction OPEN:]               │
│         [ 🚀 Bắt đầu phiên đấu giá ]       │
│                                              │
│  [Phần đặt giá — TUẦN 5]                    │
│  [Lịch sử đặt giá — TUẦN 5]                │
└──────────────────────────────────────────────┘
```

> [!NOTE]
> Tuần 4: Chỉ hiển thị thông tin + nút Start. Phần PLACE_BID và lịch sử bid sẽ làm tuần 5.

---

## PHẦN C — Nửa sau tuần: Client Network + Kết nối GUI ↔ Server

### C.1 File MỚI: `ItemClient.java`

**Đường dẫn:** `client/network/ItemClient.java`

**Vai trò:** Adapter giữa GUI và Network cho tất cả thao tác liên quan đến sản phẩm.

**Methods:**

```java
public class ItemClient {

    public ItemDTO createItem(String token, Map<String, Object> itemData) {
        // 1. Tạo Request(CREATE_ITEM, itemData, token)
        // 2. ServerConnection.sendRequest(request)
        // 3. Parse response → ItemDTO hoặc throw
    }

    public List<ItemDTO> getMyItems(String token) {
        // 1. Tạo Request(GET_MY_ITEMS, null, token)
        // 2. ServerConnection.sendRequest(request)
        // 3. Parse response.data → List<ItemDTO>
    }
}
```

---

### C.2 File MỚI: `AuctionClient.java`

**Đường dẫn:** `client/network/AuctionClient.java`

**Methods:**

```java
public class AuctionClient {

    public AuctionDTO createAuction(String token, String itemId,
            String startTime, String endTime, String title, String description) {
        // Tạo Request(CREATE_AUCTION, {itemId, startTime, endTime, title, description}, token)
        // sendRequest → parse → AuctionDTO
    }

    public void startAuction(String token, String auctionId) {
        // Tạo Request(START_AUCTION, {auctionId}, token)
        // sendRequest → check OK
    }

    public List<AuctionDTO> getAuctions(String token) {
        // Tạo Request(GET_AUCTIONS, null, token)
        // sendRequest → parse → List<AuctionDTO>
    }

    public AuctionDTO getAuctionDetail(String token, String auctionId) {
        // Tạo Request(GET_AUCTION_DETAIL, {auctionId}, token)
        // sendRequest → parse → AuctionDTO
    }
}
```

---

### C.3 Kết nối GUI → Network

**Pattern cho mọi Controller (background thread + Platform.runLater):**

```java
// Trong CreateItemController, khi bấm nút "Đăng sản phẩm":
@FXML
private void onCreateItem(ActionEvent event) {
    // 1. Disable nút, hiện loading
    createButton.setDisable(true);
    loadingIndicator.setVisible(true);
    errorLabel.setVisible(false);

    // 2. Lấy dữ liệu từ form
    Map<String, Object> itemData = collectFormData();

    // 3. Gọi network trên background thread
    new Thread(() -> {
        try {
            ItemDTO result = ItemClient.createItem(currentToken, itemData);

            Platform.runLater(() -> {
                // 4. Thành công → hiện thông báo + reset form
                showSuccess("Đăng sản phẩm thành công! ID: " + result.getId());
                resetForm();
            });
        } catch (Exception e) {
            Platform.runLater(() -> {
                // 5. Lỗi → hiện message
                showError(e.getMessage());
            });
        } finally {
            Platform.runLater(() -> {
                createButton.setDisable(false);
                loadingIndicator.setVisible(false);
            });
        }
    }).start();
}
```

---

### C.4 Lưu trạng thái user (Session client-side)

Sau khi login, Client cần lưu `token` và `UserDTO` (để biết role, username). Tạo class đơn giản:

```java
public class ClientSession {
    private static ClientSession instance;
    private String token;
    private UserDTO currentUser;

    // Singleton methods
    public static ClientSession getInstance() { ... }
    public void login(String token, UserDTO user) { ... }
    public void logout() { ... }
    public String getToken() { ... }
    public UserDTO getCurrentUser() { ... }
    public boolean isSeller() { return currentUser.getRole() == UserRole.SELLER; }
}
```

---

## PHẦN D — Test bắt buộc (thủ công)

| # | Kịch bản | Kết quả mong đợi |
|---|----------|-------------------|
| 1 | Chạy Client → Login Seller → Sidebar có menu "Sản phẩm", "Tạo đấu giá" | Sidebar đúng role |
| 2 | Login Bidder → Sidebar KHÔNG có "Sản phẩm", "Tạo đấu giá" | Sidebar ẩn menu riêng Seller |
| 3 | Seller → "Đăng sản phẩm" → Chọn Electronics → Hiện trường Brand, Warranty | Dynamic form |
| 4 | Chuyển sang Art → Trường Brand biến mất, hiện Artist, Year | Dynamic form switch |
| 5 | Để trống tên → bấm Đăng → Hiện lỗi "Vui lòng nhập tên" | Client validation |
| 6 | Nhập đầy đủ → bấm Đăng → Hiện "Đăng sản phẩm thành công" | E2E with server |
| 7 | "Tạo đấu giá" → ComboBox hiện danh sách item của seller | Load items from server |
| 8 | Chọn item + thời gian → Tạo → Thành công → Chuyển về danh sách | E2E create auction |
| 9 | "Danh sách đấu giá" → Hiển thị cards với đúng thông tin | GET_AUCTIONS |
| 10 | Click "Xem chi tiết" → Hiện chi tiết auction + sản phẩm | GET_AUCTION_DETAIL |
| 11 | Seller bấm "Bắt đầu phiên" → Status đổi thành RUNNING | START_AUCTION |
| 12 | Server tắt → Client hiển thị "Mất kết nối" | Error handling |

---

## PHẦN E — Dependency & Nhắc nhở phối hợp

### E.1 Nửa đầu tuần: Công KHÔNG phụ thuộc ai

GUI hoàn toàn độc lập. Chỉ cần biết cấu trúc data (Contract 9, 11) để thiết kế form.

### E.2 Nửa sau tuần: Công phụ thuộc Khánh

| Công cần | Ai cung cấp | Deadline |
|----------|-------------|---------|
| Protocol CREATE_ITEM format | **Khánh** (Contract 11) | Đầu tuần |
| Protocol GET_AUCTIONS format | **Khánh** | Đầu tuần |
| Server chạy được, xử lý requests | **Khánh + Cường** | **Thứ 6** |

> [!WARNING]
> **NHẮC NHỞ:**
> - **Thứ 5-6:** Công nhắc Khánh: "Khánh ơi, server chạy được chưa? Tôi cần test GUI"
> - Nếu server chưa chạy → Công mock response:
>   ```java
>   // Mock tạm trong AuctionClient
>   public List<AuctionDTO> getAuctions(String token) {
>       // Return hardcoded list để test UI
>       return List.of(mockAuction1, mockAuction2);
>   }
>   ```

---

## PHẦN F — Checklist hoàn thành

### Nửa đầu tuần — Deadline: Tối thứ 4

- [ ] Dashboard có sidebar navigation theo role
- [ ] `CreateItemView.fxml` — Form đầy đủ với dynamic fields theo loại
- [ ] `AuctionListView.fxml` — Danh sách cards đẹp, dark theme
- [ ] `CreateAuctionView.fxml` — Form tạo phiên đấu giá
- [ ] `AuctionDetailView.fxml` — Trang chi tiết auction
- [ ] Tất cả FXML + Controller compile
- [ ] Client validation hoạt động
- [ ] Code push lên branch

### Nửa sau tuần — Deadline: Tối thứ 7

- [ ] `ItemClient.java` — createItem, getMyItems
- [ ] `AuctionClient.java` — createAuction, startAuction, getAuctions, getAuctionDetail
- [ ] `ClientSession.java` — lưu token + role
- [ ] GUI kết nối server: tạo item, tạo auction, xem danh sách
- [ ] Background thread cho tất cả network calls
- [ ] **12 test thủ công ALL PASS** ✅
- [ ] Code push lên branch
- [ ] Tham gia meeting cuối tuần, demo live

---

> **Ghi chú:** Phần hay nhất tuần 4 của Công là **dynamic form** cho CreateItemView và **card layout** cho AuctionListView. Hãy tham khảo các marketplace UI (eBay, Shopee) để lấy cảm hứng thiết kế. Dark theme + gradient cards là điểm cộng lớn.
