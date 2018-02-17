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
function Status(lang) {
    var self = this;
    var $pingChart = $.plot("#ping-chart", [], {
        grid: {
            borderColor: '#f3f3f3',
            borderWidth: 1,
            tickColor: '#f3f3f3'
        },
        series: {
            shadowSize: 0
        },
        lines: {
            fill: true
        },
        yaxis: {
            min: -1,
            show: true,
            tickFormatter: function (v) {
                if (v < 0) {
                    return lang.disconnected;
                }
                return v + " " + lang.pingPostfix;
            }
        },
        xaxis: {
            mode: "time",
            timezone: "browser",
            show: true,
            axisLabel: lang.timeLabel
        },
        zoom: {
            interactive: true
        },
        pan: {
            interactive: true
        }
    });


    self.init = function () {
        update();
    };

    function update() {
        fetch(function(data) {
            $pingChart.setData(data);
            $pingChart.setupGrid();
            $pingChart.draw();
        }, function() {
            setTimeout(update, 5000);
        });
    }

    function fetch(doneCallback, alwaysCallback) {
        $.get(contextPath + 'api/ping').done(doneCallback).always(alwaysCallback);
    }

    return self;
}