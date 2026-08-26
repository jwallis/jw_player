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

## Phase 2 follow-ups

**Fixed the recurring "Node.js 20 is deprecated" warning** on every Actions run -
`actions/checkout@v4` internally targets Node 20, and GitHub-hosted runners now
force it onto Node 24. Bumped to `actions/checkout@v7` (Node-24-native) in both
`story-implementation.yml` and `lint.yml`.

**Added real unit tests to prove `testDebugUnitTest` actually asserts things**,
not just compiles: `FormattingTest.kt` covers `MiniPlayer.formatTime` (bumped
from `private` to `internal` for testability) and
`DirectoryLister.titleFromFileName`. Verified via the JUnit XML report
(`tests="3" failures="0" errors="0"`), not just Gradle's "BUILD SUCCESSFUL" -
worth remembering that a passing build can still mean zero tests ran.

**Debugging note, not a code change:** the reused JWP-2 test PR branch didn't
trigger `lint.yml` on push, because `pull_request`-triggered workflows read
their YAML from the PR's *head* branch, not `main` - a workflow added to `main`
doesn't apply to a PR branch that predates it until that branch is updated.
Fixed for that one branch by merging `main` in (a real 3-block conflict in
`MainActivity.kt`, resolved by keeping the branch's actual feature content and
pulling in `main`'s unrelated accessibility imports).

## Phase 3 — complete, verified end-to-end

`.github/workflows/ai-review.yml`. Design detail lives in the plan file; the
one decision worth calling out here is the **verdict signal**. Phase 1 uses "did
Claude commit anything" as an implicit pass/fail signal, which works with two
outcomes. Phase 3 needs three - approve, fix-and-loop, or blocked (found a real
problem but can't safely fix it) - and collapsing "blocked" into "no commit"
would silently auto-merge code Claude gave up on, the one failure mode this
pipeline can't afford with no human backstop. So Claude writes an explicit
verdict (`APPROVE`/`FIXED`/`BLOCKED`, plus a reason) to a git-ignored scratch
file that a workflow step reads afterward.

Triggered by `workflow_run` on `Lint`'s completion (not a second `pull_request`
trigger) specifically so review never races lint's own autofix-and-push. A
staleness guard re-checks the PR's actual head SHA before doing anything, since
a fast autofix-then-review sequence could otherwise act on code that's already
been superseded. An iteration cap (1 fix commit) stops an unbounded fix/re-review
loop and hands off to a human via Slack instead. On approval, merges with a real
merge commit, not squash - explicit user preference, applies to every future
auto-merge in this pipeline, not just this one.

**Live testing found a hard blocker, not a bug in the workflow logic:** GitHub
rejects `gh pr review --approve` when the reviewer and the PR's author resolve
to the same actor - and every workflow so far authenticated with the shared
`github.token` (`github-actions[bot]`), so Phase 3's first real run failed with
`Can not approve your own pull request`. Confirmed live that this isn't a
branch-protection setting (this repo has none configured, and it still failed)
- it's a platform-level rule on the review-submission API itself.

**Fix: two dedicated GitHub Apps**, one per side of the review boundary -
`jw-company-developer-bot` (Phase 1 implementation, Phase 2 lint autofixes) and
`jw-company-reviewer-bot` (Phase 3 review/approve/merge) - so the PR's author
and its reviewer are genuinely different accounts, visible as such in the PR
history, not just different in workflow YAML. Each workflow mints a short-lived
installation token via `actions/create-github-app-token`, fed the App's Client
ID + private key (GitHub's current-recommended identifier over the numeric App
ID - see `docs/config/config.md`), and uses it everywhere `github.token`
previously touched checkout/push/`gh` commands. Commit-author identity was
updated to match too, using GitHub's documented
`<bot-user-id>+<slug>[bot]@users.noreply.github.com` format (the bot user's own
ID, looked up via `gh api users/<slug>[bot]`, not the App ID) - otherwise
commits would still show the old shared identity even though a different token
pushed them. `ai-review.yml` was also missing a git identity step entirely
(never needed one before, since it had never actually committed anything) -
would have failed the first time the FIXED path tried to commit.

**A second, real design bug found via live testing, not the auth one:** the
iteration cap originally counted total commits ahead of `main`, not actual
review cycles. The reused JWP-2 branch had 5 commits ahead of main purely from
prior phase testing (merges, implementation) and tripped the cap before Claude
ever ran. Fixed by having Claude tag fix commits with a literal
`[ai-review-fix] ` message marker and counting only those.

**Verified live, in order:** trigger fires correctly off `Lint`'s completion;
staleness guard and (fixed) iteration cap both fire at the right moments; both
bots mint tokens and act under their own identities; the FIXED path ran for
real end-to-end - Claude found something to fix, the independent
`assembleDebug`/`testDebugUnitTest` re-check passed, reviewer-bot pushed the
fix commit, which correctly re-triggered `lint.yml` then `ai-review.yml` again,
which correctly hit the iteration cap on that second pass and alerted Slack
instead of looping forever. **Not yet observed live:** an `APPROVE` verdict
actually reaching `gh pr review --approve` + `gh pr merge` - JWP-2 exhausted
its one allowed fix cycle before producing a clean approval. The self-approval
bug that motivated the two-bot design is structurally fixed (reviewer-bot is a
genuinely different actor now), but the merge step itself will get its first
live exercise the next time a PR needs no fixes.

## Phase 3 — first real APPROVE + merge (JWP-3)

A real Jira story (JWP-3: add `testTag`/`testTagsAsResourceId` locators for
Appium, separate from `contentDescription`) went through the whole pipeline
for real, surfacing three more live bugs before finally reaching a clean
merge:

- **`git -C` broke the tool allowlist.** Claude's implementation run actually
  wrote the correct code, then ran `git -C /home/runner/.../jw_player diff
  --stat` - the `-C` prefix meant the command no longer started with `git
  diff`, so it didn't match the `Bash(git diff:*)` allowlist pattern and got
  denied as "requires approval" (impossible non-interactively). The whole run
  ended without ever reaching `git commit` - real work, lost to a permission
  string mismatch, not a decision by Claude. Fixed by telling Claude
  explicitly that every git command must start with the bare subcommand - no
  `-C`, `-c`, `--no-pager`, `cd`, or piping - and by setting `core.pager cat`
  so there's no reason to reach for `--no-pager` in the first place. Not a
  hard guarantee (the mutating commands stay tightly scoped on purpose, so
  the allowlist can't just be widened) - but the failure mode is safe either
  way: no commit means "no changes made," not a broken PR.
- **`claude-code-action` refuses bot-initiated workflows by default.** Once
  past the git bug, review failed instantly: `"Workflow initiated by
  non-human actor: jw-company-developer-bot ... Add bot to allowed_bots
  list."` Fixed with `allowed_bots: "jw-company-developer-bot[bot],
  jw-company-reviewer-bot[bot]"` - explicitly, not `'*'`, since this repo is
  public and `'*'` would let any external App invoke the action too.
- **`create-github-app-token`'s `app-id` input is deprecated** in favor of
  `client-id` (same value, new input name) - renamed across all three
  workflows while in there.

With those fixed, JWP-3 went all the way through: developer-bot implemented
it, lint passed clean, reviewer-bot reviewed and wrote `APPROVE`, and
**merged for real** - `mergedBy: app/jw-company-reviewer-bot`, a genuine merge
commit. First full, unassisted proof of the entire Phase 1-3 pipeline. Cap
raised from 1 to 3 fix commits afterward, now that the mechanism itself was
proven live.

## Phase 4 retired, `test_cases.md` moved to `jw_player_automation`

Reconsidered mid-build: having QA test-case authoring live inside the app
repo was a smell, especially once automation script generation was always
going to happen in `jw_player_automation` anyway. Moved `docs/qa/
test_cases.md` there (`user_stories.md` stays in `jw_player` - explicitly
frozen retroactive backlog, not part of ongoing automation). The standalone
`test-case-generation.yml` workflow (built, never run against a real merge)
was deleted outright - Phase 6 now covers writing the test case *and* its
automation script together, in one dispatch, rather than two separate
cross-repo hops.

## Phase 5 — `jw_player_automation` framework, complete

Hand-built layered Python framework: config (`config.yaml` vs.
`environments/` - only the Appium server URL, app package, and app activity
are genuinely environment-specific), a driver wrapper owning all raw
interaction (pages just hold a reference and delegate - never call
`tap()`/`find_by()` themselves), page objects, services (user-action-named
methods like `play_song`/`restart_song`, `validate_*` assertions - never
"click"/"set" in a name), `exceptions`/`utils` each in their own directory.
Rules captured in `docs/standards/standards.md` as the user specified them,
not invented.

**The locator strategy took real live debugging to get right, twice.**
Decided early to locate by `testTag`/resource-id (Compose's
`testTagsAsResourceId`), never `contentDescription` - deliberately, since
content-desc is a translatable string and would break the moment the app
supports more than one language. First pass tagged the outer clickable
`Row`/`Button` for each element. Dumping the *actual* live accessibility tree
(not just reading the Kotlin) showed the problem: Compose puts the tag on the
container and the real displayed text on a separate child node with no
resource-id of its own - `find_element(resource-id="root_folder_button")
.text` returned `""`, the real value ("Music") was two nodes down. Tried
`semantics(mergeDescendants = true)` next, since that fixed an analogous case
elsewhere in the same dump (`seek_backward_button`) - turned out that one
only worked because its sibling was `Icon(contentDescription = null)`, which
never gets a semantics node in the first place, not because
`mergeDescendants` generally collapses real `Text` content. Confirmed live
that it does not (dumped again, same split-node problem, unchanged). Real
fix: move `testTag` onto the inner `Text` itself, nothing on the outer
container (tapping the labeled child still lands within the parent's
clickable bounds). Toggle-state buttons (play/pause) went further - two
distinct static tags per state instead of one tag plus a content-desc read,
so state-reading never touches translatable text at all. Every fix verified
against a real emulator dump, not just re-read Kotlin.

**CI** (`.github/workflows/ci.yml`, this repo's first workflow): `pytest`
(5 framework-wiring tests using a mocked driver, no device needed), `mypy`
(caught a real type mismatch on its first run - `find_by()`'s return type
didn't match what `WebDriverWait.until()` actually proves), and a stubbed
`appium-run` job that echoes a fake AWS Device Farm result and posts
`[Appium] passed (stub)` to Slack.

## Jira: 25 stories imported, IDs backfilled

Generated a CSV from the 25 retroactive `user_stories.md` entries (Summary/
Description/Issue Type/Priority/Status columns, priorities judged by
core-flow vs. polish) for manual Jira import - deliberately not committed,
a one-time import artifact. Import landed as a flat `+3` offset
(`PLAYER-NNN -> JWP-(NNN+3)`) - confirmed against 4 spot-checked mappings
spanning the full range before trusting it for the rest. Backfilled real
`Jira Issue ID` values into both `user_stories.md` and `test_cases.md`
(replacing `TBD`) using that offset.

## Lesson: comments/docs should say what's true now, not how it got there

Caught adding narrative like "backfilled after importing X, using offset Y
confirmed via Z spot-checks" into `test_cases.md`/`user_stories.md`'s intro
comments - exactly the kind of change-log-shaped sentence that belongs in a
commit message, not a file that has to stay readable long after the change
is old news. Added as an explicit convention to `jwplayer`'s `CLAUDE.md`, and
mirrored into the global `~/.claude/CLAUDE.md` so it applies to every future
project, not just this one.

## Phase 6 prep

`jw-company-developer-bot`'s GitHub App is now installed on both repos (was
`jw_player`-only) - `jw_player_automation` needs it to write directly there
for Phase 6. `jw_player_automation` also got its own `CLAUDE.md` (didn't
exist before) - every AI-agent prompt in this project opens with "read
CLAUDE.md first," so a repo an AI agent writes in needs one; without it,
Phase 6's generation step would have had nothing telling it about the unit-
test convention, `mypy`, or that `standards.md` even exists.

## Phase 6: built and exercised live for the first time (JWP-29)

Two new workflows: `jw_player`'s `notify-automation.yml` (fires
`repository_dispatch` at `jw_player_automation` when a merged `ai/story/*`
PR closes, recovering Jira context from the PR body) and
`jw_player_automation`'s `generate-test-automation.yml` (receives the
dispatch, runs developer-bot/Claude to append a `test_cases.md` entry and
an Appium scenario test built on the service layer, extending
pages/services when needed, verifies independently with `pytest` + `mypy`,
pushes straight to `main` - no PR gate on this repo).

Ran a real story (JWP-29: "Update empty-library placeholder text to be
friendlier," AC: change the empty-library text to read exactly `Hey man
choose a root folder!`) through the full six-phase chain for the first
time: Story Implementation -> Lint -> AI Code Review -> merge -> Notify
Automation -> Generate Test Automation.

**Result: the story failed its own acceptance criteria, and the pipeline
merged it anyway.** AI Code Review's first pass didn't approve the literal
text - it rewrote it, unprompted, from `Hey man choose a root folder!` to
`No music yet — choose a folder to get started!`, citing "unprofessional/
gendered slang" as its reason, committed that rewrite as a `[ai-review-fix]`
commit, and re-triggered itself. The second pass approved and merged *that*
version instead. Nothing in the pipeline compares shipped behavior against
the actual AC - the independent re-verification step only checks that the
code compiles and passes tests, not that it does what the story asked.
Phase 6 then faithfully generated a test case and automation script for the
reviewer's substituted text, and even noted in its own commit message that
this text didn't match the literal AC - correctly diagnosing the mismatch
it inherited, but with no mechanism to do anything about it. No human was
in the loop at any point to catch this - exactly the risk the "no human
code review anywhere" design accepts in exchange for full automation. This
is a real gap, not a curiosity: a reviewer with unrestricted fix authority
and no check against the original requirement can silently replace what
was asked for with what it thinks should have been asked for.

**Three real bugs found and fixed live, all pushed to `main` in both repos
before the run above finished successfully:**

1. **Slack `curl` steps broke on embedded quotes in AI-written free text.**
   `ai-review.yml`'s "fix pushed"/"blocked" messages and
   `generate-test-automation.yml`'s case-summary/blocked messages spliced a
   model-generated reason or case title directly into a shell-quoted JSON
   string; the FIXED-verdict reason above happened to contain literal
   double quotes (`"Hey man..."`), which broke the shell quoting and
   silently killed the Slack notification (curl exit 3, URL malformed) even
   though the underlying commit/push had already succeeded. Fixed by
   building every such payload with `jq -n --arg` from `env:`-passed values
   instead of splicing into the JSON string directly - the same
   env-not-splice pattern already used for shell commands, extended to JSON
   payloads.
2. **Claude stalled running `./gradlew` as a background task.** During the
   same review run, Claude used the Bash tool's background-execution mode
   to kick off `ktlintCheck`/`assembleDebug`/`testDebugUnitTest`, then spent
   its entire turn polling/sleeping for a completion notification that this
   CI environment (`claude-code-action`, not an interactive session) never
   delivers - it never got a result back, never wrote `.ai_review_verdict`,
   and the step failed with "Claude did not write .ai_review_verdict" after
   burning several minutes doing nothing. Fixed by adding an explicit
   prompt instruction to run `./gradlew`/`pytest`/`mypy` synchronously in
   the foreground, applied to all three workflows that invoke build/test
   commands (`story-implementation.yml`, `ai-review.yml`,
   `generate-test-automation.yml`) since all three were equally exposed.
3. **`generate-test-automation.yml`'s `claude-code-action` step refused to
   run.** Its `repository_dispatch` trigger comes from `notify-automation.
   yml` firing with developer-bot's own token, so `github.actor` for the
   run is the bot itself - `claude-code-action` refuses bot-initiated
   workflows by default, the same wrinkle already hit and fixed for
   `ai-review.yml` back in Phase 3. Fixed with the same `allowed_bots`
   input.

**Also learned: `repository_dispatch` reruns don't re-resolve the workflow
file from `main`.** After pushing the `allowed_bots` fix, `gh run rerun`
replayed the exact same (pre-fix) failure - unlike `workflow_run`, which
does pick up a `main`-branch fix on rerun (confirmed separately in the same
session, re-running the stalled AI Code Review job). Worked around it by
firing a brand-new `repository_dispatch` by hand (payload reconstructed
from the merged PR's own data), which does resolve against current `main`.

## Judgment calls and direction that were the user's, not the AI's

- Insisted `jw_player_automation` own the whole QA layer (test cases and
  automation together) as a single repo, rather than splitting it further
  or leaving `test_cases.md` in `jw_player` - caught "test cases in the app
  repo is a weird smell" directly.
- Set a standing rule for the life of the project: never invent a name
  (repo, Slack app/bot/channel, workflow, AWS resource, Jira key) without
  asking first.
- Designed the automation framework's actual architecture (config/driver/
  pages/services/exceptions/utils/tests layering) from 20 years of building
  these at other companies, then gave explicit naming rules (`get_`/
  `click_`/`open_`/`set_` page-method prefixes, user-action-named service
  methods, `validate_*` for every assertion, exceptions/utils each in their
  own directory) and two design concepts (the driver wrapper owns raw
  interaction, not `BasePage`; config vs. environment split) - then caught
  and fixed a real edge case in his own split (`app_package`/`app_activity`
  can vary by build variant, moved to the environment file).
- Called "we will not be using contentDesc for hooks" outright, correctly
  identifying it as unsafe once the app supports more than one language -
  this one call drove three real iterations (content-desc read ->
  `mergeDescendants` -> tag-on-leaf-node) before landing on the locator
  strategy actually verified live.
- Named the historical-narrative-in-comments anti-pattern ("backfilled
  after importing X using offset Y...") and asked for it to become a
  permanent, account-wide convention, not just a one-off cleanup.
- Named the parallel anti-pattern in acceptance criteria (don't define a
  story by what it's NOT, e.g. "no other behavior changes") and asked for
  that guardrail to live once in the bot's own instructions instead of
  repeated in every story.
- Set the no-squash-merge convention (real merge commits only) as a
  standing preference.
- Pushed back on wasting a full second CI cycle on a trivial test-case-only
  follow-up PR - led directly to retiring Phase 4 and relocating
  `test_cases.md` ownership to `jw_player_automation`.
- Deliberately scoped Jira status sync as a stretch goal rather than
  letting it creep into the main build.
- Ran the pipeline live, repeatedly, rather than trusting it on paper -
  this is how the JWP-29 AC-substitution failure, the Slack
  quote-escaping bug, the backgrounded-gradle stall, and the
  `allowed_bots` bug were all actually found.
- After the JWP-29 incident, explicitly chose to put the "follow the AC
  literally" guardrail on developer-bot's prompt specifically - a
  deliberate call, made with full knowledge that reviewer-bot was the
  actual bot that rewrote the text.

## JWP-29 follow-up: closed the AC-substitution gap

Reviewer-bot's fix authority was unrestricted, so it silently rewrote a
story's literal acceptance-criteria text. Fixed on both sides: developer-bot's
prompt now says to follow AC exactly and make no unrequested changes;
reviewer-bot's prompt now scopes its FIXED authority to code
correctness/convention only - never rewriting a string literal, copy, or
wording choice, even one it disagrees with. A genuine content concern goes
to BLOCKED (human-visible) instead of a silent rewrite. Verified on JWP-30:
clean `APPROVE`, no fix cycle, ~16 minutes start to finish including the
`jw_player_automation` CI run.

## Phase 7: scoped in detail, not yet built

Replaces the `appium-run` stub with a real AWS Device Farm run. Fully
designed in conversation; nothing built yet. Key decisions:

- **Three separate workflow files**, not one, so generation/verification/
  running can each fail independently: `create-tcs-and-automation-scripts.yml`
  (renamed from `generate-test-automation.yml`) writes the TC + script + a
  small manifest of what's new; `run-unit-tests-and-type-checks.yml`
  (renamed from `ci.yml`) runs on that push; `run-automation.yml` (new)
  triggers via `workflow_run` off *that* succeeding - same gating pattern
  `ai-review.yml` already uses off `Lint` - so real device-minutes are never
  spent against code whose fresh test/type-check run hasn't passed.
- **AWS Device Farm project: "JW Company Device Farm," a 1-device pool**
  (one known Android version/model - confirmed via AWS's own docs that a
  pool doesn't shard a suite, every device runs the whole thing
  independently, so more devices only multiplies cost here, doesn't
  parallelize). Setup will be done interactively with the user in the AWS
  console, deliberately, as a hands-on AWS learning exercise.
- **Every push runs only the new script(s) from that push + one standing
  critical-path smoke test** (play a known song, confirm the timer
  advances) - not a full regression of the whole accumulated suite.
  "What's new" comes from a manifest file `create-tcs-and-automation-
  scripts.yml` writes in its own commit, not from inferring it via
  `git diff` (considered and rejected - breaks on multi-commit pushes,
  can't tell modified from newly-added, and rename quirks would bite once
  `tests/`/`unittests/` splits, so the process that wrote the file just
  declares what it wrote instead).
- **Test data:** real fixture files (mixed extensions/case, nested
  folders) the user is building, delivered via Device Farm's built-in
  "Extra Data" package (auto-extracted onto the device before any test
  runs, including the folder structure for free).
- **Root folder selection uses a debug-only backdoor** (a broadcast intent
  setting the path directly), not the real system folder picker - that
  picker is already marked "not automatable" in `test_cases.md` and stays
  out of scope; every other test calls the backdoor per-test via a pytest
  fixture.
- **Slack gets a full per-test pass/fail list + a link to the AWS run/log**
  - no screenshots or video links.
- Still open: how the built APK gets from `jw_player` to AWS (two hops -
  GitHub Actions artifact, then Device Farm's own `CreateUpload` - since
  Device Farm won't pull from an arbitrary URL), splitting `tests/`
  (scenario tests, what Device Farm runs) from a new `unittests/`
  (mocked-driver framework tests), and wiring the `jw-player-ci` IAM user's
  actual permissions + CI secrets.

## Lesson: GitHub Actions workflow grouping matters - inter-workflow communication is hard

A workflow's steps share a filesystem and step outputs freely; two separate
*workflows* share almost nothing automatically. Every cross-workflow
handoff in this project needed its own deliberate mechanism: `workflow_run`
to gate one workflow on another's completion (`ai-review.yml` off `Lint`,
`run-automation.yml` off `run-unit-tests-and-type-checks.yml`), a committed
file to pass data forward (`last_generated_tests.txt` - a step output
can't cross a workflow boundary, only something durably written to the
repo or a downloadable artifact can), and `actions/download-artifact`'s
cross-repo inputs to pull a build output out of a different repo entirely.
Getting the grouping wrong costs real correctness, not just tidiness -
`repository_dispatch` reruns turned out to replay a frozen pre-fix
snapshot instead of re-resolving the workflow file from `main`. Decide
what one workflow is responsible for and how the next one downstream gets
what it needs *before* writing the YAML, not after.

## Lesson: we went too fast against real Device Farm hardware - should have slowed down and verified locally

Chasing a real-device `ElementNotFoundError`, several rounds went straight
to push-and-rerun against real AWS Device Farm hardware - a longer
timeout, removing an implicit wait, a page-source cache-refresh - each one
plausible, each one a real minutes-long round trip (CI chain, a paid
device run, pulling logs back down), and none of them the actual bug.
The real fix only surfaced once that loop stopped: a local Android
emulator plus a local Appium server reproduced the exact failure in
seconds, which made it possible to isolate the true cause directly -
`AppiumBy.ID` never matches these bare Compose `testTag` resource-ids on
this UiAutomator2 driver version, while a raw `UiSelector().resourceId()`
query matches immediately - and to verify the fix by literally watching it
pass against a running app before ever pushing. Real hardware is for
final confirmation, not for iterating on a hypothesis; the local
emulator/Appium loop should have been the first move, not the fourth or
fifth.

## Status

Phase 0, 1, 2, 3, 5, and 6 are built and verified end-to-end. JWP-2 proved
Phases 0-1; JWP-3 proved the full Phase 1-3 pipeline including a real
`APPROVE` + merge under `jw-company-reviewer-bot`'s own identity; JWP-29
proved the full Phase 1-6 chain for the first time and exposed the
AC-substitution gap (closed, see above); JWP-30 proved the fix with a clean
run. Phase 4 is retired (folded into Phase 6). Phase 7 (real AWS Device
Farm) is scoped in detail but not yet built - the next real piece of work.
Deprioritized/stretch: Jira status sync (Ready for AI -> Coded by AI ->
Merged by AI). Known open items: Gradle caching.
