package com.example.liferpg;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.example.liferpg.logic.AttributeGain;
import com.example.liferpg.logic.XpCalculator;
import com.example.liferpg.model.Attribute;
import com.example.liferpg.model.Category;
import com.example.liferpg.model.Difficulty;

/** Quest reward rules. */
public class XpCalculatorTest {

    @Test
    public void difficultyDefaultsAreOrdered() {
        assertEquals(20, Difficulty.EASY.defaultXp);
        assertEquals(40, Difficulty.MEDIUM.defaultXp);
        assertEquals(70, Difficulty.HARD.defaultXp);
        assertEquals(120, Difficulty.EPIC.defaultXp);
    }

    @Test
    public void questXpIsClampedToValidRange() {
        assertEquals(1, XpCalculator.sanitizeQuestXp(-10));
        assertEquals(1, XpCalculator.sanitizeQuestXp(0));
        assertEquals(75, XpCalculator.sanitizeQuestXp(75));
        assertEquals(500, XpCalculator.sanitizeQuestXp(9999));
    }

    @Test
    public void questXpValidation() {
        assertFalse(XpCalculator.isValidQuestXp(0));
        assertTrue(XpCalculator.isValidQuestXp(1));
        assertTrue(XpCalculator.isValidQuestXp(500));
        assertFalse(XpCalculator.isValidQuestXp(501));
    }

    @Test
    public void questGainTrainsPrimaryAndSecondaryAttribute() {
        AttributeGain g = XpCalculator.questGain(Category.STUDY, 80); // 80/20 = 4 primary, 2 secondary
        assertEquals(4, g.get(Attribute.INTELLIGENCE));
        assertEquals(2, g.get(Attribute.DISCIPLINE));
        assertEquals(0, g.get(Attribute.STRENGTH));
        assertEquals(6, g.total());
    }

    @Test
    public void smallQuestStillGivesAtLeastOnePoint() {
        AttributeGain g = XpCalculator.questGain(Category.CREATIVITY, 5);
        assertEquals(1, g.get(Attribute.CREATIVITY));
        assertEquals(0, g.get(Attribute.INTELLIGENCE));
    }

    @Test
    public void everyCategoryTrainsItsPrimaryAttribute() {
        for (Category c : Category.values()) {
            assertTrue(c.label, XpCalculator.questGain(c, 40).get(c.primary) >= 2);
        }
    }

    @Test
    public void focusXpForStandardDurations() {
        assertEquals(20, XpCalculator.focusXp(15));
        assertEquals(35, XpCalculator.focusXp(25));
        assertEquals(70, XpCalculator.focusXp(45));
        assertEquals(100, XpCalculator.focusXp(60));
        assertTrue(XpCalculator.focusXp(15) < XpCalculator.focusXp(25));
    }

    @Test
    public void focusTrainsIntelligence() {
        AttributeGain g = XpCalculator.focusGain(70);
        assertEquals(3, g.get(Attribute.INTELLIGENCE));
        assertEquals(1, g.get(Attribute.DISCIPLINE));
    }
}
