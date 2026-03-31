# 📋 TUẦN 2 & 3 - PART 1: NHIỆM VỤ CỦA CƯỜNG (User Model & User DAO)

## 1. MỤC TIÊU VÀ ĐỊNH HƯỚNG KIẾN TRÚC
Nhiệm vụ của bạn là xây dựng nền móng cho các thực thể (Entity) quan trọng nhất trong hệ thống và cơ chế lưu trữ dữ liệu (DAO). 
- **Domain Model (Entity):** Là các đối tượng trọng tâm, mang theo dữ liệu và logic cơ bản. Mọi thành phần khác trong app đều tương tác với chúng.
- **DAO (Data Access Object):** Design pattern giúp tách biệt giữa tầng Business Logic (Service) và cách dữ liệu được lưu xuống (JSON, SQL,...). Nếu sau này bạn đổi sang SQL, các Service của các bạn khác sẽ không bị ảnh hưởng.

## 2. TÀI LIỆU CẦN HỌC & NGHIÊN CỨU
> 💡 **KHÔNG code ngay.** Hãy đọc hiểu các bài viết này trước.
1. **Abstract Class & Inheritance:** Tìm đọc "Oracle Java Tutorials: Abstract Methods and Classes". HIểu tại sao `Entity` và `User` cần là `abstract`.
2. **Enum trong Java:** Tìm đọc "Baeldung Java Enums" để biết cách định nghĩa các methods và fields bên trong một Enum (không chỉ là danh sách các chuỗi).
3. **DAO Pattern:** Tìm đọc "Baeldung Data Access Object Pattern in Java".
4. **Gson Serialization / Deserialization:** Đọc "Gson User Guide" trên Github của Google để biết chu trình biến Object thành String JSON và ngược lại.
5. **Java I/O:** Tìm đọc cách đọc ghi file bằng `java.nio.file.Files` và cơ chế `try-with-resources`.

---

## 3. CHI TIẾT CÁC LỚP CẦN XÂY DỰNG (GIAI ĐOẠN 1: T3-T4)

### 3.1. `model/Entity.java` (Abstract Class)
- **Mục đích:** Đảm bảo bất cứ thực thể nào lưu xuống database đều có chung các thông tin định danh và thời gian.
- **Fields (protected):** `id` (String UUID), `createdAt` (LocalDateTime), `updatedAt` (LocalDateTime).
- **Yêu cầu triển khai:**
  - Constructor mặc định cần tự sinh ra một UUID ngẫu nhiên cho `id` và lấy giờ hiện tại cho `createdAt`, `updatedAt`.
  - Bạn phải tự Override (ghi đè) method `equals()` và `hashCode()` để so sánh 2 Entity. Hai entity bằng nhau khi và chỉ khi `id` giống nhau.

### 3.2. `model/UserRole.java` (Enum)
- **Mục đích:** Phân định quyền của User. Enum giúp tránh việc gõ sai chuỗi (ví dụ: gõ "Admin" thay vì "ADMIN") dẫn đến lỗi bảo mật.
- **Values:** `BIDDER`, `SELLER`, `ADMIN`.
- **Yêu cầu triển khai:**
  - Kèm theo một thuộc tính `displayName` (tiếng Việt).
  - Viết constructor cho Enum này và method `String getDisplayName()`.

### 3.3. `model/User.java` (Abstract Class)
- **Mục đích:** Chứa các thông tin chung nhất của người dùng. Là abstract vì trong hệ thống, chúng ta sẽ quản lý các role cụ thể (Bidder/Seller/Admin) chứ không quản lý "User chung chung".
- **Kế thừa:** `extends Entity`.
- **Fields (private):** `username`, `password` (sau này chứa hash), `email`, `role` (UserRole).
- **Yêu cầu triển khai:**
  - Viết 1 phương thức abstract `public abstract String getInfo();` để bắt buộc các class con phải định nghĩa cách chúng giới thiệu bản thân.

### 3.4. `model/Bidder.java`, `model/Seller.java`, `model/Admin.java`
- **Mục đích:** Đại diện cho các Actor cụ thể của hệ thống.
- **Kế thừa:** Đều `extends User`.
- **Yêu cầu triển khai:**
  - Constructor nhận vào username, password, email. Tham số `role` không cần nhận từ ngoài mà _tự gán cứng_ ở constructor `super(...)` để đảm bảo không ai khởi tạo Bidder nhưng lại gán quyển Admin.
  - Implement phương thức `getInfo()` trả về các chuỗi khác nhau phù hợp với từng class.

---

## 4. CHI TIẾT CÁC LỚP CẦN XÂY DỰNG (GIAI ĐOẠN 2: T6-T7)

### 4.1. `dao/GenericDAO.java` (Interface)
- **Mục đích:** Định nghĩa hành vi chung cho bất kỳ DAO nào. Sử dụng Generics `<T extends Entity>` để có thể áp dụng cho `User`, `Item`, `Auction`...
- **Method (Signature):**
  - `void save(T entity);`
  - `T findById(String id);`
  - `List<T> findAll();`
  - `void update(T entity);`
  - `void delete(String id);`

### 4.2. `dao/JsonFileHelper.java` (Utility Class)
- **Mục đích:** Tập trung toàn bộ logic việc đóng/mở file, đọc JSON và parse vào Class này. Không để logic mở file lem nhem sang các class DAO khác.
- **Yêu cầu triển khai:**
  - Các method phải là `static`.
  - Có method nhận đầu vào là List Data và `filePath`, sử dụng `Gson` để convert list thành String và dùng `Files.writeString` (hoặc BufferedWriter) đẩy xuống ổ đĩa đè lên file.
  - Có method nhận filePath và đọc String từ đĩa lên.

### 4.3. `dao/UserDAO.java`
- **Mục đích:** Quản lý hành vi thêm, sửa, xóa, lấy User từ database.
- **Implement:** `implements GenericDAO<User>`.
- **Yêu cầu triển khai:**
  - Quy ước lưu dữ liệu vào thư mục `data/users.json`. Đảm bảo file được tự tạo néu chưa tồn tại.
  - Định nghĩa thêm các hành vi đặc thù như `User findByUsername(String username)` và `User findByEmail(String email)`.
  - Trong hàm `save` hoặc `update`, dữ liệu sau khi chỉnh sửa trên bộ nhớ phải lập tức gọi đến `JsonFileHelper` để ghi đè xuống đĩa. (Không quản lý cache bộ nhớ ở mức độ bài tập này để tránh data không nhất quán).

---

## 5. YÊU CẦU TEST BẮT BUỘC (JUNIT 5)
Bạn cần viết test cho code của mình tại thư mục test (`src/test/java/...`).

**1. `UserTest.java`**
- Test khởi tạo các loại người dùng. Kiểm tra constructor có set đúng Role không.
- Test Đa hình (`Polymorphism`): Nhét tất cả Admin, Seller, Bidder vào trong `List<User>`, duyệt qua list và gọi `getInfo()`. Xác nhận đa hình hoạt động đúng.
- Test việc `id` khi tạo entity mới có dính nhau không.

**2. `UserDAOTest.java`**
- Test ghi file: Gọi lệnh `save`, sau đó vào thử file `users.json` bằng Notepad để xem file có tạo không.
- Chạy `findByUsername` với username đúng và username sai (kì vọng trả về `null`).
- Test Update email một user, lấy ra kiểm tra lại xem email có thay đổi dính xuống ổ cứng hay không.

> **💡 Lời khuyên khi code:** Hãy viết các interface trống và đẩy (push) lên nhánh trung gian (develop) để các thành viên khác có thể nhìn thấy cấu trúc hàm của bạn. Còn ruột hàm bạn viết sau.
