package com.example.liferpg;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.example.liferpg.logic.AttributeGain;
import com.example.liferpg.logic.XpCalculator;
import com.example.liferpg.model.Attribute;
import com.example.liferpg.model.WorkoutType;

public class WorkoutRewardTest {

    @Test
    public void tooShortWorkoutsEarnNothing() {
        assertEquals(0, XpCalculator.workoutXp(WorkoutType.STRENGTH, 0));
        assertEquals(0, XpCalculator.workoutXp(WorkoutType.CARDIO, XpCalculator.MIN_WORKOUT_MINUTES - 1));
    }

    @Test
    public void minimumWorkoutEarnsXp() {
        assertTrue(XpCalculator.workoutXp(WorkoutType.GENERAL, XpCalculator.MIN_WORKOUT_MINUTES) > 0);
    }

    @Test
    public void workoutXpByType() {
        assertEquals(50, XpCalculator.workoutXp(WorkoutType.GENERAL, 30));
        assertEquals(60, XpCalculator.workoutXp(WorkoutType.STRENGTH, 30));
        assertEquals(55, XpCalculator.workoutXp(WorkoutType.CARDIO, 30));
    }

    @Test
    public void longWorkoutsAreCapped() {
        int capped = XpCalculator.workoutXp(WorkoutType.GENERAL, XpCalculator.MAX_WORKOUT_MINUTES);
        assertEquals(capped, XpCalculator.workoutXp(WorkoutType.GENERAL, 600));
        assertEquals(140, capped);
    }

    @Test
    public void strengthWorkoutBoostsStrengthMost() {
        AttributeGain g = XpCalculator.workoutGain(WorkoutType.STRENGTH, 60); // p = 3
        assertEquals(3, g.get(Attribute.STRENGTH));
        assertEquals(1, g.get(Attribute.VITALITY));
        assertEquals(0, g.get(Attribute.INTELLIGENCE));
    }

    @Test
    public void cardioWorkoutBoostsVitalityMost() {
        AttributeGain g = XpCalculator.workoutGain(WorkoutType.CARDIO, 60);
        assertEquals(3, g.get(Attribute.VITALITY));
        assertEquals(1, g.get(Attribute.STRENGTH));
    }

    @Test
    public void generalFitnessIsBalancedAndAlwaysImprovesBoth() {
        AttributeGain g = XpCalculator.workoutGain(WorkoutType.GENERAL, 25); // p = 1
        assertEquals(1, g.get(Attribute.STRENGTH));
        assertEquals(1, g.get(Attribute.VITALITY));
    }
}
