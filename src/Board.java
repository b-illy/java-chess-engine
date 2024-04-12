import java.util.ArrayList;
import java.util.HashMap;

public class Board {
    private final static String startFEN = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";
    
    // private Piece[][] tiles = new Piece[8][8];
    private GameState gameState;
    private int gameStateLastUpdate;
    private ArrayList<Move> legalMoves;
    private int legalMovesLastUpdate;
    private Colour sideToMove;
    private Coord enPassantTarget = new Coord(-1,-1);  // placeholder invalid coord
    private ArrayList<Move> moveHistory;
    private int halfmove;
    private int move;
    private HashMap<String, Integer> repetitionTable;
    private boolean[][] canCastle = new boolean[2][2];  // first index is colour, second is direction
                                                        // 0=black, 1=white, 0=short, 1=long
    
    // bitboards
    private long[] bitboards = {
        // white
        0, // pawn
        0, // knight
        0, // bishop
        0, // rook
        0, // queen
        0, // king
        // black
        0, // pawn
        0, // knight
        0, // bishop
        0, // rook
        0, // queen
        0, // king
        // white controlled squares
        0,
        // black controlled squares
        0
    };

    public long[] getBitboards() {
        return this.bitboards;
    }


    // main constructor, initialises board from fen string (falls back to default position)
    public Board(String fen) {
        // try to load fen
        if (!this.loadFEN(fen)) {
            // fallback to default fen
            if (!this.loadFEN(Board.startFEN)) {
                // error if could not load
                System.out.println("Failed to load given FEN or default FEN");
                throw new ExceptionInInitializerError("Failed to load given FEN or default FEN");
            }
        }

        // init empty repetition table and move history arraylist
        this.repetitionTable = new HashMap<String, Integer>();
        this.moveHistory = new ArrayList<Move>();

        // naively assume game is ongoing -- can't call getGameState here or infinite loop
        this.gameState = GameState.Ongoing;

        //
        this.gameStateLastUpdate = -1;
        this.legalMovesLastUpdate = -1;
    }
    
    // constructor for creating board with default position (starting position)
    public Board() {
        this(Board.startFEN);
    }

    public boolean isCheck() {
        return this.isSquareAttacked(this.getKing(this.sideToMove).getCoord(), (this.sideToMove==Colour.White ? Colour.Black : Colour.White));
    }

    // updates game state and then returns it
    public GameState getGameState() {
        // return 'cached' value if position hasnt changed
        int currentTurn = 2*this.move + (this.sideToMove == Colour.Black ? 1 : 0);
        if (currentTurn != this.gameStateLastUpdate || this.gameState == null) {
            this.gameStateLastUpdate = currentTurn;
        } else {
            return this.gameState;
        }

        if (this.halfmove >= 100) {
            // if 50 moves have passed, draw by inactivity
            this.gameState = GameState.Draw;
        } else if (this.repetitionTable.get(this.getStrippedFEN()) != null && this.repetitionTable.get(this.getStrippedFEN()) >= 3) {
            // 3 (or more) occurances of this position during this game, draw by repetition
            this.gameState = GameState.Draw;
        } else if (this.getLegalMoveCount() == 0) {
            // no legal moves, game is over
            // its stalemate if king not in check, checkmate otherwise
            // therefore we need to check if the king is in check right now
            if (this.isCheck()) {
                this.gameState = (this.sideToMove == Colour.White ? GameState.BlackWon : GameState.WhiteWon);
            } else {
                this.gameState = GameState.Draw;
            }
        } else {
            // no game end conditions seem to be met, game should be ongoing
            this.gameState = GameState.Ongoing;
        }

        return this.gameState;
    }

    public void print() {
        System.out.print(this.toString());
    }

    // for printing board preview to console
    public String toString() {
        // using a character sequence is efficient here as the exact length is known
        char[] charSeq = new char[64 + 8];  // 64 squares + 8 newlines
        int x = 0;
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                Piece p = this.pieceAt(j, 7-i);
                // this tile should contain a letter showing its type and colour
                switch(p.getType()) {
                    case king:   charSeq[x] = 'k'; break;
                    case queen:  charSeq[x] = 'q'; break;
                    case rook:   charSeq[x] = 'r'; break;
                    case bishop: charSeq[x] = 'b'; break;
                    case knight: charSeq[x] = 'n'; break;
                    case pawn:   charSeq[x] = 'p'; break;
                    default:     charSeq[x] = '-';
                }

                // make white pieces uppercase
                if (p.getColour() == Colour.White) {
                    charSeq[x] = Character.toUpperCase(charSeq[x]);
                }

                x++;
            }

            charSeq[x] = '\n';

            x++;
        }

        return new String(charSeq);
    }

    // wrapper function, load default position if no string given
    public boolean loadFEN() {
        return this.loadFEN(Board.startFEN);
    }

    // loads a position from fen string. returns false on error, true on success.
    public boolean loadFEN(String fen) {
        String[] fields = fen.split(" ");
        if (fields.length != 6) {
            return false;
        }

        // field 0: piece positions
        String[] rows = fields[0].split("/");
        if (rows.length != 8) {
            return false;
        }

        
        // for (int i = 0; i < this.bitboards.length; i++) this.bitboards[i] = 0;
        // clear all bitboards and this.tiles
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                this.removePieceAt(new Coord(i,j));
            }
        }
        
        for (int i = 0; i < 8; i++) {
            int end = 8; // make sure we only read required amount of chars
            // x used to index this.tiles[], j used to index rows[i]
            // these need to increase every loop, but x may need to be increased more
            // due to '5' representing 5 pieces despite being 1 character, for example.
            int x = 0;
            for (int j = 0; j < end; j++) {
                if ("12345678".indexOf(rows[i].charAt(j)) != -1) {
                    // get int value of this char
                    int value = Integer.parseInt(Character.toString(rows[i].charAt(j)));
                    
                    // don't try to read 8 characters here, there will be fewer
                    end -= (value - 1);

                    // add the correct amount of empty spaces to this row
                    for (int a = 0; a < value; a++) {
                        // this.tiles[i][x] = new Piece(Colour.None, PieceType.empty, new Coord(x, 7-i), this);
                        // this.removePieceAt(new Coord (x, 7-i));
                        if (a < value - 1) x++;
                    }
                } else if ("kqrbnpKQRBNP".indexOf(rows[i].charAt(j)) != -1) {
                    // determine piecetype
                    PieceType type;
                    switch (rows[i].toLowerCase().charAt(j)) {
                        case 'k': type = PieceType.king;   break;
                        case 'q': type = PieceType.queen;  break;
                        case 'r': type = PieceType.rook;   break;
                        case 'b': type = PieceType.bishop; break;
                        case 'n': type = PieceType.knight; break;
                        case 'p': type = PieceType.pawn;   break;
                        default:  return false;  // invalid character, error
                    }

                    // determine colour
                    Colour colour;
                    if (rows[i].charAt(j) == rows[i].toLowerCase().charAt(j)) {
                        colour = Colour.Black;
                    } else {
                        colour = Colour.White;
                    }

                    // set piece at relevant coord to new piece with the relevant values set
                    // (x,y)->(y,7-x) is the transform done here, this rotates 90 deg clockwise as we are
                    // using a different coordinate system in this loop than is expected elsewhere
                    // this.tiles[i][x] = new Piece(colour, type, new Coord(x, 7-i), this);
                    this.setPieceAt(new Coord(x, 7-i), new Piece(colour, type, new Coord(x, 7-i), this));
                    // System.out.println("x: " + i + " y: " + x + " | newX: " + x + " newY: " + (7-i));
                } else {
                    // invalid character, error
                    return false;
                }

                x++;
            }
        }

        // field 1: current turn
        // white = 1, black = 0
        switch (fields[1]) {
            case "w": this.sideToMove = Colour.White;  break;
            case "b": this.sideToMove = Colour.Black;  break;
            default:  this.sideToMove = Colour.None;  // should never occur
        }

        // field 2: castling ability
        // black
        this.canCastle[0][0] = fields[2].contains("k");
        this.canCastle[0][1] = fields[2].contains("q");
        // white
        this.canCastle[1][0] = fields[2].contains("K");
        this.canCastle[1][1] = fields[2].contains("Q");

        // field 3: en passant target
        if (fields[3].equals("-")) this.enPassantTarget = new Coord(-1,-1);
        else this.enPassantTarget = new Coord(fields[3]);

        // field 4: halfmove clock (for draw by inactivity)
        this.halfmove = Integer.parseInt(fields[4]);

        // field 5: fullmove number
        this.move = Integer.parseInt(fields[5]);

        // finally, check controlled squares for each side and store this
        this.checkControlledSquares();

        // everything seems to have worked, success (return true)
        return true;
    }

    // wrapper function for below, describes only the positions of pieces
    public String getStrippedFEN() {
        return this.getFEN().split(" ")[0];
    }

    // converts the position of this board into a FEN string
    public String getFEN() {
        // start with empty string, add everything to it on the fly, then return it
        String str = "";
        
        // FIELD 1 - piece placements
        int emptyCount = 0;  // current streak of empty squares 
        for (int i = 0; i < 8; i++) {
            emptyCount = 0;
            for (int j = 0; j < 8; j++) {
                // Piece t = this.tiles[i][j];  // shorthand shortcut
                Piece t = this.pieceAt(j, 7-i);

                if (t.getType() == PieceType.empty) {
                    emptyCount++;
                } else {
                    if (emptyCount != 0) {
                        // print the number of empty squares and reset the count when non-empty found
                        str += Integer.toString(emptyCount);
                        emptyCount = 0;
                    }
                    // actual piece encountered, print its letter representation (pnbrqk)
                    char pieceChar;
                    switch (t.getType()) {
                        case pawn:   pieceChar = 'p'; break;
                        case knight: pieceChar = 'n'; break;
                        case bishop: pieceChar = 'b'; break;
                        case rook:   pieceChar = 'r'; break;
                        case queen:  pieceChar = 'q'; break;
                        case king:   pieceChar = 'k'; break;
                        default:     pieceChar = '?';  // this should never happen
                    }

                    // make uppercase if white
                    if (t.getColour() == Colour.White) pieceChar = Character.toUpperCase(pieceChar);

                    str += pieceChar;
                }
            }
            if (emptyCount != 0) str += Integer.toString(emptyCount);  // print emptycount if needed at row end
            str += "/";  // rank seperators
        }

        // remove the trailing '/'
        str = str.substring(0,str.length()-1);


        // FIELD 2 - active colour
        str += " ";
        if (this.sideToMove == Colour.White) str += "w";
        else str += "b";


        // FIELD 3 - castling options
        str += " ";
        boolean noCastling = true;
        if (this.canCastle[1][1]) {str += "K"; noCastling = false;}
        if (this.canCastle[1][0]) {str += "Q"; noCastling = false;}
        if (this.canCastle[0][1]) {str += "k"; noCastling = false;}
        if (this.canCastle[0][0]) {str += "q"; noCastling = false;}
        if (noCastling) str += "-";


        // FIELD 4 - en passant target
        str += " ";
        if (!this.enPassantTarget.isInBounds()) str += "-";
        else str += this.enPassantTarget.toString();

        // FIELD 5 - halfmove
        str += " " + this.halfmove;

        // FIELD 6 - fullmove
        str += " " + this.move;

        return str;
    }

    
    // wrapper for the below function
    public Piece pieceAt(int x, int y) {
        return this.pieceAt(new Coord(x, y));
    }
    
    // returns the piece on the board at given coord
    public Piece pieceAt(Coord coord) {
        Colour colour;
        PieceType pieceType = PieceType.empty;
        
        // determine the colour of the piece
        if (Bitboards.match(Bitboards.whiteSquares(this.bitboards), coord)) {
            // there is a white piece here
            colour = Colour.White;
        } else if (Bitboards.match(Bitboards.blackSquares(this.bitboards), coord)) {
            // there is a black piece here
            colour = Colour.Black;
        } else {
            // empty square, return new empty piece
            return new Piece(Colour.None, PieceType.empty, coord, this);
        }

        // iterate through bitboards of appropriate colour to determine piecetype
        int offset = (colour == Colour.White ? 0 : 6);
        for (int i = offset; i < 6+offset; i++) {
            if (Bitboards.match(this.bitboards[i], coord)) {
                pieceType = PieceType.values()[i-offset];
                // System.out.println(colour + " " + pieceType + " @ " + coord);
                break;
            }
        }
        
        if (pieceType == PieceType.empty) {
            // something went wrong, couldnt match piece to any of the bitboards
            // System.out.println("ERROR, info below");
            // for (long b : this.bitboards) System.out.println(Long.toBinaryString(b));
            // System.out.println("coord " + coord + " (index " + Bitboards.toIndex(coord) + ") " + "colour " + colour);
            // System.out.println(Long.toBinaryString(Bitboards.whiteSquares(this.bitboards)));
            // System.out.println(Long.toBinaryString(Bitboards.blackSquares(this.bitboards)));
            throw new RuntimeException("Board.pieceAt() bitboard error");
        }
        
        return new Piece(colour, pieceType, coord, this);
    }
    
    public void setPieceAt(Coord coord, Piece piece) {
        // firstly we need to make this square empty on all bitboards
        for (int i = 0; i < this.bitboards.length; i++) {
            this.bitboards[i] = Bitboards.unsetBit(this.bitboards[i], Bitboards.toIndex(coord));
        }
        
        // also handle this.tiles
        // this.tiles[7-coord.getY()][coord.getX()] = new Piece(Colour.None, PieceType.empty, coord, this);
        
        // if we are setting this square to be empty, we are already done
        if (piece.getType() == PieceType.empty || piece.getColour() == Colour.None) {
            return;
        }
        
        // next we are going to set the bit on the appropriate bitboard
        
        // determine which bitboard needs to be modified
        int pieceTypeIndex = -1;
        for (int i = 0; i < PieceType.values().length-1; i++) {
            if (piece.getType() == PieceType.values()[i]) {
                pieceTypeIndex = i;
                break;
            }
        }
        
        if (pieceTypeIndex == -1) throw new RuntimeException("unable to determine PieceType");
        
        int offset = (piece.getColour() == Colour.White ? 0 : 6);  // colour-based offset into this.bitboards
        offset += pieceTypeIndex;  // final index of this.bitboards to modify
        
        // set the appropriate bit in appropriate bitboard
        this.bitboards[offset] = Bitboards.setBit(this.bitboards[offset], Bitboards.toIndex(coord));
        
        // old, non-bitboard way
        // set this.tiles for new square to piece
        // this.tiles[7 - coord.getY()][coord.getX()] = new Piece(piece.getColour(), piece.getType(), coord, this);
    }

    public void removePieceAt(Coord coord) {
        // bitboards
        for (int i = 0; i < this.bitboards.length; i++) {
            this.bitboards[i] = Bitboards.unsetBit(this.bitboards[i], Bitboards.toIndex(coord));
        }
        
        // old way with this.tiles
        // this.tiles[7 - coord.getY()][coord.getX()] = new Piece(Colour.None, PieceType.empty, coord, this);
    }

    public boolean isSquareAttacked(Coord atCoord, Colour byColour) {
        return Bitboards.match(this.bitboards[byColour == Colour.White ? 12 : 13], atCoord);
    }

    private void checkControlledSquares() {
        // reset existing controlled square bitboards
        this.bitboards[12] = 0;
        this.bitboards[13] = 0;

        // handle pawn attacks all at once with masks
        this.bitboards[12] |= Bitboards.pawnAttacksL(this.bitboards[12], true);
        this.bitboards[12] |= Bitboards.pawnAttacksR(this.bitboards[12], true);
        this.bitboards[13] |= Bitboards.pawnAttacksL(this.bitboards[13], false);
        this.bitboards[13] |= Bitboards.pawnAttacksR(this.bitboards[13], false);

        // handle knights
        for (int i = 0; i < 64; i++) {
            for (int j = 0; j < 2; j++) {
                if (Bitboards.match(this.bitboards[1+(6*j)], i))
                this.bitboards[12+j] |= Bitboards.knightMoveMask(i);
            }
        }

        // handle kings
        this.bitboards[12] |= Bitboards.kingMoveMask(Bitboards.toIndex(this.getKing(Colour.White).getCoord()));
        this.bitboards[13] |= Bitboards.kingMoveMask(Bitboards.toIndex(this.getKing(Colour.Black).getCoord()));

        // iterate over every square
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                // set relevant bit for each square attacked for each colour
                for (int k = 0; k < 2; k++) {
                    if (this.checkIfSquareControlled(new Coord(i, j), k==0?Colour.White:Colour.Black)) {
                        this.bitboards[k==0?12:13] = Bitboards.setBit(this.bitboards[k==0?12:13], Bitboards.toIndex(i, j));
                    }
                }
            }
        }
    }

    // only to be used for detection of checks, not intended for showing only fully legal moves
    private boolean checkIfSquareControlled(Coord atCoord, Colour byColour) {
        if (byColour == Colour.None) return false; // method should only be called with byColour White or Black

        // seperate handling of pawn moves
        // final int moveDirection = (byColour == Colour.White ? 1 : -1);
        // final Coord[] pawnCoords = {new Coord(atCoord.getX()+1, atCoord.getY()-moveDirection),
        //                             new Coord(atCoord.getX()-1, atCoord.getY()-moveDirection)};
        // for (Coord c : pawnCoords) {
        //     if (!c.isInBounds()) continue;
        //     Piece p = this.pieceAt(c);
        //     if (p.getColour() != byColour) continue;
        //     if (p.getType() == PieceType.pawn) return true;
        // }

        // seperate handling of knight moves

        // (this comment copied from Piece.getCandidateMoves())
        // there are 8 possible moves that a knight can make:
        // -2 -1, -2 +1, +2 -1, +2 +1, -1 -2, -1 +2, +1 -2, +1 +2
        // all of these can be represented in another way by using 3 booleans:
        // most significant direction (x/y), x is positive (t/f), y is positive (t/f)
        // these can be handled iteratively in a similar way to rook moves but with an extra bool
        // for (int sigX = 0; sigX < 2; sigX++) {
        //     for (int posX = 0; posX < 2; posX ++) {
        //         for (int posY = 0; posY < 2; posY++) {
        //             // calculate the amount that should be moved in each direction on this move (see above)
        //             int xMovement = -1;
        //             int yMovement = -1;
        //             if (posX != 0) xMovement = 1;
        //             if (posY != 0) yMovement = 1;
        //             if (sigX != 0) xMovement *= 2;
        //             else yMovement *= 2;

        //             // add this change to current coord and add if move is legal
        //             Coord c = new Coord(atCoord.getX()+xMovement, atCoord.getY()+yMovement);
        //             if (!c.isInBounds()) continue;
        //             if (this.pieceAt(c).getType() == PieceType.knight && this.pieceAt(c).getColour() == byColour) {
        //                 return true;
        //             }
        //         }
        //     }
        // }

        // iterate over each possible direction a piece can move in
        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                if (i == 0 && j == 0) continue;  // cant move to the same square
                
                final int cX = atCoord.getX();
                final int cY = atCoord.getY();
                Coord c = new Coord(cX + i, cY + j);

                if (!c.isInBounds()) continue;
                Piece p = this.pieceAt(c);

                // firstly, check for king (range of only 1 square)
                // if (p.getColour() == byColour && p.getType() == PieceType.king) return true;

                for (int multiplyingFactor = 1; multiplyingFactor < 8; multiplyingFactor++) {
                    // setup coordinate and piece for this iteration
                    c = new Coord(cX + (i*multiplyingFactor), cY + (j*multiplyingFactor));
                    if (!c.isInBounds()) break;
                    p = this.pieceAt(c);

                    // skip over empty squares
                    if (p.getType() == PieceType.empty) continue;

                    // check for friendly blocking pieces
                    if (p.getColour() != byColour) break;
                    
                    if (p.getType() == PieceType.queen) return true;

                    // check for non-sliding blocking pieces
                    if (p.getType() != PieceType.bishop && p.getType() != PieceType.rook) break;

                    if (i != 0 && j != 0) {
                        // diagonal movement (bishop)
                        if (p.getType() == PieceType.bishop) return true;
                    } else {
                        // horizontal/vertical movement (rook)
                        if (p.getType() == PieceType.rook) return true;
                    }

                    // if no check was found and square isnt empty, this piece will block the rest
                    break;
                }
            }
        }

        return false;
    }

    // returns the piece object for the king of a given colour.
    // assumes that a position is valid and such has exactly one
    // king of each colour on the board; no more and no less
    public Piece getKing(Colour colour) {
        // linear search over each square to check for king of given colour
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                if (this.pieceAt(i, j).getType() == PieceType.king) {
                    if (this.pieceAt(i, j).getColour() == colour) {
                        // piece was found, stop searching and return it
                        return this.pieceAt(i, j);
                    }
                }
            }
        }

        // this code will only be reached if no valid king piece was found
        // returns a placeholder empty piece. any code calling this function should
        // seperately check that the piece type returned is actually a king and handle
        // that scenario independently - no errors are actually thrown.
        return new Piece(Colour.None, PieceType.empty, new Coord(-1, -1), this);
    }

    // simple check used to check if en passant is possible on a given square
    public boolean isSquareEnPassantable(Coord coord) {
        if (!coord.isInBounds()) return false;
        return this.enPassantTarget.equals(coord);
    }

    public void setEnPassantSquare(Coord coord) {
        this.enPassantTarget = coord;
    }

    public void incMoveCount() {
        // now is the perfect time to store this position in the repetition table
        Integer repetitions = this.repetitionTable.get(this.getStrippedFEN());
        this.repetitionTable.put(this.getStrippedFEN(), (repetitions == null ? 1 : repetitions + 1));

        // switch colour due to move
        if (this.sideToMove == Colour.White) this.sideToMove = Colour.Black;
        else this.sideToMove = Colour.White;
        this.halfmove++;  // inc halfmove counter by 1
        if (this.sideToMove == Colour.White) this.move++;  // inc fullmove count if necessary
    }

    public void resetHalfMoveCount() {
        this.halfmove = 0;
    }

    // removes the possibility of castling for a certain colour in a certain direction
    // we only need to remove this as it starts enabled and cant be reenabled
    // colour: 0=black, 1=white
    // type:   0=long,  1=short
    // return true means success, false means failure
    public boolean removeCastling(Colour colour, int type) {
        if (colour == Colour.None) {
            // invalid parameter
            return false;
        }

        // update castling possibilities
        this.canCastle[colour == Colour.White ? 1 : 0][type] = false;

        return true;
    }

    public long getLegalMoveCount() {
        return this.getLegalMoves().size();
    }

    public ArrayList<Move> getLegalMoves() {
        // return 'cached' value if position hasnt changed
        int currentTurn = 2*this.move + (this.sideToMove == Colour.Black ? 1 : 0);
        if (currentTurn != this.legalMovesLastUpdate || this.legalMoves == null) {
            this.legalMovesLastUpdate = currentTurn;
        } else {
            return this.legalMoves;
        }

        ArrayList<Move> legalMoves = new ArrayList<Move>();
        // iterate over all pieces
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                // filter for pieces of appropriate colour based on turn
                if (this.pieceAt(i, j).getType() != PieceType.empty && this.pieceAt(i, j).getColour() == this.sideToMove) {
                    // add all of this piece's legal moves to the legal moves for the board
                    legalMoves.addAll(this.pieceAt(i, j).getLegalMoves());
                }
            }
        }

        // a temporary variable is written to above and then copied here to maximise atomicity and prevent any race conditions
        this.legalMoves = legalMoves;

        return this.legalMoves;
    }

    public void addMoveHistory(Move m) {
        this.moveHistory.add(m);
    }

    public ArrayList<Move> getMoveHistory() {
        return this.moveHistory;
    }

    public int getMoveNumber() {
        return this.move;
    }

    public int getHalfMoveNumber() {
        return this.halfmove;
    }
    
    public boolean[][] getCastlingPossibilities() {
        return this.canCastle;
    }

    public Colour getSideToMove() {
        return this.sideToMove;
    }

    public Coord getEnPassantSquare() {
        return this.enPassantTarget;
    }

    public HashMap<String, Integer> getRepetitionTable() {
        return this.repetitionTable;
    }

    public void load(Board b) {
        // copy everything about the position itself
        // this.loadFEN(b.getFEN());
        this.bitboards = b.getBitboards().clone();
        this.sideToMove = b.getSideToMove();
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 2; j++) {
                this.canCastle[i][j] = b.getCastlingPossibilities()[i][j];
            }
        }
        this.move = b.getMoveNumber();
        this.halfmove = b.getHalfMoveNumber();
        this.enPassantTarget = b.getEnPassantSquare();
        
        // copy over move history
        this.moveHistory = new ArrayList<Move>();
        for (Move m : b.getMoveHistory()) {
            this.moveHistory.add(m);
        }

        // copy over repetition table
        this.repetitionTable = new HashMap<String, Integer>();
        for (String s : b.getRepetitionTable().keySet()) {
            this.repetitionTable.put(s, b.getRepetitionTable().get(s));
        }

        // discard any currently cached values as they as probably invalid now
        this.gameStateLastUpdate = -1;
        this.legalMovesLastUpdate = -1;
    }
}
