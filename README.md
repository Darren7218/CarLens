# CarLens 🚗🔧 — Offline Android Vehicle Garage, Fuel Log & Maintenance Tracker

**CarLens** is a **100% offline, privacy-first native Android application** built with **Kotlin, Jetpack Compose (Material 3), Room SQLite, and Clean Architecture**. It helps car owners manage multiple vehicles, log fuel fill-ups, track recurring maintenance schedules, evaluate monthly budgets, and store maintenance records locally.

---

## ✨ Features

### 1. 🏎 Garage & Vehicle Profiles
- Manage multiple vehicles with detailed specifications: Make, Model, Year, Plate Number, VIN, Fuel Type, and initial odometer readings.
- Quick vehicle switcher and summary statistics for each vehicle in your garage.

### 2. ⛽ Fuel Logging & Efficiency Analytics
- Log odometer readings, fuel volume (L / gal), total price, and station notes.
- Automatic fuel economy calculation (e.g., L/100km, km/L, or MPG).
- Historical fuel price charts and running cost per kilometer.

### 3. 🛠 Service & Maintenance Care Schedules
- Configurable maintenance intervals by mileage and/or time duration (e.g., engine oil, brake pads, tire rotations, coolant flushes).
- Visual status indicators: Normal, Due Soon, and Overdue.
- Scheduled local reminders and notifications for upcoming service intervals.

### 4. 📊 Monthly Cost & Budget Evaluations
- Detailed spending breakdown across fuel, servicing, repairs, parts, insurance, and road tax.
- Set monthly budget limits and visualize actual expenditure against targets.

### 5. 💾 Offline Backup & Restore
- Full database export and import via JSON backup files.
- Zero cloud dependency, zero external analytics, and zero network permissions required.

---

## 🏗 Architecture & Tech Stack

- **UI:** Modern declarative Jetpack Compose with Material 3.
- **Architecture:** Clean Architecture with multi-module modularization (`:app`, `:domain`, `:data`, `:ui`, `:platform`).
- **Database:** Local Room SQLite with relational entities.
- **Dependency Injection:** Hilt / Dagger.
- **Concurrency:** Kotlin Coroutines & Flow.

---

## 🛠 Building & Testing

```bash
# Run all unit tests
./gradlew test

# Assemble debug build
./gradlew assembleDebug
```
