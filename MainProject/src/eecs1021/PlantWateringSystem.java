package eecs1021;
import org.firmata4j.firmata.FirmataDevice;
import org.firmata4j.IODevice;
import org.firmata4j.I2CDevice;
import org.firmata4j.ssd1306.SSD1306;
import org.firmata4j.Pin;
import edu.princeton.cs.introcs.StdDraw;
import java.io.IOException;
import java.util.ArrayList;
public class PlantWateringSystem {
    private static final int SENSOR_PIN = 15;
    private static final int PUMP_PIN = 7;
    private static final int BUTTON_PIN = 6;
    private static final byte DISPLAY_I2C_ADDRESS = 0x3C;
    private static final int DRY_LINE = 720; // Sensor value when fully dry
    private static final int WET_LINE = 575; // Sensor value when fully wet
    private static IODevice microcontroller;
    private static SSD1306 display;
    private static ArrayList<Long> moistureLevels = new ArrayList<>();
    private static ArrayList<Double> timeStamps = new ArrayList<>();
    private static double graphTimeWindow = 60;
    public static void main(String[] args) throws IOException, InterruptedException {
        microcontroller = new FirmataDevice("/dev/cu.usbserial-0001");
        microcontroller.start();
        microcontroller.ensureInitializationIsDone();
        I2CDevice i2cDevice = microcontroller.getI2CDevice(DISPLAY_I2C_ADDRESS);
        display = new SSD1306(i2cDevice, SSD1306.Size.SSD1306_128_64);
        display.init();
        Pin moistureSensor = microcontroller.getPin(SENSOR_PIN);
        Pin pump = microcontroller.getPin(PUMP_PIN);
        Pin button = microcontroller.getPin(BUTTON_PIN);
        moistureSensor.setMode(Pin.Mode.ANALOG);
        pump.setMode(Pin.Mode.OUTPUT);
        button.setMode(Pin.Mode.INPUT);
        double startTime = System.currentTimeMillis() / 1000.0;
        double nextReadingTime = 0;
        while (true) {
            double currentTime = System.currentTimeMillis() / 1000.0 - startTime;
            if (currentTime >= nextReadingTime) {
                long currentMoisture = moistureSensor.getValue();
                moistureLevels.add(currentMoisture);
                timeStamps.add(currentTime);
                renderGraph();
                System.out.println("Current Moisture Level: " + currentMoisture);
                if (currentMoisture >= DRY_LINE) {
                    pump.setValue(1);
                    Thread.sleep(3000);
                    updateDisplay(currentMoisture, true, "Soil is dry. Pump is ON.");
                } else if (currentMoisture > WET_LINE) {
                    pump.setValue(1);
                    updateDisplay(currentMoisture, true, "Soil is a little wet. Light watering.");
                    Thread.sleep(1000);
                    pump.setValue(0);
                    updateDisplay(currentMoisture, false, "Pump OFF after light watering.");
                } else {
                    pump.setValue(0);
                    updateDisplay(currentMoisture, false, "Soil is wet. No watering needed.");
                }
                nextReadingTime += 5; // update every 5 seconds
            }
            if (button.getValue() == 1) {
                pump.setValue(0);
                updateDisplay(moistureLevels.get(moistureLevels.size() - 1), false, "Button pressed. Stopping system...");
                System.out.println("Button pressed. Stopping the pump and system.");
                break;
            }
            Thread.sleep(200); // for button
        }
        System.out.println("Loop finished. Stopping the system.");
        display.getCanvas().clear();
        display.display();
        microcontroller.stop();
    }
    private static double calculateMoisturePercentage(long currentMoisture) {
        double percentage = ((double)(DRY_LINE - currentMoisture) / (DRY_LINE - WET_LINE)) * 100.0;
        if (percentage < 0) percentage = 0;
        if (percentage > 100) percentage = 100;
        return percentage;
    }
    private static void initializeGraph(double maxTime) {
        StdDraw.setCanvasSize(1000, 500);
        StdDraw.setXscale(0, maxTime);
        StdDraw.setYscale(0, 100);
        StdDraw.setPenRadius(0.003);
        StdDraw.clear();
        StdDraw.setPenColor(StdDraw.MAGENTA);
        StdDraw.line(0, 0, maxTime, 0);
        StdDraw.line(0, 0, 0, 100);
        StdDraw.text(maxTime / 2, 100, "Soil Moisture Over Time");
        StdDraw.text(6, 95, "Moisture (%)", 0);
        StdDraw.text(maxTime / 2, -5, "Time (s)");
        for (int i = 0; i <= maxTime; i += 5) {
            StdDraw.text(i, -2, String.valueOf(i));
        }
        for (int i = 0; i <= 100; i += 10) {
            StdDraw.text(0, i, String.valueOf(i));
        }
    }
    private static void renderGraph() {
        double maxTime = Math.max(60, timeStamps.get(timeStamps.size() - 1) + 10);
        initializeGraph(maxTime);
        StdDraw.setPenColor(StdDraw.PINK);
        int size = moistureLevels.size();
        for (int i = 1; i < size; i++) {
            double x1 = timeStamps.get(i - 1);
            double y1 = calculateMoisturePercentage(moistureLevels.get(i - 1));
            double x2 = timeStamps.get(i);
            double y2 = calculateMoisturePercentage(moistureLevels.get(i));
            StdDraw.line(x1, y1, x2, y2);
        }
        StdDraw.show();
    }
    private static void updateDisplay(long currentMoisture, boolean pumpOn, String statusMessage) {
        display.getCanvas().clear();
        display.getCanvas().setTextsize(1);
        String line1 = "Moisture: " + currentMoisture;
        String line2 = "Pump: " + (pumpOn ? "ON - Watering" : "OFF");
        String line3 = statusMessage;
        display.getCanvas().drawString(0, 48, line1);
        display.getCanvas().drawString(0, 32, line2);
        display.getCanvas().drawString(0, 16, line3);
        display.display();
        System.out.println(line1 + " | " + line2 + " | " + line3);
    }
}