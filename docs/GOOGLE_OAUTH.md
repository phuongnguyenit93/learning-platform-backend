# Google OAuth2/OIDC + Oracle session

## Current deployment status

This release wires the login and role flow in source code. Google login remains
**disabled until OAuth client credentials are provisioned**. Do not enable it
without Google OAuth client ID/secret and an HTTPS reverse proxy.

Database migration: `sql/22_authentication.sql` (run **once** as the schema
owner `LEARNING_PLATFORM`). It creates `APP_USER`, `APP_ROLE`,
`APP_USER_ROLE`, `SPRING_SESSION`, and `SPRING_SESSION_ATTRIBUTES`, and
seeds roles `USER` and `ADMIN`. Schema management remains manual:
`spring.jpa.hibernate.ddl-auto=none` and
`spring.session.jdbc.initialize-schema=never` in production. Never rerun this
DDL blindly; Oracle commits DDL statements independently.

## Google Cloud Console

1. Configure the OAuth consent screen with the necessary users/test users.
   Request only `openid email profile`; no Google API permissions are needed.
2. Create an OAuth Client of type **Web application**.
3. Add the exact **Authorized redirect URI**:
   `https://api.nguyendodinhphuong.name.vn/login/oauth2/code/google`.
   For local testing, add `http://localhost:8080/login/oauth2/code/google`
   as a second URI. The backend must receive HTTPS forwarded headers on
   production.
4. Keep Client ID/Secret in VPS secret files, never in React, Git or the
   Cloudflare Worker.

## Oracle A1 deployment

Production `compose.yaml` continues working without OAuth. To enable it:

```bash
cd /opt/learning-platform/backend
sudo bash scripts/setup-google-oauth-secrets.sh
sudo docker compose -f compose.yaml -f compose.oauth.yaml config --quiet
sudo docker compose -f compose.yaml -f compose.oauth.yaml up -d
```

The optional `compose.oauth.yaml` mounts two read-only secret files into
`/run/secrets/`, where Spring Boot imports them through `configtree:`:

- `spring.security.oauth2.client.registration.google.client-id`
- `spring.security.oauth2.client.registration.google.client-secret`

It also sets `GOOGLE_AUTH_ENABLED=true`. The base Compose config doesn't
require these files or enable login, so old deployments stay functional.

Nginx should forward the public host and TLS scheme, for example:

```nginx
proxy_set_header Host $host;
proxy_set_header X-Forwarded-Host $host;
proxy_set_header X-Forwarded-Proto $scheme;
proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
```

Only trust forwarded headers from your Nginx reverse proxy. The backend
should remain bound to `127.0.0.1:8080`.

The React site calls `https://api.nguyendodinhphuong.name.vn` directly
with `credentials: include`, bypassing Cloudflare Worker execution.
For local frontend work, set
`VITE_BACKEND_API_BASE_URL=http://localhost:8080/api/v1` in an untracked
`.env.local`; use `AUTH_FRONTEND_URL=http://localhost:5173` in your local
backend environment when testing Google login end to end.

## HTTP contract

| Route | Response/behavior |
| --- | --- |
| `GET /api/auth/config` | `{ "googleLoginEnabled": true/false }` |
| `GET /oauth2/authorization/google` | Redirect to Google (only when enabled) |
| `GET /login/oauth2/code/google` | Spring Security callback, sets session cookie and redirects to frontend |
| `GET /api/auth/me` | `{ id, email, displayName, avatarUrl, roles }` or 401 |
| `GET /api/auth/csrf` | `{ token, headerName }` for cookie-authenticated POST requests |
| `POST /api/auth/logout` | 204 and session invalidation; requires CSRF header |
| `/api/admin/**` | Only signed-in accounts with active `ADMIN` role (401/403 otherwise) |
| `GET /api/admin/users?page=0&size=20` | Admin-only paginated list of accounts, statuses and roles (max size 50) |

The Oracle-backed session expires after 30 days of inactivity. The persistent
browser cookie is valid for 90 days from issuance (it survives browser restarts).
This is a client cookie expiry, not a server-enforced absolute session lifetime.
Logout invalidates the session server-side.

## First ADMIN

Google users are always registered as `USER` on first login. After your own
account has signed in, verify its `GOOGLE_SUB` in `APP_USER` using Oracle
SQL Worksheet, then execute this **once for your verified subject**:

```sql
INSERT INTO APP_USER_ROLE (USER_ID, ROLE_ID)
SELECT u.ID, r.ID FROM APP_USER u CROSS JOIN APP_ROLE r
WHERE u.GOOGLE_SUB = :verified_google_sub AND r.CODE = 'ADMIN'
  AND NOT EXISTS (
    SELECT 1 FROM APP_USER_ROLE ur
    WHERE ur.USER_ID = u.ID AND ur.ROLE_ID = r.ID
  );
COMMIT;
```

The server checks roles against Oracle on admin requests, so an admin role
revocation takes effect without waiting for an old OIDC token to expire. An
account can be disabled with `APP_USER.STATUS='DISABLED'`, which blocks
login and admin API access.

## Separate database user

If the Spring Boot datasource uses a non-owner account, it needs read access
to the existing catalog tables **plus** these object grants (replace the
grantee with a real database user):

```sql
GRANT SELECT, INSERT, UPDATE ON LEARNING_PLATFORM.APP_USER TO APP_RUNTIME;
GRANT SELECT ON LEARNING_PLATFORM.APP_ROLE TO APP_RUNTIME;
GRANT SELECT, INSERT, DELETE ON LEARNING_PLATFORM.APP_USER_ROLE TO APP_RUNTIME;
GRANT SELECT, INSERT, UPDATE, DELETE ON LEARNING_PLATFORM.SPRING_SESSION TO APP_RUNTIME;
GRANT SELECT, INSERT, UPDATE, DELETE ON LEARNING_PLATFORM.SPRING_SESSION_ATTRIBUTES TO APP_RUNTIME;
```

Ensure the runtime connection resolves `LEARNING_PLATFORM` as its current
schema (or references qualified table names). These grants are guidance only:
the backend never executes GRANT/CREATE statements on startup.
