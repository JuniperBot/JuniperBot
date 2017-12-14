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


<div class="box box-warning">
    <div id="ranking-container" class="box-body"></div>
</div>
<script type="text/javascript" src="<c:url value="/resources/js/ranking.js?v=${buildTimestamp}"/>"></script>
<script type="text/javascript">
    $(document).ready(function () {
        var ranking = new Ranking();
        ranking.init();
    });
</script>

<div id="update-level-modal" class="modal bootstrap-dialog type-warning fade" tabindex="-1" role="dialog" aria-labelledby="update-level-modal-label">
    <div class="modal-dialog" role="document">
        <div class="modal-content">
            <div class="modal-header">
                <div class="bootstrap-dialog-header">
                    <div class="bootstrap-dialog-close-button">
                        <button class="close" aria-label="close">×</button></div>
                    <div id="update-level-modal-label" class="bootstrap-dialog-title" id="e3384762-1886-42af-b99a-1b37d8134022_title">Сброс прогресса пользователя "Карамелька"</div>
                </div>
            </div>
            <div class="modal-body">
                <p>Укажите новый уровень пользователя от 0 до 999 в поле ниже и нажмите кнопку "Продолжить":</p>
            </div>
            <div class="modal-footer">
                <div class="col-md-9">
                    <div class="row">
                        <div class="form-group col-md-6">
                            <input id="update-level-value" type="number" max="999" min="1" class="form-control" placeholder="Уровень пользователя" />
                        </div>
                    </div>
                </div>
                <div class="col-md-2">
                    <button id="update-level-button" type="button" class="btn btn-warning" style="width: 110px;">
                        <span id="update-level-text">Обновить</span>
                        <span id="update-level-spinner" style="display: none;"><i class="fa fa-circle-o-notch fa-spin" style="font-size:18px;"></i></span>
                    </button>
                </div>
            </div>
        </div>
    </div>
</div>