# Category and Topic administration

The Learning page uses the existing public GET endpoints for anonymous users
and ordinary accounts. An authenticated user with an active `ADMIN` role sees
the complete catalog and can create or edit entries.

## Routes

| Method | Route | Purpose |
| --- | --- | --- |
| GET | `/api/admin/categories` | All categories, including inactive and empty ones |
| GET | `/api/admin/topics` | All topics, including `HIDDEN` and topics under inactive categories |
| POST | `/api/admin/categories` | Create category with `code`, `name` |
| PUT | `/api/admin/categories/{id}` | Update category `code`, `name` |
| POST | `/api/admin/categories/{categoryId}/topics` | Create topic with `code`, `slug`, `name`, optional `description` |
| PUT | `/api/admin/topics/{id}` | Update topic `code`, `slug`, `name`, optional `description` |

The existing public GET `/api/v1/categories` still lists active categories;
the public GET `/api/v1/topics` still excludes hidden topics and those in
inactive categories. The frontend also hides categories without visible topics
from ordinary users.

## Creation rules

- A newly created category is `IS_ACTIVE=1` and receives
  `SORT_ORDER = COALESCE(MAX(CATEGORY.SORT_ORDER), 0) + 1`.
- A new topic is created within the specified category and receives
  `SORT_ORDER = COALESCE(MAX(TOPIC.SORT_ORDER) for the category, 0) + 1`;
  `STATUS=COMING_SOON`, `ICON_SVG=NULL`.
- The server normalizes codes to uppercase and slugs to lowercase, checks
  required lengths/patterns, and rejects known duplicate codes/slugs with
  HTTP 409. Concurrent conflicting inserts may still be rejected by the
  database's unique constraints.
- Update endpoints do not change Category `IS_ACTIVE` or Topic `CATEGORY_ID`,
  `SORT_ORDER`, `STATUS`, or `ICON_SVG`. Continue editing SVG directly in Oracle.
- Changing a topic slug may invalidate older URLs that use the previous slug.

## Security and deployment

All `/api/admin/**` routes require a signed-in Google OIDC principal with a
currently active `ADMIN` role in Oracle. Without Google OAuth enabled, admin
routes are denied. Non-admin users cannot read the admin lists or write them.

Cookie-authenticated POST/PUT requests require the CSRF token returned by
`GET /api/auth/csrf`: send its `headerName` and `token`, together with the
`SESSION` cookie (`credentials: include`). The frontend performs this step.

No new tables or migrations are needed. If the backend connects as a dedicated
database user instead of the schema owner, it must also have `SELECT, INSERT,
UPDATE` privileges on `CATEGORY` and `TOPIC`. The production schema remains
manually managed (`spring.jpa.hibernate.ddl-auto=none`).
