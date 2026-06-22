import { initializeApp } from "firebase/app";
import { getDatabase } from "firebase/database";

const firebaseConfig = {
  apiKey: "AIzaSyC2RpT5yec3R-zt2KwhgYrLWKe52T_lwAg",
  authDomain: "thevillagemafia.firebaseapp.com",
  databaseURL: "https://thevillagemafia-default-rtdb.europe-west1.firebasedatabase.app",
  projectId: "thevillagemafia",
  storageBucket: "thevillagemafia.firebasestorage.app",
  messagingSenderId: "182136317203",
  appId: "1:182136317203:web:4d4db29648a151cbc126e6"
};

// Initialize Firebase
export const app = initializeApp(firebaseConfig);
export const db = getDatabase(app);
