# Config

Where configuration and secrets for this project actually live. Values aren't
duplicated here — just pointers to where to find/manage them.

## Secrets

- **Slack Incoming Webhook URL** ("CI Bot" app, "JW Player CI" channel in the
  "JW Company" workspace) - stored as a GitHub Actions secret named
  `SLACK_JW_PLAYER_CI_WEBHOOK_URL` in both `jw_player` and
  `jw_player_automation`.
  <https://github.com/jwallis/jw_player/settings/secrets/actions>
- **GitHub PAT (classic, `repo` scope)** used to call `repository_dispatch` on
  `jw_player` - stored in plaintext in the `Authorization` header of the
  "Ready for AI" Flow's "Send web request" action, in the JWP Jira project's
  Automation/Flows. No Jira secrets vault was available on this plan, so this
  is not encrypted at rest - only exposure is anyone with edit access to that
  Flow.
- **AWS access key/secret** for the `jw-player-ci` IAM user (AWS account
  975669876094, region `us-east-2`) - stored locally in
  `~/.aws/credentials` under the `jw-player-ci` profile
  (`aws configure --profile jw-player-ci`). No permissions attached yet -
  scoping deferred to Phase 7 (Device Farm integration). Not yet stored
  anywhere in CI; will need to become a GitHub Actions secret (or be replaced
  by OIDC federation) when Phase 7 actually wires up Device Farm calls from
  GitHub Actions.
- **Anthropic API key** (dedicated key, named `jw-player-ci` in the Anthropic
  console - not the personal/shared key) used by `story-implementation.yml`
  to run Claude Code non-interactively via `anthropics/claude-code-action`.
  Stored as a GitHub Actions secret named `ANTHROPIC_API_KEY` in both
  `jw_player` and `jw_player_automation`, set via `gh secret set
  ANTHROPIC_API_KEY --repo <owner>/<repo>`.
  <https://github.com/jwallis/jw_player/settings/secrets/actions> - GitHub
  secrets are write-only (can't be viewed again once set). The raw key value
  was deliberately **not** saved anywhere (no password manager copy) - it's
  set in both repos that will ever need it (Phase 6 covered already), so it
  stays a CI-only credential with zero copies outside GitHub. If it's ever
  rotated, generate a new key and re-run `gh secret set` in both repos.
- **`jw-company-developer-bot` and `jw-company-reviewer-bot` GitHub App private
  keys** - give Phase 1/2 (developer-bot: opens the PR, commits lint autofixes)
  and Phase 3 (reviewer-bot: approves/requests changes/merges) each their own
  GitHub identity, distinct from the shared `github-actions[bot]` - needed
  because GitHub blocks a PR's own author from formally approving it, so
  review has to run as a genuinely different actor. Installed on `jw_player`
  only (not `jw_player_automation` - nothing there needs them yet). Stored as
  GitHub Actions secrets, set via `gh secret set <NAME> --repo jwallis/jw_player
  < path/to/key.pem`:
  - `DEVELOPER_BOT_PRIVATE_KEY` / `DEVELOPER_BOT_CLIENT_ID`
  - `REVIEWER_BOT_PRIVATE_KEY` / `REVIEWER_BOT_CLIENT_ID`
  <https://github.com/jwallis/jw_player/settings/secrets/actions> - private
  keys are write-only once set (regenerate + re-run `gh secret set` to
  rotate). Client IDs aren't sensitive (GitHub uses them in public OAuth
  flows) but are stored as secrets anyway for consistency with how the
  workflow reads them.

## GitHub Apps (non-secret reference)

App ID and Client ID aren't secrets - recorded here for reference, since
they're needed to know which app a workflow run authenticated as.

- **`jw-company-developer-bot`** - App ID `4706766`, Client ID
  `Iv23ctGq9Lvc36MMKrzd`. <https://github.com/settings/apps/jw-company-developer-bot>
- **`jw-company-reviewer-bot`** - App ID `4706705`, Client ID
  `Iv23liCTa1vEkxRXn3IR`. <https://github.com/settings/apps/jw-company-reviewer-bot>

Both: Repository permissions `Contents: Read & write`, `Pull requests: Read &
write`; installed on `jw_player` only.

## Jira status sync

- **Jira site:** `https://joshuawallis.atlassian.net` (not sensitive — used
  directly in `.github/scripts/transition_jira_issue.sh`, present identically
  in both `jw_player` and `jw_player_automation`).
- **`JIRA_EMAIL` / `JIRA_API_TOKEN`** — an Atlassian API token
  (`id.atlassian.com/manage-profile/security/api-tokens`) paired with the
  account email, used for Basic Auth against the Jira Cloud REST API
  (`/rest/api/3/issue/{key}/transitions`) to move an issue's status as the
  pipeline progresses (`Ready for AI` → `AI Coding` → `AI Testing` → `Done`,
  or `AI Coding Failed`/`AI Testing Failed` on the way). Stored as GitHub
  Actions secrets in **both** `jw_player` and `jw_player_automation` (every
  workflow stage transitions the issue itself), set via:
  `gh secret set JIRA_EMAIL --repo jwallis/jw_player`
  `gh secret set JIRA_API_TOKEN --repo jwallis/jw_player`
  (repeat both for `jwallis/jw_player_automation`) — write-only once set,
  same as every other secret here.
- A failed Jira call never fails the calling workflow step — same
  non-blocking treatment as the Slack notifications. The transition script
  looks up the target transition by status *name* (not a hardcoded id) so a
  Jira workflow reconfiguration can't silently break it; a name that doesn't
  match anything just logs a warning.

<!--
  Where the Client ID actually gets used: it's the `app-id` input to
  actions/create-github-app-token in the workflow (accepts either the numeric
  App ID or the Client ID - Client ID is GitHub's current recommendation).
  That step exchanges the App's private key for a short-lived installation
  token, which then replaces github.token in whichever steps should run as
  that bot instead of the default github-actions[bot]:
    - developer-bot: story-implementation.yml (branch/commit/push/PR-create),
      lint.yml (autofix commit/push)
    - reviewer-bot: ai-review.yml (review/approve/merge, and the FIXED-path
      commit/push)
  The App ID itself isn't referenced anywhere in a workflow once the Client ID
  is in use - it's kept here purely so a GitHub Apps settings page can be
  found by ID if needed.
-->
