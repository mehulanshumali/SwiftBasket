# User Service

Registration, login, profiles, and password reset. It issues JWTs that Kong validates on every protected request.

| | |
| --- | --- |
| Port | 8081 |
| Database | MySQL `user_db` (`users`, `password_reset_tokens`) |
| Publishes | `user.registered` |
| Email | Password-reset links via Mailtrap SMTP |

## Endpoints

Public routes are reachable through Kong at `http://localhost:8000`.

| Method | Path | Access | Description |
| --- | --- | --- | --- |
| POST | `/api/v1/auth/register` | Public | Create an account (role `USER`) |
| POST | `/api/v1/auth/login` | Public | Return a signed JWT |
| GET | `/api/v1/auth/me` | Signed in | Current profile |
| PUT | `/api/v1/auth/me` | Signed in | Update name, email, phone, or address |
| DELETE | `/api/v1/auth/me` | Signed in | Soft-delete the account |
| POST | `/api/v1/auth/password-reset` | Public | Email a reset link (always returns success) |
| POST | `/api/v1/auth/reset-password` | Public | Set a new password with a reset token |
| GET | `/api/v1/auth/validate` | Kong | Validate a JWT and return user ID and role |
| GET | `/api/v1/users/{id}/email` | Internal | Email lookup for the notification service; not routed by Kong |
| GET | `/api/v1/auth/health` | Public | Liveness probe |

Signed-in routes read the caller from the `X-User-ID` header that Kong sets after validating the JWT; requests without it get `401`.

## Configuration

| Setting | Source | Default |
| --- | --- | --- |
| `jwt.secret` | `JWT_SECRET` | Development-only value; set a 64+ byte secret |
| `jwt.expiration` | `application.yaml` | 24 hours |
| `spring.mail.username` / `password` | `MAILTRAP_USERNAME` / `MAILTRAP_PASSWORD` | Dummy values; emails fail until set |
| `app.base-url` | `application.yaml` | `http://localhost:5173`, the storefront used in reset links |

Reset tokens expire after one hour and are single-use.

## Run

```sh
bash scripts/swiftbasket.sh start user          # from the repository root
# or, with the variables from scripts/.env.example exported
./mvnw spring-boot:run
```

API docs: http://localhost:8081/swagger-ui.html

New accounts are `USER`. To grant admin, update the role in MySQL; see the [root README](../../README.md#4-create-an-admin-and-seed-the-catalog).
