# Hướng dẫn JavaFX cho nhóm

## 1. Cách FXML và Controller liên kết nhau
Có thể hiểu đơn giản FXML là phần "xác" (giao diện), còn Controller là phần "hồn" (code xử lý logic). Chúng nó liên kết với nhau qua 2 bước:
- Tại FXML:Thẻ root (ngoài cùng) phải có thuộc tính `fx:controller="com.auctionuet.client.controllers.MainController"` để trỏ đúng đường dẫn đến class Java.
- Tại Controller:Các thành phần giao diện bên FXML (ví dụ nút bấm, chữ) phải được đặt `fx:id` (VD: `fx:id="startButton"`). Trong Controller, ta khai báo biến trùng tên 100% với `fx:id` đó và gắn thêm annotation `@FXML` lên đầu để JavaFX tự động map chúng lại với nhau.

## 2. Lifecycle của JavaFX Application
Vòng đời của một ứng dụng JavaFX trải qua các giai đoạn chính:
1. Application.launch(): Điểm bắt đầu kích hoạt app từ hàm main.
2. init(): Chạy ngầm trước khi giao diện hiện lên (thường dùng để load data, cấu hình).
3. start(Stage primaryStage): Quan trọng nhất! Sân khấu lên đèn. Đây là nơi ta load file FXML vào `Scene`, nhét Scene vào `Stage` và gọi `primaryStage.show()` để ném giao diện lên màn hình.
4. Ứng dụng chạy và chờ người dùng tương tác.
5. stop(): Được gọi khi tắt ứng dụng (dùng để đóng kết nối mạng, dọn dẹp bộ nhớ).

## 3. @FXML annotation hoạt động thế nào
Bình thường trong Java, nếu khai báo biến là `private`, các class bên ngoài đéo thể chọc vào được. Nhưng `@FXML` giống như một "chìa khóa cửa sau". Khi cắm cờ này trước biến hoặc phương thức, ta cho phép `FXMLLoader` sử dụng cơ chế Reflection của Java để lách luật, tự động gán giá trị hoặc kích hoạt phương thức đó một cách hợp lệ.

## 4. Cách thêm event handler cho button
- **Cách nhàn nhất (dùng Scene Builder):** Chọn Button, mở tab Code bên phải. Tìm ô `On Action` và gõ tên hàm bắt đầu bằng dấu `#` (ví dụ: `#onStartClicked`). Sau đó về file Controller, viết một method `private void onStartClicked()` và cắm cờ `@FXML` lên đầu hàm là xong.

## 5. Cách thay đổi style của element từ code Java
Dù nhóm mình ưu tiên dùng file CSS riêng, nhưng nếu cần đổi UI động lúc đang chạy (ví dụ báo lỗi đỏ chót), ta có thể gọi các hàm setter.
Ví dụ muốn đổi màu chữ của một Label:
Dùng `statusLabel.setTextFill(Color.RED);` hoặc `statusLabel.setStyle("-fx-text-fill: red;");`

## 6. Hướng dẫn dùng Scene Builder
Quy trình chuẩn cho anh em trong nhóm làm UI:
1. Chuột phải vào file `.fxml` trong IntelliJ -> chọn **Open in SceneBuilder**.
2. Tìm Layout ở cột Library bên trái (VBox, HBox, BorderPane...) kéo vào giữa làm khung.
3. Kéo tiếp các UI Controls (Label, Button, TextField) thả vào khung.
4. Chọn phần tử vừa kéo, sang tab Code bên phải đặt tên cho ô `fx:id`.
5. Sang tab Controller (góc dưới trái), điền đường dẫn class Controller vào.
6. Ấn `Ctrl + S` lưu lại, code XML sẽ tự động render vào IntelliJ.