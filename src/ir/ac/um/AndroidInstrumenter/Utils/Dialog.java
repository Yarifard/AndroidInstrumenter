package ir.ac.um.AndroidInstrumenter.Utils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

public class Dialog {
    private JLabel mainLabel;
    private JLabel libLabel;
    private JTextField libPath;
    private JButton browseButton;
    private JButton yesButton;
    private String pathContent;
    private JPanel mainPanel;
    private JPanel topPanel;
    private JPanel centerPanel;
    private JPanel bottomPanel;
    private File targetFolder;

    public Dialog() {
        mainLabel = new JLabel("Please enter the path of AddOnLibrary:");
        libLabel = new JLabel("Lib path :");
        browseButton = new JButton("Browse");
        libPath = new JTextField();

        mainPanel = new JPanel();
        topPanel = new JPanel();
        centerPanel = new JPanel();
        bottomPanel = new JPanel();
        pathContent = "";
        targetFolder = null;
        initial();
        createLayout();
    }

    private void initial() {

        mainPanel.setLayout(new BorderLayout());
        mainPanel.setSize(new Dimension(500, 200));
        mainPanel.setBorder(BorderFactory.createLineBorder(Color.white, 2));
        libPath.setPreferredSize(new Dimension(300,50));
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.X_AXIS));
        topPanel.setSize(new Dimension(500,50));
        centerPanel.setLayout(new FlowLayout());
        centerPanel.setSize(new Dimension(500,100));
        bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.X_AXIS));
        bottomPanel.setSize(new Dimension(500,75));
        browseButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFrame jframe = new JFrame();
                JFileChooser fc = new JFileChooser();
                fc.setCurrentDirectory(new java.io.File(".")); // start at application current directory
                fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                int returnVal = fc.showSaveDialog(jframe);
                if(returnVal == JFileChooser.APPROVE_OPTION) {
                  libPath.setText(fc.getSelectedFile().getPath());
                  targetFolder = fc.getSelectedFile();
                }
            }
        });

        mainLabel.setFont(new Font("Times New Roman", Font.BOLD, 14));
        libLabel.setFont(new Font("Times New Roman", Font.PLAIN, 12));

    }

    public void createLayout() {
        topPanel.add(mainLabel);
        centerPanel.add(libLabel);
        centerPanel.add(libPath);
        centerPanel.add(browseButton);
        mainPanel.add(topPanel, BorderLayout.NORTH);
        mainPanel.add(centerPanel, BorderLayout.CENTER);

        int result = JOptionPane.showConfirmDialog(null, mainPanel,
                "Selecting AddOnLibrary Folder", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);
        if (result == JOptionPane.YES_OPTION) {
            pathContent = libPath.getText();
        }
    }

    public File getTargetFolder() {
           return targetFolder;
    }

}