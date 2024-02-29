public class EnPassantMove extends Move {
    public EnPassantMove(Piece piece, Coord coord) {
        super(piece, coord);

        if (piece.getType() != PieceType.pawn) {
            // only pawns can take en passant - throw error if this condition is not met
            throw new ExceptionInInitializerError("Could not create en passant move: piece is not a pawn");
        }

        if (!piece.getBoard().isSquareEnPassantable(coord)) {
            // if square is not en passantable we cannot create this move - throw error
            throw new ExceptionInInitializerError("Could not create en passant move: square is not en passant-able");
        }
    }

    public Board simulate() {
        // make a copy of the board
        Board board = new Board(this.piece.getBoard().getFEN());
        
        // find where the pawn which is being taken en passant is located
        Coord otherPawnCoord = new Coord(this.coord.getX(), this.coord.getY() + (this.piece.getColour() == 1 ? -1 : 1));
        // make sure that there is a pawn here
        if (board.pieceAt(otherPawnCoord).getType() != PieceType.pawn) {
            throw new RuntimeException("Couldn't find a pawn on en passant square");
        }

        // put this pawn on its new square
        board.pieceAt(this.coord).overwrite(this.piece);
        // remove the now old duplicate on our piece's original square
        board.pieceAt(this.piece.getCoord()).setEmpty();
        // also remove the en passanted pawn we are taking
        board.pieceAt(otherPawnCoord).setEmpty();

        // (dont need to update castling possibilities on these types of moves, move on)

        // update en passant target (make it out of bounds to represent no en passant square)
        board.setEnPassantSquare(new Coord(-1, -1));

        // reset halfmove clock for 50 move rule (this is a pawn move)
        board.resetHalfMoveCount();

        // return the new modified copy of the board
        board.incMoveCount();
        return board;
    }
}
