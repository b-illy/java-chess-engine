public class HeuristicEval {
    public static Evaluation evaluate(Board position) {
        long centipawns = 0;

        // STAGE 1: check for game over
        GameState state = position.getGameState();
        if (state == GameState.WhiteWon) {
            return new Evaluation(Colour.White);
        } else if (state == GameState.BlackWon) {
            return new Evaluation(Colour.Black);
        } else if (state == GameState.Draw) {
            return new Evaluation(Colour.None);
        }

        // STAGE 2: check for forced mate
        // TODO

        long totalMaterialValue = 0;
        int numPiecesOnBoard = 0;
        // STAGE 3: add up raw piece values based on constants
        for (int i = 0; i < 8; i ++) {
            for (int j = 0; j < 8; j++) {
                Piece p = position.pieceAt(i, j);
                double value = 0;
                switch(p.getType()) {
                    case pawn:
                        value = Constants.VALUE_PAWN;
                        numPiecesOnBoard++;
                        break;
                    case knight:
                        value = Constants.VALUE_KNIGHT;
                        numPiecesOnBoard++;
                        break;
                    case bishop:
                        value = Constants.VALUE_BISHOP;
                        numPiecesOnBoard++;
                        break;
                    case rook:
                        value = Constants.VALUE_ROOK;
                        numPiecesOnBoard++;
                        break;
                    case queen:
                        value = Constants.VALUE_QUEEN;
                        numPiecesOnBoard++;
                        break;
                    case king: case empty: default:
                        break;
                }
                
                totalMaterialValue += value; // track total amt of material on the board
                if (p.getColour() == Colour.White) {
                    centipawns += value;
                } else if (p.getColour() == Colour.Black) {
                    centipawns -= value;
                }
            }
        }

        // check for doubled (or more) pawns
        int doubledPawns[] = new int[16];  // first 8 represent white, last 8 black
        for (int j = 0; j < 8; j++) {
            int pawnsWhiteHere = 0;
            int pawnsBlackHere = 0;
            for (int i = 0; i < 8; i++) {
                if (position.pieceAt(i, j).getType() == PieceType.pawn) {
                    switch (position.pieceAt(i, j).getColour()) {
                        case White:
                            pawnsWhiteHere++;
                            break;
                        case Black:
                            pawnsBlackHere++;
                            break;
                        case None: default:
                            break;
                    }
                }
            }
            doubledPawns[j] = pawnsWhiteHere;
            doubledPawns[j+8] = pawnsBlackHere;
        }

        // apply penalties for doubled (or more) pawns
        for (int i = 0; i < 16; i++) {
            long penalties = (long)(Math.pow(Math.max(0, doubledPawns[i]-1), 1.2));

            centipawns += Constants.EVAL_DOUBLED_PAWN_PENALTY * penalties * (i>7 ? -1 : 1);
        }

        // apply bonuses for number of unique squares 'controlled'
        // get all candidate moves for all pieces of both sides
        // TODO
        // for (int i = 0; i < 8; i++) {
        //     for (int j = 0; j < 8; j++) {

        //     }
        // }


        // give bonus for each pawn moved off starting rank
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                Piece p = position.pieceAt(i, j);
                if (p.getType() == PieceType.pawn) {
                    int distFromMid = (int)Math.floor(Math.abs(3.5 - p.getCoord().getX())); // 0-3
                    long scaledBonus = (long)(Constants.EVAL_DOUBLED_PAWN_PENALTY * (1 - distFromMid*0.2));
                    if (p.getColour() == Colour.White && p.getCoord().getY() != 2) {
                        centipawns += scaledBonus;
                    }
                    if (p.getColour() == Colour.White && p.getCoord().getY() != 2) {
                        centipawns -= scaledBonus;
                    }
                }
            }
        }


        // apply bonuses for number of legal moves
        // create new board which is equal but with opponent to move to get opponents potential legal moves
        Board oppMovePosition = new Board(position.getFEN());
        oppMovePosition.setEnPassantSquare(new Coord(-1, -1));
        oppMovePosition.incMoveCount();

        long currLegalMoveCount = position.getLegalMoveCount();
        long oppLegalMoveCount = oppMovePosition.getLegalMoveCount();
        long moveCountDiff = (currLegalMoveCount - oppLegalMoveCount) * (position.getSideToMove() == Colour.White ? 1 : -1);
        boolean moveCountInc = moveCountDiff >= 0;
        if (moveCountDiff >= 3) centipawns += (moveCountInc ? 1 : -1) * Math.max(Math.pow(Math.abs(moveCountDiff) * 0.02, 0.5), 50);
        

        // increase magnitude of eval based on number of pieces
        final int startPosPieceCount = 30;
        double pieceCountRatio = (double)Math.min(startPosPieceCount, numPiecesOnBoard) / (double)startPosPieceCount;
        centipawns /= Math.max(pieceCountRatio,0.4);
        
        // increase magnitude of eval based on total value of all pieces
        // (2 pawns in endgame are much more significant than 2 pawns in the opening)
        final long startPosMaterialValue = 4*Constants.VALUE_ROOK+4*Constants.VALUE_KNIGHT+4*Constants.VALUE_BISHOP+2*Constants.VALUE_QUEEN+16*Constants.VALUE_PAWN;
        double materialValueRatio = (double)Math.min(startPosMaterialValue, totalMaterialValue) / (double)startPosMaterialValue;
        centipawns /= Math.max(materialValueRatio,0.2);
        
        // increase magnitude of eval based on move count
        if (position.getMoveNumber() > Constants.EVAL_HIGH_MOVE_COUNT) {
            centipawns *= 1 + Math.min(0.5, (position.getMoveNumber()-20)*0.01);
        }

        // drop magnitude of eval if halfmove count is getting too high
        if (position.getHalfMoveNumber() > Constants.EVAL_HIGH_HALFMOVE_COUNT) {
            centipawns *= 1 - Math.min(0.5, position.getHalfMoveNumber());
        }

        return new Evaluation(centipawns);
    }
}
