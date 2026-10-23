export const RATE = 16000;
export const MIN_HZ = 65;
export const MAX_HZ = 1100;
export const SILENCE_DB = -50;
export const YIN_THRESHOLD = 0.2;
export const ANALYSER_SIZE = 2048;

export const MAX_SECONDS = 179;
export const CONTEXT_SECONDS = 1;
export const ZOOMS = [10, 20, 40, 80, 160];
export const NBSP = String.fromCharCode(160);

export const SVG_NS = 'http://www.w3.org/2000/svg';
export const CHART_HEIGHT = 260;
export const CHART_TOP = 20;
export const CHART_BOTTOM = 28;
export const CHART_AXIS = 44;
export const NOTE_NAMES = ['C', 'C♯', 'D', 'D♯', 'E', 'F', 'F♯', 'G', 'G♯', 'A', 'A♯', 'B'];
export const LIVE_AXIS = 40;
export const NOTE_TOLERANCE = 0.7;
export const MIN_NOTE_SECONDS = 0.15;
export const NOTE_GAP_SECONDS = 0.25;
export const SARGAM = ['Sa', 'Ri♭', 'Ri', 'Ga♭', 'Ga', 'Ma', 'Ma♯', 'Pa', 'Da♭', 'Da', 'Ni♭', 'Ni'];
export const SCALE = [0, 2, 4, 5, 7, 9, 11];

export const FOLD_FRAMES = 10;
export const MAX_JUMP = 7;

export const LEAD_SECONDS = 0.5;
export const VOICE_RATE = 32000;
export const VOICE_CONTROL = 64;
export const VOICE_HARMONICS = 80;
export const SOURCE_TILT = 1.8;
export const FUNDAMENTAL_BOOST = 1.8;
export const BREATH_LEVEL = 0.12;
export const GLIDE_SECONDS = 0.12;
export const TRANSITION_SECONDS = 0.06;
export const VOICING_SECONDS = 0.03;
export const SCOOP = 0.3;
export const DRIFT = 0.01;
export const VIBRATO = { depth: 0.2, rate: 5.4, delay: 0.25, rise: 0.35 };
export const PHRASE_ATTACK = 0.08;
export const PHRASE_RELEASE = 0.15;
export const REVERB_MIX = 0.15;
export const VOWELS = {
  a: [[850, 90], [1220, 100], [2810, 140], [3800, 200], [4950, 250]],
  i: [[310, 60], [2790, 140], [3310, 180], [4000, 220], [4950, 250]],
};
export const CONSONANTS = {
  s: { closure: 0.1, voice: 0, hiss: 0.6, loci: [400, 1600, 2600] },
  r: { closure: 0.03, voice: 0.35, loci: [350, 1500, 2200] },
  g: { closure: 0.05, voice: 0.08, burst: 0.25, loci: [300, 1900, 2100] },
  m: { closure: 0.07, voice: 0.45, loci: [250, 1100, 2400] },
  p: { closure: 0.06, voice: 0, burst: 0.35, loci: [400, 900, 2300] },
  d: { closure: 0.05, voice: 0.08, burst: 0.3, loci: [350, 1700, 2600] },
  n: { closure: 0.07, voice: 0.45, loci: [250, 1600, 2600] },
};
