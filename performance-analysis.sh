#!/bin/bash

echo "📊 Messaging Service Performance Analysis Report"
echo "================================================"
echo ""

# SLOW OPERATIONS ANALYSIS
echo "SLOW OPERATIONS (over threshold):"
echo "---------------------------------"
grep "SLOW OPERATION" logs/messaging.log | \
  awk -F'SLOW OPERATION: ' '{print $2}' | \
  sort | uniq -c | sort -nr

echo ""

# SLOW API CALLS
echo "SLOW API CALLS (over 2000ms):"
echo "-----------------------------"
grep "SLOW API" logs/messaging.log | \
  awk -F'SLOW API: ' '{print $2}' | \
  sort | uniq -c | sort -nr

echo ""

# SLOW WEBSOCKET OPERATIONS
echo "SLOW WEBSOCKET OPERATIONS (over 1000ms):"
echo "----------------------------------------"
grep "SLOW WEBSOCKET" logs/messaging.log | \
  awk -F'SLOW WEBSOCKET: ' '{print $2}' | \
  sort | uniq -c | sort -nr

echo ""

# SLOW EVENT PROCESSING
echo "SLOW EVENT PROCESSING (over 500ms):"
echo "-----------------------------------"
grep "SLOW EVENT" logs/messaging.log | \
  awk -F'SLOW EVENT: ' '{print $2}' | \
  sort | uniq -c | sort -nr

echo ""

# AVERAGE OPERATION TIMES
echo "AVERAGE OPERATION TIMES:"
echo "-----------------------"
echo "Message Sending: $(grep "Message sending completed" logs/messaging.log | awk -F'in ' '{print $2}' | awk -F'ms' '{sum+=$1; count++} END {if(count>0) printf "%.2fms", sum/count}')"
echo "Message Retrieval: $(grep "Message retrieval completed" logs/messaging.log | awk -F'in ' '{print $2}' | awk -F'ms' '{sum+=$1; count++} END {if(count>0) printf "%.2fms", sum/count}')"
echo "Room Cleanup: $(grep "Room cleanup completed" logs/messaging.log | awk -F'in ' '{print $2}' | awk -F'ms' '{sum+=$1; count++} END {if(count>0) printf "%.2fms", sum/count}')"

echo ""

# WEBSOCKET OPERATION TIMES
echo "WEBSOCKET OPERATION TIMES:"
echo "-------------------------"
echo "Chat Message Processing: $(grep "WebSocket CHAT_MESSAGE processing completed" logs/messaging.log | awk -F'in ' '{print $2}' | awk -F'ms' '{sum+=$1; count++} END {if(count>0) printf "%.2fms", sum/count}')"
echo "Key Event Processing: $(grep "WebSocket KEY_EVENT processing completed" logs/messaging.log | awk -F'in ' '{print $2}' | awk -F'ms' '{sum+=$1; count++} END {if(count>0) printf "%.2fms", sum/count}')"
echo "User Join Processing: $(grep "WebSocket USER_JOIN processing completed" logs/messaging.log | awk -F'in ' '{print $2}' | awk -F'ms' '{sum+=$1; count++} END {if(count>0) printf "%.2fms", sum/count}')"
echo "User Leave Processing: $(grep "WebSocket USER_LEAVE processing completed" logs/messaging.log | awk -F'in ' '{print $2}' | awk -F'ms' '{sum+=$1; count++} END {if(count>0) printf "%.2fms", sum/count}')"

echo ""

# EVENT PROCESSING TIMES
echo "EVENT PROCESSING TIMES:"
echo "----------------------"
echo "Message Sent Event: $(grep "MESSAGE_SENT event processing completed" logs/messaging.log | awk -F'in ' '{print $2}' | awk -F'ms' '{sum+=$1; count++} END {if(count>0) printf "%.2fms", sum/count}')"
echo "Room Created Event: $(grep "ROOM_CREATED event processing completed" logs/messaging.log | awk -F'in ' '{print $2}' | awk -F'ms' '{sum+=$1; count++} END {if(count>0) printf "%.2fms", sum/count}')"
echo "Room Deleted Event: $(grep "ROOM_DELETED event processing completed" logs/messaging.log | awk -F'in ' '{print $2}' | awk -F'ms' '{sum+=$1; count++} END {if(count>0) printf "%.2fms", sum/count}')"
echo "User Joined Event: $(grep "USER_JOINED event processing completed" logs/messaging.log | awk -F'in ' '{print $2}' | awk -F'ms' '{sum+=$1; count++} END {if(count>0) printf "%.2fms", sum/count}')"
echo "User Left Event: $(grep "USER_LEFT event processing completed" logs/messaging.log | awk -F'in ' '{print $2}' | awk -F'ms' '{sum+=$1; count++} END {if(count>0) printf "%.2fms", sum/count}')"
echo "User Created Event: $(grep "USER_CREATED event processing completed" logs/messaging.log | awk -F'in ' '{print $2}' | awk -F'ms' '{sum+=$1; count++} END {if(count>0) printf "%.2fms", sum/count}')"
echo "User Updated Event: $(grep "USER_UPDATED event processing completed" logs/messaging.log | awk -F'in ' '{print $2}' | awk -F'ms' '{sum+=$1; count++} END {if(count>0) printf "%.2fms", sum/count}')"

echo ""

# API SUCCESS RATES
echo "API SUCCESS RATES:"
echo "-----------------"
TOTAL_SEND_REQUESTS=$(grep "SEND_MESSAGE request" logs/messaging.log | wc -l)
SUCCESSFUL_SENDS=$(grep "SEND_MESSAGE success" logs/messaging.log | wc -l)
if [ $TOTAL_SEND_REQUESTS -gt 0 ]; then
    SEND_SUCCESS_RATE=$((SUCCESSFUL_SENDS * 100 / TOTAL_SEND_REQUESTS))
    echo "Message Send Success Rate: $SEND_SUCCESS_RATE% ($SUCCESSFUL_SENDS/$TOTAL_SEND_REQUESTS)"
else
    echo "No message send attempts found"
fi

TOTAL_GET_REQUESTS=$(grep "GET_MESSAGES request" logs/messaging.log | wc -l)
SUCCESSFUL_GETS=$(grep "GET_MESSAGES success" logs/messaging.log | wc -l)
if [ $TOTAL_GET_REQUESTS -gt 0 ]; then
    GET_SUCCESS_RATE=$((SUCCESSFUL_GETS * 100 / TOTAL_GET_REQUESTS))
    echo "Message Retrieval Success Rate: $GET_SUCCESS_RATE% ($SUCCESSFUL_GETS/$TOTAL_GET_REQUESTS)"
else
    echo "No message retrieval attempts found"
fi

echo ""

# ERROR ANALYSIS
echo "ERROR ANALYSIS:"
echo "--------------"
echo "Total Errors: $(grep "ERROR" logs/messaging.log | wc -l)"
echo "Room Not Found Errors: $(grep "Room not found in local_rooms" logs/messaging.log | wc -l)"
echo "User Not Found Errors: $(grep "User not found in local_users" logs/messaging.log | wc -l)"
echo "WebSocket Errors: $(grep "Failed to handle chat message" logs/messaging.log | wc -l)"
echo "Event Processing Errors: $(grep "Error processing.*event" logs/messaging.log | wc -l)"

echo ""

# MESSAGE STATISTICS
echo "MESSAGE STATISTICS:"
echo "------------------"
TOTAL_MESSAGES_SENT=$(grep "SEND_MESSAGE success" logs/messaging.log | wc -l)
AVG_MESSAGE_LENGTH=$(grep "SEND_MESSAGE request" logs/messaging.log | awk -F'contentLength: ' '{print $2}' | awk '{sum+=$1; count++} END {if(count>0) printf "%.0f characters", sum/count}')
echo "Total Messages Sent: $TOTAL_MESSAGES_SENT"
echo "Average Message Length: $AVG_MESSAGE_LENGTH"

echo ""

# EVENT PROCESSING VOLUME
echo "EVENT PROCESSING VOLUME:"
echo "-----------------------"
echo "Message Sent Events: $(grep "Processing MESSAGE_SENT event" logs/messaging.log | wc -l)"
echo "Room Created Events: $(grep "Processing ROOM_CREATED event" logs/messaging.log | wc -l)"
echo "Room Deleted Events: $(grep "Processing ROOM_DELETED event" logs/messaging.log | wc -l)"
echo "User Joined Events: $(grep "Processing USER_JOINED event" logs/messaging.log | wc -l)"
echo "User Left Events: $(grep "Processing USER_LEFT event" logs/messaging.log | wc -l)"
echo "User Created Events: $(grep "Processing USER_CREATED event" logs/messaging.log | wc -l)"
echo "User Updated Events: $(grep "Processing USER_UPDATED event" logs/messaging.log | wc -l)"

echo ""

# WEBSOCKET ACTIVITY
echo "WEBSOCKET ACTIVITY:"
echo "------------------"
echo "Chat Messages via WebSocket: $(grep "WebSocket CHAT_MESSAGE" logs/messaging.log | wc -l)"
echo "Key Events via WebSocket: $(grep "WebSocket KEY_EVENT" logs/messaging.log | wc -l)"
echo "User Joins via WebSocket: $(grep "WebSocket USER_JOIN" logs/messaging.log | wc -l)"
echo "User Leaves via WebSocket: $(grep "WebSocket USER_LEAVE" logs/messaging.log | wc -l)"

echo ""

# PERFORMANCE SUMMARY
echo "PERFORMANCE SUMMARY:"
echo "-------------------"
SLOW_OPERATIONS_COUNT=$(grep -c "SLOW OPERATION" logs/messaging.log)
SLOW_API_COUNT=$(grep -c "SLOW API" logs/messaging.log)
SLOW_WEBSOCKET_COUNT=$(grep -c "SLOW WEBSOCKET" logs/messaging.log)
SLOW_EVENT_COUNT=$(grep -c "SLOW EVENT" logs/messaging.log)

TOTAL_SLOW_OPERATIONS=$((SLOW_OPERATIONS_COUNT + SLOW_API_COUNT + SLOW_WEBSOCKET_COUNT + SLOW_EVENT_COUNT))

if [ $TOTAL_SLOW_OPERATIONS -eq 0 ]; then
    echo "✅ No slow operations detected"
elif [ $TOTAL_SLOW_OPERATIONS -lt 10 ]; then
    echo "⚠️  Few slow operations: $TOTAL_SLOW_OPERATIONS total"
else
    echo "🚨 High number of slow operations: $TOTAL_SLOW_OPERATIONS total"
    echo "   - SLOW OPERATIONS: $SLOW_OPERATIONS_COUNT"
    echo "   - SLOW API: $SLOW_API_COUNT"
    echo "   - SLOW WEBSOCKET: $SLOW_WEBSOCKET_COUNT"
    echo "   - SLOW EVENT: $SLOW_EVENT_COUNT"
fi
