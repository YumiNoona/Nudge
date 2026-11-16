# Nudge trailer

`trailer/nudge-app-trailer-vertical.mp4` is the finished 20-second, 60 fps vertical app trailer. Its source is `tools/make_nudge_trailer.mjs` at the repository root.

The renderer requires Node.js, Playwright, Microsoft Edge and FFmpeg. It creates temporary HTML and PNG frames under `marketing/trailer`; those generated intermediates are ignored by Git and can be regenerated. The website and Android builds do not consume this folder.
