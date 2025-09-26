package pages;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.labels.PieSectionLabelGenerator;
import org.jfree.chart.plot.PiePlot;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.data.general.PieDataset;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.text.AttributedString;
import javax.swing.JPanel;





public class Page1 extends JPanel {
    private final DefaultPieDataset dataset = new DefaultPieDataset();
    private final JTextField monthlyBudget = new JTextField(10);
    private final JTextField weeklyFoodBudget = new JTextField(10);
    private final JButton percentButton = new JButton("Percent");
    private final JButton amountButton = new JButton("Amount");
    private boolean isPercentMode = true;


    //==================================================================================================================

    public Page1() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Inputs Panel

        JPanel inputs = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 10));

        inputs.add(new JLabel("Monthly Budget: $"));
        monthlyBudget.setText("2000");
        inputs.add(monthlyBudget);

        inputs.add(new JLabel("Weekly Food Budget:"));
        weeklyFoodBudget.setText("10");  // default 10% for percent mode
        inputs.add(weeklyFoodBudget);

        inputs.add(percentButton);
        inputs.add(amountButton);

        add(inputs, BorderLayout.NORTH);


        //==============================================================================================================
        // Create chart

        JFreeChart chart = ChartFactory.createPieChart(
                "Monthly Budget Breakdown (Weekly x4)",
                dataset,
                true, true, false);

        PiePlot plot = (PiePlot) chart.getPlot();

        //==============================================================================================================
        // amount and percent showing and switching

        plot.setLabelGenerator(new PieSectionLabelGenerator() {
            @Override
            public String generateSectionLabel(PieDataset dataset, Comparable key) {
                Number value = dataset.getValue(key);
                if (value == null) return null;

                double total = 0.0;
                for (Object x : dataset.getKeys()) {
                    Number y = dataset.getValue((Comparable) x);
                    if (y != null) total += y.doubleValue();
                }
                double percent = total > 0 ? value.doubleValue() / total * 100 : 0;

                if (isPercentMode) {
                    // Show: percent (amount)
                    return String.format("%s: %.1f%% ($%.2f)", key, percent, value.doubleValue());
                } else {
                    // Show: amount (percent)
                    return String.format("%s: $%.2f (%.1f%%)", key, value.doubleValue(), percent);
                }
            }


            @Override
            public AttributedString generateAttributedSectionLabel(PieDataset dataset, Comparable key) {
                return null;
            }
        });

    //==================================================================================================================
        // colors

        plot.setSectionPaint(0, new Color(255, 180, 180)); // Food
        plot.setSectionPaint(1, new Color(180, 180, 255)); // Other

        ChartPanel chartPanel = new ChartPanel(chart);
        add(chartPanel, BorderLayout.CENTER);

        //==============================================================================================================
        // Highlight buttons based on mode

        updateButtonHighlight();


        //==============================================================================================================
        // Percent button logic

        percentButton.addActionListener(e -> {
            if (!isPercentMode) {
                double monthlyBudget = parseInput(this.monthlyBudget);
                double weeklyAmount = parseInput(weeklyFoodBudget);
                double monthlyFood = weeklyAmount * 4;
                double percent = monthlyBudget > 0 ? (monthlyFood / monthlyBudget) * 100 : 0;

                weeklyFoodBudget.setText(String.format("%.2f", percent));

                isPercentMode = true;
                updateButtonHighlight();
                updateChart();
                chart.fireChartChanged();
            }
        });

        //==============================================================================================================
        // Amount button logic

        amountButton.addActionListener(e -> {
            if (isPercentMode) {
                double monthlyBudget = parseInput(this.monthlyBudget);
                double percent = parseInput(weeklyFoodBudget);
                double monthlyFood = (percent / 100.0) * monthlyBudget;
                double weeklyAmount = monthlyFood / 4;

                weeklyFoodBudget.setText(String.format("%.2f", weeklyAmount));

                isPercentMode = false;
                updateButtonHighlight();
                updateChart();
                chart.fireChartChanged();
            }
        });

        //==============================================================================================================
        // CHART UPDATER - Part 1

        DocumentListener inputListener = new DocumentListener() {
            public void changedUpdate(DocumentEvent e) { updateChart(); chart.fireChartChanged(); }
            public void removeUpdate(DocumentEvent e) { updateChart(); chart.fireChartChanged(); }
            public void insertUpdate(DocumentEvent e) { updateChart(); chart.fireChartChanged(); }
        };
        monthlyBudget.getDocument().addDocumentListener(inputListener);
        weeklyFoodBudget.getDocument().addDocumentListener(inputListener);

        // Initialize chart
        updateChart();
    }


    //==================================================================================================================
    // button background - shows what is selected
    private void updateButtonHighlight() {
        if (isPercentMode) {
            percentButton.setBackground(new Color(173, 216, 230)); // light blue
            percentButton.setOpaque(true);
            percentButton.setBorder(BorderFactory.createLineBorder(Color.BLUE, 2));

            amountButton.setBackground(null);
            amountButton.setOpaque(false);
            amountButton.setBorder(UIManager.getBorder("Button.border"));
        } else {
            amountButton.setBackground(new Color(173, 216, 230)); // light blue
            amountButton.setOpaque(true);
            amountButton.setBorder(BorderFactory.createLineBorder(Color.BLUE, 2));

            percentButton.setBackground(null);
            percentButton.setOpaque(false);
            percentButton.setBorder(UIManager.getBorder("Button.border"));
        }
    }

    //==================================================================================================================
    // parse double from text field

    private double parseInput(JTextField field) {
        try {
            return Double.parseDouble(field.getText());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    //==================================================================================================================
    // CHART UPDATER - Part 2
    private void updateChart() {
        double monthlyBudget = parseInput(this.monthlyBudget);
        double weeklyInput = parseInput(weeklyFoodBudget);

        double foodMonthlyAmount;
        if (isPercentMode) {
            foodMonthlyAmount = monthlyBudget * (weeklyInput / 100.0);
        } else {
            foodMonthlyAmount = weeklyInput * 4;
        }

        double other = monthlyBudget - foodMonthlyAmount;
        if (other < 0) other = 0;

        dataset.setValue("Food", foodMonthlyAmount);
        dataset.setValue("Other", other);
    }

    //==================================================================================================================


}
