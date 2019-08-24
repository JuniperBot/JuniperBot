/*
 * This file is part of JuniperBot.
 *
 * JuniperBot is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * JuniperBot is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with JuniperBot. If not, see <http://www.gnu.org/licenses/>.
 */
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
