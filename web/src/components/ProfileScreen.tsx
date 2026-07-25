import { motion } from 'framer-motion';
import {
  IconCloud, IconDatabase, IconFileImport, IconMoon, IconShield,
  IconSun, IconTag, IconTrophy, IconTarget, IconUser,
} from '../lib/icons';

type Destination = 'achievements' | 'challenges' | 'goals' | 'csv' | 'backup' | 'merchants' | 'sync';

interface Props {
  name: string;
  dark: boolean;
  level: number;
  xp: number;
  onToggleTheme: () => void;
  onNavigate: (destination: Destination) => void;
  onResetOnboarding: () => void;
}

const groups = [
  {
    title: 'Grow',
    items: [
      ['achievements', IconTrophy, 'Achievements', 'Badges and milestones'],
      ['challenges', IconTarget, 'Challenges', 'Build a healthier rhythm'],
      ['goals', IconCloud, 'Savings goals', 'Plan for what matters'],
    ],
  },
  {
    title: 'Your data',
    items: [
      ['csv', IconFileImport, 'Import transactions', 'Bring a CSV from your bank'],
      ['backup', IconDatabase, 'Backup & export', 'Portable, encrypted copies'],
      ['merchants', IconTag, 'Merchant rules', 'Names and categories Nudge learned'],
      ['sync', IconCloud, 'Device sync', 'Optional end-to-end encrypted sync'],
    ],
  },
] as const;

export default function ProfileScreen({ name, dark, level, xp, onToggleTheme, onNavigate, onResetOnboarding }: Props) {
  return (
    <div className="screen-wrap profile-screen">
      <motion.section initial={{ opacity: 0, y: 12 }} animate={{ opacity: 1, y: 0 }} className="profile-hero liquid-card">
        <div className="profile-avatar"><IconUser size={28} /></div>
        <div><p>Your Nudge space</p><h1>{name}</h1><span>Level {level} · {xp} XP</span></div>
        <button className="soft-button" onClick={onToggleTheme}>{dark ? <IconSun size={18} /> : <IconMoon size={18} />}{dark ? 'Light' : 'Dark'}</button>
      </motion.section>

      {groups.map((group, gi) => (
        <section key={group.title} className="settings-group">
          <h2>{group.title}</h2>
          <div className="settings-list liquid-card">
            {group.items.map(([id, Icon, title, body], i) => (
              <motion.button key={id} whileTap={{ scale: .985 }} onClick={() => onNavigate(id)} className="settings-row">
                <span className={`settings-icon tone-${(gi * 4 + i) % 4}`}><Icon size={19} /></span>
                <span><strong>{title}</strong><small>{body}</small></span><b>›</b>
              </motion.button>
            ))}
          </div>
        </section>
      ))}

      <section className="settings-group">
        <h2>Privacy & app</h2>
        <div className="settings-list liquid-card">
          <div className="settings-row"><span className="settings-icon tone-2"><IconShield size={19} /></span><span><strong>Privacy</strong><small>Local-first storage is active</small></span><b className="status-dot">On</b></div>
          <button className="settings-row" onClick={onResetOnboarding}><span className="settings-icon tone-3"><IconUser size={19} /></span><span><strong>Replay welcome</strong><small>See onboarding again</small></span><b>›</b></button>
        </div>
      </section>
      <p className="profile-footnote">Nudge 0.1 · Your money stays yours.</p>
    </div>
  );
}
