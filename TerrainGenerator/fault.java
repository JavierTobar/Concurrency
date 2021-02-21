import java.awt.*;
import java.awt.image.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import javax.imageio.*;

public class fault {

    // Parameters
    public static int n = 1;
    public static int width;
    public static int height;
    public static int threads;
    public static int maxFaultLines;
    public static ArrayList<Block> blocks; // stores height values
    public static Random random;
    public static volatile AtomicInteger faultLineCounter;

    public static long startTime;
    public static long endTime;

    // TAKES W, H, T, K
    // W = width, H = height, T = # Threads, K = # fault lines
    public static void main(String[] args) {
//        try {
            // Note : You might want to check for invalid arguments (?) e.g. width = -1
            if (args.length>0) {
                // initializing our variables
                width = Integer.parseInt(args[0]);
                height = Integer.parseInt(args[1]);
                threads = Integer.parseInt(args[2]);
                maxFaultLines = Integer.parseInt(args[3]);
                faultLineCounter = new AtomicInteger();
                random = new Random();


                // initializing our grid accordingly
                blocks = new ArrayList<Block>();
                for (int i = 0; i < width*height; i++){
                    blocks.add(new Block(i % width, i / width));
                }
            }

            ArrayList<MyThread> threadHolder = new ArrayList<MyThread>();
            for (int i = 0; i < threads; i++)
                threadHolder.add(new MyThread());

            threadHolder.get(0).setName("TheChosenOne"); // the one that will create our image at the end
            startTime = System.currentTimeMillis();
            for (MyThread t : threadHolder)
                t.start();

            // once we know what size we want we can creat an empty image
//            BufferedImage outputimage = new BufferedImage(width,height,BufferedImage.TYPE_INT_ARGB);

            // ------------------------------------
            // The easiest mechanisms for setting and getting pixels are the
            // BufferedImage.setRGB(x,y,value) and getRGB(x,y) functions.
            // Note that setRGB is synchronized (on the BufferedImage object).
            // Consult the javadocs for other methods.

            // The getRGB/setRGB functions return/expect the pixel value in ARGB format, one byte per channel.  For example,
            //  int p = img.getRGB(x,y);
            // With the 32-bit pixel value you can extract individual colour channels by shifting and masking:
            //  int red = ((p>>16)&0xff);
            //  int green = ((p>>8)&0xff);
            //  int blue = (p&0xff);
            // If you want the alpha channel value it's stored in the uppermost 8 bits of the 32-bit pixel value
            //  int alpha = ((p>>24)&0xff);
            // Note that an alpha of 0 is transparent, and an alpha of 0xff is fully opaque.
            // ------------------------------------

            // Write out the image
//            File outputfile = new File("outputimage.png");
//            ImageIO.write(outputimage, "png", outputfile);

//        } catch (Exception e) {
//            System.out.println("ERROR " +e);
//            e.printStackTrace();
//        }
    }


    /**
     * Helper method
     * @return pseudo random int between 0 and 10 inclusive
     */
    private static int GetRandomHeight(){
        return random.nextInt(11);
    }

    /**
     * The thread that will modify our terrain
     * It will pseudo randomly pick whether it should change the points on the left or right
     * if pseudo-random bool is true => right points, else left points
     */
    static class MyThread extends Thread {

        private boolean right; // if true, then we change booleans on the right

        // variables that depict our line
        private int px0;
        private int py0;
        private int px1;
        private int py1;

        // variables for min/max for our image
        private int min;
        private int max;


        /**
         We need to pick two points between 2 distinct bounds/walls.
              3
          0 |---| 2
            |___|
              1
          This is how our bounds are numbered,
          essentially we want to pick 2 distinct numbers to represent our bounds
          and then we pick a random point in that bound, and then we draw our line and do our algorithm

        */

        private void GetRandomLine(){
            int wall1 = random.nextInt(4);
            int wall2 = random.nextInt(4);
            // makes sure we didn't select the same wall/bound
            while (wall1 == wall2)
                wall2 = random.nextInt(4);

            GetRandomPoints(wall1, true);
            GetRandomPoints(wall2, false);
        }

        /**
         * We get random points from the wall ID
         * Boiler plate code to increase readability
         * @param wallID See image above
         * @param firstPoint if firstPoint, then we edit px0 and py0 else px1 and py1
         */
        private void GetRandomPoints(int wallID, boolean firstPoint){
            switch(wallID) {
                case (0):
                    if (firstPoint) {
                        px0 = 0;
                        py0 = random.nextInt(height + 1);
                    } else {
                        px1 = 0;
                        py1 = random.nextInt(height + 1);
                    }
                    break;

                case (1):
                    if (firstPoint) {
                        px0 = random.nextInt(width + 1);
                        py0 = 0;
                    } else {
                        px1 = random.nextInt(width + 1);
                        py1 = 0;
                    }
                    break;

                case(2):
                    if (firstPoint) {
                        px0 = width;
                        py0 = random.nextInt(height + 1);
                    } else {
                        px1 = width;
                        py1 = random.nextInt(height + 1);
                    }
                    break;

                case(3):
                    if (firstPoint) {
                        px0 = random.nextInt(width + 1);
                        py0 = height;
                    } else {
                        px1 = random.nextInt(width + 1);
                        py1 = height;
                    }
                    break;
            }
        }

        /**
         * Very naive implementation : iterate through all the blocks
         * For each block find out if it's a block of interest, i.e. coordinates that we want to modify
         * If no => ignore
         * If yes => See if another thread is modifying the block
         *              if no => increment height and continue
         *              if yes => wait, this is actually ok because you won't wait long
         *
         * The bottleneck about this naive implementation is that you can potentially check
         * for a lot of blocks that aren't of interest, especially if our grid is insanely large.
         */
        @Override
        public void run(){
            // we have to increment it before we get in to prevent concurrency issues
            while(true && faultLineCounter.incrementAndGet() <= maxFaultLines){

//                System.out.println("Fault line # " + String.valueOf(faultLineCounter) + " by " + this.getId());

                int height = random.nextInt(11); // pick pseudo random height from 0 to 10
                GetRandomLine();
                right = random.nextBoolean(); // pseudo random bool

                // standard boilerplate code to increase readability
                if (right){
                    // iterate through all the blocks
                    for (Block block : blocks){
                        if (block.IsRight(px0, py0, px1, py1))
                            block.AddHeight(height);
                    }
                } else { // ELSE we focus on left side of our line
                    for (Block block : blocks){
                        if (!block.IsRight(px0, py0, px1, py1))
                            block.AddHeight(height);
                    }
                }
            }
            if (!this.getName().equals("TheChosenOne"))
                System.out.println("Termination of thread ID: " + this.getId());

            // We get here when we've done all our fault lines, this is the thread responsible for plotting the image
            if (this.getName().equals("TheChosenOne")){
                endTime = System.currentTimeMillis();
                System.out.println("Total ms : " + String.valueOf(endTime-startTime));
                System.out.println("Chosen thread ID : " + this.getId() + " is creating the image");
                min = Integer.MAX_VALUE;
                max = Integer.MIN_VALUE;
                for (Block block : blocks){
                    if (block.height > max)
                        max = block.height;
                    if (block.height < min)
                        min = block.height;
                }

                // We now generate the image
                BufferedImage outputimage = new BufferedImage(width,height,BufferedImage.TYPE_INT_ARGB);
                File outputfile = new File("outputimage.png");

                int color;

                for (Block block : blocks){
                    color = (int) (((block.height*1.0 - min) / (max - min))*255);
                    outputimage.setRGB(block.x, block.y, (new Color(0, color, 0)).getRGB());
                }

                try {
                    ImageIO.write(outputimage, "png", outputfile);
                } catch (IOException e) {
                    System.out.println(e);
                }
                // We decrement by the number of threads since each one of them did getAndIncrement in the while loop before executing a fault line
                faultLineCounter.getAndAdd(-1*threads);
                System.out.println("Fault lines executed : " + String.valueOf(faultLineCounter));

            }
        }
    }

    // This will represent our block object in our grid
    static class Block {

        private int x;
        private int y;
        private int height;
        private boolean right;

        public Block(int px, int py){
            this.x = px;
            this.y = py;
            this.height = 0; // default is 0 anyway
        }

        /**
         * Helper method to increment the height of the block
         *
         * @param pheight determines by how much the height will be increased
         */
        public synchronized void AddHeight(int pheight){
            this.height += pheight;
        }

        /**
         * Determines whether this point is on the right of a given line
         * Note: if the point is colinear then we will consider it AS BEING ON THE RIGHT
         *      Regardless, the colinear case is unimportant for the assignment's concurrency problem
         *
         * @param px0 x coordinate of point 1
         * @param py0 y coordinate of point 1
         * @param px1 x coordinate of point 2
         * @param py1 y coordinate of point 2
         * @return true if this block is on the right of the given line
         */
        public boolean IsRight(int px0, int py0, int px1, int py1){
            // our line is p0 -> p1
            // our point is __this__ instance
            return (px1 - px0) * (this.y - py0) - (this.x - px0) * (py1 - py0) <= 0;
        }



    }

    }
