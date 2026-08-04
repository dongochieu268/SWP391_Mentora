# Mentora — System Test Flows (tầng nghiệp vụ)

Tạo: 2026-07-20 · theo code `dev1` @ `b67d7c4`. Khung khái niệm: `docs/bpm-reference.md`
(process vs procedure §5, IOD §13.3, end-sớm-hợp-lệ §13.2). Flow gốc: `business-process.md`
§8 (đã superseded bởi file này — §8 stale ở 3 điểm: review-gate, progression policy
`b67d7c4`, timer).

**Phân tầng test** (lý do tồn tại của file này):

- **System test (file này)** — kiểm *business process đạt outcome nghiệp vụ*: chuỗi
  hành động UI thật, cross-actor, state tích luỹ, pass/fail theo outcome đo được.
- **Integration matrix (`test-scenarios.md`, 156 case)** — kiểm *contract từng
  endpoint/guard/message*. Giữ nguyên giá trị, không thay thế; file này `ref` về đó.

## Nguyên tắc viết flow (đã chốt qua thảo luận 2026-07-20)

1. **Outcome đo được** — mỗi flow khai báo trạng thái kết thúc nghiệp vụ; pass/fail
   theo outcome, không theo message một request.
2. **Chỉ hành động UI thật** — không POST thẳng, không sửa DOM, không inject. Các case
   "không có đường click" (JOIN-09, LV-15, LV-20, WZ-06, CL-01..06, QB, AD-08/09…)
   ở lại integration matrix.
3. **Truy-vết-ngược kiểu IOD** — mỗi step có cột `Ref` trỏ về case ID trong
   `test-scenarios.md` (mô hình `ref` của Interaction Overview Diagram): khi step gãy,
   nhảy thẳng sang case chi tiết đã verify (ngày, evidence, DB) thay vì điều tra lại.
4. **Variant thay vì rẽ giữa dòng** — nhánh lỗi/nhánh phụ viết thành variant (V1, V2…)
   dùng chung stage structure, mỗi variant chạy trên state sạch của riêng nó. End sớm
   do nghiệp vụ (bị từ chối join, lớp đóng) là **outcome hợp lệ**, không phải test fail.
5. **Né bug đã biết** — step nào đi qua vùng có known-issue còn mở (xem `fix-list.md`,
   `business-process.md` §9) phải ghi chú để fail không bị nhiễu.

## Catalog flow (value chain)

Chuỗi giá trị: F1 → F2 → F3 (chuẩn bị) → F4 (vào lớp) → **F5 (core, lặp)** ∥ F6/F8
(song song suốt kỳ) → F7 (đóng). Pools: **Admin / Lecturer / Student / TA**.

| # | Flow | Loại | Outcome đo được | Nhóm scenario gốc | Dataset |
|---|---|---|---|---|---|
| F1 | Admin chuẩn bị kỳ | Support | Kỳ ACTIVE + môn ACTIVE có tiên quyết + tài khoản GV login được | AD-01..07, 10..16 | throwaway (quy ước `UM-*`) |
| F2 | GV soạn ngân hàng nội dung | Support | Material có content + đủ câu hỏi ACTIVE, sẵn gắn level | MT, QB | throwaway trong ST-DEMO (reset dọn được) |
| F3 | GV dựng lộ trình & lớp | Support | Lớp OPEN có mã mời; path khoá cấu trúc; variant clone (LV-22) | WZ, LV, NC | **tự dựng từ 0** (trong môn ST-DEMO) |
| F4 | SV gia nhập lớp (bước 0: register AU-03) | Core | Membership ACTIVE; end sớm hợp lệ: bị từ chối / lớp CLOSE | AU-03, JOIN-01..08 | **seed** (`ST2026OK`, sv3) |
| F5 | SV vòng học mastery | **Core** | Node completed / node kế mở đúng policy; bestScore ghi đúng; roadmap phản ánh | LT | **seed** (sv1/sv2, Lớp System Test) |
| F6 | GV điều hành lớp đang chạy | Management | Lớp vận hành đúng ý GV (ẩn/hiện node, CLOSE, duyệt member giữa kỳ, TA); grant = cơ hội cải thiện, không khoá lại tiến độ (LT-29) | RS, GR, NV, CL-07, JOIN-09/10 | **seed, chạy nối sau F5** (cần attempt data) |
| F7 | Kết thúc vòng đời lớp | Management | COMPLETED; attempt dở vẫn nộp được; CSV khớp; reopen được | END | **seed, chạy cuối chuỗi** (sau F5/F6) |
| F8 | Hỗ trợ trong lớp — QnA 3 actor | Support | Câu hỏi ANSWERED bởi GV/TA; GV kiểm duyệt được | QA-01..04 | **seed** (TA từ F6.V4) |

**Loại trừ có chủ đích** (không phải bỏ sót): nhóm SY (hành vi khung/rủi ro — ở lại
integration matrix); AS + CL-01..06 (legacy/route mồ côi — quyết định chủ dự án
2026-07-14: coi như không tồn tại); mọi case defensive-only không đường click.

**Thứ tự chạy khuyến nghị cả bộ:** reset+seed → F1 (throwaway riêng) → F2 → F3 →
F4 (trên lớp seed) → F5 (sv1: V1/V2 → sv2: V3) → F6 (cần attempt của F5) → F8 (cần
TA của F6.V4) → F7 (đóng lớp cuối cùng). F4.V3.2 (join lớp CLOSE) chạy sau F6.V5.

---

## F1 — Admin chuẩn bị kỳ

**Mục tiêu nghiệp vụ:** Admin mở một kỳ học mới hoàn chỉnh: học kỳ, môn (kèm tiên
quyết), tài khoản GV — sao cho **GV thấy đủ nguyên liệu trong wizard step1**.

**Pools:** Admin (chính) · Lecturer (verify). **Dataset:** throwaway toàn bộ (học kỳ,
môn, GV mới) theo quy ước `UM-*`; KHÔNG đụng ST-DEMO/học kỳ E2E đang được seed dùng
(rủi ro AD-12/AD-16).

### Stage map

```
S1 Học kỳ → S2 Môn + tiên quyết → S3 Tài khoản GV → S4 Verify từ phía GV
```

### V1 — Happy: mở kỳ trọn vẹn

| # | Stage | Hành động | Expected (verbatim) | Ref |
|---|---|---|---|---|
| 1 | S1 | Login admin1 → tạo học kỳ mới (ngày hợp lệ, không chồng kỳ) | "Đã thêm học kỳ mới." | AD-06 |
| 2 | S2 | Tạo môn mới (mã + tên hợp lệ) | Môn ACTIVE, hiện trong danh sách | AD-02 |
| 3 | S2 | Gán môn khác làm tiên quyết của môn mới | "Đã thêm môn tiên quyết!"; bảng hiện **tên môn** (không phải id thô — fix #16) | AD-03 |
| 4 | S3 | Tạo tài khoản GV mới | "Tạo tài khoản giảng viên thành công!" | AD-04 |
| 5 | S4 | Logout → login GV mới → mở wizard step1 | Vào `/lecturer/dashboard`; dropdown "Học kỳ *" có kỳ bước 1, dropdown "Môn học *" có môn bước 2 | AD-04, AD-01, AD-02 |

**Outcome pass:** bước 5 — GV mới nhìn thấy đủ kỳ + môn. Tạo được ở phía admin mà GV
không thấy = fail nghiệp vụ.

### V2 — Nhánh chặn qua UI thật (mỗi case một submit, state không đổi)

| # | Hành động | Expected | Ref |
|---|---|---|---|
| 1 | Tạo GV với email đã tồn tại | "Email này đã được sử dụng." — modal giữ nguyên dữ liệu đã nhập | AD-07 |
| 2 | Tạo môn trùng mã | "Mã môn đã tồn tại" | AD-10 |
| 3 | Tạo học kỳ trùng tên | "Đã tồn tại học kỳ với tên "…"." | AD-14 |
| 4 | Ngày kết thúc trước ngày bắt đầu | "Ngày kết thúc phải sau hoặc bằng ngày bắt đầu." | AD-15 |
| 5 | Khoảng ngày chồng kỳ khác | "Khoảng thời gian trùng với học kỳ khác." | AD-15, AD-06 |

### V3 — Khoá tài khoản (end sớm hợp lệ cho user bị khoá)

| # | Hành động | Expected | Ref |
|---|---|---|---|
| 1 | Admin đổi trạng thái 1 tài khoản throwaway sang "Bị cấm" | "Đã cập nhật trạng thái tài khoản." | AD-05 |
| 2 | Login bằng tài khoản đó | "Tài khoản đã bị khóa hoặc không hoạt động." (BANNED và INACTIVE cùng message) | AD-05 |
| 3 | (nếu user đang có session) request kế tiếp | Tự văng về `/login`, không flash giải thích (hành vi đã chốt, không phải bug) | SY-02 |

### Known-issues chạm F1

- Ẩn môn (unpublish) làm môn biến mất khỏi dropdown GV ngay (AD-02) — nếu flow khác
  đang giữa chừng wizard với môn đó, hành vi chưa được định nghĩa; không test chồng.

---

## F2 — GV soạn ngân hàng nội dung

**Mục tiêu nghiệp vụ:** GV có material đủ điều kiện gắn vào level: có nội dung học
và có câu hỏi ACTIVE rút đề được.

**Pools:** Lecturer (duy nhất). **Dataset:** material throwaway trong môn ST-DEMO
(login `st.gv1`); câu hỏi lấy từ bank ST-DEMO seed sẵn.

### Stage map

```
S1 Tạo material → S2 Thêm nội dung học → S3 Gắn câu hỏi bank → S4 Sẵn sàng gắn level
```

### V1 — Happy: material hoàn chỉnh

| # | Stage | Hành động | Expected (verbatim) | Ref |
|---|---|---|---|---|
| 1 | S1 | Tạo material mới (title + môn đang lọc) | "Đã tạo material.", đứng **đầu danh sách** | MT-01 |
| 2 | S2 | Thêm nội dung TEXT | "Đã thêm nội dung." — DB row `material_id` set, `node_id=NULL` | MT-15 |
| 3 | S2 | Thêm nội dung FILE (upload tệp thật, đuôi hợp lệ) | "Đã thêm nội dung." — `content_url` dạng `/uploads/node-contents/files/{uuid}.*` | MT-17 |
| 4 | S3 | Tick câu hỏi cùng môn → "Gắn câu đã chọn" | "Đã gắn N câu hỏi vào material." — `material_id` set | MT-10 |
| 5 | S4 | Mở cấu hình level bất kỳ (node của mình) → dropdown material | Material mới xuất hiện trong dropdown, gắn được ("Đã thêm tài liệu vào level.") | LV-17 |

**Outcome pass:** bước 5 — material dùng được ở nơi tiêu thụ (level). Tạo xong mà
không gắn được = fail.

### V2 — Sửa/xoá và ràng buộc vòng đời

| # | Hành động | Expected | Ref |
|---|---|---|---|
| 1 | Sửa title + xoá trắng description | "Đã cập nhật material." — DB `description=NULL` | MT-05 |
| 2 | Sửa rồi xoá 1 nội dung | "Đã cập nhật nội dung." / dialog "Xóa nội dung này?" → row biến mất | MT-16 |
| 3 | Xoá material đang gắn level | "Material đang được sử dụng trong cấu hình level, không thể xóa." | MT-08 |
| 4 | Xoá material còn nội dung con | "Material đang có nội dung, không thể xóa." → xoá content trước → "Đã xóa material." | MT-09 |
| 5 | Chuyển câu hỏi từ material A sang B cùng môn | `material_id` chuyển ngay (1 câu ≤ 1 material); gỡ chỉ ảnh hưởng câu thuộc material đang thao tác | MT-13, MT-14 |

### V3 — Nhánh chặn UI thật

| # | Hành động | Expected | Ref |
|---|---|---|---|
| 1 | Bấm "Gắn câu đã chọn" khi không tick gì | "Vui lòng chọn ít nhất một câu hỏi." — không mất dữ liệu | MT-12 |
| 2 | Upload FILE đuôi không hỗ trợ (`.exe`) | "Định dạng không hỗ trợ. Cho phép: pdf, zip, png, doc, pptx, jpg, jpeg, docx, xlsx, gif, rar, ppt, xls, txt" | NC-04 |
| 3 | Submit nội dung TEXT/LINK/FILE rỗng (không bị client chặn) | Chặn đúng nghiệp vụ nhưng message nhiễm tiền tố "400 BAD_REQUEST …" — **bug #26 còn mở**, expected tạm theo hiện trạng | MT-18 |

### Known-issues chạm F2

- Bug #26 (`ResponseStatusException` lộ tiền tố) — V3.3 assert theo hiện trạng; khi
  sửa thì đổi expected (đó là dấu hiệu test đúng).
- Bug #12 (crash auto-select material GV khác) **đã FIX** (MT-19) — nếu sidebar
  "Material" crash 500 trở lại là regression.

---

## F3 — GV dựng lộ trình & lớp

**Mục tiêu nghiệp vụ:** từ số 0, GV có một lớp OPEN kèm mã mời, lộ trình có node
được cấu hình level + material, sẵn cho SV join và học.

**Pools:** Lecturer (duy nhất; SV chỉ verify ở F4/F5). **Dataset:** tự dựng từ 0 qua
wizard, trong môn ST-DEMO (reset dọn được); material dùng bản của F2 hoặc seed.

### Stage map

```
S1 Wizard step1 (môn/kỳ/tên) → S2 Builder (node + level + material + content)
→ S3 Review → S4 Confirm nhận mã mời
```

### V1 — Happy: dựng lớp trọn vẹn

| # | Stage | Hành động | Expected (verbatim) | Ref |
|---|---|---|---|---|
| 1 | S1 | Login gv → wizard: chọn môn ST-DEMO, học kỳ, đặt tên lớp → tạo path mới | Sang step2, builder trống | WZ-01 |
| 2 | S2 | Thêm node đầu tiên | Node tự có Level 1 "Cơ bản" maxScore 10 / pass 5 | LV-01 |
| 3 | S2 | Thêm Level 2 cho node (số thứ tự kế tiếp, điểm hợp lệ) | "Đã thêm level mới." — hiện đúng trong thang level | LV-02 |
| 4 | S2 | Gắn material vào level + questionCount | "Đã thêm tài liệu vào level." — row Trạng thái "OK" | LV-17 |
| 5 | S2 | Thêm nội dung học cho node | "Đã thêm nội dung!" (dấu chấm than) | NC-01 |
| 6 | S2 | Thêm node 2, đặt node 1 làm tiên quyết | Node 2 hiện sau node 1 trong builder | LV-01 |
| 7 | S3 | Sang review | Sạch hoặc chỉ khuyến nghị; nút "Tạo lớp học" **enabled** | WZ-07 |
| 8 | S4 | Confirm | 'Đã tạo lớp "…". Mã mời: XXXXXXXX' (8 ký tự) → về danh sách lớp | WZ-01 |

**Outcome pass:** bước 8 + mã mời dùng được ở F4. Lớp tạo xong mà SV không join được
bằng mã = fail chuỗi.

### V2 — Ràng buộc cấu hình trong builder (UI thật)

| # | Hành động | Expected | Ref |
|---|---|---|---|
| 1 | Thêm level trùng số thứ tự | "Số thứ tự level đã tồn tại trong node này." | LV-04 |
| 2 | passingScore > maxScore | "Ngưỡng qua level phải nằm trong phổ điểm của level đó." | LV-06 |
| 3 | Gắn lại material đã gắn | "Material này đã được thêm vào level. Sửa số câu trên dòng hiện có." | LV-16 |
| 4 | questionCount > pool material | Không chặn; row cảnh báo "Material chỉ có N câu, ít hơn số câu đã cấu hình." | LV-18 |
| 5 | Gỡ material cuối của level | "Đã gỡ tài liệu khỏi level." → "Level này chưa có tài liệu nào — sẽ tạo đề 0 câu hỏi." | LV-19 |
| 6 | Xoá level không attempt (node còn level khác) | "Đã xóa level." — không dồn số | LV-12 |

### V3 — Đổi môn giữa wizard

| # | Hành động | Expected | Ref |
|---|---|---|---|
| 1 | Tạo path ở step2 → quay step1 đổi môn → submit | "Bạn đã đổi môn học nên lộ trình đã tạo trước đó không còn khớp — hãy tạo/chọn lộ trình mới. Lộ trình cũ vẫn nằm trong trang Lộ trình học của bạn." — path cũ còn nguyên trong `/lecturer/learning-paths` | WZ-03 |

### V4 — Huỷ wizard (end sớm hợp lệ)

| # | Hành động | Expected | Ref |
|---|---|---|---|
| 1 | "Hủy wizard" → confirm (không tick xoá path) | Về `/lecturer/classes`, không lớp mới; path wizard còn nguyên | WZ-08 |

### V5 — Dùng lại tài sản: path có sẵn / clone

| # | Hành động | Expected | Ref |
|---|---|---|---|
| 1 | Wizard chọn path có sẵn đang bị lớp khác dùng | Warning "Cấu trúc lộ trình đã khóa…" → confirm vẫn tạo lớp OK | WZ-02 |
| 2 | Clone path (từ trang Lộ trình học) | "Đã clone lộ trình thành công." — node/level/material copy đủ, id mới, không attempt | LV-22 |

### Known-issues chạm F3

- Bug #30 (returnTo wizard) **đã FIX qua nhiều vòng** — từ step2 bấm "Cấu hình
  level"/"Quản lý nội dung", nút quay lại phải về đúng wizard; văng ra
  `/lecturer/learning-paths` là regression (đã tái phát 1 lần ở `76a80eb`).
- Review "0 node" làm nút confirm disabled (WZ-04) — coi là guard, không phải bước flow.

---

## F4 — SV gia nhập lớp

**Mục tiêu nghiệp vụ:** một người dùng mới trở thành thành viên ACTIVE của lớp — hoặc
kết thúc sớm hợp lệ (bị từ chối, lớp đóng).

**Pools:** Student (chính) · Lecturer (duyệt). **Dataset:** seed — sv3 (không thuộc
lớp nào), lớp `Lớp System Test` mã `ST2026OK`, gv1 duyệt. Bước 0 (register) dùng tài
khoản throwaway riêng nếu chạy.

### Stage map

```
S0 Register (tuỳ chọn) → S1 Nhập mã join → S2 GV xử lý yêu cầu → S3 Xác nhận vào lớp
```

### V1 — Happy: join → duyệt → vào lớp

| # | Stage | Hành động | Expected (verbatim) | Ref |
|---|---|---|---|---|
| 1 | S0 | Register tài khoản mới → login | "Đăng ký thành công! Hãy đăng nhập." → login vào `/student/dashboard` | AU-03 |
| 2 | S1 | sv3 nhập mã `ST2026OK` → gửi | "Đã gửi yêu cầu tham gia lớp. Chờ giảng viên chấp nhận." — trạng thái PENDING | JOIN-01 |
| 3 | S2 | gv1 mở members → "Chấp nhận" | "Đã chấp nhận yêu cầu tham gia." — member ACTIVE | JOIN-01 |
| 4 | S3 | sv3 mở lớp → roadmap | Roadmap tải được, thấy node VISIBLE của lớp | JOIN-01, LT-19 |

**Outcome pass:** bước 4 — sv3 nhìn thấy lộ trình. ACTIVE trong DB mà roadmap không
vào được = fail.

### V2 — Bị từ chối → rejoin (end sớm hợp lệ ở bước 2)

| # | Stage | Hành động | Expected | Ref |
|---|---|---|---|---|
| 1 | S1 | sv3 join như V1.2 | PENDING | JOIN-01 |
| 2 | S2 | gv1 "Từ chối thành viên" + confirm | "Đã từ chối yêu cầu tham gia." — request biến mất cả 2 danh sách; **không có danh sách đã-từ-chối** | JOIN-06 |
| 3 | S1' | sv3 join lại ngay | Được phép (không khoá vĩnh viễn) → PENDING → gv1 approve → ACTIVE | JOIN-06, JOIN-01 |

### V3 — Mã sai / lớp đóng (end sớm hợp lệ)

| # | Hành động | Expected | Ref |
|---|---|---|---|
| 1 | Nhập mã không tồn tại | "Mã lớp không hợp lệ." | JOIN-03 |
| 2 | (GV đóng lớp trước — F6.V5) → join lớp CLOSE | "Lớp học này đã đóng, không thể tham gia." | JOIN-04, CL-07 |

### V4 — Join lặp

| # | Hành động | Expected | Ref |
|---|---|---|---|
| 1 | Join lần 2 khi đang PENDING hoặc đã ACTIVE | "Bạn đã gửi yêu cầu hoặc đã là thành viên lớp này rồi." | JOIN-05 |

### Known-issues chạm F4

- JOIN-07/08 (approve/reject yêu cầu hết PENDING) cần kỹ thuật đua 2 tab — không phải
  hành vi UI đơn của một người dùng, ở lại integration matrix.

---

## F5 — SV vòng học mastery

**Mục tiêu nghiệp vụ:** sinh viên học material, chứng minh mastery qua level test, và
hệ thống mở tiến độ đúng **policy một chiều** (`NodeLevelProgressionPolicy` @ `b67d7c4`):
đạt bất kỳ level nào của node tiên quyết HOẶC rớt-hết-lượt ở level đầu chưa đạt đều mở
node kế; grant về sau không khoá lại.

**Pools:** Student (chính) · System (chấm điểm, progression) · Lecturer (chỉ ở
precondition). **Precondition chung:** DB reset + seed (`README-systemtest.md` §2/§5);
sv1/sv2 ACTIVE trong `Lớp System Test`; app chạy. **Dataset seed liên quan:** Node 1
(L1: 3 câu từ "ST Cơ bản", không giới hạn lượt, pass 5/10; L2: 3 câu từ "ST Nâng cao",
maxAttempts 2, pass 7/10) → Node 2 (tiên quyết Node 1; L1: 2 câu từ "ST Cơ bản").
Đáp án đúng luôn là "Phương án A (đúng)…". ID tra theo code/email, không hardcode.

### Stage map

```
S1 Vào lộ trình → S2 Học material (review-gate) → S3 Làm level test → S4 Nhận kết quả
→ S5 Kiểm progression trên roadmap
```

### V1 — Happy: đạt level, node kế mở (actor: sv1)

| # | Stage | Hành động (UI thật) | Expected (verbatim từ code) | Ref |
|---|---|---|---|---|
| 1 | S1 | Login sv1 → mở lớp → roadmap | Node 1 "Có thể học"; Node 2 "Đang khóa"; header "0/2 bài đã học" + "Điểm 0/Y" (Y theo công thức max-per-node) | AU-01, LT-19 |
| 2 | S2 | Mở node detail Node 1 → đọc material → bấm "Đã học xong" | Badge L1 đổi "Cần học nội dung" → "Sẵn sàng làm bài"; DB `student_level_reviews` có row mới | LT-24 |
| 3 | S3 | Bấm "Làm bài" L1 → chọn toàn "Phương án A (đúng)…" → nộp | Trang kết quả 10.00/10.00, "ĐẠT", CTA "Thử Level tiếp theo"; review đúng từng câu | LT-01 |
| 4 | S4 | Xem lịch sử trên node detail | L1 hiện số attempt + điểm best + "Đạt"; L2 chưa làm: "Chưa làm" | LT-18 |
| 5 | S5 | Quay lại roadmap | Node 1 badge "Lv. 1/2"; **Node 2 đổi "Có thể học"** (đạt 1 level của node tiên quyết là đủ — rule a/b); header cập nhật điểm | LT-27, LT-19 |

**Outcome pass:** bước 5 — Node 2 mở, điểm/badge khớp. Mọi thứ khác đúng mà Node 2
không mở = fail nghiệp vụ.

### V2 — Rớt còn lượt: làm lại được, tiến độ không mở nhầm (actor: sv1, tiếp sau V1)

| # | Stage | Hành động | Expected | Ref |
|---|---|---|---|---|
| 1 | S2–S3 | "Đã học xong" material L2 → làm L2 sai đa số (1/3 đúng) | 3.33/10.00, "CHƯA ĐẠT", CTA "Thử lại Level này" (còn lượt 1/2) | LT-02 |
| 2 | S4 | Quay lại node detail | Badge L2 "Cần làm lại" (trạng thái thứ 3, khác "Cần qua level trước"/"Đã hết lượt làm bài") | LT-26 |
| 3 | S3' | "Thử lại Level này" → làm đúng 3/3 → nộp | 10.00/10.00 "ĐẠT"; lịch sử giữ điểm **best**, không phải lượt cuối | LT-02, LT-18 |

### V3 — Rớt hết lượt ở level đầu: node kế VẪN mở (policy mới) (actor: sv2, state sạch)

| # | Stage | Hành động | Expected | Ref |
|---|---|---|---|---|
| 1 | S1–S2 | sv2 login → roadmap → "Đã học xong" L1 Node 1 | như V1.1–2 | LT-24 |
| 2 | S3 | Làm L2 (maxAttempts 2) sai toàn bộ, cả 2 lượt | Lượt cuối: cả 2 CTA ẩn + "Bạn đã hết số lần thử cho level này." (nếu là level đầu chưa đạt của node: kèm "…Level sau vẫn khóa, nhưng bạn có thể chuyển sang bài học tiếp theo trên lộ trình.") | LT-03, LT-28 |
| 3 | S5 | Roadmap | Node vẫn "Đang học" (chưa đạt) nhưng **node kế "Có thể học"** — rule c: rớt + hết lượt vẫn mở tiến độ | LT-28 |

> V3 là điểm nghiệp vụ *mới* của `b67d7c4` mà §8 cũ mô tả sai ("đậu mở Level 2") —
> đây chính là case phân biệt system test theo policy hiện hành vs theo tài liệu cũ.

### V4 — Chế độ thi có timer (actor: sv theo fixture; **cần fixture**: seed không có
level nào đặt `duration_minutes` — set `duration_minutes=1` cho 1 level trước khi chạy)

| # | Stage | Hành động | Expected | Ref |
|---|---|---|---|---|
| 1 | S3 | Vào `/take` level có duration | Badge đồng hồ đếm ngược hiện; level không duration thì **không có** đồng hồ | LT-25, LT-34 |
| 2 | S3 | Bấm Back trình duyệt giữa bài | Modal "Bài làm chưa được nộp" / "Bạn cần nộp bài trước khi rời khỏi trang này." — vẫn ở nguyên trang | LT-31 |
| 3 | S3 | Rời trang (chấp nhận beforeunload) → quay lại khi còn giờ | Cùng attempt (không tạo mới), đồng hồ **đếm tiếp theo thời gian thực**, lựa chọn cũ mất (không có draft) | LT-32 |
| 4 | S3→S4 | Để hết giờ | Tự nộp đúng lúc hết giờ (chênh <1s), chuyển trang kết quả; DB `timed_out=1` | LT-25, LT-33 |

> Ghi chú kiến trúc (không phải bug): server KHÔNG chặn cứng quá hạn — tự nộp là JS
> client; server chỉ ghi nhận `timedOut` lúc submit (LT-33).

### V5 — Node không có bài test (mọi level 0 câu) (actor: bất kỳ, cần node fixture qua wizard)

| # | Stage | Hành động | Expected | Ref |
|---|---|---|---|---|
| 1 | S2 | Mở node detail node 0 câu | Không có "Danh sách level"/nút "Làm bài"; chỉ có "Đánh dấu hoàn thành" | LT-15 |
| 2 | S5 | Bấm "Đánh dấu hoàn thành" → roadmap | Node "Đã hoàn thành"; roadmap **không có badge "Lv. X/M"** cho node này | LT-15, LT-30 |

### Known-issues chạm F5

- Bug #15 (roadmap/node-detail nuốt flash) **đã FIX 2026-07-16** — nếu tái xuất hiện
  "bị đá về roadmap im lặng" ở bất kỳ step nào, đó là regression, đối chiếu LT-05/23.
- Nút "Đánh dấu hoàn thành" dùng `alert()` cho nhánh lỗi (bug #33, chưa sửa) — V5 chỉ
  đi happy path, không assert nhánh lỗi này.

---

## F6 — GV điều hành lớp đang chạy

**Mục tiêu nghiệp vụ:** trong lúc lớp vận hành, GV theo dõi được tiến độ thật, can
thiệp được (cấp lượt, ẩn/hiện node, quản lý thành viên/TA, đóng lớp) và **mọi can
thiệp phản ánh đúng phía SV** — riêng grant không được khoá lại tiến độ đã mở.

**Pools:** Lecturer (chính) · Student (quan sát hiệu lực) · TA (được bổ nhiệm).
**Dataset:** seed, **chạy nối sau F5** (RS/GR cần attempt data thật của sv1/sv2).

### Stage map

```
S1 Theo dõi kết quả → S2 Can thiệp lượt (grant) → S3 Điều tiết lộ trình (ẩn/hiện node)
→ S4 Quản trị thành viên (TA) → S5 Đóng lớp (CLOSE)
```

### V1 — Theo dõi kết quả (đọc, sau khi F5 đã sinh attempt)

| # | Stage | Hành động | Expected | Ref |
|---|---|---|---|---|
| 1 | S1 | gv1 mở bảng kết quả lớp | Tổng điểm/`Hoàn thành K/N (x%)` đúng cho sv đã làm; "Chưa làm" cho sv chưa làm | RS-01 |
| 2 | S1 | Xem dòng tổng hợp per node | "Điểm trung bình", "Đã đạt n (x%)" = tỉ lệ trên member ACTIVE | RS-02 |
| 3 | S1 | Mở chi tiết student → chi tiết attempt | Đúng từng attempt thật; snapshot khớp 100% lựa chọn lúc làm; **không có nút sửa/chấm lại** | RS-03, RS-04 |

### V2 — Grant: cơ hội cải thiện, không đụng tiến độ (nối sau F5.V3 — sv2 đã hết lượt)

| # | Stage | Hành động | Expected | Ref |
|---|---|---|---|---|
| 1 | S2 | gv1 → student detail sv2 → bấm nút số lượt level đã hết | Modal "Xác nhận cấp thêm lượt" → "Đã cấp thêm 1 lượt." — "Lượt: 2/3" + badge "+1 đã cấp" | GR-01, LT-29 |
| 2 | S2 | Kiểm roadmap sv2 | Node kế **VẪN "Có thể học"** — grant không khoá lại (công thức bỏ `grantedAttempts`) | LT-29 |
| 3 | S2 | sv2 dùng lượt cấp, làm đạt | Điểm cải thiện ghi nhận; node tiên quyết "Đã hoàn thành" nếu đủ điều kiện | LT-29 |
| 4 | S2 | Grant thêm nhiều lần | Badge gộp "+N đã cấp" tăng dần; **không có nút thu hồi grant** | GR-04 |
| 5 | S2 | Xem level không giới hạn lượt | Không render "Lượt: x/y" lẫn nút cấp lượt | GR-02 |

**Outcome pass:** bước 2–3 — đây là kiểm chứng nghiệp vụ "grant = cơ hội cải thiện
điểm, không phải chìa khoá tiến độ" (điểm pivot của `b67d7c4`).

### V3 — Điều tiết lộ trình theo lớp

| # | Stage | Hành động | Expected | Ref |
|---|---|---|---|---|
| 1 | S3 | gv1 ẩn 1 node của lớp → SV mở roadmap | Node biến mất khỏi roadmap SV; ẩn hết node → "Giảng viên chưa mở bài học nào." | NV-01, LT-20 |
| 2 | S3 | Hiện lại node | "Đã bật hiển thị node cho lớp." — SV thấy lại; node mới mặc định **VISIBLE** (sau migration) | NV-01, LT-20 |

### V4 — Bổ nhiệm / thu hồi TA

| # | Stage | Hành động | Expected | Ref |
|---|---|---|---|---|
| 1 | S4 | gv1 → members → "Chỉ định trợ giảng" trên member ACTIVE | "Đã chỉ định trợ giảng (TA)." — nút chỉ render với member ACTIVE (PENDING không có) | JOIN-10, JOIN-09 |
| 2 | S4 | Thu hồi | "Đã thu hồi quyền trợ giảng." | JOIN-10 |

### V5 — Đóng lớp (CLOSE, khác COMPLETED)

| # | Stage | Hành động | Expected | Ref |
|---|---|---|---|---|
| 1 | S5 | gv1 sửa lớp: Trạng thái OPEN → CLOSE | "Đã cập nhật lớp học." | CL-07 |
| 2 | S5 | SV mới join bằng mã | "Lớp học này đã đóng, không thể tham gia." (nối F4.V3.2) | JOIN-04 |

### Known-issues chạm F6

- Bug #25/#27/#28 (non-owner crash 500) **đã FIX 2026-07-16** qua `LecturerExceptionHandler`
  — flow này chỉ đi trên lớp của chính GV nên không chạm; ghi để nhận diện regression.
- Modal a11y `aria-hidden` (bug #14) còn dạng "fix nhầm file mồ côi" ở màn node —
  không assert accessibility tree trong flow.

---

## F7 — Kết thúc vòng đời lớp

**Mục tiêu nghiệp vụ:** GV chốt sổ lớp: điểm quá trình được bảo toàn và xuất được;
SV không bắt đầu bài mới nhưng bài dở không bị mất; quyết định nhầm thì đảo được.

**Pools:** Lecturer (chính) · Student (hiệu lực). **Dataset:** seed, **chạy cuối
chuỗi** (sau F5/F6 — cần điểm thật và nên có 1 attempt IN_PROGRESS dựng trước ở F5).

### Stage map

```
S1 Complete lớp → S2 Hiệu lực phía SV → S3 Xuất CSV → S4 Reopen (nếu nhầm)
```

### V1 — Happy: chốt sổ

| # | Stage | Hành động | Expected (verbatim) | Ref |
|---|---|---|---|---|
| 1 | S1 | gv1 bấm kết thúc lớp | "Đã kết thúc lớp học." — status COMPLETED, redirect thẳng trang tổng kết, badge "Đã kết thúc" | END-01 |
| 2 | S2 | SV bấm "Làm bài" (start attempt mới) | Bị chặn, roadmap hiện "Lớp học đã kết thúc." (flash đã render sau fix #15) | END-03 |
| 3 | S2 | SV có attempt IN_PROGRESS từ trước → resume + nộp | Nộp và chấm điểm bình thường; UI kết quả GV tự ghi chú "Lớp đã kết thúc — … lượt đang làm dở vẫn được nộp." | END-04, LT-06 |
| 4 | S2 | SV mở các màn đọc (roadmap/kết quả/QnA) | Đều tải bình thường, không màn nào bị chặn | END-05 |
| 5 | S3 | Tải `export.csv` mở bằng bảng tính | BOM `EF BB BF` (tiếng Việt không vỡ); dạng ngang 1 dòng/SV, cột theo node, ô = bestScore, trống nếu chưa làm | END-06, SY-08 |

**Outcome pass:** bước 5 — CSV khớp đúng dữ liệu F5 đã tạo (đối chiếu số cụ thể,
không chỉ "có file").

### V2 — Reopen (đảo quyết định)

| # | Stage | Hành động | Expected | Ref |
|---|---|---|---|---|
| 1 | S4 | gv1 bấm "Mở lại lớp" trên lớp COMPLETED | "Đã mở lại lớp học." — status OPEN | END-07 |
| 2 | S4 | SV start attempt mới | Làm bài lại bình thường | END-07, LT-01 |

### Known-issues chạm F7

- Nút "Làm bài" phía SV **không bị ẩn/disable** trước khi bấm dù lớp COMPLETED
  (LT-07 ghi nhận) — flow assert theo hiện trạng: chặn sau khi bấm, có message.
- Nhắc lớp quá hạn theo semester endDate: **không tồn tại, đã loại khỏi phạm vi**
  (quyết định chủ dự án — END-10). Không viết step.

---

## F8 — Hỗ trợ trong lớp (QnA, 3 actor)

**Mục tiêu nghiệp vụ:** SV đặt câu hỏi và nhận được trả lời chính thức từ GV hoặc TA;
GV kiểm duyệt được nội dung.

**Pools:** Student (hỏi) · Lecturer (trả lời/kiểm duyệt) · TA (trả lời official).
**Dataset:** seed; precondition TA: F6.V4 đã bổ nhiệm 1 member ACTIVE làm TA.

### Stage map

```
S1 SV đặt câu hỏi → S2 GV/TA trả lời → S3 GV kiểm duyệt
```

### V1 — Happy: hỏi → GV trả lời

| # | Stage | Hành động | Expected (verbatim) | Ref |
|---|---|---|---|---|
| 1 | S1 | sv1 gửi câu hỏi trong QnA lớp | "Đã gửi câu hỏi." — badge "Mới"/"Chưa trả lời", hiện ở cả 2 phía | QA-01 |
| 2 | S2 | gv1 bấm "Phản hồi" → gửi | "Đã gửi phản hồi." — status ANSWERED, đếm "Chưa trả lời" giảm | QA-02 |

**Outcome pass:** bước 2 — SV thấy câu trả lời, trạng thái ANSWERED hai phía.

### V2 — TA trả lời official

| # | Stage | Hành động | Expected | Ref |
|---|---|---|---|---|
| 1 | S2 | TA (member được bổ nhiệm ở F6.V4) mở QnA phía student → trả lời | Status ANSWERED; reply lưu `responder_id` = user TA — tư cách official | QA-04 |

### V3 — Kiểm duyệt

| # | Stage | Hành động | Expected | Ref |
|---|---|---|---|---|
| 1 | S3 | gv1 xoá câu hỏi không phù hợp (confirm) | "Đã xóa câu hỏi." — biến mất hoàn toàn khỏi 2 phía | QA-03 |

### Known-issues chạm F8

- Message lỗi vùng QnA **tiếng Việt không dấu** (bug #10, chủ dự án đã chốt cần sửa)
  — các nhánh lỗi đó không nằm trong flow UI thật (QA-05 là GET thẳng), ở lại matrix;
  khi sửa xong không ảnh hưởng flow này.

---

## Khi chạy thật

- Ghi evidence theo quy ước `docs/evidence/` (ảnh + ngày) như các vòng verify trước;
  cập nhật cột kết quả từng variant vào file này hoặc file kết quả riêng theo đợt.
- Mỗi variant fail → đối chiếu `Ref` trước khi kết luận (phân biệt regression vs
  known-issue vs flow viết sai).
- Sau mỗi đợt: nếu code đổi policy/message, sửa flow này TRƯỚC, `test-scenarios.md`
  theo sau — flow là tầng nghiệp vụ, matrix là tầng contract.
