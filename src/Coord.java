public class Coord {
    private int x;
    private int y;


    public Coord(int x, int y) {
        // check if this coordinate is in bounds / valid
        if (x >= 0 && x <= 7 && y >= 0 && y <= 7) {
            this.x = x;
            this.y = y;
        } else {
            // use fixed values to show that this is invalid so can be easily checked for 
            this.x = -1;
            this.y = -1;
        }
    }

    // converts typical chess coord into (int,int) form
    public Coord(String str) {
        this("abcdefgh".indexOf(str.charAt(0)), 
             Integer.parseInt(Character.toString(str.charAt(1))) - 1);
    }

    public int getX() {
        return this.x;
    }

    public int getY() {
        return this.y;
    }

    public String toString() {
        return "abcdefgh".charAt(this.x) + Integer.toString(y + 1);
    }

    public boolean isInBounds() {
        // when invalid coord is detected, these are set to -1 to signal that
        return !(this.x == -1 || this.y == -1);
    }

    public boolean equals(Coord coord2) {
        return (coord2.getX() == this.x && coord2.getY() == this.y);
    }
}
