package eu.biogateway.cytoscape.internal.gui.tutorial

import eu.biogateway.cytoscape.internal.BGServiceManager
import eu.biogateway.cytoscape.internal.libs.JCheckBoxTree

import javax.swing.*
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreePath
import java.awt.*
import java.io.IOException
import java.net.URI
import java.net.URISyntaxException
import java.util.function.Consumer

class BGSelectDatasets : JPanel() {
    private val rootPanel: JPanel
    private val datasetsTreePanel: JPanel
    private val infoPanel: JPanel
    private val infoTextPanel = JTextPane()

    init {
        rootPanel = JPanel(GridLayout(1, 2))
        this.datasetsTreePanel = JPanel(BorderLayout())
        this.infoPanel = JPanel(BorderLayout())
        rootPanel.add(datasetsTreePanel)
        rootPanel.add(infoPanel)
        this.add(rootPanel)
        setupTreePanel()
        setupInfoPanel()
    }

    private fun setupInfoPanel() {

        infoTextPanel.text = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum."
        infoPanel.add(infoTextPanel, BorderLayout.CENTER)
    }

    private fun setupTreePanel() {
        datasetsTreePanel.removeAll()
        val model = BGServiceManager.cache.configPanelTreeModel
        val tree = JCheckBoxTree(model)
        //tree.expandRow(0);
        tree.isRootVisible = false
        tree.showsRootHandles = true

        tree.rightClickCallback = Consumer { mouseEvent ->
            val tp = tree.getPathForLocation(mouseEvent.getX(), mouseEvent.getY())
            if (tp == null) {
                return@Consumer
            }
            val row = tree.getRowForPath(tp)
            val nodeName = tp!!.lastPathComponent.toString()
            val uri: URI
            try {
                uri = URI(BGServiceManager.cache.datasetGraphs[nodeName])
            } catch (e: URISyntaxException) {
                e.printStackTrace()
                return@Consumer
            }

            val popupMenu = JPopupMenu()
            val menuItem = JMenuItem("Open $nodeName graph description in browser.")

            menuItem.addActionListener { actionEvent ->
                println("Right-clicked node with path: " + tp.toString())
                if (Desktop.isDesktopSupported()) {
                    try {
                        Desktop.getDesktop().browse(uri)
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }

                }
            }

            popupMenu.add(menuItem)

            tree.setSelectionRow(row)

            popupMenu.show(mouseEvent.getComponent(), mouseEvent.getX(), mouseEvent.getY())

        }

        datasetsTreePanel.add(tree, BorderLayout.CENTER)

        tree.addCheckChangeEventListener { event -> BGServiceManager.dataModelController.setActiveNodesForPaths(tree.checkedPaths) }

        BGServiceManager.dataModelController.setSelectionFromPreferences(tree)

        val root = model.root as DefaultMutableTreeNode
        val path = TreePath(root.path)
        tree.fireCheckChangeEvent(JCheckBoxTree.CheckChangeEvent(path))
    }
}
