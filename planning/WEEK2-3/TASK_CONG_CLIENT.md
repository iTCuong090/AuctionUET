# 🅲️ NHIỆM VỤ CHI TIẾT — CÔNG (Client GUI + Client Network)

> **Tuần:** 2–3 · **Branch tuần 2:** `feature/tuan-2-cong-client-gui` · **Branch tuần 3:** `feature/tuan-3-cong-client-network`
>
> **Tài liệu tham chiếu:**
> - `docs/javafx-guide.md` — Hướng dẫn JavaFX cơ bản
> - `planning/WEEK2-3/WEEK2_3_REVISED.md` — Contract 4 (Client ↔ Server message format)
> - `docs/server_architecture_overhaul.md` — Mục 4.4 (phần Client-side)

---

## MỤC LỤC

1. [PHẦN A — Tổng quan nhiệm vụ](#phần-a--tổng-quan-nhiệm-vụ)
2. [PHẦN B — Tuần 2: GUI Login + Register](#phần-b--tuần-2-gui-login--register)
3. [PHẦN C — Tuần 3: Client Network Layer](#phần-c--tuần-3-client-network-layer)
4. [PHẦN D — Test thủ công](#phần-d--test-thủ-công)
5. [PHẦN E — Dependency & Nhắc nhở phối hợp](#phần-e--dependency--nhắc-nhở-phối-hợp)
6. [PHẦN F — Checklist hoàn thành](#phần-f--checklist-hoàn-thành)

---

## PHẦN A — Tổng quan nhiệm vụ

Công phụ trách **toàn bộ phần Client** — giao diện người dùng (JavaFX) và kết nối mạng phía client.

**Tuần 2:** Tạo giao diện Login + Register đẹp, dark theme, hoạt động offline (chưa kết nối server).

**Tuần 3:** Kết nối GUI với Server qua TCP socket — bấm Login/Register thật sự gửi request tới server.

```
Tuần 2: GUI chạy độc lập
  ┌─────────────────────┐
  │ Login Screen        │  Bấm Login → print console
  │ Register Screen     │  Bấm Register → print console  
  │ SceneManager        │  Chuyển màn hình
  │ CSS Dark Theme      │
  └─────────────────────┘

Tuần 3: Kết nối Server
  ┌─────────────────────┐         ┌──────────────┐
  │ Login Screen        │ ──TCP──►│ AuctionServer │
  │ + ServerConnection  │ ◄──TCP──│ (Khánh viết)  │
  │ + AuthClient        │         └──────────────┘
  │ Dashboard Screen    │
  └─────────────────────┘
```

---

## PHẦN B — Tuần 2: GUI Login + Register

### Package đích:
```
src/main/java/com/auctionuet/client/
├── ClientApp.java              (entry point)
├── view/
│   ├── SceneManager.java
│   ├── LoginController.java
│   └── RegisterController.java
└── resources/
    ├── fxml/
    │   ├── LoginView.fxml
    │   └── RegisterView.fxml
    └── css/
        └── styles.css
```

---

### B.1 File: `ClientApp.java`

**Vai trò:** Entry point của ứng dụng client. Khởi tạo JavaFX, load LoginView.

```java
public class ClientApp extends Application {
    @Override
    public void start(Stage primaryStage) {
        SceneManager.getInstance().init(primaryStage);
        SceneManager.getInstance().switchScene("/fxml/LoginView.fxml");
        primaryStage.setTitle("AuctionUET");
        primaryStage.setWidth(800);
        primaryStage.setHeight(600);
        primaryStage.show();
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}
```

---

### B.2 File: `SceneManager.java`

**Vai trò:** Singleton quản lý chuyển đổi giữa các màn hình (Login ↔ Register ↔ Dashboard).

**Methods bắt buộc:**

| Method | Mô tả |
|--------|-------|
| `getInstance()` | Trả về Singleton instance |
| `init(Stage stage)` | Lưu reference tới Stage chính |
| `switchScene(String fxmlPath)` | Load FXML, set CSS, set scene mới cho Stage |

**Chi tiết `switchScene`:**
1. Load FXML: `FXMLLoader.load(getClass().getResource(fxmlPath))`
2. Tạo Scene mới từ root node
3. Gắn CSS: `scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm())`
4. Set scene cho stage: `stage.setScene(scene)`

---

### B.3 File: `LoginView.fxml` + `LoginController.java`

**Giao diện Login cần có:**

| Element | Kiểu | ID | Mô tả |
|---------|------|-----|-------|
| Logo/Title | Label | — | "AuctionUET" hoặc icon |
| Username | TextField | `usernameField` | Placeholder: "Tên đăng nhập" |
| Password | PasswordField | `passwordField` | Placeholder: "Mật khẩu" |
| Error Label | Label | `errorLabel` | Ẩn mặc định, hiện khi có lỗi (text đỏ) |
| Login Button | Button | `loginButton` | Text: "Đăng nhập" |
| Register Link | Hyperlink | `registerLink` | Text: "Chưa có tài khoản? Đăng ký" |

**LoginController — Methods bắt buộc:**

| Method | Trigger | Logic |
|--------|---------|-------|
| `handleLogin()` | Bấm loginButton | Validate → (tuần 2: print console) / (tuần 3: gửi server) |
| `handleRegisterLink()` | Bấm registerLink | `SceneManager.switchScene("RegisterView.fxml")` |

**Validation tuần 2 (client-side):**
- Username trống → errorLabel: "Vui lòng nhập username"
- Password trống → errorLabel: "Vui lòng nhập password"
- Validation OK → `System.out.println("Login: " + username)` (tuần 2)

---

### B.4 File: `RegisterView.fxml` + `RegisterController.java`

**Giao diện Register cần có:**

| Element | Kiểu | ID | Mô tả |
|---------|------|-----|-------|
| Title | Label | — | "Đăng ký tài khoản" |
| Username | TextField | `usernameField` | Placeholder: "Tên đăng nhập (≥ 3 ký tự)" |
| Email | TextField | `emailField` | Placeholder: "Email" |
| Password | PasswordField | `passwordField` | Placeholder: "Mật khẩu (≥ 8 ký tự)" |
| Confirm | PasswordField | `confirmField` | Placeholder: "Nhập lại mật khẩu" |
| Role | ComboBox | `roleComboBox` | Giá trị: "Bidder", "Seller" |
| Error Label | Label | `errorLabel` | Ẩn mặc định |
| Register Button | Button | `registerButton` | Text: "Đăng ký" |
| Login Link | Hyperlink | `loginLink` | Text: "Đã có tài khoản? Đăng nhập" |

**RegisterController — Validation bắt buộc:**

| Rule | Message lỗi |
|------|-------------|
| Username < 3 ký tự | "Username phải có ít nhất 3 ký tự" |
| Email không chứa @ | "Email không hợp lệ" |
| Password < 8 ký tự | "Mật khẩu phải có ít nhất 8 ký tự" |
| Password ≠ Confirm | "Mật khẩu không khớp" |
| Role chưa chọn | "Vui lòng chọn vai trò" |

---

### B.5 File: `styles.css` — Dark Theme

**Yêu cầu thiết kế:**

```css
/* Màu nền tối */
.root {
    -fx-background-color: #1a1a2e;
}

/* Input fields */
.text-field, .password-field {
    -fx-background-color: #16213e;
    -fx-text-fill: #e0e0e0;
    -fx-border-color: #0f3460;
    -fx-border-radius: 8;
    -fx-background-radius: 8;
    -fx-padding: 10;
    -fx-font-size: 14;
}

/* Input focus effect */
.text-field:focused, .password-field:focused {
    -fx-border-color: #e94560;
    -fx-effect: dropshadow(gaussian, #e94560, 10, 0, 0, 0);
}

/* Button */
.button {
    -fx-background-color: linear-gradient(to right, #e94560, #0f3460);
    -fx-text-fill: white;
    -fx-font-size: 16;
    -fx-font-weight: bold;
    -fx-padding: 12 40;
    -fx-border-radius: 8;
    -fx-background-radius: 8;
    -fx-cursor: hand;
}

/* Button hover */
.button:hover {
    -fx-background-color: linear-gradient(to right, #ff6b6b, #1a5276);
}

/* Error label */
.error-label {
    -fx-text-fill: #e94560;
    -fx-font-size: 12;
}

/* Hyperlink */
.hyperlink {
    -fx-text-fill: #4ecdc4;
    -fx-font-size: 12;
}
```

Công được tự do sáng tạo thêm. Chỉ cần đảm bảo: **dark theme, bo tròn, hover effect, giao diện hiện đại**.

---

## PHẦN C — Tuần 3: Client Network Layer

### Package bổ sung:
```
src/main/java/com/auctionuet/client/
├── network/
│   ├── ServerConnection.java     ← MỚI
│   └── AuthClient.java           ← MỚI
├── view/
│   ├── LoginController.java      ← CẬP NHẬT
│   ├── RegisterController.java   ← CẬP NHẬT
│   └── DashboardController.java  ← MỚI
└── resources/
    └── fxml/
        └── DashboardView.fxml    ← MỚI
```

---

### C.1 File: `ServerConnection.java`

**Vai trò:** Singleton quản lý kết nối TCP tới server. Mọi request đều đi qua class này.

**Methods bắt buộc:**

| Method | Mô tả |
|--------|-------|
| `getInstance()` | Singleton |
| `connect(String host, int port)` | Mở Socket TCP |
| `Response sendRequest(Request req)` | Gửi JSON, chờ đọc response JSON, trả Response |
| `disconnect()` | Đóng socket |
| `isConnected()` | Kiểm tra socket còn mở không |

**Chi tiết `sendRequest`:**
1. Serialize Request → JSON string (1 dòng)
2. Gửi qua `PrintWriter.println(json)` — tự thêm `\n`
3. Đọc response: `BufferedReader.readLine()` — blocking chờ
4. Deserialize JSON → Response object
5. Return Response

> **Lưu ý:** Method này là **blocking** — nó chờ server trả lời. GUI thread KHÔNG ĐƯỢC gọi trực tiếp (sẽ đơ giao diện). Phải gọi từ background thread.

---

### C.2 File: `AuthClient.java`

**Vai trò:** Adapter giữa GUI và Network. GUI chỉ cần gọi `login(u, p)`, AuthClient lo phần tạo Request và parse Response.

**Methods:**

| Method | Mô tả |
|--------|-------|
| `LoginResult login(String username, String password)` | Tạo Request LOGIN → sendRequest → parse response |
| `void register(String username, String password, String email, String role)` | Tạo Request REGISTER → sendRequest |
| `void logout(String token)` | Tạo Request LOGOUT → sendRequest |

---

### C.3 Cập nhật LoginController (tuần 3)

**Thay đổi chính:** Khi bấm Login, gọi `AuthClient.login()` trên **background thread**, rồi cập nhật UI trên **JavaFX thread**.

```java
public void handleLogin() {
    String username = usernameField.getText();
    String password = passwordField.getText();
    
    // Validate client-side (giữ nguyên từ tuần 2)
    if (username.isBlank()) { errorLabel.setText("..."); return; }
    
    // Gọi server trên background thread
    new Thread(() -> {
        try {
            ServerConnection.getInstance().connect("localhost", 8888);
            LoginResult result = new AuthClient().login(username, password);
            
            // Cập nhật UI trên JavaFX thread
            Platform.runLater(() -> {
                // Lưu token + user info
                SceneManager.getInstance().switchScene("/fxml/DashboardView.fxml");
            });
        } catch (Exception e) {
            Platform.runLater(() -> {
                errorLabel.setText(e.getMessage());
            });
        }
    }).start();
}
```

> **`Platform.runLater()` là gì?** JavaFX chỉ cho phép cập nhật UI từ **JavaFX Application Thread**. Code trong background thread KHÔNG được gọi `setText()`, `switchScene()` trực tiếp. `Platform.runLater()` đẩy code vào hàng đợi của JavaFX thread → an toàn.

---

### C.4 File: `DashboardView.fxml` + `DashboardController.java`

**Giao diện Dashboard (shell cơ bản):**

| Element | Mô tả |
|---------|-------|
| Header Label | "Xin chào, {username} ({role})" |
| Logout Button | Bấm → gửi LOGOUT → quay về Login |

**DashboardController:**
- Nhận `username` và `role` từ login result
- Hiển thị lên header
- Bấm Logout → gọi `AuthClient.logout(token)` → `SceneManager.switchScene("LoginView.fxml")`

---

## PHẦN D — Test thủ công

### Tuần 2 — GUI Test (không cần server)

| # | Scenario | Kết quả mong đợi |
|---|----------|-------------------|
| 1 | Chạy ClientApp | Cửa sổ Login hiện, dark theme đẹp |
| 2 | Để trống username → Login | Error: "Vui lòng nhập username" |
| 3 | Để trống password → Login | Error: "Vui lòng nhập password" |
| 4 | Bấm "Đăng ký" | Chuyển sang Register screen |
| 5 | Nhập password ≠ confirm → Register | Error: "Mật khẩu không khớp" |
| 6 | Bấm "Đăng nhập" trên Register | Quay về Login screen |
| 7 | Nhập đủ data → Register | Console: print info (tuần 2) |

### Tuần 3 — E2E Test (cần server chạy)

| # | Scenario | Kết quả mong đợi |
|---|----------|-------------------|
| 1 | Chạy Server → Chạy Client | Login screen hiện |
| 2 | Login sai password | Error label: message từ server |
| 3 | Register → quay về Login | OK message |
| 4 | Login đúng | Chuyển sang Dashboard: "Xin chào, X (BIDDER)" |
| 5 | Logout | Quay về Login |
| 6 | Kiểm tra `data/users.json` | User vừa register tồn tại, password đã hash |
| 7 | Server crash → Login | Error: "Không thể kết nối server" (không treo) |

---

## PHẦN E — Dependency & Nhắc nhở phối hợp

### E.1 Tuần 2: Công KHÔNG phụ thuộc ai ✅

GUI tuần 2 chạy hoàn toàn offline, không import code server.

### E.2 Tuần 3: Công phụ thuộc Khánh

| Công cần | Ai cung cấp | Deadline |
|----------|------------|----------|
| Server chạy (REGISTER + LOGIN OK) | **Khánh** (tuần 3) | **Thứ 5 tuần 3** |
| Protocol format (Request/Response JSON) | **Khánh** (tuần 2) | Merge cuối tuần 2 |

Công cũng dùng các protocol class (Request, Response, ActionType) — có thể import từ code Khánh hoặc tự tạo tương đương bên client.

> [!WARNING]
> **NHẮC NHỞ CHO CÔNG:**
> - **Thứ 5 tuần 3:** Nhắc Khánh: *"Khánh ơi, server chạy được chưa? Mình cần test GUI kết nối."*
> - Nếu server chưa có → Công viết **mock server** đơn giản:
>   ```java
>   // Mock: luôn trả OK
>   ServerSocket ss = new ServerSocket(8888);
>   Socket s = ss.accept();
>   BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));
>   PrintWriter out = new PrintWriter(s.getOutputStream(), true);
>   String line = in.readLine();
>   out.println("{\"type\":\"RESPONSE\",\"status\":\"OK\",\"data\":{\"token\":\"fake\"}}");
>   ```
> - Hoặc test GUI mà không cần server: bắt exception → hiện error "Không thể kết nối".

### E.3 Lưu ý về Protocol Class

Client cần biết format JSON để gửi/nhận. Có 2 cách:
1. **Dùng chung class** `Request`, `Response`, `ActionType` từ module `auction-server` — thêm dependency trong `pom.xml`
2. **Tạo class tương đương** bên client — copy structure (đơn giản hơn cho tuần 2-3)

**Khuyến nghị:** Tuần 2-3, Công tự tạo class tương đương. Tuần 4+ refactor thành shared module nếu cần.

---

## PHẦN F — Checklist hoàn thành

### Tuần 2 — Deadline: Tối thứ 7

- [ ] `ClientApp.java` — Compile, chạy được, load LoginView
- [ ] `SceneManager.java` — Compile, Singleton, switchScene hoạt động
- [ ] `LoginView.fxml` + `LoginController.java` — Giao diện đẹp, validate client-side
- [ ] `RegisterView.fxml` + `RegisterController.java` — Validate đầy đủ, chuyển màn hình
- [ ] `styles.css` — Dark theme, input bo tròn, button gradient, hover effect
- [ ] Test thủ công 7 scenarios → OK
- [ ] Code push lên branch `feature/tuan-2-cong-client-gui`

### Tuần 3 — Deadline: Tối thứ 7

- [ ] `ServerConnection.java` — Compile, Singleton, connect/send/disconnect
- [ ] `AuthClient.java` — Compile, login/register/logout methods
- [ ] `LoginController.java` cập nhật — Gọi server trên background thread
- [ ] `RegisterController.java` cập nhật — Gọi server trên background thread
- [ ] `DashboardView.fxml` + `DashboardController.java` — Hiện username + role + Logout
- [ ] Test thủ công E2E 7 scenarios → OK
- [ ] Code push lên branch `feature/tuan-3-cong-client-network`

---

> **Ghi chú cuối:** Frontend không cần viết JUnit test (test thủ công là đủ cho scope này). Tập trung vào giao diện đẹp, trải nghiệm mượt, và xử lý lỗi tốt (server crash → hiện thông báo, không treo app). Nếu gặp khó với JavaFX layout (VBox, HBox, GridPane), đọc `docs/javafx-guide.md`.
