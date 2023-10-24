define([
    "text!overrides/dataset/dataset-save.hbs",
    "common/modal"
], function(template, modal) {
    const renderSiteIds = function(siteQueryIds){
        const ids = $("#dataset-ids");
        siteQueryIds.forEach((site) => {
            const { name, queryId } = site;
            const container = $(`<div id="site-${name}" class="row"></div>`);
            container.append(`
                <div class="col-md-3">${name}</div>
                <div class="col-md-9"><input type="text" value="${queryId}" maxlength="38" size="38" readonly /></div>
            `);
            ids.append(container);
        });
    };
    return {
        template,
        onSave: function(package) {
            const name = $("#dataset-name").val();
            if(name === ""){
                package.onError("Please input a Dataset ID Name value");
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
            renderSiteIds(package.queryUUID.siteQueryIds);
            modal.createTabIndex();
        }
    };
});