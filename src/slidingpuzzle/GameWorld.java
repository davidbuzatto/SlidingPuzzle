package slidingpuzzle;

import br.com.davidbuzatto.jsge.core.engine.EngineFrame;
import br.com.davidbuzatto.jsge.core.utils.ColorUtils;
import br.com.davidbuzatto.jsge.geom.Rectangle;
import br.com.davidbuzatto.jsge.image.Image;
import br.com.davidbuzatto.jsge.image.ImageUtils;
import br.com.davidbuzatto.jsge.math.MathUtils;
import br.com.davidbuzatto.jsge.math.Vector2;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * Sliding Puzzle.
 * 
 * @author Prof. Dr. David Buzatto
 */
public class GameWorld extends EngineFrame {
    
    private static final int SIZE = 3;
    //private static final int SHUFFLE_COUNT = SIZE * SIZE * SIZE;
    private static final int SHUFFLE_COUNT = SIZE * SIZE;
    private static final boolean DRAW_VALUES = false;
    private static final int MAX_RECURSION_DEPTH = 10000;
    
    private static final int[] NEIGHBOR_ROWS = { 0, 1, 0, -1 };
    private static final int[] NEIGHBOR_COLS = { 1, 0, -1, 0 };
    
    private Piece[][] pieces;
    private int pieceWidth;
    private int fontSize;
    private Image image;
    
    private GameState state;
    
    // for move animation
    private double xAnimStart;
    private double yAnimStart;
    private double xAnimEnd;
    private double yAnimEnd;
    private Piece movingPiece;
    private double movePieceAnimationTime;
    private double movePieceAnimationCounter;
    
    // for automatic solving
    private boolean stopSolving;
    private int currentRecursionDepth;
    private boolean reachedMaxRecursionDepth;
    private Set<String> visitedStates;
    private Deque<Vector2> solutionMoves;
    private boolean runningSolution;
    private int totalMovements;
    
    public GameWorld() {
        super( 600, 600, "Sliding Puzzle", 60, true );
    }
    
    @Override
    public void create() {
        
        pieces = new Piece[SIZE][SIZE];
        pieceWidth = getScreenWidth() / SIZE;
        fontSize = (int) ( pieceWidth / 2.5 );
        image = ImageUtils.loadImage( "resources/images/prof.png" );
        movingPiece = null;
        movePieceAnimationTime = 0.2;
        movePieceAnimationCounter = 0.0;
        
        visitedStates = new HashSet<>();
        solutionMoves = new LinkedList<>();
        runningSolution = false;
        
        for ( int i = 0; i < SIZE; i++ ) {
            for ( int j = 0; j < SIZE; j++ ) {
                pieces[i][j] = new Piece( 
                    i * SIZE + j, 
                    j * pieceWidth, 
                    i * pieceWidth, 
                    pieceWidth,
                    image
                );
            }
        }
        
        pieces[SIZE-1][SIZE-1] = null;
        state = GameState.START;
        
    }
    
    @Override
    public void update( double delta ) {
        
        if ( state == GameState.PLAYING ) {
            
            if ( isMouseButtonPressed( MOUSE_BUTTON_LEFT ) ) {
                search:
                for ( int i = 0; i < SIZE; i++ ) {
                    for ( int j = 0; j < SIZE; j++ ) {
                        if ( pieces[i][j] != null ) {
                            if ( pieces[i][j].checkCollision( getMousePositionPoint() ) ) {
                                movePieceToEmptyPosUsingAnimation( i, j );
                                break search;
                            }
                        }
                    }
                }
            }
            
            if ( !solutionMoves.isEmpty() ) {
                Vector2 pos = solutionMoves.removeFirst();
                movePieceToEmptyPosUsingAnimation( (int) pos.y, (int) pos.x );
            } else {
                runningSolution = false;
            }
        
        } else if ( state == GameState.MOVING ) {
            
            if ( movingPiece != null ) {
                
                movePieceAnimationCounter += delta;
                
                double xAnim = MathUtils.lerp( xAnimStart, xAnimEnd, movePieceAnimationCounter / movePieceAnimationTime );
                double yAnim = MathUtils.lerp( yAnimStart, yAnimEnd, movePieceAnimationCounter / movePieceAnimationTime );
                movingPiece.setPos( xAnim, yAnim );
                
                if ( movePieceAnimationCounter >= movePieceAnimationTime ) {
                    movePieceAnimationCounter = 0;
                    movingPiece.setPos( xAnimEnd, yAnimEnd );
                    state = GameState.PLAYING;
                    checkFinishedAndChangeState();
                }
                
            }
            
        }
        
        if ( isKeyPressed( KEY_R ) ) {
            reachedMaxRecursionDepth = false;
            performSuffle();
        }
        
        if ( isKeyPressed( KEY_S ) ) {
            
            try {
                
                performSuffle();

                // store current state
                Piece[][] currentPieces = new Piece[SIZE][SIZE];
                for ( int i = 0; i < SIZE; i++ ) {
                    for ( int j = 0; j < SIZE; j++ ) {
                        currentPieces[i][j] = pieces[i][j];
                    }
                }

                // solving...
                solve();

                // restore state before solving execution
                for ( int i = 0; i < SIZE; i++ ) {
                    for ( int j = 0; j < SIZE; j++ ) {
                        if ( pieces[i][j] != null ) {
                            pieces[i][j] = currentPieces[i][j];
                            pieces[i][j].setPos( j * pieceWidth, i * pieceWidth );
                        }
                    }
                }

                totalMovements = solutionMoves.size();
                runningSolution = true;
                
            } catch ( IllegalStateException exc ) {
                reachedMaxRecursionDepth = true;
                performSuffle();
            }
            
        }
        
        double m = getMouseWheelMove();
        if ( m > 0 ) {
            movePieceAnimationTime -= 0.05;
            if ( movePieceAnimationTime < 0.016 ) {
                movePieceAnimationTime = 0.016;
            }
        } else if ( m < 0 ) {
            if ( movePieceAnimationTime < 5.0 ) {
                movePieceAnimationTime += 0.05;
            }
        }
        
    }
    
    @Override
    public void draw() {
        
        clearBackground( WHITE );
        
        for ( int i = 0; i < SIZE; i++ ) {
            for ( int j = 0; j < SIZE; j++ ) {
                if ( pieces[i][j] != null ) {
                    pieces[i][j].draw( this, SIZE );
                }
            }
        }
        
        if ( DRAW_VALUES ) {
            for ( int i = 0; i < SIZE; i++ ) {
                for ( int j = 0; j < SIZE; j++ ) {
                    Piece p = pieces[i][j];
                    if ( p != null ) {
                        Rectangle textBounds = measureTextBounds( p.getStringValue(), fontSize );
                        drawText( 
                            p.getStringValue(), 
                            p.getPos().x + pieceWidth / 2 - textBounds.width / 2, 
                            p.getPos().y + pieceWidth / 2 - textBounds.height / 4, 
                            fontSize, 
                            WHITE
                        );
                    }
                }
            }
        }
        
        if ( state == GameState.START || state == GameState.FINISHED ) {
            
            fillRectangle( 0, 0, getScreenWidth(), getScreenHeight(), ColorUtils.fade( BLACK, 0.5 ) );
            int messageFontSize = 60;
            
            String wonMessage = state == GameState.FINISHED ? "You Won!" : "Let's Play!";
            String restartMessage = state == GameState.FINISHED ? "Press <R> to Shuffle!" : "Press <R> to Start!";
            
            drawText( 
                wonMessage, 
                getScreenWidth() / 2 - measureText( wonMessage, messageFontSize ) / 2, 
                getScreenHeight() / 2 - messageFontSize / 2, 
                messageFontSize, 
                state == GameState.FINISHED ? BLUE : GREEN
            );
            
            drawText( 
                restartMessage, 
                getScreenWidth() / 2 - measureText( restartMessage, messageFontSize / 2 ) / 2, 
                getScreenHeight() / 2 + messageFontSize / 2, 
                messageFontSize / 2, 
                WHITE
            );
            
        }
        
        if ( runningSolution ) {
            drawText( String.format( "%d/%d", totalMovements - solutionMoves.size(), totalMovements ), 10, 10, 20, GREEN );
        }
        
        if ( reachedMaxRecursionDepth ) {
            fillRectangle( 0, 0, getScreenWidth(), getScreenHeight(), ColorUtils.fade( BLACK, 0.5 ) );
            drawText( String.format( "Reached the maximum recursion depth (%d)...\nTry again!", MAX_RECURSION_DEPTH ), 10, 10, 20, RED );
        }
    
    }
    
    private void movePieceToEmptyPosUsingAnimation( int row, int col ) {
        
        Vector2 targetPos = getEmptyPos( row, col );
        
        int targetRow = (int) targetPos.y;
        int targetCol = (int) targetPos.x;
        
        movingPiece = null;
        
        if ( targetRow != -1 ) {
            
            Piece p = pieces[row][col];
            
            pieces[targetRow][targetCol] = p;
            pieces[row][col] = null;
            
            xAnimStart = col * pieceWidth;
            yAnimStart = row * pieceWidth;
            xAnimEnd = targetCol * pieceWidth;
            yAnimEnd = targetRow * pieceWidth;
            
            movingPiece = p;
            
            state = GameState.MOVING;
            
        }
        
    }
    
    private void movePieceToEmptyPos( int row, int col ) {
        
        Vector2 targetPos = getEmptyPos( row, col );
        
        int targetRow = (int) targetPos.y;
        int targetCol = (int) targetPos.x;
        
        if ( targetRow != -1 ) {
            
            Piece p = pieces[row][col];
            p.setPos( targetCol * pieceWidth, targetRow * pieceWidth );
            
            pieces[targetRow][targetCol] = p;
            pieces[row][col] = null;
            
        }
        
    }
    
    // gets the empty position based in a piece position
    private Vector2 getEmptyPos( int row, int col ) {
        
        Vector2 pos = new Vector2( -1, -1 );
        
        for ( int i = 0; i < 4; i++ ) {
            int r = row + NEIGHBOR_COLS[i];
            int c = col + NEIGHBOR_ROWS[i];
            if ( r >= 0 && r < SIZE && c >= 0 && c < SIZE ) {
                if ( pieces[r][c] == null ) {
                    pos.x = c;
                    pos.y = r;
                    break;
                }
            }
        }
        
        return pos;
        
    }
    
    // get the empty position
    private Vector2 getEmptyPos() {
        
        int row = -1;
        int col = -1;
        
        getNullPos:
        for ( int i = 0; i < SIZE; i++ ) {
            for ( int j = 0; j < SIZE; j++ ) {
                if ( pieces[i][j] == null ) {
                    row = i;
                    col = j;
                    break getNullPos;
                }
            }
        }
        
        return new Vector2( col, row );
        
    }
    
    // based in the empty position, get all surrounding positions
    // that can be moved
    private List<Vector2> getCandidatesToMove() {
        
        List<Vector2> candidates = new ArrayList<>();
        Vector2 nullPos = getEmptyPos();
        int row = (int) nullPos.y;
        int col = (int) nullPos.x;
        
        for ( int i = 0; i < 4; i++ ) {
            int r = row + NEIGHBOR_COLS[i];
            int c = col + NEIGHBOR_ROWS[i];
            if ( r >= 0 && r < SIZE && c >= 0 && c < SIZE ) {
                candidates.add( new Vector2( c, r ) );
            }
        }
        
        return candidates;
        
    }
    
    private void performSuffle() {
        runningSolution = false;
        solutionMoves.clear();
        do {
            shufflePieces( SHUFFLE_COUNT );
            state = GameState.PLAYING;
            checkFinishedAndChangeState();
        } while ( state == GameState.FINISHED );
    }
    
    // suffle pieces applying "count" movements
    private void shufflePieces( int count ) {
        
        // perform the suffle
        for ( int i = 0; i < count; i++ ) {
            List<Vector2> candidates = getCandidatesToMove();
            int p = MathUtils.getRandomValue( 0, candidates.size() - 1 );
            Vector2 pp = candidates.get( p );
            movePieceToEmptyPos( (int) pp.y, (int) pp.x );
        }
        
        // returns the empty position to the bottom right corner
        Vector2 nullPos = getEmptyPos();
        int row = (int) nullPos.y;
        int col = (int) nullPos.x;
        
        int qRow = SIZE - row - 1;
        int qCol = SIZE - col - 1;
        
        for ( int i = 0; i < qRow; i++ ) {
            movePieceToEmptyPos( row + 1, col);
            row++;
        }
        
        for ( int i = 0; i < qCol; i++ ) {
            movePieceToEmptyPos( row, col + 1 );
            col++;
        }
        
    }
    
    private void checkFinishedAndChangeState() {
        state = checkFinished() ? GameState.FINISHED : state;
    }
    
    private boolean checkFinished() {
        
        int k = 0;
        
        for ( int i = 0; i < SIZE; i++ ) {
            for ( int j = 0; j < SIZE; j++ ) {
                if ( pieces[i][j] != null && pieces[i][j].getValue() != k ) {
                    return false;
                }
                k++;
            }
        }
        
        return true;
        
    }
    
    // perform the solve algorithm
    private void solve() throws IllegalStateException {
        
        stopSolving = false;
        currentRecursionDepth = 0;
        reachedMaxRecursionDepth = false;
        visitedStates.clear();
        solutionMoves.clear();
        
        // stores the initial state
        String initialState = getCurrentBoardState();
        visitedStates.add( initialState );
        
        // get the initial moving candidates
        List<Vector2> candidates = getCandidatesToMove();
        
        for ( Vector2 c : candidates ) {
            if ( solveRecurive( c ) ) {
                stopSolving = true;
                break;
            }
        }
        
    }
    
    // perform the solve algorithm using backtracking based in a position
    private boolean solveRecurive( Vector2 pos ) throws IllegalStateException {
        
        currentRecursionDepth++;
        
        if ( currentRecursionDepth > MAX_RECURSION_DEPTH ) {
            stopSolving = true;
            throw new IllegalStateException( "reached max recursion depth!" );
        }
        
        // stop trying to solve
        if ( stopSolving ) {
            return false;
        }
        
        // for backtracking, stores the backward movement before starting
        Vector2 backward = getEmptyPos( (int) pos.y, (int) pos.x );
        
        // move the current piece to the empty space
        movePieceToEmptyPos( (int) pos.y, (int) pos.x );
        
        // adds to the solutions (maybe will need to remove)
        solutionMoves.addLast( pos );
        
        // get the current state
        String currentState = getCurrentBoardState();
        
        // this state was already processed?
        if ( visitedStates.contains( currentState ) ) {
            
            // undo movement
            movePieceToEmptyPos( (int) backward.y, (int) backward.x );
            
            // remove the move, because it is not correct
            solutionMoves.removeLast();
            
            // stop the current solution try
            return false;
            
        }
        
        // ok, this movement shows promise!
        
        // add the state
        visitedStates.add( currentState );
        
        // checks solution
        if ( checkFinished() ) {
            // solution found, stop and signals other calls to stop
            stopSolving = true;
            return true;
        }
        
        // recursion, trying to find the solution in subproblems
        List<Vector2> candidates = getCandidatesToMove();
        for ( Vector2 c : candidates ) {
            if ( solveRecurive( c ) ) {
                return true; // solution found in a subproblem
            }
        }
        
        // no solution found in subproblems, so the current path for
        // problem solving is incorret
        movePieceToEmptyPos( (int) backward.y, (int) backward.x );
        //visitedStates.remove( currentState );
        solutionMoves.removeLast();
        
        currentRecursionDepth--;
        
        // theres no path from here
        return false;
        
    }
    
    // creates a string for the current board state representation
    private String getCurrentBoardState() {
        StringBuilder sb = new StringBuilder();
        for ( int i = 0; i < SIZE; i++ ) {
            for ( int j = 0; j < SIZE; j++ ) {
                if ( pieces[i][j] != null )  {
                    sb.append( pieces[i][j].getValue() ).append( "," );
                } else {
                    sb.append( "null," );
                }
            }
        }
        return sb.toString();
    }
    
    public static void main( String[] args ) {
        new GameWorld();
    }
    
}
