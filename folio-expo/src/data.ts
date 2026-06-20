// data.ts — library, stats, sample chapter. Ported from the prototype's
// data.jsx; oklch cover gradients converted to sRGB hex stops.

const INK = '#efe7d8'; // cream foreground for dark covers
const DARKINK = '#2c2620'; // ink foreground for light covers

export type BookStatus = 'reading' | 'finished' | 'want';

export interface Book {
  id: string;
  title: string;
  author: string;
  genre: string;
  year: number;
  pages: number;
  cover: [string, string]; // gradient stops
  fg: string;
  titleSize: number;
  rating: number;
  status: BookStatus;
  page?: number;
  started?: string;
  finished?: string;
  minPerPage?: number;
  desc: string;
}

export const BOOKS: Book[] = [
  { id: 'pp', title: 'Pride and\nPrejudice', author: 'Jane Austen', genre: 'Classics', year: 1813, pages: 432, cover: ['#774B60', '#603A4C'], fg: INK, titleSize: 1.5, rating: 5, status: 'reading', page: 142, started: 'May 28', minPerPage: 0.7, desc: 'A sparkling comedy of manners in which Elizabeth Bennet matches wits with the proud Mr. Darcy across the drawing rooms of Regency England.' },
  { id: 'md', title: 'Moby-\nDick', author: 'Herman Melville', genre: 'Adventure', year: 1851, pages: 625, cover: ['#223A51', '#172839'], fg: INK, titleSize: 1.75, rating: 0, status: 'reading', page: 74, started: 'Jun 9', minPerPage: 0.8, desc: 'Ishmael ships aboard the Pequod under the monomaniacal Captain Ahab, bound for a reckoning with the white whale.' },
  { id: 'od', title: 'The\nOdyssey', author: 'Homer', genre: 'Classics', year: -700, pages: 384, cover: ['#1F6CB0', '#0E518F'], fg: INK, titleSize: 1.7, rating: 0, status: 'reading', page: 223, started: 'Apr 2', minPerPage: 0.7, desc: 'The long voyage home of Odysseus — storms, sirens, and the patience of a kingdom waiting at Ithaca.' },
  { id: 'ge', title: 'The Great\nGatsby', author: 'F. Scott Fitzgerald', genre: 'Literary', year: 1925, pages: 218, cover: ['#2A566B', '#204154'], fg: '#e9c97a', titleSize: 1.5, rating: 5, status: 'finished', finished: 'Jun 2', started: 'May 24', minPerPage: 0.6, desc: 'A green light, a careless crowd, and Jay Gatsby reaching for a past that will not return.' },
  { id: 'fr', title: 'Franken-\nstein', author: 'Mary Shelley', genre: 'Gothic', year: 1818, pages: 280, cover: ['#416050', '#2D493D'], fg: INK, titleSize: 1.55, rating: 4, status: 'finished', finished: 'May 18', started: 'May 6', minPerPage: 0.7, desc: 'Victor Frankenstein animates a creature and learns the cost of creation without care.' },
  { id: 'dg', title: 'The Picture\nof Dorian\nGray', author: 'Oscar Wilde', genre: 'Gothic', year: 1890, pages: 254, cover: ['#245F49', '#164737'], fg: '#e7d8a8', titleSize: 1.25, rating: 5, status: 'finished', finished: 'Apr 29', started: 'Apr 18', minPerPage: 0.6, desc: 'Beauty preserved on a canvas while the soul behind it decays.' },
  { id: 'je', title: 'Jane\nEyre', author: 'Charlotte Brontë', genre: 'Classics', year: 1847, pages: 532, cover: ['#9F5E45', '#7E4835'], fg: INK, titleSize: 1.7, rating: 5, status: 'finished', finished: 'Apr 11', started: 'Mar 22', minPerPage: 0.7, desc: 'A governess of fierce conscience finds love and reckoning at Thornfield Hall.' },
  { id: 'cp', title: 'Crime and\nPunishment', author: 'Fyodor Dostoevsky', genre: 'Literary', year: 1866, pages: 671, cover: ['#753934', '#542523'], fg: INK, titleSize: 1.4, rating: 4, status: 'finished', finished: 'Mar 14', started: 'Feb 19', minPerPage: 0.8, desc: 'Raskolnikov commits the perfect crime and spends the rest of the novel undone by it.' },
  { id: 'mw', title: 'Mrs\nDalloway', author: 'Virginia Woolf', genre: 'Literary', year: 1925, pages: 194, cover: ['#DAB7B5', '#C6A0A0'], fg: DARKINK, titleSize: 1.55, rating: 4, status: 'finished', finished: 'Feb 8', started: 'Feb 1', minPerPage: 0.7, desc: 'A single June day in London, told in the turning thoughts of Clarissa Dalloway.' },
  { id: 'mm', title: 'Middle-\nmarch', author: 'George Eliot', genre: 'Classics', year: 1871, pages: 880, cover: ['#7E9071', '#67785D'], fg: DARKINK, titleSize: 1.55, rating: 0, status: 'want', desc: 'A panoramic study of provincial life and the quiet heroism of ordinary ambition.' },
  { id: 'dr', title: 'Dracula', author: 'Bram Stoker', genre: 'Gothic', year: 1897, pages: 418, cover: ['#201818', '#0E0A0A'], fg: '#d8534a', titleSize: 1.85, rating: 0, status: 'want', desc: 'A count, a coffin, and the long pursuit across Europe to undo him.' },
  { id: 'gx', title: 'Great\nExpectations', author: 'Charles Dickens', genre: 'Classics', year: 1861, pages: 544, cover: ['#BC9357', '#A37B47'], fg: DARKINK, titleSize: 1.35, rating: 0, status: 'want', desc: 'Pip rises from the forge toward a fortune whose source he cannot guess.' },
  { id: 'wh', title: 'Wuthering\nHeights', author: 'Emily Brontë', genre: 'Gothic', year: 1847, pages: 416, cover: ['#534959', '#3D3441'], fg: INK, titleSize: 1.45, rating: 0, status: 'want', desc: 'Love and vengeance on the Yorkshire moors, told through generations.' },
  { id: 'sr', title: 'The Sun\nAlso Rises', author: 'Ernest Hemingway', genre: 'Literary', year: 1926, pages: 251, cover: ['#CBB897', '#B59E7B'], fg: DARKINK, titleSize: 1.45, rating: 0, status: 'want', desc: 'The lost generation drifts from Paris cafés to the bullrings of Pamplona.' },
];

export const byId = (id: string) => BOOKS.find((b) => b.id === id)!;
export const reading = BOOKS.filter((b) => b.status === 'reading');
export const finished = BOOKS.filter((b) => b.status === 'finished');
export const want = BOOKS.filter((b) => b.status === 'want');

// deterministic heatmap (18 weeks)
const HEAT = (() => {
  const days: number[] = [];
  let s = 7;
  for (let i = 0; i < 18 * 7; i++) {
    s = (s * 9301 + 49297) % 233280;
    const r = s / 233280;
    let v = r < 0.18 ? 0 : r < 0.4 ? 1 : r < 0.66 ? 2 : r < 0.86 ? 3 : 4;
    if (i > 18 * 7 - 25 && r < 0.3) v = Math.max(v, 2);
    days.push(v);
  }
  return days;
})();

export const STATS = {
  year: 2026,
  goal: 36,
  booksRead: 24,
  streak: 24,
  longestStreak: 58,
  hoursYear: 312,
  hoursThisWeek: 7.1,
  pagesYear: 9420,
  wordsYear: '2.6M',
  avgPace: 38,
  avgSession: 32,
  weekMinutes: [42, 65, 30, 80, 55, 95, 48],
  weekDays: ['M', 'T', 'W', 'T', 'F', 'S', 'S'],
  monthly: [2, 1, 3, 2, 4, 3, 2, 3, 1, 2, 1, 0],
  monthLabels: ['J', 'F', 'M', 'A', 'M', 'J', 'J', 'A', 'S', 'O', 'N', 'D'],
  genres: [
    { name: 'Classics', pct: 42 },
    { name: 'Literary', pct: 24 },
    { name: 'Gothic', pct: 18 },
    { name: 'Adventure', pct: 10 },
    { name: 'Poetry', pct: 6 },
  ],
  topAuthors: [
    { name: 'Charlotte Brontë', books: 3 },
    { name: 'F. Scott Fitzgerald', books: 2 },
    { name: 'Virginia Woolf', books: 2 },
  ],
  heat: HEAT,
};

export interface Highlight {
  id: number;
  color: 'y' | 'b';
  book: string;
  author: string;
  text: string;
}

export const HIGHLIGHTS: Highlight[] = [
  { id: 1, color: 'y', book: 'The Great Gatsby', author: 'Fitzgerald', text: 'So we beat on, boats against the current, borne back ceaselessly into the past.' },
  { id: 2, color: 'b', book: 'Jane Eyre', author: 'C. Brontë', text: 'I am no bird; and no net ensnares me: I am a free human being with an independent will.' },
  { id: 3, color: 'y', book: 'Pride and Prejudice', author: 'Austen', text: 'I could easily forgive his pride, if he had not mortified mine.' },
  { id: 4, color: 'b', book: 'Mrs Dalloway', author: 'Woolf', text: 'She had the perpetual sense, as she watched the taxi cabs, of being out, out, far out to sea and alone.' },
];

export const CHAPTER = {
  no: 'Chapter One',
  title: 'A Truth Universally Acknowledged',
  paras: [
    'It is a truth universally acknowledged, that a single man in possession of a good fortune, must be in want of a wife.',
    'However little known the feelings or views of such a man may be on his first entering a neighbourhood, this truth is so well fixed in the minds of the surrounding families, that he is considered as the rightful property of some one or other of their daughters.',
    '“My dear Mr. Bennet,” said his lady to him one day, “have you heard that Netherfield Park is let at last?”',
    'Mr. Bennet replied that he had not.',
    '“But it is,” returned she; “for Mrs. Long has just been here, and she told me all about it.”',
    'Mr. Bennet made no answer.',
    '“Do not you want to know who has taken it?” cried his wife impatiently.',
    '“You want to tell me, and I have no objection to hearing it.”',
    'This was invitation enough.',
    '“Why, my dear, you must know, Mrs. Long says that Netherfield is taken by a young man of large fortune from the north of England; that he came down on Monday in a chaise and four to see the place, and was so much delighted with it that he agreed with Mr. Morris immediately; that he is to take possession before Michaelmas, and some of his servants are to be in the house by the end of next week.”',
    '“What is his name?”',
    '“Bingley.”',
    '“Is he married or single?”',
    '“Oh! single, my dear, to be sure! A single man of large fortune; four or five thousand a year. What a fine thing for our girls!”',
    '“How so? how can it affect them?”',
    '“My dear Mr. Bennet,” replied his wife, “how can you be so tiresome! You must know that I am thinking of his marrying one of them.”',
    '“Is that his design in settling here?”',
    '“Design! nonsense, how can you talk so! But it is very likely that he may fall in love with one of them, and therefore you must visit him as soon as he comes.”',
    '“I see no occasion for that. You and the girls may go, or you may send them by themselves, which perhaps will be still better; for as you are as handsome as any of them, Mr. Bingley might like you the best of the party.”',
    '“My dear, you flatter me. I certainly have had my share of beauty, but I do not pretend to be anything extraordinary now. When a woman has five grown-up daughters, she ought to give over thinking of her own beauty.”',
    '“In such cases, a woman has not often much beauty to think of.”',
    '“But, my dear, you must indeed go and see Mr. Bingley when he comes into the neighbourhood.”',
    '“It is more than I engage for, I assure you.”',
    '“But consider your daughters. Only think what an establishment it would be for one of them. Sir William and Lady Lucas are determined to go, merely on that account; for in general, you know, they visit no newcomers. Indeed you must go, for it will be impossible for us to visit him, if you do not.”',
    '“You are over-scrupulous, surely. I dare say Mr. Bingley will be very glad to see you; and I will send a few lines by you to assure him of my hearty consent to his marrying whichever he chooses of the girls; though I must throw in a good word for my little Lizzy.”',
    '“I desire you will do no such thing. Lizzy is not a bit better than the others; and I am sure she is not half so handsome as Jane, nor half so good-humoured as Lydia. But you are always giving her the preference.”',
    '“They have none of them much to recommend them,” replied he; “they are all silly and ignorant like other girls; but Lizzy has something more of quickness than her sisters.”',
    '“Mr. Bennet, how can you abuse your own children in such a way? You take delight in vexing me. You have no compassion for my poor nerves.”',
    '“You mistake me, my dear. I have a high respect for your nerves. They are my old friends. I have heard you mention them with consideration these last twenty years at least.”',
    '“Ah, you do not know what I suffer.”',
    '“But I hope you will get over it, and live to see many young men of four thousand a year come into the neighbourhood.”',
    '“It will be no use to us, if twenty such should come, since you will not visit them.”',
    '“Depend upon it, my dear, that when there are twenty, I will visit them all.”',
    'Mr. Bennet was so odd a mixture of quick parts, sarcastic humour, reserve, and caprice, that the experience of three-and-twenty years had been insufficient to make his wife understand his character. Her mind was less difficult to develop. She was a woman of mean understanding, little information, and uncertain temper. When she was discontented, she fancied herself nervous. The business of her life was to get her daughters married; its solace was visiting and news.',
  ],
};

export const pctOf = (b: Book) => (b.page ? Math.min(1, b.page / b.pages) : 0);
export const timeLeft = (b: Book) => {
  const m = Math.round((b.pages - (b.page || 0)) * (b.minPerPage || 0.7));
  const h = Math.floor(m / 60);
  return h ? `${h}h ${m % 60}m left` : `${m}m left`;
};
