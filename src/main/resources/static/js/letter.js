$(function () {
    $("#sendBtn").click(send_letter);
    $(".close").click(delete_msg);
});

function send_letter() {
    //隐藏内容框
    $("#sendModal").modal("hide");

    //获取用户名和填写内容框的值
    var targetName = $("#recipient-name").val();
    var content = $("#message-text").val();
    $.post(
        CONTEXT_PATH + "/letter/send",
        {"targetName": targetName, "content": content},
        function (data) {
            data = $.parseJSON(data);
            if (data.code == 0) {
                $("#hintBody").text("发送成功");
            } else {
                $("#hintBody").text(data.msg);
            }
            //弹出成功与否提示框
            $("#hintModal").modal("show");
            setTimeout(function () {
                $("#hintModal").modal("hide");
            }, 2000);
        })
}

function delete_msg() {
    // TODO 删除数据
    $(this).parents(".media").remove();
}