define(['jquery', 
        'common/spinner',
        'picSure/settings',
], function($, spinner, settings) {

    callInstituteNodes = function(package) {
        return package.outputModel.get('resources').map(resource => {
            const safeCopyQuery = {...package.exportModel.get('query'), resourceUUID: resource.uuid};
            return package.queryAsync(safeCopyQuery);
        });
    };
    handleUpdateStatusError = function(response) {
        console.log("Error preparing async download: ");
        console.dir(response);
        const serverMsg = response && response.responseText ? JSON.parse(response.responseText)?.message : 'No Message';
        return `There was an error preparing your query on this institution.\nPlease try again later, if the problem persists please reach out to an admin.\nMessage from server: ${serverMsg}`;
    };
    createResourceDisplay = function(queryUUID, resourceID, name, status) {
        const container = $(`<div id="${resourceID}" class="resource-container"></div>`);
        container.append(`
            <input type="checkbox" class="resource-checkbox tabable" id="${resourceID}" ${status !== "ERROR" ? 'checked' : ''} />
            <div class="resource-name">${name}</div>
            <input type="text" id="${resourceID}-queryid-span" class="query-id-span ${status}" value="${queryUUID}" readonly />
            <i role="img" id="${resourceID}-status" class="fa-solid 
            ${(status === "ERROR" || status === '' || status === undefined) ? 
            'fa-circle-xmark error' : 
            (status === "COMPLETE" || status === "AVAILABLE") ? 
            'fa-circle-check success' : 'fa-spinner fa-spin'}" aria-label="${status}" ></i>
        `);
        return container;
    };
    updateNodesStatus = function(package, responses, queryIdSpinnerPromise) {
        responses.map((promise, index) => {
            promise.then((response) => {
                const resource = package.outputModel.get('resources').find(resource => resource.uuid === response.resourceID);
                if (!package.cancelPendingPromises) {
                    const resourceIdContainer = createResourceDisplay(response.picsureResultId, resource.uuid, resource.name, response.status);
                    $('#queryIds').append(resourceIdContainer);
                    $(`#${resource.uuid}`).change(function() {
                        $('#copy-query-ids-btn').html('<span>Copy Dataset IDs</span>');
                        $('#copy-query-ids-btn').addClass('tabable');
                    });
                }
                index === responses.length-1 &&  queryIdSpinnerPromise.resolve();
                const safeCopyQuery = {...package.exportModel.get('query'), resourceUUID: resource.uuid};
                const deffered = $.Deferred();
                updateStatus(safeCopyQuery, response.picsureResultId, deffered);
                deffered.then((statusResponse) => {
                    updateStatusIcon(statusResponse.resourceID, statusResponse.status);
                    if (statusResponse.status === "AVAILABLE" || statusResponse.status === "COMPLETE") {
                        $('#copy-query-ids-btn').removeClass('hidden');
                    }
                }).catch((response)=>{
                    updateStatusIcon(response.resourceUUID, 'ERROR', response.error);
                });
            }).catch((response) => {
                index === responses.length-1 &&  queryIdSpinnerPromise.resolve();
                console.error('UpdateNodesStatus for ' + response + ' Failed');
                const resource = package.outputModel.get('resources').find(resource => resource.uuid === response);
                const resourceIdContainer = createResourceDisplay(`${resource.name} returned an error`, resource.uuid, resource.name, 'ERROR');
                $('#queryIds').append(resourceIdContainer);
            });
        });
        package.modal.createTabIndex();
    };
    updateStatusIcon = function(resourceID, status, message) {
        let statusIcon = $('#' + resourceID + '-status');
        statusIcon.removeClass('fa-spin fa-spinner success error fa-circle-check fa-circle-xmark');
        switch (status) {
            case 'COMPLETE':
            case 'AVAILABLE':
                statusIcon.addClass('success fa-circle-check');
                statusIcon.attr('aria-label', 'Complete');
                break;
            case 'ERROR':
                statusIcon.addClass('error fa-circle-xmark');
                statusIcon.attr('aria-label', 'Error');
                break;
            default:
                statusIcon.addClass('fa-spinner fa-spin');
                statusIcon.attr('aria-label', 'In Progress');
                break;
        }
        if (message) {
            statusIcon.attr('title', message);
        }
    };
    generateCommonAreaUUID = function(package) {
        // TODO: get resource from resources even though its hidden
        var uuidGenResourceID = settings.uuidGenResourceID;
        var uuidGenURL = window.location.origin + "/picsure/query"
        var uuidGenQuery = {
            "resourceUUID":uuidGenResourceID,
            "query":{}
        };
        $.ajax({
            url: uuidGenURL,
            type: 'POST',
            headers: { "Authorization": "Bearer " + JSON.parse(sessionStorage.getItem("session")).token },
            contentType: 'application/json',
            dataType: 'text',
            data: JSON.stringify(uuidGenQuery),
            success: function (response) {
                const queryIdSpinnerPromise = $.Deferred();
                spinner.small(queryIdSpinnerPromise, "#queryIdSpinner");
                const respJson = JSON.parse(response);
                const query = package.exportModel.get('query');
                query.commonAreaUUID = respJson.picsureResultId;
                package.exportModel.set('query', query);
                const responses = callInstituteNodes(package);
                updateNodesStatus(package, responses, queryIdSpinnerPromise);
            }
        });
    }
    updateStatus = function(query, queryUUID, deffered, interval = 0) {
        let queryUrlFragment = "/" + queryUUID + "/status?isInstitute=true";
        query.query.expectedResultType = "SECRET_ADMIN_DATAFRAME";
        $.ajax({
            url: window.location.origin + "/picsure/query" + queryUrlFragment,
            type: 'POST',
            headers: { "Authorization": "Bearer " + JSON.parse(sessionStorage.getItem("session")).token },
            contentType: 'application/json',
            dataType: 'text',
            data: JSON.stringify(query),
            success: function (response) {
                const respJson = JSON.parse(response);
                if (!respJson.status || respJson.status === "ERROR") {
                    const errMsg = handleUpdateStatusError(response);
                    deffered.reject({resourceUUID:query.resourceUUID, error: errMsg});
                    return;
                } else if (respJson.status && (respJson.status === "AVAILABLE" || respJson.status === "COMPLETE")) {
                    //resolve any waiting functions.
                    deffered.resolve(respJson);
                    return;
                }
                //check again, but back off at 2, 4, 6, ... 30 second (max) intervals
                interval = Math.min(interval + 2000, 30000);
                setTimeout(()=> {updateStatus(query, queryUUID, deffered, interval)}, interval);
            },
            statusCode: { 401:function() {
                const errMsg = handleUpdateStatusError(response);
                deffered.reject({resourceUUID:query.resourceUUID, error: errMsg});
                return;
                }}, 
            error: function (response) {
                const errMsg = handleUpdateStatusError(response);
                deffered.reject({resourceUUID:query.resourceUUID, error: errMsg});
                return;
            }
        });
    }
    return {
        addEvents: function(package) {
            $('#copy-query-ids-btn').on('click', function(){
                let queryIds = [];
                $('#queryIds').find('input[type="checkbox"]:checked').each(function(){
                    let siteCode = $(this).parent().find(".resource-name").text();
                    let queryId = $(this).parent().find('input[type="text"]').val();
                    queryIds.push(siteCode + ": " + queryId);
                });
                let queryIdsString = queryIds.join(',');
                navigator.clipboard.writeText(queryIdsString);
                $('#copy-query-ids-btn').html('<span>Copied! </span><i class="fa-solid fa-circle-check success" role="img" aria-label="Success"></i>');
            });
    
            $('#finalize-btn').on('click', function(){
                package.queryChangedCallback();
                package.cancelPendingPromises = false;
                generateCommonAreaUUID(package);
                $('#finalize-btn').addClass('hidden');
            });
    
            $('#concept-tree').on('changed.jstree', (e, data) => {
                $('#queryIds').empty();
                $('#finalize-btn').removeClass('hidden');
                $('#copy-query-ids-btn').addClass('hidden');
                $('#copy-query-ids-btn').addClass('tabable');
                $('#copy-query-ids-btn').html('<span>Copy Dataset IDs</span>');
                package.modal.createTabIndex();
                package.cancelPendingPromises = true;
            });
        },
        /*
		 * hook to allow overrides to customize the download data function
		 */
        downloadData: undefined,
        /*
		 * hook to allow overrides to customize the prepare function
		 */
        prepare: function() {
            window.open('https://redcap.tch.harvard.edu/redcap_edc/surveys/?s=EWYX8X8XX77TTWFR', '_blank');
        },
        /*
		 * hook to allow overrides to customize the queryAsync function
		 */
        queryAsync: function (query) {
            /*
             * This will send a query to PICSURE to evaluate and execute; it will not return results.  Use downloadData to do that.
             */
            let queryUrlFragment = '?isInstitute=true';
            query.query.expectedResultType = "SECRET_ADMIN_DATAFRAME";
            return new Promise((resolve, reject) => {
                $.ajax({
                    url: window.location.origin + "/picsure/query" + queryUrlFragment,
                    type: 'POST',
                    headers: { "Authorization": "Bearer " + JSON.parse(sessionStorage.getItem("session")).token },
                    contentType: 'application/json',
                    dataType: 'text',
                    data: JSON.stringify(query),
                    success: function (response) {
                        respJson = JSON.parse(response);
                        queryUUID = respJson.picsureResultId;
                        console.log("Async query submitted: " + queryUUID);
                        resolve(respJson);
                        return;
                    },
                    statusCode: { 401:function() {
                        reject(query.resourceUUID);
                        return;
                    } }, 
                    error: function (response) {
                        console.log("Error preparing async download: ");
                        console.dir(response);
                        reject(query.resourceUUID);
                        return;
                    }
                });
            });
        },
        /*
		 * hook to allow overrides to customize the querySync function
		 */
        querySync: undefined,

        queryChangedCallback: undefined,
        /*
		 * hook to allow overrides to customize the updateEstimations function
		 */
        updateEstimations: undefined,
        /*
		 * hook to allow overrides to customize the updateQuery function
		 */
        updateQuery: undefined,

        renderExt: function(package){
            this.addEvents(package);    
            package.copyReady = false;
            package.cancelPendingPromises = false;
            generateCommonAreaUUID(package);
		},
    };
});
