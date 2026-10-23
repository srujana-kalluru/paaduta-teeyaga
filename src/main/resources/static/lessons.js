export const SWARAS = { Sa: 0, Ri: 2, Ga: 4, Ma: 5, Pa: 7, Da: 9, Ni: 11, 'Ṡa': 12 };
export const SYLLABLES = { Sa: ['s', 'a'], Ri: ['r', 'i'], Ga: ['g', 'a'], Ma: ['m', 'a'], Pa: ['p', 'a'], Da: ['d', 'a'], Ni: ['n', 'i'], 'Ṡa': ['s', 'a'] };

export const CHAPTERS = [
  { title: 'One note', hint: 'Hold one note steady.', seconds: 3, lessons: ['Sa', 'Ri', 'Ga', 'Ma', 'Pa'] },
  { title: 'Two notes', hint: 'Step to the next note, then back.', seconds: 1, lessons: ['Sa Ri', 'Ri Sa', 'Sa Ri Sa', 'Ri Ga Ri', 'Ga Ma Ga', 'Ma Pa Ma'] },
  { title: 'Three notes', hint: 'Climb three notes and come back down.', seconds: 1, lessons: ['Sa Ri Ga', 'Ga Ri Sa', 'Ri Ga Ma', 'Ma Ga Ri', 'Ga Ma Pa', 'Pa Ma Ga'] },
  { title: 'Four and five notes', hint: 'Keep every note even as the phrase grows.', seconds: 1, lessons: ['Sa Ri Ga Ma', 'Ma Ga Ri Sa', 'Sa Ri Ga Ma Pa', 'Pa Ma Ga Ri Sa'] },
  { title: 'The full scale', hint: 'Reach the upper Sa and come back down.', seconds: 1, lessons: ['Pa Da Ni Ṡa', 'Ṡa Ni Da Pa', 'Sa Ri Ga Ma Pa Da Ni Ṡa', 'Ṡa Ni Da Pa Ma Ga Ri Sa'] },
  { title: 'Leaps', hint: 'Land on the note without sliding up to it.', seconds: 1, lessons: ['Sa Ga', 'Sa Pa', 'Sa Ṡa', 'Sa Ga Pa', 'Pa Ga Sa'] },
];

export const LESSONS = CHAPTERS.flatMap((chapter) => chapter.lessons.map((name) => ({
  id: name.replaceAll('Ṡa', 'Sa2').toLowerCase().replaceAll(' ', '-'),
  name,
  chapter,
  notes: name.split(' ').map((swara) => SWARAS[swara]),
  seconds: chapter.seconds,
})));
