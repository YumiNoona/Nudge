import { motion } from 'framer-motion';

interface Props {
  segments: { label: string; value: number; color: string }[];
  total: number;
  centerLabel: string;
  centerSubtext?: string;
  size?: number;
  className?: string;
}

export function DonutChart({
  segments,
  total,
  centerLabel,
  centerSubtext,
  size = 160,
  className = '',
}: Props) {
  let cumulative = 0;
  const stops: string[] = [];

  for (const seg of segments) {
    if (seg.value <= 0 || total <= 0) continue;
    const pct = (seg.value / total) * 100;
    const start = cumulative;
    const end = cumulative + pct;
    stops.push(`${seg.color} ${start}% ${end}%`);
    cumulative = end;
  }

  if (cumulative < 100) {
    stops.push(`transparent ${cumulative}% 100%`);
  }

  const gradient = stops.length > 0
    ? `conic-gradient(${stops.join(', ')})`
    : `conic-gradient(transparent 0% 100%)`;

  const innerSize = Math.round(size * 0.6);
  const innerOffset = (size - innerSize) / 2;

  const totalFormatted = total.toLocaleString();

  return (
    <motion.div
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      className={`flex flex-col items-center gap-4 ${className}`}
    >
      <div className="relative" style={{ width: size, height: size }}>
        <motion.div
          className="absolute inset-0 rounded-full"
          style={{ background: gradient }}
          initial={{ rotate: 0 }}
          animate={{ rotate: 360 }}
          transition={{ duration: 1, ease: 'easeOut' }}
        />

        <div
          className="absolute rounded-full bg-[var(--surface)]"
          style={{
            width: innerSize,
            height: innerSize,
            top: innerOffset,
            left: innerOffset,
          }}
        />

        <div
          className="absolute flex flex-col items-center justify-center leading-tight"
          style={{
            width: innerSize,
            height: innerSize,
            top: innerOffset,
            left: innerOffset,
          }}
        >
          <span className="text-lg font-extrabold font-mono tabular-nums text-ink-1">
            {totalFormatted}
          </span>
          <span className="text-[10px] font-semibold text-ink-soft mt-0.5">
            {centerLabel}
          </span>
          {centerSubtext && (
            <span className="text-[10px] text-ink-mute">{centerSubtext}</span>
          )}
        </div>
      </div>

      {segments.length > 0 && (
        <div className="flex flex-wrap gap-x-4 gap-y-1.5 justify-center">
          {segments.map((seg, i) => (
            <motion.div
              key={i}
              className="flex items-center gap-1.5"
              initial={{ opacity: 0, x: -4 }}
              animate={{ opacity: 1, x: 0 }}
              transition={{ delay: 0.1 * i, duration: 0.3 }}
            >
              <span
                className="w-2.5 h-2.5 rounded-full flex-shrink-0"
                style={{ backgroundColor: seg.color }}
              />
              <span className="text-[11px] text-ink-soft">{seg.label}</span>
              <span className="text-[11px] font-semibold font-mono tabular-nums text-ink-1">
                {seg.value.toLocaleString()}
              </span>
            </motion.div>
          ))}
        </div>
      )}
    </motion.div>
  );
}
