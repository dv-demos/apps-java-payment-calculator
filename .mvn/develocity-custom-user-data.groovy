import java.time.Duration
import java.util.stream.Collectors
import java.security.MessageDigest
import java.security.DigestInputStream

import static com.gradle.Utils.execAndGetStdOut
import static com.gradle.Utils.execAndCheckSuccess

// Add JVM Major version as tag
Runtime.Version version = Runtime.version()
buildScan.tag("Java ${version.feature()}")

// Grab the project URL from the POM
def projectUrl = session.getTopLevelProject().getUrl()
buildScan.value("Project URL", projectUrl)
buildScan.link("Project Repository", projectUrl)

// Process this block after the build as finished and when no failures
buildScan.buildFinished(result -> {
    if (result.getFailures().empty) {
//        buildScan.value("artifacts", artifactsAsJson())

        // if 'docker' is available on the path add custom values
        // capture docker image info when `image.name` property is set in the pom
        if (execAndCheckSuccess("docker", "-v")) {
            String dockerImage = project.getProperties().getProperty("image.name")
            if (dockerImage) {
                def (dockerImageName, dockerImageTag) = dockerImage.tokenize(':')
                String imageDigest = execAndGetStdOut('docker', 'inspect', '--format={{index .RepoDigests 0}}', dockerImage)

                // docker image info (only available if image was published)
                if (imageDigest) {
                    buildScan.value("OCI Image Digest", imageDigest)
                    buildScan.value("OCI Image Digest Hash", imageDigest.tokenize('@').last())
                }
                buildScan.value("OCI Image Name", dockerImageName)
            }
        }
    }
})
buildScan.buildScanPublished(action -> {
    String dockerImage = project.getProperties().getProperty("image.name")
    def (dockerImageName, dockerImageTag) = dockerImage.tokenize(':')

    String props = "buildScanUrl=${action.getBuildScanUri()}\n"
    props += "dvAttestationUrl=${project.getProperties().getProperty("attestationServiceUrl")}/develocity/attestation/${action.getBuildScanId()}\n"
    props += "buildScanId=${action.getBuildScanId()}\n"
    props += "user=${System.getProperty("user.name")}\n"
    props += "version=${project.version}\n"
    props += "imageName=${dockerImageName}\n"
    props += "imageTag=${dockerImageTag}\n"
    props += "repo=docker-trial\n"
    props += "sign_key=default-rsa-key\n"
    new File(project.getBuild().getDirectory(), "build-scan.properties") << props
})

String artifactsAsJson() {
    Map<String, String> artifacts = new HashMap<>()
    // Create json object of artifacts
    session.getAllProjects().stream()
        // get all artifacts and attached artifacts
        .map(project -> List.of(
                Collections.singleton(project.getArtifact()),
                project.getAttachedArtifacts()))
        .flatMap(Collection::stream)
        .filter(it -> !it.empty)
        // flat map down to a Stream of artifacts
        .flatMap(Collection::stream)
        // only look at jar's for now, this filters out non-archive files, this could be improved to include, wars, ears, etc if needed
        .filter(artifact -> "jar" == artifact.type)
        // get the hash for each jar
        .forEach(artifact -> {
            File artifactFile = artifact.getFile()
            InputStream inputStream = new FileInputStream(artifactFile)
            DigestInputStream digestInputStream = new DigestInputStream(inputStream, MessageDigest.getInstance("SHA-256"))
            while (digestInputStream.read() != -1) {}
            byte[] hash = digestInputStream.getMessageDigest().digest()
            String checksum = new BigInteger(1, hash).toString(16)

            // manually create purl, as to avoid adding a dependency
            String purl = "pkg:maven/${artifact.getGroupId()}/${artifact.getArtifactId()}@${artifact.getVersion()}"
            artifacts.put(purl, checksum)
        })

    // manually build JSON, there isn't a JSON lib on the classpath
    return "[" + artifacts.entrySet().stream()
                .map(entry -> "{\"purl\": \"" + entry.key + "\", \"sha256\": " + "\"" + entry.value + "\"}")
                .collect(Collectors.joining(","))+ "]"
}