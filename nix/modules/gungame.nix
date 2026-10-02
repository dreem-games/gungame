{
  config,
  lib,
  ...
}:

let
  cfg = config.services.gungame;
in
{
  options.services.gungame = {
    enable = lib.mkEnableOption "GunGame authoritative multiplayer server";

    package = lib.mkOption {
      type = lib.types.package;
      description = "Пакет авторитетного GunGame WebSocket-сервера.";
    };

    address = lib.mkOption {
      type = lib.types.str;
      default = "127.0.0.1";
      description = "Адрес, на котором WebSocket-сервер принимает соединения.";
    };

    port = lib.mkOption {
      type = lib.types.port;
      default = 8080;
      description = "Порт WebSocket-сервера.";
    };
  };

  config = lib.mkIf cfg.enable {
    systemd.services.gungame-server = {
      description = "GunGame authoritative multiplayer server";
      wantedBy = [ "multi-user.target" ];
      after = [ "network-online.target" ];
      wants = [ "network-online.target" ];

      environment = {
        HOST = cfg.address;
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
  };
}
