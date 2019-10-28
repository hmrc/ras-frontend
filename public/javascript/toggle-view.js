$(function() {
    $('#errors').focus();

    var pageTitle = $('title').text();

    $("#get-help-action").click(function() {
        $('send', 'event', 'link - click', pageTitle, 'get help with this page');
    });


    $("#get-help-action").click(function() {
        ga('send', 'event', 'link - click', pageTitle, 'get help with this page');
    });

    $("#full-width-banner-no-thanks").click(function(e){
        e.preventDefault();
        $('.full-width-banner').fadeOut('slow');
        GOVUK.setCookie("rasUrBannerHide", 1, 99999999999);
    });
});