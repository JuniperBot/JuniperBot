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
package ru.caramel.juniperbot.web.controller.pub;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.common.TextFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.caramel.juniperbot.web.controller.base.BasePublicRestController;
import ru.caramel.juniperbot.web.dao.StatusDao;
import ru.caramel.juniperbot.web.dto.StatusDto;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.*;

@RestController
public class StatusController extends BasePublicRestController {

    @Autowired
    private StatusDao statusDao;

    private final CollectorRegistry registry;

    public StatusController() {
        this.registry = CollectorRegistry.defaultRegistry;
    }

    @GetMapping("/health")
    @ResponseBody
    public String getHealth() {
        return "OK";
    }

    @GetMapping(value = "metrics", produces = TextFormat.CONTENT_TYPE_004)
    public ResponseEntity<String> getMetrics(@RequestParam(name = "name[]", required = false) String[] includedParam)
            throws IOException {
        Set<String> params = includedParam == null ? Collections.emptySet() : new HashSet<>(Arrays.asList(includedParam));
        try (Writer writer = new StringWriter()) {
            TextFormat.write004(writer, this.registry.filteredMetricFamilySamples(params));
            writer.flush();
            return new ResponseEntity<>(writer.toString(), HttpStatus.OK);
        }
    }

    @GetMapping("/status")
    @ResponseBody
    public StatusDto get() {
        return statusDao.get();
    }
}
