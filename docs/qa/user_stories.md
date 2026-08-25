# JW Player - User Stories

Retroactive backlog covering the app's implemented functionality.
Numbered PLAYER-001+. Grouped by epic for readability only. Frozen - Jira is
the source of truth for stories going forward, this file isn't touched by
automation. Each story's `Jira Issue ID` was backfilled after importing these
into Jira (PLAYER-NNN -> JWP-(NNN+3), a flat offset confirmed against 4
spot-checked imports spanning the full range).

## Epic: Library Browsing & Navigation

### PLAYER-001: Choose root library folder
**Jira Issue ID:** JWP-4
As a user, I want to select a root folder for my music library so the app knows where to browse from.

**Acceptance Criteria:**
- Settings screen has a "Root Folder" section with a button that opens the system folder picker.
- Selecting a folder persists it (survives app restart) and grants persistable read permission.
- The button label shows the currently selected folder's name, or "Select Folder" if none chosen.
- If no root folder is set, the main screen shows "set the root folder" instead of a file list.

### PLAYER-002: Browse folder contents
**Jira Issue ID:** JWP-5
As a user, I want to see the folders and playable audio files inside my current folder so I can find what to play.

**Acceptance Criteria:**
- Lists subfolders (alphabetical, case-insensitive) followed by playable audio files (mp3, m4a, wav; alphabetical).
- Hidden entries (names starting with ".") are excluded.
- Folders show a folder icon; files show a music-note icon.

### PLAYER-003: Navigate into a subfolder
**Jira Issue ID:** JWP-6
As a user, I want to tap a folder to open it and see its contents.

**Acceptance Criteria:**
- Tapping a folder row navigates into that folder and displays its contents.

### PLAYER-004: Hardware back button navigates up one folder level
**Jira Issue ID:** JWP-7
As a user, I want the system back button to take me up to the parent folder when I'm browsing a subfolder.

**Acceptance Criteria:**
- When the current folder is not the root folder, pressing the system back button navigates to the parent folder.

### PLAYER-005: Hardware back button backgrounds the app at the root folder
**Jira Issue ID:** JWP-8
As a user, I want the system back button to background the app once I'm back at my root folder, matching normal Android behavior.

**Acceptance Criteria:**
- When the current folder is the root folder, pressing the system back button backgrounds the app instead of navigating anywhere further.

### PLAYER-006: Pinned subfolder name header
**Jira Issue ID:** JWP-9
As a user, I want to always see which subfolder I'm in, even while scrolling its contents.

**Acceptance Criteria:**
- When browsing any folder other than root, the folder's name is shown as a fixed header at the top of the screen.
- The header does not scroll away as the folder's contents are scrolled.
- Tapping the header navigates up to the parent folder.

### PLAYER-007: Scroll position indicator arrows
**Jira Issue ID:** JWP-10
As a user, I want a visual cue when a folder's contents overflow the screen, so I know there's more to see.

**Acceptance Criteria:**
- When there are items below the visible area, a static down arrow replaces the bottom visible row.
- Scrolling to the true bottom removes the down arrow and shows the real last item.
- Once scrolled away from the top, a static up arrow replaces the top visible row.
- Scrolling back to the top removes the up arrow.

### PLAYER-008: Highlight currently playing file
**Jira Issue ID:** JWP-11
As a user, I want to see which file is currently playing while browsing its folder.

**Acceptance Criteria:**
- The file matching the currently playing track is visually highlighted (inverted colors) in the list.

### PLAYER-009: Cache visited folder listings for the browsing session
**Jira Issue ID:** JWP-12
As a user, I want folder navigation to feel fast when I revisit a folder I've already browsed.

**Acceptance Criteria:**
- Each folder's listing is read from disk once per browsing session and cached by folder location.
- Revisiting a cached folder (e.g. via the back button) does not re-read from disk.
- The cache is scoped to the main library screen; leaving to Settings and returning clears it.

### PLAYER-010: Prefetch subfolder contents one level ahead
**Jira Issue ID:** JWP-13
As a user, I want drilling into a subfolder to feel instant most of the time.

**Acceptance Criteria:**
- While viewing a folder, the contents of each of its immediate subfolders are read in the background without blocking the UI.
- If the user navigates again before a prefetch finishes, the in-flight prefetch for the old folder is cancelled.

## Epic: Library Playback

### PLAYER-011: Play a file from the library
**Jira Issue ID:** JWP-14
As a user, I want to tap a song to start playing it and queue up the rest of the songs in that folder.

**Acceptance Criteria:**
- Tapping a file begins playback and builds a queue from all playable files in that same folder (siblings), in the order shown.

### PLAYER-012: Play/pause toggle
**Jira Issue ID:** JWP-15
As a user, I want a single button to play or pause the current track.

**Acceptance Criteria:**
- Mini player shows a play/pause icon button reflecting current state and toggles playback on tap.

### PLAYER-013: Display current track title and artist
**Jira Issue ID:** JWP-16
As a user, I want to see what's currently playing.

**Acceptance Criteria:**
- Mini player shows "Artist - Title" (or just the title if there's no artist metadata) on a single line.
- Text that overflows the available width scrolls automatically (marquee).

### PLAYER-014: Display elapsed time
**Jira Issue ID:** JWP-17
As a user, I want to see how far into the track I am.

**Acceptance Criteria:**
- Elapsed time is shown as MM:SS, centered above the seek bar, and updates continuously during playback.

### PLAYER-015: Tap-to-seek on the seek bar
**Jira Issue ID:** JWP-18
As a user, I want to tap anywhere on the seek bar to jump playback to that position.

**Acceptance Criteria:**
- Tapping a point on the seek bar seeks the track to the proportional position.

### PLAYER-016: Drag-to-seek on the seek bar
**Jira Issue ID:** JWP-19
As a user, I want to drag the seek bar's thumb to scrub through the track.

**Acceptance Criteria:**
- Dragging along the seek bar previews the seek position live and commits the seek on release.

### PLAYER-017: Skip buttons (restart / previous track, next track)
**Jira Issue ID:** JWP-20
As a user, I want dedicated buttons to jump between tracks.

**Acceptance Criteria:**
- The "previous" button restarts the current track from 0:00 if more than 3 seconds have elapsed; otherwise it jumps to the previous track in the queue.
- The "next" button advances to the next track in the queue, wrapping to the first track if currently on the last.

### PLAYER-018: Seek buttons (press-and-hold fast-forward / rewind)
**Jira Issue ID:** JWP-21
As a user, I want press-and-hold buttons to quickly scrub forward or backward through a track.

**Acceptance Criteria:**
- Holding the rewind/fast-forward button seeks continuously (accelerated) in that direction while held, muting audio during the hold.
- Reaching the start while rewinding seeks to 0:00, restores volume, and resumes playback.
- Reaching the end while fast-forwarding advances to the next track (wrapping if needed) and restores volume.
- Releasing before hitting either boundary restores volume and resumes playback at the current position.

### PLAYER-019: Auto-advance to next track
**Jira Issue ID:** JWP-22
As a user, I want the next track to start automatically when the current one finishes.

**Acceptance Criteria:**
- When a track finishes playing naturally (not via a skip action), the next track in the queue plays automatically.
- If the last track in the queue finishes, playback stops.

## Epic: White Noise

### PLAYER-020: Select white noise file
**Jira Issue ID:** JWP-23
As a user, I want to choose an audio file to use as white noise.

**Acceptance Criteria:**
- Settings screen has a "White Noise" section with a button that opens the system file picker (single audio file).
- The selection persists across app restarts.

### PLAYER-021: Play/pause white noise
**Jira Issue ID:** JWP-24
As a user, I want to start or stop white noise playback from Settings.

**Acceptance Criteria:**
- A play/pause button toggles white noise playback.
- White noise loops continuously while playing.

### PLAYER-022: White noise and library playback are mutually exclusive
**Jira Issue ID:** JWP-25
As a user, I don't want white noise and my music to play at the same time.

**Acceptance Criteria:**
- Starting white noise playback replaces whatever library track was loaded; the two never play simultaneously.
- Starting library playback (tapping a file, or pressing play in the mini player) replaces white noise if it was playing.
- Pressing play in the mini player while white noise is active resumes the last library track instead of resuming white noise.

## Epic: App Shell & Layout

### PLAYER-023: Splash screen
**Jira Issue ID:** JWP-26
As a user, I want a quick branded splash on launch rather than jumping straight into the app.

**Acceptance Criteria:**
- On launch, shows "jw player" as a single line of bold, rainbow-gradient text on a black background for 1.5 seconds.
- After 1.5 seconds, proceeds automatically to the main screen (or the "set root folder" prompt if none is set).

### PLAYER-024: Dark theme
**Jira Issue ID:** JWP-27
As a user, I want the app to use a dark theme throughout.

**Acceptance Criteria:**
- All screens (library browser, mini player, settings) use a dark color scheme.
- System status bar and navigation bar are styled dark to match.

### PLAYER-025: Accessible content descriptions for screen readers
**Jira Issue ID:** JWP-28
As a low-vision user relying on a screen reader (e.g. TalkBack), I want every interactive element to announce a clear, meaningful label so I can navigate and use the app.

**Acceptance Criteria:**
- Every tappable folder row announces "folder \<name\>"; every tappable file row announces "file \<name\>"; the pinned subfolder header (which navigates up to the parent) announces "folder \<name\>" as well.
- The scroll-edge up/down arrow indicators announce "More folders or files above" / "More folders or files below" instead of being silent/decorative.
- The mini player's title/artist line announces "Now playing: \<title\>"; the elapsed time announces "Elapsed time \<MM:SS\>"; the seek bar announces "Seek bar".
- The play/pause button (mini player and the white-noise toggle in Settings) announces "Play"/"Pause" (or "Play white noise"/"Pause white noise"), reflecting current state, instead of a static "Play or pause".
- The press-and-hold seek (fast-forward/rewind) buttons are exposed to accessibility services as buttons with a description ("Seek backward"/"Seek forward"), not just an unlabeled tappable icon.
- Settings screen's root-folder and white-noise-file buttons announce "folder \<name\>" / "file \<name\>" when a selection exists, or a descriptive prompt ("Select root folder" / "Select white noise file") when none is set.
- None of the above changes what is visually displayed on screen - only the label exposed to accessibility services.

**Known limitation:** the press-and-hold seek buttons are built on a raw gesture detector (not a standard clickable control), so while they are now correctly labeled and reachable by a screen reader, a screen reader's simulated "double-tap to activate" may not trigger the same hold-to-seek gesture as a real press-and-hold. Flagged here rather than silently claimed as fully solved.
