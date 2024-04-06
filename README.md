# java-chess-engine

## Usage
- `make build`: compiles all classes and packs them into executable `out.jar` with Main entrypoint.
- `make run`: same as `make build`, but `out.jar` is executed immediately after compiling.
- `make clean`: removes any extraneous files associated with compiliation.

Right now, there is no working implementation of the UCI protocol. Once this is added (soon) the functionality when running `out.jar` will change. For the time being, `Main.java`/`Main.class` is the entrypoint, which executes a set of tests - you can modify this easily to configure which tests are ran.
