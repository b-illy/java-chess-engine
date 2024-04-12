public class EnPassantMove extends Move {
    public EnPassantMove(Piece piece, Coord coord) {
        super(piece, coord);
        this.type = MoveType.enPassant;

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
        Board newBoard = new Board();
        newBoard.load(this.board);
        
        // find where the pawn which is being taken en passant is located
        Coord otherPawnCoord = new Coord(this.coord.getX(), this.coord.getY() + (this.piece.getColour() == Colour.White ? -1 : 1));
        // make sure that there is a pawn here
        if (newBoard.pieceAt(otherPawnCoord).getType() != PieceType.pawn) {
            System.out.println("\n\nEN PASSANT ERROR!");
            System.out.println("- Attempted to en passant square: " + this.coord);
            System.out.println("- Pawn expected (but not found) at: " + otherPawnCoord);
            System.out.println("\nBoard printout at time of error:\n\n" + this.piece.getBoard().toString() + "\n");
            throw new RuntimeException("Couldn't find relevant pawn for en passant square");
        }

        // put this pawn on its new square
        newBoard.setPieceAt(this.coord, this.piece);
        // remove the now old duplicate on our piece's original square
        newBoard.removePieceAt(this.piece.getCoord());
        // also remove the en passanted pawn we are taking
        newBoard.removePieceAt(otherPawnCoord);

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
}
