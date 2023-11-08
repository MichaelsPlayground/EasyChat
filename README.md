# Firebase Chat App

Welcome to the Firebase Chat App repository! This app enables real-time chat functionality using Firebase as the backend. Below is a list of key files and their functionalities:

Source: https://github.com/bimalkaf/Android_Chat_Application

Video Tutorial: https://www.youtube.com/watch?v=jHH-ZreOs1k

## Activity Files

- `ChatActivity.java`: The main activity for individual chat conversations.
- `LoginOtpActivity.java`: Handles user authentication using OTP.
- `LoginPhoneNumberActivity.java`: Manages phone number-based user login.
- `LoginUsernameActivity.java`: Controls user login using a username.
- `MainActivity.java`: The app's entry point and primary navigation hub.
- `SearchUserActivity.java`: Allows users to search for other users to initiate chats.
- `SplashActivity.java`: Displays a splash screen while the app initializes.

## Fragment Files

- `ChatFragment.java`: Manages chat UI and logic within the chat activity.
- `ProfileFragment.java`: Handles user profile display and editing.
- `SearchUserFragment.java`: Displays user search results and options for starting a chat.

## Service File

- `FCMNotificationService.java`: Integrates Firebase Cloud Messaging for push notifications.

Feel free to explore these files to understand the structure of the app and how different components interact. The app leverages Firebase Authentication, Realtime Database, and Firebase Cloud Messaging to provide seamless chat functionality.

## Getting Started

To use this app:

1. Clone or download the repository.
2. Set up your Firebase project and update the `google-services.json` file.
3. Build and run the app on your Android device or emulator.

## Notes

- This repository provides a basic structure for a Firebase-based chat app. You can extend and customize it as per your requirements.
- Make sure to handle security and privacy aspects when implementing user authentication and chat features.

For more details about Firebase services and Android app development, refer to the official [Firebase Documentation](https://firebase.google.com/docs) and [Android Documentation](https://developer.android.com/docs).

Happy coding!

## Firebase console settings

### Authentication provider

The original version of this app worked with "**Phone number authentication**" - I changed this to 
"**Email & Password**" authentication (without any verification).

### Firestore rules

```plaintext
// Allow read/write access on all documents to any user signed in to the application
service cloud.firestore {
  match /databases/{database}/documents {
    match /{document=**} {
      allow read, write: if request.auth != null;
    }
  }
}
```

Note: when running the app the first time you will find some errors in your logcat:
```plaintext
onError
com.google.firebase.firestore.FirebaseFirestoreException: FAILED_PRECONDITION: 
The query requires an index. You can create it here: 
https://console.firebase.google.com/v1/r/project/easychat..xxx
	at com.google.firebase.firestore.util.Util.exceptionFromStatus(Util.java:113)
...
```
The means that the Firestore database needs one or more indexes. Click on the link and allow to 
create indexes or see Firebase console / Firestore Database / Indexes

### Storage rules

```plaintext
rules_version = '2';

// Craft rules based on data in your Firestore database
// allow write: if firestore.get(
//    /databases/(default)/documents/users/$(request.auth.uid)).data.isAdmin;
service firebase.storage {
  match /b/{bucket}/o {
    match /{allPaths=**} {
      allow read, write: if request.auth != null;
    }
  }
}
```

### Firebase Cloud Messaging

"*Conversion measurement and most targeting options require Google Analytics, 
which is currently not enabled for this project*"

t.b.d.