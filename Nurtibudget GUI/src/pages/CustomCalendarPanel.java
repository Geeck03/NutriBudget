package pages;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class CustomCalendarPanel extends JPanel {
    private final JLabel monthYearLabel;
    private final JPanel calendarGrid;
    private final JTextArea eventTextArea;
    private final JButton saveButton;

    private final Map<String, String> eventMap = new HashMap<>();
    private final Calendar calendar = Calendar.getInstance();
    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

    public CustomCalendarPanel() {
        setLayout(new BorderLayout());

        // Top panel with prev, month-year, next
        JPanel topPanel = new JPanel(new BorderLayout());
        JButton prevButton = new JButton("<");
        JButton nextButton = new JButton(">");

        monthYearLabel = new JLabel("", SwingConstants.CENTER);
        monthYearLabel.setFont(new Font("Arial", Font.BOLD, 18));

        topPanel.add(prevButton, BorderLayout.WEST);
        topPanel.add(monthYearLabel, BorderLayout.CENTER);
        topPanel.add(nextButton, BorderLayout.EAST);

        add(topPanel, BorderLayout.NORTH);

        // Calendar grid
        calendarGrid = new JPanel(new GridLayout(7, 7)); // 7x7 for weekday headers + days
        add(calendarGrid, BorderLayout.CENTER);

        // Bottom panel for event details
        JPanel bottomPanel = new JPanel(new BorderLayout());
        eventTextArea = new JTextArea(3, 40);
        eventTextArea.setLineWrap(true);
        eventTextArea.setWrapStyleWord(true);
        saveButton = new JButton("Save Event");

        bottomPanel.add(new JLabel("Event for selected date:"), BorderLayout.NORTH);
        bottomPanel.add(new JScrollPane(eventTextArea), BorderLayout.CENTER);
        bottomPanel.add(saveButton, BorderLayout.EAST);

        add(bottomPanel, BorderLayout.SOUTH);

        // Actions
        prevButton.addActionListener(e -> {
            calendar.add(Calendar.MONTH, -1);
            updateCalendar();
        });

        nextButton.addActionListener(e -> {
            calendar.add(Calendar.MONTH, 1);
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
            updateCalendar();
        });

        updateCalendar();
    }

    private void updateCalendar() {
        calendarGrid.removeAll();
        calendarGrid.revalidate();
        calendarGrid.repaint();

        // Set month label
        SimpleDateFormat labelFormat = new SimpleDateFormat("MMMM yyyy");
        monthYearLabel.setText(labelFormat.format(calendar.getTime()));

        // Weekday headers
        String[] headers = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
        for (String day : headers) {
            JLabel lbl = new JLabel(day, SwingConstants.CENTER);
            lbl.setFont(lbl.getFont().deriveFont(Font.BOLD));
            calendarGrid.add(lbl);
        }

        // Setup date calculations
        Calendar temp = (Calendar) calendar.clone();
        temp.set(Calendar.DAY_OF_MONTH, 1);
        int firstDay = temp.get(Calendar.DAY_OF_WEEK) - 1;
        int daysInMonth = temp.getActualMaximum(Calendar.DAY_OF_MONTH);

        // Fill leading empty cells
        for (int i = 0; i < firstDay; i++) {
            calendarGrid.add(new JLabel(""));
        }

        // Fill actual days
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
    }
}
