public class Main {
    // configure which tests are to be run here
    public final static boolean testMode = true;
    private final static boolean testFENLoading = false;
    private final static boolean testMoveMaking = false;
    private final static boolean testPositionCounts = true;
    private final static boolean testPlaySelf = false;

    private final static long countPositions(int depth, Board pos) {
        if (depth == 0) return 1;

        int numPositions = 0;
        for (Move m : pos.getLegalMoves()) {
            numPositions += countPositions(depth - 1, m.simulate());
        }

        return numPositions;
    }

    public static void main(String[] args) throws InterruptedException {
        if (testMode) {
            System.out.println("--> NOTICE: Test mode ACTIVE");
            Board testBoard = new Board();

            for (int i = 0; i < 8; i++) {
                for (int j = 0; j < 8; j++) {
                    Piece p = testBoard.pieceAt(i, j);
                    System.out.println(new Coord(i,j) + ": " + p.getColour() + " " + p.getType());
                }
            }

            if (testFENLoading) {
                System.out.println("--> Testing FEN loading and printing");

                System.out.println("\nDefault board");
                testBoard.print();

                String[] testFENs = {
                    "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1", // normal start pos
                    "8/5k2/3p4/1p1Pp2p/pP2Pp1P/P4P1K/8/8 b - - 99 40",
                    "rnQ2bn1/p2pp1p1/8/8/3PN1p1/1P6/4B2P/2R1K2R w - - 0 0",
                    "rnbq1bnr/ppppkppp/8/4p3/4P3/8/PPPPKPPP/RNBQ1BNR w - - 2 3",
                    "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/R2QKBNR w KQkq - 0 1", // white missing k+b
                    "rnb1kbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1", // black missing q
                    "rnbqkbnr/ppppp2p/5p2/6p1/3PP3/8/PPP2PPP/RNBQKBNR w KQkq - 0 3" // fools mate (m1)
                };

                for (int i = 0; i < testFENs.length; i++) {
                    System.out.println("\nTest FEN #" + (i + 1));
                    if (!testBoard.loadFEN(testFENs[i])) {
                        System.out.println("ERROR: failed to load");
                    } else {
                        testBoard.print();
                        System.out.println("Legal moves in this position: " + testBoard.getLegalMoveCount());
                        // make sure that the fens given by getFEN() are accurate
                        if (testFENs[i].equals(testBoard.getFEN())) {
                            System.out.println("Regenerated FEN matches with loaded FEN");
                        } else {
                            System.out.println("ERROR: FEN mismatch, returned: " + testBoard.getFEN());
                        }

                        // evaluation testing
                        long startTime = System.nanoTime();
                        SearchThread st = new SearchThread(testBoard, 0, (short)0);
                        st.start();
                        try {
                            st.join();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                            break;
                        }
                        System.out.println("Evaluation: " + st.getEval());
                        System.out.println("Best move: " + st.getBestMove());
                        System.out.println("Time taken (ms): " + ((System.nanoTime() - startTime)/1000000));
                    }
                }
            }


            if (testMoveMaking) {
                System.out.println("\n\n--> Testing moves");

                // each move here corresponds to an initial position from testFENs
                // long form algebraic list (e.g. e1e2)
                // e1g1 (white short castling), e7e8q (for promotion)
                String[][] testMoves = {
                    {"a2a3", "a7a6", "h2h3", "h7h6"},  // edge pawn 1 tile moves
                    {"a2a4", "h7h6", "a4a5", "b7b5", "a5b6"},  // en passant
                    {"e2e4", "e7e5", "g1f3", "b8c6", "f1c4", "f8c5", "e1g1", "g8f6", "d2d3", "e8g8"}  // italian line w/ double castling
                };

                for (int i = 0; i < testMoves.length; i++) {
                    if (!testBoard.loadFEN()) {
                        System.out.println("couldnt load fen");
                    };

                    for (int j = 0; j < testMoves[i].length; j++) {
                        System.out.println("\nTest move " + (i + 1) + "." + (j+1));
                        
                        Move move1 = MoveFactory.fromLongAlgebraicStr(testMoves[i][j], testBoard);

                        System.out.print(move1 + " - from (");
                        System.out.print(move1.getPiece().getCoord().getX() + ",");
                        System.out.print(move1.getPiece().getCoord().getY() + ") to (");
                        System.out.print(move1.getCoord().getX() + ",");
                        System.out.print(move1.getCoord().getY() + ")\n");

                        System.out.println("Legality check " + (move1.isLegal() ? "passed" : "failed"));
                        move1.make();
                        testBoard.print();
                    }
                }

                System.out.print("\n--> Testing move parsing\n");

                // move parsing test (in given position)
                String[][] testMovesParsing = {
                    {"8/7P/2k5/8/8/2K5/8/8 w - - 0 1", "h7h8q"},
                    {"8/7P/2k5/8/8/2K5/8/8 w - - 0 1", "h7h8r"},
                    {"8/7P/2k5/8/8/2K5/8/8 w - - 0 1", "h7h8b"},
                    {"8/7P/2k5/8/8/2K5/8/8 w - - 0 1", "h7h8n"}
                };

                for (int i = 0; i < testMovesParsing.length; i++) {
                    System.out.print("Testing move parsing, test #" + (i+1) + ": " );
                    testBoard = new Board(testMovesParsing[i][0]);
                    Move m = MoveFactory.fromLongAlgebraicStr(testMovesParsing[i][1], testBoard);
                    System.out.print(m + ", type: " + m.getType() + ", promotype: " + m.getPromoType());
                    System.out.print("\n");
                }
            }

            
            if (testPositionCounts) {
                // provided by https://www.chessprogramming.org/Perft_Results, with reference output
                final String[] perftTestPositions = {
                    "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1", // start pos
                    "r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq -", // 'kiwipete'
                    "8/2p5/3p4/KP5r/1R3p1k/8/4P1P1/8 w - -", 
                    "r3k2r/Pppp1ppp/1b3nbN/nP6/BBP1P3/q4N2/Pp1P2PP/R2Q1RK1 w kq - 0 1",
                    "rnbq1k1r/pp1Pbppp/2p5/8/2B5/8/PPP1NnPP/RNBQK2R w KQ - 1 8" // very tricky
                };

                testBoard.loadFEN(perftTestPositions[4]);
                
                // count num positions
                for (int i = 0; i <= 5; i++) {
                    System.out.println(i + " ply: " + countPositions(i, testBoard));
                }
            }
            
            if (testPlaySelf) {
                testBoard.loadFEN(); // load starting position again
                
                // play a game against itself
                System.out.println("\n\n--> Attempting to play a game against self\n");

                final long gameTimePerSideMs = 60000;
                final int incrementMs = 000;

                long[] timeUsedMs = {0,0};

                while (testBoard.getGameState() == GameState.Ongoing) {
                    long startTime = System.nanoTime();

                    // track time usage per side
                    int index = 0;
                    if (testBoard.getSideToMove() == Colour.Black) index = 1;

                    // spawn new SearchThread with fixed depth
                    // SearchThread st = new SearchThread(testBoard, (int)4);
                    SearchThread st = new SearchThread(testBoard, gameTimePerSideMs-timeUsedMs[0], gameTimePerSideMs-timeUsedMs[1], incrementMs, incrementMs);
                    st.start();
                    st.join();
                    st.getBestMove().make();

                    timeUsedMs[index] += (System.nanoTime() - startTime)/1000000;
                    timeUsedMs[index] -= incrementMs;

                    // print board and some info
                    System.out.println("");
                    testBoard.print();
                    System.out.println("Eval: " + st.getEval());
                    System.out.println("Time taken (ms): " + ((System.nanoTime() - startTime)/1000000));
                    System.out.println("Max depth: " + st.getMaxDepthReached());
                    System.out.println("Move: " + st.getBestMove() + " (move no. " + testBoard.getMoveNumber() + ")");
                    System.out.println("Time left (ms): " + (gameTimePerSideMs-timeUsedMs[0]) + ", " + (gameTimePerSideMs-timeUsedMs[1]));
                    System.out.println("");
                }

                // game now complete
                System.out.println("\nGame over. Move history:");
                int printingMoveNum = 1;
                boolean whiteMove = true;
                for (Move m : testBoard.getMoveHistory()) {
                    if (whiteMove) {
                        System.out.print(printingMoveNum + ". ");
                    }

                    System.out.print(m);

                    if (!whiteMove) {
                        System.out.print("\n");
                        printingMoveNum++;
                    } else {
                        System.out.print(" ");
                    }

                    whiteMove = !whiteMove;
                }
            }
        } else {
            // test mode off, run in uci mode

            UCIThread uci = new UCIThread();
            uci.start();
            uci.join();
        }
    }
}
