# Play Data safety — Bloom

Pre-filled answers for the Play Console Data safety form. Re-verify at listing time;
this reflects the v1 implementation (local-first + Firebase chat, no billing SDK yet).

## Does the app collect or share user data? — Yes

## Data collected (on device and/or transmitted)

| Category | Types | Collected | Shared | Purpose |
|---|---|---|---|---|
| Health and fitness → Health info | Period dates, symptoms, cycle settings (user-entered) | Yes — stored encrypted on device (SQLCipher); **never transmitted** | No | App functionality |
| Personal info → Name | Chat pseudonym (user-made-up, e.g. "Quiet Fern") | Yes — transmitted to Firebase when posting | Yes — Firebase (Google), to display in shared rooms | App functionality |
| Personal info → User IDs | Anonymous Firebase auth uid (random, no account) | Yes — transmitted on sign-in/chat | Yes — Firebase (Google), authorship + abuse prevention | Fraud prevention, security |
| App info and performance → Crash logs | Via Play Console crash reporting if enabled | Per Console defaults | Google | Analytics (Console) |
| Device or other IDs | Play Integrity verdicts (App Check, release) | Yes — transmitted for attestation | Yes — Google Play, bot/abuse prevention | Fraud prevention, security |

Chat message bodies, phase buckets and cycle-length bands are transmitted to Firebase
as part of rooms; they contain no names, dates or birth dates by design (see `firestore.rules`).

## Not collected

Location, contacts, photos/media, files, audio, financial info, messages outside
in-app chat, advertising ID. No third-party advertising or analytics SDKs.

## Security

- Data encrypted in transit (TLS to Firebase).
- Local health data encrypted at rest (SQLCipher, device-held key).
- Users can request deletion in-app: Settings → Delete all my data
  (`CycleRepository.eraseEverything()`); chat posts are covered by report/block and
  the retention policy in the privacy policy.

## Accounts

No accounts. Anonymous Firebase auth only (no email, no phone).
