package botenanna.behaviortree.builder;

import botenanna.behaviortree.BehaviorTree;
import botenanna.behaviortree.Node;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class BehaviourTreeBuilder {

    private Component parent;
    private File file;

    /** A BehaviourTreeBuilder is an assisting tool for building BehaviourTrees from files. The file can be specified
     * with a FileChooser. */
    public BehaviourTreeBuilder(Component parent) {
        this.parent = parent;
    }

    /** Set the file used to generate a behaviour tree with the build method. This method will open a file chooser
     * window, where the user can specify which file to open. */
    public void setFileWithChooser() throws MissingBehaviourTreeException {
        // Open file window
        JFileChooser fc = new JFileChooser();
        int status = fc.showOpenDialog(parent);
        if (status == JFileChooser.APPROVE_OPTION) {
            // Build from file
            file = fc.getSelectedFile();
        } else {
            // User did not choose a file or error occurred
            throw new MissingBehaviourTreeException("User did not choose a file or error occurred.");
        }
    }

    /** Build a BehaviourTree from the file specified with the {@code setFileWithChooser} method. Throws a MissingBehaviourTreeException
     * if the file has not been specified.
     * @return a BehaviourTree. */
    public BehaviorTree build() throws MissingBehaviourTreeException, FileNotFoundException, IOException {
        if (file == null) throw new MissingBehaviourTreeException("File not specified yet.");
        return build(file);
    }

    /** Build a BehaviourTree from a filepath. */
    public BehaviorTree build(String path) throws FileNotFoundException, IOException {
        File file = new File(path);
        return build(file);
    }

    /** Build a behaviour tree from a File. */
    public BehaviorTree build(File file) throws FileNotFoundException, IOException {

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            Queue<String> queue = fileToQueue(reader);
            BehaviorTree bt = new BehaviorTree();
            readTreeRecursive(bt, -1, queue, queue.size());
            return bt;
        }
    }

    /** Recursively read a Queue of lines and construct a tree of Nodes from it. The tree's structure is formed from
     * the indentation of the lines. Will crash the program, if a line is unreadable.
     * @param parent the Node created from previous line. Can be an external node. The tree will become children of this node.
     * @param level the current amount of indentation.
     * @param queue a queue of unread lines.
     * @param lineCount the original size of the queue. Used to determine in which line errors occur.*/
    // Inspiration: https://stackoverflow.com/questions/6075974/python-file-parsing-build-tree-from-text-file?rq=1
    private void readTreeRecursive(Node parent, int level, Queue<String> queue, int lineCount) {
        while (queue.size() > 0) {
            String line = queue.peek();

            // Empty line?
            if (line.length() == 0) {
                throw new BehaviourTreeReadException("Empty line met when reading behaviour tree source file (line " + (lineCount - queue.size()) + ").");
            }

            // Break if this line belongs to another parent
            int indent = readIndent(line);
            if (indent <= level) {
                break;
            }

            // Is a child?
            if (indent == level + 1) {
                try {
                    // Node in this line is a child of the parent
                    Node node = translateLineToNode(queue.remove());
                    parent.addChild(node);
                    // Check if the node has children (recursion!)
                    readTreeRecursive(node, indent, queue, lineCount);
                } catch (BehaviourTreeUnknownNodeException e) {
                    e.printStackTrace();
                } catch (BehaviourTreeBuildingException e) {
                    throw new BehaviourTreeReadException("Error in source file. Could not add node to parent (line " + (lineCount - queue.size()) + ").");
                }
            } else {
                // Error in indentation
                throw new BehaviourTreeReadException("Wrong indentation in behaviour tree source file (line " + (lineCount - queue.size()) + ").");
            }
        }
    }

    /** Break a line into parts and create a Node from it.
     * @param line a line from a BehaviourTree source file. */
    private Node translateLineToNode(String line) {
        List<String> parts = Arrays.stream(line.replace("\t", "").split(" ")).filter(s -> s.length() != 0).collect(Collectors.toList());
        List<String> args = parts.size() > 1 ? parts.subList(1, parts.size()) : new ArrayList<>();
        return NodeLibrary.nodeFromString(parts.get(0), args.toArray(new String[0]));
    }

    /** Transform a BufferedReader of Strings to a Queue of Strings. Throws an IOException if reading fails.
     * @param reader a BufferedReader.
     * @return a Queue of Strings with the first line as the first element. */
    private Queue<String> fileToQueue(BufferedReader reader) throws IOException {
        Queue<String> queue = new LinkedList<>();

        // Add lines to queue
        while (true) {
            String line = reader.readLine();
            if (line != null) {
                queue.add(line);
            } else {
                break;
            }
        }

        return queue;
    }

    /** Calculate the number of tabs leading a String.
     * @param line a String
     * @return the number of tabs leading the String. */
    private int readIndent(String line) throws BehaviourTreeReadException {
        // Count lines
        int indent = 0;
        for (int i = 0; i < line.length(); i++) {
            if (line.charAt(i) == '\t') {
                indent++;
            } else {
                break;
            }
        }
        return indent;
    }
}