# HomeHub

**HomeHub App Logo**

HomeHub is an Android app (Kotlin) that allows South African households to book trusted home services (plumbing, electrical, cleaning, painting, pest control, and more) from their phones, track their jobs, and choose Cash or Card once the job is completed.

**Team:** Amadyani Systems, Vuyolwethu Sicwebi, Musasa Ncukana, and Cwenga Sokupe

**Module:** Open Source Coding (Module code: `OPSC6321`)

**Final PoE tag:** `final-poe`

## 1. Purpose of the App

Finding a reliable plumber or electrician can be stressful. HomeHub provides users with one convenient place to request a service, track its status (Pending → In Progress → Completed), and pay after the job is completed.

The app continues working with a poor or unavailable internet connection and supports English, isiZulu, and isiXhosa.

## 2. Features

| Feature | How It Works |
|---|---|
| Register / Login | ASP.NET Core REST API. Passwords are hashed using ASP.NET `PasswordHasher` and are never stored in plain text. |
| Single Sign-On (SSO) | Google Sign-In (Credential Manager) → Firebase Authentication → The API verifies the Google ID token. |
| Settings | Notifications on/off, default payment method, and language preferences. |
| REST API + Database | ASP.NET Core API hosted on Azure App Service (`https://homehub20261105134433-f6dzbebrh6frcjet.canadacentral-01.azurewebsites.net/`) with SQLite through Entity Framework. Swagger is available at `/swagger`. |
| Offline Mode with Sync | Requests made without an internet connection are saved in a local SQLite database and submitted automatically when the phone reconnects. |
| Push Notifications | Firebase Cloud Messaging (topic `homehub_updates`) and a local "Requests synced" notification. |
| Multi-language Support | English, isiZulu, and isiXhosa, selectable in Settings. |
| NoSQL Database | Cloud Firestore stores service reviews and profile data. |
| Blob Storage | Firebase Storage stores profile pictures, which are compressed before uploading. |
| Booking and Payments | Validated service bookings, request tracking, and Cash/Card payment options. |

## 3. Design Considerations

- **Colours:** Navy (`#0B3273`) for trust, orange (`#F7941D`) for action buttons, and a cream background (`#FFF3E0`).
- **Simple Navigation:** One home screen with large buttons and a "Back to Home" link on every screen.
- **Error Handling:** Every input is validated. Network and Firebase failures display friendly messages without crashing the app.
- **Offline-First Bookings:** A booking that cannot reach the server is saved on the phone instead of being lost.
- **Fast Failure:** The API client uses short timeouts so that a missing connection is detected quickly.
- **Security:** Hashed passwords, token-verified SSO, and Firestore/Storage rules that require authentication.
- **Logging:** `Log.d / Log.i / Log.e` with the tag `HomeHub` shows what happens at each step.

**Login Page Screenshot**

## 4. Architecture

- Android app (Kotlin, Retrofit) → ASP.NET Core API → SQLite
- Android app → Firebase (Authentication, Firestore, Storage, Cloud Messaging)
- Android app → Local SQLite (offline request queue and cached services list)

### How Offline Mode with Sync Works

1. **While online:** Opening *Services* downloads the services list and saves a copy in the local SQLite table `cached_services`.
2. **Without an internet connection:** *Services* displays the saved copy immediately, allowing the user to access the booking screen.
3. When the user taps *Submit Request* offline (or the server cannot be reached), the request is saved in the local table `pending_requests`.
4. *My Requests* displays these saved requests as **Waiting to Sync** cards. They come from the phone's own database, so server calls cannot remove them.
5. A network callback in `HomeHubApp.kt` detects when a working connection returns and calls `SyncManager`. It sends the saved requests to the API in the order they were created, deletes each request after successful submission, and displays a **Requests synced** notification.
6. Requests that the server permanently rejects (for example, a booking date that is now in the past) are removed so that they do not block the queue.

### Firebase Sign-In

Email/password users receive a silent anonymous Firebase session (`FirebaseHelper.ensureSignedIn`) before any Firestore or Storage operation. This is necessary because the security rules require `request.auth != null`.

## 5. GitHub and GitHub Actions

- The repository was initialised with this README, and we worked on separate branches (`feature/cloud`, `feature/offline-lang`), merging them through pull requests.
- We committed regularly, and the final commit is tagged `final-poe`.
- **GitHub Actions** (`.github/workflows/android.yml`) runs on every push and pull request. It sets up JDK 17, runs the unit tests (`ValidationUtilsTest`, `RequestStatusHelperTest`), builds the debug APK, and uploads the test report and APK.
- `google-services.json` is stored as an encrypted GitHub secret (`GOOGLE_SERVICES_JSON`) and is not included in the repository.

**Passing Workflow Screenshots**

## 6. Running the Project

1. Clone the repository and open it in Android Studio.
2. Add your own `google-services.json` file to the `app/` folder.
3. Set `BASE_URL` in `network/ApiClient.kt` to your API address.
4. Run the app on a physical Android phone (Android 7.0 or newer).

### Firebase Setup Required

- **Authentication:** Enable Google and Anonymous sign-in.
- **Firestore:** Create the database and publish security rules that require authentication for `reviews` and `users`.
- **Storage:** Set it up (Blaze plan required) and publish security rules that require authentication for `profile_photos`.
- **Cloud Messaging:** Send test push notifications to the topic `homehub_updates`.

## 7. Testing

### Automated Tests

Unit tests cover email validation, password rules, and the request status flow.

Run the tests using:

```bash
./gradlew testDebugUnitTest
```

GitHub Actions automatically runs these tests on every push.

### Manual Test: Offline Mode with Sync (Physical Phone)

1. With the internet **on**, open *Services* and wait 3 seconds for the list to be cached.
2. Return to Home, then turn **Flight Mode on and Wi-Fi/mobile data off**.
3. Open *Services*. A message should appear: "You are offline. Showing saved services."
4. Choose a service, complete all fields using a future date, and tap *Submit Request*.
5. Open *My Requests*. A **Waiting to Sync** card should appear.
6. Turn Wi-Fi back on. After a few seconds, the card should disappear and a **Requests synced** notification should appear.

### Troubleshooting

| Problem | Cause and Fix |
|---|---|
| "Services are still loading" offline | The cache is empty. Open *Services* once while online to cache the services. |
| No "Waiting to Sync" card | The phone may still be online. Turn off Wi-Fi and mobile data, not only Flight Mode. |
| Review or photo fails with `PERMISSION_DENIED` | Firestore/Storage security rules may not be published, or Anonymous sign-in may be disabled. |
| Photo upload fails with "Object does not exist" | Storage may not be configured. The Blaze plan is required. |
| Build error "google-services.json is missing" | Place the file inside `app/`, not in the project root. |

## 8. Publishing Preparation

The signed release build, app icon, phone screenshots, and Play Console upload are available in `/publishing`.

## 9. AI Tools Used

- **Planning:** Claude helped us divide the final PoE into two parts (cloud and API work, and app experience and quality work) and suggested a Git branching order to minimise merge conflicts.
- **Deployment Help:** Claude explained how to configure the Azure API address in `ApiClient.kt`, how to prevent the SQLite file from being wiped during deployment, and how to enable Swagger for our demonstration.
- **Debugging:** We described our errors to Claude, and it helped us identify their causes.

## 10. References

Firebase (2026a) *Firebase Authentication*. Google. Available at: `https://firebase.google.com/docs/auth` (Accessed: 2 November 2026).

Firebase (2026b) *Cloud Firestore*. Google. Available at: `https://firebase.google.com/docs/firestore` (Accessed: 2 November 2026).

Firebase (2026c) *Cloud Storage for Firebase*. Google. Available at: `https://firebase.google.com/docs/storage` (Accessed: 4 November 2026).

Firebase (2026d) *Firebase Cloud Messaging*. Google. Available at: `https://firebase.google.com/docs/cloud-messaging` (Accessed: 3 November 2026).

Microsoft (2026a) *Azure App Service overview*. Microsoft Learn. Available at: `https://learn.microsoft.com/en-us/azure/app-service/overview` (Accessed: 3 November 2026).

Microsoft (2026b) *Deploy an ASP.NET Core web app to Azure App Service*. Microsoft Learn. Available at: `https://learn.microsoft.com/en-us/azure/app-service/quickstart-dotnetcore` (Accessed: 4 November 2026).

---

# Release Notes

## Version 2.0 (Final PoE)

Updates since the Part 2 prototype:

### New Innovative Features

- **Single Sign-On:** Sign in using Google.
- **Offline Mode with Sync:** Book a service without an internet connection. The request is stored on the phone, displayed as "Waiting to Sync", and submitted automatically when the connection is restored.
- **Push Notifications:** Real-time alerts through Firebase Cloud Messaging, along with notifications when offline requests are synchronised.
- **Multi-language Support:** English, isiZulu, and isiXhosa.
- **Service Reviews:** Stored in and retrieved from Cloud Firestore (NoSQL).
- **Profile Pictures:** Uploaded to Firebase Storage (blob storage).

### Improvements

- The API is hosted online, allowing the app to run on physical Android phones.
- The services list is cached and loaded immediately, allowing users to book services offline.
- Short network timeouts improve offline detection.
- Waiting requests now have their own container and are displayed before the server call, ensuring they do not disappear.
- Profile photos are resized and compressed before uploading.
- Firestore and Storage operations wait for a Firebase session, and error messages display the actual cause of failures.
- The notification toggle now enables or disables push notifications.
- The payment method is sent in a language-independent format, ensuring compatibility across all supported languages.
- Automated tests and a GitHub Actions build pipeline have been added.

### Fixes

- Failed network calls no longer result in lost bookings.
- The "Waiting to Sync" card is no longer removed when the request list loads.

## Version 1.0 (Part 2 Prototype)

Register/login, service booking, request tracking, payments (Cash/Card), profile, settings, and a REST API with SQLite.