import java.util.ArrayList;

public class MoveOrdering {
    public static ArrayList<Move> reorder(ArrayList<Move> moves) {
        int[] movesWeights = new int[moves.size()];

        for (int i = 0; i < moves.size(); i++) {
            // calculate a priority weighting for each move
            int weight = 0;

            // TODO

            movesWeights[i] = weight;
        }


        // sort into new ArrayList based on corresponding weight values
        ArrayList<Move> reorderedMoves = new ArrayList<Move>();
        // TODO

        // TODO: remove stub implementation, return sorted reorderedMoves instead
        return moves;
    }
}
