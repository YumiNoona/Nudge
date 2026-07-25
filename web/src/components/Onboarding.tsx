import { useState } from 'react';
import { AnimatePresence, motion } from 'framer-motion';
import {
  IconArrowRight, IconBell, IconChartBar, IconCheck, IconMoon,
  IconShield, IconSun, IconWallet,
} from '../lib/icons';

type ThemeChoice = 'system' | 'light' | 'dark';

interface Props {
  onComplete: (profile: { name: string; currency: string; theme: ThemeChoice }) => void;
}

const steps = [
  { eyebrow: 'Welcome to Nudge', title: 'Money clarity, without the cloud.', body: 'A calm, private home for every expense—captured automatically on Android and always owned by you.' },
  { eyebrow: 'Automatic capture', title: 'Your spending finds its way home.', body: 'Bank SMS and UPI notifications are understood on your phone. Only the transaction is saved; your messages stay private.' },
  { eyebrow: 'A gentler routine', title: 'Review in seconds. Learn for life.', body: 'Nudge learns merchant names and categories as you swipe, then turns the result into useful patterns—not guilt.' },
  { eyebrow: 'Make it yours', title: 'Your space, your rhythm.', body: 'Choose a name, currency, and appearance. You can change everything later from your profile.' },
];

export default function Onboarding({ onComplete }: Props) {
  const [step, setStep] = useState(0);
  const [name, setName] = useState('');
  const [currency, setCurrency] = useState('INR');
  const [theme, setTheme] = useState<ThemeChoice>('system');

  const next = () => {
    if (step < steps.length - 1) setStep(step + 1);
    else onComplete({ name: name.trim() || 'Friend', currency, theme });
  };

  return (
    <main className="onboarding-shell">
      <div className="onboarding-orb onboarding-orb-one" />
      <div className="onboarding-orb onboarding-orb-two" />
      <header className="onboarding-header">
        <div className="brand-lockup"><span className="brand-mark"><IconWallet size={19} /></span><span>Nudge</span></div>
        <button className="text-button" onClick={() => onComplete({ name: 'Friend', currency: 'INR', theme: 'system' })}>Skip for now</button>
      </header>

      <section className="onboarding-stage">
        <AnimatePresence mode="wait">
          <motion.div
            key={step}
            initial={{ opacity: 0, y: 24, filter: 'blur(10px)' }}
            animate={{ opacity: 1, y: 0, filter: 'blur(0px)' }}
            exit={{ opacity: 0, y: -18, filter: 'blur(8px)' }}
            transition={{ duration: .5, ease: [0.22, 1, 0.36, 1] }}
            className="onboarding-copy"
          >
            <Visual step={step} />
            <p className="eyebrow">{steps[step].eyebrow}</p>
            <h1>{steps[step].title}</h1>
            <p className="onboarding-body">{steps[step].body}</p>
            {step === 3 && (
              <div className="setup-fields">
                <label>Your name<input value={name} onChange={e => setName(e.target.value)} placeholder="What should we call you?" autoFocus /></label>
                <label>Currency<select value={currency} onChange={e => setCurrency(e.target.value)}><option value="INR">₹ Indian Rupee</option><option value="USD">$ US Dollar</option><option value="GBP">£ British Pound</option><option value="EUR">€ Euro</option></select></label>
                <div>
                  <span className="field-label">Appearance</span>
                  <div className="theme-picker">
                    {([
                      ['system', <IconWallet size={17} />, 'System'],
                      ['light', <IconSun size={17} />, 'Light'],
                      ['dark', <IconMoon size={17} />, 'Dark'],
                    ] as const).map(([value, icon, label]) => (
                      <button key={value} className={theme === value ? 'selected' : ''} onClick={() => setTheme(value)}>{icon}<span>{label}</span>{theme === value && <IconCheck size={14} />}</button>
                    ))}
                  </div>
                </div>
              </div>
            )}
          </motion.div>
        </AnimatePresence>
      </section>

      <footer className="onboarding-footer">
        <div className="step-dots">{steps.map((_, i) => <span key={i} className={i === step ? 'active' : i < step ? 'done' : ''} />)}</div>
        <button className="primary-pill" onClick={next}>{step === steps.length - 1 ? 'Enter Nudge' : 'Continue'}<IconArrowRight size={18} /></button>
      </footer>
    </main>
  );
}

function Visual({ step }: { step: number }) {
  if (step === 0) return (
    <div className="onboarding-visual welcome-visual">
      <motion.div animate={{ y: [0, -10, 0], rotate: [-2, 2, -2] }} transition={{ duration: 5, repeat: Infinity }} className="glass-card mini-card card-back"><IconChartBar size={24} /><span>All clear</span></motion.div>
      <motion.div animate={{ y: [0, 8, 0] }} transition={{ duration: 4, repeat: Infinity }} className="glass-card balance-card"><span>This month</span><strong>₹24,860</strong><small>↓ 12% from June</small></motion.div>
    </div>
  );
  if (step === 1) return (
    <div className="onboarding-visual capture-visual">
      <motion.div initial={{ y: -20 }} animate={{ y: 0 }} className="notification-card glass-card"><span className="notification-icon"><IconBell size={19} /></span><div><small>HDFC Bank · now</small><strong>₹640 paid to Swiggy</strong></div></motion.div>
      <motion.div initial={{ scale: .7 }} animate={{ scale: 1 }} transition={{ delay: .25, type: 'spring' }} className="capture-check"><IconCheck size={24} /></motion.div>
      <span className="privacy-chip"><IconShield size={15} /> Processed on this device</span>
    </div>
  );
  if (step === 2) return (
    <div className="onboarding-visual review-visual">
      <motion.div animate={{ rotate: [-1, 1, -1] }} transition={{ duration: 4, repeat: Infinity }} className="review-card glass-card"><span>🍜</span><small>Today, 1:24 PM</small><strong>Swiggy</strong><b>−₹640</b><div><em>Dining</em><em>Personal</em></div></motion.div>
    </div>
  );
  return <div className="onboarding-visual profile-visual"><div className="avatar-preview">{(String('N'))}</div><div className="glass-card profile-preview"><strong>Your private space</strong><span>Local data · Easy backup · Your theme</span></div></div>;
}
