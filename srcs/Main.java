import java.io.PrintStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Scanner;

public class Main{

    /* Data collection */
    static class Data {
        static int N;
        static Airport airpStart;

        static HashMap<String, Area> areas;
        static HashMap<String, Airport> airports;
        static HashMap<Integer, Flight> flights;
        static HashSet<Flight>[] flightsByDay;


        ////Synchronized////
        static Solution sBestSolution;
        ////////////////////
    }

    public static void main(String[] args) {
        //TODO: save start time
        parseInput();
        //TODO: Start time limiter


        testFewExampleSolutions();

        printBestAndFinish();
    }




    /**
     * Few path examples for example test case. TODO: remove later
     */
    static void testFewExampleSolutions(){
        int[][] examplePaths = {
                {0, 3, 2},  //Correct 100
                {1, 4, 2},  //Correct 130
                {0, 4, 2},  //Incorrect
                {1, 3, 2},  //Incorrect
                {2, 3, 0},  //Incorrect
                {0,5,6}     //Incorrect
        };

        for(int i = 0; i < examplePaths.length; i++){
            pathEvaluator(examplePaths[i]);
        }
    }

    /**
     * Evaluates given path, submits best Solutions to data
     * All areas must be visited exactly once, finish area equals start area
     * @param path Array of flight ids.
     * @return total cost of path or -1 if invalid.
     */
    static boolean isSet = false;
    static Flight[] fSolution;
    static HashMap<String, Boolean> bVisited; //bitmap for visited areas
    static String[] sAreaNames;
    static int pathEvaluator(int[] path){
        int cost = 0;
        if(path.length != Data.N) return -1; //Wrong size -> invalid
        if(!isSet){
            fSolution = new Flight[Data.N];
            bVisited = new HashMap<>(Data.N);
            sAreaNames = Data.areas.keySet().toArray(new String[Data.N]);
            isSet = true;
        }
        for(int i = 0; i < Data.N; i++){
            fSolution[i] = Data.flights.get(path[i]);
            bVisited.put(sAreaNames[i], false);
        }
        if(fSolution[0].airportDeparture != Data.airpStart) return -1; //Start area != input start area -> invalid
        if(fSolution[0].airportDeparture.arAreaLocation != fSolution[Data.N-1].airportDestination.arAreaLocation) return -1; //Start area != end area -> invalid
        Airport airCurrLoc = Data.airpStart;
        for(int i = 0; i < Data.N; i++){
            if(fSolution[i].date != 0 && fSolution[i].date-1 != i) return -1; //Flight date != date of travel -> invalid
            if(fSolution[i].airportDeparture != airCurrLoc) return -1; //Flight location area != current travel location -> invalid
            airCurrLoc = fSolution[i].airportDestination;
            if(bVisited.get(airCurrLoc.arAreaLocation.name)) return -1; //Area visited before.
            bVisited.put(airCurrLoc.arAreaLocation.name, true);
            cost+=fSolution[i].cost;
        }
        submitSolution(new Solution(fSolution, cost));
        return cost;
    }

    /** Parses input into Main.Data */
    static void parseInput(){
        Scanner scanner = new Scanner(System.in);

        String[] temp = scanner.nextLine().split(" ");
        Data.N = Integer.parseInt(temp[0]);
        String start = temp[1];

        Data.areas = new HashMap<>(Data.N);
        Data.airports = new HashMap<>();
        Data.flights = new HashMap<>();
        Data.flightsByDay = new HashSet[Data.N];

        for (int i = 0; i < Data.N; i ++) {
            Data.flightsByDay[i] = new HashSet<>();
        }


        for(int i = 0; i < Data.N; i++){
            String name = scanner.nextLine();
            String[] airports = scanner.nextLine().split(" ");
            Area inArea = new Area(name, airports);
            Data.areas.put(inArea.name, inArea);
            Data.airports.putAll(inArea.areaAirports);
        }
        Data.airpStart = Data.airports.get(start);

        while(scanner.hasNextLine()){
            String[] inFlightLine = scanner.nextLine().split(" ");
            if(inFlightLine.length < 4)break;
            Flight inFlight = new Flight(inFlightLine[0], inFlightLine[1], Integer.parseInt(inFlightLine[2]), Integer.parseInt(inFlightLine[3]));
            Data.flights.put(inFlight.id, inFlight);
        }
    }

    static synchronized Solution retriveSoltuin(){
        return new Solution(Data.sBestSolution.fPath, Data.sBestSolution.mCost);
    }

    /** saves solution if best */
    static synchronized void submitSolution(Solution s){
        if(Data.sBestSolution == null || Data.sBestSolution.mCost > s.mCost){
            Data.sBestSolution = s;
        }
    }

    /** prints best solution */
    static synchronized void printBestAndFinish(){
        if(Data.sBestSolution != null){
            Data.sBestSolution.printSolution(System.out);
        }
        System.exit(0);
    }
}




class Area{
    String name;
    HashMap<String, Airport> areaAirports;

    public Area(String name, String[] inAirports){
        this.areaAirports = new HashMap<>(inAirports.length);
        this.name = name;
        for(String inAirport:inAirports){
            areaAirports.put(inAirport, new Airport(inAirport, this));
        }
    }
}

class Airport{
    String name;
    HashMap<Integer, Flight> flightsIn,flightsOut;
    Area arAreaLocation;
    public Airport(String name, Area area){
        this.name = name;
        this.flightsIn = new HashMap<>();
        this.flightsOut = new HashMap<>();
        this.arAreaLocation = area;
    }
}

class Flight{
    private static int idCounter = 0; //Unique id counter
    int id;                           //Unique id
    Airport airportDeparture, airportDestination;
    int date, cost;

    public Flight(String departure, String destination, int date, int cost){
        this.id = idCounter++;
        airportDeparture = Main.Data.airports.get(departure);
        airportDestination = Main.Data.airports.get(destination);
        this.date = date;
        this.cost = cost;
        airportDeparture.flightsOut.put(id, this);
        airportDestination.flightsIn.put(id, this);

        if (date == 0) {
            for (int i = 0; i < Main.Data.N; i++) {
                Main.Data.flightsByDay[i].add(this);
            }
        } else {
            Main.Data.flightsByDay[date - 1].add(this);
        }
    }
}

/**
 * Class for storing valid solutions. todo: solution methods
 */
class Solution{
    Flight[] fPath;
    int mCost;
    public Solution(Flight[] path, int cost){
        this.fPath = path.clone();
        mCost = cost;
    }

    void printSolution(PrintStream destination){
        destination.println(mCost);
        for(int i= 0; i < fPath.length; i++){
            destination.println(fPath[i].airportDeparture.name+" "+fPath[i].airportDestination.name+" "+(i+1)+" "+fPath[i].cost);
        }
    }

    String printSolution(){
        String output = mCost+"\n";
        for(int i= 0; i < fPath.length; i++){
            output+=fPath[i].airportDeparture.name+" "+fPath[i].airportDestination.name+" "+(i+1)+" "+fPath[i].cost;
            if(i!=fPath.length-1)output+="\n";
        }
        return output;
    }
}
