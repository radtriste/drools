#!/bin/bash

set -euo pipefail

script_dir_path=$(cd `dirname "${BASH_SOURCE[0]}"`; pwd -P)
mvn_cmd="mvn ${BUILD_MVN_OPTS:-}"

revert_commits_file="${script_dir_path}/revert_commits"

echo "Quarkus branch = ${QUARKUS_BRANCH}"
quarkus_version='999-SNAPSHOT'
echo "Quarkus version = ${quarkus_version}"

integration_branch="${INTEGRATION_BRANCH_PREFIX}${QUARKUS_BRANCH}"
echo "Setup integration branch ${integration_branch}"

set -x

# Create branch locally
if [ `git rev-parse --verify ${${integration_branch}} 2>/dev/null` ]; then
    git branch -D "${integration_branch}"
fi
git checkout -b "${integration_branch}"

# Revert commits
while read -r commit; do
    git revert ${commit}
done <"${revert_commits_file}"

# Update with Quarkus version and commit
# ${mvn_cmd} versions:compare-dependencies -pl :drools-build-parent -DremotePom=io.quarkus:quarkus-bom:${quarkus_version} -DupdatePropertyVersions=true -DupdateDependencies=true -DgenerateBackupPoms=false
# ${mvn_cmd} versions:set-property -pl :drools-build-parent -Dproperty=version.io.quarkus -DnewVersion=${quarkus_version} -DgenerateBackupPoms=false -Dmaven.wagon.http.ssl.insecure=true
if [ "$(git status --porcelain)" != '' ]; then
    git status
    git diff
    git add -u
    git commit -m "Updated to Quarkus ${quarkus_version}"
else
    echo "No quarkus update changes to commit"
fi

if [ "${GIT_PUSH_INTEGRATION_CHANGES}" = "true" ]; then
    # Remove remote branch if exists
    git fetch origin
    if [ "$(git ls-remote origin ${integration_branch})" != "" ]; then
        git push -d origin "${integration_branch}"
    fi

    # Push integration branch
    git push origin "${integration_branch}"
else
    echo "No push of integration branch as per configuration"
fi

