# Foon AQI Bot

**Foon AQI Bot** is a Spring Boot LINE bot that fetches real-time air quality data from IQAir, stores results, and sends rich AQI messages through LINE Messaging API.

![Java](https://img.shields.io/badge/Java-21-007396?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.5.13-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)
![Maven](https://img.shields.io/badge/Maven-C71A36?style=for-the-badge&logo=apachemaven&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-17-336791?style=for-the-badge&logo=postgresql&logoColor=white)
![LINE](https://img.shields.io/badge/LINE-Messaging_API-00C300?style=for-the-badge&logo=line&logoColor=white)
![MIT](https://img.shields.io/badge/MIT-green?style=for-the-badge)

> Experimental project for webhook-based bot architecture and AQI notification workflows.

## LINE LIFF Preview
<p align="center">
  <img src="https://github.com/user-attachments/assets/06c81b0b-98fa-4f43-b807-b4e73d2bde0e"
       alt="LINE LIFF Preview"
       width="280" />
</p>

## Features

- Realtime AQI fetch from IQAir API
- LINE broadcast and direct reply flows
- Rich Flex message output for AQI details
- Per-user daily schedule (default 07:00, Asia/Bangkok)
- User-specific AQI history (records linked to LINE user)
- REST endpoints for air quality, user settings, and history
- LIFF pages for AQI check and schedule settings

## Tech Stack

- Java 21
- Spring Boot 3.5.x
- Spring Web + Spring Data JPA + Scheduler
- PostgreSQL (Docker Compose)
- Maven Wrapper (`./mvnw`)
- LINE Messaging API
- IQAir API

## Prerequisites

- Java 21+
- LINE Messaging API channel
- IQAir API key
- Docker + Docker Compose
- ngrok (or Cloudflare Tunnel) for local webhook testing

## Setup

1. Clone this repository.
2. Create `.env` in project root (see `.env.example`).
3. Put your secrets in `.env`:

```env
IQAIR_API_KEY=your_iqair_api_key
LINE_CHANNEL_TOKEN=your_line_channel_access_token
```

4. Start PostgreSQL:

```bash
docker compose up -d
```

5. Run the app:

```bash
./mvnw spring-boot:run
```

## Usage

### Local API

Base path: `/api/air-quality`

- `GET /notify` - fetch AQI + broadcast LINE message
- `GET /fetch` - fetch AQI only
- `GET /latest` - get latest AQI record
- `GET /history` - get last 10 records (global)
- `GET /history/me?userId=<LINE_USER_ID>&limit=30` - get user-specific history
- `POST /by-location` - save latest location + push AQI to that user

Base path: `/api/users`

- `GET /me/settings?userId=<LINE_USER_ID>` - get user notification settings
- `PUT /me/settings` - update `notifyEnabled`, `notifyTime`, `timezone`

LIFF routes:

- `/liff/aqi` - get location and check nearest AQI
- `/liff/settings` - update schedule settings

### LINE Webhook (Local)

1. Start app on port `8080`.
2. Start ngrok:

```bash
ngrok http 8080
```

3. Set LINE webhook URL in Developers Console:

```text
https://<your-ngrok-domain>/api/line/webhook
```

4. Ensure:
   - Use webhook = ON
   - Response mode = Bot
   - Auto-response messages = OFF (to avoid duplicate default replies)

5. In LINE chat, send:

```text
Check Air Quality
```

## Project Structure

```text
foonbot/
├── .env.example
├── pom.xml
├── README.md
└── src/
    ├── main/
    │   ├── java/com/foonbot/aqi/
    │   │   ├── FoonAqiBotApplication.java
    │   │   ├── config/
    │   │   ├── controller/
    │   │   ├── dtos/
    │   │   ├── model/
    │   │   ├── repository/
    │   │   └── service/
    │   └── resources/application.properties
    └── test/
```

## Security Notes

- Never commit real API keys.
- Keep `.env` local only.
- Rotate keys immediately if leaked.
- For production, use managed secrets and a stable domain.
