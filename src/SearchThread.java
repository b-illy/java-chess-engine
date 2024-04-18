import java.util.ArrayList;

public class SearchThread extends Thread {
    // can be set to true to send a stop signal, as soon as thats detected we stop searching
    boolean stopSignal;

    // 0=normal, 1=depth, 2=nodes, 3=movetime, 4=infinite
    short mode;

    // ambigious purpose variable used according to mode
    long value;

    // time
    long wtime;
    long btime;
    long winc;
    long binc;

    // position to start search tree from
    Board rootPos;

    Move bestMove;
    Evaluation eval;
    int maxDepthReached;


    // normal mode
    public SearchThread(Board rootPos, long wtime, long btime, long winc, long binc) {
        this.stopSignal = false;
        this.rootPos = rootPos;
        this.mode = 0;

        this.wtime = wtime;
        this.btime = btime;
        this.winc = winc;
        this.binc = binc;
    }

    public SearchThread(Board rootPos, long value, short submode) {
        this.stopSignal = false;
        this.rootPos = rootPos;
        this.value = Math.max(0, value);

        switch(submode) {
            case 0:
                // search with fixed depth goal
                this.mode = 1;
                break;
            case 1:
                // search with fixed node count goal
                this.mode = 2;
                break;
            case 2:
                // search with fixed / precalculated goal time to take
                this.mode = 3;
                break;
                
            default:
                throw new ExceptionInInitializerError("unrecognised search thread mode");
        }
    }

    // infinite search
    public SearchThread(Board rootPos) {
        this.stopSignal = false;
        this.rootPos = rootPos;
        this.mode = 4;
    }

    public void run() {
        // set some placeholder values before real ones calculated
        if (this.rootPos.getLegalMoveCount() != 0) this.bestMove = this.rootPos.getLegalMoves().get(0);
        this.eval = new Evaluation(0);
        this.maxDepthReached = 0;


        // handle time-based stuff if in one of the relevant modes
        if (mode == 0 || mode == 3) {
            long myTimeMs;
            long myIncMs;
            long goalTimeMs = 0;

            // calculate a reasonable goal time using time left and move count if in mode0(normal)
            if (mode == 0) {
                // figure out which time and increment are our's
                if (this.rootPos.getSideToMove() == Colour.White) {
                    myTimeMs = wtime;
                    myIncMs = winc;
                } else {
                    myTimeMs = btime;
                    myIncMs = binc;
                }

                // take an educated (very approximate) guess at how many more moves we will have to make this game
                // to be safe, this guess is quite conservative i.e. most likely bigger than it needs to be
                int estMovesLeft = 40;
                if (this.rootPos.getMoveNumber() > 20) estMovesLeft -= (this.rootPos.getMoveNumber() - 20) / (3/2);
                estMovesLeft = Math.max(10, estMovesLeft);  // make sure this doesnt drop below a certain threshold
                
                // create a reasonable search time goal to aim for, e.g. 100s and 40 moves left, use 100/40 = 2.5s for this move
                // also account for increment if applicable
                if (myIncMs > 0) goalTimeMs = (myTimeMs + (estMovesLeft * myIncMs)/2) / estMovesLeft;
                else goalTimeMs = myTimeMs / estMovesLeft;
            }

            // set goaltime if in mode3(goaltime)
            if (mode == 3) {
                goalTimeMs = value;
            }

            // setup parameters used to decide when to stop
            long lastIterTimeMs = 0;
            long passedTimeMs = 0;
            int idsDepth = 0;

            // do iterative deepening search using underlying minimax function
            // decides when to stop going deeper based on goal time
            while (idsDepth <= Constants.MAX_MINIMAX_DEPTH && !stopSignal) {
                long iterStartTime = System.nanoTime();
                minimax(this.rootPos, idsDepth, this.rootPos.getSideToMove() == Colour.White);
                lastIterTimeMs = (System.nanoTime() - iterStartTime)/1000000;
                passedTimeMs += lastIterTimeMs;

                // stop searching if we've taken longer than goal time or are too close to continue
                if (passedTimeMs + 2*lastIterTimeMs >= goalTimeMs) {
                    stopSignal = true;
                    break;
                }

                this.maxDepthReached = idsDepth;
                idsDepth++;
            }
        }
        
        if (mode == 1 || mode == 4) { // fixed depth mode OR infinite search mode
            long goalDepth = this.value;
            if (mode == 4) goalDepth = Long.MAX_VALUE;

            // basic fixed depth approach (but still ids)
            for (int i = 0; i <= goalDepth; i++) {
                if (stopSignal) break;
                minimax(this.rootPos, i, this.rootPos.getSideToMove() == Colour.White);
                // System.out.println("Completed search to depth " + i + "/" + goalDepth);

                this.maxDepthReached = i;
            }
        }

        if (mode == 2) { // fixed node count mode
            // TODO
        }


        // if the bottom of this method is reached, everything is finished.
        // set the stop signal just to make this clear
        stopSignal = true;
    }

    // minimax reference: https://www.youtube.com/watch?v=l-hh51ncgDI

    // wrapper function with minimal arguments
    private Evaluation minimax(Board pos, int depth, boolean max) {
        return minimax(pos, depth, true, max, new Evaluation(Colour.Black), new Evaluation(Colour.White));
    }

    private Evaluation minimaxCaptures(Board pos, boolean max, Evaluation alpha, Evaluation beta) {
        final Evaluation currentStaticEval = HeuristicEval.evaluate(pos);
        
        // update pruning params with static eval
        if (max) {
            if (currentStaticEval.toLong() >= beta.toLong()) return beta;
            if (currentStaticEval.toLong() >= alpha.toLong()) alpha = currentStaticEval;
        } else {
            if (currentStaticEval.toLong() <= alpha.toLong()) return alpha;
            if (currentStaticEval.toLong() <= beta.toLong()) beta = currentStaticEval;
        }
        
        // filter for only capturing moves
        ArrayList<Move> moves = pos.getLegalMoves();
        ArrayList<Move> capturingMoves = new ArrayList<Move>();
        for (Move m : moves) {
            if (m.getType() == MoveType.enPassant) {
                capturingMoves.add(m);
            } else if (pos.pieceAt(m.getCoord()).getType() != PieceType.empty && m.getType() != MoveType.castling) {
                capturingMoves.add(m);
            }
        }
        
        // no capturing moves to check, pos is 'quiet', just return this basic eval
        if (capturingMoves.size() == 0) return currentStaticEval;

        // order moves in a more optimal way
        capturingMoves = MoveOrdering.reorder(capturingMoves);

        Evaluation bestEvalHere = currentStaticEval;

        // there are some capturing moves here, check them all recursively until quiet pos found
        if (max) {
            for (Move m : capturingMoves) {
                Evaluation eval = minimaxCaptures(m.simulate(), false, alpha, beta);

                if (eval.toLong() >= beta.toLong()) return beta;
                if (eval.toLong() > bestEvalHere.toLong()) bestEvalHere = eval; // max
                if (eval.toLong() > alpha.toLong()) alpha = eval;
                if (beta.toLong() <= alpha.toLong()) break;
            }
        } else {
            for (Move m : capturingMoves) {
                Evaluation eval = minimaxCaptures(m.simulate(), true, alpha, beta);

                if (eval.toLong() <= alpha.toLong()) return alpha;
                if (eval.toLong() < bestEvalHere.toLong()) bestEvalHere = eval; // min
                if (eval.toLong() < beta.toLong()) beta = eval;
                if (beta.toLong() <= alpha.toLong()) break;
            }
        }

        return bestEvalHere.tick();
    }

    // main minimax function
    private Evaluation minimax(Board pos, int depth, boolean isRoot, boolean max, Evaluation alpha, Evaluation beta) {
        // do not keep searching if stop signal was detected, just return placeholder eval to get ignored
        if (stopSignal) {
            return max ? new Evaluation(Colour.Black) : new Evaluation(Colour.White);
        }

        if (pos.getGameState() != GameState.Ongoing) {
            return HeuristicEval.evaluate(pos);
        }

        if (depth == 0) {
            return minimaxCaptures(pos, max, alpha, beta);
        }
        
        ArrayList<Move> legalMoves = pos.getLegalMoves();
        // order moves in a more optimal way
        legalMoves = MoveOrdering.reorder(legalMoves);

        Evaluation bestEvalHere;
        Move bestMoveHere = legalMoves.get(0);

        if (max) {
            bestEvalHere = new Evaluation(Colour.Black); // track maximum
            for (Move m : legalMoves) {
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
            for (Move m : legalMoves) {
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

        // if the best position after next move is 1-0, this is +M1, if +M1 then +M2 etc
        bestEvalHere.tick();

        // to be executed on the head / root pos (for this minimax search) only
        if (isRoot) {
            this.eval = bestEvalHere;
            this.bestMove = bestMoveHere;
        }

        return bestEvalHere;
    }

    public void sendStopSignal() {
        this.stopSignal = true;
    }

    public Evaluation getEval() {
        return this.eval;
    }

    public Move getBestMove() {
        return this.bestMove;
    }

    public int getMaxDepthReached() {
        return this.maxDepthReached;
    }
}