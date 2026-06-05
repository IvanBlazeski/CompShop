const functions = require("firebase-functions");
const admin = require("firebase-admin");
admin.initializeApp();

// Испрати нотификација до сите корисници
exports.sendNotificationToAll = functions.https.onCall(async (data) => {
    const { title, message } = data;
    
    if (!title || !message) {
        throw new functions.https.HttpsError(
            "invalid-argument",
            "Title and message are required"
        );
    }

    try {
        const usersSnapshot = await admin.firestore()
            .collection("users")
            .get();

        const batch = admin.firestore().batch();

        usersSnapshot.forEach((userDoc) => {
            const notifRef = admin.firestore()
                .collection("users")
                .doc(userDoc.id)
                .collection("notifications")
                .doc();

            batch.set(notifRef, {
                title: title,
                message: message,
                isRead: false,
                createdAt: admin.firestore.FieldValue.serverTimestamp()
            });
        });

        await batch.commit();

        return { 
            success: true, 
            message: `Notification sent to ${usersSnapshot.size} users` 
        };
    } catch (error) {
        throw new functions.https.HttpsError("internal", error.message);
    }
});

// Испрати нотификација до специфичен корисник
exports.sendNotificationToUser = functions.https.onCall(async (data) => {
    const { userId, title, message } = data;

    if (!userId || !title || !message) {
        throw new functions.https.HttpsError(
            "invalid-argument",
            "userId, title and message are required"
        );
    }

    try {
        await admin.firestore()
            .collection("users")
            .doc(userId)
            .collection("notifications")
            .add({
                title: title,
                message: message,
                isRead: false,
                createdAt: admin.firestore.FieldValue.serverTimestamp()
            });

        return { success: true };
    } catch (error) {
        throw new functions.https.HttpsError("internal", error.message);
    }
});

// Автоматска нотификација кога се додава нов компјутер
exports.onNewComputer = functions.firestore
    .document("computers/{computerId}")
    .onCreate(async (snap) => {
        const computer = snap.data();
        const title = `New laptop added! 💻`;
        const message = `${computer.brand} ${computer.model} is now available for $${computer.price}`;

        const usersSnapshot = await admin.firestore()
            .collection("users")
            .get();

        const batch = admin.firestore().batch();

        usersSnapshot.forEach((userDoc) => {
            const notifRef = admin.firestore()
                .collection("users")
                .doc(userDoc.id)
                .collection("notifications")
                .doc();

            batch.set(notifRef, {
                title: title,
                message: message,
                isRead: false,
                createdAt: admin.firestore.FieldValue.serverTimestamp()
            });
        });

        await batch.commit();
    });