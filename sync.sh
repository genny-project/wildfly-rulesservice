#!/bin/bash
mkdir -p rulesservice-war/src/main/java/life/genny/utils
rsync -rav ../rulesservice/src/main/java/life/genny/utils/* rulesservice-war/src/main/java/life/genny/utils/
