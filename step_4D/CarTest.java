//Prototype implementation of Car Test class
//Mandatory assignment
//Course 02158 Concurrent Programming, DTU   Fall 2012


public class CarTest extends Thread {

    CarTestingI cars;
    int testno;

    public CarTest(CarTestingI ct, int no) {
        cars = ct;
        testno = no;
    }

    public void run() {
        try {  
            switch (testno) { 
            case 0:
                // Demonstration of startAll/stopAll.
                // Should let the cars go one round (unless very fast)
                cars.startAll();
                sleep(3000);
                cars.stopAll();
                break;
            case 1:
            	//Demonstration of barrier On-Off functionality 
            	cars.barrierOn();
            	sleep(10000);
            	cars.barrierOff();
            	break;
            case 2:
            	/*
            	*Demonstration of barrier functionality when decreasing threshold from initial->6->4
            	*It should work with only 4 cars in total waiting on barrier 
            	*Number 6 or initial number (8 or 9) should not be taken into account
            	*If it had already started counting for 6,8 or 9, the corresponding cars should be 
            	*released */
            	cars.barrierOn();
            	cars.barrierSet(6);
            	cars.barrierSet(4);
            	break;
            case 3: 
            	/*
            	*Demonstration of barrier functionality when threshold from initial->4->6->3
            	*It should work with only 6 cars in total waiting on barrier 
            	*Number 4 ,3 or initial number (8 or 9) should not be taken into account
            	*If it had already started counting for 8 or 9, the corresponding cars should be 
            	*released and start counting for 4. Of course, this will be immediately replaced by 6.
            	*When cars threshold is increased (to 6) then the BarrierSet() function is blocked
            	*and no other change (here to 3) takes place. Therefore, this test should in total 
            	*count 6 cars as barrier threshold. */
            	cars.barrierOn();
            	cars.barrierSet(4);
            	cars.barrierSet(6);
            	cars.barrierSet(3);
            	break;
            case 19:
                // Demonstration of speed setting.
                // Change speed to double of default values
                cars.println("Doubling speeds");
                for (int i = 1; i < 9; i++) {
                    cars.setSpeed(i,50);
                };
                break;

            default:
                cars.println("Test " + testno + " not available");
            }

            cars.println("Test ended");

        } catch (Exception e) {
            System.err.println("Exception in test: "+e);
        }
    }

}



