{
  description = "GunGame NixOS deployment host";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-26.05-small";
    gungame = {
      url = "github:dreem-games/gungame/web";
      inputs.nixpkgs.follows = "nixpkgs";
    };
  };

  outputs =
    {
      gungame,
      nixpkgs,
      ...
    }:
    let
      # При необходимости замените на x86_64-linux.
      system = "aarch64-linux";
    in
    {
      nixosConfigurations.gungame = nixpkgs.lib.nixosSystem {
        inherit system;
        modules = [
          ./hardware-configuration.nix
          gungame.nixosModules.default
          gungame.nixosModules.host
          ./local.nix
          {
            services.gungame = {
              package = gungame.packages.${system}.gungame-server;
              frontendPackage = gungame.packages.${system}.gungame-frontend;
            };
          }
        ];
      };
    };
}
