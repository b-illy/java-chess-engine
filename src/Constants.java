public class Constants {
    public static final int VALUE_PAWN = 100;
    public static final int VALUE_KNIGHT = 300;
    public static final int VALUE_BISHOP = 320;
    public static final int VALUE_ROOK = 500;
    public static final int VALUE_QUEEN = 900;

    public static final int MAX_FORCED_MATE_DEPTH = 500;

    public static final int MAX_MINIMAX_DEPTH = 50;

    public static final int EVAL_HIGH_HALFMOVE_COUNT = 30;
    public static final int EVAL_DOUBLED_PAWN_PENALTY = 20;
    public static final int EVAL_CONTROLLED_SQUARE_BONUS = 5;
    public static final int[] EVAL_PASSED_PAWN_BONUSES = {0,80,60,50,40,25,20,0};


    // piece-square tables for evaluation
    // https://www.chessprogramming.org/Simplified_Evaluation_Function

    public static final int[][] PST_PAWN = {
        { 0,  0,  0,  0,  0,  0,  0,  0},
        {50, 50, 50, 50, 50, 50, 50, 50},
        {10, 10, 20, 30, 30, 20, 10, 10},
        { 5,  5, 10, 25, 25, 10,  5,  5},
        { 0,  0,  0, 20, 20,  0,  0,  0},
        { 5, -5,-10,  0,  0,-10, -5,  5},
        { 5, 10, 10,-20,-20, 10, 10,  5},
        { 0,  0,  0,  0,  0,  0,  0,  0}
    };

    public static final int[][] PST_KNIGHT = {
        {-50,-40,-30,-30,-30,-30,-40,-50},
        {-40,-20,  0,  0,  0,  0,-20,-40},
        {-30,  0, 10, 15, 15, 10,  0,-30},
        {-30,  5, 15, 20, 20, 15,  5,-30},
        {-30,  0, 15, 20, 20, 15,  0,-30},
        {-30,  5, 10, 15, 15, 10,  5,-30},
        {-40,-20,  0,  5,  5,  0,-20,-40},
        {-50,-40,-30,-30,-30,-30,-40,-50}
    };

    public static final int[][] PST_BISHOP = {
        {-20,-10,-10,-10,-10,-10,-10,-20},
        {-10,  0,  0,  0,  0,  0,  0,-10},
        {-10,  0,  5, 10, 10,  5,  0,-10},
        {-10,  5,  5, 10, 10,  5,  5,-10},
        {-10,  0, 10, 10, 10, 10,  0,-10},
        {-10, 10, 10, 10, 10, 10, 10,-10},
        {-10,  5,  0,  0,  0,  0,  5,-10},
        {-20,-10,-10,-10,-10,-10,-10,-20}
    };

    public static final int[][] PST_ROOK = {
       { 0,  0,  0,  0,  0,  0,  0,  0},
       { 5, 10, 10, 10, 10, 10, 10,  5},
       {-5,  0,  0,  0,  0,  0,  0, -5},
       {-5,  0,  0,  0,  0,  0,  0, -5},
       {-5,  0,  0,  0,  0,  0,  0, -5},
       {-5,  0,  0,  0,  0,  0,  0, -5},
       {-5,  0,  0,  0,  0,  0,  0, -5},
       { 0,  0,  0,  5,  5,  0,  0,  0}
    };

    public static final int[][] PST_QUEEN = {
        {-20,-10,-10, -5, -5,-10,-10,-20},
        {-10,  0,  0,  0,  0,  0,  0,-10},
        {-10,  0,  5,  5,  5,  5,  0,-10},
        { -5,  0,  5,  5,  5,  5,  0, -5},
        {  0,  0,  5,  5,  5,  5,  0, -5},
        {-10,  5,  5,  5,  5,  5,  0,-10},
        {-10,  0,  5,  0,  0,  0,  0,-10},
        {-20,-10,-10, -5, -5,-10,-10,-20}
    };

    public static final int[][] PST_KING_EARLY = {
        {-30,-40,-40,-50,-50,-40,-40,-30},
        {-30,-40,-40,-50,-50,-40,-40,-30},
        {-30,-40,-40,-50,-50,-40,-40,-30},
        {-30,-40,-40,-50,-50,-40,-40,-30},
        {-20,-30,-30,-40,-40,-30,-30,-20},
        {-10,-20,-20,-20,-20,-20,-20,-10},
        { 20, 20,  0,  0,  0,  0, 20, 20},
        { 20, 30, 10,  0,  0, 10, 30, 20}
    };

    public static final int[][] PST_KING_LATE = {
        {-50,-40,-30,-20,-20,-30,-40,-50},
        {-30,-20,-10,  0,  0,-10,-20,-30},
        {-30,-10, 20, 30, 30, 20,-10,-30},
        {-30,-10, 30, 40, 40, 30,-10,-30},
        {-30,-10, 30, 40, 40, 30,-10,-30},
        {-30,-10, 20, 30, 30, 20,-10,-30},
        {-30,-30,  0,  0,  0,  0,-30,-30},
        {-50,-30,-30,-30,-30,-30,-30,-50}
    };
}

