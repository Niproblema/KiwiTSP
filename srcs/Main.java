import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
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

        static class DebugStats {
            static int mFlightCutsInEndpointCities = 0;
            static int mDuplicateFlightsCut = 0;
            static long maxMetScore = 0;
            static long averageMetScore = 0;
        }

        static int N;
        static Airport airpStart;

        static HashMap<String, Area> areas;
        static HashMap<String, Airport> airports;
        static FlightmapByCityByDay flights;

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
                System.out.println("\nOPTIMISATION:\nEndpoint cities flights cuts: " + DebugStats.mFlightCutsInEndpointCities + "\nDuplicate flights with different costs: " + DebugStats.mDuplicateFlightsCut + "\nMax met scoore: " + DebugStats.maxMetScore);
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
            private static LinkedHashSet<Metadata> mets;
            private static int bestCost = Integer.MAX_VALUE;
            private static int pathsFound = 0;

            public static void run() {
                fPath = new Flight[Data.N];         //Length N
                aCity = new Airport[Data.N + 1];    //Length of N+1, 0th is start city
                visited = new HashSet<>();
                bFixSol = new boolean[Data.N + 1];
                rng = new Random();
                day = 0;
                aCity[0] = Data.airpStart;
                bFixSol[0] = true;
                mets = new LinkedHashSet<>();


                /////////Optimizacije/////////
                //Optimizacije - rezanje nemogočih(npr nasledn dan letališče nima letov) + slabih flightov(isti dan isti flight, slabši cost). Lock in obveznih flightov/mest(določitev obveznih mest)

                //Remove flights that end in city that has no good flights the next day.
                int cutsInLastRound = -1;
                while (cutsInLastRound != 0) {
                    cutsInLastRound = 0;
                    for (int day = Data.N-1; day > 1; day--) {
                        for (int city = 0; city < Airport.getAirportCount(); city++) {
                            if (Algos.Utils.countNonNull(Data.flights.getFlightsByDepartureCityIDbyDay(day,city))==0) {  //TODO: many other cases also...
                                for(int cityWalk = 0; cityWalk<Airport.getAirportCount(); cityWalk++){
                                    if(Data.flights.getMap()[day-2][cityWalk][city]!= null){
                                        Data.DebugStats.mFlightCutsInEndpointCities++;
                                        cutsInLastRound++;
                                        Data.flights.getMap()[day-2][cityWalk][city]= null;
                                    }
                                }
                            }
                        }
                    }
                }
                //

                /////////////////////////////

                Data.Time.tOptimisation = System.currentTimeMillis();
                while (true) {  //Path search loop
                    day++;
                    if (day > Data.N) {   //Exit condition
                        int cost = pathEvaluator(fPath);
                        if (cost != -1) {
                            pathsFound++;
                            if (pathsFound != 1) {
                                cooldownMets();
                                int score = Math.round((((float) bestCost) / ((float) cost)) * 1000);
                                if (Data.isDebugMode) {
                                    System.out.println(score);
                                }
                                for (int i = 0; i < Data.N; i++) {
                                    Metadata m = Data.flights.mMap[i][fPath[i].airportDeparture.id][fPath[i].airportDestination.id];
                                    m.chance += score;
                                    if (m.chance > Data.DebugStats.maxMetScore) {
                                        Data.DebugStats.maxMetScore = m.chance;
                                    }
                                    mets.add(m);
                                }

                            }
                            if (cost < bestCost) {
                                bestCost = cost;
                            }
                        }
                        moveToStart();
                        continue;
                    }

                    Flight[] fAllPossibleFlights = Data.flights.getFlightsByDepartureCityIDbyDay(day, aCity[day - 1].id);
                    ArrayList<Flight> fRealPossibilities = new ArrayList<>();
                    long sumOdds = 0;
                    for (Flight fPossibility : fAllPossibleFlights) {
                        if (fPossibility == null) continue;
                        if ((!bFixSol[day] || (bFixSol[day] && fPossibility.airportDestination == aCity[day]))  //If next airport if predetermined -> filter flights
                                && !visited.contains(fPossibility.airportDestination.arAreaLocation.name)       //Area can't have been visited before
                                && (day == Data.N || (day != Data.N && fPossibility.airportDestination.arAreaLocation != Data.airpStart.arAreaLocation))) { //Dont pick airports in start area, if it's not final day
                            fRealPossibilities.add(fPossibility);
                            sumOdds += Data.flights.mMap[day - 1][fPossibility.airportDeparture.id][fPossibility.airportDestination.id].chance;
                        }
                    }
                    if (fRealPossibilities.isEmpty()) {
                        moveBack();
                        continue;
                    }
                    int scorePick = rng.nextInt((int) sumOdds) + 1;
                    int score = 0;
                    Flight pick = fRealPossibilities.get(fRealPossibilities.size() - 1);
                    for (Flight f : fRealPossibilities) {
                        score += Data.flights.mMap[day - 1][f.airportDeparture.id][f.airportDestination.id].chance;
                        if (score >= scorePick) {
                            pick = f;
                            break;
                        }
                    }
                    fPath[day - 1] = pick;
                    aCity[day] = pick.airportDestination;
                    visited.add(pick.airportDestination.arAreaLocation.name);
                }
            }

            private static void moveBack() {        //Moveback hevristics to search for other solutions - todo:how much?
                int moveBack = Math.min(day, (rng.nextInt(Math.max(1, day - 1)) + 2));
                for (int i = 1; i < moveBack; i++) {
                    if (!bFixSol[day - i]) {
                        visited.remove(aCity[day - i].arAreaLocation.name);
                        aCity[day - i] = null;
                    }
                }
                day -= moveBack;
            }

            private static void moveToStart() {
                for (int i = Data.N; i > 0; i--) {
                    if (!bFixSol[i] && aCity[i] !=null) {
                        visited.remove(aCity[i].arAreaLocation.name);
                        aCity[i] = null;
                    }
                }
                day = 0;
            }

            private static void cooldownMets() {
                Iterator<Metadata> metIte = mets.iterator();
                while (metIte.hasNext()) {
                    Metadata m = metIte.next();
                    m.chance = 600+Math.round(((float) m.chance) * 0.4f);
/*                    if (m.chance <= 1000) {
                        m.chance = 1000;
                        metIte.remove();
                    }*/
                }
            }

        }

        static class Utils {
            public static int countNonNull(Object[] in) {
                int k = 0;
                for (int i = 1; i < in.length; i++) {
                    if (in[i] != null)
                        k++;
                }
                return k;
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
        }, Math.max(50, (Data.N <= 20 ? 2900 : (Data.N <= 100 ? 4900 : 14500)) - (System.currentTimeMillis() - Data.Time.tStartTime)));
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
        Data.flights = new FlightmapByCityByDay(Data.N, Airport.getAirportCount());

        String inLine;
        while ((inLine = inReader.readLine()) != null) {
            String[] inFlightLine = inLine.split(" ");
            if (inFlightLine.length < 4) break;
            Flight inFlight = new Flight(inFlightLine[0], inFlightLine[1], Integer.parseInt(inFlightLine[2]), Integer.parseInt(inFlightLine[3]));
            Data.flights.addFlight(inFlight);
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
    private Flight[][][] dcMap; // days / city Depart / city Destination
    public Metadata[][][] mMap;

    private int nDays, nCitites;

    public FlightmapByCityByDay(int nDays, int nCities) {
        this.nDays = nDays;
        this.nCitites = nCities;
        dcMap = new Flight[nDays][nCities][nCities];
        mMap = new Metadata[nDays][nCities][nCities];
    }

    public Flight[] getFlightsByDepartureCityIDbyDay(int day, int cityId) {
        return dcMap[day - 1][cityId];
    }

    public Flight[] getFlightsByDestinationCityIDbyDay(int day, int cityId) {
        Flight[] fRtn = new Flight[Main.Data.N];
        for (int i = 0; i < Main.Data.N; i++) {
            fRtn[i] = dcMap[day - 1][i][cityId];
        }
        return fRtn;
    }


    public Flight[][][] getMap() {
        return dcMap;
    }


    public void addFlight(Flight inFlight) {
        if (inFlight.date == 0) {
            for (int day = 0; day < Main.Data.N; day++) {
                if (dcMap[day][inFlight.airportDeparture.id][inFlight.airportDestination.id] == null) {
                    dcMap[day][inFlight.airportDeparture.id][inFlight.airportDestination.id] = inFlight;
                    mMap[day][inFlight.airportDeparture.id][inFlight.airportDestination.id] = new Metadata();
                } else if (inFlight.cost < dcMap[day][inFlight.airportDeparture.id][inFlight.airportDestination.id].cost) {
                    dcMap[day][inFlight.airportDeparture.id][inFlight.airportDestination.id] = inFlight;
                    Main.Data.DebugStats.mDuplicateFlightsCut++;
                }
            }
        } else {
            int day = inFlight.date - 1;
            if (dcMap[day][inFlight.airportDeparture.id][inFlight.airportDestination.id] == null) {
                dcMap[day][inFlight.airportDeparture.id][inFlight.airportDestination.id] = inFlight;
                mMap[day][inFlight.airportDeparture.id][inFlight.airportDestination.id] = new Metadata();
            } else if (inFlight.cost < dcMap[day][inFlight.airportDeparture.id][inFlight.airportDestination.id].cost) {
                dcMap[day][inFlight.airportDeparture.id][inFlight.airportDestination.id] = inFlight;
                Main.Data.DebugStats.mDuplicateFlightsCut++;
            }
        }
    }
}

class Metadata {
    long chance = 1000;
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