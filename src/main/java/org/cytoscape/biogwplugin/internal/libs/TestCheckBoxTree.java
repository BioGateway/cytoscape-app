package org.cytoscape.biogwplugin.internal.libs;

import javax.swing.*;
import javax.swing.tree.*;
import java.awt.*;

public class TestCheckBoxTree extends JFrame {

    public TestCheckBoxTree() {

        super();
        setSize(500, 500);
        this.getContentPane().setLayout(new BorderLayout());

        MutableTreeNode rootNode = new DefaultMutableTreeNode("Root");
        MutableTreeNode firstChild = new DefaultMutableTreeNode("Bob");
        MutableTreeNode secondChild = new DefaultMutableTreeNode("Sally");
        rootNode.insert(firstChild, 0);
        rootNode.insert(secondChild, 1);
        MutableTreeNode roger = new DefaultMutableTreeNode("Roger");
        MutableTreeNode dennis = new DefaultMutableTreeNode("Dennis");
        MutableTreeNode ruth = new DefaultMutableTreeNode("Ruth");
        firstChild.insert(roger, firstChild.getChildCount());
        firstChild.insert(dennis, firstChild.getChildCount());
        secondChild.insert(ruth, secondChild.getChildCount());

        DefaultTreeModel model = new DefaultTreeModel(rootNode);
        final JCheckBoxTree cbt = new JCheckBoxTree(model);

        this.getContentPane().add(cbt);

        cbt.addCheckChangeEventListener(new JCheckBoxTree.CheckChangeEventListener() {
            public void checkStateChanged(JCheckBoxTree.CheckChangeEvent event) {
                System.out.println(event);
                TreePath path = (TreePath) event.getSource();
                System.out.println(path);
                TreePath[] paths = cbt.getCheckedPaths();
                for (TreePath tp : paths) {
                    for (Object pathPart : tp.getPath()) {
                        System.out.print(pathPart + ",");
                    }
                    System.out.println();
                }
            }
        });
        this.setDefaultCloseOperation(EXIT_ON_CLOSE);
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) model.getRoot();
        MutableTreeNode node = cbt.find(root, "Sally");
        System.out.println(node.toString());
        TreePath path = new TreePath(root.getPath());
        cbt.checkSubTree(path, true);
        cbt.fireCheckChangeEvent(new JCheckBoxTree.CheckChangeEvent(path));
    }

    public static void main(String args[]) {
        TestCheckBoxTree test = new TestCheckBoxTree();


        test.setVisible(true);
    }
}
