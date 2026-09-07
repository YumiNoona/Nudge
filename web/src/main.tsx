import React, { useEffect, useState } from "react";
import { createRoot } from "react-dom/client";
import "@fontsource/dm-sans/latin-400.css";
import "@fontsource/dm-sans/latin-500.css";
import "@fontsource/dm-sans/latin-600.css";
import "@fontsource/dm-sans/latin-700.css";
import "@fontsource/jetbrains-mono/latin-400.css";
import "@fontsource/jetbrains-mono/latin-500.css";
import "@fontsource/jetbrains-mono/latin-600.css";
import "@fontsource/jetbrains-mono/latin-700.css";
import { ArrowLeft, BarChart3, Camera, Check, ChevronRight, DatabaseBackup, FileUp, Moon, ReceiptText, Repeat2, ScanLine, ShieldCheck, Sparkles, Split, Sun, Tag, WalletCards, Zap } from "lucide-react";
import "./styles.css";

const features = [
  [Zap, "Automatic capture", "Reads permitted bank and UPI alerts and prepares clean entries for review."],
  [Sparkles, "Smart review", "Confirm uncertain captures, correct details, and teach Nudge better local suggestions."],
  [FileUp, "Statement import", "Review transactions from supported PDF, CSV, text, and image statements before saving."],
  [ScanLine, "Receipt intelligence", "Scan multi-page receipts, reconcile totals and GST, then save one expense or itemized entries."],
  [Split, "Shared expenses", "Split costs equally, by exact amount, or by percentage and keep local balances settled."],
  [Repeat2, "Recurring entries", "Schedule weekly, monthly, or yearly transactions without relying on a cloud account."],
  [WalletCards, "Accounts & categories", "Organize cash, cards, UPI and wallets with custom categories, colors, icons, or emoji."],
  [BarChart3, "Analytics & widgets", "See monthly category mix and daily cash flow, with responsive home-screen snapshots."],
  [DatabaseBackup, "Your data, portable", "Export or restore a versioned backup, manage saved sources, or delete everything locally."],
] as const;

function ThemeButton() {
  const [light, setLight] = useState(false);
  useEffect(() => { document.documentElement.dataset.theme = light ? "light" : "dark"; }, [light]);
  return <button className="iconButton" onClick={() => setLight(!light)} aria-label="Toggle color theme">{light ? <Moon /> : <Sun />}</button>;
}
function Logo() { return <a className="logo" href="/"><img src="/nudge-icon.png" alt="" /><span>Nudge</span></a>; }
function Header() { return <header><Logo /><nav><a href="/#features">Features</a><a href="/#privacy">Privacy</a><a href="/privacy">Policy</a></nav><ThemeButton /></header>; }

function Phone() {
  const rows = [["District Dining","Food & Dining","−₹725"],["Swiggy","Food & Dining","−₹359"],["Salary","Income","₹13,000"]];
  return <div className="phoneWrap"><div className="glow"/><div className="phone">
    <div className="status"><span>9:41</span><span>● ●</span></div>
    <div className="appTitle"><div className="avatar">V</div><strong>Transactions</strong><ScanLine /></div>
    <div className="flowCard"><div className="eyebrow">NET CASH FLOW</div><div className="month">August 2026</div><div className="amount">−₹11,486</div><div className="flowSub">18 entries this month</div><div className="metrics"><span><i className="green"/>IN<br/><b>₹29,286</b></span><span><i className="coral"/>SPENT<br/><b>₹40,772</b></span><span><i className="amber"/>REFUNDS<br/><b>₹0</b></span></div></div>
    <div className="filters"><b>All</b><span>Expenses</span><span>Income</span></div><div className="dateLabel">TODAY</div>
    {rows.map((r,i)=><div className="transaction" key={r[0]}><div className={`txIcon tx${i}`}><ReceiptText/></div><div><strong>{r[0]}</strong><small>{r[1]} · UPI</small></div><b className={i===2?"plus":""}>{r[2]}</b></div>)}
    <div className="dock"><ReceiptText/><button>+</button><BarChart3/></div>
  </div></div>;
}

function Landing() {
  return <><Header/><main>
    <section className="hero"><div className="heroCopy"><div className="pill"><span/>PRIVATE BY DESIGN</div><h1>Your money,<br/><em>one calm timeline.</em></h1><p>Nudge turns transaction alerts, statements and receipts into an organized expense history—without turning your financial life into someone else’s dataset.</p><div className="actions"><a className="primary" href="#features"><Sparkles/>Explore Nudge<ChevronRight/></a><a className="secondary" href="#privacy"><ShieldCheck/>Our privacy promise</a></div><div className="trust"><span><Check/>Android-first</span><span><Check/>No account required</span><span><Check/>Local processing</span></div></div><Phone/></section>
    <section className="signal"><p>BUILT FOR THE MESSY REALITY OF MONEY</p><div><span>Bank alerts</span><i>→</i><span>UPI messages</span><i>→</i><span>Statements</span><i>→</i><span>Receipts</span></div></section>
    <section id="features" className="section"><div className="sectionHead"><div><div className="eyebrow lime">ONE APP. LESS ADMIN.</div><h2>Capture quickly.<br/>Understand clearly.</h2></div><p>Each feature has one job: reduce manual entry while keeping you in control of what becomes a transaction.</p></div><div className="featureGrid">{features.map(([Icon,title,body],i)=><article key={title} className={i===0?"featured":""}><div className="featureIcon"><Icon/></div><span>0{i+1}</span><h3>{title}</h3><p>{body}</p></article>)}</div></section>
    <section id="privacy" className="privacyBand"><div><div className="eyebrow">THE IMPORTANT PART</div><h2>Your financial data<br/>belongs to you.</h2></div><div className="privacyPoints"><Point icon={ShieldCheck} title="Processed on-device">Transaction parsing and learning happen locally.</Point><Point icon={Camera} title="Permission with purpose">Camera, files, SMS and notifications are used only for features you enable.</Point><Point icon={Tag} title="No advertising profile">Nudge does not sell personal or financial data.</Point><a href="/privacy">Read the full privacy policy <ChevronRight/></a></div></section>
    <section className="cta"><img src="/nudge-icon.png" alt="Nudge app icon"/><div><div className="eyebrow lime">GOOGLE PLAY · COMING SOON</div><h2>Make expense tracking<br/>feel less like work.</h2></div><a className="primary" href="#features">Explore features<ChevronRight/></a></section>
  </main><Footer/></>;
}
function Point({icon:Icon,title,children}:{icon:React.ElementType,title:string,children:React.ReactNode}) { return <p><Icon/><span><b>{title}</b>{children}</span></p>; }

function Privacy() {
  return <><Header/><main className="policy"><a className="back" href="/"><ArrowLeft/>Back to Nudge</a><div className="policyHero"><div className="pill"><ShieldCheck/>PRIVACY POLICY</div><h1>Plain language.<br/>No hidden trade.</h1><p>Effective 11 August 2026 · Nudge for Android</p></div>
    <div className="policyLayout"><aside><a href="#overview">Overview</a><a href="#permissions">Permissions</a><a href="#storage">Storage</a><a href="#sharing">Sharing</a><a href="#control">Your control</a><a href="#contact">Contact</a></aside><article>
      <Policy id="overview" n="01" title="Overview"><p>Nudge is a personal expense manager designed to work primarily on your Android device. It can create transactions from information you choose to provide, such as notification alerts, SMS or MMS messages, bank statements, files and receipt images.</p><p>Nudge does not require an account. Core transaction data is stored locally on your device.</p></Policy>
      <Policy id="permissions" n="02" title="Permissions and data used"><p>Nudge may request camera access for receipt capture, notification access for supported payment alerts, SMS/MMS access for transaction-history scanning, file access through Android’s system picker for imports, and notification permission for capture or reminder notices.</p><p>Permissions are requested for specific features. You can deny or revoke them in Android settings, though the related feature will stop working.</p></Policy>
      <Policy id="storage" n="03" title="Local storage and saved sources"><p>Transactions, categories, accounts, preferences and learning rules are stored on the device. If you enable transaction-message saving, linked source messages are retained locally so you can inspect them later. You may delete saved messages and app data within Nudge.</p><p>Imported documents and images are read to prepare a preview. Nudge does not intentionally upload their content to a Nudge-operated server.</p></Policy>
      <Policy id="sharing" n="04" title="Sharing and third parties"><p>Nudge does not sell your personal or financial information and does not use it to build an advertising profile. Android, Google Play and services you explicitly open—such as a payment app, browser or file provider—may process information under their own policies.</p><p>If update checking is enabled, Nudge may request public release metadata to determine whether a newer version exists. This does not include transaction history.</p></Policy>
      <Policy id="control" n="05" title="Your controls"><ul><li>Review and correct automatically detected transactions.</li><li>Disable automatic capture or individual permissions.</li><li>Delete transactions, saved messages or all local app data.</li><li>Export a backup and restore it later.</li><li>Uninstall Nudge to remove its app data, subject to Android behavior.</li></ul></Policy>
      <Policy id="security" n="06" title="Security and limitations"><p>Nudge uses Android platform protections and local storage controls. No software can promise absolute security. Protect your device with a secure screen lock and install updates from trusted sources.</p><p>Automatic classification can be wrong. Always review financial entries before relying on totals or reports.</p></Policy>
      <Policy id="children" n="07" title="Children"><p>Nudge is not directed to children under 13 and is not designed to knowingly collect children’s personal information.</p></Policy>
      <Policy id="changes" n="08" title="Policy changes"><p>This policy may be updated when features or legal requirements change. The effective date above will be revised, and material changes may also be highlighted in the app or release notes.</p></Policy>
      <Policy id="contact" n="09" title="Contact"><p>For privacy questions or deletion help, use the developer contact listed on Nudge’s Google Play page. Do not include bank statements, card numbers, SMS contents or other sensitive financial information.</p></Policy>
    </article></div></main><Footer/></>;
}
function Policy({id,n,title,children}:{id:string,n:string,title:string,children:React.ReactNode}) { return <section id={id} className="policySection"><span>{n}</span><div><h2>{title}</h2>{children}</div></section>; }
function Footer(){return <footer><Logo/><small>Made with <span>♥</span> by Veil · © 2026 Nudge</small><div><a href="/#features">Features</a><a href="/privacy">Privacy</a></div></footer>}

createRoot(document.getElementById("root")!).render(location.pathname.startsWith("/privacy") ? <Privacy/> : <Landing/>);
