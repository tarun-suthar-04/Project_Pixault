# PIXAULT Project Context

## 1. Project Overview
**Pixault** is a high-security, Zero-Trust enterprise desktop vault designed for secure data storage and sharing. It leverages advanced steganography (LSB encoding) and multi-layered encryption (AES-256-GCM) to hide sensitive information inside ordinary images. The application is built with a focus on premium aesthetics (Neumorphic design) and a modern, JavaFX-native inline OTP authentication flow.

## 2. Purpose of the System
The system provides a "Digital Vault" where users can:
*   Hide sensitive text/data inside image files (Steganography).
*   Extract hidden data from protected images.
*   Securely share encrypted assets via temporary one-time links.
*   Monitor all activities through a tamper-evident Audit Log.
*   Manage security settings including Account Lockout and Session Revocation.

## 3. Real-World Problem Solved
Traditional encrypted files are easily identifiable as targets. Pixault solves this by:
*   **Plausible Deniability**: Encrypted data is hidden inside innocuous images.
*   **Secure Sharing**: Eliminates raw encrypted file transfers via one-time, time-bound access links.
*   **Inline OTP & Lockout**: Protects against brute-force and credential stuffing without relying on external browser redirects.

## 4. Complete Tech Stack
*   **Language**: Java 21 (LTS)
*   **Build System**: Maven 3.x
*   **Application Framework**: JavaFX 21
*   **Database**: MySQL 8.x
*   **Cryptography**: BouncyCastle, Argon2id
*   **Communication**: Jakarta Mail (SMTP)
*   **Logging**: SLF4J + Logback

## 5. Frontend Technologies
*   **UI Framework**: JavaFX 21.0.2
*   **Layouts**: FXML (XML-based layouts)
*   **Styling**: Vanilla CSS (Neumorphic Design System)
*   **Design Paradigm**: Neumorphic / Glassmorphic premium interface

## 6. Backend Technologies
*   **Core Engine**: Java 21
*   **ORM/Data Access**: JDBC with DAO Pattern (UserDAO, OTPDAO, SessionDAO, ShareDAO)
*   **Security Provider**: BouncyCastle
*   **KDF**: Argon2id (for password hashing)
*   **Email**: Jakarta Mail (for transactional OTP)

## 7. Database Technologies
*   **MySQL 8.0+**: Primary relational storage using InnoDB.
*   **Key Tables**:
    *   `users`: Identity, Argon2 hashes, and lockout status.
    *   `otp_codes`: SHA-256 hashed OTPs with purpose-based TTL.
    *   `sessions`: DB-backed persistent sessions with revocation support.
    *   `audit_logs`: Tamper-evident activity tracking.
    *   `vault_shares`: Metadata for temporary share links.

## 8. Authentication & Authorization Flow
1.  **Identity Verification**: User enters identifier (Username/Email) and password. Verified via Argon2id.
2.  **Inline OTP**: If unverified or reset requested, a 6-digit OTP is sent via email and verified within the JavaFX app.
3.  **Persistent Session**: Upon success, a session is created in the DB and bound to a hardware fingerprint.
4.  **Revocation**: Sessions can be invalidated globally (e.g., on password reset) or individually.

## 10. Security Implementation
*   **Data at Rest**: AES-256-GCM.
*   **OTP Security**: Stored as SHA-256 hashes; 5-minute expiry; 5-attempt hard limit.
*   **Password Hashing**: Argon2id (m=65536, t=3, p=4).
*   **Lockout**: Account locks after 5 failed attempts for 15 minutes.
*   **Session Revocation**: Database-backed session validation on every critical action.

## 11. API Structure (Internal/HTTP)
*   **Local Server**: `GET /share?id={uuid}` - Landing page for shared files.
*   **Internal Service**: `AuthController`, `SessionManager`, `StegoEngine` exposed as Java APIs.

## 12. Folder Structure Explanation
*   `src/main/java/com/pixault/`:
    *   `auth/`: OTP logic, Session management, and AuthController.
    *   `database/`: JDBC DAO implementations (UserDAO, OTPDAO, etc.).
    *   `model/`: Domain models (User, Session, AuditLog).
    *   `security/`: CryptoUtils, Argon2, EmailService.
    *   `ui/`: View controllers (Login, Register, VerifyEmail, ForgotPassword).

## 13. Important Modules & Responsibilities
*   **AuthController**: Central orchestrator for the OTP-driven authentication lifecycle.
*   **SessionManager**: Manages DB-backed session validation and revocation.
*   **StegoEngine**: Responsible for bit-level image manipulation.
*   **EmailService**: Handles HTML-based transactional OTP emails.

## 14. Performance Optimizations
*   **Async UI Updates**: All network/crypto tasks use `ExecutorService` and `Platform.runLater()`.
*   **DB Indexing**: Fast session and OTP lookups via indexed hashes.

## 15. Future Improvements Planned
*   **FIDO2 Support**: Hardware security key integration.
*   **Blockchain Logs**: Decentralized integrity for audit logs.
