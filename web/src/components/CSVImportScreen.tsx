import { useState, useRef, useCallback } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import type { Category, Account } from '../lib/db';

interface ImportTransaction {
  merchantRaw: string;
  amountCents: number;
  type: 'debit' | 'credit';
  dateEpoch: number;
  categoryId: string | null;
  accountId: string;
  note: string | null;
}

interface Props {
  categories: Category[];
  accounts: Account[];
  onBack: () => void;
  onImport: (transactions: ImportTransaction[]) => void;
}

// --- CSV parsing helpers ---

function parseCSVLine(line: string): string[] {
  const fields: string[] = [];
  let current = '';
  let inQuotes = false;

  for (let i = 0; i < line.length; i++) {
    const ch = line[i];
    if (inQuotes) {
      if (ch === '"') {
        if (i + 1 < line.length && line[i + 1] === '"') {
          current += '"';
          i++;
        } else {
          inQuotes = false;
        }
      } else {
        current += ch;
      }
    } else {
      if (ch === '"') {
        inQuotes = true;
      } else if (ch === ',') {
        fields.push(current.trim());
        current = '';
      } else {
        current += ch;
      }
    }
  }
  fields.push(current.trim());
  return fields;
}

function detectColumns(headers: string[]): {
  amountIdx: number | null;
  dateIdx: number | null;
  merchantIdx: number | null;
  typeIdx: number | null;
} {
  const lower = headers.map((h) => h.toLowerCase().trim());

  const amountPatterns = ['amount', 'sum', 'value', 'price', 'amt'];
  const datePatterns = ['date', 'timestamp', 'time'];
  const merchantPatterns = ['merchant', 'description', 'name', 'payee', 'narrative', 'details'];
  const typePatterns = ['type', 'transaction_type', 'direction'];

  function findFirst(patterns: string[]): number | null {
    for (const pattern of patterns) {
      for (let i = 0; i < lower.length; i++) {
        if (lower[i].includes(pattern)) return i;
      }
    }
    return null;
  }

  return {
    amountIdx: findFirst(amountPatterns),
    dateIdx: findFirst(datePatterns),
    merchantIdx: findFirst(merchantPatterns),
    typeIdx: findFirst(typePatterns),
  };
}

function parseAmount(raw: string): { amountCents: number; isCredit: boolean } | null {
  let s = raw.trim();
  let isNegative = false;

  if (s.startsWith('(') && s.endsWith(')')) {
    isNegative = true;
    s = s.slice(1, -1).trim();
  }
  if (s.startsWith('-')) {
    isNegative = true;
    s = s.slice(1).trim();
  }

  s = s.replace(/[^\d.,\-]/g, '');

  const hasDot = s.includes('.');
  const hasComma = s.includes(',');

  if (hasDot && hasComma) {
    const lastDot = s.lastIndexOf('.');
    const lastComma = s.lastIndexOf(',');
    if (lastComma > lastDot) {
      s = s.replace(/\./g, '').replace(',', '.');
    } else {
      s = s.replace(/,/g, '');
    }
  } else if (hasComma && !hasDot) {
    const commaPos = s.indexOf(',');
    if (s.length - commaPos === 3) {
      s = s.replace(',', '.');
    } else {
      s = s.replace(',', '');
    }
  }

  const number = parseFloat(s);
  if (isNaN(number)) return null;

  const absCents = Math.round(Math.abs(number) * 100);
  return { amountCents: absCents, isCredit: isNegative || number < 0 };
}

function parseDate(raw: string): number | null {
  const s = raw.trim();

  const isoMatch = s.match(/^(\d{4})-(\d{1,2})-(\d{1,2})/);
  if (isoMatch) {
    const d = new Date(+isoMatch[1], +isoMatch[2] - 1, +isoMatch[3]);
    if (!isNaN(d.getTime())) return d.getTime();
  }

  const slashMatch = s.match(/^(\d{1,2})\/(\d{1,2})\/(\d{4})/);
  if (slashMatch) {
    const a = +slashMatch[1];
    const b = +slashMatch[2];
    const y = +slashMatch[3];
    if (a > 12) {
      const d = new Date(y, b - 1, a);
      if (!isNaN(d.getTime())) return d.getTime();
    } else if (b > 12) {
      const d = new Date(y, a - 1, b);
      if (!isNaN(d.getTime())) return d.getTime();
    } else {
      const d = new Date(y, b - 1, a);
      if (!isNaN(d.getTime())) return d.getTime();
    }
  }

  const mmmMatch = s.match(/^(\d{1,2})-([A-Za-z]{3})-(\d{4})/);
  if (mmmMatch) {
    const months: Record<string, number> = {
      jan: 0, feb: 1, mar: 2, apr: 3, may: 4, jun: 5,
      jul: 6, aug: 7, sep: 8, oct: 9, nov: 10, dec: 11,
    };
    const m = months[mmmMatch[2].toLowerCase()];
    if (m !== undefined) {
      const d = new Date(+mmmMatch[3], m, +mmmMatch[1]);
      if (!isNaN(d.getTime())) return d.getTime();
    }
  }

  const fallback = Date.parse(s);
  if (!isNaN(fallback)) return fallback;

  return null;
}

function detectType(raw: string | null): 'debit' | 'credit' | null {
  if (!raw) return null;
  const v = raw.toLowerCase().trim();
  if (/^(debit|dr|expense|withdrawal|out)/i.test(v)) return 'debit';
  if (/^(credit|cr|income|deposit|in)/i.test(v)) return 'credit';
  return null;
}

function parseCSV(
  csvText: string,
  defaultAccountId: string
): { rows: ImportTransaction[]; errors: number } {
  const lines = csvText.split(/\r?\n/).filter((l) => l.trim() !== '');
  if (lines.length < 2) return { rows: [], errors: lines.length };

  const headers = parseCSVLine(lines[0]);
  const cols = detectColumns(headers);

  if (cols.amountIdx === null || cols.dateIdx === null || cols.merchantIdx === null) {
    return { rows: [], errors: lines.length - 1 };
  }

  const rows: ImportTransaction[] = [];
  let errors = 0;

  for (let i = 1; i < lines.length; i++) {
    try {
      const fields = parseCSVLine(lines[i]);
      if (fields.every((f) => f === '')) continue;

      if (fields.length <= Math.max(cols.amountIdx!, cols.dateIdx!, cols.merchantIdx!)) {
        errors++;
        continue;
      }

      const amountRaw = fields[cols.amountIdx!];
      const dateRaw = fields[cols.dateIdx!];
      const merchantRaw = fields[cols.merchantIdx!];
      const typeRaw = cols.typeIdx !== null ? fields[cols.typeIdx] : null;

      const amountParsed = parseAmount(amountRaw);
      const dateEpoch = parseDate(dateRaw);
      const detectedType = detectType(typeRaw);

      if (!amountParsed || !dateEpoch || !merchantRaw.trim()) {
        errors++;
        continue;
      }

      let type: 'debit' | 'credit';
      if (detectedType) {
        type = detectedType;
      } else {
        type = amountParsed.isCredit ? 'credit' : 'debit';
      }

      rows.push({
        merchantRaw: merchantRaw.trim(),
        amountCents: amountParsed.amountCents,
        type,
        dateEpoch,
        categoryId: null,
        accountId: defaultAccountId,
        note: null,
      });
    } catch {
      errors++;
    }
  }

  return { rows, errors };
}

function formatDateShort(epoch: number): string {
  const d = new Date(epoch);
  const day = String(d.getDate()).padStart(2, '0');
  const month = String(d.getMonth() + 1).padStart(2, '0');
  const year = d.getFullYear();
  return `${day}/${month}/${year}`;
}

function formatAmountCents(cents: number): string {
  return (cents / 100).toLocaleString('en-IN', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  });
}

// --- Component ---

export default function CSVImportScreen({ categories, accounts, onBack, onImport }: Props) {
  const [dragOver, setDragOver] = useState(false);
  const [parsedRows, setParsedRows] = useState<ImportTransaction[]>([]);
  const [parseErrors, setParseErrors] = useState(0);
  const [selectedAccountId, setSelectedAccountId] = useState<string>(
    accounts.length > 0 ? accounts[0].id : ''
  );
  const [categoryMap, setCategoryMap] = useState<Record<number, string | null>>({});
  const [showSuccess, setShowSuccess] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const handleFile = useCallback(
    (file: File) => {
      if (!file.name.endsWith('.csv')) return;

      const reader = new FileReader();
      reader.onload = (e) => {
        const text = e.target?.result as string;
        const accId = selectedAccountId || (accounts.length > 0 ? accounts[0].id : '');
        const result = parseCSV(text, accId);
        setParsedRows(result.rows);
        setParseErrors(result.errors);
        setCategoryMap({});
        setShowSuccess(false);
      };
      reader.readAsText(file);
    },
    [accounts, selectedAccountId]
  );

  const handleDrop = useCallback(
    (e: React.DragEvent) => {
      e.preventDefault();
      setDragOver(false);
      const file = e.dataTransfer.files[0];
      if (file) handleFile(file);
    },
    [handleFile]
  );

  const handleDragOver = (e: React.DragEvent) => {
    e.preventDefault();
    setDragOver(true);
  };

  const handleDragLeave = (e: React.DragEvent) => {
    e.preventDefault();
    setDragOver(false);
  };

  const handleFileSelect = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) handleFile(file);
  };

  const handleCategoryChange = (rowIndex: number, categoryId: string) => {
    setCategoryMap((prev) => ({ ...prev, [rowIndex]: categoryId === '__none' ? null : categoryId }));
  };

  const handleImport = () => {
    const transactions = parsedRows.map((row, i) => ({
      ...row,
      categoryId: categoryMap[i] !== undefined ? categoryMap[i] : row.categoryId,
    }));
    onImport(transactions);
    setShowSuccess(true);
    setTimeout(() => {
      onBack();
    }, 1000);
  };

  const previewRows = parsedRows.slice(0, 10);

  return (
    <div className="min-h-screen bg-surface-base">
      <div className="max-w-3xl mx-auto p-4">
        {/* Header */}
        <div className="flex items-center justify-between mb-6">
          <button
            onClick={onBack}
            className="text-content-secondary hover:text-content-primary text-body"
          >
            ← Back
          </button>
          <h1 className="text-title font-bold text-content-primary">Import from CSV</h1>
          <div className="w-14" />
        </div>

        {/* Drag-and-drop zone */}
        {previewRows.length === 0 && (
          <motion.div
            initial={{ opacity: 0, y: 16 }}
            animate={{ opacity: 1, y: 0 }}
            className="relative"
          >
            <div
              onDrop={handleDrop}
              onDragOver={handleDragOver}
              onDragLeave={handleDragLeave}
              onClick={() => fileInputRef.current?.click()}
              className="flex flex-col items-center justify-center p-16 border-2 border-dashed rounded-xl cursor-pointer transition-all duration-300"
              style={{
                borderColor: dragOver
                  ? 'var(--color-accent-primary)'
                  : 'var(--color-content-tertiary)',
                backgroundColor: dragOver
                  ? 'var(--color-accent-primary)'
                  : 'transparent',
                boxShadow: dragOver
                  ? '0 0 24px rgba(99, 102, 241, 0.3)'
                  : 'none',
              }}
            >
              <motion.div
                animate={dragOver ? { scale: 1.1 } : { scale: 1 }}
                transition={{ type: 'spring', stiffness: 300, damping: 20 }}
              >
                <svg
                  width="64"
                  height="64"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke={dragOver ? 'white' : 'var(--color-content-tertiary)'}
                  strokeWidth="1.5"
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  className="mb-4 transition-colors duration-300"
                >
                  <path d="M21 15v4a2 2 0 01-2 2H5a2 2 0 01-2-2v-4" />
                  <polyline points="17 8 12 3 7 8" />
                  <line x1="12" y1="3" x2="12" y2="15" />
                </svg>
              </motion.div>
              <p
                className="text-heading font-semibold mb-1 transition-colors duration-300"
                style={{ color: dragOver ? 'white' : 'var(--color-content-primary)' }}
              >
                Drop CSV file here
              </p>
              <p
                className="text-caption mb-4 transition-colors duration-300"
                style={{ color: dragOver ? 'rgba(255,255,255,0.8)' : 'var(--color-content-secondary)' }}
              >
                or click to browse
              </p>
              <span
                className="px-4 py-2 rounded-pill text-caption font-medium transition-colors duration-300"
                style={{
                  backgroundColor: dragOver ? 'rgba(255,255,255,0.2)' : 'var(--color-accent-primary)',
                  color: dragOver ? 'white' : 'white',
                }}
              >
                Browse Files
              </span>
              <p className="text-micro text-content-tertiary mt-4">Accepts .csv files only</p>
            </div>
            <input
              ref={fileInputRef}
              type="file"
              accept=".csv"
              onChange={handleFileSelect}
              className="hidden"
            />
          </motion.div>
        )}

        {/* Preview */}
        {previewRows.length > 0 && !showSuccess && (
          <motion.div
            initial={{ opacity: 0, y: 16 }}
            animate={{ opacity: 1, y: 0 }}
            className="space-y-4"
          >
            {/* Account selector */}
            <div className="p-4 bg-surface-raised rounded-xl" style={{ boxShadow: 'var(--shadow-sm)' }}>
              <label className="text-caption font-medium text-content-secondary mb-2 block">
                Import into account
              </label>
              <select
                value={selectedAccountId}
                onChange={(e) => {
                  setSelectedAccountId(e.target.value);
                  setParsedRows((prev) =>
                    prev.map((r) => ({ ...r, accountId: e.target.value }))
                  );
                }}
                className="w-full p-3 bg-surface-base rounded-md outline-none text-body text-content-primary border border-content-tertiary/20"
              >
                {accounts.map((acc) => (
                  <option key={acc.id} value={acc.id}>
                    {acc.icon || '💳'} {acc.name}
                  </option>
                ))}
              </select>
            </div>

            {/* Summary */}
            <div className="flex items-center gap-4 text-caption">
              <span className="text-content-primary font-semibold">
                {parsedRows.length} transactions found
              </span>
              {parseErrors > 0 && (
                <span className="text-warning font-semibold">
                  {parseErrors} rows couldn't be parsed
                </span>
              )}
            </div>

            {/* Table */}
            <div className="bg-surface-raised rounded-xl overflow-hidden" style={{ boxShadow: 'var(--shadow-sm)' }}>
              <div className="overflow-x-auto max-h-96 overflow-y-auto">
                <table className="w-full text-left">
                  <thead>
                    <tr className="border-b" style={{ borderColor: 'var(--color-content-tertiary)' }}>
                      <th className="p-3 text-caption font-semibold text-content-secondary">Date</th>
                      <th className="p-3 text-caption font-semibold text-content-secondary">Merchant</th>
                      <th className="p-3 text-caption font-semibold text-content-secondary">Amount</th>
                      <th className="p-3 text-caption font-semibold text-content-secondary">Category</th>
                    </tr>
                  </thead>
                  <tbody>
                    {previewRows.map((row, i) => (
                      <tr
                        key={i}
                        className="border-b last:border-0 transition-colors hover:bg-surface-base/50"
                        style={{ borderColor: 'var(--color-content-tertiary)' }}
                      >
                        <td className="p-3 text-caption text-content-primary tabular-nums">
                          {formatDateShort(row.dateEpoch)}
                        </td>
                        <td className="p-3 text-caption text-content-primary max-w-[200px] truncate">
                          {row.merchantRaw}
                        </td>
                        <td className="p-3 text-caption">
                          <span className="flex items-center gap-2">
                            <span
                              className={`text-caption font-mono tabular-nums font-semibold ${
                                row.type === 'debit' ? 'text-negative' : 'text-positive'
                              }`}
                            >
                              {row.type === 'debit' ? '-' : '+'}₹{formatAmountCents(row.amountCents)}
                            </span>
                            <span
                              className={`px-1.5 py-0.5 rounded text-micro font-medium ${
                                row.type === 'debit'
                                  ? 'bg-negative/15 text-negative'
                                  : 'bg-positive/15 text-positive'
                              }`}
                            >
                              {row.type === 'debit' ? 'Dr' : 'Cr'}
                            </span>
                          </span>
                        </td>
                        <td className="p-3">
                          <select
                            value={categoryMap[i] ?? '__none'}
                            onChange={(e) => handleCategoryChange(i, e.target.value)}
                            className="w-full p-2 bg-surface-base rounded-md outline-none text-caption text-content-primary border border-content-tertiary/20"
                          >
                            <option value="__none">Uncategorized</option>
                            {categories.map((cat) => (
                              <option key={cat.id} value={cat.id}>
                                {cat.icon || '📁'} {cat.name}
                              </option>
                            ))}
                          </select>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
              {parsedRows.length > 10 && (
                <div className="p-3 text-center text-caption text-content-tertiary border-t" style={{ borderColor: 'var(--color-content-tertiary)' }}>
                  + {parsedRows.length - 10} more rows
                </div>
              )}
            </div>

            {/* Actions */}
            <div className="flex gap-3 pt-2">
              <button
                onClick={() => {
                  setParsedRows([]);
                  setParseErrors(0);
                  setCategoryMap({});
                }}
                className="px-6 py-3 rounded-lg text-body font-medium text-content-secondary hover:text-content-primary border border-content-tertiary/20 hover:border-content-tertiary/40 transition-colors"
              >
                Cancel
              </button>
              <button
                onClick={handleImport}
                className="flex-1 py-3 rounded-lg bg-accent-primary text-white text-body font-semibold hover:bg-accent-primary/90 transition-colors"
              >
                Import {parsedRows.length} transactions
              </button>
            </div>
          </motion.div>
        )}

        {/* Success */}
        <AnimatePresence>
          {showSuccess && (
            <motion.div
              initial={{ opacity: 0, scale: 0.9 }}
              animate={{ opacity: 1, scale: 1 }}
              exit={{ opacity: 0, scale: 0.9 }}
              className="text-center py-24"
            >
              <motion.div
                initial={{ scale: 0 }}
                animate={{ scale: 1 }}
                transition={{ type: 'spring', stiffness: 200, damping: 15, delay: 0.1 }}
                className="w-20 h-20 bg-positive/15 rounded-full flex items-center justify-center mx-auto mb-4"
              >
                <svg width="36" height="36" viewBox="0 0 24 24" fill="none" stroke="var(--color-positive)" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
                  <polyline points="20 6 9 17 4 12" />
                </svg>
              </motion.div>
              <h2 className="text-title font-bold text-content-primary mb-1">Import complete!</h2>
              <p className="text-caption text-content-secondary">
                {parsedRows.length} transactions imported successfully
              </p>
            </motion.div>
          )}
        </AnimatePresence>
      </div>
    </div>
  );
}
