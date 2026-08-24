# JW Player — Session Summary (resume/interview prep)

High-level memory hooks, not a full changelog. Organized by topic.

## App UX polish (jw player)

- Splash screen simplified: single-line branding, cut from 4s to 1.5s
- Back-button semantics fixed to match Android conventions: subfolder -> go up;
  root folder -> background the app
- Icon iterated through two full concepts (rainbow gradient -> black/white-outline)
  using a generated-icon pipeline
- Folder browser: added scroll-position affordances (edge arrows that
  appear/disappear based on scroll state) and a pinned, non-scrolling header for
  the current folder
- Layout pass: reclaimed wasted screen space, pinned mini-player to natural size
  above the nav bar
- Performance: added a session-scoped cache and one-level-ahead background
  prefetch for folder navigation
- Doubled the hold-to-seek speed (a tunable constant, quick win)
- Renamed the whole project (folder + Android package) from `mp3player` /
  `com.joshuawallis.mp3player` to `jwplayer` / `com.joshuawallis.jwplayer`

## QA artifacts + accessibility

- Wrote 26 user stories + 42 test cases retroactively for the whole app -
  deliberately fine-grained (e.g. kept "skip buttons" and "seek buttons" as
  separate stories per explicit direction). Live in `docs/qa/` as Markdown.
- Added real `contentDescription` accessibility labels app-wide (not just Appium
  prep - actual screen-reader support)
  - Learned/applied the real distinction: `contentDescription` (TalkBack-facing) vs.
    `testTag` + `testTagsAsResourceId` (Appium-facing resource-id) - different Compose
    APIs, don't conflate them
  - Found and fixed a real, pre-existing a11y bug: hold-to-seek buttons had zero
    accessibility semantics (confirmed via a live `uiautomator` dump showing
    `clickable="false"`); also caught a Kotlin scoping self-reference bug while
    wiring the fix

## Tooling built

- Two Claude Code skills (`show-source-app`, `show-source-app-full`) to dump/
  pretty-print/trim the live Android accessibility tree for locator inspection
- Key lesson: Claude Code skills index at session start - a skill created
  mid-session needs a fresh session to appear as a slash command

## CI/automation system design (the big one)

- Framed as two repos, not three: `jw_player` and `jw_player_automation` - the "CI
  system" isn't a repo, it's GitHub Actions config + Slack config + AWS config +
  a Jira Automation rule
- Compared Slack trigger mechanisms (Events API/webhook vs. Socket Mode vs.
  polling) before choosing
  - Self-caught architecture flaw: initially mixed Slack-as-trigger with
    Slack-as-notification between automated systems - recognized as non-standard
    ("chat app as control plane"), corrected to a strict split
  - Locked principle: control = GitHub-native (`repository_dispatch`/
    `workflow_dispatch`) only; observability = Slack (Incoming Webhook only) only -
    this dropped Socket Mode from the design entirely
- AWS decision: automation runs on AWS Device Farm only, never the laptop; build
  the pipeline with a stub result first, swap in the real Device Farm call last
  (cost control)
- Jira-triggered pipeline researched and adopted from the start (not a bonus
  add-on): Jira Automation (free tier) can call GitHub's `repository_dispatch` API
  directly on issue status change - no new public endpoint needed, reuses the
  same mechanism as everything else
- Naming locked (naming/organization stated explicitly as a critical personal
  standard): repos `jw_player`/`jw_player_automation` (both public), workspace
  "JW Company," channel "JW Player CI," one bot ("CI Bot") with text-tag
  specificity (`[Linter]`, `[AI Code Review]`, etc.) instead of per-bot identities
- Corrected pipeline order (caught a missing step on a self-review): story -> AI
  writes the app code (PR) -> lint -> AI review/fix -> AI writes test cases from
  story+diff -> AI writes+commits Appium scripts (no review gate there,
  deliberately) -> AWS run -> Slack at every step
- No human code review anywhere in the pipeline (app code, automation scripts, or
  generated test cases) - PRs auto-merge once automated checks pass; explicit
  design choice for a maximal-automation portfolio piece
- Branch protection: designed in originally, then explicitly dropped later once
  Phase 0 execution started - decided it wasn't worth caring about for these repos
- Traceability: test cases carry a "Jira Issue ID" field back to the originating
  story; generated Appium test function names are prefixed with their test case ID
  (e.g. `PLAYER_TC-001_Selecting_a_root_folder_persists_it()`)
- Every pipeline step posts to Slack on both success and failure - no quiet
  successes; test-case-generation and script-authoring steps post actual content
  (titles/automatable status, function names), not just a generic "done"
- Automation framework architecture spec'd up front (config layer, Appium driver
  wrapper, page-object model layer, service/business layer, test-script layer,
  plus `AppUtil`/`DriverUtil` utilities) - drawing on 20 years of building these from
  scratch
- Stated scope philosophy: maximal automation for portfolio value, not realistic
  for a real company; failures are considered valuable to demonstrate, not to
  avoid
- Confirmed no paid tools needed beyond Claude API (have) and AWS Device Farm
  (pending) - public repos make GitHub free-tier Actions unlimited

## Phase 0 — execution (complete)

Everything below was actually built and verified, not just designed.

**Local project cleanup.** Split all the uncommitted work sitting in the repo into
3 clean commits: the mp3player->jwplayer rename, the accessibility pass, and the QA
docs. Verified `./gradlew assembleDebug` still succeeds after the split.

**Toolchain fixes.** `node`/`npm` were broken (Homebrew `icu4c` version mismatch) -
`brew reinstall node` fixed it, pulled in v26.7.0. `gh` CLI wasn't installed at
all - installed via Homebrew and authed.

**GitHub repos.** Created and pushed `jw_player` (public,
<https://github.com/jwallis/jw_player>) and `jw_player_automation` (public, empty
skeleton, <https://github.com/jwallis/jw_player_automation>). Branch protection
discussed, then dropped entirely - not worth caring about for these repos.

**Slack.** New workspace "JW Company," channel "JW Player CI." Slack App "CI Bot,"
Incoming Webhooks only (no bot scopes, no Socket Mode - matches the
observability-only design). Webhook URL stored as GitHub secret
`SLACK_JW_PLAYER_CI_WEBHOOK_URL` in both repos. Tested with a manual `curl` -
confirmed messages land in the channel.

**Jira.** Project `JWP` ("JW Player") - confirmed a real Jira Software project
despite Atlassian's "Space"/"Work Type" terminology rebrand briefly causing
confusion (pure rename, not a feature merge - Jira projects are now labeled
"Spaces" in the UI). Added workflow status "Ready for AI," reachable from any other
status. Built a Flow (Jira's renamed "Automation rule"): trigger = issue
transitions to "Ready for AI" -> sends a `POST` to GitHub's `repository_dispatch`
endpoint for `jw_player`, with `event_type: "story-ready-for-ai"` and the issue
key/summary/URL as payload. Auth: a classic GitHub PAT (`repo` scope), pasted
directly into the Flow's request header - no Jira secrets vault was available on
this plan, so it's plaintext there (documented, not hidden). **Tested
end-to-end**: moved a work item to "Ready for AI" -> confirmed a real GitHub
Actions run fired in `jw_player`, using a small stub workflow
(`.github/workflows/dispatch-test.yml`) that just echoes the payload - proves the
whole trigger chain works before any real implementation logic exists.

**AWS.** IAM user `jw-player-ci` (not root), no console access, no permissions
attached yet - permission scoping deferred to Phase 7 when the real Device Farm
integration gets built. Considered and declined AWS's "Agent Toolkit" (MCP-based
direct AWS access for the AI agent) in favor of manual console setup - narrower
blast radius, matches the design principle that AWS calls happen from GitHub
Actions runners later, not from a local AI session. Access keys configured locally
as CLI profile `jw-player-ci`, region `us-east-2`. Verified via
`aws sts get-caller-identity`.

**Documentation.** New file `docs/config/config.md` in `jw_player` - tracks
*where* every secret/credential actually lives (Slack webhook, GitHub PAT, AWS
keys), without storing the values themselves.

## Phase 1 — in progress

Planned in detail (see the plan file) and partway through execution.

**Fixed `CLAUDE.md` first.** It was badly stale - still described the app as the
untouched Android Studio "Empty Activity" template with a `Greeting("Android")`
placeholder, and claimed `minSdk 34` (actually 26). Since Phase 1's design leans on
`CLAUDE.md` as the way headless Claude Code runs learn the codebase's conventions,
this had to be accurate before any automated run could trust it. Rewrote "Project
state"/"Architecture" to reflect the real app (splash screen, nav host, folder
browser backed by SAF, `PlaybackViewModel`/`StateFlow` as the sole playback state
owner, white noise sharing the same `ExoPlayer` instance, thin `SharedPreferences`
repository, no DI framework) and added a new "Conventions to follow" section stating
those patterns explicitly for an unsupervised AI agent to preserve.

**Replaced the stub workflow.** `.github/workflows/dispatch-test.yml` (the Phase 0
echo-only stub) is gone; `.github/workflows/story-implementation.yml` is the real
thing - validates the dispatch payload, creates a per-story branch
(`ai/story/{ISSUE_KEY}`), runs `anthropics/claude-code-action` with a prompt built
from the Jira issue's key/summary/description/url (description explicitly delimited
as untrusted data, not instructions - a prompt-injection mitigation), independently
re-verifies the build (`./gradlew assembleDebug`) rather than trusting the agent's
self-report, and only then pushes and opens a PR. Three distinct Slack outcomes,
tagged `[Story Implementation]`: success (PR link), no changes made (agent declined -
distinct from failure), and failure (covers pipeline errors *and* a post-commit
build failure - resolved during planning to fail loudly rather than silently
discard broken code, since there's no review gate before Phase 3). No auto-merge -
Phase 1 stops at "PR opened."

**Jira Flow payload expanded.** Added a `description` field
(`{{issue.description}}`) to the "Ready for AI" Flow's request body, alongside the
existing `issue_key`/`summary`/`url` - gives the workflow real story content to
implement, not just a one-line summary.

**New secret: `ANTHROPIC_API_KEY`.** A dedicated Anthropic API key (not the
personal one), so this pipeline's spend is separately trackable/revocable. Added via
`gh secret set` to both `jw_player` (needed now) and `jw_player_automation` (needed
later, Phase 6) in one pass, then the raw value was deliberately discarded rather
than saved in a password manager - since it's already stored everywhere it'll ever
be needed, keeping zero copies outside GitHub keeps it a cleaner CI-only credential.

**Not yet done:** pushing this all end-to-end with a synthetic test dispatch
(`gh api repos/jwallis/jw_player/dispatches ...`) to verify the whole chain works,
including the ADF/description-escaping concern flagged during planning (Jira
descriptions are rich text internally - need to confirm the smart-value rendering
doesn't mangle a real multi-line/formatted description before trusting this for
actual stories).

## Status

Phase 0 is complete and verified (see above). Phase 1 is implemented and pushed to
`main`; end-to-end verification (synthetic dispatch test) is the next step before
trusting it with a real Jira story.
