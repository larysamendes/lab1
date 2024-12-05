import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

/**
 * This class provides functionality to apply a mean filter to an image
 * using multiple threads for concurrent processing.
 */
public class ImageMeanFilter {
    
    /**
     * Applies mean filter to an image using multithreading.
     * 
     * @param inputPath  Path to input image
     * @param outputPath Path to output image 
     * @param kernelSize Size of mean kernel
     * @throws IOException If there is an error reading/writing
     */
    public static void applyMeanFilter(String inputPath, String outputPath, int kernelSize) throws IOException {
        // Load image
        BufferedImage originalImage = ImageIO.read(new File(inputPath));
        
        // Create result image
        BufferedImage filteredImage = new BufferedImage(
            originalImage.getWidth(), 
            originalImage.getHeight(), 
            BufferedImage.TYPE_INT_RGB
        );
        
        int width = originalImage.getWidth();
        int height = originalImage.getHeight();
        
        // Divide image processing into multiple threads
        int numThreads = 4;  // Number of threads (adjust as needed)
        int rowsPerThread = height / numThreads;
        
        // Create threads and assign them work
        Thread[] threads = new Thread[numThreads];
        for (int i = 0; i < numThreads; i++) {
            int startY = i * rowsPerThread;
            int endY = (i == numThreads - 1) ? height : startY + rowsPerThread;
            
            threads[i] = new Thread(new ImageProcessor(originalImage, filteredImage, startY, endY, kernelSize, width));
            threads[i].start();
        }
        
        // Wait for all threads to finish
        for (int i = 0; i < numThreads; i++) {
            try {
                threads[i].join();
            } catch (InterruptedException e) {
                System.err.println("Thread interrupted: " + e.getMessage());
            }
        }
        
        // Save filtered image
        ImageIO.write(filteredImage, "jpg", new File(outputPath));
    }
    
    /**
     * Calculates average colors in a pixel's neighborhood
     * 
     * @param image      Source image
     * @param centerX    X coordinate of center pixel
     * @param centerY    Y coordinate of center pixel
     * @param kernelSize Kernel size
     * @return Array with R, G, B averages
     */
    private static int[] calculateNeighborhoodAverage(BufferedImage image, int centerX, int centerY, int kernelSize) {
        int width = image.getWidth();
        int height = image.getHeight();
        int pad = kernelSize / 2;
        
        // Arrays for color sums
        long redSum = 0, greenSum = 0, blueSum = 0;
        int pixelCount = 0;
        
        // Process neighborhood
        for (int dy = -pad; dy <= pad; dy++) {
            for (int dx = -pad; dx <= pad; dx++) {
                int x = centerX + dx;
                int y = centerY + dy;
                
                // Check image bounds
                if (x >= 0 && x < width && y >= 0 && y < height) {
                    // Get pixel color
                    int rgb = image.getRGB(x, y);
                    
                    // Extract color components
                    int red = (rgb >> 16) & 0xFF;
                    int green = (rgb >> 8) & 0xFF;
                    int blue = rgb & 0xFF;
                    
                    // Sum colors
                    redSum += red;
                    greenSum += green;
                    blueSum += blue;
                    pixelCount++;
                }
            }
        }
        
        // Calculate average
        return new int[] {
            (int)(redSum / pixelCount),
            (int)(greenSum / pixelCount),
            (int)(blueSum / pixelCount)
        };
    }

    /**
     * ImageProcessor is a Runnable class that processes part of the image
     * to apply the mean filter.
     */
    static class ImageProcessor implements Runnable {
        private BufferedImage originalImage;
        private BufferedImage filteredImage;
        private int startY;
        private int endY;
        private int kernelSize;
        private int width;

        public ImageProcessor(BufferedImage originalImage, BufferedImage filteredImage, int startY, int endY, int kernelSize, int width) {
            this.originalImage = originalImage;
            this.filteredImage = filteredImage;
            this.startY = startY;
            this.endY = endY;
            this.kernelSize = kernelSize;
            this.width = width;
        }

        @Override
        public void run() {
            for (int y = startY; y < endY; y++) {
                for (int x = 0; x < width; x++) {
                    // Calculate neighborhood average
                    int[] avgColor = calculateNeighborhoodAverage(originalImage, x, y, kernelSize);
                    
                    // Set filtered pixel in a synchronized block to ensure thread safety
                    synchronized (filteredImage) {
                        filteredImage.setRGB(x, y, 
                            (avgColor[0] << 16) | 
                            (avgColor[1] << 8)  | 
                            avgColor[2]
                        );
                    }
                }
            }
        }
    }
    
    /**
     * Main method for demonstration
     * 
     * Usage: java ImageMeanFilter <input_file>
     * 
     * Arguments:
     *   input_file - Path to the input image file to be processed
     *                Supported formats: JPG, PNG
     * 
     * Example:
     *   java ImageMeanFilter input.jpg
     * 
     * The program will generate a filtered output image named "filtered_output.jpg"
     * using a 7x7 mean filter kernel
     */
    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: java ImageMeanFilter <input_file>");
            System.exit(1);
        }

        String inputFile = args[0];
        try {
            applyMeanFilter(inputFile, "filtered_output.jpg", 7);
        } catch (IOException e) {
            System.err.println("Error processing image: " + e.getMessage());
        }
    }
}
