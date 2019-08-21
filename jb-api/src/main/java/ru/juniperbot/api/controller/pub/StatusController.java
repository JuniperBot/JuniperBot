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
package ru.juniperbot.api.controller.pub;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import ru.juniperbot.api.controller.base.BasePublicRestController;
import ru.juniperbot.common.model.status.StatusDto;

@RestController
public class StatusController extends BasePublicRestController {

    @GetMapping("/health")
    @ResponseBody
    public String getHealth() {
        return "OK";
    }

    @GetMapping("/status")
    @ResponseBody
    public StatusDto get() {
        return gatewayService.getWorkerStatus();
    }
}
