# 💰 Personal Budgeting App

A modern Android application to help users manage and track their personal expenses with ease. The app includes budget goal setting, categorized expense tracking, image attachment, and Firebase-based backup and authentication.
https://youtu.be/LweRZM5zu3c

---

## 📱 Features

- 🔐 **User Authentication** via Firebase (Email/Password)
- 🧾 **Expense Entry Management**
  - Add, view, edit, and delete expenses
  - Attach images to each expense
- 📊 **Category-Based Tracking**
  - View total expenses per category
  - Seeded with preset categories
- 🎯 **Budget Goal Setting**
  - Set monthly min and max goals
  - Budget progress displayed
- 📂 **Export to CSV**
  - One-click export of expenses to a downloadable CSV file
- 🌐 **Firebase Integration**
  - Cloud Firestore for data
  - Firebase Storage for image upload
  - Authentication with FirebaseAuth
- 🧭 **Smooth Navigation**
  - Dashboard overview
  - Bottom navigation bar with fragment transitions
  - ScrollView and FragmentContainer managed dynamically

---

## 🛠️ Tech Stack

- **Kotlin** + **Android SDK**
- **Firebase Authentication**
- **Cloud Firestore** (Database)
- **Firebase Storage** (Image files)
- **RoomDB** (Optional local use)
- **Material UI Components**
- **MVVM-ish architecture** with FirebaseService acting as Repository

---

## 🚀 Getting Started

### Prerequisites

- Android Studio Dolphin or newer
- Android device or emulator
- Firebase project with Authentication, Firestore, and Storage enabled

### Firebase Setup

1. Create a Firebase Project at [firebase.google.com](https://firebase.google.com)
2. Enable **Email/Password Authentication**
3. Create **Firestore Database**
4. Enable **Firebase Storage**
5. Download `google-services.json` and place it inside `app/`

### Run the App

```bash
git clone https://github.com/your-username/PersonalBudgetingApp.git
cd PersonalBudgetingApp
