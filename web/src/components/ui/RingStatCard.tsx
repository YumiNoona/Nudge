import { motion } from 'framer-motion';

interface Props {
  progress: number;
  label: string;
  subtext?: string;
  icon?: string;
  color?: 'purple' | 'coral' | 'green' | 'amber';
  size?: 'sm' | 'md';
  className?: string;
}

const COLOR_MAP: Record<string, string> = {
  purple: 'var(--purple)',
  coral: 'var(--coral)',
  green: 'var(--green)',
  amber: 'var(--amber)',
};

const SIZE_MAP = { sm: 80, md: 96 };
const THICKNESS_MAP = { sm: 6, md: 7 };

export function RingStatCard({
  progress,
  label,
  subtext,
  icon,
  color = 'purple',
  size = 'sm',
  className = '',
}: Props) {
  const ringSize = SIZE_MAP[size];
  const thickness = THICKNESS_MAP[size];
  const innerPercent = ((ringSize - thickness * 2) / ringSize) * 100;
  const pct = Math.max(0, Math.min(100, progress));
  const strokeColor = COLOR_MAP[color] || COLOR_MAP.purple;

  return (
    <motion.div
      initial={{ opacity: 0, scale: 0.92 }}
      animate={{ opacity: 1, scale: 1 }}
      className={`flex flex-col items-center gap-1.5 ${className}`}
    >
      <span className="text-xs font-semibold text-ink-1">{label}</span>

      <div className="relative flex items-center justify-center" style={{ width: ringSize, height: ringSize }}>
        <div
          className="absolute inset-0 rounded-full"
          style={{
            background: `conic-gradient(${strokeColor} ${pct * 3.6}deg, transparent ${pct * 3.6}deg)`,
            mask: `radial-gradient(transparent ${innerPercent}%, black ${innerPercent}%)`,
            WebkitMask: `radial-gradient(transparent ${innerPercent}%, black ${innerPercent}%)`,
          }}
        />
        <div className="z-10 flex flex-col items-center justify-center">
          {icon ? (
            <span className="text-lg leading-none">{icon}</span>
          ) : (
            <span className="text-sm font-bold font-mono tabular-nums text-ink-1">
              {Math.round(pct)}%
            </span>
          )}
        </div>
      </div>

      {subtext && <span className="text-[11px] text-ink-mute">{subtext}</span>}
    </motion.div>
  );
}
