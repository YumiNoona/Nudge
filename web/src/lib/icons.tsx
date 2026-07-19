// Tabler icons — hand-picked SVGs as React components
// https://tabler.io/icons — MIT licensed

export function IconWallet({ size = 24, stroke = 1.5, className = '' }: { size?: number; stroke?: number; className?: string }) {
  return (
    <svg className={className} width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={stroke} strokeLinecap="round" strokeLinejoin="round">
      <path d="M17 8V5a1 1 0 0 0-1-1H6a2 2 0 0 0-2 2v12a2 2 0 0 0 2 2h10a1 1 0 0 0 1-1v-3" />
      <path d="M20 12v4h-5a2 2 0 0 1-2-2v0a2 2 0 0 1 2-2h5z" />
      <path d="M17 8h3a1 1 0 0 1 1 1v3" />
    </svg>
  );
}

export function IconPlus({ size = 24, stroke = 1.5, className = '' }: { size?: number; stroke?: number; className?: string }) {
  return (
    <svg className={className} width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={stroke} strokeLinecap="round" strokeLinejoin="round">
      <path d="M12 5v14M5 12h14" />
    </svg>
  );
}

export function IconChartBar({ size = 24, stroke = 1.5, className = '' }: { size?: number; stroke?: number; className?: string }) {
  return (
    <svg className={className} width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={stroke} strokeLinecap="round" strokeLinejoin="round">
      <path d="M3 13h4v6H3zM10 9h4v10h-4zM17 5h4v14h-4z" />
    </svg>
  );
}

export function IconChartPie({ size = 24, stroke = 1.5, className = '' }: { size?: number; stroke?: number; className?: string }) {
  return (
    <svg className={className} width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={stroke} strokeLinecap="round" strokeLinejoin="round">
      <path d="M12 12L3 20" /><path d="M12 12L21 7" /><path d="M12 2a10 10 0 1 0 10 10H12V2z" />
    </svg>
  );
}

export function IconFlame({ size = 24, stroke = 1.5, className = '' }: { size?: number; stroke?: number; className?: string }) {
  return (
    <svg className={className} width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={stroke} strokeLinecap="round" strokeLinejoin="round">
      <path d="M12 2c-3.5 5-6 8.5-6 13a6 6 0 1 0 12 0c0-4.5-2.5-8-6-13z" />
      <path d="M12 12a2 2 0 1 0 0 4 2 2 0 0 0 0-4z" fill="currentColor" opacity="0.4" />
    </svg>
  );
}

export function IconTrophy({ size = 24, stroke = 1.5, className = '' }: { size?: number; stroke?: number; className?: string }) {
  return (
    <svg className={className} width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={stroke} strokeLinecap="round" strokeLinejoin="round">
      <path d="M8 21h8M12 17v4M7 4h10M17 4v8a5 5 0 0 1-10 0V4" />
      <path d="M7 4H5a2 2 0 0 0-2 2v2a4 4 0 0 0 4 4h0" />
      <path d="M17 4h2a2 2 0 0 1 2 2v2a4 4 0 0 1-4 4h0" />
    </svg>
  );
}

export function IconTarget({ size = 24, stroke = 1.5, className = '' }: { size?: number; stroke?: number; className?: string }) {
  return (
    <svg className={className} width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={stroke} strokeLinecap="round" strokeLinejoin="round">
      <circle cx="12" cy="12" r="10" /><circle cx="12" cy="12" r="6" /><circle cx="12" cy="12" r="2" />
    </svg>
  );
}

export function IconShoppingCart({ size = 24, stroke = 1.5, className = '' }: { size?: number; stroke?: number; className?: string }) {
  return (
    <svg className={className} width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={stroke} strokeLinecap="round" strokeLinejoin="round">
      <circle cx="9" cy="21" r="1" /><circle cx="20" cy="21" r="1" />
      <path d="M1 1h4l2.68 13.39a2 2 0 0 0 2 1.61h9.72a2 2 0 0 0 2-1.61L23 6H6" />
    </svg>
  );
}

export function IconBus({ size = 24, stroke = 1.5, className = '' }: { size?: number; stroke?: number; className?: string }) {
  return (
    <svg className={className} width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={stroke} strokeLinecap="round" strokeLinejoin="round">
      <circle cx="7" cy="18" r="2" /><circle cx="17" cy="18" r="2" />
      <path d="M3 5h18v12a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5zM3 9h18" />
    </svg>
  );
}

export function IconPizza({ size = 24, stroke = 1.5, className = '' }: { size?: number; stroke?: number; className?: string }) {
  return (
    <svg className={className} width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={stroke} strokeLinecap="round" strokeLinejoin="round">
      <path d="M12 2a10 10 0 1 1-4.8 18.8l-.8-.8a1 1 0 0 1 0-1.4l.4-.4a1 1 0 0 1 1.4 0l.4.4c.4.4 1 .4 1.4 0l.4-.4a1 1 0 0 1 1.4 0l.4.4c.4.4 1 .4 1.4 0l.4-.4a1 1 0 0 1 1.4 0l.4.4c.4.4 1 .4 1.4 0l.8-.8A10 10 0 0 0 12 2z" />
      <circle cx="8.5" cy="8.5" r="1" fill="currentColor" /><circle cx="15.5" cy="8.5" r="1" fill="currentColor" />
      <circle cx="12" cy="12.5" r="1" fill="currentColor" /><circle cx="9.5" cy="15.5" r="1" fill="currentColor" />
    </svg>
  );
}

export function IconHome({ size = 24, stroke = 1.5, className = '' }: { size?: number; stroke?: number; className?: string }) {
  return (
    <svg className={className} width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={stroke} strokeLinecap="round" strokeLinejoin="round">
      <path d="M5 12H3l9-9 9 9h-2M5 12v7a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2v-7" />
      <path d="M9 21v-6a2 2 0 0 1 2-2h2a2 2 0 0 1 2 2v6" />
    </svg>
  );
}

export function IconZap({ size = 24, stroke = 1.5, className = '' }: { size?: number; stroke?: number; className?: string }) {
  return (
    <svg className={className} width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={stroke} strokeLinecap="round" strokeLinejoin="round">
      <path d="M13 2L3 14h9l-1 8 10-12h-9l1-8z" />
    </svg>
  );
}

export function IconHeart({ size = 24, stroke = 1.5, className = '' }: { size?: number; stroke?: number; className?: string }) {
  return (
    <svg className={className} width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={stroke} strokeLinecap="round" strokeLinejoin="round">
      <path d="M19.5 12.572l-7.5 7.428-7.5-7.428A5 5 0 1 1 12 6.006a5 5 0 1 1 7.5 6.572" />
    </svg>
  );
}

export function IconDeviceMobile({ size = 24, stroke = 1.5, className = '' }: { size?: number; stroke?: number; className?: string }) {
  return (
    <svg className={className} width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={stroke} strokeLinecap="round" strokeLinejoin="round">
      <rect x="7" y="2" width="10" height="20" rx="2" /><path d="M11 18h2" />
    </svg>
  );
}

export function IconPlane({ size = 24, stroke = 1.5, className = '' }: { size?: number; stroke?: number; className?: string }) {
  return (
    <svg className={className} width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={stroke} strokeLinecap="round" strokeLinejoin="round">
      <path d="M17.8 19.817L11 16l-5.6 3.817 1.8-7.136L2 8.5l6-.5 2.8-5.5L14 8l6-.5-5.2 4.181 1.8 7.136z" />
    </svg>
  );
}

export function IconGift({ size = 24, stroke = 1.5, className = '' }: { size?: number; stroke?: number; className?: string }) {
  return (
    <svg className={className} width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={stroke} strokeLinecap="round" strokeLinejoin="round">
      <rect x="3" y="8" width="18" height="4" rx="1" /><path d="M12 8v13M19 12v7a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2v-7" />
      <path d="M7.5 8a2.5 2.5 0 0 1 2.5-2.5C11 5.5 12 8 12 8s1-2.5 2-2.5a2.5 2.5 0 0 1 0 5" />
    </svg>
  );
}

export function IconPigMoney({ size = 24, stroke = 1.5, className = '' }: { size?: number; stroke?: number; className?: string }) {
  return (
    <svg className={className} width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={stroke} strokeLinecap="round" strokeLinejoin="round">
      <path d="M5 10v0a7 7 0 0 1 14 0v0c0 2.5-1 5-2.5 6.5V19a1 1 0 0 1-1 1h-1a1 1 0 0 1-1-1v-1h-3v1a1 1 0 0 1-1 1h-1a1 1 0 0 1-1-1v-2.5C6 16 5 13.5 5 10z" />
      <circle cx="9" cy="10" r="1" /><circle cx="15" cy="10" r="1" fill="currentColor" />
      <path d="M9 14c.83.67 1.83 1 3 1s2.17-.33 3-1" />
    </svg>
  );
}

export function IconBook({ size = 24, stroke = 1.5, className = '' }: { size?: number; stroke?: number; className?: string }) {
  return (
    <svg className={className} width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={stroke} strokeLinecap="round" strokeLinejoin="round">
      <path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20" />
      <path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z" />
    </svg>
  );
}

export function IconSearch({ size = 24, stroke = 1.5, className = '' }: { size?: number; stroke?: number; className?: string }) {
  return (
    <svg className={className} width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={stroke} strokeLinecap="round" strokeLinejoin="round">
      <circle cx="10" cy="10" r="7" /><path d="M21 21l-6-6" />
    </svg>
  );
}

export function IconSettings({ size = 24, stroke = 1.5, className = '' }: { size?: number; stroke?: number; className?: string }) {
  return (
    <svg className={className} width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={stroke} strokeLinecap="round" strokeLinejoin="round">
      <path d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 0 0 2.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 0 0 1.066 2.573c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 0 0-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 0 0-2.573 1.066c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 0 0-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 0 0-1.066-2.573c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 0 0 1.066-2.573c-.94-1.543.826-3.31 2.37-2.37 1 .608 2.296.07 2.573-1.066z" />
      <circle cx="12" cy="12" r="3" />
    </svg>
  );
}

export function IconDownload({ size = 24, stroke = 1.5, className = '' }: { size?: number; stroke?: number; className?: string }) {
  return (
    <svg className={className} width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={stroke} strokeLinecap="round" strokeLinejoin="round">
      <path d="M4 17v2a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2v-2M7 11l5 5 5-5M12 4v12" />
    </svg>
  );
}

export function IconUpload({ size = 24, stroke = 1.5, className = '' }: { size?: number; stroke?: number; className?: string }) {
  return (
    <svg className={className} width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={stroke} strokeLinecap="round" strokeLinejoin="round">
      <path d="M4 17v2a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2v-2M7 9l5-5 5 5M12 4v12" />
    </svg>
  );
}

export function IconStar({ size = 24, stroke = 1.5, className = '' }: { size?: number; stroke?: number; className?: string }) {
  return (
    <svg className={className} width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={stroke} strokeLinecap="round" strokeLinejoin="round">
      <path d="M12 2l2.939 5.955L21.5 9l-4.75 4.63L17.871 21.5 12 18.412 6.129 21.5l1.121-7.87L2.5 9l6.561-1.045L12 2z" />
    </svg>
  );
}

export function IconCheck({ size = 24, stroke = 1.5, className = '' }: { size?: number; stroke?: number; className?: string }) {
  return (
    <svg className={className} width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={stroke} strokeLinecap="round" strokeLinejoin="round">
      <path d="M5 12l5 5L20 7" />
    </svg>
  );
}

export function IconX({ size = 24, stroke = 1.5, className = '' }: { size?: number; stroke?: number; className?: string }) {
  return (
    <svg className={className} width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={stroke} strokeLinecap="round" strokeLinejoin="round">
      <path d="M18 6L6 18M6 6l12 12" />
    </svg>
  );
}

export function IconArrowLeft({ size = 24, stroke = 1.5, className = '' }: { size?: number; stroke?: number; className?: string }) {
  return (
    <svg className={className} width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={stroke} strokeLinecap="round" strokeLinejoin="round">
      <path d="M5 12h14M5 12l6 6M5 12l6-6" />
    </svg>
  );
}

export function IconArrowRight({ size = 24, stroke = 1.5, className = '' }: { size?: number; stroke?: number; className?: string }) {
  return (
    <svg className={className} width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={stroke} strokeLinecap="round" strokeLinejoin="round">
      <path d="M5 12h14M13 18l6-6M13 6l6 6" />
    </svg>
  );
}

export function IconChevronDown({ size = 24, stroke = 1.5, className = '' }: { size?: number; stroke?: number; className?: string }) {
  return (
    <svg className={className} width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={stroke} strokeLinecap="round" strokeLinejoin="round">
      <path d="M6 9l6 6 6-6" />
    </svg>
  );
}

export function IconAlertTriangle({ size = 24, stroke = 1.5, className = '' }: { size?: number; stroke?: number; className?: string }) {
  return (
    <svg className={className} width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={stroke} strokeLinecap="round" strokeLinejoin="round">
      <path d="M12 9v4M12 17h.01M10.29 3.86l-8.6 14.86A1 1 0 0 0 2.56 20h18.88a1 1 0 0 0 .87-1.28l-8.6-14.86a1 1 0 0 0-1.72 0z" />
    </svg>
  );
}

export function IconDatabase({ size = 24, stroke = 1.5, className = '' }: { size?: number; stroke?: number; className?: string }) {
  return (
    <svg className={className} width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={stroke} strokeLinecap="round" strokeLinejoin="round">
      <ellipse cx="12" cy="5" rx="9" ry="3" /><path d="M3 5v14c0 1.66 4.03 3 9 3s9-1.34 9-3V5" />
      <path d="M3 12c0 1.66 4.03 3 9 3s9-1.34 9-3" />
    </svg>
  );
}

export function IconFileImport({ size = 24, stroke = 1.5, className = '' }: { size?: number; stroke?: number; className?: string }) {
  return (
    <svg className={className} width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={stroke} strokeLinecap="round" strokeLinejoin="round">
      <path d="M14 3v4a1 1 0 0 0 1 1h4" />
      <path d="M5 13V5a2 2 0 0 1 2-2h7l5 5v5M2 19h7M2 15l4 4-4 4" />
    </svg>
  );
}

export function IconSun({ size = 24, stroke = 1.5, className = '' }: { size?: number; stroke?: number; className?: string }) {
  return (
    <svg className={className} width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={stroke} strokeLinecap="round" strokeLinejoin="round">
      <circle cx="12" cy="12" r="4" /><path d="M12 2v2M12 20v2M4.93 4.93l1.41 1.41M17.66 17.66l1.41 1.41M2 12h2M20 12h2M6.34 17.66l-1.41 1.41M19.07 4.93l-1.41 1.41" />
    </svg>
  );
}

export function IconMoon({ size = 24, stroke = 1.5, className = '' }: { size?: number; stroke?: number; className?: string }) {
  return (
    <svg className={className} width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={stroke} strokeLinecap="round" strokeLinejoin="round">
      <path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z" />
    </svg>
  );
}

export function IconMail({ size = 24, stroke = 1.5, className = '' }: { size?: number; stroke?: number; className?: string }) {
  return (
    <svg className={className} width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={stroke} strokeLinecap="round" strokeLinejoin="round">
      <rect x="2" y="4" width="20" height="16" rx="2" /><path d="M22 4L12 13 2 4" />
    </svg>
  );
}

export function IconBell({ size = 24, stroke = 1.5, className = '' }: { size?: number; stroke?: number; className?: string }) {
  return (
    <svg className={className} width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={stroke} strokeLinecap="round" strokeLinejoin="round">
      <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9M13.73 21a2 2 0 0 1-3.46 0" />
    </svg>
  );
}

export function IconRefresh({ size = 24, stroke = 1.5, className = '' }: { size?: number; stroke?: number; className?: string }) {
  return (
    <svg className={className} width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={stroke} strokeLinecap="round" strokeLinejoin="round">
      <path d="M20 11A8.1 8.1 0 0 0 4.5 9M4 5v4h4M4 13a8.1 8.1 0 0 0 15.5 2M20 19v-4h-4" />
    </svg>
  );
}

export function IconTrash({ size = 24, stroke = 1.5, className = '' }: { size?: number; stroke?: number; className?: string }) {
  return (
    <svg className={className} width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={stroke} strokeLinecap="round" strokeLinejoin="round">
      <path d="M4 7h16M10 11v6M14 11v6M5 7l1 12a2 2 0 0 0 2 2h8a2 2 0 0 0 2-2l1-12M9 7V4a1 1 0 0 1 1-1h4a1 1 0 0 1 1 1v3" />
    </svg>
  );
}

export function IconLayoutDashboard({ size = 24, stroke = 1.5, className = '' }: { size?: number; stroke?: number; className?: string }) {
  return (
    <svg className={className} width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={stroke} strokeLinecap="round" strokeLinejoin="round">
      <rect x="3" y="3" width="7" height="9" rx="1" /><rect x="14" y="3" width="7" height="5" rx="1" />
      <rect x="14" y="12" width="7" height="9" rx="1" /><rect x="3" y="16" width="7" height="5" rx="1" />
    </svg>
  );
}

export function IconChecklist({ size = 24, stroke = 1.5, className = '' }: { size?: number; stroke?: number; className?: string }) {
  return (
    <svg className={className} width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={stroke} strokeLinecap="round" strokeLinejoin="round">
      <path d="M11 17h6M8 12l-2 2-1-1M11 12h6M8 7L6 9 5 8M11 7h6" />
    </svg>
  );
}

export function IconCreditCard({ size = 24, stroke = 1.5, className = '' }: { size?: number; stroke?: number; className?: string }) {
  return (
    <svg className={className} width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={stroke} strokeLinecap="round" strokeLinejoin="round">
      <rect x="2" y="4" width="20" height="16" rx="2" /><path d="M2 10h20M7 15h3M12 15h2" />
    </svg>
  );
}

export function IconCash({ size = 24, stroke = 1.5, className = '' }: { size?: number; stroke?: number; className?: string }) {
  return (
    <svg className={className} width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={stroke} strokeLinecap="round" strokeLinejoin="round">
      <rect x="1" y="5" width="22" height="14" rx="2" />
      <circle cx="12" cy="12" r="3" /><path d="M18 12h.01M6 12h.01" />
    </svg>
  );
}

export function IconUser({ size = 24, stroke = 1.5, className = '' }: { size?: number; stroke?: number; className?: string }) {
  return (
    <svg className={className} width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={stroke} strokeLinecap="round" strokeLinejoin="round">
      <circle cx="12" cy="8" r="4" /><path d="M6 21v-2a4 4 0 0 1 4-4h4a4 4 0 0 1 4 4v2" />
    </svg>
  );
}

export function IconChartLine({ size = 24, stroke = 1.5, className = '' }: { size?: number; stroke?: number; className?: string }) {
  return (
    <svg className={className} width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={stroke} strokeLinecap="round" strokeLinejoin="round">
      <path d="M4 20h16M4 20V4M8 16l4-6 3 3 5-7" />
    </svg>
  );
}

export function IconTag({ size = 24, stroke = 1.5, className = '' }: { size?: number; stroke?: number; className?: string }) {
  return (
    <svg className={className} width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={stroke} strokeLinecap="round" strokeLinejoin="round">
      <circle cx="8" cy="8" r="2" /><path d="M2.5 12.5l7-7a2 2 0 0 1 1.4-.6H18a2 2 0 0 1 2 2v7.1a2 2 0 0 1-.6 1.4l-7 7a2 2 0 0 1-2.8 0l-7.1-7.1a2 2 0 0 1 0-2.8z" />
    </svg>
  );
}

export function IconShield({ size = 24, stroke = 1.5, className = '' }: { size?: number; stroke?: number; className?: string }) {
  return (
    <svg className={className} width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={stroke} strokeLinecap="round" strokeLinejoin="round">
      <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z" />
    </svg>
  );
}

export function IconCloud({ size = 24, stroke = 1.5, className = '' }: { size?: number; stroke?: number; className?: string }) {
  return (
    <svg className={className} width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={stroke} strokeLinecap="round" strokeLinejoin="round">
      <path d="M17.5 19H9a7 7 0 1 1 6.71-9h1.79a4.5 4.5 0 1 1 0 9z" />
    </svg>
  );
}

export function IconMenu({ size = 24, stroke = 1.5, className = '' }: { size?: number; stroke?: number; className?: string }) {
  return (
    <svg className={className} width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={stroke} strokeLinecap="round" strokeLinejoin="round">
      <path d="M4 6h16M4 12h16M4 18h16" />
    </svg>
  );
}

export function IconFilter({ size = 24, stroke = 1.5, className = '' }: { size?: number; stroke?: number; className?: string }) {
  return (
    <svg className={className} width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={stroke} strokeLinecap="round" strokeLinejoin="round">
      <path d="M4 4h16v2.172a2 2 0 0 1-.586 1.414L15 12v7l-6 3v-9.5L4.586 7.586A2 2 0 0 1 4 6.172V4z" />
    </svg>
  );
}

export function IconCalendar({ size = 24, stroke = 1.5, className = '' }: { size?: number; stroke?: number; className?: string }) {
  return (
    <svg className={className} width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={stroke} strokeLinecap="round" strokeLinejoin="round">
      <rect x="3" y="4" width="18" height="18" rx="2" /><path d="M16 2v4M8 2v4M3 10h18" />
    </svg>
  );
}
