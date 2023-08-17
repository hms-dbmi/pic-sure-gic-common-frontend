define(["jquery", "backbone", "underscore", "handlebars", "text!output/moreInformation.hbs", "text!options/modal.hbs",
        "common/config", "common/spinner"],
    function($, BB, _, HBS, moreInfoTemplate, modalTemplate, config, spinner){


        let moreInfoView = BB.View.extend({
            initialize: function(biosampleFields, genomicFields, resources) {
                this.moreInfoTemplate = HBS.compile(moreInfoTemplate);
                this.modalTemplate = HBS.compile(modalTemplate);
                this.biosampleFields = biosampleFields;
                this.genomicFields = genomicFields;
            	this.resources = resources;
            },
            events: {
                "click #more-info-btn" : "showMoreInformation"
            },
            updateResource: function(resource) {
            	
            	if( resource.patientCount ){
					$("#" + resource.uuid + "-patients").html( resource.patientCount.toLocaleString() )
				} else {
					$("#" + resource.uuid + "-patients").html("-");
				}
            	
//            	if( resource.genomicdataCount ){
//					$("#" + resource.uuid + "-genomicdata").html( resource.genomicdataCount.toLocaleString() )
//				} else {
//					$("#" + resource.uuid + "-genomicdata").html("-");
//				}
//            	
//            	if( resource.biosampleCount ){
//					$("#" + resource.uuid + "-biosamples").html( resource.biosampleCount.toLocaleString() )
//				} else {
//					$("#" + resource.uuid + "-biosamples").html("-");
//				}
            	
            	_.each(this.genomicFields, function(genomicField){
					if( resource.genomicdataCounts[genomicField.id] ){
						$("#" + resource.uuid + "-" + genomicField.id).html( resource.genomicdataCounts[genomicField.id].toLocaleString() )
					} else {
						$("#" + resource.uuid + "-" + genomicField.id).html("-");
					}
    			});
            	
            	_.each(this.biosampleFields, function(bioField){
					if( resource.bioSampleCounts[bioField.id] ){
						$("#" + resource.uuid + "-" + bioField.id).html( resource.bioSampleCounts[bioField.id].toLocaleString() )
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