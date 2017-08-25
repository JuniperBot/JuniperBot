
function VkConnector() {
    var self = this;
    var $connect = $("#vk-connect-button");
    var $modal = $("#vk-connect-modal");
    var $button = $('#vk-create-button');
    var $codeInput = $("#vk-confirmation-code");
    var $codeGroup = $codeInput.closest('.form-group');
    var $nameInput = $("#vk-connection-name");
    var $nameGroup = $nameInput.closest('.form-group');


    var $connectText = $('#vk-connect-text');
    var $connectSpinner = $('#vk-connect-spinner');

    var $step1Block = $('#vk-first-step');
    var $step2Block = $('#vk-second-step');
    var $modalFooter = $modal.find(".modal-footer");

    var $addressInput = $('#vk-address-input');

    self.onConnect = function () {
        var code = $codeInput.val().trim();
        var name = $nameInput.val().trim();


        var valid = true;
        if (!name) {
            $nameGroup.addClass('has-error');
            $nameInput.focus();
            valid = false;
        } else {
            $nameGroup.removeClass('has-error');
        }
        if (!/^([a-zA-Z0-9]+)$/.test(code)) {
            $codeGroup.addClass('has-error');
            $codeInput.focus();
            valid = false;
        } else {
            $codeGroup.removeClass('has-error');
        }
        if (valid) {
            block(true);
            send(name, code);
        }
    };

    self.init = function() {
        if (!$connect.attr('disabled')) {
            $connect.click(function() {
                $codeInput.val('');
                $nameInput.val('');
                $codeGroup.removeClass('has-error');
                $nameGroup.removeClass('has-error');
                block(false);
                switchStep(true);
                $modal.modal();
            });
            $button.click(self.onConnect);
        }
    };

    function send(name, code) {
        $.post(contextPath + 'vk/create/' + serverId, {name: name, code: code})
            .done(function (data) {
                $addressInput.val(contextPath + 'vk/callback/' + data.token);
                switchStep(false);
            })
            .fail(function (data) {
                BootstrapDialog.warning('Что-то пошло не так!');
            });
    }

    function block(block) {
        $button.prop('disabled', block);
        $codeInput.prop('disabled', block);
        $nameInput.prop('disabled', block);
        (block ? $connectText : $connectSpinner).hide();
        (block ? $connectSpinner : $connectText).show();
    }

    function switchStep(first) {
        (first ? $step2Block : $step1Block).hide();
        (first ? $step1Block : $step2Block).show();
        $modalFooter[first ? 'show' : 'hide']();
    }

    $('#vk-address-copy').click(function() {
        try {
            $addressInput.focus().select();
            document.execCommand('copy');
        } catch (err) { }
    });

    return self;
}