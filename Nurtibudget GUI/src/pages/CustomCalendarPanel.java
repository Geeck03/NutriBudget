package pages;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

//==============================================================================================================
// CustomCalendarPanel Class
//==============================================================================================================
public class CustomCalendarPanel extends JPanel {

    //==============================================================================================================
    // Fields and constants
    //==============================================================================================================
    private final JLabel monthYearLabel;
    private final JPanel calendarGrid;
    private final JTextArea eventTextArea;
    private final JButton saveButton;
    private final Map<String, String> eventMap = new HashMap<>();
    private final Calendar calendar = Calendar.getInstance();
    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
    private static final File EVENT_FILE = new File("events.txt");
    private final JComboBox<String> monthBox;
    private final JSpinner yearSpinner;

    //==============================================================================================================
    // Constructor
    //==============================================================================================================
    public CustomCalendarPanel() {
        setLayout(new BorderLayout());

        //==========================================================================================================
        // Top panel with navigation and selectors
        //==========================================================================================================
        JPanel topPanel = new JPanel(new BorderLayout());
        JButton prevButton = new JButton("<");
        JButton nextButton = new JButton(">");
        monthYearLabel = new JLabel("", SwingConstants.CENTER);
        monthYearLabel.setFont(new Font("Arial", Font.BOLD, 18));

        JPanel selectorPanel = new JPanel();
        String[] months = new java.text.DateFormatSymbols().getMonths();
        monthBox = new JComboBox<>();
        for (int i = 0; i < 12; i++) {
            monthBox.addItem(months[i]);
        }

        yearSpinner = new JSpinner(new SpinnerNumberModel(calendar.get(Calendar.YEAR), 1900, 3000, 1));
        JSpinner.NumberEditor editor = new JSpinner.NumberEditor(yearSpinner, "#");
        yearSpinner.setEditor(editor);

        selectorPanel.add(new JLabel("Month:"));
        selectorPanel.add(monthBox);
        selectorPanel.add(new JLabel("Year:"));
        selectorPanel.add(yearSpinner);

        topPanel.add(prevButton, BorderLayout.WEST);
        topPanel.add(monthYearLabel, BorderLayout.CENTER);
        topPanel.add(nextButton, BorderLayout.EAST);
        topPanel.add(selectorPanel, BorderLayout.SOUTH);
        add(topPanel, BorderLayout.NORTH);

        //==========================================================================================================
        // Calendar grid
        //==========================================================================================================
        calendarGrid = new JPanel(new GridLayout(0, 7));
        add(calendarGrid, BorderLayout.CENTER);

        //==========================================================================================================
        // Bottom panel for event details
        //==========================================================================================================
        JPanel bottomPanel = new JPanel(new BorderLayout());
        eventTextArea = new JTextArea(3, 40);
        eventTextArea.setLineWrap(true);
        eventTextArea.setWrapStyleWord(true);
        saveButton = new JButton("Save Event");

        bottomPanel.add(new JLabel("Event for selected date:"), BorderLayout.NORTH);
        bottomPanel.add(new JScrollPane(eventTextArea), BorderLayout.CENTER);
        bottomPanel.add(saveButton, BorderLayout.EAST);
        add(bottomPanel, BorderLayout.SOUTH);

        //==========================================================================================================
        // Actions
        //==========================================================================================================
        prevButton.addActionListener(e -> {
            calendar.add(Calendar.MONTH, -1);
            syncSelectors();
            updateCalendar();
        });

        nextButton.addActionListener(e -> {
            calendar.add(Calendar.MONTH, 1);
            syncSelectors();
            updateCalendar();
        });

        saveButton.addActionListener(e -> {
            Date date = calendar.getTime();
            String key = sdf.format(date);
            String event = eventTextArea.getText().trim();
            if (!event.isEmpty()) {
                eventMap.put(key, event);
            } else {
                eventMap.remove(key);
            }
            saveEventsToFile();
            updateCalendar();
        });

        monthBox.addActionListener(e -> {
            calendar.set(Calendar.MONTH, monthBox.getSelectedIndex());
            updateCalendar();
        });

        yearSpinner.addChangeListener(e -> {
            calendar.set(Calendar.YEAR, (int) yearSpinner.getValue());
            updateCalendar();
        });

        //==========================================================================================================
        // Initialization
        //==========================================================================================================
        loadEventsFromFile();
        syncSelectors();
        updateCalendar();
    }

    //==============================================================================================================
    // Calendar update
    //==============================================================================================================
    private void updateCalendar() {
        calendarGrid.removeAll();

        // Month label
        SimpleDateFormat labelFormat = new SimpleDateFormat("MMMM yyyy");
        monthYearLabel.setText(labelFormat.format(calendar.getTime()));

        // Weekday headers
        String[] headers = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
        for (String day : headers) {
            JLabel lbl = new JLabel(day, SwingConstants.CENTER);
            lbl.setFont(lbl.getFont().deriveFont(Font.BOLD));
            calendarGrid.add(lbl);
        }

        // Date calculations
        Calendar temp = (Calendar) calendar.clone();
        temp.set(Calendar.DAY_OF_MONTH, 1);
        int firstDay = temp.get(Calendar.DAY_OF_WEEK) - 1;
        int daysInMonth = temp.getActualMaximum(Calendar.DAY_OF_MONTH);

        // Leading empty cells
        for (int i = 0; i < firstDay; i++) {
            calendarGrid.add(new JLabel(""));
        }

        // Actual days
        for (int day = 1; day <= daysInMonth; day++) {
            int currentDay = day;
            JButton dayButton = new JButton(String.valueOf(day));

            temp.set(Calendar.DAY_OF_MONTH, currentDay);
            String key = sdf.format(temp.getTime());

            if (eventMap.containsKey(key)) {
                dayButton.setForeground(Color.RED);
                dayButton.setText(day + " •");
            }

            dayButton.addActionListener(e -> {
                calendar.set(Calendar.DAY_OF_MONTH, currentDay);
                String selectedKey = sdf.format(calendar.getTime());
                eventTextArea.setText(eventMap.getOrDefault(selectedKey, ""));
            });

            calendarGrid.add(dayButton);
        }

        calendarGrid.revalidate();
        calendarGrid.repaint();
    }

    //==============================================================================================================
    // Sync dropdowns and year spinner
    //==============================================================================================================
    private void syncSelectors() {
        monthBox.setSelectedIndex(calendar.get(Calendar.MONTH));
        yearSpinner.setValue(calendar.get(Calendar.YEAR));
    }

    //==============================================================================================================
    // Save events to file
    //==============================================================================================================
    private void saveEventsToFile() {
        try (PrintWriter out = new PrintWriter(new FileWriter(EVENT_FILE))) {
            for (Map.Entry<String, String> entry : eventMap.entrySet()) {
                String safeValue = entry.getValue().replace("\n", "\\n");
                out.println(entry.getKey() + "|" + safeValue);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //==============================================================================================================
    // Load events from file
    //==============================================================================================================
    private void loadEventsFromFile() {
        if (!EVENT_FILE.exists()) return;

        try (BufferedReader reader = new BufferedReader(new FileReader(EVENT_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\\|", 2);
                if (parts.length == 2) {
                    String restoredValue = parts[1].replace("\\n", "\n");
                    eventMap.put(parts[0], restoredValue);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
