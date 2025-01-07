import java.util.regex.Matcher
import java.util.regex.Pattern
import java.util.stream.Collectors
import java.util.stream.Stream
import java.security.MessageDigest
import java.security.DigestInputStream

import static com.gradle.CiUtils.isGitLab
import static com.gradle.Utils.envVariable
import static com.gradle.Utils.redactUserInfo
import static com.gradle.Utils.urlEncode
import static com.gradle.Utils.appendIfMissing
import static com.gradle.Utils.execAndGetStdOut
import static com.gradle.Utils.execAndCheckSuccess


// Process this block after the build as finished and when no failures
buildScan.buildFinished(result -> {

    Map<String, String> artifacts = new HashMap<>()
    if (result.getFailures().empty) {

        // Create json object of artifacts
        session.getAllProjects().stream()
            .map(project -> List.of(
                    Collections.singleton(project.getArtifact()),
                    project.getAttachedArtifacts()))
                .flatMap(Collection::stream)
                .filter(it -> !it.empty)
                .flatMap(Collection::stream)
                .filter(wtf -> "jar" == wtf.type)
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

        // manually build JSON, there isn't one on the classpath
        buildScan.value("artifacts", "[" +
                artifacts.entrySet().stream()
                    .map(entry -> "{\"purl\": \"" + entry.key + "\", \"sha256\": " + "\"" + entry.value + "\"}")
                    .collect(Collectors.joining(","))
                + "]")
    }

    // if 'docker' is available on the path add custom values
    // capture docker image info when `image.name` property is set in the pom
    if (execAndCheckSuccess("docker", "-v")) {
        String dockerImage = project.getProperties().getProperty("image.name")
        if (dockerImage) {
            def (dockerImageName, dockerImageTag) = dockerImage.tokenize(':')
            String dockerImageSha256 = execAndGetStdOut('docker', 'inspect', '--format=\'{{index .RepoDigests 0}}\'', dockerImageName)

            // docker image info
            if (dockerImageSha256) {
                buildScan.value("docker-image", dockerImageName + "@" + dockerImageSha256)
                buildScan.value("docker-image-sha256", dockerImageSha256)
            } else {
                buildScan.value("docker-image", dockerImage)
            }

            buildScan.value("docker-image-name", dockerImageName)
            buildScan.value("docker-image-tag", dockerImageTag)
        }
    }
})

// Add JVM Major version as tag
Runtime.Version version = Runtime.version()
buildScan.tag("Java ${version.feature()}")

Optional<String> projectUrl = projectUrl()
projectUrl.ifPresent(url -> {
    buildScan.value("Project URL", url)
    buildScan.link("Project Repository", url)
})

// Add more details for CI builds
if (isGitLab()) {
    Optional<String> commitSha = envVariable("CI_COMMIT_SHA")

    // 'git' might not be installed in gitlab docker runners, use the equivalent environment variables
    commitSha.ifPresent(gitCommitId ->
        buildScan.value("Git commit id", gitCommitId))

    envVariable("CI_COMMIT_SHORT_SHA").ifPresent(value -> {
        String name = "Git commit id short"
        String linkLabel = "Git commit id"
        buildScan.value(name, value)

        String server = buildScan.getServer()
        if (server != null) {
            String searchParams = "search.names=" + urlEncode(name) + "&search.values=" + urlEncode(value);
            String url = appendIfMissing(server, "/") + "scans?" + searchParams + "#selection.buildScanB=" + urlEncode("{SCAN_ID}");
            buildScan.link(linkLabel + " build scans", url);
        }
    })
    projectUrl.ifPresent(gitRepo ->
        buildScan.value("Git repository", redactUserInfo(gitRepo)))
    if (commitSha.isPresent() && projectUrl.isPresent()) {
        buildScan.link("GitLab source", projectUrl.get() + "/-/commit/" + commitSha.get())
    }

    // CI builds should have branch name defined in one of two ways
    // see: https://docs.gitlab.com/ee/ci/variables/predefined_variables.html
    Stream.of(
        envVariable("CI_COMMIT_BRANCH"),
        envVariable("CI_MERGE_REQUEST_SOURCE_BRANCH_NAME"))
        .filter(Optional::isPresent)
        .findFirst()
        .map(Optional::get)
        .ifPresent(gitBranchName -> {
            buildScan.tag(gitBranchName)
            buildScan.value("Git branch", gitBranchName)
        })

    // Add Pull Request info to custom data
    envVariable("CI_MERGE_REQUEST_TARGET_BRANCH_NAME").ifPresent(value -> {
        buildScan.value("PR", "true")
        buildScan.value("PR Target Branch", value)
    })
}

// Project URL, try CI ENV var's first, then fallback to POM data.
Optional<String> projectUrl() {
    return Stream.of(
            envVariable("CI_PROJECT_URL"),
            projectUrlFromGit(),
            Optional.ofNullable(project.scm?.url))
        .filter(Optional::isPresent)
        .findFirst()
        .map(Optional::get)
        .map { toWebUrl(it) }
}

Optional<String> projectUrlFromGit() {
    try {
        String url = execAndGetStdOut("git", "config", "--get", "remote.origin.url")
        if (url) {
            return Optional.of(url)
        }
    } catch (RuntimeException e) {
        // ignore
    }
    return Optional.empty();
}

String toWebUrl(String vcsUrl) throws URISyntaxException {
    String gitUrlRegEx = "(?<schema>https?|git|ssh)(?:://(?:\\w+@)?|@)(?<host>.*?)(?:/|:)(?<slug>.*?)(?:\\.git|/)?\$"
    Pattern p = Pattern.compile(gitUrlRegEx)
    Matcher m = p.matcher(vcsUrl)

    if (m.find()) {
        String host = m.group("host")
        String slug = m.group("slug")
        return new URI("https", null, host, -1, "/" + slug, null, null).toString()
    }
    return null
}
