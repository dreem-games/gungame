{ pkgs }:

let
  inherit (pkgs) lib;
  packageJson = builtins.fromJSON (builtins.readFile ../package.json);
  version = packageJson.version;
  npmDepsHash = "sha256-BGRieG9uXPK4UAfxi0Z+B5NTX4rbBlAlz7SjbPiupiw=";

  frontendSource = lib.fileset.toSource {
    root = ../.;
    fileset = lib.fileset.unions [
      ../index.html
      ../multiplayer-map.json
      ../package.json
      ../package-lock.json
      ../public
      ../src
      ../tsconfig.json
    ];
  };

  serverSource = lib.fileset.toSource {
    root = ../.;
    fileset = lib.fileset.unions [
      ../map-gen.js
      ../multiplayer-map.json
      ../package.json
      ../package-lock.json
      ../server.js
    ];
  };
in
{
  gungame-frontend = pkgs.buildNpmPackage {
    pname = "gungame-frontend";
    inherit version npmDepsHash;
    src = frontendSource;

    npmBuildScript = "build";

    installPhase = ''
      runHook preInstall
      mkdir -p "$out/share/gungame"
      cp -R dist/. "$out/share/gungame/"
      runHook postInstall
    '';
  };

  gungame-server = pkgs.buildNpmPackage {
    pname = "gungame-server";
    inherit version npmDepsHash;
    src = serverSource;

    npmInstallFlags = [ "--omit=dev" ];
    dontNpmBuild = true;
    nativeBuildInputs = [ pkgs.makeWrapper ];

    installPhase = ''
      runHook preInstall

      mkdir -p "$out/lib/gungame" "$out/bin"
      cp map-gen.js multiplayer-map.json server.js "$out/lib/gungame/"
      cp -R node_modules "$out/lib/gungame/"

      makeWrapper ${pkgs.nodejs_24}/bin/node "$out/bin/gungame-server" \
        --add-flags "$out/lib/gungame/server.js"

      runHook postInstall
    '';
  };
}
