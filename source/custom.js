require(['base/js/namespace', 'base/js/events'],
function (Jupyter, events) {
    // save a reference to the cell we're currently executing inside of,
    // to avoid clearing it later (which would remove this js)
    var this_cell = $(element).closest('.cell').data('cell');
    function clear_other_cells () {
        Jupyter.notebook.get_cells().forEach(function (cell) {
            if (cell.cell_type === 'code' && cell !== this_cell) {
                cell.clear_output();
            }
            Jupyter.notebook.set_dirty(true);
        });
    }

    if (Jupyter.notebook._fully_loaded) {
        // notebook has already been fully loaded, so clear now
        clear_other_cells();
    }
    // Also clear on any future load
    // (e.g. when notebook finishes loading, or when a checkpoint is reloaded)
    events.on('notebook_loaded.Notebook', clear_other_cells);
});
