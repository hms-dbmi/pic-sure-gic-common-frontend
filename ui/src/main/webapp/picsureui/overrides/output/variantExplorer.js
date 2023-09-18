define([
    "jquery", "underscore", "datatables.net", "backbone", "handlebars", "text!output/variantTable.hbs",
    "text!options/modal.hbs", "picSure/settings", "common/config", "common/spinner", "output/variantExplorer",
    "text!overrides/output/commonAreaVariantExpl.hbs", "text!overrides/output/requestError.hbs"
],
    function(
        $, _, datatables, BB, HBS, variantTableTemplate, modalTemplate, settings, config, spinner, baseExplorer,
        commonAreaVETemplate, requestErrorTemplate,
    ){
        const maxVariantCount =  settings.maxVariantCount ? settings.maxVariantCount : 1000;
        let createDownloadLink = function(response){
            $("#variant-explorer-modal-title-container").append("<a id='variant-download-btn'>Download Variant Data</a>");
            const responseDataUrl = URL.createObjectURL(new Blob([response], {type: "octet/stream"}));
            $("#variant-download-btn", $(".modal-header")).off('click');
            $("#variant-download-btn", $(".modal-header")).attr("href", responseDataUrl);
            $("#variant-download-btn", $(".modal-header")).attr("download", "variantData.tsv");
        }

        let session = undefined;
        let resources = [];
        let selectedInstitute = undefined;

        let updateResourceInfo = function() {
            session = JSON.parse(sessionStorage.getItem("session"));
            resources = session ? session.resources : [];
            selectedInstitute = resources && resources.length ? resources[0] : undefined;
        }

        let variantExplorerView = BB.View.extend({
            initialize: function(opts) {
                this.baseQuery = opts.query;
                this.dataErrorMsg = opts.errorMsg ? opts.errorMsg : "There was an error loading the data";
                this.commonAreaVETemplate = HBS.compile(commonAreaVETemplate);
                this.requestErrorTemplate = HBS.compile(requestErrorTemplate);
            },
            resourceOnClick: function(institute) {
                if (selectedInstitute && selectedInstitute.uuid === institute.uuid) {
                    return;
                }
                $("#" + selectedInstitute.uuid).removeClass("active-institute");
                $("#" + selectedInstitute.uuid).addClass("inactive-institute");

                selectedInstitute = institute;
                $("#" + selectedInstitute.uuid).addClass("active-institute");
                $("#" + selectedInstitute.uuid).removeClass("inactive-institute");

                $('#variant-download-btn').remove();
                this.render(true);
            },

            render: function() {
                // make sure close button isn't deleted by moving it to a stable node
                if ($("#close-modal-button").parent().attr('class') !== 'modal-header') {
                    $(".modal-header").append($("#close-modal-button"));
                }
                // make sure resource is set
                if (!resources || !resources.length) {
                    updateResourceInfo();
                }
                // create institute gutter
                this.$el.html(this.commonAreaVETemplate({"resources": resources}));
                const self = this;
                resources.forEach(function (resource) {
                    $("#" + resource.uuid).click(function () {self.resourceOnClick(resource);});
                });
                $("#" + selectedInstitute.uuid).removeClass("inactive-institute");
                $("#" + selectedInstitute.uuid).addClass("active-institute");

                // edit query to use institute resource uuid
                const instQuery = JSON.parse(JSON.stringify(this.baseQuery));
                instQuery.resourceUUID = selectedInstitute.uuid;

                // render base variant explorer
                const errorMsg = this.requestErrorTemplate({"institute": selectedInstitute.name});
                const base = new baseExplorer({
                    el: $(".variant-explorer-main-content"),
                    query: instQuery,
                    errorMsg: errorMsg,
                    modalTitleSelector: '#variant-explorer-modal-title'
                });
                base.render();

                // clean up old header
                $('.modal-header').css('display', 'none');
                $(".modal-body").prepend($("#close-modal-button"));
            }
        });
        return variantExplorerView;
    }
);