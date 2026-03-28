/*
@Test
// Đánh dấu đây là một bài kiểm tra. Không có cái này máy sẽ không chạy hàm.

@BeforeEach
// Chạy trước mỗi bài test. Dùng để reset dữ liệu, làm sạch máy tính trước khi test mới.

@AfterEach
// Chạy sau mỗi bài test. Dùng để dọn dẹp hoặc ghi nhật ký sau khi test xong một nội dung.

@BeforeAll
// Chạy duy nhất 1ần trước tất cả. Dùng để mở kết nối nặng hoặc bật nguồn hệ thống (phải đi kèm static).

@AfterAll
// Chạy duy nhất 1ần sau khi xong hết. Dùng để đóng kết nối, tắt nguồn hoặc giải phóng bộ nhớ.

@DisplayName
// Đặt tên hiển thị bằng tiếng Việt có dấu cho bài test để báo cáo dễ đọc hơn.
*/

package com.auctionuet.server.utils;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

// YÊU CẦU: Viết comment giải thích MỖI annotation và mỗi assertion method

@DisplayName("Calculator Test Suite")
class CalculatorTest {

    private Calculator calc;

    @BeforeEach
    void setUp() {
        calc = new Calculator();
    }

    // === ADD ===
    @Test // Đánh dấu đây là 1 hàm để kiểm tra
    @DisplayName("Cộng hai số dương")
    void testAddPositiveNumbers() {assertEquals(5, calc.add(2, 3));}

    @Test
    @DisplayName("Cộng số dương và số âm")
    void testAddPositiveAndNegative() { assertEquals(-5,calc.add(-7,2)); }

    @Test
    @DisplayName("Cộng hai số 0")
    void testAddZeros() { assertEquals(0,calc.add(0,0)); }

    // === SUBTRACT ===
    @Test
    @DisplayName("Trừ hai số — kết quả dương")
    void testSubtractResultPositive() { assertEquals(3,calc.subtract(5,2));  }

    @Test
    @DisplayName("Trừ hai số — kết quả âm")
    void testSubtractResultNegative() { assertEquals(-1,calc.subtract(2,3)); }

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
    void testFactorial5() { assertEquals(120, calc.factorial(5));}

    @Test
    @DisplayName("Giai thừa 0! = 1")
    void testFactorial0() { /* ... */ }

    @Test
    @DisplayName("Giai thừa số âm — throw IllegalArgumentException")
    void testFactorialNegative() {assertThrows(IllegalArgumentException.class,()->{calc.factorial(-5);}); }

    // === PRIME ===
    @Test
    @DisplayName("2 là số nguyên tố")
    void testIsPrime2() { assertTrue(calc.isPrime(2)); }

    @Test
    @DisplayName("4 không phải số nguyên tố")
    void testIsNotPrime4() { assertFalse(calc.isPrime(4)) ; }

    @Test
    @DisplayName("Số âm không phải nguyên tố")
    void testIsPrimeNegative() { assertFalse(calc.isPrime(-5));}
}