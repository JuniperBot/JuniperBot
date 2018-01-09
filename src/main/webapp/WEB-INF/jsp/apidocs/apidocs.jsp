<%--
This file is part of JuniperBotJ.

JuniperBotJ is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

JuniperBotJ is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with JuniperBotJ. If not, see <http://www.gnu.org/licenses/>.
--%>
<%@ page contentType="text/html;charset=UTF-8" %>
<%@include file="/WEB-INF/template/include.jsp" %>

<div class="container">
    <div class="row">
        <div class="col-md-4">
            <div class="box box-solid">
                <div class="box-header with-border">
                    <i class="fa fa-list-ul"></i>
                    <h3 class="box-title">Документация</h3>
                </div>
                <div class="box-body">
                    <ul>
                        <li><a href="#introduction">Введение</a></li>
                        <li><a href="#rating-group">Рейтинг участников</a>
                            <ul>
                                <li><a href="#ranking-list">Список пользователей</a></li>
                            </ul>
                        </li>
                    </ul>
                </div>
            </div>
        </div>
    </div>

    <div id="introduction" class="box box-warning">
        <div class="box-header with-border">
            <h3 class="box-title hyperlink">Введение <a href="#introduction"></a></h3>
            <div class="box-tools pull-right">
                <button type="button" class="btn btn-box-tool" data-widget="collapse" data-toggle="tooltip" title="Свернуть">
                    <i class="fa fa-minus"></i>
                </button>
            </div>
        </div>
        <div class="box-body">
            <p>Данный документ предназначен для разработчиков и содержит описание программного интерфейса JuniperBot (далее — API).</p>
            <p>API обеспецивает возможность автоматизированного получения данных от JuniperBot.
                С помощью API внешние приложения могут получать сведения о пользователях серверов, их ранг, уровень и так далее.</p>
            <p>Доступ к внешнему API JuniperBot расположен по адресу <a href="#">https://juniperbot.ru/api/</a> и реализован как REST-интрефейс.
                Он формируется из совокупности REST-интерфейсов отдельных ресурсов и их действий.
                Таким образом, запросы следует отправлять по адресам вида <a href="#">https://juniperbot.ru/api/resource/action</a>, где:</p>
            <ul>
                <li><code>resource</code> - название ресурса;</li>
                <li><code>action</code> - название действия над этим ресурсом.</li>
            </ul>
            <p>Далее документация будет описывать лишь пути к ресурсам и действиям <code>/resource/action[/optionalPath][?param=value]</code>. Ответы всех действий будут возвращены в формате JSON.</p>
        </div>
    </div>

    <h1 id="rating-group" class="hyperlink">Рейтинг участников <a href="#rating-group"></a></h1>

    <div id="ranking-list" class="box box-warning">
        <div class="box-header with-border">
            <h3 class="box-title hyperlink">Список пользователей <a href="#ranking-list"></a></h3>
            <div class="box-tools pull-right">
                <button type="button" class="btn btn-box-tool" data-widget="collapse" data-toggle="tooltip" title="Свернуть">
                    <i class="fa fa-minus"></i>
                </button>
            </div>
        </div>
        <div class="box-body">
            <h4 class="rest-resource-url"><span class="rest-resource-method">GET</span> <code>/ranking/list/:guildId</code></h4>
            <p>Позволяет получить сведение о пользователях сервера, их идентификаторы, ранги, уровни и так далее.</p>

            <h4 class="text-bold">Параметры</h4>
            <ul>
                <li>
                    <p>Обязательные:</p>
                    <ul>
                        <li><code>:guildId</code> - идентификатор сервера (snowflake)</li>
                    </ul>
                </li>
            </ul>

            <h4 class="text-bold">Успешный ответ</h4>
            Успешный ответ на запрос (<code>HTTP 200</code>) вернет JSON-массив объектов следующей структуры:
            <table class="table table-striped table-hover table-responsive">
                <thead>
                <tr>
                    <th>Поле</th>
                    <th>Тип</th>
                    <th>Описание</th>
                </tr>
                </thead>
                <tbody>
                <tr>
                    <td>id</td>
                    <td>snowflake</td>
                    <td>Идентификатор пользователя в Discord</td>
                </tr>
                <tr>
                    <td>name</td>
                    <td>string</td>
                    <td>Имя пользователя в Discord</td>
                </tr>
                <tr>
                    <td>discriminator</td>
                    <td>string</td>
                    <td>Четырехзначный тэг пользователя в Discord</td>
                </tr>
                <tr>
                    <td>nick</td>
                    <td>string</td>
                    <td>Имя пользователя на сервере</td>
                </tr>
                <tr>
                    <td>avatarUrl</td>
                    <td>string</td>
                    <td>Ссылка на аватар пользователя</td>
                </tr>
                <tr>
                    <td>level</td>
                    <td>integer</td>
                    <td>Уровень пользователя на сервере</td>
                </tr>
                <tr>
                    <td>remainingExp</td>
                    <td>integer</td>
                    <td>Опыт пользователя <span class="text-bold">на текущем уровне</span></td>
                </tr>
                <tr>
                    <td>levelExp</td>
                    <td>integer</td>
                    <td>Опыт, необходимый для достижения следующего уровня</td>
                </tr>
                <tr>
                    <td>totalExp</td>
                    <td>integer</td>
                    <td>Суммарный заработанный пользователем опыт</td>
                </tr>
                <tr>
                    <td>rank</td>
                    <td>integer</td>
                    <td>Ранг пользователя на сервере</td>
                </tr>
                <tr>
                    <td>pct</td>
                    <td>integer</td>
                    <td>Процент прогресса пользователя на текущем уровне</td>
                </tr>
                </tbody>
            </table>

            <h4 class="text-bold">Пример запроса и ответа</h4>
            <ul>
                <li>
                    <p class="text-bold">Запрос</p>
                    <p>Следующий запрос: <code>GET https://juniperbot.ru/api/ranking/list/310850506107125760</code></p>
                </li>
                <li>
                    <p class="text-bold">Ответ</p>
                    <pre>[
  {
    "id":"247734710682255361",
    "name":"Карамелька",
    "discriminator":"1453",
    "nick":"Карамелька",
    "avatarUrl":"https://cdn.discordapp.com/avatars/247734710682255361/0a3618a74a3914aaadb18955f3d2cdd2.png",
    "level":0,
    "remainingExp":76,
    "levelExp":100,
    "totalExp":76,
    "rank":1,
    "pct":76
  }
]</pre>
                </li>
            </ul>
        </div>
    </div>
</div>
