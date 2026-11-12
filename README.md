# learning-platform-backend

Backend Java 21 + Spring Boot 4.1.2 cho Learning Platform. Có Health API và hai GET API Catalog (chỉ đọc từ Oracle khi bật profile `oracle`).

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

## Oracle Autonomous Database: Learning Category / Topic (read-only)

Backend implements exactly two JPA entities, organized in
`catalog/model`, `catalog/repository`, `catalog/service`,
`catalog/controller` and `catalog/dto` packages:

| Table | Important columns | Notes |
| --- | --- | --- |
| `CATEGORY` | `ID`, `CODE`, `NAME`, `SORT_ORDER`, `IS_ACTIVE` | One category can own many topics. No translation or description columns. |
| `TOPIC` | `ID`, `CATEGORY_ID`, `CODE`, `SLUG`, `NAME`, `DESCRIPTION`, `ICON_SVG`, `STATUS`, `SORT_ORDER` | `CATEGORY_ID` is a database FK and `@ManyToOne(fetch = LAZY)` in Java. |

- `NAME` / `DESCRIPTION` are single, language-neutral application-managed values (no `lang` parameter, no translation table).
- `SLUG` is used by the frontend for navigation; no database `ROUTE_PATH`.
- `ICON_SVG` is an Oracle `CLOB` containing a **complete SVG document** (`<svg ...>...</svg>`), including `viewBox`. The GET API returns it as the `iconSvg` JSON string, so the logo can be changed in Oracle without redeploying the backend/frontend.
- **Security:** SVG markup is active content. Before displaying `iconSvg` as inline SVG in React, sanitize with a SVG-aware sanitizer such as DOMPurify (SVG profile), disallow scripts, event handlers, foreignObject and external resources. Never inject raw database SVG directly with `dangerouslySetInnerHTML`. The FE is not switched to database logos in this backend-only change.
- `STATUS`: `AVAILABLE`, `COMING_SOON`, `HIDDEN`. The read API excludes hidden topics and topics in inactive categories.
- Both Entities are Hibernate `@Immutable`. Repositories expose only read methods; service queries use read-only transactions. There is **no REST create/update/delete** endpoint. This is not a substitute for database authorization: use a SELECT-only DB user if strict DB-level enforcement is required.

### Prepare tables manually

1. In Oracle Autonomous Database, create or choose an application schema/user; do **not** use `ADMIN` as the production application's username.
2. Connect to SQL Worksheet as the **schema owner** and execute `sql/01_learning_catalog.sql` once. This creates the two tables and PK, FK, UNIQUE, CHECK constraints and an index.
3. Optionally execute `sql/02_learning_catalog_sample.sql` once for the three existing topic cards. Modify subsequent data directly in Oracle SQL Worksheet or your preferred SQL client.

No SQL script is executed by Spring Boot. `spring.jpa.hibernate.ddl-auto=none` in the Oracle profile means Hibernate **never creates or alters schema**.

### Kết nối Oracle qua Docker Compose secrets (Ubuntu A1)

Không lưu JDBC URL, username hoặc password trong `application.yml`, `compose.yaml`, Docker image hay biến môi trường container. Profile `oracle` đọc trực tiếp file được mount ở `/run/secrets/` qua `spring.config.import=optional:configtree:/run/secrets/`. Tên từng file phải đúng như sau:

| File mount vào container | Spring property | Nội dung |
| --- | --- | --- |
| `/run/secrets/spring.datasource.url` | `spring.datasource.url` | JDBC URL TCPS, đầy đủ `(description=...)` từ Oracle Console |
| `/run/secrets/spring.datasource.username` | `spring.datasource.username` | User đọc dữ liệu, ví dụ `LEARNING_READ` |
| `/run/secrets/spring.datasource.password` | `spring.datasource.password` | Mật khẩu user đó |

**TLS không Wallet:** trong Oracle Console → Database connection, chọn *TLS authentication = TLS*, copy **đầy đủ** connection string `learningplatform_low` và thêm prefix `jdbc:oracle:thin:@`. Ví dụ định dạng (chỉ minh họa; không phải service thật):

```text
jdbc:oracle:thin:@(description=(address=(protocol=tcps)(port=1521)(host=adb.ap-osaka-1.oraclecloud.com))(connect_data=(service_name=YOUR_ACTUAL_LOW_SERVICE_NAME))(security=(ssl_server_dn_match=yes)))
```

Không cần tải/mount Wallet trong cấu hình TLS này. Whitelist IP outbound của VPS A1 ở ACL của Autonomous Database.

**Khuyến nghị user read-only:** các bảng nằm ở schema `LEARNING_PLATFORM`. Từ SQL Worksheet của chủ schema, cấp quyền cho user riêng (`ADMIN` tạo user trước nếu chưa có):

```sql
GRANT SELECT ON LEARNING_PLATFORM.CATEGORY TO LEARNING_READ;
GRANT SELECT ON LEARNING_PLATFORM.TOPIC TO LEARNING_READ;
```

User `LEARNING_READ` cũng cần quyền `CREATE SESSION`. `compose.yaml` đặt `CURRENT_SCHEMA=LEARNING_PLATFORM` cho mỗi kết nối Hikari bằng `ALTER SESSION`, giúp JPA truy vấn bảng không cần tiền tố schema; **không cấp thêm quyền**. Có thể đổi schema bằng `ORACLE_SCHEMA` nếu cần.

#### 1. Tạo ba secret trên VPS

Đảm bảo máy A1 có Docker Engine và plugin `docker compose`. Sao chép `compose.yaml` và `scripts/setup-docker-secrets.sh` của **phiên bản source mới** lên thư mục làm việc, ví dụ `/opt/learning-platform/backend/`.

```bash
cd /opt/learning-platform/backend
sudo bash scripts/setup-docker-secrets.sh
```

Script sẽ hỏi JDBC URL, username, password **với chế độ ẩn ký tự** và tạo ba file tại `/opt/learning-platform/secrets/`. Mỗi file được lưu với owner `root`, group `10001`, mode `0440`; thư mục có mode `0750`. Tất cả nằm ngoài source Git, không truyền giá trị qua lệnh `docker run -e`, `--env-file` hoặc tham số CLI.

Lý do group `10001`: Dockerfile hiện chạy Java bằng user/group `10001:10001`. Docker Compose standalone dùng **bind mount file** để cung cấp secrets; mode/owner của host có hiệu lực trong container, không được Compose tự đổi sang user khác. Không dùng `chmod 644` hoặc lưu mật khẩu trong Git.

#### 2. Kiểm tra cấu hình trước khi đổi container

```bash
cd /opt/learning-platform/backend
sudo docker compose -f compose.yaml config --quiet
sudo docker compose -f compose.yaml pull
```

Image trên GHCR phải đã chứa code JPA và tích hợp secrets này. Push source và đợi workflow `Build and Publish Backend` thành công **trước khi deploy**. `config --quiet` kiểm tra cú pháp Compose mà không hiển thị các giá trị secret.

#### 3. Chuyển từ container `docker run` cũ sang Compose

Nếu trên A1 đã có container cũ tên `learning-platform-backend` chiếm port 8080, tạm dừng nó (giữ nguyên container để rollback):

```bash
sudo docker stop learning-platform-backend
sudo docker compose -f compose.yaml up -d
sudo docker compose -f compose.yaml ps
curl -fsS http://127.0.0.1:8080/api/v1/health
curl -fsS http://127.0.0.1:8080/api/v1/categories
curl -fsS http://127.0.0.1:8080/api/v1/topics
```

Compose tạo container mới riêng, chỉ mở `127.0.0.1:8080` để Nginx tiếp tục reverse proxy HTTPS. Nếu kết nối Oracle thất bại, xem log (không chia sẻ log nếu có chứa thông tin nhạy cảm):

```bash
sudo docker compose -f compose.yaml logs --tail=100 backend
```

**Rollback** nếu backend mới không chạy:

```bash
sudo docker compose -f compose.yaml down
sudo docker start learning-platform-backend
```

Chỉ xóa container cũ sau khi kiểm tra backend mới hoạt động. Khi thay mật khẩu, chạy lại script, sau đó `sudo docker compose -f compose.yaml up -d --force-recreate` để ứng dụng nạp secret mới.

**Giới hạn bảo mật cần hiểu:** đây là *Docker Compose file-backed secrets* cho một VPS, không phải `docker secret create` của Docker Swarm. Dữ liệu bí mật vẫn tồn tại dưới dạng file trên ổ đĩa host, được hạn chế bằng quyền filesystem; người có quyền root/Docker trên VPS vẫn có thể đọc được. Nếu cần secret được quản lý và mã hóa bởi Swarm hoặc secret manager, phải triển khai theo cơ chế khác.

Khi không bật profile `oracle`, Health API vẫn chạy mà không cần database hoặc các file secrets. Khi bật `oracle` mà không có datasource hợp lệ, Spring Boot sẽ không kết nối thành công; `ddl-auto: none` ngăn Hibernate tạo hay sửa bảng.

### Read endpoints

| Method | Endpoint | Behavior |
| --- | --- | --- |
| GET | `/api/v1/categories` | Active categories, ordered by category sort order |
| GET | `/api/v1/topics` | Visible topics with raw SVG and category DTO, ordered by category then topic sort order |

No `lang` parameter or POST, PATCH, PUT or DELETE mapping exists.

```bash
curl -i 'http://127.0.0.1:8080/api/v1/topics'
```

Spring Data JPA uses Oracle JDBC, Hibernate, and HikariCP (maximum five DB connections). For local and CI integration tests, an in-memory H2 database is used; no production Oracle schema or data is modified by the test suite.
