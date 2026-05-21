# Planning tuần 6: Hoàn thiện logic Auto-Bid

## Thông tin nhánh

- Nhánh: `khanh-hoan-thien-auto-bid`
- Điểm bắt đầu so với `main`: commit `51e14eb`
- Thời điểm chuyển sang nhánh theo reflog: `2026-05-20 19:22:06 +0700`
- Trạng thái worktree khi lập tài liệu: sạch

## Các commit đã thực hiện

1. `9ec15b8` - Implement proxy autobid pricing
2. `1afffa8` - Add autobid status reporting and UI feedback
3. `17314c8` - Notify when auto-bid loses effectiveness

## Phạm vi thay đổi

Các thay đổi tập trung vào logic auto-bid từ server domain, protocol contract, service response, UI client và test regression.

Các file chính đã thay đổi:

- `auction-server/src/main/java/com/auctionuet/server/domain/model/LiveAuction.java`
- `auction-server/src/main/java/com/auctionuet/server/domain/model/AutoBidConfig.java`
- `auction-server/src/main/java/com/auctionuet/server/domain/service/BidService.java`
- `auction-protocol/src/main/java/com/auctionuet/protocol/dto/response/bid/AutoBidConfigDTO.java`
- `auction-protocol/src/main/java/com/auctionuet/protocol/enums/AutoBidStatus.java`
- `auction-client/src/main/java/com/auctionuet/client/view/BiddingController.java`
- `auction-server/src/test/java/com/auctionuet/server/JUnitTest/DomainModelTest.java`
- `auction-server/src/test/java/com/auctionuet/server/domain/service/AuctionServiceTest.java`

## Mục tiêu logic

Trước thay đổi, auto-bid hoạt động gần giống cơ chế phản hồi từng bước: sau mỗi bid, hệ thống tìm auto-bid đủ điều kiện rồi tăng thêm một increment. Cách này dễ tạo nhiều vòng xử lý và chưa thể hiện rõ logic proxy bidding.

Sau thay đổi, auto-bid được chuyển sang hướng proxy bidding:

- Người có `maxBid` cao nhất được ưu tiên thắng.
- Nếu nhiều người có cùng `maxBid`, người bật auto-bid sớm hơn được ưu tiên.
- Giá thắng không nhất thiết bằng toàn bộ `maxBid` của người thắng.
- Giá thắng được đẩy tới mức đủ vượt đối thủ mạnh nhất, theo công thức gần đúng: `maxBid` của đối thủ mạnh nhất cộng `increment` của người thắng.
- Giá thắng không vượt quá `maxBid` của người thắng.

## Luồng xử lý Auto-Bid

### 1. Người dùng bật Auto-Bid

Client gọi network adapter để gửi request bật auto-bid. Server đi vào `BidService.setAutoBid(...)`.

Service xử lý:

1. Lấy `LiveAuction` đang chạy.
2. Validate `maxBid > currentPrice` và `increment > 0`.
3. Tính tiền cọc dựa trên giá khởi điểm item.
4. Đảm bảo bidder đã được giữ tiền cọc.
5. Tạo `AutoBidConfig`.
6. Gọi `liveAuction.addAutoBid(...)`.
7. Đồng bộ lại trạng thái auction xuống persistence.

Nếu thêm auto-bid thất bại, service rollback phần tiền cọc vừa giữ nếu cần.

### 2. LiveAuction thêm hoặc cập nhật Auto-Bid

`LiveAuction.addAutoBid(...)` làm việc dưới `bidLock`.

Các bước chính:

1. Xóa config cũ của cùng bidder khỏi queue.
2. Thêm config mới vào queue.
3. Lưu config vào map `autoBids` để tra cứu nhanh theo bidder.
4. Gọi `resolveAutoBids(...)` để cập nhật giá và người thắng nếu config mới đủ mạnh.

Map `autoBids` vẫn giữ được config inactive để client còn kiểm tra trạng thái `INEFFECTIVE`.

### 3. Chọn Auto-Bid mạnh nhất

`AutoBidConfig.comparePriority(...)` định nghĩa thứ tự ưu tiên:

1. `maxBid` cao hơn đứng trước.
2. Nếu `maxBid` bằng nhau, `registeredAt` sớm hơn đứng trước.

`LiveAuction.findBestAutoBidConfig(...)` chỉ xét các config đang active.

### 4. Tính giá proxy

`LiveAuction.calculateProxyAutoBidAmount(...)` xử lý các trường hợp:

- Nếu người thắng proxy hiện cũng là `currentWinnerId`:
  - Không có đối thủ active khác: giữ nguyên giá hiện tại.
  - Có đối thủ active khác: tăng tới `min(winner.maxBid, competitor.maxBid + winner.increment)`.

- Nếu người thắng proxy chưa phải current winner:
  - Có đối thủ active khác: lấy giá cần vượt là max giữa giá hiện tại và `maxBid` của đối thủ mạnh nhất, rồi cộng increment của người thắng.
  - Không có đối thủ active khác: tăng từ giá hiện tại thêm increment nếu chưa vượt `maxBid`.

Sau khi tính được giá mới:

1. Nếu giá mới không lớn hơn giá hiện tại thì không tạo bid mới.
2. Nếu giá mới hợp lệ, tạo `BidRecord` loại `BidType.AUTO`.
3. Cập nhật `currentHighestBid`.
4. Cập nhật `currentWinnerId`.
5. Thêm record vào `bidHistory`.
6. Gọi callback để lưu bid auto xuống DB.
7. Áp dụng anti-sniping nếu bid xảy ra gần cuối phiên.
8. Vô hiệu hóa các auto-bid đã bị vượt trần.
9. Notify observers để client nhận push realtime.

### 5. Vô hiệu hóa Auto-Bid bị vượt trần

`LiveAuction.deactivateExhaustedAutoBids(...)` tìm các config:

- Đang active.
- Không thuộc về người đang thắng.
- Có `maxBid <= currentPrice`.

Các config này sẽ bị:

- `markInactive()`
- Xóa khỏi queue active
- Vẫn còn trong map `autoBids`

Việc giữ lại trong map là quan trọng vì client có thể gọi `CHECK_AUTO_BID` và biết auto-bid của mình đã thành `INEFFECTIVE`.

## Trạng thái Auto-Bid trong protocol

Enum mới `AutoBidStatus` gồm:

- `PROTECTING`: auto-bid còn hiệu lực và người dùng đang dẫn đầu.
- `WAITING`: auto-bid còn hiệu lực nhưng người dùng chưa dẫn đầu.
- `INEFFECTIVE`: auto-bid đã bị vượt trần, không còn khả năng bảo vệ.

`AutoBidConfigDTO` được mở rộng thêm:

- `status`
- `protectedUntil`

Ý nghĩa `protectedUntil`:

- Với `PROTECTING`: mức trần mà auto-bid đang bảo vệ tới.
- Với `WAITING`: mức trần còn đang chờ kích hoạt.
- Với `INEFFECTIVE`: trả `0.0`.

## Luồng hiển thị ở client

`BiddingController` được cập nhật để gọi `checkAutoBidState()` sau khi:

- Mở màn hình bidding.
- Nhận push bid mới.
- Bật auto-bid thành công.

Khi nhận trạng thái:

- `PROTECTING`: disable nút bật auto-bid, enable nút hủy, hiển thị thông báo màu xanh.
- `WAITING`: disable nút bật auto-bid, enable nút hủy, hiển thị thông báo màu vàng/cam.
- `INEFFECTIVE`: enable nút bật lại auto-bid, vẫn cho phép hủy config cũ, hiển thị thông báo màu đỏ.

Client còn có biến `pendingAutoBidLossNotice`. Khi nhận push `BidType.AUTO` từ người khác, biến này được bật. Nếu sau đó `CHECK_AUTO_BID` trả về `INEFFECTIVE`, lịch sử bid sẽ thêm thông báo:

`Auto-Bid của bạn không còn hiệu lực vì đã bị Auto-Bid khác vượt trần.`

## Các case đã được test

### DomainModelTest

Đã thêm test cho các case:

- Hai auto-bid có cùng `maxBid` thì người bật sớm hơn thắng.
- Auto-bid có `maxBid` cao hơn được ưu tiên.
- Giá cuối dùng `increment` của người thắng.
- Auto-bid có trần thấp nhưng vẫn chưa bị vượt giá hiện tại thì còn `active`.
- Auto-bid inactive có thể được bật lại bằng config mới.

### AuctionServiceTest

Đã thêm hoặc cập nhật test cho các case:

- Sau khi set auto-bid thành công, trạng thái trả về là `PROTECTING`.
- Auto-bid chưa dẫn đầu nhưng còn hiệu lực trả `WAITING`.
- Auto-bid bị vượt trần trả `INEFFECTIVE` và `protectedUntil = 0.0`.
- Không có auto-bid hoặc đã cancel thì trả `null`.

## Kết quả kiểm tra

Đã chạy:

```powershell
mvn -pl auction-server -am "-Dtest=DomainModelTest,AuctionServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Kết quả:

- `AuctionServiceTest`: 24 tests pass
- `DomainModelTest`: 9 tests pass
- Tổng cộng: 33 tests pass, 0 failures, 0 errors

## Nhận xét kỹ thuật

Thiết kế hiện tại bám đúng kiến trúc contract-first:

- Protocol thay đổi ở `auction-protocol`.
- Business logic nằm trong `LiveAuction` và `BidService`.
- JavaFX controller chỉ gọi API và render trạng thái.
- Không tạo DTO riêng ở client/server.
- Test được bổ sung ở cả domain model và service.

Một điểm cần theo dõi là trạng thái `WAITING`: hiện tại chỉ cần `maxBid > currentPrice` và `increment > 0` là có thể bật auto-bid. Nếu `currentPrice + increment > maxBid`, config vẫn có thể ở trạng thái chờ nhưng chưa chắc đặt được bid kế tiếp. Có thể xử lý theo một trong hai hướng:

1. Siết validate thành `maxBid >= currentPrice + increment`.
2. Giữ logic hiện tại nhưng chỉnh text UI để không hứa chắc hệ thống sẽ đặt giá khi cần.

Một điểm khác là `checkAutoBidState()` ở client đang bỏ qua exception. Nếu request fail đúng lúc auto-bid mất hiệu lực, thông báo UI có thể bị trễ hoặc bị mất. Có thể cải thiện bằng retry nhẹ hoặc hiển thị lỗi không chặn luồng bidding.

## Đề xuất bước tiếp theo

1. Chạy full test của server trước khi merge.
2. Kiểm tra thủ công luồng client với 2 bidder cùng tham gia một phiên.
3. Xác nhận wording UI cho `WAITING` và `INEFFECTIVE`.
4. Nếu cần độ tin cậy cao hơn, thêm push event riêng cho auto-bid bị mất hiệu lực thay vì client suy luận từ bid push rồi gọi lại `CHECK_AUTO_BID`.
