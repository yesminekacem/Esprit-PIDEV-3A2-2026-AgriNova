# 🌱 AgriNova — Digital Farm Management System

> **Repository:** `Esprit-PIDEV-3A2-2026-AgriNova`

This project was developed as part of the **PIDEV – 3rd Year Engineering Program** at **Esprit School of Engineering** (Academic Year 2025–2026).

AgriNova is a comprehensive JavaFX desktop application for modern farm management, featuring crop tracking, AI-powered task generation, disease detection, a community forum, a marketplace with PayPal checkout, and a full user authentication system with Face ID.

---

## 📸 Overview

AgriNova is a full-stack desktop application built with **JavaFX 22** and **MySQL**, designed to help farmers manage every aspect of their operation in one place — from planting to selling produce.

---

## 🚀 Features

| Module | Key Features |
|---|---|
| 👤 **User** | Registration, Login, Face ID, Email OTP, Password Reset, Admin Panel |
| 🌾 **Crops** | Crop tracking, task management, AI task generation, disease detection, PDF export |
| 💬 **Forum** | Posts, comments, reactions, notifications, AI title/tag generation, profanity filter |
| 🛒 **Marketplace** | Product listings, cart, orders, PayPal checkout, delivery map |

---

## 🛠️ Tech Stack

### Backend
| Layer | Technology |
|---|---|
| **Language** | Java 17 |
| **Build Tool** | Maven |
| **Database** | MySQL 8 (JDBC) |
| **Password Security** | BCrypt (jBCrypt 0.4) |
| **Email** | JavaMail via MailerSend SMTP |
| **Face Recognition** | Face++ API (free tier) |
| **AI Tasks** | Flask (Python) + Ollama LLM — local REST API |
| **AI Forum** | Groq API (LLaMA 3.3 70B) |
| **Disease Detection** | YOLOv8 (Python) — local REST API |
| **Payment** | PayPal REST API (Sandbox) |
| **Excel Export** | Apache POI 5.2.5 |
| **PDF Export** | iText 5.5.13 |
| **Webcam** | Sarxos Webcam Capture 0.3.12 |

### Frontend
| Layer | Technology |
|---|---|
| **UI Framework** | JavaFX 22 |
| **Styling** | CSS (JavaFX stylesheets) |
| **Layout** | FXML |

---

## 🏗️ Architecture

```
AgriNova/
├── src/main/java/tn/esprit/
│   ├── MainFX.java                    # App entry point
│   ├── user/                          # User module (auth, admin)
│   │   ├── entity/
│   │   ├── service/
│   │   └── controller/
│   ├── crop/                          # Crops & tasks module
│   │   ├── entity/
│   │   ├── dao/
│   │   ├── service/
│   │   └── controller/
│   ├── forum/                         # Community forum module
│   │   ├── entity/
│   │   ├── dao/
│   │   └── controller/
│   ├── marketplace/                   # Marketplace & orders module
│   │   ├── entity/
│   │   ├── service/
│   │   └── controller/
│   ├── navigation/                    # Routing & layout controllers
│   └── utils/                        # Shared utilities
├── src/main/resources/
│   ├── fxml/                          # All FXML view files
│   ├── styles/styles.css              # Global stylesheet
│   ├── logo.png                       # App icon
│   └── faceid.png                     # Face ID button icon
├── crop_ai_api/                       # Python Flask API (AI tasks + disease detection)
│   └── app.py
├── plant_ai_api/                      # YOLOv8 disease detection model
│   └── main.py
├── uploads/                           # User-uploaded images
├── database-updates.sql               # DB migration scripts
└── pom.xml
```

---

## 📦 Dependencies

```xml
<!-- Core -->
JavaFX 22 (controls, fxml, swing, web)
MySQL Connector 8.0.33

<!-- Security -->
jBCrypt 0.4

<!-- Email -->
javax.mail 1.6.2

<!-- AI & HTTP -->
Groq API (java.net.http.HttpClient)
Face++ API (java.net.http.HttpClient)

<!-- Data -->
Apache POI 5.2.5   (Excel)
iText 5.5.13       (PDF)
Gson 2.10.1        (JSON parsing)
Jackson 2.17.0     (JSON parsing)
org.json 20231013  (JSON parsing)

<!-- Webcam -->
sarxos/webcam-capture 0.3.12
SLF4J Simple 1.7.36

<!-- Payment -->
PayPal REST API (Sandbox) — java.net.http
```

---

## ⚙️ Getting Started

### Prerequisites

- **Java 17+**
- **Maven 3.8+**
- **MySQL 8+**
- **Python 3.9+** (for AI APIs — optional)
- **Ollama** (for local LLM task generation — optional)

### Database Setup

1. Create a MySQL database:
```sql
CREATE DATABASE agrinova;
```

2. Import the schema and run migrations:
```sql
SOURCE database-updates.sql;
```

3. Configure your DB connection in `src/main/java/tn/esprit/utils/DbConnect.java`:
```java
private static final String URL = "jdbc:mysql://localhost:3306/agrinova";
private static final String USER = "root";
private static final String PASSWORD = "yourpassword";
```

### Python AI APIs (Optional)

**Crop Task Generation:**
```bash
cd crop_ai_api
pip install flask requests
python app.py
# Runs on http://127.0.0.1:5000
```

**Plant Disease Detection:**
```bash
cd plant_ai_api
pip install flask ultralytics pillow
python main.py
# Runs on http://127.0.0.1:5000/detect
```

### External API Configuration

| API | File | Where to get credentials |
|---|---|---|
| **Face++** | `FaceIdService.java` | [faceplusplus.com](https://www.faceplusplus.com/) — free 1000 calls/month |
| **Groq AI** | env `GROQ_API_KEY` | [console.groq.com](https://console.groq.com) — free |
| **PayPal** | `PayPalConfig.java` | [developer.paypal.com](https://developer.paypal.com) — sandbox |
| **MailerSend** | `EmailService.java` | [mailersend.com](https://www.mailersend.com) — free tier |

### Run the App

```bash
# Clone the repo
git clone https://github.com/yourusername/Esprit-PIDEV-3A2-2026-AgriNova.git
cd Esprit-PIDEV-3A2-2026-AgriNova

# Build and run
mvn clean javafx:run
```

Or open in **IntelliJ IDEA** and run `MainFX.java`.

### Default Admin Account

```sql
UPDATE user SET role = 'ADMIN' WHERE email = 'your@email.com';
```

---

## 🌟 Module Details

### 👤 User Module
- Signup with **email OTP verification** (MailerSend)
- Login with **Remember Me** (AES-128 encrypted persistent token, 7-day TTL)
- **Face ID login** via webcam + Face++ API comparison (76% confidence threshold)
- Forgot password → OTP → reset flow
- Admin panel: view, search, filter, edit, ban/unban, delete, export users to Excel
- Password hashed with **BCrypt cost factor 12**
- See [`USER_MODULE.md`](USER_MODULE.md) for full documentation

### 🌾 Crop Module
- Add/edit/delete crops with image, growth stage, area, planting/harvest dates
- Task management per crop (CRUD, status tracking, cost)
- **AI Task Generation** — sends crop data to local Flask API → LLM generates 5 farming tasks
- **Disease Detection** — upload a plant image → YOLOv8 model identifies diseases
- **PDF Report** export of tasks via iText
- Data access via DAO pattern with raw JDBC

### 💬 Forum Module
- Create/edit/delete posts with images and categories
- Comments with likes
- **Emoji reactions** (ReactionType enum)
- **Notifications** system
- **AI-generated title & tags** via Groq (LLaMA 3.3 70B) when creating posts
- **Profanity filter** on post/comment content
- Card-based UI with custom `ListCell` components

### 🛒 Marketplace Module
- Product listings with images, price, quantity, category, status
- Shopping cart
- Order management with delivery address
- **Interactive delivery map** dialog (GPS coordinates)
- **PayPal Sandbox checkout** — full OAuth2 flow, order creation, approval redirect
- Order status tracking

---

## 🗂️ Key Files Reference

| File | Purpose |
|---|---|
| `MainFX.java` | App entry point, auto-login logic, icon, stage setup |
| `navigation/Router.java` | In-app navigation between pages |
| `navigation/MainLayoutController.java` | User dashboard layout + topbar |
| `utils/DbConnect.java` | MySQL singleton connection |
| `utils/SessionManager.java` | In-memory user session |
| `utils/TokenManager.java` | Persistent AES auth token |
| `utils/PasswordUtil.java` | BCrypt hash/verify |
| `utils/EmailService.java` | SMTP OTP sending |
| `utils/FaceIdService.java` | Face++ API wrapper |
| `utils/VerificationCodeManager.java` | OTP storage with expiry |
| `utils/ValidationUtil.java` | Email & password validators |
| `utils/GroqAiService.java` | Groq LLM API client |
| `crop_ai_api/app.py` | Flask API for AI task generation |
| `plant_ai_api/main.py` | YOLOv8 disease detection API |

---

## 🔒 Security Notes

| Concern | Implementation |
|---|---|
| Passwords | BCrypt cost 12 — never stored plain |
| Auth token | AES-128 encrypted, stored in OS user preferences |
| SQL injection | 100% `PreparedStatement` — no string concatenation |
| OTP codes | In-memory only, 10–15 min expiry, auto-purged |
| Banned users | Blocked at login before session creation |
| Email verification | Account unusable until OTP confirmed |

---

## 👥 Contributors

Developed by 3rd Year Engineering students — Class **3A2**

| Name | Module |
|---|---|
| Team Member 1 | User Module & Authentication |
| Team Member 2 | Crop Module & AI Integration |
| Team Member 3 | Forum Module |
| Team Member 4 | Marketplace & PayPal |

> Replace with actual team member names and GitHub profiles.

---

## 🎓 Academic Context

Developed at **Esprit School of Engineering – Tunisia**

- **Program:** PIDEV – 3rd Year Engineering
- **Class:** 3A2
- **Academic Year:** 2025–2026
- **Institution:** [Esprit School of Engineering](https://esprit.tn)

This project was developed as part of the **PIDEV – 3rd Year Engineering Program** at **Esprit School of Engineering** (Academic Year 2025–2026), as a practical application of software engineering principles including layered architecture, design patterns, API integration, and full-stack desktop development.

---

## 🏷️ GitHub Topics

```
esprit-school-of-engineering
academic-project
esprit-pidev
2025-2026
javafx
java
mysql
javafx-application
desktop-application
face-recognition
ai-integration
paypal
yolov8
```

---

## 🙏 Acknowledgments

- **Esprit School of Engineering** — for the academic framework and guidance
- **GitHub Education** — for providing free developer tools and resources to students
- [Face++](https://www.faceplusplus.com/) — free face recognition API
- [Groq](https://groq.com/) — free LLaMA inference API
- [MailerSend](https://www.mailersend.com/) — transactional email service
- [PayPal Developer](https://developer.paypal.com/) — sandbox payment API
- [Sarxos Webcam Capture](https://github.com/sarxos/webcam-capture) — open-source webcam library
- [Apache POI](https://poi.apache.org/) — open-source Excel library
- [YOLOv8 / Ultralytics](https://ultralytics.com/) — open-source object detection

---

## 📄 License

This project is for educational purposes at **Esprit School of Engineering**.
