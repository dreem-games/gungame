{ modulesPath, ... }:

{
  imports = [ "${modulesPath}/profiles/qemu-guest.nix" ];

  networking.hostName = "gungame";
  networking.useDHCP = true;
  time.timeZone = "Europe/Moscow";

  services.gungame.enable = true;

  system.stateVersion = "26.05";
}
