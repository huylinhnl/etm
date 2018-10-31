function buildIndexStatsPage() {
	$.ajax({
	    type: 'GET',
	    contentType: 'application/json',
	    url: '../rest/settings/indicesstats',
	    cache: false,
	    success: function(data) {
	        if (!data) {
	            return;
	        }
	        setStatisticsData(data)
	    }
	});

    function getLineChartDataForPerformance(indices) {
        var search = []
        var index = []
        $.each(indices, function (ix, value) {
            search.push(value.average_search_time ? value.average_search_time : 0);
            index.push(value.average_index_time ? value.average_index_time : 0);
        });
        return [
            {
                data: search,
                name: 'Search'
            },
            {
                data: index,
                name: 'Index'
            }
        ];
    }

    function setStatisticsData(response) {
        $("#text-total-events").text(response.totals.document_count_as_string);
        $("#text-total-size").text(response.totals.size_in_bytes_as_string);

        response.indices.sort(function (a, b) {
            if (a.name == b.name) {
                return 0;
            }
            if (a.name > b.name) {
                return -1;
            } else {
                return 1;
            }
        });
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
            currency: response.locale.currency
        });

        Highcharts.chart('count_chart', {
            credits: {
                enabled: false
            },
            chart: {
                type: 'column',
                height: '20%'
            },
            legend: {
                enabled: false
            },
            title: {
                text: 'Event count per index'
            },
            plotOptions: {
                column: {
                    colorByPoint: true
                }
            },
            xAxis: {
                categories: $.map(response.indices, function (index) {
                    return index.name;
                })
            },
            yAxis: {
                title: {
                    text: 'Event count'
                }
            },
            series: [{
                name: 'Count',
                data: $.map(response.indices, function (index) {
                    return index.document_count;
                })
            }]
        });
        Highcharts.chart('performance_chart', {
            credits: {
                enabled: false
            },
            chart: {
                type: 'line',
                height: '20%'
            },
            legend: {
                enabled: true
            },
            title: {
                text: 'Performance averages in milliseconds'
            },
            xAxis: {
                categories: $.map(response.indices, function (index) {
                    return index.name;
                })
            },
            tooltip: {
                shared: true
            },
            yAxis: {
                title: {
                    text: 'Milliseconds'
                }
            },
            series: getLineChartDataForPerformance(response.indices)
        });
        Highcharts.chart('size_chart', {
            credits: {
                enabled: false
            },
            chart: {
                type: 'column',
                height: '20%'
            },
            legend: {
                enabled: false
            },
            title: {
                text: 'Size per index'
            },
            plotOptions: {
                column: {
                    colorByPoint: true
                }
            },
            xAxis: {
                categories: $.map(response.indices, function (index) {
                    return index.name;
                })
            },
            yAxis: {
                title: {
                    text: 'Index size'
                },
                labels: {
                    formatter: function () {
                        return formatLabel(".2s", this.value);
                    }
                }
            },
            tooltip: {
                enabled: true
            },
            series: [{
                name: 'Size',
                data: $.map(response.indices, function (index) {
                    return index.size_in_bytes;
                })
            }]
        });

        function formatLabel(labelFormat, labelValue) {
            if (labelFormat) {
                try {
                    var format = d3.format(labelFormat);
                    return format(labelValue);
                } catch (err) {
                    console.log(err);
                }
            }
            return labelValue;
        }
    }
}