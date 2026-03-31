# 📋 TUẦN 2 & 3 - PART 2: NHIỆM VỤ CỦA KHÁNH (Item Model, Factory & Item/Auction DAO)

## 1. MỤC TIÊU VÀ ĐỊNH HƯỚNG KIẾN TRÚC
Nhiệm vụ của bạn là định hình sản phẩm đấu giá (Item), đưa Design Pattern vào dự án và giải quyết một trong những phần khó nhằn nhất của việc dùng JSON: "Gson Polymorphism" (Gson Đa hình).
- **Factory Pattern:** Giúp gom logic khởi tạo đối tượng phức tạp lại một chỗ.
- **Gson Polymorphism:** Khi chúng ta load một list các `Item` từ file JSON, làm sao để Gson biết đoạn text A nên biến thành `Electronics`, đoạn text B nên biến thành `Art`?

## 2. TÀI LIỆU CẦN HỌC & NGHIÊN CỨU
> 💡 **KHÔNG code ngay.** Hãy đọc hiểu các bài viết này trước.
1. **Factory Method Pattern:** Đọc trang Refactoring.guru phần "Factory Method Pattern". Hiểu được lợi ích của The Creator Class.
2. **Polymorphic Serialization with Gson:** Search Google "Gson polymorphism RuntimeTypeAdapterFactory". Đây là class bổ sung của Google (Gson Extras) giúp định nghĩa trường `type` vào trong file JSON. Bạn sẽ hiểu được cách máy tính phân biệt được các object kế thừa.
3. **Generics in Java:** Ôn lại kiến thức về truyền tham số kiểu Generic (`<T>`, `List<T>`).

---

## 3. CHI TIẾT CÁC LỚP CẦN XÂY DỰNG (GIAI ĐOẠN 1: T3-T4)

### 3.1. `model/ItemType.java` (Enum)
- **Mục đích:** Định danh loại sản phẩm đấu giá.
- **Values:** `ELECTRONICS`, `ART`, `VEHICLE`.
- **Yêu cầu triển khai:** Có thể bao gồm `displayName` nếu cần hiển thị lên giao diện tiếng Việt.

### 3.2. `model/Item.java` (Abstract Class)
- **Mục đích:** Định nghĩa các khung sườn chung nhất của một vật phẩm đem ra đấu giá.
- **Kế thừa:** Kế thừa từ class `Entity` mà Cường sẽ tạo. (Hãy đợi Cường push code hoặc nói Cường đưa cho bạn cấu trúc Interface trước).
- **Fields (private):** `name`, `description`, `startingPrice`, `imageUrl`, `type` (ItemType).
- **Yêu cầu triển khai:**
  - Gồm một hàm abstract `public abstract String printInfo()`.
  - Có getters/setters cho các trường dữ liệu.

### 3.3. `model/Electronics.java`, `model/Art.java`, `model/Vehicle.java`
- **Mục đích:** Các sản phẩm chuyên biệt.
- **Kế thừa:** Tất cả đều `extends Item`.
- **Fields bổ sung (private):** 
  - *Electronics:* `brand`, `warrantyMonths`
  - *Art:* `artist`, `year`
  - *Vehicle:* `make`, `model`, `mileage`
- **Yêu cầu triển khai:** 
  - Override `printInfo()` để trả chuỗi kèm các thông số đặc thù. 
  - Constructor của các con phải set cứng thuộc tính `type` của class cha tương ứng với enum của nó.

### 3.4. `model/ItemFactory.java`
- **Mục đích:** Nơi duy nhất tạo ra các Item, giấu đi các chi tiết khởi tạo.
- **Yêu cầu triển khai:**
  - Viết method `public static Item createItem(...)`. Nhan vào `ItemType` và các tham số cần thiết (hoặc một đối số Map/Record nếu quá nhiều tham số).
  - Dùng câu lệnh `switch` theo `ItemType`. Trả về đúng object tương ứng.
  - Có `default:` ném ra ngoại lệ `IllegalArgumentException` nếu truyền vào type lạ.

---

## 4. CHI TIẾT CÁC LỚP CẦN XÂY DỰNG (GIAI ĐOẠN 2: T6-T7)

### 4.1. VẤN ĐỀ KHÓ NHẤT: `dao/GsonFactory.java`
- **Mục đích:** Trả về một đối tượng `Gson` tĩnh (static instance) đã được chèn vào các quy tắc phân tích đa hình cho dự án.
- **Yêu cầu triển khai:**
  - Bạn cần tìm source code của class `RuntimeTypeAdapterFactory.java` (thuộc Gson Extras) trên Internet, tạo class đó trong package `util` hoặc `dao` của dự án của mình (vì nó không nằm trong thư viện gson gốc core).
  - Khởi tạo instance:
    ```java
    RuntimeTypeAdapterFactory<Item> itemAdapterFactory = RuntimeTypeAdapterFactory.of(Item.class, "type")
        .registerSubtype(Electronics.class, "ELECTRONICS")
        .registerSubtype(Art.class, "ART")
        .registerSubtype(Vehicle.class, "VEHICLE");
    ```
  - Khởi tạo Gson: `Gson gson = new GsonBuilder().registerTypeAdapterFactory(itemAdapterFactory).create();`
  - Chức năng này phục vụ không chỉ cho bạn là còn cho cả Cường và Công.

### 4.2. `dao/ItemDAO.java`
- **Mục đích:** Lưu trữ / load object `Item`.
- **Implement:** `implements GenericDAO<Item>` (Đợi interface của Cường).
- **Yêu cầu triển khai:**
  - Ghi vào `data/items.json`.
  - Sử dụng chung hàm của class `JsonFileHelper` do Cường cung cấp. Thay vì dùng `new Gson()`, yêu cầu phải lấy `GsonFactory` trong hàm read/write.
  - Bổ sung `List<Item> findByType(ItemType type)`. Cày vòng lặp qua `findAll` để gạn lọc ra.

### 4.3. `dao/AuctionDAO.java`
- **Mục đích:** Lưu trữ phiên đấu giá. Object `Auction` do Công viết (chứa cả object `Item` bên trong nó).
- **Implement:** `implements GenericDAO<Auction>`.
- **Yêu cầu triển khai:**
  - Ghi vào `data/auctions.json`. 
  - GsonFactory đã được setup tự động hiểu cách bung cấu trúc đa hình của `Item` nằm bên trong `Auction`.
  - Bổ sung: `List<Auction> findByStatus(AuctionStatus status)` bằng cách lặp và đếm.

---

## 5. YÊU CẦU TEST BẮT BUỘC (JUNIT 5)
Bạn cần viết test cho code của mình tại thư mục test (`src/test/java/...`).

**1. `ItemFactoryTest.java`**
- Gọi Factory đẩy ra `Electronics`. Phải assert là `instanceof Electronics` trước mặt máy tính.
- Cố tình truyền `null` vào Type, kiểm tra xem có quăng ra `IllegalArgumentException` không bằng hàm `assertThrows` của Junit.

**2. `ItemDAOTest.java` & `AuctionDAOTest.java`**
- Test quan trọng nhất dự án (nằm ở bạn): Lưu xuống 1 `List<Item>` chứa 1 điện thoại + 1 bức tranh. Load lại List trên. Gán cái ở index 0 thành biến điện thoại. Gán index 1 thành bức tranh. Check xem dữ liệu đa hình có sống lại toàn diện không. (Nếu chết tức là `GsonFactory` của bạn chưa hoạt động).
- Test hàm `findByStatus` đảm bảo đọc đúng.

> **💡 Lời khuyên khi code:** Hãy liên lạc với Cường bằng comment trên Issue khi bạn gặp rắc rối vì cả hai đều dính chung vào Json và DAO. Bạn cần `RuntimeTypeAdapterFactory` - hãy search code này ném vào dự án chứ không cần tự code lại bánh xe của GG.
