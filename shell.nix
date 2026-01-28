with import <nixpkgs> {};

mkShell {
  buildInputs = [
    maven
    jdk25_headless
  ];

  shellHook = ''
    export JAVA_HOME=${jdk25_headless}
    export PATH=$JAVA_HOME/bin:$PATH

    build() {
        # local dest="$1"
        local dest="/home/loevi/.var/app/com.hypixel.HytaleLauncher/data/Hytale/UserData/Mods"

        echo "Building project..."
        mvn clean package

        echo "Copying JAR to $dest"
        mkdir -p "$dest"
        cp target/*.jar "$dest/"

        echo "Build complete!"
    }


    debug() {
        local dest="/home/loevi/.var/app/com.hypixel.HytaleLauncher/data/Hytale/UserData/Mods"

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
  '';
}
