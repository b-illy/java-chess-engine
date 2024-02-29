build:
	rm -rf classes
	@echo "[-] Compiling..."
	javac -d classes src/*.java || exit 1
	@echo "[-] Creating jar archive..."
	jar cvfe out.jar Main -C classes . || exit 1
run:
	rm -rf classes
	@echo "[-] Compiling..."
	javac -d classes src/*.java || exit 1
	@echo "[-] Creating jar archive..."
	jar cvfe out.jar Main -C classes . || exit 1
	@echo "[-] Executing jar..."
	java -jar out.jar
clean:
	rm -rf classes
	rm -f out.jar
