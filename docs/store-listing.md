# Play Store listing prep — Bloom

## Short description (80 chars)

A quiet, private period tracker. Your cycles stay encrypted on your phone.

## Full description (draft)

Bloom is a calm place for your cycle: log a period in seconds, see where you are
today, and read what your logged history actually says — charts, patterns and
optional reflections. No account, no feed, no streaks.

- Private by design: periods and symptoms are encrypted on your device.
- Honest predictions: estimates with their uncertainty spelled out, never false certainty.
- Optional chat rooms with people at the same point in their cycle — pseudonymous,
  moderated, with report and block built in.
- Free core: logging, calendar, charts and reminders stay free.

Bloom is not a medical device and gives no medical advice.

## Content rating (questionnaire guidance)

- User-generated content: **yes** (anonymous chat, text only; report + block in-app,
  server-side authorship/length rules). No photo/video sharing, no direct messages.
- Sexual content: none explicit; reproductive-health education (NHS/NICE-cited).
- Expected outcome: **Teen** (or regional equivalent) on the strength of
  unsupervised anonymous interaction. Confirm in the rating questionnaire.
- Ads: none. In-app purchases: none in v1 (premium readings unlock on-device;
  Play Billing arrives with the store release).

## Target audience

Adults (18+). Not directed at children; see privacy policy §5.

## Medical disclaimer (listing)

"Health & fitness. Bloom is not a medical device. Predictions are estimates, not
certainty; fertility information is not contraception advice. Consult a clinician
for medical concerns." Stainless version also ships in-app (onboarding privacy
step, Settings, fertility/cycle screens).

## Release checklist (store release, not this PR)

Real `keystore.properties` → signed AAB → Play Console internal track →
content-rating questionnaire → data-safety form (see `data-safety.md`) →
privacy-policy URL (see `privacy-policy.md`) → medical disclaimer review.
