/*
 * Config:
 * rowClass =
 *      Required
 *      The css class that will identify the top level element of a row
 *      Note also the use of a 'defaultRow' class to be used with rowClass
 *          when a defaultRow is not required
 * addRowId =
 *      Required
 *      The DOM id of the link or button that will initiate the adding of a new row
 * removeRowClass =
 *      Required
 *      The css class that will identify the links or buttons that
 *      when clicked will result in the removal of the row
 * formId =
 *      Required
 *      The DOM id of the form
 * rowContainerId =
 *      Required
 *      It is a requirement that rows be wrapped in a containing element
 *      rowContainerId is the DOM id of this wrapper element
 * indexedPropertyName =
 *      Required
 *      The name of indexed property in the Java bean
 * indexedPropertyMemberNames =
 *      Required
 *      A comma separated list of member variable (property) names
 *      The indexed property wil be a List or Collection containing
 *      a bunch of objects or a praticulr type e.g. List<Person> indexedPropertyName
 *      The Person onject in this case will have member variables
 *      e.g.    private String name;
 *              private String age;
 *      indexedPropertyMemberNames : 'name, age',
 * rowAddedListener =
 *      Optional
 *      The function to be invoked when a row is added
 *      The row element is passed in as an argument.
 *      rowAddedListener : rowAdded,
 *      function rowAdded( element ) {
 *          var rowHtml = $(element).html();
 *      }
 *  rowRemovedListener =
 *      Optional
 *      The function to be invoked when a row is removed
 *      The row element is passed in as an argument.
 *      rowRemovedListener : rowRemoved,
 *      function rowRemoved( element ) {
 *          var rowHtml = $(element).html();
 *      }
 *   beforeSubmit =
 *      Optional
 *      The function to be invoked before the form is submitted
 *      beforeSubmit : validate,
 *      function validate() {
 *          //do validation
 *          return valid;
 *      }
 *      function should return true or false, false will prevent form submission
 */
function DynamicListHelper(config) {

    init();

    var lastRow;

    function init() {
        config.rowClass = makeClass(config.rowClass);
        config.emptyClass = makeClass(config.emptyClass);
        config.addRowId = makeId(config.addRowId);
        config.removeRowClass = makeClass(config.removeRowClass);
        config.formId = makeId(config.formId);
        config.rowContainerId = makeId(config.rowContainerId);
        //config.indexedPropertyName =
        //config.indexedPropertyMemberNames =
        //config.rowAddedListener =
        //config.rowRemovedListener =
        //config.beforeSubmit =
        addRemoveRowListener();
        addAddRowListener();
        prepRows();
        $(config.formId).submit(function () {
            prepFormForSubmit();
            if (config.beforeSubmit != null) {
                return config.beforeSubmit();
            } else {
                return true;
            }
        });
        dealWithPotentialDefaultRow();
    }

    function dealWithPotentialDefaultRow() {
        var defaultRow = $(config.formId).find(config.rowClass + ".defaultRow:last");
        $(defaultRow).removeClass('defaultRow');
        lastRow = defaultRow.clone(true);
        $(defaultRow).remove();
        $(config.formId).find(config.emptyClass)[$(config.formId).find(config.rowClass).length ? 'hide' : 'show']();
    }

    function prepFormForSubmit() {
        var memberArray = config.indexedPropertyMemberNames.split(',');
        for (var i in memberArray) {
            var className = '.DynamicListHelper_' + $.trim(memberArray[i]);
            var index = 0;
            $(className).each(function () {
                $(this).attr('name', config.indexedPropertyName + "[" + index + "]." + $.trim(memberArray[i]));
                index++;
            });
        }
        $(config.formId).find(config.emptyClass)[$(config.formId).find(config.rowClass).length ? 'hide' : 'show']();
    }

    function prepRows() {
        var memberArray = config.indexedPropertyMemberNames.split(',');
        for (var i in memberArray) {
            var s = config.indexedPropertyName + "[";
            var e = "]." + $.trim(memberArray[i]);
            $(config.rowContainerId).find('*').each(function () {
                if ($(this).attr('name')) {
                    var bs = ($(this).attr('name').indexOf(s) === 0);
                    var be = ($(this).attr('name').match(e + "$") != null);
                    if (bs && be) {
                        $(this).addClass('DynamicListHelper_' + $.trim(memberArray[i]));
                    }
                }
            });
        }
    }

    function removeRow(element) {
        lastRow = $(config.formId).find(config.rowClass + ":last").clone(true);
        $(element).closest(config.rowClass).remove();
        prepFormForSubmit();
        if (config.rowRemovedListener != null) {
            config.rowRemovedListener(element);
        }
    }

    function addRow() {
        var row = $(config.formId).find(config.rowClass + ":last").clone(true);
        if (!row.length)
            row = lastRow;
        if (typeof $(row).attr('id') !== 'undefined') {
            $(row).attr('id', $(row).attr('id') + '_1');
        }
        $(row).find('*').each(function () {
            if (typeof $(this).attr('id') !== 'undefined') {
                $(this).attr('id', $(this).attr('id') + '_1');
            }
        });
        $(config.rowContainerId).append(row);
        prepFormForSubmit();
        if (config.rowAddedListener != null) {
            config.rowAddedListener($(row));
        }
    }

    function addRemoveRowListener() {
        $(config.removeRowClass).click(function () {
            removeRow($(this).closest(config.rowClass));
            return false;
        });
    }

    function addAddRowListener() {
        $(config.addRowId).click(function () {
            addRow();
            return false;
        });
    }

    function makeClass(className) {
        if (className.indexOf('.') != 0)
            className = "." + className;
        return className;
    }

    function makeId(className) {
        if (className.indexOf('#') != 0)
            className = "#" + className;
        return className;
    }
}