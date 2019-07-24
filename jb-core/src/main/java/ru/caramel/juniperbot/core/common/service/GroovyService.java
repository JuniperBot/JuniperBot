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
package ru.caramel.juniperbot.core.common.service;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.customizers.ImportCustomizer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

@Service
public class GroovyService {

    @Autowired
    private ApplicationContext context;

    @Autowired
    private DiscordService discordService;

    private CompilerConfiguration configuration;

    private Binding sharedData;

    @PostConstruct
    public void init() {
        ImportCustomizer importCustomizer = new ImportCustomizer();
        importCustomizer.addStarImports("net.dv8tion.jda.core.entities");

        configuration = new CompilerConfiguration();
        configuration.addCompilationCustomizers(importCustomizer);

        sharedData = new Binding();
        sharedData.setProperty("ctx", context);
    }

    public GroovyShell createShell() {
        return new GroovyShell(sharedData, configuration);
    }
}
