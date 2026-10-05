# Security Notes

## Private Configuration

Never commit real credentials or long-term secrets. Use one of these instead:

- `config.local.properties` in the project root
- Environment variables such as `PIXAULT_DB_PASSWORD`
- A secure secret manager for production deployments

The committed `src/main/resources/config.properties` file must remain placeholder-only.

## Previously Exposed Secrets

Earlier project versions contained live-looking database, Gmail, ngrok, and HMAC values in the committed config file. Treat those values as compromised.

Before using the app again:

- Rotate the Gmail app password.
- Generate a fresh `token.hmac.secret`.
- Review any database password that was previously stored in the project.
- Avoid reusing the old ngrok/public URL if it should stay private.

## Recommended HMAC Secret

Use a long random value, at least 32 characters. Example generation command:

```powershell
[Convert]::ToBase64String((1..32 | ForEach-Object { Get-Random -Minimum 0 -Maximum 256 }))
```

Store the result in `config.local.properties`:

```properties
token.hmac.secret=YOUR_RANDOM_SECRET
```

## Before Public Upload

Run a final scan for obvious secrets:

```powershell
rg -n "password=|secret=|api[_-]?key|token|gmail|ngrok" --glob "!target/**" --glob "!share_messages/**"
```

Review any result before pushing.
