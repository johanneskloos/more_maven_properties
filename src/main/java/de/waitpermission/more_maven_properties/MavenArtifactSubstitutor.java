package de.waitpermission.more_maven_properties;

import org.apache.maven.artifact.versioning.ComparableVersion;
import org.jetbrains.idea.maven.model.MavenArtifact;
import org.jetbrains.idea.maven.project.MavenProject;

import java.io.File;
import java.util.Objects;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MavenArtifactSubstitutor implements Function<Matcher, String> {
  public static final Pattern MAVEN_DEPENDENCY_PATTERN =
    Pattern.compile("@\\{([^:]*?):([^:]*?):([^:]*?)(?::([^:]*?))?}");

  private final MavenProject mavenProject;

  public MavenArtifactSubstitutor(MavenProject mavenProject) {
    this.mavenProject = mavenProject;
  }

  private static int versionSorter(MavenArtifact artifact1, MavenArtifact artifact2) {
    ComparableVersion version1 = new ComparableVersion(artifact1.getVersion());
    ComparableVersion version2 = new ComparableVersion(artifact2.getVersion());
    return version2.compareTo(version1);
  }

  @Override
  public String apply(Matcher matcher) {
    String expectedType = matcher.group(3);
    String expectedClassifier = matcher.group(4);
    return mavenProject.findDependencies(matcher.group(1), matcher.group(2))
                       .stream()
                       .filter(MavenArtifact::isResolved)
                       .filter(dep -> expectedType.equals(dep.getType()))
                       .filter(dep -> Objects.equals(expectedClassifier, dep.getClassifier()))
                       .min(MavenArtifactSubstitutor::versionSorter)
                       .map(MavenArtifact::getFile)
                       .map(File::getAbsolutePath)
                       .orElse(matcher.group());
  }

  public String substitute(String value) {
    return TransformHelpers.replacePatternWithComputation(MAVEN_DEPENDENCY_PATTERN,
      this, value);
  }
}
