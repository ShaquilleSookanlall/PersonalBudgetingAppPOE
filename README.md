# Personal Budgeting App

A modern Kotlin-based budgeting app to help users track expenses, set budget goals, and view insights through interactive graphs. Built as part of the PROG7313 Portfolio of Evidence (Part 3), it combines local and cloud storage with gamified progress tracking to make budgeting simple and engaging.

---

## 📱 Features

- 🔐 **User Authentication** (Firebase)
- 🧾 **Add Expenses** with amount, date, category, and optional photo
- 📂 **Custom Categories** for expense classification
- 🎯 **Monthly Budget Goals** (Min & Max)
- 📅 **Filter by Date Range** to view entries
- 📊 **Graph View** showing category spending with goal comparison (MPAndroidChart)
- ✅ **Cloud Sync** via Firebase Firestore
- 💾 **Offline Storage** via RoomDB
- 🏆 **Gamification** (add your own two features here)
- 📸 **Attach Receipts** to expense entries

---

## 🛠️ Technologies Used

| Technology       | Purpose                                              |
|------------------|------------------------------------------------------|
| Kotlin (Android) | Main development language                           |
| Firebase Auth     | User registration & login                          |
| Firestore         | Online data storage & syncing                      |
| RoomDB            | Local storage for offline support                  |
| MPAndroidChart    | Graphs for spending insights                       |
| Glide / Coil      | Image handling (receipt photos)                    |
| Jetpack ViewModel | State management & UI logic (optional)             |
| DataBinding       | Efficient view binding                             |

---

## 🔄 Data Flow Overview

- Expenses are first saved to RoomDB for offline availability.
- They are also uploaded to Firestore for cross-device sync.
- Graphs are generated based on the last 30 days of expenses.
- Goal limits are shown using colored lines.

---

## 🚀 Getting Started

1. Clone the repo:
   ```bash
   git clone https://github.com/YOUR_USERNAME/PersonalBudgetingApp.git
