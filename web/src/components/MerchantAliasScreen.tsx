import { useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';

interface MerchantAlias {
  id: string;
  rawPattern: string;
  normalizedName: string;
  suggestedCategoryId: string | null;
}

interface Props {
  aliases: MerchantAlias[];
  onAdd: (rawPattern: string, normalizedName: string, categoryId: string | null) => void;
  onDelete: (id: string) => void;
  onBack: () => void;
}

export default function MerchantAliasScreen({ aliases, onAdd, onDelete, onBack }: Props) {
  const [showForm, setShowForm] = useState(false);
  const [rawPattern, setRawPattern] = useState('');
  const [normalizedName, setNormalizedName] = useState('');

  const handleSubmit = () => {
    if (!rawPattern.trim() || !normalizedName.trim()) return;
    onAdd(rawPattern.trim(), normalizedName.trim(), null);
    setRawPattern('');
    setNormalizedName('');
    setShowForm(false);
  };

  return (
    <div className="min-h-screen bg-lavender-bg">
      <div className="max-w-2xl mx-auto p-4">
        <div className="flex items-center justify-between mb-6">
          <button onClick={onBack} className="text-ink-soft text-[13px] hover:text-ink-1 transition-colors">
            ← Back
          </button>
          <h1 className="text-sm font-bold text-ink-soft uppercase tracking-wide">Merchant Aliases</h1>
          <button
            onClick={() => setShowForm(true)}
            className="px-4 py-2 bg-gradient-to-r from-purple-1 to-purple-2 text-white rounded-pill text-[13px] font-medium"
          >
            + Add
          </button>
        </div>

        <p className="text-[11px] text-ink-mute mb-6">
          Map messy bank names to clean merchant names. E.g., "AMAZON PAY IN*ORDR8827" → "Amazon"
        </p>

        {/* Add form */}
        <AnimatePresence>
          {showForm && (
            <motion.div
              initial={{ height: 0, opacity: 0 }}
              animate={{ height: 'auto', opacity: 1 }}
              exit={{ height: 0, opacity: 0 }}
              className="overflow-hidden mb-4"
            >
              <div className="p-4 rounded-card shadow-card bg-[var(--surface)] space-y-3">
                <input
                  type="text"
                  placeholder="Raw pattern (e.g. AMAZON PAY IN)"
                  value={rawPattern}
                  onChange={(e) => setRawPattern(e.target.value)}
                  className="w-full p-3 bg-lavender-bg rounded-chip outline-none text-[13px] text-ink-1 placeholder:text-ink-mute"
                />
                <input
                  type="text"
                  placeholder="Normalized name (e.g. Amazon)"
                  value={normalizedName}
                  onChange={(e) => setNormalizedName(e.target.value)}
                  className="w-full p-3 bg-lavender-bg rounded-chip outline-none text-[13px] text-ink-1 placeholder:text-ink-mute"
                />
                <div className="flex gap-2">
                  <button
                    onClick={handleSubmit}
                    disabled={!rawPattern.trim() || !normalizedName.trim()}
                    className={`flex-1 py-2 rounded-pill text-[13px] font-medium ${
                      rawPattern.trim() && normalizedName.trim()
                        ? 'bg-gradient-to-r from-purple-1 to-purple-2 text-white'
                        : 'bg-ink-mute/20 text-ink-mute cursor-not-allowed'
                    }`}
                  >
                    Save
                  </button>
                  <button
                    onClick={() => setShowForm(false)}
                    className="px-4 py-2 text-[13px] text-ink-soft hover:text-ink-1"
                  >
                    Cancel
                  </button>
                </div>
              </div>
            </motion.div>
          )}
        </AnimatePresence>

        {/* Alias list */}
        {aliases.length === 0 ? (
          <div className="flex flex-col items-center justify-center py-16 gap-3 rounded-card bg-[var(--surface)] shadow-card">
            <span className="text-4xl opacity-50">🏷️</span>
            <p className="text-sm text-ink-soft">No merchant aliases yet</p>
            <p className="text-[11px] text-ink-mute">Add your first alias to teach the app to recognize merchants</p>
          </div>
        ) : (
          <div className="space-y-2">
            {aliases.map((alias) => (
              <motion.div
                key={alias.id}
                initial={{ opacity: 0 }}
                animate={{ opacity: 1 }}
                exit={{ opacity: 0 }}
                className="flex items-center justify-between p-3 rounded-card shadow-card bg-[var(--surface)]"
              >
                <div>
                  <p className="font-mono text-xs text-ink-mute">{alias.rawPattern}</p>
                  <p className="text-sm text-ink-1 font-medium">→ {alias.normalizedName}</p>
                </div>
                <button
                  onClick={() => onDelete(alias.id)}
                  className="p-2 text-ink-mute hover:text-coral-1 transition-colors text-sm"
                >
                  ✕
                </button>
              </motion.div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
