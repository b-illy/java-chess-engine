// a collection of helper methods relating to bitboards
public class Bitboards {
    public final static void print(long bitboard) {
        for (int i = 0; i < 64; i++) {
            if (i != 0 && i % 8 == 0) System.out.print("\n");
            System.out.print(Bitboards.match(bitboard, i) ? 1 : 0);
        }
        System.out.print("\n");
    }


    // single bit modifications methods

    public final static long toggleBit(long bitboard, int index) {
        return bitboard ^ (1L << (63-index));
    }

    public final static long setBit(long bitboard, int index) {
        return bitboard | (1L << (63-index));
    }

    public final static long unsetBit(long bitboard, int index) {
        return bitboard & ~(1L << (63-index));
    }


    // index handling methods

    public final static int toIndex(Coord coord) {
        return (8*coord.getY()) + coord.getX();
    }

    public final static int toIndex(int x, int y) {
        return 8*y + x;
    }

    public final static Coord toCoord(int index) {
        return new Coord(index % 8, index / 8);
    }



    // bit value checking
    
    // returns true if bit at index ==1, false otherwise (==0)
    public final static boolean match(long bitboard, int index) {
        long mask = 1L << (63-index);
        return ((bitboard & mask) == mask);
    }
    
    public final static boolean match(long bitboard, Coord coord) {
        return Bitboards.match(bitboard, Bitboards.toIndex(coord));
    }


    // colour-based compositing methods

    // generate bitboard of all white pieces
    public final static long whiteSquares(long[] bitboards) {
        long result = bitboards[0];
        for (int i = 1; i < 6; i++) {
            result |= bitboards[i];
        }
        return result;
    }
    
     // generate bitboard of all black pieces
    public final static long blackSquares(long[] bitboards) {
        long result = bitboards[6];
        for (int i = 7; i < 12; i++) {
            result |= bitboards[i];
        }
        return result;
    }

    // generate bitboard of all empty squares
    public final static long emptySquares(long[] bitboards) {
        // combine white piece and black piece bitboards and then inverse
        return ~(Bitboards.whiteSquares(bitboards) | Bitboards.blackSquares(bitboards));
    }


    // mask constants

    public final static long fileMask = 0x0101010101010101L;
    public final static long rankMask = 0xff00000000000000L;


    // mask generating methods

    // TODO: review necessity
    public final static long squareMask(int index) {
        return 1L << (63-index);
    }

    public final static long fileMask(int index) {
        return fileMask << (7-(index%8));
    }

    public final static long rankMask(int index) {
        return rankMask >>> (index/8)*8;
    }

    // public final static long diagonalMask(int index) {
    //     final long mask1 = 0x8040201008040201L;
    //     final long mask2 = 0x0102040880402010L;
    // }

    public final static long kingMoveMask(int index) {
        // mask for king moves 1 square all around
        long mask = 0b1110000010100000111L;
        final int x = index % 8;
        final int y = index / 8;

        if (x == 0) mask = 0b0110000000100000011L;
        else if (x == 7) mask = 0b1100000010000000110L;

        // shift across and down an extra square to centre the mask
        int shiftAmount = (6-x) + 8*(6-y);
        if (shiftAmount < 0) return mask >>> -shiftAmount;
        return mask << shiftAmount;
    }

    public final static long knightMoveMask(int index) {
        long mask = 0b101000010001000000000001000100001010L;
        final int x = index % 8;
        final int y = index / 8;

        int shiftAmount = (5-x) + 8*(5-y);
        if (shiftAmount < 0) mask >>>= -shiftAmount;
        else mask <<= shiftAmount;

        if (x < 2) mask &= ~fileMask(index-2);
        if (x < 1) return mask & ~fileMask(index-1);
        if (x > 5) mask &= ~fileMask(index+2);
        if (x > 6) return mask & ~fileMask(index+1);

        return mask;
    }

    public final static long pawnAttacksL(long bitboard, boolean forwards) {
        if (forwards) bitboard <<= 9;
        else bitboard >>>= 7;

        // exclude moves that wrapped around
        return bitboard & ~fileMask;
    }

    public final static long pawnAttacksR(long bitboard, boolean forwards) {
        if (forwards) bitboard <<= 7;
        else bitboard >>>= 9;

        // exclude moves that wrapped around
        return bitboard & ~(fileMask << 7);
    }

    public final static long passedPawnMaskWhite(int index) {
        final int x = index % 8;
        final int y = index / 8;

        final long fileMask = Bitboards.fileMask(index);
        final long fileMaskL = (x == 0) ? 0 : fileMask << 1;
        final long fileMaskR = (x == 7) ? 0 : fileMask >>> 1;

        return (fileMask|fileMaskL|fileMaskR) >>> 8*(y+1);
    }

    public final static long passedPawnMaskBlack(int index) {
        final int x = index % 8;
        final int y = index / 8;

        final long fileMask = Bitboards.fileMask(index);
        final long fileMaskL = (x == 0) ? 0 : fileMask << 1;
        final long fileMaskR = (x == 7) ? 0 : fileMask >>> 1;

        return (fileMask|fileMaskL|fileMaskR) << 8*(y+1);
    }


    // bitboard manipulating methods

    public final static long cropBorders(long bitboard) {
        final long mask = 0xff818181818181ffL;
        return bitboard & ~mask;
    }
}
