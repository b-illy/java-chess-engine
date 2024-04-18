test:
	rm -rf classes
	@echo "[-] Compiling classes..."
	javac -d classes src/*.java || exit 1
	@echo "[-] Creating test mode jar archive..."
	jar cfe java-chess-engine-testing.jar SelfTest -C classes . || exit 1
	@echo "[-] Executing jar..."
	java -jar java-chess-engine-testing.jar
build:
	rm -rf classes
	@echo "[-] Compiling classes..."
	javac -d classes src/*.java || exit 1
	@echo "[-] Creating UCI mode jar archive..."
	jar cfe java-chess-engine.jar Main -C classes . || exit 1
	@echo "[-] Done"
clean:
	rm -rf classes
	rm -f java-chess-engine.jar java-chess-engine-testing.jar out.jar
	@echo "[-] Done"
