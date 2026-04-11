# 📋 TUẦN 1 — BÀI TẬP CHI TIẾT (23/03 – 29/03/2026)

> **Deadline nộp bài:** Thứ 7 ngày 28/03/2026, 18:00  
> **Kiểm tra chéo:** Thứ 7 28/03/2026 (buổi tối)  
> **Merge code:** Thứ 7 29/03/2026 (sau khi review xong)

---

## 📌 PHẦN CHUNG — AI CŨNG PHẢI LÀM (Tự học)

> [!CAUTION]
> Mỗi người **BẮT BUỘC** phải hoàn thành phần tự học này. Sẽ hỏi bất kỳ ai bất kỳ câu nào.

### Bài 0.1 — Cài đặt môi trường

**Yêu cầu cụ thể:**
1. Cài **JDK 21** (Adoptium/Eclipse Temurin): https://adoptium.net
2. Cài **IntelliJ IDEA** (Community hoặc Ultimate): https://jetbrains.com/idea
3. Cài **Scene Builder** cho JavaFX: https://gluonhq.com/products/scene-builder/
4. Cài **Git**: https://git-scm.com
5. Cài **Maven** (nếu chưa có bundled trong IntelliJ): https://maven.apache.org

**Đầu ra kiểm tra (chụp ảnh screenshots gửi nhóm):**
```bash
# Chạy lệnh này trong terminal/cmd, chụp kết quả:
java -version        # → openjdk version "21.x.x"
mvn -version         # → Apache Maven 3.x.x
git --version        # → git version 2.x.x
```
- Mở IntelliJ → New Project → chạy được file Java "Hello World"
- Mở Scene Builder → kéo thả 1 Button vào canvas → Save thành file `.fxml`

---

### Bài 0.2 — Học Git cơ bản

**Tài liệu bắt buộc đọc:**
- Chơi game: https://learngitbranching.js.org (hoàn thành ít nhất Main > Introduction Sequence)
- Đọc: https://docs.github.com/en/get-started/quickstart/hello-world

**Đầu ra kiểm tra (Chủ nhật sẽ kiểm tra):**

Mỗi người phải trả lời được các câu hỏi sau (bằng miệng, không xem tài liệu):
1. `git add` vs `git commit` khác nhau thế nào?
2. `git pull` vs `git fetch` khác nhau thế nào?
3. `git merge` vs `git rebase` — cái nào nên dùng khi làm nhóm? Tại sao?
4. Làm sao tạo branch mới, chuyển sang branch đó, push lên GitHub?
5. Conflict xảy ra khi nào? Cách giải quyết conflict?
6. Pull Request (PR) là gì? Tại sao cần review trước khi merge?

**Bài thực hành Git (mỗi người tự làm):**
```bash
# 1. Clone repo
git clone https://github.com/iTCuong090/AuctionUET.git

# 2. Tạo branch riêng
git checkout -b practice/tuan-1-[TEN-BAN]
# Ví dụ: practice/tuan-1-cuong

# 3. Tạo file `members/[TEN-BAN].md` với nội dung:
# - Họ tên
# - MSSV
# - Một câu tự giới thiệu
# - Công cụ đã cài (JDK version, IntelliJ version, etc.)

# 4. Commit
git add .
git commit -m "docs: thêm thông tin thành viên [TEN]"

# 5. Push
git push origin practice/tuan-1-[TEN-BAN]

# 6. Vào GitHub → Tạo Pull Request → gán reviewer cho 2 người còn lại
```

---

### Bài 0.3 — Học Maven cơ bản

**Tài liệu bắt buộc đọc:**
- https://maven.apache.org/guides/getting-started/maven-in-five-minutes.html
- https://maven.apache.org/guides/introduction/introduction-to-the-pom.html

**Đầu ra kiểm tra (Chủ nhật hỏi miệng):**
1. `pom.xml` là gì? Chứa những thông tin gì quan trọng?
2. `groupId`, `artifactId`, `version` nghĩa là gì?
3. `<dependency>` dùng để làm gì? Thêm dependency mới như thế nào?
4. `mvn compile`, `mvn test`, `mvn package`, `mvn clean` — mỗi lệnh làm gì?
5. Maven project structure chuẩn `src/main/java`, `src/test/java` có ý nghĩa gì?
6. Multi-module project là gì? Khi nào nên dùng?

---

### Bài 0.4 — Học JavaFX cơ bản

**Tài liệu bắt buộc đọc:**
- https://openjfx.io/openjfx-docs/
- Video: tìm "JavaFX Hello World tutorial" trên YouTube (xem 1 video ~15 phút)

**Đầu ra kiểm tra:**
1. JavaFX Stage, Scene, Node là gì? Mối quan hệ giữa chúng?
2. FXML là gì? Tại sao dùng FXML thay vì code Java thuần?
3. Controller trong JavaFX dùng để làm gì? `@FXML` annotation là gì?
4. Scene Builder kết nối với code Java qua cơ chế nào?

---

## 🔨 PHẦN CÁ NHÂN — NHIỆM VỤ RIÊNG

> [!IMPORTANT]
> Mỗi người làm trên **branch riêng**. KHÔNG push thẳng vào `main`.
> Đến Chủ nhật mới tạo PR để merge.

---

## 👤 CƯỜNG — Khởi tạo Maven Multi-Module Project

```
Branch: feature/tuan-1-cuong-maven-setup
Deadline: Thứ 7 28/03 23:59
```

### 📝 Mô tả bài tập

Bạn là người tạo nền móng cho toàn bộ dự án. Nhiệm vụ là tạo Maven multi-module project với cấu trúc chuẩn, sao cho tất cả thành viên khác có thể clone về và bắt đầu code ngay trên nền tảng này.

### 📋 Yêu cầu chi tiết

#### Bước 1: Tạo Parent POM (`pom.xml` ở thư mục gốc)

Tạo file `pom.xml` tại thư mục gốc (`AuctionUET/pom.xml`) với nội dung:

```xml
<!-- Yêu cầu trong Parent POM: -->
<groupId>com.auctionuet</groupId>
<artifactId>auction-uet</artifactId>
<version>1.0-SNAPSHOT</version>
<packaging>pom</packaging>

<!-- Modules -->
<modules>
    <module>auction-server</module>
    <module>auction-client</module>
</modules>

<!-- Properties: -->
<!-- - Java version: 21 -->
<!-- - Maven compiler source/target: 21 -->
<!-- - Encoding: UTF-8 -->

<!-- Dependencies chung (dependencyManagement): -->
<!-- - JUnit 5 (version 5.10.x) -->
<!-- - Gson (version 2.10.x) -->
```

**Yêu cầu kỹ thuật:**
- Dùng `<dependencyManagement>` để quản lý version cho toàn bộ project
- Set `<maven.compiler.source>` và `<maven.compiler.target>` là `21`
- Set `<project.build.sourceEncoding>` là `UTF-8`

#### Bước 2: Tạo module `auction-server`

Tạo thư mục `auction-server/` với cấu trúc:
```
auction-server/
├── pom.xml
└── src/
    ├── main/
    │   └── java/
    │       └── com/
    │           └── auctionuet/
    │               └── server/
    │                   └── ServerApp.java
    └── test/
        └── java/
            └── com/
                └── auctionuet/
                    └── server/
                        └── ServerAppTest.java
```

**`auction-server/pom.xml`:**
- `<parent>` trỏ về parent POM
- Dependencies: JUnit 5 (scope test)

**`ServerApp.java`:**
```java
package com.auctionuet.server;

public class ServerApp {
    
    public static final String APP_NAME = "AuctionUET Server";
    public static final String VERSION = "1.0-SNAPSHOT";
    
    public static String getWelcomeMessage() {
        return APP_NAME + " v" + VERSION + " started successfully!";
    }
    
    public static void main(String[] args) {
        System.out.println(getWelcomeMessage());
        System.out.println("Listening on port 8888...");
        // Server sẽ được implement ở tuần 4
    }
}
```

**`ServerAppTest.java`:**
```java
package com.auctionuet.server;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ServerAppTest {
    
    @Test
    void testGetWelcomeMessage() {
        String message = ServerApp.getWelcomeMessage();
        assertNotNull(message);
        assertTrue(message.contains("AuctionUET Server"));
        assertTrue(message.contains("1.0-SNAPSHOT"));
    }
    
    @Test
    void testAppNameNotNull() {
        assertNotNull(ServerApp.APP_NAME);
        assertFalse(ServerApp.APP_NAME.isEmpty());
    }
}
```

#### Bước 3: Tạo module `auction-client`

Tạo thư mục `auction-client/` với cấu trúc:
```
auction-client/
├── pom.xml
└── src/
    ├── main/
    │   ├── java/
    │   │   └── com/
    │   │       └── auctionuet/
    │   │           └── client/
    │   │               └── ClientApp.java
    │   └── resources/
    │       └── com/
    │           └── auctionuet/
    │               └── client/
    │                   └── (FXML files sẽ thêm ở đây sau)
    └── test/
        └── java/
            └── com/
                └── auctionuet/
                    └── client/
                        └── ClientAppTest.java
```

**`auction-client/pom.xml`:**
- `<parent>` trỏ về parent POM
- Dependencies:
  - `org.openjfx:javafx-controls:21`
  - `org.openjfx:javafx-fxml:21`
  - JUnit 5 (scope test)
- Plugin: `org.openjfx:javafx-maven-plugin:0.0.8` (để chạy `mvn javafx:run`)

**`ClientApp.java`:**
```java
package com.auctionuet.client;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class ClientApp extends Application {
    
    public static final String APP_NAME = "AuctionUET Client";
    
    @Override
    public void start(Stage primaryStage) {
        Label label = new Label("Welcome to AuctionUET!");
        label.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");
        
        StackPane root = new StackPane(label);
        Scene scene = new Scene(root, 800, 600);
        
        primaryStage.setTitle(APP_NAME);
        primaryStage.setScene(scene);
        primaryStage.show();
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}
```

**`ClientAppTest.java`:**
```java
package com.auctionuet.client;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ClientAppTest {
    
    @Test
    void testAppNameNotNull() {
        assertNotNull(ClientApp.APP_NAME);
        assertEquals("AuctionUET Client", ClientApp.APP_NAME);
    }
}
```

#### Bước 4: Cập nhật `.gitignore`

Đảm bảo file `.gitignore` ở thư mục gốc chứa:
```gitignore
# Maven
target/

# IntelliJ IDEA
.idea/
*.iml
out/

# OS
.DS_Store
Thumbs.db

# VS Code
.vscode/

# Compiled
*.class
*.jar

# Logs
*.log
```

### ✅ TEST ĐẦU RA — Cách kiểm tra bài của Cường

Người review (Khánh, Công, hoặc Anh) clone/pull branch `feature/tuan-1-cuong-maven-setup` và chạy lần lượt:

```bash
# Test 1: Compile toàn bộ project
cd AuctionUET
mvn compile
# ✅ PASS nếu: BUILD SUCCESS, không có error

# Test 2: Chạy tests
mvn test
# ✅ PASS nếu: Tests run: ≥3, Failures: 0, Errors: 0

# Test 3: Package
mvn clean package
# ✅ PASS nếu: BUILD SUCCESS ở cả 2 modules

# Test 4: Chạy ServerApp
cd auction-server
mvn exec:java -Dexec.mainClass="com.auctionuet.server.ServerApp"
# ✅ PASS nếu: In ra "AuctionUET Server v1.0-SNAPSHOT started successfully!"

# Test 5: Chạy ClientApp (cần JavaFX)
cd ../auction-client
mvn javafx:run
# ✅ PASS nếu: Mở cửa sổ JavaFX với label "Welcome to AuctionUET!"

# Test 6: Kiểm tra cấu trúc thư mục
# ✅ PASS nếu: Đúng cấu trúc Maven chuẩn như mô tả ở trên
```

### ❌ FAIL nếu:
- `mvn compile` bị lỗi
- Tests fail hoặc không có test
- Cấu trúc thư mục không theo Maven standard
- Thiếu module server hoặc client
- `.gitignore` không loại trừ `target/`, `.idea/`

---

## 👤 KHÁNH — Thiết lập Git Workflow & CI/CD

```
Branch: feature/tuan-1-khanh-git-cicd
Deadline: Thứ 7 28/03 23:59
```

### 📝 Mô tả bài tập

Bạn chịu trách nhiệm thiết lập quy trình làm việc nhóm trên GitHub. Mục tiêu là đảm bảo mọi người đều tuân thủ quy trình: tạo branch → code → push → tạo PR → review → merge. Ngoài ra setup CI/CD để tự động chạy test mỗi khi push code.

### 📋 Yêu cầu chi tiết

#### Bước 1: Tạo file CI/CD `.github/workflows/ci.yml`

```yaml
# File: .github/workflows/ci.yml
# Yêu cầu:
# - Trigger: on push (tất cả branch) và on pull_request (vào main và develop)
# - Chạy trên: ubuntu-latest
# - Steps:
#   1. Checkout code
#   2. Setup JDK 21 (dùng actions/setup-java@v4, distribution: 'temurin')
#   3. Cache Maven dependencies
#   4. Chạy mvn test
```

**Yêu cầu kỹ thuật:**
- Workflow phải dùng `actions/checkout@v4`
- JDK setup dùng `actions/setup-java@v4` với `distribution: 'temurin'`, `java-version: '21'`
- Cache Maven dùng `actions/cache@v4` với path `~/.m2/repository`
- Chạy `mvn test --batch-mode` (batch-mode để tránh interactive prompts)

#### Bước 2: Tạo file `CONTRIBUTING.md`

File hướng dẫn đóng góp code, nội dung **BẮT BUỘC** phải có:

```markdown
# Hướng dẫn đóng góp - AuctionUET

## Quy trình làm việc

### 1. Tạo branch
- Từ `main`, tạo branch mới theo format: `feature/tuan-X-ten-nguoi-mo-ta`
- Ví dụ: `feature/tuan-1-cuong-maven-setup`

### 2. Coding
- Tuân thủ Google Java Style Guide
- Viết comment tiếng Việt hoặc tiếng Anh (nhất quán)
- Mỗi commit phải có message rõ ràng theo Conventional Commits:
  - `feat: thêm class User`
  - `fix: sửa lỗi chia cho 0 trong Calculator`
  - `docs: thêm Javadoc cho class Auction`
  - `test: thêm test cho UserService`
  - `refactor: tách method validateBid`

### 3. Push & Pull Request
- Push branch lên GitHub: `git push origin feature/tuan-X-...`
- Tạo Pull Request vào `main`
- Gán ít nhất 2 reviewers
- Mô tả PR rõ ràng (dùng PR template)

### 4. Review
- Mỗi người phải review ít nhất 2 PR / tuần
- Khi review, kiểm tra:
  - Code chạy được không?
  - Logic đúng không?
  - Có viết test không?
  - Naming convention OK không?
  - Có edge case nào bỏ sót?

### 5. Merge
- Cần ít nhất 2 approvals
- CI phải pass (GitHub Actions xanh ✅)
- Merge chỉ thực hiện trong buổi họp Chủ nhật
```

#### Bước 3: Tạo PR Template `.github/pull_request_template.md`

```markdown
## Mô tả
<!-- Mô tả ngắn gọn thay đổi của bạn -->

## Tuần / Task
- Tuần: X
- Người thực hiện: [Tên]
- Branch: feature/tuan-X-...

## Loại thay đổi
- [ ] Feature mới
- [ ] Bug fix
- [ ] Documentation
- [ ] Refactoring
- [ ] Test

## Checklist
- [ ] Code compile thành công (`mvn compile`)
- [ ] Tất cả tests pass (`mvn test`)
- [ ] Đã viết test cho logic mới
- [ ] Code theo coding convention
- [ ] Đã tự review code trước khi tạo PR

## Screenshots (nếu có thay đổi UI)
<!-- Paste screenshots ở đây -->

## Ghi chú cho reviewer
<!-- Có gì cần lưu ý khi review? -->
```

#### Bước 4: Tạo Issue Templates `.github/ISSUE_TEMPLATE/bug_report.md`

```markdown
---
name: Bug Report
about: Báo lỗi tìm được
title: '[BUG] '
labels: bug
---

## Mô tả bug


## Cách tái hiện
1. 
2. 
3. 

## Kết quả mong đợi


## Kết quả thực tế


## Screenshots

## Môi trường
- OS: 
- JDK version: 
- IntelliJ version: 
```

#### Bước 5: Tạo file `README.md` (cập nhật)

Cập nhật `README.md` ở root với nội dung:

```markdown
# AuctionUET - Hệ thống đấu giá trực tuyến

## Thành viên nhóm
| Tên | Vai trò |
|-----|---------|
| Tạ Hữu Cường | Server Core & Database |
| Đào Đình Khánh | Networking & Protocol |
| Nguyễn Cao Công | Client GUI (JavaFX) |
| Ngô Duy Anh | Business Logic & Testing |

## Yêu cầu hệ thống
- JDK 21
- Maven 3.x
- Scene Builder (cho phát triển UI)

## Cách chạy
```bash
# Build toàn bộ project
mvn clean compile

# Chạy tests
mvn test

# Chạy Server
cd auction-server
mvn exec:java -Dexec.mainClass="com.auctionuet.server.ServerApp"

# Chạy Client
cd auction-client
mvn javafx:run
```

## Cấu trúc project
```
AuctionUET/
├── auction-server/    # Server module
├── auction-client/    # Client module (JavaFX)
├── pom.xml            # Parent POM
└── docs/              # Tài liệu
```
```

#### Bước 6: Setup Branch Protection (trên GitHub UI)

Vào GitHub → Repository Settings → Branches → Add rule cho `main`:
- ✅ Require a pull request before merging
- ✅ Require approvals: **2**
- ✅ Require status checks to pass (chọn CI workflow)
- ✅ Include administrators

> **Chụp screenshot** cấu hình này để chứng minh đã setup.

### ✅ TEST ĐẦU RA — Cách kiểm tra bài của Khánh

```bash
# Test 1: Kiểm tra file CI/CD tồn tại
ls .github/workflows/ci.yml
# ✅ PASS nếu: File tồn tại

# Test 2: Push lên GitHub và kiểm tra Actions
git push origin feature/tuan-1-khanh-git-cicd
# Vào GitHub → tab Actions → kiểm tra workflow chạy
# ✅ PASS nếu: Workflow chạy và hiện ✅ (hoặc nếu chưa có code để test, 
#              ít nhất workflow trigger được, có thể fail do chưa có pom.xml)

# Test 3: Kiểm tra PR template
# Vào GitHub → tạo Pull Request mới → kiểm tra template có tự hiện không
# ✅ PASS nếu: Template hiện ra với đầy đủ checklist

# Test 4: Kiểm tra CONTRIBUTING.md
cat CONTRIBUTING.md
# ✅ PASS nếu: Có đầy đủ các mục: Quy trình, Commit convention, Review rules

# Test 5: Kiểm tra Branch Protection
# Thử push trực tiếp vào main → phải bị từ chối
# ✅ PASS nếu: Không thể push trực tiếp vào main

# Test 6: Kiểm tra README.md
cat README.md
# ✅ PASS nếu: Có đầy đủ thông tin project, cách chạy, thành viên
```

### ❌ FAIL nếu:
- CI workflow không trigger khi push
- Không có PR template
- `CONTRIBUTING.md` thiếu quy trình hoặc commit convention
- Ai cũng push được trực tiếp vào `main`
- `README.md` không có hướng dẫn chạy project

---

## 👤 CÔNG — JavaFX Hello World + Scene Builder

```
Branch: feature/tuan-1-cong-javafx-hello
Deadline: Thứ 7 28/03 23:59
```

### 📝 Mô tả bài tập

Bạn chịu trách nhiệm tạo giao diện JavaFX đầu tiên cho dự án. Bài này giúp cả nhóm hiểu cách JavaFX + FXML + Controller hoạt động. Code phải có **comment giải thích từng dòng** vì bài này sẽ là tài liệu tham khảo cho cả nhóm.

### 📋 Yêu cầu chi tiết

#### Bước 1: Tạo file `MainView.fxml` bằng Scene Builder

**Đặt tại:** `auction-client/src/main/resources/com/auctionuet/client/views/MainView.fxml`

**Giao diện phải có:**
- **Label** id=`titleLabel`: text "🏠 AuctionUET", font-size 28px, bold, ở giữa (center)
- **Label** id=`subtitleLabel`: text "Hệ thống đấu giá trực tuyến", font-size 14px, phía dưới title
- **Button** id=`startButton`: text "🚀 Bắt đầu", width 200px, height 45px, ở giữa
- **Label** id=`statusLabel`: text rỗng "", font-size 12px, phía dưới button (dùng để hiện thông báo)
- **Label** id=`versionLabel`: text "v1.0", font-size 10px, góc dưới phải
- Layout: dùng **VBox** (vertical), alignment CENTER, spacing 20

**Yêu cầu FXML:**
- `fx:controller` phải trỏ đúng tới `com.auctionuet.client.view.MainController`
- Mỗi element có `fx:id` tương ứng
- Button có `onAction="#onStartClicked"`

#### Bước 2: Tạo file `MainController.java`

**Đặt tại:** `auction-client/src/main/java/com/auctionuet/client/controllers/MainController.java`

```java
package com.auctionuet.client.view;

// YÊU CẦU: Viết comment giải thích TẤT CẢ annotation và method

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

public class MainController {

    // 1. Khai báo các @FXML fields tương ứng với fx:id trong FXML
    @FXML
    private Label titleLabel;
    @FXML
    private Label subtitleLabel;
    @FXML
    private Button startButton;
    @FXML
    private Label statusLabel;
    @FXML
    private Label versionLabel;

    // 2. Method initialize() — tự động gọi sau khi FXML load xong
    @FXML
    public void initialize() {
        // Set initial styles hoặc logic cần thiết
        statusLabel.setText("");
        // Comment: giải thích tại sao cần initialize
    }

    // 3. Handler cho nút Start
    @FXML
    private void onStartClicked() {
        // Khi click:
        // - Đổi text statusLabel thành "✅ Chào mừng đến với AuctionUET!"
        // - Đổi style statusLabel: text fill = green
        // - Disable button (vì đã click rồi)
        // - Sau 2 giây, đổi text thành "Đang kết nối server..." (dùng Timeline hoặc PauseTransition)
    }
}
```

**Yêu cầu hành vi `onStartClicked()`:**
1. Click lần đầu → `statusLabel` hiện "✅ Chào mừng đến với AuctionUET!" (màu xanh)
2. Button bị disable (không click được nữa)
3. Sau 2 giây → `statusLabel` đổi thành "⏳ Đang kết nối server..." (màu cam)
4. Sau 3 giây nữa (tổng 5 giây) → `statusLabel` đổi thành "❌ Chưa có server (sẽ implement ở tuần 4)" (màu đỏ), button enable lại

> **Hint:** Dùng `javafx.animation.PauseTransition` hoặc `javafx.animation.Timeline`

#### Bước 3: Cập nhật `ClientApp.java` (load FXML)

Sửa `ClientApp.java` để load `MainView.fxml` thay vì tạo UI bằng code:

```java
@Override
public void start(Stage primaryStage) {
    // Load FXML
    FXMLLoader loader = new FXMLLoader(getClass().getResource("views/MainView.fxml"));
    Parent root = loader.load();
    
    // Tạo Scene
    Scene scene = new Scene(root, 800, 600);
    
    // Thêm CSS (nếu có)
    // scene.getStylesheets().add(getClass().getResource("styles/main.css").toExternalForm());
    
    primaryStage.setTitle("AuctionUET");
    primaryStage.setScene(scene);
    primaryStage.setMinWidth(400);
    primaryStage.setMinHeight(300);
    primaryStage.show();
}
```

#### Bước 4: Tạo file CSS cơ bản (BONUS, không bắt buộc)

**Đặt tại:** `auction-client/src/main/resources/com/auctionuet/client/styles/main.css`

```css
/* Style tối thiểu: */
.root {
    -fx-background-color: #1a1a2e;
}

#titleLabel {
    -fx-text-fill: #e94560;
    -fx-font-size: 28px;
}

#startButton {
    -fx-background-color: #e94560;
    -fx-text-fill: white;
    -fx-font-size: 16px;
    -fx-cursor: hand;
}

#startButton:hover {
    -fx-background-color: #c73e54;
}
```

#### Bước 5: Viết document giải thích

Tạo file `docs/javafx-guide.md` giải thích cho cả nhóm:

```markdown
# Hướng dẫn JavaFX cho nhóm

## 1. Cách FXML và Controller liên kết nhau
(Giải thích bằng lời của bạn, KHÔNG copy-paste)

## 2. Lifecycle của JavaFX Application  
(Application.launch() → start() → ...)

## 3. @FXML annotation hoạt động thế nào

## 4. Cách thêm event handler cho button

## 5. Cách thay đổi style của element từ code Java

## 6. Hướng dẫn dùng Scene Builder
(Từng bước: mở → kéo element → set fx:id → set controller → save)
```

### ✅ TEST ĐẦU RA — Cách kiểm tra bài của Công

```bash
# Test 1: Chạy ứng dụng
cd auction-client
mvn javafx:run
# ✅ PASS nếu: Cửa sổ mở ra, hiện label "AuctionUET" và button "Bắt đầu"

# Test 2: Click button
# Click nút "Bắt đầu"
# ✅ PASS nếu: 
#   - Label hiện "✅ Chào mừng đến với AuctionUET!" (màu xanh)
#   - Button bị disable
#   - Sau 2s → label đổi thành "⏳ Đang kết nối server..."
#   - Sau 5s → label đổi thành "❌ Chưa có server..."
#   - Button enable lại

# Test 3: Mở FXML trong Scene Builder
# Mở Scene Builder → File → Open → chọn MainView.fxml
# ✅ PASS nếu: Hiển thị đúng layout, tất cả elements đều thấy

# Test 4: Kiểm tra comments
# Mở MainController.java
# ✅ PASS nếu: Mỗi @FXML field và method đều có comment giải thích

# Test 5: Kiểm tra document
cat docs/javafx-guide.md
# ✅ PASS nếu: Có đầy đủ 6 mục, viết bằng lời của bạn (không copy-paste)

# Test 6: Code compile
mvn compile
# ✅ PASS nếu: BUILD SUCCESS
```

### ❌ FAIL nếu:
- `mvn javafx:run` bị lỗi, không mở được cửa sổ
- Click button không có phản ứng gì
- FXML không mở được trong Scene Builder
- Không có comment trong code
- Không có file `docs/javafx-guide.md`

---

## 👤 ANH — JUnit Test Setup + Coding Convention

```
Branch: feature/tuan-1-anh-test-convention
Deadline: Thứ 7 28/03 23:59
```

### 📝 Mô tả bài tập

Bạn chịu trách nhiệm thiết lập Testing framework và Coding Convention cho cả nhóm. Cả nhóm sẽ tuân theo convention bạn viết, và dùng pattern test mà bạn demo. Vì vậy phần này rất quan trọng — phải rõ ràng, dễ hiểu.

### 📋 Yêu cầu chi tiết

#### Bước 1: Đảm bảo JUnit 5 trong `pom.xml`

Kiểm tra parent `pom.xml` (do Cường tạo) đã có JUnit 5 chưa. Nếu chưa, thêm vào `<dependencyManagement>`:

```xml
<dependency>
    <groupId>org.junit.jupiter</groupId>
    <artifactId>junit-jupiter</artifactId>
    <version>5.10.2</version>
    <scope>test</scope>
</dependency>
```

Và trong `auction-server/pom.xml` thêm vào `<dependencies>`:
```xml
<dependency>
    <groupId>org.junit.jupiter</groupId>
    <artifactId>junit-jupiter</artifactId>
    <scope>test</scope>
</dependency>
```

#### Bước 2: Tạo class `Calculator.java`

**Đặt tại:** `auction-server/src/main/java/com/auctionuet/server/utils/Calculator.java`

```java
package com.auctionuet.server.utils;

/**
 * Lớp Calculator phục vụ luyện tập viết JUnit test.
 * 
 * LƯU Ý: Class này chỉ dùng để luyện tập.
 * Sẽ xóa sau khi cả nhóm đã hiểu cách viết test.
 */
public class Calculator {
    
    /**
     * Cộng hai số.
     * @param a số thứ nhất
     * @param b số thứ hai
     * @return tổng a + b
     */
    public int add(int a, int b) {
        return a + b;
    }
    
    /**
     * Trừ hai số.
     */
    public int subtract(int a, int b) {
        return a - b;
    }
    
    /**
     * Nhân hai số.
     */
    public int multiply(int a, int b) {
        return a * b;
    }
    
    /**
     * Chia hai số nguyên.
     * @throws ArithmeticException nếu b = 0
     */
    public int divide(int a, int b) {
        if (b == 0) {
            throw new ArithmeticException("Không thể chia cho 0");
        }
        return a / b;
    }
    
    /**
     * Tính giai thừa.
     * @param n số nguyên không âm
     * @throws IllegalArgumentException nếu n < 0
     * @return n!
     */
    public long factorial(int n) {
        if (n < 0) {
            throw new IllegalArgumentException("n phải >= 0");
        }
        if (n <= 1) return 1;
        long result = 1;
        for (int i = 2; i <= n; i++) {
            result *= i;
        }
        return result;
    }
    
    /**
     * Kiểm tra số nguyên tố.
     */
    public boolean isPrime(int n) {
        if (n < 2) return false;
        for (int i = 2; i * i <= n; i++) {
            if (n % i == 0) return false;
        }
        return true;
    }
}
```

#### Bước 3: Viết `CalculatorTest.java` — ĐÂY LÀ PHẦN QUAN TRỌNG NHẤT

**Đặt tại:** `auction-server/src/test/java/com/auctionuet/server/utils/CalculatorTest.java`

**YÊU CẦU:** Viết **TỐI THIỂU 15 test cases** bao phủ các trường hợp sau:

```java
package com.auctionuet.server.utils;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

// YÊU CẦU: Viết comment giải thích MỖI annotation và mỗi assertion method

@DisplayName("Calculator Test Suite")
class CalculatorTest {
    
    private Calculator calc;
    
    @BeforeEach  // Comment: giải thích @BeforeEach chạy khi nào
    void setUp() {
        calc = new Calculator();
    }
    
    // === ADD ===
    @Test
    @DisplayName("Cộng hai số dương")
    void testAddPositiveNumbers() { /* assertEquals(5, calc.add(2, 3)) */ }
    
    @Test
    @DisplayName("Cộng số dương và số âm")
    void testAddPositiveAndNegative() { /* ... */ }
    
    @Test
    @DisplayName("Cộng hai số 0")
    void testAddZeros() { /* ... */ }
    
    // === SUBTRACT ===
    @Test
    @DisplayName("Trừ hai số — kết quả dương")
    void testSubtractResultPositive() { /* ... */ }
    
    @Test
    @DisplayName("Trừ hai số — kết quả âm")
    void testSubtractResultNegative() { /* ... */ }
    
    // === MULTIPLY ===
    @Test
    @DisplayName("Nhân hai số dương")
    void testMultiplyPositive() { /* ... */ }
    
    @Test
    @DisplayName("Nhân với 0")
    void testMultiplyByZero() { /* ... */ }
    
    @Test
    @DisplayName("Nhân hai số âm — kết quả dương")
    void testMultiplyTwoNegatives() { /* ... */ }
    
    // === DIVIDE ===
    @Test
    @DisplayName("Chia bình thường")
    void testDivideNormal() { /* ... */ }
    
    @Test
    @DisplayName("Chia cho 0 — phải throw ArithmeticException")
    void testDivideByZero() {
        // Dùng assertThrows
        ArithmeticException ex = assertThrows(ArithmeticException.class, () -> {
            calc.divide(10, 0);
        });
        // Kiểm tra message
        assertTrue(ex.getMessage().contains("0"));
    }
    
    // === FACTORIAL ===
    @Test
    @DisplayName("Giai thừa 5! = 120")
    void testFactorial5() { /* assertEquals(120, calc.factorial(5)) */ }
    
    @Test
    @DisplayName("Giai thừa 0! = 1")
    void testFactorial0() { /* ... */ }
    
    @Test
    @DisplayName("Giai thừa số âm — throw IllegalArgumentException")
    void testFactorialNegative() { /* assertThrows(...) */ }
    
    // === PRIME ===
    @Test
    @DisplayName("2 là số nguyên tố")
    void testIsPrime2() { /* assertTrue(calc.isPrime(2)) */ }
    
    @Test
    @DisplayName("4 không phải số nguyên tố")
    void testIsNotPrime4() { /* assertFalse(calc.isPrime(4)) */ }
    
    @Test
    @DisplayName("Số âm không phải nguyên tố")
    void testIsPrimeNegative() { /* assertFalse(calc.isPrime(-5)) */ }
}
```

**YÊU CẦU BẮT BUỘC cho CalculatorTest:**
1. ≥ 15 test methods
2. Dùng `@DisplayName` cho MỖI test (tên tiếng Việt)
3. Dùng `@BeforeEach` để khởi tạo
4. Dùng ít nhất 5 assertion methods khác nhau: `assertEquals`, `assertTrue`, `assertFalse`, `assertNotNull`, `assertThrows`
5. Comment giải thích TỪNG annotation: `@Test`, `@BeforeEach`, `@DisplayName`, `@BeforeAll`, `@AfterEach`
6. Nhóm tests theo category dùng comment block (`// === ADD ===`)

#### Bước 4: Tạo file `STYLE_GUIDE.md`

**Đặt tại:** `docs/STYLE_GUIDE.md`

**Tham khảo:** https://google.github.io/styleguide/javaguide.html

**Nội dung BẮT BUỘC (viết bằng tiếng Việt, dễ hiểu cho nhóm):**

```markdown
# 📏 Coding Convention — AuctionUET

## 1. Đặt tên (Naming Convention)

### Classes & Interfaces
- Dùng PascalCase
- ✅ `UserService`, `AuctionManager`, `BidTransaction`
- ❌ `userService`, `auction_manager`, `bidtransaction`

### Methods & Variables
- Dùng camelCase
- ✅ `getUsername()`, `placeBid()`, `currentHighestBid`
- ❌ `GetUsername()`, `place_bid()`, `CurrentHighestBid`

### Constants
- Dùng UPPER_SNAKE_CASE
- ✅ `MAX_BID_AMOUNT`, `DEFAULT_PORT`
- ❌ `maxBidAmount`, `default_port`

### Packages
- Dùng lowercase, không underscore
- ✅ `com.auctionuet.server.controller`
- ❌ `com.AuctionUET.Server.Controller`

## 2. Format code

### Indentation
- Dùng **4 spaces** (KHÔNG dùng tab)
- IntelliJ: Settings → Editor → Code Style → Java → Use tab character: unchecked

### Độ dài dòng
- Tối đa **100 ký tự** / dòng
- Nếu dài hơn → xuống dòng

### Braces
- Opening brace `{` cùng dòng
- ✅ 
```java
if (condition) {
    doSomething();
}
```
- ❌
```java
if (condition)
{
    doSomething();
}
```

## 3. Javadoc

### Khi nào PHẢI viết Javadoc:
- Tất cả **public** class
- Tất cả **public** method
- Method có logic phức tạp

### Format:
```java
/**
 * Đặt giá cho phiên đấu giá.
 * 
 * @param auctionId ID phiên đấu giá
 * @param bidderId ID người đấu giá
 * @param amount Số tiền đặt
 * @return BidTransaction đã lưu
 * @throws InvalidBidException nếu giá thấp hơn giá hiện tại
 * @throws AuctionClosedException nếu phiên đã kết thúc
 */
public BidTransaction placeBid(String auctionId, String bidderId, double amount) {
    // ...
}
```

## 4. Tổ chức file

### Import
- Không dùng wildcard import (`import java.util.*`)
- ✅ `import java.util.List;`
- ❌ `import java.util.*;`
- Thứ tự: java → javax → third-party → project

### Package structure
```
com.auctionuet.server/
├── controller/     # Xử lý request
├── service/        # Business logic
├── model/          # Domain classes
├── dao/            # Data access
├── exception/      # Custom exceptions
└── utils/          # Utility classes
```

## 5. Test Convention

### Đặt tên test
- Format: `test[Method]_[Scenario]_[ExpectedResult]`
- ✅ `testPlaceBid_BidTooLow_ThrowsException`
- ✅ `testLogin_ValidCredentials_ReturnsUser`
- Hoặc dùng `@DisplayName` với tên tiếng Việt

### Test structure (AAA Pattern)
```java
@Test
void testExample() {
    // Arrange - Chuẩn bị dữ liệu
    Calculator calc = new Calculator();
    
    // Act - Thực hiện action
    int result = calc.add(2, 3);
    
    // Assert - Kiểm tra kết quả
    assertEquals(5, result);
}
```

## 6. Các quy tắc khác

- **Không commit code đã comment out** (xóa đi hoặc dùng feature flag)
- **Không dùng `System.out.println` cho debugging** (sẽ dùng Logger từ tuần 9)
- **Mỗi class chỉ làm 1 việc** (Single Responsibility)
- **Method ngắn** (< 30 dòng, nếu dài hơn → tách method)
```

### ✅ TEST ĐẦU RA — Cách kiểm tra bài của Anh

```bash
# Test 1: Chạy tests
cd auction-server
mvn test
# ✅ PASS nếu:
#   - Tests run: ≥ 15
#   - Failures: 0
#   - Errors: 0

# Test 2: Kiểm tra test coverage
# Đếm số test methods trong CalculatorTest.java
grep -c "@Test" src/test/java/com/auctionuet/server/utils/CalculatorTest.java
# ✅ PASS nếu: ≥ 15

# Test 3: Kiểm tra divide-by-zero test
mvn test -Dtest="CalculatorTest#testDivideByZero"
# ✅ PASS nếu: Test pass, ArithmeticException được thrown

# Test 4: Kiểm tra @DisplayName
# Chạy mvn test và xem output
# ✅ PASS nếu: Mỗi test có tên tiếng Việt rõ ràng trong output

# Test 5: Kiểm tra STYLE_GUIDE.md
cat docs/STYLE_GUIDE.md
# ✅ PASS nếu: Có đầy đủ 6 mục:
#   1. Naming convention (với ví dụ)
#   2. Format code (indentation, braces)
#   3. Javadoc
#   4. Tổ chức file
#   5. Test convention (AAA pattern)
#   6. Các quy tắc khác

# Test 6: Kiểm tra comments trong test
# Mở CalculatorTest.java
# ✅ PASS nếu: Mỗi annotation (@Test, @BeforeEach, @DisplayName) đều có comment giải thích

# Test 7: Kiểm tra đa dạng assertions
grep -E "assert(Equals|True|False|NotNull|Throws)" src/test/java/com/auctionuet/server/utils/CalculatorTest.java | wc -l
# ✅ PASS nếu: Có ≥ 5 loại assertion khác nhau
```

### ❌ FAIL nếu:
- `mvn test` fail hoặc có < 15 test cases
- Không có test cho chia 0 / giai thừa số âm (edge cases)
- Thiếu `@DisplayName` cho tests
- Không có comment giải thích annotations
- `STYLE_GUIDE.md` thiếu mục hoặc không có ví dụ cụ thể

---

## 🔄 QUY TRÌNH KIỂM TRA CHÉO — CHỦ NHẬT 29/03/2026

### Phân công review

| Người làm | Reviewer 1 | Reviewer 2 |
|-----------|------------|------------|
| **Cường** (Maven Setup) | Khánh | Anh |
| **Khánh** (Git/CI) | Cường | Công |
| **Công** (JavaFX) | Anh | Cường |
| **Anh** (JUnit/Convention) | Công | Khánh |

### Quy trình review (mỗi người ~30 phút)

#### Bước 1: Pull code (5 phút)
```bash
git fetch origin
git checkout feature/tuan-1-[ten-nguoi-can-review]
```

#### Bước 2: Chạy test đầu ra (10 phút)
- Chạy từng lệnh test trong phần "TEST ĐẦU RA" ở trên
- Ghi lại kết quả: ✅ PASS hoặc ❌ FAIL

#### Bước 3: Đọc code (10 phút)
- Đọc từng file, hiểu logic
- Chuẩn bị để **giải thích lại** code này cho nhóm

#### Bước 4: Viết review (5 phút)
Tạo comment trên Pull Request với template:

```markdown
## Review — [Tên reviewer] → [Tên người code]

### Test Results
- [ ] Test 1: ... → ✅/❌
- [ ] Test 2: ... → ✅/❌
- [ ] ...

### Code Quality
- Naming convention: OK / Cần sửa (mô tả)
- Comments: Đầy đủ / Thiếu
- Structure: Tốt / Cần refactor

### Questions (câu hỏi khi đọc code)
1. ...
2. ...

### Verdict
- [ ] ✅ Approve
- [ ] 🔄 Request changes (ghi rõ cần sửa gì)
```

---

## 📝 CHECKLIST TỔNG HỢP — CHECK TRƯỚC KHI NỘP

### Cho tất cả mọi người:
- [ ] Đã cài JDK 21, IntelliJ, Scene Builder, Git, Maven
- [ ] Đã hoàn thành bài thực hành Git (push branch `practice/tuan-1-[TEN]`)
- [ ] Đã đọc tài liệu Git, Maven, JavaFX
- [ ] Có thể trả lời các câu hỏi trong phần tự học
- [ ] Code đã push lên branch riêng trước deadline

### Cho Cường:
- [ ] `mvn compile` thành công (cả 2 modules)
- [ ] `mvn test` thành công (≥ 3 tests pass)
- [ ] `mvn clean package` thành công
- [ ] ServerApp chạy được, in ra đúng message
- [ ] ClientApp chạy được, hiện cửa sổ JavaFX
- [ ] `.gitignore` đầy đủ
- [ ] Cấu trúc thư mục đúng Maven standard

### Cho Khánh:
- [ ] CI workflow trigger khi push
- [ ] PR template hiện khi tạo PR
- [ ] `CONTRIBUTING.md` đầy đủ quy trình
- [ ] `README.md` có hướng dẫn chạy project
- [ ] Branch protection đã setup (có screenshot)
- [ ] Issue template tồn tại

### Cho Công:
- [ ] `mvn javafx:run` mở được cửa sổ
- [ ] Click button có đúng 3 trạng thái (xanh → cam → đỏ)
- [ ] FXML mở được trong Scene Builder
- [ ] Có comment giải thích trong code
- [ ] Có file `docs/javafx-guide.md` đầy đủ 6 mục
- [ ] CSS cơ bản (bonus)

### Cho Anh:
- [ ] `mvn test` → ≥ 15 tests pass, 0 failures
- [ ] Có test chia cho 0 (assertThrows)
- [ ] Có test giai thừa số âm
- [ ] `@DisplayName` tiếng Việt cho mỗi test
- [ ] Comment giải thích mỗi annotation
- [ ] `docs/STYLE_GUIDE.md` đầy đủ 6 mục + ví dụ
- [ ] Dùng ≥ 5 loại assertion khác nhau

---

## ⏰ TIMELINE GỢI Ý

| Ngày | Việc cần làm |
|------|-------------|
| **T2 23/03 (Tối)** | Kick-off meeting, phân công, giải đáp |
| **T3 24/03** | Cài đặt môi trường, làm phần tự học |
| **T4 25/03** | Hoàn thành phần tự học, bắt đầu code bài cá nhân |
| **T5 26/03 (Tối)** | Mid-week check-in: báo tiến độ, hỏi nếu stuck |
| **T6 27/03** | Hoàn thiện code, viết test, push code |
| **T7 28/03** | **DEADLINE** — push code cuối cùng trước 23:59 |
| **CN 29/03 (Sáng)** | Review chéo, merge code, demo cho nhau |

> [!WARNING]
> **Không push code sau 23:59 Thứ 7.** Code nào push sau deadline sẽ không được tính vào review Chủ nhật.

> [!TIP]
> Nếu gặp khó khăn, **HỎI NGAY** trên nhóm chat. Đừng đợi đến Chủ nhật mới nói. Mọi người đều đang học, không ai đánh giá ai cả. 💪
