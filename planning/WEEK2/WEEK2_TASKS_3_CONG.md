# 📋 TUẦN 2 & 3 - PART 3: NHIỆM VỤ CỦA CÔNG (Auction Model & MVC Services)

## 1. MỤC TIÊU VÀ ĐỊNH HƯỚNG KIẾN TRÚC
Nhiệm vụ của bạn là kết nối các thực thể rời rạc thành nghiệp vụ Logic thật (áp dụng Model-View-Controller).
- **Auction / BidTransaction (Model):** Đại diện cho một phiên chợ đấu giá đang chạy.
- **Service Layer (`UserService`, `AuctionService`):** Nơi chứa chất "kinh doanh" (nghiệp vụ). DAO chỉ biết "lưu/xóa", còn Service sẽ kiểm tra "User có bị trùng tên không?", "Phiên đấu giá đã đóng chưa mà lại đòi đổi?".
- Phân tách rõ ràng tầng Service và Tầng Model. Tránh nhét quá nhiều logic kết nối vào Model. Model chỉ kiểm tra tính đúng đắn trên thân thể nó.

## 2. TÀI LIỆU CẦN HỌC & NGHIÊN CỨU
> 💡 **KHÔNG code ngay.** Hãy đọc hiểu các bài viết này trước.
1. **Service Layer Pattern:** Thường thấy trong Spring Boot. Tìm docs của Baeldung/những bài blog "Service vs DAO layer". Hiểu tại sao cần 1 lớp Service kẹp giữa Controller(Network) và DAO.
2. **Java Datetime API:** Học cách sử dụng `LocalDateTime` (không dùng `java.util.Date` cổ lỗ sĩ). Cách tính `Duration` hoặc so sánh `isBefore()`, `isAfter()`.
3. **Dependency Injection (đơn giản):** Truyền đối tượng DAO thông qua Constructor của Service thay vì gọi `new UserDAO()` bên trong các dòng code của Service.

---

## 3. CHI TIẾT CÁC LỚP CẦN XÂY DỰNG (GIAI ĐOẠN 1: T3-T4)

### 3.1. `model/AuctionStatus.java` (Enum)
- **Mục đích:** Quy ước trạng thái phiên chợ. Tránh trường hợp người thì gõ "running", người thì gõ "RUNNING".
- **Values:** `OPEN`, `RUNNING`, `FINISHED`, `PAID`, `CANCELED`.

### 3.2. `model/Auction.java`
- **Mục đích:** Quản lý trọn gói 1 phiên đấu giá sản phẩm.
- **Kế thừa:** `extends Entity` (phụ thuộc Cường).
- **Fields (private):** 
  - `item` (Item) (nguồn phụ thuộc Khánh)
  - `seller` (User) (nguồn phụ thuộc Cường)
  - `startTime`, `endTime` (LocalDateTime)
  - `currentHighestBid` (double)
  - `currentHighestBidderId` (String) - có thể lưu mỗi Id đỡ nặng json
  - `status` (AuctionStatus)
  - `bidHistory` (List<BidTransaction>) -> Khởi tạo danh sách bằng mảng rỗng ArrayList.
- **Yêu cầu triển khai - CÁC HÀM CỐT LÕI:**
  - `placeBid(Bidder bidder, double amount)`:
    - Nếu `status` khác `RUNNING`: ném `AuctionClosedException` (Anh sẽ cung cấp).
    - Nếu `amount <= currentHighestBid`: ném `InvalidBidException`.
    - Xử lý thành công: Cập nhật `currentHighestBid`, ghi nhận thông tin `BidderId`, tạo `BidTransaction` và add vào `bidHistory`.
  - `updateStatus(AuctionStatus newStatus)`: Thay đổi trạng thái.

### 3.3. `model/BidTransaction.java`
- **Mục đích:** Biên lai điện tử cho mỗi lần ra giá thành công.
- **Kế thừa:** `extends Entity`.
- **Fields (private):** `auctionId`, `bidderId`, `bidderUsername`, `amount`, `timestamp`.
- **Yêu cầu triển khai:** Class này chủ yếu mang tính lưu trữ. Cần constructor điền đủ thông tin, không cần logic quá rườm rà.

---

## 4. CHI TIẾT CÁC LỚP CẦN XÂY DỰNG (GIAI ĐOẠN 2: T6-T7)

### 4.1. `service/UserService.java`
- **Mục đích:** Cung cấp API nội bộ cho UI/Network để thực hiện việc tương tác vào bảng Account.
- **Yêu cầu triển khai:**
  - Dùng Dependency Injection qua Constructor: `public UserService(GenericDAO<User> userDao)`
  - `User register(username, password, email, role)`: Sử dụng `ValidationUtils` (Anh cung cấp) để check định dạng email/pass. Vòng qua list All Users xem ai trùng username không, trùng thì ném `DuplicateUserException` (Anh cung cấp). Hoàn tất thì gọi the DAO save.
  - `User login(username, password)`: get object bằng `findByUsername` bên DAO. Không thấy hoặc sai mật khẩu ném chung `AuthenticationException` (để bảo mật, không nói rõ user ko tồn tại hay sai pass).
  - `User getUserById(String id)`.

### 4.2. `service/AuctionService.java`
- **Mục đích:** Cung cấp API thao tác bảng Auction.
- **Yêu cầu triển khai:**
  - Dùng Dependency Injection: Cần `GenericDAO<Auction> auctionDAO`.
  - `Auction createAuction(item, seller, startTime, endTime)`: Validate không cho startTime ở ngày quá khứ. Gọi DAO save. Trả về đối tượng vừa tạo.
  - `List<Auction> getActiveAuctions()`: Thông qua function của DAO, lấy về danh sách các sàn đang mở (status: `RUNNING` hoặc `OPEN`).
  - `Auction getAuctionById(String id)`.
- **Ghi chú:** Service này chưa có chức năng trực tiếp đặt thầu (place bid), đặt thầu sẽ do `BidService` bên (Anh) xử lý logic cho tách biệt.

---

## 5. YÊU CẦU TEST BẮT BUỘC (JUNIT 5)
Bạn cần viết test cho code của mình tại thư mục test (`src/test/java/...`).

**1. `AuctionTest.java` (Test Model không dùng DAO)**
- Tạo thẳng Auction ảo trong memory.
- Gọi hàm `placeBid` với giá trị thấp hơn giá hiện tại. Dùng `assertThrows` xem nó có ném đúng ngoại lệ dội ngược không.
- Gọi `placeBid` vào thời điểm phiên đang ở `FINISHED`.

**2. `UserServiceTest.java` (Test Services)**
- *Lưu ý: Vì bạn làm test lúc DAO chưa có code thật, hãy tạo 1 class `MockUserDAO implements GenericDAO` trong test, quản lý data bằng một cái `HashMap` trong RAM.*
- Mock database đã có user "cuong". Gọi `register` tạo thêm 1 thằng "cuong" -> kì vọng văng ném ngoại lệ đúng.
- Gọi `login` -> pass đúng nhận object, pass sai đập lỗi.

> **💡 Lời khuyên khi code:** Hãy liên lạc với Anh, xin list tên file Custom Exception. Bạn cứ viết `throw new ...` rồi import lớp của Anh, nhờ IDE tự sinh class hoặc interface để code của bạn chạy được trước.
