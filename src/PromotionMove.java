public class PromotionMove extends Move {
    private PieceType promoPieceType;

    public PromotionMove(Piece piece, Coord coord, PieceType type) {
        super(piece, coord);

        final PieceType[] promotionTypes = {PieceType.queen, PieceType.rook, PieceType.bishop, PieceType.knight};
        boolean found = false;
        for (PieceType t : promotionTypes) {
            if (type == t) {
                found = true;
                break;
            }
        }

        if (!found) {
            throw new ExceptionInInitializerError("PieceType given in constructor is not valid for promotion");
        }

        this.promoPieceType = type;
    }

    public Board simulate() {
        // make a copy of the board
        Board newBoard = new Board();
        newBoard.load(this.board);
        
        // put appropriate piece on its new square
        newBoard.pieceAt(this.coord).overwrite(new Piece(this.piece.getColour(), this.promoPieceType, this.coord, newBoard));
        // remove the now old duplicate on our piece's original square
        newBoard.pieceAt(this.piece.getCoord()).setEmpty();

        // (dont need to update castling possibilities on these types of moves, move on)

        // update en passant target (make it out of bounds to represent no en passant square)
        newBoard.setEnPassantSquare(new Coord(-1, -1));

        // reset halfmove clock for 50 move rule (this is a pawn move)
        newBoard.resetHalfMoveCount();

        // return the new modified copy of the board
        newBoard.incMoveCount();
        newBoard.addMoveHistory(this);
        return newBoard;
    }

    public PieceType getPromoType() {
        return this.promoPieceType;
    }
}
