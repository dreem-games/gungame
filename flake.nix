{
  description = "GunGame frontend, multiplayer server and NixOS service module";

  inputs.nixpkgs.url = "github:NixOS/nixpkgs/nixpkgs-unstable";

  outputs =
    { self, nixpkgs }:
    let
      packageSystems = [
        "x86_64-linux"
        "aarch64-linux"
      ];
      formatterSystems = packageSystems ++ [
        "aarch64-darwin"
      ];
      forAllPackageSystems = nixpkgs.lib.genAttrs packageSystems;
      packagesFor = system: import ./nix/packages.nix { pkgs = nixpkgs.legacyPackages.${system}; };
    in
    {
      packages = forAllPackageSystems (
        system:
        let
          packages = packagesFor system;
        in
        packages
        // {
          default = packages.gungame-server;
        }
      );

      checks = forAllPackageSystems (system: packagesFor system);

      formatter = nixpkgs.lib.genAttrs formatterSystems (
        system: nixpkgs.legacyPackages.${system}.nixfmt-tree
      );

      nixosModules = {
        default = self.nixosModules.gungame;
        gungame = import ./nix/modules/gungame.nix;
        host = import ./nix/host/configuration.nix;
      };
    };
}
