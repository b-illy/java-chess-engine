import java.util.Date;

public class SearchThread extends Thread {
    // can be set to true to send a stop signal, as soon as thats detected we stop searching
    boolean stopSignal;

    // time tracking variables - create goal search time, stop searching automatically when reached
    long myTimeLeftMs;
    long oppTimeLeftMs;
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
        long goalTimeMs = myTimeLeftMs / estMovesLeft;
        // TODO: detect if this game is being played with increment and account for that

        // setup parameters used to decide when to stop
        long lastIterTimeMs = 0;
        long passedTimeMs = 0;
        int idsDepth = 0;

        // do iterative deepening search using underlying minimax function
        while (idsDepth <= Constants.MAX_MINIMAX_DEPTH && !stopSignal) {
            Date iterStartDate = new Date();
            minimax(this.rootPos, idsDepth, this.rootPos.getSideToMove() == Colour.White);
            lastIterTimeMs = (new Date()).getTime() - iterStartDate.getTime();
            passedTimeMs += lastIterTimeMs;

            // stop searching if we've taken longer than goal time or are too close to continue
            if (passedTimeMs + 4*lastIterTimeMs >= goalTimeMs) {
                stopSignal = true;
                break;
            }

            idsDepth++;
        }
    }

    // minimax reference: https://www.youtube.com/watch?v=l-hh51ncgDI

    // wrapper function with minimal arguments
    private Evaluation minimax(Board pos, int depth, boolean max) {
        return minimax(pos, depth, true, max, new Evaluation(Colour.Black), new Evaluation(Colour.White));
    }

    // main minimax function
    private Evaluation minimax(Board pos, int depth, boolean isRoot, boolean max, Evaluation alpha, Evaluation beta) {
        // do not keep searching if stop signal was detected, just return placeholder eval to get ignored
        if (stopSignal) {
            return max ? new Evaluation(Colour.Black) : new Evaluation(Colour.White);
        }

        if (depth == 0 || pos.getGameState() != GameState.Ongoing) {
            return HeuristicEval.evaluate(pos);
        }

        Evaluation bestEvalHere;
        Move bestMoveHere = null;

        if (max) {
            bestEvalHere = new Evaluation(Colour.Black); // track maximum
            for (Move m : pos.getLegalMoves()) {
                Evaluation eval = minimax(m.simulate(), depth-1, false, false, alpha, beta);
                if (eval.toLong() > bestEvalHere.toLong()) {
                    bestEvalHere = eval;
                    bestMoveHere = m;
                }
                if (eval.toLong() > alpha.toLong()) alpha = eval;
                if (beta.toLong() <= alpha.toLong()) {
                    break;
                }
            }
        } else {
            bestEvalHere = new Evaluation(Colour.White); // track minimum
            for (Move m : pos.getLegalMoves()) {
                Evaluation eval = minimax(m.simulate(), depth-1, false, true, alpha, beta);
                if (eval.toLong() < bestEvalHere.toLong()) {
                    bestEvalHere = eval;
                    bestMoveHere = m;
                }
                if (eval.toLong() < beta.toLong()) beta = eval;
                if (beta.toLong() <= alpha.toLong()) {
                    break;
                }
            }
        }

        // to be executed on the head / root pos (for this minimax search) only
        if (isRoot) {
            this.bestEval = bestEvalHere;
            this.bestMove = bestMoveHere;
        }

        return bestEvalHere;
    }

    public void sendStopSignal() {
        this.stopSignal = true;
    }

    public Evaluation getBestEval() {
        return this.bestEval;
    }

    public Move getBestMove() {
        return this.bestMove;
    }
}