define(["backbone", "text!overrides/output/outputPanel.hbs",  "common/transportErrors" ],
function(BB, outputTemplate, transportErrors){
	
	var resources = {};
	
	if(sessionStorage.getItem("session")){
		$.ajax({
			url: window.location.origin + '/picsure/resource',
			headers: {"Authorization": "Bearer " + JSON.parse(sessionStorage.getItem("session")).token},
			contentType: 'application/json',
			type:'GET',
			success: function(resourceData){
				_.each(resourceData, (resource) => {
					
					resources[resource.uuid] = {
							uuid: resource.uuid,
							name: resource.name,
							description: resource.description,
							patientCount: 0,
							spinnerClasses: "spinner-center ",
							spinning: false
					};
				});
			}
		});
	}
	
    
    return {
    	
    	resources: resources,
		/*
		 * This should be a function that returns the name of a Handlebars
		 * partial that will be used to render the count. The Handlebars partial
		 * should be registered at the top of this module.
		 */
		countDisplayOverride : undefined,
		/*
		 * This is a function that if defined replaces the normal render
		 * function from outputPanel.
		 */
		renderOverride : undefined,

		/*
		 * If you want to replace the entire Backbone.js Model that is used for
		 * the output panel, define it here.
		 */
		modelOverride :  BB.Model.extend({
			spinAll: function(){
				this.set('spinning', true);
				this.set('queryRan', false);
	  			
				_.each(resources, function(resource){
	  				resource.spinning=true;
	  				resource.queryRan=false;
	  			});
			}
		}),
		
		/*
		 * If you want to replace the entire Backbone.js View that is used for
		 * the output panel, define it here.
		 */
		viewOverride : 	undefined,
		/*
		 * In case you want to change the update logic, but not the rendering or
		 * anything else, you can define a function that takes an incomingQuery
		 * and dispatches it to the resources you choose, and handles
		 * registering callbacks for the responses and error handling.
		 */
		update: undefined,
		/*
		 * If you want to show your customized error message, please override
		 * this
		 */
		outputErrorMessage: "A server error occurred. please use the help link for further support.",
		
		outputTemplate: outputTemplate,
		
		dataCallback: function(resource, result, resultId, model, defaultOutput){
			var count = parseInt(result);
			var model = defaultOutput.model;
			
			model.set("totalPatients", model.get("totalPatients") + count);
			$("#patient-count").html(model.get("totalPatients"));
			
			resources[resource.uuid].queryRan = true;
			resources[resource.uuid].patientCount = count;
			//the spinning attribute maintains the spinner state when we render, but doesn't immediately update
			resources[resource.uuid].spinning = false;
			$(".spinner-" + resource.uuid).hide();
			
			$("#results-" + resource.name + "-count").html(count); 
				
			if(_.every(resources, (resource)=>{return resource.spinning==false})){
				model.set("spinning", false);
				model.set("queryRan", true);
			}
		},
		
		errorCallback: function(resource, message, defaultOutput){
			var model = defaultOutput.model;
			
			resources[resource.uuid].queryRan = true;
			resources[resource.uuid].patientCount = '-';
			//the spinning attribute maintains the spinner state when we render, but doesn't immediately update
			resources[resource.uuid].spinning = false;
			$(".spinner-" + resource.uuid).hide();
			
			$("#results-" + resource.name + "-count").html('-'); 
			
			if(_.every(resources, (resource)=>{return resource.spinning==false})){
				model.set("spinning", false);
				model.set("queryRan", true);
			}
		},
		
		/*
		 * The new hook for overriding all custom query logic
		 */
		runQuery: function(defaultOutput, incomingQuery, defaultDataCallback, defaultErrorCallback){
			var model = defaultOutput.model;
			model.set("resources", this.resources);
			model.set("totalPatients",0);
			model.spinAll();
			
			model.baseQuery = incomingQuery;   
  			defaultOutput.render();

			//run a query for each resource 
			_.each(resources, function(resource){
				// make a safe deep copy (scoped per resource) of the incoming query so we don't modify it
				var query = JSON.parse(JSON.stringify(incomingQuery));
				query.resourceUUID = resource.uuid;
				query.resourceCredentials = {};
				query.query.expectedResultType="COUNT";
			
				$.ajax({
				 	url: window.location.origin + "/picsure/query/sync",
				 	type: 'POST',
				 	headers: {"Authorization": "Bearer " + JSON.parse(sessionStorage.getItem("session")).token},
				 	contentType: 'application/json',
				 	data: JSON.stringify(query),
  				 	success: function(response, textStatus, request){
  				 		this.dataCallback(resource, response, request.getResponseHeader("resultId"), model, defaultOutput);
  						}.bind(this),
				 	error: function(response){
						if (!transportErrors.handleAll(response, "Error while processing query")) {
							response.responseText = "<h4>"
								+ this.outputErrorMessage;
								+ "</h4>";
					 		this.errorCallback(resource, response.responseText, defaultOutput);
						}
					}.bind(this)
				});
			}.bind(this));
		}
	};
});
