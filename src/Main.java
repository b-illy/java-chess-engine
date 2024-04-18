public class Main {
    public static void main(String[] args) throws InterruptedException {
        // test mode off, run in uci mode

        UCIThread uci = new UCIThread();
        uci.start();
        uci.join();
    }
}
