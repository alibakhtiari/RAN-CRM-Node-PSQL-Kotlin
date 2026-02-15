const { parsePhoneNumber } = require('libphonenumber-js');
const logger = require('./logger');

const normalizePhoneNumber = (phone) => {
  try {
    // Clean the phone number first
    let cleanedPhone = phone.replace(/\s+/g, '').replace(/[-()]/g, '');

    // Handle Iranian phone number formats
    if (cleanedPhone.startsWith('00989')) {
      // 00989123456789 -> +989123456789
      cleanedPhone = '+' + cleanedPhone.substring(2);
    } else if (cleanedPhone.startsWith('09')) {
      // 09123456789 -> +989123456789
      cleanedPhone = '+98' + cleanedPhone.substring(1);
    } else if (cleanedPhone.startsWith('9') && cleanedPhone.length === 10) {
      // 9123456789 -> +989123456789
      cleanedPhone = '+98' + cleanedPhone;
    }

    // Try to parse with Iran region first, then fallback to international
    let phoneNumber = parsePhoneNumber(cleanedPhone, 'IR');
    if (!phoneNumber || !phoneNumber.isValid()) {
      phoneNumber = parsePhoneNumber(cleanedPhone);
    }

    if (phoneNumber && phoneNumber.isValid()) {
      return phoneNumber.format('E.164');
    }
  } catch (error) {
    logger.warn('Phone normalization error:', { phone, error: error.message });
  }
  return phone; // Return original if normalization fails
};

module.exports = { normalizePhoneNumber };
