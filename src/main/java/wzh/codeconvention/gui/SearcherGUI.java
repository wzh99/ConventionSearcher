package wzh.codeconvention.gui;

import wzh.codeconvention.core.Searcher;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.io.File;
import java.io.IOException;

public class SearcherGUI {
    public static void main(String[] args) {
        // Create main frame
        var frame = new JFrame("Code Convention Searcher");
        frame.setSize(386, 630);
        frame.setResizable(false);
        frame.setLayout(new FlowLayout(FlowLayout.LEADING));
        frame.setLocationByPlatform(true);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Create menu bar
        var menuBar = new JMenuBar();
        frame.setJMenuBar(menuBar);
        var fileMenu = menuBar.add(new JMenu("File"));
        var loadItem = fileMenu.add("Load...");
        var buildItem = fileMenu.add("Build...");
        var aboutMenu = menuBar.add(new JMenu("About"));

        // Create search bar
        var searchPanel = new JPanel();
        searchPanel.setPreferredSize(new Dimension(366, 32));
        frame.add(searchPanel);
        var inputText = new JTextField(18);
        inputText.setFont(new Font(null, Font.PLAIN, 18));
        searchPanel.add(inputText);
        var searchButton = new JButton("Search");
        searchButton.setEnabled(false); // cannot search when index is not loaded
        searchPanel.add(searchButton);

        // Create search result pane
        var resultPanel = new JPanel();
        resultPanel.setPreferredSize(new Dimension(366, 520));
        frame.add(resultPanel);
        String[] contents = {"<html><code>1</code>", "2", "3", "4", "5", "6", "7", "8", "9"};
        var resultList = new JList<>(contents);
        resultList.setFixedCellWidth(336);
        resultList.setFixedCellHeight(128);
        resultList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        var scrollPane = new JScrollPane(resultList, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setPreferredSize(new Dimension(352, 512));
        resultPanel.add(scrollPane);

        // Create index building dialog
        var buildDialog = new JDialog(frame);
        buildDialog.setTitle("Build");
        buildDialog.setSize(480, 124);
        buildDialog.setResizable(false);
        buildDialog.setLayout(new FlowLayout(FlowLayout.CENTER));
        buildDialog.setLocationByPlatform(true);
        buildDialog.setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);
        var mdLabel = new JLabel("Markdown file: ");
        mdLabel.setPreferredSize(new Dimension(100, 18));
        buildDialog.add(mdLabel);
        var mdField = new JTextField(26);
        buildDialog.add(mdField);
        var mdButton = new JButton("Browse");
        mdButton.setPreferredSize(new Dimension(80, 18));
        buildDialog.add(mdButton);
        var outLabel = new JLabel("Output path: ");
        outLabel.setPreferredSize(new Dimension(100, 18));
        buildDialog.add(outLabel);
        var outField = new JTextField(26);
        buildDialog.add(outField);
        var outButton = new JButton("Browse");
        outButton.setPreferredSize(new Dimension(80, 18));
        buildDialog.add(outButton);
        var buildButton = new JButton("Build");
        buildDialog.add(buildButton);

        frame.setVisible(true);

        // Initialize searcher
        var searcher = new Searcher();

        // Set load action
        loadItem.addActionListener(event -> {
            var chooser = new JFileChooser("./");
            chooser.setMultiSelectionEnabled(false);
            chooser.setFileFilter(new FileFilter() {
                @Override
                public boolean accept(File f) {
                    return f.isDirectory() || f.getName().toLowerCase().endsWith(".idx");
                }

                @Override
                public String getDescription() {
                    return "Index file (*.idx)";
                }
            });

            if (chooser.showOpenDialog(loadItem) == JFileChooser.APPROVE_OPTION) {
                var file = chooser.getSelectedFile();
                String errMsg = null;
                try {
                    searcher.load(file.getPath());
                } catch (IOException e) {
                    errMsg = "Error opening file";
                } catch (ClassNotFoundException e) {
                    errMsg = "Invalid index file";
                }
                if (errMsg != null) { // show error dialog box
                    showErrorMessage(errMsg);
                } else {
                    searchButton.setEnabled(true); // can search then
                }
            }
        });

        // Add build action
        var nameField = new JTextField(); // invisible, just help getting output path
        mdButton.addActionListener(event -> {
            var chooser = new JFileChooser("./");
            chooser.setMultiSelectionEnabled(false);
            chooser.setFileFilter(new FileFilter() {
                @Override
                public boolean accept(File f) {
                    return f.isDirectory() || f.getName().toLowerCase().endsWith(".md");
                }

                @Override
                public String getDescription() {
                    return "Markdown File (*.md)";
                }
            });
            if (chooser.showOpenDialog(buildDialog) == JFileChooser.APPROVE_OPTION) {
                mdField.setText(chooser.getSelectedFile().getPath());
                nameField.setText(chooser.getSelectedFile().getName());
            }
        });

        outButton.addActionListener(event -> {
            var chooser = new JFileChooser("./");
            chooser.setMultiSelectionEnabled(false);
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            if (chooser.showOpenDialog(buildDialog) == JFileChooser.APPROVE_OPTION) {
                outField.setText(chooser.getSelectedFile().getPath());
            }
        });

        buildButton.addActionListener(event -> {
            var mdPath = mdField.getText();
            if (mdPath.isBlank()) {
                showErrorMessage("Please specify path of Markdown File");
                return;
            }
            var outDir = outField.getText();
            if (outDir.isBlank()) {
                showErrorMessage("Please specify path of output index file.");
                return;
            }
            var nameStr = nameField.getText();
            var outPath = outDir + "\\" + nameStr.substring(0, nameStr.lastIndexOf('.')) + ".idx";
            try {
                searcher.build(mdPath, outPath);
            } catch (IOException e) {
                showErrorMessage("Error reading Markdown file.");
                return;
            }
            searchButton.setEnabled(true);
            buildDialog.setVisible(false);
        });

        buildItem.addActionListener(event -> buildDialog.setVisible(true));
    }

    private static void showErrorMessage(String message) {
        var errDialog = new JDialog();
        errDialog.add(new JLabel(message));
        errDialog.setLocationByPlatform(true);
        errDialog.setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);
        errDialog.setVisible(true);
    }
}
