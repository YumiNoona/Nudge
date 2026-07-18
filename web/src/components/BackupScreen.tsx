import { useState, useRef } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { db, generateId } from '../lib/db';
import type {
  Transaction,
  Account,
  Category,
  Budget,
  GamificationProfile,
  MerchantAlias,
} from '../lib/db';
import {
  initEncryption,
  encrypt,
  decrypt,
  isEncryptionReady,
} from '../lib/crypto';

interface Props {
  onBack: () => void;
}

interface BackupData {
  version: number;
  exportedAt: number;
  transactions: Transaction[];
  accounts: Account[];
  categories: Category[];
  budgets: Budget[];
  gamificationProfile: GamificationProfile | null;
  merchantAliases: MerchantAlias[];
}

interface ToastState {
  message: string;
  type: 'success' | 'error';
}

function formatDate(epoch: number): string {
  const d = new Date(epoch);
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  return `${y}-${m}-${day}`;
}

function downloadBlob(blob: Blob, filename: string) {
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = filename;
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
  URL.revokeObjectURL(url);
}

export default function BackupScreen({ onBack }: Props) {
  // Export state
  const [exporting, setExporting] = useState(false);
  const [exportPassphrase, setExportPassphrase] = useState('');
  const [exportConfirmPassphrase, setExportConfirmPassphrase] = useState('');
  const [showExportEncrypted, setShowExportEncrypted] = useState(false);

  // Import state
  const [importing, setImporting] = useState(false);
  const [importFile, setImportFile] = useState<File | null>(null);
  const [importPassphrase, setImportPassphrase] = useState('');
  const [importPreview, setImportPreview] = useState<BackupData | null>(null);
  const [importEncrypted, setImportEncrypted] = useState(false);

  // Reset state
  const [showResetConfirm, setShowResetConfirm] = useState(false);
  const [resetInput, setResetInput] = useState('');
  const [resetting, setResetting] = useState(false);

  // Toast
  const [toast, setToast] = useState<ToastState | null>(null);
  const importFileRef = useRef<HTMLInputElement>(null);

  const showToast = (message: string, type: 'success' | 'error') => {
    setToast({ message, type });
    setTimeout(() => setToast(null), 4000);
  };

  // --- Export helpers ---

  const fetchAllData = async (): Promise<BackupData> => {
    const [transactions, accounts, categories, budgets, gamificationProfile, merchantAliases] =
      await Promise.all([
        db.transactions.toArray(),
        db.accounts.toArray(),
        db.categories.toArray(),
        db.budgets.toArray(),
        db.gamificationProfile.toCollection().first(),
        db.merchantAliases.toArray(),
      ]);

    return {
      version: 1,
      exportedAt: Date.now(),
      transactions,
      accounts,
      categories,
      budgets,
      gamificationProfile: gamificationProfile ?? null,
      merchantAliases,
    };
  };

  const handleStandardExport = async () => {
    setExporting(true);
    try {
      const data = await fetchAllData();
      const json = JSON.stringify(data, null, 2);
      const blob = new Blob([json], { type: 'application/json' });
      downloadBlob(blob, `nudge-backup-${formatDate(Date.now())}.json`);
      showToast('Backup exported successfully', 'success');
    } catch {
      showToast('Export failed', 'error');
    } finally {
      setExporting(false);
    }
  };

  const handleEncryptedExport = async () => {
    if (exportPassphrase !== exportConfirmPassphrase) {
      showToast('Passphrases do not match', 'error');
      return;
    }
    if (exportPassphrase.length < 4) {
      showToast('Passphrase must be at least 4 characters', 'error');
      return;
    }

    setExporting(true);
    try {
      await initEncryption(exportPassphrase);
      const data = await fetchAllData();
      const json = JSON.stringify(data, null, 2);
      const encrypted = await encrypt(json);
      const blob = new Blob([encrypted], { type: 'application/octet-stream' });
      downloadBlob(blob, `nudge-backup-${formatDate(Date.now())}.enc`);
      setShowExportEncrypted(false);
      setExportPassphrase('');
      setExportConfirmPassphrase('');
      showToast('Encrypted backup exported successfully', 'success');
    } catch {
      showToast('Encrypted export failed', 'error');
    } finally {
      setExporting(false);
    }
  };

  // --- Import helpers ---

  const handleImportFileSelect = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    setImportFile(file);
    setImportPreview(null);
    setImportEncrypted(false);

    const isEnc = file.name.endsWith('.enc');

    if (isEnc) {
      setImportEncrypted(true);
      setImportPassphrase('');
      return;
    }

    const reader = new FileReader();
    reader.onload = (ev) => {
      try {
        const data = JSON.parse(ev.target?.result as string) as BackupData;
        setImportPreview(data);
      } catch {
        showToast('Invalid backup file', 'error');
        setImportFile(null);
      }
    };
    reader.readAsText(file);
  };

  const handleImportDecrypt = async () => {
    if (!importFile || !importPassphrase) return;

    setImporting(true);
    try {
      await initEncryption(importPassphrase);
      const reader = new FileReader();
      reader.onload = async (ev) => {
        try {
          const encryptedText = ev.target?.result as string;
          const decrypted = await decrypt(encryptedText);
          const data = JSON.parse(decrypted) as BackupData;
          setImportPreview(data);
          setImportEncrypted(false);
        } catch {
          showToast('Incorrect passphrase or corrupted file', 'error');
          setImportFile(null);
          setImportPassphrase('');
        } finally {
          setImporting(false);
        }
      };
      reader.readAsText(importFile);
    } catch {
      showToast('Decryption failed', 'error');
      setImporting(false);
    }
  };

  const handleImportExecute = async () => {
    if (!importPreview) return;

    setImporting(true);
    try {
      if (importPreview.categories.length > 0) {
        await db.categories.bulkAdd(importPreview.categories);
      }
      if (importPreview.accounts.length > 0) {
        await db.accounts.bulkAdd(importPreview.accounts);
      }
      if (importPreview.budgets.length > 0) {
        await db.budgets.bulkAdd(importPreview.budgets);
      }
      if (importPreview.transactions.length > 0) {
        await db.transactions.bulkAdd(importPreview.transactions);
      }
      if (importPreview.merchantAliases.length > 0) {
        await db.merchantAliases.bulkAdd(importPreview.merchantAliases);
      }
      if (importPreview.gamificationProfile) {
        const existing = await db.gamificationProfile.toCollection().first();
        if (!existing) {
          await db.gamificationProfile.add(importPreview.gamificationProfile);
        }
      }

      showToast('Data imported successfully', 'success');
      setImportFile(null);
      setImportPreview(null);
      setImportPassphrase('');
      if (importFileRef.current) importFileRef.current.value = '';
    } catch {
      showToast('Import failed — some items may already exist', 'error');
    } finally {
      setImporting(false);
    }
  };

  // --- Reset helpers ---

  const handleReset = async () => {
    if (resetInput !== 'DELETE') return;

    setResetting(true);
    try {
      await Promise.all([
        db.transactions.clear(),
        db.accounts.clear(),
        db.categories.clear(),
        db.budgets.clear(),
        db.gamificationProfile.clear(),
        db.merchantAliases.clear(),
      ]);
      showToast('All data has been deleted', 'success');
      setShowResetConfirm(false);
      setResetInput('');
    } catch {
      showToast('Failed to delete data', 'error');
    } finally {
      setResetting(false);
    }
  };

  return (
    <div className="min-h-screen bg-surface-base">
      <div className="max-w-2xl mx-auto p-4">
        {/* Header */}
        <div className="flex items-center justify-between mb-6">
          <button
            onClick={onBack}
            className="text-content-secondary hover:text-content-primary text-body"
          >
            ← Back
          </button>
          <h1 className="text-title font-bold text-content-primary">Backup & Restore</h1>
          <div className="w-14" />
        </div>

        {/* Toast */}
        <AnimatePresence>
          {toast && (
            <motion.div
              initial={{ opacity: 0, y: -10 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, y: -10 }}
              className="fixed top-4 left-1/2 -translate-x-1/2 z-50"
            >
              <div
                className={`px-6 py-3 rounded-xl text-caption font-medium shadow-lg ${
                  toast.type === 'success'
                    ? 'bg-positive text-white'
                    : 'bg-negative text-white'
                }`}
              >
                {toast.message}
              </div>
            </motion.div>
          )}
        </AnimatePresence>

        <div className="space-y-6">
          {/* --- Export Section --- */}
          <motion.section
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.05 }}
            className="p-6 bg-surface-raised rounded-xl"
            style={{ boxShadow: 'var(--shadow-sm)' }}
          >
            <h2 className="text-heading font-bold text-content-primary mb-1">Export Your Data</h2>
            <p className="text-caption text-content-secondary mb-6">
              Download a complete backup of all your transactions, categories, budgets, and settings. Your data, your control.
            </p>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              {/* Standard Export */}
              <div
                className="p-5 rounded-xl border transition-colors hover:border-accent-primary/30"
                style={{
                  borderColor: 'var(--color-content-tertiary)',
                  backgroundColor: 'var(--color-surface-base)',
                }}
              >
                <h3 className="text-body font-semibold text-content-primary mb-1">Standard Export</h3>
                <p className="text-caption text-content-secondary mb-4">
                  Plain JSON file. All your data in a human-readable format.
                </p>
                <button
                  onClick={handleStandardExport}
                  disabled={exporting}
                  className="w-full py-2.5 bg-accent-primary text-white rounded-pill text-caption font-semibold hover:bg-accent-primary/90 disabled:opacity-50 transition-colors"
                >
                  {exporting ? 'Exporting...' : 'Export as JSON'}
                </button>
              </div>

              {/* Encrypted Export */}
              <div
                className="p-5 rounded-xl border transition-colors hover:border-accent-primary/30"
                style={{
                  borderColor: 'var(--color-content-tertiary)',
                  backgroundColor: 'var(--color-surface-base)',
                }}
              >
                <h3 className="text-body font-semibold text-content-primary mb-1">Encrypted Export</h3>
                <p className="text-caption text-content-secondary mb-4">
                  AES-GCM encrypted backup. Requires a passphrase to restore.
                </p>
                {!showExportEncrypted ? (
                  <button
                    onClick={() => setShowExportEncrypted(true)}
                    className="w-full py-2.5 bg-accent-primary text-white rounded-pill text-caption font-semibold hover:bg-accent-primary/90 transition-colors"
                  >
                    Export Encrypted
                  </button>
                ) : (
                  <motion.div
                    initial={{ opacity: 0, height: 0 }}
                    animate={{ opacity: 1, height: 'auto' }}
                    className="space-y-3"
                  >
                    <input
                      type="password"
                      placeholder="Enter passphrase"
                      value={exportPassphrase}
                      onChange={(e) => setExportPassphrase(e.target.value)}
                      className="w-full p-2.5 bg-surface-base rounded-md outline-none text-caption text-content-primary placeholder:text-content-tertiary border border-content-tertiary/20"
                    />
                    <input
                      type="password"
                      placeholder="Confirm passphrase"
                      value={exportConfirmPassphrase}
                      onChange={(e) => setExportConfirmPassphrase(e.target.value)}
                      className="w-full p-2.5 bg-surface-base rounded-md outline-none text-caption text-content-primary placeholder:text-content-tertiary border border-content-tertiary/20"
                    />
                    <p className="text-micro text-warning">
                      Keep your passphrase safe — without it, this backup cannot be restored
                    </p>
                    <div className="flex gap-2">
                      <button
                        onClick={() => {
                          setShowExportEncrypted(false);
                          setExportPassphrase('');
                          setExportConfirmPassphrase('');
                        }}
                        className="px-4 py-2 text-caption text-content-secondary hover:text-content-primary"
                      >
                        Cancel
                      </button>
                      <button
                        onClick={handleEncryptedExport}
                        disabled={
                          exporting ||
                          !exportPassphrase ||
                          exportPassphrase !== exportConfirmPassphrase
                        }
                        className="flex-1 py-2.5 bg-accent-primary text-white rounded-pill text-caption font-semibold hover:bg-accent-primary/90 disabled:opacity-50 transition-colors"
                      >
                        {exporting ? 'Exporting...' : 'Export Encrypted'}
                      </button>
                    </div>
                  </motion.div>
                )}
              </div>
            </div>
          </motion.section>

          {/* --- Import Section --- */}
          <motion.section
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.1 }}
            className="p-6 bg-surface-raised rounded-xl"
            style={{ boxShadow: 'var(--shadow-sm)' }}
          >
            <h2 className="text-heading font-bold text-content-primary mb-1">Import Data</h2>
            <p className="text-caption text-content-secondary mb-6">
              Restore from a previous backup. This will MERGE with your existing data.
            </p>

            {/* File input */}
            <div
              onClick={() => importFileRef.current?.click()}
              className="border-2 border-dashed rounded-xl p-8 text-center cursor-pointer transition-colors hover:border-accent-primary/40 mb-4"
              style={{ borderColor: 'var(--color-content-tertiary)' }}
            >
              {importFile ? (
                <div>
                  <p className="text-body font-medium text-content-primary">{importFile.name}</p>
                  <p className="text-micro text-content-tertiary mt-1">
                    {(importFile.size / 1024).toFixed(1)} KB
                  </p>
                </div>
              ) : (
                <div>
                  <span className="text-3xl block mb-2">📂</span>
                  <p className="text-caption text-content-secondary">Click to select a backup file</p>
                  <p className="text-micro text-content-tertiary mt-1">Accepts .json and .enc files</p>
                </div>
              )}
              <input
                ref={importFileRef}
                type="file"
                accept=".json,.enc"
                onChange={handleImportFileSelect}
                className="hidden"
              />
            </div>

            {/* Encrypted import passphrase */}
            <AnimatePresence>
              {importEncrypted && importFile && !importPreview && (
                <motion.div
                  initial={{ opacity: 0, height: 0 }}
                  animate={{ opacity: 1, height: 'auto' }}
                  exit={{ opacity: 0, height: 0 }}
                  className="space-y-3 mb-4"
                >
                  <p className="text-caption text-content-secondary">
                    This backup is encrypted. Enter your passphrase to decrypt.
                  </p>
                  <input
                    type="password"
                    placeholder="Enter passphrase"
                    value={importPassphrase}
                    onChange={(e) => setImportPassphrase(e.target.value)}
                    className="w-full p-3 bg-surface-base rounded-md outline-none text-body text-content-primary placeholder:text-content-tertiary border border-content-tertiary/20"
                    onKeyDown={(e) => {
                      if (e.key === 'Enter') handleImportDecrypt();
                    }}
                  />
                  <button
                    onClick={handleImportDecrypt}
                    disabled={importing || !importPassphrase}
                    className="w-full py-2.5 bg-accent-primary text-white rounded-pill text-caption font-semibold hover:bg-accent-primary/90 disabled:opacity-50 transition-colors"
                  >
                    {importing ? 'Decrypting...' : 'Decrypt & Preview'}
                  </button>
                </motion.div>
              )}
            </AnimatePresence>

            {/* Import preview */}
            <AnimatePresence>
              {importPreview && !importEncrypted && (
                <motion.div
                  initial={{ opacity: 0, y: 10 }}
                  animate={{ opacity: 1, y: 0 }}
                  className="space-y-4"
                >
                  <div className="p-4 bg-surface-base rounded-lg">
                    <p className="text-caption font-semibold text-content-primary mb-2">
                      Preview — the following will be imported:
                    </p>
                    <div className="grid grid-cols-2 gap-2 text-caption">
                      {importPreview.transactions.length > 0 && (
                        <span className="text-content-secondary">
                          {importPreview.transactions.length} transactions
                        </span>
                      )}
                      {importPreview.accounts.length > 0 && (
                        <span className="text-content-secondary">
                          {importPreview.accounts.length} accounts
                        </span>
                      )}
                      {importPreview.categories.length > 0 && (
                        <span className="text-content-secondary">
                          {importPreview.categories.length} categories
                        </span>
                      )}
                      {importPreview.budgets.length > 0 && (
                        <span className="text-content-secondary">
                          {importPreview.budgets.length} budgets
                        </span>
                      )}
                      {importPreview.merchantAliases.length > 0 && (
                        <span className="text-content-secondary">
                          {importPreview.merchantAliases.length} merchant aliases
                        </span>
                      )}
                    </div>
                  </div>

                  <p className="text-micro text-warning">
                    Existing data will not be deleted. Duplicates may occur if importing the same data twice.
                  </p>

                  <div className="flex gap-2">
                    <button
                      onClick={() => {
                        setImportFile(null);
                        setImportPreview(null);
                        if (importFileRef.current) importFileRef.current.value = '';
                      }}
                      className="px-6 py-3 text-caption text-content-secondary hover:text-content-primary"
                    >
                      Cancel
                    </button>
                    <button
                      onClick={handleImportExecute}
                      disabled={importing}
                      className="flex-1 py-3 bg-accent-primary text-white rounded-pill text-caption font-semibold hover:bg-accent-primary/90 disabled:opacity-50 transition-colors"
                    >
                      {importing ? 'Importing...' : 'Import'}
                    </button>
                  </div>
                </motion.div>
              )}
            </AnimatePresence>
          </motion.section>

          {/* --- Reset Section --- */}
          <motion.section
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.15 }}
            className="p-6 rounded-xl border-2"
            style={{
              borderColor: 'var(--color-negative)',
              backgroundColor: 'var(--color-surface-raised)',
              boxShadow: 'var(--shadow-sm)',
            }}
          >
            <h2
              className="text-heading font-bold mb-1"
              style={{ color: 'var(--color-negative)' }}
            >
              Delete All Data
            </h2>
            <p className="text-caption text-content-secondary mb-4">
              This will permanently delete ALL your data including transactions, categories, accounts, budgets, and settings.
            </p>

            {!showResetConfirm ? (
              <button
                onClick={() => setShowResetConfirm(true)}
                className="px-6 py-2.5 text-caption font-semibold rounded-pill transition-colors"
                style={{
                  backgroundColor: 'var(--color-negative)',
                  color: 'white',
                }}
              >
                Delete Everything
              </button>
            ) : (
              <motion.div
                initial={{ opacity: 0, height: 0 }}
                animate={{ opacity: 1, height: 'auto' }}
                className="space-y-3"
              >
                <p className="text-caption font-semibold" style={{ color: 'var(--color-negative)' }}>
                  This cannot be undone. Type DELETE to confirm.
                </p>
                <input
                  type="text"
                  placeholder='Type "DELETE"'
                  value={resetInput}
                  onChange={(e) => setResetInput(e.target.value)}
                  className="w-full p-3 bg-surface-base rounded-md outline-none text-body text-content-primary placeholder:text-content-tertiary border"
                  style={{ borderColor: 'var(--color-negative)' }}
                />
                <div className="flex gap-2">
                  <button
                    onClick={() => {
                      setShowResetConfirm(false);
                      setResetInput('');
                    }}
                    className="px-4 py-2 text-caption text-content-secondary hover:text-content-primary"
                  >
                    Cancel
                  </button>
                  <button
                    onClick={handleReset}
                    disabled={resetInput !== 'DELETE' || resetting}
                    className="px-6 py-2 rounded-pill text-caption font-semibold transition-colors disabled:opacity-50"
                    style={{
                      backgroundColor:
                        resetInput === 'DELETE'
                          ? 'var(--color-negative)'
                          : 'var(--color-content-tertiary)',
                      color: 'white',
                    }}
                  >
                    {resetting ? 'Deleting...' : 'Permanently Delete All Data'}
                  </button>
                </div>
              </motion.div>
            )}
          </motion.section>
        </div>
      </div>
    </div>
  );
}
