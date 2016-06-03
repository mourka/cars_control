//Prototype implementation of Car Control
//Mandatory assignment
//Course 02158 Concurrent Programming, DTU   Fall 2012


import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.*;

class Gate {

    Semaphore g = new Semaphore(0);
    Semaphore e = new Semaphore(1);
    boolean isopen = false;

    public void pass() throws InterruptedException {
        g.P();
        g.V();
    }

    public void open() {
        try {
            e.P();
        } catch (InterruptedException e) {
        }
        if (!isopen) {
            g.V();
            isopen = true;
        }
        e.V();
    }

    public void close() {
        try {
            e.P();
        } catch (InterruptedException e) {
        }
        if (isopen) {
            try {
                g.P();
            } catch (InterruptedException e) {
            }
            isopen = false;
        }
        e.V();
    }

}

class Car extends Thread {

    volatile int basespeed = 100;    // Rather: degree of slowness
    volatile int variation = 50;    // Percentage of base speed

    CarDisplayI cd;                  // GUI part

    int no;                          // Car number
    Pos startpos;                    // Startpositon (provided by GUI)
    Pos barpos;                      // Barrierpositon (provided by GUI)
    Color col;                       // Car  color
    Gate mygate;                     // Gate at startposition


    int speed;                       // Current car speed
    Pos curpos;                      // Current position 
    Pos newpos;                      // New position to go to

    Alley alley;
    Map<Pos, Semaphore> semaphores;
    private Barrier barrier;

    public Car(int no, CarDisplayI cd, Gate g, Alley alley, Map<Pos, Semaphore> semaphores, Barrier barrier) {

        this.no = no;
        this.cd = cd;
        mygate = g;
        startpos = cd.getStartPos(no);
        barpos = cd.getBarrierPos(no);  // For later use
        col = chooseColor();
        this.alley = alley;
        this.semaphores = semaphores;
        this.barrier = barrier;

        // do not change the special settings for car no. 0
        if (no == 0) {
            basespeed = 0;
            variation = 0;
            setPriority(Thread.MAX_PRIORITY);
        }
    }

    public synchronized void setSpeed(int speed) {
        if (no != 0 && speed >= 0) {
            basespeed = speed;
        } else
            cd.println("Illegal speed settings");
    }

    public synchronized void setVariation(int var) {
        if (no != 0 && 0 <= var && var <= 100) {
            variation = var;
        } else
            cd.println("Illegal variation settings");
    }

    synchronized int chooseSpeed() {
        double factor = (1.0D + (Math.random() - 0.5D) * 2 * variation / 100);
        return (int) Math.round(factor * basespeed);
    }

    private int speed() {
        // Slow down if requested
        final int slowfactor = 3;
        return speed * (cd.isSlow(curpos) ? slowfactor : 1);
    }

    Color chooseColor() {
        return Color.blue; // You can get any color, as longs as it's blue 
    }

    Pos nextPos(Pos pos) {
        // Get my track from display
        return cd.nextPos(no, pos);
    }

    boolean atGate(Pos pos) {
        return pos.equals(startpos);
    }

    public void run() {
        try {

            speed = chooseSpeed();
            curpos = startpos;
            cd.mark(curpos, col, no);

            while (true) {
                sleep(speed());

                if (atGate(curpos)) {
                    mygate.pass();
                    speed = chooseSpeed();
                }


                newpos = nextPos(curpos);
                if (atBarrier(newpos)) {
                    barrier.sync(no);
                }

                if (!inAlley(curpos) && inAlley(newpos)) {
                    alley.enter(no);
                }

                if (inAlley(curpos) && !inAlley(newpos)) {
                    alley.leave(no);
                }

                semaphores.get(newpos).P();
                //  Move to new position
                cd.clear(curpos);
                cd.mark(curpos, newpos, col, no);

                sleep(speed());

                cd.clear(curpos, newpos);
                cd.mark(newpos, col, no);
                semaphores.get(curpos).V();
                curpos = newpos;

            }

        } catch (Exception e) {
            cd.println("Exception in Car no. " + no);
            System.err.println("Exception in Car no. " + no + ":" + e);
            e.printStackTrace();
        }
    }

    public boolean inAlley(Pos position) {

        List<Pos> alleyPositions = new ArrayList<Pos>();
        for (int i = 0; i < 11; i++) {
            alleyPositions.add(new Pos(i, 0));
        }
        for (int j = 1; j < 3; j++) {
            alleyPositions.add(new Pos(9, j));
//           alleyPositions.add(new Pos(8,j));
        }

        return alleyPositions.contains(position);
    }

    private boolean atBarrier(Pos position) {

        if (this.no < 5) {
            if (position.row == 5 && position.col > 2) {
                return true;
            }
        } else {
            if (position.row == 4 && position.col > 2) {
                return true;
            }
        }
        return false; 
    }

}

class Barrier {

    private static Semaphore[] arrive = new Semaphore[9];
    private boolean barrierOn;
    private int totalCars=0;
    private int carNo;
    private int carslimit=8;  //if we have 8 cars and no one set the barrier threshold
    private int threshold=0;   //if no one set the barrier threshold-> it is 0.
    private boolean barrierdecreased=false;
    private boolean barrierincreased=false;
    private int nextthreshold=0;
    //private boolean firsttime=false;

    
    public Barrier() {
        barrierOn = false;
        for (int i=0;i<9;i++) {
            arrive[i] = new Semaphore(0);
        }
    }

    public synchronized void sync(int carNo1){
    	this.carNo = carNo1;
    	//if we have 9 cars and no one set the barrier threshold
    	if(carNo==0 && threshold==0){ 
    		carslimit=9;
    	}
    	
        if(!barrierOn) {
        	notifyAll();
			totalCars=0;
        	//setThreshold(0);
            return;
        }        
        else{
        	
        	if(barrierdecreased==true){  //if barrierthreshold decreased (in the meanwhile)
        		notifyAll();  //release all cars
        	    totalCars=0;
        	   /* if(firsttime=true){
        	    	carslimit=8;
        	    	firsttime=false;
        	    }else{*/
        	        carslimit=threshold; //set new threshold
        	   // }
        	    barrierdecreased=false;
        	}
        	
        	totalCars++;
        	
    		if(totalCars==carslimit){  
    			notifyAll();
    			totalCars=0;

    			if(threshold==0){   //if no one set the barrier threshold & we have 8 cars
    			   carslimit=8;
    			}else{
    				if(barrierincreased==true){     //If someone increased the threshold
    	        			threshold=nextthreshold;
    	        			carslimit=threshold;
    	        			nextthreshold=0;
    	        			barrierincreased=false;
    	        	}else{    				//if threshold is set but not altered
    				   carslimit=threshold;  }
    			}
    		}else{  //if we haven't rechead carslimit (=total cars waiting at the barrier) yet
		    	try {
					wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
    		}
        }
    }

    public void on() {
        for (int i=0;i<9;i++) {
            arrive[i] = new Semaphore(0);
        }
       barrierOn = true;
    }

    public void off() {
    	barrierOn = false;

        for(Semaphore semaphore: arrive) {
            semaphore.V();
        }
    }
    
    public void setThreshold(int k){
    	if(this.threshold!=0){  //if barrier threshold was already set up
	    	if(nextthreshold==0){	//If this value !=0, then there was an increment in threshold before
	    		if(k<this.threshold){  //if new value is smaller than previous
		    	   this.threshold = k;
		    	   this.barrierdecreased=true;
		    	}else{  //if new value is bigger than previous
		    	//	save the new/next barrierthreshold and don't change it again unless implemented
		    		if(k>this.threshold){
		    		   this.nextthreshold=k;   //new/next barrierthreshold
		    		   this.barrierincreased=true;   //will be implemented when this becomes false
		    		}   
	    	    }
    	    }	
    	}else{  //if no one set the barrier threshold before then it is automatically set to 9
    		this.threshold = k;   //this command has effect the 1st time we change the threshold
    		this.carslimit = k;    //if you enable =>this then carslimit=threshold=9 (for some reason) 
    		//this.barrierdecreased=true;  //or this    & it gets blocked on barrier waiting for 9 cars
    	}
    	
    }
}

class Alley {

	private volatile int nrUp ;
    private volatile int nrDown ;

    public Alley() {
        nrUp = 0;
        nrDown = 0;
    }

    public void enter(int no) {
       
            if (no < 5) {
            	synchronized (this){
                    try {
                        while (nrDown > 0)
                            wait();
                        //critical ...
                        ++nrUp;

                    } catch (InterruptedException ex) {
                        System.out.println("interrupted occured");
                    }             
            }
            } else {
            	synchronized (this){
                    try {
                        while (nrUp > 0)
                            wait();
                        //critical ...
                        ++nrDown;

                    } catch (InterruptedException ex) {
                        System.out.println("interrupted occured");
                    }
                }
            }
    }

    public void leave(int no) {
        if (no < 5) {
        	synchronized (this){        
                  --nrUp;
                  if (nrUp == 0){
                      notify();                     
                  }
              }
        } else {
        	synchronized (this){ 
                --nrDown;
                if (nrDown == 0){
                    notifyAll();
                }
            }
        }
    }
}    

public class CarControl implements CarControlI {

    CarDisplayI cd;           // Reference to GUI
    Car[] car;               // Cars
    Gate[] gate;              // Gates
    private final Alley alley;
    private Map<Pos, Semaphore> semaphoreMap;
    private final Barrier barrier;

    public CarControl(CarDisplayI cd) {
        this.cd = cd;
        car = new Car[9];
        gate = new Gate[9];
        alley = new Alley();
        barrier = new Barrier();
        semaphoreMap = new HashMap<Pos, Semaphore>();
        for (int i = 0; i < 11; i++) {
            for (int j = 0; j < 12; j++) {
                semaphoreMap.put(new Pos(i, j), new Semaphore(1));
            }
        }


        for (int no = 0; no < 9; no++) {
            gate[no] = new Gate();
            car[no] = new Car(no, cd, gate[no], alley, semaphoreMap, barrier);
            car[no].start();
        }
    }

    public void startCar(int no) {
        gate[no].open();
    }

    public void stopCar(int no) {
        gate[no].close();
    }

    public void barrierOn() {
        //cd.println("Barrier On not implemented in this version");
        this.barrier.on();

    }

    public void barrierOff() {
        //cd.println("Barrier Off not implemented in this version");
        this.barrier.off();
    }

    public void barrierSet(int k) {
        //cd.println("Barrier threshold setting not implemented in this version");
    	this.barrier.setThreshold(k);
    }

    public void removeCar(int no) {
        cd.println("Remove Car not implemented in this version");
    }

    public void restoreCar(int no) {
        cd.println("Restore Car not implemented in this version");
    }

    /* Speed settings for testing purposes */

    public void setSpeed(int no, int speed) {
        car[no].setSpeed(speed);
    }

    public void setVariation(int no, int var) {
        car[no].setVariation(var);
    }

}






