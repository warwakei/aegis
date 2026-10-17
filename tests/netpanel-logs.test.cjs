const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');

// Exercise the shipped function without starting the DOM-based dashboard.
const source = fs.readFileSync(path.join(__dirname, '../src/main/resources/netpanel/app.js'), 'utf8');
const start = source.indexOf('    function mergeLogs(');
const end = source.indexOf('    function startClock(', start);
assert.ok(start >= 0 && end > start);
const mergeLogs = vm.runInNewContext(source.slice(start, end) + '\nmergeLogs');

test('keeps distinct entries emitted in the same millisecond', () => {
    const logs = mergeLogs([], [
        { id: 'session:1', timestamp: 100, message: 'same' },
        { id: 'session:2', timestamp: 100, message: 'same' }
    ]);
    assert.equal(logs.length, 2);
});

test('repeated stream snapshots do not duplicate entries', () => {
    const entry = { id: 'session:1', timestamp: 100, message: 'first' };
    assert.equal(mergeLogs([entry], [entry]).length, 1);
});

test('legacy records are distinguished by message and level', () => {
    const logs = mergeLogs([], [
        { timestamp: 100, level: 'INFO', message: 'first' },
        { timestamp: 100, level: 'INFO', message: 'second' },
        { timestamp: 100, level: 'WARN', message: 'first' }
    ]);
    assert.equal(logs.length, 3);
});

test('history remains bounded during long sessions', () => {
    const entries = Array.from({ length: 12000 }, (_, i) => ({ id: 'session:' + i, timestamp: i }));
    const logs = mergeLogs([], entries);
    assert.equal(logs.length, 10000);
    assert.equal(logs[0].timestamp, 2000);
    assert.equal(logs[9999].timestamp, 11999);
});

test('out of order snapshots are merged chronologically', () => {
    const logs = mergeLogs([{ id: 'b', timestamp: 200 }], [{ id: 'a', timestamp: 100 }]);
    assert.equal(logs.map(entry => entry.id).join(','), 'a,b');
});
