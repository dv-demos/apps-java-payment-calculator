#!/usr/bin/env bash

set -e
# export JFROG_CLI_LOG_LEVEL=DEBUG

source target/build-scan.properties

DOCKER_REPO="${repo}"
BUILD_NAME="$(basename ${imageName})"
BUILD_NUMBER="${imageTag}"
BUILD_ACTOR="${user}"
SIGN_KEY="${sign_key}"

# Use docker image built/published in other process
IMAGE_NAME_SHORT="$(basename ${imageName})"
IMAGE_NAME="${imageName}"
IMAGE_TAG="${imageTag}"

# Output image with SHA256 to file
echo "Creating image meta file"
META_FILE=image-meta
IMAGE_NAME_SHA=$(docker inspect --format='{{index .RepoDigests 0}}' ${IMAGE_NAME}:${IMAGE_TAG})
echo "${IMAGE_NAME}:${IMAGE_TAG}@${IMAGE_NAME_SHA#*@}" > ${META_FILE}
echo "Processing image: $(cat ${META_FILE})"

# Register build with docker info
echo "Registering build with Artifactory"
jf rt build-docker-create \
  ${DOCKER_REPO} \
  --image-file ${META_FILE} \
  --build-name ${BUILD_NAME} \
  --build-number ${BUILD_NUMBER}

# Create attestation
echo "Creating build attestation file"
jq -n \
  --arg actor "${BUILD_ACTOR}" \
  --arg date $(date -u +"%Y-%m-%dT%H:%M:%SZ") \
  '{actor: $actor, date: $date}' \
  > actor-attestation.json

# Attach attestation to build
echo "Attaching attestation file to build"
jf evd create \
  --package-name ${IMAGE_NAME_SHORT} \
  --package-version ${IMAGE_TAG} \
  --package-repo-name ${DOCKER_REPO} \
  --predicate ./actor-attestation.json \
  --predicate-type https://jfrog.com/evidence/signature/v1

# Add git info attestation
jf rt build-add-git ${BUILD_NAME} ${BUILD_NUMBER}

# Collect env vars
jf rt build-collect-env ${BUILD_NAME} ${BUILD_NUMBER}

# Publish build info to Artifactory
echo "Publishing build info"
jf rt build-publish ${BUILD_NAME} ${BUILD_NUMBER}

echo "Creating Develocity attestation file"
curl ${dvAttestationUrl} -o dv-build-scan-attestation-org.json
cat dv-build-scan-attestation-org.json | jq .predicate
cat dv-build-scan-attestation-org.json | jq .predicate > dv-build-scan-attestation.json

echo "Attach build scan attestation to build"
jf evd create \
  --build-name ${BUILD_NAME} \
  --build-number ${BUILD_NUMBER} \
  --predicate ./dv-build-scan-attestation.json \
  --predicate-type https://develocity.com/provenance/v1

# Create release bundle
echo "Creating release bundle file"
jq -n \
  --arg buildFile "${BUILD_NAME}/${BUILD_NUMBER}" \
  '{files: [{build: $buildFile}]}' \
  > bundle-spec.json

echo "Creating release bundle in Artifactory"
jf release-bundle-create ${BUILD_NAME} ${BUILD_NUMBER} \
  --build-name ${BUILD_NAME} \
  --build-number ${BUILD_NUMBER} \
  --signing-key ${SIGN_KEY} \
  --spec bundle-spec.json \
  --sync=true

jf evd create \
  --release-bundle ${BUILD_NAME} \
  --release-bundle-version ${BUILD_NUMBER} \
  --predicate ./dv-build-scan-attestation.json \
  --predicate-type https://develocity.com/provenance/v1

echo
echo "*****************************************************************************"
echo "** Docker Image sha256:                                                    **"
echo "** ${IMAGE_NAME_SHA#*@} **"
echo "*****************************************************************************"
echo