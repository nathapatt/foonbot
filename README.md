# Foon AQI Bot

**Foon AQI Bot** is a Spring Boot LINE bot for checking air quality, saving user-specific AQI history, sending scheduled notifications, and generating short health guidance from recent AQI history, designed with Thai users in mind.

![Java](https://img.shields.io/badge/Java-21-007396?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.5.13-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)
![Maven](https://img.shields.io/badge/Maven-C71A36?style=for-the-badge&logo=apachemaven&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-17-336791?style=for-the-badge&logo=postgresql&logoColor=white)
![LINE](https://img.shields.io/badge/LINE-Messaging_API-00C300?style=for-the-badge&logo=line&logoColor=white)
![Groq](https://img.shields.io/badge/Groq-LLM-black?style=for-the-badge)
![MIT](https://img.shields.io/badge/MIT-green?style=for-the-badge)

> Mini project for a LINE-based AQI assistant tailored for Thailand, where air pollution is a recurring public-health issue and LINE is already part of many people's daily communication.

## LINE LIFF Preview

<p align="center">
  <img src="https://github.com/user-attachments/assets/83a2d5f5-d6a5-4633-af4e-f50a1cefec90"
       alt="LINE LIFF Preview"
       width="280" />
</p>

## Features

- Real-time AQI fetch from IQAir
- LINE reply, push, and broadcast messaging flows
- Flex message response for AQI details
- LIFF page for checking AQI from the user's current location
- LIFF page for AQI history and trend
- LIFF page for daily notification settings
- Per-user AQI history linked to LINE user ID
- Daily scheduled notifications by user time and timezone
- Health-guideline chat reply generated from AQI history
- Groq integration with safe fallback to rule-based health text

## Why Thailand

This project is aimed at Thai users, especially in places where PM2.5 and seasonal haze are a serious concern, such as Chiang Mai and other high-risk areas in Thailand.

Instead of asking users to install a separate app, Foon AQI Bot uses LINE because it is already one of the most familiar and widely used messaging platforms in Thailand. That makes AQI updates, alerts, and health guidance easier to access in a channel people already check every day.

## Project Sections

The LINE rich menu is designed around 4 user-facing sections:

1. `Check AQI Right Now`
   Opens LIFF and asks for location to fetch nearby AQI.
2. `History / Trend`
   Opens LIFF and shows recent AQI trend plus history list.
3. `AQI Schedule Settings`
   Opens LIFF and lets the user enable/disable daily notifications and choose time.
4. `Health Guideline`
   Sends a text command in LINE chat and returns a short health-guideline response based on recent AQI history.

## Tech Stack

- Java 21
- Spring Boot 3.5.x
- Spring Web
- Spring Data JPA
- Spring Scheduler
- PostgreSQL
- Maven Wrapper (`./mvnw`)
- LINE Messaging API + LIFF
- IQAir API
- Groq API

## Prerequisites

- Java 21+
- LINE Messaging API channel
- 3 LIFF apps/pages
- IQAir API key
- Groq API key for health-guideline generation
- Docker + Docker Compose
- ngrok or Cloudflare Tunnel for local webhook testing

## Setup

1. Clone this repository.
2. Create `.env` in the project root from `.env.example`.
3. Fill in your environment variables:

```env
IQAIR_API_KEY=your_iqair_api_key_here
LINE_CHANNEL_TOKEN=your_line_channel_token_here
GROQ_API_KEY=your_groq_api_key_here
GROQ_MODEL=openai/gpt-oss-120b
LIFF_AQI_ID=your_aqi_liff_id_here
LIFF_SETTINGS_ID=your_settings_liff_id_here
LIFF_HISTORY_ID=your_history_liff_id_here
POSTGRES_USER=your_postgres_user_here
POSTGRES_PASSWORD=your_postgres_password_here
POSTGRES_DB=your_postgres_db_here
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

- `GET /notify` - fetch AQI and broadcast a LINE message
- `GET /fetch` - fetch AQI and save it without sending LINE message
- `GET /latest` - get the latest AQI record
- `GET /history` - get the latest global AQI records
- `GET /history/me?userId=<LINE_USER_ID>&limit=30` - get user-specific AQI history
- `POST /by-location` - fetch AQI from the user's location and push AQI result to that LINE user
  - body: `{ "userId": "...", "lat": 13.75, "lon": 100.50 }`

Base path: `/api/users`

- `GET /me/settings?userId=<LINE_USER_ID>` - get notification settings for one user
- `PUT /me/settings` - update `notifyEnabled`, `notifyTime`, and `timezone`

### LIFF Routes

- `/liff/aqi` - AQI check page
- `/liff/history` - AQI history and trend page
- `/liff/settings` - daily notification settings page

All 3 LIFF pages use IDs from environment-based config instead of hardcoded IDs.

### LINE Chat Commands

Send one of these messages in LINE chat:

- `Check Air Quality`
  Replies with the latest AQI result as a Flex message.
- `คำแนะนำสุขภาพ`
- `Health Guideline`
- `Health Guidelines`
  Replies with a short health-guideline text generated from the user's recent AQI history.

### Scheduler Behavior

- The scheduler runs every minute.
- It checks each user's configured notification time and timezone.
- It sends only to users who have notifications enabled and are due at that minute.
- It requires saved location data, so the user must check AQI at least once before scheduled pushes can work.

### Health Guideline Flow

- The app loads recent AQI history for the current LINE user.
- It computes summary facts such as latest AQI, trend, min/max, average, and dominant pollutant.
- It sends that summary to Groq to generate a short Thai response.
- If Groq is unavailable, the app falls back to rule-based text.
- The LLM is used for natural-language guidance, not for medical diagnosis.

## LINE Webhook (Local)

1. Start the app on port `8080`.
2. Start ngrok:

```bash
ngrok http 8080
```

3. Set the LINE webhook URL in LINE Developers Console:

```text
https://<your-ngrok-domain>/api/line/webhook
```

4. Ensure:

- Use webhook = ON
- Response mode = Bot
- Auto-response messages = OFF

5. Test in LINE chat with:

```text
Check Air Quality
```

or

```text
คำแนะนำสุขภาพ
```

## Project Structure

```text
foonbot/
├── .env.example
├── README.md
├── docker-compose.yml
├── pom.xml
└── src/
    ├── main/
    │   ├── java/com/foonbot/aqi/
    │   │   ├── FoonAqiBotApplication.java
    │   │   ├── config/
    │   │   ├── controller/
    │   │   ├── dtos/
    │   │   ├── exception/
    │   │   ├── model/
    │   │   ├── repository/
    │   │   └── service/
    │   └── resources/
    │       ├── META-INF/
    │       ├── application.properties
    │       └── static/liff/
    │           ├── aqi/
    │           ├── history/
    │           └── settings/
    └── test/java/com/foonbot/aqi/
```

## Development Notes

- Custom Spring properties are documented in `META-INF/additional-spring-configuration-metadata.json` for better IDE support.
- `application.properties` loads `.env` automatically during local development.
- Section 4 health guidance is a chat reply, not a LIFF page.
- User history is only available after records are associated with a LINE user.

## Security Notes

- Never commit real API keys or LIFF IDs meant to stay private.
- Keep `.env` local only.
- Rotate secrets immediately if leaked.
- Use managed secrets and a stable public domain in production.
