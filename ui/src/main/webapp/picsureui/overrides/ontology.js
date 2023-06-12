define([], function(){
	return {

		infoColumnsTimeout : 5000,
		/*
		 * A function that takes a PUI that is already split on forward slash and returns
		 * the category value for that PUI.
		 */
		extractCategoryFromPui : undefined,
		
		/*
		 * A function that takes a PUI that is already split on forward slash and returns
		 * the parent value for that PUI.
		 */
		extractParentFromPui : undefined
	};
});