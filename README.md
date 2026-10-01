# Tele Expense Counter

**Offline-first Ethiopian telecom expense tracker**

> “Where is my telecom money going?”

Tele Expense automatically detects airtime recharges, data packages, and other telecom spends from SMS messages **entirely on your phone**. No account, no cloud, no upload.

## Features

- **Local SMS analysis** – READ_SMS / RECEIVE_SMS only; bodies never leave the device
- **Rule-based parser** – COUNT / IGNORE / REFERENCE classification
- **Transaction grouping** – multiple SMS → one real expense (e.g. 3× ETB 5 eBirr → 1 expense)
- **Ethiopian calendar first** – Meskerem, Tikimt, … Pagume with correct leap years
- **Privacy by design** – Room/SQLite, no Firebase, no Supabase, no network
- **Modern Compose UI** – Home, Transactions, Calendar, Analytics, Settings

## Architecture

```
SMS → Normalize → Provider → Classify → Extract amount/date/IDs
    → Gregorian→Ethiopian → Candidate → Group/Deduplicate → Expense
```

Key modules:

| Module | Role |
|--------|------|
| `domain/parser` | SmsParser, AmountExtractor, DateExtractor, ProviderDetector, CategoryClassifier |
| `domain/grouping` | TransactionGrouper (ID / amount / time / package matching) |
| `domain/calendar` | EthiopianCalendarConverter |
| `data` | Room entities, DAOs, ExpenseRepository |
| `ui` | Jetpack Compose screens |

## Build on GitHub (recommended)

This project is set up for **GitHub + GitHub Actions**. You do not need a local Android SDK to get an APK.

### 1. Create the repository

```bash
# On your machine or in GitHub UI
gh repo create tele-expense-counter --public --source=. --remote=origin --push
# or:
git init
git add .
git commit -m "Initial commit: Tele Expense Counter"
git branch -M main
git remote add origin https://github.com/YOUR_USER/tele-expense-counter.git
git push -u origin main
```

### 2. CI (automatic)

On every push / PR to `main`, `master`, or `develop`:

| Workflow | What it does |
|----------|----------------|
| **Android CI** (`.github/workflows/android-ci.yml`) | Runs unit tests → builds debug APK → uploads artifact |

Download the APK from the workflow run → **Artifacts** → `app-debug`.

### 3. Release tags

```bash
git tag v1.0.0
git push origin v1.0.0
```

The **Release APK** workflow builds debug + release APKs and attaches them to a GitHub Release.

### Local build (optional)

**Requirements:** JDK 17, Android SDK (or Android Studio).

```bash
./gradlew test
./gradlew assembleDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`

## First run

1. Grant SMS permission (explained in-app).
2. Choose **Scan This Month** or **Start From Today**.
3. Only the current month is scanned initially; later only new SMS after `trackerStartedAt`.

## Tests

```bash
./gradlew test
```

Critical cases covered:

- 3× ETB 5 eBirr SMS → **1** expense  
- 2× ETB 35 packages → **2** expenses  
- P2P transfer → **0**  
- Birthday free package → **0**  
- Package without price → **0** until matched with paid SMS  

## Adding new SMS rules

1. Add example to `data/sms_training.json`
2. Extend patterns in `AmountExtractor` / `CategoryClassifier` / `SmsParser`
3. Add unit test in `SmsParserTest`
4. Keep the conservative rule: **when uncertain → REFERENCE, never invent an expense**

## Privacy

- No account / backend / cloud DB  
- SMS bodies processed in memory only  
- Stored: extracted amount, category, Ethiopian date, IDs — not full SMS text  
- Backup of the DB is excluded from cloud backup  

## License

MIT — see [LICENSE](LICENSE)

## Roadmap

- More bank → telebirr formats  
- Optional learned rules from repeated user corrections  
- Widget & export (CSV, local only)  
