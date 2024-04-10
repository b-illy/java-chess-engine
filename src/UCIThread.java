import java.util.Scanner;

public class UCIThread extends Thread {
    public UCIThread() {

    }

    public void run() {
        // engine related setup
        Board board = new Board();
        SearchThread searchThread = new SearchThread(board);

        long wtimeMs = 0;
        long btimeMs = 0;
        long wincMs = 0;
        long bincMs = 0;

        // i/o handling
        final Scanner scanner = new Scanner(System.in);

        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();

            switch (line.split(" ")[0]) {
                case "uci":
                    System.out.println("id name java-chess-engine");
                    System.out.println("id author github.com/b-illy");
                    System.out.println("uciok");
                    break;

                case "isready":
                    System.out.println("readyok");
                    break;

                case "register":
                    break;

                case "ucinewgame":
                    break;
                
                case "position":
                    int movesStartIndex = 2;
                    if (line.split(" ")[1].equals("startpos")) {
                        // load default start position
                        board = new Board();
                    } else if (line.split(" ")[1].equals("fen")) {
                        movesStartIndex = 8;

                        // read in next few args for fen string
                        String fen = line.split(" ")[2];
                        for (int i = 3; i < 8; i++) {
                            fen += " " + line.split(" ")[i];
                        }

                        // create new board from fen string
                        board = new Board(fen);
                    }

                    // handle moves
                    if (line.split(" ").length <= movesStartIndex) break;
                    if (!line.split(" ")[movesStartIndex].equals("moves")) {
                        // 'moves' argument expected but something else found, error
                        // TODO
                        break;
                    }

                    for (int i = movesStartIndex+1; i < line.split(" ").length; i++) {
                        Move m = MoveFactory.fromLongAlgebraicStr(line.split(" ")[i], board);
                        m.make();
                    }

                    break;
                
                case "go":
                    short mode = 0; // 0=normal, 1=depth, 2=nodes, 3=movetime, 4=infinite
                    long value = 0; // value to use according to mode

                    // read in options
                    for (int i = 1; i < line.split(" ").length; i++) {
                        switch (line.split(" ")[i]) {
                            case "wtime":
                                wtimeMs = Integer.parseInt(line.split(" ")[i+1]);
                                i++;
                                break;
                            case "btime":
                                btimeMs = Integer.parseInt(line.split(" ")[i+1]);
                                i++;
                                break;
                            case "winc":
                                wincMs = Integer.parseInt(line.split(" ")[i+1]);
                                i++;
                                break;
                            case "binc":
                                bincMs = Integer.parseInt(line.split(" ")[i+1]);
                                i++;
                                break;

                            case "depth":
                                mode = 1;
                                break;
                            case "nodes":
                                mode = 2;
                                break;
                            case "movetime":
                                mode = 3;
                                break;
                            case "infinite":
                                mode = 4;
                                break;

                            default:
                                break;
                        }
                    }

                    // setup search in line with requested options
                    switch(mode) {
                        case 0: // normal
                            searchThread = new SearchThread(board, wtimeMs, btimeMs, wincMs, bincMs);
                            break;
                        case 1: // fixed depth search
                            searchThread = new SearchThread(board, value, (short)0);
                            break;
                        case 2: // fixed node count search
                            searchThread = new SearchThread(board, value, (short)1);
                            break;
                        case 3: // set movetime search (goalTime)
                            searchThread = new SearchThread(board, value, (short)2);
                            break;
                        case 4: // infinite search
                            searchThread = new SearchThread(board);
                            break;
                        default:
                            break;
                    }

                    // go!
                    UCIBackgroundThread bgThread = new UCIBackgroundThread(searchThread);
                    bgThread.start();

                    break;
                
                case "stop":
                    searchThread.sendStopSignal();
                    try {
                        searchThread.join();
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }

                    System.out.println("bestmove " + searchThread.getBestMove());
                    break;

                case "quit":
                    System.exit(100);
                    break;

                default:
                    break;
            }
        }

        scanner.close();
    }
}
