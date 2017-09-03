BootstrapDialog.DEFAULT_TEXTS[BootstrapDialog.TYPE_DEFAULT] = 'Информация';
BootstrapDialog.DEFAULT_TEXTS[BootstrapDialog.TYPE_INFO] = 'Информация';
BootstrapDialog.DEFAULT_TEXTS[BootstrapDialog.TYPE_PRIMARY] = 'Информация';
BootstrapDialog.DEFAULT_TEXTS[BootstrapDialog.TYPE_SUCCESS] = 'Выполнено';
BootstrapDialog.DEFAULT_TEXTS[BootstrapDialog.TYPE_WARNING] = 'Внимание';
BootstrapDialog.DEFAULT_TEXTS[BootstrapDialog.TYPE_DANGER] = 'Внимание!';
BootstrapDialog.DEFAULT_TEXTS['OK'] = 'OK';
BootstrapDialog.DEFAULT_TEXTS['CANCEL'] = 'Отмена';
BootstrapDialog.DEFAULT_TEXTS['CONFIRM'] = 'Подтверждение';

$(document).ready(function () {
    $('[data-toggle="tooltip"]').tooltip();
    $('.select2').select2();
    setTimeout(function() {
        $('.alert-success.flash-message').slideUp(400, function() { $(this).remove(); });
    }, 5000);
});
