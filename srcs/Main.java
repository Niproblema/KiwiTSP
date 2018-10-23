import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public class Main{

    static Graph graph;

    public static void main(String[] args) {
        graph = buildGraphFromInput();

        Solution sol = bruteForceRandomSearch(50);

        System.out.print("Done");
    }

    static Solution bruteForceRandomSearch(int attempts) {
        int bestCost = Integer.MAX_VALUE;
        Solution best = new Solution(graph);

        for (int i = 0; i < attempts; i++) {
            Solution attempt = randomSearch(graph);
            if (attempt != null) {
                System.out.println("Found valid solution, cost: " + attempt.cost);
                if (attempt.cost < bestCost) {
                    bestCost = attempt.cost;
                    best = attempt;
                }
            } else {
                System.out.println("INVALID SOLUTION FOUND");
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

            List<Flight> potentialFlights = graph.getPossibleFlightsFromAirportForRandomSearch(currentAirport, day, visited);
            if (potentialFlights.size() == 0) return null;
            int randomNum = ThreadLocalRandom.current().nextInt(0, potentialFlights.size());

            Flight selected = potentialFlights.get(randomNum);
            solution.addFlight(selected);

            currentAirport = selected.airportDestination;
            visited.add(currentAirport.area);
        }

        return solution;
    }

    static Graph buildGraphFromInput() {
        Scanner scanner = new Scanner(System.in);

        String[] temp = scanner.nextLine().split(" ");
        int N = Integer.parseInt(temp[0]);
        String startAirportName = temp[1];

        Airport airpStart = null;
        Area areaStart = null;
        HashMap<String, Area> areas = new HashMap<>(N);
        HashMap<String, Airport> airports = new HashMap<>();
        HashMap<Integer, Flight> flights = new HashMap<>();

        for (int i = 0; i < N; i++) {
            String areaName = scanner.nextLine();
            String[] airportNames = scanner.nextLine().split(" ");
            Area area = new Area(areaName);
            for (String airportName : airportNames) {
                Airport airport = new Airport(airportName, area);
                area.addAirport(airport);
                airports.put(airportName, airport);

                if (airportName.equals(startAirportName)) {
                    airpStart = airport;
                    areaStart = area;
                }
            }
        }

        while (scanner.hasNextLine()) {
            String[] inFlightLine = scanner.nextLine().split(" ");
            if (inFlightLine.length < 4) break; /* for debug */
            Airport departure = airports.get(inFlightLine[0]);
            Airport arrival = airports.get(inFlightLine[1]);
            Flight inFlight = new Flight(departure, arrival, Integer.parseInt(inFlightLine[2]), Integer.parseInt(inFlightLine[3]));
            flights.put(inFlight.id, inFlight);
            departure.addFlightOut(inFlight);
            arrival.addFlightIn(inFlight);
        }

        return new Graph(N, airpStart, areaStart, areas, airports, flights);
    }
}


class Graph {
    int N;
    Airport airpStart;
    Area areaStart;
    HashMap<String, Area> areas;
    HashMap<String, Airport> airports;
    HashMap<Integer, Flight> flights;

    public Graph(int n, Airport airpStart, Area areaStart, HashMap<String, Area> areas, HashMap<String, Airport> airports, HashMap<Integer, Flight> flights) {
        N = n;
        this.airpStart = airpStart;
        this.areaStart = areaStart;
        this.areas = areas;
        this.airports = airports;
        this.flights = flights;
    }

    public List<Flight> getPossibleFlightsFromAirportForRandomSearch(Airport airport, int day, HashSet<Area> visited) {
        return airport.flightsOut.values().stream()
                .filter(flight -> (flight.day == day || flight.day == 0) &&
                        // must be on right day (above) and in valid area (below)
                        (day == N ?
                            flight.airportDestination.area == areaStart :
                            !visited.contains(flight.airportDestination.area)))
                .collect(Collectors.toList());
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

    public Airport(String name, Area area){
        this.name = name;
        this.area = area;
        this.flightsIn = new HashMap<>();
        this.flightsOut = new HashMap<>();
    }

    public void addFlightIn(Flight flight) {
        this.flightsIn.put(flight.id, flight);
    }

    public void addFlightOut(Flight flight) {
        this.flightsOut.put(flight.id, flight);
    }
}

class Flight{
    private static int idCounter = 0; //Unique id counter
    int id;                           //Unique id
    Airport airportDeparture, airportDestination;
    int day, cost;

    public Flight(Airport departure, Airport destination, int day, int cost){
        this.id = idCounter++;
        this.airportDeparture = departure;
        this.airportDestination = destination;
        this.day = day;
        this.cost = cost;
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

    void printSolution(PrintStream destination){
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
