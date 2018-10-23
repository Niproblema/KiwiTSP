import java.io.PrintStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;

public class Main {

    public static void main(String[] args) {
        Data.startTime = System.currentTimeMillis();
        parseInput();
        enableTimeoutTimer();

        //Algo
        Algos.testFewExampleSolutions();  //Replace with algo
        //
    }

    /* Data collection */
    static class Data {
        static int N;
        static Airport airpStart;

        static HashMap<String, Area> areas;
        static HashMap<String, Airport> airports;
        static HashMap<Integer, Flight> flights;
        static HashSet<Flight>[] flightsByDay;

        static long startTime;
        ////Synchronized////
        private static Solution sBestSolution;

        public static synchronized Solution retrieveBestSolution() {
            return new Solution(sBestSolution.fPath, sBestSolution.mCost);
        }

        /**
         * saves solution if best
         */
        private static synchronized void submitSolution(Solution s) {
            if (sBestSolution == null || sBestSolution.mCost > s.mCost) {
                sBestSolution = s;
            }
        }

        /**
         * prints best solution
         */
        private static synchronized void printBestAndFinish() {
            if (sBestSolution != null) {
                sBestSolution.printSolution(System.out);
            }
            System.exit(0);
        }
        ////////////////////
    }

    static class Algos {

        /**
         * Few path examples for example test case 0.in TODO: remove later
         */
        static void testFewExampleSolutions() {
            int[][] examplePaths = {
                    {0, 3, 2},  //Correct 100
                    {1, 4, 2},  //Correct 130
                    {0, 4, 2},  //Incorrect
                    {1, 3, 2},  //Incorrect
                    {2, 3, 0},  //Incorrect
                    {0, 5, 6}    //Incorrect
            };

            for (int i = 0; i < examplePaths.length; i++) {
                pathEvaluator(examplePaths[i]);
            }
        }
    }

    /**
     * Prints best solution before time limit
     */
    static void enableTimeoutTimer() {
        Timer t = new Timer();
        t.schedule(new TimerTask() {
            @Override
            public void run() {
                Data.printBestAndFinish();
            }
        }, Math.max(50, (Data.N <= 20 ? 2950 : (Data.N <= 100 ? 4950 : 14950)) - (System.currentTimeMillis() - Data.startTime)));
    }

    /**
     * Evaluates given path, submits best Solutions to data
     * All areas must be visited exactly once, finish area equals start area
     *
     * @param path Array of flight ids.
     * @return total cost of path or -1 if invalid.
     */
    static Flight[] fSolution = null;
    static int pathEvaluator(int[] path) {
        int cost = 0;
        if (path.length != Data.N) return -1; //Wrong size -> invalid
        if (fSolution == null) {
            fSolution = new Flight[Data.N];
        }
        HashSet<Area> hsVisited = new HashSet<>();
        for (int i = 0; i < Data.N; i++) {
            fSolution[i] = Data.flights.get(path[i]);
        }
        if (fSolution[0].airportDeparture != Data.airpStart)
            return -1; //Start area != input start area -> invalid
        if (fSolution[0].airportDeparture.arAreaLocation != fSolution[Data.N - 1].airportDestination.arAreaLocation)
            return -1; //Start area != end area -> invalid
        Airport airCurrLoc = Data.airpStart;
        for (int i = 0; i < Data.N; i++) {
            if (fSolution[i].date != 0 && fSolution[i].date - 1 != i)
                return -1; //Flight date != date of travel -> invalid
            if (fSolution[i].airportDeparture != airCurrLoc)
                return -1; //Flight location area != current travel location -> invalid
            airCurrLoc = fSolution[i].airportDestination;
            if (hsVisited.contains(airCurrLoc.arAreaLocation)) return -1; //Area visited before.
            hsVisited.add(airCurrLoc.arAreaLocation);
            cost += fSolution[i].cost;
        }
        Data.submitSolution(new Solution(fSolution, cost));
        return cost;
    }

    /**
     * Parses input into Main.Data
     */
    static void parseInput() {
        Scanner scanner = new Scanner(System.in);

        String[] temp = scanner.nextLine().split(" ");
        Data.N = Integer.parseInt(temp[0]);
        String start = temp[1];

        Data.areas = new HashMap<>(Data.N);
        Data.airports = new HashMap<>();
        Data.flights = new HashMap<>();
        Data.flightsByDay = new HashSet[Data.N];

        for (int i = 0; i < Data.N; i++) {
            Data.flightsByDay[i] = new HashSet<>();
        }


        for (int i = 0; i < Data.N; i++) {
            String name = scanner.nextLine();
            String[] airports = scanner.nextLine().split(" ");
            Area inArea = new Area(name, airports);
            Data.areas.put(inArea.name, inArea);
            Data.airports.putAll(inArea.areaAirports);
        }
        Data.airpStart = Data.airports.get(start);

        while (scanner.hasNextLine()) {
            String[] inFlightLine = scanner.nextLine().split(" ");
            if (inFlightLine.length < 4) break;
            Flight inFlight = new Flight(inFlightLine[0], inFlightLine[1], Integer.parseInt(inFlightLine[2]), Integer.parseInt(inFlightLine[3]));
            Data.flights.put(inFlight.id, inFlight);
        }
    }

}


class Area {
    String name;
    HashMap<String, Airport> areaAirports;

    public Area(String name, String[] inAirports) {
        this.areaAirports = new HashMap<>(inAirports.length);
        this.name = name;
        for (String inAirport : inAirports) {
            areaAirports.put(inAirport, new Airport(inAirport, this));
        }
    }
}

class Airport {
    String name;
    HashMap<Integer, Flight> flightsIn, flightsOut;
    Area arAreaLocation;

    public Airport(String name, Area area) {
        this.name = name;
        this.flightsIn = new HashMap<>();
        this.flightsOut = new HashMap<>();
        this.arAreaLocation = area;
    }
}

class Flight {
    private static int idCounter = 0; //Unique id counter
    int id;                           //Unique id
    Airport airportDeparture, airportDestination;
    int date, cost;

    public Flight(String departure, String destination, int date, int cost) {
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
class Solution {
    Flight[] fPath;
    int mCost;

    public Solution(Flight[] path, int cost) {
        this.fPath = path.clone();
        mCost = cost;
    }

    void printSolution(PrintStream destination) {
        destination.println(mCost);
        for (int i = 0; i < fPath.length; i++) {
            destination.println(fPath[i].airportDeparture.name + " " + fPath[i].airportDestination.name + " " + (i + 1) + " " + fPath[i].cost);
        }
    }

    String printSolution() {
        String output = mCost + "\n";
        for (int i = 0; i < fPath.length; i++) {
            output += fPath[i].airportDeparture.name + " " + fPath[i].airportDestination.name + " " + (i + 1) + " " + fPath[i].cost;
            if (i != fPath.length - 1) output += "\n";
        }
        return output;
    }
}
