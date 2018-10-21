import java.io.PrintStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Scanner;

public class Main{

    static int N;
    static Airport airpStart;

    /* Collecton of all data, parsed in main. */
    static class Data {
        static HashMap<String, Area> areas;
        static HashMap<String, Airport> airports;
        static HashMap<Integer, Flight> flights;
        static HashSet<Flight>[] flightsByDay;
    }

    public static void main(String[] args) {
        parseInput();



        testExampleSolution();
        System.out.println("done");
    }

    /**
     * Few path examples for example test case. TODO: remove later
     */
    static void testExampleSolution(){
        int[][] examplePaths = {
                //{0, 3, 2},  //Correct 100
                //{1, 4, 2},  //Correct 130
                //{0, 4, 2},  //Incorrect
                //{1, 3, 2},  //Incorrect
                //{2, 3, 0},  //Incorrect
                {0,5,6}
        };

        for(int i = 0; i < examplePaths.length; i++){
            Solution pSol = pathEvaluator(examplePaths[i]);
            System.out.println("Path "+(i+1)+": \n"+ (pSol == null ? "invalid" : pSol.printSolution()));
        }
    }

    /**
     * Evaluates given path.
     * All areas must be visited exactly once, finish area equals start area
     * @param path Array of flight ids.
     * @return total cost of path or -1 if invalid.
     */
    static boolean isSet = false;
    static Flight[] fSolution;
    static HashMap<String, Boolean> bVisited; //bitmap for visited areas
    static String[] sAreaNames;
    static Solution pathEvaluator(int[] path){
        int cost = 0;
        if(path.length != N) return null; //Wrong size -> invalid
        if(!isSet){
            fSolution = new Flight[N];
            bVisited = new HashMap<>(N);
            sAreaNames = Data.areas.keySet().toArray(new String[N]);
            isSet = true;
        }
        for(int i = 0; i < N; i++){
            fSolution[i] = Data.flights.get(path[i]);
            bVisited.put(sAreaNames[i], false);
        }
        if(fSolution[0].airportDeparture != airpStart) return null; //Start area != input start area -> invalid
        if(fSolution[0].airportDeparture.arAreaLocation != fSolution[N-1].airportDestination.arAreaLocation) return null; //Start area != end area -> invalid
        Airport airCurrLoc = airpStart;
        for(int i = 0; i < N; i++){
            if(fSolution[i].date != 0 && fSolution[i].date-1 != i) return null; //Flight date != date of travel -> invalid
            if(fSolution[i].airportDeparture != airCurrLoc) return null; //Flight location area != current travel location -> invalid
            airCurrLoc = fSolution[i].airportDestination;
            if(bVisited.get(airCurrLoc.arAreaLocation.name)) return null; //Area visited before.
            bVisited.put(airCurrLoc.arAreaLocation.name, true);
            cost+=fSolution[i].cost;
        }
        return new Solution(fSolution, cost);
    }

    /** Parses input into Main.Data */
    static void parseInput(){
        Scanner scanner = new Scanner(System.in);

        String[] temp = scanner.nextLine().split(" ");
        N = Integer.parseInt(temp[0]);
        String start = temp[1];

        Data.areas = new HashMap<>(N);
        Data.airports = new HashMap<>();
        Data.flights = new HashMap<>();
        Data.flightsByDay = new HashSet[N];

        for (int i = 0; i < N; i ++) {
            Data.flightsByDay[i] = new HashSet<>();
        }


        for(int i = 0; i < N; i++){
            String name = scanner.nextLine();
            String[] airports = scanner.nextLine().split(" ");
            Area inArea = new Area(name, airports);
            Data.areas.put(inArea.name, inArea);
            Data.airports.putAll(inArea.areaAirports);
        }
        airpStart = Data.airports.get(start);

        while(scanner.hasNextLine()){
            String[] inFlightLine = scanner.nextLine().split(" ");
            if(inFlightLine.length < 4)break;
            Flight inFlight = new Flight(inFlightLine[0], inFlightLine[1], Integer.parseInt(inFlightLine[2]), Integer.parseInt(inFlightLine[3]));
            Data.flights.put(inFlight.id, inFlight);
        }
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
            for (int i = 0; i < Main.N; i++) {
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
        this.fPath = path;
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
