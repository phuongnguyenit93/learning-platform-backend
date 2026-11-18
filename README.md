# learning-platform-backend

Backend Java 21 + Spring Boot 4.1.2 cho Learning Platform. Có hai GET API Catalog và một GET API Structure (chỉ đọc từ Oracle khi bật profile `oracle`).

## Chạy local (Windows)

Yêu cầu JDK 21. Sử dụng Gradle Wrapper đi kèm, không cần cài Gradle riêng:

```powershell
.\gradlew.bat test bootJar
java -jar build\libs\learning-platform-backend.jar
```

Để gọi API Catalog, cần bật profile `oracle` và cấu hình kết nối database.
Ví dụ sau khi đã cấu hình Oracle:

```powershell
Invoke-RestMethod http://localhost:8080/api/v1/topics
```

Có thể đổi port bằng biến môi trường `PORT`.

## Docker

Build JAR trước khi build image:

```powershell
.\gradlew.bat test bootJar
docker build -t learning-platform-backend:local .
docker run --rm -p 8080:8080 learning-platform-backend:local
```

Khi chạy với profile `oracle` và database đã kết nối, dùng `/api/v1/topics` để kiểm tra API. Base image `eclipse-temurin:21-jre` hỗ trợ ARM64 (Oracle Ampere A1) và x86_64; khi build image trên GitHub Actions cần chọn kiến trúc phù hợp với VPS.

Khi triển khai lên Oracle VPS, chỉ nên cho phép truy cập từ Internet qua HTTPS/reverse proxy; Spring Boot có thể lắng nghe nội bộ ở cổng 8080.

## Oracle Autonomous Database: Learning Category / Topic (read-only)

The catalog is split into the `category/` and `topic/` domain packages. Each
contains its own model, repository, service, controller and DTO where needed.

| Table | Important columns | Notes |
| --- | --- | --- |
| `CATEGORY` | `ID`, `CODE`, `NAME`, `SORT_ORDER`, `IS_ACTIVE` | One category can own many topics. No translation or description columns. |
| `TOPIC` | `ID`, `CATEGORY_ID`, `CODE`, `SLUG`, `NAME`, `DESCRIPTION`, `ICON_SVG`, `STATUS`, `SORT_ORDER` | `CATEGORY_ID` is a database FK and `@ManyToOne(fetch = LAZY)` in Java. |
| `TOPIC_GROUP` | `ID`, `TOPIC_ID`, `GROUP_NAME`, `SLUG`, `PARENT_GROUP_ID`, `SORT_ORDER`, `IS_ACTIVE` | Recursive groups belong to one Topic; `SLUG` is unique within its Topic. |
| `TOPIC_GROUP_MODULE` | `ID`, `GROUP_ID`, `TOPIC_ID`, `MODULE_ID`, `SORT_ORDER`, `IS_ACTIVE` | Places a Module either inside one Group or directly under one Topic. |

- `NAME` / `DESCRIPTION` are single, language-neutral application-managed values (no `lang` parameter, no translation table).
- `SLUG` is used by the frontend for navigation; no database `ROUTE_PATH`.
- `ICON_SVG` is a nullable Oracle `CLOB`; when present it contains a **complete SVG document** (`<svg ...>...</svg>`), including `viewBox`. The GET API returns it as `iconSvg` (string or `null`).
- **Security:** SVG markup is active content. The frontend sanitizes it with DOMPurify's SVG profile and uses it as an image resource. Never inject raw database SVG directly with `dangerouslySetInnerHTML`.
- `STATUS`: `AVAILABLE`, `COMING_SOON`, `HIDDEN`. The read API excludes hidden topics and topics in inactive categories.
- Group slugs are unique per Topic; Module slugs are globally unique. The Topic Structure service also rejects a Group/Module slug collision within the visible tree.
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

Khi không bật profile `oracle`, ứng dụng chạy không cần database hoặc các file secrets, nhưng không có endpoint Catalog. Khi bật `oracle` mà không có datasource hợp lệ, Spring Boot sẽ không kết nối thành công; `ddl-auto: none` ngăn Hibernate tạo hay sửa bảng.

### Read endpoints

| Method | Endpoint | Behavior |
| --- | --- | --- |
| GET | `/api/v1/categories` | Active categories, ordered by category sort order |
| GET | `/api/v1/topics` | Visible topics with raw SVG and category DTO, ordered by category then topic sort order |
| GET | `/api/v1/topics/{topicId}/structure` | Frontend-compatible `ModuleCatalog` JSON (`version`, `root`, recursive `children`), ordered by `SORT_ORDER` and stable database IDs |
| GET | `/api/v1/modules/{moduleSlug}` | Core Module metadata resolved by its public `SLUG` |
| GET | `/api/v1/modules/{moduleSlug}/overview?lang=vi` | One localized Overview Markdown document |
| GET | `/api/v1/modules/{moduleSlug}/roadmap?lang=vi` | Ordered localized Roadmap milestones |
| GET | `/api/v1/modules/{moduleSlug}/reference?lang=vi` | Ordered localized Reference links |
| GET | `/api/v1/modules/{moduleSlug}/menu?lang=vi` | Localized Chapter/Section tree with lightweight lesson availability |
| GET | `/api/v1/modules/{moduleSlug}/knowledge/{chapterSlug}/{sectionSlug}?lang=vi` | Fully written, localized Markdown lesson and QA metadata |
| GET | `/api/v1/modules/{moduleSlug}/chapters/{chapterSlug}/insights?lang=vi` | Comparison fields of published Chapter lessons without CLOBs |

Category, Topic and Topic Structure do not take a `lang` parameter; localized
Module Overview/Roadmap/Reference/Menu/Knowledge/Insights endpoints require
`lang`. No POST, PATCH, PUT or DELETE mapping exists.

### Topic Structure (read-only API)

The Topic navigation tree is assembled from the current domain tables; it does
**not** use the former standalone `MODULE_STRUCTURE` table:

- `TOPIC_GROUP` stores recursive Group hierarchy through `PARENT_GROUP_ID`.
- `TOPIC_GROUP_MODULE` places each Module either in a Group (`GROUP_ID`) or
  directly under a Topic (`TOPIC_ID`). Its check constraint requires exactly
  one of those two placements.
- `MODULE` supplies the Module's stable `SERVICE_NAME`, public `SLUG`, display
  `NAME` and optional `SERVICE_NAME_DESCRIPTION`.
- Group `SLUG` was added to existing Oracle databases by
  `sql/21_topic_group_slug.sql`; `sql/10.sql` includes it for new table creation.

The API builds the tree in memory. A Group node uses `GROUP_<ID>` as its stable
`nodeKey`; a Module node uses `MODULE.SERVICE_NAME`. `SLUG` is the public URL
identifier and is never substituted with `nodeKey`. The response's `path` is a
computed hierarchy path such as `/module/java/java-core/java-stream-api`.
Modules are leaves; Groups may contain nested Groups and Modules. Siblings are
ordered by `SORT_ORDER`, with a deterministic Group-before-Module tie-break.

Inactive Groups hide their entire descendant subtree. Inactive placements and
Modules are omitted. A visible Topic with no active structure returns
`root.children: []`; a missing/hidden Topic or one in an inactive Category
returns 404. The service validates same-Topic parent links, cycles, blank or
unsafe slugs, and duplicate visible node keys/slugs before returning the tree.
The API is read-only and only active under the `oracle` profile. All module
content tables reference the canonical `MODULE.ID`, not the Topic Group
placement row. Moving a Module to another Group or Topic therefore does not
change its Overview, Roadmap or Reference ownership.

### Module Core, Overview, Roadmap and Reference (read-only APIs)

`MODULE` is the language-independent identity and metadata table:

| Column | Purpose |
| --- | --- |
| `ID` | Canonical database identity and FK target for Module content |
| `SERVICE_NAME` | Stable internal key, migrated from the old navigation `NODE_KEY` |
| `SLUG` | Globally unique public identifier used by frontend routes and API requests |
| `NAME` | Display name |
| `SERVICE_NAME_DESCRIPTION` | Module description |
| `SPECIAL_NOTE` | Nullable module metadata |
| `IS_ACTIVE` | Module visibility switch |

`MODULE` intentionally has no `TOPIC_ID` or `SORT_ORDER`. Current placement is
owned by `TOPIC_GROUP_MODULE`, with exactly one of `GROUP_ID` or `TOPIC_ID`
populated and one placement per Module. Topic Group ancestry stays in
`TOPIC_GROUP`.

The read-only Module Core endpoint returns:

```http
GET /api/v1/modules/java-stream-api
```

```json
{
  "id": 100,
  "serviceName": "JAVA_STREAM_API",
  "slug": "java-stream-api",
  "name": "Java Stream API",
  "description": "Learn the Java Stream API",
  "specialNote": null
}
```

Localized content remains separate and is fetched only when its tab is opened:

```http
GET /api/v1/modules/java-stream-api/overview?lang=vi
GET /api/v1/modules/java-stream-api/roadmap?lang=vi
GET /api/v1/modules/java-stream-api/reference?lang=vi
```

Overview response:

```json
{
  "slug": "java-stream-api",
  "languageCode": "vi",
  "markdownContent": "# Stream API\n\nMarkdown content..."
}
```

Roadmap response:

```json
{
  "slug": "java-stream-api",
  "languageCode": "vi",
  "roadmap": [
    {
      "id": 400,
      "title": "Nền tảng",
      "purpose": "Nắm vững nền tảng",
      "objectives": ["Hiểu Stream API", "Thực hành pipeline"],
      "sortOrder": 1
    }
  ]
}
```

Each nonblank line in `MODULE_ROADMAP_LANGUAGE.OBJECTIVE` becomes one
`objectives[]` entry. Milestones are ordered by `SORT_ORDER`, then database ID.

Reference response:

```json
{
  "slug": "java-stream-api",
  "languageCode": "vi",
  "references": [
    {
      "id": 500,
      "title": "Oracle Docs",
      "url": "https://docs.oracle.com/",
      "description": "Official documentation",
      "sortOrder": 1
    }
  ]
}
```

Reference URLs must be absolute HTTPS URLs without embedded credentials. Items
are ordered by `SORT_ORDER`, then database ID.

The current table ownership is:

| Table | Ownership |
| --- | --- |
| `MODULE_OVERVIEW` | One Markdown CLOB per `(MODULE_ID, LANGUAGE_CODE)` |
| `MODULE_ROADMAP` | Language-independent `ROADMAP_KEY`, ordering and active flag per Module |
| `MODULE_ROADMAP_LANGUAGE` | Localized milestone title, purpose and newline-delimited objectives |
| `MODULE_REFERENCE` | Language-independent URL, ordering and active flag per Module |
| `MODULE_REFERENCE_LANGUAGE` | Localized reference title and description |
| `LANGUAGE` | Shared active language lookup referenced by localized rows |

The corresponding Oracle DDL is maintained in `sql/09.sql` and `sql/12.sql`
through `sql/16.sql`; `sql/04_language.sql` creates the shared Language table.
These are manual schema scripts; do not execute CREATE scripts twice. Spring
Boot uses `ddl-auto=none` and never mutates production Oracle schema.

All four Module endpoints share the same visibility rule: the Module must be
active, have an active placement, belong to a Topic that is not `HIDDEN` and
whose Category is active, and (when placed in a Group) every Group ancestor
must be active and belong to that same Topic. A direct Topic placement is also
supported. An invisible or unplaced Module returns `404` for Core and all
content endpoints.

`lang` is required and uses an active `LANGUAGE.CODE`. The API checks actual
stored translations rather than assuming that a Module supports every active
language. Missing Overview, Roadmap or Reference translations return `404`;
there is **no locale fallback**. Missing `lang`, a blank value or a code longer
than 10 characters returns `400`; an unknown or disabled language returns
`404`. The endpoints use the same CORS origins as Category/Topic/Structure.

`learning-platform` maps `nodeKey` to its internal tree identity and maps
`slug` to the web route (`routeId`). The API intentionally does not send the
legacy `routeId = nodeKey` field, preventing the internal key from becoming a
public URL.

For example, a Group `ID=200` containing Group `ID=201`, which contains Module
`SERVICE_NAME=JAVA_STREAM_API`, is returned in this shape:

```json
{
  "version": 1,
  "root": {
    "id": "topic:10",
    "nodeKey": "TOPIC_10",
    "name": "Java",
    "kind": "GROUP",
    "path": "/module/java",
    "slug": "java",
    "description": "Java fundamentals",
    "children": [
      {
        "id": "GROUP_200",
        "nodeKey": "GROUP_200",
        "name": "Java Core",
        "kind": "GROUP",
        "path": "/module/java/java-core",
        "slug": "java-core",
        "sortOrder": 1,
        "children": [
          {
            "id": "GROUP_201",
            "nodeKey": "GROUP_201",
            "name": "Java Streams",
            "kind": "GROUP",
            "path": "/module/java/java-core/java-streams",
            "slug": "java-streams",
            "sortOrder": 1,
            "children": [
              {
                "id": "JAVA_STREAM_API",
                "nodeKey": "JAVA_STREAM_API",
                "name": "Java Stream API",
                "kind": "MODULE",
                "path": "/module/java/java-core/java-streams/java-stream-api",
                "slug": "java-stream-api",
                "description": "Java Stream API description",
                "sortOrder": 1,
                "children": []
              }
            ]
          }
        ]
      }
    ]
  }
}
```

This endpoint supplies the **navigation hierarchy**, not module lesson content
or Knowledge menu metadata. Module Core, Overview, Roadmap and Reference are
separate APIs; the frontend loads Core when a Module is selected and fetches
localized tab content only when the corresponding tab is opened.

Category and Topic Structure are implemented in the `category/` and `topic/`
domain packages. No import job, CRUD endpoint, or production DB write is
included in this read-only API.

```bash
curl -i 'http://127.0.0.1:8080/api/v1/topics'
```

Spring Data JPA uses Oracle JDBC, Hibernate, and HikariCP (maximum five DB connections). For local and CI integration tests, an in-memory H2 database is used; no production Oracle schema or data is modified by the test suite.

### Roadmap and Reference implementation notes

The current `MODULE_ROADMAP` / `MODULE_ROADMAP_LANGUAGE` and
`MODULE_REFERENCE` / `MODULE_REFERENCE_LANGUAGE` ownership, DTOs, ordering,
language behavior, and public endpoints are documented in
**Module Core, Overview, Roadmap and Reference (read-only APIs)** above.
Do not use the retired `ID_STRUCTURE`-based `sql/06_module_roadmap.sql` or
`sql/07_module_reference.sql` model for new imports or API changes.

The current implementations live under:

- `module/roadmap/{model,repository,service,controller,dto}`
- `module/reference/{model,repository,service,controller,dto}`
- `module/overview/{model,repository,service,controller,dto}`
- `module/core/{model,repository,service,controller,dto}`

The integration test `DomainModelMappingIntegrationTest` uses H2 with
`ddl-auto=create-drop`; it verifies the current Module/placement/content
ownership and the read-only HTTP contracts. Production Oracle remains
`ddl-auto=none` and is never changed by the test suite.

### MODULE_MENU and MODULE_KNOWLEDGE (read-only Knowledge APIs)

The current Knowledge model uses the canonical `MODULE.ID` through
`MODULE_MENU.MODULE_ID`, not the retired `MODULE_STRUCTURE.ID_STRUCTURE`:

- `MODULE_MENU` stores Chapter and Section nodes. A Chapter belongs to a
  Module; a Section belongs to a Chapter. `NODE_TYPE` is `CHAPTER` or
  `SECTION`, and slugs are unique within their owner scope.
- `MODULE_MENU_LANGUAGE` stores localized `TITLE` and optional `DESCRIPTION`.
- `MODULE_KNOWLEDGE` stores one active lesson metadata row per Section,
  including optional `DIFFICULT` (`BASIC`, `INTERMEDIATE`, `ADVANCED`).
- `MODULE_KNOWLEDGE_LANGUAGE` stores localized lesson `CONTENT`, `STATUS`,
  `FOCUS`, `PROBLEM_SOLVED`, `MECHANISM`, `APPLICATION_VALUE` and `SUMMARY`.

Create these tables manually using `sql/17.sql` through `sql/20.sql`; Spring
Boot never runs schema DDL. The retired `sql/08_module_knowledge_menu.sql`
depends on `MODULE_STRUCTURE.ID_STRUCTURE` and is **not** this schema.
Read-only models, repositories, services and controllers live under
`module/menu/` and `module/knowledge/`.

All Knowledge GET APIs call `ModuleCoreService.getVisibleModule` first; the
Module, active placement, all ancestor Groups and owning Topic/Category must
be visible. Chapter identity is scoped to Module; Section identity is scoped
to Chapter. The primary keys (`MODULE_MENU.ID`, `MODULE_KNOWLEDGE.ID`) remain
stable UI identities; public URLs use Chapter and Section slugs.

`GET /api/v1/modules/{moduleSlug}/menu?lang=vi` returns a lightweight object
with `slug`, `languageCode` and `chapters`. Each Chapter exposes `id`,
`slug`, `title`, `description`, `sortOrder`, `lessonCount` and ordered
`sections`. Each Section exposes `id`, `slug`, `title`, `description`,
`sortOrder`, nullable `knowledgeId`/`difficulty` and Boolean `available`/
`hasSummary`. `lessonCount` counts only available lessons. Nodes are ordered
by `(SORT_ORDER, ID)` within their parent. Untranslated Chapters or Sections
are omitted without fallback. A visible Module with **zero Chapters** returns
`200` and an empty `chapters` list; a Module with Chapters but no translations
for the requested locale returns `404`.

`GET /api/v1/modules/{moduleSlug}/knowledge/{chapterSlug}/{sectionSlug}?lang=vi`
returns `moduleSlug`, `languageCode`, localized `chapter` and `section`
objects (`id`, `slug`, `title`, `description`), `knowledgeId`,
`difficulty`, `markdownContent`, `focus`, `problemSolved`, `mechanism`,
`applicationValue` and `summary`.
`GET /api/v1/modules/{moduleSlug}/chapters/{chapterSlug}/insights?lang=vi`
returns `moduleSlug`, `languageCode`, localized `chapter` and ordered
`lessons` containing Section `id`, `slug`, `title`, `sortOrder`,
`knowledgeId`, `difficulty` and four comparison fields (`focus`,
`problemSolved`, `mechanism`, `applicationValue`). Only published lessons
appear; no CONTENT/SUMMARY CLOBs are loaded.

**Readiness policy:** only active Knowledge with non-null `DIFFICULT` and an
active-language translation with `STATUS = 'WRITTEN'` is public; `CONTENT`,
`SUMMARY`, `FOCUS`, `PROBLEM_SOLVED`, `MECHANISM` and `APPLICATION_VALUE`
must each contain a non-whitespace character. The Menu computes readiness
using an Oracle/H2 `REGEXP_LIKE` scalar projection without fetching CLOB
contents. The detail service checks the same policy in Java; insights first
check the same scalar gate and fetch only four metadata columns. Unpublished
Sections remain in Menu with `available: false` and `hasSummary: false`,
but detail returns `404`. There is no locale fallback.

Missing/blank/oversized `lang` and malformed slugs return `400`. Invisible
Modules, inactive Chapters/Sections/Knowledge, missing or inactive locales,
missing required translations and unpublished lesson detail return `404`.
All three APIs require the `oracle` profile, return JSON and use the same
CORS allowlist as the existing Module endpoints.

MockMvc/H2 tests are in
`src/test/java/com/learningplatform/backend/DomainModelMappingIntegrationTest.java`,
covering empty menu, ordering, slug scope, localization, readiness (including
whitespace-only CLOBs), inactive data and Module/Topic visibility. H2 schema
creation is test-only; no Oracle writes are performed.
