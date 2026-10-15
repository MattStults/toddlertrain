# Toddler Train

> **Written June–July 2014** (23 commits, all by Matt Stults). This is old personal work, not
> recent code. It was moved from Bitbucket to GitHub in October 2026 with its full history; the
> code is unchanged since July 2014. This README is the only later addition.
> It predates AI coding tools: every line of code here was written by hand.

An Android game for very young children: a train runs around a looping track, and the child
drags it with a finger. It also has a "toddler lock" that keeps the child inside the app.

## What's interesting in it

About 2,200 lines of Java in two packages:

- **`com.mattstults.track`** generates the track layouts offline as SVG.
  - `TrackGenerator` enumerates layouts as permutations of track positions and treats a layout
    and its mirror images as one (`Mirror.buildCanonical`).
  - `Cubic` solves for Bézier control points so a curve passes through chosen points, and
    computes offset curves along it.
  - `TaperedCubic` uses those offset curves to draw rails that taper with perspective.
- **`com.mattstultsdragexperiments`** is the app itself.
  - `DrawPanel` runs a render thread at a fixed frame rate, feeds touch events through a queue,
    and measures FPS.
  - `TrackModel` cuts each track into pieces, loads the next track in the background, and draws
    the train cars at the right depth where the track crosses over itself.
  - `Train` moves the train to the point on the track nearest the child's finger, with a cap on
    how far it can move per frame.
  - `ToddlerLockButton` / `ToddlerLockService` implement the toddler lock: five taps on the lock
    icon unlock the app, and the service brings the app back to the front if the child leaves it.

## Status and known issues

This is a working prototype, not a finished product. There are no automated tests, some
exceptions are silently swallowed, and some debug drawing was left in. It is an Eclipse ADT
project from 2014 (target SDK 19) and would need migrating to build in current Android
Studio. The toddler lock uses `ActivityManager.getRunningTasks`, which Android later
restricted.

## Third-party code

- `libs/android-support-v4.jar`: Android Support Library.
- `libs/svgAndroid2-3Jan2014.jar`: svg-android, an SVG parser for Android (`com.larvalabs.svgandroid`).
