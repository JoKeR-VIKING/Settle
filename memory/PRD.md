# Settle — Product Requirements (PRD)

## Original problem statement
Improve the UX of Settle (Android / Kotlin-Jetpack-Compose expense-management app).
Targets: color themes, transitions, animations, component placement, sound effects,
tutorials, skeleton loading, API delay handling, and on-demand permissions.

## Architecture (unchanged)
- **Platform**: Android (minSdk 30, targetSdk 36, Kotlin 2.3, Compose BOM 2026.01.01)
- **Auth / data**: Firebase Auth + Firestore + Messaging, Room (SMS drafts)
- **Screens**: Login, PhoneVerification, Expenses, AddEditExpense, Groups,
  GroupExpenses, Analytics, Account

## User personas
- Primary: Individuals who want to track personal expenses and split with friends.
- Secondary: Group organizers (trips, flatmates) who need shared ledgers + settlements.

## Design system (implemented in this iteration)
- **Brand palette**: Teal `#14B8A6` + Blue `#2563EB` (logo-derived) with playful
  accents: Coral, Lime, Lilac, Butter-gold. Dark + Light ColorSchemes.
- **Typography**: Inter for body/UI (readable), Orbitron for display/hero (bold brand moments).
- **Motion kit**: `bounceClickable`, `breathing`, `pulse`, `shimmerBrush`,
  `ShimmerBox`, `ShimmerCircle`.
- **Loader**: Custom Compose "CoinLoader" (brand-gradient spinning coin + bouncing
  dots). No 3rd-party animation dep.
- **Skeletons**: Expense rows, group rows, analytics cards.
- **Sound + Haptics**: `SoundManager` using `ToneGenerator` + `VibrationEffect`
  (tap / success / error / delete). No audio assets required.

## What's implemented (Jan 2026 – v1.1.5 UX pass)
### Theme & foundation
- `ui/theme/Color.kt`, `Theme.kt`, `Type.kt` — brand palette + typography rebuilt
- `ui/animations/SettleAnimations.kt` — motion utility kit
- `ui/animations/CoinLoader.kt` — brand-themed loader
- `components/common/Skeletons.kt` — shimmer placeholders
- `components/common/CoachMark.kt` — multi-step first-run tutorial overlay
- `utils/SoundManager.kt` — taps, success, error, delete beeps + haptics
- `utils/SettlePrefs.kt` — first-run flags via SharedPreferences
- `utils/Permissions.kt` (rewritten) — on-demand `rememberPermissionRequester`
  with friendly rationale AlertDialog (SMS / Notifications / Contacts)

### Screens & components
- **MainActivity**: removed eager permission request; NavHost now uses
  slide + fade transitions for natural screen continuity; notifications
  permission is requested *once* after login.
- **LoginScreen**: breathing logo, animated brand-gradient radial backgrounds,
  slide-up CTAs, CoinLoader while signing in.
- **ExpensesScreen**: greeting hero card with month-to-date spend, pill tab
  switcher, skeleton on first load, empty-state illustration, first-run CoachMark.
- **GroupsScreen**: display-styled header, gradient "Start a new group" card,
  skeletons, empty-state, first-run CoachMark. Overflow-safe row with weights.
- **ExpenseRow**: staggered fade-in + slide, bounce press, long-press haptic,
  overflow-safe layout, "you lent / you owe" hint, ellipsis on description.
- **BottomBar**: animated brand-gradient selection pill, label fades-in only on
  active tab, haptic tick on tab change.
- **FabMenu**: gradient brand FAB with spring rotation + haptics; SMS permission
  gated inline when user taps "Add From SMS".
- **LoadingScreenWrapper**: CoinLoader + cycling tips carousel for perceived-
  performance during long API calls.
- **AddEditExpenseScreen**: success/error sound on submit.
- **RecurringExpensesList / BalanceCard**: row weight + `TextOverflow.Ellipsis`
  fixes to stop title/amount/balance text bleeding off-screen.

## Known limitations
- Build / APK packaging must be performed locally in Android Studio
  (this environment is a Linux dev container). All changes are source-only
  and require **no new gradle deps**.
- ktlint is enabled with `ignoreFailures = false`. If the local ktlint fails on
  import-order of newly added files, run `./gradlew ktlintFormat`.

## Prioritized backlog (next)
P1
- [ ] "Replay tour" entry point in Account screen (re-open CoachMarks)
- [ ] Sound on/off toggle in Account screen (SettlePrefs already supports it)
- [ ] Empty-state for Analytics + screen-enter staggered animations for chart
      cards

P2
- [ ] Contacts permission flow wired into AddMemberButton (currently SMS +
      Notifications are wired; Contacts permission is ready to use but
      needs UI hook when "Add from Contacts" action exists)
- [ ] Optimistic UI for expense add (render row before Firestore acks)
- [ ] Shared-element transition from ExpenseRow → AddEditExpenseScreen
- [ ] Replace ToneGenerator with short WAV assets for richer sound design

## Changelog
- **2026-01** UX overhaul (this task): theme, motion, sounds, skeletons,
  on-demand permissions, tutorial, screen transitions, overflow fixes.
