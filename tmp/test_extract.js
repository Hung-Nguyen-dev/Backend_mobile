
function stripVi(s) {
  return s
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .replace(/đ/g, 'd')
    .replace(/Đ/g, 'd')
    .toLowerCase();
}

const KNOWN_PLACES = [
  { canon: 'Hà Nội', needles: ['ha noi', 'hanoi'] },
  { canon: 'TP.HCM', needles: ['ho chi minh', 'sai gon', 'saigon', 'tphcm', 'tp hcm', 'tp. hcm', 'hcm'] },
  // ...
];
const KNOWN_PLACES_EXT = [...KNOWN_PLACES];

function extractDestination(raw) {
  const t = raw.trim();
  if (!t) return '';

  const n = stripVi(t);

  for (const { canon, needles } of KNOWN_PLACES_EXT) {
    for (const needle of needles) {
      if (n.includes(needle)) return canon;
    }
  }

  let rest = t
    .replace(/^tôi\s+muốn\s+(đi|đến|tới)\s+/iu, '')
    .replace(/^mình\s+muốn\s+(đi|đến|tới)\s+/iu, '')
    .replace(/^mình\s+(đi|đến|tới)\s+/iu, '')
    .replace(/^tôi\s+(đi|đến|tới)\s+/iu, '')
    .replace(/^muốn\s+(đi|đến|tới)\s+/iu, '')
    .replace(/^(đi|đến|tới)\s+/iu, '')
    .replace(/\s*(nhé|nha|ạ|nhỉ|đi|!|。)*$/iu, '')
    .trim();

  return rest || t;
}

console.log('Result for "3":', extractDestination("3"));
console.log('Result for "hanoi":', extractDestination("hanoi"));
