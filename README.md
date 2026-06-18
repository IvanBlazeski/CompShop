# 🖥️ CompShop

> Android e-commerce app for computers — Diploma Thesis Project  
> Faculty of Information and Communication Technologies (FIKT), Prilep, Macedonia — 2026

---

## 📱 About

**CompShop** is a full-featured Android e-commerce application for browsing and purchasing computers. Built as a diploma thesis project using Kotlin, MVVM architecture, Firebase, and Room.

---

## ✨ Features

- 🔐 **Authentication** — Email/Password, Google, Facebook, Anonymous (Guest)
- 🖥️ **Product Listing** — Real-time computer catalog with search, filters and brand chips
- ❤️ **Favorites** — Save and view favorite computers (Firestore persisted)
- 🛒 **Cart & Checkout** — Add to cart, coupon codes, delivery options, card/cash payment
- 📦 **Order Tracking** — Real-time tracking (Placed → Processing → Shipped → Delivered)
- 🔔 **Notifications** — Global and personal push notifications via FCM
- 👤 **Profile** — Edit info, change password, profile photo (camera/gallery), stats
- 🌍 **Localization** — Macedonian 🇲🇰 and English 🇬🇧
- 🌙 **Dark/Light Mode** — System-wide theme switching
- 📊 **Stock Management** — Real-time inventory updates
- 👥 **Guest Restriction** — Guests can browse but must login to purchase

---

## 🛠️ Tech Stack

| Category | Technology |
|----------|-----------|
| Language | Kotlin |
| Architecture | MVVM |
| UI | XML Layouts, ViewBinding |
| Local DB | Room (SQLite) |
| Backend | Firebase Firestore |
| Auth | Firebase Authentication |
| Push | Firebase Cloud Messaging (FCM) |
| Analytics | Firebase Analytics |
| Image Loading | Glide |
| Async | Coroutines |

---

## 🔥 Firebase Modules

- **Firebase Authentication** — Anonymous, Email/Password, Google, Facebook
- **Firebase Firestore** — Real-time database for products, orders, users, notifications
- **Firebase Cloud Messaging** — Push notifications for order tracking updates
- **Firebase Analytics** — User behavior tracking

---

## 📸 Screenshots

### Home Screen
![Home](screenshots/home.png)

### Detail Screen
![Detail](screenshots/detail.png)

### Cart & Checkout
![Cart](screenshots/cart.png)

### Orders & Tracking
![Orders](screenshots/orders.png)

### Profile
![Profile](screenshots/profile.png)

### Notifications
![Notifications](screenshots/notifications.png)

---

## 🗂️ Project Structure

```
app/src/main/java/com/ivan/compshop/
├── CompShopApplication.kt
├── CompShopMessagingService.kt
├── data/
│   ├── local/          (Room - AppDatabase, CartDao, CartItemEntity)
│   └── repository/     (AuthRepository, CartRepository, ComputerRepository)
├── model/              (Computer, User, Order, CartItem)
└── ui/
    ├── auth/           (LoginActivity, RegisterActivity)
    ├── cart/           (CartActivity, CartAdapter, CheckoutDialog, CardPaymentDialog)
    ├── detail/         (DetailActivity)
    ├── home/           (HomeActivity, ComputerAdapter, FilterBottomSheetFragment)
    ├── notifications/  (NotificationsBottomSheet)
    ├── orders/         (OrdersActivity, OrdersAdapter)
    └── profile/        (ProfileActivity)
```

---

## 🚀 Setup

1. Clone the repository
```bash
git clone https://github.com/IvanBlazeski/CompShop.git
```

2. Open in **Android Studio**

3. Add your `google-services.json` to `app/`

4. Build and run on Android device or emulator (API 26+)

---

## 👨‍💻 Admin Panel

A local HTML admin panel (`compshop-admin.html`) is included for:
- Adding new computers to Firestore
- Sending global notifications to all users
- Sending private notifications to specific users

---

## 📋 Requirements

- Android 8.0+ (API 26+)
- Internet connection
- Firebase project with Authentication, Firestore, Messaging, Analytics enabled

---

## 👤 Author

**Ivan Blazeski**  
ICT Student — FIKT Prilep, Macedonia  
GitHub: [@IvanBlazeski](https://github.com/IvanBlazeski)

---

## 📄 License

This project is developed for academic purposes as a diploma thesis at FIKT, Prilep.
