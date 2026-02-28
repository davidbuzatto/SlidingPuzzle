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
    
    private static final int[] NEIGHBOR_ROWS = { 0, 1, 0, -1 };
    private static final int[] NEIGHBOR_COLS = { 1, 0, -1, 0 };
    
    private Piece[][] pieces;
    private int pieceWidth;
    private int fontSize;
    private Image image;
    
    private boolean finished;
    private GameState state;
    
    private double xAnimStart;
    private double yAnimStart;
    private double xAnimEnd;
    private double yAnimEnd;
    private Piece movingPiece;
    private double movePieceAnimationTime;
    private double movePieceAnimationCounter;
    
    private boolean stopSolving;
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
                pieces[i][j] = new Piece( i * SIZE + j, j * pieceWidth, i * pieceWidth, pieceWidth, image );
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
            runningSolution = false;
            solutionMoves.clear();
            do {
                shufflePieces( SHUFFLE_COUNT );
                state = GameState.PLAYING;
                checkFinishedAndChangeState();
            } while ( state == GameState.FINISHED );
        }
        
        if ( isKeyPressed( KEY_S ) ) {
            
            do {
                shufflePieces( SHUFFLE_COUNT );
                state = GameState.PLAYING;
                checkFinishedAndChangeState();
            } while ( state == GameState.FINISHED );
            
            // store current state
            Piece[][] currentPieces = new Piece[SIZE][SIZE];
            for ( int i = 0; i < SIZE; i++ ) {
                for ( int j = 0; j < SIZE; j++ ) {
                    currentPieces[i][j] = pieces[i][j];
                }
            }
            
            // solving...
            solve();
            
            // restore state before solving
            for ( int i = 0; i < SIZE; i++ ) {
                for ( int j = 0; j < SIZE; j++ ) {
                    if ( pieces[i][j] != null ) {
                        pieces[i][j] = currentPieces[i][j];
                        pieces[i][j].setPos( j * pieces[i][j].getDim().x, i * pieces[i][j].getDim().y );
                    }
                }
            }
            
            totalMovements = solutionMoves.size();
            runningSolution = true;
            
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
                            p.getPos().x + p.getDim().x / 2 - textBounds.width / 2, 
                            p.getPos().y + p.getDim().y / 2 - textBounds.height / 4, 
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
            
            xAnimStart = col * p.getDim().x;
            yAnimStart = row * p.getDim().y;
            xAnimEnd = targetCol * p.getDim().x;
            yAnimEnd = targetRow * p.getDim().y;
            
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
            p.setPos( targetCol * p.getDim().x, targetRow * p.getDim().y );
            
            pieces[targetRow][targetCol] = p;
            pieces[row][col] = null;
            
        }
        
    }
    
    // gets the empty pos based in a piece position
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
    
    private void shufflePieces( int count ) {
        
        for ( int i = 0; i < count; i++ ) {
            List<Vector2> candidates = getCandidatesToMove();
            int p = MathUtils.getRandomValue( 0, candidates.size() - 1 );
            Vector2 pp = candidates.get( p );
            movePieceToEmptyPos( (int) pp.y, (int) pp.x );
        }
        
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
    
    private void shufflePiecesMaybeUnsolvable() {
        
        for ( int i = 0; i < SIZE; i++ ) {
            for ( int j = 0; j < SIZE; j++ ) {
                int ri = MathUtils.getRandomValue( 0, SIZE - 1 );
                int rj = MathUtils.getRandomValue( 0, SIZE - 1 );
                Piece t = pieces[i][j];
                pieces[i][j] = pieces[ri][rj];
                pieces[ri][rj] = t;
            }
        }
        
        setNullPiece:
        for ( int i = 0; i < SIZE; i++ ) {
            for ( int j = 0; j < SIZE; j++ ) {
                if ( pieces[i][j] == null ) {
                    pieces[i][j] = pieces[SIZE-1][SIZE-1];
                    pieces[SIZE-1][SIZE-1] = null;
                    break setNullPiece;
                }
            }
        }
        
        for ( int i = 0; i < SIZE; i++ ) {
            for ( int j = 0; j < SIZE; j++ ) {
                Piece p = pieces[i][j];
                if ( p != null ) {
                    p.setPos( j * p.getDim().x, i * p.getDim().y );
                }
            }
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
    
    private void solve() {
        
        stopSolving = false;
        visitedStates.clear();
        solutionMoves.clear();
        
        String initialState = getCurrentBoardState();
        visitedStates.add( initialState );
        
        List<Vector2> candidates = getCandidatesToMove();
        
        for ( Vector2 c : candidates ) {
            if ( solveRecurive( c ) ) {
                stopSolving = true;
                break;
            }
        }
        
    }
    
    private boolean solveRecurive( Vector2 pos ) {
        
        if ( stopSolving ) {
            return false;
        }
        
        // for backtracking
        Vector2 backward = getEmptyPos( (int) pos.y, (int) pos.x );
        
        // move
        movePieceToEmptyPos( (int) pos.y, (int) pos.x );
        
        // adds to the solutions (maybe will need to remove)
        solutionMoves.addLast( pos );
        
        // get the current state
        String currentState = getCurrentBoardState();
        
        // already visited?
        if ( visitedStates.contains( currentState ) ) {
            // undo movement
            movePieceToEmptyPos( (int) backward.y, (int) backward.x );
            solutionMoves.removeLast(); // the current pos is not correct
            return false;
        }
        
        // add state
        visitedStates.add( currentState );
        
        // checks solution
        if ( checkFinished() ) {
            stopSolving = true;
            return true;  // solution found!
        }
        
        // recursion
        List<Vector2> candidates = getCandidatesToMove();
        for ( Vector2 c : candidates ) {
            if ( solveRecurive( c ) ) {
                return true; // solution found in a subproblem
            }
        }
        
        // backtracking
        movePieceToEmptyPos( (int) backward.y, (int) backward.x );
        visitedStates.remove( currentState );
        solutionMoves.removeLast(); // the current pos is not correct
        
        // theres no path from here
        return false;
        
    }
    
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
