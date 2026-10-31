#!/usr/bin/env node
// Seeds a sales history: a few customers and their COMPLETED orders over the last 3 weeks, so the shop has
// real numbers for "Đã bán N" and the best sellers carousel.
//
// Requires: docker compose up -d, services running (scripts/start-all.sh). Works from any shell:
//   node scripts/seed-sales.js
//
// The orders are written straight into order_db and kitchen_db (past cooking days, so no saga, no emails, and
// tomorrow's 50 jars are untouched). Running it again replaces the previous seed: seed orders have ids starting
// with 5eed0000-, so they are easy to find and delete.
const { spawnSync } = require('node:child_process');
const path = require('node:path');

const GW = process.env.GW ?? 'http://localhost:9000';
const ROOT = path.join(__dirname, '..');
const DAYS = 21;
const SEED_PREFIX = '5eed0000-';

const CUSTOMERS = [
  ['Nguyễn Thị Lan', 'lan.nguyen'], ['Trần Minh Khoa', 'khoa.tran'], ['Lê Hoàng Anh', 'anh.le'],
  ['Phạm Thu Hà', 'ha.pham'], ['Võ Quốc Bảo', 'bao.vo'], ['Đặng Mỹ Linh', 'linh.dang'],
  ['Bùi Thanh Tâm', 'tam.bui'], ['Hồ Ngọc Vy', 'vy.ho'],
].map(([name, user]) => ({ name, email: `${user}@khach.pameobeo.vn` }));
const PASSWORD = 'Password1';
const ADDRESSES = ['12 Nguyễn Huệ, Q1', '45 Lê Văn Sỹ, Q3', '88 Phan Xích Long, Phú Nhuận', '210 Võ Văn Ngân, Thủ Đức',
  '7 Trần Não, Q2', '33 Cộng Hòa, Tân Bình', '19 Nguyễn Thị Thập, Q7', '64 Bạch Đằng, Bình Thạnh'];

// How often each product is picked (others: 1). Gives a believable best sellers ranking.
const POPULARITY = {
  'Pate gà có gan': 12, 'Pate cá hồi có gan': 8, 'Pate mix gà + cá hồi có gan': 7, 'Pate bò không gan': 6,
  'Pate cá ngừ có gan': 5, 'Pate mix gà + bò có gan': 4, 'Pate gà không gan': 3, 'Pate mix cá ngừ + cá hồi không gan': 2,
};

// Deterministic random numbers: the same seed gives the same history
let state = 20261029;
function random() {
  state = (state + 0x6d2b79f5) | 0;
  let t = Math.imul(state ^ (state >>> 15), 1 | state);
  t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t;
  return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
}
const between = (min, max) => min + Math.floor(random() * (max - min + 1));
const sql = (s) => `'${String(s).replaceAll("'", "''")}'`;

async function api(method, url, body) {
  const res = await fetch(GW + url, {
    method, headers: { 'Content-Type': 'application/json' }, body: body && JSON.stringify(body),
  });
  return { status: res.status, body: await res.json().catch(() => null) };
}

/** Registers the customer if needed, returns its user id (the JWT subject). */
async function customerId(c) {
  await api('POST', '/api/auth/register', { email: c.email, password: PASSWORD, fullName: c.name }); // 409 if it exists
  const login = await api('POST', '/api/auth/login', { email: c.email, password: PASSWORD });
  if (login.status !== 200) throw new Error(`login ${c.email} failed: ${JSON.stringify(login.body)}`);
  return JSON.parse(Buffer.from(login.body.accessToken.split('.')[1], 'base64url')).sub;
}

function psql(db, script) {
  const r = spawnSync('docker', ['compose', 'exec', '-T', 'postgres', 'psql', '-U', 'pameobeo', '-d', db,
    '-v', 'ON_ERROR_STOP=1', '-q'], { cwd: ROOT, input: `BEGIN;\n${script}\nCOMMIT;\n`, encoding: 'utf8' });
  if (r.status !== 0) throw new Error(`psql ${db} failed:\n${r.stderr || r.stdout || r.error}`);
}

/** Vietnam date (UTC+7) as yyyy-mm-dd, `daysAgo` days before today */
function vnDate(daysAgo) {
  const d = new Date(Date.now() + 7 * 3600_000 - daysAgo * 86400_000);
  return d.toISOString().slice(0, 10);
}
/** Instant of `hh:mm` Vietnam time on `date` shifted by `dayOffset` days */
function vnInstant(date, dayOffset, hh, mm) {
  const t = Date.parse(`${date}T00:00:00Z`) + dayOffset * 86400_000 + (hh - 7) * 3600_000 + mm * 60_000;
  return new Date(t).toISOString();
}

async function main() {
  const menu = (await api('GET', '/api/kitchen/menu')).body;
  if (!Array.isArray(menu)) throw new Error(`cannot read the menu at ${GW}: are the services running?`);
  const weighted = menu.filter((m) => m.active).flatMap((m) => Array(POPULARITY[m.name] ?? 1).fill(m));
  const customers = [];
  for (const c of CUSTOMERS) customers.push({ ...c, id: await customerId(c) });

  const orders = [];
  for (let daysAgo = DAYS; daysAgo >= 1; daysAgo--) {
    const cookingDate = vnDate(daysAgo);
    for (let n = between(2, 6); n > 0; n--) {
      const lines = new Map();
      for (let k = between(1, 2); k > 0; k--) {
        const item = weighted[between(0, weighted.length - 1)];
        lines.set(item.id, { item, quantity: (lines.get(item.id)?.quantity ?? 0) + between(1, 3) });
      }
      const customer = customers[between(0, customers.length - 1)];
      orders.push({
        id: `${SEED_PREFIX}0000-4000-8000-${String(orders.length + 1).padStart(12, '0')}`,
        customer, cookingDate, lines: [...lines.values()],
        address: ADDRESSES[CUSTOMERS.findIndex((c) => c.email === customer.email)],
        createdAt: vnInstant(cookingDate, -1, between(8, 17), between(0, 59)), // ordered the day before, before 18:00
        cutoffAt: vnInstant(cookingDate, -1, 18, 0),
        completedAt: vnInstant(cookingDate, 0, between(19, 21), between(0, 59)),
      });
    }
  }
  const dates = [...new Set(orders.map((o) => o.cookingDate))];

  const orderSql = [`DELETE FROM order_items WHERE order_id::text LIKE '${SEED_PREFIX}%';`,
    `DELETE FROM orders WHERE id::text LIKE '${SEED_PREFIX}%';`];
  const kitchenSql = [`DELETE FROM slot_reservation_items WHERE order_id::text LIKE '${SEED_PREFIX}%';`,
    `DELETE FROM slot_reservations WHERE order_id::text LIKE '${SEED_PREFIX}%';`];
  for (const o of orders) {
    const total = o.lines.reduce((s, l) => s + l.quantity * l.item.priceVnd, 0);
    const jars = o.lines.reduce((s, l) => s + l.quantity, 0);
    orderSql.push(`INSERT INTO orders (id, customer_id, customer_email, customer_name, cooking_date, cutoff_at, delivery_address,
      idempotency_key, status, status_reason, total_vnd, created_at, updated_at) VALUES (${sql(o.id)}, ${sql(o.customer.id)},
      ${sql(o.customer.email)}, ${sql(o.customer.name)}, ${sql(o.cookingDate)}, ${sql(o.cutoffAt)}, ${sql(o.address)},
      ${sql('seed-' + o.id)}, 'COMPLETED', NULL, ${total}, ${sql(o.createdAt)}, ${sql(o.completedAt)});`);
    o.lines.forEach((l, i) => orderSql.push(`INSERT INTO order_items VALUES (${sql(o.id)}, ${i + 1}, ${sql(l.item.id)},
      ${sql(l.item.name)}, ${l.item.priceVnd}, ${l.quantity});`));
    kitchenSql.push(`INSERT INTO slot_reservations (order_id, cooking_date, quantity, status, created_at)
      VALUES (${sql(o.id)}, ${sql(o.cookingDate)}, ${jars}, 'RESERVED', ${sql(o.createdAt)});`);
    o.lines.forEach((l) => kitchenSql.push(`INSERT INTO slot_reservation_items (order_id, menu_item_id, quantity)
      VALUES (${sql(o.id)}, ${sql(l.item.id)}, ${l.quantity});`));
  }
  // Past cooking days: 50 jars of capacity, reserved = what the seed (and any real order) holds
  const dateList = dates.map(sql).join(', ');
  kitchenSql.push(`INSERT INTO cooking_days (cooking_date, capacity, reserved_count)
      SELECT d::date, 50, 0 FROM unnest(ARRAY[${dateList}]) AS d ON CONFLICT (cooking_date) DO NOTHING;`,
  `UPDATE cooking_days c SET reserved_count = COALESCE((SELECT SUM(r.quantity) FROM slot_reservations r
      WHERE r.cooking_date = c.cooking_date AND r.status = 'RESERVED'), 0),
      capacity = GREATEST(c.capacity, COALESCE((SELECT SUM(r.quantity) FROM slot_reservations r
      WHERE r.cooking_date = c.cooking_date AND r.status = 'RESERVED'), 0))
      WHERE c.cooking_date IN (${dateList});`);

  psql('order_db', orderSql.join('\n'));
  psql('kitchen_db', kitchenSql.join('\n'));

  const jars = orders.flatMap((o) => o.lines).reduce((s, l) => s + l.quantity, 0);
  console.log(`Seeded ${customers.length} customers, ${orders.length} completed orders (${jars} jars), `
    + `cooking days ${dates[0]} .. ${dates.at(-1)}. Customer password: ${PASSWORD}`);
  const highlights = (await api('GET', '/api/kitchen/menu/highlights')).body ?? [];
  console.log('Best sellers (last 30 days):');
  for (const h of highlights) console.log(`  ${String(h.item.jarsSold).padStart(4)} jars  ${h.item.name}  (${h.reason})`);
}

main().catch((e) => { console.error(e.message); process.exit(1); });
