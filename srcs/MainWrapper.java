import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.StringTokenizer;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

//public class MainWrapper {
//    public static void main(String[] args) throws IOException {
//        FileInputStream is = new FileInputStream(new File("./tests/1.in"));
//        System.setIn(is);
//        Main.main(args);
//    }
//}

class Main{

    static Graph graph;

    public static void main(String[] args) {
        graph = buildGraphFromInput();

        Solution sol = RandomSearch.bruteForceRandomSearch(500, graph);

        sol.printSolution(System.out);
    }

    static Graph buildGraphFromInput() {
        FastReader scanner = new FastReader();

        String[] temp = scanner.nextLine().split(" ");
        int N = Integer.parseInt(temp[0]);
        String startAirportName = temp[1];

        Airport airpStart = null;
        Area areaStart = null;
        HashMap<String, Area> areas = new HashMap<>(N);
        HashMap<String, Airport> airports = new HashMap<>();

        List<Airport> startAreaAirports = new ArrayList<>();

        for (int i = 0; i < N; i++) {
            String areaName = scanner.nextLine();
            String[] airportNames = scanner.nextLine().split(" ");
            Area area = new Area(areaName);
            for (String airportName : airportNames) {
                Airport airport = new Airport(airportName, area, N);
//                area.addAirport(airport);
                airports.put(airportName, airport);

                if (airportName.equals(startAirportName)) {
                    airpStart = airport;
                    areaStart = area;
                }

                if (area == areaStart) {
                    startAreaAirports.add(airport);
                }
            }
            areas.put(areaName, area);
        }

        String line;
        while ((line = scanner.nextLine()) != null) {
            String[] flightLine = line.split(" ");
//            if (flightLine.length < 4) break; /* for debug */
            Airport departure = airports.get(flightLine[0]);
            Airport arrival = airports.get(flightLine[1]);
            int day = Integer.parseInt(flightLine[2]);
            int cost = Integer.parseInt(flightLine[3]);
            Flight flight = new Flight(departure, arrival, day, cost);
            departure.addFlightOutOnDay(flight);

            // if leaving from one airport in start area, can leave from any
            if (departure.area == areaStart) {
                for (Airport startAreaAirpot: startAreaAirports) {
                    if (startAreaAirpot == departure) {
                        continue;
                    }
                    startAreaAirpot.addFlightOutOnDay(new Flight(startAreaAirpot, arrival, day, cost, flight));
                }
            }
        }

        return new Graph(N, airpStart, areaStart, areas, airports);
    }
}

class RandomSearch{

    static Solution bruteForceRandomSearch(int attempts, Graph graph) {
        Solution best = new Solution(graph);
        best.cost = Integer.MAX_VALUE;

        for (int i = 0; i < attempts; i++) {
            Solution attempt = randomSearch(graph);
            if (attempt != null) {
                if (attempt.cost < best.cost) {
                    best = attempt;
                }
            }
        }
        return best;
    }

    static Solution randomSearch(Graph graph) {
        Solution solution = new Solution(graph);

        HashSet<Area> visited = new HashSet<>();
        Airport currentAirport = graph.airpStart;
        visited.add(currentAirport.area);

        for (int i = 0; i < graph.N; i++) {
            int day = i+1;

            List<Flight> potentialFlights = RandomSearch.getPossibleFlightsFromAirportForRandomSearch(currentAirport, day, visited, graph);
            if (potentialFlights.size() == 0) return null;
            int randomNum = ThreadLocalRandom.current().nextInt(0, potentialFlights.size());

            Flight selected = potentialFlights.get(randomNum);
            solution.addFlight(selected);

            currentAirport = selected.airportDestination;
            visited.add(currentAirport.area);
        }

        return solution;
    }

    static List<Flight> getPossibleFlightsFromAirportForRandomSearch(Airport airport, int day,
                                                                            HashSet<Area> visited, Graph graph) {
        return airport.flightsOutOnDay.get(day).stream()
                .filter(flight -> (day == graph.N ?
                        flight.airportDestination.area == graph.areaStart :
                        !visited.contains(flight.airportDestination.area)))
                .collect(Collectors.toList());
    }
}

class Graph{
    int N;
    Airport airpStart;
    Area areaStart;
    HashMap<String, Area> areas;
    HashMap<String, Airport> airports;

    public Graph(int n, Airport airpStart, Area areaStart, HashMap<String, Area> areas,
                 HashMap<String, Airport> airports) {
        N = n;
        this.airpStart = airpStart;
        this.areaStart = areaStart;
        this.areas = areas;
        this.airports = airports;
    }
}

class Area{
    String name;
    HashMap<String, Airport> airports;

    public Area(String name){
        this.airports = new HashMap<>();
        this.name = name;
    }

    public void addAirport(Airport airport) {
        this.airports.put(airport.name, airport);
    }
}

class Airport{
    String name;
    Area area;
    HashMap<Integer, Flight> flightsIn,flightsOut;
    HashMap<Integer, List<Flight>> flightsOutOnDay;

    public Airport(String name, Area area, int numDays){
        this.name = name;
        this.area = area;
        this.flightsIn = new HashMap<>();
        this.flightsOut = new HashMap<>();
        this.flightsOutOnDay = new HashMap<>();

        for (int i = 0; i < numDays; i++) {
            int day = i+1;
            this.flightsOutOnDay.put(day, new ArrayList<>());
        }
    }

//    public void addFlightIn(Flight flight) {
//        this.flightsIn.put(flight.id, flight);
//    }
//
//    public void addFlightOut(Flight flight) {
//        this.flightsOut.put(flight.id, flight);
//    }

    public void addFlightOutOnDay(Flight flight) {
        if (flight.day == 0) {
            for (List<Flight> flights: this.flightsOutOnDay.values()) {
                flights.add(flight);
            }
        } else {
            this.flightsOutOnDay.get(flight.day).add(flight);
        }
    }
}

class Flight{
    Airport airportDeparture, airportDestination;
    int day, cost;

    boolean isShadow;
    Flight shadow;

    public Flight(Airport airportDeparture, Airport airportDestination, int day, int cost, Flight shadow) {
        this(airportDeparture, airportDestination, day, cost);
        this.shadow = shadow;
        this.isShadow = true;
    }

    public Flight(Airport departure, Airport destination, int day, int cost){
        this.airportDeparture = departure;
        this.airportDestination = destination;
        this.day = day;
        this.cost = cost;
        this.isShadow = false;
    }
}

class Solution{
    List<Flight> path;
    int cost;
    Graph graph;

    public Solution(Graph graph){
        this.path = new ArrayList<>();
        this.cost = 0;
        this.graph = graph;
    }

    public void addFlight(Flight flight) {
        this.path.add(flight);
        this.cost += flight.cost;
    }

    public boolean isValid() {
        // too few or too many areas visited
        if (path.size() != graph.N) return false;
        // does not begin in right place
        if (path.get(0).airportDeparture != graph.airpStart) return false;
        // does not end in right place
        if (path.get(path.size()-1).airportDestination.area != graph.areaStart) return false;

        HashSet<Area> visited = new HashSet<>();
        Airport currentLocation = graph.airpStart;
        for (int i = 0; i < path.size(); i++) {
            int day = i + 1;
            Flight currentFlight = path.get(i);

            // flight on wrong day
            if (currentFlight.day != day && currentFlight.day != 0) return false;
            // flight leaving from wrong location
            if (currentFlight.airportDeparture != currentLocation) return false;
            currentLocation = currentFlight.airportDestination;
            // area visited before
            if (visited.contains(currentLocation.area)) return false;
            visited.add(currentLocation.area);
        }

        return true;
    }

    public void unshadow() {
        for (int i = 0; i < path.size(); i++) {
            Flight flight = path.get(i);
            if (flight.isShadow) {
                path.set(i, flight.shadow);
            }
        }
    }

    void printSolution(PrintStream destination){
        this.unshadow();
        destination.println(cost);
        for(int i= 0; i < path.size(); i++){
            destination.println(path.get(i).airportDeparture.name+" "+path.get(i).airportDestination.name+" "+(i+1)+" "+path.get(i).cost);
        }
    }

    String solutionString(){
        String output = cost+"\n";
        for(int i= 0; i < path.size(); i++){
            output+=path.get(i).airportDeparture.name+" "+path.get(i).airportDestination.name+" "+(i+1)+" "+path.get(i).cost;
            if(i!=path.size()-1)output+="\n";
        }
        return output;
    }
}

class FastReader{
    BufferedReader br;
    StringTokenizer st;

    public FastReader()
    {
        br = new BufferedReader(new
                InputStreamReader(System.in));
    }

    String next()
    {
        while (st == null || !st.hasMoreElements())
        {
            try
            {
                st = new StringTokenizer(br.readLine());
            }
            catch (IOException  e)
            {
                e.printStackTrace();
            }
        }
        return st.nextToken();
    }

    int nextInt()
    {
        return Integer.parseInt(next());
    }

    long nextLong()
    {
        return Long.parseLong(next());
    }

    double nextDouble()
    {
        return Double.parseDouble(next());
    }

    String nextLine()
    {
        String str = "";
        try
        {
            str = br.readLine();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        return str;
    }
}