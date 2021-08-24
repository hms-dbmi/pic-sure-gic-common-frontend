define(["jquery", "backbone", "handlebars", "text!output/moreInformation.hbs", "text!options/modal.hbs", 
        "common/config", "common/spinner"],
    function($, BB, HBS, moreInfoTemplate, modalTemplate, config, spinner){


        let moreInfoView = BB.View.extend({
            initialize: function() {
                this.moreInfoTemplate = HBS.compile(moreInfoTemplate);
                this.modalTemplate = HBS.compile(modalTemplate);
            },
            events: {
                "click #more-info-btn" : "moreInformation"
            },
            updateQuery: function(query) {
                this.baseQuery = query;
                this.displayVariantButton();
            },
            moreInformation: function(event){
            	
            	  //lines ends up with a trailing empty object; strip that and the header row for the count
                $("#modal-window").html(this.modalTemplate({title: "More Information"}));
                $(".modal-body").html(this.moreInfoTemplate(this.model));
                
                
                $(".close").click(function(){
                    $("#modalDialog").hide();
                });

                $("#modalDialog").show();
                $(".modal-body").html();

            	
            	
            }
            
        });
        
        return {
            View: moreInfoView,
            Model: BB.Model.extend({
            	biosampleFields: undefined,
            	resources: undefined
            })
        }
    }
);