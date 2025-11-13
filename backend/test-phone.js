const { normalizePhoneNumber } = require('./src/utils/phone');

const testPhones = [
  '09123456789',
  '+989123456789',
  '00989123456789',
  '9123456789',
  '02112345678', // Tehran area code
  '+1-555-123-4567' // International
];

console.log('Testing Iranian phone number normalization:');
testPhones.forEach(phone => {
  const normalized = normalizePhoneNumber(phone);
  console.log(`${phone.padEnd(15)} -> ${normalized}`);
});
