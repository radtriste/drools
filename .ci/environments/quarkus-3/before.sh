#!/bin/bash
set -euo pipefail

script_dir_path=$(cd `dirname "${BASH_SOURCE[0]}"`; pwd -P)
mvn_cmd="mvn ${BUILD_MVN_OPTS:-} ${BUILD_MVN_OPTS_QUARKUS_UPDATE:-}"

quarkus_version=${QUARKUS_VERSION:-3.0.0.Alpha2}
quarkus_file="${script_dir_path}/quarkus3.yml"
project_version=$(${mvn_cmd} help:evaluate -Dexpression=project.version -q -DforceStdout)

echo "Update project with Quarkus version ${QUARKUS_VERSION}"

set -x

# Make sure artifacts are updated locally
${mvn_cmd} clean install -Dquickly
# Update Quarkus version in project
${mvn_cmd} versions:set-property \
    -pl :drools-build-parent 
    -Dproperty=version.io.quarkus \
    -DnewVersion=${quarkus_version} \
    -DgenerateBackupPoms=false \
    -Dmaven.wagon.http.ssl.insecure=true
# Launch Quarkus 3 Openrewrite
${mvn_cmd} org.openrewrite.maven:rewrite-maven-plugin:4.36.0:run \
    -Drewrite.configLocation="${quarkus_file}" \
    -DactiveRecipes=io.quarkus.openrewrite.Quarkus3 \
    -Denforcer.skip
# Launch JPMML Openrewrite
${mvn_cmd} org.openrewrite.maven:rewrite-maven-plugin:4.36.0:run \
    -DactiveRecipes=org.kie.openrewrite.recipe.jpmml.JPMMLRecipe \
    -Drewrite.recipeArtifactCoordinates=org.kie:jpmml-migration-recipe:"${project_version}" \
    -Denforcer.skip
# Update dependencies with Quarkus 3 bom
${mvn_cmd} versions:compare-dependencies -pl :drools-build-parent -DremotePom=io.quarkus:quarkus-bom:${quarkus_version} -DupdatePropertyVersions=true -DupdateDependencies=true -DgenerateBackupPoms=false
