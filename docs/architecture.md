# Tele Expense Counter – Architecture

## Goals

1. Answer “Where is my telecom money going?” without manual entry.
2. Process SMS **only on-device**.
3. Prefer accuracy and conservatism over recall (never invent expenses).
4. Treat Ethiopian calendar as the primary user-facing calendar.
5. Support continuous addition of new SMS patterns without rewriting the engine.

## Pipeline

```
Raw SMS
  → looksTelecomRelated filter
  → SmsParser.parse()
       ProviderDetector
       CategoryClassifier → COUNT | IGNORE | REFERENCE
       AmountExtractor (semantic, balance-aware)
       DateExtractor (prefer explicit transaction date)
       EthiopianCalendarConverter
  → SmsParseResult (candidate)
  → TransactionGrouper.group()
       Match by Transfer ID / Transaction ID
       then amount + recipient + package + time window
  → TelecomTransaction (final expense)
  → Room
```

## Why SMS ≠ transaction

One airtime purchase often produces:

1. “successfully sent airtime top-up of ETB5” (COUNT)
2. “received airtime top-up of ETB5” (IGNORE – other party)
3. “recharged ETB5 … balance is ETB3.6” (REFERENCE)

All three share the same Transfer ID → **one** ETB 5 expense.

## Storage

| Table | Purpose |
|-------|---------|
| `transactions` | Final expenses (extracted fields only) |
| `processed_sms` | SMS IDs already handled (no body) |
| `app_meta` | trackerStartedAt, lastProcessedAt, onboardingDone |

## Initial scan policy

- **Scan This Month**: only SMS with `DATE >= first day of current Gregorian month`
- **Start From Today**: set `trackerStartedAt = now`, process zero historical messages
- Ongoing: `SmsReceiver` + manual “Scan New Messages” only after `lastProcessedAt`

## Extending the parser

1. Add labeled example to `data/sms_training.json`
2. Add / adjust regex or keyword rules in the relevant extractor/classifier
3. Add unit test asserting classification, amount, grouping
4. Keep default path = IGNORE when confidence is low

## Non-goals (v1)

- Machine learning models
- Cloud sync
- Bank statement import (architecture is ready for parallel bank parsers later)
- Contacts / location / camera permissions
