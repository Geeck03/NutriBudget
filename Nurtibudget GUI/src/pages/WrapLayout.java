package pages;

import java.awt.*;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;


public class WrapLayout extends FlowLayout {

    //==================================================================================================================
    // Constructors
    //==================================================================================================================

    public WrapLayout() {
        super();
    }

    public WrapLayout(int align) {
        super(align);
    }

    public WrapLayout(int align, int hgap, int vgap) {
        super(align, hgap, vgap);
    }

    //==================================================================================================================
    // Layout methods
    //==================================================================================================================

    @Override
    public Dimension preferredLayoutSize(Container target) {
        return layoutSize(target, true);
    }

    @Override
    public Dimension minimumLayoutSize(Container target) {
        Dimension minimum = layoutSize(target, false);
        minimum.width -= (getHgap() + 1);
        return minimum;
    }

    //==================================================================================================================
    // Core calculation
    //==================================================================================================================

    private Dimension layoutSize(Container target, boolean preferred) {
        synchronized (target.getTreeLock()) {
            int targetWidth = target.getWidth();

            // If the width is 0, assume it's inside a JScrollPane
            if (targetWidth == 0 && target.getParent() instanceof JScrollPane) {
                targetWidth = ((JScrollPane) target.getParent()).getViewport().getWidth();
            }

            Insets insets = target.getInsets();
            int hgap = getHgap();
            int vgap = getVgap();
            int maxWidth = targetWidth - (insets.left + insets.right + hgap * 2);

            Dimension dim = new Dimension(0, 0);
            int rowWidth = 0;
            int rowHeight = 0;

            int nmembers = target.getComponentCount();

            for (int i = 0; i < nmembers; i++) {
                Component m = target.getComponent(i);

                if (m.isVisible()) {
                    Dimension d = preferred ? m.getPreferredSize() : m.getMinimumSize();

                    // Wrap to next row if necessary
                    if (rowWidth + d.width > maxWidth && rowWidth != 0) {
                        addRow(dim, rowWidth, rowHeight);
                        rowWidth = 0;
                        rowHeight = 0;
                    }

                    if (rowWidth != 0) rowWidth += hgap;

                    rowWidth += d.width;
                    rowHeight = Math.max(rowHeight, d.height);
                }
            }

            // Add the last row
            addRow(dim, rowWidth, rowHeight);

            dim.width += insets.left + insets.right + hgap * 2;
            dim.height += insets.top + insets.bottom + vgap * 2;

            // Adjust width slightly for scrollable containers
            Container scrollPane = SwingUtilities.getAncestorOfClass(JScrollPane.class, target);
            if (scrollPane != null) {
                dim.width -= (hgap + 1);
            }

            return dim;
        }
    }

    //==================================================================================================================
    // Helper
    //==================================================================================================================

    private void addRow(Dimension dim, int rowWidth, int rowHeight) {
        dim.width = Math.max(dim.width, rowWidth);
        if (dim.height > 0) {
            dim.height += getVgap();
        }
        dim.height += rowHeight;
    }
}
