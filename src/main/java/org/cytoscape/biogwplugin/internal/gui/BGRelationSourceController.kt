package org.cytoscape.biogwplugin.internal.gui

import org.cytoscape.biogwplugin.internal.model.BGRelation
import org.cytoscape.biogwplugin.internal.model.BGRelationMetadata
import java.awt.Desktop
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.net.URI
import javax.swing.table.DefaultTableModel

class BGRelationSourceController(val metadata: BGRelationMetadata): ActionListener {

    val view = BGRelationSourceView(this, null)

    init {
        val model = view.sourceInformationTable.model as DefaultTableModel
        model.setColumnIdentifiers(arrayOf("Source", "Values"))

        model.addRow(arrayOf("Edge URI:", metadata.relationTypeUri))

        for (uri in metadata.pubmedUris) {
            model.addRow(arrayOf("Pubmed URI:", uri))
        }

        metadata.sourceGraph?.let {
            model.addRow(arrayOf("Source graph:", it))
        }

        metadata.confidence?.let {
            model.addRow(arrayOf("Confidence:", it))
        }
    }


    private fun openSelectedPubmedId() {
        val model = view.sourceInformationTable.model
        val row = view.sourceInformationTable.selectedRow
        if (row == -1) return
        val pubmedId = model.getValueAt(view.sourceInformationTable.convertRowIndexToModel(row), 1) as? String
        pubmedId?.let {
            if (it.startsWith("http")) {
                // Probably a pubmed id?
                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().browse(URI(it))
                }
            }
        }

        println("SHOULD OPEN PUBMED NOW! DOESN'T THO...")
    }

    override fun actionPerformed(e: ActionEvent?) {
        if (e?.source == view.openSelectedURLButton)
        openSelectedPubmedId()
    }
}