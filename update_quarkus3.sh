#!/bin/bash
set -euo pipefail
set -x

# Make sure artifacts are updated locally
mvn clean install -Dquickly
# Update Quarkus version in project
mvn versions:set-property -pl :drools-build-parent -Dproperty=version.io.quarkus -DnewVersion=3.0.0.Alpha1 -DgenerateBackupPoms=false  -Dmaven.wagon.http.ssl.insecure=true
# Launch Openrewrite
mvn org.openrewrite.maven:rewrite-maven-plugin:4.36.0:run -Drewrite.configLocation=$(pwd)/quarkus3.yml -DactiveRecipes=io.quarkus.openrewrite.Quarkus3 -Denforcer.skip
# Update dependencies with Quarkus 3 bom
mvn versions:compare-dependencies -pl :drools-build-parent -DremotePom=io.quarkus:quarkus-bom:3.0.0.Alpha1 -DupdatePropertyVersions=true -DupdateDependencies=true -DgenerateBackupPoms=false
