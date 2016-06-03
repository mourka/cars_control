//Prototype implementation of Car Control
//Mandatory assignment
//Course 02158 Concurrent Programming, DTU   Fall 2012


import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.*;
import java.util.logging.Level;
import java.util.logging.Logger;


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

    public Car(int no, CarDisplayI cd, Gate g, Alley alley, Map<Pos, Semaphore> semaphores) {

        this.no = no;
        this.cd = cd;
        mygate = g;
        startpos = cd.getStartPos(no);
        barpos = cd.getBarrierPos(no);  // For later use
        col = chooseColor();
        this.alley = alley;
        this.semaphores = semaphores;

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



                if(no==5||no==6||no==7||no==8){  
	                if((inAlley1(newpos)&&inAlley2(newpos)&&(!inAlley1(curpos)&&(!inAlley2(curpos))))) {
	                   alley.enter(no);
	                }
                }
                if(no==5||no==6||no==7||no==8){ 
	                 if(inAlley2(curpos) && !inAlley2(newpos)) {
	                     alley.leave(no,2);
	                 }
               
	                if(inAlley1(curpos) && !inAlley1(newpos)) {
	                    alley.leave(no,1);
	                } 
                }
                if(no==1||no==2){  
	                if(!inAlley2(curpos) && inAlley2(newpos)) {
	                    alley.enter(no);
	                 }
                }
                if(no==3||no==4){
                	 if(!inAlley1(curpos) && inAlley1(newpos)){
                		 alley.enter(no);
                	 }
                }
                if(no==1||no==2||no==3||no==4){
                	if((inAlley1(curpos)&&inAlley2(curpos)&&(!inAlley1(newpos)&&(!inAlley2(newpos)))))
                		alley.leave(no,0);
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
/*New*/
    public boolean inAlley1(Pos position) {

        List<Pos> alleyPositions = new ArrayList<Pos>();
        for(int i=0;i<11;i++) {  
            alleyPositions.add(new Pos(i,0));
        }
        for(int j=1;j<3;j++) {
           alleyPositions.add(new Pos(9,j));
//           alleyPositions.add(new Pos(8,j));
        }

        return alleyPositions.contains(position);
    }
    
    public boolean inAlley2(Pos position) {

        List<Pos> alleyPositions = new ArrayList<Pos>();
        for(int i=0;i<9;i++) {   //only column 0, rows 0-8
            alleyPositions.add(new Pos(i,0));
        }
        /*for(int j=0;j<3;j++) {  
           alleyPositions.add(new Pos(9,j));
//           alleyPositions.add(new Pos(8,j));
        }*/

        return alleyPositions.contains(position);
    }

}
class Alley {

    private volatile int nrUp1 ;
    private volatile int nrDown1 ;
    private volatile int nrUp2 ;
    private volatile int nrDown2 ;
    private int AlleyNo;

    public Alley() {

        nrUp1 = 0;
        nrDown1 = 0;
        nrUp2 = 0;
        nrDown2 = 0;

    }

    public void enter(int no) {

        if (no <5){
            
            synchronized (this){
                try {
                	if(no==1||no==2){
                    while (nrDown2 > 0) //while there cars coming down, wait
                        wait();
                    //critical ...
                    ++nrUp2;   //otherwise count cars going up!
                	}
                	if(no==3||no==4){
                		while (nrDown1 > 0) //while there cars coming down, wait
                            wait();
                        //critical ...
                     ++nrUp1;
                	}
                } catch (InterruptedException ex) {
                    System.out.println("interrupted occured");
                }             
            }

        } else {  //if cars no.5-8 enter the 2 Alleys

            synchronized (this){
                try {
                    while (nrUp1 > 0 || nrUp2>0)
                        wait();
    //                ok_for_down.await();
                    //critical ...
                    ++nrDown1;
                    ++nrDown2;
                } catch (InterruptedException ex) {
                    System.out.println("interrupted occured");
                }
            }
        }
    }

    public  void leave(int no, int AlleyNo) {

    	this.AlleyNo = AlleyNo;
    	
        if (no < 5){
            synchronized (this){        
                if(no==3||no==4)
            	  --nrUp1;  //cars going up leave the alley (No of cars going up decreases)
                if(no==1||no==2)
                  --nrUp2;
                if (nrUp1 == 0 && nrUp2==0){  //if it is the last car going up, leaving the alley, then notify!
                    notify();
                }
            }            
        } else {    //for cars no. 5-8
              synchronized (this){ 
                    
                    if(AlleyNo==2){
                    	--nrDown2;
                    }
                    if(AlleyNo==1){
                    	--nrDown1;
                    }
                    if (nrDown2 == 0){  //when number of cars going down=0, then notify others
                        //notify cars no.1 & 2 so they enter the alley before no.3 & 4
                        	notify();
                    }
                    if(nrDown1==0)  //notify cars no.3 & 4
                    	notify();
              } 
        }
              
    }
}

//--------------------------
public class CarControl implements CarControlI {

    CarDisplayI cd;           // Reference to GUI
    Car[] car;               // Cars
    Gate[] gate;              // Gates
    private final Alley alley;
    private Map<Pos, Semaphore> semaphoreMap;

    public CarControl(CarDisplayI cd) {
        this.cd = cd;
        car = new Car[9];
        gate = new Gate[9];
        alley = new Alley();
        semaphoreMap = new HashMap<Pos, Semaphore>();
        // critical areas, give every cells a semaphore
        for (int i = 0; i < 11; i++) {
            for (int j = 0; j < 12; j++) {
                semaphoreMap.put(new Pos(i,j), new Semaphore(1));
            }
        }


        for (int no = 0; no < 9; no++) {
            gate[no] = new Gate();
            car[no] = new Car(no, cd, gate[no], alley, semaphoreMap);
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
        cd.println("Barrier On not implemented in this version");
    }

    public void barrierOff() {
        cd.println("Barrier Off not implemented in this version");
    }

    public void barrierSet(int k) {
        cd.println("Barrier threshold setting not implemented in this version");
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






