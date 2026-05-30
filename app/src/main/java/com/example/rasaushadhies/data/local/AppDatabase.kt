package com.example.rasaushadhies.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

import androidx.room.*

@Database(entities = [MedicineEntity::class], version = 23, exportSchema = false)
@TypeConverters(MedicineTypeConverters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun medicineDao(): MedicineDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE medicines ADD COLUMN clinicalNotes TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE medicines ADD COLUMN anupana TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE medicines ADD COLUMN properties TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE medicines ADD COLUMN hiBenefits TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE medicines ADD COLUMN hiIngredients TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE medicines ADD COLUMN hiPreparation TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE medicines ADD COLUMN hiDosage TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE medicines ADD COLUMN hiAnupana TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE medicines ADD COLUMN hiProperties TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE medicines ADD COLUMN lastViewedTimestamp INTEGER NOT NULL DEFAULT 0")
            }
        }

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "rasaushadhies_database"
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
