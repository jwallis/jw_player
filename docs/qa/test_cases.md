# JW Player - Test Cases

Maps to [user_stories.md](user_stories.md) (PLAYER-001 .. PLAYER-025).
IDs are sequential across the whole file: PLAYER_TC-001+.
"Automatable (Appium)" reflects practical UI-automation feasibility, not
theoretical possibility (e.g. system file-picker dialogs and internal
player-volume/timing state are marked No even though Appium can technically
reach across apps, because they are flaky/opaque in practice).
"Jira Issue ID" is the Jira key of the user story that caused this test case to
be written. Set to TBD for all cases below since these predate the Jira
integration; will be populated for real once stories move to Jira.

## Test Data / Fixture Setup

Unless a test case says otherwise, assume this folder tree exists under a
root folder that has already been granted to the app:

```
Root/
  FolderA/
    FolderA1/                 (contains 2 playable audio files)
    track_a1.mp3               (~10s)
    track_a2.mp3               (~10s)
  .hidden_folder/               (name starts with ".")
  .hidden_track.mp3             (name starts with ".")
  mixed_ext/
    song.mp3
    song.m4a
    song.wav
    song.flac                   (unsupported extension)
    notes.txt                   (unsupported extension)
  many_items/                   (30+ folders/files - more than one screen)
  seek_test.mp3                 (exactly 60s duration, no ID3 artist tag)
  Long Track With A Really Long Title And Artist Name.mp3
                                 (~90s, has an ID3 artist tag; title+artist
                                  combined text is wider than the screen)

white_noise_sample.mp3           (short file, ~5s, picked separately via
                                  the white-noise file picker, does not
                                  need to live under Root/)
```

TC IDs below reference files from this tree by name.

## Epic: Library Browsing & Navigation

---
### PLAYER_TC-001: Selecting a root folder persists it and updates the button label
**Story:** PLAYER-001
**Jira Issue ID:** TBD
**Priority:** High
**Automatable (Appium):** No (system folder picker / DocumentsUI is out of app control)
**Test Data:** Root/ folder from fixture setup
**Steps:**
 1. Open Settings, note the "Root Folder" button reads "Select Folder".
 2. Tap the button, pick Root/ in the system folder picker.
 3. Confirm the button now reads "Root".
 4. Force-stop and relaunch the app, reopen Settings.
    Expected: Button still reads "Root" (selection survived restart) and
    the main screen browses Root/'s contents.
---
### PLAYER_TC-002: Main screen prompts for a root folder when none is set
**Story:** PLAYER-001
**Jira Issue ID:** TBD
**Priority:** Medium
**Automatable (Appium):** Yes
**Test Data:** Fresh app install / cleared app data (no root folder chosen)
**Steps:**
 1. Launch the app on a fresh install, past the splash screen.
    Expected: Main screen shows the text "set the root folder" instead of
    a file/folder list.
---
### PLAYER_TC-003: Folder contents are listed sorted, folders before files, with correct icons
**Story:** PLAYER-002
**Jira Issue ID:** TBD
**Priority:** High
**Automatable (Appium):** Yes
**Test Data:** Root/ (contains FolderA, mixed_ext, many_items as folders; seek_test.mp3,
           "Long Track..." as files)
**Steps:**
 1. Navigate to Root/.
 2. Read the row order and each row's icon.
    Expected: All folders appear first (alphabetical, case-insensitive),
    followed by all playable files (alphabetical); folder rows show a
    folder icon, file rows show a music-note icon.
---
### PLAYER_TC-004: Hidden files and folders (leading ".") are excluded from the listing
**Story:** PLAYER-002
**Jira Issue ID:** TBD
**Priority:** Medium
**Automatable (Appium):** Yes
**Test Data:** Root/ containing .hidden_folder and .hidden_track.mp3
**Steps:**
 1. Navigate to Root/.
    Expected: Neither ".hidden_folder" nor ".hidden_track.mp3" appears
    anywhere in the list.
---
### PLAYER_TC-005: Only supported audio extensions (mp3, m4a, wav) are listed as files
**Story:** PLAYER-002
**Jira Issue ID:** TBD
**Priority:** High
**Automatable (Appium):** Yes
**Test Data:** mixed_ext/ (song.mp3, song.m4a, song.wav, song.flac, notes.txt)
**Steps:**
 1. Navigate to Root/mixed_ext/.
    Expected: song.mp3, song.m4a and song.wav are listed; song.flac and
    notes.txt are not listed anywhere.
---
### PLAYER_TC-006: Tapping a folder opens it and shows its contents
**Story:** PLAYER-003
**Jira Issue ID:** TBD
**Priority:** High
**Automatable (Appium):** Yes
**Test Data:** Root/FolderA/ (contains FolderA1, track_a1.mp3, track_a2.mp3)
**Steps:**
 1. Navigate to Root/.
 2. Tap "FolderA".
    Expected: Screen now shows FolderA's contents (FolderA1, track_a1.mp3,
    track_a2.mp3) and the pinned header reads "FolderA".
---
### PLAYER_TC-007: System back button navigates up one level from a subfolder
**Story:** PLAYER-004
**Jira Issue ID:** TBD
**Priority:** High
**Automatable (Appium):** Yes
**Test Data:** Root/FolderA/FolderA1/
**Steps:**
 1. Navigate to Root/FolderA/FolderA1/.
 2. Press the system back button.
    Expected: Screen shows Root/FolderA/'s contents (one level up), app
    remains in the foreground.
---
### PLAYER_TC-008: System back button backgrounds the app when at the root folder
**Story:** PLAYER-005
**Jira Issue ID:** TBD
**Priority:** High
**Automatable (Appium):** Yes
**Test Data:** Root/ (already selected as root folder)
**Steps:**
 1. Navigate to Root/ (the root folder itself, not a subfolder).
 2. Press the system back button.
    Expected: App moves to the background (device shows the previous
    app/home screen); app process is not killed (re-opening it returns to
    Root/, not the splash screen).
---
### PLAYER_TC-009: Subfolder name stays pinned at the top while its contents scroll, and tapping it navigates up
**Story:** PLAYER-006
**Jira Issue ID:** TBD
**Priority:** Medium
**Automatable (Appium):** Yes
**Test Data:** many_items/ (30+ entries, overflows one screen)
**Steps:**
 1. Navigate to Root/many_items/.
    Expected: Header reads "many_items" at the very top of the screen.
 2. Scroll the list down through its contents.
    Expected: The "many_items" header remains fixed at the top and does
    not scroll away; only the folder/file rows beneath it scroll.
 3. Tap the pinned "many_items" header.
    Expected: Screen navigates up to Root/.
---
### PLAYER_TC-010: Scroll-edge arrows appear/disappear correctly as the list is scrolled
**Story:** PLAYER-007
**Jira Issue ID:** TBD
**Priority:** Medium
**Automatable (Appium):** Yes
**Test Data:** many_items/ (30+ entries, overflows one screen)
**Steps:**
 1. Navigate to Root/many_items/ and observe the top of the list.
    Expected: No up arrow is shown (already at the top); a static down
    arrow is shown in place of the bottom visible row (more content below).
 2. Scroll down partway (not to the end).
    Expected: A static up arrow now replaces the top visible row; the
    down arrow is still shown at the bottom.
 3. Continue scrolling to the very bottom of the list.
    Expected: The down arrow is gone and the true last item is visible;
    the up arrow is still shown at the top.
 4. Scroll back to the very top.
    Expected: The up arrow is gone and the true first item is visible
    again; the down arrow re-appears at the bottom.
---
### PLAYER_TC-011: The currently playing file is highlighted in its folder's list
**Story:** PLAYER-008
**Jira Issue ID:** TBD
**Priority:** Medium
**Automatable (Appium):** Yes
**Test Data:** Root/seek_test.mp3
**Steps:**
 1. Navigate to Root/ and tap seek_test.mp3 to start playback.
 2. While it is playing, remain on / return to Root/'s listing.
    Expected: The seek_test.mp3 row shows inverted/highlighted colors
    (background and text swapped) compared to other rows.
---
### PLAYER_TC-012: Revisiting a folder within the same session does not re-read it from disk
**Story:** PLAYER-009
**Jira Issue ID:** TBD
**Priority:** Medium
**Automatable (Appium):** No (cache-hit vs. disk-read is an internal timing/
                       implementation detail not exposed to UI automation)
**Test Data:** Root/FolderA/ (containing several subfolders/files)
**Steps:**
 1. Navigate to Root/FolderA/ and note the time until its contents render.
 2. Navigate into FolderA/FolderA1/, then press back to return to FolderA/.
    Expected: FolderA/'s contents render immediately (no visible loading
    delay or flicker) on the second visit, and the listing is identical
    to step 1.
---
### PLAYER_TC-013: Drilling into a subfolder that was visible on screen loads instantly
**Story:** PLAYER-010
**Jira Issue ID:** TBD
**Priority:** Medium
**Automatable (Appium):** No (background-prefetch timing is an internal
                       implementation detail, not directly observable via UI)
**Test Data:** Root/FolderA/ (containing FolderA1)
**Steps:**
 1. Navigate to Root/FolderA/ and wait briefly (~1s) for the background
    prefetch of its subfolders to complete.
 2. Tap "FolderA1".
    Expected: FolderA1's contents render with no visible loading delay.

## Epic: Library Playback

---
### PLAYER_TC-014: Tapping a file starts playback and queues its sibling files
**Story:** PLAYER-011
**Jira Issue ID:** TBD
**Priority:** High
**Automatable (Appium):** Yes
**Test Data:** Root/FolderA/ (track_a1.mp3, track_a2.mp3)
**Steps:**
 1. Navigate to Root/FolderA/ and tap track_a1.mp3.
    Expected: Mini player shows track_a1 as the current track and starts
    playing (play/pause button shows the "pause" icon).
 2. Tap the "next" skip button.
    Expected: Playback advances to track_a2.mp3 (the other sibling file
    in the same folder), confirming both files were queued together.
---
### PLAYER_TC-015: Play/pause button toggles playback and preserves position on resume
**Story:** PLAYER-012
**Jira Issue ID:** TBD
**Priority:** High
**Automatable (Appium):** Yes
**Test Data:** Root/seek_test.mp3 (60s duration)
**Steps:**
 1. Play seek_test.mp3, let it play for ~10 seconds.
 2. Tap play/pause.
    Expected: Playback pauses; icon switches to "play"; elapsed time
    stops advancing at roughly 0:10.
 3. Tap play/pause again.
    Expected: Playback resumes from ~0:10 (not restarted from 0:00); icon
    switches back to "pause".
---
### PLAYER_TC-016: Mini player shows the current track's title and artist
**Story:** PLAYER-013
**Jira Issue ID:** TBD
**Priority:** Medium
**Automatable (Appium):** Yes
**Test Data:** "Long Track With A Really Long Title And Artist Name.mp3" (has
           an ID3 artist tag); Root/seek_test.mp3 (no artist tag)
**Steps:**
 1. Play seek_test.mp3.
    Expected: Mini player shows just the title text (no " - " separator),
    since there is no artist metadata.
 2. Play "Long Track With A Really Long Title And Artist Name.mp3".
    Expected: Mini player shows "<Artist> - <Title>" on a single line.
---
### PLAYER_TC-017: Title/artist text that overflows the screen width scrolls (marquee)
**Story:** PLAYER-013
**Jira Issue ID:** TBD
**Priority:** Low
**Automatable (Appium):** No (verifying an animated scroll position needs
                       visual/frame comparison beyond standard element
                       assertions)
**Test Data:** "Long Track With A Really Long Title And Artist Name.mp3"
**Steps:**
 1. Play the long-titled track.
 2. Observe the title/artist line for several seconds.
    Expected: The text scrolls horizontally (marquee) since it is wider
    than the screen; a short title (e.g. seek_test.mp3) does not scroll.
---
### PLAYER_TC-018: Elapsed time displays as MM:SS and updates during playback
**Story:** PLAYER-014
**Jira Issue ID:** TBD
**Priority:** High
**Automatable (Appium):** Yes
**Test Data:** Root/seek_test.mp3 (60s duration)
**Steps:**
 1. Play seek_test.mp3 and read the elapsed time immediately.
    Expected: Text reads "00:00" (or close to it), centered above the
    seek bar.
 2. Wait 5 seconds and read the elapsed time again.
    Expected: Text now reads approximately "00:05" and continues to
    advance while playing.
---
### PLAYER_TC-019: Tapping a point on the seek bar jumps playback to that position
**Story:** PLAYER-015
**Jira Issue ID:** TBD
**Priority:** High
**Automatable (Appium):** Yes
**Test Data:** Root/seek_test.mp3 (60s duration, so the bar's midpoint = ~30s)
**Steps:**
 1. Play seek_test.mp3.
 2. Tap the horizontal midpoint of the seek bar.
    Expected: Elapsed time jumps to approximately "00:30" and playback
    continues from there.
---
### PLAYER_TC-020: Dragging the seek bar thumb scrubs and commits the position on release
**Story:** PLAYER-016
**Jira Issue ID:** TBD
**Priority:** High
**Automatable (Appium):** Yes
**Test Data:** Root/seek_test.mp3 (60s duration)
**Steps:**
 1. Play seek_test.mp3.
 2. Press down near the start of the seek bar and drag to roughly 75% of
    its width, then release.
    Expected: Elapsed time updates live while dragging, and on release
    settles at approximately "00:45" with playback continuing from there.
---
### PLAYER_TC-021: "Previous" button jumps to the previous track when within the first 3 seconds
**Story:** PLAYER-017
**Jira Issue ID:** TBD
**Priority:** High
**Automatable (Appium):** Yes
**Test Data:** Root/FolderA/ (track_a1.mp3, track_a2.mp3)
**Steps:**
 1. Play track_a1.mp3, then tap "next" to move to track_a2.mp3.
 2. Within 3 seconds of track_a2.mp3 starting, tap the "previous" button.
    Expected: Playback switches to track_a1.mp3 (the previous track in
    the queue), starting from 0:00.
---
### PLAYER_TC-022: "Previous" button restarts the current track when more than 3 seconds have elapsed
**Story:** PLAYER-017
**Jira Issue ID:** TBD
**Priority:** High
**Automatable (Appium):** Yes
**Test Data:** Root/seek_test.mp3 (60s duration)
**Steps:**
 1. Play seek_test.mp3 and let it play for at least 5 seconds.
 2. Tap the "previous" button.
    Expected: seek_test.mp3 restarts from 0:00 (elapsed time resets),
    same track continues playing rather than moving to a different file.
---
### PLAYER_TC-023: "Next" button on the last track in the queue wraps to the first track
**Story:** PLAYER-017
**Jira Issue ID:** TBD
**Priority:** Medium
**Automatable (Appium):** Yes
**Test Data:** Root/FolderA/ (track_a1.mp3, track_a2.mp3 - 2-file queue)
**Steps:**
 1. Play track_a1.mp3, tap "next" once to reach track_a2.mp3 (the last
    track in the queue).
 2. Tap "next" again.
    Expected: Playback wraps around to track_a1.mp3 (the first track),
    rather than stopping.
---
### PLAYER_TC-024: Holding the rewind button seeks backward and stops cleanly at the start
**Story:** PLAYER-018
**Jira Issue ID:** TBD
**Priority:** Medium
**Automatable (Appium):** Yes
**Test Data:** Root/seek_test.mp3 (60s duration)
**Steps:**
 1. Play seek_test.mp3 and let it reach roughly 0:05.
 2. Press and hold the rewind button until elapsed time reaches 0:00.
 3. Release the button.
    Expected: Playback stops seeking at exactly 0:00 (does not go
    negative/error), and resumes normal playback from 0:00.
---
### PLAYER_TC-025: Holding the fast-forward button to the end of a track advances to the next track
**Story:** PLAYER-018
**Jira Issue ID:** TBD
**Priority:** Medium
**Automatable (Appium):** Yes
**Test Data:** Root/FolderA/ (track_a1.mp3 ~10s, track_a2.mp3)
**Steps:**
 1. Play track_a1.mp3.
 2. Press and hold the fast-forward button until it reaches the end of
    track_a1.mp3.
    Expected: Playback automatically advances to track_a2.mp3 and
    continues playing (does not stay stuck at the end of track_a1).
---
### PLAYER_TC-026: Releasing a hold-seek gesture mid-track resumes playback from the reached position
**Story:** PLAYER-018
**Jira Issue ID:** TBD
**Priority:** Medium
**Automatable (Appium):** Yes
**Test Data:** Root/seek_test.mp3 (60s duration)
**Steps:**
 1. Play seek_test.mp3 from 0:00.
 2. Press and hold the fast-forward button for ~2 seconds (well short of
    the track's end), then release.
    Expected: Elapsed time is now well ahead of where playback started
    (seek advanced faster than real time), and playback resumes normally
    (audible/playing) from that new position, not paused or muted.
---
### PLAYER_TC-027: Track auto-advances to the next queued track when it finishes naturally
**Story:** PLAYER-019
**Jira Issue ID:** TBD
**Priority:** High
**Automatable (Appium):** Yes
**Test Data:** Root/FolderA/ (track_a1.mp3 ~10s, track_a2.mp3)
**Steps:**
 1. Play track_a1.mp3 and let it play to completion without interacting
    with any controls.
    Expected: When track_a1.mp3 ends, track_a2.mp3 begins playing
    automatically without user action.
---
### PLAYER_TC-028: Playback stops (without wrapping) when the last track finishes naturally
**Story:** PLAYER-019
**Jira Issue ID:** TBD
**Priority:** Medium
**Automatable (Appium):** Yes
**Test Data:** Root/FolderA/ (track_a1.mp3, track_a2.mp3 - 2-file queue)
**Steps:**
 1. Play track_a1.mp3, tap "next" to reach track_a2.mp3 (last in queue).
 2. Let track_a2.mp3 play to completion without interacting with controls.
    Expected: Playback stops after track_a2.mp3 ends; it does NOT wrap
    back around to play track_a1.mp3 automatically.

## Epic: White Noise

---
### PLAYER_TC-029: Selecting a white noise file persists it and updates the button label
**Story:** PLAYER-020
**Jira Issue ID:** TBD
**Priority:** Medium
**Automatable (Appium):** No (system file picker / DocumentsUI is out of app control)
**Test Data:** white_noise_sample.mp3
**Steps:**
 1. Open Settings, note the white-noise button reads "Select File".
 2. Tap the button and pick white_noise_sample.mp3 in the system file
    picker.
 3. Confirm the button now reads "white_noise_sample".
 4. Force-stop and relaunch the app, reopen Settings.
    Expected: Button still reads "white_noise_sample" (selection survived
    restart).
---
### PLAYER_TC-030: Play/pause button toggles white noise playback
**Story:** PLAYER-021
**Jira Issue ID:** TBD
**Priority:** Medium
**Automatable (Appium):** Yes
**Test Data:** white_noise_sample.mp3 already selected in Settings
**Steps:**
 1. In Settings, tap the white-noise play/pause button.
    Expected: Icon switches to "pause" and audio is audible/playing.
 2. Tap the button again.
    Expected: Icon switches back to "play" and audio stops.
---
### PLAYER_TC-031: White noise loops continuously past the end of the file
**Story:** PLAYER-021
**Jira Issue ID:** TBD
**Priority:** Low
**Automatable (Appium):** No (verifying continued playback across a loop
                       boundary needs audio/position instrumentation, not
                       exposed via standard UI elements on this screen)
**Test Data:** white_noise_sample.mp3 (~5s short file)
**Steps:**
 1. Start white noise playback and let it run for longer than the file's
    duration (e.g. 12 seconds for a 5s file).
    Expected: Audio is still playing continuously with no gap or stop
    (it looped at least once) and the play/pause button still shows
    "pause".
---
### PLAYER_TC-032: Starting white noise stops the currently playing library track
**Story:** PLAYER-022
**Jira Issue ID:** TBD
**Priority:** High
**Automatable (Appium):** Yes
**Test Data:** Root/seek_test.mp3; white_noise_sample.mp3 already selected
**Steps:**
 1. Play seek_test.mp3 from the library.
 2. Open Settings and tap the white-noise play button.
    Expected: White noise begins playing; seek_test.mp3 is no longer
    playing (mini player no longer shows it advancing/playing).
---
### PLAYER_TC-033: Pressing play in the mini player while white noise is active restarts the last library track from 0:00
**Story:** PLAYER-022
**Jira Issue ID:** TBD
**Priority:** Medium
**Automatable (Appium):** Yes
**Test Data:** Root/seek_test.mp3; white_noise_sample.mp3
**Steps:**
 1. Play seek_test.mp3, let it reach ~0:15, then start white noise
    (stopping seek_test.mp3 per PLAYER_TC-032).
 2. Return to the main screen and tap the mini player's play button.
    Expected: White noise stops; seek_test.mp3 starts playing again from
    0:00 (not resumed from 0:15).

## Epic: App Shell & Layout

---
### PLAYER_TC-034: Splash screen shows "jw player" for 1.5 seconds on launch
**Story:** PLAYER-023
**Jira Issue ID:** TBD
**Priority:** Low
**Automatable (Appium):** Yes
**Test Data:** None
**Steps:**
 1. Launch the app and immediately capture the screen.
    Expected: A black screen showing "jw player" as a single line of
    text is visible.
 2. Wait 1.5 seconds and capture the screen again.
    Expected: Splash screen is gone; main screen (library browser or
    "set the root folder" prompt) is now visible.
---
### PLAYER_TC-035: App uses a dark theme throughout, including system bars
**Story:** PLAYER-024
**Jira Issue ID:** TBD
**Priority:** Low
**Automatable (Appium):** No (color-scheme verification needs screenshot/
                       visual comparison tooling, not standard element
                       assertions)
**Test Data:** None
**Steps:**
 1. Visually inspect the library browser, mini player, and Settings
    screen, plus the status bar and navigation bar.
    Expected: All screens use a dark background with light text/icons;
    status bar and navigation bar are styled dark to match.
---

## Epic: Accessibility

---
### PLAYER_TC-036: Folder rows, file rows, and the pinned header announce "folder <name>" / "file <name>"
**Story:** PLAYER-025
**Jira Issue ID:** TBD
**Priority:** High
**Automatable (Appium):** Yes (locate via accessibility id / content-desc)
**Test Data:** Root/FolderA/ (contains FolderA1, track_a1.mp3, track_a2.mp3)
**Steps:**
 1. Navigate to Root/FolderA/.
 2. Query the accessibility tree (e.g. driver.findElement(AccessibilityId,
    "folder FolderA1")) for the subfolder row.
    Expected: Element is found and is clickable.
 3. Query for accessibility id "file track_a1" (display name, extension
    stripped per DirectoryLister.displayName).
    Expected: Element is found and is clickable.
 4. Query for accessibility id "folder FolderA" (the pinned header).
    Expected: Element is found; tapping it navigates up to Root/.
---
### PLAYER_TC-037: Scroll-edge arrows announce descriptive text, not silent/decorative
**Story:** PLAYER-025
**Jira Issue ID:** TBD
**Priority:** Medium
**Automatable (Appium):** Yes (locate via accessibility id / content-desc)
**Test Data:** many_items/ (30+ entries, overflows one screen)
**Steps:**
 1. Navigate to Root/many_items/.
 2. Query for accessibility id "More folders or files below".
    Expected: Element is found (down arrow is showing since content
    overflows).
 3. Scroll down partway and query for accessibility id "More folders or
    files above".
    Expected: Element is found (up arrow is now showing).
---
### PLAYER_TC-038: Mini player's title, elapsed time, and seek bar are individually labeled
**Story:** PLAYER-025
**Jira Issue ID:** TBD
**Priority:** Medium
**Automatable (Appium):** Yes (locate via accessibility id / content-desc)
**Test Data:** Root/seek_test.mp3
**Steps:**
 1. Play seek_test.mp3.
 2. Query for an element whose content-desc starts with "Now playing:".
    Expected: Element is found and its content-desc includes the track
    title.
 3. Query for an element whose content-desc starts with "Elapsed time".
    Expected: Element is found and its content-desc includes the current
    MM:SS value (distinct from the visible "00:0X" text node).
 4. Query for accessibility id "Seek bar".
    Expected: Element is found and is a distinct, targetable element
    (not just an unlabeled drag region).
---
### PLAYER_TC-039: Mini player play/pause button's description reflects current state
**Story:** PLAYER-025
**Jira Issue ID:** TBD
**Priority:** High
**Automatable (Appium):** Yes (locate via accessibility id / content-desc)
**Test Data:** Root/seek_test.mp3
**Steps:**
 1. Play seek_test.mp3.
    Expected: An element with accessibility id "Pause" is present (not
    the generic "Play or pause").
 2. Tap it to pause playback.
    Expected: The same button now exposes accessibility id "Play" instead
    of "Pause".
---
### PLAYER_TC-040: Press-and-hold seek buttons are exposed as labeled, actionable buttons
**Story:** PLAYER-025
**Jira Issue ID:** TBD
**Priority:** Medium
**Automatable (Appium):** Yes (locate via accessibility id / content-desc;
                       actual hold-gesture activation via a screen
                       reader's simulated tap is a known limitation, not
                       covered by this test case)
**Test Data:** Root/seek_test.mp3
**Steps:**
 1. Play seek_test.mp3.
 2. Query for accessibility id "Seek backward".
    Expected: Element is found and its accessibility role is Button.
 3. Query for accessibility id "Seek forward".
    Expected: Element is found and its accessibility role is Button.
---
### PLAYER_TC-041: Settings root-folder and white-noise-file buttons announce dynamic descriptions
**Story:** PLAYER-025
**Jira Issue ID:** TBD
**Priority:** Medium
**Automatable (Appium):** Yes (locate via accessibility id / content-desc)
**Test Data:** Fresh app data (nothing selected yet); Root/ folder; white_noise_sample.mp3
**Steps:**
 1. Open Settings before anything is selected.
    Expected: Root-folder button exposes accessibility id "Select root
    folder"; white-noise button exposes accessibility id "Select white
    noise file".
 2. Select Root/ as the root folder and white_noise_sample.mp3 as the
    white noise file.
    Expected: Root-folder button now exposes accessibility id "folder
    Root"; white-noise button now exposes accessibility id "file
    white_noise_sample".
---
### PLAYER_TC-042: White noise play/pause button's description reflects current state
**Story:** PLAYER-025
**Jira Issue ID:** TBD
**Priority:** Medium
**Automatable (Appium):** Yes (locate via accessibility id / content-desc)
**Test Data:** white_noise_sample.mp3 already selected in Settings
**Steps:**
 1. Open Settings.
    Expected: An element with accessibility id "Play white noise" is
    present.
 2. Tap it to start white noise playback.
    Expected: The same button now exposes accessibility id "Pause white
    noise" instead of "Play white noise".
---
