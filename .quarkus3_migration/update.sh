#!/bin/bash
set -euo pipefail

script_dir_path=$(cd `dirname "${BASH_SOURCE[0]}"`; pwd -P)

mvn_cmd="mvn ${BUILD_MVN_OPTS:-}"
cherry_picks_file="${script_dir_path}/cherry-picks"
quarkus_file="${script_dir_path}/quarkus3.yml"
quarkus_version=${QUARKUS_VERSION:-3.0.0.Alpha2}

set -x

# TODO should be changed once going to kiegroup/main
curl -o "${quarkus_file}" https://raw.githubusercontent.com/radtriste/kogito-pipelines/quarkus_3/.quarkus3_migration/quarkus3.yml

# Make sure artifacts are updated locally
${mvn_cmd} clean install -Dquickly
# Update Quarkus version in project
${mvn_cmd} versions:set-property -pl :drools-build-parent -Dproperty=version.io.quarkus -DnewVersion=${quarkus_version} -DgenerateBackupPoms=false  -Dmaven.wagon.http.ssl.insecure=true
# Launch Openrewrite
${mvn_cmd} org.openrewrite.maven:rewrite-maven-plugin:4.36.0:run -Drewrite.configLocation="${quarkus_file}" -DactiveRecipes=io.quarkus.openrewrite.Quarkus3 -Denforcer.skip
# Update dependencies with Quarkus 3 bom
${mvn_cmd} versions:compare-dependencies -pl :drools-build-parent -DremotePom=io.quarkus:quarkus-bom:${quarkus_version} -DupdatePropertyVersions=true -DupdateDependencies=true -DgenerateBackupPoms=false

git status
git diff
git add -u
git commit -m "Updated to Quarkus ${quarkus_version} with openrewrite"

set +x

# Read cherry-picks
while read -r commit; do
    set -x
    git cherry-pick ${commit}
    set +x
done <"${cherry_picks_file}" 

rm -rf "${quarkus_file}"