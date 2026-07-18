import { motion, AnimatePresence, Variants } from 'framer-motion';
import { useEffect, useState } from 'react';

/**
 * Reusable animation components matching §7.3 motion spec.
 * Principles:
 * - Standard duration: 200-300ms small UI, 400-600ms celebrations
 * - Spring physics for touch-driven interactions
 * - Respect prefers-reduced-motion
 */

// ================================================
// Spring configs matching Android specs
// ================================================

export const springBouncy = { type: 'spring' as const, damping: 15, stiffness: 300 };
export const springDefault = { type: 'spring' as const, damping: 20, stiffness: 250 };
export const springStiff = { type: 'spring' as const, damping: 30, stiffness: 500 };

// ================================================
// 1. Card slide-in from bottom
// ================================================

export const cardSlideIn: Variants = {
  hidden: { y: 100, opacity: 0 },
  visible: {
    y: 0,
    opacity: 1,
    transition: { ...springBouncy, delay: 0.05 },
  },
  exit: { y: 50, opacity: 0, transition: { duration: 0.15 } },
};

// ================================================
// 2. Staggered list animation
// ================================================

export const staggerList: Variants = {
  hidden: { opacity: 0 },
  visible: {
    opacity: 1,
    transition: { staggerChildren: 0.06, delayChildren: 0.1 },
  },
};

export const staggerItem: Variants = {
  hidden: { y: 20, opacity: 0 },
  visible: { y: 0, opacity: 1, transition: springDefault },
};

// ================================================
// 3. Amount CountUp component
// ================================================

export function AmountCountUp({
  amountCents,
  prefix = '₹',
  duration = 0.5,
  className = '',
}: {
  amountCents: number;
  prefix?: string;
  duration?: number;
  className?: string;
}) {
  const [display, setDisplay] = useState(0);

  useEffect(() => {
    const target = amountCents / 100;
    const start = display;
    const startTime = performance.now();

    function animate(currentTime: number) {
      const elapsed = (currentTime - startTime) / 1000;
      const progress = Math.min(elapsed / duration, 1);
      // Ease-out cubic
      const eased = 1 - Math.pow(1 - progress, 3);
      setDisplay(start + (target - start) * eased);

      if (progress < 1) {
        requestAnimationFrame(animate);
      }
    }

    requestAnimationFrame(animate);
  }, [amountCents]);

  const formatted = new Intl.NumberFormat('en-IN', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(display);

  return (
    <span className={`font-mono tabular-nums ${className}`}>
      {prefix}
      {formatted}
    </span>
  );
}

// ================================================
// 4. CardSlideIn wrapper
// ================================================

export function CardSlideIn({
  children,
  className = '',
  delay = 0,
}: {
  children: React.ReactNode;
  className?: string;
  delay?: number;
}) {
  return (
    <motion.div
      variants={cardSlideIn}
      initial="hidden"
      animate="visible"
      exit="exit"
      transition={{ ...springBouncy, delay }}
      className={className}
    >
      {children}
    </motion.div>
  );
}

// ================================================
// 5. AnimatedProgressBar — budget progress
// ================================================

export function AnimatedProgressBar({
  progress,
  className = '',
}: {
  progress: number; // 0.0 to 1.5
  className?: string;
}) {
  const clamped = Math.min(progress, 1);
  const color =
    progress > 1
      ? 'var(--color-negative)'
      : progress > 0.8
        ? 'var(--color-warning)'
        : 'var(--color-accent-primary)';

  return (
    <div className={`h-2 bg-surface-base rounded-full overflow-hidden ${className}`}>
      <motion.div
        className="h-full rounded-full"
        style={{ backgroundColor: color }}
        initial={{ width: 0 }}
        animate={{ width: `${clamped * 100}%` }}
        transition={{ duration: 0.5, ease: 'easeOut' }}
      />
    </div>
  );
}

// ================================================
// 6. Celebration overlay — full screen
// ================================================

export function CelebrationOverlay({
  isVisible,
  title,
  subtitle,
  emoji = '🎉',
  onDismiss,
}: {
  isVisible: boolean;
  title: string;
  subtitle?: string;
  emoji?: string;
  onDismiss: () => void;
}) {
  return (
    <AnimatePresence>
      {isVisible && (
        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          exit={{ opacity: 0 }}
          transition={{ duration: 0.3 }}
          className="fixed inset-0 z-50 flex items-center justify-center bg-black/80"
          onClick={onDismiss}
        >
          <motion.div
            initial={{ scale: 0.5, opacity: 0 }}
            animate={{ scale: 1, opacity: 1 }}
            exit={{ scale: 0.5, opacity: 0 }}
            transition={springBouncy}
            className="text-center"
          >
            {/* Radial burst */}
            <motion.div
              initial={{ scale: 0 }}
              animate={{ scale: 2.5 }}
              transition={{ duration: 0.8, ease: 'easeOut' }}
              className="absolute inset-0 w-48 h-48 mx-auto rounded-full bg-accent-primary/20"
            />
            <span className="text-6xl block relative z-10">{emoji}</span>
            <h2 className="text-display font-display font-bold text-white mt-4 relative z-10">
              {title}
            </h2>
            {subtitle && (
              <p className="text-body text-content-secondary mt-2 relative z-10">
                {subtitle}
              </p>
            )}
            <button
              onClick={(e) => {
                e.stopPropagation();
                onDismiss();
              }}
              className="mt-6 px-6 py-2 bg-accent-primary text-white rounded-pill text-body font-medium relative z-10"
            >
              Nice!
            </button>
          </motion.div>
        </motion.div>
      )}
    </AnimatePresence>
  );
}

// ================================================
// 7. Shake on error
// ================================================

export function ShakeOnError({
  isError,
  children,
}: {
  isError: boolean;
  children: React.ReactNode;
}) {
  return (
    <motion.div
      animate={isError ? { x: [0, -3, 3, -3, 3, 0] } : {}}
      transition={{ duration: 0.3 }}
    >
      {children}
    </motion.div>
  );
}

// ================================================
// 8. Hooks — useReducedMotion
// ================================================

export function useReducedMotion(): boolean {
  const [reduced, setReduced] = useState(
    () => window.matchMedia('(prefers-reduced-motion: reduce)').matches
  );

  useEffect(() => {
    const mq = window.matchMedia('(prefers-reduced-motion: reduce)');
    const handler = (e: MediaQueryListEvent) => setReduced(e.matches);
    mq.addEventListener('change', handler);
    return () => mq.removeEventListener('change', handler);
  }, []);

  return reduced;
}
