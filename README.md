# learning-platform-backend

Backend Java 21 + Spring Boot 4.1.2 cho Learning Platform. Bước khởi tạo chỉ cung cấp API kiểm tra triển khai; chưa cấu hình Oracle Database hoặc xác thực người dùng.

## API test

`GET /api/v1/health`

Response (HTTP 200, `application/json`):

```json
{
  "status": "UP",
  "service": "learning-platform-backend"
}
```

## Chạy local (Windows)

Yêu cầu JDK 21. Sử dụng Gradle Wrapper đi kèm, không cần cài Gradle riêng:

```powershell
.\gradlew.bat test bootJar
java -jar build\libs\learning-platform-backend.jar
```

Mở `http://localhost:8080/api/v1/health` hoặc chạy:

```powershell
Invoke-RestMethod http://localhost:8080/api/v1/health
```

Có thể đổi port bằng biến môi trường `PORT`.

## Docker

Build JAR trước khi build image:

```powershell
.\gradlew.bat test bootJar
docker build -t learning-platform-backend:local .
docker run --rm -p 8080:8080 learning-platform-backend:local
```

Mở `http://localhost:8080/api/v1/health` để kiểm tra. Base image `eclipse-temurin:21-jre` hỗ trợ ARM64 (Oracle Ampere A1) và x86_64; khi build image trên GitHub Actions cần chọn kiến trúc phù hợp với VPS.

Khi triển khai lên Oracle VPS, chỉ nên cho phép truy cập từ Internet qua HTTPS/reverse proxy; Spring Boot có thể lắng nghe nội bộ ở cổng 8080.
