# matcha-auth-service

JWT authentication service for the Matcha platform.

## Responsibilities

- User registration and login
- RS256 JWT issuance (access token: 15 min, refresh token: 7 days)
- Multi-session refresh token management (hashed, stored in DB)
- Login rate limiting: 5 failures per 15 min per email
- Telegram account linking via 6-digit OTP

## Endpoints

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/api/v1/auth/register` | Public | Register new account |
| POST | `/api/v1/auth/login` | Public | Login, receive tokens |
| POST | `/api/v1/auth/refresh` | Public | Refresh access token |
| POST | `/api/v1/auth/logout` | Bearer | Revoke one refresh token |
| POST | `/api/v1/auth/logout-all` | Bearer | Revoke all sessions |
| GET | `/api/v1/auth/me` | Bearer | Current user info |
| POST | `/api/v1/auth/telegram/link-code` | Bearer | Generate Telegram OTP |
| POST | `/api/v1/auth/telegram/verify` | Public | Link Telegram chat ID |

Swagger UI: `http://localhost:8081/swagger-ui.html`

## Running locally

```bash
# Start PostgreSQL
docker run -d --name matcha-auth-db \
  -e POSTGRES_DB=matcha_auth \
  -e POSTGRES_USER=matcha \
  -e POSTGRES_PASSWORD=matcha \
  -p 5432:5432 postgres:16-alpine

# Run with dev profile (RSA keys pre-configured)
./gradlew bootRun --args='--spring.profiles.active=dev'
```

## Environment variables (production)

| Variable | Description |
|----------|-------------|
| `DB_URL` | PostgreSQL JDBC URL |
| `DB_USERNAME` | Database user |
| `DB_PASSWORD` | Database password |
| `RSA_PUBLIC_KEY` | PEM-encoded RSA public key |
| `RSA_PRIVATE_KEY` | PEM-encoded RSA private key (PKCS8) |
| `SERVER_PORT` | HTTP port (default: 8081) |
