/**
 * WebCrypto-based field-level encryption for Web IndexedDB.
 * Encrypts money amounts and merchant text at the field level
 * since IndexedDB isn't encrypted at rest by browsers.
 *
 * Uses PBKDF2 key derivation from a passphrase + WebCrypto AES-GCM.
 * Per §8: "Web IndexedDB encrypted at the field level (money amounts + merchant text)
 * using a passphrase-derived key"
 */

const ALGORITHM = { name: 'AES-GCM', length: 256 };
const SALT_LENGTH = 16;
const IV_LENGTH = 12;
const ITERATIONS = 100_000;

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

async function deriveKey(passphrase: string, salt: Uint8Array): Promise<CryptoKey> {
  const encoder = new TextEncoder();
  const keyMaterial = await crypto.subtle.importKey(
    'raw',
    encoder.encode(passphrase),
    'PBKDF2',
    false,
    ['deriveKey']
  );

  return crypto.subtle.deriveKey(
    { name: 'PBKDF2', salt: salt.buffer as ArrayBuffer, iterations: ITERATIONS, hash: 'SHA-256' },
    keyMaterial,
    ALGORITHM,
    false,
    ['encrypt', 'decrypt']
  );
}

let cachedKey: CryptoKey | null = null;
let cachedSalt: Uint8Array | null = null;

/**
 * Initialize or retrieve the encryption key from a passphrase.
 * Call once when the user provides their passphrase (e.g., on app unlock).
 */
export async function initEncryption(passphrase: string): Promise<void> {
  // Try to load existing salt from localStorage
  const storedSalt = localStorage.getItem('nudge_enc_salt');
  if (storedSalt) {
    cachedSalt = new Uint8Array(base64ToBuffer(storedSalt));
  } else {
    cachedSalt = crypto.getRandomValues(new Uint8Array(SALT_LENGTH));
    localStorage.setItem('nudge_enc_salt', bufferToBase64(cachedSalt.buffer as ArrayBuffer));
  }
  cachedKey = await deriveKey(passphrase, cachedSalt);
}

/**
 * Check if encryption has been initialized
 */
export async function isEncryptionReady(): Promise<boolean> {
  return localStorage.getItem('nudge_enc_salt') !== null;
}

/**
 * Encrypt a string value. Returns base64-encoded ciphertext with IV prepended.
 */
export async function encrypt(plaintext: string): Promise<string> {
  if (!cachedKey) throw new Error('Encryption not initialized. Call initEncryption() first.');

  const iv = crypto.getRandomValues(new Uint8Array(IV_LENGTH));
  const encoder = new TextEncoder();
  const ciphertext = await crypto.subtle.encrypt(
    { name: 'AES-GCM', iv },
    cachedKey,
    encoder.encode(plaintext)
  );

  // Prepend IV to ciphertext
  const combined = new Uint8Array(iv.length + ciphertext.byteLength);
  combined.set(iv);
  combined.set(new Uint8Array(ciphertext), iv.length);

  return bufferToBase64(combined.buffer as ArrayBuffer);
}

/**
 * Decrypt a base64 value (with IV prepended).
 */
export async function decrypt(encoded: string): Promise<string> {
  if (!cachedKey) throw new Error('Encryption not initialized. Call initEncryption() first.');

  const data = new Uint8Array(base64ToBuffer(encoded));
  const iv = data.slice(0, IV_LENGTH);
  const ciphertext = data.slice(IV_LENGTH);

  const decrypted = await crypto.subtle.decrypt(
    { name: 'AES-GCM', iv },
    cachedKey,
    ciphertext
  );

  const decoder = new TextDecoder();
  return decoder.decode(decrypted);
}

/**
 * Generate a secure random passphrase for first-time setup
 */
export function generatePassphrase(): string {
  const chars = 'abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789';
  let result = '';
  const values = crypto.getRandomValues(new Uint8Array(32));
  for (let i = 0; i < 32; i++) {
    result += chars[values[i] % chars.length];
  }
  return result;
}

/**
 * Clear cached key (e.g., on logout/lock)
 */
export function clearEncryption(): void {
  cachedKey = null;
}
