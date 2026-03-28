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
