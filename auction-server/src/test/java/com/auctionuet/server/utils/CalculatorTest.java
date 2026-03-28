package com.auctionuet.server.utils;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

// YÊU CẦU: Viết comment giải thích MỖI annotation và mỗi assertion method

@DisplayName("Calculator Test Suite")
class CalculatorTest {

    private Calculator calc;

    @BeforeEach  // Comment: Beforeach chạy ngay trước khi gặp 1 hàm test
    void setUp() {
        calc = new Calculator();
    }

    // === ADD ===
    @Test
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