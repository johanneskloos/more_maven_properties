package de.waitpermission.more_maven_properties;

import com.intellij.execution.JUnitPatcher;
import com.intellij.execution.configurations.JavaParameters;
import com.intellij.openapi.module.Module;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.idea.maven.model.MavenPlugin;
import org.jetbrains.idea.maven.model.MavenPlugin.Execution;
import org.jetbrains.idea.maven.project.MavenProject;
import org.jetbrains.idea.maven.project.MavenProjectsManager;

import java.util.List;

public class MoreMavenPropertiesJUnitPatcher extends JUnitPatcher {
  @Override
  public void patchJavaParameters(@Nullable Module module,
    JavaParameters javaParameters) {
    if (module == null) return;


    MavenProjectsManager mavenProjectsManager =
      MavenProjectsManager.getInstance(module.getProject());
    if (mavenProjectsManager == null) return;

    MavenProject mavenProject = mavenProjectsManager.findProject(module);
    if (mavenProject == null) return;

    if (!hasDependencyPropertiesGoalEnabled(mavenProject)) return;

    TransformHelpers.mutateParametersList(javaParameters.getVMParametersList(),
      new MavenArtifactSubstitutor(mavenProject)::substitute);
  }

  private boolean hasDependencyPropertiesGoalEnabled(MavenProject mavenProject) {
    MavenPlugin plugin =
      mavenProject.findPlugin("org.apache.maven.plugins", "maven-dependency-plugin");
    List<Execution> executions = plugin.getExecutions();
    if (executions == null) return false;
    return executions.stream().anyMatch(execution -> execution.getGoals().contains("properties"));
  }

}
