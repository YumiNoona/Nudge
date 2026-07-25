import { motion } from 'framer-motion';

interface Props {
  icon: string;
  merchant: string;
  amount: string;
  date: string;
  confidence?: number;
  onApprove: () => void;
  onSkip: () => void;
  className?: string;
}

export function SwipeReviewCard({
  icon,
  merchant,
  amount,
  date,
  confidence,
  onApprove,
  onSkip,
  className = '',
}: Props) {
  const catColor = 'var(--purple)';

  return (
    <div className={`relative ${className}`}>
      <div
        className="absolute top-2 left-0 right-0 h-full rounded-card bg-[var(--surface)] shadow-card-md opacity-40"
        style={{ zIndex: 0 }}
      />

      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        className="relative z-10 bg-[var(--surface)] rounded-card shadow-card-md p-6 flex flex-col items-center gap-3"
      >
        <div className="relative flex items-center justify-center w-12 h-12 rounded-chip overflow-hidden">
          <div
            className="absolute inset-0 rounded-chip"
            style={{ backgroundColor: catColor, opacity: 0.15 }}
          />
          <span
            className="relative z-10 text-xl leading-none"
            style={{ color: catColor }}
          >
            {icon}
          </span>
        </div>

        <p className="text-[28px] font-extrabold font-mono tabular-nums text-ink-1">
          {amount}
        </p>

        <p className="text-[14px] font-semibold text-ink-1">{merchant}</p>

        <p className="text-[11px] text-ink-mute">{date}</p>

        {confidence !== undefined && confidence < 70 && (
          <span className="px-2.5 py-0.5 rounded-full text-[10px] font-semibold bg-amber-bg text-amber-1">
            Low confidence
          </span>
        )}

        <div className="flex gap-6 mt-2">
          <motion.button
            whileTap={{ scale: 0.9 }}
            onClick={onSkip}
            className="w-12 h-12 rounded-full bg-coral-bg text-coral-1 flex items-center justify-center text-xl font-bold shadow-sm hover:shadow-md transition-shadow"
            aria-label="Skip"
          >
            ✕
          </motion.button>
          <motion.button
            whileTap={{ scale: 0.9 }}
            onClick={onApprove}
            className="w-12 h-12 rounded-full bg-green-bg text-green-1 flex items-center justify-center text-xl font-bold shadow-sm hover:shadow-md transition-shadow"
            aria-label="Approve"
          >
            ✓
          </motion.button>
        </div>
      </motion.div>
    </div>
  );
}
