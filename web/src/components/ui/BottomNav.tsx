import { motion } from 'framer-motion';
import type { ReactNode } from 'react';

interface Props {
  items: { id: string; icon: ReactNode; label: string; badge?: number }[];
  activeId: string;
  onSelect: (id: string) => void;
  onFabClick?: () => void;
  fabIcon?: ReactNode;
  className?: string;
}

export function BottomNav({
  items,
  activeId,
  onSelect,
  onFabClick,
  fabIcon,
  className = '',
}: Props) {
  const hasFab = items.length % 2 === 1;
  const midIndex = Math.floor(items.length / 2);

  return (
    <motion.nav
      initial={{ y: 60, opacity: 0 }}
      animate={{ y: 0, opacity: 1 }}
      className={`fixed bottom-6 left-1/2 -translate-x-1/2 w-fit max-w-[90vw] z-50 ${className}`}
    >
      <div className="flex items-center gap-1 px-2 py-2 rounded-pill bg-[var(--surface)] shadow-card-md">
        {items.map((item, i) => {
          const isActive = item.id === activeId;
          const isCenterFab = hasFab && i === midIndex;

          if (isCenterFab) {
            return (
              <motion.button
                key={item.id}
                whileTap={{ scale: 0.9 }}
                onClick={onFabClick}
                className="-mt-8 flex-shrink-0 relative z-10 mx-0.5 w-12 h-12 rounded-full bg-gradient-to-br from-purple-1 to-purple-2 shadow-purple flex items-center justify-center text-white text-xl"
                aria-label={item.label}
              >
                {fabIcon || item.icon}
              </motion.button>
            );
          }

          return (
            <motion.button
              key={item.id}
              whileTap={{ scale: 0.92 }}
              onClick={() => onSelect(item.id)}
              className={`relative flex flex-col items-center justify-center w-11 h-11 rounded-xl transition-colors ${
                isActive
                  ? 'bg-purple-bg text-purple-1'
                  : 'text-ink-mute hover:text-ink-soft'
              }`}
              aria-label={item.label}
            >
              <span className="text-lg leading-none">{item.icon}</span>
              <span className="text-[10px] font-medium mt-0.5">{item.label}</span>

              {item.badge !== undefined && item.badge > 0 && (
                <span className="absolute -top-0.5 -right-0.5 min-w-[16px] h-4 px-1 rounded-full bg-coral-1 text-white text-[9px] font-bold flex items-center justify-center leading-none">
                  {item.badge > 9 ? '9+' : item.badge}
                </span>
              )}
            </motion.button>
          );
        })}
      </div>
    </motion.nav>
  );
}
