/* Web FCM service worker for dev token issuance/testing */
self.addEventListener("install", () => {
  self.skipWaiting();
});

self.addEventListener("activate", (event) => {
  event.waitUntil(self.clients.claim());
});

self.addEventListener("push", (event) => {
  if (!event.data) {
    return;
  }

  let payload = {};
  try {
    payload = event.data.json();
  } catch (_) {
    payload = { notification: { title: "RE:FIT", body: event.data.text() } };
  }

  const title = payload?.notification?.title || payload?.data?.title || "RE:FIT";
  const body = payload?.notification?.body || payload?.data?.body || "New notification arrived.";

  event.waitUntil(
    self.registration.showNotification(title, {
      body,
      data: payload?.data || {},
    })
  );
});

self.addEventListener("notificationclick", (event) => {
  event.notification.close();
  event.waitUntil(self.clients.openWindow("/dev/chat-test.html"));
});
