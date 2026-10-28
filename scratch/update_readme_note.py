with open('README.md', 'r', encoding='utf-8') as f:
    text = f.read()

target = "## 5. Giải thích các trường dữ liệu CSV (Bảng 4)"
replacement = """## 5. Giải thích các trường dữ liệu CSV (Bảng 4)

> **Ghi chú về Schema và Dữ liệu Sandbox:** Toàn bộ các file CSV thực nghiệm chính thức từ phòng máy lab (`rq1_runner_results.csv`, `rq2_results.csv`, `rq3_results.csv`,...) đều tuân thủ nghiêm ngặt đầy đủ 21 cột (bao gồm `build_ms`). Dữ liệu sandbox thiếu cột `build_ms` do môi trường ảo không đo được thời gian dựng bảng (chỉ có 20 cột); nhóm lưu trữ riêng file này tại `results/notes/` và không điền số 0 giả tạo để đảm bảo tính trung thực học thuật tuyệt đối."""

assert target in text, "Target header not found in README.md"
new_text = text.replace(target, replacement, 1)

with open('README.md', 'w', encoding='utf-8') as f:
    f.write(new_text)

print("Updated README.md successfully!")
