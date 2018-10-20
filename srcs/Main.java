import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Scanner;

public class Main{

    static int N;
    static Area arStart;

    /* Collecton of all data, parsed in main. */
    static class Data {
        static HashMap<String, Area> areas;
        static HashMap<String, Airport> airports;
        static HashMap<Integer, Flight> flights;
        static HashSet<Flight>[] flightsByDay;
    }

    public static void main(String[] args) {
        parseInput();

        System.out.println("done");
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
        arStart = Data.areas.get(start);

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
            areaAirports.put(inAirport, new Airport(inAirport));
        }
    }
}

class Airport{
    String name;
    HashMap<Integer, Flight> flightsIn,flightsOut;
    public Airport(String name){
        this.name = name;
        this.flightsIn = new HashMap<>();
        this.flightsOut = new HashMap<>();
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