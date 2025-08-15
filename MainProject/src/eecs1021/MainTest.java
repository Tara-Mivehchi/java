package eecs1021;
import org.junit.Test;
import static org.junit.Assert.*;
public class MainTest {
    private static final int DRY_LINE = 720;
    private static final int WET_LINE = 575;
    // Logic under test
    public static double toPercentage(long raw) {
        double percent = ((double)(DRY_LINE - raw) / (DRY_LINE - WET_LINE)) * 100.0;
        if (percent < 0) return 0;
        if (percent > 100) return 100;
        return (int)Math.round(percent);
    }
    public static String categorizeMoisture(long moisture) {
        if (moisture >= DRY_LINE) return "DRY";
        if (moisture > WET_LINE) return "SLIGHTLY_WET";
        return "WET";
    }
    public static boolean shouldStop(int buttonValue) {
        return buttonValue == 1;
    }
    //Unit Tests
    @Test
    public void testToPercentage() {
        assertEquals(100.0, toPercentage(575), 0.01);
        assertEquals(0.0, toPercentage(720), 0.01);
        assertEquals(50.0, toPercentage(647), 0.1);  // fixed with tolerance
        assertEquals(100.0, toPercentage(500), 0.01); // below wet line
        assertEquals(0.0, toPercentage(800), 0.01);   // above dry line
    }
    @Test
    public void testCategorizeMoisture_Dry() {
        assertEquals("DRY", categorizeMoisture(725));
        assertEquals("DRY", categorizeMoisture(720));
    }
    @Test
    public void testCategorizeMoisture_SlightlyWet() {
        assertEquals("SLIGHTLY_WET", categorizeMoisture(600));
    }
    @Test
    public void testCategorizeMoisture_Wet() {
        assertEquals("WET", categorizeMoisture(575));
        assertEquals("WET", categorizeMoisture(500));
    }
    @Test
    public void testButtonInterrupt() {
        assertTrue(shouldStop(1));   // button pressed
        assertFalse(shouldStop(0));  // button not pressed
    }
}