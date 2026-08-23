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
