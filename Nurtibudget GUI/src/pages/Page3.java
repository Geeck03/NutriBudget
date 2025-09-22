package pages;

import javax.swing.*;
import java.awt.*;

public class Page3 extends JPanel {
    public Page3() {
        setLayout(new BorderLayout());
        add(new JLabel("Welcome to Page 3", SwingConstants.CENTER), BorderLayout.CENTER);
    }
}
