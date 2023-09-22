package org.lwtsc;

import org.checkerframework.framework.type.GenericAnnotatedTypeFactory;
import org.checkerframework.framework.type.MostlyNoElementQualifierHierarchy;
import org.checkerframework.framework.util.QualifierKind;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.util.Elements;
import java.lang.annotation.Annotation;
import java.util.Collection;

class LightWeightTypeStateQualifierHierarchy extends MostlyNoElementQualifierHierarchy {

  private final TypeStateConverter converter;

  public LightWeightTypeStateQualifierHierarchy(
      Collection<Class<? extends Annotation>> qualifierClasses,
      Elements elements,
      GenericAnnotatedTypeFactory<?, ?, ?, ?> atypeFactory,
      TypeStateConverter converter) {
    super(qualifierClasses, elements, atypeFactory);
    this.converter = converter;
  }

  @Override
  protected boolean isSubtypeWithElements(
      AnnotationMirror subAnnotationMirror,
      QualifierKind subQualifierKind,
      AnnotationMirror superAnnotationMirror,
      QualifierKind superQualifierKind) {
    StateIDSet sub = converter.fromAnnotationMirror(subAnnotationMirror);
    StateIDSet sup = converter.fromAnnotationMirror(superAnnotationMirror);
    return sup.containsAll(sub);
  }

  @Override
  protected AnnotationMirror leastUpperBoundWithElements(
      AnnotationMirror annotationMirror1,
      QualifierKind qualifierKind1,
      AnnotationMirror annotationMirror2,
      QualifierKind qualifierKind2,
      QualifierKind glbKind) {
    StateIDSet t1 = converter.fromAnnotationMirror(annotationMirror1);
    StateIDSet t2 = converter.fromAnnotationMirror(annotationMirror2);
    return converter.toAnnotationMirror(t1.union(t2));
  }

  @Override
  protected AnnotationMirror greatestLowerBoundWithElements(
      AnnotationMirror annotationMirror1,
      QualifierKind qualifierKind1,
      AnnotationMirror annotationMirror2,
      QualifierKind qualifierKind2,
      QualifierKind lubKind) {
    StateIDSet t1 = converter.fromAnnotationMirror(annotationMirror1);
    StateIDSet t2 = converter.fromAnnotationMirror(annotationMirror2);
    return converter.toAnnotationMirror(t1.intersection(t2));
  }

}
