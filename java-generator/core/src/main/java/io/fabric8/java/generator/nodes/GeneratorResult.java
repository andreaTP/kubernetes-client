package io.fabric8.java.generator.nodes;

import java.util.ArrayList;
import java.util.List;

public class GeneratorResult {

  private List<String> topLevelClasses;

  public List<String> getInnerClasses() {
    return innerClasses;
  }

  private List<String> innerClasses;

  public List<String> getTopLevelClasses() {
    return topLevelClasses;
  }

  public GeneratorResult() {
    this.topLevelClasses = new ArrayList<>();
    this.innerClasses = new ArrayList<>();
  }

  public GeneratorResult(List<String> topLevelClasses) {
    this.topLevelClasses = topLevelClasses;
    this.innerClasses = new ArrayList<>();
  }

  public GeneratorResult(List<String> topLevelClasses, List<String> innerClasses) {
    this.topLevelClasses = topLevelClasses;
    this.innerClasses = innerClasses;
  }
}
