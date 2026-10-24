import { wav } from './audio.js';
import { noteName } from './chart.js';
import { LADDER, LEAD_SECONDS, LISTEN, SCALE, SVG_NS, VOICE_RATE } from './constants.js';
import { sing } from './voice.js';

const $ = (selector) => document.querySelector(selector);
const $$ = (selector) => [...document.querySelectorAll(selector)];
const pick = (items) => items[Math.floor(Math.random() * items.length)];
const node = (name, attrs, parent) => {
  const el = document.createElementNS(SVG_NS, name);
  Object.entries(attrs).forEach(([k, v]) => el.setAttribute(k, v));
  parent.append(el);
  return el;
};
const HUM = ['m', 'a'];
const ANSWERS = [['up', 'Up ↑'], ['down', 'Down ↓'], ['same', 'Same =']];

let quiz, audio, frame;

// Two scale notes a chosen distance apart, so every answer can be named in sargam.
function pair(tonic, intervals, same) {
  const notes = Array.from({ length: LISTEN.high - LISTEN.low + 1 }, (_, k) => tonic + LISTEN.low + k).filter((m) => SCALE.includes((m - tonic + 12) % 12));
  const size = same && Math.random() < 1 / 3 ? 0 : pick(intervals), direction = pick([1, -1]);
  const first = pick(notes.filter((m) => notes.includes(m + direction * size)));
  return [first, first + direction * size];
}

// Both notes are sung separately and joined with a short gap, so only their height differs.
function clip([first, second]) {
  const one = (m) => sing([{ note: m, syllable: HUM, beats: 1 }], LISTEN.seconds).wet, keep = Math.round((LEAD_SECONDS + LISTEN.seconds + LISTEN.gap) * VOICE_RATE);
  const a = one(first).slice(0, keep), b = one(second), out = new Float32Array(a.length + b.length);
  out.set(a);
  out.set(b, a.length);
  return { url: URL.createObjectURL(wav(out, VOICE_RATE)), starts: [LEAD_SECONDS, keep / VOICE_RATE + LEAD_SECONDS] };
}

// A ladder of the scale with the two notes as dots: higher on the screen is higher in pitch.
function drawLadder(shown) {
  const svg = $('#ladder'), { tonic, notes } = quiz, top = tonic + LISTEN.high, step = (LADDER.height - 2 * LADDER.pad) / (LISTEN.high - LISTEN.low);
  const y = (m) => LADDER.pad + (top - m) * step;
  svg.replaceChildren();
  svg.setAttribute('viewBox', `0 0 ${LADDER.width} ${LADDER.height}`);
  for (let m = tonic + LISTEN.low; m <= top; m++) {
    if (!SCALE.includes((m - tonic + 12) % 12)) continue;
    node('line', { x1: LADDER.axis, x2: LADDER.width - 8, y1: y(m), y2: y(m), class: m === tonic ? 'rung home' : 'rung' }, svg);
    node('text', { x: LADDER.axis - 8, y: y(m) + 4, 'text-anchor': 'end' }, svg).textContent = noteName(m, tonic);
  }
  const xs = [LADDER.width * 0.4, LADDER.width * 0.75];
  if (shown > 1) {
    const move = Math.sign(notes[1] - notes[0]), cls = move > 0 ? 'up' : move < 0 ? 'down' : 'same';
    node('line', { x1: xs[0], y1: y(notes[0]), x2: xs[1], y2: y(notes[1]), class: `path ${cls}` }, svg);
  }
  notes.slice(0, shown).forEach((m, i) => {
    node('circle', { cx: xs[i], cy: y(m), r: 11, class: `dot n${i}` }, svg);
    node('text', { x: xs[i] + 18, y: y(m) + 5, class: 'dot-label' }, svg).textContent = noteName(m, quiz.tonic);
  });
}

function stopSound() {
  cancelAnimationFrame(frame);
  audio?.pause();
}

function play() {
  stopSound();
  quiz.shown = -1;
  if (!quiz.answered) $('#listen-feedback').textContent = '';
  audio = new Audio(quiz.clip.url);
  audio.play().catch(() => {});
  const follow = () => {
    // In a demonstration, or once answered, each dot appears as its note sounds. Before an answer nothing is shown.
    const shown = quiz.clip.starts.filter((t) => audio.currentTime >= t).length;
    if ((quiz.lesson.listen.demo || quiz.answered) && shown !== quiz.shown) drawLadder((quiz.shown = shown));
    if (!audio.paused && !audio.ended) frame = requestAnimationFrame(follow);
    else if (quiz.lesson.listen.demo && !quiz.answered) reveal();
    else if (!quiz.answered) setAnswering(true);
  };
  frame = requestAnimationFrame(follow);
  $('#listen-play').textContent = 'Hear again';
}

function setAnswering(on) {
  $$('#listen-answers button').forEach((b) => (b.disabled = !on || quiz.answered));
}

const direction = ([first, second]) => ({ 1: 'up', [-1]: 'down', 0: 'same' })[Math.sign(second - first)];
const SAID = { up: 'went up', down: 'went down', same: 'stayed the same' };

function showNext() {
  $('#listen-next').textContent = quiz.round < quiz.rounds ? 'Next' : 'Finish';
  $('#listen-next').hidden = false;
}

// A demonstration asks nothing: it names the direction once both notes have played.
function reveal() {
  quiz.answered = true;
  Object.assign($('#listen-feedback'), { textContent: `It ${SAID[direction(quiz.notes)]}.`, className: 'good' });
  showNext();
}

function newRound() {
  quiz.round++;
  quiz.answered = false;
  quiz.notes = pair(quiz.tonic, quiz.lesson.listen.intervals, quiz.lesson.listen.same);
  quiz.clip = clip(quiz.notes);
  const { demo, same } = quiz.lesson.listen;
  $('#listen-round').textContent = demo ? `Round ${quiz.round} of ${quiz.rounds}` : `Round ${quiz.round} of ${quiz.rounds} · ${quiz.correct} right so far`;
  $('#listen-question').textContent = demo ? 'Listen and watch where the second note lands.' : same ? 'Did the second note go up, go down or stay the same?' : 'Did the second note go up or down?';
  $('#listen-feedback').textContent = quiz.round === 1 ? 'Press Play and listen to both notes.' : '';
  $('#listen-feedback').className = '';
  $('#listen-play').textContent = 'Play';
  $('#listen-play').hidden = false;
  $('#ladder').removeAttribute('hidden');
  $('#listen-next').hidden = true;
  $('#listen-answers').hidden = Boolean(demo);
  setAnswering(false);
  drawLadder(0);
  if (quiz.round > 1) play();
}

function answer(choice) {
  if (quiz.answered) return;
  quiz.answered = true;
  const truth = direction(quiz.notes), right = choice === truth, said = SAID[truth];
  if (right) quiz.correct++;
  Object.assign($('#listen-feedback'), { textContent: right ? `Right, it ${said}.` : `Not quite: it ${said}. Hear it again while you watch.`, className: right ? 'good' : 'miss' });
  $('#listen-round').textContent = `Round ${quiz.round} of ${quiz.rounds} · ${quiz.correct} right so far`;
  setAnswering(false);
  drawLadder(2);
  showNext();
}

function finish() {
  stopSound();
  const { demo } = quiz.lesson.listen, passed = demo || quiz.correct >= Math.ceil(LISTEN.pass * quiz.rounds);
  quiz.done(passed, demo ? 0 : quiz.rounds - quiz.correct);
  $('#listen-round').textContent = demo ? `${quiz.rounds} pairs heard` : `${quiz.correct} of ${quiz.rounds} right`;
  $('#listen-question').textContent = passed ? 'Lesson passed' : 'Nearly there';
  Object.assign($('#listen-feedback'), {
    textContent: demo ? 'Next, you will hear the notes first and answer before the picture appears.'
      : passed ? 'Your ear is learning to hear the direction of a note.' : `Get ${Math.ceil(LISTEN.pass * quiz.rounds)} right to pass. Each try trains your ear a little more.`,
    className: passed ? 'good' : '',
  });
  $('#listen-answers').hidden = true;
  $('#listen-next').hidden = true;
  $('#ladder').setAttribute('hidden', '');
  $('#listen-play').hidden = true;
  $('#listen-end').hidden = false;
  $('#listen-continue').hidden = !(passed && quiz.hasNext());
  $('#listen-skip').hidden = passed || !quiz.hasNext();
}

export function stopListen() {
  stopSound();
}

export function startListen(lesson, tonic, { autoplay, done, next, hasNext }) {
  stopSound();
  quiz = { lesson, tonic, done, hasNext, rounds: lesson.listen.rounds ?? LISTEN.rounds, round: 0, correct: 0 };
  $('#listen .listen-title').textContent = lesson.name;
  Object.assign($('#listen .theory'), { hidden: !lesson.theory });
  $('#listen .theory p:last-child').textContent = lesson.theory ?? '';
  $('#listen-answers').replaceChildren(...ANSWERS.filter(([key]) => key !== 'same' || lesson.listen.same)
    .map(([key, label]) => Object.assign(document.createElement('button'), { className: 'secondary', textContent: label, onclick: () => answer(key) })));
  $('#listen-play').onclick = play;
  $('#listen-next').onclick = () => (quiz.round < quiz.rounds ? newRound() : finish());
  $('#listen-again').onclick = () => startListen(lesson, tonic, { done, next, hasNext });
  $('#listen-continue').onclick = next;
  $('#listen-end').hidden = true;
  $('#listen-skip').hidden = !hasNext();
  newRound();
  if (autoplay) play();
}
