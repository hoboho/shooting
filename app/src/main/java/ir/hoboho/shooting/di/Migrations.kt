package ir.hoboho.shooting.di

import androidx.room.migration.Migration

/**
 * Registered Room migrations.
 *
 * Version 1 is the initial release, so there is nothing to migrate yet. From
 * here on **every** schema change must add an explicit, additive migration
 * (typically `ALTER TABLE ... ADD COLUMN ... NOT NULL DEFAULT ...`) to [ALL].
 *
 * `fallbackToDestructiveMigration()` is deliberately never used: a missing
 * migration must fail loudly rather than silently wipe a user's shooting history.
 * Exported schema JSON (app/schemas) lets CI validate every version.
 */
object Migrations {
    val ALL: Array<Migration> = emptyArray()
}
