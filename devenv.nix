{
  config,
  pkgs,
  lib,
  ...
}:

let
  projectRoot = config.devenv.root;
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
    gg-production.exec = ''
      npm run build
      exec devenv up
    '';
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

  processes.gungame-server.exec = "HOST=127.0.0.1 PORT=18082 node server.js";

  services.nginx = {
    enable = true;
    httpConfig = ''
      server {
        listen 127.0.0.1:18083;
        server_name localhost;
        root ${projectRoot}/dist;

        location = /ws {
          proxy_pass http://127.0.0.1:18082;
          proxy_http_version 1.1;
          proxy_set_header Upgrade $http_upgrade;
          proxy_set_header Connection "upgrade";
        }

        location / {
          try_files $uri $uri/ /index.html;
        }
      }
    '';
  };

  scripts = allScripts;
}
