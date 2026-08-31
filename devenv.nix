{ pkgs, lib, ... }:

let
  packageJson = builtins.fromJSON (builtins.readFile ./package.json);
  npmScripts = packageJson.scripts or { };

  sanitizeScriptName = name: builtins.replaceStrings [ ":" "/" " " ] [ "-" "-" "-" ] name;

  npmScriptNames = builtins.attrNames npmScripts;
  generatedScriptEntries = map (name: {
    name = "gg-${sanitizeScriptName name}";
    value = {
      exec = "npm run ${name}";
    };
  }) npmScriptNames;

  generatedScripts = builtins.listToAttrs generatedScriptEntries;

  customScripts = {
    gg-install.exec = "npm ci";
  };

  allScriptNames =
    (map (entry: entry.name) generatedScriptEntries) ++ builtins.attrNames customScripts;
  uniqueScriptNames = builtins.attrNames (
    builtins.listToAttrs (
      map (name: {
        inherit name;
        value = true;
      }) allScriptNames
    )
  );

  allScripts = generatedScripts // customScripts;
in
assert builtins.length allScriptNames == builtins.length uniqueScriptNames;
{
  env.NODE_ENV = "development";

  packages = with pkgs; [
    git
  ];

  languages.javascript = {
    enable = true;
    package = pkgs.nodejs_24;
    lsp.enable = false;
    npm.enable = true;
  };

  scripts = allScripts;
}
