public class CastlingMove extends Move {
    public CastlingMove(Piece piece, boolean shortCastle) {
        super(piece, new Coord(piece.getCoord().getX() + (shortCastle ? 2 : -2), piece.getCoord().getY()));
        this.type = MoveType.castling;
    }

    public Board simulate() {
        // make a copy of the board
        Board newBoard = new Board();
        newBoard.load(this.board);

        // find corresponding rook
        Piece rook = null;
        boolean shortCastle = this.coord.getX() > this.piece.getCoord().getX();
        int rookX = this.piece.getCoord().getX() + (shortCastle ? 1 : -1);
        boolean found = false;
        while (rookX <= 7 && rookX >= 0) {
            if (this.piece.getBoard().pieceAt(rookX, this.piece.getCoord().getY()).getType() == PieceType.rook) {
                // a rook was found
                rook = this.piece.getBoard().pieceAt(rookX, this.piece.getCoord().getY());
                if (rook.getColour() == this.piece.getColour()) {
                    found = true;  // found it, mark this and stop looping
                    break;
                }
            }

            if (shortCastle) rookX++;
            else rookX--;
        }

        if (!found || rook == null) {
            throw new RuntimeException("Couldn't find rook to castle with");
        }

        // put corresponding rook on its new square
        newBoard.setPieceAt(new Coord(this.piece.getCoord().getX()+(shortCastle ? 1 : -1), this.coord.getY()), rook);
        // remove rook from previous square
        newBoard.removePieceAt(rook.getCoord());

        // put king on its new square
        newBoard.setPieceAt(this.coord, this.piece);
        // remove king from previous square
        newBoard.removePieceAt(this.piece.getCoord());

        // remove all castling for this side
        newBoard.removeCastling(this.piece.getColour(), 0);
        newBoard.removeCastling(this.piece.getColour(), 1);

        // update en passant target (make it out of bounds to represent no en passant square)
        newBoard.setEnPassantSquare(new Coord(-1, -1));

        // return the new modified copy of the board
        newBoard.incMoveCount();
        newBoard.addMoveHistory(this);
        return newBoard;
    }
}
