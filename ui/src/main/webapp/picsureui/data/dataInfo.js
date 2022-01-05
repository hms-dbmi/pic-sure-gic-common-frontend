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
                "click #data-info-btn" : "showDataInfo",
            },
            showDataForm: function(){
            	resouce =  this.resources.filter(x => x.name == this.managedSite)[0];
            	$(".modal-body").html(this.dataInfoFormTemplate(resouce));
            	
            	//attaching these manually because BB events hash is locked to it's element (in this case, the header)
            	$("#data-info-submit-btn").click(this.submitDataForm.bind(this));
            },
            submitDataForm: function(){
            	
            	//We need to build an object that has exactly the fields used in the back end; unrecognized or missing data will cause an error.
            	resource = {};
            	ui_resource =  this.resources.filter(x => x.name == this.managedSite)[0];
            	resource.uuid = ui_resource.uuid;
				resource.name = ui_resource.name;
				resource.description = ui_resource.description;
				resource.resourceRSPath = ui_resource.resourceRSPath;
            	
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
            	//metadata has to be a string so hibernate doesn't try to interpret it
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
						session.resources.find(x => x.name == this.managedSite).metadata = metadata;
						sessionStorage.setItem("session", JSON.stringify(session));
						
						alert("successfully updated resource");	
						$("#modalDialog").hide();
					}.bind(this),
					error: function(response){
						console.log("unable to update resource: " + response.responseText);
					}
				});
				
            },
            showDataInfo: function(){
            	
        		session = JSON.parse(sessionStorage.getItem("session"));
        		
        		//attache resources to session object for use in template
        		this.resources = session.resources;
        		
        		managerPrivs = session.privileges.filter(x => x.includes("PRIV_DATA_MANAGER_"));
        		if(managerPrivs.length > 0){
        			this.managedSite = managerPrivs[0].substring("PRIV_DATA_MANAGER_".length);
        		}
        		
                $("#modal-window").html(this.modalTemplate({title: "Institution Data Information"}));
                $(".modal-body").html(this.dataInfoTemplate(this));
                
            	//attaching these manually because BB events hash is locked to it's element (in this case, the header)
                $(".close").click(function(){
                    $("#modalDialog").hide();
                });
                $("#data-info-form-btn").click(this.showDataForm.bind(this));

                $("#modalDialog").show();
            }
        });
        
        return {
            View: dataInfoView,
            Model: BB.Model.extend({  })
        }
    }
);