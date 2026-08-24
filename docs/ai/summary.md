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
- Control vs. observability, strictly split: control (what makes things happen)
  goes through GitHub-native mechanisms only (`repository_dispatch`/
  `workflow_dispatch`); observability (what tells humans what's happening) is
  Slack, via Incoming Webhook only - no Socket Mode, no bot listening for events
- AWS decision: automation runs on AWS Device Farm; build the pipeline with a stub
  result first, swap in the real Device Farm call last (cost control)
- Jira Automation (free tier) calls GitHub's `repository_dispatch` API directly on
  issue status change - reuses the same mechanism as every other cross-repo trigger
- Naming locked (naming/organization stated explicitly as a critical personal
  standard): repos `jw_player`/`jw_player_automation` (both public), workspace
  "JW Company," channel "JW Player CI," one bot ("CI Bot") with text-tag
  specificity (`[Linter]`, `[AI Code Review]`, etc.) instead of per-bot identities
- Pipeline order: story -> AI writes the app code (PR) -> lint -> AI review/fix ->
  AI writes test cases from story+diff -> AI writes+commits Appium scripts (no
  review gate there, deliberately) -> AWS run -> Slack at every step
- No human code review anywhere in the pipeline (app code, automation scripts, or
  generated test cases) - PRs auto-merge once automated checks pass; explicit
  design choice for a maximal-automation portfolio piece
- Branch protection: not used
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
skeleton, <https://github.com/jwallis/jw_player_automation>). No branch protection
on either repo.

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

## Phase 1 — complete, verified end-to-end

Planned in detail (see the plan file), built, debugged against a real Jira story,
and now working. Took a handful of real failures to get there - each one informative
about how Jira Automation and GitHub Actions actually behave, not just how the docs
describe them.

**Fixed `CLAUDE.md` first.** It was badly stale - still described the app as the
untouched Android Studio "Empty Activity" template with a `Greeting("Android")`
placeholder, and claimed `minSdk 34` (actually 26). Since Phase 1's design leans on
`CLAUDE.md` as the way headless Claude Code runs learn the codebase's conventions,
this had to be accurate before any automated run could trust it. Rewrote "Project
state"/"Architecture" to reflect the real app (splash screen, nav host, folder
browser backed by SAF, `PlaybackViewModel`/`StateFlow` as the sole playback state
owner, white noise sharing the same `ExoPlayer` instance, thin `SharedPreferences`
repository, no DI framework) and added a "Conventions to follow" section: SAF-only
file access, `PlaybackViewModel`/`StateFlow` as the only playback-state source,
accessibility semantics required, no new DI framework, write a unit test for any
new non-trivial logic, reuse existing patterns rather than inventing a second way to
do the same thing, and prefer clarity over cleverness - a modest set of "guide a
junior developer" rules, added mainly for portfolio value on an app this small.

**Built `story-implementation.yml`.** Replaces the Phase 0 stub
(`dispatch-test.yml`, deleted). Validates the dispatch payload, creates a per-story
branch, runs `anthropics/claude-code-action` (pinned `@v1.0.201`, model pinned to
Sonnet via `claude_args: --model sonnet` - it defaulted to Opus otherwise, way more
than this needs) with a prompt built from the Jira issue's key/summary/description/
url (description explicitly delimited as untrusted data, not instructions - a
prompt-injection mitigation), independently re-runs `assembleDebug` *and*
`testDebugUnitTest` rather than trusting the agent's self-report, and only then
pushes and opens a PR. Claude is scoped via `--allowedTools` to `git add/commit/
status/diff/log` only - notably **not** `push`, and no `gh` commands at all - so
"don't push, don't open a PR" is enforced by tool permissions, not just prompt
instruction. Three distinct Slack outcomes tagged `[Story Implementation]`: success
(PR link), no changes made (agent declined - distinct from failure), and failure
(covers pipeline errors *and* a post-commit build/test failure - resolved during
planning to fail loudly rather than silently discard broken code, since there's no
review gate before Phase 3). No auto-merge - Phase 1 stops at "PR opened."
`show_full_output`/`display_report` enabled after the first run turned out to log
almost nothing to stdout (Claude's execution output went to a temp JSON file
instead).

**Design question worth remembering:** Claude commits its own changes (has real
`git commit` access) rather than the workflow blindly committing whatever's left in
the working tree afterward. Deliberate - the presence of a commit *is* Claude's own
signal that it considers the work finished and correct ("if you can't implement it,
make no commits"). A workflow-owns-the-commit design is also valid (GitHub Actions
step outputs can carry multi-line text like a commit message/verdict between
steps), but it adds a data-passing layer that has to agree with a second git action,
instead of the source of truth and the pass/fail signal being the same thing.

**Jira Flow payload expanded, then debugged.** Added `description`
(`{{issue.description}}`) to the "Ready for AI" Flow's request body. First real
dispatch attempt failed at Jira's end with GitHub returning `400: Problems parsing
JSON` - the description contained rich-text formatting (numbered list), and Jira's
smart-value substitution splices raw text directly into the JSON template with no
automatic escaping, so a real newline character landed inside a JSON string
literal. Fix: Atlassian's `asJsonString` smart-value function
(`{{issue.description.asJsonString}}`), which both escapes special characters *and*
supplies its own surrounding quotes - meaning the manual `"..."` around each
smart-value in the template had to be removed for any field using it. Applied to
all four payload fields for robustness. Then hit a second, narrower bug:
`{{issue.url.asJsonString}}` rendered as completely empty (though `{{issue.url}}` is
a valid, documented smart-value and worked fine unescaped) - reverted just that one
field to plain manual quoting (`"url": "{{issue.url}}"`) since a Jira URL will never
contain JSON-breaking characters anyway. One more human error along the way: after
removing `asJsonString` from `url`, the manually-typed quote characters around the
placeholder were forgotten entirely, producing invalid JSON again - added back by
hand.

**First real run: Claude succeeded, PR creation failed.** First live dispatch had
Claude Code genuinely implement the test story (added a live clock to the splash
screen, including a generated unit test) and pass its own build check - but
`gh pr create` failed with `GitHub Actions is not permitted to create or approve
pull requests`. Root cause: a repo setting (Settings -> Actions -> General ->
"Allow GitHub Actions to create and approve pull requests"), off by default,
independent of the workflow's own `permissions: pull-requests: write` block. Fixed
by enabling it.

**Second bug found in the same failed run's log:** the branch name came out as
`ai/story/JWP-2-` (trailing dash). Cause: `echo "$raw" | tr -c 'A-Za-z0-9_-' '-'` -
`echo` appends a trailing newline, and `tr` converted that newline (not in the
allowed character set) into a dash too. Fixed with `printf '%s'` instead of `echo`.
While fixing it, also extended branch names to include a slugified story summary
(lowercased, non-alphanumeric collapsed to dashes, truncated to 50 chars) for
readability - e.g. `ai/story/JWP-2-test-story-01-add-the-time-to-the-splash-screen`
instead of just the bare issue key.

**Second full run: complete success.** Every step passed, including the new
`testDebugUnitTest` gate (added to the independent re-verification step alongside
`assembleDebug`, matching the same "hard fail on broken build" logic already in
place). PR #1 opened cleanly:
<https://github.com/jwallis/jw_player/pull/1>. The success-path Slack message fired
correctly; no-changes/failure paths correctly skipped.

**New secret: `ANTHROPIC_API_KEY`.** A dedicated Anthropic API key (not the
personal one), so this pipeline's spend is separately trackable/revocable. Added via
`gh secret set` to both `jw_player` (needed now) and `jw_player_automation` (needed
later, Phase 6) in one pass, then the raw value was deliberately discarded rather
than saved in a password manager - since it's already stored everywhere it'll ever
be needed, keeping zero copies outside GitHub keeps it a cleaner CI-only credential.

**Also learned along the way:** GitHub Actions runners are fully ephemeral - every
run currently reinstalls Gradle, every Kotlin/AGP/Compose dependency, and Claude
Code's own runtime (Node/Bun) from scratch, since nothing is cached yet. Flagged in
the plan file as a known follow-up (`actions/cache` for the Gradle cache), not
blocking, worth doing once more phases exist to amortize the fix across.

## Phase 2 — complete

`.github/workflows/lint.yml`, triggered by `pull_request` into `main` (any PR, not
scoped to AI-authored branches). Runs `./gradlew ktlintFormat`
(`org.jlleitschuh.gradle.ktlint`, default ruleset), commits any autofix as `CI Bot`,
pushes it back to the PR's head branch, and hard-fails if violations remain that
ktlint couldn't fix itself. Posts `[Linter]` to Slack on success (`clean` vs.
`autofixed`) and failure. No LLM involvement anywhere in this step - purely a static
formatter plus scripted git/Slack calls.

One real wrinkle: ktlint's default naming rule doesn't know about Compose and
flagged every `@Composable` function (PascalCase, e.g. `MainScreen`) as a violation,
since standard Kotlin style expects camelCase. Fixed with a repo-root
`.editorconfig` entry (`ktlint_function_naming_ignore_when_annotated_with =
Composable`) - the standard, documented way ktlint projects exempt Compose UI
functions from that one rule.

Since the existing codebase had never been linted, `ktlintFormat` was also run once
locally and the resulting formatting fixes (plus two boilerplate `import
org.junit.Assert.*` wildcard imports ktlint couldn't auto-fix, made explicit by
hand) were committed straight to `main`, so the first real PR to hit this workflow
only shows its actual diff.

## Status

Phase 0, Phase 1, and Phase 2 are complete and verified end-to-end - a real Jira
story (JWP-2) went through Phases 0-1 and produced a real, working PR
(<https://github.com/jwallis/jw_player/pull/1>); Phase 2's lint workflow builds,
tests, and `ktlintCheck` all pass against the newly-formatted codebase. Next up is
Phase 3 (AI code review), which gets its own focused plan when work on it starts.
