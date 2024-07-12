## NOTICE

This project is not in active development or maintenance and will only receive occassional changes - it was created as an experimental hobby project and isn't expected to be used in real-world settings.


# java-chess-engine

## Usage
- `make build`: compiles all classes and packs them into executable `java-chess-engine.jar` with Main entrypoint. This is the standard version which functions as a UCI engine
- `make test`: similar to `make build`, but instead builds (and executes) `java-chess-engine-testing.jar`, which runs predefined self-tests instead.
- `make clean`: removes any extraneous files associated with compiliation.

When testing the engine using `make test`, the program will run some of its own various tests. You can configure which tests run exactly by modifying some constant variables in `SelfTest.java`.  With `make build`, the engine will function in UCI mode - this mode allows it to be used by 3rd party external programs, such as a chess GUI, which will handle the sending and receiving of data using commands. An executable archive compiled in UCI mode is not intended to be used by humans, therefore using it in a non-automated fashion may be confusing.
