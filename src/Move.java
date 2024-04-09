import java.util.Arrays;

public class Move {
    protected Piece piece;
    protected Board board;
    protected Coord coord;  // coord to move to
    protected MoveType type;

    // typical move, subclasses overwrite this and have their own constructors
    public Move(Piece piece, Coord coord) {
        this.piece = piece;
        this.board = piece.getBoard();
        this.coord = coord;
        this.type = MoveType.normal;
    }

    // update underlying board with new position after this move
    public void make() {
        this.piece.getBoard().load(this.simulate());
    }

    public Board simulate() {
        // make a copy of the board
        Board board = new Board();
        board.load(this.piece.getBoard());

        // reset halfmove clock for 50 move rule if applicable
        // pawn move
        if (this.piece.getType() == PieceType.pawn) board.resetHalfMoveCount();
        // capture
        if (board.pieceAt(this.coord).getType() != PieceType.empty) board.resetHalfMoveCount();

        // overwrite the piece being taken, creating a copy
        board.pieceAt(this.coord).overwrite(this.piece);
        // remove the now old duplicate on our piece's original square
        board.pieceAt(this.piece.getCoord()).setEmpty();


        // update castling possibilities

        // 2-size array representing castling oppurtunities for the colour of the piece of this move
        // 0=short, 1=long e.g. [true, false] means short is possible but not long
        boolean[] myColourCastle = this.board.getCastlingPossibilities()[this.piece.getColour() == Colour.White ? 1 : 0];
        // if any type of castling is possible for this side...
        if (myColourCastle[0] || myColourCastle[1]) {
            // any king move makes any type of castling impossible
            if (this.piece.getType() == PieceType.king) {
                // make both castling types impossible
                board.removeCastling(this.piece.getColour(), 0);
                board.removeCastling(this.piece.getColour(), 1);
            } else if (this.piece.getType() == PieceType.rook) {
                // make castling in the direction of this rook impossible

                // detect whether this rook would be used to castle short or long by x coords
                int castleType = 0;  // 0=long, 1=short
                if (this.piece.getCoord().getX() > this.board.getKing(this.piece.getColour()).getCoord().getX()) {
                    // rook has higher x coord than king
                    if (this.piece.getColour() == Colour.White) castleType = 0;  // white, long
                    else castleType = 1;  // black, short
                } else {
                    // rook has lower (or same but illegal) x coord than king
                    if (this.piece.getColour() == Colour.White) castleType = 1;  // white, short
                    else castleType = 0;  // black, long
                }

                // now actually remove castling possibility for this type of castling
                board.removeCastling(this.piece.getColour(), castleType);
            }
        }

        // update en passant target
        if (this.piece.getType() != PieceType.pawn) {
            // if no en passant square would be created, make it out of bounds to represent no en passant square
            board.setEnPassantSquare(new Coord(-1, -1));
        } else {
            // set appropriate en passant square if this piece is a pawn moving 2 squares
            if (Math.abs(this.piece.getCoord().getY() - this.coord.getY()) == 2) {
                board.setEnPassantSquare(new Coord(this.coord.getX(), this.coord.getY() - (this.piece.getColour() == Colour.White ? 1 : -1)));
            } else {
                // as above, if no en passant square would be created, make it out of bounds to represent no en passant square
                board.setEnPassantSquare(new Coord(-1, -1));
            }
        }

        board.incMoveCount();
        board.addMoveHistory(this);
        return board;
    }

    public boolean isLegal() {
        // search for this move in relevant pieces legal moves
        for (Move m : this.piece.getLegalMoves()) {
            if (this.equals(m)) {
                return true;  // found, return true
            }
        }
        return false;  // not found, return false
    }

    public Coord getCoord() {
        return this.coord;
    }

    public Board getBoard() {
        return this.board;
    }

    public Piece getPiece() {
        return this.piece;
    }

    public MoveType getType() {
        return this.type;
    }

    public String toString() {
        final char[] promotionChars = {'q', 'r', 'b', 'n'};
        final PieceType[] promotionTypes = {PieceType.queen, PieceType.rook, PieceType.bishop, PieceType.knight};   
        // long algebraic form, 1st coord + 2nd coord + piece type if promoting
        return this.piece.getCoord().toString().concat(this.coord.toString())
        + (this.type == MoveType.promotion ? promotionChars[Arrays.asList(promotionTypes).indexOf(this.getPromoType())] : "");
    }

    public boolean equals(Move move2) {
        // compare values of all attributes - piece board coord type
        return (this.piece.equals(move2.getPiece()) &&
                this.board.getFEN().equals(move2.getBoard().getFEN()) &&
                this.coord.equals(move2.getCoord()) &&
                this.type == move2.getType()
                );
    }

    public PieceType getPromoType() {
        // stub impl. for method to be overwritten by PromotionMove
        return PieceType.empty;
    }
}
