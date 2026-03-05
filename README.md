# 🌱 AgriNova — Digital Farm Management System

> A comprehensive JavaFX desktop application for modern farm management, featuring crop tracking, AI-powered task generation, disease detection, a community forum, a marketplace with PayPal checkout, and a full user authentication system with Face ID.

---

## 📸 Overview

AgriNova is a full-stack desktop application built with **JavaFX 22** and **MySQL**, designed to help farmers manage every aspect of their operation in one place — from planting to selling produce.

---

## 🚀 Features at a Glance

| Module | Key Features |
|---|---|
| 👤 **User** | Registration, Login, Face ID, Email OTP, Password Reset, Admin Panel |
| 🌾 **Crops** | Crop tracking, task management, AI task generation, disease detection, PDF export |
| 💬 **Forum** | Posts, comments, reactions, notifications, AI title/tag generation, profanity filter |
| 🛒 **Marketplace** | Product listings, cart, orders, PayPal checkout, delivery map |

---

## 🛠️ Tech Stack

| Layer | Technology |
|---|---|
| **Language** | Java 17 |
| **UI Framework** | JavaFX 22 |
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

---

## 📁 Project Structure

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
│   └── utils/                         # Shared utilities
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

## ⚙️ Prerequisites

- **Java 17+**
- **Maven 3.8+**
- **MySQL 8+**
- **Python 3.9+** (for AI APIs — optional)
- **Ollama** (for local LLM task generation — optional)

---

## 🗄️ Database Setup

1. Create a MySQL database:
```sql
CREATE DATABASE agrinova;
```

2. Import the schema and run migrations:
```sql
SOURCE database-updates.sql;
```

3. Configure your DB connection in:
```
src/main/java/tn/esprit/utils/DbConnect.java
```
```java
private static final String URL = "jdbc:mysql://localhost:3306/agrinova";
private static final String USER = "root";
private static final String PASSWORD = "yourpassword";
```

---

## 🤖 Python AI APIs (Optional)

### Crop Task Generation API
```bash
cd crop_ai_api
pip install flask requests
python app.py
# Runs on http://127.0.0.1:5000
```
Requires **Ollama** running locally with a compatible model.

### Plant Disease Detection API
```bash
cd plant_ai_api
pip install flask ultralytics pillow
python main.py
# Runs on http://127.0.0.1:5000/detect
```
Uses a **YOLOv8** model trained on plant disease images.

---

## 🔑 External API Configuration

### Face++ (Face ID)
1. Sign up at [faceplusplus.com](https://www.faceplusplus.com/) (free — 1000 calls/month)
2. Update credentials in `FaceIdService.java`:
```java
public static final String API_KEY    = "YOUR_API_KEY";
public static final String API_SECRET = "YOUR_API_SECRET";
```

### Groq AI (Forum title/tag generation)
1. Get a free API key at [console.groq.com](https://console.groq.com)
2. Set environment variable:
```bash
set GROQ_API_KEY=your_key_here   # Windows
export GROQ_API_KEY=your_key_here # Linux/Mac
```

### PayPal (Marketplace checkout)
1. Create a sandbox app at [developer.paypal.com](https://developer.paypal.com)
2. Update credentials in `PayPalConfig.java`:
```java
public static final String CLIENT_ID     = "YOUR_CLIENT_ID";
public static final String CLIENT_SECRET = "YOUR_CLIENT_SECRET";
public static final String API_BASE      = "https://api-m.sandbox.paypal.com";
```

### MailerSend (Email / OTP)
Update credentials in `EmailService.java`:
```java
private static final String SMTP_HOST     = "smtp.mailersend.net";
private static final String FROM_EMAIL    = "your@domain.com";
private static final String EMAIL_PASSWORD = "your_smtp_password";
```

---

## ▶️ Running the App

```bash
# Clone the repo
git clone https://github.com/yourusername/AgriNova.git
cd AgriNova

# Build and run
mvn clean javafx:run
```

Or open in **IntelliJ IDEA** and run `MainFX.java`.

---

## 🔐 Default Admin Account

After running the DB migration, create an admin account manually or via signup and update the role:
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

## 👥 Authors

Built by students at **ESPRIT** — School of Engineering, Tunisia.

---

## 📄 License

This project is for educational purposes.

