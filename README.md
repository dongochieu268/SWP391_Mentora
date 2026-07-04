# Mentora

Mentora là hệ thống quản lý học tập được xây dựng cho môn SWP391. Dự án tập trung vào ba nhóm người dùng chính: quản trị viên, giảng viên và sinh viên.

## Chức năng chính

### Quản trị viên

- Quản lý tài khoản người dùng.
- Tạo tài khoản giảng viên.
- Khóa hoặc mở khóa tài khoản.
- Quản lý môn học, môn tiên quyết và học kỳ.

### Giảng viên

- Tạo lớp học và quản lý thành viên trong lớp.
- Tạo lộ trình học cho từng môn học.
- Thêm bài học, nội dung học tập và bài kiểm tra rẽ nhánh.
- Quản lý ngân hàng câu hỏi theo môn.
- Tạo bài đánh giá, import câu hỏi từ ngân hàng câu hỏi và công bố bài đánh giá.
- Bật hoặc ẩn các node học tập trong từng lớp.
- Trả lời câu hỏi của sinh viên trong Q&A.

### Sinh viên

- Đăng ký, đăng nhập và tham gia lớp bằng mã mời.
- Xem roadmap học tập của lớp.
- Học từng node và đánh dấu hoàn thành.
- Làm bài đánh giá và xem kết quả.
- Đặt câu hỏi trong Q&A của lớp.
- Trả lời Q&A khi được giảng viên gán vai trò TA.

<<<<<<< HEAD
## Điểm nổi bật của Mentora

### 1. Xây dựng lộ trình học phân nhánh

Giảng viên có thể thiết kế lộ trình học theo node bài học và bài kiểm tra rẽ nhánh. Hệ thống hỗ trợ tự động điều hướng sinh viên dựa trên kết quả đánh giá (ĐẠT/CHƯA ĐẠT).

![Path Builder](docs/images/readme-flow/11-lecturer-path-builder.png)

### 2. Hỏi đáp trong lớp học

Sinh viên có thể đặt câu hỏi, yêu cầu hỗ trợ từ giảng viên hoặc trợ giảng. Tính năng Q&A giúp duy trì sự tương tác và hỗ trợ học tập trong lớp.

![Q&A lớp học](docs/images/readme-flow/12-student-qna.png)

### 3. Sinh viên học bài và lưu tiến độ

Mỗi node học tập bao gồm video hướng dẫn, ghi chú trọng tâm và tài liệu tham khảo. Sinh viên có thể đánh dấu bài đã hoàn thành và theo dõi tiến độ học tập.

![Sinh viên học bài](docs/images/readme-flow/13-student-lesson-video.png)

## Công nghệ sử dụng

- Java 17
- Spring Boot 4.0.6
- Spring MVC
- Thymeleaf
- Spring Data JPA
- SQL Server
- Maven Wrapper
- Lombok
- Bootstrap, jQuery, ApexCharts

## Cấu trúc thư mục

```text
src/main/java/com/edunac/mentora
├── config        Cấu hình web, session, seed tài khoản admin
├── controller    Controller cho admin, lecturer, student và auth
├── domain        Entity và enum
├── dto           Form và dữ liệu truyền ra view
├── repository    Spring Data repository
└── service       Xử lý nghiệp vụ

src/main/resources
├── application.properties
├── templates     Giao diện Thymeleaf
└── static        CSS, JS, ảnh và thư viện frontend

src/test/java     Unit test và test kiểm tra cấu trúc view
docs              Tài liệu phân tích nghiệp vụ và nghiên cứu
```

## Cài đặt môi trường

Cần chuẩn bị:

- JDK 17 trở lên.
- SQL Server.
- Maven Wrapper đã có sẵn trong dự án, không cần cài Maven riêng.

Cấu hình database nằm trong:

```text
src/main/resources/application.properties
```

Nếu máy dùng tài khoản SQL Server khác thì sửa lại `username`, `password` và `url` cho phù hợp.

## Cài đặt database

Tạo database tên `MentoraDB`, sau đó chạy script SQL trong `src/main/resources/db/schema.sql` để tạo bảng và dữ liệu nền.

Các role bắt buộc:

```text
ADMIN
LECTURER
STUDENT
```

Ứng dụng không tự tạo schema vì `spring.jpa.hibernate.ddl-auto=none`, nên cần chạy script SQL trước khi start project.

## Chạy dự án

Mở terminal/PowerShell tại **thư mục gốc của dự án** (nơi có `mvnw.cmd`, `pom.xml` và `src/`), rồi chạy:

Trên Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

Trên macOS hoặc Linux:

```bash
./mvnw spring-boot:run
```

Sau khi chạy thành công, mở:

```text
http://localhost:8080/login
```

## Tài khoản admin mặc định

Khi database đã có role `ADMIN`, project sẽ tự tạo tài khoản admin nếu tài khoản này chưa tồn tại:

```text
Email: admin1@mentora.com
Password: admin123
```

Tài khoản này chỉ dùng để chạy thử trong môi trường local.

## Lưu ý
Dự án được xây dựng cho mục đích học tập.
Hệ thống chưa được thiết kế để sử dụng trong môi trường production.
Không lưu mật khẩu, API key hoặc thông tin kết nối production trong repository.
