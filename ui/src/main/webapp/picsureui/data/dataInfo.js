define(["jquery", "backbone", "handlebars", "text!data/dataInfo.hbs", "text!data/dataModal.hbs", 
	"text!data/dataInfoForm.hbs" ],
    function($, BB, HBS, dataInfoTemplate, modalTemplate, dataInfoFormTemplate){

        let dataInfoView = BB.View.extend({
            initialize: function() {
                this.dataInfoTemplate = HBS.compile(dataInfoTemplate);
                this.dataInfoFormTemplate = HBS.compile(dataInfoFormTemplate);
                this.modalTemplate = HBS.compile(modalTemplate);
            },
            events: {
                "click #data-info-btn" : "showDataInfo"
            },
            showDataInfo: function(event){
            	
        		session = JSON.parse(sessionStorage.getItem("session"));
        		
        		//attache resources to session object for use in template
        		this.resources = session.resources;
        		
        		
        		managerPrivs = session.privileges.filter(x => x.includes("DATA_MANAGER_"));
        		if(managerPrivs.length > 0){
        			this.managedSite = managerPrivs[0].substring("DATA_MANAGER_".length);
        		}
        		
                $("#modal-window").html(this.modalTemplate({title: "Institution Data Information"}));
                $(".modal-body").html(this.dataInfoTemplate(this));
                
                $(".close").click(function(){
                    $("#modalDialog").hide();
                });

                $("#data-info-form-btn").click(function(){
                	resouce =  this.resources.filter(x => x.name == this.managedSite)[0];
                	 $(".modal-body").html(this.dataInfoFormTemplate(resouce));
                }.bind(this));
                
                $("#data-info-submit-btn").click(function(){
                	resouce =  this.resources.filter(x => x.name == this.managedSite)[0];
                	
                	metadata = {};
    				metadata.clinicalPopulation = $("#clinicalPopulation").val();
    				metadata.genomicPopulation = $("#genomicPopulation").val();
    				metadata.genomicDataDiagnosis = $("#genomicDataDiagnosis").val();
    				metadata.genomicDataTypes = $("#genomicDataTypes").val();
    				metadata.biosamplePopulation = $("#biosamplePopulation").val();
    				metadata.firstDataPoint = $("#firstDataPoint").val();
    				metadata.clinicalDate = $("#clinicalDate").val();
    				metadata.genomicDate = $("#genomicDate").val();
    				metadata.availability = $("#availability").val();
                	
    				resource.metadata = JSON.stringify(metadata);
    				
    				resourceList = [resource];
    				
    				$.ajax({
    					url: window.location.origin + '/picsure/resource',
    					headers: {"Authorization": "Bearer " + session.token},
    					contentType: 'application/json',
    					type:'PUT',
    					data: JSON.stringify(resourceList),
    					success: function(){
				    		
    						//update stored resource
    						session = JSON.parse(sessionStorage.getItem("session"));
    						resouce =  session.resources.filter(x => x.name == this.managedSite)[0];
    						resource.metadata = metadata;
    						sessionStorage.setItem("session", JSON.stringify(session));
    						
    						alert("success");				
    					},
    					error: function(response){
    						console.log("unable to update resource: " + response.responseText);
    					}
    				});
    				
                }.bind(this));
                
                
                $("#modalDialog").show();
            }
        });
        
        return {
            View: dataInfoView,
            Model: BB.Model.extend({  })
        }
    }
);