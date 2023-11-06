$(function () {
    $("#share").submit(sub);
});

function sub() {
    $.post(
        CONTEXT_PATH + "/share",
        {"url": $("#old-password").val()},
        function (data) {
            data = $.parseJSON(data);
            if (data.code == 0) {
                alert("复制此链接到地址栏下载，并自行更改后缀为图片格式\n" + data.url);
            } else {
                alert("服务器错误！");
            }
        }
    );
    //执行完异步请求后，停止，防止submit继续提交
    return false;
}