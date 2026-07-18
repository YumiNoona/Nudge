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
    <div className="min-h-screen bg-surface-base">
      <div className="max-w-2xl mx-auto p-4">
        {/* Header */}
        <div className="flex items-center justify-between mb-6">
          <button onClick={onBack} className="text-content-secondary hover:text-content-primary">
            ← Back
          </button>
          <h1 className="text-title font-bold text-content-primary">Merchant Aliases</h1>
          <button
            onClick={() => setShowForm(true)}
            className="px-4 py-2 bg-accent-primary text-white rounded-pill text-caption font-medium hover:bg-accent-primary/90"
          >
            + Add
          </button>
        </div>

        <p className="text-caption text-content-secondary mb-6">
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
              <div className="p-4 bg-surface-raised rounded-xl space-y-3" style={{ boxShadow: 'var(--shadow-sm)' }}>
                <input
                  type="text"
                  placeholder="Raw pattern (e.g. AMAZON PAY IN)"
                  value={rawPattern}
                  onChange={(e) => setRawPattern(e.target.value)}
                  className="w-full p-3 bg-surface-base rounded-md outline-none text-body text-content-primary placeholder:text-content-tertiary"
                />
                <input
                  type="text"
                  placeholder="Normalized name (e.g. Amazon)"
                  value={normalizedName}
                  onChange={(e) => setNormalizedName(e.target.value)}
                  className="w-full p-3 bg-surface-base rounded-md outline-none text-body text-content-primary placeholder:text-content-tertiary"
                />
                <div className="flex gap-2">
                  <button
                    onClick={handleSubmit}
                    disabled={!rawPattern.trim() || !normalizedName.trim()}
                    className={`flex-1 py-2 rounded-lg text-caption font-medium ${
                      rawPattern.trim() && normalizedName.trim()
                        ? 'bg-accent-primary text-white'
                        : 'bg-content-tertiary/20 text-content-tertiary cursor-not-allowed'
                    }`}
                  >
                    Save
                  </button>
                  <button
                    onClick={() => setShowForm(false)}
                    className="px-4 py-2 text-caption text-content-secondary hover:text-content-primary"
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
          <div className="text-center py-16">
            <span className="text-4xl block mb-2">🏷️</span>
            <p className="text-body text-content-secondary">No merchant aliases yet</p>
            <p className="text-caption text-content-tertiary">Add your first alias to teach the app to recognize merchants</p>
          </div>
        ) : (
          <div className="space-y-2">
            {aliases.map((alias) => (
              <motion.div
                key={alias.id}
                initial={{ opacity: 0 }}
                animate={{ opacity: 1 }}
                exit={{ opacity: 0 }}
                className="flex items-center justify-between p-3 bg-surface-raised rounded-lg"
                style={{ boxShadow: 'var(--shadow-sm)' }}
              >
                <div>
                  <p className="text-caption text-content-tertiary font-mono">{alias.rawPattern}</p>
                  <p className="text-body text-content-primary font-medium">→ {alias.normalizedName}</p>
                </div>
                <button
                  onClick={() => onDelete(alias.id)}
                  className="p-2 text-content-tertiary hover:text-negative transition-colors text-sm"
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
