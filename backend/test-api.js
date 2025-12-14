const http = require('http');

const BASE_URL = 'http://localhost:3001';
let authToken = '';

// Helper to make HTTP requests
function makeRequest(method, path, body = null, token = null) {
    return new Promise((resolve, reject) => {
        const url = new URL(path, BASE_URL);
        const options = {
            method,
            headers: {
                'Content-Type': 'application/json',
            }
        };

        if (token) {
            options.headers['Authorization'] = `Bearer ${token}`;
        }

        const req = http.request(url, options, (res) => {
            let data = '';
            res.on('data', chunk => data += chunk);
            res.on('end', () => {
                try {
                    const json = data ? JSON.parse(data) : null;
                    resolve({ status: res.statusCode, data: json, headers: res.headers });
                } catch (e) {
                    resolve({ status: res.statusCode, data, headers: res.headers });
                }
            });
        });

        req.on('error', reject);

        if (body) {
            req.write(JSON.stringify(body));
        }
        req.end();
    });
}

// Test results storage
const results = {
    passed: [],
    failed: [],
    warnings: []
};

function logTest(name, passed, message) {
    const result = { name, message };
    if (passed) {
        results.passed.push(result);
        console.log(`✓ ${name}: ${message}`);
    } else {
        results.failed.push(result);
        console.log(`✗ ${name}: ${message}`);
    }
}

function logWarning(name, message) {
    results.warnings.push({ name, message });
    console.log(`⚠ ${name}: ${message}`);
}

async function runTests() {
    console.log('========================================');
    console.log('CRM Backend API Tests');
    console.log('========================================\n');

    // Test 1: Health Check
    try {
        const res = await makeRequest('GET', '/health');
        logTest('Health Check', res.status === 200, `Server is ${res.data?.status || 'unknown'}`);
    } catch (e) {
        logTest('Health Check', false, `Error: ${e.message}`);
    }

    // Test 2: Login
    try {
        const res = await makeRequest('POST', '/auth/login', {
            username: 'admin',
            password: 'admin123'
        });

        if (res.status === 200 && res.data?.token) {
            authToken = res.data.token;
            logTest('Login', true, 'Successfully logged in');
        } else {
            logTest('Login', false, `Status: ${res.status}, Response: ${JSON.stringify(res.data)}`);
            return; // Cannot continue without auth
        }
    } catch (e) {
        logTest('Login', false, `Error: ${e.message}`);
        return;
    }

    // Test 3: Get Contacts (without filter)
    try {
        const res = await makeRequest('GET', '/contacts?page=1&limit=10', null, authToken);
        logTest('Get Contacts', res.status === 200, `Retrieved ${res.data?.data?.length || 0} contacts`);
    } catch (e) {
        logTest('Get Contacts', false, `Error: ${e.message}`);
    }

    // Test 4: Get Contacts with updated_since
    try {
        const yesterday = new Date(Date.now() - 24 * 60 * 60 * 1000).toISOString();
        const res = await makeRequest('GET', `/contacts?page=1&limit=10&updated_since=${encodeURIComponent(yesterday)}`, null, authToken);

        if (res.status === 200) {
            logTest('Get Contacts (Delta Sync)', true, `Retrieved ${res.data?.data?.length || 0} updated contacts`);
        } else {
            logTest('Get Contacts (Delta Sync)', false, `Status: ${res.status}, Error: ${JSON.stringify(res.data)}`);
        }
    } catch (e) {
        logTest('Get Contacts (Delta Sync)', false, `Error: ${e.message}`);
    }

    // Test 5: Create Contact
    let createdContactId = null;
    try {
        const res = await makeRequest('POST', '/contacts', {
            name: 'Test Contact',
            phone_raw: '+1234567890',
            created_at: new Date().toISOString()
        }, authToken);

        if (res.status === 201 || res.status === 200) {
            createdContactId = res.data?.contact?.id;
            logTest('Create Contact', true, `Created contact with ID: ${createdContactId}`);
        } else {
            logTest('Create Contact', false, `Status: ${res.status}, Response: ${JSON.stringify(res.data)}`);
        }
    } catch (e) {
        logTest('Create Contact', false, `Error: ${e.message}`);
    }

    // Test 6: Batch Create Contacts (Upsert Test)
    try {
        // We will try to update the contact created in Test 5 and add a new one
        const randomPhone = `+1${Math.floor(Math.random() * 1000000000).toString().padStart(9, '0')}`;
        const batchContacts = [
            {
                name: 'Batch New Contact',
                phone_raw: randomPhone,
                created_at: new Date().toISOString()
            }
        ];

        if (createdContactId) {
            batchContacts.push({
                name: 'Updated via Batch', // New name
                phone_raw: '+1234567890', // Same phone as Test 5 (should trigger update)
                created_at: new Date(Date.now() + 10000).toISOString() // Newer timestamp
            });
        }

        const res = await makeRequest('POST', '/contacts/batch', {
            contacts: batchContacts
        }, authToken);

        if (res.status === 200) {
            const summary = res.data?.summary;
            // Expect 1 created (new) and 1 updated (existing)
            const expectedCreated = 1;
            const expectedUpdated = createdContactId ? 1 : 0;

            if (summary.created === expectedCreated && summary.updated === expectedUpdated) {
                logTest('Batch Create Contacts (Upsert)', true, `Correctly created ${summary.created} and updated ${summary.updated}`);
            } else {
                logTest('Batch Create Contacts (Upsert)', false, `Mismatch: Expected ${expectedCreated} created, ${expectedUpdated} updated. Got: ${JSON.stringify(summary)}`);
            }
        } else {
            logTest('Batch Create Contacts (Upsert)', false, `Status: ${res.status}, Response: ${JSON.stringify(res.data)}`);
        }
    } catch (e) {
        logTest('Batch Create Contacts (Upsert)', false, `Error: ${e.message}`);
    }

    // Test 7: Update Contact
    if (createdContactId) {
        try {
            const res = await makeRequest('PUT', `/contacts/${createdContactId}`, {
                name: 'Updated Test Contact',
                phone_raw: '+1234567890'
            }, authToken);

            logTest('Update Contact', res.status === 200, `Updated contact ${createdContactId}`);
        } catch (e) {
            logTest('Update Contact', false, `Error: ${e.message}`);
        }
    } else {
        logWarning('Update Contact', 'Skipped - no contact created');
    }

    // Test 8: Search Contacts
    try {
        const res = await makeRequest('GET', '/contacts/search?q=Test', null, authToken);
        logTest('Search Contacts', res.status === 200, `Found ${res.data?.data?.length || 0} contacts`);
    } catch (e) {
        logTest('Search Contacts', false, `Error: ${e.message}`);
    }

    // Test 8b: Fuzzy Search Contacts
    try {
        // Search for "Contac" (missing 't') to find "Contact"
        const res = await makeRequest('GET', '/contacts/search?q=Contac', null, authToken);
        const found = res.data?.data?.length || 0;
        if (res.status === 200 && found > 0) {
            logTest('Fuzzy Search Contacts', true, `Found ${found} contacts with typo "Contac"`);
        } else {
            logTest('Fuzzy Search Contacts', false, `Failed to find contacts with typo. Status: ${res.status}, Found: ${found}`);
        }
    } catch (e) {
        logTest('Fuzzy Search Contacts', false, `Error: ${e.message}`);
    }

    // Test 9: Get Call Logs
    try {
        const res = await makeRequest('GET', '/calls?page=1&limit=10', null, authToken);
        logTest('Get Call Logs', res.status === 200, `Retrieved ${res.data?.data?.length || 0} call logs`);
    } catch (e) {
        logTest('Get Call Logs', false, `Error: ${e.message}`);
    }

    // Test 10: Upload Call Logs (Normal)
    const testTimestamp = new Date().toISOString();
    try {
        const res = await makeRequest('POST', '/calls', {
            calls: [
                {
                    contact_id: createdContactId,
                    direction: 'outgoing',
                    duration_seconds: 120,
                    timestamp: testTimestamp
                }
            ]
        }, authToken);

        if (res.status === 200 || res.status === 201) {
            logTest('Upload Call Logs', true, `Uploaded call logs`);
        } else {
            logTest('Upload Call Logs', false, `Status: ${res.status}, Response: ${JSON.stringify(res.data)}`);
        }
    } catch (e) {
        logTest('Upload Call Logs', false, `Error: ${e.message}`);
    }

    // Test 11: Upload Duplicate Call Logs (Should be deduplicated)
    try {
        const res = await makeRequest('POST', '/calls', {
            calls: [
                {
                    contact_id: createdContactId,
                    direction: 'outgoing',
                    duration_seconds: 120,
                    timestamp: testTimestamp // Same timestamp
                }
            ]
        }, authToken);

        if (res.status === 200 || res.status === 201) {
            // We expect the backend to accept it but NOT create a new record (count should be 0 or it might return empty array depending on implementation)
            // My implementation returns the inserted calls. If it's a duplicate, it skips it.
            // So if we send 1 duplicate, we expect `calls` array to be empty or length 0.
            const insertedCount = res.data?.calls?.length || 0;
            if (insertedCount === 0) {
                logTest('Duplicate Call Log Check', true, `Correctly ignored duplicate call log`);
            } else {
                logTest('Duplicate Call Log Check', false, `Failed: Inserted ${insertedCount} duplicates`);
            }
        } else {
            logTest('Duplicate Call Log Check', false, `Status: ${res.status}, Response: ${JSON.stringify(res.data)}`);
        }
    } catch (e) {
        logTest('Duplicate Call Log Check', false, `Error: ${e.message}`);
    }

    // Test 12: Delete Contact (cleanup)
    if (createdContactId) {
        try {
            const res = await makeRequest('DELETE', `/contacts/${createdContactId}`, null, authToken);
            logTest('Delete Contact', res.status === 200, `Deleted contact ${createdContactId}`);
        } catch (e) {
            logTest('Delete Contact', false, `Error: ${e.message}`);
        }
    } else {
        logWarning('Delete Contact', 'Skipped - no contact to delete');
    }

    // Test 13: CSV Export
    try {
        // Note: makeRequest parses JSON by default, but for CSV it might fail or return string if we handle it.
        // Let's adjust makeRequest or just handle the string data if it fails to parse.
        // Actually, my makeRequest implementation tries JSON.parse and returns raw data if it fails.
        // So res.data will be the CSV string.
        const res = await makeRequest('GET', '/contacts/export', null, authToken);

        if (res.status === 200) {
            const contentType = res.headers['content-type'];
            const isCsv = contentType && contentType.includes('text/csv');
            // Check for CSV headers
            const hasHeaders = res.data && typeof res.data === 'string' && res.data.includes('"Name","Phone","Created By","Created At"');

            if (isCsv && hasHeaders) {
                logTest('CSV Export', true, 'Received valid CSV file');
            } else {
                logTest('CSV Export', false, `Invalid format. Content-Type: ${contentType}, Data start: ${typeof res.data === 'string' ? res.data.substring(0, 50) : 'Not a string'}...`);
            }
        } else {
            logTest('CSV Export', false, `Status: ${res.status}`);
        }
    } catch (e) {
        logTest('CSV Export', false, `Error: ${e.message}`);
    }

    // Summary
    console.log('\n========================================');
    console.log('Test Summary');
    console.log('========================================');
    console.log(`✓ Passed: ${results.passed.length}`);
    console.log(`✗ Failed: ${results.failed.length}`);
    console.log(`⚠ Warnings: ${results.warnings.length}`);

    if (results.failed.length > 0) {
        console.log('\nFailed Tests:');
        results.failed.forEach(r => console.log(`  - ${r.name}: ${r.message}`));
    }

    if (results.warnings.length > 0) {
        console.log('\nWarnings:');
        results.warnings.forEach(r => console.log(`  - ${r.name}: ${r.message}`));
    }

    process.exit(results.failed.length > 0 ? 1 : 0);
}

runTests().catch(err => {
    console.error('Fatal error:', err);
    process.exit(1);
});
