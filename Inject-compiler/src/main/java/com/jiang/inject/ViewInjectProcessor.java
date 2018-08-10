package com.jiang.inject;

import com.google.auto.service.AutoService;
import com.jiang.annotation.BindView;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;

@AutoService(Processor.class)
@SupportedSourceVersion(SourceVersion.RELEASE_7)
public class ViewInjectProcessor extends AbstractProcessor {
    private Map<String, List<VariableInfo>> classMap = new HashMap<>();
    private Map<String, TypeElement> typeElementMap = new HashMap<>();
    private Elements elementUtils;
    private Filer filer;
    private Messager messager;


    @Override
    public synchronized void init(ProcessingEnvironment processingEnvironment) {
        super.init(processingEnvironment);
        elementUtils = processingEnvironment.getElementUtils();
        filer = processingEnvironment.getFiler();
        messager = processingEnvironment.getMessager();
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> annotations=new LinkedHashSet<>();
        annotations.add(BindView.class.getCanonicalName());
        return annotations;
    }

    @Override

    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        sweepAnnotations(roundEnvironment);
        writeToFile();
        return true;
    }

    private void sweepAnnotations(RoundEnvironment roundEnvironment) {
        classMap.clear();
        typeElementMap.clear();
        Set<? extends Element> elements = roundEnvironment.getElementsAnnotatedWith(BindView.class);
        for (Element element : elements) {
            int viewId = element.getAnnotation(BindView.class).value();
            VariableElement variableElement = (VariableElement) element;
            TypeElement typeElement = (TypeElement) variableElement.getEnclosingElement();
            String className = typeElement.getQualifiedName().toString();
            List<VariableInfo> variableInfos = classMap.get(className);
            if (variableInfos == null) {
                variableInfos = new ArrayList<>();
                classMap.put(className, variableInfos);
                typeElementMap.put(className, typeElement);
            }
            VariableInfo variableInfo = new VariableInfo();
            variableInfo.setViewId(viewId);
            variableInfo.setVariableElement(variableElement);
            variableInfos.add(variableInfo);
        }
    }

    private void writeToFile() {
        try {
            for (String className : classMap.keySet()) {
                TypeElement typeElement = typeElementMap.get(className);
                MethodSpec.Builder methodBuilder = MethodSpec.constructorBuilder()
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(ParameterSpec
                                .builder(TypeName.get(typeElement.asType()), "activity")
                                .build());
                List<VariableInfo> variableInfos = classMap.get(className);
                for (VariableInfo variableInfo : variableInfos) {
                    VariableElement variableElement = variableInfo.getVariableElement();
                    String variableName = variableElement.getSimpleName().toString();
                    String variableType = variableElement.asType().toString();
                    methodBuilder.addStatement("activity.$L=($L)activity.findViewById($L)", variableName, variableType, variableInfo.getViewId());
                }
                MethodSpec methodSpec=methodBuilder.build();

                TypeSpec typeSpec = TypeSpec.classBuilder(typeElement.getSimpleName() + "$$ViewBinding")
                        .addModifiers(Modifier.PUBLIC)
                        .addMethod(methodSpec)
                        .build();

                String packageFullName = elementUtils.getPackageOf(typeElement).getQualifiedName().toString();
                JavaFile javaFile = JavaFile.builder(packageFullName, typeSpec)
                        .build();
                javaFile.writeTo(filer);
            }
        } catch (Exception e) {
            messager.printMessage(Diagnostic.Kind.ERROR, e.getMessage());
        }

    }
}
