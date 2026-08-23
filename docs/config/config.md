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
