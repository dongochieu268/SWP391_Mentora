# README — System Test (môi trường + seed + tài khoản)

Cập nhật: 2026-07-13. Mục đích: **mọi người / mọi máy chạy system test ra cùng
một kết quả**. Toàn bộ lệnh dưới đây đã chạy thử thật ngày 2026-07-13 (kể cả
chu trình reset → seed lặp lại và tái hiện bug #12) — không phải viết chay.

## 1. Môi trường

| Thành phần | Phiên bản / giá trị |
|---|---|
| Java | 21 (pom khai `<java.version>17</java.version>` — 17 trở lên) |
| Build | Maven Wrapper (`mvnw.cmd`, không cần cài Maven) |
| DB | SQL Server 16 (2022), instance local, port 1433 |
| DB name / user | `MentoraDB` / `sa` — mật khẩu xem `application.properties` (mặc định repo: `123`; máy khác sửa lại file đó) |
| App | `.\mvnw.cmd spring-boot:run` → http://localhost:8080 |
| CLI nạp SQL | `sqlcmd` (có sẵn khi cài SQL Server) |

## 2. Nạp database — đúng thứ tự, đúng lệnh

Tạo database `MentoraDB` trước (SSMS hoặc `CREATE DATABASE MentoraDB`), rồi:

```powershell
cd src\main\resources\db
sqlcmd -S localhost,1433 -U sa -P <mật-khẩu> -C -i schema.sql
sqlcmd -S localhost,1433 -U sa -P <mật-khẩu> -C -i migration-node-levels.sql
sqlcmd -S localhost,1433 -U sa -P <mật-khẩu> -C -i migration-node-levels-followup.sql
sqlcmd -S localhost,1433 -U sa -P <mật-khẩu> -C -i migration-classroom-completed.sql
sqlcmd -S localhost,1433 -U sa -P <mật-khẩu> -C -i seed-systemtest.sql
```

⚠ **2 cái bẫy đã dính thật (2026-07-13) — và cách file seed mới đã né sẵn:**

1. **Hỏng tiếng Việt khi nạp bằng sqlcmd**: file SQL UTF-8 *không BOM* bị
   sqlcmd đọc sai codepage → dữ liệu lưu vào DB đã hỏng sẵn (`Tài liệu` →
   `TÃ i liá»u`), app hiển thị sai mà không phải lỗi app.
   `seed-systemtest.sql` / `reset-systemtest.sql` đã lưu **UTF-8 có BOM** nên
   nạp thẳng như trên là đúng. Nếu tự viết file SQL mới: lưu UTF-8 BOM, hoặc
   thêm cờ `-f i:65001` khi chạy. Kiểm chứng sau nạp (xem byte thật, đừng tin
   hiển thị console — console vẫn lem nhem kể cả khi DB đúng):
   ```sql
   SELECT title, CAST(title AS VARBINARY(40)) FROM MentoraDB.dbo.materials;
   -- đúng khi thấy các mã như A101 (ơ), A31E (ả), EA00 (ê)
   ```
2. **Lỗi `QUOTED_IDENTIFIER`** khi chạy seed cũ (`seed-node-levels-demo.sql`)
   qua sqlcmd: sqlcmd mặc định OFF. 2 file systemtest đã `SET QUOTED_IDENTIFIER ON`
   ngay đầu file nên không dính; file cũ muốn chạy phải bọc thêm dòng SET đó.

## 3. Tài khoản test (seed tạo, mật khẩu biết trước)

| Email | Mật khẩu | Vai trò | Dùng cho |
|---|---|---|---|
| `st.gv1@mentora.test` | `password` | LECTURER | Chủ dữ liệu chính: 2 material, path, lớp |
| `st.gv2@mentora.test` | `password` | LECTURER | Chủ material "ST GV2" — tái hiện bug #12 và mọi ca ownership |
| `st.sv1@mentora.test` | `password` | STUDENT | ACTIVE trong lớp — làm level test |
| `st.sv2@mentora.test` | `password` | STUDENT | ACTIVE trong lớp — ca 2 student (bảng kết quả, concurrency) |
| `st.sv3@mentora.test` | `password` | STUDENT | KHÔNG thuộc lớp nào — test join/PENDING/reject |
| `admin1@mentora.com` | `admin123` | ADMIN | App tự tạo lúc khởi động (không thuộc seed) |

## 4. Dữ liệu seed (số lượng biết trước — để viết expected)

- Môn `ST-DEMO` (ACTIVE) + question bank.
- Material: `ST Cơ bản` (GV1, **6 câu**), `ST Nâng cao` (GV1, **4 câu**),
  `ST GV2` (GV2, **3 câu**, **mới nhất → đứng đầu danh sách**). Mỗi material
  1 nội dung TEXT. Mọi câu hỏi: 4 phương án, **đáp án đúng luôn là
  "Phương án A (đúng)…"** → tự động hóa chọn đáp án không cần đọc hiểu câu.
- Path `Lộ trình ST` (GV1): Node 1 (Level 1: 3 câu từ M1, không giới hạn
  lượt, pass 5/10; Level 2: 3 câu từ M2, **maxAttempts 2**, pass 7/10) →
  Node 2 (tiên quyết Node 1; Level 1: 2 câu từ M1).
- Lớp `Lớp System Test` (GV1, OPEN, mã mời **`ST2026OK`**), sv1+sv2 ACTIVE,
  mọi node VISIBLE. Không có attempt sẵn — attempt sinh ra khi chạy test.
- ⚠ **ID không cố định giữa các lần seed** (IDENTITY tăng dần) — test/tài liệu
  phải tra id theo code/email (`WHERE code = N'ST-DEMO'`), tuyệt đối không
  hardcode số id.

## 5. Reset về trạng thái gốc

```powershell
sqlcmd -S localhost,1433 -U sa -P <mật-khẩu> -C -i reset-systemtest.sql
sqlcmd -S localhost,1433 -U sa -P <mật-khẩu> -C -i seed-systemtest.sql
```

`reset-systemtest.sql` xóa **mọi thứ thuộc môn ST-DEMO** kể cả dữ liệu sinh ra
khi test (attempt + snapshot, grant, QnA, node_progress, material tạo thêm,
assessment legacy) theo thứ tự FK-safe; **giữ lại 5 tài khoản st.\***. Seed
idempotent — chạy lại khi đã có dữ liệu sẽ tự bỏ qua, không nhân đôi.

## 6. Ca lỗi đã biết, tái hiện tất định (đừng tưởng là hỏng môi trường)

**ST-BUG12** (business-process §9 #12, test-scenarios MT-19): đăng nhập
`st.gv1@mentora.test` → mở `/lecturer/materials?subjectId=<id ST-DEMO>`
(không kèm `materialId`) → **whitelabel 500**. Nguyên nhân: material đứng đầu
danh sách thuộc GV2, GET auto-select rồi gọi `findQuestionsByMaterial` →
`requireOwned` ném lỗi không được bắt. Đăng nhập `st.gv2@mentora.test` mở
cùng URL → 200 bình thường. Đã xác nhận thật 2026-07-13 bằng cả trình duyệt
lẫn curl. Trạng thái: **chưa chốt hướng sửa** — khi được sửa, ca này phải đổi
expected và đây chính là dấu hiệu test hoạt động đúng.

Lưu ý kỹ thuật khi tự viết seed thêm: 2 INSERT trong cùng transaction có thể
trùng `created_at` đến nano-giây → `ORDER BY created_at DESC` ra thứ tự tùy ý.
Muốn thứ tự danh sách tất định phải gán `created_at` tường minh lệch nhau
(seed hiện tại đã làm, lệch 1 giây/material).

## 7. Chạy app + kiểm tra nhanh sau khi dựng

```powershell
.\mvnw.cmd spring-boot:run
```

Smoke tay 3 bước (~1 phút): (1) login `st.gv1` → vào dashboard lecturer;
(2) login `st.sv1` → mở lớp `Lớp System Test` → roadmap có 2 node;
(3) ca ST-BUG12 ở mục 6 ra đúng 500. Ba bước này pass = môi trường dựng đúng.
