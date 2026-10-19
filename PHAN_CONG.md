# BẢNG PHÂN CÔNG ĐỒ ÁN NHÓM CSD201 - FA26
## Đề tài 2: Chỉ mục tra cứu bằng Hash Table (tombstone, rebuild và Robin Hood hashing)

> Căn cứ theo mục 3.4 và mục 7 của Quy định chung đồ án nhóm CSD201 FA26.

| STT | MSSV | Họ và tên | Cặp | Module & Test phụ trách | RQ tham gia phân tích | Phần Paper viết / Review |
|:---:|:---:|:---|:---:|:---|:---:|:---|
| 1 | QExxxxxx | [Họ tên SV 1] (Trưởng nhóm) | Cặp 1 | Separate Chaining (`sc`), Generator khóa/truy vấn, Runner | RQ1 | IV. Experimental Setup, Tổng hợp & Review |
| 2 | QExxxxxx | [Họ tên SV 2] | Cặp 1 | Khung Test Oracle, Benchmark Runner CLI, Đo đạc RQ1 | RQ1 | I. Introduction, V-A. Results & Discussion (RQ1) |
| 3 | QExxxxxx | [Họ tên SV 3] | Cặp 2 | Linear Probing Tombstone (`lp-tomb`), Rebuild ($\tau$) | RQ2 | III. Methods (LP & Rebuild), V-B. Results & Discussion (RQ2) |
| 4 | QExxxxxx | [Họ tên SV 4] | Cặp 2 | Workload xóa–thêm (C chu kỳ), Bảng đối chứng (Clean Table) | RQ2 | II. Background (LP & Tombstone), Threats to Validity |
| 5 | QExxxxxx | [Họ tên SV 5] | Cặp 3 | Robin Hood (`rh`), Robin Hood không dừng sớm (`rh-noearly`) | RQ3 | III. Methods (Robin Hood & DIB), V-C. Results & Discussion (RQ3) |
| 6 | QExxxxxx | [Họ tên SV 6] | Cặp 3 | Linear Probing Backshift (`lp-backshift`), Script vẽ biểu đồ | RQ3 | II. Background (Robin Hood), VII. Conclusion |

---

### Quy ước hoạt động nhóm
1. **Luân phiên Cài đặt & Kiểm thử**: Trong mỗi cặp, một thành viên cài đặt thuật toán thì thành viên còn lại viết/chạy bộ test kiểm thử và ngược lại.
2. **Review chéo**:
   - Cặp 1 review module của Cặp 2.
   - Cặp 2 review module của Cặp 3.
   - Cặp 3 review module của Cặp 1.
3. **Liêm chính học thuật**: Toàn bộ số liệu trong báo cáo và paper phải được tái lập từ các file CSV gốc đo trên máy thực tế, có seed và checksum xác thực.
