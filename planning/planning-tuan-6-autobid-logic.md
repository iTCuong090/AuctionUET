# Planning tuần 6: Hoàn thiện logic Auto-Bid

## 1. Thông tin nhánh

- Nhánh: `khanh-hoan-thien-auto-bid`
- Nhánh gốc dùng để so sánh: `main`
- Commit gốc: `51e14eb` - `Merge pull request #42 from iTCuong090/develop/week6-cuong`
- Tài liệu này mô tả trạng thái code thực tế đang giữ lại `WAITING`; không mô tả phương án xóa `WAITING`.
- Phạm vi chính: hoàn thiện auto-bid theo hướng proxy bidding, bổ sung trạng thái auto-bid cho client, và sửa tie-break khi nhiều người cùng trần tiền.

## 2. Các commit logic được tài liệu hóa

1. `9ec15b8` - `Implement proxy autobid pricing`
2. `1afffa8` - `Add autobid status reporting and UI feedback`
3. `17314c8` - `Notify when auto-bid loses effectiveness`
4. `2b84d4f` - `Them file md`
5. `c52e827` - `Fix auto-bid tie-break at equal max bids`

Commit `2b84d4f` chủ yếu thêm tài liệu planning ban đầu. Các thay đổi logic nằm ở bốn commit còn lại.

Các thử nghiệm sau đó về việc xóa `WAITING` không phải trạng thái code đang được mô tả ở tài liệu này. Code hiện tại vẫn dùng `WAITING` trong protocol, server DTO mapping, client UI và test.

## 3. File thay đổi theo layer

### Protocol

- `auction-protocol/src/main/java/com/auctionuet/protocol/enums/AutoBidStatus.java`
- `auction-protocol/src/main/java/com/auctionuet/protocol/dto/response/bid/AutoBidConfigDTO.java`

### Server domain/service

- `auction-server/src/main/java/com/auctionuet/server/domain/model/AutoBidConfig.java`
- `auction-server/src/main/java/com/auctionuet/server/domain/model/LiveAuction.java`
- `auction-server/src/main/java/com/auctionuet/server/domain/service/BidService.java`

### Client UI

- `auction-client/src/main/java/com/auctionuet/client/view/BiddingController.java`

### Test

- `auction-server/src/test/java/com/auctionuet/server/JUnitTest/DomainModelTest.java`
- `auction-server/src/test/java/com/auctionuet/server/domain/service/AuctionServiceTest.java`

## 4. Những gì thực sự đã thay đổi trên code

Thay đổi code hiện tại tập trung vào các điểm sau:

- Protocol thêm `AutoBidStatus` với 3 trạng thái `PROTECTING`, `WAITING`, `INEFFECTIVE`.
- `AutoBidConfigDTO` có thêm `status` và `protectedUntil` để server trả trạng thái auto-bid cho client.
- `AutoBidConfig.comparePriority(...)` định nghĩa thứ tự ưu tiên: `maxBid` cao hơn thắng, cùng `maxBid` thì người đăng ký sớm hơn thắng, cuối cùng dùng `registrationOrder` để tie-break ổn định.
- `LiveAuction` xử lý auto-bid theo mô hình proxy bidding, giữ quyền ưu tiên của auto-bid đặt trước khi người khác bid thủ công đúng bằng trần.
- `BidService` chịu trách nhiệm validate tham số auto-bid, giữ cọc, rollback khi lỗi, đồng bộ auction state xuống JSON và map trạng thái auto-bid sang DTO.
- `BiddingController` render 3 trạng thái auto-bid và hiển thị thông báo khi auto-bid của người dùng mất hiệu lực.
- Test server bổ sung các case proxy bidding, equal max tie-break, manual bid đúng bằng trần auto-bid và trạng thái `WAITING`/`PROTECTING`/`INEFFECTIVE`.

## 5. Mục tiêu nghiệp vụ

Trước nhánh này, auto-bid chưa thể hiện rõ mô hình proxy bidding. Sau mỗi bid, hệ thống tăng giá theo từng bước, nhưng chưa xử lý đầy đủ các tình huống cạnh tranh giữa nhiều auto-bid, đặc biệt là khi hai bidder có cùng `maxBid`.

Mục tiêu sau thay đổi:

- Auto-bid có `maxBid` cao hơn được ưu tiên thắng.
- Nếu `maxBid` bằng nhau, bidder bật auto-bid sớm hơn được ưu tiên.
- Giá thắng chỉ tăng tới mức cần thiết để vượt đối thủ, không tự động nhảy thẳng lên toàn bộ `maxBid` nếu chưa cần.
- Khi auto-bid bị vượt trần, server vẫn giữ config inactive để client có thể hiển thị trạng thái `INEFFECTIVE`.
- Client biết auto-bid đang bảo vệ, đang chờ, hoặc đã mất hiệu lực.
- Luồng đặt giá thủ công không được phá quyền ưu tiên của auto-bid đã đặt trước ở cùng trần tiền.

## 6. Thay đổi trong protocol

### AutoBidStatus

Thêm enum `AutoBidStatus` với 3 trạng thái:

- `PROTECTING`: auto-bid còn hiệu lực và bidder đang dẫn đầu.
- `WAITING`: auto-bid còn hiệu lực nhưng bidder chưa dẫn đầu.
- `INEFFECTIVE`: auto-bid đã bị vượt trần hoặc không còn khả năng bảo vệ.

### AutoBidConfigDTO

`AutoBidConfigDTO` được mở rộng thêm:

- `status`: trạng thái hiện tại của auto-bid.
- `protectedUntil`: mức trần mà auto-bid đang bảo vệ hoặc đang chờ tới.

Quy ước hiện tại:

- `PROTECTING`: `protectedUntil = maxBid`.
- `WAITING`: `protectedUntil = maxBid`.
- `INEFFECTIVE`: `protectedUntil = 0.0`.

DTO vẫn implement `ValidatableDTO` và validate các trường chính: `auctionId`, `bidder`, `maxBid`, `increment`, `status`, `protectedUntil`.

## 7. Thay đổi trong server domain

### AutoBidConfig

`AutoBidConfig` hiện là nơi định nghĩa thứ tự ưu tiên giữa các auto-bid.

Thứ tự so sánh trong `comparePriority(...)`:

1. `maxBid` cao hơn được ưu tiên.
2. Nếu `maxBid` bằng nhau, `registeredAt` sớm hơn được ưu tiên.
3. Nếu thời gian đăng ký trùng nhau, `registrationOrder` nhỏ hơn được ưu tiên.

`registrationOrder` dùng `AtomicLong` để tie-break ổn định hơn trong trường hợp hai config được tạo quá sát nhau và `LocalDateTime.now()` không đủ khác biệt.

### LiveAuction và startingPrice

`LiveAuction` có thêm `startingPrice` để `getCurrentPrice()` trả đúng giá khởi điểm khi chưa có bid thật:

```java
return currentHighestBid > 0 ? currentHighestBid : startingPrice;
```

Constructor đầy đủ nhận thêm `startingPrice`. Constructor cũ vẫn còn để giữ tương thích với các test/call site cũ, và tự gọi sang constructor đầy đủ bằng `this(...)`.

### Luồng thêm auto-bid

`LiveAuction.addAutoBid(...)` chạy dưới `bidLock`:

1. Xóa config cũ của cùng bidder khỏi `autoBidQueue`.
2. Thêm config mới vào `autoBidQueue`.
3. Lưu config vào `autoBids` để tra cứu theo bidder.
4. Gọi `resolveAutoBids(...)`.

`autoBidQueue` chỉ dùng cho các config active đang cạnh tranh. `autoBids` giữ cả config inactive để `CHECK_AUTO_BID` vẫn có thể trả trạng thái cho client.

### Luồng resolve auto-bid

`LiveAuction.resolveAutoBids(...)` xử lý theo mô hình proxy:

1. Vô hiệu hóa các auto-bid có `maxBid < currentPrice`.
2. Tìm config tốt nhất bằng `findBestAutoBidConfig()`.
3. Tính giá auto-bid mới bằng `calculateProxyAutoBidAmount(...)`.
4. Kiểm tra giá mới có được áp dụng bằng `canApplyAutoBid(...)`.
5. Nếu hợp lệ, tạo `BidRecord` loại `BidType.AUTO`.
6. Cập nhật `currentHighestBid` và `currentWinnerId`.
7. Lưu bid auto qua callback từ `BidService`.
8. Áp dụng anti-sniping nếu cần.
9. Vô hiệu hóa các auto-bid đã hết hiệu lực.
10. Notify observer để client nhận push realtime.

Điểm quan trọng là bước đầu chỉ loại `maxBid < currentPrice`, chưa loại `maxBid == currentPrice`. Điều này cho phép auto-bid đặt trước vẫn có cơ hội giữ quyền ưu tiên khi người khác bid thủ công đúng bằng trần của nó.

### Tính giá proxy

`calculateProxyAutoBidAmount(...)` xử lý các nhóm tình huống:

- Nếu config tốt nhất đã là `currentWinnerId`:
  - Không có đối thủ active khác: giữ nguyên giá hiện tại.
  - Có đối thủ active khác: giá mới là `min(winner.maxBid, competitor.maxBid + winner.increment)`.

- Nếu config tốt nhất chưa phải `currentWinnerId`:
  - Có đối thủ active khác: lấy mức cần vượt là `max(currentPrice, competitor.maxBid)`, rồi cộng `winner.increment`, nhưng không vượt `winner.maxBid`.
  - Không có đối thủ active khác: tăng từ giá hiện tại thêm `increment` nếu còn trong trần; nếu increment bị vượt trần nhưng đã có current winner, dùng đúng `winner.maxBid`.

Cách này giúp auto-bid có thể dùng đúng trần cuối cùng để bảo vệ bidder, thay vì bỏ cuộc chỉ vì `currentPrice + increment > maxBid`.

### Tie-break khi bằng trần tiền

Lỗi đã sửa ở commit `c52e827`:

- Người A bật auto-bid với `maxBid = 1000`.
- Người B đặt giá thủ công `1000`.
- Trước khi sửa, hệ thống có thể để B thắng vì auto-bid của A bị xem là exhausted khi `maxBid <= currentPrice`.
- Sau khi sửa, A vẫn thắng vì A đã đăng ký auto-bid trước và cùng trần tiền.

Các hàm liên quan:

- `deactivateAutoBidsBelowCurrentPrice()`: chỉ loại config có `maxBid < currentPrice` trước khi resolve.
- `canApplyAutoBid(...)`: cho phép auto-bid tạo bid ở cùng giá hiện tại nếu đang xử lý tie-break hợp lệ.
- `canClaimCurrentPriceTie(...)`: kiểm tra auto-bid có được quyền giữ mức giá hòa hiện tại hay không.
- `findCurrentWinningBidRecord()`: tìm bid hiện đang giữ winner ở mức giá hiện tại để so thời điểm với auto-bid.

Sau khi resolve xong, `deactivateExhaustedAutoBids()` mới loại các config không thắng có `maxBid <= currentPrice`.

## 8. Thay đổi trong BidService

`BidService.setAutoBid(...)` vẫn chịu trách nhiệm:

1. Lấy `LiveAuction`.
2. Validate `maxBid > currentPrice` và `increment > 0`.
3. Tính và giữ tiền cọc.
4. Tạo `AutoBidConfig`.
5. Gọi `liveAuction.addAutoBid(...)`.
6. Đồng bộ auction state xuống JSON.
7. Rollback tiền cọc nếu thêm auto-bid thất bại.

`BidService.getAutoBidConfigDTO(...)` được cập nhật để trả trạng thái:

- Config null: trả `null`.
- Config inactive: trả `INEFFECTIVE`, `protectedUntil = 0.0`.
- Config active và bidder là current winner: trả `PROTECTING`.
- Config active nhưng bidder chưa dẫn đầu: trả `WAITING`.

Business logic vẫn nằm trong service/domain, controller chỉ parse DTO, check auth/permission và gọi service.

### Khi nào `WAITING` xảy ra

`WAITING` xảy ra khi server kiểm tra trạng thái auto-bid của một bidder và thấy config vẫn còn hiệu lực nhưng bidder đó chưa phải người dẫn đầu hiện tại.

Điều kiện cụ thể trong `BidService.getAutoBidConfigDTO(...)`:

1. `AuctionManager` vẫn có `LiveAuction` cho auction đó.
2. Bidder có `AutoBidConfig` trong `liveAuction.getAutoBidConfig(bidder.getId())`.
3. `config.isActive() == true`.
4. `bidder.getId()` khác `liveAuction.getCurrentWinnerId()`.

Ví dụ đang được test trong `AuctionServiceTest`: current price là `500`, bidder set auto-bid `maxBid = 520`, `increment = 50`. Config được lưu và tiền cọc được giữ, nhưng chưa có auto bid record vì bước bid hợp lệ tiếp theo cần lên `550`, vượt quá `maxBid = 520`. Bidder chưa dẫn đầu nên `CHECK_AUTO_BID` trả `WAITING` với `protectedUntil = 520`.

Trạng thái này không có nghĩa auto-bid bị hỏng. Nó chỉ nói rằng config còn active nhưng hiện chưa bảo vệ vị trí dẫn đầu cho bidder.

## 9. Thay đổi trong client

`BiddingController` được cập nhật để client phản ánh trạng thái auto-bid rõ hơn.

### Khi nào client gọi CHECK_AUTO_BID

Client gọi `checkAutoBidState()` khi:

- Mở màn hình bidding.
- Bật auto-bid thành công.
- Nhận push `BID_UPDATE`.

Các request vẫn chạy trong background thread, UI update bằng `Platform.runLater`.

### Hiển thị trạng thái

Khi server trả:

- `PROTECTING`:
  - Disable nút bật auto-bid.
  - Enable nút hủy.
  - Hiển thị thông báo bidder đang dẫn đầu và auto-bid bảo vệ tới `protectedUntil`.

- `WAITING`:
  - Disable nút bật auto-bid.
  - Enable nút hủy.
  - Hiển thị thông báo auto-bid đang chờ và sẽ tự đặt giá khi cần.

- `INEFFECTIVE`:
  - Enable nút bật lại auto-bid.
  - Vẫn cho phép hủy config cũ.
  - Hiển thị thông báo auto-bid không còn hiệu lực.

### Thông báo khi mất hiệu lực

Client có thêm `pendingAutoBidLossNotice`.

Khi nhận push `BidType.AUTO` từ người khác, client đánh dấu cần kiểm tra lại auto-bid của mình. Nếu lần `CHECK_AUTO_BID` sau đó trả `INEFFECTIVE`, lịch sử bid sẽ thêm thông báo:

```text
Auto-Bid của bạn không còn hiệu lực vì đã bị Auto-Bid khác vượt trần.
```

## 10. Các case đã được test

### DomainModelTest

Các case chính:

- Manual bid hợp lệ cập nhật winner và current highest bid.
- Bid thấp hơn hoặc bằng giá hiện tại bị reject.
- Seller không được tự bid.
- Auction đã đóng không cho bid.
- Hai auto-bid cùng `maxBid` thì người bật trước thắng.
- Manual bid đúng bằng `maxBid` của auto-bid trước đó không cướp winner.
- Manual bid thấp hơn `maxBid` nhưng `increment` bị vượt trần vẫn cho auto-bid dùng đúng trần để bảo vệ.
- Auto-bid có `maxBid` cao hơn được ưu tiên.
- Giá proxy dùng `increment` của người thắng.
- Auto-bid đang chờ vẫn active.
- Auto-bid inactive có thể được bật lại bằng config mới.

### AuctionServiceTest

Các case chính:

- Set auto-bid thành công thì giữ tiền cọc và tạo bid auto nếu đủ điều kiện.
- Không đủ tiền cọc thì không tạo config hoặc bid.
- Manual bid không hợp lệ rollback tiền cọc.
- Auto-bid thấp hơn nhưng chưa đủ để đặt bid vẫn lưu tiền cọc và ở trạng thái `WAITING`.
- Auto-bid bị vượt trần trả `INEFFECTIVE`.
- Auto-bid đang dẫn đầu trả `PROTECTING`.
- Hai bidder set auto-bid cùng trần thì bidder set trước vẫn là winner.
- Manual bid đúng bằng trần auto-bid không cướp winner của bidder set trước.
- Cancel hoặc chưa có auto-bid thì `CHECK_AUTO_BID` trả `null`.

## 11. Kết quả kiểm tra

Đã chạy nhóm test trực tiếp cho auto-bid:

```powershell
mvn "-pl" "auction-server" "-am" "-Dtest=DomainModelTest,AuctionServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Kết quả:

- `AuctionServiceTest`: 26 tests pass.
- `DomainModelTest`: 11 tests pass.
- Tổng: 37 tests pass, 0 failures, 0 errors.

Đã chạy full test server:

```powershell
mvn "-pl" "auction-server" "-am" test
```

Kết quả:

- Tổng: 92 tests pass, 0 failures, 0 errors.

Maven vẫn có warning sẵn có về thiếu version của `maven-compiler-plugin` và khuyến nghị dùng `--release 21`, nhưng không làm fail test.

## 12. Nhận xét kỹ thuật

Thiết kế hiện tại bám theo kiến trúc contract-first:

- Trạng thái auto-bid dùng enum/DTO trong `auction-protocol`.
- Server controller không chứa business logic.
- Logic cạnh tranh auto-bid nằm trong `LiveAuction`.
- Logic tiền cọc, persistence và mapping DTO nằm trong `BidService`.
- Client không tạo DTO riêng, chỉ dùng DTO từ protocol.
- Network call trên client vẫn chạy background thread.

Một số điểm cần lưu ý sau nhánh này:

- Config auto-bid hiện là runtime state trong `LiveAuction`; bid auto được lưu vào `bids.json`, nhưng chưa có persistence riêng cho config auto-bid sau restart.
- Constructor cũ của `LiveAuction` vẫn public để giữ tương thích, nhưng code mới nên ưu tiên constructor có `startingPrice` để tránh sai `getCurrentPrice()` khi chưa có bid.
- `checkAutoBidState()` ở client đang bỏ qua exception. Nếu request check trạng thái fail đúng lúc auto-bid mất hiệu lực, thông báo UI có thể bị trễ.
- Thông báo mất hiệu lực hiện được client suy luận từ push bid auto của người khác rồi gọi lại `CHECK_AUTO_BID`; nếu muốn chắc hơn, có thể bổ sung push event riêng cho auto-bid bị mất hiệu lực.

## 13. Đề xuất sau khi merge

1. Kiểm thử thủ công với 2 bidder bật auto-bid cùng `maxBid`.
2. Kiểm thử thủ công case bidder thứ hai bid thủ công đúng bằng trần auto-bid của bidder thứ nhất.
3. Cân nhắc persistence riêng cho auto-bid config nếu cần khôi phục sau server restart.
4. Cân nhắc thêm push event riêng cho trạng thái auto-bid để client không phải suy luận từ bid history.
