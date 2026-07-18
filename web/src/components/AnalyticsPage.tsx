import { useState, useMemo, useCallback } from 'react';
import { motion } from 'framer-motion';
import {
  AreaChart,
  Area,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  PieChart,
  Pie,
  Cell,
} from 'recharts';
import type { Transaction, Category, Budget } from '../lib/db';
import { formatAmount } from '../lib/db';
import { categoryColor } from '../lib/engine';

interface Props {
  transactions: Transaction[];
  categories: Category[];
  budgets: Budget[];
  onBack: () => void;
}

type ViewMode = 'weekly' | 'monthly' | 'yearly';

const MONTH_NAMES = [
  'Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun',
  'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec',
];

const DAY_NAMES = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'];
const DAY_NAMES_SHORT = ['S', 'M', 'T', 'W', 'T', 'F', 'S'];

function daysInMonth(year: number, month: number): number {
  return new Date(year, month + 1, 0).getDate();
}

function isSameDay(a: Date, b: Date): boolean {
  return (
    a.getFullYear() === b.getFullYear() &&
    a.getMonth() === b.getMonth() &&
    a.getDate() === b.getDate()
  );
}

function getStartOfWeek(d: Date): Date {
  const date = new Date(d);
  const day = date.getDay();
  const diff = day === 0 ? -6 : 1 - day;
  date.setDate(date.getDate() + diff);
  date.setHours(0, 0, 0, 0);
  return date;
}

const containerVariants = {
  hidden: { opacity: 0 },
  visible: { opacity: 1, transition: { staggerChildren: 0.07 } },
};

const itemVariants = {
  hidden: { opacity: 0, y: 20 },
  visible: { opacity: 1, y: 0, transition: { duration: 0.4 } },
};

const tabClasses = (active: boolean) =>
  `px-4 py-1.5 rounded-pill text-caption font-medium transition-colors ${
    active
      ? 'bg-accent-primary text-white'
      : 'text-content-secondary hover:text-content-primary hover:bg-surface-base'
  }`;

interface TooltipPayloadEntry {
  name: string;
  value: number;
  color: string;
}

function ChartTooltip({ active, payload, label }: any) {
  if (!active || !payload?.length) return null;
  return (
    <div
      className="bg-surface-raised p-3 rounded-md border border-surface-base"
      style={{ boxShadow: 'var(--shadow-md)' }}
    >
      <p className="text-caption text-content-secondary mb-1">{label}</p>
      {payload.map((entry: TooltipPayloadEntry, i: number) => (
        <p
          key={i}
          className="font-mono tabular-nums text-body font-semibold"
          style={{ color: entry.color }}
        >
          {entry.name}: ₹{formatAmount(Math.round(entry.value * 100))}
        </p>
      ))}
    </div>
  );
}

function PieTooltip({ active, payload }: any) {
  if (!active || !payload?.length) return null;
  const entry = payload[0];
  return (
    <div
      className="bg-surface-raised p-3 rounded-md"
      style={{ boxShadow: 'var(--shadow-md)' }}
    >
      <p className="text-caption font-medium" style={{ color: entry.payload.color }}>
        {entry.name}
      </p>
      <p className="font-mono tabular-nums text-body font-semibold text-content-primary">
        ₹{formatAmount(Math.round(entry.value * 100))}
      </p>
    </div>
  );
}

function getHeatmapIntensity(amount: number, max: number): number {
  if (amount <= 0 || max <= 0) return 0;
  const ratio = amount / max;
  if (ratio <= 0.25) return 1;
  if (ratio <= 0.5) return 2;
  if (ratio <= 0.75) return 3;
  return 4;
}

function getHeatmapStyle(level: number): React.CSSProperties {
  if (level === 0) return {};
  const opacities = [0, 0.15, 0.4, 0.7, 1];
  return {
    backgroundColor: 'var(--color-accent-primary)',
    opacity: opacities[level],
  };
}

function getBudgetColor(pct: number): string {
  if (pct <= 0.5) return 'var(--color-accent-primary)';
  if (pct <= 0.8) return 'var(--color-warning)';
  return 'var(--color-negative)';
}

export default function AnalyticsPage({
  transactions,
  categories,
  budgets,
  onBack,
}: Props) {
  const [viewMode, setViewMode] = useState<ViewMode>('monthly');
  const [heatmapMonth, setHeatmapMonth] = useState(new Date().getMonth());
  const [heatmapYear, setHeatmapYear] = useState(new Date().getFullYear());
  const [heatmapTooltip, setHeatmapTooltip] = useState<{
    date: string;
    amount: number;
    ix: number;
    iy: number;
  } | null>(null);

  const now = useMemo(() => new Date(), []);
  const currentYear = now.getFullYear();
  const currentMonth = now.getMonth();

  const expenseCategories = useMemo(
    () => categories.filter((c) => c.type === 'expense'),
    [categories],
  );

  const debits = useMemo(
    () => transactions.filter((t) => t.type === 'debit'),
    [transactions],
  );

  const categoryMap = useMemo(() => {
    const m = new Map<string, Category>();
    for (const c of categories) m.set(c.id, c);
    return m;
  }, [categories]);

  // ── Spending Trends ──────────────────────────────────────────────

  const trendData = useMemo(() => {
    if (viewMode === 'monthly') {
      const thisMonthDays = daysInMonth(currentYear, currentMonth);
      const prevMonth = currentMonth === 0 ? 11 : currentMonth - 1;
      const prevYear = currentMonth === 0 ? currentYear - 1 : currentYear;
      const prevMonthDays = daysInMonth(prevYear, prevMonth);

      const maxDays = Math.max(thisMonthDays, prevMonthDays);
      const days: { day: number; current: number; prev: number }[] = [];

      for (let d = 1; d <= maxDays; d++) {
        let currentSpend = 0;
        let prevSpend = 0;

        if (d <= thisMonthDays) {
          const date = new Date(currentYear, currentMonth, d);
          currentSpend = debits
            .filter((t) => isSameDay(new Date(t.timestampEpoch), date))
            .reduce((s, t) => s + t.amountCents, 0) / 100;
        }
        if (d <= prevMonthDays) {
          const date = new Date(prevYear, prevMonth, d);
          prevSpend = debits
            .filter((t) => isSameDay(new Date(t.timestampEpoch), date))
            .reduce((s, t) => s + t.amountCents, 0) / 100;
        }

        days.push({ day: d, current: currentSpend, prev: prevSpend });
      }
      return days;
    }

    if (viewMode === 'weekly') {
      const weeks: { day: string; current: number; prev: number }[] = [];
      const nowStart = getStartOfWeek(now);
      for (let w = 0; w < 4; w++) {
        const wStart = new Date(nowStart);
        wStart.setDate(wStart.getDate() - w * 7);
        const wEnd = new Date(wStart);
        wEnd.setDate(wEnd.getDate() + 6);
        wEnd.setHours(23, 59, 59, 999);

        const prevWStart = new Date(wStart);
        prevWStart.setDate(prevWStart.getDate() - 7);
        const prevWEnd = new Date(wEnd);
        prevWEnd.setDate(prevWEnd.getDate() - 7);

        const currentSpend = debits
          .filter(
            (t) =>
              t.timestampEpoch >= wStart.getTime() &&
              t.timestampEpoch <= wEnd.getTime(),
          )
          .reduce((s, t) => s + t.amountCents, 0) / 100;

        const prevSpend = debits
          .filter(
            (t) =>
              t.timestampEpoch >= prevWStart.getTime() &&
              t.timestampEpoch <= prevWEnd.getTime(),
          )
          .reduce((s, t) => s + t.amountCents, 0) / 100;

        weeks.unshift({
          day: `W${w + 1}`,
          current: currentSpend,
          prev: prevSpend,
        });
      }
      return weeks;
    }

    // yearly
    const months: { day: string; current: number; prev: number }[] = [];
    for (let m = 0; m < 12; m++) {
      const mStart = new Date(currentYear, m, 1).getTime();
      const mEnd = new Date(currentYear, m + 1, 0, 23, 59, 59, 999).getTime();
      const pmStart = new Date(currentYear - 1, m, 1).getTime();
      const pmEnd = new Date(currentYear - 1, m + 1, 0, 23, 59, 59, 999).getTime();

      months.push({
        day: MONTH_NAMES[m],
        current:
          debits
            .filter(
              (t) =>
                t.timestampEpoch >= mStart && t.timestampEpoch <= mEnd,
            )
            .reduce((s, t) => s + t.amountCents, 0) / 100,
        prev:
          debits
            .filter(
              (t) =>
                t.timestampEpoch >= pmStart && t.timestampEpoch <= pmEnd,
            )
            .reduce((s, t) => s + t.amountCents, 0) / 100,
      });
    }
    return months;
  }, [viewMode, currentYear, currentMonth, debits, now]);

  const xKey = viewMode === 'monthly' ? 'day' : 'day';

  // ── Category Breakdown ───────────────────────────────────────────

  const categoryBreakdown = useMemo(() => {
    const nowStart = new Date(currentYear, currentMonth, 1).getTime();
    const nowEnd = new Date(currentYear, currentMonth + 1, 0, 23, 59, 59, 999).getTime();
    const monthDebits = debits.filter(
      (t) => t.timestampEpoch >= nowStart && t.timestampEpoch <= nowEnd,
    );

    const byCategory = new Map<string, { name: string; amount: number; color: string }>();
    let unassigned = 0;

    for (const t of monthDebits) {
      if (t.categoryId && categoryMap.has(t.categoryId)) {
        const cat = categoryMap.get(t.categoryId)!;
        const key = cat.id;
        if (!byCategory.has(key)) {
          byCategory.set(key, {
            name: cat.name,
            amount: 0,
            color: cat.color || categoryColor(byCategory.size),
          });
        }
        byCategory.get(key)!.amount += t.amountCents;
      } else {
        unassigned += t.amountCents;
      }
    }

    const result = Array.from(byCategory.values())
      .map((c) => ({ ...c, amount: c.amount / 100 }))
      .sort((a, b) => b.amount - a.amount);

    if (unassigned > 0) {
      result.push({ name: 'Uncategorized', amount: unassigned / 100, color: 'var(--color-content-tertiary)' });
    }

    return result;
  }, [debits, currentYear, currentMonth, categoryMap]);

  const totalCategorySpend = useMemo(
    () => categoryBreakdown.reduce((s, c) => s + c.amount * 100, 0),
    [categoryBreakdown],
  );

  // ── Merchant Leaderboard ─────────────────────────────────────────

  const merchantLeaderboard = useMemo(() => {
    const nowStart = new Date(currentYear, currentMonth, 1).getTime();
    const nowEnd = new Date(currentYear, currentMonth + 1, 0, 23, 59, 59, 999).getTime();
    const monthDebits = debits.filter(
      (t) => t.timestampEpoch >= nowStart && t.timestampEpoch <= nowEnd,
    );

    const byMerchant = new Map<
      string,
      { name: string; total: number; count: number }
    >();
    for (const t of monthDebits) {
      const name = t.merchantNormalized || t.merchantRaw;
      if (!byMerchant.has(name)) {
        byMerchant.set(name, { name, total: 0, count: 0 });
      }
      const entry = byMerchant.get(name)!;
      entry.total += t.amountCents;
      entry.count += 1;
    }

    return Array.from(byMerchant.values())
      .sort((a, b) => b.total - a.total)
      .slice(0, 10);
  }, [debits, currentYear, currentMonth]);

  const maxMerchantSpend = useMemo(
    () => (merchantLeaderboard.length > 0 ? merchantLeaderboard[0].total : 1),
    [merchantLeaderboard],
  );

  // ── Calendar Heatmap ─────────────────────────────────────────────

  const heatmapData = useMemo(() => {
    const totalDays = daysInMonth(heatmapYear, heatmapMonth);
    const firstDay = new Date(heatmapYear, heatmapMonth, 1).getDay();
    const offset = firstDay === 0 ? 6 : firstDay - 1;
    const totalCells = offset + totalDays;
    const columns = Math.ceil(totalCells / 7);

    const dailyAmounts = new Map<number, number>();
    for (const t of debits) {
      const d = new Date(t.timestampEpoch);
      if (d.getFullYear() === heatmapYear && d.getMonth() === heatmapMonth) {
        const day = d.getDate();
        dailyAmounts.set(day, (dailyAmounts.get(day) || 0) + t.amountCents);
      }
    }

    const maxAmount = dailyAmounts.size > 0 ? Math.max(...dailyAmounts.values()) : 0;

    const cells: { day: number | null; amount: number; label: string }[] = [];
    for (let i = 0; i < columns * 7; i++) {
      const dayNum = i - offset + 1;
      if (dayNum >= 1 && dayNum <= totalDays) {
        const amount = dailyAmounts.get(dayNum) || 0;
        cells.push({
          day: dayNum,
          amount,
          label: `${dayNum} ${MONTH_NAMES[heatmapMonth]} ${heatmapYear}`,
        });
      } else {
        cells.push({ day: null, amount: 0, label: '' });
      }
    }

    return { cells, columns, maxAmount };
  }, [debits, heatmapYear, heatmapMonth]);

  const handleHeatmapPrev = useCallback(() => {
    if (heatmapMonth === 0) {
      setHeatmapMonth(11);
      setHeatmapYear((y) => y - 1);
    } else {
      setHeatmapMonth((m) => m - 1);
    }
    setHeatmapTooltip(null);
  }, [heatmapMonth]);

  const handleHeatmapNext = useCallback(() => {
    if (heatmapMonth === 11) {
      setHeatmapMonth(0);
      setHeatmapYear((y) => y + 1);
    } else {
      setHeatmapMonth((m) => m + 1);
    }
    setHeatmapTooltip(null);
  }, [heatmapMonth]);

  // ── Budget vs Actual ────────────────────────────────────────────

  const budgetActuals = useMemo(() => {
    return budgets.map((budget) => {
      const cat = budget.categoryId ? categoryMap.get(budget.categoryId) : null;

      let periodStart: number;
      let periodEnd: number;

      if (budget.period === 'weekly') {
        const ws = getStartOfWeek(now);
        periodStart = ws.getTime();
        const we = new Date(ws);
        we.setDate(we.getDate() + 6);
        we.setHours(23, 59, 59, 999);
        periodEnd = we.getTime();
      } else {
        periodStart = new Date(currentYear, currentMonth, 1).getTime();
        periodEnd = new Date(currentYear, currentMonth + 1, 0, 23, 59, 59, 999).getTime();
      }

      const spent = debits
        .filter(
          (t) =>
            t.timestampEpoch >= periodStart &&
            t.timestampEpoch <= periodEnd &&
            (budget.categoryId === null || t.categoryId === budget.categoryId),
        )
        .reduce((s, t) => s + t.amountCents, 0);

      const pct = budget.amountCents > 0 ? spent / budget.amountCents : 0;

      return {
        id: budget.id,
        categoryName: cat?.name || 'All Categories',
        categoryIcon: cat?.icon || null,
        spent,
        budget: budget.amountCents,
        pct,
        color: getBudgetColor(pct),
      };
    });
  }, [budgets, categoryMap, currentYear, currentMonth, debits, now]);

  // ── Render ───────────────────────────────────────────────────────

  return (
    <motion.div
      className="min-h-screen bg-surface-base"
      initial="hidden"
      animate="visible"
      variants={containerVariants}
    >
      <div className="max-w-7xl mx-auto p-4 lg:p-8">
        {/* Header */}
        <div className="flex items-center justify-between mb-6">
          <button
            onClick={onBack}
            className="text-content-secondary text-body hover:text-content-primary transition-colors"
          >
            ← Back
          </button>
          <h1 className="text-title font-display font-bold text-content-primary">
            Analytics
          </h1>
          <div className="w-16" />
        </div>

        {/* ── 1. Spending Trends ────────────────────────────────── */}
        <motion.div
          variants={itemVariants}
          className="p-5 lg:p-6 bg-surface-raised rounded-xl mb-4"
          style={{ boxShadow: 'var(--shadow-md)' }}
        >
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 mb-4">
            <h2 className="text-heading font-semibold text-content-primary">
              Spending Trends
            </h2>
            <div className="flex gap-1 bg-surface-base rounded-pill p-1 w-fit">
              {(['weekly', 'monthly', 'yearly'] as ViewMode[]).map((m) => (
                <button
                  key={m}
                  onClick={() => setViewMode(m)}
                  className={tabClasses(viewMode === m)}
                >
                  {m.charAt(0).toUpperCase() + m.slice(1)}
                </button>
              ))}
            </div>
          </div>

          {trendData.length === 0 ? (
            <div className="text-center py-16 text-content-tertiary text-body">
              No spending data for this period
            </div>
          ) : (
            <ResponsiveContainer width="100%" height={320}>
              <AreaChart
                data={trendData}
                margin={{ top: 8, right: 8, left: -16, bottom: 0 }}
              >
                <defs>
                  <linearGradient id="accentFill" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="0%" stopColor="var(--color-accent-primary)" stopOpacity={0.3} />
                    <stop offset="100%" stopColor="var(--color-accent-primary)" stopOpacity={0.02} />
                  </linearGradient>
                </defs>
                <CartesianGrid
                  strokeDasharray="3 3"
                  stroke="var(--color-content-tertiary)"
                  strokeOpacity={0.15}
                />
                <XAxis
                  dataKey={xKey}
                  tick={{ fill: 'var(--color-content-tertiary)', fontSize: 12 }}
                  axisLine={{ stroke: 'var(--color-content-tertiary)', strokeOpacity: 0.2 }}
                  tickLine={false}
                />
                <YAxis
                  tick={{ fill: 'var(--color-content-tertiary)', fontSize: 12 }}
                  axisLine={false}
                  tickLine={false}
                  tickFormatter={(v: number) => `₹${v}`}
                />
                <Tooltip content={<ChartTooltip />} />
                <Area
                  type="monotone"
                  dataKey="current"
                  stroke="var(--color-accent-primary)"
                  strokeWidth={2}
                  fillOpacity={1}
                  fill="url(#accentFill)"
                  name="This period"
                />
                {viewMode !== 'weekly' && (
                  <Area
                    type="monotone"
                    dataKey="prev"
                    stroke="var(--color-content-tertiary)"
                    strokeWidth={1.5}
                    strokeDasharray="5 5"
                    fill="none"
                    name="Previous"
                  />
                )}
              </AreaChart>
            </ResponsiveContainer>
          )}
        </motion.div>

        {/* ── 2. Category + Merchant (side by side) ──────────────── */}
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-4 mb-4">
          {/* Category Breakdown */}
          <motion.div
            variants={itemVariants}
            className="p-5 lg:p-6 bg-surface-raised rounded-xl"
            style={{ boxShadow: 'var(--shadow-md)' }}
          >
            <h2 className="text-heading font-semibold text-content-primary mb-4">
              Category Breakdown
            </h2>

            {categoryBreakdown.length === 0 ? (
              <div className="text-center py-16 text-content-tertiary text-body">
                No categorized expenses this month
              </div>
            ) : (
              <>
                <div className="relative flex justify-center">
                  <PieChart width={240} height={240}>
                    <Pie
                      data={categoryBreakdown}
                      cx="50%"
                      cy="50%"
                      innerRadius={65}
                      outerRadius={100}
                      dataKey="amount"
                      nameKey="name"
                      paddingAngle={2}
                      stroke="none"
                    >
                      {categoryBreakdown.map((entry, i) => (
                        <Cell key={i} fill={entry.color} />
                      ))}
                    </Pie>
                    <Tooltip content={<PieTooltip />} />
                  </PieChart>
                  <div className="absolute inset-0 flex flex-col items-center justify-center pointer-events-none">
                    <p className="text-micro text-content-tertiary">Total</p>
                    <p className="text-heading font-bold font-mono tabular-nums text-content-primary">
                      ₹{formatAmount(totalCategorySpend)}
                    </p>
                  </div>
                </div>

                <div className="mt-4 space-y-2 max-h-48 overflow-y-auto">
                  {categoryBreakdown.map((entry) => (
                    <div
                      key={entry.name}
                      className="flex items-center justify-between text-caption"
                    >
                      <div className="flex items-center gap-2">
                        <span
                          className="w-2.5 h-2.5 rounded-full flex-shrink-0"
                          style={{ backgroundColor: entry.color }}
                        />
                        <span className="text-content-secondary truncate max-w-[120px]">
                          {entry.name}
                        </span>
                      </div>
                      <div className="flex items-center gap-3 font-mono tabular-nums">
                        <span className="text-content-primary font-medium">
                          ₹{formatAmount(Math.round(entry.amount * 100))}
                        </span>
                        <span className="text-content-tertiary">
                          {totalCategorySpend > 0
                            ? ((entry.amount * 100) / totalCategorySpend * 100).toFixed(1)
                            : '0.0'}
                          %
                        </span>
                      </div>
                    </div>
                  ))}
                </div>
              </>
            )}
          </motion.div>

          {/* Merchant Leaderboard */}
          <motion.div
            variants={itemVariants}
            className="p-5 lg:p-6 bg-surface-raised rounded-xl"
            style={{ boxShadow: 'var(--shadow-md)' }}
          >
            <h2 className="text-heading font-semibold text-content-primary mb-4">
              Merchant Leaderboard
            </h2>

            {merchantLeaderboard.length === 0 ? (
              <div className="text-center py-16 text-content-tertiary text-body">
                No merchant data this month
              </div>
            ) : (
              <div className="space-y-3">
                {merchantLeaderboard.map((m, i) => (
                  <div key={m.name}>
                    <div className="flex items-center justify-between mb-1">
                      <div className="flex items-center gap-2 min-w-0">
                        <span className="text-micro font-mono text-content-tertiary w-4 text-right">
                          {i + 1}
                        </span>
                        <span className="text-caption text-content-primary truncate max-w-[140px]">
                          {m.name}
                        </span>
                      </div>
                      <div className="flex items-center gap-3 flex-shrink-0">
                        <span className="text-caption text-content-tertiary">
                          {m.count}
                        </span>
                        <span className="font-mono tabular-nums text-caption font-medium text-content-primary">
                          ₹{formatAmount(m.total)}
                        </span>
                      </div>
                    </div>
                    <div className="h-1.5 bg-surface-base rounded-full overflow-hidden">
                      <div
                        className="h-full rounded-full transition-all duration-500"
                        style={{
                          width: `${(m.total / maxMerchantSpend) * 100}%`,
                          backgroundColor: 'var(--color-accent-primary)',
                          opacity: 0.2 + (m.total / maxMerchantSpend) * 0.8,
                        }}
                      />
                    </div>
                  </div>
                ))}
              </div>
            )}
          </motion.div>
        </div>

        {/* ── 3. Cash-Flow Calendar Heatmap ──────────────────────── */}
        <motion.div
          variants={itemVariants}
          className="p-5 lg:p-6 bg-surface-raised rounded-xl mb-4"
          style={{ boxShadow: 'var(--shadow-md)' }}
        >
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-heading font-semibold text-content-primary">
              Cash Flow Calendar
            </h2>
            <div className="flex items-center gap-2">
              <button
                onClick={handleHeatmapPrev}
                className="w-7 h-7 flex items-center justify-center rounded-md text-content-secondary hover:text-content-primary hover:bg-surface-base transition-colors text-sm"
              >
                ‹
              </button>
              <span className="text-body font-medium text-content-primary min-w-[120px] text-center">
                {MONTH_NAMES[heatmapMonth]} {heatmapYear}
              </span>
              <button
                onClick={handleHeatmapNext}
                className="w-7 h-7 flex items-center justify-center rounded-md text-content-secondary hover:text-content-primary hover:bg-surface-base transition-colors text-sm"
              >
                ›
              </button>
            </div>
          </div>

          {/* Day headers */}
          <div
            className="grid gap-1 mb-1"
            style={{ gridTemplateColumns: `repeat(${heatmapData.columns}, 1fr)` }}
          >
            {Array.from({ length: heatmapData.columns }, (_, ci) => (
              <div key={ci} className="text-center text-micro text-content-tertiary leading-none">
                &nbsp;
              </div>
            ))}
          </div>

          <div className="flex gap-1">
            {/* Row labels */}
            <div className="flex flex-col gap-1 mr-0.5">
              {DAY_NAMES_SHORT.map((d, ri) => (
                <div
                  key={ri}
                  className="w-4 h-3 flex items-center justify-end text-micro text-content-tertiary leading-none"
                >
                  {d}
                </div>
              ))}
            </div>

            {/* Heatmap grid */}
            <div
              className="grid gap-1 flex-1"
              style={{
                gridTemplateColumns: `repeat(${heatmapData.columns}, 1fr)`,
                gridTemplateRows: 'repeat(7, 1fr)',
                gridAutoFlow: 'column',
              }}
            >
              {heatmapData.cells.map((cell, i) => {
                const ri = i % 7;
                const isEmpty = cell.day === null;
                const intensity = isEmpty
                  ? 0
                  : getHeatmapIntensity(cell.amount, heatmapData.maxAmount);
                const showDayLabel = ri === 0 && cell.day !== null && cell.day <= 7;

                return (
                  <div
                    key={i}
                    className="relative aspect-square rounded-sm transition-transform duration-150"
                    style={{
                      ...(isEmpty
                        ? { backgroundColor: 'transparent' }
                        : getHeatmapStyle(intensity)),
                      ...(!isEmpty ? { cursor: 'pointer' } : {}),
                    }}
                    onMouseEnter={(e) => {
                      if (isEmpty) return;
                      const rect = (e.target as HTMLElement).getBoundingClientRect();
                      setHeatmapTooltip({
                        date: cell.label,
                        amount: cell.amount,
                        ix: rect.left + rect.width / 2,
                        iy: rect.top,
                      });
                    }}
                    onMouseLeave={() => setHeatmapTooltip(null)}
                    title={isEmpty ? '' : `${cell.label}: ₹${formatAmount(cell.amount)}`}
                  >
                    {showDayLabel && (
                      <span className="absolute -top-3 -left-0.5 text-micro text-content-tertiary leading-none">
                        {cell.day}
                      </span>
                    )}
                  </div>
                );
              })}
            </div>
          </div>

          {/* Legend */}
          <div className="flex items-center justify-end gap-1.5 mt-3">
            <span className="text-micro text-content-tertiary">Less</span>
            {[0, 1, 2, 3, 4].map((level) => (
              <div
                key={level}
                className="w-3 h-3 rounded-sm"
                style={
                  level === 0
                    ? { backgroundColor: 'var(--color-surface-base)' }
                    : getHeatmapStyle(level)
                }
              />
            ))}
            <span className="text-micro text-content-tertiary">More</span>
          </div>

          {/* Tooltip */}
          {heatmapTooltip && (
            <div
              className="fixed z-50 bg-content-primary text-surface-raised px-2.5 py-1.5 rounded-md pointer-events-none text-caption font-mono"
              style={{
                left: heatmapTooltip.ix,
                top: heatmapTooltip.iy - 36,
                transform: 'translateX(-50%)',
              }}
            >
              <p className="text-micro opacity-80">{heatmapTooltip.date}</p>
              <p className="font-semibold">₹{formatAmount(heatmapTooltip.amount)}</p>
            </div>
          )}
        </motion.div>

        {/* ── 4. Budget vs Actual ────────────────────────────────── */}
        <motion.div
          variants={itemVariants}
          className="p-5 lg:p-6 bg-surface-raised rounded-xl"
          style={{ boxShadow: 'var(--shadow-md)' }}
        >
          <h2 className="text-heading font-semibold text-content-primary mb-4">
            Budget vs Actual
          </h2>

          {budgetActuals.length === 0 ? (
            <div className="text-center py-12 text-content-tertiary text-body">
              No budgets set. Create budgets to track your spending limits.
            </div>
          ) : (
            <div className="space-y-4">
              {budgetActuals.map((b) => (
                <div key={b.id}>
                  <div className="flex items-center justify-between mb-1.5">
                    <div className="flex items-center gap-2">
                      {b.categoryIcon && (
                        <span className="text-sm">{b.categoryIcon}</span>
                      )}
                      <span className="text-body font-medium text-content-primary">
                        {b.categoryName}
                      </span>
                    </div>
                    <span className="font-mono tabular-nums text-caption text-content-secondary">
                      Spent{' '}
                      <span
                        className="font-semibold"
                        style={{ color: b.color }}
                      >
                        ₹{formatAmount(b.spent)}
                      </span>{' '}
                      of ₹{formatAmount(b.budget)}
                    </span>
                  </div>
                  <div className="h-2.5 bg-surface-base rounded-full overflow-hidden">
                    <motion.div
                      className="h-full rounded-full"
                      initial={{ width: 0 }}
                      animate={{ width: `${Math.min(b.pct * 100, 100)}%` }}
                      transition={{ duration: 0.7, ease: 'easeOut' }}
                      style={{ backgroundColor: b.color }}
                    />
                  </div>
                  {b.pct > 1 && (
                    <p className="text-micro text-negative mt-0.5 text-right">
                      {((b.pct - 1) * 100).toFixed(0)}% over budget
                    </p>
                  )}
                </div>
              ))}
            </div>
          )}
        </motion.div>

        {/* Bottom spacer */}
        <div className="h-8" />
      </div>
    </motion.div>
  );
}
