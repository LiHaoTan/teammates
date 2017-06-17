import { makeCsrfTokenParam } from '../common/crypto.es6';
import { encodeHtmlString } from '../common/sanitizer.es6';

/**
 * Functions defined and used in `/adminLtiCredentials`
 */

/**
 * Generates HTML text for a row containing instructor's information
 * and status of the action.
 *
 * @param {String} consumerKey
 * @param {String} consumerSecret
 * @param {bool} isSuccess is a flag to show the action is successful or not.
 * The color and status of the row is affected by its value.
 * @param {String} status
 * @returns {String} a HTML row of action result table
 */
function createRowForResultTable(consumerKey, consumerSecret, isSuccess, status) {
    return `
    <tr class="${isSuccess ? 'success' : 'danger'}">
        <td>${encodeHtmlString(consumerKey)}</td>
        <td>${encodeHtmlString(consumerSecret)}</td>
        <td>${isSuccess ? 'Success' : 'Fail'}</td>
        <td>${status}</td>
    </tr>
    `;
}

let params;    // list of parameter strings that will be sent via ajax
let consumerKey;

/**
 * Disables the Add Credentials form.
 */
function disableAddCredentialsForm() {
    $('#btnGenerateCredentials').html("<img src='/images/ajax-loader.gif'/>");
    $('.addCredentialsFormControl').each(function () {
        $(this).prop('disabled', true);
    });
}

/**
 * Enables the Add Credentials form.
 */
const enableAddCredentialsFormImpl = () => {
    $('#btnGenerateCredentials').html('Generate Credentials');
    $('.addCredentialsFormControl').each(function () {
        $(this).prop('disabled', false);
    });
};

let enableAddCredentialsForm = enableAddCredentialsFormImpl;

function addInstructorAjax(isError, data) {
    let rowText;
    if (isError) {
        rowText = createRowForResultTable('-', '-', false, 'Cannot send Ajax Request!');
    } else {
        rowText = createRowForResultTable(
            data.consumerKey,
            data.consumerSecret,
            data.isInstructorAddingResultForAjax,
            data.statusForAjax,
        );
    }
    $('#addCredentialsResultTable').find('tbody').append(rowText);
    const isNotAddingResultForAjax = !(data && data.isInstructorAddingResultForAjax);
    if (isNotAddingResultForAjax) {
        $('#consumerKey').val(consumerKey);
    }
    enableAddCredentialsForm();
}

/**
 * Sends Ajax request to add new instructor(s).
 * It only sends another Ajax request after it finishes.
 */
const addInstructorByAjaxRecursivelyImpl = () => {
    $.ajax({
        type: 'POST',
        url: `/admin/adminLtiCredentialsAdd?${makeCsrfTokenParam()}&${params}`,
        beforeSend: disableAddCredentialsForm,
        error() {
            addInstructorAjax(true, null);
        },
        success(data) {
            addInstructorAjax(false, data);
        },
    });
};

let addInstructorByAjaxRecursively = addInstructorByAjaxRecursivelyImpl;

/**
 * Reads information of the instructor from the second panel then add him/her.
 */
function addInstructorFromSecondFormByAjax() {
    const addCredentialsResultPanel = $('#addCredentialsResultPanel');
    addCredentialsResultPanel.show();

    consumerKey = $('#consumerKey').val();

    params = $.param({
        consumerkey: consumerKey,
    });

    $('#addCredentialsResultTable').find('tbody').html('');    // clear table
    addCredentialsResultPanel.find('div.panel-heading').html('<strong>Result</strong>');    // clear panel header
    addInstructorByAjaxRecursively();
}

$(document).ready(() => {
    $('#btnGenerateCredentials').on('click', () => {
        addInstructorFromSecondFormByAjax();
    });
});

function stubEnableAddInstructorForm(fn) {
    enableAddCredentialsForm = fn;
}

function unstubEnableAddInstructorForm() {
    enableAddCredentialsForm = enableAddCredentialsFormImpl;
}

function stubAddInstructorByAjaxRecursively(fn) {
    addInstructorByAjaxRecursively = fn;
}

function unstubAddInstructorByAjaxRecursively() {
    addInstructorByAjaxRecursively = addInstructorByAjaxRecursivelyImpl;
}

function getParams() {
    return params;
}

function setParams(par) {
    params = par;
}

export {
    addInstructorAjax,
    addInstructorFromSecondFormByAjax,
    createRowForResultTable,
    getParams,
    setParams,
    stubAddInstructorByAjaxRecursively,
    stubEnableAddInstructorForm,
    unstubAddInstructorByAjaxRecursively,
    unstubEnableAddInstructorForm,
};
