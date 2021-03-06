/*
 * Licensed to Jecstar Innovation under one or more contributor
 * license agreements. Jecstar Innovation licenses this file to you
 * under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied. See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

function buildGraphsPage(groupName) {
    'use strict';
    let contextRoot = '../rest/visualization/';
    if (groupName) {
        contextRoot += encodeURIComponent(groupName) + '/';
        const $groupItem = $('#sub_visualizations > ul > li > a > p').filter(function () {
            return $(this).text() === groupName;
        });
        ;
        const $liItem = $groupItem.parent().removeClass('collapsed').attr('aria-expanded', 'true').parent();
        $liItem.children('div').addClass('show');
        $liItem.find('p:contains(Graphs)').closest('li').addClass('active');
    } else {
        const $liMenu = $('#sub_vis_user').addClass('show').parent();
        $liMenu.children('a').removeClass('collapsed').attr('aria-expanded', 'true');
        $liMenu.find('p:contains(Graphs)').closest('li').addClass('active');
    }
    let timeZone;
    let currentSelectedFile;

    const graphConfig = {
        area: {
            headingClass: 'fa-chart-area',
            subTypes: [
                {value: 'basic', text: 'Basic'},
                {value: 'percentage', text: 'Percentage'},
                {value: 'stacked', text: 'Stacked'},
            ],
            elements: {
                xAxis: true,
                yAxis: true,
                category: false,
                parts: false,
                fontSize: false,
                verticalAlignment: false,
                lineType: true,
                orientation: true,
                markers: true,
                dataLabels: true,
                legend: true
            }
        },
        bar: {
            headingClass: 'fa-chart-bar',
            subTypes: [
                {value: 'basic', text: 'Basic'},
                {value: 'percentage', text: 'Percentage'},
                {value: 'stacked', text: 'Stacked'},
            ],
            elements: {
                xAxis: true,
                yAxis: true,
                category: false,
                parts: false,
                fontSize: false,
                verticalAlignment: false,
                lineType: false,
                orientation: true,
                markers: false,
                dataLabels: false,
                legend: true
            }
        },
        line: {
            headingClass: 'fa-chart-line',
            subTypes: [],
            elements: {
                xAxis: true,
                yAxis: true,
                category: false,
                parts: false,
                fontSize: false,
                verticalAlignment: false,
                lineType: true,
                orientation: true,
                markers: true,
                dataLabels: true,
                legend: true
            }
        },
        number: {
            headingClass: 'fa-list-ol',
            subTypes: [],
            elements: {
                xAxis: false,
                yAxis: false,
                category: false,
                parts: true,
                fontSize: true,
                verticalAlignment: true,
                lineType: false,
                orientation: false,
                markers: false,
                dataLabels: false,
                legend: false
            }
        },
        pie: {
            headingClass: 'fa-chart-pie',
            subTypes: [
                {value: 'basic', text: 'Basic'},
                {value: 'donut', text: 'Donut'},
                {value: 'semi_circle', text: 'Semi circle'},
            ],
            elements: {
                xAxis: false,
                yAxis: false,
                category: true,
                parts: true,
                fontSize: false,
                lineType: false,
                verticalAlignment: false,
                orientation: false,
                markers: false,
                dataLabels: true,
                legend: true
            }
        },
        scatter: {
            headingClass: 'fa-braille',
            subTypes: [],
            elements: {
                xAxis: true,
                yAxis: true,
                category: false,
                parts: false,
                fontSize: false,
                verticalAlignment: false,
                lineType: false,
                orientation: true,
                markers: false,
                dataLabels: true,
                legend: true
            }
        },
        changeToGraphType: function (graphType) {
            const config = graphConfig[graphType];
            if (config === undefined) {
                return;
            }
            const $heading = $('#acc-heading-graph > button > span');
            $heading.removeClass(function (index, className) {
                return (className.match(/(^|\s)fa-\S+/g) || []).join(' ');
            });
            $heading.addClass(config.headingClass);
            $('#card-x-axis').toggle(config.elements.xAxis);
            $('#card-y-axis').toggle(config.elements.yAxis);
            $('#card-category').toggle(config.elements.category);
            $('#card-parts').toggle(config.elements.parts);

            const $subTypeGroup = $('#grp-graph-subtype').toggle(config.subTypes.length > 0);
            $('#grp-graph-font-size').toggle(config.elements.fontSize);
            $('#grp-graph-vertical-alignment').toggle(config.elements.verticalAlignment);
            $('#grp-graph-line-type').toggle(config.elements.lineType);
            $('#grp-graph-orientation').toggle(config.elements.orientation);
            $('#grp-graph-show-markers').toggle(config.elements.markers);
            $('#grp-graph-show-data-labels').toggle(config.elements.dataLabels);
            $('#grp-graph-show-legend').toggle(config.elements.legend);

            if (config.subTypes.length > 0) {
                const $subTypes = $('#sel-graph-subtype').empty();
                $.each(config.subTypes, function (index, element) {
                    $subTypes.append($('<option>').attr('value', element.value).text(element.text));
                });
                $subTypeGroup.show();
            }
        }

    };

    const graphContainerMap = {};
    let keywords = [];
    const $page = $('body > .wrapper > .main-panel');

    const originalFromValue = $('#input-graph-from').val();
    const originalTillValue = $('#input-graph-till').val();

    $('#input-graph-from').val('').parent()
        .flatpickr({
            dateFormat: "Y-m-dTH:i:S",
            enableTime: true,
            enableSeconds: true,
            time_24hr: true,
            allowInput: true,
            defaultHour: 0,
            defaultMinute: 0,
            clickOpens: false,
            wrap: true
        });
    $('#input-graph-till').val('').parent()
        .flatpickr({
            dateFormat: "Y-m-dTH:i:S",
            enableTime: true,
            enableSeconds: true,
            time_24hr: true,
            allowInput: true,
            defaultHour: 23,
            defaultMinute: 59,
            clickOpens: false,
            wrap: true
        });
    if (originalFromValue) {
        $('#input-graph-from').val(originalFromValue);
    }
    if (originalTillValue) {
        $('#input-graph-till').val(originalTillValue)
    }

    $.ajax({
        type: 'GET',
        contentType: 'application/json',
        url: contextRoot + 'datasources',
        cache: false,
        success: function (data) {
            if (!data || !data.dashboard_datasources) {
                return;
            }
            const $dsSelect = $('#sel-data-source');
            $.each(data.dashboard_datasources, function (index, datasource) {
                $dsSelect.append($('<option>').attr('value', datasource).text(datasourceToText(datasource)));
            });
            commons.sortSelectOptions($dsSelect);

            function datasourceToText(datasource) {
                if ('etm_audit_all' === datasource) {
                    return 'Audits';
                } else if ('etm_event_all' === datasource) {
                    return 'Events';
                } else if ('etm_metrics_all' === datasource) {
                    return 'Metrics';
                } else {
                    return datasource;
                }
            }
        }
    });
    $.when(
        $.ajax({
            type: 'GET',
            contentType: 'application/json',
            url: contextRoot + 'keywords',
            cache: false,
            success: function (data) {
                if (!data || !data.keywords) {
                    return;
                }
                keywords = data.keywords;
            }
        }),
        $.ajax({
            type: 'GET',
            contentType: 'application/json',
            url: contextRoot + 'graphs',
            cache: false,
            success: function (data) {
                if (!data) {
                    return;
                }
                const $graphSelect = $('#sel-graph');
                $.each(data.graphs, function (index, graphContainer) {
                    $graphSelect.append($('<option>').attr('value', graphContainer.name).text(graphContainer.name));
                    graphContainerMap[graphContainer.name] = graphContainer;
                });
                commons.sortSelectOptions($graphSelect);
                $graphSelect.val('');
                timeZone = data.timeZone;
            }
        })
    ).done(function () {
        aggregators.initialize({
            keywords: keywords,
            enableOrDisableButtons: enableOrDisableButtons
        });
        $page.on('input autocomplete:selected', 'input, textarea', enableOrDisableButtons);
        $page.on('change', 'select', enableOrDisableButtons);

        $('#input-graph-query').on('keydown', function (event) {
            if (event.which === $.ui.keyCode.SPACE && event.ctrlKey && !$(this).autocomplete('instance').menu.active) {
                $(this).autocomplete("search", $(this).val());
            }
        }).autocompleteFieldQuery(
            {
                queryKeywords: keywords,
                keywordIndexFilter: function (index) {
                    return index !== $('#sel-data-source').val();
                },
                allowJoins: true
            }
        );
        $('#input-graph-time-filter-field').on('keydown', function (event) {
            if (event.keyCode === $.ui.keyCode.ESCAPE && $(this).autocomplete('instance').menu.active) {
                event.stopPropagation();
            }
        }).autocompleteFieldQuery(
            {
                queryKeywords: keywords,
                mode: 'field',
                keywordFilter: function (index, group, keyword) {
                    return !keyword.date;
                },
                keywordIndexFilter: function (index) {
                    return index !== $('#sel-data-source').val();
                }
            }
        );
        const $xAxisNBucketContainer = $('#acc-collapse-x-axis > .card-body');
        aggregators.addBucketAggregator('x-axis', $xAxisNBucketContainer, 1, 0, null);
        shapeXAxisBucketAggregator($xAxisNBucketContainer);

        const $categoryBucketContainer = $('#acc-collapse-category > .card-body');
        aggregators.addBucketAggregator('category', $categoryBucketContainer, 1, 0, null);
        shapeCategoryBucketAggregator($categoryBucketContainer);

        $('#sel-graph-type').trigger('change');
    });

    $('#sel-graph-type').on('change', function (event) {
        event.preventDefault();
        graphConfig.changeToGraphType($(this).val());
    });

    // Handle the save graph button
    $('#btn-confirm-save-graph').on('click', function (event) {
        event.preventDefault();
        const graphName = $('#input-graph-name').val();
        if (isGraphExistent(graphName)) {
            $('#overwrite-graph-name').text(graphName);
            $('#modal-graph-overwrite').modal();
        } else {
            saveGraph();
        }
    });

    $('#btn-save-graph').on('click', function (event) {
        event.preventDefault();
        saveGraph();
    });

    $('#btn-preview-graph').on('click', function (event) {
        event.preventDefault();
        previewGraph();
    });

    $('#btn-confirm-remove-graph').on('click', function (event) {
        event.preventDefault();
        $('#remove-graph-name').text($('#input-graph-name').val());
        $('#modal-graph-remove').modal();
    });

    $('#btn-remove-graph').on('click', function (event) {
        event.preventDefault();
        removeGraph($('#input-graph-name').val());
    });

    $('#btn-select-import-file').on('click', function (event) {
        event.preventDefault();
        $('#modal-graph-import').modal();
    });

    $('#graph-import-file').on('change', function (event) {
        const files = event.target.files;
        if (files.length > 0) {
            currentSelectedFile = files[0];
        } else {
            currentSelectedFile = null;
        }
    });

    $('#btn-import-graph').on('click', function (event) {
        event.preventDefault();
        if (currentSelectedFile) {
            const reader = new FileReader();
            reader.onload = function (e) {
                const contents = e.target.result;
                const graphData = JSON.parse(contents);
                resetValues();
                if ('undefined' == typeof graphData) {
                    return;
                }
                setValuesFromData(graphData);
                enableOrDisableButtons();
            };
            reader.readAsText(currentSelectedFile);
        }
        commons.hideModals($('#modal-graph-import'));
    });

    $('#lnk-export-graph').on('click', function (event) {
        event.preventDefault();
        const anchor = document.createElement('a');
        const graphData = createGraphData();
        const blob = new Blob([JSON.stringify(graphData)], {'type': 'application/json'});
        anchor.href = window.URL.createObjectURL(blob);
        anchor.download = graphData.name + '.json';
        document.body.appendChild(anchor);
        anchor.click();
        document.body.removeChild(anchor);
    });

    $('#sel-graph').on('change', function (event) {
        event.preventDefault();
        const graphData = graphContainerMap[$(this).val()];
        resetValues();
        if ('undefined' == typeof graphData) {
            return;
        }
        setValuesFromData(graphData);
        enableOrDisableButtons();
    });

    function enableOrDisableButtons() {
        let valid = validateForm('form-data');
        valid = valid && validateForm('form-graph');
        if ($('#card-x-axis:hidden').length === 0) {
            valid = valid && validateForm('form-x-axis');
        }
        if ($('#card-y-axis:hidden').length === 0) {
            valid = valid && validateForm('form-y-axis');
            valid = valid && aggregators.validateNumberOfMetricsAndPipelines('form-y-axis');
        }
        if ($('#card-category:hidden').length === 0) {
            valid = valid && validateForm('form-category');
        }
        if ($('#card-parts:hidden').length === 0) {
            valid = valid && validateForm('form-parts');
            valid = valid && aggregators.validateNumberOfMetricsAndPipelines('form-parts');
        }

        if (valid) {
            $('#btn-preview-graph').removeAttr('disabled');
        } else {
            $('#btn-preview-graph').attr('disabled', 'disabled');
        }

        const $graphName = $('#input-graph-name');
        valid = valid && $graphName[0].checkValidity();
        if (valid) {
            $('#btn-confirm-save-graph').removeAttr('disabled');
            $('#lnk-export-graph').show();
        } else {
            $('#btn-confirm-save-graph').attr('disabled', 'disabled');
            $('#lnk-export-graph').hide();
        }

        const graphName = $graphName.val();
        if (graphName && isGraphExistent(graphName)) {
            $('#btn-confirm-remove-graph').removeAttr('disabled');
        } else {
            $('#btn-confirm-remove-graph').attr('disabled', 'disabled');
        }

        function validateForm(formId) {
            const $form = $('#' + formId);
            $form.find('div.form-group').each(function () {
                if ($(this).css('display') === 'none') {
                    $(this).find('[data-required="required"]').removeAttr('required');
                }
            });

            let valid = false;
            if ($form[0].checkValidity()) {
                valid = true;
            }
            $form.find('[data-required]').attr('required', 'required');
            return valid;
        }
    }

    function shapeXAxisBucketAggregator(bucketContainer) {
        const $bucketContainer = $(bucketContainer);
        $bucketContainer.find('.bucket-aggregator-header').remove();
        $bucketContainer.children('.aggregator-container-block').removeAttr('style').removeClass('bg border rounded bg-light');
        const $children = $bucketContainer.find(".bucket-aggregator-block > .form-group");
        $children.first().next().remove();
        $children.first().remove();
        // Remove options from select...
        $bucketContainer.find("select[data-element-type='bucket-aggregator-selector']").children().each(function () {
            if (isSingleValueBucketAggregator($(this).attr('value'))) {
                $(this).remove();
            }
        }).trigger('change');
    }

    function shapeCategoryBucketAggregator(bucketContainer) {
        const $bucketContainer = $(bucketContainer);
        $bucketContainer.find('.bucket-aggregator-header').remove();
        $bucketContainer.children('.aggregator-container-block').removeAttr('style').removeClass('bg border rounded bg-light');
        const $children = $bucketContainer.find(".bucket-aggregator-block > .form-group");
        $children.first().next().remove();
        $children.first().remove();
        // Remove options from select...
        $bucketContainer.find("select[data-element-type='bucket-aggregator-selector']").children().each(function () {
            if (!('term' === $(this).attr('value') || 'significant_term' === $(this).attr('value'))) {
                $(this).remove();
            }
        }).trigger('change');
    }

    function isSingleValueBucketAggregator(aggregatorType) {
        switch (aggregatorType) {
            case 'filter':
            case 'missing':
                return true;
            default:
                return false;
        }
    }

    function createGraphData() {
        const graphName = $('#input-graph-name').val();
        const dataSource = $('#sel-data-source').val();
        const graphFrom = $('#input-graph-from').val();
        const graphTill = $('#input-graph-till').val();
        const graphTimeFilterField = $('#input-graph-time-filter-field').val();
        const graphQuery = $('#input-graph-query').val();
        const graphData = {
            name: graphName ? graphName : null,
            data: {
                data_source: dataSource,
                from: graphFrom ? graphFrom : null,
                till: graphTill ? graphTill : null,
                time_filter_field: graphTimeFilterField ? graphTimeFilterField : null,
                query: graphQuery ? graphQuery : null,
            },
            graph: {
                type: $('#sel-graph-type').val()
            }
        };

        if ($('#grp-graph-subtype').css('display') !== 'none') {
            graphData.graph.sub_type = $('#sel-graph-subtype').val();
        }
        if ($('#grp-graph-font-size').css('display') !== 'none') {
            graphData.graph.font_size = Number($('#input-graph-font-size').val());
        }
        if ($('#grp-graph-vertical-alignment').css('display') !== 'none') {
            graphData.graph.vertical_alignment = $('#sel-graph-vertical-alignment').val();
        }
        if ($('#grp-graph-line-type').css('display') !== 'none') {
            graphData.graph.line_type = $('#sel-graph-line-type').val();
        }
        if ($('#grp-graph-orientation').css('display') !== 'none') {
            graphData.graph.orientation = $('#sel-graph-orientation').val();
        }
        if ($('#grp-graph-show-markers').css('display') !== 'none') {
            graphData.graph.show_markers = $('#sel-graph-show-markers').val() === 'true';
        }
        if ($('#grp-graph-show-legend').css('display') !== 'none') {
            graphData.graph.show_legend = $('#sel-graph-show-legend').val() === 'true';
        }
        if ($('#grp-graph-show-data-labels').css('display') !== 'none') {
            graphData.graph.show_data_labels = $('#sel-graph-show-data-labels').val() === 'true';
        }
        if ($('#card-x-axis:hidden').length === 0) {
            const dataAggregators = aggregators.createAggregatorData('x-axis', $('#acc-collapse-x-axis > .aggregator-container-block'));
            graphData.graph.x_axis = {
                aggregator: dataAggregators[0]
            };
            graphData.graph.x_axis.aggregator.show_on_graph = true;
        }
        if ($('#card-y-axis:hidden').length === 0) {
            graphData.graph.y_axis = {
                title: $('#input-y-axis-title').val(),
                format: $('#input-y-axis-format').val(),
                aggregators: aggregators.createAggregatorData('y-axis', $('#acc-collapse-y-axis > .card-body > .aggregator-container-block'))
            };

        }
        if ($('#card-category:hidden').length === 0) {
            const dataAggregators = aggregators.createAggregatorData('category', $('#acc-collapse-category > .aggregator-container-block'));
            graphData.graph.x_axis = {
                aggregator: dataAggregators[0]
            };
            graphData.graph.x_axis.aggregator.show_on_graph = true;
        }
        if ($('#card-parts:hidden').length === 0) {
            graphData.graph.y_axis = {
                format: $('#input-parts-format').val(),
                aggregators: aggregators.createAggregatorData('parts', $('#acc-collapse-parts > .card-body > .aggregator-container-block'))
            };

        }
        return graphData;
    }

    function saveGraph() {
        const graphData = createGraphData();
        $.ajax({
            type: 'PUT',
            contentType: 'application/json',
            url: contextRoot + 'graph/' + encodeURIComponent(graphData.name),
            cache: false,
            data: JSON.stringify(graphData),
            success: function (data) {
                if (!data) {
                    return;
                }
                if (!isGraphExistent(graphData.name)) {
                    const $graphSelect = $('#sel-graph');
                    $graphSelect.append($('<option>').attr('value', graphData.name).text(graphData.name));
                    commons.sortSelectOptions($graphSelect);
                }
                graphContainerMap[graphData.name] = graphData;
                commons.showNotification('Graph \'' + graphData.name + '\' saved.', 'success');
                enableOrDisableButtons();
            }
        }).always(function () {
            commons.hideModals($('#modal-graph-overwrite'));
        });
    }

    function removeGraph(graphName) {
        $.ajax({
            type: 'DELETE',
            contentType: 'application/json',
            url: contextRoot + 'graph/' + encodeURIComponent(graphName),
            cache: false,
            success: function (data) {
                if (!data) {
                    return;
                }
                delete graphContainerMap[graphName];
                $("#sel-graph > option").filter(function () {
                    return $(this).attr("value") === graphName;
                }).remove();
                commons.showNotification('Graph \'' + graphName + '\' removed.', 'success');
                enableOrDisableButtons();
            }
        }).always(function () {
            commons.hideModals($('#modal-graph-remove'));
        });
    }

    function previewGraph() {
        const graphData = createGraphData();
        $.ajax({
            type: 'POST',
            contentType: 'application/json',
            url: contextRoot + 'graphdata',
            cache: false,
            data: JSON.stringify(graphData),
            success: function (response) {
                if (!response) {
                    return;
                }
                const $previewBox = $('#preview_box').empty();
                Highcharts.setOptions({
                    lang: {
                        decimalPoint: response.locale.decimal,
                        thousandsSep: response.locale.thousands,
                        timezone: response.locale.timezone
                    }
                });
                d3.formatDefaultLocale({
                    decimal: response.locale.decimal,
                    thousands: response.locale.thousands,
                    grouping: [3],
                    currency: response.locale.currency
                });
                const chartConfig = response.chart_config;
                chartConfig.title = {text: 'Chart preview'};
                if (chartConfig.yAxis) {
                    chartConfig.yAxis.labels = {
                        formatter: function () {
                            return formatLabel(response.valueFormat, this.value)
                        }
                    };
                }
                if (chartConfig.chart) {
                    chartConfig.chart.style = {
                        fontFamily: 'inherit'
                    };
                }
                if ('number' === response.type) {
                    chartConfig.chart = {
                        events: {
                            load: function () {
                                this.renderer.text(formatLabel(response.valueFormat, response.value), this.chartWidth / 2, 'BOTTOM' === chartConfig.vertical_alignment ? this.chartHeight - 10 : this.chartHeight / 2)
                                    .css({
                                        fontSize: chartConfig.font_size,
                                    })
                                    .attr('text-anchor', 'middle')
                                    .add();

                            }
                        }
                    }
                }
                if (chartConfig.tooltip) {
                    chartConfig.tooltip.pointFormatter = function () {
                        return '<span style="color:' + this.color + '">\u25CF</span> ' + this.series.name + ': <b>' + formatLabel(response.valueFormat, this.y) + '</b><br/>'
                    };
                }
                Highcharts.chart('preview_box', chartConfig);
                $('html,body').animate({scrollTop: $previewBox.parent().parent().offset().top}, 'fast');
            }
        });

        function formatLabel(labelFormat, labelValue) {
            if (labelFormat) {
                try {
                    const format = d3.format(labelFormat);
                    return format(labelValue);
                } catch (err) {
                    console.log(err);
                }
            }
            return labelValue;
        }
    }

    function setValuesFromData(graphContainer) {
        $('#input-graph-name').val(graphContainer.name);
        // Set the data block
        $('#sel-data-source').val(graphContainer.data.data_source);
        let momentValue = moment(graphContainer.data.from, 'x', true);
        if (momentValue.isValid() && timeZone) {
            $('#input-graph-from').val(momentValue.tz(timeZone).format('YYYY-MM-DDTHH:mm:ss'));
        } else {
            $('#input-graph-from').val(graphContainer.data.from);
        }
        momentValue = moment(graphContainer.data.till, 'x', true);
        if (momentValue.isValid() && timeZone) {
            $('#input-graph-till').val(momentValue.tz(timeZone).format('YYYY-MM-DDTHH:mm:ss'));
        } else {
            $('#input-graph-till').val(graphContainer.data.till);
        }
        $('#input-graph-time-filter-field').val(graphContainer.data.time_filter_field);
        $('#input-graph-query').val(graphContainer.data.query);
        // Set the graph block
        $('#sel-graph-type').val(graphContainer.graph.type).trigger('change');
        if (graphContainer.graph.sub_type) {
            $('#sel-graph-subtype').val(graphContainer.graph.sub_type);
        }
        if (graphContainer.graph.font_size) {
            $('#input-graph-font-size').val(graphContainer.graph.font_size);
        }
        if (graphContainer.graph.vertical_alignment) {
            $('#sel-graph-vertical-alignment').val(graphContainer.graph.vertical_alignment);
        }
        if (graphContainer.graph.line_type) {
            $('#sel-graph-line-type').val(graphContainer.graph.line_type);
        }
        if (graphContainer.graph.orientation) {
            $('#sel-graph-orientation').val(graphContainer.graph.orientation);
        }
        const $showMarkers = $('#sel-graph-show-markers');
        const $showLegend = $('#sel-graph-show-legend');
        const $showDataLabels = $('#sel-graph-show-data-labels');
        graphContainer.graph.show_markers ? $showMarkers.val('true') : $showMarkers.val('false');
        graphContainer.graph.show_legend ? $showLegend.val('true') : $showLegend.val('false');
        graphContainer.graph.show_data_labels ? $showDataLabels.val('true') : $showDataLabels.val('false');
        if (graphContainer.graph.x_axis) {
            if ('area' === graphContainer.graph.type ||
                'bar' === graphContainer.graph.type ||
                'line' === graphContainer.graph.type ||
                'scatter' === graphContainer.graph.type) {
                const $xAxisBucketContainer = $('#acc-collapse-x-axis > .card-body').empty();
                aggregators.addBucketAggregator('x-axis', $xAxisBucketContainer, 1, 0, graphContainer.graph.x_axis.aggregator);
                shapeXAxisBucketAggregator($xAxisBucketContainer);
            } else if ('pie' === graphContainer.graph.type) {
                const $categoryBucketContainer = $('#acc-collapse-category > .card-body').empty();
                aggregators.addBucketAggregator('category', $categoryBucketContainer, 1, 0, graphContainer.graph.x_axis.aggregator);
                shapeCategoryBucketAggregator($categoryBucketContainer);
            }
        }
        if (graphContainer.graph.y_axis) {
            if ('area' === graphContainer.graph.type ||
                'bar' === graphContainer.graph.type ||
                'line' === graphContainer.graph.type ||
                'scatter' === graphContainer.graph.type) {
                $('#input-y-axis-format').val(graphContainer.graph.y_axis.format);
                $('#input-y-axis-title').val(graphContainer.graph.y_axis.title);
                const $container = $('#acc-collapse-y-axis > .card-body > .aggregator-container-block');
                $.each(graphContainer.graph.y_axis.aggregators, function (index, aggregator) {
                    addAggregator('y-axis', $container, 0, index, aggregator);
                });
            } else if ('number' === graphContainer.graph.type ||
                'pie' === graphContainer.graph.type) {
                $('#input-parts-format').val(graphContainer.graph.y_axis.format);
                const $container = $('#acc-collapse-parts > .card-body > .aggregator-container-block');
                $.each(graphContainer.graph.y_axis.aggregators, function (index, aggregator) {
                    addAggregator('parts', $container, 0, index, aggregator);
                });
            }
        }

        $('[id^=sel-bucket-aggregator-], [id^=sel-metrics-aggregator-], [id^=sel-pipeline-aggregator-]').trigger('change');

        function addAggregator(dataContext, $container, level, index, aggregator) {
            if ('metrics' === aggregator.type) {
                aggregators.addMetricsAggregator(dataContext, $container, level, index, aggregator);
            } else if ('bucket' === aggregator.type) {
                aggregators.addBucketAggregator(dataContext, $container, level + 1, index, aggregator);
            } else if ('pipeline' === aggregator.type) {
                aggregators.addPipelineAggregator(dataContext, $container, level, index, aggregator);
            }
        }
    }

    function resetValues() {
        $('#input-graph-name').val('');
        document.getElementById('form-data').reset();
        document.getElementById('form-graph').reset();
        document.getElementById('form-x-axis').reset();
        document.getElementById('form-y-axis').reset();
        document.getElementById('form-category').reset();
        document.getElementById('form-parts').reset();
        $("a[data-element-type='remove-metrics-aggregator'], a[data-element-type='remove-bucket-aggregator'], a[data-element-type='remove-pipeline-aggregator']").trigger('click');
        $('#sel-graph-type').trigger('change');
        enableOrDisableButtons();
    }

    function isGraphExistent(name) {
        return "undefined" != typeof graphContainerMap[name];
    }
}