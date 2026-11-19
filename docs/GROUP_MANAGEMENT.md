# Group management in Topic navigation (ADMIN only)

The Topic sidebar now supports managing Group nodes and the placement/order of
both Group and Module nodes. All writes are confined to the **currently open
Topic**; cross-Topic transfers and deletion are intentionally unsupported.

## HTTP API

All endpoints require a signed-in user with the live `ADMIN` role and are
covered by Spring Security at `/api/admin/**`. POST and PUT requests must send
the current session cookie and a CSRF token from `GET /api/auth/csrf`.

| Method | Path | Description |
| --- | --- | --- |
| GET | `/api/admin/topics/{topicId}/structure` | Full admin tree, including inactive/empty Groups and inactive Module placements, plus a revision |
| POST | `/api/admin/topics/{topicId}/groups` | Add a Group at the root, order = max(root Group and Module order) + 1 |
| PUT | `/api/admin/groups/{groupId}` | Change name, slug and active flag only |
| PUT | `/api/admin/topics/{topicId}/structure` | Save a complete rearranged Group/Module tree atomically |

Group input: `{ "name": "Cloud", "slug": "cloud", "active": true,
"revision": "<revision from GET>" }`. A new Group always has
`PARENT_GROUP_ID = NULL`; the Topic comes from the URL, never the payload.
Changing a Group's Topic through editing is deliberately disallowed.

The GET tree returns `{ topicId, revision, nodes }`. Each node contains
`kind` (`GROUP` or `MODULE`), `id`, `name`, `slug`, `active`, and `children`.
`GROUP.id` maps to `TOPIC_GROUP.ID`, whereas `MODULE.id` maps to
`TOPIC_GROUP_MODULE.ID` (the **placement**, not `MODULE.ID`).

The structure PUT sends `{ revision, nodes }` with each node containing only
`kind`, `id`, and `children`. The entire Topic tree must be included exactly
once, including inactive and empty nodes. Node order and nesting determine
the persisted values:

- `TOPIC_GROUP.PARENT_GROUP_ID` and `SORT_ORDER` for Group nodes;
- `TOPIC_GROUP_MODULE.GROUP_ID`, `TOPIC_ID`, and `SORT_ORDER` for Module nodes.

Siblings of either kind are assigned consecutive `SORT_ORDER` values starting
at 1. A Module at the Topic root gets `TOPIC_ID` set and `GROUP_ID = NULL`;
under a Group those values are reversed. `IS_ACTIVE` and Module metadata are
not changed by the drag-and-drop save.

## Concurrency, validation and rollback

Every admin write is `@Transactional` and takes a pessimistic lock on the
`TOPIC` row before touching the tree. The revision is a SHA-256 digest of
the physical Group and Module placement snapshot, including flags and order.
If an admin's revision is stale, the server responds with `409 Conflict`,
**without changing the tree**. Add/Edit Group also require a revision, so
they cannot silently overwrite a newer tree version.

On structure PUT the backend validates all Group IDs, Module placement IDs,
same-Topic ownership, no duplicate/missing nodes, no children under Modules,
and a maximum Group nesting depth. A Group cannot be placed inside itself or
one of its descendants. It performs updates through JPA and flushes them
inside a **single transaction**, so database failures roll all changes back.

Group slugs must match lowercase kebab-case, be unique within the Topic, and
not collide with any Module slug in the same Topic, including inactive nodes.

## Frontend behavior

- All users see the `G` / `M` type badges in the navigation tree.
- Only authenticated admins see the Add Group / Modify Group toolbar and the
  Edit Group actions beside each Group.
- Admin views show inactive Groups/Modules with an `INACTIVE` marker; the
  existing public read endpoint still filters unavailable nodes.
- Modify Group uses a local draft, with drag/drop positions before/after/inside,
  keyboard arrow move controls, Reset, Cancel, and Save Changes. No API write
  occurs before Save.
- After a successful save the sidebar refreshes. On conflict the form offers
  to reload; it does not retry using a stale revision.

## Database and deployment

The existing `TOPIC_GROUP` and `TOPIC_GROUP_MODULE` tables are sufficient.
No schema migration is needed. Both entities were previously `@Immutable`;
the ADMIN write layer now uses Spring Data JPA. Production schema management
remains `spring.jpa.hibernate.ddl-auto=none`. The Oracle datasource account
must have `SELECT`, `INSERT` and `UPDATE` permissions on `TOPIC_GROUP`, and
`SELECT`, `UPDATE` on `TOPIC_GROUP_MODULE` (plus reads for Topic/Module).

The frontend and backend must be deployed together for the new admin features
to work. Changes to repository files here do not update the Oracle VM by
themselves until the CI/CD pipeline deploys a new backend image.
