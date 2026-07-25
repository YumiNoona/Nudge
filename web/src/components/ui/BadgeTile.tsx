import { motion } from 'framer-motion';

interface Props {
  icon: string;
  label: string;
  unlocked: boolean;
  isSecret?: boolean;
  className?: string;
}

export function BadgeTile({
  icon,
  label,
  unlocked,
  isSecret,
  className = '',
}: Props) {
  return (
    <motion.div
      initial={{ opacity: 0, scale: 0.8 }}
      animate={{ opacity: 1, scale: 1 }}
      transition={{ type: 'spring', stiffness: 260, damping: 20 }}
      className={`flex flex-col items-center justify-center gap-1.5 p-3 rounded-chip select-none ${
        unlocked
          ? 'bg-[var(--surface)] shadow-card'
          : 'border border-dashed border-ink-mute opacity-35'
      } ${className}`}
    >
      {unlocked ? (
        <span className="text-2xl leading-none">{icon}</span>
      ) : isSecret ? null : (
        <span className="text-2xl leading-none text-ink-mute">🔒</span>
      )}

      <span
        className={`text-[11px] font-medium text-center leading-tight ${
          unlocked ? 'text-ink-1' : 'text-ink-mute'
        }`}
      >
        {unlocked ? label : isSecret ? '?' : '???'}
      </span>
    </motion.div>
  );
}
