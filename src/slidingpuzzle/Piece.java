package slidingpuzzle;

import br.com.davidbuzatto.jsge.core.engine.EngineFrame;
import br.com.davidbuzatto.jsge.geom.Rectangle;
import br.com.davidbuzatto.jsge.image.Image;
import br.com.davidbuzatto.jsge.math.Vector2;
import java.awt.Color;

/**
 * Represents a puzzle piece.
 * 
 * @author Prof. Dr. David Buzatto
 */
public class Piece {
    
    private int value;
    private Vector2 pos;
    private Vector2 dim;
    private Color color;
    private Image image;
    private String stringValue;
    
    public Piece( int value, int x, int y, int w, Image image ) {
        
        this.value = value;
        this.pos = new Vector2( x, y );
        this.dim = new Vector2( w, w );
        this.color = EngineFrame.BLUE;
        this.image = image;
        
        this.stringValue = String.valueOf( value );
        
    }
    
    public void draw( EngineFrame e, int boardSize ) {
        
        e.fillRectangle( pos, dim, color );
        
        e.drawImage( image, 
            new Rectangle( value % boardSize * dim.x, value / boardSize * dim.y, dim.x, dim.y ), 
            new Rectangle( pos.x, pos.y, dim.x, dim.y ), 
            EngineFrame.WHITE
        );
        
        e.drawRectangle( pos, dim, EngineFrame.BLACK );
        
    }
    
    public boolean checkCollision( Vector2 mousePos ) {
        return mousePos.x >= pos.x && mousePos.x <= pos.x + dim.x &&
               mousePos.y >= pos.y && mousePos.y <= pos.y + dim.y;
    }
    
    public void setPos( double x, double y ) {
        pos.x = x;
        pos.y = y;
    }
    
    public Vector2 getPos() {
        return pos;
    }
    
    public Vector2 getDim() {
        return dim;
    }
    
    public int getValue() {
        return value;
    }
    
    public String getStringValue() {
        return stringValue;
    }

    @Override
    public String toString() {
        return "Piece{" + "value=" + value + ", pos=" + pos + '}';
    }
    
}
