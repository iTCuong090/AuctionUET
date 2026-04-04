# 🅱️ NHIỆM VỤ CHI TIẾT — KHÁNH (Network Layer + Controller/Router)

> **Tuần:** 2–3 · **Branch tuần 2:** `feature/tuan-2-khanh-network` · **Branch tuần 3:** `feature/tuan-3-khanh-controller`
>
> **Tài liệu tham chiếu bắt buộc đọc trước khi code:**
> - `docs/server_architecture_overhaul.md` — Mục 3 (Package), Mục 4.4 (Network)
> - `docs/diagrams_class_hierarchy.md` — Mục 5 (ClientHandler), Mục 6 (DTO)
> - `docs/diagrams_flow_auth.md` — TOÀN BỘ (Login, Register, Logout, Token Validation)
> - `planning/WEEK2-3/WEEK2_3_REVISED.md` — Contract 3 + Contract 4

---

## MỤC LỤC

1. [PHẦN A — Hiểu vai trò của tầng Network](#phần-a--hiểu-vai-trò-của-tầng-network)
2. [PHẦN B — Tuần 2: Chi tiết từng file phải viết](#phần-b--tuần-2-chi-tiết-từng-file-phải-viết)
3. [PHẦN C — Tuần 3: AuthController + RequestRouter](#phần-c--tuần-3-authcontroller--requestrouter)
4. [PHẦN D — Test bắt buộc](#phần-d--test-bắt-buộc)
5. [PHẦN E — Dependency & Nhắc nhở phối hợp](#phần-e--dependency--nhắc-nhở-phối-hợp)
6. [PHẦN F — Checklist hoàn thành](#phần-f--checklist-hoàn-thành)

---

## PHẦN A — Hiểu vai trò của tầng Network

### A.1 Tầng Network là gì và tại sao cần nó?

Tầng Network là **lớp ngoài cùng** của server. Nó là nơi duy nhất giao tiếp trực tiếp với Client. Hãy tưởng tượng:

```
Client (Công viết)     ←—TCP Socket—→     Server (Khánh viết Network, Cường viết Logic)
     │                                         │
     │  "Tôi muốn đăng nhập"                   │
     │  ─────────JSON──────────►                │
     │                            ClientHandler nhận JSON
     │                            Router phân loại request
     │                            Controller xử lý
     │                            ◄── trả kết quả ──
     │  ◄─────────JSON──────────                │
     │  "OK, đây là token của bạn"              │
```

**Vai trò cụ thể:**
1. **Lắng nghe kết nối** từ client (mở cổng TCP, chờ client kết nối)
2. **Nhận message** (đọc JSON từ socket, chuyển thành Java object)
3. **Phân loại request** (LOGIN? REGISTER? PLACE_BID? → chuyển đến đúng Controller)
4. **Gọi logic nghiệp vụ** (Controller gọi xuống Service → Service gọi DAO → kết quả)
5. **Trả response** (chuyển kết quả thành JSON, gửi lại client qua socket)
6. **Push notification** (chủ động gửi thông báo cho client mà không cần client hỏi — dùng cho đấu giá realtime)

### A.2 Cách Client giao tiếp với Server — TCP Socket

#### Socket là gì?

Socket là **2 đầu ống** nối Client với Server qua mạng. Khi Client kết nối tới Server:
- Server tạo 1 **ServerSocket** lắng nghe trên 1 port (ví dụ: 8888)
- Client tạo 1 **Socket** kết nối tới `localhost:8888`
- Khi kết nối thành công, mỗi bên có 1 cặp **InputStream + OutputStream**:
  - Client ghi vào OutputStream → Server đọc từ InputStream
  - Server ghi vào OutputStream → Client đọc từ InputStream

```
CLIENT                                   SERVER
┌────────────┐                    ┌────────────────┐
│ Socket     │ ── TCP Connect ──► │ ServerSocket   │
│            │                    │ .accept() ──┐  │
│ out ──────────────────────────► │ in          │  │
│ in  ◄────────────────────────── │ out         │  │
│            │                    │    = 1 ClientHandler (1 thread)
└────────────┘                    └────────────────┘
```

#### Một "cuộc trò chuyện" trông như thế nào?

Mỗi message qua mạng là **1 dòng text JSON**, kết thúc bằng ký tự xuống dòng `\n`. 

**Client gửi (1 dòng):**
```json
{"action":"LOGIN","data":{"username":"cuong","password":"abc123"},"token":null}
```

**Server trả (1 dòng):**
```json
{"type":"RESPONSE","status":"OK","message":null,"data":{"token":"550e8400-...","user":{"id":"...","username":"cuong","role":"BIDDER"}}}
```

**Server push (không cần client hỏi):**
```json
{"type":"PUSH","event":"BID_UPDATE","data":{"auctionId":"...","bidderUsername":"khanh","amount":500000}}
```

> **Tại sao dùng JSON chứ không phải binary?** Vì JSON dễ đọc khi debug (mở terminal, thấy ngay nội dung), dễ parse (Gson), và cả Client lẫn Server đều dùng Java (cùng Gson). Cho scope dự án này, hiệu năng JSON là đủ.

#### Phân biệt Response vs Push

Mọi message Server gửi cho Client đều là JSON. Trường `type` cho phép Client phân biệt:

| `type` | Ý nghĩa | Khi nào gửi? | Client xử lý thế nào? |
|--------|---------|--------------|----------------------|
| `"RESPONSE"` | Phản hồi cho request Client vừa gửi | Ngay sau khi nhận request | Main thread chờ → nhận → xử lý |
| `"PUSH"` | Server CHỦ ĐỘNG thông báo | Khi có sự kiện (bid mới, auction kết thúc) | Background thread đọc → `Platform.runLater()` |

Cùng 1 TCP connection dùng cho CẢ 2 chiều. Client không cần mở 2 connection.

### A.3 Giải thích vai trò từng lớp trong Network Layer

#### `AuctionServer` — Người gác cổng

`AuctionServer` mở cửa (port 8888) và chờ khách (client) đến. Mỗi khi có khách mới:
1. Gọi `serverSocket.accept()` → lấy được `Socket` của khách
2. Tạo 1 `ClientHandler` mới cho khách đó
3. Bỏ `ClientHandler` vào 1 thread mới → thread đó chạy độc lập phục vụ khách

Server tiếp tục chờ khách tiếp theo (vòng lặp vô hạn). Vậy là mỗi client có 1 thread riêng phục vụ, server xử lý nhiều client song song.

```
Server (1 thread chính):
  while (true) {
    Socket client = serverSocket.accept();  // blocking — chờ client
    ClientHandler handler = new ClientHandler(client);
    new Thread(handler).start();  // mỗi client = 1 thread
  }
```

#### `ClientHandler` — Người phục vụ từng khách

Mỗi `ClientHandler` là 1 `Runnable` chạy trên 1 thread riêng. Nó phục vụ **đúng 1 client** trong suốt thời gian client kết nối.

**Công việc chính (vòng lặp trong `run()`):**
1. Đọc 1 dòng JSON từ socket (blocking — chờ client gửi)
2. Deserialize JSON → `Request` object
3. Chuyển Request cho `RequestRouter` → nhận `Response`
4. Serialize Response → JSON → gửi lại client
5. Quay lại bước 1

**Công việc phụ (Observer — tuần 4+):**
Khi client đang xem 1 phiên đấu giá và có người khác đặt giá → Server push thông báo cho client. `ClientHandler` đồng thời cũng implement `AuctionObserver` interface, nhận callback từ `LiveAuction`.

**Tại sao `sendMessage()` phải `synchronized`?**

Vì có 2 chỗ gọi `sendMessage()`:
1. Trong `run()` — gửi Response sau khi xử lý request (thread chính của ClientHandler)
2. Trong `onBidPlaced()` — push thông báo (gọi từ thread KHÁC — thread của người đặt giá)

Nếu 2 thread ghi vào socket đồng thời → dữ liệu bị xen kẽ (corrupt). `synchronized` đảm bảo chỉ 1 thread ghi tại 1 thời điểm.

#### `ActionType` — Bảng mã hành động chung

`ActionType` là enum liệt kê TẤT CẢ các hành động client có thể gửi. Tại sao cần?

1. **Thống nhất giữa Client và Server:** Cả Công (client) và Khánh (server) đều dùng cùng enum này. Client gửi `"LOGIN"`, server biết đó là `ActionType.LOGIN`.
2. **Type-safe:** Thay vì so sánh String (`if action.equals("LOGIN")` — dễ typo), dùng enum (`switch (action) case LOGIN` — compiler kiểm tra).
3. **Mở rộng dễ:** Thêm chức năng mới = thêm 1 giá trị enum. Compiler sẽ cảnh báo nếu switch chưa handle giá trị mới.

#### `Request` / `Response` — Phong bì thư

`Request` là phong bì Client gửi lên, chứa: "Tôi muốn làm gì?" (action), "Với dữ liệu nào?" (data), "Tôi là ai?" (token).

`Response` là phong bì Server gửi về, chứa: "Đây là trả lời" (type=RESPONSE) hoặc "Đây là thông báo" (type=PUSH), "Thành công hay lỗi?" (status), "Dữ liệu kết quả" (data).

#### `MessageSerializer` — Người phiên dịch

`MessageSerializer` chuyển đổi giữa Java object và JSON string:
- `serialize(Response)` → JSON string (để gửi qua socket)
- `deserialize(String json)` → Request object (để xử lý)

#### `RequestRouter` — Bảng phân luồng

Router nhận Request, nhìn `action`, rồi chuyển tới đúng Controller:
- `LOGIN` → `AuthController.handleLogin()`
- `REGISTER` → `AuthController.handleRegister()`
- `PLACE_BID` → `BidController.handlePlaceBid()` (tuần 4+)
- ...

Router KHÔNG xử lý logic. Nó chỉ "lái xe" — biết đường nào dẫn tới đâu.

#### `AuthController` — Người tiếp nhận yêu cầu Auth

Controller nhận Request từ Router, **bóc tách dữ liệu**, gọi Service (của Cường), rồi **đóng gói** kết quả thành Response.

**Controller KHÔNG chứa logic nghiệp vụ.** Nó không biết cách hash password, không biết cách tìm user trong DB. Nó chỉ biết:
1. Lấy `username`, `password` từ `request.getData()`
2. Gọi `authService.login(username, password)` → kết quả
3. Nếu thành công → `Response.ok(data)`
4. Nếu lỗi (catch exception) → `Response.error(message)`

**Tóm tắt luồng đi của 1 request:**
```
Client → Socket → ClientHandler → Router → Controller → Service → DAO → JSON file
                                                         ↓
Client ← Socket ← ClientHandler ← Response ← Controller ← Service
```

---

## PHẦN B — Tuần 2: Chi tiết từng file phải viết

### Package đích:
```
src/main/java/com/auctionuet/server/network/
├── protocol/
│   ├── ActionType.java
│   ├── Request.java
│   ├── Response.java
│   └── MessageSerializer.java
└── server/
    ├── AuctionServer.java
    └── ClientHandler.java
```

---

### B.1 File: `ActionType.java`

**Đường dẫn:** `network/protocol/ActionType.java`

**Vai trò:** Enum liệt kê tất cả hành động Client có thể gửi. Tuần 2 chỉ cần 4 giá trị, tuần 4+ sẽ mở rộng thêm.

**Khai báo:**
```
public enum ActionType
```

**Giá trị tuần 2:**

| Giá trị | Ý nghĩa | Controller xử lý |
|---------|---------|-------------------|
| `LOGIN` | Đăng nhập | `AuthController` (tuần 3) |
| `REGISTER` | Đăng ký tài khoản | `AuthController` (tuần 3) |
| `LOGOUT` | Đăng xuất | `AuthController` (tuần 3) |
| `PING` | Test kết nối | Xử lý trực tiếp trong Router |

**Mở rộng sau (tuần 4+):**
```
GET_PROFILE, UPDATE_PROFILE,
GET_AUCTIONS, GET_AUCTION_DETAIL, CREATE_AUCTION, START_AUCTION,
SUBSCRIBE, UNSUBSCRIBE,
PLACE_BID, GET_BID_HISTORY,
GET_ALL_USERS, DELETE_USER, UPDATE_ROLE
```

> **Lưu ý cho Khánh:** Enum name phải ĐÚNG chính tả với JSON Client gửi. Client gửi `"LOGIN"` → Gson deserialize thành `ActionType.LOGIN`. Nếu sai chữ → `null` → crash.

---

### B.2 File: `Request.java`

**Đường dẫn:** `network/protocol/Request.java`

**Vai trò:** Đại diện cho 1 message Client gửi lên Server. Chứa: hành động gì, dữ liệu kèm theo, và token xác thực (nếu đã login).

**Khai báo class:**
```
public class Request
```

**Trường dữ liệu (private):**

| Trường | Kiểu | Mô tả | Ví dụ |
|--------|------|-------|-------|
| `action` | `ActionType` | Hành động Client muốn thực hiện | `ActionType.LOGIN` |
| `data` | `Map<String, Object>` | Dữ liệu kèm theo (key-value) | `{"username":"cuong", "password":"abc"}` |
| `token` | `String` | Token xác thực (null nếu chưa login) | `"550e8400-e29b-..."` |

**Tại sao `data` là `Map<String, Object>` chứ không phải class riêng?**

Vì mỗi ActionType cần dữ liệu khác nhau:
- LOGIN cần: `{username, password}` → 2 trường String
- PLACE_BID cần: `{auctionId, amount}` → 1 String + 1 Number
- REGISTER cần: `{username, password, email, role}` → 4 trường

Nếu tạo class riêng cho mỗi action → quá nhiều class. Dùng Map linh hoạt hơn cho scope dự án này.

**Constructor:**
```
public Request()                      // Constructor rỗng cho Gson deserialize
public Request(ActionType action, Map<String, Object> data, String token)
```

**Methods:**

| Method | Mô tả |
|--------|-------|
| `getAction()` | Trả về ActionType |
| `getData()` | Trả về Map data |
| `getToken()` | Trả về token string |
| `setAction(ActionType)` | Setter |
| `setData(Map)` | Setter |
| `setToken(String)` | Setter |

**Helper method hữu ích (khuyến khích thêm):**
```
public String getDataString(String key)
```
Tiện ích lấy giá trị String từ Map data. Vì Gson deserialize JSON number/string đều thành Object, cần cast an toàn.

---

### B.3 File: `Response.java`

**Đường dẫn:** `network/protocol/Response.java`

**Vai trò:** Đại diện cho 1 message Server gửi cho Client. Có 2 loại: RESPONSE (trả lời request) và PUSH (thông báo chủ động).

**Khai báo class:**
```
public class Response
```

**Trường dữ liệu (private):**

| Trường | Kiểu | Dùng khi | Mô tả |
|--------|------|----------|-------|
| `type` | `String` | Luôn có | `"RESPONSE"` hoặc `"PUSH"` |
| `status` | `String` | type=RESPONSE | `"OK"` hoặc `"ERROR"` |
| `event` | `String` | type=PUSH | Tên sự kiện: `"BID_UPDATE"`, `"AUCTION_ENDED"` |
| `message` | `String` | Tùy chọn | Thông báo cho người dùng: `"Đăng ký thành công"`, `"Sai mật khẩu"` |
| `data` | `Object` | Tùy chọn | Payload dữ liệu (UserDTO, BidDTO, hoặc bất kỳ object nào) |

**Constructor:**
- **Private** — không cho code bên ngoài gọi trực tiếp. Bắt buộc dùng factory methods.

**3 Factory Methods (static):**

#### `Response.ok(Object data)`
```
public static Response ok(Object data)
```
Tạo response thành công.
- `type` = `"RESPONSE"`
- `status` = `"OK"`
- `data` = tham số truyền vào
- `message` = null
- `event` = null

**Khi nào dùng?** Khi request xử lý thành công. Ví dụ: login OK → `Response.ok(Map.of("token", token, "user", userDTO))`

#### `Response.error(String message)`
```
public static Response error(String message)
```
Tạo response lỗi.
- `type` = `"RESPONSE"`
- `status` = `"ERROR"`
- `message` = tham số truyền vào
- `data` = null
- `event` = null

**Khi nào dùng?** Khi request bị lỗi. Ví dụ: sai password → `Response.error("Sai mật khẩu")`

#### `Response.push(String event, Object data)`
```
public static Response push(String event, Object data)
```
Tạo push notification.
- `type` = `"PUSH"`
- `event` = tham số truyền vào
- `data` = tham số truyền vào
- `status` = null
- `message` = null

**Khi nào dùng?** Khi server cần thông báo cho client mà client không hỏi. Ví dụ: ai đó đặt giá mới → `Response.push("BID_UPDATE", bidDTO)`

**Tại sao dùng Factory Method thay vì constructor public?**
1. **Tên method nói rõ ý đồ:** `Response.ok()` rõ hơn `new Response("RESPONSE", "OK", null, null, data)`
2. **Ngăn lỗi:** Không thể tạo Response với `type=PUSH` nhưng `status=OK` (vô nghĩa)
3. **Đề bài yêu cầu Factory Method** — đây là nơi thể hiện design pattern.

---

### B.4 File: `MessageSerializer.java`

**Đường dẫn:** `network/protocol/MessageSerializer.java`

**Vai trò:** Lớp chuyên serialize/deserialize JSON cho protocol. Là cầu nối giữa Java object và JSON string truyền qua socket.

**Khai báo class:**
```
public class MessageSerializer
```

**Trường dữ liệu:**

| Trường | Kiểu | Mô tả |
|--------|------|-------|
| `gson` | `Gson` | Lấy từ `GsonFactory.create()` (của Cường). Nếu Cường chưa merge → tự `new Gson()` tạm |

**Methods bắt buộc:**

#### Method 1: `serialize`
```
public static String serialize(Response response)
```
Chuyển Response object → JSON string (1 dòng, không xuống dòng).

**Lưu ý quan trọng:** Phải ghi ra 1 dòng duy nhất (không có `\n` trong nội dung). Vì Client đọc bằng `BufferedReader.readLine()` — nó đọc tới `\n` mới dừng. Nếu JSON trải nhiều dòng → Client đọc thiếu.

**Cách đảm bảo:** Dùng `Gson` KHÔNG có `setPrettyPrinting()` cho network (khác với `GsonFactory.create()` dùng pretty printing cho file). Hoặc sau khi serialize, replace `\n` → `""`.

> **Giải pháp đề xuất:** Tạo 2 Gson instance — 1 cho file (pretty), 1 cho network (compact). Hoặc dùng `GsonBuilder().create()` riêng trong MessageSerializer.

#### Method 2: `deserialize`
```
public static Request deserialize(String json)
```
Chuyển JSON string → Request object.

**Logic:**
1. Dùng `gson.fromJson(json, Request.class)` → `Request`
2. Nếu `json` invalid (không phải JSON hợp lệ) → `catch JsonSyntaxException` → trả `null` hoặc throw custom exception

---

### B.5 File: `AuctionServer.java`

**Đường dẫn:** `network/server/AuctionServer.java`

**Vai trò:** Entry point của network layer. Mở cổng TCP, chờ client kết nối, tạo ClientHandler cho mỗi client.

**Khai báo class:**
```
public class AuctionServer
```

**Trường dữ liệu:**

| Trường | Kiểu | Mô tả |
|--------|------|-------|
| `port` | `int` | Port lắng nghe, mặc định `8888` |
| `serverSocket` | `ServerSocket` | Socket server chính |
| `isRunning` | `boolean` | Cờ kiểm soát vòng lặp accept |

**Constructor:**
```
public AuctionServer(int port)
```

**Methods bắt buộc:**

#### Method: `start()`
```
public void start()
```

**Logic chi tiết:**
1. Tạo `ServerSocket(port)` → lắng nghe trên port
2. In ra console: `"[SERVER] Listening on port " + port + "..."`
3. Set `isRunning = true`
4. **Vòng lặp chính:**
   ```
   while (isRunning) {
       Socket clientSocket = serverSocket.accept();  // blocking
       System.out.println("[SERVER] New client connected: " + clientSocket.getInetAddress());
       ClientHandler handler = new ClientHandler(clientSocket);
       new Thread(handler).start();
   }
   ```
5. **Catch IOException:** In lỗi, không crash server (1 client lỗi không ảnh hưởng client khác)

#### Method: `stop()`
```
public void stop()
```
1. Set `isRunning = false`
2. Đóng `serverSocket` → `accept()` sẽ throw exception → thoát vòng lặp
3. In: `"[SERVER] Stopped."`

---

### B.6 File: `ClientHandler.java`

**Đường dẫn:** `network/server/ClientHandler.java`

**Vai trò:** Xử lý 1 client trên 1 thread riêng. Đây là class PHỨC TẠP NHẤT trong tuần 2 của Khánh.

**Khai báo class:**
```
public class ClientHandler implements Runnable
```
> Tuần 4+ sẽ thêm `implements AuctionObserver`. Tuần 2 chưa cần.

**Trường dữ liệu (private):**

| Trường | Kiểu | Mô tả |
|--------|------|-------|
| `socket` | `Socket` (final) | Kết nối TCP với client |
| `out` | `PrintWriter` (final) | Luồng ghi ra socket (gửi cho client) |
| `in` | `BufferedReader` (final) | Luồng đọc từ socket (nhận từ client) |
| `currentUser` | `User` | User đang login trên socket này. `null` nếu chưa login |

**Constructor:**
```
public ClientHandler(Socket socket) throws IOException
```
1. Gán `this.socket = socket`
2. Tạo `out = new PrintWriter(socket.getOutputStream(), true)` 
   - `true` = auto-flush: mỗi lần gọi `println()` tự động flush, không buffer
3. Tạo `in = new BufferedReader(new InputStreamReader(socket.getInputStream()))`
4. Set `currentUser = null`

**Method: `run()` (override từ Runnable):**

```
@Override
public void run() {
    try {
        String line;
        while ((line = in.readLine()) != null) {
            // 1. Deserialize JSON → Request
            Request request = MessageSerializer.deserialize(line);
            
            // 2. Xử lý request
            Response response;
            if (request == null) {
                response = Response.error("Invalid JSON format");
            } else if (request.getAction() == ActionType.PING) {
                // Tuần 2: chỉ xử lý PING
                response = Response.ok("PONG");
            } else {
                // Tuần 3: sẽ thay bằng RequestRouter.route(request)
                response = Response.error("Action not implemented yet");
            }
            
            // 3. Gửi response
            sendMessage(response);
        }
    } catch (IOException e) {
        System.out.println("[SERVER] Client disconnected: " + socket.getInetAddress());
    } finally {
        cleanup();
    }
}
```

**Method: `sendMessage()` (synchronized):**
```
public synchronized void sendMessage(Response response) {
    String json = MessageSerializer.serialize(response);
    out.println(json);  // println tự thêm \n → BufferedReader.readLine() của Client đọc được
}
```

**Tại sao `synchronized`?**
Tuần 2 chưa cần (mỗi client chỉ có 1 thread ghi). Nhưng tuần 4+, khi có Observer push notification từ thread khác, 2 thread có thể ghi đồng thời → cần `synchronized` để đảm bảo thread-safe. Thêm sẵn từ tuần 2 để không quên.

**Method: `cleanup()`:**
```
private void cleanup() {
    try {
        socket.close();
    } catch (IOException e) {
        // ignore
    }
    System.out.println("[SERVER] Client handler cleaned up");
}
```

**Setter/Getter bổ sung (tuần 3 Khánh sẽ dùng):**
- `setCurrentUser(User user)` — gán user sau login thành công
- `getCurrentUser()` — lấy user hiện tại
- `getToken()` — lấy token hiện tại (nếu Khánh muốn lưu token trong ClientHandler)

---

## PHẦN C — Tuần 3: AuthController + RequestRouter

### Package đích (bổ sung):
```
src/main/java/com/auctionuet/server/network/
├── controller/
│   └── AuthController.java        ← MỚI
└── server/
    ├── AuctionServer.java         ← Đã có
    ├── ClientHandler.java         ← CẬP NHẬT
    └── RequestRouter.java         ← MỚI
```

> [!IMPORTANT]
> Tuần 3, Khánh sẽ sử dụng AuthService + SessionManager của **Cường**. Khánh cần Cường push code trước **thứ 4 tuần 3**.

---

### C.1 File: `AuthController.java`

**Đường dẫn:** `network/controller/AuthController.java`

**Vai trò:** Xử lý TẤT CẢ request liên quan đến xác thực (Login, Register, Logout). Controller là "người tiếp nhận" — nó bóc dữ liệu từ Request, gọi Service, rồi đóng gói Response.

**Khai báo class:**
```
public class AuthController
```

**Trường dữ liệu:**

| Trường | Kiểu | Nguồn |
|--------|------|-------|
| `authService` | `AuthService` | Inject qua constructor |

**Constructor:**
```
public AuthController(AuthService authService)
```

---

#### Method 1: `handleLogin`

```
public Response handleLogin(Request request)
```

**Luồng xử lý chi tiết (bước từng bước):**

| Bước | Code | Mô tả |
|------|------|-------|
| 1 | `String username = (String) request.getData().get("username")` | Lấy username từ Map data |
| 2 | `String password = (String) request.getData().get("password")` | Lấy password từ Map data |
| 3 | `LoginResult result = authService.login(username, password)` | Gọi Service (của Cường) |
| 4 | `UserDTO dto = UserMapper.toDTO(result.getUser())` | Chuyển User → DTO (không có password) |
| 5 | `Map<String, Object> responseData = Map.of("token", result.getToken(), "user", dto)` | Đóng gói data |
| 6 | `return Response.ok(responseData)` | Trả response thành công |

**Xử lý lỗi (try-catch bao bọc toàn bộ bước 1-6):**

| Exception | Response trả về |
|-----------|----------------|
| `UserNotFoundException` | `Response.error("User không tồn tại")` |
| `AuthenticationException` | `Response.error("Sai mật khẩu")` |
| `Exception` (mọi lỗi khác) | `Response.error("Lỗi server: " + e.getMessage())` |

---

#### Method 2: `handleRegister`

```
public Response handleRegister(Request request)
```

| Bước | Code | Mô tả |
|------|------|-------|
| 1 | Lấy `username`, `password`, `email`, `role` từ `request.getData()` | Bóc dữ liệu |
| 2 | Chuyển `role` String → `UserRole` enum: `UserRole.valueOf(roleStr)` | Parse enum |
| 3 | `authService.register(username, password, email, role)` | Gọi Service |
| 4 | `return Response.ok("Đăng ký thành công")` | Trả response |

**Xử lý lỗi:**

| Exception | Response |
|-----------|---------|
| `DuplicateUserException` | `Response.error("Username đã tồn tại")` |
| `IllegalArgumentException` | `Response.error("Dữ liệu không hợp lệ: " + e.getMessage())` |
| `Exception` | `Response.error("Lỗi server: " + e.getMessage())` |

---

#### Method 3: `handleLogout`

```
public Response handleLogout(Request request)
```

| Bước | Code | Mô tả |
|------|------|-------|
| 1 | `String token = request.getToken()` | Lấy token |
| 2 | `SessionManager.getInstance().validateToken(token)` | Kiểm tra token hợp lệ |
| 3 | `SessionManager.getInstance().removeSession(token)` | Xóa session |
| 4 | `return Response.ok("Đã đăng xuất")` | Trả response |

**Xử lý lỗi:**

| Exception | Response |
|-----------|---------|
| `AuthenticationException` | `Response.error("Token không hợp lệ")` |

---

### C.2 File: `RequestRouter.java`

**Đường dẫn:** `network/server/RequestRouter.java`

**Vai trò:** Nhận Request, dựa vào `action` để chuyển tới đúng Controller method. Giống tổng đài — nhận cuộc gọi, chuyển máy.

**Khai báo class:**
```
public class RequestRouter
```

**Trường dữ liệu:**

| Trường | Kiểu | Mô tả |
|--------|------|-------|
| `authController` | `AuthController` | Controller xử lý auth |

**Constructor:**
```
public RequestRouter(AuthController authController)
```
Tuần 4+ sẽ thêm: `AuctionController`, `BidController`, `UserController`, `AdminController`.

**Method chính: `route`**

```
public Response route(Request request)
```

**Logic (switch theo action):**

```java
public Response route(Request request) {
    if (request == null || request.getAction() == null) {
        return Response.error("Invalid request: missing action");
    }
    
    switch (request.getAction()) {
        case LOGIN:
            return authController.handleLogin(request);
        case REGISTER:
            return authController.handleRegister(request);
        case LOGOUT:
            return authController.handleLogout(request);
        case PING:
            return Response.ok("PONG");
        default:
            return Response.error("Unknown action: " + request.getAction());
    }
}
```

**Mở rộng tuần 4+:**
```java
case PLACE_BID:
    return bidController.handlePlaceBid(request);
case GET_AUCTIONS:
    return auctionController.handleGetAuctions(request);
// ... thêm case cho mỗi ActionType mới
```

---

### C.3 Cập nhật: `ClientHandler.java` (tuần 3)

**Thay đổi so với tuần 2:**

1. **Thêm trường:**
   ```java
   private final RequestRouter router;
   ```

2. **Cập nhật constructor:** Nhận `RequestRouter` qua tham số
   ```java
   public ClientHandler(Socket socket, RequestRouter router) throws IOException
   ```

3. **Cập nhật `run()`:**
   - Thay xử lý PING cứng → gọi `router.route(request)`
   - Sau login thành công → `this.currentUser = user`

```java
@Override
public void run() {
    try {
        String line;
        while ((line = in.readLine()) != null) {
            Request request = MessageSerializer.deserialize(line);
            Response response;
            
            if (request == null) {
                response = Response.error("Invalid JSON format");
            } else {
                response = router.route(request);  // ← Thay đổi chính!
                
                // Nếu login thành công → cập nhật currentUser
                if (request.getAction() == ActionType.LOGIN 
                    && "OK".equals(response.getStatus())) {
                    // Extract user info from response or session
                    // (chi tiết implement tùy Khánh)
                }
            }
            
            sendMessage(response);
        }
    } catch (IOException e) {
        System.out.println("[SERVER] Client disconnected");
    } finally {
        cleanup();
    }
}
```

4. **Cập nhật `AuctionServer.start()`:** Tạo `RequestRouter` rồi truyền cho mỗi `ClientHandler`

```java
// Trong AuctionServer.start():
AuthService authService = new AuthService(new UserDAO());
AuthController authController = new AuthController(authService);
RequestRouter router = new RequestRouter(authController);

while (isRunning) {
    Socket clientSocket = serverSocket.accept();
    ClientHandler handler = new ClientHandler(clientSocket, router);
    new Thread(handler).start();
}
```

---

## PHẦN D — Test bắt buộc

### D.1 Test tuần 2 (JUnit — `MessageSerializerTest.java`)

| # | Test name | Mô tả | Kết quả mong đợi |
|---|-----------|-------|-------------------|
| 1 | `testSerializeResponseOk` | `Response.ok("hello")` → JSON string | JSON chứa `"status":"OK"`, `"type":"RESPONSE"` |
| 2 | `testSerializeResponseError` | `Response.error("fail")` → JSON | JSON chứa `"status":"ERROR"`, `"message":"fail"` |
| 3 | `testDeserializeRequest` | JSON string → Request | `request.getAction() == ActionType.LOGIN` |
| 4 | `testSerializeDeserializeRoundTrip` | Request → JSON → Request | Dữ liệu giữ nguyên |
| 5 | `testDeserializeInvalidJson` | `"not json"` → deserialize | Trả `null` hoặc throw, KHÔNG crash |
| 6 | `testResponsePush` | `Response.push("BID_UPDATE", data)` → JSON | JSON chứa `"type":"PUSH"`, `"event":"BID_UPDATE"` |

### D.2 Test thủ công tuần 2

**Bước 1:** Chạy `AuctionServer` → Terminal hiện `"Listening on port 8888..."`

**Bước 2:** Mở terminal mới, dùng telnet hoặc netcat:
```bash
# Windows: dùng ncat (Nmap) hoặc viết client Java đơn giản
ncat localhost 8888
```

**Bước 3:** Gõ JSON, nhấn Enter:
```json
{"action":"PING","data":{},"token":null}
```
→ Phải nhận được:
```json
{"type":"RESPONSE","status":"OK","data":"PONG"}
```

**Bước 4:** Mở terminal thứ 3, kết nối lại → Server phải xử lý được cả 2 client đồng thời.

### D.3 Test tuần 3 (JUnit — `AuthControllerTest.java` + `RequestRouterTest.java`)

| # | Test name | Mô tả | Kết quả mong đợi |
|---|-----------|-------|-------------------|
| 1 | `testRouteLogin` | Request(LOGIN) → router → gọi đúng handleLogin | Response có token |
| 2 | `testRouteRegister` | Request(REGISTER) → router | Response.ok |
| 3 | `testRoutePing` | Request(PING) → router | `Response.ok("PONG")` |
| 4 | `testRouteUnknown` | Request(null/unknown) → router | `Response.error(...)` |
| 5 | `testLoginResponseFormat` | Login thành công → response chứa token + UserDTO | Không chứa password |

### D.4 Test thủ công end-to-end tuần 3

Chạy Server → telnet → gửi lần lượt:

```json
{"action":"REGISTER","data":{"username":"khanh","password":"abc12345","email":"khanh@uet.vn","role":"BIDDER"},"token":null}
```
→ Nhận: `{"type":"RESPONSE","status":"OK","message":"Đăng ký thành công"}`

```json
{"action":"LOGIN","data":{"username":"khanh","password":"abc12345"},"token":null}
```
→ Nhận: `{"type":"RESPONSE","status":"OK","data":{"token":"xxx-xxx","user":{"id":"...","username":"khanh","role":"BIDDER"}}}`

```json
{"action":"LOGOUT","data":{},"token":"xxx-xxx"}
```
→ Nhận: `{"type":"RESPONSE","status":"OK","message":"Đã đăng xuất"}`

---

## PHẦN E — Dependency & Nhắc nhở phối hợp

### E.1 Tuần 2: Khánh gần như KHÔNG phụ thuộc ai

Code tuần 2 của Khánh gần như độc lập. Chỉ có 1 dependency nhẹ:

| Khánh cần | Ai cung cấp | Nếu chưa có? |
|-----------|------------|--------------|
| `GsonFactory.create()` | **Cường** (tuần 2) | Khánh tự `new Gson()` tạm. Merge sau sẽ đổi |

### E.2 Tuần 3: Khánh phụ thuộc Cường (tuần 3)

| Khánh cần (tuần 3) | Cường cung cấp | Deadline |
|---------------------|---------------|----------|
| `AuthService` | `domain/service/AuthService.java` | **Thứ 4 tuần 3** |
| `SessionManager` | `domain/service/SessionManager.java` | **Thứ 4 tuần 3** |
| `LoginResult` | `domain/service/LoginResult.java` | **Thứ 4 tuần 3** |

Ngoài ra, cần code tuần 2 của **Anh**:

| Khánh cần (tuần 3) | Anh cung cấp (tuần 2) | Nếu chưa có? |
|---------------------|----------------------|--------------|
| `UserMapper.toDTO()` | `mapper/UserMapper.java` | Khánh tạo UserDTO trực tiếp |
| `UserDTO` | `network/dto/UserDTO.java` | Khánh tự tạo tạm |
| Exception classes | `exception/` | Khánh tự tạo tạm |

> [!WARNING]
> **NHẮC NHỞ CHO KHÁNH:**
> - **Thứ 3-4 tuần 3**: Nhắc Cường: *"Cường ơi, push AuthService chưa? Tôi cần để viết AuthController."*
> - Nếu Cường chậm → Khánh viết mock AuthService tạm:
>   ```java
>   // Mock: luôn trả thành công
>   public LoginResult login(String u, String p) {
>       return new LoginResult("fake-token", new Bidder("id", u));
>   }
>   ```
> - **Thứ 5-6 tuần 3**: Công sẽ hỏi Khánh: *"Server chạy được chưa? Tôi cần kết nối GUI."* 
>   → Khánh phải đảm bảo server chạy được (dù là mock) trước **thứ 5**.
> - **Khánh cũng cần check với Anh** xem UserDTO, UserMapper đã merge chưa.

### E.3 Tóm tắt chuỗi phụ thuộc tuần 3

```
Anh (tuần 2: Utils, Mapper, Exception)
     ↓
Cường (tuần 3: AuthService, SessionManager)
     ↓
Khánh (tuần 3: AuthController, Router, ClientHandler update)
     ↓
Công (tuần 3: ServerConnection, kết nối GUI ↔ Server)
     ↓
Anh (tuần 3: Integration Test — cần TẤT CẢ)
```

**Khánh nằm giữa chuỗi** → nếu Khánh chậm, Công và Anh đều bị ảnh hưởng.

---

## PHẦN F — Checklist hoàn thành

### Tuần 2 — Deadline: Tối thứ 7

- [ ] `ActionType.java` — Compile, có 4 giá trị (LOGIN, REGISTER, LOGOUT, PING)
- [ ] `Request.java` — Compile, có 3 trường + getter/setter + constructor rỗng
- [ ] `Response.java` — Compile, 3 factory methods hoạt động đúng
- [ ] `MessageSerializer.java` — Compile, serialize/deserialize JSON thành công
- [ ] `AuctionServer.java` — Compile, lắng nghe port 8888
- [ ] `ClientHandler.java` — Compile, xử lý PING, sendMessage synchronized
- [ ] `MessageSerializerTest.java` — **6 tests ALL PASS** ✅
- [ ] Test thủ công: telnet PING → nhận PONG
- [ ] Test thủ công: 2 client đồng thời → server xử lý cả 2
- [ ] Code push lên branch `feature/tuan-2-khanh-network`
- [ ] Tham gia meeting cuối tuần 2

### Tuần 3 — Deadline: Tối thứ 7

- [ ] `AuthController.java` — Compile, handleLogin + handleRegister + handleLogout
- [ ] `RequestRouter.java` — Compile, route 4 action types
- [ ] `ClientHandler.java` cập nhật — Dùng RequestRouter thay xử lý cứng
- [ ] `AuctionServer.java` cập nhật — Khởi tạo AuthService, Controller, Router
- [ ] `AuthControllerTest.java` — **5 tests ALL PASS** ✅
- [ ] Test thủ công E2E: Register → Login (nhận token) → Logout
- [ ] Code push lên branch `feature/tuan-3-khanh-controller`
- [ ] Server chạy được trước **thứ 5** để Công test GUI
- [ ] Tham gia meeting cuối tuần 3, demo live

---

> **Ghi chú cuối:** Socket programming có thể tricky. Nếu gặp lỗi "Connection refused", "Broken pipe", "Stream closed" → kiểm tra: (1) Server đã start chưa? (2) Port có bị chiếm không? (3) Socket đã đóng chưa? Đọc stack trace cẩn thận, hỏi nhóm nếu mắc kẹt > 1 tiếng.
