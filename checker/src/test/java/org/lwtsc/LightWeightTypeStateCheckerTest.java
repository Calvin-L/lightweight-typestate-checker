package org.lwtsc;

import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized;

import java.io.File;
import java.util.List;

public class LightWeightTypeStateCheckerTest extends CheckerFrameworkPerDirectoryTest {

  public LightWeightTypeStateCheckerTest(List<File> testFiles) {
    super(
        testFiles,
        LightWeightTypeStateChecker.class,
        "",
        "-Anomsgtext");
  }

  @Parameterized.Parameters
  public static String[] getTestDirs() {
    return new String[] { "" };
  }

}
