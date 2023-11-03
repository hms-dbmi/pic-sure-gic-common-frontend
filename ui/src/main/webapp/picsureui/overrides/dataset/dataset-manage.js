define([ "underscore" ],  function(_){
    return {
        staticColumnDefs: [
            { // hide dataset id column
                targets: 1,
                visible: false
            }
        ],
        actions: function(actions){
            const btnStyle = item => item.replaceAll("btn-default", "btn-outline");
            actions.active.copy.style = btnStyle(actions.active.copy.style);
            actions.active.view.style = btnStyle(actions.active.view.style);
            actions.active.archive.style = btnStyle(actions.active.archive.style);
            actions.archived.restore.style = btnStyle(actions.archived.restore.style);

            actions.active.copy.handler = row => {
                const { uuid, metadata } = row.data();

                const queryIdsString = metadata.siteQueryIds.map(site => site.name + ": " + site.queryId ).join(',');
                navigator.clipboard.writeText(queryIdsString);
                
                const key = "dataset-action-copy-" + uuid?.split("-")[0];
                const originalText = document.getElementById(key).innerText;
                document.getElementById(key).innerText = "Copied!";

                // reset after some delay
                _.delay(() => { document.getElementById(key).innerText = originalText; }, 4500);
            }
            return actions;
        },
        renderTable: function(tableId, data, package){
            data.forEach(obj =>  obj.query.startTime = obj.metadata.saved);

            const { columns, columnDefs, handlers } = package.getColumnsAndActions(tableId);

            const table = $("#" + tableId + "-table").DataTable({
                data, columns, columnDefs,
                order: [[2, "desc"]] // order by created date
            });

            handlers.forEach(({name, handler}) => {
                table.on("click", "button.action-" + name, event => {
                    handler(table.row(event.target.closest("tr")));
                });
            });
        },
        renderExt: function(){
            $("#toggle-archived-btn").removeClass("btn-default");
            $("#toggle-archived-btn").addClass("btn-outline");
        }
    };
});