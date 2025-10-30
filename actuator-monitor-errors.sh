#!/bin/bash

LOG_FILE="logs/messaging.log"
MINUTES_AGO=5
START_HOUR_MINUTE=$(date -d "$MINUTES_AGO minutes ago" '+%H:%M')
CURRENT_HOUR_MINUTE=$(date '+%H:%M')
CURRENT_DATE=$(date '+%Y-%m-%d')

ERROR_LINES=$(awk -v start="$START_HOUR_MINUTE" -v end="$CURRENT_HOUR_MINUTE" -v date="$CURRENT_DATE" '
/ERROR/ {
    # Extract time from timestamp (fields $2 is "HH:MM:SS.MS")
    log_time = substr($2, 1, 5)  # Get just HH:MM part
    if (log_time >= start && log_time <= end) {
        print $0
    }
}' "$LOG_FILE")

ERROR_COUNT=$(echo "$ERROR_LINES" | wc -l)

echo "Errors in the last $MINUTES_AGO minutes: $ERROR_COUNT"

if [ $ERROR_COUNT -gt 0 ]; then
    echo "Matching error lines:"
    echo "$ERROR_LINES"
else
    echo "No errors found."
fi

if [ $ERROR_COUNT -gt 5 ]; then
    echo "🚨 HIGH ERROR COUNT"
    # todo send to telegram
fi
