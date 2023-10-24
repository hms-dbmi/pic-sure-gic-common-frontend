define([
    "underscore",
    "text!overrides/dataset/dataset-view.hbs",
    "text!filter/variant-data.json",
    "common/modal"
], function(_, template, variantDataJson, modal) {
    // Create an index between the consequence and it's severity so we can group in the ui
	const variantConsequenceIndex = JSON.parse(variantDataJson)?.consequences.reduce((map, severity) => {
        severity.children.forEach(({text}) => map[text] = severity.text);
        return map;
    }, {});

    // Given some list of selected consequences, return a grouped map of consequences by severity
	const groupConsequences = function(consequences){
		return consequences.reduce((map, consequence) => {
			const severity = variantConsequenceIndex[consequence];
			const severityList = map[severity] || [];
			severityList.push(consequence);
			map[severity] = severityList;
			return map;
		}, {});
	};
	
	const categoryVariant = function(filter){
		const {
			Gene_with_variant: gene = [],
			Variant_consequence_calculated: consequenceList = [],
			Variant_frequency_as_text: frequency = []
		} = filter;
		const list = [];
		if(gene.length > 0){
			list.push(`<span class="list-title">Gene${gene.length > 1 ? 's' : ''} with Variant:</span> ${gene.join(', ')}`);
		}
		if(frequency.length > 0){
			list.push(`<span class="list-title">Variant Frequency:</span> ${frequency.join(', ')}`);
		}
		if(consequenceList.length > 0){
			const conString = Object.entries(groupConsequences(consequenceList))
				.map(([severity, consequences]) => `<li><span class="list-title">${severity}:</span> ${consequences.join(', ')}</li>`);
			list.push(`<span class="list-title">Calculated Variant Consequence</span><ul>${conString.join('')}</ul>`);
		}
		return list.join('<br />');
	};

	const numericVariant = function(filter){
		return Object.entries(filter)
			.map(([category, { min, max }]) => {
				const range = [];
				min && range.push(`Min: ${min}`);
				max && range.push(`Max: ${max}`);
				return `<span class="list-title">${category}:</span> Restrict values by ${range.join(', ')}`;
			});
	};

    return {
		template,
		mappers: {
			uuid: {
                path: ['metadata', 'siteQueryIds'],
                renderId: "detail-summary-ids",
				render: function(siteList){
					return siteList.map(({ name, queryId }) => 
						`<div id="site-${name}" class="row p-0">` +
							`<div class="col-md-3 p-0">${name}</div>` +
							`<div class="col-md-9 p-0">${queryId}</div>` +
						`</div>`
					).join('');
				}
            },
			genomic: {
				path: ['query', 'query', 'query', 'variantInfoFilters'],
				renderId: "detail-filters",
				render: function(filtersList = []){
					const filterString = [];
					filtersList.map(({
						numericVariantInfoFilters,
						categoryVariantInfoFilters
					}) => {
						if(!_.isEmpty(categoryVariantInfoFilters)){
							filterString.push(categoryVariant(categoryVariantInfoFilters))
						}
						if(!_.isEmpty(numericVariantInfoFilters)){
							filterString.push(numericVariant(numericVariantInfoFilters));
						}
					});
                    return filterString.map(item => `<li>${item}</li>`).join('');
				}
			}
		},
		renderExt: function(package){
			// Variables could be empty in GIC, so if it is, don't show it's box
			if($.trim($("#detail-variables").html()) == ''){
				$("#detail-variables-container").addClass("hidden");
			}
			modal.createTabIndex();
		},
    };
});