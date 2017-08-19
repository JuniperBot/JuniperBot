
/**
 * Get a prestored setting
 *
 * @param String name Name of of the setting
 * @returns String The value of the setting | null
 */
function getStored(name) {
    if (typeof (Storage) !== 'undefined') {
        return localStorage.getItem(name)
    } else {
        window.alert('Please use a modern browser to properly view this site!')
    }
}

/**
 * Store a new settings in the browser
 *
 * @param String name Name of the setting
 * @param String val Value of the setting
 * @returns void
 */
function setStored(name, val) {
    if (typeof (Storage) !== 'undefined') {
        localStorage.setItem(name, val)
    } else {
        window.alert('Please use a modern browser to properly view this site!')
    }
}
