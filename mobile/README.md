# Global Setu mobile

Expo / React Native source for Android and iOS, connected to the same Java API. Includes secure token storage, registration/sign-in, role-based module navigation, record forms, quote line items, workflow actions and document uploads.

1. Install Node 22+ and run `npm install` in this folder.
2. Copy `.env.example` to `.env` and set `EXPO_PUBLIC_API_URL` to the backend origin reachable from your device. Never put secrets in EXPO_PUBLIC variables.
3. Run `npx expo install --check`, `npm run check`, then `npm start` for development. A physical device cannot reach your computer through localhost. Use an HTTPS backend for real device and production testing.
4. Run `npm run android` with Android SDK, or `npm run ios` on macOS with Xcode. Set your own application identifiers before distribution.

Native release signing, Apple/Google accounts, device acceptance tests, push notifications and store submission are not included in a source build. No native release binaries have been produced. Mobile currently lacks the web account administration and CSV export views, attachment download management, and profile/catalogue edit forms. These remain documented gaps, not completed BRD acceptance items.
