// Static contract verification, no database connection and no additional npm dependencies.
const fs = require('node:fs');
const path = require('node:path');
const assert = require('node:assert/strict');
const root = path.resolve(__dirname, '..');
process.chdir(root);
const camel = s => s.replace(/_([a-z])/g, (_, c) => c.toUpperCase());
const javaType = s => s === 'BIGINT' ? 'Long' : s === 'SMALLINT' ? 'Short' : s === 'INT' ? 'Integer'
    : s === 'BOOLEAN' ? 'Boolean' : s.startsWith('DATETIME') ? 'LocalDateTime'
    : s.startsWith('DECIMAL') ? 'BigDecimal' : 'String';
const groups = ['auth', 'academic', 'question', 'exam', 'attempt', 'proctoring', 'common'];
const expectedCounts = [6, 5, 6, 5, 6, 4, 2];
const files = fs.readdirSync('database/migrations').filter(f => /^V\d+__.*\.sql$/.test(f)).sort();
assert.equal(files.length, 7);
const tables = new Map(), allConstraints = new Set(), classNames = new Set(), rows = [], foreignKeys = [];
let beans = 0, columns = 0;
files.forEach((file, index) => {
    assert.ok(file.startsWith(`V${index + 1}__`));
    const sql = fs.readFileSync(`database/migrations/${file}`, 'utf8');
    const matches = [...sql.matchAll(/-- (\w+): (\w+)\nCREATE TABLE (\w+) \(\n([\s\S]*?)\n\) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;/g)];
    assert.equal(matches.length, expectedCounts[index]);
    const beanFiles = fs.readdirSync(`src/main/java/vn/edu/onlineexam/model/bean/${groups[index]}`).filter(f => f.endsWith('.java'));
    assert.equal(beanFiles.length, expectedCounts[index]);
    for (const [, bean, name, sqlName, body] of matches) {
        assert.equal(name, sqlName);
        assert.ok(!classNames.has(bean), `Duplicate Bean class name ${bean}`);
        classNames.add(bean);
        assert.ok(!tables.has(name), `Duplicate table ${name}`);
        // SQL type parameters end with ')', so use a space boundary rather than a word boundary.
        const parsed = body.split('\n').map(line => line.match(/^    (\w+) ((?:BIGINT|SMALLINT|INT|BOOLEAN|JSON|LONGTEXT|TEXT|VARCHAR\(\d+\)|CHAR\(\d+\)|DATETIME\(3\)|DECIMAL\(\d+,\d+\)))(?= )/)).filter(Boolean);
        assert.ok(parsed.length > 0);
        const colMap = new Map(parsed.map(c => [c[1], c[2]]));
        const keys = [...body.matchAll(/(?:PRIMARY KEY|UNIQUE) \(([^)]+)\)/g)].map(m => m[1].split(',').map(s => s.trim()));
        assert.ok(/PRIMARY KEY/.test(body));
        tables.set(name, { colMap, keys });
        const pkg = `vn.edu.onlineexam.model.bean.${groups[index]}`;
        const source = fs.readFileSync(`src/main/java/${pkg.replaceAll('.', '/')}/${bean}.java`, 'utf8');
        assert.ok(source.includes(`package ${pkg};`));
        assert.ok(!source.includes('toString('), 'Do not expose sensitive fields in toString');
        const fields = [...source.matchAll(/private (\w+) (\w+);/g)];
        assert.equal(fields.length, parsed.length, name);
        parsed.forEach((col, i) => {
            const field = camel(col[1]), type = javaType(col[2]);
            assert.equal(fields[i][1], type, `${name}.${field} type`);
            assert.equal(fields[i][2], field, `${name}.${field} name/order`);
            rows.push(`${pkg}.${bean}\t${field}\t${type}`);
        });
        for (const m of body.matchAll(/CONSTRAINT (\w+)/g)) {
            assert.ok(m[1].length <= 64);
            assert.ok(!allConstraints.has(m[1]), `Duplicate constraint ${m[1]}`);
            allConstraints.add(m[1]);
        }
        for (const m of body.matchAll(/FOREIGN KEY \(([^)]+)\)\s+REFERENCES (\w+) \(([^)]+)\) ON DELETE RESTRICT ON UPDATE RESTRICT/g)) {
            const local = m[1].split(',').map(s => s.trim()), remote = m[3].split(',').map(s => s.trim());
            const parent = tables.get(m[2]);
            assert.ok(parent, `Parent ${m[2]} must exist before ${name}`);
            assert.equal(local.length, remote.length);
            local.forEach((col, i) => assert.equal(colMap.get(col), parent.colMap.get(remote[i]), `FK type: ${name}.${col}`));
            assert.ok(parent.keys.some(k => JSON.stringify(k) === JSON.stringify(remote)), `FK target must be a unique key: ${m[2]}`);
            foreignKeys.push(`${name}.${m[1]} -> ${m[2]}.${m[3]}`);
        }
        columns += parsed.length;
        beans++;
    }
});
assert.equal(tables.size, 34);
assert.equal(beans, 34);
assert.equal(foreignKeys.length, 60);
const attempts = fs.readFileSync('database/migrations/V5__create_attempt_tables.sql', 'utf8');
assert.ok(attempts.includes("(exam_id, (CASE WHEN status = 'IN_PROGRESS' THEN student_id ELSE NULL END))"));
fs.mkdirSync('target', { recursive: true });
fs.writeFileSync('target/database-bean-contract.tsv', rows.join('\n') + '\n');
console.log(`PASS: ${tables.size} tables, ${beans} beans, ${columns} columns/fields, ${foreignKeys.length} FK dependencies/types/unique targets.`);
console.log('Static check only; SQL execution on MySQL still requires a separately authorized empty database.');
