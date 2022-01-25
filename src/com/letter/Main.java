package com.letter;

import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import java.awt.*;
import javax.swing.*;
import java.awt.image.*;

import java.io.*;
import javax.imageio.*;
import javax.swing.filechooser.FileNameExtensionFilter;

public class Main {

    public static void main(String[] args) {
	    new PaintProgram();
    }
}
class PaintProgram extends JPanel implements MouseMotionListener, MouseListener, KeyListener{
    JFrame frame;
    ArrayList<Point> points;
    Stack<Object> shapes, undoRedo;
    Color currentColor, currentBackground, oldColor;
    Point p;
    Shape currShape;
    int currX, currY;

    JButton bLine, bRec, bOval, selectedShape, bUndo, bRedo, bErase;
    JMenuBar menuBar;
    JScrollBar size;
    JMenu colorMenu, backgroundMenu, fileMenu;
    JMenuItem save, load, clear, exit;
    Color[] colors;
    JColorChooser colorChooser1, colorChooser2;

    JFileChooser fileChooser;
    BufferedImage loadedImage;
    public PaintProgram(){
        frame = new JFrame("Coolest Paint Program");
        frame.add(this);

        menuBar = new JMenuBar();
        colorMenu = new JMenu("Colors");
        backgroundMenu = new JMenu("Background");
        size = new JScrollBar(JScrollBar.HORIZONTAL,1,0,1,15);
        size.setFocusable(false);

        colors = new Color[]{Color.RED,Color.ORANGE,Color.YELLOW,Color.GREEN,Color.BLUE,Color.CYAN,Color.MAGENTA};
        for(Color c : colors){
            JMenuItem b = new JMenuItem();
            b.setBackground(c);
            b.setPreferredSize(new Dimension(50,30));
            b.setFocusable(false);
            b.addActionListener(e -> {
                currentColor = c;
                checkErase();
            });
            colorMenu.add(b);

            b = new JMenuItem();
            b.setBackground(c);
            b.setPreferredSize(new Dimension(50,30));
            b.setFocusable(false);
            b.addActionListener(e -> {
                currentBackground = c;
                loadedImage = null;
                checkErase();
                repaint();
            });
            backgroundMenu.add(b);
        }
        currentColor = colors[0];
        currentBackground = Color.WHITE;

        colorChooser1 = new JColorChooser();
        colorChooser1.getSelectionModel().addChangeListener(e -> {
            currentColor = colorChooser1.getColor();
            checkErase();
        });
        colorMenu.add(colorChooser1);

        colorChooser2 = new JColorChooser();
        colorChooser2.getSelectionModel().addChangeListener(e -> {
            currentBackground = colorChooser2.getColor();
            loadedImage = null;
            checkErase();
            repaint();
        });
        backgroundMenu.add(colorChooser2);

        fileMenu = new JMenu("File");
        save = new JMenuItem("Save");
        save.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, ActionEvent.CTRL_MASK));
        save.setIcon(getScaledImage("save"));
        load = new JMenuItem("Load");
        load.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_L, ActionEvent.CTRL_MASK));
        load.setIcon(getScaledImage("load"));
        clear = new JMenuItem("New");
        clear.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, ActionEvent.CTRL_MASK));
        exit = new JMenuItem("Exit");

        fileChooser = new JFileChooser(System.getProperty("user.dir"));
        fileChooser.setFileFilter(new FileNameExtensionFilter(".png","png"));

        save.addActionListener(e -> {
            if(fileChooser.showSaveDialog(null)==JFileChooser.APPROVE_OPTION){
                File file = fileChooser.getSelectedFile();
                try{
                    String st = file.getAbsolutePath();
                    if(st.contains(".png"))
                        st = st.substring(0,st.length()-4);
                    ImageIO.write(createImage(),"png",new File(st+".png"));
                }
                catch(IOException i){ }
            }
        });
        load.addActionListener(e -> {
            fileChooser.showOpenDialog(null);
            File file = fileChooser.getSelectedFile();
            if(file!=null && file.toString().contains(".png")) {
                try {
                    loadedImage = ImageIO.read(file);
                }
                catch (IOException i) { }
                shapes = new Stack<>();
                repaint();
            }
            else if(file!=null)
                JOptionPane.showMessageDialog(null,"Wrong file type. Please select a PNG file.");
        });
        clear.addActionListener(e ->{
            shapes = new Stack<>();
            loadedImage = null;
            currentBackground = Color.WHITE;
            currentColor = colors[0];
            size.setValue(1);
            repaint();
        });
        exit.addActionListener(e -> System.exit(0));

        fileMenu.add(save);
        fileMenu.add(load);
        fileMenu.add(clear);
        fileMenu.add(exit);

        menuBar.add(fileMenu);
        menuBar.add(colorMenu);
        menuBar.add(backgroundMenu);

        bLine = setDrawButton("freeLine");
        bLine.setBackground(Color.LIGHT_GRAY);
        selectedShape = bLine;
        bRec = setDrawButton("rect");
        bOval = setDrawButton("oval");
        bErase = setDrawButton("eraser");
        bUndo = setDrawButton("undo");

        bUndo.removeActionListener(bUndo.getActionListeners()[0]);
        bUndo.addActionListener(e -> {
            if(!shapes.isEmpty()){
                undoRedo.push(shapes.pop());
                repaint();
            }
        });
        bRedo = setDrawButton("redo");
        bRedo.removeActionListener(bRedo.getActionListeners()[0]);
        bRedo.addActionListener(e -> {
            if(!undoRedo.isEmpty()){
                shapes.push(undoRedo.pop());
                repaint();
            }
        });

        points = new ArrayList<>();
        shapes = new Stack<>();
        undoRedo = new Stack<>();

        this.addMouseMotionListener(this);
        this.addMouseListener(this);
        frame.addKeyListener(this);

        menuBar.add(size);

        frame.add(menuBar,BorderLayout.NORTH);
        frame.setSize(1000,700);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D)g;

        g2.setColor(currentBackground);
        g2.fillRect(0,0,frame.getWidth(),frame.getHeight());

        if(loadedImage!=null)
            g2.drawImage(loadedImage,0,0,null);

        for(Object a : shapes) {
            if(a instanceof ArrayList) {
                p = (Point)((ArrayList<?>) a).get(0);
                drawLine(g2, (ArrayList<?>) a);
            }
            else
                drawShape(g2,(Shape)a);
        }
        p = points.isEmpty() ? null : points.get(0);
        drawLine(g2,points);
        if(currShape!=null)
            drawShape(g2,currShape);
    }

    public void drawLine(Graphics2D g2, ArrayList<?> list){
        for (int i = 1; i < list.size(); i++) {
            g2.setStroke(new BasicStroke(p.getSize()));
            g2.setColor(((Point)list.get(i)).getColor());
            g2.drawLine(p.getX(), p.getY(), ((Point)list.get(i)).getX(), ((Point)list.get(i)).getY());
            p = (Point)list.get(i);
        }
    }
    public void drawShape(Graphics2D g2, Shape s){
        g2.setColor(s.getColor());
        g2.setStroke(new BasicStroke(s.getSize()));
        g2.draw(s.getShape());
    }
    public BufferedImage createImage() {
        BufferedImage img = new BufferedImage(this.getWidth(),this.getHeight(),BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = img.createGraphics();
        this.paint(g2);
        g2.dispose();
        return img;
    }
    public JButton setDrawButton(String name){
        JButton b = new JButton();
        b.setIcon(getScaledImage(name));
        b.setFocusPainted(false);
        b.setFocusable(false);
        b.addActionListener(l ->{
            bLine.setBackground(null);
            bOval.setBackground(null);
            bRec.setBackground(null);
            bErase.setBackground(null);
            b.setBackground(Color.LIGHT_GRAY);
            if(name.equals("eraser")){
                oldColor = currentColor;
                currentColor = currentBackground;
            }
            else if(selectedShape==bErase)
                currentColor = oldColor;
            selectedShape = b;
        });
        menuBar.add(b);
        return b;
    }

    public void mouseDragged(MouseEvent e) {
        if(currShape!=null){
            if(e.getX()!=currShape.getX()){
                currShape.setWidth(Math.abs(currX-e.getX()));
                currShape.setX(Math.min(e.getX(), currX));
            }
            if(e.getY()!=currShape.getY()){
                currShape.setHeight(Math.abs(currY-e.getY()));
                currShape.setY(Math.min(e.getY(), currY));
            }
        }
        else
            points.add(new Point(e.getX(), e.getY(),size.getValue(), currentColor));
        repaint();
    }
    public void mousePressed(MouseEvent e) {
        currX = e.getX();
        currY = e.getY();
        if(selectedShape.equals(bRec))
            currShape = new Rectangle(currX, currY, size.getValue(), currentColor, 0, 0);
        else if(selectedShape.equals(bOval))
            currShape = new Oval(currX, currY, size.getValue(), currentColor, 0, 0);
    }
    public void mouseReleased(MouseEvent e) {
        if(!points.isEmpty()) {
            shapes.push(points);
            points = new ArrayList<>();
        }
        if(currShape!=null) {
            shapes.push(currShape);
            currShape = null;
        }
        undoRedo = new Stack<>();
        repaint();
    }
    public void mouseClicked(MouseEvent e) { }
    public void mouseEntered(MouseEvent e) { }
    public void mouseExited(MouseEvent e) { }
    public void mouseMoved(MouseEvent e) { }

    public ImageIcon getScaledImage(String name){
        return new ImageIcon(new ImageIcon("src/"+name+"Img.png").getImage().getScaledInstance(20, 20, Image.SCALE_SMOOTH));
    }
    public void checkErase(){
        if(selectedShape==bErase)
            bLine.getActionListeners()[0].actionPerformed(null);
    }

    public void keyPressed(KeyEvent e) {
        if(e.isControlDown()){
            if(e.getKeyCode()==KeyEvent.VK_Z)
                bUndo.getActionListeners()[0].actionPerformed(null);
            else if(e.getKeyCode()==KeyEvent.VK_Y)
                bRedo.getActionListeners()[0].actionPerformed(null);
        }
    }
    public void keyReleased(KeyEvent e) { }
    public void keyTyped(KeyEvent e) { }

    public static class Point{
        int x,y;
        Color color;
        int size;
        public Point(int x, int y, int size, Color color){
            this.x = x;
            this.y = y;
            this.color = color;
            this.size = size;
        }

        public int getX() {
            return x;
        }
        public int getY() {
            return y;
        }
        public Color getColor() {
            return color;
        }
        public int getSize() {
            return size;
        }
        public void setX(int x) {
            this.x = x;
        }
        public void setY(int y) {
            this.y = y;
        }
    }
    public static class Shape extends Point{
        private int w, h;
        public Shape(int x, int y, int size, Color color, int w, int h){
            super(x,y,size, color);
            this.w = w;
            this.h = h;
        }
        public void setWidth(int w) {
            this.w = w;
        }
        public void setHeight(int h) {
            this.h = h;
        }
        public int getWidth() {
            return w;
        }
        public int getHeight() {
            return h;
        }
        public java.awt.Shape getShape(){
            return null;
        }
    }
    public static class Rectangle extends Shape{
        public Rectangle(int x, int y, int size, Color color, int w, int h){
            super(x,y,size, color,w,h);
        }
        public Rectangle2D.Double getShape(){
            return new Rectangle2D.Double(getX(),getY(),getWidth(),getHeight());
        }
    }
    public static class Oval extends Shape{
        public Oval(int x, int y, int size, Color color, int w, int h){
            super(x,y,size, color,w,h);
        }
        public Ellipse2D.Double getShape(){
            return new Ellipse2D.Double(getX(),getY(),getWidth(),getHeight());
        }
    }
}
