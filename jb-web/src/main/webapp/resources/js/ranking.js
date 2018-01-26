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
function Ranking(lang) {
    var self = this;
    var overlayContent = '<div class="overlay"><i class="fa fa-refresh fa-spin"></i></div>';

    self.container = $('#ranking-container');
    self.reloading = false;
    self.updateId = null;

    self.pageSize = 100;
    self.pageNum = 1;
    self.search = null;

    var $updateModal = $("#update-level-modal");
    var $updateTitle = $('#update-level-modal-label');
    var $updateInput = $('#update-level-value');
    var $updateGroup = $updateInput.closest('.form-group');

    var $updateButton = $('#update-level-button');
    var $updateText = $('#update-level-text');
    var $updateSpinner = $('#update-level-spinner');

    var $syncButton = $('#ranking-sync-button');

    var $importButton = $('#ranking-import-button');
    var $resetButton = $('#ranking-reset-button');

    var $searchInput = $("#ranking-search-input");
    var $searchButton = $("#ranking-search-button");

    var defaultPagingOptions = {
        total: 0,
        page: 1,
        maxVisible: 5,
        leaps: true
    };

    self.pagination = $('#ranking-pagination')
        .bootpag(defaultPagingOptions)
        .on("page", function(event, num){
        self.pageNum = num;
        self.reload();
    });

    self.setPagination = function(options) {
        self.pagination.bootpag($.extend(defaultPagingOptions, options));
    };

    var rewardsTable = $('#rewards-table').DataTable({
        'paging': false,
        'searching': false,
        "scrollY": "300px",
        "bInfo": false,
        "language": {
            "zeroRecords": lang.noRoles,
            "emptyTable": lang.noRoles,
            "aria": {
                "sortAscending": ": " + lang.sortAsc,
                "sortDescending": ": " + lang.sortDesc
            }
        },
        "order": [[1, "desc"]],
        "columnDefs": [
            {"width": "150px", "targets": 1}
        ],
        "columns": [
            null,
            {"orderDataType": "dom-text-numeric"}
        ]
    });

    self.init = function () {
        self.reload();
    };

    self.doSearch = function() {
        self.search = $searchInput.val();
        self.reload();
    };

    self.reload = function () {
        if (self.reloading) {
            return;
        }
        self.reloading = true;
        self.container.html(overlayContent);
        $.post(contextPath + 'ranking/list/' + serverId + '/count', {search: self.search})
            .done(function (data) {
                var totalPages = Math.ceil(data / self.pageSize) || 1;
                self.pageNum = totalPages > self.pageNum ? self.pageNum : totalPages;
                self.setPagination({total: totalPages, page: self.pageNum});
                $.post(contextPath + 'ranking/list/' + serverId, {
                    page: self.pageNum - 1,
                    pageSize: self.pageSize,
                    search: self.search
                })
                    .done(function (data) {
                        self.container.hide().html(data).fadeIn('fast');
                        bindActions();
                    })
                    .fail(function () {
                        self.container.html(lang.somethingIsWrong);
                    })
                    .always(function () {
                        self.reloading = false;
                    });
            })
            .fail(function () {
                BootstrapDialog.warning(lang.somethingIsWrong);
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

    self.resetRank = function (id, name) {
        BootstrapDialog.show({
            title: lang.resetModalTitle.replace('{name}', name),
            type: BootstrapDialog.TYPE_WARNING,
            message: lang.resetModalContent.replace('{name}', name),
            spinicon: 'fa fa-circle-o-notch',
            buttons: [{
                label: lang.resetButtonText,
                cssClass: 'btn-warning',
                autospin: true,
                action: function (dialogRef) {
                    dialogRef.enableButtons(false);
                    dialogRef.setClosable(false);
                    update(id, 0, function () {
                        dialogRef.close();
                    });
                }
            }, {
                label: lang.closeButtonText,
                action: function (dialogRef) {
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
            update(self.updateId, level, function () {
                block(false);
            });
        }
    });

    $syncButton.click(self.reload);

    $importButton.click(function () {
        BootstrapDialog.show({
            title: lang.importModalTitle,
            type: BootstrapDialog.TYPE_WARNING,
            message: $("#import-content").clone().removeClass('hidden'),
            spinicon: 'fa fa-circle-o-notch',
            buttons: [{
                label: lang.importButtonText,
                cssClass: 'btn-info',
                autospin: true,
                action: function (dialogRef) {
                    dialogRef.enableButtons(false);
                    dialogRef.setClosable(false);
                    syncMee6(function () {
                        dialogRef.close();
                    });
                }
            }, {
                label: lang.closeButtonText,
                action: function (dialogRef) {
                    dialogRef.close();
                }
            }]
        });
    });

    $resetButton.click(function () {
        BootstrapDialog.show({
            title: lang.resetAllModalTitle,
            type: BootstrapDialog.TYPE_WARNING,
            message: lang.resetAllModalContent,
            spinicon: 'fa fa-circle-o-notch',
            buttons: [{
                label: lang.resetAllButtonText,
                cssClass: 'btn-warning',
                autospin: true,
                action: function (dialogRef) {
                    dialogRef.enableButtons(false);
                    dialogRef.setClosable(false);
                    resetAll(function () {
                        dialogRef.close();
                    });
                }
            }, {
                label: lang.closeButtonText,
                action: function (dialogRef) {
                    dialogRef.close();
                }
            }]
        });
    });

    self.setLevel = function (id, name, currentLevel) {
        self.updateId = id;
        $updateTitle.text(lang.updateModalTitle.replace("{name}", name));
        $updateInput.val(currentLevel);
        block(false);
        $updateModal.modal();
    };

    function block(block) {
        $updateButton.prop('disabled', block);
        $updateInput.prop('disabled', block);
        (block ? $updateText : $updateSpinner).hide();
        (block ? $updateSpinner : $updateText).show();
    }

    function update(userId, level, callback) {
        $.post(contextPath + 'ranking/update/' + serverId, {userId: userId, level: level})
            .done(function () {
                $updateModal.modal('hide');
                self.reload();
            })
            .fail(function () {
                BootstrapDialog.warning(lang.somethingIsWrong);
            })
            .always(callback);
    }

    function syncMee6(callback) {
        $.post(contextPath + 'ranking/syncMee6/' + serverId)
            .done(function () {
                self.reload();
            })
            .fail(function () {
                BootstrapDialog.warning(lang.somethingIsWrong);
            })
            .always(callback);
    }

    function resetAll(callback) {
        $.post(contextPath + 'ranking/resetAll/' + serverId)
            .done(function () {
                self.reload();
            })
            .fail(function () {
                BootstrapDialog.warning(lang.somethingIsWrong);
            })
            .always(callback);
    }

    $('.level-input').on('keydown keyup', function (e) {
        if (e.keyCode != 46 && e.keyCode != 8 && $(this).val()) {
            if ($(this).val() > 999) {
                e.preventDefault();
                $(this).val(999);
            }
            if ($(this).val() < 0) {
                e.preventDefault();
                $(this).val(0);
            }
        }
    });

    function updateBackgroundImage() {
        var $bg = $('.widget-server-background');
        $bg.height($bg.parent().height());
    }

    $(window).resize(updateBackgroundImage);
    updateBackgroundImage();

    $searchInput.on('keypress', function (e) {
        var keyCode = e.keyCode || e.which;
        if (keyCode === 13) {
            e.preventDefault();
            self.doSearch();
            return false;
        }
    });
    $searchButton.on('click', self.doSearch);
}