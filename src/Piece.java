import java.util.ArrayList;

public class Piece {
    private Colour colour;
    private PieceType type;
    private Coord coord;
    private Board board;

    public Piece(Colour colour, PieceType type, Coord coord, Board board) {
        this.colour = colour;
        this.type = type;
        this.coord = coord;
        this.board = board;
    }

    // for being taken
    // there is only one piece object per square/tile; instead
    // of manually editing the board's tiles array, we can just
    // change the piece types and colours when necessary
    public void overwrite(Piece piece) {
        this.colour = piece.getColour();
        this.type = piece.getType();
    }

    // for being moved
    // see above
    public void setEmpty() {
        this.type = PieceType.empty;
    }

    // returns an ArrayList of all the moves a piece could potentially make, ignoring checks
    // this may include illegal moves and these will only later be filtered out in a seperate
    // process, such as by using wrapper function getLegalMoves() instead. 
    public ArrayList<Move> getCandidateMoves() {
        // candidate moves will be added here, culled down, and then the arraylist returned
        ArrayList<Move> moves = new ArrayList<Move>();

        switch (this.type) {
            case king:
                // king can move one square horizontally and diagonally
                // cardinals: 0 +1, 0 -1, +1 0, -1 0
                // diagonals: +1 +1, +1 -1, -1 -1, -1 +1
                // NOT valid: 0 0 - this is a move to the same square and is never possible
                // this totals 8 possible moves
                for (int i = -1; i <= 1; i++) {
                    for (int j = -1; j <= 1; j++) {
                        if (i == 0 && j == 0) continue;  // cant move to the same square
                        
                        // add move if it is in bounds and not taken by another piece of same colour
                        Coord c = new Coord(this.coord.getX() + i, this.coord.getY() + j);
                        if (!c.isInBounds()) continue;
                        if (this.board.pieceAt(c).getType() == PieceType.empty || this.board.pieceAt(c).getColour() != this.colour) {
                            moves.add(new Move(this, c));
                        }
                        
                    }
                }

                // king can also castle, either short and long.
                // for this to be legal the king and rook involved can't have moved yet,
                // there also can't be any pieces in the way.
                boolean[][] castling = this.getBoard().getCastlingPossibilities();

                // check for clear path between king and rook, add the move if available

                // short / kingside castling
                if (castling[this.colour == Colour.White ? 1 : 0][0]) {
                    for (int x = this.coord.getX() + 1; x < 8; x++) {
                        if (this.board.pieceAt(x, this.coord.getY()).getType() == PieceType.empty) {
                            continue;
                        } else if (this.board.pieceAt(x, this.coord.getY()).getType() == PieceType.rook && 
                                   this.board.pieceAt(x, this.coord.getY()).getColour() == this.colour) {
                            moves.add(new CastlingMove(this, true));
                        } else {
                            break;
                        }
                    }
                }

                // long / queenside castling
                if (castling[this.colour == Colour.White ? 1 : 0][1]) {
                    for (int x = this.coord.getX() - 1; x >= 0; x--) {
                        if (this.board.pieceAt(x, this.coord.getY()).getType() == PieceType.empty) {
                            continue;
                        } else if (this.board.pieceAt(x, this.coord.getY()).getType() == PieceType.rook &&
                                   this.board.pieceAt(x, this.coord.getY()).getColour() == this.colour) {
                            moves.add(new CastlingMove(this, false));
                        } else {
                            break;
                        }
                    }
                }

                break;

            case queen:
                // queen can move any amount of squares horizontally or diagonally
                // this combines the abilities of rooks and bishops

                // for the purposes of minimising code reuse, this case will run over into the rook case
                // and once that case is handled it will only break if piecetype is rook, allowing queen
                // case to run over again onto the bishop case before finally breaking. this has the effect
                // of adding all rook moves and then all bishop moves as candidate moves for queens.
            case rook:
                // rook can move any amount of squares horizontally

                // we need to cover 4 directions:
                // horizontal+ horizontal- vertical+ vertical-
                // this can be represented with 2 variables such as isHorizontal and isPositive
                // to simplify this down into a for loop they are integers but analogous to bools (0=false,1=true)
                // this allows each direction to be handled iteratively without the need to repeat code
                for (int horizontalMarker = 0; horizontalMarker < 2; horizontalMarker++) {
                    for (int posMarker = 0; posMarker < 2; posMarker++) {

                        // start looking for moves outwards from where this piece is
                        Coord c = this.getCoord();

                        while (true) {
                            // check next square in appropriate direction, see explanation above
                            int cX = c.getX();
                            int cY = c.getY();
                            if (horizontalMarker != 0) {    // horizontal
                                if (posMarker != 0) cX++;   // add
                                else                cX--;   // sub
                            } else {                        // vertical
                                if (posMarker != 0) cY++;   // add
                                else                cY--;   // sub
                            }
                            c = new Coord(cX, cY);

                            // stop looking if the edge of the board is reached
                            if (!c.isInBounds()) break;

                            // this will be a valid move whether moving to empty square or capturing
                            if (this.board.pieceAt(c).getType() == PieceType.empty || this.board.pieceAt(c).getColour() != this.colour) {
                                moves.add(new Move(this, c));
                            }
        
                            // if a piece couldve been taken on this square, we cant move past it and so
                            // shouldnt continue looking any further in this direction - break
                            if (this.board.pieceAt(c).getType() != PieceType.empty) {
                                break;
                            }
        
                        }
                    }
                }

                if (this.type == PieceType.rook) break;
                // see queen case above for explanation on the condition for this break

            case bishop:
                // bishop can move any amount of squares diagonally

                // we need to check moves in 4 directions, similar to rook but of course diagonal
                // +1 +1, +1 -1, -1 -1, -1 +1
                // as with rooks, this can be handled iteratively using 2 booleans i.e isXPositive and isYPositive
                for (int posX = 0; posX < 2; posX++) {
                    for (int posY = 0; posY < 2; posY++) {

                        // start looking for moves outwards from where this piece is
                        Coord c = this.getCoord();

                        while (true) {
                            // check next square in appropriate direction, see explanation above
                            int cX = c.getX();
                            int cY = c.getY();
                            if (posX != 0) cX++;
                            else cX--;
                            if (posY != 0) cY++;
                            else cY--;
                            c = new Coord(cX, cY);

                            // stop looking if the edge of the board is reached
                            if (!c.isInBounds()) break;

                            // this will be a valid move whether moving to empty square or capturing
                            if (this.board.pieceAt(c).getType() == PieceType.empty || this.board.pieceAt(c).getColour() != this.colour) {
                                moves.add(new Move(this, c));
                            }
        
                            // if a piece couldve been taken on this square, we cant move past it and so
                            // shouldnt continue looking any further in this direction - break
                            if (this.board.pieceAt(c).getType() != PieceType.empty) {
                                break;
                            }
        
                        }
                    }
                }

                break;

            case knight:
                // moves 2 squares horizontally and 1 square perpendicular to the inital direction in one move

                // there are 8 possible moves that a knight can make:
                // -2 -1, -2 +1, +2 -1, +2 +1, -1 -2, -1 +2, +1 -2, +1 +2
                // all of these can be represented in another way by using 3 booleans:
                // most significant direction (x/y), x is positive (t/f), y is positive (t/f)
                // these can be handled iteratively in a similar way to rook moves but with an extra bool
                for (int sigX = 0; sigX < 2; sigX++) {
                    for (int posX = 0; posX < 2; posX ++) {
                        for (int posY = 0; posY < 2; posY++) {
                            // calculate the amount that should be moved in each direction on this move (see above)
                            int xMovement = -1;
                            int yMovement = -1;
                            if (posX != 0) xMovement = 1;
                            if (posY != 0) yMovement = 1;
                            if (sigX != 0) xMovement *= 2;
                            else yMovement *= 2;

                            // add this change to current coord and add if move is legal
                            Coord c = new Coord(this.coord.getX()+xMovement, this.coord.getY()+yMovement);
                            if (!c.isInBounds()) continue;
                            if (this.board.pieceAt(c).getType() == PieceType.empty || this.board.pieceAt(c).getColour() != this.colour) {
                                moves.add(new Move(this, c));
                            }
                        }
                    }
                }

                break;

            case pawn:
                // pawn can move 1 square up the board (or 2 on the first move)
                // can also take 1 square diagonally up the board (and can take en passant (holy hell))

                // pawn move direction is affected by colour, use this variable to simplify things
                int moveDirection = 1;  // white moves up the board
                if (this.colour == Colour.Black) moveDirection = -1;  // black moves down the board

                // check if promotion is possible (pawn on penultimate rank)
                if (this.coord.getY() == (moveDirection == 1 ? 6 : 1)) {
                    // promoting pawn moves

                    // list of types of pieces that pawns are allowed to promote into
                    final PieceType[] promotionTypes = {PieceType.knight, PieceType.bishop, PieceType.rook, PieceType.queen};

                    // add each possible promotion: knight, bishop, rook, queen (if square 1 ahead is empty)
                    if (this.board.pieceAt(this.coord.getX(), this.coord.getY() + moveDirection).getType() == PieceType.empty) {
                        for (PieceType t : promotionTypes) {
                            moves.add(new PromotionMove(this, new Coord(this.coord.getX(), this.coord.getY() + moveDirection), t));
                        }
                    }

                    // also allow taking into promotion (also seen below)
                    // unlike non-promotion pawn taking moves, we dont need to handle en passant here
                    if (this.coord.getX() != 0 &&
                        this.board.pieceAt(new Coord(this.coord.getX()-1, this.coord.getY() + moveDirection)).getType() != PieceType.empty) {
                    
                        // add each possible promotion: knight, bishop, rook, queen
                        for (PieceType t : promotionTypes) {
                            moves.add(new PromotionMove(this, new Coord(this.coord.getX()-1, this.coord.getY() + moveDirection), t));
                        }
                    }
    
                    if (this.coord.getX() != 7 &&
                        this.board.pieceAt(new Coord(this.coord.getX()+1, this.coord.getY() + moveDirection)).getType() != PieceType.empty) {
    
                        // add each possible promotion: knight, bishop, rook, queen
                        for (PieceType t : promotionTypes) {
                            moves.add(new PromotionMove(this, new Coord(this.coord.getX()+1, this.coord.getY() + moveDirection), t));
                        }
                    }


                } else {
                    // non-promotion pawn moves

                    // regular 1 square forward move - check if square 1 tile above is empty + add if so
                    if (this.board.pieceAt(new Coord(this.coord.getX(), this.coord.getY() + moveDirection)).getType() == PieceType.empty) {
                        moves.add(new Move(this, new Coord(this.coord.getX(), this.coord.getY() + moveDirection)));
                    }

                    // first-move 2 square forward move
                    if (this.coord.getY() == (moveDirection == 1 ? 1 : 6)) { // detect if this is first move by rank
                        // check if square 2 tiles above is empty + add if so
                        if (this.board.pieceAt(new Coord(this.coord.getX(), this.coord.getY() + 2*moveDirection)).getType() == PieceType.empty &&
                            this.board.pieceAt(new Coord(this.coord.getX(), this.coord.getY() + 1*moveDirection)).getType() == PieceType.empty) {
                            moves.add(new Move(this, new Coord(this.coord.getX(), this.coord.getY() + 2*moveDirection)));
                        }
                    }

                    // normal diagonal taking moves
                    // movement rules for pawns are different for taking - can *only* take diagonally 1 square up each way
                    // taking up and towards left
                    if (this.coord.getX() != 0 &&
                        this.board.pieceAt(new Coord(this.coord.getX()-1, this.coord.getY() + moveDirection)).getType() != PieceType.empty &&
                        this.board.pieceAt(new Coord(this.coord.getX()-1, this.coord.getY() + moveDirection)).getColour() != this.colour) {
                        moves.add(new Move(this, new Coord(this.coord.getX()-1, this.coord.getY() + moveDirection)));
                    }
    
                    // taking up and towards right
                    if (this.coord.getX() != 7 &&
                        this.board.pieceAt(new Coord(this.coord.getX()+1, this.coord.getY() + moveDirection)).getType() != PieceType.empty &&
                        this.board.pieceAt(new Coord(this.coord.getX()+1, this.coord.getY() + moveDirection)).getColour() != this.colour) {
                        moves.add(new Move(this, new Coord(this.coord.getX()+1, this.coord.getY() + moveDirection)));
                    }

                    // taking en passant to the left
                    if (this.coord.getX() != 0 &&
                        this.board.isSquareEnPassantable(new Coord(this.coord.getX()-1, this.coord.getY() + moveDirection))) {
                        moves.add(new EnPassantMove(this, new Coord(this.coord.getX()-1, this.coord.getY() + moveDirection)));
                    }

                    // taking en passant to the right
                    if (this.coord.getX() != 7 &&
                        this.board.isSquareEnPassantable(new Coord(this.coord.getX()+1, this.coord.getY() + moveDirection))) {
                        moves.add(new EnPassantMove(this, new Coord(this.coord.getX()+1, this.coord.getY() + moveDirection)));
                    }
                }

                break;

            default:
                break;
        }

        // return the arraylist of all candidate moves for the relevant piece
        return moves;
    }

    // this function returns all moves that are legal for a given piece
    // this is done by finding all 'candidate' moves and then filtering out moves where the king would
    // be able to be captured on the next move (this would put yourself in check and thus be illegal)
    public ArrayList<Move> getLegalMoves() {
        // start with list of candidate moves to review legality of
        ArrayList<Move> moves = this.getCandidateMoves();


        // next we need to cull any moves that would result in the king remaining in check
        // but first, modifying the list while iterating could cause problems, make a list to remove later
        ArrayList<Move> removalList = new ArrayList<Move>();

        // simulate each move and see if any moves exist after that which could capture the king - flag for removal
        for (Move m1 : moves) {
            Board newBoard;
            try {
                newBoard = m1.simulate();
            } catch (RuntimeException e) {
                // probably means no rook found for castling, no pawn for en passant etc
                removalList.add(m1);  // remove the move, most likely illegal
                continue;
            }
            Piece kingPiece = newBoard.getKing(this.colour);
            if (kingPiece.getType() != PieceType.king) {
                // king not found - this shouldnt happen in valid positions
                // return empty list
                return new ArrayList<Move>();
            }

            // remove this move if we would still be in check on next move
            if (newBoard.isSquareAttacked(kingPiece.getCoord(), (this.colour == Colour.White ? Colour.Black : Colour.White))) {
                removalList.add(m1);
            }
            
            if (m1.getType() == MoveType.castling) {
                // its illegal to castle 'through' check or into/out of it
                final Coord startPos = m1.getPiece().getCoord();
                final Coord endPos = m1.getCoord();

                int x = startPos.getX();
                final int y = startPos.getY();

                while (x <= 7 && x >= 0) {
                    if (m1.getBoard().isSquareAttacked(new Coord(x, y), m1.getPiece().getColour() == Colour.White ? Colour.Black : Colour.White)) {
                        // if any square checked here is attacked, castling would be impossible - remove this move
                        removalList.add(m1);
                        break;
                    }

                    if (startPos.getX() > endPos.getX()) { // short castle
                        x--;
                        if (x < endPos.getX()) break;
                    } else if (startPos.getX() < endPos.getX()) { // long castle
                        x++;
                        if (x > endPos.getX()) break;
                    } else {
                        // castling onto the same x coord - this should never happen
                        break;
                    }
                }
            }
        }
        
        // remove all moves marked as illegal and return whatever is left
        moves.removeAll(removalList);
        return moves;
    }

    // TODO: review necessity of this function
    public boolean hasMove(Move move) {
        // note: this function assumes the move is definitely for this piece.
        // this means that it doesnt check piece type or colour
        for (Move m : this.getLegalMoves()) {
            if (move.getType() == MoveType.castling) {
                // TODO
            } else {
                if (move.getCoord().equals(m)) {
                    return true;  // found
                }
            }
        }
        return false;  // not found
    }

    public Colour getColour() {
        return this.colour;
    }

    public PieceType getType() {
        return this.type;
    }

    public Coord getCoord() {
        return this.coord;
    }

    public Board getBoard() {
        return this.board;
    }

    public boolean equals(Piece piece2) {
        // compare values of type colour + coord but NOT board
        return (this.type == piece2.getType() &&
                this.colour == piece2.getColour() &&
                this.coord.equals(piece2.getCoord()));
    }
}
