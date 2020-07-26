package de.waitpermission.more_maven_properties;

import com.intellij.execution.configurations.ParametersList;
import com.intellij.execution.configurations.ParamsGroup;

import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TransformHelpers {
  public static void mutateParametersList(ParametersList parameters,
    UnaryOperator<String> operator) {
    ParametersList original = parameters.clone();
    parameters.clearAll();
    parameters.addAll(original.getParameters().stream().map(operator).toArray(String[]::new));
    for (ParamsGroup group : original.getParamsGroups()) {
      ParamsGroup newGroup = group.clone();
      mutateParametersList(newGroup.getParametersList(), operator);
      parameters.addParamsGroup(newGroup);
    }
  }

  public static String replacePatternWithComputation(Pattern pattern,
    Function<Matcher, String> substitution, String value) {
    Matcher matcher = pattern.matcher(value);
    if (matcher.find()) {
      StringBuilder replaced = new StringBuilder(value.length() * 2);
      int lastEnd = 0;
      do {
        replaced.append(value, lastEnd, matcher.start());
        lastEnd = matcher.end();
        replaced.append(substitution.apply(matcher));
      } while (matcher.find());
      replaced.append(value.substring(lastEnd));
      return replaced.toString();
    }
    else {
      return value;
    }
  }
}
