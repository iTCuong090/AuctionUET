# Hướng dẫn đóng góp - AuctionUET
### 1. Tạo branch
- Từ `main`, tạo branch mới theo format: `feat/tuan-X-ten-nguoi-mo-ta`
- Ví dụ: `feat/tuan-1-cuong-maven-setup`

### 2. Coding
- Tuân thủ Google Java Style Guide
- Viết comment tiếng Việt
- Mỗi commit phải có message rõ ràng theo Conventional Commits:
    - `feat: thêm class User`
    - `fix: sửa lỗi chia cho 0 trong Calculator`
    - `docs: thêm Javadoc cho class Auction`
    - `test: thêm test cho UserService`
    - `refactor: tách method validateBid`

### 3. Push & Pull Request
- Push branch lên GitHub: `git push origin feature/tuan-X-...`
- Tạo Pull Request vào `main`
- Mô tả PR rõ ràng (dùng PR template)

### 4. Review
- Khi review, kiểm tra:
    - Code chạy được không?
    - Logic đúng không?
    - Có viết test không?
    - Naming convention OK không?
    - Có edge case nào bỏ sót?

### 5. Merge
- CI phải pass (GitHub Actions xanh ✅)
- Merge chỉ thực hiện trong buổi họp Chủ nhật

