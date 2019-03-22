/*
 * This file is part of JuniperBotJ.
 *
 * JuniperBotJ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * JuniperBotJ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with JuniperBotJ. If not, see <http://www.gnu.org/licenses/>.
 */
package ru.caramel.juniperbot.core.configuration;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.jmx.JmxReporter;
import com.codahale.metrics.jvm.JvmAttributeGaugeSet;
import com.ryantenney.metrics.spring.config.annotation.EnableMetrics;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.task.TaskExecutor;
import org.springframework.jmx.export.MBeanExporter;
import org.springframework.jmx.export.annotation.AnnotationJmxAttributeSource;
import org.springframework.jmx.export.assembler.MetadataMBeanInfoAssembler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import ru.caramel.juniperbot.core.support.jmx.JbMetadataNamingStrategy;
import ru.caramel.juniperbot.core.support.jmx.ThreadPoolTaskExecutorMBean;

import java.util.concurrent.TimeUnit;

@EnableMetrics
@Configuration
public class MetricsConfiguration {

    @Autowired
    private MetricRegistry metricRegistry;

    @Autowired
    @Qualifier("executor")
    private TaskExecutor executor;

    @Autowired
    @Qualifier("eventManagerExecutor")
    private TaskExecutor eventManagerExecutor;

    @Bean(destroyMethod = "stop")
    public ConsoleReporter consoleReporter() {
        ConsoleReporter reporter = ConsoleReporter.forRegistry(metricRegistry).build();
        reporter.start(1, TimeUnit.DAYS);
        return reporter;
    }

    @Bean(destroyMethod = "stop")
    public JmxReporter jmxReporter() {
        JmxReporter reporter = JmxReporter.forRegistry(metricRegistry).build();
        reporter.start();
        return reporter;
    }

    @Bean
    public JvmAttributeGaugeSet jvmGauge() {
        JvmAttributeGaugeSet jvmMetrics = new JvmAttributeGaugeSet();
        metricRegistry.register("jvm", jvmMetrics);
        return jvmMetrics;
    }

    @Bean
    public AnnotationJmxAttributeSource annotationJmxAttributeSource() {
        return new AnnotationJmxAttributeSource();
    }

    @Bean
    public JbMetadataNamingStrategy namingStrategy() {
        JbMetadataNamingStrategy strategy = new JbMetadataNamingStrategy();
        strategy.setAttributeSource(annotationJmxAttributeSource());
        return strategy;
    }

    @Bean
    public MetadataMBeanInfoAssembler infoAssembler() {
        MetadataMBeanInfoAssembler assembler = new MetadataMBeanInfoAssembler();
        assembler.setAttributeSource(annotationJmxAttributeSource());
        return assembler;
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

    @Bean
    public ThreadPoolTaskExecutorMBean taskExecutorMBean() {
        return new ThreadPoolTaskExecutorMBean("Spring TaskExecutor", (ThreadPoolTaskExecutor) executor);
    }

    @Bean
    public ThreadPoolTaskExecutorMBean eventTaskExecutorMBean() {
        return new ThreadPoolTaskExecutorMBean("Event TaskExecutor", (ThreadPoolTaskExecutor) eventManagerExecutor);
    }
}
