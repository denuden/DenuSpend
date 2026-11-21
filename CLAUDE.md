# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project
DenuSpend: an Android app (Kotlin, Jetpack Compose, Material 3) that tracks income and expenses and keeps a spending history. It has a single Gradle module, `:app`, with package `com.gmail.vondenuelle.denuspend`. The backend is Firebase (Auth + Firestore), configured through `app/google-services.json`. `web/` is a separate static Firebase Hosting site (`web/firebase.json`, public dir `denu-spend-public`) and is not part of the Gradle build.

## Commands
- Debug build: `./gradlew assembleDebug`. Debug builds get the applicationId suffix `.debug` and the version suffix `-debug`.
- Release builds: `./gradlew assembleRelease`, and `./gradlew assembleRelease-debuggable` for R8-minified and debuggable, which is useful for reproducing release-only R8/Firestore bugs.
- APKs are renamed to `DenuSpend_<versionName>_<buildType>.apk`.
- Install: `./gradlew installDebug`
- Lint: `./gradlew lint`
- Unit tests: `./gradlew testDebugUnitTest`. Single test: `./gradlew testDebugUnitTest --tests "com.gmail.vondenuelle.denuspend.ExampleUnitTest"`
- Instrumented tests: `./gradlew connectedDebugAndroidTest`. Only the template example tests exist so far.
- Dependency versions live in `gradle/libs.versions.toml` (version catalog `libs.*`). Annotation processing uses KSP (Hilt, Room).

## Architecture
Layered MVVM + MVI-style events, wired with Hilt:

UI (Composable screen) → `XViewModel.onEvent(XScreenEvents)` → Repository → Service interface → Firebase impl

- **Feature package shape** (`ui/<feature>/`): `XScreenState` (an immutable data class held in a `MutableStateFlow`), `XScreenEvents` (a sealed class of user intents), `XViewModel` (`@HiltViewModel`) and `screen/` and `components/` subfolders. Follow this pattern for new features.
- **One-off UI effects**: ViewModels send `utils/OneTimeEvents` (navigate, pop back stack, snackbar, toast, error) through a `Channel` exposed with `receiveAsFlow()`. Screens collect them with `ObserveAsEvents` (in `utils/`). There is also a global `SnackBarController`.
- **Async results**: services return `Flow<T>`, and ViewModels wrap it with `.asResult()` (`utils/network/FlowResultExtension.kt`). That emits `ResultState.Loading/Success/Error/Completed` and retries `IOException` 3 times with a 15s delay. Firestore reads are usually `callbackFlow` + `addSnapshotListener`, which are live listeners. ViewModels keep a `Job` so they can cancel and restart a listener.
- **Services and qualifiers**: each domain has an interface in `data/remote/services/<domain>/` and a `Firebase*ServiceImpl`. The impls are bound in `di/modules/ServiceModule.kt` with a qualifier from `di/qualifiers/ServiceQualifiers.kt` (e.g. `@FirebaseBudget`), and repositories inject them using that qualifier. A new service needs a qualifier annotation, an `@Binds` entry and the qualifier at the injection site. Dispatchers are injected through qualifiers in `di/qualifiers/CoroutineQualifiers.kt` (e.g. `@IoDispatcher`).
- **Repositories** (`data/repositories/`) are `@ViewModelScoped`. They build request objects (`data/remote/models/<domain>/request/`), for example converting filter strings to Firestore `Timestamp` ranges, and apply `flowOn(dispatcher)`.
- **Firestore data**: the main collection is `transactions`, queried by `userId` plus `date` range. Domain models (`domain/models/`) are deserialized with `toObject()`. That means they need default values for every field, `@Keep` (R8 strips or renames them otherwise, and release builds minify), and `@get:Exclude` on fields that are not stored, such as `docId`.
- **Sample code**: `ui/sample`, `SampleRepository`, `RetrofitSampleServiceImpl` and `SampleAPI` are the template's Retrofit and Firebase example. Retrofit/OkHttp (`RetrofitModule`, `APIModule`) is only used there. Room (`data/local/`) is stubbed out and unused. Settings persist through the encrypted DataStore in `data/storage/UserPreferences*`.

## Navigation
Type-safe Navigation Compose, with routes defined as `@Serializable` objects/classes in `navigation/Screens.kt`. Details are in `docs/notes/navigation-and-session.md`. There are three NavHosts:
1. `AppRoot.kt` (top level): `AppRootScreens.SplashTopLevel / AuthTopLevel / MainTopLevel`. It switches **only** from `MainViewModel.session` (`Loading / LoggedIn / LoggedOut`) in one `LaunchedEffect`, using `popUpTo(graph.id){inclusive=true}`, so the old world's ViewModels are cleared. `AppRootScreens` is deliberately not a `NavigationScreens`, so screens can't navigate there.
2. `AuthNavHost` (`navigation/AuthNavGraph.kt`): Login ⇄ Register.
3. `MainScreen.kt`: one shared Scaffold (top bar, bottom bar, snackbar) around the inner `AppNavigation` NavHost with the `RootGraphs` (Main, Profile, Add, Budget), each set up through an `add*NavGraph` extension in `navigation/`. Bar visibility, the title and the selected tab are derived from the back stack with `hasRoute`/`hierarchy`. Tabs use the standard `popUpTo(start){saveState}` + `launchSingleTop` + `restoreState` options.

- **Login:** `AuthViewModel` sends `OneTimeEvents.OnLoginOrRegisterCompleted` → `MainViewModel.onLoginOrRegisterCompleted()`.
- **Logout:** never navigate. Just call `logout()` or delete the account. `MainViewModel.observeSignOut()` (Firebase `AuthStateListener`) sets `LoggedOut` and clears DataStore.
- **Inner navigation:** screens get `onNavigate(route, NavOptions?)` / `onPopBackStack`. ViewModels send `OnNavigate(route, NavBehavior)` (`None | PopUpTo`), and screens convert it with `event.behavior.toNavOptions()`.

`EmailActionActivity` handles Firebase email-action links.

## Release / R8 gotchas
- `proguard-rules.pro` strips all `android.util.Log` calls in release.
- Models used with Firestore or Gson must be `@Keep`, or must be covered by the package `-keep` rules there. A missing `@Keep` shows up only in release, as empty or default objects.

## What's done
- Auth: sign in and register both using email with firebase is working. Forgot password is working as well
- Profile: All modules in profile screen works such as picture and display name, update password, update email, delete account, sign out
- Budget Screen (from bottom nav bar): Only the budget dashboard works. The main graph along with transactions number per category and filter by range of date works too. Not yet finished. ( will be discuess with you along with other features)
- Add Screen (from bottom nav bar): This is transactions of expenses and income. All of this works as intended.

## Rules
- One viewmodel and screen only per one module. Just like my structure here (home, budget, profile, etc.)
- We don't make unnecessary DI files if we can annotate it individually like my Repositories. But if recommendation is to create a new file and defeats the purpose of not creating it, then it shall be created and notify me.  Don't create unnecessary DI module files. Prefer annotating classes directly (e.g. @Inject constructor, like my Repositories). If a new module file is truly needed, create it and tell me why.
- Strings go to resource file strings.xml. And will be organized per screen, unless the string can, might, or will be used in many different screen. Then it shall be under <!--    GENERAL LABELS-->
- Don't touch google-services.json or signing config.
- Jetpack Compose only. No XML layouts.
- Type-safe Navigation only. Never use traditional navigation with compose
- Never build or run any extensive tokens such as testing or building unless instructed you to.
- - Never run Gradle builds, tests, or the app unless I ask. I build in Android Studio myself.
- always confirm with me before implementing something unless i instructed you TO CONTINUE without my supervision
- Don't edit or delete my existing code comments. Only touch them if you delete that code or change how it works, and then revise the comment so it matches the new behavior.

## How I work
- Small changes, one feature at a time.
- If UI, one screen at a time and then we will discuss idea behind it per section
- Ask before adding a new library.
- Firestore listeners: ON_STOP stops are only for other screens, as needed. **Never on the first screens of the bottom nav bar** (Home, Add, Budget). Their listeners keep running so the tab stays cached, and back on those tabs goes to Home with saveState instead of popping them.
- After changes, tell me what to test. I build in Android Studio myself.

In terms of theming, we will follow material 3 guidelines as always especially if using their components. We can use custom one if the UI needs a more complex design and is generally easier if made from scratch rather than editing the material 3 component (E.G like MaterialCard). We will follow the color and as much as possible to use MaterialTheme.colorScheme.****. Only use hardcoded colors if really needed so we can stick to theming without really worrying about switching to dark mode and light mode as much as possible

## What's next
- We will review the code and possibly make changes to existing ones since i had doubts on some of here. I feel like we can make it better without overdoing stuff. Since this code has been handwritten by me.

## HARD RULE
- Never overcomplicate and overengineer. Like thinking so much how to make it dynamic even tho we don't need it and it's not even repeated.
- Composable files: each distinct UI section (an editor's fields, a sheet, a dialog, a card with its own logic, anything long) gets its **own file** in the feature's `components/` folder, named after the composable, so it's easy to find (see the `add`, `profile` and `wallet` packages). A small helper used by only one composable stays in that composable's file (e.g. `NegativeChip` next to `WalletCard`), and isn't moved to `common/` or its own file. "Don't make many files" means no files for tiny single-use pieces, NOT putting every section in one big file.
- We don't need to make it super clean code. We need it understandable, easy to read, and easy to edit. Clean code is fine, making it dynamic is fine and you can definitely do it. I just wanted to remind you that coding like that can get out of hand and produce so many files or many codes that won't really get used many times. We're trying to avoid overengineering stuff. BUT, if practicality and if it's the traditional way to do certain feature or coding, then we can make it dynamic or do what you recommend
- - Do not over-comment. Only comment non-obvious code: one simple line on what it does, then a short technical note. Max 5 sentences.
## Notes
- Explanations of how parts of the app work are in docs/notes/. Read the relevant note before exploring the code.
  With that, future sessions read your short note instead of going through all the files again, which saves tokens every time you come back to that topic.
- Docs/notes are written when we PR (compose-pr skill), not after every change.