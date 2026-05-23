# Báo cáo refactor nhánh Công tuần 6

> Tài liệu này tổng hợp các thay đổi trên nhánh `week6-nguyen-cao-cong`
> so với `main` tại thời điểm rà soát ngày 23/05/2026.
>
> Mục tiêu: ghi lại các phần đã sửa, đã thêm mới, và chỉ ra khác biệt giữa
> phiên bản cũ với phiên bản hiện tại để cả nhóm dễ review, demo và tiếp tục
> phát triển.

## 1. Tóm tắt nhanh

Nhánh tuần 6 tập trung vào ba nhóm việc chính:

- Hoàn thiện luồng winner phải thanh toán sau khi thắng auction.
- Thêm màn quản lý vật phẩm của chính user, bao gồm cả Bidder sở hữu vật phẩm sau khi thanh toán thành công.
- Cải thiện nhiều màn JavaFX: danh sách đấu giá, trang chủ, chi tiết phiên, thanh toán, ví tiền, hồ sơ cá nhân và lịch sử vật phẩm.

Điểm khác biệt lớn nhất so với bản cũ là hệ thống không còn coi đấu giá chỉ kết thúc ở trạng thái thắng/thua. Sau khi có winner, auction đi tiếp vào trạng thái chờ thanh toán, giữ cọc winner, cho thanh toán trong 5 phút, rồi mới quyết định chuyển vật phẩm sang Bidder hoặc hủy phiên và tịch thu cọc.

Số liệu diff đã commit so với `main`:

| Chỉ số | Kết quả |
|---|---:|
| Base branch | `main` |
| Base commit | `38eac64` |
| Current branch | `week6-nguyen-cao-cong` |
| Current commit | `d1ee55f` |
| Số commit trên nhánh | 19 |
| Số file thay đổi | 54 |
| Số dòng thêm | 4.207 |
| Số dòng xóa | 244 |
| File thêm mới đáng chú ý | 11 |

Ngoài phần đã commit, working tree còn có một số chỉnh sửa mới ở client/server và một vài file JSON demo do quá trình chạy app. Báo cáo này tập trung vào thay đổi chức năng và không coi dữ liệu demo trong `data/*.json` là migration bắt buộc.

## 2. So sánh trước và sau

| Hạng mục | Phiên bản cũ | Phiên bản hiện tại |
|---|---|---|
| Winner sau khi hết phiên | Auction thường kết thúc mà chưa có luồng thanh toán rõ. | Winner vào trạng thái `WAITING_PAYMENT`, có hạn thanh toán 5 phút. |
| Tiền cọc | Có cọc/frozen balance nhưng chưa gắn đầy đủ với màn thanh toán tuần 6. | Cọc winner được giữ; khi thanh toán chỉ trừ thêm phần tiền tươi cần trả; quá hạn thì tịch thu cọc. |
| Quyền sở hữu vật phẩm | Vật phẩm gần như gắn với Seller. | Bidder có thể đứng tên vật phẩm sau khi thanh toán thành công. |
| Màn vật phẩm | Chưa có màn quản lý vật phẩm riêng đúng nghĩa cho cả Seller/Bidder. | Có `MyItemsView`: Seller thêm/gỡ/tạo phiên từ vật phẩm; Bidder xem vật phẩm đã sở hữu và lịch sử. |
| Màn thanh toán | Chưa có danh sách phiên thắng cần thanh toán/đã thanh toán. | Có `PaymentView` với chip `Tất cả`, `Chưa thanh toán`, `Đã thanh toán (30 ngày)`. |
| Checkout thanh toán | Thanh toán nằm trong detail hoặc chưa có UI riêng. | Có popup `PaymentCheckoutView` riêng, chọn QR hoặc tiền mặt, xác nhận thanh toán xong thì tự đóng và refresh danh sách. |
| Danh sách đấu giá | Filter/search/pagination còn lỗi, card layout dễ thừa khoảng trống. | Search/filter hoạt động, bỏ pagination thừa, card tự căn 4 thẻ mỗi hàng. |
| Trang chủ | Nút khám phá và nút chi tiết chưa đồng bộ routing. | Nút khám phá mở danh sách đấu giá; card home dùng cùng logic với danh sách đấu giá. |
| Chi tiết đấu giá | Thông tin item còn raw, trạng thái là text thường, nút start bị cắt chữ. | Item type thành badge, brand/warranty tách riêng, có `Chi tiết:`, trạng thái thành badge màu, Seller start auction ngay tại detail. |
| Lịch sử vật phẩm | Chưa có màn riêng rõ ràng. | Có `ItemHistoryView`, hiển thị item và lịch sử các auction gắn với item. |
| Ví tiền | Số dư dài có thể làm vỡ layout. | Có `CurrencyFormatter` compact số tiền dài như `1.23T VND`, hạn chế tràn UI. |
| Hồ sơ cá nhân | Thống kê hoạt động chưa cập nhật đúng theo role. | Seller xem sản phẩm đã bán; Bidder xem phiên đã tham gia; điểm uy tín hiển thị bằng sao. |

## 3. Luồng thanh toán sau khi thắng auction

### 3.1 Logic nghiệp vụ hiện tại

Luồng hiện tại:

```mermaid
flowchart TD
    A["Auction kết thúc"] --> B{"Có winner?"}
    B -->|Không| C["Auction CANCELED\nhoàn cọc các bidder"]
    B -->|Có| D["Auction WAITING_PAYMENT\nset paymentDeadlineAt = now + 5 phút"]
    D --> E{"Winner thanh toán đúng hạn?"}
    E -->|Có| F["Trừ phần còn lại sau cọc\nseller nhận đủ giá thắng"]
    F --> G["Chuyển item sang owner mới là Bidder"]
    G --> H["Auction PAID"]
    E -->|Không| I["Tịch thu cọc winner"]
    I --> J["Auction CANCELED\nitem vẫn thuộc Seller"]
```

Điểm quan trọng trong logic tiền:

- Tổng giá winner phải trả vẫn là giá thắng cuối cùng.
- Tiền cọc đã được giữ trước đó nên khi bấm thanh toán, ví chỉ bị trừ thêm phần còn lại.
- UI ghi rõ tổng tiền phải trả đã bao gồm cọc.
- Nếu tiền tươi không đủ để trả phần còn lại, hệ thống báo không đủ tiền và giữ auction ở trạng thái chờ thanh toán cho đến khi hết hạn.
- Nếu đã quá hạn nhưng client vẫn bấm thanh toán, server kiểm tra lại deadline, expire phiên và từ chối thanh toán.

### 3.2 Server thay đổi

Các thay đổi chính nằm ở:

- `auction-server/src/main/java/com/auctionuet/server/domain/service/AuctionService.java`
- `auction-server/src/main/java/com/auctionuet/server/domain/manager/AuctionManager.java`
- `auction-server/src/main/java/com/auctionuet/server/domain/service/ItemService.java`
- `auction-server/src/main/java/com/auctionuet/server/persistence/schema/AuctionSchema.java`
- `auction-server/src/main/java/com/auctionuet/server/persistence/schema/ItemSchema.java`

Các điểm đã thêm/sửa:

- `AuctionManager.PAYMENT_DEADLINE_MINUTES = 5` để dễ test, sau này có thể đổi thành 24 giờ.
- `AuctionSchema` có `paymentDeadlineAt` và `paidAt`.
- `AuctionDTO` trả thêm `paymentDeadlineAt` để client hiển thị hạn thanh toán.
- `AuctionService.endAuction(...)` chuyển phiên có winner sang `WAITING_PAYMENT`.
- `AuctionService.payAuction(...)` xử lý thanh toán, chuyển owner item sang winner và log giao dịch.
- `AuctionService.expirePaymentDeadline(...)` xử lý quá hạn: tịch thu cọc, hủy phiên, giữ item ở Seller.
- `ItemService.transferOwnership(...)` đổi owner item sang Bidder sau khi thanh toán thành công.
- `AuctionService.payAuction(...)` có check deadline ngay tại server để tránh client cũ hoặc UI stale thanh toán sau hạn.

### 3.3 Protocol và permission

Các action/permission liên quan:

- `PAY_AUCTION`
- `GET_MY_PENDING_PAYMENTS`
- `GET_MY_ITEMS`
- `DELETE_ITEM`
- `GET_ITEM_AUCTION_HISTORY`

Các DTO/protocol liên quan:

- `auction-protocol/src/main/java/com/auctionuet/protocol/ActionType.java`
- `auction-protocol/src/main/java/com/auctionuet/protocol/enums/Permission.java`
- `auction-protocol/src/main/java/com/auctionuet/protocol/dto/request/item/ItemIdRequestDTO.java`
- `auction-protocol/src/main/java/com/auctionuet/protocol/dto/response/auction/AuctionDTO.java`

Permission đã được mở cho đúng role:

- Bidder có thể xem vật phẩm của mình, xem lịch sử item, thanh toán auction.
- Seller có thể xem/gỡ item của mình, tạo phiên từ item.
- Controller vẫn chỉ làm auth/permission mỏng; rule chính nằm trong service.

## 4. Màn Thanh toán đấu giá

### 4.1 Danh sách thanh toán

Thêm màn:

- `auction-client/src/main/java/com/auctionuet/client/view/PaymentController.java`
- `auction-client/src/main/resources/fxml/PaymentView.fxml`

Màn này có ba chip lọc:

- `Tất cả`: hiển thị phiên chưa thanh toán, phiên đã thanh toán trong 30 ngày và các phiên thắng nhưng đã quá hạn trong 30 ngày.
- `Chưa thanh toán`: chỉ hiển thị phiên `WAITING_PAYMENT` mà user hiện tại là winner.
- `Đã thanh toán (30 ngày)`: hiển thị phiên đã thanh toán gần đây.

Card thanh toán đã được chỉnh để lấp layout giống danh sách đấu giá:

- mặc định 4 thẻ mỗi hàng;
- không để thừa khoảng trống lớn bên phải;
- có scroll dọc;
- nút card tùy trạng thái: thanh toán, xem chi tiết, hoặc trạng thái quá hạn/đã thanh toán.

### 4.2 Popup Trả tiền

Thêm màn popup riêng:

- `auction-client/src/main/java/com/auctionuet/client/view/PaymentCheckoutController.java`
- `auction-client/src/main/resources/fxml/PaymentCheckoutView.fxml`

Thiết kế hiện tại:

- Popup có tiêu đề `Trả tiền`.
- Bên trái là thẻ chọn phương thức thanh toán.
- Bên trái phía dưới là thẻ thông tin thanh toán: vật phẩm, người bán, mã phiên, tổng tiền phải trả, cọc đã giữ, cần thanh toán thêm.
- Bên phải là nội dung theo phương thức đang chọn.
- Với QR: hiển thị mã QR, số tiền cần trả thêm, mã phiên, nút `Xác nhận đã thanh toán`, nút `Quay về`.
- Với tiền mặt: hiển thị nội dung giao dịch trực tiếp và vẫn dùng nút xác nhận tạm thời.
- Sau khi xác nhận thanh toán thành công, popup tự đóng và `PaymentView` refresh lại danh sách.

Đây là UI tạm cho coursework/demo. Xác nhận thanh toán hiện vẫn là nút trong app, chưa tích hợp cổng thanh toán thật.

## 5. Màn Vật phẩm và lịch sử vật phẩm

### 5.1 MyItemsView

Thêm màn:

- `auction-client/src/main/java/com/auctionuet/client/view/MyItemsController.java`
- `auction-client/src/main/resources/fxml/MyItemsView.fxml`

Thay đổi chính:

- Sidebar đổi `Hoạt động` thành `Vật phẩm`.
- Seller có thể thêm vật phẩm mới ngay trong màn này.
- Sau khi đăng sản phẩm thành công, form đăng sản phẩm tự ẩn và danh sách item reload lại.
- Seller có thể gỡ mềm item nếu item không có auction đang `OPEN`, `RUNNING`, hoặc `WAITING_PAYMENT`.
- Bidder có thể xem các vật phẩm đã sở hữu sau khi thắng và thanh toán thành công.
- Thẻ item hiển thị tiêu chí theo chiều dọc để không bị cắt chữ.
- `brand` và `warrantyMonths` được tách thành tiêu chí riêng thay vì hiện raw map.
- Trạng thái auction của item hiện bằng badge màu giống card danh sách đấu giá.
- Nếu item chưa gắn auction thì hiển thị `NOT_LISTED`.
- Có ô menu, thanh tìm kiếm và nút tìm giống màn danh sách đấu giá.

### 5.2 Lịch sử vật phẩm

Thêm màn:

- `auction-client/src/main/java/com/auctionuet/client/view/ItemHistoryController.java`
- `auction-client/src/main/resources/fxml/ItemHistoryView.fxml`

Mục tiêu:

- Bấm `Lịch sử` từ card item sẽ mở màn lịch sử của đúng item.
- Màn lịch sử giống chi tiết phiên đấu giá nhưng đổi tiêu đề từ `Đấu giá` sang `Vật phẩm`.
- Bỏ ô thông tin giá hiện tại/người dẫn đầu/trạng thái phiên vì màn này tập trung vào item.
- Bảng lịch sử lấy auction theo `itemId`, không theo tên title, để tránh nhầm khi title cũ bị stale.

Lỗi đã phát hiện trong quá trình làm:

- Có dữ liệu demo cũ tạo auction title `Đấu giá: vin` nhưng `itemId` lại trỏ sang item `sexy lady`.
- Root cause là khi chọn item trong màn tạo phiên, title auto không luôn được cập nhật theo item mới.
- Đã sửa `CreateAuctionController` để title tự đổi khi item đổi, miễn là user chưa tự nhập title thủ công.
- Dữ liệu JSON cũ vẫn cần dọn riêng nếu muốn demo sạch tuyệt đối.

## 6. Danh sách đấu giá và trang chủ

### 6.1 Danh sách đấu giá

Các file chính:

- `auction-client/src/main/java/com/auctionuet/client/view/AuctionListController.java`
- `auction-client/src/main/java/com/auctionuet/client/view/AuctionViewFilter.java`
- `auction-client/src/main/resources/fxml/AuctionListView.fxml`

Thay đổi:

- Ô menu lọc auction hoạt động.
- Thanh tìm kiếm hoạt động theo tên sản phẩm/phiên.
- Bỏ các nút phân trang `1`, `2`, `3` vì màn đã có scroll.
- Sửa lỗi theme tối làm layout bị nhảy và thừa khoảng trống cạnh thanh cuộn.
- Bỏ thanh cuộn ngang thừa.
- Card được thu lại để mặc định 4 card mỗi hàng.
- Seller bấm `Xem chi tiết` sẽ vào `AuctionDetailView`.
- Bidder không dùng một nút `Xem chi tiết` duy nhất nữa:
  - nút chính là `Đăng ký đấu giá`, mở `BiddingView` khi phiên đang chạy;
  - dòng bên dưới là `Xem chi tiết thông tin`, mở `AuctionDetailView`.

### 6.2 Trang chủ

Các file chính:

- `auction-client/src/main/java/com/auctionuet/client/view/HomeController.java`
- `auction-client/src/main/resources/fxml/HomeView.fxml`

Thay đổi:

- Nút `Khám phá ngay` chuyển sang `Danh sách đấu giá`.
- Card đấu giá trên trang chủ dùng logic routing giống card ở danh sách đấu giá.
- Bidder thấy `Đăng ký đấu giá` và `Xem chi tiết thông tin`.
- Seller vẫn thấy nút `Xem chi tiết` như cũ.

## 7. Chi tiết phiên đấu giá

Các file chính:

- `auction-client/src/main/java/com/auctionuet/client/view/AuctionDetailController.java`
- `auction-client/src/main/resources/fxml/AuctionDetailView.fxml`
- `auction-client/src/main/resources/css/styles.css`
- `auction-client/src/main/resources/css/light-theme.css`
- `auction-client/src/main/resources/css/dark-theme.css`

Thay đổi:

- Nút `Bắt đầu phiên đấu giá` được tăng kích thước để không bị cắt chữ.
- Khung chứa nút bắt đầu chỉ hiện với Seller và chỉ khi auction đang `OPEN`.
- Seller bấm bắt đầu phiên sẽ gọi server start auction, reload detail và chuyển trạng thái sang `RUNNING`.
- Push `AUCTION_STARTED` không còn ép Seller nhảy sang màn bidding; Bidder vẫn có thể đi vào bidding.
- Trạng thái trong ô thông tin được bọc bằng badge màu như danh sách đấu giá.
- Giá hiện tại, người dẫn đầu, thời gian kết thúc được thêm class CSS riêng để dễ tạo màu theo theme.
- Type item chuyển thành badge đặt cạnh tên item.
- `brand` và `warrantyMonths` tách thành tiêu chí riêng.
- Các extra field khác cũng được render từng dòng thay vì hiển thị raw map.
- Phần mô tả có nhãn `Chi tiết:` và nội dung mô tả nằm bên dưới.
- Bid chart load lịch sử bid và cập nhật qua push `BID_UPDATE`.

Gợi ý màu đang áp dụng:

| Tiêu chí | Light theme | Dark theme |
|---|---|---|
| Giá hiện tại | Xanh dương | Xanh cyan |
| Người dẫn đầu | Vàng/cam | Vàng |
| Kết thúc | Cam | Cam sáng |
| Trạng thái | Badge theo status | Badge theo status |
| Loại item | Badge xanh nhạt | Badge xanh đậm |

## 8. Ví tiền, giao dịch và số tiền dài

Thêm helper:

- `auction-client/src/main/java/com/auctionuet/client/view/CurrencyFormatter.java`

Mục tiêu:

- Số tiền bình thường vẫn hiển thị đầy đủ.
- Số tiền quá dài được rút gọn để không phá layout, ví dụ dạng `1.23T VND`.
- Header `TK:` và màn ví dùng formatter này để tránh tràn màn hình.

Màn ví/giao dịch cũng được chỉnh:

- Tổng tiền đang cọc hiển thị rõ hơn.
- Lịch sử giao dịch hiển thị thông tin dễ đọc hơn.
- Phần auction trong lịch sử giao dịch nên ưu tiên tên phiên thay vì mã UUID để user hiểu được giao dịch gắn với phiên nào.

## 9. Hồ sơ cá nhân và thống kê hoạt động

File chính:

- `auction-client/src/main/java/com/auctionuet/client/view/ProfileController.java`
- `auction-client/src/main/resources/fxml/ProfileView.fxml`

Thay đổi:

- Thống kê không còn hard-code toàn 0.
- `Đấu giá đã thắng` tính theo auction user là winner.
- Với Seller, ô thứ hai là `Sản phẩm đã bán`.
- Với Bidder, ô thứ hai là `Phiên đã tham gia`.
- `Đang tham gia` tính theo các phiên user còn liên quan.
- Điểm uy tín hiển thị bằng sao `★`.

Lưu ý kỹ thuật:

- Cơ chế điểm uy tín hiện tại là tính ở client theo heuristic dựa trên số hoạt động thành công.
- Chưa có bảng/schema reputation riêng ở server.
- Nếu muốn chặt hơn, nên đưa reputation về server, lưu điểm theo user và cập nhật khi thanh toán đúng hạn/quá hạn.

## 10. Chống tạo nhiều auction active cho một item

Vấn đề đã phát hiện:

- Một vật phẩm có thể xuất hiện ở hai phiên đấu giá cùng lúc, ví dụ một phiên `RUNNING` và một phiên `CANCELED`.
- Điều này dễ làm user hiểu sai vì cùng một item không nên có nhiều phiên active.

Hướng đã làm trong nhánh:

- Khi tạo phiên mới, service kiểm tra item có auction active chưa.
- Active được hiểu là các trạng thái còn ảnh hưởng quyền sở hữu hoặc thanh toán: `OPEN`, `RUNNING`, `WAITING_PAYMENT`.
- Không chặn lịch sử cũ đã `CANCELED` hoặc `PAID`, vì cần giữ lịch sử item.
- Có xử lý reconcile/cancel duplicate active để dữ liệu demo không tiếp tục sinh lỗi.

Kết quả mong muốn:

- Tại một thời điểm chỉ có một auction active cho một item.
- Item vẫn có thể có nhiều auction trong lịch sử, nhưng không được có nhiều phiên đang mở/chạy/chờ thanh toán cùng lúc.

## 11. File thêm mới đáng chú ý

| File | Vai trò |
|---|---|
| `auction-client/src/main/java/com/auctionuet/client/view/AuctionViewFilter.java` | Lọc/tìm kiếm auction dùng lại cho list/home/payment. |
| `auction-client/src/main/java/com/auctionuet/client/view/CurrencyFormatter.java` | Format tiền compact để tránh vỡ layout. |
| `auction-client/src/main/java/com/auctionuet/client/view/MyItemsController.java` | Controller màn vật phẩm của tôi. |
| `auction-client/src/main/java/com/auctionuet/client/view/ItemHistoryController.java` | Controller màn lịch sử vật phẩm. |
| `auction-client/src/main/java/com/auctionuet/client/view/PaymentController.java` | Controller màn thanh toán đấu giá. |
| `auction-client/src/main/java/com/auctionuet/client/view/PaymentCheckoutController.java` | Controller popup trả tiền. |
| `auction-client/src/main/resources/fxml/MyItemsView.fxml` | FXML màn vật phẩm. |
| `auction-client/src/main/resources/fxml/ItemHistoryView.fxml` | FXML màn lịch sử vật phẩm. |
| `auction-client/src/main/resources/fxml/PaymentView.fxml` | FXML màn thanh toán đấu giá. |
| `auction-client/src/main/resources/fxml/PaymentCheckoutView.fxml` | FXML popup trả tiền. |
| `auction-protocol/src/main/java/com/auctionuet/protocol/dto/request/item/ItemIdRequestDTO.java` | Request DTO dùng cho item id actions. |

## 12. Test và kiểm chứng

Test server được mở rộng ở:

- `auction-server/src/test/java/com/auctionuet/server/domain/service/AuctionServiceTest.java`

Các nhóm test đã thêm/cập nhật:

- Không cho tạo nhiều auction active cho cùng một item.
- End auction có winner chuyển sang `WAITING_PAYMENT`.
- Payment deadline quá hạn thì winner không được thanh toán nữa và mất cọc.
- Thanh toán thành công chuyển auction sang `PAID`.
- Chuyển ownership item sang winner sau khi thanh toán thành công.
- Giữ item thuộc Seller nếu auction bị hủy do quá hạn.

Kết quả đã chạy trong quá trình làm:

| Lệnh | Kết quả |
|---|---|
| `mvn test` | Pass toàn bộ tại thời điểm kiểm tra sau phần deadline/payment service. |
| `mvn -pl auction-client -am test` | Pass sau các chỉnh sửa UI/controller gần đây. |

Vì báo cáo này chỉ thêm tài liệu, không bắt buộc chạy lại test. Trước khi merge/nộp vẫn nên chạy lại `mvn test` một lần sau khi chốt toàn bộ working tree.

## 13. Rủi ro và việc nên dọn tiếp

Các điểm cần nói thật với nhóm:

- Một số file `data/*.json` đang là dữ liệu demo sinh ra khi chạy app, không nên coi là migration sạch.
- Có dữ liệu cũ bị stale title, ví dụ auction title không khớp item name; code đã sửa root cause cho dữ liệu mới nhưng dữ liệu cũ cần dọn riêng nếu demo.
- Popup `Trả tiền` hiện xác nhận thanh toán bằng nút tạm trong app, chưa tích hợp ngân hàng/cổng thanh toán thật.
- Điểm uy tín hiện tính ở client, chưa phải business rule server-side.
- Transaction history nên tiếp tục chuẩn hóa để mọi dòng liên quan auction đều ưu tiên tên phiên thay vì UUID.
- Cần review lại tiếng Việt có dấu ở toàn bộ UI để tránh còn string cũ không dấu.
- Trước khi chấm/demo nên chạy lại full `mvn test` và kiểm tra manual các flow: Seller tạo item, tạo auction, start auction; Bidder bid, thắng, thanh toán đủ/thiếu/quá hạn.

## 14. Kết luận

So với bản cũ, nhánh tuần 6 đã đưa AuctionUET tiến gần hơn tới một sản phẩm đấu giá hoàn chỉnh:

- Auction có hậu xử lý sau khi có winner.
- Tiền cọc, hạn thanh toán, thanh toán thành công và quá hạn được nối thành một flow rõ ràng.
- Bidder có thể sở hữu vật phẩm.
- Seller và Bidder đều có màn quản lý vật phẩm riêng.
- Người dùng có màn thanh toán, popup trả tiền, lịch sử vật phẩm, thống kê hoạt động và giao diện danh sách ổn định hơn.
- Client/server contract vẫn đi qua `auction-protocol`, không tạo DTO riêng bên client/server.

Phần đáng chú ý nhất là luồng thanh toán và ownership item: đây là thay đổi nghiệp vụ lớn, chạm cả protocol, service, persistence, client adapter và JavaFX UI. Phần cần dọn tiếp là dữ liệu demo cũ, reputation server-side và kiểm thử lại full flow sau khi chốt UI.
