# Smart Receipt Scanner — v0.2 Feature Plan

Scope: everything except cloud sync. Target: `versionName = "0.2.0"`, `versionCode = 2`.

Ordering is dependency-aware — categories land first because export and reports read from
them; parsing improvements are isolated and can technically happen anytime but are placed
before reports since better parsed data makes reports more meaningful.

---

## Phase 0 — Groundwork
- [x] Bump `versionCode`/`versionName` in `app/build.gradle.kts`
- [x] Reconcile `minSdk` mismatch (gradle says 26, README says 24) — pick one and update the other
- [x] Add a Room migration strategy note (we're adding columns across phases; each phase gets its own `Migration` object, no destructive migrations)

## Phase 1 — Categories & Budgeting
**Goal:** tag receipts, see monthly spend by category.

- [x] `Entities.kt`: add `category: String?` to `ReceiptEntity`
- [x] `AppDb.kt`: bump DB version, add `Migration(n, n+1)` adding the `category` column
- [x] New: `core/data/Category.kt` — small enum/sealed list of defaults (Groceries, Fuel, Dining, Utilities, Shopping, Health, Other) + display icon/color
- [x] `ReceiptParser.kt`: add `guessCategory(merchant: String?): String?` heuristic (keyword match against merchant name), called from `parse()` as a suggestion only (not authoritative)
- [x] `ReviewScreen.kt`: add a category chip row (FilterChip/AssistChip group), pre-selected from the heuristic guess, user can override
- [x] `Dao.kt`: add query for `SUM(totalMinor) GROUP BY category` for the current month
- [x] `HomeScreen.kt`: add a compact monthly summary card ("This month: PKR 24,500 across 5 categories") with a simple bar breakdown per category
- [x] `HistoryScreen.kt`: add category filter chips above the list
- [x] Tests: `ReceiptParserTest.kt` — cases for `guessCategory`

## Phase 2 — Export & Backup
**Goal:** get data out for expense/tax reporting; local backup/restore.

- [ ] New: `core/export/CsvExporter.kt` — writes filtered receipts (merchant, date, total, tax, currency, category) to CSV via `FileProvider`
- [ ] New: `core/export/JsonBackup.kt` — full DB → JSON export/import (for local backup, not sync)
- [ ] New: `core/export/PdfReportExporter.kt` — single receipt or date-range → simple PDF (use Android `PdfDocument`, no external lib)
- [ ] `HistoryScreen.kt`: add overflow menu → "Export CSV" (respects active filters), "Export PDF report"
- [ ] `HomeScreen.kt` or new `SettingsScreen.kt`: "Backup to JSON" / "Restore from JSON"
- [ ] Add `FileProvider` entry to `AndroidManifest.xml` + `file_paths.xml` if not present, for sharing exported files
- [ ] Tests: CSV row formatting, JSON round-trip (export then import produces identical entities)

## Phase 3 — Parsing Accuracy Improvements
**Goal:** make the heuristic parser self-aware and correctable.

- [x] `ReceiptParser.kt`: return a `confidence: Map<String, Float>` per field (merchant/date/total/tax/currency) alongside `ParsedReceipt` — e.g. total confidence is high if matched a "total" hint line, low if it fell back to `lastBlockMax`
- [x] `Entities.kt`: no schema change needed (confidence is review-time only, not persisted) — confirm this is acceptable, otherwise add a `parseConfidenceJson` column
- [x] `ReviewScreen.kt`: visually flag low-confidence fields (e.g. amber outline + small "please verify" label) so the user double-checks before saving
- [x] New: `core/parser/MerchantPatternStore.kt` (backed by a small Room table `merchant_patterns`) — remembers per-merchant corrections (e.g. "for merchant X, total is always the line above 'Cash'") and re-applies them on next scan from the same merchant
- [x] `Dao.kt`/`AppDb.kt`: add `MerchantPatternEntity` + migration
- [x] Tests: expand `ReceiptParserTest.kt` with multi-language and multi-currency receipt fixtures; add confidence-score assertions

## Phase 4 — Multi-Currency & Reports
**Goal:** meaningful totals/reports when receipts span currencies.

- [ ] `HistoryScreen.kt`: group/subtotal by currency instead of assuming one currency app-wide
- [ ] New: `core/data/ExchangeRateStore.kt` — simple user-entered rate table (e.g. DataStore-backed key/value: `"USD_TO_PKR" -> 278.5`), no network calls (keeps app "private by design")
- [ ] `HomeScreen.kt`: optional combined total ("≈ PKR 142,000 combined") shown only if rates are configured, otherwise show per-currency subtotals
- [ ] New: `SettingsScreen.kt` (may already exist from Phase 2 backup UI — reuse) — section to edit exchange rates
- [ ] Extend CSV/PDF export from Phase 2 to include a currency column and optional converted-total column
- [ ] Tests: rate conversion math, rounding to minor units

## Phase 5 — Polish & Quality-of-Life
**Goal:** lower-effort daily-use improvements.

- [ ] `HistoryScreen.kt`: add amount-range and date-range filters alongside existing text search/month grouping
- [ ] `HistoryScreen.kt`: multi-select mode — bulk delete, bulk export (feeds into Phase 2 exporter)
- [ ] Optional: notification/reminder nudging user to scan a receipt shortly after opening the app during typical shopping hours (skip if it feels like scope creep — flag for a future release instead)
- [ ] Confirm Phase 0 minSdk/targetSdk fix shipped
- [ ] Full regression pass: capture → OCR → review (with category + confidence flags) → save → history (filters + export) → settings (backup + rates)

---

## Suggested build order
1. Phase 0 (quick, unblocks nothing but avoids churn later)
2. Phase 1 — Categories
3. Phase 3 — Parsing accuracy (isolated, improves data quality before we build reports on top of it)
4. Phase 2 — Export & Backup
5. Phase 4 — Multi-Currency & Reports
6. Phase 5 — Polish

## Out of scope (explicitly excluded per your request)
- Cloud sync / multi-device / auth
