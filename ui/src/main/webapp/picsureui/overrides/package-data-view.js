define(['jquery', 
        'common/spinner',
], function($, spinner) {

    callInstituteNodes = function(package) {
        return package.model.get('resources').map(resource => {
            const safeCopyQuery = {...package.query, resourceUUID: resource.uuid};
            return package.queryAsync(safeCopyQuery);
        });
    };
    createResourceDisplay = function(queryUUID, resourceID, name, status) {
        const container = $(`<div id="${resourceID}" class="resource-container"></div>`);
        container.append(`
            <input type="checkbox" class="resource-checkbox" id="${resourceID}" ${status !== "ERROR" ? 'checked' : ''} />
            <div class="resource-name">${name}</div>
            <input type="text" id="${resourceID}-queryid-span" class="query-id-span ${status}" value="${queryUUID}" readonly />
            <i id="${resourceID}-status" class="fa-solid 
            ${(status === "ERROR" || status === '' || status === undefined) ? 
            'fa-circle-xmark error' : 
            (status === "COMPLETE" || status === "AVAILABLE") ? 
            'fa-circle-check success' : 'fa-spinner fa-spin'}"></i>
        `);
        return container;
    };
    updateNodesStatus = function(package, responses, queryIdSpinnerPromise) {
        responses.forEach((promise, index) => {
            const resourcePromise = promise.then((response) => {
                const resource = package.model.get('resources').find(resource => resource.uuid === response.resourceID);
                if (!package.cancelPendingPromises) {
                    const resourceIdContainer = createResourceDisplay(response.picsureResultId, resource.uuid, resource.name, response.status);
                    $('#queryIds').append(resourceIdContainer);
                }
                index === responses.length-1 &&  queryIdSpinnerPromise.resolve();
                const safeCopyQuery = {...package.query, resourceUUID: resource.uuid};
                return updateStatus(safeCopyQuery, response.picsureResultId).then(response => {
                    updateStatusIcon(response.resourceUUID, response.status);
                    if (response.status === "AVAILABLE" || response.status === "COMPLETE") {
                        $('#copy-query-ids-btn').removeClass('hidden');
                    }
                }).catch((response)=>{
                    updateStatusIcon(response.resourceUUID, 'ERROR', response.error);
                });
            }).catch((response) => {
                index === responses.length-1 &&  queryIdSpinnerPromise.resolve();
                console.error('UpdateNodesStatus for ' + response + ' Failed');
                const resource = package.model.get('resources').find(resource => resource.uuid === response);
                const resourceIdContainer = createResourceDisplay(`${resource.name} returned an error`, resource.uuid, resource.name, 'ERROR');
                $('#queryIds').append(resourceIdContainer);
            });
        });
    };
    updateStatusIcon = function(resourceID, status, message) {
        let statusIcon = $('#' + resourceID + '-status');
        statusIcon.removeClass('fa-spin fa-spinner success error fa-circle-check fa-circle-xmark');
        switch (status) {
            case 'COMPLETE':
            case 'AVAILABLE':
                statusIcon.addClass('success fa-circle-check');
                break;
            case 'ERROR':
                statusIcon.addClass('error fa-circle-xmark');
                break;
            default:
                statusIcon.addClass('fa-spinner fa-spin');
                break;
        }
        if (message) {
            statusIcon.attr('title', message);
        }
    };
    updateStatus = function(query, queryUUID) {
        let queryUrlFragment = "/" + queryUUID + "/status";
        let interval = 0;
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
                    const respJson = JSON.parse(response);
                    if (!respJson.status || respJson.status === "ERROR") {
                        return;
                    } else if (respJson.status && (respJson.status === "AVAILABLE" || respJson.status === "COMPLETE")) {
                        //resolve any waiting functions.
                        resolve(respJson);
                    }
                    //check again, but back off at 2, 4, 6, ... 30 second (max) intervals
                    interval = Math.min(interval + 2000, 30000);
                    setTimeout(()=>updateStatus(query, queryUUID), interval);
                },
                error: function (response) {
                    console.log("Error preparing async download: ");
                    console.dir(response);
                    const serverMsg = JSON.parse(response.responseText).message;
                    const errMsg = `There was an error preparing your query on this institution.\nPlease try again later, if the problem persists please reach out to an admin.\n Message from server: ${serverMsg}`;
                    reject({resourceUUID:query.resourceUUID, error: errMsg});
                }
            });
        });
    }
    return {
        addEvents: function(package) {
            $('#copy-query-ids-btn').on('click', function(){
                let queryIds = [];
                $('#queryIds').find('input[type="checkbox"]:checked').each(function(){
                    queryIds.push($(this).parent().find('input[type="text"]').val());
                });
                let queryIdsString = queryIds.join(',');
                navigator.clipboard.writeText(queryIdsString);
                const text = $('#copy-query-ids-btn').html();
                $('#copy-query-ids-btn').html('<span>'+text+' </span><i class="fa-solid fa-circle-check success"></i>')
            });
    
            $('#finalize-btn').on('click', function(){
                let queryIdSpinnerPromise = $.Deferred();
                spinner.small(queryIdSpinnerPromise, "#queryIdSpinner");
                package.queryChangedCallback();
                package.cancelPendingPromises = false;
                let responses = callInstituteNodes(package);
                updateNodesStatus(package, responses, queryIdSpinnerPromise);
                $('#finalize-btn').addClass('hidden');
            });
    
            $('#concept-tree').on('changed.jstree', (e, data) => {
                $('#queryIds').empty();
                $('#finalize-btn').removeClass('hidden');
                $('#copy-query-ids-btn').addClass('hidden');
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
            let queryUrlFragment = '';
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
                    },
                    error: function (response) {
                        console.log("Error preparing async download: ");
                        console.dir(response);
                        reject(query.resourceUUID);
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
            let queryIdSpinnerPromise = $.Deferred();
            spinner.small(queryIdSpinnerPromise, "#queryIdSpinner");
            let responses = callInstituteNodes(package);
            updateNodesStatus(package, responses, queryIdSpinnerPromise);
		},
    };
});