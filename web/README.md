<div align="center">
  <img src="public/nudge-icon.png" width="112" alt="Nudge app icon" />
  <h1>Nudge — App Website</h1>
  <p>A private, polished home for the Nudge Android expense manager.</p>

  ![Vite](https://img.shields.io/badge/Vite-8-1f6f50?style=flat-square)
  ![React](https://img.shields.io/badge/React-19-1f6f50?style=flat-square)
  ![Vercel](https://img.shields.io/badge/Vercel-ready-111111?style=flat-square)
  ![Privacy](https://img.shields.io/badge/privacy-on--device-d5ff37?style=flat-square&labelColor=111111)
</div>

## About

This folder contains the public landing page and privacy policy for Nudge. It is isolated from the Android Gradle modules, so website dependencies and build output do not participate in the mobile build.

The site includes:

- A responsive product landing page
- Dark and light color themes
- A realistic Nudge transaction preview
- Product, privacy and trust sections
- A dedicated `/privacy` route for Google Play
- Locally bundled DM Sans and JetBrains Mono fonts
- Open Graph artwork for social previews
- Vercel SPA routing and basic security headers

No database, authentication, analytics or cookie banner is required.

## Project structure

```text
web/
├─ public/
│  ├─ nudge-icon.png
│  └─ og.png
├─ src/
│  ├─ main.tsx
│  └─ styles.css
├─ index.html
├─ vercel.json
├─ vite.config.ts
└─ package.json
```

## Requirements

- Node.js 22 or newer
- npm

## Development

```powershell
npm ci --prefix web
npm run dev --prefix web
```

Vite prints the local preview URL after starting.

## Quality checks

```powershell
npm run lint --prefix web
npm run build --prefix web
```

The production output is generated in `dist/` and is intentionally excluded from Git.

## Deploy to Vercel

1. In Vercel, select **Add New → Project** and import the Nudge repository.
2. Set the Vercel Root Directory to `web`.
3. Deploy, then open **Project settings → Domains** and connect your domain.

The included `vercel.json` installs and builds from this folder, publishes `dist`, and keeps direct links such as `/privacy` working after refresh.

## Google Play privacy URL

After connecting the domain, use this URL in Google Play Console:

```text
https://your-domain.com/privacy
```

Replace `your-domain.com` with the final production hostname. Keep the page publicly accessible without login.

## Useful links

- [Nudge Android repository](https://github.com/YumiNoona/Nudge)
- [Nudge releases](https://github.com/YumiNoona/Nudge/releases)
- [Issue tracker and support](https://github.com/YumiNoona/Nudge/issues)

## License

The Nudge name, application artwork and product content belong to the project owner. Refer to the Android repository for the application license.
