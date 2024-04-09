import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

public class MoveOrdering {
    private final static class MoveWeightPair {
        private final Move move;
        private final int weight;

        public MoveWeightPair(Move move, int weight) {
            this.move = move;
            this.weight = weight;
        }

        public Move getMove() { return this.move; }
        public int getWeight() { return this.weight; }
    }

    public static ArrayList<Move> reorder(ArrayList<Move> moves) {
        int[] movesWeights = new int[moves.size()];

        for (int i = 0; i < moves.size(); i++) {
            // calculate a priority weighting for each move
            int weight = 0;

            Move m = moves.get(i);
            Board board = m.getBoard();

            Piece piece = m.getPiece();
            Piece capPiece = board.pieceAt(m.getCoord());

            // give weight for checks
            if (m.simulate().isCheck()) weight += 200;

            // detect capturing move
            if ((capPiece.getType() != PieceType.empty || m.getType() == MoveType.enPassant) && m.getType() != MoveType.castling) {
                // always add some amount of weight to any capture
                weight += 100; 

                // add weight depending on piece value vs captured piece value
                final Piece[] piecePair = {piece, capPiece};
                int[] pieceValuePair    = {0,     0};
                for (int j = 0; j < 2; j++) {
                    switch(piecePair[j].getType()) {
                        case pawn:
                            pieceValuePair[j] = Constants.VALUE_PAWN;
                            break;
                        case knight:
                            pieceValuePair[j] = Constants.VALUE_KNIGHT;
                            break;
                        case bishop:
                            pieceValuePair[j] = Constants.VALUE_BISHOP;
                            break;
                        case rook:
                            pieceValuePair[j] = Constants.VALUE_ROOK;
                            break;
                        case queen:
                            pieceValuePair[j] = Constants.VALUE_QUEEN;
                            break;
                        case king: case empty: default:
                            break;
                    }
                }

                // give weight for capturing a piece of higher value with one of lower value
                weight += pieceValuePair[1] - pieceValuePair[0];
            }

            // give weight for pawn pushes
            if (piece.getType() == PieceType.pawn) {
                weight += 20;
                // extra weight for pushing 2 squares
                if (Math.abs(piece.getCoord().getY() - m.getCoord().getY()) == 2) weight += 10;
            }


            movesWeights[i] = weight;
        }

        // combine moves and their weights into pairs in one array using custom class
        MoveWeightPair[] pairArray = new MoveWeightPair[moves.size()];
        for (int i = 0; i < moves.size(); i++) {
            pairArray[i] = new MoveWeightPair(moves.get(i), movesWeights[i]);
        }
        
        // sort pair array
        Arrays.sort(pairArray, new Comparator<MoveWeightPair>() {
            @Override public int compare(MoveWeightPair lhs, MoveWeightPair rhs) {
                return rhs.getWeight() - lhs.getWeight();
            }
        });

        // convert pair array back into ArrayList<Move>
        for (int i = 0; i < moves.size(); i++) {
            moves.set(i, pairArray[i].getMove());
        }

        return moves;
    }
}
