import { CLEFS, CYCLE_BEATS, LETTERS, NOTE_NAMES, STAFF_BEAT, STAFF_CLEF, STAFF_MARGIN, STAFF_SPACE, SVG_NS, TREBLE_FROM } from './constants.js';

const node = (name, attrs, parent) => {
  const el = document.createElementNS(SVG_NS, name);
  Object.entries(attrs).forEach(([k, v]) => el.setAttribute(k, v));
  parent.append(el);
  return el;
};

export const westernName = (m) => NOTE_NAMES[m % 12] + (Math.floor(m / 12) - 1);
// Staff position counted in lines and spaces from C-1, spelling black keys as sharps.
const position = (m) => 7 * (Math.floor(m / 12) - 1) + LETTERS[m % 12];
const sharp = (m) => NOTE_NAMES[m % 12].length > 1;

function drawNote(g, m, beats, x, clef, half) {
  const step = position(m) - clef.bottom, y = -step * half, open = beats >= 2;
  for (let line = step <= -2 ? -2 : 10; step <= -2 ? line >= step : line <= step; line += step <= -2 ? -2 : 2) {
    node('line', { x1: x - 10, x2: x + 10, y1: -line * half, y2: -line * half, class: 'ledger' }, g);
  }
  if (sharp(m)) node('text', { x: x - 11, y: y + 4, 'text-anchor': 'end', class: 'accidental' }, g).textContent = '♯';
  node('ellipse', { cx: x, cy: y, rx: 5.6, ry: 4, transform: `rotate(-20 ${x} ${y})`, class: open ? 'head open' : 'head' }, g);
  if (beats < 4) {
    const up = step < 4, sx = up ? x + 5 : x - 5;
    node('line', { x1: sx, x2: sx, y1: y, y2: y + (up ? -7 : 7) * half, class: 'stem' }, g);
  }
  if (beats === 3) node('circle', { cx: x + 10, cy: y - (step % 2 ? 0 : half), r: 1.6, class: 'dot' }, g);
}

// Western staff notation for a lesson, one line per tala cycle, with one beat played every `seconds`.
export function drawStaff(svg, steps, seconds, tonic, cycle = CYCLE_BEATS) {
  const notes = steps.map((s) => tonic + s.note), clef = CLEFS[notes.reduce((a, b) => a + b, 0) / notes.length >= TREBLE_FROM ? 'treble' : 'bass'];
  const half = STAFF_SPACE / 2, total = steps.reduce((sum, s) => sum + s.beats, 0), perLine = Math.min(cycle, total);
  const lines = Math.ceil(total / perLine), height = (4 + 2 * STAFF_MARGIN) * STAFF_SPACE, width = STAFF_CLEF + perLine * STAFF_BEAT + 8;
  svg.replaceChildren();
  Object.entries({ viewBox: `0 0 ${width} ${lines * height + 14}`, width, height: lines * height + 14 }).forEach(([k, v]) => svg.setAttribute(k, v));
  svg.setAttribute('aria-label', `Staff notation at ${Math.round(60 / seconds)} beats a minute: ${notes.map(westernName).join(', ')}`);
  node('text', { x: 4, y: 12, class: 'tempo' }, svg).textContent = `♩ = ${Math.round(60 / seconds)}`;
  const systems = Array.from({ length: lines }, (_, k) => {
    const g = node('g', { transform: `translate(0 ${14 + k * height + (STAFF_MARGIN + 4) * STAFF_SPACE})` }, svg);
    for (let line = 0; line < 5; line++) node('line', { x1: 0, x2: width - 1, y1: -line * STAFF_SPACE, y2: -line * STAFF_SPACE, class: 'line' }, g);
    node('line', { x1: width - 1, x2: width - 1, y1: 0, y2: -4 * STAFF_SPACE, class: 'bar' }, g);
    node('text', { x: 4, y: clef.baseline * STAFF_SPACE, 'font-size': clef.size * STAFF_SPACE, class: 'clef' }, g).textContent = clef.glyph;
    return g;
  });
  let beat = 0;
  return steps.map((s, i) => {
    const line = Math.min(lines - 1, Math.floor(beat / perLine)), g = node('g', { class: 'staff-note' }, systems[line]);
    drawNote(g, notes[i], s.beats, STAFF_CLEF + (beat - line * perLine + 0.5) * STAFF_BEAT, clef, half);
    beat += s.beats;
    return g;
  });
}
