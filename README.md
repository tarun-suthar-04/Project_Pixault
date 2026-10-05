# Pixault Secure Vault

Pixault is a JavaFX desktop vault for hiding encrypted messages inside images, authenticating users with OTP, tracking sessions, auditing security events, and creating one-time share links.

## Requirements

- JDK 21
- Maven
- MySQL 8.x
- Gmail app password or another SMTP account for OTP email delivery

## Local Configuration

The committed `src/main/resources/config.properties` file contains safe defaults only. Private values must go in an ignored local file:

```powershell
Copy-Item config.local.properties.example config.local.properties
```

Edit `config.local.properties` and set:

- `db.username`
- `db.password`
- `mail.username`
- `mail.password`
- `mail.from`
- `token.hmac.secret`

Environment variables can also override config values. Examples:

- `PIXAULT_DB_PASSWORD`
- `PIXAULT_MAIL_PASSWORD`
- `PIXAULT_TOKEN_HMAC_SECRET`

## Database Setup

Create and import the MySQL schema:

```powershell
mysql -u root -p < schema.sql
```

Update `config.local.properties` with the MySQL user that has access to `pixault_db`.

## Build

```powershell
mvn -q -DskipTests package
```

The runnable JAR is created at:

```text
target/pixault-1.0.0.jar
```

## Run

```powershell
java -jar target/pixault-1.0.0.jar
```

If using the local desktop shortcut flow, make sure `config.local.properties` exists before launching.

## Git Upload Checklist

- Rotate any secrets that were ever committed or shared.
- Confirm `config.local.properties` is not staged.
- Confirm `target/`, `share_messages/`, logs, and installer files are not staged.
- Build once locally before tagging/submitting.
