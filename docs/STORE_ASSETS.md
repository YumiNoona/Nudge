# Google Play store assets

Google Play Console requires store media that must represent the shipping app truthfully. Do not use generated UI screenshots.

## Required

| Asset | Requirement | Nudge source/action |
|---|---|---|
| App icon | 512×512 PNG, up to 1 MB | Export a 512×512 copy from `Nudge.png` (the source is 1254×1254). Keep the artwork identical and do not add Play-style rounded corners. |
| Feature graphic | 1024×500 JPG or 24-bit PNG, no alpha | Create a restrained Nudge graphic using the forest/acid-lime palette, app icon, and a short non-promotional line such as “Your money, one calm timeline.” |
| Phone screenshots | At least 2; use 1080×1920 or another accepted phone aspect ratio | Capture the real signed Play build on a clean test profile. Recommended sequence is in `PLAY_STORE_LISTING.md`. |

## Capture checklist

- Use test transactions and fictional names; never expose real SMS bodies, account numbers, email addresses, UPI IDs or balances.
- Capture both automatic and manual flows only after granting the permission shown in the screenshot.
- Keep status-bar time, network and battery consistent where practical.
- Do not add device frames, awards, rankings, prices or claims that cannot be substantiated.
- Show the actual current UI and avoid overlays that imply unavailable features.
- Review every image at full resolution before upload.

## Optional but recommended

- 7-inch and 10-inch tablet screenshots after verifying the adaptive layouts on real/emulated tablets.
- A short preview video hosted on YouTube with monetization disabled.
- Localized listing graphics only after the corresponding store text is translated and reviewed.

