import { spawn, spawnSync } from "node:child_process";
import { mkdirSync, mkdtempSync, readFileSync, writeFileSync, existsSync } from "node:fs";
import { tmpdir } from "node:os";
import { resolve, join } from "node:path";
import { pathToFileURL } from "node:url";

const root = resolve(import.meta.dirname, "..");
const outDir = join(root, "marketing", "trailer");
const frameDir = join(outDir, "frames");
const htmlPath = join(outDir, "nudge-trailer-frame.html");
const outputMp4 = join(outDir, "nudge-app-trailer-vertical.mp4");
const fps = 60;
const duration = 20;
const frames = fps * duration;

const edge = "C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe";
if (!existsSync(edge)) {
  throw new Error(`Microsoft Edge was not found at ${edge}`);
}

mkdirSync(outDir, { recursive: true });
mkdirSync(frameDir, { recursive: true });

const assets = {
  icon: pathToFileURL(join(root, "android", "src", "main", "res", "drawable-nodpi", "nudge_launcher_icon.png")).href,
  ready: pathToFileURL(join(root, "android", "src", "main", "res", "drawable-nodpi", "onboarding_ready_v2.png")).href,
  auto: pathToFileURL(join(root, "android", "src", "main", "res", "drawable-nodpi", "onboarding_auto_capture_v2.png")).href,
  story: pathToFileURL(join(root, "android", "src", "main", "res", "drawable-nodpi", "onboarding_finance_story_v2.png")).href,
};

const fonts = {
  regular: pathToFileURL(join(root, "android", "src", "main", "res", "font", "jetbrains_mono_regular.ttf")).href,
  medium: pathToFileURL(join(root, "android", "src", "main", "res", "font", "jetbrains_mono_medium.ttf")).href,
  bold: pathToFileURL(join(root, "android", "src", "main", "res", "font", "jetbrains_mono_bold.ttf")).href,
};

writeFileSync(htmlPath, `<!doctype html>
<html>
<head>
<meta charset="utf-8" />
<style>
  @font-face { font-family: 'JetBrains Mono'; src: url('${fonts.regular}') format('truetype'); font-weight: 400; }
  @font-face { font-family: 'JetBrains Mono'; src: url('${fonts.medium}') format('truetype'); font-weight: 600; }
  @font-face { font-family: 'JetBrains Mono'; src: url('${fonts.bold}') format('truetype'); font-weight: 700 900; }
  html, body { margin: 0; width: 1080px; height: 1920px; overflow: hidden; background: #06110d; }
  canvas { display: block; width: 1080px; height: 1920px; }
</style>
</head>
<body>
<canvas id="c" width="1080" height="1920"></canvas>
<script>
const W = 1080, H = 1920;
let t = Number(new URLSearchParams(location.search).get("t") || 0);
const assets = ${JSON.stringify(assets)};
const images = {};
const ease = x => x < 0 ? 0 : x > 1 ? 1 : 1 - Math.pow(1 - x, 3);
const pulse = (x) => 0.5 + 0.5 * Math.sin(x * Math.PI * 2);
function seg(a, b) { return ease((t - a) / (b - a)); }
function sceneAlpha(start, inEnd, outStart, end) {
  return seg(start, inEnd) * (1 - seg(outStart, end));
}
function money(n) { return "₹" + n.toLocaleString("en-IN"); }
// Fixed anchors keep commas and changing digit counts from shifting the layout.
function animatedMoney(ctx, amount, x, y, size, color, start, length = .85, align = "left", prefix = "") {
  const progress = Math.max(0, Math.min(1, (t - start) / length));
  const value = Math.round(amount * ease(progress));
  const landing = Math.max(0, 1 - Math.abs(t - start - length) / .24);
  ctx.save();
  ctx.shadowColor = color;
  ctx.shadowBlur = landing * 14;
  text(ctx, prefix + money(value), x, y + (1 - ease(progress)) * 8, size, color, 700, align);
  ctx.restore();
}
function load(src) {
  return new Promise((resolve) => {
    const img = new Image();
    img.onload = () => resolve(img);
    img.src = src;
  });
}
function roundRect(ctx, x, y, w, h, r) {
  ctx.beginPath();
  ctx.moveTo(x + r, y);
  ctx.arcTo(x + w, y, x + w, y + h, r);
  ctx.arcTo(x + w, y + h, x, y + h, r);
  ctx.arcTo(x, y + h, x, y, r);
  ctx.arcTo(x, y, x + w, y, r);
  ctx.closePath();
}
function fillRound(ctx, x, y, w, h, r, color) {
  ctx.fillStyle = color;
  roundRect(ctx, x, y, w, h, r);
  ctx.fill();
}
function strokeRound(ctx, x, y, w, h, r, color, lw = 2) {
  ctx.strokeStyle = color;
  ctx.lineWidth = lw;
  roundRect(ctx, x, y, w, h, r);
  ctx.stroke();
}
function text(ctx, value, x, y, size, color = "#f7fff7", weight = 700, align = "left", alpha = 1) {
  ctx.save();
  ctx.globalAlpha *= alpha;
  ctx.fillStyle = color;
  ctx.font = "700 " + size + "px 'JetBrains Mono'";
  ctx.textAlign = align;
  ctx.textBaseline = "top";
  ctx.fillText(value, x, y);
  ctx.restore();
}
function centerText(ctx, value, y, size, color = "#f7fff7", weight = 800, alpha = 1) {
  text(ctx, value, W / 2, y, size, color, weight, "center", alpha);
}
function phone(ctx, x, y, w, h, alpha = 1) {
  ctx.save();
  ctx.globalAlpha = alpha;
  fillRound(ctx, x, y, w, h, 78, "#07100c");
  strokeRound(ctx, x, y, w, h, 78, "#244236", 5);
  fillRound(ctx, x + 36, y + 38, w - 72, h - 76, 54, "#0b1511");
  fillRound(ctx, x + w / 2 - 64, y + 26, 128, 20, 10, "#111f18");
  ctx.restore();
}
function transaction(ctx, x, y, width, icon, title, meta, amt, positive = false, alpha = 1, start = 3.7) {
  ctx.save();
  ctx.globalAlpha = alpha;
  fillRound(ctx, x, y, width, 86, 22, "#101d17");
  fillRound(ctx, x + 18, y + 18, 56, 56, 16, positive ? "#123c2d" : "#391b16");
  text(ctx, icon, x + 34, y + 31, 23, positive ? "#4ee39b" : "#ff7769", 800);
  text(ctx, title, x + 94, y + 17, 24, "#f7fff7", 800);
  text(ctx, meta, x + 94, y + 51, 16, "#8aa295", 500);
  animatedMoney(ctx, amt, x + width - 28, y + 30, 22, positive ? "#4ee39b" : "#ff7769", start, .7, "right", positive ? "+" : "−");
  ctx.restore();
}
function drawApp(ctx, alpha = 1) {
  ctx.save();
  ctx.globalAlpha = alpha;
  const px = 250, py = 260, pw = 580, ph = 1260;
  phone(ctx, px, py, pw, ph, 1);
  ctx.save();
  roundRect(ctx, px + 38, py + 46, pw - 76, ph - 92, 48);
  ctx.clip();
  text(ctx, "Transactions", px + pw / 2, py + 106, 36, "#f7fff7", 900, "center");
  fillRound(ctx, px + 62, py + 205, pw - 124, 224, 38, "#123d2f");
  text(ctx, "NET CASH FLOW", px + 102, py + 250, 18, "#9bb6aa", 700);
  text(ctx, "October 2026", px + 102, py + 286, 19, "#d2e5dc", 600);
  animatedMoney(ctx, 15240, px + 102, py + 338, 44, "#d7ff3f", 3.65, 1.15);
  text(ctx, "12 entries this month", px + 102, py + 393, 16, "#9bb6aa", 500);
  fillRound(ctx, px + 82, py + 470, 96, 42, 18, "#d7ff3f");
  text(ctx, "All", px + 130, py + 481, 17, "#07100c", 900, "center");
  fillRound(ctx, px + 194, py + 470, 132, 42, 18, "#101d17");
  text(ctx, "Expenses", px + 260, py + 481, 17, "#f7fff7", 700, "center");
  transaction(ctx, px + 62, py + 560, pw - 124, "▣", "District Dining", "Food · UPI", 725, false, 1, 3.75);
  transaction(ctx, px + 62, py + 662, pw - 124, "▣", "Swiggy", "Food · UPI", 359, false, 1, 3.9);
  transaction(ctx, px + 62, py + 764, pw - 124, "₹", "Salary", "Bank", 13000, true, 1, 4.05);
  fillRound(ctx, px + 62, py + 925, pw - 124, 88, 36, "#131c17");
  fillRound(ctx, px + pw / 2 - 38, py + 940, 76, 58, 22, "#d7ff3f");
  text(ctx, "+", px + pw / 2, py + 950, 40, "#07100c", 900, "center");
  ctx.restore();
  ctx.restore();
}
function drawScanner(ctx, p) {
  const x = 225, y = 470, w = 630, h = 690;
  fillRound(ctx, x, y, w, h, 48, "#101b16");
  strokeRound(ctx, x, y, w, h, 48, "#2b4c3f", 3);
  fillRound(ctx, x + 58, y + 75, w - 116, h - 150, 30, "#f5f2de");
  text(ctx, "RECEIPT", x + 112, y + 118, 30, "#102014", 900);
  text(ctx, "District Dining", x + 112, y + 168, 24, "#102014", 800);
  for (let i = 0; i < 7; i++) {
    const yy = y + 230 + i * 44;
    text(ctx, ["Paneer Roll", "Iced Tea", "Delivery", "GST", "Discount", "Round off", "Total"][i], x + 112, yy, 20, "#21372c", 600);
    text(ctx, ["360.00", "120.00", "25.00", "18.00", "-20.00", "0.00", "503.00"][i], x + w - 112, yy, 20, "#21372c", 700, "right");
  }
  const scanY = y + 80 + pulse(p * 2) * 500;
  ctx.fillStyle = "rgba(215,255,63,.18)";
  ctx.fillRect(x + 60, scanY, w - 120, 12);
  ctx.strokeStyle = "rgba(215,255,63,.75)";
  ctx.lineWidth = 3;
  ctx.beginPath();
  ctx.moveTo(x + 72, scanY + 6);
  ctx.lineTo(x + w - 72, scanY + 6);
  ctx.stroke();
  fillRound(ctx, 248, 1210, 584, 110, 30, "#132c22");
  text(ctx, "OCR found", 292, 1232, 22, "#9bb6aa", 700);
  text(ctx, "Total", 292, 1270, 30, "#d7ff3f", 900);
  animatedMoney(ctx, 503, 405, 1270, 30, "#d7ff3f", 7.05, .9);
  text(ctx, "· 2 items", 540, 1270, 26, "#b8d2c7");
}
function drawShareSheet(ctx, p) {
  fillRound(ctx, 105, 300, 870, 420, 58, "#101d17");
  text(ctx, "Share to Nudge", 540, 355, 52, "#f7fff7", 900, "center");
  text(ctx, "From gallery, screenshots, bills or text.", 540, 432, 28, "#b8d2c7", 700, "center");
  const apps = [["✉", "Mail"], ["☁", "Drive"], ["N", "Nudge"], ["↗", "More"]];
  apps.forEach((a, i) => {
    const x = 158 + i * 193;
    fillRound(ctx, x, 525, 130, 130, 34, i === 2 ? "#d7ff3f" : "#17251e");
    text(ctx, a[0], x + 65, 555, 45, i === 2 ? "#07100c" : "#d8eee3", 900, "center");
    text(ctx, a[1], x + 65, 672, 22, "#b8d2c7", 700, "center");
  });
  const scan = ease(p);
  fillRound(ctx, 155, 835, 770, 470, 48, "#102018");
  strokeRound(ctx, 155, 835, 770, 470, 48, "#29493c", 3);
  fillRound(ctx, 255, 910, 570, 300, 30, "#f5f2de");
  text(ctx, "Cafe Bloom", 315, 955, 30, "#102014", 900);
  text(ctx, "Coffee", 315, 1025, 23, "#21372c", 650);
  text(ctx, "₹180", 765, 1025, 23, "#21372c", 750, "right");
  text(ctx, "Sandwich", 315, 1090, 23, "#21372c", 650);
  text(ctx, "₹220", 765, 1090, 23, "#21372c", 750, "right");
  text(ctx, "Total", 315, 1155, 25, "#102014", 900);
  text(ctx, "₹400", 765, 1155, 25, "#102014", 900, "right");
  const lineY = 925 + scan * 250;
  ctx.fillStyle = "rgba(215,255,63,.18)";
  ctx.fillRect(275, lineY, 530, 16);
  ctx.strokeStyle = "rgba(215,255,63,.8)";
  ctx.lineWidth = 4;
  ctx.beginPath();
  ctx.moveTo(285, lineY + 8);
  ctx.lineTo(795, lineY + 8);
  ctx.stroke();
  fillRound(ctx, 200, 1340, 680, 132, 36, "#132c22");
  text(ctx, "Preview before saving", 540, 1370, 28, "#b8d2c7", 800, "center");
  text(ctx, "Cafe Bloom · Food", 245, 1412, 28, "#d7ff3f", 900);
  animatedMoney(ctx, 400, 835, 1412, 32, "#d7ff3f", 10.25, .9, "right");
}
function drawInsightImport(ctx, p) {
  fillRound(ctx, 120, 385, 840, 720, 54, "#101d17");
  text(ctx, "Bring your old data", 170, 445, 46, "#f7fff7", 900);
  text(ctx, "Import CSV exports from popular expense apps.", 170, 515, 27, "#b8d2c7", 600);
  const names = ["CSV", "Wallet", "Spendee", "MoneyMgr"];
  names.forEach((n, i) => {
    const y = 610 + i * 92;
    fillRound(ctx, 170, y, 740, 70, 22, i < 3 ? "#17251e" : "#132c22");
    text(ctx, n, 210, y + 21, 22, "#f7fff7", 800);
    const progress = Math.min(1, Math.max(0, p * 1.4 - i * .25));
    fillRound(ctx, 505, y + 27, 330, 14, 7, "#20362d");
    fillRound(ctx, 505, y + 27, 330 * progress, 14, 7, "#d7ff3f");
  });
  fillRound(ctx, 190, 1180, 700, 260, 44, "#123d2f");
  text(ctx, "Smart monthly summary", 540, 1235, 34, "#f7fff7", 900, "center");
  text(ctx, "Food", 270, 1320, 23, "#b8d2c7", 700);
  animatedMoney(ctx, 4720, 810, 1320, 28, "#ff7769", 13.1, .95, "right");
  text(ctx, "Transport", 270, 1372, 23, "#b8d2c7", 700);
  animatedMoney(ctx, 1140, 810, 1372, 28, "#ffcf67", 13.3, .95, "right");
}
function drawFrame() {
  const ctx = document.getElementById("c").getContext("2d");
  const bg = ctx.createLinearGradient(0, 0, W, H);
  bg.addColorStop(0, "#06110d");
  bg.addColorStop(.55, "#0a1712");
  bg.addColorStop(1, "#10251d");
  ctx.fillStyle = bg;
  ctx.fillRect(0, 0, W, H);
  ctx.save();
  ctx.globalAlpha = .18;
  for (let i = 0; i < 28; i++) {
    const x = (i * 157 + t * 38) % 1200 - 80;
    const y = (i * 241 + t * 72) % 2100 - 90;
    fillRound(ctx, x, y, 46, 46, 16, i % 3 === 0 ? "#d7ff3f" : "#42e9a0");
  }
  ctx.restore();

  const s0 = seg(0, 1.4), s1 = seg(3.2, 5.3), s2 = seg(6.25, 8.65), s4 = seg(17.3, 20);
  if (t < 3.05) {
    ctx.save();
    ctx.globalAlpha = sceneAlpha(0, .55, 2.55, 3.05);
    const logoSize = 215 + 18 * pulse(t * .55);
    ctx.drawImage(images.icon, W / 2 - logoSize / 2, 360 - logoSize / 2, logoSize, logoSize);
    centerText(ctx, "Nudge", 540, 86, "#f7fff7", 900, s0);
    centerText(ctx, "Expense Manager", 632, 36, "#b8d2c7", 800, s0);
    fillRound(ctx, 210, 785, 660, 86, 34, "rgba(19,44,34,.9)");
    centerText(ctx, "Private expense tracking", 805, 32, "#d7ff3f", 900, seg(.45, 1));
    centerText(ctx, "without accounts, ads, or cloud lock-in.", 920, 32, "#f7fff7", 750, seg(.85, 1.45));
    centerText(ctx, "Your money, one calm timeline.", 1125, 42, "#d7ff3f", 900, seg(1.2, 1.8));
    centerText(ctx, "Built for receipts, UPI alerts, imports, and quick review.", 1200, 25, "#b8d2c7", 650, seg(1.45, 2));
    ctx.restore();
  }
  if (t >= 3.2 && t < 5.95) {
    ctx.save();
    ctx.globalAlpha = sceneAlpha(3.2, 3.65, 5.5, 5.95);
    ctx.translate(0, -40 + 40 * s1);
    drawApp(ctx, 1);
    centerText(ctx, "Auto-capture spends", 1590, 48, "#f7fff7", 900, 1);
    centerText(ctx, "Bank alerts and UPI activity stay on-device.", 1660, 27, "#b8d2c7", 600, 1);
    ctx.restore();
  }
  if (t >= 6.25 && t < 9.05) {
    ctx.save();
    ctx.globalAlpha = sceneAlpha(6.25, 6.7, 8.6, 9.05);
    drawScanner(ctx, s2);
    centerText(ctx, "Receipt intelligence", 1470, 48, "#f7fff7", 900, 1);
    centerText(ctx, "Scan bills. Review totals. Save with confidence.", 1540, 27, "#b8d2c7", 600, 1);
    ctx.restore();
  }
  if (t >= 9.45 && t < 12.15) {
    ctx.save();
    ctx.globalAlpha = sceneAlpha(9.45, 9.9, 11.7, 12.15);
    drawShareSheet(ctx, seg(9.95, 11.35));
    centerText(ctx, "Share sheet ready", 1510, 48, "#f7fff7", 900, 1);
    centerText(ctx, "Send bills or screenshots directly into Nudge.", 1580, 27, "#b8d2c7", 600, 1);
    ctx.restore();
  }
  if (t >= 12.55 && t < 15.2) {
    ctx.save();
    ctx.globalAlpha = sceneAlpha(12.55, 13, 14.75, 15.2);
    drawInsightImport(ctx, seg(13, 14.45));
    centerText(ctx, "Imports + insights", 1535, 48, "#f7fff7", 900, 1);
    centerText(ctx, "Move in, clean up, and understand spending faster.", 1605, 27, "#b8d2c7", 600, 1);
    ctx.restore();
  }
  if (t >= 15.55 && t < 17.45) {
    ctx.save();
    ctx.globalAlpha = sceneAlpha(15.55, 15.9, 17.05, 17.45);
    fillRound(ctx, 120, 390, 840, 760, 56, "#101d17");
    ctx.drawImage(images.ready, 350, 460, 380, 506);
    centerText(ctx, "Private by design", 1195, 56, "#f7fff7", 900, 1);
    centerText(ctx, "No account. Local OCR. You confirm every save.", 1275, 30, "#b8d2c7", 750, 1);
    const chips = ["Android-first", "No account", "Local OCR"];
    chips.forEach((c, i) => {
      fillRound(ctx, 120 + i * 310, 1390, 260, 74, 28, "#d7ff3f");
      text(ctx, c, 250 + i * 310, 1412, 23, "#07100c", 900, "center");
    });
    ctx.restore();
  }
  if (t >= 17.75) {
    ctx.save();
    ctx.globalAlpha = s4;
    const logoSize = 170;
    ctx.drawImage(images.icon, W / 2 - logoSize / 2, 305, logoSize, logoSize);
    centerText(ctx, "Nudge", 520, 76, "#f7fff7", 900, 1);
    centerText(ctx, "Official app on Google Play", 615, 36, "#b8d2c7", 800, 1);
    fillRound(ctx, 205, 760, 670, 104, 32, "#d7ff3f");
    centerText(ctx, "Get Nudge", 792, 38, "#07100c", 900, 1);
    centerText(ctx, "Track expenses without turning your money", 980, 31, "#f7fff7", 800, 1);
    centerText(ctx, "into someone else's dataset.", 1036, 31, "#f7fff7", 800, 1);
    const tags = ["Local-first", "Receipt OCR", "Smart imports", "Share sheet"];
    tags.forEach((c, i) => {
      const x = 118 + i * 212;
      fillRound(ctx, x, 1165, 190, 58, 22, "#132c22");
      text(ctx, c, x + 95, 1182, 19, "#d7ff3f", 900, "center");
    });
    centerText(ctx, "Calm money tracking for Android.", 1310, 30, "#b8d2c7", 750, 1);
    ctx.restore();
  }
}
window.trailerReady = Promise.all(Object.entries(assets).map(async ([key, src]) => { images[key] = await load(src); })).then(async () => {
  await document.fonts.load("900 80px 'JetBrains Mono'");
  await document.fonts.ready;
  drawFrame();
});
window.renderTrailer = time => { t = time; drawFrame(); };
</script>
</body>
</html>`);

// Reuse one headless browser, rendering every animation frame at its exact time.
const browserProfile = mkdtempSync(join(tmpdir(), "nudge-trailer-"));
const browserProcess = spawn(edge, [
  "--headless", "--disable-gpu", "--hide-scrollbars", "--no-first-run",
  "--remote-debugging-port=0", `--user-data-dir=${browserProfile}`, "about:blank",
], { stdio: "ignore", windowsHide: true });
const pause = ms => new Promise(resolve => setTimeout(resolve, ms));
let socket;
try {
  const portFile = join(browserProfile, "DevToolsActivePort");
  for (let attempt = 0; !existsSync(portFile); attempt++) {
    if (attempt > 100) throw new Error("Headless browser did not start");
    await pause(100);
  }
  const port = readFileSync(portFile, "utf8").split(/\r?\n/)[0];
  const tabs = await (await fetch(`http://127.0.0.1:${port}/json/list`)).json();
  socket = new WebSocket(tabs.find(tab => tab.type === "page").webSocketDebuggerUrl);
  await new Promise((resolve, reject) => {
    socket.addEventListener("open", resolve, { once: true });
    socket.addEventListener("error", reject, { once: true });
  });
  let id = 0;
  const pending = new Map();
  socket.addEventListener("message", event => {
    const message = JSON.parse(event.data);
    const request = pending.get(message.id);
    if (!request) return;
    pending.delete(message.id);
    clearTimeout(request.timeout);
    if (message.error) request.reject(new Error(JSON.stringify(message.error)));
    else request.resolve(message.result);
  });
  const send = (method, params = {}) => new Promise((resolve, reject) => {
    const requestId = ++id;
    const timeout = setTimeout(() => { pending.delete(requestId); reject(new Error(method + " timed out")); }, 30000);
    pending.set(requestId, { resolve, reject, timeout });
    socket.send(JSON.stringify({ id: requestId, method, params }));
  });
  await send("Emulation.setDeviceMetricsOverride", { width: 1080, height: 1920, deviceScaleFactor: 1, mobile: false });
  await send("Page.navigate", { url: pathToFileURL(htmlPath).href });
  for (let attempt = 0; ; attempt++) {
    const ready = await send("Runtime.evaluate", { expression: "Boolean(window.trailerReady)", returnByValue: true });
    if (ready.result.value) break;
    if (attempt > 100) throw new Error("Trailer page did not load");
    await pause(100);
  }
  const loaded = await send("Runtime.evaluate", { expression: "window.trailerReady", awaitPromise: true });
  if (loaded.exceptionDetails) throw new Error("Trailer assets failed to load");
  const previewTime = process.env.NUDGE_TRAILER_PREVIEW;
  const frameIndices = previewTime ? [Math.round(Number(previewTime) * fps)] : Array.from({ length: frames }, (_, i) => i);
  for (const index of frameIndices) {
    const result = await send("Runtime.evaluate", { expression: `window.renderTrailer(${index / fps})` });
    if (result.exceptionDetails) throw new Error(`Frame ${index} failed: ${JSON.stringify(result.exceptionDetails)}`);
    const screenshot = await send("Page.captureScreenshot", { format: "png", clip: { x: 0, y: 0, width: 1080, height: 1920, scale: 1 } });
    const file = join(frameDir, `frame-${String(index).padStart(4, "0")}.png`);
    writeFileSync(file, Buffer.from(screenshot.data, "base64"));
    if ((index + 1) % 120 === 0) console.log(`Rendered ${index + 1}/${frames} frames`);
  }
} finally {
  socket?.close();
  browserProcess.kill();
}
if (process.env.NUDGE_TRAILER_PREVIEW) process.exit(0);

const ffmpeg = spawnSync("ffmpeg", [
  "-y",
  "-framerate", String(fps),
  "-i", join(frameDir, "frame-%04d.png"),
  "-vf", "fps=60,format=yuv420p",
  "-frames:v", String(frames),
  "-c:v", "libx264",
  "-profile:v", "high",
  "-crf", "18",
  "-pix_fmt", "yuv420p",
  "-movflags", "+faststart",
  outputMp4,
], { stdio: "inherit" });

if (ffmpeg.status !== 0) {
  throw new Error("FFmpeg failed to encode the trailer");
}

console.log(outputMp4);
