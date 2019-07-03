package wzh.codeconvention.gui;

import wzh.codeconvention.core.IndexNotLoadedException;
import wzh.codeconvention.core.SearchResult;
import wzh.codeconvention.core.Searcher;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;

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
        var searchField = new JTextField(18);
        searchField.setFont(new Font(null, Font.PLAIN, 18));
        searchPanel.add(searchField);
        var searchButton = new JButton("Search");
        searchButton.setEnabled(false); // cannot search when index is not loaded
        searchPanel.add(searchButton);

        // Create search result pane
        var resultPanel = new JPanel();
        resultPanel.setPreferredSize(new Dimension(366, 520));
        frame.add(resultPanel);
        var resultList = new JList<>();
        var cellRenderer = new DefaultListCellRenderer();
        cellRenderer.setVerticalAlignment(SwingConstants.TOP);
        resultList.setCellRenderer(cellRenderer);
        resultList.setFixedCellWidth(336);
        resultList.setFixedCellHeight(100);
        resultList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        var scrollPane = new JScrollPane(resultList, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setPreferredSize(new Dimension(352, 512));
        resultPanel.add(scrollPane);

        // Create index building dialog
        var buildDialog = new JDialog(frame);
        buildDialog.setTitle("Build");
        buildDialog.setSize(470, 124);
        buildDialog.setResizable(false);
        buildDialog.setLayout(new FlowLayout(FlowLayout.CENTER));
        buildDialog.setLocationByPlatform(true);
        buildDialog.setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);
        var mdLabel = new JLabel("Markdown file: ");
        mdLabel.setPreferredSize(new Dimension(90, 18));
        buildDialog.add(mdLabel);
        var mdField = new JTextField(26);
        buildDialog.add(mdField);
        var mdButton = new JButton("Browse");
        mdButton.setPreferredSize(new Dimension(80, 18));
        buildDialog.add(mdButton);
        var outLabel = new JLabel("Output path: ");
        outLabel.setPreferredSize(new Dimension(90, 18));
        buildDialog.add(outLabel);
        var outField = new JTextField(26);
        buildDialog.add(outField);
        var outButton = new JButton("Browse");
        outButton.setPreferredSize(new Dimension(80, 18));
        buildDialog.add(outButton);
        var buildButton = new JButton("Build");
        buildDialog.add(buildButton);

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
        var nameField = new JTextField(); // invisible, just help get output path
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

        // Set search action
        var results = new ArrayList<SearchResult>();
        searchButton.addActionListener(event -> {
            var searchText = searchField.getText();
            if (searchText.length() == 0) return;
            try {
                results.addAll(searcher.search(searchText));
            } catch (IndexNotLoadedException e) { // this will not occur in GUI program
                e.printStackTrace();
                return;
            }
            var itemArr = new String[results.size()];
            for (var i = 0; i < itemArr.length; i++)
                itemArr[i] = MarkdownRenderer.render(results.get(i).getTag().toString());
            resultList.setListData(itemArr);
        });

        // Set selection action
        resultList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() < 2) return;
                // Get HTML content to be displayed
                var selItem = results.get(resultList.getSelectedIndex());
                var pageStr = MarkdownRenderer.render(selItem.getTag().getNode().toString());
                // Write to a temporary local file
                var file = new File("tmp.html");
                try {
                    var out = new FileWriter(file);
                    out.write(pageStr);
                    out.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                    return;
                }
                // Show HTML in a window
                JEditorPane pagePane = null;
                try {
                    pagePane = new JEditorPane(file.toURI().toURL());
                } catch (IOException ex) {
                    ex.printStackTrace();
                    return;
                }
                pagePane.setText(pageStr);
                pagePane.setSize(800, 0);
                pagePane.setEditable(false);
                var pageScroll = new JScrollPane(pagePane, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
                        ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
                var pageDialog = new JDialog(frame);
                pageDialog.setContentPane(pageScroll);
                pageDialog.setLocationByPlatform(true);
                pageDialog.setSize(800, 500);
                pageDialog.setVisible(true);
                pageDialog.addWindowListener(new WindowAdapter() {
                    @Override
                    public void windowClosing(WindowEvent e) {
                        file.delete();
                    }
                });
            }
        });

        frame.setVisible(true);
    }

    private static void showErrorMessage(String message) {
        var errDialog = new JDialog();
        errDialog.add(new JLabel(message));
        errDialog.setLocationByPlatform(true);
        errDialog.setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);
        errDialog.setVisible(true);
    }
}
