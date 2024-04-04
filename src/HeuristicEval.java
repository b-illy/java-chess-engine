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

        // STAGE 3: add up raw piece values based on constants
        for (int i = 0; i < 8; i ++) {
            for (int j = 0; j < 8; j++) {
                Piece p = position.pieceAt(i, j);
                double value = 0;
                switch(p.getType()) {
                    case pawn:
                        value = Constants.VALUE_PAWN;
                        break;
                    case knight:
                        value = Constants.VALUE_KNIGHT;
                        break;
                    case bishop:
                        value = Constants.VALUE_BISHOP;
                        break;
                    case rook:
                        value = Constants.VALUE_ROOK;
                        break;
                    case queen:
                        value = Constants.VALUE_QUEEN;
                        break;
                    case king: case empty: default:
                        break;
                }
                
                if (p.getColour() == Colour.White) {
                    centipawns += value;
                } else if (p.getColour() == Colour.Black) {
                    centipawns -= value;
                }
            }
        }

        // TODO: remove placeholder
        return new Evaluation(0);
    }
}
