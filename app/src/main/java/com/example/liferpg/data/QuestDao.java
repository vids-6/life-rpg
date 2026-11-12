package com.example.liferpg.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface QuestDao {
    @Insert
    long insert(Quest quest);

    @Update
    void update(Quest quest);

    @Query("DELETE FROM quests WHERE id = :id")
    void delete(long id);

    @Query("SELECT * FROM quests WHERE id = :id")
    Quest byId(long id);

    @Query("SELECT * FROM quests")
    List<Quest> all();

    @Query("UPDATE quests SET completed = 0, completedAt = 0, lastCompletedDate = NULL, timesCompleted = 0")
    void resetCompletion();
}
