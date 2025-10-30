#!/bin/bash

URL="http://localhost:9003/actuator/loggers"
USERNAME="admin"
PASSWORD="admin123"

echo "Testing actuator connectivity..."
response_code=$(curl -s -o /dev/null -w "%{http_code}" -u "$USERNAME:$PASSWORD" "$URL")

if [ "$response_code" -ne 200 ]; then
    echo "❌ Cannot connect to actuator (HTTP $response_code)"
    echo "Check if:"
    echo "1. Application is running on port 9003"
    echo "2. Actuator endpoints are enabled"
    echo "3. Basic auth credentials are correct"
    exit 1
fi

echo "✅ Actuator accessible"
echo ""
echo "Current Log Levels:"
echo "==================="

TARGET_PACKAGES=(
    "com.mm_mk.Messaging"
    "org.springframework.security"
    "org.springframework.web"
    "org.springframework.boot"
    "org.springframework.transaction"
    "org.hibernate.SQL"
    "org.hibernate.type.descriptor.sql"
    "org.springframework.jdbc.core"
    "org.apache.tomcat"
    "org.apache.catalina"
    "ROOT"
)

echo "PACKAGE".ljust\(40\) " | LEVEL"
echo "---------------------------------------- | -----"

for package in "${TARGET_PACKAGES[@]}"; do
    response=$(curl -s -u "$USERNAME:$PASSWORD" "$URL/$package")
    level=$(echo "$response" | grep -o '"configuredLevel":"[^"]*"' | cut -d'"' -f4)

    if [ -z "$level" ]; then
        level="DEFAULT"
    fi

    printf "%-40s | %s\n" "$package" "$level"
done
