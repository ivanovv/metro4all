var ma = {};

ma.viewmodel = {};
ma.view = {};

(function ($, ma) {
    ma.url = {};
    ma.view.$document = $(document);

    $.extend(ma.url, {
        init: function () {
            this.bindEvents();
        },


        bindEvents: function() {
            var context = this,
                $document = ma.view.$document;

            $document.on('/url/update', function(e, key, value) {
                var uri = context.updateQueryStringParameter(window.location.href, key, value);
                window.location.href = uri;
            })
        },


        // from http://stackoverflow.com/questions/5999118/add-or-update-query-string-parameter
        updateQueryStringParameter: function (uri, key, value) {
            uri.replace('?', '#');
            var re = new RegExp("([#|&])" + key + "=.*?(&|$)", "i"),
                separator = uri.indexOf('#') !== -1 ? "&" : "#";

            if (uri.match(re)) {
                return uri.replace(re, '$1' + key + "=" + value + '$2');
            }
            else {
                return uri + separator + key + "=" + value;
            }
        }
    })
})(jQuery, ma)