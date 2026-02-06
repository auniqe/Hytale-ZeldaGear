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

    build() {
        local dest="$HOME/.var/app/com.hypixel.HytaleLauncher/data/Hytale/UserData/Mods"

        echo "Building project..."
        mvn clean package -P release

        echo "Copying JAR to $dest"
        mkdir -p "$dest"
        cp target/*.jar "$dest/"

        echo "Build complete!"
    }


    debug() {
        local dest="$HOME/.var/app/com.hypixel.HytaleLauncher/data/Hytale/UserData/Mods"

        echo "Building project with debug profile..."
        mvn clean package -P debug

        echo "Copying JAR to $dest"
        mkdir -p "$dest"
        cp target/*.jar "$dest/"

        echo "Copying Common and Server resources into DebugAssets..."
        debugDir="$dest/DebugAssets"
        
        mkdir -p "$debugDir/Common" "$debugDir/Server"

        cp -r src/main/resources/Common/* "$debugDir/Common/"
        cp -r src/main/resources/Server/* "$debugDir/Server/"
        cp -r src/main/resources/debug_manifest.json "$debugDir/manifest.json"

        echo "Debug build complete!"
    }

    pull() {
        local dest="$(pwd)/src/main/resources"
        local debugDir="$HOME/.var/app/com.hypixel.HytaleLauncher/data/Hytale/UserData/Mods/DebugAssets"

        if [ ! -d "$debugDir" ]; then
            echo "No DebugAssets folder found at $debugDir"
            return 1
        fi

        echo "Pulling changes from DebugAssets..."

        # Pull Common
        if [ -d "$debugDir/Common" ]; then
            mkdir -p "$dest/Common"
            cp -ru "$debugDir/Common/"* "$dest/Common/"
        fi

        # Pull Server
        if [ -d "$debugDir/Server" ]; then
            mkdir -p "$dest/Server"
            cp -ru "$debugDir/Server/"* "$dest/Server/"
        fi

        echo "Changes pulled successfully!"
    }
  '';
}
