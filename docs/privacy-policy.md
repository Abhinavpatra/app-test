# Bloom — Privacy Policy (v1 draft)

> Replace `[CONTACT]` with a real support address before listing. This draft matches
> the shipped implementation; have it reviewed before the store release.

Bloom ("the app") is a period tracker that keeps your health data on your phone.
There is no account, no feed and no advertising.

## 1. What stays on your device

- Period dates, symptoms, cycle settings, birth date (optional), reminder
  preferences: stored in an encrypted database (SQLCipher) with a key only your
  device holds. We cannot read them, and they are never uploaded.
- Clearing the app's data, uninstalling, or Settings → Delete all my data removes
  them permanently. There is no server copy to recover.

## 2. What leaves your device

- **Chat posts.** Anything you post in a chat room is sent to our servers (Google
  Firebase) so other people in the room can read it. A post contains: the message
  text, a made-up display name, a coarse phase label (e.g. "near ovulation") and a
  cycle-length band. Never your real name, exact dates or birth date.
- **Anonymous sign-in.** To post, the app signs in anonymously (a random id, no
  email or phone) so the server can prove a message is yours and enforce the
  posting rules.
- **Abuse prevention.** Release builds use Play Integrity attestation (App Check)
  to stop bots; this sends device-integrity signals to Google.

## 3. What we never do

No sale of data, no advertising SDKs, no cross-app tracking, no health-data
sharing with third parties. Firebase (Google) processes chat and attestation data
solely to run those features.

## 4. Your rights

Export (Settings → Export my data, machine-readable JSON) and deletion (Settings →
Delete all my data) are built into the app and work without contacting us. For
anything else, including questions about chat posts still on the server: `[CONTACT]`.

## 5. Children

The app is intended for adults (see store listing). Chat is an anonymous,
user-moderated space with report and block tools, but it is not supervised in real
time — keep that in mind before posting anything personal.

## 6. Medical disclaimer

Bloom is not a medical device. Predictions and readings are estimates built from
what you log — not diagnoses, not contraception advice, not pregnancy tests. Talk
to a clinician about anything that concerns you.
