importScripts('https://www.gstatic.com/firebasejs/10.4.0/firebase-app-compat.js');
importScripts('https://www.gstatic.com/firebasejs/10.4.0/firebase-messaging-compat.js');

// Actual Firebase project configuration provided by the user
firebase.initializeApp({
  apiKey: "AIzaSyDILT_YoD1JIDfCbb6K-r4pqt8vuLLH7ew",
  authDomain: "mindcare-59fdb.firebaseapp.com",
  projectId: "mindcare-59fdb",
  storageBucket: "mindcare-59fdb.firebasestorage.app",
  messagingSenderId: "579956676071",
  appId: "1:579956676071:web:149c28c89a6bc91c74d942"
});

const messaging = firebase.messaging();

messaging.onBackgroundMessage((payload) => {
  console.log('[firebase-messaging-sw.js] Received background message ', payload);
  const notificationTitle = payload.notification.title;
  const notificationOptions = {
    body: payload.notification.body,
    icon: '/assets/icons/icon-72x72.png'
  };

  self.registration.showNotification(notificationTitle, notificationOptions);
});
