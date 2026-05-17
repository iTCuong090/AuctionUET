# AGENTS.md - AuctionUET Coding Guide

File này là điểm nạp context bắt buộc cho agent hoặc người vibe code trong repo AuctionUET.
Trước khi sửa code, hãy đọc file này trước, sau đó mới đọc các file liên quan trực tiếp.

## Mục Tiêu

AuctionUET là hệ thống đấu giá trực tuyến viết bằng Java 21, Maven multi-module, socket client-server và JavaFX.
Kiến trúc hiện tại đi theo hướng contract-first: client và server dùng chung network contract trong module `auction-protocol`.

Khi thêm tính năng hoặc sửa lỗi, mục tiêu là:

- Giữ contract client/server đồng bộ qua `auction-protocol`.
- Không đưa business logic vào JavaFX controller hoặc network controller.
- Không tạo lại DTO/protocol riêng ở client/server.
- Không làm lệch luồng xác thực, phân quyền, persistence JSON, realtime push.
- Đọc code hiện có trước khi thay đổi; ưu tiên pattern đang có trong repo.

## Luôn Đọc Trước Khi Sửa

1. `AGENTS.md` ở root repo.
2. Các file code trực tiếp liên quan tới task và test tương ứng.

Trước khi edit, chạy `git status --short` để biết worktree có thay đổi sẵn hay không. Không revert hoặc overwrite thay đổi không phải của mình.

## Module Chính

Root Maven project có 3 module:

- `auction-protocol`: contract chung giữa client và server.
- `auction-server`: socket server, domain logic, JSON persistence.
- `auction-client`: JavaFX app, FXML controllers, socket client adapters.

Parent `pom.xml` quản lý Java 21, JUnit 5, Gson, SLF4J, Logback.

## Quy Ước Chung

- Dùng Java 21.
- Tuân thủ style gần Google Java Style Guide.
- Comment tiếng Việt được chấp nhận, nhưng chỉ comment khi giúp hiểu logic.
- Không thêm dependency mới nếu chưa thật sự cần.
- Không log hoặc expose password, `hashedPassword`, `passwordSalt`, token đầy đủ, hoặc dữ liệu nhạy cảm.
- Không đặt logic nghiệp vụ trong controller mạng hoặc JavaFX controller.
- Không parse `Map<String, Object>` thủ công nếu đã có request DTO.
- Không tạo duplicate DTO/protocol ở `auction-client` hoặc `auction-server`.
- Không dùng `System.out.println`/`printStackTrace` cho server code mới; dùng `AppLogger` và `GlobalExceptionHandler`.
- Với client, không block JavaFX Application Thread bằng network call; chạy background thread rồi cập nhật UI bằng `Platform.runLater`.

## Contract-First Protocol

Nguồn sự thật duy nhất cho giao thức mạng nằm trong `auction-protocol/src/main/java/com/auctionuet/protocol`.

Các file quan trọng:

- `ActionType.java`: danh sách action client gửi lên server.
- `Request.java`: `{ action, data, token }`, có `fromDto(...)` và `getDataAs(...)`.
- `Response.java`: `{ type, status, event, message, data }`, có `getDataAs(...)` và `getDataListAs(...)`.
- `MessageSerializer.java`: serialize/deserialize request/response.
- `PushMessage.java`, `PushActionType.java`, `dto/push/PushEvents.java`: realtime push.
- `dto/request/**`: DTO cho request.
- `dto/response/**`: DTO cho response.
- `enums/**`: enum dùng chung như `UserRole`, `Permission`, `ItemType`, `AuctionStatus`.
- `util/NetworkGson.java`: Gson cho network, có adapter `LocalDateTime`.

Request DTO quan trọng nên implement `ValidatableDTO`. `Request.fromDto(...)` validate trước khi gửi, `Request.getDataAs(...)` validate lại sau khi server deserialize.

Không tạo các package legacy sau:

- `auction-client/src/main/java/com/auctionuet/client/network/protocol`
- `auction-client/src/main/java/com/auctionuet/client/model/*DTO.java`
- `auction-server/src/main/java/com/auctionuet/server/network/dto`
- `auction-server/src/main/java/com/auctionuet/server/mapper` cho DTO mapping cũ

`auction-client/src/main/java/com/auctionuet/client/model/ClientSession.java` vẫn là model hợp lệ vì giữ token/current user phía client.

## Server Architecture

Server entrypoint:

- `auction-server/src/main/java/com/auctionuet/server/ServerApp.java`
- `auction-server/src/main/java/com/auctionuet/server/network/server/AuctionServer.java`

Luồng request server hiện tại:

1. `AuctionServer` khởi tạo DAO, service, controller, `RequestRouter`.
2. Mỗi socket client được xử lý bởi `ClientHandler`.
3. `ClientHandler` đọc raw JSON, deserialize bằng `MessageSerializer`, gọi `RequestRouter`.
4. `RequestRouter` route theo `ActionType`.
5. Network controller parse DTO, validate token/quyền, gọi service.
6. Service xử lý nghiệp vụ, đọc/ghi DAO/schema nếu cần, trả protocol response DTO.
7. `GlobalExceptionHandler` chuyển exception thành `Response.error(...)`.

Layer server:

- `network/server`: socket lifecycle, request routing, exception boundary.
- `network/controller`: nhận request, auth/permission mỏng, parse DTO, gọi service, trả `Response`.
- `domain/service`: business logic chính và mapping sang response DTO hiện tại.
- `domain/model`: runtime model trong RAM như `User`, `LiveAuction`, `BidRecord`.
- `domain/manager`: singleton managers như `DataManager`, `SessionManager`, `AuctionManager`.
- `persistence/schema`: object lưu xuống JSON.
- `persistence/dao`: đọc/ghi JSON qua `JsonFileHelper`.
- `exception`: exception nghiệp vụ.
- `util`: validation, password, id, logging, JSON helpers.

## Controller Và Service

Network controller chỉ nên làm những việc này:

- `request.getDataAs(SomeRequestDTO.class)`.
- `SessionManager.getInstance().validateToken(request.getToken())` nếu action cần đăng nhập.
- Check `user.hasPermission(Permission.X)` nếu action cần quyền cụ thể.
- Gọi service.
- Trả `Response.ok(...)` hoặc để exception đi tới `GlobalExceptionHandler`.

Service chịu trách nhiệm:

- Business rules.
- Ownership/status checks.
- Ghi dữ liệu quan trọng qua DAO ngay khi thay đổi.
- Tạo hoặc cập nhật schema/domain objects.
- Mapping schema/domain sang response DTO dùng chung trong `auction-protocol`.
- Các service hoạt động nhịp nhàng với nhau, trước khi tiến hành công việc thì suy nghĩ và đọc xem các service khác đã hỗ trợ nghiệp vụ đó chưa trước khi tự viết lại nghiệp vụ riêng. 

Không đưa logic nghiệp vụ như tính tiền cọc, lifecycle auction, auto-bid, quyền sở hữu item vào controller.

## Auth Và Permission

User runtime nằm ở:

- `domain/model/User.java`
- `Bidder.java`
- `Seller.java`
- `Admin.java`

Mỗi role override `hasPermission(Permission action)`.

Token/session nằm ở `domain/manager/SessionManager.java` và dùng `ConcurrentHashMap`.

Nguyên tắc:

- Action cần login phải validate token.
- Action cần quyền phải check `Permission`.
- Password/hash/salt chỉ thuộc `UserSchema`, không nằm trong runtime `User` hoặc response DTO.
- Không cho đăng ký `ADMIN` qua flow register thường.

## Persistence Và Schema

Persistence dùng JSON files:

- Root data seed: `data/users.json`, `data/items.json`, `data/auctions.json`, `data/bids.json`.
- Server runtime cũng có `auction-server/data/**` trong repo hiện tại.
- Test có thể dùng file `data/test_*.json`.

DAO pattern:

- DAO implement `GenericDAO<T extends BaseSchema>`.
- DAO mặc định dùng file trong `data/*.json`.
- DAO có constructor nhận `filePath` để phục vụ test.
- DAO hiện đọc toàn bộ list, sửa trong RAM, rồi ghi lại toàn bộ file; đây là lựa chọn chấp nhận được cho project học phần.

Schema rules:

- `BaseSchema` constructor rỗng không có side effect để Gson deserialize an toàn.
- Khi tạo entity mới, truyền id/time rõ ràng bằng `IdGenerator.generate()` và `LocalDateTime.now()`.
- `update(...)` nên cập nhật `updatedAt`.
- Dữ liệu quan trọng như bid, wallet, auction status phải write-through xuống JSON ngay.

Item hiện đã refactor sang schema phẳng:

- `ItemSchema` có `ItemType type`, `ItemCondition condition`, `Map<String, Object> extraFields`.
- `ItemType.normalizeAndValidateExtraFields(...)` là nơi validate/normalize field đặc thù từng loại.
- Không tái tạo cây class `ElectronicsSchema`, `ArtSchema`, `VehicleSchema` nếu không có yêu cầu rất rõ.

## Auction, Bid, Wallet, Realtime

Các lớp trọng tâm:

- `AuctionService`: tạo/start/end/pay auction, load running auctions, status/payment flow.
- `BidService`: place bid, bid history, auto-bid, freeze deposit.
- `WalletService`: balance/frozenBalance, deposit/withdraw/freeze/unfreeze/forfeit.
- `AuctionManager`: quản lý `LiveAuction` đang chạy trong RAM, scheduler auto-end.
- `LiveAuction`: lock bidding, auto-bid resolution, anti-sniping, observer notify.
- `ClientHandler`: implements `AuctionObserver`, gửi `PushMessage` về client.

Concurrency rules:

- Không bỏ lock/synchronized hiện có nếu chưa hiểu bidding flow.
- `LiveAuction` dùng lock và collection thread-safe cho observer/deposit state.
- `BidService.placeBid(...)` và `setAutoBid(...)` đang synchronized để bảo vệ flow bid/deposit.
- `WalletService` có các method synchronized cho balance mutations.

Realtime push:

- Server gửi `PushMessage` với `type = "PUSH"`.
- Client `ServerConnection` listener phân biệt `"PUSH"` và response thường.
- Push DTO nằm trong `auction-protocol/dto/push/PushEvents.java`.
- Khi thêm push event, cập nhật `PushActionType`, `PushEvents`, server sender, client listener.

## Client Architecture

Client JavaFX nằm trong:

- `auction-client/src/main/java/com/auctionuet/client/ClientApp.java`
- `auction-client/src/main/java/com/auctionuet/client/view/**`
- `auction-client/src/main/java/com/auctionuet/client/network/**`
- `auction-client/src/main/resources/fxml/**`
- `auction-client/src/main/resources/css/**`

Các singleton đáng chú ý:

- `SceneManager`: chuyển scene/root FXML.
- `ThemeManager`: apply light/dark CSS.
- `ClientSession`: giữ token và `UserDTO` hiện tại.
- `ServerConnection`: socket, send request, listener thread cho push.

JavaFX rules:

- Mỗi FXML phải khai báo đúng `fx:controller`.
- `fx:id` trong FXML phải khớp field `@FXML` trong controller.
- Network call chạy trong background thread.
- UI update chạy trong `Platform.runLater`.
- Khi thêm view mới, cập nhật FXML, controller, navigation trong `SceneManager`/controller liên quan, CSS nếu cần.
- Không tạo DTO riêng trong client; import từ `com.auctionuet.protocol.dto...`.

Client network adapter pattern:

- `AuthClient`, `ItemClient`, `AuctionClient`, `BidClient`, `WalletClient` build request DTO, gọi `ServerConnection.sendRequest(...)`, unwrap `Response`.
- Khi response `status != "OK"`, ném exception với `response.getMessage()`.
- Nếu refactor, ưu tiên tạo helper generic để giảm lặp nhưng giữ public methods dễ dùng cho JavaFX controllers.

## Thêm Action Mới

Checklist tối thiểu khi thêm action request/response:

1. Đọc action tương tự đang có.
2. Thêm hoặc chỉnh `ActionType`.
3. Thêm request DTO trong `auction-protocol/dto/request/<domain>` nếu action có data.
4. Request DTO nên implement `ValidatableDTO`.
5. Thêm response DTO trong `auction-protocol/dto/response/<domain>` nếu response không thể dùng DTO hiện có.
6. Thêm enum/shared type vào `auction-protocol/enums` nếu cả client/server cùng dùng.
7. Server: thêm method controller mỏng ở `network/controller`.
8. Server: thêm hoặc chỉnh service/domain/DAO/schema đúng layer.
9. Server: route action trong `RequestRouter` hiện tại.
10. Server: đảm bảo đã check token và `Permission` nếu cần.
11. Client: thêm method vào network adapter tương ứng.
12. Client: cập nhật JavaFX controller/FXML nếu feature có UI.
13. Tests: thêm unit/integration test cho rule chính và ít nhất một error case.
14. Chạy test phù hợp.

Lưu ý hiện tại `ActionType` đã có một số action chưa được route/triển khai đầy đủ như `GET_PROFILE`, `UPDATE_PROFILE`, `UPDATE_ITEM`, `DELETE_ITEM`, `GET_ALL_USERS`, `DELETE_USER`, `UPDATE_ROLE`. Nếu task đụng các action này, kiểm tra route/controller/service/client/test thay vì chỉ thấy enum rồi cho rằng feature đã hoàn tất.

## Sửa Bug

Khi sửa bug:

1. Tái hiện bug bằng cách bám sát mô tả bug của người dùng. Trong trường hợp mô tả mơ hồ, tự nghĩ ra cách để hỗ trợ người dùng debug và mô tả thêm dựa vào những gì hiện có. Không tự viết test, sửa code, thêm dòng log, vv.
2. Đọc file ở layer gây bug và layer liền kề phía trên/dưới.
3. Sửa nguyên nhân gốc, không chỉ sửa triệu chứng ở UI.
4. Nếu bug thuộc contract, kiểm tra cả `auction-protocol`, client adapter, server controller.
5. Nếu bug thuộc persistence, kiểm tra schema, DAO, data file và test dùng custom file path.
6. Nếu bug thuộc bidding/realtime, kiểm tra cả `LiveAuction`, `BidService`, `AuctionManager`, `ClientHandler`, `ServerConnection`.
7. Thêm hoặc cập nhật test regression nếu hợp lý.

## Logging Và Error Handling

Server:

- Dùng `AppLogger` cho lifecycle/request/service/exception logging.
- `GlobalExceptionHandler` là boundary chuyển exception thành response.
- Business exception log WARN và trả message rõ.
- Unexpected exception log ERROR và trả `Response.error(...)`.
- Mask sensitive fields trước khi log.

Client:

- Ưu tiên hiển thị lỗi trong label/dialog phù hợp với view.
- Không để exception từ background thread làm chết UI.
- Với cleanup như unsubscribe, có thể ignore failure nếu navigation không nên bị chặn, nhưng chỉ làm vậy khi có lý do rõ.

## Definition Of Done

Một thay đổi được coi là xong khi:

- Code compile hoặc test phù hợp đã chạy, hoặc có lý do rõ nếu không chạy được.
- Contract client/server không lệch.
- Auth/permission được xử lý đúng.
- DTO/protocol không bị duplicate ngoài `auction-protocol`.
- Business logic nằm ở service/domain, không nằm ở UI/controller mạng.
- Persistence quan trọng được ghi xuống JSON đúng thời điểm.
- UI không block JavaFX thread.
- Sensitive data không bị trả về response hoặc log.
- Test mới/cũ phản ánh behavior quan trọng.
- Tài liệu hoặc comment được cập nhật nếu thay đổi làm lệch cách dùng hệ thống.
