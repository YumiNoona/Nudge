/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,ts,jsx,tsx}'],
  darkMode: 'class',
  theme: {
    extend: {
      colors: {
        // Semantic design tokens matching KMP shared module
        'surface-base': 'var(--color-surface-base)',
        'surface-raised': 'var(--color-surface-raised)',
        'surface-overlay': 'var(--color-surface-overlay)',
        'content-primary': 'var(--color-content-primary)',
        'content-secondary': 'var(--color-content-secondary)',
        'content-tertiary': 'var(--color-content-tertiary)',
        'accent-primary': 'var(--color-accent-primary)',
        'accent-secondary': 'var(--color-accent-secondary)',
        'positive': 'var(--color-positive)',
        'negative': 'var(--color-negative)',
        'warning': 'var(--color-warning)',
      },
      fontFamily: {
        display: ['"Cabinet Grotesk"', '"General Sans"', 'sans-serif'],
        body: ['Inter', '"Public Sans"', 'sans-serif'],
        mono: ['"JetBrains Mono"', '"Fira Code"', 'monospace'],
      },
      fontSize: {
        'display': ['36px', { lineHeight: '44px' }],
        'title': ['24px', { lineHeight: '32px' }],
        'heading': ['18px', { lineHeight: '24px' }],
        'body': ['16px', { lineHeight: '22px' }],
        'caption': ['13px', { lineHeight: '18px' }],
        'micro': ['11px', { lineHeight: '14px' }],
      },
      spacing: {
        'xs': '4px',
        'sm': '8px',
        'md': '12px',
        'base': '16px',
        'lg': '24px',
        'xl': '32px',
        '2xl': '48px',
        '3xl': '64px',
      },
      borderRadius: {
        'sm': '8px',
        'md': '14px',
        'lg': '20px',
        'xl': '28px',
        'pill': '9999px',
      },
    },
  },
  plugins: [],
};
