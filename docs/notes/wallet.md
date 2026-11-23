# Wallet (how it works)

- **Wallets are where real money sits:** cash, banks, e-wallets, cards. Allowance is a plan and never touches a wallet. A wallet has a name, a color (`colorIndex` into `WalletStyle`'s 10 colors), an icon (`iconKey`, one of 18 bundled icons, so no Storage) and `balance` (Long cents, can be negative, and shows red). There's no type and no limit in v1.
- **`wallets/{id}`** (`WalletModel`): `userId`, `name`, `colorIndex`, `iconKey`, `balance`, `createdAt`. The physical wallet's id is fixed, `{uid}_physical`. `isPhysical` is computed from the id and isn't stored. It can be renamed and its balance edited, but it can never be closed (`closeWallet` throws on it).
- **The physical doc is also the setup flag.** `needsWalletSetup()` = `!exists && !metadata.isFromCache`. A cached "missing" is not trusted, because running setup again would overwrite the real balance. `MainViewModel.loggedInOrSetup()` runs after login/register and at startup, and a thrown check falls through to LoggedIn. `SessionState.NeedsWalletSetup` → AppRoot's `WalletSetupTopLevel` (the same `popUpTo` switch, `navigation-and-session.md`).
- **Setup is 2 steps** (`WalletSetupScreen`, its own Scaffold, no snackbar host). Step 1: the physical wallet (₱0 is fine) is saved with `set()` on the fixed id, so Back → Next just saves over it. Step 2: add more wallets → Finish → `onWalletSetupCompleted()`. `OnSetupStepBack` reloads step 1 from the live list, because the step 2 sheet can edit the physical wallet. A Sign out button is there too.
- **Reads:** `getWallets()` is a live listener on `userId ==`, sorted on the device (physical first, then `createdAt`), so it needs no index. The Wallet tab follows the tab rule (no ON_STOP stop). Details and the expense form stop on ON_STOP. `getWalletTransactions(walletId, limit)` = `transactions` where userId + walletId, `date` desc, growing limit of 20. **It needs a composite index** (userId ASC, walletId ASC, date DESC). The details list is display-only; edit or delete through Add/History, which know the day doc.
- **"Paid from" on Add Expense** (`ui/add/components/WalletPickerRow.kt`, mini wallet cards): it defaults to the physical wallet unless the user picked one or is editing (`walletPicked`), and also offers "Don't deduct" (`walletId = ""`). An id that's no longer in the list shows "Closed wallet". Old expenses have `""`.
- **"Paid from" waits for the list.** Until `hasLoadedWallets`, it shows two grey placeholders and Save is disabled, so an expense is never saved as "Don't deduct" just because the list was late.
  - In the 1.2.0 release test, the first list sometimes never arrived on a fresh form (twice in ~10 opens, right after applying a template, on a fresh account). The cause wasn't found, and re-adding the listener fixed it.
  - So `OnGetWallets` runs `collectWallets()` as a child of `walletsJob`. If nothing arrives in `WALLETS_RETRY_MS` (3s), it cancels that try and starts one more. `stopWalletsListener()` stops both.
  - Accepted: if the retry also gets nothing, Save stays disabled until the form is reopened.
- **Balance math is in the same batch as the transaction** (`FirebaseTransactionServiceImpl`):
  - Add: `increment(-sum)` per wallet. A wallet closed elsewhere while the form is open fails the whole batch with NOT_FOUND, on purpose, so nothing half-saves.
  - Edit: one `get()` checks that the old wallet still exists. Same wallet: it moves by the delta. Otherwise the old wallet is refunded (if open) and the new one charged.
  - Delete: refund if the wallet still exists.
  - Two devices editing at the same time could drift, like the category totals.
- **Wallet UI bits (1.2.1):** a negative wallet shows `NegativeBadge` ("!", top-right, in `WalletCard.kt`, also used by Home's mini cards) next to the Negative chip. The Wallet tab's total and Home's balance card have an ⓘ `InformationTooltip`, "Wallets vs allowance" (`lbl_wallet_vs_allowance_*`, GENERAL LABELS): wallets are real money, allowance is an imaginary daily limit, and they're deliberately not linked. The details buttons use Material's icon-button sizes so "Close wallet" stays on one line. The name hint names no banks.
- **Close wallet** deletes only the wallet doc. Its expenses stay everywhere, and later edits or deletes skip the wallet math. It's on the details screen (next to Edit, non-physical only) and in the Edit sheet, both behind `CloseWalletDialog`. The VM sends the snackbar straight to `SnackBarController` before clearing `isLoading`, and the details screen pops once the wallet is gone and nothing is loading. That way the snackbar survives the VM being cleared.
- **Rules:** `get` must allow `resource == null` (the setup and closed-wallet checks read docs that may not exist). Create checks that a `_physical` id is `request.auth.uid + '_physical'`. Owner-only list/update/delete. Account deletion deletes `wallets` where `userId == uid` (`profile.md`).
  ```
  match /wallets/{walletId} {
    allow get:    if resource == null || isMe(resource.data.userId);
    allow list:   if isMe(resource.data.userId);
    allow delete: if isMe(resource.data.userId);
    allow create: if isMe(request.resource.data.userId)
                  && (!walletId.matches('.*_physical') || walletId == request.auth.uid + '_physical');
    allow update: if isMe(resource.data.userId) && isMe(request.resource.data.userId);
  }
  ```
- **Known gaps:**
  - Wallet saves use `awaitWrite` (`add-transactions.md`), so offline they close at once and sync later.
  - Tab listeners log one `PERMISSION_DENIED` on sign-out or delete. Nothing shows on screen.
  - "Load more" hasn't been tested on a device yet.
  - Editing a wallet writes its balance as an absolute value, so an expense saved on another device while the sheet is open gets overwritten (accepted for v1).
  - A sign-out that lands while the startup wallet check is still running could be overwritten by LoggedIn (very narrow race in `MainViewModel.loggedInOrSetup`).
