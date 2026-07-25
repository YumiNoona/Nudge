import { motion } from 'framer-motion';

interface Props {
  icon: string;
  color?: string;
  label?: string;
  selected?: boolean;
  size?: 'sm' | 'md';
  onClick?: () => void;
  className?: string;
}

const SIZE_MAP = { sm: 36, md: 44 };
const ICON_SIZE_MAP = { sm: 'text-base', md: 'text-lg' };

export function CategoryChip({
  icon,
  color = 'var(--purple)',
  label,
  selected = false,
  size = 'md',
  onClick,
  className = '',
}: Props) {
  const containerSize = SIZE_MAP[size];
  const iconSizeClass = ICON_SIZE_MAP[size];

  return (
    <motion.div
      className={`inline-flex flex-col items-center gap-1 ${className}`}
      initial={{ opacity: 0, scale: 0.9 }}
      animate={{ opacity: 1, scale: 1 }}
    >
      <motion.button
        whileTap={onClick ? { scale: 0.9 } : undefined}
        onClick={onClick}
        className="relative flex items-center justify-center rounded-chip overflow-hidden"
        style={{ width: containerSize, height: containerSize }}
      >
        <div
          className="absolute inset-0 rounded-chip"
          style={{ backgroundColor: color, opacity: 0.15 }}
        />
        {selected && (
          <div
            className="absolute inset-0 rounded-chip ring-2"
            style={{ boxShadow: `0 0 0 2px ${color}` }}
          />
        )}
        <span
          className={`relative z-10 leading-none ${iconSizeClass}`}
          style={{ color }}
        >
          {icon}
        </span>
      </motion.button>

      {label && (
        <span className="text-[10px] font-medium text-ink-mute text-center leading-tight max-w-[56px] truncate">
          {label}
        </span>
      )}
    </motion.div>
  );
}
