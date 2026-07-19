/**
 * Field-level encryption layer for IndexedDB per §8.
 *
 * Encrypts all text fields containing PII before writes:
 *   merchantRaw, merchantNormalized, note, sourceRawText
 *
 * amountCents is stored as a plain number — without the merchant name (which IS
 * encrypted), the amount alone carries no identifying information.
 *
 * Uses WebCrypto AES-256-GCM with a per-session key stored in sessionStorage.
 * For persistent encryption across sessions, pass a user key to initSecureStorage().
 *
 * Usage (drop-in replacement for direct Dexie calls):
 *   import { secureGetAll, securePut, secureUpdate, secureDelete } from './secureDb'
 */

import { db } from './db';
import type { Transaction } from './db';
import { initEncryption, encrypt, decrypt, isEncryptionReady, generatePassphrase } from './crypto';

let ready = false;

export async function initSecureStorage(passphrase?: string): Promise<void> {
  if (ready) return;
  const key = passphrase ?? sessionStorage.getItem('nudge_ekey') ?? generatePassphrase();
  if (!passphrase) sessionStorage.setItem('nudge_ekey', key);

  const alreadySetup = await isEncryptionReady();
  if (!alreadySetup) await initEncryption(key);

  ready = true;
}

function guard() { if (!ready) throw new Error('Call initSecureStorage() first'); }

async function enc(s: string): Promise<string> { guard(); return encrypt(s); }
async function dec(s: string): Promise<string> { guard(); return decrypt(s); }

const SENSITIVE_FIELDS = ['merchantRaw', 'merchantNormalized', 'note', 'sourceRawText'] as const;

// Heuristic: ciphertext from our encrypt() is > 40 chars base64. Plaintext merchant
// names and notes are rarely that long. Use length to distinguish.
function isEncrypted(val: string | null | undefined): boolean {
  return !!val && val.length > 40;
}

async function encryptTx(t: Transaction): Promise<Transaction> {
  return {
    ...t,
    merchantRaw:          t.merchantRaw          ? await enc(t.merchantRaw)          : t.merchantRaw,
    merchantNormalized:   t.merchantNormalized   ? await enc(t.merchantNormalized)   : t.merchantNormalized,
    note:                 t.note                 ? await enc(t.note)                 : t.note,
    sourceRawText:        t.sourceRawText        ? await enc(t.sourceRawText)        : t.sourceRawText,
  };
}

async function decryptTx(t: Transaction): Promise<Transaction> {
  try {
    return {
      ...t,
      merchantRaw:        isEncrypted(t.merchantRaw)        ? await dec(t.merchantRaw!)        : t.merchantRaw,
      merchantNormalized: isEncrypted(t.merchantNormalized) ? await dec(t.merchantNormalized!) : t.merchantNormalized,
      note:               isEncrypted(t.note)               ? await dec(t.note!)               : t.note,
      sourceRawText:      isEncrypted(t.sourceRawText)      ? await dec(t.sourceRawText!)      : t.sourceRawText,
    };
  } catch {
    return t;
  }
}

// --- Public API ---

export async function securePut(txn: Transaction): Promise<void> {
  await db.transactions.put(await encryptTx(txn));
}

export async function secureGet(id: string): Promise<Transaction | undefined> {
  const t = await db.transactions.get(id);
  return t ? decryptTx(t) : undefined;
}

export async function secureGetAll(): Promise<Transaction[]> {
  const txns = await db.transactions.orderBy('timestampEpoch').reverse().toArray();
  return Promise.all(txns.map(decryptTx));
}

export async function secureGetNeedsReview(): Promise<Transaction[]> {
  const txns = await db.transactions.filter(t => !t.isReviewed).toArray();
  return Promise.all(txns.map(decryptTx));
}

export async function secureUpdate(id: string, changes: Partial<Transaction>): Promise<void> {
  const existing = await db.transactions.get(id);
  if (!existing) return;
  const merged = { ...existing, ...changes, updatedAt: Date.now() };
  await db.transactions.put(await encryptTx(merged));
}

export async function secureDelete(id: string): Promise<void> {
  await db.transactions.delete(id);
}
