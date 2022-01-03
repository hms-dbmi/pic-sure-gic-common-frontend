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
            	
            	if(!this.resources){
            		session = JSON.parse(sessionStorage.getItem("session"));
            		this.resources = session.resources;
            	}
            	
                $("#modal-window").html(this.modalTemplate({title: "Institution Data Information"}));
                $(".modal-body").html(this.dataInfoTemplate(this));
                
                $(".close").click(function(){
                    $("#modalDialog").hide();
                });

                $("#data-info-form-btn").click(function(){
                	 $(".modal-body").html(this.dataInfoFormTemplate(this));
                });
                
                $("#modalDialog").show();
            }
        });
        
        return {
            View: dataInfoView,
            Model: BB.Model.extend({  })
        }
    }
);