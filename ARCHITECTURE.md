# PIXAULT – System Architecture Documentation

## 1. High-Level Architecture
Pixault follows a **Multitier (Layered) Architecture** designed for security-centric desktop applications. The system is decoupled into distinct layers to ensure separation of concerns and a robust security perimeter.

### Architectural Tiers:
*   **Presentation Layer**: JavaFX-based GUI utilizing FXML and CSS.
*   **Application Layer**: Controllers managing user workflows and UI state.
*   **Service Layer**: Business logic for Encryption, Steganography, OTP, and Sessions.
*   **Data Access Layer (DAL)**: Persistent DAOs (User, OTP, Session, Share).
*   **Persistence Layer**: MySQL 8.x relational storage.

## 2. Frontend Architecture
The frontend utilizes the **Model-View-Controller (MVC)** pattern.
*   **Views**: Declarative FXML defining high-fidelity Neumorphic interfaces.
*   **Controllers**: Asynchronous handlers that delegate heavy tasks to the Service Layer.
*   **Async Bridge**: Uses `javafx.concurrent` and `Platform.runLater` to maintain UI responsiveness.

## 3. Backend Architecture
The backend is a high-performance Java 21 heavy-client.
*   **Core Services**: `AuthController` (Auth Orchestration), `SessionManager` (Persistence-backed), `SteganoEngine` (Core Algorithm).
*   **Thread Management**: Dedicated `ExecutorService` per controller to offload cryptographic and SMTP tasks.

## 4. Database Architecture
Utilizes **MySQL 8.x** with **InnoDB** storage.
*   **Schema**: Normalized schema optimized for fast lookups on hashed tokens and identifiers.
*   **Data Integrity**: Foreign keys ensure consistent relationships between Users, Sessions, and Audit Logs.

## 5. Authentication Architecture
The system implements a **JavaFX-Native Inline OTP Authentication** model.
1.  **Identity Verification**: Dual-identifier login (Username/Email) + Argon2id password validation.
2.  **Inline Challenge**: OTP verification occurs directly within the application (no browser redirects).
3.  **Persistent Session**: Sessions are stored in the DB, HMAC-signed, and bound to a hardware-based **Device Fingerprint**.
4.  **Security Hierarchy**: Sessions are validated against the DB on every sensitive action, supporting instant revocation.

## 6. Communication Flow
*   **Internal**: Direct method calls between Layers.
*   **External (Share)**: `ShareHttpServer` serves one-time assets via HTTP.
*   **External (Auth)**: SMTP (Jakarta Mail) for transactional OTP delivery.

## 7. Module Interaction Diagram Explanation
*   **UI Controllers** -> Interact with **AuthController**.
*   **AuthController** -> Orchestrates **UserDAO**, **OTPDAO**, and **EmailService**.
*   **SessionManager** -> Synchronizes state between application memory and **SessionDAO**.
*   **StegoEngine** -> Orchestrates **EncryptionUtils** and **BufferedImage** manipulation.

## 8. Request-Response Lifecycle (Authentication)
1.  **Login Event**: User submits credentials.
2.  **Auth Check**: `AuthController` validates pwd -> Checks `isEmailVerified`.
3.  **OTP Trigger**: If unverified, `VerifyEmailController` is loaded, OTP is sent.
4.  **Verification**: User enters OTP -> `OTPDAO` validates hash -> `UserDAO` updates status.
5.  **Sessioning**: `SessionManager` creates DB-backed session -> Dashboard loads.

## 9. Security Architecture
"Secure by Design":
*   **Zero-Knowledge Storage**: Only Argon2 hashes and encrypted keys are stored.
*   **OTP Hardening**: SHA-256 hashing for OTP storage with strict TTL and attempt limits.
*   **Session Revocation**: A "Global Logout" mechanism invalidates all active session tokens in the DB upon security events.

## 10. Design Patterns Used
*   **DAO**: For data persistence abstraction.
*   **Singleton**: For global services like SessionManager.
*   **Command/Task**: For asynchronous UI-logic separation.
*   **Utility**: For stateless cryptographic operations.
