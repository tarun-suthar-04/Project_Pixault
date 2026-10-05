# PIXAULT – Feature Log & Development History

## 1. Core Steganographic Engine
*   **Purpose**: Concealing encrypted data within image assets.
*   **Implementation**: AES-256-GCM encryption followed by non-sequential LSB embedding using password-seeded pixel scattering.
*   **Status**: Production-ready.

## 2. Inline OTP Authentication (Upgrade)
*   **Purpose**: Replaces legacy link-based verification with a seamless, high-security inline flow.
*   **Problem Solved**: Eliminates browser redirection and phishing risks associated with email links.
*   **Implementation**: Integrated `OTPDAO` and `EmailService` for purpose-based (Verify/Forgot Pwd) transactional codes.
*   **Status**: Fully implemented.

## 3. Persistent Session Management
*   **Purpose**: Robust tracking and revocation of active authenticated users.
*   **Implementation**: DB-backed `SessionDAO` with HMAC integrity checks and hardware fingerprint binding.
*   **Features**: Supports Global Logout (all sessions) and individual session revocation.
*   **Status**: Fully implemented.

## 4. Secure One-Time Sharing
*   **Purpose**: Sharing hidden assets via time-bound, consumable links.
*   **Implementation**: `ShareHttpServer` serves landing pages; links are invalidated immediately after first successful access.
*   **Status**: Active.

## 5. Tamper-Evident Audit Log
*   **Purpose**: Real-time tracking of security events.
*   **Implementation**: Logging of logins, failures, lockouts, and vault actions with device metadata.
*   **Status**: Active.

## 6. Neumorphic Design System
*   **Purpose**: Premium UI aesthetics for a professional security application.
*   **Implementation**: CSS-driven neumorphic shadows and sleek dark mode gradients.
*   **Status**: Active.

---

## Technical Milestones & Architectural Decisions

### 1. Shift to SHA-256 OTP Hashing
*   **Decision**: Store OTPs as hashes rather than plaintext.
*   **Rationale**: Prevents a database compromise from revealing active codes.

### 2. Dual-Identifier Login (Username/Email)
*   **Decision**: Allow users to log in with either identifier.
*   **Rationale**: Improves UX and accessibility while maintaining strict uniqueness constraints.

### 3. Asynchronous Workflow Standardization
*   **Decision**: Wrapping all cryptographic and SMTP tasks in `ExecutorService`.
*   **Rationale**: Prevents UI "freezing" during long-running security operations.

### 4. Account Lockout Hardening
*   **Decision**: Implementing `login_locked_until` in the `users` table.
*   **Rationale**: Prevents brute-force attacks via persistent time-based penalties.
