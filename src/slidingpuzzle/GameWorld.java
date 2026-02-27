package slidingpuzzle;

import br.com.davidbuzatto.jsge.core.engine.EngineFrame;
import br.com.davidbuzatto.jsge.image.Image;
import br.com.davidbuzatto.jsge.image.ImageUtils;
import br.com.davidbuzatto.jsge.math.MathUtils;
import br.com.davidbuzatto.jsge.math.Vector2;

/**
 * Sliding Puzzle.
 * 
 * @author Prof. Dr. David Buzatto
 */
public class GameWorld extends EngineFrame {
    
    private static final int SIZE = 3;
    private Piece[][] pieces;
    private Image image;
    private boolean finished;
    
    public GameWorld() {
        super( 600, 600, "Sliding Puzzle", 60, true );
    }
    
    @Override
    public void create() {
        
        pieces = new Piece[SIZE][SIZE];
        int w = getScreenWidth() / SIZE;
        image = ImageUtils.loadImage( "resources/images/prof.png" );
        
        for ( int i = 0; i < SIZE; i++ ) {
            for ( int j = 0; j < SIZE; j++ ) {
                pieces[i][j] = new Piece( i * SIZE + j, j * w, i * w, w, image );
            }
        }
        
        pieces[SIZE-1][SIZE-1] = null;
        checkFinished();
        
        //shufflePieces();
        
    }
    
    @Override
    public void update( double delta ) {
        
        if ( isMouseButtonPressed( MOUSE_BUTTON_LEFT ) ) {
            for ( int i = 0; i < SIZE; i++ ) {
                for ( int j = 0; j < SIZE; j++ ) {
                    if ( pieces[i][j] != null ) {
                        if ( pieces[i][j].checkCollision( getMousePositionPoint() ) ) {
                            movePieceToEmptyPos( i, j );
                        }
                    }
                }
            }
            checkFinished();
        }
        
        if ( isKeyPressed( KEY_R ) ) {
            shufflePieces();
            checkFinished();
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
        
        if ( finished ) {
            drawText( "Finished!", 10, 10, 20, BLUE );
        } else {
            drawText( "Continue!", 10, 10, 20, RED );
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
    
    private Vector2 getEmptyPos( int row, int col ) {
        
        Vector2 pos = new Vector2( -1, -1 );
        
        int[] rows = { 0, 1, 0, -1 };
        int[] cols = { 1, 0, -1, 0 };
        
        for ( int i = 0; i < 4; i++ ) {
            int r = row + cols[i];
            int c = col + rows[i];
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
    
    private void shufflePieces() {
        
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
    
    private void checkFinished() {
        
        finished = true;
        int k = 0;
        
        for ( int i = 0; i < SIZE; i++ ) {
            for ( int j = 0; j < SIZE; j++ ) {
                if ( pieces[i][j] != null && pieces[i][j].getValue() != k ) {
                    finished = false;
                    return;
                }
                k++;
            }
        }
        
    }
    
    public static void main( String[] args ) {
        new GameWorld();
    }
    
}
