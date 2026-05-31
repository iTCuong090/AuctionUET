# AuctionUET Report

Thư mục này chứa báo cáo LaTeX và các placeholder ảnh cho biểu đồ.

## Biên dịch

```powershell
cd report
xelatex -interaction=nonstopmode -halt-on-error main.tex
xelatex -interaction=nonstopmode -halt-on-error main.tex
```

File nộp cuối cùng: `AuctionUET_Report.pdf`.

## Ảnh cần ghi đè sau khi render Mermaid

Render các file `.mmd` trong `mermaid/`, rồi ghi đè đúng tên ảnh sau:

- `images/system_architecture.png`
- `images/domain_class_interfaces.png`
- `images/realtime_bidding_flow.png`

Giữ nguyên tên file để `main.tex` không cần chỉnh lại.
