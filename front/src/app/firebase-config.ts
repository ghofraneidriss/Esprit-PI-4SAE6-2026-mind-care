import { initializeApp } from "firebase/app";
import { getMessaging, Messaging } from "firebase/messaging";

// Actual Firebase project configuration provided by the user
const firebaseConfig = {
  apiKey: "AIzaSyDILT_YoD1JIDfCbb6K-r4pqt8vuLLH7ew",
  authDomain: "mindcare-59fdb.firebaseapp.com",
  projectId: "mindcare-59fdb",
  storageBucket: "mindcare-59fdb.firebasestorage.app",
  messagingSenderId: "579956676071",
  appId: "1:579956676071:web:149c28c89a6bc91c74d942"
};

const app = initializeApp(firebaseConfig);
export const messaging: Messaging = getMessaging(app);
export const VAPID_KEY = "BK_P1YhZ6L_5f7e6f8_7_5_7_5"; // Placeholder - User may need to provide the actual VAPID key
