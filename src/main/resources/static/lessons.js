// The curriculum lives in lessons.json so teachers can edit it without touching code. Swaras are written in the shorthand
// of a Carnatic notebook: S R G M P D N, ' for the upper octave (tara sthayi), . for the lower octave (mandra sthayi),
// a comma to hold the previous swara for one more beat and | between lines.
const SWARAS = { S: ['Sa', 0], R: ['Ri', 2], G: ['Ga', 4], M: ['Ma', 5], P: ['Pa', 7], D: ['Da', 9], N: ['Ni', 11] };
const OCTAVES = { "'": ['̇', 12, '2'], '.': ['̣', -12, '0'], '': ['', 0, ''] };
const CONSONANT = { Sa: 's', Ri: 'r', Ga: 'g', Ma: 'm', Pa: 'p', Da: 'd', Ni: 'n' };
export const SYLLABLES = Object.fromEntries(Object.entries(CONSONANT).map(([name, c]) => [name, [c, name[1]]]));

function parse(text, where) {
  return text.split(/\s+/).filter((token) => token && token !== '|').reduce((steps, token) => {
    if (token === ',') {
      if (!steps.length) throw new Error(`${where}: a comma needs a swara before it.`);
      steps.at(-1).beats++;
      return steps;
    }
    if (!SWARAS[token[0]] || !OCTAVES[token.slice(1)]) throw new Error(`${where}: "${token}" is not a swara. Use S R G M P D N, with ' above or . below.`);
    const [name, degree] = SWARAS[token[0]], [dot, octave, id] = OCTAVES[token.slice(1)];
    steps.push({ swara: name[0] + dot + name.slice(1), note: degree + octave, syllable: SYLLABLES[name], id: name.toLowerCase() + id, beats: 1 });
    return steps;
  }, []);
}

const curriculum = await (await fetch(new URL('lessons.json', import.meta.url))).json();

export const STAGES = curriculum.stages;
export const CHAPTERS = STAGES.flatMap((stage) => stage.chapters.map((chapter) => Object.assign(chapter, { stage })));

export const LESSONS = CHAPTERS.flatMap((chapter) => chapter.lessons.map((lesson) => {
  const steps = parse(lesson.swaras, `${chapter.title}, ${lesson.title ?? lesson.swaras}`);
  return {
    id: lesson.title ? lesson.title.toLowerCase().replaceAll(/[^a-z0-9]+/g, '-') : steps.map((step) => step.id).join('-'),
    name: lesson.title ?? steps.map((step) => step.swara).join(' '),
    chapter,
    steps,
    seconds: chapter.secondsPerBeat,
    cycle: chapter.beatsPerLine,
  };
}));

const seen = new Set();
LESSONS.forEach((lesson) => {
  if (seen.has(lesson.id)) throw new Error(`${lesson.chapter.title}, ${lesson.name}: another lesson already has this name. Give one of them a title.`);
  seen.add(lesson.id);
});
