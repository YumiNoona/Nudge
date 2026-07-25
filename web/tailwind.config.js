/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './app.html', './src/**/*.{js,ts,jsx,tsx}'],
  darkMode: 'class',
  theme: {
    extend: {
      colors: {
        purple:  { 1: 'var(--purple)', 2: 'var(--purple-deep)', bg: 'var(--purple-bg)', shadow: 'var(--purple-shadow)' },
        green:   { 1: 'var(--green)', bg: 'var(--green-bg)' },
        coral:   { 1: 'var(--coral)', bg: 'var(--coral-bg)' },
        amber:   { 1: 'var(--amber)', bg: 'var(--amber-bg)' },
        ink:     { 1: 'var(--ink)', soft: 'var(--ink-soft)', mute: 'var(--ink-mute)', inv: 'var(--ink-inverse)' },
        surface: { DEFAULT: 'var(--surface)', hover: 'var(--surface-hover)' },
        lavender:{ bg: 'var(--lavender-bg)' },
        chip:    {
          blue:  { fg: 'var(--cat-blue)',  bg: 'var(--cat-blue-bg)' },
          pink:  { fg: 'var(--cat-pink)',  bg: 'var(--cat-pink-bg)' },
          teal:  { fg: 'var(--cat-teal)',  bg: 'var(--cat-teal-bg)' },
          orange:{ fg: 'var(--cat-orange)',bg: 'var(--cat-orange-bg)' },
          violet:{ fg: 'var(--cat-violet)',bg: 'var(--cat-violet-bg)' },
          rose:  { fg: 'var(--cat-rose)',  bg: 'var(--cat-rose-bg)' },
          cyan:  { fg: 'var(--cat-cyan)',  bg: 'var(--cat-cyan-bg)' },
          lime:  { fg: 'var(--cat-lime)',  bg: 'var(--cat-lime-bg)' },
        },
      },
      fontFamily: {
        sans: ['Manrope', 'Inter', 'system-ui', 'sans-serif'],
        mono: ['JetBrains Mono', 'Fira Code', 'monospace'],
      },
      borderRadius: { card: '20px', chip: '12px', pill: '9999px' },
      boxShadow: {
        'purple': 'var(--shadow-purple)',
        'purple-lg': 'var(--shadow-purple-lg)',
        'card': 'var(--shadow-card)',
        'card-md': 'var(--shadow-card-md)',
      },
    },
  },
  plugins: [],
};
