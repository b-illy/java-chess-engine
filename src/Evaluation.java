// this class is used to handle evaluations
// this is usually a float such as +1.2 (white is 1.2 pawns better), -0.4 (black 0.4 pawns better) or 0.0 (equal)
// however, we also need to handle some other types of eval: 1-0 (white wins), M8 (white has forced checkmate in 8)

public class Evaluation {
    long centipawnsMagnitude;
    boolean whiteIsBetter;

    boolean isForcedCheckmate;
    int movesToForcedCheckmate;

    boolean isGameOver;
    Colour winningColour;

    // handles most typical evaluations like -1.2, +9.4, +0.5
    public Evaluation(long standardEval) {
        if (standardEval < 0) {
            standardEval = Math.abs(standardEval);
            this.whiteIsBetter = false;
        } else {
            this.whiteIsBetter = true;
        }

        this.centipawnsMagnitude = standardEval;

        this.isForcedCheckmate = false;
        this.isGameOver = false;
    }

    public Evaluation(double standardEval) {
        this(Math.round(standardEval*(double)100));
    }

    // handles forced checkmates like -M7, M2
    public Evaluation(int movesToForcedCheckmate, boolean isForWhite) {
        this.isForcedCheckmate = true;
        this.movesToForcedCheckmate = movesToForcedCheckmate;
        this.whiteIsBetter = isForWhite;

        this.isGameOver = false;
    }

    // handles finished games, 1-0, 0-1, or 0.5-0.5
    public Evaluation(Colour winningColour) {
        this.isGameOver = true;
        this.winningColour = winningColour;

        this.isForcedCheckmate = false;
    }

    public boolean isForcedCheckmate() {
        return this.isForcedCheckmate;
    }

    public boolean isGameOver() {
        return this.isGameOver;
    }

    // convert to very basic long format -- easier for comparisons
    public long toLong() {
        if (this.isForcedCheckmate || this.isGameOver) {
            if (this.whiteIsBetter) return Long.MAX_VALUE;
            else return Long.MIN_VALUE;
        } else {
            return this.centipawnsMagnitude * (this.whiteIsBetter ? 1 : -1);
        }
    }

    // convert to string for debugging and console printing etc.
    public String toString() {
        int type = 0;
        if (this.isForcedCheckmate) type = 1;
        if (this.isGameOver) type = 2;

        switch(type) {
            case 0:
                return (this.whiteIsBetter ? "+" : "-") + Double.toString((double)this.centipawnsMagnitude / (double)100);
            case 1:
                return (this.whiteIsBetter ? "+" : "-") + "M" + Integer.toString(this.movesToForcedCheckmate);
            case 2:
                if (this.winningColour == Colour.White) return "1-0";
                else if (this.winningColour == Colour.Black) return "0-1";
                else return "0.5-0.5";
            default:
                return "";
        }
    }
}
