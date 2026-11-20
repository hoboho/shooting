# Room schema exports

The current database is **version 6** (`ShootingDatabase.kt`).
`app/build.gradle.kts` configures `room.schemaLocation` as `app/schemas`, and
`exportSchema = true`. With the Android build prerequisites installed, run:

```bash
./gradlew :app:kspDebugKotlin
```

Room generates the current schema at
`app/schemas/ir.hoboho.shooting.data.local.ShootingDatabase/6.json`. The CI
workflow also runs this task and uploads `app/schemas/` as the `room-schema`
artifact. Check the result of the particular run before relying on that artifact.

## Current gap and migration policy

* No versioned schema JSON is currently tracked in this repository. The schema
  files contain Room-generated identity hashes; do not hand-author them. Recover
  historical exports from the corresponding builds or artifacts where available,
  and verify their provenance before committing them. Running KSP on the current
  sources generates v6, **not** earlier versions.
* `Migrations.ALL` registers explicit migrations 1→2→3→4→5→6. The app does not
  use destructive fallback. Future changes need an explicit migration that
  preserves user data and a generated schema committed with the change.
* Add `MigrationTestHelper` tests using genuine historical schemas to verify the
  upgrade paths and data preservation, including the 5→6 ledger migration. No
  such end-to-end migration tests currently exist. `LegacyFatCodeMigrationTest`
  and `CashLedgerMigrationTest` exercise the 3→4 and 5→6 SQL on minimal tables.
* `DatabaseSchemaTest` checks selected aspects of a newly created v6 SQLite
  database; it does not validate an upgrade from an earlier version.
