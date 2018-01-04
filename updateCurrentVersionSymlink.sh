#!/usr/bin/env bash

# This script will update the reference of the biogwplugin_current.jar symlink to the last compiled (installed) version.
# If a symlink is created from the Cytoscape installed plugins folder to this symlink, Cytoscape will automatically reload new versions when compiled.
# This script is executed by maven as part of the install goal, as defined in the project POM file.

ln -sf target/biogwplugin-$1.jar biogwplugin_current.jar
