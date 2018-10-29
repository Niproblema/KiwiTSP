import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;
import java.util.StringTokenizer;
import java.util.Timer;
import java.util.TimerTask;

public class Main {

    public static void main(String[] args) {
        Data.Time.tStartTime = System.currentTimeMillis();
        Data.isDebugMode = true;       //DEBUG SWITCH
        parseInput("tests/4.in");       //INPUT doesn't matter if debug is set to false

        Data.Time.tParseTime = System.currentTimeMillis();
        enableTimeoutTimer();

        //Algo
        Algos.SickSearch.run();  //Replace with algo
        //
    }

    /* Data collection */
    static class Data {
        static boolean isDebugMode = false;
        static class DebugStats{
            static int mFlightCutsInEndpointCities = 0;
            static int mDuplicateFlightsCut = 0;
        }

        static int N;
        static Airport airpStart;

        static HashMap<String, Area> areas;
        static HashMap<String, Airport> airports;
        //static HashMap<String, Flight> flights;

        static FlightmapByCityByDay inFlights, outFlights;

        static class Time {
            static long tStartTime = 0;
            static long tParseTime = 0;
            static long tOptimisation = 0;
            static long tFinishTime = 0;
        }

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
            if (Data.isDebugMode) {
                Data.Time.tFinishTime = System.currentTimeMillis();
                System.out.println("\nTIME SHARES:\nParse: " + (Data.Time.tParseTime - Data.Time.tStartTime) + "\nOptimisation: " + (Data.Time.tOptimisation - Data.Time.tParseTime) + "\nAlgo: " + (Data.Time.tFinishTime - Data.Time.tOptimisation));
                System.out.println("\nOPTIMISATION:\nEndpoint cities flights cuts: "+DebugStats.mFlightCutsInEndpointCities);
            }
            System.exit(0);
        }
        ////////////////////
    }

    static class Algos {
        static class SickSearch {

            private static Flight[] fPath = null;
            private static Airport[] aCity = null;
            private static HashSet<String> visited = null;
            private static boolean bFixSol[] = null;
            private static Random rng = null;
            private static int day;

            public static void run() {
                fPath = new Flight[Data.N];         //Length N
                aCity = new Airport[Data.N + 1];    //Length of N+1, 0th is start city
                visited = new HashSet<>();
                bFixSol = new boolean[Data.N + 1];
                rng = new Random();
                day = 0;
                aCity[0] = Data.airpStart;
                bFixSol[0] = true;


                /////////Optimizacije/////////
                //Optimizacije - rezanje nemogočih(npr nasledn dan letališče nima letov) + slabih flightov(isti dan isti flight, slabši cost). Lock in obveznih flightov/mest(določitev obveznih mest)
                ArrayList<Flight>[][] fOutData = Data.outFlights.getMap();  // days / cities
                ArrayList<Flight>[][] fInData = Data.inFlights.getMap();    // days / cities
                int cutsInLastRound = -1;
                while (cutsInLastRound != 0) {
                    cutsInLastRound = 0;
                    for (int day = 0; day < Data.N; day++) {
                        for (int city = 0; city < Airport.getAirportCount(); city++) {
                            if (day != 0 && fOutData[day][city].isEmpty()) {  //TODO: many other cases also...
                                ArrayList<Flight> fInToCity = fInData[day - 1][city];
                                for (Flight fToRemove : fInToCity) {
                                    if (fOutData[day - 1][fToRemove.airportDeparture.id].remove(fToRemove) && Data.isDebugMode) {
                                        Data.DebugStats.mFlightCutsInEndpointCities++;
                                        cutsInLastRound++;
                                        //System.out.println("Removed flight:" + fToRemove.airportDeparture.name + " " + fToRemove.airportDestination.name + " " + fToRemove.date + " " + fToRemove.cost);
                                    }
                                }
                                fInToCity.clear();
                            }
                        }
                    }
                }
                /////////////////////////////

                Data.Time.tOptimisation = System.currentTimeMillis();
                while (true) {  //Path search loop
                    day++;
                    if (day > Data.N) {   //Exit condition
                        pathEvaluator(fPath); //TODO: do something with cost?
                        moveBack();
                        continue;
                    }

                    ArrayList<Flight> fAllPossibleFlights = Data.outFlights.getFlights(aCity[day - 1].id, day);
                    if (fAllPossibleFlights.isEmpty()) {  //No possible flights - return back a bit
                        moveBack();
                        continue;
                    }
                    ArrayList<Flight> fRealPossibilities = new ArrayList<>();
                    for (Flight fPossibility : fAllPossibleFlights) {
                        if ((!bFixSol[day] || (bFixSol[day] && fPossibility.airportDestination == aCity[day]))  //If next airport if predetermined -> filter flights
                                && !visited.contains(fPossibility.airportDestination.arAreaLocation.name)       //Area can't have been visited before
                                && (day == Data.N || (day != Data.N && fPossibility.airportDestination.arAreaLocation != Data.airpStart.arAreaLocation))) { //Dont pick airports in start area, if it's not final day
                            fRealPossibilities.add(fPossibility);
                            //TODO: calculate odds
                        }
                    }
                    if (fRealPossibilities.isEmpty()) {
                        moveBack();
                        continue;
                    }
                    int mFlightIndexPick = rng.nextInt(fRealPossibilities.size());   //TODO: better picking heuristics
                    fPath[day - 1] = fRealPossibilities.get(mFlightIndexPick);
                    aCity[day] = fPath[day - 1].airportDestination;
                    visited.add(fPath[day - 1].airportDestination.arAreaLocation.name);
                }
            }

            private static void moveBack() {        //Moveback hevristics to search for other solutions - todo:how much?
                int moveBack = Math.min(day, (rng.nextInt(Math.max(0, day - 1)) + 2));
                for (int i = 1; i < moveBack; i++) {
                    if (!bFixSol[day - i]) {
                        visited.remove(aCity[day - i].arAreaLocation.name);
                        aCity[day - i] = null;
                    }
                }
                day -= moveBack;
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
        }, Math.max(50, (Data.N <= 20 ? 2900 : (Data.N <= 100 ? 4900 : 14900)) - (System.currentTimeMillis() - Data.Time.tStartTime)));
    }

    /**
     * Evaluates given path, submits best Solutions to data
     * All areas must be visited exactly once, finish area equals start area
     *
     * @param fSolution Array of flight ids.
     * @return total cost of path or -1 if invalid.
     */
    static int pathEvaluator(Flight[] fSolution) {
        int cost = 0;
        if (fSolution.length != Data.N) return -1; //Wrong size -> invalid
        HashSet<Area> hsVisited = new HashSet<>();
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
    static void parseInput(String in) {
        InputReader inReader = (in == null || !Data.isDebugMode) ? new InputReader() : new InputReader(in);

        String[] temp = inReader.readLine().split(" ");
        Data.N = Integer.parseInt(temp[0]);
        String start = temp[1];

        Data.areas = new HashMap<>(Data.N);
        Data.airports = new HashMap<>(300);


        for (int i = 0; i < Data.N; i++) {
            String name = inReader.readLine();
            String[] airports = inReader.readLine().split(" ");
            Area inArea = new Area(name, airports);
            Data.areas.put(inArea.name, inArea);
            Data.airports.putAll(inArea.areaAirports);
        }
        Data.airpStart = Data.airports.get(start);
        Data.outFlights = new FlightmapByCityByDay(Data.N, Airport.getAirportCount(), true);
        Data.inFlights = new FlightmapByCityByDay(Data.N, Airport.getAirportCount(), false);

        String inLine;
        while ((inLine = inReader.readLine()) != null) {
            String[] inFlightLine = inLine.split(" ");
            if (inFlightLine.length < 4) break;
            Flight inFlight = new Flight(inFlightLine[0], inFlightLine[1], Integer.parseInt(inFlightLine[2]), Integer.parseInt(inFlightLine[3]));
            Data.outFlights.addFlight(inFlight);
            Data.inFlights.addFlight(inFlight);
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
    private static int idCounter = 0; //Unique id counter
    int id;                           //Unique id
    Area arAreaLocation;

    public Airport(String name, Area area) {
        this.id = idCounter++;
        this.name = name;
        this.arAreaLocation = area;
    }

    public static int getAirportCount() {
        return idCounter;
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
    }

    public static int getFlightCount() {
        return idCounter;
    }
}


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

class FlightmapByCityByDay {
    private ArrayList<Flight>[][] dcMap; // days / cities
    private boolean isDepartureMap;
    private int nDays, nCitites;

    public FlightmapByCityByDay(int nDays, int nCities, boolean isDepartureMap) {
        this.nDays = nDays;
        this.nCitites = nCities;
        this.isDepartureMap = isDepartureMap;
        dcMap = new ArrayList[nDays][nCities];
        for (int day = 0; day < nDays; day++) {
            for (int city = 0; city < nCities; city++) {
                dcMap[day][city] = new ArrayList<>();
            }
        }
    }

    public ArrayList<Flight> getFlights(int cityId, int day) {
        return dcMap[day - 1][cityId];
    }

    public void addFlight(Flight flight) {
        int start = Math.max(flight.date - 1, 0);
        int lim = flight.date == 0 ? Main.Data.N : flight.date;
        if (isDepartureMap) {
            for (int day = start; day < lim; day++) {
                dcMap[day][flight.airportDeparture.id].add(flight);
            }
        } else {
            for (int day = start; day < lim; day++) {
                dcMap[day][flight.airportDestination.id].add(flight);
            }
        }

    }

    public ArrayList<Flight>[][] getMap() {
        return dcMap;
    }
}

class InputReader {
    BufferedReader br;
    StringTokenizer st;

    public InputReader() {
        br = new BufferedReader(new
                InputStreamReader(System.in));
    }

    public InputReader(String name) {
        try {
            br = new BufferedReader(new InputStreamReader(new DataInputStream(new FileInputStream(name))));
        } catch (Exception e) {
            br = new BufferedReader(new InputStreamReader(System.in));
        }
    }

    String readLine() {
        String str = "";
        try {
            str = br.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return str;
    }
}