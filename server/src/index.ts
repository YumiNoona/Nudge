/**
 * Nudge Sync Relay Server v0.1
 *
 * E2E encrypted blob sync relay.
 * Stores only encrypted blobs it CANNOT read.
 * Uses sql.js (SQLite compiled to WASM) for zero native dependencies.
 *
 * Self-hostable — run on a $5 VPS, a Raspberry Pi, or your own machine.
 * No third-party server ever holds plaintext.
 */

import express from 'express';
import cors from 'cors';
import initSqlJs, { Database } from 'sql.js';
import { v4 as uuidv4 } from 'uuid';
import { readFileSync, writeFileSync, existsSync } from 'fs';

const app = express();
app.use(cors());
app.use(express.json({ limit: '10mb' }));

const PORT = parseInt(process.env.PORT || '3741');
const DB_PATH = process.env.DB_PATH || './nudge-sync.db';

// --- Database ---

let db: Database;

function saveDb() {
  const data = db.export();
  writeFileSync(DB_PATH, Buffer.from(data));
}

function loadDb(): Database {
  if (existsSync(DB_PATH)) {
    const buffer = readFileSync(DB_PATH);
    const SQL = initSqlJs as any;
    // We need a sync init — sql.js provides it differently
    throw new Error('Async init required — see startServer');
  }
  return new (initSqlJs as any).Database();
}

// --- Types ---

interface PushRequest {
  deviceId: string;
  pairingCode: string;
  blobs: Array<{
    encryptedPayload: string;
    blobHash: string;
  }>;
}

interface PullRequest {
  deviceId: string;
  pairingCode: string;
  sinceSequence?: number;
}

interface RegisterRequest {
  deviceName: string;
}

interface PairRequest {
  pairingCode: string;
  deviceName: string;
}

// --- Routes ---

function setupRoutes() {
  app.post('/sync/register', (req, res) => {
    try {
      const { deviceName } = req.body as RegisterRequest;
      if (!deviceName) return res.status(400).json({ error: 'deviceName required' });

      const deviceId = uuidv4();
      const pairingCode = generatePairingCode();

      db.run(
        'INSERT INTO devices (id, name, pairing_code, paired_at, last_seen_at) VALUES (?, ?, ?, ?, ?)',
        [deviceId, deviceName, pairingCode, Date.now(), Date.now()]
      );
      db.run(
        'INSERT OR IGNORE INTO sync_state (device_id, last_pull_at, last_push_sequence) VALUES (?, 0, 0)',
        [deviceId]
      );
      saveDb();

      console.log(`Device registered: ${deviceName} (${deviceId})`);
      res.json({ deviceId, pairingCode });
    } catch (err: any) {
      console.error('Register error:', err);
      res.status(500).json({ error: 'Internal server error' });
    }
  });

  app.post('/sync/pair', (req, res) => {
    try {
      const { pairingCode, deviceName } = req.body as PairRequest;
      if (!pairingCode || !deviceName) {
        return res.status(400).json({ error: 'pairingCode and deviceName required' });
      }

      const stmt = db.prepare('SELECT * FROM devices WHERE pairing_code = ? AND is_active = 1');
      stmt.bind([pairingCode]);
      const found = stmt.step();
      const existing = found ? stmt.getAsObject() : null;
      stmt.free();

      if (!existing) {
        return res.status(404).json({ error: 'Invalid pairing code' });
      }

      const newDeviceId = uuidv4();
      const newPairingCode = generatePairingCode();

      db.run(
        'INSERT INTO devices (id, name, pairing_code, paired_at, last_seen_at) VALUES (?, ?, ?, ?, ?)',
        [newDeviceId, deviceName, newPairingCode, Date.now(), Date.now()]
      );
      db.run(
        'INSERT OR IGNORE INTO sync_state (device_id, last_pull_at, last_push_sequence) VALUES (?, 0, 0)',
        [newDeviceId]
      );
      saveDb();

      console.log(`Device paired: ${deviceName} (${newDeviceId}) to ${(existing as any).name}`);
      res.json({ deviceId: newDeviceId, pairingCode: newPairingCode, pairedWith: (existing as any).name });
    } catch (err: any) {
      console.error('Pair error:', err);
      res.status(500).json({ error: 'Internal server error' });
    }
  });

  app.post('/sync/push', (req, res) => {
    try {
      const { deviceId, pairingCode, blobs } = req.body as PushRequest;
      if (!deviceId || !pairingCode || !blobs?.length) {
        return res.status(400).json({ error: 'deviceId, pairingCode, and blobs required' });
      }

      if (!verifyDevice(deviceId, pairingCode)) {
        return res.status(403).json({ error: 'Invalid device or pairing code' });
      }

      const stateStmt = db.prepare('SELECT last_push_sequence FROM sync_state WHERE device_id = ?');
      stateStmt.bind([deviceId]);
      const hasState = stateStmt.step();
      const lastPushSeq = hasState ? (stateStmt.getAsObject() as any).last_push_sequence : 0;
      stateStmt.free();

      let nextSeq = (lastPushSeq ?? 0) + 1;
      let inserted = 0;

      for (const blob of blobs) {
        const checkStmt = db.prepare('SELECT id FROM sync_blobs WHERE blob_hash = ?');
        checkStmt.bind([blob.blobHash]);
        const hasExisting = checkStmt.step();
        checkStmt.free();

        if (hasExisting) continue;

        db.run(
          'INSERT OR IGNORE INTO sync_blobs (id, device_id, encrypted_payload, blob_hash, created_at, sequence_number) VALUES (?, ?, ?, ?, ?, ?)',
          [uuidv4(), deviceId, blob.encryptedPayload, blob.blobHash, Date.now(), nextSeq++]
        );
        inserted++;
      }

      db.run('UPDATE sync_state SET last_push_sequence = ? WHERE device_id = ?', [nextSeq - 1, deviceId]);
      db.run('UPDATE devices SET last_seen_at = ? WHERE id = ?', [Date.now(), deviceId]);
      saveDb();

      console.log(`Push from ${deviceId}: ${inserted} blobs stored`);
      res.json({ accepted: inserted, sequence: nextSeq - 1 });
    } catch (err: any) {
      console.error('Push error:', err);
      res.status(500).json({ error: 'Internal server error' });
    }
  });

  app.post('/sync/pull', (req, res) => {
    try {
      const { deviceId, pairingCode, sinceSequence } = req.body as PullRequest;
      if (!deviceId || !pairingCode) {
        return res.status(400).json({ error: 'deviceId and pairingCode required' });
      }

      if (!verifyDevice(deviceId, pairingCode)) {
        return res.status(403).json({ error: 'Invalid device or pairing code' });
      }

      const since = sinceSequence ?? 0;

      const blobStmt = db.prepare(`
        SELECT sb.id, sb.device_id as deviceId, sb.encrypted_payload as encryptedPayload,
               sb.blob_hash as blobHash, sb.created_at as createdAt, sb.sequence_number as sequence
        FROM sync_blobs sb
        JOIN devices d ON sb.device_id = d.id AND d.is_active = 1
        WHERE sb.sequence_number > ?
        ORDER BY sb.sequence_number ASC
        LIMIT 500
      `);
      blobStmt.bind([since]);

      const blobs: any[] = [];
      while (blobStmt.step()) {
        blobs.push(blobStmt.getAsObject());
      }
      blobStmt.free();

      db.run('UPDATE sync_state SET last_pull_at = ? WHERE device_id = ?', [Date.now(), deviceId]);
      db.run('UPDATE devices SET last_seen_at = ? WHERE id = ?', [Date.now(), deviceId]);
      saveDb();

      const maxSequence = blobs.length > 0
        ? Math.max(...blobs.map((b: any) => b.sequence))
        : since;

      console.log(`Pull for ${deviceId}: ${blobs.length} blobs since seq ${since}`);
      res.json({ blobs, latestSequence: maxSequence });
    } catch (err: any) {
      console.error('Pull error:', err);
      res.status(500).json({ error: 'Internal server error' });
    }
  });

  app.get('/sync/status', (req, res) => {
    try {
      const deviceId = req.query.deviceId as string;
      const pairingCode = req.query.pairingCode as string;

      if (!deviceId || !pairingCode) {
        return res.status(400).json({ error: 'deviceId and pairingCode required' });
      }

      if (!verifyDevice(deviceId, pairingCode)) {
        return res.status(403).json({ error: 'Invalid device or pairing code' });
      }

      const stateStmt = db.prepare('SELECT * FROM sync_state WHERE device_id = ?');
      stateStmt.bind([deviceId]);
      const hasState = stateStmt.step();
      const state = hasState ? stateStmt.getAsObject() : { last_pull_at: 0, last_push_sequence: 0 };
      stateStmt.free();

      const countStmt = db.prepare('SELECT COUNT(*) as count FROM sync_blobs');
      countStmt.step();
      const count = countStmt.getAsObject();
      countStmt.free();

      const devStmt = db.prepare('SELECT id, name, last_seen_at FROM devices WHERE is_active = 1');
      const devices: any[] = [];
      while (devStmt.step()) {
        devices.push(devStmt.getAsObject());
      }
      devStmt.free();

      res.json({
        lastPullAt: (state as any).last_pull_at ?? 0,
        lastPushSequence: (state as any).last_push_sequence ?? 0,
        totalBlobsStored: (count as any).count ?? 0,
        pairedDevices: devices.map((d: any) => ({
          deviceId: d.id,
          name: d.name,
          lastSeenAt: d.last_seen_at,
          isCurrent: d.id === deviceId,
        })),
      });
    } catch (err: any) {
      console.error('Status error:', err);
      res.status(500).json({ error: 'Internal server error' });
    }
  });

  app.get('/health', (_req, res) => {
    res.json({ status: 'ok', timestamp: Date.now() });
  });
}

// --- Helpers ---

function verifyDevice(deviceId: string, pairingCode: string): boolean {
  const stmt = db.prepare(
    'SELECT id FROM devices WHERE id = ? AND pairing_code = ? AND is_active = 1'
  );
  stmt.bind([deviceId, pairingCode]);
  const hasRow = stmt.step(); // REQUIRED by sql.js — step() before reading
  stmt.free();
  return hasRow;
}

function generatePairingCode(): string {
  const chars = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789';
  let code = '';
  for (let i = 0; i < 6; i++) {
    code += chars[Math.floor(Math.random() * chars.length)];
  }
  return code.slice(0, 3) + '-' + code.slice(3, 6);
}

function initDb(sqlDb: Database) {
  sqlDb.run(`
    CREATE TABLE IF NOT EXISTS devices (
      id TEXT PRIMARY KEY,
      name TEXT NOT NULL,
      pairing_code TEXT UNIQUE NOT NULL,
      paired_at INTEGER NOT NULL,
      last_seen_at INTEGER NOT NULL,
      is_active INTEGER DEFAULT 1,
      public_key_fingerprint TEXT
    );

    CREATE TABLE IF NOT EXISTS sync_blobs (
      id TEXT PRIMARY KEY,
      device_id TEXT NOT NULL,
      encrypted_payload TEXT NOT NULL,
      blob_hash TEXT NOT NULL,
      created_at INTEGER NOT NULL,
      sequence_number INTEGER NOT NULL
    );

    CREATE TABLE IF NOT EXISTS sync_state (
      device_id TEXT PRIMARY KEY,
      last_pull_at INTEGER NOT NULL DEFAULT 0,
      last_push_sequence INTEGER NOT NULL DEFAULT 0
    );

    CREATE INDEX IF NOT EXISTS idx_blobs_device ON sync_blobs(device_id, sequence_number);
    CREATE INDEX IF NOT EXISTS idx_blobs_created ON sync_blobs(created_at);
    CREATE INDEX IF NOT EXISTS idx_devices_code ON devices(pairing_code);
  `);
}

// --- Start ---

async function startServer() {
  const SQL = await initSqlJs();
  if (existsSync(DB_PATH)) {
    const buffer = readFileSync(DB_PATH);
    db = new SQL.Database(buffer);
  } else {
    db = new SQL.Database();
  }
  initDb(db);
  saveDb();
  setupRoutes();

  app.listen(PORT, () => {
    console.log('╔══════════════════════════════════════╗');
    console.log('║    Nudge Sync Relay Server v0.1      ║');
    console.log('║    E2E Encrypted — Zero Plaintext    ║');
    console.log(`║    sql.js WASM — No native deps      ║`);
    console.log(`║    Listening on port ${PORT}             ║`);
    console.log('╚══════════════════════════════════════╝');
  });
}

startServer().catch((err) => {
  console.error('Failed to start server:', err);
  process.exit(1);
});
