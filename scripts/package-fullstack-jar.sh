#!/usr/bin/env bash
set -e

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo "==> Step 1: Building React Frontend..."
cd "$DIR/../frontend"
npm run build

echo "==> Step 2: Copying frontend build artifacts into Spring Boot static resources..."
STATIC_DIR="$DIR/../backend/src/main/resources/static"
rm -rf "$STATIC_DIR"
mkdir -p "$STATIC_DIR"
cp -r "$DIR/../frontend/dist/"* "$STATIC_DIR/"

echo "==> Step 3: Packaging Spring Boot JAR with embedded frontend..."
cd "$DIR/../backend"
mvn clean package -DskipTests

echo ""
echo "SUCCESS: Full-stack standalone JAR created at backend/target/placetrack-backend-0.0.1-SNAPSHOT.jar"
echo "Run it with: java -jar backend/target/placetrack-backend-0.0.1-SNAPSHOT.jar"
