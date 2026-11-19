# Auth screens (how it works)

- **One `AuthViewModel` per destination.** Login, Register and Forgot password each get their own `hiltViewModel()` from their back stack entry, so loading and error flags don't leak between them. Navigation and session details are in `navigation-and-session.md`.
- **Field errors are checked before Firebase.** `emailError()` / `passwordError()` in `AuthViewModel` return the message to show under the field (or `""`). Each submit event sets the errors and returns early, so no loading dialog appears. Typing in a field clears only that field's error (`OnChange*Field`).
- **The 6-character password check is Register-only** (`checkLength = true`), so existing accounts with short passwords can still log in.
- **Keyboard Done on the last field submits** (`onImeAction` on Login/Register password and the Forgot password email). It goes through the same validation as the button.
- **Success toasts use the display name** ("Welcome back, <name>" / "Account created. Welcome, <name>"), not the raw email. ViewModel texts come from `strings.xml` via `@ApplicationContext`, which is only used for reading strings.
- **Register re-reads the profile at the end.** `AuthRepository.register` caches the user in DataStore before the display name exists, which showed "Welcome, ---" (the `orDefault()` placeholder). `RegisterUseCase` sets the name, then returns `profileRepository.getUserProfile()`, which saves the name to DataStore.
- **Login and register keep `users/{uid}` in Firestore up to date** through `ProfileRepository.syncUserDoc()`. It isn't awaited, so it never blocks login. Details are in `profile.md`.
- **Server errors stay in the dialog.** `onError(e)` → `OneTimeEvents.ShowError` → `ErrorDialog`. Field errors are for input problems only.
- **Emails are trimmed in `AuthRepository`** (`login`, `register`, `sendPasswordReset`), not in the UI. Validation also trims, so autocomplete's trailing space passes both.
- **Forgot password is a screen, not a sheet.** Its route is `AuthScreens.ForgotPasswordNavigation(email)`, pushed on Login with no PopUpTo. The VM reads `savedStateHandle.get<String>("email")`, because type-safe routes store args by property name. On success it sets `isPasswordResetSent`, which switches the screen to its "Check your inbox" state. Resend reuses the same event.
- **"Remember me" was removed on purpose.** Firebase keeps users signed in until logout, so the checkbox could never do anything. Don't add it back.
- **Shared UI:**
  - `ui/common/components/AppTextField.kt` is the app-wide field (label above, the placeholder inside instead of a floating label, red border and message on error, decorative leading icon, built-in password toggle, `onImeAction` for Done). It's also used in Profile and `EmailActionActivity`.
  - `ui/auth/components/AuthComponents.kt` has `AuthHeader`, `AuthGlowButton` and `SocialSignInButtons`, shared by Login, Register and Forgot password.
- **Logo placeholder:** `AuthHeader` shows a wallet icon in a `primaryContainer` circle. Swap that `Box` for an `Image` when a real logo exists (see the comment in `AuthComponents.kt`).
- **Social buttons are placeholders.** One row of icon-only buttons using the official Google (`ic_google_neutral_rd_na`, icon-only asset) and Facebook (`ic_facebook_logo`) drawables as-is, plus a tonal phone button. Each shows a "Coming soon" toast until it's wired up.
- **Insets:** auth screens use `safeDrawing.only(Top + Bottom)`. Don't use `safeContentPadding()`, because it adds gesture-nav side insets and makes the form narrow.
- **Colors:** fields and the back button use `surfaceContainerLowest` (white in light mode, dark in dark mode). Never `Color.White`.
