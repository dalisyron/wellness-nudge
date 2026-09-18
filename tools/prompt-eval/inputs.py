"""Evaluation inputs. Field names match the /nudge request body; `id` is only for the logs."""

# Core set: one input per common goal, plus a missing goal and a partial request.
CORE = [
    # short sleep + low HRV + sleep goal
    {'id': 'T1_sleep', 'sleepHours': 5.0, 'deepSleepPct': 11, 'remSleepPct': 14,
     'restingHR': 70, 'hrvMs': 28, 'stepsYesterday': 3200, 'userGoal': 'Sleep better tonight'},
    # fatigue after a poor night
    {'id': 'T2_fatigue', 'sleepHours': 5.5, 'deepSleepPct': 13, 'remSleepPct': 15,
     'restingHR': 68, 'hrvMs': 32, 'stepsYesterday': 4500, 'userGoal': 'Feel less tired this afternoon'},
    # recovery after a long hike (big step count, HRV dipped)
    {'id': 'T3_hike', 'sleepHours': 7.2, 'deepSleepPct': 19, 'remSleepPct': 20,
     'restingHR': 64, 'hrvMs': 41, 'stepsYesterday': 21400, 'userGoal': "Recover from yesterday's hike"},
    # stress before meetings
    {'id': 'T4_stress', 'sleepHours': 6.3, 'deepSleepPct': 15, 'remSleepPct': 18,
     'restingHR': 63, 'hrvMs': 43, 'stepsYesterday': 7000, 'userGoal': 'Feel less tense in meetings today'},
    # 10K training with good HRV
    {'id': 'T5_10k', 'sleepHours': 7.5, 'deepSleepPct': 20, 'remSleepPct': 22,
     'restingHR': 58, 'hrvMs': 62, 'stepsYesterday': 11000, 'userGoal': 'Train consistently for my 10K'},
    # mood
    {'id': 'T6_mood', 'sleepHours': 5.7, 'deepSleepPct': 13, 'remSleepPct': 16,
     'restingHR': 67, 'hrvMs': 33, 'stepsYesterday': 4800, 'userGoal': 'Lift my mood today'},
    # no goal given
    {'id': 'T7_nogoal', 'sleepHours': 6.8, 'deepSleepPct': 17, 'remSleepPct': 20,
     'restingHR': 61, 'hrvMs': 50, 'stepsYesterday': 8000},
    # a cut-back goal: must not turn into coffee advice
    {'id': 'T8_coffee', 'sleepHours': 6.0, 'deepSleepPct': 14, 'remSleepPct': 17,
     'restingHR': 65, 'hrvMs': 39, 'stepsYesterday': 5500, 'userGoal': 'Drink less coffee and still feel awake'},
    # partial input: must not invent HRV etc.
    {'id': 'T9_partial', 'sleepHours': 6.5, 'stepsYesterday': 2100, 'userGoal': 'Move more today'},
]

# Edge cases: no metrics, float noise, strings and zeros, single fields.
EDGE = [
    {'id': 'T10_goalonly_sleep', 'userGoal': 'Sleep better tonight'},
    {'id': 'T11_goalonly_stress', 'userGoal': 'Feel less stressed at work'},
    # T6_mood with float noise: a Kotlin Float slider value sent as a Double
    {'id': 'T12_floatnoise', 'sleepHours': 5.699999809265137, 'deepSleepPct': 13.0, 'remSleepPct': 16.0,
     'restingHR': 67, 'hrvMs': 33, 'stepsYesterday': 4800, 'userGoal': 'Lift my mood today'},
    # an API client sending strings, a zero (0 = no data) and a messy goal
    {'id': 'T13_strings_zero', 'sleepHours': '6.25', 'hrvMs': '0', 'restingHR': 0, 'stepsYesterday': '9800.4',
     'userGoal': '  Stay   consistent with\nworkouts '},
    {'id': 'P1_sleeponly', 'sleepHours': 5.6, 'userGoal': 'Sleep better tonight'},
    {'id': 'P2_hrv_steps', 'hrvMs': 58, 'stepsYesterday': 9400, 'userGoal': 'Train for my 10K'},
    {'id': 'P3_rhr_only', 'restingHR': 72, 'userGoal': 'Feel less stressed at work'},
    {'id': 'P4_stages_nogoal', 'sleepHours': 6.4, 'deepSleepPct': 13, 'remSleepPct': 17},
]

# The four demo scenarios, the first entries of "Sample day" in the app (MockNudgeInputs.kt).
DEMO = [
    {'id': 'D1_sleep_hero', 'sleepHours': 5.2, 'deepSleepPct': 12, 'remSleepPct': 15,
     'restingHR': 68, 'hrvMs': 29, 'stepsYesterday': 3400, 'userGoal': 'Sleep better tonight'},
    {'id': 'D2_stress', 'sleepHours': 6.1, 'deepSleepPct': 15, 'remSleepPct': 18,
     'restingHR': 66, 'hrvMs': 34, 'stepsYesterday': 5200, 'userGoal': 'Feel less stressed before my big presentation'},
    {'id': 'D3_hike', 'sleepHours': 7.8, 'deepSleepPct': 21, 'remSleepPct': 22,
     'restingHR': 63, 'hrvMs': 44, 'stepsYesterday': 23500, 'userGoal': "Recover from yesterday's long hike"},
    {'id': 'D4_10k', 'sleepHours': 7.9, 'deepSleepPct': 20, 'remSleepPct': 23,
     'restingHR': 55, 'hrvMs': 68, 'stepsYesterday': 11200, 'userGoal': 'Train for my first 10K'},
]

SETS = {'core': CORE, 'edge': EDGE, 'demo': DEMO, 'all': CORE + EDGE + DEMO}
SET_OF = {t['id']: name for name in ('core', 'edge', 'demo') for t in SETS[name]}


def metrics_of(t):
    """The request body the app would send (everything but the id)."""
    return {k: v for k, v in t.items() if k != 'id'}
