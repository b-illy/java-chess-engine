public class HeuristicEval {
    private static int totalPositionsEvaluated = 0;

    // small helper function that queries the pst values, maps coords, and flips if necessary
    private static final int getPstValue(int x, int y, int[][] table, boolean flip) {
        // translate to this coordinate system, for details on how this works and why,
        // see the comments relating to Board.pieceAt()
        int i = 7-y;
        int j = x;
        if (flip) {
            // rotate the coordinate 180 deg
            i = 7-i;
            j = 7-j;
        }
        
        return table[i][j];
    }

    public static Evaluation evaluate(Board position) {
        // if we are running in self-test mode right now, show some debug info
        HeuristicEval.totalPositionsEvaluated++;
        if (SelfTest.testMode && HeuristicEval.totalPositionsEvaluated % 10000 == 0)
        System.out.println("Positions evaluated: " + HeuristicEval.totalPositionsEvaluated);


        long centipawns = 0;

        // check for game over
        GameState state = position.getGameState();

        if (state == GameState.WhiteWon) {
            return new Evaluation(Colour.White);
        } else if (state == GameState.BlackWon) {
            return new Evaluation(Colour.Black);
        } else if (state == GameState.Draw) {
            return new Evaluation(Colour.None);
        }


        long whiteOccupancy = Bitboards.whiteSquares(position.getBitboards());
        long blackOccupancy = Bitboards.blackSquares(position.getBitboards());
        
        // add up raw piece values based on constants
        long totalMaterialValue = 0;
        int numPiecesOnBoard = 0;
        for (int i = 0; i < 8; i ++) {
            for (int j = 0; j < 8; j++) {
                Piece p = position.pieceAt(i, j);
                int value = 0;
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
        

        // deduce which phase of the game we are in
        int gamePhase = 0; // early=0, mid=1, late=2
        if (position.getMoveNumber() > 15 || totalMaterialValue < 50 || numPiecesOnBoard < 20) gamePhase = 1;
        if (totalMaterialValue < 30 || numPiecesOnBoard < 10) gamePhase = 2;


        // add piece-square table values
        // (functions very similarly to above loop)
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                Piece p = position.pieceAt(i, j);
                int value = 0;
                switch(p.getType()) {
                    case pawn:
                        value = HeuristicEval.getPstValue(p.getCoord().getX(), p.getCoord().getY(), Constants.PST_PAWN, p.getColour()==Colour.Black);
                        break;
                    case knight:
                        value = HeuristicEval.getPstValue(p.getCoord().getX(), p.getCoord().getY(), Constants.PST_KNIGHT, p.getColour()==Colour.Black);
                        break;
                    case bishop:
                        value = HeuristicEval.getPstValue(p.getCoord().getX(), p.getCoord().getY(), Constants.PST_BISHOP, p.getColour()==Colour.Black);
                        break;
                    case rook:
                        value = HeuristicEval.getPstValue(p.getCoord().getX(), p.getCoord().getY(), Constants.PST_ROOK, p.getColour()==Colour.Black);
                        break;
                    case queen:
                        value = HeuristicEval.getPstValue(p.getCoord().getX(), p.getCoord().getY(), Constants.PST_QUEEN, p.getColour()==Colour.Black);
                        break;
                    case king: 
                        if (gamePhase == 0 || gamePhase == 1) {
                            value = HeuristicEval.getPstValue(p.getCoord().getX(), p.getCoord().getY(), Constants.PST_KING_EARLY, p.getColour()==Colour.Black);
                        } else if (gamePhase == 2) {
                            value = HeuristicEval.getPstValue(p.getCoord().getX(), p.getCoord().getY(), Constants.PST_KING_LATE, p.getColour()==Colour.Black);
                        }
                        break;
                    case empty: default:
                        break;
                }
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
            centipawns -= Constants.EVAL_DOUBLED_PAWN_PENALTY * penalties * (i>7 ? -1 : 1);
        }


        // detect passed pawns and give bonuses
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                Piece p = position.pieceAt(i, j);
                if (p.getType() != PieceType.pawn) continue;
                int index = Bitboards.toIndex(p.getCoord());

                // generate mask covering all squares that need to be pawnless for this to be a passer
                long passedPawnMask;
                if (p.getColour() == Colour.White) passedPawnMask = Bitboards.passedPawnMaskWhite(index);
                else passedPawnMask = Bitboards.passedPawnMaskBlack(index);

                long enemyPawns = position.getBitboards()[p.getColour()==Colour.White?0:6];

                if ((passedPawnMask & enemyPawns) == 0) {
                    // this must be a passed pawn, give bonus
                    if (p.getColour() == Colour.White) {
                        centipawns += Constants.EVAL_PASSED_PAWN_BONUSES[7 - (index/8)];
                    } else {
                        centipawns -= Constants.EVAL_PASSED_PAWN_BONUSES[index/8];
                    }
                }
            }
        }


        // apply bonuses for number of unique squares 'controlled'
        int squaresControlledDiff = 0;
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                if (position.isSquareAttacked(new Coord(i, j), Colour.White)) squaresControlledDiff++;
                else if (position.isSquareAttacked(new Coord(i, j), Colour.Black)) squaresControlledDiff--;
            }
        }

        centipawns += squaresControlledDiff * Constants.EVAL_CONTROLLED_SQUARE_BONUS;
        

        // increase magnitude of eval based on number of pieces
        final int startPosPieceCount = 30;
        double pieceCountRatio = (double)Math.min(startPosPieceCount, numPiecesOnBoard) / (double)startPosPieceCount;
        centipawns /= Math.max(pieceCountRatio,0.5);
        

        // increase magnitude of eval based on total value of all pieces
        // (2 pawns in endgame are much more significant than 2 pawns in the opening)
        final long startPosMaterialValue = 4*Constants.VALUE_ROOK+4*Constants.VALUE_KNIGHT+4*Constants.VALUE_BISHOP+2*Constants.VALUE_QUEEN+16*Constants.VALUE_PAWN;
        double materialValueRatio = (double)Math.min(startPosMaterialValue, totalMaterialValue) / (double)startPosMaterialValue;
        centipawns /= Math.max(materialValueRatio,0.3);
        

        // drop magnitude of eval if halfmove count is getting too high
        if (position.getHalfMoveNumber() > Constants.EVAL_HIGH_HALFMOVE_COUNT) {
            centipawns *= 1 - Math.min(0.5, 0.03*(Constants.EVAL_HIGH_HALFMOVE_COUNT-position.getHalfMoveNumber()));
        }

        return new Evaluation(centipawns);
    }
}
