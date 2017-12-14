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
function Ranking() {
    var self = this;
    var overlayContent = '<div class="overlay"><i class="fa fa-refresh fa-spin"></i></div>';

    self.container = $('#ranking-container');
    self.reloading = false;
    self.updateId = null;

    var $updateModal = $("#update-level-modal");
    var $updateTitle = $('#update-level-modal-label');
    var $updateInput = $('#update-level-value');
    var $updateGroup = $updateInput.closest('.form-group');
    var $updateButton = $('#update-level-button');
    var $updateText = $('#update-level-text');
    var $updateSpinner = $('#update-level-spinner');


    self.init = function () {
        self.reload();
    };

    self.reload = function() {
        if (self.reloading) {
            return;
        }
        self.reloading = true;
        self.container.html(overlayContent);
        $.post(contextPath + 'ranking/list/' + serverId)
            .done(function (data) {
                self.container.hide().html(data).fadeIn('fast');
                bindActions();
            })
            .fail(function () {
                self.container.html('Упс! Что-то пошло не так!');
            })
            .always(function () {
                self.reloading = false;
            });
    };

    function bindActions() {

        $(".update-level-item").unbind("click.action").bind("click.action", function () {
            var item = $(this).closest('.list-group-item');
            if (item.data('id')) {
                self.setLevel(item.data('id'), item.data('name'), item.data('level'));
            }
        });

        $(".reset-level-item").unbind("click.action").bind("click.action", function () {
            var item = $(this).closest('.list-group-item');
            if (item.data('id')) {
                self.resetRank(item.data('id'), item.data('name'));
            }
        });
    }

    self.resetRank = function(id, name) {
        BootstrapDialog.show({
            title: 'Сброс прогресса пользователя "' + name + '"',
            type: BootstrapDialog.TYPE_WARNING,
            message: 'Вы уверены что хотите сбросить прогресс пользователю "' + name + '"?',
            spinicon: 'fa fa-circle-o-notch',
            buttons: [{
                label: 'Сбросить',
                cssClass: 'btn-warning',
                autospin: true,
                action: function(dialogRef){
                    dialogRef.enableButtons(false);
                    dialogRef.setClosable(false);
                    reset(id, function() {
                        dialogRef.close();
                    });
                }
            }, {
                label: 'Закрыть',
                action: function(dialogRef){
                    dialogRef.close();
                }
            }]
        });
    };

    $updateButton.click(function () {
        var level = $updateInput.val().trim();
        if (!/^([0-9]+)$/.test(level) || level > 999 || level < 0) {
            $updateGroup.addClass('has-error');
            $updateInput.focus();
            valid = false;
        } else {
            block(true);
            update(self.updateId, level, function() {
                block(false);
            });
        }
    });

    function block(block) {
        $updateButton.prop('disabled', block);
        $updateInput.prop('disabled', block);
        (block ? $updateText : $updateSpinner).hide();
        (block ? $updateSpinner : $updateText).show();
    }

    self.setLevel = function(id, name, currentLevel) {
        self.updateId = id;
        $updateTitle.text('Обновление уровня пользователя "' + name + '"');
        $updateInput.val(currentLevel);
        block(false);
        $updateModal.modal();
    };

    function reset(userId, callback) {
        $.post(contextPath + 'ranking/reset/' + serverId, {userId: userId})
            .done(self.reload)
            .fail(function () {
                BootstrapDialog.warning('Упс! Что-то пошло не так!');
            })
            .always(callback);
    }

    function update(userId, level, callback) {
        $.post(contextPath + 'ranking/update/' + serverId, {userId: userId, level: level})
            .done(function() {
                $updateModal.modal('toggle');
                self.reload();
            })
            .fail(function () {
                BootstrapDialog.warning('Упс! Что-то пошло не так!');
            })
            .always(callback);
    }
}