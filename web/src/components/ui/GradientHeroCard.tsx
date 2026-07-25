import { motion } from 'framer-motion';

interface Props {
  value: string;
  label: string;
  delta?: string;
  deltaDown?: boolean;
  pills?: { label: string; onClick: () => void }[];
  className?: string;
}

export function GradientHeroCard({ value, label, delta, deltaDown, pills, className = '' }: Props) {
  return (
    <motion.div
      initial={{ opacity: 0, y: 16 }}
      animate={{ opacity: 1, y: 0 }}
      className={`relative overflow-hidden bg-gradient-to-br from-purple-1 to-purple-2 rounded-card shadow-purple p-6 text-ink-inv ${className}`}
    >
      {/* decorative circle bleeding off corner */}
      <div className="absolute -top-6 -right-6 w-32 h-32 rounded-full bg-white/8" />
      <div className="absolute top-8 right-12 w-10 h-10 rounded-full bg-white/6" />

      <p className="text-xs font-semibold opacity-70 mb-1 tracking-wide uppercase">{label}</p>
      <p className="text-3xl font-extrabold font-mono tabular-nums">{value}</p>

      {delta && (
        <p className={`inline-flex items-center gap-1 mt-2 px-2.5 py-1 rounded-full text-xs font-semibold ${deltaDown ? 'bg-coral-bg text-coral-1' : 'bg-green-bg text-green-1'}`}>
          {deltaDown ? '↑' : '↓'}{delta}
        </p>
      )}

      {pills && pills.length > 0 && (
        <div className="flex gap-2 mt-4">
          {pills.map((p, i) => (
            <button key={i} onClick={p.onClick} className="px-3 py-1.5 rounded-pill bg-white/15 hover:bg-white/25 text-xs font-semibold transition-colors">
              {p.label}
            </button>
          ))}
        </div>
      )}
    </motion.div>
  );
}
