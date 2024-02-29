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
        Board board = new Board(this.piece.getBoard().getFEN());
        
        // put appropriate piece on its new square
        board.pieceAt(this.coord).overwrite(new Piece(this.piece.getColour(), this.promoPieceType, this.coord, board));
        // remove the now old duplicate on our piece's original square
        board.pieceAt(this.piece.getCoord()).setEmpty();

        // (dont need to update castling possibilities on these types of moves, move on)

        // update en passant target (make it out of bounds to represent no en passant square)
        board.setEnPassantSquare(new Coord(-1, -1));

        // return the new modified copy of the board
        board.incMoveCount();
        return board;
    }
}