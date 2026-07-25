import { motion } from 'framer-motion';

interface Props {
  icon: string;
  merchant: string;
  subtext: string;
  amount: string;
  isExpense: boolean;
  categoryColor?: string;
  className?: string;
}

export function TransactionRow({
  icon,
  merchant,
  subtext,
  amount,
  isExpense,
  categoryColor = 'var(--cat-blue)',
  className = '',
}: Props) {
  return (
    <motion.div
      initial={{ opacity: 0, x: -8 }}
      animate={{ opacity: 1, x: 0 }}
      className={`flex items-center gap-3 py-3 border-b border-black/5 dark:border-white/5 hover:bg-surface-hover rounded-lg px-2 -mx-2 transition-colors cursor-pointer ${className}`}
    >
      <div className="relative flex items-center justify-center w-9 h-9 rounded-chip flex-shrink-0 overflow-hidden">
        <div
          className="absolute inset-0 rounded-chip"
          style={{ backgroundColor: categoryColor, opacity: 0.15 }}
        />
        <span
          className="relative z-10 text-base leading-none"
          style={{ color: categoryColor }}
        >
          {icon}
        </span>
      </div>

      <div className="flex-1 min-w-0">
        <p className="text-[13px] font-medium text-ink-1 truncate">{merchant}</p>
        <p className="text-[11px] text-ink-mute truncate">{subtext}</p>
      </div>

      <p
        className={`text-[13px] font-semibold font-mono tabular-nums flex-shrink-0 ${
          isExpense ? 'text-coral-1' : 'text-green-1'
        }`}
      >
        {amount}
      </p>
    </motion.div>
  );
}
