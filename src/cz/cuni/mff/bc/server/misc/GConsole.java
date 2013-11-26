/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cuni.mff.bc.server.misc;

import com.googlecode.lanterna.gui.Action;
import com.googlecode.lanterna.gui.Border;
import com.googlecode.lanterna.gui.GUIScreen;
import com.googlecode.lanterna.gui.Interactable;
import com.googlecode.lanterna.gui.Window;
import com.googlecode.lanterna.gui.component.Label;
import com.googlecode.lanterna.gui.component.Panel;
import com.googlecode.lanterna.gui.component.TextArea;
import com.googlecode.lanterna.gui.component.TextBox;
import com.googlecode.lanterna.gui.layout.BorderLayout;
import com.googlecode.lanterna.gui.layout.HorisontalLayout;
import com.googlecode.lanterna.gui.layout.VerticalLayout;
import com.googlecode.lanterna.input.Key;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.terminal.TerminalSize;
import com.googlecode.lanterna.terminal.swing.SwingTerminal;
import cz.cuni.mff.bc.server.logging.ILogTarget;
import java.awt.event.WindowListener;
import java.util.LinkedList;

/**
 *
 * @author Jakub
 */
public class GConsole extends Thread implements ILogTarget {

    private WindowListener listener;
    private String inputDeviceType;
    private SwingTerminal terminal;
    private Screen screen;
    private GUIScreen GUI;
    private Panel mainPanel;
    private Panel inputPanel;
    private Panel historyPanel;
    private Panel logPanel;
    private TextBox input;
    private TextArea log;
    private TextArea history;
    private IConsole node;
    Window mainWindow;
    private int position = -1;
    private final int inputHistoryCapacity = 10;
    private LinkedList<String> hist = new LinkedList<>();

    private void restartHist() {
        position = -1;
    }

    private String nextInHist() {
        if (hist.size() == 0) {
            return "";
        } else if (position == hist.size() - 1) {
            return hist.get(position);
        } else {
            position++;
            return hist.get(position);
        }
    }

    private String previousInHist() {
        if (position == 0) {
            position = -1;
            return "";
        } else if (position == -1) {
            return "";
        } else {
            position--;
            return hist.get(position);
        }
    }

    private void addToHist(String msg) {
        if (hist.size() >= inputHistoryCapacity) {
            hist.removeLast();
        }
        hist.addFirst(msg);
    }

    @Override
    public synchronized void log(String message) {
        log.insertLine(0, " " + message + "  ");
    }

    public synchronized void printToHistory(String message) {
        history.insertLine(0, " " + message + "  ");
    }

    public GConsole(IConsole node, String type, WindowListener listener) {
        this.listener = listener;
        this.node = node;
        this.inputDeviceType = type;
        terminal = new SwingTerminal(new TerminalSize(100, 30));

        screen = new Screen(terminal);
        GUI = new GUIScreen(screen);
        mainWindow = new Window("DSCN: Distributed Computing in Small Networks");
        mainPanel = new Panel(new Border.Invisible(), Panel.Orientation.VERTICAL);
        mainPanel.setLayoutManager(new BorderLayout());

        inputPanel = new Panel("Input: ", new Border.Standard(), Panel.Orientation.HORISONTAL);
        inputPanel.setLayoutManager(new HorisontalLayout());
        historyPanel = new Panel("Input history: ", new Border.Standard(), Panel.Orientation.HORISONTAL);
        historyPanel.setLayoutManager(new VerticalLayout());
        logPanel = new Panel("Log: ", new Border.Standard(), Panel.Orientation.HORISONTAL);

        log = new TextArea();
        log.removeLine(0);

        history = new TextArea();
        history.removeLine(0);

    }

    public void startConsole() {
       start();
    }

    @Override
    public void run() {
        showConsole();
    }

    private void showConsole() {
        if (GUI == null) {
            System.err.println("Couldn't allocate a terminal!");
            return;
        }
        GUI.getScreen().startScreen();
        terminal.getJFrame().addWindowListener(listener);

        input = new TextBox("") {
            @Override
            public Interactable.Result keyboardInteraction(Key key) {
                if (key.equals(new Key(Key.Kind.Enter))) {
                    if (!this.getText().equals("")) {
                        printToHistory(inputDeviceType + "> " + this.getText());
                        addToHist(this.getText());
                        restartHist();
                        node.proceedCommand(this.getText());
                    }
                    this.setText("");
                    return Result.EVENT_HANDLED;

                } else if (key.equals(new Key(Key.Kind.ArrowUp))) {
                    this.setText(previousInHist());
                    return Result.EVENT_HANDLED;
                } else if (key.equals(new Key(Key.Kind.ArrowDown))) {
                    this.setText(nextInHist());
                    return Result.EVENT_HANDLED;
                } else {
                    return super.keyboardInteraction(key);
                }
            }
        };
        input.setPreferredSize(new TerminalSize(50, 1));

        inputPanel.addComponent(new Label(inputDeviceType + ">"));
        inputPanel.addComponent(input);
        historyPanel.addComponent(history);
        logPanel.addComponent(log);


        TerminalSize size = GUI.getScreen().getTerminalSize();
        int logPanelRows = (int) Math.floor(((float) size.getRows() / (float) 100) * 25);
        history.setMinimumSize(new TerminalSize(size.getColumns(), size.getRows() - logPanelRows - 1));
        input.setPreferredSize(new TerminalSize(size.getColumns() - 8, 1));
        log.setMaximumSize(new TerminalSize(size.getColumns(), logPanelRows));
        log.setMinimumSize(new TerminalSize(size.getColumns(), logPanelRows));
        mainPanel.addComponent(inputPanel, BorderLayout.TOP);
        mainPanel.addComponent(logPanel, BorderLayout.BOTTOM);
        mainPanel.addComponent(historyPanel, BorderLayout.LEFT);

        mainPanel.addShortcut(Key.Kind.Escape, new Action() {
            @Override
            public void doAction() {
                listener.windowClosing(null);
            }
        });

        mainWindow.addComponent(mainPanel, BorderLayout.CENTER);
        //GUI.showWindow(mainWindow, GUIScreen.Position.FULL_SCREEN);
        //GUI.getScreen().stopScreen();
        new Thread(){
            public void run(){
        GUI.showWindow(mainWindow, GUIScreen.Position.FULL_SCREEN);
            }
        }.start();
        
        

        //GUI.getScreen().stopScreen();
    }
}
