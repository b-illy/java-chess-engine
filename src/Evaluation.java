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
    // explatatory examples with Long.MAX_VALUE=1000000 in a theoretical scenario:
    // a finished game is -1000000 or 0 or 1000000 depending on side
    // a forced mate in 5 is +-999995 depending on side
    // any forced mate in more moves than mfmd(=500) is +-999500 (they are treated as equal therefore)
    // the max normal-type eval is +-999499 with mfmd=500
    public long toLong() {
        if (this.isGameOver) {
            if (this.winningColour == Colour.None) return 0;
            return Long.MAX_VALUE * (this.winningColour == Colour.White ? 1 : -1);
        } else if (this.isForcedCheckmate) {
            return (Long.MAX_VALUE - Math.min(this.movesToForcedCheckmate, Constants.MAX_FORCED_MATE_DEPTH)) * (this.whiteIsBetter ? 1 : -1);
        } else {
            return Math.min(this.centipawnsMagnitude, Long.MAX_VALUE-Constants.MAX_FORCED_MATE_DEPTH-1) * (this.whiteIsBetter ? 1 : -1);
        }
    }

    // convert to string for debugging and console printing etc.
    public String toString() {
        int type = 0;
        if (this.isForcedCheckmate) type = 1;
        if (this.isGameOver) type = 2;

        switch(type) {
            case 0: // normal type eval
                return (this.whiteIsBetter ? "+" : "-") + Double.toString((double)this.centipawnsMagnitude / (double)100);
            case 1: // forced mate
                return (this.whiteIsBetter ? "+" : "-") + "M" + Integer.toString(this.movesToForcedCheckmate);
            case 2: // game over
                if (this.winningColour == Colour.White) return "1-0";
                else if (this.winningColour == Colour.Black) return "0-1";
                else return "0.5-0.5";
            default: // failsafe
                return "";
        }
    }

    // used in search function to 'bring up a level' e.g. 1-0 -> +M1, +M5 -> +M6
    public Evaluation tick() {
        if (this.isForcedCheckmate) {
            // increment moves until forced checkmate
            this.movesToForcedCheckmate++;
        } else if (this.isGameOver) {
            // draw should just return +0.0
            if (winningColour == Colour.None) {
                this.centipawnsMagnitude = 0;
                this.whiteIsBetter = true;
                this.isGameOver = false;
            }

            // change eval to mate in 1 equivalent
            this.isForcedCheckmate = true;
            this.movesToForcedCheckmate = 1;
            this.whiteIsBetter = (this.winningColour == Colour.White);
            this.isGameOver = false;
        }

        return this;
    }
}
