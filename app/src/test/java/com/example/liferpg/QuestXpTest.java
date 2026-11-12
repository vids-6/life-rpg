package com.example.liferpg;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import com.example.liferpg.logic.QuestXp;
import com.example.liferpg.model.Category;
import com.example.liferpg.model.Difficulty;

public class QuestXpTest {
    private static final ZoneId Z = ZoneId.of("Asia/Kolkata");

    @Test
    public void baseXpTable() {
        // duration multipliers: <=15 x0.75, 16-30 x1, 31-60 x1.5, 61-120 x2, >120 x2.5
        assertEquals(15, QuestXp.baseXp(Difficulty.EASY, 15));
        assertEquals(20, QuestXp.baseXp(Difficulty.EASY, 30));
        assertEquals(30, QuestXp.baseXp(Difficulty.EASY, 60));
        assertEquals(40, QuestXp.baseXp(Difficulty.EASY, 120));
        assertEquals(50, QuestXp.baseXp(Difficulty.EASY, 180));
        assertEquals(30, QuestXp.baseXp(Difficulty.MEDIUM, 15));
        assertEquals(60, QuestXp.baseXp(Difficulty.MEDIUM, 45));
        assertEquals(53, QuestXp.baseXp(Difficulty.HARD, 15));   // 52.5 rounds half up
        assertEquals(105, QuestXp.baseXp(Difficulty.HARD, 31));
        assertEquals(140, QuestXp.baseXp(Difficulty.HARD, 61));
        assertEquals(300, QuestXp.baseXp(Difficulty.EPIC, 300));
        assertEquals(90, QuestXp.baseXp(Difficulty.EPIC, 10));
    }

    @Test
    public void multiplierBoundaries() {
        assertEquals(75, QuestXp.durationMultiplierPercent(5));
        assertEquals(75, QuestXp.durationMultiplierPercent(15));
        assertEquals(100, QuestXp.durationMultiplierPercent(16));
        assertEquals(100, QuestXp.durationMultiplierPercent(30));
        assertEquals(150, QuestXp.durationMultiplierPercent(31));
        assertEquals(150, QuestXp.durationMultiplierPercent(60));
        assertEquals(200, QuestXp.durationMultiplierPercent(61));
        assertEquals(200, QuestXp.durationMultiplierPercent(120));
        assertEquals(250, QuestXp.durationMultiplierPercent(121));
    }

    @Test
    public void durationOptionsPerCategory() {
        assertArrayEquals(new int[]{15, 30, 60, 120, 180}, QuestXp.durationOptions(Category.STUDY));
        assertArrayEquals(new int[]{15, 30, 45, 60, 90}, QuestXp.durationOptions(Category.FITNESS));
        assertArrayEquals(new int[]{10, 20, 30, 60, 120}, QuestXp.durationOptions(Category.PERSONAL));
        assertArrayEquals(new int[]{30, 60, 120, 180, 300}, QuestXp.durationOptions(Category.PROJECTS));
        assertArrayEquals(new int[]{5, 15, 30, 60, 120}, QuestXp.durationOptions(Category.DAILY_LIFE));
        assertArrayEquals(new int[]{15, 30, 60, 120, 180}, QuestXp.durationOptions(Category.CUSTOM));
        assertEquals("1h", QuestXp.formatDuration(60));
        assertEquals("90m", QuestXp.formatDuration(90));
        assertEquals("5h", QuestXp.formatDuration(300));
        assertEquals("5m", QuestXp.formatDuration(5));
    }

    @Test
    public void suggestedDueDates() {
        LocalDate today = LocalDate.of(2026, 10, 28);
        assertEquals(today.atStartOfDay(Z).toInstant().toEpochMilli(), QuestXp.suggestedDueDate(15, today, Z));
        assertEquals(today.atStartOfDay(Z).toInstant().toEpochMilli(), QuestXp.suggestedDueDate(30, today, Z));
        assertEquals(today.plusDays(1).atStartOfDay(Z).toInstant().toEpochMilli(), QuestXp.suggestedDueDate(31, today, Z));
        assertEquals(today.plusDays(1).atStartOfDay(Z).toInstant().toEpochMilli(), QuestXp.suggestedDueDate(60, today, Z));
        assertEquals(today.plusDays(2).atStartOfDay(Z).toInstant().toEpochMilli(), QuestXp.suggestedDueDate(120, today, Z));
        assertEquals(today.plusDays(3).atStartOfDay(Z).toInstant().toEpochMilli(), QuestXp.suggestedDueDate(121, today, Z));
        assertEquals(today.plusDays(3).atStartOfDay(Z).toInstant().toEpochMilli(), QuestXp.suggestedDueDate(300, today, Z));
    }

    private static long at(String iso) {
        return ZonedDateTime.parse(iso + "[Asia/Kolkata]").toInstant().toEpochMilli();
    }

    @Test
    public void latePenaltyBoundaries() {
        long due = LocalDate.of(2026, 10, 28).atStartOfDay(Z).toInstant().toEpochMilli(); // deadline = end of 28th
        assertEquals(100, QuestXp.latePercent(due, at("2026-10-28T10:00:00+05:30"), Z));
        assertEquals(100, QuestXp.latePercent(due, at("2026-10-28T23:59:59+05:30"), Z));
        assertEquals(80, QuestXp.latePercent(due, at("2026-10-29T00:00:01+05:30"), Z));
        assertEquals(80, QuestXp.latePercent(due, at("2026-10-29T23:59:59+05:30"), Z));   // < 24h late
        assertEquals(60, QuestXp.latePercent(due, at("2026-10-30T00:30:00+05:30"), Z));   // 24-48h
        assertEquals(60, QuestXp.latePercent(due, at("2026-10-30T23:59:00+05:30"), Z));
        assertEquals(40, QuestXp.latePercent(due, at("2026-10-31T12:00:00+05:30"), Z));   // 48-72h
        assertEquals(25, QuestXp.latePercent(due, at("2026-11-01T00:30:00+05:30"), Z));   // > 72h
        assertEquals(25, QuestXp.latePercent(due, at("2027-03-01T00:00:00+05:30"), Z));
        assertEquals(100, QuestXp.latePercent(0, at("2027-03-01T00:00:00+05:30"), Z));    // no due date
    }

    @Test
    public void earnedXpAppliesShareAndNeverDropsBelowOne() {
        assertEquals(60, QuestXp.earnedXp(60, 100));
        assertEquals(48, QuestXp.earnedXp(60, 80));
        assertEquals(36, QuestXp.earnedXp(60, 60));
        assertEquals(24, QuestXp.earnedXp(60, 40));
        assertEquals(15, QuestXp.earnedXp(60, 25));
        assertEquals(4, QuestXp.earnedXp(15, 25));   // 3.75 -> 4
        assertEquals(1, QuestXp.earnedXp(1, 25));
    }
}
