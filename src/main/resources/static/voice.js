import {
  BREATH_LEVEL, CONSONANTS, DRIFT, EFFORT_TILT, FORMANT_WANDER, FUNDAMENTAL_BOOST, GLIDE_SECONDS, JITTER, LEAD_SECONDS, NOTE_LEVEL_SPREAD, OVERSHOOT,
  PHRASE_ATTACK, PHRASE_RELEASE, RELEASE_FALL, REVERB_MIX, SCOOP, SHIMMER, SOURCE_TILT, SWELL, TRANSITION_SECONDS, VIBRATO, VOICE_CONTROL, VOICE_HARMONICS,
  VOCAL_TRACT, VOICE_RATE, VOICING_SECONDS, VOWELS,
} from './constants.js';

const hz = (m) => 440 * 2 ** ((m - 69) / 12);
const clamp = (x) => Math.min(1, Math.max(0, x));
const ease = (x) => {
  const c = clamp(x);
  return c * c * (3 - 2 * c);
};
const resonance = (f, [centre, width]) => (centre * centre) / Math.hypot(centre * centre - f * f, width * f);

function noiseSource() {
  let state = 0x9e3779b9;
  return () => {
    state ^= state << 13;
    state ^= state >>> 17;
    state ^= state << 5;
    return (state >>> 0) / 2147483648 - 1;
  };
}

function bandpass(centre, q) {
  const w = (2 * Math.PI * centre) / VOICE_RATE, alpha = Math.sin(w) / (2 * q), a0 = 1 + alpha;
  const b0 = alpha / a0, a1 = (-2 * Math.cos(w)) / a0, a2 = (1 - alpha) / a0;
  let x1 = 0, x2 = 0, y1 = 0, y2 = 0;
  return (x) => {
    const y = b0 * (x - x2) - a1 * y1 - a2 * y2;
    x2 = x1;
    x1 = x;
    y2 = y1;
    y1 = y;
    return y;
  };
}

function reverb(dry) {
  const scale = VOICE_RATE / 44100;
  const combs = [1116, 1188, 1277, 1356].map((d) => ({ line: new Float32Array(Math.round(d * scale)), at: 0, low: 0 }));
  const passes = [556, 441].map((d) => ({ line: new Float32Array(Math.round(d * scale)), at: 0 }));
  return dry.map((x) => {
    let wet = 0;
    for (const comb of combs) {
      const y = comb.line[comb.at];
      comb.low = 0.7 * y + 0.3 * comb.low;
      comb.line[comb.at] = x + 0.78 * comb.low;
      comb.at = (comb.at + 1) % comb.line.length;
      wet += y / 4;
    }
    for (const pass of passes) {
      const y = pass.line[pass.at];
      pass.line[pass.at] = wet + 0.5 * y;
      pass.at = (pass.at + 1) % pass.line.length;
      wet = y - wet;
    }
    return (1 - REVERB_MIX) * x + REVERB_MIX * wet;
  });
}

// One unit-variance step of a first-order random walk, so a larger keep wanders more slowly.
const wander = (value, keep, random) => keep * value + Math.sqrt(3 * (1 - keep * keep)) * random();

function moment(phrase, seconds, t, motion) {
  const at = (t - LEAD_SECONDS) / seconds, i = Math.max(0, phrase.findLastIndex((step) => step.start <= at));
  const step = phrase[i], local = t - LEAD_SECONDS - step.start * seconds, [consonant, vowel] = step.syllable, spec = CONSONANTS[consonant];
  const from = i ? phrase[i - 1].note : step.note - SCOOP, leap = step.note - from;
  const overshoot = Math.sign(leap) * Math.min(OVERSHOOT.max, OVERSHOOT.ratio * Math.abs(leap)) * Math.sin(Math.PI * clamp(local / (2 * GLIDE_SECONDS))) ** 2;
  const depth = VIBRATO.depth * (1 + VIBRATO.depthWander * motion.depth);
  const vibrato = depth * ease((local - VIBRATO.delay) / VIBRATO.rise) * Math.sin(motion.wobble);
  const fall = RELEASE_FALL * (1 - ease((motion.end - t) / PHRASE_RELEASE));
  const f0 = hz(from + leap * ease(local / GLIDE_SECONDS) + overshoot + motion.drift + JITTER * motion.jitter + vibrato - fall);
  const opening = ease((local - spec.closure) / TRANSITION_SECONDS), stretch = VOCAL_TRACT * (1 + FORMANT_WANDER * motion.formant);
  const formants = VOWELS[vowel].map(([centre, width], k) => {
    const start = spec.loci[k] ?? centre;
    return [(start + (centre - start) * opening) * stretch, width];
  });
  formants[0][0] = Math.max(formants[0][0], 1.15 * f0);
  return {
    f0,
    formants,
    loud: step.level * (SWELL.floor + (1 - SWELL.floor) * ease(local / SWELL.seconds)),
    voice: spec.voice + (1 - spec.voice) * ease((local - spec.closure) / VOICING_SECONDS),
    sibilance: (spec.hiss ?? 0) * ease(local / VOICING_SECONDS) * ease((spec.closure - local) / VOICING_SECONDS),
    pop: local >= spec.closure && local < spec.closure + 0.012 ? (spec.burst ?? 0) : 0,
  };
}

// A softer voice loses its upper harmonics faster, as a singer's does.
function harmonics({ f0, formants, voice, loud }, gains) {
  const tilt = SOURCE_TILT + EFFORT_TILT * (1 - loud);
  let count = 0, power = 0;
  for (let h = 1; h <= VOICE_HARMONICS && h * f0 < 0.45 * VOICE_RATE; h++) {
    gains[h] = (h === 1 ? FUNDAMENTAL_BOOST : h ** -tilt) * formants.reduce((g, formant) => g * resonance(h * f0, formant), 1);
    power += gains[h] ** 2;
    count = h;
  }
  return { count, level: (voice * loud) / Math.sqrt(power) };
}

function normalise(samples) {
  const peak = samples.reduce((max, v) => Math.max(max, Math.abs(v)), 0);
  return samples.map((v) => (0.8 * v) / peak);
}

export function sing(steps, seconds) {
  const random = noiseSource();
  let beat = 0;
  const phrase = steps.map((step) => {
    const placed = { ...step, start: beat, level: 1 - NOTE_LEVEL_SPREAD * (random() + 1) / 2 };
    beat += step.beats;
    return placed;
  });
  const end = LEAD_SECONDS + beat * seconds, total = Math.round((end + LEAD_SECONDS) * VOICE_RATE);
  const out = new Float32Array(total), phase = new Float64Array(VOICE_HARMONICS + 1), gains = new Float64Array(VOICE_HARMONICS + 1);
  const hiss = bandpass(6500, 1.2), breath = bandpass(2500, 0.6), burst = bandpass(1500, 0.8);
  const motion = { end, drift: 0, wobble: 0, jitter: 0, shimmer: 0, rate: 0, depth: 0, formant: 0 };
  for (let block = 0; block < total; block += VOICE_CONTROL) {
    const t = block / VOICE_RATE;
    motion.drift = 0.98 * motion.drift + (DRIFT * random()) / 2;
    motion.jitter = wander(motion.jitter, 0.5, random);
    motion.shimmer = wander(motion.shimmer, 0.5, random);
    motion.rate = wander(motion.rate, 0.997, random);
    motion.depth = wander(motion.depth, 0.997, random);
    motion.formant = wander(motion.formant, 0.995, random);
    motion.wobble += (2 * Math.PI * VIBRATO.rate * (1 + VIBRATO.rateWander * motion.rate) * VOICE_CONTROL) / VOICE_RATE;
    const now = moment(phrase, seconds, t, motion), { count, level } = harmonics(now, gains);
    const step = (2 * Math.PI * now.f0) / VOICE_RATE, envelope = ease((t - LEAD_SECONDS) / PHRASE_ATTACK) * ease((end - t) / PHRASE_RELEASE);
    const shake = 1 + SHIMMER * motion.shimmer;
    for (let k = block; k < Math.min(total, block + VOICE_CONTROL); k++) {
      let sample = 0;
      for (let h = 1; h <= count; h++) {
        phase[h] += h * step;
        sample += gains[h] * Math.sin(phase[h]);
      }
      const noise = random(), pulse = 0.6 + 0.4 * Math.cos(phase[1]);
      out[k] = envelope * (shake * level * sample + BREATH_LEVEL * now.voice * pulse * breath(noise) + now.sibilance * hiss(noise) + now.pop * burst(noise));
    }
  }
  return { dry: normalise(out), wet: normalise(reverb(out)) };
}
