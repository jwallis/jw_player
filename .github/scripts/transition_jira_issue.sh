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

get_response=$(curl -s -w '\n%{http_code}' -H "Authorization: Basic $auth" -H "Accept: application/json" \
  "$site/rest/api/3/issue/$issue_key/transitions")
get_status=$(echo "$get_response" | tail -n1)
transitions=$(echo "$get_response" | sed '$d')
if [ "$get_status" != "200" ]; then
  echo "::warning::Could not fetch Jira transitions for $issue_key (HTTP $get_status) — skipping status update to '$target_status'. Response: $transitions"
  exit 0
fi

transition_id=$(echo "$transitions" | jq -r --arg name "$target_status" '.transitions[] | select(.name == $name) | .id' | head -n1)
if [ -z "$transition_id" ]; then
  available=$(echo "$transitions" | jq -r '[.transitions[].name] | join(", ")')
  echo "::warning::No Jira transition named '$target_status' available for $issue_key — skipping. Available: $available"
  exit 0
fi

post_status=$(curl -s -o /dev/null -w '%{http_code}' -X POST -H "Authorization: Basic $auth" -H "Content-Type: application/json" \
  "$site/rest/api/3/issue/$issue_key/transitions" \
  --data "$(jq -n --arg id "$transition_id" '{transition: {id: $id}}')")
if [ "$post_status" = "204" ]; then
  echo "Transitioned $issue_key -> $target_status"
else
  echo "::warning::Jira transition POST failed for $issue_key -> $target_status (HTTP $post_status)"
fi
