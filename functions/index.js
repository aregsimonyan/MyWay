const { onDocumentUpdated } = require("firebase-functions/v2/firestore");
const { onDocumentCreated } = require("firebase-functions/v2/firestore");
const { initializeApp }     = require("firebase-admin/app");
const { getFirestore }      = require("firebase-admin/firestore");
const { getMessaging }      = require("firebase-admin/messaging");

initializeApp();

// ─── Notify driver when a passenger books or cancels ─────────────────────────

exports.notifyDriverOnBooking = onDocumentUpdated(
    "trips/{tripId}",
    async (event) => {
        const before = event.data.before.data();
        const after  = event.data.after.data();

        const prevIds = before.passengerIds || [];
        const currIds = after.passengerIds  || [];

        const newIds     = currIds.filter(uid => !prevIds.includes(uid));
        const removedIds = prevIds.filter(uid => !currIds.includes(uid));

        if (newIds.length === 0 && removedIds.length === 0) return null;

        const driverId = after.driverId;
        if (!driverId) return null;

        const db = getFirestore();

        const driverDoc = await db.collection("users").doc(driverId).get();
        const fcmToken  = driverDoc.data()?.fcmToken;
        if (!fcmToken) return null;

        const from   = after.fromLocation || "?";
        const to     = after.toLocation   || "?";
        const route  = `${from} → ${to}`;
        const tripId = event.params.tripId;

        if (newIds.length > 0) {
            const passengerDoc  = await db.collection("users").doc(newIds[0]).get();
            const passengerName = passengerDoc.data()?.name || "A passenger";

            await getMessaging().send({
                token: fcmToken,
                data: { tripId, passengerName, route, type: "booking" },
                notification: {
                    title: "New Booking!",
                    body:  `${passengerName} booked your trip: ${route}`,
                },
                android: {
                    priority: "high",
                    notification: { channelId: "myway_bookings", sound: "default" },
                },
            });
        }

        if (removedIds.length > 0) {
            const passengerDoc  = await db.collection("users").doc(removedIds[0]).get();
            const passengerName = passengerDoc.data()?.name || "A passenger";

            await getMessaging().send({
                token: fcmToken,
                data: { tripId, passengerName, route, type: "cancellation" },
                notification: {
                    title: "Booking Cancelled",
                    body:  `${passengerName} cancelled their booking: ${route}`,
                },
                android: {
                    priority: "high",
                    notification: { channelId: "myway_bookings", sound: "default" },
                },
            });
        }

        return null;
    }
);


exports.updateUserAverageRating = onDocumentCreated(
    "ratings/{ratingId}",
    async (event) => {
        const rating = event.data.data();
        if (!rating || !rating.ratedUserId) return null;

        const db          = getFirestore();
        const ratedUserId = rating.ratedUserId;

        const snapshot = await db
            .collection("ratings")
            .where("ratedUserId", "==", ratedUserId)
            .get();

        if (snapshot.empty) return null;

        let total = 0;
        snapshot.forEach(doc => {
            const stars = doc.data().stars;
            if (typeof stars === "number") total += stars;
        });

        const count   = snapshot.size;
        const average = Math.round((total / count) * 10) / 10;

        await db.collection("users").doc(ratedUserId).update({
            averageRating: average,
            ratingCount:   count,
        });

        console.log(`Updated rating for user ${ratedUserId}: avg=${average}, count=${count}`);
        return null;
    }
);