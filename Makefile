build:
	rm -rf classes
	@echo "[-] Compiling..."
	javac -d classes src/*.java || exit 1
	@echo "[-] Creating jar archive..."
	jar cfe java-chess-engine.jar Main -C classes . || exit 1
	@echo "[-] Done"
run:
	rm -rf classes
	@echo "[-] Compiling..."
	javac -d classes src/*.java || exit 1
	@echo "[-] Creating jar archive..."
	jar cfe java-chess-engine.jar Main -C classes . || exit 1
	@echo "[-] Executing jar..."
	java -jar java-chess-engine.jar
clean:
	rm -rf classes
	rm -f java-chess-engine.jar out.jar
	@echo "[-] Done"
