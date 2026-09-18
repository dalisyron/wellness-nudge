// Post-processing for raw model output. Strips runtime/special tokens and
// formatting, keeps the two-sentence shape, and repairs the rare HRV wording
// that contradicts the user's actual value. Duktape-safe regexes only (no
// lookbehind, no named groups, no s/u/y flags).
const QUOTES = /^["'“”‘’]+|["'“”‘’]+$/g;

const hrvWordFor = (v) => {
  if (v < 35) return 'low';
  if (v < 50) return 'average';
  return 'good';
};

const cleanText = (s, finishReason, metrics) => {
  let t = String(s || '')
    .replace(/<think>[\s\S]*?<\/think>/g, '') // Qwen3 reasoning block (empty with /no_think)
    .replace(/<\/?think>/g, '')
    .replace(/<\|[^|]*\|>\s*\d+%/g, '') // mILM progress markers
    .replace(/<\|[^|]*\|>/g, '') // other special tokens
    .replace(/<br\s*\/?>/gi, ' ')
    .replace(/\*\*|__|`/g, '') // markdown emphasis / code
    .replace(/^\s*#+\s*/, '') // leading heading marker
    .replace(/\s+/g, ' ') // single line
    .trim()
    .replace(QUOTES, '')
    .trim()
    .replace(/^(nudge|today'?s nudge|tip|here'?s (your|a|my|the) (nudge|tip)[^:]*)\s*:\s*/i, '')
    .replace(/\b(sentence\s*\d|(first|second) sentence)\s*:\s*/gi, '')
    .replace(QUOTES, '')
    .trim()
    // SmolLM2 slip: "a 10-minute power nap of 20 minutes" -> "a power nap of 20 minutes"
    .replace(/\b\d+-minute (power )?nap of (\d+(?:-\d+)?) minutes?/gi, (all, power, mins) => `${power || ''}nap of ${mins} minutes`);

  // SmolLM2 slip: wrong HRV word for the user's actual value ("HRV is high at 28 ms" -> "low")
  const hrvRaw = metrics ? Number(metrics.hrvMs) : NaN;
  if (!isNaN(hrvRaw) && isFinite(hrvRaw) && hrvRaw > 0) {
    const hrv = Math.round(hrvRaw);
    t = t.replace(/\bHRV is (?:a |an )?(low|average|good|high|strong|great|solid|normal|healthy|poor|moderate) at (\d+)/g,
      (all, word, n) => (Number(n) === hrv ? `HRV is ${hrvWordFor(hrv)} at ${n}` : all));
  }

  // keep at most two sentences (sentence 1 = the numbers, sentence 2 = the action)
  const ends = /[.!?](?=\s+[A-Z])/g;
  let count = 0;
  let mt = ends.exec(t);
  while (mt !== null) {
    count += 1;
    if (count === 2) {
      t = t.slice(0, mt.index + 1);
      break;
    }
    mt = ends.exec(t);
  }

  if (t && !/[.!?]$/.test(t)) {
    const cut = Math.max(t.lastIndexOf('. '), t.lastIndexOf('! '), t.lastIndexOf('? '));
    if (finishReason === 'length' && cut > 0) {
      t = t.slice(0, cut + 1); // drop the fragment cut off by max_tokens
    } else {
      t += '.'; // complete thought that just lacks a full stop
    }
  }
  return t;
};

module.exports = { cleanText };
