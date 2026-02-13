with import <nixpkgs> {};

mkShell {
  buildInputs = [
    maven
    jdk25_headless
    jetbrains.idea-oss
  ];

  shellHook = ''
    export JAVA_HOME=${jdk25_headless}
    export PATH=$JAVA_HOME/bin:$PATH
    
    DEST="$HOME/.var/app/com.hypixel.HytaleLauncher/data/Hytale/UserData/Mods"

    plugin() {
        echo "Building project with debug profile..."
        mvn clean package -P debug

        echo "Copying JAR to $DEST"
        mkdir -p "$DEST"
        cp target/*.jar "$DEST/"

        echo "Plugin build complete!"
    }

    resource() {
        echo "Copying Common and Server resources into DebugAssets..."
        local debugDir="$DEST/DebugAssets"

        # Remove existing folders first
        rm -rf "$debugDir/Common" "$debugDir/Server"

        mkdir -p "$debugDir/Common" "$debugDir/Server"

        cp -r src/main/resources/Common/* "$debugDir/Common/"
        cp -r src/main/resources/Server/* "$debugDir/Server/"
        cp -r src/main/resources/debug_manifest.json "$debugDir/manifest.json"

        echo "Asset copy complete!"
    }

    debug() {
        #call plugin only build
        plugin
        
        #call resource copy
        resource

        echo "Debug setup complete!"
    }

    build() {
        echo "Building project..."
        mvn clean package -P release

        echo "Copying JAR to $DEST"
        mkdir -p "$DEST"
        cp target/*.jar "$DEST/"

        echo "Build complete!"
    }

    pull() {
        local dest="$(pwd)/src/main/resources"
        local debugDir="$DEST/DebugAssets"

        if [ ! -d "$debugDir" ]; then
            echo "No DebugAssets folder found at $debugDir"
            return 1
        fi

        echo "Pulling changes from DebugAssets..."

        # Pull Common
        if [ -d "$debugDir/Common" ]; then
            rm -rf "$dest/Common"
            cp -r "$debugDir/Common" "$dest/Common"
        fi

        # Pull Server
        if [ -d "$debugDir/Server" ]; then
            rm -rf "$dest/Server"
            cp -r "$debugDir/Server" "$dest/Server"
        fi

        echo "Changes pulled successfully!"
    }
  '';
}
