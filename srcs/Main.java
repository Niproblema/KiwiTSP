import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;

import sun.rmi.runtime.Log;

public class Main {

    public static void main(String[] args) {
        Data.Time.tStartTime = System.currentTimeMillis();

        parseInput("tests/4.in");
        Data.Time.tParseTime = System.currentTimeMillis();
        enableTimeoutTimer();

        //Algo
        //Algos.testFewExampleSolutions();  //Replace with algo
        //
        Data.Time.tFinishTime = System.currentTimeMillis();

        System.out.println("\nParse: " + (Data.Time.tParseTime - Data.Time.tStartTime) + "\nAlgo: " + (Data.Time.tFinishTime - Data.Time.tParseTime));
        Data.printBestAndFinish();
    }

    /* Data collection */
    static class Data {
        static int N;
        static Airport airpStart;

        static HashMap<String, Area> areas;
        static HashMap<String, Airport> airports;
        static FlightmapByCityByDay inFlights, outFlights;

        static class Time {
            static long tStartTime = 0;
            static long tParseTime = 0;
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
            System.exit(0);
        }
        ////////////////////
    }

    static class Algos {

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
        }, Math.max(50, (Data.N <= 20 ? 2950 : (Data.N <= 100 ? 4950 : 14950)) - (System.currentTimeMillis() - Data.Time.tStartTime)));
    }

    /**
     * Evaluates given path, submits best Solutions to data
     * All areas must be visited exactly once, finish area equals start area
     *
     * @param path Array of flight ids.
     * @return total cost of path or -1 if invalid.
     */
    static Flight[] fSolution = null;
    static int pathEvaluator(Flight[] path) {
        int cost = 0;
        if (path.length != Data.N) return -1; //Wrong size -> invalid
        if (fSolution == null) {
            fSolution = new Flight[Data.N];
        }
        HashSet<Area> hsVisited = new HashSet<>();
        for (int i = 0; i < Data.N; i++) {
            fSolution[i] = path[i];
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
    static void parseInput(String in) {
        try {
            InputReader inReader = null;
            inReader = in != null ? new InputReader(in) : new InputReader();

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
        } catch (IOException ioe) {
            System.out.println(ioe.toString());
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

    public static int getAirportCount() {
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
        for (int day = Math.max(flight.date-1, 0); day < (flight.date == 0 ? Main.Data.N : flight.date); day++) {
            dcMap[day][isDepartureMap ? flight.airportDeparture.id : flight.airportDestination.id].add(flight);
        }
    }
}

class InputReader {
    final private int BUFFER_SIZE = 1 << 16;
    private DataInputStream din;
    private byte[] buffer;
    private int bufferPointer, bytesRead;

    public InputReader() {
        din = new DataInputStream(System.in);
        buffer = new byte[BUFFER_SIZE];
        bufferPointer = bytesRead = 0;
    }

    public InputReader(String file_name) {
        try {
            din = new DataInputStream(new FileInputStream(file_name));
        } catch (IOException ioE) {
            System.out.println("No input file, using stdin instead");
            din = new DataInputStream(System.in);
        }
        buffer = new byte[BUFFER_SIZE];
        bufferPointer = bytesRead = 0;
    }

    private byte read() throws IOException {
        if (bufferPointer == bytesRead)
            fillBuffer();
        return buffer[bufferPointer++];
    }

    private void fillBuffer() throws IOException {
        bytesRead = din.read(buffer, bufferPointer = 0, BUFFER_SIZE);
        if (bytesRead == -1)
            buffer[0] = -1;
    }

    public void close() throws IOException {
        if (din == null)
            return;
        din.close();
    }

    public String readLine() throws IOException {
        byte[] buf = new byte[64];
        int cnt = 0, c;
        while ((c = read()) != -1) {
            if (c == '\n')
                break;
            if (c != '\r') {
                buf[cnt++] = (byte) c;
            }
        }
        return new String(buf, 0, cnt);
    }
}
