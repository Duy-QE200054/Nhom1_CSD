import os
import subprocess

html = """<!DOCTYPE html>
<html>
<head>
<meta charset="utf-8">
<title>Slides Bảo Vệ CSD201 - Đề Tài 2 - Nhóm 1</title>
<style>
  @page {
    size: 297mm 210mm; /* A4 Landscape 16:9 like presentation */
    margin: 0;
  }
  body {
    margin: 0;
    padding: 0;
    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;
    color: #1a202c;
    background: #f7fafc;
  }
  .slide {
    width: 297mm;
    height: 210mm;
    box-sizing: border-box;
    padding: 24mm 28mm;
    page-break-after: always;
    background: #ffffff;
    position: relative;
    display: flex;
    flex-direction: column;
    justify-content: flex-start;
  }
  .slide-header {
    border-bottom: 3px solid #2b6cb0;
    padding-bottom: 8px;
    margin-bottom: 16px;
  }
  .slide-header h1 {
    margin: 0;
    font-size: 24px;
    color: #2b6cb0;
    text-transform: uppercase;
    letter-spacing: 0.5px;
  }
  .slide-header .meta {
    font-size: 13px;
    color: #718096;
    margin-top: 4px;
    font-weight: 500;
  }
  .slide-content {
    flex: 1;
    font-size: 14px;
    line-height: 1.55;
  }
  .slide-footer {
    position: absolute;
    bottom: 12mm;
    left: 28mm;
    right: 28mm;
    display: flex;
    justify-content: space-between;
    font-size: 11px;
    color: #a0aec0;
    border-top: 1px solid #e2e8f0;
    padding-top: 6px;
  }
  h2 { font-size: 17px; color: #2d3748; margin: 12px 0 6px 0; }
  h3 { font-size: 15px; color: #4a5568; margin: 8px 0 4px 0; }
  p { margin: 6px 0; }
  ul, ol { margin: 6px 0 10px 22px; padding: 0; }
  li { margin-bottom: 4px; }
  table { border-collapse: collapse; width: 100%; margin: 10px 0; font-size: 12.5px; }
  th, td { border: 1px solid #cbd5e0; padding: 6px 10px; text-align: left; }
  th { background-color: #ebf8ff; color: #2b6cb0; font-weight: 700; }
  tr:nth-child(even) { background-color: #f7fafc; }
  .grid-2 { display: grid; grid-template-columns: 1fr 1fr; gap: 20px; }
  .grid-3 { display: grid; grid-template-columns: 1fr 1fr 1fr; gap: 16px; }
  .card { background: #edf2f7; border-radius: 6px; padding: 12px 14px; margin-bottom: 10px; border-left: 4px solid #3182ce; }
  .card-title { font-weight: bold; color: #2b6cb0; margin-bottom: 4px; font-size: 14px; }
  .highlight { color: #c53030; font-weight: bold; }
  .badge { background: #bee3f8; color: #2b6cb0; padding: 2px 6px; border-radius: 4px; font-size: 11px; font-weight: bold; }
  code { background: #edf2f7; padding: 2px 4px; border-radius: 3px; font-family: Consolas, monospace; font-size: 12px; }
</style>
</head>
<body>

  <!-- SLIDE 1 -->
  <div class="slide">
    <div class="slide-header">
      <h1>Slide 1: Giới Thiệu Đề Tài & Phân Vai 6 Thành Viên</h1>
      <div class="meta">Thời lượng: 30s | Người trình bày: SV 1 (Nguyễn Duy - Cặp 1)</div>
    </div>
    <div class="slide-content">
      <div class="card">
        <div class="card-title">Đề tài 2 - CSD201 (FA26) - Nhóm 1</div>
        <p><strong>Nghiên cứu và Đánh giá Hiệu năng Chỉ mục Tra cứu bằng Hash Table: Tombstone, Rebuild và Robin Hood Hashing</strong></p>
        <p>Môi trường đo chính thức: <code>PC_LAB_01</code> (Intel Core i5-11500H @ 2.90GHz, 32GB RAM, Windows 11 Pro, JDK 24, <code>-Xmx2g</code>)</p>
      </div>
      <h2>Phân Vai & Trách Nhiệm 3 Cặp Nghiên Cứu (6 Thành Viên)</h2>
      <div class="grid-3">
        <div class="card" style="border-left-color: #3182ce;">
          <div class="card-title">Cặp 1 (C1) - RQ1: SC vs LP</div>
          <p><strong>Nguyễn Duy (SV 1)</strong>: Cài đặt Separate Chaining, Multi-process Runner CLI, Trình bày Slide 1 & 5.</p>
          <p><strong>Nguyễn Tuấn Hưng (SV 2)</strong>: Mô hình hóa bộ nhớ Java Object (32B vs 13B), Cache L1/L2, Trình bày Slide 2 & 5.</p>
        </div>
        <div class="card" style="border-left-color: #38a169;">
          <div class="card-title">Cặp 2 (C2) - RQ2: LP-Tomb & Rebuild</div>
          <p><strong>Lê Phước Anh Quân (SV 3)</strong>: Cài đặt LP-Tombstone, Rebuild theo &tau;, Trình bày Slide 3 & 6.</p>
          <p><strong>Nguyễn Viết Cường (SV 4)</strong>: Động học suy kiệt ô rỗng, Clean Table đối chứng, Trình bày Slide 4 & 6.</p>
        </div>
        <div class="card" style="border-left-color: #805ad5;">
          <div class="card-title">Cặp 3 (C3) - RQ3: Robin Hood & Ablation</div>
          <p><strong>Nguyễn Đình Tiến (SV 5)</strong>: Cài đặt Robin Hood, Early Exit, Backshift (Knuth Algorithm R), Trình bày Slide 7.</p>
          <p><strong>Lê Hoàng Cầu (SV 6)</strong>: Micro-profiling 10⁴ ops, Đột biến Rebuild, Ma trận khuyến nghị, Trình bày Slide 8.</p>
        </div>
      </div>
    </div>
    <div class="slide-footer">
      <span>Nhóm 1 - CSD201 (FA26)</span>
      <span>Trang 1 / 8</span>
    </div>
  </div>

  <!-- SLIDE 2 -->
  <div class="slide">
    <div class="slide-header">
      <h1>Slide 2: Bối Cảnh Ứng Dụng & 3 Câu Hỏi Nghiên Cứu (RQs)</h1>
      <div class="meta">Thời lượng: 30s | Người trình bày: SV 2 (Nguyễn Tuấn Hưng - Cặp 1)</div>
    </div>
    <div class="slide-content">
      <h2>1. Bối Cảnh Thực Tiễn: High-Churn In-Memory Key-Value Index</h2>
      <ul>
        <li>Các hệ thống giao dịch vi mô (HFT, sàn TMĐT) xử lý hàng triệu truy vấn/giây với yêu cầu khắt khe về độ trễ cực đại (p99/max latency).</li>
        <li><strong>Đặc thù khối lượng công việc (Workload):</strong> Xóa-thêm liên tục (high-churn); đa số tra cứu là <span class="highlight">tra cứu thất bại (Miss query / Absent keys)</span> để kiểm tra tính duy nhất hoặc chống gian lận.</li>
      </ul>
      <h2>2. Ba Câu Hỏi Nghiên Cứu Trọng Tâm (Research Questions)</h2>
      <div class="card" style="border-left-color: #3182ce;">
        <strong>RQ1 (C1):</strong> Dưới áp lực tăng trưởng hệ số tải &alpha; và quy mô bảng M, SC hay LP chiếm ưu thế? Nghịch lý giữa số bước dò lý thuyết và thời gian thực thi (The Java Memory Gap) được giải thích ra sao?
      </div>
      <div class="card" style="border-left-color: #38a169;">
        <strong>RQ2 (C2):</strong> Khi chịu chu kỳ churn kéo dài không tái cấu trúc (&tau;=&infin;), ô rỗng suy kiệt khiến hiệu năng tra cứu trượt sụp đổ thế nào? Tại sao Rebuild theo ngưỡng &tau;&middot;M là bắt buộc?
      </div>
      <div class="card" style="border-left-color: #805ad5;">
        <strong>RQ3 (C3):</strong> Robin Hood Hashing và Xóa dịch lùi (Knuth Algorithm R) giải quyết triệt để độ trễ đuôi (tail latency) ra sao? Giá trị độc lập của từng cơ chế: Backshift, DIB swap, và Early Exit là bao nhiêu?
      </div>
    </div>
    <div class="slide-footer">
      <span>Nhóm 1 - CSD201 (FA26)</span>
      <span>Trang 2 / 8</span>
    </div>
  </div>

  <!-- SLIDE 3 -->
  <div class="slide">
    <div class="slide-header">
      <h1>Slide 3: Thiết Kế Kiến Trúc & 5 Cấu Hình Thuật Toán</h1>
      <div class="meta">Thời lượng: 30s | Người trình bày: SV 3 (Lê Phước Anh Quân - Cặp 2)</div>
    </div>
    <div class="slide-content">
      <h2>1. Hàm Băm Chuẩn Fibonacci Hash (Biến thể A: long key, p bits)</h2>
      <p>Áp dụng hàm băm nhân Fibonacci chuẩn xác cho toàn bộ 5 thuật toán: <code>h(k) = (k * 11400714819323198485L) >>> (64 - p)</code>, phân phối đồng đều cực đại trên các bit có nghĩa cao.</p>
      <h2>2. Ma Trận 5 Cấu Hình Cài Đặt Nghiên Cứu</h2>
      <table>
        <tr>
          <th>Thuật toán</th>
          <th>Cấu trúc lưu trữ</th>
          <th>Cơ chế xóa</th>
          <th>Cơ chế dừng sớm</th>
          <th>Chi phí bộ nhớ</th>
        </tr>
        <tr>
          <td><strong>sc</strong></td>
          <td>Mảng con trỏ <code>Node[]</code> phân tán trên Heap</td>
          <td>Cắt nối con trỏ liên kết</td>
          <td>Dừng khi hết chuỗi liên kết</td>
          <td>32 bytes/node + 4B slot</td>
        </tr>
        <tr>
          <td><strong>lp-tomb</strong></td>
          <td>3 mảng phẳng: <code>keys[]</code>, <code>values[]</code>, <code>state[]</code></td>
          <td>Ghi <code>TOMB=2</code>; Rebuild khi tombstones &ge; &tau;&middot;M</td>
          <td>Dừng khi gặp ô EMPTY (0)</td>
          <td>13 bytes/slot</td>
        </tr>
        <tr>
          <td><strong>lp-backshift</strong></td>
          <td>3 mảng phẳng: <code>keys[]</code>, <code>values[]</code>, <code>state[]</code></td>
          <td><strong>Xóa dịch lùi Knuth Algorithm R (0 tombstone)</strong></td>
          <td>Dừng khi gặp ô EMPTY (0)</td>
          <td>13 bytes/slot</td>
        </tr>
        <tr>
          <td><strong>rh</strong></td>
          <td>4 mảng phẳng: bổ sung <code>int[] dib</code></td>
          <td>Xóa dịch lùi cập nhật DIB (0 tombstone)</td>
          <td><strong>Early Exit khi DIB_probe > DIB_slot</strong></td>
          <td>17 bytes/slot</td>
        </tr>
        <tr>
          <td><strong>rh-noearly</strong></td>
          <td>4 mảng phẳng: bổ sung <code>int[] dib</code></td>
          <td>Xóa dịch lùi cập nhật DIB</td>
          <td>Dò đến EMPTY (tắt dừng sớm để bóc tách)</td>
          <td>17 bytes/slot</td>
        </tr>
      </table>
    </div>
    <div class="slide-footer">
      <span>Nhóm 1 - CSD201 (FA26)</span>
      <span>Trang 3 / 8</span>
    </div>
  </div>

  <!-- SLIDE 4 -->
  <div class="slide">
    <div class="slide-header">
      <h1>Slide 4: Phương Pháp Thực Nghiệm & Kiểm Thử Hợp Đồng</h1>
      <div class="meta">Thời lượng: 30s | Người trình bày: SV 4 (Nguyễn Viết Cường - Cặp 2)</div>
    </div>
    <div class="slide-content">
      <div class="grid-2">
        <div>
          <h2>1. Kiểm Thử Oracle 1,000,000 Thao Tác</h2>
          <ul>
            <li>Kiểm thử so khớp nghiêm ngặt 1 triệu thao tác ngẫu nhiên (40% put, 40% get, 20% remove) giữa mọi cài đặt và <code>java.util.HashMap</code>.</li>
            <li>Tất cả 5 cấu hình vượt qua 100% với <strong>0 lỗi sai lệch (Match 100.0%)</strong>.</li>
            <li>6 ca test hợp đồng bắt buộc: xử lý chuỗi đụng độ dài, xóa giữa chuỗi, khóa âm/khóa biên, và phép toán chỉ số vòng <code>(i + 1) & (M - 1)</code>.</li>
          </ul>
        </div>
        <div>
          <h2>2. Phương Pháp Đo Đạc Khoa Học (Mốc M5)</h2>
          <ul>
            <li><strong>Tiến trình JVM độc lập:</strong> Mỗi cấu hình chạy trong JVM riêng biệt với cờ <code>-Xmx2g -XX:+UseG1GC</code>, warm-up 1000 lượt, timeout an toàn 60 giây.</li>
            <li><strong>Tách bạch 2 chế độ:</strong> Chế độ <code>--mode count</code> đo số bước dò toán học; chế độ <code>--mode time</code> đo thời gian thực tế (wall-clock latency).</li>
            <li><strong>Schema 21 cột đồng bộ:</strong> Chuẩn hóa ghi nhận <code>build_ms</code>, <code>elapsed_ms</code>, <code>heap_peak_bytes</code>, <code>output_check</code> chống tối ưu DCE của JIT compiler.</li>
          </ul>
        </div>
      </div>
    </div>
    <div class="slide-footer">
      <span>Nhóm 1 - CSD201 (FA26)</span>
      <span>Trang 4 / 8</span>
    </div>
  </div>

  <!-- SLIDE 5 -->
  <div class="slide">
    <div class="slide-header">
      <h1>Slide 5: Kết Quả RQ1 - Separate Chaining vs Linear Probing</h1>
      <div class="meta">Thời lượng: 45s | Người trình bày: SV 1 & SV 2 (Nguyễn Duy & Nguyễn Tuấn Hưng - Cặp 1)</div>
    </div>
    <div class="slide-content">
      <h2>1. Đối Chiếu Thực Nghiệm Phòng Lab (PC_LAB_01) Với Tiệm Cận Knuth (M = 2²⁰)</h2>
      <table>
        <tr>
          <th>Hệ số tải &alpha;</th>
          <th>Knuth LP Hit</th>
          <th>Lab LP Hit</th>
          <th>Knuth SC Hit</th>
          <th>Lab SC Hit</th>
          <th>Lab LP Time</th>
          <th>Lab SC Time</th>
          <th>So sánh hiệu năng</th>
        </tr>
        <tr>
          <td><strong>&alpha; = 0.50</strong></td>
          <td>1.500</td>
          <td><strong>1.503</strong></td>
          <td>1.250</td>
          <td><strong>1.250</strong></td>
          <td>30.92 ms</td>
          <td>19.81 ms</td>
          <td>SC nhanh hơn 1.56x</td>
        </tr>
        <tr>
          <td><strong>&alpha; = 0.75</strong></td>
          <td>2.500</td>
          <td><strong>2.507</strong></td>
          <td>1.375</td>
          <td><strong>1.375</strong></td>
          <td><strong>36.10 ms</strong></td>
          <td>50.81 ms</td>
          <td><strong>LP nhanh hơn 1.41x (Nghịch lý!)</strong></td>
        </tr>
        <tr>
          <td><strong>&alpha; = 0.90</strong></td>
          <td>5.500</td>
          <td><strong>5.504</strong></td>
          <td>1.450</td>
          <td><strong>1.450</strong></td>
          <td>60.15 ms</td>
          <td>31.70 ms</td>
          <td>SC nhanh hơn 1.90x</td>
        </tr>
        <tr>
          <td><strong>&alpha; = 0.95</strong></td>
          <td>10.500</td>
          <td><strong>10.626</strong></td>
          <td>1.475</td>
          <td><strong>1.476</strong></td>
          <td>73.26 ms</td>
          <td>78.66 ms</td>
          <td>Tương đương (~75 ms)</td>
        </tr>
      </table>
      <h2>2. Luận Giải Nghịch Lý Bước Dò vs Thời Gian Thực Thi (The Java Memory Gap)</h2>
      <div class="grid-2">
        <div class="card">
          <div class="card-title">Mô hình bộ nhớ & Cache Line 64 Bytes</div>
          <p>Mỗi Node của SC tốn <strong>32 bytes</strong> trên Heap. Duyệt chuỗi liên kết gây <em>pointer chasing</em> và liên tục L1/L2 cache miss. LP lưu 3 mảng phẳng (13 bytes/slot), nạp gọn trong CPU Cache Line, kích hoạt <strong>Hardware Stream Prefetcher</strong>!</p>
        </div>
        <div class="card">
          <div class="card-title">Thử nghiệm dồn cụm (10% khóa dồn vào M/64)</div>
          <p>Tại M=2²⁰, cụm sơ cấp của LP nối tầng khiến bước dò tăng <strong>4,493x</strong> (11,264 bước), thời gian bùng nổ <strong>315x</strong> (7.48s). SC chỉ tăng 17% bước dò (1.61) do đụng độ bị cô lập trong từng bucket.</p>
        </div>
      </div>
    </div>
    <div class="slide-footer">
      <span>Nhóm 1 - CSD201 (FA26)</span>
      <span>Trang 5 / 8</span>
    </div>
  </div>

  <!-- SLIDE 6 -->
  <div class="slide">
    <div class="slide-header">
      <h1>Slide 6: Kết Quả RQ2 - Động Học Ô Rỗng & Đột Biến Do Rebuild</h1>
      <div class="meta">Thời lượng: 45s | Người trình bày: SV 3 & SV 4 (Lê Phước Anh Quân & Nguyễn Viết Cường - Cặp 2)</div>
    </div>
    <div class="slide-content">
      <h2>1. Sự Tàn Phá Của Tombstone Khi Không Rebuild (&tau; = &infin;)</h2>
      <div class="grid-2">
        <div>
          <table>
            <tr>
              <th>Chu kỳ C</th>
              <th>Ô rỗng (Empty)</th>
              <th>Hit Probes</th>
              <th>Miss Probes</th>
              <th>Miss Latency</th>
            </tr>
            <tr>
              <td>C = 0</td>
              <td>50.00%</td>
              <td>1.502</td>
              <td>2.499</td>
              <td>22.75 ms</td>
            </tr>
            <tr>
              <td>C = 1M</td>
              <td>6.77%</td>
              <td>2.229</td>
              <td>17.235</td>
              <td>51.21 ms</td>
            </tr>
            <tr>
              <td>C = 2M</td>
              <td>0.91%</td>
              <td>2.330</td>
              <td>86.089</td>
              <td>297.37 ms</td>
            </tr>
            <tr>
              <td><strong>C = 4M</strong></td>
              <td><strong>0.04%</strong></td>
              <td><strong>2.345</strong></td>
              <td><strong>2,843.37</strong></td>
              <td><strong>5,847.48 ms</strong></td>
            </tr>
          </table>
        </div>
        <div>
          <div class="card">
            <div class="card-title">Hiện tượng suy kiệt ô rỗng (Empty Slot Starvation)</div>
            <p>Sau 4M chu kỳ, ô rỗng chỉ còn <strong>0.04%</strong>. Tra cứu trượt (Miss query) phải quét qua toàn bộ các ô Tombstone để tìm ô EMPTY. Kết quả: số bước dò bùng nổ <strong>1,137.9x</strong>, độ trễ tăng vọt <strong>233.3x (từ 25ms lên 5.85s)</strong>!</p>
          </div>
          <div class="card">
            <div class="card-title">Clean Table Đối Chứng</div>
            <p>Bảng dựng lại mới tinh từ các khóa sống tại C=4M duy trì ổn định 2.50 bước dò và 25.06 ms, chứng minh 100% sự suy giảm hiệu năng là do ô rác Tombstone gây ra.</p>
          </div>
        </div>
      </div>
    </div>
    <div class="slide-footer">
      <span>Nhóm 1 - CSD201 (FA26)</span>
      <span>Trang 6 / 8</span>
    </div>
  </div>

  <!-- SLIDE 7 -->
  <div class="slide">
    <div class="slide-header">
      <h1>Slide 7: Kết Quả RQ3 - Robin Hood, Dừng Sớm & Backshift</h1>
      <div class="meta">Thời lượng: 45s | Người trình bày: SV 5 (Nguyễn Đình Tiến - Cặp 3)</div>
    </div>
    <div class="slide-content">
      <h2>1. Phân Tích Bóc Tách 3 Cơ Chế Độc Lập (Ablation Analysis)</h2>
      <table>
        <tr>
          <th>Thuật toán</th>
          <th>Cơ chế hoạt động</th>
          <th>Hit Probes (&alpha;=0.8)</th>
          <th>Miss Probes (&alpha;=0.8)</th>
          <th>Miss Latency (&alpha;=0.8)</th>
          <th>Tác động định lượng</th>
        </tr>
        <tr>
          <td><code>lp-tomb</code> (&tau;=0.1)</td>
          <td>Tombstone + Rebuild định kỳ</td>
          <td>3.162</td>
          <td>14.605</td>
          <td>58.67 ms</td>
          <td>Gốc so sánh (Baseline)</td>
        </tr>
        <tr>
          <td><code>lp-backshift</code></td>
          <td>Xóa dịch lùi Algorithm R</td>
          <td>2.998</td>
          <td>12.933</td>
          <td>35.45 ms</td>
          <td>Giảm 11.4% bước dò, giảm 39.6% thời gian</td>
        </tr>
        <tr>
          <td><code>rh-noearly</code></td>
          <td>Hoán đổi DIB (Tắt dừng sớm)</td>
          <td>2.998</td>
          <td>12.933</td>
          <td>52.80 ms</td>
          <td>Thu hẹp phương sai DIB (Max DIB: 41 &rarr; 12)</td>
        </tr>
        <tr>
          <td><strong><code>rh</code> (Full)</strong></td>
          <td><strong>DIB Swap + Early Exit + Backshift</strong></td>
          <td><strong>2.998</strong></td>
          <td><strong>3.398</strong></td>
          <td><strong>43.38 ms</strong></td>
          <td><strong>Giảm 73.8% miss probes vs noearly! Giảm 83.7% vs lp-tomb!</strong></td>
        </tr>
      </table>
      <h2>2. Bản Chất Triệt Tiêu Độ Trễ Đuôi Của Robin Hood</h2>
      <p>Cơ chế hoán đổi DIB "nhường chỗ cho phần tử nghèo hơn" giảm độ lệch chuẩn của khoảng cách dò, khống chế <code>Max DIB &le; 12</code> (so với 41 ở LP). Nhờ đó điều kiện dừng sớm kích hoạt rất nhanh khi tìm kiếm phần tử vắng mặt!</p>
    </div>
    <div class="slide-footer">
      <span>Nhóm 1 - CSD201 (FA26)</span>
      <span>Trang 7 / 8</span>
    </div>
  </div>

  <!-- SLIDE 8 -->
  <div class="slide">
    <div class="slide-header">
      <h1>Slide 8: Đột Biến Khối Rebuild & Ma Trận Khuyến Nghị</h1>
      <div class="meta">Thời lượng: 45s | Người trình bày: SV 6 (Lê Hoàng Cầu - Cặp 3)</div>
    </div>
    <div class="slide-content">
      <h2>1. Phân Tích Độ Trễ Khối 10⁴ Thao Tác (Micro-profiling 8.39 Triệu Phép Toán)</h2>
      <div class="grid-2">
        <div class="card">
          <div class="card-title">Đột biến do Rebuild ở LP-Tomb (&alpha; = 0.80, &tau; = 0.10)</div>
          <p>Trong 839 khối, <code>lp-tomb</code> kích hoạt <strong>83 lần rebuild</strong>. Độ trễ cực đại của khối rebuild lên tới <strong>60.03 ms</strong> (gấp <strong>11.5x</strong> độ trễ trung vị 5.21 ms). Gây hiện tượng <em>Stop-the-world</em> gián đoạn dịch vụ!</p>
        </div>
        <div class="card">
          <div class="card-title">Tính ổn định tuyệt đối của Backshift & Robin Hood</div>
          <p>Cả <code>lp-backshift</code> và <code>rh</code> thực hiện xóa dịch lùi tại chỗ nên <strong>0 lần rebuild</strong>. Độ trễ khối cực đại chỉ 8.52 ms, phân phối độ trễ phẳng hoàn toàn qua 8.39 triệu thao tác.</p>
        </div>
      </div>
      <h2>2. Ma Trận Khuyến Nghị Kiến Trúc Thực Tiễn</h2>
      <table>
        <tr>
          <th>Kịch bản ứng dụng</th>
          <th>Thuật toán tối ưu</th>
          <th>Lý do kỹ thuật</th>
        </tr>
        <tr>
          <td>Dữ liệu tĩnh / Đọc nhiều, &alpha; &le; 0.75</td>
          <td><strong>Linear Probing (lp-tomb)</strong></td>
          <td>Tối đa hóa CPU cache locality, tiết kiệm 3.2x RAM so với Separate Chaining.</td>
        </tr>
        <tr>
          <td>Tải trọng cao (&alpha; &ge; 0.90) hoặc dữ liệu dồn cụm</td>
          <td><strong>Separate Chaining (sc)</strong></td>
          <td>Miễn nhiễm đụng độ diện rộng, cách ly đụng độ trong bucket, an toàn O(1).</td>
        </tr>
        <tr>
          <td>Hệ thống nhạy cảm SLA / Độ trễ đuôi nghiêm ngặt</td>
          <td><strong>Robin Hood Hashing (rh)</strong></td>
          <td>Early Exit triệt tiêu 73.8% miss probes; Backshift triệt tiêu hoàn toàn rebuild spike!</td>
        </tr>
      </table>
    </div>
    <div class="slide-footer">
      <span>Nhóm 1 - CSD201 (FA26)</span>
      <span>Trang 8 / 8</span>
    </div>
  </div>

</body>
</html>
"""

html_path = "scratch/presentation_slides.html"
with open(html_path, "w", encoding="utf-8") as f:
    f.write(html)

edge_path = r"C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe"
input_html = os.path.abspath(html_path)
output_pdf = os.path.abspath("slides/slides_final.pdf")

cmd = [edge_path, "--headless", "--disable-gpu", f"--print-to-pdf={output_pdf}", f"file:///{input_html}"]
res = subprocess.run(cmd, capture_output=True, text=True)
print("Return code:", res.returncode)
print("Output PDF exists:", os.path.exists(output_pdf))
if os.path.exists(output_pdf):
    print("Output PDF size:", os.path.getsize(output_pdf), "bytes")
