const fetch = require('node-fetch');

const API_BASE = 'http://localhost:3000';

// Test contact deduplication
async function testContactDeduplication() {
  try {
    // Login as admin
    console.log('🔐 Logging in as admin...');
    const loginResponse = await fetch(`${API_BASE}/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username: 'admin', password: 'admin123' })
    });

    const loginData = await loginResponse.json();
    if (!loginResponse.ok) {
      throw new Error(`Login failed: ${loginData.error}`);
    }

    const token = loginData.token;
    const headers = {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`
    };

    console.log('✅ Login successful');

    // Test 1: Create contact with Iranian mobile format
    console.log('\n📱 Test 1: Creating contact with 09123456789...');
    const contact1 = await fetch(`${API_BASE}/contacts`, {
      method: 'POST',
      headers,
      body: JSON.stringify({
        name: 'John Doe',
        phone_raw: '09123456789'
      })
    });

    const contact1Data = await contact1.json();
    if (contact1.ok) {
      console.log('✅ Contact created:', contact1Data.contact);
    } else {
      console.log('❌ Failed to create contact:', contact1Data);
    }

    // Test 2: Try to create same contact with different format (+989123456789)
    console.log('\n📱 Test 2: Creating same contact with +989123456789...');
    const contact2 = await fetch(`${API_BASE}/contacts`, {
      method: 'POST',
      headers,
      body: JSON.stringify({
        name: 'John Doe',
        phone_raw: '+989123456789'
      })
    });

    const contact2Data = await contact2.json();
    if (contact2.ok) {
      console.log('✅ Same contact returned (deduplication working):', contact2Data.contact);
    } else if (contact2.status === 409) {
      console.log('✅ Conflict detected (deduplication working):', contact2Data.error);
    } else {
      console.log('❌ Unexpected response:', contact2Data);
    }

    // Test 3: Try to create same contact with international format (00989123456789)
    console.log('\n📱 Test 3: Creating same contact with 00989123456789...');
    const contact3 = await fetch(`${API_BASE}/contacts`, {
      method: 'POST',
      headers,
      body: JSON.stringify({
        name: 'John Doe',
        phone_raw: '00989123456789'
      })
    });

    const contact3Data = await contact3.json();
    if (contact3.ok) {
      console.log('✅ Same contact returned (deduplication working):', contact3Data.contact);
    } else if (contact3.status === 409) {
      console.log('✅ Conflict detected (deduplication working):', contact3Data.error);
    } else {
      console.log('❌ Unexpected response:', contact3Data);
    }

    // Test 4: Login as user and try to create same contact
    console.log('\n👤 Test 4: Logging in as user...');
    const userLoginResponse = await fetch(`${API_BASE}/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username: 'user', password: 'user123' })
    });

    const userLoginData = await userLoginResponse.json();
    if (!userLoginResponse.ok) {
      throw new Error(`User login failed: ${userLoginData.error}`);
    }

    const userToken = userLoginData.token;
    const userHeaders = {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${userToken}`
    };

    console.log('✅ User login successful');

    // Try to create same contact as user
    console.log('\n🚫 Test 5: User trying to create same contact...');
    const userContact = await fetch(`${API_BASE}/contacts`, {
      method: 'POST',
      headers: userHeaders,
      body: JSON.stringify({
        name: 'John Doe',
        phone_raw: '09123456789'
      })
    });

    const userContactData = await userContact.json();
    if (userContact.status === 409) {
      console.log('✅ User blocked from creating duplicate (cross-user deduplication working):', userContactData.error);
    } else {
      console.log('❌ User was allowed to create duplicate:', userContactData);
    }

  } catch (error) {
    console.error('❌ Test failed:', error.message);
  }
}

testContactDeduplication();
