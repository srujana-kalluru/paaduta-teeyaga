import { CHART_AXIS, CHART_BOTTOM, CHART_HEIGHT, CHART_TOP, NOTE_NAMES, SVG_NS } from './constants.js';

const node = (name, attrs, parent) => {
  const el = document.createElementNS(SVG_NS, name);
  Object.entries(attrs).forEach(([k, v]) => el.setAttribute(k, v));
  parent.append(el);
  return el;
};
const midi = (hz) => (hz == null ? null : 69 + 12 * Math.log2(hz / 440));
const runs = (flags) => flags.reduce((out, on, i) => {
  if (on && !flags[i - 1]) out.push([i, i + 1]);
  else if (on) out.at(-1)[1] = i + 1;
  return out;
}, []);

export function drawChart(host, result, { pxPerSecond, onSelect }) {
  const c = result.contours, times = c.time_seconds, shift = result.key_shift_semitones;
  const ref = c.reference_f0_hz.map(midi), you = c.user_f0_hz.map((hz) => (hz == null ? null : midi(hz) - shift));
  const values = [...ref, ...you].filter((v) => v != null);
  host.replaceChildren();
  if (!values.length) {
    host.textContent = 'There is no clear pitch to draw.';
    return { playhead() {} };
  }

  const lo = Math.floor(Math.min(...values)) - 2, hi = Math.ceil(Math.max(...values)) + 2;
  const duration = result.duration_seconds, hop = times[1] - times[0] || 0.02, plotHeight = CHART_HEIGHT - CHART_TOP - CHART_BOTTOM;
  const width = Math.max(host.clientWidth - CHART_AXIS, duration * pxPerSecond);
  const x = (t) => (t / duration) * width;
  const y = (m) => CHART_TOP + ((hi - m) / (hi - lo)) * plotHeight;

  const axis = node('svg', { width: CHART_AXIS, height: CHART_HEIGHT, class: 'axis', 'aria-hidden': 'true' }, host);
  const scroller = Object.assign(document.createElement('div'), { className: 'scroller' });
  host.append(scroller);
  const svg = node('svg', { width, height: CHART_HEIGHT, class: 'plot', role: 'img', 'aria-label': 'Pitch of the reference and the attempt over time. Flagged moments are listed below the chart.' }, scroller);

  const step = [1, 2, 3, 4, 6, 12].find((s) => (s * plotHeight) / (hi - lo) >= 18) ?? 12;
  for (let m = lo; m <= hi; m++) {
    if (m % step) continue;
    node('line', { x1: 0, x2: width, y1: y(m), y2: y(m), class: 'grid' }, svg);
    node('text', { x: CHART_AXIS - 8, y: y(m) + 4, 'text-anchor': 'end' }, axis).textContent = NOTE_NAMES[m % 12] + (Math.floor(m / 12) - 1);
  }
  const tick = [0.25, 0.5, 1, 2, 5, 10].find((t) => (t * width) / duration >= 50) ?? 10;
  for (let k = 0; x(k * tick) < width - 32; k++) node('text', { x: x(k * tick) + 3, y: CHART_HEIGHT - 8 }, svg).textContent = `${+(k * tick).toFixed(2)}s`;

  runs(c.deviation_cents.map((d, i) => d == null && (ref[i] != null || you[i] != null))).forEach(([a, b]) =>
    node('rect', { x: x(times[a]), width: x((b - a) * hop), y: CHART_TOP, height: plotHeight, class: 'gap' }, svg));

  [[ref, 'ref'], [you, 'you']].forEach(([series, cls]) => {
    const d = series.map((v, i) => {
      if (v == null) return '';
      const command = series[i - 1] == null ? 'M' : 'L';
      return `${command}${x(times[i]).toFixed(1)} ${y(v).toFixed(1)}`;
    }).join('');
    node('path', { d, class: cls }, svg);
  });

  result.issues.forEach((issue, k) => {
    const band = node('g', { class: 'flag', tabindex: 0, role: 'button', 'aria-label': `Moment ${k + 1}, ${issue.start_seconds} seconds` }, svg);
    node('rect', { x: x(issue.start_seconds), width: Math.max(4, x(issue.end_seconds - issue.start_seconds)), y: CHART_TOP, height: plotHeight }, band);
    const centre = x((issue.start_seconds + issue.end_seconds) / 2);
    node('circle', { cx: centre, cy: CHART_TOP - 8, r: 9 }, band);
    node('text', { x: centre, y: CHART_TOP - 4, 'text-anchor': 'middle' }, band).textContent = k + 1;
    const select = (e) => {
      e.preventDefault();
      e.stopPropagation();
      onSelect(issue.start_seconds);
    };
    band.addEventListener('click', select);
    band.addEventListener('keydown', (e) => (e.key === 'Enter' || e.key === ' ') && select(e));
  });

  const head = node('line', { y1: CHART_TOP, y2: CHART_HEIGHT - CHART_BOTTOM, class: 'head', visibility: 'hidden' }, svg);
  svg.addEventListener('click', (e) => onSelect(((e.clientX - svg.getBoundingClientRect().left) / width) * duration));

  return {
    playhead(t) {
      const px = x(t);
      head.setAttribute('x1', px);
      head.setAttribute('x2', px);
      head.setAttribute('visibility', 'visible');
      if (px < scroller.scrollLeft || px > scroller.scrollLeft + scroller.clientWidth - 24) scroller.scrollLeft = px - 40;
    },
  };
}

export function drawLive(canvas, series) {
  const ratio = devicePixelRatio || 1, width = canvas.clientWidth, height = canvas.clientHeight;
  canvas.width = width * ratio;
  canvas.height = height * ratio;
  const g = canvas.getContext('2d');
  g.scale(ratio, ratio);
  const voiced = series.flatMap(({ points, shift }) => points.filter(([, m]) => m != null).map(([t, m]) => [t + shift, m]));
  if (!voiced.length) return;
  const end = Math.max(4, ...voiced.map(([t]) => t + 0.5)), notes = voiced.map(([, m]) => m);
  const lo = Math.min(...notes) - 2, hi = Math.max(...notes) + 2;
  series.forEach(({ points, shift, color, width: stroke }) => {
    g.strokeStyle = color;
    g.lineWidth = stroke;
    g.lineJoin = g.lineCap = 'round';
    g.beginPath();
    points.forEach(([t, m], i) => {
      if (m == null) return;
      const px = ((t + shift) / end) * width, py = ((hi - m) / (hi - lo)) * height;
      points[i - 1]?.[1] == null ? g.moveTo(px, py) : g.lineTo(px, py);
    });
    g.stroke();
  });
}
