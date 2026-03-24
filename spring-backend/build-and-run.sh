#!/bin/bash
echo "🚀 Building all Spring Boot microservices..."

=for dir in */; do
  if [ -f "$dir/pom.xml" ]; then
    echo "📦 Compiling $dir..."
    (cd "$dir" && mvn -B -ntp clean package -DskipTests)
    if [ $? -ne 0 ]; then
        echo "❌ Maven build failed in $dir. Check the errors above."
        exit 1
    fi
  fi
done

echo "✅ Maven build across all services successful!"
echo "🐳 You can now run docker compose up with your selected services!"
