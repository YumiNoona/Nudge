import { db } from './db';
import type {
  Transaction,
  Account,
  Category,
  Budget,
  GamificationProfile,
  MerchantAlias,
} from './db';
import {
  initEncryption,
  encrypt,
  decrypt,
} from './crypto';

const ALGORITHM = { name: 'AES-GCM' as const, length: 256 as const };
const SYNC_SALT_LENGTH = 16;
const SYNC_IV_LENGTH = 12;
const SYNC_ITERATIONS = 100_000;
const CONFIG_KEY = 'nudge_sync_config';
const KNOWN_VERSIONS_KEY = 'nudge_sync_known_versions';
const SYNC_SALT_KEY = 'nudge_sync_key_salt';
const PROTOCOL_VERSION = 1;

export interface SyncConfig {
  deviceId: string;
  pairingCode: string;
  serverUrl: string;
  lastPullSequence: number;
  lastPushSequence: number;
  isEnabled: boolean;
  lastSyncAt: number;
}

export interface SyncRecord {
  id: string;
  table: 'transactions' | 'accounts' | 'categories' | 'budgets' | 'gamification' | 'recurring_rules' | 'merchant_aliases';
  action: 'create' | 'update' | 'delete';
  data: string;
  last_modified_at: number;
  device_id: string;
  version: number;
}

export interface SyncBlob {
  device_id: string;
  records: SyncRecord[];
  sync_timestamp: number;
  protocol_version: number;
}

interface SyncLogEntry {
  timestamp: number;
  type: 'push' | 'pull' | 'full';
  recordCount: number;
}

function bufferToBase64(buffer: ArrayBuffer): string {
  const bytes = new Uint8Array(buffer);
  let binary = '';
  for (let i = 0; i < bytes.byteLength; i++) {
    binary += String.fromCharCode(bytes[i]);
  }
  return btoa(binary);
}

function base64ToBuffer(base64: string): ArrayBuffer {
  const binary = atob(base64);
  const bytes = new Uint8Array(binary.length);
  for (let i = 0; i < binary.length; i++) {
    bytes[i] = binary.charCodeAt(i);
  }
  return bytes.buffer as ArrayBuffer;
}

async function deriveSyncKey(keyMaterial: string): Promise<CryptoKey> {
  const encoder = new TextEncoder();
  let salt: Uint8Array;
  const storedSalt = localStorage.getItem(SYNC_SALT_KEY);
  if (storedSalt) {
    salt = new Uint8Array(base64ToBuffer(storedSalt));
  } else {
    salt = crypto.getRandomValues(new Uint8Array(SYNC_SALT_LENGTH));
    localStorage.setItem(SYNC_SALT_KEY, bufferToBase64(salt.buffer as ArrayBuffer));
  }

  const keyMaterialObj = await crypto.subtle.importKey(
    'raw',
    encoder.encode(keyMaterial),
    'PBKDF2',
    false,
    ['deriveKey']
  );

  return crypto.subtle.deriveKey(
    { name: 'PBKDF2', salt: salt.buffer as ArrayBuffer, iterations: SYNC_ITERATIONS, hash: 'SHA-256' },
    keyMaterialObj,
    ALGORITHM,
    false,
    ['encrypt', 'decrypt']
  );
}

let cachedSyncKey: CryptoKey | null = null;

async function getSyncKey(pairingCode: string): Promise<CryptoKey> {
  if (!cachedSyncKey) {
    cachedSyncKey = await deriveSyncKey(pairingCode);
  }
  return cachedSyncKey;
}

export function clearSyncKey(): void {
  cachedSyncKey = null;
}

export function loadSyncConfig(): SyncConfig | null {
  const raw = localStorage.getItem(CONFIG_KEY);
  if (!raw) return null;
  try {
    return JSON.parse(raw) as SyncConfig;
  } catch {
    return null;
  }
}

export function saveSyncConfig(config: SyncConfig): void {
  localStorage.setItem(CONFIG_KEY, JSON.stringify(config));
}

function clearSyncConfig(): void {
  localStorage.removeItem(CONFIG_KEY);
  localStorage.removeItem(KNOWN_VERSIONS_KEY);
  localStorage.removeItem(SYNC_SALT_KEY);
  clearSyncKey();
}

function getKnownVersions(): Record<string, number> {
  const raw = localStorage.getItem(KNOWN_VERSIONS_KEY);
  if (!raw) return {};
  try {
    return JSON.parse(raw) as Record<string, number>;
  } catch {
    return {};
  }
}

function saveKnownVersions(versions: Record<string, number>): void {
  localStorage.setItem(KNOWN_VERSIONS_KEY, JSON.stringify(versions));
}

function getSyncLog(): SyncLogEntry[] {
  const raw = localStorage.getItem('nudge_sync_log');
  if (!raw) return [];
  try {
    return JSON.parse(raw) as SyncLogEntry[];
  } catch {
    return [];
  }
}

function appendSyncLog(entry: SyncLogEntry): void {
  const log = getSyncLog();
  log.unshift(entry);
  localStorage.setItem('nudge_sync_log', JSON.stringify(log.slice(0, 20)));
}

async function sha256Hash(data: string): Promise<string> {
  const encoder = new TextEncoder();
  const hashBuffer = await crypto.subtle.digest('SHA-256', encoder.encode(data));
  return bufferToBase64(hashBuffer);
}

const TABLE_MAP: Record<string, string> = {
  transactions: 'transactions',
  accounts: 'accounts',
  categories: 'categories',
  budgets: 'budgets',
  gamification: 'gamificationProfile',
  merchant_aliases: 'merchantAliases',
};

function getDbTable(tableKey: string) {
  const dbTable = TABLE_MAP[tableKey];
  if (!dbTable) return null;
  return (db as any)[dbTable] || null;
}

export async function registerDevice(serverUrl: string, deviceName: string): Promise<SyncConfig> {
  const res = await fetch(`${serverUrl}/sync/register`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ device_name: deviceName }),
  });
  if (!res.ok) {
    const text = await res.text();
    throw new Error(`Registration failed: ${text}`);
  }
  const data = await res.json();
  const config: SyncConfig = {
    deviceId: data.device_id,
    pairingCode: data.pairing_code,
    serverUrl,
    lastPullSequence: 0,
    lastPushSequence: 0,
    isEnabled: true,
    lastSyncAt: 0,
  };
  saveSyncConfig(config);
  clearSyncKey();
  return config;
}

export async function pairDevice(serverUrl: string, pairingCode: string, deviceName: string): Promise<SyncConfig> {
  const res = await fetch(`${serverUrl}/sync/pair`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ pairing_code: pairingCode, device_name: deviceName }),
  });
  if (!res.ok) {
    const text = await res.text();
    throw new Error(`Pairing failed: ${text}`);
  }
  const data = await res.json();
  const config: SyncConfig = {
    deviceId: data.device_id,
    pairingCode: data.pairing_code,
    serverUrl,
    lastPullSequence: 0,
    lastPushSequence: 0,
    isEnabled: true,
    lastSyncAt: 0,
  };
  saveSyncConfig(config);
  clearSyncKey();
  return config;
}

export async function getSyncStatus(config: SyncConfig): Promise<any> {
  const url = `${config.serverUrl}/sync/status?device_id=${encodeURIComponent(config.deviceId)}`;
  const res = await fetch(url);
  if (!res.ok) {
    const text = await res.text();
    throw new Error(`Status fetch failed: ${text}`);
  }
  return res.json();
}

async function gatherLocalRecords(config: SyncConfig): Promise<SyncRecord[]> {
  const records: SyncRecord[] = [];
  const lastSyncAt = config.lastSyncAt || 0;

  const transactions = await db.transactions
    .filter((t) => t.updatedAt > lastSyncAt)
    .toArray();
  for (const t of transactions) {
    records.push({
      id: t.id,
      table: 'transactions',
      action: t.createdAt === t.updatedAt ? 'create' : 'update',
      data: JSON.stringify(t),
      last_modified_at: t.updatedAt,
      device_id: config.deviceId,
      version: Date.now(),
    });
  }

  const accounts = await db.accounts.toArray();
  for (const a of accounts) {
    records.push({
      id: a.id,
      table: 'accounts',
      action: 'update',
      data: JSON.stringify(a),
      last_modified_at: lastSyncAt + records.length,
      device_id: config.deviceId,
      version: Date.now(),
    });
  }

  const categories = await db.categories.toArray();
  for (const c of categories) {
    records.push({
      id: c.id,
      table: 'categories',
      action: 'update',
      data: JSON.stringify(c),
      last_modified_at: lastSyncAt + records.length,
      device_id: config.deviceId,
      version: Date.now(),
    });
  }

  const budgets = await db.budgets.toArray();
  for (const b of budgets) {
    records.push({
      id: b.id,
      table: 'budgets',
      action: 'update',
      data: JSON.stringify(b),
      last_modified_at: lastSyncAt + records.length,
      device_id: config.deviceId,
      version: Date.now(),
    });
  }

  const gamification = await db.gamificationProfile.toCollection().first();
  if (gamification) {
    records.push({
      id: gamification.userId,
      table: 'gamification',
      action: 'update',
      data: JSON.stringify(gamification),
      last_modified_at: lastSyncAt + records.length,
      device_id: config.deviceId,
      version: Date.now(),
    });
  }

  const merchantAliases = await db.merchantAliases.toArray();
  for (const m of merchantAliases) {
    records.push({
      id: m.id,
      table: 'merchant_aliases',
      action: 'update',
      data: JSON.stringify(m),
      last_modified_at: m.createdAt,
      device_id: config.deviceId,
      version: Date.now(),
    });
  }

  return records;
}

async function encryptSyncBlob(blob: SyncBlob, pairingCode: string): Promise<string> {
  const key = await getSyncKey(pairingCode);
  const iv = crypto.getRandomValues(new Uint8Array(SYNC_IV_LENGTH));
  const encoder = new TextEncoder();
  const plaintext = JSON.stringify(blob);
  const ciphertext = await crypto.subtle.encrypt(
    { name: 'AES-GCM', iv },
    key,
    encoder.encode(plaintext)
  );
  const combined = new Uint8Array(iv.length + ciphertext.byteLength);
  combined.set(iv);
  combined.set(new Uint8Array(ciphertext), iv.length);
  return bufferToBase64(combined.buffer as ArrayBuffer);
}

async function decryptSyncBlob(encrypted: string, pairingCode: string): Promise<SyncBlob> {
  const key = await getSyncKey(pairingCode);
  const data = new Uint8Array(base64ToBuffer(encrypted));
  const iv = data.slice(0, SYNC_IV_LENGTH);
  const ciphertext = data.slice(SYNC_IV_LENGTH);
  const decrypted = await crypto.subtle.decrypt(
    { name: 'AES-GCM', iv },
    key,
    ciphertext
  );
  const decoder = new TextDecoder();
  const json = decoder.decode(decrypted);
  return JSON.parse(json) as SyncBlob;
}

export async function pushChanges(config: SyncConfig): Promise<number> {
  const records = await gatherLocalRecords(config);
  if (records.length === 0) return 0;

  const blob: SyncBlob = {
    device_id: config.deviceId,
    records,
    sync_timestamp: Date.now(),
    protocol_version: PROTOCOL_VERSION,
  };

  const encrypted = await encryptSyncBlob(blob, config.pairingCode);

  const res = await fetch(`${config.serverUrl}/sync/push`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      device_id: config.deviceId,
      blob: encrypted,
      sync_timestamp: blob.sync_timestamp,
    }),
  });
  if (!res.ok) {
    const text = await res.text();
    throw new Error(`Push failed: ${text}`);
  }
  const data = await res.json();

  const updatedConfig = { ...config, lastPushSequence: data.sequence ?? config.lastPushSequence, lastSyncAt: Date.now() };
  saveSyncConfig(updatedConfig);

  appendSyncLog({ timestamp: Date.now(), type: 'push', recordCount: records.length });
  return records.length;
}

async function applyRecords(records: SyncRecord[]): Promise<void> {
  const knownVersions = getKnownVersions();

  for (const record of records) {
    const versionKey = `${record.table}:${record.id}`;
    if (knownVersions[versionKey] && knownVersions[versionKey] >= record.version) {
      continue;
    }

    const parsed = JSON.parse(record.data);

    if (record.action === 'delete') {
      const dbTable = getDbTable(record.table);
      if (dbTable) {
        try {
          await dbTable.delete(record.id);
        } catch {
          // record may not exist locally
        }
      }
    } else {
      switch (record.table) {
        case 'transactions':
          await db.transactions.put(parsed as Transaction);
          break;
        case 'accounts':
          await db.accounts.put(parsed as Account);
          break;
        case 'categories':
          await db.categories.put(parsed as Category);
          break;
        case 'budgets':
          await db.budgets.put(parsed as Budget);
          break;
        case 'gamification':
          await db.gamificationProfile.put(parsed as GamificationProfile);
          break;
        case 'merchant_aliases':
          await db.merchantAliases.put(parsed as MerchantAlias);
          break;
        case 'recurring_rules':
          break;
      }
    }

    knownVersions[versionKey] = record.version;
  }

  saveKnownVersions(knownVersions);
}

export async function pullChanges(config: SyncConfig): Promise<number> {
  const res = await fetch(`${config.serverUrl}/sync/pull`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      device_id: config.deviceId,
      last_pull_sequence: config.lastPullSequence,
    }),
  });
  if (!res.ok) {
    const text = await res.text();
    throw new Error(`Pull failed: ${text}`);
  }
  const data = await res.json();
  const blobs: Array<{ blob: string; sequence: number }> = data.blobs || [];

  let totalRecords = 0;

  for (const { blob: encryptedBlob, sequence } of blobs) {
    if (sequence <= config.lastPullSequence) continue;

    let syncBlob: SyncBlob;
    try {
      syncBlob = await decryptSyncBlob(encryptedBlob, config.pairingCode);
    } catch {
      continue;
    }

    if (syncBlob.device_id === config.deviceId) continue;

    if (syncBlob.records && syncBlob.records.length > 0) {
      await applyRecords(syncBlob.records);
      totalRecords += syncBlob.records.length;
    }

    config.lastPullSequence = sequence;
  }

  const updatedConfig = { ...config, lastSyncAt: Date.now() };
  saveSyncConfig(updatedConfig);

  appendSyncLog({ timestamp: Date.now(), type: 'pull', recordCount: totalRecords });
  return totalRecords;
}

export async function fullSync(config: SyncConfig): Promise<{ pushed: number; pulled: number }> {
  const pushed = await pushChanges(config);
  const refreshedConfig = loadSyncConfig() || config;
  const pulled = await pullChanges(refreshedConfig);

  appendSyncLog({ timestamp: Date.now(), type: 'full', recordCount: pushed + pulled });
  return { pushed, pulled };
}

export function getSyncEventLog(): SyncLogEntry[] {
  return getSyncLog();
}

export function disconnectSync(): void {
  clearSyncConfig();
  clearSyncKey();
}
