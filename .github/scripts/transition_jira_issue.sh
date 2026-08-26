#!/usr/bin/env bash
# Transitions a Jira issue to the named status. Looks up the transition id by
# name via the Jira Cloud REST API rather than hardcoding one, so a Jira
# workflow reconfiguration can't silently break this. Requires JIRA_EMAIL and
# JIRA_API_TOKEN in the environment.
#
# Never fails the calling workflow step - same "non-blocking, low priority"
# treatment as this pipeline's Slack notifications. Any lookup/API problem is
# logged as a warning and the script exits 0.
set -uo pipefail

issue_key="$1"
target_status="$2"
site="https://joshuawallis.atlassian.net"
auth=$(printf '%s:%s' "$JIRA_EMAIL" "$JIRA_API_TOKEN" | base64 | tr -d '\n')

transitions=$(curl -sf -H "Authorization: Basic $auth" -H "Accept: application/json" \
  "$site/rest/api/3/issue/$issue_key/transitions")
if [ -z "$transitions" ]; then
  echo "::warning::Could not fetch Jira transitions for $issue_key — skipping status update to '$target_status'"
  exit 0
fi

transition_id=$(echo "$transitions" | jq -r --arg name "$target_status" '.transitions[] | select(.name == $name) | .id' | head -n1)
if [ -z "$transition_id" ]; then
  echo "::warning::No Jira transition named '$target_status' available for $issue_key — skipping"
  exit 0
fi

if curl -sf -X POST -H "Authorization: Basic $auth" -H "Content-Type: application/json" \
  "$site/rest/api/3/issue/$issue_key/transitions" \
  --data "$(jq -n --arg id "$transition_id" '{transition: {id: $id}}')"; then
  echo "Transitioned $issue_key -> $target_status"
else
  echo "::warning::Jira transition POST failed for $issue_key -> $target_status"
fi
