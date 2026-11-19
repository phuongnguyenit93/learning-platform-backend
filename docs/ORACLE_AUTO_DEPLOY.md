# GitHub Actions auto-deployment to Oracle A1

Workflow: `.github/workflows/build-publish.yml`.

Every push to `main` (or manual **Run workflow**) runs Gradle tests, builds
and publishes the `linux/amd64` and `linux/arm64` images to GHCR, then deploys
the **exact** `sha-<commit>` image to the Oracle Ubuntu VM over SSH. Deploy does
not begin if the build or image publish fails.

## GitHub Actions secrets

Configure these **repository secrets** in the backend repository under
**Settings -> Secrets and variables -> Actions**:

| Name | Value |
| --- | --- |
| `ORACLE_SSH_HOST` | Oracle VM public IP / SSH hostname |
| `ORACLE_SSH_USER` | SSH username (currently `ubuntu`) |
| `ORACLE_SSH_PRIVATE_KEY` | Entire *private* Ed25519 key dedicated to GitHub deployment (not `.pub`) |
| `ORACLE_SSH_KNOWN_HOSTS` | Verified SSH server host key record for this host |

The dedicated SSH **public** key must be listed in the VM user's
`~/.ssh/authorized_keys`. Private keys never belong in Git or Docker images.
Keep the old personal SSH key separate for emergency access. Confirm the
host-key fingerprint directly on the VM before trusting the `known_hosts`
record. The job enforces `StrictHostKeyChecking=yes` and `BatchMode=yes`.

## Oracle VM prerequisites

- The working directory is `/opt/learning-platform/backend`.
- Both `compose.yaml` and `compose.oauth.yaml` already exist there.
- Existing Oracle datasource and Google OAuth secrets remain at
  `/opt/learning-platform/secrets/` and are mounted by Compose; Actions does
  **not** read or replace them.
- The SSH user can run `sudo -n docker compose ...` without an interactive
  password. Verify with `sudo -n docker compose version` **on the VM**.
- Docker can pull the published GHCR image (if the GHCR package is private,
  registry authentication must be configured for the root Docker user on VM).
- Port `127.0.0.1:8080` serves the Spring Boot backend on the VM.

## Deploy and verification

After a successful build, GitHub Actions uses the image
`ghcr.io/<owner>/<repo>:sha-<commit>` and runs, on the VM:

```bash
cd /opt/learning-platform/backend
sudo -n env BACKEND_IMAGE="ghcr.io/<owner>/<repo>:sha-<commit>" \
  docker compose -f compose.yaml -f compose.oauth.yaml \
  up -d --pull always --no-deps backend
```

The job verifies both `GET http://127.0.0.1:8080/api/v1/categories`
(real Oracle-backed request) and `GET http://127.0.0.1:8080/api/auth/config`
(Google login must be enabled). If readiness fails, the job fails and shows
the container status; it **does not automatically roll back**. See the
Actions run logs and inspect the VM using the personal SSH key.

The job deliberately does **not** SSH as root, disable host verification, copy
Compose files from GitHub, alter VM secret files, or run `docker compose down`.
Changes to either Compose file in Git must be synchronized to the VM
separately before they take effect; this automation deploys **backend images**.

## Troubleshooting

- `Missing repository secret ...`: add the named secret to the repository.
- `Permission denied (publickey)`: the private key secret does not correspond
  to a public key in `authorized_keys`, or SSH file permissions are wrong.
- `Host key verification failed`: compare the VM's real SSH host fingerprint
  with the `ORACLE_SSH_KNOWN_HOSTS` value; never bypass verification.
- `sudo: a password is required`: the VM's SSH user needs a safe,
  non-interactive deployment permission configuration.
- `pull access denied`: check GHCR package visibility and Docker registry
  login on the VM.
- `Backend did not pass the HTTP readiness checks`: inspect
  `sudo docker compose -f compose.yaml -f compose.oauth.yaml logs --tail=100 backend`
  on the VM. Do not post logs that include sensitive information.
