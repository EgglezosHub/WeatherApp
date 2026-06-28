# Weather App ⛅️

A native Android application that provides real-time weather updates and background alerts for extreme weather conditions. This app was built using purely native Android components and standard Java libraries, prioritizing a lightweight, battery-efficient architecture.

## ✨ Features

* **Real-Time Weather:** Fetches current weather conditions and temperatures using the Open-Meteo API.
* **City Search:** Converts user-typed city names into exact coordinates using a Geocoding API.
* **GPS Location:** Uses Google's `FusedLocationProviderClient` to fetch weather for the user's exact current location.
* **Background Alerts:** Utilizes `WorkManager` to silently check for extreme weather (like thunderstorms) every 12 hours.
* **Clickable Notifications:** Triggers a high-priority push notification using a `PendingIntent` that pulls the app back to the foreground if severe weather is detected.

## 🛠️ Tech Stack

* **Language:** Java
* **UI:** Android XML (Material Design Components)
* **Networking:** `HttpURLConnection`, `ExecutorService` (No heavy third-party libraries like Retrofit)
* **Data Parsing:** Native `JSONObject`
* **Background Processing:** AndroidX `WorkManager`
* **API:** [Open-Meteo Free Weather API](https://open-meteo.com/) (No API key required)

## 🚀 How to Run the Project

1. Clone this repository to your local machine.
2. Open the project in **Android Studio**.
3. Allow Gradle to sync the required dependencies (`play-services-location` and `work-runtime`).
4. Build and run the app on an Android Emulator or a physical device running Android 8.0 (API 26) or higher.
5. *Note: Ensure location services are enabled on the testing device for the "Current Location" feature to work.*

---
*Developed as an academic project demonstrating clean Android architecture and network handling*
