#!/bin/bash

echo "🏗️ Building Inventory Blockchain Application..."

# Create necessary directories
mkdir -p target/classes
mkdir -p target/lib

echo "📥 Downloading dependencies..."

# Create lib directory for manual dependency management
LIBS_DIR="target/lib"

# Download JavaFX (for Ubuntu/Linux)
echo "📦 Setting up JavaFX..."
if [ ! -f "$LIBS_DIR/javafx-controls-17.0.2.jar" ]; then
    echo "JavaFX not found. Please install JavaFX manually or use Maven."
    echo "For now, we'll create a simple version without JavaFX dependencies."
fi

echo "📝 Compiling Java sources..."

# Compile all Java files
find src/main/java -name "*.java" > sources.txt

# Compile with basic classpath
javac -d target/classes \
      -cp "target/lib/*" \
      @sources.txt

if [ $? -eq 0 ]; then
    echo "✅ Compilation successful!"
    
    # Copy resources
    echo "📋 Copying resources..."
    cp -r src/main/resources/* target/classes/ 2>/dev/null || true
    
    echo "🎯 Build completed!"
    echo ""
    echo "📖 To run the application:"
    echo "   1. Install Maven: sudo apt install maven"
    echo "   2. Run: mvn javafx:run"
    echo "   OR"
    echo "   3. Use an IDE like IntelliJ IDEA or Eclipse"
else
    echo "❌ Compilation failed!"
    exit 1
fi

# Cleanup
rm -f sources.txt