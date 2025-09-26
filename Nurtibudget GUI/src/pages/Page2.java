package pages;

import java.awt.*;
import javax.swing.*;

public class Page2 extends JPanel {
    public Page2() {
        setLayout(new BorderLayout());
        add(new JLabel("Welcome to Page 2", SwingConstants.CENTER), BorderLayout.CENTER);
    }
}
