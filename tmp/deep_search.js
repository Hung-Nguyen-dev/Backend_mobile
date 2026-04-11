
const fs = require('fs');
const path = require('path');

function search(dir, pattern) {
  const files = fs.readdirSync(dir);
  for (const file of files) {
    const fullPath = path.join(dir, file);
    if (fs.statSync(fullPath).isDirectory()) {
      if (file !== 'node_modules' && file !== '.git') {
        search(fullPath, pattern);
      }
    } else if (file.endsWith('.tsx') || file.endsWith('.ts') || file.endsWith('.js')) {
      const content = fs.readFileSync(fullPath, 'utf8');
      if (content.includes(pattern)) {
        console.log('Found in:', fullPath);
      }
    }
  }
}

const target = 'Tuyệt \u2014 mình ghi nhận'; // Uses em-dash
console.log('Searching for:', target);
search('e:\\Downloads\\Backend_mobile-dev', target);
