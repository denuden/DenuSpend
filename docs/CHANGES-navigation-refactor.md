# Navigation / session refactor: what changed (2026-11-18)

Not committed. Review it in Android Studio's diff view (Git → Local Changes).
All Kotlin paths are under `app/src/main/java/com/gmail/vondenuelle/denuspend/`.
Total: 36 files, +311 / −666 lines.

---

## The big idea in one paragraph
**Before:** login/logout was *navigation*. ViewModels sent `OnNavigate(AppRootScreens.AuthTopLevel)`, the screen passed it up, and the nav graph caught it and forwarded it to a second `topLevelNavController`. That needed code in 3 layers, copied ~6 times.
**Now:** login/logout is *state*. `MainViewModel.session` is `Loading | LoggedIn | LoggedOut`. One `LaunchedEffect` in `AppRoot` watches it and moves the top-level NavHost to `SplashTopLevel`, `AuthTopLevel` or `MainTopLevel`. Logging out is just `profileRepository.logout()`: Firebase's auth listener sees it, and AppRoot does the switch. No screen, graph or ViewModel navigates to "auth" anymore.

```
Firebase AuthStateListener ──(signed out)───────────────┐
Login/Register success ──OnLoginOrRegisterCompleted──┐  │
                                                     ▼  ▼
                                        MainViewModel.session
                                                     │
                       AppRoot: LaunchedEffect(session) → navigate + popUpTo(everything)
                      ┌──────────────────────┼──────────────────────┐
              SplashTopLevel           AuthTopLevel             MainTopLevel
               SplashScreen            AuthNavHost     MainScreen (Scaffold + inner NavHost)
```
Why the switch is still done with *navigation*, not a plain `when`: each world is a back stack entry, and popping an entry clears every ViewModel created inside it, including the inner NavHost's screens and their Firestore listeners. A plain `when` only removes the UI. The inner screens' ViewModels would stay alive in the Activity until the app closes (this was checked in the navigation 2.8.0-beta04 source: `NavHost` doesn't clear them on dispose). See "Fix 1" below.

---

## Stage 1: Crash fix + NavBehavior cleanup

| File | Line | What |
|---|---|---|
| `navigation/NavBehavior.kt` | 14 | **New** `NavBehavior.toNavOptions()`: the single place a behavior becomes NavOptions. `ClearAll` was **removed** (only login/logout used it). `PopUpTo` stays, so you keep "popUpTo wherever I want". |
| `ui/home/screen/HomeScreen.kt` | 40 | The 15-line hand-built `NavOptions.Builder` block → `event.behavior.toNavOptions()` |
| `ui/auth/screen/LoginScreen.kt` | 106 | same |
| `ui/auth/screen/RegisterScreen.kt` | 98 | same |
| `ui/add/screen/AddScreen.kt` | 76 | same |
| `ui/add/screen/AddIncomeScreen.kt` | 75 | same |
| `ui/add/screen/AddExpenseScreen.kt` | 75 | same (also removed the `NavOptions.*` wildcard import) |
| `ui/add/screen/RecentTransactionsScreen.kt` | 82 | same |
| `ui/profile/screen/ProfileScreen.kt` | 82 | same |
| `ui/budget/screen/BudgetScreen.kt` | 67 | same |
| `ui/budget/screen/BudgetInsightsScreen.kt` | 72 | same |
| `navigation/AddNavGraph.kt` | whole file | **Crash fix.** Add Expense and History were missing an `else`, so logout also navigated the inner NavHost to a route it doesn't have (IllegalArgumentException). The whole `if (route == AuthTopLevel)` block is now gone. |

## Stage 2: Session as state (Splash / Auth / Main)

| File | Line | What |
|---|---|---|
| `MainViewModel.kt` | 17 | **New** `SessionState` (`Loading / LoggedIn / LoggedOut`) |
| `MainViewModel.kt` | 49 | `checkCurrentUser()`: runs once at startup. No user → LoggedOut. User → refresh DataStore → LoggedIn. **A network error still counts as LoggedIn** (fixes "stuck on Splash forever" when offline with an expired token). |
| `MainViewModel.kt` | 45 | `onLoginOrRegisterCompleted()`: called by Login/Register *after* they fully finish |
| `MainViewModel.kt` | 67 | `observeSignOut()`: Firebase listener. User became null → LoggedOut + clear DataStore. **Sign-ins are ignored on purpose**: Firebase signs you in mid-registration, before `RegisterUseCase` saves the display name. Swapping then would kill AuthViewModel and cancel that save. |
| `MainViewModel.kt` | — | Removed `TokenProvider`, `getTopBarTitle`, `getPhoto`, `resetPhoto` (see Stage 3) |
| `AppRoot.kt` | 20–53 | Top-level NavHost kept (Splash/Auth/MainTopLevel), but the switch is now driven by `session` in **one** `LaunchedEffect` (line 40) instead of by screens. `MainViewModel` is created here with `hiltViewModel()` outside the NavHost, so it's **Activity-scoped**, and it's passed into `MainScreen`. (Updated by Fix 1.) |
| `SplashScreen.kt` | 18 | Now a plain composable: no ViewModel, no `delay(1000)`, no lifecycle observer. Shows `app_name`. |
| `navigation/AuthNavGraph.kt` | 19 | `addAuthNavGraph(...)` → `@Composable AuthNavHost(onLoginOrRegisterCompleted)` with its own small type-safe NavHost. Transitions unchanged. |
| `navigation/Screens.kt` | 66–78 | `AppRootScreens` (`SplashTopLevel / AuthTopLevel / MainTopLevel`) **no longer implements `NavigationScreens`**, so no ViewModel can `OnNavigate` to them by accident. Deleted the unused `MainTopLevelScreens` and `AuthScreens.AuthMainNavigation`. |
| `navigation/AppNavigation.kt`, `MainNavGraph.kt`, `ProfileNavGraph.kt`, `BudgetNavGraph.kt`, `AddNavGraph.kt` | whole files | Removed the `topLevelNavController` param and every `if (route == AuthTopLevel)` block. Each screen lambda is now just `navController.navigate(route, navOptions)`. |
| `data/remote/services/auth/AuthService.kt` | 16 | **New** `fun observeAuthState(): Flow<Boolean>` |
| `data/remote/services/auth/FirebaseAuthServiceImpl.kt` | 83 | Its impl: `callbackFlow` + `AuthStateListener` (same style as your Firestore listeners) |
| `data/repositories/AuthRepository.kt` | 81–90 | **New** `hasUser()`, `observeAuthState()`, `userPreferences` (DataStore flow), `clearUserPreferences()` |
| `utils/OneTimeEvents.kt` | 14 | **New** `OnLoginOrRegisterCompleted` event |
| `ui/auth/AuthViewModel.kt` | 55, 76 | Login/Register success now send `OnLoginOrRegisterCompleted` instead of `OnNavigate(MainTopLevel, ClearAll)` |
| `ui/auth/AuthViewModel.kt` | — | Removed the `OnGetCurrentUser` branch (that was Splash's job; MainViewModel does it now) |
| `ui/auth/AuthViewModel.kt` | 172 | `NoUserException`: toast + logout, **no navigation** |
| `ui/auth/AuthScreenEvents.kt` | — | Removed `OnGetCurrentUser` |
| `ui/auth/screen/LoginScreen.kt` | 97, 109 | New `onLoginOrRegisterCompleted` param + handles `OnLoginOrRegisterCompleted` |
| `ui/auth/screen/RegisterScreen.kt` | 90, 100 | same |
| `ui/profile/ProfileViewModel.kt` | 109 | Sign out: just `profileRepository.logout()` (the `OnNavigate(AuthTopLevel)` was removed) |
| `ui/profile/ProfileViewModel.kt` | 135 | Delete account: same, no navigation |
| `ui/profile/ProfileViewModel.kt` | 334 | `NoUserException`: toast + logout, no navigation |
| `ui/add/AddViewModel.kt` | 231 | same |
| `ui/budget/BudgetViewModel.kt` | 135 | same |
| 7 screens with exhaustive `when` (AddScreen 90, AddIncome 89, AddExpense 88, RecentTransactions 96, Profile 96, Budget 81, BudgetInsights 86) | — | Added an `OneTimeEvents.OnLoginOrRegisterCompleted -> {}` branch so they still compile |

## Stage 3: The shell (MainScreen)

| File | Line | What |
|---|---|---|
| `MainScreen.kt` | 71–84 | Tabs are now a small private `BottomTab` list (route, label, icons) instead of 3 parallel lists. **Wallet and Goals have `route = null`, so they show as disabled** until they're built. |
| `MainScreen.kt` | 150 | The tab highlight is **read from the back stack** (`hierarchy.any { hasRoute(...) }`). `selectedItem` and its `LaunchedEffect` are gone. This fixes "rotate → jumps to Home" and "highlight is wrong after Back". |
| `MainScreen.kt` | 166 | Tab click uses the standard options: `popUpTo(start) { saveState }`, `launchSingleTop`, `restoreState`. Each tab remembers its own state. |
| `MainScreen.kt` | 98 | `showTopBar` / `showBottomBar` are plain `val`s using the type-safe `isIn<RootGraphs.X>()` (line 191) instead of `route.contains("MainScreens")` |
| `MainScreen.kt` | 195 | `topBarTitle()`: a type-safe `hasRoute<...>()` → `stringResource`. Moved out of the ViewModel. |
| `MainScreen.kt` | 92 | Name and photo come from `viewModel.user` (a live DataStore flow), so **the top bar updates right after a profile photo or name change** |
| `MainScreen.kt` | 222 | Photo load failure is now local UI state (`photoFailed`), which replaces `viewModel.resetPhoto()` |
| `res/values/strings.xml` | 23 | New `<!-- MAIN SCREEN -->` section: titles, tab labels, content descriptions |
| `FirebaseAuthServiceImpl.kt` | 42, 69, 113 | `photoUrl.toString()` → `photoUrl?.toString().orEmpty()`. It used to save the literal string `"null"`. |
| `FirebaseProfileServiceImpl.kt` | 56 | same |

## Stage 4: Cleanups

| File | What |
|---|---|
| `DenuSpendApp.kt` | Removed the static `instance` + `applicationContext()` (unused, and `!!`-crash-prone). Use Hilt's `@ApplicationContext` if you ever need one. |
| `MainActivity.kt` | Removed the unused `firebaseAuth` / `dataStore` injections and unused imports. Intent handling is untouched. |

## Fixes after review (same day)

**Fix 1: ViewModels were left alive after login/logout (a leak I introduced).**
The first version swapped worlds with a plain `when(session)`. Compose removed the UI, but a NavHost's screen ViewModels live in its `LocalViewModelStoreOwner`. Outside any NavHost, that owner is the Activity, and NavHost never clears them when it simply leaves composition. So after logout, Home/Budget/Add/Profile ViewModels (and their Firestore snapshot listeners) stayed alive until the app closed, one more set per login/logout cycle.
Your original design didn't have this problem: `MainScreen` lived inside the `MainTopLevel` entry, so popping it cleared everything. The fix keeps that, but drives it from the session:

| File | Line | What |
|---|---|---|
| `AppRoot.kt` | 27–37 | Top-level `NavHost` is back, with `SplashTopLevel` (start), `AuthTopLevel` and `MainTopLevel` |
| `AppRoot.kt` | 40–52 | `LaunchedEffect(session)`: the only place that switches worlds. It navigates with `popUpTo(navController.graph.id) { inclusive = true }`, which pops the old world, and popping clears its ViewModels. |
| `navigation/Screens.kt` | 66–78 | `AppRootScreens` is back with the `…TopLevel` names, but it's not a `NavigationScreens` |

**Fix 2: Rename `onLoggedIn` → `onLoginOrRegisterCompleted`.** It reads as "call this when login/register is completed", not as a state. This touched `OneTimeEvents.OnLoginOrRegisterCompleted` (`utils/OneTimeEvents.kt:14`), `MainViewModel.onLoginOrRegisterCompleted()` (`MainViewModel.kt:45`), the `AuthNavHost` / `LoginScreen` / `RegisterScreen` params, and the 7 exhaustive `when` branches.

---

## Behavior changes you should know about
1. **Back on a non-Home tab now goes to Home first**, then exits. Before, every tab click cleared the whole stack (`popUpTo(0)`), so Back exited right away. This is the Material default. If you prefer the old way, change `popUpTo(...)` at `MainScreen.kt:166`.
2. **Wallet / Goals tabs are disabled** (greyed out) instead of tappable-but-dead.
3. The "No user is signed in" toast from Main screens may not show, because the screen is swapped out immediately. The logout itself works.
4. Splash is now only visible for the moment `getCurrentUser()` takes. There's no artificial 1s delay.

## Not changed (on purpose)
- `TokenProvider` is still used by `HomeViewmodel` (name toast). It's fine, just not live-updating.
- `SnackBarController`, `ObserveAsEvents`, `EmailActionActivity` and the Sample graph are untouched.
- Unused `NavBehavior` imports in `HomeViewmodel`/`AddViewModel`/`ProfileViewModel` were already unused before. They're just warnings.
- No new libraries, no new DI module files, no Gradle changes. `hasRoute` already exists in your navigation `2.8.0-beta04`.

---

## What to test (in Android Studio)
**Startup / auth**
- [ ] Fresh install (or clear data) → Login screen. **Back exits the app** (no Splash loop).
- [ ] Login → Home. Back exits the app.
- [ ] Register a new account → Home, and the top bar says "Welcome back, <your name>" (proves the name save isn't cancelled).
- [ ] Login ⇄ Register switching and its slide animations still work.
- [ ] Kill the app while logged in → reopen → straight to Home.
- [ ] Airplane mode + reopen while logged in → you still get in (no stuck Splash).

**Logout paths**
- [ ] Profile → Sign out → Login screen.
- [ ] Profile → Delete account → Login screen.
- [ ] Sign out, then log in as a different user → the top bar shows the new user's name and photo.
- [ ] Leak check (Fix 1): put a `Log.d` in `BudgetViewModel.onCleared()` (or use the Profiler's heap dump). Open Budget → sign out → the log should fire right away, not only when the app closes.
- [ ] After logout, Back on Login exits the app (Main is no longer behind it).

**Shell**
- [ ] Switch tabs → the highlight follows. Press system Back → the highlight follows.
- [ ] On Budget → Budget Insights, **rotate the phone** → you stay on Budget Insights.
- [ ] Change your profile photo or name → the top bar updates without restarting.
- [ ] No photo → the default account icon shows.
- [ ] Titles: Home "Welcome back, X", Add "Transaction", Add Income/Expense, History, Budget, Budget Insights.
- [ ] Snackbars from Add/Budget/Profile still show at the bottom.

**Release**
- [ ] `assembleRelease-debuggable` once: `SessionState` and the routes are plain Kotlin and serializable objects, so no new `@Keep` should be needed, but it's worth one run.
