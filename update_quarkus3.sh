#!/bin/bash
set -euo pipefail
set -x

quarkus_version=3.0.0.Alpha2

# Make sure artifacts are updated locally
mvn clean install -Dquickly
# Update Quarkus version in project
mvn versions:set-property -pl :drools-build-parent -Dproperty=version.io.quarkus -DnewVersion=${quarkus_version} -DgenerateBackupPoms=false  -Dmaven.wagon.http.ssl.insecure=true
# Launch Openrewrite
mvn org.openrewrite.maven:rewrite-maven-plugin:4.36.0:run -Drewrite.configLocation=$(pwd)/quarkus3.yml -DactiveRecipes=io.quarkus.openrewrite.Quarkus3 -Denforcer.skip
# Update dependencies with Quarkus 3 bom
mvn versions:compare-dependencies -pl :drools-build-parent -DremotePom=io.quarkus:quarkus-bom:${quarkus_version} -DupdatePropertyVersions=true -DupdateDependencies=true -DgenerateBackupPoms=false
