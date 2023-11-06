define([
    'jquery', 
    'underscore',
    'common/spinner',
    'picSure/settings',
    'common/modal',
    'dataset/utilities',
    'dataset/dataset-save'
], function($, _, spinner, settings, modal, dataUtils, namedDataset) {
    const callInstituteNodes = function(package) {
        return package.outputModel.get('resources').map(resource => {
            const safeCopyQuery = {...package.exportModel.get('query'), resourceUUID: resource.uuid};
            return package.queryAsync(safeCopyQuery);
        });
    };
    const handleUpdateStatusError = function(response) {
        console.log("Error preparing async download: ");
        console.dir(response);
        const serverMsg = response && response.responseText ? JSON.parse(response.responseText)?.message : 'No Message';
        return `There was an error preparing your query on this institution.\nPlease try again later, if the problem persists please reach out to an admin.\nMessage from server: ${serverMsg}`;
    };
    const createResourceDisplay = function(queryUUID, resourceID, name, status) {
        const container = $(`<div id="${resourceID}" data-resource-id="${resourceID}" data-resource-name="${name}" class="resource-container"></div>`);
        const { icon, label, checked } = statusMeta(status);
        container.append(`
            <input type="checkbox" class="resource-checkbox tabable" id="${resourceID}" ${checked} />
            <div class="resource-name">${name}</div>
            <input type="text" id="${resourceID}-queryid-span" class="query-id-span ${status}" value="${queryUUID}" readonly />
            <i role="img" id="${resourceID}-status" class="fa-solid ${icon}" aria-label="${label}" ></i>
        `);
        return container;
    };
    const updateNodesStatus = function(package, responses, queryIdSpinnerPromise) {
        responses.map((promise, index) => {
            promise.then((response) => {
                const resource = package.outputModel.get('resources').find(resource => resource.uuid === response.resourceID);
                if (!package.cancelPendingPromises) {
                    const resourceIdContainer = createResourceDisplay(response.picsureResultId, resource.uuid, resource.name, response.status);
                    $('#queryIds').append(resourceIdContainer);
                }
                index === responses.length-1 &&  queryIdSpinnerPromise.resolve();
                const safeCopyQuery = {...package.exportModel.get('query'), resourceUUID: resource.uuid};
                const deffered = $.Deferred();
                updateStatus(safeCopyQuery, response.picsureResultId, deffered);
                deffered.then((statusResponse) => {
                    updateStatusIcon(statusResponse.resourceID, statusResponse.status);
                    if (statusResponse.status === "AVAILABLE" || statusResponse.status === "COMPLETE") {
                        $('#save-dataset-btn').removeClass('hidden');
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
            }).finally(() => {
                const sorted = $("#queryIds div.resource-container")
                    .sort((a,b) => $(a).data("resource-name").localeCompare($(b).data("resource-name")));
                $("#queryIds").html(sorted);
            });
        });
        $("#finalize-btn").addClass("hidden");
        package.modal.createTabIndex();
    };
    const statusMeta = function(status){
        switch (status) {
            case 'COMPLETE':
            case 'AVAILABLE':
            return { icon: 'success fa-circle-check', label: 'Complete', checked: 'checked' };
            case 'ERROR':
            return { icon: 'error fa-circle-xmark', label: 'Error', checked: '' };
            default:
            return { icon: 'fa-spinner fa-spin', label: 'In Progress', checked: 'checked' };
        }
    };
    const updateStatusIcon = function(resourceID, status, message) {
        const { icon, label } = statusMeta(status);
        const statusIcon = $('#' + resourceID + '-status');
        statusIcon.removeClass('fa-spin fa-spinner success error fa-circle-check fa-circle-xmark');
        statusIcon.addClass(icon);
        statusIcon.attr('aria-label', label);
        message && statusIcon.attr('title', message);
    };
    // Use the query endpoint to save the original query and generate a uuid,
    // before passing it to each site.
    const generateCommonAreaUUID = function(package) {
        if(package.exportModel.get('lastQueryUUID')) {
            const queryIdSpinnerPromise = $.Deferred();
            spinner.small(queryIdSpinnerPromise, "#queryIdSpinner");
            const responses = package.exportModel.get('siteQueries');
            updateNodesStatus(package, responses, queryIdSpinnerPromise);
            return;
        }

        // TODO: get resource from resources even though its hidden
        const url = window.location.origin + "/picsure/query"
        const queryObject = package.exportModel.get('query') || {};
        queryObject.resourceUUID = settings.uuidGenResourceID;

        $.ajax({
            url,
            type: 'POST',
            headers: { "Authorization": "Bearer " + JSON.parse(sessionStorage.getItem("session")).token },
            contentType: 'application/json',
            dataType: 'text',
            data: JSON.stringify(queryObject),
            success: function (response) {
                const queryIdSpinnerPromise = $.Deferred();
                spinner.small(queryIdSpinnerPromise, "#queryIdSpinner");
                const respJson = JSON.parse(response);
                const query = queryObject;
                query.commonAreaUUID = respJson.picsureResultId;
                package.exportModel.set('lastQueryUUID', query.commonAreaUUID);
                package.exportModel.set('query', query);
                const responses = callInstituteNodes(package);
                package.exportModel.set('siteQueries', responses);
                updateNodesStatus(package, responses, queryIdSpinnerPromise);
                package.updateNamedDatasetObjects();
            }
        });
    }
    const updateStatus = function(query, queryUUID, deffered, interval = 0) {
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
    const getQueryIds = function(){
        const queryIds = [];
        $('#queryIds').find('input[type="checkbox"]:checked').each(function(){
            const resourceId = $(this).parent().data("resource-id");
            const name = $(this).parent().find(".resource-name").text();
            const queryId = $(this).parent().find('input[type="text"]').val();
            queryIds.push({ resourceId, name, queryId });
        });
        return queryIds;
    }
    return {
        saveDatasetId: function(package){
            const siteQueryIds = getQueryIds();
            const title = "Save Dataset ID";
            const onClose = () => {};
            const onSuccess = (name) => {
                package.exportModel.set('datasetName', name);
                package.updateNamedDatasetObjects();
            };
            const options = { ...package.modalSettings.options, width: "40%" };
            const query = package.exportModel.get('query');
            const modalView = new namedDataset({
                modalSettings: { title, onClose, onSuccess, options },
                previousModal: { view: package, ...package.modalSettings },
                queryUUID: {
                    commonAreaUUID: query.commonAreaUUID,
                    siteQueryIds
                }
            });
            modal.displayModal(modalView, title, onClose, options);
        },
        queryChangedCallback: function(package){
            const previousState = package.exportModel.get('treeState') || {};
            const nextState = $('#concept-tree', this.$el).jstree().get_json();

            if(_.isEqual(previousState, nextState)) {
                return;
            }

            package.exportModel.set('treeState', nextState);
            const query = package.updateQueryFields();
            package.exportModel.set('query', query);
            package.updateEstimations(query);

            package.exportModel.set('lastQueryUUID', undefined);
            package.exportModel.set('datasetName', undefined);

            $('#queryIds').empty();
            $('#finalize-btn').removeClass('hidden');
            $('#copy-pheno-ids-btn').addClass('hidden');
            $('#copy-pheno-ids-btn').html('<span>Copy Dataset Variables</span>');
            $('#copy-query-ids-btn').addClass('hidden');
            $('#copy-query-ids-btn').html('<span>Copy Dataset IDs</span>');
            $("#save-dataset-btn").addClass('hidden');
            package.modal.createTabIndex();
            package.cancelPendingPromises = true;
            package.updateNamedDatasetObjects();
        },
        addEvents: function(package) {
            const copyText = prefix => `<span>${prefix} Copied! </span><i class="fa-solid fa-circle-check success" role="img" aria-label="Success"></i>`;
            $('#copy-query-ids-btn').on('click', function(){
                const queryIdsString = getQueryIds().map(site => site.name + ": " + site.queryId ).join(',');
                navigator.clipboard.writeText(queryIdsString);
                $('#copy-query-ids-btn').html(copyText("Dataset IDs"));
            });

            $('#copy-pheno-ids-btn').on('click', function(){
                const listString = dataUtils.render.phenoToString(
                    package.exportModel.get('query').query,
                    dataUtils.format,
                    dataUtils.render.string
                );
                navigator.clipboard.writeText(listString);
                $('#copy-pheno-ids-btn').html(copyText("Dataset Variables"));
            });
    
            $('#finalize-btn').on('click', function(){
                package.cancelPendingPromises = false;
                generateCommonAreaUUID(package);
            });
            
            $('#request-btn').on('click', function(){
                this.request();
            });
        },
        updateNamedDatasetObjects: function(package) {
            const name = package.exportModel.get('datasetName');
            const uuid = package.exportModel.get('lastQueryUUID');
            
            if (!uuid){
                $("#save-dataset-btn").addClass('hidden');
            }
            if (uuid && name){
                $('#save-dataset-btn').html('<span>Dataset saved! </span><i class="fa-solid fa-circle-check success" role="img" aria-label="Success"></i>');
                $('#save-dataset-btn').prop("disabled", true);
                $("#dataset-saved").removeClass("hidden");
                $('#copy-query-ids-btn').removeClass('hidden');
                $('#copy-pheno-ids-btn').removeClass('hidden');
            } else {
                $("#save-dataset-btn").html('Save Dataset ID');
                $('#save-dataset-btn').prop("disabled", false);
                $("#dataset-saved").addClass("hidden");
                $('#copy-query-ids-btn').addClass('hidden');
                $('#copy-pheno-ids-btn').addClass('hidden');
            }
        },
        prepare: function() {}, // override to do nothing
        request: function() {
            window.open('https://redcap.tch.harvard.edu/redcap_edc/surveys/?s=EWYX8X8XX77TTWFR', '_blank');
        },
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
        renderExt: function(package){
            this.addEvents(package);    
            package.copyReady = false;
            package.cancelPendingPromises = false;
            generateCommonAreaUUID(package);
            package.updateNamedDatasetObjects();
            package.modal.createTabIndex();
        },
    };
});
