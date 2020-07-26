package de.waitpermission.more_maven_properties;

import junit.framework.TestCase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.idea.maven.model.MavenArtifact;
import org.jetbrains.idea.maven.project.MavenProject;
import org.junit.Assert;
import org.mockito.Mockito;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;

@SuppressWarnings("SameParameterValue")
public class MavenArtifactSubstitutorTest extends TestCase {
  private static final File LOCAL_REPOSITORY = new File("repo");

  @NotNull
  private static MavenArtifact mockArtifact(String groupId, String artifactId, String version,
    String classifier) {
    String resolvedFileName;
    if (classifier == null) {
      resolvedFileName = String.format("/%s.%s-%s.jar", groupId, artifactId, version);
    }
    else {
      resolvedFileName = String
        .format("/%s.%s-%s-%s.jar", groupId, artifactId, version, classifier);
    }

    MavenArtifact mavenArtifact = new MavenArtifact(groupId, artifactId, version,
      version, "jar", classifier, "compile", false, null,
      new File(resolvedFileName), LOCAL_REPOSITORY, true, false);

    MavenArtifact mockedMavenArtifact = Mockito.spy(mavenArtifact);
    Mockito.doReturn(true).when(mockedMavenArtifact).isResolved();

    return mockedMavenArtifact;
  }

  @NotNull
  private static MavenArtifact mockNonexistentArtifact(String groupId, String artifactId,
    String version, String classifier) {
    return new MavenArtifact(groupId, artifactId, version,
      version, "jar", classifier, "compile", false, null,
      null, LOCAL_REPOSITORY, false, false);
  }

  @NotNull
  private static MavenProject setUpMockedProject() {
    MavenProject mavenProjectMock = Mockito.mock(MavenProject.class);
    List<MavenArtifact> artifacts1 = Arrays.asList(
      mockArtifact("com.example", "dependency1", "1.0", null),
      mockArtifact("com.example", "dependency1", "1.0", "jar-with-dependencies"),
      mockArtifact("com.example", "dependency1", "1.1", null),
      mockArtifact("com.example", "dependency1", "1.1", "jar-with-dependencies"),
      mockArtifact("com.example", "dependency1", "1.2", null),
      mockArtifact("com.example", "dependency1", "1.2.0", "jar-with-dependencies")
    );
    Mockito.when(mavenProjectMock.findDependencies("com.example", "dependency1"))
           .thenReturn(artifacts1);
    List<MavenArtifact> artifacts2 = Arrays.asList(
      mockArtifact("com.example", "dependency2", "20190717", null),
      mockArtifact("com.example", "dependency2", "20200101", null)
    );
    Mockito.when(mavenProjectMock.findDependencies("com.example", "dependency2"))
           .thenReturn(artifacts2);
    List<MavenArtifact> artifacts3 = Arrays.asList(
      mockArtifact("org.example", "dependency", "0.1-SNAPSHOT", "shaded"),
      mockNonexistentArtifact("org.example", "dependency", "0.1-SNAPSHOT", "shaded")
    );
    Mockito.when(mavenProjectMock.findDependencies("org.example", "dependency"))
           .thenReturn(artifacts3);
    return mavenProjectMock;
  }

  @NotNull
  private Matcher makeMatcher(String s) {
    Matcher matcher = MavenArtifactSubstitutor.MAVEN_DEPENDENCY_PATTERN.matcher("@{" + s + "}");
    Assert.assertTrue(matcher.find());
    return matcher;
  }

  public void testApply() {
    MavenArtifactSubstitutor substitutor = new MavenArtifactSubstitutor(setUpMockedProject());

    Assert.assertEquals("/com.example.dependency1-1.2.jar",
      substitutor.apply(makeMatcher("com.example:dependency1:jar")));
    Assert.assertEquals("/com.example.dependency1-1.2.0-jar-with-dependencies.jar",
      substitutor.apply(makeMatcher("com.example:dependency1:jar:jar-with-dependencies")));
    Assert.assertEquals("@{com.example:dependency1:jar:shaded}",
      substitutor.apply(makeMatcher("com.example:dependency1:jar:shaded")));
    Assert.assertEquals("/com.example.dependency2-20200101.jar",
      substitutor.apply(makeMatcher("com.example:dependency2:jar")));
    Assert.assertEquals("@{org.example:dependency:jar}",
      substitutor.apply(makeMatcher("org.example:dependency:jar")));
    Assert.assertEquals("/org.example.dependency-0.1-SNAPSHOT-shaded.jar",
      substitutor.apply(makeMatcher("org.example:dependency:jar:shaded")));
  }

  public void testSubstitute() {
    MavenArtifactSubstitutor substituter = new MavenArtifactSubstitutor(setUpMockedProject());

    Assert.assertEquals("blah", substituter.substitute("blah"));
    Assert.assertEquals("@{nonexistant}", substituter.substitute("@{nonmatching}"));
    Assert.assertEquals("@{nonmatching:take2}", substituter.substitute("@{nonmatching:take2}"));
    Assert.assertEquals("@{nonmatching:take3:whatever}",
      substituter.substitute("@{nonmatching:take3:whatever}"));
    Assert.assertEquals("/org.example.dependency-0.1-SNAPSHOT-shaded.jar",
      substituter.substitute("@{org.example:dependency:jar:shaded}"));
    Assert.assertEquals("some text /org.example.dependency-0.1-SNAPSHOT-shaded.jarmore text",
      substituter.substitute("some text @{org.example:dependency:jar:shaded}more text"));
    Assert.assertEquals(
      "some text /org.example.dependency-0.1-SNAPSHOT-shaded.jar/org.example.dependency-0" +
      ".1-SNAPSHOT-shaded.jarmore text",
      substituter.substitute(
        "some text @{org.example:dependency:jar:shaded}@{org.example:dependency:jar:shaded}more " +
        "text"));
    Assert.assertEquals(
      "some text /org.example.dependency-0.1-SNAPSHOT-shaded.jar!/org.example.dependency-0" +
      ".1-SNAPSHOT-shaded.jarmore text",
      substituter.substitute(
        "some text @{org.example:dependency:jar:shaded}!@{org.example:dependency:jar:shaded}more " +
        "text"));
  }
}