import java.util.ArrayList;

public class SearchThread extends Thread {
    // can be set to true to send a stop signal, as soon as thats detected we stop searching
    boolean stopSignal;

    // time tracking variables - create goal search time, stop searching automatically when reached
    long myTimeLeftMs;
    long oppTimeLeftMs;
    long incrementMs;
    long goalTimeMs;
    int goalDepth;

    Board rootPos;

    Move bestMove;
    Evaluation eval;
    int maxDepthReached;

    public SearchThread(Board rootPos, long myTimeLeftMs, long oppTimeLeftMs, long incrementMs) {
        this.stopSignal = false;
        this.myTimeLeftMs = myTimeLeftMs;
        this.oppTimeLeftMs = oppTimeLeftMs;
        this.incrementMs = incrementMs;
        this.goalTimeMs = -1;
        this.goalDepth = -1;
        this.rootPos = rootPos;
        // note: for the time being, oppTimeLeftMs is ignored with the assumption that
        // this engine will only be playing other engines, which generally are not supposed
        // to think on their opponents time. in adapting this engine for better performance
        // against humans, however, you would not want to ignore this, so it remains a stub.
    }

    // search with fixed / precalculated goal time to take
    public SearchThread(Board rootPos, long goalTimeMs) {
        this.stopSignal = false;
        this.goalTimeMs = Math.max(0, goalTimeMs);
        this.goalDepth = -1;
        this.rootPos = rootPos;
    }

    // search with fixed depth goal
    public SearchThread(Board rootPos, int goalDepth) {
        this.stopSignal = false;
        this.goalTimeMs = -1;
        this.goalDepth = Math.max(0, goalDepth);
        this.rootPos = rootPos;
    }

    public void run() {
        // give placeholder best move if possible (first legal move found)
        if (this.rootPos.getLegalMoveCount() != 0) this.bestMove = this.rootPos.getLegalMoves().get(0);
        // give placeholder eval (equal)
        this.eval = new Evaluation(0);

        // calculate a reasonable goal time if none is provided using time left and move count
        if (this.goalTimeMs < 0) {
            // take an educated (very approximate) guess at how many more moves we will have to make this game
            // to be safe, this guess is quite conservative i.e. most likely bigger than it needs to be
            int estMovesLeft = 50;
            if (this.rootPos.getMoveNumber() > 30) estMovesLeft -= (this.rootPos.getMoveNumber() - 30) / 2;
            estMovesLeft = Math.max(20, estMovesLeft);  // make sure this doesnt drop below a certain threshold
            
            // create a reasonable search time goal to aim for, e.g. 100s and 40 moves left, use 100/40 = 2.5s for this move
            // also account for increment if applicable
            if (this.incrementMs > 0) this.goalTimeMs = (myTimeLeftMs + (estMovesLeft*this.incrementMs)/2) / estMovesLeft;
            else this.goalTimeMs = myTimeLeftMs / estMovesLeft;
        }

        // setup parameters used to decide when to stop
        long lastIterTimeMs = 0;
        long passedTimeMs = 0;
        int idsDepth = 0;

        this.maxDepthReached = 0;

        if (this.goalDepth < 0) {
            // do iterative deepening search using underlying minimax function
            // decides when to stop going deeper based on goal time
            while (idsDepth <= Constants.MAX_MINIMAX_DEPTH && !stopSignal) {
                long iterStartTime = System.nanoTime();
                minimax(this.rootPos, idsDepth, this.rootPos.getSideToMove() == Colour.White);
                lastIterTimeMs = (System.nanoTime() - iterStartTime)/1000000;
                passedTimeMs += lastIterTimeMs;

                // stop searching if we've taken longer than goal time or are too close to continue
                if (passedTimeMs + 4*lastIterTimeMs >= this.goalTimeMs) {
                    stopSignal = true;
                    break;
                }

                this.maxDepthReached = idsDepth;
                idsDepth++;
            }
        } else {
            // basic fixed depth approach (but still ids)
            for (int i = 0; i <= this.goalDepth; i++) {
                if (stopSignal) break;
                minimax(this.rootPos, i, this.rootPos.getSideToMove() == Colour.White);
                System.out.println("Completed search to depth " + i + "/" + this.goalDepth);

                this.maxDepthReached = i;
            }
            stopSignal = true;
        }
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