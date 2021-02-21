import java.util.ArrayList;
import java.util.List;

public class zombie {

    // these variables are shared between different threads
    private static volatile int k; // # threads
    // n is only read by 1 thread, doesn't need to be volatile
    // ideally would need getter for it, but its boilerplate and doesnt matter for the hw
    private static int n; // # zombies threshold
    private static volatile boolean zombiesAllowed;
    private static List<Friend> threads;


    public static synchronized boolean zombiesAllowed(){
        return zombiesAllowed;
    }

    public static synchronized void setZombiesAllowed(boolean pAllowed){
        zombiesAllowed = pAllowed;
    }



    public static void main(String[] args) throws InterruptedException {
        if (args.length != 2) {
            System.out.println("This program requires 2 integer arguments");
            System.exit(0);
        }

        try {
            k = Integer.parseInt(args[0]);
            n = Integer.parseInt(args[1]);
        } catch (Exception e) {
            System.out.println("This program requires 2 integer arguments");
        }
        if (k < 1 || n < 2) {
            System.out.println("This program requires k >= 1 and n >= 2");
            System.exit(0);
        }
        // We get here if arguments we accepted

        threads = new ArrayList<Friend>();

        for (int i = 0; i < k; i++)
            threads.add(new Friend());

        Thread me = new Me();
        me.start();
        threads.forEach(thread -> thread.start());
    }


    // This is us. We're also a thread.
    static class Me extends Thread {

        // our friends only know how many they have let in
        // these dont need to be volatile or synchronized since only Me thread accesses them
        private int totalFromFriends = 0;
        private int zombiesRemoved = 0;
        private int realTotal = 0;
        // used to know when 1s has passed, i.e. to count total
        private int timeCounter = 0;


        @Override
        public void run() {
            while(true) {
//                System.out.println("We think theres " + String.valueOf(realTotal));
                // we only set the flag to true if # zombies < n/2
                if (realTotal < n / 2) {
//                    System.out.println("Zombies allowed");
                    threads.forEach(thread -> thread.setZombiesAllowed(true));

                }


                // no more zombies allowed if # of zombies >= n
                if (realTotal >= n) {
//                    System.out.println("No more zombies allowed");
                    threads.forEach(thread -> thread.setZombiesAllowed(false));
                }


                // 40% chance to remove a zombie if it exists
                if (realTotal > 0 && Math.random() < 0.4){
                    zombiesRemoved++;
                    realTotal--; // what we believe so far, but might not be true right now

                }

                timeCounter += 10;
                // get total after 1 second, i.e. 1000 ms
                if (timeCounter > 1000){
                    timeCounter = 0;
                    getZombies();
                }

                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    System.out.println(e);
                }
            }

        }

        // when we ask our friends how many they've let in
        // we also update the real total here
        private void getZombies(){
            int counter = 0;
            for (Friend f : threads)
                counter += f.getZombiesPassed();
            totalFromFriends = counter;
            realTotal = totalFromFriends - zombiesRemoved; // now we know the REAL total
            System.out.println("Total from friends " + String.valueOf(totalFromFriends));
            System.out.println("Real total " + String.valueOf(realTotal));

        }


    }

    // Our "friends" aka threads
    // We're not doing the runnable interface approach since every
    // friend needs to keep track of certain values
    static class Friend extends Thread {

        private volatile int zombiesPassed = 0;
        private volatile boolean zombiesAllowed = true;

        /**
         * Let's a zombie inside every 10ms with a 10% success rate
         */
        @Override
        public void run() {
            while(true) {
                // Do nothing if no zombies allowed
                if (zombiesAllowed) {
                    // pseudo random 10%
                    if (Math.random() < 0.1) {
                        System.out.println(this.getName() + " : Lets zombie in");
                        zombiesPassed++;
                    }

                    // sleep for 10 ms
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        System.out.println(e);
                    }
                }
            }
        }

        public synchronized void setZombiesAllowed(boolean pAllowed) {
            zombiesAllowed = pAllowed;
        }

        public synchronized int getZombiesPassed() {
            return zombiesPassed;
        }
    }
}
