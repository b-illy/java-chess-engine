import java.util.HashMap;

public class Board {
    private final static String startFEN = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";
    
    private Piece[][] tiles = new Piece[8][8];
    private GameState gameState;
    private Colour sideToMove;
    private Coord enPassantTarget = new Coord(-1,-1);  // placeholder invalid coord
    private int halfmove;
    private int move;
    private HashMap<String, Integer> repetitionTable;
    private boolean[][] canCastle = new boolean[2][2];  // first index is colour, second is direction
                                                        // 0=black, 1=white, 0=short, 1=long
    
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

        // init empty repetition table
        this.repetitionTable = new HashMap<String, Integer>();
        // naively assume game is ongoing -- can't call getGameState here or infinite loop
        this.gameState = GameState.Ongoing;
    }
    
    // constructor for creating board with default position (starting position)
    public Board() {
        this(Board.startFEN);
    }

    // updates game state and then returns it
    public GameState getGameState() {
        // no legal moves, game is over
        if (this.getLegalMoveCount() == 0) {
            // its stalemate if king not in check, checkmate otherwise
            // therefore we need to check if the king is in check right now

            // search for opponents next moves to see if any could capture the king
            boolean isInCheck = this.isSquareAttacked(this.getKing(this.sideToMove).getCoord(), this.sideToMove == Colour.White ? Colour.Black : Colour.White);

            if (isInCheck) {
                this.gameState = (this.sideToMove == Colour.White ? GameState.BlackWon : GameState.WhiteWon);
            } else {
                this.gameState = GameState.Draw;
            }

        } else if (this.repetitionTable.get(this.getStrippedFEN()) != null &&
                   this.repetitionTable.get(this.getStrippedFEN()) >= 3) {
            // if position has been repeated 3 times, draw by repetition
            this.gameState = GameState.Draw;
        } else if (this.halfmove >= 100) {
            // if 50 moves have passed, draw by inactivity
            this.gameState = GameState.Draw;
        } else {
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
                // this tile should contain a letter showing its type and colour
                switch(this.tiles[i][j].getType()) {
                    case king:   charSeq[x] = 'k'; break;
                    case queen:  charSeq[x] = 'q'; break;
                    case rook:   charSeq[x] = 'r'; break;
                    case bishop: charSeq[x] = 'b'; break;
                    case knight: charSeq[x] = 'n'; break;
                    case pawn:   charSeq[x] = 'p'; break;
                    default:     charSeq[x] = '-';
                }

                // make white pieces uppercase
                if (this.tiles[i][j].getColour() == Colour.White) {
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
                        this.tiles[i][x] = new Piece(Colour.None, PieceType.empty, new Coord(x, 7-i), this);
                        if (a < value - 1) x++;
                    }
                } else if ("kqrbnpKQRBNP".indexOf(rows[i].charAt(j)) != -1) {
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

                    Colour colour;

                    if (rows[i].charAt(j) == rows[i].toLowerCase().charAt(j)) {
                        colour = Colour.Black;
                    } else {
                        colour = Colour.White;
                    }

                    this.tiles[i][x] = new Piece(colour, type, new Coord(x, 7-i), this);
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
                Piece t = this.tiles[i][j];  // shorthand shortcut

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

    public boolean[][] getCastlingPossibilities() {
        return this.canCastle;
    }

    // TODO: review necessity, this is currently unused
    public Colour getSideToMove() {
        return this.sideToMove;
    }

    // there are 2 different ways of representing coordinates in this project
    // pseudo coords (intutive human way in line with cartesian graphing):
    // a8=0,7 h8=7,7 a1=0,0 h1=7,0
    // 'actual' coords (optimal for board drawing top-to-bottom, left-to-right):
    // a8=0,0 h8=0,7 a1=7,0 h1=7,7 -- anticlockwise rotate 90deg around centre

    // c1=2,0 h3=7,2 f8=5,7 a6=0,5
    // c1=7,2 h3=5,7 f8=0,5 a6=2,0 -- anticlockwise rotate 90deg around centre (again)
    
    // to convert: use (-y, x) method BUT centre is (3.5, 3.5) instead of origin
    // therefore, subtract (3.5, 3.5) from point, do (-y,x) then re-add
    // c1=2,0 -> c1=-1.5,-3.5 -> c1=3.5,-1.5  -> c1=7,2 -- works!
    // h3=7,2 -> h3=3.5,-1.5  -> h3=1.5,3.5   -> h3=5,7 -- works!
    // a8=0,7 -> a8=-3.5,3.5  -> a8=-3.5,-3.5 -> a8=0,0 -- works! etc.

    // this function maps the intuitive way of thinking of coordinates into the
    // format that the drawing/tostring functions for board positions use to best
    // draw the board to the console. this involves rotate 90 deg anticlockwise
    // around the central point of the board (3.5,3.5) which is explained above
    // briefly and in further detail in my dissertation on this project
    public Piece pieceAt(int x, int y) {
        return this.tiles[7-y][x];
        // this is an optimised and condensed version of the explained process:
        // x -= 3.5;
        // y -= 3.5;
        // finalX = -y + 3.5
        // finalY = x  + 3.5
        // simplified:
        // finalX = -(y-3.5)+3.5
        // finalY = (x-3.5)+3.5
        // further simplified:
        // finalX = -y+7
        // finalY = x
    }

    public boolean isSquareAttacked(Coord atCoord, Colour byColour) {
        Board newBoard = new Board(this.getFEN());  // create temporary clone board
        newBoard.incMoveCount();  // let the opponent move

        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                if (newBoard.pieceAt(i, j).getType() == PieceType.empty || newBoard.pieceAt(i, j).getColour() != byColour) {
                    continue;  // ignore squares without pieces and our own pieces
                } else {
                    // check each candidate move for this piece and see if it could move to the king's square
                    for (Move m : newBoard.pieceAt(i, j).getCandidateMoves()) {
                        if (m.getCoord().equals(atCoord)) {
                            // this move would move onto this square
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    // wrapper for the above function, just passes x and y of the coord into it
    public Piece pieceAt(Coord coord) {
        return this.pieceAt(coord.getX(), coord.getY());
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
        return new Piece(colour, PieceType.empty, new Coord(-1, -1), this);
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
        long count = 0;
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                // filter for pieces of appropriate colour based on turn
                if (this.pieceAt(i, j).getType() != PieceType.empty && this.pieceAt(i, j).getColour() == this.sideToMove) {
                    count += this.pieceAt(i, j).getLegalMoves().size();
                    // for (Move m : this.pieceAt(i, j).getLegalMoves()) {
                    //     System.out.println(m);
                    // }
                }
            }
        }

        return count;
    }
}
