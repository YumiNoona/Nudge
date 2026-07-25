import { useState, useEffect } from 'react';
import { useLiveQuery } from 'dexie-react-hooks';
import { db, generateId, formatAmount } from '../lib/db';
import type { Transaction, Category } from '../lib/db';
import { motion, AnimatePresence, PanInfo } from 'framer-motion';
import { CategoryChip } from './ui/CategoryChip';

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
      <div className="min-h-screen bg-lavender-bg flex items-center justify-center">
        <div className="text-center">
          <span className="text-5xl block mb-4">🎉</span>
          <h2 className="text-lg font-bold text-ink-1 mb-2">All caught up!</h2>
          <p className="text-[11px] text-ink-soft mb-6">No transactions need review right now</p>
          <button
            onClick={onBack}
            className="px-6 py-2 bg-gradient-to-br from-purple-1 to-purple-2 text-ink-inv rounded-pill text-[13px] font-medium"
          >
            Back to Dashboard
          </button>
        </div>
      </div>
    );
  }

  if (!currentTxn) {
    return (
      <div className="min-h-screen bg-lavender-bg flex items-center justify-center">
        <div className="text-center">
          <span className="text-5xl block mb-4">✅</span>
          <h2 className="text-lg font-bold text-ink-1 mb-2">All reviewed!</h2>
          <p className="text-[11px] text-ink-soft mb-6">Great job reviewing everything</p>
          <button onClick={onBack} className="px-6 py-2 bg-gradient-to-br from-purple-1 to-purple-2 text-ink-inv rounded-pill text-[13px] font-medium">
            Done
          </button>
        </div>
      </div>
    );
  }

  const progress = ((currentIndex) / transactions.length) * 100;

  return (
    <div className="min-h-screen bg-lavender-bg flex flex-col">
      {/* Header */}
      <div className="p-4 flex items-center justify-between">
        <button onClick={onBack} className="text-ink-soft text-[13px] hover:text-ink-1 transition-colors">
          ← Back
        </button>
        <div className="flex items-center gap-2">
          <span className="text-[14px] font-bold text-ink-1">Review</span>
          <span className="text-[11px] text-ink-mute font-mono tabular-nums">
            {currentIndex + 1}/{transactions.length}
          </span>
        </div>
        <div className="w-16" />
      </div>

      {/* Progress bar */}
      <div className="h-1 bg-purple-bg mx-4 rounded-full overflow-hidden">
        <div
          className="h-full bg-gradient-to-r from-purple-1 to-purple-2 rounded-full transition-all duration-300"
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
              className="absolute left-8 text-lg font-bold text-coral-1"
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
              className="absolute right-8 text-lg font-bold text-green-1"
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
          className="w-full max-w-sm bg-[var(--surface)] rounded-card shadow-card-md p-6 cursor-grab active:cursor-grabbing flex flex-col items-center gap-3"
          style={{
            rotate: exitX / 25,
          }}
        >
          <CategoryChip
            icon={currentTxn.categoryId ? (categories.find((c) => c.id === currentTxn.categoryId)?.icon || '📋') : '📋'}
          />

          <p
            className={`text-3xl font-extrabold font-mono tabular-nums ${
              currentTxn.type === 'debit' ? 'text-coral-1' : 'text-green-1'
            }`}
          >
            {currentTxn.type === 'debit' ? '-' : '+'}₹{formatAmount(currentTxn.amountCents)}
          </p>

          <p className="text-sm font-semibold text-ink-1">{currentTxn.merchantRaw}</p>

          {currentTxn.sourceRawText && (
            <p className="text-[11px] text-ink-mute italic line-clamp-3 max-w-full">
              &ldquo;{currentTxn.sourceRawText}&rdquo;
            </p>
          )}

          {currentTxn.confidenceScore < 0.7 && (
            <span className="px-2.5 py-0.5 rounded-full text-[10px] font-semibold bg-amber-bg text-amber-1">
              Low confidence ({Math.round(currentTxn.confidenceScore * 100)}%)
            </span>
          )}

          <div className="flex gap-6 mt-2">
            <motion.button
              whileTap={{ scale: 0.9 }}
              onClick={handleSkip}
              className="w-12 h-12 rounded-full bg-coral-bg text-coral-1 flex items-center justify-center text-xl font-bold shadow-sm hover:shadow-md transition-shadow"
              aria-label="Skip"
            >
              ✕
            </motion.button>
            <motion.button
              whileTap={{ scale: 0.9 }}
              onClick={() => setShowCategoryPicker(true)}
              className="w-12 h-12 rounded-full bg-green-bg text-green-1 flex items-center justify-center text-xl font-bold shadow-sm hover:shadow-md transition-shadow"
              aria-label="Categorize"
            >
              ✓
            </motion.button>
          </div>
        </motion.div>

        {/* Stacked cards behind */}
        {currentIndex + 1 < transactions.length && (
          <div className="absolute w-full max-w-sm" style={{ zIndex: -1 }}>
            <div
              className="w-full mx-auto rounded-card bg-[var(--surface)] shadow-card-md opacity-40"
              style={{ height: 100, transform: 'translateY(8px)' }}
            />
          </div>
        )}
        {currentIndex + 2 < transactions.length && (
          <div className="absolute w-full max-w-sm" style={{ zIndex: -2 }}>
            <div
              className="w-[92%] mx-auto rounded-card bg-[var(--surface)] shadow-card opacity-40"
              style={{ height: 70, transform: 'translateY(16px)' }}
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
      className="fixed inset-0 bg-ink-1/40 flex items-end lg:items-center justify-center z-50"
      onClick={onClose}
    >
      <motion.div
        initial={{ y: 100, opacity: 0 }}
        animate={{ y: 0, opacity: 1 }}
        exit={{ y: 100, opacity: 0 }}
        className="bg-[var(--surface)] w-full max-w-md rounded-t-3xl lg:rounded-3xl p-6 max-h-[70vh] overflow-y-auto border border-ink-mute/5"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="w-9 h-1 bg-ink-mute rounded-full mx-auto mb-4" />
        <h3 className="text-[14px] font-bold text-ink-1 mb-1">Categorize</h3>
        <p className="text-[11px] text-ink-soft mb-2">{transaction.merchantRaw}</p>
        <p className="text-lg font-bold font-mono tabular-nums text-ink-1 mb-4">
          ₹{formatAmount(transaction.amountCents)}
        </p>

        <div className="grid grid-cols-4 gap-3 justify-items-center">
          {categories.map((cat) => (
            <CategoryChip
              key={cat.id}
              icon={cat.icon || '📁'}
              label={cat.name}
              size="md"
              onClick={() => onSelect(cat.id)}
            />
          ))}
        </div>

        <button
          onClick={onClose}
          className="w-full mt-4 py-2 text-[11px] text-ink-soft hover:text-ink-1 transition-colors"
        >
          Cancel
        </button>
      </motion.div>
    </motion.div>
  );
}
