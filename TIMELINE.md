# PIXAULT – Detailed Project Timeline (12-Week Roadmap)

This document provides a comprehensive breakdown of the development lifecycle for the Pixault Secure Vault, spanning from initial planning to final submission.

## 📅 Project Schedule Overview

| Week | Date Range | Primary Focus |
| :--- | :--- | :--- |
| **Week 1** | 20 Jan – 26 Jan 2026 | Project Planning & Requirement Analysis |
| **Week 2** | 27 Jan – 02 Feb 2026 | Architecture & Environment Setup |
| **Week 3** | 03 Feb – 09 Feb 2026 | Database Development Phase |
| **Week 4** | 10 Feb – 16 Feb 2026 | Authentication Module Development |
| **Week 5** | 17 Feb – 23 Feb 2026 | OTP & Session Management |
| **Week 6** | 02 Mar – 08 Mar 2026 | Encryption & Steganography Development |
| **Week 7** | 09 Mar – 15 Mar 2026 | Security Module Development |
| **Week 8** | 16 Mar – 22 Mar 2026 | Review & Planning Phase |
| **Week 9** | 23 Mar – 29 Mar 2026 | UI Enhancement & Optimization |
| **Week 10** | 30 Mar – 05 Apr 2026 | Integration & Validation Testing |
| **Week 11** | 06 Apr – 12 Apr 2026 | Security Testing & Debugging |
| **Week 12** | 13 Apr – 19 Apr 2026 | Documentation & Finalization |

---

## 🛠️ Weekly Activity Breakdown

### Week 1: Project Planning & Requirement Analysis
*   **Day 1-2**: Defined project vision, core problem statement, and system objectives.
*   **Day 3-4**: Conducted detailed research on LSB Steganography and AES-256-GCM encryption.
*   **Day 5-6**: Gathered functional (Vault, Hiding, Sharing) and non-functional (Security, Latency) requirements.
*   **Day 7**: Finalized project scope and created the initial Work Breakdown Structure (WBS).

### Week 2: Architecture & Environment Setup
*   **Activity 1**: Designed the N-Tier System Architecture and Module Interaction Diagrams.
*   **Activity 2**: Finalized the tech stack: Java 21, JavaFX, MySQL, Maven, and Jakarta Mail.
*   **Activity 3**: Configured the Maven `pom.xml` and initialized the project folder structure.
*   **Activity 4**: Set up the development environment and local MySQL server.

### Week 3: Database Development Phase
*   **Activity 1**: Designed the Relational Schema (ER Model) for Users, OTPs, Sessions, and Audit Logs.
*   **Activity 2**: Implemented the `DBConnection` utility and base DAO structure.
*   **Activity 3**: Executed DDL scripts and established JDBC connectivity with the MySQL instance.
*   **Activity 4**: Conducted database connectivity and basic CRUD operation testing.

### Week 4: Authentication Module Development
*   **Activity 1**: Developed `UserDAO` for persistent identity management.
*   **Activity 2**: Implemented User Registration logic with strict credential validation.
*   **Activity 3**: Integrated **Argon2id** for high-assurance password hashing.
*   **Activity 4**: Developed initial Login and Registration FXML layouts with basic validation logic.

### Week 5: OTP & Session Management
*   **Activity 1**: Developed the `OTPDAO` and `OTPService` for secure code generation and hashing.
*   **Activity 2**: Integrated **Jakarta Mail** for SMTP-based transactional OTP delivery.
*   **Activity 3**: Implemented `SessionManager` and `SessionDAO` for DB-backed persistent sessions.
*   **Activity 4**: Developed the **Verify Email** inline UI to handle post-registration verification.

### Week 6: Encryption & Steganography Development
*   **Activity 1**: Integrated **BouncyCastle** for authenticated AES-256-GCM encryption.
*   **Activity 2**: Developed the core `SteganoEngine` for Least Significant Bit (LSB) embedding.
*   **Activity 3**: Implemented the **Fisher-Yates** pixel scattering algorithm for non-sequential hiding.
*   **Activity 4**: Conducted unit testing on data integrity (verifying original data matches extracted data).

### Week 7: Security Module Development
*   **Activity 1**: Developed the **One-Time Share** module and lightweight HTTP micro-server.
*   **Activity 2**: Implemented the **Audit Log** system to record all security-sensitive events.
*   **Activity 3**: Developed the **Account Lockout** mechanism (5 failures = 15-minute lockout).
*   **Activity 4**: Implemented **Global Session Revocation** logic for enhanced security control.

### Week 8: Review & Planning Phase
*   **Activity 1**: Conducted a mid-project architectural review and code audit.
*   **Activity 2**: Refactored the `AuthController` to improve modularity and error handling.
*   **Activity 3**: Updated the technical documentation (ARCHITECTURE.md, PROJECT_CONTEXT.md).
*   **Activity 4**: Evaluated existing workflows and identified UI/UX bottlenecks.

### Week 9: UI Enhancement & Optimization
*   **Activity 1**: Applied the **Neumorphic Design System** across the entire application.
*   **Activity 2**: Optimized CSS for high-fidelity dark mode gradients and interactive hover effects.
*   **Activity 3**: Fine-tuned database query performance and connection pooling settings.
*   **Activity 4**: Resolved bugs related to image resolution and bit-depth variations.

### Week 10: Integration & Validation Testing
*   **Activity 1**: Conducted end-to-end (E2E) integration testing for the full Auth -> Vault -> Share lifecycle.
*   **Activity 2**: Validated steganography robustness and integrity across various carrier image formats.
*   **Activity 3**: Executed specialized session testing to verify hardware fingerprinting accuracy.
*   **Activity 4**: Performed user acceptance testing (UAT) on critical navigation paths.

### Week 11: Security Testing & Debugging
*   **Activity 1**: Conducted penetration testing, including brute-force and SQL injection simulations.
*   **Activity 2**: Performed stability testing and memory leak analysis for high-res image processing.
*   **Activity 3**: Finalized debugging for edge-case scenarios (e.g., interrupted SMTP connections).
*   **Activity 4**: Hardened the application against unauthorized local data access.

### Week 12: Documentation & Finalization
*   **Activity 1**: Captured high-resolution screenshots for the final project report.
*   **Activity 2**: Finalized the `ARCHITECTURE.md`, `TIMELINE.md`, and `FEATURE_LOG.md`.
*   **Activity 3**: Prepared the GitHub repository, README, and final build artifacts.
*   **Activity 4**: Successfully completed project finalization and submission preparation.

---

