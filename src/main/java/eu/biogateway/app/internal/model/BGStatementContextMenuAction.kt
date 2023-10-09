package eu.biogateway.app.internal.model

class BGStatementContextMenuAction(val id: String, val resourceURI: String, val singleLabel: String, val multipleLabel: String, val supportedRelations: Collection<BGRelationType>, val type: ActionType) {
    enum class ActionType {
        OPEN_URI,
        COPY_URI
    }
}