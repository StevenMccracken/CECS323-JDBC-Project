import java.sql.*;
import java.util.ArrayList;
import java.util.Scanner;

public class TermProject {
    static final String singleAlbumHeaderDisplayFormat = "\n%-25s%-25s%-25s%-12s%-15s%-8s%-25s%-13s%-20s%-35s%-25s%-13s\n";
    static final String singleAlbumResultsDisplayFormat = "%-25s%-25s%-25s%-12d%-15s%-8d%-25s%-13d%-20s%-35s%-25s%-13s\n";
    static final String allAlbumsHeaderDisplayFormat = "\n%-25s%-25s%-25s%-15s%-8s%-10s\n";
    static final String allAlbumsResultsDisplayFormat = "%-25s%-25s%-25s%-15s%-8s%-10s\n";
    static Connection connection = null;

    public static void main(String[] args) {
        boolean connectionStarted = establishConnection();
        if(!connectionStarted) System.exit(0);
        
        Scanner in = new Scanner(System.in);
        boolean done = false;
        
        System.out.println("Welcome!");
        while(!done) {
            int menu1Choice = getIntInput("\n1) View all albums\n2) View a specific album\n3) Create an album\n4) Replace a studio\n5) Remove an album\n6) Quit\n");
            
            switch(menu1Choice) {
                case 1:
                    listAllAlbums();
                    break;
                case 2:
                    System.out.print("\nEnter the album title you wish to view: ");
                    String album = in.nextLine();
                    listAlbum(album, false);
                    break;
                case 3:
                    insertAlbum();
                    break;
                case 4:
                    replaceStudio();
                    break;
                case 5:
                    System.out.print("\nEnter the album title you wish to remove: ");
                    album = in.nextLine();
                    removeAlbum(album);
                    break;
                case 6: done = true;
                    break;
                default: System.out.println("Invalid menu selection. Please try again");
            }
        }
        terminateConnection();
    }
    
    public static ResultSet executeStatement(String query, Object[] args) {
        PreparedStatement statement = null;
        try {
            statement = connection.prepareStatement(query);
            for(int i = 0; i < args.length; i++) {
                if(args[i].getClass() == String.class)
                    statement.setString(i+1, (String)args[i]);
                else if(args[i].getClass() == Integer.class)
                    statement.setInt(i+1, (int)args[i]);
                else if(args[i].getClass() == Date.class)
                    statement.setDate(i+1, (Date)args[i]);
            }
            
            return statement.executeQuery();
        } catch(SQLException e) {
            e.printStackTrace();
            return null;
        }
    }
    
    public static void listAllAlbums() {
        ResultSet results = executeStatement("SELECT * FROM Albums", new Object[]{});
        
        System.out.printf(allAlbumsHeaderDisplayFormat, "Title", "Group", "Studio", "Date Recorded", "Length", "# of Songs");
        try {    
            while (results.next()) {
                String title = results.getString("AlbumTitle");
                String group = results.getString("GroupName");
                String studio = results.getString("StudioName");
                Date date = results.getDate("DateRecorded");
                int length = results.getInt("Length");
                int numSongs = results.getInt("NumberOfSongs");
                
                System.out.printf(allAlbumsResultsDisplayFormat, title, group, studio, date, length, numSongs);
            }
            results.close();
        } catch(SQLException e) { e.printStackTrace(); }
    }
    
    public static void listAlbum(String albumName, boolean hideOutput) {
        ResultSet results = executeStatement("SELECT * From Albums inner join RecordingGroups using (GroupName)"
                            + " inner join RecordingStudios using (StudioName) where AlbumTitle = ?", new Object[] {albumName});
        
        try {
            if(results.next()) {
                String studio = results.getString("StudioName");
                String group = results.getString("GroupName");
                String album = results.getString("AlbumTitle");
                int numSongs = results.getInt("NumberOfSongs");
                Date dateRecorded = results.getDate("DateRecorded");
                int length = results.getInt("Length");
                String lead = results.getString("LeadSinger");
                int yearFormed = results.getInt("YearFormed");
                String genre = results.getString("Genre");
                String address = results.getString("StudioAddress");
                String owner = results.getString("StudioOwner");
                String phone = results.getString("StudioPhone");
                
                if(!hideOutput) {
                    System.out.printf(singleAlbumHeaderDisplayFormat, "Studio", "Group", "Album", "# of Songs", "Date Recorded",
                            "Length", "Lead Singer", "Year Formed", "Genre", "Studio Address", "Studio Owner", "Studio Phone");
                }
                
                System.out.printf(singleAlbumResultsDisplayFormat, studio, group, album, numSongs,
                            dateRecorded, length, lead, yearFormed, genre, address, owner, phone);
        
            }
            else System.out.println(albumName + " does not exist");
        } catch(SQLException e) { e.printStackTrace(); }
    }
    
    public static void insertAlbum() {
        Scanner in = new Scanner(System.in);
        String[] prompts = {"Album title: ","Group name: ","Studio name: ","# of songs: ","Length: ","Year recorded: ","Month recorded: ","Day recorded: "};
        Object[] albumInfo = new Object[6];
        int year = 0, month = 0, day = 0;
        
        System.out.println("\nEnter album info\n----------------");
        for(int i = 0; i < 8; ) {
            if(i < 3) {
                System.out.print(prompts[i]);
                albumInfo[i] = in.nextLine();
                i++;
            }
            else {
                if(i == 5) {
                    year = getIntInput(prompts[i]);
                    if(year < 10000 && year > -9999) i++;
                    else System.out.println("Invalid year");
                }
                else if(i == 6)
                    month = getIntInput(prompts[i++]);
                else if(i == 7)
                    day = getIntInput(prompts[i++]);
                else
                    albumInfo[i] = getIntInput(prompts[i++]);
            }
            System.out.print("\r");
        }
        
        Date dateRecorded = new Date(year-1900,month-1,day);
        albumInfo[5] = dateRecorded;
        
        boolean goodGroup = false, goodStudio = false;
        while(!goodGroup || !goodStudio) {
            if(!goodGroup) {
                ResultSet groupResult = executeStatement("SELECT GroupName FROM RecordingGroups WHERE GroupName = ?", new Object[] {albumInfo[1]});
                try {
                    if(groupResult.next()) goodGroup = true;
                } catch(SQLException e) { e.printStackTrace(); }
                if(!goodGroup) {
                    System.out.println("\nThe group " + albumInfo[1] + " doesn't exist yet");
                    int menuChoice = getIntInput("1) Create a new group called " + albumInfo[1] + "\n2) Choose from a list of groups to add " + albumInfo[0] + " to\n");
                    if(menuChoice == 1) {
                        String[] groupPrompts = {"Lead singer: ","Year formed: ","Genre: "};
                        Object[] groupInfo = new Object[4];
                        groupInfo[0] = (String)albumInfo[1];
                        
                        System.out.println("\nGroup information\n-----------------");
                        Scanner input = new Scanner(System.in);
                        for(int i = 1; i < 4; i++) {
                            if(i == 2) groupInfo[2] = getIntInput(groupPrompts[1]);
                            else {
                                System.out.print(groupPrompts[i-1]);
                                groupInfo[i] = input.nextLine();
                            }
                            System.out.print("\r");
                        }
                        
                        insertRecordingGroup(groupInfo);
                        System.out.println(groupInfo[0] + " added to groups!");
                        goodGroup = true;
                    }
                    else if(menuChoice == 2) {
                        ResultSet size = executeStatement("SELECT GroupName FROM RecordingGroups", new Object[]{});
                        try {
                            if(!size.next()) {
                                System.out.println("No groups exist");
                                continue;
                            }
                        } catch(SQLException e) { e.printStackTrace(); }
                        
                        ResultSet results = executeStatement("SELECT GroupName FROM RecordingGroups", new Object[]{});
                        ArrayList<String> groups = new ArrayList<String>();
                        
                        System.out.println("\nGroups\n------");
                        try {
                            while(results.next()) {
                                groups.add(results.getString("GroupName"));
                                System.out.println((groups.size()) + ") " + results.getString("GroupName"));
                            }
                        } catch(SQLException e) { e.printStackTrace(); }
                        System.out.println((groups.size()+1) + ") Go back");

                        boolean goodMenuChoice = false, skip = false;
                        while(!goodMenuChoice) {
                            int groupChoice = getIntInput("");
                            if((groupChoice-1) >= 0 && (groupChoice-1) < groups.size()) {
                                albumInfo[1] = groups.get(groupChoice-1);
                                goodMenuChoice = true;
                            }
                            else if((groupChoice-1) == groups.size()) {
                                goodMenuChoice = true;
                                skip = true;
                            }
                            else System.out.println("Invalid group choice");
                        }
                        if(skip) continue;
                        goodGroup = true;
                    }
                    else System.out.println("Invalid menu choice");
                }
            }
            if(!goodStudio) {
                ResultSet studioResult = executeStatement("SELECT StudioName FROM RecordingStudios WHERE StudioName = ?", new Object[] {albumInfo[2]});
                try {
                    if(studioResult.next()) goodStudio = true;
                } catch(SQLException e) { e.printStackTrace(); }
                
                if(!goodStudio) {
                    while(!goodStudio) {
                        System.out.println("\nThe studio " + albumInfo[2] + " doesn't exist yet");
                        int menuChoice = getIntInput("1) Create a new studio called " + albumInfo[2] + "\n2) Choose from a list of studios to add " + albumInfo[0] + " to\n");
                        if(menuChoice == 1) {
                            String[] studioPrompts = {"Studio address: ","Studio owner: ","Studio phone: "};
                            String[] studioInfo = new String[4];
                            studioInfo[0] = (String)albumInfo[2];
                            
                            System.out.println("\nStudio information\n------------------");
                            Scanner input = new Scanner(System.in);
                            for(int i = 1; i < 4; ) {
                                System.out.print(studioPrompts[i-1]);
                                studioInfo[i] = input.nextLine();

                                if(i == 3) {
                                    if(studioInfo[3].length() > 13)
                                        System.out.println("13-digit phone number only");
                                    else i++;
                                }
                                else i++;
                                System.out.print("\r");
                            }
                            insertRecordingStudio(studioInfo);
                            goodStudio = true;
                        }
                        else if(menuChoice == 2) {
                            ResultSet size = executeStatement("SELECT StudioName FROM RecordingStudios", new Object[]{});
                            try {
                                if(!size.next()) {
                                    System.out.println("No groups exist");
                                    continue;
                                }
                            } catch(SQLException e) { e.printStackTrace(); }
                            
                            ResultSet results = executeStatement("SELECT StudioName FROM RecordingStudios", new Object[]{});
                            ArrayList<String> studios = new ArrayList<String>();
                            
                            System.out.println("\nStudios\n-------");
                            try {
                                while(results.next()) {
                                    studios.add(results.getString("StudioName"));
                                    System.out.println((studios.size()) + ") " + results.getString("StudioName"));
                                }
                            } catch(SQLException e) { e.printStackTrace(); }
                            System.out.println((studios.size()+1) + ") Go back");
                            
                            boolean goodMenuChoice = false, skip = false;
                            while(!goodMenuChoice) {
                                int studioChoice = getIntInput("");
                                if((studioChoice-1) >= 0 && (studioChoice-1) < studios.size()) {
                                    albumInfo[2] = studios.get(studioChoice-1);
                                    goodMenuChoice = true;
                                }
                                else if((studioChoice-1) == studios.size()) {
                                    goodMenuChoice = true;
                                    skip = true;
                                }
                                else System.out.println("Invalid studio choice");
                            }
                            if(skip) continue;
                            goodStudio = true;
                        }
                        else System.out.println("Invalid menu choice");
                   }
                }
            }
        }
        
        boolean canInsert = false;
        Scanner input = new Scanner(System.in);
        while(!canInsert) {
            ResultSet results = executeStatement("SELECT AlbumTitle FROM Albums WHERE AlbumTitle = ? AND GroupName = ?", new Object[] {albumInfo[0], albumInfo[1]});
            try {
                if(results.next()) {
                    System.out.print("\n" + albumInfo[0] + " is an album that " + albumInfo[1] + " has already released.\nEnter a new album title: ");
                    albumInfo[0] = input.nextLine();
                }
                else canInsert = true;
            } catch(SQLException e) { e.printStackTrace(); }
        }
        insertAlbum(albumInfo);
    }
    
    public static void replaceStudio() {
        String studio = "";
        ResultSet isEmpty = executeStatement("SELECT StudioName FROM RecordingStudios", new Object[]{});
        try {
            if(!isEmpty.next()) {
                System.out.println("No studios exist to replace");
                return;
            }
        } catch(SQLException e) { e.printStackTrace(); }
        
        ResultSet results = executeStatement("SELECT StudioName FROM RecordingStudios", new Object[]{});
        ArrayList<String> studios = new ArrayList<String>();

        System.out.println("\nSelect a studio to replace\n---------------------------");
        try {
            while(results.next()) {
                studios.add(results.getString("StudioName"));
                System.out.println((studios.size()) + ") " + results.getString("StudioName"));
            }
        } catch(SQLException e) { e.printStackTrace(); }
        System.out.println((studios.size()+1) + ") Return to main menu");

        while(true) {
            int studioChoice = getIntInput("");
            if((studioChoice-1) >= 0 && (studioChoice-1) < studios.size()) {
                studio = studios.get(studioChoice-1);
                break;
            }
            else if((studioChoice-1) == studios.size()) return;
            else System.out.println("Invalid studio choice");
        }
        
        String[] studioPrompts = {"Studio name: ","Studio address: ","Studio owner: ","Studio phone: "};
        String[] studioInfo = new String[4];

        System.out.println("\nEnter new studio information\n----------------------------");
        Scanner input = new Scanner(System.in);
        for(int i = 0; i < 4; ) {
            System.out.print(studioPrompts[i]);
            studioInfo[i] = input.nextLine();
            
            if(i == 0) {
                ResultSet doesStudioExist = executeStatement("SELECT * FROM RecordingStudios WHERE StudioName = ?", new Object[] {studioInfo[0]});
                try {
                    if(doesStudioExist.next()) System.out.println(studioInfo[0] + " already exists");
                    else i++;
                } catch(SQLException e) { e.printStackTrace(); }
            }
            else if(i == 3) {
                if(studioInfo[3].length() > 13)
                    System.out.println("13-digit phone number only");
                else i++;
            }
            else i++;
            System.out.print("\r");
        }
        insertRecordingStudio(studioInfo);
        
        String query = "UPDATE Albums SET StudioName = ? WHERE StudioName = ?";
        PreparedStatement statement = null;
        try {
            statement = connection.prepareStatement(query);
            statement.setString(1, studioInfo[0]);
            statement.setString(2, studio);
            statement.executeUpdate();
        } catch(SQLException e) {
            e.printStackTrace();
        }
        
        System.out.print("\nUpdated albums\n--------------\n");
        ResultSet updatedAlbums = executeStatement("SELECT * FROM Albums WHERE StudioName = ?", new Object[] {studioInfo[0]});
        try {
            System.out.printf(singleAlbumHeaderDisplayFormat, "Studio", "Group", 
                    "Album", "# of Songs", "Date", "Length", "Lead Singer", "Year Formed", "Genre", "Studio Address", "Studio Owner", "Studio Phone");
            while(updatedAlbums.next()) {
                listAlbum(updatedAlbums.getString("AlbumTitle"), true);
            }
        } catch(SQLException e) { e.printStackTrace(); }
    }
    
    public static void removeAlbum(String album) {
        ResultSet doesAlbumExist = executeStatement("SELECT * FROM Albums WHERE AlbumTitle = ?", new Object[] {album});
        try {
            if(!doesAlbumExist.next()) {
                System.out.println(album + " does not exist.");
                return;
            }
        } catch(SQLException e) { e.printStackTrace(); }
        
        String query = "DELETE FROM Albums WHERE AlbumTitle = ?";
        PreparedStatement statement = null;
        try {
            statement = connection.prepareStatement(query);
            statement.setString(1, album);
            statement.executeUpdate();
        } catch(SQLException e) { e.printStackTrace(); }
        
        System.out.println(album + " deleted.");
    }
    
    public static void insertRecordingStudio(String[] studioInfo) {
        String query = "INSERT INTO RecordingStudios (StudioName, StudioAddress, StudioOwner, StudioPhone) VALUES (?,?,?,?)";
        PreparedStatement statement = null;
        try {
            statement = connection.prepareStatement(query);
            statement.setString(1, studioInfo[0]);
            statement.setString(2, studioInfo[1]);
            statement.setString(3, studioInfo[2]);
            statement.setString(4, studioInfo[3]);
            statement.executeUpdate();
        } catch(SQLException e) {
            e.printStackTrace();
        }
    }
    
    public static void insertRecordingGroup(Object[] groupInfo) {
        String query = "INSERT INTO RecordingGroups (GroupName, LeadSinger, YearFormed, Genre) VALUES (?,?,?,?)";
        PreparedStatement statement = null;
        try {
            statement = connection.prepareStatement(query);
            statement.setString(1, (String)groupInfo[0]);
            statement.setString(2, (String)groupInfo[1]);
            statement.setInt(3, (int)groupInfo[2]);
            statement.setString(4, (String)groupInfo[3]);
            statement.executeUpdate();
        } catch(SQLException e) { e.printStackTrace(); }
    }
    
    public static void insertAlbum(Object[] albumInfo) {
        String query = "INSERT INTO Albums (AlbumTitle, GroupName, StudioName, NumberOfSongs, Length, DateRecorded) VALUES (?,?,?,?,?,?)";
        PreparedStatement statement = null;
        try {
            statement = connection.prepareStatement(query);
            statement.setString(1, (String)albumInfo[0]);
            statement.setString(2, (String)albumInfo[1]);
            statement.setString(3, (String)albumInfo[2]);
            statement.setInt(4, (int)albumInfo[3]);
            statement.setInt(5, (int)albumInfo[4]);
            statement.setDate(6, (Date)albumInfo[5]);
            statement.executeUpdate();
            System.out.println(albumInfo[0] + " inserted!");
        } catch(SQLException e) {
            if(e.getMessage().substring(0,4).equals("Year")) 
                System.out.println("\n" + albumInfo[0] + " was not inserted because of an invalid date.");
        }
    }
    
    public static boolean establishConnection() {
        System.out.println("Registering JDBC driver...");
        
        try {
            Class.forName("org.apache.derby.jdbc.ClientDriver");
        }
        catch(ClassNotFoundException e) {
            System.out.println("Unalbe to register JDBC driver.");
            return false;
        }
        Scanner in = new Scanner(System.in);
        System.out.print("Successfully registered JDBC driver!\n\nEnter database name: ");
        String database = in.nextLine();
        
        System.out.println("\nConnecting to " + database + "...");
        
        try {
            connection = DriverManager.getConnection("jdbc:derby://localhost:1527/" + database);
        }
        catch(SQLException e) {
            System.out.println("Unable to connect to the database.");
            return false;
        }
        System.out.println("Successfully connected to the database!\n");
        return true;
    }
    
    public static void terminateConnection() {
        System.out.println("\nTerminating connection to database...");
        try {
            if(connection != null) connection.close();
        } catch(SQLException e) {
            System.out.println("Did not successfully close the connection to the database.");
            return;
        }
        System.out.println("Successfully disconnected");
    }
    
    public static int getIntInput(String prompt) {
        Scanner in = new Scanner(System.in);
        while(true) {
            System.out.print(prompt);
            if(in.hasNextInt()) {
                return in.nextInt();
            }
            String clearFlag = in.next();
            System.out.println("Invalid input");
        }
    }
}