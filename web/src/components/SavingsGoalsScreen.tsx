import { useState, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { formatAmount } from '../lib/db';

interface SavingsGoal {
  id: string;
  name: string;
  targetAmount: number;
  currentAmount: number;
  metaphor: 'plant' | 'jar' | 'house' | 'rocket';
  targetDate: number | null;
  monthlyContribution: number | null;
}

interface Props {
  onBack: () => void;
}

const STORAGE_KEY = 'nudge-savings-goals';

const DEFAULT_GOALS: SavingsGoal[] = [
  {
    id: 'default-emergency',
    name: 'Emergency Fund',
    targetAmount: 50000,
    currentAmount: 0,
    metaphor: 'jar',
    targetDate: null,
    monthlyContribution: 5000,
  },
  {
    id: 'default-gadget',
    name: 'New Gadget',
    targetAmount: 30000,
    currentAmount: 0,
    metaphor: 'rocket',
    targetDate: null,
    monthlyContribution: 2500,
  },
  {
    id: 'default-vacation',
    name: 'Vacation',
    targetAmount: 100000,
    currentAmount: 0,
    metaphor: 'plant',
    targetDate: null,
    monthlyContribution: 8000,
  },
];

function loadGoals(): SavingsGoal[] {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (raw) {
      const parsed = JSON.parse(raw);
      if (Array.isArray(parsed) && parsed.length > 0) return parsed;
    }
  } catch {
    // fall through
  }
  return DEFAULT_GOALS;
}

function saveGoals(goals: SavingsGoal[]) {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(goals));
}

let goalIdCounter = 0;
function nextGoalId(): string {
  goalIdCounter++;
  return `goal-${Date.now()}-${goalIdCounter}`;
}

const containerVariants = {
  hidden: { opacity: 0 },
  visible: {
    opacity: 1,
    transition: { staggerChildren: 0.1 },
  },
};

const itemVariants = {
  hidden: { opacity: 0, y: 24 },
  visible: { opacity: 1, y: 0, transition: { duration: 0.4 } },
};

const METAPHOR_OPTIONS: { key: SavingsGoal['metaphor']; emoji: string; label: string }[] = [
  { key: 'plant', emoji: '🌱', label: 'Plant' },
  { key: 'jar', emoji: '🫙', label: 'Jar' },
  { key: 'house', emoji: '🏠', label: 'House' },
  { key: 'rocket', emoji: '🚀', label: 'Rocket' },
];

function PlantMetaphor({ progress }: { progress: number }) {
  const stemHeight = Math.max(progress * 40, 2);
  return (
    <svg viewBox="0 0 100 100" className="w-full h-full">
      <rect x="30" y="62" width="40" height="30" rx="4" fill="#8B7355" />
      <rect x="35" y="60" width="30" height="5" rx="2" fill="#A0855B" />
      <g style={{ transformOrigin: '50px 62px', transform: `scaleY(${progress})` }}>
        <line x1="50" y1="62" x2="50" y2="18" stroke="#4CAF50" strokeWidth="4" strokeLinecap="round" />
        <circle cx="38" cy="35" r="8" fill="#66BB6A" />
        <circle cx="62" cy="42" r="8" fill="#66BB6A" />
        <circle cx="44" cy="20" r="7" fill="#81C784" />
      </g>
    </svg>
  );
}

function JarMetaphor({ progress }: { progress: number }) {
  const maxFillHeight = 64;
  const fillHeight = progress * maxFillHeight;
  return (
    <svg viewBox="0 0 100 100" className="w-full h-full">
      <rect x="20" y="14" width="60" height="72" rx="8" fill="none" stroke="var(--color-content-secondary)" strokeWidth="2" />
      <rect x="24" y={14 + 4 + (maxFillHeight - fillHeight)} width="52" height={fillHeight} rx="6" fill="#60A5FA" opacity="0.6" />
      <rect x="22" y="10" width="56" height="8" rx="3" fill="var(--color-content-tertiary)" />
      <line x1="28" y1="24" x2="28" y2="64" stroke="rgba(255,255,255,0.35)" strokeWidth="3" strokeLinecap="round" />
    </svg>
  );
}

function HouseMetaphor({ progress }: { progress: number }) {
  return (
    <svg viewBox="0 0 100 100" className="w-full h-full">
      {progress >= 0.15 && (
        <rect x="12" y="82" width="76" height="10" rx="3" fill="#8B7355" />
      )}
      {progress >= 0.4 && (
        <>
          <rect x="18" y="42" width="64" height="40" fill="#DEB887" />
          <rect x="40" y="52" width="20" height="14" rx="2" fill="#87CEEB" />
          <rect x="42" y="68" width="14" height="14" rx="2" fill="#8B7355" />
        </>
      )}
      {progress >= 0.75 && (
        <>
          <polygon points="8,42 50,10 92,42" fill="#CD853F" />
          <rect x="40" y="28" width="20" height="14" fill="#8B4513" />
        </>
      )}
    </svg>
  );
}

function RocketMetaphor({ progress }: { progress: number }) {
  const translateY = -(progress * 50);
  return (
    <svg viewBox="0 0 100 100" className="w-full h-full">
      <g style={{ transform: `translateY(${translateY}px)`, transition: 'transform 0.5s ease-out' }}>
        <polygon points="42,78 50,98 58,78" fill="#FF6D00" opacity="0.8" />
        <polygon points="46,78 50,90 54,78" fill="#FFAB00" />
        <polygon points="50,8 74,58 50,78 26,58" fill="var(--color-content-secondary)" />
        <rect x="26" y="58" width="48" height="18" fill="var(--color-content-tertiary)" opacity="0.6" />
        <circle cx="50" cy="38" r="10" fill="#4FC3F7" />
        <circle cx="50" cy="38" r="6" fill="#B3E5FC" />
        <polygon points="26,58 16,78 26,74" fill="#F44336" />
        <polygon points="74,58 84,78 74,74" fill="#F44336" />
      </g>
    </svg>
  );
}

function MetaphorSvg({
  metaphor,
  progress,
}: {
  metaphor: SavingsGoal['metaphor'];
  progress: number;
}) {
  const clamped = Math.min(Math.max(progress, 0), 1);
  switch (metaphor) {
    case 'plant':
      return <PlantMetaphor progress={clamped} />;
    case 'jar':
      return <JarMetaphor progress={clamped} />;
    case 'house':
      return <HouseMetaphor progress={clamped} />;
    case 'rocket':
      return <RocketMetaphor progress={clamped} />;
    default:
      return null;
  }
}

export default function SavingsGoalsScreen({ onBack }: Props) {
  const [goals, setGoals] = useState<SavingsGoal[]>(loadGoals);
  const [showModal, setShowModal] = useState(false);
  const [modalName, setModalName] = useState('');
  const [modalAmount, setModalAmount] = useState('');
  const [modalMetaphor, setModalMetaphor] = useState<SavingsGoal['metaphor']>('jar');
  const [modalDate, setModalDate] = useState('');
  const [modalContribution, setModalContribution] = useState('');

  useEffect(() => {
    saveGoals(goals);
  }, [goals]);

  const handleAddGoal = () => {
    if (!modalName.trim() || !modalAmount) return;
    setGoals((prev) => [
      ...prev,
      {
        id: nextGoalId(),
        name: modalName.trim(),
        targetAmount: Math.round(Number(modalAmount)),
        currentAmount: 0,
        metaphor: modalMetaphor,
        targetDate: modalDate ? new Date(modalDate).getTime() : null,
        monthlyContribution: modalContribution ? Number(modalContribution) : null,
      },
    ]);
    setModalName('');
    setModalAmount('');
    setModalMetaphor('jar');
    setModalDate('');
    setModalContribution('');
    setShowModal(false);
  };

  const formatTargetDate = (epoch: number | null) => {
    if (!epoch) return null;
    const d = new Date(epoch);
    return d.toLocaleDateString('en-IN', { month: 'short', year: 'numeric' });
  };

  return (
    <motion.div
      className="min-h-screen bg-lavender-bg"
      initial="hidden"
      animate="visible"
      variants={containerVariants}
    >
      <div className="max-w-2xl mx-auto p-4">
        {/* Header */}
        <div className="flex items-center justify-between mb-6">
          <button
            onClick={onBack}
            className="text-ink-soft text-[13px] hover:text-ink-1 transition-colors"
          >
            ← Back
          </button>
          <h1 className="text-lg font-bold text-ink-1">
            Goals
          </h1>
          <button
            onClick={() => setShowModal(true)}
            className="text-[13px] font-medium text-purple-1 hover:opacity-80 transition-opacity"
          >
            + New Goal
          </button>
        </div>

        {goals.length === 0 ? (
          <div className="text-center py-16">
            <span className="text-5xl block mb-4">💰</span>
            <h2 className="text-lg font-bold text-ink-1 mb-2">
              No Savings Goals
            </h2>
            <p className="text-[11px] text-ink-mute mb-6">
              Create a goal to start tracking your savings progress
            </p>
            <button
              onClick={() => setShowModal(true)}
              className="px-6 py-2 bg-gradient-to-br from-purple-1 to-purple-2 text-ink-inv rounded-pill text-[13px] font-medium"
            >
              Create First Goal
            </button>
          </div>
        ) : (
          <motion.div className="space-y-4" variants={containerVariants}>
            {goals.map((goal) => {
              const progress = goal.targetAmount > 0 ? goal.currentAmount / goal.targetAmount : 0;
              const pct = Math.round(Math.min(progress * 100, 100));
              const targetLabel = formatTargetDate(goal.targetDate);

              return (
                <motion.div
                  key={goal.id}
                  variants={itemVariants}
                  className="rounded-card shadow-card bg-[var(--surface)] p-5"
                >
                  <div className="flex gap-4">
                    {/* SVG metaphor */}
                    <div className="w-20 h-20 flex-shrink-0 rounded-card bg-[var(--lavender-bg)] flex items-center justify-center overflow-hidden">
                      <MetaphorSvg metaphor={goal.metaphor} progress={progress} />
                    </div>

                    {/* Info */}
                    <div className="flex-1 min-w-0">
                      <h3 className="text-[13px] font-semibold text-ink-1 truncate">
                        {goal.name}
                      </h3>

                      {/* Progress bar */}
                      <div className="mt-2 h-2 bg-[var(--lavender-bg)] rounded-full overflow-hidden">
                        <motion.div
                          className="h-full rounded-full"
                          initial={{ width: 0 }}
                          animate={{ width: `${pct}%` }}
                          transition={{ duration: 0.7, ease: 'easeOut' }}
                          style={{
                            background:
                              pct >= 100
                                ? 'var(--green)'
                                : 'linear-gradient(to right, var(--purple), var(--purple-deep))',
                          }}
                        />
                      </div>

                      <div className="flex items-center justify-between mt-1.5">
                        <span className="text-[13px] font-mono tabular-nums font-semibold text-ink-1">
                          ₹{formatAmount(Math.round(goal.currentAmount * 100))}
                          <span className="text-ink-mute font-normal">
                            {' '}
                            / ₹{formatAmount(Math.round(goal.targetAmount * 100))}
                          </span>
                        </span>
                        <span
                          className="text-[11px] font-semibold font-mono tabular-nums"
                          style={{
                            color:
                              pct >= 100
                                ? 'var(--green)'
                                : 'var(--purple)',
                          }}
                        >
                          {pct}%
                        </span>
                      </div>

                      {/* Meta info */}
                      <div className="flex flex-wrap gap-x-4 gap-y-1 mt-2">
                        {targetLabel && (
                          <span className="text-[11px] text-ink-mute">
                            Target: {targetLabel}
                          </span>
                        )}
                        {goal.monthlyContribution && (
                          <span className="text-[11px] text-ink-mute">
                            ₹{goal.monthlyContribution.toLocaleString('en-IN')}/mo
                          </span>
                        )}
                      </div>
                    </div>
                  </div>
                </motion.div>
              );
            })}
          </motion.div>
        )}

        <div className="h-8" />
      </div>

      {/* Add goal modal */}
      <AnimatePresence>
        {showModal && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="fixed inset-0 bg-ink-1/40 flex items-end lg:items-center justify-center z-50"
            onClick={() => setShowModal(false)}
          >
            <motion.div
              initial={{ y: 100, opacity: 0 }}
              animate={{ y: 0, opacity: 1 }}
              exit={{ y: 100, opacity: 0 }}
              className="bg-[var(--surface)] w-full max-w-md rounded-t-3xl lg:rounded-3xl p-6 max-h-[80vh] overflow-y-auto border border-ink-mute/5"
              onClick={(e) => e.stopPropagation()}
            >
              <div className="w-9 h-1 bg-ink-mute rounded-full mx-auto mb-4" />
              <h3 className="text-[14px] font-bold text-ink-1 mb-4">
                Add Savings Goal
              </h3>

              <div className="space-y-3 mb-4">
                <div>
                  <label className="text-[11px] text-ink-mute block mb-1">
                    Goal Name
                  </label>
                  <input
                    type="text"
                    placeholder="e.g. New Bike"
                    value={modalName}
                    onChange={(e) => setModalName(e.target.value)}
                    className="w-full p-3 bg-[var(--lavender-bg)] rounded-chip outline-none text-[13px] text-ink-1 placeholder:text-ink-mute"
                  />
                </div>
                <div>
                  <label className="text-[11px] text-ink-mute block mb-1">
                    Target Amount (₹)
                  </label>
                  <input
                    type="number"
                    min="1"
                    placeholder="e.g. 50000"
                    value={modalAmount}
                    onChange={(e) => setModalAmount(e.target.value)}
                    className="w-full p-3 bg-[var(--lavender-bg)] rounded-chip outline-none text-[13px] text-ink-1 placeholder:text-ink-mute"
                  />
                </div>

                {/* Metaphor selector */}
                <div>
                  <label className="text-[11px] text-ink-mute block mb-1">
                    Visual Metaphor
                  </label>
                  <div className="grid grid-cols-4 gap-2">
                    {METAPHOR_OPTIONS.map((opt) => (
                      <button
                        key={opt.key}
                        onClick={() => setModalMetaphor(opt.key)}
                        className={`flex flex-col items-center gap-1 p-2 rounded-chip transition-colors ${
                          modalMetaphor === opt.key
                            ? 'bg-purple-bg ring-2 ring-purple-1'
                            : 'bg-[var(--lavender-bg)] hover:bg-purple-bg/50'
                        }`}
                      >
                        <span className="text-xl">{opt.emoji}</span>
                        <span className="text-[10px] text-ink-mute">
                          {opt.label}
                        </span>
                      </button>
                    ))}
                  </div>
                </div>

                {/* Target date */}
                <div>
                  <label className="text-[11px] text-ink-mute block mb-1">
                    Target Date (optional)
                  </label>
                  <input
                    type="date"
                    value={modalDate}
                    onChange={(e) => setModalDate(e.target.value)}
                    className="w-full p-3 bg-[var(--lavender-bg)] rounded-chip outline-none text-[13px] text-ink-1"
                  />
                </div>

                {/* Monthly contribution */}
                <div>
                  <label className="text-[11px] text-ink-mute block mb-1">
                    Monthly Contribution (optional)
                  </label>
                  <input
                    type="number"
                    min="0"
                    placeholder="e.g. 5000"
                    value={modalContribution}
                    onChange={(e) => setModalContribution(e.target.value)}
                    className="w-full p-3 bg-[var(--lavender-bg)] rounded-chip outline-none text-[13px] text-ink-1 placeholder:text-ink-mute"
                  />
                </div>
              </div>

              <div className="flex gap-2">
                <button
                  onClick={handleAddGoal}
                  disabled={!modalName.trim() || !modalAmount}
                  className={`flex-1 py-2.5 rounded-chip text-[13px] font-medium ${
                    modalName.trim() && modalAmount
                      ? 'bg-gradient-to-r from-purple-1 to-purple-2 text-ink-inv'
                      : 'bg-[var(--lavender-bg)] text-ink-mute cursor-not-allowed'
                  }`}
                >
                  Create
                </button>
                <button
                  onClick={() => setShowModal(false)}
                  className="px-4 py-2 text-[13px] text-ink-soft hover:text-ink-1 transition-colors"
                >
                  Cancel
                </button>
              </div>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>
    </motion.div>
  );
}
