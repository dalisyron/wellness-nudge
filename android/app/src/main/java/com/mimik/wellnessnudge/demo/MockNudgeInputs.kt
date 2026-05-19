package com.mimik.wellnessnudge.demo

/**
 * Pre-filled nudge inputs used ONLY by the demo "Load sample metrics and
 * goal" button on the Home screen. These are not saved as nudges, not
 * shown anywhere in the app, and never participate in tip aggregation or
 * the classifier's training/keyword logic — they're just convenient
 * slider/field values so the demo doesn't fumble with knobs on stage.
 *
 * The 30 entries below all describe the SAME hypothetical person — a 30-ish
 * moderately fit adult — across a few weeks of normal variation. Sleep
 * 5–8 h, deep 11–22%, REM 14–25%, resting HR 56–72 bpm, HRV 27–65 ms,
 * steps 2.5k–14k. Goals are real things someone might actually open the
 * app to ask about. The wording is chosen so the wellness-nudge mim's
 * keyword classifier picks up a specific category for most entries; a
 * couple are intentionally ambiguous so the demo can show the
 * "category: other" fallback too.
 */

data class MockNudgeInput(
    val sleepHours: Float,
    val deepSleepPct: Float,
    val remSleepPct: Float,
    val restingHR: Float,
    val hrvMs: Float,
    val stepsYesterday: Float,
    val userGoal: String,
)

object MockNudgeInputs {

    val ALL: List<MockNudgeInput> = listOf(
        // 1. Sleep — short night, low deep%, low HRV
        MockNudgeInput(5.0f, 11f, 14f, 70f, 28f, 3200f, "Sleep better tonight"),
        // 2. Fatigue — tired after a poor night
        MockNudgeInput(5.5f, 13f, 15f, 68f, 32f, 4500f, "Feel less tired this afternoon"),
        // 3. Recovery — yesterday was a long hike
        MockNudgeInput(6.0f, 14f, 17f, 64f, 40f, 9800f, "Recover from yesterday's hike"),
        // 4. Weight — average baseline, low activity
        MockNudgeInput(6.5f, 15f, 18f, 62f, 48f, 5200f, "Lose 5 pounds this month"),
        // 5. Fitness — building base, decent HRV
        MockNudgeInput(7.0f, 18f, 20f, 60f, 55f, 9500f, "Train consistently for my 10K"),
        // 6. Fitness — running habit
        MockNudgeInput(7.5f, 20f, 22f, 58f, 62f, 11000f, "Build a steady running habit"),
        // 7. Recovery — explicit rest day
        MockNudgeInput(8.0f, 21f, 23f, 57f, 65f, 4200f, "Take a real rest day today"),
        // 8. Stress — workday pressure
        MockNudgeInput(6.2f, 16f, 19f, 63f, 42f, 5800f, "Reduce my work stress this week"),
        // 9. Stress — anxious before a meeting
        MockNudgeInput(5.8f, 14f, 16f, 66f, 35f, 4200f, "Be less anxious today"),
        // 10. Sleep — wind-down focus
        MockNudgeInput(6.0f, 15f, 18f, 65f, 38f, 5500f, "Wind down earlier before bedtime"),
        // 11. Sleep — chronic short sleep
        MockNudgeInput(5.5f, 12f, 15f, 69f, 30f, 3800f, "Feel more rested this week"),
        // 12. Fitness — consistency
        MockNudgeInput(7.0f, 17f, 20f, 62f, 50f, 12500f, "Stay consistent with workouts"),
        // 13. Fitness — cardio focus
        MockNudgeInput(6.8f, 16f, 19f, 61f, 46f, 10200f, "Improve my cardio endurance"),
        // 14. Appetite — fuelling the morning
        MockNudgeInput(6.5f, 15f, 18f, 64f, 42f, 6500f, "Eat more at breakfast"),
        // 15. Mood — flat day
        MockNudgeInput(5.7f, 13f, 16f, 67f, 33f, 4800f, "Lift my mood today"),
        // 16. Mood — needs a boost
        MockNudgeInput(6.0f, 14f, 17f, 65f, 38f, 5200f, "Cheer myself up after a rough week"),
        // 17. Weight — explicit weight-loss framing
        MockNudgeInput(7.2f, 19f, 21f, 59f, 58f, 13500f, "Drop weight before summer"),
        // 18. Stress — meetings-heavy day
        MockNudgeInput(6.3f, 15f, 18f, 63f, 43f, 7000f, "Feel less tense in meetings today"),
        // 19. Fatigue — chronic
        MockNudgeInput(5.0f, 11f, 14f, 71f, 27f, 2800f, "Manage my fatigue better"),
        // 20. Recovery — between training blocks
        MockNudgeInput(6.5f, 16f, 19f, 62f, 47f, 8500f, "Recover well between training sessions"),
        // 21. Fitness — add resistance
        MockNudgeInput(7.0f, 18f, 20f, 60f, 52f, 9200f, "Add light strength training to my week"),
        // 22. Appetite — post-illness
        MockNudgeInput(6.7f, 17f, 20f, 61f, 49f, 4500f, "Get my appetite back after being sick"),
        // 23. Sleep — morning energy
        MockNudgeInput(5.8f, 13f, 16f, 67f, 34f, 4000f, "Wake up easier in the morning"),
        // 24. Other — intentionally not a keyword goal
        MockNudgeInput(6.0f, 14f, 17f, 65f, 39f, 5500f, "Drink less coffee and still feel awake"),
        // 25. Fitness — marathon training
        MockNudgeInput(7.5f, 20f, 22f, 58f, 60f, 14000f, "Train for a half marathon this fall"),
        // 26. Fatigue — afternoon slump
        MockNudgeInput(6.2f, 15f, 18f, 64f, 41f, 6800f, "Feel less drained at the end of the day"),
        // 27. Recovery — getting back on track
        MockNudgeInput(6.8f, 17f, 20f, 61f, 50f, 8000f, "Recover from a tough training block"),
        // 28. Other — intentionally ambiguous
        MockNudgeInput(5.5f, 12f, 15f, 68f, 31f, 3500f, "Stop snacking late at night"),
        // 29. Stress — evening unwind
        MockNudgeInput(6.0f, 14f, 17f, 64f, 40f, 6000f, "Find ways to relax in the evening"),
        // 30. Weight — concrete goal
        MockNudgeInput(7.0f, 18f, 20f, 60f, 55f, 10500f, "Lose 5 pounds before my vacation"),
    )
}
