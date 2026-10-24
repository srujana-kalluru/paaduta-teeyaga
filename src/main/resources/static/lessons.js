// Lessons are written in the shorthand of a Carnatic notebook: S R G M P D N, ' for the upper octave (tara sthayi),
// . for the lower octave (mandra sthayi) and a comma to hold the previous swara for one more beat.
const SWARAS = { S: ['Sa', 0], R: ['Ri', 2], G: ['Ga', 4], M: ['Ma', 5], P: ['Pa', 7], D: ['Da', 9], N: ['Ni', 11] };
const OCTAVES = { "'": ['̇', 12, '2'], '.': ['̣', -12, '0'], '': ['', 0, ''] };
const CONSONANT = { Sa: 's', Ri: 'r', Ga: 'g', Ma: 'm', Pa: 'p', Da: 'd', Ni: 'n' };
export const SYLLABLES = Object.fromEntries(Object.entries(CONSONANT).map(([name, c]) => [name, [c, name[1]]]));

function parse(text) {
  return text.split(/\s+/).filter((token) => token && token !== '|').reduce((steps, token) => {
    if (token === ',') {
      steps.at(-1).beats++;
      return steps;
    }
    const [name, degree] = SWARAS[token[0]], [dot, octave, id] = OCTAVES[token.slice(1)];
    steps.push({ swara: name[0] + dot + name.slice(1), note: degree + octave, syllable: SYLLABLES[name], id: name.toLowerCase() + id, beats: 1 });
    return steps;
  }, []);
}

export const CHAPTERS = [
  { title: 'One note', hint: 'Hold one note steady for three beats.', seconds: 1, lessons: ['S , ,', 'R , ,', 'G , ,', 'M , ,', 'P , ,'] },
  { title: 'Two notes', hint: 'Step to the next note, then back.', seconds: 1, lessons: ['S R', 'R S', 'S R S', 'R G R', 'G M G', 'M P M'] },
  { title: 'Three notes', hint: 'Climb three notes and come back down.', seconds: 1, lessons: ['S R G', 'G R S', 'R G M', 'M G R', 'G M P', 'P M G'] },
  { title: 'Four and five notes', hint: 'Keep every note even as the phrase grows.', seconds: 1, lessons: ['S R G M', 'M G R S', 'S R G M P', 'P M G R S'] },
  { title: 'The full scale', hint: 'Reach the upper Sa and come back down.', seconds: 1, lessons: ["P D N S'", "S' N D P", "S R G M P D N S'", "S' N D P M G R S"] },
  { title: 'Leaps', hint: 'Land on the note without sliding up to it.', seconds: 1, lessons: ['S G', 'S P', "S S'", 'S G P', 'P G S'] },
  {
    title: 'Sarali Varisai I',
    hint: 'The first exercises a Carnatic teacher gives, in Adi tala: eight beats to a line. Slow and even before anything else.',
    seconds: 1,
    lessons: [
      ['Sarali Varisai 1', "S R G M P D N S' | S' N D P M G R S"],
      ['Sarali Varisai 2', "S R S R S R G M | S R G M P D N S' | S' N S' N S' N D P | S' N D P M G R S"],
      ['Sarali Varisai 3', "S R G S R G S R | S R G M P D N S' | S' N D S' N D S' N | S' N D P M G R S"],
      ['Sarali Varisai 4', "S R G M S R G M | S R G M P D N S' | S' N D P S' N D P | S' N D P M G R S"],
    ],
  },
  {
    title: 'Sarali Varisai II',
    hint: 'Turns back on the scale and the first held swaras. A comma holds the swara one more beat.',
    seconds: 0.8,
    lessons: [
      ['Sarali Varisai 5', "S R G M P , S R | S R G M P D N S' | S' N D P M , S' N | S' N D P M G R S"],
      ['Sarali Varisai 6', "S R G M P D S R | S R G M P D N S' | S' N D P M G S' N | S' N D P M G R S"],
      ['Sarali Varisai 7', "S R G M P D N D | S R G M P D N S' | S' N D P M G R G | S' N D P M G R S"],
      ['Sarali Varisai 8', "S R G M P M G R | S R G M P D N S' | S' N D P M P D N | S' N D P M G R S"],
      ['Sarali Varisai 9', "S R G M P M D P | S R G M P D N S' | S' N D P M P G M | S' N D P M G R S"],
    ],
  },
  {
    title: 'Sarali Varisai III',
    hint: 'Long holds and phrases that start from the upper Sa. Keep the pitch still through every comma.',
    seconds: 0.7,
    lessons: [
      ['Sarali Varisai 10', "S R G M P , G M | P , , , P , , , | G M P D N D P M | G M P G M G R S"],
      ['Sarali Varisai 11', "S' , N D N , D P | D , P M P , P , | G M P D N D P M | G M P G M G R S"],
      ['Sarali Varisai 12', "S' S' N D N N D P | D D P M P , P , | G M P D N D P M | G M P G M G R S"],
      ['Sarali Varisai 13', "S R G R G , G M | P M P , D P D , | M P D P D N D P | M P D P M G R S"],
      ['Sarali Varisai 14', "S R G M P , P , | D D P , M M P , | D N S' , S' N D P | S' N D P M G R S"],
    ],
  },
  {
    title: 'Janta Varisai',
    hint: 'Sing every swara twice and give the second one its own fresh stress.',
    seconds: 0.6,
    lessons: [
      ['Janta Varisai 1', "S S R R G G M M | P P D D N N S' S' | S' S' N N D D P P | M M G G R R S S"],
      ['Janta Varisai 2', "S S R R G G R R | S S R R G G M M | R R G G M M G G | R R G G M M P P | G G M M P P M M | G G M M P P D D | "
        + "M M P P D D P P | M M P P D D N N | P P D D N N D D | P P D D N N S' S' | S' S' N N D D N N | S' S' N N D D P P | "
        + "N N D D P P D D | N N D D P P M M | D D P P M M P P | D D P P M M G G | P P M M G G M M | P P M M G G R R | "
        + "M M G G R R G G | M M G G R R S S"],
    ],
  },
  {
    title: 'Mandra and Tara Sthayi',
    hint: 'Step below the lower Sa and above the upper Sa. Keep the tone the same in every octave.',
    seconds: 1,
    lessons: ['S N.', 'N. D. N. S', 'S N. D. P.', 'P. D. N. S R G', "S' R' S'", "N S' R' S' N", "P D N S' R' S' N D P"],
  },
  {
    title: 'Alankaram',
    hint: 'Patterns set to a tala. Triputa tala has seven beats: three, then two, then two.',
    seconds: 0.7,
    cycle: 7,
    lessons: [
      ['Triputa Tala Alankaram', "S R G S R G M | R G M R G M P | G M P G M P D | M P D M P D N | P D N P D N S' | "
        + "S' N D S' N D P | N D P N D P M | D P M D P M G | P M G P M G R | M G R M G R S"],
    ],
  },
  {
    title: 'Second speed',
    hint: 'The same varisais at twice the pace, madhyama kalam. Speed only once the slow version passes cleanly.',
    seconds: 0.5,
    lessons: [
      ['Sarali Varisai 1 - second speed', "S R G M P D N S' | S' N D P M G R S"],
      ['Sarali Varisai 3 - second speed', "S R G S R G S R | S R G M P D N S' | S' N D S' N D S' N | S' N D P M G R S"],
      ['Janta Varisai 1 - second speed', "S S R R G G M M | P P D D N N S' S' | S' S' N N D D P P | M M G G R R S S"],
    ],
  },
];

export const LESSONS = CHAPTERS.flatMap((chapter) => chapter.lessons.map((lesson) => {
  const [title, text] = Array.isArray(lesson) ? lesson : [null, lesson], steps = parse(text);
  const name = title ?? steps.map((step) => step.swara).join(' ');
  return {
    id: title ? title.toLowerCase().replaceAll(/[^a-z0-9]+/g, '-') : steps.map((step) => step.id).join('-'),
    name,
    chapter,
    steps,
    seconds: chapter.seconds,
    cycle: chapter.cycle,
  };
}));
