import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.imageio.ImageIO;
import javax.mail.util.ByteArrayDataSource;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import javax.mail.*;
import javax.mail.internet.*;
import static javax.swing.JOptionPane.showMessageDialog;

public class Main {

    // Credentials for sending the patient details
    private static final String sender = "kghlaserwaitinglist@gmail.com"; // DO NOT CHANGE
    private static final String password = "wypo msua tpct hpxe"; // DO NOT CHANGE

    // Generic KGH waiting list email address which receives patients to be added
    // replace this with the real waiting list email
    private static final String waitingListEmail = "REPLACEME@email.com";

    // Consultants' initials and email addresses
    // replace each dummy address with their real email addresses
    private static final Map<String, String> consultants = new HashMap<>() {{
        put("SSD", "REPLACEME@email.com");
        put("BT", "REPLACEME@email.com");
        put("PTN", "REPLACEME@email.com");
        put("VJM", "REPLACEME@email.com");
        put("VSA", "REPLACEME@email.com");
        put("TSS", "REPLACEME@email.com");
        put("WAA", "REPLACEME@email.com");
    }};

    // Format data method
    private static String formatData(String[] inputtedData) {
        String formattedData = "";

        String[] labels = {
                "Patient's Consultant",
                "Clinic Date",
                "Location",
                "Name",
                "Hospital Number",
                "Date of Birth",
                "Laser Procedure",
                "Seen By",
                "Appointment Date",
                "Appointment Time",
                "Phone Number",
                "Eye(s)",
                "Priority"
        };

        for(int i = 0; i < inputtedData.length; i++) {
            formattedData += labels[i] + ": " + inputtedData[i] + "\n";
        }

        return formattedData;
    }

    // Take screenshot method
    private static BufferedImage takeScreenshot(JFrame window) throws AWTException {
        Robot robot = new Robot();
        Rectangle windowRect = window.getBounds();

        // Get screen size
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

        // Check if the window is maximized
        if (windowRect.width != screenSize.width || windowRect.height != screenSize.height) {
            // Trim sides if window not maximized (prevents other parts of the screen being captured)
            windowRect.setBounds(
                    windowRect.x + 7,  // Trim 7px from left
                    windowRect.y + 5,  // Trim 5px from top
                    windowRect.width - 14,  // Trim 7px from right and left
                    windowRect.height - 10  // Trim 5px from top and bottom
            );
        }

        return robot.createScreenCapture(windowRect);
    }

    // Send email function
    private static void sendEmail(String data, String consultantEmail, BufferedImage screenshot) {
        // Set properties for SMTP
        Properties props = new Properties();
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "465");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.socketFactory.port", "465");
        props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");

        // Authentication
        Session session = Session.getInstance(props, new javax.mail.Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(sender, password);
            }
        });

        try {
            // Create message
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(sender));

            // Sets consultant and KGH's waiting list email addresses as recipients
            String recipients = waitingListEmail + "," + consultantEmail;
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipients));

            message.setSubject("Patient for Laser Waiting List");

            MimeBodyPart textPart = new MimeBodyPart();
            textPart.setText(data + "\n Sent by Benjamin McGregor's WaitingList Application");

            // Attach screenshot
            MimeBodyPart screenshotPart = new MimeBodyPart();
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            ImageIO.write(screenshot, "png", byteArrayOutputStream);
            byte[] imageData = byteArrayOutputStream.toByteArray();
            DataSource dataSource = new ByteArrayDataSource(imageData, "image/png");
            screenshotPart.setDataHandler(new DataHandler(dataSource));
            screenshotPart.setFileName("image.png");

            // Combine text and screenshot
            Multipart multipart = new MimeMultipart();
            multipart.addBodyPart(textPart);
            multipart.addBodyPart(screenshotPart);
            message.setContent(multipart);

            // Send email
            Transport.send(message);
        } catch (MessagingException e) {
            System.err.println("Error sending email");
        } catch (IOException e) {
            System.err.println("Error attaching screenshot");
        }
    }

    // Centre frame function
    // sets frame position to centre of screen
    private static void centreFrame(Window frame) {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int x = (int) ((screenSize.getWidth() - frame.getWidth()) / 2);
        int y = (int) ((screenSize.getHeight() - frame.getHeight()) / 2);

        frame.setLocation(x, y);
    }

    // Check no missing data function
    // returns true if no data is missing
    // otherwise, displays an error message and returns false
    private static boolean checkNoMissingData(String[] data) {
        for (String item : data) {
            if (item.isEmpty()) {
                showMessageDialog(null, "Fields cannot be left empty.");
                return false;
            }
        }
        return true;
    }

    // Clear inputs function
    // resets dropdowns and clears text input
    // preserves consultant/clinic date/location
    private static void clearInputs(JPanel middlePanel) {
        for (Component comp : middlePanel.getComponents()) {
            if (comp instanceof JComboBox<?> comboBox) {
                comboBox.setSelectedIndex(0);
            } else if (comp instanceof JTextField textField) {
                textField.setText("");
            } else if (comp instanceof JPanel) {
                clearInputs((JPanel) comp);
            }
        }
    }

    // Submit function
    // Just prints the formatted details to the terminal at the moment
    // This is where IT will make it send the data to the database
    private static void submit(String consultant, String data, BufferedImage screenshot) {
        sendEmail(data, consultants.get(consultant), screenshot);

        showMessageDialog(null, "Patient added to the waiting list.");
    }

    // Check date formats function
    // verifies that inputted dates follow the dd/MM/yyyy format
    private static boolean checkDateFormats(String[] dates) {
        SimpleDateFormat correctFormat = new SimpleDateFormat("dd/MM/yyyy");
        correctFormat.setLenient(false);

        // check if it matches dd/MM/yyyy format
        for (String date : dates) {
            if (date.length() != 10) {
                showMessageDialog(null, "Date '" + date + "' is invalid.");
                return false;
            }

            try {
                correctFormat.parse(date);

            } catch (ParseException e) {
                showMessageDialog(null, "Date '" + date + "' is invalid.");
                return false;
            }
        }

        return true;
    }

    // Main method
    public static void main(String[] args) {

        // Frame setup
        JFrame frame = new JFrame("KGH Laser Waiting List");
        frame.setResizable(true);
        frame.setMinimumSize(new Dimension(900, 600));
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        Image icon = Toolkit.getDefaultToolkit().getImage("res/icon.png");
        frame.setIconImage(icon);
        centreFrame(frame);

        // Main panel
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout());

        // Top panel (consultant/clinic date/location)
        JPanel topPanel = new JPanel(new GridLayout(2, 3));

        JLabel consultantLabel = new JLabel("PATIENT'S CONSULTANT");
        consultantLabel.setHorizontalAlignment(SwingConstants.CENTER);
        JComboBox<String> consultantDropdown = new JComboBox<>(consultants.keySet().toArray(new String[0]));

        JLabel clinicDateLabel = new JLabel("CLINIC DATE");
        clinicDateLabel.setHorizontalAlignment(SwingConstants.CENTER);
        JTextField clinicDateInput = new JTextField(new SimpleDateFormat("dd/MM/yyyy").format(new Date()));

        JLabel locationLabel = new JLabel("LOCATION");
        locationLabel.setHorizontalAlignment(SwingConstants.CENTER);
        JComboBox<String> locationDropdown = new JComboBox<>(new String[]{"KGH", "NPU", "CDC", "IBH"});

        topPanel.add(consultantLabel);
        topPanel.add(clinicDateLabel);
        topPanel.add(locationLabel);
        topPanel.add(consultantDropdown);
        topPanel.add(clinicDateInput);
        topPanel.add(locationDropdown);

        // Middle panel (for all other details)
        JPanel middlePanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        JLabel nameLabel = new JLabel("Name");
        JTextField nameInput = new JTextField();
        JLabel hospitalNumberLabel = new JLabel("Hospital Number");
        JTextField hospitalNumberInput = new JTextField();
        JLabel DOBLabel = new JLabel("Date of Birth (dd/mm/yyyy)");
        JTextField DOBInput = new JTextField();

        JLabel eyesLabel = new JLabel("EYE(S)");
        JComboBox<String> eyesDropdown = new JComboBox<>(new String[]{"L", "R", "BL"});
        JLabel priorityLabel = new JLabel("PRIORITY");
        JComboBox<String> priorityDropdown = new JComboBox<>(new String[]{"<2/52", "URGENT", "ROUTINE"});
        JLabel laserProcedureLabel = new JLabel("LASER PROCEDURE");
        JTextField laserProcedureInput = new JTextField();
        JLabel seenByLabel = new JLabel("SEEN BY");
        JTextField seenByInput = new JTextField();
        JLabel appointmentDateLabel = new JLabel("APPT DATE (dd/mm/yyyy)");
        JTextField appointmentDateInput = new JTextField();

        // Appointment time selector
        JLabel input7Label = new JLabel("APPT TIME");

        // Hour dropdown (00 to 23)
        String[] hours = new String[24];
        for (int i = 0; i < 24; i++) {
            hours[i] = String.format("%02d", i);
        }
        JComboBox<String> hourDropdown = new JComboBox<>(hours);

        // Minute dropdown (00 to 55)
        String[] minutes = new String[12];
        for (int i = 0; i < 12; i++) {
            minutes[i] = String.format("%02d", i * 5);
        }
        JComboBox<String> minuteDropdown = new JComboBox<>(minutes);

        // Panel for hour and minute dropdowns
        JPanel timePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        timePanel.add(hourDropdown);
        timePanel.add(minuteDropdown);

        JLabel phoneNumberLabel = new JLabel("PHONE NUMBER");
        JTextField phoneNumberInput = new JTextField();

        // Submit button
        JButton submitButton = new JButton("Submit");
        submitButton.setFont(new Font("Arial", Font.BOLD, 36));
        submitButton.addActionListener(e -> {
            submitButton.setEnabled(false);

            // Collect all inputted data
            String[] inputtedData = {
                    (String) consultantDropdown.getSelectedItem(),
                    clinicDateInput.getText(),
                    (String) locationDropdown.getSelectedItem(),
                    nameInput.getText(),
                    hospitalNumberInput.getText(),
                    DOBInput.getText(),
                    laserProcedureInput.getText(),
                    seenByInput.getText(),
                    appointmentDateInput.getText(),
                    hourDropdown.getSelectedItem() + ":" + minuteDropdown.getSelectedItem(),
                    phoneNumberInput.getText(),
                    (String) eyesDropdown.getSelectedItem(),
                    (String) priorityDropdown.getSelectedItem()
            };

            // Check for empty fields
            if (checkNoMissingData(inputtedData)) {

                // Verify dates are correct format
                if (checkDateFormats(new String[]{clinicDateInput.getText(), DOBInput.getText(), appointmentDateInput.getText()})) {
                    String consultantInitials = (String) consultantDropdown.getSelectedItem();

                    // Take screenshot
                    BufferedImage screenshot;
                    try {
                        screenshot = takeScreenshot(frame);

                        //Submit and reset window
                        clearInputs(middlePanel);
                        submit(consultantInitials, formatData(inputtedData), screenshot);

                    } catch (AWTException ex) {
                        System.err.println("Error taking screenshot");
                    }
                }
            }
            submitButton.setEnabled(true);
        });

        // Add components to middle panel
        // Row 0
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.1;
        middlePanel.add(nameLabel, gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        middlePanel.add(nameInput, gbc);
        gbc.gridx = 0;
        gbc.gridy = 1;
        middlePanel.add(hospitalNumberLabel, gbc);
        gbc.gridx = 1;
        middlePanel.add(hospitalNumberInput, gbc);
        gbc.gridx = 0;
        gbc.gridy = 2;
        middlePanel.add(DOBLabel, gbc);
        gbc.gridx = 1;
        middlePanel.add(DOBInput, gbc);

        // Row 1
        gbc.gridx = 2;
        gbc.gridy = 0;
        middlePanel.add(eyesLabel, gbc);
        gbc.gridx = 3;
        middlePanel.add(eyesDropdown, gbc);
        gbc.gridx = 2;
        gbc.gridy = 1;
        middlePanel.add(priorityLabel, gbc);
        gbc.gridx = 3;
        middlePanel.add(priorityDropdown, gbc);

        // Row 2
        gbc.gridx = 0;
        gbc.gridy = 3;
        middlePanel.add(laserProcedureLabel, gbc);
        gbc.gridx = 1;
        middlePanel.add(laserProcedureInput, gbc);
        gbc.gridx = 0;
        gbc.gridy = 4;
        middlePanel.add(seenByLabel, gbc);
        gbc.gridx = 1;
        middlePanel.add(seenByInput, gbc);
        gbc.gridx = 0;
        gbc.gridy = 5;
        middlePanel.add(appointmentDateLabel, gbc);
        gbc.gridx = 1;
        middlePanel.add(appointmentDateInput, gbc);
        gbc.gridx = 0;
        gbc.gridy = 6;
        middlePanel.add(input7Label, gbc);
        gbc.gridx = 1;
        gbc.gridy = 6;
        middlePanel.add(timePanel, gbc);
        gbc.gridx = 0;
        gbc.gridy = 7;
        middlePanel.add(phoneNumberLabel, gbc);
        gbc.gridx = 1;
        middlePanel.add(phoneNumberInput, gbc);

        // Add panels to the main panel
        mainPanel.add(topPanel, BorderLayout.NORTH);
        mainPanel.add(middlePanel, BorderLayout.CENTER);

        // Create and place panel for submit button
        JPanel submitPanel = new JPanel();
        submitPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 0, 40));
        submitPanel.add(submitButton);
        mainPanel.add(submitPanel, BorderLayout.SOUTH);

        frame.add(mainPanel);
        frame.setVisible(true);
    }
}
