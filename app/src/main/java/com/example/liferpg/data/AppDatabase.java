package com.example.liferpg.data;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

@Database(entities = {
        PlayerProfile.class, Quest.class, ActivityLog.class,
        UnlockedAchievement.class, SavedGym.class
}, version = 2, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    /** v1 -> v2: quests get an estimated duration (0 = created before durations; keeps its stored XP). */
    static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(SupportSQLiteDatabase db) {
            db.execSQL("ALTER TABLE quests ADD COLUMN durationMin INTEGER NOT NULL DEFAULT 0");
        }
    };

    private static volatile AppDatabase instance;

    public abstract ProfileDao profileDao();

    public abstract QuestDao questDao();

    public abstract ActivityDao activityDao();

    public abstract AchievementDao achievementDao();

    public abstract GymDao gymDao();

    public static AppDatabase get(Context context) {
        if (instance == null) {
            synchronized (AppDatabase.class) {
                if (instance == null) {
                    // IMPORTANT: no fallbackToDestructiveMigration(). User progress must never be
                    // wiped by a schema change. When an entity changes, bump `version` and add a
                    // Migration here, e.g.:
                    //   static final Migration MIGRATION_1_2 = new Migration(1, 2) {
                    //       public void migrate(SupportSQLiteDatabase db) {
                    //           db.execSQL("ALTER TABLE player_profile ADD COLUMN ...");
                    //       }
                    //   };
                    //   ... .addMigrations(MIGRATION_1_2)
                    // (The avatar look is stored in SharedPreferences, so it needs no migration.)
                    instance = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "liferpg.db")
                            .addMigrations(MIGRATION_1_2)
                            .build();
                }
            }
        }
        return instance;
    }
}
