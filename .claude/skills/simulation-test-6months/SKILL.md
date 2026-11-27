---
name: simulation-test-6months
description: DenuSpend-only. Simulate 6 months of the user's real money life (payroll, workdays, weekends, 2 credit cards, installments, subscriptions, family support, savings) on a fresh throwaway account, to see how the app behaves over time and what it costs in Firestore. Use only when the user asks to run "the simulation" / "simulation test".
---

# 6-month simulation test (DenuSpend)

Goal: see how the app works over a long run, from scratch, and how many Firestore reads/writes/deletes it costs.
Before Dec 1, 2026 (the user makes their real account on Dec 1). Planned 2026-11-25.

## Hard rules
- **A NEW throwaway account** (`qa.sim<random>@example.com`). Never verify it, never trigger any email.
- **Never open Profile** (no verify prompts, no email/password/delete flows).
- **Never delete the simulation account.** The user checks it by hand afterwards. Report its email and password.
- **Android 16 emulator:** AVD `Root_Android_16` (rooted, so the clock can be set). Build with
  `export JAVA_HOME=/Users/denuden/Library/Java/JavaVirtualMachines/jbr-21.0.11/Contents/Home && ./gradlew installDebug`.
- Never touch the user's real account, `google-services.json` or signing.
- Keep a running cost log (reads/writes/deletes per simulated day) and stop to report if anything looks abnormal (a loop, a spike, an error dialog).

## The clock problem (why it's hybrid)
The app stamps everything with the device clock (no `serverTimestamp`). But TLS checks the device clock too:
`firestore.googleapis.com`'s certificate was valid **Nov 3, 2026 → Jan 26, 2027** when checked. With the device date outside the cert window, every request fails. Re-check before running:
`echo | openssl s_client -connect firestore.googleapis.com:443 -servername firestore.googleapis.com 2>/dev/null | openssl x509 -noout -dates`
So the app can only be driven with a shifted clock inside that window. Approach (**approved by the user 2026-11-25**):
1. **Seed Jun 1 → Nov 2 with a script** (Node, in the scratchpad, not the repo; logs in with the normal email/password REST login and the public API key read from `google-services.json`; writes through the Firestore REST API as that user, so the security rules apply; no admin key).
   - **Never burst:** one batch at a time, wait for it, ~1 s pause between batches (≤ ~5 writes/s, ~15–20 min total). Stop on the first error, never retry in a loop. The user worries about being flagged as a DDoS: stay slow.
   - **Dry run first:** print the plan and totals per month; write nothing until the user OKs it. It must write exactly what the app writes: the same docs, fields, batches and increments as `FirebaseTransactionServiceImpl`, `FirebaseWalletServiceImpl` (movements, transfers `_out`/`_in`, card payments, auto-charge movements `{chargeId}_{yyyyMMdd}`), `daily_history`/`monthly_history` maps, and `recurring_charges` state.
2. **Drive the real app day by day for Nov 3 → Nov 30** (set the emulator date each morning, cold start for the auto-charge, add through the UI), so the last month tests the real write code, auto-charge and statements.
3. Then check every screen over the whole range (Home, Add/History, Budget with every filter + Insights, Wallet, card details, statements month by month, Subscriptions & installments, Budget bills).

## The persona (the user's real life)
- **Wallets:** physical wallet (cash), **Payroll** (debit, salary lands here), **GoTyme** (digital, savings).
- **Cards:** **UB Platinum** (limit ₱150,000) and **Visa Classic** (limit ₱90,000). Pick statement days (e.g. 5th and 20th).
- **Salary:** ₱14,000 on the 15th and ₱16,000 on the 30th (month end if no 30th), into Payroll (money in).
- **Moving money:** almost always from Payroll → GoTyme (savings, what's left) and Payroll → physical wallet (cash).
- **Card payments:** always from GoTyme or Payroll, never the physical wallet ("Pay card" with "Paid from").
- **Family:** sometimes ₱5,000 each to mother and tita from the wallet. Not an expense: a money-out movement.
- **Workdays (Mon–Fri):** mostly Transportation and Food. Sometimes overspend: mini groceries (Household), Watsons self care (Personal).
- **Weekends:** random. Some weekends nothing; some go out for clothes/shoes (Personal), food, a haircut. Some are "pamper days".
- **Cards in daily life:** some food and clothes go on a card (expense paid from that card).
- **Big purchases (installments):** ₱20,000 over 3 months, and an ₱80,000 laptop over 24 months, at some point in the 6 months.
- **Subscriptions:** Netflix, YouTube Premium, Claude Pro. Some months Netflix is paused: the app has no pause (user decision), so delete it and add it again when resumed.
- **Allowance (decided 2026-11-25):** workdays fixed, Food ₱250 + Transportation ₱150. **Weekends random**, no fixed amount: ~35% of weekend days no allowance at all (just tracking), ~40% normal (Food ₱300–₱700), ~25% pamper day (Food ₱400–₱900 + Personal ₱1,000–₱3,500). No restrictions anywhere: overspending is normal.
- **Spending** is random and independent of the allowance (sometimes under, sometimes over).
- **Big days:** about 10% of days spend ₱5,000–₱10,000 (groceries haul, shoes, gadget, night out), cash, debit or card. Not too easy on money.
- **Guard:** each month's spending + card payments + family support + savings stays covered by that month's ₱30,000 payroll. The dry run checks it and moves a big day to the next month if needed. No wallet goes negative by accident.
- **Statement days (decided):** UB Platinum the 5th, Visa Classic the 20th.
- Randomize with a fixed seed so a run can be repeated.

## Free tier (Spark)
The No-cost lines are **per day**: 50,000 reads, 20,000 writes, 20,000 deletes. They reset daily, not per billing period.
Estimate for the whole run: about 4–6K writes, 10–25K reads, ~0 deletes, spread over 1–3 real days. Fine.

## Report
Per simulated month: what was added, balances, card owed vs statements, and the Firestore cost. Then the screen checks, any bug (with the exact steps), UI/UX notes, and the account's email and password.
