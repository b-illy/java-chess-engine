public class UCIBackgroundThread extends Thread {
    private SearchThread st;

    public UCIBackgroundThread(SearchThread st) {
        this.st = st;
    }


    // this class wraps a SearchThread, starting it and sending bestmove uci command once complete
    public void run() {
        st.start();

        try {
            st.join();
            System.out.println("bestmove " + st.getBestMove());
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
