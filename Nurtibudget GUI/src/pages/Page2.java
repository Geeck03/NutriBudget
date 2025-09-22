package pages;

import javax.swing.*;
import java.awt.*;

public class Page2 extends JPanel {
    public Page2() {
        setLayout(new BorderLayout());
        add(new JLabel("Welcome to Page 2", SwingConstants.CENTER), BorderLayout.CENTER);
    }
}
