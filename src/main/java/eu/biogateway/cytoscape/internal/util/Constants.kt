package eu.biogateway.cytoscape.internal.util

/**
 * Created by sholmas on 12/06/2017.
 */


object Constants {
    val PROFILING = false
    val TEST_SERVER = true

    val BG_SELECTED_ACTIVE_RELATION_TYPES = "biogatewayPluginActiveRelationTypes"
    val BG_SHOULD_USE_BG_DICT = true
    val BG_FIELD_IDENTIFIER_URI = "identifier uri"
    val BG_FIELD_NAME = "name"
    val BG_FIELD_INTERACTION = "interaction"
    val BG_FIELD_NODE_TYPE: String = "type"
    val BG_FIELD_DESCRIPTION = "Description"
    val BG_FIELD_NODE_PARENT_EDGE_ID: String = "Parent Edge ID"
    val BG_FIELD_EDGE_ID = "Edge Id"
    val BG_FIELD_EDGE_EXPANDABLE = "Expandable"
    val BG_FIELD_PUBMED_URI = "Pubmed uri"
    val BG_TABLE_NODE_METADATA = "Biogateway Node Metadata Table"
    val BG_CONFIG_FILE_URL: String get() {
        return if (TEST_SERVER) {
            "https://www.dropbox.com/s/lrw0ue4ikzy5u61/BiogatewayQueries_Dropbox_Beta.xml?dl=1"
        } else {
            "https://www.dropbox.com/s/ad4nyuqjtchsjcc/BiogatewayQueries_Dropbox.xml?dl=1"
        }
    }

    //  val BG_CONFIG_FILE_URL = "https://bitbucket.org/druglogics/biogw_cytoscape/raw/b429639d3440fde532d2b8c8f43cf54888560950/BiogatewayQueries.xml?at=master"
    val SERVER_PATH = "http://www.semantic-systems-biology.org/biogateway/endpoint"
    val DICTIONARY_SERVER_PATH = "http://localhost:3002/"
    val BG_FIELD_SOURCE_GRAPH = "Source Graph"
    val BG_FIELD_CONFIDENCE = "Confidence Value"
    val BG_LOAD_NODE_WARNING_LIMIT = 1000
    val BG_BULK_IMPORT_WARNING_LIMIT = 100
    val BG_FILE_EXTENSION = "bgwsparql"
    val BG_PREFERENCES_LAST_FOLDER = "biogatewayPluginLastOpenedFolder"
    val BG_RELATION_COUNT_WARNING_LIMIT = 50000
    val BG_QUERY_BUILDER_URI_FIELD_COLUMNS = 20
    val BG_QUERYBUILDER_ENTITY_LABEL = "Entity:"
}