define(["backbone", "text!overrides/output/outputPanel.hbs",  "common/transportErrors", "picSure/settings", "output/moreInformation" ],
function(BB, outputTemplate, transportErrors, settings, moreInformation){
	
	//track the resources using a map to look up by UUID
	var resources = {};
	
	//separate object to maintain sort order
	var resourcesSorted = [];
	
	var biosampleFields = settings.biosampleFields;
	
	var genomicFields = settings.genomicFields;
	
	var resourceQueryDeferred = $.Deferred();
	
	if(sessionStorage.getItem("session")){
		$.ajax({
			url: window.location.origin + '/picsure/resource',
			headers: {"Authorization": "Bearer " + JSON.parse(sessionStorage.getItem("session")).token},
			contentType: 'application/json',
			type:'GET',
			success: function(resourceData){
				_.each(resourceData, (resource) => {
					if(!resource.hidden){
						resources[resource.uuid] = {
								uuid: resource.uuid,
								name: resource.name,
								description: resource.description,
								patientCount: 0,
								spinnerClasses: "spinner-center ",
								spinning: false,
								bioSampleCounts: {},
								genomicdataCounts: {}
						};
					}
					
				});
				
				resourcesSorted.push(...Object.values(resources).sort(function compareFn(a, b) {
											return a.name.localeCompare(b.name);
										}));
				
				resourceQueryDeferred.resolve();
				
				
			},
			error: function(response){
				console.log("unable to get resources: " + response.responseText);
				resourceQueryDeferred.fail();
			}
		});
	}
	
    
    return {
    	
    	resources: resourcesSorted,
    	
    	resourceQueryDeferred: resourceQueryDeferred,
    	
    	biosampleFields: biosampleFields,
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
				this.set("bioSpinning", true);
				this.set("bioQueryRan", false);
				
				_.each(resources, function(resource){
	  				resource.spinning=true;
	  				resource.queryRan=false;
	  				resource.bioQueryRan=false;
	  				
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
		
		patientDataCallback: function(resource, result, model, defaultOutput){
			
			var count = parseInt(result);
			
			resources[resource.uuid].queryRan = true;
			resources[resource.uuid].patientCount = count;
			//the spinning attribute maintains the spinner state when we render, but doesn't immediately update
			resources[resource.uuid].spinning = false;
			$("#patient-spinner-" + resource.uuid).hide();
			
			var model = defaultOutput.model;
			if(("" + result).includes("<")) {
				$("#patient-results-" + resource.uuid + "-count").html(result);
				model.set("aggregated", true);
			} else if( typeof count === "number" ){
				$("#patient-results-" + resource.uuid + "-count").html(count.toLocaleString()); 
				model.set("totalPatients", model.get("totalPatients") + count);
			} else {
				$("#patient-results-" + resource.uuid + "-count").html("-");
			}
			
			$("#patient-count").html((model.get("aggregated") ? ">" : "") +  model.get("totalPatients").toLocaleString());
			
			if(_.every(resources, (resource)=>{return resource.spinning==false})){
				model.set("spinning", false);
				model.set("queryRan", true);
				$("#patient-spinner-total").hide();
			}
		},
		
		biosampleDataCallback: function(resource, crossCounts, model, defaultOutput){
			var model = defaultOutput.model;
			
			resources[resource.uuid].biosampleCount = 0;
			
			//the spinning attribute maintains the spinner state when we render, but doesn't immediately update
			resources[resource.uuid].bioSpinning = false;
			resources[resource.uuid].bioQueryRan = true;
			
			_.each(biosampleFields, function(biosampleMetadata){
				if( crossCounts[biosampleMetadata.conceptPath] != undefined ){
					var count = parseInt(crossCounts[biosampleMetadata.conceptPath]);
					if( count >= 0 ){
						resources[resource.uuid].bioSampleCounts[biosampleMetadata.id] = count;
						resources[resource.uuid].biosampleCount += count;
						model.set("totalBiosamples", model.get("totalBiosamples") + count);
					} else {
						resources[resource.uuid].bioSampleCounts[biosampleMetadata.id] = undefined;
					}
				}
			});
			
			$("#biosamples-spinner-" + resource.uuid).hide();
			$("#biosamples-results-" + resource.uuid + "-count").html(resources[resource.uuid].biosampleCount.toLocaleString()); 
			$("#biosamples-count").html(model.get("totalBiosamples").toLocaleString());
			$("#more-info-btn").show();
			
			if(_.every(resources, (resource)=>{return resource.bioQueryRan==true})){
				model.set("bioSpinning", false);
				model.set("bioQueryRan", true);
				$("#biosamples-spinner-total").hide();
			}
		},
		
		
		genomicDataCallback: function(resource, crossCounts, model, defaultOutput){
			var model = defaultOutput.model;
			resources[resource.uuid].genomicdataCount = 0;
			
			//the spinning attribute maintains the spinner state when we render, but doesn't immediately update
			resources[resource.uuid].genomicSpinning = false;
			resources[resource.uuid].genomicQueryRan = true;
			
			_.each(genomicFields, function(genomicMetadata){
				if( crossCounts[genomicMetadata.conceptPath] != undefined ){
					var count = parseInt(crossCounts[genomicMetadata.conceptPath]);
					if( count >= 0 ){
						resources[resource.uuid].genomicdataCounts[genomicMetadata.id] = count;
						resources[resource.uuid].genomicdataCount += count;
						model.set("totalGenomicdata", model.get("totalGenomicdata") + count);
					} else {
						resources[resource.uuid].genomicdataCounts[genomicMetadata.id] = undefined;
					}
				}
			});
			
			$("#genomicdata-spinner-" + resource.uuid).hide();
			$("#genomicdata-results-" + resource.uuid + "-count").html(resources[resource.uuid].genomicdataCount.toLocaleString()); 
			$("#genomicdata-count").html(model.get("totalGenomicdata").toLocaleString());
			
			if(_.every(resources, (resource)=>{return resource.genomicQueryRan==true})){
				model.set("genomicSpinning", false);
				model.set("genomicQueryRan", true);
				$("#genomicdata-spinner-total").hide();
			}
		},
		
		//TODO remove all these defaultOutput params
		patientErrorCallback: function(resource, message, defaultOutput){
			console.log("error calling resource " + resource.uuid + ": " + message);
			var model = defaultOutput.model;
			
			resources[resource.uuid].queryRan = true;
			resources[resource.uuid].patientCount = '-';
			//the spinning attribute maintains the spinner state when we render, but doesn't immediately update
			resources[resource.uuid].spinning = false;
			$("#patient-spinner-" + resource.uuid).hide();
			$("#patient-results-" + resource.uuid + "-count").html('0'); 
			
			if(_.every(resources, (resource)=>{return resource.spinning==false})){
				model.set("spinning", false);
				model.set("queryRan", true);
				$("#patient-spinner-total").hide();
			}
		},
		
		biosampleErrorCallback: function(resource, message, defaultOutput){
			//errors from one resources shouldn't hide or change the results from other resources
			console.log("error calling resource " + resource.uuid + " biosamples: " + message);
			var model = defaultOutput.model;
			resources[resource.uuid].biosampleCount = 0;
			
			//the spinning attribute maintains the spinner state when we render, but doesn't immediately update
			resources[resource.uuid].bioSpinning = false;
			resources[resource.uuid].bioQueryRan = true;
			$("#biosamples-spinner-" + resource.uuid).hide();
			$("#biosamples-results-" + resource.uuid + "-count").html("0");
			
			_.each(biosampleFields, function(biosampleMetadata){
				resources[resource.uuid].bioSampleCounts[biosampleMetadata.id] = undefined;
			});
			
			if(_.every(resources, (resource)=>{return resource.bioQueryRan==true})){
				model.set("bioSpinning", false);
				model.set("bioQueryRan", true);
				$("#biosamples-spinner-total").hide();
			}
		},
		
		/*
		 * The new hook for overriding all custom query logic
		 */
		runQuery: function(defaultOutput, incomingQuery, defaultDataCallback, defaultErrorCallback){
			
			//sometimes the resources do not load quickly enough.  let's check and see if we are still waiting.
			if(this.resourceQueryDeferred.state() == "pending"){
				this.resourceQueryDeferred.done(function() {
					console.log("finished wiating for resources query");
					this.runQuery(defaultOutput, incomingQuery, defaultDataCallback, defaultErrorCallback);
				}.bind(this));
				return;
			} else {
				console.log("running override query");
			}
			
			var model = defaultOutput.model;
			model.set("resources", this.resources);
			model.set("aggregated", false);
			model.set("biosampleFields", this.biosampleFields);
			model.set("totalPatients",0);
			model.spinAll();
			
			model.baseQuery = incomingQuery;   
  			defaultOutput.render();
  			
  			//attach the information modal
  			this.moreInformationModal = new moreInformation.View(biosampleFields, this.resources);
  			this.moreInformationModal.setElement($("#moreInformation",this.$el));
//  			this.variantExplorerView.render();
  			
			//run a query for each resource 
			_.each(resources, function(resource){
				// make a safe deep copy (scoped per resource) of the incoming query so we don't modify it
				var query = JSON.parse(JSON.stringify(incomingQuery));
				query.query.expectedResultType="COUNT";
			
				this._runAjaxQuery(query, resource, this.patientDataCallback, this.patientErrorCallback, model, defaultOutput);
				
			}.bind(this));
			
			
			model.set("totalBiosamples",0);
			
			//run the biosample queries for each resource (sample/observation count)
			_.each(resources, function(resource){
				// make a safe deep copy (scoped per resource) of the incoming query so we don't modify it
				var query = JSON.parse(JSON.stringify(incomingQuery));
				query.query.crossCountFields = _.pluck(biosampleFields, "conceptPath");
				query.query.expectedResultType="OBSERVATION_CROSS_COUNT";
				this._runAjaxQuery(query, resource, this.biosampleDataCallback, this.biosampleErrorCallback, model, defaultOutput);
				
			}.bind(this));
			
			model.set("totalGenomicdata",0);
			
			//run the genomic data queries for each resource (CROSS COUNT, not observation count)
			_.each(resources, function(resource){
				// make a safe deep copy (scoped per resource) of the incoming query so we don't modify it
				var query = JSON.parse(JSON.stringify(incomingQuery));
				query.query.crossCountFields = _.pluck(genomicFields, "conceptPath");
				query.query.expectedResultType="CROSS_COUNT";
				this._runAjaxQuery(query, resource, this.genomicDataCallback, this.genomicErrorCallback, model, defaultOutput);
				
			}.bind(this));
		},
		
		//extract this boilerplate ajax method
		_runAjaxQuery: function(query, resource, dataCallBack, errorCallback, model, defaultOutput){
			query.resourceCredentials = {};
			query.resourceUUID = resource.uuid;
			$.ajax({
				 	url: window.location.origin + "/picsure/query/sync",
				 	type: 'POST',
				 	headers: {"Authorization": "Bearer " + JSON.parse(sessionStorage.getItem("session")).token},
				 	contentType: 'application/json',
				 	data: JSON.stringify(query),
				 	statusCode: { 401:function() { } },   //NOOP - don't fail on not authorized queries
  				 	success: function(response, textStatus, request){
  				 		dataCallBack(resource, response, model, defaultOutput);
  						}.bind(this),
				 	error: function(response){
				 		errorCallback(resource, this.outputErrorMessage, defaultOutput);
					}.bind(this)
				});
		}
	};
});
