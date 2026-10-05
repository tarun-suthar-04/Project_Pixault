# PIXAULT – Coding Standards & Style Guide

## 1. Naming Conventions
*   **Java Classes**: `PascalCase` (e.g., `OTPDAO`, `SessionManager`).
*   **Methods/Variables**: `camelCase` (e.g., `verifyEmailOtp()`).
*   **Constants**: `SCREAMING_SNAKE_CASE`.
*   **DB Tables**: `snake_case`.

## 2. Folder Organization
*   `com.pixault.auth`: Auth orchestration and session logic.
*   `com.pixault.database`: Persistence logic (DAOs).
*   `com.pixault.ui`: View controllers.
*   `com.pixault.security`: Cryptographic primitives and email services.

## 3. Architecture Rules (Inline OTP)
*   **No Browser Redirects**: All authentication flows must be handled natively within JavaFX scenes.
*   **Atomic Transactions**: Multi-step operations (like registration + OTP) should be wrapped in single services or atomic DAO calls.

## 4. Security Standards
*   **OTP Hashing**: Never store raw OTP codes. Always use `HashUtils.sha256Hex()` before database insertion.
*   **Session Binding**: Always bind sessions to `DeviceFingerprint.generate()`.
*   **Password Hygiene**: Use `char[]` for passwords and zero them out immediately after use (`Arrays.fill(password, '\0')`).
*   **Argon2id**: Use for all identity verification; never use MD5 or SHA-1 for passwords.

## 5. UI/UX Standards
*   **Async Processing**: Never run crypto, DB, or SMTP on the JavaFX Thread. Use an `ExecutorService` and update UI via `Platform.runLater()`.
*   **Neumorphism**: All buttons and cards must follow the neumorphic shadow style defined in `style.css`.
*   **Feedback**: Always show a loading state (`setLoading(true)`) during background operations.

## 6. Database Standards
*   **DAO Isolation**: No SQL queries in Controllers. All queries must reside in the `database` package.
*   **Resource Closure**: Use `try-with-resources` for all `PreparedStatement` and `ResultSet` objects.
*   **ACID Compliance**: Use InnoDB for all tables.

## 7. Logging & Auditing
*   **SLF4J**: Use for internal debugging and error tracking.
*   **Audit Log**: Every security-sensitive action must call `shareDAO.insertAuditLog()` to ensure a permanent record.
