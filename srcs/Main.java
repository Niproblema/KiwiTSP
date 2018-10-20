import java.util.HashMap;
import java.util.Scanner;

public class Main{

    static Scanner scanner;

    static int N;
    static Area arStart;
    static class Data {
        static HashMap<String, Area> areas;
        static HashMap<String, Airport> airports;
        static HashMap<String, Flight> flights;
    }

    public static void main(String[] args) {
        scanner = new Scanner(System.in);
        N = scanner.nextInt();
        String start = scanner.next();
        scanner.nextLine();

        Data.areas = new HashMap<>(N);
        Data.airports = new HashMap<>();
        Data.flights = new HashMap<>();


        for(int i = 0; i < N; i++){
            String name = scanner.nextLine();
            String[] airports = scanner.nextLine().split(" ");
            Area inArea = new Area(name, airports);
            Data.areas.put(inArea.name, inArea);
        }
        arStart = Data.areas.get("start");



        //System.out.println(N+" "+start);


    }



}




class Area{
    String name;

    public Area(String name, String[] airports){
        this.name = name;

        for(int i = 0; i < airports.length; i++){
            System.out.print(airports[i]);
        }
    }
}

class Airport{

}

class Flight{

}