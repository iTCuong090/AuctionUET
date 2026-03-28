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
        return a-b;
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