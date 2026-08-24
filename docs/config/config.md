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
