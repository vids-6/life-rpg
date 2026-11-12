package com.example.liferpg.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface ActivityDao {
    /** Returns -1 when refKey already exists (duplicate reward attempt). */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insert(ActivityLog log);

    @Query("SELECT * FROM activity_log ORDER BY timestamp DESC, id DESC LIMIT :limit")
    List<ActivityLog> recent(int limit);

    @Query("SELECT * FROM activity_log WHERE type = 'QUEST' AND refId = :questId ORDER BY timestamp DESC LIMIT :limit")
    List<ActivityLog> questHistory(long questId, int limit);

    @Query("SELECT COUNT(*) FROM activity_log WHERE type = :type")
    int count(String type);

    @Query("SELECT COUNT(*) FROM activity_log WHERE type = :type AND dateKey BETWEEN :from AND :to")
    int countBetween(String type, String from, String to);

    @Query("SELECT COALESCE(SUM(durationMin), 0) FROM activity_log WHERE type = :type")
    int totalMinutes(String type);

    @Query("SELECT COALESCE(SUM(xp), 0) FROM activity_log WHERE dateKey BETWEEN :from AND :to")
    int xpBetween(String from, String to);

    @Query("SELECT dateKey AS dateKey, SUM(xp) AS xp FROM activity_log WHERE dateKey BETWEEN :from AND :to GROUP BY dateKey")
    List<DayXp> xpByDay(String from, String to);

    @Query("SELECT category AS category, COUNT(*) AS count, COALESCE(SUM(xp), 0) AS xp FROM activity_log WHERE category IS NOT NULL GROUP BY category")
    List<CategoryStat> categoryStats();

    @Query("DELETE FROM activity_log")
    void clear();
}
