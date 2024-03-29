define([
    "underscore",
    "text!overrides/dataset/dataset-save.hbs"
], function(_, template) {
    const renderSiteIds = function(caDatasetId){
        const ids = $("#dataset-ids");
        $("#ca-dataset-id").val(caDatasetId);
    };
    const updateSaveButton = function(){
        if($("#dataset-name").val()){
            $('#save-btn').removeClass("btn-default secondary");
            $('#save-btn').addClass("btn-outline alternate");
            $('#save-btn').prop("disabled", false);
        } else {
            $('#save-btn').removeClass("btn-outline alternate");
            $('#save-btn').addClass("btn-default secondary");
            $('#save-btn').prop("disabled", true);
        }
    };
    return {
        template,
        onSave: function(package) {
            const name = $("#dataset-name").val();
            const validationError = package.validateError(name);
            if(validationError){
                package.onError(validationError);
                $("#dataset-name").addClass('error');
                return;
            }
            
            $("#dataset-name").removeClass('error');
            $("#errors").addClass('hidden');

            const { commonAreaUUID, siteQueryIds } = package.queryUUID;
            $.ajax({
                url: window.location.origin + "/picsure/dataset/named",
                type: 'POST',
                headers: { "Authorization": "Bearer " + JSON.parse(sessionStorage.getItem("session")).token },
                contentType: 'application/json',
                data: JSON.stringify({
                    "queryId": commonAreaUUID,
                    "name": name,
                    "metadata": { saved: new Date().valueOf(), "siteQueryIds": siteQueryIds }
                }),
                success: function(){
                    package.modalSettings.onSuccess(name);
                    package.modalReturn();
                }.bind(package),
                error: function(response, status, error){
                    package.onError("An error happened during request.");
                    console.log(error);
                }.bind(package)
            });
        },
        renderExt: function(package){
            renderSiteIds(package.queryUUID.commonAreaUUID);
            updateSaveButton();
            $("#dataset-name").on('input', _.debounce(updateSaveButton, 300));
        }
    };
});