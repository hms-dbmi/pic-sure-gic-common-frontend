define(["jquery", "backbone", "handlebars", "text!output/moreInformation.hbs", "text!options/modal.hbs", 
        "common/config", "common/spinner"],
    function($, BB, HBS, moreInfoTemplate, modalTemplate, config, spinner){


        let moreInfoView = BB.View.extend({
            initialize: function(biosampleFields, resources) {
                this.moreInfoTemplate = HBS.compile(moreInfoTemplate);
                this.modalTemplate = HBS.compile(modalTemplate);
                this.biosampleFields = biosampleFields;
            	this.resources = resources;
            },
            events: {
                "click #more-info-btn" : "showMoreInformation"
            },
            updateResource: function(resource) {
            	
            	if( resource.patientCount ){
					$("#" + resource.uuid + "-patients").html( resource.patientCount )
				} else {
					$("#" + resource.uuid + "-patients").html("-");
				}
            	
            	if( resource.biosampleCount ){
					$("#" + resource.uuid + "-biosamples").html( resource.biosampleCount )
				} else {
					$("#" + resource.uuid + "-biosamples").html("-");
				}
            	
            	_.each(this.biosampleFields, function(bioField){
					if( resource.bioSampleCounts[bioField.id] ){
						$("#" + resource.uuid + "-" + bioField.id).html( resource.bioSampleCounts[bioField.id] )
					} else {
						$("#" + resource.uuid + "-" + bioField.id).html("-");
					}
    			});
            },
            updateAll: function(){
            	_.each(this.resources, function(resource){
            		this.updateResource(resource);
            	}.bind(this));
            },
            showMoreInformation: function(event){
            	
                $("#modal-window").html(this.modalTemplate({title: "More Information"}));
                $(".modal-body").html(this.moreInfoTemplate(this));
                
                $(".close").click(function(){
                    $("#modalDialog").hide();
                });

                this.updateAll();

                $("#modalDialog").show();
            }
            
        });
        
        return {
            View: moreInfoView,
            Model: BB.Model.extend({  })
        }
    }
);