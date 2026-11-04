# Privacy

Go collects nothing, sends nothing, and asks for no permissions.

That is the whole policy. The rest of this page is only the evidence for it, because a
privacy policy that cannot be checked is just a promise.

## No permissions

`app/src/main/AndroidManifest.xml` declares no `<uses-permission>` at all. Not network, not
storage, not identifiers. Android will not grant the app anything it has not asked for, so
there is no data it is capable of reaching.

## No network

There is no networking code in this app, and no dependency that provides any. Without the
`INTERNET` permission the app could not open a connection even if there were. Nothing about
your games — moves, results, settings, how often you play — can leave the device, because
there is no route off it.

The opponent is not a service. It is [GNU Go](https://www.gnu.org/software/gnugo/), a
program included in the APK, which runs on your phone as a child process and is spoken to
over a pipe.

## What is stored, and where

One file, in the app's own private storage, holding the game currently in progress:

```
files/game-in-progress.txt
```

It contains a format version, your four game settings (opponent, difficulty, your colour,
handicap), and the list of moves played so far — for example `E5 D3 pass F4`. It exists so
that turning your phone off in the middle of a game does not lose the game. It is deleted
when the game is finished, resigned, or left.

Android keeps that directory private to the app. Uninstalling removes it.

Nothing else is written. There are no analytics, no crash reporting, no advertising
identifiers, no logs kept between runs, and no accounts.

## No third-party services

The app has no SDKs for analytics, advertising, attribution, or crash reporting. Its
dependencies are AndroidX and Jetpack Compose, Mudita's MMD design system, and the bundled
GNU Go engine — none of which the app uses to communicate with anything.

## Verifying this yourself

You do not have to take any of it on trust:

- The source is at <https://github.com/wanderwildwood/kuroban>, and each release is tagged.
- `grep -r uses-permission app/src/main/AndroidManifest.xml` returns nothing.
- Any APK can be checked with `aapt dump permissions` or by opening it as a zip.

<!-- shared-about privacy section -->
## Who we are

Go is made by wander wildwood, an independent developer: one person, not a company. wander wildwood is the data controller for anything the app itself handles, and that is very little. Go keeps what you put in it on your phone and sends nothing to the developer, so the developer holds no data about you.

## How to contact us

Write to email@wanderthe.dev about your data, this policy or anything else. The same address is in the app, under About.

## Your rights

If you are in the EU, the EEA or the UK, the GDPR gives you the right to:

- see the personal data held about you (access)
- have it corrected (rectification) or deleted (erasure)
- limit how it is used (restriction)
- object to its use (objection)
- get a copy to take elsewhere (portability)
- withdraw your consent at any time, where something rests on it
- complain to a data protection supervisory authority, for example the one where you live

Similar laws elsewhere, such as the CCPA in California, give you the right to know, delete and correct, and to opt out of the sale or sharing of your data. Nothing from Go is sold or shared.

Since Go keeps what it stores only on your phone, you can exercise most of these rights yourself: resign or leave the game in progress, which deletes its saved moves, clear the app's data in Android's settings, or uninstall the app, which deletes all of it. For anything you cannot do yourself, write to the address above.
<!-- /shared-about privacy section -->

## Changes

If this ever stops being true, this file changes in the same commit as the code that
changed it, and the release notes will say so plainly.
