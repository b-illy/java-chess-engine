public class Main {
    private final static boolean testMode = true;
    public static void main(String[] args) {
        if (testMode) {
            System.out.println("--> NOTICE: Test mode ACTIVE");
            System.out.println("--> Testing FEN loading and printing");
            Board testBoard = new Board();

            System.out.println("\nDefault board");
            testBoard.print();

            String[] testFENs = {
                "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
                "8/5k2/3p4/1p1Pp2p/pP2Pp1P/P4P1K/8/8 b - - 99 50",
                "rnQ2bn1/p2pp1p1/8/8/3PN1p1/1P6/4B2P/2R1K2R w - - 0 0",
                "rnbq1bnr/ppppkppp/8/4p3/4P3/8/PPPPKPPP/RNBQ1BNR w - - 2 3"
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
                }
            }

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
        }

    }
}