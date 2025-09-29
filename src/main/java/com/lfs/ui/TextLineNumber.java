package com.lfs.ui;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashMap;

public class TextLineNumber extends JComponent implements DocumentListener, CaretListener, PropertyChangeListener {
    private final static float RIGHT = 1.0f;

    private final static int HEIGHT = Integer.MAX_VALUE - 1000000;

    //  此 TextLineNumber 组件与之同步的文本组件
    private JTextComponent component;

    //  可更改的属性
    private boolean updateFont;
    private int borderGap;
    private Color currentLineForeground;
    private float digitAlignment;
    private int minimumDisplayDigits;

    //  保留历史信息以减少组件的更新次数
    private int lastDigits;
    private int lastHeight;
    private int lastLine;

    private HashMap<String, FontMetrics> fonts;

    /**
     * 为文本组件创建一个行号组件
     * 这个组件将作为包含文本组件的JScrollPane中的一列显示。
     *
     *  @param component  相关文本组件
     */
    public TextLineNumber(JTextComponent component)
    {
        this(component, 3);
    }

    /**
     *	为文本组件创建行号组件。
     *  此组件将在包含文本组件的JScrollPane中显示为一列。
     *
     *  @param component  相关文本组件
     *  @param minimumDisplayDigits  用于计算的数字位数
     *                               组件的优选宽度。
     */
    public TextLineNumber(JTextComponent component, int minimumDisplayDigits)
    {
        this.component = component;

        setFont( component.getFont() );

        setBorderGap( 5 );
        setCurrentLineForeground(new Color(255, 100, 0));
        setDigitAlignment( RIGHT );
        setMinimumDisplayDigits( minimumDisplayDigits );

        component.getDocument().addDocumentListener(this);
        component.addCaretListener( this );
        component.addPropertyChangeListener( "font", this);
    }

    /**
     *  获取 updateFont 属性
     *
     *  @return updateFont 属性
     */
    public boolean getUpdateFont()
    {
        return updateFont;
    }

    /**
     *  设置 updateFont 属性。指示此组件是否应
     *  在相关文本组件的字体更改时更新其字体。
     *
     *  @param updateFont  为 true 时更新字体
     */
    public void setUpdateFont(boolean updateFont)
    {
        this.updateFont = updateFont;
    }

    /**
     *  获取边框间隙
     *
     *  @return 边框间隙（以像素为单位）
     */
    public int getBorderGap()
    {
        return borderGap;
    }

    /**
     *  边框间隙用于计算
     *  边框的左右内边距。默认值为 5。
     *
     *  @param borderGap  间隙（以像素为单位）
     */
    public void setBorderGap(int borderGap)
    {
        this.borderGap = borderGap;
        Border inner = new EmptyBorder(0, borderGap, 0, borderGap);
        setBorder( new CompoundBorder(new MatteBorder(0, 0, 0, 1, Color.LIGHT_GRAY), inner) );
        lastDigits = 0;
        setPreferredWidth();
    }

    /**
     *  获取当前行渲染颜色
     *
     *  @return 用于渲染当前行号的颜色
     */
    public Color getCurrentLineForeground()
    {
        return currentLineForeground == null ? getForeground() : currentLineForeground;
    }

    /**
     *  用于渲染当前行号的颜色。默认值为 Color.RED。
     *
     *  @param currentLineForeground  用于渲染当前行的颜色
     */
    public void setCurrentLineForeground(Color currentLineForeground)
    {
        this.currentLineForeground = currentLineForeground;
    }

    /**
     *  获取数字对齐方式
     *
     *  @return 绘制数字的对齐方式
     */
    public float getDigitAlignment()
    {
        return digitAlignment;
    }

    /**
     *  指定组件内数字的水平对齐方式。
     *  常见值为：
     *  <ul>
     *  <li>TextLineNumber.LEFT
     *  <li>TextLineNumber.CENTER
     *  <li>TextLineNumber.RIGHT (默认)
     *	</ul>
     *  @param digitAlignment  数字的对齐方式
     */
    public void setDigitAlignment(float digitAlignment)
    {
        this.digitAlignment = digitAlignment > 1.0f ? 1.0f : digitAlignment < 0.0f ? -1.0f : digitAlignment;
    }

    /**
     *  获取最小显示位数
     *
     *  @return 最小显示位数
     */
    public int getMinimumDisplayDigits()
    {
        return minimumDisplayDigits;
    }

    /**
     *  指定用于计算组件首选宽度的
     *  最小位数。默认值为 3。
     *
     *  @param minimumDisplayDigits  位数
     */
    public void setMinimumDisplayDigits(int minimumDisplayDigits)
    {
        this.minimumDisplayDigits = minimumDisplayDigits;
        setPreferredWidth();
    }

    /**
     *  计算显示最大行号所需的宽度
     */
    private void setPreferredWidth()
    {
        Element root = component.getDocument().getDefaultRootElement();
        int lines = root.getElementCount();
        int digits = Math.max(String.valueOf(lines).length(), minimumDisplayDigits);

        //  当行号中的位数发生变化时更新大小

        if (lastDigits != digits)
        {
            lastDigits = digits;
            FontMetrics fontMetrics = getFontMetrics( getFont() );
            int width = fontMetrics.charWidth( '0' ) * digits;
            Insets insets = getInsets();
            int preferredWidth = insets.left + insets.right + width;

            Dimension d = getPreferredSize();
            d.width = preferredWidth;
            setPreferredSize( d );
            setSize( d );
        }
    }

    /**
     *  绘制行号
     */
    @Override
    public void paintComponent(Graphics g)
    {
        super.paintComponent(g);

        //	确定可用于绘制数字的空间宽度

        FontMetrics fontMetrics = component.getFontMetrics( component.getFont() );
        Insets insets = getInsets();
        int availableWidth = getSize().width - insets.left - insets.right;

        //  确定在裁剪边界内要绘制的行。

        Rectangle clip = g.getClipBounds();
        int rowStartOffset = component.viewToModel( new Point(0, clip.y) );
        int endOffset = component.viewToModel( new Point(0, clip.y + clip.height) );

        while (rowStartOffset <= endOffset)
        {
            try
            {
                if (isCurrentLine(rowStartOffset))
                    g.setColor( getCurrentLineForeground() );
                else
                    g.setColor( getForeground() );

                //  获取行号作为字符串，然后确定
                //  绘制字符串的位置。

                String lineNumber = getTextLineNumber(rowStartOffset);
                int stringWidth = fontMetrics.stringWidth( lineNumber );
                int x = getOffsetX(availableWidth, stringWidth) + insets.left;
                int y = getOffsetY(rowStartOffset, fontMetrics);
                g.drawString(lineNumber, x, y);

                //  移动到下一行

                rowStartOffset = Utilities.getRowEnd(component, rowStartOffset) + 1;
            }
            catch(Exception e) {break;}
        }
    }

    /*
     *  我们需要知道光标当前是否位于我们
     *  要绘制的行上，以便可以突出显示行号。
     */
    private boolean isCurrentLine(int rowStartOffset)
    {
        int caretPosition = component.getCaretPosition();
        Element root = component.getDocument().getDefaultRootElement();
        int currentLine = root.getElementIndex( caretPosition );
        int row = root.getElementIndex( rowStartOffset );

        return currentLine == row;
    }

    /**
     *	获取要绘制的行号。保证行号
     *  用前导零格式化。
     */
    protected String getTextLineNumber(int rowStartOffset)
    {
        Element root = component.getDocument().getDefaultRootElement();
        int index = root.getElementIndex( rowStartOffset );
        Element line = root.getElement( index );

        if (line.getStartOffset() == rowStartOffset)
            return String.valueOf(index + 1);
        else
            return "";
    }

    /*
     *  确定 X 偏移量以在绘制时正确对齐行号
     */
    private int getOffsetX(int availableWidth, int stringWidth)
    {
        return (int)((availableWidth - stringWidth) * digitAlignment);
    }

    /*
     *  确定当前行的 Y 偏移量
     */
    private int getOffsetY(int rowStartOffset, FontMetrics fontMetrics) throws BadLocationException
    {
        //  获取行的边界矩形

        Rectangle r = component.modelToView( rowStartOffset );
        int lineHeight = fontMetrics.getHeight();
        int y = r.y + r.height;
        int descent = 0;

        //  文本需要根据字体的下降高度
        //  定位在边界矩形的底部之上。

        if (r.height == lineHeight)  // 正在使用默认字体
        {
            descent = fontMetrics.getDescent();
        }
        else  // 我们需要检查所有属性以查找字体更改
        {
            if (fonts == null)
                fonts = new HashMap<String, FontMetrics>();

            Element root = component.getDocument().getDefaultRootElement();
            int index = root.getElementIndex( rowStartOffset );
            Element line = root.getElement( index );

            for (int i = 0; i < line.getElementCount(); i++)
            {
                Element child = line.getElement(i);
                AttributeSet as = child.getAttributes();
                String fontFamily = (String)as.getAttribute(StyleConstants.FontFamily);
                Integer fontSize = (Integer)as.getAttribute(StyleConstants.FontSize);
                String key = fontFamily + fontSize;

                FontMetrics fm = fonts.get( key );

                if (fm == null)
                {
                    Font font = new Font(fontFamily, Font.PLAIN, fontSize);
                    fm = component.getFontMetrics( font );
                    fonts.put(key, fm);
                }

                descent = Math.max(descent, fm.getDescent());
            }
        }

        return y - descent;
    }

    //
//  实现 DocumentListener 接口
//
    @Override
    public void insertUpdate(DocumentEvent e)
    {
        SwingUtilities.invokeLater(() -> {
            int caretPosition = component.getCaretPosition();
            Element root = component.getDocument().getDefaultRootElement();
            int currentLine = root.getElementIndex( caretPosition );

            if (lastLine != currentLine) {
                getParent().repaint();
                lastLine = currentLine;
            }
        });
        documentChanged();
    }

    @Override
    public void removeUpdate(DocumentEvent e)
    {
        documentChanged();
    }

    @Override
    public void changedUpdate(DocumentEvent e)
    {
        documentChanged();
    }

    /*
     *  文档更改可能会影响显示的文本行数。
     *  因此，行号也会更改。
     */
    private void documentChanged()
    {
        //  在触发 DocumentEvent 时，
        //  组件的视图尚未更新

        SwingUtilities.invokeLater( () ->
        {
            int preferredHeight = component.getPreferredSize().height;

            if (lastHeight != preferredHeight)
            {
                setPreferredWidth();
                setPreferredSize(new Dimension(getPreferredSize().width, preferredHeight));
                getParent().revalidate();
                getParent().repaint();
                lastHeight = preferredHeight;
            }
        });
    }

    //
//  实现 CaretListener 接口
//
    @Override
    public void caretUpdate(CaretEvent e)
    {
        //  获取光标所在行

        int caretPosition = component.getCaretPosition();
        Element root = component.getDocument().getDefaultRootElement();
        int currentLine = root.getElementIndex( caretPosition );

        //  需要重绘，以便可以突出显示正确的行号
        //  当然，行号可能已经移动

        if (lastLine != currentLine)
        {
            getParent().repaint();
            lastLine = currentLine;
        }
    }

    //
//  实现 PropertyChangeListener 接口
//
    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        if (evt.getNewValue() instanceof Font)
        {
            if (updateFont)
            {
                Font newFont = (Font) evt.getNewValue();
                setFont(newFont);
                lastDigits = 0;
                setPreferredWidth();
            }
            else
            {
                getParent().repaint();
            }
        }
    }
}