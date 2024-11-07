package org.example;

import javax.swing.*;

import java.awt.*;

import java.awt.event.*;

import java.util.ArrayList;

import java.util.Date;

import java.util.List;

import java.text.ParseException;

import java.text.SimpleDateFormat;

import java.util.concurrent.TimeUnit;

/*

* Facade – Imagine that the booking system is complex and consists of many different parts. The facade (the BookingFacade class) simply simplifies working with this system by providing you with a single method for booking. You don’t have to worry about how it works internally; you just call bookRoom and that’s it.



Abstract Factory – If you need to create a room, for example, a single or double room, you don’t create them manually. Instead, you use factories (like SingleRoomFactory and DoubleRoomFactory). These factories know how to create these objects, and you don’t have to worry about how exactly they are structured.



Strategy – There are different ways to calculate discounts. For example, you can get a discount for a birthday or a corporate program. The DiscountManager class has a strategy that decides how to calculate the discount based on the type.



Singleton – Sometimes you need a class to exist only once in the entire system. For instance, discounts are calculated through DiscountManager, and you don’t want there to be multiple instances of this class. The Singleton pattern ensures that only one instance of this class exists.



Chain of Responsibility – During booking, there can be various steps, such as selecting a room, applying a discount, checking availability, and so on. Each step passes to the next. For example, if you first check the discount, the next step will be to check room availability, and so on.



Bridge – You separate the idea of booking itself (for example, the BookingSystem interface) from how it specifically works (concrete hotels or rooms). This allows you to change one part without affecting the other. If you want to add a new type of hotel, you don’t need to rewrite everything; you just add a new hotel class.

*

* */

// Main facade that simplifies the booking process for the user

class BookingFacade {

    private BookingModel model;



    public BookingFacade(BookingModel model) {

        this.model = model;  // Initialize model

    }



    // Method to book a room by interacting with the complex internal system

    public String bookRoom(String branchAddress, String roomType, String guestName, String discountType, String checkInDate, String checkOutDate, int guestCount) {

        // Find the hotel branch from the list based on address

        HotelBranch branch = model.getBranches().stream()

                .filter(b -> b.getAddress().equals(branchAddress))

                .findFirst()

                .orElse(null);



        if (branch == null) { // If the branch is not found

            return "Branch not found!";

        }



        // Find an available room of the specified type in the branch

        Room availableRoom = branch.getAvailableRooms(roomType).stream().findFirst().orElse(null);

        if (availableRoom == null) { // If no rooms are available

            return "No available rooms of this type.";

        }



        // Calculate price by multiplying room price by the number of nights

        double price = availableRoom.getPrice() * (checkOutDate.compareTo(checkInDate)); // Calculate nights

        price = DiscountManager.getInstance().applyDiscount(discountType, price);  // Apply discount based on type



        // Create the booking and add it to the model

        model.bookRoom(branch, availableRoom, guestName, discountType, checkInDate, checkOutDate, price);

        return "Booking Successful!";

    }

}



// Abstract Factory pattern to create rooms

abstract class RoomFactory {

    public abstract Room createRoom(String roomNumber);

}



// Concrete factory for creating single rooms

class SingleRoomFactory extends RoomFactory {

    @Override

    public Room createRoom(String roomNumber) {

        return new Room(roomNumber, 100.0, "Single");

    }

}



// Concrete factory for creating double rooms

class DoubleRoomFactory extends RoomFactory {

    @Override

    public Room createRoom(String roomNumber) {

        return new Room(roomNumber, 150.0, "Double");

    }

}



// Represents a hotel branch

class HotelBranch {

    private String address;

    private List<Room> rooms = new ArrayList<>();



    public HotelBranch(String address) {

        this.address = address;



        // Using Factory Methods to create rooms

        RoomFactory singleRoomFactory = new SingleRoomFactory();

        RoomFactory doubleRoomFactory = new DoubleRoomFactory();



        // Add rooms to the branch

        for (int i = 1; i <= 5; i++) {

            rooms.add(singleRoomFactory.createRoom("Room " + i));  // Create single rooms

            rooms.add(doubleRoomFactory.createRoom("Room " + (i + 5)));  // Create double rooms

        }

    }



    // Get address of the branch

    public String getAddress() {

        return address;

    }



    // Get available rooms of a specific type

    public List<Room> getAvailableRooms(String roomType) {

        List<Room> availableRooms = new ArrayList<>();

        for (Room room : rooms) {

            if (room.isAvailable() && room.getType().equals(roomType)) availableRooms.add(room);  // Add if room is available

        }

        return availableRooms;

    }

}



// Represents a room in a hotel

class Room {

    private String roomNumber;

    private double price;

    private String type;

    private boolean isAvailable = true; // All rooms are available by default



    public Room(String roomNumber, double price, String type) {

        this.roomNumber = roomNumber;

        this.price = price;

        this.type = type;

    }



    public boolean isAvailable() {

        return isAvailable;

    }



    public void setAvailable(boolean available) {

        isAvailable = available;

    }



    public double getPrice() {

        return price;

    }



    public String getRoomNumber() {

        return roomNumber;

    }



    public String getType() {

        return type;

    }

}



// Model that manages hotel branches, bookings, and discounts

class BookingModel {

    private List<HotelBranch> branches = new ArrayList<>();

    private List<String> myBookings = new ArrayList<>();

    private List<Booking> bookings = new ArrayList<>();  // New list to track bookings



    public BookingModel() {

        branches.add(new HotelBranch("123 Main St")); // Adding branches

        branches.add(new HotelBranch("456 Central Ave"));

        branches.add(new HotelBranch("789 Park Blvd"));

    }



    public List<HotelBranch> getBranches() {

        return branches;

    }



    public List<String> getMyBookings() {

        return myBookings;

    }



    // Method to book a room and add it to the bookings list

    public void bookRoom(HotelBranch branch, Room room, String guestName, String discountType, String checkInDate, String checkOutDate, double totalPrice) {

        room.setAvailable(false); // Mark room as unavailable after booking



        // Create a new booking and add it to the list

        Booking newBooking = new Booking(room, guestName, branch, checkInDate, checkOutDate, totalPrice);

        bookings.add(newBooking);



        String bookingDetails = newBooking.getBookingDetails();  // Get booking details as string

        myBookings.add(bookingDetails);  // Add to 'myBookings' list

    }



    // Check if the room is available for the given dates

    public boolean isRoomAvailableForDates(Room room, String checkInDate, String checkOutDate) {

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

        try {

            // Loop through all bookings and check for conflicts

            for (Booking booking : bookings) {

                if (booking.getRoom().equals(room)) {

                    Date existingCheckIn = sdf.parse(booking.getCheckInDate());

                    Date existingCheckOut = sdf.parse(booking.getCheckOutDate());

                    Date requestedCheckIn = sdf.parse(checkInDate);

                    Date requestedCheckOut = sdf.parse(checkOutDate);



                    // If dates overlap, the room is not available

                    if ((requestedCheckIn.before(existingCheckOut) && requestedCheckIn.after(existingCheckIn)) ||

                            (requestedCheckOut.after(existingCheckIn) && requestedCheckOut.before(existingCheckOut))) {

                        return false;

                    }

                }

            }

        } catch (ParseException e) {

            e.printStackTrace();

        }

        return true;  // Room is available

    }

}



// Represents a booking made by a guest

class Booking {

    private Room room;

    private String guestName;

    private HotelBranch branch;

    private String checkInDate;

    private String checkOutDate;

    private double totalPrice;  // New field for total price



    public Booking(Room room, String guestName, HotelBranch branch, String checkInDate, String checkOutDate, double totalPrice) {

        this.room = room;

        this.guestName = guestName;

        this.branch = branch;

        this.checkInDate = checkInDate;

        this.checkOutDate = checkOutDate;

        this.totalPrice = totalPrice;

    }



    public Room getRoom() {

        return room;

    }



    public String getCheckInDate() {

        return checkInDate;

    }



    public String getCheckOutDate() {

        return checkOutDate;

    }



    public double getTotalPrice() {

        return totalPrice;

    }



    public String getBookingDetails() {

        return "Hotel: " + branch.getAddress() + ", Room: " + room.getRoomNumber() + " (" + room.getType() + "), Guest: " + guestName +

                ", Check-in: " + checkInDate + ", Check-out: " + checkOutDate + ", Total Price: $" + totalPrice;

    }

}



// Initial view where users choose actions

class InitialView extends JFrame {

    private JButton makeBookingButton;

    private JButton viewBookingsButton;



    public InitialView() {

        setTitle("Hotel Booking System");

        setSize(300, 150);

        setDefaultCloseOperation(EXIT_ON_CLOSE);

        setLocationRelativeTo(null);



        makeBookingButton = new JButton("Make a Booking");

        viewBookingsButton = new JButton("My Bookings");



        JPanel panel = new JPanel();

        panel.setLayout(new GridLayout(2, 1, 10, 10));

        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        panel.add(makeBookingButton);

        panel.add(viewBookingsButton);



        add(panel);

    }



    public void addMakeBookingButtonListener(ActionListener listener) {

        makeBookingButton.addActionListener(listener);  // Listener for make booking button

    }



    public void addViewBookingsButtonListener(ActionListener listener) {

        viewBookingsButton.addActionListener(listener);  // Listener for view bookings button

    }

}



// Main view where users select a branch for booking

class MainView extends JFrame {

    private JComboBox<String> branchSelector;

    private JButton nextButton;

    private JButton backButton;



    public MainView() {

        setTitle("Hotel Branch Selection");

        setSize(400, 200);

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        setLocationRelativeTo(null);



        branchSelector = new JComboBox<>(new String[]{"123 Main St", "456 Central Ave", "789 Park Blvd"});

        nextButton = new JButton("Next");

        backButton = new JButton("Go Back");



        JPanel panel = new JPanel(new BorderLayout());

        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        panel.add(branchSelector, BorderLayout.CENTER);

        panel.add(nextButton, BorderLayout.SOUTH);

        panel.add(backButton, BorderLayout.NORTH);



        add(panel);

    }



    public String getSelectedBranch() {

        return (String) branchSelector.getSelectedItem();

    }



    public void addNextButtonListener(ActionListener listener) {

        nextButton.addActionListener(listener);

    }



    public void addBackButtonListener(ActionListener listener) {

        backButton.addActionListener(listener);

    }

}

// BookingFormView class represents the view for the booking form, where the user can enter booking details.

class BookingFormView extends JFrame {

    private JTextField guestNameField; // Field to enter guest's name

    private JComboBox<String> discountTypeSelector; // Dropdown to select discount type

    private JComboBox<String> roomTypeSelector; // Dropdown to select room type

    private JTextField guestCountField; // Field to enter number of guests

    private JTextField checkInDateField; // Field to enter check-in date

    private JTextField checkOutDateField; // Field to enter check-out date

    private JButton submitButton; // Button to submit the booking form

    private JButton backButton; // Button to go back to the previous view

    private JLabel statusLabel; // Label to show status messages (e.g., errors, success)



    public BookingFormView() {

        setTitle("Booking Form"); // Title of the window

        setSize(400, 400); // Size of the window

        setDefaultCloseOperation(DISPOSE_ON_CLOSE); // Close window on exit

        setLocationRelativeTo(null); // Center the window on the screen



        // Initialize components

        guestNameField = new JTextField(10);

        discountTypeSelector = new JComboBox<>(new String[]{"None", "Birthday 10%", "New Year 20%", "Corporate 15%"});

        roomTypeSelector = new JComboBox<>(new String[]{"Single", "Double"});

        guestCountField = new JTextField(5);

        checkInDateField = new JTextField(10);

        checkOutDateField = new JTextField(10);

        submitButton = new JButton("Book Now");

        backButton = new JButton("Go Back");

        statusLabel = new JLabel(""); // Initially no status



        // Create a panel to hold the components in a grid layout

        JPanel panel = new JPanel(new GridLayout(10, 2, 5, 5));

        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        panel.add(new JLabel("Guest Name:"));

        panel.add(guestNameField);

        panel.add(new JLabel("Discount Type:"));

        panel.add(discountTypeSelector);

        panel.add(new JLabel("Room Type:"));

        panel.add(roomTypeSelector);

        panel.add(new JLabel("Number of Guests:"));

        panel.add(guestCountField);

        panel.add(new JLabel("Check-in Date:"));

        panel.add(checkInDateField);

        panel.add(new JLabel("Check-out Date:"));

        panel.add(checkOutDateField);

        panel.add(backButton);

        panel.add(submitButton);

        panel.add(statusLabel); // Add status label at the bottom



        add(panel); // Add the panel to the frame

    }



    // Method to add a listener for the submit button

    public void addSubmitButtonListener(ActionListener listener) {

        submitButton.addActionListener(listener);

    }



    // Method to add a listener for the back button

    public void addBackButtonListener(ActionListener listener) {

        backButton.addActionListener(listener);

    }



    // Method to get the guest's name from the form

    public String getGuestName() {

        return guestNameField.getText();

    }



    // Method to get the selected room type from the form

    public String getRoomType() {

        return (String) roomTypeSelector.getSelectedItem();

    }



    // Method to get the number of guests from the form

    public int getGuestCount() {

        return Integer.parseInt(guestCountField.getText());

    }



    // Method to get the selected discount type from the form

    public String getDiscountType() {

        return (String) discountTypeSelector.getSelectedItem();

    }



    // Method to check if a date is in the correct format (yyyy-MM-dd)

    public boolean isValidDateFormat(String date) {

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

        sdf.setLenient(false);

        try {

            sdf.parse(date);

            return true;

        } catch (ParseException e) {

            return false;

        }

    }



    // Method to get the check-in date from the form

    public String getCheckInDate() {

        return checkInDateField.getText();

    }



    // Method to get the check-out date from the form

    public String getCheckOutDate() {

        return checkOutDateField.getText();

    }



    // Method to display a status message (success or error)

    public void displayStatus(String message) {

        statusLabel.setText(message);

    }

}





// BookingSummaryView class shows the list of bookings and allows the user to proceed with payment.

class BookingSummaryView extends JFrame {

    private JTextArea bookingListArea; // Text area to display the list of bookings

    private JButton backButton; // Button to go back to the previous view



    public BookingSummaryView(List<String> myBookings) {

        setTitle("My Bookings"); // Title of the window

        setSize(400, 300); // Size of the window

        setDefaultCloseOperation(DISPOSE_ON_CLOSE); // Close window on exit

        setLocationRelativeTo(null); // Center the window on the screen



        bookingListArea = new JTextArea(); // Initialize text area to show bookings

        bookingListArea.setEditable(false); // Make the text area read-only



        // Loop through the bookings and append them to the text area

        for (String booking : myBookings) {

            bookingListArea.append(booking + "\n");

        }



        backButton = new JButton("Go Back"); // Button to go back



        JPanel buttonPanel = new JPanel(); // Panel for buttons

        buttonPanel.add(backButton);



        JPanel panel = new JPanel(new BorderLayout()); // Main panel with border layout

        panel.add(new JScrollPane(bookingListArea), BorderLayout.CENTER); // Add scrollable list area

        panel.add(buttonPanel, BorderLayout.SOUTH); // Add button panel at the bottom



        add(panel); // Add the main panel to the frame

    }







    // Method to add a listener for the back button

    public void addBackButtonListener(ActionListener listener) {

        backButton.addActionListener(listener);

    }

}





// BookingController class handles the logic for interacting with the views and model.

class BookingController {

    private BookingModel model; // The model that stores the data

    private InitialView initialView; // The initial view to start booking process

    private MainView mainView; // The main view to select the branch

    private BookingFormView bookingFormView; // The view where the user fills booking details

    private BookingSummaryView bookingSummaryView; // The view to display booking summary

    private PaymentView paymentView; // The view for payment after booking

    private BookingFacade bookingFacade;  // The facade to handle booking logic



    public BookingController(BookingModel model, InitialView initialView) {

        this.model = model;

        this.initialView = initialView;

        this.bookingFacade = new BookingFacade(model);  // Initialize the facade to handle booking



        // Set up listeners for buttons in the initial view

        initialView.addMakeBookingButtonListener(e -> openMainView());

        initialView.addViewBookingsButtonListener(e -> openBookingSummaryView());

    }



    // Method to open the main view where the user selects the branch

    private void openMainView() {

        mainView = new MainView();

        mainView.addNextButtonListener(e -> openBookingForm(mainView.getSelectedBranch()));

        mainView.addBackButtonListener(e -> {

            mainView.dispose();

            initialView.setVisible(true);

        });



        mainView.setVisible(true);

    }



    // Method to open the booking form view

    private void openBookingForm(String branchAddress) {

        bookingFormView = new BookingFormView();

        bookingFormView.addSubmitButtonListener(e -> submitBooking(branchAddress));

        bookingFormView.addBackButtonListener(e -> bookingFormView.dispose());

        bookingFormView.setVisible(true);

    }



    // Method to open the booking summary view

    private void openBookingSummaryView() {

        bookingSummaryView = new BookingSummaryView(model.getMyBookings());

        bookingSummaryView.addBackButtonListener(e -> bookingSummaryView.dispose());

        bookingSummaryView.setVisible(true);

    }



    // Method to handle the submission of the booking form

    private void submitBooking(String branchAddress) {

        try {

            String guestName = bookingFormView.getGuestName();

            String roomType = bookingFormView.getRoomType();

            String discountType = bookingFormView.getDiscountType();

            int guestCount = bookingFormView.getGuestCount();

            String checkInDate = bookingFormView.getCheckInDate();

            String checkOutDate = bookingFormView.getCheckOutDate();



            if (roomType.equals("Single") && guestCount > 1) {

                bookingFormView.displayStatus("Max allowed guest count is 1.");

                return;

            } else if (roomType.equals("Double") && guestCount > 3) {

                bookingFormView.displayStatus("Max allowed guest count is 3.");

                return;

            }

            // Validate dates

            if (!bookingFormView.isValidDateFormat(checkInDate) || !bookingFormView.isValidDateFormat(checkOutDate)) {

                bookingFormView.displayStatus("Invalid date format. Use yyyy-MM-dd.");

                return;

            }



            // Check if check-in is before check-out

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

            Date checkIn = sdf.parse(checkInDate);

            Date checkOut = sdf.parse(checkOutDate);

            if (checkIn.after(checkOut)) {

                bookingFormView.displayStatus("Check-out date must be after check-in date.");

                return;

            }



            // Process the booking via the facade

            String result = bookingFacade.bookRoom(branchAddress, roomType, guestName, discountType, checkInDate, checkOutDate, guestCount);



            if (result.equals("Booking Successful!")) {

                // Calculate the total price

                double basePrice = getBasePrice(roomType, guestCount, checkInDate, checkOutDate); // Method to calculate the base price (could vary by room type and guest count)



                double totalPrice = DiscountManager.getInstance().applyDiscount(discountType, basePrice);



                // Generate booking details string

                String bookingDetails = "Booking Details:\n" +

                        "Guest: " + guestName + "\n" +

                        "Room Type: " + roomType + "\n" +

                        "Check-in Date: " + checkInDate + "\n" +

                        "Check-out Date: " + checkOutDate;



                bookingFormView.dispose();

                // Open payment view, passing the total price along with the booking details

                openPaymentView(bookingDetails, basePrice, totalPrice);



            } else {

                bookingFormView.displayStatus(result);  // Display error message

            }

        } catch (Exception e) {

            e.printStackTrace();

        }

    }



    // Method to get the base price of the room (this could be more complex depending on your requirements)

    private double getBasePrice(String roomType, int guestCount, String checkInDate, String checkOutDate) throws ParseException {

        double price = 0;

        switch (roomType) {

            case "Single":

                price = 100;

                break;

            case "Double":

                price = 150;

                break;



        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

        Date checkIn = sdf.parse(checkInDate);

        Date checkOut = sdf.parse(checkOutDate);



        // Calculate the number of days between check-in and check-out

        long diffInMillies = checkOut.getTime() - checkIn.getTime();

        long diffInDays = TimeUnit.DAYS.convert(diffInMillies, TimeUnit.MILLISECONDS);



        // Multiply the price by the number of days and the guest count

        return price * diffInDays * guestCount; // Total price = price per day * number of days * number of guests



    }





    // Method to handle payment for bookings

    private void payForBookings() {

        JOptionPane.showMessageDialog(bookingSummaryView, "Payment successful. Voucher sent!");

    }



    private void openPaymentView(String bookingDetails, double basePrice, double totalPrice) {

        paymentView = new PaymentView(bookingDetails, basePrice, totalPrice);

        paymentView.addPayNowButtonListener(e -> processPayment());

        paymentView.setVisible(true);

    }





    // Method to process payment (currently just shows a simple message)

    private void processPayment() {

        JOptionPane.showMessageDialog(paymentView, "Payment Successful! Thank you for your booking.");

        paymentView.dispose();

    }

}





// PaymentView class shows the booking details with a Pay Now button





class PaymentView extends JFrame {

    private JTextArea bookingDetailsArea; // Text area for booking details

    private JLabel basePriceLabel; // Label for displaying base price

    private JLabel totalPriceLabel; // Label for displaying total price after discount

    private JButton payNowButton; // Button to make payment



    // Constructor for PaymentView, accepting both booking details and total price

    public PaymentView(String bookingDetails, double basePrice, double totalPrice) {

        setTitle("Payment"); // Title of the window

        setSize(400, 300); // Size of the window

        setDefaultCloseOperation(DISPOSE_ON_CLOSE); // Close window on exit

        setLocationRelativeTo(null); // Center the window on the screen



        // Initialize text area for booking details

        bookingDetailsArea = new JTextArea();

        bookingDetailsArea.setEditable(false); // Make the text area read-only

        bookingDetailsArea.append(bookingDetails); // Append the booking details



        // Initialize labels for prices

        basePriceLabel = new JLabel("Base Price: $" + basePrice);

        basePriceLabel.setFont(new Font("Arial", Font.BOLD, 14));



        totalPriceLabel = new JLabel("Total Price (after discount): $" + totalPrice);

        totalPriceLabel.setFont(new Font("Arial", Font.BOLD, 16)); // Set font style



        // Initialize "Pay Now" button

        payNowButton = new JButton("Pay Now");



        // Create a vertical box layout for price labels

        JPanel pricePanel = new JPanel();

        pricePanel.setLayout(new BoxLayout(pricePanel, BoxLayout.Y_AXIS));

        pricePanel.add(basePriceLabel);

        pricePanel.add(Box.createVerticalStrut(10)); // Add some space between labels

        pricePanel.add(totalPriceLabel);



        // Create a main panel with BorderLayout

        JPanel panel = new JPanel(new BorderLayout());

        panel.add(pricePanel, BorderLayout.NORTH); // Add price panel at the top

        panel.add(new JScrollPane(bookingDetailsArea), BorderLayout.CENTER); // Add scrollable booking details

        panel.add(payNowButton, BorderLayout.SOUTH); // Add "Pay Now" button at the bottom



        add(panel); // Add main panel to the frame

    }



    // Method to add a listener for the "Pay Now" button

    public void addPayNowButtonListener(ActionListener listener) {

        payNowButton.addActionListener(listener);

    }

}









// DiscountManager class implements singleton pattern to handle discounts.

class DiscountManager {

    private static DiscountManager instance;



    private DiscountManager() { } // Private constructor to prevent multiple instances



    // Get the single instance of the DiscountManager

    public static DiscountManager getInstance() {

        if (instance == null) {

            instance = new DiscountManager();

        }

        return instance;

    }



    // Method to apply discount based on the selected discount type

    public double applyDiscount(String discountType, double price) {

        double discount = 0;

        switch (discountType) {

            case "Birthday 10%":

                discount = 0.1;  // 10% off for birthday

                break;

            case "New Year 20%":

                discount = 0.2;  // 20% off for New Year

                break;

            case "Corporate 15%":

                discount = 0.15; // 15% off for corporate

                break;

            default:

                break;

        }

        return price * (1 - discount); // Return the discounted price

    }

}



// Main class to run the program

public class Main {

    public static void main(String[] args) {

        SwingUtilities.invokeLater(() -> {

            InitialView initialView = new InitialView();

            BookingModel model = new BookingModel();

            new BookingController(model, initialView); // Set up the controller with model and view

            initialView.setVisible(true); // Show the initial view

        });

    }

}