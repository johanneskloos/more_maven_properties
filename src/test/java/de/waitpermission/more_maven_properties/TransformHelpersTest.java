package de.waitpermission.more_maven_properties;

import com.intellij.execution.configurations.ParametersList;
import com.intellij.execution.configurations.ParamsGroup;
import junit.framework.TestCase;
import org.junit.Assert;

import java.util.Arrays;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TransformHelpersTest extends TestCase {
  public void testMutateParametersList() {
    ParametersList parametersList = new ParametersList();
    ParamsGroup group = parametersList.addParamsGroup("firstGroup");
    group.addParameter("first");
    group.addParameter("second");
    parametersList.add("third");
    parametersList.add("fourth");
    parametersList.add("fifth");
    group = parametersList.addParamsGroup("secondGroup");
    group = group.getParametersList().addParamsGroup("innerGroup");
    group.addParameter("sixth");
    parametersList.add("seventh");

    TransformHelpers.mutateParametersList(parametersList, String::toUpperCase);

    Assert.assertEquals(
      Arrays.asList(
        "THIRD",
        "FOURTH",
        "FIFTH",
        "SEVENTH",
        "FIRST",
        "SECOND",
        "SIXTH"
      ),
      parametersList.getList()
    );
  }

  public void testReplacePatternWithComputation() {
    Pattern pattern = Pattern.compile("([0-9]+)");
    Function<Matcher, String> hexifier = matcher -> Integer
      .toHexString(Integer.parseInt(matcher.group(1)));
    String sample = "abc123e567f";

    Assert.assertEquals("abc7be237f",
      TransformHelpers.replacePatternWithComputation(pattern, hexifier, sample));
  }
}