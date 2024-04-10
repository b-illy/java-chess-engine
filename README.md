# java-chess-engine

## Usage
- `make build`: compiles all classes and packs them into executable `out.jar` with Main entrypoint.
- `make run`: same as `make build`, but `out.jar` is executed immediately after compiling.
- `make clean`: removes any extraneous files associated with compiliation.

To configure functionality of running the engine via `out.jar`, you will need to modify some constant variables in `Main.java`. If `testmode` is set, the program will run some of its own various tests (you can further configure which tests run exactly using aforementioned variables). Otherwise, the engine will function in UCI mode - this mode allows it to be used by 3rd party external programs, such as a chess GUI, which will handle the sending and receiving of data using commands. An executable archive compiled in UCI mode is not intended to be used by humans, therefore using it in a non-automated fashion may be confusing.
