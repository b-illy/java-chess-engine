// a collection of helper methods relating to bitboards
public class Bitboards {
    public static long toggleBit(long bitboard, int index) {
        if (Bitboards.match(bitboard, index)) {
            // this bit is set to 1, make it 0
            return bitboard ^ (1L << (63-index));
        } else {
            // this bit is set to 0, make it 1
            return bitboard | (1L << (63-index));
        }
    }

    public static long setBit(long bitboard, int index) {
        // if the bit is already set, do nothing
        if (Bitboards.match(bitboard, index)) return bitboard;
        // otherwise, set the bit
        else return bitboard | (1L << (63-index));
    }

    public static long unsetBit(long bitboard, int index) {
        // if the bit is already unset, do nothing
        if (!Bitboards.match(bitboard, index)) return bitboard;
        // otherwise, unset the bit
        else return bitboard ^ (1L << (63-index));
    }

    public static int toIndex(Coord coord) {
        // rotate 90deg clockwise into new coord system
        // https://math.stackexchange.com/questions/1330161/how-to-rotate-points-through-90-degree
        // int x = coord.getY();
        // int y = 7 - coord.getX();
        int x = 7 - coord.getY();
        int y = coord.getX();
        // subtract (3.5,3.5) so that centre of board becomes origin
        // x = x-3.5
        // y = y-3.5
        // perform clockwise rotation around the origin
        // x2 = y-3.5
        // y2 = -(x-3.5) = 3.5-x
        // re-add offset so that centre of board is (3.5,3.5) again, all coords in +/+ quadrant
        // x3 = y
        // y3 = -(x-3.5)+3.5 = 7-x

        return (8*x) + y;
    }

    // wrapper function for below
    public static boolean match(long bitboard, Coord coord) {
        return Bitboards.match(bitboard, Bitboards.toIndex(coord));
    }

    // return true if bit at index ==1, false otherwise (==0)
    public static boolean match(long bitboard, int index) {
        long mask = 1L << (63-index);
        return ((bitboard & mask) == mask);
    }

    // generate bitboard of all white pieces
    public static long whiteSquares(long[] bitboards) {
        long result = bitboards[0];
        for (int i = 1; i < 6; i++) {
            result |= bitboards[i];
        }
        return result;
    }
    
     // generate bitboard of all black pieces
    public static long blackSquares(long[] bitboards) {
        long result = bitboards[6];
        for (int i = 7; i < 12; i++) {
            result |= bitboards[i];
        }
        return result;
    }

    // generate bitboard of all empty squares
    public static long emptySquares(long[] bitboards) {
        // combine white piece and black piece bitboards and then inverse
        return ~(Bitboards.whiteSquares(bitboards) | Bitboards.blackSquares(bitboards));
    }
}
