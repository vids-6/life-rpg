package com.example.liferpg;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.example.liferpg.logic.LevelSystem;

public class LevelSystemTest {

    @Test
    public void thresholdsFollowTheFormula() {
        assertEquals(0L, LevelSystem.xpToReach(1));
        assertEquals(100L, LevelSystem.xpToReach(2));
        assertEquals(250L, LevelSystem.xpToReach(3));
        assertEquals(450L, LevelSystem.xpToReach(4));
        assertEquals(700L, LevelSystem.xpToReach(5));
    }

    @Test
    public void levelIsDerivedFromTotalXp() {
        assertEquals(1, LevelSystem.levelForXp(0));
        assertEquals(1, LevelSystem.levelForXp(99));
        assertEquals(2, LevelSystem.levelForXp(100));
        assertEquals(2, LevelSystem.levelForXp(249));
        assertEquals(3, LevelSystem.levelForXp(250));
        assertEquals(5, LevelSystem.levelForXp(700));
        assertEquals(1, LevelSystem.levelForXp(-50));
    }

    @Test
    public void xpCarriesOverAfterLevelUp() {
        // 120 XP = level 2 with 20 XP already inside it
        assertEquals(2, LevelSystem.levelForXp(120));
        assertEquals(20L, LevelSystem.xpIntoLevel(120));
        assertEquals(150L, LevelSystem.xpNeededForNext(2));
        // one big reward can skip several levels, carry-over stays exact
        assertEquals(4, LevelSystem.levelForXp(500));
        assertEquals(50L, LevelSystem.xpIntoLevel(500));
    }

    @Test
    public void progressPercentIsWithinBounds() {
        assertEquals(0, LevelSystem.progressPercent(0));
        assertEquals(50, LevelSystem.progressPercent(50));
        assertEquals(13, LevelSystem.progressPercent(120)); // 20/150
        for (long xp = 0; xp < 5000; xp += 37) {
            int p = LevelSystem.progressPercent(xp);
            assertTrue(p >= 0 && p <= 100);
        }
    }

    @Test
    public void levelNeverExceedsMax() {
        assertEquals(LevelSystem.MAX_LEVEL, LevelSystem.levelForXp(Long.MAX_VALUE / 4));
    }
}
