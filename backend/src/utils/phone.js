const { parsePhoneNumber } = require('libphonenumber-js');

const normalizePhoneNumber = (phone) => {
  try {
    const phoneNumber = parsePhoneNumber(phone, 'US'); // Default to US, but can be made configurable
    if (phoneNumber && phoneNumber.isValid()) {
      return phoneNumber.format('E.164');
    }
  } catch (error) {
    console.error('Phone normalization error:', error);
  }
  return phone; // Return original if normalization fails
};

module.exports = { normalizePhoneNumber };
