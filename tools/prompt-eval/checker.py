"""Rubric checks for one generated nudge (stdlib only).

check() is the automatic rubric. A nudge passes when ALL of these hold:
  - it cites at least one input number as the prompt shows it (a goal-only request has nothing to cite)
  - it invents none: no number that is wrong, misattributed, or belongs to a metric the input does not have
    (action numbers like "10-minute walk", clock times and numbers from the goal are fine)
  - 1-2 sentences and 12-50 words
  - no forbidden pattern: preamble, greeting, list, markdown, heading label, medical wording, leftover
    runtime tokens, AI-isms, echoed data labels, several lines, truncated output
Soft flags ("they/their", "I recommend", emoji, ...) are reported for manual review only.

semantic() adds heuristics for what a regex can still catch: a wrong reading of the numbers, a goal
contradiction, a nap for a sleep goal, a long nap, garbled text, hard training after short sleep or low
HRV, an HRV word that contradicts the value, late caffeine. Flags in HARD_SEM fail the nudge;
'two_actions?' is advisory.
"""
import math
import re

# ---------------------------------------------------------------- numbers: what does each number in the text refer to?
WORDNUM = {
    'two': 2, 'three': 3, 'four': 4, 'five': 5, 'six': 6, 'seven': 7, 'eight': 8,
    'nine': 9, 'ten': 10, 'eleven': 11, 'twelve': 12, 'fifteen': 15, 'twenty': 20,
    'thirty': 30, 'forty': 40, 'forty-five': 45, 'sixty': 60, 'ninety': 90,
}

NUM_RE = re.compile(
    r"(?<![\w.])(\d{1,3}(?:,\d{3})+|\d+(?:\.\d+)?)(\s?[kK]\b)?"
    r"|(?<![\w-])(" + "|".join(sorted(WORDNUM, key=len, reverse=True)) + r")(?![\w])",
    re.I)

# unit right after the number -> the metric (or kind of number) it claims to be
AFTER_UNITS = [
    ('action', re.compile(r"^\s?(-|to|–)\s?\d+\s?-?\s?(minutes?\b|mins?\b|seconds?\b)", re.I)),
    ('hours', re.compile(r"^\s?(-|to|–)\s?\d+(\.\d+)?\s?-?\s?(hours?\b|hrs?\b)", re.I)),
    ('action', re.compile(r"^\s?(more|extra|additional)\s+steps?\b", re.I)),
    ('pct', re.compile(r"^\s?(%|percent\b|per cent\b)", re.I)),
    ('hrvMs', re.compile(r"^\s?(ms\b|milliseconds?\b|msec\b)", re.I)),
    ('restingHR', re.compile(r"^\s?(bpm\b|beats?\b)", re.I)),
    ('stepsYesterday', re.compile(r"^\s?(\w+\s)?steps?\b", re.I)),
    ('hours', re.compile(r"^\s?-?\s?(hours?\b|hrs?\b|h\b)", re.I)),
    ('action', re.compile(r"^\s?-?\s?(minutes?\b|mins?\b|seconds?\b|secs?\b)", re.I)),
    ('clock', re.compile(r"^\s?(:\d\d|am\b|pm\b|a\.m\.|p\.m\.|o'?clock)", re.I)),
    ('action', re.compile(r"^\s?(\w+\s)?(deep\s)?(breaths?|times?|reps?|sets?|glass(es)?|cups?|sips?|push-?ups?|squats?|rounds?|stretch(es)?|laps?|flights?|blocks?|days?|nights?|weeks?)\b", re.I)),
    ('goalunit', re.compile(r"^\s?(pounds?|lbs?|kg|kilos?|km|miles?)\b", re.I)),
]

# metric named shortly before a number without a unit: (metric, regex, specificity)
LABELS = [
    ('hrvMs', re.compile(r"\b(hrv|heart rate variability)\b", re.I), 3),
    ('restingHR', re.compile(r"\b(resting heart rate|resting hr|heart rate|rhr|pulse)\b", re.I), 2),
    ('deepSleepPct', re.compile(r"\bdeep( sleep)?\b", re.I), 3),
    ('remSleepPct', re.compile(r"\brem( sleep)?\b", re.I), 3),
    ('stepsYesterday', re.compile(r"\b(walked|steps)\b", re.I), 2),
    ('sleepHours', re.compile(r"\b(slept|sleep|sleeping)\b", re.I), 1),
]

TARGET_CTX = re.compile(r"(aim|target|goal|try for|reach|get to|hit|at least|shoot for|recommended|ideal|between)\W+(\w+\W+){0,3}$", re.I)
APPROX_CTX = re.compile(r"(about|around|nearly|almost|roughly|over|under|just under|just over|close to)\s+$", re.I)
HOUR_ACTION_CTX = re.compile(r"^\s?-?\s?(hours?|hrs?)\s+(earlier|before|later|after|of|away|walk|nap|break)", re.I)


def nearest_label(before, max_gap=22, max_words=4):
    best = None
    for name, rx, spec in LABELS:
        for mm in rx.finditer(before):
            gap = before[mm.end():]
            if len(gap) > max_gap or len(gap.split()) > max_words or re.search(r"[.!?;]", gap):
                continue
            key = (mm.end(), spec)
            if best is None or key > best[0]:
                best = (key, name)
    return best[1] if best else None


def _to_num(tok, ksuf):
    t = tok.lower()
    if t in WORDNUM:
        return float(WORDNUM[t])
    v = float(t.replace(',', ''))
    if ksuf:
        v *= 1000
    return v


def _metric_values(m):
    vals = {}
    for k in ['sleepHours', 'deepSleepPct', 'remSleepPct', 'restingHR', 'hrvMs', 'stepsYesterday']:
        if k in m and m[k] is not None and m[k] != '':
            try:
                fv = float(m[k])
            except (TypeError, ValueError):
                continue
            if fv > 0:                                   # 0 = no data (mirrors the prompt builder)
                vals[k] = fv
    return vals


def _eq(k, v, x):
    if k == 'sleepHours':
        shown = math.floor(v * 10 + 0.5) / 10.0          # what the prompt shows (JS Math.round semantics)
        return abs(shown - x) < 0.051 or abs(v - x) < 0.051
    return abs(math.floor(v + 0.5) - x) < 0.5 or abs(v - x) < 0.5


def _goal_numbers(goal):
    out = set()
    for mt in NUM_RE.finditer(goal or ''):
        if mt.group(1):
            out.add(_to_num(mt.group(1), None))
            if mt.group(2):
                out.add(_to_num(mt.group(1), mt.group(2)))
        elif mt.group(3):
            out.add(float(WORDNUM[mt.group(3).lower()]))
    return out


def analyze_numbers(text, m):
    """Sort every number in the text into cited / bad / allowed / soft (target, approximation, small unknown)."""
    vals = _metric_values(m)
    goal_nums = _goal_numbers(m.get('userGoal'))
    cited, bad, allowed, soft = [], [], [], []
    for mt in NUM_RE.finditer(text):
        if mt.group(1):
            tok, ksuf = mt.group(1), mt.group(2)
        else:
            tok, ksuf = mt.group(3), None
        x = _to_num(tok, ksuf)
        after = text[mt.end():mt.end() + 30]
        before = text[max(0, mt.start() - 40):mt.start()]
        span = mt.group(0).strip()

        unit = None
        for name, rx in AFTER_UNITS:
            if rx.search(after):
                unit = name
                break
        label = nearest_label(before) if unit in (None, 'pct') else None

        # which metric does the text attribute this number to?
        claimed = None
        if unit == 'pct':
            la = after.lower()
            if re.search(r"^\s?(%|percent)\s*(of\s+)?(your\s+)?(sleep\s+)?(was\s+|in\s+|as\s+)?(deep)", la):
                claimed = 'deepSleepPct'
            elif re.search(r"^\s?(%|percent)\s*(of\s+)?(your\s+)?(sleep\s+)?(was\s+|in\s+|as\s+)?(rem)", la):
                claimed = 'remSleepPct'
            elif label in ('deepSleepPct', 'remSleepPct'):
                claimed = label
            else:
                claimed = 'pct_any'
        elif unit in ('hrvMs', 'restingHR', 'stepsYesterday'):
            claimed = unit
        elif unit == 'hours':
            claimed = 'sleepHours'
        elif unit in ('action', 'clock'):
            claimed = unit
        elif unit == 'goalunit':
            claimed = 'goalunit'
        elif label:
            claimed = label

        rec = {'n': span, 'x': x, 'claimed': claimed}

        # goal echo (e.g. "10K", "5 pounds") -> allowed, not a metric cite
        if x in goal_nums and claimed in (None, 'goalunit', 'action') and not any(_eq(k, v, x) for k, v in vals.items()):
            allowed.append(dict(rec, kind='goal_echo'))
            continue
        if ksuf and (x / 1000.0) in goal_nums and claimed is None:
            allowed.append(dict(rec, kind='goal_echo'))
            continue

        if claimed in vals:
            if _eq(claimed, vals[claimed], x):
                cited.append(dict(rec, metric=claimed))
            elif claimed == 'sleepHours' and HOUR_ACTION_CTX.search(after) and x <= 2:
                allowed.append(dict(rec, kind='action_hours'))
            elif claimed == 'stepsYesterday' and re.search(r"(extra|another|more|add|additional|adding)\W+(\w+\W+){0,1}$", before, re.I):
                allowed.append(dict(rec, kind='action_steps'))
            elif claimed == 'sleepHours' and TARGET_CTX.search(before) and 7 <= x <= 9:
                soft.append(dict(rec, kind='target_sleep'))
            elif TARGET_CTX.search(before):
                soft.append(dict(rec, kind='target'))
            elif APPROX_CTX.search(before) and abs(x - vals[claimed]) <= 0.1 * vals[claimed]:
                soft.append(dict(rec, kind='approx'))
            else:
                bad.append(dict(rec, kind='wrong_or_misattributed'))
            continue
        if claimed in ('sleepHours', 'hrvMs', 'restingHR', 'stepsYesterday', 'deepSleepPct', 'remSleepPct'):
            # attributed to a metric the input does not have
            bad.append(dict(rec, kind='metric_not_in_input'))
            continue
        if claimed == 'pct_any':
            hit = [k for k in ('deepSleepPct', 'remSleepPct') if k in vals and _eq(k, vals[k], x)]
            if hit:
                cited.append(dict(rec, metric=hit[0]))
            else:
                bad.append(dict(rec, kind='invented_pct'))
            continue
        if claimed in ('action', 'clock'):
            allowed.append(dict(rec, kind=claimed))
            continue
        if claimed == 'goalunit':
            bad.append(dict(rec, kind='invented_goal_unit'))
            continue
        # no unit, no label
        hit = [k for k, v in vals.items() if _eq(k, v, x)]
        if hit:
            cited.append(dict(rec, metric=hit[0], unlabeled=True))
        elif TARGET_CTX.search(before):
            soft.append(dict(rec, kind='target'))
        elif x <= 12 and not ksuf:
            soft.append(dict(rec, kind='small_unknown'))
        else:
            bad.append(dict(rec, kind='invented'))
    return cited, bad, allowed, soft


# ---------------------------------------------------------------- form: the automatic rubric
FORBIDDEN = [
    ('preamble', re.compile(r"^\s*(nudge|here'?s|here is|sure|certainly|of course|okay|ok|absolutely|great question)\b", re.I)),
    ('greeting', re.compile(r"^\s*(hi|hey|hello|good (morning|afternoon|evening)|dear|greetings)\b", re.I)),
    ('list', re.compile(r"(^|\n)\s*([-*•]|\d+[.)])\s+")),
    ('markdown', re.compile(r"\*\*|__|(^|\n)\s*#|`")),
    ('label', re.compile(r"(^|\n)\s*(nudge|tip|advice|suggestion|action|today'?s? (nudge|tip)|note|coach)\s*:|\b(sentence\s*\d|(first|second) sentence)\s*:", re.I)),
    ('medical', re.compile(r"\b(doctor|physician|healthcare|health care provider|medical|diagnos\w*|insomnia|apnea|disorder|medication|medicine|prescri\w*|melatonin|supplement\w*|symptoms?|treatment|therap\w*|clinical|disease|consult\w*|anxiety disorder|depression)\b", re.I)),
    ('tokens', re.compile(r"<\||\|>|<br|</?think>|</?s>|im_end|im_start|<\w+>", re.I)),
    ('aiism', re.compile(r"\b(as an ai|language model|i cannot|i can't provide|i'm sorry)\b", re.I)),
    ('data_echo', re.compile(r"\b(sleep last night|resting heart rate|hrv|steps yesterday|goal|summary|pace today)\s*:", re.I)),
]

SOFT = [
    ('third_person', re.compile(r"\b(the person|this person|the user|they should|their (sleep|hrv|heart|steps))\b", re.I)),
    ('first_person_coach', re.compile(r"\b(i recommend|i suggest|i'd suggest|i would)\b", re.I)),
    ('emoji', re.compile('[\U0001F300-\U0001FAFF☀-➿]')),
    ('multi_action_hint', re.compile(r"\b(also|additionally|and then|as well as|plus)\b|;", re.I)),
]

INTENSE = re.compile(r"\b(intense|intensity|hiit|hard (workout|run|session|training)|sprints?|sprinting|max(imum)? effort|push (yourself|hard)|long run|high-intensity|heavy (lifting|weights)|tempo run|intervals?)\b", re.I)


def sentences(text):
    t = re.sub(r"\b(a\.m\.|p\.m\.|e\.g\.|i\.e\.|vs\.)", lambda mm: mm.group(0).replace('.', ''), text, flags=re.I)
    parts = [p for p in re.split(r"(?<=[.!?])[\"')\]]*\s+", t.strip()) if p.strip()]
    return parts


def check(text, m, finish_reason=None):
    """Automatic rubric for one nudge; `auto_pass` is the verdict, the other fields say why."""
    t = (text or '').strip()
    res = {}
    cited, bad, allowed, soft = analyze_numbers(t, m)
    res['cited'] = cited
    res['bad_numbers'] = bad
    res['allowed_numbers'] = allowed
    res['soft_numbers'] = soft
    sents = sentences(t)
    res['n_sent'] = len(sents)
    res['n_words'] = len(t.split())
    res['ends_clean'] = bool(re.search(r"[.!?][\"')\]]*$", t))
    flags = [name for name, rx in FORBIDDEN if rx.search(t)]
    if '\n' in t:
        flags.append('multiline')
    if finish_reason == 'length' or not res['ends_clean']:
        flags.append('truncated')
    res['flags'] = flags
    sflags = [name for name, rx in SOFT if rx.search(t)]
    low_signal = (m.get('sleepHours') is not None and float(m['sleepHours']) < 6) or \
                 (m.get('hrvMs') is not None and float(m['hrvMs']) < 35) or \
                 ('recover' in (m.get('userGoal') or '').lower()) or ('rest' in (m.get('userGoal') or '').lower())
    if low_signal and INTENSE.search(t):
        sflags.append('intense_despite_low_signal')
    res['soft_flags'] = sflags
    res['cites'] = len(cited) > 0 or not _metric_values(m)   # nothing to cite for goal-only input
    res['n_metrics_cited'] = len(set(c['metric'] for c in cited))
    res['numbers_ok'] = len(bad) == 0
    res['length_ok'] = 1 <= res['n_sent'] <= 2 and 12 <= res['n_words'] <= 50
    res['length_ideal'] = 1 <= res['n_sent'] <= 2 and 20 <= res['n_words'] <= 45
    res['forbidden_ok'] = len(flags) == 0
    res['auto_pass'] = bool(t) and res['cites'] and res['numbers_ok'] and res['length_ok'] and res['forbidden_ok']
    return res


# ---------------------------------------------------------------- semantic heuristics (manual review is the arbiter)
POS_STATE = re.compile(r"\b(more (energi[sz]ed|rested|refreshed|alert)|well[- ]rested|feel(ing)? (rested|refreshed|great|energi[sz]ed|good)|refreshed|"
                       r"better sleep (state|stage)|good sleep (stage|state)|good sleep stage|(energy|energy levels) (is|are|will be|may be|might be) (high|up|on the rise)|"
                       r"on the rise|in great shape|fully recovered|well recovered|ready to (tackle|push|go hard)|full of energy|high energy|"
                       r"back to normal|recovered well|in a good (sleep )?(state|place|stage))\b", re.I)
NEG_STATE = re.compile(r"\b(tired|drained|run-down|rundown|not (feel )?rested|sluggish|fatigued?|exhausted|off the mark|short on sleep|"
                       r"(energy|mood) (may|might|could|will) (dip|drop|be (a bit )?low)|low energy|need(s)? (more )?rest|under-?recovered)\b", re.I)
NAP = re.compile(r"\bnap\b|\bnaps\b|\bnapping\b", re.I)
LONG_NAP = re.compile(r"\b(2[5-9]|[3-9]\d|\d{3})[- ]?min\w*[- ](power )?nap|\bnap\w* (of|for) (2[5-9]|[3-9]\d)|nap\w*[^.]{0,25}\b(hour|hours)\b|\b(an|1|one)[- ]hour nap", re.I)
GARBLE_CORE = re.compile(r"\b\d+[- ]minutes?[- ][\w ]{0,20}\bof \d+[- ]?(minutes?|hours?)\b|\b(\w{3,}) \2\b", re.I)
_REP3 = re.compile(r"\b(\w+ \w+ \w+)\b(?=.*\b\1\b)", re.I)


class _Garble(object):
    """Nap/duration garble, doubled word, or a repeated trigram that carries content (a digit or a 5+ letter
    word) and is not a breathing count ("for a count of 4 ... for a count of 4" is legitimate)."""

    def search(self, t):
        mm = GARBLE_CORE.search(t or '')
        if mm:
            return mm
        for mm in _REP3.finditer(t or ''):
            tri = mm.group(1)
            if 'count' in tri.lower():
                continue
            if re.search(r"\d", tri) or any(len(w) >= 5 for w in tri.split()):
                return mm
        return None


GARBLE = _Garble()
VERBS = r"(take|try|drink|eat|go|get|do|make|add|set|spend|walk|stretch|avoid|skip|aim|consider|practice|put|turn|keep|limit|write|call|have|grab|swap|start|finish|schedule|plan|breathe|focus|listen|read|meditate|head|step|sit|lie|jot|leave|cut|switch|dim|stop|replace|enjoy|use|find|treat|give|move|pick|book|prepare)"
TWO_ACTIONS = re.compile(r"(\b(and|then|or|also|plus)\b|,|;)\s+(try to |be sure to |make sure to )?" + VERBS + r"\b", re.I)
CUT_GOAL = re.compile(r"\b(less|fewer|stop|quit|cut (back|down)( on)?|reduce|without|avoid|skip|no more|limit)\s+(\w+(?:\s\w+){0,2})", re.I)
CONSUME = r"(drink(?:ing)?|hav(?:e|ing)|grab(?:bing)?|enjoy(?:ing)?|sip(?:ping)?|get(?:ting)?|pour(?:ing)?|tak(?:e|ing)|eat(?:ing)?|reach(?:ing)? for|keep(?:ing)?|try(?:ing)?)"
NEGATORS = re.compile(r"\b(instead of|skip|swap|less|no|not|avoid|cut|replace|without|limit|before|fewer|stop|instead|last)\b", re.I)
NEGATED_BEFORE = re.compile(r"\b(instead of|swap|skip|avoid|no|not|rather than|any|replace|without|than|save|postpone|leave)\b[^.]*$", re.I)
PUSH = re.compile(r"\bready for (a bit |a little )?more\b|\bready to push\b|\bpush (a little|a bit|harder|yourself)\b|"
                  r"\bincrease (your|the) (intensity|pace)\b|\badd (\d+|a few|some) (extra )?minutes to (your|today's) (usual )?(workout|run|training)", re.I)
LATE_CAFFEINE = re.compile(r"\b(drink|drinking|have|having|grab|enjoy|sip|try)\b[^.]{0,40}\b(coffee|caffeine|espresso|energy drink)\b[^.]{0,40}\b(before bed|before bedtime|at night|tonight|this evening|in the evening)\b", re.I)
CAFFEINE_OK = re.compile(r"\b(last|no|avoid|skip|stop|cut|limit|instead|swap|replace|less|fewer|decaf|herbal)\b", re.I)

# HRV adjective vs the band the prompt builder computes (low < 35 <= average < 50 <= good)
HRV_ADJ = re.compile(r"\bHRV (?:is |was |of )?(?:a |an )?(low|average|good|high|strong|great|solid|healthy|poor|normal|moderate)\b(?: at)?\s*(\d+)?", re.I)
ADJ_CLASS = {'low': 'low', 'poor': 'low', 'average': 'average', 'normal': 'average', 'moderate': 'average',
             'good': 'good', 'high': 'good', 'strong': 'good', 'great': 'good', 'solid': 'good', 'healthy': 'good'}

# semantic flags that fail a nudge
HARD_SEM = ('late_caffeine', 'push_despite_low', 'interp_too_positive', 'interp_too_negative', 'goal_contradiction', 'nap_for_sleep_goal', 'long_nap', 'garble', 'intense_despite_low', 'hrv_word_serious')


def _cut_targets(goal):
    out = []
    for mm in CUT_GOAL.finditer(goal or ''):
        words = [w for w in re.findall(r"[a-z]+", mm.group(4).lower()) if w not in (
            'and', 'the', 'my', 'a', 'on', 'at', 'to', 'in', 'of', 'work', 'late', 'night', 'time', 'day', 'today', 'week',
            'morning', 'evening', 'still', 'this', 'so', 'much')]
        if words:
            w = words[0]
            out.append(re.sub(r"(ing|s)$", "", w) if len(w) > 5 else w)
    return out


def _state(m):
    sl = m.get('sleepHours'); hv = m.get('hrvMs')
    low = (sl is not None and float(sl) < 6.5) or (hv is not None and float(hv) < 40)
    high = (sl is None or float(sl) >= 7) and (hv is not None and float(hv) >= 55)
    return 'low' if low else ('high' if high else 'mid')


def _low_state(m):
    try:
        sl = float(m['sleepHours']) if m.get('sleepHours') not in (None, '') else None
    except (TypeError, ValueError):
        sl = None
    try:
        hv = float(m['hrvMs']) if m.get('hrvMs') not in (None, '') else None
    except (TypeError, ValueError):
        hv = None
    return (sl is not None and 0 < sl and round(sl + 1e-9, 1) < 6) or (hv is not None and 0 < hv and hv < 34.5)


def hrv_word_check(text, m):
    """'' if fine / not stated; 'boundary' if off by one band; 'serious' if low<->good."""
    if m.get('hrvMs') in (None, ''):
        return ''
    mm = HRV_ADJ.search(text or '')
    if not mm:
        return ''
    v = round(float(m['hrvMs']))
    truth = 'low' if v < 35 else ('average' if v < 50 else 'good')
    said = ADJ_CLASS[mm.group(1).lower()]
    if said == truth:
        return ''
    return 'serious' if {said, truth} == {'low', 'good'} else 'boundary'


def semantic(text, m):
    """List of semantic flags for one nudge (see HARD_SEM for the ones that fail it)."""
    t = text or ''
    flags = []
    st = _state(m)
    sents = sentences(t)
    first = sents[0] if sents else t
    if st == 'low':
        for mm in POS_STATE.finditer(first):
            if re.search(r"\b(help(s|ing)?( you)?|to|key to( an?)?|for( an?)?)( feel| wake up| be)?\s*$", first[max(0, mm.start() - 24):mm.start()], re.I):
                continue          # benefit framing, e.g. "a walk will help you feel more energized"
            flags.append('interp_too_positive')
            break
    if st == 'high' and NEG_STATE.search(first):
        flags.append('interp_too_negative')
    goal = (m.get('userGoal') or '').lower()
    for tgt in _cut_targets(goal):
        for mm in re.finditer(r"\b" + CONSUME + r"\b[^.]{0,30}?\b" + re.escape(tgt), t, re.I):
            window = t[max(0, mm.start() - 25):mm.end()]
            after = t[mm.end():mm.end() + 30]
            if not NEGATORS.search(window) and not re.search(r"^\w*\s*(less|fewer)\b|\bthan usual\b", after, re.I):
                flags.append('goal_contradiction'); break
    if NAP.search(t) and ('sleep' in goal or 'bed' in goal or 'wind down' in goal):
        flags.append('nap_for_sleep_goal')
    if LONG_NAP.search(t):
        flags.append('long_nap')
    if GARBLE.search(t):
        flags.append('garble')
    action = sents[-1] if len(sents) >= 2 else t
    if TWO_ACTIONS.search(action):
        flags.append('two_actions?')
    if _low_state(m):
        for mm in INTENSE.finditer(t):
            if not NEGATED_BEFORE.search(t[max(0, mm.start() - 30):mm.start()]):
                flags.append('intense_despite_low'); break
        if PUSH.search(t):
            flags.append('push_despite_low')
    hw = hrv_word_check(t, m)
    if hw:
        flags.append('hrv_word_' + hw)
    for s_ in sentences(t):
        mm = LATE_CAFFEINE.search(s_)
        if mm and not CAFFEINE_OK.search(s_):
            flags.append('late_caffeine')
            break
    return flags


def score(text, m):
    """Both verdicts for one cleaned nudge, plus why it failed (empty when it passes both)."""
    chk = check(text, m)
    sem = semantic(text, m)
    hard = [f for f in sem if f in HARD_SEM]
    why = ([] if chk['cites'] else ['no_cite']) + ([] if chk['numbers_ok'] else ['bad_number']) \
        + ([] if chk['length_ok'] else ['length']) + chk['flags'] + hard
    return {'rubric': chk['auto_pass'], 'semantic': chk['auto_pass'] and not hard, 'why': why,
            'check': chk, 'semantic_flags': sem}
