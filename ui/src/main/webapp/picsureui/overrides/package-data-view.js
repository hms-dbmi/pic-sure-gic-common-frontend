define(['jquery', 
        'common/spinner',
], function($, spinner) {

    callInstatueNodes = function(package) {
        let responses = [];
        _.each(package.model.get('resources'), (resource) => {
            let deferredQuery = $.Deferred();
            let tempQuery = JSON.parse(JSON.stringify(package.query));
            tempQuery.resourceUUID = resource.uuid;
            package.queryAsync(tempQuery, deferredQuery);
            responses.push(deferredQuery);
        });
        return responses;
    };
    createResourceDisplay = function(queryUUID, resourceID, name, status) {
        let container = $('<div id="'+ resourceID +'" class="resource-container"></div>');
        if (status === "ERROR") {
            container.append('<input type="checkbox" class="resource-checkbox" id="'+resourceID+'" />')
        } else {
            container.append('<input type="checkbox" class="resource-checkbox" id="'+resourceID+'" checked/>')
        }
        container.append('<div class="resource-name">' + name + '</div>');
        container.append('<input type="text" id="queryid-span" class="'+status+'" value="'+queryUUID+'" readonly />')
        // container.append('<span class="resource-status">Status: ' + status + '</span>');
        if (status === "COMPLETE" || status === "AVAILABLE") {
            container.append('<i id="'+resourceID+'-status" class="fa-solid fa-circle-check success"></i>');
        } else if (status === "ERROR") {
            container.append('<i id="'+resourceID+'-status" class="fa-solid fa-circle-xmark error"></i>');
        } else {
            container.append('<i id="'+resourceID+'-status" class="fa-solid fa-spinner fa-spin"></i>');
        }
        
        return container;
    };
    updateNodesStatus = function(package, responses, queryIdSpinnerPromise) {
        let totalResources = responses.length;
        let count = 0;
        _.each(responses, (promise) => {
            promise.then((response) => {
                console.log(response);
                const resource = package.model.get('resources').find(resource => resource.uuid === response.resourceID);
                if (!package.cancelPendingPromises) {
                    let resourceIdContainer = createResourceDisplay(response.picsureResultId, resource.uuid, resource.name, response.status);
                        $('#queryIds').append(resourceIdContainer);
                }
                let statusDerfer = $.Deferred();
                let tempQuery = JSON.parse(JSON.stringify(package.query));
                tempQuery.resourceUUID = resource.uuid;
                updateStatus(tempQuery, response.picsureResultId, statusDerfer);
                $.when(statusDerfer).done((response) => {
                    updateStatusIcon(response.resourceUUID, response.status);
                    if (response.status === "AVAILABLE" || response.status === "COMPLETE") {
                        $('#copy-query-ids-btn').removeClass('hidden');
                    }
                });
                if (count === totalResources - 1) {
                    queryIdSpinnerPromise.resolve();
                } else {
                    count++;
                }
            }).fail((response) => {
                console.error(response + ' Failed');
                const resource = package.model.get('resources').find(resource => resource.uuid === response);
                let resourceIdContainer = createResourceDisplay('An error occured', resource.uuid, resource.name, 'ERROR');
                $('#queryIds').append(resourceIdContainer);
                if (count === totalResources - 1) {
                    queryIdSpinnerPromise.resolve();
                } else {
                    count++;
                }
            });
        });
    };
    updateStatusIcon = function(resourceID, status) {
        let statusIcon = $('#' + resourceID + '-status');
        if (status === "COMPLETE" || status === "AVAILABLE") {
            statusIcon.removeClass('fa-spin');
            statusIcon.removeClass('fa-spinner');
            statusIcon.removeClass('error');
            statusIcon.addClass('success');
            statusIcon.addClass('fa-circle-check');
        } else if (status === "ERROR") {
            statusIcon.removeClass('fa-spin');
            statusIcon.removeClass('fa-spinner');
            statusIcon.removeClass('success');
            statusIcon.addClass('error');
            statusIcon.addClass('fa-circle-xmark');
        }
    };
    updateStatus = function(query, queryUUID, promise) {
        let queryUrlFragment = "/" + queryUUID + "/status";
        let interval = 0;
        query.query.expectedResultType = "TODO_DATAFRAME";
        $.ajax({
            url: window.location.origin + "/picsure/query" + queryUrlFragment,
            type: 'POST',
            headers: { "Authorization": "Bearer " + JSON.parse(sessionStorage.getItem("session")).token },
            contentType: 'application/json',
            dataType: 'text',
            data: JSON.stringify(query),
            success: function (response) {
                if (!respJson.status || respJson.status === "ERROR") {
                    return;
                } else if (respJson.status && (respJson.status === "AVAILABLE" || respJson.status === "COMPLETE")) {
                    //resolve any waiting functions.
                    promise && promise.resolve(results);
                    return;
                }
                //check again, but back off at 2, 4, 6, ... 30 second (max) intervals
                interval = Math.min(interval + 2000, 30000);
                setTimeout(updateStatus, interval);
            },
            error: function (response) {
                console.log("Error preparing async download: ");
                console.dir(response);
            }
        });
    }
    return {
        addEvents: function(package) {
            $('#copy-query-ids-btn').click(function(){
                let queryIds = [];
                $('#queryIds').find('input[type="checkbox"]:checked').each(function(){
                    queryIds.push($(this).parent().find('input[type="text"]').val());
                });
                let queryIdsString = queryIds.join(',');
                navigator.clipboard.writeText(queryIdsString);
                const text = $('#copy-query-ids-btn').html();
                $('#copy-query-ids-btn').html('<span>'+text+' </span><i class="fa-solid fa-circle-check success"></i>')
            });
    
            $('#finalize-btn').click(function(){
                let queryIdSpinnerPromise = $.Deferred();
                spinner.small(queryIdSpinnerPromise, "#queryIdSpinner");
                package.queryChangedCallback();
                package.cancelPendingPromises = false;
                let responses = callInstatueNodes(package);
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
        queryAsync: function (query, promise) {
            /*
             * This will send a query to PICSURE to evaluate and execute; it will not return results.  Use downloadData to do that.
             */
            let queryUrlFragment = '';
            query.query.expectedResultType = "TODO_DATAFRAME";
            (function updateStatus() {
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
                        promise.resolve(respJson);
                    },
                    error: function (response) {
                        console.log("Error preparing async download: ");
                        console.dir(response);
                        promise.reject(query.resourceUUID);
                    }
                });
            }());
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
            let responses = callInstatueNodes(package);
            updateNodesStatus(package, responses, queryIdSpinnerPromise);
		},
    };
});