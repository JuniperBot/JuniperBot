package ru.juniperbot.common.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.jmx.export.MBeanExporter;
import org.springframework.jmx.export.annotation.AnnotationJmxAttributeSource;
import org.springframework.jmx.export.assembler.MetadataMBeanInfoAssembler;
import ru.juniperbot.common.support.jmx.JbMetadataNamingStrategy;

@Configuration
public class MBeanConfiguration {

    @Bean
    public AnnotationJmxAttributeSource annotationJmxAttributeSource() {
        return new AnnotationJmxAttributeSource();
    }

    @Bean
    public MetadataMBeanInfoAssembler infoAssembler() {
        MetadataMBeanInfoAssembler assembler = new MetadataMBeanInfoAssembler();
        assembler.setAttributeSource(annotationJmxAttributeSource());
        return assembler;
    }

    @Bean
    public JbMetadataNamingStrategy namingStrategy() {
        JbMetadataNamingStrategy strategy = new JbMetadataNamingStrategy();
        strategy.setAttributeSource(annotationJmxAttributeSource());
        return strategy;
    }

    @Bean
    @Lazy(false)
    public MBeanExporter mBeanExporter() {
        MBeanExporter exporter = new MBeanExporter();
        exporter.setAutodetect(true);
        exporter.setNamingStrategy(namingStrategy());
        exporter.setAssembler(infoAssembler());
        return exporter;
    }
}
