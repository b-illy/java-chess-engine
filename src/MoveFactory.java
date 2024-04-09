public class MoveFactory {
    // parse move from algebraic notation
    static final public Move fromLongAlgebraicStr(String longAlgebraicMove, Board board) {
        // parse both coordinates from string
        if (longAlgebraicMove.length() != 4 && longAlgebraicMove.length() != 5) throw new ExceptionInInitializerError("invalid move format (wrong length)");
        Coord coord1 = new Coord(longAlgebraicMove.substring(0,2));  // source coord
        Coord coord2 = new Coord(longAlgebraicMove.substring(2,4));  // dest coord
        if (!coord1.isInBounds() || !coord2.isInBounds()) throw new ExceptionInInitializerError("invalid move format (bad coordinate)");
        
        Piece piece = board.pieceAt(coord1);

        // check if this move is castling
        if (piece.getType() == PieceType.king) {
            // if king moves more than 1 square across this is either castling or illegal (assume legal)
            // (there are edge cases in variants where a king and rook begin on squares directly next
            // to one another, and the king can castle by moving only 1 square - this is ignored)
            if (Math.abs(coord1.getX() - coord2.getX()) > 1) {
                // TODO: check that there is a clear path to castle
                return new CastlingMove(piece, coord2.getX() > coord1.getX());
            }
        }

        if (piece.getType() == PieceType.pawn) {
            // only promotions are of length 5 (last char is piece e.g. q r b n)
            if (longAlgebraicMove.length() == 5) {
                char pieceTypeChar = longAlgebraicMove.charAt(4);
                // parse the piece type signified by final char
                if ("qrbn".indexOf(pieceTypeChar) != -1) {
                    // create and return this new move
                    final PieceType[] pieceTypeArr = {PieceType.queen, PieceType.rook, PieceType.bishop, PieceType.knight};
                    return new PromotionMove(piece, coord2, pieceTypeArr["qrbn".indexOf(pieceTypeChar)]);
                } else {
                    // last character was not one of the valid possibilities - error
                    throw new ExceptionInInitializerError("promotion piece type not recognised");
                }
            }

            // if this is a capturing move...
            if (coord2.getX() != coord1.getX()) {
                // check for en passant
                if (board.isSquareEnPassantable(coord2)) {
                    // return appropriate en passant type move
                    return new EnPassantMove(piece, coord2);
                }
            }
        }

        // if this code was reached, the move is not of any special type - return normal move
        return new Move(piece, coord2);
    }
}
