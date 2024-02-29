public class CastlingMove extends Move {
    public CastlingMove(Piece piece, boolean shortCastle) {
        super(piece, new Coord(piece.getCoord().getX() + (shortCastle ? 2 : -2), piece.getCoord().getY()));
        this.type = MoveType.castling;

        // this class does not check that there is a clear path between the king and rook
        // this responsibility instead lies on the class creating instances of this class
    }

    public Board simulate() {
        // make a copy of the board
        Board board = new Board(this.piece.getBoard().getFEN());

        // find corresponding rook
        Piece rook = null;
        boolean shortCastle = this.coord.getX() > this.piece.getCoord().getX();
        int rookX = this.piece.getCoord().getX() + (shortCastle ? 1 : -1);
        boolean found = false;
        while (rookX <= 7 && rookX >= 0) {
            if (this.piece.getBoard().pieceAt(rookX, this.piece.getCoord().getY()).getType() == PieceType.rook) {
                // a rook was found
                rook = this.piece.getBoard().pieceAt(rookX, this.piece.getCoord().getY());
                if (rook.getColour() != this.piece.getColour()) continue; // not the rook, keep looking
                found = true;  // found it, mark this and stop looping
                break;
            }

            if (shortCastle) rookX++;
            else rookX--;
        }

        if (!found || rook == null) {
            throw new RuntimeException("Couldn't find rook to castle with");
        }

        // put corresponding rook on its new square
        board.pieceAt(this.piece.getCoord().getX() + (shortCastle ? 1 : -1), this.coord.getY()).overwrite(rook);
        // remove rook from previous square
        board.pieceAt(rook.getCoord()).setEmpty();

        // put king on its new square
        board.pieceAt(this.coord).overwrite(this.piece);
        // remove king from previous square
        board.pieceAt(this.piece.getCoord()).setEmpty();

        // remove all castling for this side
        board.removeCastling(this.piece.getColour(), 0);
        board.removeCastling(this.piece.getColour(), 1);

        // update en passant target (make it out of bounds to represent no en passant square)
        board.setEnPassantSquare(new Coord(-1, -1));

        // return the new modified copy of the board
        board.incMoveCount();
        return board;
    }
}