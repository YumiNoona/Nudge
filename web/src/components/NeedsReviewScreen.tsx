import { useState, useEffect } from 'react';
import { useLiveQuery } from 'dexie-react-hooks';
import { db, generateId, formatAmount } from '../lib/db';
import type { Transaction, Category } from '../lib/db';
import { motion, AnimatePresence, PanInfo } from 'framer-motion';

interface Props {
  onBack: () => void;
}

export default function NeedsReviewScreen({ onBack }: Props) {
  const transactions = useLiveQuery(
    () => db.transactions.filter((t) => !t.isReviewed).toArray()
  ) ?? [];

  const categories = useLiveQuery(
    () => db.categories.filter((c) => !c.isArchived).toArray()
  ) ?? [];

  const [currentIndex, setCurrentIndex] = useState(0);
  const [showCategoryPicker, setShowCategoryPicker] = useState(false);
  const [exitX, setExitX] = useState(0);

  const currentTxn = transactions[currentIndex] ?? null;

  const handleCategorize = async (categoryId: string) => {
    if (!currentTxn) return;
    await db.transactions.update(currentTxn.id, {
      categoryId,
      isReviewed: true,
      updatedAt: Date.now(),
    });
    setShowCategoryPicker(false);
    setCurrentIndex((i) => i + 1);
    setExitX(0);
  };

  const handleSkip = async () => {
    if (!currentTxn) return;
    await db.transactions.update(currentTxn.id, {
      isReviewed: true,
      updatedAt: Date.now(),
    });
    setCurrentIndex((i) => i + 1);
  };

  const handleDragEnd = (_: any, info: PanInfo) => {
    const threshold = 120;
    if (info.offset.x > threshold) {
      setShowCategoryPicker(true);
    } else if (info.offset.x < -threshold) {
      handleSkip();
    } else {
      setExitX(0);
    }
  };

  if (transactions.length === 0 || (!currentTxn && currentIndex >= transactions.length)) {
    return (
      <div className="min-h-screen bg-surface-base flex items-center justify-center">
        <div className="text-center">
          <span className="text-5xl block mb-4">🎉</span>
          <h2 className="text-title font-bold text-content-primary mb-2">All caught up!</h2>
          <p className="text-caption text-content-secondary mb-6">No transactions need review right now</p>
          <button
            onClick={onBack}
            className="px-6 py-2 bg-accent-primary text-white rounded-pill text-body font-medium hover:bg-accent-primary/90"
          >
            Back to Dashboard
          </button>
        </div>
      </div>
    );
  }

  if (!currentTxn) {
    return (
      <div className="min-h-screen bg-surface-base flex items-center justify-center">
        <div className="text-center">
          <span className="text-5xl block mb-4">✅</span>
          <h2 className="text-title font-bold text-content-primary mb-2">All reviewed!</h2>
          <p className="text-caption text-content-secondary mb-6">Great job reviewing everything</p>
          <button onClick={onBack} className="px-6 py-2 bg-accent-primary text-white rounded-pill text-body font-medium">
            Done
          </button>
        </div>
      </div>
    );
  }

  const progress = ((currentIndex) / transactions.length) * 100;

  return (
    <div className="min-h-screen bg-surface-base flex flex-col">
      {/* Header */}
      <div className="p-4 flex items-center justify-between">
        <button onClick={onBack} className="text-content-secondary text-body hover:text-content-primary">
          ← Back
        </button>
        <span className="text-caption text-content-secondary">
          {currentIndex + 1} / {transactions.length}
        </span>
        <div className="w-16" />
      </div>

      {/* Progress bar */}
      <div className="h-1 bg-surface-raised mx-4 rounded-full overflow-hidden">
        <div
          className="h-full bg-accent-primary rounded-full transition-all duration-300"
          style={{ width: `${progress}%` }}
        />
      </div>

      {/* Swipe card */}
      <div className="flex-1 flex items-center justify-center p-4 relative">
        {/* Hints */}
        <AnimatePresence>
          {exitX < -30 && (
            <motion.div
              initial={{ opacity: 0 }}
              animate={{ opacity: Math.min(Math.abs(exitX) / 80, 1) }}
              exit={{ opacity: 0 }}
              className="absolute left-8 text-lg font-semibold text-negative"
            >
              ← Skip
            </motion.div>
          )}
        </AnimatePresence>
        <AnimatePresence>
          {exitX > 30 && (
            <motion.div
              initial={{ opacity: 0 }}
              animate={{ opacity: Math.min(exitX / 80, 1) }}
              exit={{ opacity: 0 }}
              className="absolute right-8 text-lg font-semibold text-positive"
            >
              Categorize →
            </motion.div>
          )}
        </AnimatePresence>

        <motion.div
          key={currentTxn.id}
          initial={{ scale: 0.9, opacity: 0 }}
          animate={{ scale: 1, opacity: 1 }}
          exit={{ x: exitX, opacity: 0 }}
          drag="x"
          dragConstraints={{ left: 0, right: 0 }}
          dragElastic={0.8}
          onDrag={(_, info) => setExitX(info.offset.x)}
          onDragEnd={handleDragEnd}
          whileDrag={{ scale: 1.02 }}
          className="w-full max-w-sm bg-surface-raised rounded-xl p-6 cursor-grab active:cursor-grabbing"
          style={{
            boxShadow: 'var(--shadow-md)',
            rotate: exitX / 25,
          }}
        >
          {/* Amount */}
          <p
            className={`text-display font-bold font-mono tabular-nums ${
              currentTxn.type === 'debit' ? 'text-negative' : 'text-positive'
            }`}
          >
            {currentTxn.type === 'debit' ? '-' : '+'}₹{formatAmount(currentTxn.amountCents)}
          </p>

          {/* Merchant */}
          <p className="text-title font-semibold text-content-primary mt-2">{currentTxn.merchantRaw}</p>

          {/* Source text preview */}
          {currentTxn.sourceRawText && (
            <p className="text-caption text-content-tertiary mt-2 italic line-clamp-3">
              "{currentTxn.sourceRawText}"
            </p>
          )}

          {/* Confidence badge */}
          {currentTxn.confidenceScore < 0.7 && (
            <span className="inline-block mt-3 px-2 py-1 bg-warning/15 text-warning text-micro rounded-md">
              Low confidence ({Math.round(currentTxn.confidenceScore * 100)}%)
            </span>
          )}

          {/* Quick action buttons */}
          <div className="flex gap-3 mt-6">
            <button
              onClick={handleSkip}
              className="flex-1 py-3 rounded-lg border border-negative/30 text-negative text-body font-medium hover:bg-negative/5 transition-colors"
            >
              Skip
            </button>
            <button
              onClick={() => setShowCategoryPicker(true)}
              className="flex-1 py-3 rounded-lg bg-accent-primary text-white text-body font-medium hover:bg-accent-primary/90 transition-colors"
            >
              Categorize
            </button>
          </div>
        </motion.div>

        {/* Stacked cards behind */}
        {currentIndex + 1 < transactions.length && (
          <div className="absolute w-full max-w-sm" style={{ zIndex: -1 }}>
            <div
              className="w-10/12 mx-auto bg-surface-raised/60 rounded-xl"
              style={{ height: 100, transform: 'translateY(12px)', boxShadow: 'var(--shadow-sm)' }}
            />
          </div>
        )}
        {currentIndex + 2 < transactions.length && (
          <div className="absolute w-full max-w-sm" style={{ zIndex: -2 }}>
            <div
              className="w-8/12 mx-auto bg-surface-raised/30 rounded-xl"
              style={{ height: 70, transform: 'translateY(24px)' }}
            />
          </div>
        )}
      </div>

      {/* Category picker modal */}
      <AnimatePresence>
        {showCategoryPicker && currentTxn && (
          <CategoryPickerModal
            transaction={currentTxn}
            categories={categories}
            onSelect={handleCategorize}
            onClose={() => setShowCategoryPicker(false)}
          />
        )}
      </AnimatePresence>
    </div>
  );
}

function CategoryPickerModal({
  transaction,
  categories,
  onSelect,
  onClose,
}: {
  transaction: Transaction;
  categories: Category[];
  onSelect: (categoryId: string) => void;
  onClose: () => void;
}) {
  return (
    <motion.div
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      exit={{ opacity: 0 }}
      className="fixed inset-0 bg-surface-overlay flex items-end lg:items-center justify-center z-50"
      onClick={onClose}
    >
      <motion.div
        initial={{ y: 100, opacity: 0 }}
        animate={{ y: 0, opacity: 1 }}
        exit={{ y: 100, opacity: 0 }}
        className="bg-surface-base w-full max-w-md rounded-t-xl lg:rounded-xl p-6 max-h-[70vh] overflow-y-auto"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="w-9 h-1 bg-content-tertiary rounded-full mx-auto mb-4" />
        <h3 className="text-heading font-bold text-content-primary mb-1">Categorize</h3>
        <p className="text-caption text-content-secondary mb-2">{transaction.merchantRaw}</p>
        <p className="text-title font-bold font-mono tabular-nums text-content-primary mb-4">
          ₹{formatAmount(transaction.amountCents)}
        </p>

        <div className="grid grid-cols-4 gap-2">
          {categories.map((cat) => (
            <button
              key={cat.id}
              onClick={() => onSelect(cat.id)}
              className="flex flex-col items-center p-3 bg-surface-raised rounded-lg hover:bg-accent-primary/10 transition-colors"
            >
              <span className="text-xl">{cat.icon || '📁'}</span>
              <span className="text-micro text-content-secondary mt-1 truncate w-full text-center">
                {cat.name}
              </span>
            </button>
          ))}
        </div>

        <button
          onClick={onClose}
          className="w-full mt-4 py-2 text-caption text-content-secondary hover:text-content-primary"
        >
          Cancel
        </button>
      </motion.div>
    </motion.div>
  );
}
