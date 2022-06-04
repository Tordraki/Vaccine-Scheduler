package scheduler;

import scheduler.db.ConnectionManager;
import scheduler.model.Caregiver;
import scheduler.model.Patient;
import scheduler.model.Vaccine;
import scheduler.util.Util;
import java.sql.*;
import java.util.ArrayList;
import java.util.Random;
import java.lang.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Date;

public class Scheduler {

    // objects to keep track of the currently logged-in user
    // Note: it is always true that at most one of currentCaregiver and currentPatient is not null
    //       since only one user can be logged-in at a time
    private static Caregiver currentCaregiver = null;
    private static Patient currentPatient = null;

    public static void main(String[] args) {
        // printing greetings text

        System.out.println();
        System.out.println("Welcome to the COVID-19 Vaccine Reservation Scheduling Application!");
        System.out.println("*** Please enter one of the following commands ***");
        System.out.println("> create_patient <username> <password>");  //TODO: implement create_patient (Part 1)
        System.out.println("> create_caregiver <username> <password>");
        System.out.println("> login_patient <username> <password>");  // TODO: implement login_patient (Part 1)
        System.out.println("> login_caregiver <username> <password>");
        System.out.println("> search_caregiver_schedule <date>");  // TODO: implement search_caregiver_schedule (Part 2)
        System.out.println("> reserve <date> <vaccine>");  // TODO: implement reserve (Part 2)
        System.out.println("> upload_availability <date>");
        System.out.println("> cancel <appointment_id>");  // TODO: implement cancel (extra credit)
        System.out.println("> add_doses <vaccine> <number>");
        System.out.println("> show_appointments");  // TODO: implement show_appointments (Part 2)
        System.out.println("> logout");  // TODO: implement logout (Part 2)
        System.out.println("> quit");
        System.out.println();

        // read input from user
        BufferedReader r = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            System.out.print("> ");
            String response = "";
            try {
                response = r.readLine();
            } catch (IOException e) {
                System.out.println("Please try again!");
            }
            // split the user input by spaces
            String[] tokens = response.split(" ");
            // check if input exists
            if (tokens.length == 0) {
                System.out.println("Please try again!");
                continue;
            }
            // determine which operation to perform
            String operation = tokens[0];
            if (operation.equals("create_patient")) {
                createPatient(tokens);
            } else if (operation.equals("create_caregiver")) {
                createCaregiver(tokens);
            } else if (operation.equals("login_patient")) {
                loginPatient(tokens);
            } else if (operation.equals("login_caregiver")) {
                loginCaregiver(tokens);
            } else if (operation.equals("search_caregiver_schedule")) {
                searchCaregiverSchedule(tokens);
            } else if (operation.equals("reserve")) {
                reserve(tokens);
            } else if (operation.equals("upload_availability")) {
                uploadAvailability(tokens);
            } else if (operation.equals("cancel")) {
                cancel(tokens);
            } else if (operation.equals("add_doses")) {
                addDoses(tokens);
            } else if (operation.equals("show_appointments")) {
                showAppointments(tokens);
            } else if (operation.equals("logout")) {
                logout(tokens);
            } else if (operation.equals("quit")) {
                System.out.println("Bye!");
                return;
            } else {
                System.out.println("Invalid operation name!");
            }
        }
    }

    private static void createPatient(String[] tokens) {
        // create_patient <username> <password>
        // check 1: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Failed to create user.");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];
        // check 2: check if the username has been taken already
        if (usernameExistsPatient(username)) {
            System.out.println("Username taken, try again!");
            return;
        }
        byte[] salt = Util.generateSalt();
        byte[] hash = Util.generateHash(password, salt);
        // create the patient
        try {
            currentPatient = new Patient.PatientBuilder(username, salt, hash).build();
            // save to caregiver information to our database
            currentPatient.saveToDB();
            System.out.println("Created user " + username);
        } catch (SQLException e) {
            System.out.println("Failed to create user.");
            e.printStackTrace();
        }
    }

    private static boolean usernameExistsPatient(String username) {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String selectUsername = "SELECT * FROM Patients WHERE Username = ?";
        try {
            PreparedStatement statement = con.prepareStatement(selectUsername);
            statement.setString(1, username);
            ResultSet resultSet = statement.executeQuery();
            // returns false if the cursor is not before the first record or if there are no rows in the ResultSet.
            return resultSet.isBeforeFirst();
        } catch (SQLException e) {
            System.out.println("Error occurred when checking username");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
        return true;
    }

    private static void createCaregiver(String[] tokens) {
        // create_caregiver <username> <password>
        // check 1: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Failed to create user.");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];
        // check 2: check if the username has been taken already
        if (usernameExistsCaregiver(username)) {
            System.out.println("Username taken, try again!");
            return;
        }
        byte[] salt = Util.generateSalt();
        byte[] hash = Util.generateHash(password, salt);
        // create the caregiver
        try {
            currentCaregiver = new Caregiver.CaregiverBuilder(username, salt, hash).build();
            // save to caregiver information to our database
            currentCaregiver.saveToDB();
            System.out.println("Created user " + username);
        } catch (SQLException e) {
            System.out.println("Failed to create user.");
            e.printStackTrace();
        }
    }

    private static boolean usernameExistsCaregiver(String username) {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String selectUsername = "SELECT * FROM Caregivers WHERE Username = ?";
        try {
            PreparedStatement statement = con.prepareStatement(selectUsername);
            statement.setString(1, username);
            ResultSet resultSet = statement.executeQuery();
            // returns false if the cursor is not before the first record or if there are no rows in the ResultSet.
            return resultSet.isBeforeFirst();
        } catch (SQLException e) {
            System.out.println("Error occurred when checking username");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
        return true;
    }

    private static void loginPatient(String[] tokens) {
        // login_patient <username> <password>
        // check 1: if someone's already logged-in, they need to log out first
        if (currentCaregiver != null || currentPatient != null) {
            System.out.println("User already logged in.");
            return;
        }
        // check 2: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Login failed.");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];

        Patient patient = null;
        try {
            patient = new Patient.PatientGetter(username, password).get();
        } catch (SQLException e) {
            System.out.println("Login failed.");
            e.printStackTrace();
        }
        // check if the login was successful
        if (patient == null) {
            System.out.println("Login failed.");
        } else {
            System.out.println("Logged in as: " + username);
            currentPatient = patient;
        }
    }

    private static void loginCaregiver(String[] tokens) {
        // login_caregiver <username> <password>
        // check 1: if someone's already logged-in, they need to log out first
        if (currentCaregiver != null || currentPatient != null) {
            System.out.println("User already logged in.");
            return;
        }
        // check 2: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Login failed.");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];

        Caregiver caregiver = null;
        try {
            caregiver = new Caregiver.CaregiverGetter(username, password).get();
        } catch (SQLException e) {
            System.out.println("Login failed.");
            e.printStackTrace();
        }
        // check if the login was successful
        if (caregiver == null) {
            System.out.println("Login failed.");
        } else {
            System.out.println("Logged in as: " + username);
            currentCaregiver = caregiver;
        }
    }

    private static void searchCaregiverSchedule(String[] tokens) {
        // TODO: Part 2
        if (tokens.length != 2) {
            System.out.println("Search failed.");
            return;
        }
        if (currentCaregiver == null && currentPatient == null) {
            System.out.println("Please login first!");
            return;
        }
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String selectUsername = "SELECT A.Username AS name FROM Availabilities AS A WHERE A.Time = ? ORDER BY A.Username;";
        String showVaccines = "SELECT * FROM Vaccines;";

        try {
            PreparedStatement statement = con.prepareStatement(selectUsername);
            statement.setString(1, tokens[1]);
            ResultSet resultSet = statement.executeQuery();
            while(resultSet.next()){
                System.out.print(resultSet.getString("name") + " ");
            }
            System.out.println();

            PreparedStatement statement2 = con.prepareStatement(showVaccines);

            ResultSet resultSet2 = statement2.executeQuery();
            while(resultSet2.next()){
                System.out.print(resultSet2.getString("Name") + " " + resultSet2.getInt("Doses"));
            }
            System.out.println();


        } catch (SQLException e) {
            System.out.println("Please try again!");
            e.printStackTrace();
        } finally {

            cm.closeConnection();
        }
    }

    private static void reserve(String[] tokens) {
        // TODO: Part 2
        if (tokens.length != 3) {
            System.out.println("Reserve failed.");
            return;
        }
        if (currentPatient == null && currentCaregiver == null) {
            System.out.println("Please login first!");
            return;
        }
        else if (currentPatient == null){
            System.out.println("Please login as a Patient!");
        }

        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();
        Date d;
        try {
            d = Date.valueOf(tokens[1]);
        } catch (IllegalArgumentException e){
            System.out.println("Please enter a valid date in the format YYYY-MM-DD!");
            return;
        }
        String selectUsername = "SELECT Username FROM Availabilities WHERE Time = ? ORDER BY Username ;";
        try {

            PreparedStatement statement = con.prepareStatement(selectUsername);
            statement.setDate(1, d);
            ResultSet resultSet = statement.executeQuery();
            ArrayList<String> caregivers = new ArrayList();
            while(resultSet.next()){
                caregivers.add(resultSet.getString("Username"));
            }
            if(caregivers.size() == 0){
                System.out.println("There are no caregivers available, try a different date.");
                return;
            }

            Vaccine vax = new Vaccine.VaccineGetter(tokens[2]).get();
            if(vax == null){
                System.out.println("No vaccine available.");
                return;
            }
            //else if(vax.availableDoses == 0){

            //}
            else{
                vax.decreaseAvailableDoses(1);
            }
            int id = generateAppointmentID();
            String insert = "INSERT INTO Appointments(ID, Time, carename, patientname, vaccinename) VALUES (?, ?, ?, ?, ?)";
            statement = con.prepareStatement(insert);

            statement.setInt(1, id);
            statement.setDate(2, d);
            statement.setString(3, caregivers.get(0));
            statement.setString(4, currentPatient.getUsername());
            statement.setString(5, tokens[2]);
            statement.execute();
            System.out.println("Successfully reserved!");
            String changeAvailability = "DELETE FROM Availabilities WHERE Time = ? AND Username = ?;";
            statement = con.prepareStatement(changeAvailability);
            statement.setDate(1, d);
            statement.setString(2, caregivers.get(0));
            statement.execute();
            System.out.println("Appointment ID: "+ id + ", Caregiver username: " + caregivers.get(0));






        } catch (SQLException e) {
            System.out.println("Please try again!");
            e.printStackTrace();
        } finally {

            cm.closeConnection();
        }

    }

    private static void uploadAvailability(String[] tokens) {
        // upload_availability <date>
        // check 1: check if the current logged-in user is a caregiver
        if (currentCaregiver == null) {
            System.out.println("Please login as a caregiver first!");
            return;
        }
        // check 2: the length for tokens need to be exactly 2 to include all information (with the operation name)
        if (tokens.length != 2) {
            System.out.println("Please try again!");
            return;
        }
        String date = tokens[1];
        try {
            Date d = Date.valueOf(date);
            currentCaregiver.uploadAvailability(d);
            System.out.println("Availability uploaded!");
        } catch (IllegalArgumentException e) {
            System.out.println("Please enter a valid date!");
        } catch (SQLException e) {
            System.out.println("Error occurred when uploading availability");
            e.printStackTrace();
        }
    }

    private static void cancel(String[] tokens) {
        // TODO: Extra credit
        if (tokens.length != 2) {
            System.out.println("Search failed.");
            return;
        }
        //do checks
        //remove appointment from appointments
        //add availability
        if (currentCaregiver == null && currentPatient == null) {
            System.out.println("Please login first!");
            return;
        }

        try {
            ConnectionManager cm = new ConnectionManager();
            Connection con = cm.createConnection();

            String query = "SELECT Time AS time, carename AS caregiver, vaccinename FROM Appointments WHERE ID = ?;";
            PreparedStatement statement = con.prepareStatement(query);
            statement.setInt(1, Integer.parseInt(tokens[1]));
            ResultSet resultSet = statement.executeQuery();
            Date D = null;
            String name = "";
            String vaxname = "";
            while(resultSet.next()){
                D = resultSet.getDate("Time");
                name = resultSet.getString("caregiver");
                vaxname = resultSet.getString("vaccinename");

            }
            if(D == null){
                System.out.println("No appointment with that ID");
                return;
            }

            String addAvailability = "INSERT INTO Availabilities(Time, Username) VALUES (?, ?);";
            statement = con.prepareStatement(addAvailability);
            statement.setDate(1, D);
            statement.setString(2, name);
            statement.execute();

            String deleteApp = "DELETE FROM Appointments WHERE ID = ?;";
            statement = con.prepareStatement(deleteApp);
            statement.setInt(1, Integer.parseInt(tokens[1]));
            statement.execute();

            Vaccine vax = new Vaccine.VaccineGetter(vaxname).get();;
            vax.decreaseAvailableDoses(1);
            System.out.println("Success");
        } catch (SQLException e) {
            System.out.println("Please try again!");
            e.printStackTrace();
        }

    }

    private static void addDoses(String[] tokens) {
        // add_doses <vaccine> <number>
        // check 1: check if the current logged-in user is a caregiver
        if (currentCaregiver == null) {
            System.out.println("Please login as a caregiver first!");
            return;
        }
        // check 2: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Please try again!");
            return;
        }
        String vaccineName = tokens[1];
        int doses = Integer.parseInt(tokens[2]);
        Vaccine vaccine = null;
        try {
            vaccine = new Vaccine.VaccineGetter(vaccineName).get();
        } catch (SQLException e) {
            System.out.println("Error occurred when adding doses");
            e.printStackTrace();
        }
        // check 3: if getter returns null, it means that we need to create the vaccine and insert it into the Vaccines
        //          table
        if (vaccine == null) {
            try {
                vaccine = new Vaccine.VaccineBuilder(vaccineName, doses).build();
                vaccine.saveToDB();
            } catch (SQLException e) {
                System.out.println("Error occurred when adding doses");
                e.printStackTrace();
            }
        } else {
            // if the vaccine is not null, meaning that the vaccine already exists in our table
            try {
                vaccine.increaseAvailableDoses(doses);
            } catch (SQLException e) {
                System.out.println("Error occurred when adding doses");
                e.printStackTrace();
            }
        }
        System.out.println("Doses updated!");
    }

    private static void showAppointments(String[] tokens) {
        // TODO: Part 2
        if (tokens.length != 1) {
            System.out.println("Search failed.");
            return;
        }
        if (currentCaregiver == null && currentPatient == null) {
            System.out.println("Please login first!");
            return;
        }
        String user;
        String searchAppointment = "";
        try {
            ConnectionManager cm = new ConnectionManager();
            Connection con = cm.createConnection();



            if(currentPatient != null){
                searchAppointment = "SELECT ID, vaccinename, Time, carename AS Caregiver FROM Appointments WHERE patientname = ? ORDER BY ID;";
                user = currentPatient.getUsername();
            }
            else{
                searchAppointment = "SELECT ID, vaccinename, Time, patientname AS Patient FROM Appointments WHERE carename = ? ORDER BY ID;";
                user = currentCaregiver.getUsername();
            }
            PreparedStatement statement = con.prepareStatement(searchAppointment);
            statement.setString(1, user);
            ResultSet resultSet = statement.executeQuery();
            ResultSetMetaData rsmd = resultSet.getMetaData();
            System.out.println("Appointments for " + user);
            while(resultSet.next()){
                System.out.println("Appointment ID: " + resultSet.getString("ID") + " Vaccine: "
                        + resultSet.getString("vaccinename") + " Date: " + resultSet.getString("Time")
                        + " " + rsmd.getColumnName(4) + ": " + resultSet.getString(4));
            }
        } catch (SQLException e) {
            System.out.println("Please try again!");
            e.printStackTrace();
        }
    }

    private static void logout(String[] tokens) {
        if (tokens.length != 1) {
            System.out.println("Please try again!");
            return;
        }
        if (currentCaregiver == null && currentPatient == null) {
            System.out.println("Please login first.");
            return;
        }
        else{
            currentCaregiver = null;
            currentPatient = null;
            System.out.println("Successfully logged out!");
            return;
        }
    }

    private static int generateAppointmentID() {
        return ((int)Math.floor(Math.random() * 899999) + 1000);
    }
}
