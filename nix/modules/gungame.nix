{
  config,
  lib,
  pkgs,
  ...
}:

let
  cfg = config.services.gungame;

  deployImplementation = pkgs.writeShellApplication {
    name = "gungame-deploy-implementation";
    runtimeInputs = [
      config.system.build.nixos-rebuild
      pkgs.coreutils
      pkgs.curl
      pkgs.nix
      pkgs.systemd
      pkgs.util-linux
    ];
    text = builtins.readFile ../deploy-gungame.sh;
  };

  deployCommand = pkgs.writeShellApplication {
    name = "gungame-deploy";
    text = ''
      if (( $# != 0 )); then
          echo "gungame-deploy does not accept arguments" >&2
          exit 64
      fi

      export GUNGAME_NIXOS_FLAKE=${lib.escapeShellArg cfg.deploy.flakePath}
      export GUNGAME_NIXOS_CONFIGURATION=${lib.escapeShellArg cfg.deploy.configuration}
      export GUNGAME_HEALTH_URL=${lib.escapeShellArg "http://${cfg.address}:${toString cfg.port}/"}
      exec ${deployImplementation}/bin/gungame-deploy-implementation
    '';
  };

  forcedDeployKeys = map (
    key: ''restrict,command="${pkgs.sudo}/bin/sudo ${deployCommand}/bin/gungame-deploy" ${key}''
  ) cfg.deploy.authorizedKeys;
in
{
  options.services.gungame = {
    enable = lib.mkEnableOption "GunGame frontend and authoritative multiplayer server";

    package = lib.mkOption {
      type = lib.types.package;
      description = "Пакет авторитетного GunGame WebSocket-сервера.";
    };

    frontendPackage = lib.mkOption {
      type = lib.types.package;
      description = "Пакет собранного статического GunGame frontend.";
    };

    domain = lib.mkOption {
      type = lib.types.str;
      example = "game.example.com";
      description = "Публичный домен, для которого Caddy получает TLS-сертификат.";
    };

    address = lib.mkOption {
      type = lib.types.str;
      default = "127.0.0.1";
      description = "Адрес сервера, используемый Caddy для локального проксирования.";
    };

    port = lib.mkOption {
      type = lib.types.port;
      default = 8080;
      description = "Локальный порт WebSocket-сервера.";
    };

    deploy = {
      enable = lib.mkEnableOption "ограниченный CI-деплой через SSH";

      user = lib.mkOption {
        type = lib.types.str;
        default = "gungame-deploy";
        description = "Системный пользователь для CI-деплоя.";
      };

      authorizedKeys = lib.mkOption {
        type = lib.types.listOf lib.types.str;
        default = [ ];
        example = [ "ssh-ed25519 AAAAC3... github-actions-gungame" ];
        description = "Публичные SSH-ключи CI без options-префикса.";
      };

      flakePath = lib.mkOption {
        type = lib.types.str;
        default = "/etc/nixos";
        description = "Путь к host flake, содержащему input с именем gungame.";
      };

      configuration = lib.mkOption {
        type = lib.types.str;
        default = "gungame";
        description = "Имя nixosConfigurations для удалённой машины.";
      };
    };
  };

  config = lib.mkIf cfg.enable {
    assertions = [
      {
        assertion = !cfg.deploy.enable || cfg.deploy.authorizedKeys != [ ];
        message = "services.gungame.deploy.authorizedKeys must contain at least one CI public key";
      }
    ];

    systemd.services.gungame-server = {
      description = "GunGame authoritative multiplayer server";
      wantedBy = [ "multi-user.target" ];
      after = [ "network-online.target" ];
      wants = [ "network-online.target" ];

      environment = {
        NODE_ENV = "production";
        PORT = toString cfg.port;
      };

      serviceConfig = {
        Type = "simple";
        ExecStart = "${cfg.package}/bin/gungame-server";
        WorkingDirectory = "${cfg.package}/lib/gungame";
        Restart = "on-failure";
        RestartSec = 2;
        DynamicUser = true;
        NoNewPrivileges = true;
        PrivateTmp = true;
        ProtectHome = true;
        ProtectSystem = "strict";
        RestrictAddressFamilies = [
          "AF_INET"
          "AF_INET6"
          "AF_UNIX"
        ];
      };
    };

    services.caddy = {
      enable = true;
      virtualHosts.${cfg.domain}.extraConfig = ''
        route {
          @websocket {
            header Connection *Upgrade*
            header Upgrade websocket
          }
          reverse_proxy @websocket ${cfg.address}:${toString cfg.port}

          root * ${cfg.frontendPackage}/share/gungame
          encode zstd gzip
          try_files {path} /index.html
          file_server
        }
      '';
    };

    networking.firewall.allowedTCPPorts = [
      80
      443
    ];

    environment.systemPackages = lib.optionals cfg.deploy.enable [ deployCommand ];

    users.groups = lib.mkIf cfg.deploy.enable {
      ${cfg.deploy.user} = { };
    };

    users.users = lib.mkIf cfg.deploy.enable {
      ${cfg.deploy.user} = {
        isSystemUser = true;
        group = cfg.deploy.user;
        home = "/var/lib/${cfg.deploy.user}";
        createHome = true;
        shell = pkgs.bashInteractive;
        openssh.authorizedKeys.keys = forcedDeployKeys;
      };
    };

    services.openssh.enable = lib.mkIf cfg.deploy.enable true;
    services.openssh.openFirewall = lib.mkIf cfg.deploy.enable true;

    security.sudo.extraRules = lib.optionals cfg.deploy.enable [
      {
        users = [ cfg.deploy.user ];
        commands = [
          {
            command = "${deployCommand}/bin/gungame-deploy";
            options = [ "NOPASSWD" ];
          }
        ];
      }
    ];
  };
}
