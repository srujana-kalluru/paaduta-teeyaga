const RATE = 16000;
const MIN_HZ = 65;
const MAX_HZ = 1100;
const SILENCE_DB = -50;
const YIN_THRESHOLD = 0.2;
const ANALYSER_SIZE = 2048;

const midi = (hz) => 69 + 12 * Math.log2(hz / 440);

export async function decode(blob) {
  const buffer = await new OfflineAudioContext(1, 1, RATE).decodeAudioData(await blob.arrayBuffer());
  const mono = new Float32Array(buffer.length);
  for (let c = 0; c < buffer.numberOfChannels; c++) {
    buffer.getChannelData(c).forEach((v, i) => (mono[i] += v / buffer.numberOfChannels));
  }
  return mono;
}

export function wav(samples, rate = RATE) {
  const view = new DataView(new ArrayBuffer(44 + samples.length * 2));
  const text = (offset, s) => [...s].forEach((ch, i) => view.setUint8(offset + i, ch.charCodeAt(0)));
  text(0, 'RIFF');
  view.setUint32(4, 36 + samples.length * 2, true);
  text(8, 'WAVEfmt ');
  view.setUint32(16, 16, true);
  view.setUint16(20, 1, true);
  view.setUint16(22, 1, true);
  view.setUint32(24, rate, true);
  view.setUint32(28, rate * 2, true);
  view.setUint16(32, 2, true);
  view.setUint16(34, 16, true);
  text(36, 'data');
  view.setUint32(40, samples.length * 2, true);
  samples.forEach((v, i) => view.setInt16(44 + i * 2, Math.max(-1, Math.min(1, v)) * 0x7fff, true));
  return new Blob([view], { type: 'audio/wav' });
}

function livePitch(x, rate) {
  const tauMin = Math.floor(rate / MAX_HZ), tauMax = Math.floor(rate / MIN_HZ), w = x.length - tauMax;
  let energy = 0;
  for (let i = 0; i < w; i++) energy += x[i] * x[i];
  if (10 * Math.log10(energy / w + 1e-12) < SILENCE_DB) return null;
  const d = new Float32Array(tauMax + 1);
  let running = 0;
  for (let tau = 1; tau <= tauMax; tau++) {
    let diff = 0;
    for (let i = 0; i < w; i++) diff += (x[i] - x[i + tau]) ** 2;
    running += diff;
    d[tau] = running ? (diff * tau) / running : 1;
  }
  for (let tau = tauMin; tau <= tauMax; tau++) {
    if (d[tau] < YIN_THRESHOLD) {
      while (tau < tauMax && d[tau + 1] < d[tau]) tau++;
      return midi(rate / tau);
    }
  }
  return null;
}

export async function startRecording(onPitch) {
  const context = new AudioContext();
  let stream;
  try {
    stream = await navigator.mediaDevices.getUserMedia({ audio: { echoCancellation: false, noiseSuppression: false } });
  } catch (e) {
    context.close();
    throw e;
  }
  const recorder = new MediaRecorder(stream), chunks = [];
  const analyser = Object.assign(context.createAnalyser(), { fftSize: ANALYSER_SIZE });
  context.createMediaStreamSource(stream).connect(analyser);
  await context.resume();
  const step = Math.round(context.sampleRate / RATE), raw = new Float32Array(analyser.fftSize), frame = new Float32Array(Math.floor(raw.length / step));
  const started = performance.now();
  let raf;
  const tick = () => {
    analyser.getFloatTimeDomainData(raw);
    for (let i = 0; i < frame.length; i++) {
      let sum = 0;
      for (let k = 0; k < step; k++) sum += raw[i * step + k];
      frame[i] = sum / step;
    }
    onPitch((performance.now() - started) / 1000, livePitch(frame, context.sampleRate / step));
    raf = requestAnimationFrame(tick);
  };
  recorder.ondataavailable = (e) => chunks.push(e.data);
  recorder.start();
  tick();
  return () => new Promise((resolve) => {
    cancelAnimationFrame(raf);
    context.close();
    recorder.onstop = () => {
      stream.getTracks().forEach((t) => t.stop());
      resolve(new Blob(chunks, { type: recorder.mimeType }));
    };
    recorder.stop();
  });
}
