import java.util.Date;

public class SearchThread extends Thread {
    // can be set to true to send a stop signal, as soon as thats detected we stop searching
    boolean stopSignal;

    // time tracking variables - create goal search time, stop searching automatically when reached
    int myTimeLeftMs;
    int oppTimeLeftMs;
    Date timeStarted;

    Board rootPos;

    Move bestMove;
    Evaluation bestEval;

    public SearchThread(Board rootPos, int myTimeLeftMs, int oppTimeLeftMs) {
        this.stopSignal = false;
        this.rootPos = rootPos;
        this.myTimeLeftMs = myTimeLeftMs;
        this.oppTimeLeftMs = oppTimeLeftMs;
        // note: for the time being, oppTimeLeftMs is ignored with the assumption that
        // this engine will only be playing other engines, which generally are not supposed
        // to think on their opponents time. in adapting this engine for better performance
        // against humans, however, you would not want to ignore this, so it remains a stub.
    }

    public void run() {
        // date object used to keep track of how much time has elapsed
        this.timeStarted = new Date();
        
        // take an educated (very approximate) guess at how many more moves we will have to make this game
        // to be safe, this guess is quite conservative i.e. most likely bigger than it needs to be
        int estMovesLeft = 50;
        if (this.rootPos.getMoveNumber() > 30) estMovesLeft -= (this.rootPos.getMoveNumber() - 30) / 2;
        estMovesLeft = Math.max(20, estMovesLeft);  // make sure this doesnt drop below a certain threshold
        
        // create a reasonable search time goal to aim for, e.g. 100s and 40 moves left, use 100/40 = 2.5s for this move
        int goalTimeMs = myTimeLeftMs / estMovesLeft;
        // TODO: detect if this game is being played with increment and account for that

        // TODO
    }

    // reference: https://www.youtube.com/watch?v=l-hh51ncgDI
    private Evaluation minimax(Board pos, int depth, boolean max) {
        return minimax(pos, depth, max, new Evaluation(Colour.Black), new Evaluation(Colour.White));
    }

    private Evaluation minimax(Board pos, int depth, boolean max, Evaluation alpha, Evaluation beta) {
        if (depth == 0 || pos.getGameState() != GameState.Ongoing) {
            return HeuristicEval.evaluate(pos);
        }

        if (max) {
            Evaluation maxEval = new Evaluation(Colour.Black);
            for (Move m : pos.getLegalMoves()) {
                Evaluation eval = minimax(m.simulate(), depth-1, false, alpha, beta);
                if (eval.toLong() > maxEval.toLong()) maxEval = eval;
                if (eval.toLong() > alpha.toLong()) alpha = eval;
                if (beta.toLong() <= alpha.toLong()) {
                    break;
                }
            }
            return maxEval;
        } else {
            Evaluation minEval = new Evaluation(Colour.White);
            for (Move m : pos.getLegalMoves()) {
                Evaluation eval = minimax(m.simulate(), depth-1, true, alpha, beta);
                if (eval.toLong() < minEval.toLong()) minEval = eval;
                if (eval.toLong() < beta.toLong()) beta = eval;
                if (beta.toLong() <= alpha.toLong()) {
                    break;
                }
            }
            return minEval;
        }
    }

    public void sendStopSignal() {
        this.stopSignal = true;
    }
}